package com.mundosdeaxelis.world;

/**
 * Cabaña del cartógrafo (etapa 258): la casa de Bruno, que crece al plantar su Semilla de casa. Cabaña de troncos de abeto con
 * las esquinas entrelazadas, zócalo de piedra, tejado a dos aguas con la cumbrera de lado a lado, chimenea de piedra, un catalejo
 * junto a la ventana, banderín azul y leña apilada. Dentro: su cama, la mesa de cartografía, el atril, estanterías, un barril y
 * una alfombra roja. Todo con bloques baratos (si se muda, la casa se desarma sola).
 * <p>Puerta en z = 1 (z = 0 es el camino); suelo en y = -1; el habitante se queda en (0, 0, 4).
 */
final class DesignCasaBruno {
    private DesignCasaBruno() {
    }

    static final int X = 3, Z0 = 1, Z1 = 7;

    static StructBp make() {
        StructBp b = new StructBp(2582);
        // suelo: piedra bajo los muros, tablones dentro
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean wall = Math.abs(x) == X || z == Z0 || z == Z1;
                b.set(x, -1, z, wall ? (StructKit.hash01(2583, x, z) < 0.3 ? "mossy_cobblestone" : "cobblestone") : "spruce_planks");
            }
        }
        b.foundation(-X, Z0, X, Z1, "cobblestone");
        // muros de troncos (las esquinas se cruzan: un piso de lado, otro de frente)
        for (int y = 0; y <= 3; y++) {
            for (int x = -X; x <= X; x++) {
                for (int z = Z0; z <= Z1; z++) {
                    boolean sideX = Math.abs(x) == X, sideZ = z == Z0 || z == Z1;
                    if (!sideX && !sideZ) {
                        continue;
                    }
                    String s;
                    if (sideX && sideZ) {
                        s = (y & 1) == 0 ? "spruce_log[axis=x]" : "spruce_log[axis=z]";
                    } else if (sideZ) {
                        s = y == 0 ? "stripped_spruce_log[axis=x]" : "spruce_log[axis=x]";
                    } else {
                        s = y == 0 ? "stripped_spruce_log[axis=z]" : "spruce_log[axis=z]";
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        // puerta, ventanas y contraventanas
        StructKit.woodDoor(b, "spruce", 0, 0, Z0, "+z", false);
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, Z0, "glass_pane");
            b.set(2, y, Z0, "glass_pane");
            b.set(-X, y, 4, "glass_pane");
            b.set(X, y, 4, "glass_pane");
            b.set(0, y, Z1, "glass_pane");
        }
        b.set(-1, 2, Z0 - 1, "light_blue_wall_banner[facing=north]");
        // tejado con la cumbrera de lado a lado; hastiales de tablones con un ventanuco
        StructKit.gableX(b, -X, X, Z0, Z1, 4, 1, 1, "dark_oak_stairs", "spruce_planks", "dark_oak_planks", true, 1);
        b.set(-X, 5, 4, "glass_pane");
        b.set(X, 5, 4, "glass_pane");
        // chimenea de piedra detrás, con humo
        for (int y = -1; y <= 8; y++) {
            b.set(2, y, Z1 + 1, y < 1 || StructKit.hash01(2584, y, 0) < 0.35 ? "mossy_cobblestone" : "stone_bricks");
        }
        b.set(2, 9, Z1 + 1, "campfire[lit=true,signal_fire=false,facing=south]");
        b.foundation(2, Z1 + 1, 2, Z1 + 1, "cobblestone");
        // dentro
        b.set(-2, 0, 5, "red_bed[facing=south,part=foot]");
        b.set(-2, 0, 6, "red_bed[facing=south,part=head]");
        b.set(-1, 0, 6, "barrel[facing=up]");
        b.set(-1, 1, 6, "lantern");
        b.set(2, 0, 2, "cartography_table");
        b.set(2, 0, 4, "lectern[facing=west,has_book=false]");
        b.set(2, 0, 6, "chiseled_bookshelf[facing=west]");
        b.set(2, 1, 6, "chiseled_bookshelf[facing=west]");
        b.set(1, 0, 6, "chiseled_bookshelf[facing=north]");
        b.set(-2, 0, 2, "barrel[facing=up]");
        b.set(-2, 1, 2, "potted_spruce_sapling");
        for (int x = -1; x <= 1; x++) {
            for (int z = 3; z <= 4; z++) {
                b.set(x, 0, z, "red_carpet");
            }
        }
        // catalejo junto a la ventana del frente (un pararrayos de cobre sobre un trípode)
        b.set(-2, 0, 3, "spruce_fence");
        b.set(-2, 1, 3, "lightning_rod[facing=north]");
        StructKit.hangLantern(b, 0, 6, 4, 2, "lantern");
        // fuera: camino, leña apilada, un tocón con hacha... y un poste indicador
        for (int z = -1; z <= 0; z++) {
            for (int x = -2; x <= 2; x++) {
                b.set(x, -1, z, x == 0 ? "dirt_path" : "grass_block");
            }
        }
        b.foundation(-2, -1, 2, 0, "dirt");
        for (int z = 2; z <= 5; z++) {
            b.set(X + 1, -1, z, "coarse_dirt");
            b.set(X + 1, 0, z, "spruce_log[axis=z]");
            if (z <= 4) {
                b.set(X + 1, 1, z, "spruce_log[axis=z]");
            }
        }
        b.foundation(X + 1, 2, X + 1, 5, "dirt");
        b.set(-X - 1, -1, 2, "grass_block");
        b.set(-X - 1, 0, 2, "stripped_spruce_log[axis=y]");
        b.set(-X - 1, -1, 5, "grass_block");
        b.set(-X - 1, 0, 5, "spruce_fence");
        b.set(-X - 1, 1, 5, "spruce_fence");
        b.set(-X - 1, 2, 5, "spruce_sign[rotation=4]");
        b.foundation(-X - 1, 2, -X - 1, 5, "dirt");
        return b;
    }
}
