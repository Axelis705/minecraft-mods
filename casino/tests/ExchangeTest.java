import com.casino.game.Exchange;

/** El cambio del cajero: lotes enteros, mismo valor en fichas de los dos lados e ida y vuelta sin perder nada. */
public class ExchangeTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    public static void main(String[] a) {
        System.out.println("ExchangeTest");
        int n = Exchange.COUNT;
        for (int f = 0; f < n; f++) {
            for (int t = 0; t < n; t++) {
                if (f == t) {
                    check(!Exchange.valid(f, t), "no se cambia algo por lo mismo (" + f + ")");
                    continue;
                }
                int g = Exchange.give(f, t), r = Exchange.get(f, t);
                if (g * Exchange.VALUE[f] != r * Exchange.VALUE[t] || g < 1 || r < 1) {
                    throw new AssertionError("lote " + f + "->" + t + " no vale lo mismo: " + g + " x " + Exchange.VALUE[f] + " vs " + r + " x " + Exchange.VALUE[t]);
                }
                // ida y vuelta: lo recibido alcanza justo para regresar a lo de antes
                if (Exchange.give(t, f) != r || Exchange.get(t, f) != g) {
                    throw new AssertionError("ida y vuelta " + f + "<->" + t);
                }
            }
        }
        check(true, "todos los lotes valen lo mismo de los dos lados y se pueden regresar");
        check(Exchange.give(Exchange.EMERALD, Exchange.DIAMOND) == 8 && Exchange.get(Exchange.EMERALD, Exchange.DIAMOND) == 1, "8 esmeraldas = 1 diamante");
        check(Exchange.give(Exchange.CHIPS, Exchange.COINS) == 5 && Exchange.get(Exchange.CHIPS, Exchange.COINS) == 1, "5 fichas = 1 moneda arcade");
        check(Exchange.give(Exchange.DIAMOND, Exchange.COINS) == 1 && Exchange.get(Exchange.DIAMOND, Exchange.COINS) == 16, "1 diamante = 16 monedas");
        check(Exchange.give(Exchange.TICKETS, Exchange.EMERALD) == 10, "10 boletos = 1 esmeralda");
        check(Exchange.maxLots(Exchange.EMERALD, Exchange.DIAMOND, 17) == 2, "con 17 esmeraldas salen 2 diamantes");
        check(Exchange.maxLots(Exchange.CHIPS, Exchange.DIAMOND, 79) == 0, "con 79 fichas no sale ningún diamante");
        check(Exchange.maxLots(Exchange.DIAMOND, Exchange.CHIPS, 1_000_000) * 80 <= Exchange.LIMIT, "hay tope por cambio");
        check(Exchange.maxLots(Exchange.CHIPS, Exchange.COINS, 0) == 0 && !Exchange.valid(-1, 2) && !Exchange.valid(0, 5), "valores fuera de rango");
        System.out.println("ExchangeTest OK");
    }
}
