import com.casino.game.ArmRules;

import java.util.Random;

/**
 * Vencidas con jugadores simulados que aprietan dos teclas alternando (cada uno a su ritmo, cansándose un poco). Revisa el ritmo
 * suavizado, que no se pueda ganar sin apretar, que el más rápido gane más y cuánto paga cada forzudo.
 */
public class ArmTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    static int poisson(double lambda, Random r) {
        double l = Math.exp(-lambda), p = 1;
        int k = 0;
        do {
            k++;
            p *= r.nextDouble();
        } while (p > l);
        return k - 1;
    }

    /** Una lucha contra el forzudo opp de alguien que aprieta a 'speed' golpes/s (varía de lucha en lucha). Devuelve si gana. */
    static boolean match(int opp, double speed, double sd, Random r) {
        double me = Math.max(1, speed + r.nextGaussian() * sd);
        double x = 0, r0 = 0, r1 = 0, phase = r.nextDouble() * 6.28;
        for (int t = 0; t < ArmRules.MAX_TICKS; t++) {
            double tired = 1 - 0.012 * t / 20.0;
            r0 = ArmRules.rate(r0, poisson(me * tired / ArmRules.TPS, r));
            r1 = ArmRules.npcRate(opp, t, phase);
            x = ArmRules.step(x, r0, r1, r.nextGaussian());
            int w = ArmRules.winner(x);
            if (w >= 0) {
                return w == 0;
            }
        }
        return x > 0;
    }

    static double[] stats(String name, double speed, double sd) {
        double[] out = new double[ArmRules.OPPONENTS];
        StringBuilder sb = new StringBuilder(String.format("%-8s", name));
        for (int o = 0; o < ArmRules.OPPONENTS; o++) {
            Random r = new Random(11 + o);
            int n = 8000, win = 0;
            for (int i = 0; i < n; i++) {
                if (match(o, speed, sd, r)) {
                    win++;
                }
            }
            double p = win / (double) n;
            out[o] = p * ArmRules.PAY_X10[o] / 10.0;
            sb.append(String.format(" | forzudo %d: gana %3.0f%%, devuelve %3.0f%%", o, p * 100, out[o] * 100));
        }
        System.out.println(sb);
        return out;
    }

    public static void main(String[] a) {
        System.out.println("ArmTest");
        double r = 0;
        for (int i = 0; i < 100; i++) {
            r = ArmRules.rate(r, i % 2);   // un golpe cada dos ticks = 10 golpes/s
        }
        check(Math.abs(r - 10) < 1.5, "el ritmo suavizado llega a los golpes por segundo reales (" + Math.round(r) + ")");
        check(ArmRules.rate(0, 50) <= ArmRules.MAX_TAPS_PER_TICK * ArmRules.TPS * ArmRules.EMA + 1e-9, "no cuentan golpes imposibles");
        double x = 0;
        int t = 0;
        for (; t < ArmRules.MAX_TICKS && ArmRules.winner(x) < 0; t++) {
            x = ArmRules.step(x, 0, ArmRules.npcRate(0, t, 0), 0);
        }
        check(ArmRules.winner(x) == 1 && t < 15 * 20, "si no aprietas nada, hasta el forzudo más débil te gana pronto (" + t / 20 + " s)");
        check(ArmRules.winner(1.0) == 0 && ArmRules.winner(-1.0) == 1 && ArmRules.winner(0.5) == -1, "quién gana");
        check(ArmRules.payout(100, 1) == 200 && ArmRules.pvpPayout(100) == 190, "premios");
        double[] slow = stats("lento", 5.5, 1.0);
        double[] avg = stats("normal", 7.5, 1.2);
        double[] fast = stats("rápido", 10.0, 1.2);
        for (int o = 0; o < ArmRules.OPPONENTS; o++) {
            check(fast[o] > avg[o] && avg[o] > slow[o], "el más rápido gana más contra el forzudo " + o);
            check(slow[o] < 0.8, "el lento pierde contra el forzudo " + o);
            check(avg[o] < 1.1, "el normal no se hace rico contra el forzudo " + o);
        }
        check(avg[0] > 0.6, "contra Gus un jugador normal casi se recupera");
        System.out.println("ArmTest OK");
    }
}
