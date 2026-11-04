package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Rework de mascotas, lote 2 (poco comunes): cabrita montañera, erizo turbo, conejo de la luna, gato de la fortuna,
 * perrito ninja, calamar pintor, espíritu farolito y perezoso de hojas.
 */
final class Pets9 {
    private Pets9() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 16. CABRITA MONTAÑERA

    private static final float[] GT_W = hex(0xF4EEE2), GT_W2 = hex(0xD9CDB8), GT_GR = hex(0x8E8678), GT_HOOF = hex(0x4A3C34), GT_HORN = hex(0xE3CFA0),
            GT_RED = hex(0xE0504A), GT_BELL = hex(0xF2C14E), GT_FLOW = hex(0xFFFFFF), GT_PK = hex(0x5A8A4A), GT_PK2 = hex(0x7AAE62), GT_ROPE = hex(0xE0C88A);

    static void goat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.28, 0);
        // colita corta
        V3 tb = new V3(0, 0.33 + bob, -0.24);
        double wag = Math.sin(t * 0.2 + ph) * 0.02;
        tube(c, new V3[]{tb, tb.add(wag, 0.04, -0.04), tb.add(wag * 2, 0.07, -0.05)}, new double[]{0.03, 0.026, 0.012}, 6, flat(GT_W));
        legs4(c, 0.085, 0.14, -0.16, 0.22 + bob, 0.04, 0.035, 0.05, (n, p) -> mix(GT_W, GT_GR, ss(0.28, 0.02, p.y + 0.12) * 0.5), flat(GT_HOOF));
        // cuerpo
        V3 bc = new V3(0, 0.30 + bob, -0.02), br = new V3(0.13, 0.12, 0.25);
        Paint bp = (n, p) -> mix(mix(GT_W, GT_W2, ss(0.1, -0.9, n.y) * 0.8), GT_GR, ss(0.3, 0.9, n.y) * 0.18);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 210, 901, 0.024, 0.011, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        // mochila de escaladora con cuerda y bandera
        V3 pk = new V3(0, 0.44 + bob, -0.06);
        ell(c, pk, new V3(0.085, 0.07, 0.075), 12, 8, (n, p) -> mix(GT_PK, GT_PK2, ss(-0.4, 0.9, n.y)));
        ell(c, pk.add(0, 0.045, 0.0), new V3(0.08, 0.026, 0.07), 12, 5, flat(GT_PK2));
        V3[] rp = new V3[13];
        double[] rr0 = new double[13];
        for (int k = 0; k <= 12; k++) {
            double a = Math.PI * 2 * k / 12.0;
            rp[k] = pk.add(Math.cos(a) * 0.05, 0.085 + 0.0, Math.sin(a) * 0.05 * 0.8);
            rr0[k] = 0.008;
        }
        tube(c, rp, rr0, 5, flat(GT_ROPE));
        for (int sd = -1; sd <= 1; sd += 2) {
            tube(c, new V3[]{pk.add(sd * 0.05, 0.05, 0.03), new V3(sd * 0.12, 0.38 + bob, 0.1), new V3(sd * 0.1, 0.26 + bob, 0.14)}, new double[]{0.008, 0.008, 0.008}, 4, flat(GT_RED));
        }
        tube(c, new V3[]{pk.add(0.05, 0.05, -0.04), pk.add(0.05, 0.16, -0.04)}, new double[]{0.004, 0.004}, 4, flat(GT_ROPE));
        Detail.kite(c, pk.add(0.05, 0.16, -0.04), pk.add(0.11 + 0.01 * Math.sin(t * 0.2 + ph), 0.145, -0.04), new V3(0, 1, 0), 0.022, new V3(0, 0, 1), GT_RED, scale(GT_RED, 1.2F));
        // pañuelo rojo
        ell(c, new V3(0, 0.335 + bob, 0.16), new V3(0.125, 0.085, 0.07), 12, 8, flat(GT_RED));
        Detail.kite(c, new V3(0.0, 0.30 + bob, 0.20), new V3(0.03 + 0.01 * Math.sin(t * 0.1 + ph), 0.20 + bob, 0.21), new V3(1, 0, 0), 0.03, new V3(0, 0, 1), GT_RED, scale(GT_RED, 1.2));
        // cabeza
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.38, 0.2);
        V3 hc = new V3(0, 0.455 + bob, 0.27), hr = new V3(0.105, 0.125, 0.12);
        Paint hp = (n, p) -> mix(GT_W, GT_W2, ss(0.0, -0.9, n.y) * 0.4);
        ell(c, hc, hr, 14, 10, hp);
        Detail.fur(c, hc, hr, 50, 902, 0.025, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.4 ? 1 : 0.0, hp, null);
        V3 sc = new V3(0, 0.415 + bob, 0.37), sr = new V3(0.06, 0.05, 0.065);
        ell(c, sc, sr, 10, 7, flat(GT_W2));
        ell(c, sc.add(0, 0.015, 0.055), new V3(0.02, 0.014, 0.014), 6, 4, flat(hex(0xE89AA0)));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.065, 0, T_MOUTH, new float[]{0.35F, 0.2F, 0.2F}, 0.9, 0.003);
        // barbita
        tube(c, new V3[]{sc.add(0, -0.04, 0.02), sc.add(0.0, -0.1, 0.03), sc.add(0.005, -0.14, 0.02)}, new double[]{0.016, 0.013, 0.004}, 5, flat(GT_W2));
        eyes(c, hc, hr, 0.55, 0.20, 0.80, 0.09, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.07);
        // cuernitos curvos y orejas laterales
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] hn = {new V3(sd * 0.05, 0.55 + bob, 0.24), new V3(sd * 0.09, 0.62 + bob, 0.19), new V3(sd * 0.095, 0.675 + bob, 0.12), new V3(sd * 0.075, 0.69 + bob, 0.075)};
            curve(c, hn, new double[]{0.026, 0.021, 0.015, 0.006}, 4, 8, (n, p) -> mix(GT_HORN, scale(GT_HORN, 0.7), ss(0.55, 0.69, p.y - bob)));
            double ef = Detail.flick(c, sd > 0 ? 11 : 12);
            V3 eb = new V3(sd * 0.1, 0.46 + bob, 0.24);
            V3 et = new V3(sd * (0.2 + ef * 0.02), 0.445 + bob - ef * 0.03, 0.2);
            tube(c, new V3[]{eb, eb.lerp(et, 0.5), et}, new double[]{0.034, 0.028, 0.012}, 5, flat(GT_W2));
        }
        // florcita de las montañas
        Detail.kite(c, new V3(0.06, 0.575 + bob, 0.30), new V3(0.06, 0.60 + bob, 0.33), new V3(1, 0, 0), 0.016, new V3(0, 1, 0), GT_FLOW, GT_FLOW);
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5;
            Detail.kite(c, new V3(0.06, 0.60 + bob, 0.33), new V3(0.06 + Math.cos(a) * 0.03, 0.60 + bob + Math.sin(a) * 0.03, 0.335), new V3(0, 0, 1), 0.012, new V3(0, 0, 1), GT_FLOW, GT_FLOW);
        }
        ell(c, new V3(0.06, 0.60 + bob, 0.335), new V3(0.01, 0.01, 0.01), 6, 4, flat(GT_BELL));
        c.head = false;
        // campanita en el cuello
        double sw = Math.sin(t * 0.12 + ph) * 0.01 + Math.sin(c.gait) * 0.02 * w;
        ell(c, new V3(sw, 0.29 + bob, 0.215), new V3(0.026, 0.026, 0.026), 8, 6, flat(GT_BELL));
        Detail.glint(c, new V3(sw, 0.29 + bob, 0.215), new V3(0.026, 0.026, 0.026), new V3(-0.4, 0.5, 1), 0.018, WHITE, 0.9);
    }

    // ------------------------------------------------------------------ 17. ERIZO TURBO

    private static final float[] ET_B = hex(0x2E9AD8), ET_B2 = hex(0x6CCCF2), ET_Q = hex(0x2A2C84), ET_Q2 = hex(0x58D0F0), ET_SK = hex(0xFFEBD0), ET_SH = hex(0xB8E83A), ET_SC = hex(0xFF9A2E), ET_GG = hex(0x3A3A52), ET_LN = hex(0x9AE8F8),
            ET_GL = hex(0xFFFFFF), ET_BEL = hex(0xFFE6CC);

    static void turboHedgehog(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bounce = Math.abs(Math.sin(c.gait * 1.5)) * 0.03 * w + 0.01 * Math.sin(t * 0.16 + ph);
        c.pivot = new V3(0, 0.25, 0);
        double lean = 0.12 * w;
        // piernas con zapatillas grandes
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.5 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.09 * w, lift = Math.max(0, Math.cos(p)) * 0.06 * w;
            double tap = (1 - w) * Math.max(0, Math.sin(t * 0.25 + ph)) * 0.02 * (sd > 0 ? 1 : 0);
            V3 a = new V3(sd * 0.055, 0.16 + bounce, 0.0), b = new V3(sd * 0.06, 0.055 + lift + tap, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.032, 0.027, 0.026}, 6, flat(ET_B));
            ell(c, b.add(0, -0.03, 0.04), new V3(0.05, 0.034, 0.09), 10, 7, flat(ET_SH));
            ell(c, b.add(0, -0.006, 0.0), new V3(0.052, 0.012, 0.05), 10, 4, flat(WHITE));
            ell(c, b.add(0, -0.052, 0.04), new V3(0.05, 0.009, 0.09), 10, 3, flat(ET_GL));
        }
        // cuerpo
        V3 bc = new V3(0, 0.275 + bounce, 0), br = new V3(0.115, 0.125, 0.105);
        Paint bp = (n, p) -> mix(ET_B, ET_B2, ss(0.3, 0.9, n.y) * 0.4);
        ell(c, bc, br, 14, 9, bp);
        ell(c, bc.add(0, -0.01, 0.045), new V3(0.075, 0.095, 0.07), 12, 8, flat(ET_BEL));
        // brazos con guantes blancos
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(c.gait * 1.5 + (sd > 0 ? 0 : Math.PI)) * 0.09 * w;
            V3 a = new V3(sd * 0.1, 0.30 + bounce, 0.02), b = new V3(sd * 0.155, 0.22 + bounce, 0.03 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.026, 0.023, 0.021}, 6, flat(ET_B));
            ell(c, b.add(0, -0.01, 0), new V3(0.04, 0.038, 0.04), 8, 6, flat(ET_GL));
        }
        // cabeza grande
        headLook(c, 0.12, -lean * 0.3);
        c.hpv = new V3(0, 0.40, 0.0);
        V3 hc = new V3(0, 0.47 + bounce, 0.02), hr = new V3(0.15, 0.135, 0.135);
        Paint hp = (n, p) -> mix(ET_B, ET_B2, ss(0.2, 0.9, n.y) * 0.35);
        ell(c, hc, hr, 16, 11, hp);
        // púas hacia atrás
        double fl = Math.sin(t * 0.15 + ph) * 0.01 + w * 0.05;
        for (int k = 0; k < 9; k++) {
            double a = -0.9 + k * 0.225;
            V3 base = hc.add(Math.sin(a) * 0.09, 0.07 + Math.cos(a * 1.3) * 0.03, -0.06 - Math.cos(a) * 0.03);
            V3 tip = base.add(Math.sin(a) * (0.12 + fl), 0.01 - 0.045 * Math.abs(a) - fl * 0.5, -0.22 - 0.03 * Math.cos(a) - fl * 2);
            Detail.feather(c, base, tip, new V3(0, 1, 0), 0.036, new V3(Math.sin(a), 0.3, -0.5), ET_Q, ET_Q2);
        }
        for (int k = 0; k < 5; k++) {
            double a = -0.6 + k * 0.3;
            V3 base = hc.add(Math.sin(a) * 0.1, 0.1, -0.02);
            V3 tip = base.add(Math.sin(a) * 0.07, 0.02, -0.17 - fl * 2);
            Detail.feather(c, base, tip, new V3(0, 1, 0), 0.03, new V3(0, 1, -0.3), ET_Q2, scale(ET_Q2, 1.1));
        }
        // cara
        V3 fc = new V3(0, 0.44 + bounce, 0.115), fr = new V3(0.105, 0.085, 0.07);
        ell(c, fc, fr, 12, 8, flat(ET_SK));
        V3 sc = new V3(0, 0.425 + bounce, 0.19), sr = new V3(0.05, 0.04, 0.045);
        ell(c, sc, sr, 10, 7, flat(ET_SK));
        ell(c, sc.add(0, 0.02, 0.035), new V3(0.02, 0.016, 0.014), 6, 4, flat(BLACK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.07, 0, T_MOUTH, new float[]{0.4F, 0.22F, 0.2F}, 0.95, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.055, 0.485 + bounce, 0.115), er = new V3(0.06, 0.07, 0.04);
            ell(c, ec, er, 12, 8, flat(WHITE));
            decal(c, ec, er, new V3(sd * 0.05, 0.0, 1), 0.075, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? BLACK : new float[]{1F, 0.72F, 0.25F}, 1, 0.003);
            Detail.glint(c, ec, er, new V3(sd * 0.05 - 0.25, 0.4, 1), 0.016, WHITE, 0.9);
            // orejita
            ell(c, new V3(sd * 0.085, 0.585 + bounce, 0.0), new V3(0.035, 0.05, 0.02), 0, 0, sd * 0.25, 8, 6, flat(ET_B));
        }
        cheeks(c, hc, hr, 0.62, -0.25, 0.7, 0.06);
        // gafas de piloto sobre la frente
        ell(c, new V3(0, 0.555 + bounce, 0.075), new V3(0.145, 0.016, 0.11), 16, 4, flat(ET_GG));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 gl = new V3(sd * 0.06, 0.575 + bounce, 0.115);
            ell(c, gl, new V3(0.05, 0.05, 0.022), 12, 8, flat(ET_GG));
            ell(c, gl.add(0, 0, 0.012), new V3(0.038, 0.038, 0.014), 12, 6, flat(ET_LN));
            Detail.glint(c, gl.add(0, 0, 0.012), new V3(0.038, 0.038, 0.014), new V3(-0.4, 0.5, 1), 0.02, WHITE, 0.9);
        }
        c.head = false;
        // bufanda amarilla al viento
        V3 sc0 = new V3(0, 0.375 + bounce, 0.0);
        ell(c, sc0, new V3(0.105, 0.03, 0.095), 12, 4, flat(ET_SC));
        double wave = Math.sin(t * 0.25 + ph);
        V3[] sk = {sc0.add(0, 0, -0.05), sc0.add(0.0, 0.0 + 0.01 * wave, -0.15 - w * 0.05), sc0.add(0.02 * wave, -0.01 + 0.02 * wave, -0.26 - w * 0.1), sc0.add(0.04 * wave, 0.0, -0.36 - w * 0.15)};
        curve(c, sk, new double[]{0.026, 0.022, 0.016, 0.005}, 4, 6, flat(ET_SC));
        // rayitos de velocidad
        if (w > 0.2) {
            for (int k = 0; k < 3; k++) {
                double u = ((t * 0.05 + k * 0.33) % 1.0);
                Detail.twinkle(c, new V3((k - 1) * 0.12, 0.2 + k * 0.12, -0.2 - u * 0.4), 0.025, new float[]{0.7F, 0.9F, 1F}, t, ph + k, 0.4);
            }
        }
    }

    // ------------------------------------------------------------------ 3. CONEJO DE LA LUNA

    private static final float[] MB_W = hex(0xFBF7F2), MB_W2 = hex(0xE6E0F0), MB_PK = hex(0xFFB6C8), MB_PU = hex(0x7B64C8), MB_GOLD = hex(0xFFE08A),
            MB_WOOD = hex(0xB07A4A), MB_MOCHI = hex(0xFFFFFF);

    static void moonBunny(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 1.3)) * 0.07 * w + c.joy * 0.0;
        double bob = hop + 0.01 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // patas traseras grandes
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.115, 0.075 + hop * 0.5, -0.09), new V3(0.062, 0.062, 0.125), 12, 8, flat(MB_W));
            ell(c, new V3(sd * 0.115, 0.03 + hop * 0.3, -0.02), new V3(0.05, 0.022, 0.095), 10, 5, flat(MB_W2));
            ell(c, new V3(sd * 0.075, 0.03 + hop * 0.3, 0.14), new V3(0.032, 0.03, 0.045), 8, 6, flat(MB_W));
        }
        // cuerpo
        V3 bc = new V3(0, 0.23 + bob, -0.03), br = new V3(0.15, 0.16, 0.18);
        Paint bp = (n, p) -> mix(MB_W, MB_W2, ss(0.1, -0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 130, 911, 0.03, 0.016, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        // colita pompón
        ell(c, new V3(0, 0.2 + bob, -0.21), new V3(0.06, 0.06, 0.06), 10, 7, flat(MB_W));
        Detail.fur(c, new V3(0, 0.2 + bob, -0.21), new V3(0.06, 0.06, 0.06), 30, 912, 0.025, 0.012, new V3(0, 0, -1), 0.5, Detail.ALL, flat(MB_W), null);
        // faja morada con luna
        ell(c, bc.add(0, -0.02, 0), new V3(0.158, 0.035, 0.188), 16, 4, flat(MB_PU));
        // martillo de mochi en el hombro
        V3 hb = new V3(-0.125, 0.18 + bob, 0.1), ht = new V3(-0.2, 0.37 + bob, 0.02);
        tube(c, new V3[]{hb, hb.lerp(ht, 0.5), ht}, new double[]{0.014, 0.014, 0.014}, 6, flat(MB_WOOD));
        ell(c, ht.add(-0.01, 0.045, -0.005), new V3(0.075, 0.048, 0.048), 0, 0, 0.5, 10, 7, (n, p) -> MB_WOOD);
        ell(c, ht.add(-0.01, 0.045, -0.005), new V3(0.078, 0.01, 0.05), 0, 0, 0.5, 10, 3, flat(MB_GOLD));
        // bracitos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.1, 0.25 + bob, 0.1);
            V3 b = sd < 0 ? hb : new V3(0.075, 0.12 + bob, 0.19);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.027, 0.024, 0.022}, 6, bp);
            ell(c, b, new V3(0.032, 0.03, 0.032), 8, 6, flat(MB_W));
        }
        // mochi flotando
        double fy = 0.5 + 0.03 * Math.sin(t * 0.09 + ph);
        ell(c, new V3(0.17, fy + bob, 0.12), new V3(0.05, 0.034, 0.05), 10, 7, flat(MB_MOCHI));
        ell(c, new V3(0.17, fy + 0.028 + bob, 0.12), new V3(0.03, 0.02, 0.03), 8, 5, flat(MB_MOCHI));
        decal(c, new V3(0.17, fy + bob, 0.12), new V3(0.05, 0.034, 0.05), new V3(0, 0.1, 1), 0.04, 0, T_BEAD, WHITE, 1, 0.003);
        // cabeza
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.38, 0.12);
        V3 hc = new V3(0, 0.43 + bob, 0.12), hr = new V3(0.125, 0.115, 0.115);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(MB_W, MB_W2, ss(0.0, -0.9, n.y) * 0.3));
        Detail.fur(c, hc, hr, 50, 913, 0.02, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.4 ? 1 : 0.0, flat(MB_W), null);
        V3 sc = new V3(0, 0.40 + bob, 0.215), sr = new V3(0.05, 0.04, 0.05);
        ell(c, sc, sr, 10, 7, flat(MB_W));
        ell(c, sc.add(0, 0.015, 0.045), new V3(0.016, 0.012, 0.012), 6, 4, flat(MB_PK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.065, 0, T_MOUTH, new float[]{0.45F, 0.25F, 0.3F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.52, 0.16, 0.84, 0.10, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.85, -0.22, 0.5, 0.075);
        // luna creciente en la frente (dorada)
        Detail.spot(c, hc, hr, new V3(0, 0.78, 0.62), 0.07, MB_GOLD);
        Detail.spot(c, hc, hr, new V3(0.015, 0.8, 0.6), 0.058, mix(MB_W, MB_W2, 0.15));
        Detail.glint(c, hc, hr, new V3(-0.15, 0.72, 0.67), 0.03, new float[]{1F, 0.95F, 0.7F}, 0.7);
        // orejas largas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 13 : 14);
            double flop = sd < 0 ? 0.0 : 0.0;
            V3 b = new V3(sd * 0.055, 0.51 + bob, 0.08);
            V3 m = new V3(sd * (0.075 + ef * 0.02), 0.65 + bob, 0.04);
            V3 tp = new V3(sd * (0.095 + ef * 0.04), 0.76 + bob - ef * 0.02, -0.02);
            tube(c, new V3[]{b, m, tp}, new double[]{0.04, 0.036, 0.012}, 6, flat(MB_W));
            V3 b2 = b.add(0, 0.01, 0.025), m2 = m.add(0, 0, 0.026), t2 = tp.add(0, -0.03, 0.02);
            tube(c, new V3[]{b2, m2, t2}, new double[]{0.026, 0.023, 0.008}, 6, flat(MB_PK));
        }
        c.head = false;
        // polvo de luna
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.4 + u * 4) * 0.25, 0.3 + u * 0.4 + bob, Math.cos(k * 1.7) * 0.2), 0.018 * (1 - u * 0.4), new float[]{1F, 0.95F, 0.7F}, t, ph + k, 0.24);
        }
    }

    // ------------------------------------------------------------------ 1. GATO DE LA FORTUNA

    private static final float[] MN_W = hex(0xFFFDF8), MN_W2 = hex(0xEFE6D8), MN_OR = hex(0xF59A4A), MN_BK = hex(0x3A3038), MN_RED = hex(0xD8403A),
            MN_GOLD = hex(0xF6C84A), MN_PK = hex(0xFFB0B8);

    static void fortuneCat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // cola
        double wag = Math.sin(t * 0.1 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.16, -0.22), new V3(wag * 0.5, 0.17, -0.33), new V3(wag * 1.2, 0.25, -0.36), new V3(wag * 1.6, 0.34, -0.31)};
        double[] trd = {0.04, 0.04, 0.036, 0.028};
        curve(c, tcp, trd, 4, 8, (n, p) -> mix(MN_W, MN_OR, ss(0.2, 0.34, p.y) * 0.9));
        // patitas traseras sentadas
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.115, 0.075, -0.07), new V3(0.075, 0.07, 0.11), 12, 8, flat(MN_W));
            ell(c, new V3(sd * 0.085, 0.03, 0.065), new V3(0.04, 0.028, 0.06), 10, 6, flat(MN_W));
        }
        // cuerpo sentado y redondo
        V3 bc = new V3(0, 0.22 + bob, -0.02), br = new V3(0.165, 0.185, 0.19);
        Paint bp = (n, p) -> {
            double patch = ss(0.3, 0.1, Math.abs(n.x - 0.7) + Math.abs(n.y - 0.1) * 0.3);
            return mix(MN_W, MN_OR, patch * 0.0 + ss(0.4, 0.9, n.x) * ss(-0.3, 0.3, n.y) * 0.9);
        };
        ell(c, bc, br, 16, 11, bp);
        Detail.spot(c, bc, br, new V3(-0.7, 0.6, -0.4), 0.14, MN_BK);
        Detail.spot(c, bc, br, new V3(0.5, 0.9, -0.3), 0.1, MN_OR);
        // babero con campanita y moneda
        ell(c, bc.add(0, 0.075, 0.115), new V3(0.145, 0.05, 0.075), 12, 6, flat(MN_RED));
        ell(c, bc.add(0, 0.075, 0.115), new V3(0.147, 0.006, 0.077), 12, 3, flat(MN_GOLD));
        V3 bell = bc.add(0, 0.035, 0.175);
        ell(c, bell, new V3(0.04, 0.04, 0.04), 10, 7, flat(MN_GOLD));
        ell(c, bell.add(0, -0.01, 0.036), new V3(0.012, 0.008, 0.006), 6, 4, flat(scale(MN_GOLD, 0.5)));
        Detail.glint(c, bell, new V3(0.04, 0.04, 0.04), new V3(-0.4, 0.5, 1), 0.026, WHITE, 0.9);
        // brazo levantado saludando
        double wave = Math.sin(t * 0.14 + ph) * 0.5;
        V3 a = new V3(0.12, 0.28 + bob, 0.08);
        V3 m = new V3(0.2, 0.40 + bob, 0.1), tp = new V3(0.24, 0.52 + bob + 0.01 * wave, 0.11 + 0.03 * wave);
        tube(c, new V3[]{a, m, tp}, new double[]{0.046, 0.04, 0.036}, 7, flat(MN_W));
        ell(c, tp.add(0, 0.015, 0.0), new V3(0.05, 0.05, 0.045), 10, 8, flat(MN_W));
        decal(c, tp.add(0, 0.015, 0), new V3(0.05, 0.05, 0.045), new V3(0, 0, 1), 0.04, 0, Detail.T_DISC, MN_PK, 1, 0.004);
        // otro brazo con moneda grande dorada
        V3 a2 = new V3(-0.12, 0.26 + bob, 0.1), b2 = new V3(-0.1, 0.17 + bob, 0.21);
        tube(c, new V3[]{a2, a2.lerp(b2, 0.5), b2}, new double[]{0.04, 0.036, 0.034}, 7, flat(MN_W));
        ell(c, b2, new V3(0.042, 0.04, 0.04), 8, 6, flat(MN_W));
        V3 coin = new V3(-0.1, 0.235 + bob, 0.22);
        ell(c, coin, new V3(0.085, 0.105, 0.015), 14, 4, flat(MN_GOLD));
        ell(c, coin.add(0, 0, 0.012), new V3(0.06, 0.08, 0.006), 12, 3, flat(scale(MN_GOLD, 1.12)));
        Detail.spot(c, coin, new V3(0.085, 0.105, 0.015), new V3(0, 0, 1), 0.06, MN_RED);
        Detail.glint(c, coin, new V3(0.085, 0.105, 0.015), new V3(-0.4, 0.4, 1), 0.04, WHITE, 0.9);
        // cabeza
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.40, 0.14);
        V3 hc = new V3(0, 0.50 + bob, 0.15), hr = new V3(0.155, 0.125, 0.135);
        ell(c, hc, hr, 16, 11, (n, p) -> MN_W);
        Detail.spot(c, hc, hr, new V3(0.65, 0.75, 0.1), 0.12, MN_OR);
        Detail.spot(c, hc, hr, new V3(-0.4, 0.9, -0.1), 0.1, MN_BK);
        V3 sc = new V3(0, 0.465 + bob, 0.255), sr = new V3(0.05, 0.035, 0.045);
        ell(c, sc, sr, 10, 7, flat(MN_W2));
        ell(c, sc.add(0, 0.015, 0.04), new V3(0.016, 0.012, 0.01), 6, 4, flat(MN_PK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.075, 0, T_CAT, WHITE, 0.9, 0.003);
        eyes(c, hc, hr, 0.50, 0.16, 0.84, 0.095, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.075);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 15 : 16) * 0.03;
            V3 b = new V3(sd * 0.1, 0.595 + bob, 0.12), tpe = new V3(sd * (0.14 + ef), 0.71 + bob - ef * 2, 0.09);
            tube(c, new V3[]{b, b.lerp(tpe, 0.5), tpe}, new double[]{0.06, 0.044, 0.006}, 4, flat(sd > 0 ? MN_OR : MN_W));
            V3 eb2 = b.add(0, 0.005, 0.02), et2 = tpe.add(sd * -0.006, -0.04, 0.02);
            tube(c, new V3[]{eb2, eb2.lerp(et2, 0.5), et2}, new double[]{0.04, 0.028, 0.004}, 4, flat(MN_PK));
            for (int k = -1; k <= 1; k += 2) {
                Detail.whisker(c, new V3(sd * 0.04, 0.465 + bob + k * 0.012, 0.28), new V3(sd * 0.14, 0.47 + bob + k * 0.035, 0.25), -0.006, 0.0022, new float[]{1, 1, 1});
            }
        }
        c.head = false;
        // monedas que brillan
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.013 + ph * 0.3 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.2 + u * 4) * 0.28, 0.25 + u * 0.4 + bob, 0.1 + Math.cos(k * 1.8) * 0.15), 0.02 * (1 - u * 0.4), new float[]{1F, 0.88F, 0.45F}, t, ph + k, 0.27);
        }
    }

    // ------------------------------------------------------------------ 26. PERRITO NINJA

    private static final float[] NJ_F = hex(0xF0A04C), NJ_F2 = hex(0xFFC27A), NJ_CR = hex(0xFFF3E0), NJ_BL = hex(0x2E3A66), NJ_BL2 = hex(0x4A5A9A),
            NJ_ST = hex(0xD6DCE8), NJ_RED = hex(0xD8403A);

    static void ninjaDog(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.26, 0);
        // cola enroscada
        double wag = Math.sin(t * 0.16 + ph) * 0.03 + Math.sin(c.gait) * 0.02 * w;
        V3[] tcp = {new V3(0, 0.30 + bob, -0.22), new V3(wag * 0.4, 0.37 + bob, -0.29), new V3(wag, 0.46 + bob, -0.27), new V3(wag * 1.2, 0.50 + bob, -0.19), new V3(wag, 0.46 + bob, -0.15)};
        double[] trd = {0.036, 0.05, 0.055, 0.048, 0.034};
        Paint tp = (n, p) -> mix(NJ_F, NJ_CR, ss(0.45, 0.5, p.y - bob));
        curve(c, tcp, trd, 4, 8, tp);
        Detail.furTube(c, tcp, trd, 40, 921, 0.03, 0.016, 0.7, tp, null);
        legs4(c, 0.08, 0.13, -0.15, 0.2 + bob, 0.04, 0.035, 0.06, (n, p) -> mix(NJ_F, NJ_BL, ss(0.1, 0.0, p.y) * 0.9), flat(NJ_BL));
        // cuerpo
        V3 bc = new V3(0, 0.285 + bob, -0.02), br = new V3(0.125, 0.115, 0.22);
        Paint bp = (n, p) -> mix(mix(NJ_F, NJ_F2, ss(0.0, 0.9, n.y) * 0.4), NJ_CR, ss(-0.2, -0.8, n.y) * 0.9);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 110, 922, 0.026, 0.014, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        // bufanda larga azul ondeando
        ell(c, new V3(0, 0.30 + bob, 0.15), new V3(0.115, 0.08, 0.06), 12, 7, flat(NJ_BL));
        double wv = Math.sin(t * 0.22 + ph);
        V3 sb = new V3(-0.04, 0.34 + bob, 0.1);
        V3[] sk = {sb, sb.add(-0.04, 0.0 + 0.01 * wv, -0.15 - w * 0.06), sb.add(-0.06 - 0.02 * wv, 0.01 + 0.02 * wv, -0.32 - w * 0.1), sb.add(-0.07 - 0.04 * wv, 0.0, -0.46 - w * 0.14)};
        curve(c, sk, new double[]{0.03, 0.026, 0.02, 0.006}, 4, 6, flat(NJ_BL2));
        // cabeza de shiba
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.37, 0.17);
        V3 hc = new V3(0, 0.45 + bob, 0.25), hr = new V3(0.15, 0.125, 0.135);
        Paint hp = (n, p) -> mix(NJ_F, NJ_F2, ss(0.0, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 60, 923, 0.026, 0.013, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.55 ? 1 : 0.0, hp, null);
        // máscara crema en las mejillas y hocico
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.07, 0.415 + bob, 0.31), new V3(0.065, 0.055, 0.06), 0, 0, sd * 0.2, 10, 7, flat(NJ_CR));
        }
        V3 sc = new V3(0, 0.425 + bob, 0.355), sr = new V3(0.065, 0.05, 0.07);
        ell(c, sc, sr, 10, 7, flat(NJ_CR));
        ell(c, sc.add(0, 0.022, 0.065), new V3(0.026, 0.019, 0.02), 8, 5, flat(BLACK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.08, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.50, 0.20, 0.84, 0.09, T_BEAD, new float[]{1, 1, 1});
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.spot(c, hc, hr, new V3(sd * 0.5, 0.52, 0.68), 0.03, NJ_CR);
        }
        cheeks(c, hc, hr, 0.86, -0.2, 0.45, 0.075);
        // orejitas triangulares
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 17 : 18) * 0.03;
            V3 b = new V3(sd * 0.085, 0.535 + bob, 0.22), tpe = new V3(sd * (0.115 + ef), 0.66 + bob - ef * 2, 0.2);
            tube(c, new V3[]{b, b.lerp(tpe, 0.5), tpe}, new double[]{0.056, 0.04, 0.005}, 4, flat(NJ_F));
            V3 b2 = b.add(0, 0.004, 0.02), t2 = tpe.add(0, -0.04, 0.018);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.034, 0.024, 0.004}, 4, flat(NJ_CR));
        }
        // cinta ninja con placa metálica
        double bh = 0.50 + bob;
        V3 bnd = new V3(0, bh, 0.25);
        ell(c, new V3(0, 0.505 + bob, 0.235), new V3(0.152, 0.03, 0.14), 16, 4, flat(NJ_BL));
        V3 pl = new V3(0, 0.505 + bob, 0.375);
        ell(c, pl, new V3(0.052, 0.034, 0.008), 10, 4, flat(NJ_ST));
        Detail.spot(c, pl, new V3(0.052, 0.034, 0.008), new V3(0, 0, 1), 0.025, NJ_RED);
        Detail.glint(c, pl, new V3(0.052, 0.034, 0.008), new V3(-0.3, 0.3, 1), 0.03, WHITE, 0.8);
        // colitas de la cinta
        double bw = Math.sin(t * 0.2 + ph) * 0.02;
        for (int k = 0; k < 2; k++) {
            V3 a = new V3(-0.1, 0.5 + bob, 0.16);
            tube(c, new V3[]{a, a.add(-0.06, 0.0 - k * 0.04 + bw, -0.07), a.add(-0.1 - k * 0.02, -0.03 - k * 0.08 + bw * 2, -0.14)}, new double[]{0.014, 0.012, 0.005}, 5, flat(NJ_BL2));
        }
        c.head = false;
        // shuriken flotante
        double sp = t * 0.3;
        V3 shc = new V3(0.22, 0.52 + 0.02 * Math.sin(t * 0.07 + ph) + bob, 0.1);
        for (int k = 0; k < 4; k++) {
            double a = sp + k * Math.PI / 2;
            Detail.kite(c, shc, shc.add(Math.cos(a) * 0.07, Math.sin(a) * 0.07, 0), new V3(-Math.sin(a), Math.cos(a), 0), 0.02, new V3(0, 0, 1), NJ_ST, WHITE);
        }
        ell(c, shc, new V3(0.014, 0.014, 0.01), 6, 4, flat(NJ_RED));
    }

    // ------------------------------------------------------------------ 36. CALAMAR PINTOR

    private static final float[] SQ_P = hex(0xB86CE0), SQ_P2 = hex(0xE29AF0), SQ_CY = hex(0x4FD6F0), SQ_YE = hex(0xFFE04A), SQ_PK = hex(0xFF5FA8), SQ_BE = hex(0xE04A5A),
            SQ_WOOD = hex(0xB0804A);

    static void squidPainter(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.015 * w + 0.012 * c.breath;
        c.pivot = new V3(0, 0.25, 0);
        // seis tentáculos, uno con pincel
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI * 2 / 6 + 0.3;
            double ca = Math.cos(a), sa = Math.sin(a);
            double sw = Math.sin(c.gait * 1.2 + k * 1.3) * 0.05 * w + Math.sin(t * 0.07 + ph + k) * 0.015;
            V3 b = new V3(ca * 0.08, 0.15 + bob, sa * 0.08);
            V3 m = new V3(ca * 0.17, 0.09 + bob * 0.3, sa * 0.17 + sw);
            V3 e = new V3(ca * 0.23, 0.02, sa * 0.23 + sw * 1.4);
            if (k == 0) {
                m = new V3(0.17, 0.2 + bob, 0.12);
                e = new V3(0.24, 0.31 + bob + 0.015 * Math.sin(t * 0.12 + ph), 0.2 + 0.03 * Math.sin(t * 0.12 + ph));
            }
            curve(c, new V3[]{b, m, e}, new double[]{0.035, 0.028, 0.012}, 4, 7, (n, p) -> mix(SQ_P, SQ_P2, ss(0.0, 0.3, p.y) * 0.4));
            ell(c, e, new V3(0.016, 0.014, 0.016), 6, 4, flat(SQ_P2));
            if (k == 0) {
                V3 dir = new V3(0.05, 0.12, 0.09).norm();
                tube(c, new V3[]{e, e.add(dir.mul(0.12))}, new double[]{0.008, 0.008}, 5, flat(SQ_WOOD));
                tube(c, new V3[]{e.add(dir.mul(0.12)), e.add(dir.mul(0.16))}, new double[]{0.012, 0.004}, 6, flat(SQ_PK));
                Detail.twinkle(c, e.add(dir.mul(0.17)), 0.016, new float[]{1F, 0.5F, 0.8F}, t, ph, 0.3);
            }
        }
        // cuerpo (manto alargado)
        V3 mc = new V3(0, 0.31 + bob, 0), mr = new V3(0.15, 0.2, 0.14);
        Paint mp = (n, p) -> mix(SQ_P, SQ_P2, ss(-0.3, 0.9, n.y) * 0.5);
        ell(c, mc, mr, 16, 11, mp);
        // manchas de pintura
        double[][] sp = {{0.8, 0.1, 0.5, 0.11, 0}, {-0.7, 0.3, 0.6, 0.09, 1}, {0.4, 0.55, -0.7, 0.12, 2}, {-0.5, 0.0, -0.8, 0.1, 0}, {0.9, -0.3, -0.1, 0.07, 1}, {-0.1, 0.8, 0.5, 0.08, 2}};
        float[][] cols = {SQ_CY, SQ_YE, SQ_PK};
        for (double[] s : sp) {
            Detail.spot(c, mc, mr, new V3(s[0], s[1], s[2]), s[3], cols[(int) s[4]]);
            Detail.spot(c, mc, mr, new V3(s[0] + 0.15, s[1] - 0.1, s[2] + 0.1), s[3] * 0.4, cols[(int) s[4]]);
        }
        // cabeza/cara al frente + boina
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.30, 0.12);
        V3 fc = new V3(0, 0.27 + bob, 0.1);
        eyes(c, mc, mr, 0.40, 0.0, 0.92, 0.115, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, mc, mr, 0.72, -0.25, 0.7, 0.085);
        decal(c, mc, mr, new V3(0, -0.28, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.35F}, 0.95, 0.003);
        // boina roja ladeada
        V3 bt = new V3(0.02, 0.495 + bob, 0.0);
        Detail.cap(c, bt, new V3(0.17, 0.065, 0.16), 0, 0, 0.18, 1.55, 0.0, 16, 5, (n, p) -> mix(SQ_BE, scale(SQ_BE, 1.25), ss(-0.2, 0.9, n.y)));
        ell(c, bt.add(0.01, 0.06, 0), new V3(0.016, 0.02, 0.016), 6, 4, flat(scale(SQ_BE, 0.8)));
        c.head = false;
        // gotitas de pintura que flotan
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(0.26 + Math.sin(k * 2.3 + u * 3) * 0.06, 0.3 + u * 0.3 + bob, 0.22 + Math.cos(k * 1.9) * 0.05), 0.018 * (1 - u * 0.4), cols[k % 3], t, ph + k, 0.27);
        }
    }

    // ------------------------------------------------------------------ 39. ESPÍRITU FAROLITO

    private static final float[] LN_R = hex(0xE8503A), LN_R2 = hex(0xFF9A5A), LN_BK = hex(0x3A2A2E), LN_GOLD = hex(0xF2C14E), LN_FL = hex(0xFFE9A0);

    static void lanternSpirit(Ctx c) {
        double t = c.t, ph = c.ph;
        double fl = Math.sin(t * 0.08 + ph);
        double bob = 0.02 * fl + 0.008 * c.breath;
        c.pivot = new V3(0, 0.35, 0);
        double flick = 0.85 + 0.15 * Math.sin(t * 0.33 + ph * 3) * Math.sin(t * 0.17 + ph);
        // farolillo de papel: cuerpo con costillas
        V3 bc = new V3(0, 0.40 + bob, 0), br = new V3(0.185, 0.21, 0.185);
        Paint bp = (n, p) -> {
            double rib = Math.abs(Math.sin(Math.atan2(p.z, p.x) * 6)) > 0.93 ? 0.55 : 1.0;
            return mix(scale(LN_R, rib), LN_R2, ss(-0.4, 0.9, n.y) * 0.5 * rib);
        };
        c.emissive = false;
        ell(c, bc, br, 18, 11, bp);
        // luz interior
        boolean em = c.emissive;
        c.emissive = true;
        glow(c, bc, 0.5 * flick, new float[]{1F, 0.75F, 0.3F}, 0.55);
        glow(c, bc, 0.28 * flick, new float[]{1F, 0.95F, 0.7F}, 0.7);
        c.emissive = em;
        // tapas negras y dorado
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bc.add(0, sd * 0.2, 0), new V3(0.105, 0.032, 0.105), 14, 4, flat(LN_BK));
            ell(c, bc.add(0, sd * 0.178, 0), new V3(0.115, 0.01, 0.115), 14, 3, flat(LN_GOLD));
        }
        // asa y borla
        tube(c, new V3[]{bc.add(-0.07, 0.225, 0), bc.add(0, 0.33 + 0.01 * fl, 0), bc.add(0.07, 0.225, 0)}, new double[]{0.008, 0.008, 0.008}, 5, flat(LN_GOLD));
        V3 tb = bc.add(0, -0.225, 0);
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5, sw = Math.sin(t * 0.1 + ph + k) * 0.01;
            tube(c, new V3[]{tb, tb.add(Math.cos(a) * 0.015 + sw, -0.05, Math.sin(a) * 0.015), tb.add(Math.cos(a) * 0.03 + sw * 2, -0.11, Math.sin(a) * 0.03)}, new double[]{0.008, 0.006, 0.003}, 4, flat(LN_GOLD));
        }
        ell(c, tb.add(0, -0.02, 0), new V3(0.018, 0.024, 0.018), 8, 5, flat(LN_GOLD));
        // carita brillante
        headLook(c, 0.08, 0.0);
        c.hpv = bc;
        float[] eyeC = {1F, 0.97F, 0.8F};
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, bc, br, new V3(sd * 0.33, 0.06, 0.93), 0.10, sd * 0.1, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? LN_BK : LN_BK, 1, 0.004);
            Detail.glint(c, bc, br, new V3(sd * 0.33 - 0.12, 0.2, 0.94), 0.025, WHITE, 0.9);
        }
        cheeks(c, bc, br, 0.7, -0.2, 0.7, 0.08);
        decal(c, bc, br, new V3(0, -0.2, 1), 0.06, 0, T_MOUTH, new float[]{0.4F, 0.15F, 0.1F}, 0.95, 0.004);
        c.head = false;
        // bracitos de papel
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.09 + ph + sd * 1.5) * 0.03;
            V3 a = new V3(sd * 0.17, 0.38 + bob, 0.02), b = new V3(sd * 0.27, 0.43 + bob + sw, 0.06);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.022, 0.02, 0.018}, 6, flat(LN_R2));
            ell(c, b, new V3(0.03, 0.03, 0.03), 8, 6, flat(LN_R2));
        }
        // chispas de brasa
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.016 + ph * 0.2 + k * 0.17) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 3) * 0.2, 0.55 + u * 0.45 + bob, Math.cos(k * 1.6 + u * 2) * 0.18), 0.02 * (1 - u * 0.5), new float[]{1F, 0.7F, 0.3F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 40. PEREZOSO DE HOJAS

    private static final float[] SL_F = hex(0x9A8264), SL_F2 = hex(0xC2A888), SL_CR = hex(0xE8DAB8), SL_DK = hex(0x4A382E), SL_MO = hex(0x6FA05A), SL_MO2 = hex(0x9CCB70),
            SL_FL = hex(0xFF9CC0), SL_CL = hex(0xEAE0C8);

    static void sloth(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.7)) * 0.01 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.25, 0);
        // patas traseras cortas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 0.7 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.04 * w;
            V3 a = new V3(sd * 0.09, 0.16 + bob, -0.12), b = new V3(sd * 0.1, 0.03, -0.1 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.05, 0.042, 0.04}, 7, (n, q) -> SL_F);
            ell(c, b.add(0, -0.008, 0.03), new V3(0.05, 0.03, 0.07), 10, 6, flat(SL_F2));
        }
        // cuerpo peludo con musgo
        V3 bc = new V3(0, 0.27 + bob, -0.03), br = new V3(0.16, 0.16, 0.2);
        Paint bp = (n, p) -> mix(SL_F, SL_F2, ss(0.0, -0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 200, 931, 0.06, 0.022, new V3(0, -1, -0.2), 0.9, Detail.ALL, bp, null);
        Detail.fur(c, bc, br, 70, 932, 0.07, 0.03, new V3(0, 0.1, -1), 0.5, Detail.TOP, (n, p) -> SL_MO, SL_MO2);
        // flor en el lomo
        V3 fc = bc.add(0.07, 0.15, -0.05);
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5;
            Detail.kite(c, fc, fc.add(Math.cos(a) * 0.045, 0.01, Math.sin(a) * 0.045), new V3(-Math.sin(a), 0, Math.cos(a)), 0.02, new V3(0, 1, 0), SL_FL, scale(SL_FL, 1.15));
        }
        ell(c, fc.add(0, 0.008, 0), new V3(0.014, 0.012, 0.014), 6, 4, flat(hex(0xFFE08A)));
        // brazos largos abrazando una ramita
        double sway = Math.sin(t * 0.04 + ph) * 0.01;
        V3 br0 = new V3(-0.04, 0.2 + bob, 0.25), br1 = new V3(0.17, 0.215 + bob, 0.245 + sway);
        tube(c, new V3[]{br0, br0.lerp(br1, 0.5), br1}, new double[]{0.014, 0.014, 0.012}, 5, flat(SL_DK));
        for (int k = 0; k < 2; k++) {
            V3 lb = br1.add(0.0, 0.0, 0.0);
            Detail.feather(c, lb.add(-0.1 + k * 0.06, 0.0, 0.0), lb.add(-0.03 + k * 0.07, 0.07 + 0.01 * k, 0.04), new V3(0, 0, 1), 0.035, new V3(0, 1, 0), SL_MO, SL_MO2);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            double swing = Math.sin(c.gait * 0.7 + (sd > 0 ? 0 : Math.PI)) * 0.05 * w;
            V3 a = new V3(sd * 0.12, 0.32 + bob, 0.1), b = new V3(sd * 0.1, 0.205 + bob, 0.24 + swing);
            tube(c, new V3[]{a, a.add(sd * 0.04, -0.07, 0.07), b}, new double[]{0.04, 0.036, 0.032}, 7, bp);
            ell(c, b, new V3(0.034, 0.032, 0.04), 8, 6, flat(SL_F2));
            for (int k = -1; k <= 1; k++) {
                tube(c, new V3[]{b.add(k * 0.016, -0.005, 0.03), b.add(k * 0.018, -0.02, 0.07)}, new double[]{0.008, 0.003}, 4, flat(SL_CL));
            }
        }
        // cabeza redonda con antifaz oscuro
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.38, 0.15);
        V3 hc = new V3(0, 0.40 + bob, 0.20), hr = new V3(0.13, 0.115, 0.115);
        Paint hp = (n, p) -> mix(SL_CR, SL_F2, ss(0.2, 0.9, n.y) * 0.6);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 933, 0.035, 0.016, new V3(0, -0.8, -0.3), 0.8, d -> d.z < 0.6 ? 1 : 0.0, hp, null);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.05, 0.405 + bob, 0.285), new V3(0.04, 0.055, 0.03), 0, 0, sd * -0.5, 10, 7, flat(SL_DK));
        }
        eyes(c, hc, hr, 0.40, 0.04, 0.9, 0.075, T_BEAD, new float[]{1, 1, 1});
        ell(c, new V3(0, 0.375 + bob, 0.315), new V3(0.026, 0.02, 0.016), 8, 5, flat(SL_DK));
        decal(c, hc, hr, new V3(0, -0.45, 1), 0.085, 0, T_MOUTH, new float[]{0.3F, 0.2F, 0.18F}, 0.95, 0.003);
        cheeks(c, hc, hr, 0.82, -0.22, 0.52, 0.07);
        c.head = false;
        // zzz de luz
        double u = ((t * 0.01 + ph) % 1.0);
        Detail.twinkle(c, new V3(0.15 + u * 0.1, 0.55 + u * 0.15 + bob, 0.2), 0.02 * (1 - u * 0.4), new float[]{0.8F, 1F, 0.8F}, t, ph, 0.2);
    }
}
