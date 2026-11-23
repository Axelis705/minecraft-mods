import com.pescacoleccion.pelea.PeleaSim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jugadores de mentira (bots) que pelean con peces de cada rareza, para ver qué tan difícil es: cuántas veces gana cada tipo de
 * jugador. También revisa que la pelea sea igual al repetirla con las mismas teclas (lo que hace el servidor).
 * Uso: javac -d out src/.../pelea/*.java tests/PeleaBots.java && java -cp out PeleaBots
 */
public class PeleaBots {
    record Bot(String name, int sigma, float skip, float wrong, float trapPress, int react, float band, int jumpSigma, float mash) {
    }

    static final Bot[] BOTS = {
            new Bot("pro", 22, 0.01F, 0.005F, 0.02F, 110, 0.10F, 28, 10F),
            new Bot("bueno", 42, 0.035F, 0.02F, 0.07F, 170, 0.15F, 48, 8F),
            new Bot("normal", 65, 0.07F, 0.045F, 0.15F, 240, 0.20F, 75, 6.5F),
            new Bot("torpe", 105, 0.14F, 0.09F, 0.32F, 360, 0.28F, 120, 5F)};

    /** La base de cada rareza (la misma que usa el juego) con la caña de prueba. */
    static PeleaSim.Params tier(int r, long seed) {
        PeleaSim.Params p = com.pescacoleccion.pelea.Niveles.base(r, seed);
        if (ROD != null) {
            com.pescacoleccion.pelea.Niveles.cana(p, ROD[0], ROD[1], ROD[2], ROD[3]);
        }
        return p;
    }

    /** Caña de prueba: zona, control, hilo, daño (null = la inicial). */
    static float[] ROD;

    /** Juega una pelea con un bot; devuelve la lista de teclas (ms, tecla, apretada) y deja el resultado en la simulación. */
    static List<int[]> play(PeleaSim sim, Bot b, Random r) {
        List<int[]> ev = new ArrayList<>();
        // flechas: cuándo las aprieta el bot
        List<int[]> presses = new ArrayList<>();
        for (PeleaSim.Note n : sim.notes) {
            if (n.trap) {
                if (r.nextFloat() < b.trapPress) {
                    presses.add(new int[] {n.time + (int) (r.nextGaussian() * b.sigma), n.key});
                }
                continue;
            }
            if (r.nextFloat() < b.skip) {
                continue;
            }
            int key = n.key;
            if (r.nextFloat() < b.wrong) {
                key = (key + 1 + r.nextInt(3)) % 4;
            }
            int sigma = n.ghost ? (int) (b.sigma * 1.4F) : b.sigma;
            presses.add(new int[] {n.time + (int) (r.nextGaussian() * sigma), key});
        }
        for (int j : sim.jumpTimes) {
            presses.add(new int[] {j + PeleaSim.JUMP_LEN + (int) (r.nextGaussian() * b.jumpSigma), PeleaSim.SPACE});
        }
        presses.sort((x, y) -> Integer.compare(x[0], y[0]));
        int pi = 0;
        float[] hist = new float[100000 / PeleaSim.STEP + 10];
        boolean a = false, d = false;
        int nextMash = -1, mashKey = PeleaSim.UP;
        for (int t = 0; t < sim.p.durationMs + 3000 && sim.result == PeleaSim.PELEANDO; t += PeleaSim.STEP) {
            // aguja: el bot reacciona a lo que vio hace un rato
            int seen = Math.max(0, (t - b.react) / PeleaSim.STEP);
            float nd = hist[Math.min(seen, hist.length - 1)];
            boolean wantA = nd > b.band, wantD = nd < -b.band;
            if (wantA != a) {
                a = wantA;
                ev.add(new int[] {t, PeleaSim.A, a ? 1 : 0});
                sim.input(t, PeleaSim.A, a);
            }
            if (wantD != d) {
                d = wantD;
                ev.add(new int[] {t, PeleaSim.D, d ? 1 : 0});
                sim.input(t, PeleaSim.D, d);
            }
            // flechas y saltos de este momento
            while (pi < presses.size() && presses.get(pi)[0] < t + PeleaSim.STEP) {
                int[] pr = presses.get(pi++);
                int ms = Math.max(t, pr[0]);
                if (sim.phase == PeleaSim.FINAL) {
                    continue;
                }
                ev.add(new int[] {ms, pr[1], 1});
                sim.input(ms, pr[1], true);
                ev.add(new int[] {ms + 40, pr[1], 0});
                sim.input(ms + 40, pr[1], false);
            }
            // final: alternar
            if (sim.phase == PeleaSim.FINAL) {
                if (nextMash < 0) {
                    nextMash = t + 250;
                }
                if (t >= nextMash) {
                    ev.add(new int[] {t, mashKey, 1});
                    sim.input(t, mashKey, true);
                    mashKey = mashKey == PeleaSim.UP ? PeleaSim.DOWN : PeleaSim.UP;
                    nextMash = t + (int) (1000 / (b.mash * (0.85F + 0.3F * r.nextFloat())));
                }
            } else {
                nextMash = -1;
            }
            sim.advanceTo(t + PeleaSim.STEP);
            hist[Math.min(t / PeleaSim.STEP + 1, hist.length - 1)] = sim.needle;
        }
        sim.advanceTo(sim.p.durationMs + 3000);
        return ev;
    }

