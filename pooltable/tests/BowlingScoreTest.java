import com.pooltable.game.BowlingGame;
import com.pooltable.game.BowlingScore;

import java.util.Arrays;
import java.util.Random;

/** Pruebas del marcador y los turnos del boliche (sin Minecraft). */
public class BowlingScoreTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            if (fails < 30) {
                System.out.println("FALLA: " + what);
            }
        }
    }

    static int[] rep(int v, int n) {
        int[] a = new int[n];
        Arrays.fill(a, v);
        return a;
    }

    public static void main(String[] args) {
        BowlingScore perfect = BowlingScore.of(rep(10, 12));
        check(perfect.isComplete() && perfect.total() == 300, "partida perfecta = 300, dio " + perfect.total());
        check(Arrays.equals(perfect.cumulative(), new int[]{30, 60, 90, 120, 150, 180, 210, 240, 270, 300}), "acumulado de la perfecta");
        check(Arrays.equals(perfect.marks(9), new String[]{"X", "X", "X"}), "turno 10 con tres chuzas");

        BowlingScore gutter = BowlingScore.of(rep(0, 20));
        check(gutter.isComplete() && gutter.total() == 0, "todo canaleta = 0");
        check(Arrays.equals(gutter.marks(0), new String[]{"-", "-"}), "marcas de ceros");

        BowlingScore nines = BowlingScore.of(9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0, 9, 0);
        check(nines.isComplete() && nines.total() == 90, "9 y fallo diez veces = 90");

        BowlingScore spares = BowlingScore.of(rep(5, 21));
        check(spares.isComplete() && spares.total() == 150, "todo semichuzas de 5 = 150, dio " + spares.total());
        check(Arrays.equals(spares.marks(0), new String[]{"5", "/"}), "marca de semichuza");

        // ejemplo a mano: X 7/ 9- X -8 8/ -6 X X X81
        BowlingScore ex = BowlingScore.of(10, 7, 3, 9, 0, 10, 0, 8, 8, 2, 0, 6, 10, 10, 10, 8, 1);
        check(ex.isComplete(), "ejemplo completo");
        check(Arrays.equals(ex.cumulative(), new int[]{20, 39, 48, 66, 74, 84, 90, 120, 148, 167}), "acumulado del ejemplo: " + Arrays.toString(ex.cumulative()));
        check(ex.total() == 167, "total del ejemplo");

        // turnos incompletos: una chuza sin bonos todavia no cuenta
        BowlingScore part = BowlingScore.of(10);
        check(part.cumulative()[0] == -1 && part.total() == 0, "una chuza sola aun no puntua");
        part.add(3);
        check(part.cumulative()[0] == -1, "chuza + 3 aun no puntua");
        part.add(4);
        check(part.cumulative()[0] == 17 && part.cumulative()[1] == 24, "chuza + 3 + 4 = 17, luego 24: " + Arrays.toString(part.cumulative()));
        check(part.frame() == 2, "va en el turno 3");

        // pinos en pie y rearmado
        BowlingScore s = new BowlingScore();
        check(s.standingForNext() == 10 && s.freshRackForNext(), "empieza con 10");
        s.add(7);
        check(s.standingForNext() == 3 && !s.freshRackForNext(), "tras 7 quedan 3");
        check(!s.add(4), "no se pueden tirar mas pinos de los que hay");
        s.add(3);
        check(s.standingForNext() == 10 && s.freshRackForNext() && s.frame() == 1, "tras semichuza, mesa nueva en el turno 2");
        BowlingScore t10 = BowlingScore.of(rep(0, 18));
        check(t10.frame() == 9 && t10.standingForNext() == 10, "turno 10 empieza con 10");
        t10.add(10);
        check(t10.standingForNext() == 10 && !t10.isComplete(), "chuza en el 10: mesa nueva");
        t10.add(6);
        check(t10.standingForNext() == 4, "tras X y 6 quedan 4");
        t10.add(4);
        check(t10.isComplete() && t10.total() == 20, "X 6 / = 20");
        check(Arrays.equals(t10.marks(9), new String[]{"X", "6", "/"}), "marcas X 6 /: " + Arrays.toString(t10.marks(9)));
        BowlingScore t10b = BowlingScore.of(rep(0, 18));
        t10b.add(4);
        t10b.add(6);
        check(!t10b.isComplete() && t10b.standingForNext() == 10, "semichuza en el 10: mesa nueva y tercer tiro");
        t10b.add(10);
        check(t10b.isComplete() && t10b.total() == 20, "4 / X = 20");
        BowlingScore t10c = BowlingScore.of(rep(0, 18));
        t10c.add(3);
        t10c.add(4);
        check(t10c.isComplete() && t10c.total() == 7, "sin bono el 10 termina en dos tiros");

        // turnos entre jugadores
        BowlingGame g = new BowlingGame(2);
        check(g.current == 0 && g.freshRackForNext(), "empieza el jugador 1 con mesa nueva");
        g.record(3);
        check(g.current == 0 && g.standingForNext() == 7 && !g.freshRackForNext(), "sigue el mismo jugador en su segundo tiro");
        g.record(4);
        check(g.current == 1 && g.freshRackForNext(), "pasa al jugador 2 con mesa nueva");
        g.record(10);
        check(g.current == 0, "una chuza cierra el turno");

        // partidas al azar: terminan, los totales son validos y siempre se puede tirar
        Random r = new Random(7);
        for (int games = 0; games < 20000; games++) {
            int n = 1 + r.nextInt(4);
            BowlingGame gm = new BowlingGame(n);
            int guard = 0;
            while (!gm.over && guard++ < 400) {
                int stand = gm.standingForNext();
                check(stand >= 1 && stand <= 10, "siempre hay pinos para tirar");
                int pins = r.nextInt(100) < 25 ? stand : r.nextInt(stand + 1);
                check(gm.record(pins), "tiro valido");
            }
            check(gm.over, "la partida termina");
            for (BowlingScore sc : gm.scores) {
                check(sc.isComplete() && sc.total() >= 0 && sc.total() <= 300, "total entre 0 y 300");
                int[] cum = sc.cumulative();
                for (int f = 0; f < 10; f++) {
                    check(cum[f] >= 0 && (f == 0 || cum[f] >= cum[f - 1]), "acumulado creciente");
                }
            }
            check(gm.winner() >= 0, "hay ganador");
        }
        System.out.println(fails == 0 ? "BowlingScoreTest: TODO BIEN" : "BowlingScoreTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
