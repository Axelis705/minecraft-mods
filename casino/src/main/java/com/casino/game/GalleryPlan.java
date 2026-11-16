package com.casino.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Galería de tiro del saloon (Java puro): qué blancos salen, por dónde pasan en cada momento y si un disparo le dio a alguno.
 * <p>
 * Todo sale de una semilla, así el cliente dibuja exactamente lo mismo que el servidor revisa. Coordenadas "del mostrador": u a la
 * derecha del tirador, v hacia arriba (desde la base del bloque del mostrador), w hacia el tirador (los blancos están en w negativo).
 * <ul>
 * <li>Fila de enfrente: patitos que nadan de izquierda a derecha (1 punto).</li>
 * <li>Fila del medio: botellas que pasan de derecha a izquierda (2 puntos).</li>
 * <li>Fila del fondo: bandidos que se levantan un momento (3 puntos)... y a veces una señorita: ¡no le dispares! (-3).</li>
 * <li>Arriba, muy rápido y pocas veces: la estrella del sheriff (5 puntos).</li>
 * </ul>
 */
public final class GalleryPlan {
    public static final int TPS = 20;
    /** Duración de una partida, balas y tiempo mínimo entre disparos (ticks). */
    public static final int DURATION = 30 * TPS;
    public static final int SHOTS = 12;
    public static final int COOLDOWN = 6;
    /** Cuenta regresiva antes de que salgan los blancos. */
    public static final int COUNTDOWN = 40;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    public static final int DUCK = 0, BOTTLE = 1, BANDIT = 2, LADY = 3, STAR = 4;
    public static final int[] POINTS = {1, 2, 3, -3, 5};
    /** Medio ancho y medio alto de cada blanco (bloques). */
    public static final double[] HALF_W = {0.22, 0.10, 0.27, 0.26, 0.20};
    public static final double[] HALF_H = {0.20, 0.24, 0.42, 0.42, 0.20};

    /** Filas: distancia (w), altura del centro (v) y ancho del recorrido (de -U a +U). */
    public static final double W_FRONT = -5.0, W_MID = -7.5, W_BACK = -10.0, W_STAR = -10.4;
    public static final double V_FRONT = 0.42, V_MID = 1.30, V_BACK = 1.55, V_STAR = 2.75;
    public static final double U = 4.3;

    /** Premio por puntos: desde estos puntos se paga esto (x10). */
    public static final int[] SCORE_AT = {14, 18, 22, 26, 30};
    public static final int[] SCORE_X10 = {10, 15, 20, 30, 50};

    /** Un blanco: aparece en {@code start} y se va en {@code end} (ticks desde que empieza la partida). */
    public static final class Target {
        public final int id, type, start, end;
        /** Para los que se mueven: de dónde a dónde. Para los que se levantan: dónde (u0). */
        public final double u0, u1;

        Target(int id, int type, int start, int end, double u0, double u1) {
            this.id = id;
            this.type = type;
            this.start = start;
            this.end = end;
            this.u0 = u0;
            this.u1 = u1;
        }

        public boolean popUp() {
            return type == BANDIT || type == LADY;
        }

        public double w() {
            return type == DUCK ? W_FRONT : type == BOTTLE ? W_MID : type == STAR ? W_STAR : W_BACK;
        }

        public double baseV() {
            return type == DUCK ? V_FRONT : type == BOTTLE ? V_MID : type == STAR ? V_STAR : V_BACK;
        }

        /** Posición a lo largo de su fila en el tiempo t (puede ser fraccionario). */
        public double u(double t) {
            if (popUp()) {
                return u0;
            }
            double k = (t - start) / (double) (end - start);
            return u0 + (u1 - u0) * k;
        }

        /** Cuánto está levantado (0 acostado, 1 de pie): los que se levantan suben y bajan; los demás están siempre de pie. */
        public double raise(double t) {
            if (t < start || t > end) {
                return 0;
            }
            if (!popUp()) {
                return 1;
            }
            double in = Math.min(1, (t - start) / 4.0), out = Math.min(1, (end - t) / 4.0);
            return Math.max(0, Math.min(in, out));
        }

