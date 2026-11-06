package com.mundosdeaxelis.cartas;

/** Rareza de una carta (de menos a mas rara). Sin nada de Minecraft para poder probarla fuera del juego. */
public enum CardRarity {
    COMUN("Común", 0x9AA0A6),
    RARA("Rara", 0x3B8CFF),
    EPICA("Épica", 0xB04CFF),
    LEGENDARIA("Legendaria", 0xFFB21E),
    MITICA("Mítica", 0xFF3D6E);

    public final String label;
    public final int color;

    CardRarity(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public boolean atLeast(CardRarity o) {
        return ordinal() >= o.ordinal();
    }
}
