package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Segunda vuelta (etapa 252) de las tres mascotas que Axelis marcó en el catálogo: Cachorro Rey, Cabrita Montañera y Capibara
 * Zen. Estilo de las que más le gustan: siluetas limpias y redondas (sin pelo en púas), cabeza grande, ojos grandes y brillantes,
 * patitas gorditas y un detalle propio de cada una.
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0.
 */
final class Pets24 {
    private Pets24() {
    }

    // ------------------------------------------------------------------ CACHORRO REY

    private static final float[] LN_Y = {1F, 0.80F, 0.40F}, LN_Y2 = {1F, 0.89F, 0.58F}, LN_CR = {1F, 0.96F, 0.86F},
            LN_M1 = {0.93F, 0.50F, 0.18F}, LN_M2 = {1F, 0.64F, 0.26F}, LN_M3 = {0.86F, 0.40F, 0.14F}, LN_NOSE = {0.55F, 0.28F, 0.24F},
            GOLD = {1F, 0.82F, 0.25F}, GOLD2 = {1F, 0.95F, 0.62F}, RUBY = {0.95F, 0.20F, 0.34F};

    /**
     * Cachorro Rey: un cachorrito de león redondo, con la cabeza enorme dentro de una melena en forma de sol (gajos redondos de
     * dos naranjas), mofletes de crema, ojazos ámbar, orejitas redondas, una corona chiquita ladeada y la cola con una borla.
     */
    static void lionCub(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.028 * w;
        c.pivot = new V3(0, 0.26, 0);
        Paint fur = (n, p) -> mix(LN_CR, mix(LN_Y, LN_Y2, ss(0, 1, n.y) * 0.45), ss(-0.45, 0.0, n.y + Math.max(0, n.z) * 0.3));
        Pets23.stubLegs(c, 0.085, 0.085, -0.085, 0.15 + bob, 0.05, true, fur, flat(LN_CR), 1.0);
        // cuerpecito de alubia con barriga de crema
        V3 bc = new V3(0, 0.23 + bob + 0.006 * c.breath, -0.03), br = new V3(0.14, 0.125, 0.155);
        ell(c, bc, br, 14, 10, fur);
        // cola en S con borla redonda (se mueve más cuando está contento)
        double sw = Math.sin(t * (0.2 + 0.3 * c.joy) + ph) * (0.06 + 0.06 * c.joy);
        V3[] tc = {bc.add(0, 0.02, -0.15), bc.add(sw * 0.4, 0.07, -0.25), bc.add(sw, 0.16, -0.27), bc.add(sw * 1.3, 0.22, -0.23)};
        curve(c, tc, new double[]{0.026, 0.022, 0.02, 0.018}, 3, 6, fur);
        V3 tuft = tc[3].add(0, 0.035, 0.01);
        ell(c, tuft, new V3(0.048, 0.052, 0.048), 10, 7, (n, p) -> mix(LN_M1, LN_M2, ss(-0.3, 0.9, n.y) * 0.6));
        // cabeza grande
        headLook(c, 0.12, 0.03);
        c.hpv = new V3(0, 0.34, 0.07);
        V3 hc = new V3(0, 0.46 + bob, 0.09), hr = new V3(0.18, 0.162, 0.16);
        // melena de sol: dos aros de gajos redondos detrás de la cabeza, que respiran
        for (int ring = 0; ring < 2; ring++) {
            int n = ring == 0 ? 13 : 11;
            double rx = ring == 0 ? 0.20 : 0.15, ry = ring == 0 ? 0.185 : 0.14, z = ring == 0 ? -0.07 : -0.04;
            for (int i = 0; i < n; i++) {
                double a = Math.PI * 2 * i / n + ring * 0.25;
                double puff = 1 + 0.04 * Math.sin(t * 0.11 + ph + i) + 0.03 * c.breath;
                V3 lc = hc.add(Math.cos(a) * rx, Math.sin(a) * ry - 0.01, z);
                double lr = (ring == 0 ? 0.072 : 0.07) * puff;
                float[] col = ring == 1 ? LN_M2 : (i % 2 == 0 ? LN_M1 : LN_M3);
                ell(c, lc, new V3(lr, lr, lr * 0.75), 10, 7, (nn, p) -> mix(col, LN_M2, ss(-0.2, 1, nn.y) * 0.35));
            }
        }
        ell(c, hc, hr, 18, 12, fur);
        // orejitas redondas que asoman por arriba
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 141 : 142);
            V3 ec = new V3(sd * 0.125, 0.60 + bob, 0.05);
            ell(c, ec, new V3(0.048, 0.046, 0.026), 0, 0, sd * 0.2 * ef, 9, 6, fur);
            ell(c, ec.add(0, -0.004, 0.012), new V3(0.03, 0.028, 0.012), 0, 0, sd * 0.2 * ef, 7, 5, flat(LN_CR));
        }
        // mofletes de crema, naricita y boquita
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.048, 0.415 + bob, 0.225), new V3(0.058, 0.046, 0.042), 10, 7, flat(LN_CR));
        }
        ell(c, new V3(0, 0.385 + bob, 0.215), new V3(0.04, 0.03, 0.03), 8, 5, flat(LN_CR));
        ell(c, new V3(0, 0.45 + bob, 0.262), new V3(0.03, 0.02, 0.016), 8, 5, flat(LN_NOSE));
        Detail.glint(c, new V3(0, 0.45 + bob, 0.262), new V3(0.03, 0.02, 0.016), new V3(-0.3, 0.8, 0.5), 0.012, WHITE, 0.7);
        decal(c, new V3(0, 0.415 + bob, 0.225), new V3(0.1, 0.046, 0.045), new V3(0, -0.2, 1), 0.07, 0, T_CAT, LN_NOSE, 0.9, 0.004);
        eyes(c, hc, hr, 0.42, 0.13, 0.88, 0.145, T_EYE, new float[]{1F, 0.70F, 0.26F});
        cheeks(c, hc, hr, 0.72, -0.16, 0.64, 0.085);
        // corona chiquita y ladeada, con rubí que brilla
        V3 cr = new V3(0.035, 0.625 + bob, 0.08);
        double tilt = -0.28;
        V3[] band = new V3[13];
        for (int i = 0; i <= 12; i++) {
            double a = Math.PI * 2 * i / 12.0;
            band[i] = cr.add(Math.cos(a) * 0.065, Math.cos(a) * 0.065 * Math.sin(tilt), Math.sin(a) * 0.06);
        }
        tube(c, band, Pets23.radii(13, 0.017), 6, (n, p) -> mix(GOLD, GOLD2, ss(-0.2, 0.9, n.y) * 0.6));
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5.0 + 0.3;
            V3 b = cr.add(Math.cos(a) * 0.065, Math.cos(a) * 0.065 * Math.sin(tilt) + 0.012, Math.sin(a) * 0.06);
            cone(c, b, b.add(0, 0.05, 0), 0.017, 0.004, 5, flat(GOLD));
            ell(c, b.add(0, 0.053, 0), new V3(0.009, 0.009, 0.009), 5, 3, flat(GOLD2));
        }
        c.emissive = true;
        ell(c, cr.add(0, 0.004, 0.064), new V3(0.018, 0.018, 0.009), 6, 4, flat(RUBY));
        c.emissive = false;
        Detail.twinkle(c, cr.add(0, 0.02, 0.07), 0.012, WHITE, t, ph, 0.25);
        c.head = false;
    }

    // ------------------------------------------------------------------ CABRITA MONTAÑERA

    private static final float[] GT_W = {0.98F, 0.98F, 1F}, GT_W2 = {0.86F, 0.88F, 0.94F}, GT_FACE = {0.97F, 0.95F, 0.92F},
            GT_SKIN = {0.96F, 0.80F, 0.76F}, GT_LEG = {0.88F, 0.84F, 0.80F}, GT_HOOF = {0.36F, 0.28F, 0.26F}, GT_HORN = {0.92F, 0.80F, 0.55F},
            SCARF = {0.92F, 0.24F, 0.28F}, SCARF2 = {1F, 0.42F, 0.40F}, BELL = {1F, 0.82F, 0.30F}, PACK = {0.62F, 0.42F, 0.26F},
            PACK2 = {0.74F, 0.54F, 0.34F}, ROLL = {0.30F, 0.52F, 0.86F}, ROLL2 = {0.48F, 0.70F, 0.98F};

    /** Lana de nube: bolitas que se juntan (siluetas redondas en vez de pelos en púas). */
    private static final double[][] WOOL = {
            {0, 0.02, 0, 0.13}, {0.08, 0.03, 0.06, 0.09}, {-0.08, 0.03, 0.06, 0.09}, {0.08, 0.03, -0.07, 0.09}, {-0.08, 0.03, -0.07, 0.09},
            {0, 0.09, -0.02, 0.095}, {0, 0.06, 0.09, 0.085}, {0, 0.04, -0.12, 0.08}, {0.10, -0.02, 0, 0.075}, {-0.10, -0.02, 0, 0.075}};

    /**
     * Cabrita Montañera: una cabrita bebé con la lana como una nube de bolitas, patitas cortas con pezuñas oscuras, cuernitos
     * enroscados, un copete de lana, orejas largas caídas, perilla, bufanda roja de punto con campanita y una mochila de
     * exploradora con su manta enrollada. Da brinquitos de alegría.
     */
    static void mountainGoat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait)) * 0.03 * w + c.joy * 0.05;
        c.pivot = new V3(0, 0.26, 0);
        Paint wool = (n, p) -> mix(GT_W2, GT_W, ss(-0.6, 0.5, n.y));
        Pets23.stubLegs(c, 0.072, 0.075, -0.085, 0.12 + hop, 0.037, true, flat(GT_LEG), flat(GT_HOOF), 1.1);
        V3 bc = new V3(0, 0.22 + hop + 0.006 * c.breath, -0.02);
        for (double[] b : WOOL) {
            double rr = b[3] * (1 + 0.025 * Math.sin(t * 0.1 + ph + b[0] * 40 + b[2] * 30));
            ell(c, bc.add(b[0], b[1], b[2]), new V3(rr, rr * 0.92, rr), 10, 7, wool);
        }
        // colita de pompón
        ell(c, bc.add(0, 0.10, -0.20), new V3(0.045, 0.05, 0.04), 0, -0.5 + Math.sin(t * 0.5 + ph) * 0.3, 0, 8, 6, wool);
        // mochila de exploradora con la manta enrollada encima
        V3 pk = bc.add(0, 0.15, -0.06);
        Pets21.sbox(c, pk, new V3(0.07, 0.055, 0.05), 0.6, 10, 8, (n, p) -> mix(PACK, PACK2, ss(-0.2, 0.9, n.y) * 0.6));
        Pets21.sbox(c, pk.add(0, 0.035, 0.035), new V3(0.06, 0.02, 0.022), 0.6, 8, 6, flat(PACK2));
        V3 rl = pk.add(0, 0.075, -0.005);
        tube(c, new V3[]{rl.add(-0.075, 0, 0), rl, rl.add(0.075, 0, 0)}, new double[]{0.026, 0.028, 0.026}, 8,
                (n, p) -> mix(ROLL, ROLL2, 0.5 + 0.5 * Math.sin(p.z * 260 + p.y * 200)));
        // bufanda de punto: vuelta al cuello y una punta que ondea hacia atrás con flecos
        V3 nk = new V3(0, 0.325 + hop, 0.10);
        tube(c, Pets23.ring(nk, 0.088, 0.08, 14), Pets23.radii(15, 0.033), 6,
                (n, p) -> mix(SCARF, SCARF2, (Math.sin(p.x * 220) > 0 ? 0.35 : 0.0) + ss(-0.3, 0.9, n.y) * 0.3));
        double fl = Math.sin(t * 0.28 + ph) * 0.035 + 0.06 * w;
        V3 a = nk.add(0.06, -0.01, -0.06), b = a.add(0.04 + fl, -0.05, -0.08 - 0.07 * w), d = b.add(0.02 + fl, -0.03, -0.07);
        curve(c, new V3[]{a, b, d}, new double[]{0.03, 0.027, 0.024}, 3, 5, flat(SCARF));
        for (int k = -1; k <= 1; k++) {
            V3 f0 = d.add(k * 0.012, 0, -0.01);
            cone(c, f0, f0.add(k * 0.006, -0.03, -0.012), 0.006, 0.003, 4, flat(SCARF2));
        }
        double ringA = Math.sin(t * 0.3 + ph) * 0.15 + c.joy * Math.sin(t * 1.4) * 0.3;
        V3 bell = nk.add(0, -0.065, 0.075);
        ell(c, bell, new V3(0.028, 0.03, 0.028), 0, 0, ringA, 8, 6, flat(BELL));
        Detail.glint(c, bell, new V3(0.028, 0.03, 0.028), new V3(-0.4, 0.5, 0.8), 0.02, WHITE, 0.8);
        // cabeza
        headLook(c, 0.11, 0.04);
        c.hpv = new V3(0, 0.35, 0.12);
        V3 hc = new V3(0, 0.45 + hop, 0.15), hr = new V3(0.14, 0.13, 0.13);
        ell(c, hc, hr, 16, 11, (n, p) -> mix(GT_W2, GT_FACE, ss(-0.6, 0.4, n.y)));
        // copete de lana
        ell(c, new V3(0, 0.565 + hop, 0.14), new V3(0.05, 0.045, 0.05), 9, 6, wool);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.045, 0.555 + hop, 0.12), new V3(0.04, 0.036, 0.04), 8, 6, wool);
        }
        V3 sn = new V3(0, 0.405 + hop, 0.255), sr = new V3(0.07, 0.055, 0.055);
        ell(c, sn, sr, 12, 8, flat(GT_SKIN));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(sn, sr, new V3(sd * 0.4, 0.25, 1)).add(0, 0, -0.003), new V3(0.009, 0.007, 0.005), 5, 3, flat(new float[]{0.72F, 0.46F, 0.46F}));
        }
        decal(c, sn, sr, new V3(0, -0.5, 1), 0.06, 0, T_CAT, new float[]{0.55F, 0.35F, 0.35F}, 0.9, 0.003);
        ell(c, new V3(0, 0.345 + hop, 0.24), new V3(0.018, 0.035, 0.016), 0, 0.3, 0, 6, 5, flat(GT_W2));
        eyes(c, hc, hr, 0.48, 0.12, 0.85, 0.13, T_EYE, new float[]{0.62F, 0.46F, 0.32F});
        cheeks(c, hc, hr, 0.72, -0.15, 0.62, 0.08);
        for (int sd = -1; sd <= 1; sd += 2) {
            // cuernitos gorditos y enroscados hacia atrás
            V3 h0 = new V3(sd * 0.065, 0.56 + hop, 0.10);
            V3[] hp = {h0, h0.add(sd * 0.025, 0.045, -0.03), h0.add(sd * 0.06, 0.05, -0.08), h0.add(sd * 0.07, 0.01, -0.10), h0.add(sd * 0.05, -0.015, -0.08)};
            curve(c, hp, new double[]{0.032, 0.026, 0.02, 0.014, 0.008}, 3, 6, (n, p) -> mix(GT_HORN, WHITE, 0.12 + 0.12 * Math.sin(p.y * 140 + p.z * 90)));
            // orejas largas caídas (se mueven solas de vez en cuando)
            double ef = Detail.flick(c, sd > 0 ? 241 : 242);
            ell(c, new V3(sd * 0.155, 0.46 + hop, 0.12), new V3(0.085, 0.026, 0.038), sd * 0.2, 0, sd * (-0.55 + 0.35 * ef), 10, 6, flat(GT_W));
            ell(c, new V3(sd * 0.16, 0.458 + hop, 0.134), new V3(0.062, 0.015, 0.022), sd * 0.2, 0, sd * (-0.55 + 0.35 * ef), 8, 5, flat(GT_SKIN));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ CAPIBARA ZEN

    private static final float[] CP_B = {0.80F, 0.58F, 0.38F}, CP_B2 = {0.90F, 0.71F, 0.50F}, CP_DK = {0.50F, 0.34F, 0.22F},
            CP_NOSE = {0.36F, 0.24F, 0.18F}, CP_CR = {0.95F, 0.82F, 0.66F}, TOWEL = {0.98F, 0.98F, 0.96F}, TOWEL2 = {0.45F, 0.70F, 0.95F},
            ORANGE = {1F, 0.60F, 0.14F}, ORANGE2 = {1F, 0.78F, 0.36F}, LEAF = {0.36F, 0.74F, 0.30F},
            DUCK = {1F, 0.86F, 0.30F}, DUCK2 = {1F, 0.94F, 0.56F}, BEAK = {1F, 0.56F, 0.18F};

    /**
     * Capibara Zen: una capibara redondita color caramelo con su cabeza cuadrada de hocico grande, ojitos tranquilos, una
     * sonrisa leve, un patito amigo sentado en el lomo y, como en las aguas termales, una toalla doblada en la cabeza con una
     * mandarina encima. Se mece muy despacio
     * y de vez en cuando suspira burbujitas.
     */
    static void zenCapybara(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 0.9)) * 0.014 * w;
        c.pivot = new V3(0, 0.24, 0);
        double rock = Math.sin(t * 0.06 + ph) * 0.03 * (1 - w);
        double[] sv = Pets21.tiltAll(c, 0, rock);
        Paint fur = (n, p) -> mix(CP_CR, mix(CP_B, CP_B2, ss(-0.3, 0.9, n.y) * 0.5), ss(-0.75, -0.35, n.y));
        Pets23.stubLegs(c, 0.10, 0.12, -0.13, 0.13 + bob, 0.05, true, fur, flat(CP_DK), 0.9);
        // cuerpo de barril, rechoncho
        V3 bc = new V3(0, 0.235 + bob + 0.008 * c.breath, -0.03), br = new V3(0.18, 0.155, 0.23);
        ell(c, bc, br, 16, 11, fur);
        // su amigo: un patito amarillo sentado en el lomo, que se balancea y de vez en cuando mira a los lados
        double dk = Math.sin(t * 0.12 + ph + 1) * 0.03;
        V3 dc = bc.add(0, 0.175, -0.07);
        ell(c, dc, new V3(0.05, 0.04, 0.058), dk, 0, 0, 10, 7, (n, p) -> mix(DUCK, DUCK2, ss(-0.2, 0.9, n.y) * 0.6));
        ell(c, dc.add(0, 0.015, -0.05), new V3(0.02, 0.018, 0.022), 0, -0.6, 0, 6, 4, flat(DUCK));
        double look = Detail.flick(c, 247) * 0.6 - Detail.flick(c, 248) * 0.6;
        V3 dh = dc.add(0, 0.055, 0.035);
        ell(c, dh, new V3(0.035, 0.034, 0.034), look, 0, 0, 9, 6, (n, p) -> mix(DUCK, DUCK2, ss(-0.2, 0.9, n.y) * 0.6));
        V3 bk = dh.add(Math.sin(look) * 0.035, -0.006, Math.cos(look) * 0.035);
        ell(c, bk, new V3(0.016, 0.008, 0.016), look, 0, 0, 6, 4, flat(BEAK));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ey = dh.add(Math.cos(look) * sd * 0.017 + Math.sin(look) * 0.022, 0.01, -Math.sin(look) * sd * 0.017 + Math.cos(look) * 0.022);
            ell(c, ey, new V3(0.006, 0.007, 0.004), 4, 3, flat(new float[]{0.10F, 0.08F, 0.08F}));
        }
        // cabeza cuadradita con un hocico grande y plano
        headLook(c, 0.06, 0.04);
        c.hpv = new V3(0, 0.31, 0.16);
        Paint hp = (n, p) -> mix(CP_B, CP_B2, ss(-0.2, 0.9, n.y) * 0.5);
        V3 hc = new V3(0, 0.37 + bob, 0.21), hr = new V3(0.14, 0.13, 0.14);
        Pets21.sbox(c, hc, hr, 0.62, 16, 12, hp);
        V3 sc = new V3(0, 0.345 + bob, 0.325), sr = new V3(0.12, 0.095, 0.085);
        Pets21.sbox(c, sc, sr, 0.62, 14, 10, hp);
        // la "placa" oscura de la nariz, con sus agujeritos
        V3 np = surf(sc, sr, new V3(0, 0.55, 1)).add(0, 0, -0.012);
        ell(c, np, new V3(0.07, 0.035, 0.016), 10, 6, flat(CP_NOSE));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, np.add(sd * 0.03, 0.002, 0.012), new V3(0.012, 0.008, 0.005), 6, 4, flat(new float[]{0.16F, 0.10F, 0.08F}));
        }
        V3 mo = boxSurf(sc, sr, 0.62, new V3(0, -0.35, 1));
        decal(c, mo.add(0, 0, -0.01), new V3(0.01, 0.01, 0.01), new V3(0, 0, 1), 0.06, 0, T_CAT, new float[]{0.36F, 0.22F, 0.16F}, 0.9, 0.002);
        // ojitos tranquilos y brillantes (de vez en cuando los cierra, a gusto)
        boolean shut = Detail.flick(c, 243) > 0.55 || c.shut;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 dir = new V3(sd * 0.62, 0.32, 0.72);
            V3 ep = boxSurf(hc, hr, 0.62, dir);
            if (shut) {
                decal(c, ep.sub(dir.norm().mul(0.01)), new V3(0.01, 0.01, 0.01), dir, 0.07, 0, T_EYE_SHUT, new float[]{0.22F, 0.14F, 0.10F}, 1, 0.003);
            } else {
                ell(c, ep.add(0, 0, -0.004), new V3(0.026, 0.03, 0.016), 8, 6, flat(new float[]{0.12F, 0.08F, 0.07F}));
                ell(c, ep.add(-sd * 0.006, 0.01, 0.01), new V3(0.009, 0.009, 0.005), 5, 3, flat(WHITE));
            }
            // mofletes rosados
            V3 cp = boxSurf(hc, hr, 0.62, new V3(sd * 0.75, -0.25, 0.62));
            c.blend = Sink.NORMAL;
            c.alpha = 0.55;
            ell(c, cp, new V3(0.032, 0.02, 0.008), 0, 0, 0, 8, 5, flat(new float[]{1F, 0.55F, 0.55F}));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
        // orejitas redondas oscuras
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 244 : 245);
            ell(c, new V3(sd * 0.115, 0.49 + bob, 0.14), new V3(0.034, 0.03, 0.018), 0, 0, sd * (0.3 + 0.4 * ef), 8, 5, flat(CP_DK));
        }
        // toalla de aguas termales doblada en la cabeza (blanca con raya azul) y la mandarina encima
        V3 tw = new V3(0, 0.515 + bob, 0.20);
        Pets21.sbox(c, tw, new V3(0.085, 0.022, 0.07), 0.55, 10, 6, (n, p) -> Math.abs(p.x) < 0.012 ? TOWEL2 : TOWEL);
        Pets21.sbox(c, tw.add(0, 0.022, -0.005), new V3(0.075, 0.016, 0.06), 0.55, 10, 6, (n, p) -> Math.abs(p.x) < 0.012 ? TOWEL2 : TOWEL);
        double wob = Math.sin(t * 0.09 + ph) * 0.04;
        V3 oc = new V3(wob * 0.3, 0.585 + bob, 0.20);
        ell(c, oc, new V3(0.068, 0.056, 0.068), 12, 8, (n, p) -> mix(ORANGE, ORANGE2, ss(0, 0.9, n.y) * 0.5));
        Detail.glint(c, oc, new V3(0.068, 0.056, 0.068), new V3(-0.4, 0.7, 0.6), 0.035, WHITE, 0.6);
        ell(c, oc.add(0, 0.056, 0), new V3(0.011, 0.009, 0.011), 6, 4, flat(CP_DK));
        Detail.kite(c, oc.add(0, 0.06, 0), oc.add(0.065 + wob, 0.095, -0.01), new V3(0, 0, 1), 0.02, new V3(0, 1, 0.2).norm(), LEAF,
                mix(LEAF, WHITE, 0.2));
        c.head = false;
        // suspiro de burbujitas
        double sigh = Detail.flick(c, 246);
        if (sigh > 0.05) {
            c.blend = Sink.NORMAL;
            for (int k = 0; k < 3; k++) {
                double u = (sigh + k * 0.25) % 1.0;
                c.alpha = 0.6 * (1 - u);
                double rr = 0.018 + 0.01 * k;
                ell(c, new V3(0.04 * Math.sin(k * 2.0), 0.40 + u * 0.25, 0.44 + u * 0.05), new V3(rr, rr, rr), 8, 5, flat(new float[]{0.85F, 0.95F, 1F}));
            }
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
        Pets21.restore(c, sv);
    }

    /** Punto de la superficie de una caja redondeada ({@link Pets21#sbox}) en la dirección {@code dir} desde su centro. */
    static V3 boxSurf(V3 ce, V3 r, double e, V3 dir) {
        V3 u = dir.norm();
        double k = Math.pow(Math.pow(Math.abs(u.x), 2 / e) + Math.pow(Math.abs(u.y), 2 / e) + Math.pow(Math.abs(u.z), 2 / e), -e / 2);
        return ce.add(r.x * u.x * k, r.y * u.y * k, r.z * u.z * k);
    }
}
