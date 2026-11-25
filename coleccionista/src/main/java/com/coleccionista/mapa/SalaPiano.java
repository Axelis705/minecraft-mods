package com.coleccionista.mapa;

/**
 * El salón de baile del piano gigante (capítulo 6). Al norte, un escenario con la bailarina de la caja de música gigante y la pantalla
 * grande arriba. En medio, cuatro pianos de piso, uno para cada pieza, separados por cortinas altas (para que nadie copie): cada piano es un
 * cuadro de 6×6 con un tapete de 2×2 al centro (donde se para la pieza) y ocho teclas de colores de 2×2 alrededor (do, re, mi… en el
 * sentido del reloj, empezando al norte). Debajo de cada piano, un foso con picos: cuando alguien pierde, se abre el piso de su piano.
 * <pre>
 *   x: 0 ... 63   z: -1 ... 43   y: -3 ... 16
 *   el pasillo x 2..15 (entra al salón por x 16), el salón x 17..61 / z 0..42 (alto 14)
 *   el escenario x 27..51 / z 1..9 (y 1); la bailarina en (39, 5)
 *   los pianos: tarimas de 3 de alto (las teclas en y 3, se para en y 4); el tapete del piano k en (CX[k], CZ) y (CX[k]+1, CZ+1); el
 *   piano ocupa de CX[k]-2 a CX[k]+3 (y lo mismo en z); el foso, dentro de la tarima, de y 2 a y -2 (picos al fondo)
 * </pre>
 * Marcas: {@code piano.anillo.K} (la esquina noroeste del tapete del piano K), {@code piano.bailarina}, {@code piano.entrada},
 * {@code piano.salida}, {@code piano.luz.K} (el yaw es el nivel), {@code piano.pantalla} y el pasillo {@code pas_piano}.
 */
public final class SalaPiano {
    private static final String C = "coleccionista:";
    public static final int HX0 = 17, HX1 = 61, HZ0 = 0, HZ1 = 42, ALTO = 14;
    /** Los tapetes de los cuatro pianos (esquina noroeste) y su fila. */
    public static final int[] CX = {23, 32, 41, 50};
    public static final int CZ = 22, PIANOS = 4, PISO = 3;
    /** Las teclas, en el sentido del reloj desde el norte: esquina noroeste de cada una, relativa a la del tapete. */
    public static final int[][] TECLA = {{0, -2}, {2, -2}, {2, 0}, {2, 2}, {0, 2}, {-2, 2}, {-2, 0}, {-2, -2}};
    /** El color de cada tecla (concreto de Minecraft) y el color con que brilla (RGB). */
    public static final String[] COLOR = {"red", "orange", "yellow", "lime", "light_blue", "blue", "purple", "magenta"};
    public static final int[] BRILLO = {0xFF3A3A, 0xFF9A2A, 0xFFE83A, 0x7CFF3A, 0x4AD8FF, 0x3A6AFF, 0xB04AFF, 0xFF4AD8};
    public static final int BX = 39, BZ = 5;
    public static int luces;

    private SalaPiano() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        luces = 0;
        p.caja(0, -3, -1, 63, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_piano", new Marco(2, 26, 270));
        salon(p);
        escenario(p);
        for (int k = 0; k < PIANOS; k++) {
            piano(p, k);
        }
        return p;
    }

    private static void luz(Plano p, int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
        p.marca("piano.luz." + luces++, x, y, z, nivel);
    }

    // ------------------------------------------------------------------ el salón

