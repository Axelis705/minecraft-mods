package com.coleccionista.mapa;

/**
 * La Cámara final: el mapa de la final, "Las copas". Una sala redonda y alta (la vitrina más valiosa de la colección): en medio una mesa
 * redonda con las copas, cuatro lugares alrededor, el trono del Coleccionista al norte con la pantalla gigante detrás, velas, cadenas y
 * vitrinas con los recuerdos de los eliminados.
 * <pre>
 *   x: 0 ... 30   z: 0 ... 30   y: -1 ... 16 (piso en y 0)   centro de la mesa: (15, 15)
 * </pre>
 * Marcas: {@code fin.lugar.N} (N = 0..3, alrededor de la mesa mirando al centro), {@code fin.copa.N} (N = 0..7, sobre la mesa),
 * {@code fin.trono}, {@code fin.pantalla}, {@code fin.centro}.
 */
public final class Final {
    public static final int X0 = 0, X1 = 30, Z0 = 0, Z1 = 30, Y0 = -1, Y1 = 16;
    public static final int CX = 15, CZ = 15;
    public static final int LUGARES = 4, COPAS = 8;
    private static final String[] MURO = {"polished_blackstone_bricks", "polished_blackstone_bricks", "cracked_polished_blackstone_bricks",
            "gilded_blackstone"};

    private Final() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, MURO);
        // la sala redonda (radio 13), con cúpula
        for (int x = X0 + 1; x < X1; x++) {
            for (int z = Z0 + 1; z < Z1; z++) {
                double d = Math.hypot(x - CX, z - CZ);
                if (d > 13.2) {
                    continue;
                }
                p.set(x, 0, z, d < 4.6 ? "red_wool" : ((int) Math.floor(d) % 2 == 0 ? "polished_blackstone" : "black_terracotta"));
                int alto = (int) Math.round(9 + Math.sqrt(Math.max(0, 13.2 * 13.2 - d * d)) * 0.45);
                for (int y = 1; y <= Math.min(alto, Y1 - 1); y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
        // la mesa redonda (radio 2.5) con borde dorado
        for (int x = CX - 3; x <= CX + 3; x++) {
            for (int z = CZ - 3; z <= CZ + 3; z++) {
                double d = Math.hypot(x - CX, z - CZ);
                if (d <= 2.6) {
                    p.set(x, 1, z, d > 1.9 ? "gold_block" : "dark_oak_planks");
                }
            }
        }
        // las copas sobre la mesa (las pone la prueba), en círculo
        int[][] copas = {{-1, -2}, {1, -2}, {2, -1}, {2, 1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}};
        for (int i = 0; i < COPAS; i++) {
            p.marca("fin.copa." + i, CX + copas[i][0], 2, CZ + copas[i][1], 0);
        }
        p.set(CX, 2, CZ, "candle[candles=4,lit=true]");
        // los lugares alrededor de la mesa (mirando al centro): oeste, este, sur y suroeste
        int[][] lugares = {{-4, 0, 270}, {4, 0, 90}, {0, 4, 180}, {0, -4, 0}};
        for (int i = 0; i < LUGARES; i++) {
            p.marca("fin.lugar." + i, CX + lugares[i][0], 1, CZ + lugares[i][1], lugares[i][2]);
        }
        // el trono al norte, sobre una tarima, y la pantalla gigante detrás
        for (int x = CX - 2; x <= CX + 2; x++) {
            for (int z = 3; z <= 5; z++) {
                p.set(x, 1, z, "polished_blackstone_brick_slab[type=bottom]");
            }
        }
        p.set(CX, 2, 4, "dark_oak_stairs[facing=south]");
        p.set(CX - 1, 2, 4, "red_wool");
        p.set(CX + 1, 2, 4, "red_wool");
        p.set(CX, 3, 3, "red_wool");
        p.set(CX, 4, 3, "gold_block");
        p.marca("fin.trono", CX, 2, 4, 0);
        p.set(CX, 8, 3, "coleccionista:pantalla[facing=south,grande=true]");
        p.marca("fin.pantalla", CX, 8, 3, 0);
        // vitrinas con recuerdos alrededor y velas altas
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI * 2 / 12;
            int x = (int) Math.round(CX + Math.cos(a) * 11.5), z = (int) Math.round(CZ + Math.sin(a) * 11.5);
            if (z < 7 && Math.abs(x - CX) < 6) {
                continue;
            }
            p.set(x, 1, z, "polished_blackstone");
            p.set(x, 2, z, i % 3 == 0 ? "skeleton_skull" : i % 3 == 1 ? "candle[candles=3,lit=true]" : "player_head");
            p.set(x, 3, z, "glass");
            p.set(x, 4, z, "polished_blackstone_slab[type=bottom]");
        }
        // un candelabro colgando sobre la mesa
        for (int y = 9; y <= Y1 - 1; y++) {
            p.set(CX, y, CZ, "chain[axis=y]");
        }
        p.set(CX, 8, CZ, "soul_lantern[hanging=true]");
        for (int[] d : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            for (int y = 9; y <= Y1 - 1; y++) {
                p.set(CX + d[0], y, CZ + d[1], "chain[axis=y]");
            }
            p.set(CX + d[0], 8, CZ + d[1], "lantern[hanging=true]");
        }
        p.marca("fin.centro", CX, 1, CZ + 9, 180);
        return p;
    }
}
