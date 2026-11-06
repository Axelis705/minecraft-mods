package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Sortea el contenido de un sobre. Sin nada de Minecraft (se prueba en CI con PackRollerTest). */
public final class PackRoller {
    public record Pull(CardDef card, boolean holo) {
    }

    private PackRoller() {
    }

    /** Probabilidad (0..1) de cada rareza en una tirada normal, repartida solo entre las rarezas que tienen cartas y son >= min. */
    public static double[] rarityProbs(PackDef p, CardRarity min) {
        double[] w = new double[CardRarity.values().length];
        double sum = 0;
        for (CardRarity r : CardRarity.values()) {
            if (r.atLeast(min) && Cards.count(r) > 0) {
                w[r.ordinal()] = p.odds[r.ordinal()];
                sum += w[r.ordinal()];
            }
        }
        if (sum <= 0) {
            for (CardRarity r : CardRarity.values()) {
                if (r.atLeast(min) && Cards.count(r) > 0) {
                    w[r.ordinal()] = 1;
                    sum += 1;
                }
            }
        }
        for (int i = 0; i < w.length; i++) {
            w[i] /= sum;
        }
        return w;
    }

    private static CardRarity pick(double[] probs, Random rnd) {
        double x = rnd.nextDouble();
        double acc = 0;
        CardRarity last = CardRarity.COMUN;
        for (CardRarity r : CardRarity.values()) {
            if (probs[r.ordinal()] <= 0) {
                continue;
            }
            last = r;
            acc += probs[r.ordinal()];
            if (x < acc) {
                return r;
            }
        }
        return last;
    }

    private static CardDef pickCard(CardRarity r, Random rnd) {
        List<CardDef> pool = new ArrayList<>();
        for (CardDef d : Cards.ALL) {
            if (d.rarity == r) {
                pool.add(d);
            }
        }
        return pool.get(rnd.nextInt(pool.size()));
    }

    /** Las cartas del sobre, de la menos rara a la mas rara (lo mejor se revela al final). La ultima cumple la rareza minima del sobre. */
    public static List<Pull> roll(PackDef p, Random rnd) {
        double[] normal = rarityProbs(p, CardRarity.COMUN);
        double[] guaranteed = rarityProbs(p, p.min);
        List<Pull> out = new ArrayList<>();
        boolean haveMin = false;
        for (int i = 0; i < p.count; i++) {
            CardRarity r = pick(normal, rnd);
            if (r.atLeast(p.min)) {
                haveMin = true;
            }
            if (i == p.count - 1 && !haveMin) {
                r = pick(guaranteed, rnd);
            }
            out.add(new Pull(pickCard(r, rnd), rnd.nextDouble() < p.holo));
        }
        out.sort((a, b) -> Integer.compare(a.card().rarity.ordinal(), b.card().rarity.ordinal()));
        return out;
    }

    /** Probabilidad aproximada (0..1) de que una carta concreta aparezca al menos una vez en un sobre (no cuenta la rareza minima garantizada). */
    public static double chanceInPack(PackDef p, CardDef c) {
        double[] pr = rarityProbs(p, CardRarity.COMUN);
        double slot = pr[c.rarity.ordinal()] / Math.max(1, Cards.count(c.rarity));
        return 1.0 - Math.pow(1.0 - slot, p.count);
    }
}
