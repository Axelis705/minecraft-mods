package com.autoscoleccion.game;

/** Rareza de un auto: decide cuántas veces sale (peso dentro de su caja), su color y cuántas estrellas tiene. */
public enum Rarity {
    COMUN("Común", 0xB8C4D6, 1, 40),
    POCO_COMUN("Poco común", 0x5BE38A, 2, 22),
    RARO("Raro", 0x4FB4FF, 3, 11),
    EPICO("Épico", 0xC46BFF, 4, 4.5),
    LEGENDARIO("Legendario", 0xFFC23D, 5, 1.4);

    public final String label;
    public final int rgb;
    public final int stars;
    /** Peso base: un Común sale unas 29 veces más que un Legendario de la misma caja. */
    public final double weight;

    Rarity(String label, int rgb, int stars, double weight) {
        this.label = label;
        this.rgb = rgb;
        this.stars = stars;
        this.weight = weight;
    }
}
