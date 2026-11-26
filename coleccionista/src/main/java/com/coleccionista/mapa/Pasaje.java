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

    /**
     * Cómo se ve un pasillo: el piso (dos, en ajedrez), la parte de abajo del muro, la moldura, la de arriba, el techo, el tapete del
     * centro y los cuatro asientos. El de siempre ({@link #CLASICO}) es igual en todos lados (para pasar de uno a otro sin que se note);
     * los de las salas a las que se llega caminando tienen el suyo, para que no parezca que se regresa al mismo lugar.
     */
    public record Tema(String pisoA, String pisoB, String bajo, String moldura, String alto, String techo, String tapete, String asiento,
                       int luz) {
    }

    public static final Tema CLASICO = new Tema(C + "linoleo", C + "linoleo", C + "panel_madera", C + "marco_dorado", C + "papel_tapiz",
            C + "concreto", C + "tapete_rojo", C + "sillon", 13);
    /** La caja de música: terciopelo y oro. */
    public static final Tema CAJA = new Tema(C + "parquet", C + "madera_pulida", C + "panel_madera", C + "marco_dorado", C + "acolchado",
            C + "madera_pulida", C + "tapete_rojo", C + "sillon", 12);
    /** El teatrito de Resortes: telón rojo y focos de feria. */
    public static final Tema CIRCO = new Tema(C + "madera_pulida", C + "tablon", C + "panel_madera", C + "marco_luz_amarilla", C + "telon",
            C + "telon_borde", C + "tapete_rojo", C + "silla", 12);
    /** El salón de té: azulejo y mármol. */
    public static final Tema TE = new Tema(C + "marmol_crema", C + "marmol_negro", C + "azulejo", C + "marco_dorado", C + "papel_tapiz",
            C + "yeso", C + "tapete_orilla", C + "silla", 12);
    /** El salón de baile: mármol claro y espejos. */
    public static final Tema BAILE = new Tema(C + "parquet", C + "parquet", C + "marmol_negro", C + "marco_dorado", C + "marmol_crema",
            C + "marmol_crema", C + "alfombra_roja", C + "sillon", 13);
    /** La mesa de juego: madera oscura y tapete verde. */
    public static final Tema CARTAS = new Tema(C + "marmol_negro", C + "tapete_rojo", C + "panel_madera", C + "marco_dorado",
            C + "linoleo_verde", C + "madera_pulida", C + "alfombra_negra", C + "sillon", 11);

    public static void pasillo(Plano p, String id, Marco m) {
        pasillo(p, id, m, CLASICO);
    }

    public static void pasillo(Plano p, String id, Marco m, Tema t) {
        for (int i = -1; i <= ANCHO; i++) {
            for (int k = -1; k <= LARGO; k++) {
                boolean muro = i == -1 || i == ANCHO || k == -1 || k == LARGO;
                m.set(p, i, 0, k, (i + k) % 2 == 0 ? t.pisoA() : t.pisoB());
                m.set(p, i, ALTO + 1, k, t.techo());
                for (int y = 1; y <= ALTO; y++) {
                    m.set(p, i, y, k, muro ? (y <= 2 ? t.bajo() : y == 3 ? t.moldura() : t.alto()) : "air");
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
        // la pantalla al fondo, al centro, con dos candelabros: de 4 x 3, justo entre las dos puertas (i 2..5) y corrida medio bloque
        // para quedar en medio del pasillo (que tiene 8 de ancho). La de 7 x 4 tapaba las puertas y la gente la atravesaba.
        m.set(p, 3, 3, LARGO - 1, C + "pantalla[facing=north,grande=true]");
        int[] pb = m.b(3, LARGO - 1);
        p.nbt(pb[0], 3, pb[1], "{tam:2,dx:-0.5f}");
        m.marca(p, id + ".pantalla", 3, 3, LARGO - 1, 180);
        m.set(p, 2, 1, LARGO - 1, C + "candelabro[facing=north,half=lower]");
        m.set(p, 2, 2, LARGO - 1, C + "candelabro[facing=north,half=upper]");
        m.set(p, 5, 1, LARGO - 1, C + "candelabro[facing=north,half=lower]");
        m.set(p, 5, 2, LARGO - 1, C + "candelabro[facing=north,half=upper]");
        // un tapete al centro, sillones contra las paredes, cuadros vacíos y luz baja
        boolean alfombra = t.tapete().contains("alfombra");
        for (int k = 1; k <= LARGO - 2; k++) {
            // el tapete: si es de bloque va en el piso; si es alfombra, encima
            m.set(p, 3, alfombra ? 1 : 0, k, t.tapete());
            m.set(p, 4, alfombra ? 1 : 0, k, t.tapete());
        }
        boolean alto = t.asiento().endsWith("sillon");
        for (int k : new int[] {3, 8}) {
            if (alto) {
                m.set(p, 0, 1, k, t.asiento() + "[facing=east,half=lower]");
                m.set(p, 0, 2, k, t.asiento() + "[facing=east,half=upper]");
                m.set(p, ANCHO - 1, 1, k, t.asiento() + "[facing=west,half=lower]");
                m.set(p, ANCHO - 1, 2, k, t.asiento() + "[facing=west,half=upper]");
            } else {
                m.set(p, 0, 1, k, t.asiento() + "[facing=east]");
                m.set(p, ANCHO - 1, 1, k, t.asiento() + "[facing=west]");
            }
        }
        for (int k = 2; k < LARGO; k += 4) {
            m.set(p, 3, ALTO, k, "light[level=" + t.luz() + "]");
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
