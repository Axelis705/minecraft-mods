package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Ciudadela de Cristal: un palacio gótico de cuarzo y hielo rodeado de foso helado y muralla con torres,
 * con catedral central (naves, galería, trono), edificios en los patios, y mazmorras heladas bajo tierra.
 * Entrada por z = 0, se extiende hacia z positivo.
 */
final class DesignHielo {
    private DesignHielo() {
    }

    private static final String Q = "quartz_bricks";
    private static final String QS = "quartz_stairs";
    private static final String QL = "quartz_slab";
    private static final String SQ = "smooth_quartz";
    private static final String SQS = "smooth_quartz_stairs";
    private static final String SQL = "smooth_quartz_slab";
    private static final String CQ = "chiseled_quartz_block";
    private static final String QP = "quartz_pillar[axis=y]";
    private static final String CAL = "calcite";
    private static final String SNOW = "snow_block";
    private static final String PI = "packed_ice";
    private static final String BI = "blue_ice";
    private static final String GL = "light_blue_stained_glass";
    private static final String GLP = "light_blue_stained_glass_pane";
    private static final String PB = "prismarine_bricks";
    private static final String PBS = "prismarine_brick_stairs";
    private static final String PBL = "prismarine_brick_slab";
    private static final String SOUL = "soul_lantern";

    static StructBp make() {
        StructBp b = new StructBp(3003);
        b.foundation(-42, -16, 42, 84, SNOW);
        ground(b);
        wall(b);
        towers(b);
        gatehouse(b);
        cathedral(b);
        courtyardBuildings(b);
        fountain(b);
        dungeon(b);
        finishing(b);
        Linker.link(b, 0, 0, -3, 10, Q, QS);
        return b;
    }

    // ================================================================== terreno, foso y camino

    private static void ground(StructBp b) {
        b.box(-42, -1, -16, 42, -1, 84, SNOW);
        // foso helado alrededor de la muralla
        for (int x = -36; x <= 36; x++) {
            for (int z = 8; z <= 82; z++) {
                boolean outer = x >= -36 && x <= 36 && z >= 10 && z <= 80;
                boolean inner = x >= -31 && x <= 31 && z >= 15 && z <= 77;
                if (outer && !inner) {
                    b.set(x, -1, z, ((x * 3 + z * 5) % 7 == 0) ? PI : BI);
                    b.set(x, -2, z, BI);
                }
            }
        }
        // camino de entrada y puente con arcos
        for (int z = -14; z <= 18; z++) {
            for (int x = -4; x <= 4; x++) {
                String s = Math.abs(x) == 4 ? CQ : ((z & 1) == 0 ? SQ : Q);
                b.set(x, -1, z, s);
            }
        }
        for (int z = 8; z <= 17; z++) {
            for (int s : new int[]{-1, 1}) {
                b.set(s * 5, 0, z, "quartz_slab[type=bottom]");
                b.set(s * 5, -1, z, Q);
                b.set(s * 4, 0, z, Q);
                b.set(s * 4, 1, z, "iron_bars");
                if ((z - 8) % 3 == 0) {
                    b.set(s * 4, 0, z, CQ);
                    b.set(s * 4, 1, z, CQ);
                    b.set(s * 4, 2, z, CQ);
                    b.set(s * 4, 3, z, SOUL + "[hanging=false]");
                }
            }
        }
        // farolas de cristal a lo largo del camino
        for (int z = -12; z <= 6; z += 6) {
            for (int s : new int[]{-1, 1}) {
                for (int y = 0; y <= 3; y++) {
                    b.set(s * 6, y, z, y == 0 ? CQ : QP);
                }
                b.set(s * 6, 4, z, SOUL + "[hanging=false]");
                crystalPatch(b, s * 10, z + 3);
            }
        }
        // pórtico de hielo en la entrada del camino
        for (int s : new int[]{-1, 1}) {
            for (int y = 0; y <= 12; y++) {
                b.box(s * 6 - 1, y, -15, s * 6 + 1, y, -14, y % 4 == 3 ? CQ : Q);
            }
            crystal(b, s * 6, -15, 13, 9, 2, 0, 0, 3, BI, PI, BI);
        }
        openZ(b, 0, 0, -15, -14, 2, 2, 0, null, null);
        b.box(-6, 13, -15, 6, 13, -14, CQ);
        for (int x = -5; x <= 5; x++) {
            int hh = 1 + (int) (3 - Math.abs(x) * 0.5);
            for (int y = 0; y < hh; y++) {
                b.set(x, 14 + y, -15, y == hh - 1 ? BI : PI);
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int y = 2; y <= 12; y++) {
                if (b.get(x, y, -15) != null && !b.get(x, y, -15).equals("air")) {
                    b.set(x, y, -15, "air");
                }
                b.set(x, y, -14, "air");
            }
        }
    }

