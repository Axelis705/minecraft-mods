package com.mundosdeaxelis.vfx;

import com.mundosdeaxelis.vfx.Creature3D.Ctx;
import com.mundosdeaxelis.vfx.Creature3D.Paint;

/**
 * Dragoncito en 3D: está hecho de esferas achatadas, tubos y membranas que se generan en cada cuadro
 * (así puede mover las alas, la cola, la cabeza y parpadear). Usa solo cuadriláteros: no depende de Minecraft.
 *
 * Marco local del modelo: +z = hacia donde mira, +y = arriba, los pies en y = 0, tamaño total = 1 (alto de la cabeza con cuernos).
 */
final class Dragon3D {
    private Dragon3D() {
    }

    // colores (mismo estilo que el dibujo anterior: cuerpo naranja, barriga crema, alas rojo-naranja, cuernos blancos)
    private static final float[] ORANGE = {1.00F, 0.63F, 0.21F};
    private static final float[] ORANGE_L = {1.00F, 0.77F, 0.38F};
    private static final float[] CREAM = {1.00F, 0.91F, 0.64F};
    private static final float[] RED = {0.93F, 0.37F, 0.14F};
    private static final float[] DRED = {0.74F, 0.22F, 0.10F};
    private static final float[] WHITE = {0.98F, 0.96F, 0.88F};
    private static final float[] FOOT = {0.97F, 0.52F, 0.18F};
    private static final float[] DARK = {0.30F, 0.15F, 0.08F};

    // Etapa 253: el dragoncito usa el motor de las mascotas (Creature3D): mismo sombreado con volumen, luz del mundo, ojos
    // nuevos con brillos, dibujo por rejilla (cada vértice se sombrea una vez) y te mira cuando está quieto.

    /** Esfera achatada; pitch gira la pieza alrededor del eje x. La pintura recibe la posición del modelo (no la relativa). */
    private static void ell(Ctx c, V3 ce, V3 r, double pitch, int nu, int nv, Paint paint) {
        Creature3D.ell(c, ce, r, 0, pitch, 0, nu, nv, (n, q) -> paint.at(n, q.add(ce)));
    }

    private static void tube(Ctx c, V3[] pts, double[] rad, int sides, Paint paint) {
        Creature3D.tube(c, pts, rad, sides, paint);
    }

    private static V3[] spline(V3[] cp, double[] rad, int sub, double[][] outRad) {
        return Creature3D.spline(cp, rad, sub, outRad);
    }

    private static double[] rot(double yaw, double pitch, double roll) {
        return Creature3D.rot(yaw, pitch, roll);
    }

    private static float[] mixc(float[] a, float[] b, double t) {
        return Creature3D.mix(a, b, t);
    }

    private static double ss(double a, double b, double x) {
        return Creature3D.ss(a, b, x);
    }

    // ------------------------------------------------------------------ pinturas

    private static final Paint BODY = (n, p) -> {
        double belly = ss(-0.10, -0.62, n.y) * ss(-0.30, 0.15, p.z + 0.10 - Math.abs(p.x) * 0.2 + 0.1);
        float[] top = mixc(ORANGE, ORANGE_L, ss(0.1, -0.5, -n.y) * 0.5);
        float[] c = mixc(top, CREAM, belly);
        if (belly > 0.5) {
            double band = 0.95 + 0.05 * Math.sin(p.z * 70);
            c = new float[]{(float) (c[0] * band), (float) (c[1] * band), (float) (c[2] * band)};
        }
        return c;
    };

    private static final Paint ORANGE_P = (n, p) -> mixc(ORANGE, ORANGE_L, ss(0.0, 0.9, n.y) * 0.6);
    private static final Paint HEAD_P = (n, p) -> mixc(ORANGE, ORANGE_L, 0.25 + ss(-0.2, 0.9, n.y) * 0.35);
    private static final Paint SNOUT_P = (n, p) -> mixc(ORANGE_L, CREAM, ss(0.0, -0.9, n.y) * 0.55);
    private static final Paint LEG_P = (n, p) -> mixc(ORANGE, ORANGE_L, ss(0.0, 0.8, n.y) * 0.4);
    private static final Paint FOOT_P = (n, p) -> FOOT;
    private static final Paint WHITE_P = (n, p) -> mixc(WHITE, CREAM, ss(0.3, -0.8, n.y) * 0.35);
    private static final Paint RED_P = (n, p) -> mixc(RED, ORANGE, ss(0.0, 0.8, n.y) * 0.3);
    private static final Paint BONE_P = (n, p) -> mixc(RED, ORANGE, ss(-0.5, 0.9, n.y) * 0.6);
    private static final Paint DARK_P = (n, p) -> DARK;

    // ------------------------------------------------------------------ entrada

