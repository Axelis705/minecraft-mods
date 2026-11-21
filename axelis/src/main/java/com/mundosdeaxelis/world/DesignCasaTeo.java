package com.mundosdeaxelis.world;

/**
 * Casa de Teo (etapa 261): la biblioteca del bibliotecario, que crece al poner su Semilla de casa. Casita de calcita blanca con
 * entramado de roble oscuro y tejado empinado de madera carmesí; una torrecilla de lectura en una esquina con un catalejo de cobre
 * en lo alto; un farol de almas en la puerta. Dentro: estanterías del suelo al techo, su cama, un escritorio con velas y un atril,
 * un sillón de lectura y libros por todas partes. Todo con bloques baratos (si se muda, la casa se desarma sola).
 * <p>Puerta en z = 1 (z = 0 es el camino); suelo en y = -1; el habitante se queda en (0, 0, 4).
 */
final class DesignCasaTeo {
    private DesignCasaTeo() {
    }

    static final int X = 3, Z0 = 1, Z1 = 7;
    private static final String CA = "calcite";
    private static final String LOG = "dark_oak_log[axis=y]";

    static StructBp make() {
        StructBp b = new StructBp(2614);
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean wall = Math.abs(x) == X || z == Z0 || z == Z1;
                b.set(x, -1, z, wall ? "cobbled_deepslate" : ((x + z) & 1) == 0 ? "spruce_planks" : "dark_oak_planks");
            }
        }
        b.foundation(-X, Z0, X, Z1, "cobbled_deepslate");
        // zócalo de piedra oscura (y 0), calcita con entramado de roble oscuro (y 1..3)
        for (int y = 0; y <= 3; y++) {
            for (int x = -X; x <= X; x++) {
                for (int z = Z0; z <= Z1; z++) {
                    boolean sideX = Math.abs(x) == X, sideZ = z == Z0 || z == Z1;
                    if (!sideX && !sideZ) {
                        continue;
                    }
                    String s;
                    if (sideX && sideZ) {
                        s = LOG;
                    } else if (y == 0) {
                        s = StructKit.hash01(2615, x, z) < 0.3 ? "mossy_cobblestone" : "cobbled_deepslate";
                    } else if (y == 3) {
                        s = sideX ? "stripped_dark_oak_log[axis=z]" : "stripped_dark_oak_log[axis=x]";
                    } else {
                        s = CA;
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            b.set(-X, y, 4, LOG);
            b.set(X, y, 4, LOG);
        }
        // puerta, ventanas con cristal morado arriba
        StructKit.woodDoor(b, "dark_oak", 0, 0, Z0, "+z", false);
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, Z0, "glass_pane");
            b.set(2, y, Z0, "glass_pane");
            b.set(-X, y, 3, "glass_pane");
            b.set(X, y, 2, "glass_pane");
            b.set(0, y, Z1, "glass_pane");
        }
        b.set(0, 2, Z0, "stripped_dark_oak_log[axis=x]");
        // tejado empinado de madera carmesí (cumbrera de frente)
        StructKit.gableZ(b, -X, X, Z0, Z1, 4, 1, 1, "crimson_stairs", "crimson_planks", "crimson_planks", true, 1);
        b.set(0, 5, Z0, "purple_stained_glass_pane");
        // torrecilla de lectura en la esquina de atrás a la derecha, con un catalejo de cobre arriba
        int tx = X + 1, tz = Z1 - 1;
        for (int y = -1; y <= 6; y++) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    b.set(tx + dx, y, tz + dz, y == -1 ? "cobbled_deepslate" : y >= 5 ? "dark_oak_planks" : (dx == 1 || dz == 1) ? CA : "air");
                }
            }
        }
        b.foundation(tx, tz, tx + 1, tz + 1, "cobbled_deepslate");
        b.set(tx + 1, 2, tz, "purple_stained_glass_pane");
        b.set(tx, 2, tz + 1, "purple_stained_glass_pane");
        b.set(tx, 0, tz, "bookshelf");
        b.set(tx, 1, tz, "bookshelf");
        b.set(tx, 2, tz, "candle[candles=2,lit=true]");
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                b.set(tx + dx, 7, tz + dz, StructBp.slab("crimson_slab", false));
            }
        }
        b.set(tx, 8, tz, "lightning_rod[facing=up]");
        b.set(tx + 1, 8, tz + 1, "waxed_cut_copper_slab[type=bottom]");
        // dentro: estanterías de pared, la cama, un escritorio con atril y velas, y el sillón de lectura
        for (int z = 2; z <= 6; z++) {
            if (z == 3) {
                continue; // la ventana
            }
            b.set(-2, 0, z, z >= 5 ? "air" : "bookshelf");
            b.set(-2, 1, z, z >= 5 ? "air" : "bookshelf");
        }
        b.set(-2, 0, 5, "purple_bed[facing=south,part=foot]");
        b.set(-2, 0, 6, "purple_bed[facing=south,part=head]");
        b.set(-1, 0, 6, "chiseled_bookshelf[facing=north]");
        b.set(-1, 1, 6, "lantern");
        b.set(1, 0, 6, StructBp.slab("dark_oak_slab", true));
        b.set(2, 0, 6, StructBp.slab("dark_oak_slab", true));
        b.set(1, 1, 6, "candle[candles=3,lit=true]");
        b.set(2, 1, 6, "lectern[facing=north]");
        b.set(1, 0, 5, StructBp.stair("dark_oak_stairs", "+z", false));
        b.set(2, 0, 2, StructBp.stair("crimson_stairs", "-z", false)); // el sillón mirando a la puerta
        b.set(2, 0, 3, "bookshelf");
        b.set(2, 1, 3, "potted_allium");
        for (int x = -1; x <= 1; x++) {
            for (int z = 3; z <= 4; z++) {
                b.set(x, 0, z, ((x + z) & 1) == 0 ? "purple_carpet" : "magenta_carpet");
            }
        }
        StructKit.hangLantern(b, 0, 6, 4, 2, "soul_lantern");
        // farol de almas en la puerta y un buzón de libros (barril) junto al camino
        b.set(-2, 0, Z0 - 1, "dark_oak_fence");
        b.set(-2, 1, Z0 - 1, "soul_lantern[hanging=false]");
        b.set(2, 0, Z0 - 1, "barrel[facing=up]");
        for (int z = -1; z <= 0; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, x == 0 ? "dirt_path" : "mossy_cobblestone");
            }
        }
        b.foundation(-1, -1, 1, 0, "dirt");
        return b;
    }
}
