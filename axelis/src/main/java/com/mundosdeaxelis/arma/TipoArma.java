package com.mundosdeaxelis.arma;

/**
 * Los cinco tipos de arma (etapa 281). Cada uno pelea distinto: el daño de un golpe es {@code mult} veces el daño de referencia
 * del tier (el de la espada) y la velocidad son golpes por segundo. La especial (clic derecho) gasta {@code almas} y tarda
 * {@code enfriamiento} ticks en volver.
 *
 * <ul>
 * <li>Espada: equilibrada. Barrido a los de al lado y contraataque tras recibir un golpe. Especial: Tajo creciente.</li>
 * <li>Hacha de guerra: lenta y fuerte. Quiebra armaduras y escudos. Especial: Torbellino.</li>
 * <li>Martillo: la más lenta y la que más pega. Onda y aturdimiento. Especial: Terremoto.</li>
 * <li>Lanza: más alcance, carga a la carrera y atraviesa. Especial: Embestida.</li>
 * <li>Dagas: muy rápidas; por la espalda pegan mucho más. Especial: Paso sombrío.</li>
 * </ul>
 */
public enum TipoArma {
    ESPADA("espada", 1.00F, 1.6F, 0.0F, 8, 20 * 6),
    HACHA("hacha", 1.35F, 1.0F, 0.0F, 12, 20 * 10),
    MARTILLO("martillo", 1.70F, 0.8F, 0.0F, 15, 20 * 12),
    LANZA("lanza", 0.95F, 1.4F, 1.5F, 10, 20 * 8),
    DAGAS("dagas", 0.60F, 2.5F, 0.0F, 10, 20 * 8);

    public final String id;
    /** Daño de un golpe respecto al daño de referencia del tier. */
    public final float mult;
    /** Golpes por segundo (la espada de Minecraft es 1,6). */
    public final float velocidad;
    /** Alcance extra (bloques). */
    public final float alcance;
    public final int almas;
    public final int enfriamiento;

    TipoArma(String id, float mult, float velocidad, float alcance, int almas, int enfriamiento) {
        this.id = id;
        this.mult = mult;
        this.velocidad = velocidad;
        this.alcance = alcance;
        this.almas = almas;
        this.enfriamiento = enfriamiento;
    }
}
