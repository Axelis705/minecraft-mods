package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.crate.CrateKind;
import com.mundosdeaxelis.network.BossBriefPacket;
import com.mundosdeaxelis.network.BossDamagePacket;
import com.mundosdeaxelis.network.BossResultPacket;
import com.mundosdeaxelis.network.ModNetwork;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import com.mundosdeaxelis.table.Mats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Recompensas al vencer a un jefe de dungeon, según el daño que hizo cada jugador ({@link BossDamage}).
 * <ul>
 *   <li><b>Seguras</b> (para quien peleó de verdad): el sello del jefe, que abre la caja siguiente, y el material de jefe para subir la Mesa.</li>
 *   <li><b>Con probabilidad</b>: trofeo, reliquia, mascota, material de mejora, materiales extra y Esencia. La probabilidad
 *       de cada una es {@code base × (30 % + 70 % × aporte)}, donde el aporte es tu daño entre el del que más daño hizo. Las bases
 *       son menores que 1 (nada está asegurado) y el tope con todo es 85 %. Trofeo, reliquia y mascota tienen además protección
 *       contra la mala suerte: +8 % por cada vez seguida que no te tocó (máx. +32 %).</li>
 * </ul>
 */
public final class BossRewards {
    private BossRewards() {
    }

    /** Mínimo para contar como participante: 5 % de la vida del jefe o 8 % del daño total. */
    private static final float MIN_OF_HP = 0.05F, MIN_OF_TOTAL = 0.08F;
    /**
     * Con poco daño tu probabilidad es el 30 % de la base; con el máximo de daño del grupo, la base completa. Ningún extra está nunca
     * asegurado: la base ya es menor que 1 y, aun con la protección contra mala suerte, el tope es {@link #CAP}.
     */
    private static final double FLOOR = 0.30, PITY_STEP = 0.08, PITY_MAX = 0.32, CAP = 0.85;

    private record Spec(String trophy, double trophyP, String relic, double relicP, String pet, double petP,
                        Supplier<Item> upgrade, int upMin, int upMax, double upP, Mats[] extra, double extraP, double essP, int essence) {
    }

    private static Spec spec(int stage) {
        return switch (stage) {
            case 1 -> new Spec("decor_trophy_forest", 0.50, "oak_heart", 0.30, "comp_sprout_king", 0.20,
                    ModItems.GOLDEN_ACORN::get, 1, 2, 0.60, new Mats[]{Mats.SHINY_JELLY, Mats.WOVEN_SILK, Mats.PEARL_KELP}, 0.65, 0.70, 150);
            case 2 -> new Spec("decor_trophy_forge", 0.50, "rage_gauntlet", 0.30, "comp_ember_anvil", 0.15,
                    ModItems.EMBER_STEEL::get, 5, 8, 0.60, new Mats[]{Mats.LIVE_EMBER, Mats.OLD_COIN, Mats.DARK_ASH}, 0.65, 0.70, 300);
            default -> null;
        };
    }

    /** ¿Ya existe el jefe de esa etapa? (para avisar en la Mesa de que ese nivel aún no se puede subir). */
    public static boolean exists(int stage) {
        return spec(stage) != null;
    }

    private static String idOf(Item it) {
        ResourceLocation k = ForgeRegistries.ITEMS.getKey(it);
        return k == null ? "" : k.toString();
    }

    private static int tagOf(double base) {
        return base >= 0.60 ? 1 : base >= 0.30 ? 2 : 3;
    }

    /**
     * Lista de premios de la etapa para la ficha del portal (se muestra oculta con «?» hasta que el jugador venza al jefe):
     * primero los seguros y luego los que dependen de la suerte y del daño que hagas.
     */
    public static List<BossBriefPacket.Reward> preview(int stage) {
        List<BossBriefPacket.Reward> out = new ArrayList<>();
        if (stage < 1 || stage >= CrateKind.values().length) {
            return out;
        }
        out.add(new BossBriefPacket.Reward(idOf(ModItems.SEALS.get(CrateKind.byIndex(stage)).get()), "", 0));
        Mats bm = Mats.ofBoss(stage);
        if (bm != null) {
            out.add(new BossBriefPacket.Reward(idOf(ModItems.MATS.get(bm).get()), "", 0));
        }
        Spec sp = spec(stage);
        if (sp != null) {
            Item trophy = byName(sp.trophy);
            if (trophy != null) {
                out.add(new BossBriefPacket.Reward(idOf(trophy), "", tagOf(sp.trophyP)));
            }
            Item relic = byName(sp.relic);
            if (relic != null) {
                out.add(new BossBriefPacket.Reward(idOf(relic), "", tagOf(sp.relicP)));
            }
            Item pet = byName(sp.pet);
            if (pet != null) {
                out.add(new BossBriefPacket.Reward(idOf(pet), "", tagOf(sp.petP)));
            }
            out.add(new BossBriefPacket.Reward(idOf(sp.upgrade.get()), sp.upMin == sp.upMax ? sp.upMin + "×" : sp.upMin + "–" + sp.upMax + "×", tagOf(sp.upP)));
            out.add(new BossBriefPacket.Reward(idOf(ModItems.MATS.get(sp.extra[0]).get()), "3–6×", tagOf(sp.extraP)));
            out.add(new BossBriefPacket.Reward("", "◆" + sp.essence, tagOf(sp.essP)));
        }
        return out;
    }

