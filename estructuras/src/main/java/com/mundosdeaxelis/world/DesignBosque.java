package com.mundosdeaxelis.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 289: el Gran Templo del Bosque es ahora un <b>árbol gigante</b> (el Árbol del Mundo), no un templo de piedra con un árbol
 * encima. Un tronco hueco de unos 22 bloques de ancho (36 en la base, que se abre en raíces enormes) y 56 de alto, con la copa
 * encima. Se entra por un arco entre dos raíces, en el frente (z pequeña).
 * <ul>
 * <li><b>Salón de las Raíces</b> (y = 0): una sala enorme con forma de cúpula, columnas de raíces, un estanque, el Altar Maldito y
 * la bajada a la <b>Cripta de las raíces</b> (y = -10), bajo el tronco.</li>
 * <li>Una <b>escalera de caracol</b> pegada a la pared sube dando vueltas (una vuelta por piso) con barandilla del lado del hueco.</li>
 * <li><b>Biblioteca de corteza</b> (y = 14), <b>Jardín de setas</b> (y = 28) y <b>Armería de los guardianes</b> (y = 42): plataformas
 * redondas en el centro del tronco, con ventanas (agujeros de nudo) hacia fuera.</li>
 * <li><b>La Copa</b> (y = 56): el tronco se abre en una plataforma entre ramas gruesas, bajo las hojas; allí está la Piedra de
 * Portal: Naturaleza, el tesoro y el Guardián de Musgo que la guarda (etapa 293).</li>
 * </ul>
 * Coordenadas: el tronco está en (0, CZ); la entrada en z = CZ - radio de la base.
 */
final class DesignBosque {
    private DesignBosque() {
    }

    static final String PORTAL = "mundos_de_axelis:portal_stone_forest";
    /** Centro del tronco. */
    static final int CZ = 46;
    /** Alturas de los pisos (donde se pisa) y de la Copa; arriba del todo del tronco. */
    static final int L1 = 14, L2 = 28, L3 = 42, COPA = 56, TOP = 56;
    /** Suelo de la cripta. */
    static final int CRYPT = -10;
    /** Rectángulo que ocupa el árbol con sus raíces: x X0..X1, z Z0..Z1 (el terreno se aplana dentro). */
    static final int X0 = -38, X1 = 38, Z0 = -3, Z1 = 86;
    /** Dónde está la Piedra de Portal (en la Copa) y el tesoro. */
    static final int PX = 0, PY = COPA, PZ = CZ + 1;

    private static final String BARK = "oak_wood[axis=y]";
    private static final String BARK_D = "dark_oak_wood[axis=y]";
    private static final String BARK_S = "stripped_oak_wood[axis=y]";
    private static final String PLANK = "oak_planks";
    private static final String STAIR = "oak_stairs";
    private static final String SLAB = "oak_slab[type=bottom]";
    private static final String FENCE = "oak_fence";
    private static final String LEAF = "oak_leaves[persistent=true]";
    private static final String AZ = "azalea_leaves[persistent=true]";
    private static final String FAZ = "flowering_azalea_leaves[persistent=true]";
    private static final String[] CROWN = {LEAF, LEAF, LEAF, AZ, FAZ};

    static StructBp make() {
        StructBp b = new StructBp(4004);
        ground(b);
        roots(b);
        trunk(b);
        entrance(b);
        floors(b);
        spiral(b);
        hall(b);
        crypt(b);
        library(b);
        garden(b);
        armory(b);
        crown(b);
        knotholes(b);
        approach(b);
        surroundings(b);
        chests(b);
        mobs(b);
        return b;
    }

    // ================================================================== forma del árbol

    /** Radio de fuera del tronco a la altura y, en el ángulo a (con bultos de corteza). La base se abre en una campana. */
    static double rOut(double y, double a) {
        double yy = Math.max(0, y);
        double base = (11.0 + 7.0 * Math.exp(-yy / 5.0)) * (1.0 - 0.1 * Math.min(1, yy / TOP));
        double bump = (StructKit.vnoise(4021, Math.cos(a) * 3 + 10, y * 0.25) - 0.5) * 1.4 + Math.sin(a * 5 + y * 0.12) * 0.35;
        return base + bump;
    }

    /** Radio de dentro (hueco) a la altura y: la pared tiene unos 2,5 bloques. */
    static double rIn(double y) {
        double yy = Math.max(0, y);
        return (11.0 + 7.0 * Math.exp(-yy / 5.0)) * (1.0 - 0.1 * Math.min(1, yy / TOP)) - 2.6;
    }

