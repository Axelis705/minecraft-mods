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
    private static final String[] SURF_LOW = {"tuff", "basalt[axis=y]", "blackstone", "deepslate", "smooth_basalt", "cobbled_deepslate"};
    private static final String[] SURF_MID = {"blackstone", "blackstone", "deepslate", "basalt[axis=y]", "cobbled_deepslate", "polished_deepslate"};
    private static final String[] SURF_TOP = {"blackstone", "blackstone", "deepslate", "cobbled_deepslate", "basalt[axis=y]", "netherrack"};
    private static final String[] WALL = {"basalt[axis=y]", "blackstone", "smooth_basalt", "polished_basalt[axis=y]"};
    private static final String PB = "polished_blackstone";
    private static final String PBB = "polished_blackstone_bricks";
    private static final String PBBS = "polished_blackstone_brick_stairs";
    private static final String GILD = "gilded_blackstone";

    /** Columnas por las que pasa el camino en espiral (para que los ríos y las rocas no lo tapen). */
    private static final Set<Long> TRAIL = new HashSet<>();

    static StructBp make() {
        StructBp b = new StructBp(4404);
        TRAIL.clear();
        b.foundation(-RB - 10, CZ - RB - 10, RB + 10, CZ + RB + 10, "blackstone");
        ashPlain(b);
        mountain(b);
        crater(b);
        rivers(b);
        trail(b);
        // la dungeon de dentro
        landingHall(b);
        bridgeChamber(b);
        eruptionHall(b);
        cultistAltar(b);
        sanctuary(b);
        passages(b);
        exitTunnel(b);
        encaseUnderground(b);
        boulders(b);
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

    /** Altura real de la columna (x, z): ladera con ruido y barrancos, borde del cráter dentado, cuenco del cráter. */
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
        if (d >= RB + 1) {
            return 0;
        }
        double base = profile(d);
        double t = (d - RR) / (RB - RR);
        double amp = Math.sin(Math.PI * Math.min(1, t * 1.3)) * 3.0 + 0.6;
        double n = (StructKit.fbm(4404, x, z, 9.0) - 0.5) * 2.0 * amp;
        double gully = Math.sin(a * 9 + StructKit.fbm(91, x, z, 14.0) * 4) * 1.2 * Math.min(1, t * 2.5);
        // borde del cráter dentado (pocos picos y bajos)
        double rim = d < RR + 3 ? Math.max(0, (StructKit.vnoise(5, x * 0.5, z * 0.5) - 0.62) * 9) : 0;
        return (int) Math.max(0, Math.round(base + n + gully + rim));
    }

    private static void ashPlain(StructBp b) {
        // suelo de ceniza alrededor de la base
        for (int x = -RB - 10; x <= RB + 10; x++) {
            for (int z = CZ - RB - 10; z <= CZ + RB + 10; z++) {
                double d = Math.hypot(x, z - CZ);
                if (d > RB + 10 + StructKit.vnoise(12, x * 0.2, z * 0.2) * 3) {
                    continue;
                }
                double k = StructKit.hash01(31, x, z);
                String s = k < 0.35 ? "coarse_dirt" : k < 0.6 ? "gravel" : k < 0.8 ? "tuff" : k < 0.92 ? "blackstone" : "basalt[axis=y]";
                boolean bush = d > RB - 2 && StructKit.hash01(32, x, z) < 0.035;
                // los matojos secos solo aguantan sobre tierra
                b.set(x, -1, z, bush ? "coarse_dirt" : s);
                if (bush) {
                    b.set(x, 0, z, "dead_bush");
                }
            }
        }
    }

    private static void mountain(StructBp b) {
        for (int x = -RB - 1; x <= RB + 1; x++) {
            for (int z = CZ - RB - 1; z <= CZ + RB + 1; z++) {
                int h = height(x, z);
                if (h <= 0) {
                    continue;
                }
                for (int y = 0; y < h; y++) {
                    boolean top = y == h - 1;
                    String s;
                    if (top) {
                        double k = StructKit.hash01(41, x, z);
                        double d = Math.hypot(x, z - CZ);
                        // manchas de magma más arriba, ceniza abajo
                        // franjas por altura (ceniza clara abajo, roca oscura arriba) con algo de ruido para que no sean rayas
                        double band = y + (StructKit.vnoise(44, x * 0.15, z * 0.15) - 0.5) * 10;
                        String[] pal = band < 18 ? SURF_LOW : band < 44 ? SURF_MID : SURF_TOP;
                        s = k < 0.04 + 0.08 * (1 - d / RB) ? "magma_block" : pal[(int) (StructKit.hash01(42, x, z) * pal.length)];
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
        // braseros a la entrada
        for (int sx = -1; sx <= 1; sx += 2) {
            b.set(sx * 3, -1, 1, PBB);
            b.set(sx * 3, 0, 1, "polished_blackstone_wall");
            b.set(sx * 3, 1, 1, "magma_block");
            b.set(sx * 3, 2, 1, "campfire[lit=true,facing=north]");
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

    /** Camino de losas de 3 de ancho sobre la ceniza, en línea recta de (x0, z0) a (x1, z1). */
    private static void road(StructBp b, int x0, int z0, int x1, int z1) {
        int n = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : i / (double) n;
            int cx = (int) Math.round(x0 + (x1 - x0) * t), cz = (int) Math.round(z0 + (z1 - z0) * t);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    int x = cx + dx, z = cz + dz;
                    if (height(x, z) > 1 && Math.hypot(x, z - CZ) < RB - 2) {
                        continue;
                    }
                    b.set(x, -1, z, StructKit.hash01(64, x, z) < 0.3 ? "blackstone" : PB);
                    for (int y = 0; y <= 3; y++) {
                        b.set(x, y, z, "air");
                    }
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
        // sala de erupciones -> santuario (baja 16, rodeando el santuario por fuera)
        path(b, new int[][]{{12, F_ERUPT, 85}, {12, F_ERUPT, 89}, {12, F_ERUPT - 10, 99}, {6, F_SANCT, 99}, {3, F_SANCT, 99}});
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

    /** Rocas de basalto sueltas alrededor de la base. */
    private static void boulders(StructBp b) {
        java.util.Random r = new java.util.Random(4405);
        for (int i = 0; i < 26; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = RB + 1 + r.nextDouble() * 8;
            int cx = (int) Math.round(Math.sin(a) * d), cz = CZ - (int) Math.round(Math.cos(a) * d);
            boolean near = false;
            for (int dx = -4; dx <= 4 && !near; dx++) {
                for (int dz = -4; dz <= 4 && !near; dz++) {
                    near = TRAIL.contains(StructBp.key(cx + dx, 0, cz + dz));
                }
            }
            if (near || Math.abs(cx) <= 6 && cz < CZ - RB + 12) {
                continue; // no tapar la entrada, la salida ni el camino
            }
            double rr = 1.2 + r.nextDouble() * 1.6;
            int ri = (int) Math.ceil(rr);
            for (int x = -ri; x <= ri; x++) {
                for (int y = 0; y <= ri; y++) {
                    for (int z = -ri; z <= ri; z++) {
                        if (x * x + y * y * 1.5 + z * z <= rr * rr) {
                            b.set(cx + x, y, cz + z, r.nextDouble() < 0.15 ? "magma_block" : "basalt[axis=y]");
                        }
                    }
                }
            }
        }
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
        // antes del santuario
        mob(b, 12, F_ERUPT - 10, 99, "cultista_volcan");
    }
}
