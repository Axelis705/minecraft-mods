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
        final float rc, rvF, rvT, fl, ctB, sh, noseDrop, tailDrop, bandLo, bandHi;
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
                default -> { rcv = 1.2F; flv = 0.12F; ct = 0.6F; }
            }
            rc = Math.min(rcv, wb * 0.8F);
            fl = flv;
            ctB = ct;
            sh = formula ? 0.0F : 0.09F;
            rvF = switch (s.shape) {
                case VAN, PICKUP -> 0.55F;
                case CLASSIC, MINI -> 1.1F;
                default -> 0.8F;
            };
            rvT = switch (s.shape) {
                case VAN, PICKUP, HATCH, MINI -> 0.4F;
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

        /** 0..1: cuánto se ensancha el guardabarros en x (1 justo sobre la rueda). */
        float flare(float x) {
            float f = 0;
            for (float wx : new float[] {s.wheelFront, s.wheelRear}) {
                float d = Math.abs(x - wx) / (s.wheelR + 1.3F);
                if (d < 1) {
                    f = Math.max(f, (float) (0.5 * (1 + Math.cos(Math.PI * d))));
                }
            }
            return f;
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
            float e = Math.min(x + hx, hx - x);
            if (e < rc) {
                float d = rc - Math.max(0, e);
                return wb - rc + (float) Math.sqrt(Math.max(0, rc * rc - d * d));
            }
            return wb;
        }

        float w(float x) {
            return w0(x) + fl * flare(x);
        }

        float top(float x) {
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
            float b = y0;
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
            float s1 = Math.max(0.02F, Math.min(0.85F, w - ct - 0.02F));
            float s0 = Math.max(0.01F, Math.min(0.25F, s1 - 0.02F));
            return new float[][] {{0, b}, {w - cb, b}, {w, b + cb}, {w + sh, yA}, {w + sh, yB}, {w, t - ct}, {w - ct, t}, {s1, t}, {s0, t},
                    {0, t}};
        }

        /** Dónde está la superficie del costado derecho a la altura y (-1 si no hay carrocería ahí). */
        float surfZ(float x, float y) {
            float[][] p = section(x);
            if (y < p[0][1] || y > p[9][1]) {
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

    public static void buildStatic(CarStyle s, Sink sink, int mode) {
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
        if (s.shape == Shape.PICKUP) {
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
        }
        wheels(p, bd);
        extras(p, bd, cab, chrome, dark);
    }

    /** Lo que se mueve o parpadea: torreta, neón del piso y destellos del oro. */
    public static void buildAnimated(CarStyle s, Sink sink, float time, int mode) {
        if (mode != MODE_NORMAL || !(s.has(SIREN) || s.has(NEON) || s.has(GOLD_SPARKLE))) {
            return;
        }
        Painter p = new Painter(sink, mode);
        Body bd = new Body(s);
        float hx = bd.hx, hz = bd.hz;
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
        if (s.shape == Shape.RALLY || s.shape == Shape.PICKUP) {
            segCol[1] = 0x2A2B30;   // moldura de plástico abajo
            segEm[1] = false;
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
        final int roofStart = 2;
        final int roofEnd = 4;
        final float rr, rf, yt;

        Cabin(Body bd) {
            this.bd = bd;
            CarStyle s = bd.s;
            float[] r = roofRect(bd);
            rr = r[0];
            rf = r[1];
            yt = r[3];
            float cf = s.cabFront, cr = s.cabRear;
            float ri = rr - cr, wi = cf - rf;
            float ybR = bd.top(cr), ybF = bd.top(cf);
            st = new float[][] {
                    station(cr, ybR, ybR),
                    station(lerp(cr, rr, 0.72F) - 0.09F * Math.min(1, ri), bd.top(lerp(cr, rr, 0.72F)), lerp(ybR, yt, 0.72F) + 0.06F * Math.min(1, ri)),
                    station(rr, bd.top(rr), yt),
                    station((rr + rf) / 2, bd.top((rr + rf) / 2), yt + Math.min(0.14F, s.roofH * 0.07F)),
                    station(rf, bd.top(rf), yt),
                    station(lerp(cf, rf, 0.72F) + 0.1F * Math.min(1, wi), bd.top(lerp(cf, rf, 0.72F)), lerp(ybF, yt, 0.72F) + 0.07F * Math.min(1, wi)),
                    station(cf, ybF, ybF)};
        }

        /** {roof rear x, roof front x, medio ancho del techo, y del techo}. */
        static float[] roofRect(Body bd) {
            CarStyle s = bd.s;
            float cf = s.cabFront, cr = s.cabRear;
            float ws = s.wsInset, ri = s.rearInset;
            float room = (cf - cr) - 0.8F;
            if (ws + ri > room) {
                float k = room / (ws + ri);
                ws *= k;
                ri *= k;
            }
            float yt = bd.top((cf + cr) / 2) - 0.02F + s.roofH;
            float gb = bd.w0((cf + cr) / 2) - 0.42F;
            return new float[] {cr + ri, cf - ws, gb - s.sideInset, yt};
        }

        private float[] station(float x, float yb, float ytop) {
            CarStyle s = bd.s;
            yb -= 0.02F;
            float hf = clamp((ytop - yb) / Math.max(0.3F, s.roofH), 0, 1);
            float gb = bd.w0(x) - 0.42F;
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
            float hgt = s.roofH;
            for (int j = 0; j < st.length - 1; j++) {
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

    /** Faro redondo pegado al morro o a la cola. */
    private static void roundDecal(Painter p, Body bd, float zc, float yc, float r, int rgb, boolean em, boolean front, float off, int ringRgb) {
        float dir = front ? 1 : -1;
        int n = 10;
        float[] c = {bd.surfX(yc, zc, front) + dir * (off + 0.01F), yc, zc};
        for (int i = 0; i < n; i++) {
            double a0 = i * 2 * Math.PI / n, a1 = (i + 1) * 2 * Math.PI / n;
            float y0 = yc + (float) Math.sin(a0) * r, z0 = zc + (float) Math.cos(a0) * r;
            float y1 = yc + (float) Math.sin(a1) * r, z1 = zc + (float) Math.cos(a1) * r;
            float[] a = {bd.surfX(y0, z0, front) + dir * (off + 0.01F), y0, z0};
            float[] b = {bd.surfX(y1, z1, front) + dir * (off + 0.01F), y1, z1};
            p.face(c, a, b, b, rgb, em, dir, 0, 0);
            if (ringRgb >= 0) {
                float R = r + 0.12F;
                float y2 = yc + (float) Math.sin(a0) * R, z2 = zc + (float) Math.cos(a0) * R;
                float y3 = yc + (float) Math.sin(a1) * R, z3 = zc + (float) Math.cos(a1) * R;
                float[] e = {bd.surfX(y2, z2, front) + dir * off, y2, z2};
                float[] f = {bd.surfX(y3, z3, front) + dir * off, y3, z3};
                float[] a2 = {a[0] - dir * 0.01F, a[1], a[2]};
                float[] b2 = {b[0] - dir * 0.01F, b[1], b[2]};
                p.face(a2, e, f, b2, ringRgb, false, dir, 0, 0);
            }
        }
    }

    private static void lights(Painter p, Body bd) {
        CarStyle s = bd.s;
        Shape sh = s.shape;
        float xq = bd.hx - 0.6F;
        float bq = bd.bot(xq), tq = bd.top(xq), hq = tq - bq;
        float wq = bd.w0(xq);
        int head = sh == Shape.CLASSIC || sh == Shape.LOWRIDER || sh == Shape.MINI ? 0xFFF1C8 : 0xF2F8FF;
        int lens = 0x2A2D33;
        boolean round = sh == Shape.CLASSIC || sh == Shape.MINI || sh == Shape.VAN;
        boolean quadRound = sh == Shape.MUSCLE || sh == Shape.LOWRIDER;
        boolean slim = sporty(sh);
        int chrome = 0xD9DEE4;
        // ---- faros delanteros
        if (round) {
            float yl = bq + hq * 0.58F;
            float r = Math.min(0.5F, hq * 0.2F);
            for (int side = -1; side <= 1; side += 2) {
                roundDecal(p, bd, side * wq * 0.62F, yl, r, head, true, true, 0.03F, chrome);
            }
        } else if (quadRound) {
            float yl = bq + hq * 0.6F;
            float r = Math.min(0.36F, hq * 0.15F);
            for (int side = -1; side <= 1; side += 2) {
                frontDecal(p, bd, side * wq * 0.35F, side * wq * 0.92F, yl - r - 0.15F, yl + r + 0.15F, 0x15161A, false, true, 3, 0.02F);
                roundDecal(p, bd, side * wq * 0.5F, yl, r, head, true, true, 0.04F, chrome);
                roundDecal(p, bd, side * wq * 0.77F, yl, r, head, true, true, 0.04F, chrome);
            }
        } else if (slim) {
            float yl = bq + hq * 0.62F;
            for (int side = -1; side <= 1; side += 2) {
                float zi = side * wq * 0.4F, zo = side * (wq - 0.12F);
                frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl - 0.1F, yl + 0.3F, lens, false, true, 4, 0.02F);
                frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + 0.18F, head, true, true, 4, 0.035F);
            }
        } else {
            float yl = bq + hq * 0.56F;
            for (int side = -1; side <= 1; side += 2) {
                float zi = side * wq * 0.42F, zo = side * (wq - 0.15F);
                float zm = side * (wq - 0.45F);
                frontDecal(p, bd, Math.min(zi, zm), Math.max(zi, zm), yl, yl + 0.5F, head, true, true, 3, 0.03F);
                frontDecal(p, bd, Math.min(zm, zo), Math.max(zm, zo), yl, yl + 0.5F, 0xFFB43A, true, true, 2, 0.03F);
            }
        }
        // ---- parrilla y tomas de aire
        int trim = s.has(CHROME) ? chrome : 0x1A1B1F;
        if (slim) {
            float yi = bq + 0.22F;
            frontDecal(p, bd, -wq * 0.62F, wq * 0.62F, yi, yi + hq * 0.3F, 0x101114, false, true, 6, 0.02F);
            p.box(bd.hx - 1.4F, bd.hx + 0.12F, bd.bot(bd.hx) - 0.12F, bd.bot(bd.hx) - 0.02F, -wq + 0.4F, wq - 0.4F, 0x1A1B1F, false);
        } else if (sh == Shape.PICKUP || sh == Shape.RALLY) {
            float yg = bq + hq * 0.28F;
            frontDecal(p, bd, -wq * 0.4F, wq * 0.4F, yg, yg + hq * 0.42F, trim, false, true, 4, 0.02F);
            for (int i = 0; i < 3; i++) {
                float yy = yg + hq * 0.1F * (i + 1);
                frontDecal(p, bd, -wq * 0.38F, wq * 0.38F, yy, yy + 0.06F, shade(trim, s.has(CHROME) ? 0.7F : 2.2F), false, true, 4, 0.03F);
            }
        } else if (sh == Shape.CLASSIC) {
            float yg = bq + hq * 0.3F;
            roundDecal(p, bd, 0, yg, Math.min(0.55F, hq * 0.2F), 0x1A1B1F, false, true, 0.02F, chrome);
        } else {
            float yg = bq + hq * 0.3F;
            frontDecal(p, bd, -wq * 0.34F, wq * 0.34F, yg, yg + hq * 0.2F, trim, false, true, 4, 0.02F);
            frontDecal(p, bd, -wq * 0.55F, wq * 0.55F, bq + 0.18F, bq + 0.18F + hq * 0.1F, 0x15161A, false, true, 4, 0.02F);
        }
        // ---- defensa trasera, calaveras y placa
        float xt = -bd.hx + 0.6F;
        float bt = bd.bot(xt), tt = bd.top(xt), ht = tt - bt;
        float wt = bd.w0(xt);
        int red = 0xFF2A2A;
        if (slim && sh != Shape.COUPE) {
            float yl = bt + ht * 0.66F;
            frontDecal(p, bd, -wt + 0.25F, wt - 0.25F, yl - 0.06F, yl + 0.22F, 0x3A0A0E, false, false, 8, 0.02F);
            frontDecal(p, bd, -wt + 0.3F, wt - 0.3F, yl, yl + 0.14F, red, true, false, 8, 0.035F);
            frontDecal(p, bd, -wq * 0.6F, wq * 0.6F, bt + 0.15F, bt + ht * 0.3F, 0x101114, false, false, 6, 0.02F);
        } else if (round || sh == Shape.CLASSIC) {
            float yl = bt + ht * 0.6F;
            for (int side = -1; side <= 1; side += 2) {
                roundDecal(p, bd, side * wt * 0.65F, yl, Math.min(0.32F, ht * 0.14F), red, true, false, 0.03F, chrome);
            }
        } else {
            float yl = bt + ht * 0.56F;
            float hh = sh == Shape.VAN || sh == Shape.PICKUP ? 0.8F : 0.45F;
            for (int side = -1; side <= 1; side += 2) {
                float zi = side * wt * 0.5F, zo = side * (wt - 0.12F);
                frontDecal(p, bd, Math.min(zi, zo), Math.max(zi, zo), yl, yl + hh, red, true, false, 3, 0.03F);
                float zr = side * wt * 0.5F, zr2 = side * wt * 0.62F;
                frontDecal(p, bd, Math.min(zr, zr2), Math.max(zr, zr2), yl, yl + hh, 0xF0F0F0, true, false, 1, 0.04F);
            }
        }
        if (sh != Shape.PROTO && sh != Shape.HYPER) {
            float yp = bt + ht * 0.22F;
            frontDecal(p, bd, -0.95F, 0.95F, yp, yp + 0.55F, 0xF2F0E6, false, false, 3, 0.03F);
            frontDecal(p, bd, -0.6F, 0.6F, yp + 0.2F, yp + 0.32F, 0x30343C, false, false, 2, 0.04F);
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
        for (int side = -1; side <= 1; side += 2) {
            float z0 = side < 0 ? -zout : zin, z1 = side < 0 ? -zin : zout;
            p.box(mx - 0.12F, mx + 0.05F, yb, yb + 0.25F, z0, z1, 0x1A1B1F, false);
            float zh0 = side < 0 ? -zout : zout - 0.42F, zh1 = side < 0 ? -zout + 0.42F : zout;
            p.box(mx - 0.42F, mx + 0.05F, yb + 0.18F, yb + 0.58F, zh0, zh1, paint(bd, upper, yb + 0.5F), false);
            float zg = side < 0 ? -zout + 0.02F : zout - 0.02F;
            p.box(mx - 0.45F, mx - 0.41F, yb + 0.22F, yb + 0.54F, Math.min(zg, zg - side * 0.36F), Math.max(zg, zg - side * 0.36F), 0x9FB4C8, false);
        }
    }

    // ------------------------------------------------------------------------------------------------------------- ruedas

    private static float archZ(Body bd, float wx) {
        float ra = bd.s.wheelR + 0.3F;
        float m = 0;
        for (int i = 0; i <= 8; i++) {
            float x = wx - ra + 2 * ra * i / 8.0F;
            m = Math.max(m, bd.w(x) + bd.sh);
        }
        return m;
    }

    private static void wheels(Painter p, Body bd) {
        CarStyle s = bd.s;
        Shape sh = s.shape;
        float R = s.wheelR;
        float W = s.wheelW;
        boolean gold = s.has(GOLD_SPARKLE);
        for (float wx : new float[] {s.wheelFront, s.wheelRear}) {
            float outer;
            if (bd.formula) {
                outer = bd.hz + s.track + 0.05F;
            } else {
                float az = archZ(bd, wx);
                outer = Math.max(bd.hz + s.track + 0.05F, az + 0.1F);
                // paso de rueda oscuro y su borde
                float ra = R + 0.28F;
                int n = 14;
                float cyB = bd.bot(wx);
                float[] c = null;
                boolean cladding = sh == Shape.RALLY || sh == Shape.PICKUP;
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
                        p.face(new float[] {wx, cyB, zc}, new float[] {x0, y0, z0}, new float[] {x1, y1, z1}, new float[] {x1, y1, z1}, 0x0B0B0D, false, 0,
                                0, side);
                        // borde del guardabarros
                        float ylo = Math.min(y0, y1);
                        if (ylo > cyB + 0.05F) {
                            float X0 = wx + (float) Math.cos(a0) * (ra + lip), X1 = wx + (float) Math.cos(a1) * (ra + lip);
                            float Y0 = Math.min(R + (float) Math.sin(a0) * (ra + lip), bd.top(X0) - 0.12F);
                            float Y1 = Math.min(R + (float) Math.sin(a1) * (ra + lip), bd.top(X1) - 0.12F);
                            float zl0 = side * (bd.w(x0) + bd.sh + 0.02F), zl1 = side * (bd.w(x1) + bd.sh + 0.02F);
                            float zL0 = side * (bd.w(X0) + bd.sh + 0.02F), zL1 = side * (bd.w(X1) + bd.sh + 0.02F);
                            p.face(new float[] {x0, y0, zl0}, new float[] {X0, Y0, zL0}, new float[] {X1, Y1, zL1}, new float[] {x1, y1, zl1},
                                    paint(bd, lipCol, Y0), false, 0, 0, side);
                        }
                    }
                }
            }
            for (int side = -1; side <= 1; side += 2) {
                wheel(p, s, wx, R, side * outer, side * (outer - W), side, gold);
            }
        }
    }

    private static void wheel(Painter p, CarStyle s, float cx, float R, float zo, float zi, int side, boolean gold) {
        Shape sh = s.shape;
        int n = 16;
        float rot = (float) (Math.PI / n);
        int tire = 0x17181B;
        int wall = 0x24252A;
        int rim = s.rim;
        float cy = R;
        float zs = side;
        p.tubeZ(cx, cy, zi, zo - zs * 0.16F, R, R, n, rot, tire, false);
        p.tubeZ(cx, cy, zo - zs * 0.16F, zo, R, R * 0.9F, n, rot, wall, false);
        p.ringZ(cx, cy, zi, 0, R, n, rot, tire, false, -side);
        p.ringZ(cx, cy, zo, R * 0.7F, R * 0.9F, n, rot, wall, false, side);
        if (sh == Shape.CLASSIC || sh == Shape.LOWRIDER) {
            p.ringZ(cx, cy, zo + zs * 0.004F, R * 0.73F, R * 0.86F, n, rot, 0xF1EFE6, false, side);
        }
        // fondo del rin, disco de freno y caliper
        p.ringZ(cx, cy, zo - zs * 0.22F, 0, R * 0.7F, n, rot, 0x1C1D21, false, side);
        boolean sportBrake = sporty(sh) || sh == Shape.RALLY || sh == Shape.MUSCLE || sh == Shape.FORMULA;
        if (sportBrake) {
            p.ringZ(cx, cy, zo - zs * 0.19F, R * 0.12F, R * 0.5F, n, rot, 0x8C9097, false, side);
            int cal = gold ? 0xFFD34D : s.has(NEON) ? s.neon : 0xD7262E;
            float ca0 = zo - zs * 0.18F, ca1 = zo - zs * 0.12F;
            p.box(cx - R * 0.42F, cx - R * 0.16F, cy + R * 0.12F, cy + R * 0.52F, Math.min(ca0, ca1), Math.max(ca0, ca1), cal, false);
        }
        p.ringZ(cx, cy, zo - zs * 0.02F, R * 0.61F, R * 0.7F, n, rot, shade(rim, 1.08F), gold, side);
        float zsp = zo - zs * 0.06F;
        if (sh == Shape.CLASSIC || sh == Shape.MINI || sh == Shape.VAN) {
            // tapón completo, estilo antiguo
            p.ringZ(cx, cy, zsp, 0, R * 0.6F, n, rot, rim, gold, side);
            p.ringZ(cx, cy, zo - zs * 0.045F, R * 0.36F, R * 0.42F, n, rot, shade(rim, 0.75F), false, side);
            p.ringZ(cx, cy, zo - zs * 0.04F, 0, R * 0.18F, 8, 0, shade(rim, 1.15F), gold, side);
            return;
        }
        int spokes;
        float hw0, hw1;
        boolean twin = false;
        switch (sh) {
            case LOWRIDER -> { spokes = 14; hw0 = 0.02F; hw1 = 0.02F; }
            case PICKUP, RALLY -> { spokes = 6; hw0 = 0.12F; hw1 = 0.1F; }
            case FORMULA -> { spokes = 6; hw0 = 0.07F; hw1 = 0.06F; }
            case HATCH, SEDAN -> { spokes = 7; hw0 = 0.07F; hw1 = 0.08F; }
            default -> { spokes = 5; hw0 = 0.05F; hw1 = 0.065F; twin = true; }
        }
        for (int i = 0; i < spokes; i++) {
            double base = Math.PI / 2 + i * 2 * Math.PI / spokes;
            for (int t = 0; t < (twin ? 2 : 1); t++) {
                double a = twin ? base + (t == 0 ? -0.13 : 0.13) : base;
                float ca = (float) Math.cos(a), sa = (float) Math.sin(a);
                float r0 = R * 0.17F, r1 = R * 0.64F;
                float w0 = R * hw0, w1 = R * hw1;
                float[] A = {cx + ca * r0 - sa * w0, cy + sa * r0 + ca * w0, zsp};
                float[] B = {cx + ca * r1 - sa * w1, cy + sa * r1 + ca * w1, zsp};
                float[] C = {cx + ca * r1 + sa * w1, cy + sa * r1 - ca * w1, zsp};
                float[] D = {cx + ca * r0 + sa * w0, cy + sa * r0 - ca * w0, zsp};
                p.face(A, B, C, D, rim, gold, 0, 0, side);
            }
        }
        p.ringZ(cx, cy, zo - zs * 0.05F, 0, R * 0.22F, 10, 0, shade(rim, 0.85F), gold, side);
        p.ringZ(cx, cy, zo - zs * 0.04F, 0, R * 0.09F, 6, 0, sh == Shape.LOWRIDER ? 0xFFF1B0 : shade(rim, 1.2F), gold, side);
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
        if (s.has(EXHAUST)) {
            for (int side = -1; side <= 1; side += 2) {
                float ey = bd.bot(-hx + 0.5F) + 0.3F;
                for (int k = 0; k < 2; k++) {
                    float ez = side * (1.0F + k * 0.6F);
                    float ex = bd.surfX(ey, ez, false);
                    p.tubeX(ex + 0.6F, ex - 0.35F, ey, ez, 0.26F, 8, 0xC4CAD2, 0xFF7A2A, true);
                }
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
                p.box(x - 0.08F, x + 0.06F, 0.25F, bd.bot(x) + 0.4F, -az - 0.05F, -az + 1.0F, 0x1E1F23, false);
                p.box(x - 0.08F, x + 0.06F, 0.25F, bd.bot(x) + 0.4F, az - 1.0F, az + 0.05F, 0x1E1F23, false);
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
