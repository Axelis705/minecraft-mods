import com.casino.game.DuelRules;

import java.util.Random;

/**
 * Duelo al mediodía con tiradores simulados: el tiempo para levantar el revólver y apuntar (desde mirar al suelo) y la puntería de
 * cada disparo. Revisa que el grito llegue en un tiempo razonable, que el más rápido gane más seguido y cuánto paga cada pistolero.
 */
public class DuelTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    /** Un duelo: react = ticks promedio hasta el primer disparo (desde el grito), sd, hit = puntería por disparo. Devuelve si gana. */
    static boolean duel(long seed, int opp, double react, double sd, double hit, Random rnd) {
        int draw = DuelRules.drawTick(seed);
        int kill = DuelRules.npcKillTick(seed, opp);
        double t = draw + Math.max(5, react + rnd.nextGaussian() * sd);
        for (int s = 0; s < DuelRules.SHOTS; s++) {
            int tick = (int) Math.ceil(t);
            if (tick >= kill) {
                return false;    // empate o tarde: gana el pistolero
            }
            if (rnd.nextDouble() < hit) {
                return true;
            }
            t += 7 + rnd.nextGaussian();
        }
        return false;
    }

    static double[] stats(String name, double react, double sd, double hit) {
        double[] out = new double[DuelRules.OPPONENTS];
        StringBuilder sb = new StringBuilder(String.format("%-8s", name));
        for (int o = 0; o < DuelRules.OPPONENTS; o++) {
            Random rnd = new Random(9 + o);
            int n = 20000, win = 0;
            for (int i = 0; i < n; i++) {
                if (duel(rnd.nextLong(), o, react, sd, hit, rnd)) {
                    win++;
                }
            }
            double p = win / (double) n;
            out[o] = p * DuelRules.PAY_X10[o] / 10.0;
            sb.append(String.format(" | rival %d: gana %3.0f%%, devuelve %3.0f%%", o, p * 100, out[o] * 100));
        }
        System.out.println(sb);
        return out;
    }

    public static void main(String[] a) {
        System.out.println("DuelTest");
        int lo = Integer.MAX_VALUE, hi = 0;
        for (long s = 0; s < 2000; s++) {
            int d = DuelRules.drawTick(s);
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
            for (int o = 0; o < DuelRules.OPPONENTS; o++) {
                if (DuelRules.npcKillTick(s, o) <= d) {
                    throw new AssertionError("el pistolero disparó antes del grito");
                }
                if (DuelRules.npcKillTick(s, o) - d > DuelRules.LAST_CHANCE + DuelRules.REFIRE) {
                    throw new AssertionError("el pistolero tardó demasiado");
                }
            }
        }
        check(lo >= DuelRules.TOLL_3 + DuelRules.WAIT_MIN && hi <= DuelRules.TOLL_3 + DuelRules.WAIT_MAX, "el grito llega entre 1 y 3.5 s después de la tercera campanada");
        check(DuelRules.drawTick(77) == DuelRules.drawTick(77) && DuelRules.npcKillTick(77, 2) == DuelRules.npcKillTick(77, 2), "misma semilla, mismo duelo");
        check(DuelRules.noon(6000) && DuelRules.noon(30000) && !DuelRules.noon(13000), "mediodía de 11:00 a 13:00");
        check(DuelRules.payout(100, 1, false) == 200 && DuelRules.payout(100, 1, true) == 250, "premios");
        double[] slow = stats("lento", 18, 3, 0.55);
        double[] avg = stats("normal", 13, 2.5, 0.70);
        double[] fast = stats("rápido", 9, 2, 0.85);
        for (int o = 0; o < DuelRules.OPPONENTS; o++) {
            check(fast[o] > avg[o] && avg[o] > slow[o], "el más rápido gana más contra el rival " + o);
            check(slow[o] < 0.8, "el lento pierde contra el rival " + o);
            check(avg[o] < 1.05, "el normal no se hace rico contra el rival " + o);
        }
        check(avg[0] > 0.6, "contra Kid Pecas un tirador normal casi se recupera");
        System.out.println("DuelTest OK");
    }
}
