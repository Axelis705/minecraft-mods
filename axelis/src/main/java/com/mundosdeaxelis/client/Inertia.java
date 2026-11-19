package com.mundosdeaxelis.client;

import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Transiciones suaves entre estados de un jefe (inercia): cuando cambia de estado, guarda la diferencia entre la última pose
 * dibujada y la nueva, y la va apagando poco a poco. Así ninguna pieza "salta" de golpe de una pose a otra, pero cada
 * animación conserva su ritmo (no se retrasa como con un suavizado normal).
 */
public final class Inertia {
    private static final Map<LivingEntity, S> ALL = new WeakHashMap<>();

    private static final class S {
        int state = Integer.MIN_VALUE;
        float[] last;
        float[] off;
        float t0;
    }

    private Inertia() {
    }

    /**
     * Mezcla la pose {@code pose} (se modifica) con la anterior durante {@code ticks} ticks tras cada cambio de {@code state}.
     * Cada modelo usa siempre el mismo orden de valores.
     */
    public static void blend(LivingEntity e, int state, float[] pose, float ticks, float pt) {
        S s = ALL.computeIfAbsent(e, k -> new S());
        float now = e.tickCount + pt;
        if (s.last == null || s.last.length != pose.length) {
            s.last = pose.clone();
            s.off = new float[pose.length];
            s.state = state;
            s.t0 = now - ticks;
            return;
        }
        if (state != s.state) {
            for (int i = 0; i < pose.length; i++) {
                s.off[i] = s.last[i] - pose[i];
            }
            s.t0 = now;
            s.state = state;
        }
        float k = (now - s.t0) / ticks;
        if (k < 1.0F) {
            float w = (1.0F - Math.max(0.0F, k));
            w *= w;
            for (int i = 0; i < pose.length; i++) {
                pose[i] += s.off[i] * w;
            }
        }
        System.arraycopy(pose, 0, s.last, 0, pose.length);
    }
}
