package com.mundosdeaxelis.vfx;

import static com.mundosdeaxelis.vfx.Creature3D.*;

/**
 * Herramientas de detalle para las criaturas (rework de mascotas): mechones de pelo, plumas, escamas, manchas planas y destellos.
 * Todo son cuadriláteros sueltos (1 cara cada uno) para que el detalle sea barato; la cantidad baja sola con la distancia.
 */
final class Detail {
    private Detail() {
    }

    // celdas nuevas de creature3d.png (zona libre abajo en el centro): disco duro, disco suave, destello de 4 puntas, hoja
    static final float[] T_DISC = {0.5F, 0.75F, 0.625F, 0.875F};
    static final float[] T_SOFT = {0.625F, 0.75F, 0.75F, 0.875F};
    static final float[] T_STAR = {0.5F, 0.875F, 0.625F, 1F};
    static final float[] T_LEAF = {0.625F, 0.875F, 0.75F, 1F};

    /** Qué parte de la superficie se cubre: devuelve un peso de 0 a 1 para una dirección unitaria. */
    interface Region {
        double w(V3 d);
    }

    static final Region ALL = d -> 1;
    static final Region TOP = d -> ss(-0.1, 0.5, d.y);
    static final Region BOTTOM = d -> ss(0.1, -0.5, d.y);
    static final Region BACK = d -> ss(0.0, -0.6, d.z);
    static final Region FRONT = d -> ss(0.0, 0.6, d.z);
    static final Region SIDES = d -> ss(0.3, 0.8, Math.abs(d.x));

    static Region and(Region a, Region b) {
        return d -> a.w(d) * b.w(d);
    }

    /** Número pseudoaleatorio estable (0..1) a partir de una semilla y un índice. */
    static double rnd(long seed, int i, int k) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL + k * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /** Dirección uniforme sobre la esfera. */
    static V3 sph(long seed, int i) {
        double y = rnd(seed, i, 1) * 2 - 1;
        double a = rnd(seed, i, 2) * Math.PI * 2;
        double r = Math.sqrt(Math.max(0, 1 - y * y));
        return new V3(Math.cos(a) * r, y, Math.sin(a) * r);
    }

    // ------------------------------------------------------------------ cuadriláteros sueltos

    /** Cometa (pluma, hoja, mechón): base, dos puntos laterales a ~38% y la punta. */
    static void kite(Ctx c, V3 base, V3 tip, V3 side, double w, V3 nrm, float[] c0, float[] c1) {
        V3 mid = base.lerp(tip, 0.38);
        V3[] p = {base, mid.add(side.mul(w)), tip, mid.sub(side.mul(w))};
        V3[] n = {nrm, nrm, nrm, nrm};
        float[] cm = mix(c0, c1, 0.5);
        float[][] b = {c0, cm, c1, cm};
        c.quad(c.blend, c.alpha, T_WHITE, p, n, b, null);
    }

    /** Pluma / hoja alargada con dos tramos (más natural que la cometa): 2 caras. */
    static void feather(Ctx c, V3 base, V3 tip, V3 side, double w, V3 nrm, float[] c0, float[] c1) {
        V3 m1 = base.lerp(tip, 0.45).add(nrm.mul(0.0));
        V3[] p = {base.sub(side.mul(w * 0.35)), base.add(side.mul(w * 0.35)), m1.add(side.mul(w)), m1.sub(side.mul(w))};
        V3[] n = {nrm, nrm, nrm, nrm};
        float[] cm = mix(c0, c1, 0.5);
        c.quad(c.blend, c.alpha, T_WHITE, p, n, new float[][]{c0, c0, cm, cm}, null);
        V3[] q = {m1.sub(side.mul(w)), m1.add(side.mul(w)), tip, tip};
        c.quad(c.blend, c.alpha, T_WHITE, q, n, new float[][]{cm, cm, c1, c1}, null);
    }

    /** Rombo tumbado sobre la superficie (escama). */
    static void plate(Ctx c, V3 at, V3 nrm, V3 up, double size, float[] c0, float[] c1) {
        V3 right = nrm.cross(up).norm();
        V3 u = nrm.cross(right).norm();
        V3 ctr = at.add(nrm.mul(size * 0.12));
        V3[] p = {ctr.sub(u.mul(size)), ctr.add(right.mul(size * 0.8)).add(u.mul(size * 0.1)), ctr.add(u.mul(size * 0.9)), ctr.sub(right.mul(size * 0.8)).add(u.mul(size * 0.1))};
        V3[] n = {nrm, nrm, nrm, nrm};
        c.quad(c.blend, c.alpha, T_WHITE, p, n, new float[][]{c0, c1, c1, c1}, null);
    }

    // ------------------------------------------------------------------ cobertura

