package com.casino.game;

/**
 * La rueda y la bola de la ruleta: el movimiento completo se calcula como función del tiempo, así que el servidor solo dice "salió el 17,
 * empezó en el tick X" y todos los clientes ven exactamente la misma tirada (y la bola cae justo en el número sorteado).
 * Ángulos en radianes, en el plano de la mesa (x, z): el ángulo a apunta a (cos a, sin a). La rueda gira hacia ángulos mayores; la bola, al revés.
 */
public final class RouletteWheel {
    public static final int N = 37;
    public static final double POCKET = 2 * Math.PI / N;
    /** Duración de la tirada (13 s): la bola corre por la pista, cae hacia las casillas y rebota hasta quedarse quieta. */
    public static final int SPIN_TICKS = 260;
    /** Hasta aquí la bola corre por la pista. */
    public static final int TA = 150;

    private static final double W0 = 0.018;    // la rueda siempre gira despacio (rad por tick)
    private static final double BOOST = 0.055; // y se acelera al lanzar la bola
    private static final double DECAY = 90.0;
    private static final int TMAX = 400;
    private static final double BALL_V0 = 0.45; // velocidad inicial de la bola en la pista
    private static final double BALL_VC = 0.20; // velocidad al empezar a caer

    private RouletteWheel() {
    }

    public static int pocketIndex(int number) {
        for (int i = 0; i < N; i++) {
            if (RouletteBets.WHEEL[i] == number) {
                return i;
            }
        }
        return 0;
    }

    /** Ángulo de una casilla en el marco de la rueda. */
    public static double pocketAngle(int number) {
        return pocketIndex(number) * POCKET;
    }

    private static double extra(double tau) {
        double t = Math.max(0, Math.min(TMAX, tau));
        return BOOST * DECAY * (1 - Math.exp(-t / DECAY));
    }

    private static double extraSpeed(double tau) {
        return tau < 0 || tau > TMAX ? 0 : BOOST * Math.exp(-tau / DECAY);
    }

    /** Ángulo de la rueda en el instante g (ticks del juego). seq = cuántas tiradas se lanzaron; start = tick en que empezó la última. */
    public static double wheelAngle(double g, long start, int seq) {
        double a = W0 * g;
        if (seq > 0) {
            a += (seq - 1) * extra(TMAX) + extra(g - start);
        }
        return a;
    }

    private static double wheelSpeed(double g, long start, int seq) {
        return W0 + (seq > 0 ? extraSpeed(g - start) : 0);
    }

    /** Dónde está la bola: ángulo en el mundo, radio normalizado (1 = pista, 0 = casillas), altura del rebote (m) y si ya quedó quieta. */
    public static final class Ball {
        public double angle;
        public double radius;
        public double height;
        public boolean landed;
        /** Ángulo respecto de la rueda (para contar casillas que cruza y para los "tic"). */
        public double rel;
        /** 0 = corriendo por la pista, 1 = cayendo y rebotando, 2 = quieta en su casilla. */
        public int phase;
    }

    private static double startAngle(int seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return ((h & 0xFFFFFF) / (double) 0x1000000) * 2 * Math.PI;
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    private static double mod2pi(double a) {
        double m = a % (2 * Math.PI);
        return m < 0 ? m + 2 * Math.PI : m;
    }

    /** Distancia total (rad) que recorre la bola respecto de la rueda desde TA hasta quedar quieta, y el exponente de frenado. */
    private static double[] fall(double phiA, double target, double v0abs) {
        double d = mod2pi(phiA - target);
        double dur = SPIN_TICKS - TA;
        while (d < 4.0 || v0abs * dur / d > 3.2) {
            d += 2 * Math.PI;
        }
        return new double[]{d, v0abs * dur / d};
    }

    private static double ballWorldA(double tau, double angle0) {
        double t = Math.min(tau, TA);
        return angle0 - (BALL_V0 * t - (BALL_V0 - BALL_VC) * t * t / (2.0 * TA));
    }

    public static Ball ball(double g, long start, int seq, int number, int seed) {
        Ball b = new Ball();
        double wa = wheelAngle(g, start, seq);
        double target = pocketAngle(number);
        if (seq <= 0) { // todavía no se jugó: la bola descansa en el cero
            b.rel = target;
            b.angle = wa + target;
            b.landed = true;
            b.phase = 2;
            return b;
        }
        double tau = Math.max(0, g - start);
        double angle0 = startAngle(seed);
        if (tau >= SPIN_TICKS) {
            b.rel = target;
            b.angle = wa + target;
            b.landed = true;
            b.phase = 2;
            return b;
        }
        if (tau <= TA) {
            b.angle = ballWorldA(tau, angle0);
            b.rel = b.angle - wa;
            b.radius = 1.0;
            // en el lanzamiento la bola vuela un instante
            double lt = Math.max(0, 1 - tau / 10.0);
            b.height = 0.12 * lt * lt;
            b.phase = 0;
            return b;
        }
        double wa0 = wheelAngle(start + TA, start, seq);
        double phiA = ballWorldA(TA, angle0) - wa0;
        double v0abs = BALL_VC + wheelSpeed(start + TA, start, seq);
        double[] f = fall(phiA, target, v0abs);
        double dur = SPIN_TICKS - TA;
        double u = (tau - TA) / dur;
        double s = 1 - Math.pow(1 - u, f[1]);
        double osc = POCKET * 0.9 * Math.sin(Math.PI * 7 * u) * Math.pow(1 - u, 3) * smooth((u - 0.5) / 0.1);
        b.rel = phiA - f[0] * s + osc;
        b.angle = wa + b.rel;
        // la bola se despega de la pista y cae en espiral hacia las casillas
        b.radius = 1 - smooth(u / 0.55);
        // rebotes cada vez más chicos
        double[] hu = {0.56, 0.70, 0.81, 0.90, 0.96};
        double[] hd = {0.14, 0.11, 0.09, 0.06, 0.04};
        double[] ha = {0.085, 0.055, 0.032, 0.016, 0.007};
        double h = 0;
        for (int i = 0; i < hu.length; i++) {
            double t = (u - hu[i]) / hd[i];
            if (t > 0 && t < 1) {
                h += ha[i] * 4 * t * (1 - t);
            }
        }
        b.height = h;
        b.phase = 1;
        return b;
    }

    /** Exponente de frenado de la caída, para las pruebas. */
    public static double fallExponent(int number, int seed, long start, int seq) {
        double angle0 = startAngle(seed);
        double wa0 = wheelAngle(start + TA, start, seq);
        double phiA = ballWorldA(TA, angle0) - wa0;
        double v0abs = BALL_VC + wheelSpeed(start + TA, start, seq);
        return fall(phiA, pocketAngle(number), v0abs)[1];
    }
}
