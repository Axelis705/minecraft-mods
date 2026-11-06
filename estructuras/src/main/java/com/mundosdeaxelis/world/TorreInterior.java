package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Interior de la Torre del Hechicero, diseñado a mano sala por sala (nada se coloca "donde encaje": cada pieza tiene su casilla).
 * Todas las salas usan la decoración nueva (bloques {@code est_*}, ver {@link EstIds}) y cada una tiene su propio tema y su propia luz.
 * Coordenadas: x desde el centro, y absoluta, z = CZ + dz (dz negativo = hacia la entrada).
 */
final class TorreInterior {
    private TorreInterior() {
    }

    static final int CZ = DesignTorre.CZ;
    static final String DB = "deepslate_bricks";
    static final String DBS = "deepslate_brick_stairs";
    static final String DBL = "deepslate_brick_slab";
    static final String DBW = "deepslate_brick_wall";
    static final String CR = "cracked_deepslate_bricks";
    static final String DT = "deepslate_tiles";
    static final String PD = "polished_deepslate";
    static final String CD = "chiseled_deepslate";
    static final String PP = "purpur_pillar[axis=y]";
    static final String PPS = "purpur_stairs";
    static final String AM = "amethyst_block";
    static final String PB = "polished_blackstone";
    static final String GOLD = "gold_block";
    static final String CHAIN = "chain[axis=y]";

    // ------------------------------------------------------------------ ayudas

    /** true si (x, dz) es suelo útil del piso i (dentro del octógono interior). */
    static boolean in(int i, int x, int dz) {
        return DesignTorre.inI(x, dz, DesignTorre.rad(i));
    }

    /** El macizo de la escalera central (5x5). */
    static boolean core(int x, int dz) {
        return Math.abs(x) <= 2 && Math.abs(dz) <= 2;
    }

    static int cheb(int x, int dz) {
        return Math.max(Math.abs(x), Math.abs(dz));
    }

    /** Casilla de la galería del piso de arriba (y = losa del piso 1): tiras junto al muro y franjas diagonales en las esquinas. */
    static boolean gal(int x, int dz) {
        if (!in(0, x, dz) || core(x, dz) || dz > 7) {
            return false;
        }
        return cheb(x, dz) >= 8 || Math.abs(x) + Math.abs(dz) >= 12;
    }

    /** Casilla vacía del salón (ni galería, ni macizo, ni puente): lo que hay al otro lado de la barandilla. */
    static boolean hole(int x, int dz) {
        if (!in(0, x, dz) || core(x, dz) || gal(x, dz)) {
            return false;
        }
        return !(Math.abs(x) <= 2 && dz >= -7 && dz <= -3); // el puente
    }

