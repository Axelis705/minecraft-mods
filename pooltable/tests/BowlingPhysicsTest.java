import com.pooltable.game.BowlingPhysics;

import java.util.Random;

/** Pruebas de la física del boliche: termina siempre, nada se sale de la pista, y el "tacto" (cuántos pinos caen) es razonable. */
public class BowlingPhysicsTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            if (fails < 30) {
                System.out.println("FALLA: " + what);
            }
        }
    }

    /** Simula un tiro y devuelve los pinos que cayeron; comprueba invariantes en cada paso. */
    static int shoot(BowlingPhysics ph, int mask, double v, double angle, double speed, double spin) {
        ph.reset(mask);
        int before = Integer.bitCount(mask);
        ph.launch(v, angle, speed, spin);
        double dt = 1.0 / 240.0;
        int steps = 0;
        while (!ph.settled() && steps < 240 * 20) {
            ph.step(dt);
            steps++;
            check(!Double.isNaN(ph.bu) && !Double.isNaN(ph.bv), "bola sin NaN");
            for (int i = 0; i < BowlingPhysics.PINS; i++) {
                check(!Double.isNaN(ph.pu[i]) && !Double.isNaN(ph.pv[i]), "pino sin NaN");
                check(Math.abs(ph.pu[i]) < 40 && Math.abs(ph.pv[i]) < 10, "pino cerca de la pista");
                if (ph.state[i] == BowlingPhysics.GONE && (mask & (1 << i)) == 0) {
                    check(true, "");
                }
            }
        }
        check(ph.settled(), "el tiro termina (" + steps + " pasos)");
        int after = ph.standingCount();
        check(after <= before, "no aparecen pinos");
        check((ph.standingMask() & ~mask) == 0, "solo quedan de pie los que estaban");
        return before - after;
    }

    public static void main(String[] a) {
        BowlingPhysics ph = new BowlingPhysics();
        // geometria: 10 pinos en triangulo, sin solapes
        for (int i = 0; i < 10; i++) {
            for (int j = i + 1; j < 10; j++) {
                double d = Math.hypot(BowlingPhysics.spotU(i) - BowlingPhysics.spotU(j), BowlingPhysics.spotV(i) - BowlingPhysics.spotV(j));
                check(d > 2 * BowlingPhysics.PIN_R + 0.1, "pinos " + i + " y " + j + " separados");
            }
        }
        check(Math.abs(BowlingPhysics.spotV(0)) < 1e-9 && Math.abs(BowlingPhysics.spotV(1) + BowlingPhysics.spotV(2)) < 1e-9, "triangulo simetrico");
        check(BowlingPhysics.spotU(9) < BowlingPhysics.DECK_END_U, "el ultimo pino esta dentro de la mesa");

        // bola al centro: tira varios pinos; bola a la canaleta: cero
        int center = shoot(ph, 0x3FF, 0.0, 0, 6.0, 0);
        System.out.println("Bola al centro: " + center + " pinos");
        check(center >= 1, "una bola al centro tira algo");
        int gutter = shoot(ph, 0x3FF, 0.7, 6, 6.0, 0);
        check(gutter == 0, "bola a la canaleta tira 0 (tiro " + gutter + ")");
        int wide = shoot(ph, 0x3FF, -0.7, -8, 6.0, 0);
        check(wide == 0, "bola por la izquierda a la canaleta tira 0 (tiro " + wide + ")");
        check(ph.inGutter, "queda marcada como canaleta");

        // una bola lenta casi siempre llega; una bola con efecto se curva hacia donde se le da
        shoot(ph, 0x3FF, -0.3, 0, 6.0, 0.8);
        check(ph.bv > -0.3 || !ph.ballActive, "el efecto positivo la mueve a la derecha");

        // "tacto": jugador decente apunta al bolsillo (entre el pino 1 y el 3, a la derecha del centro) con algo de error
        Random r = new Random(11);
        long total = 0, strikes = 0, n = 20000, gutters = 0, zero = 0;
        int[] hist = new int[11];
        for (int i = 0; i < n; i++) {
            double v = 0.12 + r.nextGaussian() * 0.08;
            double ang = r.nextGaussian() * 1.2;
            double speed = 7.4 + r.nextGaussian() * 0.5;
            double spin = r.nextGaussian() * 0.15;
            int p = shoot(ph, 0x3FF, v, ang, speed, spin);
            total += p;
            hist[p]++;
            if (p == 10) {
                strikes++;
            }
            if (ph.inGutter) {
                gutters++;
            }
            if (p == 0) {
                zero++;
            }
        }
        System.out.printf("Jugador decente: promedio %.2f pinos, chuzas %.1f%%, canaletas %.1f%%%n", total / (double) n, 100.0 * strikes / n, 100.0 * gutters / n);
        System.out.println("Histograma: " + java.util.Arrays.toString(hist));
        check(total / (double) n > 6.5 && total / (double) n < 9.2, "promedio de un jugador decente entre 6,5 y 9,2");
        check(100.0 * strikes / n > 5 && 100.0 * strikes / n < 45, "chuzas entre 5% y 45%");

        // jugador malo: apunta al azar en toda la tabla
        total = 0; strikes = 0;
        for (int i = 0; i < n; i++) {
            double v = (r.nextDouble() * 2 - 1) * 0.7;
            double ang = r.nextGaussian() * 4;
            double speed = 4.0 + r.nextDouble() * 4.0;
            int p = shoot(ph, 0x3FF, v, ang, speed, r.nextGaussian() * 0.5);
            total += p;
            if (p == 10) {
                strikes++;
            }
        }
        System.out.printf("Jugador al azar: promedio %.2f pinos, chuzas %.2f%%%n", total / (double) n, 100.0 * strikes / n);
        check(total / (double) n > 2.5 && total / (double) n < 7.0, "promedio al azar entre 2,5 y 7");

        // segundo tiro: con pinos faltantes
        int second = shoot(ph, 0b1000000101, -0.4, 2, 6, 0);
        check(second >= 0 && second <= 3, "segundo tiro con 3 pinos");
        // los pinos que ya no estan no cuentan ni estorban
        ph.reset(0b0000000001);
        check(ph.standingCount() == 1 && ph.state[1] == BowlingPhysics.GONE, "reset con un solo pino");

        System.out.println(fails == 0 ? "BowlingPhysicsTest: TODO BIEN" : "BowlingPhysicsTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
