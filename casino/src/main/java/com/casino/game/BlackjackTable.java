package com.casino.game;

import com.casino.game.BlackjackGame.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La partida de una mesa de blackjack con varios asientos y un solo crupier, sin nada de Minecraft (así se puede probar sola).
 * Reglas: baraja infinita (cada carta es al azar), el crupier se planta con todos los 17 (incluso 17 blando), el blackjack natural paga 3 a 2,
 * se puede doblar con las dos primeras cartas, y si el crupier muestra As o 10 mira su carta tapada antes de que juegue nadie.
 * No hay separar ni seguro (de momento).
 *
 * Una ronda: IDLE -> BETTING (se apuesta) -> TURNS (cada jugador en orden) -> DEALER -> RESULT (se muestran resultados) -> IDLE.
 */
public final class BlackjackTable {
    public static final int SEATS = 5;

    public enum Phase { IDLE, BETTING, TURNS, DEALER, RESULT }

    public enum SeatState { EMPTY, BET, PLAYING, STAND, BUST, BLACKJACK }

    public static final class Seat {
        public final List<Integer> cards = new ArrayList<>();
        public int bet;
        public boolean doubled;
        public SeatState state = SeatState.EMPTY;
        public Result result = Result.NONE;
        public int payout;

        void clear() {
            cards.clear();
            bet = 0;
            doubled = false;
            state = SeatState.EMPTY;
            result = Result.NONE;
            payout = 0;
        }

        public int total() {
            return BlackjackGame.total(cards);
        }
    }

    public final Seat[] seats = new Seat[SEATS];
    public final List<Integer> dealer = new ArrayList<>();
    public Phase phase = Phase.IDLE;
    public boolean holeRevealed;
    /** Asiento al que le toca (solo en TURNS). */
    public int current = -1;

    public BlackjackTable() {
        for (int i = 0; i < SEATS; i++) {
            seats[i] = new Seat();
        }
    }

    private static int draw(Random r) {
        return r.nextInt(52);
    }

    public int dealerShownTotal() {
        if (holeRevealed || dealer.size() < 2) {
            return BlackjackGame.total(dealer);
        }
        return BlackjackGame.total(dealer.subList(0, 1));
    }

    public int betCount() {
        int n = 0;
        for (Seat s : seats) {
            if (s.bet > 0) {
                n++;
            }
        }
        return n;
    }

    public boolean canBet(int seat) {
        return (phase == Phase.IDLE || phase == Phase.BETTING) && seat >= 0 && seat < SEATS && seats[seat].state == SeatState.EMPTY;
    }

    /** Un jugador apuesta en un asiento libre (el cobro de las fichas lo hace quien llama antes). */
    public boolean placeBet(int seat, int bet) {
        if (!canBet(seat) || !BlackjackGame.validBet(bet)) {
            return false;
        }
        Seat s = seats[seat];
        s.bet = bet;
        s.state = SeatState.BET;
        phase = Phase.BETTING;
        return true;
    }

    /** Reparte a todos los que apostaron. Si el crupier tiene blackjack la ronda termina al instante. */
    public void deal(Random rnd) {
        if (phase != Phase.BETTING || betCount() == 0) {
            return;
        }
        dealer.clear();
        holeRevealed = false;
        for (Seat s : seats) {
            s.cards.clear();
        }
        for (int round = 0; round < 2; round++) {
            for (Seat s : seats) {
                if (s.bet > 0) {
                    s.cards.add(draw(rnd));
                }
            }
            dealer.add(draw(rnd));
        }
        for (Seat s : seats) {
            if (s.bet > 0) {
                s.state = BlackjackGame.isBlackjack(s.cards) ? SeatState.BLACKJACK : SeatState.PLAYING;
            }
        }
        int up = BlackjackGame.total(dealer.subList(0, 1));
        if (up >= 10 && BlackjackGame.isBlackjack(dealer)) {
            holeRevealed = true;
            settle();
            return;
        }
        phase = Phase.TURNS;
        current = -1;
        advance();
    }

