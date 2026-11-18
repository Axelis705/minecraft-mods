package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/** Rework de mascotas, lote 5 (legendarias): ave trueno y ballena de cielo. */
final class Pets12 {
    private Pets12() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // zigzag emisivo de relámpago
    private static void bolt(Ctx c, V3 a, V3 b, double amp, int seg, double r, long seed, float[] col) {
        java.util.Random rnd = new java.util.Random(seed);
        V3[] pts = new V3[seg + 1];
        double[] rd = new double[seg + 1];
        V3 d = b.sub(a);
        V3 side = d.cross(new V3(0, 1, 0.3)).norm();
        V3 up = d.cross(side).norm();
        for (int i = 0; i <= seg; i++) {
            double u = i / (double) seg;
            double k = (i == 0 || i == seg) ? 0 : 1;
            pts[i] = a.lerp(b, u).add(side.mul((rnd.nextDouble() - 0.5) * amp * k)).add(up.mul((rnd.nextDouble() - 0.5) * amp * k));
            rd[i] = r * (1 - u * 0.6);
        }
        boolean em = c.emissive;
        c.emissive = true;
        tube(c, pts, rd, 4, flat(col));
        c.emissive = em;
    }

    // ------------------------------------------------------------------ 5. AVE TRUENO

    private static final float[] TB_B = hex(0x2A2F6A), TB_B2 = hex(0x4A58B8), TB_Y = hex(0xFFE04A), TB_Y2 = hex(0xFFF6A8), TB_W = hex(0xE8EEFF), TB_CL = hex(0xC8D0E8),
            TB_CL2 = hex(0x8A94B8), TB_BK = hex(0xF2A63A);

