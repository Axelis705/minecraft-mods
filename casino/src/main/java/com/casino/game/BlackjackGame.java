package com.casino.game;

import java.util.List;

/**
 * Cartas y cuentas del blackjack, sin nada de Minecraft (así se puede probar sola). La partida completa está en BlackjackTable.
 */
public final class BlackjackGame {
    public enum Result { NONE, BLACKJACK, WIN, PUSH, LOSE, BUST }

    public static final int[] BETS = {10, 20, 50, 100, 250, 500};

    public static boolean validBet(int b) {
        for (int x : BETS) {
            if (x == b) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ cartas: 0..51, valor = carta % 13 (0 = As, 9..12 = 10, J, Q, K)

    public static int rank(int card) {
        return card % 13;
    }

    public static int suit(int card) {
        return card / 13;
    }

    /** Valor de una carta para comparar al dividir (todas las de 10 valen igual). */
    public static int value(int card) {
        return pts(card);
    }

    private static int pts(int card) {
        int r = rank(card);
        return r == 0 ? 11 : Math.min(10, r + 1);
    }

    /** Total de una mano (cuenta los ases como 11 mientras no se pase). */
    public static int total(List<Integer> hand) {
        int t = 0;
        int aces = 0;
        for (int c : hand) {
            t += pts(c);
            if (rank(c) == 0) {
                aces++;
            }
        }
        while (t > 21 && aces > 0) {
            t -= 10;
            aces--;
        }
        return t;
    }

    /** Mano blanda: tiene un as contado como 11. */
    public static boolean soft(List<Integer> hand) {
        int t = 0;
        int aces = 0;
        for (int c : hand) {
            t += pts(c);
            if (rank(c) == 0) {
                aces++;
            }
        }
        while (t > 21 && aces > 0) {
            t -= 10;
            aces--;
        }
        return aces > 0;
    }

    public static boolean isBlackjack(List<Integer> hand) {
        return hand.size() == 2 && total(hand) == 21;
    }
}
