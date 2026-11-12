package com.autoscoleccion.game;

import com.autoscoleccion.game.CarStyle.Shape;

import static com.autoscoleccion.game.CarStyle.*;

/**
 * Geometría de un cochecito a partir de su {@link CarStyle} (Java puro: la usan el cliente para dibujar y la herramienta de vista
 * previa). La carrocería se arma como un "loft": muchas rebanadas a lo largo del auto, cada una con una sección redondeada (bajos,
 * costado con línea de hombro, esquinas de arriba suaves), con morro y cola redondeados en planta y en alzado y guardabarros que se
 * ensanchan sobre cada rueda. Encima va la cabina (otro loft) con parabrisas y medallón curvos, pilares del color de la carrocería y
 * vidrios con reflejo. Luego pasos de rueda oscuros, llantas con rines de rayos y disco de freno, faros que se pegan a la forma del
 * morro, parrilla, puertas, espejos y los extras de cada modelo.
 * Medidas en píxeles (1/16 de bloque); el auto queda apoyado en y = 0, centrado y mirando a +X.
 */
public final class CarMesh {
    public static final int MODE_NORMAL = 0;
    /** Silueta negra para los autos que todavía no tienes. */
    public static final int MODE_SILHOUETTE = 1;
    public static final int SILHOUETTE_RGB = 0x0A0A10;

    /** Quien recibe las caras: cuatro esquinas en orden (la normal sale de (b-a)x(d-a)), un color por esquina y si brilla. */
    public interface Sink {
        void quad(float[] a, float[] b, float[] c, float[] d, int ca, int cb, int cc, int cd, boolean emissive);
    }

    private CarMesh() {
    }

    /** Todo el auto: la parte fija y la animada. */
    public static void build(CarStyle s, Sink sink, float time, int mode) {
        buildStatic(s, sink, mode);
        buildAnimated(s, sink, time, mode);
    }

    // =================================================================================================================== pintor

    private static final class Painter {
        final Sink sink;
        final int mode;

        Painter(Sink sink, int mode) {
            this.sink = sink;
            this.mode = mode;
        }

        void quad(float[] a, float[] b, float[] c, float[] d, int rgb, boolean em) {
            quad4(a, b, c, d, rgb, rgb, rgb, rgb, em);
        }

        void quad4(float[] a, float[] b, float[] c, float[] d, int ca, int cb, int cc, int cd, boolean em) {
            if (mode == MODE_SILHOUETTE) {
                sink.quad(a, b, c, d, SILHOUETTE_RGB, SILHOUETTE_RGB, SILHOUETTE_RGB, SILHOUETTE_RGB, false);
            } else {
                sink.quad(a, b, c, d, ca, cb, cc, cd, em);
            }
        }

        /** Igual que quad4 pero voltea el orden si hace falta para que la normal apunte hacia (nx, ny, nz). */
        void face(float[] a, float[] b, float[] c, float[] d, int ca, int cb, int cc, int cd, boolean em, float nx, float ny, float nz) {
            float[] n = normal(a, b, c, d);
            if (n[0] * nx + n[1] * ny + n[2] * nz < 0) {
                quad4(a, d, c, b, ca, cd, cc, cb, em);
            } else {
                quad4(a, b, c, d, ca, cb, cc, cd, em);
            }
        }

        void face(float[] a, float[] b, float[] c, float[] d, int rgb, boolean em, float nx, float ny, float nz) {
            face(a, b, c, d, rgb, rgb, rgb, rgb, em, nx, ny, nz);
        }

        /**
         * Prisma de 8 esquinas: abajo (y0) el rectángulo x0..x1 / z0..z1 y arriba (y1) el rectángulo tx0..tx1 / tz0..tz1. Sirve para
         * cajas normales y para cuñas.
         */
        void hexa(float x0, float x1, float z0, float z1, float y0, float tx0, float tx1, float tz0, float tz1, float y1, int rgb, boolean em) {
            float[] b00 = {x0, y0, z0};
            float[] b10 = {x1, y0, z0};
            float[] b11 = {x1, y0, z1};
            float[] b01 = {x0, y0, z1};
            float[] t00 = {tx0, y1, tz0};
            float[] t10 = {tx1, y1, tz0};
            float[] t11 = {tx1, y1, tz1};
            float[] t01 = {tx0, y1, tz1};
            quad(t00, t01, t11, t10, rgb, em);   // arriba
            quad(b00, b10, b11, b01, rgb, em);   // abajo
            quad(b10, t10, t11, b11, rgb, em);   // frente (+x)
            quad(b01, t01, t00, b00, rgb, em);   // atrás (-x)
            quad(b00, t00, t10, b10, rgb, em);   // lado -z
            quad(b11, t11, t01, b01, rgb, em);   // lado +z
        }

        void box(float x0, float x1, float y0, float y1, float z0, float z1, int rgb, boolean em) {
            hexa(x0, x1, z0, z1, y0, x0, x1, z0, z1, y1, rgb, em);
        }

        /** Anillo plano en el plano z (r0 = 0 da un disco) mirando hacia +z (side = 1) o -z (side = -1). */
        void ringZ(float cx, float cy, float z, float r0, float r1, int n, float rot, int rgb, boolean em, int side) {
            for (int i = 0; i < n; i++) {
                double a0 = rot + i * 2 * Math.PI / n;
                double a1 = rot + (i + 1) * 2 * Math.PI / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                face(new float[] {cx + c0 * r0, cy + s0 * r0, z}, new float[] {cx + c0 * r1, cy + s0 * r1, z},
                        new float[] {cx + c1 * r1, cy + s1 * r1, z}, new float[] {cx + c1 * r0, cy + s1 * r0, z}, rgb, em, 0, 0, side);
            }
        }

        /** Aro vertical (volante) en el plano x, mirando hacia +x. */
        void ringZ2(float x, float cy, float cz, float r, int rgb) {
            int n = 10;
            for (int i = 0; i < n; i++) {
                double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
                float r0 = r * 0.72F;
                face(new float[] {x, cy + (float) Math.sin(a0) * r0, cz + (float) Math.cos(a0) * r0},
                        new float[] {x, cy + (float) Math.sin(a0) * r, cz + (float) Math.cos(a0) * r},
                        new float[] {x, cy + (float) Math.sin(a1) * r, cz + (float) Math.cos(a1) * r},
                        new float[] {x, cy + (float) Math.sin(a1) * r0, cz + (float) Math.cos(a1) * r0}, rgb, false, 1, 0, 0);
            }
        }

        /** Anillo plano en el plano x (r0 = 0 da un disco) mirando hacia +x (dir = 1) o -x. */
        void ringX(float x, float cy, float cz, float r0, float r1, int n, int rgb, boolean em, int dir) {
            for (int i = 0; i < n; i++) {
                double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                face(new float[] {x, cy + s0 * r0, cz + c0 * r0}, new float[] {x, cy + s0 * r1, cz + c0 * r1}, new float[] {x, cy + s1 * r1, cz + c1 * r1},
                        new float[] {x, cy + s1 * r0, cz + c1 * r0}, rgb, em, dir, 0, 0);
            }
        }

        /** Tubo (solo el costado) a lo largo de Z, con radio r0 en z0 y r1 en z1. */
        void tubeZ(float cx, float cy, float z0, float z1, float r0, float r1, int n, float rot, int rgb, boolean em) {
            for (int i = 0; i < n; i++) {
                double a0 = rot + i * 2 * Math.PI / n;
                double a1 = rot + (i + 1) * 2 * Math.PI / n;
                double am = (a0 + a1) / 2;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                face(new float[] {cx + c0 * r0, cy + s0 * r0, z0}, new float[] {cx + c1 * r0, cy + s1 * r0, z0},
                        new float[] {cx + c1 * r1, cy + s1 * r1, z1}, new float[] {cx + c0 * r1, cy + s0 * r1, z1}, rgb, em,
                        (float) Math.cos(am), (float) Math.sin(am), 0);
            }
        }

        /** Tubo a lo largo de X con tapa en el extremo x1 (escapes). */
        void tubeX(float x0, float x1, float cy, float cz, float r, int n, int rgb, int capRgb, boolean capEm) {
            for (int i = 0; i < n; i++) {
                double a0 = i * 2 * Math.PI / n;
                double a1 = (i + 1) * 2 * Math.PI / n;
                double am = (a0 + a1) / 2;
                float y0 = cy + (float) Math.cos(a0) * r, z0 = cz + (float) Math.sin(a0) * r;
                float y1 = cy + (float) Math.cos(a1) * r, z1 = cz + (float) Math.sin(a1) * r;
                face(new float[] {x0, y0, z0}, new float[] {x0, y1, z1}, new float[] {x1, y1, z1}, new float[] {x1, y0, z0}, rgb, false, 0,
                        (float) Math.cos(am), (float) Math.sin(am));
                face(new float[] {x1, cy, cz}, new float[] {x1, y0, z0}, new float[] {x1, y1, z1}, new float[] {x1, cy, cz}, capRgb, capEm,
                        Math.signum(x1 - x0), 0, 0);
            }
        }
    }

    static float[] normal(float[] a, float[] b, float[] c, float[] d) {
        float[] n = cross(b[0] - a[0], b[1] - a[1], b[2] - a[2], d[0] - a[0], d[1] - a[1], d[2] - a[2]);
        if (n[0] * n[0] + n[1] * n[1] + n[2] * n[2] < 1e-10F) {
            n = cross(c[0] - b[0], c[1] - b[1], c[2] - b[2], a[0] - b[0], a[1] - b[1], a[2] - b[2]);
        }
        return n;
    }