    /** Si la casilla de galería (x, dz) da al vacío, hacia qué lado (dx, dz); si no, null. */
    static int[] voidSide(int x, int dz) {
        int[][] ds = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : ds) {
            if (hole(x + d[0], dz + d[1])) {
                return d;
            }
        }
        return null;
    }

    /** Casilla libre para poner decoración: aire (o una alfombra, que se puede cubrir). */
    static boolean free(StructBp b, int x, int y, int z) {
        String g = b.get(x, y, z);
        return "air".equals(g) || g != null && g.endsWith("_carpet");
    }

    private static void skip(String id, int x, int y, int z) {
        System.err.println("[TorreInterior] sin sitio: " + id.replace("mundos_de_axelis:est_", "") + " en " + x + "," + y + "," + z);
    }

    /** Decoración nueva con orientación. face: north, south, east, west (hacia dónde mira el frente). Si la casilla no está libre, no se pone. */
    static void est(StructBp b, int x, int y, int z, String id, String face) {
        if (free(b, x, y, z)) {
            b.set(x, y, z, id + "[facing=" + face + "]");
        } else {
            skip(id, x, y, z);
        }
    }

    /** Decoración nueva de dos bloques de alto. */
    static void estTall(StructBp b, int x, int y, int z, String id, String face) {
        if (free(b, x, y, z) && free(b, x, y + 1, z)) {
            b.set(x, y, z, id + "[facing=" + face + ",half=lower]");
            b.set(x, y + 1, z, id + "[facing=" + face + ",half=upper]");
        } else {
            skip(id, x, y, z);
        }
    }

    /** Araña de luces o jaula colgada de una cadena: la cadena sale del techo (yTop) y baja {@code drop} bloques. */
    static void estHang(StructBp b, int x, int yTop, int z, int drop, String id) {
        if (!free(b, x, yTop, z)) {
            skip(id, x, yTop, z);
            return;
        }
        for (int i = 0; i < drop; i++) {
            b.set(x, yTop - i, z, CHAIN);
        }
        b.set(x, yTop - drop, z, id + "[facing=north]");
    }

    /** Columna oscura de deepslate con base y capitel labrados. */
    static void pillar(StructBp b, int x, int z, int y0, int y1) {
        b.set(x, y0, z, CD);
        for (int y = y0 + 1; y < y1; y++) {
            b.set(x, y, z, "deepslate[axis=y]");
        }
        b.set(x, y1, z, CD);
        b.set(x, y1 - 1, z, PD);
    }

    /** Alfombra solo si la casilla está libre. */
    static void carpet(StructBp b, int x, int y, int z, String c) {
        if ("air".equals(b.get(x, y, z))) {
            b.set(x, y, z, c);
        }
    }

    static String dir(String face) {
        return switch (face) {
            case "north" -> "-z";
            case "south" -> "+z";
            case "east" -> "+x";
            default -> "-x";
        };
    }

    // ================================================================== GRAN SALÓN (pisos 0 y 1, 15 bloques de alto)

    /**
     * Gran Salón: una nave de dos pisos de alto alrededor del macizo de la escalera. Se entra por el portón de la fachada (alfombra roja
     * hasta el cruce de caminos), se rodea el macizo por dos pasillos y al fondo está el trono bajo una gran vidriera.
     * El piso de arriba es una galería en forma de U (biblioteca) que da al salón; un puente la une con la escalera.
     */
    static void hall(StructBp b) {
        final int f = 3;
        final int g = 10; // losa de la galería
        final int top = 17;
        // ---- suelo de damero
        for (int x = -10; x <= 10; x++) {
            for (int dz = -10; dz <= 10; dz++) {
                if (in(0, x, dz) && !core(x, dz)) {
                    b.set(x, f - 1, CZ + dz, ((x + dz) & 1) == 0 ? PB : DT);
                }
            }
        }
        // ---- alfombras: camino de entrada, anillo alrededor del macizo y pie del trono
        for (int x = -10; x <= 10; x++) {
            for (int dz = -10; dz <= 10; dz++) {
                if (!in(0, x, dz) || core(x, dz)) {
                    continue;
                }
                int ch = cheb(x, dz);
                String c = null;
                if (Math.abs(x) <= 1 && (dz <= -6 || dz == 6 || dz == 7)) {
                    c = "red_carpet";
                } else if (Math.abs(x) == 2 && dz <= -6) {
                    c = "yellow_carpet";
                } else if (ch >= 3 && ch <= 5) {
                    c = "red_carpet";
                } else if (ch == 6 && !(Math.abs(x) <= 2 && dz <= -6)) {
                    c = "yellow_carpet";
                }
                if (c != null && !(dz >= 8)) {
                    carpet(b, x, f, CZ + dz, c);
                }
            }
        }
        // ---- estrado del trono
        for (int dz = 8; dz <= 10; dz++) {
            for (int x = -6; x <= 6; x++) {
                if (in(0, x, dz)) {
                    b.set(x, f, CZ + dz, Math.abs(x) >= 5 || dz == 8 ? PPS.replace("stairs", "block") : PD);
                    b.set(x, f - 1, CZ + dz, PD);
                }
            }
        }
        for (int dz = 9; dz <= 10; dz++) {
            for (int x = -3; x <= 3; x++) {
                b.set(x, f, CZ + dz, PD);
                b.set(x, f + 1, CZ + dz, "purpur_block");
                carpet(b, x, f + 2, CZ + dz, Math.abs(x) <= 1 ? "red_carpet" : "yellow_carpet");
            }
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, f + 1, CZ + 8, st(PPS, "+z"));
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, f, CZ + 7, st(PPS, "+z"));
        }
        // trono, candelabros y gárgolas
        estTall(b, 0, f + 2, CZ + 10, EstIds.TRONO, "north");
        est(b, -2, f + 2, CZ + 10, EstIds.CANDELABRO, "north");
        est(b, 2, f + 2, CZ + 10, EstIds.CANDELABRO, "north");
        est(b, -3, f + 2, CZ + 10, EstIds.GARGOLA, "north");
        est(b, 3, f + 2, CZ + 10, EstIds.GARGOLA, "north");
        for (int s = -1; s <= 1; s += 2) {
            estTall(b, 4 * s, f + 1, CZ + 10, EstIds.TAPIZ_REAL, "north");
            estTall(b, 4 * s, f + 3, CZ + 10, EstIds.TAPIZ_ESTRELLAS, "north");
            est(b, 6 * s, f + 1, CZ + 8, EstIds.CANDELABRO, "north");
        }
        // ---- vidriera tras el trono (2 de grosor: dz 11 y 12)
        for (int y = 7; y <= 16; y++) {
            int hw = y <= 13 ? 3 : (y == 14 ? 2 : (y == 15 ? 1 : 0));
            String glass = y <= 10 ? "magenta_stained_glass_pane" : (y <= 13 ? "purple_stained_glass_pane" : "blue_stained_glass_pane");
            for (int x = -3; x <= 3; x++) {
                for (int dz = 11; dz <= 12; dz++) {
                    if (Math.abs(x) <= hw) {
                        boolean bar = y == 11 || Math.abs(x) == 1 && y != 16;
                        b.set(x, y, CZ + dz, bar && Math.abs(x) != 0 || y == 11 ? PP : glass);
                    } else if (y <= 15) {
                        b.set(x, y, CZ + dz, PD);
                    }
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            b.set(x, 17, CZ + 11, DB);
            b.set(x, 17, CZ + 12, DB);
        }
        // ---- columnas del salón
        int[][] cols = {{7, 7}, {-7, 7}, {7, -7}, {-7, -7}, {4, 7}, {-4, 7}, {4, -7}, {-4, -7}, {7, 4}, {-7, 4}, {7, -4}, {-7, -4}, {7, 0}, {-7, 0}};
        for (int[] c : cols) {
            pillar(b, c[0], CZ + c[1], f, g - 1);
            int ix = Math.abs(c[0]) == 7 ? c[0] - Integer.signum(c[0]) : c[0];
            int iz = Math.abs(c[0]) == 7 ? c[1] : c[1] - Integer.signum(c[1]);
            est(b, ix, f, CZ + iz, EstIds.CANDELABRO, "north");
        }
        // ---- galería en U (y = g) y su barandilla: dos tiras junto al muro y una franja diagonal que las une en cada esquina
        for (int x = -10; x <= 10; x++) {
            for (int dz = -10; dz <= 7; dz++) {
                if (!gal(x, dz)) {
                    continue;
                }
                b.set(x, g, CZ + dz, ((x + dz) & 1) == 0 ? PD : DT);
                int[] v = voidSide(x, dz);
                if (v != null) {
                    // borde: repisa de escaleras boca abajo y barandilla
                    String face = v[0] > 0 ? "+x" : v[0] < 0 ? "-x" : v[1] > 0 ? "+z" : "-z";
                    b.set(x, g - 1, CZ + dz, sti(DBS, face));
                    b.set(x, g + 1, CZ + dz, ((x + dz) % 3 == 0) ? PP : DBW);
                }
            }
        }
        // ---- puente: de la escalera a la galería de la entrada
        for (int dz = -7; dz <= -3; dz++) {
            for (int x = -2; x <= 2; x++) {
                b.set(x, g, CZ + dz, Math.abs(x) == 2 ? DB : PD);
                if (Math.abs(x) == 2) {
                    b.set(x, g + 1, CZ + dz, dz % 2 == 0 ? PP : DBW);
                    b.set(x, g - 1, CZ + dz, sti(DBS, x < 0 ? "+x" : "-x"));
                }
            }
        }
        for (int x = -1; x <= 1; x++) {
            carpet(b, x, g + 1, CZ - 7, "red_carpet");
            for (int dz = -6; dz <= -3; dz++) {
                carpet(b, x, g + 1, CZ + dz, "red_carpet");
            }
        }
        // ---- arañas de luces (cadena desde el techo)
        int[][] lamps = {{0, -7, 4}, {-6, -6, 3}, {6, -6, 3}, {-6, 0, 5}, {6, 0, 5}, {0, 5, 4}, {-5, 5, 3}, {5, 5, 3}};
        for (int[] l : lamps) {
            estHang(b, l[0], top, CZ + l[1], l[2], EstIds.ARANA);
        }
        // ---- el macizo de la escalera como monumento: tapices a ambos lados de la puerta y en las otras caras
        for (int s = -1; s <= 1; s += 2) {
            estTall(b, 2 * s, f + 1, CZ - 3, EstIds.TAPIZ_ARCANO, "north");
            estTall(b, 2 * s, f + 3, CZ - 3, EstIds.TAPIZ_ESTRELLAS, "north");
            estTall(b, 3 * s, f + 1, CZ - 1, EstIds.TAPIZ_ARBOL, s < 0 ? "west" : "east");
            estTall(b, 3 * s, f + 1, CZ + 1, EstIds.TAPIZ_ARBOL, s < 0 ? "west" : "east");
            estTall(b, 3 * s, f + 3, CZ - 1, EstIds.TAPIZ_ARCANO, s < 0 ? "west" : "east");
            estTall(b, 3 * s, f + 3, CZ + 1, EstIds.TAPIZ_ARCANO, s < 0 ? "west" : "east");
            estTall(b, s, f + 1, CZ + 3, EstIds.TAPIZ_REAL, "south");
            estTall(b, s, f + 3, CZ + 3, EstIds.TAPIZ_ESTRELLAS, "south");
        }
        est(b, 0, f + 1, CZ + 3, EstIds.GLOBO, "south");
        // ---- entrada: caballeros de guardia a cada lado del camino
        for (int s = -1; s <= 1; s += 2) {
            estTall(b, 3 * s, f, CZ - 9, EstIds.ESTATUA_CABALLERO, "south");
            est(b, 3 * s, f, CZ - 7, EstIds.CANDELABRO, "south");
        }
        // ---- ala izquierda: armería
        for (int s = -1; s <= 1; s += 2) {
            estTall(b, -9, f, CZ + 2 * s, EstIds.ESTATUA_CABALLERO, "east");
            estTall(b, -9, f + 2, CZ + 2 * s, EstIds.TAPIZ_REAL, "east");
        }
        b.set(-9, f, CZ, "anvil[facing=north]");
        b.set(-8, f, CZ, "grindstone[face=floor,facing=east]");
        // ---- ala derecha: trofeos (gárgolas sobre pilares y jaulas colgantes)
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            pillar(b, 9, CZ + 2 * sgn, f, f + 1);
            est(b, 9, f + 2, CZ + 2 * sgn, EstIds.GARGOLA, "west");
        }
        estHang(b, 7, top, CZ - 2, 6, EstIds.JAULA);
        estHang(b, 7, top, CZ + 2, 7, EstIds.JAULA);
        est(b, 9, f, CZ, EstIds.MONTON_ORO, "west");
        // ---- galería de lectura: estanterías altas contra el muro y rincones de lectura
        for (int x = -10; x <= 10; x++) {
            for (int dz = -10; dz <= 6; dz++) {
                if (!in(0, x, dz) || cheb(x, dz) != 10) {
                    continue;
                }
                boolean sideWindow = Math.abs(x) == 10 && Math.abs(dz) <= 2; // ventana del ala
                boolean gate = dz == -10 && Math.abs(x) <= 1;                // sobre el portón
                if (sideWindow || gate) {
                    continue;
                }
                String face = Math.abs(x) == 10 ? (x > 0 ? "west" : "east") : "south";
                estTall(b, x, g + 1, CZ + dz, EstIds.ESTANTERIA, face);
                b.set(x, g + 3, CZ + dz, "bookshelf");
                b.set(x, g + 4, CZ + dz, "bookshelf");
            }
        }
        // rincones de lectura en los dos alas (el pasillo de la barandilla queda libre) y candelabros sobre los postes de las esquinas
        for (int s = -1; s <= 1; s += 2) {
            String toHall = s < 0 ? "east" : "west";
            est(b, 10 * s, g + 1, CZ - 1, EstIds.SILLON, toHall);
            est(b, 10 * s, g + 1, CZ, EstIds.PILA_LIBROS, toHall);
            est(b, 10 * s, g + 1, CZ + 1, EstIds.SILLON, toHall);
            for (int sz = -1; sz <= 1; sz += 2) {
                est(b, 6 * s, g + 2, CZ + 6 * sz, EstIds.CANDELABRO, "north");
            }
        }
    }
}
