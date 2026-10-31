import com.pooltable.game.BowlingAim;
import com.pooltable.game.BowlingPhysics;

import java.util.Random;

/** Calibra la dificultad: jugadores de distinto nivel (qué tan bien sueltan en la zona verde y qué tan bien apuntan) y cuántas chuzas hacen. */
public class BowlingTuneTest {
    static boolean strict = false;

    static double[] play(double timingSd, double aimSd, double aimV, int n, long seed) {
        Random r = new Random(seed);
        BowlingPhysics ph = new BowlingPhysics();
        long total = 0, strikes = 0, gutters = 0, zero = 0;
        for (int i = 0; i < n; i++) {
            double m = Math.max(0, Math.min(1, BowlingAim.SWEET + r.nextGaussian() * timingSd));
            BowlingAim a = BowlingAim.shoot(m, r);
            double v = Math.max(-0.55, Math.min(0.55, aimV + r.nextGaussian() * aimSd + a.lateralErr));
            double ang = Math.max(-10, Math.min(10, a.angleErr + r.nextGaussian() * aimSd * 8));
            ph.reset(0x3FF);
            ph.launch(v, ang, a.speed, a.spinErr);
            int steps = 0;
            while (!ph.settled() && steps < 240 * 20) {
                ph.step(1.0 / 240);
                steps++;
            }
            int down = 10 - ph.standingCount();
            total += down;
            if (down == 10) strikes++;
            if (down == 0) zero++;
        }
        return new double[]{total / (double) n, 100.0 * strikes / n, 100.0 * zero / n};
    }

    public static void main(String[] args) {
        String[] names = {"Muy bueno (suelta siempre en el verde, apunta fino)", "Bueno", "Normal", "Malo", "Pixel fijo y solo carga (el truco anterior)"};
        double[][] cfg = {{0.03, 0.02, -0.05}, {0.05, 0.04, -0.05}, {0.12, 0.08, -0.05}, {0.25, 0.14, -0.05}, {0.04, 0.0, -0.05}};
        double[] strikes = new double[5];
        for (int k = 0; k < 5; k++) {
            double[] r = play(cfg[k][0], cfg[k][1], cfg[k][2], 6000, 3 + k);
            strikes[k] = r[1];
            System.out.printf("%-52s promedio %.2f pinos, chuzas %.1f%%, ceros %.1f%%%n", names[k], r[0], r[1], r[2]);
        }
        boolean ok = true;
        // dificil pero posible: el mejor jugador hace entre 10% y 28% de chuzas, un jugador malo bastante menos, y el truco del pixel fijo no pasa de 28%
        ok &= strikes[0] >= 10 && strikes[0] <= 28;
        ok &= strikes[3] < strikes[0];
        ok &= strikes[4] <= 28;
        // la zona verde y el indicador
        ok &= BowlingAim.meter(0) == 0.0 && BowlingAim.meter(BowlingAim.PERIOD / 2.0) == 1.0 && BowlingAim.meter(BowlingAim.PERIOD) < 1e-9;
        for (double t = 0; t < 200; t += 0.37) {
            double m = BowlingAim.meter(t);
            ok &= m >= 0 && m <= 1;
        }
        ok &= BowlingAim.miss(BowlingAim.SWEET) == 0 && BowlingAim.miss(0.3) > 0.4 && BowlingAim.miss(1.0) > 0.05;
        System.out.println(ok ? "BowlingTuneTest: TODO BIEN" : "BowlingTuneTest: FALLA");
        if (!ok) {
            System.exit(1);
        }
    }
}
