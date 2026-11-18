package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Rework de mascotas, lote 1 (comunes): hongo paseante, sapo brote, motita de hollín, caracol casita, tanuki aldeano,
 * patito pirata y cangrejo tetera. Diseños originales; todo se arma con las mismas piezas del motor (esferas, tubos, decalcomanías).
 */
final class Pets8 {
    private Pets8() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 11. HONGO PASEANTE

    private static final float[] MU_CAP = hex(0xD8487F), MU_CAP2 = hex(0xFF8DB8), MU_SPOT = hex(0xFFF4DE), MU_STEM = hex(0xFBEBCF),
            MU_STEM2 = hex(0xE8C99C), MU_GILL = hex(0xB27CD8), MU_BR = hex(0x8C5A44), MU_LEAF = hex(0x6FD36A);

    static void mushroom(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.022 * w;
        double sway = Math.sin(c.gait) * 0.05 * w;
        c.pivot = new V3(0, 0.22, 0);
        // patitas cortas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.06 * w, lift = Math.max(0, Math.cos(p)) * 0.045 * w;
            V3 hip = new V3(sd * 0.07, 0.15 + bob, 0.0), fo = new V3(sd * 0.075, 0.035 + lift, sw);
            tube(c, new V3[]{hip, hip.lerp(fo, 0.5), fo}, new double[]{0.042, 0.038, 0.036}, 7, (n, q) -> MU_STEM2);
            ell(c, fo.add(0, -0.012, 0.03), new V3(0.062, 0.032, 0.085), 10, 6, flat(MU_BR));
        }
        // tallo redondo: la carita va aquí
        V3 bc = new V3(0, 0.30 + bob, 0), br = new V3(0.15, 0.17, 0.14);
        Paint sp = (n, q) -> mix(MU_STEM, MU_STEM2, ss(0.1, -0.9, n.y) * 0.8);
        ell(c, bc, br, 14, 10, sp);
        // bracitos que se balancean
        for (int sd = -1; sd <= 1; sd += 2) {
            double s2 = Math.sin(c.gait + (sd > 0 ? 0 : Math.PI)) * 0.07 * w + Math.sin(t * 0.07 + ph + sd) * 0.012;
            V3 a = new V3(sd * 0.13, 0.30 + bob, 0.02), b = new V3(sd * 0.21, 0.235 + bob, 0.05 + s2);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.028, 0.025, 0.022}, 6, (n, q) -> MU_STEM2);
            ell(c, b, new V3(0.032, 0.03, 0.032), 8, 6, flat(MU_STEM));
        }
        // cara
        eyes(c, bc, br, 0.32, -0.02, 0.90, 0.095, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, bc, br, 0.62, -0.30, 0.72, 0.075);
        decal(c, bc, br, new V3(0, -0.40, 1), 0.06, 0, T_MOUTH, new float[]{0.45F, 0.22F, 0.22F}, 0.95, 0.003);
        // sombrero de hongo (se inclina con la cabeza)
        headLook(c, 0.10, 0.02);
        c.hpv = new V3(0, 0.38 + bob, 0);
        V3 cc = new V3(0, 0.485 + bob, 0), cr = new V3(0.26, 0.17, 0.25);
        Paint capP = (n, q) -> mix(MU_CAP, MU_CAP2, ss(-0.1, 0.95, n.y) * 0.75);
        Detail.cap(c, cc, cr, 0, 0, 0, 1.78, 0.0, 22, 9, capP);
        // láminas moradas bajo el sombrero
        ell(c, new V3(0, 0.428 + bob, 0), new V3(0.245, 0.028, 0.235), 20, 4, (n, q) -> MU_GILL);
        for (int k = 0; k < 14; k++) {
            double a = k * Math.PI * 2 / 14;
            ell(c, new V3(Math.cos(a) * 0.19, 0.42 + bob, Math.sin(a) * 0.18), new V3(0.012, 0.012, 0.012), 6, 4, flat(scale(MU_GILL, 0.8)));
        }
        // lunares crema
        double[][] sp0 = {{0.0, 1.0, 0.1, 0.17}, {0.65, 0.78, 0.25, 0.13}, {-0.7, 0.72, 0.3, 0.14}, {0.35, 0.65, -0.7, 0.12}, {-0.4, 0.7, -0.65, 0.13}, {0.1, 0.35, 0.95, 0.10}, {-0.85, 0.35, -0.2, 0.09}, {0.9, 0.3, -0.25, 0.09}};
        for (double[] s : sp0) {
            Detail.spot(c, cc, cr, new V3(s[0], s[1], s[2]), s[3], MU_SPOT);
        }
        Detail.soft(c, cc, cr, new V3(-0.35, 1, 0.35), 0.2, WHITE, 0.35);
        // brotecito en la punta
        double lf = Math.sin(t * 0.09 + ph) * 0.02;
        V3[] st = {new V3(0.02, 0.65 + bob, 0.0), new V3(0.03 + lf, 0.695 + bob, 0.0), new V3(0.01 + lf, 0.74 + bob, 0.0)};
        tube(c, st, new double[]{0.012, 0.01, 0.008}, 5, flat(MU_LEAF));
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.kite(c, st[2], st[2].add(sd * 0.075, 0.025 + lf, 0.01), new V3(0, 0, 1), 0.028, new V3(0, 1, 0), MU_LEAF, scale(MU_LEAF, 1.2));
        }
        c.head = false;
        // esporas brillantes
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.011 + ph * 0.3 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 4) * 0.26, 0.50 + u * 0.28 + bob, Math.cos(k * 1.7 + u * 3) * 0.22), 0.02 * (1 - u * 0.4), new float[]{1F, 0.8F, 0.95F}, t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 12. SAPO BROTE

    private static final float[] FB_G = hex(0x5FB8A0), FB_G2 = hex(0x8FD9B8), FB_BELLY = hex(0xE6F5C8), FB_DK = hex(0x2F7A6A),
            FB_BULB = hex(0x7BC85A), FB_BULB2 = hex(0xB4EA7C), FB_PET = hex(0xFF9CC8), FB_EYE = hex(0xFFD24A);

    static void frogBulb(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait)) * 0.05 * w;
        double bob = hop + 0.012 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // patas traseras dobladas y delanteras rectas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 hip = new V3(sd * 0.14, 0.15 + bob * 0.4, -0.10);
            V3 kn = new V3(sd * 0.21, 0.09 + bob * 0.3, -0.16), fo = new V3(sd * 0.17, 0.03 + hop * 0.3, -0.02);
            tube(c, new V3[]{hip, kn, fo}, new double[]{0.07, 0.048, 0.034}, 8, (n, p) -> FB_G);
            ell(c, fo.add(sd * 0.01, -0.012, 0.045), new V3(0.075, 0.022, 0.085), 10, 6, flat(FB_G2));
            for (int k = -1; k <= 1; k++) {
                ell(c, fo.add(sd * 0.01 + k * 0.034, -0.008, 0.115), new V3(0.017, 0.015, 0.025), 6, 4, flat(FB_G2));
            }
            V3 sh = new V3(sd * 0.10, 0.2 + bob, 0.12), pw = new V3(sd * 0.12, 0.025 + hop * 0.2, 0.2);
            tube(c, new V3[]{sh, sh.lerp(pw, 0.5), pw}, new double[]{0.045, 0.036, 0.032}, 7, (n, p) -> FB_G);
            ell(c, pw.add(0, -0.01, 0.025), new V3(0.05, 0.02, 0.06), 8, 5, flat(FB_G2));
        }
        // cuerpo rechoncho con vientre claro
        V3 bc = new V3(0, 0.215 + bob, 0.0), br = new V3(0.205, 0.145, 0.235);
        Paint bp = (n, p) -> mix(mix(FB_G, FB_G2, ss(0.0, 0.9, n.y) * 0.5), FB_BELLY, ss(-0.2, -0.8, n.y) * 0.9);
        ell(c, bc, br, 16, 11, bp);
        for (int k = 0; k < 7; k++) {
            double a = k * 0.95;
            Detail.spot(c, bc, br, new V3(Math.cos(a) * 0.8, 0.5 + 0.4 * Math.sin(a * 1.7), Math.sin(a) * 0.9), 0.07 + 0.02 * (k % 3), FB_DK);
        }
        // bulbo gigante en el lomo con hojas
        double open = 0.5 + 0.5 * Math.sin(t * 0.045 + ph);
        V3 uc = new V3(0, 0.36 + bob, -0.07), ur = new V3(0.15, 0.12, 0.15);
        Paint up = (n, p) -> mix(FB_BULB, FB_BULB2, ss(-0.4, 0.9, n.y));
        ell(c, uc, ur, 14, 9, up);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3 + 0.3;
            V3 base = uc.add(Math.cos(a) * 0.11, 0.075, Math.sin(a) * 0.11);
            V3 tip = base.add(Math.cos(a) * (0.12 + 0.03 * open), 0.09 + 0.05 * open, Math.sin(a) * (0.12 + 0.03 * open));
            V3 sd2 = new V3(-Math.sin(a), 0, Math.cos(a));
            Detail.feather(c, base, tip, sd2, 0.075, new V3(0, 1, 0), FB_BULB, FB_BULB2);
        }
        // florcita rosa en el centro
        V3 fc = uc.add(0, 0.12 + 0.02 * open, 0);
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5;
            Detail.kite(c, fc, fc.add(Math.cos(a) * 0.065, 0.012, Math.sin(a) * 0.065), new V3(-Math.sin(a), 0, Math.cos(a)), 0.03, new V3(0, 1, 0), FB_PET, scale(FB_PET, 1.15));
        }
        ell(c, fc.add(0, 0.012, 0), new V3(0.022, 0.016, 0.022), 8, 5, flat(FB_EYE));
        // cabeza ancha con ojos saltones
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.26, 0.2);
        V3 hc = new V3(0, 0.285 + bob, 0.20), hr = new V3(0.185, 0.10, 0.13);
        ell(c, hc, hr, 16, 10, (n, p) -> mix(FB_G, FB_G2, ss(0.0, 0.9, n.y) * 0.5));
        // sonrisa ancha
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.16, 0, T_MOUTH, new float[]{0.2F, 0.35F, 0.28F}, 0.95, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.085, 0.365 + bob, 0.205);
            ell(c, ec, new V3(0.062, 0.058, 0.058), 12, 8, flat(FB_G2));
            V3 er = new V3(0.062, 0.058, 0.058);
            Detail.spot(c, ec, er, new V3(sd * 0.1, 0.1, 1), 0.075, WHITE);
            Detail.spot(c, ec, er, new V3(sd * 0.1, 0.05, 1), 0.04, c.shut ? WHITE : BLACK);
            Detail.glint(c, ec, er, new V3(sd * 0.1 - 0.2, 0.4, 1), 0.02, WHITE, 0.9);
            if (c.shut) {
                decal(c, ec, er, new V3(sd * 0.1, 0.05, 1), 0.06, 0, T_EYE_SHUT, WHITE, 1, 0.004);
            }
            ell(c, new V3(sd * 0.115, 0.375 + bob, 0.19), new V3(0.03, 0.022, 0.02), 6, 4, flat(FB_DK));
        }
        cheeks(c, hc, hr, 0.85, -0.18, 0.55, 0.08);
        c.head = false;
        // polen que flota del bulbo
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 4) * 0.2, 0.55 + u * 0.3 + bob, -0.07 + Math.cos(k * 1.4) * 0.15), 0.017 * (1 - u * 0.4), new float[]{1F, 0.95F, 0.55F}, t, ph + k, 0.27);
        }
    }

    // ------------------------------------------------------------------ 13. MOTITA DE HOLLÍN

    private static final float[] SO_K = {0.10F, 0.09F, 0.13F}, SO_K2 = {0.22F, 0.19F, 0.28F}, SO_CANDY = hex(0xFF8FC0), SO_CANDY2 = hex(0xFFE08A), SO_CANDY3 = hex(0x8FE0FF), SO_K3 = {0.42F, 0.38F, 0.55F};

    static void sootSprite(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.03 * w + 0.01 * c.breath;
        double roll = Math.sin(c.gait) * 0.06 * w;
        c.pivot = new V3(0, 0.2, 0);
        // piecitos y bracitos de palito
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.2 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.05 * w, lift = Math.max(0, Math.cos(p)) * 0.04 * w;
            V3 a = new V3(sd * 0.05, 0.10 + bob, 0), b = new V3(sd * 0.06, 0.015 + lift, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.014, 0.012, 0.011}, 5, flat(SO_K));
            ell(c, b.add(0, -0.004, 0.015), new V3(0.03, 0.014, 0.04), 8, 5, flat(SO_K));
        }
        // bola de pelusa
        V3 bc = new V3(0, 0.215 + bob, 0), br = new V3(0.17, 0.165, 0.165);
        Paint p0 = (n, p) -> mix(SO_K, SO_K2, ss(-0.2, 0.9, n.y) * 0.4);
        ell(c, bc, br, 14, 10, p0);
        double spin = t * 0.01;
        Detail.fur(c, bc, br, 300, 801, 0.055, 0.017, new V3(0, 0, 0), 0.0, Detail.ALL, p0, SO_K3);
        Detail.fur(c, bc, br, 160, 802, 0.04, 0.013, new V3(0, 0.3, 0), 0.0, Detail.ALL, p0, SO_K2);
        // brillo suave de polvo en lo alto y brazos de palito que sujetan el dulce
        Detail.soft(c, bc, br, new V3(-0.4, 0.8, 0.4), 0.14, new float[]{0.55F, 0.5F, 0.7F}, 0.35);
        for (int sd = -1; sd <= 1; sd += 2) {
            double wv = Math.sin(t * 0.09 + ph + sd) * 0.012;
            V3 sa = new V3(sd * 0.17, 0.24 + bob, 0.06), sb2 = new V3(sd * 0.2, 0.37 + bob + wv, 0.08);
            tube(c, new V3[]{sa, sa.lerp(sb2, 0.5).add(sd * 0.03, 0, 0), sb2}, new double[]{0.011, 0.01, 0.009}, 5, flat(SO_K));
            ell(c, sb2, new V3(0.02, 0.02, 0.02), 6, 5, flat(SO_K2));
        }
        // ojos grandes blancos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.075, 0.235 + bob, 0.145), er = new V3(0.062, 0.068, 0.045);
            ell(c, ec, er, 12, 8, flat(WHITE));
            double lx = Math.sin(t * 0.04 + ph) * 0.012;
            decal(c, ec, er, new V3(sd * 0.05 + lx * 4, 0.0, 1), 0.058, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? BLACK : WHITE, 1, 0.003);
            Detail.glint(c, ec, er, new V3(sd * 0.05 - 0.25, 0.4, 1), 0.016, WHITE, 0.9);
        }
        // boquita pequeña
        decal(c, bc, br, new V3(0, -0.3, 1), 0.05, 0, T_MOUTH, new float[]{0.95F, 0.6F, 0.65F}, 0.9, 0.012);
        cheeks(c, bc, br, 0.62, -0.1, 0.7, 0.05);
        // estrellita de dulce que carga encima (konpeito rosa)
        double lift = 0.01 * Math.sin(t * 0.08 + ph);
        V3 sc = new V3(0.0, 0.43 + bob + lift, 0.02);
        ell(c, sc, new V3(0.042, 0.042, 0.042), 10, 7, (n, p) -> mix(SO_CANDY, SO_CANDY2, ss(-0.5, 0.9, n.y)));
        for (int k = 0; k < 8; k++) {
            V3 d = Detail.sph(803, k);
            Detail.kite(c, sc.add(d.mul(0.035)), sc.add(d.mul(0.07)), d.anyPerp(), 0.016, d, SO_CANDY, scale(SO_CANDY, 1.2));
        }
        Detail.twinkle(c, sc.add(0.03, 0.03, 0.03), 0.03, new float[]{1F, 0.9F, 0.7F}, t, ph, 0.22);
        // motitas más chicas flotando alrededor
        for (int k = 0; k < 5; k++) {
            double u = ((t * 0.008 + ph * 0.2 + k * 0.2) % 1.0);
            V3 pos = new V3(Math.sin(k * 2.4 + u * 5) * 0.22, 0.12 + u * 0.4, Math.cos(k * 1.9 + u * 4) * 0.2);
            ell(c, pos, new V3(0.016, 0.016, 0.016), 6, 4, flat(SO_K));
        }
    }

    // ------------------------------------------------------------------ 14. CARACOL CASITA

    private static final float[] SN_BODY = hex(0xF3D9B8), SN_BODY2 = hex(0xE0B98A), SN_WALL = hex(0xF6E7CC), SN_WALL2 = hex(0xD9B98C),
            SN_ROOF = hex(0xC8584A), SN_ROOF2 = hex(0xE8806A), SN_DOOR = hex(0x7A4A38), SN_WIN = hex(0xFFD36B), SN_BRICK = hex(0xA0604A);

    static void cottageSnail(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double glide = Math.sin(c.gait * 0.7) * 0.012 * w;
        c.pivot = new V3(0, 0.2, 0);
        // cuerpo largo y suave
        V3[] cp = {new V3(0, 0.07, -0.20), new V3(0, 0.075 + glide, -0.05), new V3(0, 0.09, 0.10), new V3(0, 0.13 + glide, 0.2), new V3(0, 0.17, 0.25)};
        double[] rd = {0.075, 0.095, 0.095, 0.082, 0.07};
        Paint bp = (n, p) -> mix(SN_BODY, SN_BODY2, ss(0.2, -0.9, n.y) * 0.8);
        curve(c, cp, rd, 4, 10, bp);
        ell(c, new V3(0, 0.025, 0.02), new V3(0.095, 0.022, 0.26), 12, 5, flat(SN_BODY2));
        // casita-concha en el lomo
        V3 hc0 = new V3(0, 0.285, -0.07);
        double sway = Math.sin(c.gait * 0.7) * 0.02 * w;
        double[] rm = null;
        V3 sc = hc0.add(sway, 0, 0), sr = new V3(0.17, 0.15, 0.17);
        Paint wall = (n, p) -> mix(SN_WALL, SN_WALL2, ss(0.2, -0.9, n.y) * 0.7);
        ell(c, sc, sr, 14, 9, wall);
        // vigas de madera
        for (int k = 0; k < 3; k++) {
            double y = sc.y + 0.05 - k * 0.06;
            ell(c, new V3(sc.x, y, sc.z), new V3(sr.x * 1.01 * Math.sqrt(Math.max(0.1, 1 - sq((y - sc.y) / sr.y))), 0.008, sr.z * 1.01 * Math.sqrt(Math.max(0.1, 1 - sq((y - sc.y) / sr.y)))), 14, 3, flat(SN_DOOR));
        }
        // techo de tejas: pirámide de cuatro caras
        V3 rb = sc.add(0, 0.095, 0);
        Paint roofP = (n, p) -> mix(SN_ROOF, SN_ROOF2, ss(-0.2, 0.9, n.y) * 0.8);
        tube(c, new V3[]{rb, rb.lerp(rb.add(0, 0.17, 0), 0.5), rb.add(0, 0.17, 0)}, new double[]{0.235, 0.12, 0.004}, 4, roofP);
        ell(c, rb.add(0, -0.004, 0), new V3(0.215, 0.012, 0.215), 4, 3, flat(scale(SN_ROOF, 0.8)));
        // chimenea de ladrillos
        Detail.pixBox(c, sc.add(0.07, 0.235, -0.03), new V3(0.032, 0.05, 0.032), 3, 4, 3, (f, i, j, ni, nj) -> (i + j) % 2 == 0 ? SN_BRICK : scale(SN_BRICK, 0.85));
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.012 + ph * 0.3 + k * 0.33) % 1.0);
            V3 sm = sc.add(0.07 + Math.sin(u * 5 + k) * 0.03, 0.31 + u * 0.25, -0.03);
            boolean em = c.emissive;
            c.emissive = true;
            glow(c, sm, 0.07 + 0.08 * u, new float[]{1F, 1F, 1F}, 0.5 * (1 - u));
            c.emissive = em;
        }
        // puertita y ventanas luminosas
        decal(c, sc, sr, new V3(0, -0.2, 1), 0.07, 0, Detail.T_DISC, SN_DOOR, 1, 0.006);
        Detail.glint(c, sc, sr, new V3(0.12, -0.2, 1), 0.012, SN_WIN, 0.9);
        Detail.glint(c, sc, sr, new V3(0.7, 0.15, 0.7), 0.055, SN_WIN, 0.95);
        Detail.glint(c, sc, sr, new V3(-0.7, 0.15, 0.7), 0.055, SN_WIN, 0.95);
        Detail.soft(c, sc, sr, new V3(0.7, 0.15, 0.7), 0.1, SN_WIN, 0.35);
        Detail.soft(c, sc, sr, new V3(-0.7, 0.15, 0.7), 0.1, SN_WIN, 0.35);
        // cabeza y tentáculos con ojitos
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.16, 0.2);
        V3 hc = new V3(0, 0.19, 0.27), hr = new V3(0.09, 0.085, 0.09);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(SN_BODY, SN_BODY2, ss(0.2, -0.9, n.y) * 0.5));
        for (int sd = -1; sd <= 1; sd += 2) {
            double a = Math.sin(t * 0.08 + ph + sd) * 0.02 + Detail.flick(c, sd > 0 ? 3 : 4) * 0.03;
            V3 b = new V3(sd * 0.035, 0.245, 0.27), mid = new V3(sd * 0.06 + a, 0.31, 0.29), tp = new V3(sd * 0.075 + a * 1.5, 0.355, 0.30);
            tube(c, new V3[]{b, mid, tp}, new double[]{0.017, 0.014, 0.012}, 6, flat(SN_BODY));
            ell(c, tp, new V3(0.026, 0.026, 0.026), 8, 6, flat(SN_BODY));
            V3 ec = tp.add(0, 0.004, 0.012);
            ell(c, ec, new V3(0.017, 0.017, 0.015), 8, 5, flat(c.shut ? SN_BODY2 : BLACK));
            Detail.glint(c, ec, new V3(0.017, 0.017, 0.015), new V3(-0.3, 0.5, 1), 0.012, WHITE, 0.9);
        }
        decal(c, hc, hr, new V3(0, -0.4, 1), 0.06, 0, T_MOUTH, new float[]{0.55F, 0.28F, 0.28F}, 0.9, 0.003);
        cheeks(c, hc, hr, 0.75, -0.12, 0.65, 0.05);
        c.head = false;
    }

    private static double sq(double x) {
        return x * x;
    }

    // ------------------------------------------------------------------ 23. TANUKI ALDEANO

    private static final float[] TN_F = hex(0x9A7A5C), TN_F2 = hex(0xC2A07C), TN_CR = hex(0xF4E6CE), TN_DK = hex(0x3C2E2A), TN_LF = hex(0x5CC66A),
            TN_CL = hex(0xE0584A), TN_CL2 = hex(0xF9F2E0);

    static void tanuki(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.25, 0);
        // cola anillada
        double wag = Math.sin(t * 0.14 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.25, -0.2), new V3(wag * 0.4, 0.27, -0.33), new V3(wag, 0.32, -0.43), new V3(wag * 1.3, 0.40, -0.47)};
        double[] trd = {0.06, 0.09, 0.10, 0.075};
        Paint tp = (n, p) -> {
            double r = Math.sin(p.z * 55 + p.y * 12);
            return r > 0.15 ? TN_DK : mix(TN_F, TN_F2, 0.3);
        };
        curve(c, tcp, trd, 4, 10, tp);
        Detail.furTube(c, tcp, trd, 60, 811, 0.05, 0.022, 0.8, tp, null);
        legs4(c, 0.095, 0.12, -0.13, 0.19 + bob, 0.052, 0.034, 0.075, (n, p) -> mix(TN_F, TN_DK, ss(0.22, 0.0, p.y + 0.12) * 0.9), flat(TN_DK));
        // cuerpo muy redondo con panza clara
        V3 bc = new V3(0, 0.27 + bob, -0.01), br = new V3(0.185, 0.16, 0.22);
        Paint bp = (n, p) -> mix(mix(TN_F, TN_F2, ss(0.0, 0.9, n.y) * 0.5), TN_CR, ss(-0.1, -0.7, n.y) * 0.85 * ss(-0.1, 0.8, n.z));
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 140, 812, 0.03, 0.016, new V3(0, -0.4, -1), 0.8, Detail.ALL, bp, null);
        // bulto de tela atado a la espalda (furoshiki)
        V3 bun = new V3(0, 0.40 + bob, -0.06);
        ell(c, bun, new V3(0.105, 0.085, 0.10), 12, 8, (n, p) -> mix(TN_CL, scale(TN_CL, 1.2), ss(-0.3, 0.9, n.y) * 0.5));
        for (int k = 0; k < 6; k++) {
            double a = k * 1.1;
            Detail.spot(c, bun, new V3(0.105, 0.085, 0.10), new V3(Math.cos(a), 0.6, Math.sin(a)), 0.04, TN_CL2);
        }
        V3 kn = bun.add(0, 0.085, 0);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 tpk = kn.add(sd * 0.09, 0.05 + 0.01 * Math.sin(t * 0.1 + ph), 0.0);
            tube(c, new V3[]{kn, kn.lerp(tpk, 0.5).add(0, 0.02, 0), tpk}, new double[]{0.02, 0.026, 0.018}, 6, flat(TN_CL));
        }
        ell(c, kn, new V3(0.03, 0.025, 0.03), 8, 6, flat(scale(TN_CL, 0.9)));
        // cabeza
        headLook(c, 0.13, 0.0);
        c.hpv = new V3(0, 0.36, 0.17);
        V3 hc = new V3(0, 0.455 + bob, 0.20), hr = new V3(0.17, 0.135, 0.15);
        Paint hp = (n, p) -> mix(TN_F, TN_F2, ss(0.0, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 813, 0.03, 0.014, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.55 ? 1 : 0.0, hp, null);
        // antifaz oscuro
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.soft(c, hc, hr, new V3(sd * 0.6, 0.0, 0.8), 0.27, TN_DK, 1.0);
        }
        // hocico claro y nariz
        V3 sc = new V3(0, 0.42 + bob, 0.30), sr = new V3(0.07, 0.055, 0.075);
        ell(c, sc, sr, 10, 7, flat(TN_CR));
        ell(c, sc.add(0, 0.022, 0.065), new V3(0.026, 0.02, 0.02), 8, 5, flat(TN_DK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.08, 0, T_MOUTH, new float[]{0.35F, 0.2F, 0.2F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.50, 0.14, 0.84, 0.095, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.86, -0.26, 0.45, 0.075);
        // orejitas redondas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 5 : 6);
            V3 ec = new V3(sd * 0.12, 0.575 + bob, 0.15);
            ell(c, ec, new V3(0.05, 0.05, 0.03), 0, 0, sd * (0.3 - 0.3 * ef), 10, 7, flat(TN_DK));
            ell(c, ec.add(0, 0, 0.012), new V3(0.032, 0.032, 0.02), 8, 5, flat(TN_F2));
        }
        // hoja mágica en la cabeza
        double lf = Math.sin(t * 0.06 + ph) * 0.015;
        V3 lb = new V3(0.0, 0.59 + bob, 0.20);
        for (int k = 0; k < 2; k++) {
            V3 tpk = lb.add((k == 0 ? -0.03 : 0.04), 0.095 + lf, 0.01 - k * 0.02);
            Detail.feather(c, lb, tpk, new V3(0, 0, 1), 0.05, new V3(1, 0, 0), TN_LF, scale(TN_LF, 1.25));
        }
        Detail.twinkle(c, lb.add(0, 0.11, 0), 0.03, new float[]{0.7F, 1F, 0.7F}, t, ph, 0.2);
        c.head = false;
    }

    // ------------------------------------------------------------------ 24. PATITO PIRATA

    private static final float[] PD_Y = hex(0xFFE04A), PD_Y2 = hex(0xFFF3A0), PD_OR = hex(0xFF9A2E), PD_HAT = hex(0x2B2536), PD_GOLD = hex(0xF2C14E),
            PD_RED = hex(0xD14A4A), PD_STEEL = hex(0xC8D2E0);

    static void pirateDuck(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.4)) * 0.02 * w + 0.008 * c.breath;
        double waddle = Math.sin(c.gait * 1.4) * 0.07 * w;
        c.pivot = new V3(0, 0.2, 0);
        // patas palmeadas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.4 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.045 * w, lift = Math.max(0, Math.cos(p)) * 0.04 * w;
            V3 a = new V3(sd * 0.06, 0.13 + bob, -0.01), b = new V3(sd * 0.065, 0.025 + lift, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.02, 0.018, 0.017}, 6, flat(PD_OR));
            ell(c, b.add(0, -0.01, 0.05), new V3(0.05, 0.012, 0.075), 10, 4, flat(PD_OR));
        }
        // cuerpo de pelusa
        V3 bc = new V3(0, 0.24 + bob, -0.02), br = new V3(0.15, 0.135, 0.17);
        Paint bp = (n, p) -> mix(PD_Y, PD_Y2, ss(0.0, -0.8, n.y) * 0.8);
        ell(c, bc, br, 14, 10, bp);
        Detail.plumage(c, bc, br, 7, 11, 0.045, 0.026, Detail.ALL, bp, PD_Y2);
        // colita parada
        for (int k = -1; k <= 1; k++) {
            Detail.feather(c, bc.add(k * 0.02, 0.05, -0.15), bc.add(k * 0.05, 0.13, -0.24), new V3(1, 0, 0), 0.03, new V3(0, 0.6, -0.8), PD_Y, PD_Y2);
        }
        // alita con espadita
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Math.sin(t * 0.07 + ph + sd) * 0.03 + (sd > 0 ? 0.0 : 0.0) + Math.sin(c.gait * 1.4) * 0.04 * w;
            V3 a = new V3(sd * 0.135, 0.265 + bob, 0.0), b = new V3(sd * 0.19, (sd > 0 ? 0.255 : 0.20) + bob + fl, -0.04);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.04, 0.034, 0.02}, 7, bp);
        }
        // sable de abordaje bien visible, en alto sobre el ala derecha
        V3 hilt = new V3(0.2, 0.255 + bob + Math.sin(t * 0.1 + ph) * 0.006, 0.0);
        tube(c, new V3[]{hilt.add(0, -0.03, 0), hilt.add(0, 0.03, 0)}, new double[]{0.013, 0.013}, 6, flat(PD_HAT));
        ell(c, hilt.add(0, -0.036, 0), new V3(0.02, 0.02, 0.02), 8, 6, flat(PD_GOLD));
        tube(c, new V3[]{hilt.add(-0.05, 0.035, -0.012), hilt.add(0.0, 0.04, 0.0), hilt.add(0.05, 0.035, 0.012)}, new double[]{0.009, 0.011, 0.009}, 5, flat(PD_GOLD));
        ell(c, hilt.add(0, 0.042, 0.0), new V3(0.04, 0.012, 0.04), 10, 4, flat(PD_GOLD));
        V3[] bl = {hilt.add(0, 0.05, 0), hilt.add(0.0, 0.13, 0.03), hilt.add(0.0, 0.2, 0.09), hilt.add(0.0, 0.245, 0.17)};
        curve(c, bl, new double[]{0.017, 0.016, 0.012, 0.003}, 4, 6, flat(PD_STEEL));
        for (int k = 0; k < 3; k++) {
            double u = 0.2 + k * 0.28;
            V3 bp0 = bl[0].lerp(bl[3], u);
            Detail.kite(c, bp0.add(0, 0.0, -0.002), bp0.add(0, 0.04, 0.035), new V3(1, 0, 0), 0.011, new V3(0, 0, 1), WHITE, scale(PD_STEEL, 1.3F));
        }
        Detail.glint(c, hilt.add(0, 0.16, 0.06), new V3(0.03, 0.03, 0.03), new V3(0.3, 0.6, 1), 0.07, WHITE, 0.95);
        Detail.twinkle(c, hilt.add(0, 0.22, 0.14), 0.045, new float[]{1F, 1F, 0.95F}, t, ph, 0.4);
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.34, 0.1);
        V3 hc = new V3(0, 0.40 + bob, 0.10), hr = new V3(0.125, 0.115, 0.115);
        Paint hp = (n, p) -> mix(PD_Y, PD_Y2, 0.25 + ss(0.0, -0.8, n.y) * 0.5);
        ell(c, hc, hr, 14, 10, hp);
        Detail.plumage(c, hc, hr, 4, 9, 0.03, 0.02, Detail.TOP, hp, PD_Y2);
        // pico
        ell(c, new V3(0, 0.375 + bob, 0.215), new V3(0.062, 0.022, 0.07), 10, 5, flat(PD_OR));
        ell(c, new V3(0, 0.362 + bob, 0.205), new V3(0.055, 0.018, 0.058), 10, 5, flat(scale(PD_OR, 0.85)));
        // ojo sano y parche
        decal(c, hc, hr, new V3(0.5, 0.2, 0.85), 0.085, 0.1, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.2, 0.85), 0.1, 0, Detail.T_DISC, PD_HAT, 1, 0.006);
        Detail.glint(c, hc, hr, new V3(-0.55, 0.2, 0.85), 0.02, PD_GOLD, 0.9);
        tube(c, new V3[]{hc.add(-0.11, 0.07, 0.04), hc.add(0.0, 0.125, 0.0), hc.add(0.115, 0.07, 0.04)}, new double[]{0.005, 0.005, 0.005}, 4, flat(PD_HAT));
        cheeks(c, hc, hr, 0.86, -0.2, 0.52, 0.07);
        // sombrero de tres picos
        V3 hat = new V3(0, 0.50 + bob, 0.08);
        Detail.cap(c, hat, new V3(0.11, 0.07, 0.11), 0, 0, 0, 1.5, 0.0, 14, 5, (n, p) -> PD_HAT);
        ell(c, hat.add(0, -0.015, 0), new V3(0.17, 0.014, 0.16), 16, 3, flat(PD_HAT));
        for (int k = 0; k < 3; k++) {
            double a = k * Math.PI * 2 / 3 + Math.PI / 2;
            ell(c, hat.add(Math.cos(a) * 0.12, 0.012, Math.sin(a) * 0.115), new V3(0.07, 0.016, 0.06), -a + Math.PI / 2, 0, 0, 10, 4, flat(PD_HAT));
        }
        ell(c, hat.add(0, -0.012, 0), new V3(0.172, 0.006, 0.162), 16, 3, flat(PD_GOLD));
        // calavera en el sombrero
        Detail.spot(c, hat, new V3(0.11, 0.07, 0.11), new V3(0, 0.1, 1), 0.045, WHITE);
        Detail.spot(c, hat, new V3(0.11, 0.07, 0.11), new V3(-0.14, 0.18, 1), 0.012, PD_HAT);
        Detail.spot(c, hat, new V3(0.11, 0.07, 0.11), new V3(0.14, 0.18, 1), 0.012, PD_HAT);
        // pluma roja
        V3 pf = hat.add(0.09, 0.03, -0.02);
        Detail.feather(c, pf, pf.add(0.06, 0.1, -0.06), new V3(0, 0, 1), 0.026, new V3(1, 0, 0), PD_RED, scale(PD_RED, 1.3));
        c.head = false;
        // brillos de monedas
        double u = ((t * 0.012 + ph) % 1.0);
        Detail.twinkle(c, new V3(0.22, 0.38 + u * 0.1, 0.18), 0.02, new float[]{1F, 0.9F, 0.5F}, t, ph, 0.28);
        Detail.twinkle(c, new V3(-0.2, 0.30, 0.2), 0.016, new float[]{1F, 0.9F, 0.5F}, t, ph + 2, 0.31);
    }

    // ------------------------------------------------------------------ 38. CANGREJO TETERA

    private static final float[] CT_W = hex(0xF4F0E4), CT_B = hex(0x4F7FCF), CT_B2 = hex(0x8CB4F0), CT_R = hex(0xF0705A), CT_R2 = hex(0xFFA078),
            CT_GOLD = hex(0xE8C070);

    static void teapotCrab(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.3)) * 0.012 * w;
        double lidUp = 0.012 + 0.012 * Math.sin(t * 0.08 + ph) + Detail.flick(c, 8) * 0.04;
        c.pivot = new V3(0, 0.18, 0);
        // patitas de cangrejo asomando bajo la tetera
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                double p = c.gait * 1.3 + (k + (sd > 0 ? 1 : 0)) * 2.1;
                double sw = Math.sin(p) * 0.035 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
                double z = 0.06 - k * 0.08;
                V3 a = new V3(sd * 0.12, 0.10 + bob, z), kn = new V3(sd * 0.21, 0.13 + bob, z + sw), fo = new V3(sd * 0.26, 0.015 + lift, z + sw * 1.3);
                tube(c, new V3[]{a, kn, fo}, new double[]{0.018, 0.016, 0.012}, 5, (n, q) -> mix(CT_R, CT_R2, 0.3));
            }
        }
        // cuerpo del cangrejo (se ve abajo)
        V3 cb = new V3(0, 0.12 + bob, 0.02), cr = new V3(0.16, 0.07, 0.15);
        ell(c, cb, cr, 12, 8, (n, q) -> mix(CT_R, CT_R2, ss(-0.2, 0.9, n.y) * 0.6));
        // pinzas
        for (int sd = -1; sd <= 1; sd += 2) {
            double sn = Math.sin(t * 0.09 + ph + sd * 1.5) * 0.015;
            double cl = 0.35 + 0.3 * Math.sin(t * 0.13 + ph + sd) * (0.4 + 0.6 * Detail.flick(c, sd > 0 ? 9 : 10));
            V3 a = new V3(sd * 0.14, 0.13 + bob, 0.1), m = new V3(sd * 0.21, 0.15 + bob + sn, 0.17), tpk = new V3(sd * 0.22, 0.17 + bob + sn, 0.26);
            tube(c, new V3[]{a, m, tpk}, new double[]{0.022, 0.02, 0.018}, 6, (n, q) -> CT_R);
            V3 hand = tpk.add(0, 0, 0.012);
            ell(c, hand, new V3(0.05, 0.04, 0.055), 10, 7, (n, q) -> mix(CT_R, CT_R2, ss(-0.2, 0.8, n.y) * 0.5));
            for (int jw = -1; jw <= 1; jw += 2) {
                V3 jb = hand.add(jw * 0.02, 0.0, 0.035);
                V3 jt = jb.add(jw * 0.012 * cl + sd * 0.0, 0.0, 0.06);
                tube(c, new V3[]{jb, jb.lerp(jt, 0.5).add(jw * 0.008, 0, 0), jt}, new double[]{0.018, 0.015, 0.006}, 6, (n, q) -> CT_R2);
            }
        }
        // ojitos en tallos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.04, 0.17 + bob, 0.15), tp = new V3(sd * 0.05, 0.225 + bob + 0.005 * Math.sin(t * 0.1 + ph + sd), 0.18);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.012, 0.011, 0.01}, 5, flat(CT_R));
            V3 ec = tp.add(0, 0.015, 0.008);
            ell(c, ec, new V3(0.028, 0.028, 0.026), 10, 7, flat(WHITE));
            decal(c, ec, new V3(0.028, 0.028, 0.026), new V3(0, 0, 1), 0.03, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? BLACK : WHITE, 1, 0.003);
        }
        // boquita y rubor
        decal(c, cb, cr, new V3(0, -0.2, 1), 0.05, 0, T_MOUTH, new float[]{0.45F, 0.15F, 0.15F}, 0.95, 0.004);
        cheeks(c, cb, cr, 0.65, 0.0, 0.7, 0.05);
        // tetera encima: cuerpo
        V3 tc = new V3(0, 0.29 + bob, -0.04), tr = new V3(0.185, 0.14, 0.17);
        Paint tp = (n, q) -> CT_W;
        ell(c, tc, tr, 18, 11, tp);
        // dibujos azules (ramitas y flores)
        for (int k = 0; k < 9; k++) {
            double a = k * 0.72;
            Detail.spot(c, tc, tr, new V3(Math.cos(a), 0.35 * Math.sin(a * 1.7), Math.sin(a)), 0.07 - 0.015 * (k % 3), CT_B);
            Detail.spot(c, tc, tr, new V3(Math.cos(a + 0.2), 0.35 * Math.sin(a * 1.7) - 0.28, Math.sin(a + 0.2)), 0.03, CT_B2);
        }
        for (int lv = -1; lv <= 1; lv += 2) {
            ell(c, tc.add(0, lv * 0.092, 0), new V3(tr.x * 0.97 * (lv > 0 ? 0.88 : 0.95), 0.008, tr.z * 0.97 * (lv > 0 ? 0.88 : 0.95)), 18, 3, flat(CT_B));
        }
        // tapa con perilla
        V3 lc = tc.add(0, 0.115 + lidUp, 0);
        Detail.cap(c, lc, new V3(0.095, 0.05, 0.088), 0, 0, 0, 1.4, 0.0, 14, 5, (n, q) -> CT_W);
        ell(c, lc.add(0, -0.003, 0), new V3(0.1, 0.01, 0.092), 14, 3, flat(CT_B));
        ell(c, lc.add(0, 0.055, 0), new V3(0.022, 0.022, 0.022), 8, 6, flat(CT_GOLD));
        // pico (vertedero) y asa
        V3[] sp = {tc.add(0.12, -0.02, 0.1), tc.add(0.2, 0.0, 0.15), tc.add(0.25, 0.07, 0.19)};
        tube(c, sp, new double[]{0.05, 0.034, 0.024}, 8, tp);
        ell(c, sp[2].add(0.008, 0.004, 0.01), new V3(0.03, 0.012, 0.03), 8, 4, flat(CT_B));
        V3[] ha = {tc.add(-0.13, 0.07, -0.11), tc.add(-0.27, 0.11, -0.16), tc.add(-0.3, -0.01, -0.2), tc.add(-0.16, -0.06, -0.13)};
        curve(c, ha, new double[]{0.026, 0.024, 0.024, 0.026}, 4, 8, tp);
        // vapor
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.016 + ph * 0.3 + k * 0.25) % 1.0);
            V3 sm = sp[2].add(0.02 + Math.sin(u * 6 + k) * 0.03, 0.04 + u * 0.3, 0.02 + u * 0.05);
            boolean em = c.emissive;
            c.emissive = true;
            glow(c, sm, 0.06 + 0.09 * u, new float[]{1F, 1F, 1F}, 0.45 * (1 - u));
            c.emissive = em;
        }
    }
}
