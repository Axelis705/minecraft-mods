package com.coleccionista.mapa;

import java.util.Random;

/**
 * Herramientas para construir edificios sobre un {@link Plano}: cajas, paredes, huecos, tramos de escalera, barandales, puertas dobles,
 * ventanas con marco y repisa, y un espejo para hacer las cosas simétricas. No usa nada de Minecraft (se revisa sin el juego).
 * <p>
 * Los bloques van como texto ({@code "minecraft:dark_oak_planks"}, {@code "coleccionista:parquet"}...) y las direcciones como en
 * Minecraft ({@code north} = z hacia menos).
 */
public final class Trazo {
    public static final String C = "coleccionista:", M = "minecraft:";
    public final Plano p;
    public final Random r;
    /** El eje de simetría: el espejo de x es {@code espejo - x} (con un eje entre dos bloques, por ejemplo 53 para el eje en 26|27). */
    public final int espejo;

    public Trazo(Plano p, long semilla, int espejo) {
        this.p = p;
        this.r = new Random(semilla);
        this.espejo = espejo;
    }

    public int e(int x) {
        return espejo - x;
    }

    public void set(int x, int y, int z, String b) {
        p.set(x, y, z, b);
    }

    /** El mismo bloque aquí y en el espejo (con facing este y oeste cambiados, y la bisagra al revés). */
    public void par(int x, int y, int z, String b) {
        p.set(x, y, z, b);
        p.set(e(x), y, z, reflejar(b));
    }

    public static String reflejar(String b) {
        if (!b.contains("[")) {
            return b;
        }
        String s = b.replace("facing=east", "facing=@E").replace("facing=west", "facing=east").replace("facing=@E", "facing=west");
        s = s.replace("hinge=left", "hinge=@L").replace("hinge=right", "hinge=left").replace("hinge=@L", "hinge=right");
        s = s.replace("shape=inner_left", "shape=@IL").replace("shape=inner_right", "shape=inner_left").replace("shape=@IL", "shape=inner_right");
        s = s.replace("shape=outer_left", "shape=@OL").replace("shape=outer_right", "shape=outer_left").replace("shape=@OL", "shape=outer_right");
        return s;
    }

