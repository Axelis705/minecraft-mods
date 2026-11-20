package com.mundosdeaxelis.world;

/**
 * Casa de Ruda, la herbolaria (etapa 257): la que crece al plantar su Semilla de casa. Una cabaña pequeña de 7 × 7 con zócalo de
 * piedra con musgo, muros de adobe con marco de roble oscuro, tejado a dos aguas con chimenea humeante, un camino con dos
 * faroles, flores delante y un huerto de hierbas a un lado. Dentro: su cama, un barril con helechos, la mesa de trabajo, la
 * compostera, vigas con raíces secándose y un farol colgado.
 * <p>Puerta en z = 1 mirando al frente (z = 0 es el camino); suelo en y = -1. Todo con bloques baratos: si Ruda se muda, la casa
 * se desarma sola (ver {@code NpcService}).
 */
final class DesignCasaRuda {
    private DesignCasaRuda() {
    }

    static final int X = 3, Z0 = 1, Z1 = 7;
    /** Donde se queda Ruda dentro de la casa. */
    static final int SPOT_X = 0, SPOT_Z = 4;

    private static final String POST = "dark_oak_log[axis=y]";
    private static final String BEAM_X = "stripped_dark_oak_log[axis=x]";
    private static final String BEAM_Z = "stripped_dark_oak_log[axis=z]";
    private static final String INFILL = "mud_bricks";

    static StructBp make() {
        StructBp b = new StructBp(2571);
        floor(b);
        walls(b);
        roof(b);
        interior(b);
        outside(b);
        return b;
    }

    private static boolean wallCol(int x, int z) {
        return (Math.abs(x) == X && z >= Z0 && z <= Z1) || ((z == Z0 || z == Z1) && Math.abs(x) <= X);
    }

