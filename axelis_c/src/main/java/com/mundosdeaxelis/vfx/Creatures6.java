package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Compañeros del lote 6 (primera parte): pulpo, mariposa, cangrejo, luciérnaga, koala y elefante bebé.
 * Mismo motor que {@link Creature3D}; el detalle (pelaje, brillos, tics) sale de {@link Detail}.
 */
final class Creatures6 {
    private Creatures6() {
    }

    // ------------------------------------------------------------------ 36. PULPO

    private static final float[] PU_B = {0.82F, 0.52F, 0.88F}, PU_B2 = {0.97F, 0.76F, 0.97F}, PU_DK = {0.58F, 0.32F, 0.72F}, PU_S = {1F, 0.82F, 0.92F},
            PU_ST = {1F, 0.62F, 0.35F};

    static void octopus(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w + 0.006 * Math.sin(t * 0.1 + ph);
        c.pivot = new V3(0, 0.25, 0);
        // ocho tentáculos que se ondulan y apoyan; uno saluda de vez en cuando
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.2;
            V3 dir = new V3(Math.sin(a), 0, Math.cos(a)), side = new V3(Math.cos(a), 0, -Math.sin(a));
            int n = 6;
            V3[] cp = new V3[n];
            double[] rd = new double[n];
            double hello = k == 1 ? Detail.flick(c, 50) : 0;
            for (int i = 0; i < n; i++) {
                double f = i / (double) (n - 1);
                double dist = 0.06 + f * 0.24;
                double wave = Math.sin(t * 0.12 + ph + k * 0.8 - f * 3.0) * 0.035 * f + Math.sin(c.gait - f * 2.2 + k) * 0.05 * w * f;
                double y = 0.20 + bob - 0.185 * ss(0, 0.55, f) + 0.075 * ss(0.65, 1, f) + hello * 0.35 * f * f;
                cp[i] = dir.mul(dist).add(side.mul(wave)).add(0, y, 0);
                rd[i] = 0.052 * (1 - f * 0.82) + 0.006;
            }
            curve(c, cp, rd, 2, 6, (nn, p) -> mix(PU_B, PU_B2, ss(0.0, -1.0, nn.y) * 0.55));
            for (int i = 1; i < n - 1; i++) {
                V3 sp = cp[i].add(0, -rd[i] * 0.85, 0);
                Detail.kite(c, sp, sp.add(dir.mul(0.016)), side, 0.0065, new V3(0, -1, 0), PU_S, scale(PU_S, 1.1));
            }
        }
        // falda y manto (la cabeza grande)
        ell(c, new V3(0, 0.26 + bob, 0), new V3(0.15, 0.085, 0.15), 14, 7, (n, p) -> mix(PU_B, PU_B2, ss(0, -1, n.y) * 0.5));
        headLook(c, 0.09, 0.0);
        c.hpv = new V3(0, 0.30, 0.0);
        V3 hc = new V3(0, 0.44 + bob, -0.015), hr = new V3(0.185, 0.215, 0.18);
        Paint hp = (n, p) -> mix(PU_B, PU_B2, ss(-0.2, 0.9, n.z - n.y * 0.2) * 0.55);
        ell(c, hc, hr, 18, 13, hp);
        for (int k = 0; k < 11; k++) {
            V3 d = Detail.sph(401, k);
            if (d.y < 0.2) {
                d = new V3(d.x, 0.2 + (0.2 - d.y), d.z);
            }
            if (d.z > 0.45) {
                d = new V3(d.x, d.y, -d.z);
            }
            Detail.spot(c, hc, hr, d, 0.035 + 0.02 * Detail.rnd(401, k, 8), PU_DK);
        }
        Detail.glint(c, hc, hr, new V3(-0.4, 1, 0.3), 0.08, WHITE, 0.75);
        Detail.glint(c, hc, hr, new V3(0.55, 0.7, -0.3), 0.05, WHITE, 0.6);
        eyes(c, hc, hr, 0.50, -0.02, 0.86, 0.125, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.86, -0.28, 0.45, 0.085);
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.35F}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.50, 0.26, 0.82, 0.07, sd * 0.4, PU_DK);
        }
        // estrellita de mar de adorno
        {
            V3 sc = new V3(0.07, 0.655 + bob, 0.07);
            for (int k = 0; k < 5; k++) {
                double a = k * Math.PI * 2 / 5 + 0.5;
                V3 tp = sc.add(Math.cos(a) * 0.05, 0.012, Math.sin(a) * 0.05);
                Detail.kite(c, sc, tp, new V3(-Math.sin(a), 0, Math.cos(a)), 0.016, new V3(0, 1, 0), PU_ST, new float[]{1F, 0.8F, 0.5F});
            }
            ell(c, sc.add(0, 0.006, 0), new V3(0.016, 0.01, 0.016), 6, 4, flat(PU_ST));
            Detail.twinkle(c, sc.add(0, 0.02, 0), 0.016, new float[]{1F, 0.9F, 0.6F}, t, ph, 0.1);
        }
        c.head = false;
        // burbujitas que suben
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.25) % 1.0);
            V3 bp = new V3(Math.sin(k * 2.3 + u * 6) * 0.06 + 0.1, 0.55 + u * 0.4, 0.14 + Math.cos(k * 1.7) * 0.06);
            c.blend = Sink.NORMAL;
            c.alpha = 0.4 * (1 - u * 0.6);
            ell(c, bp, new V3(0.014, 0.014, 0.014), 8, 5, flat(new float[]{0.85F, 0.95F, 1F}));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
    }

    // ------------------------------------------------------------------ 37. MARIPOSA (vuela)

    // Fosu (mítica): mariposa de zafiro con alas de abanico, borde marino con lunares de luz, venas brillantes,
    // destellos que cambian de color, halo de estrellas que giran y rastro de polvo de luz.
    private static final float[] BU_SA = {0.12F, 0.42F, 1F}, BU_CY = {0.40F, 0.86F, 1F}, BU_PA = {0.88F, 0.98F, 1F},
            BU_VI = {0.64F, 0.46F, 1F}, BU_NV = {0.025F, 0.04F, 0.17F}, BU_FZ = {0.50F, 0.62F, 1F}, BU_GD = {1F, 0.90F, 0.55F};

    /** Punto de un ala: ángulo a (0 = hacia fuera, positivo hacia delante) y radio r; ang es el aleteo, lag el retraso de la punta. */
    private static V3 bwp(int sd, V3 root, double ang, double lag, double a, double r, double rmax) {
        double xo = r * Math.cos(a), z = r * Math.sin(a);
        double sf = Math.min(1, Math.abs(xo) / Math.max(1e-6, rmax));
        double an = ang - lag * sf;
        double cam = 0.014 * Math.sin(Math.PI * Math.min(1, r / rmax)) * Math.cos(an);
        return root.add(sd * xo * Math.cos(an), xo * Math.sin(an) + cam, z);
    }

    private static double bwR(boolean fore, double f, double rmax) {
        double r;
        if (fore) {
            r = f < 0.40 ? 0.64 + 0.36 * Math.pow(f / 0.40, 0.8) : 1 - 0.74 * Math.pow((f - 0.40) / 0.60, 1.5);
        } else {
            r = 0.45 + 0.55 * Math.sin(Math.PI * Math.min(1, f * 0.95 + 0.04)) * 0.95;
            r += 0.55 * Math.exp(-Math.pow((f - 0.66) / 0.075, 2));
        }
        r *= 1 - 0.035 * Math.cos(f * 21);
        return r * rmax;
    }

    private static void bwing(Ctx c, int sd, V3 root, double ang, double lag, boolean fore, double rmax, double t, double ph) {
        double a0 = fore ? -0.30 : -2.35, a1 = fore ? 1.55 : -0.12;
        int nf = Math.max(8, (int) Math.round(18 * c.lod)), ns = Math.max(5, (int) Math.round(9 * c.lod));
        V3[][] P = new V3[nf + 1][ns + 1];
        float[][][] C = new float[nf + 1][ns + 1][];
        for (int i = 0; i <= nf; i++) {
            double f = (double) i / nf, a = a0 + (a1 - a0) * f, rr = bwR(fore, f, rmax);
            for (int j = 0; j <= ns; j++) {
                double s = (double) j / ns;
                P[i][j] = bwp(sd, root, ang, lag, a, rr * s, rmax);
                float[] col = mix(BU_SA, BU_CY, ss(0.08, 0.62, s) * (0.75 + 0.25 * Math.sin(f * 9 + (fore ? 0 : 2))));
                double sh = 0.5 + 0.5 * Math.sin(f * 15 + s * 4 - t * 0.04 + ph + (fore ? 0 : 1.5));
                col = mix(col, BU_VI, ss(0.72, 1.0, sh) * ss(0.25, 0.9, s) * 0.55);
                col = mix(col, BU_PA, ss(0.55, 0.88, s) * (0.25 + 0.2 * Math.sin(f * 7 + 1)) * (fore ? 1 : 0.7));
                col = mix(col, BU_NV, ss(0.86, 0.96, s) * 0.85);
                col = mix(col, BU_NV, ss(0.16, 0.02, s) * 0.8);
                C[i][j] = col;
            }
        }
        for (int i = 0; i < nf; i++) {
            for (int j = 0; j < ns; j++) {
                V3[] p = {P[i][j], P[i + 1][j], P[i + 1][j + 1], P[i][j + 1]};
                V3 n = p[1].sub(p[0]).cross(p[3].sub(p[0])).norm();
                if (n.y < 0) {
                    n = n.mul(-1);
                }
                V3[] nn = {n, n, n, n};
                c.quad(c.blend, c.alpha, T_WHITE, p, nn, new float[][]{C[i][j], C[i + 1][j], C[i + 1][j + 1], C[i][j + 1]}, null);
            }
        }
        boolean em = c.emissive;
        c.emissive = true;
        int nv = fore ? 5 : 4;
        for (int k = 0; k < nv; k++) {
            double f = fore ? 0.08 + 0.84 * k / (nv - 1) : 0.12 + 0.76 * k / (nv - 1), a = a0 + (a1 - a0) * f, rr = bwR(fore, f, rmax);
            V3[] pts = new V3[4];
            for (int j = 0; j < 4; j++) {
                pts[j] = bwp(sd, root, ang, lag, a, rr * (0.10 + 0.78 * j / 3.0), rmax);
            }
            tube(c, pts, new double[]{0.004, 0.0034, 0.0028, 0.002}, 3, flat(mix(BU_PA, BU_CY, 0.3 + 0.3 * Math.sin(k + ph))));
        }
        for (int k = 0; k < (fore ? 7 : 5); k++) {
            double f = fore ? 0.06 + 0.88 * k / 6.0 : 0.12 + 0.78 * k / 4.0, a = a0 + (a1 - a0) * f, rr = bwR(fore, f, rmax);
            V3 dp = bwp(sd, root, ang, lag, a, rr * 0.915, rmax);
            double sz = 0.0095 + 0.004 * Math.sin(k * 1.7 + (fore ? 0 : 1)) * 0.5 + (fore ? 0 : 0.002);
            ell(c, dp, new V3(sz, sz * 0.55, sz), 6, 4, flat(BU_PA));
        }
        for (int k = 0; k < 3; k++) {
            double f = 0.25 + 0.25 * k, a = a0 + (a1 - a0) * f, rr = bwR(fore, f, rmax);
            Detail.twinkle(c, bwp(sd, root, ang, lag, a, rr * (0.45 + 0.17 * k), rmax), 0.018, mix(BU_PA, BU_VI, k * 0.3), t, ph + k * 2 + sd + (fore ? 0 : 5), 0.11);
        }
        c.emissive = em;
    }

    static void butterfly(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.2 + ph) * 0.03;
        c.pivot = new V3(0, 0.45, 0);
        double fl = Math.sin(t * 0.5 + ph), fd = Math.cos(t * 0.5 + ph);
        double ang = 0.12 + 0.78 * (0.5 + 0.5 * fl);
        boolean em0 = c.emissive;
        // aura: halo grande y suave, estrellas que giran alrededor
        glow(c, new V3(0, 0.46 + bob, 0), 1.5, BU_CY, 0.11 + 0.04 * Math.sin(t * 0.2));
        glow(c, new V3(0, 0.46 + bob, 0), 0.7, BU_PA, 0.10);
        for (int k = 0; k < 9; k++) {
            double a = t * 0.035 + k * Math.PI * 2 / 9, r = 0.50 + 0.04 * Math.sin(t * 0.1 + k);
            V3 mp = new V3(Math.cos(a) * r, 0.46 + bob + Math.sin(a) * r * 0.30 * Math.cos(0.6), Math.sin(a) * r * 0.55 - 0.02);
            Detail.twinkle(c, mp, 0.016 + 0.006 * (k % 3), k % 3 == 0 ? BU_GD : k % 3 == 1 ? BU_PA : BU_CY, t, ph + k * 1.3, 0.2);
        }
        // alas: delanteras grandes y traseras con cola
        for (int sd = -1; sd <= 1; sd += 2) {
            bwing(c, sd, new V3(sd * 0.012, 0.465 + bob, 0.03), ang, 0.20 * fd, true, 0.40, t, ph);
            bwing(c, sd, new V3(sd * 0.012, 0.455 + bob, -0.02), ang - 0.07 + 0.03 * fl, 0.24 * fd, false, 0.30, t, ph);
        }
        // cuerpo peludito
        V3 ab = new V3(0, 0.44 + bob, -0.10), abr = new V3(0.032, 0.032, 0.125);
        ell(c, ab, abr, 10, 7, (n, p) -> mix(BU_NV, BU_FZ, ss(-0.5, 0.9, n.y) * 0.55 + ss(0.0, -0.9, p.z) * 0.2));
        Detail.fur(c, ab, abr, 30, 411, 0.02, 0.01, new V3(0, 0, -1), 0.9, Detail.ALL, flat(BU_FZ), null);
        V3 th = new V3(0, 0.46 + bob, 0.01), thr = new V3(0.055, 0.055, 0.065);
        ell(c, th, thr, 10, 7, flat(BU_PA));
        Detail.fur(c, th, thr, 50, 412, 0.03, 0.014, new V3(0, 0, -1), 0.9, Detail.ALL, flat(BU_PA), new float[]{0.80F, 0.95F, 1F});
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 a = new V3(sd * 0.03, 0.42 + bob, 0.04 - k * 0.04), b = new V3(sd * 0.065, 0.34 + bob + Math.sin(t * 0.3 + k + sd) * 0.008, 0.06 - k * 0.05);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.006, 0.005, 0.004}, 4, flat(BU_NV));
            }
        }
        // cabeza
        headLook(c, 0.13, 0.03);
        c.hpv = new V3(0, 0.46, 0.06);
        V3 hc = new V3(0, 0.48 + bob, 0.095), hr = new V3(0.075, 0.07, 0.07);
        ell(c, hc, hr, 14, 10, flat(new float[]{0.96F, 0.98F, 1F}));
        eyes(c, hc, hr, 0.56, 0.12, 0.80, 0.075, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.90, -0.25, 0.42, 0.05);
        decal(c, hc, hr, new V3(0, -0.40, 1), 0.04, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.3F}, 0.9, 0.003);
        // corona de luz: un pequeño cristal entre las antenas
        c.emissive = true;
        ell(c, hc.add(0, 0.075, 0.015), new V3(0.011, 0.020, 0.011), 6, 4, flat(BU_GD));
        glow(c, hc.add(0, 0.085, 0.015), 0.14, BU_GD, 0.35);
        c.emissive = em0;
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 51 : 52);
            double sw = Math.sin(t * 0.15 + sd) * 0.012 + 0.025 * ef;
            V3 a = new V3(sd * 0.03, 0.54 + bob, 0.11), m = new V3(sd * (0.06 + sw), 0.63 + bob, 0.14), b = new V3(sd * (0.10 + sw * 1.5), 0.69 + bob, 0.20);
            tube(c, new V3[]{a, m, b}, new double[]{0.004, 0.0035, 0.003}, 4, flat(BU_NV));
            c.emissive = true;
            ell(c, b, new V3(0.012, 0.012, 0.012), 6, 4, flat(BU_PA));
            glowStar(c, b, 0.06, BU_CY, t, ph + sd);
            c.emissive = em0;
        }
        // trompa enroscada
        tube(c, new V3[]{new V3(0, 0.465 + bob, 0.16), new V3(0, 0.45 + bob, 0.185), new V3(0, 0.435 + bob, 0.175)}, new double[]{0.006, 0.005, 0.004}, 4, flat(BU_NV));
        c.head = false;
        // rastro de polvo de luz y polen que cae
        for (int k = 0; k < 12; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k / 12.0) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.6 + u * 5) * (0.12 + 0.30 * u), 0.40 + bob - u * 0.22, -0.18 - u * 0.55 + Math.cos(k * 1.9) * 0.05),
                    0.016 * (1 - u * 0.6), k % 4 == 0 ? BU_GD : k % 2 == 0 ? BU_PA : BU_VI, t, ph + k, 0.4);
        }
    }

    // ------------------------------------------------------------------ 38. CANGREJO

    private static final float[] CB_B = {1F, 0.47F, 0.40F}, CB_B2 = {1F, 0.72F, 0.60F}, CB_DK = {0.78F, 0.26F, 0.26F}, CB_SH = {1F, 0.85F, 0.9F};

    static void crab(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.012 * w;
        c.pivot = new V3(0, 0.16, 0);
        // ocho patitas con rodillas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 4; k++) {
                double p = c.gait * 1.4 + ((k + (sd > 0 ? 1 : 0)) % 2) * Math.PI;
                double sw = Math.sin(p) * 0.04 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
                double z = 0.07 - k * 0.055;
                V3 a = new V3(sd * 0.13, 0.15 + bob, z), kn = new V3(sd * 0.27, 0.24 + bob, z * 1.3 + sw * 0.5), fo = new V3(sd * 0.36, 0.02 + lift, z * 1.6 + sw);
                tube(c, new V3[]{a, kn.lerp(a, 0.35), kn, kn.lerp(fo, 0.5), fo}, new double[]{0.017, 0.015, 0.014, 0.012, 0.007}, 5, (n, p2) -> mix(CB_B, CB_DK, ss(0.15, 0.0, p2.y)));
            }
        }
        // caparazón ancho con lunares y un pliegue
        V3 bc = new V3(0, 0.17 + bob, 0), br = new V3(0.21, 0.11, 0.15);
        Paint bp = (n, p) -> mix(CB_B, CB_B2, ss(-0.2, 0.9, n.y) * 0.5);
        ell(c, bc, br, 18, 11, bp);
        ell(c, bc.add(0, -0.045, 0.01), new V3(0.20, 0.05, 0.14), 14, 6, flat(CB_B2));
        for (int k = 0; k < 14; k++) {
            V3 d = Detail.sph(421, k);
            if (d.y < 0.25) {
                d = new V3(d.x, 0.25 + (0.25 - d.y), d.z);
            }
            Detail.spot(c, bc, br, d, 0.028 + 0.02 * Detail.rnd(421, k, 8), k % 2 == 0 ? CB_DK : CB_SH);
        }
        Detail.glint(c, bc, br, new V3(-0.3, 1, 0.3), 0.09, WHITE, 0.75);
        Detail.glint(c, bc, br, new V3(0.5, 0.9, -0.2), 0.06, WHITE, 0.6);
        for (int k = 0; k < 7; k++) {
            double a = -1.1 + k * 0.37;
            V3 e0 = bc.add(Math.sin(a) * br.x * 0.99, 0.0, -0.1 + Math.cos(a) * 0.0);
            ell(c, new V3(Math.sin(a) * 0.205, 0.145 + bob, 0.065 + Math.cos(a) * 0.1 - 0.09), new V3(0.022, 0.016, 0.022), 6, 4, flat(CB_B2));
        }
        // pinzas grandes: se abren y cierran de vez en cuando
        for (int sd = -1; sd <= 1; sd += 2) {
            double snap = Detail.flick(c, sd > 0 ? 53 : 54);
            double open = 0.35 + 0.35 * Math.sin(t * 0.09 + ph + sd) + snap * 0.5;
            V3 sh = new V3(sd * 0.17, 0.17 + bob, 0.10), el = new V3(sd * 0.25, 0.20 + bob, 0.20), wr = new V3(sd * 0.22, 0.20 + bob + 0.01 * Math.sin(t * 0.1 + sd), 0.29);
            tube(c, new V3[]{sh, el, wr}, new double[]{0.026, 0.024, 0.026}, 6, flat(CB_B));
            V3 pc = wr.add(sd * -0.005, 0.0, 0.045);
            ell(c, pc, new V3(0.052, 0.04, 0.062), 12, 8, (n, p) -> mix(CB_B, CB_B2, ss(-0.3, 0.9, n.y) * 0.5));
            for (int h = -1; h <= 1; h += 2) {
                V3 jaw = pc.add(sd * -0.012 * h * 0.0, h * 0.02, 0.065);
                ell(c, jaw.add(0, h * open * 0.02, 0), new V3(0.017, 0.016, 0.055), 0, -h * open * 0.35, 0, 8, 5, flat(h > 0 ? CB_B : CB_B2));
            }
            Detail.glint(c, pc, new V3(0.052, 0.04, 0.062), new V3(-0.3, 1, 0.3), 0.04, WHITE, 0.7);
        }
        // ojos sobre tallos
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.13 + ph + sd * 1.3) * 0.012;
            V3 a = new V3(sd * 0.05, 0.24 + bob, 0.10), b = new V3(sd * (0.07 + sw), 0.34 + bob, 0.13 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.011, 0.010, 0.009}, 5, flat(CB_B2));
            ell(c, b, new V3(0.034, 0.034, 0.034), 10, 7, flat(CB_SH));
            decal(c, b, new V3(0.034, 0.034, 0.034), new V3(sd * 0.1, 0.1, 1), 0.065, 0, c.shut ? T_EYE_SHUT : T_BEAD, new float[]{1, 1, 1}, 1, 0.002);
        }
        decal(c, bc, br, new V3(0, -0.15, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.25F}, 0.9, 0.003);
        cheeks(c, bc, br, 0.88, -0.1, 0.46, 0.07);
        // caracolita rosa en la espalda con su gemita
        {
            V3 sc = new V3(0, 0.30 + bob, -0.06);
            int steps = 18;
            V3[] sp = new V3[steps];
            double[] sr = new double[steps];
            for (int i = 0; i < steps; i++) {
                double f = i / (double) (steps - 1);
                double a = f * Math.PI * 3.2;
                double rr = 0.01 + 0.045 * f;
                sp[i] = new V3((f - 0.5) * 0.03, sc.y + Math.sin(a) * rr, sc.z + Math.cos(a) * rr);
                sr[i] = 0.01 + 0.03 * f;
            }
            curve(c, sp, sr, 1, 7, (nn, p) -> mix(new float[]{1F, 0.7F, 0.85F}, new float[]{1F, 0.95F, 0.8F}, ss(0, 1, nn.y)));
            Detail.twinkle(c, sc.add(0.03, 0.04, 0.0), 0.016, new float[]{1F, 0.9F, 0.95F}, t, ph, 0.12);
        }
        // espumita
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.016 + ph * 0.2 + k * 0.33) % 1.0);
            c.blend = Sink.NORMAL;
            c.alpha = 0.4 * (1 - u * 0.6);
            ell(c, new V3(Math.sin(k * 2.4) * 0.16, 0.3 + u * 0.28, 0.1 + Math.cos(k * 2.4) * 0.1), new V3(0.012, 0.012, 0.012), 8, 5, flat(new float[]{0.9F, 0.97F, 1F}));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
    }

    // ------------------------------------------------------------------ 39. LUCIÉRNAGA (vuela)

    private static final float[] FF_B = {0.30F, 0.24F, 0.38F}, FF_B2 = {0.52F, 0.42F, 0.58F}, FF_GL = {0.80F, 1F, 0.35F}, FF_H = {1F, 0.92F, 0.78F};

    static void firefly(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.17 + ph) * 0.03;
        c.pivot = new V3(0, 0.42, 0);
        double fl = Math.sin(t * 2.0 + ph);
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.09 + ph);
        // alas traslúcidas detrás de los élitros
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 2; k++) {
                double ang = (k == 0 ? 0.55 : 0.25) + fl * 0.35;
                double len = k == 0 ? 0.20 : 0.15;
                V3 ce = new V3(sd * Math.cos(ang) * len * 0.62, 0.52 + bob + Math.sin(ang) * len * 0.62, -0.08 - k * 0.05);
                c.blend = Sink.NORMAL;
                c.alpha = 0.5;
                ell(c, ce, new V3(len * 0.6, len * 0.20, 0.008), 0, 0, -sd * ang, 12, 6, flat(new float[]{0.88F, 0.98F, 1F}));
                c.alpha = 1;
                c.blend = Sink.SOLID;
            }
        }
        // abdomen: la lámpara
        V3 ab = new V3(0, 0.40 + bob, -0.13), abr = new V3(0.115, 0.105, 0.15);
        ell(c, ab, abr, 14, 10, (n, p) -> mix(FF_GL, new float[]{1F, 1F, 0.7F}, ss(0.1, 0.9, -n.z) * 0.6 + 0.15 * pulse));
        c.emissive = true;
        glow(c, ab.add(0, -0.02, -0.04), 0.65 + 0.3 * pulse, FF_GL, 0.42 + 0.30 * pulse);
        glow(c, ab.add(0, -0.02, -0.04), 0.28, WHITE, 0.55 + 0.3 * pulse);
        c.emissive = false;
        // élitros oscuros con el borde claro
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.055, 0.475 + bob, -0.07), new V3(0.062, 0.05, 0.14), sd * 0.08, 0.05, -sd * (0.12 + 0.05 * fl), 12, 8, (n, p) -> mix(FF_B, FF_B2, ss(-0.4, 0.9, n.y) * 0.7));
        }
        Detail.glint(c, new V3(0, 0.50 + bob, -0.07), new V3(0.11, 0.05, 0.14), new V3(0, 1, 0.2), 0.05, WHITE, 0.6);
        // tórax
        V3 th = new V3(0, 0.44 + bob, 0.04), thr = new V3(0.075, 0.065, 0.07);
        ell(c, th, thr, 10, 7, flat(FF_H));
        Detail.fur(c, th, thr, 40, 431, 0.022, 0.011, new V3(0, 0, -1), 0.9, Detail.ALL, flat(FF_H), WHITE);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 a = new V3(sd * 0.04, 0.39 + bob, 0.06 - k * 0.06), b = new V3(sd * 0.085, 0.27 + bob + Math.sin(t * 0.3 + k + sd) * 0.012, 0.09 - k * 0.07);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.008, 0.007, 0.005}, 4, flat(FF_B));
            }
        }
        // cabeza
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.44, 0.1);
        V3 hc = new V3(0, 0.47 + bob, 0.15), hr = new V3(0.105, 0.10, 0.10);
        ell(c, hc, hr, 14, 10, flat(FF_H));
        eyes(c, hc, hr, 0.56, 0.12, 0.80, 0.115, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.92, -0.25, 0.40, 0.07);
        decal(c, hc, hr, new V3(0, -0.40, 1), 0.055, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.3F}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.56, 0.48, 0.70, 0.06, sd * 0.5, FF_B);
            double ef = Detail.flick(c, sd > 0 ? 55 : 56);
            double sw = Math.sin(t * 0.13 + sd) * 0.012 + 0.03 * ef;
            V3 a = new V3(sd * 0.04, 0.55 + bob, 0.16), m = new V3(sd * (0.07 + sw), 0.64 + bob, 0.20), b = new V3(sd * (0.10 + sw * 1.4), 0.70 + bob, 0.27);
            tube(c, new V3[]{a, m, b}, new double[]{0.006, 0.005, 0.004}, 4, flat(FF_B));
            c.emissive = true;
            ell(c, b, new V3(0.012, 0.012, 0.012), 6, 4, flat(FF_GL));
            c.emissive = false;
        }
        c.head = false;
        // chispitas de luz que caen
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.013 + ph * 0.2 + k / 6.0) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.2 + u * 5) * 0.22, 0.34 + bob - u * 0.25, -0.2 + Math.cos(k * 1.9 + u * 4) * 0.12), 0.016 * (1 - u * 0.5), FF_GL, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 40. KOALA

    private static final float[] KO_G = {0.64F, 0.66F, 0.74F}, KO_G2 = {0.90F, 0.90F, 0.95F}, KO_DK = {0.22F, 0.22F, 0.28F}, KO_LF = {0.45F, 0.78F, 0.52F},
            KO_LF2 = {0.72F, 0.92F, 0.7F}, KO_BR = {0.55F, 0.40F, 0.28F};

    static void koala(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.01 * w;
        c.pivot = new V3(0, 0.25, 0);
        // siestas: de vez en cuando cierra los ojos un buen rato y salen "zzz" de luz
        boolean nap = ((t + ph * 50) % 700) > 560;
        if (nap) {
            c.shut = true;
        }
        legs4(c, 0.10, 0.10, -0.12, 0.17 + bob, 0.055, 0.03, 0.06, flat(KO_G), flat(KO_DK));
        V3 bc = new V3(0, 0.27 + bob, -0.02), br = new V3(0.19, 0.20, 0.21);
        Paint bp = (n, p) -> mix(KO_G, KO_G2, ss(0.0, -1.0, n.y) * 0.75);
        ell(c, bc, br, 16, 12, bp);
        Detail.fur(c, bc, br, 150, 441, 0.018, 0.012, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        Detail.soft(c, bc, br, new V3(0, -0.2, 1), 0.16, KO_G2, 0.4 + 0.1 * c.breath);
        // ramita de eucalipto abrazada
        V3 b0 = new V3(0.03, 0.12 + bob, 0.21), b1 = new V3(0.07, 0.30 + bob, 0.25), b2 = new V3(0.12, 0.50 + bob, 0.24);
        tube(c, new V3[]{b0, b1, b2}, new double[]{0.018, 0.016, 0.013}, 6, flat(KO_BR));
        double lf = Math.sin(t * 0.09 + ph) * 0.02;
        for (int k = 0; k < 6; k++) {
            double f = 0.3 + k * 0.13;
            V3 bs = b0.lerp(b1, Math.min(1, f * 1.6)).lerp(b2, Math.max(0, f * 1.6 - 1));
            double sd = k % 2 == 0 ? 1 : -1;
            V3 tp = bs.add(sd * (0.08 + lf * sd), 0.03 + 0.012 * k, 0.04);
            Detail.kite(c, bs, tp, new V3(0, 0, 1), 0.026, new V3(0, 1, 0), KO_LF, KO_LF2);
        }
        // bracitos que abrazan
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.16, 0.33 + bob, 0.05), m = new V3(sd * 0.17, 0.27 + bob, 0.17), b = new V3(sd * 0.05, 0.27 + bob, 0.24);
            tube(c, new V3[]{a, m, b}, new double[]{0.048, 0.042, 0.038}, 7, flat(KO_G));
            ell(c, b, new V3(0.04, 0.034, 0.04), 8, 5, flat(KO_DK));
            Detail.fur(c, m, new V3(0.045, 0.04, 0.06), 6, 442 + sd, 0.012, 0.009, new V3(0, -0.5, -1), 0.6, Detail.ALL, flat(KO_G), KO_G2);
        }
        // cabeza grande
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.45, 0.10);
        V3 hc = new V3(0, 0.55 + bob, 0.12), hr = new V3(0.185, 0.15, 0.16);
        Paint hp = (n, p) -> mix(KO_G, KO_G2, ss(0.2, -0.9, n.y) * 0.35);
        ell(c, hc, hr, 18, 12, hp);
        Detail.fur(c, hc, hr, 60, 443, 0.016, 0.011, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.55 ? 1 : 0, hp, KO_G2);
        for (int sd = -1; sd <= 1; sd += 2) {
            // orejas redondas y esponjosas
            double ef = Detail.flick(c, sd > 0 ? 57 : 58);
            V3 ec = new V3(sd * 0.155, 0.66 + bob, 0.04), er = new V3(0.085, 0.085, 0.045);
            ell(c, ec, er, 0, 0, sd * (0.3 - 0.3 * ef), 12, 8, flat(KO_G));
            ell(c, ec.add(sd * 0.004, 0, 0.026), new V3(0.06, 0.06, 0.02), 0, 0, sd * (0.3 - 0.3 * ef), 10, 6, flat(KO_G2));
            Detail.fur(c, ec, er, 14, 444 + sd, 0.02, 0.012, new V3(sd, 0.3, -0.2), 0.7, d -> d.z < 0.5 ? 1 : 0, flat(KO_G2), null);
            Detail.brow(c, hc, hr, sd, 0.56, 0.42, 0.72, 0.06, sd * 0.4, KO_DK);
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.05, 0.50 + bob + k * 0.012, 0.28), new V3(sd * 0.16, 0.505 + bob + k * 0.03, 0.25), -0.008, 0.0028, new float[]{0.85F, 0.85F, 0.9F});
            }
        }
        // nariz grande y oscura
        V3 nc = new V3(0, 0.52 + bob, 0.265);
        ell(c, nc, new V3(0.048, 0.06, 0.035), 10, 7, flat(KO_DK));
        Detail.glint(c, nc, new V3(0.048, 0.06, 0.035), new V3(-0.2, 0.9, 1), 0.035, WHITE, 0.9);
        eyes(c, hc, hr, 0.50, 0.14, 0.82, 0.075, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.92, -0.2, 0.40, 0.08);
        decal(c, hc, hr, new V3(0, -0.40, 1), 0.06, 0, T_MOUTH, new float[]{0.3F, 0.25F, 0.3F}, 0.9, 0.003);
        c.head = false;
        if (nap) {
            for (int k = 0; k < 3; k++) {
                double u = ((t * 0.02 + k / 3.0) % 1.0);
                Detail.twinkle(c, new V3(0.18 + u * 0.12 + Math.sin(u * 8) * 0.02, 0.72 + u * 0.28, 0.1), 0.02 + 0.03 * u, new float[]{0.8F, 0.9F, 1F}, t, ph + k * 2, 0.2);
            }
        }
    }

    // ------------------------------------------------------------------ 41. ELEFANTE BEBÉ

    private static final float[] EL_G = {0.70F, 0.72F, 0.86F}, EL_G2 = {0.88F, 0.88F, 0.97F}, EL_PK = {1F, 0.72F, 0.80F}, EL_DK = {0.50F, 0.50F, 0.66F},
            EL_TU = {1F, 0.97F, 0.88F}, EL_BL = {0.30F, 0.38F, 0.80F}, EL_GD = {1F, 0.82F, 0.30F};

    static void elephant(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.8)) * 0.012 * w;
        c.pivot = new V3(0, 0.35, 0);
        // cola con borlita
        double tw = Math.sin(t * 0.14 + ph) * 0.03;
        V3[] tcp = {new V3(0, 0.40 + bob, -0.25), new V3(tw, 0.34 + bob, -0.31), new V3(tw * 1.5, 0.26 + bob, -0.33)};
        tube(c, tcp, new double[]{0.014, 0.011, 0.009}, 5, flat(EL_G));
        Detail.fur(c, tcp[2], new V3(0.02, 0.03, 0.02), 14, 451, 0.04, 0.014, new V3(0, -1, 0), 0.6, Detail.ALL, flat(EL_DK), null);
        legs4(c, 0.115, 0.15, -0.16, 0.26 + bob, 0.075, 0.035, 0.07, flat(EL_G), flat(EL_G2));
        V3 bc = new V3(0, 0.37 + bob, -0.02), br = new V3(0.205, 0.21, 0.27);
        Paint bp = (n, p) -> mix(EL_G, EL_G2, ss(0.0, -1.0, n.y) * 0.6);
        ell(c, bc, br, 18, 13, bp);
        // arruguitas suaves
        for (int k = 0; k < 8; k++) {
            Detail.soft(c, bc, br, new V3(Math.cos(k * 0.8 + 1.0), 0.2 - 0.12 * k % 3, Math.sin(k * 0.8 + 1.0)), 0.06, EL_DK, 0.35);
        }
        Detail.glint(c, bc, br, new V3(-0.3, 1, 0.3), 0.1, WHITE, 0.5);
        // mantita real
        {
            ell(c, bc.add(0, 0.15, -0.02), new V3(0.20, 0.07, 0.19), 14, 6, flat(EL_BL));
            ell(c, bc.add(0, 0.08, -0.02), new V3(0.215, 0.012, 0.205), 14, 4, flat(EL_GD));
            ell(c, bc.add(0, 0.215, -0.02), new V3(0.09, 0.012, 0.09), 12, 4, flat(EL_GD));
            Detail.glint(c, bc.add(0, 0.15, -0.02), new V3(0.2, 0.07, 0.19), new V3(0, 1, 0), 0.07, EL_GD, 0.95);
            Detail.twinkle(c, bc.add(0.0, 0.25, -0.02), 0.02, new float[]{1F, 0.9F, 0.5F}, t, ph, 0.1);
        }
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.46, 0.2);
        V3 hc = new V3(0, 0.50 + bob, 0.27), hr = new V3(0.17, 0.165, 0.15);
        Paint hp = (n, p) -> mix(EL_G, EL_G2, ss(0.2, -0.9, n.y) * 0.4);
        ell(c, hc, hr, 18, 12, hp);
        // orejotas que se mueven
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Math.sin(t * 0.08 + ph + sd) * 0.12 + 0.35 * Detail.flick(c, sd > 0 ? 59 : 60);
            V3 ec = new V3(sd * 0.215, 0.55 + bob, 0.20);
            ell(c, ec, new V3(0.018, 0.14, 0.12), sd * (0.35 + fl), 0, 0, 14, 10, flat(EL_G));
            ell(c, ec.add(sd * -0.006, -0.005, 0.0), new V3(0.012, 0.11, 0.09), sd * (0.35 + fl), 0, 0, 12, 8, flat(EL_PK));
            Detail.brow(c, hc, hr, sd, 0.48, 0.38, 0.78, 0.06, sd * 0.4, EL_DK);
            // colmillitos
            cone(c, new V3(sd * 0.055, 0.40 + bob, 0.385), new V3(sd * 0.075, 0.34 + bob, 0.43), 0.014, 0.003, 5, flat(EL_TU));
        }
        // trompa: se mece y a veces se enrosca
        double sw = Math.sin(t * 0.07 + ph) * 0.03;
        double curl = Detail.flick(c, 61);
        V3[] tr = {new V3(0, 0.45 + bob, 0.38), new V3(sw * 0.3, 0.35 + bob, 0.42), new V3(sw, 0.25 + bob + curl * 0.12, 0.45 + curl * 0.04), new V3(sw * 1.4, 0.19 + bob + curl * 0.28, 0.50 + curl * 0.07)};
        curve(c, tr, new double[]{0.06, 0.05, 0.042, 0.032}, 3, 8, (nn, p) -> mix(EL_G, EL_G2, ss(0, -1, nn.y) * 0.3));
        for (int k = 0; k < 6; k++) {
            double f = (k + 1) / 7.0;
            V3 rp = tr[0].lerp(tr[3], f).add(0, 0.02 * Math.sin(k), 0.04);
            Detail.spot(c, rp, new V3(0.001, 0.001, 0.001), new V3(0, 0, 1), 0.001, EL_DK);
        }
        eyes(c, hc, hr, 0.55, 0.14, 0.80, 0.085, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.92, -0.18, 0.40, 0.075);
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.5), 0.07, WHITE, 0.5);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.fur(c, hc.add(sd * 0.0, 0.12, 0.0), new V3(0.03, 0.025, 0.03), 8, 452, 0.026, 0.012, new V3(0, 0.5, -1), 0.5, Detail.ALL, flat(EL_DK), null);
        }
        c.head = false;
        // chorrito de agua de vez en cuando
        double su = ((t + ph * 40) % 260) / 30.0;
        if (su < 1) {
            V3 mouth = new V3(sw * 1.4, 0.19 + bob + curl * 0.28, 0.53 + curl * 0.07);
            for (int k = 0; k < 7; k++) {
                double f = su + k * 0.07;
                if (f > 1) {
                    continue;
                }
                V3 dp = mouth.add(Math.sin(k * 2.0) * 0.02 * f, 0.5 * f * (1 - f * 0.9) + 0.0, 0.5 * f);
                c.blend = Sink.NORMAL;
                c.alpha = 0.55 * (1 - f);
                ell(c, dp, new V3(0.014, 0.014, 0.014), 6, 4, flat(new float[]{0.6F, 0.85F, 1F}));
                c.alpha = 1;
                c.blend = Sink.SOLID;
            }
        }
    }
}