    private static void crystalPatch(StructBp b, int x, int z) {
        crystal(b, x, z, 0, 5 + b.r.nextInt(4), 2, b.r.nextBoolean() ? 1 : -1, 0, 3, BI, PI, "light_blue_stained_glass");
        crystal(b, x + 2, z + 1, 0, 3 + b.r.nextInt(3), 1, 0, 1, 2, PI, BI);
        crystal(b, x - 2, z - 1, 0, 3 + b.r.nextInt(3), 1, 0, -1, 2, PI, BI);
    }

    private static void crystal(StructBp b, int x, int z, int y0, int h, int w, int dx, int dz, int every, String... pal) {
        StructDesigns.crystal(b, x, z, y0, h, w, dx, dz, every, pal);
    }

    // ================================================================== muralla

    private static void wall(StructBp b) {
        b.box(-28, -1, 18, 28, 14, 74, Q);
        b.air(-25, 0, 21, 25, 14, 71);
        // zócalo de calcita, bandas y cornisa
        b.walls(-29, -1, 17, 29, 1, 75, CAL);
        ring(b, -29, 17, 29, 75, 2, QS, false);
        b.walls(-28, 6, 18, 28, 6, 74, SQ);
        b.walls(-28, 11, 18, 28, 11, 74, CQ);
        ring(b, -29, 17, 29, 75, 13, QS, true);
        // paseo de ronda y almenas
        b.box(-28, 14, 18, 28, 14, 74, SQ);
        b.air(-25, 14, 21, 25, 14, 71);
        crenel(b, -28, 18, 28, 74, 15, 2, Q);
        // barandilla interior
        for (int x = -25; x <= 25; x++) {
            b.set(x, 15, 21, "iron_bars");
            b.set(x, 15, 71, "iron_bars");
        }
        for (int z = 21; z <= 71; z++) {
            b.set(-25, 15, z, "iron_bars");
            b.set(25, 15, z, "iron_bars");
        }
        // pilastras de pilar de cuarzo hacia fuera
        for (int x = -24; x <= 24; x += 8) {
            if (Math.abs(x) >= 8) {
                for (int y = 2; y <= 12; y++) {
                    b.set(x, y, 17, y == 2 ? CQ : QP);
                }
                b.set(x, 13, 17, sti(QS, "+z"));
            }
            for (int y = 2; y <= 12; y++) {
                b.set(x, y, 75, y == 2 ? CQ : QP);
            }
            b.set(x, 13, 75, sti(QS, "-z"));
        }
        for (int z = 26; z <= 66; z += 8) {
            for (int s : new int[]{-1, 1}) {
                for (int y = 2; y <= 12; y++) {
                    b.set(s * 29, y, z, y == 2 ? CQ : QP);
                }
                b.set(s * 29, 13, z, sti(QS, s > 0 ? "-x" : "+x"));
            }
        }
        // ventanas estrechas de cristal
        for (int x = -20; x <= 20; x += 8) {
            if (Math.abs(x) >= 12) {
                openZ(b, x, 7, 17, 20, 0, 3, 2, CQ, GLP);
            }
            openZ(b, x, 7, 72, 75, 0, 3, 2, CQ, GLP);
        }
        for (int z = 30; z <= 62; z += 8) {
            for (int s : new int[]{-1, 1}) {
                openX(b, z, 7, s * 26, s * 29, 0, 3, 2, CQ, GLP);
            }
        }
        // escaleras de la muralla (en el interior, cerca de las torres de la puerta)
        for (int s : new int[]{-1, 1}) {
            stairway(b, s * 22, 0, 22, "+z", 1, 1, QS, Q, false);
            for (int k = 0; k < 13; k++) {
                b.set(s * 23, k, 22 + k, st(QS, "+z"));
                b.set(s * 24, k, 22 + k, st(QS, "+z"));
                for (int yy = 0; yy < k; yy++) {
                    b.set(s * 23, yy, 22 + k, Q);
                    b.set(s * 24, yy, 22 + k, Q);
                }
            }
        }
    }

    // ================================================================== torres redondas con aguja de hielo

    private static void towers(StructBp b) {
        int[][] t = {{-27, 19, 6, 32}, {27, 19, 6, 32}, {-27, 73, 6, 34}, {27, 73, 6, 34}};
        for (int[] c : t) {
            roundTower(b, c[0], c[1], c[2], c[3]);
        }
    }

