package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Fundición del Titán: una fortaleza de piedra negra con foso de lava, torres con tejados picudos, una gran nave de fundición
 * con río de lava, pasarelas, grúas y yunque gigante, talleres en los patios y hornos profundos bajo tierra.
 * Entrada por z = 0, se extiende hacia z positivo.
 * <p>Etapa 266: es la estructura grande del tier de la forja (la «Gran Fundición»). Tiene las dos cosas importantes, abajo, en los
 * hornos profundos: en la isla del lago de lava, la Piedra de Portal: Forja (el final del recorrido) con un cofre, y en la pared del
 * fondo, la celda de Hilda. La llave la lleva el Fundidor que vigila la isla. Escorias y chisperos por el camino; aparece sola.
 */
final class DesignForja {
    private DesignForja() {
    }

    private static final String PBB = "polished_blackstone_bricks";
    private static final String PBS = "polished_blackstone_brick_stairs";
    private static final String PBL = "polished_blackstone_brick_slab";
    private static final String PBW = "polished_blackstone_brick_wall";
    private static final String BLK = "blackstone";
    private static final String CPB = "chiseled_polished_blackstone";
    private static final String GB = "gilded_blackstone";
    private static final String NB = "nether_bricks";
    private static final String RNB = "red_nether_bricks";
    private static final String DT = "deepslate_tiles";
    private static final String DTS = "deepslate_tile_stairs";
    private static final String DTL = "deepslate_tile_slab";
    private static final String OBS = "obsidian";
    private static final String CRY = "crying_obsidian";
    private static final String BAS = "smooth_basalt";
    private static final String PBAS = "polished_basalt[axis=y]";
    private static final String CU = "waxed_oxidized_cut_copper";
    private static final String CUS = "waxed_oxidized_cut_copper_stairs";
    private static final String CUL = "waxed_oxidized_cut_copper_slab";
    private static final String WCU = "waxed_weathered_cut_copper";
    private static final String LAVA = "lava";
    private static final String M = "mundos_de_axelis:";
    /** Lo que ocupa (para mezclar el borde con el terreno). */
    static final int X0 = -40, X1 = 40, Z0 = -16, Z1 = 84;
    /** Suelo de los hornos profundos y sitio de Hilda en su celda (etapa 266). */
    static final int FY = -14;
    static final int NPC_X = -13, NPC_Z = 78;

    static StructBp make() {
        StructBp b = new StructBp(5005);
        b.foundation(-40, -16, 40, 84, BLK);
        ground(b);
        // etapa 291: ya no es un castillo (con muralla y torres de tejado picudo, igual que la Ciudadela): es una fundición de
        // verdad, con cerca de hierro, cuatro altos hornos con chimeneas que echan humo, portón de grúa, vías y montones de carbón,
        // mineral y escoria en el patio, y la nave con tejado de dientes de sierra y claraboyas
        perimeter(b);
        factoryGate(b);
        blastFurnaces(b);
        hall(b);
        yardBuildings(b);
        yardWork(b);
        basement(b);
        finishing(b);
        Linker.link(b, 0, 0, -3, 10, PBB, PBS);
        // la celda se hace después de unir las salas: es cerrada a propósito (se abre con la llave)
        hildaCell(b, FY);
        surroundings(b);
        mobs(b);
        roomChests(b);
        return b;
    }

    // ================================================================== suelo, foso, camino

