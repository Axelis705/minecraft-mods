package com.casino.game;

/**
 * Reglas y medidas del Plinko, sin nada de Minecraft (así se puede probar sola y el cliente y el servidor usan exactamente lo mismo).
 *
 * La bola baja por {@link #ROWS} filas de clavijas; en cada una cae a la izquierda o a la derecha con la misma probabilidad (50%).
 * El camino es un número de {@link #ROWS} bits (1 = derecha): el casillero final es la cantidad de unos. Los casilleros del centro son
 * los más probables y los de las orillas los que más pagan.
 */
public final class PlinkoLayout {
    public static final int ROWS = 10;
    public static final int BUCKETS = ROWS + 1;
    public static final int RISKS = 3;
    /** Apuestas permitidas (en fichas). */
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /**
     * Multiplicadores en décimas (80 = x8,0) de la orilla al centro: casilleros 0..5; el otro lado es igual (espejo).
     * Devuelven entre 95% y 97% de lo apostado en promedio.
     */
    private static final int[][] HALF = {
            {80, 30, 15, 11, 9, 5},    // bajo
            {220, 55, 22, 13, 6, 3},   // medio
            {550, 100, 30, 10, 3, 2}   // alto
    };

    // ---- medidas del tablero (en bloques, relativas al centro de la mesa; y se mide desde el suelo)
    public static final double DX = 0.26;
    public static final double DY = 0.20;
    public static final double PEG_Y0 = 3.20;
    public static final double START_Y = 3.72;
    public static final double BUCKET_Y = 0.85;
    /** Ticks que tarda la bola en llegar a la primera clavija y entre una clavija y la siguiente. */
    public static final int T_START = 6;
    public static final int T_ROW = 7;
    /** Tick (desde que se suelta) en el que la bola cae en su casillero y se paga. */
    public static final int LAND_TICKS = T_START + T_ROW * ROWS;

    private PlinkoLayout() {
    }

    public static boolean validBet(int bet) {
        for (int b : BETS) {
            if (b == bet) {
                return true;
            }
        }
        return false;
    }

    public static int bucketOf(int path) {
        return Integer.bitCount(path & ((1 << ROWS) - 1));
    }

    /** Multiplicador (en décimas) de un casillero con un nivel de riesgo. */
    public static int mult10(int risk, int bucket) {
        int i = bucket <= ROWS / 2 ? bucket : ROWS - bucket;
        return HALF[risk][i];
    }

    /** Fichas que paga una apuesta (redondeado al más cercano). */
    public static int payout(int bet, int mult10) {
        return (int) (((long) bet * mult10 + 5) / 10);
    }

    public static String format(int mult10) {
        return mult10 % 10 == 0 ? Integer.toString(mult10 / 10) : (mult10 / 10) + "." + (mult10 % 10);
    }

    /** Color (RGB) del casillero según lo que paga. */
    public static int colorFor(int mult10) {
        if (mult10 >= 500) {
            return 0xFF2D95;
        }
        if (mult10 >= 100) {
            return 0xFF4040;
        }
        if (mult10 >= 30) {
            return 0xFF8C1A;
        }
        if (mult10 >= 15) {
            return 0xFFD21F;
        }
        if (mult10 >= 10) {
            return 0x7CDB4A;
        }
        return 0x3FA9F5;
    }

    /** Color de la bola según el riesgo. */
    public static int ballColor(int risk) {
        return switch (risk) {
            case 0 -> 0x45E6D0;
            case 2 -> 0xFF4A4A;
            default -> 0xFFD23C;
        };
    }

    // ------------------------------------------------------------------ trayectoria de la bola

    /** Cuántas veces cayó a la derecha en las primeras k filas. */
    public static int rights(int path, int k) {
        return Integer.bitCount(path & ((1 << k) - 1));
    }

    public static double stageX(int path, int k) {
        return (rights(path, k) - k / 2.0) * DX;
    }

    /** Altura de la bola cuando toca la fila k de clavijas (k = ROWS es el casillero). */
    public static double stageY(int k) {
        return k >= ROWS ? BUCKET_Y : PEG_Y0 - k * DY + 0.09;
    }

    /** Posición (x, y) de la bola {@code t} ticks después de soltarla (t puede tener decimales). */
    public static double[] position(int path, double t) {
        if (t <= 0) {
            return new double[]{0, START_Y};
        }
        if (t < T_START) {
            double f = t / T_START;
            return new double[]{0, START_Y + (stageY(0) - START_Y) * f * f};
        }
        if (t >= LAND_TICKS) {
            return new double[]{stageX(path, ROWS), BUCKET_Y};
        }
        int k = Math.min(ROWS - 1, (int) ((t - T_START) / T_ROW));
        double f = (t - T_START - k * (double) T_ROW) / T_ROW;
        double x0 = stageX(path, k);
        double x1 = stageX(path, k + 1);
        // sale rebotando de lado rápido y luego cae: así rodea la clavija sin atravesarla
        double sm = 1 - (1 - f) * (1 - f) * (1 - f);
        double y0 = stageY(k);
        double y1 = stageY(k + 1);
        double bump = (k == ROWS - 1 ? 0.03 : 0.06) * Math.sin(Math.PI * f);
        return new double[]{x0 + (x1 - x0) * sm, y0 + (y1 - y0) * f * f + bump};
    }

    /** Última clavija tocada a los t ticks (-1 = todavía ninguna). */
    public static int pegsHit(double t) {
        if (t < T_START) {
            return -1;
        }
        return Math.min(ROWS - 1, (int) ((t - T_START) / T_ROW));
    }
}
