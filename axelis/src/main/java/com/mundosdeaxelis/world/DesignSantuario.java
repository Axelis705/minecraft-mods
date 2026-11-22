package com.mundosdeaxelis.world;

/**
 * Santuario del Bosque (etapa 256; desde la etapa 262 es la estructura «chica» del bosque: la del NPC). Un claro con un camino de
 * tierra, un muro bajo en ruinas con su arco, un patio con flores y un estanque, y al fondo un santuario de piedra con musgo bajo un
 * roble enorme. El recorrido termina al fondo del santuario, en la celda de Ruda (barrotes y puerta de hierro, con el musgo del muro
 * del fondo dentro); el Raizal que la vigila lleva la llave, y al lado hay un cofre de provisiones. La Piedra de Portal: Naturaleza
 * está en el Gran Templo del Bosque (la estructura grande). Enemigos: tres Brotes en el patio, un Espinoso y el Raizal.
 * <p>Entrada por z = 0; el santuario ocupa x -8..8, z 19..32. El suelo de dentro del muro va en el plano (y = -1); alrededor, el
 * terreno real se aplana con una rampa suave (sin ceniza: el pasto de siempre).
 */
final class DesignSantuario {
    private DesignSantuario() {
    }

    private static final String M = "mundos_de_axelis:";
    private static final String SB = "stone_bricks";
    private static final String MSB = "mossy_stone_bricks";
    private static final String[] WALL = {"mossy_stone_bricks", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "mossy_cobblestone"};
    private static final String SBS = "stone_brick_stairs";
    private static final String MSBS = "mossy_stone_brick_stairs";
    private static final String ROOF = "spruce_stairs";
    /** Muro del patio (x -13..13, z 9..33) y santuario (x -8..8, z 19..32). */
    static final int WX = 13, WZ0 = 9, WZ1 = 33;
    static final int SX = 8, SZ0 = 19, SZ1 = 32;
    /** Celda al fondo del santuario (x -CW..CW, z CZ0..SZ1-1; la reja en z = CZ0) y sitio del NPC. */
    static final int CW = 4, CZ0 = 28;
    static final int NPC_X = 0, NPC_Z = 30;

    static StructBp make() {
        StructBp b = new StructBp(2561);
        ground(b);
        path(b);
        courtyardWall(b);
        courtyard(b);
        shrine(b);
        cell(b);
        oak(b);
        surroundings(b);
        finishing(b);
        chests(b);
        mobs(b);
        return b;
    }

    // ================================================================== suelo

    /** ¿La columna está dentro del muro del patio (o bajo el camino de entrada)? */
    private static boolean inside(int x, int z) {
        return Math.abs(x) <= WX && z >= WZ0 && z <= WZ1;
    }

