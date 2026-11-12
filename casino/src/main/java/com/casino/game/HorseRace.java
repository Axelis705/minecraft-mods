package com.casino.game;

import java.util.Random;

/**
 * Una carrera de caballos completa a partir de una semilla: los 6 caballos (nombre, pelaje, sedas del jinete), su probabilidad de
 * ganar, las cuotas, el orden de llegada y cómo corre cada uno. El servidor y los clientes la calculan igual con la misma semilla,
 * así que solo se manda la semilla. Lógica pura (sin Minecraft): se prueba sola.
 */
public final class HorseRace {
    public static final int N = 6;
    /** Nombres posibles (claves casino.horse.name.0 ... ). */
    public static final int NAMES = 40;
    public static final int COATS = 6;
    public static final int SILK_COLORS = 12;
    public static final int PATTERNS = 4;

    /** Fases (en ticks): apuestas, caballos en los cajones con cuenta regresiva, carrera, resultados. */
    public static final int BET_TICKS = 45 * 20;
    public static final int GATE_TICKS = 5 * 20;
    public static final int RESULT_TICKS = 12 * 20;

    /** Cuotas posibles (fraccionarias, como en las apuestas reales): paga apuesta × num / den, más lo apostado. */
    public static final int[][] LADDER = {{1, 2}, {4, 5}, {1, 1}, {6, 4}, {2, 1}, {5, 2}, {3, 1}, {4, 1}, {5, 1}, {6, 1}, {8, 1},
            {10, 1}, {12, 1}, {15, 1}, {20, 1}, {25, 1}, {30, 1}};
    /** Cuota justa multiplicada por esto y redondeada hacia abajo a la escalera: la casa se queda con ~13 % (como en un hipódromo). */
    public static final double HOUSE = 1.0;

    public final long seed;
    public final int[] name = new int[N];
    public final int[] coat = new int[N];
    public final int[] silk1 = new int[N];
    public final int[] silk2 = new int[N];
    public final int[] pattern = new int[N];
    /** Probabilidad real de ganar de cada caballo. */
    public final double[] p = new double[N];
    public final int[] oddsNum = new int[N];
    public final int[] oddsDen = new int[N];
    /** Estrellas de "forma" (1-5) para mostrar. */
    public final int[] form = new int[N];
    /** order[0] es el ganador, order[1] el segundo... */
    public final int[] order = new int[N];
    /** Puesto de cada caballo (0 = primero). */
    public final int[] place = new int[N];
    /** Segundos que tarda cada caballo en llegar a la meta. */
    public final double[] finish = new double[N];
    private final double[] curveC = new double[N];
    private final double[] curveA = new double[N];
    private final double[] curveF = new double[N];
    private final double[] curvePh = new double[N];

