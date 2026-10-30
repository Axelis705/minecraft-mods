import com.casino.game.PlinkoLayout;
public class PegTest {
    public static void main(String[] a) {
        double worst = 9;
        int wp = 0; double wt = 0;
        for (int path = 0; path < 1024; path++) {
            for (double t = 0; t <= PlinkoLayout.LAND_TICKS; t += 0.05) {
                double[] p = PlinkoLayout.position(path, t);
                for (int k = 0; k < PlinkoLayout.ROWS; k++) {
                    double y = PlinkoLayout.PEG_Y0 - k * PlinkoLayout.DY;
                    for (int j = 0; j <= k; j++) {
                        double x = (j - k / 2.0) * PlinkoLayout.DX;
                        double d = Math.hypot(p[0] - x, p[1] - y) - (0.034 + 0.066);
                        if (d < worst) { worst = d; wp = path; wt = t; }
                    }
                }
            }
        }
        System.out.printf("penetracion maxima en pegs: %.3f (camino %d, t=%.2f)%n", worst, wp, wt);
        // altura minima del tablero que usa la bola dentro del tablero
        double[] top = PlinkoLayout.position(0, 0);
        System.out.println("inicio y=" + top[1]);
    }
}
