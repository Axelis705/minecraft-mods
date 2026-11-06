package com.showdeaxelis.game;

/** Algo que paso en el show y que el servidor debe anunciar (sonido, texto del presentador, efecto). */
public record Ev(String type, int side, String player, String text, int a, int b) {
    public static Ev of(String type) {
        return new Ev(type, -1, "", "", 0, 0);
    }
}
