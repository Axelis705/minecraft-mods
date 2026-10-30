import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackLayout;
import com.casino.game.BlackjackGame.Result;
import com.casino.game.BlackjackTable;
import com.casino.game.BlackjackTable.Phase;
import com.casino.game.BlackjackTable.SeatState;

import java.util.List;
import java.util.Random;

/** Pruebas de las reglas del blackjack con varios asientos (sin Minecraft). */
public class BlackjackTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            if (fails < 30) {
                System.out.println("FALLA: " + what);
            }
        }
    }

    static int c(int rank) {
        return rank; // carta = rank + 13*palo ; rank 0 = As, 1..8 = 2..9, 9..12 = 10 J Q K
    }

    /** Juega una ronda completa con una estrategia sencilla en todos los asientos apostados. Devuelve false si algo se atoro. */
    static void playRound(BlackjackTable t, Random rnd, boolean basic) {
        int guard = 0;
        while (t.phase == Phase.TURNS && guard++ < 500) {
            int s = t.current;
            check(s >= 0 && t.seats[s].state == SeatState.PLAYING, "el turno es de alguien que puede jugar");
            int tot = t.seats[s].total();
            boolean soft = BlackjackGame.soft(t.seats[s].cards);
            int up = BlackjackGame.rank(t.dealer.get(0));
            int upv = up == 0 ? 11 : Math.min(10, up + 1);
            if (basic) {
                if (t.canDouble(s) && (tot == 11 || (tot == 10 && upv < 10) || (soft && tot >= 15 && tot <= 18 && upv >= 4 && upv <= 6))) {
                    t.doubleDown(s, rnd);
                } else if (soft ? tot < 18 : tot < 12 || (tot < 17 && upv >= 7)) {
                    t.hit(s, rnd);
                } else {
                    t.stand(s);
                }
            } else {
                if (t.canDouble(s) && (tot == 10 || tot == 11) && rnd.nextInt(2) == 0) {
                    t.doubleDown(s, rnd);
                } else if (tot < 17) {
                    t.hit(s, rnd);
                } else {
                    t.stand(s);
                }
            }
        }
        guard = 0;
        while (t.phase == Phase.DEALER && guard++ < 50) {
            t.dealerStep(rnd);
        }
    }

    public static void main(String[] a) {
        check(BlackjackGame.total(List.of(c(0), c(12))) == 21, "A+K = 21");
        check(BlackjackGame.isBlackjack(List.of(c(0), c(9))), "A+10 es blackjack");
        check(!BlackjackGame.isBlackjack(List.of(c(0), c(4), c(5))), "A+5+6 = 21 no es blackjack natural");
        check(BlackjackGame.total(List.of(c(0), c(0), c(8))) == 21, "A+A+9 = 21 (11+1+9)");
        check(BlackjackGame.total(List.of(c(0), c(0), c(0))) == 13, "A+A+A = 13");
        check(BlackjackGame.total(List.of(c(0), c(5), c(12))) == 17, "A+6+K = 17 (as vale 1)");
        check(BlackjackGame.soft(List.of(c(0), c(5))), "A+6 es blando");
        check(!BlackjackGame.soft(List.of(c(0), c(5), c(12))), "A+6+K no es blando");
        check(BlackjackGame.total(List.of(c(12), c(11), c(10))) == 30, "K+Q+J = 30");

        // --- forma de la mesa: todos los puntos de la media luna caen en algun bloque, y los bloques tocan la media luna
        for (double x = -2.49; x <= 2.49; x += 0.07) {
            for (double z = -0.49; z <= 2.0; z += 0.07) {
                if (BlackjackLayout.inside(x, z, 0)) {
                    int col = (int) Math.floor(x + 0.5);
                    int row = (int) Math.floor(z + 0.5);
                    check(BlackjackLayout.cellIndex(col, row) >= 0, "el punto (" + x + "," + z + ") cae en el bloque " + col + "," + row + " que no esta en la mesa");
                }
            }
        }
        for (int i = 0; i < BlackjackLayout.CELLS.length; i++) {
            int col = BlackjackLayout.CELLS[i][0];
            int row = BlackjackLayout.CELLS[i][1];
            boolean touches = false;
            for (double x = col - 0.5; x <= col + 0.5; x += 0.05) {
                for (double z = row - 0.5; z <= row + 0.5; z += 0.05) {
                    touches |= BlackjackLayout.inside(x, z, 0.02);
                }
            }
            check(touches, "el bloque " + col + "," + row + " no toca la mesa");
        }
        for (int i = 0; i < BlackjackLayout.SEATS; i++) {
            double[] p = BlackjackLayout.polar(1.6, BlackjackLayout.seatAngle(i));
            check(BlackjackLayout.inside(p[0], p[1], 0.3), "las cartas del asiento " + i + " caen dentro de la mesa");
            boolean[] free = new boolean[BlackjackLayout.SEATS];
            java.util.Arrays.fill(free, true);
            double[] q = BlackjackLayout.polar(3.2, BlackjackLayout.seatAngle(i));
            check(BlackjackLayout.nearestSeat(q[0], q[1], free) == i, "quien se para frente al asiento " + i + " se sienta ahi");
        }

        // --- reglas basicas de la mesa
        BlackjackTable t = new BlackjackTable();
        Random rnd = new Random(3);
        check(t.phase == Phase.IDLE, "empieza en IDLE");
        check(!t.placeBet(0, 15), "apuesta invalida rechazada");
        check(t.placeBet(2, 50) && t.phase == Phase.BETTING, "apuesta valida pasa a BETTING");
        check(!t.placeBet(2, 50), "no se puede apostar dos veces en el mismo asiento");
        check(t.placeBet(4, 100), "otro asiento puede apostar");
        t.deal(rnd);
        check(t.seats[2].cards.size() == 2 && t.seats[4].cards.size() == 2 && t.seats[0].cards.isEmpty() && t.dealer.size() == 2, "reparto solo a los que apostaron");
        check(!t.placeBet(1, 50), "no se apuesta con la ronda en marcha");
        if (t.phase == Phase.TURNS) {
            check(t.current == 2 || t.current == 4, "el primer turno es del primer asiento que juega");
            int other = t.current == 2 ? 4 : 2;
            t.stand(other); // fuera de turno: ignorado
            check(t.seats[other].state != SeatState.STAND || t.current != other, "no se puede jugar fuera de turno");
        }

        // --- orden de turnos y pagos con cartas fijas
        t = new BlackjackTable();
        t.placeBet(0, 10); t.placeBet(1, 20); t.placeBet(3, 50);
        t.phase = Phase.BETTING;
        t.deal(new Random(11));
        t.clearRound();
        check(t.phase == Phase.IDLE && t.betCount() == 0 && t.dealer.isEmpty(), "clearRound limpia todo");

        // --- simulaciones: invariantes con 1 a 5 asientos
        long bet = 0, back = 0, rounds = 0, bj = 0;
        for (int i = 0; i < 300000; i++) {
            t = new BlackjackTable();
            int n = 1 + rnd.nextInt(5);
            for (int k = 0; k < n; k++) {
                int seat = rnd.nextInt(5);
                if (t.canBet(seat)) {
                    t.placeBet(seat, BlackjackGame.BETS[rnd.nextInt(BlackjackGame.BETS.length)]);
                }
            }
            if (t.betCount() == 0) {
                continue;
            }
            int n0 = t.betCount();
            t.deal(rnd);
            check(t.dealer.size() == 2, "el crupier recibe 2");
            for (var s : t.seats) {
                check((s.bet > 0) == (s.cards.size() >= 2), "solo los que apostaron reciben cartas");
            }
            playRound(t, rnd, false);
            check(t.phase == Phase.RESULT, "la ronda termina en RESULT");
            check(t.holeRevealed, "al final la carta tapada esta visible");
            int dt = BlackjackGame.total(t.dealer);
            boolean anyStand = false;
            for (var s : t.seats) {
                if (s.bet <= 0) {
                    check(s.result == Result.NONE && s.payout == 0, "asientos sin apuesta no tienen resultado");
                    continue;
                }
                check(s.result != Result.NONE, "hay resultado");
                if (s.result == Result.BUST) {
                    check(s.total() > 21 && s.payout == 0, "BUST solo si se paso y no paga");
                }
                if (s.result == Result.BLACKJACK) {
                    check(s.payout == s.bet * 5 / 2 && !s.doubled, "blackjack paga 3 a 2");
                    bj++;
                }
                if (s.result == Result.WIN) {
                    check(s.payout == s.bet * 2, "ganar paga el doble");
                    check(dt > 21 || s.total() > dt, "WIN: crupier pasado o menos puntos");
                }
                if (s.result == Result.PUSH) {
                    check(s.payout == s.bet, "empate devuelve la apuesta");
                }
                if (s.result == Result.LOSE) {
                    check(s.payout == 0 && (dt >= s.total() && dt <= 21), "LOSE: el crupier tiene mas o igual sin pasarse");
                }
                if (s.state == SeatState.STAND) {
                    anyStand = true;
                }
                bet += s.bet;
                back += s.payout;
            }
            if (anyStand && !BlackjackGame.isBlackjack(t.dealer)) {
                check(dt >= 17, "el crupier no se planta con menos de 17 (" + dt + ") si alguien sigue en juego");
            }
            rounds++;
        }
        System.out.printf("Blackjack multiasiento: %d rondas, retorno %.1f%% (estrategia floja), blackjacks %d%n", rounds, 100.0 * back / bet, bj);
        check(100.0 * back / bet > 85 && 100.0 * back / bet < 100, "retorno con estrategia floja entre 85% y 100%");

        // --- estrategia basica sencilla en un asiento: la casa debe quedar ligeramente arriba
        back = 0; bet = 0;
        for (int i = 0; i < 600000; i++) {
            t = new BlackjackTable();
            t.placeBet(2, 10);
            t.deal(rnd);
            playRound(t, rnd, true);
            bet += t.seats[2].bet;
            back += t.seats[2].payout;
        }
        double rtp = 100.0 * back / bet;
        System.out.printf("Con estrategia basica sencilla (un asiento): retorno %.2f%%%n", rtp);
        check(rtp > 97 && rtp < 100.5, "retorno con estrategia basica entre 97% y 100.5% (" + rtp + ")");

        // --- el crupier no pide cartas si todos se pasaron
        int noDraw = 0;
        for (int i = 0; i < 20000; i++) {
            t = new BlackjackTable();
            t.placeBet(1, 10);
            t.deal(rnd);
            while (t.phase == Phase.TURNS) {
                t.hit(1, rnd);
            }
            if (t.seats[1].state == SeatState.BUST) {
                while (t.phase == Phase.DEALER) {
                    t.dealerStep(rnd);
                }
                check(t.dealer.size() == 2, "si el unico jugador se paso, el crupier no pide mas");
                noDraw++;
            }
        }
        check(noDraw > 100, "hubo manos pasadas para comprobar");

        System.out.println(fails == 0 ? "BlackjackTest: TODO BIEN" : "BlackjackTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
