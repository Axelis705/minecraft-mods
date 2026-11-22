package com.mundosdeaxelis.world;

/**
 * Fragua Olvidada (etapa 259): la estructura de la forja, de tamaño mediano (entre el santuario y el fortín). El viejo taller del
 * Gran Herrero: un patio de trabajo con muro bajo de piedra negra, montones de mineral y carbón, una vía de vagonetas, una pila
 * para templar y un yunque al aire libre; al fondo, una nave de ladrillo y piedra negra con tejado bajo de pizarra y dos grandes
 * chimeneas humeantes. Dentro: el gran horno (altos hornos y un crisol de lava), un canal de metal fundido por el suelo, yunques,
 * piedras de afilar, un martillo gigante colgado y, sobre un altar con forma de yunque, el tesoro del Gran Herrero (un cofre con
 * cerradura difícil) con un cofre de provisiones al lado. Pegado a la nave, el almacén (era la celda de Hilda).
 * <p>Etapa 266: es la estructura chica del tier de la forja, la del botín (la Piedra de Portal: Forja y Hilda están en la Gran
 * Fundición). Enemigos: Escorias y un Fundidor en el patio, un Chispero en un balcón y, delante del altar, el minijefe: el Coloso de
 * Hierro (etapa 267), que duerme de pie hasta que alguien se acerca.
 * Entrada por z = 0; el patio ocupa x -14..14, z 8..20; la nave x -10..10, z 21..38.
 */
final class DesignFragua {
    private DesignFragua() {
    }

    private static final String M = "mundos_de_axelis:";
    private static final String PB = "polished_blackstone_bricks";
    private static final String CPB = "cracked_polished_blackstone_bricks";
    private static final String BS = "blackstone";
    private static final String PBL = "polished_blackstone";
    private static final String BR = "bricks";
    private static final String CU = "waxed_cut_copper";
    private static final String LAVA = M + "lava_falsa";
    /** Patio y nave. */
    static final int YX = 14, YZ0 = 8, YZ1 = 20;
    static final int HX = 10, HZ0 = 21, HZ1 = 38;
    /** Celda (al este de la nave) y sitio de la presa. */
    static final int CX1 = 15, CZ0 = 24, CZ1 = 30;
    static final int NPC_X = 13, NPC_Z = 27;

    static StructBp make() {
        StructBp b = new StructBp(2591);
        ground(b);
        yardWall(b);
        yard(b);
        hall(b);
        chimneys(b);
        interior(b);
        cell(b);
        surroundings(b);
        chests(b);
        mobs(b);
        return b;
    }

    private static String mix(long seed, int x, int y, int z) {
        double h = StructKit.hash01(seed + y, x, z);
        return h < 0.6 ? PB : h < 0.8 ? CPB : h < 0.93 ? BS : "gilded_blackstone";
    }

    // ================================================================== suelo y patio

    private static void ground(StructBp b) {
        for (int x = -YX; x <= YX; x++) {
            for (int z = YZ0; z <= HZ1 + 2; z++) {
                double n = StructKit.fbm(2592, x, z, 4.5) + (StructKit.hash01(2593, x, z) - 0.5) * 0.15;
                String s = n < 0.25 ? "coarse_dirt" : n < 0.55 ? "gravel" : n < 0.8 ? BS : n < 0.9 ? "tuff" : "basalt[axis=y]";
                b.set(x, -1, z, s);
                b.set(x, -2, z, "stone");
            }
        }
        // camino empedrado de la entrada a la nave
        for (int z = YZ0; z < HZ0; z++) {
            for (int x = -2; x <= 2; x++) {
                b.set(x, -1, z, Math.abs(x) == 2 ? "polished_blackstone_bricks" : StructKit.hash01(2594, x, z) < 0.3 ? "cobblestone" : PBL);
            }
        }
    }

