package com.casino.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Una mesa de Texas Hold'em contra el crupier ("Casino Hold'em"), sin nada de Minecraft (así se puede probar sola).
 * Cada jugador pone una apuesta inicial (ante). Todos reciben 2 cartas, el crupier recibe 2 tapadas y se muestran 3 cartas comunes (el flop).
 * Cada jugador decide: retirarse (pierde el ante) o IGUALAR pagando el doble del ante. Después salen la 4.ª y la 5.ª carta común, el crupier
 * muestra las suyas y gana la mejor mano de 5 cartas entre las 7. El crupier califica con par de 4 o mejor:
 *  - si no califica: el ante paga 1 a 1 y la igualación se devuelve;
 *  - si califica y gana el jugador: la igualación paga 1 a 1 y el ante paga según la mano (escalera 1:1, color 2:1, full 3:1, póker 10:1,
 *    escalera de color 20:1, escalera real 100:1);
 *  - empate: todo se devuelve; si gana el crupier se pierde todo.
 * Una ronda: IDLE -> BETTING -> DECIDE -> SHOWDOWN -> RESULT -> IDLE.
 */
public final class PokerTable {
    public static final int SEATS = 5;
    public static final int[] ANTES = {10, 20, 50, 100, 250, 500};

    public enum Phase { IDLE, BETTING, DECIDE, SHOWDOWN, RESULT }

    public enum SeatState { EMPTY, ANTE, DECIDING, CALLED, FOLDED }

    public enum Result { NONE, FOLD, WIN, WIN_NQ, PUSH, LOSE }

    public static final class Seat {
        public final List<Integer> hole = new ArrayList<>();
        public int ante;
        public int call;
        public SeatState state = SeatState.EMPTY;
        public Result result = Result.NONE;
        public int payout;
        /** Categoría de la mano final (0..9), solo después del resultado. */
        public int handCat = -1;

        void clear() {
            hole.clear();
            ante = 0;
            call = 0;
            state = SeatState.EMPTY;
            result = Result.NONE;
            payout = 0;
            handCat = -1;
        }
    }

    public final Seat[] seats = new Seat[SEATS];
    /** Cartas comunes ya mostradas (3, 4 o 5). */
    public final List<Integer> board = new ArrayList<>();
    /** Las 2 cartas del crupier. Se conocen en el servidor desde que se reparte, pero el cliente solo las ve al mostrarse. */
    public final List<Integer> dealer = new ArrayList<>();
    /** Las cartas comunes que todavía no salieron (turn y river). Solo el servidor las sabe. */
    public final List<Integer> future = new ArrayList<>();
    public Phase phase = Phase.IDLE;
    public boolean dealerRevealed;
    public boolean dealerQualified;
    public int dealerCat = -1;

    public PokerTable() {
        for (int i = 0; i < SEATS; i++) {
            seats[i] = new Seat();
        }
    }

    public static boolean validAnte(int b) {
        for (int x : ANTES) {
            if (x == b) {
                return true;
            }
        }
        return false;
    }

    public int anteCount() {
        int n = 0;
        for (Seat s : seats) {
            if (s.ante > 0) {
                n++;
            }
        }
        return n;
    }

    public boolean canBet(int seat) {
        return (phase == Phase.IDLE || phase == Phase.BETTING) && seat >= 0 && seat < SEATS && seats[seat].state == SeatState.EMPTY;
    }

    /** Un jugador pone su apuesta inicial en un asiento libre (el cobro de las fichas lo hace quien llama antes). */
    public boolean placeAnte(int seat, int ante) {
        if (!canBet(seat) || !validAnte(ante)) {
            return false;
        }
        Seat s = seats[seat];
        s.ante = ante;
        s.state = SeatState.ANTE;
        phase = Phase.BETTING;
        return true;
    }