    private static float[] cross(float ax, float ay, float az, float bx, float by, float bz) {
        return new float[] {ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
    }

    /** Curva suave (Catmull-Rom) que pasa por los pares x, y dados (x de menor a mayor). */
    static float curve(float[] p, float x) {
        int n = p.length / 2;
        if (x <= p[0]) {
            return p[1];
        }
        if (x >= p[(n - 1) * 2]) {
            return p[(n - 1) * 2 + 1];
        }
        int i = 0;
        while (i < n - 2 && x > p[(i + 1) * 2]) {
            i++;
        }
        float x0 = p[i * 2], x1 = p[(i + 1) * 2];
        float t = (x - x0) / Math.max(1e-5F, x1 - x0);
        float y0 = p[Math.max(0, i - 1) * 2 + 1], y1 = p[i * 2 + 1], y2 = p[(i + 1) * 2 + 1], y3 = p[Math.min(n - 1, i + 2) * 2 + 1];
        // tangentes de Catmull-Rom con cuidado de no pasarse (monótono)
        float m1 = (y2 - y0) * 0.5F, m2 = (y3 - y1) * 0.5F;
        float d = y2 - y1;
        if (d == 0) {
            m1 = 0;
            m2 = 0;
        } else {
            if (Math.signum(m1) != Math.signum(d)) {
                m1 = 0;
            }
            if (Math.signum(m2) != Math.signum(d)) {
                m2 = 0;
            }
            m1 = Math.signum(m1) * Math.min(Math.abs(m1), 3 * Math.abs(d));
            m2 = Math.signum(m2) * Math.min(Math.abs(m2), 3 * Math.abs(d));
        }
        float t2 = t * t, t3 = t2 * t;
        return (2 * t3 - 3 * t2 + 1) * y1 + (t3 - 2 * t2 + t) * m1 + (-2 * t3 + 3 * t2) * y2 + (t3 - t2) * m2;
    }

    static int shade(int rgb, float k) {
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 255) * k)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 255) * k)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * k)));
        return (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean sporty(Shape sh) {
        return sh == Shape.COUPE || sh == Shape.WEDGE || sh == Shape.GT || sh == Shape.HYPER || sh == Shape.PROTO;
    }

    // =================================================================================================================== carrocería

    /** La forma de la carrocería: ancho, techo y piso en cada punto del largo, y la sección redondeada. */
    static final class Body {
        static final int N = 30;
        final CarStyle s;
        final boolean formula;
        final float hx, hz, y0, y1, wb;
        final float rc, rvF, rvT, flF, flR, ctB, sh, noseDrop, tailDrop, bandLo, bandHi, midX;
        final float[] xs = new float[N];

        Body(CarStyle s) {
            this.s = s;
            this.formula = s.shape == Shape.FORMULA;
            hx = s.len / 2.0F;
            hz = s.wid / 2.0F;
            y0 = s.ride;
            y1 = s.ride + s.bodyH;
            wb = hz - 0.12F;
            float rcv;
            float flv;
            float ct;
            switch (s.shape) {
                case CLASSIC -> { rcv = 2.0F; flv = 0.32F; ct = 0.95F; }
                case MINI -> { rcv = 1.5F; flv = 0.14F; ct = 0.85F; }
                case PICKUP -> { rcv = 0.8F; flv = 0.18F; ct = 0.35F; }
                case VAN -> { rcv = 1.0F; flv = 0.06F; ct = 0.55F; }
                case LOWRIDER -> { rcv = 1.0F; flv = 0.08F; ct = 0.45F; }
                case MUSCLE -> { rcv = 0.9F; flv = 0.26F; ct = 0.5F; }
                case WEDGE -> { rcv = 1.2F; flv = 0.3F; ct = 0.6F; }
                case GT -> { rcv = 1.6F; flv = 0.28F; ct = 0.75F; }
                case HYPER -> { rcv = 1.7F; flv = 0.36F; ct = 0.7F; }
                case PROTO -> { rcv = 2.0F; flv = 0.42F; ct = 0.7F; }
                case COUPE -> { rcv = 1.4F; flv = 0.22F; ct = 0.65F; }
                case RALLY -> { rcv = 1.3F; flv = 0.3F; ct = 0.6F; }
                case FORMULA -> { rcv = 0.3F; flv = 0.0F; ct = 0.5F; }
                case SUV -> { rcv = 0.9F; flv = 0.2F; ct = 0.5F; }
                case BOXVAN -> { rcv = 0.6F; flv = 0.05F; ct = 0.4F; }
                case WAGON -> { rcv = 1.2F; flv = 0.14F; ct = 0.6F; }
                case ROADSTER -> { rcv = 1.6F; flv = 0.3F; ct = 0.75F; }
                default -> { rcv = 1.2F; flv = 0.12F; ct = 0.6F; }
            }
            float rd = Math.max(0.15F, s.round);
            rc = Math.min(rcv * rd, wb * 0.85F);
            flF = s.flareF >= 0 ? s.flareF : flv;
            flR = s.flareR >= 0 ? s.flareR : flv;
            ctB = ct * Math.min(1.6F, rd);
            midX = (s.cabFront + s.cabRear) / 2.0F;
            sh = formula ? 0.0F : 0.09F;
            rvF = rd * switch (s.shape) {
                case VAN, PICKUP, SUV, BOXVAN -> 0.55F;
                case CLASSIC, MINI -> 1.1F;
                default -> 0.8F;
            };
            rvT = rd * switch (s.shape) {
                case VAN, PICKUP, HATCH, MINI, SUV, BOXVAN, WAGON -> 0.4F;
                case CLASSIC -> 1.1F;
                default -> 0.7F;
            };
            noseDrop = Math.min(s.bodyH * 0.62F, s.noseSlope * 0.34F + 0.12F);
            tailDrop = Math.min(s.bodyH * 0.45F, s.tailSlope * 0.36F);
            if (s.has(SIDE_STRIPE)) {
                bandLo = 0.34F;
                bandHi = 0.54F;
            } else if (s.has(CHROME)) {
                bandLo = 0.56F;
                bandHi = 0.64F;
            } else {
                bandLo = 0.42F;
                bandHi = 0.6F;
            }
            for (int i = 0; i < N; i++) {
                double u = i / (double) (N - 1);
                double g = u - 0.78 * Math.sin(2 * Math.PI * u) / (2 * Math.PI);
                xs[i] = (float) (-hx + s.len * g);
            }
            xs[0] = -hx;
            xs[N - 1] = hx;
        }

        /** 0..1: cuánto se ensancha el guardabarros sobre la rueda que está en wx. */
        float bump(float x, float wx) {
            float d = Math.abs(x - wx) / (s.wheelR + 1.3F);
            return d < 1 ? (float) (0.5 * (1 + Math.cos(Math.PI * d))) : 0;
        }

        /** Ensanchamiento total de los guardabarros en x. */
        float flareAt(float x) {
            return Math.max(flF * bump(x, s.wheelFront), flR * bump(x, s.wheelRear));
        }

        /** Medio ancho sin guardabarros. */
        float w0(float x) {
            if (formula) {
                float base = hz - 0.1F;
                if (x > 0.6F) {
                    float u = (x - 0.6F) / (hx - 0.6F);
                    return lerp(base, 0.42F, (float) Math.pow(clamp(u, 0, 1), 0.7));
                }
                if (x < -3.5F) {
                    float u = (-3.5F - x) / (hx - 3.5F);
                    return lerp(base, base * 0.6F, clamp(u, 0, 1));
                }
                return base;
            }
            if (s.plan != null) {
                float w = curve(s.plan, x);
                if (s.waist > 0) {
                    float d = Math.abs(x - midX) / 3.2F;
                    if (d < 1) {
                        w -= s.waist * (float) (0.5 * (1 + Math.cos(Math.PI * d)));
                    }
                }
                return Math.max(0.2F, w);
            }
            float e = Math.min(x + hx, hx - x);
            float w = wb;
            if (s.noseW >= 0 && x > hx - s.noseLen) {
                float u = (x - (hx - s.noseLen)) / s.noseLen;
                w *= lerp(1, s.noseW, u * u * (3 - 2 * u));
            }
            if (s.tailW >= 0 && x < -hx + s.tailLen) {
                float u = ((-hx + s.tailLen) - x) / s.tailLen;
                w *= lerp(1, s.tailW, u * u * (3 - 2 * u));
            }
            if (e < rc) {
                float d = rc - Math.max(0, e);
                w = wb - rc + (float) Math.sqrt(Math.max(0, rc * rc - d * d));
            }
            if (s.waist > 0) {
                float d = Math.abs(x - midX) / 3.2F;
                if (d < 1) {
                    w -= s.waist * (float) (0.5 * (1 + Math.cos(Math.PI * d)));
                }
            }
            return w;
        }

        float w(float x) {
            return w0(x) + flareAt(x);
        }

        float top(float x) {
            if (s.sideTop != null) {
                return curve(s.sideTop, x);
            }
            float t = y1;
            float cf = s.cabFront;
            float cr = s.cabRear;
            if (x > cf) {
                float u = (x - cf) / Math.max(0.5F, hx - cf);
                t -= noseDrop * (float) Math.pow(clamp(u, 0, 1), 1.7);
            } else if (x < cr) {
                float u = (cr - x) / Math.max(0.5F, cr + hx);
                t -= tailDrop * (float) Math.pow(clamp(u, 0, 1), 1.5);
            }
            if (s.beltRise != 0) {
                t += s.beltRise * clamp(-x / hx, -1, 1) * 0.5F;
            }
            if (s.ducktail > 0) {
                float e = x + hx;
                if (e < 2.2F) {
                    float u = 1 - e / 2.2F;
                    t += s.ducktail * (float) Math.sin(u * Math.PI * 0.85);
                }
            }
            float ef = hx - x;
            if (ef < rvF) {
                float d = rvF - Math.max(0, ef);
                t -= (rvF - (float) Math.sqrt(Math.max(0, rvF * rvF - d * d))) * 0.85F;
            }
            float er = x + hx;
            if (er < rvT) {
                float d = rvT - Math.max(0, er);
                t -= (rvT - (float) Math.sqrt(Math.max(0, rvT * rvT - d * d))) * 0.85F;
            }
            return t;
        }

        float bot(float x) {
            if (s.sideBot != null) {
                return curve(s.sideBot, x);
            }
            float b = y0;
            if (s.tailLift > 0 && x < -hx + 3.0F) {
                float u = ((-hx + 3.0F) - x) / 3.0F;
                b += s.tailLift * u * u;
            }
            float e = Math.min(hx - x, x + hx);
            if (e < 1.1F) {
                float u = 1 - Math.max(0, e) / 1.1F;
                b += 0.38F * u * u;
            }
            return b;
        }

        /** Sección del lado derecho (+z): 10 puntos {z, y} desde el centro de abajo hasta el centro de arriba. */
        float[][] section(float x) {
            float b = bot(x);
            float t = Math.max(top(x), b + 0.3F);
            float w = w(x);
            float hgt = t - b;
            float cb = Math.min(0.32F, hgt * 0.2F);
            float ct = Math.min(Math.min(ctB, hgt * 0.45F), w * 0.45F);
            float yA = clamp(b + hgt * bandLo, b + cb + 0.01F, t - ct - 0.02F);
            float yB = clamp(b + hgt * bandHi, yA + 0.01F, t - ct - 0.01F);
            float s1 = Math.max(0.02F, Math.min(Math.max(0.85F, w * 0.5F), w - ct - 0.02F));
            float s0 = Math.max(0.01F, Math.min(0.25F, s1 - 0.02F));
            // guardabarros esculpidos: la orilla sube sobre las ruedas y el centro del cofre se abomba un poco
            float hump = Math.max(0, s.humpF) * bump(x, s.wheelFront) + Math.max(0, s.humpR) * bump(x, s.wheelRear);
            float dome = s.dome;
            return new float[][] {{0, b}, {w - cb, b}, {w, b + cb}, {w + sh, yA}, {w + sh, yB}, {w, t - ct + hump * 0.6F}, {w - ct, t + hump},
                    {s1, t + hump * 0.35F + dome * 0.6F}, {s0, t + dome}, {0, t + dome}};
        }

        /** Dónde está la superficie del costado derecho a la altura y (-1 si no hay carrocería ahí). */
        float surfZ(float x, float y) {
            float[][] p = section(x);
            float ymax = Math.max(p[9][1], Math.max(p[6][1], p[7][1]));
            if (y < p[0][1] || y > ymax) {
                return -1;
            }
            if (y <= p[1][1]) {
                return p[1][0];
            }
            float best = -1;
            for (int k = 1; k < 9; k++) {
                float ya = p[k][1], yb = p[k + 1][1];
                if (yb > ya && y >= ya && y <= yb) {
                    best = Math.max(best, lerp(p[k][0], p[k + 1][0], (y - ya) / (yb - ya)));
                }
            }
            return best < 0 ? p[6][0] : best;
        }

        boolean inside(float x, float y, float z) {
            if (x < -hx || x > hx) {
                return false;
            }
            float sz = surfZ(x, y);
            return sz >= 0 && Math.abs(z) <= sz;
        }

        /** x de la superficie del morro (front) o de la cola a la altura y y en el punto z. */
        float surfX(float y, float z, boolean front) {
            float dir = front ? -1 : 1;
            float x = front ? hx : -hx;
            for (int i = 0; i < 500; i++) {
                if (inside(x, y, z)) {
                    return x;
                }
                x += dir * 0.02F;
            }
            return x;
        }
    }

    /** Pintura con un degradado: más oscura abajo y más clara arriba, para que parezca brillante. */
    private static int paint(Body bd, int rgb, float y) {
        float k = 0.74F + 0.36F * clamp((y - bd.y0) / Math.max(0.5F, bd.y1 - bd.y0), 0, 1.15F);
        return shade(rgb, k);
    }

    private static int glassAt(int glass, float y, float yBase, float h) {
        float t = clamp((y - yBase) / Math.max(0.3F, h), 0, 1);
        return mix(shade(glass, 0.85F), 0xFFFFFF, 0.06F + 0.3F * t * t);
    }

    // =================================================================================================================== construir

    /** El auto completo sin animación (carrocería y llantas en su lugar). */
    public static void buildStatic(CarStyle s, Sink sink, int mode) {
        buildBody(s, sink, mode);
        Painter p = new Painter(sink, mode);
        for (float[] m : wheelMounts(s)) {
            int side = (int) m[3];
            float hw = m[5] / 2;
            wheel(p, s, m[0], m[1], m[1], m[2] + side * hw, m[2] - side * hw, side, s.has(GOLD_SPARKLE));
        }
    }

    /** Todo menos las llantas (que se dibujan aparte cuando el auto se maneja). */
    public static void buildBody(CarStyle s, Sink sink, int mode) {
        Painter p = new Painter(sink, mode);
        Body bd = new Body(s);
        boolean formula = bd.formula;
        boolean gold = s.has(GOLD_SPARKLE);
        boolean chrome = s.has(CHROME);
        int upper = s.has(TWO_TONE) ? s.accent : s.body;
        int dark = 0x141518;

        bodyShell(p, bd, upper, gold);
        Cabin cab = null;
        if (s.roofH > 0.01F && !formula) {
            cab = new Cabin(bd);
            cab.build(p, upper);
        }
        if (s.shape == Shape.PICKUP || s.shape == Shape.MONSTER) {
            float bx1 = s.cabRear - 0.3F;
            float bt = bd.top(-1) - 0.02F;
            float ww = bd.wb - 0.05F;
            p.box(-bd.hx + 0.5F, bx1, bt, bt + 0.05F, -ww + 0.35F, ww - 0.35F, shade(s.body, 0.3F), false);
            p.box(-bd.hx + 0.3F, bx1, bt, bt + 0.9F, -ww, -ww + 0.35F, paint(bd, s.body, bt + 0.9F), false);
            p.box(-bd.hx + 0.3F, bx1, bt, bt + 0.9F, ww - 0.35F, ww, paint(bd, s.body, bt + 0.9F), false);
            p.box(-bd.hx + 0.2F, -bd.hx + 0.55F, bt, bt + 0.9F, -ww + 0.3F, ww - 0.3F, paint(bd, s.body, bt + 0.7F), false);
            p.box(bx1 - 0.05F, bx1 + 0.3F, bt, bt + 0.9F, -ww, ww, paint(bd, s.body, bt + 0.8F), false);
        }
        if (formula) {
            formulaParts(p, bd);
        } else {
            lights(p, bd);
            doors(p, bd, cab, upper);
            details(p, bd, cab);
        }
        special(p, bd, cab);
        arches(p, bd);
        extras(p, bd, cab, chrome, dark);
    }

    /** Lo que se mueve o parpadea: torreta, neón del piso y destellos del oro. */
    public static void buildAnimated(CarStyle s, Sink sink, float time, int mode) {
        if (mode != MODE_NORMAL || !(s.has(SIREN) || s.has(NEON) || s.has(GOLD_SPARKLE) || s.hasD(D_HOVER) || s.hasD(D_JET))) {
            return;
        }
        Painter p = new Painter(sink, mode);
        Body bd = new Body(s);
        float hx = bd.hx, hz = bd.hz;
        if (s.hasD(D_HOVER)) {
            float pulse = 0.75F + 0.25F * (float) Math.sin(time * 0.3F);
            for (float x : new float[] {s.wheelFront, s.wheelRear}) {
                for (int side = -1; side <= 1; side += 2) {
                    float z = side * (bd.w0(x) - 1.1F);
                    float y = bd.bot(x) - 0.08F;
                    for (int k = 0; k < 3; k++) {
                        float r = 0.9F - k * 0.25F;
                        int c = shade(s.neon, pulse * (0.6F + 0.2F * k));
                        int n = 14;
                        for (int i = 0; i < n; i++) {
                            double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
                            float yy = y - k * 0.02F;
                            p.face(new float[] {x, yy, z}, new float[] {x + (float) Math.cos(a0) * r, yy, z + (float) Math.sin(a0) * r},
                                    new float[] {x + (float) Math.cos(a1) * r, yy, z + (float) Math.sin(a1) * r}, new float[] {x, yy, z}, c, true, 0, -1, 0);
                        }
                    }
                    float gy = 0.03F;
                    float rr = 1.0F + 0.2F * pulse;
                    p.face(new float[] {x - rr, gy, z - rr}, new float[] {x + rr, gy, z - rr}, new float[] {x + rr, gy, z + rr}, new float[] {x - rr, gy, z + rr},
                            shade(s.neon, 0.35F * pulse), true, 0, 1, 0);
                }
            }
        }
        if (s.hasD(D_JET)) {
            float y = lerp(bd.bot(-hx), bd.top(-hx + 1), 0.55F);
            float flick = 0.8F + 0.2F * (float) Math.sin(time * 1.7F) + 0.1F * (float) Math.sin(time * 3.1F);
            float len = 1.8F * flick;
            int n = 10;
            for (int layer = 0; layer < 2; layer++) {
                float r = layer == 0 ? 0.62F : 0.35F;
                float L = layer == 0 ? len : len * 1.4F;
                int c = layer == 0 ? 0xFF7A1A : 0xFFF1B0;
                for (int i = 0; i < n; i++) {
                    double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
                    float xb = -hx - 1.06F - layer * 0.01F;
                    p.face(new float[] {xb, y + (float) Math.cos(a0) * r, (float) Math.sin(a0) * r}, new float[] {xb, y + (float) Math.cos(a1) * r,
                            (float) Math.sin(a1) * r}, new float[] {xb - L, y, 0}, new float[] {xb - L, y, 0}, c, true, (float) -Math.cos((a0 + a1) / 2) * 0
                            - 1, 0, 0);
                }
            }
        }
        if (s.has(SIREN) && s.roofH > 0.01F) {
            float[] rr = Cabin.roofRect(bd);
            float cx = (rr[0] + rr[1]) / 2.0F;
            float yt = rr[3] + Math.min(0.14F, s.roofH * 0.07F) + 0.2F;
            boolean on = ((int) (time / 4.0F)) % 2 == 0;
            p.box(cx - 0.45F, cx + 0.45F, yt, yt + 0.42F, -1.9F, -0.12F, on ? 0xFF2030 : 0x5A1015, on);
            p.box(cx - 0.45F, cx + 0.45F, yt, yt + 0.42F, 0.12F, 1.9F, on ? 0x1B2E5A : 0x2A6BFF, !on);
        }
        if (s.has(NEON)) {
            float pulse = 0.8F + 0.2F * (float) Math.sin(time * 0.25F);
            p.face(new float[] {-hx + 1.0F, 0.03F, -hz + 0.3F}, new float[] {hx - 1.0F, 0.03F, -hz + 0.3F}, new float[] {hx - 1.0F, 0.03F, hz - 0.3F},
                    new float[] {-hx + 1.0F, 0.03F, hz - 0.3F}, shade(s.neon, pulse), true, 0, 1, 0);
            p.box(-hx + 1.5F, hx - 1.5F, bd.y0 - 0.3F, bd.y0 - 0.22F, -hz + 0.5F, hz - 0.5F, s.neon, true);
        }
        if (s.has(GOLD_SPARKLE)) {
            for (int i = 0; i < 7; i++) {
                float ph = (time * 0.09F + i * 0.37F) % 1.0F;
                if (ph > 0.35F) {
                    continue;
                }
                float k = 1.0F - Math.abs(ph - 0.17F) / 0.17F;
                float sx = lerp(-hx + 1.5F, hx - 1.5F, (i * 0.618F) % 1.0F);
                float sy = lerp(bd.bot(sx), bd.top(sx), 0.35F + 0.5F * ((i * 0.381F) % 1.0F));
                int side = i % 2 == 0 ? -1 : 1;
                float sz = side * (bd.surfZ(sx, sy) + 0.06F);
                float r = 0.12F + 0.3F * k;
                p.box(sx - r, sx + r, sy - 0.05F, sy + 0.05F, sz - 0.03F, sz + 0.03F, 0xFFFFF0, true);
                p.box(sx - 0.05F, sx + 0.05F, sy - r, sy + r, sz - 0.03F, sz + 0.03F, 0xFFFFF0, true);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- casco

    private static void bodyShell(Painter p, Body bd, int upper, boolean gold) {
        CarStyle s = bd.s;
        int n = Body.N;
        float[][][] sec = new float[n][][];
        for (int i = 0; i < n; i++) {
            sec[i] = bd.section(bd.xs[i]);
        }
        boolean vanTwoTone = s.shape == Shape.VAN && s.has(TWO_TONE);
        int[] segCol = new int[9];
        boolean[] segEm = new boolean[9];
        boolean[] segPaint = new boolean[9];
        for (int k = 0; k < 9; k++) {
            segCol[k] = s.body;
            segEm[k] = gold;
            segPaint[k] = true;
        }
        segCol[0] = 0x111214;
        segEm[0] = false;
        segPaint[0] = false;
        if (s.shape == Shape.RALLY || s.shape == Shape.PICKUP || s.hasD(D_SKIRTS) || s.hasD(D_GUARDS)) {
            segCol[1] = s.hasD(D_SKIRTS) ? 0x18191D : 0x2A2B30;   // moldura de plástico / faldones
            segEm[1] = false;
            segPaint[1] = false;
        }
        if (s.has(SIDE_STRIPE)) {
            segCol[3] = s.accent;
            segEm[3] = s.has(NEON);
            segPaint[3] = !s.has(NEON);
        } else if (s.has(CHROME)) {
            segCol[3] = 0xEEF2F6;
            segEm[3] = gold;
            segPaint[3] = false;
        }
        if (vanTwoTone) {
            for (int k = 4; k < 9; k++) {
                segCol[k] = upper;
            }
        }
        if (s.has(STRIPES)) {
            segCol[7] = s.accent;
        }
        if (s.belly >= 0) {
            segCol[1] = s.belly;
            segCol[2] = s.belly;
            segEm[1] = segEm[2] = false;
        }
        for (int i = 0; i < n - 1; i++) {
            float xa = bd.xs[i], xb = bd.xs[i + 1];
            for (int k = 0; k < 9; k++) {
                float[] pa = sec[i][k], pb = sec[i + 1][k], pc = sec[i + 1][k + 1], pd = sec[i][k + 1];
                float dz = (pd[0] + pc[0] - pa[0] - pb[0]) / 2, dy = (pd[1] + pc[1] - pa[1] - pb[1]) / 2;
                if (Math.abs(dz) + Math.abs(dy) < 1e-4F) {
                    continue;
                }
                int col = segCol[k];
                int ca = segPaint[k] ? paint(bd, col, pa[1]) : col;
                int cb = segPaint[k] ? paint(bd, col, pb[1]) : col;
                int cc = segPaint[k] ? paint(bd, col, pc[1]) : col;
                int cd = segPaint[k] ? paint(bd, col, pd[1]) : col;
                if (k == 5) {
                    // reflejo en el hombro
                    cc = shade(cc, 1.08F);
                    cd = shade(cd, 1.08F);
                }
                for (int side = -1; side <= 1; side += 2) {
                    p.face(new float[] {xa, pa[1], side * pa[0]}, new float[] {xb, pb[1], side * pb[0]}, new float[] {xb, pc[1], side * pc[0]},
                            new float[] {xa, pd[1], side * pd[0]}, ca, cb, cc, cd, segEm[k], 0, -dz, side * dy);
                }
            }
        }
        // tapas de los extremos
        for (int end = 0; end < 2; end++) {
            float[][] q = sec[end == 0 ? 0 : n - 1];
            float x = end == 0 ? -bd.hx : bd.hx;
            float cy = (q[0][1] + q[9][1]) / 2;
            float[] c = {x, cy, 0};
            int col = paint(bd, s.body, cy);
            for (int side = -1; side <= 1; side += 2) {
                for (int k = 0; k < 9; k++) {
                    float[] a = {x, q[k][1], side * q[k][0]};
                    float[] b = {x, q[k + 1][1], side * q[k + 1][0]};
                    p.face(c, a, b, b, k == 0 ? 0x111214 : col, k == 0 ? 0x111214 : col, k == 0 ? 0x111214 : col, k == 0 ? 0x111214 : col, gold && k > 0,
                            end == 0 ? -1 : 1, 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- cabina

    /** La cabina: rebanadas desde la base del medallón hasta la base del parabrisas. */
    static final class Cabin {
        final Body bd;
        /** Por estación: x, y de la base, y del techo, medio ancho abajo, medio ancho arriba, radio de la esquina. */
        final float[][] st;
        final int roofStart;
        final int roofEnd;
        final float rr, rf, yt;
        /** Alto de la cabina (para el degradado del vidrio y la estrechez de arriba). */
        final float rh;
        final float xr0, xf0;

        Cabin(Body bd) {
            this.bd = bd;
            CarStyle s = bd.s;
            float[] r = roofRect(bd);
            rr = r[0];
            rf = r[1];
            yt = r[3];
            if (s.roofLine != null) {
                float[] q = s.roofLine;
                int n = q.length / 2;
                xr0 = q[0];
                xf0 = q[(n - 1) * 2];
                float h = 0.3F;
                for (int i = 0; i < n; i++) {
                    h = Math.max(h, q[i * 2 + 1] - bd.top(q[i * 2]));
                }
                rh = h;
                roofStart = s.roofA;
                roofEnd = s.roofB;
                st = new float[n][];
                for (int i = 0; i < n; i++) {
                    float x = q[i * 2];
                    float yb = bd.top(x);
                    st[i] = station(x, yb, i == 0 || i == n - 1 ? yb : q[i * 2 + 1]);
                }
                return;
            }
            rh = s.roofH;
            roofStart = 2;
            roofEnd = 4;
            xr0 = s.cabRear;
            xf0 = s.cabFront;
            float cf = s.cabFront, cr = s.cabRear;
            float ri = rr - cr, wi = cf - rf;
            float ybR = bd.top(cr), ybF = bd.top(cf);
            st = new float[][] {
                    station(cr, ybR, ybR),
                    station(lerp(cr, rr, 0.72F) - 0.09F * Math.min(1, ri), bd.top(lerp(cr, rr, 0.72F)), lerp(ybR, yt, 0.72F) + 0.06F * Math.min(1, ri)),
                    station(rr, bd.top(rr), yt),
                    station((rr + rf) / 2, bd.top((rr + rf) / 2), yt + crownOf(s)),
                    station(rf, bd.top(rf), yt),
                    station(lerp(cf, rf, 0.72F) + 0.1F * Math.min(1, wi), bd.top(lerp(cf, rf, 0.72F)), lerp(ybF, yt, 0.72F) + 0.07F * Math.min(1, wi)),
                    station(cf, ybF, ybF)};
        }

        static float crownOf(CarStyle s) {
            return s.crown >= 0 ? s.crown : Math.min(0.14F, s.roofH * 0.07F);
        }

        static boolean open(CarStyle s) {
            return switch (s.shape) {
                case ROADSTER, HOTROD, BUGGY, DRAGSTER, ROVER, KART -> true;
                default -> false;
            };
        }

        /** Sin parabrisas (buggy, rover, dragster, kart). */
        static boolean noGlass(CarStyle s) {
            return switch (s.shape) {
                case BUGGY, DRAGSTER, ROVER, KART -> true;
                default -> false;
            };
        }

        /** {roof rear x, roof front x, medio ancho del techo, y del techo}. */
        static float[] roofRect(Body bd) {
            CarStyle s = bd.s;
            if (s.roofLine != null) {
                float[] q = s.roofLine;
                float xa = q[s.roofA * 2], xb = q[s.roofB * 2];
                float y = 0;
                for (int i = s.roofA; i <= s.roofB; i++) {
                    y = Math.max(y, q[i * 2 + 1]);
                }
                float gb = bd.w0((xa + xb) / 2) - s.cabInset;
                return new float[] {xa, xb, gb - s.sideInset, y};
            }
            float cf = s.cabFront, cr = s.cabRear;
            float ws = s.wsInset, ri = s.rearInset;
            float room = (cf - cr) - 0.8F;
            if (ws + ri > room) {
                float k = room / (ws + ri);
                ws *= k;
                ri *= k;
            }
            float yt = bd.top((cf + cr) / 2) - 0.02F + s.roofH;
            float gb = bd.w0((cf + cr) / 2) - s.cabInset;
            return new float[] {cr + ri, cf - ws, gb - s.sideInset, yt};
        }

        private float[] station(float x, float yb, float ytop) {
            CarStyle s = bd.s;
            yb -= 0.02F;
            float hf = clamp((ytop - yb) / Math.max(0.3F, s.roofLine != null ? rh : s.roofH), 0, 1);
            float gb = bd.w0(x) - s.cabInset;
            if (s.cabTaper > 0) {
                float cf = s.cabFront, cr = s.cabRear;
                gb -= s.cabTaper * clamp((cf - x) / Math.max(0.5F, cf - cr), 0, 1);
            }
            float gt = gb - s.sideInset * hf;
            float r = Math.min(0.5F, s.roofH * 0.28F);
            return new float[] {x, yb, Math.max(ytop, yb), gb, gt, r, hf};
        }

        /** Sección derecha en una estación: 6 puntos {z, y}. */
        static float[][] sec(float[] q) {
            float yb = q[1], ytop = q[2], gb = q[3], gt = q[4], r = q[5], hf = q[6];
            float rz = Math.max(0.2F, r);
            float q1z = gt, q1y = ytop - r * hf;
            float q2z = gt - rz;
            float s1 = Math.max(0.02F, Math.min(0.85F, q2z - 0.02F));
            float s0 = Math.max(0.01F, Math.min(0.25F, s1 - 0.02F));
            return new float[][] {{gb, yb}, {q1z, Math.max(yb, q1y)}, {q2z, ytop}, {s1, ytop}, {s0, ytop}, {0, ytop}};
        }

        /** Estación interpolada en x (entre las estaciones del techo). */
        float[] at(float x) {
            for (int j = 0; j < st.length - 1; j++) {
                if (x >= st[j][0] && x <= st[j + 1][0]) {
                    float t = (x - st[j][0]) / Math.max(1e-4F, st[j + 1][0] - st[j][0]);
                    float[] o = new float[7];
                    for (int k = 0; k < 7; k++) {
                        o[k] = lerp(st[j][k], st[j + 1][k], t);
                    }
                    return o;
                }
            }
            return st[x < st[0][0] ? 0 : st.length - 1];
        }

        void build(Painter p, int upper) {
            CarStyle s = bd.s;
            boolean canopy = s.has(CANOPY);
            boolean cPillar = switch (s.shape) {
                case HATCH, VAN, MINI, PICKUP -> false;
                default -> true;
            };
            float hgt = rh;
            boolean open = open(s);
            if (noGlass(s)) {
                openCockpit(p, upper);
                return;
            }
            for (int j = open ? roofEnd : 0; j < st.length - 1; j++) {
                float[][] a = sec(st[j]);
                float[][] b = sec(st[j + 1]);
                float xa = st[j][0], xb = st[j + 1][0];
                boolean rear = j < roofStart;
                boolean roof = j >= roofStart && j < roofEnd;
                for (int k = 0; k < 5; k++) {
                    float[] pa = a[k], pb = b[k], pc = b[k + 1], pd = a[k + 1];
                    float dz = (pd[0] + pc[0] - pa[0] - pb[0]) / 2, dy = (pd[1] + pc[1] - pa[1] - pb[1]) / 2;
                    if (Math.abs(dz) + Math.abs(dy) < 1e-4F) {
                        continue;
                    }
                    boolean isGlass;
                    int col = upper;
                    if (open && k == 0) {
                        continue;
                    }
                    if (k == 0) {
                        isGlass = canopy || !(rear && cPillar);
                    } else if (k == 1) {
                        isGlass = canopy && roof;
                    } else {
                        isGlass = !roof || canopy;
                        if (k == 3 && roof && s.has(STRIPES) && !canopy) {
                            col = s.accent;
                        }
                    }
                    int ca, cb, cc, cd;
                    float base = Math.min(st[j][1], st[j + 1][1]);
                    if (isGlass) {
                        ca = glassAt(s.glass, pa[1], base, hgt);
                        cb = glassAt(s.glass, pb[1], base, hgt);
                        cc = glassAt(s.glass, pc[1], base, hgt);
                        cd = glassAt(s.glass, pd[1], base, hgt);
                    } else {
                        ca = paint(bd, col, pa[1]);
                        cb = paint(bd, col, pb[1]);
                        cc = paint(bd, col, pc[1]);
                        cd = paint(bd, col, pd[1]);
                    }
                    for (int side = -1; side <= 1; side += 2) {
                        p.face(new float[] {xa, pa[1], side * pa[0]}, new float[] {xb, pb[1], side * pb[0]},
                                new float[] {xb, pc[1], side * pc[0]}, new float[] {xa, pd[1], side * pd[0]}, ca, cb, cc, cd, false, 0, -dz, side * dy);
                    }
                }
            }
            if (open) {
                openCockpit(p, upper);
                return;
            }
            if (s.glassLow > 0 || s.hasD(D_CHROME_TRIM)) {
                windowBand(p, upper);
            }
            if (canopy) {
                return;
            }
            // pilar B (y los de la combi), pegados encima del vidrio del costado
            float mx = (rr + rf) / 2 - 0.15F;
            pillar(p, mx, s.shape == Shape.VAN ? 0.32F : 0.2F, upper);
            if (s.shape == Shape.VAN) {
                pillar(p, lerp(rr, mx, 0.5F), 0.28F, upper);
            }
            if (s.shape == Shape.SEDAN || s.shape == Shape.RALLY) {
                // cuarto vidrio trasero: un pilar delgado antes del pilar C
                pillar(p, rr + 0.1F, 0.12F, upper);
            }
        }

        /** Parte baja de las ventanas en color carrocería (ventanas bajitas) y moldura cromada. */
        private void windowBand(Painter p, int col) {
            CarStyle s = bd.s;
            boolean cPillar = s.shape != Shape.HATCH && s.shape != Shape.VAN && s.shape != Shape.MINI && s.shape != Shape.PICKUP;
            for (int j = 0; j < st.length - 1; j++) {
                if (j < roofStart && cPillar) {
                    continue;
                }
                float[][] a = sec(st[j]), b = sec(st[j + 1]);
                float xa = st[j][0], xb = st[j + 1][0];
                float k = s.glassLow;
                float[] a1 = {lerp(a[0][0], a[1][0], k), lerp(a[0][1], a[1][1], k)};
                float[] b1 = {lerp(b[0][0], b[1][0], k), lerp(b[0][1], b[1][1], k)};
                for (int side = -1; side <= 1; side += 2) {
                    float off = 0.02F;
                    if (k > 0) {
                        p.face(new float[] {xa, a[0][1], side * (a[0][0] + off)}, new float[] {xb, b[0][1], side * (b[0][0] + off)},
                                new float[] {xb, b1[1], side * (b1[0] + off)}, new float[] {xa, a1[1], side * (a1[0] + off)},
                                paint(bd, col, a[0][1]), paint(bd, col, b[0][1]), paint(bd, col, b1[1]), paint(bd, col, a1[1]), false, 0, 0.3F, side);
                    }
                    if (s.hasD(D_CHROME_TRIM)) {
                        float t2 = Math.min(1, k + 0.07F);
                        float[] a2 = {lerp(a[0][0], a[1][0], t2), lerp(a[0][1], a[1][1], t2)};
                        float[] b2 = {lerp(b[0][0], b[1][0], t2), lerp(b[0][1], b[1][1], t2)};
                        p.face(new float[] {xa, a1[1], side * (a1[0] + off + 0.005F)}, new float[] {xb, b1[1], side * (b1[0] + off + 0.005F)},
                                new float[] {xb, b2[1], side * (b2[0] + off + 0.005F)}, new float[] {xa, a2[1], side * (a2[0] + off + 0.005F)},
                                0xE8ECF0, false, 0, 0.3F, side);
                    }
                }
            }
        }

        /** Descapotable: tina oscura, asientos, volante y arcos de seguridad. */
        private void openCockpit(Painter p, int upper) {
            CarStyle s = bd.s;
            float cf = s.cabFront, cr = s.cabRear;
            float gb = bd.w0((cf + cr) / 2) - s.cabInset;
            float y = bd.top((cf + cr) / 2);
            int tub = 0x1A1414;
            int seat = s.interior == 0x1C1C22 ? 0x6A2A22 : s.interior;
            p.face(new float[] {cr, y + 0.02F, -gb}, new float[] {cf, y + 0.02F, -gb}, new float[] {cf, y + 0.02F, gb}, new float[] {cr, y + 0.02F, gb}, tub,
                    false, 0, 1, 0);
            for (int side = -1; side <= 1; side += 2) {
                float zc = side * gb * 0.48F;
                float sx = lerp(cr, cf, 0.32F);
                p.box(sx - 0.7F, sx + 0.5F, y, y + 0.35F, zc - 0.65F, zc + 0.65F, seat, false);
                p.hexa(sx - 0.75F, sx - 0.35F, zc - 0.6F, zc + 0.6F, y + 0.3F, sx - 0.95F, sx - 0.6F, zc - 0.55F, zc + 0.55F, y + 1.3F, seat, false);
                // arco de seguridad detrás de cada asiento
                float hx0 = sx - 1.25F;
                p.box(hx0, hx0 + 0.18F, y, y + 1.45F, zc - 0.65F, zc - 0.5F, 0x2A2C30, false);
                p.box(hx0, hx0 + 0.18F, y, y + 1.45F, zc + 0.5F, zc + 0.65F, 0x2A2C30, false);
                p.box(hx0, hx0 + 0.18F, y + 1.3F, y + 1.45F, zc - 0.65F, zc + 0.65F, 0x2A2C30, false);
            }
            // volante (del lado izquierdo) y tablero
            float wx = cf - 0.9F;
            p.box(cf - 0.5F, cf - 0.1F, y, y + 0.45F, -gb + 0.1F, gb - 0.1F, 0x15151A, false);
            p.ringZ2(wx, y + 0.75F, -gb * 0.48F, 0.32F, 0x101012);
            p.box(wx - 0.05F, wx + 0.35F, y + 0.4F, y + 0.75F, -gb * 0.48F - 0.04F, -gb * 0.48F + 0.04F, 0x101012, false);
        }

        private void pillar(Painter p, float x, float half, int col) {
            float[] qa = at(x - half), qb = at(x + half);
            float[][] a = sec(qa), b = sec(qb);
            for (int side = -1; side <= 1; side += 2) {
                float off = 0.025F;
                p.face(new float[] {x - half, a[0][1], side * (a[0][0] + off)}, new float[] {x + half, b[0][1], side * (b[0][0] + off)},
                        new float[] {x + half, b[1][1], side * (b[1][0] + off)}, new float[] {x - half, a[1][1], side * (a[1][0] + off)},
                        paint(bd, col, a[0][1]), paint(bd, col, b[0][1]), paint(bd, col, b[1][1]), paint(bd, col, a[1][1]), false, 0, 0.3F, side);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- luces y frente

    /** Una calcomanía pegada al morro o a la cola: cada esquina se apoya en la superficie. */
    private static void frontDecal(Painter p, Body bd, float z0, float z1, float ya, float yb, int rgb, boolean em, boolean front, int sub, float off) {
        float dir = front ? 1 : -1;
        for (int i = 0; i < sub; i++) {
            float za = lerp(z0, z1, i / (float) sub), zb = lerp(z0, z1, (i + 1) / (float) sub);
            float[] a = {bd.surfX(ya, za, front) + dir * off, ya, za};
            float[] b = {bd.surfX(ya, zb, front) + dir * off, ya, zb};
            float[] c = {bd.surfX(yb, zb, front) + dir * off, yb, zb};
            float[] d = {bd.surfX(yb, za, front) + dir * off, yb, za};
            p.face(a, b, c, d, rgb, em, dir, 0, 0);
        }
    }

    private static int headStyle(CarStyle s) {
        if (s.head >= 0) {
            return s.head;
        }
        return switch (s.shape) {
            case CLASSIC, MINI, VAN, ROADSTER -> HEAD_ROUND;
            case MUSCLE, LOWRIDER -> HEAD_QUAD;
            case COUPE, WEDGE, GT, HYPER, PROTO -> HEAD_SLIM;
            default -> HEAD_RECT;
        };
    }

    private static int tailStyle(CarStyle s) {
        if (s.tail >= 0) {
            return s.tail;
        }
        return switch (s.shape) {
            case CLASSIC, MINI, VAN, ROADSTER -> TAIL_ROUND1;
            case WEDGE, GT, HYPER, PROTO -> TAIL_BAR;
            case SUV, BOXVAN, PICKUP -> TAIL_VERT;
            default -> TAIL_RECT;
        };
    }

    /** Elipse pegada al morro o a la cola (faros redondos, parrillas ovaladas). */
    private static void ovalDecal(Painter p, Body bd, float zc, float yc, float rz, float ry, int rgb, boolean em, boolean front, float off, int ringRgb) {
        float dir = front ? 1 : -1;
        int n = 12;
        float[] c = {bd.surfX(yc, zc, front) + dir * (off + 0.01F), yc, zc};
        for (int i = 0; i < n; i++) {
            double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
            float y0 = yc + (float) Math.sin(a0) * ry, z0 = zc + (float) Math.cos(a0) * rz;
            float y1 = yc + (float) Math.sin(a1) * ry, z1 = zc + (float) Math.cos(a1) * rz;
            float[] a = {bd.surfX(y0, z0, front) + dir * (off + 0.01F), y0, z0};
            float[] b = {bd.surfX(y1, z1, front) + dir * (off + 0.01F), y1, z1};
            p.face(c, a, b, b, rgb, em, dir, 0, 0);
            if (ringRgb >= 0) {
                float y2 = yc + (float) Math.sin(a0) * (ry + 0.12F), z2 = zc + (float) Math.cos(a0) * (rz + 0.12F);
                float y3 = yc + (float) Math.sin(a1) * (ry + 0.12F), z3 = zc + (float) Math.cos(a1) * (rz + 0.12F);
                float[] e = {bd.surfX(y2, z2, front) + dir * off, y2, z2};
                float[] f = {bd.surfX(y3, z3, front) + dir * off, y3, z3};
                p.face(new float[] {a[0] - dir * 0.01F, a[1], a[2]}, e, f, new float[] {b[0] - dir * 0.01F, b[1], b[2]}, ringRgb, false, dir, 0, 0);
            }
        }
    }

    /** Una tira inclinada pegada al morro o a la cola, de (z0, y0) a (z1, y1) con grosor h. */
    private static void slantDecal(Painter p, Body bd, float z0, float y0, float z1, float y1, float h, int rgb, boolean em, boolean front, float off) {
        float dir = front ? 1 : -1;
        int sub = 4;
        for (int i = 0; i < sub; i++) {
            float ta = i / (float) sub, tb = (i + 1) / (float) sub;
            float za = lerp(z0, z1, ta), zb = lerp(z0, z1, tb), ya = lerp(y0, y1, ta), yb = lerp(y0, y1, tb);
            float[] a = {bd.surfX(ya, za, front) + dir * off, ya, za};
            float[] b = {bd.surfX(yb, zb, front) + dir * off, yb, zb};
            float[] c = {bd.surfX(yb + h, zb, front) + dir * off, yb + h, zb};
            float[] d = {bd.surfX(ya + h, za, front) + dir * off, ya + h, za};
            p.face(a, b, c, d, rgb, em, dir, 0, 0);
        }
    }

    /** Calcomanía en el costado (ambos lados): polígono convexo de puntos (x, y), pegado a la superficie. */
    private static void sideDecal(Painter p, Body bd, float[][] pts, int rgb, boolean em, float off) {
        for (int side = -1; side <= 1; side += 2) {
            float[] c = pts[0];
            for (int i = 1; i < pts.length - 1; i++) {
                float[] a = pts[i], b = pts[i + 1];
                float zc = bd.surfZ(c[0], c[1]), za = bd.surfZ(a[0], a[1]), zb = bd.surfZ(b[0], b[1]);
                if (zc < 0 || za < 0 || zb < 0) {
                    continue;
                }
                p.face(new float[] {c[0], c[1], side * (zc + off)}, new float[] {a[0], a[1], side * (za + off)}, new float[] {b[0], b[1], side * (zb + off)},
                        new float[] {b[0], b[1], side * (zb + off)}, rgb, em, 0, 0, side);
            }
        }
    }

    /** Rectángulo sobre el cofre o la cajuela (sigue la altura de arriba). */
    private static void topDecal(Painter p, Body bd, float x0, float x1, float z0, float z1, int rgb, boolean em, float off) {
        int sub = 3;
        for (int i = 0; i < sub; i++) {
            float xa = lerp(x0, x1, i / (float) sub), xb = lerp(x0, x1, (i + 1) / (float) sub);
            float ya = bd.top(xa) + off, yb = bd.top(xb) + off;
            p.face(new float[] {xa, ya, z0}, new float[] {xb, yb, z0}, new float[] {xb, yb, z1}, new float[] {xa, ya, z1}, rgb, em, 0, 1, 0);
        }
    }

    private static void lights(Painter p, Body bd) {
        CarStyle s = bd.s;
        Shape sh = s.shape;
        float xq = bd.hx - 0.6F;
        float bq = bd.bot(xq), tq = bd.top(xq), hq = tq - bq;
        float wq = bd.w0(xq);
        int head = sh == Shape.CLASSIC || sh == Shape.LOWRIDER || sh == Shape.MINI ? 0xFFF1C8 : 0xF2F8FF;
        int lens = 0x23262C;
        int chrome = 0xD9DEE4;
        int amber = 0xFFB43A;
        int hs = headStyle(s);
        boolean sportyNose = hs == HEAD_SLIM || hs == HEAD_ANGULAR || hs == HEAD_BAR || hs == HEAD_POPUP;
        // ---- faros delanteros
        switch (hs) {
            case HEAD_ROUND -> {
                float yl = bq + hq * 0.58F;
                float r = Math.min(0.5F, hq * 0.2F);
                for (int side = -1; side <= 1; side += 2) {
                    ovalDecal(p, bd, side * wq * 0.62F, yl, r, r, head, true, true, 0.03F, chrome);
                    ovalDecal(p, bd, side * wq * 0.62F, yl, r * 0.35F, r * 0.35F, 0xFFFFFF, true, true, 0.05F, -1);
                }
            }
            case HEAD_QUAD -> {
                float yl = bq + hq * 0.6F;
                float r = Math.min(0.36F, hq * 0.15F);
                for (int side = -1; side <= 1; side += 2) {
                    frontDecal(p, bd, Math.min(side * wq * 0.33F, side * wq * 0.93F), Math.max(side * wq * 0.33F, side * wq * 0.93F), yl - r - 0.15F,
                            yl + r + 0.15F, 0x15161A, false, true, 3, 0.02F);
                    ovalDecal(p, bd, side * wq * 0.5F, yl, r, r, head, true, true, 0.04F, chrome);
                    ovalDecal(p, bd, side * wq * 0.77F, yl, r, r, head, true, true, 0.04F, chrome);
                }
            }
            case HEAD_POPUP -> {
                // faros escondidos: tapas cerradas en el cofre y cuartos en la defensa
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * wq * 0.42F, zo = side * (wq - 0.35F);
                    float xa = bd.hx - 1.9F, xb = bd.hx - 0.75F;
                    topDecal(p, bd, xa, xb, Math.min(zi, zo), Math.min(zi, zo) + 0.06F, shade(s.body, 0.45F), false, 0.015F);
                    topDecal(p, bd, xa, xb, Math.max(zi, zo) - 0.06F, Math.max(zi, zo), shade(s.body, 0.45F), false, 0.015F);
                    topDecal(p, bd, xa, xa + 0.06F, Math.min(zi, zo), Math.max(zi, zo), shade(s.body, 0.45F), false, 0.015F);
                    float yl = bq + hq * 0.42F;
                    frontDecal(p, bd, Math.min(side * wq * 0.5F, side * (wq - 0.2F)), Math.max(side * wq * 0.5F, side * (wq - 0.2F)), yl, yl + 0.22F,
                            amber, true, true, 3, 0.03F);
                }
            }
            case HEAD_BAR -> {
                float yl = bq + hq * 0.66F;
                frontDecal(p, bd, -wq + 0.2F, wq - 0.2F, yl - 0.12F, yl + 0.22F, lens, false, true, 8, 0.02F);
                frontDecal(p, bd, -wq + 0.25F, wq - 0.25F, yl, yl + 0.08F, head, true, true, 8, 0.035F);
                for (int side = -1; side <= 1; side += 2) {
                    frontDecal(p, bd, Math.min(side * wq * 0.55F, side * (wq - 0.3F)), Math.max(side * wq * 0.55F, side * (wq - 0.3F)), yl - 0.45F,
                            yl - 0.2F, head, true, true, 3, 0.03F);
                }
            }
            case HEAD_ANGULAR -> {
                float yl = bq + hq * 0.6F;
                for (int side = -1; side <= 1; side += 2) {
                    frontDecal(p, bd, Math.min(side * wq * 0.42F, side * (wq - 0.12F)), Math.max(side * wq * 0.42F, side * (wq - 0.12F)), yl - 0.15F,
                            yl + 0.3F, lens, false, true, 4, 0.02F);
                    slantDecal(p, bd, side * wq * 0.45F, yl + 0.18F, side * (wq - 0.2F), yl + 0.02F, 0.08F, head, true, true, 0.04F);
                    slantDecal(p, bd, side * wq * 0.55F, yl - 0.08F, side * (wq - 0.2F), yl + 0.02F, 0.08F, head, true, true, 0.04F);
                }
            }
            case HEAD_SLIM -> {
                float yl = bq + hq * 0.62F;
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * wq * 0.4F, zo = side * (wq - 0.12F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl - 0.1F, yl + 0.3F, lens, false, true, 4, 0.02F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + 0.16F, head, true, true, 4, 0.035F);
                    ovalDecal(p, bd, side * wq * 0.55F, yl + 0.08F, 0.13F, 0.1F, 0xFFFFFF, true, true, 0.05F, -1);
                }
            }
            default -> {
                float yl = bq + hq * 0.56F;
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * wq * 0.42F, zo = side * (wq - 0.15F);
                    float zm = side * (wq - 0.45F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl - 0.08F, yl + 0.58F, lens, false, true, 3, 0.02F);
                    frontDecal(p, bd, Math.min(zi, zm), Math.max(zi, zm), yl, yl + 0.5F, head, true, true, 3, 0.03F);
                    frontDecal(p, bd, Math.min(zm, zo), Math.max(zm, zo), yl, yl + 0.5F, amber, true, true, 2, 0.03F);
                }
            }
        }
        if (s.hasD(D_LIGHT_BAR) && hs != HEAD_BAR) {
            float yl = bq + hq * 0.7F;
            frontDecal(p, bd, -wq * 0.4F, wq * 0.4F, yl, yl + 0.07F, head, true, true, 6, 0.035F);
        }
        // ---- parrilla y tomas de aire
        int trim = s.has(CHROME) ? chrome : 0x1A1B1F;
        if (s.hasD(D_BIG_GRILLE)) {
            float y0g = bq + hq * 0.16F, y1g = bq + hq * 0.6F;
            frontDecal(p, bd, -wq * 0.42F, wq * 0.42F, y0g - 0.1F, y1g + 0.1F, chrome, false, true, 6, 0.02F);
            frontDecal(p, bd, -wq * 0.36F, wq * 0.36F, y0g, y1g, 0x0E0F12, false, true, 6, 0.03F);
            for (int i = 1; i < 6; i++) {
                float yy = lerp(y0g, y1g, i / 6.0F);
                frontDecal(p, bd, -wq * 0.36F, wq * 0.36F, yy, yy + 0.05F, chrome, false, true, 6, 0.04F);
            }
            frontDecal(p, bd, -wq * 0.02F, wq * 0.02F, y0g, y1g, chrome, false, true, 1, 0.045F);
        } else if (s.hasD(D_OVAL_GRILLE)) {
            ovalDecal(p, bd, 0, bq + hq * 0.34F, wq * 0.36F, hq * 0.2F, 0x0E0F12, false, true, 0.02F, chrome);
            frontDecal(p, bd, -wq * 0.3F, wq * 0.3F, bq + hq * 0.33F, bq + hq * 0.35F, chrome, false, true, 4, 0.04F);
        } else if (sportyNose) {
            float yi = bq + 0.2F;
            frontDecal(p, bd, -wq * 0.62F, wq * 0.62F, yi, yi + hq * 0.3F, 0x101114, false, true, 6, 0.02F);
            for (int i = 1; i < 4; i++) {
                float yy = yi + hq * 0.075F * i;
                frontDecal(p, bd, -wq * 0.6F, wq * 0.6F, yy, yy + 0.04F, 0x2A2C32, false, true, 6, 0.03F);
            }
            if (!s.hasD(D_SPLITTER)) {
                p.box(bd.hx - 1.4F, bd.hx + 0.12F, bd.bot(bd.hx) - 0.12F, bd.bot(bd.hx) - 0.02F, -wq + 0.4F, wq - 0.4F, 0x1A1B1F, false);
            }
        } else if (sh == Shape.PICKUP || sh == Shape.RALLY || sh == Shape.SUV || sh == Shape.BOXVAN) {
            float yg = bq + hq * 0.28F;
            frontDecal(p, bd, -wq * 0.42F, wq * 0.42F, yg, yg + hq * 0.42F, trim, false, true, 4, 0.02F);
            for (int i = 0; i < 4; i++) {
                float yy = yg + hq * 0.085F * (i + 1);
                frontDecal(p, bd, -wq * 0.4F, wq * 0.4F, yy, yy + 0.06F, s.has(CHROME) ? 0x7A8088 : 0x3A3C42, false, true, 4, 0.03F);
            }
        } else if (sh == Shape.CLASSIC) {
            ovalDecal(p, bd, 0, bq + hq * 0.3F, Math.min(0.55F, hq * 0.2F), Math.min(0.55F, hq * 0.2F), 0x1A1B1F, false, true, 0.02F, chrome);
        } else {
            float yg = bq + hq * 0.3F;
            frontDecal(p, bd, -wq * 0.34F, wq * 0.34F, yg, yg + hq * 0.2F, trim, false, true, 4, 0.02F);
            frontDecal(p, bd, -wq * 0.3F, wq * 0.3F, yg + hq * 0.09F, yg + hq * 0.11F, 0x55585E, false, true, 4, 0.03F);
            frontDecal(p, bd, -wq * 0.55F, wq * 0.55F, bq + 0.18F, bq + 0.18F + hq * 0.1F, 0x15161A, false, true, 4, 0.02F);
        }
        if (s.hasD(D_FOG)) {
            for (int side = -1; side <= 1; side += 2) {
                ovalDecal(p, bd, side * wq * 0.68F, bq + hq * 0.22F, 0.2F, 0.16F, 0xFFF6D8, true, true, 0.03F, 0x1A1B1F);
            }
        }
        if (!sportyNose && sh != Shape.CLASSIC && sh != Shape.PROTO) {
            // placa delantera
            float yp = bq + hq * 0.14F;
            frontDecal(p, bd, -0.85F, 0.85F, yp, yp + 0.42F, 0xF2F0E6, false, true, 3, 0.035F);
        }
        // ---- defensa trasera, calaveras y placa
        float xt = -bd.hx + 0.6F;
        float bt = bd.bot(xt), tt = bd.top(xt), ht = tt - bt;
        float wt = bd.w0(xt);
        int red = 0xFF2A2A;
        int dred = 0x3A0A0E;
        int ts = tailStyle(s);
        switch (ts) {
            case TAIL_BAR -> {
                float yl = bt + ht * 0.66F;
                frontDecal(p, bd, -wt + 0.25F, wt - 0.25F, yl - 0.08F, yl + 0.24F, dred, false, false, 8, 0.02F);
                frontDecal(p, bd, -wt + 0.3F, wt - 0.3F, yl, yl + 0.12F, red, true, false, 8, 0.035F);
            }
            case TAIL_ROUND2 -> {
                float yl = bt + ht * 0.62F;
                float r = Math.min(0.34F, ht * 0.13F);
                for (int side = -1; side <= 1; side += 2) {
                    ovalDecal(p, bd, side * wt * 0.42F, yl, r, r, red, true, false, 0.03F, 0x2A2C30);
                    ovalDecal(p, bd, side * wt * 0.75F, yl, r, r, red, true, false, 0.03F, 0x2A2C30);
                }
            }
            case TAIL_ROUND1 -> {
                float yl = bt + ht * 0.6F;
                for (int side = -1; side <= 1; side += 2) {
                    ovalDecal(p, bd, side * wt * 0.65F, yl, Math.min(0.32F, ht * 0.14F), Math.min(0.32F, ht * 0.14F), red, true, false, 0.03F, chrome);
                }
            }
            case TAIL_SLIM -> {
                float yl = bt + ht * 0.64F;
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * wt * 0.35F, zo = side * (wt - 0.15F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl - 0.08F, yl + 0.26F, dred, false, false, 4, 0.02F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + 0.12F, red, true, false, 4, 0.035F);
                }
            }
            case TAIL_VERT -> {
                float yl = bt + ht * 0.3F;
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * (wt - 0.55F), zo = side * (wt - 0.1F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + ht * 0.6F, red, true, false, 2, 0.03F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl + ht * 0.25F, yl + ht * 0.33F, 0xF0F0F0, true, false, 2, 0.04F);
                }
            }
            case TAIL_C -> {
                float yl = bt + ht * 0.48F;
                for (int side = -1; side <= 1; side += 2) {
                    float za = side * wt * 0.45F, zb = side * (wt - 0.15F);
                    float lo = Math.min(za, zb), hi = Math.max(za, zb);
                    frontDecal(p, bd, lo, hi, yl + 0.5F, yl + 0.62F, red, true, false, 3, 0.035F);
                    frontDecal(p, bd, lo, hi, yl, yl + 0.12F, red, true, false, 3, 0.035F);
                    float zz = side < 0 ? lo : hi;
                    frontDecal(p, bd, Math.min(zz, zz - side * 0.14F), Math.max(zz, zz - side * 0.14F), yl, yl + 0.62F, red, true, false, 1, 0.035F);
                }
            }
            default -> {
                float yl = bt + ht * 0.56F;
                float hh = 0.45F;
                for (int side = -1; side <= 1; side += 2) {
                    float zi = side * wt * 0.5F, zo = side * (wt - 0.12F);
                    frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + hh, red, true, false, 3, 0.03F);
                    float zr = side * wt * 0.5F, zr2 = side * wt * 0.62F;
                    frontDecal(p, bd, Math.min(zr, zr2), Math.max(zr, zr2), yl, yl + hh, 0xF0F0F0, true, false, 1, 0.04F);
                }
            }
        }
        if (s.hasD(D_LIGHT_BAR) && ts != TAIL_BAR) {
            float yl = bt + ht * 0.7F;
            frontDecal(p, bd, -wt * 0.5F, wt * 0.5F, yl, yl + 0.07F, red, true, false, 6, 0.035F);
        }
        if (s.hasD(D_DIFFUSER) || sportyNose) {
            float y0d = bt + 0.12F, y1d = bt + ht * 0.3F;
            frontDecal(p, bd, -wq * 0.62F, wq * 0.62F, y0d, y1d, 0x101114, false, false, 6, 0.02F);
            if (s.hasD(D_DIFFUSER)) {
                for (int i = -3; i <= 3; i++) {
                    float z = i * wq * 0.17F;
                    frontDecal(p, bd, z - 0.05F, z + 0.05F, y0d, y1d, 0x34363C, false, false, 1, 0.03F);
                }
            }
        }
        if (sh != Shape.PROTO && sh != Shape.HYPER) {
            float yp = bt + ht * 0.22F;
            frontDecal(p, bd, -0.95F, 0.95F, yp, yp + 0.55F, 0xF2F0E6, false, false, 3, 0.03F);
            frontDecal(p, bd, -0.6F, 0.6F, yp + 0.2F, yp + 0.32F, 0x30343C, false, false, 2, 0.04F);
        }
        // cuartos laterales (pequeñas luces ámbar en los costados del frente)
        if (!sportyNose) {
            float mx = bd.hx - 1.0F;
            float my = lerp(bd.bot(mx), bd.top(mx), 0.5F);
            sideDecal(p, bd, new float[][] {{mx - 0.35F, my}, {mx + 0.05F, my}, {mx + 0.05F, my + 0.18F}, {mx - 0.35F, my + 0.18F}}, amber, true, 0.03F);
        }
    }

    /** Piezas de los autos exclusivos: motor al aire, flamas, jaula, suspensión, aleta de tiburón, blindaje, antena... */
    private static void special(Painter p, Body bd, Cabin cab) {
        CarStyle s = bd.s;
        float hx = bd.hx;
        int chromeC = 0xDDE3EA;
        int dark = 0x16171A;
        if (s.openWheels && !bd.formula) {
            // brazos de suspensión desde la carrocería hasta cada llanta
            for (float[] m : wheelMounts(s)) {
                float zb = (float) Math.copySign(Math.max(0.3F, bd.w0(m[0]) - 0.3F), m[3]);
                float zw = m[2];
                float y = m[1];
                float yb = Math.max(bd.bot(m[0]) + 0.2F, y - 0.1F);
                p.box(m[0] - 0.12F, m[0] + 0.12F, Math.min(y, yb) - 0.08F, Math.max(y, yb) + 0.08F, Math.min(zb, zw), Math.max(zb, zw), 0x2A2C30, false);
                p.box(m[0] - 0.3F, m[0] + 0.3F, y + m[1] * 0.4F, y + m[1] * 0.4F + 0.14F, Math.min(zb, zw) + 0.1F, Math.max(zb, zw) - 0.1F,
                        s.has(CHROME) ? chromeC : 0xC0281E, false);
            }
        }
        if (s.hasD(D_ENGINE_OUT)) {
            // motor V8 al aire con sobrealimentador, toma de aire y escapes de tubo
            float ex = s.cabFront + 1.4F;
            float ey = bd.top(ex);
            float bw = 0.9F;
            p.box(ex - 1.2F, ex + 1.2F, ey - 0.2F, ey + 0.7F, -bw, bw, 0x3A3D44, false);
            p.box(ex - 1.1F, ex + 1.1F, ey + 0.7F, ey + 1.1F, -0.55F, 0.55F, chromeC, false);
            p.box(ex - 0.5F, ex + 0.5F, ey + 1.1F, ey + 1.7F, -0.45F, 0.45F, chromeC, false);
            p.box(ex - 0.4F, ex + 0.4F, ey + 1.7F, ey + 1.8F, -0.35F, 0.35F, 0x111214, false);
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < 4; i++) {
                    float x = ex - 0.9F + i * 0.6F;
                    float z = side * (bw + 0.2F);
                    p.tubeX(x, x + 0.01F, ey + 0.3F, z, 0.01F, 4, chromeC, chromeC, false);
                    p.box(x - 0.1F, x + 0.1F, ey + 0.15F, ey + 0.35F, Math.min(z, side * bw), Math.max(z, side * bw), chromeC, false);
                    p.box(x - 0.1F, x + 0.1F, ey + 0.15F, ey + 1.0F, z - 0.1F, z + 0.1F, chromeC, false);
                    p.box(x - 0.08F, x + 0.08F, ey + 0.98F, ey + 1.02F, z - 0.08F, z + 0.08F, 0x2A1208, false);
                }
            }
        }
        if (s.hasD(D_FLAMES)) {
            int[] cols = {0xFFD23F, 0xFF8A1F, 0xE8331B};
            float x0 = hx - 0.8F;
            for (int k = 0; k < 3; k++) {
                for (int i = 0; i < 5; i++) {
                    float y = lerp(bd.bot(x0) + 0.3F, bd.top(x0) - 0.25F, (i + 0.5F) / 5.0F);
                    float len = 2.6F + (i % 2) * 1.4F + (i == 2 ? 1.2F : 0) - k * 0.6F;
                    float hh = 0.22F - k * 0.05F;
                    sideDecal(p, bd, new float[][] {{x0 - k * 0.2F, y - hh}, {x0 - k * 0.2F, y + hh}, {x0 - len - k * 0.2F, y + hh * 0.2F}}, cols[k], false,
                            0.03F + k * 0.005F);
                }
            }
        }
        if (s.hasD(D_CAGE) && cab != null) {
            // jaula de tubos sobre la cabina abierta
            float cf = s.cabFront, cr = s.cabRear;
            float y = bd.top((cf + cr) / 2);
            float h = Math.max(1.6F, s.roofH + 0.6F);
            float zw = bd.w0((cf + cr) / 2) - 0.45F;
            int tube = s.hasD(D_DISH) ? 0xDADDE2 : 0x2A2C30;
            for (int side = -1; side <= 1; side += 2) {
                float z = side * zw;
                p.hexa(cf - 0.15F, cf + 0.05F, z - 0.08F, z + 0.08F, y, cf - 0.9F, cf - 0.7F, z - 0.08F, z + 0.08F, y + h, tube, false);
                p.box(cr, cr + 0.2F, y, y + h, z - 0.08F, z + 0.08F, tube, false);
                p.box(cr, cf - 0.7F, y + h - 0.1F, y + h + 0.06F, z - 0.08F, z + 0.08F, tube, false);
            }
            p.box(cr, cr + 0.2F, y + h - 0.1F, y + h + 0.06F, -zw, zw, tube, false);
            p.box(cf - 0.9F, cf - 0.7F, y + h - 0.1F, y + h + 0.06F, -zw, zw, tube, false);
            if (s.hasD(D_DISH)) {
                // antena de plato y bandera
                float ax = cr + 0.1F;
                p.box(ax - 0.05F, ax + 0.05F, y + h, y + h + 1.4F, -0.05F, 0.05F, tube, false);
                p.ringX(ax - 0.06F, y + h + 1.6F, 0, 0, 0.9F, 14, 0xEDEDED, false, -1);
                p.ringX(ax - 0.04F, y + h + 1.6F, 0, 0, 0.9F, 14, 0xC9CCD2, false, 1);
                p.box(ax - 0.3F, ax - 0.06F, y + h + 1.55F, y + h + 1.65F, -0.05F, 0.05F, tube, false);
                float fx = s.cabFront - 0.2F;
                p.box(fx, fx + 0.04F, y, y + h + 1.8F, zw - 0.04F, zw, 0xDADDE2, false);
                p.box(fx - 1.0F, fx, y + h + 1.2F, y + h + 1.8F, zw - 0.03F, zw - 0.01F, s.accent, false);
            }
        }
        if (s.hasD(D_JET)) {
            // tobera de cohete atrás (la llama se anima)
            float y = lerp(bd.bot(-hx), bd.top(-hx + 1), 0.55F);
            p.tubeX(-hx + 0.3F, -hx - 1.0F, y, 0, 0.75F, 14, 0x3A3D44, 0x1A0A06, false);
            p.tubeX(-hx - 0.9F, -hx - 1.05F, y, 0, 0.85F, 14, 0x9AA0A8, 0x1A0A06, false);
        }
        if (s.hasD(D_SHARK)) {
            // aleta dorsal, branquias y dientes
            float fx = cab != null ? (cab.rr + cab.rf) / 2 : 0;
            float fy = cab != null ? cab.yt + 0.05F : bd.y1;
            p.hexa(fx - 1.4F, fx + 0.6F, -0.12F, 0.12F, fy - 0.1F, fx - 1.6F, fx - 0.9F, -0.06F, 0.06F, fy + 2.2F, shade(s.body, 0.9F), false);
            for (int i = 0; i < 3; i++) {
                float gx = s.cabFront + 1.0F + i * 0.55F;
                float yb = lerp(bd.bot(gx), bd.top(gx), 0.3F), yt = lerp(bd.bot(gx), bd.top(gx), 0.8F);
                sideDecal(p, bd, new float[][] {{gx, yb}, {gx + 0.12F, yb}, {gx + 0.32F, yt}, {gx + 0.2F, yt}}, dark, false, 0.03F);
            }
            float xq = hx - 0.5F;
            float bq = bd.bot(xq), tq = bd.top(xq);
            float wq = bd.w0(xq);
            float ym = lerp(bq, tq, 0.35F);
            frontDecal(p, bd, -wq * 0.7F, wq * 0.7F, ym - 0.35F, ym + 0.35F, 0x1A0B0E, false, true, 6, 0.02F);
            for (int i = 0; i < 7; i++) {
                float z = lerp(-wq * 0.6F, wq * 0.6F, i / 6.0F);
                slantDecal(p, bd, z - 0.14F, ym + 0.35F, z, ym + 0.05F, 0.0F, 0xF4F4F0, false, true, 0.03F);
                frontDecal(p, bd, z - 0.12F, z + 0.12F, ym + 0.12F, ym + 0.35F, 0xF4F4F0, false, true, 1, 0.035F);
                frontDecal(p, bd, z - 0.1F, z + 0.1F, ym - 0.35F, ym - 0.14F, 0xF4F4F0, false, true, 1, 0.035F);
            }
        }
        if (s.hasD(D_ARMOR)) {
            // placas de blindaje en los costados con remaches y rejilla frontal
            for (int k = 0; k < 3; k++) {
                float x0 = lerp(-hx + 1.2F, hx - 1.4F, k / 3.0F) + 0.15F, x1 = lerp(-hx + 1.2F, hx - 1.4F, (k + 1) / 3.0F) - 0.15F;
                float yb = lerp(bd.bot(x0), bd.top(x0), 0.2F), yt = lerp(bd.bot(x0), bd.top(x0), 0.62F);
                sideDecal(p, bd, new float[][] {{x0, yb}, {x1, yb}, {x1 - 0.2F, yt}, {x0 + 0.2F, yt}}, shade(s.body, 1.35F), false, 0.04F);
                for (int r = 0; r < 2; r++) {
                    float rx = r == 0 ? x0 + 0.25F : x1 - 0.25F;
                    sideDecal(p, bd, new float[][] {{rx - 0.06F, yb + 0.1F}, {rx + 0.06F, yb + 0.1F}, {rx + 0.06F, yb + 0.22F}, {rx - 0.06F, yb + 0.22F}},
                            0x8A8F98, false, 0.05F);
                }
            }
            float xq = hx - 0.5F;
            float wq = bd.w0(xq);
            float yq = lerp(bd.bot(xq), bd.top(xq), 0.3F);
            for (int i = 0; i < 5; i++) {
                float y = yq + i * 0.14F;
                frontDecal(p, bd, -wq * 0.55F, wq * 0.55F, y, y + 0.06F, 0x3A3D44, false, true, 4, 0.03F);
            }
        }
    }

    /** Detalles extra de cada modelo (tomas laterales, rejillas, difusor, faldones, snorkel, llanta de refacción...). */
    private static void details(Painter p, Body bd, Cabin cab) {
        CarStyle s = bd.s;
        float hx = bd.hx;
        float R = s.wheelR;
        int dark = 0x0E0F12;
        if (s.hasD(D_SIDE_INTAKE)) {
            float xr = s.wheelRear + R + 0.45F;
            float x0 = Math.min(xr + 2.6F, s.cabFront - 1.0F);
            float yb = bd.bot(xr) + (bd.y1 - bd.y0) * 0.22F, yt = bd.bot(xr) + (bd.y1 - bd.y0) * 0.78F;
            sideDecal(p, bd, new float[][] {{xr, yb}, {xr, yt}, {x0, yt - 0.15F}, {x0 - 0.5F, yb + 0.35F}}, dark, false, 0.025F);
            for (int i = 1; i < 4; i++) {
                float y = lerp(yb + 0.2F, yt - 0.25F, i / 4.0F);
                sideDecal(p, bd, new float[][] {{xr + 0.05F, y}, {xr + 0.05F, y + 0.06F}, {x0 - 0.4F, y + 0.06F}, {x0 - 0.4F, y}}, 0x2E3036, false, 0.035F);
            }
        }
        if (s.hasD(D_C_LINE)) {
            float cx = (s.cabFront + s.cabRear) / 2 - 0.2F;
            float cy = lerp(bd.y0, bd.y1, 0.5F);
            float rx = Math.abs(s.cabFront - s.cabRear) * 0.42F, ry = (bd.y1 - bd.y0) * 0.38F;
            int n = 12;
            for (int i = 0; i < n; i++) {
                double a0 = Math.toRadians(80 + 200.0 * i / n), a1 = Math.toRadians(80 + 200.0 * (i + 1) / n);
                float x0 = cx + (float) Math.cos(a0) * rx, y0 = cy + (float) Math.sin(a0) * ry;
                float x1 = cx + (float) Math.cos(a1) * rx, y1 = cy + (float) Math.sin(a1) * ry;
                float x2 = cx + (float) Math.cos(a1) * (rx - 0.3F), y2 = cy + (float) Math.sin(a1) * (ry - 0.18F);
                float x3 = cx + (float) Math.cos(a0) * (rx - 0.3F), y3 = cy + (float) Math.sin(a0) * (ry - 0.18F);
                sideDecal(p, bd, new float[][] {{x0, y0}, {x1, y1}, {x2, y2}, {x3, y3}}, s.accent, false, 0.03F);
            }
        }
        if (s.hasD(D_HOOD_VENTS)) {
            float xa = s.cabFront + 0.6F, xb = Math.min(hx - 1.3F, xa + 1.4F);
            for (int side = -1; side <= 1; side += 2) {
                float z0 = side * 0.45F, z1 = side * 1.25F;
                topDecal(p, bd, xa, xb, Math.min(z0, z1), Math.max(z0, z1), dark, false, 0.02F);
                for (int i = 1; i < 4; i++) {
                    float xx = lerp(xa, xb, i / 4.0F);
                    topDecal(p, bd, xx, xx + 0.07F, Math.min(z0, z1), Math.max(z0, z1), 0x3A3C42, false, 0.03F);
                }
            }
        }
        if (s.hasD(D_HOOD_LINES)) {
            float xa = s.cabFront + 0.2F, xb = hx - 0.9F;
            for (int side = -1; side <= 1; side += 2) {
                float z = side * 0.95F;
                topDecal(p, bd, xa, xb, z - 0.04F, z + 0.04F, shade(s.body, 0.55F), false, 0.015F);
            }
        }
        if (s.hasD(D_SPLITTER)) {
            float w = bd.w0(hx - 0.5F) - 0.05F;
            float y = bd.bot(hx - 0.3F);
            p.box(hx - 1.3F, hx + 0.3F, y - 0.14F, y - 0.03F, -w, w, 0x16171A, false);
        }
        if (s.hasD(D_ENGINE_GLASS) && cab != null) {
            float xa = -hx + 1.6F, xb = s.cabRear - 0.15F;
            if (xb > xa + 0.5F) {
                float zw = Math.min(1.5F, bd.w0(xa) - 0.9F);
                topDecal(p, bd, xa, xb, -zw, zw, 0x14181E, false, 0.02F);
                for (int i = 0; i < 3; i++) {
                    float xx = lerp(xa + 0.2F, xb - 0.2F, i / 2.0F);
                    topDecal(p, bd, xx - 0.12F, xx + 0.12F, -zw * 0.7F, zw * 0.7F, s.has(GOLD_SPARKLE) ? 0xFFE58A : 0xB0141E, false, 0.03F);
                }
                for (int i = 0; i < 5; i++) {
                    float xx = lerp(xa, xb, i / 4.0F);
                    topDecal(p, bd, xx - 0.03F, xx + 0.03F, -zw, zw, shade(s.body, 0.5F), false, 0.04F);
                }
            }
        }
        if (s.hasD(D_LOUVERS) && cab != null) {
            for (int i = 0; i < 6; i++) {
                float x = lerp(s.cabRear + 0.1F, cab.rr - 0.05F, (i + 0.5F) / 6.0F);
                float[] q = cab.at(x);
                float[][] sc = Cabin.sec(q);
                float z = sc[2][0];
                float y = q[2] + 0.04F;
                p.box(x - 0.07F, x + 0.07F, y - 0.06F, y + 0.06F, -z, z, 0x141518, false);
            }
        }
        if (s.hasD(D_ROOF_SCOOP) && cab != null) {
            float x0 = cab.rr + 0.2F, x1 = Math.min(cab.rf - 0.2F, x0 + 2.2F);
            float y = cab.yt + Cabin.crownOf(s) - 0.05F;
            p.hexa(x0, x1, -0.7F, 0.7F, y, x0, x1 - 0.8F, -0.55F, 0.55F, y + 0.6F, s.accent, false);
            p.box(x1 - 0.85F, x1 - 0.75F, y + 0.08F, y + 0.52F, -0.45F, 0.45F, dark, false);
        }
        if (s.hasD(D_RAILS) && cab != null) {
            float y = cab.yt + 0.05F;
            float z = cab.at((cab.rr + cab.rf) / 2)[4] - 0.25F;
            for (int side = -1; side <= 1; side += 2) {
                p.box(cab.rr + 0.2F, cab.rf - 0.2F, y + 0.25F, y + 0.4F, side * z - 0.12F, side * z + 0.12F, 0x2A2C30, false);
                p.box(cab.rr + 0.2F, cab.rr + 0.45F, y, y + 0.4F, side * z - 0.12F, side * z + 0.12F, 0x2A2C30, false);
                p.box(cab.rf - 0.45F, cab.rf - 0.2F, y, y + 0.4F, side * z - 0.12F, side * z + 0.12F, 0x2A2C30, false);
            }
        }
        if (s.hasD(D_SNORKEL) && cab != null) {
            float x = s.cabFront + 0.4F;
            float z = bd.w(x) + 0.1F;
            float y0 = bd.top(x) - 0.6F;
            p.box(x - 0.18F, x + 0.18F, y0, y0 + 0.7F, z - 0.1F, z + 0.25F, 0x1A1B1F, false);
            float[] q = cab.at(s.cabFront - 0.4F);
            float yt = q[2] + 0.3F;
            p.hexa(x - 0.18F, x + 0.18F, z - 0.1F, z + 0.25F, y0 + 0.6F, s.cabFront - 0.7F, s.cabFront - 0.35F, q[3] - 0.05F, q[3] + 0.3F, yt, 0x1A1B1F, false);
            p.box(s.cabFront - 0.9F, s.cabFront - 0.3F, yt - 0.05F, yt + 0.3F, q[3] - 0.05F, q[3] + 0.3F, 0x1A1B1F, false);
        }
        if (s.hasD(D_SPARE)) {
            float x = -hx - 0.05F;
            float y = lerp(bd.y0, bd.y1, 0.62F);
            float r = R * 0.95F;
            p.ringX(x - 0.55F, y, 0, 0, r, 14, 0x17181B, false, -1);
            p.tubeX(x, x - 0.55F, y, 0, r, 14, 0x17181B, 0x17181B, false);
            p.ringX(x - 0.57F, y, 0, r * 0.4F, r * 0.72F, 14, s.accent, false, -1);
            p.ringX(x - 0.58F, y, 0, 0, r * 0.25F, 10, s.rim, false, -1);
        }
        if (s.hasD(D_WIPERS) && cab != null) {
            float x = s.cabFront - 0.25F;
            float y = bd.top(x) + 0.06F;
            for (int i = 0; i < 2; i++) {
                float z0 = -1.4F + i * 1.5F;
                p.face(new float[] {x, y, z0}, new float[] {x - 0.45F, y + 0.32F, z0 + 1.1F}, new float[] {x - 0.43F, y + 0.37F, z0 + 1.1F},
                        new float[] {x + 0.02F, y + 0.05F, z0}, 0x121214, false, 1, 1, 0);
            }
        }
        if (s.hasD(D_RACE_DECALS)) {
            float x0 = s.cabFront - 0.4F, x1 = s.wheelRear + R + 0.6F;
            float y = lerp(bd.y0, bd.y1, 0.22F);
            sideDecal(p, bd, new float[][] {{x1, y}, {x0, y}, {x0, y + 0.2F}, {x1, y + 0.2F}}, s.accent, false, 0.03F);
            float xs = lerp(x1, x0, 0.25F);
            sideDecal(p, bd, new float[][] {{xs, y + 0.35F}, {xs + 1.2F, y + 0.35F}, {xs + 1.2F, y + 0.7F}, {xs, y + 0.7F}}, 0xF4F4F4, false, 0.03F);
            sideDecal(p, bd, new float[][] {{xs + 0.15F, y + 0.43F}, {xs + 1.05F, y + 0.43F}, {xs + 1.05F, y + 0.62F}, {xs + 0.15F, y + 0.62F}}, 0xD7263D,
                    false, 0.04F);
        }
        // tapa de gasolina (lado derecho, sobre la rueda de atrás)
        if (cab != null) {
            float fx = s.wheelRear + R + 0.2F;
            float fy = lerp(bd.bot(fx), bd.top(fx), 0.72F);
            float z = bd.surfZ(fx, fy);
            if (z > 0) {
                p.ringZ(fx, fy, z + 0.03F, 0, 0.26F, 10, 0, shade(s.body, 0.7F), false, 1);
                p.ringZ(fx, fy, z + 0.035F, 0.2F, 0.26F, 10, 0, shade(s.body, 0.45F), false, 1);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------- puertas y espejos

    private static void sideLine(Painter p, Body bd, float x, float ya, float yb, float half, int rgb, float off) {
        int steps = 4;
        for (int i = 0; i < steps; i++) {
            float y0 = lerp(ya, yb, i / (float) steps), y1 = lerp(ya, yb, (i + 1) / (float) steps);
            float za0 = bd.surfZ(x - half, y0), zb0 = bd.surfZ(x + half, y0), zb1 = bd.surfZ(x + half, y1), za1 = bd.surfZ(x - half, y1);
            if (za0 < 0 || zb0 < 0 || zb1 < 0 || za1 < 0) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                p.face(new float[] {x - half, y0, side * (za0 + off)}, new float[] {x + half, y0, side * (zb0 + off)},
                        new float[] {x + half, y1, side * (zb1 + off)}, new float[] {x - half, y1, side * (za1 + off)}, rgb, false, 0, 0, side);
            }
        }
    }

    private static void doors(Painter p, Body bd, Cabin cab, int upper) {
        CarStyle s = bd.s;
        if (cab == null) {
            return;
        }
        Shape sh = s.shape;
        int line = shade(s.body, 0.42F);
        int handle = s.has(CHROME) ? 0xE6EAEF : shade(s.body, 0.62F);
        float cf = s.cabFront;
        float front = cf - 0.25F;
        boolean fourDoor = sh == Shape.SEDAN || sh == Shape.HATCH || sh == Shape.RALLY || sh == Shape.VAN;
        float[] edges;
        if (fourDoor) {
            float mid = (cab.rr + cab.rf) / 2 - 0.15F;
            edges = new float[] {front, mid, Math.max(s.cabRear + 0.35F, mid - 3.2F)};
        } else if (sh == Shape.PICKUP) {
            edges = new float[] {front, s.cabRear + 0.2F};
        } else {
            edges = new float[] {front, Math.max(cab.rr - 0.1F, front - 4.2F)};
        }
        for (float x : edges) {
            float b = bd.bot(x) + 0.3F, t = bd.top(x) - 0.12F;
            if (t > b) {
                sideLine(p, bd, x, b, t, 0.035F, line, 0.012F);
            }
        }
        // manijas, justo antes del borde trasero de cada puerta
        for (int i = 1; i < edges.length; i++) {
            float x = edges[i] + 0.35F;
            float y = lerp(bd.bot(x), bd.top(x), 0.72F);
            sideLine(p, bd, x + 0.25F, y, y + 0.14F, 0.25F, handle, 0.03F);
        }
        // espejos
        float mx = cf - 0.45F;
        float yb = bd.top(mx);
        float zin = cab.at(mx)[3];
        float zout = bd.w(mx) + 0.4F;
        if (sh == Shape.VAN) {
            mx = cf + 0.25F;
            yb = bd.top(mx) + 0.3F;
            zin = bd.w(mx) - 0.2F;
            zout = bd.w(mx) + 0.4F;
        }
        if (s.hasD(D_ARMOR) || Cabin.open(s)) {
            return;
        }
        int mc = paint(bd, upper, yb + 0.5F);
        for (int side = -1; side <= 1; side += 2) {
            // brazo delgado que sale de la base de la ventana
            float za = side * zin, zb = side * (zout - 0.3F);
            p.hexa(mx - 0.1F, mx + 0.08F, Math.min(za, zb), Math.max(za, zb), yb + 0.02F, mx - 0.06F, mx + 0.04F, Math.min(za, zb), Math.max(za, zb),
                    yb + 0.16F, 0x1A1B1F, false);
            // carcasa en forma de gota: más delgada adelante, con el espejo atrás
            float zo = side * zout, zi = side * (zout - 0.48F);
            p.hexa(mx - 0.32F, mx + 0.12F, Math.min(zo, zi), Math.max(zo, zi), yb + 0.08F, mx - 0.3F, mx - 0.02F, Math.min(zo, zi) + 0.04F,
                    Math.max(zo, zi) - 0.04F, yb + 0.42F, mc, false);
            p.face(new float[] {mx - 0.33F, yb + 0.12F, zo - side * 0.04F}, new float[] {mx - 0.33F, yb + 0.12F, zi + side * 0.04F},
                    new float[] {mx - 0.31F, yb + 0.38F, zi + side * 0.06F}, new float[] {mx - 0.31F, yb + 0.38F, zo - side * 0.06F}, 0xA9C0D6, false, -1, 0,
                    0);
        }
    }

    // ------------------------------------------------------------------------------------------------------------- ruedas

    /** Radio y ancho de la llanta en el eje x (las de adelante pueden ser distintas). */
    static float radiusAt(CarStyle s, float wx) {
        return wx == s.wheelFront && s.wheelRF > 0 ? s.wheelRF : s.wheelR;
    }

    static float widthAt(CarStyle s, float wx) {
        return wx == s.wheelFront && s.wheelWF > 0 ? s.wheelWF : s.wheelW;
    }

    /** Los ejes del auto (adelante, atrás y, si tiene, uno en medio). */
    static float[] axles(CarStyle s) {
        if (s.hasD(D_HOVER)) {
            return new float[0];
        }
        return Float.isNaN(s.axleMid) ? new float[] {s.wheelFront, s.wheelRear} : new float[] {s.wheelFront, s.axleMid, s.wheelRear};
    }

    private static boolean openWheels(Body bd) {
        return bd.formula || bd.s.openWheels;
    }

    private static float archZ(Body bd, float wx) {
        float ra = radiusAt(bd.s, wx) + 0.3F;
        float m = 0;
        for (int i = 0; i <= 8; i++) {
            float x = wx - ra + 2 * ra * i / 8.0F;
            m = Math.max(m, bd.w(x) + bd.sh);
        }
        return m;
    }

    /** z de la cara de afuera de la llanta que va en wx. */
    private static float wheelOuter(Body bd, float wx) {
        CarStyle s = bd.s;
        if (openWheels(bd)) {
            return bd.hz + s.track + 0.05F;
        }
        return Math.max(bd.hz + s.track + 0.05F, archZ(bd, wx) + 0.1F);
    }

    /**
     * Dónde van las llantas: {x del eje, y del centro (= radio), z del centro de la llanta, lado (-1/1), 1 si es delantera, ancho}.
     */
    public static float[][] wheelMounts(CarStyle s) {
        Body bd = new Body(s);
        float[] ax = axles(s);
        float[][] out = new float[ax.length * 2][];
        int i = 0;
        for (float wx : ax) {
            float R = radiusAt(s, wx), W = widthAt(s, wx);
            float outer = wheelOuter(bd, wx);
            for (int side = -1; side <= 1; side += 2) {
                out[i++] = new float[] {wx, R, side * (outer - W / 2), side, wx == s.wheelFront ? 1 : 0, W};
            }
        }
        return out;
    }

    /** Una llanta suelta con el centro en el origen (para hacerla girar y doblar mientras se maneja). */
    public static void buildWheel(CarStyle s, Sink sink, int mode, int side, float R, float W) {
        Painter p = new Painter(sink, mode);
        float hw = W / 2;
        wheel(p, s, 0, 0, R, side * hw, -side * hw, side, s.has(GOLD_SPARKLE));
    }

    /** Pasos de rueda oscuros con su borde, y los calipers de freno (que no giran con la llanta). */
    private static void arches(Painter p, Body bd) {
        CarStyle s = bd.s;
        Shape sh = s.shape;
        boolean gold = s.has(GOLD_SPARKLE);
        boolean sportBrake = sporty(sh) || sh == Shape.RALLY || sh == Shape.MUSCLE || sh == Shape.FORMULA || s.caliper >= 0;
        for (float wx : axles(s)) {
            float R = radiusAt(s, wx);
            float outer = wheelOuter(bd, wx);
            if (sportBrake) {
                int cal = s.caliper >= 0 ? s.caliper : gold ? 0xFFD34D : s.has(NEON) ? s.neon : 0xD7262E;
                for (int side = -1; side <= 1; side += 2) {
                    float zo = side * outer;
                    float ca0 = zo - side * 0.18F, ca1 = zo - side * 0.12F;
                    p.box(wx - R * 0.42F, wx - R * 0.16F, R + R * 0.12F, R + R * 0.52F, Math.min(ca0, ca1), Math.max(ca0, ca1), cal, false);
                }
            }
            if (openWheels(bd)) {
                continue;
            }
            float az = archZ(bd, wx);
            float ra = R + 0.28F;
            int n = 14;
            float cyB = bd.bot(wx);
            boolean cladding = sh == Shape.RALLY || sh == Shape.PICKUP || s.hasD(D_GUARDS);
            float lip = cladding ? 0.34F : 0.13F;
            int lipCol = cladding ? 0x26272B : shade(s.body, 0.9F);
            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i < n; i++) {
                    double a0 = Math.PI * i / n, a1 = Math.PI * (i + 1) / n;
                    float x0 = wx + (float) Math.cos(a0) * ra, x1 = wx + (float) Math.cos(a1) * ra;
                    float y0 = clamp(R + (float) Math.sin(a0) * ra, bd.bot(x0), bd.top(x0) - 0.3F);
                    float y1 = clamp(R + (float) Math.sin(a1) * ra, bd.bot(x1), bd.top(x1) - 0.3F);
                    float z0 = side * (bd.w(x0) + bd.sh + 0.012F), z1 = side * (bd.w(x1) + bd.sh + 0.012F);
                    float zc = side * (az + 0.012F);
                    p.face(new float[] {wx, cyB, zc}, new float[] {x0, y0, z0}, new float[] {x1, y1, z1}, new float[] {x1, y1, z1}, 0x0B0B0D, false, 0, 0,
                            side);
                    float ylo = Math.min(y0, y1);
                    if (ylo > cyB + 0.05F) {
                        float X0 = wx + (float) Math.cos(a0) * (ra + lip), X1 = wx + (float) Math.cos(a1) * (ra + lip);
                        float Y0 = Math.min(R + (float) Math.sin(a0) * (ra + lip), bd.top(X0) - 0.12F);
                        float Y1 = Math.min(R + (float) Math.sin(a1) * (ra + lip), bd.top(X1) - 0.12F);
                        float zl0 = side * (bd.w(x0) + bd.sh + 0.02F), zl1 = side * (bd.w(x1) + bd.sh + 0.02F);
                        float zL0 = side * (bd.w(X0) + bd.sh + 0.02F), zL1 = side * (bd.w(X1) + bd.sh + 0.02F);
                        p.face(new float[] {x0, y0, zl0}, new float[] {X0, Y0, zL0}, new float[] {X1, Y1, zL1}, new float[] {x1, y1, zl1}, paint(bd, lipCol, Y0),
                                false, 0, 0, side);
                    }
                }
            }
        }
    }

    private static int rimStyle(CarStyle s) {
        if (s.rimStyle >= 0) {
            return s.rimStyle;
        }
        return switch (s.shape) {
            case CLASSIC, MINI, VAN -> RIM_HUB;
            case LOWRIDER -> RIM_WIRE;
            case PICKUP, RALLY, FORMULA -> RIM_SIX;
            case SUV, BOXVAN -> RIM_STEEL;
            case HATCH, SEDAN, WAGON -> RIM_SEVEN;
            default -> RIM_TWIN5;
        };
    }

    private static void spoke(Painter p, float cx, float cy, float z, double a0, double a1, float r0, float r1, float w0, float w1, int rgb, boolean em,
            int side) {
        float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
        float[] A = {cx + c0 * r0 - s0 * w0, cy + s0 * r0 + c0 * w0, z};
        float[] B = {cx + c1 * r1 - s1 * w1, cy + s1 * r1 + c1 * w1, z};
        float[] C = {cx + c1 * r1 + s1 * w1, cy + s1 * r1 - c1 * w1, z};
        float[] D = {cx + c0 * r0 + s0 * w0, cy + s0 * r0 - c0 * w0, z};
        p.face(A, B, C, D, rgb, em, 0, 0, side);
    }

    private static void wheel(Painter p, CarStyle s, float cx, float cy, float R, float zo, float zi, int side, boolean gold) {
        Shape sh = s.shape;
        int n = 16;
        float rot = (float) (Math.PI / n);
        int tire = 0x17181B;
        int wall = 0x24252A;
        int rim = s.rim;
        float zs = side;
        p.tubeZ(cx, cy, zi, zo - zs * 0.16F, R, R, n, rot, tire, false);
        p.tubeZ(cx, cy, zo - zs * 0.16F, zo, R, R * 0.9F, n, rot, wall, false);
        p.ringZ(cx, cy, zi, 0, R, n, rot, tire, false, -side);
        p.ringZ(cx, cy, zo, R * 0.7F, R * 0.9F, n, rot, wall, false, side);
        // letras blancas en la llanta (deportivos y muscle) o banda blanca (clásicos)
        if (sh == Shape.CLASSIC || sh == Shape.LOWRIDER) {
            p.ringZ(cx, cy, zo + zs * 0.004F, R * 0.73F, R * 0.86F, n, rot, 0xF1EFE6, false, side);
        } else if (sh == Shape.MUSCLE || sh == Shape.RALLY) {
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3 + 0.3;
                spoke(p, cx, cy, zo + zs * 0.004F, a, a + 0.35, R * 0.78F, R * 0.78F, R * 0.035F, R * 0.035F, 0xE8E8E0, false, side);
            }
        }
        p.ringZ(cx, cy, zo - zs * 0.22F, 0, R * 0.7F, n, rot, 0x1C1D21, false, side);
        if (sporty(sh) || sh == Shape.RALLY || sh == Shape.MUSCLE || sh == Shape.FORMULA || s.caliper >= 0) {
            p.ringZ(cx, cy, zo - zs * 0.2F, R * 0.12F, R * 0.5F, n, rot, 0x8C9097, false, side);
            for (int i = 0; i < 8; i++) {
                double a = i * Math.PI / 4;
                spoke(p, cx, cy, zo - zs * 0.195F, a, a, R * 0.2F, R * 0.47F, R * 0.015F, R * 0.015F, 0x5A5E66, false, side);
            }
        }
        int style = rimStyle(s);
        int lip = shade(rim, 1.1F);
        float zsp = zo - zs * 0.06F;
        if (style == RIM_HUB || style == RIM_STEEL) {
            boolean steel = style == RIM_STEEL;
            p.ringZ(cx, cy, zo - zs * 0.02F, R * 0.6F, R * 0.7F, n, rot, steel ? shade(rim, 0.8F) : lip, gold, side);
            p.ringZ(cx, cy, zsp, 0, R * 0.62F, n, rot, steel ? shade(rim, 0.85F) : rim, gold, side);
            if (steel) {
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3;
                    p.ringZ(cx + (float) Math.cos(a) * R * 0.42F, cy + (float) Math.sin(a) * R * 0.42F, zsp + zs * 0.005F, 0, R * 0.09F, 8, 0, 0x101114, false,
                            side);
                }
                p.ringZ(cx, cy, zsp + zs * 0.01F, 0, R * 0.2F, 8, 0, shade(rim, 1.1F), false, side);
            } else {
                p.ringZ(cx, cy, zo - zs * 0.045F, R * 0.36F, R * 0.42F, n, rot, shade(rim, 0.75F), false, side);
                p.ringZ(cx, cy, zo - zs * 0.04F, 0, R * 0.18F, 8, 0, shade(rim, 1.15F), gold, side);
            }
            return;
        }
        p.ringZ(cx, cy, zo - zs * 0.02F, R * 0.6F, R * 0.7F, n, rot, lip, gold, side);
        switch (style) {
            case RIM_MULTI -> {
                for (int i = 0; i < 10; i++) {
                    double a = Math.PI / 2 + i * Math.PI / 5;
                    spoke(p, cx, cy, zsp, a, a, R * 0.17F, R * 0.62F, R * 0.035F, R * 0.045F, rim, gold, side);
                }
            }
            case RIM_MESH, RIM_WIRE -> {
                int k = style == RIM_WIRE ? 18 : 12;
                float w = style == RIM_WIRE ? R * 0.012F : R * 0.028F;
                for (int i = 0; i < k; i++) {
                    double a = i * 2 * Math.PI / k;
                    spoke(p, cx, cy, zsp, a, a + 0.45, R * 0.18F, R * 0.62F, w, w, rim, gold, side);
                    spoke(p, cx, cy, zsp - zs * 0.006F, a, a - 0.45, R * 0.18F, R * 0.62F, w, w, shade(rim, 0.85F), gold, side);
                }
                if (style == RIM_MESH) {
                    p.ringZ(cx, cy, zsp + zs * 0.004F, R * 0.38F, R * 0.42F, n, rot, rim, gold, side);
                }
            }
            case RIM_DISH -> {
                p.ringZ(cx, cy, zo - zs * 0.025F, R * 0.42F, R * 0.62F, n, rot, shade(rim, 1.2F), gold, side);
                for (int i = 0; i < 5; i++) {
                    double a = Math.PI / 2 + i * 2 * Math.PI / 5;
                    spoke(p, cx, cy, zo - zs * 0.1F, a, a, R * 0.17F, R * 0.44F, R * 0.09F, R * 0.11F, rim, gold, side);
                }
            }
            case RIM_Y -> {
                for (int i = 0; i < 5; i++) {
                    double a = Math.PI / 2 + i * 2 * Math.PI / 5;
                    spoke(p, cx, cy, zsp, a, a, R * 0.17F, R * 0.4F, R * 0.06F, R * 0.06F, rim, gold, side);
                    spoke(p, cx, cy, zsp, a, a - 0.28, R * 0.38F, R * 0.63F, R * 0.04F, R * 0.045F, rim, gold, side);
                    spoke(p, cx, cy, zsp, a, a + 0.28, R * 0.38F, R * 0.63F, R * 0.04F, R * 0.045F, rim, gold, side);
                }
            }
            case RIM_TURBINE -> {
                p.ringZ(cx, cy, zsp - zs * 0.01F, 0, R * 0.62F, n, rot, shade(rim, 0.55F), false, side);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6;
                    spoke(p, cx, cy, zsp, a, a + 0.55, R * 0.2F, R * 0.62F, R * 0.03F, R * 0.07F, rim, gold, side);
                }
            }
            case RIM_SIX, RIM_SEVEN -> {
                int k = style == RIM_SIX ? 6 : 7;
                float w0 = style == RIM_SIX ? 0.12F : 0.07F, w1 = style == RIM_SIX ? 0.1F : 0.08F;
                for (int i = 0; i < k; i++) {
                    double a = Math.PI / 2 + i * 2 * Math.PI / k;
                    spoke(p, cx, cy, zsp, a, a, R * 0.17F, R * 0.63F, R * w0, R * w1, rim, gold, side);
                }
            }
            default -> {
                for (int i = 0; i < 5; i++) {
                    double base = Math.PI / 2 + i * 2 * Math.PI / 5;
                    for (int t = 0; t < 2; t++) {
                        double a = base + (t == 0 ? -0.13 : 0.13);
                        spoke(p, cx, cy, zsp, a, a, R * 0.17F, R * 0.64F, R * 0.05F, R * 0.065F, rim, gold, side);
                    }
                }
            }
        }
        p.ringZ(cx, cy, zo - zs * 0.05F, 0, R * 0.22F, 10, 0, shade(rim, 0.85F), gold, side);
        for (int i = 0; i < 5; i++) {
            double a = i * 2 * Math.PI / 5;
            p.ringZ(cx + (float) Math.cos(a) * R * 0.13F, cy + (float) Math.sin(a) * R * 0.13F, zo - zs * 0.045F, 0, R * 0.03F, 6, 0, 0x2A2C30, false, side);
        }
        p.ringZ(cx, cy, zo - zs * 0.042F, 0, R * 0.07F, 6, 0, sh == Shape.LOWRIDER ? 0xFFF1B0 : shade(rim, 1.2F), gold, side);
    }

    // ------------------------------------------------------------------------------------------------------------- monoplaza

    private static void formulaParts(Painter p, Body bd) {
        CarStyle s = bd.s;
        float hx = bd.hx, hz = bd.hz, y0 = bd.y0, y1 = bd.y1;
        int dark = 0x141518;
        float ww = hz + s.track + s.wheelW * 0.7F;
        // alerón delantero de dos planos con placas
        p.box(hx - 1.1F, hx + 0.25F, y0 - 0.3F, y0 - 0.1F, -ww, ww, s.accent, false);
        p.box(hx - 1.5F, hx - 0.6F, y0 - 0.1F, y0 + 0.1F, -ww + 0.2F, ww - 0.2F, s.body, false);
        p.box(hx - 1.4F, hx + 0.2F, y0 - 0.3F, y0 + 0.45F, -ww - 0.1F, -ww, s.accent, false);
        p.box(hx - 1.4F, hx + 0.2F, y0 - 0.3F, y0 + 0.45F, ww, ww + 0.1F, s.accent, false);
        // pontones redondeados
        p.hexa(-3.0F, 1.0F, -hz - 1.15F, hz + 1.15F, y0, -2.6F, 0.2F, -hz - 0.85F, hz + 0.85F, y0 + 1.2F, s.body, false);
        p.box(0.95F, 1.05F, y0 + 0.2F, y0 + 1.0F, -hz - 1.0F, -hz - 0.2F, dark, false);
        p.box(0.95F, 1.05F, y0 + 0.2F, y0 + 1.0F, hz + 0.2F, hz + 1.0F, dark, false);
        // cabina, halo y piloto
        p.box(s.cabRear, s.cabFront, y1 - 0.05F, y1 + 0.02F, -0.9F, 0.9F, dark, false);
        p.box(-0.6F, 0.6F, y1, y1 + 1.15F, -0.6F, 0.6F, s.accent, false);
        p.box(0.35F, 0.66F, y1 + 0.4F, y1 + 0.8F, -0.45F, 0.45F, 0x101418, false);
        p.box(-0.9F, 1.4F, y1 + 1.25F, y1 + 1.4F, -0.12F, 0.12F, 0x2A2C30, false);
        p.box(1.3F, 1.45F, y1, y1 + 1.4F, -0.12F, 0.12F, 0x2A2C30, false);
        p.box(-0.9F, -0.75F, y1, y1 + 1.4F, -0.75F, -0.6F, 0x2A2C30, false);
        p.box(-0.9F, -0.75F, y1, y1 + 1.4F, 0.6F, 0.75F, 0x2A2C30, false);
        // toma de aire sobre el piloto y alerón trasero
        p.hexa(-2.6F, -0.8F, -0.55F, 0.55F, y1, -2.6F, -1.4F, -0.45F, 0.45F, y1 + 1.4F, s.body, false);
        p.box(-1.0F, -0.8F, y1 + 0.9F, y1 + 1.3F, -0.35F, 0.35F, dark, false);
        float wy = y1 + 1.6F;
        p.box(-hx - 0.3F, -hx + 1.3F, wy, wy + 0.3F, -hz - 1.2F, hz + 1.2F, s.accent, false);
        p.box(-hx - 0.2F, -hx + 0.9F, wy - 0.55F, wy - 0.35F, -hz - 1.2F, hz + 1.2F, s.accent, false);
        p.box(-hx - 0.3F, -hx + 1.3F, wy - 1.8F, wy + 0.4F, -hz - 1.35F, -hz - 1.2F, s.accent, false);
        p.box(-hx - 0.3F, -hx + 1.3F, wy - 1.8F, wy + 0.4F, hz + 1.2F, hz + 1.35F, s.accent, false);
        p.box(-hx - 0.4F, -hx - 0.2F, y0 + 0.4F, y0 + 0.7F, -0.25F, 0.25F, 0xFF2A2A, true);
        // suspensión hacia las ruedas
        for (float wx : new float[] {s.wheelFront, s.wheelRear}) {
            float zo = hz + s.track + 0.05F - s.wheelW;
            p.box(wx - 0.08F, wx + 0.08F, s.wheelR - 0.05F, s.wheelR + 0.05F, -zo, -hz + 0.2F, dark, false);
            p.box(wx - 0.08F, wx + 0.08F, s.wheelR - 0.05F, s.wheelR + 0.05F, hz - 0.2F, zo, dark, false);
        }
    }

    // ------------------------------------------------------------------------------------------------------------- extras

    private static void extras(Painter p, Body bd, Cabin cab, boolean chrome, int dark) {
        CarStyle s = bd.s;
        float hx = bd.hx, hz = bd.hz;
        boolean formula = bd.formula;
        if (cab != null) {
            float[] rt = {cab.rr, cab.rf, -cab.at((cab.rr + cab.rf) / 2)[4], cab.at((cab.rr + cab.rf) / 2)[4]};
            float yt = cab.yt + Math.min(0.14F, s.roofH * 0.07F);
            roofExtras(p, s, rt, yt);
            if (chrome) {
                // moldura cromada en la base de las ventanas
                for (int i = 0; i < 12; i++) {
                    float xa = lerp(s.cabRear + 0.2F, s.cabFront - 0.2F, i / 12.0F), xb = lerp(s.cabRear + 0.2F, s.cabFront - 0.2F, (i + 1) / 12.0F);
                    float za = cab.at(xa)[3], zb = cab.at(xb)[3];
                    float ya = bd.top(xa) + 0.01F, yb = bd.top(xb) + 0.01F;
                    for (int side = -1; side <= 1; side += 2) {
                        p.face(new float[] {xa, ya, side * (za + 0.02F)}, new float[] {xb, yb, side * (zb + 0.02F)},
                                new float[] {xb, yb, side * (zb + 0.2F)}, new float[] {xa, ya, side * (za + 0.2F)}, 0xEEF2F6, false, 0, 1, 0);
                    }
                }
            }
        }
        if (s.has(NUMBER_DISC)) {
            float dx = (s.cabFront + s.cabRear) / 2.0F - 0.2F;
            float dy = lerp(bd.bot(dx), bd.top(dx), 0.52F);
            for (int side = -1; side <= 1; side += 2) {
                float zf = side * (bd.surfZ(dx, dy) + 0.03F);
                p.ringZ(dx, dy, zf, 0, 0.85F, 14, 0, 0xF8F8F8, false, side);
                float zn = zf + side * 0.01F;
                p.face(new float[] {dx - 0.12F, dy - 0.5F, zn}, new float[] {dx + 0.12F, dy - 0.5F, zn}, new float[] {dx + 0.12F, dy + 0.5F, zn},
                        new float[] {dx - 0.12F, dy + 0.5F, zn}, s.accent, false, 0, 0, side);
                p.face(new float[] {dx - 0.4F, dy + 0.25F, zn}, new float[] {dx - 0.12F, dy + 0.25F, zn}, new float[] {dx - 0.12F, dy + 0.5F, zn},
                        new float[] {dx - 0.4F, dy + 0.42F, zn}, s.accent, false, 0, 0, side);
            }
        }
        if (s.has(HOOD_SCOOP)) {
            float hx0 = s.cabFront + 0.4F;
            float hx1 = Math.min(hx - 1.2F, hx0 + 2.6F);
            float yb = Math.min(bd.top(hx0), bd.top(hx1)) - 0.05F;
            float ytp = bd.top(hx0) + 0.55F;
            p.hexa(hx0, hx1, -0.95F, 0.95F, yb, hx0, hx1 - 0.7F, -0.78F, 0.78F, ytp, s.accent, false);
            p.box(hx1 - 0.78F, hx1 - 0.62F, yb + 0.1F, ytp - 0.08F, -0.62F, 0.62F, 0x0E0E10, false);
        }
        if ((s.has(SPOILER) || s.has(BIG_WING)) && !formula) {
            boolean big = s.has(BIG_WING);
            float wx0 = -hx + 0.25F;
            float wx1 = wx0 + (big ? 1.7F : 1.25F);
            float base = bd.top(wx0 + 0.6F) - 0.05F;
            float h = big ? 1.55F : 0.55F;
            float wy = base + h;
            float ww = bd.w0(wx0 + 0.6F) - 0.1F;
            // ala con perfil: más alta atrás
            p.hexa(wx0, wx1, -ww, ww, wy, wx0, wx1 - 0.3F, -ww, ww, wy + 0.22F, s.accent, false);
            if (big) {
                p.hexa(wx0 - 0.05F, wx1 + 0.05F, -ww - 0.12F, -ww, wy - 0.55F, wx0 - 0.05F, wx1 - 0.2F, -ww - 0.12F, -ww, wy + 0.45F, s.accent, false);
                p.hexa(wx0 - 0.05F, wx1 + 0.05F, ww, ww + 0.12F, wy - 0.55F, wx0 - 0.05F, wx1 - 0.2F, ww, ww + 0.12F, wy + 0.45F, s.accent, false);
            }
            float sz = Math.min(ww - 0.6F, 1.6F);
            p.hexa(wx0 + 0.45F, wx0 + 0.85F, -sz - 0.12F, -sz + 0.12F, base, wx0 + 0.3F, wx0 + 0.6F, -sz - 0.1F, -sz + 0.1F, wy + 0.05F, dark, false);
            p.hexa(wx0 + 0.45F, wx0 + 0.85F, sz - 0.12F, sz + 0.12F, base, wx0 + 0.3F, wx0 + 0.6F, sz - 0.1F, sz + 0.1F, wy + 0.05F, dark, false);
        }
        if (s.has(FIN)) {
            float fx1 = cab != null ? cab.rr + 0.3F : -0.5F;
            float y = bd.top(-hx + 0.5F) - 0.05F;
            p.hexa(-hx + 0.5F, fx1, -0.1F, 0.1F, y, -hx + 0.5F, -hx + 1.4F, -0.08F, 0.08F, y + 1.35F, s.has(TWO_TONE) ? s.accent : s.body, false);
        }
        if (s.has(EXHAUST) || s.hasD(D_QUAD_EXH) || s.hasD(D_CENTER_EXH)) {
            float ey = bd.bot(-hx + 0.5F) + 0.3F;
            float[] zs;
            float r = 0.26F;
            if (s.hasD(D_CENTER_EXH)) {
                zs = new float[] {-0.75F, -0.25F, 0.25F, 0.75F};
                r = 0.22F;
            } else if (s.hasD(D_QUAD_EXH)) {
                zs = new float[] {-1.9F, -1.35F, 1.35F, 1.9F};
                r = 0.22F;
            } else {
                zs = new float[] {-1.6F, -1.0F, 1.0F, 1.6F};
            }
            for (float ez : zs) {
                float ex = bd.surfX(ey, ez, false);
                p.tubeX(ex + 0.6F, ex - 0.35F, ey, ez, r, 10, 0xC4CAD2, 0x26120A, false);
                p.ringX(ex - 0.36F, ey, ez, 0, r * 0.6F, 8, 0xFF7A2A, true, -1);
            }
        }
        if (s.hasD(D_SIDE_EXH)) {
            for (int side = -1; side <= 1; side += 2) {
                float z = side * (bd.w(0) + 0.15F);
                float y = bd.y0 + 0.15F;
                float x0 = s.wheelRear + s.wheelR + 0.4F, x1 = s.wheelFront - s.wheelR - 0.4F;
                p.tubeX(x1, x0, y, z, 0.2F, 8, 0xC4CAD2, 0x26120A, false);
            }
        }
        if (s.has(BULLBAR)) {
            float y0 = bd.y0;
            float bh = s.bodyH;
            float fx = hx + 0.1F;
            p.box(fx, fx + 0.3F, y0 + 0.1F, y0 + bh * 0.8F, -hz + 0.7F, -hz + 1.0F, 0x3B3E44, false);
            p.box(fx, fx + 0.3F, y0 + 0.1F, y0 + bh * 0.8F, hz - 1.0F, hz - 0.7F, 0x3B3E44, false);
            p.box(fx, fx + 0.3F, y0 + bh * 0.55F, y0 + bh * 0.8F, -hz + 0.7F, hz - 0.7F, 0x3B3E44, false);
            p.box(fx, fx + 0.3F, y0 + 0.1F, y0 + 0.35F, -hz + 0.7F, hz - 0.7F, 0x3B3E44, false);
        }
        if (s.has(MUDFLAPS)) {
            for (float wx : new float[] {s.wheelFront, s.wheelRear}) {
                float az = archZ(bd, wx);
                float x = wx - s.wheelR - 0.45F;
                float my0 = Math.max(0.3F, bd.bot(x) - 0.9F);
                p.box(x - 0.08F, x + 0.06F, my0, bd.bot(x) + 0.3F, -az - 0.05F, -az + 0.9F, 0x1E1F23, false);
                p.box(x - 0.08F, x + 0.06F, my0, bd.bot(x) + 0.3F, az - 0.9F, az + 0.05F, 0x1E1F23, false);
            }
        }
        if (s.has(SIREN) && cab != null) {
            float cx = (cab.rr + cab.rf) / 2.0F;
            float yt = cab.yt + Math.min(0.14F, s.roofH * 0.07F);
            p.box(cx - 0.6F, cx + 0.6F, yt - 0.02F, yt + 0.2F, -2.0F, 2.0F, 0x222428, false);
            p.box(cx - 0.1F, cx + 0.1F, yt + 0.2F, yt + 0.62F, -0.12F, 0.12F, 0xDDDDDD, false);
        }
    }

    /** Lo que va sobre el techo (fijo): letrero, faros extra, parrilla, tabla de surf, cono de helado, casco. */
    private static void roofExtras(Painter p, CarStyle s, float[] rt, float yt) {
        float cx = (rt[0] + rt[1]) / 2.0F;
        if (s.has(TAXI_SIGN)) {
            p.hexa(cx - 0.8F, cx + 0.8F, -1.1F, 1.1F, yt, cx - 0.6F, cx + 0.6F, -1.0F, 1.0F, yt + 0.8F, 0xFFF4B0, true);
            p.box(cx - 0.82F, cx + 0.82F, yt - 0.02F, yt + 0.12F, -1.12F, 1.12F, 0x1E1E1E, false);
            p.box(cx - 0.3F, cx + 0.3F, yt + 0.25F, yt + 0.55F, -1.11F, 1.11F, 0x1E1E1E, false);
        }
        if (s.has(ROOF_LIGHTS)) {
            float fx = rt[1] - 0.3F;
            p.box(fx - 0.25F, fx + 0.25F, yt, yt + 0.2F, rt[2] + 0.2F, rt[3] - 0.2F, 0x1A1A1A, false);
            for (int i = 0; i < 4; i++) {
                float z = lerp(rt[2] + 0.5F, rt[3] - 0.5F, i / 3.0F);
                p.box(fx - 0.1F, fx + 0.3F, yt + 0.12F, yt + 0.72F, z - 0.34F, z + 0.34F, 0x15161A, false);
                p.box(fx + 0.3F, fx + 0.36F, yt + 0.17F, yt + 0.67F, z - 0.28F, z + 0.28F, 0xFFF8DC, true);
            }
        }
        if (s.has(ROOF_RACK)) {
            p.box(rt[0] + 0.2F, rt[1] - 0.2F, yt, yt + 0.35F, rt[2] + 0.2F, rt[2] + 0.4F, 0x2A2C30, false);
            p.box(rt[0] + 0.2F, rt[1] - 0.2F, yt, yt + 0.35F, rt[3] - 0.4F, rt[3] - 0.2F, 0x2A2C30, false);
            for (int i = 0; i < 3; i++) {
                float x = lerp(rt[0] + 0.5F, rt[1] - 0.5F, i / 2.0F);
                p.box(x - 0.1F, x + 0.1F, yt + 0.25F, yt + 0.38F, rt[2] + 0.2F, rt[3] - 0.2F, 0x2A2C30, false);
            }
        }
        if (s.has(SURFBOARD)) {
            float bx0 = rt[0] - 1.4F, bx1 = rt[1] + 1.2F;
            p.hexa(bx0 + 0.4F, bx1 - 0.6F, -0.8F, 0.8F, yt + 0.38F, bx0 + 0.4F, bx1 - 0.6F, -0.75F, 0.75F, yt + 0.6F, 0x3FC1C9, false);
            p.hexa(bx1 - 0.6F, bx1 - 0.6F, -0.8F, 0.8F, yt + 0.38F, bx1 - 0.6F, bx1 - 0.6F, -0.75F, 0.75F, yt + 0.6F, 0x3FC1C9, false);
            p.hexa(bx1 - 0.6F, bx1, -0.8F, 0.8F, yt + 0.38F, bx1 - 0.6F, bx1, -0.2F, 0.2F, yt + 0.6F, 0x3FC1C9, false);
            p.hexa(bx0, bx0 + 0.4F, -0.4F, 0.4F, yt + 0.38F, bx0, bx0 + 0.4F, -0.4F, 0.4F, yt + 0.6F, 0x3FC1C9, false);
            p.box(bx0 + 0.2F, bx1 - 0.2F, yt + 0.6F, yt + 0.62F, -0.12F, 0.12F, 0xFFFFFF, false);
        }
        if (s.has(ICE_CREAM)) {
            p.hexa(cx - 0.3F, cx + 0.3F, -0.3F, 0.3F, yt, cx - 1.1F, cx + 1.1F, -1.1F, 1.1F, yt + 2.2F, 0xD9A066, false);
            p.box(cx - 1.25F, cx + 1.25F, yt + 2.1F, yt + 3.4F, -1.25F, 1.25F, 0xFF8FC0, false);
            p.box(cx - 1.4F, cx + 1.4F, yt + 2.3F, yt + 3.1F, -1.0F, 1.0F, 0xFF8FC0, false);
            p.box(cx - 1.0F, cx + 1.0F, yt + 2.3F, yt + 3.1F, -1.4F, 1.4F, 0xFF8FC0, false);
            p.box(cx - 0.95F, cx + 0.95F, yt + 3.4F, yt + 3.9F, -0.95F, 0.95F, 0xFFA6CF, false);
            p.box(cx - 0.2F, cx + 0.2F, yt + 3.9F, yt + 4.3F, -0.2F, 0.2F, 0xE0212F, false);
        }
        if (s.has(HELMET)) {
            p.box(cx - 0.5F, cx + 0.5F, yt, yt + 0.9F, -0.5F, 0.5F, s.accent, false);
        }
    }
}
