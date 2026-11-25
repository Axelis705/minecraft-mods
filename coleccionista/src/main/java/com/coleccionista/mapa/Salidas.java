package com.coleccionista.mapa;

/**
 * El cuarto de la "salida" de un circuito de los sentidos: un vestíbulo de azulejo con tres sillas mirando a una puerta de acero con el
 * letrero de SALIDA. Hay uno al final de cada circuito y una copia idéntica de cada uno en el teatro de la colección
 * ({@link SalaVitrina}): cuando terminan todos, los que se salvaron pasan (sentados, sin que se note) del cuarto del circuito al del teatro,
 * y ahí sí se abre la puerta. Al último equipo, en cambio, su cuarto se le llena de gas.
 * <p>
 * Por dentro mide {@link #ANCHO} x {@link #ALTO} x {@link #FONDO}. Se entra por atrás ({@code <id>.entrada.a/.b}, k = -1) y la puerta de
 * SALIDA está al frente (k = FONDO, marcas {@code cab.<id>.puerta.a/.b}). Marcas: {@code cab.<id>.origen} (la esquina, como las cabinas,
 * para pasar de un cuarto a otro con {@code Cabina.trasladar}) y {@code <id>.ventila.0..3} (de donde sale el gas).
 */
public final class Salidas {
    public static final int ANCHO = 8, ALTO = 4, FONDO = 7;
    private static final String C = "coleccionista:";

    private Salidas() {
    }

    /** Dónde va la silla i (0..2), en coordenadas locales del cuarto (u a lo ancho, w hacia el frente). */
    public static double[] silla(int i) {
        return new double[] {2.0 + 2 * i, 4.5};
    }

    public static void sala(Plano p, String id, Marco m, int y0) {
        for (int i = -1; i <= ANCHO; i++) {
            for (int k = -1; k <= FONDO; k++) {
                boolean muro = i == -1 || i == ANCHO || k == -1 || k == FONDO;
                m.set(p, i, y0, k, C + ((i + k) % 2 == 0 ? "linoleo" : "linoleo"));
                m.set(p, i, y0 + ALTO + 1, k, C + "concreto");
                for (int y = 1; y <= ALTO; y++) {
                    m.set(p, i, y0 + y, k, muro ? C + (y <= 2 ? "azulejo_sucio" : y == 3 ? "azulejo_remate" : "yeso") : "air");
                }
            }
        }
        // las dos puertas: la de entrada atrás y la de SALIDA al frente (de acero, opacas)
        puerta(p, m, y0, -1);
        puerta(p, m, y0, FONDO);
        m.marca(p, id + ".entrada.a", 3, y0 + 1, -1, 0);
        m.marca(p, id + ".entrada.b", 4, y0 + 1, -1, 0);
        m.marca(p, "cab." + id + ".puerta.a", 3, y0 + 1, FONDO, 0);
        m.marca(p, "cab." + id + ".puerta.b", 4, y0 + 1, FONDO, 0);
        p.marca("cab." + id + ".origen", m.cx, y0 + 1, m.cz, m.yaw);
        // el letrero verde de SALIDA sobre la puerta, y el de "tomen asiento"
        m.texto(p, 3, y0 + 3, FONDO - 1, "letrero", "north", 0x4CB050, "SALIDA");
        m.letrero(p, 7, y0 + 2, 2, "west", "gold", "TOMEN", "ASIENTO");
        // una tarima de metal debajo de las sillas
        for (int i = 1; i <= 6; i++) {
            for (int k = 3; k <= 5; k++) {
                m.set(p, i, y0, k, C + "metal_placa");
            }
        }
        // las rejillas del techo (de ahí sale el gas para el último equipo) y una bocina
        int[][] v = {{0, 1, 1}, {7, 1, -1}, {0, 5, 1}, {7, 5, -1}};
        for (int n = 0; n < v.length; n++) {
            m.set(p, v[n][0], y0 + ALTO, v[n][1], C + "ventila[facing=" + (v[n][2] > 0 ? "east" : "west") + "]");
            m.marca(p, id + ".ventila." + n, v[n][0], y0 + ALTO, v[n][1], 0);
        }
        m.set(p, 0, y0 + 3, 3, C + "bocina[facing=east]");
        m.set(p, 7, y0 + ALTO, 6, C + "camara[facing=west]");
        // la luz: dos paneles en el techo con su luz pegada
        for (int i : new int[] {2, 5}) {
            m.set(p, i, y0 + ALTO + 1, 3, C + "panel_luz");
            m.set(p, i, y0 + ALTO, 3, "light[level=13]");
        }
        m.set(p, 3, y0 + ALTO, 0, "light[level=10]");
    }

    /** Una puerta corrediza de acero de dos hojas en el muro k (atrás o al frente), con el dintel arriba. */
    private static void puerta(Plano p, Marco m, int y0, int k) {
        m.set(p, 3, y0 + 1, k, C + "puerta_metal[facing=south,half=lower,hinge=left,open=false]");
        m.set(p, 3, y0 + 2, k, C + "puerta_metal[facing=south,half=upper,hinge=left,open=false]");
        m.set(p, 4, y0 + 1, k, C + "puerta_metal[facing=south,half=lower,hinge=right,open=false]");
        m.set(p, 4, y0 + 2, k, C + "puerta_metal[facing=south,half=upper,hinge=right,open=false]");
        m.set(p, 3, y0 + 3, k, C + "metal_negro");
        m.set(p, 4, y0 + 3, k, C + "metal_negro");
    }
}
