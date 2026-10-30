package com.kensei.client;

/**
 * Matemática de las posturas de la katana en tercera persona (sin nada del juego, solo números).
 * <p>
 * En vez de "inventar" ángulos de brazo a ojo, cada postura se describe con dos direcciones, en el espacio del personaje
 * (derecha, arriba, delante): hacia dónde apunta la mano y hacia dónde apunta la hoja, más un giro del torso. Aquí se calcula el giro del
 * brazo (ángulos del modelo) con el que la hoja queda exactamente así, y el brazo libre apunta a la empuñadura para sostener la katana con
 * las dos manos. Los números base (dirección de la hoja respecto al brazo y posición del agarre) salen de la inclinación de 30° que lleva
 * {@code katana.json} en tercera persona.
 */
public final class KatanaStance {
    private KatanaStance() {
    }

    /** Dirección de la hoja en el espacio del brazo (con la inclinación de 30° de katana.json en tercera persona). */
    private static final double[] B0 = {0.0, 0.471404963, -0.881916867};
    /** Dónde está la empuñadura (donde se agarra con la otra mano) en el espacio del brazo, en píxeles. */
    private static final double[] GRIP = {-1.0, 7.39, 1.2385};
    private static final double[] SR = {-5.0, 2.0, 0.0};
    private static final double[] SL = {5.0, 2.0, 0.0};

    /** Postura = {mano r, u, f, hoja r, u, f, giro del torso}. */
    public static final double[] IDLE = {-0.45, -0.62, 0.64, 0.0, 0.62, 0.78, -0.12};
    public static final double[] GUARD = {-0.40, -0.20, 0.89, -0.05, 0.90, 0.40, -0.28};
    public static final double[] SPRINT = {0.15, -0.85, -0.50, 0.0, 0.10, -1.0, 0.0};

    /** Cada fila: {tiempo 0..1, postura (7 números)}. */
    public static final double[][] SLASH1 = {
            {0.0, 0.55, -0.25, 0.2, 0.75, 0.35, -0.5, 0.5},
            {0.35, 0.1, -0.30, 0.85, 0.1, 0.15, 0.98, 0.05},
            {1.0, -0.55, -0.25, 0.7, -0.8, 0.2, 0.55, -0.45}};
    public static final double[][] SLASH2 = {
            {0.0, -0.5, -0.2, 0.5, -0.75, 0.3, -0.3, -0.5},
            {0.35, -0.1, -0.30, 0.85, -0.1, 0.15, 0.98, -0.05},
            {1.0, 0.55, -0.25, 0.7, 0.8, 0.2, 0.55, 0.45}};
    public static final double[][] SLASH3 = {
            {0.0, 0.3, 0.85, 0.35, 0.15, 0.55, -0.8, 0.4},
            {0.45, 0.0, 0.3, 0.9, 0.0, 0.6, 0.8, 0.0},
            {1.0, -0.25, -0.55, 0.8, -0.3, -0.25, 0.92, -0.35}};
    public static final double[][] SLASH4 = {
            {0.0, -0.5, -0.75, 0.4, -0.45, -0.35, 0.8, -0.5},
            {0.5, 0.0, -0.2, 0.95, 0.0, 0.45, 0.9, 0.0},
            {1.0, 0.35, 0.7, 0.5, 0.2, 0.85, -0.45, 0.4}};
    public static final double[][] THRUST = {
            {0.0, 0.3, -0.6, 0.2, 0.0, 0.1, 0.99, 0.55},
            {1.0, -0.25, -0.30, 0.90, 0.0, 0.05, 1.0, -0.3}};
    /** Crítico: reposo, hoja alzada sobre la cabeza (0,5), tajo hacia abajo (1). */
    public static final double[][] HEAVY = {
            {0.0, -0.45, -0.62, 0.64, 0.0, 0.62, 0.78, -0.12},
            {0.5, 0.1, 0.9, 0.25, 0.0, 0.45, -0.9, 0.0},
            {1.0, 0.0, -0.3, 0.95, 0.0, -0.5, 0.85, 0.0}};

    /** Resultado de resolver una postura (todo en el lado derecho; el que llama lo espeja si el arma va en la mano izquierda). */
    public static final class Out {
        public float mx;
        public float my;
        public float mz;
        public float ox;
        public float oy;
        public float twist;
        /** 1 si la mano libre llega a la empuñadura, 0 si queda muy lejos (entonces se suelta). */
        public float follow;
        public double dist;
    }

    // ------------------------------------------------------------------ muestreo de posturas

    private static double ease(double u) {
        u = Math.max(0.0, Math.min(1.0, u));
        return u * u * (3.0 - 2.0 * u);
    }

    private static void norm3(double[] v, int o) {
        double l = Math.sqrt(v[o] * v[o] + v[o + 1] * v[o + 1] + v[o + 2] * v[o + 2]);
        if (l > 1e-9) {
            v[o] /= l;
            v[o + 1] /= l;
            v[o + 2] /= l;
        }
    }

    /** Mezcla dos posturas (las direcciones se mezclan y se vuelven a normalizar). */
    public static double[] lerp(double[] p, double[] q, double w) {
        double[] r = new double[7];
        for (int i = 0; i < 6; i++) {
            r[i] = p[i] * (1.0 - w) + q[i] * w;
        }
        // si se anulan (direcciones opuestas) se conserva la de partida
        norm3(r, 0);
        norm3(r, 3);
        r[6] = p[6] * (1.0 - w) + q[6] * w;
        return r;
    }

