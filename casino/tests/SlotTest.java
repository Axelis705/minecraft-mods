import com.casino.game.SlotLayout;
import java.util.Random;

public class SlotTest {
    static void check(boolean ok, String msg) { if (!ok) throw new AssertionError(msg); }
    public static void main(String[] a) {
        // 1) las cintas tienen el mismo reparto y la corona sale 1 vez por carrete
        int[] cnt = new int[SlotLayout.SYMBOLS];
        for (int[] s : SlotLayout.STRIPS) {
            check(s.length == SlotLayout.STRIP, "largo de cinta");
            int[] c = new int[SlotLayout.SYMBOLS];
            for (int v : s) c[v]++;
            if (cnt[0] == 0) System.arraycopy(c, 0, cnt, 0, c.length);
            for (int i = 0; i < c.length; i++) check(c[i] == cnt[i], "reparto distinto en cinta");
        }
        check(cnt[SlotLayout.CROWN] == 1, "una corona por carrete");
        // 2) retorno exacto
        double rtp = SlotLayout.exactRtp();
        System.out.printf("retorno exacto (fichas): %.4f%n", rtp);
        check(rtp > 0.92 && rtp < 0.97, "retorno fuera de rango");
        // 3) apuestas: las 5 lineas dividen la apuesta exacto
        for (int b : SlotLayout.BETS) check(b % SlotLayout.LINES == 0, "apuesta no divisible");
        // 4) simulacion = teoria
        Random rnd = new Random(7);
        long paid = 0, wins = 0, jack = 0; int N = 3_000_000;
        for (int i = 0; i < N; i++) {
            int[] st = {rnd.nextInt(24), rnd.nextInt(24), rnd.nextInt(24)};
            SlotLayout.Result r = SlotLayout.evaluate(st);
            paid += r.units; if (r.units > 0) wins++; if (r.jackpot) jack++;
        }
        double simRtp = paid / 5.0 / N;
        System.out.printf("simulado %.4f  aciertos %.1f%%  premio gordo 1 de cada %d%n", simRtp, 100.0 * wins / N, jack == 0 ? -1 : N / jack);
        check(Math.abs(simRtp - rtp) < 0.02, "simulacion no coincide");
        // 5) animacion: termina en la posicion exacta y es continua
        for (int reel = 0; reel < 3; reel++) {
            for (int stop = 0; stop < 24; stop += 5) {
                double prev = SlotLayout.offset(reel, stop, 0);
                for (int k = 1; k <= 8 * SlotLayout.END_TICK; k++) {
                    double t = k / 8.0;
                    double v = SlotLayout.offset(reel, stop, t);
                    check(Math.abs(v - prev) < 1.2, "salto en la animacion reel " + reel + " t=" + t + " " + (v - prev));
                    prev = v;
                }
                check(SlotLayout.offset(reel, stop, SlotLayout.STOP_TICK[reel] + 10) == stop, "no termina en el punto");
            }
        }
        // 6) ganadores conocidos
        int[] st = {2, 15, 10}; // corona al centro en los tres carretes
        SlotLayout.Result r = SlotLayout.evaluate(st);
        check(r.jackpot && (r.lineMask & 2) != 0, "tres coronas en la linea central deben dar premio gordo");
        System.out.println("TRAGAMONEDAS OK");
    }
}
