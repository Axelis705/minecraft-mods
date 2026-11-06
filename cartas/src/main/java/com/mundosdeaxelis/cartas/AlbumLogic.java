package com.mundosdeaxelis.cartas;

import java.util.LinkedHashSet;
import java.util.Set;

/** Lo que el jugador ya guardo en su album y las cuentas de los bonus. Sin nada de Minecraft. */
public class AlbumLogic {
    public final Set<String> normal = new LinkedHashSet<>();
    public final Set<String> holo = new LinkedHashSet<>();

    public boolean has(String id, boolean isHolo) {
        return (isHolo ? holo : normal).contains(id);
    }

    /** @return true si era nueva */
    public boolean add(String id, boolean isHolo) {
        return (isHolo ? holo : normal).add(id);
    }

    public void clear() {
        normal.clear();
        holo.clear();
    }

    /** Huecos totales del album: cada carta tiene su hueco normal y su hueco holografico. */
    public static int totalSlots() {
        return Cards.ALL.size() * 2;
    }

    public int filled() {
        return normal.size() + holo.size();
    }

    public double percent() {
        return 100.0 * filled() / Math.max(1, totalSlots());
    }

    /** Cuantos tramos de 5 % lleva el album. */
    public int steps() {
        return (int) Math.floor(percent() / 5.0 + 1e-9);
    }

    public boolean categoryComplete(CardCategory c) {
        for (CardDef d : c.cards) {
            if (!normal.contains(d.id)) {
                return false;
            }
        }
        return !c.cards.isEmpty();
    }

    public int completeCategories() {
        int n = 0;
        for (CardCategory c : Cards.CATEGORIES) {
            if (categoryComplete(c)) {
                n++;
            }
        }
        return n;
    }

    /** Vida extra en puntos de vida (2 = un corazon): medio corazon por cada 5 % del album y un corazon por categoria completa. */
    public double bonusHealth() {
        return steps() * HEALTH_PER_STEP + completeCategories() * HEALTH_PER_CATEGORY;
    }

    public static final double HEALTH_PER_STEP = 1.0;
    public static final double HEALTH_PER_CATEGORY = 2.0;
}