    private static void ground(StructBp b) {
        b.box(-40, -1, -16, 40, -1, 84, BLK);
        // grietas de magma en el suelo (siguiendo una línea quebrada, no puntos sueltos)
        for (int x = -40; x <= 40; x++) {
            for (int z = -16; z <= 84; z++) {
                double n = fbm(7, x, z, 6.0);
                if (n > 0.46 && n < 0.50) {
                    b.set(x, -1, z, "magma_block");
                } else if (n > 0.50 && n < 0.56) {
                    b.set(x, -1, z, b.pick("basalt[axis=y]", BLK, "cracked_polished_blackstone_bricks"));
                }
            }
        }
        // foso de lava con borde
        for (int x = -37; x <= 37; x++) {
            for (int z = 7; z <= 83; z++) {
                boolean outerRim = x == -37 || x == 37 || z == 7 || z == 83;
                boolean inRing = x >= -36 && x <= 36 && z >= 8 && z <= 82 && !(x >= -32 && x <= 32 && z >= 12 && z <= 78);
                if (outerRim) {
                    b.set(x, -1, z, PBB);
                    b.set(x, 0, z, PBB);
                } else if (inRing) {
                    b.set(x, -2, z, OBS);
                    b.set(x, -1, z, LAVA);
                }
            }
        }
        // camino de entrada y puente de piedra con arcos
        for (int z = -14; z <= 13; z++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, -1, z, Math.abs(x) == 4 ? CPB : ((z & 1) == 0 ? PBB : "polished_blackstone"));
            }
        }
        for (int z = 6; z <= 13; z++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, -1, z, Math.abs(x) >= 3 ? PBB : "polished_blackstone_bricks");
            }
            for (int s : new int[]{-1, 1}) {
                b.set(s * 4, 0, z, PBW);
                b.set(s * 5, -1, z, PBB);
                b.set(s * 5, 0, z, PBW);
                if ((z - 6) % 3 == 0) {
                    b.set(s * 4, 0, z, PBB);
                    b.set(s * 4, 1, z, CPB);
                    b.set(s * 4, 2, z, "lantern[hanging=false]");
                }
            }
        }
        // pilares de apoyo del puente en la lava
        for (int s : new int[]{-1, 1}) {
            for (int z = 8; z <= 12; z += 4) {
                for (int y = -2; y <= -1; y++) {
                    b.set(s * 3, y, z, PBB);
                }
            }
        }
        // braseros a lo largo del camino
        for (int z = -12; z <= 4; z += 4) {
            for (int s : new int[]{-1, 1}) {
                brazier(b, s * 6, 0, z, PBB, "campfire[lit=true]");
                b.set(s * 6, 0, z, CPB);
            }
        }
        // portal exterior: dos pilares con garras y arco
        for (int s : new int[]{-1, 1}) {
            for (int y = 0; y <= 14; y++) {
                b.box(s * 7 - 1, y, -14, s * 7 + 1, y, -13, y % 5 == 4 ? CPB : PBB);
            }
            for (int i = 0; i < 4; i++) {
                b.set(s * 7, 15 + i, -14, i == 3 ? GB : BLK);
                b.set(s * 7 + s, 15 + i / 2, -14, BLK);
            }
            b.set(s * 7, 19, -14, "lantern[hanging=false]");
        }
        b.box(-7, 14, -14, 7, 14, -13, CPB);
        for (int x = -6; x <= 6; x++) {
            b.set(x, 15, -14, (x & 1) == 0 ? PBB : "air");
        }
    }

    // ================================================================== etapa 291: la fundición industrial

    private static final String BR = "bricks";
    private static final String IRON = "iron_block";

    /** Cerca del recinto: zócalo de piedra negra con barrotes de hierro encima, postes con faroles, y el hueco del portón. */
    private static void perimeter(StructBp b) {
        int x0 = -31, x1 = 31, z0 = 15, z1 = 75;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                if (!edge || (z == z0 && Math.abs(x) <= 6)) {
                    continue;
                }
                boolean post = (x - x0) % 6 == 0 && (z == z0 || z == z1) || (z - z0) % 6 == 0 && (x == x0 || x == x1);
                b.set(x, -1, z, BLK);
                b.set(x, 0, z, PBB);
                b.set(x, 1, z, CPB);
                for (int y = 2; y <= 4; y++) {
                    b.set(x, y, z, post ? PBB : "iron_bars");
                }
                if (post) {
                    b.set(x, 5, z, PBW);
                    b.set(x, 6, z, "lantern[hanging=false]");
                }
            }
        }
    }

    /** El portón: dos pilares de ladrillo con una viga de acero arriba, cadenas y ganchos colgando, y las vías que entran. */
    private static void factoryGate(StructBp b) {
        for (int s = -1; s <= 1; s += 2) {
            for (int y = -1; y <= 13; y++) {
                for (int dx = 0; dx <= 2; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        b.set(s * (7 + dx), y, 15 + dz, y % 5 == 4 ? PBB : BR);
                    }
                }
            }
            b.set(s * 8, 14, 15, "campfire[lit=true]");
            // la puerta corrediza, abierta: barrotes plegados junto al pilar
            for (int y = 0; y <= 6; y++) {
                b.set(s * 6, y, 14, "iron_bars");
            }
        }
        for (int x = -9; x <= 9; x++) {
            b.set(x, 12, 15, "polished_basalt[axis=x]");
            b.set(x, 13, 15, x % 3 == 0 ? IRON : PBL + "[type=bottom]");
        }
        for (int x : new int[]{-3, 3}) {
            for (int y = 9; y <= 11; y++) {
                b.set(x, y, 15, "chain[axis=y]");
            }
            b.set(x, 8, 15, "lantern[hanging=true]");
        }
        // vías de mineral: del portón a la nave
        for (int z = 14; z <= 30; z++) {
            for (int x : new int[]{-2, 2}) {
                b.set(x, -1, z, "polished_blackstone");
                b.set(x, 0, z, "rail[shape=north_south]");
            }
        }
    }

    /**
     * Cuatro altos hornos en las esquinas del recinto: torres redondas de ladrillo que se estrechan hacia arriba, con aros de hierro,
     * la boca del horno brillando hacia el patio y una chimenea que echa humo.
     */
    private static void blastFurnaces(StructBp b) {
        int[][] t = {{-31, 16}, {31, 16}, {-31, 74}, {31, 74}};
        for (int[] c : t) {
            int cx = c[0], cz = c[1];
            int top = 26;
            for (int y = -1; y <= top; y++) {
                double r = 6.2 - 2.0 * y / top;
                for (int dx = -7; dx <= 7; dx++) {
                    for (int dz = -7; dz <= 7; dz++) {
                        double d = Math.hypot(dx, dz);
                        if (d > r) {
                            continue;
                        }
                        String m = y % 7 == 3 && d > r - 1.1 ? IRON : (d < r - 1.2 ? BLK : (StructKit.hash01(2910 + y, dx, dz) < 0.12 ? "cracked_stone_bricks" : BR));
                        b.set(cx + dx, y, cz + dz, m);
                    }
                }
            }
            // la chimenea de arriba (con humo)
            for (int y = top + 1; y <= top + 14; y++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            b.set(cx + dx, y, cz + dz, y % 6 == 0 ? PBB : BR);
                        } else {
                            b.set(cx + dx, y, cz + dz, y == top + 13 ? "hay_block[axis=y]" : "air");
                        }
                    }
                }
            }
            b.set(cx, top + 14, cz, "campfire[lit=true,signal_fire=true]");
            // la boca del horno, hacia el patio: magma detrás de una reja
            int ix = cx < 0 ? 1 : -1, iz = cz < 40 ? 1 : -1;
            for (int k = -1; k <= 1; k++) {
                for (int y = 0; y <= 2; y++) {
                    int mx = cx + ix * 5 + (k * iz), mz = cz + iz * 5 - (k * ix);
                    b.set(mx, y, mz, "iron_bars");
                    b.set(mx - ix, y, mz - iz, "magma_block");
                }
            }
            b.set(cx + ix * 6, -1, cz + iz * 6, "magma_block");
        }
    }

    /** El patio de trabajo: montones de carbón y de escoria, cajas de mineral, una grúa sobre las vías y yunques al aire libre. */
    private static void yardWork(StructBp b) {
        heap(b, -14, 22, 3.2, 4, "coal_block", BLK, 2911);
        heap(b, 14, 22, 3.0, 3, "raw_iron_block", "raw_copper_block", 2912);
        heap(b, -13, 69, 2.6, 3, BLK, "magma_block", 2913);
        heap(b, 13, 69, 2.6, 3, "basalt[axis=y]", "magma_block", 2914);
        // cajas de mineral junto a las vías
        for (int[] c : new int[][]{{-5, 24}, {5, 24}, {-5, 27}, {5, 27}}) {
            b.set(c[0], 0, c[1], "barrel[facing=up]");
            b.set(c[0], 1, c[1], StructKit.hash01(2915, c[0], c[1]) < 0.5 ? "raw_gold_block" : "raw_iron_block");
        }
        // grúa de pórtico sobre las vías: dos patas de acero, viga, y un gancho con un cubo
        for (int s = -1; s <= 1; s += 2) {
            for (int y = 0; y <= 10; y++) {
                b.set(s * 6, y, 20, "polished_basalt[axis=y]");
            }
        }
        for (int x = -6; x <= 6; x++) {
            b.set(x, 11, 20, IRON);
        }
        for (int y = 7; y <= 10; y++) {
            b.set(0, y, 20, "chain[axis=y]");
        }
        b.set(0, 6, 20, "cauldron");
    }

    /** Un montón redondeado (carbón, mineral, escoria) de radio r y altura h, con dos materiales mezclados. */
    private static void heap(StructBp b, int cx, int cz, double r, int h, String a, String c, long seed) {
        for (int y = 0; y < h; y++) {
            double rr = r * (1 - (double) y / h) + 0.4;
            for (int dx = (int) -Math.ceil(rr); dx <= rr; dx++) {
                for (int dz = (int) -Math.ceil(rr); dz <= rr; dz++) {
                    if (Math.hypot(dx, dz) <= rr) {
                        b.set(cx + dx, y, cz + dz, StructKit.hash01(seed + y, dx, dz) < 0.7 ? a : c);
                    }
                }
            }
        }
    }

    /**
     * Tejado de dientes de sierra a lo largo de Z: cada diente (8 bloques) sube de golpe con una cara de cristal que mira a la entrada
     * (la claraboya) y baja en pendiente hacia el fondo. Por dentro, el techo queda plano.
     */
    private static void sawtooth(StructBp b, int x0, int x1, int z0, int z1, int y) {
        int tooth = 8, hgt = 4;
        for (int z = z0; z <= z1; z++) {
            int k = (z - z0) % tooth;
            int h = (int) Math.round(hgt * (1.0 - k / (double) tooth));
            for (int x = x0; x <= x1; x++) {
                boolean edgeX = x == x0 || x == x1;
                for (int dy = 0; dy < h; dy++) {
                    String m;
                    if (k == 0 && !edgeX) {
                        m = dy == 0 ? DT : "glass"; // la claraboya
                    } else if (dy == h - 1) {
                        m = k == 0 ? DT : DTS + "[facing=north,half=bottom]";
                    } else {
                        m = DT;
                    }
                    b.set(x, y + dy, z, m);
                }
                if (h == 0) {
                    b.set(x, y, z, DTL + "[type=bottom]");
                }
            }
        }
    }

    // ================================================================== gran nave de fundición

    private static void hall(StructBp b) {
        b.box(-18, -1, 32, 18, 24, 72, PBB);
        Room nave = new Room(b, -16, 34, 16, 70, -1, 25).carve();
        // zócalo, bandas, cornisa
        b.walls(-19, -1, 31, 19, 2, 73, BLK);
        ring(b, -19, 31, 19, 73, 3, PBS, false);
        b.walls(-18, 10, 32, 18, 10, 72, CPB);
        b.walls(-18, 18, 32, 18, 18, 72, GB);
        ring(b, -19, 31, 19, 73, 24, PBS, true);
        // etapa 291: tejado industrial de dientes de sierra, con claraboyas mirando a la entrada
        sawtooth(b, -19, 19, 31, 73, 25);
        // chimeneas
        chimney(b, -10, 40, 3, 52);
        chimney(b, 10, 52, 3, 58);
        chimney(b, -6, 64, 3, 50);
        // ventanas altas con luz naranja
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{40, 48, 56, 64}) {
                openX(b, z, 12, s * 17, s * 18, 1, 9, 2, CPB, "orange_stained_glass_pane");
            }
        }
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{38, 46, 54, 62, 70}) {
                buttress(b, s * 19, z, s > 0 ? "+x" : "-x", 3, 21, 4, PBS, PBB);
            }
        }
        // portón principal en la fachada, a eje con la entrada
        openZ(b, 0, 0, 31, 33, 5, 16, 2, CPB, null);
        for (int x = -5; x <= 5; x++) {
            for (int y = 8; y <= 12; y++) { // rastrillo levantado
                if (Math.abs(x) <= 5 - (y > 8 ? (y - 8) : 0)) {
                    b.set(x, y, 32, "iron_bars");
                }
            }
        }
        // gran rosetón de fuego sobre el portón
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                double d = Math.hypot(x, y);
                if (d <= 4.2) {
                    b.set(x, 20 + y, 31, d < 1.5 ? "shroomlight" : ((x == 0 || y == 0 || Math.abs(x) == Math.abs(y)) ? CPB : "orange_stained_glass"));
                    b.set(x, 20 + y, 32, "air");
                } else if (d <= 5.2) {
                    b.set(x, 20 + y, 31, GB);
                }
            }
        }

        // ---- interior ----
        nave.baseboard(PBS).torches(8, 5, false);
        for (int x = -16; x <= 16; x++) {
            for (int z = 34; z <= 70; z++) {
                b.set(x, -1, z, (x + z) % 2 == 0 ? BAS : "polished_basalt");
            }
        }
        borderLine(b, -16, 34, 16, 70, -1, 1, CPB);
        // río de lava central que desemboca en el crisol
        b.air(-3, -3, 34, 3, -1, 56);
        for (int x = -3; x <= 3; x++) {
            for (int z = 34; z <= 56; z++) {
                b.set(x, -3, z, OBS);
                b.set(x, -2, z, LAVA);
                b.set(x, -1, z, LAVA);
                if (x == -3 || x == 3) {
                    b.set(x, -1, z, LAVA);
                    b.set(x, 0, z, (z & 1) == 0 ? PBW : "air");
                }
            }
        }
        // puentes de basalto con barandilla
        for (int z : new int[]{40, 50}) {
            for (int x = -4; x <= 4; x++) {
                for (int dz = -1; dz <= 1; dz++) {
                    b.set(x, -1, z + dz, dz == 0 ? "polished_blackstone" : BAS);
                    if (Math.abs(dz) == 1) {
                        b.set(x, 0, z + dz, "iron_bars");
                    }
                }
            }
            b.set(-4, 0, z - 1, PBW);
            b.set(4, 0, z - 1, PBW);
        }
        crucible(b, 0, 63);
        // columnas
        for (int z = 38; z <= 66; z += 7) {
            for (int s : new int[]{-1, 1}) {
                column2(b, s > 0 ? 8 : -9, z, 0, 22, "polished_blackstone_bricks", PBL);
                b.box(s > 0 ? 8 : -9, 23, z, s > 0 ? 9 : -8, 23, z + 1, GB);
            }
        }
        // vigas y grúas
        for (int z = 38; z <= 66; z += 7) {
            b.box(-16, 22, z, 16, 22, z + 1, PBB);
        }
        for (int z : new int[]{44, 58}) {
            b.box(-12, 19, z, 12, 19, z, "polished_blackstone_brick_wall");
            for (int x : new int[]{-8, 0, 8}) {
                for (int y = 18; y >= 12; y--) {
                    b.set(x, y, z, "chain[axis=y]");
                }
                b.set(x, 11, z, "lava_cauldron");
            }
        }
        for (int z : new int[]{41, 51, 61}) {
            for (int s : new int[]{-1, 1}) {
                chandelier(b, s * 8, 21, z, 3, "lantern");
            }
        }
        // pasarelas (galerías) laterales con barandilla
        for (int s : new int[]{-1, 1}) {
            int xa = s > 0 ? 13 : -16;
            b.box(xa, 8, 46, xa + 3, 8, 66, DT);
            for (int z = 46; z <= 66; z++) {
                int rx = s > 0 ? 13 : -13;
                b.set(rx, 9, z, "iron_bars");
                b.set(rx, 10, z, z % 3 == 0 ? PBB : "iron_bars");
            }
            for (int z = 46; z <= 66; z += 5) {
                for (int y = 0; y <= 7; y++) {
                    b.set(s > 0 ? 13 : -13, y, z, y % 4 == 3 ? CPB : BLK);
                }
            }
            for (int k = 0; k < 8; k++) {
                for (int w = 0; w < 3; w++) {
                    int x = s > 0 ? 13 + w : -15 + w;
                    int z = 45 - k;
                    // escalera que sube hacia -z... pero la galería está hacia +z: sube hacia +z desde z=38
                }
            }
            // escalera: sube hacia +z desde z=38 hasta la galería (z=46)
            for (int k = 0; k < 8; k++) {
                for (int w = 0; w < 3; w++) {
                    int x = s > 0 ? 13 + w : -15 + w;
                    b.set(x, k, 38 + k, st(PBS, "+z"));
                    for (int yy = 0; yy < k; yy++) {
                        b.set(x, yy, 38 + k, PBB);
                    }
                }
            }
        }
        yunque(b, 10, 38);
        martillo(b, -12, 36);
        // hornos y puestos de trabajo en las paredes
        for (int z = 38; z <= 44; z += 2) {
            b.set(-15, 0, z, "blast_furnace[facing=east,lit=true]");
            b.set(15, 0, z, "blast_furnace[facing=west,lit=true]");
        }
        for (int z = 56; z <= 64; z += 4) {
            b.set(-15, 0, z, "smithing_table");
            b.set(-15, 1, z, "grindstone[face=floor,facing=east]");
            b.set(15, 0, z, "anvil[facing=north]");
            b.set(15, 0, z + 1, "grindstone[face=floor,facing=west]");
        }
        // estandartes rojos
        for (int z : new int[]{44, 52, 60}) {
            for (int s : new int[]{-1, 1}) {
                wallBanner(b, s * 16, 14, z, "red", s > 0 ? "-x" : "+x");
            }
        }
    }

    private static void chimney(StructBp b, int cx, int cz, int r, int top) {
        b.tube(cx, cz, r + 0.5, 1.8, 24, top, PBB);
        b.tube(cx, cz, r + 1.5, 1.2, top - 3, top - 3, CPB);
        b.tube(cx, cz, r + 1.8, 1.2, top, top, GB);
        b.tube(cx, cz, r + 1.0, 1.0, top + 1, top + 1, BLK);
        b.disc(cx, cz, r - 1.0, top - 2, top - 2, "hay_block[axis=y]");
        b.set(cx, top - 1, cz, "campfire[lit=true,signal_fire=true]");
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            int px = cx + (int) Math.round(Math.cos(ang) * (r + 1));
            int pz = cz + (int) Math.round(Math.sin(ang) * (r + 1));
            for (int j = 1; j <= 3; j++) {
                b.set(px, top + j, pz, PBW);
            }
        }
    }

    private static void crucible(StructBp b, int cx, int cz) {
        // foso redondo de lava con borde de obsidiana
        for (int x = -9; x <= 9; x++) {
            for (int z = -9; z <= 9; z++) {
                double d = Math.hypot(x, z);
                if (d <= 8.6) {
                    if (d > 7.0) {
                        b.set(cx + x, -1, cz + z, OBS);
                        b.set(cx + x, 0, cz + z, d > 7.8 ? "air" : (d > 7.4 ? PBL : "air"));
                    } else {
                        b.set(cx + x, -3, cz + z, OBS);
                        b.set(cx + x, -2, cz + z, LAVA);
                        b.set(cx + x, -1, cz + z, LAVA);
                    }
                }
            }
        }
        // isla central con pilar de obsidiana llorosa
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                b.set(cx + x, -1, cz + z, OBS);
            }
        }
        for (int y = 0; y <= 9; y++) {
            b.set(cx, y, cz, y % 3 == 2 ? CRY : OBS);
        }
        b.set(cx, 10, cz, "soul_campfire[lit=true]");
        // cuatro cadenas hasta el techo desde el pilar y calderos
        for (int[] d : new int[][]{{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
            for (int y = 21; y >= 12; y--) {
                b.set(cx + d[0], y, cz + d[1], "chain[axis=y]");
            }
            b.set(cx + d[0], 11, cz + d[1], "lava_cauldron");
        }
        // pasarela hacia el crisol
        for (int z = 56; z <= 58; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, "polished_blackstone");
            }
        }
        for (int z = 56; z <= 63 - 7; z++) {
            b.set(0, -1, z, "polished_blackstone");
        }
    }

    private static void yunque(StructBp b, int cx, int cz) {
        // base
        b.box(cx - 3, 0, cz - 2, cx + 3, 1, cz + 2, BLK);
        b.box(cx - 2, 2, cz - 1, cx + 2, 4, cz + 1, "polished_blackstone");
        // mesa superior larga con cuerno
        b.box(cx - 4, 5, cz - 2, cx + 4, 6, cz + 2, "deepslate");
        b.box(cx - 5, 6, cz - 1, cx - 5, 6, cz + 1, BLK);
        for (int i = 0; i < 3; i++) {
            b.set(cx + 5 + i, 6, cz, i == 2 ? sti(PBS, "-x") : "polished_blackstone");
        }
        // pieza al rojo vivo encima
        b.box(cx - 2, 7, cz, cx + 1, 7, cz, "magma_block");
        b.box(cx - 3, 6, cz, cx - 3, 6, cz, "shroomlight");
        // bandas doradas
        b.box(cx - 3, 5, cz - 2, cx + 3, 5, cz - 2, GB);
        b.box(cx - 3, 5, cz + 2, cx + 3, 5, cz + 2, GB);
    }

    private static void martillo(StructBp b, int cx, int cz) {
        // mango apoyado en el suelo hacia la pared y cabeza enorme
        for (int i = 0; i < 9; i++) {
            b.set(cx, 1 + i / 2, cz + i, "polished_blackstone_brick_wall");
            b.set(cx, 0, cz + i, "polished_blackstone_brick_wall");
        }
        b.box(cx - 2, 1, cz + 9, cx + 2, 3, cz + 12, "polished_deepslate");
        b.box(cx - 2, 2, cz + 9, cx + 2, 2, cz + 12, GB);
        b.set(cx, 0, cz + 11, BLK);
    }

    // ================================================================== talleres en los patios

    private static void yardBuildings(StructBp b) {
        shed(b, -26, 24, -21, 42, 0);
        shed(b, -26, 50, -21, 68, 1);
        shed(b, 21, 24, 26, 42, 2);
        shed(b, 21, 50, 26, 68, 3);
        for (int z = 22; z <= 70; z += 8) {
            for (int s : new int[]{-1, 1}) {
                brazier(b, s * 19, 0, z, PBB, "campfire[lit=true]");
            }
        }
    }

    /**
     * Etapa 268: cofres de la Forja en los talleres del patio: provisiones en el comedor, premios arriba en la armería y en la
     * herrería, y una cerradura arriba en el dormitorio (el cofre del capataz).
     */
    private static void roomChests(StructBp b) {
        vault(b, 26, 0, 40, "north", "ANTE", 2, 0);
        vault(b, -26, 6, 62, "east", "TRIAL", 2, 0);
        vault(b, -26, 6, 38, "east", "TRIAL", 2, 0);
        vault(b, 26, 6, 62, "west", "LOCKED", 2, 3);
    }

    private static void shed(StructBp b, int x0, int z0, int x1, int z1, int kind) {
        b.box(x0 - 1, -1, z0 - 1, x1 + 1, 13, z1 + 1, PBB);
        Room low = new Room(b, x0, z0, x1, z1, -1, 5).carve();
        Room up = new Room(b, x0, z0, x1, z1, 5, 5).carve();
        b.box(x0, 5, z0, x1, 5, z1, "dark_oak_planks");
        b.walls(x0 - 1, -1, z0 - 1, x1 + 1, 1, z1 + 1, BLK);
        b.walls(x0 - 1, 5, z0 - 1, x1 + 1, 5, z1 + 1, CPB);
        ring(b, x0 - 2, z0 - 2, x1 + 2, z1 + 2, 11, PBS, true);
        gableZ(b, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 12, 1, 1, CUS, CU, CUL, true, 1);
        low.baseboard(PBS).torches(6, 2, false);
        up.baseboard(PBS).torches(6, 8, false);
        for (int k = 0; k < 6; k++) {
            b.set(x0, k, z0 + k, st(PBS, "+z"));
            b.air(x0, k + 1, z0 + k, x0, k + 3, z0 + k);
            b.set(x0, k, z0 + k, st(PBS, "+z"));
        }
        b.air(x0, 5, z0 + 1, x0, 5, z0 + 6);
        boolean west = x1 < 0;
        openX(b, (z0 + z1) / 2, 0, west ? x1 : x0, west ? x1 + 1 : x0 - 1, 1, 4, 1, CPB, null);
        for (int z = z0 + 3; z <= z1 - 3; z += 5) {
            openX(b, z, 6, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 3, 0, CPB, "orange_stained_glass_pane");
            openX(b, z, 1, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 3, 0, CPB, "orange_stained_glass_pane");
        }
        int cx = (x0 + x1) / 2;
        int cz = (z0 + z1) / 2;
        switch (kind) {
            case 0 -> {
                // herrería: yunques, hornos y cubas de lava
                for (int z = z0 + 8; z <= z1 - 2; z += 3) {
                    b.set(cx, 0, z, "anvil[facing=east]");
                    b.set(x1, 0, z, "blast_furnace[facing=west,lit=true]");
                    b.set(x0 + (x1 - x0) / 3, 0, z, "lava_cauldron");
                }
                b.set(cx, 6, cz, "smithing_table");
                b.set(x1, 6, cz, "grindstone[face=floor,facing=west]");
                for (int z = z0 + 2; z <= z0 + 6; z++) {
                    b.set(x1, 0, z, "barrel[facing=up]");
                }
            }
            case 1 -> {
                // armería: maniquíes con armadura y estandartes
                for (int z = z0 + 3; z <= z1 - 3; z += 3) {
                    b.set(cx, 0, z, PBW);
                    b.set(cx, 1, z, PBW);
                    b.set(cx, 2, z, "iron_block");
                    b.set(cx, 3, z, "skeleton_skull[rotation=" + (z & 15) + "]");
                    b.set(cx - 1, 2, z, "iron_bars");
                    b.set(cx + 1, 2, z, "iron_bars");
                    wallBanner(b, west ? x0 : x1, 3, z, "red", west ? "+x" : "-x");
                }
            }
            case 2 -> {
                // comedor de los herreros
                for (int z = z0 + 3; z <= z1 - 3; z += 4) {
                    table(b, cx - 1, 0, z, 'x', 3, "dark_oak_slab", "dark_oak_fence");
                    chair(b, cx - 1, 0, z - 1, "+z", "dark_oak_stairs");
                    chair(b, cx + 1, 0, z - 1, "+z", "dark_oak_stairs");
                    chair(b, cx, 0, z + 1, "-z", "dark_oak_stairs");
                    candles(b, cx, 2, z, 2);
                }
                b.set(x1, 0, z1, "furnace[facing=west,lit=true]");
                b.set(x1, 0, z1 - 1, "smoker[facing=west,lit=true]");
                for (int z = z0 + 2; z <= z0 + 5; z++) {
                    b.set(x0, 0, z, "barrel[facing=up]");
                }
            }
            default -> {
                // dormitorio
                for (int z = z0 + 2; z <= z1 - 2; z += 3) {
                    b.set(x1, 0, z, "red_bed[facing=west,part=foot]");
                    b.set(x1 - 1, 0, z, "red_bed[facing=west,part=head]");
                    b.set(x1, 6, z, "black_bed[facing=west,part=foot]");
                    b.set(x1 - 1, 6, z, "black_bed[facing=west,part=head]");
                    b.set(x0 + 1, 0, z, "barrel[facing=up]");
                }
            }
        }
        chandelier(b, cx, 4, cz, 1, "lantern");
        // chimenea humeante del taller
        for (int y = 12; y <= 18; y++) {
            b.set(x1 + (west ? 0 : 1) - (west ? 0 : 0), y, z0 - 1, y == 18 ? GB : PBB);
        }
        b.set(x1 + (west ? 0 : 1), 19, z0 - 1, "campfire[lit=true,signal_fire=false]");
    }

    // ================================================================== hornos profundos bajo tierra

    private static void basement(StructBp b) {
        int fy = -14;
        b.box(-18, fy, 40, 18, fy + 12, 78, "deepslate_bricks");
        Room cav = new Room(b, -15, 43, 15, 75, fy, 10).carve();
        for (int x = -15; x <= 15; x++) {
            for (int z = 43; z <= 75; z++) {
                b.set(x, fy, z, ((x + z) & 1) == 0 ? "polished_blackstone" : DT);
            }
        }
        // lago de lava con pasarelas de obsidiana y rejillas
        for (int x = -9; x <= 9; x++) {
            for (int z = 49; z <= 69; z++) {
                b.set(x, fy, z, OBS);
                b.set(x, fy + 1, z, LAVA);
                b.set(x, fy + 2, z, LAVA);
            }
        }
        for (int z = 49; z <= 69; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, fy + 2, z, x == 0 ? "polished_blackstone" : PBL.concat("[type=bottom]"));
            }
        }
        for (int x = -9; x <= 9; x++) {
            for (int z = 58; z <= 60; z++) {
                b.set(x, fy + 2, z, z == 59 ? "polished_blackstone" : PBL.concat("[type=bottom]"));
            }
        }
        // isla del núcleo
        for (int x = -2; x <= 2; x++) {
            for (int z = 57; z <= 61; z++) {
                b.set(x, fy + 2, z, OBS);
            }
        }
        b.box(-1, fy + 3, 58, 1, fy + 3, 60, CRY);
        // el final de la fundición: la Piedra de Portal: Forja sobre la isla, con el gran tesoro de la Forja (etapa 268)
        b.set(0, fy + 4, 59, M + "portal_stone_forge");
        vault(b, 2, fy + 3, 59, "east", "TREASURE", 2, 0);
        for (int y = fy + 5; y <= fy + 10; y++) {
            b.set(0, y, 59, "chain[axis=y]");
        }
        // columnas en el borde y lámparas
        for (int z = 46; z <= 72; z += 6) {
            for (int s : new int[]{-1, 1}) {
                column(b, s * 12, z, fy + 1, fy + 10, "polished_blackstone_bricks", PBS, CPB);
                hangLantern(b, s * 12, fy + 10, z + 3, 2, "lantern");
            }
        }
        cav.baseboard(PBS).cornice(PBS).torches(6, fy + 4, false);
        // escalera de bajada desde la nave (lado este)
        for (int k = 0; k < 14; k++) {
            int z = 34 + k;
            int y = -1 - k;
            for (int x = 13; x <= 15; x++) {
                b.air(x, y + 1, z, x, Math.min(y + 4, 1), z);
                b.set(x, y, z, st(PBS, "-z"));
                b.set(x, y - 1, z, PBB);
            }
        }
        b.air(13, fy + 1, 48, 15, fy + 4, 52);
        for (int x = 12; x <= 16; x++) {
            b.set(x, 0, 34, "iron_bars");
        }
        for (int z = 34; z <= 48; z++) {
            b.set(12, 0, z, "iron_bars");
            b.set(16, 0, z, "iron_bars");
        }
        for (int x = 12; x <= 16; x++) {
            b.set(x, 0, 48, "iron_bars");
        }
        // goteras de lava y fundiciones del fondo (delante de la celda no hay magma)
        for (int x = -12; x <= 12; x += 6) {
            b.set(x, fy + 9, 75, "shroomlight");
            if (x != -12) {
                b.set(x, fy + 1, 74, "magma_block");
            }
        }
    }

    /**
     * Celda de Hilda (etapa 266), en la pared del fondo de los hornos: barrotes que dan al lago de lava y una puerta de hierro; dentro,
     * un jergón, un yunque roto, un balde, una vela y un farol.
     */
    private static void hildaCell(StructBp b, int fy) {
        b.box(-16, fy, 76, -9, fy + 6, 81, "deepslate_bricks");
        for (int x = -14; x <= -11; x++) {
            for (int z = 77; z <= 79; z++) {
                b.set(x, fy, z, ((x + z) & 1) == 0 ? "cracked_deepslate_bricks" : "deepslate_tiles");
                for (int y = fy + 1; y <= fy + 4; y++) {
                    b.set(x, y, z, "air");
                }
            }
        }
        for (int x = -14; x <= -12; x++) {
            for (int y = fy + 1; y <= fy + 3; y++) {
                b.set(x, y, 76, "iron_bars");
            }
        }
        ironDoor(b, -11, fy + 1, 76, "-z", false);
        b.set(-14, fy + 1, 79, "hay_block[axis=x]");
        b.set(-13, fy + 1, 79, "hay_block[axis=x]");
        b.set(-11, fy + 1, 79, "chipped_anvil[facing=east]");
        b.set(-14, fy + 1, 77, "cauldron");
        b.set(-12, fy + 1, 79, "candle[candles=2,lit=true]"); // al fondo: delante de la puerta no estorba nada
        hangLantern(b, -12, fy + 4, 78, 1, "lantern");
    }

    // ================================================================== alrededores y enemigos (etapa 266)

    /** Fuera del rectángulo, el terreno real baja o sube en rampa suave hasta el nivel de la fundición; el camino sigue recto. */
    private static void surroundings(StructBp b) {
        int rr = 26;
        for (int x = X0 - rr; x <= X1 + rr; x++) {
            for (int z = Z0 - rr; z <= Z1 + rr; z++) {
                double dx = Math.max(Math.max(0, X0 - x), Math.max(0, x - X1));
                double dz = Math.max(Math.max(0, Z0 - z), Math.max(0, z - Z1));
                double e = Math.hypot(dx, dz);
                if (e <= 0) {
                    continue;
                }
                double ramp = 9 + 4 * vnoise(5011, x * 0.08, z * 0.08);
                double w = Math.min(1, e / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 2 && z < Z0;
                if (w >= 0.999 && !road) {
                    continue;
                }
                b.blend(x, z, road ? Math.min(w, 0.5) : w, road ? (hash01(5012, x, z) < 0.4 ? BLK : "gravel") : null, null);
            }
        }
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        return (at == null || at.equals("air")) && (up == null || up.equals("air")) && below != null && !below.equals("air")
                && !below.contains("stairs") && !below.contains("campfire") && !below.equals(LAVA) && !below.contains("bars")
                && !below.contains("wall") && !below.contains("fence") && !below.contains("magma");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind, String tag) {
        if (free(b, x, y, z)) {
            b.mobs.add(new StructBp.Mob(x, y, z, kind, tag));
        } else {
            System.err.println("[Fundicion] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    /** Escorias en los patios y la nave, chisperos en la nave y los hornos, el Fundidor con la llave en la isla, y Hilda presa. */
    private static void mobs(StructBp b) {
        mob(b, -17, 0, 28, "escoria", null);
        mob(b, 27, 0, 46, "escoria", null);
        mob(b, -6, 0, 40, "escoria", null);
        mob(b, 5, 0, 58, "escoria", null);
        mob(b, -12, 0, 52, "chispero", null);
        mob(b, 0, 0, 64, "fundidor", null);
        mob(b, -12, FY + 1, 44, "escoria", null);
        mob(b, 12, FY + 1, 66, "chispero", null);
        mob(b, 0, FY + 3, 57, "fundidor", "llave_celda");
        b.mobs.add(new StructBp.Mob(NPC_X, FY + 1, NPC_Z, "habitante", "hilda"));
    }

    // ================================================================== acabado

    private static void finishing(StructBp b) {
        // rocas de basalto y pinchos de obsidiana alrededor
        for (int i = 0; i < 26; i++) {
            int x = -40 + b.r.nextInt(81);
            int z = -14 + b.r.nextInt(100);
            if (Math.abs(x) < 8 && z < 14) {
                continue;
            }
            if (z > 6 && z < 84 && Math.abs(x) < 38) {
                continue;
            }
            int h = 3 + b.r.nextInt(8);
            for (int y = 0; y < h; y++) {
                double rr = 1.6 * (1 - (double) y / h) + 0.3;
                b.disc(x, z, rr, y, y, b.pick(BAS, BLK, "basalt[axis=y]"));
            }
        }
        // desgaste en manchas: grietas en las piedras negras y enfriado dorado
        weather(b, 17, "polished_blackstone_bricks", "cracked_polished_blackstone_bricks", 3.0, 0.68);
        weather(b, 19, "deepslate_bricks", "cracked_deepslate_bricks", 3.0, 0.7);
    }
}
