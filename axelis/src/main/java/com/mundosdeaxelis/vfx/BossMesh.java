package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Mallas suaves de la etapa 4 (fuego), con el mismo motor que las mascotas 3D ({@link Creature3D}), para dibujarlas desde un
 * renderer de entidad: el Corazón de Magma (que late, con coraza de obsidiana que se agrieta), sus cadenas, las anclas de
 * cristal de magma y las larvas de lava. No depende de Minecraft: el renderer da un {@link Sink} y una {@link Cam}.
 *
 * <p>Coordenadas: las que se pasan son relativas a la entidad (el renderer pone el origen en su posición).
 */
public final class BossMesh {
    private BossMesh() {
    }

    private static float[] hex(int c) {
        return rgb(c);
    }

    private static final float[] MAG = hex(0xFF5A14), MAG2 = hex(0xFFB030), MAG3 = hex(0xFFF2B0), DEEP = hex(0xB0200C),
            OBS = hex(0x1B1222), OBS2 = hex(0x3C2C4E), CRUST = hex(0x2A1814), CRUST2 = hex(0x4A2A1E), IRON = hex(0x34303A), IRON2 = hex(0x5E5664),
            EYE = hex(0xFFE36A);

    static Ctx ctx(Sink s, Cam cam, V3 origin, double scale, double yaw) {
        Ctx c = new Ctx(s, cam, origin, scale);
        c.lod = 1.0;
        c.pivot = new V3(0, 0, 0);
        // mismo sombreado con volumen que las mascotas (luz principal que sigue un poco a la cámara, brillo y banda cálida)
        c.pretty = true;
        c.key = c.light.add(cam.fwd.mul(-0.55).add(cam.up.mul(0.75)).add(cam.right.mul(-0.40)).norm()).norm();
        System.arraycopy(rot(yaw, 0, 0), 0, c.m, 0, 9);
        return c;
    }

    /** Latido doble (pum-pum) entre 0 y 1; {@code period} en ticks (más corto = late más rápido). */
    public static double beat(double t, double period) {
        double u = (t % period) / period;
        double a = Math.exp(-Math.pow((u - 0.06) / 0.05, 2));
        double b = 0.65 * Math.exp(-Math.pow((u - 0.24) / 0.06, 2));
        return Math.max(a, b);
    }

    /** Dirección fija de la placa i de la coraza (repartidas por la superficie). */
    private static V3 plateDir(int i) {
        double y = 1 - (i + 0.5) / 14.0 * 2;
        double r = Math.sqrt(Math.max(0, 1 - y * y));
        double a = i * 2.39996;
        return new V3(Math.cos(a) * r, y * 0.92, Math.sin(a) * r).norm();
    }

    private static final V3 BODY = new V3(0, 0, 0), BODY_R = new V3(0.42, 0.46, 0.38);

    /** Punto de la superficie del corazón hacia una dirección (para enganchar cadenas). Unidades del modelo. */
    public static V3 heartSurface(V3 dir) {
        return surf(BODY, BODY_R, dir);
    }

