package com.mundosdeaxelis.vfx;

/**
 * Motor de criaturas 3D (compañeros): igual que el dragoncito, cada cuadro se arman esferas achatadas, tubos y decalcomanías
 * (ojos, mejillas, boca) con luz por vértice. Aquí están el motor, la animación común y las primeras especies;
 * el resto está en {@link Creatures2}. No depende de Minecraft.
 *
 * Marco local: +z = hacia donde mira, +y = arriba, los pies en y = 0, altura total ≈ 1.
 */
final class Creature3D {
    private Creature3D() {
    }

    static final int FOX = 0, CAT = 1, OWL = 2, BUNNY = 3, JELLY = 4, PHOENIX = 5, TURTLE = 6, PENGUIN = 7, UNICORN = 8, PANDA = 9, FAIRY = 10,
            CHICK = 11, FROG = 12, HAMSTER = 13, SNAIL = 14, BEE = 15, SHEEP = 16, HEDGEHOG = 17, AXOLOTL = 18, RACCOON = 19, DEER = 20, WHALE = 21, KITSUNE = 22,
            CAPYBARA = 23, DUCK = 24, SLIME = 25, CORGI = 26, CREEPER = 27, SNOWMAN = 28, GHOST = 29, GOLEM = 30, SPARKMOUSE = 31, DROID = 32,
            FOREST_SPIRIT = 33, MOON_WOLF = 34, NIGHT_DRAGON = 35,
            OCTOPUS = 36, BUTTERFLY = 37, CRAB = 38, FIREFLY = 39, KOALA = 40, ELEPHANT = 41, LION = 42, SALAMANDER = 43, PEACOCK = 44, DOLPHIN = 45,
            PEGASUS = 46, CELESTIAL = 47, FERRET = 48, MIMIC = 49, GRYPHON = 50, PIGGY = 51, SKUNK = 52, PATCH = 53, SEAL = 54, IMP = 55, TRIKE = 61, YETI = 62, SPROUT_KING = 63, EMBER_ANVIL = 64;
    static final int SPECIES = 65;

    // celdas de la textura creature3d.png: u0, v0, u1, v1
    static final float[] T_EYE = {0F, 0F, 0.5F, 0.5F};
    static final float[] T_EYE_SHUT = {0.5F, 0F, 1F, 0.5F};
    static final float[] T_BLUSH = {0F, 0.5F, 0.5F, 1F};
    static final float[] T_MOUTH = {0.75F, 0.5F, 1F, 0.75F};
    static final float[] T_BEAD = {0.5F, 0.5F, 0.75F, 0.75F};
    static final float[] T_CAT = {0.75F, 0.75F, 1F, 1F};
    static final float[] T_WHITE = {0.55F, 0.90F, 0.57F, 0.92F};

    static final float[] WHITE = {1F, 1F, 1F};
    static final float[] BLACK = {0.12F, 0.10F, 0.13F};
    static final float[] PINK = {1F, 0.62F, 0.70F};

    interface Paint {
        float[] at(V3 n, V3 p);
    }

    static Paint flat(float[] c) {
        return (n, p) -> c;
    }

    /** Contexto de un dibujo: de coordenadas del modelo a las del mundo + estado de animación. */
    static final class Ctx {
        final Sink s;
        final Cam cam;
        V3 origin;
        final double scale;
        final double[] m = new double[9];
        V3 pivot = new V3(0, 0.3, 0);
        boolean head;
        final double[] hm = new double[9];
        V3 hpv = new V3(0, 0.45, 0.2);
        final V3 light = new V3(-0.40, 0.80, 0.45).norm();
        boolean emissive;
        double lod = 1;
        double alpha = 1;
        int blend = Sink.SOLID;
        // animación
        double t, ph, walk, gait, speed, pitch, roll, yaw;
        float[] tint = {1, 1, 1};
        boolean shut;
        /** Vida: respiración (-1..1), saltito de alegría (0..1), sacudida (-1..1) y ladeo de cabeza curioso (0..1). */
        double breath, joy, shake, tilt;

        Ctx(Sink s, Cam cam, V3 origin, double scale) {
            this.s = s;
            this.cam = cam;
            this.origin = origin;
            this.scale = scale;
        }

        static V3 mul(double[] m, V3 v) {
            return new V3(m[0] * v.x + m[1] * v.y + m[2] * v.z, m[3] * v.x + m[4] * v.y + m[5] * v.z, m[6] * v.x + m[7] * v.y + m[8] * v.z);
        }

        V3 pos(V3 p) {
            if (head) {
                p = mul(hm, p.sub(hpv)).add(hpv);
            }
            V3 q = mul(m, p.sub(pivot)).add(pivot);
            return origin.add(q.mul(scale));
        }

        V3 nor(V3 n) {
            if (head) {
                n = mul(hm, n);
            }
            return mul(m, n);
        }

        float[] shade(float[] base, V3 wp, V3 wn, double a) {
            if (emissive) {
                return new float[]{Math.min(1, base[0]), Math.min(1, base[1]), Math.min(1, base[2]), (float) a};
            }
            V3 toCam = cam.pos.sub(wp).norm();
            double facing = wn.dot(toCam);
            V3 n = facing < 0 ? wn.mul(-1) : wn;
            double lam = Math.max(0, n.dot(light));
            double rim = Math.pow(1 - Math.abs(facing), 3) * 0.14;
            double k = 0.68 + 0.40 * lam;
            return new float[]{(float) Math.min(1, base[0] * k + rim), (float) Math.min(1, base[1] * k + rim * 0.85),
                    (float) Math.min(1, base[2] * k + rim * 0.6), (float) a};
        }

        void quad(int bl, double a, float[] cell, V3[] p, V3[] n, float[][] base, float[][] uvRel) {
            double[] pp = new double[12];
            float[] uv = new float[8];
            float[] col = new float[16];
            for (int i = 0; i < 4; i++) {
                V3 wp = pos(p[i]);
                V3 wn = nor(n[i]).norm();
                pp[i * 3] = wp.x;
                pp[i * 3 + 1] = wp.y;
                pp[i * 3 + 2] = wp.z;
                float[] c = shade(base[i], wp, wn, a);
                System.arraycopy(c, 0, col, i * 4, 4);
                float ru = uvRel == null ? 0.5F : uvRel[i][0], rv = uvRel == null ? 0.5F : uvRel[i][1];
                uv[i * 2] = cell[0] + (cell[2] - cell[0]) * ru;
                uv[i * 2 + 1] = cell[1] + (cell[3] - cell[1]) * rv;
            }
            s.quad(VfxTex.CREATURE3D, bl, pp, uv, col);
        }
    }

    // ------------------------------------------------------------------ utilidades

    static double[] rot(double yaw, double pitch, double roll) {
        double cy = Math.cos(yaw), sy = Math.sin(yaw), cz = Math.cos(roll), sz = Math.sin(roll), cx = Math.cos(pitch), sx = Math.sin(pitch);
        double[] rx = {1, 0, 0, 0, cx, -sx, 0, sx, cx};
        double[] rz = {cz, -sz, 0, sz, cz, 0, 0, 0, 1};
        double[] ry = {cy, 0, sy, 0, 1, 0, -sy, 0, cy};
        return mm(ry, mm(rz, rx));
    }

