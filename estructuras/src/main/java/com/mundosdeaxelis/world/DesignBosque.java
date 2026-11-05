package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Templo del Bosque: un gran templo escalonado cubierto de musgo, con avenida de estatuas, dos terrazas,
 * un santuario con un árbol gigante que sale por el techo, salas por dentro (biblioteca, taller, armería, comedor, celdas...)
 * y una cripta bajo tierra. Entrada por z = 0, se extiende hacia z positivo.
 */
final class DesignBosque {
    private DesignBosque() {
    }

    private static final String SB = "stone_bricks";
    private static final String SBS = "stone_brick_stairs";
    private static final String SBL = "stone_brick_slab";
    private static final String SBW = "stone_brick_wall";
    private static final String CSB = "chiseled_stone_bricks";
    private static final String MSB = "mossy_stone_bricks";
    private static final String MSBS = "mossy_stone_brick_stairs";
    private static final String MSBW = "mossy_stone_brick_wall";
    private static final String PA = "polished_andesite";
    private static final String[] CROWN = {LEAF, LEAF, AZ, FAZ};

    private record Face(String out, int c, int a0, int a1) {
        int x(int a, int off) {
            return switch (out) {
                case "-x" -> c - off;
                case "+x" -> c + off;
                default -> a;
            };
        }

        int z(int a, int off) {
            return switch (out) {
                case "-z" -> c - off;
                case "+z" -> c + off;
                default -> a;
            };
        }
    }

    static StructBp make() {
        StructBp b = new StructBp(4004);
        b.foundation(-34, 6, 34, 84, SB);
        massing(b);
        terraceDetail(b, -30, 22, 30, 80, -1, 9);
        terraceDetail(b, -22, 34, 22, 74, 10, 19);
        parapets(b);
        flights(b);
        avenue(b);
        gates(b);
        interiorT1(b);
        interiorT2(b);
        basement(b);
        shrine(b);
        finishing(b);
        return b;
    }

    // ================================================================== masas y fachadas

    private static void massing(StructBp b) {
        b.box(-30, -1, 22, 30, 9, 80, SB);
        b.box(-22, 10, 34, 22, 19, 74, SB);
    }

    private static void terraceDetail(StructBp b, int x0, int z0, int x1, int z1, int yb, int yt) {
        // zócalo, banda y cornisa que rodean toda la terraza
        b.walls(x0 - 1, yb, z0 - 1, x1 + 1, yb + 1, z1 + 1, SB);
        ring(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, yb + 2, SBS, false);
        int mid = (yb + yt) / 2;
        b.walls(x0 - 1, mid, z0 - 1, x1 + 1, mid, z1 + 1, CSB);
        ring(b, x0 - 1, z0 - 1, x1 + 1, z1 + 1, yt - 1, SBS, true);
        b.walls(x0 - 1, yt, z0 - 1, x1 + 1, yt, z1 + 1, SB);
        Face[] faces = {new Face("-z", z0, x0, x1), new Face("+z", z1, x0, x1), new Face("-x", x0, z0, z1), new Face("+x", x1, z0, z1)};
        for (Face f : faces) {
            for (int a = f.a0() + 4; a <= f.a1() - 4; a += 6) {
                boolean front = f.out().equals("-z");
                if (front && (Math.abs(a) <= 7 || Math.abs(Math.abs(a) - 14) <= 3)) {
                    continue;
                }
                pilaster(b, f, a, yb, yt);
                // ranura luminosa entre pilastras
                int a2 = a + 3;
                if (a2 <= f.a1() - 3 && !(front && (Math.abs(a2) <= 7 || Math.abs(Math.abs(a2) - 14) <= 3))) {
                    for (int y = yb + 4; y <= yt - 4; y++) {
                        b.set(f.x(a2, -1), y, f.z(a2, -1), "shroomlight");
                        b.set(f.x(a2, 0), y, f.z(a2, 0), "iron_bars");
                    }
                }
            }
        }
    }

    private static void pilaster(StructBp b, Face f, int a, int yb, int yt) {
        for (int y = yb + 2; y <= yt - 2; y++) {
            b.set(f.x(a, 1), y, f.z(a, 1), y == yb + 2 ? CSB : SB);
        }
        b.set(f.x(a, 1), yt - 1, f.z(a, 1), sti(SBS, opp(f.out())));
    }

    private static void parapets(StructBp b) {
        parapet(b, -30, 22, 30, 80, 10, 4);
        parapet(b, -22, 34, 22, 74, 20, 4);
    }

