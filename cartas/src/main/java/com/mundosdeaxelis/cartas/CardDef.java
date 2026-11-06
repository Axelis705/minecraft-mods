package com.mundosdeaxelis.cartas;

/** Una carta del album (datos fijos, vienen de cartas.json). */
public final class CardDef {
    public final String id;
    public final String name;
    public final String category;
    public final CardRarity rarity;
    public final String desc;
    public final int color1;
    public final int color2;
    /** posicion en la lista general (el orden del album) */
    public final int index;

    CardDef(int index, String[] r) {
        this.index = index;
        this.id = r[0];
        this.name = r[1];
        this.category = r[2];
        this.rarity = CardRarity.valueOf(r[3]);
        this.desc = r[4];
        this.color1 = Integer.parseInt(r[5].substring(1), 16);
        this.color2 = Integer.parseInt(r[6].substring(1), 16);
    }
}
