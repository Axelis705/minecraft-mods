import com.casino.game.GalleryPlan;
import com.casino.game.GalleryPlan.Target;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Galería de tiro con tiradores simulados (mala, normal y buena puntería). Revisa que los blancos salgan bien, que un rayo directo
 * siempre le dé, que errar por mucho nunca le dé, y cuánto paga en promedio.
 */
public class GalleryTest {
    /** Ojo del tirador en coordenadas del mostrador (parado frente al mostrador). */
    static final double EU = 0, EV = 0.62, EW = 0.85;

    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    /** Una partida: sigma = error al apuntar (radianes), lag = error de tiempo con blancos en movimiento (s), gap = ticks entre tiros. */
    static int play(long seed, double sigma, double lag, int gap, double ladyMistake, Random rnd) {
        GalleryPlan plan = new GalleryPlan(seed);
        Set<Integer> down = new HashSet<>();
        int score = 0, shots = GalleryPlan.SHOTS, next = 0;
        for (int t = 0; t < GalleryPlan.DURATION && shots > 0; t++) {
            if (t < next) {
                continue;
            }
            // elige el blanco que más conviene
            Target best = null;
            double bestV = 0;
            for (Target g : plan.targets) {
                if (!g.alive(t) || down.contains(g.id) || g.raise(t) < 0.9 || g.end - t < 6) {
                    continue;
                }
                double pts = g.type == GalleryPlan.LADY ? (rnd.nextDouble() < ladyMistake ? 2 : -9) : GalleryPlan.POINTS[g.type];
                double dist = Math.abs(g.w() - EW);
                double size = Math.min(GalleryPlan.HALF_W[g.type], GalleryPlan.HALF_H[g.type]);
                double pHit = Math.min(1, size / (sigma * dist * 2 + 1e-9));
                double v = pts * pHit;
                if (v > bestV) {
                    bestV = v;
                    best = g;
                }
            }
            if (best == null || bestV < 0.35) {
                continue;
            }
            // apunta (con error)
            double speed = best.popUp() ? 0 : (best.u1 - best.u0) / (best.end - best.start); // bloques por tick
            double aimU = best.u(t) + speed * rnd.nextGaussian() * lag * 20;
            double aimV = best.v(t);
            double du = aimU - EU, dv = aimV - EV, dw = best.w() - EW;
            double len = Math.sqrt(du * du + dv * dv + dw * dw);
            du = du / len + rnd.nextGaussian() * sigma;
            dv = dv / len + rnd.nextGaussian() * sigma;
            dw = dw / len;
            int id = plan.hit(t, EU, EV, EW, du, dv, dw, down::contains);
            shots--;
            next = t + Math.max(GalleryPlan.COOLDOWN, gap + rnd.nextInt(6));
            if (id >= 0) {
                down.add(id);
                score += GalleryPlan.POINTS[plan.get(id).type];
            }
        }
        return Math.max(0, score);
    }

    static double[] stats(String name, double sigma, double lag, int gap, double lady) {
        Random rnd = new Random(5);
        int n = 3000;
        long paid = 0, sum = 0;
        int max = 0, win = 0;
        for (int i = 0; i < n; i++) {
            int s = play(rnd.nextLong(), sigma, lag, gap, lady, rnd);
            sum += s;
            max = Math.max(max, s);
            long p = GalleryPlan.payout(100, s);
            paid += p;
            if (p > 0) {
                win++;
            }
        }
        double rtp = paid / (100.0 * n);
        System.out.printf("%-8s puntos promedio %.1f, máximo %d, cobra algo %.0f%%, devuelve %.0f%%%n", name, sum / (double) n, max, 100.0 * win / n, rtp * 100);
        return new double[]{sum / (double) n, rtp};
    }

    public static void main(String[] a) {
        System.out.println("GalleryTest");
        GalleryPlan p = new GalleryPlan(1234);
        GalleryPlan q = new GalleryPlan(1234);
        check(p.targets.size() == q.targets.size() && p.targets.get(10).u0 == q.targets.get(10).u0, "misma semilla, mismos blancos");
        int stars = 0, ladies = 0, bandits = 0;
        for (Target g : p.targets) {
            if (g.end > GalleryPlan.DURATION + 100 || g.start < 0) {
                throw new AssertionError("blanco fuera de la partida: " + g.id);
            }
            stars += g.type == GalleryPlan.STAR ? 1 : 0;
            ladies += g.type == GalleryPlan.LADY ? 1 : 0;
            bandits += g.type == GalleryPlan.BANDIT ? 1 : 0;
        }
        check(stars == 3 && bandits >= 8 && ladies >= 1, "salen estrellas, bandidos y alguna señorita");
        // un disparo justo al centro de un blanco visible le da; uno al lado no
        Target d = null;
        for (Target g : p.targets) {
            if (g.type == GalleryPlan.BANDIT) {
                d = g;
                break;
            }
        }
        double t = d.start + 16;
        double du = d.u(t) - EU, dv = d.v(t) - EV, dw = d.w() - EW;
        check(p.hit(t, EU, EV, EW, du, dv, dw, i -> false) == d.id, "tiro al centro del bandido le da");
        check(p.hit(t, EU, EV, EW, du + 3, dv, dw, i -> false) != d.id, "tiro desviado no le da");
        final int did = d.id;
        check(p.hit(t, EU, EV, EW, du, dv, dw, i -> i == did) != did, "un blanco derribado ya no cuenta");
        check(p.hit(t, EU, EV, EW, 0, 0, 1, i -> false) == -1, "disparar hacia atrás no le da a nada");
        check(GalleryPlan.payout(100, GalleryPlan.SCORE_AT[0] - 1) == 0 && GalleryPlan.payout(100, GalleryPlan.SCORE_AT[0]) == 100 && GalleryPlan.payout(100, 99) == 500, "tabla de premios");
        double[] bad = stats("mala", 0.030, 0.15, 24, 0.25);
        double[] avg = stats("normal", 0.016, 0.08, 16, 0.08);
        double[] good = stats("buena", 0.008, 0.04, 11, 0.0);
        check(bad[1] < 0.8, "con mala puntería se pierde");
        check(avg[1] > 0.6 && avg[1] < 1.15, "con puntería normal casi se recupera");
        check(good[1] > avg[1] && good[1] < 3.0, "con buena puntería se gana, pero con tope");
        System.out.println("GalleryTest OK");
    }
}
