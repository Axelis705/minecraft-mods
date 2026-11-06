package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Fundición del Titán: una fortaleza de piedra negra con foso de lava, torres con tejados picudos, una gran nave de fundición
 * con río de lava, pasarelas, grúas y yunque gigante, talleres en los patios y hornos profundos bajo tierra.
 * Entrada por z = 0, se extiende hacia z positivo.
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

    static StructBp make() {
        StructBp b = new StructBp(5005);
        b.foundation(-40, -16, 40, 84, BLK);
        ground(b);
        wall(b);
        gatehouse(b);
        cornerTowers(b);
        hall(b);
        yardBuildings(b);
        basement(b);
        finishing(b);
        Linker.link(b, 0, 0, -3, 10, PBB, PBS);
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

    // ================================================================== muralla

    private static void wall(StructBp b) {
        b.box(-30, -1, 16, 30, 16, 74, PBB);
        b.air(-27, 0, 19, 27, 16, 71);
        b.walls(-31, -1, 15, 31, 2, 75, BLK);
        ring(b, -31, 15, 31, 75, 3, PBS, false);
        b.walls(-30, 8, 16, 30, 8, 74, CPB);
        ring(b, -31, 15, 31, 75, 15, PBS, true);
        b.box(-30, 16, 16, 30, 16, 74, DT);
        b.air(-27, 16, 19, 27, 16, 71);
        crenel(b, -30, 16, 30, 74, 17, 2, PBB);
        for (java.util.Map.Entry<Long, String> e : new java.util.ArrayList<>(b.m.entrySet())) {
            long k = e.getKey();
            if (e.getValue().equals(PBB) && StructBp.ky(k) == 18 && !b.m.containsKey(StructBp.key(StructBp.kx(k), 19, StructBp.kz(k)))) {
                b.set(StructBp.kx(k), 19, StructBp.kz(k), PBW);
                b.set(StructBp.kx(k), 20, StructBp.kz(k), PBW);
            }
        }
        // contrafuertes con forma de espolón hacia fuera
        for (int x = -24; x <= 24; x += 8) {
            if (Math.abs(x) >= 16) {
                buttress(b, x, 15, "-z", 3, 13, 3, PBS, PBB);
            }
            buttress(b, x, 75, "+z", 3, 13, 3, PBS, PBB);
        }
        for (int z = 24; z <= 66; z += 8) {
            for (int s : new int[]{-1, 1}) {
                buttress(b, s * 31, z, s > 0 ? "+x" : "-x", 3, 13, 3, PBS, PBB);
            }
        }
        // ventanas de rendija con luz naranja
        for (int x = -22; x <= 22; x += 4) {
            if (Math.abs(x) >= 18) {
                openZ(b, x, 9, 15, 18, 0, 4, 0, CPB, "orange_stained_glass_pane");
            }
            openZ(b, x, 9, 72, 75, 0, 4, 0, CPB, "orange_stained_glass_pane");
        }
        // escaleras a la muralla
        for (int s : new int[]{-1, 1}) {
            for (int k = 0; k < 16; k++) {
                for (int w = 24; w <= 26; w++) {
                    b.set(s * w, k, 22 + k, st(PBS, "+z"));
                    for (int yy = 0; yy < k; yy++) {
                        b.set(s * w, yy, 22 + k, PBB);
                    }
                }
            }
        }
        // antorchas en el paseo
        for (int x = -24; x <= 24; x += 6) {
            b.set(x, 17, 20, "lantern[hanging=false]");
            b.set(x, 17, 70, "lantern[hanging=false]");
        }
    }

    // ================================================================== torres

    private static void cornerTowers(StructBp b) {
        int[][] t = {{-31, 16}, {31, 16}, {-31, 74}, {31, 74}};
        for (int[] c : t) {
            sqTower(b, c[0], c[1], 5, 36, 14);
        }
    }

    private static void sqTower(StructBp b, int cx, int cz, int r, int top, int roofH) {
        b.box(cx - r, -1, cz - r, cx + r, top, cz + r, PBB);
        b.air(cx - r + 2, 0, cz - r + 2, cx + r - 2, top - 1, cz + r - 2);
        b.walls(cx - r - 1, -1, cz - r - 1, cx + r + 1, 2, cz + r + 1, BLK);
        ring(b, cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, 3, PBS, false);
        for (int y : new int[]{10, 20, 30}) {
            b.walls(cx - r - 1, y, cz - r - 1, cx + r + 1, y, cz + r + 1, CPB);
        }
        ring(b, cx - r - 1, cz - r - 1, cx + r + 1, cz + r + 1, top, PBS, true);
        b.box(cx - r - 1, top + 1, cz - r - 1, cx + r + 1, top + 1, cz + r + 1, DT);
        hipRoof(b, cx - r, cz - r, cx + r, cz + r, top + 2, 2, CUS, CU, CUL, 2);
        int peak = top + 2 + r + 2;
        for (int i = 0; i < 9; i++) {
            b.set(cx, peak + 1 + i, cz, i < 6 ? "chain[axis=y]" : (i == 6 ? GB : (i == 7 ? "lantern[hanging=false]" : "lightning_rod[facing=up]")));
        }
        // pilares en las esquinas
        for (int dx : new int[]{-r - 1, r + 1}) {
            for (int dz : new int[]{-r - 1, r + 1}) {
                for (int y = 4; y <= top - 1; y++) {
                    b.set(cx + dx, y, cz + dz, y % 10 == 0 ? CPB : BLK);
                }
                b.set(cx + dx, top, cz + dz, BLK);
                for (int j = 1; j <= 4; j++) {
                    b.set(cx + dx, top + j, cz + dz, PBW);
                }
            }
        }
        // ventanas naranjas
        for (int y : new int[]{6, 16, 26}) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int wx = cx + d[0] * r;
                int wz = cz + d[1] * r;
                for (int dy = 0; dy < 4; dy++) {
                    b.set(wx, y + dy, wz, "orange_stained_glass_pane");
                    b.set(wx - d[0], y + dy, wz - d[1], "air");
                }
            }
        }
        spiral(b, cx, cz, 0, top - 2, PBS, CPB, true);
        for (int y : new int[]{8, 18, 28}) {
            hangLantern(b, cx, y + 6, cz, 0, "lantern");
        }
    }

    private static void gatehouse(StructBp b) {
        for (int s : new int[]{-1, 1}) {
            sqTower(b, s * 12, 17, 5, 32, 12);
        }
        openZ(b, 0, 0, 15, 20, 4, 11, 2, CPB, null);
        for (int x = -4; x <= 4; x++) {
            for (int y = 6; y <= 10; y++) { // rastrillo levantado: se pasa por debajo
                if (Math.abs(x) <= 4 - (y > 7 ? y - 7 : 0)) {
                    b.set(x, y, 17, "iron_bars");
                }
            }
        }
        for (int s : new int[]{-1, 1}) {
            for (int y = 10; y <= 15; y++) {
                b.set(s * 3, y, 15, "chain[axis=y]");
            }
        }
        // cara de ogro sobre la puerta: ojos de fuego y dientes
        for (int x = -4; x <= 4; x++) {
            b.set(x, 14, 15, CPB);
        }
        for (int s : new int[]{-1, 1}) {
            b.box(s * 2, 16, 15, s * 2 + s, 17, 15, "shroomlight");
            b.set(s * 2, 18, 15, st(PBS, s > 0 ? "-x" : "+x"));
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, 15, 15, (x & 1) == 0 ? sti(PBS, "+z") : PBB);
        }
        hangLantern(b, 0, 11, 17, 2, "lantern");
        // pasarela de la puerta (puente levadizo recogido) con cadenas largas
        for (int s : new int[]{-1, 1}) {
            for (int y = 2; y <= 9; y++) {
                b.set(s * 4, y, 14, "chain[axis=y]");
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
        // tejado oscuro a dos aguas con cumbrera a lo largo de Z
        gableZ(b, -18, 18, 32, 72, 25, 2, 2, DTS, DT, DTL, true, 1);
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
        b.set(0, fy + 4, 59, "soul_campfire[lit=true]");
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
        // goteras de lava y fundiciones del fondo
        for (int x = -12; x <= 12; x += 6) {
            b.set(x, fy + 9, 75, "shroomlight");
            b.set(x, fy + 1, 74, "magma_block");
        }
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