    /**
     * Pelaje o plumaje: reparte mechones por la superficie de un elipsoide. comb = hacia dónde se peinan (p. ej. hacia atrás),
     * combAmt cuánto se tumban. tip = color de las puntas (null = el mismo, más claro).
     */
    static void fur(Ctx c, V3 ce, V3 r, int count, long seed, double len, double w, V3 comb, double combAmt, Region region, Paint paint, float[] tip) {
        int n = (int) Math.round(count * Math.max(0.35, c.lod));
        for (int i = 0; i < count && n > 0; i++) {
            V3 d = sph(seed, i);
            if (rnd(seed, i, 3) > region.w(d)) {
                continue;
            }
            n--;
            V3 p = surf(ce, r, d);
            V3 nr = new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
            V3 dir = nr.mul(0.55).add(comb.mul(combAmt)).norm();
            double L = len * (0.65 + 0.7 * rnd(seed, i, 4));
            V3 tp = p.add(dir.mul(L)).add(nr.mul(-len * 0.1));
            V3 sd = nr.cross(comb.len() > 0.01 ? comb : V3.UP);
            sd = sd.len() < 0.05 ? nr.anyPerp() : sd.norm();
            sd = sd.rot(nr, (rnd(seed, i, 5) - 0.5) * 1.2);
            float[] pc = paint.at(nr, p.sub(ce));
            float[] c0 = scale(pc, 0.92), c1 = tip != null ? mix(pc, tip, 0.8) : scale(pc, 1.12 + 0.1 * rnd(seed, i, 6));
            kite(c, p.add(nr.mul(-0.004)), tp, sd, w * (0.7 + 0.6 * rnd(seed, i, 7)), nr, c0, c1);
        }
    }

