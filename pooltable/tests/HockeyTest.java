import com.pooltable.game.HockeyAI;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;

import java.util.Random;

/** Pruebas del hockey de mesa: el disco nunca se sale, los goles cuentan, las partidas terminan, y la máquina juega mejor mientras más difícil es. */
public class HockeyTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            if (fails < 30) {
                System.out.println("FALLA: " + what);
            }
        }
    }

    /** Juega una partida completa entre dos máquinas (A con nivel da, B con nivel db). Devuelve {ganador, golesA, golesB, segundos}. */
    static double[] play(int da, int db, long seed) {
        Random r = new Random(seed);
        HockeyMatch m = new HockeyMatch(r);
        HockeyAI aiA = new HockeyAI(r);
        m.start(true, db);
        m.ph.maxSpeed[0] = HockeyAI.SPEED[da];
        double dt = 0.05;
        double t = 0;
        while (m.state != HockeyMatch.State.OVER && t < 900) {
            aiA.update(m.ph, 0, da, dt);
            m.ph.tx[0] = aiA.tx;
            m.ph.tz[0] = aiA.tz;
            m.tick(dt);
            t += dt;
            HockeyPhysics p = m.ph;
            check(!Double.isNaN(p.px) && !Double.isNaN(p.pz) && !Double.isNaN(p.pvx), "disco sin NaN");
            check(Math.abs(p.pz) <= HockeyPhysics.HW + 1e-6, "disco dentro del ancho (" + p.pz + ")");
            check(Math.abs(p.px) <= HockeyPhysics.HL + HockeyPhysics.PUCK_R + 0.3, "disco dentro del largo (" + p.px + ")");
            for (int s = 0; s < 2; s++) {
                check(p.mx[s] >= HockeyPhysics.minX(s) - 1e-6 && p.mx[s] <= HockeyPhysics.maxX(s) + 1e-6, "mazo " + s + " en su mitad");
                check(Math.abs(p.mz[s]) <= HockeyPhysics.HW - HockeyPhysics.MALLET_R + 1e-6, "mazo " + s + " dentro del ancho");
            }
        }
        check(m.state == HockeyMatch.State.OVER, "la partida termina (" + da + " vs " + db + ")");
        return new double[]{m.winner, m.scoreA, m.scoreB, t};
    }

    /** Un mazo rápido (11 b/s) no atraviesa el disco: se prueba a pasos de 50 ms como en el servidor, sin y con el disco quieto o llegando de frente. */
    static void tunneling() {
        java.util.Random r = new java.util.Random(11);
        int through = 0;
        int trials = 3000;
        for (int i = 0; i < trials; i++) {
            HockeyPhysics ph = new HockeyPhysics();
            ph.maxSpeed[0] = 11.0;
            ph.putMallet(0, -1.1 + r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.8);
            double px = -0.75 + r.nextDouble() * 0.5;
            double pz = ph.mz[0] + (r.nextDouble() - 0.5) * 0.16;
            ph.placePuck(px, pz);
            if (r.nextBoolean()) {
                ph.pvx = -1 - r.nextDouble() * 5;
                ph.pvz = (r.nextDouble() - 0.5) * 2;
            }
            // el mazo se lanza hacia (y más allá de) el disco
            ph.tx[0] = -0.04;
            ph.tz[0] = pz + (r.nextDouble() - 0.5) * 0.1;
            boolean hit = false;
            double startMx = ph.mx[0];
            for (int t = 0; t < 12; t++) {
                for (int k = 0; k < 8; k++) {
                    ph.step(0.05 / 8, true);
                }
                if (ph.mallethits > 0) {
                    hit = true;
                }
                ph.clearEvents();
            }
            // si el mazo acabó más allá de donde estaba el disco sin haberlo tocado, lo atravesó
            if (!hit && ph.mx[0] > px + 0.1 && Math.abs(ph.mz[0] - pz) < 0.1 && ph.px > -1.1 && ph.px < ph.mx[0] - 0.0 && startMx < px) {
                through++;
            }
        }
        check(through == 0, "el mazo no atraviesa el disco (" + through + "/" + trials + ")");
        System.out.println("atravesar: " + through + "/" + trials);
    }

    public static void main(String[] a) {
        tunneling();
        // un golpe de mazo de frente manda el disco hacia el otro lado
        HockeyPhysics ph = new HockeyPhysics();
        ph.placePuck(-0.5, 0);
        ph.putMallet(0, -0.7, 0);
        ph.tx[0] = -0.4;
        ph.tz[0] = 0;
        for (int i = 0; i < 200; i++) {
            ph.step(1 / 240.0, true);
        }
        check(ph.pvx > 1.0 || ph.px > -0.45, "el mazo empuja el disco hacia delante");
        check(ph.mx[0] <= -0.03 + 1e-9, "el mazo no cruza el centro");

        // gol: el disco lanzado hacia la porteria de B cuenta para A; fuera de la porteria rebota
        ph = new HockeyPhysics();
        ph.placePuck(1.0, 0);
        ph.pvx = 4;
        for (int i = 0; i < 400 && ph.goal == 0; i++) {
            ph.step(1 / 240.0, true);
        }
        check(ph.goal == 1, "gol de A");
        ph = new HockeyPhysics();
        ph.placePuck(-1.0, 0.1);
        ph.pvx = -4;
        for (int i = 0; i < 400 && ph.goal == 0; i++) {
            ph.step(1 / 240.0, true);
        }
        check(ph.goal == 2, "gol de B");
        ph = new HockeyPhysics();
        ph.placePuck(1.0, 0.5);
        ph.pvx = 4;
        for (int i = 0; i < 400 && ph.wallHits == 0; i++) {
            ph.step(1 / 240.0, true);
        }
        check(ph.goal == 0 && ph.pvx < 0 && ph.wallHits == 1, "fuera de la porteria rebota");

        // disco lanzado en todas direcciones con mazos moviendose al azar: nunca sale ni se vuelve loco
        Random r = new Random(4);
        for (int n = 0; n < 300; n++) {
            ph = new HockeyPhysics();
            ph.placePuck((r.nextDouble() - 0.5) * 2, (r.nextDouble() - 0.5) * 1.2);
            ph.pvx = (r.nextDouble() - 0.5) * 14;
            ph.pvz = (r.nextDouble() - 0.5) * 14;
            for (int i = 0; i < 2400; i++) {
                if (i % 40 == 0) {
                    ph.tx[0] = (r.nextDouble() - 0.5) * 3;
                    ph.tz[0] = (r.nextDouble() - 0.5) * 2;
                    ph.tx[1] = (r.nextDouble() - 0.5) * 3;
                    ph.tz[1] = (r.nextDouble() - 0.5) * 2;
                }
                ph.step(1 / 160.0, true);
                if (ph.goal != 0) {
                    break;
                }
                check(Math.abs(ph.pz) <= HockeyPhysics.HW + 1e-6 && Math.abs(ph.px) <= HockeyPhysics.HL + 0.2, "disco dentro (azar)");
                check(Math.hypot(ph.pvx, ph.pvz) <= HockeyPhysics.MAX_PUCK + 1e-6, "velocidad maxima del disco");
            }
        }

        // niveles: mas dificil gana mas
        String[] nm = {"facil", "normal", "dificil"};
        double[][] res = new double[3][3];
        int games = 60;
        for (int da = 0; da < 3; da++) {
            for (int db = 0; db < 3; db++) {
                if (da == db) {
                    continue;
                }
                int winsB = 0;
                double gf = 0;
                double time = 0;
                for (int g = 0; g < games; g++) {
                    double[] o = play(da, db, 100 + g * 7 + da * 3 + db);
                    if (o[0] == 1) {
                        winsB++;
                    }
                    gf += o[2] - o[1];
                    time += o[3];
                }
                res[da][db] = 100.0 * winsB / games;
                System.out.printf("A %-7s vs B %-7s: gana B %3.0f%%  (dif. media de goles B-A %+.1f, %.0f s por partida)%n", nm[da], nm[db], res[da][db], gf / games, time / games);
            }
        }
        check(res[0][2] >= 80, "dificil le gana a facil al menos 80% (" + res[0][2] + ")");
        check(res[1][2] >= 60, "dificil le gana a normal al menos 60% (" + res[1][2] + ")");
        check(res[0][1] >= 60, "normal le gana a facil al menos 60% (" + res[0][1] + ")");
        check(res[2][0] <= 20 && res[2][1] <= 40, "facil y normal casi no le ganan a dificil");

        System.out.println(fails == 0 ? "HockeyTest: TODO BIEN" : "HockeyTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
