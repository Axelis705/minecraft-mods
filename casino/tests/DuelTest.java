import com.casino.game.DuelRules;

import java.util.Random;

/**
 * Duelo al mediodía con jugadores simulados: cuánto tardan en ver la tecla al azar y apretarla (a veces se equivocan de tecla y el
 * revólver se traba). Revisa el grito, las teclas, que el más rápido gane más y cuánto paga cada pistolero.
 */
public class DuelTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    /** Tiempo del jugador: media y desviación (ms) y probabilidad de apretar una tecla equivocada antes. */
    static int human(double mean, double sd, double typo, Random r) {
        int ms = (int) Math.max(180, mean + r.nextGaussian() * sd);
        while (r.nextDouble() < typo) {
            ms += DuelRules.WRONG_LOCK_MS + 120;
        }
        return ms;
    }

    static double[] stats(String name, double mean, double sd, double typo) {
        double[] out = new double[DuelRules.OPPONENTS];
        StringBuilder sb = new StringBuilder(String.format("%-8s", name));
        for (int o = 0; o < DuelRules.OPPONENTS; o++) {
            Random r = new Random(7 + o);
            int n = 40000, win = 0;
            for (int i = 0; i < n; i++) {
                int me = human(mean, sd, typo, r);
                int npc = DuelRules.npcMs(o, r.nextGaussian());
                if (me < npc) {
                    win++;
                }
            }
            double p = win / (double) n;
            out[o] = p * DuelRules.PAY_X10[o] / 10.0;
            sb.append(String.format(" | rival %d: gana %3.0f%%, devuelve %3.0f%%", o, p * 100, out[o] * 100));
        }
        System.out.println(sb);
        return out;
    }

    public static void main(String[] a) {
        System.out.println("DuelTest");
        int lo = Integer.MAX_VALUE, hi = 0;
        boolean[] seen = new boolean[128];
        Random r = new Random(1);
        for (int i = 0; i < 20000; i++) {
            int d = DuelRules.drawTick(r.nextDouble());
            lo = Math.min(lo, d);
            hi = Math.max(hi, d);
            char k = DuelRules.key(r.nextDouble());
            seen[k] = true;
            if (!((k >= 'A' && k <= 'Z') || (k >= '1' && k <= '9'))) {
                throw new AssertionError("tecla rara: " + k);
            }
        }
        check(lo == DuelRules.TOLL_3 + DuelRules.WAIT_MIN && hi == DuelRules.TOLL_3 + DuelRules.WAIT_MAX, "el grito llega entre 1 y 3.5 s después de la tercera campanada");
        int n = 0;
        for (boolean b : seen) {
            n += b ? 1 : 0;
        }
        check(n == DuelRules.KEYS.length(), "salen todas las teclas (" + n + ")");
        check(DuelRules.key(0.0) == 'A' && DuelRules.key(0.9999999) == '9', "las orillas de la tabla de teclas");
        check(DuelRules.npcMs(2, -50) == 250, "ningún pistolero reacciona en menos de 250 ms");
        check(!DuelRules.validMs(40) && DuelRules.validMs(400), "tiempos imposibles se rechazan");
        check(DuelRules.noon(6000) && DuelRules.noon(30000) && !DuelRules.noon(13000), "mediodía de 11:00 a 13:00");
        check(DuelRules.payout(100, 1, false) == 200 && DuelRules.payout(100, 1, true) == 250 && DuelRules.pvpPayout(100) == 190, "premios");
        double[] slow = stats("lento", 1050, 220, 0.20);
        double[] avg = stats("normal", 820, 160, 0.12);
        double[] fast = stats("rápido", 600, 100, 0.06);
        for (int o = 0; o < DuelRules.OPPONENTS; o++) {
            check(fast[o] > avg[o] && avg[o] > slow[o], "el más rápido gana más contra el rival " + o);
            check(slow[o] < 0.85, "el lento pierde contra el rival " + o);
            check(avg[o] < 1.05, "el normal no se hace rico contra el rival " + o);
        }
        check(avg[0] > 0.6, "contra Kid Pecas un tirador normal casi se recupera");
        System.out.println("DuelTest OK");
    }
}