    private static void yardWall(StructBp b) {
        for (int x = -YX; x <= YX; x++) {
            for (int z = YZ0; z <= HZ1 + 2; z++) {
                boolean edge = Math.abs(x) == YX || z == YZ0 || z == HZ1 + 2;
                if (!edge || (z == YZ0 && Math.abs(x) <= 2)) {
                    continue;
                }
                // muro bajo en ruinas (1 a 3) con una verja de hierro en los tramos bajos
                double n = StructKit.vnoise(2595, x * 0.3, z * 0.3);
                int h = 1 + (int) Math.round(n * 2.2);
                for (int y = 0; y < h; y++) {
                    b.set(x, y, z, mix(2596, x, y, z));
                }
                if (h < 3 && StructKit.hash01(2597, x, z) < 0.6) {
                    b.set(x, h, z, "iron_bars");
                }
            }
        }
        // pilares de la entrada con braseros
        for (int sx : new int[]{-3, 3}) {
            for (int y = 0; y <= 3; y++) {
                b.set(sx, y, YZ0, y == 3 ? "chiseled_polished_blackstone" : BR);
            }
            b.set(sx, 4, YZ0, "campfire[lit=true,signal_fire=false,facing=north]");
        }
        // un letrero de hierro sobre la entrada (cadenas y una viga)
        for (int x = -2; x <= 2; x++) {
            b.set(x, 4, YZ0, Math.abs(x) == 2 ? "chain[axis=y]" : "air");
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, 5, YZ0, x == 0 ? "chiseled_polished_blackstone" : "polished_blackstone_brick_slab[type=bottom]");
        }
    }

    private static void yard(StructBp b) {
        // montones de mineral y de carbón
        int[][] ore = {{-11, 11}, {-10, 11}, {-11, 12}, {-12, 12}, {-11, 13}};
        for (int[] o : ore) {
            b.set(o[0], 0, o[1], "raw_iron_block");
        }
        b.set(-11, 1, 12, "raw_iron_block");
        b.set(-10, 0, 12, "raw_copper_block");
        int[][] coal = {{-12, 16}, {-11, 16}, {-12, 17}, {-11, 17}, {-10, 17}};
        for (int[] o : coal) {
            b.set(o[0], 0, o[1], "coal_block");
        }
        b.set(-12, 1, 17, "coal_block");
        // vía de vagonetas que acaba en un tope
        for (int x = 4; x <= 12; x++) {
            b.set(x, 0, 11, "rail[shape=east_west]");
        }
        b.set(13, 0, 11, "barrel[facing=west]");
        // pila de agua para templar
        for (int x = 6; x <= 9; x++) {
            for (int z = 15; z <= 17; z++) {
                boolean rim = x == 6 || x == 9 || z == 15 || z == 17;
                b.set(x, -1, z, rim ? PB : "water");
                b.set(x, -2, z, PB);
                if (rim) {
                    b.set(x, 0, z, "polished_blackstone_brick_slab[type=bottom]");
                }
            }
        }
        // yunque al aire libre con su tocón y una piedra de afilar
        b.set(-5, 0, 15, "stripped_dark_oak_log[axis=y]");
        b.set(-5, 1, 15, "anvil[facing=east]");
        b.set(-6, 0, 18, "grindstone[face=floor,facing=east]");
        b.set(-4, 0, 18, "smithing_table");
        // farolas
        for (int z : new int[]{12, 18}) {
            for (int sx : new int[]{-3, 3}) {
                b.set(sx, 0, z, "polished_blackstone_wall");
                b.set(sx, 1, z, "polished_blackstone_wall");
                b.set(sx, 2, z, "lantern");
            }
        }
    }

    // ================================================================== la nave

    private static void hall(StructBp b) {
        int top = 6;
        for (int x = -HX; x <= HX; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                b.set(x, -1, z, ((x + z) & 1) == 0 ? PBL : "polished_blackstone_bricks");
                b.set(x, -2, z, "stone");
                boolean edge = Math.abs(x) == HX || z == HZ0 || z == HZ1;
                for (int y = 0; y <= top; y++) {
                    b.set(x, y, z, edge ? (y <= 1 ? mix(2600, x, y, z) : y == top ? PB : BR) : "air");
                }
            }
        }
        // pilares de piedra negra y vigas de cobre
        for (int x : new int[]{-HX, -5, 0, 5, HX}) {
            for (int z : new int[]{HZ0, HZ1}) {
                for (int y = 0; y <= top; y++) {
                    b.set(x, y, z, x == 0 && z == HZ0 ? b.get(x, y, z) : "polished_basalt[axis=y]");
                }
            }
        }
        for (int z : new int[]{HZ0, 27, 33, HZ1}) {
            for (int x : new int[]{-HX, HX}) {
                for (int y = 0; y <= top; y++) {
                    b.set(x, y, z, "polished_basalt[axis=y]");
                }
            }
        }
        for (int z = HZ0; z <= HZ1; z++) {
            b.set(-HX, top, z, CU);
            b.set(HX, top, z, CU);
        }
        // portón ancho con dintel de cobre
        for (int x = -2; x <= 2; x++) {
            for (int y = 0; y <= 3; y++) {
                b.set(x, y, HZ0, "air");
            }
            b.set(x, 4, HZ0, CU);
        }
        b.set(-3, 4, HZ0, "polished_basalt[axis=y]");
        b.set(3, 4, HZ0, "polished_basalt[axis=y]");
        // ventanas altas con reja (al este solo detrás de la celda)
        for (int y = 3; y <= 4; y++) {
            for (int z : new int[]{24, 30, 36}) {
                b.set(-HX, y, z, "iron_bars");
            }
            b.set(HX, y, 35, "iron_bars");
            for (int x : new int[]{-7, 7}) {
                b.set(x, y, HZ0, "iron_bars");
            }
        }
        roof(b, top + 1);
    }

    /** Tejado bajo de pizarra (media pendiente: escalón y losa alternados) con la cumbrera a lo largo de la nave. */
    private static void roof(StructBp b, int y0) {
        int half = HX + 1;
        for (int z = HZ0 - 1; z <= HZ1 + 1; z++) {
            boolean endZ = z == HZ0 - 1 || z == HZ1 + 1;
            for (int i = 0; i <= half; i++) {
                int y = y0 + i / 2;
                if (i == half) {
                    b.set(0, y, z, "polished_deepslate");
                    b.set(0, y + 1, z, "polished_deepslate_slab[type=bottom]");
                    continue;
                }
                for (int side : new int[]{-1, 1}) {
                    int x = side * (half - i);
                    String up = side < 0 ? "+x" : "-x";
                    String st = i % 2 == 0 ? StructBp.stair("deepslate_tile_stairs", up, false) : "deepslate_tile_slab[type=top]";
                    if (endZ && i % 2 == 0) {
                        st = StructBp.stair("polished_blackstone_brick_stairs", up, false);
                    }
                    b.set(x, y, z, st);
                    // tablones bajo el tejado (techo de la nave)
                    if (i % 2 == 1 && !endZ) {
                        b.set(x, y - 1, z, b.get(x, y - 1, z) == null ? "dark_oak_planks" : b.get(x, y - 1, z));
                    }
                }
            }
        }
        // hastiales de ladrillo
        for (int z : new int[]{HZ0, HZ1}) {
            for (int i = 1; i < half; i++) {
                int yTop = y0 + i / 2;
                for (int x : new int[]{-(half - i), half - i}) {
                    for (int y = y0; y < yTop + (i % 2 == 1 ? 1 : 0); y++) {
                        if (Math.abs(x) <= HX) {
                            b.set(x, y, z, BR);
                        }
                    }
                }
            }
            for (int y = y0; y <= y0 + half / 2; y++) {
                b.set(0, y, z, BR);
            }
            b.set(0, y0 + 2, z, "chiseled_polished_blackstone");
        }
    }

    /** Dos grandes chimeneas de ladrillo en las esquinas de atrás, que sacan humo. */
    private static void chimneys(StructBp b) {
        for (int sx : new int[]{-1, 1}) {
            int cx = sx * 7, cz = HZ1 - 2;
            for (int x = cx - 1; x <= cx + 1; x++) {
                for (int z = cz - 1; z <= cz + 1; z++) {
                    for (int y = 0; y <= 19; y++) {
                        boolean core = x == cx && z == cz;
                        b.set(x, y, z, core ? (y == 19 ? "campfire[lit=true,signal_fire=true,facing=north]" : BR)
                                : (y == 13 || y == 18 ? PB : StructKit.hash01(2601 + y, x, z) < 0.12 ? "mossy_stone_bricks" : BR));
                    }
                }
            }
            // corona
            for (int x = cx - 2; x <= cx + 2; x++) {
                for (int z = cz - 2; z <= cz + 2; z++) {
                    if (Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2) {
                        b.set(x, 19, z, "polished_blackstone_brick_slab[type=bottom]");
                    }
                }
            }
        }
    }

    private static void interior(StructBp b) {
        // el gran horno al fondo: altos hornos encendidos y el crisol de lava en medio
        for (int x = -5; x <= 5; x++) {
            if (Math.abs(x) <= 1) {
                continue;
            }
            b.set(x, 0, HZ1 - 1, Math.abs(x) % 2 == 0 ? "blast_furnace[facing=north,lit=true]" : PB);
            b.set(x, 1, HZ1 - 1, Math.abs(x) % 2 == 0 ? "blast_furnace[facing=north,lit=true]" : PB);
            b.set(x, 2, HZ1 - 1, PB);
            b.set(x, 3, HZ1 - 1, StructBp.stair("polished_blackstone_brick_stairs", "+z", false));
        }
        // altar con forma de yunque y la Piedra de Portal
        for (int x = -2; x <= 2; x++) {
            for (int z = 33; z <= 36; z++) {
                b.set(x, 0, z, z == 33 ? StructBp.stair("polished_blackstone_brick_stairs", "+z", false) : PB);
            }
        }
        b.setNbt(0, 1, 35, M + "vault_chest_locked_forge[facing=north]", "{Kind:\"LOCKED\",Stage:2,LockLevel:4}");
        b.set(-1, 1, 35, "chiseled_polished_blackstone");
        b.set(1, 1, 35, "chiseled_polished_blackstone");
        b.set(-2, 1, 36, "lava_cauldron");
        b.set(2, 1, 36, "lava_cauldron");
        b.set(0, 1, 36, "anvil[facing=east]");
        // canal de metal fundido desde el horno, por el suelo, hasta los moldes
        for (int z = 25; z <= 31; z++) {
            b.set(-7, -1, z, z == 25 ? LAVA + "[flujo=east]" : LAVA + "[flujo=north]");
            b.set(-7, -2, z, "magma_block");
            b.set(-8, -1, z, PB);
            b.set(-6, -1, z, PB);
        }
        for (int x = -7; x <= -4; x++) {
            b.set(x, -1, 25, x == -4 ? PB : LAVA + "[flujo=east]");
            b.set(x, -2, 25, "magma_block");
        }
        // martillo gigante colgado de cadenas sobre el centro
        // etapa 282: el mango va arriba, hacia las cadenas (antes colgaba un palo hasta media sala)
        b.set(0, 9, 28, "dark_oak_log[axis=y]");
        b.set(0, 10, 28, "dark_oak_log[axis=y]");
        b.set(0, 11, 28, "chain[axis=y]");
        for (int x = -2; x <= 2; x++) {
            b.set(x, 8, 28, Math.abs(x) == 2 ? CU : "iron_block");
            b.set(x, 7, 28, Math.abs(x) <= 1 ? "iron_block" : "air");
        }
        // yunques, piedras de afilar y armeros
        b.set(-8, 0, 32, "anvil[facing=south]");
        b.set(-8, 0, 34, "grindstone[face=floor,facing=east]");
        b.set(8, 0, 32, "smithing_table");
        b.set(8, 0, 34, "anvil[facing=south]");
        for (int z = 22; z <= 23; z++) {
            b.set(-9, 0, z, "barrel[facing=up]");
            b.set(-9, 1, z, "barrel[facing=up]");
        }
        b.set(-8, 0, 22, "lava_cauldron");
        // balcón del chispero (repisa de piedra negra a la izquierda) y faroles
        for (int z = 29; z <= 31; z++) {
            b.set(-9, 2, z, PB);
            b.set(-8, 2, z, StructBp.slab("polished_blackstone_brick_slab", true));
        }
        for (int z : new int[]{24, 30, 36}) {
            StructKit.hangLantern(b, -4, 5, z, 1, "lantern");
            StructKit.hangLantern(b, 4, 5, z, 1, "lantern");
        }
    }

    // ================================================================== la celda

    private static void cell(StructBp b) {
        int x0 = HX;
        for (int x = x0 + 1; x <= CX1; x++) {
            for (int z = CZ0; z <= CZ1; z++) {
                b.set(x, -1, z, StructKit.hash01(2602, x, z) < 0.4 ? CPB : PB);
                b.set(x, -2, z, "stone");
                for (int y = 0; y <= 4; y++) {
                    boolean edge = x == CX1 || z == CZ0 || z == CZ1;
                    b.set(x, y, z, y == 4 || edge ? mix(2603, x, y, z) : "air");
                }
                b.set(x, 5, z, StructBp.slab("polished_blackstone_brick_slab", false));
            }
        }
        for (int z = CZ0 + 1; z <= CZ0 + 3; z++) {
            for (int y = 0; y <= 2; y++) {
                b.set(x0, y, z, "iron_bars");
            }
        }
        StructKit.woodDoor(b, "spruce", x0, 0, CZ1 - 1, "-x", false);
        b.set(x0, 2, CZ1 - 1, PB);
        // dentro (etapa 266, el almacén): barriles de carbón y mineral, un cubo de agua, cadenas
        b.set(CX1 - 1, 0, CZ0 + 1, "barrel[facing=up]");
        b.set(CX1 - 1, 0, CZ0 + 2, "barrel[facing=west]");
        b.set(CX1 - 1, 1, CZ0 + 1, "barrel[facing=up]");
        b.set(CX1 - 1, 0, CZ1 - 1, "water_cauldron[level=1]");
        b.set(x0 + 1, 0, CZ0 + 1, "candle[candles=1,lit=true]");
        StructKit.hangLantern(b, NPC_X, 3, NPC_Z, 1, "lantern");
        b.set(CX1 - 1, 3, CZ1 - 1, "cobweb");
        b.set(CX1 - 1, 3, CZ0 + 1, "chain[axis=y]");
    }

    // ================================================================== alrededor

    static double edge(int x, int z) {
        double dx = Math.max(0, Math.abs(x) - (YX + 2));
        double dz = Math.max(Math.max(0, (YZ0 - 3) - z), Math.max(0, z - (HZ1 + 5)));
        double out = Math.hypot(dx, dz);
        if (out > 0) {
            return out;
        }
        return -Math.min(Math.min((YX + 2) - Math.abs(x), z - (YZ0 - 3)), (HZ1 + 5) - z);
    }

    private static void surroundings(StructBp b) {
        int R = 34;
        for (int x = -YX - R; x <= YX + R; x++) {
            for (int z = YZ0 - R; z <= HZ1 + R; z++) {
                double e = edge(x, z);
                double ramp = 7 + 3 * StructKit.vnoise(2604, x * 0.1, z * 0.1);
                double w = e <= 2 ? 0 : Math.min(1, (e - 2) / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 1 && z >= -6 && z < YZ0;
                if (w >= 0.999 && !road) {
                    continue;
                }
                String top = road ? (StructKit.hash01(2605, x, z) < 0.35 ? "cobblestone" : "gravel") : null;
                b.blend(x, z, road ? Math.min(w, 0.5) : w, top, null);
            }
        }
        // vagoneta volcada y escoria junto al camino (sobre el suelo real)
        StructBp.Anchor slag = b.anchor(-6, 1);
        slag.set(0, 0, 0, "blackstone");
        slag.set(1, 0, 0, "basalt[axis=y]");
        slag.set(0, 0, 1, "magma_block");
        slag.set(0, 1, 0, "blackstone_slab[type=bottom]");
        StructBp.Anchor ore = b.anchor(6, -2);
        ore.set(0, 0, 0, "raw_iron_block");
        ore.set(1, 0, 0, "cobblestone");
        for (int sx : new int[]{-3, 3}) {
            StructBp.Anchor p = b.anchor(sx, -4);
            p.set(0, 0, 0, "polished_blackstone_wall");
            p.set(0, 1, 0, "polished_blackstone_wall");
            p.set(0, 2, 0, "lantern");
        }
    }

    // ================================================================== cofre y enemigos

    private static void chests(StructBp b) {
        b.setNbt(3, 0, 35, M + "vault_chest_forge[facing=east]", "{Kind:\"ANTE\",Stage:2}");
        // premio del almacén
        b.setNbt(NPC_X, 0, NPC_Z, M + "vault_chest_forge[facing=west]", "{Kind:\"TRIAL\",Stage:2}");
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        return (at == null || at.equals("air")) && (up == null || up.equals("air"))
                && below != null && !below.equals("air") && !below.contains("stairs") && !below.startsWith(LAVA);
    }

    private static void mob(StructBp b, int x, int y, int z, String kind, String tag) {
        if (free(b, x, y, z)) {
            b.m.remove(StructBp.key(x, y, z));
            b.mobs.add(new StructBp.Mob(x, y, z, kind, tag));
        } else {
            System.err.println("[Fragua] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    private static void mobs(StructBp b) {
        mob(b, -6, 0, 12, "escoria", null);
        mob(b, 5, 0, 19, "escoria", null);
        mob(b, 4, 0, 26, "escoria", null);
        // el chispero no se mueve: vigila la nave desde el balcón
        mob(b, -9, 3, 30, "chispero", null);
        // etapa 267: el Coloso de Hierro duerme de pie delante del altar del tesoro (se despierta al acercarse); un fundidor en el patio
        mob(b, 0, 0, 30, "coloso_hierro", null);
        mob(b, 8, 0, 16, "fundidor", null);
    }
}
