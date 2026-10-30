package com.casino.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Las reglas del blackjack, sin nada de Minecraft (así se puede probar sola).
 * Reglas de la mesa: baraja infinita (cada carta es al azar), el crupier se planta con todos los 17 (incluso 17 blando),
 * el blackjack natural paga 3 a 2, se puede doblar con las dos primeras cartas, el crupier mira si tiene blackjack antes de que juegues.
 * No hay separar ni seguro (de momento).
 */
public final class BlackjackGame {
    public enum Phase { IDLE, PLAYER, DEALER, DONE }

    public enum Result { NONE, BLACKJACK, WIN, PUSH, LOSE, BUST }

    public static final int[] BETS = {10, 20, 50, 100, 250, 500};

    public final List<Integer> player = new ArrayList<>();
    public final List<Integer> dealer = new ArrayList<>();
    public Phase phase = Phase.IDLE;
    public Result result = Result.NONE;
    public int bet;
    public boolean doubled;
    /** El crupier ya dio vuelta su carta tapada. */
    public boolean holeRevealed;
    /** Fichas que se le devuelven al jugador al terminar (0 si perdió). */
    public int payout;

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

    public int playerTotal() {
        return total(player);
    }

    /** Lo que ve el jugador del crupier: solo la primera carta mientras la otra está tapada. */
    public int dealerShownTotal() {
        if (holeRevealed || dealer.size() < 2) {
            return total(dealer);
        }
        return total(dealer.subList(0, 1));
    }

    // ------------------------------------------------------------------ juego

    private static int draw(Random r) {
        return r.nextInt(52);
    }

    /** Reparte una mano nueva. */
    public void start(Random rnd, int betChips) {
        player.clear();
        dealer.clear();
        bet = betChips;
        doubled = false;
        holeRevealed = false;
        result = Result.NONE;
        payout = 0;
        player.add(draw(rnd));
        dealer.add(draw(rnd));
        player.add(draw(rnd));
        dealer.add(draw(rnd));
        phase = Phase.PLAYER;
        int up = pts(dealer.get(0));
        boolean peek = up >= 10; // As o figura/10: el crupier mira su carta tapada
        if (isBlackjack(player) || (peek && isBlackjack(dealer))) {
            holeRevealed = true;
            finish();
        }
    }

    public boolean canHit() {
        return phase == Phase.PLAYER;
    }

    public boolean canDouble() {
        return phase == Phase.PLAYER && player.size() == 2;
    }

    public void hit(Random rnd) {
        if (!canHit()) {
            return;
        }
        player.add(draw(rnd));
        int t = playerTotal();
        if (t > 21) {
            holeRevealed = true;
            finish();
        } else if (t == 21) {
            stand();
        }
    }

    public void stand() {
        if (phase == Phase.PLAYER) {
            phase = Phase.DEALER;
        }
    }

    /** Dobla la apuesta y recibe una sola carta (el cobro de las fichas extra lo hace quien llama antes). */
    public void doubleDown(Random rnd) {
        if (!canDouble()) {
            return;
        }
        bet *= 2;
        doubled = true;
        player.add(draw(rnd));
        if (playerTotal() > 21) {
            holeRevealed = true;
            finish();
        } else {
            phase = Phase.DEALER;
        }
    }

    /** Un paso del crupier: primero da vuelta su carta, luego pide mientras tenga menos de 17, y al final se cierra la mano. */
    public void dealerStep(Random rnd) {
        if (phase != Phase.DEALER) {
            return;
        }
        if (!holeRevealed) {
            holeRevealed = true;
            return;
        }
        if (total(dealer) < 17) {
            dealer.add(draw(rnd));
            return;
        }
        finish();
    }

    private void finish() {
        int p = playerTotal();
        int d = total(dealer);
        boolean pBJ = isBlackjack(player);
        boolean dBJ = isBlackjack(dealer);
        if (p > 21) {
            result = Result.BUST;
        } else if (pBJ && !dBJ) {
            result = Result.BLACKJACK;
        } else if (dBJ && !pBJ) {
            result = Result.LOSE;
        } else if (d > 21 || p > d) {
            result = Result.WIN;
        } else if (p == d) {
            result = Result.PUSH;
        } else {
            result = Result.LOSE;
        }
        payout = switch (result) {
            case BLACKJACK -> bet * 5 / 2;
            case WIN -> bet * 2;
            case PUSH -> bet;
            default -> 0;
        };
        holeRevealed = true;
        phase = Phase.DONE;
    }

    public boolean isOver() {
        return phase == Phase.DONE;
    }
}
