package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Compañeros del lote 5 (todos con diseño propio, algunos con guiños a personajes queridos):
 * capibara, patito, babosa, corgi, creepercito, muñeco de nieve, fantasmita, golemcito, ratón relámpago,
 * droide astromecánico, espíritu del bosque, lobo de luna y dragón de la noche.
 */
final class Creatures5 {
    private Creatures5() {
    }

    // ------------------------------------------------------------------ piezas nuevas

    /** Caja con 6 caras (los cuerpos de los robots, creepers y golems). rx/ry/rz = medias dimensiones. */
    static void box(Ctx c, V3 ce, V3 r, double yaw, Paint paint) {
        double[] rm = yaw == 0 ? null : rot(yaw, 0, 0);
        double[][] sg = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (double[] s : sg) {
            V3 n = new V3(s[0], s[1], s[2]);
            V3 u, v;
            if (s[1] != 0) {
                u = new V3(1, 0, 0);
                v = new V3(0, 0, 1);
            } else if (s[0] != 0) {
                u = new V3(0, 0, 1);
                v = new V3(0, 1, 0);
            } else {
                u = new V3(1, 0, 0);
                v = new V3(0, 1, 0);
            }
            V3[] p = new V3[4];
            double[][] cs = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            for (int i = 0; i < 4; i++) {
                V3 q = new V3(n.x * r.x + u.x * cs[i][0] * r.x + v.x * cs[i][1] * r.x, n.y * r.y + u.y * cs[i][0] * r.y + v.y * cs[i][1] * r.y,
                        n.z * r.z + u.z * cs[i][0] * r.z + v.z * cs[i][1] * r.z);
                p[i] = q;
            }
            V3 nn = n;
            if (rm != null) {
                for (int i = 0; i < 4; i++) {
                    p[i] = Ctx.mul(rm, p[i]);
                }
                nn = Ctx.mul(rm, n);
            }
            V3[] pp = new V3[4];
            V3[] ns = {nn, nn, nn, nn};
            float[][] b = new float[4][];
            for (int i = 0; i < 4; i++) {
                pp[i] = ce.add(p[i]);
                b[i] = paint.at(nn, p[i]);
            }
            c.quad(c.blend, c.alpha, T_WHITE, pp, ns, b, null);
        }
    }

    static void box(Ctx c, V3 ce, V3 r, Paint paint) {
        box(c, ce, r, 0, paint);
    }

    /** Cilindro vertical de radio r entre dos alturas. */
    static void cyl(Ctx c, V3 base, double h, double r, int sides, Paint paint) {
        tube(c, new V3[]{base, base.add(0, h * 0.5, 0), base.add(0, h, 0)}, new double[]{r, r, r}, sides, paint);
        ell(c, base.add(0, h, 0), new V3(r, r * 0.12, r), sides, 4, paint);
        ell(c, base, new V3(r, r * 0.12, r), sides, 4, paint);
    }

    // ------------------------------------------------------------------ 23. CAPIBARA

    private static final float[] CP_B = {0.70F, 0.50F, 0.33F}, CP_B2 = {0.84F, 0.66F, 0.46F}, CP_DK = {0.30F, 0.20F, 0.16F},
            CP_OR = {1F, 0.62F, 0.16F}, CP_LF = {0.35F, 0.78F, 0.35F};

