package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lista de todas las cartas, categorias y sobres. Datos en CardData (generado). Sin nada de Minecraft. */
public final class Cards {
    public static final List<CardCategory> CATEGORIES;
    public static final List<CardDef> ALL;
    public static final List<PackDef> PACKS;
    private static final Map<String, CardDef> BY_ID = new LinkedHashMap<>();
    private static final Map<String, CardCategory> CAT_BY_ID = new LinkedHashMap<>();
    private static final Map<String, PackDef> PACK_BY_ID = new LinkedHashMap<>();

    static {
        List<CardCategory> cats = new ArrayList<>();
        for (String[] r : CardData.CATEGORIES) {
            CardCategory c = new CardCategory(r);
            cats.add(c);
            CAT_BY_ID.put(c.id, c);
        }
        List<CardDef> all = new ArrayList<>();
        for (int i = 0; i < CardData.CARDS.length; i++) {
            CardDef d = new CardDef(i, CardData.CARDS[i]);
            all.add(d);
            BY_ID.put(d.id, d);
            CAT_BY_ID.get(d.category).cards.add(d);
        }
        List<PackDef> packs = new ArrayList<>();
        for (int i = 0; i < CardData.PACKS.length; i++) {
            PackDef p = new PackDef(CardData.PACKS[i], CardData.ODDS[i]);
            packs.add(p);
            PACK_BY_ID.put(p.id, p);
        }
        CATEGORIES = Collections.unmodifiableList(cats);
        ALL = Collections.unmodifiableList(all);
        PACKS = Collections.unmodifiableList(packs);
    }

    private Cards() {
    }

    public static CardDef get(String id) {
        return BY_ID.get(id);
    }

    public static CardCategory category(String id) {
        return CAT_BY_ID.get(id);
    }

    public static PackDef pack(String id) {
        return PACK_BY_ID.get(id);
    }

    public static int count(CardRarity r) {
        int n = 0;
        for (CardDef d : ALL) {
            if (d.rarity == r) {
                n++;
            }
        }
        return n;
    }

    /** Parte un texto largo en lineas de como mucho maxChars letras (corta por palabras). */
    public static java.util.List<String> wrap(String text, int maxChars) {
        java.util.List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            if (cur.length() > 0 && cur.length() + 1 + w.length() > maxChars) {
                out.add(cur.toString());
                cur.setLength(0);
            }
            if (cur.length() > 0) {
                cur.append(' ');
            }
            cur.append(w);
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }
}
