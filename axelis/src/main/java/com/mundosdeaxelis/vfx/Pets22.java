package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas nuevas (etapa 252), originales: Gatito Cometa, Capibara Zen, Pulpo Astronauta y Dragoncito de Cristal.
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0, altura total ≈ 1.
 */
final class Pets22 {
    private Pets22() {
    }

    /** Estrella plana de cinco puntas (con un poco de grosor), mirando hacia {@code nrm}. */
    static void star(Ctx c, V3 at, V3 nrm, V3 up, double r, double spin, float[] col, float[] col2) {
        V3 n = nrm.norm();
        V3 u = up.sub(n.mul(up.dot(n))).norm();
        V3 rt = n.cross(u).norm();
        V3[] pts = new V3[10];
        for (int i = 0; i < 10; i++) {
            double a = spin + Math.PI * i / 5.0;
            double rr = i % 2 == 0 ? r : r * 0.45;
            pts[i] = at.add(u.mul(Math.cos(a) * rr)).add(rt.mul(Math.sin(a) * rr));
        }
        for (int side = -1; side <= 1; side += 2) {
            V3 ce = at.add(n.mul(side * r * 0.18));
            V3 nn = n.mul(side);
            for (int i = 0; i < 10; i += 2) {
                V3[] p = {ce, pts[(i + 9) % 10], pts[i], pts[i + 1]};
                V3[] ns = {nn, nn, nn, nn};
                c.quad(c.blend, c.alpha, T_WHITE, p, ns, new float[][]{col2, col, col, col}, null);
            }
        }
    }

    // ------------------------------------------------------------------ GATITO COMETA

    private static final float[] CC_W = {0.98F, 0.98F, 1.0F}, CC_W2 = {0.86F, 0.88F, 0.98F}, CC_IN = {1F, 0.72F, 0.80F},
            CC_BLUE = {0.42F, 0.62F, 1F}, CC_STAR = {1F, 0.90F, 0.42F}, CC_TRAIL = {0.62F, 0.80F, 1F};

