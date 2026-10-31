package com.casino.game;

import java.util.List;

/**
 * Evaluador de manos de póker (la mejor mano de 5 cartas entre 5, 6 o 7), sin nada de Minecraft. Las cartas son 0..51 como en el blackjack:
 * valor = carta % 13 (0 = As, 1 = 2, ... 12 = K) y palo = carta / 13. Una mano se resume en un número: mientras más grande, mejor.
 */
public final class PokerHands {
    public static final int HIGH_CARD = 0, PAIR = 1, TWO_PAIR = 2, TRIPS = 3, STRAIGHT = 4, FLUSH = 5, FULL_HOUSE = 6, QUADS = 7,
            STRAIGHT_FLUSH = 8, ROYAL_FLUSH = 9;
    public static final int CATEGORIES = 10;

    private PokerHands() {
    }

    /** Valor de póker de la carta: 2..14 (el As vale 14). */
    public static int rank(int card) {
        int r = card % 13;
        return r == 0 ? 14 : r + 1;
    }

    public static int suit(int card) {
        return card / 13;
    }

    public static int category(long score) {
        return (int) (score >> 20);
    }

    /** El valor principal de la mano (por ejemplo, el valor del par o de la pareja más alta), 2..14. */
    public static int mainRank(long score) {
        return (int) ((score >> 16) & 0xF) + 1; // se guarda r-1 para que quepa en 4 bits
    }

    /** Mejor mano de 5 cartas entre las dadas (5 a 7 cartas). */
    public static long best(List<Integer> cards) {
        int n = cards.size();
        int[] c = new int[n];
        for (int i = 0; i < n; i++) {
            c[i] = cards.get(i);
        }
        return best(c);
    }

    public static long best(int[] c) {
        int n = c.length;
        long best = -1;
        int[] pick = new int[5];
        for (int a = 0; a < n - 4; a++) {
            for (int b = a + 1; b < n - 3; b++) {
                for (int d = b + 1; d < n - 2; d++) {
                    for (int e = d + 1; e < n - 1; e++) {
                        for (int f = e + 1; f < n; f++) {
                            pick[0] = c[a];
                            pick[1] = c[b];
                            pick[2] = c[d];
                            pick[3] = c[e];
                            pick[4] = c[f];
                            long s = eval5(pick);
                            if (s > best) {
                                best = s;
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    /** Puntaje de exactamente 5 cartas: categoría en los bits altos y después hasta 5 valores de desempate (4 bits cada uno, valor-1). */
    static long eval5(int[] h) {
        int[] r = new int[5];
        boolean flush = true;
        for (int i = 0; i < 5; i++) {
            r[i] = rank(h[i]);
            if (suit(h[i]) != suit(h[0])) {
                flush = false;
            }
        }
        java.util.Arrays.sort(r); // ascendente
        int[] cnt = new int[15];
        for (int v : r) {
            cnt[v]++;
        }
        boolean straight = false;
        int top = r[4];
        if (r[0] + 1 == r[1] && r[1] + 1 == r[2] && r[2] + 1 == r[3] && r[3] + 1 == r[4]) {
            straight = true;
        } else if (r[4] == 14 && r[0] == 2 && r[1] == 3 && r[2] == 4 && r[3] == 5) {
            straight = true; // la rueda: A-2-3-4-5, el As vale 1
            top = 5;
        }
        // grupos ordenados: primero los de más cartas, y entre iguales los de más valor
        int[] ord = new int[5];
        int k = 0;
        for (int size = 4; size >= 1; size--) {
            for (int v = 14; v >= 2; v--) {
                if (cnt[v] == size) {
                    ord[k++] = v;
                }
            }
        }
        int cat;
        if (straight && flush) {
            cat = top == 14 ? ROYAL_FLUSH : STRAIGHT_FLUSH;
        } else if (cnt[ord[0]] == 4) {
            cat = QUADS;
        } else if (cnt[ord[0]] == 3 && cnt[ord[1]] == 2) {
            cat = FULL_HOUSE;
        } else if (flush) {
            cat = FLUSH;
        } else if (straight) {
            cat = STRAIGHT;
        } else if (cnt[ord[0]] == 3) {
            cat = TRIPS;
        } else if (cnt[ord[0]] == 2 && cnt[ord[1]] == 2) {
            cat = TWO_PAIR;
        } else if (cnt[ord[0]] == 2) {
            cat = PAIR;
        } else {
            cat = HIGH_CARD;
        }
        long s = (long) cat << 20;
        if (straight) {
            s |= (long) (top - 1) << 16; // en una escalera solo importa la carta alta
        } else {
            for (int i = 0; i < k; i++) {
                s |= (long) (ord[i] - 1) << (16 - 4 * i);
            }
        }
        return s;
    }

    /** El crupier "califica" con par de 4 o mejor. */
    public static boolean dealerQualifies(long score) {
        int cat = category(score);
        return cat > PAIR || (cat == PAIR && mainRank(score) >= 4);
    }

    /** Cuánto paga la apuesta inicial (además de devolverla) cuando el jugador gana y el crupier calificó. */
    public static int antePayMultiplier(int category) {
        return switch (category) {
            case ROYAL_FLUSH -> 100;
            case STRAIGHT_FLUSH -> 20;
            case QUADS -> 10;
            case FULL_HOUSE -> 3;
            case FLUSH -> 2;
            default -> 1;
        };
    }
}
