import com.casino.game.LiarsDice;

import java.util.Random;

/**
 * Dados del mentiroso: reglas (subidas legales, conteo con comodines, quién gana al gritar) y partidas del tahúr contra jugadores
 * simulados (uno que nunca grita, uno que grita al azar, uno que juega como el tahúr y uno muy listo), para revisar cuánto paga.
 */
public class DiceTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    interface Brain {
        void play(LiarsDice g, Random r);
    }

    static double rate(String name, Brain player) {
        Random r = new Random(3);
        int n = 20000, win = 0;
        for (int i = 0; i < n; i++) {
            LiarsDice g = new LiarsDice(r.nextLong());
            int guard = 0;
            while (!g.over() && guard++ < 40) {
                if (g.turn == LiarsDice.PLAYER) {
                    player.play(g, r);
                } else {
                    g.think(LiarsDice.NPC, LiarsDice.NPC_CALL, LiarsDice.NPC_BLUFF);
                }
            }
            if (g.winner == LiarsDice.PLAYER) {
                win++;
            }
        }
        double p = win / (double) n;
        System.out.printf("%-14s gana %3.0f%%, devuelve %3.0f%%%n", name, p * 100, p * LiarsDice.PAY_X10 * 10);
        return p * LiarsDice.PAY_X10 / 10.0;
    }

    public static void main(String[] a) {
        System.out.println("DiceTest");
        LiarsDice g = new LiarsDice(5);
        g.dice[0] = new int[]{1, 3, 3, 5, 6};
        g.dice[1] = new int[]{3, 1, 2, 2, 4};
        check(g.count(3) == 5 && g.count(2) == 4 && g.count(6) == 3, "los unos cuentan como comodines");
        check(g.bid(0, 3, 3) && !g.legal(3, 2) && !g.legal(2, 6) && g.legal(3, 4) && g.legal(4, 2), "cada apuesta debe subir");
        check(!g.bid(0, 4, 4), "no se puede apostar dos veces seguidas");
        check(!g.legal(5, 1), "no se apuesta a los unos");
        check(g.bid(1, 6, 3) && g.call(0) && g.winner == 0, "seis treses es mentira (hay cinco): gana quien gritó");
        LiarsDice h = new LiarsDice(5);
        h.dice[0] = new int[]{1, 3, 3, 5, 6};
        h.dice[1] = new int[]{3, 1, 2, 2, 4};
        check(h.bid(0, 5, 3) && h.call(1) && h.winner == 0, "cinco treses es verdad: gana quien apostó");
        double never = rate("nunca grita", (x, r) -> {
            LiarsDice.Bid b = x.last();
            int q = b == null ? 2 : b.face() < 6 ? b.qty() : b.qty() + 1;
            int f = b == null ? 2 + r.nextInt(5) : b.face() < 6 ? b.face() + 1 : 2;
            if (!x.bid(0, q, f)) {
                x.call(0);
            }
        });
        double random = rate("al azar", (x, r) -> {
            if (x.last() != null && r.nextInt(3) == 0) {
                x.call(0);
            } else {
                x.think(0, 0.0, 1.0);
            }
        });
        double same = rate("como el tahúr", (x, r) -> x.think(0, LiarsDice.NPC_CALL, LiarsDice.NPC_BLUFF));
        double smart = rate("listo", (x, r) -> x.think(0, 0.45, 0.05));
        check(never < 0.6, "el que nunca grita pierde");
        check(random < 0.95, "el que juega al azar pierde");
        check(same > 0.75 && same < 1.05, "jugando como el tahúr casi se recupera (la casa gana un poco)");
        check(smart < 1.3, "ni el más listo se hace rico");
        System.out.println("DiceTest OK");
    }
}
