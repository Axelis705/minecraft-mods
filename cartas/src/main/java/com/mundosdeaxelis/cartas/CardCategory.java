package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.List;

/** Una categoria del album (Jefes, Mascotas...). */
public final class CardCategory {
    public final String id;
    public final String name;
    public final int color;
    public final List<CardDef> cards = new ArrayList<>();

    CardCategory(String[] r) {
        this.id = r[0];
        this.name = r[1];
        this.color = Integer.parseInt(r[2].substring(1), 16);
    }
}
