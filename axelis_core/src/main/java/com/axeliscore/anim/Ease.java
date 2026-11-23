package com.axeliscore.anim;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Curvas de suavizado para pasar de un cuadro clave al siguiente (los mismos nombres que usa GeckoLib/Blockbench en la clave
 * "easing" de un cuadro). Cada curva recibe t de 0 a 1 y devuelve cuánto se avanzó (puede pasarse de 1 un poco en las de rebote).
 */
public final class Ease {
    public interface Fn {
        float apply(float t);
    }

    public static final Fn LINEAR = t -> t;
    private static final Map<String, Fn> BY_NAME = new HashMap<>();

    private Ease() {
    }

    private static float outBounce(float t) {
        float n = 7.5625F, d = 2.75F;
        if (t < 1 / d) {
            return n * t * t;
        } else if (t < 2 / d) {
            t -= 1.5F / d;
            return n * t * t + 0.75F;
        } else if (t < 2.5 / d) {
            t -= 2.25F / d;
            return n * t * t + 0.9375F;
        }
        t -= 2.625F / d;
        return n * t * t + 0.984375F;
    }

    private static void add(String base, Fn in) {
        Fn out = t -> 1 - in.apply(1 - t);
        Fn inOut = t -> t < 0.5F ? in.apply(t * 2) / 2 : 1 - in.apply((1 - t) * 2) / 2;
        BY_NAME.put("easein" + base, in);
        BY_NAME.put("easeout" + base, out);
        BY_NAME.put("easeinout" + base, inOut);
    }

    static {
        BY_NAME.put("linear", LINEAR);
        BY_NAME.put("step", t -> t < 1 ? 0 : 1);
        add("sine", t -> 1 - (float) Math.cos(t * Math.PI / 2));
        add("quad", t -> t * t);
        add("cubic", t -> t * t * t);
        add("quart", t -> t * t * t * t);
        add("quint", t -> t * t * t * t * t);
        add("expo", t -> t == 0 ? 0 : (float) Math.pow(2, 10 * t - 10));
        add("circ", t -> 1 - (float) Math.sqrt(Math.max(0, 1 - t * t)));
        add("back", t -> 2.70158F * t * t * t - 1.70158F * t * t);
        add("elastic", t -> t == 0 || t == 1 ? t : (float) (-Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * (2 * Math.PI / 3))));
        add("bounce", t -> 1 - outBounce(1 - t));
    }

    /** La curva por nombre ("easeInOutSine", "easeOutBack", "linear"...). Si no se conoce, lineal. */
    public static Fn get(String name) {
        if (name == null) {
            return LINEAR;
        }
        return BY_NAME.getOrDefault(name.toLowerCase(Locale.ROOT).replace("_", ""), LINEAR);
    }

    /** Suave de entrada y salida (la más usada para movimientos naturales). */
    public static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
