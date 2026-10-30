package com.casino.game;

/**
 * Reglas de la tragamonedas, sin nada de Minecraft (se puede probar sola; cliente y servidor usan exactamente lo mismo).
 *
 * Tres carretes, cada uno con una cinta de {@link #STRIP} símbolos. Se ven 3 filas por carrete. Hay {@link #LINES} líneas de pago
 * (3 filas y las 2 diagonales) que juegan siempre; la apuesta se reparte en partes iguales entre las líneas (apuesta / 5 por línea).
 * El servidor sortea en qué posición se detiene cada carrete (todas igual de probables); el cliente solo anima hasta ahí.
 */
public final class SlotLayout {
    public static final int REELS = 3;
    public static final int ROWS = 3;
    public static final int STRIP = 24;
    public static final int LINES = 5;

    public static final int CHERRY = 0, LEMON = 1, BELL = 2, GEM = 3, STAR = 4, SEVEN = 5, CROWN = 6;
    public static final int SYMBOLS = 7;

    /** Apuestas permitidas (en fichas); todas se pueden dividir entre las 5 líneas. */
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /** Cintas de cada carrete (el mismo reparto de símbolos, en distinto orden). */
    public static final int[][] STRIPS = {
            {4, 1, 6, 3, 4, 2, 0, 1, 1, 0, 0, 1, 0, 3, 2, 1, 5, 2, 2, 0, 1, 0, 3, 0},
            {2, 1, 0, 1, 2, 0, 0, 3, 3, 4, 2, 2, 4, 1, 3, 6, 0, 1, 1, 0, 1, 5, 0, 0},
            {0, 2, 4, 2, 0, 0, 3, 1, 1, 2, 6, 1, 0, 1, 4, 5, 0, 0, 2, 1, 0, 3, 3, 1}
    };

    /** Qué fila (0 = arriba) mira cada línea en cada carrete. */
    public static final int[][] LINE_ROWS = {{0, 0, 0}, {1, 1, 1}, {2, 2, 2}, {0, 1, 2}, {2, 1, 0}};

    /** Pago de tres iguales, en veces lo apostado en esa línea. La corona paga fichas Y el premio gordo. */
    private static final int[] PAY3 = {6, 12, 32, 80, 200, 800, 120};
    /** Dos cerezas seguidas empezando por la izquierda. */
    private static final int PAY_CHERRY2 = 2;

    // ---- tiempos (ticks desde que se tira de la palanca)
    /** Tick en que se detiene cada carrete. */
    public static final int[] STOP_TICK = {36, 50, 64};
    /** Tick en que se paga (un poco después de que pare el último). */
    public static final int END_TICK = 72;
    /** Símbolos por tick que avanza un carrete mientras gira. */
    public static final double SPEED = 0.85;

    private SlotLayout() {
    }

    public static boolean validBet(int bet) {
        for (int b : BETS) {
            if (b == bet) {
                return true;
            }
        }
        return false;
    }

    public static int mod(int i) {
        return ((i % STRIP) + STRIP) % STRIP;
    }

    /** Símbolo que se ve en un carrete, en una fila (0 arriba, 2 abajo), cuando está detenido en {@code stop}. */
    public static int symbolAt(int reel, int stop, int row) {
        return STRIPS[reel][mod(stop + row - 1)];
    }

    public static final class Result {
        /** grid[carrete][fila] */
        public final int[][] grid = new int[REELS][ROWS];
        /** Suma de pagos de todas las líneas, en veces lo apostado en UNA línea. */
        public int units;
        /** Cuáles líneas ganaron (un bit por línea). */
        public int lineMask;
        /** Hay tres coronas en alguna línea. */
        public boolean jackpot;
        /** Mejor símbolo de tres iguales (-1 si no hubo); sirve para elegir el sonido y el aviso. */
        public int best = -1;
    }

    public static Result evaluate(int[] stops) {
        Result r = new Result();
        for (int c = 0; c < REELS; c++) {
            for (int w = 0; w < ROWS; w++) {
                r.grid[c][w] = symbolAt(c, stops[c], w);
            }
        }
        for (int l = 0; l < LINES; l++) {
            int a = r.grid[0][LINE_ROWS[l][0]];
            int b = r.grid[1][LINE_ROWS[l][1]];
            int c = r.grid[2][LINE_ROWS[l][2]];
            if (a == b && b == c) {
                r.units += PAY3[a];
                r.lineMask |= 1 << l;
                if (a == CROWN) {
                    r.jackpot = true;
                }
                if (a > r.best) {
                    r.best = a;
                }
            } else if (a == CHERRY && b == CHERRY) {
                r.units += PAY_CHERRY2;
                r.lineMask |= 1 << l;
            }
        }
        return r;
    }

    /** Fichas que se pagan por un resultado. */
    public static int payout(int bet, int units) {
        return units * (bet / LINES);
    }

    /** Retorno teórico (fracción de lo apostado que se devuelve en fichas, sin contar el premio gordo), contando todas las posiciones. */
    public static double exactRtp() {
        long total = 0;
        int[] s = new int[REELS];
        for (s[0] = 0; s[0] < STRIP; s[0]++) {
            for (s[1] = 0; s[1] < STRIP; s[1]++) {
                for (s[2] = 0; s[2] < STRIP; s[2]++) {
                    total += evaluate(s).units;
                }
            }
        }
        return total / (double) LINES / (STRIP * STRIP * STRIP);
    }

    /**
     * Posición continua del carrete {@code reel} (en símbolos) a los {@code t} ticks de empezar: gira hacia abajo y frena justo en
     * {@code stop}. El valor se usa módulo {@link #STRIP}; a partir de {@link #STOP_TICK} queda exactamente en {@code stop}
     * (con un pequeño rebote al frenar).
     */
    public static double offset(int reel, int stop, double t) {
        double x = STOP_TICK[reel] - t;
        if (x <= 0) {
            double k = -x;
            // rebote amortiguado de 6 ticks
            return k < 6 ? stop + 0.10 * Math.exp(-k * 0.7) * Math.sin(k * 1.9) : stop;
        }
        // últimos 7 ticks: frenado suave (la velocidad baja de SPEED a 0 sin saltos); antes, velocidad constante
        double c = SPEED * 7.0 / 2.0;
        double f = x > 7 ? SPEED * (x - 7) + c : c * (x / 7.0) * (x / 7.0);
        return stop + f;
    }
}
