package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Objetos 3D de los efectos que antes eran dibujos planos: el Árbol Mundo (tronco, raíces, ramas y copa), el estandarte,
 * la moneda y los colmillos. Se arman con las mismas piezas que las criaturas (esferas achatadas y tubos).
 */
final class Props3D {
    private Props3D() {
    }

    private static final float[] BARK = {0.45F, 0.30F, 0.19F}, BARK2 = {0.62F, 0.44F, 0.28F}, BARK_D = {0.30F, 0.19F, 0.12F};
    private static final float[][] LEAF = {{0.26F, 0.72F, 0.34F}, {0.34F, 0.80F, 0.38F}, {0.46F, 0.88F, 0.42F}, {0.22F, 0.64F, 0.40F}, {0.58F, 0.92F, 0.50F}};
    private static final float[] BLOSSOM = {1F, 0.74F, 0.86F}, BLOSSOM2 = {1F, 0.95F, 0.97F}, FRUIT = {1F, 0.90F, 0.45F};

    private static double h(long k) {
        return VfxGeom.hash(k);
    }

    static Ctx world(Sink s, Cam cam, V3 pos, double scale, double yaw) {
        Ctx c = new Ctx(s, cam, pos, scale);
        System.arraycopy(rot(yaw, 0, 0), 0, c.m, 0, 9);
        c.pivot = new V3(0, 0, 0);
        return c;
    }

    // ------------------------------------------------------------------ árbol mundo

