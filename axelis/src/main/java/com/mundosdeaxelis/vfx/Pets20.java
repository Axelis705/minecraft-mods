package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas de los jefes 3 y 5: Lobito Ártico (del Lobo Ártico) y Archimaguito (del Archimago). Solo caen de ellos; no salen en
 * cajas. Son el jefe en chiquito, como el Brotecito Rey y el Gran Herrerito.
 */
final class Pets20 {
    private Pets20() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    // ------------------------------------------------------------------ 65. LOBITO ÁRTICO

    private static final float[] FP_W = hex(0xF4F9FF), FP_B = hex(0xB4CFEE), FP_HD = hex(0x1E1C28), FP_HD2 = hex(0x3C3852), FP_IC = hex(0x9AEAFF),
            FP_IC2 = hex(0xE2FBFF), FP_PAD = hex(0x6F8CBA), FP_NOSE = hex(0x2A2838), FP_PK = hex(0xFFB4C8), FP_BONE = hex(0xEDE6D6),
            FP_GRIP = hex(0x4A3E5E), FP_BELT = hex(0x3A3044), FP_EYE = hex(0xB8E4FF);

    static void frostPup(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.02 * w + 0.008 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        // patitas blancas con almohadillas azules
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait + (sd > 0 ? Math.PI : 0);
            double sw = Math.sin(p) * 0.045 * w, lift = Math.max(0, Math.cos(p)) * 0.035 * w;
            V3 fc = new V3(sd * 0.07, 0.034 + lift, 0.03 + sw);
            ell(c, fc, new V3(0.055, 0.034, 0.075), 14, 9, (n, q) -> mix(FP_PAD, FP_W, ss(-0.5, 0.4, n.y)));
            for (int k = -1; k <= 1; k++) {
                ell(c, fc.add(k * 0.024, -0.004, 0.066), new V3(0.015, 0.014, 0.016), 6, 5, flat(FP_W));
            }
        }
        // cola esponjosa que se menea
        double ts = Math.sin(t * 0.14 + ph) * 0.05 + Math.sin(c.gait) * 0.03 * w;
        V3[] tcp = {new V3(0, 0.1 + bob, -0.13), new V3(ts * 0.4, 0.12 + bob, -0.22), new V3(ts, 0.19 + bob, -0.27), new V3(ts * 1.2, 0.26 + bob, -0.26)};
        double[] trr = {0.026, 0.042, 0.046, 0.028};
        Paint tp = (n, q) -> mix(FP_B, FP_W, ss(-0.2, 0.9, n.y + n.z * 0.3));
        curve(c, tcp, trr, 4, 10, tp);
        Detail.furTube(c, tcp, trr, 60, 9501, 0.02, 0.018, 0.5, tp, FP_W);
        // capa negra a la espalda que ondea un poco, con el borde rasgado
        double cw = Math.sin(t * 0.1 + ph) * 0.02 + 0.04 * w;
        Paint capeP = (n, q) -> mix(FP_HD, FP_HD2, ss(-0.2, 0.9, n.y) * 0.5F);
        V3 cc = new V3(0, 0.2 + bob, -0.128 - cw * 0.5);
        ell(c, cc, new V3(0.118, 0.118, 0.016), 0.0, -0.12 - cw, 0.0, 16, 10, capeP);
        for (int k = -2; k <= 2; k++) {
            V3 b0 = cc.add(k * 0.045, -0.095 + Math.abs(k) * 0.012, -0.012 - cw * 0.3);
            cone(c, b0, b0.add(k * 0.006, -0.05 + Math.abs(k) * 0.008, -0.012 - cw * 0.4), 0.024, 0.0, 5, capeP);
        }
        ell(c, new V3(0, 0.27 + bob, -0.005), new V3(0.1, 0.03, 0.095), 14, 6, capeP);
        // cuerpo de peluche blanco-azulado
        V3 bc = new V3(0, 0.19 + bob, 0.0), br = new V3(0.145, 0.13, 0.135);
        Paint bp = (n, p) -> mix(FP_W, FP_B, ss(0.1, -0.95, n.y) * 0.65F);
        ell(c, bc, br, 24, 16, bp);
        Detail.fur(c, bc, br, 380, 9502, 0.024, 0.026, new V3(0, -0.7, -0.3), 0.6, Detail.ALL, bp, FP_W);
        decal(c, bc, br, new V3(0, -0.1, 1), 0.2, 0.0, Detail.T_DISC, FP_IC2, 0.55, 0.004);
        // cinturón con una calaverita colgando (como las del Lobo)
        ell(c, bc.add(0, -0.055, 0), new V3(0.142, 0.016, 0.133), 18, 4, flat(FP_BELT));
        V3 sk = bc.add(0.06, -0.085, 0.118);
        tube(c, new V3[]{bc.add(0.055, -0.06, 0.13), sk.add(0, 0.02, 0)}, new double[]{0.004, 0.004}, 4, flat(hex(0x9AA0B4)));
        ell(c, sk, new V3(0.024, 0.022, 0.018), 8, 6, flat(FP_BONE));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, sk.add(sd * 0.009, 0.003, 0.016), new V3(0.006, 0.007, 0.003), 5, 3, flat(FP_HD));
        }
        // cristales de hielo en los hombros
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 b0 = bc.add(sd * (0.1 + k * 0.016), 0.09 - k * 0.012, -0.02 - k * 0.025);
                boolean em = c.emissive;
                c.emissive = true;
                cone(c, b0, b0.add(sd * 0.03, 0.05 - k * 0.008, -0.02), 0.016, 0.0, 5, flat(k == 0 ? FP_IC2 : FP_IC));
                c.emissive = em;
            }
        }
        // guadañita de escarcha cruzada en la espalda
        V3 g0 = bc.add(-0.13, -0.06, -0.15), g1 = bc.add(0.12, 0.26, -0.16);
        tube(c, new V3[]{g0, g0.lerp(g1, 0.5), g1}, new double[]{0.011, 0.011, 0.01}, 6, flat(FP_GRIP));
        ell(c, g0.lerp(g1, 0.82), new V3(0.016, 0.016, 0.016), 6, 4, flat(FP_IC));
        boolean em0 = c.emissive;
        c.emissive = true;
        V3[] blade = {g1, g1.add(0.05, 0.035, 0.0), g1.add(0.11, 0.02, 0.0), g1.add(0.15, -0.035, 0.0), g1.add(0.16, -0.08, 0.0)};
        curve(c, blade, new double[]{0.02, 0.022, 0.018, 0.011, 0.003}, 3, 6, (n, q) -> mix(FP_IC, FP_IC2, ss(-0.3, 0.8, n.y)));
        c.emissive = em0;
        glow(c, g1.add(0.09, 0.0, 0.0), 0.16, new float[]{0.6F, 0.9F, 1F}, 0.35);
        // bracitos con patas azules
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(c.gait + sd) * 0.025 * w + (sd > 0 ? Math.sin(t * 0.15 + ph) * 0.01 : 0);
            V3 sh = new V3(sd * 0.13, 0.24 + bob, 0.02), hd = new V3(sd * 0.19, 0.14 + bob + sw, 0.09);
            V3 mid = sh.lerp(hd, 0.5);
            ell(c, mid, new V3(0.045, 0.068, 0.045), 0.0, 0.0, sd * -0.45, 12, 8, bp);
            Detail.fur(c, mid, new V3(0.045, 0.068, 0.045), 30, 9503 + sd, 0.022, 0.022, new V3(0, -1, 0), 0.5, Detail.ALL, bp, FP_W);
            ell(c, hd, new V3(0.045, 0.042, 0.045), 12, 8, (n, q) -> mix(FP_PAD, FP_W, ss(-0.3, 0.6, n.y)));
        }
        // cabeza: carita de lobo dentro de la capucha
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, 0.29, 0.04);
        V3 hc = new V3(0, 0.35 + bob, 0.04), hr = new V3(0.13, 0.112, 0.112);
        Paint hp = (n, p) -> mix(FP_W, FP_B, ss(0.0, -0.95, n.y) * 0.45F);
        ell(c, hc, hr, 22, 15, hp);
        Detail.fur(c, hc, hr, 60, 9504, 0.02, 0.022, new V3(0, -0.4, -1), 0.6, Detail.ALL, hp, FP_W);
        // hocico de lobo con nariz negra
        V3 sn = hc.add(0, -0.042, 0.1);
        ell(c, sn, new V3(0.05, 0.036, 0.062), 12, 8, (n, q) -> mix(FP_W, FP_IC2, ss(-0.6, 0.2, n.y) * 0.4F));
        ell(c, sn.add(0, 0.02, 0.058), new V3(0.02, 0.014, 0.013), 8, 6, flat(FP_NOSE));
        decal(c, sn, new V3(0.05, 0.036, 0.062), new V3(0, -0.6, 0.8), 0.045, 0, T_MOUTH, new float[]{0.35F, 0.15F, 0.25F}, 0.95, 0.004);
        // ojos con brillo helado
        decal(c, hc, hr, new V3(0.42, 0.2, 0.88), 0.066, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, FP_EYE, 1, 0.005);
        decal(c, hc, hr, new V3(-0.42, 0.2, 0.88), 0.066, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, FP_EYE, 1, 0.005);
        cheeks(c, hc, hr, 0.74, -0.2, 0.66, 0.055);
        // capucha negra con orejas puntiagudas
        Detail.cap(c, hc.add(0, 0.01, -0.018), new V3(hr.x * 1.12, hr.y * 1.14, hr.z * 1.12), 0.0, -1.1, 0.0, 1.8, 0.025, 20, 9,
                (n, q) -> mix(FP_HD, FP_HD2, ss(0.2, 1.0, n.y) * 0.6F));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 e0 = hc.add(sd * 0.072, 0.085, -0.035), e1 = hc.add(sd * 0.1, 0.19, -0.045);
            cone(c, e0, e1, 0.04, 0.0, 8, flat(FP_HD));
            cone(c, e0.add(0, 0.008, 0.022), e1.add(0, -0.035, 0.014), 0.02, 0.0, 6, flat(FP_PK));
        }
        // flequillo blanco que asoma de la capucha
        for (int k = -2; k <= 2; k++) {
            V3 b0 = hc.add(k * 0.026, 0.08 - Math.abs(k) * 0.01, 0.075);
            cone(c, b0, b0.add(k * 0.006, -0.026, 0.024), 0.015, 0.0, 5, flat(FP_W));
        }
        c.head = false;
        // copitos de nieve a su alrededor
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.012 + k * 0.17 + ph * 0.1) % 1.0);
            Detail.twinkle(c, new V3(Math.sin(k * 2.1 + u * 3) * 0.24, 0.52 - u * 0.42, Math.cos(k * 1.9 + u) * 0.21), 0.02,
                    new float[]{0.8F, 0.94F, 1F}, t, ph + k, 0.3);
        }
    }

    // ------------------------------------------------------------------ 66. ARCHIMAGUITO

    private static final float[] AM_RB = hex(0x3C2F8E), AM_RB2 = hex(0x5C4ABC), AM_RBD = hex(0x261D5E), AM_GD = hex(0xE6B648), AM_GD2 = hex(0xFFE28C),
            AM_BD = hex(0xF4F4F8), AM_BD2 = hex(0xC6CAD8), AM_SK = hex(0xDAD3E6), AM_EYE = hex(0x8AF4FF), AM_WD = hex(0x6B4A30), AM_CR = hex(0x8AF0FF),
            AM_CR2 = hex(0xE0FFFF), AM_PK = hex(0xFF9AE0), AM_BK = hex(0x7A2E44), AM_PG = hex(0xFFF2D6);

    static void littleArchmage(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        // flota un poco sobre el suelo (no tiene pies: se desliza)
        double fl = 0.06 + 0.025 * Math.sin(t * 0.09 + ph) + 0.006 * c.breath;
        c.pivot = new V3(0, 0.25, 0);
        double hem = Math.sin(t * 0.13 + ph) * 0.012 + 0.03 * w;
        // brillo mágico bajo la túnica
        glow(c, new V3(0, fl + 0.01, 0), 0.26, new float[]{0.55F, 0.5F, 1F}, 0.35);
        // túnica larga (de ancha abajo a estrecha arriba) con ribete dorado
        Paint rp = (n, p) -> mix(AM_RBD, AM_RB2, ss(-0.4, 0.9, n.x * 0.35 + n.z * 0.6 + n.y * 0.3) * 0.75F);
        V3[] rob = {new V3(hem * 0.3, fl + 0.02, -hem), new V3(0, fl + 0.1, 0), new V3(0, fl + 0.2, 0), new V3(0, fl + 0.29, 0.005)};
        tube(c, rob, new double[]{0.15, 0.135, 0.11, 0.085}, 16, rp);
        ell(c, rob[0], new V3(0.15, 0.022, 0.15), 18, 4, flat(AM_RBD));
        ell(c, rob[0].add(0, 0.012, 0), new V3(0.155, 0.014, 0.155), 18, 3, flat(AM_GD));
        // franja dorada al frente y estrellitas
        tube(c, new V3[]{new V3(0, fl + 0.03, 0.148), new V3(0, fl + 0.16, 0.124), new V3(0, fl + 0.28, 0.09)},
                new double[]{0.016, 0.014, 0.012}, 5, flat(AM_GD));
        for (int k = 0; k < 6; k++) {
            double a = 0.5 + k * 1.0;
            double y = fl + 0.06 + (k % 3) * 0.07;
            double rr = 0.15 - (y - fl) * 0.22;
            boolean em = c.emissive;
            c.emissive = true;
            ell(c, new V3(Math.sin(a) * rr, y, Math.cos(a) * rr), new V3(0.011, 0.011, 0.011), 5, 3, flat(AM_GD2));
            c.emissive = em;
        }
        // mangas anchas y manitas
        V3 staffHand = null;
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = sd > 0 ? Math.sin(t * 0.11 + ph) * 0.012 : Math.sin(t * 0.13 + ph + 1) * 0.015;
            V3 sh = new V3(sd * 0.085, fl + 0.27, 0.0);
            V3 hd = sd > 0 ? new V3(0.16, fl + 0.2 + sw, 0.08) : new V3(-0.15, fl + 0.22 + sw, 0.09);
            tube(c, new V3[]{sh, sh.lerp(hd, 0.55), hd}, new double[]{0.04, 0.045, 0.05}, 10, rp);
            ell(c, hd.add(0, 0, 0.012), new V3(0.052, 0.018, 0.052), 0.0, 0.6, 0.0, 12, 4, flat(AM_GD));
            ell(c, hd.add(0, -0.01, 0.03), new V3(0.03, 0.03, 0.03), 10, 7, flat(AM_SK));
            if (sd > 0) {
                staffHand = hd.add(0, -0.01, 0.03);
            }
        }
        // bastón de madera con cristal brillante y garra dorada
        double sb = Math.sin(t * 0.08 + ph) * 0.01;
        V3 st0 = staffHand.add(0.005, -0.17, -0.01), st1 = staffHand.add(0.0, 0.2 + sb, -0.015);
        tube(c, new V3[]{st0, staffHand, st1}, new double[]{0.011, 0.012, 0.012}, 6, flat(AM_WD));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + 0.4;
            cone(c, st1, st1.add(Math.cos(a) * 0.03, 0.045, Math.sin(a) * 0.03), 0.008, 0.002, 4, flat(AM_GD));
        }
        boolean em1 = c.emissive;
        c.emissive = true;
        double pulse = 1 + 0.12 * Math.sin(t * 0.15 + ph);
        V3 cr = st1.add(0, 0.055, 0);
        cone(c, cr, cr.add(0, 0.05 * pulse, 0), 0.026, 0.0, 6, flat(AM_CR2));
        cone(c, cr, cr.add(0, -0.03, 0), 0.026, 0.0, 6, flat(AM_CR));
        c.emissive = em1;
        glow(c, cr.add(0, 0.01, 0), 0.2 * pulse, new float[]{0.55F, 0.95F, 1F}, 0.55);
        // librito que flota a su izquierda y pasa las hojas
        double bb = Math.sin(t * 0.1 + ph + 2) * 0.02;
        V3 bk = new V3(-0.25, fl + 0.32 + bb, 0.06);
        double flap = 0.35 + 0.1 * Math.sin(t * 0.2 + ph);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bk.add(sd * 0.042, 0, 0), new V3(0.045, 0.008, 0.058), 0.0, 0.0, -sd * flap, 8, 3, flat(AM_BK));
            ell(c, bk.add(sd * 0.04, 0.008, 0), new V3(0.04, 0.007, 0.052), 0.0, 0.0, -sd * flap, 8, 3, flat(AM_PG));
        }
        for (int k = 0; k < 3; k++) {
            double u = ((t * 0.02 + k * 0.33 + ph * 0.1) % 1.0);
            Detail.twinkle(c, bk.add(Math.sin(k * 2.4 + u * 5) * 0.04, 0.02 + u * 0.12, Math.cos(k * 1.8) * 0.03), 0.018,
                    new float[]{0.8F, 0.75F, 1F}, t, ph + k, 0.45);
        }
        // cabeza: carita pálida, ojos que brillan y barba blanca enorme
        headLook(c, 0.1, 0.0);
        c.hpv = new V3(0, fl + 0.32, 0.03);
        V3 hc = new V3(0, fl + 0.38, 0.03), hr = new V3(0.1, 0.092, 0.095);
        ell(c, hc, hr, 20, 14, (n, p) -> mix(AM_SK, scale(AM_SK, 0.86), ss(0.1, -0.9, n.y) * 0.6F));
        decal(c, hc, hr, new V3(0.36, 0.12, 0.92), 0.068, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, AM_EYE, 1, 0.005);
        decal(c, hc, hr, new V3(-0.36, 0.12, 0.92), 0.068, 0.0, c.shut ? T_EYE_SHUT : T_BEAD, AM_EYE, 1, 0.005);
        if (!c.shut) {
            boolean em = c.emissive;
            c.emissive = true;
            for (int sd = -1; sd <= 1; sd += 2) {
                ell(c, surf(hc, hr, new V3(sd * 0.36, 0.12, 0.92)).add(sd * 0.004, 0.006, 0.006), new V3(0.009, 0.009, 0.004), 5, 3, flat(AM_CR2));
            }
            c.emissive = em;
        }
        cheeks(c, hc, hr, 0.7, -0.15, 0.7, 0.05);
        // cejas blancas pobladas
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(hc, hr, new V3(sd * 0.38, 0.42, 0.82)).add(0, 0.004, 0.004), new V3(0.03, 0.011, 0.012), 0.0, 0.0, -sd * 0.25, 8, 5,
                    flat(AM_BD));
        }
        // barba en tres mechones y bigote
        Paint bdp = (n, p) -> mix(AM_BD, AM_BD2, ss(0.2, -0.9, n.y) * 0.5F);
        V3 bd = hc.add(0, -0.075, 0.055);
        ell(c, bd, new V3(0.085, 0.07, 0.055), 16, 10, bdp);
        double bsw = Math.sin(t * 0.09 + ph) * 0.01 + 0.02 * w;
        for (int k = -1; k <= 1; k++) {
            V3 b0 = bd.add(k * 0.04, -0.03, 0.01 - Math.abs(k) * 0.01);
            cone(c, b0, b0.add(k * 0.012 + bsw * 0.5, -0.1 + Math.abs(k) * 0.03, 0.015 - bsw), 0.035, 0.0, 8, bdp);
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.03, -0.028, 0.092), new V3(0.032, 0.013, 0.014), 0.0, 0.0, sd * 0.3, 8, 5, flat(AM_BD));
        }
        // sombrero de mago enorme con ala, banda dorada, estrellas y punta doblada
        V3 hb = hc.add(0, 0.06, -0.01);
        ell(c, hb, new V3(0.17, 0.014, 0.17), 22, 3, (n, p) -> mix(AM_RBD, AM_RB, ss(-0.5, 0.9, n.y)));
        Paint hatP = (n, p) -> mix(AM_RB, AM_RB2, ss(-0.4, 0.8, n.x * 0.4 + n.z * 0.6) * 0.6F);
        cone(c, hb.add(0, 0.005, 0), hb.add(0, 0.15, -0.025), 0.1, 0.05, 16, hatP);
        cone(c, hb.add(0, 0.15, -0.025), hb.add(0.02, 0.24, -0.09), 0.05, 0.016, 12, hatP);
        cone(c, hb.add(0.02, 0.24, -0.09), hb.add(0.06, 0.25, -0.15), 0.016, 0.004, 8, hatP);
        ell(c, hb.add(0, 0.02, 0), new V3(0.102, 0.017, 0.102), 18, 3, flat(AM_GD));
        boolean em2 = c.emissive;
        c.emissive = true;
        ell(c, hb.add(0, 0.022, 0.1), new V3(0.016, 0.016, 0.006), 6, 3, flat(AM_CR2));
        for (int k = 0; k < 4; k++) {
            double a = -0.9 + k * 0.6;
            ell(c, hb.add(Math.sin(a) * 0.07, 0.07 + (k % 2) * 0.04, 0.065 * Math.cos(a) - 0.01), new V3(0.012, 0.012, 0.005), 5, 2, flat(AM_GD2));
        }
        c.emissive = em2;
        glowStar(c, hb.add(0.06, 0.25, -0.15), 0.06, new float[]{0.8F, 0.65F, 1F}, t, ph);
        c.head = false;
        // tres cristalitos que lo rodean (como los del Archimago)
        for (int k = 0; k < 3; k++) {
            double a = t * 0.05 + ph + k * Math.PI * 2 / 3;
            V3 cp = new V3(Math.sin(a) * 0.25, fl + 0.24 + Math.sin(t * 0.12 + k * 2) * 0.03, Math.cos(a) * 0.22);
            float[] col = k == 1 ? AM_PK : AM_CR;
            boolean em = c.emissive;
            c.emissive = true;
            cone(c, cp, cp.add(0, 0.035, 0), 0.014, 0.0, 5, flat(col));
            cone(c, cp, cp.add(0, -0.028, 0), 0.014, 0.0, 5, flat(scale(col, 0.85F)));
            c.emissive = em;
            glow(c, cp, 0.09, k == 1 ? new float[]{1F, 0.6F, 0.9F} : new float[]{0.55F, 0.95F, 1F}, 0.4);
        }
    }
}
