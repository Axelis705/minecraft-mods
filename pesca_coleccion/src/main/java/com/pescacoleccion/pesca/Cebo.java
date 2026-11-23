package com.pescacoleccion.pesca;

import com.axeliscore.ui.Rareza;

/**
 * Las carnadas. Se llevan en la otra mano (o en el inventario): al caer el corcho al agua se pone una en el anzuelo y el pez se la come
 * cuando pica (si lo recoges antes, no se gasta).
 */
public enum Cebo {
    /** La de siempre: los peces pican mucho antes. */
    LOMBRIZ("lombriz", Rareza.COMUN, 0.65F, 0, false, 1F, 1F),
    /** Dulce: pican antes y el pez pelea más tranquilo (jala menos y se cansa antes). */
    DULCE("cebo_dulce", Rareza.POCO_COMUN, 0.8F, 0, false, 0.85F, 0.9F),
    /** Brillante: atrae peces más raros. */
    BRILLANTE("cebo_brillante", Rareza.RARO, 0.85F, 1, false, 1F, 1F),
    /** De luna: los legendarios pican sin importar la luna ni el clima (pero siguen pidiendo su lugar y su hora). */
    LUNA("cebo_luna", Rareza.LEGENDARIO, 0.8F, 1, true, 1F, 1F);

    public final String id;
    public final Rareza rareza;
    /** Cuánto se tarda en picar (menos = antes). */
    public final float espera;
    /** Suerte extra (peces más raros). */
    public final int suerte;
    /** Los legendarios olvidan la luna y el clima. */
    public final boolean luna;
    /** En la pelea: fuerza del tirón y energía del pez (1 = igual). */
    public final float tiron, energia;

    Cebo(String id, Rareza rareza, float espera, int suerte, boolean luna, float tiron, float energia) {
        this.id = id;
        this.rareza = rareza;
        this.espera = espera;
        this.suerte = suerte;
        this.luna = luna;
        this.tiron = tiron;
        this.energia = energia;
    }
}