    static void emit(VfxData d, double age, double partial, V3 pos, Cam cam, Sink s) {
        double scale = d.size;
        if (scale <= 0) {
            return;
        }
        double ph = d.id * 1.7;
        double t = age;

        // orientación suavizada (el cliente la va acercando al objetivo cada tick)
        double yaw;
        if (d.yawInit) {
            double dy = d.yaw - d.pyaw;
            while (dy > Math.PI) {
                dy -= 2 * Math.PI;
            }
            while (dy < -Math.PI) {
                dy += 2 * Math.PI;
            }
            yaw = d.pyaw + dy * partial;
        } else {
            yaw = (Math.abs(d.dx) + Math.abs(d.dz) > 1.0E-4) ? Math.atan2(d.dx, d.dz) : 0;
        }
        V3 fwd = new V3(Math.sin(yaw), 0, Math.cos(yaw));
        V3 left = new V3(Math.cos(yaw), 0, -Math.sin(yaw));
        V3 vel = new V3(d.x - d.px, d.y - d.py, d.z - d.pz);
        double vf = vel.dot(fwd), vl = vel.dot(left), vy = vel.y;
        double pitch = Math.max(-0.30, Math.min(0.45, vf * 1.1 - vy * 0.5));
        double roll = Math.max(-0.30, Math.min(0.30, vl * 1.0));
        double speed = Math.min(1, vel.len() * 2.0);

        double bob = Math.sin(t * 0.30 + ph + 0.9) * 0.018;
        double flapRate = 0.38 + speed * 0.10;
        double fl = Math.sin(t * flapRate + ph);
        double flD = Math.cos(t * flapRate + ph);
        double wingA = 0.98 + 0.42 * fl;

        Ctx c = new Ctx(s, cam, pos.add(0, bob * scale, 0), scale);
        double dist = cam.pos.sub(pos).len();
        if (dist > 64) {
            return;
        }
        c.lod = dist < 7 ? 1.35 : (dist < 12 ? 1.1 : (dist < 28 ? 0.72 : 0.5));
        c.pretty = true;
        c.world = d.light;
        c.key = c.light.add(cam.fwd.mul(-0.55).add(cam.up.mul(0.75)).add(cam.right.mul(-0.40)).norm()).norm();
        c.pivot = new V3(0, 0.36, -0.06);
        c.hpv = new V3(0, 0.47, 0.20);
        c.t = t;
        c.ph = ph;
        c.pitch = pitch;
        c.roll = roll;
        c.breath = Math.sin(t * 0.11 + ph);
        // te mira de vez en cuando si va despacio
        {
            V3 tc = cam.pos.sub(pos.add(0, 0.5 * scale, 0));
            double lx = tc.dot(left), lz = tc.dot(fwd);
            c.lookYaw = Math.atan2(lx, lz);
            c.lookPitch = -Math.atan2(tc.y, Math.sqrt(lx * lx + lz * lz) + 1.0E-6);
            double aU = (t + ph * 13) % 260;
            double att = ss(0, 14, aU) * (1 - ss(150, 172, aU));
            c.look = att * (1 - Math.min(1, speed * 3)) * (1 - ss(1.3, 2.0, Math.abs(c.lookYaw)));
        }
        double[] bm = rot(yaw, pitch, roll);
        System.arraycopy(bm, 0, c.m, 0, 9);

        // ---------------- cola
        {
            V3[] cp = new V3[7];
            double[] cr = {0.105, 0.092, 0.076, 0.060, 0.046, 0.035, 0.030};
            double[][] base = {{0, 0.34, -0.20}, {0, 0.30, -0.33}, {0, 0.25, -0.45}, {0, 0.23, -0.57}, {0, 0.28, -0.67}, {0, 0.36, -0.73}, {0, 0.44, -0.74}};
            for (int i = 0; i < 7; i++) {
                double w = Math.pow(i / 6.0, 1.3);
                double sx = Math.sin(t * 0.17 + ph - i * 0.75) * 0.075 * w + roll * 0.0;
                double sy = Math.sin(t * 0.21 + ph - i * 0.6) * 0.03 * w;
                cp[i] = new V3(base[i][0] + sx, base[i][1] + sy, base[i][2]);
            }
            double[][] rr = new double[1][];
            V3[] sp = spline(cp, cr, 3, rr);
            tube(c, sp, rr[0], 8, (n, p) -> mixc(ORANGE, ORANGE_L, ss(0.1, -0.8, n.y) * 0.45));
            V3 tip = cp[6];
            ell(c, tip.add(0, 0.012, -0.012), new V3(0.050, 0.052, 0.058), 0, 10, 7, RED_P);
            // pequeña punta
            tube(c, new V3[]{tip.add(0, 0.03, -0.02), tip.add(0, 0.085, -0.045)}, new double[]{0.028, 0.0}, 5, RED_P);
        }

        // ---------------- alas (detrás del cuerpo para que se vea bien la silueta)
        for (int sd = -1; sd <= 1; sd += 2) {
            wing(c, sd, wingA, flD, t, ph);
        }

        // ---------------- patas
        double sw = Math.sin(t * 0.30 + ph);
        for (int sd = -1; sd <= 1; sd += 2) {
            double k = sd * 0.5;
            // delanteras
            double dz = Math.sin(t * 0.30 + ph + k) * 0.015;
            ell(c, new V3(sd * 0.095, 0.15, 0.135), new V3(0.07, 0.10, 0.075), 0, 10, 7, LEG_P);
            ell(c, new V3(sd * 0.097, 0.045, 0.165 + dz), new V3(0.076, 0.043, 0.096), 0, 10, 6, FOOT_P);
            for (int j = -1; j <= 1; j++) {
                V3 b = new V3(sd * 0.097 + j * 0.034, 0.036, 0.250 + dz);
                tube(c, new V3[]{b, b.add(j * 0.006, -0.006, 0.040)}, new double[]{0.015, 0.002}, 4, WHITE_P);
            }
            // traseras
            double dz2 = Math.sin(t * 0.30 + ph + 2 + k) * 0.015;
            ell(c, new V3(sd * 0.115, 0.155, -0.145), new V3(0.088, 0.115, 0.105), 0, 10, 7, LEG_P);
            ell(c, new V3(sd * 0.115, 0.045, -0.105 + dz2), new V3(0.080, 0.043, 0.100), 0, 10, 6, FOOT_P);
            for (int j = -1; j <= 1; j++) {
                V3 b = new V3(sd * 0.115 + j * 0.034, 0.036, -0.012 + dz2);
                tube(c, new V3[]{b, b.add(j * 0.006, -0.006, 0.040)}, new double[]{0.015, 0.002}, 4, WHITE_P);
            }
        }

        // ---------------- cuerpo
        ell(c, new V3(0, 0.365, -0.06), new V3(0.19, 0.19, 0.26), 0, 16, 11, BODY);
        ell(c, new V3(0, 0.285, 0.0), new V3(0.145, 0.14, 0.21), 0, 14, 9, (n, p) -> mixc(CREAM, ORANGE_L, ss(0.2, 0.9, n.y) * 0.3));

        // púas del lomo
        double[] sz = {0.16, 0.07, -0.03, -0.13, -0.24, -0.35};
        double[] sh = {0.115, 0.155, 0.155, 0.142, 0.122, 0.098};
        for (int i = 0; i < sz.length; i++) {
            double z = sz[i];
            double e = (z + 0.06) / 0.26;
            double y = e > 1 ? 0.30 : 0.365 + 0.19 * Math.sqrt(Math.max(0, 1 - e * e));
            if (z < -0.30) {
                y = 0.31 - (z + 0.30) * 0.0 + 0.0;
            }
            if (i == 5) {
                y = 0.285;
            }
            V3 b = new V3(0, y - 0.012, z);
            tube(c, new V3[]{b, b.add(0, sh[i] * 0.55, -0.012), b.add(0, sh[i], -0.040)}, new double[]{0.046, 0.028, 0.0}, 4, RED_P);
        }

        // ---------------- cuello y cabeza (la cabeza gira un poco sola)
        ell(c, new V3(0, 0.47, 0.17), new V3(0.118, 0.13, 0.14), 0, 12, 8, ORANGE_P);
        Creature3D.headLook(c, 0.16, 0.0);

        V3 hc = new V3(0, 0.56, 0.33), hr = new V3(0.178, 0.158, 0.165);
        ell(c, hc, hr, 0, 16, 11, HEAD_P);
        V3 sc = new V3(0, 0.51, 0.468), sr = new V3(0.092, 0.070, 0.098);
        ell(c, sc, sr, 0, 12, 8, SNOUT_P);
        // fosas nasales
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.032, 0.525, 0.557), new V3(0.013, 0.010, 0.008), 0, 6, 4, DARK_P);
        }
        // boca (sonrisa)
        Creature3D.decal(c, sc, sr, new V3(0, -0.45, 1), 0.11, 0, Creature3D.T_MOUTH, new float[]{0.55F, 0.25F, 0.12F}, 1, 0.003);
        // mejillas
        for (int sd = -1; sd <= 1; sd += 2) {
            Creature3D.decal(c, hc, hr, new V3(sd * 0.95, -0.28, 0.55), 0.105, 0, Creature3D.T_BLUSH, new float[]{1F, 0.62F, 0.55F}, 0.85, 0.002);
        }
        // ojos grandes
        boolean shut = ((t + ph * 7) % 86) < 4;
        for (int sd = -1; sd <= 1; sd += 2) {
            Creature3D.decal(c, hc, hr, new V3(sd * 0.76, 0.14, 0.66), 0.125, sd * 0.10, shut ? Creature3D.T_EYE_SHUT : Creature3D.T_EYE, shut ? new float[]{0.55F, 0.25F, 0.12F} : new float[]{1F, 0.72F, 0.22F}, 1, 0.004);
        }
        // cuernos blancos curvados hacia atrás
        for (int sd = -1; sd <= 1; sd += 2) {
            V3[] hcp = {new V3(sd * 0.065, 0.665, 0.285), new V3(sd * 0.078, 0.745, 0.250), new V3(sd * 0.095, 0.820, 0.185), new V3(sd * 0.110, 0.870, 0.100)};
            double[] hrr = {0.034, 0.026, 0.016, 0.002};
            double[][] rr = new double[1][];
            V3[] sp = spline(hcp, hrr, 3, rr);
            tube(c, sp, rr[0], 6, WHITE_P);
            // aletas a los lados de la cabeza
            V3 fb = new V3(sd * 0.150, 0.600, 0.250);
            tube(c, new V3[]{fb, fb.add(sd * 0.050, 0.020, -0.040), fb.add(sd * 0.105, 0.040, -0.090)}, new double[]{0.032, 0.022, 0.0}, 4, RED_P);
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ alas

    private static V3 wp(int sd, double u, double w, double ang, V3 S) {
        return new V3(S.x + sd * u * Math.cos(ang), S.y + u * Math.sin(ang), S.z + w);
    }

    private static void wing(Ctx c, int sd, double ang, double flD, double t, double ph) {
        V3 S = new V3(sd * 0.115, 0.46, -0.03);
        double lag = 0.30 * flD;
        // puntos en el plano del ala (u hacia afuera, w hacia adelante) y cuánto se doblan al aletear
        double[][] pts = {
                {0.25, 0.10, 0.35},    // 0 muñeca
                {0.63, 0.20, 1.00},    // 1 dedo delantero
                {0.63, -0.02, 1.00},   // 2
                {0.53, -0.20, 0.90},   // 3
                {0.39, -0.33, 0.70},   // 4
                {0.05, -0.30, 0.10},   // 5 raíz (parte trasera)
        };
        V3[] P = new V3[pts.length];
        for (int i = 0; i < pts.length; i++) {
            double a = ang - lag * pts[i][2];
            P[i] = wp(sd, pts[i][0], pts[i][1], a, S);
        }
        V3 W = P[0];
        // membranas: cada hoja es un cuadrilátero (muñeca, punta A, borde festoneado, punta B)
        float[] cIn = {1.0F, 0.66F, 0.26F}, cOut = {0.97F, 0.36F, 0.15F}, cMid = {1.0F, 0.52F, 0.20F};
        int[][] pan = {{1, 2}, {2, 3}, {3, 4}, {4, 5}};
        for (int[] pr : pan) {
            V3 a = P[pr[0]], b = P[pr[1]];
            V3 mid = a.add(b).mul(0.5).lerp(W, 0.26);
            V3 nrm = a.sub(W).cross(b.sub(W)).norm();
            V3[] p = {W, a, mid, b};
            V3[] n = {nrm, nrm, nrm, nrm};
            float[][] col = {cIn, cOut, cMid, cOut};
            c.quad(Sink.SOLID, 1, Creature3D.T_WHITE, p, n, col, null);
        }
        // pliegue junto al cuerpo
        {
            V3 nrm = W.sub(S).cross(P[5].sub(S)).norm();
            c.quad(Sink.SOLID, 1, Creature3D.T_WHITE, new V3[]{S, W, P[5], S.add(0, -0.02, -0.04)}, new V3[]{nrm, nrm, nrm, nrm},
                    new float[][]{{1.0F, 0.72F, 0.34F}, cIn, {1.0F, 0.66F, 0.28F}, {1.0F, 0.72F, 0.34F}}, null);
        }
        // huesos
        tube(c, new V3[]{S, S.lerp(W, 0.5).add(0, 0.012, 0.02), W}, new double[]{0.030, 0.024, 0.020}, 5, BONE_P);
        for (int i = 1; i <= 4; i++) {
            V3 mid = W.lerp(P[i], 0.5);
            tube(c, new V3[]{W, mid, P[i]}, new double[]{0.015, 0.011, 0.004}, 4, BONE_P);
        }
        // garra del ala
        tube(c, new V3[]{W, W.add(0, 0.020, 0.045)}, new double[]{0.018, 0.0}, 4, WHITE_P);
    }
}
