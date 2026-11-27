package com.coleccionista.mapa;

/**
 * El salón de baile del piano gigante (capítulo 6). Al norte, un escenario con la bailarina de la caja de música gigante y la pantalla
 * grande arriba. En medio, cuatro pianos de piso, uno para cada pieza, con cortinas altas a los dos lados (para que nadie copie) y una
 * escalera al frente con su número (cada quien sube a su pista): cada piano es un
 * cuadro de 6×6 con un tapete de 2×2 al centro (donde se para la pieza) y ocho teclas de colores de 2×2 alrededor (do, re, mi… en el
 * sentido del reloj, empezando al norte). Debajo de cada piano, un foso con picos: cuando alguien pierde, se abre el piso de su piano.
 * <pre>
 *   x: 0 ... 63   z: -1 ... 43   y: -3 ... 16
 *   el pasillo x 2..15 (entra al salón por x 16), el salón x 17..61 / z 0..42 (alto 14); todo es simétrico respecto a x 39 (la
 *   bailarina): los pianos en x 22..29, 31..38, 40..47 y 49..56, las cortinas en x 21, 30, 39, 48 y 57, y entre la pared y la
 *   cortina de cada orilla quedan 4 de ancho para pasar (x 17..20 y 58..61)
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
    // simétricos con la bailarina (x 39): la cortina de en medio queda justo frente a ella y a los dos lados quedan 4 de ancho para pasar
    public static final int[] CX = {25, 34, 43, 52};
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
        Pasaje.pasillo(p, "pas_piano", new Marco(2, 26, 270), Pasaje.BAILE);
        // se llega caminando desde la sala anterior (el pasillo de en medio es la zona "paso_...")
        Salida.llegada(p, C + "madera_pulida");
        salon(p);
        escenario(p);
        for (int k = 0; k < PIANOS; k++) {
            piano(p, k);
        }
        // la puerta de salida hacia el capítulo siguiente (hacia adelante)
        Salida.puerta(p, "salida.piano", 62, 37, 63, C + "madera_pulida");
        adornos(p);
        return p;
    }

    // ------------------------------------------------------------------ los adornos: el salón de música

    /**
     * El salón de música: arpas y atriles con partituras en el escenario de la bailarina, bustos de mármol y palmas en las esquinas,
     * violines en el muro de la entrada y notas musicales colgando del techo sobre la parte de atrás del salón.
     */
    private static void adornos(Plano p) {
        p.set(29, 2, 3, C + "arpa[facing=south,half=lower]");
        p.set(29, 3, 3, C + "arpa[facing=south,half=upper]");
        p.set(49, 2, 3, C + "arpa[facing=south,half=lower]");
        p.set(49, 3, 3, C + "arpa[facing=south,half=upper]");
        p.set(32, 2, 6, C + "atril[facing=south]");
        p.set(46, 2, 6, C + "atril[facing=south]");
        p.set(31, 2, 2, C + "papeles");
        p.set(47, 2, 2, C + "papeles");
        for (int x : new int[] {HX0 + 2, HX1 - 2}) {
            p.set(x, 1, HZ0 + 2, C + "busto[facing=south,half=lower]");
            p.set(x, 2, HZ0 + 2, C + "busto[facing=south,half=upper]");
            p.set(x + (x < 39 ? 3 : -3), 1, HZ0 + 1, C + "planta[facing=south,half=lower]");
            p.set(x + (x < 39 ? 3 : -3), 2, HZ0 + 1, C + "planta[facing=south,half=upper]");
        }
        for (int z : new int[] {6, 36}) {
            p.set(HX0, 6, z, C + "violin[facing=east]");
        }
        for (int[] q : new int[][] {{22, 36}, {30, 38}, {34, 35}, {44, 38}, {48, 35}, {56, 36}, {39, 40}}) {
            p.set(q[0], ALTO - 1, q[1], C + "nota_musical[facing=" + (q[0] % 2 == 0 ? "south" : "east") + "]");
        }
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
        // la pantalla grande arriba del escenario, delante del telón del muro del norte (metida en el telón, la tapaba)
        p.set(BX, 10, HZ0 + 1, C + "pantalla[facing=south,grande=true]");
        p.marca("piano.pantalla", BX, 10, HZ0 + 1, 0);
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
        // la escalera al sur de la tarima: tres escalones (de 2 de ancho) para subir a la pista, con su número arriba
        for (int x = cx; x <= cx + 1; x++) {
            for (int i = 0; i < 3; i++) {
                int z = cz + 5 + i, y = PISO - i;
                p.set(x, y, z, C + "escalera_tablon[facing=north]");
                for (int yy = 1; yy < y; yy++) {
                    p.set(x, yy, z, C + "panel_madera");
                }
                for (int yy = y + 1; yy <= y + 3; yy++) {
                    p.set(x, yy, z, "air");
                }
            }
        }
        // el número de la pista, en la pared de la tarima a los dos lados de la escalera
        p.texto(cx - 1, 2, cz + 5, "placa", "south", 0xD9A93A, String.valueOf(k + 1));
        p.texto(cx + 2, 2, cz + 5, "placa", "south", 0xD9A93A, String.valueOf(k + 1));
        // las cortinas a los DOS lados de cada piano (entre dos pianos, una sola): altas, para que nadie vea las teclas del otro, y parejas
        // (el primero y el último también tienen la suya hacia la pared)
        int oeste = k == 0 ? cx - 4 : (CX[k - 1] + CX[k]) / 2 + 1;
        int este = k == PIANOS - 1 ? cx + 5 : (CX[k] + CX[k + 1]) / 2 + 1;
        for (int xc : new int[] {oeste, este}) {
            for (int z = cz - 5; z <= cz + 7; z++) {
                for (int y = 1; y <= PISO + 5; y++) {
                    p.set(xc, y, z, y == PISO + 5 ? C + "telon_borde" : C + "telon");
                }
            }
        }
        // la luz de cada piano (no se baja): fuerte, para que las teclas se vean bien; y una tenue en el foso
        luz(p, cx, PISO + 5, cz, 15);
        luz(p, cx, PISO + 3, cz + 1, 14);
        luz(p, cx + 1, PISO + 3, cz, 14);
        luz(p, cx, 0, cz, 9);
        p.marca("piano.anillo." + k, cx, PISO, cz, 180);
    }
}