    private static double[] unit(double[] p) {
        double[] r = p.clone();
        norm3(r, 0);
        norm3(r, 3);
        return r;
    }

    private static double[] row(double[][] keys, int i) {
        double[] r = new double[7];
        System.arraycopy(keys[i], 1, r, 0, 7);
        return r;
    }

    /** La postura de una secuencia en el instante u (0..1), suavizando entre cada par de claves. */
    public static double[] sample(double[][] keys, double u) {
        if (u <= keys[0][0]) {
            return unit(row(keys, 0));
        }
        for (int i = 0; i < keys.length - 1; i++) {
            double t0 = keys[i][0];
            double t1 = keys[i + 1][0];
            if (u <= t1) {
                return lerp(unit(row(keys, i)), unit(row(keys, i + 1)), ease((u - t0) / (t1 - t0)));
            }
        }
        return unit(row(keys, keys.length - 1));
    }

    // ------------------------------------------------------------------ álgebra 3x3 (fila por fila)

    private static double[] mul(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
            }
        }
        return r;
    }

    private static double[] mulv(double[] a, double[] v) {
        return new double[] {
                a[0] * v[0] + a[1] * v[1] + a[2] * v[2],
                a[3] * v[0] + a[4] * v[1] + a[5] * v[2],
                a[6] * v[0] + a[7] * v[1] + a[8] * v[2]};
    }

    private static double[] ry(double a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[] {c, 0, s, 0, 1, 0, -s, 0, c};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] unitv(double[] v) {
        double l = Math.sqrt(dot(v, v));
        return l < 1e-9 ? new double[] {0, 1, 0} : new double[] {v[0] / l, v[1] / l, v[2] / l};
    }

    private static double[] cols(double[] c1, double[] c2, double[] c3) {
        return new double[] {c1[0], c2[0], c3[0], c1[1], c2[1], c3[1], c1[2], c2[2], c3[2]};
    }

    private static double[] transpose(double[] a) {
        return new double[] {a[0], a[3], a[6], a[1], a[4], a[7], a[2], a[5], a[8]};
    }

    /** Del espacio del personaje (derecha, arriba, delante) al del modelo (x a la izquierda, y hacia abajo, z hacia atrás). */
    private static double[] toModel(double r, double u, double f) {
        return new double[] {-r, -u, -f};
    }

    /** Giro del brazo con el que su eje (hacia abajo) apunta a {@code a} y la hoja apunta a {@code d}. */
    private static double[] solveMain(double[] a, double[] d) {
        double[] u1 = {0, 1, 0};
        double c = dot(B0, u1);
        double[] u2 = unitv(new double[] {B0[0] - c * u1[0], B0[1] - c * u1[1], B0[2] - c * u1[2]});
        double[] u3 = cross(u1, u2);
        double[] big = cols(u1, u2, u3);
        // el ángulo entre brazo y hoja es fijo (viene del modelo del objeto): se corrige la dirección de la mano para respetarlo
        double k = dot(a, d) - c;
        double[] aa = unitv(new double[] {a[0] - k * d[0], a[1] - k * d[1], a[2] - k * d[2]});
        double ad = dot(d, aa);
        double[] w2 = unitv(new double[] {d[0] - ad * aa[0], d[1] - ad * aa[1], d[2] - ad * aa[2]});
        double[] w3 = cross(aa, w2);
        double[] w = cols(aa, w2, w3);
        return mul(w, transpose(big));
    }

    /** Ángulos del modelo (x, y, z) de una rotación hecha como Rz·Ry·Rx, que es como el juego aplica rotationZYX. */
    private static double[] eulerZyx(double[] r) {
        double y = -Math.asin(Math.max(-1.0, Math.min(1.0, r[6])));
        double cy = Math.cos(y);
        double x;
        double z;
        if (Math.abs(cy) > 1e-6) {
            x = Math.atan2(r[7], r[8]);
            z = Math.atan2(r[3], r[0]);
        } else {
            x = Math.atan2(-r[5], r[4]);
            z = 0.0;
        }
        return new double[] {x, y, z};
    }

    /** Ángulos (x, y) de un brazo que apunta a {@code a} sin girar sobre sí mismo. */
    private static double[] armDir(double[] a) {
        a = unitv(a);
        double s = -Math.hypot(a[0], a[2]);
        double x = Math.atan2(s, a[1]);
        double y = s != 0.0 ? Math.atan2(-a[0], -a[2]) : 0.0;
        return new double[] {x, y};
    }

    /** Resuelve una postura (lado derecho): ángulos del brazo con el arma, del brazo libre y giro del torso. */
    public static void solve(double[] pose, Out out) {
        double[] a = unitv(toModel(pose[0], pose[1], pose[2]));
        double[] d = unitv(toModel(pose[3], pose[4], pose[5]));
        double tw = pose[6];
        double[] rt = ry(tw);
        double[] r = mul(rt, solveMain(a, d));
        double[] e = eulerZyx(r);
        out.mx = (float) e[0];
        out.my = (float) e[1];
        out.mz = (float) e[2];
        out.twist = (float) tw;
        double[] sr = mulv(rt, SR);
        double[] sl = mulv(rt, SL);
        double[] grip = mulv(r, GRIP);
        double[] v = {sr[0] + grip[0] - sl[0], sr[1] + grip[1] - sl[1], sr[2] + grip[2] - sl[2]};
        double dist = Math.sqrt(dot(v, v));
        double[] o = armDir(v);
        out.ox = (float) o[0];
        out.oy = (float) o[1];
        out.dist = dist;
        out.follow = (float) ease((14.5 - dist) / 2.5);
    }
}
