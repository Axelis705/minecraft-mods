package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Rework de mascotas, lote 4 (épicas): hipocampo real, osito ángel, panda rojo monje, alce aurora, cerbero cachorro,
 * quetzal dorado y narval estelar.
 */
final class Pets11 {
    private Pets11() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    private static float[] aur(double u) {
        // aurora: verde -> turquesa -> violeta -> rosa
        u = Math.max(0, Math.min(1, u));
        float[] g = hex(0x5CFFB0), t = hex(0x4FD8FF), v = hex(0xA878FF), p = hex(0xFF8AE0);
        if (u < 0.34) {
            return mix(g, t, u / 0.34);
        }
        if (u < 0.67) {
            return mix(t, v, (u - 0.34) / 0.33);
        }
        return mix(v, p, (u - 0.67) / 0.33);
    }

    // ------------------------------------------------------------------ 4. HIPOCAMPO REAL

    private static final float[] HS_B = hex(0x2FB8C8), HS_B2 = hex(0x7ADCE8), HS_PE = hex(0xFFF2E6), HS_GOLD = hex(0xF6C84A), HS_GOLD2 = hex(0xFFE590), HS_FIN = hex(0xA8F0F8),
            HS_PK = hex(0xFF9AC0);

    static void seahorse(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.1 + ph) * 0.03;
        c.pivot = new V3(0, 0.45, 0);
        double sw = Math.sin(t * 0.12 + ph) * 0.035;
        // cuerpo curvo: cabeza arriba, cola enroscada abajo
        V3[] sp = {new V3(0, 0.62 + bob, 0.04), new V3(0, 0.50 + bob, 0.07), new V3(0, 0.38 + bob, 0.06), new V3(sw * 0.3, 0.27 + bob, 0.0), new V3(sw * 0.6, 0.19 + bob, -0.1),
                new V3(sw, 0.16 + bob, -0.22), new V3(sw * 1.2, 0.22 + bob, -0.3), new V3(sw * 1.2, 0.30 + bob, -0.25), new V3(sw, 0.30 + bob, -0.17)};
        double[] rd = {0.1, 0.125, 0.135, 0.1, 0.075, 0.055, 0.04, 0.026, 0.012};
        Paint bp = (n, p) -> {
            double band = Math.sin(p.y * 90 + p.z * 20) > 0.55 ? 0.7 : 0;
            double belly = ss(0.0, 0.7, n.z) * (p.y > 0.25 + bob ? 1 : 0.4);
            return mix(mix(HS_B, HS_B2, ss(-0.5, 0.8, n.x) * 0.4), HS_PE, belly * 0.6 + band * 0.0);
        };
        curve(c, sp, rd, 4, 12, bp);
        // anillitos dorados
        for (int k = 1; k < 6; k++) {
            V3 a = sp[k];
            ell(c, a.add(0, 0, 0.0), new V3(rd[k] * 1.03, 0.012, rd[k] * 1.03), 12, 4, flat(HS_GOLD));
        }
        // aleta dorsal
        for (int k = 0; k < 6; k++) {
            double ff = Math.sin(t * 0.3 + ph + k * 0.5) * 0.02;
            V3 a = sp[1].add(0, -0.03 * k, -0.11 - 0.005 * k);
            Detail.kite(c, a, a.add(0, -0.01, -0.09 - ff), new V3(0, 1, 0), 0.026, new V3(1, 0, 0), HS_FIN, HS_PK);
        }
        // aletitas laterales
        for (int sd = -1; sd <= 1; sd += 2) {
            double ff = Math.sin(t * 0.4 + ph + (sd > 0 ? 0 : 1)) * 0.03;
            V3 a = new V3(sd * 0.1, 0.50 + bob, 0.0);
            Detail.kite(c, a, a.add(sd * (0.1 + ff), 0.05, -0.05), new V3(0, 1, 0), 0.05, new V3(0, 0, 1), HS_FIN, HS_PK);
        }
        // cabeza y hocico largo
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.62 + bob, 0.06);
        V3 hc = new V3(0, 0.67 + bob, 0.08), hr = new V3(0.13, 0.115, 0.115);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(HS_B, HS_B2, ss(-0.3, 0.9, n.y) * 0.4));
        V3[] sn = {new V3(0, 0.66 + bob, 0.15), new V3(0, 0.655 + bob, 0.23), new V3(0, 0.64 + bob, 0.30)};
        tube(c, sn, new double[]{0.055, 0.045, 0.034}, 8, flat(HS_B2));
        ell(c, new V3(0, 0.638 + bob, 0.31), new V3(0.034, 0.03, 0.026), 8, 6, flat(HS_B2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.012, 0.645 + bob, 0.335), new V3(0.006, 0.006, 0.005), 4, 3, flat(hex(0x1A6A78)));
        }
        eyes(c, hc, hr, 0.62, 0.22, 0.72, 0.11, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.15, 0.40, 0.075);
        // corona dorada con perla
        V3 cb = hc.add(0, 0.115, 0.0);
        ell(c, cb, new V3(0.1, 0.018, 0.09), 14, 4, flat(HS_GOLD));
        for (int k = 0; k < 5; k++) {
            double a = (k - 2) * 0.55;
            V3 b0 = cb.add(Math.sin(a) * 0.085, 0.01, Math.cos(a) * 0.075 - 0.0);
            Detail.kite(c, b0, b0.add(Math.sin(a) * 0.012, 0.055 - Math.abs(k - 2) * 0.012, 0.0), new V3(Math.cos(a), 0, -Math.sin(a)), 0.02, new V3(0, 0, 1), HS_GOLD, HS_GOLD2);
        }
        ell(c, cb.add(0, 0.03, 0.08), new V3(0.016, 0.016, 0.016), 8, 6, flat(WHITE));
        Detail.glint(c, cb.add(0, 0.03, 0.08), new V3(0.016, 0.016, 0.016), new V3(-0.4, 0.5, 1), 0.012, WHITE, 0.9);
        // crestas laterales de la cabeza
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.kite(c, hc.add(sd * 0.09, 0.05, -0.06), hc.add(sd * 0.15, 0.09, -0.12), new V3(0, 1, 0), 0.03, new V3(1, 0, 0), HS_FIN, HS_PK);
        }
        c.head = false;
        // burbujas
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.011 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.2 + u * 3) * 0.2, 0.25 + u * 0.6 + bob, 0.1 + Math.cos(k * 1.9) * 0.15), 0.022 * (1 - u * 0.3), new float[]{0.8F, 0.95F, 1F}, t, ph + k, 0.22);
        }
    }

    // ------------------------------------------------------------------ 8. OSITO ÁNGEL

    private static final float[] AB_F = hex(0xFFF4E4), AB_F2 = hex(0xF0DCC0), AB_PK = hex(0xFFB0C0), AB_GOLD = hex(0xFFE08A), AB_W = hex(0xFFFFFF), AB_W2 = hex(0xD8E8FF),
            AB_STAR = hex(0xFFF0A0);

    static void angelBear(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.012 * c.breath + 0.01 * Math.sin(t * 0.08 + ph);
        c.pivot = new V3(0, 0.25, 0);
        // alas con plumas
        double fl = Math.sin(t * 0.14 + ph) * 0.08 + Math.sin(c.gait * 1.2) * 0.06 * w;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.07, 0.36 + bob, -0.1);
            for (int k = 0; k < 6; k++) {
                double a = 0.55 + k * 0.17 + fl * (1 + k * 0.1);
                double len = 0.2 + 0.05 * (1 - Math.abs(k - 2) / 3.0);
                V3 e = sh.add(sd * (0.07 + Math.cos(a) * 0.01 + 0.07 * (6 - k) * 0.4 + 0.04), 0.07 + Math.sin(a) * len * 0.55 + (5 - k) * 0.012, -0.08 - k * 0.02);
                Detail.feather(c, sh, e, new V3(0, 1, 0.2).norm(), 0.085 - k * 0.004, new V3(sd * 0.3, 0.2, 1).norm(), k > 3 ? AB_W2 : AB_W, AB_W2);
            }
            Detail.feather(c, sh, sh.add(sd * 0.12, 0.17 + fl, -0.06), new V3(0, 1, 0.2).norm(), 0.09, new V3(sd * 0.3, 0.2, 1).norm(), AB_W, AB_W);
        }
        legs4(c, 0.075, 0.1, -0.1, 0.17 + bob, 0.04, 0.03, 0.05, (n, p) -> AB_F, (n, p) -> AB_F2);
        // cuerpo redondo
        V3 bc = new V3(0, 0.25 + bob, 0), br = new V3(0.14, 0.14, 0.15);
        Paint bp = (n, p) -> mix(AB_F, AB_F2, ss(0.0, -0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 90, 981, 0.028, 0.016, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        ell(c, bc.add(0, -0.01, 0.075), new V3(0.09, 0.1, 0.07), 12, 8, flat(AB_W));
        // colita pompón
        ell(c, new V3(0, 0.2 + bob, -0.15), new V3(0.04, 0.04, 0.04), 8, 6, flat(AB_F));
        // estrella dorada en el pecho
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5 + Math.PI / 2;
            Detail.kite(c, bc.add(0, 0.0, 0.14), bc.add(Math.cos(a) * 0.045, Math.sin(a) * 0.045, 0.146), new V3(-Math.sin(a), Math.cos(a), 0), 0.016, new V3(0, 0, 1), AB_GOLD, AB_STAR);
        }
        // cabeza grande
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.40, 0.04);
        V3 hc = new V3(0, 0.47 + bob, 0.06), hr = new V3(0.165, 0.145, 0.14);
        Paint hp = (n, p) -> mix(AB_F, AB_F2, ss(0.2, -0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 60, 982, 0.022, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0.0, hp, null);
        V3 sc = new V3(0, 0.435 + bob, 0.18), sr = new V3(0.065, 0.05, 0.06);
        ell(c, sc, sr, 10, 7, flat(AB_W));
        ell(c, sc.add(0, 0.022, 0.05), new V3(0.022, 0.016, 0.014), 8, 5, flat(hex(0x7A5A5A)));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.075, 0, T_MOUTH, new float[]{0.45F, 0.28F, 0.3F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.55, 0.14, 0.82, 0.10, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.86, -0.2, 0.5, 0.08);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 29 : 30) * 0.02;
            V3 ec = new V3(sd * (0.12 + ef), 0.58 + bob, 0.05);
            ell(c, ec, new V3(0.05, 0.05, 0.03), 0, 0, 0, 10, 8, flat(AB_F));
            ell(c, ec.add(0, 0, 0.012), new V3(0.03, 0.03, 0.02), 8, 6, flat(AB_PK));
        }
        // aureola
        double hv = 0.015 * Math.sin(t * 0.09 + ph);
        V3 hl = new V3(0, 0.665 + bob + hv, 0.04);
        V3[] ring = new V3[17];
        for (int i = 0; i <= 16; i++) {
            double a = Math.PI * 2 * i / 16.0;
            ring[i] = hl.add(Math.cos(a) * 0.075, 0.0, Math.sin(a) * 0.075);
        }
        boolean em = c.emissive;
        c.emissive = true;
        double[] rr = new double[17];
        java.util.Arrays.fill(rr, 0.01);
        tube(c, ring, rr, 6, flat(AB_GOLD));
        c.emissive = em;
        c.head = false;
        glow(c, hl, 0.28, new float[]{1F, 0.9F, 0.5F}, 0.5);
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.25, 0.3 + u * 0.45 + bob, Math.cos(k * 1.7) * 0.2), 0.02 * (1 - u * 0.4), new float[]{1F, 0.95F, 0.7F}, t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 9. PANDA ROJO MONJE

    private static final float[] RP_R = hex(0xD8603A), RP_R2 = hex(0xF08A50), RP_CR = hex(0xFFF4E4), RP_DK = hex(0x3A2A2A), RP_RO = hex(0xF2A63A), RP_RO2 = hex(0xFFC870),
            RP_RED = hex(0xB03A3A), RP_BEAD = hex(0x8A5A34);

    static void monkRedPanda(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.25, 0);
        // cola de anillos
        double wag = Math.sin(t * 0.1 + ph) * 0.04 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.20 + bob, -0.14), new V3(wag * 0.5, 0.17 + bob, -0.27), new V3(wag * 1.2, 0.21 + bob, -0.40), new V3(wag * 1.8, 0.28 + bob, -0.48)};
        double[] trd = {0.055, 0.075, 0.075, 0.055};
        Paint tp = (n, p) -> Math.sin(-p.z * 34) > 0.2 ? RP_DK : mix(RP_R, RP_R2, 0.5);
        curve(c, tcp, trd, 4, 8, tp);
        Detail.furTube(c, tcp, trd, 50, 991, 0.035, 0.02, 0.7, tp, null);
        ell(c, tcp[3].add(0, 0, -0.02), new V3(0.05, 0.05, 0.05), 8, 6, flat(RP_CR));
        // pies
        for (int sd = -1; sd <= 1; sd += 2) {
            double phs = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(phs) * 0.06 * w;
            ell(c, new V3(sd * 0.09, 0.035, 0.05 + sw), new V3(0.055, 0.035, 0.085), 10, 6, flat(RP_DK));
        }
        // túnica azafrán
        V3 bc = new V3(0, 0.25 + bob, 0), br = new V3(0.15, 0.17, 0.14);
        Paint rb = (n, p) -> mix(RP_RO, RP_RO2, ss(-0.3, 0.9, n.y) * 0.5 + 0.2 * Math.sin(p.y * 45));
        ell(c, bc, br, 14, 10, rb);
        // falda de la túnica
        ell(c, bc.add(0, -0.1, 0), new V3(0.165, 0.09, 0.155), 14, 8, rb);
        // faja roja y cruce de solapa
        ell(c, bc.add(0, -0.02, 0), new V3(0.158, 0.03, 0.148), 14, 4, flat(RP_RED));
        Detail.kite(c, bc.add(-0.04, 0.12, 0.12), bc.add(0.04, -0.01, 0.152), new V3(0, 0, 1), 0.03, new V3(0, 0, 1), RP_RED, scale(RP_RED, 1.2));
        // mangas anchas y manos juntas con cuentas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.14, 0.31 + bob, 0.0), b = new V3(sd * 0.05, 0.25 + bob, 0.15);
            tube(c, new V3[]{a, a.lerp(b, 0.5).add(sd * 0.01, -0.03, 0.02), b}, new double[]{0.06, 0.055, 0.04}, 8, rb);
            ell(c, b.add(-sd * 0.01, 0.0, 0.03), new V3(0.032, 0.03, 0.03), 8, 6, flat(RP_R));
        }
        // cuentas de oración
        V3[] bd = new V3[9];
        for (int i = 0; i < 9; i++) {
            double a = i / 8.0 * Math.PI;
            bd[i] = new V3(Math.cos(a) * 0.1, 0.34 + bob - Math.sin(a) * 0.1, 0.12 + Math.sin(a) * 0.035);
        }
        for (int i = 0; i < 9; i += 1) {
            ell(c, bd[i], new V3(0.013, 0.013, 0.013), 6, 4, flat(RP_BEAD));
        }
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.40, 0.06);
        V3 hc = new V3(0, 0.49 + bob, 0.08), hr = new V3(0.165, 0.125, 0.13);
        Paint hp = (n, p) -> mix(RP_R, RP_R2, ss(0.0, 0.9, n.y) * 0.6);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 992, 0.03, 0.014, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.6 ? 1 : 0.0, hp, null);
        // máscara blanca
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.075, 0.455 + bob, 0.16), new V3(0.065, 0.06, 0.05), 0, 0, sd * 0.3, 10, 7, flat(RP_CR));
            Detail.spot(c, hc, hr, new V3(sd * 0.62, 0.55, 0.68), 0.05, RP_CR);
        }
        V3 sc = new V3(0, 0.46 + bob, 0.215), sr = new V3(0.06, 0.045, 0.05);
        ell(c, sc, sr, 10, 7, flat(RP_CR));
        ell(c, sc.add(0, 0.018, 0.045), new V3(0.02, 0.015, 0.013), 6, 4, flat(BLACK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.25F, 0.2F}, 0.9, 0.003);
        // ojos sereños (siempre entrecerrados)
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, hc, hr, new V3(sd * 0.5, 0.2, 0.84), 0.1, 0, T_EYE_SHUT, new float[]{0.2F, 0.12F, 0.12F}, 1, 0.004);
        }
        cheeks(c, hc, hr, 0.86, -0.15, 0.5, 0.07);
        // orejas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 31 : 32) * 0.02;
            V3 ec = new V3(sd * (0.12 + ef), 0.58 + bob, 0.04);
            ell(c, ec, new V3(0.055, 0.06, 0.035), 0, 0, sd * 0.25, 10, 8, flat(RP_R));
            ell(c, ec.add(0, 0, 0.012), new V3(0.04, 0.045, 0.024), 8, 6, flat(RP_CR));
        }
        // puntito de sabiduría en la frente
        ell(c, hc.add(0, 0.07, 0.115), new V3(0.016, 0.016, 0.01), 6, 4, flat(RP_RED));
        c.head = false;
        // luz serena
        glow(c, new V3(0, 0.55 + bob, 0.0), 0.5, new float[]{1F, 0.8F, 0.4F}, 0.14);
        double u = ((t * 0.011 + ph) % 1.0);
        Detail.twinkle(c, new V3(Math.sin(u * 6) * 0.1, 0.65 + u * 0.3 + bob, 0.05), 0.02 * (1 - u * 0.4), new float[]{1F, 0.9F, 0.55F}, t, ph, 0.22);
    }

    // ------------------------------------------------------------------ 20. ALCE AURORA

    private static final float[] EL_F = hex(0xC8D8E8), EL_F2 = hex(0xF0F6FF), EL_DK = hex(0x5A6A8A), EL_HOOF = hex(0x3A4260), EL_NOSE = hex(0x3A4A6A);

    static void auroraMoose(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.35, 0);
        tube(c, new V3[]{new V3(0, 0.45 + bob, -0.27), new V3(0, 0.46 + bob, -0.32), new V3(0, 0.43 + bob, -0.35)}, new double[]{0.03, 0.026, 0.01}, 5, flat(EL_F2));
        legs4(c, 0.09, 0.19, -0.2, 0.34 + bob, 0.04, 0.04, 0.05, (n, p) -> mix(EL_F, EL_DK, ss(0.3, 0.05, p.y) * 0.5), flat(EL_HOOF));
        // cuerpo
        V3 bc = new V3(0, 0.42 + bob, -0.02), br = new V3(0.14, 0.14, 0.27);
        Paint bp = (n, p) -> mix(mix(EL_F, EL_F2, ss(0.0, -0.9, n.y) * 0.8), EL_DK, ss(0.4, 0.95, n.y) * 0.25);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 130, 1001, 0.035, 0.018, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, EL_F2);
        // joroba/cruz y estrellitas en el lomo
        ell(c, bc.add(0, 0.09, 0.1), new V3(0.1, 0.07, 0.11), 12, 8, bp);
        float[][] sc = {hex(0x5CFFB0), hex(0xA878FF), hex(0x4FD8FF), hex(0xFF8AE0)};
        for (int k = 0; k < 8; k++) {
            double a = k * 2.4;
            Detail.spot(c, bc, br, new V3(Math.sin(a) * 0.7, 0.8 - 0.1 * (k % 3), Math.cos(a) * 0.7), 0.05, sc[k % 4]);
        }
        // cuello y cabeza
        headLook(c, 0.11, 0.0);
        c.hpv = new V3(0, 0.55, 0.22);
        V3[] nk = {new V3(0, 0.46 + bob, 0.2), new V3(0, 0.55 + bob, 0.27), new V3(0, 0.6 + bob, 0.31)};
        tube(c, nk, new double[]{0.095, 0.085, 0.075}, 9, bp);
        V3 hc = new V3(0, 0.62 + bob, 0.36), hr = new V3(0.09, 0.1, 0.14);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(EL_F, EL_F2, ss(0.0, 0.9, n.y) * 0.5));
        // hocico alargado
        V3 sn = new V3(0, 0.575 + bob, 0.46), sr = new V3(0.07, 0.065, 0.085);
        ell(c, sn, sr, 12, 8, flat(EL_F2));
        ell(c, sn.add(0, 0.015, 0.075), new V3(0.05, 0.035, 0.026), 8, 6, flat(EL_NOSE));
        decal(c, sn, sr, new V3(0, -0.5, 1), 0.085, 0, T_MOUTH, new float[]{0.3F, 0.3F, 0.4F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.75, 0.15, 0.6, 0.095, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.2, 0.4, 0.07);
        // barbita
        tube(c, new V3[]{sn.add(0, -0.05, -0.02), sn.add(0, -0.12, -0.03), sn.add(0, -0.16, -0.04)}, new double[]{0.02, 0.018, 0.006}, 5, flat(EL_F2));
        // orejas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 33 : 34) * 0.03;
            V3 b = new V3(sd * 0.075, 0.67 + bob, 0.3), tp = new V3(sd * (0.16 + ef), 0.7 + bob - ef, 0.25);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.04, 0.034, 0.01}, 5, flat(EL_F));
        }
        // astas de aurora: tubos emisivos con degradado
        boolean em = c.emissive;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] main = {new V3(sd * 0.05, 0.71 + bob, 0.32), new V3(sd * 0.11, 0.80 + bob, 0.30), new V3(sd * 0.2, 0.88 + bob, 0.26), new V3(sd * 0.3, 0.92 + bob, 0.2), new V3(sd * 0.38, 0.9 + bob, 0.13)};
            c.emissive = true;
            curve(c, main, new double[]{0.022, 0.02, 0.017, 0.013, 0.006}, 4, 6, (n, p) -> aur((p.y - bob - 0.71) / 0.21 * 0.5 + Math.abs(p.x) * 0.9 + 0.1 * Math.sin(t * 0.05 + ph)));
            for (int kk = 0; kk < 3; kk++) {
                final int k = kk;
                V3 a = main[1 + k];
                V3 b = a.add(sd * 0.03, 0.09 - k * 0.012, 0.075 + 0.02 * k);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.014, 0.011, 0.004}, 4, (n, p) -> aur((p.y - bob - 0.7) / 0.3 + 0.3 + k * 0.1 + 0.1 * Math.sin(t * 0.05 + ph)));
            }
            c.emissive = em;
            glow(c, main[2], 0.22, aur(0.4 + 0.3 * Math.sin(t * 0.04 + ph)), 0.3);
            glow(c, main[4], 0.14, aur(0.8), 0.28);
        }
        c.head = false;
        // destellos de aurora
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 3) * 0.3, 0.8 + u * 0.3 + bob, 0.2 + Math.cos(k * 1.7) * 0.15), 0.025 * (1 - u * 0.4), aur(k / 4.0), t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 34. CERBERO (temible)

    private static final float[] CB_B = hex(0x1C1826), CB_B2 = hex(0x3C3250), CB_BE = hex(0x6A5A78), CB_FL = hex(0x4AA8FF), CB_FL2 = hex(0xB8E8FF),
            CB_IRON = hex(0x6E7484), CB_BONE = hex(0xF2EAD8), CB_EMB = hex(0xFF5A1A), CB_EMB2 = hex(0xFFC040), CB_MOUTH = hex(0x8A0E10);

    private static void pupHead(Ctx c, V3 hc, double yaw, float[] eye, double bob, int seed) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        V3 fw = new V3(sy, 0, cy), sd0 = new V3(cy, 0, -sy);
        Paint hp = (n, p) -> mix(CB_B, CB_B2, ss(0.0, 0.9, n.y) * 0.55);
        // cráneo y hocico largo con la mandíbula abierta
        ell(c, hc, new V3(0.098, 0.088, 0.1), yaw, 0, 0, 14, 10, hp);
        ell(c, hc.add(fw.mul(0.115)).add(0, -0.012, 0), new V3(0.052, 0.04, 0.085), yaw, 0, 0, 12, 8, hp);
        ell(c, hc.add(fw.mul(0.105)).add(0, -0.068, 0), new V3(0.042, 0.017, 0.07), yaw, 0.28, 0, 10, 6, hp);
        ell(c, hc.add(fw.mul(0.115)).add(0, -0.04, 0), new V3(0.034, 0.012, 0.062), yaw, 0.14, 0, 8, 5, flat(CB_MOUTH));
        ell(c, hc.add(fw.mul(0.195)).add(0, -0.005, 0), new V3(0.025, 0.02, 0.02), yaw, 0, 0, 8, 6, flat(BLACK));
        // colmillos arriba y abajo
        for (int s = -1; s <= 1; s += 2) {
            V3 b = hc.add(fw.mul(0.16)).add(sd0.mul(s * 0.034)).add(0, -0.045, 0);
            cone(c, b, b.add(fw.mul(0.004)).add(0, -0.052, 0), 0.0105, 0.0, 5, flat(CB_BONE));
            V3 l = hc.add(fw.mul(0.15)).add(sd0.mul(s * 0.026)).add(0, -0.07, 0);
            cone(c, l, l.add(fw.mul(0.008)).add(0, 0.045, 0), 0.009, 0.0, 5, flat(CB_BONE));
            V3 m = hc.add(fw.mul(0.115)).add(sd0.mul(s * 0.04)).add(0, -0.048, 0);
            cone(c, m, m.add(0, -0.03, 0), 0.007, 0.0, 4, flat(CB_BONE));
        }
        // ojos rasgados que brillan + ceño fruncido
        boolean em = c.emissive;
        for (int s = -1; s <= 1; s += 2) {
            V3 ec = hc.add(sd0.mul(s * 0.052)).add(fw.mul(0.07)).add(0, 0.026, 0);
            c.emissive = true;
            ell(c, ec, new V3(0.024, 0.0105, 0.012), yaw, 0, s * 0.55, 8, 5, flat(eye));
            ell(c, ec.add(fw.mul(0.008)), new V3(0.006, 0.0095, 0.006), yaw, 0, 0, 6, 4, flat(BLACK));
            c.emissive = em;
            glow(c, ec.add(fw.mul(0.012)), 0.07, eye, 0.5);
            V3 b0 = hc.add(sd0.mul(s * 0.088)).add(fw.mul(0.045)).add(0, 0.058, 0);
            V3 b1 = hc.add(sd0.mul(s * 0.024)).add(fw.mul(0.088)).add(0, 0.036, 0);
            tube(c, new V3[]{b0, b0.lerp(b1, 0.5).add(0, 0.007, 0), b1}, new double[]{0.014, 0.016, 0.012}, 5, flat(CB_B));
            // orejas en punta
            V3 ea = hc.add(sd0.mul(s * 0.058)).add(0, 0.07, -0.02);
            cone(c, ea, ea.add(sd0.mul(s * 0.03)).add(0, 0.115, -0.045), 0.04, 0.0, 6, flat(CB_B2));
            cone(c, ea.add(fw.mul(0.008)), ea.add(sd0.mul(s * 0.026)).add(0, 0.095, -0.037), 0.026, 0.0, 6, flat(CB_MOUTH));
        }
        // collar de hierro con pinchos
        V3 cl = hc.add(0, -0.085, -0.012);
        ell(c, cl, new V3(0.093, 0.022, 0.087), 14, 4, flat(CB_IRON));
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.4;
            V3 b = cl.add(Math.cos(a) * 0.09, 0.0, Math.sin(a) * 0.084);
            cone(c, b, b.add(Math.cos(a) * 0.032, 0.012, Math.sin(a) * 0.032), 0.011, 0.0, 4, flat(CB_IRON));
        }
        // filo de brasa en la frente
        c.emissive = true;
        ell(c, hc.add(fw.mul(0.03)).add(0, 0.088, 0), new V3(0.01, 0.006, 0.04), yaw, 0, 0, 6, 4, flat(CB_EMB));
        c.emissive = em;
    }

    static void cerberusPup(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.27, 0);
        // cola larga acabada en llama azul
        double wag = Math.sin(t * 0.1 + ph) * 0.03 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.34 + bob, -0.24), new V3(wag * 0.5, 0.38 + bob, -0.33), new V3(wag, 0.46 + bob, -0.37), new V3(wag * 1.2, 0.55 + bob, -0.34)};
        curve(c, tcp, new double[]{0.04, 0.032, 0.026, 0.02}, 4, 7, flat(CB_B));
        boolean em = c.emissive;
        c.emissive = true;
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 0.4 + t * 0.12;
            double fh = 0.12 + 0.03 * Math.sin(t * 0.3 + ph + k);
            Detail.kite(c, tcp[3], tcp[3].add(Math.cos(a) * 0.03 + wag, fh, Math.sin(a) * 0.03 - 0.02), new V3(-Math.sin(a), 0, Math.cos(a)), 0.035, new V3(0, 0, 1), CB_FL, CB_FL2);
        }
        c.emissive = em;
        glow(c, tcp[3].add(0, 0.06, 0), 0.3, new float[]{0.3F, 0.65F, 1F}, 0.5);
        // patas gruesas con garras
        Paint lp = (n, p) -> mix(CB_B, CB_B2, 0.35);
        legs4(c, 0.1, 0.13, -0.15, 0.24 + bob, 0.05, 0.04, 0.07, lp, flat(CB_B));
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.13 : -0.15;
                double phase = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double swg = Math.sin(phase) * 0.075 * w, lft = Math.max(0, Math.cos(phase)) * 0.05 * w;
                for (int k = -1; k <= 1; k++) {
                    V3 b = new V3(sd * 0.1 + k * 0.022, 0.03 + lft, z0 + swg + 0.075);
                    cone(c, b, b.add(0, -0.022, 0.03), 0.0095, 0.0, 4, flat(CB_BONE));
                }
            }
        }
        // cuerpo: pecho ancho y lomo con púas de hueso y grietas de brasa
        V3 bc = new V3(0, 0.31 + bob, -0.03), br = new V3(0.165, 0.14, 0.23);
        Paint bp = (n, p) -> mix(CB_B, CB_B2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        ell(c, bc.add(0, 0.0, 0.14), new V3(0.17, 0.15, 0.13), 12, 8, bp);
        Detail.fur(c, bc, br, 70, 1011, 0.035, 0.018, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        for (int k = 0; k < 7; k++) {
            double z = 0.2 - k * 0.075;
            double h = 0.05 + 0.02 * Math.sin(k * 0.9 + 0.4);
            V3 b = new V3(0, bc.y + br.y * 0.96 - Math.abs(z + 0.03) * 0.25, z);
            cone(c, b, b.add(0, h, -0.025), 0.022 - k * 0.0012, 0.0, 5, flat(CB_BONE));
        }
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] cr = {new V3(sd * 0.163, bc.y + 0.07, -0.12), new V3(sd * 0.166, bc.y + 0.02, -0.07), new V3(sd * 0.168, bc.y + 0.05, -0.02), new V3(sd * 0.166, bc.y - 0.03, 0.03)};
            tube(c, cr, new double[]{0.006, 0.005, 0.006, 0.004}, 4, flat(CB_EMB));
            V3[] cr2 = {new V3(sd * 0.15, bc.y - 0.06, -0.15), new V3(sd * 0.158, bc.y - 0.02, -0.11), new V3(sd * 0.157, bc.y - 0.07, -0.07)};
            tube(c, cr2, new double[]{0.005, 0.005, 0.004}, 4, flat(CB_EMB2));
        }
        c.emissive = em;
        // llamas azules que lamen los hombros
        c.emissive = true;
        for (int k = 0; k < 6; k++) {
            double sdp = k % 2 == 0 ? -1 : 1;
            double a = t * 0.2 + k * 1.3 + ph;
            V3 b = new V3(sdp * (0.07 + 0.02 * (k / 2)), bc.y + 0.13, 0.12 - (k / 2) * 0.07);
            Detail.kite(c, b, b.add(sdp * 0.01, 0.075 + 0.03 * Math.sin(a), -0.02), new V3(1, 0, 0), 0.028, new V3(0, 0, 1), CB_FL, CB_FL2);
        }
        c.emissive = em;
        // tres cabezas
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.38, 0.16);
        double sw = Math.sin(t * 0.07 + ph) * 0.02;
        pupHead(c, new V3(0, 0.49 + bob, 0.28), 0.0, CB_EMB, bob, 1);
        tube(c, new V3[]{new V3(0, 0.34 + bob, 0.1), new V3(0, 0.42 + bob, 0.2), new V3(0, 0.46 + bob, 0.26)}, new double[]{0.09, 0.075, 0.065}, 7, bp);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 hc = new V3(sd * (0.21 + sw), 0.45 + bob, 0.21);
            V3[] nk = {new V3(sd * 0.09, 0.35 + bob, 0.08), new V3(sd * 0.15, 0.39 + bob, 0.14), hc.add(sd * -0.04, 0, -0.03)};
            tube(c, nk, new double[]{0.08, 0.065, 0.055}, 7, bp);
            pupHead(c, hc, sd * 0.5, sd > 0 ? CB_EMB2 : CB_FL, bob, 2);
        }
        c.head = false;
        // brasas y chispas azules
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(wag + Math.sin(k * 2.1) * 0.06, 0.58 + u * 0.4 + bob, -0.3 + Math.cos(k * 1.7) * 0.06), 0.022 * (1 - u * 0.5), k % 2 == 0 ? new float[]{0.5F, 0.8F, 1F} : new float[]{1F, 0.55F, 0.2F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 44. QUETZAL DORADO

    private static final float[] QZ_G = hex(0x2FB878), QZ_G2 = hex(0x7AE8A8), QZ_GO = hex(0xF6C84A), QZ_GO2 = hex(0xFFE68A), QZ_RD = hex(0xE8503A), QZ_W = hex(0xFFF8E8),
            QZ_BL = hex(0x2A8AC8), QZ_BK = hex(0x2A3A3A);

    static void quetzal(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.012 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.3, 0);
        // cola larga de cintas
        for (int k = -1; k <= 1; k++) {
            double sw = Math.sin(t * 0.07 + ph + k) * 0.03 + Math.sin(c.gait * 1.2 + k) * 0.015 * w;
            V3[] tl = {new V3(k * 0.03, 0.27 + bob, -0.12), new V3(k * 0.06 + sw * 0.4, 0.2 + bob, -0.3), new V3(k * 0.1 + sw, 0.1 + bob, -0.5), new V3(k * 0.14 + sw * 1.6, 0.04, -0.7), new V3(k * 0.15 + sw * 2, 0.02, -0.88)};
            curve(c, tl, new double[]{0.02, 0.026, 0.026, 0.02, 0.004}, 4, 6, (n, p) -> mix(QZ_G, QZ_BL, ss(0.15, 0.0, p.y) * 0.8));
            // flecos
            for (int j = 0; j < 4; j++) {
                V3 a = tl[1 + j % 3];
                Detail.feather(c, a, a.add(k * 0.03 + (j - 1.5) * 0.025, -0.02, -0.08), new V3(1, 0, 0), 0.025, new V3(0, 1, 0), QZ_G, QZ_G2);
            }
        }
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double phs = c.gait * 1.2 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(phs) * 0.06 * w, lift = Math.max(0, Math.cos(phs)) * 0.04 * w;
            V3 a = new V3(sd * 0.06, 0.2 + bob, 0.0), b = new V3(sd * 0.065, 0.03 + lift, 0.02 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.018, 0.014, 0.012}, 5, flat(QZ_RD));
            for (int k = -1; k <= 1; k++) {
                tube(c, new V3[]{b, b.add(k * 0.02, -0.01, 0.045)}, new double[]{0.01, 0.003}, 4, flat(QZ_RD));
            }
        }
        // cuerpo
        V3 bc = new V3(0, 0.31 + bob, -0.01), br = new V3(0.15, 0.17, 0.19);
        Paint bp = (n, p) -> {
            double belly = ss(-0.3, 0.6, n.z) * ss(0.2, -0.9, n.y + 0.0);
            float[] g = mix(QZ_G, QZ_G2, ss(-0.2, 0.9, n.y) * 0.5 + 0.2 * Math.sin(p.x * 40));
            return mix(g, QZ_RD, ss(0.0, 0.8, n.z) * ss(0.3, -0.5, n.y) * 0.9);
        };
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 8, 12, 0.045, 0.02, d -> 1, bp, QZ_G2);
        // alas plegadas con orilla dorada
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Math.sin(t * 0.1 + ph + sd) * 0.01 + w * Math.sin(c.gait * 1.2) * 0.02;
            V3 sh = new V3(sd * 0.14, 0.36 + bob, 0.04);
            for (int k = 0; k < 5; k++) {
                V3 e = sh.add(sd * (0.05 + 0.01 * k + fl), -0.1 - 0.018 * k, -0.12 - 0.05 * k);
                Detail.feather(c, sh.add(0, 0.0, -0.02 * k), e, new V3(0, 1, 0), 0.05, new V3(sd, 0.3, 0).norm(), k % 2 == 0 ? QZ_G : QZ_BL, k % 2 == 0 ? QZ_GO : QZ_GO2);
            }
        }
        // collar dorado
        ell(c, new V3(0, 0.44 + bob, 0.07), new V3(0.12, 0.03, 0.1), 14, 4, flat(QZ_GO));
        ell(c, new V3(0, 0.425 + bob, 0.15), new V3(0.026, 0.026, 0.01), 8, 6, flat(QZ_BL));
        // cabeza
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.46, 0.08);
        V3 hc = new V3(0, 0.55 + bob, 0.1), hr = new V3(0.12, 0.12, 0.115);
        Paint hp = (n, p) -> mix(QZ_G, QZ_G2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        Detail.plumage(c, hc, hr, 5, 10, 0.04, 0.02, d -> d.z < 0.2 ? 1 : 0.0, hp, QZ_G2);
        cone(c, new V3(0, 0.545 + bob, 0.19), new V3(0, 0.525 + bob, 0.265), 0.036, 0.008, 6, flat(QZ_GO));
        cone(c, new V3(0, 0.525 + bob, 0.19), new V3(0, 0.512 + bob, 0.25), 0.026, 0.008, 6, flat(scale(QZ_GO, 0.85)));
        eyes(c, hc, hr, 0.60, 0.20, 0.74, 0.095, T_BEAD, new float[]{1F, 0.95F, 0.7F});
        cheeks(c, hc, hr, 0.86, -0.25, 0.4, 0.07);
        // cresta dorada
        double fk = Detail.flick(c, 35) * 0.02;
        for (int k = 0; k < 7; k++) {
            double a = -0.9 + k * 0.3;
            V3 b0 = hc.add(Math.sin(a) * 0.05, 0.1, -0.02 - Math.abs(a) * 0.02);
            Detail.feather(c, b0, b0.add(Math.sin(a) * 0.04 + fk, 0.1 - Math.abs(a) * 0.04, -0.07 - Math.abs(a) * 0.02), new V3(1, 0, 0), 0.026, new V3(0, 0.2, 1), QZ_GO, QZ_GO2);
        }
        c.head = false;
        double u = ((t * 0.012 + ph) % 1.0);
        Detail.twinkle(c, new V3(Math.sin(u * 6) * 0.25, 0.4 + u * 0.4 + bob, -0.2), 0.022 * (1 - u * 0.4), new float[]{1F, 0.9F, 0.5F}, t, ph, 0.25);
    }

    // ------------------------------------------------------------------ 45. NARVAL ESTELAR

    private static final float[] NV_B = hex(0x2A3A8A), NV_B2 = hex(0x5A6AC8), NV_W = hex(0xE8EEFF), NV_HO = hex(0xFFF0C0), NV_HO2 = hex(0xA8E0FF), NV_STAR = hex(0xFFF8C0);

    static void narwhal(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.09 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double sw = Math.sin(t * 0.18 + ph);
        // cola con aleta
        V3[] tl = {new V3(0, 0.40 + bob, -0.15), new V3(0, 0.39 + bob + sw * 0.01, -0.3), new V3(0, 0.40 + bob + sw * 0.04, -0.45), new V3(0, 0.45 + bob + sw * 0.08, -0.56)};
        curve(c, tl, new double[]{0.1, 0.07, 0.045, 0.025}, 4, 10, (n, p) -> mix(NV_B, NV_B2, ss(-0.3, 0.9, n.y) * 0.5));
        V3 fp = tl[3];
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.kite(c, fp, fp.add(sd * 0.16, 0.08 + sw * 0.06, -0.06), new V3(0, 0, 1), 0.07, new V3(0, 1, 0), NV_B2, NV_W);
        }
        // cuerpo
        V3 bc = new V3(0, 0.40 + bob, 0.04), br = new V3(0.17, 0.17, 0.24);
        Paint bp = (n, p) -> {
            float[] top = mix(NV_B, NV_B2, ss(-0.3, 0.9, n.y) * 0.5);
            return mix(top, NV_W, ss(0.0, -0.8, n.y));
        };
        ell(c, bc, br, 18, 12, bp);
        // estrellas en el lomo
        for (int k = 0; k < 10; k++) {
            double a = k * 2.399;
            Detail.spot(c, bc, br, new V3(Math.sin(a) * 0.6, 0.55 + 0.35 * Math.cos(k * 1.1), Math.cos(a) * 0.9), 0.04 + 0.02 * (k % 3), NV_STAR);
        }
        // aletas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ff = Math.sin(t * 0.2 + ph + (sd > 0 ? 0 : 1)) * 0.06;
            V3 a = new V3(sd * 0.15, 0.33 + bob, 0.1), b = new V3(sd * (0.27 + ff * 0.2), 0.24 + bob + ff, 0.06);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.04, 0.035, 0.012}, 6, flat(NV_B2));
        }
        // cabeza
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.40, 0.2);
        V3 hc = new V3(0, 0.42 + bob, 0.27), hr = new V3(0.15, 0.14, 0.15);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(mix(NV_B, NV_B2, ss(-0.3, 0.9, n.y) * 0.5), NV_W, ss(0.0, -0.8, n.y)));
        ell(c, hc.add(0, -0.02, 0.1), new V3(0.1, 0.09, 0.08), 12, 8, flat(mix(NV_B2, NV_W, 0.4)));
        eyes(c, hc, hr, 0.64, 0.12, 0.72, 0.105, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.2, 0.4, 0.08);
        decal(c, hc, hr, new V3(0, -0.35, 1), 0.08, 0, T_MOUTH, new float[]{0.5F, 0.55F, 0.8F}, 0.9, 0.003);
        // cuerno espiral brillante
        V3 hb = hc.add(0.0, 0.07, 0.15);
        V3 ht = hb.add(0.0, 0.08, 0.38);
        boolean em = c.emissive;
        V3[] hn = new V3[9];
        double[] hrd = new double[9];
        for (int i = 0; i < 9; i++) {
            double u = i / 8.0;
            hn[i] = hb.lerp(ht, u);
            hrd[i] = 0.034 * (1 - u * 0.9) + 0.004;
        }
        Paint hpn = (n, p) -> {
            double band = 0.5 + 0.5 * Math.sin(p.z * 60 + Math.atan2(n.y, n.x) * 2);
            return mix(NV_HO, NV_HO2, band);
        };
        curve(c, hn, hrd, 2, 8, hpn);
        c.emissive = true;
        Detail.twinkle(c, ht.add(0, 0.01, 0.015), 0.045, new float[]{1F, 0.95F, 0.7F}, t, ph, 0.2);
        c.emissive = em;
        glow(c, ht, 0.2, new float[]{0.8F, 0.9F, 1F}, 0.4);
        c.head = false;
        // estelas de polvo estelar
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.17) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 2) * 0.2, 0.35 + Math.sin(k * 1.3) * 0.2 + bob, -0.3 - u * 0.4), 0.025 * (1 - u * 0.4), new float[]{1F, 0.95F, 0.7F}, t, ph + k, 0.25);
        }
    }
}
