package com.casino.game;

import com.casino.game.BlackjackGame.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La partida de una mesa de blackjack con varios asientos y un solo crupier, sin nada de Minecraft (así se puede probar sola).
 * Reglas: baraja infinita (cada carta es al azar), el crupier se planta con todos los 17 (incluso 17 blando), el blackjack natural paga 3 a 2,
 * se puede doblar con las dos primeras cartas, y si el crupier muestra As o 10 mira su carta tapada antes de que juegue nadie.
 * Separar: con dos cartas del mismo valor se separa en dos manos (una vez; los ases separados reciben una sola carta cada uno; un 21 tras
 * separar paga 1 a 1). Seguro: si el crupier muestra As se ofrece una apuesta extra de la mitad que paga 2 a 1 si tiene blackjack.
 *
 * Una ronda: IDLE -> BETTING (se apuesta) -> TURNS (cada jugador en orden) -> DEALER -> RESULT (se muestran resultados) -> IDLE.
 */
public final class BlackjackTable {
    public static final int SEATS = 5;

    public enum Phase { IDLE, BETTING, TURNS, DEALER, RESULT, INSURANCE }

    public enum SeatState { EMPTY, BET, PLAYING, STAND, BUST, BLACKJACK }

    public static final class Seat {
        public final List<Integer> cards = new ArrayList<>();
        public int bet;
        public boolean doubled;
        public SeatState state = SeatState.EMPTY;
        public Result result = Result.NONE;
        public int payout;
        /** Segunda mano (solo si se separó). */
        public final List<Integer> cards2 = new ArrayList<>();
        public int bet2;
        public boolean doubled2;
        public SeatState state2 = SeatState.EMPTY;
        public Result result2 = Result.NONE;
        public int payout2;
        public boolean split;
        /** Mano que está jugando (0 o 1). */
        public int hand;
        /** Seguro: lo apostado y lo que pagó, y si ya decidió. */
        public int insurance;
        public int insPayout;
        public boolean insDone;

        void clear() {
            cards.clear();
            bet = 0;
            doubled = false;
            state = SeatState.EMPTY;
            result = Result.NONE;
            payout = 0;
            cards2.clear();
            bet2 = 0;
            doubled2 = false;
            state2 = SeatState.EMPTY;
            result2 = Result.NONE;
            payout2 = 0;
            split = false;
            hand = 0;
            insurance = 0;
            insPayout = 0;
            insDone = false;
        }

        public int total() {
            return BlackjackGame.total(cards);
        }

        public int total2() {
            return BlackjackGame.total(cards2);
        }

        /** Cartas / apuesta / estado de la mano h (0 o 1). */
        public List<Integer> cards(int h) {
            return h == 0 ? cards : cards2;
        }

        public int bet(int h) {
            return h == 0 ? bet : bet2;
        }

        public SeatState state(int h) {
            return h == 0 ? state : state2;
        }

        /** Todo lo que puso en la ronda (manos + seguro). */
        public int stake() {
            return bet + bet2 + insurance;
        }

        /** Todo lo que cobra al final. */
        public int totalPayout() {
            return payout + payout2 + insPayout;
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
        current = -1;
        if (BlackjackGame.rank(dealer.get(0)) == 0) {
            phase = Phase.INSURANCE; // el crupier muestra un As: se ofrece el seguro antes de mirar su carta
            return;
        }
        peekOrPlay();
    }

