package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Doce compañeros más: pollito, rana, hámster, caracol, abeja, oveja, erizo, ajolote, mapache, ciervo lunar,
 * ballena estelar y zorro de nueve colas. Mismo motor que {@link Creature3D}.
 */
final class Creatures3 {
    private Creatures3() {
    }

    // ------------------------------------------------------------------ 11. POLLITO

    private static final float[] CH_Y = {1F, 0.90F, 0.36F}, CH_Y2 = {1F, 0.97F, 0.62F}, CH_OR = {1F, 0.60F, 0.18F};

    static void chick(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 1.1)) * 0.07 * w;
        c.pivot = new V3(0, 0.2, 0);
        // patitas con tres deditos y espolón
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.1 + (sd > 0 ? 0 : Math.PI);
            double sw = Math.sin(p) * 0.04 * w;
            V3 a = new V3(sd * 0.06, 0.1 + hop * 0.4, 0.0), b = new V3(sd * 0.06, 0.012, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.013, 0.012, 0.011}, 5, flat(CH_OR));
            ell(c, b.add(0, 0, 0.03), new V3(0.04, 0.010, 0.05), 8, 4, flat(CH_OR));
            for (int k = -1; k <= 1; k++) {
                ell(c, b.add(k * 0.026, 0.002, 0.075 - Math.abs(k) * 0.012), new V3(0.009, 0.007, 0.022), 0, k * 0.35, 0, 6, 4, flat(CH_OR2));
            }
        }
        // cuerpo esponjoso cubierto de plumón
        Paint down = (n, p) -> mix(CH_Y, CH_Y2, ss(-0.2, 0.9, n.z * 0.5 + n.y * 0.5));
        V3 bc = new V3(0, 0.25 + hop, -0.01), br = new V3(0.20, 0.19, 0.21);
        ell(c, bc, br, 16, 11, down);
        Detail.fur(c, bc, br, 170, 11, 0.036, 0.015, new V3(0, -1, -0.5), 0.9, Detail.ALL, down, CH_Y2);
        // pancita más clara
        Detail.soft(c, bc, br, new V3(0, -0.25, 1), 0.22, CH_Y2, 0.7);
        // alitas con plumas largas que cuelgan
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = 0.35 + Math.sin(t * 0.22 + ph) * 0.05 + Math.abs(Math.sin(c.gait * 1.1)) * 0.5 * w;
            V3 wc = new V3(sd * 0.205, 0.26 + hop, -0.02);
            ell(c, wc, new V3(0.035, 0.095, 0.11), 0, 0, -sd * fl, 8, 6, flat(CH_Y));
            for (int k = -2; k <= 2; k++) {
                V3 base = wc.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.012, -0.055, k * 0.042)));
                V3 tip = wc.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.03, -0.14 + Math.abs(k) * 0.012, k * 0.05)));
                Detail.feather(c, base, tip, new V3(0, 0, 1), 0.020, new V3(sd, 0, 0), CH_Y, CH_Y2);
            }
        }
        // colita de plumas (abanico de cinco)
        for (int k = -2; k <= 2; k++) {
            V3 a = new V3(0, 0.27 + hop, -0.18), b = new V3(k * 0.045, 0.37 + hop - Math.abs(k) * 0.012, -0.28);
            Detail.feather(c, a, b, new V3(1, 0, 0), 0.026, new V3(0, 0.3, 1).norm(), CH_Y, CH_Y2);
            Detail.feather(c, a, b, new V3(0, 1, 0), 0.020, new V3(1, 0, 0), CH_Y, CH_Y2);
        }
        // cabeza (pegada al cuerpo, muy redonda)
        headLook(c, 0.14, 0);
        c.hpv = new V3(0, 0.38, 0.05);
        V3 hc = new V3(0, 0.40 + hop, 0.03), hr = new V3(0.17, 0.16, 0.16);
        Paint hp = (n, p) -> mix(CH_Y, CH_Y2, ss(-0.3, 1.0, n.z));
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 110, 23, 0.034, 0.013, new V3(0, -0.6, -1), 0.8, d -> ss(-0.2, -0.6, d.y * -1) * 0.0 + (d.z < 0.55 ? 1 : 0), hp, CH_Y2);
        // piquito de dos piezas con narinas
        cone(c, new V3(0, 0.392 + hop, 0.165), new V3(0, 0.388 + hop, 0.255), 0.034, 0.008, 6, flat(CH_OR));
        cone(c, new V3(0, 0.372 + hop, 0.165), new V3(0, 0.378 + hop, 0.245), 0.028, 0.006, 6, flat(CH_OR2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.012, 0.405 + hop, 0.215), new V3(0.004, 0.003, 0.006), 4, 3, flat(CH_OR2));
        }
        eyes(c, hc, hr, 0.60, 0.15, 0.78, 0.115, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.90, -0.22, 0.40, 0.10);
        // cejitas
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.58, 0.40, 0.70, 0.045, sd * 0.9, CH_OR2);
        }
        // sombrerito de cascarón roto con mechón asomando
        Paint shell = (n, p) -> mix(SHELL_D, SHELL, ss(-0.6, 0.8, n.y));
        Detail.cap(c, hc.add(0, 0.015, -0.01), new V3(0.178, 0.168, 0.168), 0, -0.25, 0.12, 0.95, 0.12, 14, 5, shell);
        for (int k = 0; k < 3; k++) {
            V3 a = new V3((k - 1) * 0.012, 0.545 + hop, 0.0), b = new V3((k - 1) * 0.04 + Math.sin(t * 0.1 + ph) * 0.01, 0.64 + hop - Math.abs(k - 1) * 0.02, 0.02 + k * 0.01 - 0.01);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.010, 0.008, 0.002}, 4, flat(CH_Y));
        }
        c.head = false;
    }

    private static final float[] CH_OR2 = {0.85F, 0.42F, 0.12F}, SHELL = {1F, 0.97F, 0.90F}, SHELL_D = {0.86F, 0.80F, 0.70F};

    // ------------------------------------------------------------------ 12. RANA

    private static final float[] FR_G = {0.40F, 0.85F, 0.42F}, FR_G2 = {0.72F, 0.96F, 0.55F}, FR_DK = {0.22F, 0.60F, 0.32F};

    static void frog(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 0.8)) * 0.14 * w;
        double crouch = (1 - Math.abs(Math.sin(c.gait * 0.8))) * 0.03 * w;
        c.pivot = new V3(0, 0.2, 0);
        // patas traseras dobradas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 hip = new V3(sd * 0.15, 0.17 + hop, -0.12);
            V3 knee = new V3(sd * 0.25, 0.14 + hop * 0.6, -0.04 - 0.05 * w);
            V3 foot = new V3(sd * 0.22, 0.03 + hop * 0.2, 0.08 - 0.06 * w);
            tube(c, new V3[]{hip, knee, foot}, new double[]{0.07, 0.05, 0.03}, 8, (n, p) -> FR_G);
            ell(c, foot.add(sd * 0.01, 0, 0.05), new V3(0.075, 0.018, 0.075), 10, 5, flat(FR_G2));
            for (int k = -1; k <= 1; k++) {
                ell(c, foot.add(sd * 0.01 + k * 0.035, 0.0, 0.11), new V3(0.016, 0.014, 0.022), 6, 4, flat(FR_G2));
            }
        }
        // cuerpo ancho y bajo
        ell(c, new V3(0, 0.20 + hop - crouch, -0.01), new V3(0.23, 0.15, 0.25), 16, 11, (n, p) -> mix(FR_G, FR_G2, ss(0.0, -1.0, n.y) * 0.9));
        // piel brillante: verrugas, motas claras y reflejos de humedad
        {
            V3 sc0 = new V3(0, 0.20 + hop - crouch, -0.01), sr0 = new V3(0.23, 0.15, 0.25);
            for (int k = 0; k < 16; k++) {
                V3 d = Detail.sph(301, k);
                if (d.y < 0.1) d = new V3(d.x, -d.y + 0.2, d.z);
                Detail.spot(c, sc0, sr0, d, 0.022 + 0.014 * Detail.rnd(301, k, 7), k % 3 == 0 ? FR_G2 : FR_DK);
            }
            Detail.glint(c, sc0, sr0, new V3(0.3, 1, 0.4), 0.05, WHITE, 0.7);
            Detail.glint(c, sc0, sr0, new V3(-0.5, 0.8, -0.2), 0.035, WHITE, 0.6);
        }
        // manchas
        for (int k = 0; k < 4; k++) {
            double a = k * 1.7 + 0.5;
            V3 dir = new V3(Math.cos(a) * 0.7, 0.85, Math.sin(a) * 0.7 - 0.1);
            V3 ce = new V3(0, 0.20 + hop - crouch, -0.01);
            V3 pt = surf(ce, new V3(0.23, 0.15, 0.25), dir);
            ell(c, pt.add(0, 0.002, 0), new V3(0.04, 0.008, 0.04), 8, 4, flat(FR_DK));
        }
        // patas delanteras
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.13, 0.14 + hop, 0.15), b = new V3(sd * 0.15, 0.025, 0.2);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.04, 0.034, 0.03}, 6, flat(FR_G));
            ell(c, b.add(0, 0, 0.03), new V3(0.05, 0.016, 0.055), 8, 4, flat(FR_G2));
        }
        // cabeza
        headLook(c, 0.10, 0);
        c.hpv = new V3(0, 0.25, 0.1);
        V3 hc = new V3(0, 0.30 + hop - crouch, 0.13), hr = new V3(0.21, 0.12, 0.17);
        ell(c, hc, hr, 16, 10, (n, p) -> mix(FR_G, FR_G2, ss(0.2, -1.0, n.y) * 0.6));
        // ojos saltones
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.115, 0.40 + hop - crouch, 0.11), er = new V3(0.07, 0.07, 0.07);
            ell(c, ec, er, 12, 8, flat(FR_G));
            decal(c, ec, er, new V3(sd * 0.25, 0.1, 1), 0.105, 0, c.shut ? T_EYE_SHUT : T_EYE, c.shut ? WHITE : new float[]{1, 0.95F, 0.6F}, 1, 0.003);
        }
        // sonrisa ancha y saco de la garganta
        decal(c, hc, hr, new V3(0, -0.30, 1), 0.16, 0, T_MOUTH, new float[]{0.2F, 0.35F, 0.2F}, 0.9, 0.003);
        double puff = 0.5 + 0.5 * Math.sin(t * 0.16 + ph);
        ell(c, new V3(0, 0.22 + hop - crouch, 0.20), new V3(0.075 + 0.02 * puff, 0.05 + 0.02 * puff, 0.07), 10, 6, flat(new float[]{1F, 0.80F, 0.78F}));
        cheeks(c, hc, hr, 1.0, -0.32, 0.45, 0.09);
        Detail.glint(c, hc, hr, new V3(0.4, 1, 0.5), 0.04, WHITE, 0.7);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.50, 0.62, 0.50, 0.06, sd * 0.2, FR_DK);
        }
        // lengua pegajosa que atrapa una mosca de vez en cuando
        double tu = ((t + ph * 50) % 300) / 22.0;
        double tl = tu < 1 ? Math.sin(tu * Math.PI) : 0;
        V3 tip0 = new V3(0, 0.275 + hop - crouch, 0.31 + 0.30 * tl);
        if (tl > 0.02) {
            tube(c, new V3[]{new V3(0, 0.272 + hop - crouch, 0.29), tip0.lerp(new V3(0, 0.272 + hop - crouch, 0.29), 0.5), tip0}, new double[]{0.014, 0.012, 0.018}, 5, flat(new float[]{1F, 0.5F, 0.58F}));
        }
        {
            double fa = t * 0.07 + ph;
            V3 orb = new V3(Math.cos(fa) * 0.42, 0.50 + 0.06 * Math.sin(fa * 2.3) + hop, Math.sin(fa) * 0.42 + 0.1);
            V3 fp = orb.lerp(tip0, tl);
            ell(c, fp, new V3(0.011, 0.01, 0.016), 6, 4, flat(new float[]{0.12F, 0.12F, 0.15F}));
            double wf = Math.sin(t * 2.6 + ph) * 0.5 + 0.5;
            c.blend = Sink.NORMAL; c.alpha = 0.55;
            for (int sd = -1; sd <= 1; sd += 2) {
                ell(c, fp.add(sd * 0.012, 0.01, 0), new V3(0.012, 0.002, 0.007 + 0.005 * wf), 0, 0, sd * (0.3 + 0.5 * wf), 6, 3, flat(new float[]{0.9F, 0.97F, 1F}));
            }
            c.alpha = 1; c.blend = Sink.SOLID;
        }
        // hojita en la cabeza
        V3 lb = new V3(0.04, 0.415 + hop - crouch, -0.01);
        ell(c, lb.add(0.03, 0.02, 0.0), new V3(0.075, 0.01, 0.045), 0, 0, 0.25, 10, 5, flat(new float[]{0.35F, 0.80F, 0.35F}));
        c.head = false;
    }

    // ------------------------------------------------------------------ 13. HÁMSTER

    private static final float[] HA_BROW = {0.95F, 0.85F, 0.7F};
    private static final float[] HA_O = {1F, 0.74F, 0.40F}, HA_W = {1F, 0.96F, 0.88F}, HA_PK = {1F, 0.62F, 0.68F};

    static void hamster(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.3)) * 0.035 * w;
        c.pivot = new V3(0, 0.2, 0);
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.3 + (sd > 0 ? 0 : Math.PI);
            ell(c, new V3(sd * 0.11, 0.045 + Math.max(0, Math.cos(p)) * 0.03 * w, 0.05 + Math.sin(p) * 0.04 * w), new V3(0.055, 0.04, 0.08), 8, 5, flat(HA_PK));
        }
        // cuerpo gordito
        V3 bc = new V3(0, 0.22 + bob, -0.02), br = new V3(0.23, 0.20, 0.24);
        Paint bp = (n, p) -> mix(HA_O, HA_W, ss(0.1, 0.9, n.z * 0.6 - n.y * 0.3) * 0.9 * ss(0, 1, 1 - n.y));
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 220, 131, 0.022, 0.013, new V3(0, -0.6, -1), 0.9, Detail.ALL, bp, HA_W);
        Detail.fur(c, bc, br, 40, 132, 0.04, 0.015, new V3(0, -1, 0.2), 0.7, d -> ss(0.4, 0.8, d.z) * ss(0.6, -0.3, d.y), bp, HA_W);
        // manchita en el lomo
        ell(c, new V3(0, 0.36 + bob, -0.07), new V3(0.12, 0.05, 0.13), 10, 5, flat(mix(HA_O, new float[]{0.9F, 0.5F, 0.25F}, 0.6)));
        // brazos con una semilla de girasol
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.14, 0.22 + bob, 0.14), b = new V3(sd * 0.05, 0.19 + bob, 0.24);
            tube(c, new V3[]{a, a.lerp(b, 0.5).add(0, -0.01, 0.02), b}, new double[]{0.032, 0.028, 0.026}, 6, flat(HA_W));
            ell(c, b, new V3(0.03, 0.03, 0.03), 6, 4, flat(HA_PK));
        }
        ell(c, new V3(0, 0.205 + bob, 0.26), new V3(0.03, 0.045, 0.014), 0, 0, 0.5, 8, 5, (n, p) -> mix(new float[]{0.25F, 0.2F, 0.2F}, new float[]{0.9F, 0.9F, 0.85F}, ss(-0.2, 0.7, Math.sin(p.x * 150))));
        // cabeza con mejillas hinchadas
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.36, 0.1);
        V3 hc = new V3(0, 0.40 + bob, 0.10), hr = new V3(0.20, 0.16, 0.17);
        Paint hp = (n, p) -> mix(HA_O, HA_W, ss(0.0, 0.8, n.z - n.y * 0.4));
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 80, 133, 0.028, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.6 ? 1 : 0, hp, HA_W);
        // mejillas que se inflan y desinflan mientras mordisquea la semilla
        double chew = 0.5 + 0.5 * Math.sin(t * 0.55 + ph);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 cc = new V3(sd * 0.135, 0.355 + bob, 0.15), cr2 = new V3(0.095 + 0.012 * chew, 0.08 + 0.008 * chew, 0.09 + 0.008 * chew);
            ell(c, cc, cr2, 12, 8, flat(HA_W));
            Detail.fur(c, cc, cr2, 22, 134 + sd, 0.026, 0.012, new V3(sd * 0.3, -0.5, -1), 0.7, Detail.ALL, flat(HA_W), HA_W);
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.17, 0.375 + bob + k * 0.012, 0.22), new V3(sd * 0.30, 0.385 + bob + k * 0.035, 0.19), -0.01, 0.0028, new float[]{0.95F, 0.90F, 0.85F});
            }
            Detail.brow(c, hc, hr, sd, 0.52, 0.44, 0.68, 0.05, sd * 0.5, HA_BROW);
            // orejas
            ell(c, new V3(sd * 0.115, 0.55 + bob, 0.06), new V3(0.055, 0.06, 0.025), 0, 0, -sd * 0.3 + sd * 0.25 * Detail.flick(c, sd > 0 ? 9 : 10), 10, 6, flat(HA_O));
            ell(c, new V3(sd * 0.115, 0.55 + bob, 0.075), new V3(0.035, 0.04, 0.012), 0, 0, -sd * 0.3, 8, 5, flat(HA_PK));
        }
        double nz = Math.sin(t * 0.55 + ph) * 0.003;
        ell(c, new V3(0, 0.395 + bob + nz, 0.255), new V3(0.016, 0.012, 0.012), 8, 5, flat(HA_PK));
        Detail.glint(c, new V3(0, 0.40 + bob + nz, 0.26), new V3(0.016, 0.012, 0.012), new V3(0, 0.7, 1), 0.016, WHITE, 0.9);
        // dientitos de roedor
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.007, 0.372 + bob - 0.004 * chew, 0.248), new V3(0.007, 0.014, 0.004), 6, 4, flat(WHITE));
        }
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.065, 0, T_CAT, new float[]{0.45F, 0.3F, 0.3F}, 0.8, 0.003);
        eyes(c, hc, hr, 0.56, 0.20, 0.80, 0.105, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.12, 0.35, 0.085);
        c.head = false;
    }

    // ------------------------------------------------------------------ 14. CARACOL

    private static final float[] SN_B = {0.98F, 0.88F, 0.72F}, SN_B2 = {1F, 0.95F, 0.85F};

    static void snail(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        c.pivot = new V3(0, 0.15, 0);
        double wave = c.gait * 0.7;
        // cuerpo largo que ondula
        int n = 6;
        V3[] cp = new V3[n];
        double[] rd = new double[n];
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            double z = -0.28 + f * 0.62;
            cp[i] = new V3(0, 0.07 + 0.015 * Math.sin(wave - f * 4) * w + (i == n - 1 ? 0.03 : 0), z);
            rd[i] = i == 0 ? 0.03 : (i == n - 1 ? 0.075 : 0.09 - 0.012 * Math.abs(f - 0.4));
        }
        curve(c, cp, rd, 3, 10, (nn, p) -> mix(SN_B, SN_B2, ss(0.0, -1.0, nn.y) * 0.8));
        for (int k = 0; k < 14; k++) {
            V3 d = Detail.sph(311, k);
            if (d.y < 0.0) d = new V3(d.x, -d.y, d.z);
            double f = Detail.rnd(311, k, 5);
            V3 bc0 = cp[1 + (int) (f * 3)], br0 = new V3(0.09, 0.07, 0.10);
            Detail.spot(c, bc0.add(0, 0.0, 0), br0, d, 0.02 + 0.012 * Detail.rnd(311, k, 6), new float[]{1F, 0.78F, 0.62F});
        }
        // rastro de baba brillante detrás
        c.blend = Sink.NORMAL; c.alpha = 0.18;
        ell(c, new V3(0, 0.005, -0.42), new V3(0.075, 0.003, 0.14), 12, 3, flat(new float[]{0.85F, 0.95F, 1F}));
        c.alpha = 1; c.blend = Sink.SOLID;
        Detail.twinkle(c, new V3(0.02, 0.02, -0.46 - 0.06 * Math.sin(t * 0.02 + ph)), 0.02, new float[]{0.9F, 1F, 1F}, t, ph, 0.12);
        // base de la panza (suela)
        ell(c, new V3(0, 0.02, 0.04), new V3(0.105, 0.02, 0.33), 10, 4, flat(new float[]{0.95F, 0.78F, 0.66F}));
        // concha en espiral
        V3 sc = new V3(0, 0.27, -0.06);
        int steps = 34;
        V3[] sp = new V3[steps];
        double[] sr = new double[steps];
        for (int i = 0; i < steps; i++) {
            double f = i / (double) (steps - 1);
            double a = f * Math.PI * 4.2;
            double rr = 0.02 + 0.115 * f;
            sp[i] = new V3(0.0 + (f - 0.5) * 0.06, sc.y + Math.sin(a) * rr * 1.05 - 0.04, sc.z + Math.cos(a) * rr * 1.05);
            sr[i] = 0.018 + 0.08 * f;
        }
        curve(c, sp, sr, 1, 9, (nn, p) -> Creatures2.rain(p.z * 3.1 + p.y * 3.7 + 0.12));
        // florecita que le creció en la concha y brillos de caracola
        {
            V3 fc = new V3(0.0, 0.405, -0.06);
            for (int k = 0; k < 6; k++) {
                double a = k * Math.PI / 3 + 0.2;
                ell(c, fc.add(Math.cos(a) * 0.026, 0.004, Math.sin(a) * 0.026), new V3(0.018, 0.008, 0.012), 0, a, 0.0, 6, 4, flat(k % 2 == 0 ? new float[]{1F, 0.7F, 0.82F} : new float[]{1F, 0.85F, 0.9F}));
            }
            ell(c, fc.add(0, 0.008, 0), new V3(0.014, 0.012, 0.014), 6, 4, flat(new float[]{1F, 0.85F, 0.3F}));
            Detail.twinkle(c, new V3(0.06, 0.33, 0.0), 0.022, new float[]{1F, 0.9F, 1F}, t, ph, 0.09);
            Detail.twinkle(c, new V3(-0.06, 0.25, -0.15), 0.02, new float[]{0.9F, 1F, 1F}, t, ph + 2, 0.11);
        }
        // brillo
        c.emissive = true;
        glow(c, new V3(0, 0.30, -0.06), 0.45, new float[]{1F, 0.9F, 1F}, 0.10 + 0.05 * Math.sin(t * 0.1 + ph));
        c.emissive = false;
        // cabeza y antenas con ojitos
        headLook(c, 0.10, 0);
        c.hpv = new V3(0, 0.16, 0.30);
        V3 hc = new V3(0, 0.19, 0.34), hr = new V3(0.085, 0.075, 0.09);
        ell(c, hc, hr, 12, 8, flat(SN_B2));
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.12 + ph + sd * 1.3) * 0.02 + 0.03 * Detail.flick(c, sd > 0 ? 35 : 36);
            V3 a = new V3(sd * 0.035, 0.22, 0.35), b = new V3(sd * (0.06 + sw), 0.34, 0.38 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.011, 0.010, 0.009}, 5, flat(SN_B));
            ell(c, b, new V3(0.032, 0.032, 0.032), 8, 6, flat(SN_B2));
            decal(c, b, new V3(0.032, 0.032, 0.032), new V3(sd * 0.1, 0.1, 1), 0.058, 0, c.shut ? T_EYE_SHUT : T_BEAD, new float[]{1, 1, 1}, 1, 0.002);
        }
        decal(c, hc, hr, new V3(0, -0.45, 1), 0.05, 0, T_MOUTH, new float[]{0.4F, 0.25F, 0.25F}, 0.9, 0.003);
        // tentáculos cortitos bajo los ojos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.05, 0.19, 0.415), b = new V3(sd * 0.07, 0.17 + 0.012 * Math.sin(t * 0.2 + ph + sd), 0.46);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.008, 0.007, 0.006}, 4, flat(SN_B));
            ell(c, b, new V3(0.01, 0.01, 0.01), 5, 4, flat(SN_B2));
        }
        Detail.brow(c, hc, hr, 1, 0.55, 0.42, 0.80, 0.04, 0.4, new float[]{0.85F, 0.7F, 0.55F});
        Detail.brow(c, hc, hr, -1, 0.55, 0.42, 0.80, 0.04, -0.4, new float[]{0.85F, 0.7F, 0.55F});
        cheeks(c, hc, hr, 0.95, -0.2, 0.4, 0.06);
        c.head = false;
    }

    // ------------------------------------------------------------------ 15. ABEJA (vuela)

    private static final float[] BE_Y = {1F, 0.80F, 0.20F}, BE_K = {0.22F, 0.17F, 0.18F};

    static void bee(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.2 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double fl = Math.sin(t * 2.2 + ph);
        glow(c, new V3(0, 0.42 + bob, 0), 0.9, new float[]{1F, 0.85F, 0.35F}, 0.14);
        // alas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 2; k++) {
                double ang = (k == 0 ? 0.40 : 0.10) + fl * 0.45;
                double len = k == 0 ? 0.20 : 0.14;
                V3 ce = new V3(sd * (0.06 + Math.cos(ang) * len * 0.6), 0.55 + bob + Math.sin(ang) * len * 0.6 - k * 0.02, -0.04 - k * 0.05);
                c.blend = Sink.NORMAL;
                c.alpha = 0.55;
                ell(c, ce, new V3(len * 0.62, len * 0.24, 0.01), 0, 0, -sd * ang, 12, 6, (n, p) -> mix(new float[]{0.85F, 0.95F, 1F}, WHITE, ss(-0.5, 0.8, n.y)));
                c.alpha = 1;
                c.blend = Sink.SOLID;
            }
        }
        // abdomen rayado
        ell(c, new V3(0, 0.40 + bob, -0.12), new V3(0.15, 0.14, 0.20), 16, 10, (n, p) -> mix(BE_Y, BE_K, ss(0.0, 0.1, Math.sin((p.z + 0.12) * 28))));
        cone(c, new V3(0, 0.38 + bob, -0.30), new V3(0, 0.34 + bob, -0.40), 0.035, 0.004, 6, flat(BE_K));
        // tórax peludo
        V3 tcx = new V3(0, 0.43 + bob, 0.06), trx = new V3(0.125, 0.12, 0.10);
        ell(c, tcx, trx, 12, 8, flat(mix(BE_Y, WHITE, 0.25)));
        Detail.fur(c, tcx, trx, 90, 321, 0.03, 0.014, new V3(0, 0.1, -1), 0.9, Detail.ALL, flat(mix(BE_Y, WHITE, 0.25)), WHITE);
        V3 acx = new V3(0, 0.40 + bob, -0.12), arx = new V3(0.15, 0.14, 0.20);
        Detail.fur(c, acx, arx, 70, 322, 0.018, 0.011, new V3(0, 0, -1), 0.9, d -> d.z > -0.2 ? 1 : 0, (n, p) -> mix(BE_Y, BE_K, ss(0.0, 0.1, Math.sin((p.z + 0.12) * 28))), null);
        Detail.glint(c, acx, arx, new V3(0.3, 1, -0.1), 0.05, WHITE, 0.7);
        // bolsitas de polen en las patas traseras
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.075, 0.30 + bob, -0.07), new V3(0.026, 0.03, 0.03), 8, 5, flat(new float[]{1F, 0.62F, 0.15F}));
            Detail.fur(c, new V3(sd * 0.075, 0.30 + bob, -0.07), new V3(0.026, 0.03, 0.03), 8, 323 + sd, 0.014, 0.01, new V3(sd, 0.3, 0), 0.7, Detail.ALL, flat(new float[]{1F, 0.62F, 0.15F}), new float[]{1F, 0.8F, 0.3F});
        }
        // polvillo dorado que va cayendo
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.018 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.4 + u * 4) * 0.08, 0.30 + bob - u * 0.25, -0.15 + Math.cos(k * 1.7) * 0.06), 0.014 * (1 - u * 0.5), new float[]{1F, 0.85F, 0.3F}, t, ph + k, 0.3);
        }
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 a = new V3(sd * 0.05, 0.34 + bob, 0.08 - k * 0.07), b = new V3(sd * 0.09, 0.22 + bob + Math.sin(t * 0.3 + k + sd) * 0.012, 0.11 - k * 0.07);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.008, 0.007, 0.006}, 4, flat(BE_K));
            }
        }
        // cabeza
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.45, 0.14);
        V3 hc = new V3(0, 0.46 + bob, 0.20), hr = new V3(0.13, 0.115, 0.115);
        ell(c, hc, hr, 14, 10, flat(BE_Y));
        Detail.fur(c, hc, hr, 60, 324, 0.022, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, flat(BE_Y), WHITE);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.58, 0.44, 0.74, 0.05, sd * 0.5, BE_K);
        }
        eyes(c, hc, hr, 0.58, 0.12, 0.80, 0.12, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.25, 0.38, 0.075);
        decal(c, hc, hr, new V3(0, -0.4, 1), 0.06, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.15 + sd) * 0.015 + 0.03 * Detail.flick(c, sd > 0 ? 37 : 38);
            V3 a = new V3(sd * 0.04, 0.55 + bob, 0.24), m = new V3(sd * 0.07, 0.65 + bob, 0.27), b = new V3(sd * (0.09 + sw), 0.70 + bob, 0.31);
            tube(c, new V3[]{a, m, b}, new double[]{0.007, 0.006, 0.005}, 4, flat(BE_K));
            ell(c, b, new V3(0.016, 0.016, 0.016), 6, 4, flat(BE_Y));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 16. OVEJA NUBE

    private static final float[] SH_W = {1F, 0.98F, 0.96F}, SH_W2 = {0.88F, 0.93F, 1F}, SH_F = {0.33F, 0.26F, 0.28F};

    static void sheep(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w;
        c.pivot = new V3(0, 0.3, 0);
        // patas
        legs4(c, 0.09, 0.13, -0.13, 0.20 + bob, 0.028, 0.03, 0.04, flat(SH_F), flat(SH_F));
        // lana: nube de esferas
        double[][] puffs = {{0, 0.34, -0.02, 0.17}, {0.12, 0.30, 0.07, 0.12}, {-0.12, 0.30, 0.07, 0.12}, {0.14, 0.31, -0.10, 0.12}, {-0.14, 0.31, -0.10, 0.12},
                {0, 0.46, 0.0, 0.12}, {0.09, 0.42, 0.10, 0.09}, {-0.09, 0.42, 0.10, 0.09}, {0.0, 0.34, -0.20, 0.11}, {0.1, 0.42, -0.1, 0.1}, {-0.1, 0.42, -0.1, 0.1},
                {0.17, 0.22, 0.0, 0.09}, {-0.17, 0.22, 0.0, 0.09}, {0, 0.20, 0.07, 0.09}, {0, 0.20, -0.12, 0.09}};
        for (double[] pf : puffs) {
            double jig = Math.sin(t * 0.05 + pf[0] * 30 + pf[2] * 20 + ph) * 0.004;
            double rr = pf[3];
            V3 pc = new V3(pf[0], pf[1] + bob + jig, pf[2]);
            Paint wp = (n, p) -> mix(SH_W2, SH_W, ss(-0.5, 0.8, n.y));
            ell(c, pc, new V3(rr, rr * 0.92, rr), 10, 7, wp);
            // rizos de lana
            if (rr > 0.095) Detail.fur(c, pc, new V3(rr, rr * 0.92, rr), (int) (rr * 220), 181 + (long) (pf[0] * 50 + pf[2] * 30), 0.032, 0.02, new V3(0, 0.2, 0), 0.0, Detail.ALL, wp, SH_W);
        }
        // colita de lana
        ell(c, new V3(0, 0.30 + bob, -0.27), new V3(0.04, 0.04, 0.04), 8, 5, flat(SH_W));
        // patitas con pezuñas oscuras y toquecito de rizos
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                Detail.furTube(c, new V3[]{new V3(sd * 0.09, 0.19 + bob, fb == 0 ? 0.13 : -0.13), new V3(sd * 0.09, 0.14 + bob, fb == 0 ? 0.13 : -0.13)}, new double[]{0.03, 0.028}, 6, 183 + sd * 3 + fb, 0.03, 0.016, 0.0, flat(SH_W), null);
            }
        }
        // cabeza oscura de oveja
        headLook(c, 0.13, 0);
        c.hpv = new V3(0, 0.36, 0.14);
        V3 hc = new V3(0, 0.40 + bob, 0.22), hr = new V3(0.105, 0.115, 0.115);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(SH_F, new float[]{0.45F, 0.36F, 0.38F}, ss(0, 1, n.z) * 0.5));
        for (int k = -1; k <= 1; k++) {
            V3 fc = new V3(k * 0.05, 0.53 + bob, 0.17);
            ell(c, fc, new V3(0.06, 0.05, 0.06), 8, 5, flat(SH_W));
            Detail.fur(c, fc, new V3(0.06, 0.05, 0.06), 14, 190 + k, 0.03, 0.018, new V3(0, 0.3, 0), 0.0, Detail.ALL, flat(SH_W), SH_W);
        }
        Detail.fur(c, hc, hr, 50, 195, 0.018, 0.01, new V3(0, -0.4, -1), 0.8, d -> d.z > 0.2 ? 1 : 0, (n, p) -> SH_F, new float[]{0.5F, 0.4F, 0.42F});
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.04, 0.35 + bob + k * 0.01, 0.325), new V3(sd * 0.12, 0.355 + bob + k * 0.03, 0.30), -0.006, 0.0025, new float[]{0.9F, 0.88F, 0.9F});
            }
            Detail.brow(c, hc, hr, sd, 0.55, 0.42, 0.72, 0.05, sd * 0.4, new float[]{0.55F, 0.45F, 0.48F});
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 17 : 18);
            ell(c, new V3(sd * 0.135, 0.40 + bob, 0.20), new V3(0.06, 0.028, 0.03), 0, 0, sd * (0.35 - 0.45 * ef), 10, 5, flat(SH_F));
            ell(c, new V3(sd * 0.135, 0.40 + bob, 0.215), new V3(0.035, 0.014, 0.012), 0, 0, sd * (0.35 - 0.45 * ef), 8, 4, flat(PINK));
        }
        eyes(c, hc, hr, 0.55, 0.18, 0.85, 0.095, T_BEAD, new float[]{1, 1, 1});
        decal(c, hc, hr, new V3(0, -0.35, 1), 0.055, 0, T_MOUTH, new float[]{0.95F, 0.8F, 0.8F}, 0.9, 0.003);
        cheeks(c, hc, hr, 0.95, -0.2, 0.4, 0.075);
        // gorrito de dormir azul con estrella y pompón (se mece)
        {
            float[] NB = {0.28F, 0.34F, 0.72F}, NB2 = {0.55F, 0.62F, 0.95F};
            double sw = 0.012 * Math.sin(t * 0.06 + ph);
            V3 b0 = new V3(0, 0.575 + bob, 0.16), b1 = new V3(0.01, 0.60 + bob, 0.08), b2 = new V3(0.02 + sw, 0.58 + bob, -0.02), b3 = new V3(0.03 + sw * 2, 0.54 + bob, -0.07);
            curve(c, new V3[]{b0, b1, b2, b3}, new double[]{0.08, 0.06, 0.036, 0.01}, 3, 8, (n, p) -> mix(NB, NB2, ss(0.5, 0.65, p.y)));
            ell(c, b3.add(0, -0.015, 0), new V3(0.024, 0.024, 0.024), 8, 5, flat(WHITE));
            Detail.fur(c, b3.add(0, -0.015, 0), new V3(0.024, 0.024, 0.024), 14, 197, 0.014, 0.01, new V3(0, 0, 0), 0.0, Detail.ALL, flat(WHITE), null);
            Detail.glint(c, new V3(0.005, 0.605 + bob, 0.10), new V3(0.04, 0.03, 0.04), new V3(0, 1, 0.3), 0.03, new float[]{1F, 0.95F, 0.6F}, 0.95);
        }
        c.head = false;
        // luna de sueño
        c.emissive = true;
        glow(c, new V3(0, 0.36, 0), 1.0, new float[]{0.75F, 0.8F, 1F}, 0.06 + 0.02 * Math.sin(t * 0.08 + ph));
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 17. ERIZO

    private static final float[] HE_B = {0.52F, 0.36F, 0.30F}, HE_S = {0.34F, 0.24F, 0.26F}, HE_F = {1F, 0.88F, 0.74F};

    static void hedgehog(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.02 * w;
        c.pivot = new V3(0, 0.15, 0);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double p = c.gait * 1.2 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                ell(c, new V3(sd * 0.09, 0.035 + Math.max(0, Math.cos(p)) * 0.025 * w, (fb == 0 ? 0.11 : -0.1) + Math.sin(p) * 0.04 * w), new V3(0.042, 0.035, 0.065), 8, 5, flat(HE_F));
            }
        }
        ell(c, new V3(0, 0.19 + bob, -0.03), new V3(0.20, 0.17, 0.24), 14, 10, (n, p) -> HE_S);
        // barriguita suave: se hincha al respirar
        ell(c, new V3(0, 0.12 + bob, 0.08), new V3(0.13 + 0.004 * c.breath, 0.09, 0.15), 10, 7, flat(HE_F));
        Detail.fur(c, new V3(0, 0.12 + bob, 0.08), new V3(0.13, 0.09, 0.15), 60, 201, 0.03, 0.013, new V3(0, -0.5, 0.3), 0.7, d -> ss(0.1, -0.5, d.y), flat(HE_F), null);
        // púas
        int count = (int) Math.round(96 * (0.45 + 0.55 * c.lod));
        double bristle = 1 + 0.35 * c.joy + 0.25 * c.shake;
        for (int i = 0; i < count; i++) {
            double f = (i + 0.5) / count;
            double y = 1 - f * 1.55;
            double rr = Math.sqrt(Math.max(0, 1 - y * y));
            double a = i * 2.399963;
            V3 dir = new V3(Math.cos(a) * rr, Math.max(0.05, y), Math.sin(a) * rr * 1.2 - 0.30);
            V3 ce = new V3(0, 0.19 + bob, -0.03);
            V3 base = surf(ce, new V3(0.20, 0.17, 0.24), dir);
            V3 tip = base.add(dir.norm().mul((0.095 + 0.02 * Math.sin(i * 3.1)) * bristle));
            cone(c, base.sub(dir.norm().mul(0.012)), tip, 0.024, 0.003, 4, (n, p) -> mix(HE_S, HE_B, ss(0.0, 0.1, Math.hypot(p.x, p.z) * 0.4 + p.y * 0.3)));
        }
        // cara
        headLook(c, 0.10, 0);
        c.hpv = new V3(0, 0.20, 0.17);
        V3 hc = new V3(0, 0.19 + bob, 0.21), hr = new V3(0.115, 0.105, 0.125);
        ell(c, hc, hr, 14, 10, flat(HE_F));
        ell(c, new V3(0, 0.175 + bob, 0.33), new V3(0.05, 0.045, 0.07), 10, 7, flat(HE_F));
        double nt = Math.sin(t * 0.5 + ph) * 0.004;
        ell(c, new V3(0, 0.185 + bob + nt, 0.395), new V3(0.018, 0.015, 0.014), 8, 5, flat(BLACK));
        Detail.glint(c, new V3(0, 0.192 + bob + nt, 0.402), new V3(0.018, 0.015, 0.014), new V3(0, 0.7, 1), 0.016, WHITE, 0.9);
        Detail.fur(c, hc, hr, 60, 202, 0.022, 0.011, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.3 ? 1 : 0, flat(HE_F), null);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.04, 0.17 + bob + k * 0.012, 0.375), new V3(sd * 0.14, 0.175 + bob + k * 0.03, 0.34), -0.008, 0.0028, new float[]{1F, 0.95F, 0.85F});
            }
            Detail.brow(c, hc, hr, sd, 0.62, 0.44, 0.72, 0.05, sd * 0.4, HE_B);
        }
        // una manzanita roja pinchada en las púas, con su hojita
        {
            V3 ap = new V3(0.07, 0.395 + bob, -0.06);
            ell(c, ap, new V3(0.036, 0.034, 0.036), 10, 7, (n, p) -> mix(new float[]{0.80F, 0.12F, 0.14F}, new float[]{1F, 0.4F, 0.35F}, ss(0.2, 1.0, n.y) * 0.5));
            Detail.leaf(c, ap.add(0.01, 0.034, 0), new V3(0.03, 0.01, 0.014), new V3(1, 0.4, 0), 0.03, 0.4 + 0.1 * Math.sin(t * 0.08 + ph), new float[]{0.4F, 0.8F, 0.35F});
            Detail.glint(c, ap, new V3(0.036, 0.034, 0.036), new V3(-0.4, 0.8, 0.6), 0.02, WHITE, 0.85);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.09, 0.31 + bob, 0.15), new V3(0.04, 0.05, 0.022), 0, 0, -sd * 0.2 + sd * 0.4 * Detail.flick(c, sd > 0 ? 19 : 20), 8, 6, flat(HE_F));
        }
        eyes(c, hc, hr, 0.62, 0.30, 0.72, 0.10, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.15, 0.30, 0.075);
        c.head = false;
    }

    // ------------------------------------------------------------------ 18. AJOLOTE

    private static final float[] AX_P = {1F, 0.72F, 0.80F}, AX_P2 = {1F, 0.88F, 0.90F}, AX_R = {1F, 0.38F, 0.55F};

    static void axolotl(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        c.pivot = new V3(0, 0.15, 0);
        double sway = Math.sin(c.gait * 0.7) * 0.05 * w + Math.sin(t * 0.08 + ph) * 0.015;
        // cuerpo y cola ondulante
        int n = 7;
        V3[] cp = new V3[n];
        double[] rd = new double[n];
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            cp[i] = new V3(Math.sin(c.gait * 0.7 - f * 3.2 + t * 0.03) * 0.045 * (0.4 + w) * f + sway * f * 0.5, 0.13 + (f > 0.55 ? (f - 0.55) * 0.06 : 0), 0.18 - f * 0.62);
            rd[i] = 0.105 * (1 - f * 0.55) * (i == n - 1 ? 0.4 : 1) + 0.02;
        }
        curve(c, cp, rd, 3, 10, (nn, p) -> mix(AX_P, AX_P2, ss(0.0, -1.0, nn.y)));
        // cresta dorsal de aleta suave recorriendo el lomo y la cola
        for (int i = 1; i < n; i++) {
            V3 a = cp[i - 1].add(0, rd[i - 1] * 0.8, 0), b = cp[i].add(0, rd[i] * 0.8, 0);
            double hh = 0.05 * (1 - i / (double) n) + 0.02;
            Detail.kite(c, a.lerp(b, 0.5), a.lerp(b, 0.5).add(0, hh + 0.01 * Math.sin(t * 0.1 + i + ph), 0), new V3(0, 0, 1), 0.035, new V3(1, 0, 0), AX_P2, AX_R);
        }
        // pecas rosadas por el cuerpo
        for (int k = 0; k < 18; k++) {
            V3 d = Detail.sph(331, k);
            if (d.y < 0.1) d = new V3(d.x, d.y + 0.5, d.z);
            double f = 0.1 + 0.8 * Detail.rnd(331, k, 5);
            int idx = Math.min(n - 2, (int) (f * (n - 1)));
            Detail.spot(c, cp[idx], new V3(rd[idx], rd[idx], 0.09), d, 0.016 + 0.01 * Detail.rnd(331, k, 6), mix(AX_R, AX_P, 0.35));
        }
        // burbujitas que suben (traslúcidas)
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.25) % 1.0);
            V3 bp = new V3(Math.sin(k * 2.1 + u * 7) * 0.04, 0.28 + u * 0.4, 0.34 + Math.cos(k * 1.3) * 0.03);
            c.blend = Sink.NORMAL; c.alpha = 0.35;
            ell(c, bp, new V3(0.016, 0.016, 0.016), 8, 5, flat(new float[]{0.85F, 0.95F, 1F}));
            c.alpha = 1; c.blend = Sink.SOLID;
            Detail.glint(c, bp, new V3(0.016, 0.016, 0.016), new V3(-0.3, 0.8, 0.6), 0.012, WHITE, 0.9);
        }
        // aleta de la cola
        ell(c, new V3(cp[n - 1].x, 0.17, cp[n - 1].z - 0.02), new V3(0.012, 0.085, 0.16), 0, 0, 0, 8, 6, flat(AX_P2));
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double p = c.gait * 0.7 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                V3 a = new V3(sd * 0.09, 0.09, fb == 0 ? 0.10 : -0.08);
                V3 b = new V3(sd * 0.17, 0.025, a.z + Math.sin(p) * 0.04 * w + 0.03);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.032, 0.026, 0.024}, 6, flat(AX_P));
                ell(c, b.add(sd * 0.01, 0, 0.02), new V3(0.04, 0.012, 0.04), 8, 4, flat(AX_P2));
                for (int k = -1; k <= 1; k++) {
                    ell(c, b.add(sd * 0.01 + k * 0.016, 0.0, 0.06), new V3(0.008, 0.007, 0.014), 5, 4, flat(AX_P2));
                }
            }
        }
        // cabeza ancha
        headLook(c, 0.10, 0);
        c.hpv = new V3(0, 0.17, 0.2);
        V3 hc = new V3(0, 0.19, 0.27), hr = new V3(0.17, 0.115, 0.14);
        ell(c, hc, hr, 16, 10, (nn, p) -> mix(AX_P, AX_P2, ss(0.3, -0.9, nn.y) * 0.6));
        // branquias en plumas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                double a = 0.35 + k * 0.38;
                double fl = Math.sin(t * 0.14 + ph + k * 1.3 + sd) * 0.025;
                V3 b0 = new V3(sd * 0.145, 0.22 + (k - 1) * 0.04, 0.22);
                V3 b1 = b0.add(sd * (0.06 + 0.02 * k), 0.03 + k * 0.045, -0.02 + fl);
                V3 b2 = b1.add(sd * (0.05 + fl), 0.05 + k * 0.02, -0.03);
                curve(c, new V3[]{b0, b1, b2}, new double[]{0.014, 0.011, 0.004}, 2, 5, flat(AX_R));
                for (int q = 0; q < 2; q++) {
                    V3 tip = b1.add(sd * (0.035 + q * 0.01), 0.04 - q * 0.02, 0.02 * (q - 0.5) + fl);
                    ell(c, tip, new V3(0.02, 0.03, 0.01), 0, 0, -sd * 0.5, 6, 4, flat(mix(AX_R, WHITE, 0.25)));
                }
            }
        }
        eyes(c, hc, hr, 0.70, 0.25, 0.65, 0.09, T_BEAD, new float[]{1, 1, 1});
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.5), 0.04, WHITE, 0.7);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.70, 0.40, 0.58, 0.05, sd * 0.3, AX_R);
            for (int k = 0; k < 3; k++) {
                Detail.spot(c, hc, hr, new V3(sd * (0.95 - k * 0.1), 0.15 - k * 0.12, 0.3 + k * 0.1), 0.02, mix(AX_R, AX_P, 0.3));
            }
        }
        // corona de florecitas entre las branquias
        for (int k = 0; k < 5; k++) {
            double a = k / 4.0 * Math.PI - Math.PI / 2;
            V3 fp = new V3(Math.sin(a) * 0.09, 0.298 + 0.015 * Math.cos(a), 0.22);
            float[] fc = k % 2 == 0 ? new float[]{1F, 1F, 0.9F} : new float[]{0.7F, 0.85F, 1F};
            ell(c, fp, new V3(0.016, 0.016, 0.016), 6, 4, flat(fc));
            ell(c, fp.add(0, 0.004, 0.012), new V3(0.007, 0.007, 0.007), 4, 3, flat(new float[]{1F, 0.85F, 0.3F}));
        }
        decal(c, hc, hr, new V3(0, -0.30, 1), 0.14, 0, T_MOUTH, new float[]{0.55F, 0.25F, 0.3F}, 0.9, 0.003);
        cheeks(c, hc, hr, 0.92, -0.25, 0.42, 0.08);
        c.head = false;
    }

    // ------------------------------------------------------------------ 19. MAPACHE

    private static final float[] RA_G = {0.62F, 0.62F, 0.70F}, RA_G2 = {0.80F, 0.80F, 0.86F}, RA_K = {0.20F, 0.19F, 0.24F};

    static void raccoon(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w;
        c.pivot = new V3(0, 0.3, 0);
        // cola anillada
        V3[] cp = new V3[5];
        double[] rd = {0.055, 0.075, 0.08, 0.07, 0.035};
        for (int i = 0; i < 5; i++) {
            double f = i / 4.0;
            cp[i] = new V3(Math.sin(t * 0.1 + ph - f * 2) * 0.05 * f, 0.26 + bob + f * 0.14, -0.20 - 0.26 * f);
        }
        curve(c, cp, rd, 3, 9, (n, p) -> mix(RA_G2, RA_K, ss(0.0, 0.1, Math.sin(p.z * 34))));
        Detail.furTube(c, cp, rd, 90, 141, 0.04, 0.022, 0.8, (n, p) -> mix(RA_G2, RA_K, ss(0.0, 0.1, Math.sin(p.z * 34))), null);
        legs4(c, 0.08, 0.12, -0.13, 0.20 + bob, 0.032, 0.03, 0.05, flat(RA_K), flat(RA_K));
        V3 bc = new V3(0, 0.29 + bob, -0.02), br = new V3(0.15, 0.15, 0.22);
        Paint bp = (n, p) -> mix(RA_G, RA_G2, ss(0.1, 0.9, n.y) * 0.4 + ss(0.0, -1.0, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 220, 142, 0.028, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        Detail.fur(c, bc, br, 40, 143, 0.05, 0.018, new V3(0, -1, 0.2), 0.7, d -> ss(0.4, 0.8, d.z) * ss(0.6, -0.3, d.y), bp, RA_G2);
        // cabeza
        headLook(c, 0.13, 0.02);
        c.hpv = new V3(0, 0.40, 0.14);
        V3 hc = new V3(0, 0.46 + bob, 0.17), hr = new V3(0.17, 0.14, 0.145);
        Paint hp = (n, p) -> mix(RA_G, RA_G2, ss(-0.2, 0.8, n.z) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 80, 144, 0.03, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.6 ? 1 : 0, hp, null);
        // patillas esponjosas a los lados de la cara
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 6; k++) {
                V3 bs = surf(hc, hr, new V3(sd * (0.92 - k * 0.04), -0.28 + k * 0.05, 0.28 - k * 0.1));
                Detail.kite(c, bs, bs.add(sd * (0.035 + k * 0.004), -0.035, -0.04), new V3(0, 0, 1), 0.015, new V3(sd, 0, 0), RA_G, RA_G2);
            }
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.05, 0.43 + bob + k * 0.012, 0.335), new V3(sd * 0.17, 0.435 + bob + k * 0.03, 0.29), -0.01, 0.0028, new float[]{0.9F, 0.9F, 0.95F});
            }
            // cejas blancas sobre el antifaz
            Detail.brow(c, hc, hr, sd, 0.50, 0.50, 0.70, 0.06, sd * 0.5, RA_G2);
        }
        Detail.glint(c, new V3(0, 0.447 + bob, 0.35), new V3(0.018, 0.014, 0.013), new V3(0, 0.7, 1), 0.018, WHITE, 0.9);
        // antifaz
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.085, 0.47 + bob, 0.27), new V3(0.085, 0.056, 0.05), 0, 0, sd * 0.25, 12, 8, flat(RA_K));
        }
        ell(c, new V3(0, 0.425 + bob, 0.29), new V3(0.05, 0.043, 0.06), 10, 7, flat(RA_G2));
        ell(c, new V3(0, 0.44 + bob, 0.345), new V3(0.018, 0.014, 0.013), 8, 5, flat(BLACK));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.115, 0.59 + bob, 0.15), new V3(0.05, 0.07, 0.03), 0, 0, -sd * 0.2, 8, 6, flat(RA_G));
            ell(c, new V3(sd * 0.115, 0.59 + bob, 0.17), new V3(0.03, 0.045, 0.014), 0, 0, -sd * 0.2, 8, 5, flat(RA_K));
        }
        eyes(c, hc, hr, 0.50, 0.30, 0.76, 0.10, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.25, 0.40, 0.075);
        c.head = false;
        // gema brillante entre las patitas
        double fl = Math.sin(t * 0.1 + ph) * 0.01;
        V3 gem = new V3(0, 0.30 + bob + fl, 0.30);
        c.emissive = true;
        ell(c, gem, new V3(0.035, 0.05, 0.035), 6, 4, (n, p) -> mix(new float[]{0.5F, 0.95F, 1F}, WHITE, ss(0, 1, n.y) * 0.7));
        glow(c, gem, 0.35, new float[]{0.5F, 0.9F, 1F}, 0.25);
        c.emissive = false;
        // dos manitas que hacen rodar la gema (movimiento de lavarse las manos)
        double rub = Math.sin(t * 0.22 + ph);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 hand = new V3(sd * 0.055, 0.285 + bob + fl, 0.285 + rub * 0.012 * sd);
            ell(c, hand, new V3(0.027, 0.03, 0.03), 8, 5, flat(RA_K));
            for (int k = -1; k <= 1; k++) {
                ell(c, hand.add(k * 0.012, -0.012, 0.024), new V3(0.007, 0.007, 0.011), 5, 4, flat(RA_K));
            }
        }
    }

    // ------------------------------------------------------------------ 20. CIERVO LUNAR

    private static final float[] DE_B = {0.80F, 0.60F, 0.42F}, DE_L = {1F, 0.94F, 0.82F}, DE_BL = {0.60F, 0.78F, 1F};

    static void deer(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w;
        c.pivot = new V3(0, 0.45, 0);
        // cola
        double tf = Detail.flick(c, 21) * 0.5 + Math.sin(t * 0.5 + ph) * 0.1 * w;
        V3 tc = new V3(0, 0.55 + bob, -0.27);
        ell(c, tc, new V3(0.04, 0.05, 0.05), 0, 0, tf * 0.6, 8, 6, flat(DE_L));
        Detail.fur(c, tc, new V3(0.04, 0.05, 0.05), 14, 211, 0.03, 0.014, new V3(0, 0.6, -1), 0.8, Detail.ALL, flat(DE_L), null);
        legs4(c, 0.085, 0.18, -0.19, 0.48 + bob, 0.038, 0.03, 0.055, (n, p) -> mix(DE_B, DE_L, ss(0.15, 0.04, p.y)), flat(new float[]{0.35F, 0.28F, 0.28F}));
        V3 bc = new V3(0, 0.56 + bob, -0.02), br = new V3(0.14, 0.14, 0.30);
        Paint bp = (n, p) -> {
            float[] b = mix(DE_B, DE_L, ss(0.0, -0.9, n.y));
            double spot = ss(0.88, 0.94, Math.sin(p.x * 70 + 1) * Math.sin(p.z * 60) * (n.y > 0.1 ? 1 : 0));
            return mix(b, DE_L, spot * 0.9);
        };
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 200, 212, 0.028, 0.013, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, DE_L);
        // las motitas del lomo brillan suave como estrellas
        for (int k = 0; k < 5; k++) {
            Detail.twinkle(c, new V3(Math.sin(k * 2.1) * 0.08, 0.70 + bob, -0.12 + k * 0.07), 0.016, new float[]{1F, 0.97F, 0.8F}, t, ph + k * 1.3, 0.09);
        }
        // cuello
        V3[] nk = {new V3(0, 0.62 + bob, 0.17), new V3(0, 0.72 + bob, 0.23), new V3(0, 0.80 + bob, 0.26)};
        tube(c, nk, new double[]{0.085, 0.07, 0.065}, 10, (n, p) -> mix(DE_B, DE_L, ss(0.0, -1.0, n.y) * 0.5));
        Detail.furTube(c, nk, new double[]{0.085, 0.07, 0.065}, 50, 213, 0.03, 0.014, 0.4, (n, p) -> mix(DE_B, DE_L, ss(0.0, -1.0, n.y) * 0.5), DE_L);
        // guirnalda de florecitas alrededor del cuello
        {
            float[][] fc = {{1F, 0.62F, 0.78F}, {1F, 1F, 0.95F}, {0.62F, 0.78F, 1F}, {1F, 0.88F, 0.45F}};
            for (int k = 0; k < 10; k++) {
                double a = k / 10.0 * Math.PI * 2;
                V3 fp = new V3(Math.cos(a) * 0.082, 0.715 + bob + Math.sin(k * 1.7) * 0.004, 0.225 + Math.sin(a) * 0.082);
                float[] col = fc[k % 4];
                ell(c, fp, new V3(0.016, 0.016, 0.016), 6, 4, flat(col));
                ell(c, fp.add(Math.cos(a) * 0.01, 0.002, Math.sin(a) * 0.01), new V3(0.007, 0.007, 0.007), 4, 3, flat(new float[]{1F, 0.85F, 0.3F}));
                Detail.leaf(c, fp.add(Math.cos(a + 0.5) * 0.012, -0.004, Math.sin(a + 0.5) * 0.012), new V3(0.012, 0.005, 0.008), new V3(Math.cos(a), 0, Math.sin(a)), 0.016, a, new float[]{0.4F, 0.78F, 0.4F});
            }
        }
        // cabeza
        headLook(c, 0.12, 0);
        c.hpv = new V3(0, 0.78, 0.26);
        V3 hc = new V3(0, 0.88 + bob, 0.30), hr = new V3(0.10, 0.095, 0.125);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(DE_B, DE_L, ss(0.3, -0.8, n.y) * 0.5));
        ell(c, new V3(0, 0.845 + bob, 0.41), new V3(0.055, 0.050, 0.08), 10, 7, flat(DE_L));
        double nt = Math.sin(t * 0.45 + ph) * 0.003;
        ell(c, new V3(0, 0.855 + bob + nt, 0.485), new V3(0.020, 0.016, 0.014), 8, 5, flat(BLACK));
        Detail.glint(c, new V3(0, 0.862 + bob + nt, 0.492), new V3(0.02, 0.016, 0.014), new V3(0, 0.7, 1), 0.016, WHITE, 0.9);
        Detail.fur(c, hc, hr, 60, 215, 0.022, 0.011, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, (n, p) -> mix(DE_B, DE_L, ss(0.3, -0.8, n.y) * 0.5), DE_L);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 22 : 23);
            ell(c, new V3(sd * 0.10, 0.94 + bob, 0.24), new V3(0.075, 0.035, 0.025), 0, 0, sd * (0.5 - 0.6 * ef), 10, 6, flat(DE_B));
            ell(c, new V3(sd * 0.105, 0.94 + bob, 0.255), new V3(0.05, 0.02, 0.012), 0, 0, sd * (0.5 - 0.6 * ef), 8, 5, flat(PINK));
            Detail.brow(c, hc, hr, sd, 0.62, 0.40, 0.72, 0.045, sd * 0.3, DE_L);
            Detail.fur(c, new V3(sd * 0.10, 0.94 + bob, 0.24), new V3(0.075, 0.035, 0.025), 8, 214 + sd, 0.022, 0.011, new V3(sd, 0.3, 0), 0.7, d -> d.y > 0 ? 0 : 1, flat(DE_L), null);
        }
        // astas brillantes
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = new V3(sd * 0.05, 0.95 + bob, 0.27);
            V3 b1 = b0.add(sd * 0.05, 0.09, -0.02);
            V3 b2 = b1.add(sd * 0.03, 0.09, -0.03);
            V3 b3 = b2.add(sd * 0.02, 0.08, 0.0);
            curve(c, new V3[]{b0, b1, b2, b3}, new double[]{0.016, 0.014, 0.012, 0.006}, 3, 6, (n, p) -> mix(DE_BL, WHITE, ss(0.95, 1.3, p.y) * 0.8));
            for (int k = 0; k < 2; k++) {
                V3 s0 = k == 0 ? b1 : b2;
                V3 s1 = s0.add(sd * 0.07, 0.08, 0.02);
                V3 s2 = s1.add(sd * 0.03, 0.06, 0.01);
                curve(c, new V3[]{s0, s1, s2}, new double[]{0.011, 0.009, 0.004}, 2, 5, (n, p) -> mix(DE_BL, WHITE, 0.6));
                glow(c, s2, 0.12, DE_BL, 0.35);
            }
        }
        c.emissive = false;
        eyes(c, hc, hr, 0.62, 0.15, 0.72, 0.095, T_EYE, new float[]{0.6F, 0.8F, 1F});
        cheeks(c, hc, hr, 0.95, -0.3, 0.40, 0.065);
        // luna en la frente
        c.emissive = true;
        glow(c, new V3(0, 0.95 + bob, 0.40), 0.12, new float[]{1F, 0.95F, 0.6F}, 0.7);
        c.emissive = false;
        c.head = false;
        // motas de luz de luna
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            double a = t * 0.04 + k * 1.57;
            glowStar(c, new V3(Math.cos(a) * 0.3, 0.4 + 0.3 * Math.sin(a * 1.3 + k), Math.sin(a) * 0.3), 0.03, DE_BL, t, ph + k);
        }
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 21. BALLENA ESTELAR (flota)

    private static final float[] WH_B = {0.30F, 0.45F, 0.95F}, WH_L = {0.72F, 0.84F, 1F};
    private static final float[] WH_FOLD = {0.12F, 0.22F, 0.62F};

    static void whale(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.07 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double sw = Math.sin(t * 0.14 + ph);
        glow(c, new V3(0, 0.40 + bob, 0), 1.7, new float[]{0.45F, 0.6F, 1F}, 0.10);
        // cuerpo (se arma con un tubo que ondula)
        int n = 7;
        V3[] cp = new V3[n];
        double[] rd = {0.10, 0.22, 0.27, 0.25, 0.17, 0.09, 0.05};
        for (int i = 0; i < n; i++) {
            double f = i / (double) (n - 1);
            cp[i] = new V3(sw * 0.07 * f * f, 0.40 + bob + 0.1 * f * f - 0.02 * Math.cos(f * 3), 0.42 - f * 0.95);
        }
        curve(c, cp, rd, 3, 14, (nn, p) -> {
            float[] b = mix(WH_B, WH_L, ss(0.1, -0.9, nn.y));
            double star = ss(0.86, 0.95, Math.sin(p.x * 61 + p.z * 47) * Math.sin(p.y * 53 + p.z * 31)) * (nn.y > -0.1 ? 1 : 0);
            return mix(b, WHITE, star);
        });
        // cabeza redondeada
        ell(c, new V3(0, 0.42 + bob, 0.44), new V3(0.17, 0.19, 0.20), 14, 10, (nn, p) -> mix(WH_B, WH_L, ss(0.1, -0.9, nn.y)));
        // aletas pectorales
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.27, 0.27 + bob, 0.22), new V3(0.13, 0.025, 0.07), 0, 0, -sd * (0.5 + sw * 0.3), 10, 6, flat(WH_B));
        }
        // aleta de cola
        V3 tail = cp[n - 1];
        double fl = sw * 0.35;
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, tail.add(sd * 0.11, 0.07, -0.05), new V3(0.13, 0.014, 0.065), 0, 0, -sd * (0.1 + fl), 10, 6, flat(WH_B));
        }
        // ojos y sonrisa
        V3 hc = new V3(0, 0.42 + bob, 0.44), hr = new V3(0.17, 0.19, 0.20);
        eyes(c, hc, hr, 0.82, 0.22, 0.52, 0.12, T_BEAD, new float[]{1, 1, 1});
        decal(c, hc, hr, new V3(0, -0.25, 1), 0.14, 0, T_MOUTH, new float[]{0.2F, 0.25F, 0.5F}, 0.9, 0.003);
        cheeks(c, hc, hr, 0.92, -0.18, 0.35, 0.09);
        // pliegues del vientre: cinco líneas finas más oscuras a lo largo del cuerpo
        double[][] rr0 = new double[1][];
        V3[] spl = spline(cp, rd, 3, rr0);
        for (int k = -2; k <= 2; k++) {
            V3[] line = new V3[spl.length - 6];
            double[] lr = new double[line.length];
            for (int i = 0; i < line.length; i++) {
                int j = i + 2;
                double ang = Math.PI * 1.5 + k * 0.30;
                line[i] = spl[j].add(Math.cos(ang) * rr0[0][j] * 0.985, Math.sin(ang) * rr0[0][j] * 0.985, 0);
                lr[i] = 0.0075;
            }
            tube(c, line, lr, 4, flat(WH_FOLD));
        }
        // constelación en el lomo: puntitos claros y destellos
        for (int k = 0; k < 11; k++) {
            int j = 3 + (int) (Detail.rnd(5, k, 1) * (spl.length - 8));
            double ang = Math.PI * (0.18 + 0.64 * Detail.rnd(5, k, 2));
            if (k % 2 == 0) {
                ang = Math.PI - ang;
            }
            V3 d = new V3(Math.cos(ang), Math.sin(ang), 0);
            V3 cr = new V3(rr0[0][j], rr0[0][j], rr0[0][j]);
            if (k % 3 == 0) {
                Detail.glint(c, spl[j], cr, d, 0.10, new float[]{1F, 1F, 0.85F}, 1.0);
            } else {
                Detail.spot(c, spl[j], cr, d, 0.045 + 0.015 * (k % 3), new float[]{0.80F, 0.90F, 1F});
            }
        }
        // surco del hocico: tres perlitas a cada lado y un respiradero
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                ell(c, surf(hc, hr, new V3(sd * (0.55 + k * 0.1), -0.45 + k * 0.1, 0.8)), new V3(0.011, 0.011, 0.011), 6, 4, flat(WH_L));
            }
        }
        ell(c, surf(hc, hr, new V3(0, 1, 0.2)).add(0, 0.002, 0), new V3(0.020, 0.005, 0.014), 8, 4, flat(WH_FOLD));
        // pestañas y cejitas
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.74, 0.46, 0.55, 0.06, sd * 0.7, WH_FOLD);
        }
        // aleta de cola con muesca: dos lóbulos más
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, tail.add(sd * 0.17, 0.085, -0.075), new V3(0.06, 0.012, 0.04), 0, 0, -sd * (0.25 + fl), 8, 5, flat(WH_L));
        }
        // chorro de estrellitas
        c.emissive = true;
        for (int k = 0; k < 5; k++) {
            double f = ((t * 0.02 + k / 5.0) % 1);
            glowStar(c, new V3(Math.sin(k * 2.3) * 0.05, 0.66 + bob + f * 0.28, 0.40 + Math.cos(k) * 0.03), 0.045 * (1 - f * 0.5), WH_L, t, ph + k);
        }
        c.emissive = false;
    }
}
