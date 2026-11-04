package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Lote 9: Remendín, Tricerito Corazón y Yetito de las Nieves.
 */
final class Pets18 {
    private Pets18() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    private static final float[] GOLD = hex(0xFFC83A), GOLD2 = hex(0xFFE88A);

    private static void eyePair(Ctx c, V3 hc, V3 hr, double dx, double dy, double dz, double size) {
        decal(c, hc, hr, new V3(dx, dy, dz), size, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-dx, dy, dz), size, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
    }

    private static void sparkle(Ctx c, double t, double ph, double bob, float[] col) {
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.33) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.25 + u * 0.3 + bob, Math.cos(k * 1.7) * 0.15), 0.018, col, t, ph + k, 0.25);
        }
    }

    // ------------------------------------------------------------------ 61. TRICERITO CORAZÓN

    private static final float[] TC_G = hex(0x62D8B4), TC_G2 = hex(0x9AF0D0), TC_CR = hex(0xFFF2D8), TC_PK = hex(0xFF7C98), TC_PK2 = hex(0xFFB4C4), TC_HT = hex(0xFF4F7A);

    private static void heart(Ctx c, V3 at, double s, float[] col) {
        boolean em = c.emissive;
        c.emissive = true;
        ell(c, at.add(-0.55 * s, 0.35 * s, 0), new V3(0.62 * s, 0.62 * s, 0.4 * s), 10, 8, flat(col));
        ell(c, at.add(0.55 * s, 0.35 * s, 0), new V3(0.62 * s, 0.62 * s, 0.4 * s), 10, 8, flat(col));
        cone(c, at.add(0, 0.25 * s, 0), at.add(0, -1.3 * s, 0), 1.1 * s, 0.0, 8, flat(col));
        c.emissive = em;
    }

    static void heartTrike(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        double sw = Math.sin(t * 0.08 + ph) * 0.03 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.2 + bob, -0.14), new V3(sw * 0.4, 0.19 + bob, -0.23), new V3(sw, 0.21 + bob, -0.29)};
        curve(c, tcp, new double[]{0.07, 0.05, 0.03}, 4, 8, (n, p) -> mix(TC_G, TC_G2, 0.3F));
        ell(c, tcp[2].add(0, 0.01, -0.012), new V3(0.032, 0.032, 0.032), 8, 6, flat(TC_PK));
        legs4(c, 0.088, 0.095, -0.095, 0.13 + bob, 0.05, 0.032, 0.05, (n, p) -> mix(TC_G, TC_G2, 0.4F), flat(TC_CR));
        V3 bc = new V3(0, 0.205 + bob, -0.01), br = new V3(0.145, 0.125, 0.165);
        Paint bp = (n, p) -> mix(TC_G, TC_G2, ss(0.1, 0.9, n.y) * 0.5F);
        ell(c, bc, br, 26, 17, (n, p) -> mix(bp.at(n, p), new float[]{TC_CR[0], TC_CR[1], TC_CR[2], 1F}, ss(-0.15, -0.65, n.y)));
        for (int k = 0; k < 4; k++) {
            V3 b0 = bc.add(0, 0.115 - Math.abs(k - 1.5) * 0.012, 0.1 - k * 0.07);
            cone(c, b0, b0.add(0, 0.045, -0.008), 0.02, 0.0, 5, flat(TC_PK));
        }
        headLook(c, 0.1, 0.0);
        c.hpv = new V3(0, 0.25, 0.12);
        V3 hc = new V3(0, 0.27 + bob, 0.17), hr = new V3(0.125, 0.11, 0.11);
        ell(c, hc, hr, 24, 15, bp);
        V3 sn = hc.add(0, -0.03, 0.09);
        ell(c, sn, new V3(0.062, 0.05, 0.055), 12, 8, flat(TC_CR));
        ell(c, sn.add(0, 0.035, 0.035), new V3(0.026, 0.02, 0.02), 8, 6, flat(TC_PK2));
        decal(c, hc, hr, new V3(0, -0.45, 1), 0.085, 0.0, T_MOUTH, new float[]{0.4F, 0.2F, 0.25F}, 0.95, 0.006);
        decal(c, hc, hr, new V3(0.5, 0.2, 0.8), 0.085, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        decal(c, hc, hr, new V3(-0.5, 0.2, 0.8), 0.085, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        cheeks(c, hc, hr, 0.82, -0.12, 0.5, 0.055);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 h0 = hc.add(sd * 0.06, 0.075, 0.05);
            cone(c, h0, h0.add(sd * 0.01, 0.045, 0.06), 0.024, 0.006, 6, flat(TC_CR));
            ell(c, h0.add(sd * 0.01, 0.045, 0.06), new V3(0.011, 0.011, 0.011), 6, 5, flat(TC_PK));
        }
        // gola rosa con puntitos crema
        V3 fc = hc.add(0, 0.05, -0.06);
        ell(c, fc, new V3(0.15, 0.14, 0.02), 0.0, -0.3, 0.0, 24, 14, (n, p) -> mix(TC_PK, TC_PK2, ss(0.2, 1.0, n.y) * 0.6F));
        for (int k = 0; k < 11; k++) {
            double a = -Math.PI * 0.5 - 0.1 + k * (Math.PI + 0.2) / 10;
            V3 sp = fc.add(Math.sin(a) * 0.135, Math.cos(a) * 0.125 - 0.005, 0.0);
            ell(c, sp, new V3(0.017, 0.017, 0.012), 6, 5, flat(TC_CR));
        }
        double pulse = 1.0 + 0.14 * Math.sin(t * 0.16 + ph) + 0.06 * Math.sin(t * 0.32 + ph);
        V3 hp = fc.add(0, 0.09, 0.025);
        heart(c, hp, 0.046 * pulse, TC_HT);
        glow(c, hp, 0.16, new float[]{1F, 0.4F, 0.55F}, 0.4 * pulse);
        c.head = false;
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + k * 0.25 + ph * 0.1) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.16, 0.42 + u * 0.3, -0.05 + Math.cos(k * 1.7) * 0.1), 0.02 * (1.1 - u * 0.6), new float[]{1F, 0.6F, 0.75F}, t, ph + k, 0.35);
        }
    }

    // ------------------------------------------------------------------ 62. YETITO DE LAS NIEVES

    private static final float[] YT_W = hex(0xF4FAFF), YT_B = hex(0xBCDCFF), YT_FC = hex(0xB8D4F4), YT_FT = hex(0x4A9CF0), YT_IC = hex(0x9AEAFF), YT_RD = hex(0xE0443C);

    private static final float[] YT_PK = hex(0xFFB4C8);

    static void snowYeti(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.018 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // patotas azules redondas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.045 * w, lift = Math.max(0, Math.cos(p)) * 0.035 * w;
            V3 fc = new V3(sd * 0.075, 0.035 + lift, 0.035 + sw);
            ell(c, fc, new V3(0.06, 0.036, 0.085), 14, 9, (n, q) -> mix(YT_FT, YT_IC, ss(0.3, 1.0, n.y) * 0.5F));
            for (int k = -1; k <= 1; k++) {
                ell(c, fc.add(k * 0.028, 0.0, 0.075), new V3(0.017, 0.016, 0.02), 6, 5, flat(YT_W));
            }
        }
        V3 bc = new V3(0, 0.2 + bob, 0.0), br = new V3(0.16, 0.145, 0.15);
        Paint bp = (n, p) -> mix(YT_W, YT_B, ss(0.1, -0.95, n.y) * 0.7F);
        ell(c, bc, br, 24, 16, bp);
        Detail.fur(c, bc, br, 420, 9301, 0.026, 0.028, new V3(0, -0.7, -0.3), 0.6, Detail.ALL, bp, YT_W);
        // panza clarita
        decal(c, bc, br, new V3(0, -0.15, 1), 0.2, 0.0, Detail.T_DISC, YT_FC, 0.5, 0.004);
        // cristales de hielo en la espalda
        for (int k = 0; k < 5; k++) {
            double a = -0.8 + k * 0.4;
            V3 b0 = bc.add(Math.sin(a) * 0.1, 0.08 + (k % 2) * 0.02, -0.1 - Math.cos(a) * 0.03);
            boolean em = c.emissive;
            c.emissive = true;
            cone(c, b0, b0.add(Math.sin(a) * 0.04, 0.05 + (k % 2) * 0.04, -0.035), 0.02, 0.0, 5, flat(YT_IC));
            c.emissive = em;
        }
        // brazos redondos y suaves
        for (int sd = -1; sd <= 1; sd += 2) {
            boolean wave = sd > 0;
            double wv = wave ? Math.sin(t * 0.16 + ph) * 0.03 : Math.sin(c.gait + sd) * 0.025 * w;
            V3 sh = new V3(sd * 0.15, 0.25 + bob, 0.02);
            V3 hd = wave ? new V3(sd * 0.23, 0.33 + bob + wv, 0.07) : new V3(sd * 0.2, 0.15 + bob + wv, 0.1);
            V3 mid = sh.lerp(hd, 0.5);
            double ang = wave ? -0.9 : -0.4;
            ell(c, mid, new V3(0.052, 0.075, 0.052), 0.0, 0.0, sd * ang, 12, 8, bp);
            Detail.fur(c, mid, new V3(0.052, 0.075, 0.052), 40, 9303 + sd, 0.026, 0.024, new V3(0, -1, 0), 0.5, Detail.ALL, bp, YT_W);
            ell(c, hd, new V3(0.052, 0.05, 0.05), 12, 8, (n, q) -> mix(YT_FT, YT_IC, ss(0.2, 1, n.y) * 0.5F));
            if (!wave) {
                // cristal brillante que carga en la mano
                boolean em = c.emissive;
                c.emissive = true;
                V3 cr = hd.add(0.0, 0.045, 0.03);
                double sp = Math.sin(t * 0.07 + ph) * 0.01;
                cone(c, cr.add(0, 0.045 + sp, 0), cr, 0.022, 0.0, 6, flat(YT_IC));
                cone(c, cr.add(0, -0.04 + sp, 0), cr, 0.022, 0.0, 6, flat(hex(0xD8F6FF)));
                c.emissive = em;
                glow(c, cr, 0.14, new float[]{0.6F, 0.9F, 1F}, 0.45);
            }
        }
        // cabeza grande y tierna
        headLook(c, 0.1, 0.0);
        c.hpv = new V3(0, 0.3, 0.05);
        V3 hc = new V3(0, 0.36 + bob, 0.04), hr = new V3(0.135, 0.115, 0.115);
        Paint hp = (n, p) -> mix(YT_W, YT_B, ss(0.0, -0.95, n.y) * 0.5F);
        ell(c, hc, hr, 22, 15, hp);
        Detail.fur(c, hc, hr, 110, 9302, 0.024, 0.026, new V3(0, -0.3, -1), 0.7, Detail.TOP, hp, YT_W);
        Detail.fur(c, hc, hr, 40, 9305, 0.022, 0.024, new V3(0, 0, -1), 0.7, Detail.BACK, hp, YT_W);
        // flequillo
        for (int k = -2; k <= 2; k++) {
            V3 b0 = hc.add(k * 0.03, 0.095 - Math.abs(k) * 0.012, 0.055);
            cone(c, b0, b0.add(k * 0.008, -0.04, 0.03), 0.022, 0.0, 5, flat(YT_W));
        }
        // orejitas peludas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = hc.add(sd * 0.13, 0.02, -0.01);
            ell(c, ec, new V3(0.03, 0.04, 0.025), 10, 7, hp);
            Detail.fur(c, ec, new V3(0.03, 0.04, 0.025), 14, 9306 + sd, 0.02, 0.02, new V3(0, 0, 0), 0, Detail.ALL, hp, YT_W);
            ell(c, ec.add(sd * 0.01, 0, 0.012), new V3(0.016, 0.026, 0.012), 8, 6, flat(YT_PK));
        }
        decal(c, hc, hr, new V3(0, -0.1, 1), 0.22, 0.0, Detail.T_DISC, YT_FC, 0.95, 0.004);
        decal(c, hc, hr, new V3(0.34, 0.06, 0.93), 0.07, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.005);
        decal(c, hc, hr, new V3(-0.34, 0.06, 0.93), 0.07, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.005);
        ell(c, hc.add(0, -0.03, 0.113), new V3(0.02, 0.014, 0.012), 8, 6, flat(hex(0x32466E)));
        decal(c, hc, hr, new V3(0, -0.32, 1), 0.08, 0, T_MOUTH, new float[]{0.35F, 0.15F, 0.25F}, 0.95, 0.006);
        cheeks(c, hc, hr, 0.72, -0.18, 0.7, 0.06);
        for (int sd = -1; sd <= 1; sd += 2) {
            boolean em = c.emissive;
            c.emissive = true;
            cone(c, hc.add(sd * 0.075, 0.1, 0.0), hc.add(sd * 0.095, 0.17, -0.01), 0.02, 0.0, 5, flat(YT_IC));
            c.emissive = em;
        }
        c.head = false;
        // bufanda roja con flecos
        ell(c, bc.add(0, 0.115 + 0.0, 0.05), new V3(0.115, 0.03, 0.1), 16, 8, flat(YT_RD));
        double sw2 = Math.sin(t * 0.12 + ph) * 0.015 + Math.sin(c.gait) * 0.01 * w;
        V3[] sc = {bc.add(-0.06, 0.12, 0.12), bc.add(-0.07 + sw2, 0.04, 0.15), bc.add(-0.065 + sw2 * 1.4, -0.04, 0.145)};
        tube(c, sc, new double[]{0.026, 0.024, 0.022}, 5, flat(YT_RD));
        for (int k = -1; k <= 1; k++) {
            cone(c, sc[2].add(k * 0.014, 0.0, 0), sc[2].add(k * 0.016, -0.04, 0.0), 0.009, 0.004, 4, flat(scale(YT_RD, 0.8F)));
        }
        for (int k = 0; k < 7; k++) {
            double u = ((t * 0.014 + k * 0.14 + ph * 0.1) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.24, 0.55 - u * 0.45, Math.cos(k * 1.7 + u) * 0.22), 0.02, new float[]{0.85F, 0.95F, 1F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 53. REMENDÍN

    private static final float[][] PT = {hex(0xFF9EBC)};
    private static final float[] PT_TH = hex(0x3A2A55);

    private static final float[] PK = hex(0xFFBAD2), PK_IN = hex(0xFF8FB4), MINT = hex(0x8FE0C8), LILAC = hex(0xBBA6F4), SKY = hex(0xA2D0FF), YEL = hex(0xFFDE7A);

    /** Peluche rosa con parches grandes y planos (borde nítido) y costura de puntadas. */
    private static Paint plush(float[] base, double[][] ps, float[][] cols) {
        return (n, p) -> {
            float[] col = mix(base, scale(base, 0.88F), ss(0.1, -0.95, n.y) * 0.7F);
            for (int i = 0; i < ps.length; i++) {
                double d = n.x * ps[i][0] + n.y * ps[i][1] + n.z * ps[i][2];
                double ca = ps[i][3];
                float k = (float) ss(ca - 0.006, ca + 0.006, d);
                if (k > 0F) {
                    float[] pc = mix(cols[i], scale(cols[i], 0.9F), ss(0.1, -0.95, n.y) * 0.7F);
                    col = mix(col, pc, k);
                    if (d > ca + 0.01 && d < ca + 0.06 && Math.sin((p.x * 1.1 + p.y * 1.3 + p.z * 0.9) * 85) > -0.1) {
                        col = mix(col, PT_TH, 0.92F);
                    }
                }
            }
            return col;
        };
    }

    /** Parche de tela en relieve (almohadillado) que sigue la superficie de un elipsoide, con puntadas alrededor. */
    private static void fabric(Ctx c, V3 ce, V3 r, V3 dir, double rho, double rot, double sq, float[] col) {
        V3 d0 = dir.norm();
        V3 t1 = d0.anyPerp().norm();
        V3 t2 = d0.cross(t1).norm();
        int nr = 6, na = 40;
        V3[][] P = new V3[nr + 1][na + 1], N = new V3[nr + 1][na + 1];
        for (int i = 0; i <= nr; i++) {
            double s = i / (double) nr;
            for (int j = 0; j <= na; j++) {
                double phi = 2 * Math.PI * j / na;
                P[i][j] = fabricPt(ce, r, d0, t1, t2, phi, s, rho, rot, sq, 0.0);
                N[i][j] = fabricN(r, d0, t1, t2, phi, s, rho, rot, sq);
            }
        }
        for (int i = 0; i < nr; i++) {
            double s = (i + 0.5) / nr;
            float[] cc = mix(col, scale(col, 0.82F), (float) (s * s * s));
            for (int j = 0; j < na; j++) {
                V3[] p = {P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j]};
                V3[] n = {N[i][j], N[i][j + 1], N[i + 1][j + 1], N[i + 1][j]};
                float[][] b = new float[4][];
                for (int k = 0; k < 4; k++) {
                    b[k] = flat(cc).at(n[k], p[k].sub(ce));
                }
                c.quad(c.blend, c.alpha, T_WHITE, p, n, b, null);
            }
        }
        int nd = 22;
        for (int k = 0; k < nd; k++) {
            double p0 = 2 * Math.PI * k / nd, p1 = p0 + 2 * Math.PI / nd * 0.58;
            V3[] pts = new V3[4];
            for (int m = 0; m < 4; m++) {
                pts[m] = fabricPt(ce, r, d0, t1, t2, p0 + (p1 - p0) * m / 3.0, 0.84, rho, rot, sq, 0.0035);
            }
            tube(c, pts, new double[]{0.0042, 0.0042, 0.0042, 0.0042}, 4, flat(PT_TH));
        }
    }

    private static V3 fabricDir(V3 d0, V3 t1, V3 t2, double phi, double s, double rho, double rot, double sq) {
        double e = Math.pow(Math.abs(Math.cos(phi - rot)), sq) + Math.pow(Math.abs(Math.sin(phi - rot)), sq);
        double rr = s * rho / Math.pow(e, 1.0 / sq);
        return d0.mul(Math.cos(rr)).add(t1.mul(Math.cos(phi)).add(t2.mul(Math.sin(phi))).mul(Math.sin(rr))).norm();
    }

    private static V3 fabricPt(V3 ce, V3 r, V3 d0, V3 t1, V3 t2, double phi, double s, double rho, double rot, double sq, double extra) {
        V3 d = fabricDir(d0, t1, t2, phi, s, rho, rot, sq);
        V3 nn = new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
        double h = 0.0015 + 0.0075 * Math.sqrt(Math.max(0.0, 1 - s * s)) + extra;
        return ce.add(new V3(d.x * r.x, d.y * r.y, d.z * r.z)).add(nn.mul(h));
    }

    private static V3 fabricN(V3 r, V3 d0, V3 t1, V3 t2, double phi, double s, double rho, double rot, double sq) {
        V3 d = fabricDir(d0, t1, t2, phi, s, rho, rot, sq);
        return new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
    }

    private static void ring(Ctx c, V3 ce, double rx, double ry, int n, double len, double z) {
        for (int k = 0; k < n; k++) {
            double a = k * Math.PI * 2 / n;
            V3 q = ce.add(Math.cos(a) * rx, Math.sin(a) * ry, z);
            V3 d = new V3(-Math.sin(a) * rx, Math.cos(a) * ry, 0).norm();
            tube(c, new V3[]{q.sub(d.mul(len)), q.add(d.mul(len))}, new double[]{0.0042, 0.0042}, 4, flat(PT_TH));
        }
    }

    /** Costura de cierre con puntadas en cruz a lo largo de un meridiano (x = 0) de un elipsoide. */
    private static void seam(Ctx c, V3 ce, V3 r, double t0, double t1, int n) {
        V3[] line = new V3[n];
        double[] rr = new double[n];
        for (int i = 0; i < n; i++) {
            double th = t0 + (t1 - t0) * i / (n - 1.0);
            line[i] = ce.add(0, r.y * Math.cos(th) + Math.cos(th) * 0.003, -r.z * Math.sin(th) - Math.sin(th) * 0.003);
            rr[i] = 0.0032;
            V3 along = new V3(0, -r.y * Math.sin(th), -r.z * Math.cos(th)).norm();
            V3 side = new V3(1, 0, 0);
            double L = 0.017;
            V3 a1 = line[i].add(side.mul(-L)).add(along.mul(0.012)), b1 = line[i].add(side.mul(L)).add(along.mul(-0.012));
            V3 a2 = line[i].add(side.mul(-L)).add(along.mul(-0.012)), b2 = line[i].add(side.mul(L)).add(along.mul(0.012));
            tube(c, new V3[]{a1, b1}, new double[]{0.0042, 0.0042}, 4, flat(PT_TH));
            tube(c, new V3[]{a2, b2}, new double[]{0.0042, 0.0042}, 4, flat(PT_TH));
        }
        tube(c, line, rr, 4, flat(PT_TH));
    }

    private static void smile(Ctx c, V3 hc, V3 hr, int sd) {
        V3[] pts = new V3[7];
        for (int i = 0; i < 7; i++) {
            double s = i / 6.0;
            double x = sd * (0.004 + 0.036 * s), y = -0.056 - 0.013 * Math.sin(Math.PI * s * 0.85) + 0.0;
            double zz = hr.z * Math.sqrt(Math.max(0.01, 1 - (x / hr.x) * (x / hr.x) - (y / hr.y) * (y / hr.y))) + 0.002;
            pts[i] = hc.add(x, y, zz);
        }
        double[] rr = new double[7];
        java.util.Arrays.fill(rr, 0.0036);
        tube(c, pts, rr, 4, flat(PT_TH));
    }

    static void patchMonster(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.014 * w + 0.01 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        Paint pb = plush(PK, new double[0][], new float[0][]);
        Paint pl = plush(SKY, new double[0][], new float[0][]);
        Paint pmn = plush(MINT, new double[0][], new float[0][]);
        Paint plc = plush(LILAC, new double[0][], new float[0][]);
        Paint pa = pb;
        Paint phd = pb;
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sp = Math.sin(p) * 0.04 * w, lift = Math.max(0, Math.cos(p)) * 0.03 * w;
            V3 lc = new V3(sd * 0.072, 0.038 + lift, 0.025 + sp);
            ell(c, lc, new V3(0.05, 0.043, 0.065), 16, 11, pl);
            ell(c, lc.add(0, -0.012, 0.032), new V3(0.032, 0.028, 0.03), 10, 7, flat(PK_IN));
        }
        V3 bc = new V3(0, 0.145 + bob, 0.0), br = new V3(0.125, 0.105, 0.108);
        ell(c, bc, br, 48, 34, pb);
        fabric(c, bc, br, new V3(1.0, 0.12, -0.1), 0.78, 0.2, 2.0, MINT);
        fabric(c, bc, br, new V3(-0.3, 0.45, -0.85), 0.62, 0.4, 4.0, LILAC);
        fabric(c, bc, br, new V3(-0.95, -0.2, 0.2), 0.5, 0.0, 2.0, SKY);
        // parche crema con corazón y costura
        V3 pc = bc.add(0, -0.024, 0.093);
        ell(c, pc, new V3(0.07, 0.064, 0.016), 28, 12, flat(hex(0xFFF4E6)));
        ring(c, pc, 0.062, 0.056, 16, 0.0075, 0.0115);
        heart(c, pc.add(0, 0.004, 0.012), 0.03, hex(0xE8456E));
        // bracitos
        for (int sd = -1; sd <= 1; sd += 2) {
            double wv = Math.sin(t * 0.12 + ph + sd * 1.2) * 0.03 + Math.sin(c.gait + sd) * 0.03 * w;
            double wave = sd > 0 ? (0.5 + 0.5 * Math.sin(t * 0.03 + ph)) * (1 - w) * 0.05 + Math.max(0, Math.sin(t * 0.25 + ph)) * 0.012 * (1 - w) : 0;
            V3 ac = new V3(sd * 0.135, 0.165 + bob + wv * 0.4 + wave, 0.035);
            ell(c, ac, new V3(0.04, 0.058, 0.04), 0.0, 0.0, -sd * (0.6 + wv * 3), 16, 12, sd > 0 ? pa : pb);
        }
        // costura de cierre a lo largo de la espalda
        seam(c, bc, br, 0.35, 2.3, 8);
        // algodón que se asoma por una costura del costado
        V3 sm = bc.add(-0.118, 0.0, 0.035);
        tube(c, new V3[]{sm.add(0.002, 0.03, -0.02), sm, sm.add(0.002, -0.03, 0.02)}, new double[]{0.0028, 0.0028, 0.0028}, 4, flat(PT_TH));
        for (int k = 0; k < 3; k++) {
            ell(c, sm.add(-0.012 - k * 0.005, 0.012 * k - 0.01, 0.004 * k), new V3(0.02, 0.018, 0.018), 8, 6, flat(WHITE));
        }
        // colita de pompón
        ell(c, bc.add(Math.sin(t * 0.2 + ph) * 0.012, -0.025, -0.112), new V3(0.042, 0.042, 0.042), 12, 9, flat(hex(0xFFF4F8)));
        // cabeza grande (peluche)
        headLook(c, 0.14, 0.0);
        c.hpv = new V3(0, 0.27, 0.05);
        V3 hc = new V3(0, 0.295 + bob, 0.02), hr = new V3(0.15, 0.13, 0.13);
        ell(c, hc, hr, 56, 40, phd);
        Detail.glint(c, hc, hr, new V3(-0.35, 0.8, 0.5), 0.1, WHITE, 0.3);
        // orejas largas (una doblada)
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Math.sin(t * 0.08 + ph + sd) * 0.06 + Math.max(0, Math.sin(t * 0.05 + ph + sd * 2.0)) * 0.12;
            V3 ec = hc.add(sd * 0.095, 0.15, -0.01);
            double er = -sd * (0.3 + (sd > 0 ? 0.5 : 0.0) + fl);
            ell(c, ec, new V3(0.042, 0.09, 0.024), 0.0, 0.0, er, 18, 14, sd > 0 ? pmn : plc);
            ell(c, ec.add(0, 0, 0.014), new V3(0.026, 0.068, 0.012), 0.0, 0.0, er, 14, 10, flat(PK_IN));
            for (int k = 0; k < 14; k++) {
                double aa = k * Math.PI * 2 / 14;
                double lx = Math.cos(aa) * 0.032, ly = Math.sin(aa) * 0.074;
                double dx = -Math.sin(aa) * 0.032, dy = Math.cos(aa) * 0.074;
                double dl = Math.sqrt(dx * dx + dy * dy);
                dx = dx / dl * 0.0075;
                dy = dy / dl * 0.0075;
                double ce2 = Math.cos(er), se2 = Math.sin(er);
                V3 q = ec.add(lx * ce2 - ly * se2, lx * se2 + ly * ce2, 0.0265);
                V3 dd = new V3(dx * ce2 - dy * se2, dx * se2 + dy * ce2, 0);
                tube(c, new V3[]{q.sub(dd), q.add(dd)}, new double[]{0.004, 0.004}, 4, flat(PT_TH));
            }
        }
        fabric(c, hc, hr, new V3(0.62, 0.74, -0.15), 0.62, 0.3, 4.0, MINT);
        // costura de cierre en la cabeza (de la coronilla a la nuca)
        seam(c, hc, hr, 0.25, 2.0, 10);
        // ojo normal
        decal(c, hc, hr, new V3(0.46, 0.1, 0.88), 0.095, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, WHITE, 1, 0.004);
        // ojo de botón sobre un parche celeste cosido
        V3 bt = hc.add(-0.068, 0.035, 0.118);
        ell(c, bt.add(0, 0, -0.01), new V3(0.056, 0.052, 0.018), 20, 10, flat(SKY));
        ring(c, bt.add(0, 0, -0.01), 0.048, 0.045, 14, 0.0065, 0.0165);
        ell(c, bt.add(0, 0, 0.002), new V3(0.036, 0.036, 0.011), 18, 8, flat(hex(0xF6D86A)));
        ell(c, bt.add(0, 0, 0.009), new V3(0.028, 0.028, 0.005), 16, 6, flat(hex(0xE9BE3C)));
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                ell(c, bt.add(i * 0.0085, j * 0.0085, 0.0135), new V3(0.0042, 0.0042, 0.003), 6, 4, flat(PT_TH));
            }
        }
        tube(c, new V3[]{bt.add(-0.0085, -0.0085, 0.0155), bt.add(0.0085, 0.0085, 0.0155)}, new double[]{0.0018, 0.0018}, 4, flat(PT_TH));
        tube(c, new V3[]{bt.add(-0.0085, 0.0085, 0.0155), bt.add(0.0085, -0.0085, 0.0155)}, new double[]{0.0018, 0.0018}, 4, flat(PT_TH));
        // nariz y sonrisa cosida en "w"
        V3 ns = hc.add(0, -0.033, hr.z * Math.sqrt(1 - 0.0644 * 0.0644 / 0.0169 * 0.0 - (0.033 / 0.13) * (0.033 / 0.13)) + 0.002);
        ell(c, ns, new V3(0.02, 0.015, 0.012), 10, 8, flat(hex(0xE2678C)));
        tube(c, new V3[]{ns.add(0, -0.012, 0.0), ns.add(0, -0.026, 0.0)}, new double[]{0.0022, 0.0022}, 4, flat(PT_TH));
        smile(c, hc, hr, 1);
        smile(c, hc, hr, -1);
        cheeks(c, hc, hr, 0.7, -0.12, 0.6, 0.06);
        // aguja con hilo clavada en la cabeza
        V3 n0 = hc.add(0.09, 0.105, 0.0), n1 = n0.add(0.035, 0.065, 0.0);
        tube(c, new V3[]{n0, n1}, new double[]{0.0035, 0.0028}, 4, flat(hex(0xD8DCE8)));
        double th = Math.sin(t * 0.1 + ph) * 0.012;
        tube(c, new V3[]{n1, n1.add(0.015 + th, -0.02, 0.01), n1.add(0.01 + th * 2, -0.05, 0.02), n1.add(-0.01 + th * 2, -0.075, 0.015)}, new double[]{0.0022, 0.002, 0.002, 0.0018}, 4, flat(hex(0xE8456E)));
        c.head = false;
        // moño de listón bajo la barbilla
        V3 bw = new V3(0, 0.2 + bob, 0.108);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bw.add(sd * 0.03, 0.0, 0.0), new V3(0.03, 0.02, 0.012), 0.0, 0.0, sd * 0.35, 12, 8, flat(hex(0xE8456E)));
            cone(c, bw.add(sd * 0.012, -0.006, 0.004), bw.add(sd * 0.03, -0.05, 0.008), 0.014, 0.004, 4, flat(hex(0xC8305A)));
        }
        ell(c, bw, new V3(0.014, 0.014, 0.014), 8, 6, flat(hex(0xC8305A)));
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.01 + k * 0.33 + ph * 0.1) % 1.0);
            double sc = 0.011 * Math.sin(Math.PI * Math.min(1.0, u * 1.15));
            heart(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.2, 0.3 + u * 0.35, 0.05 + Math.cos(k * 1.7) * 0.12), sc, hex(0xFF6A94));
        }
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.013 + k * 0.33 + ph * 0.2) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 4.3 + u * 3) * 0.24, 0.15 + u * 0.4, Math.cos(k * 2.7) * 0.16), 0.016 * (1.1 - u * 0.5), new float[]{1F, 0.88F, 0.45F}, t, ph + k, 0.35);
        }
    }
}
