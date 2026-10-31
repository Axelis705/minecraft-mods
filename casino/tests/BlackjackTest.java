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
    static boolean SPLITS = false;

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
        if (t.phase == Phase.INSURANCE) {
            t.endInsurance();
        }
        while (t.phase == Phase.TURNS && guard++ < 500) {
            int s = t.current;
            check(s >= 0 && t.seats[s].state(t.seats[s].hand) == SeatState.PLAYING, "el turno es de alguien que puede jugar");
            int tot = BlackjackGame.total(t.seats[s].cards(t.seats[s].hand));
            boolean soft = BlackjackGame.soft(t.seats[s].cards(t.seats[s].hand));
            int up = BlackjackGame.rank(t.dealer.get(0));
            int upv = up == 0 ? 11 : Math.min(10, up + 1);
            if (SPLITS && t.canSplit(s) && rnd.nextInt(2) == 0) {
                t.split(s, rnd);
            } else if (basic) {
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
            if (t.phase == Phase.INSURANCE) {
                t.endInsurance();
            }
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


        // --- separar con cartas fijas
        t = new BlackjackTable();
        t.placeBet(1, 50);
        t.deal(new Random(5));
        t.dealer.clear(); t.dealer.add(c(5)); t.dealer.add(c(9)); // 6 + 10 = 16 (pedira)
        t.seats[1].cards.clear(); t.seats[1].cards.add(c(7)); t.seats[1].cards.add(c(7) + 13); // 8 y 8
        t.seats[1].state = SeatState.PLAYING; t.phase = Phase.TURNS; t.current = 1; t.holeRevealed = false;
        check(t.canSplit(1) && t.canDouble(1), "con 8-8 se puede separar y doblar");
        t.split(1, new Random(1));
        var s1 = t.seats[1];
        check(s1.split && s1.cards.size() == 2 && s1.cards2.size() == 2 && s1.bet2 == 50, "separar deja dos manos de dos cartas con la misma apuesta");
        check(s1.cards.get(0) == c(7) && s1.cards2.get(0) == c(7) + 13, "cada mano conserva una de las cartas originales");
        check(!t.canSplit(1), "no se puede separar dos veces");
        check(t.current == 1 && s1.hand == 0 || s1.state == SeatState.STAND, "juega primero la mano 1");
        // ahora fijamos las manos para comprobar el pago: mano 1 = 8+3 (11) y mano 2 = 8+K (18)
        s1.cards.clear(); s1.cards.add(c(7)); s1.cards.add(c(2)); s1.state = SeatState.PLAYING; s1.hand = 0;
        s1.cards2.clear(); s1.cards2.add(c(7) + 13); s1.cards2.add(c(12)); s1.state2 = SeatState.PLAYING;
        check(t.canDouble(1), "se puede doblar una mano separada");
        t.doubleDown(1, new Random(2));
        check(s1.doubled && s1.bet == 100 && s1.cards.size() == 3 && s1.state != SeatState.PLAYING, "doblar la primera mano: apuesta 100, una carta, se planta");
        check(s1.hand == 1 && s1.state2 == SeatState.PLAYING && t.phase == Phase.TURNS, "luego juega la segunda mano");
        check(s1.bet2 == 50 && !s1.doubled2, "la segunda mano no se doblo");
        t.stand(1);
        check(t.phase == Phase.DEALER, "con las dos manos hechas juega el crupier");
        int guardS = 0;
        while (t.phase == Phase.DEALER && guardS++ < 50) {
            t.dealerStep(new Random(9));
        }
        check(t.phase == Phase.RESULT && s1.result != Result.NONE && s1.result2 != Result.NONE, "cada mano tiene su resultado");
        check(s1.totalPayout() == s1.payout + s1.payout2 && s1.stake() == s1.bet + s1.bet2, "totales de la ronda");
        int dtt = BlackjackGame.total(t.dealer);
        if (dtt <= 21 && dtt > 18) {
            check(s1.result2 == Result.LOSE, "18 pierde contra " + dtt);
        }

        // ases separados: una sola carta cada uno y no es blackjack
        t = new BlackjackTable();
        t.placeBet(0, 20);
        t.deal(new Random(5));
        t.dealer.clear(); t.dealer.add(c(8)); t.dealer.add(c(7)); // 9 + 8 = 17
        t.seats[0].cards.clear(); t.seats[0].cards.add(c(0)); t.seats[0].cards.add(c(0) + 13);
        t.seats[0].state = SeatState.PLAYING; t.phase = Phase.TURNS; t.current = 0; t.holeRevealed = false;
        t.split(0, new Random(4));
        check(t.seats[0].cards.size() == 2 && t.seats[0].cards2.size() == 2, "ases separados: una carta cada uno");
        check(t.seats[0].state == SeatState.STAND && t.seats[0].state2 == SeatState.STAND && t.phase == Phase.DEALER, "ases separados: se plantan solos");
        while (t.phase == Phase.DEALER) {
            t.dealerStep(new Random(1));
        }
        check(t.seats[0].result != Result.BLACKJACK && t.seats[0].result2 != Result.BLACKJACK, "A+10 tras separar no es blackjack");

        // no se separa con valores distintos; si, con J y K
        t = new BlackjackTable();
        t.placeBet(0, 20);
        t.deal(new Random(5));
        t.seats[0].cards.clear(); t.seats[0].cards.add(c(9)); t.seats[0].cards.add(c(5));
        t.seats[0].state = SeatState.PLAYING; t.phase = Phase.TURNS; t.current = 0;
        check(!t.canSplit(0), "10 y 6 no se separan");
        t.seats[0].cards.clear(); t.seats[0].cards.add(c(10)); t.seats[0].cards.add(c(12));
        check(t.canSplit(0), "J y K se pueden separar (valen 10 los dos)");

        // --- seguro
        int insRounds = 0, insDealerBJ = 0;
        for (int seed = 0; seed < 4000; seed++) {
            t = new BlackjackTable();
            t.placeBet(2, 50);
            t.placeBet(3, 20);
            Random r = new Random(seed);
            t.deal(r);
            int upRank = BlackjackGame.rank(t.dealer.get(0));
            if (upRank == 0) {
                check(t.phase == Phase.INSURANCE, "con un As visible se ofrece el seguro");
                check(t.canInsure(2) && t.canInsure(3) && !t.canInsure(0), "seguro solo para quien apostó");
                t.insure(2);
                check(t.seats[2].insurance == 25 && t.seats[2].insDone, "el seguro cuesta la mitad");
                check(!t.canInsure(2), "no se asegura dos veces");
                check(!t.insuranceDone(), "falta decidir el asiento 3");
                t.declineInsurance(3);
                check(t.insuranceDone(), "todos decidieron");
                boolean dealerBJ = BlackjackGame.isBlackjack(t.dealer);
                t.endInsurance();
                insRounds++;
                if (dealerBJ) {
                    insDealerBJ++;
                    check(t.phase == Phase.RESULT && t.seats[2].insPayout == 75 && t.seats[3].insPayout == 0, "blackjack del crupier: el seguro paga 2 a 1 (75 de 25)");
                } else {
                    check(t.phase != Phase.RESULT || t.seats[2].insPayout == 0, "sin blackjack del crupier el seguro se pierde");
                    check(t.seats[2].insPayout == 0, "seguro perdido no paga");
                    check(t.phase == Phase.TURNS || t.phase == Phase.DEALER, "sin blackjack siguen los turnos");
                }
            } else if (BlackjackGame.value(t.dealer.get(0)) == 10) {
                check(t.phase != Phase.INSURANCE, "con un 10 no hay seguro");
                check(!t.canInsure(2), "sin fase de seguro no se puede asegurar");
            } else {
                check(t.phase == Phase.TURNS || t.phase == Phase.DEALER, "con otra carta siguen los turnos");
            }
        }
        check(insRounds > 150 && insDealerBJ > 20, "hubo suficientes rondas de seguro para probar (" + insRounds + ", " + insDealerBJ + ")");

        // --- simulacion con separar activado: invariantes por mano
        SPLITS = true;
        long sb = 0, sbk = 0, splitsDone = 0;
        for (int i = 0; i < 200000; i++) {
            t = new BlackjackTable();
            int n = 1 + rnd.nextInt(3);
            for (int k = 0; k < n; k++) {
                int seat = rnd.nextInt(5);
                if (t.canBet(seat)) {
                    t.placeBet(seat, 20);
                }
            }
            if (t.betCount() == 0) {
                continue;
            }
            t.deal(rnd);
            if (t.phase == Phase.INSURANCE) {
                for (int k = 0; k < 5; k++) {
                    if (rnd.nextBoolean()) {
                        t.insure(k);
                    } else {
                        t.declineInsurance(k);
                    }
                }
                check(t.insuranceDone(), "todos decidieron el seguro");
            }
            playRound(t, rnd, true);
            check(t.phase == Phase.RESULT, "con separar la ronda tambien termina");
            int dt2 = BlackjackGame.total(t.dealer);
            for (var s : t.seats) {
                if (s.bet <= 0) {
                    continue;
                }
                for (int h = 0; h < (s.split ? 2 : 1); h++) {
                    Result r = h == 0 ? s.result : s.result2;
                    int pay = h == 0 ? s.payout : s.payout2;
                    int bet2 = s.bet(h);
                    int tot = BlackjackGame.total(s.cards(h));
                    check(r != Result.NONE, "cada mano tiene resultado");
                    if (r == Result.BLACKJACK) {
                        check(!s.split && pay == bet2 * 5 / 2, "blackjack solo sin separar");
                    }
                    if (r == Result.WIN) {
                        check(pay == bet2 * 2 && (dt2 > 21 || tot > dt2) && tot <= 21, "ganar paga el doble");
                    }
                    if (r == Result.BUST) {
                        check(tot > 21 && pay == 0, "pasado no paga");
                    }
                }
                if (s.split) {
                    splitsDone++;
                    check(s.cards.size() >= 2 && s.cards2.size() >= 2 && s.bet2 >= 20, "dos manos validas");
                    check(s.result2 != Result.NONE, "la segunda mano tiene resultado");
                } else {
                    check(s.cards2.isEmpty() && s.result2 == Result.NONE && s.bet2 == 0, "sin separar no hay segunda mano");
                }
                check(s.totalPayout() == s.payout + s.payout2 + s.insPayout, "total cobrado");
                sb += s.stake();
                sbk += s.totalPayout();
            }
        }
        SPLITS = false;
        System.out.printf("Con separar y seguro al azar: %d separaciones, retorno %.1f%%%n", splitsDone, 100.0 * sbk / sb);
        check(splitsDone > 1000, "hubo separaciones para probar");
        check(100.0 * sbk / sb > 85 && 100.0 * sbk / sb < 102, "retorno con separar entre 85% y 102%");

        System.out.println(fails == 0 ? "BlackjackTest: TODO BIEN" : "BlackjackTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
