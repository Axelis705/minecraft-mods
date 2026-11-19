package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mascotas nuevas (etapa 252), originales: Tiburoncito de Tierra, Nubecita, Robot Tostadora y Murcielaguito Vampiro.
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0, altura total ≈ 1.
 */
final class Pets21 {
    private Pets21() {
    }

    /** Gira todo lo que se dibuje después un poco más (cabeceo) alrededor del pivote; devuelve la matriz para restaurarla. */
    static double[] tiltAll(Ctx c, double pitch, double roll) {
        double[] save = c.m.clone();
        System.arraycopy(mm(save, rot(0, pitch, roll)), 0, c.m, 0, 9);
        return save;
    }

    static void restore(Ctx c, double[] save) {
        System.arraycopy(save, 0, c.m, 0, 9);
    }

    // ------------------------------------------------------------------ TIBURONCITO DE TIERRA

    private static final float[] SH_B = {0.42F, 0.62F, 0.86F}, SH_B2 = {0.58F, 0.78F, 0.98F}, SH_W = {0.97F, 0.98F, 1.0F},
            SH_DK = {0.24F, 0.36F, 0.58F}, SAND = {0.86F, 0.74F, 0.52F}, SAND2 = {0.74F, 0.60F, 0.40F};

    /**
     * Tiburoncito de tierra: al moverse "nada" bajo el suelo y solo asoma la aleta con un montoncito de tierra; quieto, sale y
     * de vez en cuando da un salto de delfín. Azul con barriga blanca, ojos enormes y sonrisa con dos dientitos.
     */
    static void landShark(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        // salto: cada ~7 s, solo quieto
        double ju = ((t + ph * 13) % 150) / 26.0;
        double jump = (ju < 1 && w < 0.2) ? Math.sin(ju * Math.PI) : 0;
        double dive = ss(0.12, 0.45, w) * (1 - jump);
        double sink = -0.30 * dive + 0.30 * jump;
        double flip = (ju < 1 && w < 0.2) ? Math.cos(ju * Math.PI) * 0.55 : 0;
        c.pivot = new V3(0, 0.22, 0);
        double[] sv = tiltAll(c, -flip + dive * 0.10, 0);
        double y0 = 0.24 + sink + 0.01 * c.breath;
        // cola: aleta que se menea (más rápido al nadar)
        double wag = Math.sin(t * (0.22 + 0.25 * w) + ph) * (0.10 + 0.12 * w);
        V3 t0 = new V3(0, y0, -0.20), t1 = new V3(wag * 0.4, y0 + 0.01, -0.36), t2 = new V3(wag, y0 + 0.03, -0.48);
        Paint skin = (n, p) -> mix(SH_W, mix(SH_B, SH_B2, ss(0.2, 0.9, n.y) * 0.4), ss(-0.35, 0.05, n.y));
        curve(c, new V3[]{t0, t1, t2}, new double[]{0.15, 0.10, 0.05}, 4, 10, skin);
        // aleta de la cola: lóbulo de arriba grande y de abajo pequeño
        V3 tb = t2.add(wag * 0.2, 0, -0.03);
        ell(c, tb.add(wag * 0.15, 0.09, -0.06), new V3(0.028, 0.11, 0.06), wag * 0.6, -0.55, 0, 8, 6, flat(SH_B));
        ell(c, tb.add(wag * 0.12, -0.05, -0.04), new V3(0.026, 0.07, 0.045), wag * 0.6, 0.6, 0, 8, 6, flat(SH_B));
        // cuerpo: redondo y gordito, la cabeza es la parte de delante
        V3 bc = new V3(0, y0, 0.0), br = new V3(0.20, 0.19, 0.27);
        ell(c, bc, br, 16, 11, skin);
        V3 hc = new V3(0, y0 + 0.035, 0.11), hr = new V3(0.215, 0.205, 0.215);
        ell(c, hc, hr, 16, 11, skin);
        // aleta dorsal (la que asoma al nadar)
        Paint finP = (n, p) -> mix(SH_B, SH_DK, ss(-0.02, 0.10, p.y) * 0.35);
        ell(c, new V3(0, y0 + 0.20, -0.02), new V3(0.032, 0.09, 0.10), 0, 0.25, 0, 10, 7, finP);
        ell(c, new V3(0, y0 + 0.29, -0.08), new V3(0.022, 0.07, 0.045), 0, 0.85, 0, 8, 6, finP);
        // puntas blancas de las aletas y una muesca de "aventurero" en la dorsal
        ell(c, new V3(0, y0 + 0.335, -0.115), new V3(0.016, 0.026, 0.02), 0, 0.85, 0, 6, 4, flat(SH_W));
        ell(c, new V3(0.02, y0 + 0.24, -0.05), new V3(0.006, 0.014, 0.012), 0, 0.5, 0, 5, 3, flat(SH_DK));
        ell(c, tb.add(wag * 0.2, 0.18, -0.1), new V3(0.02, 0.035, 0.03), wag * 0.6, -0.55, 0, 6, 4, flat(SH_W));
        // rayitas del lomo (como un tiburón tigre bebé)
        for (int k = 0; k < 4; k++) {
            for (int sd = -1; sd <= 1; sd += 2) {
                Detail.leaf(c, bc, br, new V3(sd * 0.55, 0.75, 0.15 - k * 0.22), 0.07, sd * 0.9, SH_DK);
            }
        }
        // aletas pectorales: bracitos que reman
        double pad = Math.sin(t * 0.30 + ph) * (0.25 + 0.35 * jump);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.20, y0 - 0.07, 0.04), new V3(0.09, 0.025, 0.06), 0, 0.2, sd * (0.5 + pad), 8, 5, flat(SH_B));
        }
        // agallas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                decal(c, bc, br, new V3(sd * 0.95, 0.05, 0.10 - k * 0.12), 0.06, Math.PI / 2, T_MOUTH, SH_DK, 0.55, 0.003);
            }
        }
        // manchitas en el lomo
        for (int k = 0; k < 4; k++) {
            Detail.soft(c, bc, br, new V3((k % 2 == 0 ? 0.35 : -0.3), 0.85, -0.5 + k * 0.25), 0.05, SH_B2, 0.6);
        }
        // cara
        headLook(c, 0.10, 0.02);
        c.hpv = new V3(0, y0, 0.1);
        eyes(c, hc, hr, 0.50, 0.22, 0.80, 0.14, T_EYE, new float[]{0.55F, 0.80F, 1F});
        cheeks(c, hc, hr, 0.70, -0.12, 0.66, 0.08);
        decal(c, hc, hr, new V3(0, -0.22, 1), 0.12, 0, T_MOUTH, new float[]{0.30F, 0.16F, 0.20F}, 0.95, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 tooth = surf(hc, hr, new V3(sd * 0.13, -0.30, 1)).add(0, -0.008, 0.004);
            cone(c, tooth.add(0, 0.006, 0), tooth.add(0, -0.022, 0.004), 0.009, 0.002, 5, flat(SH_W));
            Detail.brow(c, hc, hr, sd, 0.48, 0.55, 0.68, 0.07, sd * 0.3, SH_DK);
            ell(c, surf(hc, hr, new V3(sd * 0.12, 0.05, 1)), new V3(0.007, 0.005, 0.004), 4, 3, flat(SH_DK));
        }
        c.head = false;
        restore(c, sv);
        // montoncito de tierra que empuja al nadar bajo el suelo (gira con el cuerpo, delante de la aleta)
        // quieto: el hoyito de tierra del que salió, con terrones, piedritas y matitas de pasto
        double hole = (1 - dive) * (1 - jump);
        if (hole > 0.05) {
            for (int k = 0; k < 16; k++) {
                double a = k / 16.0 * Math.PI * 2 + 0.3;
                double r0 = 0.2 + 0.025 * Math.sin(k * 2.7);
                V3 at = new V3(Math.cos(a) * r0, 0.012, 0.02 + Math.sin(a) * r0 * 1.2);
                double rs = 1 + 0.25 * Math.sin(k * 1.9);
                float[] dc = k % 2 == 0 ? SAND2 : mix(SAND, SAND2, 0.5);
                ell(c, at, new V3(0.06 * hole * rs, 0.034 * hole * rs, 0.05 * hole * rs), 8, 5, (n, p) -> mix(dc, SAND, ss(-0.2, 1, n.y) * 0.6));
                if (k % 3 == 0) {
                    ell(c, at.add(0.02, 0.02 * hole, 0.01), new V3(0.012, 0.009, 0.011).mul(hole), 5, 3, flat(new float[]{0.62F, 0.62F, 0.66F}));
                }
                if (k % 3 == 1) {
                    for (int g = -1; g <= 1; g++) {
                        V3 gb = at.add(g * 0.012, 0.02 * hole, 0);
                        cone(c, gb, gb.add(g * 0.012, 0.05 * hole, 0.005), 0.005, 0.001, 3, flat(new float[]{0.42F, 0.72F, 0.32F}));
                    }
                }
            }
        }
        if (dive > 0.05) {
            for (int k = 0; k < 7; k++) {
                double a = k / 7.0 * Math.PI * 2 + t * 0.05;
                double r0 = 0.12 + 0.05 * Math.sin(t * 0.6 + k * 1.7);
                ell(c, new V3(Math.cos(a) * r0, 0.012, 0.06 + Math.sin(a) * r0 * 1.3), new V3(0.06 * dive, 0.035 * dive, 0.06 * dive),
                        8, 5, flat(k % 2 == 0 ? SAND : SAND2));
            }
            // terrones que saltan
            for (int k = 0; k < 3; k++) {
                double u = ((t * 0.09 + k * 0.33) % 1.0);
                V3 at = new V3((k - 1) * 0.08, 0.02 + Math.sin(u * Math.PI) * 0.16, 0.20 - u * 0.25);
                ell(c, at, new V3(0.022 * dive, 0.02 * dive, 0.022 * dive), 6, 4, flat(SAND2));
            }
        }
    }

    // ------------------------------------------------------------------ NUBECITA

    private static final float[] CL_W = {0.97F, 0.98F, 1.0F}, CL_SH = {0.74F, 0.82F, 0.97F}, CL_RAIN = {0.55F, 0.78F, 1F},
            BOLT = {1F, 0.94F, 0.45F};

    /**
     * Nubecita: una nube esponjosa con carita que flota, gotea lluvia y, cuando está contenta, suelta un rayito y le sale un
     * arcoíris pequeño por detrás.
     */
    static void cloud(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.13 + ph) * 0.035;
        double sq = 1 + 0.04 * Math.sin(t * 0.26 + ph);
        c.pivot = new V3(0, 0.45, 0);
        double y = 0.45 + bob;
        Paint fluff = (n, p) -> mix(CL_SH, CL_W, ss(-0.7, 0.4, n.y));
        // bolas de la nube (la del centro es la cara)
        double[][] puffs = {{0, 0, 0, 0.26, 0.22, 0.22}, {-0.23, -0.04, -0.02, 0.17, 0.15, 0.16}, {0.23, -0.04, -0.02, 0.17, 0.15, 0.16},
                {-0.11, 0.15, -0.04, 0.16, 0.15, 0.15}, {0.12, 0.14, -0.06, 0.15, 0.14, 0.14}, {0, 0.06, -0.15, 0.2, 0.17, 0.15},
                {-0.36, -0.08, -0.06, 0.1, 0.09, 0.1}, {0.36, -0.08, -0.06, 0.1, 0.09, 0.1}};
        for (int i = 0; i < puffs.length; i++) {
            double[] pf = puffs[i];
            double wob = 1 + 0.035 * Math.sin(t * 0.2 + i * 1.3 + ph);
            ell(c, new V3(pf[0] * sq, y + pf[1], pf[2]), new V3(pf[3] * wob, pf[4] * wob / sq, pf[5] * wob), 14, 9, fluff);
        }
        V3 hc = new V3(0, y, 0.0), hr = new V3(0.26, 0.22, 0.22);
        c.look = 0;
        headLook(c, 0.08, 0.0);
        c.hpv = new V3(0, y, 0);
        eyes(c, hc, hr, 0.36, 0.10, 0.92, 0.12, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.62, -0.14, 0.78, 0.10);
        decal(c, hc, hr, new V3(0, -0.22, 1), 0.09, 0, c.joy > 0.3 ? T_CAT : T_MOUTH, new float[]{0.35F, 0.25F, 0.40F}, 0.95, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.34, 0.34, 0.88, 0.05, sd * 0.2, CL_SH);
        }
        c.head = false;
        // un solecito asoma por detrás (con rayos que giran) y dos nubecitas que le dan vueltas
        {
            V3 sun = new V3(0.2, y + 0.17, -0.2);
            c.emissive = true;
            ell(c, sun, new V3(0.08, 0.08, 0.05), 12, 8, (n, p) -> mix(new float[]{1F, 0.78F, 0.25F}, new float[]{1F, 0.95F, 0.6F}, ss(-0.2, 1, n.y) * 0.6));
            for (int k = 0; k < 8; k++) {
                double a = Math.PI * 2 * k / 8 + t * 0.02;
                V3 d = new V3(Math.cos(a), Math.sin(a), 0);
                cone(c, sun.add(d.mul(0.085)), sun.add(d.mul(0.125 + 0.01 * Math.sin(t * 0.2 + k))), 0.016, 0.002, 4, flat(new float[]{1F, 0.85F, 0.35F}));
            }
            c.emissive = false;
            glow(c, sun, 0.3, new float[]{1F, 0.85F, 0.4F}, 0.3);
            for (int k = 0; k < 2; k++) {
                double a = t * 0.03 + k * Math.PI + ph;
                V3 mc = new V3(Math.cos(a) * 0.42, y + 0.05 + 0.06 * Math.sin(a * 2), Math.sin(a) * 0.32);
                ell(c, mc, new V3(0.05, 0.035, 0.04), 8, 5, fluff);
                ell(c, mc.add(0.035, -0.008, 0), new V3(0.035, 0.026, 0.03), 7, 5, fluff);
                ell(c, mc.add(-0.034, -0.01, 0), new V3(0.03, 0.022, 0.028), 7, 5, fluff);
            }
        }
        // lluvia: gotitas que caen y se reinician
        c.blend = Sink.NORMAL;
        for (int k = 0; k < 6; k++) {
            double u = ((t * 0.045 + k * 0.17 + ph) % 1.0);
            double x = -0.22 + k * 0.088;
            double gy = y - 0.18 - u * 0.42;
            c.alpha = 0.85 * (1 - u * u);
            ell(c, new V3(x, gy, -0.02 + (k % 2) * 0.05), new V3(0.012, 0.026, 0.012), 6, 4, flat(CL_RAIN));
        }
        c.alpha = 1;
        c.blend = Sink.SOLID;
        // rayito y arcoíris cuando está contenta
        double happy = Math.max(c.joy, Detail.flick(c, 31));
        if (happy > 0.2) {
            c.emissive = true;
            V3 a = new V3(0.10, y - 0.20, 0.04), b = a.add(-0.06, -0.10, 0), d = b.add(0.07, -0.02, 0), e = d.add(-0.05, -0.12, 0);
            tube(c, new V3[]{a, b, d, e}, new double[]{0.014 * happy, 0.016 * happy, 0.014 * happy, 0.004}, 5, flat(BOLT));
            glow(c, d, 0.25 * happy, BOLT, 0.6);
            c.emissive = false;
        }
        float[][] rb = {{1F, 0.45F, 0.5F}, {1F, 0.8F, 0.35F}, {0.5F, 0.9F, 0.5F}, {0.45F, 0.7F, 1F}};
        double arc = 0.55 + 0.45 * happy;
        for (int k = 0; k < 4; k++) {
            V3[] pts = new V3[9];
            double rr = (0.30 - k * 0.045) * arc;
            for (int i = 0; i < 9; i++) {
                double a = Math.PI * i / 8.0;
                pts[i] = new V3(Math.cos(a) * rr, y + 0.02 + Math.sin(a) * rr * 0.9, -0.22 - 0.03 * k);
            }
            double[] rad = new double[9];
            java.util.Arrays.fill(rad, 0.022);
            final float[] col = rb[k];
            c.emissive = true;
            c.blend = Sink.NORMAL;
            c.alpha = 0.55 + 0.35 * happy;
            tube(c, pts, rad, 5, flat(col));
            c.alpha = 1;
            c.blend = Sink.SOLID;
            c.emissive = false;
        }
    }

    // ------------------------------------------------------------------ ROBOT TOSTADORA

    private static final float[] TO_MINT = {0.45F, 0.85F, 0.78F}, TO_MINT2 = {0.62F, 0.95F, 0.88F}, TO_CHROME = {0.86F, 0.88F, 0.92F},
            TO_DK = {0.16F, 0.18F, 0.22F}, TOAST = {0.86F, 0.62F, 0.34F}, TOAST2 = {0.98F, 0.86F, 0.58F}, TO_SCREEN = {0.10F, 0.14F, 0.18F},
            TO_GLOW = {0.45F, 1F, 0.95F}, TO_RED = {1F, 0.38F, 0.36F};

    private static double spow(double v, double e) {
        return Math.copySign(Math.pow(Math.abs(v), e), v);
    }

    /** Caja de esquinas suaves (superelipsoide): e = 1 es una esfera, e ≈ 0.25 casi una caja. */
    static void sbox(Ctx c, V3 ce, V3 r, double e, int nu, int nv, Paint paint) {
        nu = Math.max(8, (int) Math.round(nu * c.lod));
        nv = Math.max(6, (int) Math.round(nv * c.lod));
        V3[][] P = new V3[nv + 1][nu + 1], N = new V3[nv + 1][nu + 1];
        for (int i = 0; i <= nv; i++) {
            double th = Math.PI * i / nv, ct = Math.cos(th), st = Math.sin(th);
            for (int j = 0; j <= nu; j++) {
                double fi = 2 * Math.PI * j / nu, cp = Math.cos(fi), sp = Math.sin(fi);
                V3 p = new V3(spow(st, e) * spow(cp, e) * r.x, spow(ct, e) * r.y, spow(st, e) * spow(sp, e) * r.z);
                V3 n = new V3(spow(st, 2 - e) * spow(cp, 2 - e) / r.x, spow(ct, 2 - e) / r.y, spow(st, 2 - e) * spow(sp, 2 - e) / r.z);
                P[i][j] = ce.add(p);
                N[i][j] = n.len() < 1.0E-9 ? new V3(0, ct >= 0 ? 1 : -1, 0) : n.norm();
            }
        }
        for (int i = 0; i < nv; i++) {
            for (int j = 0; j < nu; j++) {
                V3[] p = {P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j]};
                V3[] n = {N[i][j], N[i][j + 1], N[i + 1][j + 1], N[i + 1][j]};
                float[][] b = new float[4][];
                for (int k = 0; k < 4; k++) {
                    b[k] = paint.at(n[k], p[k].sub(ce));
                }
                c.quad(c.blend, c.alpha, T_WHITE, p, n, b, null);
            }
        }
    }

    /**
     * Robot Tostadora: tostadora retro color menta con patitas, antena con lucecita y una pantalla con ojos de luz. Cuando
     * está contento le saltan las dos tostadas y vuelven a bajar.
     */
    static void toaster(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait * 1.3)) * 0.03 * w;
        c.pivot = new V3(0, 0.3, 0);
        double y = 0.36 + bob + 0.006 * c.breath;
        // patitas con ruedita
        for (int sd = -1; sd <= 1; sd += 2) {
            double p = c.gait * 1.3 + (sd > 0 ? 0 : Math.PI);
            double lift = Math.max(0, Math.cos(p)) * 0.04 * w;
            V3 hip = new V3(sd * 0.11, y - 0.14, 0), foot = new V3(sd * 0.11, 0.05 + lift, Math.sin(p) * 0.04 * w);
            tube(c, new V3[]{hip, hip.lerp(foot, 0.5), foot}, new double[]{0.028, 0.024, 0.024}, 6, flat(TO_CHROME));
            ell(c, foot.add(0, -0.01, 0.02), new V3(0.05, 0.04, 0.07), 10, 6, flat(TO_DK));
        }
        // cuerpo
        V3 bc = new V3(0, y, 0), br = new V3(0.22, 0.17, 0.15);
        sbox(c, bc, br, 0.32, 24, 16, (n, p) -> mix(TO_MINT, TO_MINT2, ss(-0.2, 0.9, n.y) * 0.5));
        // franja cromada abajo y palanca al lado
        ell(c, bc.add(0, -0.125, 0), new V3(0.215, 0.03, 0.145), 16, 5, flat(TO_CHROME));
        double lever = 0.5 + 0.5 * Math.sin(t * 0.05 + ph);
        tube(c, new V3[]{bc.add(0.2, 0.09, 0), bc.add(0.222, 0.09, 0), bc.add(0.222, -0.08, 0)}, new double[]{0.006, 0.006, 0.006}, 4, flat(TO_DK));
        ell(c, bc.add(0.225, 0.02 - 0.05 * lever, 0), new V3(0.03, 0.02, 0.04), 8, 5, flat(TO_RED));
        // borde cromado arriba, perillas de tostado y cable con enchufe que cuelga detrás
        tube(c, Pets23.ring(bc.add(0, 0.155, 0), 0.205, 0.135, 20), Pets23.radii(21, 0.01), 5, (n, p) -> mix(TO_CHROME, WHITE, ss(0.3, 1, n.y) * 0.5));
        for (int k = 0; k < 2; k++) {
            V3 kn = bc.add(-0.226, 0.04 - 0.07 * k, 0.03);
            ell(c, kn, new V3(0.012, 0.025, 0.025), 8, 5, flat(TO_CHROME));
            double ang = t * 0.01 * (k + 1) + ph;
            V3 mk = kn.add(-0.012, Math.cos(ang) * 0.016, Math.sin(ang) * 0.016);
            ell(c, mk, new V3(0.004, 0.005, 0.005), 4, 3, flat(TO_RED));
        }
        {
            double sw = Math.sin(t * 0.17 + ph) * 0.03 + Math.sin(c.gait * 1.3) * 0.03 * w;
            V3 c0 = bc.add(0, -0.08, -0.15), c1 = c0.add(sw * 0.5, -0.08, -0.08), c2 = new V3(sw, 0.02, c0.z - 0.2);
            curve(c, new V3[]{c0, c1, c2}, new double[]{0.008, 0.008, 0.008}, 4, 4, flat(TO_DK));
            Pets21.sbox(c, c2.add(0, 0.012, -0.02), new V3(0.022, 0.016, 0.024), 0.5, 6, 5, flat(new float[]{0.92F, 0.92F, 0.9F}));
            for (int sd = -1; sd <= 1; sd += 2) {
                tube(c, new V3[]{c2.add(sd * 0.008, 0.012, -0.044), c2.add(sd * 0.008, 0.012, -0.064)}, new double[]{0.003, 0.003}, 3, flat(TO_CHROME));
            }
        }
        // tostadas que saltan
        double pop = Math.max(c.joy, Detail.flick(c, 41));
        for (int k = -1; k <= 1; k += 2) {
            double hy = y + 0.15 + 0.20 * pop + 0.012 * Math.sin(t * 0.3 + k * 1.7);
            V3 tc = new V3(k * 0.085, hy, -0.02);
            sbox(c, tc, new V3(0.07, 0.07, 0.022), 0.45, 12, 8, (n, p) -> mix(TOAST2, TOAST, ss(0.6, 0.95, Math.abs(n.x) + Math.max(0, n.y))));
            ell(c, tc.add(0, 0.07, 0), new V3(0.075, 0.035, 0.024), 12, 6, flat(TOAST));
            if (k > 0) {
                // cuadrito de mantequilla que se derrite
                Pets21.sbox(c, tc.add(0.0, 0.03, 0.024), new V3(0.022, 0.016, 0.006), 0.5, 6, 5, flat(new float[]{1F, 0.94F, 0.55F}));
            } else {
                // carita de mermelada
                for (int sd = -1; sd <= 1; sd += 2) {
                    ell(c, tc.add(sd * 0.022, 0.03, 0.023), new V3(0.009, 0.009, 0.003), 5, 3, flat(new float[]{0.75F, 0.12F, 0.25F}));
                }
                tube(c, new V3[]{tc.add(-0.022, 0.0, 0.023), tc.add(0, -0.012, 0.024), tc.add(0.022, 0.0, 0.023)}, new double[]{0.004, 0.004, 0.004}, 3,
                        flat(new float[]{0.75F, 0.12F, 0.25F}));
            }
        }
        // ranuras (encima, tapan la parte baja de las tostadas) con el calorcito naranja dentro y vapor
        for (int k = -1; k <= 1; k += 2) {
            ell(c, bc.add(k * 0.085, 0.168, -0.02), new V3(0.082, 0.008, 0.03), 10, 4, flat(TO_DK));
            c.emissive = true;
            ell(c, bc.add(k * 0.085, 0.17, -0.02), new V3(0.06, 0.004, 0.012), 8, 3, flat(new float[]{1F, 0.5F, 0.2F}));
            c.emissive = false;
            for (int j = 0; j < 2; j++) {
                double u = ((t * 0.012 + j * 0.5 + k * 0.25 + ph) % 1 + 1) % 1;
                c.blend = Sink.NORMAL;
                c.alpha = 0.35 * Math.sin(u * Math.PI);
                ell(c, bc.add(k * 0.085 + Math.sin(u * 6 + j) * 0.02, 0.19 + u * 0.22 + 0.2 * pop, -0.02), new V3(0.02 + 0.02 * u, 0.02 + 0.02 * u, 0.02 + 0.02 * u), 6, 4, flat(WHITE));
                c.alpha = 1;
                c.blend = Sink.SOLID;
            }
        }
        // antena con lucecita
        V3 a0 = bc.add(0.12, 0.17, 0.05), a1 = a0.add(0.02 + Math.sin(t * 0.2 + ph) * 0.015, 0.12, 0);
        tube(c, new V3[]{a0, a0.lerp(a1, 0.5), a1}, new double[]{0.008, 0.007, 0.006}, 5, flat(TO_CHROME));
        c.emissive = true;
        double blink = 0.5 + 0.5 * Math.sin(t * 0.35 + ph);
        ell(c, a1, new V3(0.024, 0.024, 0.024), 8, 6, flat(mix(TO_RED, WHITE, 0.3 * blink)));
        glow(c, a1, 0.12 + 0.06 * blink, TO_RED, 0.5 * blink + 0.2);
        c.emissive = false;
        // pantalla con cara de luz (la pantalla va pegada a la caja: no gira la "cabeza" para mirarte)
        c.look = 0;
        headLook(c, 0.06, 0.0);
        c.hpv = bc;
        V3 sc = bc.add(0, 0.015, 0.145);
        ell(c, sc, new V3(0.15, 0.10, 0.012), 14, 6, flat(TO_DK));
        ell(c, sc.add(0, 0, 0.004), new V3(0.135, 0.085, 0.01), 14, 6, flat(TO_SCREEN));
        c.emissive = true;
        boolean shut = c.shut;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 e = sc.add(sd * 0.055, 0.02, 0.014);
            if (shut) {
                ell(c, e, new V3(0.03, 0.006, 0.004), 8, 4, flat(TO_GLOW));
            } else if (pop > 0.3) {
                // ojos felices ^ ^
                tube(c, new V3[]{e.add(-0.03, -0.01, 0), e.add(0, 0.02, 0), e.add(0.03, -0.01, 0)}, new double[]{0.007, 0.007, 0.007}, 5, flat(TO_GLOW));
            } else {
                ell(c, e, new V3(0.022, 0.03, 0.004), 8, 6, flat(TO_GLOW));
            }
            glow(c, e, 0.09, TO_GLOW, 0.35);
        }
        tube(c, new V3[]{sc.add(-0.03, -0.045, 0.014), sc.add(0, -0.058, 0.014), sc.add(0.03, -0.045, 0.014)}, new double[]{0.006, 0.006, 0.006}, 5, flat(TO_GLOW));
        c.emissive = false;
        c.head = false;
        // bracitos con pinzas redondas
        double wave = Detail.flick(c, 42);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = bc.add(sd * 0.22, -0.02, 0.02);
            double up = sd > 0 ? wave : 0;
            V3 hand = sh.add(sd * 0.07, -0.08 + 0.18 * up, 0.04 + Math.sin(c.gait * 1.3 + sd) * 0.03 * w);
            tube(c, new V3[]{sh, sh.lerp(hand, 0.5).add(sd * 0.02, 0, 0), hand}, new double[]{0.018, 0.016, 0.015}, 6, flat(TO_CHROME));
            ell(c, hand, new V3(0.032, 0.032, 0.032), 8, 6, flat(TO_DK));
        }
    }

    // ------------------------------------------------------------------ MURCIELAGUITO VAMPIRO

    private static final float[] BT_F = {0.36F, 0.26F, 0.48F}, BT_F2 = {0.50F, 0.38F, 0.66F}, BT_MEM = {0.30F, 0.20F, 0.40F},
            BT_IN = {0.95F, 0.62F, 0.72F}, CAPE_R = {0.82F, 0.12F, 0.20F}, CAPE_K = {0.12F, 0.10F, 0.16F};

    /**
     * Murcielaguito vampiro: una bolita peluda morada con orejas grandes, colmillitos, cuello alto de capa (negra por fuera,
     * roja por dentro) y alas que aletean. De vez en cuando da una voltereta en el aire.
     */
    static void vampireBat(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.18 + ph) * 0.04;
        double fl = Math.sin(t * 0.55 + ph);
        c.pivot = new V3(0, 0.45, 0);
        double flipU = ((t + ph * 11) % 260) / 24.0;
        double flip = flipU < 1 ? Anim(flipU) : 0;
        double[] sv = tiltAll(c, flip * Math.PI * 2, 0);
        double y = 0.45 + bob;
        Paint fur = (n, p) -> mix(BT_F, BT_F2, ss(-0.5, 0.8, n.y) * 0.6);
        // alas: hueso y membrana festoneada
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 root = new V3(sd * 0.14, y + 0.02, -0.02);
            double up = 0.3 + fl * 0.7;
            V3 elbow = root.add(sd * 0.17, 0.06 + up * 0.14, -0.04);
            V3 hand = elbow.add(sd * 0.20, 0.05 + up * 0.12, -0.06);
            V3[] tips = new V3[4];
            for (int k = 0; k < 4; k++) {
                double f = k / 3.0;
                tips[k] = hand.add(sd * (0.06 - f * 0.20), (0.02 - f * 0.28) * (0.5 + up * 0.6) - f * 0.04, -0.03 - f * 0.12);
            }
            tube(c, new V3[]{root, elbow, hand}, new double[]{0.018, 0.014, 0.010}, 6, flat(BT_F));
            c.blend = Sink.NORMAL;
            c.alpha = 0.96;
            V3[] fan = new V3[tips.length + 1];
            fan[0] = elbow;
            System.arraycopy(tips, 0, fan, 1, tips.length);
            Creatures4.membrane(c, root.add(0, -0.06, -0.02), fan, (n, p) -> BT_MEM);
            c.alpha = 1;
            c.blend = Sink.SOLID;
            for (V3 tp : tips) {
                tube(c, new V3[]{hand, hand.lerp(tp, 0.5), tp}, new double[]{0.007, 0.006, 0.004}, 4, flat(BT_F));
            }
            // garrita del pulgar en la "mano" del ala
            cone(c, hand, hand.add(sd * 0.01, 0.035, 0.015), 0.008, 0.001, 4, flat(new float[]{0.92F, 0.88F, 0.9F}));
        }
        // patitas colgando
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 a = new V3(sd * 0.06, y - 0.17, -0.02), b = a.add(sd * 0.01, -0.06, -0.02 + Math.sin(t * 0.2 + sd) * 0.01);
            tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{0.016, 0.013, 0.011}, 5, flat(BT_F));
        }
        // cuerpo-cabeza: una bolita peluda
        V3 hc = new V3(0, y, 0.0), hr = new V3(0.19, 0.18, 0.17);
        ell(c, hc, hr, 16, 11, fur);
        Detail.fur(c, hc, hr, 160, 521, 0.035, 0.016, new V3(0, -0.6, -0.5), 0.7, Detail.ALL, fur, BT_F2);
        Detail.soft(c, hc, hr, new V3(0, -0.4, 1), 0.2, new float[]{0.62F, 0.50F, 0.78F}, 0.8);
        // cuello alto de capa: dos solapas detrás de la cabeza (rojo por dentro, negro por fuera)
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 2; k++) {
                V3 base = new V3(sd * (0.03 + k * 0.07), y - 0.14, -0.05 - k * 0.03);
                V3 tip = new V3(sd * (0.17 + k * 0.06), y + 0.08 - k * 0.07, -0.10 - k * 0.02);
                V3 side = tip.sub(base).cross(new V3(0, 0, 1)).norm();
                Detail.kite(c, base, tip, side, 0.075, new V3(sd * 0.4, 0.1, 1).norm(), CAPE_R, mix(CAPE_R, WHITE, 0.15));
                Detail.kite(c, base.add(0, 0, -0.01), tip.add(0, 0, -0.01), side, 0.078, new V3(sd * 0.4, 0.1, -1).norm(), CAPE_K, CAPE_K);
            }
        }
        ell(c, new V3(0, y - 0.13, 0.11), new V3(0.03, 0.025, 0.012), 8, 5, flat(CAPE_R));
        // medallón dorado con rubí en el cuello de la capa
        ell(c, new V3(0, y - 0.13, 0.122), new V3(0.026, 0.026, 0.008), 9, 5, flat(new float[]{1F, 0.8F, 0.3F}));
        c.emissive = true;
        ell(c, new V3(0, y - 0.13, 0.13), new V3(0.012, 0.012, 0.006), 6, 4, flat(new float[]{0.95F, 0.15F, 0.3F}));
        c.emissive = false;
        Detail.glint(c, new V3(0, y - 0.13, 0.13), new V3(0.012, 0.012, 0.006), new V3(-0.3, 0.5, 0.8), 0.012, WHITE, 0.8);
        // orejas grandes
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 51 : 52);
            V3 b0 = new V3(sd * 0.09, y + 0.14, 0.0), tip = new V3(sd * (0.19 + 0.03 * ef), y + 0.34 - 0.05 * ef, -0.03);
            tube(c, new V3[]{b0, b0.lerp(tip, 0.5).add(sd * 0.02, 0, 0), tip}, new double[]{0.07, 0.05, 0.004}, 6, fur);
            V3 b2 = b0.add(0, 0.01, 0.03), t2 = tip.add(-sd * 0.012, -0.05, 0.03);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.045, 0.032, 0.004}, 5, flat(BT_IN));
        }
        // cara
        headLook(c, 0.10, 0.0);
        c.hpv = hc;
        eyes(c, hc, hr, 0.40, 0.14, 0.88, 0.13, T_EYE, new float[]{1F, 0.55F, 0.60F});
        cheeks(c, hc, hr, 0.62, -0.14, 0.74, 0.09);
        ell(c, surf(hc, hr, new V3(0, -0.05, 1)).add(0, 0, 0.004), new V3(0.018, 0.012, 0.01), 6, 4, flat(BT_IN));
        decal(c, hc, hr, new V3(0, -0.28, 1), 0.08, 0, T_CAT, new float[]{0.30F, 0.12F, 0.20F}, 0.95, 0.004);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 fang = surf(hc, hr, new V3(sd * 0.09, -0.36, 1)).add(0, -0.005, 0.004);
            cone(c, fang, fang.add(0, -0.03, 0.004), 0.009, 0.0015, 5, flat(WHITE));
        }
        c.head = false;
        restore(c, sv);
    }

    private static double Anim(double u) {
        return u * u * (3 - 2 * u);
    }
}
