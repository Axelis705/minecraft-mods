package com.coleccionista.mapa;

/**
 * El Ala de celdas: donde despiertan las piezas. Un salón largo de dos pisos (la colección del Coleccionista: museo y prisión a la vez)
 * con 24 celdas, 6 por lado y por piso, mirando a un pasillo con alfombra roja. Arriba, pasarelas con barandal. Al oeste, la pantalla
 * gigante; al este, las escaleras y el portón de salida.
 * <pre>
 *   x: -8 ... 44 (vestíbulo oeste -7..-1, celdas 0..36, vestíbulo este 37..43)
 *   z: 0 ... 22 (celdas sur 1..5, pasillo 7..15, celdas norte 17..21)
 *   y: 0 ... 10 (piso de abajo 1..4, piso de arriba 6..9)
 * </pre>
 * Celdas: 1-6 abajo al sur, 7-12 abajo al norte, 13-18 arriba al sur, 19-24 arriba al norte (de oeste a este).
 * Marcas por celda N: {@code celda.N} (donde aparece, mirando a su pantalla), {@code celda.N.pantalla}, {@code celda.N.puerta} (mitad de
 * abajo de la puerta de hierro), {@code celda.N.candado}, {@code celda.N.barril}, {@code celda.N.letrero} y {@code celda.N.letrero2}
 * (pistas en las paredes), {@code celda.N.cama}, {@code celda.N.foco}, {@code celda.N.cam1} y {@code celda.N.cam2} (la cámara del despertar).
 * Generales: {@code pantalla_grande}, {@code salida}, {@code centro}.
 */
public final class AlaCeldas {
    public static final int CELDAS = 24;
    // muros
    private static final String[] MURO = {"deepslate_bricks", "deepslate_bricks", "cracked_deepslate_bricks", "polished_deepslate"};
    private static final String[] PISO_CELDA = {"stone_bricks", "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks"};
    private static final String[] PARED_CELDA = {"stone_bricks", "stone_bricks", "stone_bricks", "cracked_stone_bricks", "mossy_stone_bricks"};
    private static final String VIGA = "stripped_dark_oak_log[axis=y]";