    private static void floor(StructBp b) {
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                b.set(x, -1, z, wallCol(x, z) ? (StructKit.hash01(2572, x, z) < 0.35 ? "mossy_cobblestone" : "cobblestone") : "spruce_planks");
            }
        }
        b.foundation(-X, Z0, X, Z1, "cobblestone");
    }

    private static void walls(StructBp b) {
        for (int x = -X; x <= X; x++) {
            for (int z = Z0; z <= Z1; z++) {
                if (!wallCol(x, z)) {
                    continue;
                }
                boolean corner = Math.abs(x) == X && (z == Z0 || z == Z1);
                for (int y = 0; y <= 3; y++) {
                    String s;
                    if (corner) {
                        s = POST;
                    } else if (y == 0) {
                        s = StructKit.hash01(2573, x * 7 + y, z) < 0.4 ? "mossy_cobblestone" : "cobblestone";
                    } else if (y == 3) {
                        s = Math.abs(x) == X ? BEAM_Z : BEAM_X;
                    } else {
                        s = INFILL;
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        // postes a media pared en los lados largos
        for (int y = 0; y <= 2; y++) {
            b.set(-X, y, 4, POST);
            b.set(X, y, 4, POST);
        }
        // puerta y ventanas
        StructKit.woodDoor(b, "spruce", 0, 0, Z0, "+z", false);
        for (int y = 1; y <= 2; y++) {
            b.set(-2, y, Z0, "glass_pane");
            b.set(2, y, Z0, "glass_pane");
            b.set(-X, y, 3, "glass_pane");
            b.set(X, y, 3, "glass_pane");
            b.set(-X, y, 5, "glass_pane");
            b.set(X, y, 5, "glass_pane");
            b.set(0, y, Z1, "glass_pane");
        }
        // dintel sobre la puerta
        b.set(0, 2, Z0, BEAM_X);
    }

    private static void roof(StructBp b) {
        StructKit.gableZ(b, -X, X, Z0, Z1, 4, 1, 1, "spruce_stairs", "spruce_planks", "dark_oak_planks", true, 1);
        // ventanita redonda en los hastiales
        b.set(0, 5, Z0, "glass_pane");
        b.set(0, 5, Z1, "glass_pane");
        // musgo y hojas creciendo en la cumbrera
        for (int z = Z0 - 1; z <= Z1 + 1; z++) {
            if (StructKit.hash01(2574, 0, z) < 0.45) {
                b.set(0, 9, z, "moss_carpet");
            }
        }
        b.set(-4, 4, Z1 + 1, "flowering_azalea_leaves[persistent=true]");
        b.set(-4, 4, Z1, "azalea_leaves[persistent=true]");
        b.set(-4, 3, Z1 + 1, "azalea_leaves[persistent=true]");
        // chimenea de piedra por fuera del lado derecho, con humo
        for (int y = -1; y <= 7; y++) {
            b.set(X + 1, y, 6, y < 1 || StructKit.hash01(2575, y, 6) < 0.3 ? "mossy_cobblestone" : "cobblestone");
        }
        b.set(X + 1, 8, 6, "campfire[lit=true,signal_fire=false,facing=south]");
        b.foundation(X + 1, 6, X + 1, 6, "cobblestone");
    }

    private static void interior(StructBp b) {
        // alfombra verde en el centro
        for (int x = -1; x <= 1; x++) {
            for (int z = 3; z <= 4; z++) {
                b.set(x, 0, z, "green_carpet");
            }
        }
        // cama al fondo a la izquierda y mesita con velas
        b.set(-2, 0, 5, "green_bed[facing=south,part=foot]");
        b.set(-2, 0, 6, "green_bed[facing=south,part=head]");
        b.set(-1, 0, 6, "barrel[facing=up]");
        b.set(-1, 1, 6, "candle[candles=3,lit=true]");
        // mesa de trabajo, compostera y barril con plantas a la derecha
        b.set(1, 0, 6, "crafting_table");
        b.set(2, 0, 6, "composter");
        b.set(2, 0, 2, "barrel[facing=up]");
        b.set(2, 1, 2, "potted_fern");
        b.set(-2, 0, 2, "potted_azalea_bush");
        b.set(2, 0, 4, StructBp.stair("spruce_stairs", "+x", false));
        // vigas con raíces secándose y un farol en el centro
        for (int x = -2; x <= 2; x++) {
            b.set(x, 3, 3, BEAM_X);
            b.set(x, 3, 5, BEAM_X);
        }
        b.set(-1, 2, 3, "cave_vines[age=25,berries=true]");
        b.set(1, 2, 5, "cave_vines[age=25,berries=true]");
        b.set(-2, 2, 5, "cave_vines[age=25,berries=true]");
        b.set(0, 2, 5, "lantern[hanging=true]");
        StructKit.hangLantern(b, 0, 6, 4, 2, "lantern");
    }

    private static void outside(StructBp b) {
        // camino de tierra con dos faroles en postes
        for (int z = -1; z <= 0; z++) {
            for (int x = -2; x <= 2; x++) {
                b.set(x, -1, z, x == 0 ? "dirt_path" : "grass_block");
            }
        }
        b.set(-1, 0, -1, "spruce_fence");
        b.set(-1, 1, -1, "lantern");
        b.set(1, 0, -1, "spruce_fence");
        b.set(1, 1, -1, "lantern");
        // flores bajo las ventanas del frente
        b.set(-2, 0, 0, "flowering_azalea");
        b.set(2, 0, 0, "flowering_azalea");
        b.set(-1, 0, 0, "allium");
        b.set(1, 0, 0, "lily_of_the_valley");
        b.foundation(-2, -1, 2, 0, "dirt");
        // huerto de hierbas por el lado izquierdo
        String[] herbs = {"allium", "fern", "azure_bluet", "lily_of_the_valley", "oxeye_daisy"};
        for (int z = 2; z <= 6; z++) {
            b.set(-X - 1, -1, z, z % 2 == 0 ? "moss_block" : "rooted_dirt");
            b.set(-X - 1, 0, z, herbs[z - 2]);
        }
        b.foundation(-X - 1, 2, -X - 1, 6, "dirt");
        // enredaderas en el frente y el costado
        b.set(-1, 2, Z0 - 1, "vine[south=true]");
        b.set(-1, 3, Z0 - 1, "vine[south=true]");
        b.set(X + 1, 3, 2, "vine[west=true]");
        b.set(X + 1, 2, 2, "vine[west=true]");
        b.set(-X - 1, 3, 7, "vine[east=true]");
    }
}
