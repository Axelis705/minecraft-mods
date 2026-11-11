package com.autoscoleccion.game;

/** Un cochecito de la colección: número (1-30), id de objeto, nombre, línea (caja donde aparece por primera vez), rareza y forma. */
public final class CarDef {
    public final int number;
    public final String id;
    public final String name;
    /** Línea: 0 = Ruta Urbana, 1 = Pista Nocturna, 2 = Leyendas del Circuito. */
    public final int line;
    public final Rarity rarity;
    public final String blurb;
    public final CarStyle style;

    public CarDef(int number, String id, String name, int line, Rarity rarity, String blurb, CarStyle style) {
        this.number = number;
        this.id = id;
        this.name = name;
        this.line = line;
        this.rarity = rarity;
        this.blurb = blurb;
        this.style = style;
    }

    /** Índice (0-29) dentro de la colección. */
    public int index() {
        return number - 1;
    }
}
