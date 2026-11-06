package com.showdeaxelis.game;

public enum Phase {
    /** Armando equipos; todavia no empieza el show. */
    LOBBY,
    /** El show esta en marcha pero no hay una ronda en curso. */
    IDLE,
    /** Ronda de 100 mexicanos dijeron. */
    R100,
    /** Ronda de trivia. */
    TRIVIA,
    /** El Impostor: pistas, charla y votacion. */
    IMPOSTOR,
    /** Adivina la palabra sin decir las prohibidas. */
    TABOO,
    /** Terminado: se muestran los ganadores. */
    FINAL
}
