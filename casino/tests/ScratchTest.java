import com.casino.game.ScratchCards;

import java.util.Random;

public class ScratchTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        if (!ok) {
            fails++;
            System.out.println("FALLA: " + msg);
        }
    }

    public static void main(String[] a) {
        check(ScratchCards.winningSymbol(new int[]{1, 1, 1, 2, 3, 4}) == 1, "tres iguales ganan");
        check(ScratchCards.winningSymbol(new int[]{1, 1, 2, 2, 3, 3}) == -1, "dos pares no ganan");
        check(ScratchCards.prizeOf(new int[]{7, 7, 7, 0, 0, 1}) == 5000, "premio gordo");
        check(ScratchCards.prizeOf(new int[]{0, 1, 2, 3, 4, 5}) == 0, "sin premio");
        Random rnd = new Random(5);
        int N = 3_000_000;
        long paid = 0;
        int wins = 0;
        int[] tier = new int[ScratchCards.SYMBOLS];
        for (int i = 0; i < N; i++) {
            int[] s = ScratchCards.generate(rnd);
            check(s.length == ScratchCards.SPOTS, "6 casillas");
            int[] cnt = new int[ScratchCards.SYMBOLS];
            for (int x : s) {
                check(x >= 0 && x < ScratchCards.SYMBOLS, "simbolo valido");
                cnt[x]++;
            }
            int triples = 0;
            for (int c : cnt) {
                if (c >= 3) {
                    triples++;
                }
                check(c <= 3, "nunca mas de 3 iguales");
            }
            check(triples <= 1, "como mucho un premio por boleto");
            int w = ScratchCards.winningSymbol(s);
            if (w >= 0) {
                wins++;
                tier[w]++;
                paid += ScratchCards.PRIZES[w];
            }
            if (fails > 20) {
                break;
            }
        }
        double rtp = paid / (double) (N * (long) ScratchCards.PRICE);
        System.out.printf("Boletos: %d, con premio: %.2f%%, retorno simulado: %.2f%% (teorico %.2f%%)%n", N, 100.0 * wins / N, rtp * 100, ScratchCards.expectedReturn() * 100);
        for (int i = 0; i < tier.length; i++) {
            System.out.printf("  premio %d: %d veces%n", ScratchCards.PRIZES[i], tier[i]);
        }
        check(Math.abs(rtp - ScratchCards.expectedReturn()) < 0.03, "el retorno simulado coincide con el teorico");
        check(ScratchCards.expectedReturn() > 0.85 && ScratchCards.expectedReturn() < 0.97, "la casa gana un poco");
        check(Math.abs(100.0 * wins / N - ScratchCards.winChance() * 100) < 0.3, "frecuencia de premios");
        System.out.println(fails == 0 ? "ScratchTest: todo bien" : "ScratchTest: " + fails + " fallas");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
