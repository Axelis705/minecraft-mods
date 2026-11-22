package com.mundosdeaxelis.encargo;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.block.VaultLoot;
import com.mundosdeaxelis.registry.ModItems;
import com.mundosdeaxelis.table.Essence;
import com.mundosdeaxelis.world.Elites;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 280: los <b>encargos</b> del Tablón (como los «susurros» de Diablo 4). Cada día del juego, cada jugador tiene 3 encargos
 * distintos (vencer élites, abrir cofres de santuario, vencer un mímico, un Altar Maldito…). Al cumplirlos se recogen en el Tablón
 * de Encargos: Esencia y una recompensa de su tier; si recoges los 3, hay una gran recompensa. Al día siguiente salen otros 3.
 * Los datos van en la parte de los datos del jugador que sobrevive a la muerte.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class Encargos {
    private Encargos() {
    }

    public enum Tipo {
        ENEMIGOS("enemigos", 15, 25), ELITES("elites", 2, 3), COFRES("cofres", 4, 6), MIMICO("mimico", 1, 1), ALTAR("altar", 1, 1),
        ARROJADIZOS("arrojadizos", 4, 6), GEMAS("gemas", 1, 1), FORJA("forja", 1, 1);

        public final String id;
        public final int min, max;

        Tipo(String id, int min, int max) {
            this.id = id;
            this.min = min;
            this.max = max;
        }

        public static Tipo of(int ord) {
            Tipo[] v = values();
            return v[Math.max(0, Math.min(v.length - 1, ord))];
        }
    }

    public static final int COUNT = 3;
    private static final String KEY = "mdc_encargos";

    /** Un encargo: qué es, cuántos hacen falta, cuántos llevas y si ya lo recogiste. */
    public record Encargo(Tipo tipo, int need, int progress, boolean claimed) {
        public boolean done() {
            return progress >= need;
        }
    }

    // ------------------------------------------------------------------ datos

    private static CompoundTag persisted(Player p) {
        return p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
    }

    private static void save(Player p, CompoundTag per) {
        p.getPersistentData().put(Player.PERSISTED_NBT_TAG, per);
    }

    /** Día del juego (cambia al amanecer de cada día del mundo principal). */
    public static long day(ServerPlayer sp) {
        return sp.server.overworld().getDayTime() / 24000L;
    }

    /** Tier de las recompensas: el siguiente al último jefe vencido (de 1 a 5). */
    public static int rewardTier(ServerPlayer sp) {
        return Math.max(1, Math.min(5, com.mundosdeaxelis.world.Sellos.playerStage(sp) + 1));
    }

    /** Los encargos de hoy (si es un día nuevo, salen otros 3). */
    public static List<Encargo> today(ServerPlayer sp) {
        CompoundTag per = persisted(sp);
        CompoundTag d = per.getCompound(KEY);
        long day = day(sp);
        if (!d.contains("Day") || d.getLong("Day") != day) {
            d = generate(sp, day);
            per.put(KEY, d);
            save(sp, per);
        }
        List<Encargo> out = new ArrayList<>();
        for (Tag t : d.getList("List", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            out.add(new Encargo(Tipo.of(c.getInt("T")), c.getInt("N"), c.getInt("P"), c.getBoolean("C")));
        }
        return out;
    }

    public static boolean bonusClaimed(ServerPlayer sp) {
        today(sp);
        return persisted(sp).getCompound(KEY).getBoolean("Bonus");
    }

    /** Tres tipos distintos al azar (el mismo día salen siempre los mismos para ese jugador). */
    static CompoundTag generate(ServerPlayer sp, long day) {
        RandomSource r = RandomSource.create(sp.getUUID().getLeastSignificantBits() ^ (day * 0x9E3779B97F4A7C15L));
        List<Tipo> pool = new ArrayList<>(List.of(Tipo.values()));
        CompoundTag d = new CompoundTag();
        d.putLong("Day", day);
        ListTag l = new ListTag();
        for (int i = 0; i < COUNT; i++) {
            Tipo t = pool.remove(r.nextInt(pool.size()));
            CompoundTag c = new CompoundTag();
            c.putInt("T", t.ordinal());
            c.putInt("N", t.min + r.nextInt(t.max - t.min + 1));
            c.putInt("P", 0);
            c.putBoolean("C", false);
            l.add(c);
        }
        d.put("List", l);
        d.putBoolean("Bonus", false);
        return d;
    }

    /** Para las pruebas: pone hoy esos encargos (con esos avances; -1 = cumplido) sin recoger. */
    public static void force(ServerPlayer sp, Tipo[] tipos, int[] progress) {
        CompoundTag per = persisted(sp);
        CompoundTag d = new CompoundTag();
        d.putLong("Day", day(sp));
        ListTag l = new ListTag();
        for (int i = 0; i < tipos.length; i++) {
            CompoundTag c = new CompoundTag();
            c.putInt("T", tipos[i].ordinal());
            c.putInt("N", tipos[i].min);
            c.putInt("P", progress[i] < 0 ? tipos[i].min : progress[i]);
            c.putBoolean("C", false);
            l.add(c);
        }
        d.put("List", l);
        d.putBoolean("Bonus", false);
        per.put(KEY, d);
        save(sp, per);
    }

    /** Para las pruebas: hace como si los encargos fueran de ayer (al mirarlos, salen otros). */
    public static void ageOneDay(ServerPlayer sp) {
        CompoundTag per = persisted(sp);
        CompoundTag d = per.getCompound(KEY);
        d.putLong("Day", d.getLong("Day") - 1);
        per.put(KEY, d);
        save(sp, per);
    }

    /** Suma avance a los encargos de ese tipo; avisa cuando se cumple uno. */
    public static void progress(ServerPlayer sp, Tipo t, int amount) {
        if (amount <= 0) {
            return;
        }
        today(sp);
        CompoundTag per = persisted(sp);
        CompoundTag d = per.getCompound(KEY);
        ListTag l = d.getList("List", Tag.TAG_COMPOUND);
        boolean changed = false;
        for (int i = 0; i < l.size(); i++) {
            CompoundTag c = l.getCompound(i);
            if (c.getInt("T") != t.ordinal() || c.getBoolean("C")) {
                continue;
            }
            int need = c.getInt("N"), before = c.getInt("P");
            if (before >= need) {
                continue;
            }
            int now = Math.min(need, before + amount);
            c.putInt("P", now);
            changed = true;
            if (now >= need) {
                sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.encargo.cumplido", describe(t, need))
                        .withStyle(ChatFormatting.GOLD), true);
                sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
            }
        }
        if (changed) {
            d.put("List", l);
            per.put(KEY, d);
            save(sp, per);
        }
    }

    public static Component describe(Tipo t, int need) {
        return Component.translatable("encargo.mundos_de_axelis." + t.id, need);
    }

    // ------------------------------------------------------------------ recompensas

    /** La recompensa de un encargo de ese tipo y tier (además de la Esencia). */
    public static List<ItemStack> reward(Tipo t, int tier, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        int T = Math.max(1, Math.min(5, tier));
        switch (t) {
            case ENEMIGOS -> out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(T).get(), 2));
            case ELITES -> {
                out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(T)).get()));
                out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(T).get()));
            }
            case COFRES, FORJA -> out.add(new ItemStack(ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.byIndex(T)).get(), 2));
            case MIMICO -> {
                out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(T)).get()));
                out.add(new ItemStack(ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.byIndex(T)).get()));
            }
            case ALTAR -> out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(T)).get(), 2));
            // etapa 282: «arrojadizos» pasó a ser «estallidos» (golpes cargados de las armas que estallan)
            case ARROJADIZOS -> {
                out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(T).get()));
                out.add(new ItemStack(ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.ofTier(T)).get()));
            }
            case GEMAS -> out.add(VaultLoot.emptyGem(T));
        }
        return out;
    }

    public static int essence(int tier) {
        return 40 + 20 * Math.max(1, Math.min(5, tier));
    }

    /** La gran recompensa por recoger los 3: un fragmento de corazón o una gema vacía, y Esencia. */
    public static List<ItemStack> bonus(int tier, RandomSource r) {
        List<ItemStack> out = new ArrayList<>();
        out.add(r.nextBoolean() ? new ItemStack(ModItems.HEART_FRAGMENT.get()) : VaultLoot.emptyGem(tier));
        out.add(new ItemStack(ModItems.ESSENCE_RUNES.get(Math.max(1, Math.min(5, tier))).get(), 2));
        return out;
    }

    /** Recoger el encargo i (0..2) o, con i = 3, la gran recompensa. Devuelve true si se entregó algo. */
    public static boolean claim(ServerPlayer sp, int i) {
        List<Encargo> list = today(sp);
        CompoundTag per = persisted(sp);
        CompoundTag d = per.getCompound(KEY);
        int tier = rewardTier(sp);
        List<ItemStack> give;
        int ess;
        if (i == COUNT) {
            boolean all = list.stream().allMatch(Encargo::claimed);
            if (!all || d.getBoolean("Bonus")) {
                return false;
            }
            d.putBoolean("Bonus", true);
            give = bonus(tier, sp.getRandom());
            ess = 100 * tier;
        } else {
            if (i < 0 || i >= list.size() || !list.get(i).done() || list.get(i).claimed()) {
                return false;
            }
            ListTag l = d.getList("List", Tag.TAG_COMPOUND);
            l.getCompound(i).putBoolean("C", true);
            d.put("List", l);
            give = reward(list.get(i).tipo(), tier, sp.getRandom());
            ess = essence(tier);
        }
        per.put(KEY, d);
        save(sp, per);
        for (ItemStack it : give) {
            if (!sp.getInventory().add(it)) {
                sp.drop(it, false);
            }
        }
        Essence.add(sp, ess);
        Essence.sync(sp);
        sp.serverLevel().playSound(null, sp.blockPosition(), i == COUNT ? SoundEvents.UI_TOAST_CHALLENGE_COMPLETE : SoundEvents.VILLAGER_YES,
                SoundSource.PLAYERS, 0.8F, i == COUNT ? 1.0F : 1.3F);
        sp.displayClientMessage(Component.translatable(i == COUNT ? "message.mundos_de_axelis.encargo.gran_recompensa"
                : "message.mundos_de_axelis.encargo.recogido", ess).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    // ------------------------------------------------------------------ avance automático por muertes

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (le.level().isClientSide || !(e.getSource().getEntity() instanceof ServerPlayer sp)) {
            return;
        }
        if (le instanceof com.mundosdeaxelis.entity.Mimico) {
            progress(sp, Tipo.MIMICO, 1);
        }
        if (Elites.eligibleTier(le) > 0) {
            progress(sp, Tipo.ENEMIGOS, 1);
        }
        if (Elites.isElite(le)) {
            progress(sp, Tipo.ELITES, 1);
        }
    }
}