    private AlaCeldas() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        int xa = -8, xb = 44, za = 0, zb = 22, ya = 0, yb = 10;
        // casco: piso, techo y muros exteriores
        p.cajaMezcla(xa, ya, za, xb, ya, zb, "polished_deepslate", "deepslate_tiles");
        p.cajaMezcla(xa, yb, za, xb, yb, zb, "dark_oak_planks");
        for (int y = ya + 1; y < yb; y++) {
            for (int x = xa; x <= xb; x++) {
                p.set(x, y, za, MURO[p.r.nextInt(MURO.length)]);
                p.set(x, y, zb, MURO[p.r.nextInt(MURO.length)]);
            }
            for (int z = za; z <= zb; z++) {
                p.set(xa, y, z, MURO[p.r.nextInt(MURO.length)]);
                p.set(xb, y, z, MURO[p.r.nextInt(MURO.length)]);
            }
        }
        // aire adentro (el salón entero), luego se llena
        p.caja(xa + 1, ya + 1, za + 1, xb - 1, yb - 1, zb - 1, "air");
        pasillo(p);
        for (int k = 0; k < 6; k++) {
            int xs = 1 + 6 * k;
            celda(p, 1 + k, xs, 0, true);
            celda(p, 7 + k, xs, 0, false);
            celda(p, 13 + k, xs, 5, true);
            celda(p, 19 + k, xs, 5, false);
        }
        // paredes entre celdas (columnas de madera oscura al frente)
        for (int k = 0; k <= 6; k++) {
            int x = 6 * k;
            for (int y = 1; y <= 9; y++) {
                for (int z = 1; z <= 5; z++) {
                    p.set(x, y, z, y == 5 ? "dark_oak_planks" : PARED_CELDA[p.r.nextInt(PARED_CELDA.length)]);
                    p.set(x, y, 22 - z, y == 5 ? "dark_oak_planks" : PARED_CELDA[p.r.nextInt(PARED_CELDA.length)]);
                }
                p.set(x, y, 6, VIGA);
                p.set(x, y, 16, VIGA);
            }
        }
        pasarelas(p);
        vestibuloOeste(p);
        vestibuloEste(p);
        luces(p);
        p.marca("centro", 18, 1, 11, 90);
        return p;
    }

    // ------------------------------------------------------------------ pasillo

    private static void pasillo(Plano p) {
        for (int x = -7; x <= 43; x++) {
            for (int z = 7; z <= 15; z++) {
                String s;
                if (z == 7 || z == 15) {
                    s = "polished_blackstone_bricks";
                } else if (z >= 10 && z <= 12) {
                    s = "dark_oak_planks";
                } else {
                    s = p.elegir("spruce_planks", "spruce_planks", "dark_oak_planks");
                }
                p.set(x, 0, z, s);
            }
            // alfombra roja en medio (con orilla negra)
            for (int z = 10; z <= 12; z++) {
                p.set(x, 1, z, z == 11 ? "red_carpet" : "black_carpet");
            }
        }
    }

    // ------------------------------------------------------------------ una celda

    /**
     * Celda {@code n} con su interior en x = xs..xs+4. {@code sur}: del lado sur (interior z 1..5, frente en z 6) o norte (interior z
     * 17..21, frente en z 16). {@code dy}: 0 abajo, 5 arriba.
     */
    private static void celda(Plano p, int n, int xs, int dy, boolean sur) {
        int zf = sur ? 6 : 16; // muro del frente (barrotes)
        int zi0 = sur ? 1 : 17, zi1 = sur ? 5 : 21; // interior
        int zFondo = sur ? 1 : 21; // la fila pegada a la pared del fondo
        int zFrente = sur ? 5 : 17; // la fila pegada a los barrotes
        int dz = sur ? 1 : -1; // hacia el pasillo
        String haciaPasillo = sur ? "south" : "north"; // hacia dónde mira algo puesto en el fondo
        String haciaFondo = sur ? "north" : "south";
        int y0 = dy; // piso de la celda
        // piso y techo
        p.cajaMezcla(xs, y0, zi0, xs + 4, y0, zi1, PISO_CELDA);
        p.caja(xs, y0 + 5, zi0, xs + 4, y0 + 5, zi1, "dark_oak_planks");
        // paredes del fondo y laterales (las laterales son las columnas, las pone crear)
        // frente: barrotes con dintel y la puerta en medio
        for (int x = xs; x <= xs + 4; x++) {
            for (int y = y0 + 1; y <= y0 + 4; y++) {
                p.set(x, y, zf, y == y0 + 4 ? "polished_blackstone_bricks" : "iron_bars");
            }
            p.set(x, y0, zf, "polished_blackstone");
        }
        int xp = xs + 2;
        p.set(xp, y0 + 1, zf, "iron_door[facing=" + haciaFondo + ",half=lower,hinge=left,open=false]");
        p.set(xp, y0 + 2, zf, "iron_door[facing=" + haciaFondo + ",half=upper,hinge=left,open=false]");
        p.marca("celda." + n + ".puerta", xp, y0 + 1, zf, sur ? 0 : 180);
        // número de la celda arriba de la puerta, del lado del pasillo
        p.letrero(xp, y0 + 4, zf + dz, "dark_oak_wall_sign", sur ? "south" : "north", "red", "", "PIEZA", String.format("%02d", n), "");
        // cama (cabecera contra el fondo, del lado oeste)
        p.set(xs, y0 + 1, zFondo, "red_bed[facing=" + haciaFondo + ",part=head]");
        p.set(xs, y0 + 1, zFondo + dz, "red_bed[facing=" + haciaFondo + ",part=foot]");
        p.marca("celda." + n + ".cama", xs, y0 + 1, zFondo, 0);
        // barril en la esquina del fondo al este, con una vela encima
        p.set(xs + 4, y0 + 1, zFondo, "barrel[facing=up]");
        p.set(xs + 4, y0 + 2, zFondo, "candle[candles=2,lit=true]");
        p.marca("celda." + n + ".barril", xs + 4, y0 + 1, zFondo, 0);
        // la pantalla en la pared del fondo, al centro (mira hacia la celda)
        p.set(xs + 2, y0 + 3, zFondo, "coleccionista:pantalla[facing=" + haciaPasillo + "]");
        p.marca("celda." + n + ".pantalla", xs + 2, y0 + 3, zFondo, sur ? 0 : 180);
        // el candado junto a la puerta, por dentro
        p.set(xs + 3, y0 + 2, zFrente, "coleccionista:candado[facing=" + haciaFondo + "]");
        p.marca("celda." + n + ".candado", xs + 3, y0 + 2, zFrente, sur ? 180 : 0);
        // dos rayones (pistas) en las paredes: uno en la pared este y otro arriba de los pies de la cama
        p.marca("celda." + n + ".letrero", xs + 4, y0 + 3, zi0 + 2, 90);
        p.marca("celda." + n + ".letrero2", xs, y0 + 2, zFondo + dz, 270);
        // tapete, mesita, telarañas y una lámpara colgada
        for (int x = xs + 1; x <= xs + 3; x++) {
            p.set(x, y0 + 1, zFondo + 2 * dz, "gray_carpet");
        }
        p.set(xs + 4, y0 + 1, zFrente, "dark_oak_fence");
        p.set(xs + 4, y0 + 2, zFrente, "dark_oak_pressure_plate");
        if (p.azar(0.7)) {
            p.set(xs, y0 + 4, zFondo, "cobweb");
        }
        if (p.azar(0.5)) {
            p.set(xs + 4, y0 + 4, zFrente, "cobweb");
        }
        // el foco del techo (lo prende el evento en el "¡pum!" del despertar)
        p.set(xs + 2, y0 + 4, zFondo + 2 * dz, "coleccionista:foco[encendido=false]");
        p.marca("celda." + n + ".foco", xs + 2, y0 + 4, zFondo + 2 * dz, 0);
        // la cámara del despertar: empieza arriba en la esquina de los barrotes y baja hasta la cama
        p.marca("celda." + n + ".cam1", xs + 4, y0 + 3, zFrente, 0);
        p.marca("celda." + n + ".cam2", xs + 1, y0 + 2, zFondo + 2 * dz, 0);
        // donde aparece: en medio, mirando a la pantalla
        p.marca("celda." + n, xs + 2, y0 + 1, zFrente - dz, sur ? 180 : 0);
    }

    // ------------------------------------------------------------------ pasarelas del segundo piso

    private static void pasarelas(Plano p) {
        for (int x = 0; x <= 36; x++) {
            for (int z = 7; z <= 9; z++) {
                p.set(x, 5, z, "dark_oak_slab[type=top]");
                p.set(x, 5, 22 - z, "dark_oak_slab[type=top]");
            }
            p.set(x, 6, 9, "iron_bars");
            p.set(x, 6, 13, "iron_bars");
            // ménsulas bajo las pasarelas, cada 6
            if (x % 6 == 0) {
                p.set(x, 4, 9, "dark_oak_stairs[facing=north,half=top]");
                p.set(x, 4, 13, "dark_oak_stairs[facing=south,half=top]");
            }
        }
    }

    // ------------------------------------------------------------------ vestíbulos

    private static void vestibuloOeste(Plano p) {
        // la pantalla gigante en la pared oeste: marco de piedra negra y la pantalla al centro
        for (int z = 5; z <= 17; z++) {
            for (int y = 1; y <= 9; y++) {
                boolean marco = z == 5 || z == 17 || y == 1 || y == 9;
                p.set(-8, y, z, marco ? "polished_blackstone_bricks" : "black_concrete");
            }
        }
        for (int z = 6; z <= 16; z++) {
            p.set(-7, 1, z, "polished_blackstone_stairs[facing=east]");
        }
        p.set(-7, 5, 11, "coleccionista:pantalla[facing=east,grande=true]");
        p.marca("pantalla_grande", -7, 5, 11, 270);
        // candelabros a los lados y estandartes rojos
        for (int z : new int[] {7, 15}) {
            p.set(-6, 1, z, "polished_blackstone_wall");
            p.set(-6, 2, z, "polished_blackstone_wall");
            p.set(-6, 3, z, "candle[candles=4,lit=true]");
        }
        for (int z : new int[] {3, 19}) {
            p.set(-7, 7, z, "red_wall_banner[facing=east]");
        }
        // esquinas: vitrinas de la colección (cristal con algo adentro)
        vitrina(p, -6, 2, "skeleton_skull[rotation=4]");
        vitrina(p, -6, 20, "skeleton_skull[rotation=4]");
        vitrina(p, -3, 2, "player_head[rotation=0]");
        vitrina(p, -3, 20, "player_head[rotation=8]");
    }

    private static void vitrina(Plano p, int x, int z, String cosa) {
        p.set(x, 1, z, "polished_blackstone");
        p.set(x, 2, z, cosa);
        p.set(x, 3, z, "glass");
        p.set(x, 4, z, "dark_oak_slab[type=bottom]");
    }

    private static void vestibuloEste(Plano p) {
        // escaleras a las pasarelas (bajan hacia el este)
        for (int i = 0; i < 5; i++) {
            int x = 37 + i, y = 5 - i;
            for (int z = 7; z <= 9; z++) {
                p.set(x, y, z, "dark_oak_stairs[facing=west,half=bottom]");
                p.set(x, y, 22 - z, "dark_oak_stairs[facing=west,half=bottom]");
                for (int yy = 1; yy < y; yy++) {
                    p.set(x, yy, z, "dark_oak_planks");
                    p.set(x, yy, 22 - z, "dark_oak_planks");
                }
            }
            p.set(x, y + 1, 9, "dark_oak_fence");
            p.set(x, y + 1, 13, "dark_oak_fence");
        }
        // el portón de salida (rejas de hierro de 3x4) en la pared este
        for (int z = 10; z <= 12; z++) {
            for (int y = 1; y <= 4; y++) {
                p.set(44, y, z, "iron_bars");
            }
            p.set(44, 5, z, "chiseled_polished_blackstone");
        }
        p.marca("salida", 43, 1, 11, 270);
        p.letrero(43, 6, 11, "dark_oak_wall_sign", "west", "red", "", "LA COLECCIÓN", "continúa", "");
    }

    // ------------------------------------------------------------------ luces

    private static void luces(Plano p) {
        // faroles de alma colgando del techo sobre el pasillo (luz fría y baja)
        for (int x = -4; x <= 40; x += 6) {
            p.set(x, 9, 11, "chain[axis=y]");
            p.set(x, 8, 11, "chain[axis=y]");
            p.set(x, 7, 11, "soul_lantern[hanging=true]");
        }
        // velas en las pasarelas
        for (int x = 3; x <= 33; x += 6) {
            p.set(x, 6, 7, "candle[candles=3,lit=true]");
            p.set(x, 6, 15, "candle[candles=3,lit=true]");
        }
    }
}
