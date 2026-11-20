package com.mundosdeaxelis.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * El Volcán (etapa 4, Corazón de Magma): un volcán enorme (unos 70 bloques de alto y 108 de ancho) con un camino en espiral que
 * sube hasta el cráter, ríos de lava falsa por las laderas, humo y erupciones de adorno. En el cráter hay un lago de lava falsa:
 * quien se tira cae despacio por un pozo hasta la dungeon que hay dentro:
 * <ol>
 * <li>Cámara del cráter (donde se aterriza).</li>
 * <li>Puente de basalto que se derrumba al pisarlo, sobre un foso de magma con salamandras (y una escalera para reintentar).</li>
 * <li>Sala de las erupciones: filas de respiraderos que escupen fuego por turnos, diablillos, y un altar con cultistas.</li>
 * <li>Santuario del corazón: la Piedra de Portal: Volcán sobre un altar, bajo un corazón de magma encadenado.</li>
 * </ol>
 * Del santuario sale un túnel hacia el frente del volcán (de un solo sentido: termina en un escalón de 2 que no se puede subir).
 * Entrada por z = 0; el centro del volcán está en z = {@link #CZ}.
 */
final class DesignVolcan {
    private DesignVolcan() {
    }

    static final int CZ = 60;
    /** Radio de la base, alto del borde del cráter y radios del cráter (borde y lago). */
    static final int RB = 54, H = 69, RR = 16, RL = 7;
    /** Altura de la capa de lava falsa del cráter. */
    static final int LAVA_Y = 61;
    /** Suelo de cada nivel de la dungeon. */
    static final int F_HALL = 32, F_BRIDGE = 24, F_PIT = 21, F_ERUPT = 8, F_SANCT = -8;

    private static final String M = "mundos_de_axelis:";
    private static final String LAVA = M + "lava_falsa";
    private static final String CRACK = M + "basalto_agrietado";
    private static final String VENT = M + "respiradero_volcan";
    private static final String CORE = M + "nucleo_volcan";
    private static final String[] ROCK = {"basalt[axis=y]", "basalt[axis=y]", "blackstone", "smooth_basalt", "basalt[axis=y]", "tuff"};
    private static final String[] WALL = {"basalt[axis=y]", "blackstone", "smooth_basalt", "polished_basalt[axis=y]"};
    private static final String PB = "polished_blackstone";
    private static final String PBB = "polished_blackstone_bricks";
    private static final String PBBS = "polished_blackstone_brick_stairs";
    private static final String GILD = "gilded_blackstone";

    /** Columnas por las que pasa el camino en espiral (para que los ríos y las rocas no lo tapen). */
    private static final Set<Long> TRAIL = new HashSet<>();
    /** Columnas del camino de losas que llega desde fuera (se apoya en el terreno real: va en la mezcla, no en el plano). */
    private static final Set<Long> ROAD = new HashSet<>();
    /** Coladas de lava ya enfriada al pie de cada río (columna -> bloque de la superficie). */
    private static final Map<Long, String> FAN = new HashMap<>();
    private static final String FUMAROLA = M + "fumarola";
    private static final String ASH_CEIL = M + "ceniza_volcan";
    private static final String VEIN = M + "veta_magma";
    private static final String BRAZ = M + "brasero_volcan";
    private static final String GATE = M + "reja_volcan";
    private static final String VC = M + "vault_chest_volcano";
    private static final String VCL = M + "vault_chest_locked_volcano";
    /** Suelo de la cámara del guardián (la Gran Salamandra). */
    static final int F_GUARD = -8;
    /** Centro de la cámara del guardián. */
    static final int GX = 22, GZ = 100;
    /** Rocas de las paredes que se pueden "pintar" con manchas y vetas (no los suelos ni los adornos). */
    private static final Set<String> ROCKY = new HashSet<>(List.of("basalt[axis=y]", "blackstone", "smooth_basalt", "tuff", "deepslate",
            "cobbled_deepslate", "polished_deepslate", "netherrack", "polished_basalt[axis=y]", "gravel", "coarse_dirt"));

    /** Sincronizado: usa conjuntos estáticos y el registro de volcanes lo calcula también en otro hilo. */
    static synchronized StructBp make() {
        StructBp b = new StructBp(4404);
        TRAIL.clear();
        ROAD.clear();
        FAN.clear();
        mountain(b);
        crater(b);
        rivers(b);
        trail(b);
        // la dungeon de dentro
        landingHall(b);
        bridgeChamber(b);
        eruptionHall(b);
        cultistAltar(b);
        treasureRoom(b);
        guardianChamber(b);
        sanctuary(b);
        passages(b);
        exitTunnel(b);
        encaseUnderground(b);
        interiorDetail(b);
        // por fuera: fumarolas, coladas enfriadas, el pie que se funde con el terreno, árboles quemados y rocas
        slopeFumaroles(b);
        coneCraters(b);
        fans(b);
        surroundings(b);
        charredTrees(b);
        boulders(b);
        plainFumaroles(b);
        chests(b);
        mobs(b);
        return b;
    }

    // ================================================================== forma del volcán

    /** Altura (sin ruido) de la ladera a distancia d del centro: cóncava, más empinada arriba. */
    static double profile(double d) {
        if (d >= RB) {
            return 0;
        }
        if (d <= RR) {
            return H;
        }
        return H * Math.pow((RB - d) / (RB - RR), 1.25);
    }

    /** Distancia al centro a la que la ladera (sin ruido) tiene altura h. */
    static double radiusAt(double h) {
        return RB - (RB - RR) * Math.pow(Math.max(0, Math.min(1, h / H)), 1 / 1.25);
    }

    /**
     * Conos pequeños al pie del volcán (volcanes "hijos"): ángulo desde el frente, distancia al centro, radio y alto. Están lejos del
     * camino, de la entrada, de la salida y de los ríos.
     */
    static final double[][] CONES = {{2.35, RB - 2, 11.0, 17.0}, {4.45, RB - 1, 10.0, 14.0}, {5.75, RB + 3, 9.0, 12.0}};

    static int coneX(int i) {
        return (int) Math.round(Math.sin(CONES[i][0]) * CONES[i][1]);
    }

    static int coneZ(int i) {
        return CZ - (int) Math.round(Math.cos(CONES[i][0]) * CONES[i][1]);
    }

    /** Altura del cono i en (x, z) (0 fuera de él), con un cráter pequeño arriba. */
    static double coneH(int i, int x, int z) {
        double dc = Math.hypot(x - coneX(i), z - coneZ(i));
        double r = CONES[i][2];
        if (dc >= r) {
            return 0;
        }
        double t = 1 - dc / r;
        double v = CONES[i][3] * Math.pow(t, 0.95) + (StructKit.vnoise(300 + i, x * 0.5, z * 0.5) - 0.5) * 1.6 * t;
        if (dc < 2.4) {
            v -= 3.2 * (1 - dc / 2.4) + 0.6;
        }
        return v;
    }

    /** Altura real de la columna (x, z): ladera con ruido y barrancos, borde del cráter dentado, cuenco del cráter y conos. */
    static int height(int x, int z) {
        double dx = x, dz = z - CZ;
        double d = Math.sqrt(dx * dx + dz * dz);
        double a = Math.atan2(dx, -dz);
        if (d <= RL + 0.3) {
            return LAVA_Y; // bajo el lago: roca (luego se abre el pozo)
        }
        if (d < RR) {
            // cuenco: de la orilla del lago al borde, pendiente suave (se baja andando)
            double t = (d - RL) / (RR - RL);
            return LAVA_Y + (int) Math.round(t * (H - LAVA_Y) + StructKit.vnoise(77, x * 0.4, z * 0.4) * 0.8);
        }
        double main = 0;
        if (d < RB + 1) {
            double base = profile(d);
            double t = (d - RR) / (RB - RR);
            double amp = Math.sin(Math.PI * Math.min(1, t * 1.3)) * 3.0 + 0.6;
            double n = (StructKit.fbm(4404, x, z, 9.0) - 0.5) * 2.0 * amp;
            double gully = Math.sin(a * 9 + StructKit.fbm(91, x, z, 14.0) * 4) * 1.2 * Math.min(1, t * 2.5);
            // borde del cráter dentado (pocos picos y bajos)
            double rim = d < RR + 3 ? Math.max(0, (StructKit.vnoise(5, x * 0.5, z * 0.5) - 0.62) * 9) : 0;
            main = Math.max(0, base + n + gully + rim);
        }
        double cone = 0;
        if (d > RB - 20) {
            for (int i = 0; i < CONES.length; i++) {
                cone = Math.max(cone, coneH(i, x, z));
            }
        }
        return (int) Math.max(0, Math.round(Math.max(main, cone)));
    }

    /** Distancia (aprox.) desde la columna al pie del volcán o de sus conos: negativa dentro. */
    static double edge(int x, int z) {
        double e = Math.hypot(x, z - CZ) - RB;
        for (int i = 0; i < CONES.length; i++) {
            e = Math.min(e, Math.hypot(x - coneX(i), z - coneZ(i)) - CONES[i][2]);
        }
        return e;
    }

    /** Ruido sin costuras alrededor del volcán (según el ángulo): 0..1. */
    private static double around(long seed, double a, double scale) {
        return StructKit.vnoise(seed, 40 + Math.cos(a) * scale, 40 + Math.sin(a) * scale);
    }

    /**
     * El bloque de la ladera: manchas grandes (no sal y pimienta), coladas viejas que bajan en franjas oscuras, ceniza clara en las
     * faldas, roca negra y rojiza arriba, chispas de magma que abundan cerca del cráter; donde es muy empinado, roca desnuda.
     */
    static String surf(int x, int z, int y, double slope) {
        double dz = z - CZ;
        double d = Math.hypot(x, dz);
        double a = Math.atan2(x, -dz);
        double hot = Math.max(0, Math.min(1, 1 - d / RB));
        double fine = StructKit.hash01(41, x, z);
        if (fine < 0.004 + 0.05 * hot * hot * hot) {
            return "magma_block";
        }
        double flow = StructKit.vnoise3(101, Math.cos(a) * 17, d * 0.07, Math.sin(a) * 17);
        double patch = StructKit.fbm(102, x, z, 7.0) + (StructKit.hash01(104, x, z) - 0.5) * 0.14;
        double band = y + (StructKit.vnoise(44, x * 0.15, z * 0.15) - 0.5) * 10;
        if (slope >= 3.5) {
            return patch < 0.5 ? "basalt[axis=y]" : "smooth_basalt";
        }
        if (flow > 0.66) {
            return patch < 0.45 ? "blackstone" : patch < 0.82 ? "basalt[axis=y]" : "smooth_basalt";
        }
        if (band < 14) {
            if (patch < 0.30) {
                return "gravel";
            }
            if (patch < 0.56) {
                return "tuff";
            }
            if (patch < 0.70) {
                return y < 6 ? "coarse_dirt" : "tuff";
            }
            return "smooth_basalt";
        }
        if (band < 40) {
            if (patch < 0.27) {
                return "tuff";
            }
            if (patch < 0.52) {
                return "cobbled_deepslate";
            }
            if (patch < 0.76) {
                return "deepslate";
            }
            return "basalt[axis=y]";
        }
        if (patch < 0.24 && band > 50) {
            return "netherrack";
        }
        if (patch < 0.52) {
            return "blackstone";
        }
        if (patch < 0.8) {
            return "basalt[axis=y]";
        }
        return "cobbled_deepslate";
    }

    private static void mountain(StructBp b) {
        int R = RB + 12;
        for (int x = -R; x <= R; x++) {
            for (int z = CZ - R; z <= CZ + R; z++) {
                int h = height(x, z);
                if (h <= 0) {
                    continue;
                }
                double slope = Math.max(Math.max(Math.abs(height(x + 1, z) - h), Math.abs(height(x - 1, z) - h)),
                        Math.max(Math.abs(height(x, z + 1) - h), Math.abs(height(x, z - 1) - h)));
                String top = surf(x, z, h - 1, slope);
                boolean ashy = top.equals("gravel") || top.equals("tuff") || top.equals("coarse_dirt");
                for (int y = 0; y < h; y++) {
                    String s;
                    if (y == h - 1) {
                        s = top;
                    } else if (y == h - 2 && ashy) {
                        s = "tuff"; // capa de ceniza bajo la ceniza suelta
                    } else {
                        s = ROCK[(int) (StructKit.hash01(43 + y, x, z) * ROCK.length)];
                    }
                    b.set(x, y, z, s);
                }
            }
        }
    }

    private static void crater(StructBp b) {
        // lago de lava falsa y, debajo, el pozo hasta la cámara
        for (int x = -RL - 1; x <= RL + 1; x++) {
            for (int z = CZ - RL - 1; z <= CZ + RL + 1; z++) {
                double d = Math.hypot(x, z - CZ);
                if (d <= RL + 0.3) {
                    b.set(x, LAVA_Y, z, LAVA);
                    for (int y = F_HALL + 12; y < LAVA_Y; y++) {
                        b.set(x, y, z, "air");
                    }
                    for (int y = LAVA_Y + 1; y <= H + 6; y++) {
                        b.set(x, y, z, "air");
                    }
                }
            }
        }
        // paredes del pozo con vetas de magma y luz
        for (int y = F_HALL + 12; y < LAVA_Y; y++) {
            for (int i = 0; i < 48; i++) {
                double a = i * Math.PI * 2 / 48;
                int x = (int) Math.round(Math.cos(a) * (RL + 1));
                int z = CZ + (int) Math.round(Math.sin(a) * (RL + 1));
                if ("air".equals(b.get(x, y, z))) {
                    continue;
                }
                double k = StructKit.hash01(51 + y, x, z);
                b.set(x, y, z, k < 0.12 ? "magma_block" : k < 0.16 ? "shroomlight" : WALL[(int) (k * 100) % WALL.length]);
            }
        }
        // el corazón del volcán: bloque que hace las erupciones (parece lava)
        b.set(0, LAVA_Y, CZ, CORE);
        // fogatas de señal alrededor del lago: columnas de humo que se ven de lejos
        int[] ang = {20, 75, 140, 200, 255, 320};
        for (int deg : ang) {
            double a = Math.toRadians(deg);
            int x = (int) Math.round(Math.sin(a) * (RL + 2.6));
            int z = CZ - (int) Math.round(Math.cos(a) * (RL + 2.6));
            int h = height(x, z);
            if (h <= LAVA_Y) {
                continue;
            }
            b.set(x, h - 1, z, "hay_block[axis=y]");
            b.set(x, h, z, "campfire[lit=true,signal_fire=true,facing=north]");
            b.set(x, h + 1, z, "air");
        }
    }

    /** Ríos de lava falsa que bajan del borde del cráter (brillan; no queman ni se derraman). */
    private static void rivers(StructBp b) {
        double[] a0 = {0.55, 1.75, 2.9, 3.85, 5.15};
        for (int i = 0; i < a0.length; i++) {
            double ph = i * 1.7;
            for (double d = RR + 1.5; d < RB - 7; d += 0.5) {
                double a = a0[i] + 0.22 * Math.sin(d * 0.13 + ph) + 0.06 * Math.sin(d * 0.41 + ph * 2);
                int w = d < RB - 18 ? 1 : 0;
                for (int k = -w - 1; k <= w + 1; k++) {
                    double px = Math.sin(a) * d + Math.cos(a) * k * 0.9;
                    double pz = CZ - Math.cos(a) * d + Math.sin(a) * k * 0.9;
                    int x = (int) Math.round(px), z = (int) Math.round(pz);
                    int h = height(x, z);
                    if (h <= 1) {
                        continue;
                    }
                    boolean edge = Math.abs(k) == w + 1;
                    b.set(x, h - 1, z, edge ? "magma_block" : LAVA);
                    if (!edge) {
                        b.set(x, h - 2, z, "magma_block");
                    }
                }
            }
        }
    }

    // ================================================================== camino en espiral

    /** Sube desde el frente de la base dando una vuelta al volcán, con escalones, hasta el borde del cráter. */
    private static void trail(StructBp b) {
        // de la entrada (z = 0) al pie del camino, que empieza a la derecha del frente
        double ang = ANG0;
        double r0 = radiusAt(2.5);
        int sx0 = (int) Math.round(Math.sin(ang) * r0), sz0 = (int) Math.round(CZ - Math.cos(ang) * r0);
        road(b, 0, -2, 0, 4);
        road(b, 0, 4, sx0 - 3, sz0 - 3);
        road(b, sx0 - 3, sz0 - 3, sx0, sz0);
        // braseros a la entrada (apoyados en el suelo de verdad)
        for (int sx = -1; sx <= 1; sx += 2) {
            StructBp.Anchor an = b.anchor(sx * 3, 1);
            an.set(0, -1, 0, PBB);
            an.set(0, 0, 0, "polished_blackstone_wall");
            an.set(0, 1, 0, "magma_block");
            an.set(0, 2, 0, "campfire[lit=true,facing=north]");
        }
        Map<Long, Integer> floorOf = new HashMap<>();
        int lastF = 0;
        int lampCount = 0;
        double hgt = 0;
        while (hgt < H - 1) {
            double r = Math.max(RR + 1.5, radiusAt(hgt + 2.5));
            int f = (int) Math.floor(hgt);
            double px = Math.sin(ang) * r, pz = CZ - Math.cos(ang) * r;
            // dirección de avance (sentido antihorario visto desde arriba)
            double tx = Math.cos(ang), tz = Math.sin(ang);
            String up = Math.abs(tx) > Math.abs(tz) ? (tx > 0 ? "+x" : "-x") : (tz > 0 ? "+z" : "-z");
            for (double k = -1.6; k <= 1.6; k += 0.4) {
                int x = (int) Math.round(px + Math.cos(ang) * 0 + Math.sin(ang) * k);
                int z = (int) Math.round(pz - Math.cos(ang) * k);
                long key = StructBp.key(x, 0, z);
                Integer prev = floorOf.get(key);
                if (prev != null && prev >= f) {
                    continue;
                }
                floorOf.put(key, f);
                TRAIL.add(key);
                int colTop = Math.max(0, height(x, z));
                // relleno bajo el camino (que nunca flote) y el suelo del camino
                for (int y = Math.min(colTop, f - 1); y < f - 1; y++) {
                    b.set(x, y, z, ROCK[(int) (StructKit.hash01(61, x + y, z) * ROCK.length)]);
                }
                boolean step = f > lastF && f > 0;
                b.set(x, f - 1, z, step ? StructBp.stair(PBBS, up, false) : (StructKit.hash01(62, x, z) < 0.3 ? "blackstone" : PB));
                for (int y = f; y <= f + 3; y++) {
                    b.set(x, y, z, "air");
                }
            }
            if (f > lastF) {
                lastF = f;
            }
            // farolas cada tanto en el lado de fuera
            if (++lampCount % 34 == 0 && f > 2) {
                int x = (int) Math.round(px + Math.sin(ang) * 2.6), z = (int) Math.round(pz - Math.cos(ang) * 2.6);
                if (b.get(x, f - 1, z) != null && !"air".equals(b.get(x, f - 1, z))) {
                    b.set(x, f, z, "polished_blackstone_wall");
                    b.set(x, f + 1, z, "lantern");
                }
            }
            ang += 0.42 / r;
            hgt += 0.42 / 2.7;
        }
    }

    /** Donde empieza el camino (ángulo desde el frente, hacia la derecha). */
    private static final double ANG0 = 0.75;

    private static String roadBlock(int x, int z) {
        return StructKit.hash01(64, x, z) < 0.3 ? "blackstone" : PB;
    }

    /** Camino de losas de 3 de ancho sobre la ceniza, en línea recta de (x0, z0) a (x1, z1). */
    private static void road(StructBp b, int x0, int z0, int x1, int z1) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : i / (double) n;
            int cx = (int) Math.round(x0 + (x1 - x0) * t), cz = (int) Math.round(z0 + (z1 - z0) * t);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cx + dx, z = cz + dz;
                    int h = height(x, z);
                    if (h > 1 && Math.hypot(x, z - CZ) < RB - 2) {
                        continue;
                    }
                    if (h > 0) {
                        // donde aún asoma la ladera, se corta (el resto del camino va sobre el terreno real, en la mezcla)
                        b.set(x, -1, z, roadBlock(x, z));
                        for (int y = 0; y <= 3; y++) {
                            b.set(x, y, z, "air");
                        }
                    }
                    ROAD.add(StructBp.key(x, 0, z));
                    TRAIL.add(StructBp.key(x, 0, z));
                }
            }
        }
    }

    // ================================================================== dungeon: piezas

    /** Elipsoide de aire (sala abovedada) con suelo plano en {@code floor}. */
    private static void dome(StructBp b, int cx, int cz, double r, int floor, int wallH, int domeH) {
        int ri = (int) Math.ceil(r);
        for (int x = -ri; x <= ri; x++) {
            for (int z = -ri; z <= ri; z++) {
                double d = Math.hypot(x, z);
                if (d > r) {
                    continue;
                }
                int top = floor + wallH + (int) Math.round(domeH * Math.sqrt(Math.max(0, 1 - (d / r) * (d / r))));
                for (int y = floor; y <= top; y++) {
                    b.set(cx + x, y, cz + z, "air");
                }
            }
        }
    }

    /** Pone luz de hongo en las paredes de una sala redonda, cada cierto ángulo. */
    private static void ringLights(StructBp b, int cx, int cz, double r, int y, int n, String block) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n + 0.2;
            int x = cx + (int) Math.round(Math.cos(a) * (r + 1));
            int z = cz + (int) Math.round(Math.sin(a) * (r + 1));
            if (!"air".equals(b.get(x, y, z))) {
                b.set(x, y, z, block);
            }
        }
    }

    /** Farol que cuelga del techo con una cadena: busca el techo hacia arriba desde y0. */
    private static void hangLamp(StructBp b, int x, int y0, int z, int chain) {
        int y = y0;
        while ("air".equals(b.get(x, y + 1, z)) && y < y0 + 20) {
            y++;
        }
        if (b.get(x, y + 1, z) == null || "air".equals(b.get(x, y + 1, z))) {
            return;
        }
        for (int i = 0; i < chain; i++) {
            b.set(x, y - i, z, "chain[axis=y]");
        }
        b.set(x, y - chain, z, "lantern[hanging=true]");
    }

    /** Cascada de lava falsa en un nicho de la pared (de y0 a y1), con borde de magma. */
    private static void lavafall(StructBp b, int x, int z, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            b.set(x, y, z, LAVA);
        }
    }

    private static void landingHall(StructBp b) {
        double r = 11;
        dome(b, 0, CZ, r, F_HALL, 6, 6);
        // suelo: losas oscuras con un anillo de piedra negra dorada (donde se aterriza)
        for (int x = -11; x <= 11; x++) {
            for (int z = -11; z <= 11; z++) {
                double d = Math.hypot(x, z);
                if (d > r) {
                    continue;
                }
                String s = Math.abs(d - 4.5) < 0.6 || Math.abs(d - 8.5) < 0.5 ? GILD : (x + z & 1) == 0 ? PBB : PB;
                if (d < 1.0) {
                    s = "shroomlight";
                }
                b.set(x, F_HALL - 1, CZ + z, s);
            }
        }
        ringLights(b, 0, CZ, r, F_HALL + 3, 10, "shroomlight");
        ringLights(b, 0, CZ, r - 2.5, F_HALL + 10, 6, "shroomlight");
        // cuatro cascadas de lava falsa en la pared
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            int x = (int) Math.round(Math.cos(a) * (r + 1)), z = CZ + (int) Math.round(Math.sin(a) * (r + 1));
            lavafall(b, x, z, F_HALL, F_HALL + 6);
            b.set(x, F_HALL - 1, z, "magma_block");
        }
        // cadenas que cuelgan de la bóveda
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3;
            int x = (int) Math.round(Math.cos(a) * 6), z = CZ + (int) Math.round(Math.sin(a) * 6);
            int top = F_HALL + 6 + (int) Math.round(6 * Math.sqrt(1 - 36.0 / 121));
            for (int y = top - 3; y <= top; y++) {
                b.set(x, y, z, "chain[axis=y]");
            }
            b.set(x, top - 4, z, "lantern[hanging=true]");
        }
    }

    private static void bridgeChamber(StructBp b) {
        int x0 = -13, x1 = 13, z0 = 73, z1 = 83;
        // la caverna (desde el foso hasta el techo)
        b.box(x0, F_PIT, z0, x1, F_PIT + 11, z1, "air");
        // foso: suelo de magma (quema si no vas agachado) con algo de basalto
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.set(x, F_PIT - 1, z, StructKit.hash01(71, x, z) < 0.8 ? "magma_block" : "basalt[axis=y]");
            }
        }
        // repisas a los dos lados (macizas hasta el foso)
        for (int sx : new int[]{x0, x1 - 2}) {
            for (int x = sx; x <= sx + 2; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int y = F_PIT - 1; y < F_BRIDGE - 1; y++) {
                        b.set(x, y, z, WALL[(int) (StructKit.hash01(72 + y, x, z) * WALL.length)]);
                    }
                    b.set(x, F_BRIDGE - 1, z, (x + z & 1) == 0 ? PBB : PB);
                }
            }
        }
        // el puente: 3 de ancho, de basalto agrietado
        for (int x = x0 + 3; x <= x1 - 3; x++) {
            for (int z = 77; z <= 79; z++) {
                b.set(x, F_BRIDGE - 1, z, CRACK);
            }
        }
        // escalera para volver del foso a la repisa de la entrada
        for (int i = 0; i < 3; i++) {
            int x = x0 + 3 + i;
            int sy = F_BRIDGE - 2 - i;
            for (int z = 81; z <= 83; z++) {
                for (int y = F_PIT - 1; y < sy; y++) {
                    b.set(x, y, z, PBB);
                }
                b.set(x, sy, z, StructBp.stair(PBBS, "-x", false));
            }
        }
        // luz: hongos en el techo y en las paredes; braseros en las repisas
        for (int x = x0 + 2; x <= x1 - 2; x += 5) {
            b.set(x, F_PIT + 12, 75, "shroomlight");
            b.set(x, F_PIT + 12, 81, "shroomlight");
        }
        for (int x = -8; x <= 8; x += 8) {
            hangLamp(b, x, F_BRIDGE + 4, 78, 3);
        }
        for (int z = z0 + 1; z <= z1 - 1; z += 4) {
            b.set(x0 - 1, F_PIT + 2, z, "shroomlight");
            b.set(x1 + 1, F_PIT + 2, z, "shroomlight");
        }
        for (int[] c : new int[][]{{x0, z0}, {x0, z1}, {x1, z0}, {x1, z1}}) {
            b.set(c[0], F_BRIDGE, c[1], "magma_block");
            b.set(c[0], F_BRIDGE + 1, c[1], "campfire[lit=true,facing=north]");
        }
        lavafall(b, 0, z1 + 1, F_PIT, F_PIT + 9);
        lavafall(b, -6, z1 + 1, F_PIT, F_PIT + 7);
        lavafall(b, 6, z1 + 1, F_PIT, F_PIT + 7);
    }

    private static void eruptionHall(StructBp b) {
        int x0 = -16, x1 = 14, z0 = 66, z1 = 86;
        b.box(x0, F_ERUPT, z0, x1, F_ERUPT + 8, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.set(x, F_ERUPT - 1, z, (x * 3 + z) % 5 == 0 ? "polished_basalt[axis=y]" : (x + z & 1) == 0 ? PB : "smooth_basalt");
            }
        }
        // filas de respiraderos: hay que cruzar entre erupciones
        for (int z : new int[]{70, 74, 78, 82}) {
            for (int x = x0 + 1; x <= x1 - 1; x++) {
                if ((x + z / 2) % 2 == 0) {
                    b.set(x, F_ERUPT - 1, z, VENT);
                } else {
                    b.set(x, F_ERUPT - 1, z, "polished_basalt[axis=y]");
                }
            }
        }
        // columnas de basalto (para cubrirse de los diablillos)
        for (int[] c : new int[][]{{-11, 72}, {-11, 80}, {-3, 76}, {5, 72}, {5, 80}, {11, 76}}) {
            for (int y = F_ERUPT; y <= F_ERUPT + 8; y++) {
                b.set(c[0], y, c[1], "polished_basalt[axis=y]");
            }
            b.set(c[0], F_ERUPT + 4, c[1], "shroomlight");
        }
        for (int x = x0 + 2; x <= x1 - 2; x += 6) {
            for (int z = z0 + 2; z <= z1 - 2; z += 6) {
                b.set(x, F_ERUPT + 9, z, "shroomlight");
            }
        }
        for (int z = z0 + 2; z <= z1 - 2; z += 5) {
            b.set(x1 + 1, F_ERUPT + 2, z, "shroomlight");
            b.set(x0 - 1, F_ERUPT + 2, z, "shroomlight");
        }
        for (int x = x0 + 4; x <= x1 - 4; x += 8) {
            for (int z : new int[]{72, 80}) {
                hangLamp(b, x, F_ERUPT + 4, z, 2);
            }
        }
        lavafall(b, x1 + 1, 68, F_ERUPT, F_ERUPT + 6);
        lavafall(b, x1 + 1, 84, F_ERUPT, F_ERUPT + 6);
    }

    /** Altar de los cultistas: un nicho al oeste de la sala de erupciones, un escalón más arriba. */
    private static void cultistAltar(StructBp b) {
        int x0 = -25, x1 = -18, z0 = 71, z1 = 81;
        b.box(x0, F_ERUPT + 1, z0, x1, F_ERUPT + 8, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.set(x, F_ERUPT, z, (x + z & 1) == 0 ? PBB : GILD);
            }
        }
        // escalón de entrada (la pared de la sala es x = -17: se abre un arco)
        for (int z = 75; z <= 77; z++) {
            b.set(-17, F_ERUPT, z, StructBp.stair(PBBS, "-x", false));
            for (int y = F_ERUPT + 1; y <= F_ERUPT + 4; y++) {
                b.set(-17, y, z, "air");
            }
        }
        // altar con piedra negra dorada, obsidiana llorona y velas
        b.box(-24, F_ERUPT + 1, 74, -23, F_ERUPT + 1, 78, PBB);
        b.set(-24, F_ERUPT + 2, 76, "crying_obsidian");
        b.set(-23, F_ERUPT + 2, 75, "candle[candles=3,lit=true]");
        b.set(-23, F_ERUPT + 2, 77, "candle[candles=2,lit=true]");
        b.set(-25, F_ERUPT + 3, 76, "shroomlight");
        b.set(-25, F_ERUPT + 4, 74, "magma_block");
        b.set(-25, F_ERUPT + 4, 78, "magma_block");
        for (int z = z0 + 1; z <= z1 - 1; z += 3) {
            b.set(x0 - 1, F_ERUPT + 3, z, "shroomlight");
        }
        b.set(-21, F_ERUPT + 9, 76, "shroomlight");
        hangLamp(b, -21, F_ERUPT + 4, 73, 2);
        hangLamp(b, -21, F_ERUPT + 4, 79, 2);
    }

    /**
     * Sala del tesoro, al este de la sala de erupciones, tras una reja (canal 2) que solo se abre al encender a la vez los cuatro
     * braseros de la sala (cada uno arde 25 s): hay que cruzar las filas de respiraderos corriendo.
     */
    private static void treasureRoom(StructBp b) {
        int x0 = 16, x1 = 23, z0 = 72, z1 = 80, f = F_ERUPT;
        b.box(x0, f, z0, x1, f + 5, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                b.set(x, f - 1, z, (x + z & 1) == 0 ? GILD : PBB);
            }
        }
        // la reja de la puerta y su marco
        for (int z = 75; z <= 77; z++) {
            for (int y = f; y <= f + 3; y++) {
                b.set(15, y, z, GATE + "[axis=z,open=false,channel=2]");
            }
            b.set(15, f - 1, z, PBB);
        }
        for (int y = f; y <= f + 4; y++) {
            b.set(15, y, 74, PBB);
            b.set(15, y, 78, PBB);
        }
        for (int z = 74; z <= 78; z++) {
            b.set(15, f + 4, z, PBB);
        }
        b.set(15, f + 5, 76, "gilded_blackstone");
        // tesoro a la vista: oro, velas, una cascada de lava al fondo
        b.set(23, f, 72, "raw_gold_block");
        b.set(23, f, 80, "raw_gold_block");
        b.set(22, f, 72, "gold_block");
        b.set(23, f + 1, 72, "candle[candles=3,lit=true]");
        b.set(23, f + 1, 80, "candle[candles=4,lit=true]");
        b.set(16, f, 72, GILD);
        b.set(16, f, 80, GILD);
        b.set(16, f + 1, 72, "candle[candles=2,lit=true]");
        b.set(16, f + 1, 80, "candle[candles=2,lit=true]");
        lavafall(b, x1 + 1, 76, f, f + 4);
        b.set(x1 + 1, f - 1, 76, "magma_block");
        for (int z = z0 + 1; z <= z1 - 1; z += 3) {
            b.set(x1 + 1, f + 2, z, "shroomlight");
        }
        b.set(19, f + 6, 76, "shroomlight");
        hangLamp(b, 19, f + 3, 73, 1);
        hangLamp(b, 19, f + 3, 79, 1);
        // los cuatro braseros de la sala de erupciones (canal 2)
        for (int[] c : BRAZIERS) {
            b.set(c[0], F_ERUPT - 1, c[1], PBB);
            b.set(c[0], F_ERUPT, c[1], BRAZ + "[lit=false,channel=2]");
        }
    }

    /** Braseros de la sala de erupciones (x, z): dos a cada lado de las filas de respiraderos. */
    static final int[][] BRAZIERS = {{-15, 67}, {-15, 85}, {9, 67}, {9, 85}};

    /**
     * Cámara del guardián: la Gran Salamandra (mini jefe) espera entre la sala de erupciones y el santuario. Al vencerla se abre la
     * reja (canal 1) hacia el santuario y el cofre del premio.
     */
    private static void guardianChamber(StructBp b) {
        double r = 7.5;
        dome(b, GX, GZ, r, F_GUARD, 4, 3);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                double d = Math.hypot(x, z);
                if (d > r) {
                    continue;
                }
                double k = StructKit.hash01(111, x, z);
                String s = Math.abs(d - 5.0) < 0.55 ? "magma_block" : d < 2.0 ? GILD : k < 0.5 ? PB : k < 0.8 ? "smooth_basalt" : "basalt[axis=y]";
                b.set(GX + x, F_GUARD - 1, GZ + z, s);
            }
        }
        // dos pozas de lava de verdad: la salamandra se cura en ellas (hay que sacarla a tierra firme)
        for (int[] c : new int[][]{{-3, 3}, {3, -3}}) {
            for (int dx = 0; dx <= 1; dx++) {
                for (int dz = 0; dz <= 1; dz++) {
                    b.set(GX + c[0] + dx, F_GUARD - 1, GZ + c[1] + dz, "lava");
                    b.set(GX + c[0] + dx, F_GUARD - 2, GZ + c[1] + dz, "basalt[axis=y]");
                }
            }
        }
        // huesos de los que no lo lograron
        b.set(GX + 5, F_GUARD, GZ + 3, "bone_block[axis=x]");
        b.set(GX - 4, F_GUARD, GZ - 4, "bone_block[axis=z]");
        b.set(GX + 2, F_GUARD, GZ - 6, "bone_block[axis=y]");
        ringLights(b, GX, GZ, r, F_GUARD + 2, 8, "shroomlight");
        ringLights(b, GX, GZ, r - 2, F_GUARD + 6, 5, "shroomlight");
        for (int i = 0; i < 4; i++) {
            double a = i * Math.PI / 2 + Math.PI / 4;
            int x = GX + (int) Math.round(Math.cos(a) * (r + 1)), z = GZ + (int) Math.round(Math.sin(a) * (r + 1));
            lavafall(b, x, z, F_GUARD, F_GUARD + 4);
        }
    }

    private static void sanctuary(StructBp b) {
        int cz = 90;
        double r = 10;
        dome(b, 0, cz, r, F_SANCT, 6, 7);
        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                double d = Math.hypot(x, z);
                if (d > r) {
                    continue;
                }
                String s = Math.abs(d - 6.5) < 0.6 ? GILD : Math.abs(d - 9) < 0.5 ? "magma_block" : (x + z & 1) == 0 ? PBB : "polished_basalt[axis=y]";
                b.set(x, F_SANCT - 1, cz + z, s);
            }
        }
        // altar escalonado con la Piedra de Portal: Volcán
        b.box(-2, F_SANCT, cz - 2, 2, F_SANCT, cz + 2, PBB);
        for (int i = -2; i <= 2; i++) {
            b.set(i, F_SANCT, cz - 3, StructBp.stair(PBBS, "+z", false));
        }
        b.box(-1, F_SANCT + 1, cz - 1, 1, F_SANCT + 1, cz + 1, GILD);
        b.set(0, F_SANCT + 2, cz, M + "portal_stone_volcano");
        // el corazón de magma encadenado que cuelga sobre el altar
        int hy = F_SANCT + 7;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    double d = Math.sqrt(x * x + y * y * 1.2 + z * z);
                    if (d <= 2.2) {
                        double k = StructKit.hash01(81 + y, x, z);
                        b.set(x, hy + y, cz + z, d < 1.2 ? "shroomlight" : k < 0.55 ? "magma_block" : k < 0.8 ? "crying_obsidian" : "obsidian");
                    }
                }
            }
        }
        int top = F_SANCT + 6 + 7;
        for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            for (int y = hy + 2; y <= top; y++) {
                int x = c[0] + (c[0] > 0 ? (y - hy) / 3 : -(y - hy) / 3);
                int z = cz + c[1] + (c[1] > 0 ? (y - hy) / 3 : -(y - hy) / 3);
                if ("air".equals(b.get(x, y, z)) || b.get(x, y, z) == null) {
                    b.set(x, y, z, "chain[axis=y]");
                }
            }
        }
        ringLights(b, 0, cz, r, F_SANCT + 2, 8, "shroomlight");
        for (int[] c : new int[][]{{6, -4}, {-6, -4}, {6, 4}, {-6, 4}}) {
            hangLamp(b, c[0], F_SANCT + 5, cz + c[1], 2);
        }
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3 + Math.PI / 6;
            int x = (int) Math.round(Math.cos(a) * (r + 1)), z = cz + (int) Math.round(Math.sin(a) * (r + 1));
            if (z > cz - 6) {
                lavafall(b, x, z, F_SANCT, F_SANCT + 5);
            }
        }
        // braseros alrededor del altar
        for (int[] c : new int[][]{{5, 0}, {-5, 0}, {0, 5}}) {
            b.set(c[0], F_SANCT, cz + c[1], "magma_block");
            b.set(c[0], F_SANCT + 1, cz + c[1], "soul_campfire[lit=true,facing=north]");
        }
    }

    // ================================================================== pasillos

    /**
     * Pasillo de 3 de ancho y 4 de alto que BAJA por los puntos dados (x, suelo, z), en tramos rectos (cambia x o z, no los dos).
     * El suelo baja como mucho 1 por bloque; donde baja se pone una escalera. Se dibuja siempre en el sentido de bajada.
     */
    private static void path(StructBp b, int[][] pts) {
        int lastFloor = Integer.MIN_VALUE;
        int lamp = 0;
        for (int i = 0; i + 1 < pts.length; i++) {
            int[] p = pts[i], q = pts[i + 1];
            int dx = Integer.signum(q[0] - p[0]), dz = Integer.signum(q[2] - p[2]);
            int len = Math.max(Math.abs(q[0] - p[0]), Math.abs(q[2] - p[2]));
            for (int s = 0; s <= len; s++) {
                int x = p[0] + dx * s, z = p[2] + dz * s;
                int f = len == 0 ? q[1] : p[1] + (int) Math.round((q[1] - p[1]) * (s / (double) len));
                boolean down = lastFloor != Integer.MIN_VALUE && f == lastFloor - 1;
                String up = dx > 0 ? "-x" : dx < 0 ? "+x" : dz > 0 ? "-z" : "+z";
                for (int k = -1; k <= 1; k++) {
                    int cx = x + (dz != 0 ? k : 0), cz = z + (dx != 0 ? k : 0);
                    b.set(cx, f - 1, cz, down ? StructBp.stair(PBBS, up, false) : (StructKit.hash01(91, cx, cz) < 0.25 ? "blackstone" : PB));
                    for (int y = f; y <= f + (down ? 4 : 3); y++) {
                        b.set(cx, y, cz, "air");
                    }
                }
                if (++lamp % 8 == 0) {
                    b.set(x, f + 4, z, "shroomlight");
                }
                lastFloor = f;
            }
        }
    }

    private static void passages(StructBp b) {
        // cámara del cráter -> repisa de entrada del puente (baja 8)
        path(b, new int[][]{{-6, F_HALL, 66}, {-6, F_HALL, 72}, {-12, F_HALL - 6, 72}, {-12, F_BRIDGE, 76}});
        // repisa de salida del puente -> sala de erupciones (baja 16)
        path(b, new int[][]{{12, F_BRIDGE, 76}, {12, F_BRIDGE, 71}, {12, F_BRIDGE - 9, 62}, {5, F_ERUPT, 62}, {5, F_ERUPT, 67}});
        // sala de erupciones -> cámara del guardián (baja 16)
        path(b, new int[][]{{12, F_ERUPT, 85}, {12, F_ERUPT, 88}, {28, F_GUARD, 88}, {28, F_GUARD, 96}});
        // cámara del guardián -> santuario (tras la reja que se abre al vencer a la Gran Salamandra)
        path(b, new int[][]{{14, F_GUARD, 100}, {11, F_GUARD, 100}, {11, F_SANCT, 95}, {8, F_SANCT, 95}});
        // la reja del guardián (canal 1) y el hueco del cofre de premio, junto al pasillo
        for (int z = 99; z <= 101; z++) {
            for (int y = F_GUARD; y <= F_GUARD + 3; y++) {
                b.set(14, y, z, GATE + "[axis=z,open=false,channel=1]");
            }
        }
        b.box(12, F_GUARD, 102, 13, F_GUARD + 2, 103, "air");
        b.box(12, F_GUARD - 1, 102, 13, F_GUARD - 1, 103, PBB);
        b.set(13, F_GUARD + 2, 103, "shroomlight");
    }

    /** Del santuario al frente del volcán: sube por un túnel largo y termina en un escalón de 2 hacia fuera (solo de salida). */
    private static void exitTunnel(StructBp b) {
        int mouthZ = 12;
        // se dibuja de la boca (arriba) hacia el santuario (abajo)
        path(b, new int[][]{{0, 2, mouthZ}, {0, 2, mouthZ + 3}, {0, F_SANCT, mouthZ + 13}, {0, F_SANCT, 81}});
        // plaza fuera de la boca, al nivel del suelo (y 0): el escalón de 2 no se puede subir desde fuera
        for (int x = -4; x <= 4; x++) {
            for (int z = 2; z < mouthZ; z++) {
                b.set(x, -1, z, Math.abs(x) == 4 ? "blackstone" : PB);
                for (int y = 0; y <= 9; y++) {
                    b.set(x, y, z, "air");
                }
            }
        }
        // marco de la boca
        for (int y = 1; y <= 6; y++) {
            b.set(-2, y, mouthZ, PBB);
            b.set(2, y, mouthZ, PBB);
        }
        for (int x = -2; x <= 2; x++) {
            b.set(x, 6, mouthZ, PBB);
        }
        b.set(-3, 3, mouthZ - 1, "lantern");
        b.set(-3, 2, mouthZ - 1, "polished_blackstone_wall");
        b.set(-3, 1, mouthZ - 1, "polished_blackstone_wall");
        b.set(-3, 0, mouthZ - 1, PBB);
        b.set(3, 3, mouthZ - 1, "lantern");
        b.set(3, 2, mouthZ - 1, "polished_blackstone_wall");
        b.set(3, 1, mouthZ - 1, "polished_blackstone_wall");
        b.set(3, 0, mouthZ - 1, PBB);
    }

    /** Bajo tierra (y < 0) todo hueco del plano queda cerrado: se ponen paredes donde el plano no decía nada. */
    private static void encaseUnderground(StructBp b) {
        List<long[]> add = new ArrayList<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            if (!"air".equals(e.getValue())) {
                continue;
            }
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (y > 0) {
                continue;
            }
            int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            for (int[] n : nb) {
                int nx = x + n[0], ny = y + n[1], nz = z + n[2];
                if (ny > -1) {
                    continue;
                }
                if (b.get(nx, ny, nz) == null) {
                    add.add(new long[]{nx, ny, nz});
                }
            }
        }
        for (long[] a : add) {
            int x = (int) a[0], y = (int) a[1], z = (int) a[2];
            if (b.get(x, y, z) == null) {
                b.set(x, y, z, WALL[(int) (StructKit.hash01(99 + y, x, z) * WALL.length)]);
            }
        }
    }

    // ================================================================== ambiente de dentro

    /**
     * Las paredes de las salas y pasillos: manchas grandes de basalto, piedra negra y pizarra (ruido 3D, no al azar) y vetas de
     * magma que brillan. En los techos, piedra de la que cae ceniza y estalactitas de basalto en las salas altas.
     */
    private static void interiorDetail(StructBp b) {
        Set<Long> wall = new HashSet<>();
        List<Long> ceilings = new ArrayList<>();
        int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            if (!"air".equals(e.getValue())) {
                continue;
            }
            long k = e.getKey();
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (!inside(x, y, z)) {
                continue; // solo las salas y pasillos de dentro (no el camino de fuera ni el cráter)
            }
            for (int[] n : nb) {
                int nx = x + n[0], ny = y + n[1], nz = z + n[2];
                String s = b.get(nx, ny, nz);
                if (s != null && ROCKY.contains(s)) {
                    long nk = StructBp.key(nx, ny, nz);
                    if (wall.add(nk) && n[1] == 1) {
                        ceilings.add(nk);
                    }
                }
            }
        }
        for (long k : wall) {
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            double v = StructKit.vnoise3(202, x * 0.11, y * 0.17, z * 0.11);
            if (Math.abs(v - 0.5) < 0.03 && StructKit.hash01(203 + y, x, z) < 0.85) {
                b.set(x, y, z, VEIN);
                continue;
            }
            double n = StructKit.vnoise3(201, x * 0.19, y * 0.19, z * 0.19) + (StructKit.hash01(204 + y, x, z) - 0.5) * 0.1;
            b.set(x, y, z, n < 0.34 ? "blackstone" : n < 0.62 ? "basalt[axis=y]" : n < 0.82 ? "smooth_basalt" : "cobbled_deepslate");
        }
        java.util.Collections.sort(ceilings);
        for (long k : ceilings) {
            int x = StructBp.kx(k), y = StructBp.ky(k), z = StructBp.kz(k);
            if (VEIN.equals(b.get(x, y, z))) {
                continue;
            }
            double h1 = StructKit.hash01(205 + y, x, z);
            if (h1 < 0.07) {
                b.set(x, y, z, ASH_CEIL);
                continue;
            }
            // estalactitas: solo con mucho aire debajo y lejos del pozo por el que se cae desde el cráter
            if (h1 > 0.955 && Math.hypot(x, z - CZ) > RL + 3) {
                int free = 0;
                while (free < 8 && "air".equals(b.get(x, y - 1 - free, z))) {
                    free++;
                }
                if (free < 7) {
                    continue;
                }
                int len = 1 + (int) (StructKit.hash01(206 + y, x, z) * 3);
                for (int i = 1; i <= len; i++) {
                    b.set(x, y - i, z, "basalt[axis=y]");
                }
                b.set(x, y - len - 1, z, "polished_blackstone_wall");
            }
        }
    }

    /** Caja de la dungeon (sin mirar si está bajo la ladera). */
    private static boolean insideRaw(int x, int y, int z) {
        return x >= -32 && x <= 34 && z >= 10 && z <= 114 && y >= -14 && y <= H;
    }

    /**
     * ¿La posición local está dentro de la dungeon (bajo la ladera, no al aire libre)? Para la música del volcán: suena dentro, no
     * en el camino de fuera ni en el cráter.
     */
    static boolean inside(int x, int y, int z) {
        if (!insideRaw(x, y, z)) {
            return false;
        }
        if (Math.hypot(x, z - CZ) <= RL + 0.5 && y > F_HALL + 11) {
            return y < LAVA_Y; // cayendo por el pozo
        }
        return y < height(x, z) - 2;
    }

    // ================================================================== por fuera

    /** Fumarolas en la ladera: bocas que echan vapor, con costra amarilla de azufre alrededor. */
    private static void slopeFumaroles(StructBp b) {
        java.util.Random r = new java.util.Random(4410);
        int placed = 0;
        for (int tries = 0; tries < 200 && placed < 11; tries++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = RR + 4 + r.nextDouble() * (RB - RR - 12);
            int x = (int) Math.round(Math.sin(a) * d), z = CZ - (int) Math.round(Math.cos(a) * d);
            int h = height(x, z);
            if (h < 8 || nearTrail(x, z, 3) || !solidTop(b, x, h - 1, z)) {
                continue;
            }
            b.set(x, h - 1, z, FUMAROLA);
            crust(b, x, z, r);
            placed++;
        }
    }

    private static boolean solidTop(StructBp b, int x, int y, int z) {
        String s = b.get(x, y, z);
        return s != null && !s.equals("air") && !s.startsWith(M) && !s.contains("magma") && b.get(x, y + 1, z) == null;
    }

    /** Costra de azufre y minerales alrededor de una fumarola de la ladera. */
    private static void crust(StructBp b, int x, int z, java.util.Random r) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0 || dx * dx + dz * dz > 5 || r.nextDouble() > 0.62) {
                    continue;
                }
                int h = height(x + dx, z + dz);
                if (h > 0 && solidTop(b, x + dx, h - 1, z + dz) && !nearTrail(x + dx, z + dz, 1)) {
                    b.set(x + dx, h - 1, z + dz, r.nextDouble() < 0.6 ? "yellow_terracotta" : r.nextDouble() < 0.5 ? "white_terracotta" : "tuff");
                }
            }
        }
    }

    private static boolean nearTrail(int x, int z, int rad) {
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                if (TRAIL.contains(StructBp.key(x + dx, 0, z + dz))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** En el cráter de cada cono: una fumarola con un anillo de magma. */
    private static void coneCraters(StructBp b) {
        for (int i = 0; i < CONES.length; i++) {
            int cx = coneX(i), cz = coneZ(i);
            int h = height(cx, cz);
            if (h <= 1) {
                continue;
            }
            b.set(cx, h - 1, cz, FUMAROLA);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    int hh = height(cx + dx, cz + dz);
                    if (hh > 0) {
                        b.set(cx + dx, hh - 1, cz + dz, StructKit.hash01(320 + i, dx, dz) < 0.6 ? "magma_block" : "yellow_terracotta");
                    }
                }
            }
        }
    }

    /** Ángulos de los ríos de lava (desde el frente). */
    private static final double[] RIVERS = {0.55, 1.75, 2.9, 3.85, 5.15};

    /**
     * Al final de cada río, la lava que ya se enfrió: una lengua de obsidiana, basalto y algo de magma que baja hasta el llano y se
     * abre como un abanico.
     */
    private static void fans(StructBp b) {
        for (int i = 0; i < RIVERS.length; i++) {
            double ph = i * 1.7;
            for (double d = RB - 9; d < RB + 15; d += 0.5) {
                double a = RIVERS[i] + 0.22 * Math.sin(d * 0.13 + ph) + 0.06 * Math.sin(d * 0.41 + ph * 2);
                double hw = 1.5 + (d - (RB - 9)) * 0.2;
                for (double k = -hw; k <= hw; k += 0.5) {
                    int x = (int) Math.round(Math.sin(a) * d + Math.cos(a) * k), z = (int) Math.round(CZ - Math.cos(a) * d + Math.sin(a) * k);
                    double ragged = Math.abs(k) / hw + (StructKit.hash01(330 + i, x, z) - 0.5) * 0.5;
                    if (ragged > 0.95 || TRAIL.contains(StructBp.key(x, 0, z))) {
                        continue;
                    }
                    double n = StructKit.fbm(331, x, z, 3.0) + (StructKit.hash01(332, x, z) - 0.5) * 0.2;
                    String mat = n < 0.3 ? "obsidian" : n < 0.52 ? "basalt[axis=y]" : n < 0.7 ? "blackstone" : n < 0.8 ? "magma_block"
                            : n < 0.84 ? "crying_obsidian" : "obsidian";
                    int h = height(x, z);
                    if (h > 0) {
                        String cur = b.get(x, h - 1, z);
                        if (cur != null && !cur.startsWith(M) && b.get(x, h, z) == null) {
                            b.set(x, h - 1, z, mat);
                        }
                    } else {
                        FAN.put(StructBp.key(x, 0, z), mat);
                    }
                }
            }
        }
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Ceniza suelta del llano: manchas de grava, toba, tierra quemada y roca. */
    private static String ashTop(int x, int z, double e) {
        double n = StructKit.fbm(510, x, z, 5.0) + (StructKit.hash01(511, x, z) - 0.5) * 0.16 + Math.max(0, e) * 0.006;
        return n < 0.22 ? "gravel" : n < 0.40 ? "tuff" : n < 0.46 ? "coarse_dirt" : n < 0.62 ? "smooth_basalt" : n < 0.76 ? "basalt[axis=y]"
                : n < 0.88 ? "blackstone" : "tuff";
    }

    private static final String SUB_FOOT = "basalt[axis=y]|blackstone|tuff|basalt[axis=y]";
    private static final String SUB_APRON = "tuff|coarse_dirt|gravel|basalt[axis=y]";

    /**
     * El pie del volcán se funde con el terreno: bajo el volcán y hasta 5 bloques de él, todo a su altura (se quita lo que haya
     * encima y se rellena por debajo); luego una rampa suave de 13 a 20 bloques hasta la altura del terreno de alrededor. La ceniza
     * cubre la rampa y sigue en lenguas irregulares (más largas donde bajan los ríos); más allá el terreno queda como estaba.
     */
    private static void surroundings(StructBp b) {
        int R = RB + 42;
        for (int x = -R; x <= R; x++) {
            for (int z = CZ - R; z <= CZ + R; z++) {
                double d = Math.hypot(x, z - CZ);
                if (d > R) {
                    continue;
                }
                double a = Math.atan2(x, -(z - CZ));
                int h = height(x, z);
                double e = edge(x, z);
                long key = StructBp.key(x, 0, z);
                boolean foot = h > 0 || e <= 0;
                double ramp = 13 + 7 * around(501, a, 3.0);
                double w = foot ? 0 : smooth((e - 5) / ramp);
                double tongue = 0;
                for (double ra : RIVERS) {
                    double da = Math.abs(Math.atan2(Math.sin(a - ra), Math.cos(a - ra)));
                    tongue = Math.max(tongue, Math.exp(-(da / 0.13) * (da / 0.13)));
                }
                double reach = ramp + 4 + 14 * around(502, a, 4.0) + 12 * tongue;
                double ash = 1 - (e - 5) / reach + (StructKit.fbm(503, x, z, 9.0) - 0.5) * 0.55;
                String top = null;
                if (ROAD.contains(key)) {
                    top = roadBlock(x, z);
                } else if (FAN.containsKey(key)) {
                    top = FAN.get(key);
                } else if (foot) {
                    top = h > 0 ? "basalt[axis=y]|blackstone" : ashTop(x, z, e);
                } else if (ash > 0.42) {
                    top = ashTop(x, z, e);
                }
                if (w >= 0.999 && top == null) {
                    continue;
                }
                b.blend(x, z, w, top, foot ? SUB_FOOT : SUB_APRON);
                // matojos secos sobre la tierra quemada del llano
                if (!foot && top != null && top.equals("coarse_dirt") && StructKit.hash01(504, x, z) < 0.05 && !nearTrail(x, z, 1)) {
                    b.anchor(x, z).set(0, 0, 0, "dead_bush");
                }
            }
        }
    }

    /** Árboles quemados por el llano de ceniza: troncos oscuros, sin hojas, con alguna rama. */
    private static void charredTrees(StructBp b) {
        java.util.Random r = new java.util.Random(4406);
        int placed = 0;
        List<int[]> spots = new ArrayList<>();
        for (int tries = 0; tries < 400 && placed < 18; tries++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = RB + 5 + r.nextDouble() * 26;
            int x = (int) Math.round(Math.sin(a) * d), z = CZ - (int) Math.round(Math.cos(a) * d);
            StructBp.Blend bl = b.blendAt(x, z);
            if (bl == null || bl.top() == null || edge(x, z) < 5 || nearTrail(x, z, 3) || FAN.containsKey(StructBp.key(x, 0, z))
                    || Math.abs(x) <= 7 && z < 14) {
                continue;
            }
            boolean close = false;
            for (int[] o : spots) {
                close |= Math.abs(o[0] - x) + Math.abs(o[1] - z) < 7;
            }
            if (close) {
                continue;
            }
            spots.add(new int[]{x, z});
            StructBp.Anchor t = b.anchor(x, z);
            int h = 4 + r.nextInt(4);
            t.set(0, -1, 0, "coarse_dirt");
            for (int y = 0; y < h; y++) {
                t.set(0, y, 0, "dark_oak_log[axis=y]");
            }
            int branches = 1 + r.nextInt(3);
            for (int i = 0; i < branches; i++) {
                int by = 2 + r.nextInt(Math.max(1, h - 2));
                boolean alongX = r.nextBoolean();
                int sgn = r.nextBoolean() ? 1 : -1;
                int len = 1 + r.nextInt(2);
                for (int j = 1; j <= len; j++) {
                    t.set(alongX ? sgn * j : 0, by + (j == len ? 1 : 0), alongX ? 0 : sgn * j, "dark_oak_log[axis=" + (j == len ? "y" : alongX ? "x" : "z") + "]");
                }
            }
            if (r.nextDouble() < 0.5) {
                t.set(1, -1, 0, "coarse_dirt");
                t.set(-1, -1, 1, "coarse_dirt");
            }
            placed++;
        }
    }

    /** Rocas de basalto (y alguna de obsidiana) sueltas alrededor de la base, apoyadas en el suelo real. */
    private static void boulders(StructBp b) {
        java.util.Random r = new java.util.Random(4405);
        for (int i = 0; i < 30; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = RB + 1 + r.nextDouble() * 12;
            int cx = (int) Math.round(Math.sin(a) * d), cz = CZ - (int) Math.round(Math.cos(a) * d);
            if (nearTrail(cx, cz, 4) || Math.abs(cx) <= 7 && cz < CZ - RB + 14 || height(cx, cz) > 0) {
                continue; // no tapar la entrada, la salida ni el camino
            }
            double rr = 1.1 + r.nextDouble() * 1.7;
            int ri = (int) Math.ceil(rr);
            StructBp.Anchor an = b.anchor(cx, cz);
            boolean obs = r.nextDouble() < 0.2;
            for (int x = -ri; x <= ri; x++) {
                for (int y = -1; y <= ri; y++) {
                    for (int z = -ri; z <= ri; z++) {
                        if (x * x + (y + 0.5) * (y + 0.5) * 1.6 + z * z <= rr * rr) {
                            double k = r.nextDouble();
                            an.set(x, y, z, obs ? (k < 0.7 ? "obsidian" : "crying_obsidian") : k < 0.12 ? "magma_block" : k < 0.3 ? "blackstone" : "basalt[axis=y]");
                        }
                    }
                }
            }
        }
    }

    /** Fumarolas sueltas por el llano de ceniza (apoyadas en el suelo real), con costra de azufre. */
    private static void plainFumaroles(StructBp b) {
        java.util.Random r = new java.util.Random(4411);
        int placed = 0;
        for (int tries = 0; tries < 200 && placed < 6; tries++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = RB + 4 + r.nextDouble() * 16;
            int x = (int) Math.round(Math.sin(a) * d), z = CZ - (int) Math.round(Math.cos(a) * d);
            StructBp.Blend bl = b.blendAt(x, z);
            if (bl == null || bl.top() == null || height(x, z) > 0 || nearTrail(x, z, 4) || Math.abs(x) <= 7 && z < 14) {
                continue;
            }
            StructBp.Anchor an = b.anchor(x, z);
            an.set(0, -1, 0, FUMAROLA);
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if ((dx != 0 || dz != 0) && dx * dx + dz * dz <= 5 && r.nextDouble() < 0.6) {
                        an.set(dx, -1, dz, r.nextDouble() < 0.6 ? "yellow_terracotta" : r.nextDouble() < 0.5 ? "white_terracotta" : "tuff");
                    }
                }
            }
            placed++;
        }
    }

    // ================================================================== cofres

    private static void chest(StructBp b, int x, int y, int z, String facing, String kind, int lock) {
        String block = kind.equals("LOCKED") ? VCL : VC;
        String nbt = "{Kind:\"" + kind + "\",Stage:4" + (lock > 0 ? ",LockLevel:" + lock : "") + "}";
        b.setNbt(x, y, z, block + "[facing=" + facing + "]", nbt);
    }

    /**
     * Cofres del volcán (cada jugador tiene su botín): provisiones al aterrizar y antes del santuario, premio por cruzar el puente,
     * cerradura junto al altar de los cultistas, dos en la sala del tesoro (premio del acertijo de los braseros, uno con la cerradura
     * más difícil) y el gran cofre del guardián tras la reja.
     */
    private static void chests(StructBp b) {
        chest(b, 0, F_HALL, CZ - 10, "south", "ANTE", 0);
        chest(b, 12, F_BRIDGE, 82, "west", "TRIAL", 0);
        chest(b, -25, F_ERUPT + 1, 80, "east", "LOCKED", 3);
        chest(b, 22, F_ERUPT, 74, "west", "TRIAL", 0);
        chest(b, 22, F_ERUPT, 78, "west", "LOCKED", 5);
        chest(b, 12, F_GUARD, 103, "north", "CRYPT", 0);
        chest(b, -8, F_SANCT, 90, "east", "ANTE", 0);
    }

    // ================================================================== enemigos

    private static boolean free(StructBp b, int x, int y, int z) {
        String below = b.get(x, y - 1, z);
        return "air".equals(b.get(x, y, z)) && "air".equals(b.get(x, y + 1, z)) && below != null && !below.equals("air")
                && !below.contains("stairs") && !below.contains("campfire");
    }

    private static void mob(StructBp b, int x, int y, int z, String kind) {
        if (kind.equals("diablillo") ? "air".equals(b.get(x, y, z)) && "air".equals(b.get(x, y + 1, z)) : free(b, x, y, z)) {
            b.mobs.add(new StructBp.Mob(x, y, z, kind));
        } else {
            System.err.println("[Volcan] sin sitio para " + kind + " en " + x + "," + y + "," + z);
        }
    }

    private static void mobs(StructBp b) {
        // foso del puente: salamandras
        mob(b, -4, F_PIT, 75, "salamandra_lava");
        mob(b, 3, F_PIT, 81, "salamandra_lava");
        mob(b, 8, F_PIT, 76, "salamandra_lava");
        mob(b, 0, F_BRIDGE + 3, 78, "diablillo");
        // sala de erupciones
        mob(b, -8, F_ERUPT + 4, 76, "diablillo");
        mob(b, 2, F_ERUPT + 4, 80, "diablillo");
        mob(b, 9, F_ERUPT + 4, 72, "diablillo");
        mob(b, -6, F_ERUPT, 84, "salamandra_lava");
        // altar
        mob(b, -21, F_ERUPT + 1, 74, "cultista_volcan");
        mob(b, -21, F_ERUPT + 1, 79, "cultista_volcan");
        // sala del tesoro: dos diablillos esperan tras la reja
        mob(b, 19, F_ERUPT + 2, 76, "diablillo");
        mob(b, 20, F_ERUPT + 2, 73, "diablillo");
        // a mitad del pasillo hacia el guardián
        mob(b, 28, F_GUARD, 91, "cultista_volcan");
        // el guardián
        mob(b, GX, F_GUARD, GZ, "gran_salamandra");
    }
}