    /**
     * Gatito Cometa: un gatito blanco y esponjoso que vuela "sentado", con la cola terminada en una estrella brillante y una
     * estela de luz detrás. Collar azul con una estrellita.
     */
    static void cometCat(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.15 + ph) * 0.04;
        c.pivot = new V3(0, 0.42, 0);
        double y = 0.36 + bob;
        Paint fur = (n, p) -> mix(CC_W2, CC_W, ss(-0.6, 0.6, n.y));
        // estela de cometa: brillos que se quedan atrás y se apagan
        c.emissive = true;
        for (int k = 0; k < 9; k++) {
            double f = k / 8.0;
            V3 at = new V3(Math.sin(t * 0.12 + ph - f * 2.0) * 0.08 * f, y + 0.20 + f * 0.05, -0.30 - f * 0.55);
            glow(c, at, 0.34 * (1 - f * 0.75), mix(CC_STAR, CC_TRAIL, f), 0.55 * (1 - f));
        }
        c.emissive = false;
        // cola: sube por detrás y termina en la estrella
        double sw = Math.sin(t * 0.17 + ph);
        V3[] tcp = {new V3(0, y - 0.06, -0.14), new V3(sw * 0.03, y + 0.0, -0.28), new V3(sw * 0.06, y + 0.12, -0.33), new V3(sw * 0.08, y + 0.22, -0.30)};
        curve(c, tcp, new double[]{0.040, 0.036, 0.032, 0.026}, 3, 8, fur);
        Detail.furTube(c, tcp, new double[]{0.040, 0.036, 0.032, 0.026}, 50, 611, 0.04, 0.018, 0.8, fur, CC_W);
        c.emissive = true;
        V3 st = tcp[3].add(0, 0.06, 0);
        star(c, st, new V3(0, 0, 1), V3.UP, 0.085, t * 0.06 + ph, CC_STAR, mix(CC_STAR, WHITE, 0.6));
        glow(c, st, 0.40 + 0.08 * Math.sin(t * 0.3 + ph), CC_STAR, 0.6);
        c.emissive = false;
        // cuerpo sentado, patitas recogidas
        V3 bc = new V3(0, y, -0.03), br = new V3(0.15, 0.15, 0.15);
        ell(c, bc, br, 14, 10, fur);
        Detail.fur(c, bc, br, 120, 612, 0.035, 0.016, new V3(0, -0.6, -0.6), 0.8, Detail.ALL, fur, CC_W);
        for (int sd = -1; sd <= 1; sd += 2) {
            double paddle = Math.sin(t * 0.25 + ph + sd) * 0.015;
            ell(c, new V3(sd * 0.06, y - 0.12 + paddle, 0.09), new V3(0.045, 0.04, 0.06), 8, 6, fur);
            ell(c, new V3(sd * 0.09, y - 0.13, -0.04), new V3(0.05, 0.045, 0.07), 8, 6, fur);
            Detail.paw(c, new V3(sd * 0.06, y - 0.135 + paddle, 0.14), 0.012, CC_IN);
        }
        // collar con estrellita
        V3[] ring = new V3[13];
        for (int i = 0; i < 13; i++) {
            double a = Math.PI * 2 * i / 12.0;
            ring[i] = new V3(Math.cos(a) * 0.10, y + 0.10, 0.0 + Math.sin(a) * 0.095);
        }
        double[] cr = new double[13];
        java.util.Arrays.fill(cr, 0.016);
        tube(c, ring, cr, 6, flat(CC_BLUE));
        c.emissive = true;
        star(c, new V3(0, y + 0.07, 0.105), new V3(0, -0.2, 1), V3.UP, 0.03, 0, CC_STAR, WHITE);
        c.emissive = false;
        // cabeza
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, y + 0.1, 0.02);
        V3 hc = new V3(0, y + 0.24, 0.03), hr = new V3(0.19, 0.16, 0.16);
        ell(c, hc, hr, 16, 11, fur);
        Detail.fur(c, hc, hr, 90, 613, 0.03, 0.014, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.55 ? 1 : 0, fur, CC_W);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 61 : 62);
            V3 b = new V3(sd * 0.10, y + 0.34, 0.01), tp = new V3(sd * (0.16 + 0.02 * ef), y + 0.48 - 0.04 * ef, -0.01);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.06, 0.042, 0.004}, 5, fur);
            V3 b2 = b.add(0, 0.006, 0.025), t2 = tp.add(-sd * 0.008, -0.045, 0.02);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.038, 0.026, 0.004}, 4, flat(CC_IN));
        }
        eyes(c, hc, hr, 0.44, 0.10, 0.88, 0.13, T_EYE, CC_BLUE);
        cheeks(c, hc, hr, 0.66, -0.20, 0.70, 0.09);
        ell(c, surf(hc, hr, new V3(0, -0.12, 1)).add(0, 0, 0.004), new V3(0.016, 0.011, 0.01), 6, 4, flat(CC_IN));
        decal(c, hc, hr, new V3(0, -0.30, 1), 0.075, 0, T_CAT, new float[]{0.45F, 0.30F, 0.40F}, 0.95, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                V3 wa = surf(hc, hr, new V3(sd * 0.35, -0.2, 1)).add(0, k * 0.01, 0);
                Detail.whisker(c, wa, wa.add(sd * 0.13, k * 0.03, -0.03), -0.01, 0.0025, CC_W2);
            }
        }
        // estrellita en la frente que titila
        Detail.twinkle(c, surf(hc, hr, new V3(0, 0.55, 0.85)).add(0, 0, 0.01), 0.025, CC_STAR, t, ph, 0.2);
        c.head = false;
    }

    // ------------------------------------------------------------------ CAPIBARA ZEN

    private static final float[] CP_B = {0.66F, 0.47F, 0.31F}, CP_B2 = {0.78F, 0.60F, 0.42F}, CP_DK = {0.36F, 0.24F, 0.17F},
            CP_NOSE = {0.30F, 0.21F, 0.17F}, ORANGE = {1F, 0.62F, 0.16F}, ORANGE2 = {1F, 0.78F, 0.36F}, LEAF = {0.36F, 0.74F, 0.30F};

    /**
     * Capibara Zen: una capibara gordita y tranquilísima, con los ojos entrecerrados de puro relax y una mandarina con hojita en
     * la cabeza. Se mece despacio y a veces suelta un suspiro de burbujitas.
     */
    static void zenCapybara(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.012 * w;
        c.pivot = new V3(0, 0.25, 0);
        double rock = Math.sin(t * 0.06 + ph) * 0.03 * (1 - w);
        double[] sv = Pets21.tiltAll(c, 0, rock);
        Paint fur = (n, p) -> mix(CP_B, CP_B2, ss(-0.3, 0.9, n.y) * 0.5);
        legs4(c, 0.10, 0.13, -0.15, 0.17 + bob, 0.05, 0.03, 0.06, fur, flat(CP_DK));
        // cuerpo de barril
        V3 bc = new V3(0, 0.28 + bob + 0.008 * c.breath, -0.02), br = new V3(0.19, 0.17, 0.27);
        ell(c, bc, br, 16, 11, fur);
        Detail.fur(c, bc, br, 150, 711, 0.028, 0.013, new V3(0, -0.3, -1), 0.9, Detail.ALL, fur, CP_B2);
        // cabeza cuadradita con hocico grande
        headLook(c, 0.06, 0.04);
        c.hpv = new V3(0, 0.36, 0.18);
        V3 hc = new V3(0, 0.40 + bob, 0.24), hr = new V3(0.15, 0.135, 0.14);
        Paint hp = (n, p) -> mix(CP_B, CP_B2, ss(-0.2, 0.9, n.y) * 0.45);
        Pets21.sbox(c, hc, hr, 0.62, 16, 12, hp);
        V3 sc = new V3(0, 0.375 + bob, 0.36), sr = new V3(0.12, 0.095, 0.09);
        Pets21.sbox(c, sc, sr, 0.65, 14, 10, hp);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(sc, sr, new V3(sd * 0.45, 0.25, 1)).add(0, 0, -0.004), new V3(0.018, 0.012, 0.008), 6, 4, flat(CP_NOSE));
            // orejitas redondas
            double ef = Detail.flick(c, sd > 0 ? 71 : 72);
            ell(c, new V3(sd * 0.11, 0.52 + bob, 0.17), new V3(0.035, 0.03, 0.018), 0, 0, sd * (0.3 + 0.4 * ef), 8, 5, flat(CP_DK));
        }
        decal(c, sc, sr, new V3(0, -0.45, 1), 0.07, 0, T_MOUTH, new float[]{0.35F, 0.22F, 0.18F}, 0.85, 0.003);
        // ojos de relax: casi siempre entrecerrados
        boolean open = Detail.flick(c, 73) > 0.4;
        float[] cell = open ? T_BEAD : T_EYE_SHUT;
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, hc, hr, new V3(sd * 0.60, 0.22, 0.78), open ? 0.075 : 0.10, 0, cell, open ? WHITE : new float[]{0.22F, 0.14F, 0.10F}, 1, 0.004);
        }
        cheeks(c, hc, hr, 0.78, -0.10, 0.60, 0.08);
        // mandarina con hojita, que se balancea un poco
        double wob = Math.sin(t * 0.09 + ph) * 0.04;
        V3 oc = new V3(wob * 0.3, 0.58 + bob, 0.22);
        ell(c, oc, new V3(0.075, 0.062, 0.075), 12, 8, (n, p) -> mix(ORANGE, ORANGE2, ss(0, 0.9, n.y) * 0.5));
        Detail.glint(c, oc, new V3(0.075, 0.062, 0.075), new V3(-0.4, 0.7, 0.6), 0.04, WHITE, 0.6);
        ell(c, oc.add(0, 0.062, 0), new V3(0.012, 0.01, 0.012), 6, 4, flat(CP_DK));
        Detail.kite(c, oc.add(0, 0.065, 0), oc.add(0.07 + wob, 0.10, -0.01), new V3(0, 0, 1), 0.022, new V3(0, 1, 0.2).norm(), LEAF,
                mix(LEAF, WHITE, 0.2));
        c.head = false;
        // suspiro de burbujitas zen
        double sigh = Detail.flick(c, 74);
        if (sigh > 0.05) {
            c.blend = Sink.NORMAL;
            for (int k = 0; k < 3; k++) {
                double u = (sigh + k * 0.25) % 1.0;
                c.alpha = 0.6 * (1 - u);
                ell(c, new V3(0.04 * Math.sin(k * 2.0), 0.42 + u * 0.25, 0.45 + u * 0.05), new V3(0.02 + 0.01 * k, 0.02 + 0.01 * k, 0.02 + 0.01 * k),
                        8, 5, flat(new float[]{0.85F, 0.95F, 1F}));
            }
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
        Pets21.restore(c, sv);
    }

    // ------------------------------------------------------------------ PULPO ASTRONAUTA

    private static final float[] OC_P = {1F, 0.52F, 0.66F}, OC_P2 = {1F, 0.70F, 0.80F}, OC_SUCK = {1F, 0.86F, 0.90F},
            SUIT = {0.95F, 0.96F, 1F}, SUIT2 = {0.80F, 0.84F, 0.92F}, GLASS = {0.80F, 0.92F, 1F}, OC_BLUE = {0.35F, 0.60F, 1F},
            OC_GOLD = {1F, 0.82F, 0.35F};

    /**
     * Pulpo Astronauta: un pulpito rosa dentro de una escafandra de cristal redonda, con mochila de oxígeno y antena; camina
     * ondulando los tentáculos y, contento, suelta burbujitas dentro del casco.
     */
    static void astroOctopus(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.sin(t * 0.12 + ph) * 0.012 + Math.abs(Math.sin(c.gait)) * 0.02 * w;
        c.pivot = new V3(0, 0.3, 0);
        // tentáculos (8) que salen del cuello del traje
        Paint arm = (n, p) -> mix(OC_P, OC_P2, ss(-0.4, 0.8, n.y) * 0.5);
        for (int k = 0; k < 8; k++) {
            double a = Math.PI * 2 * (k + 0.5) / 8.0;
            double ca = Math.cos(a), sa = Math.sin(a);
            double wave = Math.sin(t * 0.18 + k * 0.9 + ph) + Math.sin(c.gait * 1.2 + k * Math.PI / 2) * 1.8 * w;
            V3[] cp = new V3[5];
            for (int i = 0; i < 5; i++) {
                double f = i / 4.0;
                double rr = 0.09 + f * 0.16;
                double curl = f * f * (0.5 + 0.25 * wave);
                cp[i] = new V3(sa * rr + ca * curl * 0.06, 0.20 + bob - f * 0.19 + (f > 0.75 ? (f - 0.75) * 0.25 * (1 + 0.6 * wave) : 0),
                        ca * rr - sa * curl * 0.06);
            }
            double[] rad = {0.045, 0.040, 0.032, 0.022, 0.010};
            curve(c, cp, rad, 3, 7, arm);
            for (int i = 1; i < 4; i++) {
                V3 at = cp[i].add(0, -rad[i] * 0.8, 0);
                ell(c, at, new V3(0.009, 0.004, 0.009), 6, 3, flat(OC_SUCK));
            }
        }
        // cuello del traje
        V3 nc = new V3(0, 0.25 + bob, 0);
        ell(c, nc, new V3(0.15, 0.05, 0.15), 14, 6, (n, p) -> mix(SUIT2, SUIT, ss(-0.5, 0.8, n.y)));
        V3[] ring = new V3[17];
        for (int i = 0; i < 17; i++) {
            double a = Math.PI * 2 * i / 16.0;
            ring[i] = new V3(Math.cos(a) * 0.16, 0.28 + bob, Math.sin(a) * 0.16);
        }
        double[] rr = new double[17];
        java.util.Arrays.fill(rr, 0.022);
        tube(c, ring, rr, 6, flat(SUIT2));
        // mochila de oxígeno y antena
        V3 pack = new V3(0, 0.40 + bob, -0.21);
        Pets21.sbox(c, pack, new V3(0.11, 0.12, 0.05), 0.4, 12, 10, (n, p) -> mix(SUIT2, SUIT, ss(-0.4, 0.8, n.y)));
        ell(c, pack.add(0.05, 0.05, -0.05), new V3(0.02, 0.02, 0.006), 6, 4, flat(OC_BLUE));
        V3 an0 = pack.add(-0.06, 0.11, 0), an1 = an0.add(Math.sin(t * 0.2 + ph) * 0.02, 0.12, -0.01);
        tube(c, new V3[]{an0, an0.lerp(an1, 0.5), an1}, new double[]{0.007, 0.006, 0.005}, 5, flat(SUIT2));
        c.emissive = true;
        double blink = 0.5 + 0.5 * Math.sin(t * 0.3 + ph);
        ell(c, an1, new V3(0.02, 0.02, 0.02), 8, 6, flat(mix(OC_BLUE, WHITE, 0.3 * blink)));
        glow(c, an1, 0.1 + 0.05 * blink, OC_BLUE, 0.4 + 0.3 * blink);
        c.emissive = false;
        // tubo de aire de la mochila al casco
        V3 h0 = pack.add(0.09, -0.04, 0.02), h1 = new V3(0.17, 0.33 + bob, -0.06);
        curve(c, new V3[]{h0, h0.add(0.05, -0.03, 0.03), h1}, new double[]{0.012, 0.012, 0.012}, 3, 5, flat(SUIT2));
        // cabeza del pulpo dentro del casco
        headLook(c, 0.10, 0.0);
        c.hpv = new V3(0, 0.30, 0);
        V3 hc = new V3(0, 0.43 + bob, 0.01), hr = new V3(0.14, 0.15, 0.13);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(OC_P, OC_P2, ss(-0.3, 0.9, n.y) * 0.6));
        for (int k = 0; k < 4; k++) {
            Detail.spot(c, hc, hr, new V3(k % 2 == 0 ? 0.5 : -0.45, 0.65, -0.3 + k * 0.2), 0.04, OC_P2);
        }
        eyes(c, hc, hr, 0.46, 0.0, 0.88, 0.12, T_EYE, new float[]{0.55F, 0.75F, 1F});
        cheeks(c, hc, hr, 0.70, -0.30, 0.66, 0.08);
        decal(c, hc, hr, new V3(0, -0.35, 1), 0.07, 0, T_CAT, new float[]{0.55F, 0.20F, 0.30F}, 0.95, 0.004);
        // burbujitas dentro del casco
        if (c.joy > 0.1 || Detail.flick(c, 81) > 0.1) {
            c.blend = Sink.NORMAL;
            for (int k = 0; k < 3; k++) {
                double u = ((t * 0.04 + k * 0.33) % 1.0);
                c.alpha = 0.7 * (1 - u);
                ell(c, new V3(0.06 * Math.sin(k * 2.0 + t * 0.1), 0.50 + bob + u * 0.12, 0.10), new V3(0.012, 0.012, 0.012), 6, 4, flat(GLASS));
            }
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
        // casco de cristal (translúcido, con brillo)
        V3 gc = new V3(0, 0.44 + bob, 0.0), gr = new V3(0.20, 0.20, 0.20);
        c.blend = Sink.NORMAL;
        c.alpha = 0.26;
        ell(c, gc, gr, 18, 12, flat(GLASS));
        c.alpha = 1;
        c.blend = Sink.SOLID;
        Detail.soft(c, gc, gr, new V3(-0.45, 0.55, 0.7), 0.11, WHITE, 0.7);
        Detail.soft(c, gc, gr, new V3(0.5, -0.2, 0.84), 0.05, WHITE, 0.5);
        c.head = false;
        // parche con estrella en el cuello
        c.emissive = true;
        Pets22.star(c, new V3(0.0, 0.25 + bob, 0.155), new V3(0, 0.1, 1), V3.UP, 0.028, 0, OC_GOLD, WHITE);
        c.emissive = false;
    }

    // ------------------------------------------------------------------ DRAGONCITO DE CRISTAL

    private static final float[] CD_B = {0.80F, 0.72F, 0.98F}, CD_B2 = {0.92F, 0.88F, 1F}, CD_BELLY = {0.98F, 0.94F, 1F},
            CRYS = {0.70F, 0.40F, 1F}, CRYS2 = {0.92F, 0.78F, 1F}, CRYS_GLOW = {0.78F, 0.52F, 1F};

    /** Cristal hexagonal con punta (prisma) de {@code a} hacia {@code b}. */
    static void crystal(Ctx c, V3 a, V3 b, double r, float[] c0, float[] c1) {
        V3 mid = a.lerp(b, 0.72);
        tube(c, new V3[]{a, a.lerp(mid, 0.5), mid}, new double[]{r * 0.85, r, r}, 6, (n, p) -> mix(c0, c1, ss(-0.3, 0.8, n.y) * 0.6));
        tube(c, new V3[]{mid, mid.lerp(b, 0.5), b}, new double[]{r, r * 0.55, 0.002}, 6, (n, p) -> mix(c0, c1, 0.5));
    }

    /**
     * Dragoncito de Cristal: un dragón bebé lila con cristales de amatista en el lomo, los cuernos y la punta de la cola, alas
     * de membrana de cristal y una gema que late en el pecho.
     */
    static void crystalDragon(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.14 + ph) * 0.035;
        double fl = Math.sin(t * 0.40 + ph);
        c.pivot = new V3(0, 0.42, 0);
        double y = 0.40 + bob;
        Paint skin = (n, p) -> mix(CD_BELLY, mix(CD_B, CD_B2, ss(0, 1, n.y) * 0.5), ss(-0.45, 0.0, n.y));
        // alas de cristal
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 root = new V3(sd * 0.10, y + 0.08, -0.04);
            double up = 0.4 + fl * 0.6;
            V3 elbow = root.add(sd * 0.17, 0.08 + up * 0.14, -0.04);
            V3 hand = elbow.add(sd * 0.18, 0.06 + up * 0.14, -0.08);
            V3[] tips = new V3[4];
            for (int k = 0; k < 4; k++) {
                double f = k / 3.0;
                tips[k] = hand.add(sd * (0.10 - f * 0.20), (0.06 - f * 0.26) * (0.5 + up * 0.6), -0.04 - f * 0.16);
            }
            tube(c, new V3[]{root, elbow, hand}, new double[]{0.022, 0.017, 0.012}, 6, flat(CD_B));
            c.blend = Sink.NORMAL;
            c.alpha = 0.62;
            c.emissive = true;
            V3[] fan = new V3[tips.length + 1];
            fan[0] = elbow;
            System.arraycopy(tips, 0, fan, 1, tips.length);
            Creatures4.membrane(c, root.add(0, -0.05, -0.04), fan, (n, p) -> mix(CRYS, CRYS2, 0.5));
            c.emissive = false;
            c.alpha = 1;
            c.blend = Sink.SOLID;
            for (V3 tp : tips) {
                crystal(c, hand, tp, 0.009, CRYS, CRYS2);
            }
        }
        // cola con punta de cristal
        double sw = Math.sin(t * 0.15 + ph);
        V3[] tcp = {new V3(0, y - 0.08, -0.12), new V3(sw * 0.05, y - 0.13, -0.26), new V3(sw * 0.10, y - 0.10, -0.38), new V3(sw * 0.12, y - 0.04, -0.46)};
        curve(c, tcp, new double[]{0.07, 0.05, 0.035, 0.02}, 3, 8, skin);
        crystal(c, tcp[3], tcp[3].add(sw * 0.02, 0.06, -0.08), 0.03, CRYS, CRYS2);
        // patitas colgando
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.08, y - 0.15, 0.02), new V3(0.04, 0.04, 0.05), 8, 5, skin);
            ell(c, new V3(sd * 0.07, y - 0.04, 0.12), new V3(0.03, 0.03, 0.04), 8, 5, skin);
        }
        // cuerpo
        V3 bc = new V3(0, y - 0.02, -0.02), br = new V3(0.13, 0.15, 0.15);
        ell(c, bc, br, 14, 10, skin);
        // gema del pecho que late
        c.emissive = true;
        double beat = 0.5 + 0.5 * Math.sin(t * 0.2 + ph);
        ell(c, surf(bc, br, new V3(0, -0.1, 1)).add(0, 0, 0.005), new V3(0.03, 0.035, 0.015), 8, 6, flat(mix(CRYS, CRYS2, beat)));
        glow(c, surf(bc, br, new V3(0, -0.1, 1)).add(0, 0, 0.02), 0.16 + 0.06 * beat, CRYS_GLOW, 0.45 + 0.25 * beat);
        c.emissive = false;
        // cristales del lomo
        for (int k = 0; k < 4; k++) {
            double f = k / 3.0;
            V3 base = new V3(0, y + 0.10 - f * 0.12, 0.02 - f * 0.15);
            crystal(c, base, base.add(0, 0.09 - f * 0.03, -0.04), 0.022 - f * 0.004, CRYS, CRYS2);
        }
        // cabeza
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, y + 0.12, 0.06);
        V3 hc = new V3(0, y + 0.22, 0.08), hr = new V3(0.16, 0.14, 0.15);
        ell(c, hc, hr, 16, 11, skin);
        V3 snc = new V3(0, y + 0.18, 0.21), snr = new V3(0.085, 0.065, 0.07);
        ell(c, snc, snr, 12, 8, skin);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(snc, snr, new V3(sd * 0.4, 0.3, 1)).add(0, 0, -0.004), new V3(0.01, 0.008, 0.006), 5, 3, flat(new float[]{0.55F, 0.45F, 0.75F}));
            // cuernos de cristal
            V3 hb = new V3(sd * 0.08, y + 0.33, 0.03);
            crystal(c, hb, hb.add(sd * 0.05, 0.10, -0.06), 0.022, CRYS, CRYS2);
        }
        eyes(c, hc, hr, 0.50, 0.10, 0.84, 0.13, T_EYE, new float[]{0.80F, 0.60F, 1F});
        cheeks(c, hc, hr, 0.70, -0.24, 0.66, 0.08);
        decal(c, snc, snr, new V3(0, -0.45, 1), 0.06, 0, T_MOUTH, new float[]{0.45F, 0.30F, 0.55F}, 0.9, 0.003);
        c.head = false;
        // destellos alrededor
        for (int k = 0; k < 3; k++) {
            double a = t * 0.08 + k * 2.1 + ph;
            Detail.twinkle(c, new V3(Math.cos(a) * 0.33, y + 0.1 + 0.12 * Math.sin(a * 1.7), Math.sin(a) * 0.33), 0.02, CRYS2, t, ph + k, 0.3);
        }
    }
}
