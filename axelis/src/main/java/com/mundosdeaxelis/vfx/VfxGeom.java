package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.VfxType.*;

/**
 * Convierte un efecto visual en cuadriláteros con textura. No usa nada de Minecraft:
 * así se puede probar fuera del juego y el renderizador solo tiene que dibujar lo que sale de aquí.
 */
public final class VfxGeom {
    private VfxGeom() {
    }

    private static final double TAU = Math.PI * 2;
    private static final float[] WHITE = {1F, 1F, 1F};

    // ------------------------------------------------------------------ utilidades

    static double clamp01(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    static double smooth(double x) {
        x = clamp01(x);
        return x * x * (3 - 2 * x);
    }

    static double easeOut(double t) {
        t = clamp01(t);
        double u = 1 - t;
        return 1 - u * u * u;
    }

    static double easeOutBack(double t) {
        t = clamp01(t);
        double c1 = 1.70158, c3 = c1 + 1;
        double u = t - 1;
        return 1 + c3 * u * u * u + c1 * u * u;
    }

    static double hash(long k) {
        k += 0x9E3779B97F4A7C15L;
        k = (k ^ (k >>> 30)) * 0xBF58476D1CE4E5B9L;
        k = (k ^ (k >>> 27)) * 0x94D049BB133111EBL;
        k ^= k >>> 31;
        return (k >>> 11) * (1.0 / (1L << 53));
    }

    static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F};
    }

    static float[] mix(float[] a, float[] b, double t) {
        float f = (float) t;
        return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }

    /** Aparece en fi ticks y se apaga en los últimos fo ticks de la vida. */
    static double env(double age, int life, double fi, double fo) {
        double in = fi <= 0 ? 1 : clamp01(age / fi);
        double out = (life > 0 && fo > 0) ? clamp01((life - age) / fo) : 1;
        return in * out;
    }

    private static float cl(double a) {
        return (float) (a < 0 ? 0 : (a > 1 ? 1 : a));
    }

    private static void raw(Sink s, int tex, int blend, V3 p0, V3 p1, V3 p2, V3 p3, float[] uv, float[] c,
                            double a0, double a1, double a2, double a3) {
        double[] p = {p0.x, p0.y, p0.z, p1.x, p1.y, p1.z, p2.x, p2.y, p2.z, p3.x, p3.y, p3.z};
        float[] col = {c[0], c[1], c[2], cl(a0), c[0], c[1], c[2], cl(a1), c[0], c[1], c[2], cl(a2), c[0], c[1], c[2], cl(a3)};
        s.quad(tex, blend, p, uv, col);
    }

    /** Plano completo: el centro, el semieje U (derecha) y el semieje V (arriba de la imagen). */
    private static void plane(Sink s, int tex, int blend, V3 c, V3 u, V3 v, float[] col, double a) {
        planeF(s, tex, blend, c, u, v, col, a, 0, false);
    }

    private static void planeF(Sink s, int tex, int blend, V3 c, V3 u, V3 v, float[] col, double a, int frame, boolean flip) {
        int fr = Math.max(1, VfxTex.FRAMES[tex]);
        float v0 = frame / (float) fr, v1 = (frame + 1) / (float) fr;
        float u0 = flip ? 1F : 0F, u1 = flip ? 0F : 1F;
        double len = Math.max(u.len(), v.len());
        // Los planos grandes se parten en cuadritos: un cuadro gigante pierde precisión de profundidad
        // y el terreno lo recorta con bordes rectos (se veía en el círculo del árbol).
        int n = len > 2.5 ? (int) Math.min(14, Math.ceil(len / 1.5)) : 1;
        if (n == 1) {
            float[] uv = {u0, v1, u1, v1, u1, v0, u0, v0};
            raw(s, tex, blend, c.sub(u).sub(v), c.add(u).sub(v), c.add(u).add(v), c.sub(u).add(v), uv, col, a, a, a, a);
            return;
        }
        for (int i = 0; i < n; i++) {
            double fa = -1 + 2.0 * i / n, fb = -1 + 2.0 * (i + 1) / n;
            float ua = u0 + (u1 - u0) * (float) (i / (double) n), ub = u0 + (u1 - u0) * (float) ((i + 1) / (double) n);
            for (int j = 0; j < n; j++) {
                double ga = -1 + 2.0 * j / n, gb = -1 + 2.0 * (j + 1) / n;
                float va = v1 + (v0 - v1) * (float) (j / (double) n), vb = v1 + (v0 - v1) * (float) ((j + 1) / (double) n);
                float[] uv = {ua, va, ub, va, ub, vb, ua, vb};
                raw(s, tex, blend, c.add(u.mul(fa)).add(v.mul(ga)), c.add(u.mul(fb)).add(v.mul(ga)),
                        c.add(u.mul(fb)).add(v.mul(gb)), c.add(u.mul(fa)).add(v.mul(gb)), uv, col, a, a, a, a);
            }
        }
    }

    /** Sprite que siempre mira a la cámara (sobre cualquier eje). */
    private static void billboard(Sink s, int tex, int blend, V3 c, double half, double rot, Cam cam, float[] col, double a) {
        V3 u = cam.right.mul(half), v = cam.up.mul(half);
        if (rot != 0) {
            double cs = Math.cos(rot), sn = Math.sin(rot);
            V3 u2 = u.mul(cs).add(v.mul(sn));
            V3 v2 = v.mul(cs).sub(u.mul(sn));
            u = u2;
            v = v2;
        }
        plane(s, tex, blend, c, u, v, col, a);
    }

    /** Cinta que se orienta hacia la cámara. La textura: u a lo largo, v a lo ancho. */
    private static void ribbon(Sink s, int tex, int blend, V3[] pts, double[] hw, double[] al, float[] col, Cam cam,
                               double u0, double uPer) {
        int n = pts.length;
        if (n < 2) {
            return;
        }
        V3[] side = new V3[n];
        for (int i = 0; i < n; i++) {
            V3 tg = (i == 0 ? pts[1].sub(pts[0]) : (i == n - 1 ? pts[n - 1].sub(pts[n - 2]) : pts[i + 1].sub(pts[i - 1]))).norm();
            V3 sv = tg.cross(cam.pos.sub(pts[i]));
            if (sv.len() < 1.0E-6) {
                sv = cam.right;
            }
            side[i] = sv.norm().mul(hw[i]);
        }
        double acc = 0;
        for (int i = 0; i < n - 1; i++) {
            double len = pts[i + 1].sub(pts[i]).len();
            float ua = (float) (u0 + acc * uPer), ub = (float) (u0 + (acc + len) * uPer);
            acc += len;
            float[] uv = {ua, 1F, ua, 0F, ub, 0F, ub, 1F};
            raw(s, tex, blend, pts[i].sub(side[i]), pts[i].add(side[i]), pts[i + 1].add(side[i + 1]), pts[i + 1].sub(side[i + 1]),
                    uv, col, al[i], al[i], al[i + 1], al[i + 1]);
        }
    }

    private static void ribbonSimple(Sink s, int tex, V3[] pts, double hw, double alpha, float[] col, Cam cam) {
        double[] w = new double[pts.length], a = new double[pts.length];
        java.util.Arrays.fill(w, hw);
        java.util.Arrays.fill(a, alpha);
        ribbon(s, tex, Sink.ADD, pts, w, a, col, cam, 0, 1);
    }

    private static V3 normalOf(VfxData d) {
        if ((d.flags & F_NORMAL) != 0) {
            V3 n = new V3(d.dx, d.dy, d.dz);
            if (n.len() > 1.0E-6) {
                return n.norm();
            }
        }
        return V3.UP;
    }

    // ------------------------------------------------------------------ entrada

    /** Genera todos los cuadriláteros de un efecto. age = ticks desde que apareció (con decimales). */
    public static void emit(VfxData d, double age, double partial, Cam cam, Sink s) {
        V3 pos = new V3(d.px + (d.x - d.px) * partial, d.py + (d.y - d.py) * partial, d.pz + (d.z - d.pz) * partial);
        int life = d.life > 0 ? d.life : 100000;
        double t = clamp01(age / life);
        switch (d.type) {
            case RING -> ring(d, age, t, pos, s);
            case RUNE -> rune(d, age, life, pos, s);
            case COLUMN -> column(d, age, life, t, pos, cam, s);
            case BEAM -> beam(d, age, life, pos, cam, s);
            case BOLT -> bolt(d, age, life, pos, cam, s);
            case SLASH -> slash(d, age, life, t, pos, cam, s);
            case FLASH -> flash(d, age, life, t, pos, cam, s);
            case ORB -> orb(d, age, life, pos, cam, s);
            case DOME -> dome(d, age, life, pos, cam, s);
            case FUNNEL -> funnel(d, age, life, pos, s);
            case SPRITE -> sprite(d, age, partial, life, pos, cam, s);
            case CROSS -> cross(d, age, life, pos, cam, s);
            case MAW -> maw(d, age, life, t, pos, cam, s);
            case RIBBON -> whip(d, age, life, pos, cam, s);
            case COIN -> coin(d, age, life, pos, cam, s);
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ anillo y círculo rúnico

    private static void ring(VfxData d, double age, double t, V3 pos, Sink s) {
        float[] c = rgb(d.rgb);
        V3 n = normalOf(d);
        V3 u = n.anyPerp(), v = n.cross(u);
        double r0 = d.a > 0 ? d.a : 0.12 * d.size;
        double fade = Math.pow(1 - t, 1.25) * clamp01(age / 1.2);
        V3 c0 = pos.add(n.mul(0.055));
        // tinta oscura bajo el anillo para que se note sobre suelos claros
        double e0 = easeOut(t);
        double rIn = lerp(r0, d.size, clamp01(e0));
        plane(s, VfxTex.RING, Sink.NORMAL, c0.sub(n.mul(0.003)), u.mul(rIn * 1.03), v.mul(rIn * 1.03),
                new float[]{c[0] * 0.22F, c[1] * 0.22F, c[2] * 0.22F}, fade * 0.5);
        for (int k = 0; k < 3; k++) {
            double e = easeOut(t - k * 0.06 * (1 - t));
            double rr = lerp(r0, d.size, clamp01(e));
            double al = fade * (k == 0 ? 1.0 : (k == 1 ? 0.45 : 0.2));
            plane(s, VfxTex.RING, Sink.ADD, c0, u.mul(rr), v.mul(rr), k == 0 ? mix(c, WHITE, 0.25) : c, al);
        }
    }

    private static void rune(VfxData d, double age, int life, V3 pos, Sink s) {
        float[] c = rgb(d.rgb);
        float[] hot = mix(c, WHITE, 0.45);
        V3 n = normalOf(d);
        V3 u = n.anyPerp(), v = n.cross(u);
        double R = d.size * easeOutBack(age / 7.0);
        double al = clamp01(age / 3.0) * (d.life > 0 ? clamp01((life - age) / 10.0) : 1);
        double spin = (d.a != 0 ? d.a : 0.045) * age;
        V3 c0 = pos.add(n.mul(0.045));
        // capa de tinta oscura (mezcla normal): hace que el dibujo se vea también sobre suelos claros (nieve, arena, cuarzo...)
        float[] ink = {c[0] * 0.22F, c[1] * 0.22F, c[2] * 0.22F};
        plane(s, VfxTex.SOFT, Sink.NORMAL, c0.sub(n.mul(0.004)), u.mul(R * 1.15), v.mul(R * 1.15), ink, al * 0.22);
        plane(s, VfxTex.RING, Sink.NORMAL, c0.sub(n.mul(0.002)), u.mul(R * 1.05), v.mul(R * 1.05), ink, al * 0.55);
        plane(s, VfxTex.RUNE_A, Sink.NORMAL, c0.sub(n.mul(0.001)), u.rot(n, spin).mul(R * 1.02), v.rot(n, spin).mul(R * 1.02), ink, al * 0.5);
        plane(s, VfxTex.SOFT, Sink.ADD, c0, u.mul(R * 1.3), v.mul(R * 1.3), c, al * 0.30);
        plane(s, VfxTex.RUNE_A, Sink.ADD, c0.add(n.mul(0.004)), u.rot(n, spin).mul(R), v.rot(n, spin).mul(R), hot, al * 0.95);
        double s2 = -spin * 1.7 + 0.8;
        plane(s, VfxTex.RUNE_B, Sink.ADD, c0.add(n.mul(0.008)), u.rot(n, s2).mul(R * 0.7), v.rot(n, s2).mul(R * 0.7), hot, al * 0.9);
        plane(s, VfxTex.RING, Sink.ADD, c0.add(n.mul(0.012)), u.mul(R * 1.02), v.mul(R * 1.02), c, al * 0.7);
        // pulso al aparecer
        if (age < 12) {
            double e = easeOut(age / 12.0);
            plane(s, VfxTex.RING, Sink.ADD, c0.add(n.mul(0.016)), u.mul(R * (1 + e * 0.8)), v.mul(R * (1 + e * 0.8)), hot, (1 - e) * al * 0.9);
        }
        // pilares de luz en el borde: se ven de pie, así que se distinguen aunque el círculo quede bajo la vista en primera persona
        if (n.y > 0.9) {
            int cnt = R > 4.5 ? 14 : (R > 2.2 ? 10 : 8);
            double h = Math.min(3.4, 0.9 + R * 0.2) * easeOut(age / 9.0);
            double hw = Math.min(0.28, 0.06 + R * 0.012);
            for (int i = 0; i < cnt; i++) {
                double a0 = TAU * i / cnt + spin * 0.4;
                double fl = 0.75 + 0.25 * Math.sin(age * 0.35 + i * 1.9);
                V3 base = pos.add(Math.cos(a0) * R * 0.97, 0.02, Math.sin(a0) * R * 0.97);
                V3 rad = new V3(Math.cos(a0), 0, Math.sin(a0)), tan = new V3(-Math.sin(a0), 0, Math.cos(a0));
                for (int q = 0; q < 2; q++) {
                    V3 side = (q == 0 ? rad : tan).mul(hw);
                    float[] uv = {0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F};
                    raw(s, VfxTex.COL, Sink.ADD, base.sub(side), base.add(side), base.add(side).add(0, h, 0), base.sub(side).add(0, h, 0),
                            uv, hot, al * 0.55 * fl, al * 0.55 * fl, 0, 0);
                }
            }
        }
    }

    // ------------------------------------------------------------------ columna de luz

    private static void column(VfxData d, double age, int life, double t, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        double H = (d.a > 0 ? d.a : 8) * easeOut(age / 5.0);
        double rise = clamp01(age / 3.0);
        double fall = d.life > 0 ? 1 - Math.pow(t, 3) : 1;
        double R = d.size * rise * fall;
        double al = env(age, life, 1.5, Math.max(4, life * 0.35));
        if ((d.flags & F_FLICKER) != 0) {
            al *= 0.75 + 0.25 * hash(d.id * 31L + (long) age);
        }
        int seg = 14;
        double spin = age * 0.12;
        for (int layer = 0; layer < 2; layer++) {
            double rr = layer == 0 ? R : R * 0.42;
            float[] col = layer == 0 ? c : mix(c, WHITE, 0.7);
            double la = al * (layer == 0 ? 0.55 : 0.85);
            for (int i = 0; i < seg; i++) {
                double a0 = spin * (layer == 0 ? 1 : -1.6) + TAU * i / seg, a1 = a0 + TAU / seg;
                V3 b0 = pos.add(new V3(Math.cos(a0) * rr, 0, Math.sin(a0) * rr));
                V3 b1 = pos.add(new V3(Math.cos(a1) * rr, 0, Math.sin(a1) * rr));
                V3 t0 = b0.add(0, H, 0), t1 = b1.add(0, H, 0);
                float[] uv = {0F, 1F, 1F, 1F, 1F, 0F, 0F, 0F};
                raw(s, VfxTex.COL, Sink.ADD, b0, b1, t1, t0, uv, col, la, la, la, la);
            }
        }
        // base brillante
        V3 gu = new V3(R * 3.2, 0, 0), gv = new V3(0, 0, R * 3.2);
        plane(s, VfxTex.SOFT, Sink.ADD, pos.add(0, 0.05, 0), gu, gv, c, al * 0.6);
        plane(s, VfxTex.RING, Sink.ADD, pos.add(0, 0.06, 0), gu.mul(0.9), gv.mul(0.9), mix(c, WHITE, 0.4), al * 0.5 * (1 - easeOut(age / 14.0)));
        // destello en la cima
        billboard(s, VfxTex.FLARE, Sink.ADD, pos.add(0, H, 0), R * 2.2, 0, cam, c, al * 0.5);
    }

    // ------------------------------------------------------------------ rayo (hilo)

    private static void beam(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        V3 full = new V3(d.dx, d.dy, d.dz);
        double L = full.len();
        if (L < 0.01) {
            return;
        }
        int n = (d.flags & F_CURVE) != 0 ? Math.max(8, Math.min(40, (int) (L * 3))) : 2;
        V3 axis = full.norm();
        V3 side = axis.anyPerp();
        double sag = (d.flags & F_CURVE) != 0 ? Math.min(0.8, L * 0.06) : 0;
        double amp = d.a;
        double waves = d.b > 0 ? d.b : 3;
        V3[] pts = new V3[n + 1];
        double[] hw = new double[n + 1], hw2 = new double[n + 1], a1 = new double[n + 1], a2 = new double[n + 1];
        double al = env(age, life, 2, 7);
        if ((d.flags & F_FLICKER) != 0) {
            al *= 0.6 + 0.4 * hash(d.id * 17L + (long) (age * 2));
        }
        for (int i = 0; i <= n; i++) {
            double sN = i / (double) n;
            V3 p = pos.add(full.mul(sN));
            if (sag > 0) {
                p = p.add(0, -4 * sag * sN * (1 - sN), 0);
            }
            if (amp > 0) {
                double w = Math.sin(sN * waves * TAU - age * 0.4) * amp * Math.sin(Math.PI * sN);
                p = p.add(side.mul(w)).add(axis.cross(side).mul(Math.cos(sN * waves * TAU - age * 0.4) * amp * 0.5 * Math.sin(Math.PI * sN)));
            }
            pts[i] = p;
            double pulse = 0.82 + 0.18 * Math.sin(sN * 14 - age * 0.9);
            hw[i] = d.size * 2.4;
            hw2[i] = d.size * 0.6;
            a1[i] = al * 0.6 * pulse;
            a2[i] = al * pulse;
        }
        ribbon(s, VfxTex.GLOW_LINE, Sink.ADD, pts, hw, a1, c, cam, 0, 1);
        ribbon(s, VfxTex.LINE, Sink.ADD, pts, hw2, a2, mix(c, WHITE, 0.65), cam, 0, 1);
        if ((d.flags & F_CAPS) != 0) {
            billboard(s, VfxTex.FLARE, Sink.ADD, pts[0], d.size * 7, age * 0.05, cam, c, al * 0.9);
            billboard(s, VfxTex.FLARE, Sink.ADD, pts[n], d.size * 7, -age * 0.05, cam, c, al * 0.9);
        }
    }

    // ------------------------------------------------------------------ relámpago

    private static V3[] boltPath(V3 a, V3 b, int segs, double jitter, long seed) {
        V3 axis = b.sub(a);
        V3 ax = axis.norm();
        V3 p1 = ax.anyPerp();
        V3 p2 = ax.cross(p1);
        V3[] pts = new V3[segs + 1];
        for (int i = 0; i <= segs; i++) {
            double sN = i / (double) segs;
            V3 p = a.add(axis.mul(sN));
            double taper = Math.pow(Math.sin(Math.PI * sN), 0.6);
            double j1 = (hash(seed + i * 2L) - 0.5) * 2 * jitter * taper;
            double j2 = (hash(seed + i * 2L + 1) - 0.5) * 2 * jitter * taper;
            pts[i] = (i == 0 || i == segs) ? p : p.add(p1.mul(j1)).add(p2.mul(j2));
        }
        return pts;
    }

    private static void bolt(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        V3 full = new V3(d.dx, d.dy, d.dz);
        double L = full.len();
        if (L < 0.01) {
            return;
        }
        long seed = d.id * 7919L + (long) (age * 0.5) * 104729L;
        int segs = (int) Math.max(6, Math.min(26, L * 2.6));
        double jit = d.a > 0 ? d.a : L * 0.07;
        double al = env(age, life, 0, Math.min(6, life)) * (0.75 + 0.25 * hash(seed));
        V3 end = pos.add(full);
        V3[] main = boltPath(pos, end, segs, jit, seed);
        float[] hot = mix(c, WHITE, 0.7);
        ribbonSimple(s, VfxTex.GLOW_LINE, main, d.size * 3.2, al * 0.7, c, cam);
        ribbonSimple(s, VfxTex.LINE, main, d.size * 0.9, al, hot, cam);
        int branches = d.b > 0 ? (int) d.b : 2;
        for (int k = 0; k < branches; k++) {
            int at = 1 + (int) (hash(seed + 500 + k) * (segs - 2));
            V3 from = main[at];
            V3 dir = main[Math.min(segs, at + 1)].sub(main[at - 1]).norm();
            V3 off = dir.anyPerp().rot(dir, hash(seed + 600 + k) * TAU);
            V3 bd = dir.mul(0.55).add(off.mul(0.8)).norm().mul(L * (0.18 + 0.2 * hash(seed + 700 + k)));
            V3[] br = boltPath(from, from.add(bd), Math.max(3, segs / 3), jit * 0.7, seed + 900 + k * 40L);
            ribbonSimple(s, VfxTex.GLOW_LINE, br, d.size * 1.7, al * 0.55, c, cam);
            ribbonSimple(s, VfxTex.LINE, br, d.size * 0.5, al * 0.9, hot, cam);
        }
        billboard(s, VfxTex.FLARE, Sink.ADD, pos, d.size * 9, 0, cam, c, al * 0.7);
        billboard(s, VfxTex.FLARE, Sink.ADD, end, d.size * 14, age * 0.1, cam, hot, al * 0.9);
    }

    // ------------------------------------------------------------------ corte en el aire

    private static void slash(VfxData d, double age, int life, double t, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        V3 travel = new V3(d.dx, d.dy, d.dz);
        V3 fwd = travel.len() > 1.0E-6 ? travel.norm() : cam.fwd;
        V3 r0 = Math.abs(fwd.y) > 0.97 ? new V3(1, 0, 0) : V3.UP.cross(fwd).norm();
        V3 u0 = fwd.cross(r0).norm();
        V3 r = r0.rot(fwd, d.a), u = u0.rot(fwd, d.a);
        double move = easeOut(t);
        double thick = d.b > 0 ? d.b : 0.5;
        double al = env(age, life, 0.7, Math.max(2, life * 0.55));
        for (int k = 0; k < 4; k++) {
            double back = k * 0.14 * Math.max(0.6, travel.len() * 0.4);
            V3 c0 = pos.add(travel.mul(move)).sub(fwd.mul(back));
            double sc = 1 - 0.07 * k;
            double ak = al * (1 - 0.27 * k);
            plane(s, VfxTex.SLASH, Sink.ADD, c0, r.mul(d.size * sc), u.mul(d.size * thick * sc), c, ak);
            if (k == 0) {
                plane(s, VfxTex.SLASH, Sink.ADD, c0.add(fwd.mul(0.01)), r.mul(d.size * 0.92), u.mul(d.size * thick * 0.7),
                        mix(c, WHITE, 0.55), ak * 0.55);
            }
        }
    }

    // ------------------------------------------------------------------ destello

    private static void flash(VfxData d, double age, int life, double t, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        float[] hot = mix(c, WHITE, 0.6);
        double al = Math.pow(1 - t, 1.6);
        {
            // un destello pegado a la cámara tapaba la pantalla entera: se atenúa al acercarse
            double k = clamp01((pos.sub(cam.pos).len() - 0.8) / 2.4);
            al *= k * k * (3 - 2 * k);
        }
        double sz = d.size * (0.35 + 1.1 * easeOut(t * 1.4));
        billboard(s, VfxTex.SOFT, Sink.ADD, pos, sz * 1.6, 0, cam, c, al * 0.6);
        billboard(s, VfxTex.STARBURST, Sink.ADD, pos, sz * 1.5, age * 0.09, cam, hot, al * 0.9);
        billboard(s, VfxTex.STARBURST, Sink.ADD, pos, sz * 0.9, -age * 0.14 + 0.4, cam, c, al * 0.7);
        billboard(s, VfxTex.FLARE, Sink.ADD, pos, sz * 0.7, 0, cam, WHITE, al);
        if (age < 10) {
            double e = easeOut(age / 10.0);
            billboard(s, VfxTex.HALO, Sink.ADD, pos, sz * (0.4 + 1.8 * e), 0, cam, c, (1 - e) * 0.8);
        }
    }

    // ------------------------------------------------------------------ orbe

    private static void orb(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        double al = env(age, life, 3, 6);
        if ((d.flags & F_FLICKER) != 0) {
            al *= 0.7 + 0.3 * hash(d.id * 13L + (long) (age * 1.5));
        }
        double pulse = 1 + 0.1 * Math.sin(age * 0.6 + d.id);
        double R = d.size * pulse;
        if ((d.flags & F_TRAIL) != 0 && d.trailN > 0) {
            int n = d.trailN + 1;
            V3[] pts = new V3[n];
            double[] hw = new double[n], a = new double[n];
            for (int i = 0; i < d.trailN; i++) {
                pts[i] = new V3(d.trail[i * 3], d.trail[i * 3 + 1], d.trail[i * 3 + 2]);
            }
            pts[n - 1] = pos;
            for (int i = 0; i < n; i++) {
                double f = (i + 1) / (double) n;
                hw[i] = R * 0.8 * f;
                a[i] = al * 0.75 * f * f;
            }
            ribbon(s, VfxTex.GLOW_LINE, Sink.ADD, pts, hw, a, c, cam, 0, 1);
        }
        billboard(s, VfxTex.SOFT, Sink.ADD, pos, R * 2.6, 0, cam, c, al * 0.4);
        billboard(s, VfxTex.ORB, Sink.ADD, pos, R * 1.15, 0, cam, c, al * 0.95);
        billboard(s, VfxTex.FLARE, Sink.ADD, pos, R * 0.75, 0, cam, mix(c, WHITE, 0.8), al);
        billboard(s, VfxTex.STARBURST, Sink.ADD, pos, R * 1.9, age * 0.07 + d.id, cam, c, al * 0.35);
    }

    // ------------------------------------------------------------------ cúpula

    private static void dome(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        boolean hemi = (d.flags & F_HEMI) != 0;
        int lon = 26, lat = hemi ? 9 : 15;
        double R = d.size * easeOutBack(age / 9.0) * (1 + 0.015 * Math.sin(age * 0.5));
        double spin = age * 0.012;
        double base = env(age, life, 5, 12);
        if (d.life > 0 && life - age < 30) {
            base *= 0.65 + 0.35 * Math.sin(age * 1.7);
        }
        double flash = clamp01(d.a);
        double phiMax = hemi ? Math.PI / 2 : Math.PI;
        V3[][] P = new V3[lat + 1][lon + 1];
        double[][] A = new double[lat + 1][lon + 1];
        for (int i = 0; i <= lat; i++) {
            double phi = phiMax * i / lat;
            for (int j = 0; j <= lon; j++) {
                double th = TAU * (j / (double) lon + spin);
                V3 nrm = new V3(Math.sin(phi) * Math.cos(th), Math.cos(phi), Math.sin(phi) * Math.sin(th));
                V3 p = pos.add(nrm.mul(R));
                double nv = Math.abs(nrm.dot(cam.pos.sub(p).norm()));
                double a = base * (0.16 + 0.84 * Math.pow(1 - nv, 1.8));
                a += flash * 0.55 * base;
                if (hemi && i == lat) {
                    a = Math.max(a, base * 0.55);
                }
                P[i][j] = p;
                A[i][j] = a;
            }
        }
        for (int i = 0; i < lat; i++) {
            for (int j = 0; j < lon; j++) {
                float u0 = j / (float) lon * 7F + (float) (age * 0.004), u1 = (j + 1) / (float) lon * 7F + (float) (age * 0.004);
                float v0 = i / (float) lat * (hemi ? 1.01F : 2.02F), v1 = (i + 1) / (float) lat * (hemi ? 1.01F : 2.02F);
                float[] uv = {u0, v0, u1, v0, u1, v1, u0, v1};
                raw(s, VfxTex.DOME, Sink.ADD, P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j], uv, c,
                        A[i][j], A[i][j + 1], A[i + 1][j + 1], A[i + 1][j]);
            }
        }
        if (hemi) {
            plane(s, VfxTex.RING, Sink.ADD, pos.add(0, 0.05, 0), new V3(R, 0, 0), new V3(0, 0, R), mix(c, WHITE, 0.3), base * 0.8);
        }
    }

    // ------------------------------------------------------------------ tornado

    private static void funnel(VfxData d, double age, int life, V3 pos, Sink s) {
        float[] c = rgb(d.rgb);
        int rows = 12, cols = 22;
        double H = d.a > 0 ? d.a : 4;
        double al = env(age, life, 6, 10);
        double grow = easeOut(age / 8.0);
        V3[][] P = new V3[rows + 1][cols + 1];
        double[] A = new double[rows + 1];
        for (int i = 0; i <= rows; i++) {
            double h = i / (double) rows;
            double r = lerp(d.size * 0.28, d.size, Math.pow(h, 1.1)) * grow;
            double ox = Math.sin(age * 0.21 + h * 2.4) * 0.12 * h * d.size;
            double oz = Math.cos(age * 0.17 + h * 2.1) * 0.12 * h * d.size;
            A[i] = Math.pow(Math.sin(Math.PI * Math.min(0.999, Math.max(0.001, h))), 0.7) * 0.75 * al;
            for (int j = 0; j <= cols; j++) {
                double th = TAU * j / cols + age * 0.33 * (1 - 0.35 * h);
                P[i][j] = pos.add(new V3(Math.cos(th) * r + ox, h * H * grow, Math.sin(th) * r + oz));
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                float off = (float) (age * 0.05);
                float u0 = j / (float) cols * 2F + off, u1 = (j + 1) / (float) cols * 2F + off;
                float v0 = i / (float) rows * 1.6F, v1 = (i + 1) / (float) rows * 1.6F;
                float[] uv = {u0, v0, u1, v0, u1, v1, u0, v1};
                raw(s, VfxTex.SWIRL, Sink.ADD, P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j], uv, c, A[i], A[i], A[i + 1], A[i + 1]);
            }
        }
        plane(s, VfxTex.SOFT, Sink.ADD, pos.add(0, 0.06, 0), new V3(d.size * 1.1, 0, 0), new V3(0, 0, d.size * 1.1), c, al * 0.4);
    }

    // ------------------------------------------------------------------ sprite

    private static void sprite(VfxData d, double age, double partial, int life, V3 pos, Cam cam, Sink s) {
        int tex = (int) d.a;
        if (tex < 0 || tex >= VfxTex.COUNT) {
            return;
        }
        if (tex == VfxTex.DRAGON3D) {
            Dragon3D.emit(d, age, partial, pos, cam, s);
            return;
        }
        if (tex == VfxTex.CREATURE3D) {
            Creature3D.emit(d, age, partial, pos, cam, s);
            return;
        }
        float[] c = rgb(d.rgb);
        int fr = Math.max(1, VfxTex.FRAMES[tex]);
        int frame = (int) (age / Math.max(1, VfxTex.FRAME_TICKS[tex])) % fr;
        double hgt = d.size;
        double wid = d.size * VfxTex.ASPECT[tex];
        double al = env(age, life, 3, 6);
        // si pasa muy cerca de la cámara se desvanece (antes tapaba media pantalla como una mancha blanca)
        {
            double cd = pos.add(0, hgt * 0.5, 0).sub(cam.pos).len();
            double k = clamp01((cd - 1.3) / 2.2);
            al *= k * k * (3 - 2 * k);
            if (al < 0.01) {
                return;
            }
        }
        V3 horiz = new V3(cam.right.x, 0, cam.right.z);
        horiz = horiz.len() < 1.0E-6 ? new V3(1, 0, 0) : horiz.norm();
        V3 dirv = new V3(d.dx, d.dy, d.dz);
        boolean flip = dirv.len() > 1.0E-4 && dirv.dot(horiz) < 0;
        double bob = Math.sin(age * 0.2 + d.id) * 0.05 * (d.b > 0 ? d.b : 1);
        V3 c0 = pos.add(0, bob + hgt * 0.5, 0);
        boolean glow = (d.flags & F_ADD) != 0;
        if ((d.flags & F_TRAIL) != 0 && d.trailN > 1) {
            // estela de polvo brillante detrás del sprite
            int n = d.trailN + 1;
            V3[] pts = new V3[n];
            double[] hw = new double[n], aa = new double[n];
            for (int i = 0; i < d.trailN; i++) {
                pts[i] = new V3(d.trail[i * 3], d.trail[i * 3 + 1] + hgt * 0.5, d.trail[i * 3 + 2]);
            }
            pts[n - 1] = c0;
            for (int i = 0; i < n; i++) {
                double f = (i + 1) / (double) n;
                hw[i] = hgt * 0.16 * f;
                aa[i] = al * 0.7 * f * f;
            }
            ribbon(s, VfxTex.GLOW_LINE, Sink.ADD, pts, hw, aa, c, cam, 0, 1);
        }
        if (glow) {
            // resplandor de color detrás del dibujo (el dibujo se pinta normal para que se lea de día y de noche)
            double pul = 0.85 + 0.15 * Math.sin(age * 0.45 + d.id);
            // brillo de color (mezcla normal: tiñe el fondo en vez de blanquearlo, así se ve de día igual que de noche)
            billboard(s, VfxTex.SOFT, Sink.NORMAL, c0, hgt * 1.05 * pul, 0, cam, c, al * 0.26);
            billboard(s, VfxTex.SOFT, Sink.ADD, c0, hgt * 0.8 * pul, 0, cam, c, al * 0.16);
        }
        planeF(s, tex, Sink.NORMAL, c0, horiz.mul(wid * 0.5), V3.UP.mul(hgt * 0.5), glow ? mix(c, WHITE, 0.10) : c, al, frame, flip);
        if (glow) {
            // destellitos que giran alrededor
            for (int k = 0; k < 2; k++) {
                double an = age * 0.16 + k * Math.PI + d.id;
                V3 sp = c0.add(horiz.mul(Math.cos(an) * hgt * 0.55)).add(0, Math.sin(an * 1.3) * hgt * 0.35, 0);
                billboard(s, VfxTex.FLARE, Sink.ADD, sp, hgt * 0.16, 0, cam, mix(c, WHITE, 0.7),
                        al * (0.4 + 0.6 * Math.abs(Math.sin(age * 0.5 + k * 2 + d.id))));
            }
        }
    }

    // ------------------------------------------------------------------ árbol / estandarte

    private static void cross(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        int tex = (int) d.b;
        if (tex < 0 || tex >= VfxTex.COUNT) {
            return;
        }
        float[] c = rgb(d.rgb);
        double growT = d.a > 0 ? d.a : 20;
        double sc = easeOutBack(age / growT);
        double al = env(age, life, 3, 20);
        double H = d.size * sc;
        double W = d.size * VfxTex.ASPECT[tex] * sc;
        int style = VfxType.style(d.flags);
        if (tex == VfxTex.TREE || tex == VfxTex.BANNER) {
            // árbol y estandarte ahora son modelos 3D de verdad; aquí solo queda el resplandor del suelo
            if (tex == VfxTex.TREE) {
                Props3D.tree(d, age, life, pos, cam, s);
            } else {
                Props3D.banner(d, age, life, pos, cam, s);
            }
            double gl3 = d.size * 0.9 * sc;
            plane(s, VfxTex.SOFT, Sink.ADD, pos.add(0, 0.05, 0), new V3(gl3, 0, 0), new V3(0, 0, gl3), rgb(d.rgb), al * 0.30);
            plane(s, VfxTex.RING, Sink.ADD, pos.add(0, 0.06, 0), new V3(gl3 * 0.85, 0, 0), new V3(0, 0, gl3 * 0.85), mix(rgb(d.rgb), WHITE, 0.3),
                    al * 0.4 * (0.6 + 0.4 * Math.sin(age * 0.1)));
            return;
        }
        int cols = 6, rows = 6;
        int planes = style == 1 ? 1 : 2;
        V3 dirv = new V3(d.dx, 0, d.dz);
        double yaw0 = dirv.len() > 1.0E-4 ? Math.atan2(dirv.z, dirv.x) : 0;
        for (int pl = 0; pl < planes; pl++) {
            V3 right;
            if (style == 1) {
                right = new V3(cam.right.x, 0, cam.right.z);
                right = right.len() < 1.0E-6 ? new V3(1, 0, 0) : right.norm();
            } else {
                double ang = yaw0 + pl * Math.PI / 2 + Math.PI / 4;
                right = new V3(Math.cos(ang), 0, Math.sin(ang));
            }
            V3 nrm = right.cross(V3.UP).norm();
            // Los dos planos del árbol se funden según desde dónde se mire: el que queda de canto desaparece
            // (antes se veía como rayas verdes verticales en el centro).
            double wgt = 1;
            if (style != 1) {
                V3 tc = new V3(cam.pos.x - pos.x, 0, cam.pos.z - pos.z);
                tc = tc.len() < 1.0E-6 ? new V3(0, 0, 1) : tc.norm();
                wgt = Math.min(1.0, Math.abs(nrm.dot(tc)) * 1.8);
                if (wgt < 0.03) {
                    continue;
                }
            }
            double alp = al * wgt;
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    V3[] q = new V3[4];
                    int[][] corner = {{j, i}, {j + 1, i}, {j + 1, i + 1}, {j, i + 1}};
                    for (int k = 0; k < 4; k++) {
                        double fu = corner[k][0] / (double) cols, fv = corner[k][1] / (double) rows;
                        double sway;
                        if (style == 1) {
                            sway = Math.sin(age * 0.22 + fu * 5.0 + fv * 1.5) * 0.16 * fu * fu * d.size * 0.5;
                        } else {
                            sway = Math.sin(age * 0.11 + d.id * 0.7 + pl) * 0.010 * d.size * fv * fv;
                        }
                        q[k] = pos.add(right.mul(style == 1 ? fu * W : (fu - 0.5) * W)).add(0, fv * H, 0).add(nrm.mul(sway));
                    }
                    float u0 = j / (float) cols, u1 = (j + 1) / (float) cols;
                    float v0 = 1F - (i + 1) / (float) rows, v1 = 1F - i / (float) rows;
                    float[] uv = {u0, v1, u1, v1, u1, v0, u0, v0};
                    raw(s, tex, Sink.NORMAL, q[0], q[1], q[2], q[3], uv, c, alp, alp, alp, alp);
                }
            }
        }
        double gl = d.size * 0.9 * sc;
        plane(s, VfxTex.SOFT, Sink.ADD, pos.add(0, 0.05, 0), new V3(gl, 0, 0), new V3(0, 0, gl), rgb(d.rgb), al * 0.4);
        plane(s, VfxTex.RING, Sink.ADD, pos.add(0, 0.06, 0), new V3(gl * 0.85, 0, 0), new V3(0, 0, gl * 0.85), mix(rgb(d.rgb), WHITE, 0.3),
                al * 0.5 * (0.6 + 0.4 * Math.sin(age * 0.1)));
    }

    // ------------------------------------------------------------------ mandíbulas

    private static void maw(VfxData d, double age, int life, double t, V3 pos, Cam cam, Sink s) {
        Creature3D.Ctx fc = Props3D.world(s, cam, V3.ZERO, 1, 0);
        float[] c = rgb(d.rgb);
        float[] bone = mix(c, WHITE, 0.55);
        int n = Math.max(6, (int) Math.round(6 + d.size * 2.2));
        double H = d.a > 0 ? d.a : 1.2;
        double R = d.size;
        double lean = 0.38;
        double globalFade = clamp01(age / 2.0);
        for (int i = 0; i < n; i++) {
            double th = TAU * i / n + hash(d.id * 3L + i) * 0.4;
            double delay = hash(d.id * 5L + i) * 0.15;
            double p = clamp01((t - delay) / 0.22);
            double sink = clamp01((t - 0.72) / 0.28);
            double prog = easeOutBack(p) * (1 - smooth(sink));
            double h = H * (0.75 + 0.5 * hash(d.id * 7L + i)) * Math.max(0, prog);
            if (h < 0.02) {
                continue;
            }
            V3 rad = new V3(Math.cos(th), 0, Math.sin(th));
            V3 tan = new V3(-Math.sin(th), 0, Math.cos(th));
            V3 base = pos.add(rad.mul(R * (0.9 + 0.2 * hash(d.id * 11L + i))));
            V3 g = V3.UP.mul(Math.cos(lean)).sub(rad.mul(Math.sin(lean)));
            V3 s2 = g.cross(tan).norm();
            Props3D.fang(fc, base, g, tan, h * (0.4 + 0.6 * globalFade), bone, globalFade);
            // brillo en la punta
            if (p > 0.2 && sink < 0.5) {
                plane(s, VfxTex.SOFT, Sink.ADD, base.add(g.mul(h * 0.6)), tan.mul(0.35), V3.UP.mul(0.35), c, 0.25 * (1 - sink * 2));
            }
        }
        double al = env(age, life, 2, 10);
        plane(s, VfxTex.RUNE_B, Sink.ADD, pos.add(0, 0.04, 0), new V3(R * 1.25, 0, 0).rot(V3.UP, age * 0.05), new V3(0, 0, R * 1.25).rot(V3.UP, age * 0.05), c, al * 0.8);
        plane(s, VfxTex.SOFT, Sink.ADD, pos.add(0, 0.03, 0), new V3(R * 1.5, 0, 0), new V3(0, 0, R * 1.5), c, al * 0.3);
    }

    // ------------------------------------------------------------------ látigo / cinta

    private static void whip(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c = rgb(d.rgb);
        V3 full = new V3(d.dx, d.dy, d.dz);
        double L = full.len();
        if (L < 0.01) {
            return;
        }
        boolean grow = (d.flags & F_GROW) != 0;
        double g = grow ? easeOut(age / Math.max(1.0, 0.35 * life)) : 1;
        int n = 22;
        V3 axis = full.norm();
        V3 side = axis.anyPerp();
        V3 up2 = axis.cross(side);
        double amp = d.a > 0 ? d.a : 0.3;
        double waves = d.b > 0 ? d.b : 2.5;
        double al = env(age, life, 1, Math.max(3, life * 0.4));
        V3[] pts = new V3[n + 1];
        double[] hw = new double[n + 1], a = new double[n + 1], hw2 = new double[n + 1];
        double decay = grow ? (1 - 0.6 * clamp01((age - 0.35 * life) / (0.65 * life))) : 1;
        for (int i = 0; i <= n; i++) {
            double sN = i / (double) n;
            double w = Math.sin(sN * waves * TAU - age * 0.55) * amp * Math.pow(Math.sin(Math.PI * Math.min(1, sN * 0.97 + 0.015)), 0.7) * decay;
            double w2 = Math.cos(sN * waves * TAU - age * 0.55) * amp * 0.5 * Math.sin(Math.PI * sN) * decay;
            pts[i] = pos.add(axis.mul(sN * g * L)).add(side.mul(w)).add(up2.mul(w2));
            hw[i] = d.size * (1 - 0.82 * sN);
            hw2[i] = hw[i] * 0.4;
            a[i] = al * (0.45 + 0.55 * (1 - sN * 0.6));
        }
        int tex = VfxType.style(d.flags) == 1 ? VfxTex.RIB_VOID : VfxTex.RIB_FLAME;
        ribbon(s, tex, Sink.ADD, pts, hw, a, c, cam, -age * 0.3, 0.9);
        ribbon(s, VfxTex.LINE, Sink.ADD, pts, hw2, a, mix(c, WHITE, 0.7), cam, 0, 1);
        if (grow && g > 0.97) {
            double f = clamp01(1 - (age - 0.35 * life) / 5.0);
            if (f > 0) {
                billboard(s, VfxTex.STARBURST, Sink.ADD, pts[n], d.size * 9 * (1 + (1 - f)), age, cam, mix(c, WHITE, 0.5), f);
            }
        }
        billboard(s, VfxTex.FLARE, Sink.ADD, pts[0], d.size * 3.5, 0, cam, c, al * 0.8);
    }

    // ------------------------------------------------------------------ moneda

    private static void coin(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        if (true) {
            Props3D.coin(d, age, life, pos, cam, s);
            return;
        }
        float[] c = rgb(d.rgb);
        double flightT = clamp01(age / (life * 0.62));
        double rest = clamp01((age - life * 0.62) / (life * 0.38));
        double peak = d.a > 0 ? d.a : 1.8;
        double h = peak * Math.pow(Math.sin(Math.PI * flightT), 0.85);
        int face = d.b >= 0.5 ? 1 : 0;
        int halfTurns = 10 + face;
        double ang = Math.PI * halfTurns * (1 - Math.pow(1 - flightT, 2.2));
        V3 c0 = pos.add(0, h + 0.3, 0);
        V3 up2 = cam.up.mul(Math.cos(ang)).add(cam.fwd.mul(Math.sin(ang)));
        double front = Math.cos(ang);
        double sz = d.size;
        double al = env(age, life, 2, 8);
        int frame = front >= 0 ? 0 : 1;
        double vsign = front >= 0 ? 1 : -1;
        V3 ux = cam.right.mul(sz), vy = up2.mul(sz * vsign);
        float v0 = frame / 2F, v1 = (frame + 1) / 2F;
        float[] uv = {0F, v1, 1F, v1, 1F, v0, 0F, v0};
        raw(s, VfxTex.COIN, Sink.NORMAL, c0.sub(ux).sub(vy), c0.add(ux).sub(vy), c0.add(ux).add(vy), c0.sub(ux).add(vy), uv, c, al, al, al, al);
        // brillo y destellos
        double glint = 0.35 + 0.25 * Math.sin(age * 0.8) + (flightT >= 1 ? 0.5 * (1 - rest) : 0);
        billboard(s, VfxTex.SOFT, Sink.ADD, c0, sz * 3.0, 0, cam, c, al * 0.45);
        billboard(s, VfxTex.STARBURST, Sink.ADD, c0.add(cam.right.mul(sz * 0.4)).add(cam.up.mul(sz * 0.4)), sz * 1.4, age * 0.2, cam, WHITE, al * glint);
        if (flightT >= 1) {
            double e = easeOut(rest * 2.5);
            billboard(s, VfxTex.HALO, Sink.ADD, c0, sz * (1 + 3 * e), 0, cam, c, (1 - e) * 0.9 * al);
        }
    }
}
