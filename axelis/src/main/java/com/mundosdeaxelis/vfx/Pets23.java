package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas rehechas (etapa 252), más bonitas y en estilo "chibi" (cabeza grande, cuerpo pequeño): Sapo Brote, Cabrita
 * Montañera, Perrito Ninja, Cachorro Rey, Quetzal Dorado, Grifo Imponente y Cerdito Alcancía.
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0, altura total ≈ 1.
 */
final class Pets23 {
    private Pets23() {
    }

    /** Patitas cortas y regordetas (dos o cuatro) que caminan alternadas. */
    static void stubLegs(Ctx c, double xs, double zf, double zb, double hipY, double r, boolean four, Paint leg, Paint foot, double freq) {
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < (four ? 2 : 1); fb++) {
                double z0 = fb == 0 ? zf : zb;
                double p = c.gait * freq + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(p) * 0.05 * c.walk;
                double lift = Math.max(0, Math.cos(p)) * 0.045 * c.walk;
                V3 hip = new V3(sd * xs, hipY, z0), foot0 = new V3(sd * xs, r * 0.8 + lift, z0 + sw);
                tube(c, new V3[]{hip, hip.lerp(foot0, 0.5), foot0}, new double[]{r * 1.1, r, r * 0.95}, 7, leg);
                ell(c, foot0.add(0, -r * 0.35, r * 0.25), new V3(r * 1.1, r * 0.6, r * 1.3), 8, 5, foot);
            }
        }
    }

    static V3[] ring(V3 ce, double rx, double rz, int n) {
        V3[] out = new V3[n + 1];
        for (int i = 0; i <= n; i++) {
            double a = Math.PI * 2 * i / n;
            out[i] = new V3(ce.x + Math.cos(a) * rx, ce.y, ce.z + Math.sin(a) * rz);
        }
        return out;
    }

    static double[] radii(int n, double r) {
        double[] a = new double[n];
        java.util.Arrays.fill(a, r);
        return a;
    }

    // ------------------------------------------------------------------ SAPO BROTE

    private static final float[] FR_G = {0.45F, 0.82F, 0.42F}, FR_G2 = {0.64F, 0.92F, 0.52F}, FR_BELLY = {0.98F, 0.96F, 0.80F},
            FR_SPOT = {0.36F, 0.68F, 0.34F}, SPROUT = {0.40F, 0.78F, 0.30F}, SPROUT2 = {0.62F, 0.92F, 0.42F}, FLOWER = {1F, 0.62F, 0.78F},
            FLOWER_C = {1F, 0.90F, 0.40F};

    /**
     * Sapo Brote: un sapito redondo como un mochi, con ojos grandes arriba, sonrisa enorme, barriga crema y un brote con dos
     * hojas y una florecita en la cabeza. Camina a saltitos.
     */
    static void sproutFrog(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 0.9)) * 0.10 * w;
        double squish = 1 - 0.08 * Math.max(0, Math.cos(c.gait * 1.8)) * w + 0.02 * c.breath;
        c.pivot = new V3(0, 0.2, 0);
        double y = 0.2 + hop;
        Paint skin = (n, p) -> mix(FR_BELLY, mix(FR_G, FR_G2, ss(0, 1, n.y) * 0.5), ss(-0.35, 0.05, n.y + Math.max(0, -n.z) * 0.6));
        // patas traseras dobladas y delanteras
        for (int sd = -1; sd <= 1; sd += 2) {
            double kick = Math.max(0, Math.sin(c.gait * 0.9)) * w;
            V3 hip = new V3(sd * 0.14, y - 0.04, -0.08);
            V3 knee = hip.add(sd * 0.06, -0.02 + 0.02 * kick, 0.07);
            V3 foot = new V3(sd * 0.17, 0.03 + hop * 0.3, 0.03 - kick * 0.12);
            tube(c, new V3[]{hip, knee, foot}, new double[]{0.07, 0.05, 0.035}, 7, skin);
            ell(c, foot.add(0, -0.01, 0.04), new V3(0.06, 0.02, 0.07), 8, 5, flat(FR_G));
            V3 sh = new V3(sd * 0.10, y - 0.06, 0.10), hand = new V3(sd * 0.11, 0.03 + hop * 0.5, 0.15);
            tube(c, new V3[]{sh, sh.lerp(hand, 0.5), hand}, new double[]{0.035, 0.03, 0.028}, 6, skin);
            ell(c, hand.add(0, -0.01, 0.02), new V3(0.04, 0.016, 0.045), 8, 5, flat(FR_G));
        }
        // cuerpo-cabeza (un solo mochi)
        V3 bc = new V3(0, y + 0.02, 0), br = new V3(0.22 / Math.sqrt(squish), 0.19 * squish, 0.20 / Math.sqrt(squish));
        ell(c, bc, br, 18, 12, skin);
        for (int k = 0; k < 5; k++) {
            Detail.spot(c, bc, br, new V3((k % 2 == 0 ? 0.5 : -0.5) + k * 0.05, 0.6, -0.7 + k * 0.18), 0.04 + 0.01 * (k % 2), FR_SPOT);
        }
        // manchitas en el lomo
        for (int xk = 0; xk < 7; xk++) {
            double xa = xk * 2.399;
            Detail.soft(c, bc, br, new V3(Math.cos(xa) * 0.7, 0.65, -0.2 + Math.sin(xa) * 0.6), 0.04 + 0.012 * (xk % 3), new float[]{0.36F, 0.66F, 0.32F}, 0.7);
        }
        headLook(c, 0.10, 0.0);
        c.hpv = bc;
        // ojos saltones arriba
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.10, y + 0.20 * squish, 0.07), er = new V3(0.075, 0.07, 0.07);
            ell(c, ec, er, 12, 8, skin);
            decal(c, ec, er, new V3(sd * 0.15, 0.1, 1), 0.10, 0, c.shut ? T_EYE_SHUT : T_BEAD, c.shut ? new float[]{0.2F, 0.4F, 0.2F} : WHITE, 1, 0.004);
        }
        cheeks(c, bc, br, 0.62, -0.05, 0.75, 0.09);
        decal(c, bc, br, new V3(0, -0.10, 1), 0.20, 0, T_MOUTH, new float[]{0.25F, 0.35F, 0.20F}, 0.95, 0.004);
        // brote con florecita
        double sway = Math.sin(t * 0.13 + ph) * 0.06 + Math.sin(c.gait * 0.9) * 0.05 * w;
        V3 s0 = new V3(0, y + 0.20, -0.02), s1 = s0.add(sway, 0.08, 0), s2 = s1.add(sway * 1.4, 0.06, 0);
        curve(c, new V3[]{s0, s1, s2}, new double[]{0.014, 0.012, 0.010}, 3, 5, flat(SPROUT));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 lb = s1.add(0, 0.01, 0);
            V3 lt = lb.add(sd * 0.11, 0.05 + 0.02 * Math.sin(t * 0.2 + sd), 0.0);
            Detail.feather(c, lb, lt, new V3(0, 0, 1), 0.045, new V3(0, 1, 0.1).norm(), SPROUT, SPROUT2);
        }
        for (int k = 0; k < 5; k++) {
            double a = Math.PI * 2 * k / 5 + t * 0.01;
            ell(c, s2.add(Math.cos(a) * 0.025, 0.012, Math.sin(a) * 0.025), new V3(0.02, 0.01, 0.02), 6, 4, flat(FLOWER));
        }
        ell(c, s2.add(0, 0.018, 0), new V3(0.014, 0.01, 0.014), 6, 4, flat(FLOWER_C));
        // saco de la garganta que se infla de vez en cuando (croac)
        {
            double xu = ((t + ph * 17) % 200) / 26.0;
            double xc = xu < 1 ? Math.sin(xu * Math.PI) : 0;
            if (xc > 0.02) {
                V3 xg = surf(bc, br, new V3(0, -0.45, 1)).add(0, -0.01, -0.02);
                ell(c, xg, new V3(0.06 * xc, 0.05 * xc, 0.045 * xc), 9, 6, (n, q) -> mix(new float[]{1F, 0.95F, 0.78F}, WHITE, ss(0, 1, n.y) * 0.4));
            }
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ CABRITA MONTAÑERA

    private static final float[] GT_W = {0.97F, 0.96F, 0.94F}, GT_W2 = {0.88F, 0.86F, 0.84F}, GT_SKIN = {0.98F, 0.86F, 0.84F},
            GT_HORN = {0.86F, 0.78F, 0.62F}, GT_HOOF = {0.38F, 0.30F, 0.28F}, SCARF = {0.92F, 0.28F, 0.30F}, SCARF2 = {1F, 0.48F, 0.44F},
            BELL = {1F, 0.80F, 0.30F};

    /**
     * Cabrita Montañera: una cabrita bebé de lana esponjosa, con cuernitos enroscados, orejas caídas, perilla, una bufanda
     * roja de exploradora que ondea y una campanita. Da brinquitos de alegría.
     */
    static void mountainGoat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.03 * w;
        c.pivot = new V3(0, 0.3, 0);
        Paint wool = (n, p) -> mix(GT_W2, GT_W, ss(-0.5, 0.6, n.y));
        stubLegs(c, 0.085, 0.09, -0.10, 0.20 + bob, 0.035, true, flat(GT_W), flat(GT_HOOF), 1.0);
        V3 bc = new V3(0, 0.28 + bob + 0.006 * c.breath, -0.02), br = new V3(0.16, 0.14, 0.19);
        ell(c, bc, br, 14, 10, wool);
        Detail.fur(c, bc, br, 220, 811, 0.035, 0.02, new V3(0, 0.2, -0.4), 0.3, Detail.ALL, wool, GT_W);
        // colita
        ell(c, bc.add(0, 0.08, -0.19), new V3(0.04, 0.05, 0.035), 0, -0.6 + Math.sin(t * 0.4 + ph) * 0.3, 0, 8, 5, wool);
        // bufanda: vuelta al cuello y dos puntas que ondean hacia atrás
        V3 nk = new V3(0, 0.37 + bob, 0.10);
        tube(c, ring(nk, 0.095, 0.085, 14), radii(15, 0.03), 6, (n, p) -> mix(SCARF, SCARF2, ss(-0.3, 0.9, n.y) * 0.5));
        for (int k = 0; k < 2; k++) {
            double fl = Math.sin(t * 0.25 + ph + k) * 0.04 + 0.05 * w;
            V3 a = nk.add(0.06 - k * 0.03, -0.01, -0.07), b = a.add(0.05 + fl, -0.06, -0.10 - 0.08 * w), d = b.add(0.03 + fl, -0.03, -0.08);
            curve(c, new V3[]{a, b, d}, new double[]{0.028, 0.024, 0.02}, 3, 5, flat(k == 0 ? SCARF : SCARF2));
        }
        double ring = Math.sin(t * 0.3 + ph) * 0.15;
        V3 bell = nk.add(0, -0.06, 0.08);
        ell(c, bell, new V3(0.028, 0.03, 0.028), 0, 0, ring, 8, 6, flat(BELL));
        Detail.glint(c, bell, new V3(0.028, 0.03, 0.028), new V3(-0.4, 0.5, 0.8), 0.02, WHITE, 0.8);
        // cabeza
        headLook(c, 0.11, 0.04);
        c.hpv = new V3(0, 0.40, 0.12);
        V3 hc = new V3(0, 0.50 + bob, 0.15), hr = new V3(0.15, 0.135, 0.14);
        ell(c, hc, hr, 16, 11, wool);
        Detail.fur(c, hc, hr, 60, 812, 0.03, 0.016, new V3(0, 0.2, -1), 0.6, d -> ss(0.3, 0.8, d.y), wool, GT_W);
        V3 sn = new V3(0, 0.46 + bob, 0.26), sr = new V3(0.075, 0.06, 0.06);
        ell(c, sn, sr, 12, 8, flat(GT_SKIN));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(sn, sr, new V3(sd * 0.4, 0.2, 1)).add(0, 0, -0.003), new V3(0.009, 0.007, 0.005), 5, 3, flat(new float[]{0.7F, 0.45F, 0.45F}));
        }
        decal(c, sn, sr, new V3(0, -0.5, 1), 0.06, 0, T_CAT, new float[]{0.55F, 0.35F, 0.35F}, 0.9, 0.003);
        // perilla
        ell(c, new V3(0, 0.39 + bob, 0.24), new V3(0.02, 0.04, 0.018), 0, 0.3, 0, 6, 5, flat(GT_W2));
        eyes(c, hc, hr, 0.50, 0.12, 0.84, 0.12, T_EYE, new float[]{0.65F, 0.50F, 0.35F});
        cheeks(c, hc, hr, 0.72, -0.15, 0.62, 0.08);
        for (int sd = -1; sd <= 1; sd += 2) {
            // cuernitos enroscados
            V3 h0 = new V3(sd * 0.06, 0.61 + bob, 0.12);
            V3[] hp = {h0, h0.add(sd * 0.03, 0.05, -0.03), h0.add(sd * 0.07, 0.06, -0.08), h0.add(sd * 0.08, 0.02, -0.10)};
            curve(c, hp, new double[]{0.028, 0.022, 0.016, 0.008}, 3, 6, (n, p) -> mix(GT_HORN, WHITE, 0.15 + 0.1 * Math.sin(p.y * 120)));
            // orejas caídas
            double ef = Detail.flick(c, sd > 0 ? 91 : 92);
            ell(c, new V3(sd * 0.16, 0.52 + bob, 0.11), new V3(0.075, 0.025, 0.035), sd * 0.15, 0, sd * (-0.45 + 0.35 * ef), 10, 6, flat(GT_W));
            ell(c, new V3(sd * 0.165, 0.518 + bob, 0.125), new V3(0.055, 0.015, 0.02), sd * 0.15, 0, sd * (-0.45 + 0.35 * ef), 8, 5, flat(GT_SKIN));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ PERRITO NINJA

    private static final float[] DG_OR = {0.96F, 0.62F, 0.30F}, DG_OR2 = {1F, 0.76F, 0.45F}, DG_CR = {1F, 0.96F, 0.88F},
            DG_DK = {0.22F, 0.16F, 0.14F}, NJ = {0.16F, 0.18F, 0.26F}, NJ2 = {0.28F, 0.32F, 0.46F}, NJ_RED = {0.92F, 0.26F, 0.30F},
            STEEL = {0.80F, 0.84F, 0.90F};

    /**
     * Perrito Ninja: un cachorrito naranja y crema con la colita enroscada, una cinta ninja azul noche con placa de metal en la
     * frente y dos cintas largas que ondean, y una bufanda roja. Cuando está contento hace una "pose ninja" con la patita.
     */
    static void ninjaPup(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.1)) * 0.03 * w;
        c.pivot = new V3(0, 0.3, 0);
        Paint fur = (n, p) -> mix(DG_CR, mix(DG_OR, DG_OR2, ss(0, 1, n.y) * 0.4), ss(-0.4, 0.05, n.y));
        stubLegs(c, 0.08, 0.10, -0.10, 0.19 + bob, 0.036, true, (n, p) -> mix(DG_CR, DG_OR, ss(0.08, 0.16, p.y)), flat(DG_CR), 1.1);
        V3 bc = new V3(0, 0.27 + bob, -0.02), br = new V3(0.14, 0.13, 0.18);
        ell(c, bc, br, 14, 10, fur);
        Detail.fur(c, bc, br, 120, 911, 0.028, 0.013, new V3(0, -0.4, -1), 0.8, Detail.ALL, fur, DG_OR2);
        // colita enroscada
        double wag = Math.sin(t * (0.5 + 0.6 * c.joy) + ph) * (0.10 + 0.10 * c.joy);
        V3 t0 = bc.add(0, 0.08, -0.17);
        V3[] tc = {t0, t0.add(wag * 0.4, 0.08, -0.03), t0.add(wag, 0.12, 0.03), t0.add(wag * 0.8, 0.08, 0.07)};
        curve(c, tc, new double[]{0.045, 0.05, 0.045, 0.03}, 3, 7, (n, p) -> mix(DG_OR, DG_CR, ss(0.0, 0.8, -n.z)));
        // bufanda roja
        V3 nk = new V3(0, 0.36 + bob, 0.09);
        tube(c, ring(nk, 0.09, 0.08, 14), radii(15, 0.026), 6, flat(NJ_RED));
        // estrellita ninja (shuriken) colgada del nudo de la bufanda
        {
            V3 xs = nk.add(0.05, -0.05, 0.075);
            double xr = Math.sin(t * 0.1 + ph) * 0.3;
            for (int xk = 0; xk < 4; xk++) {
                double xa = xk * Math.PI / 2 + xr;
                V3 xd = new V3(Math.cos(xa), Math.sin(xa), 0);
                Detail.kite(c, xs, xs.add(xd.mul(0.028)), new V3(-xd.y, xd.x, 0), 0.008, new V3(0, 0, 1), new float[]{0.7F, 0.74F, 0.8F}, new float[]{0.92F, 0.94F, 1F});
            }
            ell(c, xs, new V3(0.007, 0.007, 0.004), 5, 3, flat(new float[]{0.3F, 0.32F, 0.38F}));
        }
        // cabeza
        headLook(c, 0.12, 0.03);
        c.hpv = new V3(0, 0.40, 0.10);
        V3 hc = new V3(0, 0.49 + bob, 0.12), hr = new V3(0.165, 0.145, 0.15);
        Paint hp = (n, p) -> mix(DG_CR, mix(DG_OR, DG_OR2, 0.3), ss(-0.35, 0.15, n.y + 0.3 * Math.max(0, -n.z)));
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 912, 0.026, 0.012, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, hp, null);
        V3 sn = new V3(0, 0.45 + bob, 0.25), sr = new V3(0.07, 0.055, 0.065);
        ell(c, sn, sr, 12, 8, flat(DG_CR));
        ell(c, surf(sn, sr, new V3(0, 0.3, 1)).add(0, 0, 0.002), new V3(0.022, 0.016, 0.014), 6, 4, flat(DG_DK));
        decal(c, sn, sr, new V3(0, -0.45, 1), 0.06, 0, T_CAT, new float[]{0.35F, 0.20F, 0.18F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.46, 0.10, 0.86, 0.12, T_EYE, new float[]{0.55F, 0.40F, 0.25F});
        cheeks(c, hc, hr, 0.72, -0.18, 0.62, 0.08);
        // orejas puntiagudas
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 93 : 94);
            V3 b = new V3(sd * 0.09, 0.60 + bob, 0.10), tp = new V3(sd * (0.14 + 0.02 * ef), 0.72 - 0.03 * ef + bob, 0.08);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.055, 0.04, 0.004}, 5, flat(DG_OR));
            V3 b2 = b.add(0, 0.005, 0.02), t2 = tp.add(-sd * 0.006, -0.04, 0.018);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.035, 0.025, 0.004}, 4, flat(DG_CR));
        }
        // cinta ninja: banda alrededor de la frente, placa de metal y dos cintas que ondean
        V3 bandC = new V3(0, 0.555 + bob, 0.12);
        V3[] band = new V3[17];
        for (int i = 0; i <= 16; i++) {
            double a = Math.PI * 2 * i / 16.0;
            band[i] = new V3(Math.cos(a) * 0.168, bandC.y + 0.01 * Math.cos(a), bandC.z + Math.sin(a) * 0.152);
        }
        tube(c, band, radii(17, 0.016), 5, flat(NJ));
        Pets21.sbox(c, new V3(0, bandC.y, bandC.z + 0.155), new V3(0.05, 0.025, 0.008), 0.4, 10, 8, flat(STEEL));
        Detail.glint(c, new V3(0, bandC.y, bandC.z + 0.165), new V3(0.05, 0.025, 0.008), new V3(-0.3, 0.4, 1), 0.02, WHITE, 0.7);
        c.head = false;
        for (int k = 0; k < 2; k++) {
            double fl = Math.sin(t * 0.3 + ph + k * 1.4) * 0.05 + w * 0.06;
            V3 a = new V3(0.02 - k * 0.04, 0.56 + bob, -0.04);
            V3 b = a.add(0.03 * (1 - 2 * k) + fl * 0.5, -0.02 + 0.03 * w, -0.10);
            V3 d = b.add(0.02 * (1 - 2 * k) + fl, -0.05 + 0.06 * w, -0.10);
            curve(c, new V3[]{a, b, d}, new double[]{0.016, 0.016, 0.012}, 3, 4, flat(k == 0 ? NJ : NJ2));
        }
    }

    // ------------------------------------------------------------------ CACHORRO REY

    private static final float[] LN_Y = {1F, 0.80F, 0.42F}, LN_Y2 = {1F, 0.88F, 0.58F}, LN_MANE = {0.92F, 0.52F, 0.20F},
            LN_MANE2 = {1F, 0.66F, 0.30F}, LN_CR = {1F, 0.96F, 0.86F}, GOLD = {1F, 0.82F, 0.25F}, GOLD2 = {1F, 0.94F, 0.60F},
            RUBY = {0.95F, 0.20F, 0.32F};

    /**
     * Cachorro Rey: un cachorro de león sentado, con una melena corta y esponjosa, ojos ámbar enormes, una corona dorada
     * pequeña (un poco ladeada) con un rubí y la colita con borla.
     */
    static void lionCub(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.03 * w;
        c.pivot = new V3(0, 0.28, 0);
        Paint fur = (n, p) -> mix(LN_CR, mix(LN_Y, LN_Y2, ss(0, 1, n.y) * 0.4), ss(-0.4, 0.05, n.y));
        stubLegs(c, 0.085, 0.10, -0.10, 0.19 + bob, 0.04, true, fur, flat(LN_CR), 1.0);
        V3 bc = new V3(0, 0.27 + bob, -0.03), br = new V3(0.15, 0.135, 0.18);
        ell(c, bc, br, 14, 10, fur);
        Detail.fur(c, bc, br, 100, 1011, 0.025, 0.012, new V3(0, -0.4, -1), 0.8, Detail.ALL, fur, LN_Y2);
        // cola con borla
        double sw = Math.sin(t * 0.2 + ph) * 0.08;
        V3[] tc = {bc.add(0, 0.04, -0.17), bc.add(sw * 0.4, 0.08, -0.28), bc.add(sw, 0.16, -0.32), bc.add(sw * 1.2, 0.22, -0.30)};
        curve(c, tc, new double[]{0.025, 0.02, 0.018, 0.016}, 3, 6, fur);
        V3 tuft = tc[3].add(0, 0.03, 0.01);
        ell(c, tuft, new V3(0.045, 0.05, 0.045), 10, 7, flat(LN_MANE));
        Detail.fur(c, tuft, new V3(0.045, 0.05, 0.045), 30, 1012, 0.03, 0.014, new V3(0, 1, 0), 0.4, Detail.ALL, flat(LN_MANE), LN_MANE2);
        headLook(c, 0.12, 0.03);
        c.hpv = new V3(0, 0.40, 0.10);
        V3 hc = new V3(0, 0.49 + bob, 0.12), hr = new V3(0.16, 0.145, 0.145);
        // melena: un aro esponjoso detrás de la cara
        V3 mc = hc.add(0, -0.01, -0.04), mr = new V3(0.23, 0.19, 0.14);
        ell(c, mc, mr, 16, 11, (n, p) -> mix(LN_MANE, LN_MANE2, ss(-0.4, 0.9, n.y) * 0.5));
        Detail.fur(c, mc, mr, 200, 1013, 0.05, 0.022, new V3(0, 0, -1), 0.4, Detail.ALL, (n, p) -> LN_MANE, LN_MANE2);
        ell(c, hc, hr, 16, 11, fur);
        V3 sn = new V3(0, 0.45 + bob, 0.245), sr = new V3(0.075, 0.055, 0.06);
        ell(c, sn, sr, 12, 8, flat(LN_CR));
        ell(c, surf(sn, sr, new V3(0, 0.4, 1)).add(0, 0, 0.002), new V3(0.022, 0.015, 0.012), 6, 4, flat(new float[]{0.85F, 0.45F, 0.45F}));
        decal(c, sn, sr, new V3(0, -0.45, 1), 0.06, 0, T_CAT, new float[]{0.45F, 0.25F, 0.20F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.46, 0.12, 0.86, 0.125, T_EYE, new float[]{1F, 0.72F, 0.30F});
        cheeks(c, hc, hr, 0.72, -0.18, 0.62, 0.08);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ec = new V3(sd * 0.12, 0.60 + bob, 0.09);
            ell(c, ec, new V3(0.045, 0.04, 0.025), 8, 6, fur);
            ell(c, ec.add(0, -0.004, 0.012), new V3(0.028, 0.024, 0.012), 6, 5, flat(LN_CR));
        }
        // corona ladeada
        double tilt = -0.25;
        V3 cr = new V3(0.03, 0.665 + bob, 0.13);
        V3[] band = new V3[13];
        for (int i = 0; i <= 12; i++) {
            double a = Math.PI * 2 * i / 12.0;
            band[i] = cr.add(Math.cos(a) * 0.07, Math.cos(a) * 0.07 * Math.sin(tilt), Math.sin(a) * 0.065);
        }
        tube(c, band, radii(13, 0.016), 6, (n, p) -> mix(GOLD, GOLD2, ss(-0.2, 0.9, n.y) * 0.6));
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5.0 + 0.3;
            V3 b = cr.add(Math.cos(a) * 0.07, Math.cos(a) * 0.07 * Math.sin(tilt) + 0.01, Math.sin(a) * 0.065);
            cone(c, b, b.add(0, 0.055, 0), 0.016, 0.004, 5, flat(GOLD));
            ell(c, b.add(0, 0.058, 0), new V3(0.008, 0.008, 0.008), 5, 3, flat(GOLD2));
        }
        c.emissive = true;
        ell(c, cr.add(0, 0.0, 0.07), new V3(0.016, 0.016, 0.008), 6, 4, flat(RUBY));
        c.emissive = false;
        Detail.twinkle(c, cr.add(0, 0.02, 0.075), 0.012, WHITE, t, ph, 0.25);
        c.head = false;
    }

    // ------------------------------------------------------------------ QUETZAL DORADO

    private static final float[] QZ_G = {0.18F, 0.74F, 0.46F}, QZ_G2 = {0.36F, 0.92F, 0.62F}, QZ_RED = {0.92F, 0.22F, 0.26F},
            QZ_TEAL = {0.15F, 0.62F, 0.72F}, QZ_BEAK = {1F, 0.84F, 0.30F};

    /**
     * Quetzal Dorado: una bolita de plumas verde esmeralda con la barriga roja, pico dorado, cresta esponjosa y dos plumas de la
     * cola larguísimas que ondean como cintas. Camina a saltitos y sacude las alas.
     */
    static void quetzal(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 1.2)) * 0.05 * w;
        c.pivot = new V3(0, 0.3, 0);
        double y = 0.30 + hop;
        // plumas largas de la cola (dos cintas)
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] cp = new V3[7];
            for (int i = 0; i < 7; i++) {
                double f = i / 6.0;
                double wave = Math.sin(t * 0.18 + ph - f * 3.0 + sd) * 0.05 * f + w * 0.05 * f;
                cp[i] = new V3(sd * (0.02 + f * 0.04) + wave, y - 0.06 - f * 0.18 + f * f * 0.12, -0.12 - f * 0.55);
            }
            double[] rad = {0.035, 0.03, 0.026, 0.024, 0.022, 0.018, 0.008};
            curve(c, cp, rad, 3, 5, (n, p) -> mix(QZ_G, QZ_TEAL, ss(-0.2, -0.6, p.z)));
        }
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.2 + (sd > 0 ? 0 : Math.PI);
            V3 a = new V3(sd * 0.05, y - 0.15, 0.01), b = new V3(sd * 0.055, 0.02 + Math.max(0, Math.cos(p)) * 0.03 * w, 0.02 + Math.sin(p) * 0.02 * w);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.012, 0.01, 0.01}, 5, flat(QZ_BEAK));
            Detail.toes(c, b, 0.016, 0.03, 0.007, QZ_BEAK);
        }
        // cuerpo-bolita
        V3 bc = new V3(0, y + 0.02, 0), br = new V3(0.21, 0.20, 0.20);
        Paint body = (n, p) -> mix(QZ_RED, mix(QZ_G, QZ_G2, ss(0, 1, n.y) * 0.5), ss(-0.35, 0.05, n.y + 0.4 * Math.max(0, -n.z)));
        ell(c, bc, br, 16, 11, body);
        Detail.plumage(c, bc, br, 5, 16, 0.05, 0.03, Detail.and(Detail.TOP, Detail.BACK), body, QZ_G2);
        // alas que se sacuden
        double flap = Math.max(Detail.flick(c, 101), c.joy) ;
        for (int sd = -1; sd <= 1; sd += 2) {
            double open = 0.2 + flap * 0.9 * Math.abs(Math.sin(t * 0.9));
            V3 base = new V3(sd * 0.15, y + 0.04, -0.01);
            for (int k = 0; k < 4; k++) {
                V3 tip = base.add(sd * (0.04 + open * 0.12), -0.10 - k * 0.02 + open * 0.10, -0.08 - k * 0.04);
                Detail.feather(c, base.add(0, -k * 0.015, 0), tip, new V3(0, 0, 1), 0.035, new V3(sd, 0.3, 0).norm(), QZ_G, k > 1 ? QZ_TEAL : QZ_G2);
            }
        }
        // plumitas en escamas sobre el pecho rojo y brillo iridiscente
        Detail.plumage(c, bc, br, 4, 12, 0.035, 0.014, d -> d.z > 0.35 && d.y < 0.2 ? 1 : 0, (n, q) -> QZ_RED, new float[]{1F, 0.45F, 0.45F});
        Detail.glint(c, bc, br, new V3(-0.4, 0.8, 0.3), 0.06, new float[]{0.7F, 1F, 0.8F}, 0.5);
        headLook(c, 0.12, 0.02);
        c.hpv = bc;
        eyes(c, bc, br, 0.42, 0.30, 0.80, 0.13, T_EYE, new float[]{0.35F, 0.25F, 0.15F});
        cheeks(c, bc, br, 0.66, 0.08, 0.72, 0.07);
        V3 bk = surf(bc, br, new V3(0, 0.12, 1));
        cone(c, bk.add(0, 0, -0.01), bk.add(0, -0.015, 0.06), 0.03, 0.003, 6, flat(QZ_BEAK));
        // cresta esponjosa
        for (int k = 0; k < 5; k++) {
            double a = (k - 2) * 0.22;
            V3 b0 = new V3(Math.sin(a) * 0.03, y + 0.15, 0.03 - k * 0.012);
            Detail.feather(c, b0, b0.add(Math.sin(a) * 0.05, 0.08 + 0.01 * Math.sin(t * 0.3 + k), -0.03), new V3(0, 0, 1), 0.02,
                    new V3(0, 0, 1), QZ_G, QZ_G2);
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ GRIFO IMPONENTE

    private static final float[] GR_W = {0.98F, 0.98F, 0.96F}, GR_W2 = {0.88F, 0.88F, 0.90F}, GR_GOLD = {0.95F, 0.72F, 0.32F},
            GR_GOLD2 = {1F, 0.84F, 0.50F}, GR_BEAK = {1F, 0.80F, 0.22F}, GR_EYE = {0.95F, 0.70F, 0.20F}, GR_BLUE = {0.30F, 0.42F, 0.80F};

    /**
     * Grifo Imponente: cabeza de águila blanca con pico dorado y cresta, cuerpo de león dorado, alas grandes de plumas blancas
     * y doradas, cola con borla y un pequeño medallón azul. Vuela batiendo las alas con fuerza.
     */
    static void royalGriffon(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.13 + ph) * 0.035;
        double fl = Math.sin(t * 0.32 + ph);
        c.pivot = new V3(0, 0.4, 0);
        double y = 0.38 + bob;
        Paint lion = (n, p) -> mix(GR_GOLD, GR_GOLD2, ss(-0.4, 0.9, n.y) * 0.5);
        // alas: capas de plumas
        for (int sd = -1; sd <= 1; sd += 2) {
            double up = 0.35 + fl * 0.65;
            V3 root = new V3(sd * 0.10, y + 0.07, 0.0);
            // alas en alto, como en un escudo de armas: así se ve más imponente (y cabe mejor en el icono)
            V3 elbow = root.add(sd * 0.09, 0.13 + up * 0.10, -0.06);
            V3 hand = elbow.add(sd * (0.05 + 0.04 * (1 - up)), 0.15 + up * 0.06, -0.08);
            tube(c, new V3[]{root, elbow, hand}, new double[]{0.03, 0.024, 0.016}, 6, flat(GR_W));
            for (int k = 0; k < 7; k++) {
                double f = k / 6.0;
                V3 base = root.lerp(elbow, Math.min(1, f * 2)).lerp(hand, Math.max(0, f * 2 - 1));
                V3 tip = base.add(sd * (0.06 + 0.04 * f), -0.17 - 0.05 * f + up * 0.05, -0.12 - 0.05 * f);
                Detail.feather(c, base, tip, new V3(0, 0, 1), 0.045, new V3(0, 1, 0.2).norm(), GR_W, f > 0.6 ? GR_GOLD : GR_W2);
            }
            for (int k = 0; k < 5; k++) {
                double f = k / 4.0;
                V3 base = root.lerp(elbow, 0.3 + f * 0.7);
                Detail.feather(c, base, base.add(sd * 0.03, -0.08, -0.05), new V3(0, 0, 1), 0.035, new V3(0, 1, 0.2).norm(), GR_W, GR_W2);
            }
        }
        // cuerpo de león con patitas colgando y cola
        V3 bc = new V3(0, y - 0.06, -0.06), br = new V3(0.13, 0.12, 0.18);
        ell(c, bc, br, 14, 10, lion);
        Detail.fur(c, bc, br, 90, 1111, 0.025, 0.012, new V3(0, -0.3, -1), 0.8, Detail.ALL, lion, GR_GOLD2);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.07, y - 0.17, -0.14), new V3(0.04, 0.05, 0.05), 8, 5, lion);
            V3 a = new V3(sd * 0.07, y - 0.10, 0.06), b = a.add(0, -0.08, 0.02 + Math.sin(t * 0.2 + sd) * 0.01);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.028, 0.024, 0.02}, 5, flat(GR_BEAK));
            Detail.toes(c, b, 0.02, 0.03, 0.009, GR_BEAK);
        }
        double sw = Math.sin(t * 0.18 + ph) * 0.06;
        V3[] tc = {bc.add(0, 0.0, -0.17), bc.add(sw, -0.05, -0.28), bc.add(sw * 1.5, -0.02, -0.38)};
        curve(c, tc, new double[]{0.022, 0.018, 0.015}, 3, 5, lion);
        ell(c, tc[2].add(0, 0, -0.02), new V3(0.03, 0.035, 0.04), 8, 5, flat(GR_GOLD));
        // pechera de plumas blancas y medallón
        V3 ch = new V3(0, y + 0.02, 0.06), chr = new V3(0.12, 0.13, 0.11);
        ell(c, ch, chr, 14, 10, flat(GR_W));
        Detail.plumage(c, ch, chr, 4, 12, 0.04, 0.025, Detail.FRONT, flat(GR_W), GR_W2);
        c.emissive = true;
        ell(c, surf(ch, chr, new V3(0, -0.2, 1)).add(0, 0, 0.004), new V3(0.025, 0.025, 0.01), 8, 6, flat(GR_BLUE));
        c.emissive = false;
        // cabeza de águila
        headLook(c, 0.12, 0.0);
        c.hpv = new V3(0, y + 0.12, 0.08);
        V3 hc = new V3(0, y + 0.20, 0.10), hr = new V3(0.13, 0.12, 0.125);
        ell(c, hc, hr, 16, 11, flat(GR_W));
        Detail.plumage(c, hc, hr, 4, 12, 0.035, 0.02, Detail.BACK, flat(GR_W), GR_W2);
        V3 bk = surf(hc, hr, new V3(0, -0.1, 1));
        V3[] beak = {bk.add(0, 0.02, -0.02), bk.add(0, 0.015, 0.04), bk.add(0, -0.03, 0.07)};
        curve(c, beak, new double[]{0.04, 0.026, 0.004}, 3, 6, (n, p) -> mix(GR_BEAK, GR_GOLD2, ss(0, 1, n.y) * 0.5));
        eyes(c, hc, hr, 0.50, 0.15, 0.82, 0.11, T_EYE, GR_EYE);
        cheeks(c, hc, hr, 0.75, -0.12, 0.6, 0.07);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.5, 0.45, 0.72, 0.05, sd * 0.4, GR_W2);
        }
        for (int k = 0; k < 3; k++) {
            V3 b0 = new V3((k - 1) * 0.03, y + 0.30, 0.06 - k * 0.01);
            Detail.feather(c, b0, b0.add((k - 1) * 0.04, 0.07, -0.07), new V3(0, 0, 1), 0.02, new V3(0, 0, 1), GR_W, GR_GOLD);
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ CERDITO ALCANCÍA

    private static final float[] PG = {1F, 0.66F, 0.76F}, PG2 = {1F, 0.82F, 0.88F}, PG_DK = {0.86F, 0.46F, 0.58F},
            PG_FLOWER = {1F, 1F, 1F}, COIN = {1F, 0.80F, 0.22F}, COIN2 = {1F, 0.94F, 0.55F};

    /**
     * Cerdito Alcancía: una alcancía de cerámica rosa brillante, redondita, con flores pintadas, ranura en el lomo, hocico
     * grande y patitas cortas. Cuando está contento le cae una moneda dorada por la ranura (con su destello).
     */
    static void piggyBank(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.025 * w;
        double rock = Math.sin(c.gait * 1.2) * 0.08 * w;
        c.pivot = new V3(0, 0.25, 0);
        c.gloss = 2.4;
        double[] sv = Pets21.tiltAll(c, 0, rock);
        Paint cer = (n, p) -> mix(PG, PG2, ss(-0.3, 0.9, n.y) * 0.6);
        stubLegs(c, 0.10, 0.10, -0.10, 0.12 + bob, 0.04, true, cer, flat(PG_DK), 1.2);
        V3 bc = new V3(0, 0.27 + bob, 0), br = new V3(0.21, 0.19, 0.24);
        ell(c, bc, br, 18, 12, cer);
        // corazón dorado pintado en el lomo
        for (int xs = -1; xs <= 1; xs += 2) {
            Detail.spot(c, bc, br, new V3(xs * 0.08, 0.9, -0.55), 0.06, new float[]{1F, 0.8F, 0.35F});
        }
        Detail.leaf(c, bc, br, new V3(0, 0.82, -0.6), 0.07, Math.PI, new float[]{1F, 0.8F, 0.35F});
        // brillo de cerámica
        Detail.soft(c, bc, br, new V3(-0.45, 0.75, 0.35), 0.16, WHITE, 0.75);
        Detail.soft(c, bc, br, new V3(-0.3, 0.6, 0.55), 0.06, WHITE, 0.9);
        // flores pintadas en los costados
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 d = new V3(sd, 0.1 + 0.25 * (k - 1), -0.2 + 0.3 * (k % 2));
                for (int pe = 0; pe < 5; pe++) {
                    double a = Math.PI * 2 * pe / 5;
                    Detail.spot(c, bc, br, d.add(0, Math.cos(a) * 0.09, Math.sin(a) * 0.09), 0.035, PG_FLOWER);
                }
                Detail.spot(c, bc, br, d, 0.03, COIN);
            }
        }
        // ranura en el lomo y moneda que cae
        ell(c, surf(bc, br, new V3(0, 1, -0.1)).add(0, 0.002, 0), new V3(0.06, 0.006, 0.014), 8, 4, flat(PG_DK.clone()));
        double coin = Math.max(c.joy, Detail.flick(c, 121));
        if (coin > 0.02) {
            double cy = bc.y + br.y + 0.20 * (1 - coin) + 0.01;
            V3 cc = new V3(0, cy, -0.02);
            ell(c, cc, new V3(0.008, 0.045, 0.045), Math.PI / 2 * 0, 0, 0, 12, 6, (n, p) -> mix(COIN, COIN2, ss(-0.3, 0.9, n.y + n.x * 0.3)));
            Detail.twinkle(c, cc.add(0, 0.03, 0.02), 0.02, COIN2, t, ph, 0.5);
        }
        // colita enroscada
        V3 tb = bc.add(0, 0.05, -0.24);
        V3[] tl = new V3[6];
        for (int i = 0; i < 6; i++) {
            double a = i * 1.1 + Math.sin(t * 0.3 + ph) * 0.3;
            tl[i] = tb.add(Math.cos(a) * 0.025, Math.sin(a) * 0.025 + i * 0.006, -i * 0.012);
        }
        curve(c, tl, radii(6, 0.011), 3, 5, flat(PG));
        // cara
        headLook(c, 0.06, 0.02);
        c.hpv = bc;
        V3 sn = surf(bc, br, new V3(0, -0.05, 1)).add(0, 0, 0.02), sr = new V3(0.085, 0.07, 0.045);
        ell(c, sn, sr, 14, 8, flat(PG2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, surf(sn, sr, new V3(sd * 0.45, 0, 1)).add(0, 0, -0.004), new V3(0.016, 0.022, 0.006), 6, 4, flat(PG_DK));
        }
        eyes(c, bc, br, 0.42, 0.25, 0.86, 0.12, T_EYE, new float[]{0.55F, 0.35F, 0.55F});
        cheeks(c, bc, br, 0.66, -0.05, 0.72, 0.10);
        decal(c, bc, br, new V3(0, -0.40, 1), 0.07, 0, T_MOUTH, new float[]{0.55F, 0.25F, 0.35F}, 0.9, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 122 : 123);
            V3 eb = new V3(sd * 0.12, bc.y + 0.16, 0.10), et = eb.add(sd * 0.05, 0.07 - 0.03 * ef, 0.04 + 0.03 * ef);
            tube(c, new V3[]{eb, eb.lerp(et, 0.5), et}, new double[]{0.045, 0.032, 0.004}, 5, cer);
        }
        c.head = false;
        Pets21.restore(c, sv);
        c.gloss = 1;
    }
}
