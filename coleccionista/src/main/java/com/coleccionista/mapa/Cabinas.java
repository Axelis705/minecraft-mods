package com.coleccionista.mapa;

/**
 * Construye las cabinas que viajan (ver {@code evento.Cabina}): todas iguales por dentro, para que al pasar de una a otra no se note el
 * salto. Por dentro miden {@link #ANCHO} x {@link #ALTO} x {@link #FONDO} bloques; la puerta (dos hojas) va al centro del frente.
 * <p>
 * Se ubican con una esquina ({@code ox, oz}: la orilla de enfrente, a la izquierda vista desde afuera) y un giro: {@code yaw} es hacia
 * dónde se camina al entrar (0 sur, 90 oeste, 180 norte, 270 este). Marcas: {@code cab.<id>.origen} y {@code cab.<id>.puerta.a/.b}.
 */
public final class Cabinas {
    public static final int ANCHO = 8, ALTO = 4, FONDO = 4;
    private static final String C = "coleccionista:";

    private Cabinas() {
    }

    /** El bloque del mundo (x, z) de la celda local (i a lo ancho, k al fondo) de una cabina con esquina (ox, oz) y giro yaw. */
    public static int[] celda(int ox, int oz, float yaw, int i, int k) {
        double r = Math.toRadians(yaw), c = Math.round(Math.cos(r)), s = Math.round(Math.sin(r));
        double x = i + 0.5, z = k + 0.5;
        return new int[] {ox + (int) Math.floor(x * c - z * s), oz + (int) Math.floor(x * s + z * c)};
    }

    private static String haciaDentro(float yaw) {
        int y = Math.floorMod(Math.round(yaw), 360);
        return y == 0 ? "south" : y == 90 ? "west" : y == 180 ? "north" : "east";
    }

    /** Un elevador: muros de metal negro, piso de placa, luz en el techo, barandal atrás y la puerta corrediza al frente. */
    public static void elevador(Plano p, String id, int ox, int oz, float yaw, String numero) {
        String f = haciaDentro(yaw);
        for (int i = -1; i <= ANCHO; i++) {
            for (int k = -1; k <= FONDO; k++) {
                int[] b = celda(ox, oz, yaw, i, k);
                int x = b[0], z = b[1];
                p.set(x, 0, z, C + ((i + k) % 3 == 0 ? "rejilla_piso[type=double]" : "metal_placa"));
                p.set(x, ALTO + 1, z, C + "metal_negro");
                boolean lado = i == -1 || i == ANCHO || k == FONDO;
                boolean frente = k == -1 && !lado;
                for (int y = 1; y <= ALTO; y++) {
                    if (lado || frente) {
                        p.set(x, y, z, C + (y == ALTO ? "viga[axis=y]" : "metal_negro"));
                    } else {
                        p.set(x, y, z, "air");
                    }
                }
            }
        }
        // la luz en el techo (y una luz invisible pegada a él) y el barandal del fondo
        for (int i = 2; i <= ANCHO - 3; i += 3) {
            int[] b = celda(ox, oz, yaw, i, FONDO / 2);
            p.set(b[0], ALTO + 1, b[1], C + "panel_luz");
            p.set(b[0], ALTO, b[1], "light[level=13]");
        }
        for (int i = 0; i < ANCHO; i++) {
            int[] b = celda(ox, oz, yaw, i, FONDO);
            p.set(b[0], 2, b[1], C + "metal_placa");
        }
        // la puerta corrediza: dos hojas al centro del frente
        int[] a = celda(ox, oz, yaw, ANCHO / 2 - 1, -1), b = celda(ox, oz, yaw, ANCHO / 2, -1);
        p.set(a[0], 1, a[1], C + "puerta_celda[facing=" + f + ",half=lower,hinge=left,open=false]");
        p.set(a[0], 2, a[1], C + "puerta_celda[facing=" + f + ",half=upper,hinge=left,open=false]");
        p.set(b[0], 1, b[1], C + "puerta_celda[facing=" + f + ",half=lower,hinge=right,open=false]");
        p.set(b[0], 2, b[1], C + "puerta_celda[facing=" + f + ",half=upper,hinge=right,open=false]");
        p.set(a[0], 3, a[1], C + "metal_negro");
        p.set(b[0], 3, b[1], C + "metal_negro");
        p.marca("cab." + id + ".puerta.a", a[0], 1, a[1], yaw);
        p.marca("cab." + id + ".puerta.b", b[0], 1, b[1], yaw);
        p.marca("cab." + id + ".origen", ox, 1, oz, yaw);
        // la placa con el número sobre la puerta, por fuera
        if (numero != null) {
            int[] c = celda(ox, oz, yaw, ANCHO / 2 - 1, -2);
            String afuera = switch (f) {
                case "south" -> "north";
                case "west" -> "east";
                case "north" -> "south";
                default -> "west";
            };
            p.texto(c[0], ALTO, c[1], "placa", afuera, 0xD9A93A, "ELEVADOR", numero);
        }
    }
}
