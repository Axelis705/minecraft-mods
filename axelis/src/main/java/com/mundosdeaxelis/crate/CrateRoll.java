package com.mundosdeaxelis.crate;

import java.util.Random;

/**
 * Tiempos y movimiento de la ruleta. Es una función pura del tiempo, así la pantalla del que abre y el holograma que ven los
 * demás jugadores siempre coinciden.
 */
public final class CrateRoll {
    private CrateRoll() {
    }

    /** Ticks que gira, ticks que muestra el premio y total. */
    public static final int ROLL = 170, REVEAL = 70, TOTAL = ROLL + REVEAL;
    /** Largo de la lista, posición del premio y posición inicial (centro). */
    public static final int LEN = 72, WIN = 58, START = 4;

    /** Desvío de dónde se detiene dentro de la casilla del premio (como en las cajas de CS:GO). */
    public static double jitter(long seed) {
        return (new Random(seed * 31 + 7).nextDouble() - 0.5) * 0.7;
    }

    private static double ease(double u) {
        u = Math.max(0, Math.min(1, u));
        return 1 - Math.pow(1 - u, 3.4);
    }

    /** Posición (en casillas) de la casilla que está bajo la flecha en el tick {@code el} (con decimales). */
    public static double pos(double el, long seed) {
        double end = WIN + jitter(seed);
        return START + (end - START) * ease(el / ROLL);
    }
}
