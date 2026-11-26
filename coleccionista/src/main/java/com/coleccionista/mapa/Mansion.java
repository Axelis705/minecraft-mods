package com.coleccionista.mapa;

/**
 * La casa del Coleccionista (capítulo 2, "La mascota"): una mansión vieja de dos pisos y ático donde se esconden las piezas mientras
 * Cascabel, la mascota del Coleccionista, las busca. Se llega por el pasillo de servicio ({@link Pasaje}).
 * <p>
 * Está hecha "a mano": el dibujo de cada piso está en {@code data/coleccionista/planos/mansion.txt} (cada letra, una columna de bloques;
 * lo arma {@code herramientas/mapas/mansion_plano.py}) y lo construye {@link Casa}. {@link Revisor} la camina entera en las pruebas
 * ({@code tests/run.sh}): todos los cuartos y escondites se alcanzan a pie.
 * <pre>
 *   x: 0 ... 54   z: -18 ... 61   y: -1 ... 19
 *   el pasillo x 1..16 (entra al vestíbulo por x 16), el vestíbulo x 17..40 / z 10..33 (doble altura, con galerías arriba en y 7 y una
 *   escalera a cada lado de la jaula), la jaula de Cascabel al este (x 42..52), con su reja hacia el vestíbulo
 *   abajo: comedor, cocina, despensa y servicio al norte; música, biblioteca y estudio al sur; arriba: recámara, baño y muñecas al norte;
 *   bebé, costura y estudio secreto al sur
 *   las alas (z -17..-2 y 45..60): pasillo largo con escalera de servicio, tres cuartos por piso, y arriba un ático cada una
 * </pre>
 * Marcas: {@code man.entrada}, {@code man.centro}, {@code man.pantalla}, {@code man.jaula} (donde aparece Cascabel), {@code man.reja.K}
 * (las columnas de la reja de la jaula, de abajo), {@code man.esc.N} (escondites: clósets, cortinas, entre las cajas del ático...) y el
 * pasillo {@code pas_man}.
 */
public final class Mansion {
    private static final String C = "coleccionista:";
    /** El vestíbulo (interior). */
    public static final int HX0 = 17, HX1 = 40, HZ0 = 10, HZ1 = 33, ALTO_HALL = 12;
    /** La reja de la jaula: en el muro del este del vestíbulo (x 41), de z 18 a 25, de y 1 a 6. */
    public static final int REJA_X = 41, REJA_Z0 = 18, REJA_Z1 = 25, REJA_ALTO = 6;
    /** El plano de texto. */
    public static final String PLANO = "/data/coleccionista/planos/mansion.txt";

    private Mansion() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.caja(0, -1, -18, 54, 19, 61, C + "ladrillo_viejo");
        // el pasillo de servicio: sus dos puertas del fondo dan al vestíbulo
        Pasaje.pasillo(p, "pas_man", new Marco(2, 26, 270));
        Casa casa = Casa.leer(PLANO);
        casa.construir(p);
        jaula(p);
        // la salida de la casa (hacia la caja de música): por la puerta de atrás del cuarto de servicio
        Salida.puerta(p, "salida.mansion", 53, 8, 54, C + "tablon");
        // la araña grande colgando en medio del vestíbulo
        p.set(28, ALTO_HALL, 21, C + "cadena");
        p.set(28, ALTO_HALL - 1, 21, C + "arana");
        return p;
    }

    private static void jaula(Plano p) {
        for (int x = 42; x <= 52; x++) {
            for (int z = 14; z <= 29; z++) {
                p.set(x, 0, z, (x + z) % 3 == 0 ? "minecraft:hay_block" : C + "piso_cloaca");
                for (int y = 1; y <= 8; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, 9, z, C + "metal_negro");
            }
        }
        for (int z = REJA_Z0; z <= REJA_Z1; z++) {
            for (int y = 1; y <= REJA_ALTO; y++) {
                p.set(REJA_X, y, z, C + "barrotes");
            }
            p.set(REJA_X, 0, z, C + "metal_placa");
            p.set(REJA_X, REJA_ALTO + 1, z, C + "viga[axis=z]");
            p.marca("man.reja." + (z - REJA_Z0), REJA_X, 1, z, 90);
        }
        // cadenas que cuelgan, huesos, el plato y rasguños
        for (int[] c : new int[][] {{44, 16}, {50, 17}, {45, 27}, {51, 26}}) {
            for (int y = 5; y <= 8; y++) {
                p.set(c[0], y, c[1], C + "cadena");
            }
        }
        p.set(47, 1, 16, C + "craneo");
        p.set(50, 1, 22, C + "craneo");
        p.set(44, 1, 25, C + "craneo");
        p.set(52, 1, 21, C + "argolla[facing=west]");
        p.texto(42, 4, 21, "rayon", "east", 0x8A8070, "IIII IIII", "IIII IIII", "IIII");
        p.set(48, 4, 21, "light[level=4]");
        p.marca("man.jaula", 48, 1, 21, 90);
        // la pantalla grande arriba de la reja y el letrero con su nombre
        p.set(HX1, 9, 21, C + "pantalla[facing=west,grande=true]");
        p.marca("man.pantalla", HX1, 9, 21, 90);
        p.texto(HX1, 7, 21, "letrero", "west", 0xD9A93A, "CASCABEL");
        // los candelabros van sobre una repisa pegada al muro (no flotando)
        p.set(HX1, 6, 17, C + "losa_madera[type=top]");
        p.set(HX1, 6, 26, C + "losa_madera[type=top]");
        p.set(HX1, 7, 17, C + "candelabro[facing=west,half=lower]");
        p.set(HX1, 8, 17, C + "candelabro[facing=west,half=upper]");
        p.set(HX1, 7, 26, C + "candelabro[facing=west,half=lower]");
        p.set(HX1, 8, 26, C + "candelabro[facing=west,half=upper]");
    }
}