    static void tree(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        double growT = d.a > 0 ? d.a : 40;
        double prog = VfxGeom.clamp01(age / growT);
        double fade = life > 0 ? ss(0, 22, life - age) : 1;
        double scale = d.size * (0.25 + 0.75 * fade);
        if (fade < 0.02) {
            return;
        }
        long seed = d.id * 131L;
        double yaw = h(seed) * Math.PI * 2;
        Ctx c = world(s, cam, pos, scale, yaw);
        double dist = cam.pos.sub(pos).len();
        if (dist > 160) {
            return;
        }
        c.lod = dist < 30 ? 1.0 : (dist < 60 ? 0.75 : 0.55);
        float[] tint = rgb(d.rgb);
        double rk = ss(0.0, 0.30, prog), tk = ss(0.05, 0.60, prog), bk = ss(0.40, 0.78, prog);
        double ck = VfxGeom.easeOutBack(ss(0.42, 1.0, prog));
        double wind = Math.sin(age * 0.045 + d.id) * 0.008;

        // raíces que se agarran al suelo
        for (int i = 0; i < 7; i++) {
            double a = Math.PI * 2 * i / 7 + (h(seed + i) - 0.5) * 0.5;
            double len = (0.21 + 0.10 * h(seed + 10 + i)) * rk;
            double ca = Math.cos(a), sa = Math.sin(a);
            V3[] cp = {new V3(ca * 0.03, 0.10, sa * 0.03), new V3(ca * len * 0.35, 0.055, sa * len * 0.35), new V3(ca * len * 0.7, 0.018, sa * len * 0.7), new V3(ca * len, -0.012, sa * len)};
            curve(c, cp, new double[]{0.040 * (0.4 + rk * 0.6), 0.028, 0.016, 0.004}, 3, 6, (n, p) -> mix(BARK, BARK_D, ss(0.10, 0.0, p.y) * 0.5));
        }
        // tronco
        double th = tk;
        V3[] tp = {new V3(0, 0, 0), new V3(0.012, 0.13 * th, 0.008), new V3(-0.010, 0.28 * th, -0.004), new V3(0.006 + wind * 0.4, 0.40 * th, -0.008), new V3(wind * 0.6, 0.50 * th, 0)};
        curve(c, tp, new double[]{0.078, 0.055, 0.044, 0.038, 0.034}, 4, 12, (n, p) -> {
            double ang = Math.atan2(n.z, n.x);
            double st = 0.5 + 0.5 * Math.sin(ang * 7 + p.y * 5 + 1.3 * Math.sin(ang * 3));
            return scale(mix(BARK, BARK2, st * 0.7), 0.82 + 0.25 * st);
        });
        ell(c, new V3(0, 0.012, 0), new V3(0.095, 0.04, 0.095), 12, 6, (n, p) -> mix(BARK, BARK_D, 0.3));
        // ramas
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + 0.4 + (h(seed + 30 + i) - 0.5) * 0.4;
            double y0 = (0.30 + 0.05 * (i % 3)) * th;
            double reach = (0.20 + 0.06 * h(seed + 40 + i)) * bk;
            double ca = Math.cos(a), sa = Math.sin(a);
            V3 b = new V3(ca * 0.02, y0, sa * 0.02), m = new V3(ca * reach * 0.55, y0 + 0.05 * bk, sa * reach * 0.55), e = new V3(ca * reach, y0 + (0.12 + 0.03 * (i % 2)) * bk, sa * reach);
            curve(c, new V3[]{b, m, e}, new double[]{0.026, 0.017, 0.006}, 3, 6, (n, p) -> mix(BARK, BARK2, 0.4));
        }
        // copa: capas de masas de hojas (algunas con flor de cerezo)
        V3 cc = new V3(0, 0.60, 0);
        int[] layerN = {8, 6, 4, 1};
        double[] layerY = {0.545, 0.665, 0.775, 0.855};
        double[] layerR = {0.27, 0.19, 0.10, 0.0};
        double[] blobR = {0.150, 0.150, 0.130, 0.120};
        int bi = 0;
        for (int L = 0; L < 4; L++) {
            for (int i = 0; i < layerN[L]; i++, bi++) {
                double a = Math.PI * 2 * i / layerN[L] + L * 0.7 + (h(seed + 60 + bi) - 0.5) * 0.4;
                double rr = layerR[L] * (0.9 + 0.2 * h(seed + 80 + bi));
                V3 raw = new V3(Math.cos(a) * rr, layerY[L] + (h(seed + 100 + bi) - 0.5) * 0.04, Math.sin(a) * rr);
                double sw = Math.sin(age * 0.05 + bi * 0.9 + d.id) * 0.006;
                V3 ce = cc.add(raw.sub(cc).mul(ck)).add(sw, 0, sw * 0.7);
                double r = blobR[L] * (0.85 + 0.3 * h(seed + 120 + bi)) * ck;
                if (r < 0.004) {
                    continue;
                }
                float[] base = LEAF[(int) (h(seed + 140 + bi) * LEAF.length) % LEAF.length];
                boolean pink = h(seed + 160 + bi) < 0.28;
                float[] b2 = mix(base, tint, 0.18);
                ell(c, ce, new V3(r, r * 0.78, r), 10, 7, (n, p) -> {
                    float[] col = mix(scale(b2, 0.82), b2, ss(-0.6, 0.8, n.y));
                    col = mix(col, new float[]{0.80F, 1F, 0.62F}, ss(0.55, 1.0, n.y) * 0.35);
                    if (pink) {
                        double sp = 0.5 + 0.5 * Math.sin(p.x * 70) * Math.sin(p.z * 66 + 1);
                        col = mix(col, BLOSSOM, ss(0.35, 0.95, sp) * 0.9);
                    }
                    return col;
                });
            }
        }
        // montoncitos de hojas más pequeños para que la copa no se vea lisa
        if (ck > 0.2) {
            for (int i = 0; i < 20; i++) {
                double a = h(seed + 500 + i) * Math.PI * 2, e = h(seed + 550 + i);
                double ph = Math.acos(1 - 1.0 * e);
                V3 dir = new V3(Math.sin(ph) * Math.cos(a), Math.cos(ph), Math.sin(ph) * Math.sin(a));
                V3 ps = cc.add(new V3(dir.x * 0.37, dir.y * 0.33 - 0.01, dir.z * 0.37).mul(ck));
                double r = (0.07 + 0.05 * h(seed + 600 + i)) * ck;
                float[] base = mix(LEAF[(int) (h(seed + 650 + i) * LEAF.length) % LEAF.length], tint, 0.15);
                ell(c, ps, new V3(r, r * 0.8, r), 7, 5, (n, p) -> mix(scale(base, 0.85), base, ss(-0.5, 0.8, n.y)));
            }
        }
        // florecitas sueltas sobre la copa
        if (ck > 0.3) {
            for (int i = 0; i < 32; i++) {
                double a = h(seed + 200 + i) * Math.PI * 2, e = h(seed + 250 + i);
                double ph = Math.acos(1 - 1.1 * e);
                V3 dir = new V3(Math.sin(ph) * Math.cos(a), Math.cos(ph), Math.sin(ph) * Math.sin(a));
                V3 ps = cc.add(new V3(dir.x * 0.34, dir.y * 0.31, dir.z * 0.34).mul(ck)).add(0, 0.02, 0);
                float[] col = h(seed + 300 + i) < 0.5 ? BLOSSOM : BLOSSOM2;
                ell(c, ps, new V3(0.016, 0.012, 0.016), 5, 3, (n, p) -> col);
            }
        }
        // frutos de luz: brillan y se mecen
        c.emissive = true;
        for (int i = 0; i < 9; i++) {
            double a = h(seed + 400 + i) * Math.PI * 2, e = 0.25 + 0.7 * h(seed + 450 + i);
            double ph = Math.acos(1 - e);
            V3 dir = new V3(Math.sin(ph) * Math.cos(a), Math.cos(ph) * 0.9, Math.sin(ph) * Math.sin(a));
            V3 ps = cc.add(new V3(dir.x * 0.30, dir.y * 0.28 - 0.04, dir.z * 0.30).mul(ck));
            double tw = 0.6 + 0.4 * Math.sin(age * 0.18 + i * 1.7);
            ell(c, ps, new V3(0.016, 0.018, 0.016), 6, 4, (n, p) -> FRUIT);
            glow(c, ps, 0.10 + 0.04 * tw, mix(FRUIT, tint, 0.3), 0.55 * tw * ck);
        }
        glow(c, cc.add(0, 0.05, 0), 0.9 * ck, mix(tint, WHITE, 0.3), 0.10 * fade);
        c.emissive = false;
    }

    // ------------------------------------------------------------------ estandarte

    private static final float[] CLOTH = {0.80F, 0.14F, 0.20F}, CLOTH_D = {0.58F, 0.08F, 0.14F}, GOLD = {1F, 0.80F, 0.28F}, GOLD_D = {0.80F, 0.58F, 0.14F};

    /** Estrella de cinco puntas (radio exterior R, interior 0.42 R) centrada en el origen, con una punta hacia arriba. */
    private static boolean inStar(double x, double y, double R) {
        double r = Math.hypot(x, y);
        if (r > R) {
            return false;
        }
        double ri = R * 0.42;
        double ang = Math.atan2(x, y);                      // 0 = punta de arriba
        double sector = Math.PI * 2 / 5;
        double a = ((ang % sector) + sector) % sector;       // 0..72 grados desde una punta
        if (a > sector / 2) {
            a = sector - a;                                   // simetría: 0..36
        }
        // recta entre la punta (R, 0) y el vértice interior (ri, 36 grados) en coordenadas polares
        double tx = R, ty = 0, ix = ri * Math.cos(sector / 2), iy = ri * Math.sin(sector / 2);
        double dx = Math.cos(a), dy = Math.sin(a);
        double ex = ix - tx, ey = iy - ty;
        double den = dx * ey - dy * ex;
        double t = Math.abs(den) < 1.0E-9 ? R : (tx * ey - ty * ex) / den;
        return r <= t;
    }

    static void banner(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        double growT = d.a > 0 ? d.a : 16;
        double sc = VfxGeom.easeOutBack(age / growT);
        double fade = life > 0 ? ss(0, 18, life - age) : 1;
        if (fade < 0.02 || sc < 0.01) {
            return;
        }
        double yaw = (Math.abs(d.dx) + Math.abs(d.dz) > 1.0E-4) ? Math.atan2(d.dx, d.dz) : 0;
        Ctx c = world(s, cam, pos, d.size * sc * (0.3 + 0.7 * fade), yaw);
        // base de piedra y asta
        ell(c, new V3(0, 0.012, 0), new V3(0.085, 0.032, 0.085), 12, 6, (n, p) -> mix(new float[]{0.52F, 0.50F, 0.58F}, new float[]{0.34F, 0.33F, 0.40F}, ss(0.2, -0.6, n.y)));
        tube(c, new V3[]{new V3(0, 0.0, 0), new V3(0, 0.5, 0), new V3(0, 1.0, 0)}, new double[]{0.016, 0.013, 0.011}, 8, (n, p) -> mix(GOLD_D, GOLD, 0.35 + 0.35 * n.x));
        ell(c, new V3(0, 1.015, 0), new V3(0.030, 0.030, 0.030), 10, 7, (n, p) -> mix(GOLD, WHITE, ss(0.2, 1.0, n.y) * 0.5));
        tube(c, new V3[]{new V3(0, 1.03, 0), new V3(0, 1.075, 0), new V3(0, 1.12, 0)}, new double[]{0.018, 0.012, 0.0}, 6, (n, p) -> GOLD);
        // barra horizontal
        double barY = 0.93;
        tube(c, new V3[]{new V3(-0.30, barY, 0), new V3(0, barY, 0), new V3(0.30, barY, 0)}, new double[]{0.009, 0.010, 0.009}, 6, (n, p) -> GOLD);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.305, barY, 0), new V3(0.020, 0.020, 0.020), 8, 6, (n, p) -> mix(GOLD, WHITE, ss(0.2, 1, n.y) * 0.4));
        }
        // tela: malla con ondas (más suelta abajo y lejos del asta), borde dorado y estrella en el centro
        int cols = 24, rows = 32;
        double W = 0.54, top = barY - 0.012, bot = 0.20;
        V3[][] P = new V3[rows + 1][cols + 1];
        float[][][] C = new float[rows + 1][cols + 1][];
        for (int i = 0; i <= rows; i++) {
            double fv = i / (double) rows;
            for (int j = 0; j <= cols; j++) {
                double fu = j / (double) cols;
                double x = (fu - 0.5) * W;
                double y = top - (top - bot) * fv;
                // forma de cola de golondrina
                double notch = fv > 0.78 ? (fv - 0.78) / 0.22 : 0;
                y += notch * 0.12 * (1 - Math.abs(fu - 0.5) * 2) * 1.0;
                double wave = Math.sin(age * 0.17 + fu * 4.2 + fv * 3.0 + d.id) * 0.028 * fv + Math.sin(age * 0.09 + fv * 5.5 + fu * 2) * 0.012 * fv;
                P[i][j] = new V3(x, y, 0.018 + wave + 0.01 * Math.sin(age * 0.2 + fu * 7) * fv);
                double sx = (fu - 0.5) * W, sy = y - 0.57;
                boolean border = fu < 0.07 || fu > 0.93 || fv < 0.05 || (fv > 0.86 && fv < 0.93 * 0 + 0.0);
                float[] col = mix(CLOTH, CLOTH_D, ss(0.0, 1.0, fv) * 0.5);
                if (fu < 0.075 || fu > 0.925 || fv < 0.045) {
                    col = GOLD;
                }
                if (inStar(sx, sy, 0.19)) {
                    col = mix(GOLD, WHITE, 0.15);
                }
                C[i][j] = col;
            }
        }
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double fvm = (i + 0.5) / rows, fum = (j + 0.5) / cols;
                // recorte de la cola
                double notch = fvm > 0.78 ? (fvm - 0.78) / 0.22 : 0;
                if (notch > 0 && Math.abs(fum - 0.5) * 2 < notch * 0.0) {
                    continue;
                }
                V3[] p = {P[i][j], P[i][j + 1], P[i + 1][j + 1], P[i + 1][j]};
                V3 nrm = p[1].sub(p[0]).cross(p[3].sub(p[0])).norm();
                float[] col = C[i][j];
                for (int side = 0; side < 2; side++) {
                    V3 nn = side == 0 ? nrm : nrm.mul(-1);
                    V3[] pp = side == 0 ? p : new V3[]{p[0], p[3], p[2], p[1]};
                    c.quad(Sink.SOLID, 1, T_WHITE, pp, new V3[]{nn, nn, nn, nn}, new float[][]{col, col, col, col}, null);
                }
            }
        }
        // borlas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = P[rows][sd < 0 ? 0 : cols];
            tube(c, new V3[]{b, b.add(0, -0.025, 0), b.add(0, -0.05, 0)}, new double[]{0.008, 0.012, 0.004}, 5, (n, p) -> GOLD);
        }
        c.emissive = true;
        glow(c, new V3(0, 0.57, 0.02), 0.28, new float[]{1F, 0.85F, 0.4F}, 0.22 * fade);
        c.emissive = false;
    }

    // ------------------------------------------------------------------ moneda

    static void coin(VfxData d, double age, int life, V3 pos, Cam cam, Sink s) {
        float[] c0 = rgb(d.rgb);
        double flightT = VfxGeom.clamp01(age / (life * 0.62));
        double rest = VfxGeom.clamp01((age - life * 0.62) / (life * 0.38));
        double peak = d.a > 0 ? d.a : 1.8;
        double hgt = peak * Math.pow(Math.sin(Math.PI * flightT), 0.85);
        int face = d.b >= 0.5 ? 1 : 0;
        int halfTurns = 10 + face;
        double ang = Math.PI * halfTurns * (1 - Math.pow(1 - flightT, 2.2));
        double fade = life > 0 ? ss(0, 8, life - age) : 1;
        if (fade < 0.02) {
            return;
        }
        double R = d.size * 1.15 * (0.5 + 0.5 * fade);
        V3 ce = pos.add(0, hgt + 0.3, 0);
        // eje del giro: horizontal, de izquierda a derecha de la cámara
        V3 ax = new V3(cam.right.x, 0, cam.right.z);
        ax = ax.len() < 1.0E-6 ? new V3(1, 0, 0) : ax.norm();
        V3 nrm0 = new V3(cam.fwd.x, 0, cam.fwd.z);
        nrm0 = nrm0.len() < 1.0E-6 ? new V3(0, 0, 1) : nrm0.norm().mul(-1);    // la cara mira hacia la cámara
        V3 up0 = V3.UP;
        // giro de la moneda alrededor del eje ax
        V3 nrm = nrm0.mul(Math.cos(ang)).add(up0.mul(Math.sin(ang)));
        V3 upv = up0.mul(Math.cos(ang)).sub(nrm0.mul(Math.sin(ang)));
        double th = R * 0.16;
        int n = 24;
        V3 light = new V3(-0.4, 0.8, 0.45).norm();
        for (int side = 0; side < 2; side++) {
            V3 nn = side == 0 ? nrm : nrm.mul(-1);
            double sgn = side == 0 ? 1 : -1;
            V3 cen = ce.add(nn.mul(th));
            int fr = side;   // cara 0 = cara, cara 1 = cruz (la elegida queda hacia la cámara al terminar)
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                V3 r0 = ax.mul(Math.cos(a0) * R).add(upv.mul(Math.sin(a0) * R)), r1 = ax.mul(Math.cos(a1) * R).add(upv.mul(Math.sin(a1) * R));
                V3[] p = side == 0 ? new V3[]{cen, cen.add(r0), cen.add(r1), cen.add(r1)} : new V3[]{cen, cen.add(r1), cen.add(r0), cen.add(r0)};
                double lam = Math.max(0, nn.dot(light));
                float k = (float) (0.70 + 0.40 * lam);
                float[] col = {Math.min(1, c0[0] * k), Math.min(1, c0[1] * k), Math.min(1, c0[2] * k), (float) 1};
                double[] pp = new double[12];
                for (int q = 0; q < 4; q++) {
                    pp[q * 3] = p[q].x;
                    pp[q * 3 + 1] = p[q].y;
                    pp[q * 3 + 2] = p[q].z;
                }
                // uv: círculo dentro de la imagen del cuadro de la moneda
                float v0 = fr / 2F, vs = 0.5F;
                float[] uv = new float[8];
                double[][] pts = {{0, 0}, {Math.cos(a0), Math.sin(a0)}, {Math.cos(a1), Math.sin(a1)}, {Math.cos(a1), Math.sin(a1)}};
                for (int q = 0; q < 4; q++) {
                    uv[q * 2] = (float) (0.5 + 0.5 * pts[q][0] * sgn);
                    uv[q * 2 + 1] = (float) (v0 + vs * (0.5 - 0.5 * pts[q][1]));
                }
                float[] cols = new float[16];
                for (int q = 0; q < 4; q++) {
                    System.arraycopy(col, 0, cols, q * 4, 4);
                }
                s.quad(VfxTex.COIN, Sink.SOLID, pp, uv, cols);
            }
        }
        // canto dorado (anillo de cuadriláteros)
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
            V3 r0 = ax.mul(Math.cos(a0) * R).add(upv.mul(Math.sin(a0) * R)), r1 = ax.mul(Math.cos(a1) * R).add(upv.mul(Math.sin(a1) * R));
            V3 nn = r0.add(r1).norm();
            double lam = Math.max(0, nn.dot(light));
            float k = (float) (0.55 + 0.5 * lam);
            float[] rim = {Math.min(1, c0[0] * k * 0.85F), Math.min(1, c0[1] * k * 0.8F), Math.min(1, c0[2] * k * 0.6F), 1F};
            V3 a = ce.add(r0).add(nrm.mul(th)), b = ce.add(r1).add(nrm.mul(th)), c = ce.add(r1).sub(nrm.mul(th)), dd = ce.add(r0).sub(nrm.mul(th));
            double[] pp = {a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, dd.x, dd.y, dd.z};
            float[] cols = new float[16];
            for (int q = 0; q < 4; q++) {
                System.arraycopy(rim, 0, cols, q * 4, 4);
            }
            s.quad(VfxTex.SOFT, Sink.SOLID, pp, new float[]{0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F, 0.5F}, cols);
        }
        // brillos (igual que antes)
        double glint = 0.35 + 0.25 * Math.sin(age * 0.8) + (flightT >= 1 ? 0.5 * (1 - rest) : 0);
        Ctx c = world(s, cam, V3.ZERO, 1, 0);
        c.emissive = true;
        glow(c, ce, R * 5.0, c0, 0.30 * fade);
        glow(c, ce.add(cam.right.mul(R * 0.5)).add(cam.up.mul(R * 0.5)), R * 2.0, WHITE, 0.6 * fade * glint);
        if (flightT >= 1) {
            double e = VfxGeom.easeOut(rest * 2.5);
            glow(c, ce, R * (2 + 6 * e), c0, (1 - e) * 0.5 * fade);
        }
    }

    // ------------------------------------------------------------------ colmillo

    /** Un colmillo curvo con su encía. base = punto en el suelo, g = hacia donde crece (unitario), h = altura. */
    static void fang(Ctx c, V3 base, V3 g, V3 tan, double h, float[] bone, double fade) {
        double w = h * 0.17;
        // encía / tierra levantada
        ell(c, base.add(0, 0.02, 0), new V3(w * 1.5, w * 0.55, w * 1.5), 10, 5, (n, p) -> mix(new float[]{0.32F, 0.24F, 0.30F}, bone, 0.15 + 0.3 * ss(0.2, 1, n.y)));
        V3 side = g.cross(tan).norm();
        V3 curve = tan.mul(0.0).add(side.mul(0.0));
        V3[] cp = {base, base.add(g.mul(h * 0.35)).add(side.mul(h * 0.015)), base.add(g.mul(h * 0.7)).add(side.mul(h * 0.04)), base.add(g.mul(h)).add(side.mul(h * 0.075))};
        curve(c, cp, new double[]{w * 0.95, w * 0.78, w * 0.46, w * 0.02}, 3, 7, (n, p) -> {
            double t = ss(0, h, p.sub(base).len());
            return mix(mix(bone, new float[]{0.62F, 0.58F, 0.60F}, 0.35 * (1 - t)), WHITE, 0.25 * t);
        });
    }
}
