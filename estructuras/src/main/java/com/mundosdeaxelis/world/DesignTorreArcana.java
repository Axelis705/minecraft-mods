package com.mundosdeaxelis.world;

/**
 * Torre Arcana (etapa 261): la estructura de la torre, de tamaño mediano (como el fortín y la fragua). La torre donde estudiaba el
 * Archimago antes de encerrarse en su fortaleza: una torre octogonal de piedra oscura con esquinas de púrpura, tres pisos y un
 * observatorio con cúpula de cristal violeta. Todo gira alrededor de una escalera de caracol central.
 * <ul>
 * <li>Planta baja: el vestíbulo (estanterías, una estatua de caballero, alfombras moradas) y la despensa del Archimago, pegada a la
 * torre por el este (era la celda de Teo: etapa 265, Teo pasó a la Fortaleza).</li>
 * <li>Primer piso: la biblioteca (estanterías altas, atriles, una mesa de estudio con velas). Vuelan libros embrujados.</li>
 * <li>Segundo piso: el laboratorio de alquimia (soportes de pociones, calderos, frascos, amatistas).</li>
 * <li>Observatorio: un telescopio de cobre apuntando a la cúpula, un mapa de estrellas en el suelo y, en el centro, sobre la columna,
 * el tesoro del Archimago: un cofre con la cerradura más difícil (etapa 265: es la estructura chica del tier de la magia, la del
 * botín; la Piedra de Portal: Torre está en la Fortaleza).</li>
 * </ul>
 * Fuera: un patio con el muro en ruinas, un círculo arcano en el suelo, un cerezo, columnas caídas y una fuente seca.
 * <p>Enemigos: esbirros en el patio y en el laboratorio, una armadura encantada en el vestíbulo, libros embrujados en la biblioteca
 * y otra armadura encantada en el observatorio, que guarda el tesoro. Entrada por z = 0.
 */
final class DesignTorreArcana {
    private DesignTorreArcana() {
    }

    private static final String M = "mundos_de_axelis:";
    private static final String DB = "deepslate_bricks";
    private static final String CDB = "cracked_deepslate_bricks";
    private static final String DT = "deepslate_tiles";
    private static final String PD = "polished_deepslate";
    private static final String CD = "chiseled_deepslate";
    private static final String PP = "purpur_pillar[axis=y]";
    private static final String PPS = "purpur_stairs";
    private static final String DBW = "deepslate_brick_wall";
    private static final String GP = "purple_stained_glass_pane";

    /** Centro de la torre (z), radio exterior del octógono y pisos (y del suelo de cada uno). */
    static final int TZ = 30, R = 7;
    static final int[] FLOOR = {-1, 7, 15, 23};
    /** Patio con muro: x -WX..WX, z WZ0..WZ1. */
    static final int WX = 14, WZ0 = 6, WZ1 = 41;
    /** Despensa (al este de la torre; era la celda de Teo). */
    static final int CX0 = 8, CX1 = 12;
    static final int NPC_X = 10, NPC_Z = TZ;

    static StructBp make() {
        StructBp b = new StructBp(2611);
        ground(b);
        yardWall(b);
        yard(b);
        tower(b);
        stairs(b);
        hall(b);
        library(b);
        lab(b);
        observatory(b);
        cell(b);
        surroundings(b);
        chests(b);
        mobs(b);
        return b;
    }

    // ================================================================== octógono

    static int sum(int r) {
        return (int) Math.round(1.4142 * r);
    }

    static boolean inO(int x, int dz, int r) {
        int ax = Math.abs(x), az = Math.abs(dz);
        return ax <= r && az <= r && ax + az <= sum(r);
    }

    static boolean wall(int x, int dz) {
        return inO(x, dz, R) && !inO(x, dz, R - 1);
    }

    /** Esquinas del octógono: allí van las columnas de púrpura. */
    static boolean corner(int x, int dz) {
        int ax = Math.abs(x), az = Math.abs(dz);
        return (ax == 3 && az == R) || (ax == R && az == 3);
    }

