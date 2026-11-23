package com.pescacoleccion.pesca;

import com.axeliscore.ui.Rareza;

/**
 * Las cañas: cada una mejora la pelea (zona verde más ancha, más control, hilo más fuerte, más energía por flecha) y la pesca (los peces
 * pican antes, salen más raros). Los índices se guardan en datos: solo agregar al final.
 */
public enum Cana {
    BAMBU("cana_bambu", Rareza.COMUN, 0.40F, 1.00F, 100, 10, 1.00F, 0),
    ROBLE("cana_roble", Rareza.POCO_COMUN, 0.44F, 1.05F, 110, 11, 0.90F, 1),
    HIERRO("cana_hierro", Rareza.RARO, 0.48F, 1.10F, 125, 12, 0.80F, 2),
    CORAL("cana_coral", Rareza.EPICO, 0.52F, 1.15F, 140, 13.5F, 0.70F, 3),
    CRISTAL("cana_cristal", Rareza.LEGENDARIO, 0.58F, 1.25F, 160, 15, 0.60F, 4),
    /** La caña de Nilo: su regalo con la amistad más alta. Con ella no hay piquetes falsos. */
    NILO("cana_nilo", Rareza.MITICO, 0.62F, 1.3F, 175, 16, 0.55F, 4);

    public final String id;
    public final Rareza rareza;
    /** Media zona verde de la aguja, fuerza de A/D, vida del hilo, energía que quita cada flecha. */
    public final float zone, control, lineHp, damage;
    /** Cuánto se tarda en picar un pez (1 = normal, menos = antes). */
    public final float espera;
    /** Suerte: sube la probabilidad de peces raros. */
    public final int suerte;

    Cana(String id, Rareza rareza, float zone, float control, float lineHp, float damage, float espera, int suerte) {
        this.id = id;
        this.rareza = rareza;
        this.zone = zone;
        this.control = control;
        this.lineHp = lineHp;
        this.damage = damage;
        this.espera = espera;
        this.suerte = suerte;
    }
}
