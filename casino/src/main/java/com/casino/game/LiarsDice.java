package com.casino.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Dados del mentiroso, mano a mano contra el tahúr del saloon (Java puro). Cada uno tiene 5 dados escondidos bajo su cubilete. Por
 * turnos se apuesta "hay al menos N dados de la cara C" (contando los de los dos). Los unos son comodines: cuentan como cualquier
 * cara (por eso no se apuesta a los unos). Cada apuesta nueva debe subir: más dados, o los mismos dados de una cara mayor. En vez
 * de subir puedes gritar "¡Mentiroso!": se destapan los cubiletes y si hay menos dados de los apostados, gana quien gritó; si no,
 * gana quien apostó. Paga x1.9.
 */
public final class LiarsDice {
    public static final int DICE = 5;
    public static final int TOTAL = 2 * DICE;
    public static final int PLAYER = 0, NPC = 1;
    public static final int PAY_X10 = 19;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /** Una apuesta: cantidad y cara (2..6), y quién la hizo. */
    public record Bid(int qty, int face, int who) {
        public int code() {
            return qty * 10 + face;
        }
    }

    public final int[][] dice = new int[2][DICE];
    public final List<Bid> bids = new ArrayList<>();
    public int turn = PLAYER;
    /** -1 mientras se juega; si no, quién ganó. */
    public int winner = -1;
    /** Quién gritó "¡mentiroso!" (o -1). */
    public int caller = -1;
    private long rng;

    public LiarsDice(long seed) {
        rng = seed * 0x2545F4914F6CDD1DL + 0x632BE59BD9B4E019L;
        for (int p = 0; p < 2; p++) {
            for (int i = 0; i < DICE; i++) {
                dice[p][i] = 1 + nextInt(6);
            }
        }
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

    public Bid last() {
        return bids.isEmpty() ? null : bids.get(bids.size() - 1);
    }

    public boolean over() {
        return winner >= 0;
    }

    /** ¿Se puede apostar esto ahora? */
    public boolean legal(int qty, int face) {
        if (face < 2 || face > 6 || qty < 1 || qty > TOTAL) {
            return false;
        }
        Bid b = last();
        return b == null || qty > b.qty() || (qty == b.qty() && face > b.face());
    }

    public boolean bid(int who, int qty, int face) {
        if (over() || who != turn || !legal(qty, face)) {
            return false;
        }
        bids.add(new Bid(qty, face, who));
        turn = 1 - who;
        return true;
    }

    /** Dados de esa cara (contando comodines) entre los dos. */
    public int count(int face) {
        int n = 0;
        for (int[] hand : dice) {
            for (int d : hand) {
                if (d == face || d == 1) {
                    n++;
                }
            }
        }
        return n;
    }

    public static int count(int[] hand, int face) {
        int n = 0;
        for (int d : hand) {
            if (d == face || d == 1) {
                n++;
            }
        }
        return n;
    }

    /** "¡Mentiroso!" de quien tiene el turno. */
    public boolean call(int who) {
        Bid b = last();
        if (over() || who != turn || b == null) {
            return false;
        }
        caller = who;
        winner = count(b.face()) >= b.qty() ? b.who() : who;
        return true;
    }

    // ------------------------------------------------------------------ el tahúr

    /** P(X >= k) con X ~ Binomial(n, p). */
    static double atLeast(int n, double p, int k) {
        if (k <= 0) {
            return 1;
        }
        if (k > n) {
            return 0;
        }
        double sum = 0;
        for (int x = k; x <= n; x++) {
            sum += choose(n, x) * Math.pow(p, x) * Math.pow(1 - p, n - x);
        }
        return sum;
    }

    private static double choose(int n, int k) {
        double r = 1;
        for (int i = 1; i <= k; i++) {
            r = r * (n - k + i) / i;
        }
        return r;
    }

    /** Qué tan probable es que la apuesta sea cierta, viendo solo mis dados. */
    public static double chance(int[] mine, int qty, int face) {
        return atLeast(DICE, 1.0 / 3.0, qty - count(mine, face));
    }

    /**
     * Juega el turno de alguien con la estrategia del tahúr: grita si la apuesta de enfrente es poco creíble; si no, sube a la apuesta
     * más creíble (con un poco de farol). {@code bluff} 0..1 = qué tanto se arriesga. Devuelve true si gritó.
     */
    public boolean think(int who, double callBelow, double bluff) {
        int[] mine = dice[who];
        Bid b = last();
        if (b != null) {
            double p = chance(mine, b.qty(), b.face());
            // un poco de duda extra si la apuesta ya es alta
            if (p < callBelow || b.qty() >= TOTAL) {
                call(who);
                return true;
            }
        }
        // buscar la mejor subida: cada cara con la cantidad mínima legal y una más
        int bestQ = -1, bestF = -1;
        double bestScore = -1;
        for (int f = 2; f <= 6; f++) {
            int q0 = b == null ? 1 : (f > b.face() ? b.qty() : b.qty() + 1);
            for (int q = q0; q <= Math.min(TOTAL, q0 + 1); q++) {
                if (!legal(q, f)) {
                    continue;
                }
                double p = chance(mine, q, f);
                // farol: a veces apuesta a lo que no tiene, para despistar
                double score = p + (rand() < bluff ? rand() * 0.35 : 0) - (q - q0) * 0.08;
                if (score > bestScore) {
                    bestScore = score;
                    bestQ = q;
                    bestF = f;
                }
            }
        }
        if (bestQ < 0) {
            call(who);
            return true;
        }
        // si ninguna subida es creíble y hay algo que dudar, mejor gritar
        if (b != null && chance(mine, bestQ, bestF) < 0.25 && chance(mine, b.qty(), b.face()) < 0.6) {
            call(who);
            return true;
        }
        bid(who, bestQ, bestF);
        return false;
    }

    /** Parámetros del tahúr del saloon. */
    public static final double NPC_CALL = 0.38, NPC_BLUFF = 0.18;

    public static long payout(long bet) {
        return bet * PAY_X10 / 10;
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
