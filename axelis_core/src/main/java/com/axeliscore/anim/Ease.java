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

    /**
     * La curva con un ajuste (como "easingArgs" de GeckoLib/Blockbench): en las "back" cuánto se pasa (1.70158 normal), en las
     * "elastic" y "bounce" cuánto rebota, en "step" cuántos escalones. Sin ajuste, igual que {@link #get(String)}.
     */
    public static Fn get(String name, float arg) {
        if (name == null || Float.isNaN(arg)) {
            return get(name);
        }
        String n = name.toLowerCase(Locale.ROOT).replace("_", "");
        Fn in;
        if (n.equals("step")) {
            float steps = Math.max(1, Math.round(arg));
            return t -> t >= 1 ? 1 : (float) Math.floor(t * steps) / steps;
        } else if (n.endsWith("back")) {
            float s = arg;
            in = t -> (s + 1) * t * t * t - s * t * t;
        } else if (n.endsWith("elastic")) {
            float b = Math.max(0.1F, arg);
            in = t -> t == 0 || t == 1 ? t : (float) (-Math.pow(2, 10 * t - 10) * Math.sin((t * 10 - 10.75) * (2 * Math.PI / (3 * b))));
        } else if (n.endsWith("bounce")) {
            float b = Math.max(0.1F, arg);
            in = t -> 1 - outBounceK(1 - t, b);
        } else {
            return get(name);
        }
        if (n.startsWith("easeinout")) {
            Fn f = in;
            return t -> t < 0.5F ? f.apply(t * 2) / 2 : 1 - f.apply((1 - t) * 2) / 2;
        }
        if (n.startsWith("easeout")) {
            Fn f = in;
            return t -> 1 - f.apply(1 - t);
        }
        return in;
    }

    /** Rebote con fuerza ajustable (k = 1 es el normal; más alto, botes más altos). */
    private static float outBounceK(float t, float k) {
        float base = outBounce(t);
        // el rebote normal llega a 1 en cada bote; k cambia la profundidad de los valles
        return 1 - (1 - base) * k;
    }

    /**
     * Curva de Bézier por tiempo (la de Blockbench con manijas): {@code x1, y1, x2, y2} son las manijas en 0..1 (como CSS
     * cubic-bezier). Devuelve cuánto se avanzó en el tiempo t.
     */
    public static Fn bezier(float x1, float y1, float x2, float y2) {
        return t -> {
            float s = solveBezier(t, x1, x2);
            return bez(s, y1, y2);
        };
    }

    static float bez(float s, float p1, float p2) {
        float u = 1 - s;
        return 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s;
    }

    /** Busca s (0..1) tal que la x de la curva sea t (Newton y, si no converge, bisección). */
    static float solveBezier(float t, float x1, float x2) {
        if (t <= 0) {
            return 0;
        }
        if (t >= 1) {
            return 1;
        }
        float s = t;
        for (int i = 0; i < 8; i++) {
            float x = bez(s, x1, x2) - t;
            float u = 1 - s;
            float dx = 3 * u * u * x1 + 6 * u * s * (x2 - x1) + 3 * s * s * (1 - x2);
            if (Math.abs(x) < 1e-5F) {
                return s;
            }
            if (Math.abs(dx) < 1e-6F) {
                break;
            }
            s -= x / dx;
            if (s < 0 || s > 1) {
                break;
            }
        }
        float lo = 0, hi = 1;
        s = t;
        for (int i = 0; i < 30; i++) {
            float x = bez(s, x1, x2);
            if (Math.abs(x - t) < 1e-5F) {
                break;
            }
            if (x < t) {
                lo = s;
            } else {
                hi = s;
            }
            s = (lo + hi) / 2;
        }
        return s;
    }

    /** Suave de entrada y salida (la más usada para movimientos naturales). */
    public static float smooth(float t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }
}
