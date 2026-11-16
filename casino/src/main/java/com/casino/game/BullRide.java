package com.casino.game;

/**
 * Toro mecánico: el equilibrio del jinete (Java puro, sin Minecraft, para probarlo con jugadores simulados).
 * <p>
 * {@link #lean} es cuánto se va cayendo el jinete: 0 = bien sentado, -1 / +1 = se cae por la izquierda / derecha. El toro da
 * jalones, giros y reparos que lo empujan; además estar inclinado tiende a inclinarte más (como un péndulo de cabeza). El jugador
 * empuja con A (izquierda, -1) y D (derecha, +1). Cuanto más tiempo aguantes, más paga; el toro se pone más bravo con el tiempo.
 */
public final class BullRide {
    public static final int TPS = 20;
    /** Cuenta regresiva antes de que el toro arranque (3, 2, 1, ¡arre!). */
    public static final int COUNTDOWN = 60;
    /** Segundos que hay que aguantar para cada premio (en ticks) y el premio (veces la apuesta, x10). */
    public static final int[] TIER_TICKS = {4 * TPS, 7 * TPS, 10 * TPS, 14 * TPS, 18 * TPS, 22 * TPS};
    public static final int[] TIER_X10 = {10, 15, 25, 40, 60, 100};
    /** Al llegar aquí el toro se rinde: ¡domado! */
    public static final int MAX_TICKS = 22 * TPS;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /** Tipos de movimiento del toro. */
    public static final int MOVE_NONE = 0, MOVE_JERK = 1, MOVE_SPIN = 2, MOVE_REAR = 3;

    // ---- física (calibrada con BullTest: un jinete atento a veces pasa los 8 s; domarlo es casi imposible)
    private static final double DT = 1.0 / TPS;
    private static final double PUSH = 5.0;      // fuerza de las teclas
    private static final double DAMP = 1.6;      // freno
    private static final double UNSTABLE = 5.0;  // cuánto te jala la inclinación
    private static final double NOISE = 0.6;    // sacudidas pequeñas
    private static final double IGROW = 9.0;   // cada cuántos segundos el toro suma una "bravura"

    public double lean, vel;
    /** Para dibujar: inclinación lateral del toro, cabeceo y giro (grados). */
    public double roll, pitch, spin;
    /** Ticks desde que arrancó (sin contar la cuenta regresiva). */
    public int t;
    public int move = MOVE_NONE;
    private int moveLeft;
    private int moveLen;
    private double moveDir;
    private double spinFrom, spinTo;
    private int nextMove;
    private double jerk;
    private long rng;

    public BullRide(long seed) {
        this.rng = seed ^ 0x5DEECE66DL;
        nextMove = 14 + nextInt(10);
    }

    // ------------------------------------------------------------------ azar propio (igual en todas partes)

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

    private double gauss() {
        double u = Math.max(1e-9, rand()), v = rand();
        return Math.sqrt(-2 * Math.log(u)) * Math.cos(2 * Math.PI * v);
    }

    // ------------------------------------------------------------------ reglas

    /** Qué tan bravo está el toro (1 al empezar, casi 4 a los 20 s). */
    public static double intensity(int ticks) {
        return 1.0 + ticks / (double) TPS / IGROW;
    }

    /** Premio que toca después de aguantar estos ticks (x10; 0 = nada). */
    public static int payoutX10(int ticks) {
        int x = 0;
        for (int i = 0; i < TIER_TICKS.length; i++) {
            if (ticks >= TIER_TICKS[i]) {
                x = TIER_X10[i];
            }
        }
        return x;
    }

    /** Índice del premio alcanzado (-1 = ninguno). */
    public static int tier(int ticks) {
        int k = -1;
        for (int i = 0; i < TIER_TICKS.length; i++) {
            if (ticks >= TIER_TICKS[i]) {
                k = i;
            }
        }
        return k;
    }

    public static long payout(long bet, int ticks) {
        return bet * payoutX10(ticks) / 10;
    }

    public static boolean validBet(int bet) {
        for (int b : BETS) {
            if (b == bet) {
                return true;
            }
        }
        return false;
    }

    /**
     * Un tick de la montada. input: -1 (A), 0, +1 (D). Devuelve false si el jinete salió volando.
     */
    public boolean step(int input) {
        double in = intensity(t);
        // movimientos del toro
        if (moveLeft <= 0 && t >= nextMove) {
            int r = nextInt(100);
            moveDir = rand() < 0.5 ? -1 : 1;
            if (r < 58) {
                move = MOVE_JERK;
                moveLen = 6;
                vel += moveDir * (1.0 + 0.6 * rand()) * (0.75 + 0.35 * in);
                jerk = -moveDir;
            } else if (r < 84) {
                move = MOVE_SPIN;
                moveLen = 16;
                spinFrom = spin;
                double amp = 25 + 25 * rand();
                spinTo = Math.max(-55, Math.min(55, spin * 0.3 + moveDir * amp));
                vel += moveDir * (0.25 + 0.2 * rand()) * (0.7 + 0.3 * in);
            } else {
                move = MOVE_REAR;
                moveLen = 18;
            }
            moveLeft = moveLen;
            nextMove = t + moveLen + (int) Math.max(4, (8 + nextInt(16)) / Math.sqrt(in));
        }
        if (moveLeft > 0) {
            moveLeft--;
            if (move == MOVE_SPIN) {
                double k = 1 - (double) moveLeft / moveLen;
                k = k * k * (3 - 2 * k);
                spin = spinFrom + (spinTo - spinFrom) * k;
            } else if (move == MOVE_REAR) {
                vel += gauss() * 0.10 * in;
            }
            if (moveLeft == 0) {
                move = MOVE_NONE;
            }
        }
        // física del jinete
        int u = Integer.signum(input);
        vel += (UNSTABLE * (0.85 + 0.15 * in) * lean) * DT;
        vel += gauss() * NOISE * Math.sqrt(DT) * in;
        vel += u * PUSH * DT;
        vel *= 1 - DAMP * DT;
        lean += vel * DT;
        // dibujo
        jerk *= 0.82;
        roll = jerk * 22 + lean * -6;
        double buck = Math.sin(t * 0.55) * (3 + 2.5 * in);
        double rear = move == MOVE_REAR ? Math.sin(Math.PI * (1 - (double) moveLeft / moveLen)) * 24 : 0;
        pitch = buck + rear;
        if (move != MOVE_SPIN) {
            spin *= 0.985;
        }
        t++;
        return Math.abs(lean) < 1.0;
    }

    /** ¿Ya aguantó todo? (el toro se rinde) */
    public boolean tamed() {
        return t >= MAX_TICKS;
    }
}
