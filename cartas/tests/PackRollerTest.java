import com.mundosdeaxelis.cartas.*;

import java.util.*;

/** Prueba en Java puro: las probabilidades de los sobres, la rareza minima y las cuentas del album. */
public class PackRollerTest {
    static int fails = 0;

    static void check(boolean ok, String msg) {
        if (!ok) {
            fails++;
            System.out.println("FALLO: " + msg);
        }
    }

    public static void main(String[] a) {
        Random rnd = new Random(12345);
        check(Cards.ALL.size() >= 20, "hay cartas");
        for (CardCategory c : Cards.CATEGORIES) {
            check(!c.cards.isEmpty(), "categoria vacia " + c.id);
        }
        for (PackDef p : Cards.PACKS) {
            int n = 40000;
            long[] byR = new long[CardRarity.values().length];
            long total = 0, holo = 0;
            int minOk = 0;
            for (int i = 0; i < n; i++) {
                List<PackRoller.Pull> l = PackRoller.roll(p, rnd);
                check(l.size() == p.count, "tamano del sobre " + p.id);
                boolean has = false;
                PackRoller.Pull prev = null;
                for (PackRoller.Pull x : l) {
                    byR[x.card().rarity.ordinal()]++;
                    total++;
                    if (x.holo()) {
                        holo++;
                    }
                    if (x.card().rarity.atLeast(p.min)) {
                        has = true;
                    }
                    if (prev != null) {
                        check(prev.card().rarity.ordinal() <= x.card().rarity.ordinal(), "orden por rareza");
                    }
                    prev = x;
                }
                if (has) {
                    minOk++;
                }
            }
            check(minOk == n, "rareza minima garantizada en " + p.id + " (" + minOk + "/" + n + ")");
            double hp = (double) holo / total;
            check(Math.abs(hp - p.holo) < 0.01, "probabilidad holografica " + p.id + " esperada " + p.holo + " real " + hp);
            double[] pr = PackRoller.rarityProbs(p, CardRarity.COMUN);
            StringBuilder sb = new StringBuilder(p.id + ":");
            for (CardRarity r : CardRarity.values()) {
                sb.append(String.format(" %s=%.2f%%", r.name(), 100.0 * byR[r.ordinal()] / total));
            }
            System.out.println(sb + String.format(" holo=%.2f%% garantizada>=%s", 100 * hp, p.min));
            // sin la rareza minima, lo real debe parecerse a lo declarado (la garantia solo sube un poco las rarezas altas)
            for (CardRarity r : CardRarity.values()) {
                if (r == CardRarity.COMUN) {
                    continue;
                }
                double real = (double) byR[r.ordinal()] / total;
                check(real + 0.02 >= pr[r.ordinal()] * 0.8, "rareza " + r + " muy baja en " + p.id);
            }
        }
        // album
        AlbumLogic al = new AlbumLogic();
        check(al.steps() == 0 && al.bonusHealth() == 0, "album vacio sin bonus");
        CardCategory first = Cards.CATEGORIES.get(0);
        for (CardDef d : first.cards) {
            al.add(d.id, false);
        }
        check(al.categoryComplete(first), "categoria completa");
        check(al.completeCategories() == 1, "una categoria completa");
        check(al.bonusHealth() >= AlbumLogic.HEALTH_PER_CATEGORY, "bonus por categoria");
        for (CardDef d : Cards.ALL) {
            al.add(d.id, false);
            al.add(d.id, true);
        }
        check(Math.abs(al.percent() - 100.0) < 1e-9, "album al 100 %");
        check(al.steps() == 20, "20 tramos al 100 %");
        double expect = 20 * AlbumLogic.HEALTH_PER_STEP + Cards.CATEGORIES.size() * AlbumLogic.HEALTH_PER_CATEGORY;
        check(Math.abs(al.bonusHealth() - expect) < 1e-9, "bonus total");
        System.out.println(String.format("album completo: +%.1f corazones", al.bonusHealth() / 2.0));
        if (fails > 0) {
            System.out.println("PRUEBAS FALLIDAS: " + fails);
            System.exit(1);
        }
        System.out.println("PackRollerTest: todo bien");
    }
}
