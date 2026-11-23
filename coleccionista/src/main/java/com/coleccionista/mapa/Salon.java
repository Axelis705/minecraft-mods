package com.coleccionista.mapa;

/**
 * El Salón de las mesas: el mapa de la prueba 3, "Compartir o robar". Un salón elegante y oscuro (como un casino viejo de la
 * colección): 12 mesas en tres filas, cada una con dos lugares frente a frente, una lámpara colgando encima y un letrero con su número.
 * En la pared norte, la pantalla gigante donde se revela cada mesa. Queda al este de las Cloacas (pegado al Refugio).
 * <pre>
 *   x: 0 ... 44   z: 0 ... 32   y: -1 ... 10 (piso en y 0)
 *   mesas: columnas x 7, 17, 27, 37; filas z 10, 18, 26 (la mesa ocupa x-1..x+1)
 * </pre>
 * Marcas: {@code sal.mesa.N.a} (al oeste, mirando al este) y {@code sal.mesa.N.b} (al este, mirando al oeste), N = 1..12;
 * {@code sal.mesa.N} (el centro de la mesa), {@code sal.pantalla}, {@code sal.centro}, {@code sal.perdonado} (el lugar del que se salva
 * sin jugar cuando son impares).
 */
public final class Salon {
    public static final int X0 = 0, X1 = 44, Z0 = 0, Z1 = 32, Y0 = -1, Y1 = 10;
    public static final int MESAS = 12;
    private static final int[] COLS = {7, 17, 27, 37};
    private static final int[] FILAS = {10, 18, 26};
    private static final String C = "coleccionista:";
    private static final String[] MURO = {C + "marmol_negro"};

    private Salon() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        // casco
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, MURO);
        p.caja(X0 + 1, 1, Z0 + 1, X1 - 1, Y1 - 1, Z1 - 1, "air");
        // muros: lambrín de madera abajo, pasamanos y papel tapiz de damasco arriba
        for (int y = 1; y < Y1; y++) {
            for (int x = X0; x <= X1; x++) {
                p.set(x, y, Z0, banda(y, "x"));
                p.set(x, y, Z1, banda(y, "x"));
            }
            for (int z = Z0; z <= Z1; z++) {
                p.set(X0, y, z, banda(y, "z"));
                p.set(X1, y, z, banda(y, "z"));
            }
        }
        // piso: tablero oscuro con alfombra roja bajo cada mesa
        for (int x = X0 + 1; x < X1; x++) {
            for (int z = Z0 + 1; z < Z1; z++) {
                p.set(x, 0, z, (x + z) % 2 == 0 ? C + "marmol_negro" : C + "marmol_crema");
            }
        }
        // techo de madera oscura con vigas
        for (int x = X0 + 1; x < X1; x++) {
            for (int z = Z0 + 1; z < Z1; z++) {
                p.set(x, Y1, z, x % 5 == 0 ? C + "viga_madera[axis=z]" : C + "panel_madera");
            }
        }
        int n = 1;
        for (int fz : FILAS) {
            for (int cx : COLS) {
                mesa(p, n++, cx, fz);
            }
        }
        // la pantalla gigante en la pared norte
        for (int x = 15; x <= 29; x++) {
            for (int y = 1; y <= 9; y++) {
                boolean marco = x == 15 || x == 29 || y == 1 || y == 9;
                p.set(x, y, 0, marco ? C + "marco_dorado" : C + "metal_negro");
            }
        }
        p.set(22, 5, 1, "coleccionista:pantalla[facing=south,grande=true]");
        p.marca("sal.pantalla", 22, 5, 1, 0);
        // el lugar del "perdonado" (cuando son impares): un sillón rojo al pie de la pantalla, mirando al salón
        p.set(22, 1, 3, C + "sillon[facing=south,half=lower]");
        p.set(22, 2, 3, C + "sillon[facing=south,half=upper]");
        p.set(21, 1, 3, C + "vela");
        p.set(23, 1, 3, C + "vela");
        p.marca("sal.perdonado", 22, 1, 4, 0);
        // estandartes y velas en las paredes
        for (int x = 4; x <= 40; x += 6) {
            if (x >= 15 && x <= 29) {
                continue;
            }
            p.set(x, 5, 1, C + "estandarte_rojo[facing=south,half=lower]");
            p.set(x, 6, 1, C + "estandarte_rojo[facing=south,half=upper]");
        }
        for (int x = 4; x <= 40; x += 6) {
            p.set(x, 5, Z1 - 1, C + "estandarte_negro[facing=north,half=lower]");
            p.set(x, 6, Z1 - 1, C + "estandarte_negro[facing=north,half=upper]");
        }
        for (int z = 5; z <= 28; z += 6) {
            p.set(1, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(1, 2, z, C + "candelabro[facing=east,half=upper]");
            p.set(X1 - 1, 1, z, C + "candelabro[facing=west,half=lower]");
            p.set(X1 - 1, 2, z, C + "candelabro[facing=west,half=upper]");
        }
        // candelabros colgando entre las mesas
        for (int cx = 12; cx <= 32; cx += 10) {
            for (int y = 7; y <= 9; y++) {
                p.set(cx, y, 14, "chain[axis=y]");
                p.set(cx, y, 22, "chain[axis=y]");
            }
            p.set(cx, 6, 14, C + "arana");
            p.set(cx, 6, 22, C + "arana");
        }
        p.letrero(22, 9, 1, "dark_oak_wall_sign", "south", "red", "", "COMPARTIR", "O ROBAR", "");
        p.marca("sal.centro", 22, 1, 30, 180);
        Estilo.aplicar(p, java.util.Map.of());
        return p;
    }

    /** El muro del salón según la altura: lambrín (1-2), pasamanos (3) y papel tapiz (4 en adelante). */
    private static String banda(int y, String eje) {
        return y <= 2 ? C + "panel_madera" : y == 3 ? C + "viga_madera[axis=" + eje + "]" : C + "papel_tapiz";
    }

    /** Una mesa: tablero de 3 de largo (oeste a este), un lugar a cada lado, lámpara encima, alfombra y su número. */
    private static void mesa(Plano p, int n, int cx, int cz) {
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 2; z <= cz + 2; z++) {
                p.set(x, 0, z, Math.abs(z - cz) == 2 || Math.abs(x - cx) == 3 ? C + "tapete_orilla" : C + "tapete_rojo");
            }
        }
        for (int x = cx - 1; x <= cx + 1; x++) {
            p.set(x, 1, cz, C + "mesa_tapete[facing=north]");
        }
        p.set(cx, 2, cz, C + "vela");
        // patas: las esquinas del tablero llevan una cerca debajo... (el tablero está en y 1: las cercas irían en y 0, se ve el piso)
        // la lámpara de la mesa
        for (int y = 6; y <= 9; y++) {
            p.set(cx, y, cz, "chain[axis=y]");
        }
        p.set(cx, 5, cz, C + "lampara_billar");
        // el número en el canto de la mesa (los dos lados)
        p.letrero(cx, 1, cz - 1, "dark_oak_wall_sign", "north", "white", "", "MESA", String.valueOf(n), "");
        p.letrero(cx, 1, cz + 1, "dark_oak_wall_sign", "south", "white", "", "MESA", String.valueOf(n), "");
        p.marca("sal.mesa." + n, cx, 1, cz, 0);
        p.marca("sal.mesa." + n + ".a", cx - 2, 1, cz, 270);
        p.marca("sal.mesa." + n + ".b", cx + 2, 1, cz, 90);
    }
}
