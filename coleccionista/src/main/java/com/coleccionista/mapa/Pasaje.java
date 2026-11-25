package com.coleccionista.mapa;

/**
 * El pasillo de servicio (la "antesala"): hay uno detrás del teatro de la colección y otro idéntico a la entrada de la mansión. Cuando se
 * cierra la puerta detrás de todos y la pantalla del fondo termina de hablar, todos pasan (sin que se note) de uno al otro; en el de la
 * mansión se abren las dos puertas del fondo y ahí está la casa.
 * <p>
 * Por dentro mide {@link #ANCHO} x {@link #ALTO} x {@link #LARGO}. Se entra por atrás (k = -1, marcas {@code <id>.entrada.a/.b}); al
 * fondo (k = LARGO) está la pantalla al centro y una puerta de cada lado ({@code <id>.izq.a/.b} y {@code <id>.der.a/.b}). Marcas:
 * {@code cab.<id>.origen} (para pasar de un pasillo al otro), {@code <id>.pantalla} y {@code <id>.centro}.
 */
public final class Pasaje {
    public static final int ANCHO = 8, ALTO = 5, LARGO = 14;
    private static final String C = "coleccionista:";

    private Pasaje() {
    }

    public static void pasillo(Plano p, String id, Marco m) {
        for (int i = -1; i <= ANCHO; i++) {
            for (int k = -1; k <= LARGO; k++) {
                boolean muro = i == -1 || i == ANCHO || k == -1 || k == LARGO;
                m.set(p, i, 0, k, C + ((i + k) % 2 == 0 ? "linoleo" : "linoleo"));
                m.set(p, i, ALTO + 1, k, C + "concreto");
                for (int y = 1; y <= ALTO; y++) {
                    m.set(p, i, y, k, muro ? C + (y <= 2 ? "panel_madera" : y == 3 ? "marco_dorado" : "papel_tapiz") : "air");
                }
            }
        }
        // la puerta de atrás (por donde se entra) y las dos del fondo
        puerta(p, m, 3, -1);
        puerta(p, m, 0, LARGO);
        puerta(p, m, 6, LARGO);
        m.marca(p, id + ".entrada.a", 3, 1, -1, 0);
        m.marca(p, id + ".entrada.b", 4, 1, -1, 0);
        m.marca(p, id + ".izq.a", 0, 1, LARGO, 0);
        m.marca(p, id + ".izq.b", 1, 1, LARGO, 0);
        m.marca(p, id + ".der.a", 6, 1, LARGO, 0);
        m.marca(p, id + ".der.b", 7, 1, LARGO, 0);
        p.marca("cab." + id + ".origen", m.cx, 1, m.cz, m.yaw);
        // la pantalla grande al fondo, al centro, con dos candelabros
        m.set(p, 3, 3, LARGO - 1, C + "pantalla[facing=north,grande=true]");
        m.marca(p, id + ".pantalla", 3, 3, LARGO - 1, 180);
        m.set(p, 2, 1, LARGO - 1, C + "candelabro[facing=north,half=lower]");
        m.set(p, 2, 2, LARGO - 1, C + "candelabro[facing=north,half=upper]");
        m.set(p, 5, 1, LARGO - 1, C + "candelabro[facing=north,half=lower]");
        m.set(p, 5, 2, LARGO - 1, C + "candelabro[facing=north,half=upper]");
        // un tapete al centro, sillones contra las paredes, cuadros vacíos y luz baja
        for (int k = 1; k <= LARGO - 2; k++) {
            m.set(p, 3, 0, k, C + "tapete_rojo");
            m.set(p, 4, 0, k, C + "tapete_rojo");
        }
        for (int k : new int[] {3, 8}) {
            m.set(p, 0, 1, k, C + "sillon[facing=east,half=lower]");
            m.set(p, 0, 2, k, C + "sillon[facing=east,half=upper]");
            m.set(p, ANCHO - 1, 1, k, C + "sillon[facing=west,half=lower]");
            m.set(p, ANCHO - 1, 2, k, C + "sillon[facing=west,half=upper]");
        }
        for (int k = 2; k < LARGO; k += 4) {
            m.set(p, 3, ALTO, k, "light[level=10]");
        }
        m.set(p, 0, 4, 11, C + "camara[facing=east]");
        m.marca(p, id + ".centro", 3, 1, 6, 0);
    }

    /** Una puerta corrediza de acero de dos hojas en el muro k (i, i+1), con el dintel arriba. */
    private static void puerta(Plano p, Marco m, int i, int k) {
        m.set(p, i, 1, k, C + "puerta_metal[facing=south,half=lower,hinge=left,open=false]");
        m.set(p, i, 2, k, C + "puerta_metal[facing=south,half=upper,hinge=left,open=false]");
        m.set(p, i + 1, 1, k, C + "puerta_metal[facing=south,half=lower,hinge=right,open=false]");
        m.set(p, i + 1, 2, k, C + "puerta_metal[facing=south,half=upper,hinge=right,open=false]");
        m.set(p, i, 3, k, C + "metal_negro");
        m.set(p, i + 1, 3, k, C + "metal_negro");
    }
}