    // ------------------------------------------------------------------ aviso único

    /** Jugadores que ya recibieron su pantalla de resultados (tick del servidor): no se les repite lo mismo en el chat. */
    private static final Map<UUID, Integer> RESULT_AT = new HashMap<>();

    /** ¿Acaba este jugador de recibir la pantalla de resultados de un jefe? (los avisos del mundo y del Archivo ya van ahí). */
    public static boolean hadResult(ServerPlayer p) {
        Integer t = RESULT_AT.get(p.getUUID());
        return t != null && p.server.getTickCount() - t < 200;
    }

    /** Manda la pantalla de resultados unos segundos después de la victoria, para que se vea antes el título y los efectos. */
    private static void sendResult(ServerPlayer p, BossResultPacket pk) {
        Scheduler.runLater(70, () -> {
            if (p.isAlive() && !p.hasDisconnected()) {
                ModNetwork.sendTo(p, pk);
            }
        });
    }

    /** Porcentaje que se enseña como «probabilidad de extras»: la media de lo que de verdad te toca en trofeo, mejora, materiales y Esencia. */
    private static int shownChance(Spec sp, double f) {
        if (sp == null) {
            return 0;
        }
        double avg = (sp.trophyP + sp.upP + sp.extraP + sp.essP) / 4.0 * f;
        return (int) Math.round(100.0 * Math.min(CAP, avg));
    }

