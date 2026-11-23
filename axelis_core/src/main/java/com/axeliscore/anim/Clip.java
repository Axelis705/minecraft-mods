package com.axeliscore.anim;

import java.util.Map;

/**
 * Una animación (se carga de un .animation.json de Blockbench con {@link ClipLoader}): para cada hueso, cuadros clave de giro,
 * posición y escala, más eventos (sonidos, partículas, avisos) en momentos concretos.
 * <p>
 * Los valores se guardan como en Blockbench (giro en grados, posición en píxeles con la y hacia arriba) y se pasan al espacio de Java al
 * muestrear: giro en radianes con el mismo signo, posición con la y al revés.
 */
public final class Clip {
    public enum Loop {
        /** Una vez y vuelve a la nada. */
        ONCE,
        /** Se repite. */
        LOOP,
        /** Una vez y se queda en el último cuadro. */
        HOLD
    }

    public enum Lerp {
        LINEAR, CATMULLROM, STEP,
        /** Curva con manijas (como en el editor de gráficas de Blockbench): ver {@link Key#bezier}. */
        BEZIER
    }

    public final String name;
    public final float length;
    public final Loop loop;
    public final Track[] tracks;
    /** Eventos ordenados por tiempo. */
    public final Event[] events;

    public Clip(String name, float length, Loop loop, Track[] tracks, Event[] events) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.tracks = tracks;
        this.events = events;
    }

    public static final class Track {
        public final String bone;
        /** null si ese canal no se anima. */
        public final Channel rot, pos, scl;

        public Track(String bone, Channel rot, Channel pos, Channel scl) {
            this.bone = bone;
            this.rot = rot;
            this.pos = pos;
            this.scl = scl;
        }
    }

    public static final class Channel {
        public final Key[] keys;

        public Channel(Key[] keys) {
            this.keys = keys;
        }

        /** Valor (x, y, z) en el tiempo t, en las unidades de Blockbench. */
        public void sample(float t, Expr.Ctx c, float[] out) {
            Key[] k = keys;
            if (k.length == 1 || t <= k[0].t) {
                k[0].value(k[0].pre, c, out);
                return;
            }
            int n = k.length;
            if (t >= k[n - 1].t) {
                k[n - 1].value(k[n - 1].post, c, out);
                return;
            }
            int i = 0;
            while (i < n - 2 && t >= k[i + 1].t) {
                i++;
            }
            Key a = k[i], b = k[i + 1];
            float span = b.t - a.t;
            float f = span <= 0 ? 1 : (t - a.t) / span;
            if (a.lerp == Lerp.STEP) {
                a.value(a.post, c, out);
                return;
            }
            float[] va = new float[3], vb = new float[3];
            a.value(a.post, c, va);
            b.value(b.pre, c, vb);
            if (a.lerp == Lerp.BEZIER || b.lerp == Lerp.BEZIER) {
                for (int j = 0; j < 3; j++) {
                    // manija derecha de a y manija izquierda de b (en segundos y en valor, relativas a cada cuadro)
                    // un cuadro sin manijas usa las de Blockbench por defecto (0.1 s, sin cambiar el valor)
                    float rt = a.bezier != null ? Math.max(0, a.bezier[6 + j]) : 0.1F, rv = a.bezier != null ? a.bezier[9 + j] : 0;
                    float lt = b.bezier != null ? Math.min(0, b.bezier[j]) : -0.1F, lv = b.bezier != null ? b.bezier[3 + j] : 0;
                    float x1 = span <= 0 ? 0 : Math.min(1, rt / span), x2 = span <= 0 ? 1 : Math.max(0, 1 + lt / span);
                    float s = Ease.solveBezier(f, x1, x2);
                    float u = 1 - s;
                    float p1 = va[j] + rv, p2 = vb[j] + lv;
                    out[j] = u * u * u * va[j] + 3 * u * u * s * p1 + 3 * u * s * s * p2 + s * s * s * vb[j];
                }
                return;
            }
            if (a.lerp == Lerp.CATMULLROM || b.lerp == Lerp.CATMULLROM) {
                float[] v0 = new float[3], v3 = new float[3];
                if (i > 0) {
                    k[i - 1].value(k[i - 1].post, c, v0);
                } else {
                    System.arraycopy(va, 0, v0, 0, 3);
                }
                if (i + 2 < n) {
                    k[i + 2].value(k[i + 2].pre, c, v3);
                } else {
                    System.arraycopy(vb, 0, v3, 0, 3);
                }
                for (int j = 0; j < 3; j++) {
                    out[j] = catmull(v0[j], va[j], vb[j], v3[j], f);
                }
                return;
            }
            float e = b.ease.apply(f);
            for (int j = 0; j < 3; j++) {
                out[j] = va[j] + (vb[j] - va[j]) * e;
            }
        }

        static float catmull(float p0, float p1, float p2, float p3, float t) {
            float t2 = t * t, t3 = t2 * t;
            return 0.5F * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
        }
    }

    public static final class Key {
        public final float t;
        /** Valor al llegar a este cuadro y al salir de él (normalmente iguales). */
        public final Expr[] pre, post;
        public final Lerp lerp;
        /** Curva para llegar a ESTE cuadro desde el anterior. */
        public final Ease.Fn ease;
        /** Si los tres valores son números fijos (para no evaluar expresiones). */
        private final float[] constPre, constPost;
        /**
         * Manijas de la curva (solo con {@link Lerp#BEZIER}): 12 números, en este orden: tiempo izquierdo x/y/z (segundos, negativos),
         * valor izquierdo x/y/z, tiempo derecho x/y/z (positivos), valor derecho x/y/z. Igual que bezier_left_time... de Blockbench.
         */
        public final float[] bezier;

        public Key(float t, Expr[] pre, Expr[] post, Lerp lerp, Ease.Fn ease) {
            this(t, pre, post, lerp, ease, null);
        }

        public Key(float t, Expr[] pre, Expr[] post, Lerp lerp, Ease.Fn ease, float[] bezier) {
            this.bezier = bezier;
            this.t = t;
            this.pre = pre;
            this.post = post;
            this.lerp = lerp;
            this.ease = ease;
            this.constPre = consts(pre);
            this.constPost = consts(post);
        }

        private static float[] consts(Expr[] e) {
            for (Expr x : e) {
                if (!x.isConst()) {
                    return null;
                }
            }
            return new float[] {(float) e[0].eval(Expr.EMPTY), (float) e[1].eval(Expr.EMPTY), (float) e[2].eval(Expr.EMPTY)};
        }

        void value(Expr[] which, Expr.Ctx c, float[] out) {
            float[] k = which == pre ? constPre : constPost;
            if (k != null) {
                out[0] = k[0];
                out[1] = k[1];
                out[2] = k[2];
                return;
            }
            out[0] = (float) which[0].eval(c);
            out[1] = (float) which[1].eval(c);
            out[2] = (float) which[2].eval(c);
        }
    }

    /** Algo que pasa en un momento de la animación. kind: "sound", "particle" o "timeline"; value: el nombre o el texto. */
    public static final class Event {
        public final float t;
        public final String kind, value, locator;

        public Event(float t, String kind, String value, String locator) {
            this.t = t;
            this.kind = kind;
            this.value = value;
            this.locator = locator;
        }
    }

    /** Las animaciones de un archivo, por nombre ("animation.persona.walk" y también solo "walk"). */
    public interface Library {
        Clip get(String name);

        Map<String, Clip> all();
    }
}
