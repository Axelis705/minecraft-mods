package com.coleccionista.mapa;

/**
 * La Capilla: el mapa de la prueba 5, "El sacrificio". Seis cuartos en fila (uno por equipo), cada uno con un altar al fondo y una reja
 * al frente que da a un pasillo común. Del otro lado del pasillo, una vitrina por cuarto: ahí termina quien se queda (la pieza nueva
 * de la colección, a la vista de todos).
 * <pre>
 *   x: 0 ... 72   z: 0 ... 22   y: -1 ... 8
 *   cuarto K (K = 1..6): interior x 12K-11 .. 12K-3, z 1..9; reja en z 10 (x centro-1..centro+1); altar en (centro, z 3)
 *   pasillo: z 11..15 (todo el largo)   vitrinas: z 17 (detrás de un vidrio en z 16)
 * </pre>
 * Marcas: {@code cap.cuarto.K.lugar.N} (N = 0..5, dentro del cuarto), {@code cap.cuarto.K.altar}, {@code cap.cuarto.K.reja} (la esquina
 * de la reja), {@code cap.cuarto.K.vitrina}, {@code cap.cuarto.K.pasillo} (afuera, frente a su reja), {@code cap.pantalla}.
 */
public final class Capilla {
    public static final int X0 = 0, X1 = 72, Z0 = 0, Z1 = 22, Y0 = -1, Y1 = 8;
    public static final int CUARTOS = 6, LUGARES = 6;
    public static final int REJA_ANCHO = 3, REJA_ALTO = 3;
    private static final String[] MURO = {"stone_bricks", "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks", "polished_andesite"};

    private Capilla() {
    }

    public static int centro(int k) {
        return 12 * k - 7;
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, MURO);
        // el pasillo común (techo alto con vigas)
        for (int x = 1; x < X1; x++) {
            for (int z = 11; z <= 15; z++) {
                p.set(x, 0, z, z == 13 ? "red_carpet" : (x + z) % 2 == 0 ? "polished_andesite" : "andesite");
                for (int y = 1; y <= 6; y++) {
                    p.set(x, y, z, "air");
                }
            }
            if (x % 6 == 0) {
                p.set(x, 6, 11, "spruce_stairs[facing=south,half=top]");
                p.set(x, 6, 15, "spruce_stairs[facing=north,half=top]");
            }
        }
        for (int x = 6; x < X1; x += 12) {
            p.set(x, 6, 13, "lantern[hanging=true]");
        }
        p.set(X1 - 1, 3, 13, "coleccionista:pantalla[facing=west,grande=true]");
        p.marca("cap.pantalla", X1 - 1, 3, 13, 90);
        for (int k = 1; k <= CUARTOS; k++) {
            cuarto(p, k);
        }
        return p;
    }

    private static void cuarto(Plano p, int k) {
        int c = centro(k);
        // el cuarto: piso de piedra, velas, un vitral rojo arriba del altar
        for (int x = c - 4; x <= c + 4; x++) {
            for (int z = 1; z <= 9; z++) {
                p.set(x, 0, z, Math.abs(x - c) <= 1 && z <= 8 ? "red_terracotta" : "polished_deepslate");
                for (int y = 1; y <= 5; y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
        // el altar: tarima de losas y la placa en medio
        for (int x = c - 1; x <= c + 1; x++) {
            for (int z = 2; z <= 4; z++) {
                p.set(x, 0, z, "polished_blackstone");
            }
        }
        p.set(c, 1, 3, "light_weighted_pressure_plate");
        p.marca("cap.cuarto." + k + ".altar", c, 1, 3, 0);
        for (int[] d : new int[][] {{-3, 1}, {3, 1}, {-3, 5}, {3, 5}}) {
            p.set(c + d[0], 1, d[1], "polished_blackstone_wall");
            p.set(c + d[0], 2, d[1], "candle[candles=3,lit=true]");
        }
        // vitral rojo en la pared del fondo
        for (int x = c - 1; x <= c + 1; x++) {
            for (int y = 2; y <= 4; y++) {
                p.set(x, y, 0, "red_stained_glass");
            }
        }
        p.set(c, 5, 5, "soul_lantern[hanging=true]");
        p.letrero(c, 4, 1, "dark_oak_wall_sign", "south", "red", "", "ALGUIEN", "SE QUEDA", "");
        // la reja del frente (z 10), con dintel
        for (int x = c - 1; x <= c + 1; x++) {
            for (int y = 1; y <= REJA_ALTO; y++) {
                p.set(x, y, 10, "iron_bars");
            }
            p.set(x, 0, 10, "polished_blackstone");
        }
        p.marca("cap.cuarto." + k + ".reja", c - 1, 1, 10, 0);
        p.letrero(c, REJA_ALTO + 1, 11, "dark_oak_wall_sign", "south", "white", "", "CUARTO", String.valueOf(k), "");
        // lugares dentro del cuarto (dos filas de tres, mirando al altar)
        int n = 0;
        for (int z : new int[] {6, 8}) {
            for (int dx : new int[] {-2, 0, 2}) {
                p.marca("cap.cuarto." + k + ".lugar." + n++, c + dx, 1, z, 180);
            }
        }
        p.marca("cap.cuarto." + k + ".pasillo", c, 1, 13, 0);
        // la vitrina del otro lado del pasillo
        for (int x = c - 1; x <= c + 1; x++) {
            for (int y = 1; y <= 3; y++) {
                p.set(x, y, 16, "glass");
            }
        }
        for (int y = 1; y <= 3; y++) {
            p.set(c, y, 17, "air");
            p.set(c, y, 18, y == 3 ? "lantern[hanging=true]" : "air");
        }
        p.set(c, 0, 17, "red_wool");
        p.set(c, 0, 18, "red_wool");
        p.marca("cap.cuarto." + k + ".vitrina", c, 1, 17, 0);
    }
}