    private static void salon(Plano p) {
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                p.set(x, 0, z, C + ((x + z) % 2 == 0 ? "parquet" : "madera_pulida"));
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ALTO + 1, z, C + "madera_pulida");
            }
        }
        // las paredes: lambrín, espejos de salón de baile entre columnas doradas, papel tapiz arriba
        for (int y = 1; y <= ALTO; y++) {
            for (int x = HX0 - 1; x <= HX1 + 1; x++) {
                String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO ? C + "marco_dorado" : C + "papel_tapiz";
                p.set(x, y, HZ0 - 1, b);
                p.set(x, y, HZ1 + 1, b);
            }
            for (int z = HZ0; z <= HZ1; z++) {
                String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO ? C + "marco_dorado" : C + "papel_tapiz";
                String w = p.get(HX0 - 1, y, z);
                if (w == null || !w.contains("puerta")) {
                    p.set(HX0 - 1, y, z, b);
                }
                p.set(HX1 + 1, y, z, b);
            }
        }
        for (int x = HX0 + 2; x <= HX1 - 2; x += 4) {
            for (int y = 4; y <= 8; y++) {
                p.set(x, y, HZ1 + 1, x % 8 == 3 ? C + "marco_dorado" : C + "espejo");
            }
        }
        for (int z = HZ0 + 4; z <= HZ1 - 4; z += 4) {
            for (int y = 4; y <= 8; y++) {
                p.set(HX1 + 1, y, z, C + "espejo");
            }
        }
        // el techo con casetones y tres arañas
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                boolean marco = (x - HX0) % 5 == 0 || (z - HZ0) % 5 == 0;
                p.set(x, ALTO, z, marco ? C + "marco_dorado" : C + "panel_madera");
            }
        }
        for (int x : new int[] {26, 39, 52}) {
            p.set(x, ALTO - 1, 31, C + "cadena");
            p.set(x, ALTO - 2, 31, C + "arana");
        }
        // candelabros y telarañas
        for (int z : new int[] {17, 26}) {
            p.set(HX0, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(HX0, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        for (int[] e : new int[][] {{HX0, HZ0}, {HX1, HZ0}, {HX0, HZ1}, {HX1, HZ1}}) {
            p.set(e[0], ALTO - 1, e[1], C + "telarana");
        }
        // la luz: a media altura (para el piso) y arriba; se baja mientras tocan
        for (int x = HX0 + 2; x <= HX1 - 2; x += 6) {
            for (int z = HZ0 + 3; z <= HZ1 - 2; z += 6) {
                luz(p, x, 5, z, 12);
            }
        }
        for (int x = HX0 + 3; x <= HX1 - 3; x += 7) {
            for (int z = HZ0 + 4; z <= HZ1 - 3; z += 7) {
                luz(p, x, ALTO - 2, z, 11);
            }
        }
        p.marca("piano.entrada", HX0 + 2, 1, 21, 270);
        p.marca("piano.salida", HX0 + 4, 1, 21, 270);
    }

    // ------------------------------------------------------------------ el escenario de la bailarina

    private static void escenario(Plano p) {
        int x0 = 27, x1 = 51, z0 = 1, z1 = 9;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 1, z, z == z1 ? C + "marco_dorado" : (x + z) % 2 == 0 ? C + "marmol_negro" : C + "marmol_crema");
            }
        }
        // telones a los lados y al fondo
        for (int y = 2; y <= ALTO - 1; y++) {
            for (int x = x0; x <= x1; x++) {
                p.set(x, y, HZ0, y == ALTO - 1 ? C + "telon_borde" : C + "telon");
            }
            for (int z = z0; z <= z1; z++) {
                p.set(x0 - 1, y, z, C + "telon");
                p.set(x1 + 1, y, z, C + "telon");
            }
        }
        for (int z : new int[] {z0 + 1, z1 - 1}) {
            p.set(x0, 2, z, C + "candelabro[facing=east,half=lower]");
            p.set(x0, 3, z, C + "candelabro[facing=east,half=upper]");
            p.set(x1, 2, z, C + "candelabro[facing=west,half=lower]");
            p.set(x1, 3, z, C + "candelabro[facing=west,half=upper]");
        }
        // la bailarina (la pone la prueba), mirando a los pianos (al sur)
        p.marca("piano.bailarina", BX, 2, BZ, 0);
        luz(p, BX, 9, BZ + 2, 14);
        luz(p, BX - 4, 6, BZ + 3, 13);
        luz(p, BX + 4, 6, BZ + 3, 13);
        // la pantalla grande arriba del escenario, en el muro del norte
        p.set(BX, 10, HZ0, C + "pantalla[facing=south,grande=true]");
        p.marca("piano.pantalla", BX, 10, HZ0, 0);
    }

    // ------------------------------------------------------------------ un piano de piso (con su foso)

    private static void piano(Plano p, int k) {
        int cx = CX[k], cz = CZ;
        // la tarima (3 de alto) con el foso adentro: paredes de madera por fuera, ladrillo por dentro, picos al fondo
        for (int x = cx - 3; x <= cx + 4; x++) {
            for (int z = cz - 3; z <= cz + 4; z++) {
                boolean orilla = x == cx - 3 || x == cx + 4 || z == cz - 3 || z == cz + 4;
                for (int y = -2; y < PISO; y++) {
                    p.set(x, y, z, orilla ? (y >= 1 ? C + "panel_madera" : C + "ladrillo_oscuro") : "air");
                }
                p.set(x, PISO, z, orilla ? C + "marco_dorado" : "air");
                if (!orilla && (x + z) % 2 == 0) {
                    p.set(x, -2, z, "minecraft:pointed_dripstone[vertical_direction=up,thickness=tip]");
                }
            }
        }
        // el tapete del centro (negro) y las teclas de colores
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                p.set(cx + dx, PISO, cz + dz, C + "marmol_negro");
            }
        }
        for (int i = 0; i < TECLA.length; i++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    p.set(cx + TECLA[i][0] + dx, PISO, cz + TECLA[i][1] + dz, "minecraft:" + COLOR[i] + "_concrete");
                }
            }
        }
        // los escalones al sur de la tarima (de adorno: a los pianos se llega en el apagón)
        for (int x = cx; x <= cx + 1; x++) {
            p.set(x, 1, cz + 5, C + "escalera_tablon[facing=north]");
            p.set(x, 2, cz + 5, "air");
        }
        // las cortinas entre pianos (no entre el último y la pared): altas, para que nadie vea las teclas del otro
        if (k < PIANOS - 1) {
            int xc = (CX[k] + CX[k + 1]) / 2 + 1;
            for (int z = cz - 5; z <= cz + 6; z++) {
                for (int y = 1; y <= PISO + 5; y++) {
                    p.set(xc, y, z, y == PISO + 5 ? C + "telon_borde" : C + "telon");
                }
            }
        }
        // una luz sobre cada piano (no se baja)
        luz(p, cx, PISO + 5, cz, 13);
        p.marca("piano.anillo." + k, cx, PISO, cz, 180);
    }
}