    private static String stone(long seed, int x, int y, int z) {
        double h = StructKit.hash01(seed + y * 31L, x, z);
        return h < 0.62 ? DB : h < 0.8 ? CDB : h < 0.92 ? "deepslate_tiles" : "cracked_deepslate_tiles";
    }

    // ================================================================== suelo y patio

    private static void ground(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                double n = StructKit.fbm(2612, x, z, 5.0) + (StructKit.hash01(2613, x, z) - 0.5) * 0.2;
                String s = n < 0.45 ? "grass_block" : n < 0.62 ? "moss_block" : n < 0.78 ? "podzol" : n < 0.9 ? "coarse_dirt" : "mossy_cobblestone";
                b.set(x, -1, z, s);
                b.set(x, -2, z, "dirt");
            }
        }
        // camino de losas desde la puerta del patio hasta la torre
        for (int z = WZ0; z <= TZ - R; z++) {
            for (int x = -2; x <= 2; x++) {
                double h = StructKit.hash01(2614, x, z);
                b.set(x, -1, z, Math.abs(x) == 2 ? (h < 0.5 ? "mossy_cobblestone" : "cobbled_deepslate") : h < 0.25 ? "cracked_deepslate_tiles" : DT);
            }
        }
    }

    private static void yardWall(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                boolean edge = Math.abs(x) == WX || z == WZ0 || z == WZ1;
                if (!edge || (z == WZ0 && Math.abs(x) <= 2)) {
                    continue;
                }
                // muro en ruinas: de 1 a 3 de alto, con algún hueco
                double n = StructKit.vnoise(2615, x * 0.28, z * 0.28);
                int h = (int) Math.round(n * 3.2);
                if (h <= 0 && StructKit.hash01(2616, x, z) < 0.5) {
                    h = 1;
                }
                for (int y = 0; y < h; y++) {
                    b.set(x, y, z, y == h - 1 && h >= 2 ? DBW : stone(2617, x, y, z));
                }
                if (h > 0 && StructKit.hash01(2618, x, z) < 0.15) {
                    b.set(x, h, z, "moss_carpet");
                }
            }
        }
        // pilares de la puerta del patio con un cristal de amatista encima
        for (int sx : new int[]{-3, 3}) {
            for (int y = 0; y <= 4; y++) {
                b.set(sx, y, WZ0, y == 4 ? CD : PP);
            }
            b.set(sx, 5, WZ0, "amethyst_block");
            b.set(sx, 6, WZ0, "amethyst_cluster[facing=up]");
            b.set(sx, 3, WZ0 + 1, "soul_lantern[hanging=false]");
            b.set(sx, 2, WZ0 + 1, DBW);
            b.set(sx, 1, WZ0 + 1, DBW);
            b.set(sx, 0, WZ0 + 1, DBW);
        }
    }

    private static void yard(StructBp b) {
        // círculo arcano en el suelo: anillo de calcita y amatista con velas moradas
        int cx = -8, cz = 15;
        for (int x = cx - 3; x <= cx + 3; x++) {
            for (int z = cz - 3; z <= cz + 3; z++) {
                double d = Math.hypot(x - cx, z - cz);
                if (d > 2.5 && d < 3.5) {
                    b.set(x, -1, z, ((x + z) & 1) == 0 ? "calcite" : "amethyst_block");
                } else if (d <= 0.5) {
                    b.set(x, -1, z, "crying_obsidian");
                } else if (d < 2.5) {
                    b.set(x, -1, z, "polished_deepslate");
                }
            }
        }
        for (int[] o : new int[][]{{cx - 3, cz}, {cx + 3, cz}, {cx, cz - 3}, {cx, cz + 3}}) {
            b.set(o[0], 0, o[1], "purple_candle[candles=3,lit=true]");
        }
        // fuente seca con un cristal en medio
        int fx = 8, fz = 14;
        for (int x = fx - 2; x <= fx + 2; x++) {
            for (int z = fz - 2; z <= fz + 2; z++) {
                boolean rim = Math.abs(x - fx) == 2 || Math.abs(z - fz) == 2;
                b.set(x, -1, z, rim ? PD : "mossy_cobblestone");
                if (rim) {
                    b.set(x, 0, z, StructBp.slab("deepslate_brick_slab", false));
                }
            }
        }
        b.set(fx, 0, fz, DBW);
        b.set(fx, 1, fz, DBW);
        b.set(fx, 2, fz, "amethyst_block");
        b.set(fx, 3, fz, "amethyst_cluster[facing=up]");
        // columnas caídas y una entera
        for (int z = 10; z <= 13; z++) {
            b.set(-11, 0, z, "purpur_pillar[axis=z]");
        }
        b.set(-11, 0, 9, CD);
        for (int y = 0; y <= 3; y++) {
            b.set(11, y, 20, y == 3 ? CD : PP);
        }
        b.set(10, 0, 20, "purpur_pillar[axis=x]");
        b.set(11, 4, 20, "soul_lantern[hanging=false]");
        // farolas del camino
        for (int z : new int[]{11, 17}) {
            for (int sx : new int[]{-3, 3}) {
                b.set(sx, 0, z, DBW);
                b.set(sx, 1, z, DBW);
                b.set(sx, 2, z, "soul_lantern[hanging=false]");
            }
        }
        // un cerezo en la esquina de atrás y helechos y flores sueltos
        StructKit.bigTree(b, -10, 36, 7, "cherry_log", new String[]{"cherry_leaves[persistent=true]"}, 4);
        StructKit.groundCover(b, -WX + 1, WZ0 + 1, WX - 1, WZ1 - 1, -1, 0.08, "fern", "grass", "allium", "lily_of_the_valley");
    }

    // ================================================================== la torre

    private static void tower(StructBp b) {
        int top = FLOOR[3] + 5; // muros de piedra hasta el observatorio, luego cristal
        for (int x = -R; x <= R; x++) {
            for (int dz = -R; dz <= R; dz++) {
                if (!inO(x, dz, R)) {
                    continue;
                }
                int z = TZ + dz;
                b.set(x, -2, z, "cobbled_deepslate");
                boolean w = wall(x, dz);
                for (int y = -1; y <= top; y++) {
                    boolean floor = false;
                    for (int f : FLOOR) {
                        floor |= y == f;
                    }
                    String s;
                    if (w) {
                        if (y > FLOOR[3]) {
                            s = corner(x, dz) ? PP : GP; // el observatorio: cristal violeta entre columnas
                        } else if (corner(x, dz)) {
                            s = PP;
                        } else if (floor) {
                            s = PD; // franja en cada piso
                        } else {
                            s = stone(2620, x, y, z);
                        }
                    } else if (floor) {
                        s = y == -1 ? (((x + dz) & 1) == 0 ? PD : DT) : (y == FLOOR[3] ? "deepslate_tiles" : "dark_oak_planks");
                    } else {
                        s = "air";
                    }
                    b.set(x, y, z, s);
                }
            }
        }
        // zócalo un poco más ancho
        for (int x = -R - 1; x <= R + 1; x++) {
            for (int dz = -R - 1; dz <= R + 1; dz++) {
                if (inO(x, dz, R + 1) && !inO(x, dz, R)) {
                    b.set(x, 0, TZ + dz, StructBp.slab("deepslate_tile_slab", false));
                    b.set(x, -1, TZ + dz, "cobbled_deepslate");
                }
            }
        }
        // puerta: arco de 3 de ancho y 4 de alto con marco de púrpura
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                b.set(x, y, TZ - R, "air");
            }
            b.set(x, 0, TZ - R - 1, "air");
            b.set(x, -1, TZ - R - 1, DT);
        }
        for (int y = 0; y <= 4; y++) {
            b.set(-2, y, TZ - R, PP);
            b.set(2, y, TZ - R, PP);
        }
        b.set(-1, 4, TZ - R, StructBp.stair(PPS, "+x", true));
        b.set(0, 4, TZ - R, "purpur_block");
        b.set(1, 4, TZ - R, StructBp.stair(PPS, "-x", true));
        b.set(0, 5, TZ - R, CD);
        // ventanas: 2 de alto en el centro de las caras planas (abajo solo al oeste y al norte)
        for (int i = 0; i < 3; i++) {
            int y0 = FLOOR[i] + 3;
            for (int y = y0; y <= y0 + 1; y++) {
                b.set(-R, y, TZ, GP);
                b.set(0, y, TZ + R, GP);
                if (i > 0) {
                    b.set(R, y, TZ, GP);
                    b.set(0, y, TZ - R, GP);
                }
            }
        }
        dome(b, FLOOR[3] + 6);
        outside(b);
    }

    /** Cornisas en cada piso, el balcón del observatorio, estandartes en la puerta y enredaderas por los muros. */
    private static void outside(StructBp b) {
        for (int x = -R - 2; x <= R + 2; x++) {
            for (int dz = -R - 2; dz <= R + 2; dz++) {
                if (!inO(x, dz, R + 2) || inO(x, dz, R)) {
                    continue;
                }
                int z = TZ + dz;
                boolean outer = !inO(x, dz, R + 1);
                // cornisa fina en los pisos 1 y 2 (solo el primer anillo)
                if (!outer) {
                    for (int f = 1; f <= 2; f++) {
                        b.set(x, FLOOR[f], z, StructBp.slab("deepslate_tile_slab", true));
                    }
                }
                // balcón del observatorio: dos de ancho, con barandilla en el borde
                b.set(x, FLOOR[3], z, outer ? StructBp.slab("deepslate_tile_slab", true) : PD);
                b.set(x, FLOOR[3] + 1, z, outer ? DBW : "air");
                b.set(x, FLOOR[3] + 2, z, "air");
            }
        }
        // salida al balcón por el oeste (un hueco en el cristal)
        for (int y = FLOOR[3] + 1; y <= FLOOR[3] + 2; y++) {
            b.set(-R, y, TZ, "air");
        }
        // estandartes morados a los lados de la puerta
        for (int sx : new int[]{-3, 3}) {
            b.set(sx, 3, TZ - R - 1, "purple_wall_banner[facing=north]");
        }
        // enredaderas por las caras oeste y norte
        for (int i = 0; i < 9; i++) {
            boolean west = i % 2 == 0;
            int u = (int) Math.round((StructKit.hash01(2640 + i, i, 3) - 0.5) * 6);
            int top = 6 + (int) (StructKit.hash01(2650 + i, i, 5) * 14);
            int len = 3 + (int) (StructKit.hash01(2660 + i, i, 7) * 6);
            for (int y = top; y > top - len && y > 1; y--) {
                if (west) {
                    b.setIfAbsent(-R - 1, y, TZ + u, "vine[east=true]");
                } else {
                    b.setIfAbsent(u, y, TZ + R + 1, "vine[north=true]");
                }
            }
        }
    }

    /** Cúpula escalonada de pizarra con anillos de cristal violeta, una ranura para el telescopio y un cristal arriba. */
    private static void dome(StructBp b, int y0) {
        int[] rr = {7, 6, 5, 4, 3, 2};
        for (int i = 0; i < rr.length; i++) {
            int r = rr[i], y = y0 + i;
            for (int x = -r; x <= r; x++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (!inO(x, dz, r)) {
                        continue;
                    }
                    boolean rim = !inO(x, dz, r - 1);
                    int z = TZ + dz;
                    // la ranura: una franja abierta al norte por donde mira el telescopio
                    boolean slot = x >= -1 && x <= 1 && dz >= 3 && i >= 1 && i <= 4;
                    if (slot) {
                        b.set(x, y, z, "air");
                        continue;
                    }
                    if (rim || i == rr.length - 1) {
                        b.set(x, y, z, i % 2 == 0 ? (corner(x, dz) && r == R ? "purpur_block" : DT) : "purple_stained_glass");
                    } else if (b.get(x, y, z) == null) {
                        b.set(x, y, z, "air");
                    }
                }
            }
        }
        int ytop = y0 + rr.length;
        b.set(0, ytop, TZ, "amethyst_block");
        b.set(0, ytop + 1, TZ, "lightning_rod[facing=up]");
        // bordes de la ranura (losas) para que no quede un escalón suelto
        for (int i = 1; i <= 4; i++) {
            for (int sx : new int[]{-2, 2}) {
                int r = rr[i];
                for (int dz = 3; dz <= r; dz++) {
                    if (inO(sx, dz, r) && !inO(sx, dz, r - 1)) {
                        b.set(sx, y0 + i, TZ + dz, DT);
                    }
                }
            }
        }
    }

    // ================================================================== escalera de caracol

    private static void stairs(StructBp b) {
        // un escalón por bloque de altura: el último (el 23) queda a ras del suelo del observatorio
        StructKit.spiral(b, 0, TZ, 0, FLOOR[3] + 1, "deepslate_tile_stairs", "purpur_pillar[axis=y]", true);
        int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        for (int f = 1; f < FLOOR.length; f++) {
            int y = FLOOR[f];
            // el hueco del caracol en cada piso: tapado donde no estorba la cabeza; abiertas las tres casillas de antes del rellano
            for (int c = 0; c <= 3; c++) {
                b.set(ring[c][0], y, TZ + ring[c][1], y == FLOOR[3] ? "deepslate_tiles" : "dark_oak_planks");
            }
            for (int c = 4; c <= 6; c++) {
                if (b.get(ring[c][0], y, TZ + ring[c][1]) == null || !b.get(ring[c][0], y, TZ + ring[c][1]).contains("stairs")) {
                    b.set(ring[c][0], y, TZ + ring[c][1], "air");
                }
            }
            // el rellano al oeste del último escalón, despejado
            for (int yy = y + 1; yy <= y + 3 && yy <= FLOOR[3] + 3; yy++) {
                b.set(-2, yy, TZ, "air");
            }
        }
        // arriba la columna se corta a ras del suelo y hace de base del altar
        for (int y = FLOOR[3] + 1; y <= FLOOR[3] + 3; y++) {
            b.set(0, y, TZ, "air");
        }
    }

    // ================================================================== planta baja: vestíbulo

    private static void hall(StructBp b) {
        int y0 = 0;
        // alfombra morada del arco a la escalera
        for (int dz = -6; dz <= -2; dz++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, y0, TZ + dz, x == 0 ? "purple_carpet" : "magenta_carpet");
            }
        }
        // estanterías contra el muro (oeste y noroeste)
        for (int dz = -2; dz <= 2; dz++) {
            if (dz == 0) {
                continue;
            }
            b.set(-6, 0, TZ + dz, "bookshelf");
            b.set(-6, 1, TZ + dz, "bookshelf");
        }
        b.set(-6, 2, TZ - 2, "candle[candles=2,lit=true]");
        b.set(-6, 2, TZ + 2, "candle[candles=1,lit=true]");
        // pedestal con un cristal junto a la puerta
        b.set(-4, 0, TZ - 5, CD);
        b.set(-4, 1, TZ - 5, "amethyst_block");
        b.set(-4, 2, TZ - 5, "amethyst_cluster[facing=up]");
        // bancos y una mesa con un mapa
        b.set(4, 0, TZ - 5, StructBp.stair("dark_oak_stairs", "-x", false));
        b.set(5, 0, TZ - 4, "dark_oak_planks");
        b.set(5, 1, TZ - 4, "brown_candle[candles=1,lit=true]");
        b.set(5, 0, TZ + 4, "barrel[facing=up]");
        b.set(4, 0, TZ + 5, "barrel[facing=up]");
        b.set(4, 1, TZ + 5, "potted_azure_bluet");
        // faroles colgados del techo
        StructKit.hangLantern(b, -3, FLOOR[1] - 1, TZ + 3, 1, "soul_lantern");
        StructKit.hangLantern(b, 3, FLOOR[1] - 1, TZ - 3, 1, "soul_lantern");
        StructKit.hangLantern(b, 3, FLOOR[1] - 1, TZ + 3, 1, "lantern");
        b.set(-5, 3, TZ + 4, "soul_wall_torch[facing=south]");
        // luz junto a la celda: antorcha de almas entre los barrotes y la puerta, y un farol en el suelo
        b.set(R - 1, 2, TZ, "soul_wall_torch[facing=west]");
        b.set(R - 2, 0, TZ - 3, "lantern");
    }

    // ================================================================== primer piso: biblioteca

    private static void library(StructBp b) {
        int y0 = FLOOR[1] + 1;
        for (int x = -6; x <= 6; x++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (!inO(x, dz, R - 1) || inO(x, dz, R - 2)) {
                    continue;
                }
                // estanterías altas contra todo el muro, menos ventanas y el rellano de la escalera
                boolean window = (x == -6 && dz == 0) || (x == 6 && dz == 0) || (x == 0 && Math.abs(dz) == 6);
                if (window) {
                    continue;
                }
                for (int y = y0; y <= y0 + 2; y++) {
                    b.set(x, y, TZ + dz, (x * 7 + dz * 3 + y) % 9 == 0 ? "chiseled_bookshelf" : "bookshelf");
                }
            }
        }
        // mesa de estudio con velas, atriles y una alfombra
        for (int x = 2; x <= 4; x++) {
            b.set(x, y0, TZ + 3, StructBp.slab("dark_oak_slab", true));
        }
        b.set(2, y0 + 1, TZ + 3, "candle[candles=3,lit=true]");
        b.set(4, y0 + 1, TZ + 3, "lectern[facing=south]");
        b.set(3, y0, TZ + 2, StructBp.stair("dark_oak_stairs", "+z", false));
        b.set(-3, y0, TZ - 3, "lectern[facing=north]");
        b.set(3, y0, TZ - 3, "lectern[facing=west]");
        for (int x = -4; x <= -2; x++) {
            for (int dz = 2; dz <= 4; dz++) {
                b.set(x, y0, TZ + dz, ((x + dz) & 1) == 0 ? "purple_carpet" : "blue_carpet");
            }
        }
        StructKit.hangLantern(b, 3, FLOOR[2] - 1, TZ, 1, "lantern");
        StructKit.hangLantern(b, -3, FLOOR[2] - 1, TZ - 2, 1, "soul_lantern");
        // libros por el suelo y una escalera de mano a las estanterías altas
        b.set(-4, y0, TZ - 1, "brown_carpet");
    }

    // ================================================================== segundo piso: laboratorio

    private static void lab(StructBp b) {
        int y0 = FLOOR[2] + 1;
        // mesa de trabajo en U con soportes de pociones y calderos
        for (int dz = -2; dz <= 2; dz++) {
            b.set(5, y0, TZ + dz, "polished_deepslate");
        }
        b.set(5, y0 + 1, TZ - 2, "brewing_stand");
        b.set(5, y0 + 1, TZ, "brewing_stand");
        b.set(5, y0 + 1, TZ + 2, "amethyst_cluster[facing=up]");
        b.set(5, y0 + 1, TZ + 1, "potted_crimson_fungus");
        b.set(4, y0, TZ - 4, "water_cauldron[level=3]");
        b.set(3, y0, TZ - 5, "cauldron");
        b.set(-4, y0, TZ + 4, "water_cauldron[level=2]");
        // estantes con frascos (macetas, velas y amatistas) contra el muro
        for (int x = -3; x <= 3; x++) {
            if (Math.abs(x) <= 1) {
                continue;
            }
            b.set(x, y0 + 2, TZ + 5, StructBp.slab("dark_oak_slab", true));
            b.set(x, y0 + 3, TZ + 5, (x & 1) == 0 ? "purple_candle[candles=2,lit=true]" : "small_amethyst_bud[facing=up]");
        }
        b.set(-5, y0, TZ - 2, "barrel[facing=east]");
        b.set(-5, y0 + 1, TZ - 2, "potted_warped_fungus");
        b.set(-5, y0, TZ + 1, "smoker[facing=east,lit=true]");
        b.set(-4, y0, TZ - 4, "amethyst_block");
        b.set(-4, y0 + 1, TZ - 4, "large_amethyst_bud[facing=up]");
        StructKit.hangLantern(b, 3, FLOOR[3] - 1, TZ + 2, 1, "soul_lantern");
        StructKit.hangLantern(b, -3, FLOOR[3] - 1, TZ - 2, 1, "lantern");
        b.set(-3, y0, TZ + 3, "magenta_carpet");
        b.set(-2, y0, TZ + 3, "magenta_carpet");
    }

    // ================================================================== observatorio

    private static void observatory(StructBp b) {
        int y0 = FLOOR[3] + 1;
        // el tesoro del Archimago sobre la columna del caracol: el cofre con la cerradura más difícil (el final de la torre)
        b.set(0, y0, TZ, CD);
        b.setNbt(0, y0 + 1, TZ, M + "vault_chest_locked[facing=south]", "{Kind:\"LOCKED\",Stage:4,LockLevel:5}");
        // mapa de estrellas: losas negras con puntos de luz alrededor
        for (int x = -5; x <= 5; x++) {
            for (int dz = -5; dz <= 5; dz++) {
                if (!inO(x, dz, R - 1) || (Math.abs(x) <= 1 && Math.abs(dz) <= 1) || (x == -2 && dz == 0)) {
                    continue;
                }
                double h = StructKit.hash01(2625, x, dz);
                if (h < 0.12) {
                    b.set(x, FLOOR[3], TZ + dz, "sea_lantern");
                } else if (h < 0.2) {
                    b.set(x, FLOOR[3], TZ + dz, "crying_obsidian");
                } else {
                    b.set(x, FLOOR[3], TZ + dz, ((x + dz) & 1) == 0 ? "black_concrete" : "deepslate_tiles");
                }
            }
        }
        // el telescopio de cobre en su trípode, apuntando a la ranura de la cúpula (norte)
        b.set(-1, y0, TZ + 3, "dark_oak_fence");
        b.set(1, y0, TZ + 3, "dark_oak_fence");
        b.set(0, y0, TZ + 2, "dark_oak_fence");
        b.set(0, y0 + 1, TZ + 3, "waxed_cut_copper");
        b.set(0, y0 + 2, TZ + 4, "waxed_cut_copper");
        b.set(0, y0 + 3, TZ + 5, "waxed_copper_block");
        b.set(0, y0 + 4, TZ + 5, "lightning_rod[facing=up]");
        // velas y un atril con el diario de estrellas
        b.set(-3, y0, TZ - 4, "lectern[facing=north]");
        b.set(3, y0, TZ + 4, "purple_candle[candles=4,lit=true]");
        b.set(-4, y0, TZ + 3, "purple_candle[candles=2,lit=true]");
        b.set(4, y0, TZ - 2, "end_rod[facing=up]");
        b.set(-5, y0, TZ - 1, "end_rod[facing=up]");
    }

    // ================================================================== la celda

    private static void cell(StructBp b) {
        for (int x = CX0; x <= CX1; x++) {
            for (int dz = -3; dz <= 3; dz++) {
                int z = TZ + dz;
                b.set(x, -1, z, StructKit.hash01(2630, x, z) < 0.35 ? CDB : DB);
                b.set(x, -2, z, "cobbled_deepslate");
                boolean edge = x == CX1 || Math.abs(dz) == 3;
                for (int y = 0; y <= 4; y++) {
                    b.set(x, y, z, y == 4 || edge ? stone(2631, x, y, z) : "air");
                }
                b.set(x, 5, z, StructBp.slab("deepslate_tile_slab", false));
            }
        }
        // barrotes en el muro de la torre (se ve al preso desde el vestíbulo) y la puerta de hierro
        for (int dz = -2; dz <= -1; dz++) {
            for (int y = 0; y <= 2; y++) {
                b.set(R, y, TZ + dz, "iron_bars");
            }
        }
        StructKit.woodDoor(b, "dark_oak", R, 0, TZ + 1, "-x", false);
        b.set(R, 2, TZ + 1, DB);
        // dentro: barriles, sacos (lana), un caldero, la pila de libros y un cofre de premio por subir a la torre (etapa 265)
        b.set(CX1 - 1, 0, TZ - 2, "barrel[facing=up]");
        b.set(CX1 - 2, 0, TZ - 2, "barrel[facing=west]");
        b.set(CX1 - 1, 1, TZ - 2, "barrel[facing=up]");
        b.set(CX1 - 1, 0, TZ + 2, "water_cauldron[level=1]");
        b.set(CX1 - 1, 0, TZ, "brown_wool");
        b.set(CX1 - 1, 0, TZ - 1, "white_wool");
        b.set(CX0, 0, TZ + 2, "chiseled_bookshelf");
        b.set(CX0 + 1, 0, TZ - 2, "candle[candles=2,lit=true]");
        StructKit.hangLantern(b, NPC_X, 3, NPC_Z, 1, "soul_lantern");
        b.set(CX1 - 1, 3, TZ + 2, "cobweb");
        b.set(CX0, 3, TZ - 2, "cobweb");
    }

    // ================================================================== alrededor

    static double edge(int x, int z) {
        double dx = Math.max(0, Math.abs(x) - (WX + 2));
        double dz = Math.max(Math.max(0, (WZ0 - 3) - z), Math.max(0, z - (WZ1 + 3)));
        double out = Math.hypot(dx, dz);
        if (out > 0) {
            return out;
        }
        return -Math.min(Math.min((WX + 2) - Math.abs(x), z - (WZ0 - 3)), (WZ1 + 3) - z);
    }

    private static void surroundings(StructBp b) {
        int rr = 32;
        for (int x = -WX - rr; x <= WX + rr; x++) {
            for (int z = WZ0 - rr; z <= WZ1 + rr; z++) {
                double e = edge(x, z);
                double ramp = 7 + 3 * StructKit.vnoise(2632, x * 0.1, z * 0.1);
                double w = e <= 2 ? 0 : Math.min(1, (e - 2) / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 1 && z >= -6 && z < WZ0;
                if (w >= 0.999 && !road) {
                    continue;
                }
                String top = road ? (StructKit.hash01(2633, x, z) < 0.4 ? "cobbled_deepslate" : "gravel") : null;
                b.blend(x, z, road ? Math.min(w, 0.5) : w, top, null);
            }
        }
        // farolas del camino sobre el terreno real
        for (int sx : new int[]{-3, 3}) {
            StructBp.Anchor p = b.anchor(sx, -3);
            p.set(0, 0, 0, DBW);
            p.set(0, 1, 0, DBW);
            p.set(0, 2, 0, "soul_lantern[hanging=false]");
        }
        StructBp.Anchor rock = b.anchor(-7, 0);
        rock.set(0, 0, 0, "cobbled_deepslate");
        rock.set(1, 0, 0, "mossy_cobblestone");
        rock.set(0, 1, 0, "amethyst_cluster[facing=up]");
    }

    // ================================================================== cofre y enemigos

    private static void chests(StructBp b) {
        b.setNbt(-3, FLOOR[3] + 1, TZ + 3, M + "vault_chest[facing=east]", "{Kind:\"ANTE\",Stage:4}");
        // premio de la despensa
        b.setNbt(NPC_X, 0, NPC_Z, M + "vault_chest[facing=west]", "{Kind:\"TRIAL\",Stage:4}");
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        return (at == null || at.equals("air") || at.endsWith("_carpet")) && (up == null || up.equals("air"))
                && below != null && !below.equals("air") && !below.contains("stairs");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind, String tag) {
        String at = b.get(x, y, z);
        if (at != null && (at.equals("fern") || at.equals("grass") || at.equals("allium") || at.equals("lily_of_the_valley"))) {
            b.m.remove(StructBp.key(x, y, z)); // una planta del suelo: se quita para que quepa
        }
        if (free(b, x, y, z)) {
            b.mobs.add(new StructBp.Mob(x, y, z, kind, tag));
        } else {
            System.err.println("[TorreArcana] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    private static void mobs(StructBp b) {
        mob(b, -6, 0, 12, "esbirro", null);
        mob(b, 6, 0, 20, "esbirro", null);
        mob(b, 3, 0, TZ + 2, "armadura_encantada", null);
        mob(b, -3, FLOOR[1] + 1, TZ + 1, "libro_embrujado", null);
        mob(b, 2, FLOOR[1] + 1, TZ - 4, "libro_embrujado", null);
        mob(b, -2, FLOOR[2] + 1, TZ - 3, "esbirro", null);
        // la armadura del observatorio guarda el tesoro
        mob(b, 3, FLOOR[3] + 1, TZ - 3, "armadura_encantada", null);
    }
}
