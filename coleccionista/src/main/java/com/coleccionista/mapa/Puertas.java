package com.coleccionista.mapa;

/**
 * La Sala de las puertas: el mapa de la prueba 6, "Las puertas". Un vestíbulo alto frente a un muro con tres puertas grandes (con rejas);
 * detrás de cada puerta, una cabina cuyo piso son trampillas de hierro sobre un foso con agua. En cada ronda las puertas cambian de color,
 * todos eligen una cabina y en una de ellas se abre el piso. Está elevada (el piso en y 6) para que el foso sea hondo.
 * <pre>
 *   x: 0 ... 40   z: 0 ... 40   y: 0 ... 20
 *   vestíbulo: x 2..38, z 24..38 (piso y 6)        muro de las puertas: z 23 (puertas en x 6..10, 18..22, 30..34)
 *   cabinas: z 14..22 (x 5..11, 17..23, 29..35)     fosos: debajo de cada cabina, y 0..5 (agua en y 1..2)
 * </pre>
 * Marcas: {@code pue.lugar.N} (N = 0..23, en el vestíbulo, mirando a las puertas), {@code pue.puerta.K} (K = 1..3, la esquina de abajo de
 * la reja), {@code pue.cabina.K} (el centro del piso de la cabina), {@code pue.pantalla}, {@code pue.centro}.
 */
public final class Puertas {
    public static final int X0 = 0, X1 = 40, Z0 = 0, Z1 = 40, Y0 = 0, Y1 = 20;
    public static final int PISO = 6;
    public static final int[] CENTROS = {8, 20, 32};
    public static final int PUERTA_ANCHO = 5, PUERTA_ALTO = 5;
    /** La cabina: x centro-3..centro+3, z 14..22. */
    public static final int CABINA_Z0 = 14, CABINA_Z1 = 22, CABINA_MEDIO = 3;
    public static final int LUGARES = 24;
    private static final String C = "coleccionista:";
    private static final String[] MURO = {C + "ladrillo_viejo"};

    private Puertas() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        // el edificio: macizo con todo tallado adentro
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, MURO);
        // vestíbulo (alto)
        for (int x = 2; x <= 38; x++) {
            for (int z = 24; z <= 38; z++) {
                p.set(x, PISO, z, (x + z) % 2 == 0 ? C + "marmol_negro" : C + "ladrillo_oscuro");
                for (int y = PISO + 1; y <= 18; y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
        // alfombra negra con orilla roja hacia cada puerta
        for (int c : CENTROS) {
            for (int z = 24; z <= 34; z++) {
                for (int x = c - 1; x <= c + 1; x++) {
                    p.set(x, PISO + 1, z, x == c ? "red_carpet" : "black_carpet");
                }
            }
        }
        // las puertas, las cabinas y los fosos
        for (int k = 0; k < 3; k++) {
            puerta(p, k + 1, CENTROS[k]);
        }
        // la pantalla gigante en la pared este del vestíbulo (mirando al oeste)
        for (int z = 24; z <= 38; z++) {
            for (int y = PISO + 2; y <= 17; y++) {
                boolean marco = z == 24 || z == 38 || y == PISO + 2 || y == 17;
                p.set(39, y, z, marco ? C + "marco_dorado" : C + "metal_negro");
            }
        }
        p.set(38, 13, 31, "coleccionista:pantalla[facing=west,grande=true]");
        p.marca("pue.pantalla", 38, 13, 31, 90);
        // luces: candelabros altos a los lados y faroles colgando
        for (int z = 26; z <= 36; z += 5) {
            for (int x : new int[] {3}) {
                p.set(x, PISO + 1, z, C + "candelabro[facing=east,half=lower]");
                p.set(x, PISO + 2, z, C + "candelabro[facing=east,half=upper]");
            }
        }
        // faroles colgando en dos filas (luz suficiente para la transmisión, pero tenue)
        for (int x = 6; x <= 34; x += 7) {
            for (int z : new int[] {27, 34}) {
                for (int y = 14; y <= 18; y++) {
                    p.set(x, y, z, "chain[axis=y]");
                }
                p.set(x, 13, z, (x / 7) % 2 == 0 ? "lantern[hanging=true]" : "soul_lantern[hanging=true]");
            }
        }
        // faroles en el piso a los lados de cada puerta
        for (int c : CENTROS) {
            for (int dx : new int[] {-4, 4}) {
                p.set(c + dx, PISO + 1, 24, C + "candelabro[facing=south,half=lower]");
                p.set(c + dx, PISO + 2, 24, C + "candelabro[facing=south,half=upper]");
            }
        }
        // los lugares de espera: 4 filas de 6, mirando al norte (a las puertas)
        int n = 0;
        for (int fila = 0; fila < 4; fila++) {
            for (int col = 0; col < 6; col++) {
                p.marca("pue.lugar." + n++, 6 + col * 6, PISO + 1, 29 + fila * 2 + (col % 2), 180);
            }
        }
        p.letrero(20, PISO + 3, 38, "dark_oak_wall_sign", "north", "red", "", "UNA DE ELLAS", "NO TIENE PISO", "");
        p.marca("pue.centro", 20, PISO + 1, 36, 180);
        Estilo.aplicar(p, java.util.Map.of());
        return p;
    }

    /** La puerta K con su marco (el color lo pone la prueba en cada ronda), la cabina y el foso con agua. */
    private static void puerta(Plano p, int k, int c) {
        // hueco de la puerta con reja
        for (int x = c - 2; x <= c + 2; x++) {
            for (int y = PISO + 1; y <= PISO + PUERTA_ALTO; y++) {
                p.set(x, y, 23, "iron_bars");
            }
            p.set(x, PISO, 23, C + "metal_placa");
        }
        p.marca("pue.puerta." + k, c - 2, PISO + 1, 23, 0);
        // marco (gris hasta que la prueba le ponga color)
        for (int y = PISO + 1; y <= PISO + PUERTA_ALTO + 1; y++) {
            p.set(c - 3, y, 23, C + "marco_luz_apagado");
            p.set(c + 3, y, 23, C + "marco_luz_apagado");
        }
        for (int x = c - 3; x <= c + 3; x++) {
            p.set(x, PISO + PUERTA_ALTO + 1, 23, C + "marco_luz_apagado");
        }
        String num = k == 1 ? "I" : k == 2 ? "II" : "III";
        p.letrero(c, PISO + PUERTA_ALTO + 2, 24, "dark_oak_wall_sign", "south", "white", "", num, "", "");
        // la cabina: piso de trampillas de hierro, techo con luz de color (la pone la prueba)
        for (int x = c - CABINA_MEDIO; x <= c + CABINA_MEDIO; x++) {
            for (int z = CABINA_Z0; z <= CABINA_Z1; z++) {
                p.set(x, PISO, z, "iron_trapdoor[facing=north,half=top,open=false]");
                for (int y = PISO + 1; y <= PISO + 5; y++) {
                    p.set(x, y, z, "air");
                }
                // el foso: oscuro, con dos de agua al fondo (no hace daño caer)
                for (int y = 1; y < PISO; y++) {
                    p.set(x, y, z, y <= 2 ? "water" : "air");
                }
                p.set(x, 0, z, C + "concreto_humedo");
            }
        }
        p.set(c, PISO + 6, CABINA_Z0 + 4, C + "panel_luz");
        p.marca("pue.cabina." + k, c, PISO + 1, CABINA_Z0 + 4, 180);
        p.marca("pue.foso." + k, c, 1, CABINA_Z0 + 4, 180);
    }
}