    static void thunderbird(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.15 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double fl = Math.sin(t * (0.42 + c.speed * 0.15) + ph);
        double flap = 0.3 + 0.7 * (0.5 + 0.5 * fl);
        double pulse = 0.7 + 0.3 * Math.sin(t * 0.37 + ph * 3) * Math.sin(t * 0.21 + ph);
        glow(c, new V3(0, 0.42 + bob, 0), 1.1, new float[]{0.4F, 0.55F, 1F}, 0.16 + 0.06 * pulse);
        // nubecita bajo el ave
        for (int k = 0; k < 5; k++) {
            double a = k * 2.4;
            ell(c, new V3(Math.sin(a) * 0.1, 0.2 + bob - 0.01 * k, Math.cos(a) * 0.08), new V3(0.09, 0.05, 0.08), 8, 6, flat(k % 2 == 0 ? TB_CL : TB_CL2));
        }
        // cola: 3 plumas en zigzag de relámpago
        for (int k = -1; k <= 1; k++) {
            V3 a0 = new V3(k * 0.03, 0.34 + bob, -0.12);
            V3 b0 = new V3(k * 0.14 + Math.sin(t * 0.12 + ph + k) * 0.03, 0.2 + bob, -0.62 - 0.05 * (1 - Math.abs(k)));
            bolt(c, a0, b0, 0.07, 5, 0.026, 100 + k, k == 0 ? TB_Y : TB_Y2);
            glow(c, b0, 0.12, new float[]{1F, 0.95F, 0.5F}, 0.3 * pulse);
        }
        // alas: abanico azul con puntas amarillas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.15, 0.46 + bob, -0.01);
            V3 tip = sh;
            for (int k = 0; k < 6; k++) {
                double a = 0.55 - k * 0.20 - (flap - 0.5) * 1.5;
                double len = 0.30 + 0.06 * Math.sin(k * 0.9);
                V3 e = sh.add(sd * Math.cos(a) * len * 1.15, Math.sin(a) * len + 0.05, -0.07 - k * 0.032);
                if (k == 0) {
                    tip = e;
                }
                final V3 shf = sh;
                tube(c, new V3[]{sh, sh.lerp(e, 0.5).add(0, 0.015, 0), e}, new double[]{0.042, 0.035, 0.006}, 5,
                        (n, p) -> mix(mix(TB_B, TB_B2, 0.4), TB_Y, ss(0.55, 1.0, p.sub(shf).len() / 0.34)));
            }
            // pelo de relámpagos al borde del ala
            double ex = ((t * 0.05 + ph + sd) % 1.0);
            if (ex < 0.5) {
                bolt(c, tip, tip.add(sd * 0.1, 0.12 * (ex + 0.3), 0.04), 0.05, 3, 0.012, (long) (t * 0.2) + sd * 7, TB_Y2);
            }
        }
        // cuerpo
        V3 bc = new V3(0, 0.40 + bob, 0), br = new V3(0.17, 0.17, 0.2);
        Paint bp = (n, p) -> mix(mix(TB_B, TB_B2, ss(-0.5, 0.8, n.y) * 0.7), TB_W, ss(0.0, 0.8, n.z) * ss(0.1, -0.7, n.y));
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 10, 13, 0.06, 0.02, Detail.ALL, bp, TB_B2);
        // rayo en el pecho
        boolean em = c.emissive;
        c.emissive = true;
        Detail.kite(c, bc.add(0.02, 0.07, 0.17), bc.add(-0.035, -0.02, 0.185), new V3(1, 0, 0), 0.03, new V3(0, 0, 1), TB_Y, TB_Y2);
        Detail.kite(c, bc.add(-0.035, -0.0, 0.185), bc.add(0.02, -0.09, 0.172), new V3(1, 0, 0), 0.026, new V3(0, 0, 1), TB_Y, TB_Y2);
        c.emissive = em;
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.06, 0.27 + bob, 0.05);
            tube(c, new V3[]{b, b.add(0, -0.05, 0.015)}, new double[]{0.016, 0.011}, 4, flat(TB_BK));
            for (int j = -1; j <= 1; j++) {
                V3 q = b.add(j * 0.016, -0.05, 0.015);
                tube(c, new V3[]{q, q.add(j * 0.008, -0.008, 0.04)}, new double[]{0.009, 0.003}, 3, flat(TB_BK));
            }
        }
        // cabeza
        headLook(c, 0.14, 0);
        c.hpv = new V3(0, 0.52, 0.08);
        V3 hc = new V3(0, 0.595 + bob, 0.1), hr = new V3(0.125, 0.115, 0.115);
        Paint hp = (n, p) -> mix(TB_B, TB_B2, ss(0.0, 0.9, n.y) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        Detail.plumage(c, hc, hr, 5, 10, 0.04, 0.02, d -> d.z < 0.2 ? 1 : 0.0, hp, TB_B2);
        ell(c, hc.add(0, -0.03, 0.06), new V3(0.09, 0.07, 0.06), 12, 8, flat(TB_W));
        cone(c, new V3(0, 0.595 + bob, 0.19), new V3(0, 0.575 + bob, 0.27), 0.04, 0.008, 6, flat(TB_BK));
        cone(c, new V3(0, 0.572 + bob, 0.19), new V3(0, 0.56 + bob, 0.25), 0.03, 0.008, 6, flat(scale(TB_BK, 0.85)));
        eyes(c, hc, hr, 0.60, 0.22, 0.74, 0.10, T_BEAD, new float[]{1F, 0.95F, 0.5F});
        cheeks(c, hc, hr, 0.86, -0.25, 0.4, 0.07);
        // cresta de relámpagos
        c.emissive = true;
        for (int k = -1; k <= 1; k++) {
            double fk = Detail.flick(c, 36 + k) * 0.02;
            V3 b0 = hc.add(k * 0.04, 0.1, -0.02);
            Detail.kite(c, b0, b0.add(k * 0.035 + fk, 0.09, -0.05), new V3(1, 0, 0), 0.026, new V3(0, 0, 1), TB_Y, TB_Y2);
            Detail.kite(c, b0.add(k * 0.035 + fk, 0.09, -0.05), b0.add(k * 0.02 + fk, 0.17, -0.1), new V3(1, 0, 0), 0.02, new V3(0, 0, 1), TB_Y2, TB_Y);
        }
        c.emissive = em;
        c.head = false;
        // chispas
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.02 + ph * 0.2 + k * 0.17) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.7 + u * 5) * 0.3, 0.35 + bob + u * 0.4, Math.cos(k * 1.9) * 0.2 - 0.05), 0.024 * (1 - u * 0.6), new float[]{1F, 0.95F, 0.5F}, t, ph + k, 0.45);
        }
    }

    // ------------------------------------------------------------------ 21. BALLENA DE CIELO

    private static final float[] SW_B = hex(0x6EB6F0), SW_B2 = hex(0xA8DAFF), SW_W = hex(0xFFFFFF), SW_BEL = hex(0xFFF0F6), SW_GR = hex(0x6FC070), SW_GR2 = hex(0x9AE08A),
            SW_TR = hex(0x8A5A34), SW_HUT = hex(0xFFE0B8), SW_ROOF = hex(0xE8503A), SW_CL = hex(0xFFFFFF), SW_PK = hex(0xFFA8C8);

    static void skyWhale(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.07 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double sw = Math.sin(t * 0.14 + ph);
        // cola con aleta ancha
        V3[] tl = {new V3(0, 0.38 + bob, -0.2), new V3(0, 0.4 + bob + sw * 0.01, -0.38), new V3(0, 0.45 + bob + sw * 0.04, -0.52), new V3(0, 0.52 + bob + sw * 0.08, -0.62)};
        curve(c, tl, new double[]{0.14, 0.095, 0.06, 0.035}, 4, 10, (n, p) -> mix(SW_B, SW_B2, ss(-0.3, 0.9, n.y) * 0.5));
        V3 fp = tl[3];
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] fl = {fp, fp.add(sd * 0.1, 0.03 + sw * 0.03, -0.04), fp.add(sd * 0.2, 0.09 + sw * 0.07, -0.05)};
            curve(c, fl, new double[]{0.04, 0.035, 0.01}, 3, 6, flat(SW_B2));
            Detail.kite(c, fp.add(sd * 0.02, 0, 0), fp.add(sd * 0.2, 0.09 + sw * 0.07, -0.08), new V3(0, 0, 1), 0.075, new V3(0, 1, 0), SW_B, SW_PK);
        }
        // cuerpo grande con barriga rosada
        V3 bc = new V3(0, 0.40 + bob, 0.02), br = new V3(0.22, 0.21, 0.31);
        Paint bp = (n, p) -> {
            float[] top = mix(SW_B, SW_B2, ss(-0.3, 0.9, n.y) * 0.6);
            double stripe = (Math.sin(p.z * 55) > 0.4) ? 0.35 : 0.0;
            return mix(mix(top, SW_BEL, ss(0.0, -0.7, n.y)), SW_PK, ss(-0.5, -1.0, n.y) * (0.2 + stripe));
        };
        ell(c, bc, br, 18, 12, bp);
        // aletas pectorales
        for (int sd = -1; sd <= 1; sd += 2) {
            double ff = Math.sin(t * 0.16 + ph + (sd > 0 ? 0 : 1)) * 0.07;
            V3 a = new V3(sd * 0.19, 0.33 + bob, 0.1), b = new V3(sd * (0.35 + ff * 0.2), 0.22 + bob + ff, 0.0);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.05, 0.045, 0.012}, 6, flat(SW_B2));
            Detail.kite(c, a, b, new V3(0, 0, 1), 0.06, new V3(0, 1, 0), SW_B2, SW_PK);
        }
        // jardín flotante en el lomo: nubes, árbol y casita
        V3 top = bc.add(0, 0.2, -0.02);
        for (int k = 0; k < 7; k++) {
            double a = k * 2.2;
            ell(c, top.add(Math.sin(a) * 0.12, -0.02 + 0.015 * Math.sin(a * 2), Math.cos(a) * 0.17), new V3(0.075, 0.045, 0.075), 8, 6, flat(SW_CL));
        }
        ell(c, top.add(0, 0.015, 0), new V3(0.16, 0.035, 0.22), 12, 5, (n, p) -> mix(SW_GR, SW_GR2, ss(-0.2, 0.9, n.x)));
        // arbolito
        V3 tb = top.add(-0.05, 0.03, -0.06);
        tube(c, new V3[]{tb, tb.add(0, 0.05, 0), tb.add(0, 0.1, 0)}, new double[]{0.014, 0.012, 0.01}, 5, flat(SW_TR));
        double sy = Math.sin(t * 0.06 + ph) * 0.005;
        ell(c, tb.add(sy, 0.15, 0), new V3(0.058, 0.06, 0.058), 10, 7, flat(SW_GR));
        ell(c, tb.add(sy * 2, 0.19, 0.02), new V3(0.04, 0.04, 0.04), 8, 6, flat(SW_GR2));
        ell(c, tb.add(0.03, 0.15, 0.04), new V3(0.012, 0.012, 0.012), 6, 4, flat(SW_PK));
        // casita
        V3 hb = top.add(0.06, 0.03, 0.05);
        ell(c, hb.add(0, 0.03, 0), new V3(0.05, 0.04, 0.05), 8, 6, flat(SW_HUT));
        tube(c, new V3[]{hb.add(0, 0.055, 0), hb.add(0, 0.085, 0), hb.add(0, 0.115, 0)}, new double[]{0.06, 0.04, 0.004}, 4, flat(SW_ROOF));
        ell(c, hb.add(0, 0.025, 0.048), new V3(0.012, 0.018, 0.006), 6, 4, flat(hex(0x7A4A2A)));
        ell(c, hb.add(0.02, 0.1, -0.02), new V3(0.01, 0.025, 0.01), 4, 4, flat(hex(0x9A8A7A)));
        double sm = ((t * 0.01 + ph) % 1.0);
        Detail.twinkle(c, hb.add(0.02 + sm * 0.03, 0.14 + sm * 0.12, -0.02), 0.03 * (1 - sm * 0.4), new float[]{1F, 1F, 1F}, t, ph, 0.2);
        // cabeza (parte delantera del cuerpo con cara)
        headLook(c, 0.06, 0.0);
        c.hpv = bc.add(0, 0, 0.2);
        V3 fc = bc.add(0, -0.03, 0.2);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = bc.add(sd * 0.115, 0.0, 0.25);
            decal(c, bc, br, new V3(sd * 0.5, 0.05, 0.85), 0.12, sd * 0.1, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? WHITE : WHITE, 1, 0.004);
            Detail.glint(c, bc, br, new V3(sd * 0.5 - 0.15, 0.2, 0.86), 0.03, WHITE, 0.9);
        }
        cheeks(c, bc, br, 0.78, -0.15, 0.62, 0.085);
        decal(c, bc, br, new V3(0, -0.25, 1), 0.11, 0, T_MOUTH, new float[]{0.5F, 0.4F, 0.6F}, 0.95, 0.004);
        // ceja de nube
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bc.add(sd * 0.1, 0.1, 0.25), new V3(0.04, 0.012, 0.02), 0, 0, sd * 0.3, 8, 4, flat(SW_W));
        }
        c.head = false;
        // arcoíris suave que sale del chorrito
        double u = ((t * 0.012 + ph) % 1.0);
        float[][] rb = {hex(0xFF8A8A), hex(0xFFD88A), hex(0xA8FFA8), hex(0x8AD8FF), hex(0xD8A8FF)};
        for (int k = 0; k < 5; k++) {
            double uu = (u + k * 0.2) % 1.0;
            Detail.twinkle(c, new V3(Math.sin(k * 2.3) * 0.12, 0.7 + bob + uu * 0.3, 0.15 + uu * 0.1), 0.03 * (1 - uu * 0.4), rb[k], t, ph + k, 0.2);
        }
        for (int k = 0; k < 3; k++) {
            double uu = ((t * 0.01 + ph * 0.3 + k * 0.33) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.9 + uu * 3) * 0.3, 0.25 + bob + uu * 0.1, Math.cos(k * 1.7) * 0.3), 0.025, new float[]{1F, 1F, 1F}, t, ph + k, 0.2);
        }
    }
}
