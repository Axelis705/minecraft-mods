package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Tercera vuelta (etapa 253) de las dos mascotas que Axelis volvió a marcar: Cachorro Rey y Cabrita Montañera. Diseños nuevos
 * de verdad (no retoques), con mucho más detalle de forma: hocico esculpido, orejas con dos colores, patas con dedos y
 * almohadillas, manchas suaves, ropa de tela que ondea y animaciones propias (rugido, rumiar, topetazo).
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0.
 */
final class Pets25 {
    private Pets25() {
    }

    // ------------------------------------------------------------------ utilidades

    /** Superficie de tela (capas, mantas): rejilla de puntos; la pintura recibe (u, v) en p.x, p.y (0..1). */
    static void sheet(Ctx c, V3[][] P, Paint paint) {
        int nv = P.length - 1, nu = P[0].length - 1;
        V3[][] N = new V3[nv + 1][nu + 1];
        for (int i = 0; i <= nv; i++) {
            for (int j = 0; j <= nu; j++) {
                V3 du = P[i][Math.min(j + 1, nu)].sub(P[i][Math.max(j - 1, 0)]);
                V3 dv = P[Math.min(i + 1, nv)][j].sub(P[Math.max(i - 1, 0)][j]);
                V3 n = du.cross(dv);
                N[i][j] = n.len() < 1.0E-9 ? V3.UP : n.norm();
            }
        }
        for (int i = 0; i < nv; i++) {
            for (int j = 0; j < nu; j++) {
                V3[] p = {P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j]};
                V3[] n = {N[i][j], N[i][j + 1], N[i + 1][j + 1], N[i + 1][j]};
                int[][] ij = {{i, j}, {i, j + 1}, {i + 1, j + 1}, {i + 1, j}};
                float[][] b = new float[4][];
                for (int k = 0; k < 4; k++) {
                    b[k] = paint.at(n[k], new V3(ij[k][1] / (double) nu, ij[k][0] / (double) nv, 0));
                }
                c.quad(c.blend, c.alpha, T_WHITE, p, n, b, null);
            }
        }
    }

    /** Mechón suave: curva que se afina y termina en una bolita (silueta redonda, sin púas). */
    static void lock(Ctx c, V3[] cp, double r0, double r1, Paint paint) {
        lock(c, cp, r0, r1, 3, 7, paint);
    }

    static void lock(Ctx c, V3[] cp, double r0, double r1, int sub, int sides, Paint paint) {
        double[] rad = new double[cp.length];
        for (int i = 0; i < cp.length; i++) {
            rad[i] = r0 + (r1 - r0) * i / (cp.length - 1.0);
        }
        curve(c, cp, rad, sub, sides, paint);
        V3 tip = cp[cp.length - 1];
        ell(c, tip, new V3(r1 * 1.05, r1 * 1.05, r1 * 1.05), 6, 4, (n, p) -> paint.at(n, p.add(tip)));
    }

    /** Pata de gato grande: pata, mano redonda con tres deditos delante y almohadillas rosas debajo. */
    static void catLeg(Ctx c, V3 hip, double r, double sw, double lift, Paint leg, float[] paw, float[] bean, float[] toeLine) {
        V3 foot = new V3(hip.x, r * 0.75 + lift, hip.z + sw);
        V3 knee = hip.lerp(foot, 0.55).add(0, 0, sw * 0.25 + 0.006);
        tube(c, new V3[]{hip, knee, foot}, new double[]{r * 1.12, r * 0.96, r * 0.9}, 8, leg);
        V3 pc = foot.add(0, -r * 0.2, r * 0.32);
        V3 pr = new V3(r * 1.15, r * 0.72, r * 1.3);
        ell(c, pc, pr, 10, 6, (n, p) -> mix(paw, WHITE, ss(-0.2, 1, n.y) * 0.15));
        for (int k = -1; k <= 1; k++) {
            V3 tc = surf(pc, pr, new V3(k * 0.42, 0.15, 1)).add(0, -0.002, -r * 0.18);
            ell(c, tc, new V3(r * 0.36, r * 0.34, r * 0.34), 6, 4, flat(paw));
        }
        // rayitas entre los dedos
        for (int k = -1; k <= 1; k += 2) {
            V3 a = surf(pc, pr, new V3(k * 0.22, 0.35, 1)), b = surf(pc, pr, new V3(k * 0.2, -0.2, 1));
            tube(c, new V3[]{a.add(0, 0, 0.002), b.add(0, 0, 0.002)}, new double[]{r * 0.05, r * 0.04}, 4, flat(toeLine));
        }
        if (lift > 0.004 || c.lod > 1.2) {
            ell(c, pc.add(0, -pr.y * 0.92, -r * 0.05), new V3(r * 0.5, r * 0.08, r * 0.45), 7, 3, flat(bean));
        }
    }

    // ------------------------------------------------------------------ CACHORRO REY

    private static final float[] LC_GOLD = {0.98F, 0.75F, 0.40F}, LC_GOLD2 = {1F, 0.87F, 0.58F}, LC_BACK = {0.86F, 0.58F, 0.28F},
            LC_CREAM = {1F, 0.95F, 0.84F}, LC_SPOT = {0.80F, 0.52F, 0.26F}, LC_MANE = {0.78F, 0.38F, 0.14F}, LC_MANE2 = {0.93F, 0.55F, 0.20F},
            LC_DARK = {0.36F, 0.20F, 0.13F}, LC_NOSE = {0.60F, 0.30F, 0.30F}, LC_BEAN = {1F, 0.66F, 0.70F}, LC_LINE = {0.80F, 0.60F, 0.44F},
            CAPE = {0.74F, 0.10F, 0.20F}, CAPE2 = {0.93F, 0.25F, 0.32F}, CAPE_IN = {0.98F, 0.80F, 0.32F}, ERM = {0.99F, 0.99F, 0.97F},
            ERM_DOT = {0.13F, 0.11F, 0.13F}, GOLD = {1F, 0.80F, 0.24F}, GOLD2 = {1F, 0.95F, 0.62F}, RUBY = {0.95F, 0.16F, 0.30F},
            SAPH = {0.24F, 0.52F, 1F};

    /**
     * Cachorro Rey: leoncito joven, regordete y de patas cortas con manos grandes (deditos y almohadillas rosas), pelaje dorado
     * con lomo más oscuro, barriga y hocico de crema y manchitas suaves de bebé. Melena esponjosa de tres capas de mechones
     * redondeados (naranja claro delante, rojizo detrás) que se mecen al andar; orejas redondas con el dorso oscuro, ojazos
     * color miel, naricita con brillo, puntitos y bigotes. Lleva un manto real de terciopelo rojo con ribete de armiño que
     * ondea detrás, cordón dorado con broche de rubí y una corona chiquita ladeada. Cola con borla oscura que se mece con
     * retraso y, de vez en cuando, un mini rugido.
     */
    static void lionCub(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk, idle = 1 - w;
        double bob = Math.abs(Math.sin(c.gait)) * 0.024 * w;
        double ru = ((t + ph * 29) % 560) / 34.0;
        double roar = ru < 1 ? Math.sin(ru * Math.PI) * idle * (1 - c.sleep) : 0;
        roar = roar * roar * (3 - 2 * roar);
        c.pivot = new V3(0, 0.24, 0);
        Paint fur = (n, p) -> {
            float[] side = mix(LC_GOLD, LC_GOLD2, ss(-0.2, 0.6, n.y) * 0.35);
            float[] top = mix(side, LC_BACK, ss(0.62, 0.95, n.y) * 0.7);
            return mix(top, LC_CREAM, ss(-0.15, -0.65, n.y));
        };
        Paint legFur = (n, p) -> mix(LC_GOLD, LC_GOLD2, ss(-0.4, 0.8, n.y) * 0.4);
        // patas cortas y gorditas con manos grandes
        double xs = 0.078, zf = 0.072, zb = -0.10, hipY = 0.14 + bob;
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? zf : zb;
                double p = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(p) * 0.05 * c.walk;
                double lift = Math.max(0, Math.cos(p)) * 0.045 * c.walk;
                catLeg(c, new V3(sd * xs, hipY, z0), 0.047, sw, lift, legFur, LC_CREAM, LC_BEAN, LC_LINE);
            }
        }
        // cuerpo de alubia con ancas redondas, barriga de crema y manchitas de bebé
        V3 bc = new V3(0, 0.215 + bob + 0.005 * c.breath, -0.02), br = new V3(0.13, 0.11, 0.155);
        ell(c, bc, br, 16, 11, fur);
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 hh = bc.add(sd * 0.068, -0.012, -0.088);
            ell(c, hh, new V3(0.062, 0.078, 0.075), 10, 7, (n, q) -> fur.at(n, q));
            Detail.soft(c, hh, new V3(0.062, 0.078, 0.075), new V3(sd, 0.35, -0.25), 0.034, LC_SPOT, 0.42);
        }
        double[][] spots = {{0.95, 0.15, 0.25, 0.038}, {0.9, 0.38, -0.05, 0.032}, {0.88, -0.05, 0.55, 0.028}, {0.7, 0.62, 0.35, 0.026}};
        for (int sd = -1; sd <= 1; sd += 2) {
            for (double[] s : spots) {
                Detail.soft(c, bc, br, new V3(sd * s[0], s[1], s[2]), s[3], LC_SPOT, 0.42);
            }
        }
        // cola larga que se mece con retraso, con borla oscura
        V3 t0 = bc.add(0, 0.03, -0.15);
        V3[] tc = new V3[6];
        for (int i = 0; i < 6; i++) {
            double s = i / 5.0;
            double sw = Math.sin(t * (0.17 + 0.25 * c.joy) + ph - s * 1.9) * (0.035 + 0.075 * s) * (1 + 0.8 * c.joy) + c.roll * 0.15 * s;
            tc[i] = t0.add(sw, 0.02 + 0.13 * s - 0.05 * s * s * w, -0.07 - 0.11 * s + 0.04 * s * s);
        }
        curve(c, tc, new double[]{0.023, 0.02, 0.018, 0.016, 0.015, 0.014}, 3, 7, legFur);
        V3 tdir = tc[5].sub(tc[4]).norm();
        ell(c, tc[5].add(tdir.mul(0.028)), new V3(0.034, 0.034, 0.034), 9, 6, (n, p) -> mix(LC_DARK, LC_MANE, ss(-0.4, 0.9, n.y) * 0.4));
        ell(c, tc[5].add(tdir.mul(0.054)), new V3(0.024, 0.024, 0.028), 7, 5, flat(LC_DARK));
        // manto real: cae por los costados y ondea detrás al andar
        int NV = 7, NU = 12;
        V3[][] P = new V3[NV + 1][NU + 1];
        V3 brc = br.add(0.016, 0.016, 0.016);
        for (int i = 0; i <= NV; i++) {
            double v = i / (double) NV;
            double z = 0.085 - 0.30 * v;
            for (int j = 0; j <= NU; j++) {
                double u = j / (double) NU * 2 - 1;
                double a = u * (1.45 + 0.2 * v);
                double zc = Math.max(bc.z - br.z * 0.98, z);
                V3 sp = surf(bc, brc, new V3(Math.sin(a), Math.cos(a), (zc - bc.z) * 3.2));
                double out = 1 + 0.06 * Math.abs(u) + 0.04 * v;
                double over = Math.max(0, (bc.z - br.z * 0.85) - z);
                double fl = Math.sin(t * 0.33 + ph + v * 3.4 + u * 1.7) * 0.008 * v * (0.4 + w) + c.roll * 0.04 * v * u;
                double lift = (0.06 * w + 0.02 * c.joy) * v * v;
                P[i][j] = new V3(sp.x * out + fl * Math.signum(u), sp.y + 0.004 - over * 0.9 + lift + fl * 0.4, z - lift * 0.4);
            }
        }
        sheet(c, P, (n, uv) -> mix(CAPE, CAPE2, ss(-0.2, 0.9, n.y) * 0.5));
        // ribete de armiño (con colitas negras) por los bordes que cuelgan
        java.util.List<V3> edge = new java.util.ArrayList<>();
        for (int i = 1; i <= NV; i++) {
            edge.add(P[i][0]);
        }
        for (int j = 1; j <= NU; j++) {
            edge.add(P[NV][j]);
        }
        for (int i = NV - 1; i >= 1; i--) {
            edge.add(P[i][NU]);
        }
        V3[] ep = edge.toArray(new V3[0]);
        tube(c, ep, Pets23.radii(ep.length, 0.015), 6, (n, p) -> mix(ERM, new float[]{0.86F, 0.86F, 0.9F}, ss(0.2, -0.8, n.y) * 0.5));
        for (int i = 1; i < ep.length - 1; i += 3) {
            V3 tg = ep[i + 1].sub(ep[i - 1]).norm();
            V3 o = tg.cross(V3.UP);
            o = o.len() < 0.1 ? new V3(1, 0, 0) : o.norm();
            ell(c, ep[i].add(0, 0.004, 0).add(o.mul(o.dot(ep[i].sub(bc)) > 0 ? 0.013 : -0.013)), new V3(0.0055, 0.009, 0.0055), 5, 4, flat(ERM_DOT));
        }
        // cordón dorado de las esquinas de delante al broche con rubí
        V3 clasp = new V3(0, 0.30 + bob, 0.15);
        for (int k = 0; k <= NU; k += NU) {
            V3 a = P[0][k];
            curve(c, new V3[]{a, a.lerp(clasp, 0.5).add(0, -0.015, 0.02), clasp}, new double[]{0.006, 0.006, 0.006}, 3, 5, flat(GOLD));
        }
        ell(c, clasp, new V3(0.024, 0.024, 0.011), 10, 5, (n, p) -> mix(GOLD, GOLD2, ss(-0.2, 0.9, n.y + n.z * 0.3) * 0.7));
        c.emissive = true;
        ell(c, clasp.add(0, 0, 0.01), new V3(0.012, 0.012, 0.007), 7, 4, flat(RUBY));
        c.emissive = false;
        Detail.glint(c, clasp.add(0, 0, 0.01), new V3(0.012, 0.012, 0.007), new V3(-0.3, 0.5, 0.8), 0.013, WHITE, 0.8);
        // cabeza
        headLook(c, 0.12, 0.03 - 0.22 * roar);
        c.hpv = new V3(0, 0.34, 0.08);
        V3 hc = new V3(0, 0.45 + bob, 0.105), hr = new V3(0.158, 0.145, 0.145);
        // melena: masa de fondo detrás de la cabeza y tres capas de mechones redondeados alrededor de la cara (más cortos
        // arriba, para la corona, y bajo la barbilla)
        V3 mc = hc.add(0, -0.005, -0.07), mr = new V3(0.17, 0.165, 0.12);
        ell(c, mc, mr, 14, 10, (n, p) -> mix(LC_MANE, LC_MANE2, ss(-0.3, 1, n.y) * 0.5));
        // mechones de la nuca (para que de espaldas tampoco se vea liso)
        for (int i = 0; i < 9; i++) {
            double a = Math.PI * 2 * i / 9.0 + 0.3;
            double ca = Math.cos(a), sa = Math.sin(a);
            V3 b0 = surf(mc, mr, new V3(ca * 0.75, sa * 0.75, -0.65));
            V3 o = new V3(ca * 0.55, sa * 0.55 - 0.35, -0.75).norm();
            double sway = Math.sin(t * 0.16 + a * 2 + ph) * 0.005;
            lock(c, new V3[]{b0, b0.add(o.mul(0.03)), b0.add(o.mul(0.055)).add(sway, -0.01, 0)}, 0.036, 0.018, 2, 6,
                    (n, p) -> mix(LC_MANE, LC_MANE2, ss(0.2, 1, n.y) * 0.4));
        }
        for (int L = 2; L >= 0; L--) {
            int N = L == 0 ? 15 : (L == 1 ? 14 : 12);
            for (int i = 0; i < N; i++) {
                double a = Math.PI * 2 * (i + 0.5 * L) / N;
                double ca = Math.cos(a), sa = Math.sin(a);
                double top = ss(0.55, 0.0, Math.abs(a - Math.PI / 2));
                double chin = ss(0.9, 0.0, Math.abs(a - Math.PI * 1.5));
                if (L == 0 && chin > 0.55) {
                    continue;
                }
                double len = (0.058 + 0.014 * L) * (1 + 0.15 * Math.sin(a * 3 + L * 1.7)) * (1 - 0.4 * top) * (1 - 0.45 * chin);
                V3 o = new V3(ca, sa, 0);
                V3 b0 = surf(hc, hr.mul(0.96), new V3(ca, sa * 0.98, 0.10 - 0.30 * L));
                double sway = Math.sin(t * 0.16 + a * 2 + L + ph) * 0.006 + c.joy * 0.012;
                double droop = 0.018 + 0.014 * Math.abs(ca) + bob * 0.6;
                V3[] cp = {b0, b0.add(o.mul(len * 0.5)).add(0, 0, -0.008), b0.add(o.mul(len * 0.88)).add(sway * ca, -droop * 0.5 + sway * sa, -0.02),
                        b0.add(o.mul(len)).add(sway * 1.6 * ca, -droop + sway * sa, -0.03 - 0.006 * L)};
                float[] c0 = L == 0 ? LC_MANE2 : (L == 1 ? mix(LC_MANE2, LC_MANE, 0.55) : LC_MANE);
                double r0 = 0.045 - 0.003 * L;
                lock(c, cp, r0 * (1 - 0.3 * top), 0.02, 2, 6, (n, p) -> mix(c0, LC_GOLD2, ss(0.35, 1, n.y) * 0.32));
            }
        }
        Paint headFur = (n, p) -> {
            float[] b = mix(LC_GOLD, LC_GOLD2, ss(-0.1, 0.8, n.y) * 0.5);
            return mix(b, LC_CREAM, ss(-0.05, -0.55, n.y) * ss(0.1, 0.7, n.z));
        };
        ell(c, hc, hr, 18, 13, headFur);
        // orejas redondas por delante de la melena: dorso oscuro y pelusa de crema
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 151 : 152);
            V3 ec = new V3(sd * 0.112, 0.575 + bob, 0.085);
            double rl = sd * (0.3 + 0.25 * ef);
            ell(c, ec, new V3(0.05, 0.048, 0.026), 0, -0.1, rl, 10, 7, (n, p) -> n.z < -0.1 ? mix(LC_DARK, LC_BACK, 0.25) : mix(LC_GOLD, LC_BACK, 0.15));
            ell(c, ec.add(0, -0.004, 0.012), new V3(0.032, 0.029, 0.012), 0, -0.1, rl, 8, 5, flat(LC_CREAM));
        }
        // mofletes, puntitos y bigotes
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 ck = new V3(sd * 0.044, 0.405 + bob, 0.212);
            V3 ckr = new V3(0.054, 0.042, 0.04);
            ell(c, ck, ckr, 10, 7, (n, p) -> mix(LC_CREAM, WHITE, ss(0, 1, n.y) * 0.3));
            for (int k = 0; k < 3; k++) {
                V3 dp = surf(ck, ckr, new V3(sd * (0.35 + 0.18 * k), 0.25 - 0.18 * k, 0.9));
                ell(c, dp, new V3(0.0042, 0.0042, 0.003), 4, 3, flat(LC_SPOT));
            }
            for (int k = 0; k < 2; k++) {
                V3 a = surf(ck, ckr, new V3(sd * 0.7, 0.05 - 0.25 * k, 0.6));
                Detail.whisker(c, a, a.add(sd * 0.07, 0.01 - 0.02 * k, -0.005), 0.006, 0.002, new float[]{1F, 1F, 0.96F});
            }
        }
        ell(c, new V3(0, 0.48 + bob, 0.226), new V3(0.027, 0.04, 0.018), 0, -0.25, 0, 8, 6, flat(LC_GOLD2));
        ell(c, new V3(0, 0.368 + bob - 0.022 * roar, 0.2), new V3(0.033, 0.023, 0.029), 8, 5, flat(LC_CREAM));
        if (roar > 0.05) {
            V3 mo = new V3(0, 0.381 + bob - 0.008 * roar, 0.226);
            ell(c, mo, new V3(0.029, 0.011 + 0.02 * roar, 0.012), 8, 5, flat(new float[]{0.45F, 0.12F, 0.16F}));
            ell(c, mo.add(0, -0.01 * roar, 0.004), new V3(0.017, 0.008, 0.01), 6, 4, flat(new float[]{1F, 0.52F, 0.58F}));
            for (int sd = -1; sd <= 1; sd += 2) {
                V3 f = mo.add(sd * 0.015, 0.012 + 0.008 * roar, 0.008);
                cone(c, f, f.add(0, -0.013 * roar, 0.002), 0.005, 0.0012, 4, flat(WHITE));
            }
        } else {
            decal(c, new V3(0, 0.405 + bob, 0.212), new V3(0.098, 0.042, 0.041), new V3(0, -0.25, 1), 0.058, 0, T_CAT, LC_NOSE, 0.9, 0.004);
        }
        V3 ns = new V3(0, 0.438 + bob, 0.255);
        ell(c, ns, new V3(0.029, 0.018, 0.016), 9, 5, (n, p) -> mix(LC_NOSE, new float[]{0.8F, 0.48F, 0.48F}, ss(0, 1, n.y) * 0.5));
        ell(c, ns.add(0, -0.011, -0.002), new V3(0.011, 0.011, 0.011), 6, 4, flat(LC_NOSE));
        Detail.glint(c, ns, new V3(0.029, 0.018, 0.016), new V3(-0.3, 0.8, 0.5), 0.011, WHITE, 0.7);
        eyes(c, hc, hr, 0.41, 0.12, 0.90, 0.122, T_EYE, new float[]{1F, 0.74F, 0.32F});
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.leaf(c, hc, hr, new V3(sd * 0.19, -0.08, 0.98), 0.045, sd * 0.35, LC_DARK);
            Detail.brow(c, hc, hr, sd, 0.35, 0.47, 0.82, 0.045, sd * (0.25 + 0.2 * roar), LC_MANE);
        }
        cheeks(c, hc, hr, 0.70, -0.18, 0.66, 0.075);
        // corona chiquita ladeada, con rubí y zafiros
        V3 cr = new V3(-0.015, 0.612 + bob, 0.06);
        double tilt = 0.24;
        V3[] band = new V3[15];
        for (int i = 0; i <= 14; i++) {
            double a = Math.PI * 2 * i / 14.0;
            band[i] = cr.add(Math.cos(a) * 0.06, Math.cos(a) * 0.06 * Math.sin(tilt), Math.sin(a) * 0.054);
        }
        tube(c, band, Pets23.radii(15, 0.015), 7, (n, p) -> mix(GOLD, GOLD2, ss(-0.2, 0.9, n.y) * 0.6));
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6.0 + 0.25;
            V3 b = cr.add(Math.cos(a) * 0.06, Math.cos(a) * 0.06 * Math.sin(tilt) + 0.01, Math.sin(a) * 0.054);
            cone(c, b, b.add(0, 0.043, 0), 0.015, 0.004, 5, flat(GOLD));
            ell(c, b.add(0, 0.047, 0), new V3(0.0085, 0.0085, 0.0085), 5, 4, flat(GOLD2));
        }
        c.emissive = true;
        ell(c, cr.add(0.0, -0.002, 0.056), new V3(0.015, 0.015, 0.008), 6, 4, flat(RUBY));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, cr.add(sd * 0.048, sd * 0.048 * Math.sin(tilt) - 0.002, 0.03), new V3(0.009, 0.009, 0.006), 5, 3, flat(SAPH));
        }
        c.emissive = false;
        Detail.twinkle(c, cr.add(0, 0.02, 0.064), 0.012, WHITE, t, ph, 0.25);
        c.head = false;
    }

    // ------------------------------------------------------------------ CABRITA MONTAÑERA

    private static final float[] GT_CA = {0.80F, 0.55F, 0.32F}, GT_CA2 = {0.92F, 0.70F, 0.46F}, GT_CR = {0.99F, 0.94F, 0.84F},
            GT_CH = {0.30F, 0.19F, 0.13F}, GT_MUZ = {0.97F, 0.86F, 0.80F}, GT_NOSE = {0.42F, 0.27F, 0.25F}, GT_HORN = {0.34F, 0.31F, 0.34F},
            GT_HORN2 = {0.58F, 0.55F, 0.57F}, GT_HOOF = {0.20F, 0.17F, 0.18F}, GT_EAR_IN = {0.98F, 0.74F, 0.72F},
            LEATHER = {0.52F, 0.28F, 0.15F}, LEATHER2 = {0.68F, 0.40F, 0.22F}, STITCH = {1F, 0.92F, 0.72F}, BRASS = {0.98F, 0.76F, 0.28F},
            BRASS2 = {1F, 0.93F, 0.62F}, EDEL = {0.98F, 0.98F, 0.94F}, EDEL_C = {1F, 0.84F, 0.24F}, BL_R = {0.84F, 0.18F, 0.24F},
            BL_W = {0.98F, 0.90F, 0.72F}, BL_T = {0.16F, 0.52F, 0.60F};

    /**
     * Cabrita Montañera: cabrita alpina color caramelo, con raya oscura en el lomo, calcetines de chocolate, cara clara con dos
     * franjas oscuras de los ojos al hocico, hocico alargado con naricita y labio partido, perilla que se mueve al rumiar,
     * orejas horizontales que se sacuden, cuernitos de pizarra con anillos, flequillo y colita que se agita rápido. Lleva un collar
     * de cuero con costuras y un cencerro de latón con badajo, una flor de edelweiss tras la oreja y una mantita tejida a rayas
     * con flecos. Al andar da saltitos; quieta, rumia y a veces hace un topetazo de juego.
     */
    static void mountainGoat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk, idle = 1 - w;
        // trote saltarín: dos botes por zancada
        double bob = Math.abs(Math.sin(c.gait)) * 0.034 * w + c.joy * 0.03;
        // topetazo de juego: baja la cabeza y empuja
        double bu = ((t + ph * 37 + 120) % 470) / 30.0;
        double butt = bu < 1 ? Math.sin(bu * Math.PI) * idle * (1 - c.sleep) : 0;
        double push = bu < 1 ? Math.max(0, Math.sin(bu * Math.PI * 2 - 1.2)) * idle * (1 - c.sleep) : 0;
        // rumiar: la mandíbula va de lado a lado
        double chewOn = ss(0.2, 0.0, w) * (1 - c.sleep) * (1 - butt) * (Math.sin(t * 0.021 + ph) > -0.2 ? 1 : 0);
        double chew = Math.sin(t * 0.85 + ph) * chewOn;
        c.pivot = new V3(0, 0.28, 0);
        Paint coat = (n, p) -> {
            float[] b = mix(GT_CA, GT_CA2, ss(-0.3, 0.7, n.y) * 0.4);
            return mix(b, GT_CR, ss(-0.2, -0.7, n.y));
        };
        // patas finas con rodilla, calcetines oscuros y pezuñas partidas
        double xs = 0.068, zf = 0.085, zb = -0.11, hipY = 0.215 + bob;
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? zf : zb;
                double p = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(p) * 0.06 * c.walk;
                double lift = Math.max(0, Math.cos(p)) * 0.055 * c.walk + c.joy * 0.035;
                V3 hip = new V3(sd * xs, hipY, z0);
                V3 foot = new V3(sd * xs, 0.03 + lift, z0 + sw);
                V3 knee = hip.lerp(foot, 0.5).add(0, 0, (fb == 0 ? 0.012 : -0.016) + sw * 0.2);
                if (fb == 1) {
                    ell(c, hip.add(-sd * 0.008, 0.03, 0.0), new V3(0.042, 0.06, 0.055), 0, -0.2, 0, 9, 6, coat);
                }
                tube(c, new V3[]{hip, knee}, new double[]{0.032, 0.025}, 7, coat);
                ell(c, knee, new V3(0.025, 0.026, 0.025), 6, 4, flat(GT_CH));
                tube(c, new V3[]{knee, foot.add(0, 0.02, 0)}, new double[]{0.024, 0.021}, 7, flat(GT_CH));
                // pezuña partida en dos deditos
                for (int k = -1; k <= 1; k += 2) {
                    ell(c, foot.add(k * 0.011, -0.006, 0.008), new V3(0.012, 0.022, 0.018), 0, 0.15, 0, 6, 4,
                            (n, q) -> mix(GT_HOOF, GT_HORN2, ss(0.3, 1, n.y) * 0.4));
                }
            }
        }
        // cuerpo con joroba en los hombros y raya oscura en el lomo
        V3 bc = new V3(0, 0.29 + bob + 0.005 * c.breath, -0.015), br = new V3(0.11, 0.105, 0.17);
        Paint body = (n, p) -> {
            float[] b = coat.at(n, p);
            double stripe = ss(0.03, 0.012, Math.abs(p.x)) * ss(0.3, 0.75, n.y);
            return mix(b, GT_CH, stripe * 0.9);
        };
        ell(c, bc, br, 16, 11, body);
        V3 hump = bc.add(0, 0.045, 0.075);
        ell(c, hump, new V3(0.095, 0.075, 0.085), 12, 8, body);
        // mantita tejida a rayas sobre el lomo, con flecos
        int NV = 4, NU = 12;
        V3[][] P = new V3[NV + 1][NU + 1];
        for (int i = 0; i <= NV; i++) {
            double v = i / (double) NV;
            double z = 0.06 - 0.15 * v;
            for (int j = 0; j <= NU; j++) {
                double u = j / (double) NU * 2 - 1;
                double a = u * 1.25;
                V3 sp = surf(bc, br.add(0.012, 0.012, 0.012), new V3(Math.sin(a), Math.cos(a), (z - bc.z) * 4));
                double fl = Math.sin(t * 0.3 + ph + u * 2 + v) * 0.004 * w;
                P[i][j] = new V3(sp.x * 1.04, sp.y + 0.006 + fl, z);
            }
        }
        sheet(c, P, (n, uv) -> {
            double s = Math.abs(uv.x - 0.5) * 2;
            float[] b = s > 0.86 ? BL_T : (s > 0.66 ? BL_W : (s > 0.42 ? BL_R : (s > 0.30 ? BL_W : BL_T)));
            return mix(b, WHITE, 0.08 * Math.sin(uv.y * 30) * Math.sin(uv.x * 40));
        });
        for (int sd = 0; sd <= NU; sd += NU) {
            for (int i = 0; i <= NV; i++) {
                V3 e = P[i][sd];
                double sw = Math.sin(t * 0.4 + ph + i) * 0.006 * (0.3 + w);
                cone(c, e, e.add(sd == 0 ? -0.006 : 0.006, -0.03, sw), 0.005, 0.003, 4, flat(BL_W));
            }
        }
        // colita corta hacia arriba que se agita rápido
        double wag = Math.sin(t * 1.3 + ph) * (0.25 + 0.5 * c.joy) * (0.3 + 0.7 * Detail.flick(c, 251)) + Math.sin(t * 0.2) * 0.1;
        V3 tb = bc.add(0, 0.075, -0.16);
        ell(c, tb.add(Math.sin(wag) * 0.02, 0.025, -0.015), new V3(0.022, 0.04, 0.016), wag, -0.55, 0, 7, 5,
                (n, p) -> mix(GT_CA, GT_CH, ss(0.0, 0.035, p.y)));
        // cuello con garganta clara
        V3 n0 = bc.add(0, 0.05, 0.12), n1 = new V3(0, 0.44 + bob, 0.145 + 0.02 * push);
        tube(c, new V3[]{n0, n0.lerp(n1, 0.5).add(0, 0, 0.01), n1}, new double[]{0.065, 0.058, 0.052}, 9,
                (n, p) -> mix(GT_CA, GT_CR, ss(-0.1, -0.7, n.y + n.z * 0.4)));
        // collar de cuero con costuras y cencerro
        V3 cn = n0.lerp(n1, 0.62);
        V3[] ring = new V3[15];
        for (int i = 0; i <= 14; i++) {
            double a = Math.PI * 2 * i / 14.0;
            ring[i] = cn.add(Math.sin(a) * 0.062, -0.018 * Math.cos(a), Math.cos(a) * 0.058);
        }
        tube(c, ring, Pets23.radii(15, 0.013), 6, (n, p) -> mix(LEATHER, LEATHER2, ss(-0.2, 0.9, n.y) * 0.6));
        for (int i = 0; i < 14; i++) {
            V3 d = ring[i].sub(cn);
            V3 o = new V3(d.x, 0, d.z).norm();
            ell(c, ring[i].add(o.mul(0.012)), new V3(0.004, 0.0025, 0.004), 4, 3, flat(STITCH));
        }
        double swing = Math.sin(t * 0.32 + ph) * 0.12 + Math.sin(c.gait * 2) * 0.25 * w + c.joy * Math.sin(t * 1.5) * 0.4 + push * 0.4;
        V3 top = ring[0].add(0, -0.01, 0.006);
        V3 down = new V3(0, -1, 0).rot(new V3(1, 0, 0), -swing).rot(new V3(0, 0, 1), Math.sin(t * 0.27 + ph) * 0.1);
        V3 bt = top.add(down.mul(0.012)), bb = top.add(down.mul(0.05));
        ell(c, bt, new V3(0.016, 0.012, 0.016), 7, 4, flat(BRASS));
        cone(c, bt, bb, 0.019, 0.03, 9, (n, p) -> mix(BRASS, BRASS2, ss(-0.2, 0.9, n.y + n.z * 0.5) * 0.6));
        tube(c, Pets23.ring(bb, 0.031, 0.031, 10), Pets23.radii(11, 0.004), 4, flat(BRASS2));
        ell(c, bb.add(down.mul(0.004)).add(Math.sin(t * 0.9) * 0.004 * (w + c.joy), 0, 0), new V3(0.008, 0.008, 0.008), 5, 3, flat(GT_CH));
        Detail.glint(c, bt.lerp(bb, 0.5), new V3(0.025, 0.02, 0.025), new V3(-0.4, 0.4, 0.8), 0.018, WHITE, 0.75);
        // cabeza
        headLook(c, 0.11, 0.04 + 0.42 * butt);
        c.hpv = new V3(0, 0.43, 0.15);
        V3 hc = new V3(0, 0.52 + bob, 0.17 + 0.02 * push), hr = new V3(0.112, 0.11, 0.115);
        Paint head = (n, p) -> {
            float[] b = mix(GT_CA, GT_CA2, ss(-0.1, 0.8, n.y) * 0.4);
            return mix(b, GT_CR, ss(0.15, 0.75, n.z) * ss(0.65, 0.2, Math.abs(n.x)));
        };
        ell(c, hc, hr, 16, 12, head);
        // hocico alargado (la parte de abajo rumia)
        V3 mz = hc.add(0, -0.055, 0.1), mr = new V3(0.062, 0.055, 0.075);
        ell(c, mz, mr, 0, 0.15, 0, 12, 9, (n, p) -> mix(GT_MUZ, WHITE, ss(0.2, 1, n.y) * 0.2));
        V3 jaw = mz.add(chew * 0.008, -0.04 - 0.006 * Math.abs(chew), -0.008);
        ell(c, jaw, new V3(0.042, 0.022, 0.05), 0, 0.1, 0, 9, 6, flat(GT_MUZ));
        // naricita, orificios y labio partido
        V3 ns = surf(mz, mr, new V3(0, 0.25, 1));
        ell(c, ns.add(0, 0, -0.004), new V3(0.026, 0.014, 0.012), 8, 5, flat(GT_NOSE));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, ns.add(sd * 0.012, -0.002, 0.004), new V3(0.006, 0.004, 0.003), 0, 0, sd * 0.4, 5, 3, flat(new float[]{0.2F, 0.12F, 0.12F}));
        }
        Detail.glint(c, ns.add(0, 0, -0.004), new V3(0.026, 0.014, 0.012), new V3(-0.3, 0.8, 0.5), 0.01, WHITE, 0.6);
        decal(c, mz, mr, new V3(chew * 0.15, -0.35, 1), 0.05, 0, T_CAT, GT_NOSE, 0.9, 0.003);
        // perilla que se mueve al rumiar
        V3 g0 = jaw.add(0, -0.012, -0.01);
        double bs = Math.sin(t * 0.25 + ph) * 0.006 + chew * 0.01;
        lock(c, new V3[]{g0, g0.add(bs * 0.5, -0.03, -0.004), g0.add(bs, -0.055, 0.004)}, 0.016, 0.007, flat(GT_CH));
        for (int sd = -1; sd <= 1; sd += 2) {
            lock(c, new V3[]{g0.add(sd * 0.01, 0, 0), g0.add(sd * 0.013 + bs * 0.5, -0.024, 0), g0.add(sd * 0.012 + bs, -0.04, 0.006)}, 0.011, 0.005, flat(GT_CH));
        }
        // ojos dorados a los lados, cejas y rubor
        eyes(c, hc, hr, 0.52, 0.17, 0.80, 0.104, T_EYE, new float[]{0.98F, 0.74F, 0.26F});
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, hc, hr, sd, 0.45, 0.52, 0.72, 0.042, sd * 0.2 - sd * 0.3 * butt, GT_CH);
        }
        cheeks(c, hc, hr, 0.72, -0.18, 0.6, 0.065);
        // orejas horizontales con punta oscura (se sacuden solas)
        for (int sd = -1; sd <= 1; sd += 2) {
            double ef = Detail.flick(c, sd > 0 ? 253 : 254);
            V3 ec = hc.add(sd * 0.13, 0.045, -0.02);
            double rl = sd * (-0.32 + 0.5 * ef) - sd * 0.25 * butt;
            ell(c, ec, new V3(0.072, 0.024, 0.036), sd * 0.35, 0, rl, 10, 6, (n, p) -> mix(GT_CA, GT_CH, ss(0.035, 0.065, Math.abs(p.x))));
            ell(c, ec.add(sd * 0.004, -0.002, 0.012), new V3(0.05, 0.013, 0.02), sd * 0.35, 0, rl, 8, 5, flat(GT_EAR_IN));
        }
        // cuernitos de pizarra hacia atrás, con anillos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 h0 = hc.add(sd * 0.045, 0.085, 0.0);
            V3[] hp = {h0, h0.add(sd * 0.008, 0.04, -0.012), h0.add(sd * 0.016, 0.065, -0.045), h0.add(sd * 0.02, 0.07, -0.085)};
            V3 ax = hp[3].sub(hp[0]).norm();
            curve(c, hp, new double[]{0.021, 0.017, 0.012, 0.006}, 3, 7, (n, p) -> mix(GT_HORN, GT_HORN2, 0.25 + 0.25 * Math.sin(p.sub(h0).dot(ax) * 300)));
            for (int k = 1; k <= 3; k++) {
                double s = k * 0.2;
                V3 rc = hp[0].lerp(hp[1], Math.min(1, s * 2)).lerp(hp[2], Math.max(0, s * 2 - 1) * 0.5);
                V3 tg = hp[1].sub(hp[0]).norm();
                V3 u = tg.anyPerp(), v = tg.cross(u).norm();
                V3[] rr = new V3[9];
                double rad = 0.021 - 0.005 * k;
                for (int i = 0; i <= 8; i++) {
                    double a = Math.PI * 2 * i / 8.0;
                    rr[i] = rc.add(u.mul(Math.cos(a) * rad)).add(v.mul(Math.sin(a) * rad));
                }
                tube(c, rr, Pets23.radii(9, 0.0035), 4, flat(GT_HORN2));
            }
        }
        // flequillo entre los cuernos
        for (int k = -1; k <= 1; k++) {
            V3 b0 = surf(hc, hr, new V3(k * 0.25, 1, 0.45));
            lock(c, new V3[]{b0, b0.add(k * 0.01, 0.025, 0.02), b0.add(k * 0.018, 0.018, 0.045)}, 0.02, 0.008, (n, p) -> mix(GT_CA2, GT_CA, 0.3));
        }
        // flor de edelweiss detrás de la oreja izquierda
        V3 fc = hc.add(0.1, 0.07, -0.03);
        V3 fn = new V3(0.55, 0.6, 0.4).norm();
        V3 fu = fn.anyPerp(), fv = fn.cross(fu).norm();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8.0 + 0.2;
            V3 d = fu.mul(Math.cos(a)).add(fv.mul(Math.sin(a)));
            V3 pc = fc.add(d.mul(0.018));
            ell(c, pc, new V3(0.016, 0.006, 0.008), 0, 0, 0, 5, 3, flat(EDEL));
            Detail.kite(c, fc, fc.add(d.mul(0.034)).add(fn.mul(0.004)), d.cross(fn).norm(), 0.008, fn, EDEL, mix(EDEL, new float[]{0.86F, 0.92F, 0.82F}, 0.6));
        }
        for (int k = 0; k < 3; k++) {
            double a = k * 2.1;
            ell(c, fc.add(fn.mul(0.006)).add(fu.mul(Math.cos(a) * 0.006)).add(fv.mul(Math.sin(a) * 0.006)), new V3(0.0055, 0.0055, 0.0055), 5, 3, flat(EDEL_C));
        }
        c.head = false;
    }
}
