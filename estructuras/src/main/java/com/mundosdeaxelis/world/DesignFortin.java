package com.mundosdeaxelis.world;

/**
 * Fortín de Escarcha (etapa 258): la estructura del hielo, de tamaño mediano. Una muralla baja de pizarra con almenas y cuatro
 * torres con chapitel, un portón con el rastrillo subido, un patio nevado (estanque helado, fogata con bancos de tronco, cajas,
 * carámbanos de hielo) y al fondo una gran sala nórdica de pizarra y abeto con tejado muy inclinado. Dentro, pilares, braseros
 * de fuego azul y, sobre un estrado, la mesa de mapas que los centinelas le quitaron a Bruno, con un cofre al lado. Pegada a la sala,
 * una celda con barrotes y puerta de hierro donde está preso Bruno: es el final de la estructura (etapa 264: la Piedra de Portal:
 * Hielo pasó a la Ciudadela de cristal, la estructura grande del hielo).
 * <p>Enemigos: dos Lobeznos de escarcha en el patio, uno en la sala y dos Centinelas de escarcha (el del estrado lleva la llave de
 * la celda). Entrada por z = 0; la muralla ocupa x -15..15, z 8..46; la sala x -9..9, z 24..43.
 */
final class DesignFortin {
    private DesignFortin() {
    }

    private static final String M = "mundos_de_axelis:";
    private static final String DB = "deepslate_bricks";
    private static final String CDB = "cracked_deepslate_bricks";
    private static final String CD = "cobbled_deepslate";
    private static final String PD = "polished_deepslate";
    private static final String DT = "deepslate_tiles";
    private static final String DTS = "deepslate_tile_stairs";
    private static final String DBW = "deepslate_brick_wall";
    private static final String LOG = "spruce_log[axis=y]";
    private static final String SOUL = "soul_lantern";
    /** Muralla y sala. */
    static final int WX = 15, WZ0 = 8, WZ1 = 46;
    static final int KX = 9, KZ0 = 24, KZ1 = 43;
    /** Celda (al este de la sala) y sitio del preso. */
    static final int CX1 = 14, CZ0 = 27, CZ1 = 33;
    static final int NPC_X = 12, NPC_Z = 30;
    /** La fogata del patio (sin nieve suelta cerca: se derretiría). */
    private static final int FIRE_X = 9, FIRE_Z = 15;

    static StructBp make() {
        StructBp b = new StructBp(2581);
        ground(b);
        outerWall(b);
        towers(b);
        gate(b);
        courtyard(b);
        keep(b);
        porch(b);
        cell(b);
        sheds(b);
        surroundings(b);
        finishing(b);
        chests(b);
        mobs(b);
        return b;
    }

    private static String wallMix(long seed, int x, int y, int z) {
        double h = StructKit.hash01(seed + y, x, z);
        return h < 0.55 ? DB : h < 0.78 ? CD : h < 0.92 ? CDB : "deepslate_tiles";
    }

    // ================================================================== suelo

