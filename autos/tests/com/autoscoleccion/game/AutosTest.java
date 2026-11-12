package com.autoscoleccion.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pruebas de la lógica de Autos de Colección (Java puro, sin Minecraft):
 * catálogo, cajas (contenido y probabilidades), puntaje -> caja, y los cuatro minijuegos jugados por "bots" de distinta habilidad.
 * También comprueba que repetir las teclas (lo que hace el servidor) da exactamente el mismo puntaje.
 */
public class AutosTest {
    static int fails;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            System.out.println("FALLO: " + what);
        }
    }

    // ------------------------------------------------------------------ catálogo y cajas

    static void catalog() {
        check(Cars.all().size() == 40, "40 autos");
        Set<String> ids = new HashSet<>();
        int[] perLine = new int[Boxes.COUNT];
        for (CarDef c : Cars.all()) {
            check(ids.add(c.id), "id repetido " + c.id);
            check(c.id.matches("[a-z0-9_]+"), "id válido " + c.id);
            perLine[c.line]++;
            check(c.style != null, "estilo de " + c.id);
        }
        check(perLine[0] == 10 && perLine[1] == 10 && perLine[2] == 10 && perLine[3] == 10, "10 autos por línea");
        check(Boxes.size(0) == 10 && Boxes.size(1) == 20 && Boxes.size(2) == 30 && Boxes.size(3) == 10, "cajas de 10, 20, 30 y 10");
        for (CarDef c : Cars.all()) {
            for (int b = 0; b < Boxes.COUNT; b++) {
                boolean in = b == Boxes.EXCLUSIVE ? c.line == Boxes.EXCLUSIVE : c.line <= b;
                check((Boxes.weight(c, b) > 0) == in, "contenido de la caja " + b + " (" + c.id + ")");
            }
        }
        // probabilidades dentro de cada caja: no todas iguales, la más rara sale poco
        for (int b = 0; b < Boxes.COUNT; b++) {
            double min = 1, max = 0, sum = 0;
            for (CarDef c : Cars.all()) {
                double p = Boxes.chance(c, b);
                sum += p;
                if (p > 0) {
                    min = Math.min(min, p);
                    max = Math.max(max, p);
                }
            }
            check(Math.abs(sum - 1.0) < 1e-9, "suma 100 % caja " + b);
            check(max / min > (b == Boxes.EXCLUSIVE ? 3 : 8), "diferencia entre común y raro en la caja " + b);
            System.out.printf("caja %d (%s): el más común %.1f %%, el más raro %.2f %%%n", b, Boxes.NAME[b], max * 100, min * 100);
        }
        // tirada real
        Random r = new Random(1);
        int[][] hits = new int[Boxes.COUNT][Cars.COUNT];
        for (int b = 0; b < Boxes.COUNT; b++) {
            for (int i = 0; i < 200000; i++) {
                hits[b][Boxes.roll(b, r)]++;
            }
            for (CarDef c : Cars.all()) {
                double p = hits[b][c.index()] / 200000.0;
                check(Math.abs(p - Boxes.chance(c, b)) < 0.004, "tirada caja " + b + " " + c.id);
                if (Boxes.weight(c, b) == 0) {
                    check(hits[b][c.index()] == 0, "no sale de otra línea");
                }
            }
        }
    }

    static void odds() {
        double[] lo = Boxes.odds(0.0);
        double[] mid = Boxes.odds(0.5);
        double[] hi = Boxes.odds(1.0);
        for (double[] o : new double[][] {lo, mid, hi}) {
            check(Math.abs(o[0] + o[1] + o[2] + o[3] - 1.0) < 1e-9 && o[0] >= 0, "probabilidades válidas");
        }
        check(hi[2] > mid[2] && mid[2] > lo[2], "más puntaje = más Leyendas");
        check(hi[0] < lo[0], "más puntaje = menos Ruta Urbana");
        check(lo[3] == 0 && mid[3] == 0 && hi[3] > 0.1, "Exclusivos solo con puntaje muy alto");
        System.out.printf("puntaje 0: %.0f/%.0f/%.0f %%   0.5: %.0f/%.0f/%.0f %%   1: %.0f/%.0f/%.0f %%%n",
                lo[0] * 100, lo[1] * 100, lo[2] * 100, mid[0] * 100, mid[1] * 100, mid[2] * 100, hi[0] * 100, hi[1] * 100, hi[2] * 100);
        double[] top = Boxes.odds(0.9);
        System.out.printf("Exclusivos de Cine: puntaje 0.9 = %.1f %%, puntaje 1 = %.1f %%%n", top[3] * 100, hi[3] * 100);
    }

    // ------------------------------------------------------------------ bots

    /** Un jugador simulado: error de tiempo (ms), tiempo entre teclas (ms) y probabilidad de equivocarse de tecla. */
    record Skill(String name, double sigma, double typeGap, double wrong, int react) {
    }

    static final Skill[] SKILLS = {
            new Skill("perfecto", 0, 120, 0, 0),
            new Skill("pro", 28, 170, 0.01, 120),
            new Skill("bueno", 55, 230, 0.03, 160),
            new Skill("normal", 95, 300, 0.06, 200),
            new Skill("torpe", 170, 420, 0.12, 260)};

    static List<SkillGame.Input> log;

    static void press(SkillGame g, int t, int key, boolean down) {
        g.update(t);
        log.add(new SkillGame.Input(t, key, down));
        g.input(t, key, down);
    }

    static int other(int key, Random r, int[] pool) {
        int k;
        do {
            k = pool[r.nextInt(pool.length)];
        } while (k == key);
        return k;
    }

    static double playShift(long seed, Skill s, Random r) {
        ShiftGame g = new ShiftGame(seed);
        log = new ArrayList<>();
        int plan = -1;
        int planGear = -1;
        for (int t = 0; t < g.maxDuration() && !g.done(); t++) {
            g.update(t);
            if (g.done()) {
                break;
            }
            if (!g.waiting() && planGear != g.gear()) {
                planGear = g.gear();
                plan = g.gearStart + g.timeFor(g.gear(), g.center[g.gear()]) + (int) Math.round(r.nextGaussian() * s.sigma());
            }
            if (!g.waiting() && planGear == g.gear() && t >= plan) {
                int k = g.key[g.gear()];
                if (r.nextDouble() < s.wrong()) {
                    k = other(k, r, ShiftGame.POOL);
                }
                press(g, t, k, true);
                planGear = -2;
            }
        }
        return g.score();
    }

    static double playRhythm(long seed, Skill s, Random r) {
        RhythmGame g = new RhythmGame(seed);
        log = new ArrayList<>();
        List<int[]> ev = new ArrayList<>();
        for (RhythmGame.Note n : g.notes()) {
            if (r.nextDouble() < s.wrong()) {
                continue;
            }
            ev.add(new int[] {n.t + (int) Math.round(r.nextGaussian() * s.sigma()), RhythmGame.LANES[n.lane]});
        }
        ev.sort((a, b) -> Integer.compare(a[0], b[0]));
        for (int[] e : ev) {
            press(g, Math.max(0, e[0]), e[1], true);
        }
        g.update(g.maxDuration());
        return g.score();
    }

    static double playCombo(long seed, Skill s, Random r) {
        ComboGame g = new ComboGame(seed);
        log = new ArrayList<>();
        boolean hold = false;
        double[] hist = new double[20000];
        double[] histV = new double[20000];
        double aim = 0;
        for (int t = 0; t < g.maxDuration() && !g.done(); t++) {
            g.update(t);
            hist[t] = g.rpm;
            histV[t] = g.vel;
            if (t >= ComboGame.LAUNCH_START && t % 10 == 0) {
                int d = s.react() / 2;
                double seen = hist[Math.max(0, t - d)];
                double sv = histV[Math.max(0, t - d)];
                if (t % 250 == 0) {
                    aim = r.nextGaussian() * s.sigma() / 1100.0;
                }
                double pred = seen + sv * (0.13 + d / 1000.0);
                double target = g.bandCenter(t) + aim;
                boolean want = pred < target;
                if (want != hold) {
                    hold = want;
                    press(g, t, SkillGame.K_W, hold);
                }
            }
        }
        return g.score();
    }

    static double playSequence(long seed, Skill s, Random r) {
        SequenceGame g = new SequenceGame(seed);
        log = new ArrayList<>();
        int roundSeen = -1;
        int next = -1;
        int idx = 0;
        for (int t = 0; t < g.maxDuration() && !g.done(); t++) {
            g.update(t);
            if (g.done()) {
                break;
            }
            if (g.between()) {
                continue;
            }
            if (roundSeen != g.round()) {
                roundSeen = g.round();
                idx = 0;
                next = g.roundStartT() + SequenceGame.PREVIEW + s.react();
            }
            if (t >= next && idx < g.sequence().length && !g.between()) {
                int k = g.sequence()[idx];
                if (r.nextDouble() < s.wrong()) {
                    k = other(k, r, SequenceGame.POOL);
                }
                press(g, t, k, true);
                idx++;
                next = t + (int) Math.max(50, s.typeGap() + r.nextGaussian() * s.typeGap() * 0.2);
            }
        }
        return g.score();
    }

    static double play(int type, long seed, Skill s, Random r) {
        return switch (type) {
            case SkillGame.RHYTHM -> playRhythm(seed, s, r);
            case SkillGame.COMBO -> playCombo(seed, s, r);
            case SkillGame.SEQUENCE -> playSequence(seed, s, r);
            default -> playShift(seed, s, r);
        };
    }

    static void games() {
        Random r = new Random(42);
        for (int type = 0; type < SkillGame.TYPES; type++) {
            StringBuilder line = new StringBuilder(String.format("%-20s", SkillGame.NAME[type]));
            double[] avg = new double[SKILLS.length];
            for (int si = 0; si < SKILLS.length; si++) {
                Skill s = SKILLS[si];
                int n = 300;
                double sum = 0;
                int max = 0;
                for (int i = 0; i < n; i++) {
                    long seed = r.nextLong();
                    double sc = play(type, seed, s, r);
                    // el servidor repite las teclas y debe llegar al mismo puntaje
                    double rep = SkillGame.replay(type, seed, log, SkillGame.create(type, seed).maxDuration());
                    check(Math.abs(rep - sc) < 1e-9, "repetición = juego (" + SkillGame.NAME[type] + ", " + s.name() + ") " + sc + " vs " + rep);
                    check(sc >= 0 && sc <= 1, "puntaje entre 0 y 1");
                    sum += sc;
                    if (sc >= 0.98) {
                        max++;
                    }
                }
                avg[si] = sum / n;
                line.append(String.format("  %s %3.0f%% (máx %d%%)", s.name(), avg[si] * 100, max * 100 / n));
            }
            System.out.println(line);
            check(avg[0] > 0.9, SkillGame.NAME[type] + ": jugando perfecto se llega arriba");
            check(avg[1] < 0.93 && avg[1] > 0.6, SkillGame.NAME[type] + ": un pro saca mucho pero no siempre el máximo (" + avg[1] + ")");
            check(avg[2] < avg[1] && avg[3] < avg[2] && avg[4] < avg[3], SkillGame.NAME[type] + ": más habilidad = más puntaje");
            check(avg[3] < 0.6, SkillGame.NAME[type] + ": uno normal no saca demasiado (" + avg[3] + ")");
        }
        // sin pulsar nada: 0 y el juego termina solo
        for (int type = 0; type < SkillGame.TYPES; type++) {
            SkillGame g = SkillGame.create(type, 7);
            g.update(g.maxDuration());
            check(g.done(), SkillGame.NAME[type] + " termina solo");
            check(g.score() < 0.05, SkillGame.NAME[type] + " sin jugar ~0 (" + g.score() + ")");
        }
        // aporrear todas las teclas no sirve
        for (int type = 0; type < SkillGame.TYPES; type++) {
            List<SkillGame.Input> spam = new ArrayList<>();
            int[] keys = {'Q', 'W', 'E', 'R', 'A', 'S', 'D', 'F', 'J', 'K', 'L', 262, 263, 264, 265};
            for (int t = 0; t < 30000; t += 25) {
                spam.add(new SkillGame.Input(t, keys[(t / 25) % keys.length], true));
            }
            double sc = SkillGame.replay(type, 9, spam, 30000);
            check(sc < 0.35, SkillGame.NAME[type] + ": aporrear teclas no da puntaje (" + sc + ")");
        }
    }

    public static void main(String[] a) {
        catalog();
        odds();
        games();
        System.out.println(fails == 0 ? "AutosTest: todo bien" : "AutosTest: " + fails + " fallos");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
