package com.coleccionista.mapa;

/**
 * El camino hacia adelante entre capítulos: cada sala tiene una puerta de salida (de acero, de dos hojas, en su muro del este) que da a un
 * pasillo propio ({@code paso_<siguiente>}, con su tema) que llega a la puerta de atrás del pasillo de servicio de la sala siguiente. Así
 * nadie regresa por donde vino: siempre se sigue hacia adelante.
 * <p>
 * Cada zona pone solo lo suyo (y nada de la otra): la sala que se deja pone su puerta y el pedacito de pasillo que atraviesa su muro de
 * ladrillo; la que sigue abre el pedacito de su muro (x 0) frente a la puerta de atrás de su pasillo de servicio; el pasillo de en medio va
 * en su propia zona. Así se puede volver a construir cualquiera sin romper las otras.
 */
public final class Salida {
    private static final String C = "coleccionista:";

    private Salida() {
    }

    /**
     * La puerta de salida de una sala: dos hojas en el muro {@code x} (en {@code z} y {@code z + 1}) que abren hacia el este, con su dintel,
     * y el pedazo de pasillo que atraviesa el ladrillo de afuera (x + 1 hasta {@code hastaX}). Marcas {@code <marca>.a} y {@code .b}.
     */
    public static void puerta(Plano p, String marca, int x, int z, int hastaX, String piso) {
        for (int i = 0; i < 2; i++) {
            int zz = z + i;
            p.set(x, 0, zz, piso);
            p.set(x, 1, zz, C + "puerta_metal[facing=east,half=lower,hinge=" + (i == 0 ? "left" : "right") + ",open=false]");
            p.set(x, 2, zz, C + "puerta_metal[facing=east,half=upper,hinge=" + (i == 0 ? "left" : "right") + ",open=false]");
            p.set(x, 3, zz, C + "metal_negro");
            for (int xx = x + 1; xx <= hastaX; xx++) {
                p.set(xx, 0, zz, piso);
                for (int y = 1; y <= 3; y++) {
                    p.set(xx, y, zz, "air");
                }
                p.set(xx, 4, zz, C + "metal_negro");
            }
        }
        p.marca(marca + ".a", x, 1, z, 270);
        p.marca(marca + ".b", x, 1, z + 1, 270);
    }

    /** La llegada: abre el muro de ladrillo de la zona en x 0 frente a la puerta de atrás de su pasillo de servicio (z 21 y 22). */
    public static void llegada(Plano p, String piso) {
        for (int zz = 21; zz <= 22; zz++) {
            p.set(0, 0, zz, piso);
            for (int y = 1; y <= 3; y++) {
                p.set(0, y, zz, "air");
            }
            p.set(0, 4, zz, C + "metal_negro");
        }
    }
}