    private static void roundTower(StructBp b, int cx, int cz, int r, int top) {
        b.tube(cx, cz, r + 0.5, 2.2, -1, top, Q);
        b.disc(cx, cz, r - 2.0, -1, -1, SQ);
        b.air(cx, 0, cz, cx, 0, cz);
        // bandas y cornisa de la torre
        for (int y : new int[]{6, 14, 22}) {
            b.tube(cx, cz, r + 0.9, 1.0, y, y, CQ);
        }
        b.tube(cx, cz, r + 1.6, 1.4, top, top, SQ);
        b.tube(cx, cz, r + 2.4, 1.2, top + 1, top + 1, CQ);
        // ventanas estrechas en cuatro direcciones
        for (int y = 6; y <= top - 5; y += 8) {
            for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                for (int dd = r - 3; dd <= r + 1; dd++) {
                    for (int dy = 0; dy < 3; dy++) {
                        b.set(cx + d[0] * dd, y + dy, cz + d[1] * dd, dd == r - 1 ? GLP : "air");
                    }
                }
            }
        }
        // caracol interior hasta lo alto y luces
        spiral(b, cx, cz, 0, top - 2, QS, CQ, true);
        for (int y = 8; y <= top - 6; y += 8) {
            hangLantern(b, cx, y + 6, cz, 0, SOUL);
        }
        // tejado cónico de hielo
        cone(b, cx, cz, r + 3.4, top + 2, 24, PI, null, "sea_lantern");
        for (int i = 0; i < 5; i++) {
            b.set(cx, top + 26 + i, cz, i == 4 ? "end_rod[facing=up]" : BI);
        }
        // anillo de cristal alrededor del arranque del tejado
        for (int a = 0; a < 8; a++) {
            double ang = a * Math.PI / 4;
            crystal(b, cx + (int) Math.round(Math.cos(ang) * (r + 2)), cz + (int) Math.round(Math.sin(ang) * (r + 2)), top + 2, 6, 1, 0, 0, 2, BI, "light_blue_stained_glass");
        }
    }

    // ================================================================== torre de entrada con puerta

    private static void gatehouse(StructBp b) {
        for (int s : new int[]{-1, 1}) {
            int cx = s * 11;
            roundTower(b, cx, 21, 6, 30);
        }
        // arco de la puerta
        openZ(b, 0, 0, 17, 20, 4, 11, 2, CQ, null);
        for (int x = -4; x <= 4; x++) {
            for (int y = 1; y <= 9; y++) {
                if (Math.abs(x) <= 3 - (y > 6 ? (y - 6) : 0) || y <= 6) {
                    b.set(x, y, 18, "iron_bars");
                }
            }
        }
        // puerta levadiza sobre el foso: cadenas
        for (int s : new int[]{-1, 1}) {
            for (int y = 9; y <= 14; y++) {
                b.set(s * 3, y, 17, "chain[axis=y]");
            }
        }
        // escudo sobre la puerta
        for (int x = -3; x <= 3; x++) {
            b.set(x, 13, 17, CQ);
        }
        for (int y = 14; y <= 17; y++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, y, 17, y == 15 ? "light_blue_concrete" : "white_concrete");
            }
        }
        b.set(0, 18, 17, BI);
        b.set(0, 19, 17, "sea_lantern");
        hangLantern(b, 0, 9, 19, 1, SOUL);
    }

    // ================================================================== catedral

    private static void cathedral(StructBp b) {
        // volumen: naves x[-14,14], z[34,66], hasta el alero y=24
        b.box(-14, -1, 34, 14, 24, 66, Q);
        Room nave = new Room(b, -12, 36, 12, 64, -1, 23).carve();
        // zócalo y bandas exteriores
        b.walls(-15, -1, 33, 15, 1, 67, CAL);
        ring(b, -15, 33, 15, 67, 2, QS, false);
        b.walls(-14, 8, 34, 14, 8, 66, SQ);
        b.walls(-14, 16, 34, 14, 16, 66, CQ);
        ring(b, -15, 33, 15, 67, 23, QS, true);
        // tejado a dos aguas de prismarina, con cumbrera a lo largo de Z
        gableZ(b, -14, 14, 34, 66, 25, 2, 1, PBS, PB, PBL, true, 1);
        // contrafuertes volantes y pináculos a los lados
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{38, 46, 54, 62}) {
                buttress(b, s * 14, z, s > 0 ? "+x" : "-x", 2, 22, 5, QS, Q);
                for (int y = 2; y <= 24; y++) {
                    b.set(s * 14 + s * 5, y, z, y <= 22 ? QP : Q);
                }
                // pináculo de cristal
                crystal(b, s * 19, z, 25, 7, 1, 0, 0, 2, BI, PI);
            }
        }
        // ventanas góticas
        for (int s : new int[]{-1, 1}) {
            for (int z : new int[]{40, 48, 56}) {
                openX(b, z, 5, s * 13, s * 14, 2, 17, 2, CQ, GLP);
            }
        }
        // fachada: torres gemelas
        for (int s : new int[]{-1, 1}) {
            int x0 = s > 0 ? 7 : -15;
            int x1 = s > 0 ? 15 : -7;
            b.box(x0, -1, 28, x1, 38, 37, Q);
            b.air(x0 + 2, 0, 30, x1 - 2, 38, 35);
            b.walls(x0 - 1, -1, 27, x1 + 1, 1, 38, CAL);
            for (int y : new int[]{9, 19, 29}) {
                b.walls(x0 - 1, y, 27, x1 + 1, y, 38, CQ);
            }
            ring(b, x0 - 1, 27, x1 + 1, 38, 36, QS, true);
            b.box(x0, 37, 28, x1, 38, 37, SQ);
            hipRoof(b, x0, 28, x1, 37, 39, 1, PBS, PB, PBL, 2);
            int cx = (x0 + x1) / 2;
            for (int i = 0; i < 12; i++) {
                b.set(cx, 46 + i, 32 + 1, i == 11 ? "end_rod[facing=up]" : (i % 4 == 3 ? "sea_lantern" : BI));
            }
            // ventanas de las torres
            for (int y : new int[]{8, 18, 28}) {
                openZ(b, cx, y, 27, 29, 0, 4, 2, CQ, GLP);
            }
            spiral(b, cx, 33, 0, 36, QS, CQ, s > 0);
            // pináculos en las esquinas
            for (int[] c : new int[][]{{x0 - 1, 27}, {x1 + 1, 27}}) {
                for (int y = 0; y <= 38; y++) {
                    b.set(c[0], y, c[1], y < 36 ? QP : CQ);
                }
                crystal(b, c[0], c[1], 39, 6, 1, 0, 0, 2, BI, PI);
            }
        }
        // portal principal y rosetón
        openZ(b, 0, 0, 33, 37, 5, 15, 2, CQ, null);
        for (int x = -5; x <= 5; x++) {
            for (int y = 11; y <= 14; y++) {
                if (b.get(x, y, 34) != null && b.get(x, y, 34).equals("air")) {
                    // celosía de cristal sobre la puerta (tímpano)
                    b.set(x, y, 34, ((x + y) & 1) == 0 ? GLP : "air");
                }
            }
        }
        rose(b, 0, 21, 34);
        // puertas de hierro
        ironDoor(b, -1, 0, 34, "-z", false);
        ironDoor(b, 1, 0, 34, "-z", true);
        for (int x = -4; x <= 4; x += 2) {
            candles(b, x, 0, 32, 3);
        }

        // ---- interior de la nave ----
        nave.baseboard(QS).torches(8, 5, true);
        checker(b, -12, 36, 12, 64, -1, SQ, "quartz_block");
        borderLine(b, -12, 36, 12, 64, -1, 1, CQ);
        for (int z = 36; z <= 56; z++) {
            for (int x = -2; x <= 2; x++) {
                b.set(x, 0, z, "light_blue_carpet");
            }
        }
        // columnas y nervios
        for (int z = 40; z <= 60; z += 6) {
            for (int s : new int[]{-1, 1}) {
                column2(b, s > 0 ? 6 : -7, z, 0, 20, "quartz_pillar[axis=y]", QL);
            }
            b.box(-12, 21, z, 12, 21, z + 1, CQ);
            // arcos de nervio: escaleras en cada columna hacia el centro
            for (int x = -5; x <= 5; x++) {
                int up = 22 + (int) Math.round(3 * Math.cos(x * Math.PI / 12.0));
                b.set(x, up, z, Q);
                b.set(x, up, z + 1, Q);
            }
        }
        for (int z : new int[]{43, 49, 55, 61}) {
            chandelier(b, 0, 20, z, 5, SOUL);
            for (int s : new int[]{-1, 1}) {
                chandelier(b, s * 9, 20, z, 3, SOUL);
            }
        }
        // galería superior (triforio) con barandilla
        for (int s : new int[]{-1, 1}) {
            int xa = s > 0 ? 9 : -12;
            b.box(xa, 8, 38, xa + 3, 8, 56, SQ);
            for (int z = 38; z <= 56; z++) {
                b.set(s > 0 ? 9 : -9, 9, z, "iron_bars");
                b.set(s > 0 ? 9 : -9, 10, z, z % 3 == 0 ? CQ : "iron_bars");
            }
            for (int z = 38; z <= 56; z += 6) {
                for (int y = 0; y <= 7; y++) {
                    b.set(s > 0 ? 9 : -9, y, z, QP);
                }
            }
            // escalera a la galería
            for (int k = 0; k < 8; k++) {
                int z = 64 - k;
                for (int w = 0; w < 3; w++) {
                    int x = s > 0 ? 10 + w : -12 + w;
                    b.set(x, k, z, st(QS, "-z"));
                    for (int yy = 0; yy < k; yy++) {
                        b.set(x, yy, z, Q);
                    }
                }
            }
        }
        // estandartes en las paredes de la nave
        for (int z : new int[]{44, 52, 60}) {
            for (int s : new int[]{-1, 1}) {
                wallBanner(b, s * 12, 12, z, z == 52 ? "white" : "light_blue", s > 0 ? "-x" : "+x");
            }
        }
        throne(b);
        // ventanal tras el trono
        openZ(b, 0, 7, 65, 66, 5, 17, 2, CQ, GLP);
        for (int x = -5; x <= 5; x++) {
            for (int y = 8; y <= 22; y++) {
                if (b.get(x, y, 65) != null && b.get(x, y, 65).startsWith("light_blue_stained_glass_pane") && ((x * 7 + y * 3) % 9 == 0)) {
                    b.set(x, y, 65, "blue_stained_glass_pane");
                }
            }
        }
    }

    private static void rose(StructBp b, int cx, int cy, int z) {
        for (int x = -6; x <= 6; x++) {
            for (int y = -6; y <= 6; y++) {
                double d = Math.hypot(x, y);
                if (d <= 5.6) {
                    boolean spoke = x == 0 || y == 0 || Math.abs(x) == Math.abs(y);
                    boolean ring2 = d > 2.4 && d < 3.4;
                    for (int zz = z; zz <= z + 2; zz++) {
                        b.set(cx + x, cy + y, zz, "air");
                    }
                    b.set(cx + x, cy + y, z + 1, d < 1.5 ? "sea_lantern" : (spoke || ring2) ? CQ : (d < 3.8 ? "blue_stained_glass" : GL));
                } else if (d <= 6.6) {
                    b.set(cx + x, cy + y, z, CQ);
                }
            }
        }
    }

    private static void throne(StructBp b) {
        // tarima de tres escalones al fondo
        for (int i = 0; i < 3; i++) {
            b.box(-9 + i * 3, 0 + i, 55 + i * 3, 9 - i * 3, i, 64, SQ);
            b.box(-9 + i * 3, 0, 55 + i * 3, 9 - i * 3, i - 1 + 1, 64, SQ);
        }
        b.box(-9, 0, 55, 9, 0, 64, SQ);
        for (int i = 0; i < 3; i++) {
            for (int x = -9 + i * 3; x <= 9 - i * 3; x++) {
                b.set(x, i, 55 + i * 3, st(QS, "+z"));
            }
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, 61, CQ);
        }
        // trono: asiento de escalera, brazos y respaldo de cristales
        chair(b, 0, 3, 62, "-z", QS);
        b.set(-1, 3, 62, QL.concat("[type=bottom]"));
        b.set(1, 3, 62, QL.concat("[type=bottom]"));
        for (int y = 4; y <= 8; y++) {
            b.box(-1, y, 63, 1, y, 63, Q);
        }
        crystal(b, -3, 62, 4, 11, 1, -1, 0, 4, BI, PI);
        crystal(b, 3, 62, 4, 11, 1, 1, 0, 4, BI, PI);
        crystal(b, 0, 63, 9, 10, 1, 0, 0, 3, BI, "light_blue_stained_glass");
        // pebeteros de fuego azul
        for (int s : new int[]{-1, 1}) {
            brazier(b, s * 8, 1, 58, Q, "soul_campfire[lit=true]");
            brazier(b, s * 5, 3, 60, Q, "soul_campfire[lit=true]");
        }
    }

    // ================================================================== edificios en los patios

    private static void courtyardBuildings(StructBp b) {
        iceLibrary(b, -24, 26, -17, 44);
        iceGarden(b, -24, 50, -17, 68);
        banquet(b, 17, 26, 24, 44);
        barracks(b, 17, 50, 24, 68);
        // faroles de hielo por los patios
        for (int z = 24; z <= 68; z += 8) {
            for (int s : new int[]{-1, 1}) {
                if (Math.abs(z - 46) > 3) {
                    for (int y = 0; y <= 3; y++) {
                        b.set(s * 15, y, z, y == 0 ? CQ : QP);
                    }
                    b.set(s * 15, 4, z, SOUL + "[hanging=false]");
                }
            }
        }
    }

    private static Room hut(StructBp b, int x0, int z0, int x1, int z1) {
        // dos plantas; muros de cuarzo; tejado a dos aguas de prismarina con la cumbrera a lo largo de Z
        b.box(x0 - 1, -1, z0 - 1, x1 + 1, 11, z1 + 1, Q);
        Room low = new Room(b, x0, z0, x1, z1, -1, 5).carve();
        Room up = new Room(b, x0, z0, x1, z1, 5, 5).carve();
        b.box(x0, 5, z0, x1, 5, z1, "spruce_planks");
        b.walls(x0 - 1, -1, z0 - 1, x1 + 1, 1, z1 + 1, CAL);
        b.walls(x0 - 1, 5, z0 - 1, x1 + 1, 5, z1 + 1, SQ);
        ring(b, x0 - 2, z0 - 2, x1 + 2, z1 + 2, 11, QS, true);
        gableZ(b, x0 - 1, x1 + 1, z0 - 1, z1 + 1, 12, 1, 1, PBS, PB, PBL, true, 1);
        low.baseboard(QS).torches(6, 2, true);
        up.baseboard(QS).torches(6, 8, true);
        // escalera interior a la planta alta
        for (int k = 0; k < 6; k++) {
            b.set(x0, k, z0 + k, st(QS, "+z"));
            b.air(x0, k + 1, z0 + k, x0, k + 3, z0 + k);
            b.set(x0, k, z0 + k, st(QS, "+z"));
        }
        b.air(x0, 5, z0 + 1, x0, 5, z0 + 6);
        // puerta y ventanas
        int cx = (x0 + x1) / 2;
        boolean west = x1 < 0;
        openX(b, (z0 + z1) / 2, 0, west ? x1 : x0, west ? x1 + 1 : x0 - 1, 1, 4, 2, CQ, null);
        for (int z = z0 + 3; z <= z1 - 3; z += 5) {
            openX(b, z, 6, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 3, 2, CQ, GLP);
            openX(b, z, 1, west ? x0 - 1 : x1, west ? x0 : x1 + 1, 0, 3, 2, CQ, GLP);
        }
        chandelier(b, cx, 4, (z0 + z1) / 2, 1, SOUL);
        return low;
    }

    private static void iceLibrary(StructBp b, int x0, int z0, int x1, int z1) {
        Room r = hut(b, x0, z0, x1, z1);
        for (int y : new int[]{0, 6}) {
            shelves(b, x0, y, z0 + 2, 'z', z1 - z0 - 3, 4);
        }
        b.air(x0, 5, z0 + 2, x0, 5, z1 - 1);
        for (int y : new int[]{0, 6}) {
            for (int z = z0 + 8; z <= z0 + 14; z += 3) {
                b.set(x1 - 1, y, z, "lectern[facing=west]");
            }
            table(b, x0 + 3, y, z0 + 4, 'z', 4, "spruce_slab", "spruce_fence");
        }
        candles(b, x0 + 3, 1, z0 + 5, 3);
    }

    private static void iceGarden(StructBp b, int x0, int z0, int x1, int z1) {
        hut(b, x0, z0, x1, z1);
        b.box(x0, -1, z0, x1, -1, z1, SNOW);
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            for (int z = z0 + 1; z <= z1 - 1; z++) {
                if (b.chance(0.35)) {
                    b.set(x, 0, z, b.pick("blue_orchid", "cornflower", "azure_bluet", "lily_of_the_valley", "snow[layers=1]"));
                }
            }
        }
        // árboles congelados en el interior
        for (int z = z0 + 3; z <= z1 - 3; z += 6) {
            spruce(b, (x0 + x1) / 2, z, 5, "spruce_log", "spruce_leaves[persistent=true]");
            b.set((x0 + x1) / 2, 4, z, "air");
            b.set((x0 + x1) / 2, 4, z, "spruce_log[axis=y]");
        }
        for (int x = x0 + 1; x <= x1 - 1; x += 2) {
            b.set(x, 5, z1 - 1, "air");
        }
        for (int y = 0; y <= 4; y++) {
            for (int x = x0; x <= x1; x++) {
                b.set(x, y, z1 + 1, y >= 1 && y <= 4 ? GL : Q);
            }
        }
    }

    private static void banquet(StructBp b, int x0, int z0, int x1, int z1) {
        hut(b, x0, z0, x1, z1);
        for (int z = z0 + 3; z <= z1 - 3; z += 4) {
            table(b, x0 + 2, 0, z, 'x', 4, "spruce_slab", "spruce_fence");
            for (int x = x0 + 2; x <= x0 + 5; x += 3) {
                chair(b, x, 0, z - 1, "+z", "spruce_stairs");
                chair(b, x, 0, z + 1, "-z", "spruce_stairs");
            }
            candles(b, x0 + 3, 2, z, 2);
        }
        for (int z = z0 + 2; z <= z0 + 6; z++) {
            b.set(x1, 0, z, "barrel[facing=up]");
            b.set(x1, 1, z, z % 2 == 0 ? "barrel[facing=up]" : "air");
        }
        b.set(x0, 0, z1, "furnace[facing=east,lit=true]");
        b.set(x0, 0, z1 - 1, "smoker[facing=east,lit=true]");
        b.set(x0, 0, z1 - 2, "cauldron");
        // chimenea
        for (int y = 12; y <= 16; y++) {
            b.set(x0, y, z1, Q);
        }
        b.set(x0, 17, z1, "campfire[lit=true]");
    }

    private static void barracks(StructBp b, int x0, int z0, int x1, int z1) {
        hut(b, x0, z0, x1, z1);
        for (int z = z0 + 2; z <= z1 - 2; z += 3) {
            b.set(x1, 0, z, "light_blue_bed[facing=west,part=foot]");
            b.set(x1 - 1, 0, z, "light_blue_bed[facing=west,part=head]");
            b.set(x1, 6, z, "white_bed[facing=west,part=foot]");
            b.set(x1 - 1, 6, z, "white_bed[facing=west,part=head]");
            b.set(x1 - 3, 0, z, "barrel[facing=up]");
        }
        for (int z = z0 + 3; z <= z1 - 3; z += 6) {
            b.set(x0 + 3, 0, z, "anvil[facing=north]");
            b.set(x0 + 3, 1, z + 1, "iron_bars");
            wallBanner(b, x0 - 0, 3, z, "light_blue", "+x");
        }
    }

    private static void fountain(StructBp b) {
        for (int x = -4; x <= 4; x++) {
            for (int z = 21; z <= 27; z++) {
                double d = Math.hypot(x, z - 24);
                if (d <= 3.8) {
                    b.set(x, -1, z, d > 2.8 ? CQ : "ice");
                    if (d > 2.8) {
                        b.set(x, 0, z, SQ);
                    }
                }
            }
        }
        crystal(b, 0, 24, 0, 11, 2, 0, 0, 3, BI, PI, "light_blue_stained_glass");
        b.set(0, 0, 24, BI);
        b.set(0, 12, 24, "sea_lantern");
    }

    // ================================================================== mazmorras heladas

    private static void dungeon(StructBp b) {
        int fy = -12;
        int h = 8;
        // masa y salas
        b.box(-18, fy, 36, 18, fy + h + 1, 66, "deepslate_bricks");
        Room hall = new Room(b, -3, 38, 3, 64, fy, h).carve();
        for (int x = -3; x <= 3; x++) {
            for (int z = 38; z <= 64; z++) {
                b.set(x, fy, z, ((x + z) & 1) == 0 ? "polished_deepslate" : "deepslate_tiles");
            }
        }
        // celdas al oeste: 4 con barrotes y un prisionero helado en cada una
        for (int s : new int[]{-1}) {
            for (int i = 0; i < 4; i++) {
                int z = 40 + i * 6;
                int xa = s > 0 ? 5 : -13;
                int xb = s > 0 ? 13 : -5;
                b.air(xa, fy + 1, z, xb, fy + 5, z + 4);
                for (int x = xa; x <= xb; x++) {
                    for (int zz = z; zz <= z + 4; zz++) {
                        b.set(x, fy, zz, "cobbled_deepslate");
                    }
                }
                int gx = s > 0 ? 4 : -4;
                for (int zz = z; zz <= z + 4; zz++) {
                    for (int y = fy + 1; y <= fy + 5; y++) {
                        b.set(gx, y, zz, "iron_bars");
                    }
                }
                ironDoor(b, gx, fy + 1, z + 2, s > 0 ? "-x" : "+x", true);
                int px = s > 0 ? 11 : -11;
                // prisionero congelado: cubo de hielo con una calavera dentro
                b.box(px - 1, fy + 1, z + 1, px + 1, fy + 3, z + 3, "ice");
                b.set(px, fy + 2, z + 2, "skeleton_skull[rotation=" + (s > 0 ? 4 : 12) + "]");
                b.set(px, fy + 1, z + 2, "bone_block[axis=y]");
                b.set(s * 6, fy + 5, z + 2, "cobweb");
                hangLantern(b, s * 8, fy + h, z + 2, 1, SOUL);
            }
        }
        hall.baseboard("deepslate_brick_stairs").cornice("deepslate_brick_stairs");
        for (int z = 42; z <= 62; z += 6) {
            chandelier(b, 0, fy + h, z, 2, SOUL);
        }
        // sala de la cripta de hielo al fondo
        Room crypt = new Room(b, -9, 68, 9, 80, fy, 9);
        b.box(-11, fy, 67, 11, fy + 11, 82, "deepslate_bricks");
        crypt.carve();
        for (int x = -9; x <= 9; x++) {
            for (int z = 68; z <= 80; z++) {
                b.set(x, fy, z, ((x + z) & 1) == 0 ? "blue_ice" : "packed_ice");
            }
        }
        openZ(b, 0, fy + 1, 65, 67, 2, 6, 2, "chiseled_deepslate", null);
        for (int s : new int[]{-1, 1}) {
            for (int z = 70; z <= 78; z += 4) {
                sarcophagus(b, s * 6 - (s > 0 ? 0 : 1), fy + 1, z, 'z', 2, "packed_ice", "quartz_slab", QS);
                column(b, s * 3, z, fy + 1, fy + 8, "quartz_pillar[axis=y]", QS, CQ);
            }
        }
        // altar de hielo
        b.box(-2, fy + 1, 78, 2, fy + 1, 80, BI);
        b.box(-1, fy + 2, 79, 1, fy + 2, 79, CQ);
        b.set(0, fy + 3, 79, "soul_campfire[lit=true]");
        crystal(b, 0, 80, fy + 3, 8, 1, 0, 0, 3, BI, "light_blue_stained_glass");
        crypt.baseboard(QS).torches(6, fy + 3, true);
        // sala de los carceleros al este y escalera desde la nave (bajo la galería)
        Room guard = new Room(b, 5, 40, 13, 56, fy, 6).carve();
        for (int x = 5; x <= 13; x++) {
            for (int z = 40; z <= 56; z++) {
                b.set(x, fy, z, "cobbled_deepslate");
            }
        }
        openX(b, 48, fy + 1, 4, 4, 1, 4, 1, "polished_deepslate", null);
        guard.baseboard("deepslate_brick_stairs").torches(6, fy + 3, true);
        for (int k = 0; k < 11; k++) {
            int z = 38 + k;
            int y = -1 - k;
            for (int x = 9; x <= 11; x++) {
                b.air(x, y + 1, z, x, Math.min(y + 4, 1), z);
                b.set(x, y, z, st(QS, "-z"));
                b.set(x, y - 1, z, Q);
            }
        }
        // mesa y sillas de los carceleros, barriles y armas
        table(b, 6, fy + 1, 52, 'x', 4, "spruce_slab", "spruce_fence");
        chair(b, 7, fy + 1, 51, "+z", "spruce_stairs");
        chair(b, 9, fy + 1, 51, "+z", "spruce_stairs");
        candles(b, 8, fy + 2, 52, 2);
        for (int z = 54; z <= 56; z++) {
            b.set(13, fy + 1, z, "barrel[facing=up]");
        }
        b.set(13, fy + 1, 41, "anvil[facing=north]");
        b.set(12, fy + 1, 41, "grindstone[face=floor,facing=north]");
        hangLantern(b, 9, fy + 6, 52, 1, SOUL);
        // barandilla alrededor del hueco de la escalera, en la nave
        for (int z = 37; z <= 48; z++) {
            b.set(8, 0, z, "iron_bars");
            b.set(12, 0, z, "iron_bars");
        }
        for (int x = 8; x <= 12; x++) {
            b.set(x, 0, 48, "iron_bars");
        }
    }

    // ================================================================== acabado

    private static void finishing(StructBp b) {
        // abetos nevados fuera del foso
        int[][] trees = {{-38, 6}, {38, 5}, {-40, 28}, {40, 30}, {-39, 52}, {39, 54}, {-37, 76}, {37, 78}, {-20, 86}, {20, 88}, {0, 90}, {-30, 84}, {30, 86}};
        for (int[] t : trees) {
            spruce(b, t[0], t[1], 13 + b.r.nextInt(7), "spruce_log", "spruce_leaves[persistent=true]");
        }
        for (int i = 0; i < 20; i++) {
            crystalPatch(b, -40 + b.r.nextInt(80), -10 + b.r.nextInt(8) * 12 + b.r.nextInt(5));
        }
        // carámbanos bajo los aleros y nieve en lo alto
        icicles(b, "quartz_stairs", 12, 0.18, 3, "ice");
        icicles(b, "prismarine_brick_stairs", 12, 0.22, 4, "ice");
        icicles(b, "packed_ice", 20, 0.10, 4, "ice");
        snowCover(b, 77, 0.45, 12);
        // nieve suelta en el suelo
        groundCover(b, -41, -15, 41, 84, -1, 0.06, "snow[layers=1]", "snow[layers=2]");
        // reflejos de color: algo de hielo azul en la base del zócalo
        weather(b, 31, "calcite", "snow_block", 3.0, 0.55);
    }
}