    /** Baraja y reparte: 2 a cada jugador con apuesta, 2 al crupier y el flop. */
    public void deal(Random rnd) {
        if (phase != Phase.BETTING || anteCount() == 0) {
            return;
        }
        List<Integer> deck = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            deck.add(i);
        }
        Collections.shuffle(deck, rnd);
        int k = 0;
        board.clear();
        dealer.clear();
        future.clear();
        dealerRevealed = false;
        dealerQualified = false;
        dealerCat = -1;
        for (Seat s : seats) {
            s.hole.clear();
        }
        for (int round = 0; round < 2; round++) {
            for (Seat s : seats) {
                if (s.ante > 0) {
                    s.hole.add(deck.get(k++));
                }
            }
            dealer.add(deck.get(k++));
        }
        for (int i = 0; i < 3; i++) {
            board.add(deck.get(k++));
        }
        future.add(deck.get(k++));
        future.add(deck.get(k++));
        for (Seat s : seats) {
            if (s.ante > 0) {
                s.state = SeatState.DECIDING;
            }
        }
        phase = Phase.DECIDE;
    }

    public boolean canCall(int seat) {
        return phase == Phase.DECIDE && seat >= 0 && seat < SEATS && seats[seat].state == SeatState.DECIDING;
    }

    /** Iguala: se paga el doble del ante (las fichas las cobra quien llama antes). */
    public int callCost(int seat) {
        return seats[seat].ante * 2;
    }

    public void call(int seat) {
        if (!canCall(seat)) {
            return;
        }
        Seat s = seats[seat];
        s.call = s.ante * 2;
        s.state = SeatState.CALLED;
    }

    public void fold(int seat) {
        if (!canCall(seat)) {
            return;
        }
        seats[seat].state = SeatState.FOLDED;
    }

    public boolean allDecided() {
        for (Seat s : seats) {
            if (s.state == SeatState.DECIDING) {
                return false;
            }
        }
        return true;
    }

    /** Se acabó el tiempo: los que no decidieron se retiran. */
    public void foldUndecided() {
        for (Seat s : seats) {
            if (s.state == SeatState.DECIDING) {
                s.state = SeatState.FOLDED;
            }
        }
    }

    public boolean anyCalled() {
        for (Seat s : seats) {
            if (s.state == SeatState.CALLED) {
                return true;
            }
        }
        return false;
    }

    /** Pasa del flop a mostrar el resto (si nadie igualó, la ronda termina de una vez). */
    public void startShowdown() {
        if (phase != Phase.DECIDE) {
            return;
        }
        if (!anyCalled()) {
            settle();
            return;
        }
        phase = Phase.SHOWDOWN;
    }

    /** Un paso del showdown: 4.ª carta, 5.ª carta, el crupier muestra, y al final se cierra la ronda. */
    public void showdownStep() {
        if (phase != Phase.SHOWDOWN) {
            return;
        }
        if (board.size() < 5 && !future.isEmpty()) {
            board.add(future.remove(0));
            return;
        }
        if (!dealerRevealed) {
            dealerRevealed = true;
            return;
        }
        settle();
    }

    private void settle() {
        boolean called = anyCalled();
        List<Integer> fullBoard = new ArrayList<>(board);
        List<Integer> d = new ArrayList<>();
        long dScore = 0;
        if (called) {
            fullBoard.addAll(future);
            board.clear();
            board.addAll(fullBoard);
            future.clear();
            d.addAll(dealer);
            d.addAll(fullBoard);
            dScore = PokerHands.best(d);
            dealerCat = PokerHands.category(dScore);
            dealerQualified = PokerHands.dealerQualifies(dScore);
        }
        for (Seat s : seats) {
            if (s.ante <= 0) {
                continue;
            }
            if (s.state != SeatState.CALLED) {
                s.result = Result.FOLD;
                s.payout = 0;
                continue;
            }
            List<Integer> h = new ArrayList<>(s.hole);
            h.addAll(fullBoard);
            long pScore = PokerHands.best(h);
            s.handCat = PokerHands.category(pScore);
            if (!dealerQualified) {
                s.result = Result.WIN_NQ;
                s.payout = s.ante * 2 + s.call;
            } else if (pScore > dScore) {
                s.result = Result.WIN;
                s.payout = s.ante * (1 + PokerHands.antePayMultiplier(s.handCat)) + s.call * 2;
            } else if (pScore == dScore) {
                s.result = Result.PUSH;
                s.payout = s.ante + s.call;
            } else {
                s.result = Result.LOSE;
                s.payout = 0;
            }
        }
        dealerRevealed = called;
        phase = Phase.RESULT;
    }

    /** Termina la ronda: se limpia todo. */
    public void clearRound() {
        for (Seat s : seats) {
            s.clear();
        }
        board.clear();
        dealer.clear();
        future.clear();
        dealerRevealed = false;
        dealerQualified = false;
        dealerCat = -1;
        phase = Phase.IDLE;
    }
}
