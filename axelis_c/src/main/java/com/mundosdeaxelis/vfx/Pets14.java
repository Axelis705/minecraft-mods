package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas nuevas del lote 5: Cofre Mímico (épica) y Grifo Imponente (legendaria).
 */
final class Pets14 {
    private Pets14() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 49. COFRE MÍMICO

    private static final float[] MM_W = hex(0x8A5A2E), MM_W2 = hex(0xB07C44), MM_WD = hex(0x5E3A1C), MM_GOLD = hex(0xF6C84A), MM_GOLD2 = hex(0xFFE890),
            MM_IRON = hex(0x6A6E7C), MM_RED = hex(0x7A1018), MM_RED2 = hex(0x4A0A10), MM_TONG = hex(0xFF7A9A), MM_TOOTH = hex(0xFFF8E8), MM_GEM = hex(0x5AE0FF),
            MM_GEM2 = hex(0xFF5A9A);

    private static float[] wood(int face, int i, int j, int ni, int nj) {
        double v = ((i * 7 + j * 13 + face * 5) % 5) / 5.0;
        float[] base = mix(MM_W, MM_W2, v * 0.7);
        if (j % 2 == 0) {
            base = scale(base, 0.86F);
        }
        // tiras de hierro dorado en los extremos
        if ((face >= 4 || face == 2) && (i == 0 || i == ni - 1) && j % 3 != 1) {
            return (j + i) % 3 == 0 ? MM_GOLD2 : MM_GOLD;
        }
        if (face < 2 && j == nj - 1) {
            return MM_GOLD;
        }
        return base;
    }

