import com.casino.game.BullRide;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * Toro mecánico con jinetes simulados: uno que no hace nada, uno lento y distraído, uno normal y uno muy bueno. Revisa que la
 * dificultad tenga sentido (nadie aguanta sin tocar nada, el normal pasa los 8 s a veces, el bueno muy rara vez lo doma) y cuánto paga.
 */
public class BullTest {
    static final class Rider {
        final int delay;
        final double look, th, lapse;

        Rider(int delay, double look, double th, double lapse) {
            this.delay = delay;
            this.look = look;
            this.th = th;
            this.lapse = lapse;
        }
    }

    static int ride(Rider r, long seed, Random rnd) {
        BullRide b = new BullRide(seed);
        ArrayDeque<double[]> seen = new ArrayDeque<>();
        int frozen = 0;
        int last = 0;
        while (!b.tamed()) {
            seen.addLast(new double[]{b.lean, b.vel});
            int in = 0;
            if (r != null && seen.size() > r.delay) {
                double[] s = seen.removeFirst();
                if (frozen > 0) {
                    frozen--;
                    in = last;
                } else {
                    if (rnd.nextDouble() < r.lapse / BullRide.TPS) {
                        frozen = 6;
                    }
                    double pred = s[0] + s[1] * r.look;
                    in = pred > r.th ? -1 : pred < -r.th ? 1 : 0;
                }
            }
            last = in;
            if (!b.step(in)) {
                break;
            }
        }
        return b.t;
    }

    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    static double[] stats(String name, Rider r) {
        Random rnd = new Random(42);
        int n = 4000;
        long bet = 100, paid = 0;
        int p3 = 0, p8 = 0, p12 = 0, p20 = 0;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            int t = ride(r, rnd.nextLong(), rnd);
            sum += t / 20.0;
            paid += BullRide.payout(bet, t);
            if (t >= 80) p3++;
            if (t >= 160) p8++;
            if (t >= 240) p12++;
            if (t >= BullRide.MAX_TICKS) p20++;
        }
        double rtp = paid / (double) (bet * n);
        System.out.printf("%-10s media %.1f s | >=4s %4.0f%% | >=8s %4.0f%% | >=12s %4.0f%% | domado %4.1f%% | devuelve %.0f%%%n", name,
                sum / n, 100.0 * p3 / n, 100.0 * p8 / n, 100.0 * p12 / n, 100.0 * p20 / n, rtp * 100);
        return new double[]{sum / n, p3 / (double) n, p8 / (double) n, p20 / (double) n, rtp};
    }

    public static void main(String[] a) {
        System.out.println("BullTest");
        double[] none = stats("quieto", null);
        double[] slow = stats("lento", new Rider(7, 0.6, 0.30, 0.6));
        double[] avg = stats("normal", new Rider(5, 0.5, 0.30, 0.3));
        double[] good = stats("bueno", new Rider(3, 0.4, 0.15, 0.1));
        check(none[2] < 0.02, "sin tocar nada casi nadie llega a 8 s");
        check(none[4] < 0.15, "sin tocar nada se pierde casi todo");
        check(slow[4] < 0.85, "el jinete lento pierde dinero");
        check(avg[2] > 0.10 && avg[2] < 0.50, "el jinete normal pasa los 8 s a veces");
        check(avg[4] > 0.6 && avg[4] < 1.15, "el jinete normal no se hace rico");
        check(good[3] > 0.0005, "un jinete muy bueno puede domar al toro (muy rara vez)");
        check(good[4] < 2.5, "ni el mejor jinete gana sin riesgo");
        // mismo número de semilla = misma montada (cliente y servidor no la recalculan, pero debe ser estable)
        Random r1 = new Random(1), r2 = new Random(1);
        check(ride(new Rider(5, 0.3, 0.12, 0.3), 77, r1) == ride(new Rider(5, 0.3, 0.12, 0.3), 77, r2), "misma semilla, misma montada");
        check(BullRide.payout(100, 79) == 0 && BullRide.payout(100, 80) == 100 && BullRide.payout(100, BullRide.MAX_TICKS) == 1000, "tabla de premios");
        System.out.println("BullTest OK");
    }
}
