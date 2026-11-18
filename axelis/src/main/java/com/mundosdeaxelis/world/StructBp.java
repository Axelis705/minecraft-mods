package com.mundosdeaxelis.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Plano de una estructura en coordenadas locales, sin nada de Minecraft (solo texto): así se puede dibujar y revisar fuera del juego.
 * x = a lo ancho (0 = centro), y = hacia arriba (0 = nivel del suelo), z = hacia dentro (0 = el frente, por donde se entra).
 * Cada bloque es un texto como {@code stone_bricks} o {@code oak_stairs[facing=north,half=bottom]} (se añade {@code minecraft:} si falta).
 */
public final class StructBp {
    public record Found(int x0, int z0, int x1, int z1, String state) {
    }

    public final Map<Long, String> m = new LinkedHashMap<>();
    public final List<Found> found = new ArrayList<>();
    public final Random r;
    /** Salas planificadas (nombre -> cajas x0,y0,z0,x1,y1,z1) solo para revisar el plano fuera del juego. */
    public final Map<String, List<int[]>> rooms = new LinkedHashMap<>();
    /** Lo que {@link Soporte#prune} quitó por no poder sostenerse (tipo -> cantidad); vacío si el diseño no tenía nada así. */
    public Map<String, Integer> unsupported = new java.util.TreeMap<>();

    public StructBp(long seed) {
        r = new Random(seed);
    }

    public static long key(int x, int y, int z) {
        return ((long) (x + 4096) << 40) | ((long) (y + 512) << 24) | (long) (z + 4096);
    }

    public static int kx(long k) {
        return (int) (k >> 40) - 4096;
    }

    public static int ky(long k) {
        return (int) ((k >> 24) & 0xFFFF) - 512;
    }

    public static int kz(long k) {
        return (int) (k & 0xFFFFFF) - 4096;
    }

    // ------------------------------------------------------------------ básicos

    public void set(int x, int y, int z, String s) {
        if (s != null) {
            m.put(key(x, y, z), s);
        }
    }

    public void setIfAbsent(int x, int y, int z, String s) {
        if (s != null) {
            m.putIfAbsent(key(x, y, z), s);
        }
    }

    public String get(int x, int y, int z) {
        return m.get(key(x, y, z));
    }

    public boolean solid(int x, int y, int z) {
        String s = m.get(key(x, y, z));
        return s != null && !s.equals("air");
    }

    public String pick(String... o) {
        return o[r.nextInt(o.length)];
    }

    public boolean chance(double p) {
        return r.nextDouble() < p;
    }

    public void foundation(int x0, int z0, int x1, int z1, String s) {
        found.add(new Found(Math.min(x0, x1), Math.min(z0, z1), Math.max(x0, x1), Math.max(z0, z1), s));
    }

    // ------------------------------------------------------------------ formas

