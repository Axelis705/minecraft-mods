package com.casino.game;

/**
 * La forma de la mesa de blackjack: una media luna de 5 bloques de ancho y 2,5 de fondo. Sin nada de Minecraft (así se puede probar sola).
 * Marco de la mesa: el origen es el centro del bloque del medio del lado recto (el que tiene el bloque-entidad), +x es la derecha de quien
 * juega (mirando la mesa de frente) y +z va hacia los jugadores. El crupier está del lado recto (z negativo); los jugadores, del lado curvo.
 */
public final class BlackjackLayout {
    /** Los 13 bloques que ocupa: (columna, fila). Fila 0 = lado recto del crupier. */
    public static final int[][] CELLS = {
            {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0},
            {-2, 1}, {-1, 1}, {0, 1}, {1, 1}, {2, 1},
            {-1, 2}, {0, 2}, {1, 2}};
    /** Índice del bloque del centro (tiene el bloque-entidad). */
    public static final int MASTER = 2;
    /** Centro de la media luna (en el borde recto) y radio. */
    public static final double CX = 0.0;
    public static final double CZ = -0.5;
    public static final double RADIUS = 2.5;
    public static final int SEATS = BlackjackTable.SEATS;

    private BlackjackLayout() {
    }

    /** Ángulo (grados, desde +x hacia +z) del asiento: el 0 queda a la izquierda de quien mira la mesa, el último a su derecha. */
    public static double seatAngle(int seat) {
        return 155.0 - 32.5 * seat;
    }

    public static double[] polar(double radius, double angleDeg) {
        double a = Math.toRadians(angleDeg);
        return new double[]{CX + radius * Math.cos(a), CZ + radius * Math.sin(a)};
    }

    /** Dentro de la media luna (con un margen opcional hacia adentro). */
    public static boolean inside(double x, double z, double margin) {
        return z >= CZ + margin && Math.hypot(x - CX, z - CZ) <= RADIUS - margin;
    }

    public static int cellIndex(int col, int row) {
        for (int i = 0; i < CELLS.length; i++) {
            if (CELLS[i][0] == col && CELLS[i][1] == row) {
                return i;
            }
        }
        return -1;
    }

    /** El asiento libre más cercano (por ángulo) a un punto; -1 si no hay ninguno libre. */
    public static int nearestSeat(double x, double z, boolean[] free) {
        double ang = Math.toDegrees(Math.atan2(z - CZ, x - CX));
        if (ang < 0) {
            ang = ang < -90 ? 180 : 0; // detrás de la mesa: el extremo más cercano
        }
        int best = -1;
        double bestD = 1e9;
        for (int i = 0; i < SEATS; i++) {
            if (!free[i]) {
                continue;
            }
            double d = Math.abs(seatAngle(i) - ang);
            if (d < bestD) {
                bestD = d;
                best = i;
            }
        }
        return best;
    }
}