    /** Plumas en filas sobre un elipsoide (pájaros): filas de arriba hacia abajo, cada pluma apunta hacia abajo/atrás. */
    static void plumage(Ctx c, V3 ce, V3 r, int rows, int perRow, double len, double w, Region region, Paint paint, float[] tip) {
        for (int i = 0; i < rows; i++) {
            double yy = 0.75 - i * (1.6 / Math.max(1, rows - 1));
            double rr = Math.sqrt(Math.max(0.02, 1 - yy * yy));
            for (int k = 0; k < perRow; k++) {
                double a = (k + (i % 2) * 0.5) * Math.PI * 2 / perRow;
                V3 d = new V3(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                if (region.w(d) < 0.5) {
                    continue;
                }
                V3 p = surf(ce, r, d);
                V3 nr = new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
                V3 down = new V3(0, -1, 0);
                V3 dir = nr.mul(0.55).add(down.sub(nr.mul(down.dot(nr))).norm().mul(0.85)).norm();
                V3 sd = nr.cross(down);
                sd = sd.len() < 0.05 ? nr.anyPerp() : sd.norm();
                float[] pc = paint.at(nr, p.sub(ce));
                feather(c, p, p.add(dir.mul(len)), sd, w, nr, scale(pc, 0.9), tip != null ? mix(pc, tip, 0.7) : scale(pc, 1.1));
            }
        }
    }

    /** Escamas en cuadrícula sobre un elipsoide. */
    static void scales(Ctx c, V3 ce, V3 r, int rows, int perRow, double size, Region region, Paint paint, float[] edge) {
        for (int i = 0; i < rows; i++) {
            double yy = 0.85 - i * (1.7 / Math.max(1, rows - 1));
            double rr = Math.sqrt(Math.max(0.02, 1 - yy * yy));
            for (int k = 0; k < perRow; k++) {
                double a = (k + (i % 2) * 0.5) * Math.PI * 2 / perRow;
                V3 d = new V3(Math.cos(a) * rr, yy, Math.sin(a) * rr);
                if (region.w(d) < 0.5) {
                    continue;
                }
                V3 p = surf(ce, r, d);
                V3 nr = new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
                float[] pc = paint.at(nr, p.sub(ce));
                plate(c, p, nr, V3.UP, size, scale(pc, 0.88), edge != null ? mix(pc, edge, 0.5) : scale(pc, 1.1));
            }
        }
    }


    /** Casquete de elipsoide (de la tapa hasta el ángulo thMax) con borde en zigzag: cascarones, sombreros, cúpulas rotas. */
    static void cap(Ctx c, V3 ce, V3 r, double yaw, double pitch, double roll, double thMax, double zig, int nu, int nv, Paint paint) {
        nu = Math.max(6, (int) Math.round(nu * c.lod));
        double[] rm = (yaw == 0 && pitch == 0 && roll == 0) ? null : rot(yaw, pitch, roll);
        V3[][] P = new V3[nv + 1][nu + 1], N = new V3[nv + 1][nu + 1];
        for (int i = 0; i <= nv; i++) {
            for (int j = 0; j <= nu; j++) {
                double th = thMax * i / nv;
                if (i == nv) {
                    th += zig * ((j % 2 == 0) ? 1 : -0.3);
                }
                double ph = 2 * Math.PI * j / nu;
                double dx = Math.sin(th) * Math.cos(ph), dy = Math.cos(th), dz = Math.sin(th) * Math.sin(ph);
                V3 p = new V3(dx * r.x, dy * r.y, dz * r.z);
                V3 n = new V3(dx / r.x, dy / r.y, dz / r.z).norm();
                if (rm != null) {
                    p = Ctx.mul(rm, p);
                    n = Ctx.mul(rm, n);
                }
                P[i][j] = ce.add(p);
                N[i][j] = n;
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

    /** Vector rotado con la misma convención que ell(yaw, pitch, roll). */
    static V3 rotv(double yaw, double pitch, double roll, V3 v) {
        return Ctx.mul(rot(yaw, pitch, roll), v);
    }


    /** Pelaje alrededor de un tubo (colas, cuellos): count mechones repartidos por la curva, peinados hacia la punta. */
    static void furTube(Ctx c, V3[] cp, double[] rad, int count, long seed, double len, double w, double along, Paint paint, float[] tip) {
        double[][] rr = new double[1][];
        V3[] sp = spline(cp, rad, 3, rr);
        int n = (int) Math.round(count * Math.max(0.35, c.lod));
        for (int i = 0; i < count && n > 0; i++) {
            n--;
            int j = 1 + (int) (rnd(seed, i, 1) * (sp.length - 2));
            V3 tg = sp[Math.min(sp.length - 1, j + 1)].sub(sp[j - 1]).norm();
            V3 u = tg.anyPerp();
            V3 v = tg.cross(u).norm();
            double a = rnd(seed, i, 2) * Math.PI * 2;
            V3 nr = u.mul(Math.cos(a)).add(v.mul(Math.sin(a))).norm();
            V3 base = sp[j].add(nr.mul(rr[0][j]));
            double L = len * (0.7 + 0.6 * rnd(seed, i, 3)) * (0.7 + 0.3 * (j / (double) sp.length));
            V3 tp = base.add(nr.add(tg.mul(along)).norm().mul(L));
            V3 sd = nr.cross(tg);
            sd = sd.len() < 0.05 ? nr.anyPerp() : sd.norm();
            float[] pc = paint.at(nr, base);
            kite(c, base.add(nr.mul(-0.004)), tp, sd, w * (0.7 + 0.6 * rnd(seed, i, 4)), nr, scale(pc, 0.92), tip != null ? mix(pc, tip, 0.8) : scale(pc, 1.12));
        }
    }


    /** Pulso corto y aleatorio (0..1) para tics: una oreja que se sacude, una antena, la cola. k distingue partes. */
    static double flick(Ctx c, int k) {
        double period = 95 + 31 * (k % 5);
        double u = ((c.t + c.ph * (13 + k * 7) + k * 41) % period) / 9.0;
        return u < 1 ? Math.sin(u * Math.PI) : 0;
    }

    // ------------------------------------------------------------------ manchas y brillos

    /** Mancha redonda plana sobre un elipsoide. */
    static void spot(Ctx c, V3 ce, V3 r, V3 dir, double size, float[] col) {
        decal(c, ce, r, dir, size, 0, T_DISC, col, 1, 0.0025);
    }

    /** Mancha redonda con borde difuminado (rubor, sombras, brillo). */
    static void soft(Ctx c, V3 ce, V3 r, V3 dir, double size, float[] col, double a) {
        decal(c, ce, r, dir, size, 0, T_SOFT, col, a, 0.0025);
    }

    /** Hoja o gota inclinada (marcas alargadas, hojitas, pétalos). */
    static void leaf(Ctx c, V3 ce, V3 r, V3 dir, double size, double roll, float[] col) {
        decal(c, ce, r, dir, size, roll, T_LEAF, col, 1, 0.003);
    }

    /** Destello de cuatro puntas pegado a la superficie (brillo en la nariz, ojos, gemas). */
    static void glint(Ctx c, V3 ce, V3 r, V3 dir, double size, float[] col, double a) {
        boolean em = c.emissive;
        c.emissive = true;
        decal(c, ce, r, dir, size, 0, T_STAR, col, a, 0.004);
        c.emissive = em;
    }

    /** Destello que mira a la cámara y titila (puntos de luz, gemas, ojos mágicos). */
    static void twinkle(Ctx c, V3 at, double size, float[] col, double t, double ph, double speed) {
        double k = 0.55 + 0.45 * Math.sin(t * speed + ph);
        boolean em = c.emissive;
        c.emissive = true;
        glow(c, at, size * (1.4 + 1.6 * k), col, 0.35 + 0.35 * k);
        glow(c, at, size * 0.55, WHITE, 0.8 * k + 0.1);
        c.emissive = em;
    }

    /** Bigote o antena: tubito fino curvo. */
    static void whisker(Ctx c, V3 a, V3 b, double bend, double r, float[] col) {
        V3 m = a.lerp(b, 0.5).add(0, bend, 0);
        tube(c, new V3[]{a, m, b}, new double[]{r, r * 0.8, r * 0.35}, 4, flat(col));
    }

    /** Tres dedos / garritas pequeñas hacia delante. */
    static void toes(Ctx c, V3 foot, double spread, double len, double r, float[] col) {
        for (int k = -1; k <= 1; k++) {
            ell(c, foot.add(k * spread, 0, len), new V3(r, r * 0.8, r * 1.3), 6, 4, flat(col));
        }
    }

    /** Almohadillas: una grande y tres chicas en la planta (se ve al levantar la pata). */
    static void paw(Ctx c, V3 pad, double r, float[] col) {
        ell(c, pad, new V3(r, r * 0.35, r * 0.85), 8, 4, flat(col));
        for (int k = -1; k <= 1; k++) {
            ell(c, pad.add(k * r * 0.95, 0, r * 1.15), new V3(r * 0.38, r * 0.25, r * 0.4), 6, 4, flat(col));
        }
    }

    /** Ceja corta y expresiva sobre un ojo (elipse achatada inclinada). */
    static void brow(Ctx c, V3 hc, V3 hr, double sd, double dx, double dy, double dz, double size, double roll, float[] col) {
        decal(c, hc, hr, new V3(sd * dx, dy, dz), size, roll, T_LEAF, col, 1, 0.003);
    }

    // ------------------------------------------------------------------ cajas de píxeles (criaturas estilo bloque)

    /** Color por píxel de una cara: face 0:+x 1:-x 2:+y 3:-y 4:+z 5:-z; i crece hacia la derecha vista desde fuera, j hacia arriba (o hacia +z en tapas). */
    interface Pix {
        float[] at(int face, int i, int j, int ni, int nj);
    }

    /** Caja dividida en píxeles de color propio (cada píxel es una cara). ce = centro, r = medias dimensiones. */
    static void pixBox(Ctx c, V3 ce, V3 r, int nx, int ny, int nz, Pix px) {
        int[][] dims = {{nz, ny}, {nz, ny}, {nx, nz}, {nx, nz}, {nx, ny}, {nx, ny}};
        for (int f = 0; f < 6; f++) {
            int ni = dims[f][0], nj = dims[f][1];
            V3 n, u, v;
            double ru, rv, rn;
            switch (f) {
                case 0 -> { n = new V3(1, 0, 0); u = new V3(0, 0, -1); v = new V3(0, 1, 0); ru = r.z; rv = r.y; rn = r.x; }
                case 1 -> { n = new V3(-1, 0, 0); u = new V3(0, 0, 1); v = new V3(0, 1, 0); ru = r.z; rv = r.y; rn = r.x; }
                case 2 -> { n = new V3(0, 1, 0); u = new V3(1, 0, 0); v = new V3(0, 0, 1); ru = r.x; rv = r.z; rn = r.y; }
                case 3 -> { n = new V3(0, -1, 0); u = new V3(1, 0, 0); v = new V3(0, 0, -1); ru = r.x; rv = r.z; rn = r.y; }
                case 4 -> { n = new V3(0, 0, 1); u = new V3(1, 0, 0); v = new V3(0, 1, 0); ru = r.x; rv = r.y; rn = r.z; }
                default -> { n = new V3(0, 0, -1); u = new V3(-1, 0, 0); v = new V3(0, 1, 0); ru = r.x; rv = r.y; rn = r.z; }
            }
            V3 ctr = ce.add(n.mul(rn));
            V3[] nn = {n, n, n, n};
            for (int i = 0; i < ni; i++) {
                for (int j = 0; j < nj; j++) {
                    double u0 = -1 + 2.0 * i / ni, u1 = -1 + 2.0 * (i + 1) / ni, v0 = -1 + 2.0 * j / nj, v1 = -1 + 2.0 * (j + 1) / nj;
                    V3[] p = {ctr.add(u.mul(u0 * ru)).add(v.mul(v0 * rv)), ctr.add(u.mul(u1 * ru)).add(v.mul(v0 * rv)),
                            ctr.add(u.mul(u1 * ru)).add(v.mul(v1 * rv)), ctr.add(u.mul(u0 * ru)).add(v.mul(v1 * rv))};
                    float[] col = px.at(f, i, j, ni, nj);
                    if (col == null) {
                        continue;
                    }
                    c.quad(c.blend, c.alpha, T_WHITE, p, nn, new float[][]{col, col, col, col}, null);
                }
            }
        }
    }
}
