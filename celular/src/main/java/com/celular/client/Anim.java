package com.celular.client;

import com.celular.plat.client.G;

import java.util.HashMap;
import java.util.Map;

/**
 * Animaciones suaves de la interfaz: un valor que se acerca poco a poco a su objetivo (interruptores, pestañas, números), y el
 * dibujo de un interruptor animado.
 */
final class Anim {
    private static final class Val {
        float v;
        long t;
    }

    private static final Map<Object, Val> VALS = new HashMap<>();

    private Anim() {
    }

    /** Se acerca a {@code target} con una velocidad {@code rate} (más alto = más rápido; 14 se ve natural). */
    static float ease(Object key, float target, float rate) {
        long now = System.nanoTime();
        Val v = VALS.get(key);
        if (v == null) {
            if (VALS.size() > 400) {
                VALS.clear();
            }
            v = new Val();
            v.v = target;
            v.t = now;
            VALS.put(key, v);
            return target;
        }
        float dt = Math.min(0.1F, (now - v.t) / 1e9F);
        v.t = now;
        v.v += (target - v.v) * (1 - (float) Math.exp(-rate * dt));
        if (Math.abs(target - v.v) < 0.001F * Math.max(1F, Math.abs(target))) {
            v.v = target;
        }
        return v.v;
    }

    /** Interruptor (20 × 11) con la bolita que se desliza y el color que cambia. */
    static void toggle(G g, int sx, int sy, boolean on, Object key) {
        float k = ease(key, on ? 1F : 0F, 16F);
        int off = Prefs.dark ? 0xFF39393D : 0xFFD1D1D6;
        D.rr(g, sx, sy, sx + 20, sy + 11, 5, D.mix(off, 0xFF34C759, k));
        D.circle(g, sx + 6 + Math.round(8 * k), sy + 5, 4, 0xFFFFFFFF);
    }
}
