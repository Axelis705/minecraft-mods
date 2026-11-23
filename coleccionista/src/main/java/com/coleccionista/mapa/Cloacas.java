package com.coleccionista.mapa;

/**
 * Las Cloacas: el mapa de la prueba 1, "Encadenados". Queda al este del Ala de celdas (su pasillo de entrada llega al portón de salida
 * del Ala). Todo es un bloque de roca con los túneles tallados por dentro; el recorrido va siempre hacia el este (así el gas avanza
 * como un frente de oeste a este).
 * <pre>
 *   x: -5 ... 112   z: 0 ... 44   y: -2 ... 8 (piso en y 0, se camina en y 1)
 *   -5..-1   pasillo desde el Ala de celdas (reja cerrada: de adorno)
 *    1..12   Sala de los grilletes: 24 lugares (12 filas), cada uno con su argolla y su barril
 *   14..29   túnel de entrada con canal (gira al norte)
 *   30..55   túnel norte con la Compuerta 1 (x 38: dos placas)
 *   56..61   cámara de las tuberías
 *   62..79   tres tuberías de cobre: solo una lleva al colector (se elige al azar en cada partida)
 *   80..85   colector
 *   87..98   cámara del puente (agua honda y un puente de una sola tabla)
 *  100..106  túnel final con la Compuerta 2 (x 104: dos placas)
 *  107       reja de la salida (se cierra cuando ya no hay lugar)
 *  108..111  el Refugio (a salvo)
 * </pre>
 * Marcas: {@code enc.lugar.N} (N = 0..23, dónde queda cada pieza encadenada, mirando a su pareja), {@code enc.lugar.N.barril},
 * {@code enc.lugar.N.argolla}, {@code enc.reja_inicio}, {@code enc.placa.K.a/b} y {@code enc.compuerta.K} (K = 1, 2),
 * {@code enc.tubo.1..3}, {@code enc.reja_salida}, {@code enc.salida}, {@code enc.pantalla.norte/sur/refugio}, {@code enc.centro}.
 */
public final class Cloacas {
    public static final int X0 = -5, X1 = 112, Z0 = 0, Z1 = 44, Y0 = -2, Y1 = 8;
    /** Cuántos lugares hay en la Sala de los grilletes (uno por celda del Ala). */
    public static final int LUGARES = 24;
    /** Dónde empieza y dónde termina el gas (x local). */
    public static final int GAS_INICIO = 1, GAS_FIN = 107;
    /** El Refugio (x local desde donde se está a salvo). */
    public static final int REFUGIO_X = 108;
    /** Las compuertas: ancho (en z) y alto de las rejas. */
    public static final int COMPUERTA_ANCHO = 3, COMPUERTA_ALTO = 3;
    public static final int TUBO_ANCHO = 2, TUBO_ALTO = 3;
    public static final int SALIDA_ANCHO = 5, SALIDA_ALTO = 4;
    public static final int INICIO_ANCHO = 3, INICIO_ALTO = 4;

    private static final String[] ROCA = {"stone_bricks", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "mud_bricks",
            "tuff", "stone_bricks"};
    private static final String[] PISO = {"mud_bricks", "packed_mud", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks",
            "mud_bricks"};
    private static final String[] TECHO = {"deepslate_tiles", "deepslate_tiles", "cracked_deepslate_tiles", "polished_deepslate"};
    private static final String[] COBRE = {"waxed_weathered_cut_copper", "waxed_weathered_cut_copper", "waxed_exposed_cut_copper",
            "waxed_oxidized_cut_copper"};

