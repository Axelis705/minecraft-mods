package com.mundosdeaxelis.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Caja de herramientas para construir las estructuras: tejados, arcos, columnas, ventanas, mobiliario, ruinas, árboles...
 * Todo trabaja sobre un {@link StructBp} (solo texto, sin Minecraft), así se puede revisar fuera del juego.
 * <p>
 * Direcciones locales: {@code "+z"} = hacia dentro de la estructura, {@code "-z"} = hacia la entrada, {@code "+x"} / {@code "-x"} = a los lados.
 * En las escaleras, la dirección indica el lado ALTO (hacia donde se sube): es lo que usa Minecraft en su propiedad "facing".
 */
public final class StructKit {
    private StructKit() {
    }

    public static final String LEAF = "oak_leaves[persistent=true]";
    public static final String AZ = "azalea_leaves[persistent=true]";
    public static final String FAZ = "flowering_azalea_leaves[persistent=true]";
    public static final String SPR = "spruce_leaves[persistent=true]";
    public static final String DAK = "dark_oak_leaves[persistent=true]";
    public static final String AIR = "air";

    // ------------------------------------------------------------------ direcciones

    public static int dx(String d) {
        return d.equals("+x") ? 1 : d.equals("-x") ? -1 : 0;
    }

    public static int dz(String d) {
        return d.equals("+z") ? 1 : d.equals("-z") ? -1 : 0;
    }

    public static String opp(String d) {
        return switch (d) {
            case "+x" -> "-x";
            case "-x" -> "+x";
            case "+z" -> "-z";
            default -> "+z";
        };
    }

    /** Punto cardinal de Minecraft para una dirección local: +z = south, -z = north, +x = east, -x = west. */
    public static String card(String d) {
        return switch (d) {
            case "+z" -> "south";
            case "-z" -> "north";
            case "+x" -> "east";
            default -> "west";
        };
    }

    public static String st(String base, String up) {
        return StructBp.stair(base, up, false);
    }

    public static String sti(String base, String up) {
        return StructBp.stair(base, up, true);
    }

    public static String slb(String base) {
        return base + "[type=bottom]";
    }

    public static String slt(String base) {
        return base + "[type=top]";
    }

    public static String log(String base, char axis) {
        return base + "[axis=" + axis + "]";
    }

    // ------------------------------------------------------------------ ruido suave (para desgaste, ruinas, musgo en manchas)

