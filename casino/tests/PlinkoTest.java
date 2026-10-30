import com.casino.game.PlinkoLayout;
import java.util.Random;
public class PlinkoTest {
    public static void main(String[] a) {
        // 1) probabilidad por casillero y retorno exacto (con redondeo)
        for (int r = 0; r < 3; r++) {
            for (int bet : PlinkoLayout.BETS) {
                double sum = 0;
                for (int path = 0; path < 1024; path++) {
                    sum += PlinkoLayout.payout(bet, PlinkoLayout.mult10(r, PlinkoLayout.bucketOf(path)));
                }
                double rtp = sum / 1024 / bet;
                System.out.printf("riesgo %d apuesta %d retorno %.4f%n", r, bet, rtp);
                if (rtp > 0.99 || rtp < 0.90) { throw new AssertionError("retorno fuera de rango"); }
            }
        }
        // 2) simulacion
        Random rnd = new Random(1);
        long[] cnt = new long[11];
        int N = 2_000_000;
        for (int i = 0; i < N; i++) cnt[PlinkoLayout.bucketOf(rnd.nextInt(1 << 10))]++;
        double[] exp = {1, 10, 45, 120, 210, 252, 210, 120, 45, 10, 1};
        for (int i = 0; i < 11; i++) {
            double e = exp[i] / 1024, g = cnt[i] / (double) N;
            if (Math.abs(e - g) > 0.002) throw new AssertionError("casillero " + i);
        }
        // 3) la trayectoria termina siempre en el centro de su casillero y es continua
        for (int path = 0; path < 1024; path++) {
            double[] end = PlinkoLayout.position(path, PlinkoLayout.LAND_TICKS + 5);
            double want = (PlinkoLayout.bucketOf(path) - 5) * PlinkoLayout.DX;
            if (Math.abs(end[0] - want) > 1e-9) throw new AssertionError("x final " + path);
            double[] prev = PlinkoLayout.position(path, 0);
            for (double t = 0.1; t <= PlinkoLayout.LAND_TICKS + 1; t += 0.1) {
                double[] p = PlinkoLayout.position(path, t);
                if (Math.hypot(p[0] - prev[0], p[1] - prev[1]) > 0.12) throw new AssertionError("salto en t=" + t + " path=" + path);
                prev = p;
            }
        }
        // 4) espejo y casilleros
        for (int r = 0; r < 3; r++) for (int b = 0; b < 11; b++) if (PlinkoLayout.mult10(r, b) != PlinkoLayout.mult10(r, 10 - b)) throw new AssertionError("espejo");
        System.out.println("PLINKO OK");
    }
}