    public void caja(int x0, int y0, int z0, int x1, int y1, int z1, String b) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    p.set(x, y, z, b);
                }
            }
        }
    }

    public void cajaPar(int x0, int y0, int z0, int x1, int y1, int z1, String b) {
        caja(x0, y0, z0, x1, y1, z1, b);
        caja(e(x0), y0, z0, e(x1), y1, z1, reflejar(b));
    }

    /** Una caja con varios bloques al azar (para muros viejos: ladrillo, ladrillo cuarteado, con musgo...). */
    public void mezcla(int x0, int y0, int z0, int x1, int y1, int z1, String... bs) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    p.set(x, y, z, bs[r.nextInt(bs.length)]);
                }
            }
        }
    }

    public void aire(int x0, int y0, int z0, int x1, int y1, int z1) {
        caja(x0, y0, z0, x1, y1, z1, "air");
    }

    /** Las cuatro paredes de una caja (sin piso ni techo). */
    public void paredes(int x0, int y0, int z0, int x1, int y1, int z1, String b) {
        caja(x0, y0, z0, x1, y1, z0, b);
        caja(x0, y0, z1, x1, y1, z1, b);
        caja(x0, y0, z0, x0, y1, z1, b);
        caja(x1, y0, z0, x1, y1, z1, b);
    }

    /** Ajedrez de dos bloques en un rectángulo (a la altura y). */
    public void ajedrez(int x0, int z0, int x1, int z1, int y, String a, String b) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, y, z, ((x + z) & 1) == 0 ? a : b);
            }
        }
    }

    /**
     * Un tramo de escalera recta: empieza en (x0..x1, y, z0) y sube un bloque por cada paso hacia {@code hacia} ("north", "south", "east",
     * "west"). Los escalones son del bloque {@code escalera} ({@code minecraft:dark_oak_stairs}), y debajo de cada uno, relleno con
     * {@code relleno} (o nada si es null). Devuelve la coordenada (z o x) del primer lugar después del último escalón.
     */
    public int tramo(int x0, int x1, int y, int z0, String hacia, int pasos, String escalera, String relleno) {
        int dx = hacia.equals("east") ? 1 : hacia.equals("west") ? -1 : 0, dz = hacia.equals("south") ? 1 : hacia.equals("north") ? -1 : 0;
        for (int k = 0; k < pasos; k++) {
            for (int a = Math.min(x0, x1); a <= Math.max(x0, x1); a++) {
                int x = dx == 0 ? a : x0 + dx * k, z = dx == 0 ? z0 + dz * k : a;
                p.set(x, y + k, z, escalera + "[facing=" + hacia + ",half=bottom]");
                if (relleno != null) {
                    for (int yy = y; yy < y + k; yy++) {
                        p.set(x, yy, z, relleno);
                    }
                }
            }
        }
        return dx == 0 ? z0 + dz * pasos : x0 + dx * pasos;
    }

    /** Puerta de madera de dos hojas (en x y x+1 si corre en x, o en z y z+1), mirando hacia {@code mira}, con las bisagras afuera. */
    public void puertaDoble(int x, int y, int z, String madera, String mira) {
        boolean enX = mira.equals("north") || mira.equals("south");
        // la bisagra izquierda queda a la izquierda de quien la pone mirando hacia "mira"
        String a = mira.equals("south") ? "right" : mira.equals("north") ? "left" : mira.equals("east") ? "left" : "right";
        String b = a.equals("left") ? "right" : "left";
        String d = M + madera + "_door[facing=" + mira + ",half=";
        int x2 = enX ? x + 1 : x, z2 = enX ? z : z + 1;
        p.set(x, y, z, d + "lower,hinge=" + a + ",open=false]");
        p.set(x, y + 1, z, d + "upper,hinge=" + a + ",open=false]");
        p.set(x2, y, z2, d + "lower,hinge=" + b + ",open=false]");
        p.set(x2, y + 1, z2, d + "upper,hinge=" + b + ",open=false]");
    }

    public void puerta(int x, int y, int z, String madera, String mira, String bisagra) {
        String d = M + madera + "_door[facing=" + mira + ",half=";
        p.set(x, y, z, d + "lower,hinge=" + bisagra + ",open=false]");
        p.set(x, y + 1, z, d + "upper,hinge=" + bisagra + ",open=false]");
    }

    /** Un adorno alto del Coleccionista (dos bloques: mitad de abajo y de arriba). */
    public void alto(int x, int y, int z, String id, String mira) {
        p.set(x, y, z, C + id + "[facing=" + mira + ",half=lower]");
        p.set(x, y + 1, z, C + id + "[facing=" + mira + ",half=upper]");
    }

    public void altoPar(int x, int y, int z, String id, String mira) {
        alto(x, y, z, id, mira);
        alto(e(x), y, z, id, Trazo.espejar(mira));
    }

    public static String espejar(String mira) {
        return mira.equals("east") ? "west" : mira.equals("west") ? "east" : mira;
    }

    /** Un adorno de un bloque del Coleccionista mirando hacia algún lado. */
    public void deco(int x, int y, int z, String id, String mira) {
        p.set(x, y, z, C + id + "[facing=" + mira + "]");
    }

    public void decoPar(int x, int y, int z, String id, String mira) {
        deco(x, y, z, id, mira);
        deco(e(x), y, z, id, espejar(mira));
    }

    /** Una luz invisible. */
    public void luz(int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
    }

    /** Un candelabro de techo (araña) colgado de una cadena de {@code largo} eslabones desde el techo en y. */
    public void arana(int x, int yTecho, int z, int largo) {
        for (int k = 1; k <= largo; k++) {
            p.set(x, yTecho - k, z, C + "cadena[axis=y]");
        }
        p.set(x, yTecho - largo - 1, z, C + "arana[facing=north]");
    }
}
