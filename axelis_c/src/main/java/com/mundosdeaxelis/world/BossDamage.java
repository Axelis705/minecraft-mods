package com.mundosdeaxelis.world;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GranHerrero;
import com.mundosdeaxelis.entity.ReyBrote;
import com.mundosdeaxelis.network.BossDamagePacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cuenta cuánto daño hace cada jugador a un jefe de dungeon. Sirve para el panel en vivo y para repartir las recompensas
 * ({@link BossRewards}). Cuenta el daño propio, el de proyectiles y el de mascotas del jugador; el de los esbirros del
 * jefe no cuenta, y lo que sobra de un golpe final tampoco (como mucho cuenta la vida que le quedaba).
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID)
public final class BossDamage {
    private BossDamage() {
    }

    /** Daño acumulado de una pelea. */
    public static final class Fight {
        public final Map<UUID, Float> dmg = new LinkedHashMap<>();
        public final Map<UUID, String> names = new HashMap<>();
        public float total;
        private boolean dirty;
        private LivingEntity boss;
        private String bossName = "";
    }

    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();

    /** Etapa del jefe de dungeon (1 a 8), o 0 si la entidad no es uno. */
    public static int stageOf(Entity e) {
        if (e instanceof ReyBrote) {
            return 1;
        }
        if (e instanceof GranHerrero) {
            return 2;
        }
        if (e instanceof com.mundosdeaxelis.entity.LoboArtico) {
            return 3;
        }
        return 0;
    }

    private static ServerPlayer attacker(DamageSource src) {
        Entity a = src.getEntity();
        if (a instanceof ServerPlayer sp) {
            return sp;
        }
        if (a instanceof TamableAnimal t && t.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        Entity d = src.getDirectEntity();
        if (d instanceof Projectile p && p.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent e) {
        LivingEntity t = e.getEntity();
        if (t.level().isClientSide || stageOf(t) == 0) {
            return;
        }
        ServerPlayer sp = attacker(e.getSource());
        if (sp == null) {
            return;
        }
        float amt = Math.min(e.getAmount(), t.getHealth());
        if (amt <= 0.0F) {
            return;
        }
        Fight f = FIGHTS.computeIfAbsent(t.getUUID(), k -> new Fight());
        f.boss = t;
        f.bossName = t.getName().getString();
        f.dmg.merge(sp.getUUID(), amt, Float::sum);
        f.names.put(sp.getUUID(), sp.getName().getString());
        f.total += amt;
        f.dirty = true;
    }

    /** Cada medio segundo manda el reparto a los jugadores de la dimensión del jefe (solo si cambió). */
    @SubscribeEvent
    public static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || FIGHTS.isEmpty() || e.getServer().getTickCount() % 10 != 0) {
            return;
        }
        Iterator<Map.Entry<UUID, Fight>> it = FIGHTS.entrySet().iterator();
        while (it.hasNext()) {
            Fight f = it.next().getValue();
            if (f.boss == null || f.boss.isRemoved() && !f.boss.isDeadOrDying()) {
                it.remove(); // el jefe desapareció sin morir
                continue;
            }
            if (f.dirty && f.boss.isAlive() && f.boss.level() instanceof ServerLevel sl) {
                f.dirty = false;
                BossDamagePacket pk = new BossDamagePacket(f.bossName, false, entries(f, null));
                for (ServerPlayer p : sl.players()) {
                    ModNetwork.sendTo(p, pk);
                }
            }
        }
    }

    /** Reparto ordenado de mayor a menor. {@code chances} (opcional) pone la probabilidad de extras de cada jugador. */
    public static List<BossDamagePacket.Entry> entries(Fight f, Map<UUID, Integer> chances) {
        List<Map.Entry<UUID, Float>> l = new ArrayList<>(f.dmg.entrySet());
        l.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));
        List<BossDamagePacket.Entry> out = new ArrayList<>();
        for (Map.Entry<UUID, Float> en : l) {
            int pct = f.total <= 0 ? 0 : Math.round(100.0F * en.getValue() / f.total);
            int ch = chances == null ? -1 : chances.getOrDefault(en.getKey(), 0);
            out.add(new BossDamagePacket.Entry(f.names.getOrDefault(en.getKey(), "?"), pct, ch));
        }
        return out;
    }

    /** Termina la pelea de ese jefe y devuelve su reparto (nunca null; vacío si nadie le hizo daño). */
    public static Fight take(LivingEntity boss) {
        Fight f = FIGHTS.remove(boss.getUUID());
        if (f == null) {
            f = new Fight();
        }
        f.bossName = boss.getName().getString();
        return f;
    }

    public static String bossName(Fight f) {
        return f.bossName;
    }
}