    static void mimic(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        // saltitos al andar; al estar quieto respira y de vez en cuando muerde
        double hop = Math.abs(Math.sin(c.gait * 1.2)) * 0.05 * w;
        double chompU = ((t + ph * 40) % 150) / 22.0;
        double chomp = chompU < 1 ? Math.sin(chompU * Math.PI) : 0;
        double open = 0.28 + 0.06 * Math.sin(t * 0.09 + ph) + 0.28 * chomp + 0.22 * w * Math.abs(Math.sin(c.gait * 2.4));
        c.pivot = new V3(0, 0.2, 0);
        double by = 0.1 + hop + 0.006 * c.breath;
        // patitas cortas con garras
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? 0.095 : -0.095;
                double phs = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phs) * 0.05 * w, lift = Math.max(0, Math.cos(phs)) * 0.035 * w;
                V3 hip = new V3(sd * 0.115, 0.1 + hop, z0), fo = new V3(sd * 0.115, 0.03 + lift, z0 + sw);
                tube(c, new V3[]{hip, hip.lerp(fo, 0.5), fo}, new double[]{0.034, 0.03, 0.028}, 6, flat(MM_WD));
                ell(c, fo.add(0, -0.004, 0.02), new V3(0.04, 0.022, 0.05), 8, 5, flat(MM_IRON));
                for (int k = -1; k <= 1; k++) {
                    cone(c, fo.add(k * 0.014, -0.004, 0.05), fo.add(k * 0.016, -0.012, 0.085), 0.008, 0.0, 4, flat(MM_TOOTH));
                }
            }
        }
        // cuerpo del cofre
        V3 bc = new V3(0, by + 0.1, 0);
        Detail.pixBox(c, bc, new V3(0.17, 0.09, 0.14), 7, 4, 6, MimicPix.BODY);
        // interior rojo oscuro, tesoro y lengua
        ell(c, new V3(0, by + 0.185, 0), new V3(0.15, 0.012, 0.12), 12, 4, flat(MM_RED2));
        boolean em = c.emissive;
        for (int k = 0; k < 7; k++) {
            V3 d = Detail.sph(5511, k);
            V3 pt = new V3(d.x * 0.1, by + 0.2 + Math.abs(d.y) * 0.02, d.z * 0.07 - 0.02);
            ell(c, pt, new V3(0.02, 0.008, 0.02), 6, 4, flat(k % 3 == 0 ? MM_GOLD2 : MM_GOLD));
        }
        c.emissive = true;
        ell(c, new V3(-0.05, by + 0.208, -0.045), new V3(0.012, 0.012, 0.012), 6, 4, flat(MM_GEM));
        ell(c, new V3(0.06, by + 0.207, -0.03), new V3(0.011, 0.011, 0.011), 6, 4, flat(MM_GEM2));
        c.emissive = em;
        double tw = Math.sin(t * 0.22 + ph) * 0.02;
        V3[] tg = {new V3(0, by + 0.19, 0.03), new V3(tw, by + 0.205 + 0.01 * chomp, 0.1), new V3(tw * 1.5, by + 0.17 + 0.04 * chomp, 0.175 + 0.03 * chomp)};
        curve(c, tg, new double[]{0.032, 0.03, 0.026}, 4, 7, flat(MM_TONG));
        ell(c, tg[2].add(0, 0, 0.012), new V3(0.032, 0.012, 0.03), 8, 4, flat(scale(MM_TONG, 1.1F)));
        // dientes de abajo
        for (int k = 0; k < 7; k++) {
            double x = -0.135 + k * 0.045;
            V3 b = new V3(x, by + 0.185, 0.128);
            cone(c, b, b.add(0, 0.045 + 0.008 * (k % 2), 0.004), 0.0125, 0.0, 4, flat(MM_TOOTH));
        }
        // tapa: gira sobre las bisagras de atrás
        V3 hinge = new V3(0, by + 0.19, -0.14);
        c.head = true;
        c.hpv = hinge;
        System.arraycopy(rot(0, -open, 0), 0, c.hm, 0, 9);
        V3 lc = hinge.add(0, 0.045, 0.14);
        Detail.pixBox(c, lc, new V3(0.175, 0.045, 0.14), 7, 2, 6, MimicPix.LID);
        ell(c, lc.add(0, 0.04, 0), new V3(0.168, 0.035, 0.132), 12, 5, (n, p) -> mix(MM_W, MM_W2, ss(0.0, 1.0, n.y) * 0.6));
        for (int s = -1; s <= 1; s += 2) {
            V3 gb = lc.add(s * 0.09, 0.045, 0);
            ell(c, gb, new V3(0.012, 0.04, 0.136), 6, 4, flat(MM_GOLD));
        }
        for (int k = 0; k < 7; k++) {
            double x = -0.135 + k * 0.045;
            V3 b = hinge.add(x, 0.0, 0.265);
            cone(c, b, b.add(0, -0.042 - 0.008 * ((k + 1) % 2), -0.004), 0.0125, 0.0, 4, flat(MM_TOOTH));
        }
        // cerradura dorada de nariz y ojos sobre la tapa
        V3 lk = lc.add(0, -0.015, 0.145);
        ell(c, lk, new V3(0.028, 0.034, 0.012), 8, 6, flat(MM_GOLD));
        ell(c, lk.add(0, -0.008, 0.008), new V3(0.008, 0.012, 0.006), 6, 4, flat(BLACK));
        for (int s = -1; s <= 1; s += 2) {
            V3 ec = lc.add(s * 0.075, 0.075, 0.07);
            ell(c, ec, new V3(0.042, 0.048, 0.034), 10, 8, flat(WHITE));
            decal(c, ec, new V3(0.042, 0.048, 0.034), new V3(s * 0.15, 0.0, 1), 0.075, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? BLACK : new float[]{0.95F, 0.85F, 0.3F}, 1, 0.003);
            Detail.glint(c, ec, new V3(0.042, 0.048, 0.034), new V3(s * 0.15 - 0.3, 0.5, 1), 0.016, WHITE, 0.9);
            V3 b0 = ec.add(s * 0.05, 0.05, 0.0), b1 = ec.add(-s * 0.03, 0.036, 0.012);
            tube(c, new V3[]{b0, b0.lerp(b1, 0.5).add(0, 0.006, 0), b1}, new double[]{0.008, 0.01, 0.008}, 4, flat(MM_WD));
        }
        c.head = false;
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.014 + ph * 0.2 + k * 0.33) % 1.0);
            Detail.twinkle(c, new V3((k - 1) * 0.07, by + 0.24 + u * 0.3, 0.0), 0.02 * (1 - u * 0.4), new float[]{1F, 0.9F, 0.4F}, t, ph + k, 0.3);
        }
    }

    private static final class MimicPix {
        static final Detail.Pix BODY = Pets14::wood;
        static final Detail.Pix LID = (f, i, j, ni, nj) -> wood(f, i, j + 1, ni, nj + 1);
    }

    // ------------------------------------------------------------------ 50. GRIFO IMPONENTE

    private static final float[] GR_B = hex(0xC88A3A), GR_B2 = hex(0xEDB866), GR_W = hex(0xFFF9EE), GR_W2 = hex(0xE6D8BE), GR_BK = hex(0xF6C040), GR_BK2 = hex(0xC8881E),
            GR_WG = hex(0x7A4E2C), GR_WG2 = hex(0xF4D8A0), GR_TIP = hex(0x3A2A24), GR_ARM = hex(0xF6C84A), GR_GEM = hex(0x4AB8FF), GR_EYE = hex(0xFFC030);

    static void royalGriffin(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hov = Math.sin(t * 0.07 + ph) * 0.025;
        c.pivot = new V3(0, 0.45, 0);
        double flap = Math.sin(t * 0.17 + ph) * 0.45 + 0.1 + w * Math.sin(t * 0.34 + ph) * 0.15;
        // alas grandes: dos capas de plumas, abiertas y batiendo
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 root = new V3(sd * 0.1, 0.55 + hov, -0.03);
            V3 wrist = new V3(sd * 0.36, 0.64 + hov + flap * 0.32, -0.06);
            tube(c, new V3[]{root, root.lerp(wrist, 0.5).add(0, 0.015, 0), wrist}, new double[]{0.05, 0.036, 0.028}, 6, flat(GR_B));
            for (int k = 0; k < 10; k++) {
                double u = k / 9.0;
                V3 base = root.lerp(wrist, 0.25 + 0.75 * u);
                double len = 0.2 + 0.2 * (1 - Math.abs(u - 0.8) * 1.2);
                double spread = 0.55 + 0.45 * u;
                V3 tip = base.add(sd * len * spread, -0.02 - 0.1 * (1 - u) + flap * 0.16 * (0.4 + u) - 0.05, -0.05 - 0.07 * u - (1 - u) * 0.08);
                Detail.feather(c, base, tip, new V3(0, 0.25, 1).norm(), 0.075, new V3(0, 1, -0.25).norm(), k > 6 ? GR_WG : GR_WG2, k > 6 ? GR_TIP : GR_WG);
            }
            for (int k = 0; k < 6; k++) {
                double u = k / 5.0;
                V3 base = root.lerp(wrist, 0.15 + 0.7 * u);
                V3 tip = base.add(sd * (0.16 + 0.04 * u), -0.01 + flap * 0.07 - 0.04, -0.07 - 0.03 * u);
                Detail.feather(c, base.add(0, 0.015, 0), tip.add(0, 0.015, 0), new V3(0, 0.25, 1).norm(), 0.06, new V3(0, 1, -0.25).norm(), GR_B2, GR_B);
            }
        }
        // cola de león con mechón
        double sw = Math.sin(t * 0.11 + ph) * 0.05;
        V3[] tcp = {new V3(0, 0.45 + hov, -0.24), new V3(sw * 0.5, 0.43 + hov, -0.36), new V3(sw, 0.4 + hov, -0.48), new V3(sw * 1.2, 0.41 + hov, -0.56)};
        curve(c, tcp, new double[]{0.034, 0.028, 0.022, 0.018}, 4, 6, flat(GR_B));
        for (int k = 0; k < 5; k++) {
            V3 b = tcp[3];
            Detail.feather(c, b, b.add((k - 2) * 0.025 + sw, -0.02 - Math.abs(k - 2) * 0.01, -0.12), new V3(1, 0, 0), 0.03, new V3(0, 1, 0), GR_B2, GR_W);
        }
        // patas traseras de león
        for (int sd = -1; sd <= 1; sd += 2) {
            double sp = Math.sin(t * 0.17 + ph + (sd > 0 ? 0.6 : 0)) * 0.015;
            V3 hip = new V3(sd * 0.09, 0.42 + hov, -0.16), kn = new V3(sd * 0.1, 0.33 + hov + sp, -0.2), ft = new V3(sd * 0.1, 0.26 + hov + sp, -0.17);
            tube(c, new V3[]{hip, kn, ft}, new double[]{0.055, 0.04, 0.03}, 6, flat(GR_B));
            ell(c, ft.add(0, -0.01, 0.03), new V3(0.04, 0.026, 0.055), 8, 5, flat(GR_B2));
            for (int k = -1; k <= 1; k++) {
                cone(c, ft.add(k * 0.014, -0.012, 0.075), ft.add(k * 0.016, -0.026, 0.108), 0.009, 0.0, 4, flat(GR_BK2));
            }
        }
        // cuerpo de león dorado y pecho de plumas blancas
        V3 bc = new V3(0, 0.46 + hov, -0.02), br = new V3(0.135, 0.12, 0.24);
        Paint bp = (n, p) -> mix(GR_B, GR_B2, ss(0.1, 0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 70, 5101, 0.035, 0.018, new V3(0, -0.5, -1), 0.8, d -> d.z < 0.3 ? 1 : 0.0, bp, GR_B2);
        V3 ch = bc.add(0, -0.01, 0.16);
        ell(c, ch, new V3(0.12, 0.115, 0.1), 12, 8, flat(GR_W));
        Detail.plumage(c, ch, new V3(0.12, 0.115, 0.1), 5, 9, 0.05, 0.035, d -> d.z > 0.1 ? 1 : 0.0, (n, p) -> mix(GR_W, GR_W2, ss(0.5, -0.8, n.y)), GR_W2);
        // patas delanteras de águila con garras
        for (int sd = -1; sd <= 1; sd += 2) {
            double sp = Math.sin(t * 0.17 + ph + (sd > 0 ? 0 : 0.8)) * 0.02;
            V3 sh = new V3(sd * 0.085, 0.42 + hov, 0.15), kn = new V3(sd * 0.095, 0.35 + hov + sp, 0.2), ft = new V3(sd * 0.095, 0.29 + hov + sp * 1.5, 0.19);
            tube(c, new V3[]{sh, kn, ft}, new double[]{0.042, 0.03, 0.02}, 6, flat(GR_W));
            for (int k = -1; k <= 1; k++) {
                V3 b = ft.add(k * 0.013, 0, 0.0);
                tube(c, new V3[]{b, b.add(0.0, -0.014, 0.034), b.add(0.0, -0.045, 0.045)}, new double[]{0.0095, 0.0085, 0.0}, 4, flat(GR_BK));
            }
        }
        // cabeza de águila altiva
        headLook(c, 0.10, -0.05);
        c.hpv = new V3(0, 0.56, 0.2);
        V3 hc = new V3(0, 0.63 + hov, 0.25), hr = new V3(0.095, 0.1, 0.105);
        Paint hp = (n, p) -> mix(GR_W, GR_W2, ss(0.5, -0.9, n.y) * 0.6);
        tube(c, new V3[]{new V3(0, 0.5 + hov, 0.14), new V3(0, 0.57 + hov, 0.19), new V3(0, 0.6 + hov, 0.23)}, new double[]{0.085, 0.075, 0.07}, 7, hp);
        ell(c, hc, hr, 14, 10, hp);
        Detail.plumage(c, hc, hr, 4, 9, 0.032, 0.026, d -> d.z < 0.2 ? 1 : 0.0, hp, GR_W2);
        // pico ganchudo
        V3[] bk = {hc.add(0, -0.01, 0.085), hc.add(0, -0.005, 0.15), hc.add(0, -0.025, 0.2), hc.add(0, -0.07, 0.212)};
        curve(c, bk, new double[]{0.046, 0.038, 0.026, 0.006}, 4, 7, (n, p) -> mix(GR_BK2, GR_BK, ss(-0.5, 0.8, n.y)));
        ell(c, hc.add(0, -0.052, 0.14), new V3(0.03, 0.014, 0.05), 8, 4, flat(GR_BK2));
        for (int sdn = -1; sdn <= 1; sdn += 2) {
            ell(c, hc.add(sdn * 0.014, 0.004, 0.16), new V3(0.007, 0.007, 0.006), 4, 3, flat(GR_TIP));
        }
        // ojos de rapaz dorados con ceja severa
        for (int s = -1; s <= 1; s += 2) {
            V3 ec = hc.add(s * 0.058, 0.025, 0.07);
            ell(c, ec, new V3(0.026, 0.026, 0.016), 8, 6, flat(GR_EYE));
            ell(c, ec.add(s * 0.0, 0, 0.01), new V3(0.012, 0.014, 0.008), 6, 4, flat(BLACK));
            Detail.glint(c, ec.add(0, 0, 0.01), new V3(0.012, 0.014, 0.008), new V3(-0.3, 0.5, 1), 0.01, WHITE, 0.9);
            V3 b0 = hc.add(s * 0.085, 0.058, 0.04), b1 = hc.add(s * 0.025, 0.042, 0.095);
            tube(c, new V3[]{b0, b0.lerp(b1, 0.5).add(0, 0.006, 0), b1}, new double[]{0.011, 0.013, 0.01}, 5, flat(GR_B));
        }
        // cresta de plumas hacia atrás
        double cf = Math.sin(t * 0.12 + ph) * 0.01 + w * 0.03;
        for (int k = 0; k < 7; k++) {
            double a = -0.9 + k * 0.3;
            V3 base = hc.add(Math.sin(a) * 0.06, 0.08, -0.04);
            Detail.feather(c, base, base.add(Math.sin(a) * 0.07, 0.04 - Math.abs(a) * 0.03, -0.16 - cf * 2 - 0.03 * Math.cos(a)), new V3(0, 1, 0), 0.026, new V3(Math.sin(a), 0.3, -0.5), GR_B2, GR_B);
        }
        // peto de oro con gema
        V3 cl = hc.add(0, -0.1, -0.03);
        ell(c, cl, new V3(0.1, 0.025, 0.085), 12, 4, flat(GR_ARM));
        c.head = false;
        boolean em = c.emissive;
        c.emissive = true;
        ell(c, new V3(0, 0.505 + hov, 0.255), new V3(0.016, 0.02, 0.01), 6, 5, flat(GR_GEM));
        c.emissive = em;
        glow(c, new V3(0, 0.505 + hov, 0.26), 0.12, new float[]{0.4F, 0.75F, 1F}, 0.4);
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.012 + ph * 0.2 + k * 0.25) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.3 + u * 3) * 0.45, 0.4 + u * 0.4, Math.cos(k * 1.7) * 0.25 - 0.1), 0.022 * (1 - u * 0.4), new float[]{1F, 0.9F, 0.55F}, t, ph + k, 0.25);
        }
    }
}