    private static void ground(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                double n = StructKit.fbm(2562, x, z, 4.0) + (StructKit.hash01(2563, x, z) - 0.5) * 0.15;
                String s = n < 0.2 ? "moss_block" : n < 0.78 ? "grass_block" : n < 0.9 ? "podzol" : "coarse_dirt";
                b.set(x, -1, z, s);
                b.set(x, -2, z, "dirt");
            }
        }
    }

    private static void path(StructBp b) {
        // camino de tierra de 3 de ancho del borde del claro al arco, con piedras con musgo a los lados (va sobre el terreno real)
        for (int z = -6; z <= WZ0 + 9; z++) {
            for (int x = -1; x <= 1; x++) {
                if (z < WZ0) {
                    continue; // fuera del muro va en la mezcla con el terreno (ver surroundings)
                }
                b.set(x, -1, z, StructKit.hash01(2564, x, z) < 0.2 ? "mossy_cobblestone" : "dirt_path");
            }
        }
    }

    // ================================================================== muro del patio

    private static void courtyardWall(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                boolean edge = Math.abs(x) == WX || z == WZ0 || z == WZ1;
                if (!edge) {
                    continue;
                }
                if (z == WZ0 && Math.abs(x) <= 2) {
                    continue; // el arco
                }
                // muro en ruinas: alto variable (1 a 4) con huecos
                double n = StructKit.vnoise(2565, x * 0.35, z * 0.35);
                int h = 1 + (int) Math.round(n * 3.2);
                if (StructKit.hash01(2566, x, z) < 0.07) {
                    h = 0;
                }
                for (int y = 0; y < h; y++) {
                    b.set(x, y, z, WALL[(int) (StructKit.hash01(2567 + y, x, z) * WALL.length)]);
                }
                if (h > 0 && h < 4 && StructKit.hash01(2568, x, z) < 0.25) {
                    b.set(x, h, z, "mossy_stone_brick_slab[type=bottom]");
                }
            }
        }
        // pilares de las esquinas con farol
        for (int[] c : new int[][]{{-WX, WZ0}, {WX, WZ0}, {-WX, WZ1}, {WX, WZ1}}) {
            for (int y = 0; y < 4; y++) {
                b.set(c[0], y, c[1], y == 0 ? SB : MSB);
            }
            b.set(c[0], 4, c[1], "mossy_stone_brick_wall");
            b.set(c[0], 5, c[1], "lantern");
        }
        // el arco de la entrada
        for (int sx : new int[]{-3, 3}) {
            for (int y = 0; y <= 4; y++) {
                b.set(sx, y, WZ0, y == 0 ? SB : y == 4 ? "chiseled_stone_bricks" : MSB);
            }
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, 5, WZ0, x == 0 ? "chiseled_stone_bricks" : MSB);
        }
        b.set(-2, 4, WZ0, StructBp.stair(SBS, "-x", true));
        b.set(2, 4, WZ0, StructBp.stair(SBS, "+x", true));
        b.set(-3, 6, WZ0, "mossy_stone_brick_slab[type=bottom]");
        b.set(3, 6, WZ0, "mossy_stone_brick_slab[type=bottom]");
        b.set(0, 6, WZ0, "mossy_stone_brick_slab[type=bottom]");
    }

    // ================================================================== patio

    private static void courtyard(StructBp b) {
        // estanque con nenúfares a la izquierda
        for (int x = -10; x <= -6; x++) {
            for (int z = 11; z <= 15; z++) {
                double d = Math.hypot((x + 8) / 2.3, (z - 13) / 2.3);
                if (d <= 1.0) {
                    b.set(x, -1, z, "water");
                    b.set(x, -2, z, "clay");
                    if (StructKit.hash01(2569, x, z) < 0.3) {
                        b.set(x, 0, z, "lily_pad");
                    }
                } else if (d <= 1.35) {
                    b.set(x, -1, z, StructKit.hash01(2570, x, z) < 0.5 ? "mossy_cobblestone" : "moss_block");
                }
            }
        }
        // columna caída a la derecha
        for (int z = 12; z <= 16; z++) {
            b.set(8, 0, z, StructKit.hash01(2571, 8, z) < 0.5 ? "stone_bricks" : "mossy_stone_bricks");
            if (StructKit.hash01(2572, 8, z) < 0.5) {
                b.set(8, 1, z, "moss_carpet");
            }
        }
        b.set(9, 0, 11, "chiseled_stone_bricks");
        b.set(9, 1, 11, "mossy_stone_brick_wall");
        // flores, helechos y hierba (solo sobre el pasto)
        String[] plants = {"grass", "grass", "fern", "poppy", "azure_bluet", "oxeye_daisy", "cornflower", "dandelion", "lily_of_the_valley"};
        for (int x = -WX + 1; x <= WX - 1; x++) {
            for (int z = WZ0 + 1; z <= SZ0 - 1; z++) {
                String g = b.get(x, -1, z);
                if (g != null && (g.equals("grass_block") || g.equals("moss_block")) && b.get(x, 0, z) == null && Math.abs(x) > 1
                        && StructKit.hash01(2573, x, z) < 0.3) {
                    b.set(x, 0, z, plants[(int) (StructKit.hash01(2574, x, z) * plants.length)]);
                }
            }
        }
        // a los lados del santuario también algo de verde
        for (int x = -WX + 1; x <= WX - 1; x++) {
            for (int z = SZ0; z <= WZ1 - 1; z++) {
                if (Math.abs(x) <= SX + 1) {
                    continue;
                }
                String g = b.get(x, -1, z);
                if (g != null && g.equals("grass_block") && b.get(x, 0, z) == null && StructKit.hash01(2575, x, z) < 0.25) {
                    b.set(x, 0, z, StructKit.hash01(2576, x, z) < 0.6 ? "fern" : "grass");
                }
            }
        }
        // el camino sigue dentro del patio hasta la puerta del santuario
        for (int z = WZ0; z < SZ0; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, StructKit.hash01(2577, x, z) < 0.25 ? "mossy_cobblestone" : "dirt_path");
                b.m.remove(StructBp.key(x, 0, z));
            }
        }
        // dos faroles en postes junto al camino
        for (int sx : new int[]{-3, 3}) {
            b.set(sx, 0, 14, "mossy_cobblestone_wall");
            b.set(sx, 1, 14, "mossy_cobblestone_wall");
            b.set(sx, 2, 14, "lantern");
        }
    }

    // ================================================================== el santuario

    private static void shrine(StructBp b) {
        int top = 5;
        // suelo
        for (int x = -SX; x <= SX; x++) {
            for (int z = SZ0; z <= SZ1; z++) {
                b.set(x, -1, z, (x + z & 1) == 0 ? MSB : SB);
                b.set(x, -2, z, "cobblestone");
            }
        }
        // paredes
        for (int y = 0; y <= top; y++) {
            for (int x = -SX; x <= SX; x++) {
                for (int z = SZ0; z <= SZ1; z++) {
                    boolean edge = Math.abs(x) == SX || z == SZ0 || z == SZ1;
                    b.set(x, y, z, edge ? WALL[(int) (StructKit.hash01(2580 + y, x, z) * 4)] : "air");
                }
            }
        }
        // pilares de roble en esquinas y a mitad de los muros
        for (int x : new int[]{-SX, SX}) {
            for (int z : new int[]{SZ0, 25, SZ1}) {
                for (int y = 0; y <= top; y++) {
                    b.set(x, y, z, "oak_log[axis=y]");
                }
            }
        }
        // vigas bajo el tejado
        for (int z = SZ0; z <= SZ1; z++) {
            b.set(-SX, top + 1, z, "oak_log[axis=z]");
            b.set(SX, top + 1, z, "oak_log[axis=z]");
        }
        for (int z : new int[]{22, 25, 29}) {
            for (int x = -SX + 1; x <= SX - 1; x++) {
                b.set(x, top + 1, z, "stripped_oak_log[axis=x]");
            }
        }
        // puerta (arco de 3 de ancho)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                b.set(x, y, SZ0, "air");
            }
        }
        b.set(-1, 3, SZ0, StructBp.stair(SBS, "-x", true));
        b.set(1, 3, SZ0, StructBp.stair(SBS, "+x", true));
        b.set(0, 3, SZ0, "chiseled_stone_bricks");
        b.set(-2, -1, SZ0 - 1, MSB);
        b.set(2, -1, SZ0 - 1, MSB);
        b.set(0, -1, SZ0 - 1, "mossy_cobblestone");
        // ventanas
        for (int z : new int[]{22, 28}) {
            for (int y = 2; y <= 3; y++) {
                b.set(-SX, y, z, "air");
                b.set(SX, y, z, "air");
            }
        }
        roof(b, top + 1);
        // musgo en el muro del fondo (queda dentro de la celda), luces
        for (int x = -4; x <= 4; x++) {
            for (int y = 1; y <= 5; y++) {
                if (StructKit.vnoise(2585, x * 0.6, y * 0.6) + (StructKit.hash01(2584, x, y) - 0.5) * 0.3 > 0.62) {
                    b.set(x, y, SZ1, "moss_block");
                }
            }
        }
        b.set(0, 4, SZ1, "chiseled_stone_bricks");
        b.set(-6, 2, SZ1 - 1, "lantern");
        b.set(-6, 1, SZ1 - 1, "mossy_stone_brick_wall");
        b.set(-6, 0, SZ1 - 1, MSB);
        b.set(6, 2, SZ1 - 1, "lantern");
        b.set(6, 1, SZ1 - 1, "mossy_stone_brick_wall");
        b.set(6, 0, SZ1 - 1, MSB);
        // etapa 288: el santuario es el claro de la Dríade del Roble (antes había bancos sueltos sin sentido). En el centro, un nudo de
        // raíces donde duerme arrodillada, dentro de un anillo de musgo con pétalos; raíces que bajan de las vigas y salen de las
        // esquinas, azaleas en flor en los rincones y velas. El suelo del medio queda libre para pelear.
        int ax = 0, az = 24;
        for (int x = -SX + 1; x <= SX - 1; x++) {
            for (int z = SZ0 + 1; z < CZ0; z++) {
                double d = Math.hypot(x - ax, (z - az) * 1.15);
                if (d <= 1.2) {
                    b.set(x, -1, z, "rooted_dirt");
                } else if (d <= 3.2) {
                    b.set(x, -1, z, StructKit.hash01(2600, x, z) < 0.75 ? "moss_block" : "mossy_cobblestone");
                } else if (d <= 3.9) {
                    b.set(x, -1, z, "mossy_stone_bricks");
                }
                if (d > 2.2 && d <= 3.2 && StructKit.hash01(2601, x, z) < 0.45) {
                    b.set(x, 0, z, "pink_petals[facing=" + (new String[]{"north", "east", "south", "west"})[(int) (StructKit.hash01(2602, x, z) * 4)]
                            + ",flower_amount=" + (2 + (int) (StructKit.hash01(2603, x, z) * 3)) + "]");
                }
            }
        }
        // el camino de musgo de la puerta al anillo
        for (int z = SZ0 + 1; z <= 20; z++) {
            b.set(0, 0, z, "moss_carpet");
        }
        // raíces que cuelgan de las vigas sobre el anillo
        for (int x = -3; x <= 3; x += 2) {
            for (int z : new int[]{22, 25}) {
                if (StructKit.hash01(2604, x, z) < 0.8) {
                    b.set(x, top, z, "hanging_roots");
                }
            }
        }
        // en los rincones: raíces de mangle que salen del muro, una azalea en flor y una vela encima de un muro bajo
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int z : new int[]{SZ0 + 1, CZ0 - 1}) {
                int x = sx * (SX - 1);
                boolean cofre = z == SZ0 + 1 && sx > 0; // ahí está el cofre con cerradura (mirando a la sala)
                for (int y = 2; y <= top - 1; y++) {
                    if (StructKit.hash01(2605 + y, x, z) < 0.55) {
                        b.set(x, y, z, "mangrove_roots");
                    }
                }
                if (cofre) {
                    continue;
                }
                b.set(x, 0, z, "flowering_azalea");
                b.set(x - sx, 0, z, "mossy_stone_brick_wall");
                b.set(x - sx, 1, z, "candle[candles=3,lit=true]");
            }
        }
        // faroles colgados de las vigas
        for (int z : new int[]{22, 29}) {
            StructKit.hangLantern(b, -3, top, z, 1, "lantern");
            StructKit.hangLantern(b, 3, top, z, 1, "lantern");
        }
    }

    /**
     * Tejado a dos aguas bajo (media pendiente: escalón y losa alternados), de abeto oscuro con alero de roble oscuro, con musgo y
     * hojas creciendo encima y un par de huecos (está viejo). Los hastiales se cierran con tablones.
     */
    private static void roof(StructBp b, int y0) {
        int over = 1;
        int half = SX + over;
        for (int z = SZ0 - over; z <= SZ1 + over; z++) {
            boolean endZ = z == SZ0 - over || z == SZ1 + over;
            for (int i = 0; i <= half; i++) {
                int y = y0 + i / 2;
                boolean ridge = i == half;
                for (int side : new int[]{-1, 1}) {
                    int x = side * (half - i);
                    String up = side < 0 ? "+x" : "-x";
                    String st;
                    if (ridge) {
                        st = "dark_oak_slab[type=bottom]";
                        b.set(0, y, z, "dark_oak_planks");
                        b.set(0, y + 1, z, st);
                        continue;
                    }
                    if (i == 0) {
                        st = StructBp.stair("dark_oak_stairs", up, false);
                    } else if (i % 2 == 0) {
                        st = StructBp.stair(ROOF, up, false);
                    } else {
                        st = "spruce_slab[type=top]";
                    }
                    if (endZ && i > 0) {
                        st = i % 2 == 0 ? StructBp.stair("dark_oak_stairs", up, false) : "dark_oak_slab[type=top]";
                    }
                    // huecos de tejas caídas
                    if (!endZ && i > 1 && StructKit.hash01(2598, x, z) < 0.04) {
                        continue;
                    }
                    b.set(x, y, z, st);
                    // musgo y hojas encima en algunas partes
                    double n = StructKit.vnoise(2599, x * 0.3, z * 0.3);
                    if (n > 0.62 && !st.contains("type=top") && b.get(x, y + 1, z) == null) {
                        b.set(x, y + 1, z, n > 0.75 ? "oak_leaves[persistent=true]" : "moss_carpet");
                    }
                }
            }
        }
        // hastiales: tablones bajo el tejado en los dos extremos
        for (int z : new int[]{SZ0, SZ1}) {
            for (int i = 1; i < half; i++) {
                int yTop = y0 + i / 2;
                for (int x : new int[]{-(half - i), half - i}) {
                    for (int y = y0; y < yTop + (i % 2 == 1 ? 1 : 0); y++) {
                        if (Math.abs(x) <= SX) {
                            b.set(x, y, z, "spruce_planks");
                        }
                    }
                }
            }
            for (int y = y0; y <= y0 + half / 2; y++) {
                b.set(0, y, z, "spruce_planks");
            }
            b.set(0, y0 + 1, z, z == SZ0 ? "oak_log[axis=z]" : "spruce_planks");
        }
    }

    // ================================================================== la celda

    /** Celda al este del santuario: barrotes que dan al salón y una puerta de hierro sin manivela (el NPC se rescatará). */
    private static void cell(StructBp b) {
        // muro de la celda al fondo del salón: reja de barrotes a los lados de la puerta, paredes hasta el techo
        for (int x = -CW; x <= CW; x++) {
            for (int y = 0; y <= 5; y++) {
                boolean bars = y <= 2 && x != 0 && Math.abs(x) <= CW - 1;
                b.set(x, y, CZ0, bars ? "iron_bars" : WALL[(int) (StructKit.hash01(2592 + y, x, CZ0) * 4)]);
            }
        }
        for (int z = CZ0 + 1; z < SZ1; z++) {
            for (int y = 0; y <= 5; y++) {
                b.set(-CW, y, z, WALL[(int) (StructKit.hash01(2593 + y, -CW, z) * 4)]);
                b.set(CW, y, z, WALL[(int) (StructKit.hash01(2594 + y, CW, z) * 4)]);
            }
        }
        StructKit.ironDoor(b, 0, 0, CZ0, "-z", false);
        b.set(0, 2, CZ0, "chiseled_stone_bricks");
        // dentro: suelo de musgo y piedra, cama de paja, cadenas, una vela y telarañas
        for (int x = -CW + 1; x <= CW - 1; x++) {
            for (int z = CZ0 + 1; z < SZ1; z++) {
                b.set(x, -1, z, StructKit.hash01(2590, x, z) < 0.4 ? "mossy_cobblestone" : "cobblestone");
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, "air");
                }
                b.set(x, 5, z, "mossy_cobblestone");
            }
        }
        b.set(CW - 1, 0, CZ0 + 1, "hay_block[axis=z]");
        b.set(CW - 1, 0, CZ0 + 2, "hay_block[axis=z]");
        b.set(CW - 1, 1, CZ0 + 1, "white_carpet");
        StructKit.hangLantern(b, NPC_X, 4, NPC_Z, 1, "soul_lantern");
        b.set(CW - 1, 4, SZ1 - 1, "cobweb");
        b.set(-CW + 1, 4, CZ0 + 1, "cobweb");
        b.set(-CW + 1, 0, SZ1 - 1, "cauldron");
        b.set(-CW + 1, 0, CZ0 + 1, "candle[candles=1,lit=true]");
        b.set(CW - 1, 0, SZ1 - 1, "potted_fern");
        // encima de la celda, hasta el tejado, macizo de tablones (que no quede un desván al que no se llega)
        for (int x = -CW; x <= CW; x++) {
            for (int z = CZ0; z < SZ1; z++) {
                int roofY = -1;
                for (int y = 6; y <= 14; y++) {
                    String at = b.get(x, y, z);
                    if (at != null && !at.equals("air")) {
                        roofY = y;
                        break;
                    }
                }
                for (int y = 6; y < roofY; y++) {
                    b.set(x, y, z, "spruce_planks");
                }
            }
        }
    }

    // ================================================================== el roble del santuario

    private static void oak(StructBp b) {
        String[] leaves = {"oak_leaves[persistent=true]", "oak_leaves[persistent=true]", "oak_leaves[persistent=true]", "azalea_leaves[persistent=true]"};
        // un roble enorme en la esquina de atrás a la izquierda, con la copa sobre el tejado
        StructKit.bigTree(b, -12, 29, 14, "oak_log", leaves, 7);
        // ramas gruesas hacia el santuario y hacia fuera
        for (int i = 1; i <= 4; i++) {
            b.set(-12 + i, 8 + i / 2, 30, "oak_log[axis=x]");
            b.set(-12 - i, 9 + i / 2, 29, "oak_log[axis=x]");
        }
        for (int i = 1; i <= 3; i++) {
            b.set(-11, 10 + i / 2, 30 - i, "oak_log[axis=z]");
        }
        // raíces que asoman por el patio
        for (int[] r : new int[][]{{-10, 28}, {-13, 27}, {-11, 31}, {-9, 30}}) {
            b.set(r[0], 0, r[1], "rooted_dirt");
        }
    }

    // ================================================================== alrededor (en el terreno real)

    /** Distancia aproximada (fuera) al borde de la obra; negativa dentro. */
    static double edge(int x, int z) {
        double dx = Math.max(0, Math.abs(x) - (WX + 1));
        double dz = Math.max(Math.max(0, (WZ0 - 3) - z), Math.max(0, z - (WZ1 + 4)));
        double out = Math.hypot(dx, dz);
        if (out > 0) {
            return out;
        }
        return -Math.min(Math.min((WX + 1) - Math.abs(x), z - (WZ0 - 3)), (WZ1 + 4) - z);
    }

    /**
     * El claro: bajo la obra y hasta 2 bloques de ella, todo a su altura; luego una rampa suave de 7 a 10 bloques hasta el terreno
     * de alrededor (se quitan los árboles que estorben). Fuera del muro, el camino de tierra sigue hasta el borde del claro.
     */
    private static void surroundings(StructBp b) {
        int R = 34;
        for (int x = -R; x <= R; x++) {
            for (int z = WZ0 - R; z <= WZ1 + R; z++) {
                double e = edge(x, z);
                double ramp = 7 + 3 * StructKit.vnoise(2595, x * 0.1, z * 0.1);
                double w = e <= 2 ? 0 : Math.min(1, (e - 2) / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 1 && z >= -6 && z < WZ0;
                if (w >= 0.999 && !road) {
                    continue;
                }
                String top = road ? (StructKit.hash01(2564, x, z) < 0.2 ? "mossy_cobblestone" : "dirt_path") : null;
                b.blend(x, z, road ? Math.min(w, 0.5) : w, top, null);
            }
        }
        // un tronco caído y setas junto al camino (sobre el suelo real)
        StructBp.Anchor log = b.anchor(-6, 2);
        for (int i = 0; i < 4; i++) {
            log.set(0, 0, i, "oak_log[axis=z]");
        }
        log.set(0, 1, 1, "moss_carpet");
        log.set(0, 1, 2, "brown_mushroom");
        StructBp.Anchor m = b.anchor(6, 4);
        m.set(0, 0, 0, "red_mushroom");
        StructBp.Anchor post = b.anchor(-3, -2);
        post.set(0, 0, 0, "mossy_cobblestone_wall");
        post.set(0, 1, 0, "mossy_cobblestone_wall");
        post.set(0, 2, 0, "lantern");
        StructBp.Anchor post2 = b.anchor(3, -2);
        post2.set(0, 0, 0, "mossy_cobblestone_wall");
        post2.set(0, 1, 0, "mossy_cobblestone_wall");
        post2.set(0, 2, 0, "lantern");
    }

    private static void finishing(StructBp b) {
        // musgo en las piedras de abajo, enredaderas colgando de los aleros
        StructKit.weather(b, 2596, "stone_bricks", "mossy_stone_bricks", 5.0, 0.55);
        StructKit.weather(b, 2597, "cobblestone", "mossy_cobblestone", 5.0, 0.6);
        StructKit.hangVines(b, "spruce_stairs", 6, 0.18, 3);
        StructKit.hangVines(b, "mossy", 2, 0.06, 2);
    }

    // ================================================================== cofre y enemigos

    /** Provisiones junto a la celda; etapa 268: un premio en el rincón del patio y una cerradura fácil en el santuario. */
    private static void chests(StructBp b) {
        b.setNbt(5, 0, 30, M + "vault_chest[facing=east]", "{Kind:\"ANTE\",Stage:1}");
        StructKit.vault(b, -12, 0, 10, "east", "TRIAL", 1, 0);
        StructKit.vault(b, 7, 0, 20, "west", "LOCKED", 1, 1);
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        return (at == null || at.equals("air") || at.equals("grass") || at.equals("fern") || at.equals("moss_carpet")) && (up == null || up.equals("air"))
                && below != null && !below.equals("air") && !below.contains("stairs");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind) {
        mob(b, x, y, z, kind, null);
    }

    private static void mob(StructBp b, int x, int y, int z, String kind, String tag) {
        if (free(b, x, y, z)) {
            b.m.remove(StructBp.key(x, y, z));
            b.mobs.add(new StructBp.Mob(x, y, z, kind, tag));
        } else {
            System.err.println("[Santuario] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    private static void mobs(StructBp b) {
        mob(b, -5, 0, 13, "brote");
        mob(b, 5, 0, 15, "brote");
        mob(b, -2, 0, 17, "brote");
        mob(b, -6, 0, 28, "espinoso");
        // etapa 288: la Dríade del Roble duerme en el centro del santuario y lleva la llave de la celda; el Raizal pasea por el patio
        mob(b, 0, 0, 24, "driade_roble", "llave_celda");
        mob(b, -10, 0, 25, "raizal");
        // Ruda, la herbolaria, presa en la celda
        mob(b, NPC_X, 0, NPC_Z, "habitante", "ruda");
    }
}