    private Cloacas() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        // la roca (con lodo abajo)
        p.cajaMezcla(X0, Y0, Z0, X1, -1, Z1, "mud", "packed_mud", "stone");
        p.cajaMezcla(X0, 0, Z0, X1, Y1, Z1, ROCA);
        pasilloEntrada(p);
        salaGrilletes(p);
        tunelEntrada(p);
        tunelNorte(p);
        tuberias(p);
        puente(p);
        salida(p);
        return p;
    }

    // ------------------------------------------------------------------ ayudas

    /** Talla un espacio: piso en y 0, aire de y 1 a {@code alto}, techo encima. */
    private static void hueco(Plano p, int xa, int za, int xb, int zb, int alto) {
        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, PISO[p.r.nextInt(PISO.length)]);
                for (int y = 1; y <= alto; y++) {
                    p.set(x, y, z, "air");
                }
                if (alto + 1 <= Y1) {
                    p.set(x, alto + 1, z, TECHO[p.r.nextInt(TECHO.length)]);
                }
            }
        }
    }

    /** Un canal de agua sucia a ras del piso (una cuadra de hondo). */
    private static void canal(Plano p, int xa, int za, int xb, int zb) {
        for (int x = Math.min(xa, xb); x <= Math.max(xa, xb); x++) {
            for (int z = Math.min(za, zb); z <= Math.max(za, zb); z++) {
                p.set(x, 0, z, "water");
                p.set(x, -1, z, "mud");
            }
        }
    }

    private static void farol(Plano p, int x, int y, int z) {
        p.set(x, y, z, "lantern[hanging=true]");
    }

    private static void farolAlma(Plano p, int x, int y, int z) {
        p.set(x, y, z, "soul_lantern[hanging=true]");
    }

    /** Una reja de hierro de {@code ancho} (en z) por {@code alto}, con su base en (x, 1, z). */
    private static void reja(Plano p, int x, int z, int ancho, int alto) {
        for (int dz = 0; dz < ancho; dz++) {
            for (int y = 1; y <= alto; y++) {
                p.set(x, y, z + dz, "iron_bars");
            }
        }
    }

    /** Liquen que brilla verde (luz muy baja) pegado a una pared. */
    private static void liquen(Plano p, int x, int y, int z, String cara) {
        if (p.vacio(x, y, z)) {
            p.set(x, y, z, "glow_lichen[" + cara + "=true]");
        }
    }

    // ------------------------------------------------------------------ pasillo desde el Ala de celdas

    private static void pasilloEntrada(Plano p) {
        hueco(p, -5, 21, -1, 23, 4);
        for (int x = -5; x <= -1; x++) {
            p.set(x, 1, 22, x % 2 == 0 ? "black_carpet" : "red_carpet");
        }
        reja(p, 0, 21, 3, 4);
        farolAlma(p, -3, 4, 22);
    }

    // ------------------------------------------------------------------ sala de los grilletes

    private static void salaGrilletes(Plano p) {
        hueco(p, 1, 2, 12, 42, 7);
        // piso: losas oscuras y una línea negra entre las dos filas
        for (int x = 1; x <= 12; x++) {
            for (int z = 2; z <= 42; z++) {
                p.set(x, 0, z, x == 5 ? "polished_blackstone_bricks" : p.elegir("polished_deepslate", "deepslate_tiles", "deepslate_tiles",
                        "cracked_deepslate_tiles"));
            }
        }
        for (int k = 0; k < LUGARES / 2; k++) {
            // seis filas al norte y seis al sur de la reja por donde se llega del Ala de celdas (z 21..23)
            int z = k < 6 ? 4 + 3 * k : 25 + 3 * (k - 6);
            // A: contra la pared oeste, mirando al este
            int a = 2 * k, b = 2 * k + 1;
            p.set(1, 2, z, "tripwire_hook[facing=east]");
            p.marca("enc.lugar." + a + ".argolla", 1, 2, z, 270);
            p.set(1, 1, z - 1, "barrel[facing=east]");
            p.marca("enc.lugar." + a + ".barril", 1, 1, z - 1, 270);
            p.marca("enc.lugar." + a, 3, 1, z, 270);
            // B: contra un pilar en x 9, mirando al oeste
            for (int y = 1; y <= 7; y++) {
                p.set(9, y, z, y == 7 ? "chiseled_polished_blackstone" : "polished_blackstone_bricks");
            }
            p.set(8, 2, z, "tripwire_hook[facing=west]");
            p.marca("enc.lugar." + b + ".argolla", 8, 2, z, 90);
            p.set(8, 1, z - 1, "barrel[facing=west]");
            p.marca("enc.lugar." + b + ".barril", 8, 1, z - 1, 90);
            p.marca("enc.lugar." + b, 7, 1, z, 90);
            // una vela en algunos barriles
            if (k % 2 == 0) {
                p.set(1, 2, z - 1, "candle[candles=2,lit=true]");
            } else {
                p.set(8, 2, z - 1, "candle[candles=1,lit=true]");
            }
            // una cadena colgando del techo encima de cada par
            for (int y = 5; y <= 7; y++) {
                p.set(5, y, z, "chain[axis=y]");
            }
        }
        // la reja de salida en la pared este (se abre al empezar)
        reja(p, 13, 20, INICIO_ANCHO, INICIO_ALTO);
        for (int z = 20; z <= 22; z++) {
            p.set(13, 5, z, "chiseled_polished_blackstone");
        }
        p.marca("enc.reja_inicio", 13, 1, 20, 270);
        p.letrero(12, 6, 21, "dark_oak_wall_sign", "west", "red", "LO QUE", "TE ATA", "TAMBIÉN", "TE SALVA");
        // pantallas en las paredes de los extremos
        p.set(6, 4, 2, "coleccionista:pantalla[facing=south]");
        p.marca("enc.pantalla.norte", 6, 4, 2, 0);
        p.set(6, 4, 42, "coleccionista:pantalla[facing=north]");
        p.marca("enc.pantalla.sur", 6, 4, 42, 180);
        // faroles de alma (poca luz) y telarañas en las esquinas
        for (int z = 6; z <= 38; z += 8) {
            farolAlma(p, 11, 7, z);
        }
        for (int[] c : new int[][] {{1, 2}, {12, 2}, {1, 42}, {12, 42}}) {
            p.set(c[0], 7, c[1], "cobweb");
            p.set(c[0], 6, c[1], "cobweb");
        }
        p.marca("enc.centro", 10, 1, 21, 270);
    }

    // ------------------------------------------------------------------ túneles

    private static void tunelEntrada(Plano p) {
        // hacia el este...
        hueco(p, 14, 19, 28, 23, 4);
        canal(p, 14, 21, 27, 21);
        // ...y gira al norte
        hueco(p, 25, 6, 29, 23, 4);
        canal(p, 27, 8, 27, 21);
        for (int x = 14; x <= 24; x++) {
            p.set(x, 4, 19, "stone_brick_stairs[facing=north,half=top]");
            p.set(x, 4, 23, "stone_brick_stairs[facing=south,half=top]");
        }
        for (int z = 6; z <= 18; z++) {
            p.set(25, 4, z, "stone_brick_stairs[facing=west,half=top]");
            if (z > 10) {
                p.set(29, 4, z, "stone_brick_stairs[facing=east,half=top]");
            }
        }
        for (int x = 17; x <= 24; x += 7) {
            farol(p, x, 4, 21);
        }
        farol(p, 27, 4, 14);
        p.letrero(20, 3, 19, "dark_oak_wall_sign", "south", "red", "", "¿CONFÍAS EN", "QUIEN TE SIGUE?", "");
        // rejillas de desagüe en las paredes con liquen
        for (int x = 16; x <= 24; x += 4) {
            p.set(x, 1, 18, "iron_bars");
            liquen(p, x + 1, 1, 19, "north");
        }
        p.set(26, 1, 6, "cobweb");
    }

    private static void tunelNorte(Plano p) {
        hueco(p, 30, 6, 55, 10, 4);
        canal(p, 27, 8, 34, 8);
        p.set(35, 0, 8, "iron_trapdoor[half=top,open=false]");
        canal(p, 42, 8, 53, 8);
        for (int x = 30; x <= 55; x++) {
            if (x == 38) {
                continue;
            }
            p.set(x, 4, 6, "stone_brick_stairs[facing=north,half=top]");
            p.set(x, 4, 10, "stone_brick_stairs[facing=south,half=top]");
        }
        compuerta(p, 1, 38, 6, 36);
        farol(p, 33, 4, 8);
        farol(p, 46, 4, 8);
        farol(p, 53, 4, 8);
        for (int x = 42; x <= 52; x += 5) {
            liquen(p, x, 2, 6, "north");
            liquen(p, x + 2, 1, 10, "south");
        }
    }

    /**
     * La compuerta K: un muro en {@code x} que cruza el túnel (z0..z0+4) con la reja en medio (z0+1..z0+3) y dos placas en {@code xp},
     * una pegada a cada pared: una sola persona no puede pisar las dos.
     */
    private static void compuerta(Plano p, int k, int x, int z0, int xp) {
        for (int y = 1; y <= 4; y++) {
            p.set(x, y, z0, "polished_blackstone_bricks");
            p.set(x, y, z0 + 4, "polished_blackstone_bricks");
        }
        for (int z = z0 + 1; z <= z0 + 3; z++) {
            p.set(x, 4, z, "chiseled_polished_blackstone");
            p.set(x, 0, z, "polished_blackstone");
        }
        reja(p, x, z0 + 1, COMPUERTA_ANCHO, COMPUERTA_ALTO);
        p.marca("enc.compuerta." + k, x, 1, z0 + 1, 270);
        p.set(xp, 1, z0, "heavy_weighted_pressure_plate");
        p.set(xp, 1, z0 + 4, "heavy_weighted_pressure_plate");
        p.set(xp, 0, z0, "polished_blackstone");
        p.set(xp, 0, z0 + 4, "polished_blackstone");
        p.marca("enc.placa." + k + ".a", xp, 1, z0, 270);
        p.marca("enc.placa." + k + ".b", xp, 1, z0 + 4, 270);
        p.letrero(x - 1, 4, z0 + 2, "dark_oak_wall_sign", "west", "red", "", "DOS PESAN", "MÁS QUE UNO", "");
        // por si el canal llega hasta aquí: el piso junto a la compuerta es firme
        for (int z = z0; z <= z0 + 4; z++) {
            for (int dx = -1; dx <= 1; dx++) {
                if ("water".equals(p.get(x + dx, 0, z))) {
                    p.set(x + dx, 0, z, "polished_blackstone");
                }
            }
        }
    }

    // ------------------------------------------------------------------ tuberías (suerte)

    private static final int[] TUBOS_Z = {3, 14, 25};

    private static void tuberias(Plano p) {
        // la cámara: alta, con tres bocas de cobre en la pared este
        hueco(p, 56, 1, 61, 30, 6);
        for (int i = 0; i < 3; i++) {
            int z = TUBOS_Z[i];
            for (int x = 62; x <= 79; x++) {
                for (int y = 0; y <= 4; y++) {
                    for (int dz = -1; dz <= 2; dz++) {
                        boolean dentro = y >= 1 && y <= 3 && dz >= 0 && dz <= 1;
                        p.set(x, y, z + dz, dentro ? "air" : COBRE[p.r.nextInt(COBRE.length)]);
                    }
                }
                // anillos de la tubería cada 4
                if (x % 4 == 0) {
                    for (int y = 0; y <= 4; y++) {
                        p.set(x, y, z - 1, "waxed_copper_block");
                        p.set(x, y, z + 2, "waxed_copper_block");
                    }
                }
            }
            p.marca("enc.tubo." + (i + 1), 78, 1, z, 270);
            String num = i == 0 ? "I" : i == 1 ? "II" : "III";
            p.letrero(61, 4, z, "dark_oak_wall_sign", "west", "red", "", num, "", "");
            // un poco de luz en medio de cada tubería
            p.set(70, 3, z, "lantern[hanging=true]");
        }
        p.letrero(58, 4, 1, "dark_oak_wall_sign", "south", "red", "UNA SOLA", "LLEVA A", "LA SALIDA", "");
        p.letrero(59, 4, 30, "dark_oak_wall_sign", "north", "red", "LAS OTRAS", "SOLO TE", "HACEN", "PERDER TIEMPO");
        for (int z = 5; z <= 28; z += 7) {
            farolAlma(p, 58, 6, z);
        }
        for (int z = 2; z <= 29; z += 3) {
            liquen(p, 56, 1 + (z % 3), z, "west");
        }
        // el colector
        hueco(p, 80, 1, 85, 30, 6);
        for (int z = 4; z <= 26; z += 11) {
            farol(p, 83, 6, z);
        }
        for (int z = 2; z <= 29; z += 4) {
            if (z < 11 || z > 17) {
                p.set(85, 1, z, "iron_bars");
            }
        }
    }

    // ------------------------------------------------------------------ el puente

    private static void puente(Plano p) {
        hueco(p, 87, 6, 98, 22, 7);
        // bocas de la cámara
        hueco(p, 86, 12, 86, 16, 4);
        hueco(p, 99, 12, 99, 16, 4);
        // agua honda (dos de hondo) de lado a lado
        for (int x = 88; x <= 97; x++) {
            for (int z = 6; z <= 22; z++) {
                p.set(x, 0, z, "water");
                p.set(x, -1, z, "water");
                p.set(x, -2, z, "mud");
            }
        }
        // el puente: una sola tabla (sin barandal)
        for (int x = 88; x <= 97; x++) {
            p.set(x, 0, 14, "spruce_slab[type=top]");
        }
        // postes con cadenas en las puntas
        for (int z : new int[] {13, 15}) {
            p.set(87, 1, z, "spruce_fence");
            p.set(98, 1, z, "spruce_fence");
        }
        for (int x = 89; x <= 96; x += 7) {
            for (int y = 5; y <= 7; y++) {
                p.set(x, y, 14, "chain[axis=y]");
            }
            p.set(x, 4, 14, "lantern[hanging=true]");
        }
        p.letrero(87, 4, 10, "dark_oak_wall_sign", "east", "red", "", "NO MIRES", "ABAJO", "");
        for (int z = 7; z <= 21; z += 4) {
            liquen(p, 87, 2, z, "west");
            liquen(p, 98, 1, z + 1, "east");
        }
    }

    // ------------------------------------------------------------------ la salida y el refugio

    private static void salida(Plano p) {
        hueco(p, 100, 12, 106, 16, 4);
        compuerta(p, 2, 104, 12, 102);
        farol(p, 101, 4, 14);
        // la reja de la salida (abierta mientras haya lugar)
        hueco(p, 107, 12, 107, 16, 4);
        for (int z = 12; z <= 16; z++) {
            p.set(107, 5, z, "chiseled_polished_blackstone");
            p.set(107, 0, z, "polished_blackstone");
        }
        p.marca("enc.reja_salida", 107, 1, 12, 270);
        // el refugio: cálido y con luz (contraste con las cloacas)
        hueco(p, 108, 6, 111, 22, 6);
        for (int x = 108; x <= 111; x++) {
            for (int z = 6; z <= 22; z++) {
                p.set(x, 0, z, (x + z) % 2 == 0 ? "spruce_planks" : "dark_oak_planks");
            }
        }
        for (int z = 7; z <= 21; z += 2) {
            if (z >= 12 && z <= 16) {
                continue;
            }
            p.set(111, 1, z, "spruce_stairs[facing=east]");
        }
        farol(p, 109, 6, 8);
        farol(p, 110, 6, 12);
        farol(p, 109, 6, 16);
        farol(p, 110, 6, 20);
        p.set(111, 3, 14, "coleccionista:pantalla[facing=west]");
        p.marca("enc.pantalla.refugio", 111, 3, 14, 90);
        p.letrero(108, 4, 7, "spruce_wall_sign", "south", "white", "", "A SALVO", "...POR AHORA", "");
        p.marca("enc.salida", 110, 1, 14, 90);
    }
}
