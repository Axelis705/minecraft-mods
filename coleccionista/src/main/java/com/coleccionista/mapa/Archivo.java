package com.coleccionista.mapa;

/**
 * El Archivo: el mapa de la prueba 2, "El traidor". Seis salas de lectura en fila (una por equipo), con libreros, una mesa y al fondo una
 * bóveda pequeña detrás de una reja con su teclado. Al frente, otra reja que da a un pasillo común.
 * <pre>
 *   x: 0 ... 72   z: 0 ... 20   y: -1 ... 8
 *   sala K (K = 1..6): interior x 12K-11 .. 12K-3, z 4..12; bóveda z 1..2 (reja en z 3); reja del frente en z 13
 *   pasillo: z 14..18 (todo el largo)
 * </pre>
 * Marcas: {@code arc.sala.K.lugar.N} (N = 0..5), {@code arc.sala.K.boveda} (la esquina de la reja de la bóveda), {@code arc.sala.K.teclado},
 * {@code arc.sala.K.reja} (la esquina de la reja del frente), {@code arc.sala.K.pasillo}, {@code arc.pantalla}.
 */
public final class Archivo {
    public static final int X0 = 0, X1 = 72, Z0 = 0, Z1 = 20, Y0 = -1, Y1 = 8;
    public static final int SALAS = 6, LUGARES = 6;
    public static final int REJA_ANCHO = 3, REJA_ALTO = 3;
    private static final String C = "coleccionista:";
    private static final String[] MURO = {C + "panel_madera"};

    private Archivo() {
    }

    public static int centro(int k) {
        return 12 * k - 7;
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, MURO);
        for (int x = 1; x < X1; x++) {
            for (int z = 14; z <= 18; z++) {
                p.set(x, 0, z, C + "parquet");
                for (int y = 1; y <= 6; y++) {
                    p.set(x, y, z, "air");
                }
                if (z == 16) {
                    p.set(x, 1, z, C + "alfombra_cafe");
                }
            }
            if (z18Libre(x)) {
                p.set(x, 1, 18, "bookshelf");
                p.set(x, 2, 18, "bookshelf");
            }
        }
        for (int x = 6; x < X1; x += 12) {
            p.set(x, 6, 16, "lantern[hanging=true]");
        }
        p.set(X1 - 1, 3, 16, "coleccionista:pantalla[facing=west,grande=true]");
        p.marca("arc.pantalla", X1 - 1, 3, 16, 90);
        for (int k = 1; k <= SALAS; k++) {
            sala(p, k);
        }
        ambiente(p);
        Estilo.aplicar(p, Estilo.ARCHIVO);
        return p;
    }

    /** Papeles tirados y telarañas en las esquinas de arriba (no estorban). */
    private static void ambiente(Plano p) {
        String[] dirs = {"north", "east", "south", "west"};
        for (long k : new java.util.ArrayList<>(p.m.keySet())) {
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            if (!"air".equals(p.m.get(k))) {
                continue;
            }
            if (y == 1 && p.r.nextDouble() < 0.035 && (C + "parquet").equals(p.get(x, 0, z))) {
                p.set(x, 1, z, C + "papeles[facing=" + dirs[p.r.nextInt(4)] + "]");
            } else if (y >= 4 && x < X1 - 8 && p.vacio(x, y, z) && !"air".equals(p.get(x, y + 1, z)) && p.get(x, y + 1, z) != null && p.r.nextDouble() < 0.03) {
                p.set(x, y, z, C + "telarana");
            }
        }
    }

    private static boolean z18Libre(int x) {
        return x % 12 != 0 && x < X1 - 6;
    }

    private static void sala(Plano p, int k) {
        int c = centro(k);
        for (int x = c - 4; x <= c + 4; x++) {
            for (int z = 4; z <= 12; z++) {
                p.set(x, 0, z, C + "parquet");
                for (int y = 1; y <= 5; y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
        // libreros a los lados
        for (int z = 5; z <= 11; z++) {
            for (int y = 1; y <= 3; y++) {
                p.set(c - 4, y, z, z == 8 && y == 2 ? "chiseled_bookshelf[facing=east]" : "bookshelf");
                p.set(c + 4, y, z, z == 8 && y == 2 ? "chiseled_bookshelf[facing=west]" : "bookshelf");
            }
        }
        // la mesa de lectura en medio con velas
        for (int x = c - 1; x <= c + 1; x++) {
            p.set(x, 1, 8, "dark_oak_slab[type=top]");
        }
        p.set(c - 1, 2, 8, "candle[candles=2,lit=true]");
        p.set(c + 1, 2, 8, "lectern[facing=south]");
        p.set(c, 5, 8, "lantern[hanging=true]");
        p.set(c, 5, 5, "soul_lantern[hanging=true]");
        // la bóveda al fondo: oro y un cofre detrás de una reja
        for (int x = c - 1; x <= c + 1; x++) {
            for (int z = 1; z <= 2; z++) {
                p.set(x, 0, z, "gold_block");
                for (int y = 1; y <= 3; y++) {
                    p.set(x, y, z, "air");
                }
            }
            for (int y = 1; y <= REJA_ALTO; y++) {
                p.set(x, y, 3, "iron_bars");
            }
            p.set(x, 4, 3, "chiseled_polished_blackstone");
        }
        p.set(c, 1, 1, "chest[facing=south]");
        p.set(c - 1, 1, 1, "gold_block");
        p.set(c + 1, 1, 1, "gold_block");
        p.set(c, 3, 1, "lantern[hanging=true]");
        p.marca("arc.sala." + k + ".boveda", c - 1, 1, 3, 0);
        p.set(c + 2, 2, 4, "coleccionista:teclado[facing=south]");
        p.marca("arc.sala." + k + ".teclado", c + 2, 2, 4, 0);
        p.letrero(c, 4, 4, "dark_oak_wall_sign", "south", "gold", "", "LA BÓVEDA", "", "");
        // la reja del frente
        for (int x = c - 1; x <= c + 1; x++) {
            for (int y = 1; y <= REJA_ALTO; y++) {
                p.set(x, y, 13, "iron_bars");
            }
            p.set(x, 0, 13, C + "parquet");
        }
        p.marca("arc.sala." + k + ".reja", c - 1, 1, 13, 0);
        p.letrero(c, REJA_ALTO + 1, 14, "dark_oak_wall_sign", "south", "white", "", "SALA", String.valueOf(k), "");
        int n = 0;
        for (int z : new int[] {10, 11}) {
            for (int dx : new int[] {-2, 0, 2}) {
                p.marca("arc.sala." + k + ".lugar." + n++, c + dx, 1, z, 180);
            }
        }
        p.marca("arc.sala." + k + ".pasillo", c, 1, 16, 0);
    }
}
