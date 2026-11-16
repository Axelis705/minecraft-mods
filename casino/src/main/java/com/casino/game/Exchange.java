package com.casino.game;

/**
 * El cambio del cajero entre fichas, monedas arcade, boletos, esmeraldas y diamantes. Todo vale lo mismo en fichas de ida y de vuelta
 * (cambiar y regresar nunca hace perder nada), y se cambia por "lotes" enteros: por ejemplo 8 esmeraldas = 1 diamante, 5 fichas =
 * 1 moneda arcade. Sin nada de Minecraft para poder probarlo aparte ({@code tests/ExchangeTest}).
 */
public final class Exchange {
    public static final int CHIPS = 0;
    public static final int COINS = 1;
    public static final int TICKETS = 2;
    public static final int EMERALD = 3;
    public static final int DIAMOND = 4;
    public static final int COUNT = 5;
    /** Valor de cada uno en fichas. */
    public static final int[] VALUE = {1, 5, 1, 10, 80};
    /** Lo más que se cambia de una vez (en unidades de lo que se entrega o se recibe). */
    public static final int LIMIT = 100_000;

    private Exchange() {
    }

    private static int gcd(int a, int b) {
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    /** Cuántos de {@code from} se entregan en cada lote. */
    public static int give(int from, int to) {
        return VALUE[to] / gcd(VALUE[from], VALUE[to]);
    }

    /** Cuántos de {@code to} se reciben en cada lote. */
    public static int get(int from, int to) {
        return VALUE[from] / gcd(VALUE[from], VALUE[to]);
    }

    public static boolean valid(int from, int to) {
        return from >= 0 && from < COUNT && to >= 0 && to < COUNT && from != to;
    }

    /** Cuántos lotes se pueden hacer teniendo {@code have} de lo que se entrega. */
    public static int maxLots(int from, int to, long have) {
        if (!valid(from, to) || have <= 0) {
            return 0;
        }
        int g = give(from, to);
        long byHave = have / g;
        long byLimit = LIMIT / Math.max(g, get(from, to));
        return (int) Math.max(0, Math.min(byHave, byLimit));
    }
}
