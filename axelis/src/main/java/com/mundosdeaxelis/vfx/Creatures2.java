package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/** Segunda mitad de las criaturas 3D: fénix bebé, tortuguita, pingüino, unicornio, panda y hada. */
final class Creatures2 {
    private Creatures2() {
    }

    // ------------------------------------------------------------------ 6. FÉNIX BEBÉ

    private static final float[] P_RED = {0.95F, 0.25F, 0.12F}, P_OR = {1F, 0.55F, 0.12F}, P_YE = {1F, 0.86F, 0.30F}, P_CR = {1F, 0.95F, 0.70F};

    static void phoenix(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.15 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double fl = Math.sin(t * (0.42 + c.speed * 0.15) + ph);
        double flap = 0.3 + 0.7 * (0.5 + 0.5 * fl);
        glow(c, new V3(0, 0.42 + bob, 0), 1.1, new float[]{1F, 0.45F, 0.12F}, 0.20 + 0.05 * Math.sin(t * 0.3));
        // cola de plumas largas con llamas
        for (int k = -1; k <= 1; k++) {
            V3[] cp = new V3[5];
            double[] rd = {0.032, 0.036, 0.034, 0.026, 0.004};
            for (int i = 0; i < 5; i++) {
                double f = i / 4.0;
                cp[i] = new V3(k * 0.07 * f + Math.sin(t * 0.2 + ph - f * 3 + k) * 0.05 * f, 0.30 + bob - 0.05 * f + 0.04 * Math.sin(t * 0.18 + k - f * 2) * f, -0.12 - 0.50 * f);
            }
            int kk = k;
            curve(c, cp, rd, 3, 6, (n, p) -> mix(P_RED, P_YE, ss(-0.2, -0.6, p.z)));
        }
        // alas: abanico de plumas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.15, 0.46 + bob, -0.01);
            for (int k = 0; k < 6; k++) {
                double a = 0.55 - k * 0.20 - (flap - 0.5) * 1.5;
                double len = 0.26 + 0.05 * Math.sin(k * 0.9);
                V3 e = sh.add(sd * Math.cos(a) * len * 1.1, Math.sin(a) * len + 0.05, -0.07 - k * 0.032);
                float[] col = mix(P_RED, P_YE, k / 5.0);
                tube(c, new V3[]{sh, sh.lerp(e, 0.5).add(0, 0.015, 0), e}, new double[]{0.038, 0.032, 0.006}, 5, (n, p) -> mix(col, P_YE, ss(0.35, 1.0, p.sub(sh).len() / 0.28) * 0.7));
            }
        }
        // cuerpo
        V3 bc = new V3(0, 0.40 + bob, 0), br = new V3(0.17, 0.17, 0.19);
        Paint bp = (n, p) -> mix(mix(P_RED, P_OR, ss(-0.5, 0.8, n.y) * 0.9), P_YE, ss(0.0, 0.8, n.z) * ss(0.1, -0.7, n.y));
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 10, 13, 0.06, 0.02, Detail.ALL, bp, P_YE);
        // plumas de las alas: segunda capa corta
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh2 = new V3(sd * 0.15, 0.46 + bob, -0.01);
            for (int k = 0; k < 5; k++) {
                double a = 0.35 - k * 0.2 - (flap - 0.5) * 1.5;
                double len = 0.15;
                V3 e = sh2.add(sd * Math.cos(a) * len * 1.1, Math.sin(a) * len - 0.01, -0.08 - k * 0.03);
                Detail.feather(c, sh2.add(0, -0.015, -0.01), e, new V3(0, 0, 1), 0.014, new V3(0, 1, 0), P_RED, P_OR);
            }
        }
        // brasas que suben de su cuerpo
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.016 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.7 + u * 5) * 0.14, 0.30 + bob + u * 0.5, Math.cos(k * 1.9) * 0.12 - 0.05), 0.02 * (1 - u * 0.6), new float[]{1F, 0.6F, 0.2F}, t, ph + k, 0.4);
        }
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.06, 0.26 + bob, 0.04);
            tube(c, new V3[]{b, b.add(0, -0.05, 0.015)}, new double[]{0.014, 0.010}, 4, flat(P_YE));
            for (int j = -1; j <= 1; j++) {
                V3 q = b.add(j * 0.016, -0.05, 0.015);
                tube(c, new V3[]{q, q.add(j * 0.008, -0.008, 0.035)}, new double[]{0.008, 0.003}, 3, flat(P_YE));
            }
        }
        // cabeza
        headLook(c, 0.14, 0);
        c.hpv = new V3(0, 0.50, 0.08);
        V3 hc = new V3(0, 0.595 + bob, 0.115), hr = new V3(0.15, 0.14, 0.14);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(P_OR, P_YE, ss(-0.3, 0.9, n.y) * 0.5));
        // pico
        cone(c, new V3(0, 0.578 + bob, 0.225), new V3(0, 0.575 + bob, 0.30), 0.030, 0.006, 6, flat(P_YE));
        cone(c, new V3(0, 0.562 + bob, 0.225), new V3(0, 0.538 + bob, 0.29), 0.028, 0.004, 6, flat(mix(P_YE, P_OR, 0.4)));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.01, 0.585 + bob, 0.285), new V3(0.004, 0.004, 0.004), 4, 3, flat(P_RED));
            Detail.brow(c, hc, hr, sd, 0.66, 0.45, 0.72, 0.06, sd * 0.5, P_RED);
        }
        Detail.fur(c, hc, hr, 40, 351, 0.03, 0.014, new V3(0, -0.2, -1), 0.8, d -> d.z < 0.4 ? 1 : 0, (n, p) -> mix(P_OR, P_YE, ss(-0.3, 0.9, n.y) * 0.5), P_YE);
        Detail.glint(c, new V3(0, 0.578 + bob, 0.25), new V3(0.03, 0.02, 0.05), new V3(0.2, 1, 0.5), 0.02, WHITE, 0.9);
        eyes(c, hc, hr, 0.66, 0.16, 0.74, 0.115, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.86, -0.24, 0.46, 0.09);
        // cresta de llamas
        for (int k = -1; k <= 1; k++) {
            double sway = Math.sin(t * 0.25 + ph + k * 1.2) * 0.02;
            V3 b = new V3(k * 0.045, 0.715 + bob, 0.08 - Math.abs(k) * 0.03);
            V3 tp = new V3(k * 0.08 + sway, 0.84 + bob + (k == 0 ? 0.04 : 0), 0.03 - Math.abs(k) * 0.03 - 0.02);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.032, 0.024, 0.003}, 5, (n, p) -> mix(P_OR, P_YE, ss(0.72, 0.85, p.y - bob)));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 7. TORTUGUITA DE CRISTAL

    private static final float[] T_GR = {0.55F, 0.82F, 0.45F}, T_SH = {0.30F, 0.74F, 0.72F}, T_SH2 = {0.55F, 0.92F, 0.90F};

    static void turtle(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.7)) * 0.008 * w;
        c.pivot = new V3(0, 0.2, 0);
        // patas cortas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.15 : -0.17;
                double phz = c.gait * 0.7 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phz) * 0.05 * w, lift = Math.max(0, Math.cos(phz)) * 0.025 * w;
                ell(c, new V3(sd * 0.17, 0.07 + lift, z0 + sw), new V3(0.062, 0.07, 0.075), 10, 7, (n, p) -> T_GR);
            }
        }
        // cola que se mueve despacito
        double twg = Math.sin(t * 0.14 + ph) * 0.02;
        cone(c, new V3(0, 0.12, -0.27), new V3(twg, 0.10, -0.36), 0.04, 0.004, 5, flat(T_GR));
        // uñitas y escamitas en las patas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.15 : -0.17;
                double phz = c.gait * 0.7 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phz) * 0.05 * w, lift = Math.max(0, Math.cos(phz)) * 0.025 * w;
                V3 lc = new V3(sd * 0.17, 0.07 + lift, z0 + sw), lr = new V3(0.062, 0.07, 0.075);
                for (int k = -1; k <= 1; k++) {
                    ell(c, lc.add(sd * 0.012 + k * 0.025, -0.05, 0.065), new V3(0.011, 0.008, 0.016), 5, 4, flat(T_SH2));
                }
                Detail.scales(c, lc, lr, 4, 7, 0.026, d -> d.y > -0.5 ? 1 : 0, (n, p) -> mix(T_GR, T_SH2, 0.35), new float[]{0.4F, 0.7F, 0.38F});
            }
        }
        // vientre y caparazón
        ell(c, new V3(0, 0.13 + bob, -0.01), new V3(0.20, 0.07, 0.26), 14, 7, (n, p) -> mix(T_GR, new float[]{1F, 0.95F, 0.75F}, 0.7));
        ell(c, new V3(0, 0.25 + bob, -0.02), new V3(0.245, 0.17, 0.285), 0, 0, 0, 20, 12, (n, p) -> {
            double line = Math.abs(Math.sin(p.x * 26 + 0.6)) * Math.abs(Math.sin(p.z * 22));
            float[] b = mix(T_SH, T_SH2, ss(-0.2, 0.9, n.y) * 0.8);
            return scale(mix(b, new float[]{0.18F, 0.55F, 0.62F}, ss(0.12, 0.0, line) * 0.7), 1.0);
        });
        // aro del borde
        for (int k = 0; k < 18; k++) {
            double a = Math.PI * 2 * k / 18.0;
            ell(c, new V3(Math.cos(a) * 0.225, 0.155 + bob, -0.02 + Math.sin(a) * 0.262), new V3(0.032, 0.026, 0.032), 6, 4, (n, p) -> new float[]{0.95F, 0.90F, 0.65F});
        }
        // brotecito de planta y brillos en el caparazón
        {
            V3 sp = new V3(-0.12, 0.40 + bob, -0.12);
            double sw2 = Math.sin(t * 0.06 + ph) * 0.02;
            tube(c, new V3[]{sp, sp.add(sw2 * 0.5, 0.04, 0), sp.add(sw2, 0.07, 0)}, new double[]{0.006, 0.006, 0.005}, 4, flat(new float[]{0.35F, 0.75F, 0.35F}));
            for (int sd = -1; sd <= 1; sd += 2) {
                ell(c, sp.add(sw2 + sd * 0.025, 0.075, 0), new V3(0.03, 0.006, 0.016), 0, 0, sd * (0.5 + sw2 * 6), 8, 4, flat(new float[]{0.45F, 0.88F, 0.42F}));
            }
            Detail.glint(c, new V3(0, 0.25 + bob, -0.02), new V3(0.245, 0.17, 0.285), new V3(-0.4, 1, 0.3), 0.07, WHITE, 0.7);
        }
        // cristales sobre el caparazón (brillan)
        double[][] cr = {{0, -0.02, 0.17}, {-0.10, 0.05, 0.11}, {0.11, -0.07, 0.12}, {-0.07, -0.13, 0.09}, {0.07, 0.09, 0.08}};
        float[][] cc = {{1F, 0.65F, 0.90F}, {0.60F, 0.85F, 1F}, {0.80F, 0.70F, 1F}, {0.70F, 1F, 0.85F}, {1F, 0.85F, 0.60F}};
        for (int i = 0; i < cr.length; i++) {
            double tw = 0.92 + 0.08 * Math.sin(t * 0.2 + i * 1.7 + ph);
            double ex = cr[0 + 0][0] * 0 + cr[i][0], ez = cr[i][1];
            double ys = 0.25 + 0.17 * Math.sqrt(Math.max(0.05, 1 - (ex / 0.245) * (ex / 0.245) - ((ez) / 0.285) * ((ez) / 0.285)));
            V3 b = new V3(ex, ys - 0.03 + bob, ez - 0.02);
            V3 tp = b.add(0.012 * (i - 2), cr[i][2] * tw, 0.008 * (i - 2));
            float[] col = cc[i];
            c.blend = Sink.NORMAL;
            c.alpha = 0.93;
            double rb = 0.045 * cr[i][2] / 0.1;
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{rb, rb * 0.9, 0.002}, 6, (n, p) -> mix(col, WHITE, ss(0.0, 0.8, n.y) * 0.5 + 0.1));
            c.alpha = 1;
            c.blend = Sink.SOLID;
            glow(c, tp, 0.12, col, 0.35 + 0.15 * Math.sin(t * 0.25 + i));
        }
        // cabeza que asoma y mira
        double look = Math.sin(t * 0.05 + ph) * 0.12;
        c.pivot = new V3(0, 0.2, 0);
        headLook(c, 0.18, 0.05);
        c.hpv = new V3(0, 0.22, 0.28);
        V3 hc = new V3(0, 0.25 + bob, 0.385), hr = new V3(0.105, 0.095, 0.105);
        ell(c, new V3(0, 0.22 + bob, 0.30), new V3(0.06, 0.06, 0.09), 8, 6, (n, p) -> T_GR);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(T_GR, new float[]{0.72F, 0.92F, 0.55F}, ss(-0.2, 0.9, n.y) * 0.5));
        Detail.scales(c, hc, hr, 5, 9, 0.028, d -> d.y > 0.0 && d.z < 0.5 ? 1 : 0, (n, p) -> mix(T_GR, T_SH2, 0.4), new float[]{0.4F, 0.7F, 0.38F});
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.66, 0.46, 0.70, 0.05, sd * 0.4, T_SH);
            ell(c, new V3(sd * 0.018, 0.255 + bob, 0.485), new V3(0.006, 0.006, 0.004), 4, 3, flat(new float[]{0.2F, 0.4F, 0.25F}));
        }
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.5), 0.04, WHITE, 0.7);
        eyes(c, hc, hr, 0.66, 0.20, 0.72, 0.095, T_BEAD, new float[]{1, 1, 1});
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.07, 0, T_MOUTH, new float[]{1, 1, 1}, 1, 0.002);
        cheeks(c, hc, hr, 0.86, -0.22, 0.46, 0.075);
        c.head = false;
    }

    // ------------------------------------------------------------------ 8. PINGÜINO POLAR

    private static final float[] G_FEATHER = {0.35F, 0.40F, 0.58F}, G_OR2 = {0.92F, 0.45F, 0.12F}, G_GOLD = {1F, 0.78F, 0.18F}, G_GOLD2 = {1F, 0.94F, 0.55F}, G_BROW = {1F, 0.82F, 0.3F}, G_CREAM = {1F, 0.90F, 0.82F};
    private static final float[] G_DK = {0.17F, 0.20F, 0.33F}, G_WH = {0.97F, 0.98F, 1F}, G_OR = {1F, 0.62F, 0.18F}, G_RED = {0.90F, 0.22F, 0.30F};

    static void penguin(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double wob = Math.sin(c.gait * 0.8) * 0.14 * w;
        double hop = Math.abs(Math.sin(c.gait * 0.8)) * 0.02 * w;
        c.pivot = new V3(0, 0.05, 0);
        // roll de contoneo: se combina con el roll del cuerpo
        double[] bm = rot(c.yaw, c.pitch, c.roll + wob);
        System.arraycopy(bm, 0, c.m, 0, 9);
        // pies
        for (int sd = -1; sd <= 1; sd += 2) {
            double phz = c.gait * 0.8 + (sd > 0 ? 0 : Math.PI);
            double fy = 0.026 + Math.max(0, Math.cos(phz)) * 0.03 * w, fz = 0.075 + Math.sin(phz) * 0.04 * w;
            ell(c, new V3(sd * 0.085, fy, fz), new V3(0.07, 0.026, 0.095), 10, 6, flat(G_OR));
            // dedos palmeados: tres lóbulos con membrana
            for (int k = -1; k <= 1; k++) {
                ell(c, new V3(sd * 0.085 + k * 0.034, fy - 0.004, fz + 0.075 - Math.abs(k) * 0.012), new V3(0.019, 0.014, 0.034), 0, k * 0.4, 0, 6, 4, flat(G_OR2));
            }
        }
        // cuerpo
        V3 bc = new V3(0, 0.33 + hop, 0), br = new V3(0.20, 0.285, 0.175);
        Paint bp = (n, p) -> mix(G_DK, G_WH, ss(0.15, 0.55, n.z) * ss(0.95, 0.3, Math.abs(n.x) * 1.3));
        ell(c, bc, br, 16, 12, bp);
        // plumaje en filas, peinado hacia abajo
        Detail.plumage(c, bc, br, 12, 15, 0.055, 0.017, d -> 1, bp, G_FEATHER);
        Detail.fur(c, bc, br, 60, 81, 0.03, 0.012, new V3(0, -1, -0.3), 0.8, Detail.FRONT, bp, null);
        // aletas
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = (Math.sin(c.gait * 0.8 + sd) * 0.25 * w + 0.28 + Math.sin(t * 0.1 + ph) * 0.05);
            V3 fc2 = new V3(sd * 0.215, 0.34 + hop, 0.0);
            ell(c, fc2, new V3(0.04, 0.15, 0.075), 0, 0, -sd * fl, 8, 6, flat(G_DK));
            for (int k = -2; k <= 2; k++) {
                V3 base = fc2.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.02, -0.07, k * 0.028)));
                V3 tip = fc2.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.035, -0.19 + Math.abs(k) * 0.015, k * 0.034)));
                Detail.feather(c, base, tip, new V3(0, 0, 1), 0.016, new V3(sd, 0, 0), G_DK, G_FEATHER);
            }
        }
        // cola pequeña
        cone(c, new V3(0, 0.12 + hop, -0.14), new V3(0, 0.08 + hop, -0.22), 0.05, 0.004, 5, flat(G_DK));
        // cabeza
        headLook(c, 0.16, 0);
        c.hpv = new V3(0, 0.58, 0.0);
        V3 hc = new V3(0, 0.67 + hop, 0.02), hr = new V3(0.175, 0.15, 0.155);
        ell(c, hc, hr, 16, 11, (n, p) -> G_DK);
        Detail.fur(c, hc, hr, 70, 82, 0.025, 0.011, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, (n, p) -> G_DK, G_FEATHER);
        // plumas amarillas sobre los ojos (como los pingüinos de penacho)
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 4; k++) {
                V3 d0 = new V3(sd * (0.38 + k * 0.05), 0.42 + k * 0.02, 0.82 - k * 0.12);
                V3 bs = surf(hc, hr, d0);
                V3 tp = bs.add(sd * (0.05 + k * 0.012), 0.045 + k * 0.012, -0.045 - k * 0.012);
                Detail.feather(c, bs, tp, new V3(0, 0, 1), 0.017, new V3(sd, 0.3, 0).norm(), G_GOLD, G_GOLD2);
            }
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.42, 0.34, 0.80, 0.05, sd * 0.4, G_BROW);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.075, 0.66 + hop, 0.12), new V3(0.075, 0.075, 0.04), 10, 7, flat(G_WH));
        }
        eyes(c, hc, hr, 0.46, 0.06, 0.88, 0.108, T_BEAD, new float[]{1, 1, 1});
        cone(c, new V3(0, 0.645 + hop, 0.15), new V3(0, 0.628 + hop, 0.228), 0.038, 0.006, 6, flat(G_OR));
        cone(c, new V3(0, 0.62 + hop, 0.15), new V3(0, 0.612 + hop, 0.215), 0.032, 0.005, 6, flat(G_OR2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.012, 0.652 + hop, 0.19), new V3(0.004, 0.003, 0.006), 4, 3, flat(G_OR2));
        }
        cheeks(c, hc, hr, 0.90, -0.30, 0.38, 0.075);
        // bufanda roja con cola ondeando
        V3[] ring = new V3[17];
        for (int i = 0; i < 17; i++) {
            double a = Math.PI * 2 * i / 16.0;
            ring[i] = new V3(Math.cos(a) * 0.145, 0.56 + hop - 0.012 * Math.cos(a), Math.sin(a) * 0.135);
        }
        double[] rd = new double[17];
        java.util.Arrays.fill(rd, 0.036);
        tube(c, ring, rd, 8, (n, p) -> mix(G_RED, G_CREAM, ss(0.35, 0.55, Math.sin(Math.atan2(p.z, p.x) * 8))));
        V3 sb = new V3(0.07, 0.55 + hop, 0.12);
        double flut = Math.sin(t * 0.2 + ph) * 0.02 + Math.sin(c.gait * 0.8) * 0.02 * w;
        V3 se = sb.add(0.05, -0.15, 0.03 + flut * 2);
        tube(c, new V3[]{sb, sb.add(0.03, -0.07, 0.03 + flut), se}, new double[]{0.030, 0.028, 0.022}, 6, flat(G_RED));
        ell(c, sb.add(0.03, -0.075, 0.062 + flut), new V3(0.03, 0.012, 0.004), 0, 0, 0.2, 6, 4, flat(G_CREAM));
        for (int k = -1; k <= 1; k++) {
            tube(c, new V3[]{se.add(k * 0.012, 0, 0), se.add(k * 0.014, -0.03, flut * 0.5)}, new double[]{0.005, 0.003}, 4, flat(k == 0 ? G_CREAM : G_RED));
        }
        // nevadita sobre la cabeza y la espalda
        Detail.soft(c, hc, hr, new V3(0.3, 1, -0.2), 0.07, WHITE, 0.9);
        Detail.soft(c, hc, hr, new V3(-0.4, 0.9, 0.0), 0.05, WHITE, 0.8);
        Detail.soft(c, bc, br, new V3(0.2, 0.7, -0.8), 0.08, WHITE, 0.85);
        c.head = false;
    }

    // ------------------------------------------------------------------ 9. UNICORNIO BEBÉ

    private static final float[] U_WH = {1F, 0.97F, 1F}, U_LI = {0.78F, 0.62F, 1F}, U_GD = {1F, 0.86F, 0.40F};
    private static final float[][] RAIN = {{1F, 0.45F, 0.65F}, {1F, 0.72F, 0.40F}, {1F, 0.95F, 0.50F}, {0.55F, 0.95F, 0.60F}, {0.50F, 0.80F, 1F}, {0.78F, 0.60F, 1F}};

    static float[] rain(double x) {
        double u = ((x % 1) + 1) % 1 * RAIN.length;
        int i = (int) u;
        return mix(RAIN[i % RAIN.length], RAIN[(i + 1) % RAIN.length], ss(0.55, 1.0, u - i));
    }

    static void unicorn(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w;
        c.pivot = new V3(0, 0.3, 0);
        // cola arcoíris
        for (int k = -1; k <= 1; k++) {
            V3[] cp = new V3[5];
            double[] rd = {0.040, 0.044, 0.040, 0.030, 0.012};
            for (int i = 0; i < 5; i++) {
                double f = i / 4.0;
                cp[i] = new V3(k * 0.04 * f + Math.sin(t * 0.18 + ph - f * 3 + k) * 0.05 * f, 0.38 + bob - 0.12 * f + 0.02 * f, -0.22 - 0.26 * f);
            }
            float[] col = RAIN[(k + 1) * 2 % RAIN.length], col2 = RAIN[Math.abs(k * 3 + 2) % RAIN.length];
            curve(c, cp, rd, 3, 7, (n, p) -> mix(col, col2, ss(-0.3, -0.5, p.z)));
            Detail.furTube(c, cp, rd, 12, 271 + k, 0.04, 0.02, 0.9, (n, p) -> mix(col, col2, ss(-0.3, -0.5, p.z)), null);
        }
        legs4(c, 0.075, 0.13, -0.15, 0.24 + bob, 0.04, 0.036, 0.06, (n, p) -> U_WH, flat(U_LI));
        V3 bc = new V3(0, 0.31 + bob, -0.03), br = new V3(0.135, 0.13, 0.225);
        Paint bp = (n, p) -> mix(U_WH, new float[]{0.95F, 0.90F, 1F}, ss(0, -0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 170, 272, 0.026, 0.013, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, WHITE);
        // pezuñas doradas con motitas de brillo
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.twinkle(c, new V3(sd * 0.075, 0.04, 0.17), 0.016, new float[]{1F, 0.9F, 0.55F}, t, ph + sd * 2, 0.1);
        }
        // estrellita en el flanco
        c.emissive = true;
        glow(c, new V3(0.13, 0.32 + bob, -0.05), 0.045, new float[]{1F, 0.85F, 0.5F}, 0.35);
        glow(c, new V3(-0.13, 0.32 + bob, -0.05), 0.045, new float[]{1F, 0.85F, 0.5F}, 0.35);
        c.emissive = false;
        // melena (bolitas de colores desde la nuca hasta el lomo)
        double[][] mp = {{0.64, 0.115}, {0.585, 0.07}, {0.525, 0.04}, {0.485, -0.02}, {0.465, -0.10}, {0.455, -0.18}, {0.45, -0.25}};
        for (int i = 0; i < mp.length; i++) {
            double x = 0.012 * Math.sin(t * 0.15 + ph - i * 0.7);
            V3 pc = new V3(x, mp[i][0] + bob + 0.01 * Math.sin(t * 0.12 + i), mp[i][1]);
            final double ii = i;
            ell(c, pc, new V3(0.062, 0.065, 0.062), 8, 6, (n, p) -> rain(ii * 0.17 + ss(-1, 1, n.y) * 0.1));
        }
        // cuello y cabeza
        headLook(c, 0.12, 0);
        c.hpv = new V3(0, 0.42, 0.14);
        ell(c, new V3(0, 0.44 + bob, 0.15), new V3(0.095, 0.12, 0.10), 10, 7, (n, p) -> U_WH);
        V3 hc = new V3(0, 0.555 + bob, 0.2), hr = new V3(0.15, 0.135, 0.145);
        ell(c, hc, hr, 16, 11, (n, p) -> U_WH);
        Detail.fur(c, hc, hr, 70, 273, 0.022, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, (n, p) -> U_WH, WHITE);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.64, 0.44, 0.74, 0.06, sd * 0.4, new float[]{0.85F, 0.7F, 1F});
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.03, 0.51 + bob + k * 0.01, 0.37), new V3(sd * 0.11, 0.515 + bob + k * 0.03, 0.34), -0.006, 0.0025, new float[]{0.95F, 0.9F, 1F});
            }
        }
        V3 sc = new V3(0, 0.51 + bob, 0.315), sr = new V3(0.08, 0.063, 0.08);
        ell(c, sc, sr, 12, 8, (n, p) -> mix(U_WH, PINK, ss(0.4, 1.0, n.z) * ss(-0.2, 0.2, -n.y) * 0.25));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.03, 0.515 + bob, 0.385), new V3(0.013, 0.01, 0.01), 6, 4, flat(PINK));
        }
        decal(c, sc, sr, new V3(0, -0.45, 1), 0.085, 0, T_MOUTH, new float[]{1, 1, 1}, 0.95, 0.003);
        eyes(c, hc, hr, 0.64, 0.18, 0.74, 0.125, T_EYE, new float[]{0.72F, 0.45F, 1F});
        cheeks(c, hc, hr, 0.85, -0.22, 0.46, 0.09);
        // orejitas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.095, 0.66 + bob, 0.15), tp = new V3(sd * 0.115, 0.75 + bob, 0.135);
            double ef = Detail.flick(c, sd > 0 ? 33 : 34);
            tp = tp.add(sd * 0.04 * ef, -0.03 * ef, 0);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.040, 0.030, 0.004}, 4, (n, p) -> U_WH);
            ell(c, b.lerp(tp, 0.35).add(0, 0, 0.012), new V3(0.016, 0.03, 0.01), 0, 0, -sd * 0.2, 6, 4, flat(PINK));
        }
        // flequillo y cuerno dorado espiral
        for (int k = 0; k < 3; k++) {
            final double kk2 = k;
            ell(c, new V3((k - 1) * 0.055, 0.645 + bob - Math.abs(k - 1) * 0.015, 0.215), new V3(0.05, 0.05, 0.05), 8, 6, (n, p) -> rain(kk2 * 0.2 + 0.1));
        }
        V3 hb = new V3(0, 0.665 + bob, 0.255), ht = new V3(0, 0.865 + bob, 0.30);
        tube(c, new V3[]{hb, hb.lerp(ht, 0.33), hb.lerp(ht, 0.66), ht}, new double[]{0.036, 0.028, 0.018, 0.002}, 7, (n, p) -> {
            double s = 0.5 + 0.5 * Math.sin(p.y * 90 + n.x * 3 + n.z * 3);
            return mix(U_GD, new float[]{1F, 0.97F, 0.80F}, s * 0.7);
        });
        c.emissive = true;
        glow(c, ht, 0.10 + 0.02 * Math.sin(t * 0.3), new float[]{1F, 0.9F, 0.55F}, 0.6);
        // chispas de purpurina que suben del cuerno
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.02 + ph * 0.3 + k * 0.25) % 1.0);
            V3 sp = ht.add(Math.sin(k * 2.4 + u * 6) * 0.06 * u, 0.02 + u * 0.14, Math.cos(k * 1.9 + u * 6) * 0.05 * u);
            glowStar(c, sp, 0.026 * (1 - u * 0.6), rain(k * 0.25 + u * 0.3), t, ph + k);
        }
        c.emissive = false;
        c.head = false;
    }

    // ------------------------------------------------------------------ 10. PANDA

    private static final float[] N_WH = {0.98F, 0.97F, 0.95F}, N_BK = {0.16F, 0.15F, 0.18F}, N_GR = {0.40F, 0.78F, 0.35F}, N_GR2 = {0.65F, 0.92F, 0.45F};

    static void panda(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.01 * w;
        c.pivot = new V3(0, 0.28, 0);
        double sway = Math.sin(c.gait * 0.9) * 0.06 * w;
        double[] bm = rot(c.yaw, c.pitch, c.roll + sway);
        System.arraycopy(bm, 0, c.m, 0, 9);
        // patas: negras
        legs4(c, 0.115, 0.115, -0.145, 0.19 + bob, 0.065, 0.04, 0.08, flat(N_BK), flat(N_BK));
        // cola
        ell(c, new V3(0, 0.28 + bob, -0.23), new V3(0.055, 0.055, 0.05), 8, 6, flat(N_WH));
        Detail.fur(c, new V3(0, 0.28 + bob, -0.23), new V3(0.055, 0.055, 0.05), 30, 151, 0.035, 0.015, new V3(0, 0.3, -1), 0.7, Detail.ALL, flat(N_WH), null);
        // cuerpo (banda negra en los hombros)
        V3 bc = new V3(0, 0.285 + bob, -0.02), br = new V3(0.205, 0.18, 0.225);
        Paint bp = (n, p) -> mix(N_WH, N_BK, ss(0.085, 0.125, p.z));
        ell(c, bc, br, 16, 12, bp);
        Detail.fur(c, bc, br, 230, 152, 0.026, 0.015, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        // respiración de oso: la pancita sube y baja un poquito
        Detail.soft(c, bc, br, new V3(0, -0.2, 1), 0.14, N_WH, 0.22 + 0.08 * c.breath);
        // bambú (lo sostiene al lado)
        V3 bb = new V3(0.17, 0.12 + bob, 0.20), bt = new V3(0.19, 0.60 + bob, 0.20);
        tube(c, new V3[]{bb, bb.lerp(bt, 0.5), bt}, new double[]{0.020, 0.020, 0.018}, 6, (n, p) -> mix(N_GR, N_GR2, 0.3 + 0.35 * Math.sin(p.y * 40)));
        for (int k = 1; k <= 3; k++) {
            V3 nc = bb.lerp(bt, k / 4.0);
            ell(c, nc, new V3(0.026, 0.008, 0.026), 8, 4, flat(new float[]{0.28F, 0.60F, 0.26F}));
        }
        double lf = Math.sin(t * 0.1 + ph) * 0.03;
        for (int k = 0; k < 3; k++) {
            V3 b = bt.add(0, -0.03 - k * 0.0, 0);
            double a = k * 2.1 + 0.5;
            V3 tp = b.add(Math.cos(a) * 0.11 + lf, 0.04 + 0.02 * k, Math.sin(a) * 0.11);
            tube(c, new V3[]{b, b.lerp(tp, 0.5).add(0, 0.02, 0), tp}, new double[]{0.012, 0.022, 0.003}, 4, (n, p) -> mix(N_GR, N_GR2, 0.5));
        }
        // mano que sostiene el bambú (parte del brazo ya es la pata negra)
        // cabeza
        headLook(c, 0.14, 0);
        c.hpv = new V3(0, 0.42, 0.12);
        V3 hc = new V3(0, 0.525 + bob, 0.135), hr = new V3(0.205, 0.17, 0.175);
        ell(c, hc, hr, 18, 12, (n, p) -> N_WH);
        Detail.fur(c, hc, hr, 90, 153, 0.03, 0.014, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.6 ? 1 : 0, (n, p) -> N_WH, null);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 5; k++) {
                V3 bs = surf(hc, hr, new V3(sd * (0.92 - k * 0.04), -0.30 + k * 0.05, 0.22 - k * 0.1));
                Detail.kite(c, bs, bs.add(sd * (0.04 + k * 0.004), -0.04, -0.04), new V3(0, 0, 1), 0.017, new V3(sd, 0, 0), N_WH, new float[]{0.97F, 0.97F, 1F});
            }
            // pelusa en las orejas
            for (int k = 0; k < 4; k++) {
                V3 e0 = new V3(sd * (0.14 + (k - 1.5) * 0.012), 0.70 + bob + k * 0.004, 0.075);
                Detail.kite(c, e0, e0.add(sd * 0.012, 0.03, 0.01), new V3(1, 0, 0), 0.012, new V3(0, 0, 1), N_BK, scale(N_BK, 1.6));
            }
        }
        // masticado: la cabeza cabecea suave como si mordiera el bambú
        double ch = Math.sin(t * 0.4 + ph);

        for (int sd = -1; sd <= 1; sd += 2) {
            // orejas negras
            ell(c, new V3(sd * 0.14, 0.67 + bob, 0.08), new V3(0.062, 0.062, 0.04), 0, 0, sd * 0.35 * Detail.flick(c, sd > 0 ? 11 : 12), 10, 7, flat(N_BK));
            // parches del ojo (inclinados)
            V3 pc = new V3(sd * 0.078, 0.535 + bob, 0.285);
            ell(c, pc, new V3(0.06, 0.077, 0.025), 0, 0, sd * 0.35, 12, 8, flat(N_BK));
            decal(c, pc, new V3(0.06, 0.077, 0.025), new V3(0, 0, 1), 0.075, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? WHITE : new float[]{1, 1, 1}, 1, 0.006);
        }
        V3 sc = new V3(0, 0.47 + bob, 0.285), sr = new V3(0.065, 0.045, 0.06);
        ell(c, sc, sr, 10, 7, flat(N_WH));
        ell(c, new V3(0, 0.495 + bob, 0.335), new V3(0.026, 0.018, 0.016), 8, 5, flat(N_BK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.07, 0, T_CAT, new float[]{1, 1, 1}, 0.9, 0.002);
        Detail.glint(c, new V3(0, 0.50 + bob, 0.338), new V3(0.026, 0.018, 0.016), new V3(0, 0.7, 1), 0.02, WHITE, 0.8);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.05, 0.47 + bob + k * 0.012, 0.325), new V3(sd * 0.15, 0.475 + bob + k * 0.03, 0.30), -0.008, 0.0028, new float[]{0.55F, 0.55F, 0.6F});
            }
        }
        cheeks(c, hc, hr, 0.90, -0.22, 0.44, 0.095);
        c.head = false;
    }

    // ------------------------------------------------------------------ 11. HADA

    static void fairy(Ctx c) {
        double t = c.t, ph = c.ph;
        float[] tint = c.tint;
        float[] dress = mix(tint, WHITE, 0.15);
        float[] hair = scale(mix(tint, new float[]{0.85F, 0.55F, 0.75F}, 0.25), 0.92);
        float[] skin = {1F, 0.88F, 0.80F};
        double bob = Math.sin(t * 0.17 + ph) * 0.03;
        c.pivot = new V3(0, 0.5, 0);
        double fl = Math.sin(t * 0.85 + ph);
        glow(c, new V3(0, 0.5 + bob, 0), 1.25, tint, 0.20);
        // alas: cuatro lóbulos translúcidos que baten rápido
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 2; k++) {
                double ang = (k == 0 ? 0.55 : -0.20) + fl * 0.45;
                double len = k == 0 ? 0.30 : 0.22;
                V3 base = new V3(sd * 0.03, 0.52 + bob - k * 0.05, -0.03);
                V3 ce = base.add(sd * Math.cos(ang) * len * 0.55, Math.sin(ang) * len * 0.55 + 0.05, -0.04);
                c.blend = Sink.NORMAL;
                c.alpha = 0.62;
                ell(c, ce, new V3(len * 0.55, len * 0.22, 0.012), 0, 0, -sd * ang, 14, 8, (n, p) -> mix(mix(tint, WHITE, 0.55), new float[]{0.80F, 0.95F, 1F}, ss(-0.5, 0.8, n.y) * 0.5));
                c.alpha = 1;
                c.blend = Sink.SOLID;
            }
        }
        // polvito de hada que cae de las alas
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.017 + ph * 0.2 + k / 6.0) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 5) * 0.16, 0.62 + bob - u * 0.5, -0.08 + Math.cos(k * 1.7 + u * 4) * 0.05), 0.016 * (1 - u * 0.5), mix(tint, WHITE, 0.5), t, ph + k, 0.35);
        }
        // vestido de pétalos
        V3[] dc = {new V3(0, 0.60 + bob, 0), new V3(0, 0.50 + bob, 0), new V3(0, 0.38 + bob, 0)};
        tube(c, dc, new double[]{0.055, 0.075, 0.135}, 10, (n, p) -> mix(dress, WHITE, ss(0.5, 0.35, p.y - bob) * 0.0 + ss(0.42, 0.34, p.y - bob) * 0.5));
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * k / 6.0;
            V3 b = new V3(Math.cos(a) * 0.10, 0.40 + bob, Math.sin(a) * 0.10);
            V3 tp = new V3(Math.cos(a) * 0.15, 0.31 + bob + 0.012 * Math.sin(t * 0.2 + k), Math.sin(a) * 0.15);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.040, 0.040, 0.004}, 5, (n, p) -> mix(dress, WHITE, 0.4));
            Detail.glint(c, b.lerp(tp, 0.4).add(Math.cos(a) * 0.03, 0, Math.sin(a) * 0.03), new V3(0.04, 0.04, 0.04), new V3(Math.cos(a), 0.2, Math.sin(a)), 0.02, WHITE, 0.7);
        }
        // piernas y zapatitos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.03, 0.34 + bob, 0.01);
            V3 e = new V3(sd * 0.035, 0.18 + bob, 0.02 + Math.sin(t * 0.17 + ph + sd) * 0.02);
            tube(c, new V3[]{b, b.lerp(e, 0.5), e}, new double[]{0.016, 0.014, 0.012}, 5, flat(skin));
            ell(c, e.add(0, -0.012, 0.012), new V3(0.020, 0.016, 0.034), 8, 5, (n, p) -> mix(tint, WHITE, 0.0));
        }
        // brazos y varita
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.06, 0.58 + bob, 0.01);
            V3 hand = sd > 0 ? new V3(0.17, 0.62 + bob + Math.sin(t * 0.2 + ph) * 0.02, 0.08) : new V3(-0.14, 0.48 + bob, 0.04);
            tube(c, new V3[]{sh, sh.lerp(hand, 0.5).add(0, -0.02, 0), hand}, new double[]{0.014, 0.012, 0.011}, 5, flat(skin));
            ell(c, hand, new V3(0.016, 0.016, 0.016), 6, 4, flat(skin));
            if (sd > 0) {
                V3 wt = hand.add(0.04, 0.12, 0.02);
                tube(c, new V3[]{hand, hand.lerp(wt, 0.5), wt}, new double[]{0.006, 0.005, 0.005}, 4, flat(new float[]{0.95F, 0.82F, 0.4F}));
                c.emissive = true;
                glowStar(c, wt.add(0, 0.015, 0), 0.04, mix(tint, WHITE, 0.4), t, ph);
                c.emissive = false;
            }
        }
        // cabeza
        headLook(c, 0.12, 0);
        c.hpv = new V3(0, 0.62, 0);
        V3 hc = new V3(0, 0.74 + bob, 0.0), hr = new V3(0.155, 0.145, 0.145);
        V3 hrc = new V3(0, 0.76 + bob, -0.035), hrr = new V3(0.175, 0.16, 0.16);
        Paint hpn = (n, p) -> mix(hair, WHITE, ss(0.3, 1.0, n.y) * 0.18);
        ell(c, hrc, hrr, 16, 10, hpn);
        // mechones que ondean con el vuelo
        Detail.fur(c, hrc, hrr, 120, 361, 0.07, 0.022, new V3(Math.sin(t * 0.1 + ph) * 0.3, -0.8, -1), 1.0, d -> d.z < 0.1 ? 1 : 0, hpn, scale(hair, 1.25));
        Detail.glint(c, hrc, hrr, new V3(0.3, 1, -0.2), 0.08, WHITE, 0.6);
        ell(c, hc, hr, 16, 11, (n, p) -> skin);
        // flequillo
        for (int k = -2; k <= 2; k++) {
            ell(c, new V3(k * 0.055, 0.845 + bob - Math.abs(k) * 0.012, 0.075), new V3(0.05, 0.04, 0.05), 8, 5, (n, p) -> hair);
        }
        // moños
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.145, 0.835 + bob, -0.02), new V3(0.055, 0.055, 0.055), 8, 6, (n, p) -> hair);
            ell(c, new V3(sd * 0.155, 0.76 + bob, -0.05), new V3(0.04, 0.07, 0.04), 8, 6, (n, p) -> hair);
        }
        eyes(c, hc, hr, 0.62, 0.0, 0.78, 0.12, T_EYE, mix(tint, new float[]{0.4F, 0.7F, 1F}, 0.5));
        decal(c, hc, hr, new V3(0, -0.40, 1), 0.045, 0, T_MOUTH, new float[]{1, 1, 1}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.62, 0.30, 0.78, 0.05, sd * 0.4, scale(hair, 0.85));
        }
        // florecita en el moño
        {
            V3 fp = new V3(0.15, 0.86 + bob, 0.02);
            for (int k = 0; k < 5; k++) {
                double a = k * Math.PI * 2 / 5;
                ell(c, fp.add(Math.cos(a) * 0.022, Math.sin(a) * 0.022, 0.02), new V3(0.016, 0.016, 0.01), 6, 4, flat(new float[]{1F, 1F, 0.95F}));
            }
            ell(c, fp.add(0, 0, 0.026), new V3(0.012, 0.012, 0.01), 5, 4, flat(new float[]{1F, 0.85F, 0.3F}));
        }
        cheeks(c, hc, hr, 0.86, -0.22, 0.46, 0.085);
        // orejitas puntiagudas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.145, 0.745 + bob, -0.01), tp = new V3(sd * 0.22, 0.78 + bob, -0.03);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.026, 0.018, 0.003}, 4, flat(skin));
        }
        c.head = false;
    }
}
