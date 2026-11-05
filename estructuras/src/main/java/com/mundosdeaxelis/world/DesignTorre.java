package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Fortaleza en Ruinas: una gran fortaleza de piedra rota por el tiempo (muralla, torres, patio, torreón con gran salón),
 * cubierta de musgo y enredaderas, con una mazmorra intacta bajo tierra (celdas, sala de cadenas, biblioteca, cripta).
 * Primero se construye entera y luego se arruina con cortes naturales. Entrada por z = 0.
 */
final class DesignTorre {
    private DesignTorre() {
    }

    private static final String SB = "stone_bricks";
    private static final String SBS = "stone_brick_stairs";
    private static final String SBL = "stone_brick_slab";
    private static final String SBW = "stone_brick_wall";
    private static final String CSB = "chiseled_stone_bricks";
    private static final String CB = "cobblestone";
    private static final String DB = "deepslate_bricks";
    private static final String DBS = "deepslate_brick_stairs";
    private static final String CDB = "chiseled_deepslate";
    private static final String PD = "polished_deepslate";
    private static final String[] RUB = {"cobblestone", "mossy_cobblestone", "stone_brick_slab[type=bottom]", "mossy_stone_brick_slab[type=bottom]", "stone_bricks", "cobblestone_slab[type=bottom]"};

    static StructBp make() {
        StructBp b = new StructBp(6006);
        b.foundation(-36, -14, 36, 80, "dirt");
        ground(b);
        wall(b);
        towers(b);
        gatehouse(b);
        keep(b);
        yard(b);
        dungeon(b);
        descent(b);
        ruin(b);
        roofs(b);
        finishing(b);
        return b;
    }

    // ================================================================== suelo y camino

    private static void ground(StructBp b) {
        b.box(-36, -1, -14, 36, -1, 80, "grass_block");
        // camino de tierra y adoquines rotos hasta la puerta
        for (int z = -14; z <= 22; z++) {
            for (int x = -3; x <= 3; x++) {
                double n = fbm(3, x, z, 2.5);
                if (n > 0.28) {
                    b.set(x, -1, z, n > 0.62 ? "gravel" : (n > 0.45 ? "mossy_cobblestone" : "dirt_path"));
                }
            }
        }
        // foso seco con borde
        for (int x = -34; x <= 34; x++) {
            for (int z = 10; z <= 78; z++) {
                boolean inRing = !(x >= -31 && x <= 31 && z >= 13 && z <= 75);
                if (inRing && x >= -34 && x <= 34 && z >= 10 && z <= 78 && !(Math.abs(x) <= 4 && z <= 22)) {
                    b.set(x, -1, z, b.chance(0.4) ? "coarse_dirt" : "podzol");
                    b.set(x, -2, z, "dirt");
                }
            }
        }
        // estatuas caídas y columnas rotas junto al camino
        for (int z = -10; z <= 6; z += 8) {
            for (int s : new int[]{-1, 1}) {
                for (int y = 0; y <= 3 + b.r.nextInt(3); y++) {
                    b.set(s * 6, y, z, y == 0 ? SB : CSB);
                }
                b.set(s * 6, 0, z, SB);
            }
        }
    }

    // ================================================================== muralla

    private static void wall(StructBp b) {
        b.box(-30, -1, 16, 30, 13, 72, SB);
        b.air(-27, 0, 19, 27, 13, 69);
        b.walls(-31, -1, 15, 31, 1, 73, CB);
        ring(b, -31, 15, 31, 73, 2, SBS, false);
        b.walls(-30, 7, 16, 30, 7, 72, CSB);
        ring(b, -31, 15, 31, 73, 12, SBS, true);
        b.box(-30, 13, 16, 30, 13, 72, SB);
        b.air(-27, 13, 19, 27, 13, 69);
        crenel(b, -30, 16, 30, 72, 14, 2, SB);
        // contrafuertes
        for (int x = -24; x <= 24; x += 8) {
            if (Math.abs(x) >= 14) {
                buttress(b, x, 15, "-z", 2, 11, 2, SBS, SB);
            }
            buttress(b, x, 73, "+z", 2, 11, 2, SBS, SB);
        }
        for (int z = 24; z <= 64; z += 8) {
            for (int s : new int[]{-1, 1}) {
                buttress(b, s * 31, z, s > 0 ? "+x" : "-x", 2, 11, 2, SBS, SB);
            }
        }
        // aspilleras
        for (int x = -22; x <= 22; x += 4) {
            if (Math.abs(x) >= 16) {
                openZ(b, x, 6, 15, 18, 0, 3, 0, null, null);
            }
            openZ(b, x, 6, 70, 73, 0, 3, 0, null, null);
        }
        for (int z = 26; z <= 62; z += 4) {
            for (int s : new int[]{-1, 1}) {
                openX(b, z, 6, s * 28, s * 31, 0, 3, 0, null, null);
            }
        }
        // escaleras de la muralla
        for (int s : new int[]{-1, 1}) {
            for (int k = 0; k < 13; k++) {
                for (int w = 24; w <= 26; w++) {
                    b.set(s * w, k, 22 + k, st(SBS, "+z"));
                    for (int yy = 0; yy < k; yy++) {
                        b.set(s * w, yy, 22 + k, SB);
                    }
                }
            }
        }
        for (int x = -24; x <= 24; x += 6) {
            b.set(x, 14, 20, "lantern[hanging=false]");
        }
    }

