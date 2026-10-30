import com.casino.game.RouletteWheel;
import com.casino.game.RouletteWheel.Ball;
import java.util.Random;

public class RouletteWheelTest {
    static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    static double wrap(double a) {
        double m = a % (2 * Math.PI);
        if (m < -Math.PI) m += 2 * Math.PI;
        if (m > Math.PI) m -= 2 * Math.PI;
        return m;
    }

    public static void main(String[] args) {
        Random rnd = new Random(5);
        double minP = 99, maxP = 0, maxStep = 0, maxDh = 0, maxDr = 0;
        int cases = 0;
        long totalTicks = 0;
        for (int number = 0; number <= 36; number++) {
            for (int k = 0; k < 400; k++) {
                int seed = rnd.nextInt();
                long start = 1000 + rnd.nextInt(2_000_000);
                int seq = 1 + rnd.nextInt(50);
                double p = RouletteWheel.fallExponent(number, seed, start, seq);
                minP = Math.min(minP, p);
                maxP = Math.max(maxP, p);
                check(p >= 1.3 && p <= 3.3, "exponente de frenado " + p);
                Ball prev = RouletteWheel.ball(start, start, seq, number, seed);
                double step = 0.25;
                int cross = 0;
                for (double g = start + step; g <= start + RouletteWheel.SPIN_TICKS + 40; g += step) {
                    Ball b = RouletteWheel.ball(g, start, seq, number, seed);
                    // la bola avanza en el mundo con saltos chicos
                    double da = Math.abs(wrap(b.angle - prev.angle));
                    maxStep = Math.max(maxStep, da / step);
                    check(da / step < 0.60, "salto de angulo " + da + " en tau=" + (g - start));
                    check(Math.abs(b.radius - prev.radius) / step < 0.06, "salto de radio");
                    check(Math.abs(b.height - prev.height) / step < 0.05, "salto de altura " + Math.abs(b.height - prev.height));
                    maxDh = Math.max(maxDh, Math.abs(b.height - prev.height) / step);
                    maxDr = Math.max(maxDr, Math.abs(b.radius - prev.radius) / step);
                    check(b.height >= -1e-12 && b.radius >= -1e-12 && b.radius <= 1 + 1e-12, "rango " + b.height + " " + b.radius);
                    if (b.phase == 1 && prev.phase == 1) {
                        cross += (int) Math.abs(Math.floor(b.rel / RouletteWheel.POCKET) - Math.floor(prev.rel / RouletteWheel.POCKET));
                    }
                    prev = b;
                    totalTicks++;
                }
                // al final queda quieta, exacto, en su casilla, y gira con la rueda
                Ball e = RouletteWheel.ball(start + RouletteWheel.SPIN_TICKS, start, seq, number, seed);
                double wa = RouletteWheel.wheelAngle(start + RouletteWheel.SPIN_TICKS, start, seq);
                check(e.landed && Math.abs(wrap(e.angle - wa - RouletteWheel.pocketAngle(number))) < 1e-9, "no cae en su casilla");
                Ball e2 = RouletteWheel.ball(start + RouletteWheel.SPIN_TICKS - 0.01, start, seq, number, seed);
                check(Math.abs(wrap(e2.rel - RouletteWheel.pocketAngle(number))) < 0.01, "justo antes del final ya esta en su casilla: " + wrap(e2.rel - RouletteWheel.pocketAngle(number)));
                check(cross >= 10 && cross <= 150, "casillas cruzadas al caer: " + cross);
                // 1 s despues sigue en la misma casilla relativa
                Ball e3 = RouletteWheel.ball(start + RouletteWheel.SPIN_TICKS + 500, start, seq, number, seed);
                double wa3 = RouletteWheel.wheelAngle(start + RouletteWheel.SPIN_TICKS + 500, start, seq);
                check(Math.abs(wrap(e3.angle - wa3 - RouletteWheel.pocketAngle(number))) < 1e-9, "deriva despues de caer");
                cases++;
            }
        }
        // la rueda nunca salta entre tiradas: al empezar una nueva (seq+1), el angulo es continuo con el final de la anterior
        for (int i = 0; i < 1000; i++) {
            long start = 5000 + rnd.nextInt(1_000_000);
            int seq = 1 + rnd.nextInt(30);
            long next = start + 420 + rnd.nextInt(2000);
            double before = RouletteWheel.wheelAngle(next - 1e-6, start, seq);
            double after = RouletteWheel.wheelAngle(next, next, seq + 1);
            check(Math.abs(before - after) < 1e-3, "salto de la rueda entre tiradas " + (before - after));
        }
        // la primera tirada tambien: antes (seq 0) y despues
        double b0 = RouletteWheel.wheelAngle(7000 - 1e-6, -1, 0);
        double a0 = RouletteWheel.wheelAngle(7000, 7000, 1);
        check(Math.abs(b0 - a0) < 1e-3, "salto en la primera tirada");
        System.out.printf("casos %d  exponente de frenado %.2f..%.2f  maxima velocidad de la bola %.3f rad/tick  maximo cambio de altura %.4f/tick%n",
                cases, minP, maxP, maxStep, maxDh);
        // salida para dibujar una tirada
        if (args.length > 0) {
            long start = 1000;
            for (double g = start; g <= start + 300; g += 1) {
                Ball b = RouletteWheel.ball(g, start, 3, 17, 12345);
                double wa = RouletteWheel.wheelAngle(g, start, 3);
                System.out.printf("%.1f,%.5f,%.5f,%.5f,%.5f,%d%n", g - start, b.angle, wa, b.radius, b.height, b.phase);
            }
        }
    }
}
