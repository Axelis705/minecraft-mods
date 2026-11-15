package com.casino.game;

/**
 * Ritmo del reparto de cartas (blackjack y póker): cuánto tarda una carta en llegar de la mano del crupier a su lugar y cuánto espera
 * entre una carta y la siguiente. Lento a propósito, para que se vea que el crupier toma cada carta del zapato y la desliza hasta ti.
 */
public final class CardTiming {
    /** Ticks entre una carta y la siguiente. */
    public static final int GAP = 13;
    /** Ticks que tarda en deslizarse del zapato a su lugar. */
    public static final int FLY = 15;
    /** Ticks antes de salir en que el crupier toma la carta del zapato. */
    public static final int PICK = 7;
    /** Hasta qué parte del camino la lleva con la mano (después la suelta y se desliza sola). */
    public static final double HOLD = 0.72;

    private CardTiming() {
    }
}
