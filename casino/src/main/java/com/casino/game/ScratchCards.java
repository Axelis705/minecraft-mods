package com.casino.game;

import java.util.Random;

/**
 * El rasca y gana "Tesoro", sin nada de Minecraft (así se puede probar solo). Cada boleto tiene 6 casillas con un símbolo cada una; si
 * aparecen 3 símbolos iguales se gana el premio de ese símbolo. El resultado se sortea al comprar el boleto (se guarda en el objeto) y rascar
 * solo lo descubre. Los 8 símbolos van de menor a mayor premio.
 */
public final class ScratchCards {
    public static final int SPOTS = 6;
    public static final int SYMBOLS = 8;
    public static final int PRICE = 10;
    /** Premio (en fichas) de cada símbolo, de menor a mayor. */
    public static final int[] PRIZES = {10, 15, 20, 50, 100, 250, 1000, 5000};
    /** Probabilidad de cada premio, sobre 100000. El resto (72.16%) son boletos sin premio. */
    private static final int[] WEIGHTS = {14000, 3000, 6000, 3000, 1200, 500, 120, 20};
    private static final int TOTAL = 100000;
    /** Cuántas veces puede salir un símbolo en un boleto que no lo gana (a lo sumo 2: así nunca hay tres iguales por casualidad). */
    private static final int MAX_NON_WIN = 2;

    private ScratchCards() {
    }

    /** Qué símbolo aparece 3 o más veces (el de mayor premio si hubiera varios), o -1. */
    public static int winningSymbol(int[] sym) {
        int[] cnt = new int[SYMBOLS];
        for (int s : sym) {
            if (s >= 0 && s < SYMBOLS) {
                cnt[s]++;
            }
        }
        for (int s = SYMBOLS - 1; s >= 0; s--) {
            if (cnt[s] >= 3) {
                return s;
            }
        }
        return -1;
    }

    public static int prizeOf(int[] sym) {
        int w = winningSymbol(sym);
        return w < 0 ? 0 : PRIZES[w];
    }

    /** Sortea un boleto: devuelve los 6 símbolos. */
    public static int[] generate(Random rnd) {
        int roll = rnd.nextInt(TOTAL);
        int win = -1;
        int acc = 0;
        for (int i = 0; i < SYMBOLS; i++) {
            acc += WEIGHTS[i];
            if (roll < acc) {
                win = i;
                break;
            }
        }
        int[] sym = new int[SPOTS];
        java.util.Arrays.fill(sym, -1);
        int[] cnt = new int[SYMBOLS];
        if (win >= 0) {
            int placed = 0;
            while (placed < 3) {
                int p = rnd.nextInt(SPOTS);
                if (sym[p] < 0) {
                    sym[p] = win;
                    placed++;
                }
            }
            cnt[win] = 3;
        }
        for (int p = 0; p < SPOTS; p++) {
            if (sym[p] >= 0) {
                continue;
            }
            int s;
            do {
                s = rnd.nextInt(SYMBOLS);
            } while (s == win || cnt[s] >= MAX_NON_WIN);
            sym[p] = s;
            cnt[s]++;
        }
        return sym;
    }

    /** Retorno esperado (premio medio dividido por el precio). */
    public static double expectedReturn() {
        double e = 0;
        for (int i = 0; i < SYMBOLS; i++) {
            e += WEIGHTS[i] / (double) TOTAL * PRIZES[i];
        }
        return e / PRICE;
    }

    public static double winChance() {
        int w = 0;
        for (int x : WEIGHTS) {
            w += x;
        }
        return w / (double) TOTAL;
    }

    public static boolean validQuantity(int n) {
        return n == 1 || n == 5 || n == 10;
    }
}
