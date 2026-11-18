package com.mundosdeaxelis.cartas;

/** Un tipo de sobre: cuantas cartas trae, sus probabilidades de rareza y de que una carta salga holografica. */
public final class PackDef {
    public final String id;
    public final String name;
    public final int color;
    public final int count;
    public final CardRarity min;
    public final double holo;
    /** % por rareza, en el orden de {@link CardRarity}. */
    public final double[] odds;

    PackDef(String[] r, double[] odds) {
        this.id = r[0];
        this.name = r[1];
        this.color = Integer.parseInt(r[2].substring(1), 16);
        this.count = Integer.parseInt(r[3]);
        this.min = CardRarity.valueOf(r[4]);
        this.holo = Double.parseDouble(r[5]);
        this.odds = odds;
    }
}
