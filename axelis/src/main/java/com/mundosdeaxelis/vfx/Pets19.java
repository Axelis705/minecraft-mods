package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas de los jefes de las mazmorras: Brotecito Rey (del Rey Brote) y Yunquito Ardiente (del Gran Herrero).
 * Solo caen de ellos; no salen en cajas.
 */
final class Pets19 {
    private Pets19() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 63. BROTECITO REY

    private static final float[] BR_G = hex(0x5CD66C), BR_G2 = hex(0xA6F2A4), BR_DK = hex(0x2F9A4E), BR_LF = hex(0x7BE070), BR_LF2 = hex(0xC4F7A0),
            BR_GOLD = hex(0xFFD24A), BR_GOLD2 = hex(0xFFEFA0), BR_PK = hex(0xFF9EC4), BR_CR = hex(0xFFF6DC), BR_SEED = hex(0xFFE27A);

    static void sproutKing(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait)) * 0.05 * w;
        double sq = 1.0 + 0.035 * c.breath - hop * 0.6;
        c.pivot = new V3(0, 0.15, 0);
        // dos patitas redondas de hoja
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.03 * w;
            ell(c, new V3(sd * 0.075, 0.03 + hop * 0.4, 0.03 + sw), new V3(0.05, 0.03, 0.06), 12, 8, (n, q) -> mix(BR_DK, BR_G, ss(-0.4, 1.0, n.y) * 0.8F));
        }
        // cuerpo de gelatina verde
        V3 bc = new V3(0, 0.17 + hop, 0), br = new V3(0.165 / sq, 0.145 * sq, 0.155 / sq);
        Paint bp = (n, p) -> mix(BR_G, BR_G2, ss(0.0, 1.0, n.y) * 0.75F + ss(0.4, 1.0, n.x * 0.5 + n.z * 0.5) * 0.15F);
        ell(c, bc, br, 26, 18, bp);
        // brillo de gelatina
        decal(c, bc, br, new V3(-0.45, 0.62, 0.62), 0.09, 0.0, Detail.T_DISC, new float[]{1F, 1F, 1F}, 0.55, 0.004);
        // musgo y flores
        ell(c, bc.add(0.115, -0.03, 0.05), new V3(0.04, 0.05, 0.06), 0.0, 0.0, 0.3, 10, 8, flat(BR_DK));
        ell(c, bc.add(-0.12, 0.0, -0.03), new V3(0.04, 0.045, 0.055), 0.0, 0.0, -0.3, 10, 8, flat(BR_DK));
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5;
            ell(c, bc.add(-0.125 + Math.cos(a) * 0.022, -0.045 + Math.sin(a) * 0.022, 0.07), new V3(0.012, 0.012, 0.008), 6, 5, flat(BR_PK));
        }
        ell(c, bc.add(-0.125, -0.045, 0.075), new V3(0.011, 0.011, 0.009), 6, 5, flat(BR_GOLD));
        // semilla dorada que brilla en la pancita
        boolean em0 = c.emissive;
        c.emissive = true;
        double pulse = 1.0 + 0.1 * Math.sin(t * 0.12 + ph);
        V3 sd0 = bc.add(0.0, -0.105, 0.125);
        ell(c, sd0, new V3(0.034, 0.034, 0.012), 10, 8, flat(BR_DK));
        ell(c, sd0.add(0, 0, 0.006), new V3(0.022 * pulse, 0.028 * pulse, 0.012), 10, 8, flat(BR_SEED));
        c.emissive = em0;
        glow(c, sd0.add(0, 0, 0.012), 0.11, new float[]{1F, 0.9F, 0.4F}, 0.3 * pulse);
        // cabeza = parte alta de la gelatina, con carita
        headLook(c, 0.08, 0.0);
        c.hpv = new V3(0, 0.22, 0.05);
        V3 hc = bc.add(0, 0.02, 0.0), hr = new V3(br.x * 1.0, br.y * 1.0, br.z * 1.0);
        decal(c, hc, hr, new V3(0.34, 0.12, 0.93), 0.1, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.005);
        decal(c, hc, hr, new V3(-0.34, 0.12, 0.93), 0.1, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.005);
        decal(c, hc, hr, new V3(0, -0.16, 1), 0.07, 0.0, T_MOUTH, new float[]{0.3F, 0.15F, 0.2F}, 0.95, 0.006);
        cheeks(c, hc, hr, 0.62, -0.1, 0.78, 0.06);
        // corona dorada con puntas y brote en el centro
        V3 cb = bc.add(0, 0.138 * sq, 0.0);
        ell(c, cb, new V3(0.085, 0.022, 0.085), 18, 8, (n, q) -> mix(BR_GOLD, BR_GOLD2, ss(0.0, 1.0, n.y) * 0.6F));
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI * 2 / 6;
            V3 b0 = cb.add(Math.cos(a) * 0.078, 0.012, Math.sin(a) * 0.078);
            boolean em = c.emissive;
            c.emissive = true;
            cone(c, b0, b0.add(Math.cos(a) * 0.006, 0.05, Math.sin(a) * 0.006), 0.018, 0.0, 5, flat(BR_GOLD2));
            ell(c, b0.add(Math.cos(a) * 0.006, 0.05, Math.sin(a) * 0.006), new V3(0.011, 0.011, 0.011), 6, 5, flat(BR_PK));
            c.emissive = em;
        }
        double sway = Math.sin(t * 0.07 + ph) * 0.012;
        V3[] stem = {cb.add(0, 0.0, 0), cb.add(sway * 0.4, 0.05, 0), cb.add(sway, 0.1, 0)};
        tube(c, stem, new double[]{0.014, 0.012, 0.01}, 6, (n, q) -> mix(BR_DK, BR_G, 0.6F));
        V3 top = stem[2];
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, top.add(sd * 0.05, 0.015, 0.0), new V3(0.058, 0.014, 0.03), 0.0, 0.0, sd * -0.45 + sway * 4 * sd, 14, 8,
                    (n, q) -> mix(BR_LF, BR_LF2, ss(0.0, 1.0, n.y) * 0.7F));
        }
        ell(c, top.add(0, 0.035, 0.0), new V3(0.026, 0.04, 0.02), 0.0, 0.0, 0.0, 10, 7, (n, q) -> mix(BR_LF, BR_LF2, ss(-0.2, 1.0, n.y) * 0.7F));
        c.head = false;
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.011 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.2 + u * 0.35, Math.cos(k * 1.7) * 0.17), 0.018 * (1.1 - u * 0.5),
                    new float[]{1F, 0.95F, 0.55F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 64. GRAN HERRERITO (el Gran Herrero en chiquito)

    private static final float[] GH_ST = hex(0x9A9CA8), GH_ST2 = hex(0xD2D4DE), GH_DK = hex(0x5A5C68), GH_DK2 = hex(0x3C3E48), GH_VIS = hex(0x1E1E26),
            GH_HOT = hex(0xFFB020), GH_HOT2 = hex(0xFFE060), GH_BR = hex(0x7A4A2A), GH_BR2 = hex(0xB07848), GH_BZ = hex(0xB87A3C), GH_BZ2 = hex(0xE0A862),
            GH_CU = hex(0xE0A040);

    static void emberAnvil(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.016 * w + 0.006 * c.breath;
        double pulse = 0.85 + 0.15 * Math.sin(t * 0.1 + ph);
        c.pivot = new V3(0, 0.2, 0);
        Paint steel = (n, p) -> mix(GH_ST, GH_ST2, ss(0.1, 1.0, n.y) * 0.7F);
        Paint dark = (n, p) -> mix(GH_DK2, GH_DK, ss(-0.2, 1.0, n.y) * 0.8F);
        // botas redondas de hierro
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.04 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
            ell(c, new V3(sd * 0.08, 0.04 + lift, 0.02 + sw), new V3(0.062, 0.042, 0.08), 12, 8, dark);
            ell(c, new V3(sd * 0.08, 0.075 + lift, 0.0 + sw), new V3(0.058, 0.02, 0.058), 10, 6, steel);
        }
        // torso ancho de armadura oscura
        V3 bc = new V3(0, 0.17 + bob, 0), br = new V3(0.16, 0.115, 0.125);
        ell(c, bc, br, 22, 14, (n, q) -> mix(GH_DK2, GH_DK, ss(-0.2, 1.0, n.y) * 0.6F));
        // costillas al rojo vivo en el pecho
        boolean em = c.emissive;
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            double yy = 0.07 - k * 0.026;
            double ww = 0.072 - Math.abs(k - 1) * 0.008;
            ell(c, bc.add(0, yy, 0.112 - k * 0.003), new V3(ww, 0.009, 0.02), 12, 6, (n, q) -> mix(GH_HOT, GH_HOT2, (float) (ss(0.2, 1.0, n.y) * pulse)));
        }
        ell(c, bc.add(0, 0.03, 0.108), new V3(0.012, 0.062, 0.02), 6, 8, flat(GH_HOT));
        c.emissive = em;
        glow(c, bc.add(0, 0.03, 0.12), 0.2, new float[]{1F, 0.65F, 0.15F}, 0.35 * pulse);
        // cinturón de cuero con hebilla de cobre y placa de delantal remachada
        ell(c, bc.add(0, -0.05, 0), new V3(0.163, 0.022, 0.13), 22, 8, (n, q) -> mix(GH_BR, GH_BR2, ss(0.0, 1.0, n.y) * 0.7F));
        ell(c, bc.add(0, -0.05, 0.128), new V3(0.03, 0.02, 0.012), 10, 8, flat(GH_CU));
        V3 ap = bc.add(0, -0.1, 0.11);
        ell(c, ap, new V3(0.09, 0.075, 0.02), 14, 9, steel);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                ell(c, ap.add(sx * 0.065, sy * 0.045, 0.016), new V3(0.01, 0.01, 0.008), 6, 5, flat(GH_DK2));
            }
        }
        // yunquito en la espalda
        V3 ab = bc.add(0, 0.03, -0.14);
        ell(c, ab.add(0, -0.03, 0), new V3(0.05, 0.015, 0.035), 12, 6, dark);
        ell(c, ab.add(0, 0.0, 0), new V3(0.025, 0.02, 0.022), 10, 6, dark);
        ell(c, ab.add(0, 0.028, 0), new V3(0.06, 0.02, 0.04), 12, 7, steel);
        ell(c, ab.add(-0.07, 0.028, 0), new V3(0.03, 0.014, 0.02), 8, 6, steel);
        // hombreras con postes que acaban en llama
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 pc = bc.add(sd * 0.165, 0.1, 0);
            ell(c, pc, new V3(0.07, 0.05, 0.078), 14, 9, steel);
            ell(c, pc.add(0, 0.03, 0), new V3(0.05, 0.02, 0.058), 10, 6, dark);
            V3 s0 = pc.add(sd * 0.015, 0.04, 0.0), s1 = s0.add(0, 0.1, 0.0);
            cone(c, s0, s1, 0.017, 0.017, 7, steel);
            boolean e2 = c.emissive;
            c.emissive = true;
            double fl = Math.sin(t * 0.2 + ph + sd) * 0.008;
            cone(c, s1, s1.add(sd * 0.003, 0.06 + fl, 0.0), 0.02, 0.0, 6, flat(GH_HOT2));
            c.emissive = e2;
            glow(c, s1.add(0, 0.03, 0), 0.13, new float[]{1F, 0.6F, 0.2F}, 0.4 * pulse);
        }
        // brazos con puños de metal fundido
        for (int sd = -1; sd <= 1; sd += 2) {
            double wv = Math.sin(c.gait + sd) * 0.03 * w;
            V3 sh = bc.add(sd * 0.16, 0.06, 0.02);
            V3 hd = new V3(sd * 0.215, 0.115 + bob, 0.065 + wv);
            ell(c, sh.lerp(hd, 0.5), new V3(0.048, 0.075, 0.048), 0.0, 0.0, sd * -0.4, 10, 7, steel);
            ell(c, hd.add(0, 0.045, -0.005), new V3(0.052, 0.02, 0.052), 10, 6, dark);
            boolean e2 = c.emissive;
            c.emissive = true;
            ell(c, hd, new V3(0.065, 0.062, 0.065), 14, 10, (n, q) -> mix(GH_HOT, GH_HOT2, (float) (ss(-0.2, 1.0, n.y) * 0.8 * pulse)));
            c.emissive = e2;
            glow(c, hd, 0.2, new float[]{1F, 0.6F, 0.15F}, 0.35 * pulse);
        }
        // cabeza: casco de hierro con visor, cuernos de bronce y ojitos dentro
        headLook(c, 0.06, 0.0);
        c.hpv = new V3(0, 0.3, 0.05);
        V3 hc = new V3(0, 0.335 + bob, 0.02), hr = new V3(0.135, 0.12, 0.12);
        ell(c, hc, hr, 22, 14, (n, q) -> mix(GH_DK, GH_ST, ss(0.0, 1.0, n.y) * 0.6F));
        ell(c, hc.add(0, 0.095, -0.02), new V3(0.075, 0.03, 0.09), 12, 7, steel);
        ell(c, hc.add(0, 0.0, 0.0), new V3(0.138, 0.012, 0.123), 22, 6, steel);
        ell(c, hc.add(0, -0.01, 0.1), new V3(0.095, 0.05, 0.03), 14, 9, flat(GH_VIS));
        boolean e3 = c.emissive;
        c.emissive = true;
        ell(c, hc.add(0, 0.04, 0.106), new V3(0.09, 0.008, 0.016), 14, 5, (n, q) -> mix(GH_HOT, GH_HOT2, (float) pulse));
        c.emissive = e3;
        glow(c, hc.add(0, 0.04, 0.125), 0.14, new float[]{1F, 0.7F, 0.2F}, 0.3 * pulse);
        decal(c, hc, hr, new V3(0.3, -0.07, 0.95), 0.075, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.026);
        decal(c, hc, hr, new V3(-0.3, -0.07, 0.95), 0.075, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.026);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 c0 = hc.add(sd * 0.08, 0.1, 0.0), c1 = hc.add(sd * 0.165, 0.15, 0.0), c2 = hc.add(sd * 0.175, 0.235, 0.0);
            cone(c, c0, c1, 0.036, 0.026, 8, (n, q) -> mix(GH_BZ, GH_BZ2, ss(0.0, 1.0, n.y) * 0.6F));
            cone(c, c1, c2, 0.026, 0.0, 8, (n, q) -> mix(GH_BZ, GH_BZ2, ss(0.0, 1.0, n.y) * 0.6F));
            ell(c, c1, new V3(0.026, 0.026, 0.026), 8, 6, (n, q) -> mix(GH_BZ, GH_BZ2, 0.4F));
        }
        c.head = false;
        // chispas
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.013 + k * 0.167 + ph * 0.1) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.3 + u * 0.35, Math.cos(k * 1.7) * 0.12), 0.02 * (1.1 - u * 0.6),
                    new float[]{1F, 0.7F, 0.25F}, t, ph + k, 0.35);
        }
    }
}