    /** El crupier mira su carta tapada: si tiene blackjack la ronda termina; si no, empiezan los turnos. */
    private void peekOrPlay() {
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

    // ------------------------------------------------------------------ seguro

    public boolean canInsure(int seat) {
        return phase == Phase.INSURANCE && seat >= 0 && seat < SEATS && seats[seat].bet > 0 && !seats[seat].insDone;
    }

    /** Toma el seguro (la mitad de la apuesta); el cobro de las fichas lo hace quien llama antes. */
    public void insure(int seat) {
        if (canInsure(seat)) {
            seats[seat].insurance = seats[seat].bet / 2;
            seats[seat].insDone = true;
        }
    }

    public void declineInsurance(int seat) {
        if (canInsure(seat)) {
            seats[seat].insDone = true;
        }
    }

    public boolean insuranceDone() {
        for (Seat s : seats) {
            if (s.bet > 0 && !s.insDone) {
                return false;
            }
        }
        return true;
    }

    /** Se acabó el tiempo del seguro (o todos decidieron): el crupier mira su carta. */
    public void endInsurance() {
        if (phase != Phase.INSURANCE) {
            return;
        }
        for (Seat s : seats) {
            s.insDone = true;
        }
        peekOrPlay();
    }

    /** Pasa a la siguiente mano o al siguiente jugador que todavía puede jugar; si no queda ninguno le toca al crupier. */
    private void advance() {
        int from = current;
        if (from >= 0) {
            Seat c = seats[from];
            if (c.hand == 0 && c.state == SeatState.PLAYING) {
                return;
            }
            if (c.split && c.hand == 0 && c.state2 == SeatState.PLAYING) {
                c.hand = 1;
                return;
            }
            if (c.hand == 1 && c.state2 == SeatState.PLAYING) {
                return;
            }
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
        return phase == Phase.TURNS && seat == current && seats[seat].state(seats[seat].hand) == SeatState.PLAYING;
    }

    private void setState(Seat s, SeatState st) {
        if (s.hand == 0) {
            s.state = st;
        } else {
            s.state2 = st;
        }
    }

    /** Lo que cuesta doblar o separar ahora (la apuesta de la mano que juega). */
    public int activeBet(int seat) {
        return seats[seat].bet(seats[seat].hand);
    }

    public boolean canDouble(int seat) {
        return myTurn(seat) && seats[seat].cards(seats[seat].hand).size() == 2;
    }

    public boolean canSplit(int seat) {
        if (!myTurn(seat)) {
            return false;
        }
        Seat s = seats[seat];
        return !s.split && s.cards.size() == 2 && BlackjackGame.value(s.cards.get(0)) == BlackjackGame.value(s.cards.get(1));
    }

    public void hit(int seat, Random rnd) {
        if (!myTurn(seat)) {
            return;
        }
        Seat s = seats[seat];
        List<Integer> h = s.cards(s.hand);
        h.add(draw(rnd));
        int t = BlackjackGame.total(h);
        if (t > 21) {
            setState(s, SeatState.BUST);
        } else if (t == 21) {
            setState(s, SeatState.STAND);
        }
        advance();
    }

    public void stand(int seat) {
        if (!myTurn(seat)) {
            return;
        }
        setState(seats[seat], SeatState.STAND);
        advance();
    }

    /** Dobla la apuesta de la mano que juega y recibe una sola carta (el cobro de las fichas extra lo hace quien llama antes). */
    public void doubleDown(int seat, Random rnd) {
        if (!canDouble(seat)) {
            return;
        }
        Seat s = seats[seat];
        if (s.hand == 0) {
            s.bet *= 2;
            s.doubled = true;
        } else {
            s.bet2 *= 2;
            s.doubled2 = true;
        }
        List<Integer> h = s.cards(s.hand);
        h.add(draw(rnd));
        setState(s, BlackjackGame.total(h) > 21 ? SeatState.BUST : SeatState.STAND);
        advance();
    }

    /** Separa el par en dos manos con la misma apuesta (el cobro de la apuesta extra lo hace quien llama antes). */
    public void split(int seat, Random rnd) {
        if (!canSplit(seat)) {
            return;
        }
        Seat s = seats[seat];
        boolean aces = BlackjackGame.rank(s.cards.get(0)) == 0;
        s.cards2.add(s.cards.remove(1));
        s.bet2 = s.bet;
        s.split = true;
        s.hand = 0;
        s.cards.add(draw(rnd));
        s.cards2.add(draw(rnd));
        s.state = aces || s.total() == 21 ? SeatState.STAND : SeatState.PLAYING;
        s.state2 = aces || s.total2() == 21 ? SeatState.STAND : SeatState.PLAYING;
        advance();
    }

    private boolean anyLive() {
        for (Seat s : seats) {
            if (s.state == SeatState.STAND || s.state2 == SeatState.STAND) {
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
        boolean dBJ = BlackjackGame.isBlackjack(dealer);
        for (Seat s : seats) {
            if (s.bet <= 0) {
                continue;
            }
            s.result = resolve(s.cards, s.state, s.bet, dBJ);
            s.payout = pay(s.result, s.bet);
            if (s.split) {
                s.result2 = resolve(s.cards2, s.state2, s.bet2, dBJ);
                s.payout2 = pay(s.result2, s.bet2);
            }
            s.insPayout = dBJ ? s.insurance * 3 : 0;
        }
        holeRevealed = true;
        current = -1;
        phase = Phase.RESULT;
    }

    private Result resolve(List<Integer> cards, SeatState state, int bet, boolean dBJ) {
        int d = BlackjackGame.total(dealer);
        int p = BlackjackGame.total(cards);
        boolean pBJ = state == SeatState.BLACKJACK;
        if (state == SeatState.BUST || p > 21) {
            return Result.BUST;
        } else if (pBJ && !dBJ) {
            return Result.BLACKJACK;
        } else if (dBJ && !pBJ) {
            return Result.LOSE;
        } else if (pBJ) {
            return Result.PUSH;
        } else if (d > 21 || p > d) {
            return Result.WIN;
        } else if (p == d) {
            return Result.PUSH;
        }
        return Result.LOSE;
    }

    private static int pay(Result r, int bet) {
        return switch (r) {
            case BLACKJACK -> bet * 5 / 2;
            case WIN -> bet * 2;
            case PUSH -> bet;
            default -> 0;
        };
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