    public static void main(String[] args) {
        String[] tiers = {"Común", "Poco común", "Raro", "Épico", "Legendario"};
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 200;
        if (args.length > 1) {
            String[] v = args[1].split(",");
            ROD = new float[] {Float.parseFloat(v[0]), Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3])};
            System.out.println("caña: zona " + v[0] + " control " + v[1] + " hilo " + v[2] + " daño " + v[3]);
        }
        int bad = 0;
        System.out.printf("%-12s %8s %8s %8s %8s   (duración promedio de los que ganan)%n", "rareza", BOTS[0].name, BOTS[1].name, BOTS[2].name,
                BOTS[3].name);
        for (int tr = 0; tr < tiers.length; tr++) {
            StringBuilder line = new StringBuilder(String.format("%-12s", tiers[tr]));
            StringBuilder dur = new StringBuilder();
            for (Bot b : BOTS) {
                int won = 0, snap = 0, esc = 0;
                long time = 0;
                for (int i = 0; i < n; i++) {
                    long seed = 1000L * tr + i;
                    PeleaSim sim = new PeleaSim(tier(tr, seed));
                    List<int[]> ev = play(sim, b, new Random(seed * 31 + b.name.hashCode()));
                    // repetir con las mismas teclas (como el servidor) debe dar lo mismo
                    int[] flat = new int[ev.size() * 3];
                    for (int k = 0; k < ev.size(); k++) {
                        flat[k * 3] = ev.get(k)[0];
                        flat[k * 3 + 1] = ev.get(k)[1];
                        flat[k * 3 + 2] = ev.get(k)[2];
                    }
                    PeleaSim.Params back = com.pescacoleccion.pelea.Pelea.params(com.pescacoleccion.pelea.Pelea.params(tier(tr, seed)));
                    PeleaSim again = com.pescacoleccion.pelea.Pelea.replay(back,
                            com.pescacoleccion.pelea.Pelea.events(com.pescacoleccion.pelea.Pelea.events(flat)));
                    again.advanceTo(again.p.durationMs + 3000);
                    if (again.result != sim.result || again.misses != sim.misses || Math.abs(again.hp - sim.hp) > 0.001F) {
                        bad++;
                    }
                    if (sim.result == PeleaSim.PESCADO) {
                        won++;
                        time += sim.now;
                    } else if (sim.result == PeleaSim.HILO_ROTO) {
                        snap++;
                    } else {
                        esc++;
                    }
                }
                line.append(String.format(" %4d%%(%d/%d)", won * 100 / n, snap * 100 / n, esc * 100 / n));
                dur.append(String.format(" %5.1fs", won == 0 ? 0 : time / 1000.0 / won));
            }
            System.out.println(line + "  " + dur);
        }
        System.out.println(bad == 0 ? "PELEA_REPETIBLE_OK" : "PELEA_REPETIBLE_FALLO " + bad);
        if (bad != 0) {
            System.exit(1);
        }
    }
}
