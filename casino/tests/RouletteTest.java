import com.casino.game.RouletteBets;
import com.casino.game.RouletteBets.Spot;
import com.casino.game.RouletteLayout;
import com.casino.game.RouletteTable;
import com.casino.game.RouletteWheel;
import java.util.*;

public class RouletteTest {
    static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError(msg);
    }

    public static void main(String[] a) {
        List<Spot> S = RouletteBets.SPOTS;
        System.out.println("casillas: " + S.size());
        check(S.size() == 157, "cantidad de casillas " + S.size());
        // 1) la rueda europea tiene los 37 numeros una vez y los colores correctos (18 rojos, 18 negros)
        boolean[] seen = new boolean[37];
        for (int n : RouletteBets.WHEEL) { check(!seen[n], "numero repetido " + n); seen[n] = true; }
        int reds = 0;
        for (int n = 1; n <= 36; n++) if (RouletteBets.isRed(n)) reds++;
        check(reds == 18 && RouletteBets.colorOf(0) == 0, "rojos");
        // numeros opuestos que suman 36 en la rueda: 0-26... se comprueba un par conocido: vecinos del 0 son 32 y 26
        check(RouletteBets.WHEEL[1] == 32 && RouletteBets.WHEEL[36] == 26, "vecinos del cero");
        // 2) cobertura y retorno exacto de cada casilla: cantidad de numeros * (pago+1) = 36 (salvo chances simples con La Partage)
        for (Spot s : S) {
            int n = s.numbers.length;
            int expectN = switch (s.kind) {
                case STRAIGHT -> 1; case SPLIT -> 2; case STREET, TRIO -> 3; case CORNER, FIRST_FOUR -> 4; case SIX_LINE -> 6;
                case COLUMN, DOZEN -> 12; case EVEN_MONEY -> 18;
            };
            check(n == expectN, "cantidad de numeros en " + s.id + " " + s.kind);
            check(n * (s.payout + 1) == 36, "pago de " + s.kind + " " + Arrays.toString(s.numbers));
            double ret = 0;
            for (int num = 0; num <= 36; num++) ret += RouletteBets.payoutFor(s, 1000, num);
            double rtp = ret / 37 / 1000;
            double want = s.kind == RouletteBets.Kind.EVEN_MONEY ? 36.5 / 37 : 36.0 / 37;
            check(Math.abs(rtp - want) < 1e-9, "retorno " + s.kind + " " + rtp);
            // los numeros de cada casilla estan ordenados y sin repetir y dentro de 0..36
            for (int i = 0; i < n; i++) {
                check(s.numbers[i] >= 0 && s.numbers[i] <= 36 && (i == 0 || s.numbers[i] > s.numbers[i - 1]), "numeros " + s.id);
            }
        }
        // 3) unicidad de casillas por numeros, ids consecutivos y casillas conocidas
        Set<String> uniq = new HashSet<>();
        for (int i = 0; i < S.size(); i++) {
            check(S.get(i).id == i, "id");
            check(uniq.add(Arrays.toString(S.get(i).numbers)), "casilla repetida " + Arrays.toString(S.get(i).numbers));
        }
        check(RouletteBets.byNumbers(1, 2, 4, 5) >= 0 && S.get(RouletteBets.byNumbers(1, 2, 4, 5)).kind == RouletteBets.Kind.CORNER, "cuadro 1-2-4-5");
        check(S.get(RouletteBets.byNumbers(34, 35, 36)).kind == RouletteBets.Kind.STREET, "calle 34-36");
        check(S.get(RouletteBets.byNumbers(0, 1, 2, 3)).kind == RouletteBets.Kind.FIRST_FOUR, "primeros cuatro");
        // pagos concretos con 10 fichas
        check(RouletteBets.payoutFor(S.get(17), 10, 17) == 360, "pleno paga 35 a 1");
        check(RouletteBets.payoutFor(S.get(RouletteBets.EVEN_BASE + RouletteBets.RED), 10, 0) == 5, "La Partage");
        check(RouletteBets.payoutFor(S.get(RouletteBets.EVEN_BASE + RouletteBets.RED), 10, 1) == 20, "rojo");
        check(RouletteBets.payoutFor(S.get(RouletteBets.EVEN_BASE + RouletteBets.RED), 10, 2) == 0, "rojo pierde con negro");
        check(RouletteBets.payoutFor(S.get(RouletteBets.COLUMN_BASE), 10, 34) == 30, "columna 1 paga 2 a 1");
        check(RouletteBets.payoutFor(S.get(RouletteBets.DOZEN_BASE + 2), 10, 25) == 30, "tercera docena");
        // la columna 1 son 1,4,7...; la 3 son 3,6,...
        check(S.get(RouletteBets.COLUMN_BASE).covers(1) && S.get(RouletteBets.COLUMN_BASE + 2).covers(36), "columnas");
        // 4) el clic cae donde debe: el centro de cada casilla devuelve esa casilla
        for (Spot s : S) {
            int h = RouletteBets.hit(s.gx, s.gy);
            check(h == s.id, "clic en el centro de " + s.kind + " " + Arrays.toString(s.numbers) + " dio " + (h < 0 ? "nada" : Arrays.toString(S.get(h).numbers)));
            // lo mismo a traves de la geometria 3D de la mesa
            double x = RouletteLayout.feltX(s.gx), z = RouletteLayout.feltZ(s.gy);
            int h2 = RouletteLayout.spotAt(x, z);
            check(h2 == s.id, "geometria 3D " + s.id);
        }
        // 5) cualquier punto dentro de un numero da una casilla que incluye ese numero (o un cruce que lo toca)
        Random rnd = new Random(7);
        for (int i = 0; i < 400000; i++) {
            double gx = 1 + rnd.nextDouble() * 12, gy = rnd.nextDouble() * 3;
            int col = (int) (gx - 1), row = Math.min(2, (int) gy);
            int n = RouletteBets.numberAt(col, row);
            int h = RouletteBets.hit(gx, gy);
            check(h >= 0 && S.get(h).covers(n), "punto (" + gx + "," + gy + ") numero " + n + " casilla " + (h < 0 ? "nada" : Arrays.toString(S.get(h).numbers)));
        }
        // el cero: cualquier punto del cero toca el cero
        for (int i = 0; i < 100000; i++) {
            double gx = rnd.nextDouble(), gy = rnd.nextDouble() * 3;
            int h = RouletteBets.hit(gx, gy);
            check(h >= 0 && S.get(h).covers(0), "punto del cero " + gx + "," + gy);
        }
        // 6) apuestas de llamada: cuestan lo anunciado y cubren lo que dicen
        int[] wantUnits = {9, 6, 5, 4};
        for (int c = 0; c < RouletteBets.CALLS; c++) {
            int[][] call = RouletteBets.call(c);
            check(RouletteBets.units(call) == wantUnits[c], "fichas de la llamada " + c);
            for (int[] x : call) check(x[0] >= 0, "casilla de llamada " + c);
        }
        Set<Integer> voisins = new TreeSet<>();
        for (int[] x : RouletteBets.call(0)) for (int v : S.get(x[0]).numbers) voisins.add(v);
        check(voisins.size() == 17, "vecinos del cero cubren 17 numeros: " + voisins);
        Set<Integer> tiers = new TreeSet<>();
        for (int[] x : RouletteBets.call(1)) for (int v : S.get(x[0]).numbers) tiers.add(v);
        check(tiers.size() == 12, "tercio cubre 12: " + tiers);
        Set<Integer> orph = new TreeSet<>();
        for (int[] x : RouletteBets.call(2)) for (int v : S.get(x[0]).numbers) orph.add(v);
        check(orph.size() == 8, "huerfanos cubren 8: " + orph);
        Set<Integer> all = new TreeSet<>(voisins);
        all.addAll(tiers);
        all.addAll(orph);
        check(all.size() == 37, "las tres llamadas juntas cubren los 37 numeros: " + all.size());
        // 7) la mesa: limites, deshacer, retorno de una estrategia al azar (simulacion)
        RouletteTable t = new RouletteTable();
        check(t.place(0, 17, 5) == RouletteTable.OK && t.slots[0].total == 5, "poner ficha");
        check(t.place(0, 17, 1000) == RouletteTable.SPOT_LIMIT, "limite por casilla");
        for (int i = 0; i < 4; i++) check(t.place(0, i, 1000) == RouletteTable.OK, "ok");
        check(t.place(0, 4, 1000) == RouletteTable.TOTAL_LIMIT, "limite total de 5000");
        check(t.place(0, 20, 1000) != RouletteTable.OK, "limite total");
        t.clear(0);
        check(t.slots[0].total == 0, "borrar");
        t.place(1, 3, 25); t.place(1, 4, 5);
        int[] u = t.undo(1);
        check(u != null && u[0] == 4 && u[1] == 5 && t.slots[1].total == 25, "deshacer");
        check(t.remove(1, 3, 100) == 25 && t.slots[1].total == 0, "quitar mas de lo que hay");
        t.phase = RouletteTable.Phase.SPINNING;
        check(t.place(1, 3, 1) == RouletteTable.BAD_PHASE, "no se apuesta girando");
        t.phase = RouletteTable.Phase.BETTING;
        // simulacion: apostar 1 ficha a un pleno, a rojo y a docena en 3.7M tiradas
        Random r2 = new Random(11);
        double[] stake = new double[3], ret = new double[3];
        int[] spots = {5, RouletteBets.EVEN_BASE + RouletteBets.RED, RouletteBets.DOZEN_BASE + 1};
        for (int i = 0; i < 3_700_000; i++) {
            int n = r2.nextInt(37);
            for (int k = 0; k < 3; k++) {
                stake[k] += 2;
                ret[k] += RouletteBets.payoutFor(S.get(spots[k]), 2, n);
            }
        }
        for (int k = 0; k < 3; k++) {
            double rtp = ret[k] / stake[k];
            System.out.printf("simulacion casilla %d retorno %.4f%n", spots[k], rtp);
            check(Math.abs(rtp - (k == 1 ? 0.9865 : 0.9730)) < 0.01, "retorno simulado " + k);
        }
        // pago de la mesa completa
        RouletteTable t2 = new RouletteTable();
        t2.place(0, 17, 10);
        t2.place(0, RouletteBets.EVEN_BASE + RouletteBets.ODD, 20);
        t2.place(1, RouletteBets.EVEN_BASE + RouletteBets.BLACK, 100);
        t2.phase = RouletteTable.Phase.SPINNING;
        t2.settle(17);
        check(t2.slots[0].payout == 360 + 40 && t2.slots[0].net == 400 - 30, "pago jugador 0: " + t2.slots[0].payout);
        check(t2.slots[1].payout == 200 && t2.slots[1].net == 100, "17 es negro: gana 1 a 1");
    }
}