        /** Altura del centro (los patitos se mecen, las botellas tiemblan un poco). */
        public double v(double t) {
            double b = baseV();
            if (type == DUCK) {
                return b + Math.sin((t + id * 7) * 0.35) * 0.03;
            }
            return b;
        }

        public boolean alive(double t) {
            return t >= start && t <= end;
        }
    }

    public final long seed;
    public final List<Target> targets;

    private long rng;

    public GalleryPlan(long seed) {
        this.seed = seed;
        this.rng = seed * 0x2545F4914F6CDD1DL + 1442695040888963407L;
        List<Target> l = new ArrayList<>();
        int id = 0;
        // patitos: uno cada ~1.1 s, cruzan en 4 s
        for (int t = 0; t < DURATION - 10; t += 18 + nextInt(8)) {
            l.add(new Target(id++, DUCK, t, t + 80, -U - 0.3, U + 0.3));
        }
        // botellas: una cada ~1.6 s, al revés, en 5 s
        for (int t = 10; t < DURATION - 10; t += 26 + nextInt(14)) {
            l.add(new Target(id++, BOTTLE, t, t + 100, U + 0.2, -U - 0.2));
        }
        // bandidos y señoritas: se levantan 1.3 s en un lugar al azar (sin encimarse)
        double lastU = 0;
        for (int t = 20; t < DURATION - 30; t += 22 + nextInt(18)) {
            double u;
            int tries = 0;
            do {
                u = (rand() * 2 - 1) * (U - 0.6);
            } while (Math.abs(u - lastU) < 1.6 && ++tries < 8);
            lastU = u;
            int type = nextInt(100) < 28 ? LADY : BANDIT;
            l.add(new Target(id++, type, t, t + 26, u, u));
        }
        // estrellas: 3 por partida, en momentos al azar, cruzan en 1.4 s
        int[] when = {60 + nextInt(120), 240 + nextInt(120), 420 + nextInt(120)};
        for (int i = 0; i < when.length; i++) {
            boolean ltr = rand() < 0.5;
            l.add(new Target(id++, STAR, when[i], when[i] + 28, ltr ? -U - 0.5 : U + 0.5, ltr ? U + 0.5 : -U - 0.5));
        }
        targets = Collections.unmodifiableList(l);
    }

    private long next() {
        rng += 0x9E3779B97F4A7C15L;
        long z = rng;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private double rand() {
        return (next() >>> 11) * 0x1.0p-53;
    }

    private int nextInt(int n) {
        return (int) Math.floorMod(next(), (long) n);
    }

    public Target get(int id) {
        return id >= 0 && id < targets.size() ? targets.get(id) : null;
    }

    /**
     * ¿A cuál le da un disparo? Rayo desde (ou, ov, ow) en la dirección (du, dv, dw), en coordenadas del mostrador, en el tick t.
     * {@code down} dice cuáles ya fueron derribados. Devuelve el id del blanco más cercano o -1.
     */
    public int hit(double t, double ou, double ov, double ow, double du, double dv, double dw, java.util.function.IntPredicate down) {
        int best = -1;
        double bestS = Double.MAX_VALUE;
        if (dw >= -1e-6) {
            return -1; // no apunta hacia los blancos
        }
        for (Target g : targets) {
            if (!g.alive(t) || down.test(g.id) || g.raise(t) < 0.7) {
                continue;
            }
            double s = (g.w() - ow) / dw;
            if (s <= 0 || s >= bestS) {
                continue;
            }
            double pu = ou + du * s, pv = ov + dv * s;
            if (Math.abs(pu - g.u(t)) <= HALF_W[g.type] && Math.abs(pv - g.v(t)) <= HALF_H[g.type]) {
                best = g.id;
                bestS = s;
            }
        }
        return best;
    }

    /** Premio (x10) para estos puntos; 0 = nada. */
    public static int payoutX10(int score) {
        int x = 0;
        for (int i = 0; i < SCORE_AT.length; i++) {
            if (score >= SCORE_AT[i]) {
                x = SCORE_X10[i];
            }
        }
        return x;
    }

    public static long payout(long bet, int score) {
        return bet * payoutX10(score) / 10;
    }

    public static boolean validBet(int bet) {
        for (int b : BETS) {
            if (b == bet) {
                return true;
            }
        }
        return false;
    }
}