    /** Ángulo de la columna (x, z) respecto al tronco: 0 = hacia el frente (z menor), creciendo hacia +x. */
    static double ang(int x, int z) {
        return Math.atan2(x, -(z - CZ));
    }

    private static String barkAt(int x, int y, int z) {
        double k = StructKit.hash01(4022 + y, x, z);
        double v = StructKit.vnoise3(4023, x * 0.2, y * 0.12, z * 0.2);
        return v < 0.28 ? BARK_D : k < 0.06 ? "mossy_cobblestone" : k < 0.12 ? "moss_block" : BARK;
    }

    /** El tronco: pared de corteza hasta arriba; dentro, hueco. */
    private static void trunk(StructBp b) {
        for (int y = -1; y <= TOP; y++) {
            int R = (int) Math.ceil(rOut(y, 0) + 1.5);
            for (int x = -R; x <= R; x++) {
                for (int z = CZ - R; z <= CZ + R; z++) {
                    double d = Math.hypot(x, z - CZ);
                    double ro = rOut(y, ang(x, z));
                    if (d > ro) {
                        continue;
                    }
                    if (y >= 0 && d < rIn(y)) {
                        b.set(x, y, z, "air");
                    } else {
                        b.set(x, y, z, barkAt(x, y, z));
                    }
                }
            }
        }
    }

    /** Ocho raíces enormes que salen de la base y se hunden en el suelo (la de delante no: ahí está la entrada). */
    private static void roots(StructBp b) {
        double[] as = {0.55, 1.25, 1.95, 2.65, 3.3, 3.95, 4.65, 5.35, 5.75};
        for (int i = 0; i < as.length; i++) {
            double a0 = as[i];
            double len = 24 + StructKit.hash01(4030, i, 0) * 8;
            for (double t = 0; t <= 1.0; t += 0.012) {
                double a = a0 + 0.18 * Math.sin(t * 3.0 + i);
                double d = 10 + t * len;
                double y = 6.5 * (1 - t) * (1 - t) - 1.5 * t + 0.6 * Math.sin(t * 9 + i);
                double rad = 3.6 * (1 - t) + 0.9;
                double cx = Math.sin(a) * d, cz = CZ - Math.cos(a) * d;
                int R = (int) Math.ceil(rad);
                for (int dx = -R; dx <= R; dx++) {
                    for (int dz = -R; dz <= R; dz++) {
                        for (int dy = -R; dy <= R; dy++) {
                            int x = (int) Math.round(cx) + dx, yy = (int) Math.round(y) + dy, z = (int) Math.round(cz) + dz;
                            if (yy < -2 || Math.pow(x - cx, 2) + Math.pow((yy - y) * 1.3, 2) + Math.pow(z - cz, 2) > rad * rad) {
                                continue;
                            }
                            if (Math.hypot(x, z - CZ) < rIn(yy) && yy >= 0) {
                                continue; // por dentro del tronco no
                            }
                            b.set(x, yy, z, StructKit.hash01(4031 + yy, x, z) < 0.15 ? "moss_block" : BARK);
                        }
                    }
                }
            }
        }
    }

    /** La entrada: un arco de 5 de ancho por la base del tronco, entre las dos raíces de delante. */
    private static void entrance(StructBp b) {
        int zIn = CZ - (int) Math.floor(rIn(0)) + 1;
        for (int z = CZ - 22; z <= zIn; z++) {
            for (int x = -3; x <= 3; x++) {
                int h = Math.abs(x) <= 1 ? 6 : Math.abs(x) == 2 ? 5 : 4;
                for (int y = 0; y <= h; y++) {
                    String s = b.get(x, y, z);
                    if (s != null && !s.equals("air") && Math.abs(x) <= 2) {
                        b.set(x, y, z, "air");
                    } else if (s != null && !s.equals("air") && Math.abs(x) == 3 && y <= 4) {
                        b.set(x, y, z, BARK_S);
                    }
                }
                if (Math.abs(x) <= 2) {
                    b.set(x, -1, z, StructKit.hash01(4032, x, z) < 0.4 ? "moss_block" : "rooted_dirt");
                }
            }
        }
        // marco del arco con troncos pelados y dos faroles
        int zf = CZ - (int) Math.floor(rOut(0, 0)) - 1;
        for (int s = -1; s <= 1; s += 2) {
            StructKit.hangLantern(b, 2 * s, 6, zf + 2, 1, "lantern");
        }
    }

