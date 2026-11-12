import com.casino.game.HorseRace;

/** Carreras de caballos: el ganador es el que cruza primero, todos avanzan siempre, las cuotas dejan ganancia a la casa. */
public class HorseRaceTest {
    public static void main(String[] a) {
        int races = 20000;
        double paid = 0;
        double staked = 0;
        int[] favWins = new int[1];
        int lead = 0;
        for (long s = 1; s <= races; s++) {
            HorseRace r = HorseRace.of(s * 7919L);
            // apuesta 100 a cada caballo
            for (int i = 0; i < HorseRace.N; i++) {
                staked += 100;
                if (r.order[0] == i) {
                    paid += r.payout(i, 100);
                }
            }
            // avanzan siempre y cruzan la meta en su tiempo
            for (int i = 0; i < HorseRace.N; i++) {
                double prev = -1;
                for (double t = 0; t < r.finish[i] + 2; t += 0.05) {
                    double p = r.progress(i, t);
                    if (p < prev - 1e-9) {
                        throw new AssertionError("retrocede " + s + " " + i);
                    }
                    prev = p;
                }
                if (Math.abs(r.progress(i, r.finish[i]) - 1) > 1e-6) {
                    throw new AssertionError("meta " + s);
                }
            }
            // el ganador cruza antes que todos
            int w = r.order[0];
            for (int i = 0; i < HorseRace.N; i++) {
                if (i != w && r.finish[i] <= r.finish[w]) {
                    throw new AssertionError("ganador " + s);
                }
            }
            int[] st = r.standings(r.finish[w] + 0.01);
            if (st[w] != 0) {
                throw new AssertionError("posiciones " + s);
            }
            // el favorito (mayor probabilidad) gana a veces pero no siempre
            int fav = 0;
            for (int i = 1; i < HorseRace.N; i++) {
                if (r.p[i] > r.p[fav]) {
                    fav = i;
                }
            }
            if (w == fav) {
                favWins[0]++;
            }
            // cambios de líder durante la carrera (para que sea emocionante)
            int last = -1;
            for (double t = 1; t < r.finish[w]; t += 0.5) {
                int[] s2 = r.standings(t);
                for (int i = 0; i < HorseRace.N; i++) {
                    if (s2[i] == 0) {
                        if (last >= 0 && last != i) {
                            lead++;
                        }
                        last = i;
                    }
                }
            }
        }
        double rtp = paid / staked;
        System.out.printf("retorno %.3f, favorito gana %.1f%%, cambios de lider por carrera %.2f%n", rtp, 100.0 * favWins[0] / races, (double) lead / races);
        if (rtp < 0.70 || rtp > 0.92) {
            throw new AssertionError("retorno fuera de rango: " + rtp);
        }
        System.out.println("HorseRaceTest OK");
    }
}
