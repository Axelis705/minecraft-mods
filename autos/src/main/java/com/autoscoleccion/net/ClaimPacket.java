package com.autoscoleccion.net;

/** Cliente -> servidor: terminó la animación (entregar el auto) o se cerró la prueba sin terminar. */
public record ClaimPacket(int what) {
    public static final int CLAIM_CAR = 0;
    public static final int CANCEL_GAME = 1;
}
