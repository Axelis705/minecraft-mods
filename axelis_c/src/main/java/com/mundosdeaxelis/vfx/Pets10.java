package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Rework de mascotas, lote 3 (raras): grifito aprendiz, lechuza mensajera, tortuga isla, pingüino explorador,
 * ladronzuelo enmascarado y mamut peludo.
 */
final class Pets10 {
    private Pets10() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 0. GRIFITO APRENDIZ

    private static final float[] GF_B = hex(0xD9A25A), GF_B2 = hex(0xF0C98A), GF_W = hex(0xFBF6EC), GF_W2 = hex(0xE4D8C0), GF_BK = hex(0xF2B93A), GF_BK2 = hex(0xC98A22),
            GF_WING = hex(0x8A5A3A), GF_WING2 = hex(0xE8B070), GF_SC = hex(0x3FA67A), GF_SC2 = hex(0x7ED9A8), GF_TALON = hex(0xF2B93A);

    static void griffin(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.28, 0);
        // cola de león con mechón
        double wag = Math.sin(t * 0.12 + ph) * 0.04 + Math.sin(c.gait) * 0.03 * w;
        V3[] tc = {new V3(0, 0.30 + bob, -0.24), new V3(wag * 0.5, 0.28 + bob, -0.36), new V3(wag, 0.33 + bob, -0.46), new V3(wag * 1.3, 0.40 + bob, -0.5)};
        curve(c, tc, new double[]{0.03, 0.026, 0.022, 0.016}, 4, 7, flat(GF_B));
        for (int k = 0; k < 5; k++) {
            double a = -0.8 + k * 0.4;
            Detail.kite(c, tc[3], tc[3].add(Math.sin(a) * 0.07 + wag, 0.05 + Math.cos(a) * 0.04, -0.09), new V3(1, 0, 0), 0.026, new V3(0, 1, 0), GF_B, GF_W);
        }
        // patas: traseras de león, delanteras de águila
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.15 : -0.15;
                double phs = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phs) * 0.075 * w, lift = Math.max(0, Math.cos(phs)) * 0.05 * w;
                V3 hip = new V3(sd * 0.09, 0.22 + bob, z0), fo = new V3(sd * 0.09, 0.04 + lift, z0 + sw);
                tube(c, new V3[]{hip, hip.lerp(fo, 0.5), fo}, new double[]{0.048, 0.038, 0.03}, 7, fb == 0 ? flat(GF_W) : flat(GF_B));
                if (fb == 0) {
                    // garras de ave
                    for (int k = -1; k <= 1; k++) {
                        tube(c, new V3[]{fo.add(k * 0.018, 0.0, 0.01), fo.add(k * 0.022, -0.012, 0.06)}, new double[]{0.011, 0.004}, 4, flat(GF_TALON));
                    }
                } else {
                    ell(c, fo.add(0, 0, 0.04), new V3(0.04, 0.03, 0.065), 8, 6, flat(GF_B2));
                }
            }
        }
        // cuerpo de felino
        V3 bc = new V3(0, 0.31 + bob, -0.03), br = new V3(0.14, 0.13, 0.25);
        Paint bp = (n, p) -> mix(GF_B, GF_B2, ss(0.1, -0.9, n.y) * 0.8);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 100, 941, 0.03, 0.016, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        // alitas plegadas con plumas
        double fl = Math.sin(t * 0.17 + ph) * 0.05 + Math.max(0, Math.sin(t * 0.021 + ph) - 0.93) * 8;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.09, 0.40 + bob, 0.0);
            for (int k = 0; k < 5; k++) {
                double a = 0.35 + k * 0.1 + fl * 0.4 + (sd > 0 ? 0 : 0);
                V3 e = sh.add(sd * (0.13 + 0.02 * k + fl * 0.15), 0.19 + 0.01 * k - k * 0.012 + fl * 0.2, -0.12 - 0.045 * k);
                Detail.feather(c, sh.add(0, 0.0, -0.01 * k), e, new V3(0, 1, 0.3).norm(), 0.045, new V3(sd * 0.4, 1, 0).norm(), k < 2 ? GF_WING : GF_WING2, k < 2 ? GF_WING2 : GF_W);
            }
            for (int k = 0; k < 3; k++) {
                Detail.feather(c, sh.add(0, 0.01, 0.01), sh.add(sd * (0.07 + 0.02 * k), 0.11 + 0.01 * k, -0.04 - 0.04 * k), new V3(0, 1, 0), 0.04, new V3(sd * 0.3, 1, 0).norm(), GF_W2, GF_W);
            }
        }
        // bufanda verde de aprendiz
        ell(c, new V3(0, 0.34 + bob, 0.19), new V3(0.12, 0.09, 0.07), 12, 7, flat(GF_SC));
        double wv = Math.sin(t * 0.13 + ph) * 0.01;
        Detail.kite(c, new V3(-0.06, 0.33 + bob, 0.22), new V3(-0.075 + wv, 0.19 + bob, 0.23), new V3(1, 0, 0), 0.032, new V3(0, 0, 1), GF_SC, GF_SC2);
        Detail.kite(c, new V3(-0.06, 0.23 + bob, 0.23), new V3(-0.075 + wv, 0.185 + bob, 0.235), new V3(1, 0, 0), 0.03, new V3(0, 0, 1), GF_SC2, GF_SC2);
        // cabeza de águila
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.40, 0.2);
        V3 hc = new V3(0, 0.49 + bob, 0.27), hr = new V3(0.115, 0.12, 0.115);
        Paint hp = (n, p) -> mix(GF_W, GF_W2, 0.35 + ss(0.2, -0.9, n.y) * 0.6);
        ell(c, hc, hr, 16, 11, hp);
        Detail.plumage(c, hc, hr, 5, 10, 0.045, 0.024, d -> d.z < 0.3 ? 1 : 0.0, hp, GF_W);
        // pico ganchudo
        cone(c, new V3(0, 0.50 + bob, 0.34), new V3(0, 0.485 + bob, 0.44), 0.05, 0.016, 7, flat(GF_BK));
        cone(c, new V3(0, 0.485 + bob, 0.425), new V3(0, 0.445 + bob, 0.45), 0.02, 0.003, 6, flat(GF_BK2));
        cone(c, new V3(0, 0.465 + bob, 0.34), new V3(0, 0.455 + bob, 0.415), 0.04, 0.012, 7, flat(GF_BK2));
        eyes(c, hc, hr, 0.60, 0.22, 0.74, 0.095, T_BEAD, new float[]{1F, 0.95F, 0.8F});
        cheeks(c, hc, hr, 0.85, -0.25, 0.4, 0.07);
        // cejas en pluma y penacho
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 21 : 22);
            for (int k = 0; k < 3; k++) {
                Detail.feather(c, new V3(sd * (0.03 + k * 0.025), 0.595 + bob, 0.27 - k * 0.04), new V3(sd * (0.09 + k * 0.03 + ef * 0.01), 0.64 + bob + k * 0.01, 0.18 - k * 0.07), new V3(0, 0, 1), 0.03, new V3(0, 1, 0), GF_W, GF_W2);
            }
        }
        c.head = false;
        // pluma de escribir en la bufanda
        V3 qb = new V3(0.085, 0.37 + bob, 0.22);
        tube(c, new V3[]{qb, qb.add(0.03, 0.06, -0.01), qb.add(0.06, 0.14, -0.03)}, new double[]{0.004, 0.004, 0.003}, 4, flat(GF_W));
        Detail.feather(c, qb.add(0.03, 0.06, -0.01), qb.add(0.07, 0.16, -0.03), new V3(0, 0, 1), 0.026, new V3(0, 0, 1), GF_SC, GF_SC2);
    }

    // ------------------------------------------------------------------ 2. LECHUZA MENSAJERA

    private static final float[] PO_W = hex(0xFAF3E6), PO_W2 = hex(0xE8D8BC), PO_BR = hex(0xA8784A), PO_BR2 = hex(0xD0A070), PO_BL = hex(0x3A5AA8), PO_BL2 = hex(0x6A8AD8),
            PO_OR = hex(0xF2A63A), PO_BAG = hex(0x8A5A34), PO_RED = hex(0xD8403A), PO_PAP = hex(0xFFFDF4);

    static void postOwl(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.13 + ph) * 0.03;
        c.pivot = new V3(0, 0.38, 0);
        double fl = Math.sin(t * (0.35 + c.speed * 0.2) + ph);
        double flap = 0.25 + 0.55 * (0.5 + 0.5 * fl) * (0.4 + 0.6 * Math.max(c.speed, 0.4));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.16, 0.42 + bob, -0.02);
            for (int k = 0; k < 5; k++) {
                double a = -0.6 - k * 0.2 + flap;
                double len = 0.16 + 0.04 * (4 - Math.abs(k - 1.5)) * 0.9;
                V3 e = sh.add(sd * Math.cos(a) * len * 1.05, Math.sin(a) * len + 0.09 * flap, -0.05 - k * 0.03);
                tube(c, new V3[]{sh, sh.lerp(e, 0.5).add(0, 0.02, 0), e}, new double[]{0.038, 0.032, 0.011}, 5, (n, p) -> mix(PO_BR, PO_BR2, ss(0.1, 0.65, p.y - bob) * 0.8));
            }
        }
        // cola corta
        for (int k = -2; k <= 2; k++) {
            V3 a0 = new V3(k * 0.014, 0.2 + bob, -0.08), b0 = new V3(k * 0.04, 0.12 + bob, -0.23);
            Detail.feather(c, a0, b0, new V3(1, 0, 0), 0.026, new V3(0, 0.6, 0.4).norm(), PO_BR, PO_BR2);
        }
        // cuerpo redondo cremoso
        V3 bc = new V3(0, 0.36 + bob, -0.01), br = new V3(0.17, 0.2, 0.16);
        Paint bp = (n, p) -> {
            double belly = ss(-0.1, 0.5, n.z) * ss(0.9, 0.0, Math.abs(n.x) * 1.2);
            return mix(mix(PO_BR, PO_BR2, ss(-0.2, 0.9, n.y) * 0.4), PO_W, belly);
        };
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 8, 12, 0.05, 0.02, d -> 1, bp, PO_BR2);
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int j = -1; j <= 1; j++) {
                V3 b = new V3(sd * 0.06 + j * 0.02, 0.17 + bob, 0.07);
                tube(c, new V3[]{b, b.add(j * 0.004, -0.03, 0.025), b.add(j * 0.007, -0.04, 0.05)}, new double[]{0.011, 0.007, 0.003}, 4, flat(PO_OR));
            }
        }
        // bolso de cartero cruzado
        V3 bg = new V3(-0.15, 0.27 + bob, 0.0);
        ell(c, bg, new V3(0.05, 0.075, 0.095), 12, 8, flat(PO_BAG));
        ell(c, bg.add(-0.012, 0.04, 0), new V3(0.045, 0.04, 0.098), 12, 6, flat(scale(PO_BAG, 1.15)));
        ell(c, bg.add(-0.05, 0.015, 0.02), new V3(0.01, 0.014, 0.014), 6, 4, flat(PO_RED));
        Detail.spot(c, bg, new V3(0.05, 0.075, 0.095), new V3(-1, 0.2, 0.3), 0.04, hex(0xF2C14E));
        Detail.kite(c, bg.add(-0.02, 0.07, 0.03), bg.add(-0.05, 0.12, 0.04), new V3(0, 0, 1), 0.026, new V3(1, 0, 0), PO_PAP, PO_PAP);
        tube(c, new V3[]{bg.add(0.02, 0.07, 0.0), bc.add(0.0, 0.17, 0.0).add(0.03, 0, 0), bc.add(0.15, 0.1, 0.01)}, new double[]{0.012, 0.012, 0.012}, 5, flat(PO_BAG));
        // cabeza
        headLook(c, 0.16, 0);
        c.hpv = new V3(0, 0.58, 0.0);
        V3 hc = new V3(0, 0.62 + bob, 0.04), hr = new V3(0.185, 0.155, 0.15);
        Paint hp = (n, p) -> mix(PO_BR, PO_BR2, 0.2 + ss(-0.2, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 18, 12, hp);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 fc = new V3(sd * 0.07, 0.625 + bob, 0.145);
            ell(c, fc, new V3(0.075, 0.08, 0.028), 12, 8, flat(PO_W));
            decal(c, fc, new V3(0.075, 0.08, 0.028), new V3(0, 0, 1), 0.115, sd * 0.1, c.shut ? T_EYE_SHUT : T_EYE, c.shut ? WHITE : new float[]{1F, 0.85F, 0.3F}, 1, 0.004);
        }
        cone(c, new V3(0, 0.62 + bob, 0.165), new V3(0, 0.585 + bob, 0.225), 0.03, 0.004, 6, flat(PO_OR));
        // gorra de cartero azul con insignia
        V3 gc = new V3(0, 0.735 + bob, 0.03);
        Detail.cap(c, gc, new V3(0.19, 0.075, 0.17), 0, 0, 0, 1.45, 0.0, 16, 5, (n, p) -> mix(PO_BL, PO_BL2, ss(-0.2, 0.9, n.y)));
        ell(c, gc.add(0, -0.02, 0.15), new V3(0.1, 0.012, 0.075), 12, 4, flat(scale(PO_BL, 0.85)));
        ell(c, gc.add(0, 0.015, 0.175), new V3(0.026, 0.026, 0.006), 8, 4, flat(hex(0xF2C14E)));
        // penachos asomando
        for (int sd = -1; sd <= 1; sd += 2) {
            double fk = Detail.flick(c, sd > 0 ? 23 : 24) * 0.02;
            V3 b = new V3(sd * 0.15, 0.73 + bob, 0.02), tp = new V3(sd * (0.2 + fk), 0.80 + bob, -0.01);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.036, 0.026, 0.004}, 5, flat(PO_BR));
        }
        c.head = false;
        // carta flotando
        V3 lp = new V3(0.27, 0.58 + bob + 0.025 * Math.sin(t * 0.1 + ph), 0.12);
        ell(c, lp, new V3(0.065, 0.045, 0.008), 0, 0, 0.2, 8, 4, flat(PO_PAP));
        ell(c, lp.add(0.0, -0.002, 0.007), new V3(0.014, 0.014, 0.004), 6, 4, flat(PO_RED));
        Detail.glint(c, lp, new V3(0.065, 0.045, 0.008), new V3(-0.3, 0.4, 1), 0.03, WHITE, 0.6);
        // chispita de entrega
        double u = ((t * 0.012 + ph) % 1.0);
        Detail.twinkle(c, new V3(-0.25 - u * 0.1, 0.55 + u * 0.1 + bob, -0.1), 0.02 * (1 - u * 0.4), new float[]{1F, 0.9F, 0.5F}, t, ph, 0.25);
    }

    // ------------------------------------------------------------------ 6. TORTUGA ISLA

    private static final float[] TI_G = hex(0x6FBF7A), TI_G2 = hex(0x9ADB94), TI_SAND = hex(0xF0DCA0), TI_SAND2 = hex(0xD9BE7C), TI_GR = hex(0x4FA050), TI_GR2 = hex(0x86D060),
            TI_TR = hex(0x8A5A34), TI_RK = hex(0x9A9AA8), TI_FL = hex(0xFF8AB0), TI_FLAG = hex(0xE8503A);

    static void islandTurtle(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.8)) * 0.01 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // aletas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.14 : -0.15;
                double phs = c.gait * 0.8 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phs) * 0.06 * w;
                V3 a = new V3(sd * 0.17, 0.13 + bob, z0), b = new V3(sd * 0.24, 0.045, z0 + 0.03 + sw);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.05, 0.042, 0.032}, 6, flat(TI_G));
                ell(c, b.add(sd * 0.01, 0, 0.02), new V3(0.045, 0.018, 0.06), 0, 0, 0, 10, 5, flat(TI_G2));
                for (int k = -1; k <= 1; k++) {
                    ell(c, b.add(sd * 0.01 + k * 0.022, -0.004, 0.075), new V3(0.009, 0.008, 0.014), 5, 4, flat(hex(0xFFF4E0)));
                }
            }
        }
        // colita
        tube(c, new V3[]{new V3(0, 0.15 + bob, -0.24), new V3(0, 0.12 + bob, -0.3), new V3(0, 0.1 + bob, -0.35)}, new double[]{0.03, 0.022, 0.006}, 5, flat(TI_G));
        // cuerpo verde
        V3 bc = new V3(0, 0.17 + bob, 0), br = new V3(0.19, 0.07, 0.25);
        ell(c, bc, br, 14, 8, (n, p) -> TI_G);
        // caparazón: isla
        V3 sc = new V3(0, 0.215 + bob, -0.01), sr = new V3(0.22, 0.14, 0.27);
        Paint sp = (n, p) -> {
            double y = (p.y - bob - 0.215) / 0.14;
            return y > 0.55 ? mix(TI_GR, TI_GR2, ss(0.55, 1.0, y)) : mix(TI_SAND2, TI_SAND, ss(0.0, 0.5, y));
        };
        ell(c, sc, sr, 18, 10, sp);
        ell(c, sc.add(0, -0.012, 0), new V3(0.232, 0.045, 0.282), 18, 4, flat(TI_SAND2));
        // borde de placas verdes festoneadas, espuma de ola y ondas de arena
        for (int k = 0; k < 18; k++) {
            double a = k * Math.PI * 2 / 18;
            V3 pt = sc.add(Math.cos(a) * 0.222, -0.03, Math.sin(a) * 0.272);
            ell(c, pt, new V3(0.042, 0.026, 0.04), 0, 0, 0, 7, 4, flat(mix(TI_GR, TI_G, 0.35 + 0.3 * (k % 2))));
            double fo = 0.004 * Math.sin(t * 0.12 + k * 1.3 + ph);
            ell(c, sc.add(Math.cos(a) * 0.236, -0.048 + fo, Math.sin(a) * 0.286), new V3(0.026, 0.011, 0.026), 5, 3, flat(WHITE));
        }
        for (int k = 0; k < 3; k++) {
            ell(c, sc.add(0, 0.045 + k * 0.016, 0), new V3(0.205 - k * 0.04, 0.004, 0.25 - k * 0.05), 16, 3, flat(mix(TI_SAND2, TI_SAND, 0.35)));
        }
        // conchas de mar y palmerita pequeña
        for (int k = 0; k < 2; k++) {
            V3 cs = sc.add(0.08 + k * 0.07, 0.092 - k * 0.012, 0.14 - k * 0.1);
            ell(c, cs, new V3(0.016, 0.012, 0.018), 0, k * 1.2, 0, 8, 5, flat(hex(0xFFE0E8)));
            ell(c, cs.add(0, 0.006, 0.004), new V3(0.01, 0.008, 0.01), 6, 4, flat(TI_FL));
        }
        V3 sb0 = sc.add(-0.07, 0.115, 0.03);
        curve(c, new V3[]{sb0, sb0.add(-0.005, 0.04, 0), sb0.add(-0.012, 0.075, 0.004)}, new double[]{0.008, 0.007, 0.005}, 3, 5, flat(TI_TR));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.6;
            Detail.feather(c, sb0.add(-0.012, 0.075, 0.004), sb0.add(-0.012 + Math.cos(a) * 0.06, 0.055, 0.004 + Math.sin(a) * 0.06), new V3(-Math.sin(a), 0, Math.cos(a)), 0.024, new V3(0, 1, 0), TI_GR, TI_GR2);
        }
        // palmera
        V3 pb = sc.add(0.04, 0.115, -0.04);
        double sway = Math.sin(t * 0.06 + ph) * 0.01;
        V3[] pt = {pb, pb.add(0.01 + sway, 0.08, 0.0), pb.add(0.03 + sway * 2, 0.16, 0.01), pb.add(0.035 + sway * 3, 0.22, 0.0)};
        curve(c, pt, new double[]{0.018, 0.015, 0.012, 0.01}, 4, 6, flat(TI_TR));
        V3 top = pt[3];
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI * 2 / 6 + 0.3;
            double ca = Math.cos(a), sa = Math.sin(a);
            Detail.feather(c, top, top.add(ca * 0.13, -0.04 + 0.01 * Math.sin(t * 0.08 + ph + k), sa * 0.13), new V3(-sa, 0, ca), 0.05, new V3(0, 1, 0), TI_GR, TI_GR2);
        }
        ell(c, top.add(0.012, -0.012, 0.01), new V3(0.016, 0.016, 0.016), 6, 4, flat(TI_TR));
        ell(c, top.add(-0.012, -0.012, -0.01), new V3(0.016, 0.016, 0.016), 6, 4, flat(TI_TR));
        // bandera
        V3 fb0 = sc.add(-0.08, 0.1, 0.1);
        tube(c, new V3[]{fb0, fb0.add(0, 0.09, 0), fb0.add(0, 0.17, 0)}, new double[]{0.005, 0.005, 0.005}, 4, flat(hex(0xE8E0D0)));
        double fw = Math.sin(t * 0.2 + ph) * 0.015;
        Detail.kite(c, fb0.add(0, 0.17, 0), fb0.add(0.075, 0.145 + fw, 0.0), new V3(0, 1, 0), 0.03, new V3(0, 0, 1), TI_FLAG, scale(TI_FLAG, 1.2));
        // rocas y flores
        ell(c, sc.add(-0.12, 0.085, -0.1), new V3(0.04, 0.03, 0.035), 8, 6, flat(TI_RK));
        ell(c, sc.add(-0.09, 0.075, -0.14), new V3(0.026, 0.02, 0.026), 8, 6, flat(scale(TI_RK, 1.1)));
        for (int k = 0; k < 3; k++) {
            V3 fp = sc.add(0.1 - k * 0.04, 0.115 - k * 0.012, 0.1 + k * 0.05);
            ell(c, fp, new V3(0.014, 0.012, 0.014), 6, 4, flat(TI_FL));
        }
        // cabeza con sonrisa
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.2, 0.25);
        V3 hc = new V3(0, 0.24 + bob, 0.34), hr = new V3(0.1, 0.09, 0.115);
        ell(c, hc, hr, 14, 10, (n, p) -> mix(TI_G, TI_G2, ss(-0.2, 0.9, n.y) * 0.5));
        ell(c, hc.add(0, 0, 0.06), new V3(0.075, 0.065, 0.07), 10, 7, flat(TI_G2));
        eyes(c, hc, hr, 0.55, 0.20, 0.80, 0.09, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.07);
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.075, 0, T_MOUTH, new float[]{0.3F, 0.3F, 0.25F}, 0.95, 0.003);
        ell(c, hc.add(0, 0.015, 0.115), new V3(0.012, 0.009, 0.008), 6, 4, flat(hex(0x3A5A3A)));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = hc.add(sd * 0.075, 0.074, 0.05), b1 = hc.add(sd * 0.025, 0.082, 0.09);
            tube(c, new V3[]{b0, b0.lerp(b1, 0.5).add(0, 0.006, 0), b1}, new double[]{0.009, 0.011, 0.008}, 4, flat(TI_GR));
        }

        c.head = false;
        // brisa marina
        double u = ((t * 0.01 + ph) % 1.0);
        Detail.twinkle(c, new V3(0.2 + u * 0.1, 0.35 + u * 0.2 + bob, 0.1), 0.02 * (1 - u * 0.4), new float[]{0.8F, 0.95F, 1F}, t, ph, 0.2);
    }

    // ------------------------------------------------------------------ 7. PINGÜINO EXPLORADOR

    private static final float[] PX_BK = hex(0x2C3248), PX_BK2 = hex(0x4A5578), PX_W = hex(0xFBFBFF), PX_OR = hex(0xF2A63A), PX_BEAN = hex(0xE8703A), PX_BEAN2 = hex(0xFFB070),
            PX_SC = hex(0x3AA8C8), PX_PACK = hex(0x5A8A4A), PX_PACK2 = hex(0x8A6A3A), PX_ICE = hex(0xBFE4F5);

    static void explorerPenguin(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double waddle = Math.sin(c.gait * 1.2) * 0.07 * w;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.012 * w + 0.006 * c.breath;
        c.pivot = new V3(0, 0.3, 0);
        c.roll += waddle;
        // pies
        for (int sd = -1; sd <= 1; sd += 2) {
            double phs = c.gait * 1.2 + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(phs) * 0.06 * w, lift = Math.max(0, Math.cos(phs)) * 0.04 * w;
            V3 fp0 = new V3(sd * 0.08, 0.025 + lift, 0.07 + sw);
            ell(c, fp0.add(0, 0, -0.03), new V3(0.04, 0.02, 0.05), 8, 5, flat(scale(PX_OR, 0.85F)));
            for (int k = -1; k <= 1; k++) {
                ell(c, fp0.add(k * 0.032, 0.0, 0.045), new V3(0.019, 0.016, 0.05), 6, 4, flat(PX_OR));
                ell(c, fp0.add(k * 0.034, 0.0, 0.088), new V3(0.012, 0.011, 0.012), 5, 4, flat(PX_BK));
            }
        }
        // mochila (detrás)
        V3 pk = new V3(0, 0.30 + bob, -0.14);
        ell(c, pk, new V3(0.1, 0.12, 0.07), 12, 8, flat(PX_PACK));
        ell(c, pk.add(0, 0.12, -0.01), new V3(0.095, 0.035, 0.07), 12, 5, flat(scale(PX_PACK, 1.15)));
        ell(c, pk.add(0, -0.12, -0.02), new V3(0.1, 0.03, 0.065), 0, 0, 0, 12, 5, flat(PX_PACK2));
        ell(c, pk.add(0, 0.0, -0.065), new V3(0.06, 0.05, 0.016), 10, 6, flat(PX_PACK2));
        // saco de dormir enrollado arriba
        ell(c, pk.add(0, 0.185, -0.01), new V3(0.12, 0.034, 0.034), 0, 0, 1.5708, 10, 6, flat(PX_SC));
        // cuerpo
        V3 bc = new V3(0, 0.30 + bob, 0), br = new V3(0.15, 0.2, 0.135);
        Paint bp = (n, p) -> {
            double belly = ss(0.0, 0.5, n.z) * ss(0.95, 0.3, Math.abs(n.x));
            return mix(mix(PX_BK, PX_BK2, ss(0.2, 0.9, n.y) * 0.3), PX_W, belly);
        };
        ell(c, bc, br, 16, 11, bp);
        Detail.soft(c, bc, br, new V3(0, 0.1, 1), 0.2, new float[]{1F, 1F, 1F}, 0.5);
        // colita corta y plumas del pecho
        cone(c, new V3(0, 0.14 + bob, -0.12), new V3(0, 0.09 + bob, -0.2), 0.03, 0.004, 5, flat(PX_BK2));
        // aletas
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(c.gait * 1.2 + (sd > 0 ? 0 : Math.PI)) * 0.15 * w;
            double fl = sd > 0 ? 0 : 0;
            V3 a = new V3(sd * 0.135, 0.36 + bob, 0.0);
            V3 b = sd > 0 ? new V3(0.235, 0.35 + bob + 0.0, 0.07) : new V3(-0.23, 0.30 + bob, 0.04 + sw * 0.3);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.034, 0.03, 0.014}, 6, flat(PX_BK2));
        }
        // piolet en la aleta derecha
        V3 hnd = new V3(0.235, 0.35 + bob, 0.07);
        tube(c, new V3[]{hnd.add(0, -0.1, 0.0), hnd.add(0, 0.12, 0.0)}, new double[]{0.008, 0.008}, 5, flat(PX_PACK2));
        cone(c, hnd.add(0.0, 0.12, 0.0), hnd.add(0.0, 0.14, 0.05), 0.012, 0.003, 4, flat(PX_ICE));
        cone(c, hnd.add(0.0, 0.12, 0.0), hnd.add(0.0, 0.14, -0.04), 0.012, 0.003, 4, flat(PX_ICE));
        // cabeza
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.50, 0.0);
        V3 hc = new V3(0, 0.55 + bob, 0.02), hr = new V3(0.125, 0.115, 0.115);
        Paint hp = (n, p) -> mix(PX_BK, PX_BK2, ss(0.0, 0.9, n.y) * 0.3);
        ell(c, hc, hr, 16, 11, hp);
        ell(c, hc.add(0, -0.03, 0.065), new V3(0.095, 0.075, 0.05), 12, 8, flat(PX_W));
        // pico
        cone(c, new V3(0, 0.545 + bob, 0.11), new V3(0, 0.53 + bob, 0.2), 0.04, 0.012, 7, flat(PX_OR));
        cone(c, new V3(0, 0.525 + bob, 0.11), new V3(0, 0.515 + bob, 0.18), 0.03, 0.01, 7, flat(scale(PX_OR, 0.85)));
        eyes(c, hc, hr, 0.55, 0.15, 0.80, 0.09, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.85, -0.2, 0.5, 0.07);
        // gorro con pompón
        V3 gc = new V3(0, 0.625 + bob, 0.005);
        Detail.cap(c, gc, new V3(0.13, 0.085, 0.12), 0, 0, 0, 1.5, 0.0, 16, 5, (n, p) -> mix(PX_BEAN, PX_BEAN2, 0.5 + 0.5 * Math.sin(p.x * 70)));
        ell(c, gc.add(0, -0.025, 0), new V3(0.13, 0.025, 0.12), 14, 4, flat(PX_BEAN2));
        ell(c, gc.add(0, 0.08, 0), new V3(0.032, 0.032, 0.032), 8, 6, flat(PX_W));
        Detail.fur(c, gc.add(0, 0.08, 0), new V3(0.032, 0.032, 0.032), 20, 951, 0.02, 0.01, new V3(0, 1, 0), 0.3, Detail.ALL, flat(PX_W), null);
        // gafas de nieve en la frente
        ell(c, hc.add(0, 0.075, 0.075), new V3(0.105, 0.022, 0.05), 12, 4, flat(PX_BK));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.05, 0.085, 0.108), new V3(0.04, 0.034, 0.014), 10, 6, flat(PX_ICE));
        }
        c.head = false;
        // bufanda
        ell(c, new V3(0, 0.455 + bob, 0.015), new V3(0.115, 0.032, 0.105), 14, 5, flat(PX_SC));
        double wv = Math.sin(t * 0.15 + ph) * 0.012 + w * 0.02;
        V3 sb = new V3(0.05, 0.45 + bob, 0.085);
        curve(c, new V3[]{sb, sb.add(0.01, -0.06, 0.02), sb.add(0.015 + wv, -0.12, 0.015), sb.add(0.02 + wv * 2, -0.17, 0.0)}, new double[]{0.026, 0.024, 0.02, 0.008}, 3, 6, flat(PX_SC));
        // copos
        double u = ((t * 0.012 + ph) % 1.0);
        for (int k = 0; k < 3; k++) {
            double uu = (u + k * 0.33) % 1.0;
            Detail.twinkle(c, new V3(Math.sin(k * 2.4 + uu * 3) * 0.25, 0.7 - uu * 0.55 + bob, Math.cos(k * 1.9) * 0.2), 0.016, new float[]{0.9F, 0.97F, 1F}, t, ph + k, 0.2);
        }
    }

    // ------------------------------------------------------------------ 19. LADRONZUELO ENMASCARADO

    private static final float[] RC_G = hex(0x8F8A96), RC_G2 = hex(0xB8B2C0), RC_BK = hex(0x2E2A36), RC_CR = hex(0xF4EEE6), RC_ST = hex(0x2E2A36), RC_SK = hex(0xB8864A),
            RC_SK2 = hex(0xE0B060), RC_COIN = hex(0xF6C84A), RC_GEM = hex(0x4FE0C8);

    static void maskedThief(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.26, 0);
        // cola anillada
        double wag = Math.sin(t * 0.13 + ph) * 0.03 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.27 + bob, -0.2), new V3(wag * 0.5, 0.25 + bob, -0.32), new V3(wag * 1.2, 0.28 + bob, -0.44), new V3(wag * 1.7, 0.33 + bob, -0.54)};
        double[] trd = {0.045, 0.058, 0.058, 0.04};
        Paint tp = (n, p) -> {
            double ring = Math.sin(-p.z * 38) > 0.15 ? 1 : 0;
            return mix(RC_G2, RC_BK, ring * 0.9);
        };
        curve(c, tcp, trd, 4, 8, tp);
        Detail.furTube(c, tcp, trd, 40, 961, 0.03, 0.016, 0.7, tp, null);
        ell(c, tcp[3].add(0, 0.0, -0.01), new V3(0.04, 0.04, 0.04), 8, 6, flat(RC_BK));
        legs4(c, 0.085, 0.13, -0.14, 0.21 + bob, 0.04, 0.035, 0.055, (n, p) -> mix(RC_G, RC_BK, ss(0.14, 0.05, p.y)), flat(RC_BK));
        // cuerpo
        V3 bc = new V3(0, 0.29 + bob, -0.02), br = new V3(0.135, 0.125, 0.21);
        Paint bp = (n, p) -> mix(mix(RC_G, RC_G2, ss(0.0, 0.9, n.y) * 0.5), RC_CR, ss(-0.4, -0.9, n.y) * 0.7);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 60, 962, 0.03, 0.016, new V3(0, -0.5, -1), 0.8, Detail.ALL, bp, null);
        // camiseta de rayas (negro/blanco)
        ell(c, bc.add(0, -0.005, 0.03), new V3(0.15, 0.132, 0.17), 28, 16, (n, p) -> Math.sin(p.z * 52) > 0 ? RC_ST : RC_CR);
        // saco del botín al hombro
        V3 sk = new V3(-0.04, 0.44 + bob, -0.13);
        ell(c, sk, new V3(0.13, 0.115, 0.12), 12, 8, (n, p) -> mix(RC_SK, RC_SK2, ss(-0.3, 0.9, n.y) * 0.5));
        ell(c, sk.add(0.05, 0.09, 0.0), new V3(0.05, 0.06, 0.05), 8, 6, flat(RC_SK));
        ell(c, sk.add(0.06, 0.145, 0.0), new V3(0.05, 0.015, 0.05), 8, 4, flat(scale(RC_SK, 0.7)));
        // moneda y gema asomando
        ell(c, sk.add(0.025, 0.14, 0.03), new V3(0.04, 0.04, 0.008), 10, 4, flat(RC_COIN));
        Detail.glint(c, sk.add(0.025, 0.14, 0.03), new V3(0.04, 0.04, 0.008), new V3(-0.3, 0.4, 1), 0.026, WHITE, 0.9);
        Detail.kite(c, sk.add(0.085, 0.17, -0.02), sk.add(0.085, 0.215, -0.02), new V3(1, 0, 0), 0.018, new V3(0, 0, 1), RC_GEM, WHITE);
        Detail.kite(c, sk.add(0.085, 0.17, -0.02), sk.add(0.085, 0.14, -0.02), new V3(1, 0, 0), 0.018, new V3(0, 0, 1), RC_GEM, scale(RC_GEM, 0.7));
        // cabeza
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.38, 0.17);
        V3 hc = new V3(0, 0.45 + bob, 0.25), hr = new V3(0.15, 0.125, 0.135);
        Paint hp = (n, p) -> mix(RC_G, RC_G2, ss(0.0, 0.9, n.y) * 0.6);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 60, 963, 0.028, 0.014, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.5 ? 1 : 0.0, hp, null);
        // antifaz oscuro
        ell(c, hc.add(0, 0.028, 0.108), new V3(0.04, 0.026, 0.03), 8, 5, flat(RC_BK));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 mk = hc.add(sd * 0.07, 0.03, 0.098);
            ell(c, mk, new V3(0.07, 0.048, 0.04), 0, 0, sd * -0.25, 12, 8, flat(RC_BK));
            V3 ew = mk.add(0, 0.0, 0.026);
            ell(c, ew, new V3(0.032, 0.036, 0.018), 10, 7, flat(WHITE));
            double lk = Math.sin(t * 0.05 + ph) * 0.008;
            ell(c, ew.add(lk, 0.0, 0.012), new V3(0.021, 0.027, 0.012), 8, 6, flat(BLACK));
            Detail.glint(c, ew.add(lk, 0.0, 0.012), new V3(0.021, 0.027, 0.012), new V3(-0.3, 0.5, 1), 0.012, WHITE, 0.95);
            // ceja pícara
            V3 b0 = hc.add(sd * 0.1, 0.082, 0.085), b1 = hc.add(sd * 0.035, 0.092 + (sd > 0 ? 0.01 : 0), 0.108);
            tube(c, new V3[]{b0, b0.lerp(b1, 0.5).add(0, 0.006, 0), b1}, new double[]{0.008, 0.01, 0.007}, 4, flat(RC_BK));
        }
        V3 sc = new V3(0, 0.42 + bob, 0.355), sr = new V3(0.065, 0.05, 0.07);
        ell(c, sc, sr, 10, 7, flat(RC_CR));
        ell(c, sc.add(0, 0.022, 0.065), new V3(0.024, 0.018, 0.02), 8, 5, flat(BLACK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.08, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.9, 0.003);
        // orejitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 25 : 26) * 0.03;
            V3 b = new V3(sd * 0.1, 0.53 + bob, 0.2), tpe = new V3(sd * (0.14 + ef), 0.62 + bob - ef * 2, 0.18);
            tube(c, new V3[]{b, b.lerp(tpe, 0.5), tpe}, new double[]{0.05, 0.04, 0.012}, 5, flat(RC_G));
            V3 b2 = b.add(0, 0.005, 0.02), t2 = tpe.add(0, -0.03, 0.02);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.032, 0.026, 0.006}, 5, flat(RC_BK));
        }
        // gorro de ladrón de rayas
        V3 gc = new V3(0.0, 0.545 + bob, 0.2);
        Detail.cap(c, gc, new V3(0.145, 0.075, 0.125), 0, 0, 0, 1.45, 0.0, 16, 5, (n, p) -> Math.sin(p.x * 38 + p.z * 10) > 0 ? RC_ST : RC_CR);
        ell(c, gc.add(0, 0.07, -0.05), new V3(0.026, 0.026, 0.026), 8, 6, flat(RC_ST));
        c.head = false;
        // manita con gemita
        V3 hd = new V3(0.1, 0.19 + bob, 0.22);
        Detail.twinkle(c, hd.add(0, 0.05, 0), 0.02, new float[]{0.6F, 1F, 0.9F}, t, ph, 0.3);
    }

    // ------------------------------------------------------------------ 41. MAMUT PELUDO

    private static final float[] MM_F = hex(0x8A5A3E), MM_F2 = hex(0xB88460), MM_DK = hex(0x5A3A2A), MM_TU = hex(0xF2E8D0), MM_TU2 = hex(0xCDBB98), MM_SN = hex(0xFFFFFF),
            MM_SC = hex(0x5AB0E0), MM_SC2 = hex(0xA0D8F4), MM_PK = hex(0xE8A0A0);

    static void mammoth(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.8)) * 0.012 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.3, 0);
        // cola
        tube(c, new V3[]{new V3(0, 0.33 + bob, -0.3), new V3(0, 0.28 + bob, -0.34), new V3(0, 0.21 + bob, -0.35)}, new double[]{0.026, 0.02, 0.012}, 5, flat(MM_DK));
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.16 : -0.18;
                double phs = c.gait * 0.8 + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phs) * 0.055 * w, lift = Math.max(0, Math.cos(phs)) * 0.04 * w;
                V3 hip = new V3(sd * 0.115, 0.24 + bob, z0), fo = new V3(sd * 0.115, 0.045 + lift, z0 + sw);
                tube(c, new V3[]{hip, hip.lerp(fo, 0.5), fo}, new double[]{0.07, 0.065, 0.06}, 8, (n, p) -> mix(MM_F, MM_DK, ss(0.14, 0.04, p.y)));
                ell(c, fo.add(0, -0.01, 0.015), new V3(0.072, 0.034, 0.08), 10, 6, flat(MM_DK));
                for (int k = -1; k <= 1; k++) {
                    ell(c, fo.add(k * 0.03, -0.012, 0.075), new V3(0.016, 0.014, 0.012), 6, 4, flat(MM_TU2));
                }
            }
        }
        // cuerpo peludo
        V3 bc = new V3(0, 0.33 + bob, -0.03), br = new V3(0.2, 0.18, 0.27);
        Paint bp = (n, p) -> mix(MM_F, MM_F2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 240, 971, 0.07, 0.03, new V3(0, -1, -0.1), 0.9, Detail.ALL, bp, MM_F2);
        // faldón largo de pelo
        Detail.fur(c, bc.add(0, -0.08, 0), new V3(0.2, 0.1, 0.27), 90, 972, 0.09, 0.03, new V3(0, -1, 0), 1.0, Detail.SIDES, (n, p) -> MM_DK, MM_F);
        // nieve en el lomo
        Detail.fur(c, bc.add(0, 0.06, 0), br, 50, 973, 0.03, 0.04, new V3(0, 0.2, -1), 0.3, Detail.TOP, flat(MM_SN), null);
        ell(c, bc.add(0, 0.17, 0.02), new V3(0.12, 0.04, 0.15), 12, 5, flat(MM_SN));
        ell(c, bc.add(0.03, 0.2, -0.04), new V3(0.06, 0.03, 0.07), 10, 5, flat(MM_SN));
        // cabeza con cúpula alta
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.40, 0.25);
        V3 hc = new V3(0, 0.44 + bob, 0.30), hr = new V3(0.16, 0.16, 0.14);
        Paint hp = (n, p) -> mix(MM_F, MM_F2, ss(0.0, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        // cúpula (copete)
        ell(c, hc.add(0, 0.12, -0.04), new V3(0.12, 0.1, 0.1), 12, 8, hp);
        Detail.fur(c, hc.add(0, 0.12, -0.04), new V3(0.12, 0.1, 0.1), 60, 974, 0.07, 0.03, new V3(0, 0.3, -1), 0.5, Detail.ALL, hp, MM_F2);
        Detail.fur(c, hc, hr, 70, 975, 0.05, 0.025, new V3(0, -0.5, -1), 0.8, d -> d.z < 0.55 ? 1 : 0.0, hp, MM_F2);
        // trompa
        double sw = Math.sin(t * 0.07 + ph) * 0.03 + c.joy * 0.03;
        V3[] tr = {new V3(0, 0.42 + bob, 0.40), new V3(0, 0.33 + bob, 0.45), new V3(sw * 0.4, 0.22 + bob, 0.47), new V3(sw, 0.15 + bob, 0.52), new V3(sw * 1.4, 0.15 + bob, 0.58)};
        curve(c, tr, new double[]{0.07, 0.058, 0.05, 0.042, 0.03}, 4, 8, (n, p) -> mix(MM_F, MM_F2, ss(0.3, 0.1, p.y - bob) * 0.5));
        ell(c, tr[4].add(0, 0.0, 0.015), new V3(0.03, 0.026, 0.026), 8, 6, flat(MM_PK));
        // colmillos curvos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] tk = {new V3(sd * 0.085, 0.37 + bob, 0.36), new V3(sd * 0.16, 0.30 + bob, 0.46), new V3(sd * 0.20, 0.22 + bob, 0.58), new V3(sd * 0.15, 0.20 + bob, 0.70), new V3(sd * 0.07, 0.26 + bob, 0.72)};
            curve(c, tk, new double[]{0.034, 0.03, 0.025, 0.017, 0.005}, 4, 8, (n, p) -> mix(MM_TU, MM_TU2, ss(0.25, 0.2, p.y - bob) * 0.6));
        }
        eyes(c, hc, hr, 0.60, 0.10, 0.74, 0.08, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.88, -0.2, 0.35, 0.065);
        // orejas pequeñas peludas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 27 : 28) * 0.03;
            V3 ec = new V3(sd * (0.16 + ef), 0.46 + bob, 0.24);
            ell(c, ec, new V3(0.035, 0.07, 0.06), 0, 0, sd * 0.2, 10, 7, flat(MM_F));
            ell(c, ec.add(sd * 0.01, -0.005, 0.01), new V3(0.026, 0.05, 0.04), 8, 6, flat(MM_PK));
        }
        c.head = false;
        // bufanda azul
        ell(c, new V3(0, 0.34 + bob, 0.2), new V3(0.17, 0.05, 0.1), 14, 5, flat(MM_SC));
        double wv = Math.sin(t * 0.12 + ph) * 0.01;
        Detail.kite(c, new V3(0.12, 0.33 + bob, 0.22), new V3(0.13 + wv, 0.19 + bob, 0.24), new V3(1, 0, 0), 0.035, new V3(0, 0, 1), MM_SC, MM_SC2);
        // copos
        double u = ((t * 0.012 + ph) % 1.0);
        for (int k = 0; k < 3; k++) {
            double uu = (u + k * 0.33) % 1.0;
            Detail.twinkle(c, new V3(Math.sin(k * 2.4 + uu * 3) * 0.3, 0.72 - uu * 0.5 + bob, Math.cos(k * 1.9) * 0.25), 0.018, new float[]{0.9F, 0.97F, 1F}, t, ph + k, 0.2);
        }
    }
}
