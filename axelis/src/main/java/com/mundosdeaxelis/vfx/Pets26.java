package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Etapa 253: diseños nuevos y propios para cuatro mascotas que se parecían demasiado a personajes conocidos (y de paso con
 * mucho más detalle): Hámster Glotón, Ratón Relámpago, Droide Minero y Espíritu del Bosque.
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0.
 */
final class Pets26 {
    private Pets26() {
    }

    // ------------------------------------------------------------------ HÁMSTER GLOTÓN

    private static final float[] HM_OR = {0.97F, 0.66F, 0.34F}, HM_OR2 = {1F, 0.80F, 0.52F}, HM_BACK = {0.86F, 0.52F, 0.26F},
            HM_W = {1F, 0.97F, 0.92F}, HM_PINK = {1F, 0.72F, 0.74F}, HM_NOSE = {0.95F, 0.52F, 0.58F}, SEED = {0.22F, 0.20F, 0.22F},
            SEED_ST = {0.88F, 0.88F, 0.82F}, HM_IN = {1F, 0.80F, 0.80F};

    /**
     * Hámster Glotón: hámster dorado y redondo, sentado, con barriga y hocico blancos, mofletes enormes llenos de comida que
     * se inflan al masticar, orejitas rosadas, bigotes, dientecitos y una pipa de girasol a rayas entre las manitas. Quieto,
     * mordisquea la pipa (las manos suben a la boca); al andar corretea a saltitos.
     */
    static void hamster(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk, idle = 1 - w;
        double bob = Math.abs(Math.sin(c.gait * 1.3)) * 0.03 * w;
        double nU = ((t + ph * 11) % 150) / 60.0;
        double nib = nU < 1 ? Math.sin(nU * Math.PI) * idle * (1 - c.sleep) : 0;
        double chew = nib * Math.sin(t * 1.6);
        double sniff = Math.sin(t * 1.1 + ph) * 0.004 * (0.3 + idle);
        c.pivot = new V3(0, 0.2, 0);
        Paint fur = (n, p) -> {
            float[] b = mix(HM_OR, HM_OR2, ss(-0.2, 0.8, n.y) * 0.3);
            b = mix(b, HM_BACK, ss(0.2, -0.8, n.z) * ss(0.0, 0.8, n.y) * 0.6);
            return mix(b, HM_W, ss(0.15, 0.7, n.z) * ss(0.45, -0.1, n.y) + ss(-0.4, -0.85, n.y));
        };
        // patitas traseras (pies rosas con deditos)
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.3 + (sd > 0 ? 0 : Math.PI);
            V3 f = new V3(sd * 0.085, 0.02 + Math.max(0, Math.cos(p)) * 0.035 * w, 0.06 + Math.sin(p) * 0.04 * w);
            ell(c, f, new V3(0.042, 0.022, 0.06), 9, 5, (n, q) -> mix(HM_PINK, WHITE, ss(0, 1, n.y) * 0.2));
            for (int k = -1; k <= 1; k++) {
                ell(c, f.add(k * 0.015, 0.002, 0.052), new V3(0.009, 0.008, 0.01), 5, 3, flat(HM_PINK));
            }
            ell(c, new V3(sd * 0.09, 0.10 + bob, 0.0), new V3(0.06, 0.07, 0.075), 9, 6, fur);
        }
        // cuerpo de pera sentado
        V3 bc = new V3(0, 0.19 + bob + 0.006 * c.breath, -0.01), br = new V3(0.16, 0.165, 0.15);
        ell(c, bc, br, 16, 12, fur);
        // colita
        ell(c, bc.add(0, -0.07, -0.15), new V3(0.022, 0.018, 0.02), 6, 4, flat(HM_OR2));
        // cabeza (sin cuello, como los hámsteres)
        headLook(c, 0.1, 0.02 - 0.12 * nib);
        c.hpv = new V3(0, 0.3, 0.04);
        V3 hc = new V3(0, 0.36 + bob, 0.05), hr = new V3(0.13, 0.115, 0.12);
        ell(c, hc, hr, 16, 12, (n, p) -> {
            float[] b = mix(HM_OR, HM_OR2, ss(-0.2, 0.9, n.y) * 0.35);
            // franja blanca en la frente y hocico blanco
            double blaze = ss(0.035, 0.012, Math.abs(p.x)) * ss(0.2, 0.7, n.z) * ss(-0.1, 0.4, n.y);
            return mix(b, HM_W, Math.max(blaze, ss(0.3, 0.8, n.z) * ss(0.25, -0.3, n.y)));
        });
        // mofletes llenos (se inflan al masticar)
        for (int sd = -1; sd <= 1; sd += 2) {
            double pf = 1 + 0.07 * Math.abs(chew) + 0.03 * Math.sin(t * 0.13 + ph);
            V3 ck = new V3(sd * 0.095, 0.325 + bob, 0.085);
            V3 ckr = new V3(0.07 * pf, 0.064 * pf, 0.068 * pf);
            ell(c, ck, ckr, 12, 9, (n, p) -> mix(HM_W, HM_OR2, ss(0.2, 0.9, n.y) * ss(0.0, -0.6, n.z) * 0.8));
            Detail.soft(c, ck, ckr, new V3(sd * 0.5, 0.0, 0.85), 0.05, new float[]{1F, 0.64F, 0.62F}, 0.55);
        }
        // hocico, naricita que olfatea, dientecitos y bigotes
        V3 mz = new V3(0, 0.335 + bob, 0.155), mr = new V3(0.045, 0.04, 0.035);
        ell(c, mz, mr, 10, 7, flat(HM_W));
        V3 ns = new V3(0, 0.36 + bob + sniff, 0.188);
        ell(c, ns, new V3(0.016, 0.011, 0.01), 7, 4, flat(HM_NOSE));
        Detail.glint(c, ns, new V3(0.016, 0.011, 0.01), new V3(-0.3, 0.8, 0.5), 0.008, WHITE, 0.7);
        decal(c, mz, mr, new V3(0, -0.15, 1), 0.04, 0, T_CAT, new float[]{0.6F, 0.3F, 0.3F}, 0.9, 0.003);
        for (int sd = -1; sd <= 1; sd += 2) {
            Pets21.sbox(c, new V3(sd * 0.0055, 0.308 + bob - 0.004 * Math.abs(chew), 0.183), new V3(0.005, 0.009, 0.003), 0.4, 6, 4, flat(WHITE));
            for (int k = 0; k < 3; k++) {
                V3 a = new V3(sd * 0.03, 0.345 + bob + k * 0.008 + sniff, 0.18);
                Detail.whisker(c, a, a.add(sd * 0.085, -0.01 + k * 0.012, -0.02), 0.004, 0.0018, new float[]{0.98F, 0.96F, 0.9F});
            }
        }
        // ojitos de perla, cejas y orejitas rosadas
        eyes(c, hc, hr, 0.45, 0.22, 0.86, 0.075, T_BEAD, WHITE);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.42, 0.5, 0.76, 0.035, sd * 0.25, HM_BACK);
            double ef = Detail.flick(c, sd > 0 ? 131 : 132) + 0.3 * Math.abs(chew);
            V3 ec = new V3(sd * 0.085, 0.455 + bob, 0.0);
            ell(c, ec, new V3(0.038, 0.038, 0.016), 0, -0.2, sd * (0.25 + 0.2 * ef), 9, 6, flat(HM_OR));
            ell(c, ec.add(0, -0.002, 0.008), new V3(0.026, 0.026, 0.009), 0, -0.2, sd * (0.25 + 0.2 * ef), 7, 5, flat(HM_IN));
        }
        // copete
        for (int k = -1; k <= 1; k++) {
            V3 b0 = surf(hc, hr, new V3(k * 0.2, 1, 0.1));
            Pets25.lock(c, new V3[]{b0, b0.add(k * 0.008, 0.022, 0.008), b0.add(k * 0.016, 0.03, 0.03)}, 0.015, 0.006, flat(HM_OR2));
        }
        c.head = false;
        // manitas con la pipa de girasol (suben a la boca al mordisquear)
        double up = 0.03 * nib;
        V3 hold = new V3(0, 0.245 + bob + up, 0.145 + 0.01 * nib);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.1, 0.25 + bob, 0.07);
            V3 hand = hold.add(sd * 0.026, 0.0, -0.005);
            tube(c, new V3[]{sh, sh.lerp(hand, 0.5).add(sd * 0.01, -0.01, 0.01), hand}, new double[]{0.026, 0.02, 0.016}, 6, fur);
            ell(c, hand, new V3(0.017, 0.015, 0.016), 6, 4, flat(HM_PINK));
        }
        double rot = Math.sin(t * 0.4) * 0.25 * nib;
        ell(c, hold.add(0, 0.012, 0.012), new V3(0.016, 0.03, 0.01), 0, 0.3, rot, 8, 6, (n, p) -> {
            double st = Math.sin(p.x * 260);
            return st > 0.55 ? SEED_ST : SEED;
        });
    }

    // ------------------------------------------------------------------ RATÓN RELÁMPAGO

    private static final float[] SP_G = {0.78F, 0.81F, 0.92F}, SP_G2 = {0.90F, 0.93F, 1F}, SP_DK = {0.46F, 0.50F, 0.66F},
            SP_IN = {0.55F, 0.88F, 1F}, SP_PINK = {1F, 0.74F, 0.80F}, SP_COP = {0.86F, 0.52F, 0.28F}, SP_COP2 = {1F, 0.74F, 0.48F},
            SP_GLASS = {0.70F, 0.90F, 1F}, SP_BULB = {1F, 0.94F, 0.66F}, SP_SPARK = {0.62F, 0.92F, 1F}, SP_STRAP = {0.30F, 0.22F, 0.20F};

    /**
     * Ratón Relámpago: ratoncito de tormenta color plata lavanda con barriga blanca, orejas grandes y redondas con el interior
     * celeste que brilla, mechón de nube en la cabeza, gafas de cazador de tormentas de cobre en la frente, hocico puntiagudo
     * con naricita rosa y bigotes de luz, y una cola larguísima que termina en una bombilla que parpadea. Le saltan chispas
     * entre las orejas cuando está contento.
     */
    static void sparkmouse(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.2)) * 0.026 * w;
        c.pivot = new V3(0, 0.2, 0);
        Paint fur = (n, p) -> {
            float[] b = mix(SP_G, SP_G2, ss(-0.2, 0.9, n.y) * 0.4);
            b = mix(b, SP_DK, ss(0.6, 0.95, n.y) * ss(0.2, -0.6, n.z) * 0.5);
            return mix(b, WHITE, ss(0.2, 0.75, n.z) * ss(0.3, -0.5, n.y) * 0.9);
        };
        // pies y patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.2 + (sd > 0 ? 0 : Math.PI);
            V3 f = new V3(sd * 0.075, 0.022 + Math.max(0, Math.cos(p)) * 0.03 * w, 0.04 + Math.sin(p) * 0.035 * w);
            ell(c, f, new V3(0.034, 0.02, 0.058), 8, 5, flat(SP_PINK));
            for (int k = -1; k <= 1; k++) {
                ell(c, f.add(k * 0.012, 0.002, 0.05), new V3(0.008, 0.007, 0.009), 5, 3, flat(SP_PINK));
            }
            ell(c, new V3(sd * 0.08, 0.09 + bob, -0.01), new V3(0.05, 0.065, 0.065), 8, 6, fur);
        }
        V3 bc = new V3(0, 0.19 + bob + 0.005 * c.breath, -0.02), br = new V3(0.125, 0.135, 0.13);
        ell(c, bc, br, 14, 10, fur);
        // manitas juntas delante
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.09, 0.24 + bob, 0.06), b = new V3(sd * 0.035, 0.2 + bob + 0.01 * c.joy, 0.13);
            tube(c, new V3[]{a, a.lerp(b, 0.5).add(0, -0.01, 0), b}, new double[]{0.02, 0.017, 0.015}, 6, fur);
            ell(c, b, new V3(0.016, 0.014, 0.016), 6, 4, flat(SP_PINK));
        }
        // cola larguísima con bombilla al final
        V3[] tc = new V3[7];
        for (int i = 0; i < 7; i++) {
            double s = i / 6.0;
            double sw = Math.sin(t * 0.16 + ph - s * 2.2) * 0.06 * s;
            tc[i] = bc.add(sw, -0.05 + 0.34 * s * s + 0.06 * s, -0.12 - 0.13 * s + 0.08 * s * s * s);
        }
        curve(c, tc, new double[]{0.016, 0.013, 0.011, 0.01, 0.009, 0.0085, 0.008}, 3, 6, flat(SP_PINK));
        V3 tdir = tc[6].sub(tc[5]).norm();
        V3 sock = tc[6].add(tdir.mul(0.012));
        cone(c, tc[6], sock.add(tdir.mul(0.016)), 0.012, 0.016, 7, (n, p) -> mix(SP_COP, SP_COP2, 0.5 + 0.5 * Math.sin(p.y * 900)));
        V3 bulb = sock.add(tdir.mul(0.045));
        double flick = 0.75 + 0.25 * Math.sin(t * 0.9 + ph) * Math.sin(t * 2.3) + 0.3 * c.joy;
        c.blend = Sink.NORMAL;
        c.alpha = 0.55;
        ell(c, bulb, new V3(0.03, 0.03, 0.03), 10, 7, flat(SP_GLASS));
        c.alpha = 1;
        c.blend = Sink.SOLID;
        c.emissive = true;
        tube(c, new V3[]{sock.add(tdir.mul(0.016)).add(-0.007, 0, 0), bulb.add(-0.007, 0, 0), bulb.add(0.007, 0, 0), sock.add(tdir.mul(0.016)).add(0.007, 0, 0)},
                new double[]{0.002, 0.002, 0.002, 0.002}, 3, flat(SP_BULB));
        c.emissive = false;
        glow(c, bulb, 0.14 * flick, SP_BULB, 0.55 * flick);
        // cabeza
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.3, 0.05);
        V3 hc = new V3(0, 0.37 + bob, 0.05), hr = new V3(0.128, 0.11, 0.115);
        ell(c, hc, hr, 16, 11, fur);
        V3 mz = new V3(0, 0.34 + bob, 0.145), mr = new V3(0.055, 0.045, 0.06);
        ell(c, mz, mr, 11, 8, (n, p) -> mix(WHITE, SP_G2, ss(0.4, 1, n.y) * 0.4));
        V3 ns = surf(mz, mr, new V3(0, 0.2, 1));
        double sn = Math.sin(t * 1.0 + ph) * 0.003;
        ell(c, ns.add(0, sn, 0), new V3(0.016, 0.012, 0.012), 7, 4, flat(SP_PINK));
        Detail.glint(c, ns.add(0, sn, 0), new V3(0.016, 0.012, 0.012), new V3(-0.3, 0.8, 0.5), 0.009, WHITE, 0.8);
        decal(c, mz, mr, new V3(0, -0.4, 1), 0.04, 0, T_CAT, new float[]{0.5F, 0.3F, 0.4F}, 0.9, 0.003);
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 a = surf(mz, mr, new V3(sd * 0.7, 0.1 - 0.15 * k, 0.6));
                Detail.whisker(c, a, a.add(sd * 0.09, 0.012 - 0.012 * k, -0.02), 0.005, 0.0016, SP_SPARK);
            }
        }
        c.emissive = false;
        eyes(c, hc, hr, 0.44, 0.16, 0.86, 0.1, T_EYE, new float[]{0.42F, 0.78F, 1F});
        cheeks(c, hc, hr, 0.68, -0.2, 0.66, 0.065);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.4, 0.52, 0.74, 0.04, sd * 0.3, SP_DK);
        }
        // orejas grandes y redondas: dorso plata, dentro celeste que brilla un poco
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 311 : 312);
            V3 ec = new V3(sd * 0.115, 0.475 + bob, -0.005);
            double rl = sd * (0.42 + 0.22 * ef), pt = -0.15 + 0.1 * ef;
            ell(c, ec, new V3(0.08, 0.08, 0.02), sd * 0.25, pt, rl, 12, 7, fur);
            c.emissive = true;
            ell(c, ec.add(sd * 0.004, -0.003, 0.012), new V3(0.058, 0.058, 0.009), sd * 0.25, pt, rl, 10, 6,
                    (n, p) -> mix(SP_IN, WHITE, 0.15 + 0.15 * Math.sin(t * 0.3 + ph)));
            c.emissive = false;
        }
        // mechón de nube
        V3 cl = surf(hc, hr, new V3(0, 1, 0.2));
        ell(c, cl.add(0, 0.012, 0), new V3(0.03, 0.024, 0.028), 8, 6, flat(SP_G2));
        ell(c, cl.add(0.024, 0.006, -0.004), new V3(0.022, 0.018, 0.022), 7, 5, flat(SP_G2));
        ell(c, cl.add(-0.022, 0.004, 0.006), new V3(0.02, 0.017, 0.02), 7, 5, flat(SP_G2));
        // gafas de cobre en la frente con su correa
        V3[] strap = new V3[17];
        for (int i = 0; i <= 16; i++) {
            double a = Math.PI * 2 * i / 16.0;
            strap[i] = hc.add(Math.sin(a) * hr.x * 1.03, 0.055 + 0.02 * Math.cos(a), Math.cos(a) * hr.z * 1.03);
        }
        tube(c, strap, Pets23.radii(17, 0.008), 5, flat(SP_STRAP));
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 g = surf(hc, hr.add(0.012, 0.012, 0.012), new V3(sd * 0.38, 0.62, 0.7));
            V3 gn = new V3(sd * 0.38 / hr.x, 0.62 / hr.y, 0.7 / hr.z).norm();
            V3 gu = gn.anyPerp(), gv = gn.cross(gu).norm();
            V3[] rim = new V3[11];
            for (int i = 0; i <= 10; i++) {
                double a = Math.PI * 2 * i / 10.0;
                rim[i] = g.add(gu.mul(Math.cos(a) * 0.027)).add(gv.mul(Math.sin(a) * 0.027));
            }
            tube(c, rim, Pets23.radii(11, 0.008), 5, (n, p) -> mix(SP_COP, SP_COP2, ss(-0.3, 0.9, n.y) * 0.7));
            c.blend = Sink.NORMAL;
            c.alpha = 0.7;
            ell(c, g.add(gn.mul(0.002)), new V3(0.024, 0.024, 0.024).mul(1), 8, 5, (n, p) -> mix(SP_GLASS, WHITE, ss(0.3, 1, n.y) * 0.6));
            c.alpha = 1;
            c.blend = Sink.SOLID;
        }
        c.head = false;
        // chispas entre las orejas al alegrarse y chispitas alrededor
        if (c.joy > 0.25 || Detail.flick(c, 313) > 0.5) {
            c.emissive = true;
            V3 b = new V3(-0.11, 0.53 + bob, 0);
            V3 m1 = new V3(0.04, 0.58 + bob, 0.01), m2 = new V3(-0.03, 0.55 + bob, -0.01);
            tube(c, new V3[]{new V3(0.11, 0.53 + bob, 0), m1, m2, b}, new double[]{0.003, 0.004, 0.004, 0.003}, 4, flat(WHITE));
            glow(c, m1.lerp(m2, 0.5), 0.12, SP_SPARK, 0.6);
            c.emissive = false;
        }
        for (int k = 0; k < 3; k++) {
            double a = t * 0.12 + k * 2.1;
            double fl = Math.max(0, Math.sin(t * 0.37 + k * 2.3 + ph * 3));
            glow(c, new V3(Math.cos(a) * 0.28, 0.3 + 0.1 * Math.sin(a * 2), Math.sin(a) * 0.28), 0.08 + 0.08 * fl, SP_SPARK, 0.5 * fl + 0.1);
        }
    }

    // ------------------------------------------------------------------ DROIDE MINERO

    private static final float[] DM_OR = {1F, 0.64F, 0.16F}, DM_OR2 = {1F, 0.80F, 0.42F}, DM_DK = {0.24F, 0.26F, 0.31F},
            DM_MT = {0.62F, 0.65F, 0.71F}, DM_MT2 = {0.82F, 0.85F, 0.90F}, DM_SCR = {0.07F, 0.09F, 0.13F}, DM_LED = {0.35F, 1F, 0.82F},
            DM_LAMP = {1F, 0.96F, 0.74F}, DM_RED = {1F, 0.25F, 0.22F}, DM_Y = {1F, 0.84F, 0.18F}, DM_K = {0.14F, 0.14F, 0.16F};

    /**
     * Droide Minero: robotito minero naranja con orugas, cuerpo de caja redondeada con franja de peligro, remaches y líneas de
     * panel, cabeza con visor de pantalla y ojos de luz que parpadean (se ponen felices ^^), casco con lámpara, antena con
     * lucecita roja, un brazo con taladro que gira y otro con pinza, y una batería en la espalda con tres luces de carga.
     */
    static void droid(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 2)) * 0.006 * w + 0.003 * c.breath;
        c.pivot = new V3(0, 0.22, 0);
        // orugas: módulos oscuros con tacos que corren al andar y ruedas a los lados
        double roll = c.gait * 0.04;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 tc = new V3(sd * 0.105, 0.055, 0.0);
            Pets21.sbox(c, tc, new V3(0.042, 0.052, 0.115), 0.5, 10, 8, (n, p) -> mix(DM_DK, DM_MT, ss(0.6, 1, n.y) * 0.25));
            for (int k = 0; k < 10; k++) {
                double u = ((k / 10.0 + roll) % 1 + 1) % 1;
                // camino del taco alrededor del óvalo de la oruga
                double a = u * Math.PI * 2;
                V3 pt = tc.add(0, Math.sin(a) * 0.054, Math.cos(a) * 0.118);
                ell(c, pt, new V3(0.044, 0.008, 0.01), 0, -a + Math.PI / 2, 0, 5, 3, flat(DM_K));
            }
            for (int k = -1; k <= 1; k++) {
                V3 wc = tc.add(sd * 0.043, 0, k * 0.07);
                ell(c, wc, new V3(0.004, 0.03, 0.03), 0, 0, 0, 8, 5, (n, p) -> mix(DM_MT, DM_MT2, ss(0.3, 1, n.y) * 0.5));
                ell(c, wc.add(sd * 0.003, 0, 0), new V3(0.003, 0.012, 0.012), 6, 4, flat(DM_OR));
            }
        }
        // cuerpo de caja redondeada con franja de peligro abajo
        V3 bc = new V3(0, 0.205 + bob, 0), br = new V3(0.12, 0.1, 0.1);
        Pets21.sbox(c, bc, br, 0.5, 14, 10, (n, p) -> {
            if (p.y < -0.05 && Math.abs(n.y) < 0.6) {
                return Math.sin((p.x + p.z * 0.6 + p.y) * 90) > 0 ? DM_Y : DM_K;
            }
            return mix(DM_OR, DM_OR2, ss(-0.2, 0.9, n.y) * 0.5);
        });
        // líneas de panel y remaches
        for (int sd = -1; sd <= 1; sd += 2) {
            tube(c, new V3[]{bc.add(sd * 0.06, 0.07, 0.101), bc.add(sd * 0.06, -0.035, 0.101)}, new double[]{0.0025, 0.0025}, 4, flat(DM_DK));
            for (int k = 0; k < 2; k++) {
                ell(c, bc.add(sd * 0.095, 0.07 - 0.1 * k, 0.098), new V3(0.007, 0.007, 0.004), 5, 3, flat(DM_MT2));
            }
        }
        // panel de delante con indicador
        Pets21.sbox(c, bc.add(0, 0.02, 0.098), new V3(0.04, 0.035, 0.008), 0.4, 8, 6, flat(DM_DK));
        c.emissive = true;
        for (int k = 0; k < 3; k++) {
            boolean on = ((int) (t / 6 + k)) % 3 != 0;
            ell(c, bc.add(-0.022 + 0.022 * k, 0.03, 0.107), new V3(0.007, 0.007, 0.003), 5, 3, flat(on ? (k == 2 ? DM_RED : DM_LED) : DM_DK));
        }
        c.emissive = false;
        // batería en la espalda con tres luces de carga
        V3 bat = bc.add(0, 0.02, -0.115);
        Pets21.sbox(c, bat, new V3(0.07, 0.07, 0.03), 0.5, 10, 8, (n, p) -> mix(DM_MT, DM_MT2, ss(-0.2, 0.9, n.y) * 0.5));
        c.emissive = true;
        int lv = (int) ((t * 0.08 + ph) % 4);
        for (int k = 0; k < 3; k++) {
            ell(c, bat.add(0, -0.035 + 0.035 * k, -0.03), new V3(0.03, 0.009, 0.004), 6, 3, flat(k < lv ? DM_LED : DM_DK));
        }
        c.emissive = false;
        // brazos: taladro que gira y pinza
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = bc.add(sd * 0.13, 0.04, 0.01);
            ell(c, sh, new V3(0.03, 0.03, 0.03), 8, 6, (n, p) -> mix(DM_MT, DM_MT2, ss(0, 1, n.y) * 0.5));
            double sw = Math.sin(c.gait + (sd > 0 ? 0 : Math.PI)) * 0.03 * w + (sd > 0 ? 0.02 * c.joy : 0);
            V3 el = sh.add(sd * 0.03, -0.06, 0.03 + sw), hd = el.add(sd * 0.0, -0.005, 0.07 + sw);
            tube(c, new V3[]{sh, el}, new double[]{0.014, 0.013}, 6, flat(DM_DK));
            ell(c, el, new V3(0.016, 0.016, 0.016), 6, 4, flat(DM_MT));
            tube(c, new V3[]{el, hd}, new double[]{0.013, 0.012}, 6, flat(DM_DK));
            if (sd > 0) {
                // taladro: cono con espiral que gira
                double spin = t * (0.6 + 2.0 * c.joy);
                V3 tip = hd.add(0, 0, 0.07);
                cone(c, hd, tip, 0.024, 0.003, 8, (n, p) -> {
                    double a = Math.atan2(p.y - hd.y, p.x - hd.x) + (p.z - hd.z) * 120 - spin;
                    return Math.sin(a * 2) > 0 ? DM_MT2 : DM_MT;
                });
                ell(c, hd, new V3(0.024, 0.024, 0.01), 8, 4, flat(DM_OR));
            } else {
                // pinza de tres dedos
                ell(c, hd, new V3(0.018, 0.018, 0.014), 7, 5, flat(DM_MT));
                double op = 0.3 + 0.2 * Math.sin(t * 0.2 + ph);
                for (int k = 0; k < 3; k++) {
                    double a = Math.PI * 2 * k / 3 + 0.5;
                    V3 d = new V3(Math.cos(a), Math.sin(a), 0);
                    V3 f0 = hd.add(d.mul(0.012)).add(0, 0, 0.01);
                    curve(c, new V3[]{f0, f0.add(d.mul(0.012 * op)).add(0, 0, 0.015), f0.add(d.mul(0.004)).add(0, 0, 0.028)}, new double[]{0.005, 0.004, 0.003}, 2, 4, flat(DM_DK));
                }
            }
        }
        // cabeza con visor
        headLook(c, 0.12, 0.02);
        c.hpv = new V3(0, 0.31, 0.0);
        tube(c, new V3[]{bc.add(0, 0.09, 0), bc.add(0, 0.13, 0)}, new double[]{0.03, 0.03}, 8, flat(DM_MT));
        V3 hc = new V3(0, 0.39 + bob, 0.01), hr = new V3(0.115, 0.08, 0.095);
        Pets21.sbox(c, hc, hr, 0.55, 14, 10, (n, p) -> mix(DM_OR, DM_OR2, ss(-0.2, 0.9, n.y) * 0.5));
        V3 vc = hc.add(0, -0.005, hr.z * 0.95);
        Pets21.sbox(c, vc, new V3(0.088, 0.052, 0.012), 0.45, 10, 6, (n, p) -> mix(DM_SCR, new float[]{0.18F, 0.22F, 0.3F}, ss(0.4, 1, n.y) * 0.6));
        c.emissive = true;
        boolean blink = ((t + ph * 9) % 70) < 4 || c.shut;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 e = vc.add(sd * 0.036, 0.004, 0.013);
            if (blink) {
                Pets21.sbox(c, e, new V3(0.016, 0.003, 0.002), 0.5, 6, 4, flat(DM_LED));
            } else if (c.joy > 0.2) {
                // ojos felices ^^
                tube(c, new V3[]{e.add(-0.016, -0.006, 0), e.add(0, 0.01, 0), e.add(0.016, -0.006, 0)}, new double[]{0.004, 0.004, 0.004}, 4, flat(DM_LED));
            } else {
                Pets21.sbox(c, e, new V3(0.014, 0.019, 0.002), 0.5, 6, 6, flat(DM_LED));
                Pets21.sbox(c, e.add(-0.004, 0.007, 0.001), new V3(0.004, 0.005, 0.002), 0.5, 4, 4, flat(WHITE));
            }
        }
        glow(c, vc.add(0, 0, 0.02), 0.16, DM_LED, 0.18);
        c.emissive = false;
        // casco con lámpara
        Detail.cap(c, hc.add(0, 0.045, 0), new V3(0.122, 0.07, 0.103), 0, 0, 0, 1.3, 0, 14, 5, (n, p) -> mix(DM_Y, WHITE, ss(0.5, 1, n.y) * 0.3));
        tube(c, Pets23.ring(hc.add(0, 0.03, 0), 0.124, 0.105, 16), Pets23.radii(17, 0.007), 4, flat(DM_Y));
        V3 lamp = hc.add(0, 0.1, 0.075);
        tube(c, new V3[]{lamp.add(0, 0, -0.025), lamp}, new double[]{0.02, 0.02}, 8, flat(DM_MT));
        c.emissive = true;
        double lf = 0.85 + 0.15 * Math.sin(t * 1.7 + ph) * Math.sin(t * 0.37);
        ell(c, lamp.add(0, 0, 0.002), new V3(0.017, 0.017, 0.006), 8, 4, flat(DM_LAMP));
        c.emissive = false;
        glow(c, lamp.add(0, 0, 0.02), 0.22 * lf, DM_LAMP, 0.45 * lf);
        // antena con lucecita roja
        V3 an = hc.add(-0.06, 0.07, -0.03);
        double aw = Math.sin(t * 0.3 + ph) * 0.01 + c.roll * 0.05;
        curve(c, new V3[]{an, an.add(aw * 0.5, 0.04, 0), an.add(aw, 0.075, -0.005)}, new double[]{0.004, 0.003, 0.0025}, 2, 4, flat(DM_DK));
        boolean on = ((t + ph * 5) % 30) < 15;
        c.emissive = true;
        ell(c, an.add(aw, 0.08, -0.005), new V3(0.01, 0.01, 0.01), 6, 4, flat(on ? DM_RED : new float[]{0.5F, 0.15F, 0.15F}));
        c.emissive = false;
        if (on) {
            glow(c, an.add(aw, 0.08, -0.005), 0.06, DM_RED, 0.5);
        }
        // remaches de la cabeza
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, Pets24.boxSurf(hc, hr, 0.55, new V3(sd, 0, 0.2)), new V3(0.01, 0.01, 0.01), 6, 4, flat(DM_MT2));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ ESPÍRITU DEL BOSQUE

    private static final float[] FS_M = {0.40F, 0.64F, 0.34F}, FS_M2 = {0.56F, 0.78F, 0.44F}, FS_BELLY = {0.86F, 0.92F, 0.72F},
            FS_WOOD = {0.46F, 0.32F, 0.22F}, FS_WOOD2 = {0.62F, 0.46F, 0.32F}, FS_LEAF = {0.36F, 0.74F, 0.30F}, FS_LEAF2 = {0.62F, 0.88F, 0.40F},
            FS_MUSH = {0.92F, 0.30F, 0.30F}, FS_GLOW = {0.80F, 1F, 0.55F}, FS_PINK = {1F, 0.72F, 0.84F}, FS_FL_C = {1F, 0.86F, 0.30F},
            FS_NOSE = {0.26F, 0.20F, 0.18F};

    /**
     * Espíritu del Bosque: cervatillo de musgo verde con la barriga clara, cuernitos de rama con hojas y una setita que brilla,
     * orejas de hoja que se sacuden, flores blancas y rosas en el lomo en vez de manchas, pezuñas de madera, una cola de hoja
     * grande y lucecitas de luciérnaga que le dan vueltas.
     */
    static void forestSpirit(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.03 * w;
        c.pivot = new V3(0, 0.26, 0);
        Paint moss = (n, p) -> {
            double mot = 0.5 + 0.5 * Math.sin(p.x * 70 + Math.sin(p.z * 60) * 2) * Math.sin(p.z * 55 + p.y * 40);
            float[] b = mix(FS_M, FS_M2, ss(-0.2, 0.9, n.y) * 0.45 + mot * 0.18);
            return mix(b, FS_BELLY, ss(-0.25, -0.75, n.y));
        };
        // patas finas de cervatillo con pezuña de madera
        double xs = 0.06, zf = 0.08, zb = -0.1, hipY = 0.2 + bob;
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? zf : zb;
                double p = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(p) * 0.06 * c.walk, lift = Math.max(0, Math.cos(p)) * 0.05 * c.walk;
                V3 hip = new V3(sd * xs, hipY, z0), foot = new V3(sd * xs, 0.03 + lift, z0 + sw);
                V3 knee = hip.lerp(foot, 0.5).add(0, 0, (fb == 0 ? 0.01 : -0.015) + sw * 0.2);
                if (fb == 1) {
                    ell(c, hip.add(-sd * 0.006, 0.025, 0), new V3(0.04, 0.055, 0.05), 0, -0.2, 0, 8, 6, moss);
                }
                tube(c, new V3[]{hip, knee, foot.add(0, 0.015, 0)}, new double[]{0.028, 0.02, 0.017}, 7, moss);
                ell(c, foot.add(0, -0.004, 0.006), new V3(0.019, 0.022, 0.022), 6, 4, (n, q) -> mix(FS_WOOD, FS_WOOD2, ss(0, 1, n.y) * 0.5));
            }
        }
        V3 bc = new V3(0, 0.27 + bob + 0.005 * c.breath, -0.01), br = new V3(0.105, 0.095, 0.16);
        ell(c, bc, br, 16, 11, moss);
        // flores en el lomo (en vez de manchas)
        double[][] fl = {{0.45, 0.85, 0.35}, {-0.5, 0.8, 0.0}, {0.55, 0.75, -0.4}, {-0.35, 0.9, -0.55}, {0.15, 0.97, 0.05}, {-0.6, 0.6, 0.45}};
        for (int i = 0; i < fl.length; i++) {
            V3 d = new V3(fl[i][0], fl[i][1], fl[i][2]);
            V3 at = surf(bc, br, d);
            V3 n = new V3(d.x / br.x, d.y / br.y, d.z / br.z).norm();
            flower(c, at.add(n.mul(0.004)), n, 0.016, i % 2 == 0 ? WHITE : FS_PINK, FS_FL_C, t * 0.0 + i);
        }
        // cola de hoja grande que se agita
        double wag = Math.sin(t * 0.5 + ph) * (0.3 + 0.4 * c.joy);
        V3 tb = bc.add(0, 0.05, -0.15);
        V3 ttip = tb.add(Math.sin(wag) * 0.05, 0.08, -0.06);
        V3 side = new V3(Math.cos(wag), 0, Math.sin(wag) * 0.3).norm();
        V3 tn = ttip.sub(tb).cross(side).norm();
        Detail.feather(c, tb, ttip, side, 0.04, tn, FS_LEAF, FS_LEAF2);
        tube(c, new V3[]{tb, tb.lerp(ttip, 0.5), ttip}, new double[]{0.004, 0.003, 0.0015}, 3, flat(new float[]{0.3F, 0.55F, 0.22F}));
        // cuello
        V3 n0 = bc.add(0, 0.05, 0.11), n1 = new V3(0, 0.41 + bob, 0.13);
        tube(c, new V3[]{n0, n0.lerp(n1, 0.5), n1}, new double[]{0.055, 0.048, 0.045}, 9, moss);
        // cabeza
        headLook(c, 0.13, 0.03);
        c.hpv = new V3(0, 0.4, 0.12);
        V3 hc = new V3(0, 0.47 + bob, 0.15), hr = new V3(0.1, 0.095, 0.1);
        ell(c, hc, hr, 16, 12, moss);
        V3 sn = hc.add(0, -0.035, 0.08), sr = new V3(0.05, 0.045, 0.055);
        ell(c, sn, sr, 11, 8, (n, p) -> mix(FS_BELLY, WHITE, ss(0, 1, n.y) * 0.3));
        V3 ns = surf(sn, sr, new V3(0, 0.3, 1));
        ell(c, ns, new V3(0.016, 0.011, 0.01), 7, 4, flat(FS_NOSE));
        Detail.glint(c, ns, new V3(0.016, 0.011, 0.01), new V3(-0.3, 0.8, 0.5), 0.008, WHITE, 0.6);
        decal(c, sn, sr, new V3(0, -0.4, 1), 0.04, 0, T_CAT, new float[]{0.3F, 0.35F, 0.25F}, 0.9, 0.003);
        eyes(c, hc, hr, 0.5, 0.15, 0.82, 0.105, T_EYE, new float[]{0.62F, 0.86F, 0.38F});
        cheeks(c, hc, hr, 0.7, -0.2, 0.64, 0.06);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.soft(c, hc, hr, new V3(sd * 0.45, 0.45, 0.75), 0.05, FS_BELLY, 0.6);
        }
        // orejas de hoja
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 331 : 332);
            V3 e0 = hc.add(sd * 0.075, 0.06, -0.02);
            V3 et = e0.add(sd * (0.09 - 0.02 * ef), 0.03 + 0.04 * ef, -0.02);
            V3 es = new V3(0, 1, 0.3).norm();
            V3 en = et.sub(e0).cross(es).norm();
            Detail.feather(c, e0, et, es, 0.026, en, FS_M, FS_LEAF2);
            tube(c, new V3[]{e0, et}, new double[]{0.002, 0.001}, 3, flat(new float[]{0.3F, 0.5F, 0.22F}));
        }
        // cuernitos de rama con hojas y una setita que brilla
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a0 = hc.add(sd * 0.04, 0.085, 0.0);
            V3 a1 = a0.add(sd * 0.02, 0.05, -0.01), a2 = a1.add(sd * 0.03, 0.045, -0.02);
            curve(c, new V3[]{a0, a1, a2}, new double[]{0.011, 0.008, 0.005}, 3, 5, (n, p) -> mix(FS_WOOD, FS_WOOD2, ss(0, 1, n.y) * 0.5));
            V3 b1 = a1.add(sd * 0.035, 0.02, 0.01);
            curve(c, new V3[]{a1, a1.lerp(b1, 0.5).add(0, 0.006, 0), b1}, new double[]{0.006, 0.005, 0.003}, 2, 4, flat(FS_WOOD2));
            double lw = Math.sin(t * 0.3 + ph + sd) * 0.15;
            for (V3 tip : new V3[]{a2, b1}) {
                V3 lt = tip.add(sd * 0.02, 0.016, 0.006);
                Detail.feather(c, tip, lt, new V3(0, Math.cos(lw), Math.sin(lw)).cross(lt.sub(tip)).norm(), 0.011, new V3(0, 0, 1), FS_LEAF, FS_LEAF2);
            }
            if (sd > 0) {
                V3 m = a1.add(0.004, 0.012, 0.012);
                tube(c, new V3[]{m.add(0, -0.01, 0), m}, new double[]{0.004, 0.0035}, 4, flat(WHITE));
                c.emissive = true;
                Detail.cap(c, m.add(0, 0.002, 0), new V3(0.016, 0.012, 0.016), 0, 0, 0, 1.6, 0, 8, 3, flat(FS_MUSH));
                c.emissive = false;
                glow(c, m.add(0, 0.008, 0), 0.07, new float[]{1F, 0.5F, 0.45F}, 0.35 + 0.15 * Math.sin(t * 0.2 + ph));
            } else {
                flower(c, a1.add(0, 0.005, 0.01), new V3(0, 0.3, 1).norm(), 0.013, FS_PINK, FS_FL_C, 0);
            }
        }
        c.head = false;
        // luciérnagas que le dan vueltas
        for (int k = 0; k < 4; k++) {
            double a = t * 0.05 * (k % 2 == 0 ? 1 : -1.3) + k * 1.6 + ph;
            double fl2 = 0.5 + 0.5 * Math.sin(t * 0.25 + k * 2.1);
            V3 wp = new V3(Math.cos(a) * (0.26 + 0.04 * k), 0.3 + 0.12 * Math.sin(a * 1.7 + k), Math.sin(a) * (0.26 + 0.04 * k));
            boolean em = c.emissive;
            c.emissive = true;
            glow(c, wp, 0.07 + 0.06 * fl2, FS_GLOW, 0.3 + 0.4 * fl2);
            glow(c, wp, 0.025, WHITE, 0.6 * fl2);
            c.emissive = em;
        }
    }

    /** Florecita de cinco pétalos mirando hacia {@code n}. */
    static void flower(Ctx c, V3 at, V3 n, double r, float[] petal, float[] center, double rot) {
        V3 u = n.anyPerp(), v = n.cross(u).norm();
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5.0 + rot;
            V3 d = u.mul(Math.cos(a)).add(v.mul(Math.sin(a)));
            V3 pc = at.add(d.mul(r * 0.75));
            ell(c, pc, new V3(r * 0.55, r * 0.55, r * 0.55), 5, 3, flat(petal));
        }
        ell(c, at.add(n.mul(r * 0.25)), new V3(r * 0.4, r * 0.4, r * 0.4), 5, 3, flat(center));
    }

    /** Collar de cuero con púas y placa: rodea un cuello de eje {@code axis} y radio {@code r}. */
    static void spikedCollar(Ctx c, V3 ce, V3 axis, double r, float[] band, float[] spike, float[] tag) {
        V3 a = axis.norm(), u = a.anyPerp(), v = a.cross(u).norm();
        int n = 14;
        V3[] ring = new V3[n + 1];
        for (int i = 0; i <= n; i++) {
            double ang = Math.PI * 2 * i / n;
            ring[i] = ce.add(u.mul(Math.cos(ang) * r)).add(v.mul(Math.sin(ang) * r));
        }
        tube(c, ring, Pets23.radii(n + 1, r * 0.16), 5, (nn, p) -> mix(band, WHITE, ss(0.4, 1, nn.y) * 0.15));
        for (int i = 0; i < n; i += 2) {
            V3 d = ring[i].sub(ce).norm();
            cone(c, ring[i].add(d.mul(r * 0.1)), ring[i].add(d.mul(r * 0.42)), r * 0.11, r * 0.01, 4, flat(spike));
        }
        // placa colgando abajo
        V3 down = v.mul(-1);
        double best = -2;
        for (V3 cand : new V3[]{u, u.mul(-1), v, v.mul(-1)}) {
            if (-cand.y > best) {
                best = -cand.y;
                down = cand;
            }
        }
        V3 tg = ce.add(down.mul(r * 1.2)).add(0, -r * 0.25, 0);
        ell(c, tg, new V3(r * 0.28, r * 0.28, r * 0.08), 8, 5, flat(tag));
        Detail.glint(c, tg, new V3(r * 0.28, r * 0.28, r * 0.08), new V3(-0.3, 0.5, 0.8), r * 0.3, WHITE, 0.8);
    }
}
