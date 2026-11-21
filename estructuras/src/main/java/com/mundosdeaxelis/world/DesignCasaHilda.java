package com.mundosdeaxelis.world;

/**
 * Herrería de Hilda (etapa 259): la casa de la herrera, que crece al plantar su Semilla de casa. Taller de piedra y ladrillo con
 * entramado de roble oscuro y tejado de pizarra; una chimenea alta de ladrillo con humo; un porche con la fragua (horno, fogata
 * de ascuas), una piedra de afilar y una pila de agua para templar; un armero con espadas de hierro... de mentira (pararrayos).
 * Dentro: su cama, la mesa de herrería, el horno, barriles con herramientas, cadenas y un farol.
 * Todo con bloques baratos (si se muda, la casa se desarma sola: nada de yunques ni altos hornos).
 * <p>Puerta en z = 1 (z = 0 es el camino); suelo en y = -1; la habitante se queda en (0, 0, 4).
 */
final class DesignCasaHilda {
    private DesignCasaHilda() {
    }

    static final int X = 3, Z0 = 1, Z1 = 7;

    static StructBp make() {
        StructBp b = new StructBp(2593);
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                boolean wall = Math.abs(x) == X || z == Z0 || z == Z1;
                b.set(x, -1, z, wall ? "stone_bricks" : ((x + z) & 1) == 0 ? "polished_andesite" : "stone_bricks");
            }
        }
        b.foundation(-X, Z0, X, Z1, "cobblestone");
        // zócalo de piedra (y 0), ladrillo con entramado de roble oscuro (y 1..3)
        for (int y = 0; y <= 3; y++) {
            for (int x = -X; x <= X; x++) {
                for (int z = Z0; z <= Z1; z++) {
                    boolean sideX = Math.abs(x) == X, sideZ = z == Z0 || z == Z1;
                    if (!sideX && !sideZ) {
                        continue;
                    }
                    String s;
                    if (sideX && sideZ) {
                        s = "dark_oak_log[axis=y]";
                    } else if (y == 0) {
                        s = StructKit.hash01(2594, x, z) < 0.35 ? "mossy_cobblestone" : "cobblestone";
                    } else if (y == 3) {
                        s = sideX ? "stripped_dark_oak_log[axis=z]" : "stripped_dark_oak_log[axis=x]";
                    } else {
                        s = "bricks";
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        for (int y = 0; y <= 2; y++) {
            b.set(-X, y, 4, "dark_oak_log[axis=y]");
            b.set(X, y, 4, "dark_oak_log[axis=y]");
        }
        // puerta, ventanas con reja
        StructKit.woodDoor(b, "dark_oak", 0, 0, Z0, "+z", false);
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, Z0, "glass_pane");
            b.set(2, y, Z0, "glass_pane");
            b.set(-X, y, 3, "iron_bars");
            b.set(-X, y, 5, "iron_bars");
            b.set(0, y, Z1, "glass_pane");
        }
        b.set(0, 2, Z0, "stripped_dark_oak_log[axis=x]");
        // tejado de pizarra a dos aguas (cumbrera de frente)
        StructKit.gableZ(b, -X, X, Z0, Z1, 4, 1, 1, "deepslate_tile_stairs", "dark_oak_planks", "deepslate_tiles", true, 1);
        b.set(0, 5, Z0, "iron_bars");
        // chimenea de ladrillo alta, con humo
        for (int y = -1; y <= 8; y++) {
            b.set(X + 1, y, 6, y < 1 ? "cobblestone" : "bricks");
        }
        b.set(X + 1, 9, 6, "campfire[lit=true,signal_fire=false,facing=south]");
        b.foundation(X + 1, 6, X + 1, 6, "cobblestone");
        // dentro
        b.set(-2, 0, 5, "orange_bed[facing=south,part=foot]");
        b.set(-2, 0, 6, "orange_bed[facing=south,part=head]");
        b.set(-1, 0, 6, "barrel[facing=up]");
        b.set(-1, 1, 6, "lantern");
        b.set(2, 0, 6, "furnace[facing=west,lit=true]");
        b.set(2, 1, 6, "bricks");
        b.set(2, 0, 5, "smithing_table");
        b.set(2, 0, 2, "barrel[facing=up]");
        b.set(2, 1, 2, "barrel[facing=up]");
        b.set(-2, 0, 2, "chest[facing=east]");
        for (int x = -1; x <= 1; x++) {
            for (int z = 3; z <= 4; z++) {
                b.set(x, 0, z, "brown_carpet");
            }
        }
        StructKit.hangLantern(b, 0, 6, 4, 2, "lantern");
        // porche lateral con la fragua: fogata de ascuas en un hogar de piedra, piedra de afilar y pila de agua
        for (int z = 2; z <= 5; z++) {
            b.set(X + 1, -1, z, "cobblestone");
            b.set(X + 2, -1, z, "cobblestone");
        }
        b.foundation(X + 1, 2, X + 2, 5, "cobblestone");
        b.set(X + 2, 0, 2, "dark_oak_fence");
        b.set(X + 2, 1, 2, "dark_oak_fence");
        b.set(X + 2, 2, 2, "dark_oak_fence");
        b.set(X + 2, 0, 5, "dark_oak_fence");
        b.set(X + 2, 1, 5, "dark_oak_fence");
        b.set(X + 2, 2, 5, "dark_oak_fence");
        for (int z = 1; z <= 5; z++) {
            b.set(X + 1, 3, z, StructBp.stair("deepslate_tile_stairs", "-x", false));
            b.set(X + 2, 3, z, "deepslate_tile_slab[type=bottom]");
        }
        b.set(X + 1, 0, 3, "cobblestone");
        b.set(X + 1, 1, 3, "campfire[lit=true,signal_fire=false,facing=east]");
        b.set(X + 1, 0, 4, "grindstone[face=floor,facing=north]");
        b.set(X + 2, 0, 3, "water_cauldron[level=3]");
        // armero junto a la puerta: «espadas» de cobre en un poste
        b.set(-2, 0, Z0 - 1, "dark_oak_fence");
        b.set(-2, 1, Z0 - 1, "lightning_rod[facing=up]");
        // camino y yunque de piedra (decorativo) delante
        for (int z = -1; z <= 0; z++) {
            for (int x = -1; x <= 2; x++) {
                b.set(x, -1, z, x == 0 ? "dirt_path" : "gravel");
            }
        }
        b.set(-2, -1, -1, "gravel");
        b.foundation(-2, -1, 2, 0, "dirt");
        b.set(1, 0, -1, "polished_andesite");
        b.set(1, 1, -1, "andesite_slab[type=bottom]");
        return b;
    }
}
