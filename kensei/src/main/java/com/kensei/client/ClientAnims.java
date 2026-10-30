package com.kensei.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Qué movimiento está haciendo cada entidad que se ve (tajo del combo, crítico o esquive) y desde qué tick del mundo. Llega del
 * servidor para los demás jugadores y se anota al instante para el jugador local. Solo se usa en el cliente.
 */
public final class ClientAnims {
    public static final int HEAVY = 5;
    public static final int DODGE = 6;
    public static final int THRUST = 7;

    public static final class Anim {
        public int move;
        public long start;
        public float arg;
    }

    private static final Map<Integer, Anim> MAP = new HashMap<>();

    private ClientAnims() {
    }

    private static long now() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : mc.level.getGameTime();
    }

    public static void receive(int entityId, int move, float arg) {
        set(entityId, move, arg);
    }

    public static void set(int entityId, int move, float arg) {
        Anim a = MAP.computeIfAbsent(entityId, k -> new Anim());
        // el mismo movimiento anotado dos veces seguidas (predicción local + aviso del servidor): los efectos salen una sola vez
        boolean repeat = a.move == move && now() - a.start <= 2 && a.start > 0;
        if (!repeat) {
            Vfx.onAnim(entityId, move, arg);
        }
        a.move = move;
        a.start = now();
        a.arg = arg;
    }

    /** Cuántos ticks lleva este movimiento (con el decimal del cuadro actual). */
    public static float age(Anim a, float partial) {
        return (now() - a.start) + partial;
    }

    /** El movimiento de esta entidad si todavía está vigente (dura como mucho {@code maxTicks}), o null. */
    public static Anim get(Entity e, int maxTicks) {
        Anim a = MAP.get(e.getId());
        if (a == null) {
            return null;
        }
        return (now() - a.start) <= maxTicks ? a : null;
    }

    /** Recorre los movimientos vigentes (id de la entidad y su movimiento). */
    public static void forEachActive(java.util.function.BiConsumer<Integer, Anim> action) {
        for (Map.Entry<Integer, Anim> en : MAP.entrySet()) {
            if (now() - en.getValue().start <= 40) {
                action.accept(en.getKey(), en.getValue());
            }
        }
    }

    /** Limpieza: se tiran los movimientos viejos. */
    public static void prune() {
        long n = now();
        Iterator<Map.Entry<Integer, Anim>> it = MAP.entrySet().iterator();
        while (it.hasNext()) {
            if (n - it.next().getValue().start > 200) {
                it.remove();
            }
        }
    }

    public static void clear() {
        MAP.clear();
    }
}
