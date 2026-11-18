package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas nuevas (originales) que sustituyen a tres anteriores: Cordero Serafín, Cachorro Ascua y Dragoncito de Jade.
 */
final class Pets13 {
    private Pets13() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 8. CORDERO SERAFÍN

    private static final float[] CS_W = hex(0xFFF8F0), CS_W2 = hex(0xF2E4EE), CS_SK = hex(0xFFDCCB), CS_PK = hex(0xFFA8BE), CS_GOLD = hex(0xFFD86A),
            CS_GOLD2 = hex(0xFFF0A8), CS_HOOF = hex(0xE8A0B4), CS_WING = hex(0xFFFFFF), CS_WING2 = hex(0xCFE6FF), CS_HORN = hex(0xF6E7C8);

    static void angelLamb(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.018 * w + 0.01 * c.breath + 0.008 * Math.sin(t * 0.08 + ph);
        c.pivot = new V3(0, 0.25, 0);
        // alitas de plumas que aletean despacio
        double fl = Math.sin(t * 0.16 + ph) * 0.08 + Math.sin(c.gait * 1.2) * 0.07 * w;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.06, 0.37 + bob, -0.07);
            for (int k = 0; k < 5; k++) {
                double up = 0.17 - k * 0.022 + fl * (0.6 + k * 0.15);
                V3 e = sh.add(sd * (0.10 + k * 0.035), up, -0.06 - k * 0.012);
                Detail.feather(c, sh, e, new V3(0, 1, 0.3).norm(), 0.052 - k * 0.003, new V3(sd * 0.3, 0.2, 1).norm(), k > 2 ? CS_WING2 : CS_WING, k == 4 ? CS_GOLD : CS_WING2);
            }
        }
        legs4(c, 0.075, 0.09, -0.1, 0.17 + bob, 0.032, 0.03, 0.045, (n, p) -> CS_SK, flat(CS_HOOF));
        // cuerpo de lana: nube de bolitas rizadas
        V3 bc = new V3(0, 0.255 + bob, -0.005), br = new V3(0.135, 0.125, 0.155);
        Paint bp = (n, p) -> mix(CS_W, CS_W2, ss(0.2, -0.9, n.y) * 0.6);
        ell(c, bc, br, 14, 10, bp);
        for (int i = 0; i < 19; i++) {
            V3 d = Detail.sph(7701, i);
            if (d.y < -0.55) {
                continue;
            }
            V3 pt = bc.add(d.x * br.x * 0.92, d.y * br.y * 0.92, d.z * br.z * 0.92);
            double rr = 0.044 + 0.014 * Detail.rnd(7701, i, 9);
            ell(c, pt, new V3(rr, rr, rr), 8, 6, bp);
        }
        // colita pompón
        ell(c, new V3(0, 0.27 + bob, -0.175), new V3(0.04, 0.04, 0.04), 8, 6, flat(CS_W));
        // cabeza: cara rosada clara, flequillo de lana, orejas caídas, cuernitos de perla
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.38, 0.1);
        V3 hc = new V3(0, 0.42 + bob, 0.15), hr = new V3(0.105, 0.098, 0.1);
        Paint hp = (n, p) -> mix(CS_SK, CS_W, ss(0.3, 1.0, n.y) * 0.5);
        ell(c, hc, hr, 14, 10, hp);
        V3 sn = hc.add(0, -0.03, 0.085);
        ell(c, sn, new V3(0.052, 0.04, 0.042), 10, 7, flat(CS_SK));
        ell(c, sn.add(0, 0.014, 0.034), new V3(0.017, 0.012, 0.01), 6, 4, flat(CS_PK));
        decal(c, sn, new V3(0.052, 0.04, 0.042), new V3(0, -0.5, 1), 0.06, 0, T_MOUTH, new float[]{0.5F, 0.3F, 0.3F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.5, 0.2, 0.82, 0.085, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.82, -0.22, 0.5, 0.065);
        // flequillo rizado
        for (int k = 0; k < 6; k++) {
            double a = -1.0 + k * 0.4;
            ell(c, hc.add(Math.sin(a) * 0.075, 0.075 + Math.cos(a) * 0.012, 0.03 + Math.cos(a) * 0.04), new V3(0.04, 0.036, 0.04), 8, 6, bp);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 31 : 32) * 0.02;
            V3 ea = hc.add(sd * 0.085, 0.01, -0.01);
            ell(c, ea.add(sd * (0.05 + ef), -0.02, 0), new V3(0.05, 0.026, 0.034), 0, 0, sd * -0.35, 10, 6, flat(CS_SK));
            ell(c, ea.add(sd * (0.055 + ef), -0.022, 0.004), new V3(0.036, 0.016, 0.024), 0, 0, sd * -0.35, 8, 5, flat(CS_PK));
            // cuernito enrollado
            V3[] hn = {hc.add(sd * 0.075, 0.055, 0.0), hc.add(sd * 0.115, 0.075, -0.015), hc.add(sd * 0.125, 0.045, -0.04), hc.add(sd * 0.1, 0.035, -0.045)};
            curve(c, hn, new double[]{0.017, 0.014, 0.011, 0.006}, 4, 6, flat(CS_HORN));
        }
        // aureola dorada flotando
        double hv = 0.012 * Math.sin(t * 0.09 + ph);
        V3 hl = hc.add(0, 0.17 + hv, -0.01);
        V3[] ring = new V3[17];
        double[] rr2 = new double[17];
        for (int i = 0; i <= 16; i++) {
            double a = Math.PI * 2 * i / 16.0;
            ring[i] = hl.add(Math.cos(a) * 0.065, 0.0, Math.sin(a) * 0.065);
            rr2[i] = 0.0085;
        }
        boolean em = c.emissive;
        c.emissive = true;
        tube(c, ring, rr2, 6, flat(CS_GOLD));
        c.emissive = em;
        c.head = false;
        glow(c, hl, 0.26, new float[]{1F, 0.9F, 0.5F}, 0.5);
        // lacito rosa con cascabel dorado
        V3 nk = new V3(0, 0.335 + bob, 0.105);
        ell(c, nk, new V3(0.1, 0.02, 0.07), 12, 4, flat(CS_PK));
        ell(c, nk.add(0, -0.03, 0.065), new V3(0.018, 0.018, 0.018), 8, 6, flat(CS_GOLD));
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.25, 0.3 + u * 0.45 + bob, Math.cos(k * 1.7) * 0.2), 0.02 * (1 - u * 0.4), new float[]{1F, 0.95F, 0.7F}, t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 2. CACHORRO ASCUA

    private static final float[] CA_F = hex(0xE8742A), CA_F2 = hex(0xFFA244), CA_CR = hex(0xFFEFD8), CA_DK = hex(0x3A2A2A), CA_FL = hex(0xFF8A20),
            CA_FL2 = hex(0xFFE060), CA_EMB = hex(0xFF5A1A);

    static void emberPup(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // cola en alto que acaba en llama
        double wag = Math.sin(t * 0.2 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.26 + bob, -0.15), new V3(wag * 0.5, 0.33 + bob, -0.23), new V3(wag, 0.42 + bob, -0.25), new V3(wag * 1.2, 0.5 + bob, -0.2)};
        curve(c, tcp, new double[]{0.036, 0.034, 0.03, 0.026}, 4, 7, (n, p) -> mix(CA_F, CA_F2, ss(0.0, 1.0, p.y * 0.3 + 0.3)));
        boolean em = c.emissive;
        c.emissive = true;
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 0.4 + t * 0.14;
            double fh = 0.11 + 0.03 * Math.sin(t * 0.3 + ph + k);
            Detail.kite(c, tcp[3], tcp[3].add(Math.cos(a) * 0.03 + wag, fh, Math.sin(a) * 0.03 - 0.01), new V3(-Math.sin(a), 0, Math.cos(a)), 0.034, new V3(0, 0, 1), CA_FL, CA_FL2);
        }
        c.emissive = em;
        glow(c, tcp[3].add(0, 0.05, 0), 0.26, new float[]{1F, 0.55F, 0.15F}, 0.5);
        legs4(c, 0.075, 0.1, -0.11, 0.19 + bob, 0.036, 0.032, 0.05, (n, p) -> mix(CA_F, CA_F2, 0.3), flat(CA_DK));
        V3 bc = new V3(0, 0.255 + bob, -0.01), br = new V3(0.115, 0.105, 0.17);
        Paint bp = (n, p) -> mix(CA_F, CA_F2, ss(0.0, 0.9, n.y) * 0.55);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 80, 2031, 0.03, 0.016, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, CA_F2);
        ell(c, bc.add(0, -0.025, 0.07), new V3(0.075, 0.08, 0.1), 12, 8, flat(CA_CR));
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.34, 0.12);
        V3 hc = new V3(0, 0.395 + bob, 0.18), hr = new V3(0.125, 0.11, 0.115);
        Paint hp = (n, p) -> mix(CA_F, CA_F2, ss(0.1, 0.9, n.y) * 0.45);
        ell(c, hc, hr, 14, 10, hp);
        Detail.fur(c, hc, hr, 40, 2032, 0.024, 0.014, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.3 ? 1 : 0.0, hp, CA_F2);
        V3 sn = hc.add(0, -0.03, 0.095);
        ell(c, sn, new V3(0.06, 0.045, 0.06), 10, 7, flat(CA_CR));
        ell(c, sn.add(0, 0.022, 0.05), new V3(0.021, 0.016, 0.014), 8, 5, flat(BLACK));
        decal(c, sn, new V3(0.06, 0.045, 0.06), new V3(0, -0.5, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.25F, 0.22F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.5, 0.2, 0.82, 0.095, T_BEAD, new float[]{1F, 0.8F, 0.4F});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.07);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 33 : 34) * 0.02;
            V3 eb = hc.add(sd * 0.075, 0.085, -0.01);
            cone(c, eb, eb.add(sd * (0.03 + ef), 0.1, -0.02), 0.042, 0.004, 6, hp);
            cone(c, eb.add(0, -0.005, 0.01), eb.add(sd * (0.026 + ef), 0.085, -0.012), 0.026, 0.003, 6, flat(CA_EMB));
        }
        // mechón de llama en la cabeza
        c.emissive = true;
        for (int k = 0; k < 3; k++) {
            double a = Math.sin(t * 0.2 + k * 1.7 + ph);
            V3 b = hc.add((k - 1) * 0.028, 0.1, -0.01);
            Detail.kite(c, b, b.add((k - 1) * 0.012 + a * 0.01, 0.07 + 0.02 * a, -0.01), new V3(1, 0, 0), 0.02, new V3(0, 0, 1), CA_FL, CA_FL2);
        }
        c.emissive = em;
        c.head = false;
        // pañuelito de brasa
        V3 cl = new V3(0, 0.325 + bob, 0.14);
        ell(c, cl, new V3(0.095, 0.02, 0.07), 12, 4, flat(CA_DK));
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.016 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(wag + Math.sin(k * 2.1) * 0.07, 0.5 + u * 0.35 + bob, -0.2 + Math.cos(k * 1.7) * 0.06), 0.02 * (1 - u * 0.5), new float[]{1F, 0.6F, 0.2F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 20. DRAGONCITO DE JADE

    private static final float[] DJ_J = hex(0x2DBE8A), DJ_J2 = hex(0x7CEBC0), DJ_BE = hex(0xFFF0B8), DJ_GOLD = hex(0xF6C84A), DJ_GOLD2 = hex(0xFFE890),
            DJ_DK = hex(0x1A7A64), DJ_PEARL = hex(0xE8FFF4), DJ_RED = hex(0xE8503A);

    static void jadeDragon(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        Paint bp = (n, p) -> mix(DJ_J, DJ_J2, ss(0.1, 0.9, n.y) * 0.55);
        // cola larga que serpentea, con mechón dorado
        double sw = Math.sin(t * 0.12 + ph) * 0.05 + Math.sin(c.gait) * 0.04 * w;
        V3[] tcp = {new V3(0, 0.22 + bob, -0.15), new V3(sw * 0.4, 0.2 + bob, -0.3), new V3(sw, 0.19 + bob, -0.45), new V3(sw * 0.4, 0.23 + bob, -0.58), new V3(-sw * 0.8, 0.3 + bob, -0.65)};
        curve(c, tcp, new double[]{0.07, 0.055, 0.04, 0.028, 0.016}, 4, 8, bp);
        for (int k = 0; k < 6; k++) {
            double u = k / 5.0;
            V3 b = tcp[0].lerp(tcp[3], u).add(0, 0.045 * (1 - u * 0.5), 0);
            cone(c, b, b.add(0, 0.045 * (1 - u * 0.5), -0.015), 0.016, 0.0, 4, flat(DJ_GOLD));
        }
        boolean em = c.emissive;
        for (int k = 0; k < 4; k++) {
            double a = Math.sin(t * 0.2 + k * 1.3 + ph);
            V3 b = tcp[4];
            Detail.kite(c, b, b.add((k - 1.5) * 0.02 + a * 0.01, 0.04 + 0.015 * a, -0.05 - k * 0.01), new V3(1, 0, 0), 0.018, new V3(0, 1, 0), DJ_GOLD, DJ_RED);
        }
        legs4(c, 0.085, 0.1, -0.1, 0.16 + bob, 0.036, 0.03, 0.05, (n, p) -> mix(DJ_J, DJ_DK, 0.3), flat(DJ_GOLD));
        // cuerpo alargado con escamas y barriga dorada
        V3 bc = new V3(0, 0.24 + bob, -0.02), br = new V3(0.105, 0.1, 0.2);
        Paint bp2 = (n, p) -> n.y < -0.35 ? mix(DJ_BE, DJ_J2, ss(-0.9, -0.35, n.y)) : mix(DJ_J, DJ_J2, ss(0.1, 0.9, n.y) * 0.55);
        ell(c, bc, br, 14, 10, bp2);
        Detail.scales(c, bc, br, 7, 12, 0.036, d -> d.y > -0.3 ? 1 : 0.0, bp2, DJ_DK);
        // melena dorada a lo largo del lomo
        c.emissive = false;
        for (int k = 0; k < 8; k++) {
            double z = 0.17 - k * 0.055;
            V3 b = new V3(0, bc.y + br.y * 0.95, z);
            double a = Math.sin(t * 0.15 + k * 0.7 + ph) * 0.012;
            Detail.kite(c, b, b.add(a, 0.065 - k * 0.003, -0.02), new V3(1, 0, 0), 0.02, new V3(0, 0, 1), DJ_GOLD, DJ_GOLD2);
        }
        // cuello y cabeza grande con hocico, cuernos ramificados y bigotes largos
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.34, 0.2);
        tube(c, new V3[]{new V3(0, 0.3 + bob, 0.12), new V3(0, 0.36 + bob, 0.2), new V3(0, 0.4 + bob, 0.25)}, new double[]{0.07, 0.062, 0.06}, 7, bp);
        V3 hc = new V3(0, 0.42 + bob, 0.27), hr = new V3(0.115, 0.1, 0.11);
        Paint hp = (n, p) -> mix(DJ_J, DJ_J2, ss(0.1, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 14, 10, hp);
        V3 sn = hc.add(0, -0.03, 0.095);
        ell(c, sn, new V3(0.062, 0.045, 0.05), 10, 7, flat(DJ_J2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, sn.add(sd * 0.022, 0.012, 0.045), new V3(0.009, 0.008, 0.006), 6, 4, flat(DJ_DK));
        }
        decal(c, sn, new V3(0.062, 0.045, 0.05), new V3(0, -0.5, 1), 0.07, 0, T_MOUTH, new float[]{0.45F, 0.2F, 0.2F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.52, 0.2, 0.8, 0.1, T_BEAD, new float[]{1F, 0.92F, 0.55F});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.07);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 35 : 36) * 0.02;
            // cuerno con rama
            V3[] hn = {hc.add(sd * 0.05, 0.08, -0.01), hc.add(sd * 0.075, 0.14, -0.04), hc.add(sd * 0.09 + ef, 0.2, -0.075)};
            curve(c, hn, new double[]{0.02, 0.016, 0.005}, 4, 6, flat(DJ_GOLD));
            V3 br0 = hn[1];
            cone(c, br0, br0.add(sd * 0.05, 0.035, 0.0), 0.011, 0.0, 5, flat(DJ_GOLD2));
            // aletas-orejas
            V3 ea = hc.add(sd * 0.1, 0.0, -0.02);
            Detail.feather(c, ea, ea.add(sd * (0.075 + ef), 0.03, -0.035), new V3(0, 1, 0), 0.032, new V3(sd * 0.3, 0.3, 1).norm(), DJ_J2, DJ_GOLD2);
            // bigotes que ondean
            double wv = Math.sin(t * 0.11 + sd + ph) * 0.03;
            V3[] wh = {sn.add(sd * 0.04, 0.0, 0.03), sn.add(sd * 0.1, -0.02 + wv, 0.0), sn.add(sd * 0.15, -0.07 + wv * 1.5, -0.05), sn.add(sd * 0.17, -0.11 + wv * 2, -0.12)};
            curve(c, wh, new double[]{0.006, 0.005, 0.004, 0.002}, 4, 4, flat(DJ_GOLD2));
        }
        c.head = false;
        // perla de jade que gira cerca de la cabeza
        double a = t * 0.05 + ph;
        V3 pl = new V3(Math.cos(a) * 0.2, 0.6 + bob + Math.sin(t * 0.09 + ph) * 0.02, 0.18 + Math.sin(a) * 0.15);
        c.emissive = true;
        ell(c, pl, new V3(0.03, 0.03, 0.03), 8, 6, flat(DJ_PEARL));
        c.emissive = em;
        glow(c, pl, 0.1, new float[]{0.5F, 1F, 0.8F}, 0.4);
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.22, 0.3 + u * 0.4 + bob, Math.cos(k * 1.7) * 0.2), 0.02 * (1 - u * 0.4), new float[]{0.6F, 1F, 0.8F}, t, ph + k, 0.25);
        }
    }
}