    /** Plataformas de los pisos: discos de tablones en el centro, con barandilla menos donde llega la escalera. */
    private static void floors(StructBp b) {
        for (int lv : new int[]{L1, L2, L3}) {
            double rp = rIn(lv) - 2.4;
            double entry = spiralAngle(lv);
            for (int x = (int) -rp - 1; x <= rp + 1; x++) {
                for (int z = CZ - (int) rp - 1; z <= CZ + rp + 1; z++) {
                    double d = Math.hypot(x, z - CZ);
                    if (d > rp) {
                        continue;
                    }
                    b.set(x, lv - 1, z, (x * 3 + z) % 7 == 0 ? "stripped_oak_log[axis=x]" : PLANK);
                    b.set(x, lv - 2, z, "spruce_planks");
                    if (d > rp - 1.0) {
                        double da = Math.abs(angDiff(ang(x, z), entry));
                        b.set(x, lv, z, da < 0.38 ? "air" : FENCE);
                    }
                }
            }
            // vigas de apoyo hacia la pared (cuatro)
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + 0.4;
                for (double r = rp; r <= rIn(lv) + 0.5; r += 0.5) {
                    int x = (int) Math.round(Math.sin(a) * r), z = CZ - (int) Math.round(Math.cos(a) * r);
                    String s = b.get(x, lv - 2, z);
                    if (s == null || s.equals("air")) {
                        b.set(x, lv - 2, z, "stripped_dark_oak_log[axis=y]");
                    }
                }
            }
        }
    }

    static double angDiff(double a, double b) {
        double d = (a - b) % (2 * Math.PI);
        if (d > Math.PI) {
            d -= 2 * Math.PI;
        }
        if (d < -Math.PI) {
            d += 2 * Math.PI;
        }
        return d;
    }

    /** Ángulo de la escalera de caracol a la altura y (una vuelta por piso, empieza a la derecha de la entrada). */
    static double spiralAngle(double y) {
        return 0.9 + y / 14.0 * 2 * Math.PI;
    }

    /**
     * La escalera de caracol: 2 de ancho, pegada a la pared por dentro, sube 14 por vuelta desde el Salón de las Raíces hasta la
     * Copa. Escalones donde sube un bloque, tablones donde no; aire encima; barandilla del lado del hueco lejos de los pisos.
     */
    private static void spiral(StructBp b) {
        int lastF = 0;
        for (double y = 0; y <= COPA; y += 0.05) {
            double a = spiralAngle(y);
            int f = (int) Math.floor(y);
            double ri = rIn(f + 3); // lo más estrecho por encima de la cabeza (abajo, la pared se va cerrando al subir)
            String up = upDir(a);
            boolean step = f > lastF;
            for (double k = 0.3; k <= 2.3; k += 0.5) {
                double r = ri - k;
                int x = (int) Math.round(Math.sin(a) * r), z = CZ - (int) Math.round(Math.cos(a) * r);
                if (f == 0) {
                    continue; // en el salón, el suelo ya está
                }
                String cur = b.get(x, f - 1, z);
                if (cur == null || cur.equals("air") || cur.startsWith(STAIR) || cur.equals(PLANK) || cur.startsWith("oak_fence")
                        || cur.startsWith("stripped_") || cur.equals("spruce_planks")) {
                    b.set(x, f - 1, z, step ? STAIR + "[facing=" + up + ",half=bottom]" : PLANK);
                }
                for (int h = f; h <= f + 3; h++) {
                    String s = b.get(x, h, z);
                    if (s != null && (s.startsWith("oak_fence") || s.equals(PLANK) || s.startsWith("stripped_") || s.equals("spruce_planks"))) {
                        b.set(x, h, z, "air");
                    }
                }
            }
            // barandilla del lado del hueco, lejos de los pisos (allí se baja a la plataforma)
            boolean nearFloor = false;
            for (int lv : new int[]{L1, L2, L3, COPA}) {
                if (y > lv - 3 && y < lv + 1.5) {
                    nearFloor = true;
                }
            }
            if (!nearFloor && f > 0) {
                double r = ri - 2.8;
                int x = (int) Math.round(Math.sin(a) * r), z = CZ - (int) Math.round(Math.cos(a) * r);
                String below = b.get(x, f - 1, z);
                if ((b.get(x, f, z) == null || "air".equals(b.get(x, f, z))) && below != null && !below.equals("air")) {
                    b.set(x, f, z, FENCE);
                } else if (b.get(x, f, z) == null || "air".equals(b.get(x, f, z))) {
                    b.set(x, f - 1, z, SLAB);
                    b.set(x, f, z, FENCE);
                }
            }
            lastF = Math.max(lastF, f);
        }
    }

    /** Hacia dónde sube la escalera en el ángulo a (la dirección de avance, redondeada a uno de los cuatro lados). */
    private static String upDir(double a) {
        double tx = Math.cos(a), tz = Math.sin(a); // derivada de (sin a, -cos a)
        if (Math.abs(tx) > Math.abs(tz)) {
            return tx > 0 ? "east" : "west";
        }
        return tz > 0 ? "south" : "north";
    }

    // ================================================================== las salas

    /** Salón de las Raíces: suelo de musgo y tierra, columnas de raíces, un estanque, luz de hongos y la bajada a la cripta. */
    private static void hall(StructBp b) {
        double ri = rIn(0);
        for (int x = (int) -ri; x <= ri; x++) {
            for (int z = CZ - (int) ri; z <= CZ + ri; z++) {
                if (Math.hypot(x, z - CZ) >= ri) {
                    continue;
                }
                double n = StructKit.fbm(4040, x, z, 3.0);
                b.set(x, -1, z, n < 0.35 ? "moss_block" : n < 0.6 ? "rooted_dirt" : n < 0.8 ? "coarse_dirt" : "mossy_cobblestone");
                b.set(x, -2, z, "dirt");
                if (n < 0.3 && StructKit.hash01(4041, x, z) < 0.3 && Math.abs(x) > 3) {
                    b.set(x, 0, z, "moss_carpet");
                }
            }
        }
        // nudos de raíces que salen del suelo (lejos de la escalera, que va pegada a la pared)
        for (double a : new double[]{2.6, 3.6, 4.7, 5.7}) {
            int cx = (int) Math.round(Math.sin(a) * 7.0), cz = CZ - (int) Math.round(Math.cos(a) * 7.0);
            for (int y = 0; y <= 2; y++) {
                b.set(cx, y, cz, y == 2 ? "mangrove_roots" : "stripped_oak_wood[axis=y]");
            }
            b.set(cx + 1, 0, cz, "mangrove_roots");
            b.set(cx, 0, cz + 1, "mangrove_roots");
            b.set(cx, 3, cz, "moss_carpet");
        }
        // estanque al fondo a la izquierda con nenúfares
        for (int x = -7; x <= -4; x++) {
            for (int z = CZ + 4; z <= CZ + 7; z++) {
                b.set(x, -1, z, "water");
                b.set(x, -2, z, "clay");
                if ((x + z) % 3 == 0) {
                    b.set(x, 0, z, "lily_pad");
                }
            }
        }
        // luz: hongos de luz en la pared y faroles colgados del techo de la cúpula
        for (int i = 0; i < 10; i++) {
            double a = i * Math.PI / 5 + 0.2;
            int y = 3 + (i % 3) * 3;
            double r = rIn(y) + 0.6;
            b.set((int) Math.round(Math.sin(a) * r), y, CZ - (int) Math.round(Math.cos(a) * r), "shroomlight");
        }
        for (int[] c : new int[][]{{-5, CZ - 3}, {5, CZ - 3}, {0, CZ + 4}}) {
            StructKit.hangLantern(b, c[0], L1 - 3, c[1], 2, "lantern");
        }
        // barriles de provisiones junto a la pared
        for (int[] c : new int[][]{{-9, CZ - 3}, {-9, CZ - 1}}) {
            b.setNbt(c[0], 0, c[1], "barrel[facing=up]", "{LootTable:\"mundos_de_axelis:chests/barril_bosque\"}");
        }
        // la bajada a la cripta: escalera recta hacia el fondo (z+), con barandilla
        for (int i = 0; i <= 9; i++) {
            int z = CZ + 2 + i;
            for (int x = 2; x <= 4; x++) {
                b.set(x, -1 - i, z, STAIR + "[facing=north,half=bottom]");
                for (int h = 0; h <= 3; h++) {
                    b.set(x, -i + h, z, "air");
                }
                if (i == 0) {
                    b.set(x, -1, z, STAIR + "[facing=north,half=bottom]");
                }
            }
            b.set(1, -i, z, i == 0 ? FENCE : BARK);
            b.set(5, -i, z, i == 0 ? FENCE : BARK);
        }
    }

    /** Cripta de las raíces: bajo el tronco, raíces que atraviesan el techo, velas y el cofre con cerradura. */
    private static void crypt(StructBp b) {
        int f = CRYPT;
        int z0 = CZ + 12, z1 = CZ + 22;
        b.box(-6, f - 1, z0, 8, f + 6, z1, "rooted_dirt");
        for (int x = -5; x <= 7; x++) {
            for (int z = z0 + 1; z < z1; z++) {
                b.set(x, f - 1, z, (x + z & 1) == 0 ? "mossy_stone_bricks" : "stone_bricks");
                for (int y = f; y <= f + 4; y++) {
                    b.set(x, y, z, "air");
                }
                if (StructKit.hash01(4050, x, z) < 0.25) {
                    b.set(x, f + 4, z, "hanging_roots");
                }
            }
        }
        // la escalera del salón llega por aquí (x 2..4)
        for (int x = 2; x <= 4; x++) {
            for (int y = f; y <= f + 3; y++) {
                b.set(x, y, z0, "air");
            }
            b.set(x, f - 1, z0, "stone_bricks");
        }
        for (int[] c : new int[][]{{-5, z0 + 1}, {7, z0 + 1}, {-5, z1 - 1}, {7, z1 - 1}}) {
            b.set(c[0], f, c[1], "mossy_stone_brick_wall");
            b.set(c[0], f + 1, c[1], "candle[candles=4,lit=true]");
        }
        for (int x : new int[]{-2, 4}) {
            b.set(x, f + 4, z1 - 3, "shroomlight");
        }
        // sarcófago de piedra con musgo en el centro
        for (int x = 0; x <= 2; x++) {
            b.set(x, f, z1 - 4, "mossy_stone_bricks");
        }
        b.set(1, f + 1, z1 - 4, "moss_carpet");
    }

    /** Biblioteca de corteza (y = 14): estanterías en curva sobre la plataforma, atriles y una mesa. */
    private static void library(StructBp b) {
        int f = L1;
        double rp = rIn(f) - 2.4;
        double entry = spiralAngle(f);
        for (int x = (int) -rp; x <= rp; x++) {
            for (int z = CZ - (int) rp; z <= CZ + rp; z++) {
                double d = Math.hypot(x, z - CZ);
                double da = Math.abs(angDiff(ang(x, z), entry));
                if (d > rp - 2.2 && d <= rp - 1.0 && da > 0.7 && da < 2.9) {
                    b.set(x, f, z, "bookshelf");
                    if (StructKit.hash01(4060, x, z) < 0.6) {
                        b.set(x, f + 1, z, "bookshelf");
                    }
                }
            }
        }
        b.set(0, f, CZ, "lectern[facing=south]");
        b.set(-1, f, CZ + 1, "oak_slab[type=top]");
        b.set(1, f, CZ + 1, "oak_slab[type=top]");
        b.set(1, f + 1, CZ + 1, "candle[candles=2,lit=true]");
        StructKit.hangLantern(b, 0, L2 - 3, CZ, 3, "lantern");
    }

    /** Jardín de setas (y = 28): suelo de micelio, setas grandes pequeñas, luz de hongos. */
    private static void garden(StructBp b) {
        int f = L2;
        double rp = rIn(f) - 2.4;
        for (int x = (int) -rp; x <= rp; x++) {
            for (int z = CZ - (int) rp; z <= CZ + rp; z++) {
                if (Math.hypot(x, z - CZ) <= rp - 1.0) {
                    b.set(x, f - 1, z, StructKit.hash01(4070, x, z) < 0.6 ? "mycelium" : "moss_block");
                }
            }
        }
        for (int[] c : new int[][]{{-3, -2, 0}, {3, 2, 1}, {-2, 3, 1}}) {
            int x = c[0], z = CZ + c[1];
            String cap = c[2] == 0 ? "red_mushroom_block" : "brown_mushroom_block";
            b.set(x, f, z, "mushroom_stem");
            b.set(x, f + 1, z, "mushroom_stem");
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    b.set(x + dx, f + 2, z + dz, cap);
                }
            }
        }
        StructKit.hangLantern(b, 0, L3 - 3, CZ, 2, "shroomlight");
    }

    /** Armería de los guardianes (y = 42): yunque, afiladora, barriles y estandartes verdes. */
    private static void armory(StructBp b) {
        int f = L3;
        b.set(-2, f, CZ + 2, "anvil[facing=east]");
        b.set(2, f, CZ + 2, "grindstone[face=floor,facing=north]");
        b.set(-3, f, CZ - 1, "barrel[facing=up]");
        b.set(3, f, CZ - 1, "smithing_table");
        b.set(0, f, CZ + 3, "green_banner[rotation=8]");
        StructKit.hangLantern(b, 0, COPA - 3, CZ, 2, "lantern");
    }

    /**
     * La Copa: el tronco se abre en una plataforma (y = 55) con barandilla de ramas; ramas gruesas salen hacia fuera y la cubren
     * hojas (con huecos para la luz). En el centro, la Piedra de Portal sobre un altar con flores.
     */
    private static void crown(StructBp b) {
        int f = COPA;
        double rp = rOut(f, 0) + 1.5;
        for (int x = (int) -rp - 1; x <= rp + 1; x++) {
            for (int z = CZ - (int) rp - 1; z <= CZ + rp + 1; z++) {
                double d = Math.hypot(x, z - CZ);
                if (d > rp) {
                    continue;
                }
                for (int y = f; y <= f + 5; y++) {
                    b.set(x, y, z, "air");
                }
                // por donde sube la escalera (su último cuarto de vuelta) queda el hueco
                double da = angDiff(ang(x, z), spiralAngle(COPA));
                double rl = rIn(COPA);
                if (d >= rl - 2.9 && d <= rl + 0.6 && da > -Math.PI / 2 - 0.35 && da < 0.1) {
                    continue;
                }
                b.set(x, f - 1, z, d < 2.5 ? "moss_block" : (x + z & 1) == 0 ? PLANK : "stripped_oak_wood[axis=y]");
                if (d > rp - 1.0) {
                    b.set(x, f, z, "oak_fence");
                }
            }
        }
        // ramas gruesas: de la copa hacia fuera y arriba, con su bola de hojas al final
        List<double[]> ends = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            double a = i * 2 * Math.PI / 7 + 0.3;
            double len = 16 + StructKit.hash01(4080, i, 1) * 7;
            double rise = 6 + StructKit.hash01(4081, i, 2) * 8;
            for (double t = 0; t <= 1.0; t += 0.02) {
                double d = rp - 1 + t * len;
                double y = f - 2 + t * rise + Math.sin(t * Math.PI) * 2;
                double rad = 2.2 * (1 - t) + 0.8;
                double cx = Math.sin(a) * d, cz = CZ - Math.cos(a) * d;
                int R = (int) Math.ceil(rad);
                for (int dx = -R; dx <= R; dx++) {
                    for (int dy = -R; dy <= R; dy++) {
                        for (int dz = -R; dz <= R; dz++) {
                            int x = (int) Math.round(cx) + dx, yy = (int) Math.round(y) + dy, z = (int) Math.round(cz) + dz;
                            if (Math.pow(x - cx, 2) + Math.pow(yy - y, 2) + Math.pow(z - cz, 2) > rad * rad) {
                                continue;
                            }
                            if (yy >= f && yy <= f + 5 && Math.hypot(x, z - CZ) <= rp) {
                                continue; // la plataforma queda libre
                            }
                            b.set(x, yy, z, BARK);
                        }
                    }
                }
            }
            ends.add(new double[]{Math.sin(a) * (rp - 1 + len), f - 2 + rise, CZ - Math.cos(a) * (rp - 1 + len)});
        }
        // hojas: una bola grande arriba (con agujeros de luz) y una por rama
        blob(b, 0, f + 15, CZ, 17, 8, 4090, true);
        for (int i = 0; i < ends.size(); i++) {
            double[] e = ends.get(i);
            blob(b, e[0], e[1] + 1, e[2], 7 + StructKit.hash01(4082, i, 3) * 2, 4.5, 4091 + i, false);
        }
        // el altar: Piedra de Portal sobre un pedestal de musgo, flores alrededor y luz
        b.set(PX, f - 1, PZ, "mossy_stone_bricks");
        b.set(PX, f, PZ, PORTAL);
        for (int[] c : new int[][]{{-1, 0}, {1, 0}, {0, 1}, {0, -1}}) {
            b.set(PX + c[0], f, PZ + c[1], c[1] == 0 ? "azalea" : "flowering_azalea");
        }
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 4}, {3, 4}}) {
            b.set(c[0], f, CZ + c[1], "mossy_stone_brick_wall");
            b.set(c[0], f + 1, CZ + c[1], "lantern");
        }
    }

    /** Una bola de hojas (aplastada): {@code holes} deja huecos para que entre la luz. */
    private static void blob(StructBp b, double cx, double cy, double cz, double r, double ry, long seed, boolean holes) {
        int R = (int) Math.ceil(r);
        int RY = (int) Math.ceil(ry);
        for (int x = (int) cx - R; x <= cx + R; x++) {
            for (int z = (int) cz - R; z <= cz + R; z++) {
                for (int y = (int) cy - RY; y <= cy + RY; y++) {
                    double n = (StructKit.vnoise3(seed, x * 0.25, y * 0.25, z * 0.25) - 0.5) * 0.5;
                    double q = Math.pow((x - cx) / r, 2) + Math.pow((y - cy) / ry, 2) + Math.pow((z - cz) / r, 2);
                    if (q > 1 + n) {
                        continue;
                    }
                    if (holes && q < 0.45 && y < cy) {
                        continue; // por debajo, hueca: se ve el cielo entre las hojas desde la Copa
                    }
                    if (holes && StructKit.hash01(seed + 1, x * 31 + y, z) < 0.04) {
                        continue;
                    }
                    String s = b.get(x, y, z);
                    if (s == null || s.equals("air")) {
                        b.set(x, y, z, CROWN[(int) (StructKit.hash01(seed + 2, x + y * 7, z) * CROWN.length)]);
                    }
                }
            }
        }
    }

    /** Agujeros de nudo: en cada piso, dos ventanas redondeadas hacia fuera con marco de corteza pelada. */
    private static void knotholes(StructBp b) {
        int[] levels = {L1, L2, L3};
        for (int i = 0; i < levels.length; i++) {
            for (int s = 0; s < 2; s++) {
                double a = spiralAngle(levels[i]) + Math.PI * (0.55 + s * 0.8) + i * 0.4;
                int y0 = levels[i] + 1;
                double ro = rOut(y0, a) + 1;
                for (double r = rIn(y0) - 0.5; r <= ro; r += 0.5) {
                    for (int dy = 0; dy <= 1; dy++) {
                        for (int k = -1; k <= 0; k++) {
                            int x = (int) Math.round(Math.sin(a) * r + Math.cos(a) * k), z = CZ - (int) Math.round(Math.cos(a) * r - Math.sin(a) * k);
                            String cur = b.get(x, y0 + dy, z);
                            if (cur != null && (cur.startsWith("oak_wood") || cur.startsWith("dark_oak_wood") || cur.startsWith("moss") || cur.startsWith("mossy"))) {
                                b.set(x, y0 + dy, z, "air");
                            }
                        }
                    }
                }
            }
        }
    }

    // ================================================================== fuera

    private static void ground(StructBp b) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                double n = StructKit.fbm(4010, x, z, 6.0);
                b.setIfAbsent(x, -1, z, n < 0.62 ? "grass_block" : n < 0.8 ? "moss_block" : "podzol");
                b.setIfAbsent(x, -2, z, "dirt");
            }
        }
        b.foundation(X0, Z0, X1, Z1, "dirt");
    }

    /** El camino de la entrada: tierra y musgo entre faroles, con flores a los lados. */
    private static void approach(StructBp b) {
        int zEnd = CZ - (int) Math.floor(rOut(0, 0)) - 1;
        for (int z = Z0; z <= zEnd; z++) {
            for (int x = -2; x <= 2; x++) {
                String s = b.get(x, -1, z);
                if (s == null || s.equals("grass_block") || s.equals("moss_block") || s.equals("podzol") || s.equals("dirt")) {
                    b.set(x, -1, z, StructKit.hash01(4100, x, z) < 0.35 ? "moss_block" : "coarse_dirt");
                }
            }
            if (z % 6 == 3 && z < zEnd - 2) {
                for (int s = -1; s <= 1; s += 2) {
                    String at = b.get(4 * s, 0, z);
                    if (at == null || at.equals("air")) {
                        b.set(4 * s, 0, z, "oak_fence");
                        b.set(4 * s, 1, z, "lantern");
                    }
                }
            }
        }
        // flores y helechos por el claro
        for (int x = X0 + 2; x <= X1 - 2; x++) {
            for (int z = Z0; z <= Z1 - 2; z++) {
                String below = b.get(x, -1, z), at = b.get(x, 0, z);
                if (at != null || below == null || !below.equals("grass_block") || Math.abs(x) <= 3 && z <= CZ) {
                    continue;
                }
                double k = StructKit.hash01(4101, x, z);
                if (k < 0.06) {
                    b.set(x, 0, z, k < 0.02 ? "fern" : k < 0.035 ? "poppy" : k < 0.05 ? "oxeye_daisy" : "cornflower");
                }
            }
        }
    }

    /** Fuera del rectángulo, el terreno real baja o sube en rampa suave hasta el nivel del árbol; un camino lleva a la entrada. */
    static double edge(int x, int z) {
        double dx = Math.max(Math.max(0, X0 - x), Math.max(0, x - X1));
        double dz = Math.max(Math.max(0, Z0 - z), Math.max(0, z - Z1));
        double out = Math.hypot(dx, dz);
        if (out > 0) {
            return out;
        }
        return -Math.min(Math.min(x - X0, X1 - x), Math.min(z - Z0, Z1 - z));
    }

    private static void surroundings(StructBp b) {
        int rr = 30;
        for (int x = X0 - rr; x <= X1 + rr; x++) {
            for (int z = Z0 - rr; z <= Z1 + rr; z++) {
                double e = edge(x, z);
                if (e <= 0) {
                    continue;
                }
                double ramp = 9 + 4 * StructKit.vnoise(4011, x * 0.08, z * 0.08);
                double w = Math.min(1, e / ramp);
                w = w * w * (3 - 2 * w);
                boolean road = Math.abs(x) <= 2 && z < Z0;
                if (w >= 0.999 && !road) {
                    continue;
                }
                String top = road ? (StructKit.hash01(4012, x, z) < 0.35 ? "mossy_cobblestone" : "gravel") : null;
                b.blend(x, z, road ? Math.min(w, 0.5) : w, top, null);
            }
        }
    }

    // ================================================================== cofres y enemigos

    private static void vault(StructBp b, int x, int y, int z, String facing, String kind, int stage, int lock) {
        StructKit.vault(b, x, y, z, facing, kind, stage, lock);
    }

    /** Dónde están los cofres de cada sala (los usa también la prueba del CI). */
    static final int[] C_ANTE = {-8, 0, CZ - 6}, C_LIB = {-4, L1, CZ - 1}, C_GARDEN = {4, L2, CZ + 1}, C_ARMORY = {-3, L3, CZ + 1},
            C_CRYPT = {1, CRYPT, CZ + 20}, C_TREASURE = {-3, COPA, CZ + 1};

    /** Al final del recorrido, en la Copa: la Piedra de Portal y al lado el gran tesoro; y un cofre en cada sala. */
    private static void chests(StructBp b) {
        vault(b, C_TREASURE[0], C_TREASURE[1], C_TREASURE[2], "east", "TREASURE", 1, 0);
        vault(b, C_ANTE[0], C_ANTE[1], C_ANTE[2], "east", "ANTE", 1, 0);
        vault(b, C_LIB[0], C_LIB[1], C_LIB[2], "east", "LOCKED", 1, 2);
        vault(b, C_GARDEN[0], C_GARDEN[1], C_GARDEN[2], "west", "TRIAL", 1, 0);
        vault(b, C_ARMORY[0], C_ARMORY[1], C_ARMORY[2], "east", "TRIAL", 1, 0);
        vault(b, C_CRYPT[0], C_CRYPT[1], C_CRYPT[2], "north", "LOCKED", 1, 3);
    }

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        String at = b.get(x, y, z);
        String up = b.get(x, y + 1, z);
        boolean plant = at != null && (at.equals("grass") || at.equals("fern") || at.equals("moss_carpet") || at.equals("poppy"));
        return (at == null || at.equals("air") || plant) && (up == null || up.equals("air"))
                && below != null && !below.equals("air") && !below.contains("stairs") && !below.startsWith("water");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind) {
        if (free(b, x, y, z)) {
            b.m.remove(StructBp.key(x, y, z));
            b.mobs.add(new StructBp.Mob(x, y, z, kind));
        } else {
            System.err.println("[Templo] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    /** Enemigos del bosque: en el claro, en el salón, en cada piso y el Raizal que guarda la Copa. */
    private static void mobs(StructBp b) {
        mob(b, -6, 0, CZ - 24, "brote");
        mob(b, 6, 0, CZ - 22, "brote");
        mob(b, -4, 0, CZ - 2, "brote");
        mob(b, 7, 0, CZ + 1, "brote");
        mob(b, 0, 0, CZ + 8, "espinoso");
        mob(b, 2, L1, CZ - 2, "espinoso");
        mob(b, -2, L1, CZ + 2, "brote");
        mob(b, 2, L2, CZ - 2, "espinoso");
        mob(b, -1, L2, CZ - 1, "espinoso");
        mob(b, 2, L3, CZ - 2, "espinoso");
        mob(b, -1, L3, CZ - 2, "brote");
        // etapa 293: la Copa la guarda el Guardián de Musgo (mini-jefe); el Raizal vigila la armería
        mob(b, 0, COPA, CZ + 6, "guardian_musgo");
        mob(b, 1, L3, CZ + 2, "raizal");
    }
}
