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

    /** El ascensor de un equipo (de 2 o 3): por dentro mide {@link #EQ_ANCHO} x {@link #ALTO} x {@link #EQ_FONDO}. */
    public static final int EQ_ANCHO = 4, EQ_FONDO = 3;

    /**
     * El ascensor de un equipo: viejo y feo (cobre oxidado, rejilla, un foco que parpadea), con la puerta de dos hojas al centro del frente.
     * El de la sala grande y el "falso" de cada circuito son idénticos por dentro. {@code m}: el marco (la esquina de atrás del interior está
     * en el bloque local (0, 0); el frente, con la puerta, en k = -1). Marcas: {@code cab.<id>.origen} y {@code cab.<id>.puerta.a/.b}.
     */
    public static void ascensor(Plano p, String id, Marco m) {
        for (int i = -1; i <= EQ_ANCHO; i++) {
            for (int k = -1; k <= EQ_FONDO; k++) {
                m.set(p, i, 0, k, C + ((i + k) % 2 == 0 ? "rejilla_piso[type=double]" : "metal_placa"));
                m.set(p, i, ALTO + 1, k, C + "metal_negro");
                boolean muro = i == -1 || i == EQ_ANCHO || k == -1 || k == EQ_FONDO;
                for (int y = 1; y <= ALTO; y++) {
                    if (muro) {
                        boolean franja = y == 2 && k == EQ_FONDO;
                        m.set(p, i, y, k, C + (y == ALTO ? "viga[axis=y]" : franja ? "metal_placa" : (i + y + k) % 3 == 0 ? "cobre_viejo" : "metal_negro"));
                    } else {
                        m.set(p, i, y, k, "air");
                    }
                }
            }
        }
        // el foco del techo (con una luz pegada) y una rejilla de ventilación
        m.set(p, 1, ALTO + 1, 1, C + "panel_luz");
        m.set(p, 1, ALTO, 1, "light[level=11]");
        m.set(p, 3, ALTO, EQ_FONDO - 1, C + "telarana");
        m.set(p, 0, ALTO, EQ_FONDO - 1, C + "ventila[facing=north]");
        // la puerta: dos hojas de latón al centro del frente, opacas (por dentro se ven igual en todos y no se ve que no se mueve)
        m.set(p, 1, 1, -1, C + "puerta_ascensor[facing=south,half=lower,hinge=left,open=false]");
        m.set(p, 1, 2, -1, C + "puerta_ascensor[facing=south,half=upper,hinge=left,open=false]");
        m.set(p, 2, 1, -1, C + "puerta_ascensor[facing=south,half=lower,hinge=right,open=false]");
        m.set(p, 2, 2, -1, C + "puerta_ascensor[facing=south,half=upper,hinge=right,open=false]");
        m.set(p, 1, 3, -1, C + "metal_negro");
        m.set(p, 2, 3, -1, C + "metal_negro");
        m.marca(p, "cab." + id + ".puerta.a", 1, 1, -1, 0);
        m.marca(p, "cab." + id + ".puerta.b", 2, 1, -1, 0);
        p.marca("cab." + id + ".origen", m.cx, 1, m.cz, m.yaw);
    }

    /** El ascensor de una sola persona (el hotel): por dentro mide 1 x {@link #IND_ALTO} x 1. */
    public static final int IND_ALTO = 3;

    /**
     * El ascensor de una persona, angosto como un ataúd parado: cobre viejo y metal negro, rejilla en el piso, un foco arriba y una sola
     * hoja de latón al frente. Los de la torre de la sala grande y los de llegada del hotel son idénticos por dentro. {@code m}: el marco
     * (el interior es el bloque local (0, 0); la puerta, el (0, -1)); {@code y0}: la altura del piso. Marcas: {@code cab.<id>.origen} y
     * {@code cab.<id>.puerta.a}. {@code numero}: la placa de afuera, arriba de la puerta (null: sin placa).
     */
    public static void individual(Plano p, String id, Marco m, int y0, String numero) {
        for (int i = -1; i <= 1; i++) {
            for (int k = -1; k <= 1; k++) {
                m.set(p, i, y0, k, C + "metal_placa");
                m.set(p, i, y0 + IND_ALTO + 1, k, C + "metal_negro");
                for (int y = 1; y <= IND_ALTO; y++) {
                    if (i == 0 && k == 0) {
                        m.set(p, 0, y0 + y, 0, "air");
                    } else if (!(i == 0 && k == -1)) {
                        boolean franja = y == 2 && k == 1;
                        m.set(p, i, y0 + y, k, C + (franja ? "metal_placa" : (i + y + k) % 3 == 0 ? "cobre_viejo" : "metal_negro"));
                    }
                }
            }
        }
        m.set(p, 0, y0, 0, C + "rejilla_piso[type=double]");
        m.set(p, 0, y0 + IND_ALTO + 1, 0, C + "panel_luz");
        m.set(p, 0, y0 + IND_ALTO, 0, "light[level=10]");
        // la puerta: una sola hoja de latón (opaca: por dentro todas se ven igual)
        m.set(p, 0, y0 + 1, -1, C + "puerta_ascensor[facing=south,half=lower,hinge=left,open=false]");
        m.set(p, 0, y0 + 2, -1, C + "puerta_ascensor[facing=south,half=upper,hinge=left,open=false]");
        m.set(p, 0, y0 + 3, -1, C + "metal_negro");
        m.marca(p, "cab." + id + ".puerta.a", 0, y0 + 1, -1, 0);
        p.marca("cab." + id + ".origen", m.cx, y0 + 1, m.cz, m.yaw);
        if (numero != null) {
            m.texto(p, 0, y0 + 3, -2, "placa", "north", 0xD9A93A, numero);
        }
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
        p.set(a[0], 1, a[1], C + "puerta_ascensor[facing=" + f + ",half=lower,hinge=left,open=false]");
        p.set(a[0], 2, a[1], C + "puerta_ascensor[facing=" + f + ",half=upper,hinge=left,open=false]");
        p.set(b[0], 1, b[1], C + "puerta_ascensor[facing=" + f + ",half=lower,hinge=right,open=false]");
        p.set(b[0], 2, b[1], C + "puerta_ascensor[facing=" + f + ",half=upper,hinge=right,open=false]");
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
