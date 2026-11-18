package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Compañeros del lote 6 (segunda parte): cachorro de león, salamandra de fuego, pavo real, delfín, pegaso y dragón celestial.
 */
final class Creatures7 {
    private Creatures7() {
    }

    // ------------------------------------------------------------------ 42. CACHORRO DE LEÓN

    private static final float[] LI_B = {1F, 0.76F, 0.32F}, LI_B2 = {1F, 0.92F, 0.66F}, LI_MN = {0.80F, 0.42F, 0.14F}, LI_MN2 = {0.96F, 0.62F, 0.22F},
            LI_DK = {0.25F, 0.14F, 0.10F}, LI_GD = {1F, 0.86F, 0.32F};

    static void lion(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w;
        c.pivot = new V3(0, 0.3, 0);
        // cola con borla
        double sw = Math.sin(t * 0.13 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.33 + bob, -0.22), new V3(sw * 0.4, 0.37 + bob, -0.33), new V3(sw, 0.38 + bob, -0.44), new V3(sw * 1.3, 0.34 + bob, -0.52)};
        curve(c, tcp, new double[]{0.026, 0.020, 0.016, 0.012}, 3, 6, flat(LI_B));
        ell(c, tcp[3].add(0, -0.01, -0.03), new V3(0.04, 0.04, 0.06), 8, 6, flat(LI_MN));
        Detail.fur(c, tcp[3].add(0, -0.01, -0.03), new V3(0.04, 0.04, 0.06), 30, 461, 0.035, 0.016, new V3(0, 0, -1), 0.8, Detail.ALL, flat(LI_MN2), null);
        legs4(c, 0.085, 0.14, -0.14, 0.25 + bob, 0.048, 0.034, 0.07, flat(LI_B), flat(LI_B2));
        V3 bc = new V3(0, 0.32 + bob, -0.02), br = new V3(0.15, 0.14, 0.24);
        Paint bp = (n, p) -> mix(LI_B, LI_B2, ss(0.0, -0.9, n.y) * 0.8);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 190, 462, 0.028, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        Detail.soft(c, bc, br, new V3(0.9, 0.3, 0.1), 0.1, LI_MN, 0.35);
        Detail.soft(c, bc, br, new V3(-0.9, 0.3, -0.2), 0.1, LI_MN, 0.35);
        // cabeza con melena
        double roar = Detail.flick(c, 62);
        headLook(c, 0.10 + 0.12 * roar, -0.18 * roar);
        c.hpv = new V3(0, 0.40, 0.17);
        V3 hc = new V3(0, 0.50 + bob, 0.20), hr = new V3(0.15, 0.14, 0.14);
        V3 mc = new V3(0, 0.49 + bob, 0.16), mr = new V3(0.225, 0.22, 0.17);
        ell(c, mc, mr, 18, 12, (n, p) -> mix(LI_MN, LI_MN2, ss(-0.5, 0.8, n.y) * 0.7));
        Detail.fur(c, mc, mr, 230, 463, 0.05, 0.03, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.6 ? 1 : 0.0, (n, p) -> mix(LI_MN, LI_MN2, ss(-0.2, 0.12, p.y - 0.49)), LI_MN2);
        ell(c, hc, hr, 18, 12, (n, p) -> mix(LI_B, LI_B2, 0.2));
        Detail.fur(c, hc, hr, 70, 464, 0.018, 0.011, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0.0, flat(LI_B), null);
        V3 sc = new V3(0, 0.455 + bob, 0.315), sr = new V3(0.085, 0.062, 0.075);
        ell(c, sc, sr, 12, 8, flat(LI_B2));
        ell(c, new V3(0, 0.485 + bob, 0.375), new V3(0.032, 0.022, 0.022), 8, 5, flat(new float[]{0.95F, 0.55F, 0.6F}));
        decal(c, sc, sr, new V3(0, -0.4, 1), 0.09, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.95, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 63 : 64);
            V3 ec = new V3(sd * 0.115, 0.625 + bob, 0.16);
            ell(c, ec, new V3(0.042, 0.042, 0.022), 0, 0, sd * (0.2 - 0.3 * ef), 10, 7, flat(LI_B));
            ell(c, ec.add(0, 0, 0.012), new V3(0.026, 0.026, 0.012), 8, 5, flat(new float[]{0.95F, 0.6F, 0.55F}));
            Detail.brow(c, hc, hr, sd, 0.52, 0.40, 0.76, 0.06, sd * 0.4, LI_MN);
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.04, 0.455 + bob + k * 0.01, 0.34), new V3(sd * 0.15, 0.46 + bob + k * 0.03, 0.31), -0.008, 0.0025, new float[]{1, 1, 1});
            }
            ell(c, new V3(sd * 0.035, 0.46 + bob, 0.385), new V3(0.006, 0.006, 0.006), 4, 3, flat(LI_DK));
        }
        eyes(c, hc, hr, 0.52, 0.12, 0.80, 0.115, T_EYE, new float[]{1F, 0.75F, 0.2F});
        cheeks(c, hc, hr, 0.86, -0.22, 0.44, 0.075);
        // coronita de oro con gemas
        {
            V3 cb = new V3(0, 0.63 + bob, 0.22);
            for (int k = 0; k < 5; k++) {
                double a = (k - 2) * 0.5;
                V3 b0 = cb.add(Math.sin(a) * 0.06, 0.0, -Math.cos(a) * 0.01);
                cone(c, b0, b0.add(0, 0.045 - Math.abs(k - 2) * 0.008, 0), 0.016, 0.004, 5, flat(LI_GD));
                c.emissive = true;
                ell(c, b0.add(0, 0.05 - Math.abs(k - 2) * 0.008, 0), new V3(0.007, 0.007, 0.007), 5, 4, flat(k % 2 == 0 ? new float[]{1F, 0.3F, 0.4F} : new float[]{0.4F, 0.9F, 1F}));
                c.emissive = false;
            }
            ell(c, cb, new V3(0.07, 0.014, 0.03), 12, 4, flat(LI_GD));
            Detail.twinkle(c, cb.add(0, 0.07, 0), 0.02, new float[]{1F, 0.95F, 0.6F}, t, ph, 0.12);
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 43. SALAMANDRA DE FUEGO

    private static final float[] SA_B = {1F, 0.42F, 0.16F}, SA_B2 = {1F, 0.78F, 0.40F}, SA_SP = {1F, 0.92F, 0.35F}, SA_DK = {0.62F, 0.16F, 0.10F},
            SA_FL = {1F, 0.50F, 0.12F};

    static void salamander(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.01 * w;
        c.pivot = new V3(0, 0.22, 0);
        glow(c, new V3(0, 0.3, -0.15), 1.1, SA_FL, 0.10 + 0.03 * Math.sin(t * 0.2));
        double wag = Math.sin(t * 0.12 + ph) * 0.04 + Math.sin(c.gait) * 0.07 * w;
        // cuerpo y cola en una sola curva ondulante
        int n = 8;
        V3[] cp = new V3[n];
        double[] rd = {0.105, 0.125, 0.125, 0.105, 0.080, 0.058, 0.040, 0.022};
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            cp[i] = new V3(Math.sin(f * 4.0 - t * 0.05 * 0 + wag * 10) * 0.045 * f + wag * f * f * 2.0, 0.19 + bob - 0.03 * f + 0.012 * Math.sin(t * 0.1 + f * 4 + ph), 0.14 - f * 0.65);
        }
        curve(c, cp, rd, 3, 10, (nn, p) -> mix(SA_B, SA_B2, ss(0.0, -0.9, nn.y) * 0.85));
        for (int k = 0; k < 13; k++) {
            double f = 0.04 + k * 0.07;
            int i = Math.min(n - 2, (int) (f * (n - 1)));
            V3 pc = cp[i].lerp(cp[i + 1], f * (n - 1) - i);
            double r = rd[i] + (rd[i + 1] - rd[i]) * (f * (n - 1) - i);
            Detail.spot(c, pc.add(0, r * 0.9, 0), new V3(r, r, r), new V3(0, 1, 0), 0.02 + 0.016 * Detail.rnd(471, k, 8), k % 2 == 0 ? SA_SP : SA_DK);
            ell(c, pc.add((k % 2 == 0 ? 1 : -1) * r * 0.86, r * 0.35, 0), new V3(0.012, 0.012, 0.012), 6, 4, flat(SA_SP));
        }
        // patitas con deditos
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double p = c.gait * 1.3 + (fb + (sd > 0 ? 1 : 0)) * Math.PI;
                double z = fb == 0 ? 0.11 : -0.10, sw = Math.sin(p) * 0.04 * w, lift = Math.max(0, Math.cos(p)) * 0.025 * w;
                V3 a = new V3(sd * 0.09, 0.17 + bob, z), kn = new V3(sd * 0.19, 0.12 + bob, z + sw * 0.3), fo = new V3(sd * 0.19, 0.015 + lift, z + sw + 0.03);
                tube(c, new V3[]{a, kn, fo}, new double[]{0.034, 0.028, 0.024}, 6, flat(SA_B));
                Detail.toes(c, fo, 0.014, 0.03, 0.007, SA_B2);
            }
        }
        // llamas del lomo y de la punta de la cola
        for (int k = 0; k < 5; k++) {
            double f = 0.1 + k * 0.17;
            int i = Math.min(n - 2, (int) (f * (n - 1)));
            V3 pc = cp[i].lerp(cp[i + 1], f * (n - 1) - i);
            Creatures4.flame(c, pc.add(0, rd[i] * 0.95, 0), 0.026 * (1.0 - k * 0.1), SA_FL, t, ph + k * 1.3);
        }
        Creatures4.flame(c, cp[n - 1].add(0, 0.015, 0), 0.05, SA_FL, t, ph + 9);
        // cabeza
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.21, 0.19);
        V3 hc = new V3(0, 0.215 + bob, 0.23), hr = new V3(0.15, 0.115, 0.14);
        ell(c, hc, hr, 18, 12, (nn, p) -> mix(SA_B, SA_B2, ss(0.0, -0.9, nn.y) * 0.5));
        V3 sc = new V3(0, 0.185 + bob, 0.335), sr = new V3(0.09, 0.06, 0.07);
        ell(c, sc, sr, 12, 8, flat(SA_B2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.028, 0.205 + bob, 0.395), new V3(0.01, 0.008, 0.007), 5, 4, flat(SA_DK));
            Detail.brow(c, hc, hr, sd, 0.54, 0.46, 0.76, 0.06, sd * 0.4, SA_DK);
            // branquias en abanico, como de salamandra
            for (int k = 0; k < 3; k++) {
                V3 b0 = new V3(sd * 0.125, 0.25 + bob - k * 0.015, 0.17), tp = b0.add(sd * (0.08 - k * 0.01), 0.04 - k * 0.04 + 0.01 * Math.sin(t * 0.2 + k + sd), -0.03);
                Detail.feather(c, b0, tp, new V3(0, 0, 1), 0.016, new V3(0, 1, 0), SA_FL, SA_SP);
            }
        }
        for (int k = 0; k < 5; k++) {
            Detail.spot(c, hc, hr, new V3(Math.sin(k * 1.9) * 0.6, 0.8, 0.1 - k * 0.1), 0.03, SA_SP);
        }
        Detail.glint(c, hc, hr, new V3(-0.2, 1, 0.4), 0.06, WHITE, 0.75);
        eyes(c, hc, hr, 0.58, 0.14, 0.78, 0.13, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.22, 0.44, 0.075);
        decal(c, sc, sr, new V3(0, -0.4, 1), 0.09, 0, T_MOUTH, new float[]{0.4F, 0.15F, 0.1F}, 0.95, 0.003);
        c.head = false;
        // brasitas que flotan
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.015 + ph * 0.2 + k / 6.0) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 5) * 0.2, 0.25 + u * 0.4, -0.1 + Math.cos(k * 1.7) * 0.2), 0.016 * (1 - u * 0.6), SA_FL, t, ph + k, 0.4);
        }
    }

    // ------------------------------------------------------------------ 44. PAVO REAL

    private static final float[] PA_B = {0.12F, 0.78F, 0.84F}, PA_B2 = {0.45F, 0.95F, 0.85F}, PA_DK = {0.10F, 0.28F, 0.62F}, PA_GR = {0.30F, 0.75F, 0.38F},
            PA_GD = {1F, 0.82F, 0.28F}, PA_WH = {0.97F, 0.95F, 0.90F};

    static void peacock(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w;
        c.pivot = new V3(0, 0.3, 0);
        // abanico: se abre y se cierra un poco, con destellos
        double open = 0.78 + 0.12 * Math.sin(t * 0.06 + ph) + 0.1 * Detail.flick(c, 65);
        int nf = 17;
        V3 root = new V3(0, 0.36 + bob, -0.20);
        for (int k = 0; k < nf; k++) {
            double f = (k / (double) (nf - 1)) * 2 - 1;
            double a = f * 1.25 * open;
            double len = 0.58 + 0.06 * (1 - Math.abs(f)) + 0.03 * (k % 2);
            double sh = Math.sin(t * 0.1 + ph + k * 0.6) * 0.012;
            V3 dirv = new V3(Math.sin(a), Math.cos(a) * 0.95 + 0.12, -0.28 - 0.04 * Math.abs(f)).norm();
            V3 tip = root.add(dirv.mul(len)).add(sh, sh * 0.5, 0);
            V3 mid = root.lerp(tip, 0.62);
            V3 side = new V3(Math.cos(a), -Math.sin(a), 0);
            V3 nrm = new V3(0, 0.2, 1).norm();
            tube(c, new V3[]{root, root.lerp(tip, 0.5), tip}, new double[]{0.004, 0.003, 0.002}, 3, flat(PA_GR));
            Detail.feather(c, root.lerp(tip, 0.18), tip.lerp(mid, 0.0), side, 0.042, nrm, PA_GR, PA_B);
            Detail.feather(c, root.lerp(tip, 0.4), tip, side, 0.05, nrm, PA_B, PA_GR);
            // ojo de la pluma: dorado, turquesa y azul profundo
            V3 ec = tip.lerp(root, 0.07).add(0, 0, 0.003);
            Detail.kite(c, ec.add(0, -0.040, 0), ec.add(0, 0.045, 0), side, 0.035, nrm, PA_GD, PA_GD);
            Detail.kite(c, ec.add(0, -0.030, 0.003), ec.add(0, 0.034, 0.003), side, 0.027, nrm, PA_B2, PA_B);
            Detail.kite(c, ec.add(0, -0.016, 0.006), ec.add(0, 0.020, 0.006), side, 0.016, nrm, PA_DK, PA_DK);
            Detail.twinkle(c, ec.add(0, 0.0, 0.02), 0.02, new float[]{1F, 1F, 0.8F}, t, ph + k * 0.7, 0.09);
        }
        // patas finitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0), sw = Math.sin(p) * 0.04 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
            V3 a = new V3(sd * 0.05, 0.22 + bob, 0.0), fo = new V3(sd * 0.055, 0.01 + lift, sw);
            tube(c, new V3[]{a, a.lerp(fo, 0.5), fo}, new double[]{0.014, 0.011, 0.01}, 5, flat(new float[]{0.65F, 0.55F, 0.45F}));
            Detail.toes(c, fo, 0.02, 0.04, 0.008, new float[]{0.65F, 0.55F, 0.45F});
        }
        // cuerpo con plumas pequeñas
        V3 bc = new V3(0, 0.30 + bob, -0.06), br = new V3(0.115, 0.115, 0.20);
        Paint bp = (n, p) -> mix(PA_B, PA_DK, ss(0.0, -0.9, n.y) * 0.6);
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 7, 12, 0.04, 0.026, Detail.ALL, bp, PA_B2);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 wc = bc.add(sd * 0.105, 0.0, -0.01);
            ell(c, wc, new V3(0.025, 0.09, 0.15), 0, 0, sd * 0.08, 10, 7, (n, p) -> mix(PA_GR, PA_B, ss(0.1, -0.1, p.y - 0.3 - bob)));
        }
        // cuello largo y curvado
        double sway = Math.sin(t * 0.09 + ph) * 0.012;
        V3[] nk = {new V3(0, 0.34 + bob, 0.08), new V3(0, 0.44 + bob, 0.15), new V3(sway, 0.54 + bob, 0.18), new V3(sway * 1.5, 0.62 + bob, 0.17)};
        curve(c, nk, new double[]{0.058, 0.040, 0.034, 0.034}, 3, 8, (n, p) -> mix(PA_DK, PA_B, ss(0.3, 0.55, p.y)));
        Detail.plumage(c, bc.add(0, 0.12, 0.2), new V3(0.05, 0.1, 0.05), 3, 8, 0.03, 0.02, Detail.ALL, flat(PA_B), PA_B2);
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.60, 0.17);
        V3 hc = new V3(sway * 1.5, 0.665 + bob, 0.185), hr = new V3(0.065, 0.06, 0.07);
        ell(c, hc, hr, 14, 10, flat(PA_DK));
        decal(c, hc, hr, new V3(0, 0.05, 1), 0.07, 0, Detail.T_SOFT, PA_WH, 0.95, 0.002);
        cone(c, hc.add(0, -0.01, 0.055), hc.add(0, -0.025, 0.115), 0.022, 0.004, 6, flat(PA_GD));
        eyes(c, hc, hr, 0.60, 0.12, 0.76, 0.07, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.90, -0.30, 0.38, 0.05);
        // penacho: seis tallitos con bolitas azules
        for (int k = 0; k < 6; k++) {
            double a = (k - 2.5) * 0.28, sj = Math.sin(t * 0.14 + ph + k) * 0.01 + 0.015 * Detail.flick(c, 66);
            V3 b0 = hc.add(0, 0.05, -0.01), tp = b0.add(Math.sin(a) * 0.06 + sj, 0.10, -0.015 + Math.cos(a) * 0.0);
            tube(c, new V3[]{b0, b0.lerp(tp, 0.5), tp}, new double[]{0.003, 0.0025, 0.002}, 3, flat(PA_DK));
            c.emissive = true;
            ell(c, tp, new V3(0.014, 0.014, 0.014), 6, 4, flat(k % 2 == 0 ? PA_B2 : PA_B));
            c.emissive = false;
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 45. DELFÍN (vuela)

    private static final float[] DO_B = {0.36F, 0.66F, 0.95F}, DO_B2 = {0.62F, 0.85F, 1F}, DO_BL = {0.96F, 0.98F, 1F}, DO_DK = {0.20F, 0.40F, 0.78F};

    static void dolphin(Ctx c) {
        double t = c.t, ph = c.ph;
        c.pivot = new V3(0, 0.45, 0);
        glow(c, new V3(0, 0.45, 0), 1.6, DO_B2, 0.08);
        double u = t * 0.12 + ph;
        int n = 7;
        V3[] cp = new V3[n];
        double[] rd = {0.092, 0.14, 0.15, 0.125, 0.09, 0.052, 0.03};
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            cp[i] = new V3(Math.sin(u - f * 3.0) * 0.03 * f, 0.46 + Math.sin(u * 1.0 - f * 3.4) * 0.07 * f * f + 0.03 * Math.sin(u * 0.5), 0.26 - f * 0.78);
        }
        curve(c, cp, rd, 3, 11, (nn, p) -> mix(mix(DO_B, DO_DK, ss(-0.2, 1.0, nn.y) * 0.5), DO_BL, ss(-0.15, -0.75, nn.y)));
        // aleta dorsal y de las manos
        V3 dp = cp[2].lerp(cp[3], 0.3);
        for (int k = 0; k < 3; k++) {
            Detail.kite(c, dp.add(0, 0.12 + k * 0.0, 0.05 - k * 0.0), dp.add(0, 0.2 - k * 0.03, -0.12 - k * 0.04), new V3(1, 0, 0), 0.012 + k * 0.004, new V3(1, 0, 0), DO_B, DO_DK);
        }
        ell(c, dp.add(0, 0.17, -0.02), new V3(0.012, 0.075, 0.07), 0, -0.45, 0, 10, 7, flat(DO_DK));
        double fl = Math.sin(u * 1.3);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 fp = cp[1].add(sd * 0.115, -0.10, 0.06);
            ell(c, fp.add(sd * 0.05, -0.04 - 0.02 * fl, -0.03), new V3(0.075, 0.012, 0.06), 0, 0, -sd * (0.55 + 0.2 * fl), 10, 6, flat(DO_DK));
        }
        // cola con aletas horizontales
        V3 tp = cp[n - 1];
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, tp.add(sd * 0.09, 0.012 * sd * Math.sin(u * 2), -0.07), new V3(0.15, 0.014, 0.075), sd * 0.45, 0, -sd * 0.12, 12, 6, flat(DO_DK));
        }
        // cabeza y pico sonriente
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.46, 0.24);
        V3 hc = new V3(0, 0.47 + 0.0, 0.40), hr = new V3(0.115, 0.105, 0.115);
        ell(c, hc, hr, 16, 11, (nn, p) -> mix(mix(DO_B, DO_B2, 0.3), DO_BL, ss(-0.1, -0.7, nn.y)));
        V3 bk = new V3(0, 0.44, 0.52), bkr = new V3(0.05, 0.04, 0.085);
        ell(c, bk, bkr, 12, 8, (nn, p) -> mix(DO_B2, DO_BL, ss(0.0, -0.8, nn.y)));
        decal(c, bk, bkr, new V3(0, -0.25, 1), 0.085, 0, T_MOUTH, new float[]{0.3F, 0.3F, 0.5F}, 0.95, 0.003);
        ell(c, bk.add(0, 0.03, 0.075), new V3(0.012, 0.009, 0.009), 5, 4, flat(DO_DK));
        Detail.glint(c, hc, hr, new V3(-0.3, 1, 0.4), 0.06, WHITE, 0.8);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.60, 0.40, 0.70, 0.06, sd * 0.4, DO_DK);
        }
        eyes(c, hc, hr, 0.66, 0.14, 0.72, 0.115, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.92, -0.22, 0.34, 0.07);
        c.head = false;
        // burbujas y gotitas que lo acompañan
        for (int k = 0; k < 6; k++) {
            double uu = ((t * 0.013 + ph * 0.2 + k / 6.0) % 1.0);
            V3 bp = new V3(Math.sin(k * 2.3 + uu * 6) * 0.14, 0.35 + uu * 0.25, -0.1 - uu * 0.5 + Math.cos(k * 1.7) * 0.1);
            c.blend = Sink.NORMAL;
            c.alpha = 0.45 * (1 - uu * 0.6);
            ell(c, bp, new V3(0.016, 0.016, 0.016), 8, 5, flat(new float[]{0.85F, 0.95F, 1F}));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
    }

    // ------------------------------------------------------------------ 46. PEGASO (vuela)

    private static final float[] PG_B = {0.97F, 0.97F, 1F}, PG_B2 = {0.80F, 0.88F, 1F}, PG_GD = {1F, 0.84F, 0.36F}, PG_MN = {0.70F, 0.85F, 1F},
            PG_MN2 = {1F, 0.80F, 0.95F};

    static void pegasus(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.14 + ph) * 0.03;
        c.pivot = new V3(0, 0.42, 0);
        double fl = Math.sin(t * 0.30 + ph);
        glow(c, new V3(0, 0.5, 0), 2.0, PG_B2, 0.09 + 0.03 * Math.sin(t * 0.1));
        // alas de plumas en tres filas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 root = new V3(sd * 0.09, 0.50 + bob, 0.04);
            double up = 0.45 + fl * 0.55;
            for (int row = 0; row < 3; row++) {
                int cnt = 8 - row;
                for (int k = 0; k < cnt; k++) {
                    double f = k / (double) (cnt - 1);
                    double a = (0.2 + up * 0.7) + f * 0.2 - row * 0.12;
                    double len = (0.30 + 0.28 * Math.sin(f * 2.3 + 0.4) - row * 0.07) * (1.0 - f * 0.1);
                    V3 dirv = new V3(sd * Math.cos(a), Math.sin(a) * (0.55 + up * 0.5) - f * 0.25 * (1.1 - up), -0.12 - f * 0.55).norm();
                    V3 base = root.add(sd * 0.012, row * -0.012, -0.015 * row - f * 0.02);
                    V3 tip = base.add(dirv.mul(len));
                    V3 fs = dirv.cross(new V3(0, 1, 0)).norm();
                    Detail.feather(c, base, tip, fs, 0.075 - row * 0.008, new V3(0, 1, 0), row == 0 ? PG_B : PG_B2, row == 2 ? PG_MN2 : PG_B);
                }
            }
            ell(c, root.add(sd * 0.03, 0.01, -0.01), new V3(0.05, 0.035, 0.07), 10, 7, flat(PG_B));
            Detail.twinkle(c, root.add(sd * 0.40, 0.18 + up * 0.20, -0.30), 0.024, new float[]{1F, 1F, 0.9F}, t, ph + sd * 2, 0.12);
        }
        // cola fluida
        for (int k = -1; k <= 1; k++) {
            V3[] cp = new V3[5];
            double[] rd = {0.040, 0.045, 0.040, 0.030, 0.010};
            for (int i = 0; i < 5; i++) {
                double f = i / 4.0;
                cp[i] = new V3(k * 0.05 * f + Math.sin(t * 0.14 + ph - f * 3 + k) * 0.06 * f, 0.44 + bob - 0.16 * f, -0.22 - 0.28 * f);
            }
            Paint tp = (n, p) -> mix(PG_MN, PG_MN2, ss(-0.25, -0.55, p.z));
            curve(c, cp, rd, 3, 7, tp);
            Detail.furTube(c, cp, rd, 12, 481 + k, 0.045, 0.02, 0.9, tp, WHITE);
        }
        // patas recogidas con pezuñas doradas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.13 : -0.15, swg = Math.sin(t * 0.14 + fb * 1.4 + sd) * 0.012;
                V3 hip = new V3(sd * 0.07, 0.42 + bob, z0), kn = new V3(sd * 0.075, 0.33 + bob, z0 + (fb == 0 ? 0.07 : -0.05)), fo = new V3(sd * 0.075, 0.27 + bob + swg, z0 + (fb == 0 ? 0.01 : -0.02));
                tube(c, new V3[]{hip, kn, fo}, new double[]{0.04, 0.03, 0.026}, 6, flat(PG_B));
                ell(c, fo.add(0, -0.015, 0.008), new V3(0.032, 0.022, 0.036), 8, 5, flat(PG_GD));
            }
        }
        V3 bc = new V3(0, 0.45 + bob, -0.03), br = new V3(0.13, 0.125, 0.23);
        Paint bp = (n, p) -> mix(PG_B, PG_B2, ss(0, -0.9, n.y) * 0.5);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 150, 482, 0.024, 0.013, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, WHITE);
        // melena
        double[][] mp = {{0.78, 0.115}, {0.725, 0.07}, {0.665, 0.03}, {0.625, -0.03}, {0.60, -0.11}, {0.59, -0.19}};
        for (int i = 0; i < mp.length; i++) {
            double x = 0.014 * Math.sin(t * 0.14 + ph - i * 0.7);
            V3 pc = new V3(x, mp[i][0] - 0.1 + bob + 0.012 * Math.sin(t * 0.12 + i), mp[i][1]);
            final double ii = i;
            ell(c, pc, new V3(0.058, 0.07, 0.058), 8, 6, (n, p) -> mix(PG_MN, PG_MN2, ss(0, 1, 0.5 + 0.5 * Math.sin(ii * 1.1))));
            Detail.furTube(c, new V3[]{pc, pc.add(x * 2, -0.04, -0.05)}, new double[]{0.04, 0.02}, 4, 483 + i, 0.04, 0.02, 0.8, flat(PG_MN), WHITE);
        }
        // cuello y cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.56, 0.15);
        ell(c, new V3(0, 0.58 + bob, 0.16), new V3(0.09, 0.115, 0.10), 10, 7, flat(PG_B));
        V3 hc = new V3(0, 0.675 + bob, 0.21), hr = new V3(0.145, 0.13, 0.14);
        ell(c, hc, hr, 16, 11, flat(PG_B));
        Detail.fur(c, hc, hr, 60, 484, 0.02, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0.0, flat(PG_B), WHITE);
        V3 sc = new V3(0, 0.635 + bob, 0.325), sr = new V3(0.08, 0.063, 0.08);
        ell(c, sc, sr, 12, 8, (n, p) -> mix(PG_B, PINK, ss(0.4, 1.0, n.z) * ss(-0.2, 0.2, -n.y) * 0.25));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.03, 0.64 + bob, 0.395), new V3(0.013, 0.01, 0.01), 6, 4, flat(PINK));
            double ef = Detail.flick(c, sd > 0 ? 67 : 68);
            V3 b = new V3(sd * 0.09, 0.775 + bob, 0.17), tp = new V3(sd * 0.115, 0.865 + bob, 0.15).add(sd * 0.04 * ef, -0.03 * ef, 0);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.040, 0.030, 0.004}, 4, flat(PG_B));
            ell(c, b.lerp(tp, 0.35).add(0, 0, 0.012), new V3(0.016, 0.03, 0.01), 0, 0, -sd * 0.2, 6, 4, flat(PINK));
            Detail.brow(c, hc, hr, sd, 0.62, 0.42, 0.74, 0.06, sd * 0.4, PG_B2);
        }
        decal(c, sc, sr, new V3(0, -0.45, 1), 0.085, 0, T_MOUTH, new float[]{1, 1, 1}, 0.95, 0.003);
        eyes(c, hc, hr, 0.62, 0.16, 0.74, 0.125, T_EYE, new float[]{0.45F, 0.65F, 1F});
        cheeks(c, hc, hr, 0.86, -0.22, 0.44, 0.09);
        // estrellita dorada en la frente
        c.emissive = true;
        glowStar(c, new V3(0, 0.76 + bob, 0.30), 0.05 + 0.01 * Math.sin(t * 0.3), PG_GD, t, ph);
        c.emissive = false;
        for (int k = 0; k < 3; k++) {
            double uu = ((t * 0.02 + ph * 0.3 + k / 3.0) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.4 + uu * 6) * 0.1, 0.9 + uu * 0.1, 0.3 - uu * 0.1), 0.02 * (1 - uu * 0.5), PG_GD, t, ph + k, 0.3);
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 47. DRAGÓN CELESTIAL (vuela)

    private static final float[] CD_B = {0.22F, 0.88F, 0.70F}, CD_B2 = {0.55F, 1F, 0.88F}, CD_BL = {1F, 0.96F, 0.80F}, CD_GD = {1F, 0.84F, 0.34F},
            CD_MN = {1F, 0.45F, 0.65F}, CD_DK = {0.10F, 0.48F, 0.50F};

    static void celestial(Ctx c) {
        double t = c.t, ph = c.ph;
        c.pivot = new V3(0, 0.55, 0);
        glow(c, new V3(0, 0.55, -0.2), 2.6, CD_B2, 0.08 + 0.03 * Math.sin(t * 0.07));
        int n = 14;
        V3[] cp = new V3[n];
        double[] rd = new double[n];
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            double amp = 0.06 + 0.16 * f;
            cp[i] = new V3(Math.sin(t * 0.07 + ph - i * 0.65) * amp, 0.56 + Math.sin(t * 0.05 + ph - i * 0.55) * 0.05 * (0.3 + f) - f * 0.06, 0.30 - i * 0.14);
            rd[i] = 0.115 * (1 - f * 0.88) + (i < 3 ? 0.012 * i : 0.0) * (1 - f) + 0.01;
        }
        Paint skin = (nn, p) -> mix(mix(CD_B, CD_DK, ss(-0.1, 1.0, nn.y) * 0.5), CD_BL, ss(-0.4, -0.95, nn.y));
        curve(c, cp, rd, 3, 10, skin);
        Detail.scales(c, cp[3], new V3(0.13, 0.13, 0.15), 5, 12, 0.04, d -> d.y > -0.2 ? 1 : 0.0, skin, scale(CD_B2, 1.1));
        Detail.furTube(c, cp, rd, 60, 491, 0.035, 0.026, 0.9, skin, CD_B2);
        // melena rosada y espinas doradas a lo largo del lomo
        for (int i = 0; i < n - 1; i++) {
            double r = rd[i];
            V3 b0 = cp[i].add(0, r * 0.95, 0), tp = b0.add(0, 0.05 + 0.01 * Math.sin(t * 0.2 + i) - i * 0.002, -0.06);
            cone(c, b0, tp, 0.018 * (1 - i * 0.04), 0.002, 5, flat(i % 2 == 0 ? CD_GD : CD_MN));
            Detail.furTube(c, new V3[]{b0, tp}, new double[]{0.02, 0.008}, 2, 492 + i, 0.03, 0.02, 0.5, flat(CD_MN), WHITE);
        }
        // cuatro patitas con garras
        int[] li = {3, 3, 8, 8};
        for (int k = 0; k < 4; k++) {
            int sd = k % 2 == 0 ? 1 : -1;
            V3 a = cp[li[k]].add(sd * rd[li[k]] * 0.8, -rd[li[k]] * 0.5, 0);
            double sw = Math.sin(t * 0.1 + ph + k * 1.6) * 0.012;
            V3 fo = a.add(sd * 0.075, -0.09 + sw, 0.03);
            tube(c, new V3[]{a, a.lerp(fo, 0.5).add(sd * 0.03, 0.0, 0.0), fo}, new double[]{0.026, 0.02, 0.016}, 5, skin);
            for (int q = -1; q <= 1; q++) {
                cone(c, fo.add(q * 0.012, 0, 0), fo.add(q * 0.016, -0.03, 0.02), 0.007, 0.002, 4, flat(CD_GD));
            }
        }
        // cabeza: hocico largo y cuernos de ciervo
        headLook(c, 0.10, 0.02);
        c.hpv = new V3(0, 0.56, 0.28);
        V3 hc = new V3(cp[0].x, cp[0].y + 0.02, 0.36), hr = new V3(0.115, 0.105, 0.115);
        ell(c, hc, hr, 18, 12, skin);
        Detail.scales(c, hc, hr, 5, 11, 0.026, d -> d.y > -0.1 && d.z < 0.5 ? 1 : 0.0, skin, scale(CD_B2, 1.2));
        V3 sc = hc.add(0, -0.03, 0.115), sr = new V3(0.075, 0.055, 0.095);
        ell(c, sc, sr, 12, 8, (nn, p) -> mix(CD_B2, CD_BL, ss(0.0, -0.8, nn.y)));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, sc.add(sd * 0.03, 0.018, 0.088), new V3(0.011, 0.008, 0.008), 5, 4, flat(CD_DK));
            // cuernos ramificados
            double sj = Math.sin(t * 0.1 + ph + sd) * 0.008;
            V3 h0 = hc.add(sd * 0.05, 0.09, -0.03), h1 = h0.add(sd * 0.03, 0.08, -0.07), h2 = h1.add(sd * 0.02, 0.07 + sj, -0.06);
            tube(c, new V3[]{h0, h1, h2}, new double[]{0.016, 0.012, 0.004}, 5, flat(CD_GD));
            tube(c, new V3[]{h1, h1.add(sd * 0.04, 0.03, -0.01)}, new double[]{0.008, 0.003}, 4, flat(CD_GD));
            // bigotes largos y flotantes
            V3 w0 = sc.add(sd * 0.05, -0.01, 0.0);
            V3[] wp = new V3[5];
            double[] wr = new double[5];
            for (int q = 0; q < 5; q++) {
                double f = q / 4.0;
                wp[q] = w0.add(sd * (0.05 + 0.2 * f), -0.02 - 0.05 * f + Math.sin(t * 0.12 + ph - f * 3 + sd) * 0.05 * f, -0.06 - 0.28 * f);
                wr[q] = 0.007 * (1 - f * 0.8) + 0.001;
            }
            curve(c, wp, wr, 3, 4, flat(CD_GD));
            Detail.brow(c, hc, hr, sd, 0.52, 0.45, 0.78, 0.07, sd * 0.4, CD_DK);
        }
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.4), 0.06, WHITE, 0.7);
        eyes(c, hc, hr, 0.52, 0.16, 0.80, 0.105, T_EYE, new float[]{1F, 0.8F, 0.25F});
        cheeks(c, hc, hr, 0.88, -0.2, 0.44, 0.07);
        decal(c, sc, sr, new V3(0, -0.4, 1), 0.10, 0, T_MOUTH, new float[]{0.40F, 0.25F, 0.28F}, 0.9, 0.003);
        c.head = false;
        // perla luminosa que flota ante él
        V3 pp = new V3(cp[0].x, 0.78 + Math.sin(t * 0.1 + ph) * 0.03, 0.62);
        c.emissive = true;
        ell(c, pp, new V3(0.035, 0.035, 0.035), 10, 7, flat(new float[]{1F, 1F, 0.92F}));
        glow(c, pp, 0.35, new float[]{0.7F, 1F, 0.95F}, 0.6);
        glowStar(c, pp, 0.07, CD_GD, t, ph);
        c.emissive = false;
        // nubecitas
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.01 + ph * 0.2 + k * 0.25) % 1.0);
            c.blend = Sink.NORMAL;
            c.alpha = 0.55 * Math.sin(u * Math.PI);
            ell(c, new V3(Math.sin(k * 2.1) * 0.2, 0.30 - u * 0.08, 0.1 - k * 0.2), new V3(0.09, 0.035, 0.07), 10, 6, flat(new float[]{0.95F, 1F, 1F}));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
    }

    // ------------------------------------------------------------------ 48. COOKIE (hurón)

    // Cookie es gris plateado, como el hurón de Axelis: lomo gris, vientre claro, cara casi blanca con antifaz carbón
    private static final float[] FE_B = {0.52F, 0.54F, 0.59F}, FE_B2 = {0.80F, 0.82F, 0.86F}, FE_DK = {0.24F, 0.25F, 0.29F}, FE_MK = {0.14F, 0.15F, 0.18F},
            FE_PK = {1F, 0.62F, 0.66F}, FE_FACE = {0.86F, 0.86F, 0.88F};

    static void ferret(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        // baile de guerra: de vez en cuando salta con el lomo arqueado
        double dance = Detail.flick(c, 69);
        double hop = Math.abs(Math.sin(t * 0.5)) * 0.07 * dance;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w + hop;
        c.pivot = new V3(0, 0.2, 0);
        double arch = 0.025 + 0.02 * Math.sin(c.gait * 2) * w + 0.04 * dance;
        double wig = Math.sin(t * 0.12 + ph) * 0.02 + Math.sin(c.gait) * 0.03 * w;
        int n = 6;
        V3[] cp = new V3[n];
        double[] rd = {0.070, 0.074, 0.070, 0.066, 0.060, 0.054};
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            cp[i] = new V3(wig * f * f * 2.0, 0.165 + bob + arch * Math.sin(f * Math.PI), 0.20 - f * 0.46);
        }
        Paint skin = (nn, p) -> mix(FE_B, FE_B2, ss(0.0, -0.9, nn.y) * 0.8);
        curve(c, cp, rd, 3, 10, skin);
        Detail.furTube(c, cp, rd, 150, 501, 0.024, 0.014, 0.9, skin, null);
        // pepitas de chocolate
        for (int k = 0; k < 9; k++) {
            double f = 0.12 + k * 0.1;
            int i = Math.min(n - 2, (int) (f * (n - 1)));
            V3 pc = cp[i].lerp(cp[i + 1], f * (n - 1) - i);
            double r = rd[i];
            Detail.spot(c, pc.add(0, r * 0.9, 0), new V3(r, r, r), new V3((k % 3 - 1) * 0.5, 1, 0), 0.026 + 0.012 * Detail.rnd(501, k, 8), FE_DK);
        }
        // cola fina y peluda
        V3[] tcp = {cp[n - 1], cp[n - 1].add(wig, -0.02, -0.12), cp[n - 1].add(wig * 2, -0.04 + 0.03 * Math.sin(t * 0.15 + ph), -0.24)};
        curve(c, tcp, new double[]{0.05, 0.04, 0.02}, 3, 7, (nn, p) -> mix(FE_B, FE_DK, ss(-0.28, -0.46, p.z)));
        Detail.furTube(c, tcp, new double[]{0.05, 0.04, 0.02}, 28, 502, 0.03, 0.016, 0.9, (nn, p) -> mix(FE_B, FE_DK, ss(-0.28, -0.46, p.z)), null);
        // patitas cortas con deditos
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double p = c.gait * 1.3 + (fb + (sd > 0 ? 1 : 0)) * Math.PI;
                double z = fb == 0 ? 0.15 : -0.17, sw = Math.sin(p) * 0.05 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
                V3 a = new V3(sd * 0.05, 0.13 + bob, z), fo = new V3(sd * 0.06, 0.012 + lift + hop * 0.6, z + sw);
                tube(c, new V3[]{a, a.lerp(fo, 0.5), fo}, new double[]{0.032, 0.026, 0.022}, 6, (nn, p2) -> mix(FE_B, FE_DK, ss(0.1, 0.03, p2.y)));
                Detail.toes(c, fo, 0.012, 0.026, 0.007, FE_DK);
            }
        }
        // cabeza: antifaz de bandido, orejas redondas y bigotes
        double nod = Detail.flick(c, 70);
        headLook(c, 0.14 + 0.1 * nod, 0.0);
        c.hpv = new V3(0, 0.22, 0.2);
        V3 hc = new V3(0, 0.215 + bob + arch * 0.4, 0.31), hr = new V3(0.085, 0.075, 0.095);
        ell(c, hc, hr, 16, 11, (nn, p) -> mix(mix(FE_B, FE_FACE, 0.45), FE_FACE, ss(-0.1, 0.6, nn.z) * 0.8));
        Detail.fur(c, hc, hr, 60, 503, 0.016, 0.011, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0.0, flat(mix(FE_B, FE_FACE, 0.45)), null);
        // antifaz marrón alrededor de los ojos
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.soft(c, hc, hr, new V3(sd * 0.5, 0.15, 0.85), 0.2, FE_MK, 1.0);
            Detail.soft(c, hc, hr, new V3(0, 0.55, 0.8), 0.14, FE_MK, 0.8);
        }
        V3 sc = new V3(0, 0.20 + bob + arch * 0.4, 0.385), sr = new V3(0.05, 0.04, 0.06);
        ell(c, sc, sr, 12, 8, flat(FE_FACE));
        ell(c, sc.add(0, 0.018, 0.055), new V3(0.016, 0.012, 0.011), 6, 4, flat(FE_PK));
        decal(c, sc, sr, new V3(0, -0.45, 1), 0.07, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.95, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 71 : 72);
            V3 ec = new V3(sd * 0.07, 0.285 + bob + arch * 0.4, 0.27);
            ell(c, ec, new V3(0.026, 0.028, 0.018), 0, 0, sd * (0.25 - 0.3 * ef), 10, 7, flat(FE_B));
            ell(c, ec.add(0, 0, 0.011), new V3(0.02, 0.022, 0.01), 8, 5, flat(FE_PK));
            Detail.brow(c, hc, hr, sd, 0.50, 0.40, 0.76, 0.05, sd * 0.4, FE_DK);
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.03, 0.20 + bob + arch * 0.4 + k * 0.01, 0.42), new V3(sd * 0.12, 0.205 + bob + arch * 0.4 + k * 0.03, 0.39), -0.008, 0.0022, new float[]{1, 1, 1});
            }
        }
        Detail.glint(c, hc, hr, new V3(-0.3, 1, 0.4), 0.05, WHITE, 0.7);
        eyes(c, hc, hr, 0.50, 0.12, 0.82, 0.075, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.28, 0.44, 0.065);
        c.head = false;
        // migajitas de galleta que brillan
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.2 + u * 5) * 0.18, 0.4 + u * 0.3 + bob, 0.05 + Math.cos(k * 1.9) * 0.14), 0.014 * (1 - u * 0.5), new float[]{0.88F, 0.92F, 1F}, t, ph + k, 0.3);
        }
    }
}
