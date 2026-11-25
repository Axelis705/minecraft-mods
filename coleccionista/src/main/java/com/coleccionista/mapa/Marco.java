package com.coleccionista.mapa;

/**
 * Un sistema de coordenadas girado para construir algo igual en cualquier orientación (una celda, un elevador, un cuarto). Lo local se
 * escribe como si el giro fuera 0: {@code i} (o {@code u}) crece hacia el este y {@code k} (o {@code w}) hacia el sur, que es "hacia el
 * frente". Con el giro (0 sur, 90 oeste, 180 norte, 270 este, como el yaw de Minecraft) se pasa todo al mundo: posiciones, hacia dónde
 * mira un bloque, el eje de una viga y el yaw de una marca. No usa nada de Minecraft (se puede correr en el visor).
 * <p>
 * {@code (cx, cz)} es la esquina (un punto, no un bloque) que queda atrás a la izquierda del bloque local (0, 0).
 */
public final class Marco {
    public final int cx, cz;
    public final float yaw;
    /** El eje "u" (hacia el este local) y el eje "w" (hacia el frente local) en el mundo. */
    public final int ux, uz, wx, wz;

    public Marco(int cx, int cz, float yaw) {
        this.cx = cx;
        this.cz = cz;
        this.yaw = yaw;
        double r = Math.toRadians(yaw);
        int c = (int) Math.round(Math.cos(r)), s = (int) Math.round(Math.sin(r));
        ux = c;
        uz = s;
        wx = -s;
        wz = c;
    }

    /** El bloque del mundo {x, z} del bloque local (i, k). */
    public int[] b(int i, int k) {
        double x = cx + (i + 0.5) * ux + (k + 0.5) * wx, z = cz + (i + 0.5) * uz + (k + 0.5) * wz;
        return new int[] {(int) Math.floor(x), (int) Math.floor(z)};
    }

    /** Un punto local (u, w) en el mundo {x, z}. */
    public double[] p(double u, double w) {
        return new double[] {cx + u * ux + w * wx, cz + u * uz + w * wz};
    }

    /** Una dirección local ("north", "south", "east", "west"; el sur local es hacia el frente) en el mundo. */
    public String f(String local) {
        int[] v = switch (local) {
            case "east" -> new int[] {1, 0};
            case "west" -> new int[] {-1, 0};
            case "north" -> new int[] {0, -1};
            default -> new int[] {0, 1};
        };
        int x = v[0] * ux + v[1] * wx, z = v[0] * uz + v[1] * wz;
        return x > 0 ? "east" : x < 0 ? "west" : z < 0 ? "north" : "south";
    }

    /** El eje de una viga o tubo local ("x", "y", "z") en el mundo. */
    public String eje(String local) {
        if ("y".equals(local)) {
            return "y";
        }
        boolean girado = ux == 0;
        return girado ? ("x".equals(local) ? "z" : "x") : local;
    }

    /** Un yaw local (0 = mirando al frente) en el mundo. */
    public float yaw(float local) {
        return ((local + yaw) % 360 + 360) % 360;
    }

    /** Cambia en un bloque las propiedades {@code facing=} y {@code axis=} de local a mundo (y la bisagra no cambia). */
    public String bloque(String s) {
        int a = s.indexOf('[');
        if (a < 0) {
            return s;
        }
        String base = s.substring(0, a), props = s.substring(a + 1, s.length() - 1);
        StringBuilder sb = new StringBuilder(base).append('[');
        String[] ps = props.split(",");
        for (int n = 0; n < ps.length; n++) {
            String[] kv = ps[n].split("=", 2);
            String v = kv.length > 1 ? kv[1] : "";
            if ("facing".equals(kv[0]) && !"up".equals(v) && !"down".equals(v)) {
                v = f(v);
            } else if ("axis".equals(kv[0])) {
                v = eje(v);
            }
            sb.append(n > 0 ? "," : "").append(kv[0]).append('=').append(v);
        }
        return sb.append(']').toString();
    }

    // ------------------------------------------------------------------ atajos sobre un plano

    public void set(Plano p, int i, int y, int k, String s) {
        int[] b = b(i, k);
        p.set(b[0], y, b[1], bloque(s));
    }

    public void caja(Plano p, int i0, int y0, int k0, int i1, int y1, int k1, String s) {
        for (int i = Math.min(i0, i1); i <= Math.max(i0, i1); i++) {
            for (int k = Math.min(k0, k1); k <= Math.max(k0, k1); k++) {
                for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                    set(p, i, y, k, s);
                }
            }
        }
    }

    public void marca(Plano p, String nombre, int i, int y, int k, float yawLocal) {
        int[] b = b(i, k);
        p.marca(nombre, b[0], y, b[1], yaw(yawLocal));
    }

    public void texto(Plano p, int i, int y, int k, String bloque, String facingLocal, int color, String... lineas) {
        int[] b = b(i, k);
        p.texto(b[0], y, b[1], bloque, f(facingLocal), color, lineas);
    }

    public void letrero(Plano p, int i, int y, int k, String facingLocal, String color, String... lineas) {
        int[] b = b(i, k);
        p.letrero(b[0], y, b[1], "", f(facingLocal), color, lineas);
    }

    /** Prueba sin Minecraft. */
    public static void main(String[] a) {
        Marco m0 = new Marco(7, 1, 0);
        check(java.util.Arrays.equals(m0.b(0, 0), new int[] {7, 1}), "giro 0: (0,0) -> (7,1)");
        check(m0.f("south").equals("south") && m0.f("east").equals("east"), "giro 0 no cambia direcciones");
        // giro 180: la celda del lado sur del patio, con la esquina en (x 12, z 42); el bloque (0,0) queda en (11, 41)
        Marco m1 = new Marco(12, 42, 180);
        check(java.util.Arrays.equals(m1.b(0, 0), new int[] {11, 41}), "giro 180: (0,0) -> (11,41) " + java.util.Arrays.toString(m1.b(0, 0)));
        check(java.util.Arrays.equals(m1.b(4, 4), new int[] {7, 37}), "giro 180: (4,4) -> (7,37)");
        check(m1.f("south").equals("north") && m1.f("east").equals("west"), "giro 180 voltea");
        // giro 270 (frente hacia el este): esquina (1, 12): el bloque (0,0) en (1, 11) y (4,4) en (5, 7)
        Marco m2 = new Marco(1, 12, 270);
        check(java.util.Arrays.equals(m2.b(0, 0), new int[] {1, 11}), "giro 270: (0,0) -> (1,11) " + java.util.Arrays.toString(m2.b(0, 0)));
        check(java.util.Arrays.equals(m2.b(4, 4), new int[] {5, 7}), "giro 270: (4,4) -> (5,7) " + java.util.Arrays.toString(m2.b(4, 4)));
        check(m2.f("south").equals("east"), "giro 270: el frente es el este");
        check(m2.eje("x").equals("z"), "giro 270: viga en x queda en z");
        check(m2.bloque("a:b[facing=north,half=lower,axis=x]").equals("a:b[facing=west,half=lower,axis=z]"), "bloque girado");
        // giro 90 (frente al oeste): esquina (42, 7): (0,0) -> (41, 7)
        Marco m3 = new Marco(42, 7, 90);
        check(java.util.Arrays.equals(m3.b(0, 0), new int[] {41, 7}), "giro 90: (0,0) -> (41,7) " + java.util.Arrays.toString(m3.b(0, 0)));
        System.out.println("MARCO_OK");
    }

    private static void check(boolean ok, String m) {
        if (!ok) {
            throw new AssertionError(m);
        }
        System.out.println("ok: " + m);
    }
}