    private static Item byName(String id) {
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(MundosDeAxelis.MODID, id));
    }

    private static void give(ServerPlayer p, ItemStack st) {
        if (!p.getInventory().add(st.copy())) {
            p.drop(st.copy(), false);
        }
    }

    /** Probabilidad (en %) que tienes ahora mismo en esa tirada, la misma que usa {@link #roll}. */
    private static int chancePct(ServerPlayer p, String key, double base, double f, boolean pity) {
        int misses = pity ? Essence.bossPity(p, key) : 0;
        return (int) Math.round(100.0 * Math.min(CAP, base * f + (pity ? Math.min(PITY_MAX, PITY_STEP * misses) : 0.0)));
    }

    private static boolean roll(ServerPlayer p, RandomSource rnd, String key, double base, double f, boolean pity) {
        int misses = pity ? Essence.bossPity(p, key) : 0;
        double chance = Math.min(CAP, base * f + (pity ? Math.min(PITY_MAX, PITY_STEP * misses) : 0.0));
        boolean ok = rnd.nextDouble() < chance;
        if (pity) {
            Essence.setBossPity(p, key, ok ? 0 : misses + 1);
        }
        return ok;
    }

    /**
     * Reparte las recompensas. {@code present}: jugadores cerca del jefe al morir. Manda el reparto final a su panel de daño.
     */
    public static void distribute(ServerLevel sl, LivingEntity boss, int stage, List<ServerPlayer> nearby) {
        BossDamage.Fight fight = BossDamage.take(boss);
        // también cuentan los que pelearon y siguen en la dimensión aunque ahora estén lejos del jefe
        List<ServerPlayer> present = new ArrayList<>(nearby);
        for (ServerPlayer q : sl.players()) {
            if (!present.contains(q) && q.isAlive() && fight.dmg.getOrDefault(q.getUUID(), 0.0F) > 0.0F) {
                present.add(q);
            }
        }
        float total = fight.total;
        float maxHp = boss.getMaxHealth();
        float top = 0.0F;
        for (ServerPlayer p : present) {
            top = Math.max(top, fight.dmg.getOrDefault(p.getUUID(), 0.0F));
        }
        Spec sp = spec(stage);
        RandomSource rnd = sl.random;
        Map<UUID, Integer> chances = new HashMap<>();
        for (ServerPlayer p : present) {
            float d = fight.dmg.getOrDefault(p.getUUID(), 0.0F);
            boolean eligible = total <= 0.0F || d >= MIN_OF_HP * maxHp || d >= MIN_OF_TOTAL * total;
            double r = top > 0.0F ? Math.min(1.0, d / top) : 1.0;
            double f = FLOOR + (1.0 - FLOOR) * r;
            int pct = total <= 0.0F ? 100 : Math.round(100.0F * d / total);
            int shownPct = eligible ? shownChance(sp, f) : 0;
            chances.put(p.getUUID(), shownPct);
            fight.names.putIfAbsent(p.getUUID(), p.getName().getString());
            fight.dmg.putIfAbsent(p.getUUID(), 0.0F);
            // lo que se avisaba por chat (nivel del mundo y avance del Archivo) ahora va en la pantalla de resultados
            boolean archiveNew = !Essence.archive(p).contains("seal_" + CrateKind.byIndex(stage).id);
            int tierLevel = stage > WorldTier.get(sl.getServer()).level() ? stage : 0;
            int tierPct = (int) Math.round(WorldTierEvents.HP_PER_LEVEL * stage * 100);
            String bossTitle = BossDamage.bossName(fight);
            Essence.setBossBeaten(p, stage);
            RESULT_AT.put(p.getUUID(), sl.getServer().getTickCount());
            if (!eligible) {
                sendResult(p, new BossResultPacket(stage, bossTitle, false, pct, 0, new ArrayList<>(), tierLevel, tierPct, archiveNew));
                continue;
            }
            List<BossResultPacket.Got> got = new ArrayList<>();
            List<BossResultPacket.Miss> missed = new ArrayList<>();
            // ---- seguras
            ItemStack seal = new ItemStack(ModItems.SEALS.get(CrateKind.byIndex(stage)).get());
            give(p, seal);
            got.add(new BossResultPacket.Got(idOf(seal.getItem()), 1, true));
            Mats bm = Mats.ofBoss(stage);
            if (bm != null) {
                ItemStack mat = new ItemStack(ModItems.MATS.get(bm).get());
                give(p, mat);
                got.add(new BossResultPacket.Got(idOf(mat.getItem()), 1, true));
            }
            // ---- con probabilidad
            if (sp != null) {
                Item trophy = byName(sp.trophy);
                int trophyPct = chancePct(p, "trophy" + stage, sp.trophyP, f, true);
                if (trophy != null && roll(p, rnd, "trophy" + stage, sp.trophyP, f, true)) {
                    ItemStack st = new ItemStack(trophy);
                    give(p, st);
                    got.add(new BossResultPacket.Got(idOf(st.getItem()), 1, false));
                } else if (trophy != null) {
                    missed.add(new BossResultPacket.Miss(idOf(trophy), 0, trophyPct));
                }
                Item relic = byName(sp.relic);
                int relicPct = chancePct(p, "relic" + stage, sp.relicP, f, true);
                if (relic != null && roll(p, rnd, "relic" + stage, sp.relicP, f, true)) {
                    ItemStack st = new ItemStack(relic);
                    give(p, st);
                    got.add(new BossResultPacket.Got(idOf(st.getItem()), 1, false));
                } else if (relic != null) {
                    missed.add(new BossResultPacket.Miss(idOf(relic), 0, relicPct));
                }
                Item pet = byName(sp.pet);
                int petPct = chancePct(p, "pet" + stage, sp.petP, f, true);
                if (pet != null && roll(p, rnd, "pet" + stage, sp.petP, f, true)) {
                    ItemStack st = new ItemStack(pet);
                    give(p, st);
                    got.add(new BossResultPacket.Got(idOf(st.getItem()), 1, false));
                } else if (pet != null) {
                    missed.add(new BossResultPacket.Miss(idOf(pet), 0, petPct));
                }
                int upPct = chancePct(p, "up" + stage, sp.upP, f, false);
                if (roll(p, rnd, "up" + stage, sp.upP, f, false)) {
                    ItemStack st = new ItemStack(sp.upgrade.get(), sp.upMin + rnd.nextInt(sp.upMax - sp.upMin + 1));
                    give(p, st);
                    got.add(new BossResultPacket.Got(idOf(st.getItem()), st.getCount(), false));
                } else {
                    missed.add(new BossResultPacket.Miss(idOf(sp.upgrade.get()), 0, upPct));
                }
                int matsPct = chancePct(p, "mats" + stage, sp.extraP, f, false);
                if (roll(p, rnd, "mats" + stage, sp.extraP, f, false)) {
                    Mats a = sp.extra[rnd.nextInt(sp.extra.length)];
                    ItemStack st = new ItemStack(ModItems.MATS.get(a).get(), 3 + rnd.nextInt(4));
                    give(p, st);
                    got.add(new BossResultPacket.Got(idOf(st.getItem()), st.getCount(), false));
                } else {
                    missed.add(new BossResultPacket.Miss(idOf(ModItems.MATS.get(sp.extra[0]).get()), 2, matsPct));
                }
                int essPct = chancePct(p, "ess" + stage, sp.essP, f, false);
                if (roll(p, rnd, "ess" + stage, sp.essP, f, false)) {
                    int gain = Math.round(sp.essence * (0.8F + rnd.nextFloat() * 0.4F));
                    Essence.add(p, gain);
                    Essence.sync(p, net.minecraft.network.chat.Component.translatable("message.mundos_de_axelis.boss_essence", gain), true);
                    got.add(new BossResultPacket.Got("", gain, false));
                } else {
                    missed.add(new BossResultPacket.Miss("", 1, essPct));
                }
            }
            sendResult(p, new BossResultPacket(stage, bossTitle, true, pct, shownPct, got, tierLevel, tierPct, archiveNew, missed));
            sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        BossDamagePacket pk = new BossDamagePacket(BossDamage.bossName(fight), true, BossDamage.entries(fight, chances));
        for (ServerPlayer p : sl.players()) {
            ModNetwork.sendTo(p, pk);
        }
    }
}