    static void capybara(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.8)) * 0.010 * w;
        c.pivot = new V3(0, 0.25, 0);
        legs4(c, 0.095, 0.15, -0.17, 0.17 + bob, 0.048, 0.03, 0.06, (n, p) -> mix(CP_B, CP_DK, ss(0.12, 0.0, p.y) * 0.8), flat(CP_DK));
        // cuerpo de barril
        V3 bc = new V3(0, 0.26 + bob, -0.03), br = new V3(0.175, 0.155, 0.31);
        Paint bp = (n, p) -> mix(mix(CP_B, CP_B2, ss(0.2, 1.0, n.y) * 0.5), CP_B2, ss(-0.2, -1.0, n.y) * 0.8);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 260, 161, 0.03, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, CP_B2);
        // patito amigo sobre el lomo: respira y mira a los lados
        {
            double db = 0.004 * Math.sin(t * 0.11 + ph + 1.5);
            V3 dc = new V3(0, 0.425 + bob + db, -0.10);
            float[] DY = {1F, 0.88F, 0.30F}, DO = {1F, 0.62F, 0.18F};
            ell(c, dc, new V3(0.042, 0.034, 0.05), 10, 7, flat(DY));
            V3 dh = new V3(0, 0.468 + bob + db, -0.075);
            ell(c, dh, new V3(0.03, 0.028, 0.03), 10, 7, flat(DY));
            ell(c, dh.add(0, -0.004, 0.03), new V3(0.016, 0.006, 0.014), 6, 4, flat(DO));
            for (int sd = -1; sd <= 1; sd += 2) {
                ell(c, dh.add(sd * 0.012, 0.006, 0.022), new V3(0.005, 0.005, 0.004), 5, 4, flat(BLACK));
                ell(c, dc.add(sd * 0.038, 0.004, -0.004), new V3(0.012, 0.02, 0.028), 6, 4, flat(scale(DY, 0.93)));
            }
            Detail.kite(c, dh.add(0, 0.024, 0.0), dh.add(0.004, 0.058, -0.012), new V3(1, 0, 0), 0.008, new V3(0, 0, 1), DY, DO);
        }
        // colita redonda
        ell(c, new V3(0, 0.27 + bob, -0.33), new V3(0.04, 0.035, 0.03), 8, 5, flat(CP_DK));
        Detail.fur(c, new V3(0, 0.27 + bob, -0.33), new V3(0.04, 0.035, 0.03), 14, 162, 0.025, 0.012, new V3(0, 0, -1), 0.8, Detail.ALL, flat(CP_DK), null);
        // cabeza larga y chata
        headLook(c, 0.07, 0.03);
        c.hpv = new V3(0, 0.32, 0.24);
        V3 hc = new V3(0, 0.355 + bob, 0.33), hr = new V3(0.125, 0.115, 0.15);
        Paint hp = (n, p) -> mix(CP_B, CP_B2, ss(-0.2, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 110, 163, 0.026, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, hp, CP_B2);
        V3 sc = new V3(0, 0.33 + bob, 0.455), sr = new V3(0.095, 0.085, 0.10);
        ell(c, sc, sr, 12, 8, (n, p) -> mix(CP_B, CP_B2, 0.45 + 0.2 * ss(0, 1, n.y)));
        ell(c, new V3(0, 0.355 + bob, 0.545), new V3(0.062, 0.042, 0.03), 10, 6, flat(CP_DK));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.022, 0.36 + bob, 0.572), new V3(0.009, 0.009, 0.006), 5, 4, flat(BLACK));
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.05, 0.345 + bob + k * 0.012, 0.55), new V3(sd * 0.15, 0.35 + bob + k * 0.03, 0.50), -0.008, 0.0028, new float[]{0.35F, 0.25F, 0.2F});
            }
            Detail.brow(c, hc, hr, sd, 0.58, 0.40, 0.70, 0.06, sd * 0.3, CP_DK);
            // orejitas redondas
            ell(c, new V3(sd * 0.085, 0.465 + bob, 0.28), new V3(0.032, 0.036, 0.022), 0, 0, sd * 0.5 * Detail.flick(c, sd > 0 ? 13 : 14), 8, 6, flat(CP_B));
        }
        // una naranja en la cabeza (clásico)
        V3 oc = new V3(0, 0.485 + bob, 0.34);
        ell(c, oc, new V3(0.062, 0.052, 0.062), 12, 8, (n, p) -> mix(CP_OR, new float[]{1F, 0.85F, 0.4F}, ss(0.2, 1.0, n.y) * 0.5));
        ell(c, oc.add(0.012, 0.05, 0.0), new V3(0.026, 0.006, 0.014), 0, 0, 0.5 + 0.12 * Math.sin(t * 0.07 + ph), 6, 4, flat(CP_LF));
        Detail.glint(c, oc, new V3(0.062, 0.052, 0.062), new V3(-0.4, 0.8, 0.6), 0.03, WHITE, 0.8);
        Detail.twinkle(c, oc.add(0.07, 0.03, 0.03), 0.014, new float[]{1F, 0.95F, 0.6F}, t, ph, 0.12);
        eyes(c, hc, hr, 0.62, 0.22, 0.74, 0.085, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.12, 0.38, 0.07);
        c.head = false;
    }

    // ------------------------------------------------------------------ 24. PATITO

    private static final float[] DU_OR2 = {0.88F, 0.42F, 0.08F}, DU_BOW = {0.25F, 0.55F, 0.95F}, DU_BOW2 = {0.12F, 0.30F, 0.70F};
    private static final float[] DU_Y = {1F, 0.88F, 0.28F}, DU_Y2 = {1F, 0.96F, 0.55F}, DU_OR = {1F, 0.56F, 0.12F};

    static void duck(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double wad = Math.sin(c.gait * 1.1) * 0.09 * w;
        double hop = Math.abs(Math.sin(c.gait * 1.1)) * 0.02 * w;
        c.pivot = new V3(0, 0.2, 0);
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.1 + (sd > 0 ? 0 : Math.PI);
            double sw = Math.sin(p) * 0.04 * w;
            V3 a = new V3(sd * 0.06, 0.1 + hop, 0.0), b = new V3(sd * 0.06, 0.012, sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.012, 0.011, 0.010}, 5, flat(DU_OR));
            // pie palmeado: membrana triangular con tres dedos
            Detail.kite(c, b.add(0, 0.004, 0.0), b.add(0, 0.004, 0.115), new V3(1, 0, 0), 0.055, new V3(0, 1, 0), DU_OR, DU_OR2);
            for (int k = -1; k <= 1; k++) {
                tube(c, new V3[]{b.add(0, 0.006, 0.0), b.add(k * 0.045, 0.006, 0.105)}, new double[]{0.008, 0.005}, 4, flat(DU_OR2));
            }
        }
        c.roll = wad * 0.6;
        V3 bc = new V3(0, 0.21 + hop, -0.02), br = new V3(0.18, 0.15, 0.21);
        Paint bp = (n, p) -> mix(DU_Y, DU_Y2, ss(0, 1, 0.5 - n.y * 0.5));
        ell(c, bc, br, 16, 11, bp);
        Detail.plumage(c, bc, br, 8, 12, 0.06, 0.02, d -> d.y < 0.55 ? 1 : 0, bp, DU_Y2);
        Detail.fur(c, bc, br, 60, 91, 0.028, 0.012, new V3(0, -0.6, -1), 0.8, Detail.TOP, bp, DU_Y2);
        // alitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = 0.25 + Math.sin(t * 0.25 + ph) * 0.04 + Math.abs(Math.sin(c.gait * 1.1)) * 0.4 * w;
            V3 wc = new V3(sd * 0.185, 0.22 + hop, -0.02);
            ell(c, wc, new V3(0.03, 0.085, 0.12), 0, 0, -sd * fl, 8, 6, flat(DU_Y));
            for (int k = -2; k <= 2; k++) {
                V3 base = wc.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.01, -0.05, k * 0.045 - 0.02)));
                V3 tip = wc.add(Detail.rotv(0, 0, -sd * fl, new V3(sd * 0.03, -0.14 + Math.abs(k) * 0.015, k * 0.055 - 0.04)));
                Detail.feather(c, base, tip, new V3(0, 0, 1), 0.02, new V3(sd, 0, 0), DU_Y, DU_Y2);
            }
        }
        // colita levantada
        for (int k = -2; k <= 2; k++) {
            V3 a0 = new V3(0, 0.25 + hop, -0.19), b0 = new V3(k * 0.035, 0.34 + hop - Math.abs(k) * 0.012, -0.29);
            Detail.feather(c, a0, b0, new V3(1, 0, 0), 0.022, new V3(0, 0.4, 1).norm(), DU_Y, DU_Y2);
        }
        headLook(c, 0.16, 0);
        c.hpv = new V3(0, 0.32, 0.1);
        V3 hc = new V3(0, 0.40 + hop, 0.09), hr = new V3(0.135, 0.125, 0.13);
        Paint hp = (n, p) -> mix(DU_Y, DU_Y2, ss(-0.3, 1.0, n.z) * 0.5);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 92, 0.026, 0.011, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.55 ? 1 : 0, hp, DU_Y2);
        // dos plumitas rebeldes en la coronilla
        for (int k = -1; k <= 1; k += 2) {
            V3 b1 = hc.add(k * 0.01, 0.12, -0.01);
            Detail.feather(c, b1, b1.add(k * 0.03 + Math.sin(t * 0.1 + ph) * 0.01, 0.075, -0.015), new V3(0, 0, 1), 0.013, new V3(1, 0, 0), DU_Y, DU_Y2);
        }
        // pico plano
        ell(c, new V3(0, 0.375 + hop, 0.215), new V3(0.062, 0.018, 0.058), 10, 5, flat(DU_OR));
        ell(c, new V3(0, 0.392 + hop, 0.205), new V3(0.055, 0.014, 0.05), 10, 5, flat(mix(DU_OR, WHITE, 0.15)));
        // fosas nasales y línea de la boca en el pico
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.022, 0.398 + hop, 0.235), new V3(0.006, 0.004, 0.008), 4, 3, flat(DU_OR2));
        }
        tube(c, new V3[]{new V3(-0.05, 0.372 + hop, 0.235), new V3(0, 0.368 + hop, 0.262), new V3(0.05, 0.372 + hop, 0.235)}, new double[]{0.003, 0.003, 0.003}, 4, flat(DU_OR2));
        eyes(c, hc, hr, 0.62, 0.20, 0.74, 0.105, T_BEAD, new float[]{1, 1, 1});
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.58, 0.46, 0.66, 0.05, sd * 0.7, DU_OR2);
        }
        // pajarita azul
        V3 bt = new V3(0, 0.272 + hop, 0.178);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bt.add(sd * 0.032, 0, 0.012), new V3(0.034, 0.024, 0.012), 0, 0, sd * 0.35, 8, 5, (n, p) -> mix(DU_BOW, DU_BOW2, ss(-0.4, 0.8, n.y)));
        }
        ell(c, bt.add(0, 0, 0.018), new V3(0.014, 0.014, 0.012), 6, 4, flat(DU_BOW2));
        cheeks(c, hc, hr, 0.95, -0.22, 0.38, 0.08);
        c.head = false;
        c.roll = 0;
    }

    // ------------------------------------------------------------------ 25. BABOSA (slime)

    private static final float[] SL_G = {0.45F, 0.90F, 0.42F}, SL_G2 = {0.75F, 1F, 0.60F}, SL_DK = {0.18F, 0.55F, 0.22F};

    static void slime(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double jump = Math.abs(Math.sin(c.gait * 0.7));
        double sq = 1 - 0.18 * (1 - jump) * w - 0.05 * Math.sin(t * 0.12 + ph);
        double wid = 1 / Math.sqrt(sq);
        double lift = jump * 0.10 * w;
        c.pivot = new V3(0, 0.18, 0);
        // charquito de baba bajo el cuerpo
        c.blend = Sink.NORMAL;
        c.alpha = 0.40;
        ell(c, new V3(0, 0.006, 0), new V3(0.27 * wid, 0.006, 0.27 * wid), 18, 3, flat(SL_G));
        ell(c, new V3(0.17, 0.007, -0.14), new V3(0.06, 0.005, 0.045), 10, 3, flat(SL_G));
        c.alpha = 1;
        c.blend = Sink.SOLID;
        // adentro: núcleo oscuro, estrellita que titila y burbujas que suben
        ell(c, new V3(0.03, 0.115 + lift, -0.035), new V3(0.075 * wid, 0.065 * sq, 0.075 * wid), 10, 7, flat(SL_DK));
        ell(c, new V3(0.03, 0.115 + lift, -0.035), new V3(0.045 * wid, 0.04 * sq, 0.045 * wid), 8, 6, flat(mix(SL_DK, SL_G, 0.35)));
        Detail.twinkle(c, new V3(0.03, 0.12 + lift, -0.035), 0.07, new float[]{0.8F, 1F, 0.7F}, t, ph, 0.18);
        for (int k = 0; k < 6; k++) {
            double up = ((t * 0.006 + k * 0.17 + ph * 0.1) % 1.0);
            double ang = k * 2.4 + ph;
            V3 bp = new V3(Math.cos(ang) * 0.12 * wid, 0.07 + up * 0.22 * sq + lift, Math.sin(ang) * 0.12 * wid);
            double bs = (0.014 + 0.006 * (k % 3)) * (1 - 0.5 * up);
            ell(c, bp, new V3(bs, bs, bs), 6, 4, flat(SL_BUB));
        }
        // cuerpo gelatinoso translúcido
        c.blend = Sink.NORMAL;
        c.alpha = 0.78;
        ell(c, new V3(0, 0.18 + lift, 0), new V3(0.23 * wid, 0.19 * sq, 0.23 * wid), 18, 12, (n, p) -> mix(SL_G, SL_G2, ss(-0.3, 1.0, n.y) * 0.8));
        c.alpha = 1;
        c.blend = Sink.SOLID;
        // reflejos: franja curva y puntito
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            double a = -0.15 + k * 0.28;
            ell(c, new V3(-0.115 * wid + Math.sin(a) * 0.03, (0.30 - k * 0.028) * sq + lift, (0.105 + k * 0.012) * wid), new V3(0.020 - k * 0.003, 0.012, 0.010), 0, 0, 0.8 + k * 0.1, 6, 4, flat(WHITE));
        }
        ell(c, new V3(0.13 * wid, 0.27 * sq + lift, 0.10 * wid), new V3(0.012, 0.012, 0.008), 6, 4, flat(WHITE));
        c.emissive = false;
        V3 fc = new V3(0, 0.18 + lift, 0), fr = new V3(0.23 * wid, 0.19 * sq, 0.23 * wid);
        eyes(c, fc, fr, 0.42, 0.20, 0.88, 0.125, T_BEAD, new float[]{0.1F, 0.25F, 0.1F});
        decal(c, fc, fr, new V3(0, -0.22, 1), 0.075, 0, T_MOUTH, new float[]{0.15F, 0.35F, 0.15F}, 0.9, 0.003);
        cheeks(c, fc, fr, 0.80, -0.12, 0.60, 0.10);
        // brotecito de hojas en la coronilla (se mece)
        double sw2 = Math.sin(t * 0.1 + ph) * 0.012;
        V3 tb = new V3(0.0, 0.365 * sq + lift, -0.01), tt = tb.add(sw2, 0.07, 0.0);
        tube(c, new V3[]{tb, tb.lerp(tt, 0.5), tt}, new double[]{0.006, 0.005, 0.004}, 4, flat(SL_LF));
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.kite(c, tt, tt.add(sd * 0.055 + sw2, 0.012, 0.01), new V3(0, 0, 1), 0.020, new V3(0, 1, 0), SL_LF, SL_LF2);
            Detail.kite(c, tt, tt.add(sd * 0.055 + sw2, 0.012, 0.01), new V3(0, 1, 0), 0.016, new V3(0, 0, 1), SL_LF, SL_LF2);
        }
        // gotita que cae por el costado
        double dr = (t * 0.012 + ph) % 1.0;
        c.blend = Sink.NORMAL;
        c.alpha = 0.7;
        ell(c, new V3(0.2 * wid, 0.2 * sq - dr * dr * 0.2 + lift * (1 - dr), 0.06), new V3(0.012, 0.018 * (1 - 0.3 * dr), 0.012), 6, 5, flat(SL_G2));
        c.alpha = 1;
        c.blend = Sink.SOLID;
    }

    private static final float[] SL_BUB = {0.85F, 1F, 0.8F}, SL_LF = {0.25F, 0.65F, 0.25F}, SL_LF2 = {0.55F, 0.95F, 0.4F};

    // ------------------------------------------------------------------ 26. CORGI

    private static final float[] CG_O = {0.96F, 0.62F, 0.26F}, CG_W = {1F, 0.96F, 0.88F}, CG_DK = {0.55F, 0.32F, 0.16F};

    static void corgi(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.018 * w;
        c.pivot = new V3(0, 0.22, 0);
        legs4(c, 0.075, 0.13, -0.16, 0.14 + bob, 0.04, 0.03, 0.055, (n, p) -> mix(CG_W, CG_O, ss(0.08, 0.14, p.y)), flat(CG_W));
        // cuerpo largo
        V3 bc = new V3(0, 0.215 + bob, -0.02), br = new V3(0.125, 0.115, 0.255);
        Paint bp = (n, p) -> mix(mix(CG_O, CG_W, ss(-0.1, -0.8, n.y)), CG_O, 0.0);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 220, 171, 0.028, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, CG_W);
        // lomo con silla blanca (marca típica)
        ell(c, new V3(0, 0.27 + bob, 0.02), new V3(0.075, 0.04, 0.12), 8, 5, flat(mix(CG_O, CG_DK, 0.15)));
        // trasero esponjoso y colita corta
        ell(c, new V3(0, 0.215 + bob, -0.215), new V3(0.135, 0.12, 0.10), 12, 8, flat(CG_O));
        Detail.fur(c, new V3(0, 0.215 + bob, -0.215), new V3(0.135, 0.12, 0.10), 60, 172, 0.03, 0.014, new V3(0, 0.2, -1), 0.8, Detail.ALL, flat(CG_O), CG_W);
        // colita de corgi: meneo rápido, y muy feliz cuando salta
        double wag = Math.sin(t * (0.7 + 0.9 * c.joy) + ph) * (0.03 + 0.03 * c.joy) * (0.5 + w);
        V3 tc = new V3(wag, 0.25 + bob, -0.31);
        ell(c, tc, new V3(0.04, 0.04, 0.04), 8, 5, flat(CG_W));
        Detail.fur(c, tc, new V3(0.04, 0.04, 0.04), 24, 173, 0.03, 0.014, new V3(wag * 8, 0.3, -1), 0.8, Detail.ALL, flat(CG_W), null);
        // bandana roja con una estrellita
        {
            float[] BR = {0.88F, 0.18F, 0.22F}, BR2 = {1F, 0.45F, 0.42F};
            ell(c, new V3(0, 0.272 + bob, 0.175), new V3(0.092, 0.03, 0.06), 12, 6, flat(BR));
            ell(c, new V3(0, 0.235 + bob, 0.205), new V3(0.05, 0.05, 0.016), 8, 4, flat(BR));
            Detail.kite(c, new V3(-0.045, 0.265 + bob, 0.215), new V3(0, 0.19 + bob, 0.235), new V3(0, 0, 1), 0.052, new V3(0, 0.1, 1), BR, BR2);
            Detail.glint(c, new V3(0, 0.222 + bob, 0.226), new V3(0.03, 0.03, 0.01), new V3(0, 0, 1), 0.026, new float[]{1F, 0.92F, 0.5F}, 0.95);
        }
        // pecho blanco
        ell(c, new V3(0, 0.20 + bob, 0.15), new V3(0.095, 0.10, 0.09), 10, 7, flat(CG_W));
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.30, 0.2);
        V3 hc = new V3(0, 0.345 + bob, 0.255), hr = new V3(0.14, 0.12, 0.125);
        Paint hp = (n, p) -> mix(CG_O, CG_W, ss(-0.2, 0.8, n.z) * 0.15);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 90, 174, 0.028, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, hp, CG_W);
        // franja blanca en la cara
        ell(c, new V3(0, 0.34 + bob, 0.325), new V3(0.05, 0.10, 0.06), 10, 7, flat(CG_W));
        V3 sc = new V3(0, 0.318 + bob, 0.345), sr = new V3(0.062, 0.05, 0.075);
        ell(c, sc, sr, 10, 7, flat(CG_W));
        ell(c, new V3(0, 0.342 + bob, 0.418), new V3(0.022, 0.017, 0.015), 8, 5, flat(BLACK));
        // lengüita
        double pant = 0.5 + 0.5 * Math.sin(t * 0.45 + ph);
        ell(c, new V3(0, 0.285 + bob - 0.006 * pant, 0.40), new V3(0.016, 0.006 + 0.003 * pant, 0.026 + 0.008 * pant), 6, 4, flat(PINK));
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.04, 0.325 + bob + k * 0.01, 0.40), new V3(sd * 0.13, 0.33 + bob + k * 0.03, 0.37), -0.008, 0.0028, new float[]{1F, 0.96F, 0.9F});
            }
            // cejitas de corgi: dos puntos claros sobre los ojos
            Detail.brow(c, hc, hr, sd, 0.50, 0.46, 0.74, 0.05, sd * 0.4, CG_W);
        }
        // orejas grandes
        for (int sd = -1; sd <= 1; sd += 2) {
            double fk = Detail.flick(c, sd > 0 ? 15 : 16) * 0.03;
            V3 b0 = new V3(sd * 0.075, 0.43 + bob, 0.21), b1 = new V3(sd * (0.115 + fk), 0.50 + bob, 0.20), b2 = new V3(sd * (0.135 + fk * 1.8), 0.56 + bob - fk * 0.5, 0.19);
            curve(c, new V3[]{b0, b1, b2}, new double[]{0.055, 0.045, 0.008}, 3, 7, flat(CG_O));
            curve(c, new V3[]{b0.add(0, 0.0, 0.02), b1.add(0, 0.0, 0.02), b2.add(-sd * 0.004, -0.02, 0.02)}, new double[]{0.034, 0.026, 0.004}, 3, 6, flat(PINK));
        }
        eyes(c, hc, hr, 0.50, 0.20, 0.82, 0.085, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.95, -0.22, 0.38, 0.07);
        c.head = false;
    }

    // ------------------------------------------------------------------ 27. CREEPERCITO

    private static final float[] CR_G = {0.46F, 0.80F, 0.38F}, CR_G2 = {0.62F, 0.90F, 0.50F}, CR_DK = {0.10F, 0.18F, 0.10F};

    static void creeper(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.010 * w;
        double hiss = Math.max(0, Math.sin(t * 0.05 + ph * 3) - 0.85) / 0.15; // de vez en cuando se hincha
        double sw = 1 + 0.10 * hiss;
        double flash = hiss > 0.05 ? hiss * (Math.sin(t * 1.3) > 0 ? 0.55 : 0.12) : 0;
        c.pivot = new V3(0, 0.25, 0);
        // piel moteada de píxeles: cuatro tonos al azar, más oscura abajo, más clara arriba
        final float[] pk = {0.98F, 0.62F, 0.62F};
        Detail.Pix skin = (f, i, j, ni, nj) -> {
            double r0 = Detail.rnd(41 + f, i * 31 + j, 7);
            float[] base = r0 < 0.22 ? CR_D2 : (r0 < 0.62 ? CR_G : (r0 < 0.88 ? CR_G2 : CR_L));
            double h = nj <= 1 ? 0.5 : j / (double) (nj - 1);
            float[] col = scale(base, f == 2 ? 1.08 : (f == 3 ? 0.62 : 0.82 + 0.22 * h));
            return flash > 0 ? mix(col, WHITE, flash) : col;
        };
        // cuatro patitas de píxeles con la planta más oscura
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double phs = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double z0 = fb == 0 ? 0.08 : -0.08;
                double sz = Math.sin(phs) * 0.045 * w;
                double lf = Math.max(0, Math.cos(phs)) * 0.03 * w;
                Detail.pixBox(c, new V3(sd * 0.065, 0.055 + lf, z0 + sz), new V3(0.050, 0.055, 0.050), 3, 4, 3, (f, i, j, ni, nj) -> {
                    float[] col = skin.at(f, i, j, ni, nj);
                    return (f < 2 || f > 3) && j == 0 ? scale(col, 0.55) : col;
                });
            }
        }
        // cuerpo: 7x9x5 píxeles, con una pancita más clara y unas grietitas
        Detail.pixBox(c, new V3(0, 0.255 + bob, 0), new V3(0.115 * sw, 0.13 * sw, 0.085 * sw), 7, 9, 5, (f, i, j, ni, nj) -> {
            float[] col = skin.at(f, i, j, ni, nj);
            if (f == 4 && j >= 1 && j <= 4 && i >= 2 && i <= 4) {
                col = mix(col, CR_L, 0.55);
            }
            if (f == 5 && ((i == 2 && j >= 4 && j <= 6) || (i == 3 && j == 4))) {
                col = scale(col, 0.45);
            }
            return col;
        });
        headLook(c, 0.10, 0.02);
        c.hpv = new V3(0, 0.38, 0.0);
        V3 hc = new V3(0, 0.50 + bob, 0.0);
        double blinkRow = c.shut ? 1 : 0;
        // cabeza 8x8x8 con la carita clásica: ojos con brillito, nariz, boca y mejillitas
        Detail.pixBox(c, hc, new V3(0.125 * sw, 0.12 * sw, 0.125 * sw), 8, 8, 8, (f, i, j, ni, nj) -> {
            float[] col = skin.at(f, i, j, ni, nj);
            if (f != 4) {
                return col;
            }
            int rt = 7 - j;
            boolean eye = (rt == 2 || rt == 3) && (i == 1 || i == 2 || i == 5 || i == 6);
            if (eye) {
                if (blinkRow > 0) {
                    return rt == 3 ? CR_DK : col;
                }
                return (rt == 2 && (i == 1 || i == 5)) ? new float[]{0.80F, 0.98F, 0.78F} : CR_DK;
            }
            boolean nose = rt == 4 && (i == 3 || i == 4);
            boolean mouth = ((rt == 5 || rt == 6) && i >= 2 && i <= 5) || (rt == 7 && (i == 2 || i == 5));
            if (nose || mouth) {
                return CR_DK;
            }
            if (rt == 4 && (i == 0 || i == 7)) {
                return mix(col, pk, 0.85);
            }
            return col;
        });
        // brotecito de pasto y una florecita azul en lo alto de la cabeza
        double sway = Math.sin(t * 0.09 + ph) * 0.012;
        for (int k = 0; k < 5; k++) {
            double a = -0.9 + k * 0.45;
            V3 base = hc.add(-0.045 + Math.cos(a) * 0.012, 0.118 * sw, -0.02 + Math.sin(a) * 0.03);
            V3 tip = base.add(Math.sin(a) * 0.05 + sway, 0.065 + 0.02 * (k % 2), 0.0);
            Detail.kite(c, base, tip, new V3(0, 0, 1), 0.012, new V3(1, 0, 0), CR_G, CR_L);
            Detail.kite(c, base, tip, new V3(1, 0, 0), 0.012, new V3(0, 0, 1), CR_G, CR_L);
        }
        V3 fl = hc.add(0.05, 0.145 * sw, 0.03);
        tube(c, new V3[]{hc.add(0.05, 0.118 * sw, 0.03), fl}, new double[]{0.004, 0.004}, 4, flat(CR_G));
        for (int k = 0; k < 5; k++) {
            double a = k * Math.PI * 2 / 5 + 0.3;
            ell(c, fl.add(Math.cos(a) * 0.017, 0.004, Math.sin(a) * 0.017), new V3(0.011, 0.005, 0.011), 6, 4, flat(CR_FL));
        }
        ell(c, fl.add(0, 0.008, 0), new V3(0.009, 0.006, 0.009), 6, 4, flat(new float[]{1F, 0.88F, 0.3F}));
        c.head = false;
        if (hiss > 0.05) {
            c.emissive = true;
            glow(c, new V3(0, 0.38, 0), 0.7, new float[]{1F, 0.95F, 0.6F}, 0.30 * hiss);
            c.emissive = false;
        }
    }

    private static final float[] CR_D2 = {0.30F, 0.58F, 0.28F}, CR_L = {0.74F, 0.96F, 0.62F}, CR_FL = {0.45F, 0.65F, 1F};

    // ------------------------------------------------------------------ 28. MUÑECO DE NIEVE

    private static final float[] SN_W = {0.98F, 0.99F, 1F}, SN_B = {0.78F, 0.88F, 0.98F}, SN_RD = {0.90F, 0.18F, 0.20F}, SN_BR = {0.45F, 0.30F, 0.18F};

    static void snowman(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.03 * w;
        c.pivot = new V3(0, 0.25, 0);
        Paint snow = (n, p) -> mix(SN_B, SN_W, ss(-0.6, 0.9, n.y));
        V3 b1c = new V3(0, 0.17 + bob, 0), b1r = new V3(0.19, 0.17, 0.19), b2c = new V3(0, 0.40 + bob, 0), b2r = new V3(0.145, 0.135, 0.145);
        ell(c, b1c, b1r, 16, 11, snow);
        ell(c, b2c, b2r, 14, 10, snow);
        // nieve esponjosa y cristalitos que brillan
        Detail.fur(c, b1c, b1r, 110, 371, 0.026, 0.02, new V3(0, 0.2, 0), 0.0, Detail.ALL, snow, SN_W);
        Detail.fur(c, b2c, b2r, 70, 372, 0.024, 0.018, new V3(0, 0.2, 0), 0.0, Detail.ALL, snow, SN_W);
        for (int k = 0; k < 6; k++) {
            Detail.twinkle(c, surf(b1c, b1r, Detail.sph(373, k).mul(1.0)).add(0, 0.01, 0), 0.016, new float[]{0.85F, 0.95F, 1F}, t, ph + k * 1.9, 0.08 + 0.02 * k);
        }
        // copitos de nieve cayendo alrededor
        for (int k = 0; k < 8; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k / 8.0) % 1.0);
            double a = k * 2.4 + u * 3;
            V3 fp = new V3(Math.cos(a) * (0.30 + 0.06 * Math.sin(k)), 0.72 - u * 0.72, Math.sin(a) * (0.30 + 0.06 * Math.cos(k)));
            c.emissive = true;
            ell(c, fp, new V3(0.009, 0.009, 0.009), 5, 4, flat(new float[]{0.95F, 0.98F, 1F}));
            c.emissive = false;
        }
        // botones de carbón
        for (int k = 0; k < 3; k++) {
            ell(c, new V3(0, 0.45 - k * 0.075 + bob, 0.138 - k * 0.008 + (k == 0 ? 0.0 : 0.01)), new V3(0.016, 0.016, 0.008), 6, 4, flat(BLACK));
        }
        // brazos de ramita
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw2 = Math.sin(t * 0.12 + ph + sd) * 0.02 + Math.sin(c.gait * 0.9 + sd) * 0.04 * w;
            V3 a = new V3(sd * 0.13, 0.42 + bob, 0.0), m = new V3(sd * 0.23, 0.46 + bob + sw2, 0.0), b = new V3(sd * 0.31, 0.53 + bob + sw2, 0.01);
            tube(c, new V3[]{a, m, b}, new double[]{0.012, 0.010, 0.006}, 5, flat(SN_BR));
            tube(c, new V3[]{m, m.add(sd * 0.04, 0.05, 0.0)}, new double[]{0.007, 0.004}, 4, flat(SN_BR));
            tube(c, new V3[]{b, b.add(sd * 0.03, 0.035, 0.0)}, new double[]{0.005, 0.003}, 4, flat(SN_BR));
            tube(c, new V3[]{b, b.add(sd * 0.03, -0.01, 0.025)}, new double[]{0.005, 0.003}, 4, flat(SN_BR));
            // manopla roja
            ell(c, b.add(sd * 0.012, 0.0, 0.0), new V3(0.026, 0.024, 0.024), 8, 5, flat(SN_RD));
            Detail.fur(c, b.add(sd * 0.012, 0.0, 0.0), new V3(0.026, 0.024, 0.024), 8, 374 + sd, 0.014, 0.01, new V3(sd, 0, 0), 0.6, Detail.ALL, flat(SN_RD), new float[]{1F, 0.5F, 0.5F});
        }
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.58, 0.0);
        V3 hc = new V3(0, 0.62 + bob, 0.0), hr = new V3(0.125, 0.115, 0.125);
        ell(c, hc, hr, 16, 11, snow);
        // nariz de zanahoria
        cone(c, new V3(0, 0.615 + bob, 0.115), new V3(0, 0.605 + bob, 0.235), 0.022, 0.003, 6, flat(new float[]{1F, 0.55F, 0.12F}));
        eyes(c, hc, hr, 0.50, 0.26, 0.80, 0.09, T_BEAD, new float[]{1, 1, 1});
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.10, 0, T_MOUTH, new float[]{0.2F, 0.2F, 0.3F}, 0.9, 0.003);
        cheeks(c, hc, hr, 0.92, -0.10, 0.40, 0.08);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.50, 0.46, 0.76, 0.06, sd * 0.5, SN_BR);
            // sonrisa de piedritas
            for (int k = 0; k < 3; k++) {
                ell(c, surf(hc, hr, new V3(sd * (0.18 + 0.18 * k), -0.26 - 0.04 * (k == 2 ? 1 : 0), 0.92)), new V3(0.008, 0.008, 0.005), 5, 4, flat(BLACK));
            }
        }
        Detail.glint(c, new V3(0, 0.615 + bob, 0.20), new V3(0.022, 0.022, 0.1), new V3(0, 1, 0.3), 0.012, WHITE, 0.9);
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.2), 0.06, WHITE, 0.6);
        // sombrerito
        box(c, new V3(0, 0.735 + bob, 0), new V3(0.080, 0.05, 0.080), flat(new float[]{0.18F, 0.16F, 0.24F}));
        ell(c, new V3(0, 0.695 + bob, 0), new V3(0.135, 0.012, 0.135), 14, 4, flat(new float[]{0.18F, 0.16F, 0.24F}));
        tube(c, new V3[]{new V3(0.0, 0.715 + bob, 0.0), new V3(0.0, 0.72 + bob, 0.0)}, new double[]{0.082, 0.082}, 12, flat(SN_RD));
        c.head = false;
        // muérdago en el sombrero
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.leaf(c, new V3(sd * 0.082, 0.717 + bob, 0.02), new V3(0.01, 0.01, 0.01), new V3(sd, 0.2, 1), 0.04, sd * 0.6, new float[]{0.3F, 0.7F, 0.3F});
        }
        ell(c, new V3(0.0, 0.726 + bob, 0.085), new V3(0.011, 0.011, 0.011), 5, 4, flat(SN_RD));
        // bufanda
        V3[] ring = new V3[13];
        for (int i = 0; i < 13; i++) {
            double a = Math.PI * 2 * i / 12.0;
            ring[i] = new V3(Math.cos(a) * 0.115, 0.505 + bob, Math.sin(a) * 0.115);
        }
        double[] rr = new double[13];
        java.util.Arrays.fill(rr, 0.026);
        tube(c, ring, rr, 7, flat(SN_RD));
        double fl = Math.sin(t * 0.14 + ph) * 0.02;
        curve(c, new V3[]{new V3(0.07, 0.50 + bob, 0.10), new V3(0.11 + fl, 0.42 + bob, 0.12), new V3(0.12 + fl * 1.5, 0.34 + bob, 0.11)}, new double[]{0.028, 0.026, 0.02}, 3, 7, flat(SN_RD));
    }

    // ------------------------------------------------------------------ 29. FANTASMITA

    private static final float[] GH_W = {0.93F, 0.96F, 1F}, GH_B = {0.70F, 0.80F, 1F};

    static void ghost(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.14 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        c.emissive = false;
        glow(c, new V3(0, 0.42 + bob, 0), 1.1, new float[]{0.6F, 0.75F, 1F}, 0.12);
        c.blend = Sink.NORMAL;
        c.alpha = 0.88;
        Paint body = (n, p) -> mix(GH_B, GH_W, ss(-0.9, 0.6, n.y));
        ell(c, new V3(0, 0.46 + bob, 0), new V3(0.17, 0.20, 0.165), 16, 12, body);
        // falda ondulante
        ell(c, new V3(0, 0.32 + bob, 0), new V3(0.19, 0.10, 0.185), 16, 8, body);
        for (int k = 0; k < 7; k++) {
            double a = k * Math.PI * 2 / 7 + t * 0.02;
            double wv = Math.sin(t * 0.18 + k * 1.3 + ph) * 0.02;
            ell(c, new V3(Math.cos(a) * 0.15, 0.245 + bob + wv, Math.sin(a) * 0.15), new V3(0.05, 0.065, 0.05), 8, 6, body);
        }
        // brazitos
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.2 + ph + sd * 1.3) * 0.04;
            ell(c, new V3(sd * 0.185, 0.38 + bob + sw, 0.06), new V3(0.04, 0.06, 0.04), 0, 0, -sd * 0.7, 8, 6, body);
        }
        // colita de niebla que se arrastra y se desvanece
        for (int k = 0; k < 4; k++) {
            double f = (k + 1) / 4.0;
            c.alpha = 0.55 * (1 - f * 0.7);
            ell(c, new V3(Math.sin(t * 0.12 + ph - f * 3) * 0.05 * f, 0.22 + bob - f * 0.10, -0.12 - f * 0.18), new V3(0.09 * (1 - f * 0.5), 0.05 * (1 - f * 0.4), 0.09 * (1 - f * 0.5)), 8, 5, body);
        }
        c.alpha = 0.88;
        c.alpha = 1;
        c.blend = Sink.SOLID;
        V3 fc = new V3(0, 0.47 + bob, 0), fr = new V3(0.17, 0.20, 0.165);
        // pliegues de sábana y brillos
        for (int k = 0; k < 4; k++) {
            Detail.soft(c, new V3(0, 0.40 + bob, 0), new V3(0.19, 0.16, 0.19), new V3(Math.cos(k * 1.7 + 2.0), -0.35, Math.sin(k * 1.7 + 2.0)), 0.07, GH_B, 0.35);
        }
        Detail.glint(c, fc, fr, new V3(-0.5, 1, 0.3), 0.06, WHITE, 0.6);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, fc, fr, sd, 0.40, 0.46, 0.86, 0.05, sd * 0.45, new float[]{0.45F, 0.5F, 0.8F});
            Detail.glint(c, fc, fr, new V3(sd * 0.40 - 0.06, 0.24, 0.90), 0.03, WHITE, 0.95);
        }
        // calabacita linterna que sostiene con la manita
        {
            double sw = Math.sin(t * 0.2 + ph + 1.3) * 0.04;
            V3 pc = new V3(0.235, 0.33 + bob + sw, 0.10);
            c.emissive = false;
            ell(c, pc, new V3(0.045, 0.036, 0.045), 12, 8, (n, p) -> mix(new float[]{0.95F, 0.45F, 0.08F}, new float[]{1F, 0.62F, 0.2F}, ss(-0.5, 0.9, n.y)));
            tube(c, new V3[]{pc.add(0, 0.034, 0), pc.add(0.006, 0.05, 0)}, new double[]{0.008, 0.007}, 5, flat(new float[]{0.3F, 0.55F, 0.25F}));
            c.emissive = true;
            glow(c, pc.add(0, 0, 0.03), 0.16, new float[]{1F, 0.65F, 0.2F}, 0.55 + 0.1 * Math.sin(t * 0.3 + ph));
            ell(c, pc.add(-0.014, 0.004, 0.04), new V3(0.007, 0.01, 0.004), 4, 3, flat(new float[]{1F, 0.9F, 0.4F}));
            ell(c, pc.add(0.014, 0.004, 0.04), new V3(0.007, 0.01, 0.004), 4, 3, flat(new float[]{1F, 0.9F, 0.4F}));
            ell(c, pc.add(0, -0.016, 0.042), new V3(0.016, 0.005, 0.004), 5, 3, flat(new float[]{1F, 0.9F, 0.4F}));
            c.emissive = false;
        }
        // motas de luz espectral
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.2 + u * 5) * 0.24, 0.25 + u * 0.55 + bob, Math.cos(k * 1.9 + u * 4) * 0.2), 0.014 * (1 - u * 0.5), new float[]{0.7F, 0.85F, 1F}, t, ph + k, 0.3);
        }
        // ojos grandes y oscuros
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, fc, fr, new V3(sd * 0.40, 0.18, 0.90), 0.12, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? new float[]{0.2F, 0.2F, 0.4F} : new float[]{0.08F, 0.08F, 0.2F}, 1, 0.004);
        }
        decal(c, fc, fr, new V3(0, -0.12, 1), 0.07, 0, T_MOUTH, new float[]{0.2F, 0.2F, 0.4F}, 0.9, 0.003);
        cheeks(c, fc, fr, 0.80, -0.02, 0.62, 0.09);
        // llamita espectral sobre la cabeza
        c.emissive = true;
        double k = 0.8 + 0.2 * Math.sin(t * 0.4 + ph);
        ell(c, new V3(0, 0.71 + bob, 0), new V3(0.022, 0.05 * k, 0.022), 6, 4, flat(new float[]{0.7F, 0.9F, 1F}));
        glow(c, new V3(0, 0.71 + bob, 0), 0.3, new float[]{0.5F, 0.75F, 1F}, 0.45);
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 30. GOLEMCITO

    private static final float[] GO_G = {0.74F, 0.77F, 0.82F}, GO_G2 = {0.92F, 0.94F, 0.97F}, GO_DK = {0.42F, 0.46F, 0.54F},
            GO_V = {0.30F, 0.60F, 0.28F}, GO_V2 = {0.52F, 0.80F, 0.38F}, GO_RED = {0.92F, 0.15F, 0.25F},
            GO_RUST = {0.74F, 0.42F, 0.22F}, GO_RUST2 = {0.56F, 0.30F, 0.18F}, GO_EYE = {1F, 0.80F, 0.28F};

    static void golem(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.012 * w;
        c.pivot = new V3(0, 0.3, 0);
        Paint iron = (n, p) -> mix(GO_DK, GO_G2, ss(-0.6, 0.9, n.y) * 0.85);
        Paint ironLight = (n, p) -> mix(GO_G, GO_G2, ss(-0.4, 0.9, n.y));
        // piernas cortas y fuertes con pies
        for (int sd = -1; sd <= 1; sd += 2) {
            double phs = c.gait * 0.9 + (sd > 0 ? 0 : Math.PI);
            double sz = Math.sin(phs) * 0.05 * w, lf = Math.max(0, Math.cos(phs)) * 0.03 * w;
            box(c, new V3(sd * 0.078, 0.095 + lf, sz), new V3(0.058, 0.060, 0.062), iron);
            box(c, new V3(sd * 0.078, 0.028 + lf, sz + 0.02), new V3(0.064, 0.028, 0.082), flat(GO_DK));
            box(c, new V3(sd * 0.078, 0.150 + lf, sz), new V3(0.05, 0.012, 0.055), flat(GO_RUST2));
        }
        // cintura y torso ancho
        box(c, new V3(0, 0.205 + bob, 0), new V3(0.115, 0.030, 0.088), flat(GO_DK));
        box(c, new V3(0, 0.305 + bob, 0), new V3(0.155, 0.080, 0.105), iron);
        box(c, new V3(0, 0.375 + bob, 0), new V3(0.165, 0.020, 0.112), ironLight);
        // placa del pecho con remaches
        box(c, new V3(0, 0.300 + bob, 0.103), new V3(0.085, 0.060, 0.007), ironLight);
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                ell(c, new V3(sx * 0.068, 0.300 + sy * 0.045 + bob, 0.113), new V3(0.009, 0.009, 0.006), 6, 4, flat(GO_DK));
            }
        }
        // núcleo cálido en el pecho
        c.emissive = true;
        double pulse = 0.75 + 0.25 * Math.sin(t * 0.15 + ph);
        ell(c, new V3(0, 0.300 + bob, 0.112), new V3(0.026, 0.026, 0.010), 8, 6, flat(new float[]{1F, (float) (0.62 + 0.2 * pulse), 0.22F}));
        glow(c, new V3(0, 0.300 + bob, 0.125), 0.14, new float[]{1F, 0.7F, 0.25F}, 0.35 * pulse);
        c.emissive = false;
        // óxido en el costado y la espalda
        box(c, new V3(0.10, 0.275 + bob, 0.1055), new V3(0.030, 0.028, 0.0025), flat(GO_RUST));
        box(c, new V3(-0.04, 0.245 + bob, 0.0995), new V3(0.020, 0.016, 0.0025), flat(GO_RUST2));
        box(c, new V3(0.045, 0.330 + bob, -0.1055), new V3(0.050, 0.035, 0.0025), flat(GO_RUST));
        // enredadera por el lado izquierdo con hojitas
        tube(c, new V3[]{new V3(-0.14, 0.40 + bob, 0.09), new V3(-0.125, 0.31 + bob, 0.108), new V3(-0.085, 0.215 + bob, 0.109)}, new double[]{0.008, 0.009, 0.008}, 5, flat(GO_V));
        for (int k = 0; k < 4; k++) {
            double yy = 0.37 - k * 0.045;
            ell(c, new V3(-0.115 + k * 0.012 + (k % 2 == 0 ? 0.03 : -0.02), yy + bob, 0.116), new V3(0.026, 0.008, 0.014), 0, 0, (k % 2 == 0 ? 0.5 : -0.5), 6, 4, flat(k % 2 == 0 ? GO_V2 : GO_V));
        }
        // hombros pesados con musgo
        for (int sd = -1; sd <= 1; sd += 2) {
            box(c, new V3(sd * 0.185, 0.375 + bob, 0), new V3(0.058, 0.040, 0.075), iron);
            box(c, new V3(sd * 0.185, 0.340 + bob, 0), new V3(0.050, 0.008, 0.066), flat(GO_RUST2));
        }
        ell(c, new V3(-0.19, 0.415 + bob, 0.0), new V3(0.052, 0.020, 0.062), 8, 5, flat(GO_V));
        ell(c, new V3(-0.18, 0.425 + bob, 0.012), new V3(0.026, 0.014, 0.030), 7, 4, flat(GO_V2));
        ell(c, new V3(0.18, 0.418 + bob, -0.02), new V3(0.030, 0.012, 0.035), 7, 4, flat(GO_V));
        // brazos largos con puños grandes y una flor en la mano derecha
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(c.gait * 0.9 + (sd > 0 ? Math.PI : 0)) * 0.05 * w + Math.sin(t * 0.07 + ph + sd) * 0.008;
            V3 a = new V3(sd * 0.20, 0.34 + bob, 0), b = new V3(sd * 0.225, 0.14 + bob, 0.02 + sw);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.046, 0.042, 0.048}, 8, iron);
            box(c, b.add(0, -0.032, 0.0), new V3(0.058, 0.044, 0.056), flat(GO_DK));
            box(c, b.add(0, -0.018, 0.0), new V3(0.062, 0.008, 0.060), flat(GO_RUST));
            for (int k = -1; k <= 1; k++) {
                ell(c, b.add(k * 0.025, -0.052, 0.04), new V3(0.012, 0.012, 0.012), 6, 4, flat(GO_DK));
            }
            if (sd > 0) {
                V3 f0 = b.add(0.0, -0.01, 0.05), f1 = f0.add(0.0, 0.085, 0.035);
                tube(c, new V3[]{f0, f0.lerp(f1, 0.5), f1}, new double[]{0.006, 0.006, 0.005}, 4, flat(GO_V));
                ell(c, f0.lerp(f1, 0.5).add(0.02, 0, 0), new V3(0.016, 0.006, 0.008), 0, 0, 0.5, 6, 4, flat(GO_V2));
                for (int k = 0; k < 6; k++) {
                    double a2 = k * Math.PI * 2 / 6;
                    ell(c, f1.add(Math.cos(a2) * 0.026, Math.sin(a2) * 0.026, 0.0), new V3(0.018, 0.018, 0.008), 6, 4, flat(GO_RED));
                }
                ell(c, f1.add(0, 0, 0.004), new V3(0.014, 0.014, 0.011), 6, 4, flat(new float[]{1F, 0.85F, 0.3F}));
            }
        }
        // cabeza grande y simpática
        headLook(c, 0.10, 0);
        c.hpv = new V3(0, 0.44, 0.0);
        V3 hc = new V3(0, 0.525 + bob, 0.005);
        box(c, hc, new V3(0.118, 0.098, 0.105), iron);
        box(c, hc.add(0, 0.093, 0), new V3(0.124, 0.014, 0.110), ironLight);
        // musgo y brote en lo alto
        ell(c, hc.add(-0.03, 0.115, -0.01), new V3(0.062, 0.020, 0.070), 8, 5, flat(GO_V));
        ell(c, hc.add(0.05, 0.110, 0.02), new V3(0.035, 0.016, 0.040), 7, 4, flat(GO_V2));
        tube(c, new V3[]{hc.add(-0.02, 0.12, -0.01), hc.add(-0.025, 0.155, -0.01), hc.add(-0.01, 0.18, 0.0)}, new double[]{0.004, 0.004, 0.003}, 4, flat(GO_V));
        ell(c, hc.add(0.005, 0.185, 0.0), new V3(0.020, 0.008, 0.012), 0, 0, 0.4, 6, 4, flat(GO_V2));
        ell(c, hc.add(-0.04, 0.17, -0.01), new V3(0.020, 0.008, 0.012), 0, 0, -0.5, 6, 4, flat(GO_V2));
        // tornillos a los lados
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.122, 0.0, 0.0), new V3(0.012, 0.022, 0.022), 6, 5, flat(GO_DK));
            box(c, hc.add(sd * 0.119, -0.045, 0.02), new V3(0.002, 0.020, 0.025), flat(GO_RUST2));
        }
        // ceja marcada
        box(c, hc.add(0, 0.040, 0.105), new V3(0.100, 0.012, 0.010), flat(GO_DK));
        // ojos brillantes que parpadean
        double bl = c.shut ? 0.15 : 1.0;
        for (int sd = -1; sd <= 1; sd += 2) {
            box(c, hc.add(sd * 0.050, 0.002, 0.1055), new V3(0.030, 0.030 * bl + 0.004, 0.003), flat(GO_DK));
        }
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            box(c, hc.add(sd * 0.050, 0.002, 0.1095), new V3(0.021, 0.021 * bl + 0.002, 0.0035), flat(GO_EYE));
            if (!c.shut) {
                box(c, hc.add(sd * 0.050 + 0.004, 0.0, 0.1135), new V3(0.007, 0.011, 0.002), flat(new float[]{0.35F, 0.18F, 0.05F}));
                glow(c, hc.add(sd * 0.050, 0.002, 0.125), 0.07, new float[]{1F, 0.8F, 0.3F}, 0.4);
            }
        }
        c.emissive = false;
        // nariz grande de golem
        box(c, hc.add(0, -0.030, 0.118), new V3(0.020, 0.052, 0.026), ironLight);
        box(c, hc.add(0, -0.074, 0.126), new V3(0.020, 0.010, 0.018), flat(GO_DK));
        // boquita y mejillas
        box(c, hc.add(0, -0.074, 0.1055), new V3(0.040, 0.005, 0.003), flat(GO_DK));
        for (int sd = -1; sd <= 1; sd += 2) {
            box(c, hc.add(sd * 0.080, -0.048, 0.1055), new V3(0.016, 0.010, 0.003), flat(new float[]{1F, 0.62F, 0.62F}));
        }
        // óxido en la frente
        box(c, hc.add(0.060, 0.062, 0.1055), new V3(0.022, 0.014, 0.0025), flat(GO_RUST));
        c.head = false;
    }

    // ------------------------------------------------------------------ 31. RATÓN RELÁMPAGO

    private static final float[] SM_Y = {1F, 0.84F, 0.20F}, SM_Y2 = {1F, 0.94F, 0.50F}, SM_BR = {0.45F, 0.28F, 0.14F}, SM_RD = {0.95F, 0.25F, 0.22F};

    static void sparkmouse(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.028 * w;
        c.pivot = new V3(0, 0.22, 0);
        c.emissive = false;
        // cola en zigzag de rayo
        double wg = Math.sin(t * 0.2 + ph) * 0.02;
        V3[] z = {new V3(0, 0.22 + bob, -0.12), new V3(0.0, 0.26, -0.20), new V3(0.09 + wg, 0.31, -0.24), new V3(-0.01 + wg, 0.37, -0.25), new V3(0.10 + wg * 1.5, 0.45, -0.26), new V3(0.03 + wg * 1.5, 0.50, -0.26)};
        for (int i = 0; i < z.length - 1; i++) {
            final int fi = i;
            tube(c, new V3[]{z[i], z[i + 1]}, new double[]{i == 0 ? 0.03 : 0.045, i == z.length - 2 ? 0.075 : 0.04}, 5, (n, p) -> fi == 0 ? flat(SM_BR).at(n, p) : mix(SM_Y, SM_Y2, 0.3));
        }
        // piecitos y manitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.2 + (sd > 0 ? 0 : Math.PI);
            ell(c, new V3(sd * 0.075, 0.03 + Math.max(0, Math.cos(p)) * 0.03 * w, 0.03 + Math.sin(p) * 0.035 * w), new V3(0.04, 0.03, 0.065), 8, 5, flat(SM_BR));
            V3 a = new V3(sd * 0.095, 0.24 + bob, 0.07), b = new V3(sd * 0.06, 0.20 + bob, 0.14);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.024, 0.021, 0.02}, 5, flat(SM_Y));
        }
        // cuerpo pera
        V3 bc = new V3(0, 0.205 + bob, -0.01), br = new V3(0.14, 0.155, 0.13);
        Paint bp = (n, p) -> mix(SM_Y, SM_Y2, ss(0, -1, n.y) * 0.6);
        ell(c, bc, br, 14, 10, bp);
        // pelito electrizado: se eriza cuando salta de alegría
        Detail.fur(c, bc, br, 170, 221, 0.03 + 0.02 * c.joy, 0.014, new V3(0, 0.3 + 0.6 * c.joy, -0.5), 0.7, Detail.ALL, bp, SM_Y2);
        Detail.fur(c, bc, br, 40, 222, 0.026, 0.012, new V3(0, -1, 0.2), 0.7, d -> ss(0.3, 0.8, d.z) * ss(0.4, -0.3, d.y), flat(SM_Y2), null);
        // bolsita de chispas: manchitas brillantes en la barriga
        Detail.twinkle(c, new V3(0, 0.15 + bob, 0.115), 0.02, new float[]{1F, 0.95F, 0.5F}, t, ph, 0.1);
        // rayas del lomo
        for (int k = 0; k < 2; k++) {
            ell(c, new V3(0, 0.255 - k * 0.055 + bob, -0.125 + k * 0.01), new V3(0.075, 0.012, 0.012), 8, 4, flat(SM_BR));
        }
        // cabeza
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.34, 0.06);
        V3 hc = new V3(0, 0.425 + bob, 0.075), hr = new V3(0.16, 0.125, 0.125);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(SM_Y, SM_Y2, ss(-0.3, 0.9, n.z) * 0.4));
        double nt = Math.sin(t * 0.7 + ph) * 0.003;
        ell(c, new V3(0, 0.405 + bob + nt, 0.185), new V3(0.016, 0.012, 0.010), 6, 4, flat(BLACK));
        Detail.glint(c, new V3(0, 0.411 + bob + nt, 0.19), new V3(0.016, 0.012, 0.010), new V3(0, 0.7, 1), 0.014, WHITE, 0.9);
        Detail.fur(c, hc, hr, 70, 223, 0.026, 0.012, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, (n, p) -> mix(SM_Y, SM_Y2, ss(-0.3, 0.9, n.z) * 0.4), SM_Y2);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.04, 0.395 + bob + k * 0.012, 0.18), new V3(sd * 0.17, 0.40 + bob + k * 0.04, 0.15), -0.01, 0.0028, new float[]{1F, 0.95F, 0.6F});
            }
            Detail.brow(c, hc, hr, sd, 0.50, 0.44, 0.78, 0.05, sd * 0.5, SM_BR);
            ell(c, new V3(sd * 0.006, 0.385 + bob, 0.18), new V3(0.007, 0.012, 0.004), 6, 4, flat(WHITE));
        }
        // orejas largas con punta negra
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = new V3(sd * 0.08, 0.51 + bob, 0.03), b1 = new V3(sd * 0.135, 0.60 + bob, 0.00), b2 = new V3(sd * 0.17, 0.69 + bob, -0.025);
            double ef = Detail.flick(c, sd > 0 ? 24 : 25);
            V3 fb1 = b1.add(sd * 0.03 * ef, -0.01 * ef, 0), fb2 = b2.add(sd * 0.08 * ef, -0.07 * ef, 0);
            curve(c, new V3[]{b0, fb1, fb2}, new double[]{0.042, 0.033, 0.006}, 3, 7, (n, p) -> mix(SM_Y, BLACK, ss(0.62, 0.68, p.y - bob)));
            if (ef > 0.5) Detail.twinkle(c, fb2.add(0, 0.02, 0), 0.02, new float[]{1F, 0.95F, 0.4F}, t, ph, 0.5);
        }
        eyes(c, hc, hr, 0.50, 0.14, 0.82, 0.10, T_BEAD, new float[]{1, 1, 1});
        // mejillas rojas eléctricas
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, hc, hr, new V3(sd * 0.96, -0.20, 0.42), 0.12, 0, T_WHITE, SM_RD, 0.95, 0.003);
        }
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.06, 0, T_MOUTH, new float[]{0.4F, 0.2F, 0.2F}, 0.9, 0.003);
        c.head = false;
        // arco eléctrico entre las mejillas cuando le sale la alegría
        if (c.joy > 0.3 || Detail.flick(c, 26) > 0.4) {
            c.emissive = true;
            for (int sd = -1; sd <= 1; sd += 2) {
                V3 a = new V3(sd * 0.17, 0.39 + bob, 0.10);
                V3 b = a.add(sd * 0.06, 0.04, 0.0), d = b.add(sd * 0.03, -0.05, 0.01), e = d.add(sd * 0.06, 0.04, 0.0);
                tube(c, new V3[]{a, b, d, e}, new double[]{0.004, 0.0045, 0.004, 0.002}, 4, flat(new float[]{1F, 0.97F, 0.55F}));
                glow(c, d, 0.09, new float[]{1F, 0.95F, 0.4F}, 0.6);
            }
            c.emissive = false;
        }
        // chispitas
        c.emissive = true;
        for (int k = 0; k < 3; k++) {
            double a = t * 0.12 + k * 2.1;
            double fl = Math.max(0, Math.sin(t * 0.37 + k * 2.3 + ph * 3));
            V3 wp = new V3(Math.cos(a) * 0.30, 0.32 + 0.1 * Math.sin(a * 2), Math.sin(a) * 0.30);
            glow(c, wp, 0.16 + 0.10 * fl, new float[]{1F, 0.95F, 0.4F}, 0.55);
        }
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 32. DROIDE ASTROMECÁNICO

    private static final float[] DR_W = {0.92F, 0.94F, 0.97F}, DR_B = {0.22F, 0.40F, 0.85F}, DR_S = {0.70F, 0.74F, 0.80F}, DR_DK = {0.25F, 0.28F, 0.34F};

    static void droid(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double rock = Math.sin(c.gait) * 0.03 * w;
        double bob = Math.abs(Math.sin(c.gait)) * 0.010 * w;
        c.pivot = new V3(0, 0.25, 0);
        // piernas laterales
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.135, 0.34 + bob, 0), b = new V3(sd * 0.165, 0.10, rock * sd * 0.5);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.026, 0.026, 0.028}, 6, flat(DR_S));
            box(c, new V3(sd * 0.165, 0.045, 0.012 + rock * sd * 0.5), new V3(0.04, 0.045, 0.085), flat(DR_W));
            box(c, new V3(sd * 0.165, 0.016, 0.02 + rock * sd * 0.5), new V3(0.043, 0.016, 0.09), flat(DR_DK));
        }
        // cuerpo cilíndrico
        Paint white = (n, p) -> mix(DR_S, DR_W, ss(-0.5, 0.8, n.x * 0.4 + n.y * 0.3 + n.z * 0.3 + 0.4));
        cyl(c, new V3(0, 0.15 + bob, 0), 0.26, 0.135, 16, white);
        // paneles azules y rejillas
        box(c, new V3(0, 0.30 + bob, 0.1325), new V3(0.075, 0.035, 0.004), flat(DR_B));
        box(c, new V3(0.045, 0.23 + bob, 0.1325), new V3(0.032, 0.026, 0.004), flat(DR_B));
        box(c, new V3(-0.05, 0.22 + bob, 0.1325), new V3(0.026, 0.02, 0.004), flat(DR_DK));
        box(c, new V3(0, 0.355 + bob, 0.115), new V3(0.04, 0.012, 0.008), flat(DR_DK));
        for (int sd = -1; sd <= 1; sd += 2) {
            box(c, new V3(sd * 0.1, 0.27 + bob, 0.095), new V3(0.012, 0.075, 0.004), flat(DR_B));
        }
        // tornillos, remaches y rayones
        for (int k = 0; k < 8; k++) {
            double a = k * Math.PI / 4 + 0.3;
            ell(c, new V3(Math.sin(a) * 0.134, 0.268 + bob, Math.cos(a) * 0.134), new V3(0.006, 0.006, 0.006), 5, 4, flat(DR_DK));
            ell(c, new V3(Math.sin(a) * 0.134, 0.03 + 0.13 + bob, Math.cos(a) * 0.134), new V3(0.006, 0.006, 0.006), 5, 4, flat(DR_DK));
        }
        for (int k = 0; k < 3; k++) {
            Detail.soft(c, new V3(0, 0.15 + bob, 0), new V3(0.135, 0.13, 0.135), new V3(Math.cos(k * 2.6 + 0.4), 0.1 - 0.3 * k, Math.sin(k * 2.6 + 0.4)), 0.05, DR_DK, 0.35);
        }
        // luces de estado que parpadean como un tablero
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            boolean on = Math.sin(t * (0.21 + 0.05 * k) + ph * 2 + k * 1.9) > 0.1;
            float[] lc2 = k % 2 == 0 ? new float[]{0.4F, 1F, 0.5F} : new float[]{1F, 0.8F, 0.3F};
            ell(c, new V3(-0.07 + k * 0.022, 0.185 + bob, 0.1355), new V3(0.007, 0.007, 0.004), 5, 4, flat(on ? lc2 : scale(lc2, 0.25)));
        }
        c.emissive = false;
        // tercera pierna central
        box(c, new V3(0, 0.05, 0.10), new V3(0.03, 0.045, 0.045), flat(DR_S));
        // cabeza abovedada que gira
        headLook(c, 0.55, 0);
        c.hpv = new V3(0, 0.43, 0);
        V3 hc = new V3(0, 0.43 + bob, 0);
        Paint dome = (n, p) -> {
            double a = Math.atan2(n.x, n.z);
            boolean band = Math.sin(a * 4 + 0.4) > 0.55 && n.y > 0.1;
            float[] base = mix(DR_S, DR_W, ss(-0.2, 0.9, n.y));
            return band ? mix(base, DR_B, 0.9) : base;
        };
        ell(c, hc, new V3(0.135, 0.125, 0.135), 18, 12, dome);
        Detail.glint(c, hc, new V3(0.135, 0.125, 0.135), new V3(-0.5, 1, 0.3), 0.06, WHITE, 0.7);
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            ell(c, new V3(Math.sin(a) * 0.134, 0.37 + bob, Math.cos(a) * 0.134), new V3(0.005, 0.005, 0.005), 5, 4, flat(DR_DK));
        }
        // ojo-lente
        V3 lc = new V3(0, 0.455 + bob, 0.118);
        ell(c, lc, new V3(0.042, 0.042, 0.02), 10, 7, flat(DR_DK));
        c.emissive = true;
        double bl = 0.75 + 0.25 * Math.sin(t * 0.3 + ph);
        ell(c, lc.add(0, 0, 0.012), new V3(0.026, 0.026, 0.012), 8, 6, flat(new float[]{1F, (float) (0.25 * bl), 0.2F}));
        glow(c, lc.add(0, 0, 0.03), 0.12, new float[]{1F, 0.3F, 0.25F}, 0.5 * bl);
        // el ojo se afoca: el iris cambia de tamaño
        double fo = 0.5 + 0.5 * Math.sin(t * 0.09 + ph);
        ell(c, lc.add(0, 0, 0.022), new V3(0.012 + 0.008 * fo, 0.012 + 0.008 * fo, 0.006), 6, 4, flat(new float[]{1F, 0.85F, 0.7F}));
        // lente pequeña azul
        ell(c, new V3(0.07, 0.40 + bob, 0.105), new V3(0.016, 0.016, 0.01), 6, 5, flat(new float[]{0.4F, 0.8F, 1F}));
        c.emissive = false;
        // periscopio
        tube(c, new V3[]{new V3(0.05, 0.55 + bob, 0.02), new V3(0.05, 0.60 + bob, 0.02), new V3(0.05, 0.63 + bob, 0.04)}, new double[]{0.011, 0.011, 0.012}, 6, flat(DR_DK));
        ell(c, new V3(0.05, 0.635 + bob, 0.055), new V3(0.016, 0.016, 0.016), 6, 5, flat(new float[]{0.4F, 0.8F, 1F}));
        // antenita que se mece
        {
            double aw = Math.sin(t * 0.13 + ph) * 0.02 + 0.04 * Detail.flick(c, 40);
            V3 a0 = new V3(-0.05, 0.55 + bob, 0.0), a1 = new V3(-0.05 + aw * 0.5, 0.62 + bob, 0.0), a2 = new V3(-0.05 + aw, 0.68 + bob, 0.0);
            tube(c, new V3[]{a0, a1, a2}, new double[]{0.004, 0.004, 0.0035}, 4, flat(DR_DK));
            c.emissive = true;
            ell(c, a2.add(0, 0.008, 0), new V3(0.01, 0.01, 0.01), 5, 4, flat(new float[]{1F, 0.3F, 0.3F}));
            glow(c, a2.add(0, 0.008, 0), 0.06, new float[]{1F, 0.3F, 0.3F}, 0.4 + 0.3 * Math.sin(t * 0.3 + ph));
            c.emissive = false;
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 33. ESPÍRITU DEL BOSQUE

    private static final float[] FS_G = {0.52F, 0.56F, 0.62F}, FS_G2 = {0.66F, 0.70F, 0.76F}, FS_CR = {0.97F, 0.94F, 0.84F}, FS_DK = {0.30F, 0.32F, 0.38F},
            FS_LF = {0.30F, 0.70F, 0.34F};

    static void forestSpirit(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.8)) * 0.014 * w + Math.sin(t * 0.08 + ph) * 0.004;
        c.pivot = new V3(0, 0.3, 0);
        // patas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 0.8 + (sd > 0 ? 0 : Math.PI);
            ell(c, new V3(sd * 0.12, 0.05 + Math.max(0, Math.cos(p)) * 0.03 * w, 0.06 + Math.sin(p) * 0.05 * w), new V3(0.075, 0.05, 0.10), 10, 6, flat(FS_DK));
            for (int k = -1; k <= 1; k++) {
                cone(c, new V3(sd * 0.12 + k * 0.03, 0.04, 0.15), new V3(sd * 0.12 + k * 0.035, 0.035, 0.19), 0.011, 0.002, 5, flat(WHITE));
            }
        }
        // cuerpo enorme y redondo
        Paint fur = (n, p) -> mix(FS_G, FS_G2, ss(-0.2, 1.0, n.y) * 0.7);
        V3 fc0 = new V3(0, 0.33 + bob, 0), fr0 = new V3(0.285, 0.275, 0.25);
        ell(c, fc0, fr0, 20, 14, fur);
        Detail.fur(c, fc0, fr0, 300, 231, 0.04, 0.02, new V3(0, -0.4, -1), 0.9, Detail.ALL, fur, FS_G2);
        // parches de musgo sobre el lomo
        Detail.fur(c, fc0, fr0, 130, 232, 0.05, 0.024, new V3(0, 0.6, 0), 0.0, Detail.TOP, flat(FS_LF), new float[]{0.7F, 0.95F, 0.5F});
        // dos hongos pequeñitos que crecen en su espalda
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 mb = new V3(sd * 0.14, 0.58 + bob, -0.12 - (sd > 0 ? 0.04 : 0));
            double mh = 0.05 + (sd > 0 ? 0.02 : 0);
            tube(c, new V3[]{mb, mb.add(0, mh, 0)}, new double[]{0.012, 0.010}, 6, flat(FS_CR));
            V3 mc = mb.add(0, mh, 0);
            ell(c, mc, new V3(0.035, 0.022, 0.035), 10, 5, (n, p) -> mix(new float[]{0.9F, 0.25F, 0.28F}, new float[]{1F, 0.5F, 0.45F}, ss(0.2, 1, n.y) * 0.4));
            Detail.spot(c, mc, new V3(0.035, 0.022, 0.035), new V3(0.3, 1, 0.2), 0.011, WHITE);
            Detail.spot(c, mc, new V3(0.035, 0.022, 0.035), new V3(-0.5, 0.8, -0.3), 0.009, WHITE);
        }
        // barriga crema con chevrones
        V3 bc = new V3(0, 0.29 + bob, 0.10), br = new V3(0.20, 0.215, 0.17);
        ell(c, bc, br, 16, 11, flat(FS_CR));
        for (int k = 0; k < 3; k++) {
            for (int sd = -1; sd <= 1; sd += 2) {
                decal(c, bc, br, new V3(sd * 0.22, 0.30 - k * 0.28, 1), 0.075, sd * 0.9, T_MOUTH, FS_DK, 0.8, 0.003);
            }
        }
        // bracitos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.25, 0.38 + bob, 0.02), b = new V3(sd * 0.28, 0.20 + bob, 0.10);
            tube(c, new V3[]{a, a.lerp(b, 0.5).add(sd * 0.02, 0, 0.02), b}, new double[]{0.055, 0.048, 0.045}, 7, flat(FS_G));
            for (int k = -1; k <= 1; k++) {
                cone(c, b.add(k * 0.02, -0.02, 0.01), b.add(k * 0.026, -0.05, 0.035), 0.009, 0.002, 5, flat(WHITE));
            }
        }
        // cabeza (casi fundida con el cuerpo)
        headLook(c, 0.06, 0.0);
        c.hpv = new V3(0, 0.52, 0.0);
        V3 hc = new V3(0, 0.58 + bob, 0.04), hr = new V3(0.23, 0.17, 0.20);
        Paint hp = (n, p) -> mix(FS_G, FS_G2, ss(-0.2, 1.0, n.y) * 0.6);
        ell(c, hc, hr, 18, 12, hp);
        Detail.fur(c, hc, hr, 140, 233, 0.034, 0.016, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.55 ? 1 : 0, hp, FS_G2);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 6; k++) {
                V3 bs = surf(hc, hr, new V3(sd * (0.92 - k * 0.04), -0.30 + k * 0.05, 0.3 - k * 0.12));
                Detail.kite(c, bs, bs.add(sd * (0.05 + k * 0.005), -0.045, -0.05), new V3(0, 0, 1), 0.02, new V3(sd, 0, 0), FS_G, FS_G2);
            }
            Detail.brow(c, hc, hr, sd, 0.62, 0.44, 0.70, 0.07, sd * 0.4, FS_DK);
        }
        Detail.glint(c, new V3(0, 0.612 + bob, 0.24), new V3(0.04, 0.026, 0.02), new V3(0, 0.7, 1), 0.026, WHITE, 0.85);
        // orejas puntiagudas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = new V3(sd * 0.11, 0.70 + bob, -0.01), b1 = new V3(sd * 0.14, 0.80 + bob, -0.02), b2 = new V3(sd * 0.15, 0.89 + bob, -0.03);
            double ef = Detail.flick(c, sd > 0 ? 27 : 28);
            b1 = b1.add(sd * 0.03 * ef, 0, 0); b2 = b2.add(sd * 0.09 * ef, -0.06 * ef, 0);
            curve(c, new V3[]{b0, b1, b2}, new double[]{0.065, 0.048, 0.007}, 3, 8, fur);
            Detail.furTube(c, new V3[]{b0, b1, b2}, new double[]{0.065, 0.048, 0.007}, 14, 234 + sd, 0.035, 0.016, 0.8, fur, FS_G2);
            curve(c, new V3[]{b0.add(0, 0, 0.03), b1.add(0, 0, 0.03), b2.add(-sd * 0.004, -0.02, 0.03)}, new double[]{0.036, 0.026, 0.004}, 3, 6, flat(mix(FS_CR, FS_G, 0.3)));
        }
        // nariz, ojos, sonrisa ancha y bigotes
        ell(c, new V3(0, 0.605 + bob, 0.235), new V3(0.04, 0.026, 0.02), 8, 5, flat(BLACK));
        eyes(c, hc, hr, 0.62, 0.24, 0.70, 0.085, T_BEAD, new float[]{1, 1, 1});
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.22, 0, T_MOUTH, new float[]{0.2F, 0.2F, 0.26F}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 a = surf(hc, hr, new V3(sd * 0.55, -0.08 - k * 0.07, 0.8));
                V3 b = a.add(sd * 0.17, -0.02 + (k - 1) * 0.045, 0.0);
                tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.003, 0.0025, 0.002}, 4, flat(FS_DK));
            }
        }
        cheeks(c, hc, hr, 0.95, -0.15, 0.42, 0.085);
        c.head = false;
        // hoja-paraguas
        double sway = Math.sin(t * 0.06 + ph) * 0.03;
        V3 lt = new V3(sway, 0.98 + bob, -0.02);
        tube(c, new V3[]{new V3(0.0, 0.77 + bob, 0.0), new V3(sway * 0.5, 0.88 + bob, -0.01), lt}, new double[]{0.008, 0.008, 0.008}, 5, flat(FS_LF));
        ell(c, lt.add(0, 0.03, 0), new V3(0.20, 0.016, 0.15), 0, 0.1, sway * 2, 14, 6, (n, p) -> mix(FS_LF, new float[]{0.65F, 0.90F, 0.45F}, ss(-0.5, 0.9, n.y)));
        // luciérnagas
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            double a = t * 0.04 + k * 1.6;
            V3 wp = new V3(Math.cos(a) * 0.5, 0.35 + 0.22 * Math.sin(a * 1.7 + k), Math.sin(a) * 0.5);
            glow(c, wp, 0.14, new float[]{0.8F, 1F, 0.5F}, 0.7);
        }
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 34. LOBO DE LUNA

    private static final float[] WF_G = {0.66F, 0.72F, 0.84F}, WF_G2 = {0.90F, 0.93F, 0.99F}, WF_DK = {0.28F, 0.32F, 0.46F}, WF_GL = {0.62F, 0.85F, 1F};

    static void moonWolf(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w;
        c.pivot = new V3(0, 0.34, 0);
        glow(c, new V3(0, 0.45, 0), 1.6, WF_GL, 0.07 + 0.02 * Math.sin(t * 0.07));
        // cola tupida
        double wag = Math.sin(t * 0.12 + ph) * (0.04 + 0.03 * w);
        V3[] tcp = {new V3(0, 0.40 + bob, -0.27), new V3(wag * 0.5, 0.40, -0.42), new V3(wag, 0.35, -0.58), new V3(wag * 1.3, 0.27, -0.69)};
        curve(c, tcp, new double[]{0.05, 0.085, 0.095, 0.03}, 4, 10, (n, p) -> mix(WF_G, WF_G2, ss(0.4, 0.2, p.y) * 0.7));
        Detail.furTube(c, tcp, new double[]{0.05, 0.085, 0.095, 0.03}, 130, 241, 0.05, 0.022, 0.9, (n, p) -> mix(WF_G, WF_G2, ss(0.4, 0.2, p.y) * 0.7), WF_G2);
        legs4(c, 0.085, 0.17, -0.18, 0.27 + bob, 0.04, 0.03, 0.06, (n, p) -> mix(WF_G, WF_DK, ss(0.12, 0.02, p.y) * 0.8), flat(WF_DK));
        V3 bc = new V3(0, 0.40 + bob, -0.03), br = new V3(0.15, 0.14, 0.30);
        Paint bp = (n, p) -> mix(mix(WF_G, WF_G2, ss(0.2, 1.0, n.y) * 0.3), WF_G2, ss(-0.2, -0.9, n.y) * 0.8);
        ell(c, bc, br, 16, 11, bp);
        Detail.fur(c, bc, br, 240, 242, 0.04, 0.017, new V3(0, -0.4, -1), 0.9, Detail.ALL, bp, WF_G2);
        // constelación en el lomo
        {
            double[][] st = {{-0.05, 0.545, 0.08}, {0.04, 0.55, 0.0}, {-0.02, 0.545, -0.1}, {0.06, 0.535, -0.17}, {-0.06, 0.53, -0.22}};
            for (int k = 0; k < st.length; k++) {
                Detail.twinkle(c, new V3(st[k][0], st[k][1] + bob, st[k][2]), 0.02, WF_GL, t, ph + k * 1.7, 0.08);
            }
            V3[] cl = new V3[st.length];
            for (int k = 0; k < st.length; k++) cl[k] = new V3(st[k][0], st[k][1] + bob - 0.002, st[k][2]);
            c.emissive = true;
            tube(c, cl, new double[]{0.0022, 0.0022, 0.0022, 0.0022, 0.0022}, 3, flat(new float[]{0.7F, 0.9F, 1F}));
            c.emissive = false;
        }
        // pelaje del cuello (mantilla)
        V3 mc0 = new V3(0, 0.43 + bob, 0.20), mr0 = new V3(0.145, 0.15, 0.12);
        ell(c, mc0, mr0, 12, 8, flat(WF_G2));
        Detail.fur(c, mc0, mr0, 110, 243, 0.06, 0.024, new V3(0, -0.5, 0.2), 0.7, d -> ss(0.0, 0.6, d.z + 0.3), flat(WF_G2), WHITE);
        V3[] nk = {new V3(0, 0.45 + bob, 0.20), new V3(0, 0.53 + bob, 0.27), new V3(0, 0.58 + bob, 0.32)};
        tube(c, nk, new double[]{0.095, 0.082, 0.078}, 10, (n, p) -> mix(WF_G, WF_G2, 0.4));
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.55, 0.28);
        V3 hc = new V3(0, 0.62 + bob, 0.35), hr = new V3(0.115, 0.10, 0.115);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(WF_G, WF_G2, ss(-0.2, 0.9, n.y) * 0.4));
        // hocico largo
        V3 sc = new V3(0, 0.595 + bob, 0.455), sr = new V3(0.050, 0.042, 0.095);
        ell(c, sc, sr, 12, 8, (n, p) -> mix(WF_G2, WF_G, ss(0.4, 1.0, n.y) * 0.4));
        double nt = Math.sin(t * 0.5 + ph) * 0.003;
        ell(c, new V3(0, 0.61 + bob + nt, 0.55), new V3(0.021, 0.016, 0.014), 8, 5, flat(BLACK));
        Detail.glint(c, new V3(0, 0.618 + bob + nt, 0.558), new V3(0.021, 0.016, 0.014), new V3(0, 0.7, 1), 0.016, WHITE, 0.9);
        Detail.fur(c, hc, hr, 90, 244, 0.03, 0.013, new V3(0, -0.4, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, (n, p) -> mix(WF_G, WF_G2, ss(-0.2, 0.9, n.y) * 0.4), WF_G2);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 6; k++) {
                V3 bs = surf(hc, hr, new V3(sd * (0.92 - k * 0.04), -0.30 + k * 0.05, 0.3 - k * 0.12));
                Detail.kite(c, bs, bs.add(sd * (0.05 + k * 0.005), -0.045, -0.05), new V3(0, 0, 1), 0.02, new V3(sd, 0, 0), WF_G2, WHITE);
            }
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.035, 0.595 + bob + k * 0.012, 0.52), new V3(sd * 0.16, 0.60 + bob + k * 0.035, 0.49), -0.01, 0.0028, new float[]{0.9F, 0.95F, 1F});
            }
            Detail.brow(c, hc, hr, sd, 0.50, 0.44, 0.78, 0.05, sd * 0.5, WF_GL);
        }
        // orejas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b0 = new V3(sd * 0.07, 0.69 + bob, 0.31), b1 = new V3(sd * 0.095, 0.77 + bob, 0.30), b2 = new V3(sd * 0.11, 0.84 + bob, 0.29);
            double ef = Detail.flick(c, sd > 0 ? 29 : 30);
            b1 = b1.add(sd * 0.025 * ef, 0, 0); b2 = b2.add(sd * 0.07 * ef, -0.05 * ef, 0);
            curve(c, new V3[]{b0, b1, b2}, new double[]{0.045, 0.034, 0.005}, 3, 7, (n, p) -> mix(WF_G, WF_DK, ss(0.76, 0.84, p.y - bob)));
            Detail.furTube(c, new V3[]{b0, b1, b2}, new double[]{0.045, 0.034, 0.005}, 10, 245 + sd, 0.028, 0.013, 0.8, (n, p) -> WF_G, WF_G2);
        }
        // creciente de luna en la frente (brillante)
        c.emissive = true;
        V3[] cres = new V3[7];
        for (int i = 0; i < 7; i++) {
            double a = -0.9 + i * 0.3;
            cres[i] = new V3(Math.sin(a) * 0.05 + 0.0, 0.705 + bob + (1 - Math.cos(a)) * 0.0 + Math.cos(a) * 0.03 - 0.02, 0.40 + 0.0);
        }
        // arco en forma de "C" mirando hacia arriba
        V3[] arc = new V3[7];
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * (0.15 + 0.7 * i / 6.0);
            arc[i] = new V3(Math.cos(a) * 0.045, 0.665 + bob + Math.sin(a) * 0.035, 0.425 - Math.sin(a) * 0.012);
        }
        tube(c, arc, new double[]{0.003, 0.006, 0.008, 0.009, 0.008, 0.006, 0.003}, 5, flat(new float[]{0.8F, 0.95F, 1F}));
        glow(c, new V3(0, 0.69 + bob, 0.43), 0.14 + 0.03 * c.breath, WF_GL, 0.5);
        c.emissive = false;
        eyes(c, hc, hr, 0.50, 0.22, 0.82, 0.09, T_EYE, WF_GL);
        c.head = false;
        // estrellitas flotando
        c.emissive = true;
        for (int k = 0; k < 4; k++) {
            double a = t * 0.04 + k * 1.57;
            V3 wp = new V3(Math.cos(a) * 0.55, 0.45 + 0.2 * Math.sin(a * 2 + k), Math.sin(a) * 0.55);
            glow(c, wp, 0.13, new float[]{0.85F, 0.95F, 1F}, 0.7);
        }
        c.emissive = false;
    }

    // ------------------------------------------------------------------ 35. DRAGÓN DE LA NOCHE

    private static final float[] ND_B = {0.12F, 0.12F, 0.17F}, ND_B2 = {0.22F, 0.22F, 0.32F}, ND_BL = {0.30F, 0.34F, 0.42F},
            ND_EY = {0.45F, 1F, 0.40F}, ND_PL = {0.55F, 0.45F, 1F};

    static void nightDragon(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.14 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double fl = Math.sin(t * 0.34 + ph);
        Paint skin = (n, p) -> mix(ND_B, ND_B2, ss(-0.2, 1.0, n.y) * 0.7);
        // alas: huesos y membrana
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 root = new V3(sd * 0.10, 0.47 + bob, 0.02);
            double up = 0.55 + fl * 0.55;
            V3 elbow = root.add(sd * 0.22, 0.10 + up * 0.20, -0.02);
            V3 hand = elbow.add(sd * 0.30, 0.10 + up * 0.26, -0.10);
            V3[] tips = new V3[5];
            for (int k = 0; k < 5; k++) {
                double f = k / 4.0;
                tips[k] = hand.add(sd * (0.20 - f * 0.18), (0.12 - f * 0.34) * (0.4 + up * 0.8) - f * 0.06, -0.06 - f * 0.40);
            }
            tube(c, new V3[]{root, elbow, hand}, new double[]{0.026, 0.020, 0.014}, 6, skin);
            c.blend = Sink.NORMAL;
            c.alpha = 0.95;
            Creatures4.membrane(c, root.add(0, -0.03, -0.12), tips, (n, p) -> ND_B2);
            Creatures4.membrane(c, hand, tips, (n, p) -> ND_BL);
            c.alpha = 1;
            c.blend = Sink.SOLID;
            for (int k = 0; k < 5; k++) {
                tube(c, new V3[]{hand, hand.lerp(tips[k], 0.5), tips[k]}, new double[]{0.009, 0.007, 0.004}, 4, skin);
            }
        }
        // cola con aleta
        double sw = Math.sin(t * 0.18 + ph);
        V3[] tcp = {new V3(0, 0.42 + bob, -0.22), new V3(sw * 0.04, 0.38 + bob, -0.40), new V3(sw * 0.10, 0.36 + bob, -0.58), new V3(sw * 0.15, 0.37 + bob, -0.74)};
        curve(c, tcp, new double[]{0.08, 0.058, 0.04, 0.026}, 4, 8, skin);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, tcp[3].add(sd * 0.05, 0.0, -0.05), new V3(0.075, 0.008, 0.095), 0, sd * 0.1, -sd * 0.35, 10, 5, flat(sd > 0 ? ND_BL : new float[]{0.55F, 0.20F, 0.22F}));
        }
        // patas cortas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.10 : -0.12;
                V3 hip = new V3(sd * 0.10, 0.36 + bob, z0), fo = new V3(sd * 0.115, 0.27 + bob + Math.sin(t * 0.14 + fb + sd) * 0.01, z0 + 0.03);
                tube(c, new V3[]{hip, hip.lerp(fo, 0.5), fo}, new double[]{0.04, 0.032, 0.026}, 6, skin);
                for (int k = -1; k <= 1; k++) {
                    cone(c, fo.add(k * 0.014, 0, 0.0), fo.add(k * 0.018, -0.022, 0.035), 0.008, 0.002, 4, flat(WHITE));
                }
            }
        }
        // cuerpo
        V3 bc = new V3(0, 0.44 + bob, -0.04), br = new V3(0.145, 0.135, 0.25);
        Paint bp = (n, p) -> mix(skin.at(n, p), ND_BL, ss(-0.2, -0.95, n.y) * 0.8);
        ell(c, bc, br, 16, 11, bp);
        Detail.scales(c, bc, br, 9, 14, 0.04, Detail.ALL, bp, scale(ND_B2, 1.25));
        // placas del vientre
        for (int k = 0; k < 6; k++) {
            Detail.soft(c, bc, br, new V3(0, -1, 0.9 - k * 0.35), 0.07, ND_BL, 0.8);
        }
        // crestas y manchitas de plasma a lo largo del lomo
        for (int k = 0; k < 7; k++) {
            V3 rb = new V3(0, 0.575 + bob - 0.006 * k, 0.18 - k * 0.065);
            cone(c, rb, rb.add(0, 0.04 - 0.002 * k, -0.01), 0.012, 0.002, 4, flat(ND_BL));
            Detail.twinkle(c, rb.add(0, -0.004, 0.012), 0.014, ND_PL, t, ph + k * 0.9, 0.1);
        }
        // las patas llevan escamitas
        Detail.furTube(c, tcp, new double[]{0.08, 0.058, 0.04, 0.026}, 24, 381, 0.035, 0.016, 0.8, skin, ND_BL);
        // cuello
        V3[] nk = {new V3(0, 0.46 + bob, 0.14), new V3(0, 0.52 + bob, 0.22), new V3(0, 0.56 + bob, 0.28)};
        tube(c, nk, new double[]{0.095, 0.085, 0.085}, 10, skin);
        // cabeza grande y redonda, hocico corto
        headLook(c, 0.16, 0.02);
        c.hpv = new V3(0, 0.52, 0.22);
        V3 hc = new V3(0, 0.60 + bob, 0.32), hr = new V3(0.16, 0.125, 0.14);
        ell(c, hc, hr, 18, 12, (n, p) -> mix(ND_B, ND_B2, ss(-0.2, 1.0, n.y) * 0.8));
        V3 sc = new V3(0, 0.575 + bob, 0.425), sr = new V3(0.085, 0.065, 0.075);
        ell(c, sc, sr, 12, 8, (n, p) -> mix(ND_B, ND_B2, 0.4));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.026, 0.60 + bob, 0.488), new V3(0.011, 0.008, 0.007), 5, 4, flat(BLACK));
        }
        // orejas-aletas largas hacia atrás + cuernitos
        for (int sd = -1; sd <= 1; sd += 2) {
            double fw = Math.sin(t * 0.09 + ph + sd) * 0.02;
            double ef = Detail.flick(c, sd > 0 ? 41 : 42);
            fw += 0.04 * ef;
            curve(c, new V3[]{new V3(sd * 0.08, 0.70 + bob, 0.27), new V3(sd * 0.12, 0.77 + bob + fw, 0.20), new V3(sd * 0.15, 0.80 + bob + fw, 0.10)},
                    new double[]{0.045, 0.036, 0.008}, 3, 7, skin);
            cone(c, new V3(sd * 0.05, 0.72 + bob, 0.30), new V3(sd * 0.055, 0.78 + bob, 0.33), 0.016, 0.002, 5, flat(ND_BL));
        }
        Detail.scales(c, hc, hr, 5, 11, 0.032, d -> d.y > -0.1 && d.z < 0.55 ? 1 : 0, (n, p) -> mix(ND_B, ND_B2, ss(-0.2, 1.0, n.y) * 0.8), scale(ND_B2, 1.3));
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.52, 0.46, 0.80, 0.07, sd * 0.4, ND_BL);
            ell(c, sc.add(sd * 0.026, 0.0, 0.0).add(0, 0.024, 0.056), new V3(0.012, 0.006, 0.006), 5, 3, flat(ND_PL));
        }
        Detail.glint(c, hc, hr, new V3(0.3, 1, 0.4), 0.05, WHITE, 0.6);
        // ojos grandes verdes
        eyes(c, hc, hr, 0.52, 0.20, 0.80, 0.14, T_EYE, ND_EY);
        // sonrisa sin dientes
        decal(c, hc, hr, new V3(0, -0.40, 1), 0.13, 0, T_MOUTH, new float[]{0.55F, 0.35F, 0.40F}, 0.9, 0.003);
        c.head = false;
        // chispas de plasma
        c.emissive = true;
        for (int k = 0; k < 3; k++) {
            double a = t * 0.06 + k * 2.1;
            V3 wp = new V3(Math.cos(a) * 0.5, 0.36 + 0.14 * Math.sin(a * 2 + k), Math.sin(a) * 0.5);
            glow(c, wp, 0.14, ND_PL, 0.55);
        }
        c.emissive = false;
    }
}