    private HorseRace(long seed) {
        this.seed = seed;
        Random r = new Random(seed * 0x9E3779B97F4A7C15L + 12345L);
        // nombres distintos
        boolean[] used = new boolean[NAMES];
        for (int i = 0; i < N; i++) {
            int k;
            do {
                k = r.nextInt(NAMES);
            } while (used[k]);
            used[k] = true;
            name[i] = k;
            coat[i] = r.nextInt(COATS);
            silk1[i] = r.nextInt(SILK_COLORS);
            int s2;
            do {
                s2 = r.nextInt(SILK_COLORS);
            } while (s2 == silk1[i]);
            silk2[i] = s2;
            pattern[i] = r.nextInt(PATTERNS);
        }
        // fuerza de cada caballo: unas carreras tienen un gran favorito y otras están parejas
        double spread = 0.6 + r.nextDouble() * 0.9;
        double[] rating = new double[N];
        double sum = 0;
        for (int i = 0; i < N; i++) {
            rating[i] = Math.exp(r.nextGaussian() * spread);
            sum += rating[i];
        }
        for (int i = 0; i < N; i++) {
            p[i] = rating[i] / sum;
            double fair = (1 - p[i]) / p[i];
            double offer = fair * HOUSE;
            int pick = 0;
            for (int k = 0; k < LADDER.length; k++) {
                if ((double) LADDER[k][0] / LADDER[k][1] <= offer) {
                    pick = k;
                }
            }
            oddsNum[i] = LADDER[pick][0];
            oddsDen[i] = LADDER[pick][1];
            form[i] = Math.max(1, Math.min(5, (int) Math.round(1 + p[i] * 12 + r.nextGaussian() * 0.6)));
        }
        // orden de llegada: se saca el ganador según su probabilidad, luego el segundo entre los que quedan, etc.
        boolean[] out = new boolean[N];
        for (int pos = 0; pos < N; pos++) {
            double left = 0;
            for (int i = 0; i < N; i++) {
                if (!out[i]) {
                    left += p[i];
                }
            }
            double x = r.nextDouble() * left;
            int pickH = -1;
            for (int i = 0; i < N; i++) {
                if (out[i]) {
                    continue;
                }
                pickH = i;
                x -= p[i];
                if (x <= 0) {
                    break;
                }
            }
            out[pickH] = true;
            order[pos] = pickH;
            place[pickH] = pos;
        }
        // tiempos de llegada: el ganador entre 19 y 23 s; los demás llegan detrás (a veces por una nariz)
        double t = 19 + r.nextDouble() * 4;
        for (int pos = 0; pos < N; pos++) {
            finish[order[pos]] = t;
            double gap = r.nextDouble() < 0.3 ? 0.05 + r.nextDouble() * 0.15 : 0.25 + r.nextDouble() * 0.9;
            t += gap;
        }
        // forma de correr: unos salen rápido y se cansan, otros remontan al final; todos con algo de vaivén
        for (int i = 0; i < N; i++) {
            curveC[i] = (r.nextDouble() * 2 - 1) * 0.35;
            curveA[i] = 0.01 + r.nextDouble() * 0.03;
            curveF[i] = 0.7 + r.nextDouble() * 0.9;
            curvePh[i] = r.nextDouble() * Math.PI * 2;
        }
    }

    public static HorseRace of(long seed) {
        return new HorseRace(seed);
    }

    /**
     * Qué parte del recorrido lleva el caballo i a los t segundos de la salida (0 = cajón, 1 = meta). Siempre avanza; cruza la meta
     * justo en su tiempo de llegada y después sigue un poco más despacio (se frena).
     */
    public double progress(int i, double t) {
        if (t <= 0) {
            return 0;
        }
        double u = t / finish[i];
        if (u >= 1) {
            return 1 + (u - 1) * 0.55;
        }
        return u + curveC[i] * (u * u - u) + curveA[i] * Math.sin(Math.PI * u) * Math.sin(2 * Math.PI * curveF[i] * u + curvePh[i]);
    }

    /** Duración de la carrera en ticks (hasta que llega el último y un poquito más). */
    public int raceTicks() {
        double last = 0;
        for (double f : finish) {
            last = Math.max(last, f);
        }
        return (int) Math.ceil((last + 1.5) * 20);
    }

    /** Cuánto se cobra si gana (lo apostado más la ganancia). */
    public long payout(int horse, long bet) {
        return bet + bet * oddsNum[horse] / oddsDen[horse];
    }

    /** Puesto actual de cada caballo a los t segundos (0 = va primero). */
    public int[] standings(double t) {
        Integer[] idx = new Integer[N];
        for (int i = 0; i < N; i++) {
            idx[i] = i;
        }
        java.util.Arrays.sort(idx, (a, b) -> {
            double pa = progress(a, t);
            double pb = progress(b, t);
            if (pa >= 1 && pb >= 1) {
                return Integer.compare(place[a], place[b]);
            }
            return Double.compare(pb, pa);
        });
        int[] out = new int[N];
        for (int k = 0; k < N; k++) {
            out[idx[k]] = k;
        }
        return out;
    }

    /** Texto de la cuota: "5/1", "4/5" o "PAR" (1/1). */
    public String oddsText(int i) {
        return oddsNum[i] == oddsDen[i] ? "1/1" : oddsNum[i] + "/" + oddsDen[i];
    }
}