    public static double hash01(long seed, int x, int z) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 11) / (double) (1L << 53);
    }

    public static double vnoise(long seed, double x, double z) {
        int xi = (int) Math.floor(x);
        int zi = (int) Math.floor(z);
        double fx = x - xi;
        double fz = z - zi;
        fx = fx * fx * (3 - 2 * fx);
        fz = fz * fz * (3 - 2 * fz);
        double a = hash01(seed, xi, zi);
        double b = hash01(seed, xi + 1, zi);
        double c = hash01(seed, xi, zi + 1);
        double d = hash01(seed, xi + 1, zi + 1);
        return a + (b - a) * fx + (c - a) * fz + (a - b - c + d) * fx * fz;
    }

    /** Ruido de 0 a 1 con varias escalas; {@code scale} = tamaño de las manchas en bloques. */
    public static double fbm(long seed, double x, double z, double scale) {
        double s = 1.0 / scale;
        return vnoise(seed, x * s, z * s) * 0.6 + vnoise(seed + 7, x * s * 2.1, z * s * 2.1) * 0.3 + vnoise(seed + 13, x * s * 4.3, z * s * 4.3) * 0.1;
    }

    // ------------------------------------------------------------------ desgaste en manchas (no al azar suelto)

    /** Cambia el prefijo del nombre de los bloques por otro (p. ej. stone_brick -> mossy_stone_brick) donde el ruido supere el umbral. */
    public static void weather(StructBp b, long seed, String from, String to, double scale, double thresh) {
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            String v = e.getValue();
            if (!v.startsWith(from)) {
                continue;
            }
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            // el ruido también varía un poco con la altura (más musgo abajo)
            double n = fbm(seed, x + y * 0.7, z - y * 0.4, scale);
            if (n > thresh) {
                e.setValue(to + v.substring(from.length()));
            }
        }
    }

    // ------------------------------------------------------------------ tejados

    /**
     * Tejado a cuatro aguas sobre el rectángulo de muros [x0..x1]x[z0..z1], empezando en la altura y.
     * over = voladizo. fillMode: 0 solo cáscara de escaleras, 1 cáscara doble (una fila de relleno por dentro), 2 macizo.
     */
    public static void hipRoof(StructBp b, int x0, int z0, int x1, int z1, int y, int over, String stair, String fill, String ridge, int fillMode) {
        int rx0 = x0 - over;
        int rx1 = x1 + over;
        int rz0 = z0 - over;
        int rz1 = z1 + over;
        int i = 0;
        while (rx0 <= rx1 && rz0 <= rz1) {
            int yy = y + i;
            if (rx1 - rx0 == 0 || rz1 - rz0 == 0) {
                for (int x = rx0; x <= rx1; x++) {
                    for (int z = rz0; z <= rz1; z++) {
                        b.set(x, yy, z, ridge != null ? ridge : fill);
                    }
                }
                break;
            }
            for (int x = rx0; x <= rx1; x++) {
                for (int z = rz0; z <= rz1; z++) {
                    boolean edge = x == rx0 || x == rx1 || z == rz0 || z == rz1;
                    if (edge) {
                        String up = z == rz0 ? "+z" : z == rz1 ? "-z" : x == rx0 ? "+x" : "-x";
                        b.set(x, yy, z, st(stair, up));
                    } else if (fillMode == 2) {
                        b.set(x, yy, z, fill);
                    } else if (fillMode == 1 && (x == rx0 + 1 || x == rx1 - 1 || z == rz0 + 1 || z == rz1 - 1)) {
                        b.set(x, yy, z, fill);
                    }
                }
            }
            rx0++;
            rx1--;
            rz0++;
            rz1--;
            i++;
        }
    }

    /** Tejado a dos aguas con la cumbrera a lo largo de Z (se ve de frente como un triángulo). Muros en [x0..x1]x[z0..z1]. */
    public static void gableZ(StructBp b, int x0, int x1, int z0, int z1, int y, int overX, int overZ, String stair, String fill, String ridge, boolean closeEnds, int fillMode) {
        int xl = x0 - overX;
        int xr = x1 + overX;
        int i = 0;
        while (xl <= xr) {
            int yy = y + i;
            for (int z = z0 - overZ; z <= z1 + overZ; z++) {
                if (xl == xr) {
                    b.set(xl, yy, z, ridge != null ? ridge : fill);
                } else {
                    b.set(xl, yy, z, st(stair, "+x"));
                    b.set(xr, yy, z, st(stair, "-x"));
                    if (fillMode == 2) {
                        for (int x = xl + 1; x < xr; x++) {
                            b.set(x, yy, z, fill);
                        }
                    } else if (fillMode == 1) {
                        b.set(xl + 1, yy, z, fill);
                        b.set(xr - 1, yy, z, fill);
                    }
                }
            }
            if (closeEnds) {
                for (int x = xl + 1; x < xr; x++) {
                    b.set(x, yy, z0, fill);
                    b.set(x, yy, z1, fill);
                }
            }
            xl++;
            xr--;
            i++;
        }
    }

    /** Igual pero con la cumbrera a lo largo de X. */
    public static void gableX(StructBp b, int x0, int x1, int z0, int z1, int y, int overX, int overZ, String stair, String fill, String ridge, boolean closeEnds, int fillMode) {
        int zl = z0 - overZ;
        int zr = z1 + overZ;
        int i = 0;
        while (zl <= zr) {
            int yy = y + i;
            for (int x = x0 - overX; x <= x1 + overX; x++) {
                if (zl == zr) {
                    b.set(x, yy, zl, ridge != null ? ridge : fill);
                } else {
                    b.set(x, yy, zl, st(stair, "+z"));
                    b.set(x, yy, zr, st(stair, "-z"));
                    if (fillMode == 2) {
                        for (int z = zl + 1; z < zr; z++) {
                            b.set(x, yy, z, fill);
                        }
                    } else if (fillMode == 1) {
                        b.set(x, yy, zl + 1, fill);
                        b.set(x, yy, zr - 1, fill);
                    }
                }
            }
            if (closeEnds) {
                for (int z = zl + 1; z < zr; z++) {
                    b.set(x0, yy, z, fill);
                    b.set(x1, yy, z, fill);
                }
            }
            zl++;
            zr--;
            i++;
        }
    }

    /** Anillo de escaleras con el lado alto hacia dentro, sobre el perímetro del rectángulo (cornisas, vuelos, bordes de techo). */
    public static void ring(StructBp b, int x0, int z0, int x1, int z1, int y, String stair, boolean top) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x == x0 || x == x1 || z == z0 || z == z1) {
                    String up = z == z0 ? "+z" : z == z1 ? "-z" : x == x0 ? "+x" : "-x";
                    b.set(x, y, z, StructBp.stair(stair, up, top));
                }
            }
        }
    }

    /** Tejado cónico para torres redondas: capas que se estrechan hasta una punta. */
    public static void cone(StructBp b, int cx, int cz, double r0, int y0, int h, String block, String slab, String tip) {
        for (int i = 0; i < h; i++) {
            double rr = r0 * Math.pow(1.0 - (double) i / h, 1.1);
            if (rr < 0.7) {
                b.set(cx, y0 + i, cz, tip != null && i >= h - 2 ? tip : block);
                continue;
            }
            disc(b, cx, cz, rr, y0 + i, block);
        }
    }

    private static void disc(StructBp b, int cx, int cz, double rr, int y, String block) {
        int ri = (int) Math.ceil(rr);
        for (int x = -ri; x <= ri; x++) {
            for (int z = -ri; z <= ri; z++) {
                if (x * x + z * z <= rr * rr) {
                    b.set(cx + x, y, cz + z, block);
                }
            }
        }
    }

    // ------------------------------------------------------------------ almenas y bordes

    /** Almenas: cada segundo bloque del perímetro sube {@code h} bloques. */
    public static void crenel(StructBp b, int x0, int z0, int x1, int z1, int y, int h, String block) {
        int idx = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                if (x == x0 || x == x1 || z == z0 || z == z1) {
                    int pos = (x == x0 || x == x1) ? z : x;
                    if (((pos - (x == x0 || x == x1 ? z0 : x0)) & 1) == 0) {
                        for (int j = 0; j < h; j++) {
                            b.set(x, y + j, z, block);
                        }
                    }
                    idx++;
                }
            }
        }
    }

    // ------------------------------------------------------------------ huecos con forma: puertas, ventanas, arcos

    private static boolean inOpening(int u, int yRel, int hw, int h, int kind) {
        if (Math.abs(u) > hw || yRel < 0 || yRel >= h) {
            return false;
        }
        if (kind == 0) {
            return true;
        }
        if (kind == 1) {
            int spring = h - hw - 1;
            if (yRel <= spring) {
                return true;
            }
            double dy = yRel - spring;
            return u * u + dy * dy <= (hw + 0.5) * (hw + 0.5);
        }
        int rise = (int) Math.round(hw * 1.7);
        int spring = Math.max(0, h - 1 - rise);
        if (yRel <= spring) {
            return true;
        }
        double dy = yRel - spring;
        double a = Math.abs(u) + hw;
        return a * a + dy * dy <= (2 * hw + 0.4) * (2 * hw + 0.4);
    }

    /**
     * Abre un hueco en un muro que se extiende en el plano X–Y (grosor de z0 a z1), centrado en cx, desde la altura y0.
     * kind: 0 rectangular, 1 medio punto, 2 apuntado (gótico). frame: bloque que enmarca el borde (o null).
     * glass: si no es null, el hueco se cierra con ese cristal en la capa del medio, con parteluz y travesaño si es ancho.
     */
    public static void openZ(StructBp b, int cx, int y0, int z0, int z1, int hw, int h, int kind, String frame, String glass) {
        int zm = (z0 + z1) / 2;
        int spring = kind == 1 ? h - hw - 1 : kind == 2 ? Math.max(0, h - 1 - (int) Math.round(hw * 1.7)) : h;
        for (int u = -hw - 1; u <= hw + 1; u++) {
            for (int yr = -1; yr <= h; yr++) {
                boolean in = inOpening(u, yr, hw, h, kind);
                if (in) {
                    for (int z = z0; z <= z1; z++) {
                        b.set(cx + u, y0 + yr, z, AIR);
                    }
                    if (glass != null) {
                        boolean bar = hw >= 2 && (u == 0 || yr == spring) && yr > 0 && yr < h;
                        b.set(cx + u, y0 + yr, zm, bar && frame != null ? frame : glass);
                    }
                } else if (frame != null && yr >= 0) {
                    boolean near = inOpening(u + 1, yr, hw, h, kind) || inOpening(u - 1, yr, hw, h, kind) || inOpening(u, yr + 1, hw, h, kind) || inOpening(u, yr - 1, hw, h, kind);
                    if (near) {
                        for (int z = z0; z <= z1; z++) {
                            if (b.solid(cx + u, y0 + yr, z)) {
                                b.set(cx + u, y0 + yr, z, frame);
                            }
                        }
                    }
                }
            }
        }
    }

    /** Igual que {@link #openZ} pero para un muro que se extiende en el plano Z–Y (grosor de x0 a x1), centrado en cz. */
    public static void openX(StructBp b, int cz, int y0, int x0, int x1, int hw, int h, int kind, String frame, String glass) {
        int xm = (x0 + x1) / 2;
        int spring = kind == 1 ? h - hw - 1 : kind == 2 ? Math.max(0, h - 1 - (int) Math.round(hw * 1.7)) : h;
        for (int u = -hw - 1; u <= hw + 1; u++) {
            for (int yr = -1; yr <= h; yr++) {
                boolean in = inOpening(u, yr, hw, h, kind);
                if (in) {
                    for (int x = x0; x <= x1; x++) {
                        b.set(x, y0 + yr, cz + u, AIR);
                    }
                    if (glass != null) {
                        boolean bar = hw >= 2 && (u == 0 || yr == spring) && yr > 0 && yr < h;
                        b.set(xm, y0 + yr, cz + u, bar && frame != null ? frame : glass);
                    }
                } else if (frame != null && yr >= 0) {
                    boolean near = inOpening(u + 1, yr, hw, h, kind) || inOpening(u - 1, yr, hw, h, kind) || inOpening(u, yr + 1, hw, h, kind) || inOpening(u, yr - 1, hw, h, kind);
                    if (near) {
                        for (int x = x0; x <= x1; x++) {
                            if (b.solid(x, y0 + yr, cz + u)) {
                                b.set(x, y0 + yr, cz + u, frame);
                            }
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ columnas y contrafuertes

    /** Base o capitel de 3x3 con escaleras (top=true: boca abajo, para el capitel). */
    public static void collar(StructBp b, int x, int y, int z, String stair, String center, boolean top) {
        b.set(x, y, z, center);
        b.set(x - 1, y, z, StructBp.stair(stair, "+x", top));
        b.set(x + 1, y, z, StructBp.stair(stair, "-x", top));
        b.set(x, y, z - 1, StructBp.stair(stair, "+z", top));
        b.set(x, y, z + 1, StructBp.stair(stair, "-z", top));
        b.set(x - 1, y, z - 1, StructBp.stair(stair, "+z", top));
        b.set(x + 1, y, z - 1, StructBp.stair(stair, "+z", top));
        b.set(x - 1, y, z + 1, StructBp.stair(stair, "-z", top));
        b.set(x + 1, y, z + 1, StructBp.stair(stair, "-z", top));
    }

    /** Columna con base y capitel: el fuste va de y0 a y1 (sin contar base y capitel, que usan y0 e y1). */
    public static void column(StructBp b, int x, int z, int y0, int y1, String shaft, String stair, String collarCenter) {
        for (int y = y0; y <= y1; y++) {
            b.set(x, y, z, shaft);
        }
        collar(b, x, y0, z, stair, collarCenter, false);
        collar(b, x, y1, z, stair, collarCenter, true);
    }

    /** Columna gruesa de 2x2 (esquina inferior x,z) con base y capitel de losas. */
    public static void column2(StructBp b, int x, int z, int y0, int y1, String shaft, String slab) {
        for (int y = y0; y <= y1; y++) {
            b.box(x, y, z, x + 1, y, z + 1, shaft);
        }
        b.box(x - 1, y0, z - 1, x + 2, y0, z + 2, slb(slab));
        b.box(x - 1, y1, z - 1, x + 2, y1, z + 2, slt(slab));
    }

    /**
     * Contrafuerte pegado a un muro: sale desde (x, zWall) hacia {@code out} ("+z" o "-z" o "+x"/"-x") y se inclina hasta pegarse al muro arriba.
     */
    public static void buttress(StructBp b, int x, int zWall, String out, int y0, int h, int depth, String stair, String fill) {
        int ox = dx(out);
        int oz = dz(out);
        for (int j = 0; j < h; j++) {
            int p = depth - (j * depth) / Math.max(1, h - 1);
            if (p < 1) {
                break;
            }
            for (int q = 1; q <= p; q++) {
                int px = x + ox * q;
                int pz = zWall + oz * q;
                if (q == p) {
                    b.set(px, y0 + j, pz, st(stair, opp(out)));
                } else {
                    b.set(px, y0 + j, pz, fill);
                }
            }
        }
    }

    // ------------------------------------------------------------------ escaleras de caracol y escaleras rectas

    /** Caracol de 3x3 alrededor de una columna central: cada paso sube 1. Devuelve la altura final (y0 + pasos). */
    public static int spiral(StructBp b, int cx, int cz, int y0, int steps, String stair, String pillar, boolean clockwise) {
        int[][] ring = {{-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}};
        for (int i = 0; i < steps; i++) {
            int a = clockwise ? i % 8 : (8 - (i % 8)) % 8;
            int n = clockwise ? (a + 1) % 8 : (a + 7) % 8;
            int ddx = ring[n][0] - ring[a][0];
            int ddz = ring[n][1] - ring[a][1];
            String up = ddx > 0 ? "+x" : ddx < 0 ? "-x" : ddz > 0 ? "+z" : "-z";
            b.set(cx + ring[a][0], y0 + i, cz + ring[a][1], st(stair, up));
        }
        for (int i = 0; i < steps + 2; i++) {
            b.set(cx, y0 + i, cz, pillar);
        }
        return y0 + steps;
    }

    /** Escalera recta que sube hacia {@code up} desde (x, y0, z), ancho w (en el eje perpendicular), n escalones. Rellena por debajo si fillBelow. */
    public static void stairway(StructBp b, int x, int y0, int z, String up, int w, int n, String stair, String fill, boolean fillBelow) {
        int ux = dx(up);
        int uz = dz(up);
        for (int i = 0; i < n; i++) {
            for (int s = 0; s < w; s++) {
                int px = x + (uz != 0 ? s : 0) + ux * i;
                int pz = z + (ux != 0 ? s : 0) + uz * i;
                b.set(px, y0 + i, pz, st(stair, up));
                if (fillBelow) {
                    for (int y = y0 + i - 1; y >= y0 - 1 && y >= -64; y--) {
                        if (y < y0) {
                            break;
                        }
                        b.set(px, y, pz, fill);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ luz y colgantes

    public static void wallTorch(StructBp b, int x, int y, int z, String away) {
        b.set(x, y, z, "wall_torch[facing=" + card(away) + "]");
    }

    public static void soulTorch(StructBp b, int x, int y, int z, String away) {
        b.set(x, y, z, "soul_wall_torch[facing=" + card(away) + "]");
    }

    /** Lámpara colgada del techo con cadena: la cadena va de yTop hacia abajo drop bloques y luego la linterna. */
    public static void hangLantern(StructBp b, int x, int yTop, int z, int drop, String lantern) {
        for (int i = 0; i < drop; i++) {
            b.set(x, yTop - i, z, "chain[axis=y]");
        }
        b.set(x, yTop - drop, z, lantern + "[hanging=true]");
    }

    /** Araña de luces de 5 linternas: cadena desde yTop y una cruz de cadenas con linternas colgando. */
    public static void chandelier(StructBp b, int x, int yTop, int z, int drop, String lantern) {
        int yb = yTop - drop;
        for (int y = yTop; y > yb + 1; y--) {
            b.set(x, y, z, "chain[axis=y]");
        }
        b.set(x, yb + 1, z, "chain[axis=y]");
        // los brazos son barrotes: una cadena acostada no sostiene farolas (solo la vertical), unos barrotes sí
        b.set(x - 1, yb + 1, z, "iron_bars");
        b.set(x + 1, yb + 1, z, "iron_bars");
        b.set(x, yb + 1, z - 1, "iron_bars");
        b.set(x, yb + 1, z + 1, "iron_bars");
        b.set(x, yb, z, lantern + "[hanging=true]");
        b.set(x - 1, yb, z, lantern + "[hanging=true]");
        b.set(x + 1, yb, z, lantern + "[hanging=true]");
        b.set(x, yb, z - 1, lantern + "[hanging=true]");
        b.set(x, yb, z + 1, lantern + "[hanging=true]");
    }

    /** Brasero: pedestal de muro con fuego encima (campfire = humo y luz). */
    public static void brazier(StructBp b, int x, int y, int z, String wall, String fire) {
        b.set(x, y, z, wall);
        b.set(x, y + 1, z, wall);
        b.set(x, y + 2, z, fire);
    }

    public static void candles(StructBp b, int x, int y, int z, int n) {
        b.set(x, y, z, "candle[candles=" + n + ",lit=true]");
    }

    // ------------------------------------------------------------------ mobiliario

    /** Silla: la escalera tiene el lado alto (respaldo) detrás de quien se sienta. look = hacia dónde mira. */
    public static void chair(StructBp b, int x, int y, int z, String look, String stair) {
        b.set(x, y, z, st(stair, opp(look)));
    }

    /** Mesa larga: patas de valla en los extremos y tablero de losas. axis 'x' o 'z'. */
    public static void table(StructBp b, int x, int y, int z, char axis, int len, String slab, String leg) {
        for (int i = 0; i < len; i++) {
            int px = axis == 'x' ? x + i : x;
            int pz = axis == 'z' ? z + i : z;
            b.set(px, y, pz, "air");
            b.set(px, y + 1, pz, slt(slab));
            if (i == 0 || i == len - 1) {
                b.set(px, y, pz, leg);
            }
        }
    }

    /** Fila de estanterías (altura h) a lo largo de un eje. */
    public static void shelves(StructBp b, int x, int y, int z, char axis, int len, int h) {
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < h; j++) {
                int px = axis == 'x' ? x + i : x;
                int pz = axis == 'z' ? z + i : z;
                b.set(px, y + j, pz, "bookshelf");
            }
        }
    }

    public static void wallBanner(StructBp b, int x, int y, int z, String color, String away) {
        b.set(x, y, z, color + "_wall_banner[facing=" + card(away) + "]");
    }

    public static void skull(StructBp b, int x, int y, int z, int rot) {
        b.set(x, y, z, "skeleton_skull[rotation=" + (rot & 15) + "]");
    }

    public static void ladder(StructBp b, int x, int z, int y0, int y1, String away) {
        for (int y = y0; y <= y1; y++) {
            b.set(x, y, z, "ladder[facing=" + card(away) + "]");
        }
    }

    /** Puerta de hierro de 2 de alto (o barrotes si la abierta). */
    public static void ironDoor(StructBp b, int x, int y, int z, String facing, boolean open) {
        b.set(x, y, z, "iron_door[facing=" + card(facing) + ",half=lower,hinge=left,open=" + open + "]");
        b.set(x, y + 1, z, "iron_door[facing=" + card(facing) + ",half=upper,hinge=left,open=" + open + "]");
    }

    public static void woodDoor(StructBp b, String wood, int x, int y, int z, String facing, boolean open) {
        b.set(x, y, z, wood + "_door[facing=" + card(facing) + ",half=lower,hinge=left,open=" + open + "]");
        b.set(x, y + 1, z, wood + "_door[facing=" + card(facing) + ",half=upper,hinge=left,open=" + open + "]");
    }

    /** Estatua de caballero de pie mirando a {@code look}: pedestal, piernas, torso, brazos, cabeza y lanza. */
    public static void knight(StructBp b, int x, int y, int z, String look, String block, String stair, String wall, String head, String torso) {
        collar(b, x, y, z, stair, block, false);
        b.set(x, y + 1, z, wall);
        b.set(x, y + 2, z, wall);
        b.set(x, y + 3, z, torso);
        b.set(x, y + 4, z, torso);
        int lx = dx(look);
        int lz = dz(look);
        // brazos a los lados
        int sx = lz != 0 ? 1 : 0;
        int sz = lx != 0 ? 1 : 0;
        b.set(x + sx, y + 4, z + sz, wall);
        b.set(x - sx, y + 4, z - sz, wall);
        b.set(x + sx, y + 3, z + sz, wall);
        b.set(x - sx, y + 3, z - sz, wall);
        b.set(x, y + 5, z, head);
        b.set(x, y + 6, z, st(stair, look).replace("half=bottom", "half=bottom"));
        // lanza en una mano
        for (int j = 0; j < 6; j++) {
            b.set(x + sx * 2, y + 2 + j, z + sz * 2, "iron_bars");
        }
        b.set(x + sx * 2, y + 8, z + sz * 2, "lightning_rod[facing=up]");
    }

    /** Sarcófago: cuerpo de bloques y tapa de losas, con cabecera en un extremo. axis 'x' o 'z'. */
    public static void sarcophagus(StructBp b, int x, int y, int z, char axis, int len, String body, String slab, String stair) {
        for (int i = 0; i < len; i++) {
            for (int w = 0; w < 2; w++) {
                int px = axis == 'x' ? x + i : x + w;
                int pz = axis == 'z' ? z + i : z + w;
                b.set(px, y, pz, body);
                b.set(px, y + 1, pz, slb(slab));
            }
        }
        String head = axis == 'x' ? "-x" : "-z";
        for (int w = 0; w < 2; w++) {
            int px = axis == 'x' ? x : x + w;
            int pz = axis == 'z' ? z : z + w;
            b.set(px, y + 1, pz, st(stair, opp(head)));
        }
    }

    /** Pozo/estanque cuadrado con borde: agua dentro (no se desborda porque el borde la rodea). */
    public static void pool(StructBp b, int x0, int z0, int x1, int z1, int yTop, int depth, String rim, String fluid, String lining) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                for (int d = 0; d <= depth; d++) {
                    int y = yTop - d;
                    if (edge) {
                        b.set(x, y, z, rim);
                    } else if (d == depth) {
                        b.set(x, y, z, lining);
                    } else {
                        b.set(x, y, z, fluid);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ suelos

    public static void checker(StructBp b, int x0, int z0, int x1, int z1, int y, String a, String c) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                b.set(x, y, z, ((x + z) & 1) == 0 ? a : c);
            }
        }
    }

    /** Marco de un bloque de ancho a distancia inset del borde. */
    public static void borderLine(StructBp b, int x0, int z0, int x1, int z1, int y, int inset, String mat) {
        for (int x = x0 + inset; x <= x1 - inset; x++) {
            b.set(x, y, z0 + inset, mat);
            b.set(x, y, z1 - inset, mat);
        }
        for (int z = z0 + inset; z <= z1 - inset; z++) {
            b.set(x0 + inset, y, z, mat);
            b.set(x1 - inset, y, z, mat);
        }
    }

    // ------------------------------------------------------------------ habitaciones

    /** Una sala rectangular: interior [x0..x1]x[z0..z1], suelo en fy, aire de fy+1 a fy+h, techo en fy+h+1. */
    public static final class Room {
        public final StructBp b;
        public final int x0;
        public final int x1;
        public final int z0;
        public final int z1;
        public final int fy;
        public final int h;

        public Room(StructBp b, int x0, int z0, int x1, int z1, int fy, int h) {
            this.b = b;
            this.x0 = Math.min(x0, x1);
            this.x1 = Math.max(x0, x1);
            this.z0 = Math.min(z0, z1);
            this.z1 = Math.max(z0, z1);
            this.fy = fy;
            this.h = h;
        }

        public int cx() {
            return (x0 + x1) / 2;
        }

        public int cz() {
            return (z0 + z1) / 2;
        }

        public int top() {
            return fy + h;
        }

        /** Caja maciza con muros de grosor 1 (y suelo y techo), interior vacío. */
        public Room shell(String wall, String floor, String ceil) {
            b.box(x0 - 1, fy, z0 - 1, x1 + 1, fy + h + 1, z1 + 1, wall);
            b.air(x0, fy + 1, z0, x1, fy + h, z1);
            b.box(x0, fy, z0, x1, fy, z1, floor);
            b.box(x0, fy + h + 1, z0, x1, fy + h + 1, z1, ceil);
            return this;
        }

        /** Solo vacía el interior (para cavar dentro de una masa ya hecha). */
        public Room carve() {
            b.air(x0, fy + 1, z0, x1, fy + h, z1);
            return this;
        }

        public Room floor(String m) {
            b.box(x0, fy, z0, x1, fy, z1, m);
            return this;
        }

        /** Rodapié: escaleras al pie de las paredes con el lado alto contra el muro. */
        public Room baseboard(String stair) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (x == x0 || x == x1 || z == z0 || z == z1) {
                        String up = x == x0 ? "-x" : x == x1 ? "+x" : z == z0 ? "-z" : "+z";
                        b.setIfAbsent(x, fy + 1, z, st(stair, up));
                    }
                }
            }
            return this;
        }

        /** Antorchas (o de alma) pegadas a las paredes, cada {@code gap} bloques, a la altura y. */
        public Room torches(int gap, int y, boolean soul) {
            for (int x = x0 + gap / 2; x <= x1; x += gap) {
                if (soul) {
                    soulTorch(b, x, y, z0, "+z");
                    soulTorch(b, x, y, z1, "-z");
                } else {
                    wallTorch(b, x, y, z0, "+z");
                    wallTorch(b, x, y, z1, "-z");
                }
            }
            for (int z = z0 + gap / 2; z <= z1; z += gap) {
                if (soul) {
                    soulTorch(b, x0, y, z, "+x");
                    soulTorch(b, x1, y, z, "-x");
                } else {
                    wallTorch(b, x0, y, z, "+x");
                    wallTorch(b, x1, y, z, "-x");
                }
            }
            return this;
        }

        /** Cornisa bajo el techo: escaleras boca abajo contra los muros. */
        public Room cornice(String stair) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    if (x == x0 || x == x1 || z == z0 || z == z1) {
                        String up = x == x0 ? "-x" : x == x1 ? "+x" : z == z0 ? "-z" : "+z";
                        b.setIfAbsent(x, fy + h, z, sti(stair, up));
                    }
                }
            }
            return this;
        }
    }

    // ------------------------------------------------------------------ plantas y árboles

    /** Árbol grande con tronco de 2x2, raíces y copa de bolas de hojas. */
    public static void bigTree(StructBp b, int x, int z, int h, String logName, String[] leaves, int crown) {
        for (int y = -1; y < h; y++) {
            for (int a = 0; a < 2; a++) {
                for (int c = 0; c < 2; c++) {
                    b.set(x + a, y, z + c, log(logName, 'y'));
                }
            }
        }
        for (int[] d : new int[][]{{-1, 0}, {2, 1}, {0, -1}, {1, 2}, {-1, 1}, {2, 0}}) {
            int top = 2 + b.r.nextInt(2);
            for (int y = -1; y < top; y++) {
                b.set(x + d[0], y, z + d[1], log(logName, 'y'));
            }
        }
        int[][] blobs = {{0, 0, 0, crown}, {-crown * 5 / 8, -1, crown * 3 / 8, crown * 3 / 4}, {crown * 5 / 8, -2, crown / 4, crown * 3 / 4}, {crown / 4, -3, -crown * 5 / 8, crown * 3 / 4}, {-crown / 4, -2, crown * 3 / 4, crown * 5 / 8}, {0, crown / 2, 0, crown * 5 / 8}};
        for (int[] c : blobs) {
            int bx = x + 1 + c[0];
            int by = h - 2 + c[1];
            int bz = z + 1 + c[2];
            int rr = Math.max(2, c[3]);
            for (int dx = -rr; dx <= rr; dx++) {
                for (int dy = -rr / 2 - 1; dy <= rr / 2; dy++) {
                    for (int dz = -rr; dz <= rr; dz++) {
                        double d = dx * dx + dy * dy * 2.2 + dz * dz;
                        if (d <= rr * rr && b.chance(0.93)) {
                            b.setIfAbsent(bx + dx, by + dy, bz + dz, b.pick(leaves));
                        }
                    }
                }
            }
        }
    }

    /** Abeto: tronco y capas de hojas en forma de cono. */
    public static void spruce(StructBp b, int x, int z, int h, String logName, String leaf) {
        for (int y = -1; y < h; y++) {
            b.set(x, y, z, log(logName, 'y'));
        }
        int layers = h - 2;
        for (int i = 0; i < layers; i++) {
            int y = h - 1 - i;
            double rr = 0.6 + (i / (double) layers) * (h * 0.32);
            if (i % 2 == 1) {
                rr -= 1;
            }
            int ri = (int) Math.ceil(rr);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    if (dx * dx + dz * dz <= rr * rr + 0.3) {
                        b.setIfAbsent(x + dx, y, z + dz, leaf);
                    }
                }
            }
        }
        b.setIfAbsent(x, h, z, leaf);
    }

    /** Cuelga enredaderas y hojas de los bordes inferiores de bloques cuyo nombre contenga {@code filter}. */
    public static void hangVines(StructBp b, String filter, int minY, double prob, int maxLen) {
        List<int[]> hang = new ArrayList<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            if (y < minY || !e.getValue().contains(filter) || e.getValue().contains("stairs") || e.getValue().contains("slab")) {
                continue;
            }
            if (!b.m.containsKey(StructBp.key(x, y - 1, z)) && b.r.nextDouble() < prob) {
                hang.add(new int[]{x, y - 1, z});
            }
        }
        for (int[] h : hang) {
            int len = 1 + b.r.nextInt(maxLen);
            for (int i = 0; i < len; i++) {
                b.setIfAbsent(h[0], h[1] - i, h[2], i == len - 1 ? "vine[up=true]" : "vine[up=true]");
            }
        }
    }

    /** Césped, helechos y musgo en el suelo de una zona (y = nivel del suelo; la planta va en y+1). */
    public static void groundCover(StructBp b, int x0, int z0, int x1, int z1, int y, double prob, String... plants) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                if (b.chance(prob)) {
                    b.setIfAbsent(x, y + 1, z, b.pick(plants));
                }
            }
        }
    }

    /** Escombros sueltos en una zona: bloques, losas y escaleras caídos. */
    public static void rubble(StructBp b, int x0, int z0, int x1, int z1, int y, double prob, String... pal) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                if (b.chance(prob)) {
                    String s = b.pick(pal);
                    b.setIfAbsent(x, y + 1, z, s);
                    if (b.chance(0.18)) {
                        b.setIfAbsent(x, y + 2, z, b.pick(pal));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ ruinas con cortes naturales

    /**
     * Rompe la parte alta de todo lo que haya en la zona con un borde irregular suave: en cada columna se quita lo que esté
     * por encima de {@code base + ruido * amp}. Los cortes forman pendientes naturales, no dientes de sierra.
     */
    public static void ruinCut(StructBp b, int x0, int z0, int x1, int z1, int yMin, int base, int amp, long seed, double scale) {
        List<Long> del = new ArrayList<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            if (x < x0 || x > x1 || z < z0 || z > z1 || y <= yMin) {
                continue;
            }
            double n = fbm(seed, x, z, scale);
            int cut = base + (int) Math.round((n - 0.35) * amp * 1.8);
            if (y > cut) {
                del.add(k);
            }
        }
        for (long k : del) {
            b.m.remove(k);
        }
    }


    /**
     * Ruina natural: en cada columna se conserva una fracción de su altura según un ruido suave (más el sesgo de la zona),
     * así los muros se rompen en pendientes y no en dientes de sierra. Solo toca lo que esté a partir de minY.
     * En lo alto de cada corte puede quedar un escombro. bias(x,z) suma o resta al ruido (positivo = más conservado).
     */
    public static void ruinColumns(StructBp b, long seed, double scale, int minY, java.util.function.BiFunction<Integer, Integer, Double> bias, double rubbleChance, String... rubble) {
        java.util.Map<Long, Integer> tops = new java.util.HashMap<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            int y = StructBp.ky(k);
            if (y < minY || e.getValue().equals("air")) {
                continue;
            }
            tops.merge(StructBp.key(StructBp.kx(k), 0, StructBp.kz(k)), y, Math::max);
        }
        for (Map.Entry<Long, Integer> t : tops.entrySet()) {
            int x = StructBp.kx(t.getKey());
            int z = StructBp.kz(t.getKey());
            int top = t.getValue();
            double n = fbm(seed, x, z, scale) + bias.apply(x, z);
            double f = Math.max(0, Math.min(1, (n - 0.30) / 0.45));
            double keep = 0.18 + 0.90 * f;
            int cut = minY + (int) ((top - minY) * keep);
            if (cut >= top) {
                continue;
            }
            for (int y = cut + 1; y <= top; y++) {
                b.m.remove(StructBp.key(x, y, z));
            }
            if (rubble.length > 0 && b.chance(rubbleChance)) {
                b.set(x, cut + 1, z, b.pick(rubble));
            }
        }
    }

    /** Cobertura de nieve: capas de nieve sobre los bloques sólidos expuestos (y de nieve arriba) en la zona. */
    public static void snowCover(StructBp b, long seed, double prob, int minY) {
        List<int[]> add = new ArrayList<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            String v = e.getValue();
            if (y < minY || v.equals("air") || v.contains("glass") || v.contains("stairs") || v.contains("slab") || v.contains("wall") || v.contains("lantern") || v.contains("chain") || v.contains("bars") || v.contains("rod") || v.contains("torch") || v.contains("campfire") || v.contains("carpet") || v.contains("snow") || v.contains("ice") && v.contains("[")) {
                continue;
            }
            if (b.m.containsKey(StructBp.key(x, y + 1, z))) {
                continue;
            }
            if (hash01(seed, x * 7 + y, z * 13 + y) < prob) {
                add.add(new int[]{x, y + 1, z});
            }
        }
        for (int[] a : add) {
            b.setIfAbsent(a[0], a[1], a[2], "snow[layers=" + (1 + (int) (hash01(seed + 5, a[0], a[2]) * 3)) + "]");
        }
    }

    /** Carámbanos colgando de los bordes inferiores de bloques de hielo/nieve. */
    public static void icicles(StructBp b, String filter, int minY, double prob, int maxLen, String icicle) {
        List<int[]> hang = new ArrayList<>();
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            long k = e.getKey();
            int x = StructBp.kx(k);
            int y = StructBp.ky(k);
            int z = StructBp.kz(k);
            if (y < minY || !e.getValue().contains(filter)) {
                continue;
            }
            if (!b.m.containsKey(StructBp.key(x, y - 1, z)) && b.r.nextDouble() < prob) {
                hang.add(new int[]{x, y - 1, z});
            }
        }
        for (int[] h : hang) {
            int len = 1 + b.r.nextInt(maxLen);
            for (int i = 0; i < len; i++) {
                b.setIfAbsent(h[0], h[1] - i, h[2], icicle);
            }
        }
    }
}
