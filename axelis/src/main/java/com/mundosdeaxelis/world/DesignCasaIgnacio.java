package com.mundosdeaxelis.world;

/**
 * Casa de Ignacio (etapa 262): la ermita del cultista arrepentido, que crece al poner su Semilla de casa. Casita de barro con
 * marcos de mangle y tejado rojizo a dos aguas (cumbrera de lado); un jardín de flores a cada lado (planta flores donde antes habría
 * prendido fuego), una pérgola con azaleas en la puerta y una fogata para cocinar. Dentro: su cama roja, un atril con su libro
 * («Cómo dejar una secta»), una mesa de trabajo, macetas y, en un estante, su vieja máscara de cultista convertida en adorno.
 * Todo con bloques baratos (si se muda, la casa se desarma sola).
 * <p>Puerta en z = 1 (z = 0 es el camino); suelo en y = -1; el habitante se queda en (0, 0, 4).
 */
final class DesignCasaIgnacio {
    private DesignCasaIgnacio() {
    }

    static final int X = 3, Z0 = 1, Z1 = 7;
    private static final String LOG = "mangrove_log[axis=y]";
    private static final String[] FLOWERS = {"poppy", "red_tulip", "orange_tulip", "dandelion", "cornflower", "allium", "oxeye_daisy"};

    static StructBp make() {
        StructBp b = new StructBp(2620);
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean wall = Math.abs(x) == X || z == Z0 || z == Z1;
                b.set(x, -1, z, wall ? "mud_bricks" : ((x + z) & 1) == 0 ? "mangrove_planks" : "spruce_planks");
            }
        }
        b.foundation(-X, Z0, X, Z1, "mud_bricks");
        // zócalo de ladrillo de barro (y 0), muros de barro con marcos de mangle (y 1..3)
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
                        s = "mud_bricks";
                    } else if (y == 3) {
                        s = sideX ? "stripped_mangrove_log[axis=z]" : "stripped_mangrove_log[axis=x]";
                    } else {
                        s = StructKit.hash01(2621, x * 7 + y, z) < 0.15 ? "mud_bricks" : "packed_mud";
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            b.set(-X, y, 4, LOG);
            b.set(X, y, 4, LOG);
        }
        // puerta y ventanas con una jardinera debajo
        StructKit.woodDoor(b, "mangrove", 0, 0, Z0, "+z", false);
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, Z0, "glass_pane");
            b.set(2, y, Z0, "glass_pane");
            b.set(-X, y, 3, "glass_pane");
            b.set(X, y, 6, "glass_pane");
        }
        b.set(0, 2, Z0, "stripped_mangrove_log[axis=x]");
        b.set(-2, 0, Z0 - 1, "potted_poppy");
        b.set(2, 0, Z0 - 1, "potted_red_tulip");
        // tejado rojizo de mangle (cumbrera de lado a lado)
        StructKit.gableX(b, -X, X, Z0, Z1, 4, 1, 1, "mangrove_stairs", "mangrove_planks", "mangrove_planks", true, 1);
        // pérgola de la puerta con azaleas en flor
        for (int sx : new int[]{-1, 1}) {
            b.set(sx, 0, Z0 - 2, "mangrove_fence");
            b.set(sx, 1, Z0 - 2, "mangrove_fence");
            b.set(sx, 2, Z0 - 2, "mangrove_fence");
        }
        for (int x = -1; x <= 1; x++) {
            b.set(x, 3, Z0 - 2, "flowering_azalea_leaves[persistent=true]");
            b.set(x, 3, Z0 - 1, x == 0 ? "flowering_azalea_leaves[persistent=true]" : "azalea_leaves[persistent=true]");
        }
        // jardines de flores a los dos lados
        for (int side : new int[]{-1, 1}) {
            int x0 = side * (X + 1), x1 = side * (X + 2);
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                for (int z = Z0; z <= Z1 - 1; z++) {
                    b.set(x, -1, z, "grass_block");
                    b.set(x, 0, z, FLOWERS[(int) (StructKit.hash01(2622, x, z) * FLOWERS.length)]);
                }
            }
            b.foundation(Math.min(x0, x1), Z0, Math.max(x0, x1), Z1 - 1, "dirt");
            // borde del jardín
            for (int z = Z0; z <= Z1 - 1; z++) {
                b.set(side * (X + 3), 0, z, "mud_brick_slab[type=bottom]");
                b.set(side * (X + 3), -1, z, "mud_bricks");
            }
            b.foundation(side * (X + 3), Z0, side * (X + 3), Z1 - 1, "mud_bricks");
        }
        // la fogata de cocinar, junto al jardín de la izquierda, con un tocón de asiento
        b.set(-(X + 2), 0, Z1 - 1, "campfire[lit=true,signal_fire=false,facing=east]");
        b.set(-(X + 2), -1, Z1 - 1, "mud_bricks");
        b.set(-(X + 1), 0, Z1 - 1, "stripped_spruce_log[axis=y]");
        // dentro: cama roja, mesa de trabajo, atril con su libro, macetas y la vieja máscara en un estante
        b.set(2, 0, 5, "red_bed[facing=south,part=foot]");
        b.set(2, 0, 6, "red_bed[facing=south,part=head]");
        b.set(1, 0, 6, "potted_azure_bluet");
        b.set(-2, 0, 6, "crafting_table");
        b.set(-2, 1, 6, "candle[candles=2,lit=true]");
        b.set(-2, 0, 5, "lectern[facing=east]");
        b.set(-2, 0, 2, "barrel[facing=up]");
        b.set(-2, 1, 2, "potted_cornflower");
        b.set(2, 0, 2, StructBp.stair("mangrove_stairs", "-z", false));
        // estante con la máscara de cultista (calavera de esqueleto marchito) entre flores
        b.set(0, 2, Z1 - 1, StructBp.slab("mangrove_slab", true));
        b.set(-1, 2, Z1 - 1, StructBp.slab("mangrove_slab", true));
        b.set(1, 2, Z1 - 1, StructBp.slab("mangrove_slab", true));
        b.set(0, 3, Z1 - 1, "wither_skeleton_skull[rotation=0]");
        b.set(-1, 3, Z1 - 1, "potted_poppy");
        b.set(1, 3, Z1 - 1, "potted_dandelion");
        for (int x = -1; x <= 1; x++) {
            for (int z = 3; z <= 4; z++) {
                b.set(x, 0, z, ((x + z) & 1) == 0 ? "red_carpet" : "orange_carpet");
            }
        }
        StructKit.hangLantern(b, 0, 6, 4, 2, "lantern");
        // camino
        for (int z = -1; z <= 0; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, x == 0 ? "dirt_path" : "packed_mud");
            }
        }
        b.foundation(-1, -1, 1, 0, "dirt");
        return b;
    }
}
