import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackGame.Phase;
import com.casino.game.BlackjackGame.Result;

import java.util.List;
import java.util.Random;

/** Pruebas de las reglas del blackjack (sin Minecraft). */
public class BlackjackTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            System.out.println("FALLA: " + what);
        }
    }

    // carta = rank + 13*palo ; rank 0 = As, 1..8 = 2..9, 9..12 = 10 J Q K
    static int c(int rank) {
        return rank;
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
        check(BlackjackGame.total(List.of(c(1) + 13, c(2) + 26)) == 5, "los palos no cambian el valor");

        // partidas al azar: invariantes
        Random rnd = new Random(7);
        long bet = 0, back = 0, games = 0, wins = 0, bj = 0, busts = 0;
        for (int i = 0; i < 400000; i++) {
            BlackjackGame g = new BlackjackGame();
            int b = BlackjackGame.BETS[rnd.nextInt(BlackjackGame.BETS.length)];
            g.start(rnd, b);
            check(g.player.size() == 2 && g.dealer.size() == 2, "reparto inicial 2 y 2");
            // estrategia simple: pide hasta 16 (ignora al crupier), a veces dobla con 10/11
            int guard = 0;
            while (g.phase == Phase.PLAYER && guard++ < 30) {
                int t = g.playerTotal();
                if (g.canDouble() && (t == 10 || t == 11) && rnd.nextInt(2) == 0) {
                    g.doubleDown(rnd);
                } else if (t < 17) {
                    g.hit(rnd);
                } else {
                    g.stand();
                }
            }
            guard = 0;
            while (g.phase == Phase.DEALER && guard++ < 30) {
                g.dealerStep(rnd);
            }
            check(g.phase == Phase.DONE, "la mano termina");
            check(g.result != Result.NONE, "hay resultado");
            check(g.holeRevealed, "al final la carta tapada está visible");
            int dt = BlackjackGame.total(g.dealer);
            if (g.result == Result.WIN || g.result == Result.LOSE || g.result == Result.PUSH) {
                if (!BlackjackGame.isBlackjack(g.dealer)) {
                    check(dt >= 17, "el crupier no se planta con menos de 17 (" + dt + ")");
                }
            }
            if (g.result == Result.BUST) {
                check(g.playerTotal() > 21, "BUST solo si se pasó");
                check(g.payout == 0, "BUST no paga");
            }
            if (g.result == Result.BLACKJACK) {
                check(g.payout == b * 5 / 2 && !g.doubled, "blackjack paga 3 a 2");
                bj++;
            }
            if (g.result == Result.WIN) {
                check(g.payout == g.bet * 2, "ganar paga el doble de lo apostado");
                wins++;
            }
            if (g.result == Result.PUSH) {
                check(g.payout == g.bet, "empate devuelve la apuesta");
            }
            if (g.result == Result.BUST) {
                busts++;
            }
            bet += g.bet;
            back += g.payout;
            games++;
        }
        double rtp = 100.0 * back / bet;
        System.out.printf("Blackjack: %d manos, victorias %.1f%%, blackjacks %.2f%%, pasadas %.1f%%, retorno %.1f%% (estrategia floja)%n",
                games, 100.0 * wins / games, 100.0 * bj / games, 100.0 * busts / games, rtp);
        check(rtp > 80 && rtp < 100, "el retorno con estrategia floja debe estar entre 80% y 100% (" + rtp + ")");
        check(100.0 * bj / games > 4.0 && 100.0 * bj / games < 5.5, "frecuencia de blackjack natural ~4.8%");

        // estrategia basica sencilla (aprox.) para comprobar que la casa gana poco: pide con <12, se planta con 12+ si el crupier muestra 2..6, si no hasta 17; dobla 10/11
        back = 0; bet = 0;
        for (int i = 0; i < 600000; i++) {
            BlackjackGame g = new BlackjackGame();
            g.start(rnd, 10);
            int up = BlackjackGame.rank(g.dealer.get(0));
            int upv = up == 0 ? 11 : Math.min(10, up + 1);
            int guard = 0;
            while (g.phase == Phase.PLAYER && guard++ < 30) {
                int t = g.playerTotal();
                boolean soft = BlackjackGame.soft(g.player);
                if (g.canDouble() && (t == 11 || (t == 10 && upv < 10) || (soft && t >= 15 && t <= 18 && upv >= 4 && upv <= 6))) {
                    g.doubleDown(rnd);
                } else if (soft ? t < 18 : t < 12 || (t < 17 && upv >= 7)) {
                    g.hit(rnd);
                } else {
                    g.stand();
                }
            }
            while (g.phase == Phase.DEALER) {
                g.dealerStep(rnd);
            }
            bet += g.bet;
            back += g.payout;
        }
        double rtp2 = 100.0 * back / bet;
        System.out.printf("Con estrategia basica sencilla: retorno %.2f%% (la casa debe quedar ligeramente arriba)%n", rtp2);
        check(rtp2 > 97 && rtp2 < 100.5, "retorno con estrategia basica entre 97% y 100.5% (" + rtp2 + ")");

        System.out.println(fails == 0 ? "BlackjackTest: TODO BIEN" : "BlackjackTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
