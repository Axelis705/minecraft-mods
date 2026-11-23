package com.coleccionista.mapa;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Un plano del mapa del evento: qué bloque va en cada lugar (como texto, por ejemplo {@code oak_stairs[facing=north]}) y las marcas con
 * nombre que usa el evento (dónde aparece cada jugador, dónde está su pantalla, su puerta...). No usa nada de Minecraft: así se puede
 * dibujar y revisar fuera del juego ({@code herramientas/Volcar.java}).
 */
public final class Plano {
    /** Una marca: un punto con hacia dónde mira (0 = sur, 90 = oeste, 180 = norte, 270 = este, como Minecraft). */
    public record Marca(int x, int y, int z, float yaw) {
    }

    public final Map<Long, String> m = new LinkedHashMap<>();
    /** Datos extra de un bloque (letreros, cofres): SNBT. */
    public final Map<Long, String> nbt = new LinkedHashMap<>();
    public final Map<String, Marca> marcas = new LinkedHashMap<>();
    public final Random r;
    /** Medidas (para limpiar antes de construir): mínimo y máximo. */
    public int x0 = Integer.MAX_VALUE, y0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, y1 = Integer.MIN_VALUE,
            z1 = Integer.MIN_VALUE;

    public Plano(long semilla) {
        this.r = new Random(semilla);
    }

    public static long key(int x, int y, int z) {
        return ((long) (x + 2048) << 40) | ((long) (y + 2048) << 20) | (z + 2048);
    }

    public static int kx(long k) {
        return (int) (k >>> 40) - 2048;
    }

    public static int ky(long k) {
        return (int) ((k >>> 20) & 0xFFFFF) - 2048;
    }

    public static int kz(long k) {
        return (int) (k & 0xFFFFF) - 2048;
    }

    public void set(int x, int y, int z, String s) {
        m.put(key(x, y, z), s);
        x0 = Math.min(x0, x);
        y0 = Math.min(y0, y);
        z0 = Math.min(z0, z);
        x1 = Math.max(x1, x);
        y1 = Math.max(y1, y);
        z1 = Math.max(z1, z);
    }

    public String get(int x, int y, int z) {
        return m.get(key(x, y, z));
    }

    public boolean vacio(int x, int y, int z) {
        String s = get(x, y, z);
        return s == null || s.equals("air");
    }

    /** Llena una caja (incluye las dos esquinas). */
    public void caja(int ax, int ay, int az, int bx, int by, int bz, String s) {
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
            for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++) {
                for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
                    set(x, y, z, s);
                }
            }
        }
    }

    /** Llena una caja eligiendo al azar entre varios bloques (para paredes con textura). */
    public void cajaMezcla(int ax, int ay, int az, int bx, int by, int bz, String... s) {
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++) {
            for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++) {
                for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) {
                    set(x, y, z, s[r.nextInt(s.length)]);
                }
            }
        }
    }

    /** Pone el bloque solo si ahí todavía no hay nada. */
    public void siVacio(int x, int y, int z, String s) {
        if (get(x, y, z) == null) {
            set(x, y, z, s);
        }
    }

    public void nbt(int x, int y, int z, String snbt) {
        nbt.put(key(x, y, z), snbt);
    }

    public void marca(String nombre, int x, int y, int z, float yaw) {
        marcas.put(nombre, new Marca(x, y, z, yaw));
    }

    public String elegir(String... o) {
        return o[r.nextInt(o.length)];
    }

    public boolean azar(double p) {
        return r.nextDouble() < p;
    }

    /**
     * Un bloque con texto del mod ({@code coleccionista:placa}, {@code letrero} o {@code rayon}) mirando a {@code facing}, con su color
     * (RGB) y hasta 4 líneas.
     */
    public void texto(int x, int y, int z, String bloque, String facing, int color, String... lineas) {
        set(x, y, z, "coleccionista:" + bloque + "[facing=" + facing + "]");
        StringBuilder sb = new StringBuilder("{color:" + color + ",lineas:[");
        for (int i = 0; i < lineas.length; i++) {
            sb.append(i > 0 ? "," : "").append('"').append(lineas[i].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        sb.append("]}");
        nbt(x, y, z, sb.toString());
    }

    /** Un letrero de pared con texto (hasta 4 líneas). */
    public void letrero(int x, int y, int z, String bloque, String facing, String color, String... lineas) {
        set(x, y, z, bloque + "[facing=" + facing + "]");
        StringBuilder sb = new StringBuilder("{front_text:{color:\"" + color + "\",has_glowing_text:1b,messages:[");
        for (int i = 0; i < 4; i++) {
            String l = i < lineas.length ? lineas[i].replace("\\", "\\\\").replace("\"", "\\\"") : "";
            sb.append(i > 0 ? "," : "").append("'{\"text\":\"").append(l.replace("'", "\\'")).append("\"}'");
        }
        sb.append("]}}");
        nbt(x, y, z, sb.toString());
    }
}