    private static void parapet(StructBp b, int x0, int z0, int x1, int z1, int y, int gap) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (!(x == x0 || x == x1 || z == z0 || z == z1)) {
                    continue;
                }
                if (z == z0 && Math.abs(x) <= gap + 2) {
                    continue; // hueco donde llega la escalera
                }
                boolean post = (x - x0) % 8 == 0 && (z == z0 || z == z1) || (z - z0) % 8 == 0 && (x == x0 || x == x1);
                b.set(x, y, z, MSBW);
                if (post) {
                    b.set(x, y, z, SB);
                    b.set(x, y + 1, z, CSB);
                    b.set(x, y + 2, z, "lantern");
                }
            }
        }
    }

    // ================================================================== escalinatas y avenida

    private static void flights(StructBp b) {
        flight(b, 13, -1);
        flight(b, 25, 9);
    }

    /** Escalinata de 9 peldaños (ancho 9) con mejillas laterales; sube hacia +z desde z0; base = nivel del suelo de abajo. */
    private static void flight(StructBp b, int z0, int base) {
        for (int k = 0; k < 9; k++) {
            int z = z0 + k;
            int y = base + 1 + k;
            for (int x = -4; x <= 4; x++) {
                b.set(x, y, z, st(SBS, "+z"));
                for (int yy = base + 1; yy < y; yy++) {
                    b.set(x, yy, z, SB);
                }
            }
            for (int s : new int[]{-1, 1}) {
                for (int w = 5; w <= 6; w++) {
                    for (int yy = base; yy <= y; yy++) {
                        b.set(s * w, yy, z, SB);
                    }
                    b.set(s * w, y + 1, z, w == 6 ? slb(SBL) : CSB);
                }
            }
        }
        for (int s : new int[]{-1, 1}) {
            brazier(b, s * 5, base + 10, z0 + 8, SB, "campfire[lit=true]");
            b.set(s * 5, base + 10, z0 + 8, CSB);
        }
    }

    private static void avenue(StructBp b) {
        // suelo de la avenida y la plaza
        for (int z = 0; z <= 21; z++) {
            for (int x = -7; x <= 7; x++) {
                String s = (Math.abs(x) <= 4) ? (z % 4 == 0 ? CSB : PA) : (Math.abs(x) == 5 ? CSB : SB);
                if (z >= 13 && Math.abs(x) <= 6) {
                    continue; // aquí está la escalinata
                }
                b.set(x, -1, z, s);
            }
        }
        checker(b, -14, 8, 14, 12, -1, SB, MSB);
        // muritos y farolas a los lados de la avenida
        for (int s : new int[]{-1, 1}) {
            for (int z = 0; z <= 12; z++) {
                b.set(s * 8, 0, z, MSBW);
                if (z % 4 == 0) {
                    b.set(s * 8, 0, z, SB);
                    b.set(s * 8, 1, z, SB);
                    b.set(s * 8, 2, z, CSB);
                    b.set(s * 8, 3, z, "lantern");
                }
            }
            // guardianes de piedra
            knight(b, s * 10, 0, 4, "+z", SB, SBS, SBW, CSB, PA);
            knight(b, s * 10, 0, 16, "+z", SB, SBS, SBW, CSB, PA);
            // estanques
            pond(b, s * 16 - 5, 3, s * 16 + 5, 20);
        }
        // arco-pórtico de entrada
        for (int s : new int[]{-1, 1}) {
            column2(b, s * 7 - (s > 0 ? 0 : 1), 0, 0, 11, SB, SBL);
            for (int y = 12; y <= 13; y++) {
                b.box(s * 7 - (s > 0 ? 0 : 1), y, 0, s * 7 + (s > 0 ? 1 : 0), y, 1, y == 12 ? CSB : SB);
            }
        }
        b.box(-8, 12, 0, 8, 12, 1, CSB);
        b.box(-8, 13, 0, 8, 13, 1, SB);
        for (int x = -8; x <= 8; x++) {
            b.set(x, 14, 0, (x & 1) == 0 ? SB : "air");
            b.set(x, 14, 1, (x & 1) == 0 ? SB : "air");
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, 11, 0, "air");
        }
        hangLantern(b, 0, 11, 0, 1, "lantern");
    }

    private static void pond(StructBp b, int x0, int z0, int x1, int z1) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                b.set(x, -1, z, SB);
                if (edge) {
                    b.set(x, 0, z, MSB);
                    b.set(x, 1, z, slb(SBL));
                } else {
                    b.set(x, 0, z, "water");
                    if (b.chance(0.22)) {
                        b.set(x, 1, z, "lily_pad");
                    }
                }
            }
        }
        // isla central con arbusto
        int cx = (x0 + x1) / 2;
        int cz = (z0 + z1) / 2;
        b.box(cx - 1, 0, cz - 1, cx + 1, 0, cz + 1, "mossy_cobblestone");
        b.set(cx, 1, cz, "flowering_azalea");
        b.set(cx + 1, 1, cz, "fern");
        b.set(cx - 1, 1, cz + 1, "azalea");
    }

    // ================================================================== accesos

    private static void gates(StructBp b) {
        // T1: dos puertas laterales a los lados de la escalinata
        for (int s : new int[]{-1, 1}) {
            openZ(b, s * 14, 0, 21, 24, 2, 7, 2, CSB, null);
            // T2: igual en su cara
            openZ(b, s * 13, 10, 33, 36, 2, 6, 2, CSB, null);
        }
    }

    // ================================================================== interior de la terraza baja (T1)

    private static void interiorT1(StructBp b) {
        int fy = -1;
        int h = 8;
        Room atrio = new Room(b, -27, 25, 27, 31, fy, h).carve();
        Room nave = new Room(b, -10, 33, 10, 66, fy, h).carve();
        Room tras = new Room(b, -27, 68, 27, 75, fy, h).carve();
        int[][] sides = {{33, 43}, {45, 55}, {57, 66}};
        Room[] left = new Room[3];
        Room[] right = new Room[3];
        for (int i = 0; i < 3; i++) {
            left[i] = new Room(b, -26, sides[i][0], -12, sides[i][1], fy, h).carve();
            right[i] = new Room(b, 12, sides[i][0], 26, sides[i][1], fy, h).carve();
        }
        // puertas y arcos entre salas
        openZ(b, 0, 0, 32, 32, 4, 7, 1, CSB, null);          // atrio -> nave
        openZ(b, 0, 0, 67, 67, 3, 7, 1, CSB, null);          // nave -> cámara trasera
        for (int s : new int[]{-1, 1}) {
            openZ(b, s * 19, 0, 32, 32, 2, 5, 1, CSB, null);   // atrio -> sala 1
            openZ(b, s * 19, 0, 44, 44, 2, 5, 1, CSB, null);   // sala 1 -> 2
            openZ(b, s * 19, 0, 56, 56, 2, 5, 1, CSB, null);   // sala 2 -> 3
            openZ(b, s * 19, 0, 67, 67, 2, 5, 1, CSB, null);   // sala 3 -> trasera
            for (int z : new int[]{38, 50, 61}) {
                openX(b, z, 0, s * 11, s * 11, 2, 6, 1, CSB, null); // nave -> salas laterales
            }
        }
        // suelo, rodapié, cornisa, luces
        for (Room r : new Room[]{atrio, nave, tras}) {
            r.baseboard(SBS).cornice(SBS).torches(7, 3, false);
        }
        checker(b, -27, 25, 27, 31, fy, SB, MSB);
        borderLine(b, -27, 25, 27, 31, fy, 1, CSB);
        // atrio: columnas y guardianes
        for (int x : new int[]{-24, -18, -8, -4, 4, 8, 18, 24}) {
            column(b, x, 28, 0, 7, SB, SBS, CSB);
        }
        chandelier(b, -14, 7, 28, 2, "lantern");
        chandelier(b, 14, 7, 28, 2, "lantern");
        chandelier(b, 0, 7, 28, 2, "lantern");
        wallBanner(b, -2, 5, 31, "lime", "-z");
        wallBanner(b, 2, 5, 31, "lime", "-z");
        wallBanner(b, -9, 5, 31, "green", "-z");
        wallBanner(b, 9, 5, 31, "green", "-z");

        // nave
        for (int x = -10; x <= 10; x++) {
            for (int z = 33; z <= 66; z++) {
                b.set(x, fy, z, Math.abs(x) <= 2 ? PA : SB);
            }
        }
        for (int z = 34; z <= 65; z += 4) {
            b.set(0, fy, z, CSB);
        }
        for (int s : new int[]{-1, 1}) {
            for (int z = 36; z <= 60; z += 6) {
                column2(b, s > 0 ? 5 : -6, z, 0, 7, SB, SBL);
                // nervio que cruza la bóveda
            }
        }
        for (int z = 36; z <= 60; z += 6) {
            b.box(-10, 7, z, 10, 7, z + 1, CSB);
        }
        for (int z : new int[]{39, 51, 63}) {
            chandelier(b, 0, 6, z, 2, "lantern");
        }
        for (int z : new int[]{39, 51}) {
            for (int s : new int[]{-1, 1}) {
                knight(b, s * 9, 0, z, s > 0 ? "-x" : "+x", SB, SBS, SBW, CSB, PA);
            }
        }
        // altar al fondo de la nave
        for (int x = -6; x <= 6; x++) {
            for (int z = 58; z <= 65; z++) {
                b.set(x, fy, z, MSB);
            }
        }
        b.box(-6, 0, 58, 6, 0, 65, SB);
        stairway(b, -6, 0, 58, "+z", 1, 1, SBS, SB, false);
        b.box(-3, 1, 61, 3, 1, 63, CSB);
        b.box(-2, 2, 62, 2, 2, 62, MSB);
        for (int x = -2; x <= 2; x += 2) {
            candles(b, x, 3, 62, 3);
        }
        b.set(0, 2, 63, "shroomlight");
        b.set(0, 3, 63, "shroomlight");

        // salas laterales
        furnishLibrary(b, left[0], -1);
        furnishWorkshop(b, left[1], -1);
        furnishChapel(b, left[2], -1);
        furnishArmory(b, right[0], 1);
        furnishDining(b, right[1], 1);
        furnishCells(b, right[2], 1);
        for (Room r : new Room[]{left[0], left[1], left[2], right[0], right[1], right[2]}) {
            r.baseboard(SBS).torches(6, 3, false);
        }

        // cámara trasera: escaleras hacia arriba a ambos lados y escalera hacia la cripta
        tras.floor(SB);
        borderLine(b, -27, 68, 27, 75, fy, 1, CSB);
        for (int s : new int[]{-1, 1}) {
            upStairs(b, s);
            chandelier(b, s * 12, 7, 71, 2, "lantern");
        }
        for (int x = -4; x <= 4; x += 8) {
            column(b, x, 72, 0, 7, SB, SBS, CSB);
        }
        // pozo hacia la cripta (a lo largo de -z, bajando hacia la nave)
        for (int k = 0; k < 10; k++) {
            int z = 75 - k;
            int y = -1 - k;
            for (int x = -1; x <= 1; x++) {
                for (int yy = y + 1; yy <= Math.max(y + 4, 0); yy++) {
                    if (z >= 68 || yy <= -2) {
                        b.set(x, yy, z, "air");
                    }
                }
                b.set(x, y, z, st(SBS, "+z"));
                b.set(x, y - 1, z, SB);
            }
        }
        // barandilla del pozo
        for (int z = 69; z <= 74; z++) {
            b.set(-2, 0, z, MSBW);
            b.set(2, 0, z, MSBW);
        }
        b.set(-2, 1, 69, "lantern");
        b.set(2, 1, 69, "lantern");
    }

    /** Escalera recta hacia la sala superior (T2): sube a lo largo de x, junto al fondo de la cámara trasera. */
    private static void upStairs(StructBp b, int s) {
        // el paso sube desde x = s*26 hacia el centro hasta y = 9
        String up = s < 0 ? "+x" : "-x";
        for (int k = 0; k < 10; k++) {
            int x = s * 26 - s * k;
            int y = k;
            for (int z = 70; z <= 72; z++) {
                b.set(x, y, z, st(SBS, up));
                for (int yy = 0; yy < y; yy++) {
                    b.set(x, yy, z, SB);
                }
            }
        }
        // hueco en el techo (y = 8..9) sobre los últimos pasos y despeje hasta la sala de arriba
        for (int x = s * 20; s > 0 ? x >= s * 20 - 3 : x <= s * 20 + 3; x -= s) {
            for (int z = 70; z <= 72; z++) {
                b.air(x, 8, z, x, 9, z);
            }
        }
        // barandilla a los lados del hueco, en la sala de arriba (y = 10)
        for (int x = Math.min(s * 17, s * 21); x <= Math.max(s * 17, s * 21); x++) {
            b.set(x, 10, 69, MSBW);
            b.set(x, 10, 73, MSBW);
        }
    }

    // ------------------------------------------------------------------ salas temáticas de T1

    private static void furnishLibrary(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, "spruce_planks");
        shelves(b, r.x0, 0, r.z0 + 1, 'z', r.z1 - r.z0 - 1, 5);
        shelves(b, r.x0 + 1, 0, r.z0, 'x', 4, 4);
        shelves(b, r.x0 + 1, 0, r.z1, 'x', 4, 4);
        shelves(b, r.x1 - 5, 0, r.z0, 'x', 4, 4);
        b.air(r.x0 + 1, 1, r.z0 + 1, r.x0 + 1, 4, r.z1 - 1);
        shelves(b, r.x0 + 1, 0, r.z0 + 1, 'z', r.z1 - r.z0 - 1, 0);
        // mesas de lectura
        int cx = r.cx() + 2;
        table(b, cx - 2, 0, r.cz() - 2, 'x', 5, "spruce_slab", "spruce_fence");
        table(b, cx - 2, 0, r.cz() + 2, 'x', 5, "spruce_slab", "spruce_fence");
        for (int x = cx - 2; x <= cx + 2; x += 2) {
            chair(b, x, 0, r.cz() - 3, "+z", "spruce_stairs");
            chair(b, x, 0, r.cz() + 1, "-z", "spruce_stairs");
            chair(b, x, 0, r.cz() + 1 + 2 + 1, "-z", "spruce_stairs");
            b.set(x, 2, r.cz() - 2, "air");
        }
        candles(b, cx, 2, r.cz() - 2, 2);
        candles(b, cx + 2, 2, r.cz() + 2, 3);
        b.set(r.x1 - 1, 0, r.z0 + 2, "lectern[facing=west]");
        chandelier(b, cx, 7, r.cz(), 2, "lantern");
        b.set(r.x0 + 1, 7, r.z0 + 1, "cobweb");
        b.set(r.x1, 7, r.z1, "cobweb");
        b.set(r.x1, 6, r.z0, "cobweb");
    }

    private static void furnishWorkshop(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, "moss_block");
        for (int i = 0; i < 24; i++) {
            int x = r.x0 + 1 + b.r.nextInt(r.x1 - r.x0 - 1);
            int z = r.z0 + 1 + b.r.nextInt(r.z1 - r.z0 - 1);
            b.set(x, 0, z, b.pick("fern", "azalea", "flowering_azalea", "moss_carpet", "poppy", "blue_orchid", "lily_of_the_valley"));
        }
        // mesas de trabajo
        for (int x = r.x0 + 1; x <= r.x0 + 4; x++) {
            b.set(x, 0, r.z0, "oak_planks");
        }
        b.set(r.x0, 0, r.z0 + 1, "cauldron");
        b.set(r.x0, 0, r.z0 + 3, "cauldron");
        b.set(r.x0, 0, r.z1 - 1, "composter");
        b.set(r.x0 + 1, 1, r.z0, "brewing_stand");
        b.set(r.x0 + 3, 1, r.z0, "brewing_stand");
        b.set(r.x0 + 2, 1, r.z0, "potted_fern");
        b.set(r.x1, 0, r.z0 + 1, "barrel[facing=up]");
        b.set(r.x1, 0, r.z0 + 2, "barrel[facing=up]");
        b.set(r.x1, 1, r.z0 + 1, "barrel[facing=up]");
        b.set(r.x1, 0, r.z1, "hay_block[axis=y]");
        b.set(r.x1 - 1, 0, r.z1, "hay_block[axis=y]");
        // árbol pequeño en el centro y colgantes
        int cx = r.cx();
        int cz = r.cz();
        for (int y = 0; y <= 3; y++) {
            b.set(cx, y, cz, log("oak_log", 'y'));
        }
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int y = 3; y <= 5; y++) {
                    if (x * x + z * z + (y - 4) * (y - 4) * 2 <= 5 && !(x == 0 && z == 0 && y == 3)) {
                        b.setIfAbsent(cx + x, y, cz + z, y == 3 ? FAZ : AZ);
                    }
                }
            }
        }
        for (int x = r.x0 + 2; x <= r.x1 - 2; x += 3) {
            b.set(x, 7, r.z1, "vine[north=true]");
            for (int y = 7; y >= 5; y--) {
                b.set(x, y, r.z1, "vine[north=true]");
            }
        }
        hangLantern(b, r.x0 + 3, 7, r.cz(), 2, "lantern");
        hangLantern(b, r.x1 - 3, 7, r.cz(), 2, "lantern");
    }

    private static void furnishChapel(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, MSB);
        borderLine(b, r.x0, r.z0, r.x1, r.z1, r.fy, 1, CSB);
        int cx = r.cx();
        // altar al fondo
        b.box(cx - 2, 0, r.z1 - 2, cx + 2, 0, r.z1 - 1, CSB);
        b.box(cx - 1, 1, r.z1 - 1, cx + 1, 1, r.z1 - 1, MSB);
        b.set(cx, 2, r.z1 - 1, "shroomlight");
        candles(b, cx - 2, 1, r.z1 - 2, 3);
        candles(b, cx + 2, 1, r.z1 - 2, 3);
        wallBanner(b, cx - 3, 4, r.z1, "green", "-z");
        wallBanner(b, cx + 3, 4, r.z1, "green", "-z");
        wallBanner(b, cx, 5, r.z1, "lime", "-z");
        // bancos
        for (int z = r.z0 + 2; z <= r.z0 + 7; z += 2) {
            for (int x : new int[]{cx - 4, cx + 3}) {
                for (int i = 0; i < 2; i++) {
                    chair(b, x + i, 0, z, "+z", "spruce_stairs");
                }
            }
        }
        for (int s2 : new int[]{-1, 1}) {
            brazier(b, cx + s2 * 5, 0, r.z1 - 1, SB, "soul_campfire[lit=true]");
        }
        chandelier(b, cx, 7, r.cz(), 2, "soul_lantern");
    }

    private static void furnishArmory(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, "polished_blackstone_bricks");
        // paneles con armas (barrotes y estandartes) en la pared del fondo
        for (int x = r.x0 + 1; x <= r.x1 - 1; x += 3) {
            wallBanner(b, x, 4, r.z1, "red", "-z");
            for (int y = 1; y <= 3; y++) {
                b.set(x + 1, y, r.z1, "iron_bars");
            }
        }
        for (int x = r.x0 + 1; x <= r.x0 + 6; x += 2) {
            b.set(x, 0, r.z0, "barrel[facing=up]");
        }
        b.set(r.x1, 0, r.z0, "blast_furnace[facing=west,lit=true]");
        b.set(r.x1, 0, r.z0 + 1, "anvil[facing=north]");
        b.set(r.x1, 0, r.z0 + 3, "grindstone[face=floor,facing=east]");
        b.set(r.x1, 0, r.z0 + 5, "smithing_table");
        b.set(r.x1, 0, r.z0 + 7, "cauldron");
        // maniquíes: pilares con casco
        for (int x = r.x0 + 3; x <= r.x1 - 3; x += 4) {
            b.set(x, 0, r.cz(), "polished_blackstone_brick_wall");
            b.set(x, 1, r.cz(), "polished_blackstone_brick_wall");
            b.set(x, 2, r.cz(), "iron_block");
            b.set(x, 3, r.cz(), "skeleton_skull[rotation=" + (x & 15) + "]");
            b.set(x - 1, 2, r.cz(), "iron_bars");
            b.set(x + 1, 2, r.cz(), "iron_bars");
        }
        chandelier(b, r.cx(), 7, r.cz(), 2, "lantern");
        b.set(r.x0, 7, r.z0, "cobweb");
    }

    private static void furnishDining(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, "dark_oak_planks");
        checker(b, r.x0, r.z0, r.x1, r.z1, r.fy, "dark_oak_planks", "spruce_planks");
        int cx = r.cx();
        for (int dz : new int[]{-2, 2}) {
            table(b, cx - 4, 0, r.cz() + dz, 'x', 9, "dark_oak_slab", "dark_oak_fence");
            for (int x = cx - 4; x <= cx + 4; x += 2) {
                chair(b, x, 0, r.cz() + dz - 1, "+z", "dark_oak_stairs");
                chair(b, x, 0, r.cz() + dz + 1, "-z", "dark_oak_stairs");
                candles(b, x, 2, r.cz() + dz, 1 + (x & 3) % 3);
            }
        }
        for (int z = r.z0; z <= r.z0 + 3; z++) {
            b.set(r.x1, 0, z, "barrel[facing=up]");
            b.set(r.x1, 1, z, z % 2 == 0 ? "barrel[facing=up]" : "air");
        }
        b.set(r.x0, 0, r.z1, "furnace[facing=east,lit=true]");
        b.set(r.x0, 0, r.z1 - 1, "smoker[facing=east,lit=true]");
        b.set(r.x0, 0, r.z1 - 2, "crafting_table");
        chandelier(b, cx - 3, 7, r.cz(), 2, "lantern");
        chandelier(b, cx + 3, 7, r.cz(), 2, "lantern");
    }

    private static void furnishCells(StructBp b, Room r, int s) {
        b.box(r.x0, r.fy, r.z0, r.x1, r.fy, r.z1, "cobblestone");
        // tres celdas contra el fondo
        for (int i = 0; i < 3; i++) {
            int xa = r.x0 + 1 + i * 4;
            int xb = xa + 3;
            for (int y = 0; y <= 5; y++) {
                for (int x = xa; x <= xb; x++) {
                    b.set(x, y, r.z1 - 5, "iron_bars");
                }
                b.set(xa, y, r.z1 - 5, y <= 5 ? "stone_bricks" : "air");
            }
            ironDoor(b, xa + 1, 0, r.z1 - 5, "+z", false);
            b.set(xb - 1, 0, r.z1 - 1, "hay_block[axis=x]");
            b.set(xb - 1, 1, r.z1 - 1, "skeleton_skull[rotation=" + (4 + i * 3) + "]");
            b.set(xa, 0, r.z1 - 1, "bone_block[axis=x]");
            b.set(xa + 1, 0, r.z1 - 3, "cobweb");
            b.set(xa, 6, r.z1 - 1, "cobweb");
        }
        for (int x = r.x0 + 2; x <= r.x1 - 2; x += 4) {
            hangLantern(b, x, 7, r.z0 + 3, 1, "soul_lantern");
        }
    }

    // ================================================================== sala superior (T2): jardín interior

    private static void interiorT2(StructBp b) {
        int fy = 9;
        int h = 8;
        Room hall = new Room(b, -20, 37, 20, 72, fy, h).carve();
        hall.baseboard(SBS).cornice(SBS).torches(7, fy + 4, false);
        // suelo con alfombra de musgo y orla
        for (int x = -20; x <= 20; x++) {
            for (int z = 37; z <= 72; z++) {
                b.set(x, fy, z, SB);
            }
        }
        borderLine(b, -20, 37, 20, 72, fy, 1, CSB);
        for (int z = 38; z <= 71; z++) {
            for (int x = -1; x <= 1; x++) {
                if (z < 51 || z > 61) {
                    b.set(x, fy + 1, z, "moss_carpet");
                }
            }
        }
        // estanque central con isla
        pool(b, -5, 51, 5, 61, fy + 1, 1, MSB, "water", SB);
        b.box(-1, fy + 1, 55, 1, fy + 1, 57, "mossy_cobblestone");
        b.set(0, fy + 2, 56, "flowering_azalea");
        b.set(1, fy + 2, 56, "fern");
        b.set(-1, fy + 2, 57, "azalea");
        for (int i = 0; i < 6; i++) {
            b.set(-4 + b.r.nextInt(9), fy + 1, 52 + b.r.nextInt(9), b.chance(0.5) ? "lily_pad" : "air");
        }
        // columnas
        for (int z = 40; z <= 68; z += 7) {
            for (int s : new int[]{-1, 1}) {
                column2(b, s > 0 ? 8 : -9, z, fy + 1, fy + h, SB, SBL);
            }
        }
        // claraboyas hacia la plaza (dos huecos con jardín debajo)
        for (int s : new int[]{-1, 1}) {
            int cx = s * 9;
            b.air(cx - 3, 18, 38, cx + 3, 19, 44);
            for (int x = cx - 4; x <= cx + 4; x++) {
                for (int z = 37; z <= 45; z++) {
                    if (x == cx - 4 || x == cx + 4 || z == 37 || z == 45) {
                        b.set(x, 20, z, MSBW);
                    }
                }
            }
            // jardín bajo la claraboya
            b.box(cx - 3, fy, 38, cx + 3, fy, 44, "moss_block");
            for (int x = cx - 3; x <= cx + 3; x++) {
                for (int z = 38; z <= 44; z++) {
                    if (b.chance(0.5)) {
                        b.set(x, fy + 1, z, b.pick("fern", "azalea", "flowering_azalea", "moss_carpet", "poppy", "cornflower", "grass"));
                    }
                }
            }
            for (int y = fy + 1; y <= 17; y++) {
                b.set(cx, y, 41, log("oak_log", 'y'));
            }
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    for (int dy = 0; dy <= 4; dy++) {
                        if (dx * dx + dz * dz + dy * dy * 2 <= 10 && b.chance(0.92)) {
                            b.setIfAbsent(cx + dx, 15 + dy, 41 + dz, b.pick(CROWN));
                        }
                    }
                }
            }
        }
        // ventanas gruesas hacia fuera (paredes de 2 de grosor)
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{42, 52, 62, 70}) {
                openX(b, z, fy + 2, s * 21, s * 22, 1, 6, 2, CSB, "lime_stained_glass_pane");
            }
        }
        // luces y colgantes
        for (int z : new int[]{44, 56, 66}) {
            chandelier(b, 0, 16, z, 3, "lantern");
        }
        // balaustrada de la escalera de subida (huecos en y=17..)
        for (int s : new int[]{-1, 1}) {
            wallBanner(b, s * 19, fy + 5, 40, "green", s > 0 ? "-x" : "+x");
            wallBanner(b, s * 19, fy + 5, 68, "green", s > 0 ? "-x" : "+x");
        }
        // rincones: braseros
        for (int s : new int[]{-1, 1}) {
            brazier(b, s * 19, fy + 1, 37, SB, "campfire[lit=true]");
        }
    }

    // ================================================================== cripta bajo tierra

    private static void basement(StructBp b) {
        int fy = -11;
        int h = 8;
        // bloque macizo a modo de pared de la cripta y luego se vacía
        b.box(-16, fy, 28, 16, fy + h + 1, 67, "deepslate_bricks");
        Room hall = new Room(b, -9, 40, 9, 66, fy, h).carve();
        Room end = new Room(b, -6, 30, 6, 37, fy, h).carve();
        // arco entre ambas
        openZ(b, 0, fy + 1, 38, 39, 3, 7, 2, "polished_deepslate", null);
        for (int x = -9; x <= 9; x++) {
            for (int z = 40; z <= 66; z++) {
                b.set(x, fy, z, ((x + z) & 1) == 0 ? "deepslate_tiles" : "polished_deepslate");
            }
        }
        // canal central con agua
        pool(b, -2, 44, 2, 58, fy + 1, 1, "deepslate_bricks", "water", "deepslate_tiles");
        for (int x = -2; x <= 2; x++) {
            b.set(x, fy + 1, 51, "polished_deepslate");
            b.set(x, fy + 2, 51, "air");
        }
        // columnas
        for (int z = 44; z <= 62; z += 6) {
            for (int s : new int[]{-1, 1}) {
                column(b, s * 5, z, fy + 1, fy + h, "deepslate_bricks", "deepslate_brick_stairs", "chiseled_deepslate");
            }
        }
        // nichos con sarcófagos a ambos lados
        for (int s : new int[]{-1, 1}) {
            for (int z = 44; z <= 62; z += 6) {
                b.air(s * 10, fy + 1, z - 1, s * 12, fy + 4, z + 1);
                sarcophagus(b, s > 0 ? 10 : 10 * s - 2, fy + 1, z, 'x', 3, "stone_bricks", "stone_brick_slab", "stone_brick_stairs");
                // arreglo: el sarcófago ocupa 2 en z
                b.set(s * 10, fy + 4, z, "skeleton_skull[rotation=" + (s > 0 ? 12 : 4) + "]");
                candles(b, s * 11, fy + 3, z + 1, 2);
            }
        }
        hall.baseboard("deepslate_brick_stairs").cornice("deepslate_brick_stairs").torches(6, fy + 3, true);
        end.baseboard("deepslate_brick_stairs").cornice("deepslate_brick_stairs");
        for (int x = -6; x <= 6; x++) {
            for (int z = 30; z <= 37; z++) {
                b.set(x, fy, z, "polished_blackstone_bricks");
            }
        }
        // altar de la cripta
        b.box(-2, fy + 1, 32, 2, fy + 1, 34, "obsidian");
        b.box(-1, fy + 2, 33, 1, fy + 2, 33, "crying_obsidian");
        b.set(0, fy + 3, 33, "soul_campfire[lit=true]");
        for (int x : new int[]{-5, 5}) {
            b.set(x, fy + 1, 31, "polished_blackstone_brick_wall");
            b.set(x, fy + 2, 31, "polished_blackstone_brick_wall");
            b.set(x, fy + 3, 31, "skeleton_skull[rotation=8]");
            b.set(x, fy + 4, 31, "soul_lantern[hanging=false]");
        }
        // reja a la entrada
        for (int x = -3; x <= 3; x++) {
            for (int y = fy + 1; y <= fy + 7; y++) {
                if (b.get(x, y, 38) == null || b.get(x, y, 38).equals("air")) {
                    b.set(x, y, 38, "iron_bars");
                }
            }
        }
        chandelier(b, 0, fy + h, 52, 2, "soul_lantern");
        chandelier(b, 0, fy + h, 64, 2, "soul_lantern");
        for (int x = -4; x <= 4; x += 2) {
            b.set(x, fy + h, 34, "cobweb");
        }
    }

    // ================================================================== santuario con el árbol gigante

    private static void shrine(StructBp b) {
        int fy = 19;
        int top = 32;
        // muros gruesos
        b.box(-15, fy, 46, 15, top, 72, SB);
        b.air(-13, fy + 1, 48, 13, top + 1, 70);
        // suelo: círculo de musgo con orla
        for (int x = -13; x <= 13; x++) {
            for (int z = 48; z <= 70; z++) {
                b.set(x, fy, z, SB);
            }
        }
        borderLine(b, -13, 48, 13, 70, fy, 1, CSB);
        for (int x = -6; x <= 6; x++) {
            for (int z = 53; z <= 65; z++) {
                double d = Math.hypot(x, z - 59);
                if (d <= 5.5) {
                    b.set(x, fy, z, d > 4.3 ? CSB : "moss_block");
                }
            }
        }
        // puerta gótica
        openZ(b, 0, fy + 1, 46, 47, 3, 10, 2, CSB, null);
        // ventanas altas de cristal verde
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{52, 59, 66}) {
                openX(b, z, fy + 3, s * 14, s * 15, 1, 9, 2, CSB, "lime_stained_glass_pane");
            }
        }
        b.walls(-15, fy + 1, 46, 15, fy + 1, 72, CSB);
        // contrafuertes
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{50, 59, 68}) {
                buttress(b, s * 15, z, s > 0 ? "+x" : "-x", fy + 1, 13, 3, SBS, SB);
            }
        }
        // techo a cuatro aguas con vigas
        hipRoof(b, -15, 46, 15, 72, top + 1, 1, MSBS, MSB, "mossy_stone_brick_slab", 1);
        for (int z : new int[]{52, 59, 66}) {
            b.box(-14, top - 1, z, 14, top - 1, z, "dark_oak_log[axis=x]");
            chandelier(b, -6, top - 2, z, 3, "lantern");
            chandelier(b, 6, top - 2, z, 3, "lantern");
        }
        // el árbol del mundo: tronco grueso con raíces que se abren por el suelo, ramas dentro de la sala y copa gigante fuera
        int tx = 0;
        int tz = 59;
        b.air(-3, top + 1, 56, 3, top + 18, 62);
        for (int y = fy + 1; y <= 52; y++) {
            double r = y < fy + 6 ? 3.3 - (y - fy - 1) * 0.3 : y < fy + 15 ? 2.0 : 1.5;
            b.disc(tx, tz, r, y, y, log("dark_oak_log", 'y'));
        }
        for (int i = 0; i < 10; i++) {
            double ang = i * Math.PI / 5 + 0.3;
            int len = 4 + b.r.nextInt(3);
            for (int d = 2; d <= len + 2; d++) {
                int rx = tx + (int) Math.round(Math.cos(ang) * d);
                int rz = tz + (int) Math.round(Math.sin(ang) * d);
                char ax = Math.abs(Math.cos(ang)) > Math.abs(Math.sin(ang)) ? 'x' : 'z';
                b.set(rx, fy + 1, rz, log("dark_oak_log", ax));
                if (d <= 4) {
                    b.set(rx, fy + 2, rz, log("dark_oak_log", ax));
                }
            }
        }
        // ramas largas dentro de la sala
        int[][] arms = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] a2 : arms) {
            int y = fy + 12;
            for (int d = 2; d <= 9; d++) {
                if (d % 3 == 0) {
                    y++;
                }
                int px = tx + a2[0] * d;
                int pz = tz + a2[1] * d;
                b.set(px, y, pz, log("dark_oak_log", a2[0] != 0 ? 'x' : 'z'));
            }
            int ex = tx + a2[0] * 9;
            int ez = tz + a2[1] * 9;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        if (dx * dx + dz * dz + dy * dy * 2 <= 5 && b.chance(0.9)) {
                            b.setIfAbsent(ex + dx, fy + 16 + dy, ez + dz, b.pick(CROWN));
                        }
                    }
                }
            }
        }
        int[][] blobs = {{0, 48, 0, 9}, {-8, 46, 2, 7}, {8, 45, 1, 7}, {2, 45, -7, 6}, {-3, 46, 8, 6}, {0, 54, 0, 6}, {6, 50, 5, 5}, {-6, 50, -4, 5}};
        for (int[] c : blobs) {
            int rr = c[3];
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dy = -rr / 2; dy <= rr / 2 + 1; dy++) {
                    for (int dz = -rr; dz <= rr; dz++) {
                        double d = dx * dx + dy * dy * 2.4 + dz * dz;
                        if (d <= rr * rr && b.chance(0.93)) {
                            b.setIfAbsent(tx + c[0] + dx, c[1] + dy, tz + c[2] + dz, b.pick(CROWN));
                        }
                    }
                }
            }
        }
        // torretas en las esquinas del santuario, con tejado de cobre
        for (int sx : new int[]{-1, 1}) {
            for (int zc : new int[]{47, 71}) {
                int cx = sx * 16;
                b.tube(cx, zc, 3.0, 1.6, fy + 1, top + 4, SB);
                b.disc(cx, zc, 2.6, top + 5, top + 5, CSB);
                cone(b, cx, zc, 4.2, top + 6, 11, "waxed_oxidized_cut_copper", null, "gold_block");
                b.set(cx, top + 5, zc, "lantern");
            }
        }
        // remate de cobre en lo alto del tejado
        recolor(b, top + 10, 80, "mossy_stone_brick", "waxed_oxidized_cut_copper");
        // altar frente al árbol
        b.box(-3, fy + 1, 52, 3, fy + 1, 52, CSB);
        candles(b, -3, fy + 2, 52, 3);
        candles(b, 3, fy + 2, 52, 3);
        b.set(0, fy + 2, 52, "shroomlight");
        for (int s : new int[]{-1, 1}) {
            brazier(b, s * 10, fy + 1, 50, SB, "campfire[lit=true]");
            brazier(b, s * 10, fy + 1, 68, SB, "campfire[lit=true]");
            knight(b, s * 11, fy + 1, 59, s > 0 ? "-x" : "+x", SB, SBS, SBW, CSB, PA);
        }
        // banderas a lo largo
        for (int z : new int[]{52, 59, 66}) {
            for (int s : new int[]{-1, 1}) {
                wallBanner(b, s * 13, fy + 6, z + 3, "green", s > 0 ? "-x" : "+x");
            }
        }
    }

    /** Cambia, por encima de una altura, los bloques de piedra por su versión (bloque, escalera, losa) de otro material. */
    private static void recolor(StructBp b, int minY, int maxY, String fromPrefix, String toBase) {
        for (java.util.Map.Entry<Long, String> e : b.m.entrySet()) {
            int y = StructBp.ky(e.getKey());
            String v = e.getValue();
            if (y < minY || y > maxY || !v.startsWith(fromPrefix)) {
                continue;
            }
            if (v.contains("_stairs")) {
                e.setValue(toBase + "_stairs" + v.substring(v.indexOf('[')));
            } else if (v.contains("_slab")) {
                e.setValue(toBase + "_slab" + v.substring(v.indexOf('[')));
            } else if (!v.contains("_wall")) {
                e.setValue(toBase);
            }
        }
    }

    // ================================================================== acabado: plantas, enredaderas, musgo

    private static void finishing(StructBp b) {
        // árboles grandes alrededor y atrás
        int[][] trees = {{-27, 6}, {26, 5}, {-28, 14}, {27, 15}, {-20, 86}, {18, 87}, {-6, 90}, {8, 91}, {-34, 50}, {34, 55}, {-33, 30}, {33, 28}};
        for (int[] t : trees) {
            bigTree(b, t[0], t[1], 16 + b.r.nextInt(5), "dark_oak_log", CROWN, 7);
        }
        // césped y flores en el suelo de la plaza
        groundCover(b, -12, 0, 12, 12, 0 - 1, 0.05, "grass", "fern", "poppy");
        // plantas en las terrazas
        groundCover(b, -29, 23, 29, 79, 9, 0.07, "grass", "fern", "moss_carpet", "azalea", "flowering_azalea");
        groundCover(b, -21, 35, 21, 73, 19, 0.07, "grass", "fern", "moss_carpet", "azalea", "flowering_azalea");
        // desgaste en manchas
        weather(b, 11, "stone_brick", "mossy_stone_brick", 5.5, 0.50);
        weather(b, 23, "stone_bricks", "cracked_stone_bricks", 3.0, 0.70);
        weather(b, 5, "cobblestone", "mossy_cobblestone", 4.0, 0.4);
        hangVines(b, "stone_brick", 3, 0.07, 4);
        // enredaderas pegadas a las paredes exteriores
        for (int i = 0; i < 70; i++) {
            int side = b.r.nextInt(4);
            int yTop = 5 + b.r.nextInt(5);
            int len = 3 + b.r.nextInt(6);
            int a = side < 2 ? -29 + b.r.nextInt(59) : 23 + b.r.nextInt(56);
            for (int j = 0; j < len; j++) {
                int y = yTop - j;
                if (y < 0) {
                    break;
                }
                switch (side) {
                    case 0 -> b.setIfAbsent(a, y, 21, "vine[south=true]");
                    case 1 -> b.setIfAbsent(a, y, 81, "vine[north=true]");
                    case 2 -> b.setIfAbsent(-31, y, a, "vine[east=true]");
                    default -> b.setIfAbsent(31, y, a, "vine[west=true]");
                }
            }
        }
    }
}