    /**
     * El Corazón de Magma. {@code scale} = tamaño en bloques (alto ≈ 1,3 × scale). {@code beat} 0..1 (latido), {@code plates}
     * placas de coraza que quedan (0..14), {@code crack} 0..1 (las placas se separan y brillan las grietas), {@code heat} brillo
     * extra (fase), {@code anchors} direcciones (relativas, en bloques) a las que salen cadenas, o null.
     */
    public static void heart(Sink s, Cam cam, V3 origin, double scale, double t, double beat, int plates, double crack, double heat, V3[] anchors) {
        Ctx c = ctx(s, cam, origin, scale * (1 + 0.06 * beat), 0);
        double hot = Math.min(1.0, 0.55 + 0.45 * beat + 0.15 * heat);
        // aura de calor
        glow(c, BODY, 2.6, new float[]{1F, 0.45F, 0.12F}, 0.22 + 0.25 * beat + 0.1 * heat);
        glow(c, BODY, 1.4, new float[]{1F, 0.75F, 0.35F}, 0.25 + 0.35 * beat);
        // el corazón de fuego (brilla solo): manchas más calientes que laten
        c.emissive = true;
        final double fb = hot;
        Paint lava = (n, p) -> {
            double k = 0.5 + 0.5 * Math.sin(p.x * 13 + t * 0.07) * Math.sin(p.y * 11 - t * 0.05) * Math.sin(p.z * 9 + 1.3);
            float[] col = mix(DEEP, MAG, ss(0.0, 0.7, k) * fb);
            return mix(col, MAG2, ss(0.7, 1.0, k) * fb);
        };
        ell(c, BODY, BODY_R, 0.0, 0.0, 0.22, 26, 18, lava);
        ell(c, new V3(0.06, -0.3, 0.0), new V3(0.29, 0.28, 0.26), 0.0, 0.0, 0.3, 18, 12, lava);
        cone(c, new V3(0.08, -0.42, 0.0), new V3(0.16, -0.66, 0.02), 0.2, 0.02, 12, lava);
        // aurículas
        ell(c, new V3(-0.24, 0.31, -0.05), new V3(0.23, 0.2, 0.21), 16, 12, lava);
        ell(c, new V3(0.22, 0.33, -0.09), new V3(0.21, 0.19, 0.19), 16, 12, lava);
        c.emissive = false;
        // vasos grandes arriba (aorta y venas), de roca con el borde al rojo
        Paint vessel = (n, p) -> mix(CRUST, CRUST2, ss(-0.3, 0.9, n.y) * 0.7F);
        curve(c, new V3[]{new V3(0.04, 0.36, 0.02), new V3(0.08, 0.6, 0.04), new V3(0.26, 0.74, -0.04), new V3(0.44, 0.62, -0.08)},
                new double[]{0.12, 0.11, 0.1, 0.085}, 4, 12, vessel);
        curve(c, new V3[]{new V3(-0.06, 0.32, 0.12), new V3(-0.1, 0.56, 0.16), new V3(-0.28, 0.66, 0.12), new V3(-0.42, 0.6, 0.06)},
                new double[]{0.1, 0.09, 0.08, 0.07}, 4, 10, vessel);
        curve(c, new V3[]{new V3(-0.2, 0.38, -0.16), new V3(-0.24, 0.62, -0.2), new V3(-0.22, 0.8, -0.22)}, new double[]{0.075, 0.07, 0.065}, 4, 10, vessel);
        c.emissive = true;
        for (V3 tip : new V3[]{new V3(0.44, 0.62, -0.08), new V3(-0.42, 0.6, 0.06), new V3(-0.22, 0.8, -0.22)}) {
            ell(c, tip, new V3(0.06, 0.06, 0.06), 8, 6, flat(mix(MAG, MAG3, beat)));
        }
        c.emissive = false;
        // venas de fuego por la superficie
        c.emissive = true;
        for (int v = 0; v < 6; v++) {
            double a0 = v * 1.047 + 0.3;
            V3[] cp = new V3[5];
            for (int k = 0; k < 5; k++) {
                double y = 0.55 - k * 0.28;
                double a = a0 + Math.sin(k * 1.7 + v) * 0.35;
                V3 d = new V3(Math.cos(a), y, Math.sin(a));
                cp[k] = surf(BODY, BODY_R, d).add(d.norm().mul(0.012));
            }
            curve(c, cp, new double[]{0.02, 0.022, 0.02, 0.016, 0.01}, 3, 5, flat(mix(MAG2, MAG3, beat * 0.7)));
        }
        c.emissive = false;
        // coraza de obsidiana: placas que se agrietan y se van cayendo
        for (int i = 0; i < 14 && i < plates; i++) {
            V3 n = plateDir(i);
            V3 at = surf(BODY, BODY_R, n);
            double yaw = Math.atan2(n.x, n.z), pitch = -Math.asin(Math.max(-1, Math.min(1, n.y)));
            V3 tA = n.cross(V3.UP).len() < 1e-3 ? new V3(1, 0, 0) : n.cross(V3.UP).norm();
            V3 tB = n.cross(tA).norm();
            Paint ob = (nn, p) -> mix(OBS, OBS2, ss(-0.2, 0.9, nn.y * 0.5 + nn.x * 0.3) * 0.8F);
            if (crack <= 0.02) {
                ell(c, at.add(n.mul(0.015)), new V3(0.25, 0.23, 0.05), yaw, pitch, i * 0.7, 12, 6, ob);
            } else {
                // tres pedazos que se separan; entre ellos se ve el fuego
                for (int k = 0; k < 3; k++) {
                    double ang = k * 2.094 + i;
                    V3 off = tA.mul(Math.cos(ang)).add(tB.mul(Math.sin(ang)));
                    V3 pc = at.add(n.mul(0.02 + crack * 0.035)).add(off.mul(0.09 + crack * 0.045));
                    ell(c, pc, new V3(0.13, 0.12, 0.045), yaw, pitch, ang, 9, 5, ob);
                }
                c.emissive = true;
                glow(c, at.add(n.mul(0.05)), 0.22 + crack * 0.2, new float[]{1F, 0.7F, 0.3F}, 0.35 * crack);
                c.emissive = false;
            }
        }
        // llamas que suben
        c.emissive = true;
        for (int k = 0; k < 7; k++) {
            double a = k * 0.9 + 0.4;
            double fl = 0.5 + 0.5 * Math.sin(t * 0.3 + k * 1.9);
            V3 b0 = surf(BODY, BODY_R, new V3(Math.cos(a), 0.7, Math.sin(a)));
            cone(c, b0, b0.add(Math.cos(a) * 0.05, 0.12 + 0.12 * fl + 0.08 * beat, Math.sin(a) * 0.05), 0.05, 0.0, 5,
                    flat(mix(MAG, MAG2, fl)));
        }
        // gotas de lava que caen
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.035 + k * 0.27) % 1.0);
            V3 d = new V3(0.12 * Math.cos(k * 1.7), -0.6 - u * 0.9, 0.12 * Math.sin(k * 1.7));
            double sz = 0.035 * (1 - u * 0.6);
            ell(c, d, new V3(sz, sz * 1.5, sz), 6, 4, flat(mix(MAG2, MAG, u)));
        }
        c.emissive = false;
        // grilletes donde se enganchan las cadenas
        if (anchors != null) {
            for (V3 a : anchors) {
                if (a == null) {
                    continue;
                }
                V3 dir = a.mul(1.0 / scale).norm();
                V3 at = surf(BODY, BODY_R, dir).add(dir.mul(0.03));
                shackle(c, at, dir, 0.09, 0.03);
            }
        }
    }

    /** Aro de hierro grueso cuyo eje es {@code axis}. */
    private static void shackle(Ctx c, V3 at, V3 axis, double r, double th) {
        V3 a = axis.norm();
        V3 u = a.anyPerp().norm(), v = a.cross(u).norm();
        V3[] pts = new V3[9];
        for (int i = 0; i <= 8; i++) {
            double ang = i * Math.PI * 2 / 8;
            pts[i] = at.add(u.mul(Math.cos(ang) * r)).add(v.mul(Math.sin(ang) * r));
        }
        double[] rad = new double[9];
        java.util.Arrays.fill(rad, th);
        tube(c, pts, rad, 5, (n, p) -> mix(IRON, IRON2, ss(-0.3, 0.9, n.y)));
    }

    /**
     * Cadena entre dos puntos (en bloques, relativos a la entidad). Cuelga un poco ({@code sag}); los eslabones cerca de
     * {@code a} brillan al rojo según {@code heat}. {@code broken} 0..1: la cadena se suelta y cae desde el ancla.
     */
    public static void chain(Sink s, Cam cam, V3 a, V3 b, double t, double heat, double sag) {
        Ctx c = ctx(s, cam, new V3(0, 0, 0), 1.0, 0);
        V3 d = b.sub(a);
        double len = d.len();
        if (len < 0.3) {
            return;
        }
        int n = (int) Math.max(2, Math.min(70, len / 0.42));
        V3 prev = a;
        for (int i = 0; i < n; i++) {
            double u0 = (double) i / n, u1 = (double) (i + 1) / n;
            V3 p0 = a.lerp(b, u0).add(0, -sag * 4 * u0 * (1 - u0), 0);
            V3 p1 = a.lerp(b, u1).add(0, -sag * 4 * u1 * (1 - u1), 0);
            V3 mid = p0.lerp(p1, 0.5);
            V3 tg = p1.sub(p0).norm();
            V3 side = tg.anyPerp().norm();
            if (i % 2 == 1) {
                side = tg.cross(side).norm();
            }
            double rx = len / n * 0.64, ry = 0.11;
            V3[] pts = new V3[7];
            for (int k = 0; k <= 6; k++) {
                double ang = k * Math.PI * 2 / 6;
                pts[k] = mid.add(tg.mul(Math.cos(ang) * rx)).add(side.mul(Math.sin(ang) * ry));
            }
            double[] rad = new double[7];
            java.util.Arrays.fill(rad, 0.045);
            double hot = heat * (1 - u0) * (1 - u0);
            c.emissive = hot > 0.35;
            float[] col = mix(IRON, MAG, Math.min(1, hot * 1.2));
            tube(c, pts, rad, 4, (nn, p) -> mix(col, mix(IRON2, MAG2, Math.min(1, hot)), ss(-0.2, 0.9, nn.y) * 0.5F));
            prev = p1;
        }
        c.emissive = false;
    }

    /** Ancla: roca de obsidiana con cristales de magma y un grillete arriba (donde llega la cadena). Mide ~1,6 bloques. */
    public static void anchor(Sink s, Cam cam, V3 origin, double t, double hurt, double life) {
        Ctx c = ctx(s, cam, origin, 1.0, 0);
        double pulse = 0.5 + 0.5 * Math.sin(t * 0.12);
        Paint rock = (n, p) -> mix(OBS, OBS2, ss(-0.2, 0.9, n.y) * 0.7F);
        ell(c, new V3(0, 0.22, 0), new V3(0.5, 0.3, 0.5), 14, 8, rock);
        ell(c, new V3(0.18, 0.12, -0.2), new V3(0.26, 0.18, 0.24), 10, 6, rock);
        c.emissive = true;
        int n = 3 + (int) Math.round(2 * life);
        for (int k = 0; k < n; k++) {
            double a = k * 2.4 + 0.3;
            double h = 0.55 + (k % 3) * 0.22;
            V3 b0 = new V3(Math.cos(a) * 0.18, 0.35, Math.sin(a) * 0.18);
            V3 tip = b0.add(Math.cos(a) * 0.22, h, Math.sin(a) * 0.22);
            float[] col = hurt > 0 ? mix(MAG2, MAG3, hurt) : mix(MAG, MAG2, pulse * 0.6 + 0.2);
            cone(c, b0, tip, 0.11, 0.0, 6, flat(col));
        }
        cone(c, new V3(0, 0.4, 0), new V3(0, 1.25, 0), 0.15, 0.0, 6, flat(hurt > 0 ? MAG3 : mix(MAG2, MAG3, pulse * 0.5)));
        c.emissive = false;
        glow(c, new V3(0, 0.8, 0), 1.6, new float[]{1F, 0.5F, 0.15F}, 0.35 + 0.25 * pulse + 0.4 * hurt);
        shackle(c, new V3(0, 0.95, 0), V3.UP.add(0.001, 0, 0), 0.17, 0.045);
    }

    /** Larva de magma: gusano gordito de 4 segmentos con grietas que brillan, seis patitas y ojos de fuego. Mide ~0,7 bloques. */
    public static void larva(Sink s, Cam cam, V3 origin, double scale, double t, double walk, double yaw, double hurt) {
        Ctx c = ctx(s, cam, origin, scale, yaw);
        double w = Math.min(1, walk);
        Paint crust = (n, p) -> mix(CRUST, CRUST2, ss(-0.2, 0.9, n.y) * 0.7F);
        double[] sz = {0.16, 0.19, 0.17, 0.13};
        double[] zz = {-0.32, -0.12, 0.08, 0.26};
        for (int i = 0; i < 4; i++) {
            double wig = Math.sin(t * 0.5 - i * 0.9) * 0.03 * (0.3 + w);
            V3 sc = new V3(wig, sz[i] * 0.85, zz[i]);
            ell(c, sc, new V3(sz[i], sz[i] * 0.85, sz[i] * 0.9), 12, 8, crust);
            // grieta que brilla entre segmentos y en el lomo
            c.emissive = true;
            float[] gl = hurt > 0 ? MAG3 : mix(MAG, MAG2, 0.5 + 0.5 * Math.sin(t * 0.2 + i));
            if (i > 0) {
                ell(c, new V3(wig, sz[i] * 0.85, (zz[i] + zz[i - 1]) / 2), new V3(sz[i] * 0.8, sz[i] * 0.65, 0.03), 10, 4, flat(gl));
            }
            cone(c, sc.add(0, sz[i] * 0.75, 0), sc.add(0, sz[i] * 0.75 + 0.07, -0.03), 0.035, 0.0, 4, flat(gl));
            c.emissive = false;
        }
        // patitas
        for (int i = 0; i < 3; i++) {
            for (int sd = -1; sd <= 1; sd += 2) {
                double ph = t * 0.6 * (0.3 + w) + i * 2.1 + (sd > 0 ? Math.PI : 0);
                V3 hip = new V3(sd * 0.13, 0.08, -0.2 + i * 0.16);
                V3 foot = hip.add(sd * 0.1, -0.08, Math.sin(ph) * 0.04);
                cone(c, hip, foot, 0.025, 0.012, 4, flat(CRUST));
            }
        }
        // cabeza con ojos de fuego y pinzas
        V3 hc = new V3(0, 0.15, 0.4);
        ell(c, hc, new V3(0.13, 0.11, 0.11), 12, 8, crust);
        c.emissive = true;
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, hc.add(sd * 0.06, 0.035, 0.085), new V3(0.03, 0.025, 0.02), 6, 4, flat(EYE));
            double m = Math.sin(t * 0.4) * 0.03;
            cone(c, hc.add(sd * 0.05, -0.04, 0.09), hc.add(sd * (0.02 + m), -0.06, 0.2), 0.022, 0.0, 4, flat(MAG));
        }
        c.emissive = false;
        glow(c, new V3(0, 0.15, 0), 1.0, new float[]{1F, 0.45F, 0.1F}, 0.25 + 0.3 * hurt);
    }
}