    static double[] mm(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
            }
        }
        return r;
    }

    static float[] mix(float[] a, float[] b, double t) {
        t = Math.max(0, Math.min(1, t));
        float f = (float) t;
        return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }

    static float[] scale(float[] a, double k) {
        return new float[]{(float) Math.min(1, a[0] * k), (float) Math.min(1, a[1] * k), (float) Math.min(1, a[2] * k)};
    }

    static float[] rgb(int c) {
        return new float[]{((c >> 16) & 255) / 255F, ((c >> 8) & 255) / 255F, (c & 255) / 255F};
    }

    static float[] mul(float[] a, float[] b) {
        return new float[]{a[0] * b[0], a[1] * b[1], a[2] * b[2]};
    }

    static double ss(double a, double b, double x) {
        double t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    static double cl(double x, double a, double b) {
        return Math.max(a, Math.min(b, x));
    }

    /** Punto de la superficie de un elipsoide en la dirección dada. */
    static V3 surf(V3 ce, V3 r, V3 dir) {
        V3 d = dir.norm();
        double k = 1 / Math.sqrt((d.x / r.x) * (d.x / r.x) + (d.y / r.y) * (d.y / r.y) + (d.z / r.z) * (d.z / r.z));
        return ce.add(d.mul(k));
    }

    // ------------------------------------------------------------------ piezas

    /** Esfera achatada con giro opcional (yaw, pitch, roll). */
    static void ell(Ctx c, V3 ce, V3 r, double yaw, double pitch, double roll, int nu, int nv, Paint paint) {
        nu = Math.max(6, (int) Math.round(nu * c.lod));
        nv = Math.max(4, (int) Math.round(nv * c.lod));
        double[] rm = (yaw == 0 && pitch == 0 && roll == 0) ? null : rot(yaw, pitch, roll);
        V3[][] P = new V3[nv + 1][nu + 1], N = new V3[nv + 1][nu + 1];
        for (int i = 0; i <= nv; i++) {
            double th = Math.PI * i / nv;
            for (int j = 0; j <= nu; j++) {
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
                    b[k] = paint.at(n[k], rm == null ? p[k].sub(ce) : p[k].sub(ce));
                }
                c.quad(c.blend, c.alpha, T_WHITE, p, n, b, null);
            }
        }
    }

    static void ell(Ctx c, V3 ce, V3 r, Paint paint) {
        ell(c, ce, r, 0, 0, 0, 12, 8, paint);
    }

    static void ell(Ctx c, V3 ce, V3 r, int nu, int nv, Paint paint) {
        ell(c, ce, r, 0, 0, 0, nu, nv, paint);
    }

    /** Tubo que pasa por unos puntos con radios distintos. */
    static void tube(Ctx c, V3[] pts, double[] rad, int sides, Paint paint) {
        sides = Math.max(4, (int) Math.round(sides * (0.5 + 0.5 * c.lod)));
        int n = pts.length;
        V3[][] ring = new V3[n][sides + 1], nor = new V3[n][sides + 1];
        V3 prevU = null;
        for (int i = 0; i < n; i++) {
            V3 t = (i == 0 ? pts[1].sub(pts[0]) : (i == n - 1 ? pts[n - 1].sub(pts[n - 2]) : pts[i + 1].sub(pts[i - 1]))).norm();
            V3 u = prevU == null ? t.anyPerp() : prevU.sub(t.mul(prevU.dot(t))).norm();
            if (u.len() < 1.0E-6) {
                u = t.anyPerp();
            }
            V3 v = t.cross(u).norm();
            prevU = u;
            for (int j = 0; j <= sides; j++) {
                double a = 2 * Math.PI * j / sides;
                V3 d = u.mul(Math.cos(a)).add(v.mul(Math.sin(a)));
                ring[i][j] = pts[i].add(d.mul(rad[i]));
                nor[i][j] = d;
            }
        }
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < sides; j++) {
                V3[] p = {ring[i][j], ring[i][j + 1], ring[i + 1][j + 1], ring[i + 1][j]};
                V3[] nn = {nor[i][j], nor[i][j + 1], nor[i + 1][j + 1], nor[i + 1][j]};
                float[][] b = new float[4][];
                for (int k = 0; k < 4; k++) {
                    b[k] = paint.at(nn[k], p[k]);
                }
                c.quad(c.blend, c.alpha, T_WHITE, p, nn, b, null);
            }
        }
    }

    /** Cono / pata sencilla de dos puntos. */
    static void cone(Ctx c, V3 a, V3 b, double ra, double rb, int sides, Paint paint) {
        tube(c, new V3[]{a, a.lerp(b, 0.5), b}, new double[]{ra, (ra + rb) * 0.5, rb}, sides, paint);
    }

    /** Curva suave (Catmull-Rom) por puntos de control; devuelve los puntos y los radios interpolados. */
    static V3[] spline(V3[] cp, double[] rad, int sub, double[][] outRad) {
        int n = cp.length;
        V3[] out = new V3[(n - 1) * sub + 1];
        double[] orad = new double[out.length];
        for (int i = 0; i < n - 1; i++) {
            V3 p0 = cp[Math.max(0, i - 1)], p1 = cp[i], p2 = cp[i + 1], p3 = cp[Math.min(n - 1, i + 2)];
            for (int k = 0; k < sub; k++) {
                double t = k / (double) sub, t2 = t * t, t3 = t2 * t;
                double a = -0.5 * t3 + t2 - 0.5 * t, b = 1.5 * t3 - 2.5 * t2 + 1, cc = -1.5 * t3 + 2 * t2 + 0.5 * t, d = 0.5 * t3 - 0.5 * t2;
                out[i * sub + k] = p0.mul(a).add(p1.mul(b)).add(p2.mul(cc)).add(p3.mul(d));
                orad[i * sub + k] = rad[i] + (rad[i + 1] - rad[i]) * t;
            }
        }
        out[out.length - 1] = cp[n - 1];
        orad[out.length - 1] = rad[n - 1];
        outRad[0] = orad;
        return out;
    }

    /** Tubo curvo (spline) en un solo paso. */
    static void curve(Ctx c, V3[] cp, double[] rad, int sub, int sides, Paint paint) {
        double[][] rr = new double[1][];
        V3[] sp = spline(cp, rad, sub, rr);
        tube(c, sp, rr[0], sides, paint);
    }

    /** Pegatina sobre un elipsoide (ojos, mejillas, boca). */
    static void decal(Ctx c, V3 ce, V3 r, V3 dir, double size, double rollUp, float[] cell, float[] tint, double alpha, double lift) {
        V3 d = dir.norm();
        V3 sp = surf(ce, r, d);
        V3 n = new V3(d.x / r.x, d.y / r.y, d.z / r.z).norm();
        sp = sp.add(n.mul(lift));
        V3 right = n.cross(V3.UP).norm();
        V3 up = right.cross(n).norm();
        if (rollUp != 0) {
            V3 r2 = right.mul(Math.cos(rollUp)).add(up.mul(Math.sin(rollUp)));
            up = up.mul(Math.cos(rollUp)).sub(right.mul(Math.sin(rollUp)));
            right = r2;
        }
        double h = size * 0.5;
        V3[] p = {sp.sub(right.mul(h)).sub(up.mul(h)), sp.add(right.mul(h)).sub(up.mul(h)), sp.add(right.mul(h)).add(up.mul(h)), sp.sub(right.mul(h)).add(up.mul(h))};
        V3[] nn = {n, n, n, n};
        float[][] b = {tint, tint, tint, tint};
        float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
        boolean em = c.emissive;
        c.emissive = false;
        c.quad(Sink.NORMAL, alpha, cell, p, nn, b, uv);
        c.emissive = em;
    }

    /** Par de ojos simétricos sobre la cabeza. */
    static void eyes(Ctx c, V3 hc, V3 hr, double dx, double dy, double dz, double size, float[] cell, float[] tint) {
        float[] cl = c.shut ? T_EYE_SHUT : cell;
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, hc, hr, new V3(sd * dx, dy, dz), size, cell == T_EYE ? sd * 0.10 : 0, cl, c.shut ? WHITE : tint, 1, 0.004);
        }
    }

    static void cheeks(Ctx c, V3 hc, V3 hr, double dx, double dy, double dz, double size) {
        for (int sd = -1; sd <= 1; sd += 2) {
            decal(c, hc, hr, new V3(sd * dx, dy, dz), size, 0, T_BLUSH, new float[]{1F, 0.62F, 0.58F}, 0.85, 0.002);
        }
    }

    /** Resplandor suave (cuadro que mira a la cámara, mezcla aditiva). */
    static void glow(Ctx c, V3 pt, double size, float[] col, double alpha) {
        V3 w = c.pos(pt);
        double h = size * c.scale * 0.5;
        V3 r = c.cam.right.mul(h), u = c.cam.up.mul(h);
        double[] pp = {w.x - r.x - u.x, w.y - r.y - u.y, w.z - r.z - u.z, w.x + r.x - u.x, w.y + r.y - u.y, w.z + r.z - u.z,
                w.x + r.x + u.x, w.y + r.y + u.y, w.z + r.z + u.z, w.x - r.x + u.x, w.y - r.y + u.y, w.z - r.z + u.z};
        float[] uv = {0, 1, 1, 1, 1, 0, 0, 0};
        float[] cc = new float[16];
        for (int i = 0; i < 4; i++) {
            cc[i * 4] = col[0];
            cc[i * 4 + 1] = col[1];
            cc[i * 4 + 2] = col[2];
            cc[i * 4 + 3] = (float) alpha;
        }
        c.s.quad(VfxTex.SOFT, Sink.ADD, pp, uv, cc);
    }

    /**
     * Cuatro patas: delantera-izquierda y trasera-derecha caminan juntas. hipY = altura de la cadera; los pies quedan
     * en el suelo cuando el compañero está quieto.
     */
    static void legs4(Ctx c, double xs, double zf, double zb, double hipY, double rad, double footH, double footLen, Paint leg, Paint foot) {
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int fb = 0; fb < 2; fb++) {
                double z0 = fb == 0 ? zf : zb;
                double phase = c.gait + ((sd > 0) == (fb == 0) ? 0 : Math.PI);
                double sw = Math.sin(phase) * 0.075 * c.walk;
                double lift = Math.max(0, Math.cos(phase)) * 0.05 * c.walk;
                V3 hip = new V3(sd * xs, hipY, z0);
                V3 fo = new V3(sd * xs, footH + lift, z0 + sw);
                // muslo redondeado, rodilla con un pequeño quiebre y menudillo antes del pie
                V3 kn = hip.lerp(fo, 0.5).add(0, 0, sw * 0.2 + (fb == 0 ? 0.012 : -0.014));
                tube(c, new V3[]{hip, kn, fo}, new double[]{rad * 1.15, rad * 0.93, rad * 0.8}, 7, leg);
                final V3 thC = hip.add(0, -rad * 0.5, 0), ftC = fo.add(0, rad * 0.9, 0);
                ell(c, thC, new V3(rad * 1.3, rad * 1.5, rad * 1.5), 8, 6, (n, p) -> leg.at(n, p.add(thC)));
                ell(c, ftC, new V3(rad * 0.92, rad * 0.75, rad * 0.92), 7, 5, (n, p) -> leg.at(n, p.add(ftC)));
                ell(c, fo.add(0, 0, footLen * 0.28), new V3(rad * 1.12, footH, footLen), 10, 6, foot);
            }
        }
    }

    // ------------------------------------------------------------------ entrada

    static void emit(VfxData d, double age, double partial, V3 pos, Cam cam, Sink s) {
        int sp = (int) Math.round(d.b);
        if (sp < 0 || sp >= SPECIES || d.size <= 0) {
            return;
        }
        // aparición con un rebote suave y desaparición encogiéndose
        double pop = ss(0, 9, age);
        pop = pop < 1 ? pop * (1 + 0.12 * Math.sin(pop * Math.PI)) : 1;
        if (d.life > 0) {
            pop *= ss(0, 7, d.life - age);
        }
        if (pop < 0.02) {
            return;
        }
        double ph = d.id * 1.7;
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
        double dist = cam.pos.sub(pos).len();
        if (dist > 64) {
            return;
        }
        Ctx c = new Ctx(s, cam, pos, d.size * pop);
        c.lod = dist < 12 ? 1.0 : (dist < 28 ? 0.72 : 0.5);
        c.t = age;
        c.ph = ph;
        c.yaw = yaw;
        c.speed = Math.min(1, Math.sqrt(vel.x * vel.x + vel.z * vel.z) * 2.2);
        c.walk = ss(0.06, 0.40, c.speed);
        c.gait = age * 0.62 + ph;
        boolean flier = sp == GRYPHON || sp == JELLY || sp == PHOENIX || sp == FAIRY || sp == BEE || sp == WHALE || sp == GHOST || sp == NIGHT_DRAGON
                || sp == BUTTERFLY || sp == FIREFLY || sp == DOLPHIN || sp == PEGASUS || sp == CELESTIAL;
        c.pitch = flier ? cl(vf * 1.1 - vy * 0.5, -0.30, 0.45) : cl(vf * 0.5 - vy * 0.4, -0.18, 0.18);
        c.roll = flier ? cl(vl * 1.0, -0.30, 0.30) : 0;
        double bc0 = (age + ph * 7) % 96;
        c.shut = bc0 < 5 || (bc0 >= 10 && bc0 < 14 && ((long) ((age + ph * 7) / 96)) % 3 == 0);
        // vida: respiración, saltito de alegría, sacudida y ladeo de cabeza (solo cuando está quieto)
        double idle = 1 - c.walk;
        c.breath = Math.sin(age * 0.11 + ph);
        double joyU = ((age + ph * 31) % 290) / 16.0;
        c.joy = (joyU < 1 && idle > 0.6 && !flier) ? Math.sin(joyU * Math.PI) : 0;
        double wigU = ((age + ph * 17 + 90) % 430) / 26.0;
        c.shake = (wigU < 1 && idle > 0.6) ? Math.sin(wigU * Math.PI * 6) * Math.sin(wigU * Math.PI) : 0;
        double tiltU = ((age + ph * 23 + 40) % 340) / 56.0;
        c.tilt = (tiltU < 1 && idle > 0.4) ? Math.sin(tiltU * Math.PI) : 0;
        c.tint = rgb(d.rgb);
        // el saltito eleva todo el modelo y la sacudida lo gira un poco
        c.origin = pos.add(0, c.joy * 0.075 * c.scale, 0);
        double[] bm = rot(yaw + c.shake * 0.20, c.pitch, c.roll);
        // respirar: el cuerpo se estira y encoge apenas (más al estar quieto); al saltar se estira
        double stretch = 1 + 0.014 * c.breath * idle + 0.06 * c.joy;
        bm[1] *= stretch;
        bm[4] *= stretch;
        bm[7] *= stretch;
        System.arraycopy(bm, 0, c.m, 0, 9);
        switch (sp) {
            case FOX -> Pets10.griffin(c);
            case CAT -> Pets9.fortuneCat(c);
            case OWL -> Pets13.emberPup(c);
            case BUNNY -> Pets9.moonBunny(c);
            case JELLY -> Pets11.seahorse(c);
            case PHOENIX -> Pets12.thunderbird(c);
            case TURTLE -> Pets10.islandTurtle(c);
            case PENGUIN -> Pets10.explorerPenguin(c);
            case UNICORN -> Pets13.angelLamb(c);
            case PANDA -> Pets11.monkRedPanda(c);
            case CHICK -> Pets8.mushroom(c);
            case FROG -> Pets8.frogBulb(c);
            case HAMSTER -> Pets8.sootSprite(c);
            case SNAIL -> Pets8.cottageSnail(c);
            case BEE -> Creatures3.bee(c);
            case SHEEP -> Pets9.goat(c);
            case HEDGEHOG -> Pets9.turboHedgehog(c);
            case AXOLOTL -> Creatures3.axolotl(c);
            case RACCOON -> Pets10.maskedThief(c);
            case DEER -> Pets13.jadeDragon(c);
            case WHALE -> Pets12.skyWhale(c);
            case KITSUNE -> Creatures4.kitsune(c);
            case CAPYBARA -> Pets8.tanuki(c);
            case DUCK -> Pets8.pirateDuck(c);
            case SLIME -> Creatures5.slime(c);
            case CORGI -> Pets9.ninjaDog(c);
            case CREEPER -> Creatures5.creeper(c);
            case SNOWMAN -> Creatures5.snowman(c);
            case GHOST -> Creatures5.ghost(c);
            case GOLEM -> Creatures5.golem(c);
            case SPARKMOUSE -> Creatures5.sparkmouse(c);
            case DROID -> Creatures5.droid(c);
            case FOREST_SPIRIT -> Creatures5.forestSpirit(c);
            case MOON_WOLF -> Pets11.cerberusPup(c);
            case NIGHT_DRAGON -> Creatures5.nightDragon(c);
            case OCTOPUS -> Pets9.squidPainter(c);
            case BUTTERFLY -> Creatures6.butterfly(c);
            case CRAB -> Pets8.teapotCrab(c);
            case FIREFLY -> Pets9.lanternSpirit(c);
            case KOALA -> Pets9.sloth(c);
            case ELEPHANT -> Pets10.mammoth(c);
            case LION -> Creatures7.lion(c);
            case SALAMANDER -> Creatures7.salamander(c);
            case PEACOCK -> Pets11.quetzal(c);
            case DOLPHIN -> Pets11.narwhal(c);
            case PEGASUS -> Creatures7.pegasus(c);
            case CELESTIAL -> Creatures7.celestial(c);
            case FERRET -> Creatures7.ferret(c);
            case MIMIC -> Pets14.mimic(c);
            case GRYPHON -> Pets14.royalGriffin(c);
            case PIGGY -> Pets17.piggyBank(c);
            case SKUNK -> Pets17.skunkWizard(c);
            case SEAL -> Pets17.sailorSeal(c);
            case IMP -> Pets15.moonImp(c);
            case PATCH -> Pets18.patchMonster(c);
            case TRIKE -> Pets18.heartTrike(c);
            case YETI -> Pets18.snowYeti(c);
            case SPROUT_KING -> Pets19.sproutKing(c);
            case EMBER_ANVIL -> Pets19.emberAnvil(c);
            default -> Creatures2.fairy(c);
        }
    }

    /** Giro de cabeza: mira alrededor de vez en cuando. */
    static void headLook(Ctx c, double amp, double pitchBias) {
        double t = c.t, ph = c.ph;
        double hy = amp * Math.sin(t * 0.07 + ph) + (Math.sin(t * 0.023 + ph) > 0.92 ? 0.30 : 0) + c.tilt * 0.10;
        double hp = 0.06 * Math.sin(t * 0.09 + ph + 1) - c.pitch * 0.6 + pitchBias - c.joy * 0.12 + 0.015 * c.breath;
        c.head = true;
        System.arraycopy(rot(hy, hp, 0.04 * Math.sin(t * 0.05 + ph) + c.tilt * 0.24), 0, c.hm, 0, 9);
    }

    // ------------------------------------------------------------------ 1. ZORRO ESTELAR

    private static final float[] F_OR = {1.00F, 0.55F, 0.18F}, F_OR2 = {1.00F, 0.70F, 0.34F}, F_CR = {1.00F, 0.95F, 0.84F},
            F_DK = {0.30F, 0.17F, 0.13F}, F_STAR = {1F, 0.92F, 0.45F};

    static void fox(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.015 * w;
        c.pivot = new V3(0, 0.28, 0);
        // cola esponjosa
        double wag = Math.sin(t * (0.20 + 0.25 * c.joy) + ph) * (0.05 + 0.04 * w + 0.05 * c.joy);
        V3[] tcp = {new V3(0, 0.29, -0.24), new V3(wag * 0.4, 0.27, -0.38), new V3(wag, 0.32, -0.52), new V3(wag * 1.4, 0.43, -0.60), new V3(wag * 1.2, 0.53, -0.58)};
        double[] trd = {0.055, 0.095, 0.120, 0.100, 0.060};
        curve(c, tcp, trd, 4, 10, (n, p) -> mix(F_OR, F_OR2, ss(-0.5, 0.8, n.y) * 0.5));
        Detail.furTube(c, tcp, trd, 80, 101, 0.07, 0.03, 0.9, (n, p) -> mix(F_OR, F_OR2, 0.4), F_CR);
        ell(c, tcp[4].add(wag * 0.1, 0.045, -0.015), new V3(0.075, 0.085, 0.075), F_PAINT_CREAM);
        legs4(c, 0.080, 0.12, -0.17, 0.20 + bob, 0.048, 0.034, 0.07, (n, p) -> mix(F_OR, F_DK, ss(0.28, 0.0, p.y + 0.12) * 0.9), flat(F_DK));
        // cuerpo
        V3 bc = new V3(0, 0.285 + bob, -0.02), br = new V3(0.145, 0.135, 0.25);
        Paint bp = (n, p) -> mix(mix(F_OR, F_OR2, ss(0, 0.9, n.y) * 0.4), F_CR, ss(-0.15, -0.7, n.y) * 0.9);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 140, 102, 0.03, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        // cuello esponjoso de pelo crema
        Detail.fur(c, bc, br, 50, 103, 0.06, 0.02, new V3(0, -1, 0.3), 0.7, d -> ss(0.5, 0.9, d.z) * ss(0.6, -0.2, d.y), bp, F_CR);
        // pecas de estrellas en el lomo
        double[][] fr = {{0.35, 0.9, -0.3}, {-0.4, 0.85, 0.1}, {0.15, 0.95, 0.55}, {-0.25, 0.9, -0.7}, {0.5, 0.7, 0.3}};
        for (int k = 0; k < fr.length; k++) {
            Detail.glint(c, bc, br, new V3(fr[k][0], fr[k][1], fr[k][2]), 0.032 + 0.008 * (k % 2), F_STAR, 0.55 + 0.4 * Math.sin(t * 0.1 + ph + k * 1.7));
        }
        ell(c, new V3(0, 0.27 + bob, 0.13), new V3(0.105, 0.115, 0.10), F_PAINT_CREAM);
        // cabeza
        headLook(c, 0.13, 0);
        c.hpv = new V3(0, 0.38, 0.17);
        V3 hc = new V3(0, 0.455 + bob, 0.265), hr = new V3(0.165, 0.135, 0.145);
        ell(c, new V3(0, 0.40 + bob, 0.15), new V3(0.10, 0.10, 0.09), (n, p) -> F_OR);
        Paint hp = (n, p) -> mix(F_OR, F_OR2, 0.2 + ss(-0.2, 0.9, n.y) * 0.3);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 104, 0.03, 0.013, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.6 ? 1 : 0, hp, null);
        // patillas largas de pelo crema en las mejillas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 6; k++) {
                V3 d0 = new V3(sd * (0.9 - k * 0.04), -0.30 + k * 0.05, 0.25 - k * 0.1);
                V3 bs = surf(hc, hr, d0);
                Detail.kite(c, bs, bs.add(sd * (0.035 + k * 0.004), -0.04 + k * 0.006, -0.04), new V3(0, 0, 1), 0.014, new V3(sd, 0, 0), F_OR2, F_CR);
            }
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.115, 0.415 + bob, 0.275), new V3(0.075, 0.06, 0.075), 0, 0, sd * 0.3, 10, 7, F_PAINT_CREAM);
        }
        V3 sc = new V3(0, 0.425 + bob, 0.385), sr = new V3(0.062, 0.050, 0.080);
        ell(c, sc, sr, 10, 7, F_PAINT_CREAM);
        ell(c, new V3(0, 0.45 + bob, 0.458), new V3(0.024, 0.019, 0.019), 8, 5, flat(F_DK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.075, 0, T_CAT, new float[]{1, 1, 1}, 0.9, 0.002);
        eyes(c, hc, hr, 0.62, 0.18, 0.78, 0.105, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.82, -0.22, 0.52, 0.09);
        // orejas grandes con punta oscura e interior crema
        double ear = Detail.flick(c, 1) * 0.03;
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.085, 0.545 + bob, 0.225);
            V3 tp = new V3(sd * (0.135 + ear * 2), 0.745 + bob - ear * 3, 0.19);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.068, 0.050, 0.004}, 4, (n, p) -> mix(F_OR, F_DK, ss(0.66, 0.73, p.y - bob)));
            V3 b2 = b.add(0, 0.004, 0.026), t2 = tp.add(sd * -0.006, -0.05, 0.026);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.046, 0.032, 0.004}, 4, flat(F_CR));
        }
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.05, 0.43 + bob + k * 0.012, 0.395), new V3(sd * 0.17, 0.435 + bob + k * 0.03, 0.34), -0.012, 0.0028, new float[]{1F, 0.95F, 0.85F});
            }
            Detail.brow(c, hc, hr, sd, 0.58, 0.40, 0.70, 0.055, sd * 0.6, F_DK);
        }
        // estrellita en la frente
        c.emissive = true;
        glowStar(c, surf(hc, hr, new V3(0, 0.62, 0.9)).add(0, 0, 0.012), 0.045, F_STAR, t, ph);
        c.emissive = false;
        c.head = false;
        // collar azul noche con una estrella dorada
        V3[] ring = new V3[13];
        for (int i = 0; i < 13; i++) {
            double a2 = Math.PI * 2 * i / 12.0;
            ring[i] = new V3(Math.cos(a2) * 0.105, 0.385 + bob, 0.155 + Math.sin(a2) * 0.095);
        }
        double[] cr = new double[13];
        java.util.Arrays.fill(cr, 0.014);
        tube(c, ring, cr, 6, flat(new float[]{0.20F, 0.24F, 0.58F}));
        c.emissive = true;
        glowStar(c, new V3(0, 0.355 + bob, 0.255), 0.035, F_STAR, t, ph + 1);
        c.emissive = false;
    }

    private static final Paint F_PAINT_CREAM = (n, p) -> mix(F_CR, F_OR2, ss(0.3, 0.9, n.y) * 0.25);

    /** Estrellita de 4 puntas (dos rombos cruzados) que parpadea, mirando a la cámara. */
    static void glowStar(Ctx c, V3 at, double size, float[] col, double t, double ph) {
        double k = 0.75 + 0.25 * Math.sin(t * 0.22 + ph);
        glow(c, at, size * 3.4 * k, col, 0.55);
        glow(c, at, size * 1.2, WHITE, 0.9);
    }

    // ------------------------------------------------------------------ 2. GATITO LUNAR

    private static final float[] C_DK2 = {0.52F, 0.50F, 0.80F};
    private static final float[] C_FUR = {0.74F, 0.72F, 0.95F}, C_FUR2 = {0.88F, 0.86F, 1.0F}, C_DK = {0.43F, 0.40F, 0.72F},
            C_BELLY = {0.98F, 0.96F, 1.0F}, C_MOON = {1F, 0.90F, 0.45F};

    static void cat(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double bob = Math.abs(Math.sin(c.gait)) * 0.012 * w;
        c.pivot = new V3(0, 0.27, 0);
        // cola larga y curva
        double sw = Math.sin(t * 0.16 + ph);
        double tw = 1 + 1.2 * c.joy;
        V3[] tcp = new V3[6];
        double[] trd = {0.040, 0.040, 0.038, 0.036, 0.034, 0.032};
        for (int i = 0; i < 6; i++) {
            double k = i / 5.0;
            tcp[i] = new V3(Math.sin(t * 0.16 * tw + ph - i * 0.5) * 0.06 * k * tw, 0.28 + 0.46 * k * k + 0.02 * Math.sin(i), -0.24 - 0.22 * Math.sin(k * 1.4));
        }
        curve(c, tcp, trd, 3, 8, (n, p) -> mix(C_FUR, C_DK, ss(0.52, 0.72, p.y)));
        Detail.furTube(c, tcp, trd, 50, 111, 0.045, 0.02, 0.8, (n, p) -> mix(C_FUR, C_DK, ss(0.52, 0.72, p.y)), null);
        // anillos de la cola
        for (int k = 1; k <= 3; k++) {
            int ix = k + 1;
            V3 d = tcp[ix + 1].sub(tcp[ix - 1]).norm();
            V3 u = d.anyPerp(), v = d.cross(u).norm();
            V3[] rg = new V3[9];
            for (int i = 0; i < 9; i++) {
                double a2 = Math.PI * 2 * i / 8.0;
                rg[i] = tcp[ix].add(u.mul(Math.cos(a2) * (trd[ix] + 0.003))).add(v.mul(Math.sin(a2) * (trd[ix] + 0.003)));
            }
            double[] rrg = new double[9];
            java.util.Arrays.fill(rrg, 0.007);
            tube(c, rg, rrg, 4, flat(C_DK));
        }
        ell(c, tcp[5].add(0, 0.015, -0.005), new V3(0.036, 0.040, 0.040), flat(C_DK));
        legs4(c, 0.075, 0.12, -0.18, 0.20 + bob, 0.042, 0.032, 0.065, (n, p) -> mix(C_FUR, C_BELLY, ss(0.1, 0.0, p.y + 0.1) * 0.9), flat(C_BELLY));
        V3 bc = new V3(0, 0.275 + bob, -0.03), br = new V3(0.125, 0.12, 0.24);
        Paint bp = (n, p) -> mix(mix(C_FUR, C_FUR2, ss(0, 0.9, n.y) * 0.4), C_BELLY, ss(-0.15, -0.8, n.y));
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 120, 112, 0.032, 0.014, new V3(0, -0.5, -1), 0.9, Detail.ALL, bp, null);
        Detail.fur(c, bc, br, 36, 113, 0.05, 0.018, new V3(0, -1, 0.2), 0.7, d -> ss(0.5, 0.9, d.z) * ss(0.4, -0.3, d.y), bp, C_BELLY);
        // rayas de gato atigrado en el lomo y los costados
        for (int k = 0; k < 4; k++) {
            double zz = 0.45 - k * 0.3;
            Detail.leaf(c, bc, br, new V3(0.45, 0.9, zz), 0.06, 1.2, C_DK2);
            Detail.leaf(c, bc, br, new V3(-0.45, 0.9, zz), 0.06, -1.2, C_DK2);
            Detail.leaf(c, bc, br, new V3(0.95, 0.35, zz), 0.055, 0.4, C_DK2);
            Detail.leaf(c, bc, br, new V3(-0.95, 0.35, zz), 0.055, -0.4, C_DK2);
        }
        // cabeza
        headLook(c, 0.14, 0);
        c.hpv = new V3(0, 0.38, 0.17);
        V3 hc = new V3(0, 0.46 + bob, 0.25), hr = new V3(0.155, 0.13, 0.135);
        ell(c, new V3(0, 0.395 + bob, 0.14), new V3(0.09, 0.09, 0.09), flat(C_FUR));
        Paint hp = (n, p) -> mix(C_FUR, C_FUR2, 0.2 + ss(-0.2, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 16, 11, hp);
        Detail.fur(c, hc, hr, 70, 114, 0.028, 0.012, new V3(0, -0.3, -1), 0.8, d -> d.z < 0.6 ? 1 : 0, hp, null);
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 4; k++) {
                V3 bs = surf(hc, hr, new V3(sd * (0.9 - k * 0.04), -0.25 + k * 0.06, 0.2 - k * 0.1));
                Detail.kite(c, bs, bs.add(sd * 0.04, -0.035, -0.03), new V3(0, 0, 1), 0.014, new V3(sd, 0, 0), C_FUR, C_BELLY);
            }
            // marquitas atigradas sobre los ojos
            for (int k = -1; k <= 1; k++) {
                Detail.leaf(c, hc, hr, new V3(sd * (0.18 + k * 0.16), 0.80, 0.55), 0.04, sd * k * 0.4, C_DK2);
            }
        }
        V3 sc = new V3(0, 0.43 + bob, 0.355), sr = new V3(0.05, 0.04, 0.05);
        ell(c, sc, sr, 10, 7, flat(C_BELLY));
        ell(c, new V3(0, 0.445 + bob, 0.402), new V3(0.016, 0.012, 0.012), 8, 5, flat(PINK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.07, 0, T_CAT, new float[]{1, 1, 1}, 0.9, 0.002);
        eyes(c, hc, hr, 0.60, 0.16, 0.80, 0.135, T_EYE, new float[]{1F, 0.88F, 0.35F});
        cheeks(c, hc, hr, 0.85, -0.25, 0.48, 0.085);
        // orejas triangulares
        for (int sd = -1; sd <= 1; sd += 2) {
            double fl = Detail.flick(c, sd > 0 ? 2 : 3);
            V3 b = new V3(sd * 0.085, 0.53 + bob, 0.21), tp = new V3(sd * (0.115 + 0.035 * fl), 0.66 + bob - 0.035 * fl, 0.185);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.06, 0.042, 0.004}, 4, (n, p) -> mix(C_FUR, C_DK, ss(0.60, 0.66, p.y - bob)));
            V3 b2 = b.add(0, 0.004, 0.022), t2 = tp.add(sd * -0.004, -0.04, 0.022);
            tube(c, new V3[]{b2, b2.lerp(t2, 0.5), t2}, new double[]{0.04, 0.028, 0.004}, 4, flat(PINK));
        }
        // collarcito con cascabel
        V3[] ring = new V3[13];
        for (int i = 0; i < 13; i++) {
            double a2 = Math.PI * 2 * i / 12.0;
            ring[i] = new V3(Math.cos(a2) * 0.095, 0.39 + bob, 0.15 + Math.sin(a2) * 0.085);
        }
        double[] cr = new double[13];
        java.util.Arrays.fill(cr, 0.013);
        tube(c, ring, cr, 6, flat(new float[]{0.75F, 0.30F, 0.55F}));
        ell(c, new V3(0, 0.365 + bob, 0.232), new V3(0.019, 0.019, 0.019), 8, 6, flat(new float[]{1F, 0.82F, 0.30F}));
        Detail.glint(c, new V3(0, 0.365 + bob, 0.232), new V3(0.019, 0.019, 0.019), new V3(-0.4, 0.6, 1), 0.02, WHITE, 0.9);
        // bigotes
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 3; k++) {
                V3 b = new V3(sd * 0.05, 0.425 + bob + (k - 1) * 0.012, 0.385);
                V3 e = b.add(sd * 0.115, (k - 1) * 0.022 + 0.01, -0.01);
                tube(c, new V3[]{b, b.lerp(e, 0.5), e}, new double[]{0.003, 0.0025, 0.001}, 3, flat(WHITE));
            }
        }
        // luna creciente en la frente (brilla)
        c.emissive = true;
        V3[] cp = new V3[7];
        for (int i = 0; i < 7; i++) {
            double a = 0.7 + (Math.PI * 2 - 1.4) * i / 6.0;
            cp[i] = surf(hc, hr, new V3(0.17 * Math.cos(a) + 0.04, 0.60 + 0.17 * Math.sin(a), 1)).add(0, 0, 0.004);
        }
        curve(c, cp, new double[]{0.003, 0.009, 0.013, 0.014, 0.013, 0.009, 0.003}, 2, 5, flat(C_MOON));
        c.emissive = false;
        c.head = false;
    }

    // ------------------------------------------------------------------ 3. BÚHO SABIO

    private static final float[] O_BR = {0.60F, 0.42F, 0.28F}, O_BR2 = {0.76F, 0.58F, 0.40F}, O_CR = {1.0F, 0.93F, 0.78F}, O_DK = {0.36F, 0.24F, 0.17F}, O_OR = {1F, 0.72F, 0.22F};

    static void owl(Ctx c) {
        double t = c.t, ph = c.ph;
        double bob = Math.sin(t * 0.13 + ph) * 0.03;
        c.pivot = new V3(0, 0.4, 0);
        double fl = Math.sin(t * (0.35 + c.speed * 0.2) + ph);
        double flap = 0.25 + 0.55 * (0.5 + 0.5 * fl) * (0.4 + 0.6 * Math.max(c.speed, 0.4));
        // alas
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 sh = new V3(sd * 0.185, 0.46 + bob, -0.02);
            for (int k = 0; k < 5; k++) {
                double a = -0.62 - k * 0.20 + flap * 1.0;
                double len = 0.18 + 0.045 * (4 - Math.abs(k - 1.5)) * 0.9;
                V3 e = sh.add(sd * Math.cos(a) * len * 1.05, Math.sin(a) * len + 0.10 * flap, -0.05 - k * 0.03);
                tube(c, new V3[]{sh, sh.lerp(e, 0.5).add(0, 0.02, 0), e}, new double[]{0.040, 0.034, 0.012}, 5, (n, p) -> mix(O_BR, O_BR2, ss(0.1, 0.65, p.y - bob) * 0.8));
            }
        }
        // cola de abanico con barras
        for (int k = -3; k <= 3; k++) {
            V3 a0 = new V3(k * 0.014, 0.21 + bob, -0.09), b0 = new V3(k * 0.045, 0.12 + bob, -0.27);
            Detail.feather(c, a0, b0, new V3(1, 0, 0), 0.026, new V3(0, 0.6, 0.4).norm(), O_DK, mix(O_DK, O_BR2, 0.5));
            Detail.feather(c, a0.add(0, 0.004, 0), b0.add(0, 0.004, 0), new V3(1, 0, 0), 0.022, new V3(0, 0.6, 0.4).norm(), O_BR, O_BR2);
        }
        // cuerpo
        V3 bc = new V3(0, 0.40 + bob, -0.01), br = new V3(0.19, 0.235, 0.17);
        Paint bp = (n, p) -> {
            double belly = ss(-0.1, 0.5, n.z) * ss(0.9, 0.0, Math.abs(n.x) * 1.2);
            float[] b = mix(O_BR, O_BR2, ss(-0.2, 0.9, n.y) * 0.4);
            float[] col = mix(b, O_CR, belly);
            double chev = belly * (0.5 + 0.5 * Math.sin(p.y * 70 + Math.abs(p.x) * 55));
            return scale(col, 1.0 - 0.10 * chev);
        };
        ell(c, bc, br, 16, 11, bp);
        // plumaje en filas: más claro y redondeado en el pecho, moteado en la espalda
        Detail.plumage(c, bc, br, 10, 14, 0.06, 0.02, d -> 1, bp, O_BR2);
        Detail.fur(c, bc, br, 40, 121, 0.03, 0.012, new V3(0, -1, -0.2), 0.9, Detail.FRONT, bp, O_CR);
        // patitas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int j = -1; j <= 1; j++) {
                V3 b = new V3(sd * 0.065 + j * 0.022, 0.17 + bob, 0.075);
                tube(c, new V3[]{b, b.add(j * 0.004, -0.02, 0.025)}, new double[]{0.012, 0.004}, 4, flat(O_OR));
                tube(c, new V3[]{b.add(j * 0.004, -0.02, 0.025), b.add(j * 0.007, -0.034, 0.045)}, new double[]{0.006, 0.0015}, 4, flat(O_DK));
            }
        }
        // cabeza
        headLook(c, 0.18, 0);
        c.hpv = new V3(0, 0.62, 0.0);
        V3 hc = new V3(0, 0.67 + bob, 0.035), hr = new V3(0.205, 0.17, 0.165);
        Paint hp = (n, p) -> mix(O_BR, O_BR2, 0.2 + ss(-0.2, 0.9, n.y) * 0.4);
        ell(c, hc, hr, 18, 12, hp);
        Detail.fur(c, hc, hr, 60, 122, 0.03, 0.013, new V3(0, -0.6, -1), 0.8, d -> d.z < 0.5 ? 1 : 0, hp, O_BR2);
        for (int sd = -1; sd <= 1; sd += 2) {
            // disco facial claro y ojo grande
            V3 fc = new V3(sd * 0.075, 0.675 + bob, 0.155);
            ell(c, fc, new V3(0.082, 0.088, 0.03), 12, 8, (n, p) -> O_CR);
            // borde del disco facial: plumitas que salen en abanico
            for (int k = 0; k < 16; k++) {
                double a2 = k * Math.PI * 2 / 16.0;
                V3 e0 = fc.add(Math.cos(a2) * 0.075, Math.sin(a2) * 0.080, 0.012);
                V3 e1 = e0.add(Math.cos(a2) * 0.04, Math.sin(a2) * 0.04, -0.012);
                Detail.kite(c, e0, e1, new V3(-Math.sin(a2), Math.cos(a2), 0), 0.014, new V3(0, 0, 1), O_CR, O_BR2);
            }
            decal(c, fc, new V3(0.082, 0.088, 0.03), new V3(0, 0, 1), 0.125, sd * 0.1, c.shut ? T_EYE_SHUT : T_EYE, c.shut ? WHITE : new float[]{1F, 0.80F, 0.22F}, 1, 0.004);
        }
        // pico ganchudo de dos piezas
        cone(c, new V3(0, 0.672 + bob, 0.178), new V3(0, 0.628 + bob, 0.258), 0.034, 0.004, 6, flat(O_OR));
        cone(c, new V3(0, 0.652 + bob, 0.18), new V3(0, 0.632 + bob, 0.235), 0.026, 0.005, 6, flat(mix(O_OR, O_DK, 0.35)));
        // cejas de plumas
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = 0; k < 4; k++) {
                V3 e0 = new V3(sd * (0.03 + k * 0.03), 0.74 + bob - k * 0.003, 0.15 - k * 0.012);
                Detail.feather(c, e0, e0.add(sd * 0.035, 0.03 + k * 0.004, -0.02), new V3(0, 0, 1), 0.012, new V3(0, 1, 0.3).norm(), O_DK, O_BR2);
            }
        }
        // penachos
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 b = new V3(sd * 0.115, 0.79 + bob, 0.03), tp = new V3(sd * 0.155, 0.93 + bob, -0.02);
            double fk = Detail.flick(c, sd > 0 ? 7 : 8);
            tp = tp.add(sd * 0.02 * fk, -0.03 * fk, 0);
            tube(c, new V3[]{b, b.lerp(tp, 0.5), tp}, new double[]{0.045, 0.034, 0.004}, 5, (n, p) -> mix(O_BR, O_DK, ss(0.8, 0.92, p.y - bob)));
            for (int k = 0; k < 3; k++) {
                Detail.kite(c, b.lerp(tp, 0.25 + k * 0.18).add(0, 0, 0.03), b.lerp(tp, 0.38 + k * 0.18).add(sd * 0.02, 0, 0.035), new V3(1, 0, 0), 0.012, new V3(0, 0, 1), O_DK, O_BR2);
            }
        }
        // gafas de sabio
        for (int sd = -1; sd <= 1; sd += 2) {
            V3 fc = new V3(sd * 0.075, 0.675 + bob, 0.185);
            V3[] ring = new V3[13];
            for (int i = 0; i < 13; i++) {
                double a = Math.PI * 2 * i / 12.0;
                ring[i] = fc.add(Math.cos(a) * 0.075, Math.sin(a) * 0.078, 0);
            }
            c.emissive = false;
            tube(c, ring, new double[]{0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005, 0.005}, 4, flat(new float[]{0.95F, 0.78F, 0.30F}));
        }
        tube(c, new V3[]{new V3(-0.012, 0.68 + bob, 0.19), new V3(0, 0.69 + bob, 0.195), new V3(0.012, 0.68 + bob, 0.19)}, new double[]{0.005, 0.005, 0.005}, 4, flat(new float[]{0.95F, 0.78F, 0.30F}));
        c.head = false;
    }

    // ------------------------------------------------------------------ 4. CONEJITO DE LA SUERTE

    private static final float[] B_WH = {1F, 0.97F, 0.95F}, B_PK = {1F, 0.72F, 0.78F}, B_GR = {0.30F, 0.80F, 0.38F}, B_GR2 = {0.55F, 0.95F, 0.55F};
    private static final float[] B_FLUFF = {0.94F, 0.95F, 1F}, B_EARTIP = {0.78F, 0.80F, 0.92F}, B_BOW = {1F, 0.50F, 0.68F}, B_BOW2 = {0.78F, 0.25F, 0.45F};

    static void bunny(Ctx c) {
        double t = c.t, ph = c.ph, w = c.walk;
        double hop = Math.abs(Math.sin(c.gait * 0.9)) * 0.11 * w;
        c.pivot = new V3(0, 0.25, 0);
        double lean = -Math.cos(c.gait * 0.9) * 0.12 * w;
        // pelota de cola
        V3 tc = new V3(0, 0.25 + hop, -0.215), tr = new V3(0.065, 0.065, 0.065);
        ell(c, tc, tr, 8, 6, flat(WHITE));
        Detail.fur(c, tc, tr, 46, 71, 0.04, 0.016, new V3(0, 0.3, -1), 0.5, Detail.ALL, flat(B_WH), B_FLUFF);
        // patas traseras grandes, delanteras pequeñas
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.095, 0.075 + hop * 0.6, -0.07 - 0.03 * w), new V3(0.062, 0.062, 0.115), 10, 7, (n, p) -> mix(B_WH, B_PK, ss(-0.4, -0.95, n.y) * 0.5));
            ell(c, new V3(sd * 0.075, 0.12 + hop, 0.115), new V3(0.04, 0.085, 0.045), 8, 6, flat(B_WH));
            ell(c, new V3(sd * 0.075, 0.04 + hop * 0.8, 0.135), new V3(0.045, 0.03, 0.06), 8, 6, flat(B_WH));
            Detail.toes(c, new V3(sd * 0.075, 0.032 + hop * 0.8, 0.135), 0.02, 0.055, 0.011, B_PK);
            ell(c, new V3(sd * 0.095, 0.052 + hop * 0.6, -0.17 - 0.03 * w), new V3(0.03, 0.02, 0.03), 8, 5, flat(B_PK));
        }
        V3 bc = new V3(0, 0.235 + hop, -0.045), br = new V3(0.155, 0.145, 0.185);
        Paint bp = (n, p) -> mix(B_WH, new float[]{0.95F, 0.90F, 0.90F}, ss(0, -0.9, n.y) * 0.5);
        ell(c, bc, br, 14, 10, bp);
        Detail.fur(c, bc, br, 120, 72, 0.034, 0.014, new V3(0, -0.8, -1), 0.8, Detail.ALL, bp, B_FLUFF);
        // pechito esponjoso
        Detail.fur(c, bc, br, 40, 73, 0.05, 0.016, new V3(0, -1, 0.2), 0.6, d -> ss(0.4, 0.8, d.z) * ss(0.5, -0.1, d.y), bp, B_FLUFF);
        // cabeza
        headLook(c, 0.12, lean);
        c.hpv = new V3(0, 0.34, 0.1);
        V3 hc = new V3(0, 0.43 + hop, 0.135), hr = new V3(0.165, 0.14, 0.145);
        ell(c, hc, hr, 16, 11, (n, p) -> B_WH);
        // mejillas de pelo largo y pelusa en la frente
        Detail.fur(c, hc, hr, 70, 74, 0.03, 0.012, new V3(0, -0.4, -1), 0.7, Detail.and(Detail.ALL, d -> d.z < 0.6 ? 1 : 0), flat(B_WH), B_FLUFF);
        Detail.fur(c, hc, hr, 40, 75, 0.05, 0.016, new V3(0, -0.5, -0.6), 0.6, d -> ss(0.55, 0.9, Math.abs(d.x)) * ss(0.3, -0.5, d.y), flat(B_WH), B_FLUFF);
        V3 sc = new V3(0, 0.405 + hop, 0.245), sr = new V3(0.055, 0.042, 0.05);
        ell(c, sc, sr, 10, 7, flat(B_WH));
        double nz = Math.sin(t * 0.9 + ph) * 0.0025 * (0.5 + Detail.flick(c, 6));
        ell(c, new V3(0, 0.425 + hop + nz, 0.29), new V3(0.018, 0.013, 0.012), 8, 5, flat(PINK));
        decal(c, sc, sr, new V3(0, -0.5, 1), 0.065, 0, T_CAT, new float[]{1, 1, 1}, 0.9, 0.002);
        Detail.glint(c, new V3(0, 0.432 + hop, 0.29), new V3(0.018, 0.013, 0.012), new V3(0, 0.6, 1), 0.016, WHITE, 0.9);
        // dientitos de conejo
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, new V3(sd * 0.009, 0.376 + hop, 0.283), new V3(0.0085, 0.016, 0.005), 6, 4, flat(WHITE));
        }
        // bigotes
        for (int sd = -1; sd <= 1; sd += 2) {
            for (int k = -1; k <= 1; k++) {
                Detail.whisker(c, new V3(sd * 0.045, 0.405 + hop + k * 0.008, 0.27), new V3(sd * 0.165, 0.405 + hop + k * 0.028, 0.215), -0.012, 0.0028, new float[]{0.55F, 0.52F, 0.6F});
            }
            Detail.brow(c, hc, hr, sd, 0.60, 0.50, 0.62, 0.05, sd * 0.5, new float[]{0.85F, 0.80F, 0.88F});
        }
        eyes(c, hc, hr, 0.64, 0.18, 0.76, 0.112, T_BEAD, new float[]{1, 1, 1});
        cheeks(c, hc, hr, 0.86, -0.22, 0.45, 0.095);
        // orejas largas que se mecen
        for (int sd = -1; sd <= 1; sd += 2) {
            double sw = Math.sin(t * 0.09 + ph + sd) * 0.015 + lean * 0.1 + Detail.flick(c, sd > 0 ? 4 : 5) * 0.03 * sd;
            V3[] ec = {new V3(sd * 0.075, 0.54 + hop, 0.10), new V3(sd * (0.085 + sw), 0.66 + hop, 0.085), new V3(sd * (0.11 + sw * 2), 0.78 + hop, 0.045 - lean * 0.2),
                    new V3(sd * (0.14 + sw * 3), 0.87 + hop, -0.01 - lean * 0.3)};
            curve(c, ec, new double[]{0.052, 0.046, 0.040, 0.020}, 3, 7, (n, p) -> mix(B_WH, B_EARTIP, ss(0.78, 0.88, p.y - hop)));
            V3[] ic = new V3[ec.length];
            for (int i = 0; i < ic.length; i++) {
                ic[i] = ec[i].add(0, 0, 0.022 - i * 0.002);
            }
            curve(c, ic, new double[]{0.032, 0.030, 0.026, 0.012}, 3, 6, flat(B_PK));
        }
        // moñito rosa en la oreja izquierda
        V3 bw = new V3(-0.088, 0.585 + hop, 0.13);
        for (int sd = -1; sd <= 1; sd += 2) {
            ell(c, bw.add(sd * 0.032, 0, 0), new V3(0.032, 0.021, 0.014), 0, 0, sd * 0.45, 8, 5, (n, p) -> mix(B_BOW, B_BOW2, ss(-0.5, 0.8, n.y)));
        }
        ell(c, bw, new V3(0.014, 0.014, 0.012), 6, 4, flat(B_BOW2));
        // trébol de cuatro hojas (suerte)
        V3 cb = new V3(0.085, 0.555 + hop, 0.18);
        V3 cs = cb.add(0.0, 0.05, 0.0);
        tube(c, new V3[]{cb, cb.lerp(cs, 0.5), cs}, new double[]{0.006, 0.006, 0.006}, 4, flat(B_GR));
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            V3 lc = cs.add(Math.cos(a) * 0.026, 0.004, Math.sin(a) * 0.026);
            ell(c, lc, new V3(0.025, 0.007, 0.025), 8, 5, (n, p) -> mix(B_GR, B_GR2, ss(0, 1, n.y) * 0.6));
        }
        c.head = false;
    }

    // ------------------------------------------------------------------ 5. MEDUSA DE NUBE

    static void jelly(Ctx c) {
        double t = c.t, ph = c.ph;
        double pul = Math.sin(t * 0.14 + ph);
        double bob = Math.sin(t * 0.09 + ph) * 0.03;
        c.pivot = new V3(0, 0.6, 0);
        float[] top = {0.80F, 0.90F, 1.0F}, low = {1.0F, 0.80F, 0.95F}, lilac = {0.92F, 0.82F, 1.0F};
        double hs = 1 + 0.07 * pul, vs = 1 - 0.09 * pul;
        V3 bc = new V3(0, 0.62 + bob, 0);
        // tentáculos (detrás de la campana)
        for (int k = 0; k < 7; k++) {
            double a = Math.PI * 2 * k / 7.0;
            double rr = k == 0 ? 0.0 : 0.13;
            V3 b = new V3(Math.cos(a) * rr * hs, 0.52 + bob, Math.sin(a) * rr * hs);
            int n = 6;
            V3[] cp = new V3[n];
            double[] rd = new double[n];
            double len = 0.34 + 0.14 * ((k * 5) % 3) / 2.0;
            for (int i = 0; i < n; i++) {
                double f = i / (double) (n - 1);
                double wv = Math.sin(t * 0.17 + ph + k * 1.3 - f * 3.0) * 0.055 * f;
                cp[i] = new V3(b.x + wv + Math.cos(a) * 0.02 * f, b.y - len * f - 0.03 * pul * f, b.z + Math.cos(t * 0.13 + k - f * 2.5) * 0.04 * f);
                rd[i] = 0.016 * (1 - f * 0.55);
            }
            curve(c, cp, rd, 2, 5, (nn, p) -> mix(lilac, low, ss(0.4, 0.0, p.y - 0.2)));
            c.emissive = true;
            glow(c, cp[n - 1], 0.07, new float[]{1F, 0.7F, 0.95F}, 0.55 + 0.25 * Math.sin(t * 0.2 + k));
            c.emissive = false;
        }
        // brazos largos festoneados
        for (int k = 0; k < 4; k++) {
            double a = Math.PI * 2 * k / 4.0 + 0.6;
            V3 b = new V3(Math.cos(a) * 0.06, 0.55 + bob, Math.sin(a) * 0.06);
            V3[] cp = new V3[5];
            double[] rd = new double[5];
            for (int i = 0; i < 5; i++) {
                double f = i / 4.0;
                cp[i] = new V3(b.x + Math.sin(t * 0.12 + ph + k * 2 - f * 2.2) * 0.05 * f, b.y - 0.30 * f, b.z + Math.cos(t * 0.11 + k - f * 2) * 0.05 * f);
                rd[i] = 0.032 * (1 - f * 0.8);
            }
            curve(c, cp, rd, 2, 6, (nn, p) -> mix(low, new float[]{1F, 0.92F, 0.98F}, 0.5));
        }
        // campana translúcida
        c.blend = Sink.NORMAL;
        c.alpha = 0.82;
        ell(c, bc, new V3(0.23 * hs, 0.19 * vs, 0.23 * hs), 0, 0, 0, 20, 12, (n, p) -> mix(low, top, ss(-0.2, 0.9, n.y)));
        // anillo del borde
        for (int k = 0; k < 14; k++) {
            double a = Math.PI * 2 * k / 14.0;
            ell(c, new V3(Math.cos(a) * 0.205 * hs, 0.545 + bob - 0.01 * Math.sin(a * 3 + t * 0.1), Math.sin(a) * 0.205 * hs), new V3(0.04, 0.03, 0.04), 8, 5, (n, p) -> mix(low, lilac, 0.5));
        }
        c.alpha = 1;
        c.blend = Sink.SOLID;
        // nubecitas encima
        float[] cw = {1F, 1F, 1F};
        ell(c, new V3(-0.06, 0.80 + bob - 0.02 * pul, 0.0), new V3(0.075, 0.055, 0.07), 10, 7, (n, p) -> mix(new float[]{0.85F, 0.92F, 1F}, cw, ss(-0.3, 0.9, n.y)));
        ell(c, new V3(0.04, 0.83 + bob - 0.02 * pul, 0.015), new V3(0.09, 0.065, 0.085), 10, 7, (n, p) -> mix(new float[]{0.85F, 0.92F, 1F}, cw, ss(-0.3, 0.9, n.y)));
        ell(c, new V3(0.12, 0.795 + bob - 0.02 * pul, -0.01), new V3(0.06, 0.045, 0.06), 10, 7, (n, p) -> mix(new float[]{0.85F, 0.92F, 1F}, cw, ss(-0.3, 0.9, n.y)));
        // lluvita que cae de la nube y brillitos de la campana
        for (int k = 0; k < 4; k++) {
            double u = ((t * 0.02 + ph * 0.2 + k * 0.25) % 1.0);
            V3 dp = new V3(-0.04 + k * 0.05, 0.78 + bob - u * 0.22, 0.0);
            c.blend = Sink.NORMAL; c.alpha = 0.6 * (1 - u);
            ell(c, dp, new V3(0.006, 0.014, 0.006), 5, 4, flat(new float[]{0.7F, 0.88F, 1F}));
            c.alpha = 1; c.blend = Sink.SOLID;
        }
        for (int k = 0; k < 5; k++) {
            Detail.twinkle(c, new V3(Math.sin(k * 2.4) * 0.12 * hs, 0.64 + bob + Math.cos(k * 1.9) * 0.06 * vs, Math.cos(k * 2.4) * 0.12 * hs), 0.014, new float[]{0.9F, 0.95F, 1F}, t, ph + k * 1.6, 0.1);
        }
        // cara
        V3 fr = new V3(0.23 * hs, 0.19 * vs, 0.23 * hs);
        c.blend = Sink.SOLID;
        eyes(c, bc, fr, 0.50, 0.02, 0.86, 0.095, T_BEAD, new float[]{1, 1, 1});
        decal(c, bc, fr, new V3(0, -0.20, 1), 0.07, 0, T_MOUTH, new float[]{1, 1, 1}, 1, 0.004);
        cheeks(c, bc, fr, 0.72, -0.17, 0.65, 0.085);
        Detail.glint(c, bc, fr, new V3(-0.4, 1, 0.5), 0.07, WHITE, 0.8);
        for (int sd = -1; sd <= 1; sd += 2) {
            Detail.brow(c, bc, fr, sd, 0.50, 0.22, 0.84, 0.05, sd * 0.3, new float[]{0.7F, 0.6F, 0.9F});
        }
        // brillo interior
        c.emissive = true;
        glow(c, bc.add(0, -0.03, 0), 0.52, new float[]{0.65F, 0.80F, 1F}, 0.22 + 0.06 * pul);
        c.emissive = false;
    }
}