    /** Pasa al siguiente jugador que todavía puede jugar; si no queda ninguno le toca al crupier. */
    private void advance() {
        int from = current;
        if (from >= 0 && seats[from].state == SeatState.PLAYING) {
            return;
        }
        for (int i = from + 1; i < SEATS; i++) {
            if (seats[i].state == SeatState.PLAYING) {
                current = i;
                return;
            }
        }
        current = -1;
        phase = Phase.DEALER;
    }

    private boolean myTurn(int seat) {
        return phase == Phase.TURNS && seat == current && seats[seat].state == SeatState.PLAYING;
    }

    public boolean canDouble(int seat) {
        return myTurn(seat) && seats[seat].cards.size() == 2;
    }

    public void hit(int seat, Random rnd) {
        if (!myTurn(seat)) {
            return;
        }
        Seat s = seats[seat];
        s.cards.add(draw(rnd));
        int t = s.total();
        if (t > 21) {
            s.state = SeatState.BUST;
        } else if (t == 21) {
            s.state = SeatState.STAND;
        }
        advance();
    }

    public void stand(int seat) {
        if (!myTurn(seat)) {
            return;
        }
        seats[seat].state = SeatState.STAND;
        advance();
    }

    /** Dobla la apuesta y recibe una sola carta (el cobro de las fichas extra lo hace quien llama antes). */
    public void doubleDown(int seat, Random rnd) {
        if (!canDouble(seat)) {
            return;
        }
        Seat s = seats[seat];
        s.bet *= 2;
        s.doubled = true;
        s.cards.add(draw(rnd));
        s.state = s.total() > 21 ? SeatState.BUST : SeatState.STAND;
        advance();
    }

    private boolean anyLive() {
        for (Seat s : seats) {
            if (s.state == SeatState.STAND) {
                return true;
            }
        }
        return false;
    }

    /** Un paso del crupier: da vuelta su carta, pide mientras tenga menos de 17 (si alguien sigue en juego), y al final se cierra la ronda. */
    public void dealerStep(Random rnd) {
        if (phase != Phase.DEALER) {
            return;
        }
        if (!holeRevealed) {
            holeRevealed = true;
            return;
        }
        if (anyLive() && BlackjackGame.total(dealer) < 17) {
            dealer.add(draw(rnd));
            return;
        }
        settle();
    }

    private void settle() {
        int d = BlackjackGame.total(dealer);
        boolean dBJ = BlackjackGame.isBlackjack(dealer);
        for (Seat s : seats) {
            if (s.bet <= 0) {
                continue;
            }
            int p = s.total();
            boolean pBJ = s.state == SeatState.BLACKJACK;
            if (s.state == SeatState.BUST || p > 21) {
                s.result = Result.BUST;
            } else if (pBJ && !dBJ) {
                s.result = Result.BLACKJACK;
            } else if (dBJ && !pBJ) {
                s.result = Result.LOSE;
            } else if (pBJ) {
                s.result = Result.PUSH;
            } else if (d > 21 || p > d) {
                s.result = Result.WIN;
            } else if (p == d) {
                s.result = Result.PUSH;
            } else {
                s.result = Result.LOSE;
            }
            s.payout = switch (s.result) {
                case BLACKJACK -> s.bet * 5 / 2;
                case WIN -> s.bet * 2;
                case PUSH -> s.bet;
                default -> 0;
            };
        }
        holeRevealed = true;
        current = -1;
        phase = Phase.RESULT;
    }

    /** Termina la ronda: se limpian las cartas y los asientos. */
    public void clearRound() {
        for (Seat s : seats) {
            s.clear();
        }
        dealer.clear();
        holeRevealed = false;
        current = -1;
        phase = Phase.IDLE;
    }
}