    private static void ground(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                double n = StructKit.fbm(2582, x, z, 5.0) + (StructKit.hash01(2583, x, z) - 0.5) * 0.12;
                String s = n < 0.12 ? "packed_ice" : n < 0.82 ? "snow_block" : n < 0.92 ? "gravel" : CD;
                b.set(x, -1, z, s);
                b.set(x, -2, z, "dirt");
            }
        }
        // camino de la puerta a la sala
        for (int z = WZ0; z < KZ0; z++) {
            for (int x = -1; x <= 1; x++) {
                b.set(x, -1, z, StructKit.hash01(2584, x, z) < 0.3 ? CD : PD);
            }
        }
    }

    // ================================================================== muralla, torres y portón

    private static void outerWall(StructBp b) {
        for (int x = -WX; x <= WX; x++) {
            for (int z = WZ0; z <= WZ1; z++) {
                boolean edge = Math.abs(x) == WX || z == WZ0 || z == WZ1;
                if (!edge) {
                    continue;
                }
                b.set(x, -1, z, CD);
                for (int y = 0; y <= 3; y++) {
                    b.set(x, y, z, y == 0 ? CD : wallMix(2585, x, y, z));
                }
                // almenas
                boolean merlon = ((Math.abs(x) == WX ? z : x) & 1) == 0;
                b.set(x, 4, z, merlon ? DB : "snow[layers=2]");
            }
        }
    }

    private static void towers(StructBp b) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int cz : new int[]{WZ0, WZ1}) {
                int cx = sx * WX;
                for (int x = cx - 2; x <= cx + 2; x++) {
                    for (int z = cz - 2; z <= cz + 2; z++) {
                        b.set(x, -1, z, CD);
                        for (int y = 0; y <= 7; y++) {
                            boolean corner = Math.abs(x - cx) == 2 && Math.abs(z - cz) == 2;
                            b.set(x, y, z, corner ? (y == 0 ? CD : PD) : wallMix(2586, x, y, z));
                        }
                    }
                }
                // aspilleras
                for (int y = 3; y <= 4; y++) {
                    b.set(cx + sx * 2, y, cz, "air");
                    b.set(cx, y, cz + (cz == WZ0 ? -2 : 2), "air");
                }
                b.set(cx + sx * 2, 2, cz, StructBp.slab("deepslate_brick_slab", false));
                b.set(cx, 2, cz + (cz == WZ0 ? -2 : 2), StructBp.slab("deepslate_brick_slab", false));
                // chapitel de pizarra
                for (int x = cx - 2; x <= cx + 2; x++) {
                    for (int z = cz - 2; z <= cz + 2; z++) {
                        int dx = x - cx, dz = z - cz;
                        if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                            String up = Math.abs(dx) >= Math.abs(dz) ? (dx < 0 ? "+x" : "-x") : (dz < 0 ? "+z" : "-z");
                            b.set(x, 8, z, StructBp.stair(DTS, up, false));
                        } else if (Math.abs(dx) == 1 || Math.abs(dz) == 1) {
                            b.set(x, 8, z, DT);
                            String up = Math.abs(dx) >= Math.abs(dz) ? (dx < 0 ? "+x" : "-x") : (dz < 0 ? "+z" : "-z");
                            b.set(x, 9, z, StructBp.stair(DTS, up, false));
                        } else {
                            b.set(x, 8, z, DT);
                            b.set(x, 9, z, DT);
                            b.set(x, 10, z, DBW);
                            b.set(x, 11, z, DBW);
                            b.set(x, 12, z, "light_blue_stained_glass");
                        }
                    }
                }
            }
        }
    }

    private static void gate(StructBp b) {
        // cuerpo del portón: más alto que la muralla
        for (int x = -4; x <= 4; x++) {
            for (int z = WZ0 - 1; z <= WZ0 + 1; z++) {
                for (int y = 0; y <= 6; y++) {
                    b.set(x, y, z, Math.abs(x) == 4 && z != WZ0 ? PD : wallMix(2587, x, y, z));
                }
                b.set(x, 7, z, ((x + z) & 1) == 0 ? DB : "snow[layers=2]");
            }
        }
        // arco de 3 de ancho y 4 de alto, con el rastrillo subido
        for (int z = WZ0 - 1; z <= WZ0 + 1; z++) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 3; y++) {
                    b.set(x, y, z, "air");
                }
                b.set(x, -1, z, PD);
            }
            b.set(-1, 4, z, StructBp.stair("deepslate_brick_stairs", "-x", true));
            b.set(1, 4, z, StructBp.stair("deepslate_brick_stairs", "+x", true));
            b.set(0, 4, z, "chiseled_deepslate");
        }
        for (int x = -1; x <= 1; x++) {
            b.set(x, 3, WZ0, "iron_bars");
        }
        // estandartes azules a los lados por fuera y faroles
        b.set(-3, 4, WZ0 - 2, "light_blue_wall_banner[facing=north]");
        b.set(3, 4, WZ0 - 2, "light_blue_wall_banner[facing=north]");
        b.set(-2, 2, WZ0 - 2, "soul_lantern[hanging=false]");
        b.set(2, 2, WZ0 - 2, "soul_lantern[hanging=false]");
        b.set(-2, 1, WZ0 - 2, DBW);
        b.set(2, 1, WZ0 - 2, DBW);
        b.set(-2, 0, WZ0 - 2, PD);
        b.set(2, 0, WZ0 - 2, PD);
        b.set(-2, -1, WZ0 - 2, CD);
        b.set(2, -1, WZ0 - 2, CD);
    }

    // ================================================================== patio

    private static boolean nearFire(int x, int z) {
        return Math.abs(x - FIRE_X) <= 5 && Math.abs(z - FIRE_Z) <= 5;
    }

    private static void courtyard(StructBp b) {
        // estanque helado a la izquierda (hielo compacto: no se derrite)
        for (int x = -WX + 2; x <= -6; x++) {
            for (int z = 11; z <= 19; z++) {
                double d = Math.hypot((x + 10) / 3.6, (z - 15) / 3.2);
                if (d <= 1.0) {
                    b.set(x, -1, z, StructKit.hash01(2588, x, z) < 0.12 ? "blue_ice" : "packed_ice");
                    b.set(x, -2, z, "packed_ice");
                } else if (d <= 1.3) {
                    b.set(x, -1, z, StructKit.hash01(2589, x, z) < 0.5 ? "snow_block" : CD);
                }
            }
        }
        // carámbanos de hielo (picos de hielo compacto con la punta de hielo azul)
        int[][] spikes = {{-13, 22, 4}, {-12, 23, 2}, {-13, 43, 5}, {-11, 44, 3}, {13, 43, 4}, {12, 44, 2}, {-7, 10, 3}};
        for (int[] s : spikes) {
            for (int y = 0; y < s[2]; y++) {
                b.set(s[0], y, s[1], y == s[2] - 1 ? "blue_ice" : "packed_ice");
            }
        }
        // fogata con bancos de tronco a la derecha
        b.set(FIRE_X, -1, FIRE_Z, CD);
        b.set(FIRE_X, 0, FIRE_Z, "campfire[lit=true,signal_fire=false,facing=north]");
        for (int d = -1; d <= 1; d++) {
            b.set(FIRE_X - 2, 0, FIRE_Z + d, "stripped_spruce_log[axis=z]");
            b.set(FIRE_X + 2, 0, FIRE_Z + d, "stripped_spruce_log[axis=z]");
        }
        b.set(FIRE_X, 0, FIRE_Z + 2, "stripped_spruce_log[axis=x]");
        // cajas y barriles contra la muralla
        int[][] crates = {{13, 0, 19}, {13, 0, 20}, {12, 0, 20}, {13, 1, 20}, {13, 0, 11}, {12, 0, 11}};
        for (int[] c : crates) {
            b.set(c[0], c[1], c[2], StructKit.hash01(2590, c[0], c[2] + c[1]) < 0.5 ? "barrel[facing=up]" : "spruce_planks");
        }
        b.set(12, 1, 11, "lantern");
        // armero: postes con escudos de hielo
        for (int z = 21; z <= 22; z++) {
            b.set(-13, 0, z, "spruce_fence");
            b.set(-13, 1, z, "spruce_fence");
        }
        // farolas de fuego azul a lo largo del camino
        for (int z : new int[]{12, 19}) {
            for (int sx : new int[]{-3, 3}) {
                b.set(sx, 0, z, DBW);
                b.set(sx, 1, z, DBW);
                b.set(sx, 2, z, SOUL);
            }
        }
        // nieve suelta por el suelo (sin tapar el camino ni junto a la fogata)
        for (int x = -WX + 1; x <= WX - 1; x++) {
            for (int z = WZ0 + 1; z <= WZ1 - 1; z++) {
                String g = b.get(x, -1, z);
                if (g == null || !g.equals("snow_block") || b.get(x, 0, z) != null || Math.abs(x) <= 1 && z < KZ0 || nearFire(x, z)) {
                    continue;
                }
                double h = StructKit.hash01(2591, x, z);
                if (h < 0.35) {
                    b.set(x, 0, z, "snow[layers=" + (h < 0.1 ? 2 : 1) + "]");
                }
            }
        }
    }

    // ================================================================== la sala

    private static void keep(StructBp b) {
        // suelo de baldosas a cuadros
        for (int x = -KX; x <= KX; x++) {
            for (int z = KZ0; z <= KZ1; z++) {
                b.set(x, -1, z, ((x + z) & 1) == 0 ? PD : DT);
                b.set(x, -2, z, CD);
            }
        }
        // muros de pizarra con postes de abeto y vigas
        for (int y = 0; y <= 5; y++) {
            for (int x = -KX; x <= KX; x++) {
                for (int z = KZ0; z <= KZ1; z++) {
                    boolean edge = Math.abs(x) == KX || z == KZ0 || z == KZ1;
                    b.set(x, y, z, edge ? (y <= 1 ? (StructKit.hash01(2592 + y, x, z) < 0.5 ? CD : PD) : wallMix(2593, x, y, z)) : "air");
                }
            }
        }
        for (int x : new int[]{-KX, KX}) {
            for (int z : new int[]{KZ0, 35, KZ1}) {
                for (int y = 0; y <= 5; y++) {
                    b.set(x, y, z, LOG);
                }
            }
        }
        for (int x : new int[]{-4, 4}) {
            for (int z : new int[]{KZ0, KZ1}) {
                for (int y = 0; y <= 5; y++) {
                    b.set(x, y, z, LOG);
                }
            }
        }
        for (int z = KZ0; z <= KZ1; z++) {
            b.set(-KX, 6, z, "stripped_spruce_log[axis=z]");
            b.set(KX, 6, z, "stripped_spruce_log[axis=z]");
        }
        // vigas de lado a lado (y faroles de fuego azul colgados)
        for (int z : new int[]{29, 35, 40}) {
            for (int x = -KX + 1; x <= KX - 1; x++) {
                b.set(x, 6, z, "stripped_spruce_log[axis=x]");
            }
            StructKit.hangLantern(b, -4, 5, z, 1, SOUL);
            StructKit.hangLantern(b, 4, 5, z, 1, SOUL);
        }
        // tejado muy inclinado de pizarra (a dos aguas, hastiales de piedra)
        StructKit.gableZ(b, -KX, KX, KZ0, KZ1, 6, 1, 1, DTS, "spruce_planks", DT, true, 1);
        // tejas con algo de variedad y el alero de abeto oscuro
        for (int z = KZ0 - 1; z <= KZ1 + 1; z++) {
            for (int x = -KX - 1; x <= KX + 1; x++) {
                for (int y = 6; y <= 15; y++) {
                    String st = b.get(x, y, z);
                    if (st == null || !st.startsWith(DTS)) {
                        continue;
                    }
                    String props = st.substring(DTS.length());
                    if (y == 6) {
                        b.set(x, y, z, "dark_oak_stairs" + props);
                    } else if (StructKit.vnoise(2601, x * 0.4, z * 0.25 + y) > 0.68) {
                        b.set(x, y, z, "cobbled_deepslate_stairs" + props);
                    }
                }
            }
        }
        // cuernos cruzados de madera en las puntas de la cumbrera (estilo nórdico)
        for (int z : new int[]{KZ0 - 1, KZ1 + 1}) {
            b.set(-1, 16, z, StructBp.stair("dark_oak_stairs", "+x", false));
            b.set(1, 16, z, StructBp.stair("dark_oak_stairs", "-x", false));
            b.set(-1, 17, z, "dark_oak_fence");
            b.set(1, 17, z, "dark_oak_fence");
            b.set(-2, 18, z, "dark_oak_fence");
            b.set(2, 18, z, "dark_oak_fence");
        }
        for (int z : new int[]{KZ0, KZ1}) {
            for (int y = 7; y <= 15; y++) {
                for (int x = -KX; x <= KX; x++) {
                    String s = b.get(x, y, z);
                    if (s != null && s.equals("spruce_planks")) {
                        b.set(x, y, z, wallMix(2594, x, y, z));
                    }
                }
            }
            // rosetón de cristal azul en cada hastial
            for (int x = -1; x <= 1; x++) {
                for (int y = 9; y <= 11; y++) {
                    if (Math.abs(x) + Math.abs(y - 10) <= 1) {
                        b.set(x, y, z, "light_blue_stained_glass");
                    }
                }
            }
            b.set(0, 13, z, "chiseled_deepslate");
        }
        // nieve en la cumbrera
        for (int z = KZ0 - 1; z <= KZ1 + 1; z++) {
            if (StructKit.hash01(2595, 0, z) < 0.6) {
                b.set(0, 17, z, "snow[layers=1]");
            }
        }
        // puerta (arco de 3 de ancho y 4 de alto) y escalón
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 3; y++) {
                b.set(x, y, KZ0, "air");
            }
        }
        b.set(-1, 4, KZ0, StructBp.stair("deepslate_brick_stairs", "-x", true));
        b.set(1, 4, KZ0, StructBp.stair("deepslate_brick_stairs", "+x", true));
        b.set(0, 4, KZ0, "chiseled_deepslate");
        b.set(-2, 4, KZ0 - 1, "soul_lantern[hanging=true]");
        b.set(2, 4, KZ0 - 1, "soul_lantern[hanging=true]");
        b.set(-2, 5, KZ0 - 1, "chain");
        b.set(2, 5, KZ0 - 1, "chain");
        // ventanas de cristal azul (al este solo detrás de la celda)
        for (int y = 2; y <= 3; y++) {
            for (int z : new int[]{30, 38}) {
                b.set(-KX, y, z, "light_blue_stained_glass_pane");
            }
            b.set(KX, y, 38, "light_blue_stained_glass_pane");
        }
        // pilares de dentro
        for (int x : new int[]{-5, 5}) {
            for (int z : new int[]{29, 35}) {
                for (int y = 0; y <= 5; y++) {
                    b.set(x, y, z, y == 0 ? PD : LOG);
                }
            }
        }
        // alfombra azul hasta el estrado
        for (int z = KZ0 + 1; z <= 36; z++) {
            b.set(0, 0, z, "light_blue_carpet");
        }
        // braseros de fuego azul
        for (int x : new int[]{-7, 7}) {
            for (int z : new int[]{27, 33}) {
                b.set(x, 0, z, PD);
                b.set(x, 1, z, "soul_campfire[lit=true,signal_fire=false,facing=north]");
            }
        }
        // estrado con la mesa de mapas de Bruno (se la quitaron los centinelas)
        for (int x = -5; x <= 5; x++) {
            for (int z = 38; z <= KZ1 - 1; z++) {
                b.set(x, 0, z, Math.abs(x) <= 1 && z >= 39 ? "chiseled_deepslate" : PD);
            }
            b.set(x, 0, 37, StructBp.stair("polished_deepslate_stairs", "+z", false));
        }
        b.set(0, 1, 40, "cartography_table");
        b.set(0, 2, 40, "candle[candles=3,lit=true]");
        for (int sx : new int[]{-1, 1}) {
            b.set(sx * 2, 1, 40, "blue_ice");
            b.set(sx * 2, 2, 40, "packed_ice");
            b.set(sx * 3, 1, 41, "packed_ice");
            b.set(sx * 3, 2, 41, "packed_ice");
            b.set(sx * 3, 3, 41, "blue_ice");
            b.set(sx * 5, 1, 39, "soul_lantern");
        }
        // estandartes al fondo
        for (int x : new int[]{-3, 0, 3}) {
            b.set(x, 4, KZ1 - 1, "light_blue_wall_banner[facing=north]");
        }
        // mesas y bancos de abeto a los lados de la sala
        for (int z : new int[]{26, 31}) {
            b.set(-7, 0, z + 1, StructBp.stair("spruce_stairs", "-x", false));
            b.set(-7, 0, z + 2, "spruce_fence");
            b.set(-7, 1, z + 2, "spruce_pressure_plate");
        }
    }

    /** Porche de madera delante de la puerta de la sala. */
    private static void porch(StructBp b) {
        int z0 = KZ0 - 3;
        for (int sx : new int[]{-3, 3}) {
            for (int y = 0; y <= 3; y++) {
                b.set(sx, y, z0, "stripped_spruce_log[axis=y]");
            }
            b.set(sx, -1, z0, PD);
        }
        for (int z = z0; z < KZ0; z++) {
            for (int x = -4; x <= 4; x++) {
                b.set(x, 4, z, Math.abs(x) == 4 ? StructBp.stair("spruce_stairs", x < 0 ? "+x" : "-x", false) : "spruce_slab[type=bottom]");
            }
        }
        for (int x = -3; x <= 3; x++) {
            b.set(x, 3, z0, "stripped_spruce_log[axis=x]");
        }
        b.set(0, 3, z0 - 1, "light_blue_wall_banner[facing=north]");
    }

    /**
     * Cobertizos contra la muralla: al oeste la armería (yunque, piedra de afilar, mesa de herrería, barriles y una armadura de
     * hielo de adorno) y al este, detrás de la celda, el almacén con heno y leña. Techos de una sola agua.
     */
    private static void sheds(StructBp b) {
        // armería (oeste)
        int xa = -WX + 1, xb = -KX - 2, za = 26, zb = 41;
        for (int z = za; z <= zb; z++) {
            for (int x = xa; x <= xb; x++) {
                b.set(x, -1, z, ((x + z) & 1) == 0 ? "spruce_planks" : "stripped_spruce_wood[axis=y]");
                b.m.remove(StructBp.key(x, 0, z));
            }
            // techo inclinado hacia el patio
            b.set(xa - 0, 4, z, "spruce_planks");
            b.set(xa + 1, 4, z, StructBp.stair("spruce_stairs", "-x", false));
            b.set(xa + 2, 3, z, StructBp.stair("spruce_stairs", "-x", false));
            b.set(xb, 3, z, StructBp.stair("spruce_stairs", "-x", false));
        }
        for (int z : new int[]{za, (za + zb) / 2, zb}) {
            for (int y = 0; y <= 2; y++) {
                b.set(xb, y, z, "spruce_fence");
            }
        }
        b.set(xa, 0, za + 2, "anvil[facing=east]");
        b.set(xa, 0, za + 4, "grindstone[face=floor,facing=east]");
        b.set(xa, 0, za + 6, "smithing_table");
        b.set(xa, 0, za + 8, "barrel[facing=up]");
        b.set(xa, 1, za + 8, "barrel[facing=up]");
        b.set(xa, 0, za + 10, "blast_furnace[facing=east,lit=false]");
        b.set(xa, 0, zb - 2, "barrel[facing=up]");
        b.set(xa + 1, 0, zb - 1, "spruce_fence");
        b.set(xa + 1, 1, zb - 1, "spruce_fence");
        b.set(xa, 2, za + 3, "lantern[hanging=true]");
        b.set(xa, 3, za + 3, "chain");
        b.set(xa, 2, zb - 4, "lantern[hanging=true]");
        b.set(xa, 3, zb - 4, "chain");
        // almacén (este, detrás de la celda)
        int xc = KX + 1, xd = WX - 1, zc = CZ1 + 2, zd = WZ1 - 3;
        for (int z = zc; z <= zd; z++) {
            for (int x = xc; x <= xd; x++) {
                b.set(x, -1, z, "spruce_planks");
                b.m.remove(StructBp.key(x, 0, z));
            }
            b.set(xd, 4, z, "spruce_planks");
            b.set(xd - 1, 4, z, StructBp.stair("spruce_stairs", "+x", false));
            b.set(xd - 2, 3, z, StructBp.stair("spruce_stairs", "+x", false));
            b.set(xc + 1, 3, z, StructBp.stair("spruce_stairs", "+x", false));
        }
        for (int z : new int[]{zc, zd}) {
            for (int y = 0; y <= 2; y++) {
                b.set(xc + 1, y, z, "spruce_fence");
            }
        }
        b.set(xd, 0, zc + 1, "hay_block[axis=y]");
        b.set(xd, 1, zc + 1, "hay_block[axis=y]");
        b.set(xd, 0, zc + 2, "hay_block[axis=x]");
        for (int z = zc + 4; z <= zc + 6; z++) {
            b.set(xd, 0, z, "spruce_log[axis=z]");
            b.set(xd, 1, z, "spruce_log[axis=z]");
        }
        b.set(xd, 0, zd, "barrel[facing=up]");
        b.set(xd - 1, 0, zd, "barrel[facing=up]");
        b.set(xd, 2, zc + 3, "lantern[hanging=true]");
        b.set(xd, 3, zc + 3, "chain");
    }

    // ================================================================== la celda

    /** Celda pegada a la sala por el este: barrotes y puerta de hierro sin manivela hacia la sala. */
    private static void cell(StructBp b) {
        int x0 = KX;
        for (int x = x0 + 1; x <= CX1; x++) {
            for (int z = CZ0; z <= CZ1; z++) {
                b.set(x, -1, z, StructKit.hash01(2596, x, z) < 0.4 ? CD : "cobblestone");
                b.set(x, -2, z, CD);
                for (int y = 0; y <= 4; y++) {
                    boolean edge = x == CX1 || z == CZ0 || z == CZ1;
                    b.set(x, y, z, y == 4 || edge ? wallMix(2597, x, y, z) : "air");
                }
                b.set(x, 5, z, StructBp.slab("deepslate_brick_slab", false));
            }
        }
        for (int z = CZ0 + 1; z <= CZ0 + 3; z++) {
            for (int y = 0; y <= 2; y++) {
                b.set(x0, y, z, "iron_bars");
            }
        }
        StructKit.ironDoor(b, x0, 0, CZ1 - 1, "-x", false);
        b.set(x0, 2, CZ1 - 1, DB);
        // dentro: jergón de paja, un barril helado, carámbanos y escarcha
        b.set(CX1 - 1, 0, CZ0 + 1, "hay_block[axis=z]");
        b.set(CX1 - 1, 0, CZ0 + 2, "hay_block[axis=z]");
        b.set(CX1 - 1, 1, CZ0 + 1, "white_carpet");
        b.set(CX1 - 1, 0, CZ1 - 1, "powder_snow_cauldron[level=2]");
        b.set(x0 + 1, 0, CZ0 + 1, "candle[candles=2,lit=true]");
        StructKit.hangLantern(b, NPC_X, 3, NPC_Z, 1, SOUL);
        b.set(CX1 - 1, 3, CZ1 - 1, "cobweb");
        b.set(x0 + 1, 3, CZ0 + 1, "cobweb");
    }

    // ================================================================== alrededor

    static double edge(int x, int z) {
        double dx = Math.max(0, Math.abs(x) - (WX + 3));
        double dz = Math.max(Math.max(0, (WZ0 - 4) - z), Math.max(0, z - (WZ1 + 4)));
        double out = Math.hypot(dx, dz);
        if (out > 0) {
            return out;
        }
        return -Math.min(Math.min((WX + 3) - Math.abs(x), z - (WZ0 - 4)), (WZ1 + 4) - z);
    }

    private static void surroundings(StructBp b) {
        int R = 34;
        for (int x = -WX - R; x <= WX + R; x++) {
            for (int z = WZ0 - R; z <= WZ1 + R; z++) {
                double e = edge(x, z);
                double ramp = 7 + 3 * StructKit.vnoise(2598, x * 0.1, z * 0.1);
                double w = e <= 2 ? 0 : Math.min(1, (e - 2) / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 1 && z >= -6 && z < WZ0 - 1;
                if (w >= 0.999 && !road) {
                    continue;
                }
                String top = road ? (StructKit.hash01(2599, x, z) < 0.3 ? CD : PD) : null;
                b.blend(x, z, road ? Math.min(w, 0.5) : w, top, null);
            }
        }
        // picos de hielo y farolas junto al camino (sobre el suelo real)
        int[][] spires = {{-6, -3, 5}, {-8, 0, 3}, {7, -1, 4}, {9, 3, 2}};
        for (int[] s : spires) {
            StructBp.Anchor a = b.anchor(s[0], s[1]);
            for (int y = 0; y < s[2]; y++) {
                a.set(0, y, 0, y == s[2] - 1 ? "blue_ice" : "packed_ice");
            }
            if (s[2] >= 4) {
                a.set(1, 0, 0, "packed_ice");
                a.set(0, 0, 1, "packed_ice");
            }
        }
        for (int sx : new int[]{-3, 3}) {
            StructBp.Anchor p = b.anchor(sx, -4);
            p.set(0, 0, 0, DBW);
            p.set(0, 1, 0, DBW);
            p.set(0, 2, 0, SOUL);
        }
    }

    private static void finishing(StructBp b) {
        StructKit.weather(b, 2600, DB, CDB, 4.0, 0.78);
    }

    // ================================================================== cofre y enemigos

    private static void chests(StructBp b) {
        b.setNbt(4, 1, 41, M + "vault_chest[facing=west]", "{Kind:\"ANTE\",Stage:3}");
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        return (at == null || at.equals("air") || at.startsWith("snow[") || at.endsWith("_carpet")) && (up == null || up.equals("air"))
                && below != null && !below.equals("air") && !below.contains("stairs");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind, String tag) {
        if (free(b, x, y, z)) {
            String at = b.get(x, y, z);
            if (at != null && !at.endsWith("_carpet")) {
                b.m.remove(StructBp.key(x, y, z));
            }
            b.mobs.add(new StructBp.Mob(x, y, z, kind, tag));
        } else {
            System.err.println("[Fortín] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    private static void mobs(StructBp b) {
        mob(b, -4, 0, 14, "lobo_escarcha", null);
        mob(b, 5, 0, 20, "lobo_escarcha", null);
        mob(b, 3, 0, 31, "lobo_escarcha", null);
        mob(b, -3, 0, 27, "centinela_escarcha", null);
        // el centinela del estrado lleva la llave de la celda
        mob(b, 0, 0, 35, "centinela_escarcha", "llave_celda");
        // el habitante del hielo, preso en la celda
        mob(b, NPC_X, 0, NPC_Z, "habitante", "bruno");
    }
}