    public void box(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    public void mix(int x0, int y0, int z0, int x1, int y1, int z1, String... opts) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, pick(opts));
                }
            }
        }
    }

    public void air(int x0, int y0, int z0, int x1, int y1, int z1) {
        box(x0, y0, z0, x1, y1, z1, "air");
    }

    /** Cuatro paredes (sin suelo ni techo). */
    public void walls(int x0, int y0, int z0, int x1, int y1, int z1, String... opts) {
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int x = ax; x <= bx; x++) {
                set(x, y, az, pick(opts));
                set(x, y, bz, pick(opts));
            }
            for (int z = az; z <= bz; z++) {
                set(ax, y, z, pick(opts));
                set(bx, y, z, pick(opts));
            }
        }
    }

    /** Caja hueca: paredes, suelo y techo. El interior se vacía. */
    public void shell(int x0, int y0, int z0, int x1, int y1, int z1, String... opts) {
        box(x0, y0, z0, x1, y1, z1, "air");
        walls(x0, y0, z0, x1, y1, z1, opts);
        int ax = Math.min(x0, x1), bx = Math.max(x0, x1), az = Math.min(z0, z1), bz = Math.max(z0, z1);
        for (int x = ax; x <= bx; x++) {
            for (int z = az; z <= bz; z++) {
                set(x, Math.min(y0, y1), z, pick(opts));
                set(x, Math.max(y0, y1), z, pick(opts));
            }
        }
    }

    /** Disco lleno de radio r (capas y0..y1). */
    public void disc(int cx, int cz, double r0, int y0, int y1, String... opts) {
        int ri = (int) Math.ceil(r0);
        for (int y = y0; y <= y1; y++) {
            for (int x = -ri; x <= ri; x++) {
                for (int z = -ri; z <= ri; z++) {
                    if (x * x + z * z <= r0 * r0) {
                        set(cx + x, y, cz + z, pick(opts));
                    }
                }
            }
        }
    }

    /** Tubo (pared circular) de radio r, espesor t. */
    public void tube(int cx, int cz, double r0, double thick, int y0, int y1, String... opts) {
        int ri = (int) Math.ceil(r0);
        double in = r0 - thick;
        for (int y = y0; y <= y1; y++) {
            for (int x = -ri; x <= ri; x++) {
                for (int z = -ri; z <= ri; z++) {
                    double d2 = x * x + z * z;
                    if (d2 <= r0 * r0 && d2 > in * in) {
                        set(cx + x, y, cz + z, pick(opts));
                    }
                }
            }
        }
    }

    /** Aguja: capas que se estrechan desde r0 hasta 0 en h bloques. */
    public void spire(int cx, int cz, int y0, double r0, int h, String... opts) {
        for (int i = 0; i < h; i++) {
            double rr = r0 * (1.0 - (double) i / h);
            if (rr < 0.6) {
                set(cx, y0 + i, cz, pick(opts));
            } else {
                disc(cx, cz, rr, y0 + i, y0 + i, opts);
            }
        }
    }

    /** Pirámide escalonada hueca por arriba: cada capa se retrae en step. */
    public void stepped(int x0, int z0, int x1, int z1, int y0, int layers, int step, String... opts) {
        for (int i = 0; i < layers; i++) {
            int a = i * step;
            if (x0 + a > x1 - a || z0 + a > z1 - a) {
                break;
            }
            box(x0 + a, y0 + i, z0 + a, x1 - a, y0 + i, z1 - a, pick(opts));
        }
    }

    /** Borra al azar los bloques de una zona (ruinas): prob entre 0 y 1. */
    public void decay(int x0, int y0, int z0, int x1, int y1, int z1, double prob) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    if (m.containsKey(key(x, y, z)) && r.nextDouble() < prob) {
                        m.remove(key(x, y, z));
                    }
                }
            }
        }
    }

    /** Cambia al azar los bloques que sean exactamente {@code from} por {@code to}. */
    public void swap(String from, String to, double prob) {
        for (Map.Entry<Long, String> e : m.entrySet()) {
            if (e.getValue().equals(from) && r.nextDouble() < prob) {
                e.setValue(to);
            }
        }
    }

    // ------------------------------------------------------------------ bloques con orientación

    /** Escalera que SUBE hacia {@code up}: "+z", "-z", "+x" o "-x" (el lado alto queda en esa dirección). */
    public static String stair(String base, String up, boolean top) {
        // en Minecraft, "facing" de una escalera apunta hacia el lado ALTO (hacia donde se sube)
        String f = switch (up) {
            case "+z" -> "south";
            case "-z" -> "north";
            case "+x" -> "east";
            default -> "west";
        };
        return base + "[facing=" + f + ",half=" + (top ? "top" : "bottom") + "]";
    }

    public static String slab(String base, boolean top) {
        return base + "[type=" + (top ? "top" : "bottom") + "]";
    }

    public static String log(String base, char axis) {
        return base + "[axis=" + axis + "]";
    }

    /** Muro de cuatro lados con orientación local (n/s/e/w) para carteles, estandartes y faroles de pared. */
    public static String facing(String base, String dir) {
        return base + "[facing=" + dir + "]";
    }
}
