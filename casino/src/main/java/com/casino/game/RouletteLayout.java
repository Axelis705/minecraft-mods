package com.casino.game;

/**
 * La forma de la mesa de ruleta (7 bloques de largo por 3 de fondo) y dónde cae cada cosa. Sin nada de Minecraft.
 * Marco de la mesa: el origen es el centro del bloque del medio, +x es la derecha de quien juega (mirando la mesa de frente) y +z va hacia
 * los jugadores. A la izquierda está la rueda (3x3 bloques); a la derecha, el paño de apuestas (4x3). El crupier está detrás de la rueda.
 */
public final class RouletteLayout {
    /** Los 21 bloques que ocupa: (columna, fila). La fila -1 es el lado del crupier. */
    public static final int[][] CELLS = new int[21][];
    /** Índice del bloque del centro (tiene el bloque-entidad). */
    public static final int MASTER;

    static {
        int k = 0;
        int master = 0;
        for (int row = -1; row <= 1; row++) {
            for (int col = -3; col <= 3; col++) {
                if (col == 0 && row == 0) {
                    master = k;
                }
                CELLS[k++] = new int[]{col, row};
            }
        }
        MASTER = master;
    }

    /** Dónde se para el crupier (columna, fila) respecto del bloque del medio: detrás de la rueda. */
    public static final int DEALER_COL = -2;
    public static final int DEALER_ROW = -2;

    // ---- mesa
    public static final double HALF_LEN = 3.5;
    public static final double HALF_DEP = 1.5;
    /** Alto del tablero (12/16) y del fieltro. */
    public static final double Y_TOP = 12.0 / 16.0;

    // ---- rueda (centro y radios, en metros)
    public static final double WHEEL_X = -2.0;
    public static final double WHEEL_Z = 0.0;
    public static final double R_OUTER = 1.43;     // borde de madera
    public static final double R_TRACK_OUT = 1.27; // pista de la bola: borde de afuera
    public static final double R_TRACK_IN = 1.03;  // pista: borde de adentro
    public static final double R_TRACK_MID = 1.15; // por donde corre la bola
    public static final double R_WHEEL = 1.00;     // borde de la rueda giratoria
    public static final double R_NUMBERS = 0.90;   // donde van los números
    public static final double R_POCKET = 0.80;    // centro de las casillas
    public static final double R_POCKET_IN = 0.62; // fondo de las casillas
    public static final double R_CONE = 0.60;      // base del cono central

    // ---- paño
    private static final double FELT_X0 = -0.30;
    private static final double FELT_X1 = 3.35;
    private static final double[] ROW_Z = {-1.075, -0.625, -0.175, 0.275, 0.675, 1.075};

    private RouletteLayout() {
    }

    public static double feltX(double gx) {
        return FELT_X0 + (FELT_X1 - FELT_X0) * gx / RouletteBets.COLS;
    }

    public static double feltZ(double gy) {
        int i = (int) Math.floor(gy);
        i = Math.max(0, Math.min(ROW_Z.length - 2, i));
        return ROW_Z[i] + (ROW_Z[i + 1] - ROW_Z[i]) * (gy - i);
    }

    public static double feltLeft() {
        return FELT_X0;
    }

    public static double feltRight() {
        return FELT_X1;
    }

    public static double feltTop() {
        return ROW_Z[0];
    }

    public static double feltBottom() {
        return ROW_Z[ROW_Z.length - 1];
    }

    /** Qué casilla de apuesta hay bajo un punto de la mesa (x, z en el marco de la mesa), o -1. Sirve para pruebas de la geometría. */
    public static int spotAt(double x, double z) {
        double gx = (x - FELT_X0) / (FELT_X1 - FELT_X0) * RouletteBets.COLS;
        double gy = -1;
        for (int i = 0; i < ROW_Z.length - 1; i++) {
            if (z >= ROW_Z[i] && z < ROW_Z[i + 1]) {
                gy = i + (z - ROW_Z[i]) / (ROW_Z[i + 1] - ROW_Z[i]);
            }
        }
        return gy < 0 ? -1 : RouletteBets.hit(gx, gy);
    }

    public static int cellIndex(int col, int row) {
        for (int i = 0; i < CELLS.length; i++) {
            if (CELLS[i][0] == col && CELLS[i][1] == row) {
                return i;
            }
        }
        return -1;
    }
}
