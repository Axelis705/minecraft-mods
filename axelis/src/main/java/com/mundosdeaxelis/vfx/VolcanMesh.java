package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mallas suaves de los enemigos del volcán (etapa 4), con el mismo motor que el Corazón de Magma ({@link BossMesh}): la
 * salamandra de lava, el diablillo y el cultista del volcán. Coordenadas relativas a la entidad; el frente es +z.
 */
public final class VolcanMesh {
    private VolcanMesh() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    private static final float[] MAG = hex(0xFF5A14), MAG2 = hex(0xFFB030), MAG3 = hex(0xFFF2B0), DEEP = hex(0xB0200C),
            OBS = hex(0x1B1222), OBS2 = hex(0x3C2C4E), EYE = hex(0xFFE36A),
            SKIN = hex(0x221614), SKIN2 = hex(0x4A2A22), BELLY = hex(0xE0702A),
            IMP = hex(0x8E1E14), IMP2 = hex(0xC8402A), IMP_D = hex(0x4A0E0C), HORN = hex(0x2A2022), HORN2 = hex(0x6A5A50),
            WING = hex(0x5A1410), WING2 = hex(0x9A2A1A),
            ROBE = hex(0x2A1414), ROBE2 = hex(0x4E1E16), TRIM = hex(0xD6A048), ASH = hex(0x9A8E88), WOOD = hex(0x2E2022);

    private static float[] hot(float[] c, double hurt) {
        return hurt > 0 ? mix(c, MAG3, Math.min(0.75, hurt)) : c;
    }

    // ------------------------------------------------------------------ salamandra de lava

