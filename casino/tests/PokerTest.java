import com.casino.game.PokerHands;
import com.casino.game.PokerTable;
import com.casino.game.PokerTable.Phase;
import com.casino.game.PokerTable.Result;
import com.casino.game.PokerTable.SeatState;

import java.util.*;

public class PokerTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        if (!ok) {
            fails++;
            System.out.println("FALLA: " + msg);
        }
    }

    /** carta desde texto: "AS" = As de palo 0, valor + palo (s,h,d,c) */
    static int c(String s) {
        String r = "A23456789TJQK";
        int rank = r.indexOf(s.charAt(0));
        int suit = "shdc".indexOf(s.charAt(1));
        return suit * 13 + rank;
    }

    static List<Integer> h(String... cs) {
        List<Integer> l = new ArrayList<>();
        for (String x : cs) {
            l.add(c(x));
        }
        return l;
    }

    static int cat(String... cs) {
        return PokerHands.category(PokerHands.best(h(cs)));
    }

    public static void main(String[] a) {
        // categorias
        check(cat("As", "Ks", "Qs", "Js", "Ts", "2d", "3c") == PokerHands.ROYAL_FLUSH, "escalera real");
        check(cat("9h", "8h", "7h", "6h", "5h", "2d", "Kc") == PokerHands.STRAIGHT_FLUSH, "escalera de color");
        check(cat("As", "2s", "3s", "4s", "5s", "Kd", "Kc") == PokerHands.STRAIGHT_FLUSH, "escalera de color con la rueda");
        check(cat("9h", "9s", "9d", "9c", "5h", "2d", "Kc") == PokerHands.QUADS, "poker");
        check(cat("9h", "9s", "9d", "Kc", "Kh", "2d", "3c") == PokerHands.FULL_HOUSE, "full");
        check(cat("9h", "9s", "9d", "Kc", "Kh", "Kd", "3c") == PokerHands.FULL_HOUSE, "dos tríos = full");
        check(cat("2h", "9h", "4h", "Kh", "Jh", "3d", "Qc") == PokerHands.FLUSH, "color");
        check(cat("5h", "6s", "7d", "8c", "9h", "2d", "Kc") == PokerHands.STRAIGHT, "escalera");
        check(cat("As", "2h", "3d", "4c", "5h", "9d", "Kc") == PokerHands.STRAIGHT, "escalera con la rueda");
        check(cat("Th", "Js", "Qd", "Kc", "Ah", "2d", "3c") == PokerHands.STRAIGHT, "escalera al As");
        check(cat("Qh", "Ks", "Ad", "2c", "3h", "5d", "9c") == PokerHands.HIGH_CARD, "carta alta (sin escalera K-A-2)");
        check(cat("9h", "9s", "9d", "Kc", "2h", "4d", "7c") == PokerHands.TRIPS, "trio");
        check(cat("9h", "9s", "4d", "Kc", "Kh", "2d", "7c") == PokerHands.TWO_PAIR, "dos pares");
        check(cat("9h", "9s", "4d", "Kc", "Qh", "2d", "7c") == PokerHands.PAIR, "par");
        // desempates
        check(PokerHands.best(h("Ah", "Kh", "2d", "3c", "9s")) > PokerHands.best(h("Ad", "Qh", "2s", "3h", "9c")), "A-K gana a A-Q");
        check(PokerHands.best(h("9h", "9s", "Ad", "3c", "2s")) > PokerHands.best(h("8h", "8s", "Ad", "Kc", "Qs")), "par de 9 gana a par de 8");
        check(PokerHands.best(h("9h", "9s", "Ad", "3c", "2s")) > PokerHands.best(h("9d", "9c", "Kd", "Qc", "Js")), "mismo par: patada A gana");
        check(PokerHands.best(h("9h", "9s", "Ad", "3c", "2s")) == PokerHands.best(h("9d", "9c", "Ah", "3s", "2c")), "empate exacto");
        check(PokerHands.best(h("6h", "5s", "4d", "3c", "2s")) > PokerHands.best(h("As", "2h", "3d", "4c", "5h")), "escalera al 6 gana a la rueda");
        check(PokerHands.best(h("Kh", "Kd", "Ks", "2c", "2s")) > PokerHands.best(h("Qh", "Qd", "Qs", "Ac", "As")), "full: el trio manda");
        // la mejor de 7 usa las mejores 5
        check(PokerHands.category(PokerHands.best(h("Ah", "Ad", "Ks", "Kc", "Qh", "Qd", "2s"))) == PokerHands.TWO_PAIR, "tres pares = dos pares");
        long three = PokerHands.best(h("Ah", "Ad", "Ks", "Kc", "Qh", "Qd", "2s"));
        check(PokerHands.mainRank(three) == 14, "tres pares: los mejores son A y K");
        // califica
        check(!PokerHands.dealerQualifies(PokerHands.best(h("Ah", "Kd", "9s", "7c", "2h"))), "carta alta no califica");
        check(!PokerHands.dealerQualifies(PokerHands.best(h("3h", "3d", "9s", "7c", "2h"))), "par de 3 no califica");
        check(PokerHands.dealerQualifies(PokerHands.best(h("4h", "4d", "9s", "7c", "2h"))), "par de 4 califica");
        check(PokerHands.dealerQualifies(PokerHands.best(h("2h", "2d", "9s", "9c", "5h"))), "dos pares califica");

        // 100000 manos de 7 cartas: la mas alta nunca baja al agregar cartas, y las frecuencias son las conocidas
        Random rnd = new Random(7);
        int[] freq = new int[PokerHands.CATEGORIES];
        int N = 300000;
        for (int i = 0; i < N; i++) {
            List<Integer> deck = new ArrayList<>();
            for (int k = 0; k < 52; k++) {
                deck.add(k);
            }
            Collections.shuffle(deck, rnd);
            List<Integer> s7 = deck.subList(0, 7);
            freq[PokerHands.category(PokerHands.best(s7))]++;
            check(PokerHands.best(s7) >= PokerHands.best(s7.subList(0, 5)), "mas cartas no empeoran la mano");
        }
        // Frecuencias teoricas de la mejor mano de 7 cartas: carta alta 17.4%, par 43.8%, dos pares 23.5%, trio 4.83%, escalera 4.62%, color 3.03%, full 2.60%, poker 0.168%
        double[] want = {0.1741, 0.4380, 0.2350, 0.0483, 0.0462, 0.0303 , 0.0260, 0.00168};
        for (int k = 0; k < 8; k++) {
            double got = freq[k] / (double) N;
            check(Math.abs(got - want[k]) < Math.max(0.004, want[k] * 0.25), "frecuencia de la categoria " + k + ": " + got + " vs " + want[k]);
        }

        // ---- mesa
        PokerTable t = new PokerTable();
        check(t.placeAnte(1, 50), "apostar");
        check(!t.placeAnte(1, 50), "no apostar dos veces en el mismo asiento");
        check(!t.placeAnte(2, 15), "apuesta invalida");
        check(t.placeAnte(3, 10), "otro asiento");
        t.deal(new Random(1));
        check(t.phase == Phase.DECIDE, "fase de decision");
        check(t.board.size() == 3 && t.dealer.size() == 2 && t.future.size() == 2, "flop y cartas guardadas");
        check(t.seats[1].hole.size() == 2 && t.seats[3].hole.size() == 2 && t.seats[0].hole.isEmpty(), "dos cartas a cada apostador");
        Set<Integer> all = new HashSet<>();
        all.addAll(t.board);
        all.addAll(t.dealer);
        all.addAll(t.future);
        all.addAll(t.seats[1].hole);
        all.addAll(t.seats[3].hole);
        check(all.size() == 11, "no hay cartas repetidas (" + all.size() + ")");
        check(t.callCost(1) == 100, "igualar cuesta el doble del ante");
        t.call(1);
        check(t.seats[1].call == 100 && t.seats[1].state == SeatState.CALLED, "igualar");
        check(!t.allDecided(), "falta uno");
        t.fold(3);
        check(t.allDecided(), "todos decidieron");
        t.startShowdown();
        check(t.phase == Phase.SHOWDOWN, "showdown");
        t.showdownStep();
        check(t.board.size() == 4, "turn");
        t.showdownStep();
        check(t.board.size() == 5, "river");
        t.showdownStep();
        check(t.dealerRevealed, "crupier muestra");
        t.showdownStep();
        check(t.phase == Phase.RESULT, "resultado");
        check(t.seats[3].result == Result.FOLD && t.seats[3].payout == 0, "retirado pierde el ante");
        check(t.seats[1].result != Result.NONE, "el que igualo tiene resultado");
        t.clearRound();
        check(t.phase == Phase.IDLE && t.board.isEmpty() && t.seats[1].ante == 0, "limpiar");

        // todos se retiran: termina sin showdown
        t.placeAnte(0, 100);
        t.deal(new Random(2));
        t.foldUndecided();
        t.startShowdown();
        check(t.phase == Phase.RESULT && t.seats[0].result == Result.FOLD, "todos retirados: resultado directo");
        t.clearRound();

        // ---- pagos con manos armadas a mano
        t.placeAnte(0, 100);
        t.deal(new Random(3));
        t.seats[0].hole.clear();
        t.seats[0].hole.addAll(h("As", "Ah"));
        t.dealer.clear();
        t.dealer.addAll(h("Kd", "Qc"));
        t.board.clear();
        t.board.addAll(h("2d", "7c", "9h"));
        t.future.clear();
        t.future.addAll(h("3s", "5c"));
        t.call(0);
        t.startShowdown();
        while (t.phase != Phase.RESULT) {
            t.showdownStep();
        }
        check(!t.dealerQualified, "K-Q alta no califica");
        check(t.seats[0].result == Result.WIN_NQ && t.seats[0].payout == 100 * 2 + 200, "no califica: ante paga 1:1 y la igualacion se devuelve = " + t.seats[0].payout);
        t.clearRound();

        t.placeAnte(0, 100);
        t.deal(new Random(3));
        t.seats[0].hole.clear();
        t.seats[0].hole.addAll(h("As", "Ah"));
        t.dealer.clear();
        t.dealer.addAll(h("Kd", "Kc"));
        t.board.clear();
        t.board.addAll(h("2d", "7c", "9h"));
        t.future.clear();
        t.future.addAll(h("3s", "5c"));
        t.call(0);
        t.startShowdown();
        while (t.phase != Phase.RESULT) {
            t.showdownStep();
        }
        check(t.dealerQualified, "par de reyes califica");
        check(t.seats[0].result == Result.WIN && t.seats[0].payout == 100 * 2 + 200 * 2, "ases ganan a reyes: ante 1:1 + igualacion 1:1 = " + t.seats[0].payout);
        t.clearRound();
        // gana el crupier (par de reyes contra carta alta del jugador) y empate
        t.placeAnte(0, 100);
        t.deal(new Random(3));
        t.seats[0].hole.clear();
        t.seats[0].hole.addAll(h("As", "3h"));
        t.dealer.clear();
        t.dealer.addAll(h("Kd", "Kc"));
        t.board.clear();
        t.board.addAll(h("2d", "7c", "9h"));
        t.future.clear();
        t.future.addAll(h("Js", "5c"));
        t.call(0);
        t.startShowdown();
        while (t.phase != Phase.RESULT) {
            t.showdownStep();
        }
        check(t.seats[0].result == Result.LOSE && t.seats[0].payout == 0, "gana el crupier");
        t.clearRound();
        t.placeAnte(0, 100);
        t.deal(new Random(3));
        t.seats[0].hole.clear();
        t.seats[0].hole.addAll(h("2s", "3h"));
        t.dealer.clear();
        t.dealer.addAll(h("2d", "3c"));
        t.board.clear();
        t.board.addAll(h("Ad", "Ac", "9h"));
        t.future.clear();
        t.future.addAll(h("Js", "5c"));
        t.call(0);
        t.startShowdown();
        while (t.phase != Phase.RESULT) {
            t.showdownStep();
        }
        check(t.seats[0].result == Result.PUSH && t.seats[0].payout == 300, "empate: se devuelve todo (" + t.seats[0].payout + ")");
        t.clearRound();
        // simulacion del retorno con una estrategia sencilla (igualar con par o mejor, con As, o con 4 cartas del mismo palo)
        Random r2 = new Random(11);
        long bet = 0, back = 0;
        int sims = 400000;
        for (int i = 0; i < sims; i++) {
            PokerTable x = new PokerTable();
            x.placeAnte(0, 10);
            x.deal(r2);
            List<Integer> five = new ArrayList<>(x.seats[0].hole);
            five.addAll(x.board);
            int cat5 = PokerHands.category(PokerHands.best(five));
            boolean ace = x.seats[0].hole.stream().anyMatch(cc -> PokerHands.rank(cc) == 14);
            int[] suits = new int[4];
            for (int cc : five) {
                suits[PokerHands.suit(cc)]++;
            }
            boolean flushDraw = Arrays.stream(suits).max().getAsInt() >= 4;
            bet += 10;
            if (cat5 >= PokerHands.PAIR || ace || flushDraw) {
                x.call(0);
                bet += 20;
            } else {
                x.fold(0);
            }
            x.startShowdown();
            while (x.phase != Phase.RESULT) {
                x.showdownStep();
            }
            back += x.seats[0].payout;
        }
        double rtp = back / (double) bet;
        System.out.println("Retorno simulado con estrategia sencilla: " + String.format("%.2f%%", rtp * 100));
        check(rtp > 0.90 && rtp < 1.0, "retorno razonable (la casa gana un poco): " + rtp);
        System.out.println(fails == 0 ? "PokerTest: todo bien" : "PokerTest: " + fails + " fallas");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