    // ================================================================== torres

    private static void towers(StructBp b) {
        // dos redondas y dos cuadradas en las esquinas
        roundTower(b, -30, 16, 6, 28);
        sqTower(b, 30, 16, 5, 24);
        sqTower(b, -30, 72, 5, 26);
        roundTower(b, 30, 72, 6, 30);
    }

    private static void roundTower(StructBp b, int cx, int cz, int r, int top) {
        b.tube(cx, cz, r + 0.5, 2.2, -1, top, SB);
        for (int y : new int[]{8, 16}) {
            b.tube(cx, cz, r + 0.9, 1.0, y, y, CSB);
        }
        b.tube(cx, cz, r + 1.6, 1.4, top, top, SB);
        b.tube(cx, cz, r + 2.4, 1.2, top + 1, top + 1, SB);
        for (int y = 6; y <= top - 5; y += 8) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (int dd = r - 3; dd <= r + 1; dd++) {
                    for (int dy = 0; dy < 3; dy++) {
                        b.set(cx + d[0] * dd, y + dy, cz + d[1] * dd, "air");
                    }
                }
            }
        }
        spiral(b, cx, cz, 0, top - 2, SBS, CSB, true);
    }

    /** Tejado cónico de una torre redonda (se pone después de la ruina, solo en las torres que se conservan). */
    private static void towerRoof(StructBp b, int cx, int cz, int r, int top) {
        cone(b, cx, cz, r + 2.5, top + 2, 12, "spruce_planks", "spruce_slab", "dark_oak_fence");
    }

    private static void sqTower(StructBp b, int cx, int cz, int r, int top) {
        b.box(cx - r, -1, cz - r, cx + r, top, cz + r, SB);
        b.air(cx - r + 2, 0, cz - r + 2, cx + r - 2, top - 1, cz + r - 2);
        b.walls(cx - r - 1, -1, cz - r - 1, cx + r + 1, 1, cz + r + 1, CB);
        ring(b, cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, 2, SBS, false);
        for (int y : new int[]{9, 18}) {
            b.walls(cx - r - 1, y, cz - r - 1, cx + r + 1, y, cz + r + 1, CSB);
        }
        ring(b, cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, top, SBS, true);
        crenel(b, cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, top + 1, 2, SB);
        for (int y = 6; y <= top - 5; y += 8) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (int dy = 0; dy < 3; dy++) {
                    b.set(cx + d[0] * r, y + dy, cz + d[1] * r, "air");
                    b.set(cx + d[0] * (r - 1), y + dy, cz + d[1] * (r - 1), "air");
                }
            }
        }
        spiral(b, cx, cz, 0, top - 2, SBS, CSB, false);
    }

    private static void gatehouse(StructBp b) {
        for (int s : new int[]{-1, 1}) {
            sqTower(b, s * 11, 18, 5, 24);
        }
        openZ(b, 0, 0, 15, 21, 4, 10, 2, CSB, null);
        for (int x = -4; x <= 4; x++) {
            for (int y = 1; y <= 9; y++) {
                if (Math.abs(x) <= 4 - (y > 6 ? y - 6 : 0)) {
                    if (b.chance(0.55)) {
                        b.set(x, y, 17, "iron_bars");
                    }
                }
            }
        }
        // estandartes rotos sobre la puerta
        for (int s : new int[]{-1, 1}) {
            wallBanner(b, s * 6, 8, 14, "red", "-z");
        }
        hangLantern(b, 0, 10, 18, 1, "lantern");
        // sala de guardia a cada lado
        for (int s : new int[]{-1, 1}) {
            Room g = new Room(b, s > 0 ? 6 : -9, 19, s > 0 ? 9 : -6, 25, -1, 5).carve();
            b.box(s > 0 ? 6 : -9, -1, 19, s > 0 ? 9 : -6, -1, 25, CB);
            g.baseboard(SBS).torches(6, 2, false);
            openX(b, 22, 0, s > 0 ? 5 : -5, s > 0 ? 5 : -5, 1, 3, 1, CSB, null);
            table(b, s > 0 ? 7 : -8, 0, 21, 'z', 3, "spruce_slab", "spruce_fence");
            b.set(s > 0 ? 9 : -9, 0, 24, "barrel[facing=up]");
            b.set(s > 0 ? 9 : -9, 0, 25, "barrel[facing=up]");
        }
    }

    // ================================================================== torreón con gran salón

    private static void keep(StructBp b) {
        b.box(-14, -1, 36, 14, 26, 64, SB);
        Room hall = new Room(b, -12, 38, 12, 62, -1, 24).carve();
        b.walls(-15, -1, 35, 15, 1, 65, CB);
        ring(b, -15, 35, 15, 65, 2, SBS, false);
        for (int y : new int[]{9, 18}) {
            b.walls(-14, y, 36, 14, y, 64, CSB);
        }
        ring(b, -15, 35, 15, 65, 26, SBS, true);
        for (int s : new int[]{-1, 1}) {
            for (int zc : new int[]{35, 65}) {
                roundTower(b, s * 15, zc, 5, 34);
            }
        }
        // portón del torreón
        openZ(b, 0, 0, 35, 38, 3, 8, 2, CSB, null);
        woodDoor(b, "dark_oak", -1, 0, 36, "-z", false);
        woodDoor(b, "dark_oak", 0, 0, 36, "-z", true);
        // ventanas altas
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{42, 50, 58}) {
                openX(b, z, 5, s * 13, s * 15, 1, 8, 2, CSB, null);
                openX(b, z, 20, s * 13, s * 15, 0, 4, 2, CSB, null);
            }
        }
        for (int x : new int[]{-6, 0, 6}) {
            openZ(b, x, 6, 62, 65, 1, 8, 2, CSB, null);
        }

        // ---- interior: gran salón de doble altura con galerías ----
        hall.baseboard(SBS).torches(7, 4, false);
        for (int x = -12; x <= 12; x++) {
            for (int z = 38; z <= 62; z++) {
                b.set(x, -1, z, (Math.abs(x) <= 2) ? "polished_andesite" : ((x + z) & 1) == 0 ? SB : "stone");
            }
        }
        borderLine(b, -12, 38, 12, 62, -1, 1, CSB);
        // galerías de la segunda y tercera planta alrededor de un vacío central
        gallery(b, 9, -12, 38, 12, 62, -8, 41, 8, 59, "spruce_planks");
        gallery(b, 17, -12, 38, 12, 62, -6, 43, 6, 57, "spruce_planks");
        // columnas del vacío central
        for (int x : new int[]{-9, 9}) {
            for (int z : new int[]{40, 50, 60}) {
                column2(b, x == 9 ? 8 : -9, z, 0, 8, SB, SBL);
            }
        }
        // barandillas de las galerías
        for (int x = -8; x <= 8; x++) {
            b.set(x, 10, 41, "dark_oak_fence");
            b.set(x, 10, 59, "dark_oak_fence");
        }
        for (int z = 41; z <= 59; z++) {
            b.set(-8, 10, z, "dark_oak_fence");
            b.set(8, 10, z, "dark_oak_fence");
        }
        // vigas y arañas de hierro
        for (int z : new int[]{44, 52, 60}) {
            b.box(-12, 23, z, 12, 23, z, "stripped_dark_oak_log[axis=x]");
        }
        for (int z : new int[]{44, 52}) {
            chandelier(b, 0, 22, z, 4, "lantern");
        }
        // mesa larga, sillas, trono y chimenea
        table(b, -1, 0, 42, 'z', 12, "dark_oak_slab", "dark_oak_fence");
        for (int z = 43; z <= 52; z += 2) {
            chair(b, -3, 0, z, "+x", "dark_oak_stairs");
            chair(b, 3, 0, z, "-x", "dark_oak_stairs");
            candles(b, 0, 2, z, 1 + (z % 3));
        }
        chair(b, 0, 0, 55, "-z", "spruce_stairs");
        b.box(-1, 0, 61, 1, 0, 61, CB);
        b.air(-2, 1, 62, 2, 4, 62);
        for (int y = 5; y <= 24; y++) {
            b.set(0, y, 62, SB);
        }
        b.box(-2, 0, 62, 2, 0, 62, "stone_bricks");
        b.box(-3, 0, 61, 3, 4, 61, "air");
        b.set(0, 0, 62, "campfire[lit=true]");
        wallBanner(b, -4, 5, 62, "red", "-z");
        wallBanner(b, 4, 5, 62, "red", "-z");
        wallBanner(b, 0, 7, 62, "black", "-z");
        // escudos y armas en la pared
        for (int x = -10; x <= 10; x += 4) {
            if (Math.abs(x) >= 6) {
                wallBanner(b, x, 5, 38, "red", "+z");
            }
        }
        // tablones caídos y telarañas
        for (int i = 0; i < 24; i++) {
            b.set(-11 + b.r.nextInt(23), 22, 39 + b.r.nextInt(23), "cobweb");
        }
    }

    /** Galería: suelo de madera a la altura y, alrededor de un vacío central (vx0..vx1, vz0..vz1). */
    private static void gallery(StructBp b, int y, int x0, int z0, int x1, int z1, int vx0, int vz0, int vx1, int vz1, String plank) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean hole = x >= vx0 && x <= vx1 && z >= vz0 && z <= vz1;
                if (!hole) {
                    b.set(x, y, z, plank);
                }
            }
        }
        // vigas bajo la galería
        for (int x = x0; x <= x1; x += 4) {
            b.set(x, y - 1, z0, "stripped_dark_oak_log[axis=z]");
        }
    }

    // ================================================================== patio

    private static void yard(StructBp b) {
        // pozo
        for (int x = -2; x <= 2; x++) {
            for (int z = 29; z <= 33; z++) {
                boolean edge = Math.abs(x) == 2 || z == 29 || z == 33;
                b.set(x, -1, z, SB);
                if (edge) {
                    b.set(x, 0, z, "mossy_stone_bricks");
                } else {
                    b.set(x, 0, z, "water");
                }
            }
        }
        for (int dx : new int[]{-2, 2}) {
            for (int y = 1; y <= 4; y++) {
                b.set(dx, y, 31, "dark_oak_fence");
            }
        }
        b.box(-2, 5, 31, 2, 5, 31, "dark_oak_planks");
        b.set(0, 4, 31, "chain[axis=y]");
        b.set(0, 3, 31, "lantern[hanging=true]");
        // barracones y establo
        hut(b, -25, 24, -18, 42);
        hut(b, 18, 24, 25, 42);
        hut(b, -25, 50, -18, 64);
        // mesa y barriles en el barracón oeste
        for (int z = 28; z <= 38; z += 4) {
            b.set(-24, 0, z, "red_bed[facing=east,part=foot]");
            b.set(-23, 0, z, "red_bed[facing=east,part=head]");
            b.set(-19, 0, z, "barrel[facing=up]");
        }
        // herrería este
        b.set(24, 0, 28, "blast_furnace[facing=west,lit=true]");
        b.set(24, 0, 31, "anvil[facing=north]");
        b.set(24, 0, 34, "grindstone[face=floor,facing=west]");
        b.set(24, 0, 37, "cauldron");
        // carretas y heno
        for (int i = 0; i < 4; i++) {
            b.set(-8 + i, 0, 44 + (i & 1), "hay_block[axis=x]");
        }
        // estatua rota en el centro
        for (int y = 0; y <= 4; y++) {
            b.set(0, y, 44, y < 2 ? SB : CSB);
        }
    }

    private static void hut(StructBp b, int x0, int z0, int x1, int z1) {
        b.box(x0 - 1, -1, z0 - 1, x1 + 1, 11, z1 + 1, SB);
        Room low = new Room(b, x0, z0, x1, z1, -1, 5).carve();
        Room up = new Room(b, x0, z0, x1, z1, 5, 4).carve();
        b.box(x0, 5, z0, x1, 5, z1, "spruce_planks");
        b.walls(x0 - 1, -1, z0 - 1, x1 + 1, 1, z1 + 1, CB);
        ring(b, x0 - 2, z0 - 2, x1 + 2, z1 + 2, 10, SBS, true);
        gableZ(b, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 11, 1, 1, "spruce_stairs", "spruce_planks", "spruce_slab", true, 1);
        low.baseboard(SBS).torches(6, 2, false);
        up.torches(6, 8, false);
        for (int k = 0; k < 6; k++) {
            b.set(x0, k, z0 + k, st("spruce_stairs", "+z"));
            b.air(x0, k + 1, z0 + k, x0, k + 3, z0 + k);
            b.set(x0, k, z0 + k, st("spruce_stairs", "+z"));
        }
        b.air(x0, 5, z0 + 1, x0, 5, z0 + 6);
        boolean west = x1 < 0;
        openX(b, (z0 + z1) / 2, 0, west ? x1 : x0, west ? x1 + 1 : x0 - 1, 1, 4, 1, CSB, null);
        for (int z = z0 + 3; z <= z1 - 3; z += 5) {
            openX(b, z, 6, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 2, 0, null, null);
            openX(b, z, 1, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 2, 0, null, null);
        }
    }

    // ================================================================== mazmorra (intacta, bajo tierra)

    private static void dungeon(StructBp b) {
        int fy = -13;
        b.box(-28, fy, 27, 28, fy + 7, 72, DB);
        Room chains = new Room(b, -14, 40, 14, 52, fy, 6).carve();
        Room cellsW = new Room(b, -26, 38, -17, 52, fy, 5).carve();
        Room guard = new Room(b, 17, 40, 26, 52, fy, 5).carve();
        Room lib = new Room(b, -10, 29, 10, 38, fy, 6).carve();
        Room corr = new Room(b, -1, 54, 1, 58, fy, 5).carve();
        Room crypt = new Room(b, -8, 60, 8, 70, fy, 6).carve();
        for (Room r : new Room[]{chains, cellsW, guard, lib, corr, crypt}) {
            for (int x = r.x0; x <= r.x1; x++) {
                for (int z = r.z0; z <= r.z1; z++) {
                    b.set(x, fy, z, ((x * 3 + z * 7) % 5 == 0) ? "cracked_deepslate_bricks" : (((x + z) & 1) == 0 ? PD : "deepslate_tiles"));
                }
            }
            r.baseboard(DBS).cornice(DBS).torches(7, fy + 3, true);
        }
        // puertas
        openX(b, 46, fy + 1, -16, -15, 1, 4, 1, CDB, null);
        openX(b, 46, fy + 1, 15, 16, 1, 4, 1, CDB, null);
        openZ(b, 0, fy + 1, 39, 39, 2, 5, 1, CDB, null);
        openZ(b, 0, fy + 1, 53, 53, 1, 4, 1, CDB, null);
        openZ(b, 0, fy + 1, 59, 59, 1, 4, 1, CDB, null);

        // sala de las cadenas: columnas, cadenas colgantes, instrumentos
        for (int x : new int[]{-8, 8}) {
            for (int z : new int[]{43, 49}) {
                column(b, x, z, fy + 1, fy + 6, DB, DBS, CDB);
            }
        }
        for (int x = -6; x <= 6; x += 3) {
            for (int z : new int[]{42, 50}) {
                for (int y = fy + 6; y >= fy + 3; y--) {
                    b.set(x, y, z, "chain[axis=y]");
                }
                b.set(x, fy + 2, z, "iron_bars");
            }
        }
        b.set(-3, fy + 1, 46, "anvil[facing=north]");
        b.set(3, fy + 1, 46, "grindstone[face=floor,facing=north]");
        b.set(0, fy + 1, 44, "cauldron");
        b.set(0, fy + 1, 48, "cauldron");
        for (int i = 0; i < 6; i++) {
            skull(b, -12 + b.r.nextInt(25), fy + 1, 41 + b.r.nextInt(11), b.r.nextInt(16));
        }
        b.box(-1, fy + 1, 45, 1, fy + 1, 47, "air");
        // jaula central de barrotes con un prisionero
        for (int y = fy + 1; y <= fy + 4; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = 45; z <= 47; z++) {
                    if (x != 0 || z != 46) {
                        b.set(x, y, z, (Math.abs(x) == 1 || Math.abs(z - 46) == 1) ? "iron_bars" : "air");
                    }
                }
            }
        }
        b.air(0, fy + 1, 46, 0, fy + 3, 46);
        skull(b, 0, fy + 1, 46, 8);
        b.set(-1, fy + 1, 46, "bone_block[axis=y]");

        // bloque de celdas al oeste: pasillo en x = -22..-21, celdas a ambos lados
        for (int cz = 38; cz <= 50; cz += 4) {
            b.box(-26, fy + 1, cz + 3, -23, fy + 4, cz + 3, DB);
            b.box(-20, fy + 1, cz + 3, -17, fy + 4, cz + 3, DB);
        }
        for (int y = fy + 1; y <= fy + 4; y++) {
            for (int z = 38; z <= 52; z++) {
                b.set(-23, y, z, "iron_bars");
                b.set(-20, y, z, "iron_bars");
            }
        }
        for (int cz = 39; cz <= 51; cz += 4) {
            ironDoor(b, -23, fy + 1, cz + 1, "+x", false);
            ironDoor(b, -20, fy + 1, cz + 1, "-x", false);
            b.set(-25, fy + 1, cz, "hay_block[axis=x]");
            b.set(-18, fy + 1, cz, "hay_block[axis=x]");
            skull(b, -26, fy + 1, cz + 1, 4);
            b.set(-17, fy + 1, cz + 1, "bone_block[axis=y]");
            b.set(-24, fy + 4, cz, "cobweb");
            b.set(-19, fy + 4, cz + 2, "cobweb");
        }
        // sala de guardia/tortura al este
        table(b, 19, fy + 1, 44, 'z', 5, "spruce_slab", "spruce_fence");
        chair(b, 21, fy + 1, 45, "-x", "spruce_stairs");
        chair(b, 21, fy + 1, 47, "-x", "spruce_stairs");
        for (int z = 49; z <= 51; z++) {
            b.set(25, fy + 1, z, "barrel[facing=up]");
        }
        b.set(25, fy + 1, 41, "blast_furnace[facing=west,lit=true]");
        b.set(25, fy + 1, 43, "anvil[facing=east]");
        b.set(25, fy + 1, 45, "smithing_table");
        for (int y = fy + 1; y <= fy + 3; y++) {
            b.set(24, y, 51, "iron_bars");
        }
        b.set(23, fy + 1, 51, "skeleton_skull[rotation=2]");
        hangLantern(b, 21, fy + 5, 42, 1, "soul_lantern");

        // biblioteca olvidada
        for (int x = -9; x <= 9; x += 6) {
            shelves(b, x, fy + 1, 29, 'x', 3, 4);
        }
        shelves(b, -10, fy + 1, 31, 'z', 6, 4);
        shelves(b, 10, fy + 1, 31, 'z', 6, 4);
        table(b, -3, fy + 1, 33, 'x', 7, "spruce_slab", "spruce_fence");
        for (int x = -3; x <= 3; x += 2) {
            chair(b, x, fy + 1, 32, "+z", "spruce_stairs");
            chair(b, x, fy + 1, 35, "-z", "spruce_stairs");
        }
        candles(b, -1, fy + 2, 33, 3);
        candles(b, 2, fy + 2, 33, 2);
        b.set(7, fy + 1, 36, "lectern[facing=west]");
        chandelier(b, 0, fy + 6, 33, 1, "soul_lantern");
        for (int i = 0; i < 14; i++) {
            b.set(-9 + b.r.nextInt(19), fy + 6, 29 + b.r.nextInt(9), "cobweb");
        }

        // cripta de los señores: sarcófagos y altar
        for (int s : new int[]{-1, 1}) {
            for (int z = 62; z <= 68; z += 3) {
                b.air(s * 8, fy + 1, z - 1, s * 11, fy + 4, z + 1);
                sarcophagus(b, s > 0 ? 8 : -11, fy + 1, z, 'x', 3, SB, SBL, SBS);
                skull(b, s * 8, fy + 3, z, s > 0 ? 12 : 4);
            }
            column(b, s * 4, 61, fy + 1, fy + 6, DB, DBS, CDB);
            column(b, s * 4, 69, fy + 1, fy + 6, DB, DBS, CDB);
        }
        b.box(-2, fy + 1, 68, 2, fy + 1, 70, "obsidian");
        b.box(-1, fy + 2, 69, 1, fy + 2, 69, "crying_obsidian");
        b.set(0, fy + 3, 69, "soul_campfire[lit=true]");
        for (int x : new int[]{-3, 3}) {
            b.set(x, fy + 1, 70, "polished_blackstone_brick_wall");
            b.set(x, fy + 2, 70, "polished_blackstone_brick_wall");
            skull(b, x, fy + 3, 70, 8);
            b.set(x, fy + 4, 70, "soul_lantern[hanging=false]");
        }
        chandelier(b, 0, fy + 6, 65, 1, "soul_lantern");
        b.set(0, fy + 5, 61, "cobweb");
        b.set(-2, fy + 5, 63, "cobweb");
        // charco en el pasillo
        b.set(0, fy, 56, "water");
    }

    /** Escalera desde el gran salón hasta las cadenas de la mazmorra (se hace después de la masa de la mazmorra). */
    private static void descent(StructBp b) {
        for (int k = 0; k < 12; k++) {
            int z = 62 - k;
            int y = -1 - k;
            for (int x = -12; x <= -10; x++) {
                b.air(x, y + 1, z, x, y + 3, z);
                b.set(x, y, z, st(SBS, "+z"));
                b.setIfAbsent(x, y - 1, z, DB);
                if (y + 4 <= -2) {
                    b.setIfAbsent(x, y + 4, z, DB); // techo del túnel, bajo el suelo del salón
                }
            }
            for (int yy = y; yy <= y + 3; yy++) {
                if (yy <= -2) {
                    b.setIfAbsent(-13, yy, z, DB);
                    b.setIfAbsent(-9, yy, z, DB);
                }
            }
        }
        // barandilla en el borde del hueco del suelo del salón
        for (int z = 59; z <= 62; z++) {
            b.set(-9, 0, z, SBW);
        }
    }

    // ================================================================== ruina

    /** Hundimiento suave: 1 en el centro (cx,cz), cae hacia 0 con el radio sigma. */
    private static double dip(int x, int z, int cx, int cz, double sigma) {
        double dx = x - cx;
        double dz = z - cz;
        return Math.exp(-(dx * dx + dz * dz) / (2 * sigma * sigma));
    }

    private static void ruin(StructBp b) {
        // En general la fortaleza sigue en pie; hay brechas grandes en puntos concretos y partes casi enteras.
        ruinColumns(b, 77, 6.0, 1, (x, z) -> {
            double bias = 0.22;
            bias += 0.50 * dip(x, z, -30, 72, 7); // torre trasera oeste, casi entera
            bias += 0.50 * dip(x, z, -30, 16, 7); // torre redonda delantera oeste, entera
            bias += 0.45 * dip(x, z, -15, 36, 6); // torres del torreón, lado oeste
            bias += 0.45 * dip(x, z, -15, 65, 6);
            bias += 0.45 * dip(x, z, 30, 72, 8); // torre redonda trasera este, entera
            bias -= 0.70 * dip(x, z, 31, 48, 5); // brecha en el muro este
            bias -= 0.65 * dip(x, z, 9, 73, 6); // brecha en el muro trasero
            bias -= 0.55 * dip(x, z, 11, 18, 6); // torre este de la puerta, hecha pedazos
            bias -= 0.55 * dip(x, z, -19, 15, 4); // brecha en el muro de entrada
            bias -= 0.35 * dip(x, z, 12, 50, 10); // el torreón se derrumbó por el este
            bias -= 0.40 * dip(x, z, 30, 16, 5); // torre de la esquina delantera este
            bias -= 0.40 * dip(x, z, -31, 50, 4);
            return bias;
        }, 0.22, RUB);
        // erosión de los extremos que quedan: quitar trozos sueltos
        b.decay(-32, 1, 14, 32, 14, 76, 0.03);
        // escombros al pie de muros y torres
        rubble(b, -33, 12, 33, 14, -1, 0.14, RUB);
        rubble(b, -33, 74, 33, 78, -1, 0.14, RUB);
        rubble(b, -34, 14, -32, 76, -1, 0.14, RUB);
        rubble(b, 32, 14, 34, 76, -1, 0.14, RUB);
        rubble(b, -26, 20, 26, 70, -1, 0.04, RUB);
        rubble(b, 24, 40, 36, 56, -1, 0.30, RUB); // alrededor de la brecha este
        rubble(b, 2, 70, 16, 80, -1, 0.30, RUB); // alrededor de la brecha trasera
        // columnas caídas en el patio
        for (int i = 0; i < 6; i++) {
            int x = -18 + b.r.nextInt(36);
            int z = 24 + b.r.nextInt(14);
            int len = 4 + b.r.nextInt(4);
            for (int j = 0; j < len; j++) {
                b.setIfAbsent(x + j, 0, z, j == 0 ? CSB : "stone_bricks");
            }
        }
    }

    // ================================================================== tejados que sobreviven

    private static void roofs(StructBp b) {
        // torres que se conservan enteras llevan su tejado cónico (solo si la corona sigue completa)
        int[][] tw = {{30, 72, 6, 30}, {-15, 35, 5, 34}, {-15, 65, 5, 34}, {-30, 16, 6, 28}};
        for (int[] t : tw) {
            if (topIntact(b, t[0], t[1], t[2], t[3])) {
                towerRoof(b, t[0], t[1], t[2], t[3]);
            }
        }
        // el torreón conserva el tejado solo donde los muros siguen enteros (y hasta un borde irregular)
        java.util.Set<Long> before = new java.util.HashSet<>(b.m.keySet());
        gableX(b, -14, 14, 36, 64, 27, 2, 2, "spruce_stairs", "spruce_planks", "spruce_slab", true, 1);
        java.util.List<Long> del = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            if (before.contains(k) || StructBp.ky(k) < 27) {
                continue;
            }
            int x = StructBp.kx(k);
            int z = StructBp.kz(k);
            double edge = 0 + (fbm(31, x, z, 4.0) - 0.5) * 18;
            if (x > edge || !supported(b, x, z)) {
                del.add(k);
            }
        }
        for (long k : del) {
            b.m.remove(k);
        }
    }

    /** El tejado de un punto (x,z) se apoya si el muro del torreón bajo él sigue entero hasta arriba (mira un entorno de 3x3). */
    private static boolean supported(StructBp b, int x, int z) {
        int cx = Math.max(-14, Math.min(14, x));
        int cz = Math.max(36, Math.min(64, z));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!b.m.containsKey(StructBp.key(cx + dx, 26, cz + dz))) {
                    return false;
                }
            }
        }
        return true;
    }

    /** La corona de una torre redonda está entera si hay bloque en 8 puntos de su borde a la altura {@code top}. */
    private static boolean topIntact(StructBp b, int cx, int cz, int r, int top) {
        double d = r + 1.0;
        double q = d * 0.70;
        int[][] o = {{(int) Math.round(d), 0}, {(int) -Math.round(d), 0}, {0, (int) Math.round(d)}, {0, (int) -Math.round(d)},
                {(int) Math.round(q), (int) Math.round(q)}, {(int) -Math.round(q), (int) Math.round(q)},
                {(int) Math.round(q), (int) -Math.round(q)}, {(int) -Math.round(q), (int) -Math.round(q)}};
        for (int[] p : o) {
            if (!b.m.containsKey(StructBp.key(cx + p[0], top, cz + p[1]))) {
                return false;
            }
        }
        return true;
    }

    // ================================================================== acabado

    private static void finishing(StructBp b) {
        // musgo, grietas y piedra rota en manchas grandes
        weather(b, 11, "stone_brick", "mossy_stone_brick", 6.0, 0.60);
        weather(b, 23, "stone_bricks", "cracked_stone_bricks", 3.0, 0.64);
        weather(b, 13, "stone_bricks", "cobblestone", 4.0, 0.72);
        weather(b, 5, "cobblestone", "mossy_cobblestone", 4.0, 0.52);
        hangVines(b, "stone_brick", 2, 0.035, 4);
        // enredaderas pegadas a los muros exteriores (solo donde hay muro detrás)
        for (int i = 0; i < 40; i++) {
            int side = b.r.nextInt(4);
            int yTop = 4 + b.r.nextInt(8);
            int len = 3 + b.r.nextInt(6);
            int a = side < 2 ? -29 + b.r.nextInt(59) : 18 + b.r.nextInt(54);
            for (int j = 0; j < len; j++) {
                int y = yTop - j;
                if (y < 2) {
                    break;
                }
                switch (side) {
                    case 0 -> vineOn(b, a, y, 15, a, y, 16, "south");
                    case 1 -> vineOn(b, a, y, 73, a, y, 72, "north");
                    case 2 -> vineOn(b, -31, y, a, -30, y, a, "east");
                    default -> vineOn(b, 31, y, a, 30, y, a, "west");
                }
            }
        }
        // árboles secos y robles dentro y fuera
        int[][] trees = {{-34, 6}, {34, 8}, {-35, 44}, {-22, 78}, {20, 79}};
        for (int[] t : trees) {
            bigTree(b, t[0], t[1], 12 + b.r.nextInt(6), "oak_log", new String[]{LEAF, LEAF, AZ}, 6);
        }
        deadTree(b, -12, 30, 9);
        deadTree(b, 14, 30, 10);
        deadTree(b, 8, 66, 8);
        deadTree(b, -18, 67, 11);
        groundCover(b, -26, 20, 26, 70, -1, 0.14, "grass", "fern", "dandelion", "poppy", "moss_carpet");
        groundCover(b, -12, -14, 12, 14, -1, 0.07, "grass", "fern", "dandelion");
    }

    /** Pone una enredadera en (x,y,z) pegada al bloque sólido de (wx,wy,wz) por su cara {@code face}. */
    private static void vineOn(StructBp b, int x, int y, int z, int wx, int wy, int wz, String face) {
        String w = b.m.get(StructBp.key(wx, wy, wz));
        if (w == null || w.equals("air") || w.contains("stairs") || w.contains("slab") || w.contains("vine") || w.contains("bars")) {
            return;
        }
        b.setIfAbsent(x, y, z, "vine[" + face + "=true]");
    }

    private static void deadTree(StructBp b, int x, int z, int h) {
        for (int y = 0; y < h; y++) {
            b.set(x, y, z, log("dark_oak_log", 'y'));
        }
        for (int i = 0; i < 5; i++) {
            int dir = b.r.nextInt(4);
            int y = 3 + b.r.nextInt(h - 3);
            int len = 2 + b.r.nextInt(3);
            for (int j = 1; j <= len; j++) {
                int px = x + (dir == 0 ? j : dir == 1 ? -j : 0);
                int pz = z + (dir == 2 ? j : dir == 3 ? -j : 0);
                b.set(px, y + j / 2, pz, log("dark_oak_log", dir < 2 ? 'x' : 'z'));
            }
        }
    }
}