    /**
     * Salamandra de lava (≈1,6 bloques de largo a escala 1). {@code walk} 0..1, {@code yaw} en radianes, {@code hurt} 0..1,
     * {@code open} 0..1 (abre la boca para escupir lava), {@code heat} 0..1 (brillo de las manchas; más al estar en lava).
     */
    public static void salamander(Sink s, Cam cam, V3 origin, double scale, double t, double walk, double yaw, double hurt, double open,
                                  double heat) {
        Ctx c = BossMesh.ctx(s, cam, origin, scale, yaw);
        double w = Math.min(1, walk);
        double ph = t * 0.55 * (0.25 + w);
        double amp = 0.05 + 0.07 * w;
        // la columna ondula de lado a lado (como nadan y caminan las salamandras)
        java.util.function.DoubleUnaryOperator sway = z -> Math.sin(ph * 1.0 - z * 2.6) * amp * (0.4 + Math.abs(z) * 0.9);
        double[] zs = {-1.05, -0.82, -0.58, -0.34, -0.08, 0.18, 0.38, 0.52};
        double[] ys = {0.12, 0.14, 0.17, 0.2, 0.22, 0.22, 0.24, 0.26};
        double[] rs = {0.018, 0.04, 0.07, 0.12, 0.16, 0.15, 0.1, 0.085};
        V3[] cp = new V3[zs.length];
        for (int i = 0; i < zs.length; i++) {
            cp[i] = new V3(sway.applyAsDouble(zs[i]), ys[i], zs[i]);
        }
        Paint skin = (n, p) -> {
            float[] top = mix(SKIN, SKIN2, ss(-0.2, 0.9, n.y) * 0.6);
            return hot(n.y < -0.25 ? mix(top, BELLY, ss(-0.25, -0.7, n.y)) : top, hurt);
        };
        curve(c, cp, rs, 4, 12, skin);
        // cabeza aplastada con mandíbula que se abre
        V3 neck = cp[cp.length - 1];
        double hx = neck.x + sway.applyAsDouble(0.66) * 0.5;
        V3 hc = new V3(hx, 0.27, 0.66);
        double jaw = 0.08 + 0.5 * open;
        ell(c, hc, new V3(0.13, 0.075, 0.17), 0, -0.05, 0, 14, 8, skin);
        ell(c, hc.add(0, -0.05, -0.01), new V3(0.115, 0.04, 0.15), 0, jaw, 0, 12, 6, (n, p) -> hot(mix(BELLY, SKIN2, 0.4), hurt));
        c.emissive = true;
        if (open > 0.05) {
            // garganta de lava
            ell(c, hc.add(0, -0.03, 0.05), new V3(0.08, 0.035 + 0.04 * open, 0.1), 0, jaw * 0.5, 0, 10, 6, flat(mix(MAG, MAG2, open)));
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 e = hc.add(sd * 0.085, 0.045, 0.06);
            ell(c, e, new V3(0.035, 0.03, 0.035), 8, 6, flat(EYE));
        }
        c.emissive = false;
        for (int sd = -1; sd <= 1; sd += 2) {
            // pupila de rendija
            ell(c, hc.add(sd * 0.115, 0.047, 0.065), new V3(0.006, 0.024, 0.012), 6, 4, flat(OBS));
        }
        // cresta de púas de obsidiana a lo largo del lomo
        for (int i = 0; i < 8; i++) {
            double z = 0.42 - i * 0.14;
            double h = 0.05 + 0.07 * Math.sin(Math.PI * (i + 0.5) / 8.0);
            double y = 0.22 + 0.13 * Math.cos((z - 0.1) * 1.6) - Math.max(0, -z - 0.3) * 0.12;
            V3 base = new V3(sway.applyAsDouble(z), y + 0.06, z);
            cone(c, base, base.add(0, h, -0.05), 0.03, 0.0, 5, (n, p) -> hot(mix(OBS, OBS2, ss(-0.5, 1.0, n.y)), hurt));
        }
        // manchas de magma que laten
        c.emissive = true;
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.18);
        for (int i = 0; i < 7; i++) {
            double z = 0.3 - i * 0.18;
            for (int sd = -1; sd <= 1; sd += 2) {
                if ((i + (sd > 0 ? 1 : 0)) % 2 == 0) {
                    continue;
                }
                double r = 0.14 - Math.max(0, -z - 0.2) * 0.12;
                V3 sp = new V3(sway.applyAsDouble(z) + sd * r * 0.75, 0.21 + r * 0.45, z);
                ell(c, sp, new V3(0.035, 0.018, 0.045), 8, 4, flat(mix(MAG, MAG2, pulse * (0.4 + heat * 0.6))));
            }
        }
        c.emissive = false;
        // grietas de lava que recorren los costados
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] cr = new V3[6];
            double[] rr = new double[6];
            for (int i = 0; i < 6; i++) {
                double z = 0.36 - i * 0.2;
                double r = 0.15 - Math.max(0, -z - 0.25) * 0.14;
                cr[i] = new V3(sway.applyAsDouble(z) + sd * r * 0.93, 0.205 + Math.sin(i * 1.7 + sd) * 0.025, z);
                rr[i] = 0.017 - i * 0.0018;
            }
            curve(c, cr, rr, 3, 4, flat(mix(MAG, MAG2, pulse * (0.3 + heat * 0.7))));
        }
        c.emissive = false;
        // patas abiertas (como lagarto) con garras
        double[] hz = {0.28, -0.26};
        for (int k = 0; k < 2; k++) {
            for (int sd = -1; sd <= 1; sd += 2) {
                double g = ph * 2.0 + (k == 0 ? 0 : Math.PI) + (sd > 0 ? Math.PI : 0);
                double sw = Math.sin(g) * 0.12 * (0.2 + w);
                double lift = Math.max(0, Math.cos(g)) * 0.05 * (0.2 + w);
                V3 hip = new V3(sway.applyAsDouble(hz[k]) + sd * 0.11, 0.19, hz[k]);
                V3 knee = hip.add(sd * 0.12, 0.02 + lift, sw * 0.5);
                V3 foot = new V3(hip.x + sd * 0.17, 0.02 + lift, hz[k] + sw + (k == 0 ? 0.04 : -0.02));
                Paint leg = (n, p) -> hot(mix(SKIN, SKIN2, ss(-0.3, 0.9, n.y) * 0.5), hurt);
                cone(c, hip, knee, 0.065, 0.05, 6, leg);
                cone(c, knee, foot, 0.05, 0.035, 6, leg);
                ell(c, foot.add(0, 0.0, 0.02), new V3(0.05, 0.022, 0.06), 8, 4, leg);
                for (int f = -1; f <= 1; f++) {
                    V3 dir = new V3(sd * 0.05 + f * 0.03, -0.01, 0.06 + Math.abs(f) * -0.015);
                    cone(c, foot.add(0, 0, 0.03), foot.add(dir).add(0, 0, 0.04), 0.016, 0.0, 4, flat(hot(OBS2, hurt)));
                }
            }
        }
        // gotas de lava que caen de la panza
        c.emissive = true;
        for (int i = 0; i < 2; i++) {
            double u = ((t * 0.04 + i * 0.5) % 1.0);
            double z = -0.1 + i * 0.25;
            V3 d = new V3(sway.applyAsDouble(z), 0.1 - u * 0.12, z);
            if (u < 0.85) {
                ell(c, d, new V3(0.018, 0.026 - u * 0.012, 0.018), 6, 4, flat(mix(MAG2, MAG, u)));
            }
        }
        c.emissive = false;
        glow(c, new V3(0, 0.25, 0.0), 1.3, new float[]{1F, 0.42F, 0.1F}, 0.18 + 0.25 * heat + 0.3 * hurt + 0.3 * open);
    }

    // ------------------------------------------------------------------ diablillo

    /**
     * Diablillo (≈0,9 bloques de alto a escala 1): bichito rojo que vuela con alas de murciélago, cuernos, sonrisa de fuego y
     * cola de flecha. {@code charge} 0..1: sostiene una bola de fuego que crece en la mano. {@code dive} 0..1: se lanza en picada.
     */
    public static void imp(Sink s, Cam cam, V3 origin, double scale, double t, double yaw, double hurt, double charge, double dive) {
        Ctx c = BossMesh.ctx(s, cam, origin, scale, yaw);
        double bob = Math.sin(t * 0.25) * 0.04;
        double lean = 0.25 + dive * 0.7;
        Paint body = (n, p) -> hot(mix(IMP_D, IMP, ss(-0.4, 0.9, n.y)), hurt);
        V3 bc = new V3(0, 0.42 + bob, 0);
        // inclinado hacia delante al volar (y mucho más al lanzarse)
        V3 fwd = new V3(0, -Math.sin(lean) * 0.05, Math.sin(lean) * 0.06);
        ell(c, bc.add(fwd), new V3(0.15, 0.19, 0.13), 0, lean * 0.6, 0, 12, 8, body);
        ell(c, bc.add(fwd).add(0, -0.02, 0.07), new V3(0.1, 0.13, 0.07), 0, lean * 0.6, 0, 10, 6, (n, p) -> hot(mix(IMP, IMP2, 0.6), hurt));
        // cabeza grande
        V3 hc = new V3(0, 0.74 + bob, 0.04 + Math.sin(lean) * 0.12);
        ell(c, hc, new V3(0.16, 0.14, 0.14), 14, 10, body);
        // cuernos curvos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] hp = {hc.add(sd * 0.08, 0.1, 0.0), hc.add(sd * 0.14, 0.18, -0.04), hc.add(sd * 0.15, 0.27, -0.12), hc.add(sd * 0.12, 0.31, -0.2)};
            curve(c, hp, new double[]{0.035, 0.026, 0.014, 0.0}, 3, 6, (n, p) -> hot(mix(HORN, HORN2, ss(-0.3, 1.0, n.y) * 0.7), hurt));
            // orejas puntiagudas
            cone(c, hc.add(sd * 0.14, 0.01, -0.01), hc.add(sd * 0.27, 0.06, -0.05), 0.04, 0.0, 5, body);
        }
        // ojos de fuego y sonrisa
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.06, 0.025, 0.115), new V3(0.04, 0.032, 0.025), 0, 0, sd * -0.35, 8, 6, flat(EYE));
        }
        for (int i = -4; i <= 4; i++) {
            double x = i * 0.018;
            double y = -0.045 + (x * x) * 2.2;
            ell(c, hc.add(x, y, 0.128 - x * x * 1.2), new V3(0.012, 0.008 + (Math.abs(i) < 3 ? 0.004 : 0), 0.008), 6, 4, flat(MAG2));
        }
        c.emissive = false;
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.06, 0.025, 0.135), new V3(0.008, 0.022, 0.008), 6, 4, flat(OBS));
        }
        // alas de murciélago que aletean
        double flap = Math.sin(t * (1.1 + dive * 0.6)) * (0.75 - dive * 0.35) + 0.15 - dive * 0.5;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = bc.add(sd * 0.1, 0.12, -0.08);
            double a = sd * flap;
            V3 out = new V3(sd * Math.cos(flap), Math.sin(flap), -0.25).norm();
            V3 tip = sh.add(out.mul(0.42));
            V3 mid = sh.add(out.mul(0.22)).add(0, 0.05, 0);
            Paint bone = (n, p) -> hot(mix(IMP_D, IMP, 0.4), hurt);
            curve(c, new V3[]{sh, mid, tip}, new double[]{0.025, 0.018, 0.006}, 3, 5, bone);
            // la membrana: dos lóbulos finos colgando del hueso
            for (int k = 0; k < 2; k++) {
                V3 ce = sh.lerp(tip, 0.35 + k * 0.32).add(0, -0.08 - k * 0.01, -0.02);
                ell(c, ce, new V3(0.13 - k * 0.03, 0.09 - k * 0.015, 0.008), sd * 0.3, 0, a * 0.9, 10, 4,
                        (n, p) -> hot(mix(WING, WING2, ss(-0.6, 0.8, n.y) * 0.6 + 0.2), hurt));
            }
        }
        // brazos (uno sostiene la bola de fuego) y piernitas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = bc.add(sd * 0.14, 0.1, 0.02);
            boolean holds = sd > 0 && charge > 0.01;
            V3 hand = holds ? sh.add(sd * 0.1, 0.12 + 0.08 * charge, 0.12) : sh.add(sd * 0.06, -0.16, 0.08 + Math.sin(t * 0.3 + sd) * 0.02);
            cone(c, sh, hand, 0.035, 0.025, 6, body);
            for (int f = -1; f <= 1; f++) {
                cone(c, hand, hand.add(f * 0.015, holds ? 0.03 : -0.035, 0.03), 0.01, 0.0, 4, flat(hot(HORN, hurt)));
            }
            V3 hip = bc.add(sd * 0.07, -0.15, 0.0);
            V3 foot = hip.add(sd * 0.02, -0.17, 0.03 + Math.sin(t * 0.2 + sd) * 0.03 - dive * 0.08);
            cone(c, hip, foot, 0.035, 0.022, 6, body);
            ell(c, foot.add(0, -0.01, 0.02), new V3(0.03, 0.018, 0.04), 6, 4, flat(hot(HORN, hurt)));
            if (holds) {
                c.emissive = true;
                double r = 0.03 + 0.055 * charge + Math.sin(t * 0.9) * 0.006;
                ell(c, hand.add(0, r + 0.02, 0.02), new V3(r, r, r), 10, 8, flat(mix(MAG2, MAG3, 0.35 + 0.3 * Math.sin(t * 0.7))));
                c.emissive = false;
                glow(c, hand.add(0, r + 0.02, 0.02), 0.5 + 1.2 * charge, new float[]{1F, 0.55F, 0.15F}, 0.45);
            }
        }
        // cola con punta de flecha
        double tw = Math.sin(t * 0.3) * 0.08;
        V3[] tp = {bc.add(0, -0.12, -0.1), bc.add(tw * 0.5, -0.28, -0.22), bc.add(tw, -0.3, -0.38), bc.add(tw * 1.5, -0.18, -0.48)};
        curve(c, tp, new double[]{0.03, 0.02, 0.014, 0.01}, 3, 5, body);
        V3 tip = tp[3];
        cone(c, tip.add(0, -0.02, 0.0), tip.add(0, 0.07, -0.04), 0.05, 0.0, 4, flat(hot(IMP_D, hurt)));
        glow(c, hc.add(0, 0.02, 0.22), 0.6, new float[]{1F, 0.8F, 0.3F}, 0.2 + 0.3 * hurt);
    }

    // ------------------------------------------------------------------ cultista del volcán

    /**
     * Cultista del volcán (≈1,9 bloques de alto a escala 1): túnica larga de ceniza y brasa con bordes dorados, capucha, máscara
     * de obsidiana con ojos de fuego y un bastón con un cristal de magma. {@code cast} 0..1 levanta los brazos y enciende las
     * runas que le giran alrededor.
     */
    public static void cultist(Sink s, Cam cam, V3 origin, double scale, double t, double walk, double yaw, double hurt, double cast) {
        Ctx c = BossMesh.ctx(s, cam, origin, scale, yaw);
        double w = Math.min(1, walk);
        double step = Math.sin(t * 0.5 * (0.2 + w)) * w;
        double bob = Math.abs(step) * 0.03;
        // la túnica: un tubo de la orilla a los hombros que se mece al andar
        double[] ty = {0.02, 0.3, 0.75, 1.1, 1.36};
        double[] tr = {0.4, 0.33, 0.25, 0.23, 0.2};
        V3[] tp = new V3[ty.length];
        for (int i = 0; i < ty.length; i++) {
            double k = 1 - ty[i] / 1.36;
            tp[i] = new V3(step * 0.05 * k, ty[i] + bob, -step * 0.03 * k + 0.0);
        }
        Paint robe = (n, p) -> hot(mix(ROBE, ROBE2, ss(-0.4, 0.9, n.y + n.z * 0.3) * 0.7), hurt);
        tube(c, tp, tr, 16, robe);
        // bordes dorados: dos anillos en la orilla y una franja al frente
        Paint gold = (n, p) -> hot(mix(TRIM, MAG3, ss(0.3, 1.0, n.y) * 0.3), hurt);
        for (double yy : new double[]{0.06, 0.17}) {
            double r = 0.4 + (0.33 - 0.4) * (yy - 0.02) / 0.28 + 0.012;
            V3[] ring = new V3[17];
            double[] rr = new double[17];
            for (int i = 0; i <= 16; i++) {
                double a = i / 16.0 * Math.PI * 2;
                double k = 1 - yy / 1.36;
                ring[i] = new V3(Math.sin(a) * r + step * 0.05 * k, yy + bob, Math.cos(a) * r - step * 0.03 * k);
                rr[i] = yy < 0.1 ? 0.03 : 0.014;
            }
            tube(c, ring, rr, 5, gold);
        }
        V3[] stripe = new V3[5];
        double[] sr = new double[5];
        for (int i = 0; i < 5; i++) {
            double yy = 0.22 + i * 0.27;
            double r = yy < 0.3 ? 0.33 : yy < 0.75 ? 0.33 + (0.25 - 0.33) * (yy - 0.3) / 0.45 : yy < 1.1 ? 0.25 - 0.02 * (yy - 0.75) / 0.35 : 0.23 - 0.03 * (yy - 1.1) / 0.26;
            double k = 1 - yy / 1.36;
            stripe[i] = new V3(step * 0.05 * k, yy + bob, r + 0.004 - step * 0.03 * k);
            sr[i] = 0.022;
        }
        tube(c, stripe, sr, 5, gold);
        // fondo de la túnica (para que no se vea hueca desde abajo)
        ell(c, tp[0].add(0, 0.0, 0), new V3(0.39, 0.01, 0.39), 14, 4, flat(ROBE));
        // runas de brasa en la falda
        c.emissive = true;
        double glowK = 0.35 + 0.65 * cast;
        for (int i = 0; i < 6; i++) {
            double a = i / 6.0 * Math.PI * 2 + 0.3;
            double r = 0.36;
            V3 rp = new V3(Math.sin(a) * r + step * 0.04, 0.2 + bob, Math.cos(a) * r);
            ell(c, rp, new V3(0.03, 0.045, 0.03), 6, 4, flat(mix(DEEP, MAG2, glowK * (0.6 + 0.4 * Math.sin(t * 0.3 + i)))));
        }
        c.emissive = false;
        // manto sobre los hombros
        V3 top = tp[tp.length - 1];
        ell(c, top.add(0, 0.02, 0), new V3(0.3, 0.11, 0.24), 14, 8, (n, p) -> hot(mix(ROBE2, ASH, ss(0.0, 1.0, n.y) * 0.25), hurt));
        // capucha y máscara
        V3 hc = top.add(0, 0.2, 0.01);
        ell(c, hc, new V3(0.18, 0.21, 0.19), 14, 10, robe);
        cone(c, hc.add(0, 0.14, -0.08), hc.add(0, 0.3, -0.24), 0.08, 0.0, 6, robe);
        ell(c, hc.add(0, -0.02, 0.1), new V3(0.12, 0.15, 0.09), 12, 8, flat(new float[]{0.04F, 0.02F, 0.02F}));
        ell(c, hc.add(0, -0.02, 0.155), new V3(0.11, 0.135, 0.04), 12, 8, (n, p) -> hot(mix(OBS, OBS2, ss(-0.4, 1.0, n.y) * 0.8), hurt));
        c.emissive = true;
        float[] eye = mix(MAG, MAG3, cast * 0.6 + 0.2 * Math.sin(t * 0.2));
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.045, 0.015, 0.19), new V3(0.034, 0.011, 0.008), 8, 4, flat(eye));
        }
        // grieta de lava en la máscara
        cone(c, hc.add(0.075, 0.09, 0.17), hc.add(0.03, 0.035, 0.19), 0.007, 0.003, 4, flat(MAG));
        cone(c, hc.add(0.03, 0.035, 0.19), hc.add(0.06, -0.08, 0.185), 0.006, 0.002, 4, flat(MAG));
        c.emissive = false;
        // brazos: abajo al andar, arriba al lanzar el conjuro
        V3 staffHand = null;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = top.add(sd * 0.25, -0.02, 0.0);
            V3 down = sh.add(sd * 0.08, -0.45, 0.14 + (sd > 0 ? 0.08 : -step * 0.05));
            V3 up = sh.add(sd * 0.16, 0.42, 0.14);
            V3 hand = down.lerp(up, ss(0.0, 1.0, cast));
            V3 elbow = sh.lerp(hand, 0.5).add(sd * 0.08, -0.03, -0.02);
            curve(c, new V3[]{sh, elbow, hand}, new double[]{0.1, 0.12, 0.15}, 3, 10, robe);
            ell(c, hand.add(hand.sub(elbow).norm().mul(0.06)), new V3(0.045, 0.05, 0.045), 8, 6, flat(hot(ASH, hurt)));
            if (sd > 0) {
                staffHand = hand.add(hand.sub(elbow).norm().mul(0.06));
            }
        }
        // bastón con cristal de magma
        V3 sb = staffHand.add(0.02, -0.95 + cast * 0.4, 0.04);
        V3 st = staffHand.add(0.02, 0.38 + cast * 0.12, 0.04);
        if (sb.y < 0.0) {
            sb = new V3(sb.x, 0.0, sb.z);
        }
        cone(c, sb, st, 0.04, 0.034, 6, (n, p) -> hot(mix(WOOD, OBS2, ss(-0.3, 1.0, n.y) * 0.5), hurt));
        for (int k = 0; k < 3; k++) {
            double a = k * 2.1 + t * 0.02;
            cone(c, st, st.add(Math.cos(a) * 0.07, 0.09, Math.sin(a) * 0.07), 0.015, 0.0, 4, flat(hot(OBS2, hurt)));
        }
        c.emissive = true;
        V3 cr = st.add(0, 0.14, 0);
        double cs = 0.07 + 0.03 * cast;
        cone(c, cr.add(0, -cs * 1.2, 0), cr, 0.0001, cs * 0.75, 6, flat(mix(MAG, MAG2, 0.5 + 0.5 * Math.sin(t * 0.25))));
        cone(c, cr, cr.add(0, cs * 1.5, 0), cs * 0.75, 0.0001, 6, flat(mix(MAG2, MAG3, 0.4 + 0.6 * cast)));
        c.emissive = false;
        glow(c, cr, 0.8 + 1.6 * cast, new float[]{1F, 0.5F, 0.12F}, 0.3 + 0.4 * cast);
        // runas que giran alrededor al conjurar
        if (cast > 0.05) {
            c.emissive = true;
            for (int i = 0; i < 4; i++) {
                double a = t * 0.12 + i * Math.PI / 2;
                V3 rp = new V3(Math.cos(a) * 0.62, 1.0 + 0.15 * Math.sin(t * 0.2 + i), Math.sin(a) * 0.62);
                ell(c, rp, new V3(0.05 * cast, 0.07 * cast, 0.012), 0, 0, a, 6, 4, flat(MAG2));
                glow(c, rp, 0.35 * cast, new float[]{1F, 0.6F, 0.2F}, 0.4);
            }
            c.emissive = false;
        }
        glow(c, hc.add(0, 0, 0.2), 0.45, new float[]{1F, 0.45F, 0.12F}, 0.15 + 0.3 * cast + 0.3 * hurt);
    }
}
