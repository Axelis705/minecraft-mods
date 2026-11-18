package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Lote 8 (originales con accesorios grandes): Cerdito Alcancía, Zorrillo Aprendiz, Búho Bibliotecario y Jabalí Vikingo.
 */
final class Pets17 {
    private Pets17() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    private static final float[] GOLD = hex(0xFFC83A), GOLD2 = hex(0xFFE88A);

    private static void ring(Ctx c, V3 ce, double r, double rad, float[] col) {
        V3[] pts = new V3[13];
        double[] rr = new double[13];
        for (int k = 0; k < 13; k++) {
            double a = k * Math.PI * 2 / 12;
            pts[k] = ce.add(Math.cos(a) * r, Math.sin(a) * r, 0);
            rr[k] = rad;
        }
        tube(c, pts, rr, 5, flat(col));
    }

    // ------------------------------------------------------------------ 51. CERDITO ALCANCÍA

    private static final float[] PG_P = hex(0xFFA6BE), PG_P2 = hex(0xF07C9C), PG_CR = hex(0xFFD8E0), PG_SN = hex(0xFF8CAA), PG_DK = hex(0x5A1E34), PG_HF = hex(0xB04868), PG_BW = hex(0x38B8E8);

    static void piggyBank(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.02 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // patitas cortas con pezuñas
        legs4(c, 0.08, 0.1, -0.1, 0.13 + bob, 0.03, 0.03, 0.045, flat(PG_P2), flat(PG_HF));
        V3 bc = new V3(0, 0.205 + bob, 0.0), br = new V3(0.145, 0.12, 0.17);
        Paint bp = (n, p) -> mix(mix(PG_P, PG_P2, ss(0.2, -0.9, n.y) * 0.5), PG_CR, ss(-0.2, -0.8, n.y) * 0.65);
        ell(c, bc, br, 28, 18, bp);
        Detail.glint(c, bc, br, new V3(-0.4, 0.8, 0.4), 0.09, WHITE, 0.4);
        // moneda dorada pintada en cada costado y rubor
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, bc, br, new V3(sd * 0.95, 0.12, -0.05), 0.11, 0.0, Detail.T_DISC, GOLD, 1, 0.004);
            decal(c, bc, br, new V3(sd * 0.96, 0.12, -0.05), 0.075, 0.0, Detail.T_DISC, GOLD2, 1, 0.006);
        }
        // ranura y monedas que salen y entran
        V3 sl = bc.add(0, 0.119, -0.03);
        ell(c, sl, new V3(0.055, 0.007, 0.014), 10, 3, flat(PG_DK));
        double cu = (t * 0.018 + ph) % 1.0;
        double ch = Math.sin(cu * Math.PI) * 0.05;
        V3 cp = sl.add(0, 0.012 + ch, 0.0);
        ell(c, cp, new V3(0.034, 0.034, 0.006), 12, 4, flat(GOLD));
        ell(c, cp.add(0, 0, 0.005), new V3(0.022, 0.022, 0.004), 10, 3, flat(GOLD2));
        Detail.twinkle(c, cp.add(0.03, 0.03, 0.01), 0.03, new float[]{1F, 0.95F, 0.6F}, t, ph, 0.4);
        // cola en rulo
        V3[] tl = new V3[8];
        double[] tr = new double[8];
        for (int k = 0; k < 8; k++) {
            double a = k * 0.85 + 0.3;
            double sc = 0.045 - k * 0.003;
            tl[k] = new V3(Math.sin(a) * sc * 0.6, 0.25 + bob + Math.cos(a) * sc - 0.04, -0.17 - k * 0.003);
            tr[k] = 0.014 - k * 0.001;
        }
        tube(c, tl, tr, 5, flat(PG_P2));
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.22, 0.13);
        V3 hc = new V3(0, 0.245 + bob, 0.15), hr = new V3(0.105, 0.095, 0.1);
        ell(c, hc, hr, 20, 14, bp);
        // hocico redondo y brillante
        V3 sn = hc.add(0, -0.022, 0.092);
        ell(c, sn, new V3(0.062, 0.05, 0.04), 16, 10, flat(PG_SN));
        ell(c, sn.add(0, 0, 0.032), new V3(0.054, 0.042, 0.014), 16, 8, flat(scale(PG_SN, 1.08F)));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, sn.add(sd * 0.022, 0.0, 0.045), new V3(0.01, 0.016, 0.006), 6, 4, flat(PG_DK));
        }
        decal(c, hc, hr, new V3(0.5, 0.25, 0.82), 0.075, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.25, 0.82), 0.075, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        cheeks(c, hc, hr, 0.86, -0.1, 0.5, 0.075);
        // orejas caídas y redondeadas
        double fl = Detail.flick(c, 51) * 0.1;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 e0 = hc.add(sd * 0.075, 0.07, 0.0);
            ell(c, e0.add(sd * 0.022, 0.0, 0.0), new V3(0.038, 0.012, 0.046), sd * -0.2, 0.0, sd * (0.9 + fl), 12, 6, flat(PG_P));
            ell(c, e0.add(sd * 0.024, -0.002, 0.004), new V3(0.026, 0.008, 0.034), sd * -0.2, 0.0, sd * (0.9 + fl), 10, 5, flat(PG_SN));
        }
        // lacito azul
        V3 bw = hc.add(0, -0.082, 0.03);
        for (int sd = -1; sd <= 1; sd += 2) {
            cone(c, bw, bw.add(sd * 0.05, 0.02, 0.01), 0.026, 0.0, 6, flat(PG_BW));
        }
        ell(c, bw, new V3(0.014, 0.014, 0.014), 6, 5, flat(scale(PG_BW, 0.8F)));
        c.head = false;
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.33) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.25 + u * 0.3 + bob, Math.cos(k * 1.7) * 0.15), 0.02, new float[]{1F, 0.9F, 0.5F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 52. ZORRILLO APRENDIZ

    private static final float[] SK_B = hex(0x2C2A3E), SK_W = hex(0xF8F4EC), SK_PU = hex(0x7A3CC8), SK_PU2 = hex(0xA866F0), SK_WD = hex(0x9A6A3C);

    static void skunkWizard(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // cola esponjosa, curva hacia arriba
        double sw = Math.sin(t * 0.08 + ph) * 0.02;
        V3[] tcp = {new V3(0, 0.2 + bob, -0.13), new V3(sw * 0.4, 0.3 + bob, -0.23), new V3(sw, 0.43 + bob, -0.25), new V3(sw * 1.5, 0.52 + bob, -0.19)};
        Paint tp = (n, p) -> mix(SK_B, SK_W, ss(0.2, 0.9, n.x * 0.0 + n.y * 0.0 + 0.2) * 0.0);
        curve(c, tcp, new double[]{0.05, 0.075, 0.08, 0.05}, 4, 12, tp);
        Detail.furTube(c, tcp, new double[]{0.05, 0.075, 0.08, 0.05}, 50, 8201, 0.03, 0.018, 0.6, tp, SK_B);
        // franja blanca a lo largo de la cola y punta blanca esponjosa
        V3[] st = {tcp[0].add(0, 0, -0.035), tcp[1].add(0, 0, -0.065), tcp[2].add(0, 0, -0.075), tcp[3].add(0, 0.0, -0.045)};
        curve(c, st, new double[]{0.022, 0.034, 0.036, 0.028}, 4, 8, flat(SK_W));
        ell(c, tcp[3].add(0, 0.045, 0.01), new V3(0.05, 0.065, 0.05), 12, 8, flat(SK_W));
        Detail.fur(c, tcp[3].add(0, 0.045, 0.01), new V3(0.05, 0.065, 0.05), 40, 8203, 0.03, 0.018, new V3(0, 1, 0), 0.7, Detail.ALL, flat(SK_W), SK_W);
        legs4(c, 0.065, 0.1, -0.095, 0.15 + bob, 0.032, 0.03, 0.05, flat(SK_B), flat(SK_W));
        V3 bc = new V3(0, 0.215 + bob, 0.0), br = new V3(0.1, 0.095, 0.16);
        Paint bp = (n, p) -> Math.abs(p.x) < 0.028 && n.y > 0.2 ? SK_W : SK_B;
        ell(c, bc, br, 24, 14, bp);
        Detail.fur(c, bc, br, 70, 8202, 0.02, 0.014, new V3(0, 0, -1), 0.7, Detail.ALL, bp, SK_B);
        ell(c, bc.add(0, 0.088, -0.005), new V3(0.026, 0.014, 0.15), 6, 10, flat(SK_W));
        // capa morada con estrellas
        V3 cb = bc.add(0, 0.07, 0.04);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.feather(c, cb.add(sd * 0.04, 0, 0), cb.add(sd * 0.1, -0.1, -0.2 + Math.sin(t * 0.1 + ph) * 0.01), new V3(0, 0, 1), 0.07, new V3(0, 1, 0), SK_PU, SK_PU2);
        }
        Detail.feather(c, cb, cb.add(0, -0.12, -0.22), new V3(1, 0, 0), 0.08, new V3(0, 1, 0), SK_PU, SK_PU2);
        ell(c, bc.add(0, 0.055, 0.1), new V3(0.016, 0.016, 0.01), 6, 5, flat(GOLD));
        // bufanda morada
        ell(c, bc.add(0, 0.05, 0.11), new V3(0.095, 0.03, 0.07), 14, 6, flat(SK_PU));
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.27, 0.12);
        V3 hc = new V3(0, 0.31 + bob, 0.16), hr = new V3(0.085, 0.075, 0.082);
        Paint hp = (n, p) -> Math.abs(p.x) < 0.02 && n.z > 0.3 ? SK_W : SK_B;
        ell(c, hc, hr, 20, 12, hp);
        V3 sn = hc.add(0, -0.022, 0.075);
        ell(c, sn, new V3(0.035, 0.03, 0.045), 10, 7, flat(SK_B));
        ell(c, sn.add(0, 0.012, 0.04), new V3(0.016, 0.012, 0.01), 8, 5, flat(hex(0xFF8FB0)));
        decal(c, hc, hr, new V3(0.5, 0.18, 0.84), 0.095, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.18, 0.84), 0.095, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        cheeks(c, hc, hr, 0.84, -0.14, 0.5, 0.055);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, sn.add(sd * 0.03, k * 0.008, 0.02), sn.add(sd * 0.09, k * 0.02, 0.03), -0.004, 0.0025, SK_W);
            }
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.075, 0.035, -0.04), new V3(0.026, 0.03, 0.016), 8, 5, flat(SK_B));
        }
        // sombrero de mago con ala, banda, estrellas y punta doblada
        V3 hb = hc.add(0, 0.055, -0.01);
        ell(c, hb, new V3(0.135, 0.012, 0.135), 20, 3, flat(SK_PU));
        cone(c, hb.add(0, 0.005, 0), hb.add(0, 0.13, -0.02), 0.085, 0.04, 14, flat(SK_PU));
        cone(c, hb.add(0, 0.13, -0.02), hb.add(0.0, 0.2, -0.07), 0.04, 0.012, 10, flat(SK_PU));
        ell(c, hb.add(0, 0.018, 0.0), new V3(0.088, 0.014, 0.088), 16, 3, flat(GOLD));
        for (int k = 0; k < 3; k++) {
            c.emissive = true;
            ell(c, hb.add((k - 1) * 0.04, 0.06 + (k % 2) * 0.03, 0.058 - (k % 2) * 0.004), new V3(0.014, 0.014, 0.004), 5, 2, flat(GOLD2));
            c.emissive = false;
        }
        glowStar(c, hb.add(0, 0.2, -0.07), 0.07, new float[]{0.8F, 0.6F, 1F}, t, ph);
        c.head = false;
        // varita con estrella
        double wv = Math.sin(t * 0.1 + ph) * 0.015;
        V3 hand = new V3(0.105, 0.2 + bob, 0.12);
        tube(c, new V3[]{new V3(0.075, 0.23 + bob, 0.02), hand}, new double[]{0.016, 0.014}, 5, flat(SK_B));
        V3 wt = hand.add(0.03, 0.14 + wv, 0.05);
        tube(c, new V3[]{hand.add(-0.01, -0.03, -0.01), wt}, new double[]{0.008, 0.007}, 5, flat(SK_WD));
        c.emissive = true;
        ell(c, wt.add(0, 0.012, 0), new V3(0.03, 0.03, 0.008), 5, 2, flat(GOLD2));
        c.emissive = false;
        glow(c, wt.add(0, 0.012, 0), 0.12, new float[]{1F, 0.9F, 0.5F}, 0.5);
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.02 + k * 0.25 + ph * 0.1) % 1.0);
            Detail.twinkle(c, wt.add(Math.sin(k * 2.5 + u * 4) * 0.06, u * 0.1, Math.cos(k * 1.9) * 0.05), 0.02, new float[]{0.85F, 0.7F, 1F}, t, ph + k, 0.4);
        }
    }

    // ------------------------------------------------------------------ 53. ARDILLA EXPLORADORA

    private static final float[] SQ_O = hex(0xE07A30), SQ_O2 = hex(0xF4A456), SQ_C = hex(0xFFEBCF), SQ_DK = hex(0x4A2A18), SQ_LT = hex(0x8A5530), SQ_RD = hex(0xD0403A), SQ_BR = hex(0xD8A030);

    static void squirrelScout(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.3)) * 0.022 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        double sw = Math.sin(t * 0.08 + ph) * 0.025 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.18 + bob, -0.11), new V3(sw * 0.3, 0.29 + bob, -0.23), new V3(sw, 0.43 + bob, -0.25), new V3(sw * 1.3, 0.55 + bob, -0.15)};
        double[] trr = {0.045, 0.075, 0.105, 0.115};
        Paint tp = (n, p) -> mix(SQ_O, SQ_O2, ss(-0.3, 0.9, n.z));
        curve(c, tcp, trr, 5, 12, tp);
        Detail.furTube(c, tcp, trr, 90, 9101, 0.02, 0.018, 0.5, tp, SQ_O2);
        ell(c, tcp[3].add(0, 0.03, 0.0), new V3(0.11, 0.13, 0.105), 16, 10, (n, p) -> mix(SQ_O, SQ_C, ss(0.3, 1.0, n.y)));
        legs4(c, 0.065, 0.09, -0.085, 0.15 + bob, 0.032, 0.03, 0.05, flat(SQ_O), flat(SQ_DK));
        V3 bc = new V3(0, 0.22 + bob, 0.0), br = new V3(0.1, 0.11, 0.125);
        Paint bp = (n, p) -> mix(mix(SQ_O, SQ_O2, ss(0.0, 0.9, n.y) * 0.5), SQ_C, ss(0.1, 0.7, n.z) * ss(0.5, -0.5, n.y));
        ell(c, bc, br, 24, 14, bp);
        Detail.fur(c, bc, br, 70, 9102, 0.022, 0.014, new V3(0, -0.3, -1), 0.7, Detail.ALL, bp, SQ_O2);
        // mochila de explorador con saco de dormir
        V3 bk = bc.add(0, 0.02, -0.115);
        ell(c, bk, new V3(0.085, 0.09, 0.05), 14, 9, flat(SQ_LT));
        ell(c, bk.add(0, -0.02, -0.04), new V3(0.05, 0.04, 0.012), 10, 5, flat(scale(SQ_LT, 0.75F)));
        tube(c, new V3[]{bk.add(-0.09, 0.095, 0.0), bk.add(0.0, 0.1, 0.0), bk.add(0.09, 0.095, 0.0)}, new double[]{0.03, 0.03, 0.03}, 8, flat(SQ_RD));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bk.add(sd * 0.092, 0.095, 0.0), new V3(0.01, 0.03, 0.03), 8, 6, flat(scale(SQ_RD, 0.8F)));
            tube(c, new V3[]{bk.add(sd * 0.05, 0.07, 0.03), bc.add(sd * 0.08, 0.06, 0.06), bc.add(sd * 0.09, -0.04, 0.1)}, new double[]{0.008, 0.008, 0.008}, 4, flat(SQ_DK));
            ell(c, bk.add(sd * 0.03, 0.095, 0.03), new V3(0.012, 0.012, 0.006), 6, 4, flat(SQ_BR));
        }
        // bellota entre las manitas
        V3 ac = new V3(0, 0.205 + bob, 0.135);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.085, 0.25 + bob, 0.06);
            tube(c, new V3[]{a, a.lerp(ac, 0.5).add(sd * 0.02, 0, 0.01), ac.add(sd * 0.022, 0.0, 0.0)}, new double[]{0.02, 0.018, 0.016}, 5, flat(SQ_O));
        }
        ell(c, ac, new V3(0.028, 0.034, 0.028), 10, 7, flat(hex(0xE8B864)));
        ell(c, ac.add(0, 0.026, 0), new V3(0.032, 0.016, 0.032), 10, 5, flat(SQ_LT));
        Detail.glint(c, ac, new V3(0.028, 0.034, 0.028), new V3(-0.4, 0.6, 0.7), 0.012, WHITE, 0.8);
        // cabeza
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.3, 0.1);
        V3 hc = new V3(0, 0.33 + bob, 0.1), hr = new V3(0.092, 0.082, 0.088);
        Paint hp = (n, p) -> mix(SQ_O, SQ_O2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 20, 13, hp);
        V3 sn = hc.add(0, -0.022, 0.078);
        ell(c, sn, new V3(0.034, 0.03, 0.036), 10, 7, flat(SQ_C));
        ell(c, sn.add(0, 0.014, 0.032), new V3(0.014, 0.011, 0.01), 8, 5, flat(hex(0x8A3A3A)));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.058, -0.035, 0.05), new V3(0.04, 0.035, 0.034), 10, 7, flat(SQ_C));
            V3 e0 = hc.add(sd * 0.055, 0.07, -0.01);
            cone(c, e0, e0.add(sd * 0.02, 0.085, -0.01), 0.036, 0.0, 6, flat(SQ_O));
            cone(c, e0.add(sd * 0.0, 0.07, -0.01), e0.add(sd * 0.022, 0.12, -0.012), 0.016, 0.0, 5, flat(SQ_DK));
        }
        ell(c, sn.add(0, -0.03, 0.026), new V3(0.016, 0.014, 0.006), 6, 4, flat(WHITE));
        decal(c, hc, hr, new V3(0.5, 0.17, 0.84), 0.085, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.17, 0.84), 0.085, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        // goggles de aviador sobre la frente
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 gc = hc.add(sd * 0.04, 0.062, 0.075);
            ring(c, gc, 0.03, 0.006, SQ_BR);
            ell(c, gc.add(0, 0, -0.004), new V3(0.028, 0.028, 0.004), 10, 3, flat(hex(0x6AC8F0)));
        }
        tube(c, new V3[]{hc.add(-0.068, 0.06, 0.06), hc.add(-0.09, 0.05, -0.01), hc.add(0, 0.06, -0.09), hc.add(0.09, 0.05, -0.01), hc.add(0.068, 0.06, 0.06)}, new double[]{0.007, 0.007, 0.007, 0.007, 0.007}, 4, flat(SQ_DK));
        c.head = false;
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.33) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.25 + u * 0.3 + bob, Math.cos(k * 1.7) * 0.15), 0.017, new float[]{1F, 0.9F, 0.6F}, t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 54. FOCA MARINERA

    private static final float[] SL_G = hex(0x7C98C0), SL_G2 = hex(0xA8C0E0), SL_C = hex(0xF4F0E4), SL_BL = hex(0x2A4C94), SL_RD = hex(0xE0403A);

    static void sailorSeal(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.02 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // aletas traseras
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Math.sin(c.gait * 1.2 + (sd > 0 ? Math.PI : 0)) * 0.1 * w;
            ell(c, new V3(sd * 0.06, 0.03 + bob * 0.3, -0.07), new V3(0.05, 0.012, 0.075), sd * 0.35 + fl, 0.0, 0.0, 10, 5, flat(SL_G));
        }
        V3 lo = new V3(0, 0.1 + bob * 0.5, -0.02);
        ell(c, lo, new V3(0.1, 0.09, 0.12), 16, 10, flat(SL_G));
        V3 bc = new V3(0, 0.215 + bob, 0.0), br = new V3(0.125, 0.15, 0.12);
        Paint bp = (n, p) -> mix(mix(SL_G, SL_G2, ss(0.0, 0.9, n.y) * 0.5), SL_C, ss(0.1, 0.6, n.z) * ss(0.6, -0.6, n.y));
        ell(c, bc, br, 24, 16, bp);
        // salvavidas rojo y blanco
        for (int k = 0; k < 8; k++) {
            double a0 = k * Math.PI / 4, a1 = a0 + Math.PI / 4;
            double rr = 0.15;
            V3 p0 = new V3(Math.cos(a0) * rr, 0.16 + bob * 0.8, Math.sin(a0) * rr * 0.95), p1 = new V3(Math.cos((a0 + a1) / 2) * rr, 0.16 + bob * 0.8, Math.sin((a0 + a1) / 2) * rr * 0.95), p2 = new V3(Math.cos(a1) * rr, 0.16 + bob * 0.8, Math.sin(a1) * rr * 0.95);
            tube(c, new V3[]{p0, p1, p2}, new double[]{0.03, 0.03, 0.03}, 6, flat(k % 2 == 0 ? SL_RD : WHITE));
        }
        // aletas delanteras (una saluda)
        for (int sd = -1; sd <= 1; sd += 2) {
            double wv = sd > 0 ? Math.sin(t * 0.18 + ph) * 0.05 + 0.06 : Math.sin(c.gait) * 0.02 * w;
            V3 a = new V3(sd * 0.11, 0.29 + bob, 0.02), b = new V3(sd * 0.19, 0.23 + bob + (sd > 0 ? 0.08 + wv : 0.0), 0.05);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.03, 0.026, 0.022}, 6, flat(SL_G));
            ell(c, b.add(sd * 0.012, 0.0, 0.004), new V3(0.03, 0.014, 0.05), 0.0, 0.0, sd * 0.3, 10, 5, flat(SL_G));
        }
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.36, 0.03);
        V3 hc = new V3(0, 0.4 + bob, 0.03), hr = new V3(0.105, 0.095, 0.1);
        Paint hp = (n, p) -> mix(mix(SL_G, SL_G2, ss(0.0, 0.9, n.y) * 0.5), SL_C, ss(0.4, 0.9, n.z) * ss(0.2, -0.6, n.y) * 0.6);
        ell(c, hc, hr, 20, 14, hp);
        V3 sn = hc.add(0, -0.028, 0.086);
        ell(c, sn, new V3(0.058, 0.046, 0.045), 14, 9, flat(SL_C));
        ell(c, sn.add(0, 0.018, 0.04), new V3(0.022, 0.016, 0.012), 8, 5, flat(BLACK));
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, sn.add(sd * 0.035, -0.004 + k * 0.01, 0.03), sn.add(sd * 0.11, -0.02 + k * 0.025, 0.04), -0.006, 0.003, WHITE);
            }
        }
        decal(c, hc, hr, new V3(0.5, 0.2, 0.83), 0.09, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.2, 0.83), 0.09, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        cheeks(c, hc, hr, 0.86, -0.14, 0.5, 0.07);
        // gorra de marinero
        V3 hm = hc.add(0, 0.075, -0.01);
        Detail.cap(c, hm, new V3(0.1, 0.06, 0.1), 0, 0, 0, 1.35, 0.0, 16, 6, (n, p) -> mix(WHITE, hex(0xE4ECF8), ss(0.5, -0.5, n.y) * 0.5));
        ell(c, hm.add(0, -0.01, 0), new V3(0.108, 0.014, 0.108), 16, 3, flat(SL_BL));
        ell(c, hm.add(0, 0.0, 0), new V3(0.104, 0.01, 0.104), 16, 3, flat(WHITE));
        ell(c, hm.add(0, 0.034, 0.088), new V3(0.016, 0.016, 0.004), 8, 3, flat(GOLD));
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.feather(c, hm.add(sd * 0.03, -0.01, -0.09), hm.add(sd * 0.05, -0.1 + Math.sin(t * 0.1 + ph) * 0.01, -0.12), new V3(1, 0, 0), 0.016, new V3(0, 0, 1), SL_BL, SL_BL);
        }
        c.head = false;
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.014 + k * 0.25 + ph * 0.1) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.7) * 0.2, 0.2 + u * 0.35, Math.cos(k * 1.9) * 0.15), 0.02, new float[]{0.7F, 0.9F, 1F}, t, ph + k, 0.3);
        }
    }
}
