package com.coleccionista.mapa;

/**
 * Los vehículos que unen un lugar con otro. Cada uno se construye dos veces, idéntico por dentro (el que sale y el que llega): mientras
 * "viaja" (puertas cerradas, se sacude, suena el motor y por las ventanas pasan las luces) cada quien pasa del uno al otro en el mismo
 * lugar y mirando igual ({@code evento.Cabina}), y al llegar se abren las puertas del otro lado.
 * <p>
 * Marcas de cada uno: {@code cab.<id>.origen} (la esquina, con el giro), {@code cab.<id>.puerta.a/.b} (las hojas de la puerta),
 * {@code <id>.subir} (afuera de la puerta: a donde apunta la flecha para subir), {@code <id>.centro} y {@code <id>.pantalla}.
 */
public final class Vehiculos {
    /** El autobús del Coleccionista por dentro: 5 de ancho, 3 de alto y 13 de largo (el chofer y la pantalla van al frente). */
    public static final int BUS_ANCHO = 5, BUS_ALTO = 3, BUS_LARGO = 13;
    /** Las dos hojas de la puerta del autobús (del lado i = BUS_ANCHO, cerca del frente). */
    public static final int BUS_PUERTA_K = 9;
    private static final String C = "coleccionista:";

    private Vehiculos() {
    }

    /**
     * El autobús del Coleccionista: carrocería vino con franja crema, ventanas a la altura de los ojos, llantas, faros y calaveras; por
     * dentro asientos de vinil a los dos lados, pasillo con tapete, agarraderas que cuelgan, luces de techo y al frente la pantalla del
     * Coleccionista (tapa el parabrisas) y un maniquí al volante. {@code m}: k crece hacia el frente, i a lo ancho (la puerta va del
     * lado i = ANCHO). {@code y}: el piso, al nivel del suelo de afuera (no hay escalón).
     */
    public static void autobus(Plano p, String id, Marco m, int y) {
        int a = BUS_ANCHO, l = BUS_LARGO, h = BUS_ALTO;
        for (int i = -1; i <= a; i++) {
            for (int k = -1; k <= l; k++) {
                boolean lado = i == -1 || i == a, frente = k == l, atras = k == -1;
                boolean muro = lado || frente || atras;
                m.set(p, i, y, k, muro ? C + "metal_negro" : C + "linoleo");
                m.set(p, i, y + h + 1, k, lado || frente || atras ? C + "carroceria" : C + "carroceria_crema");
                for (int yy = y + 1; yy <= y + h; yy++) {
                    m.set(p, i, yy, k, muro ? pared(i, yy - y, k) : "air");
                }
            }
        }
        // los faros y las calaveras
        m.set(p, 0, y + 1, l, C + "marco_luz_amarilla");
        m.set(p, a - 1, y + 1, l, C + "marco_luz_amarilla");
        m.set(p, 0, y + 1, -1, C + "marco_luz_roja");
        m.set(p, a - 1, y + 1, -1, C + "marco_luz_roja");
        // la puerta plegable (dos hojas) del lado derecho, cerca del frente
        int kp = BUS_PUERTA_K;
        m.set(p, a, y + 1, kp, C + "puerta_autobus[facing=west,half=lower,hinge=left,open=false]");
        m.set(p, a, y + 2, kp, C + "puerta_autobus[facing=west,half=upper,hinge=left,open=false]");
        m.set(p, a, y + 1, kp + 1, C + "puerta_autobus[facing=west,half=lower,hinge=right,open=false]");
        m.set(p, a, y + 2, kp + 1, C + "puerta_autobus[facing=west,half=upper,hinge=right,open=false]");
        m.set(p, a, y + 3, kp, C + "carroceria_crema");
        m.set(p, a, y + 3, kp + 1, C + "carroceria_crema");
        m.marca(p, "cab." + id + ".puerta.a", a, y + 1, kp, 0);
        m.marca(p, "cab." + id + ".puerta.b", a, y + 1, kp + 1, 0);
        // las llantas (pegadas a los lados, sobre el suelo)
        for (int k : new int[] {1, l - 1}) {
            m.set(p, -2, y + 1, k, C + "llanta[facing=west]");
            m.set(p, a + 1, y + 1, k, C + "llanta[facing=east]");
        }
        // el letrero de la ruta (arriba del parabrisas) y el nombre en el costado
        p.letrero(m.b(2, l + 1)[0], y + 3, m.b(2, l + 1)[1], "", m.f("south"), "gold", "LA COLECCIÓN", "Ruta 13");
        int[] pl = m.b(a + 1, 4);
        p.texto(pl[0], y + 2, pl[1], "placa", m.f("east"), 0xD9C38A, "TRANSPORTES", "DEL", "COLECCIONISTA");
        // adentro: asientos a los dos lados (mirando al frente), el pasillo con su tapete y las agarraderas
        for (int k = 0; k <= l - 5; k++) {
            m.set(p, 0, y + 1, k, C + "asiento_bus[facing=south]");
            if (k < kp - 1) {
                m.set(p, a - 1, y + 1, k, C + "asiento_bus[facing=south]");
            }
        }
        for (int k = 0; k <= l - 2; k++) {
            m.set(p, 2, y + 1, k, C + "alfombra_negra");
            if (k % 2 == 0 && k <= l - 3) {
                m.set(p, 1, y + 3, k, C + "cadena");
                m.set(p, 3, y + 3, k, C + "cadena");
            }
        }
        // el chofer: un maniquí al volante (adelante a la izquierda)
        m.set(p, 0, y + 1, l - 3, C + "maniqui[facing=south,half=lower]");
        m.set(p, 0, y + 2, l - 3, C + "maniqui[facing=south,half=upper]");
        m.set(p, 0, y + 1, l - 2, C + "volante[facing=north]");
        // junto a la puerta, una maleta olvidada
        m.set(p, a - 1, y + 1, kp - 1, C + "maleta[facing=east]");
        // la pantalla del Coleccionista: llena el frente (4 x 3), sobre el parabrisas
        m.set(p, 2, y + 2, l - 1, C + "pantalla[facing=north,grande=true]");
        int[] pb = m.b(2, l - 1);
        p.nbt(pb[0], y + 2, pb[1], "{tam:2,dx:0.0f}");
        m.marca(p, id + ".pantalla", 2, y + 2, l - 1, 180);
        // la luz del techo (paneles y una luz pareja, no muy fuerte)
        for (int k = 1; k <= l - 3; k += 4) {
            m.set(p, 2, y + h + 1, k, C + "panel_luz");
            m.set(p, 2, y + h, k, "light[level=10]");
        }
        m.marca(p, id + ".subir", a + 1, y + 1, kp, 0);
        m.marca(p, id + ".centro", 2, y + 1, 5, 0);
        p.marca("cab." + id + ".origen", m.cx, y + 1, m.cz, m.yaw);
    }

    /** La pared del autobús en (i, altura, k): abajo carrocería, a la altura de los ojos las ventanas (con postes), arriba la franja. */
    private static String pared(int i, int alto, int k) {
        boolean lado = i == -1 || i == BUS_ANCHO, frente = k == BUS_LARGO, atras = k == -1;
        if (alto == 1) {
            return C + "carroceria";
        }
        if (alto == 3) {
            return C + "carroceria_crema";
        }
        // la fila de las ventanas
        if (lado) {
            boolean poste = frente || atras || k % 3 == 2;
            return poste ? C + "carroceria" : C + "vidrio_sucio";
        }
        if (frente) {
            return C + "vidrio_sucio";
        }
        return i >= 1 && i <= BUS_ANCHO - 2 ? C + "vidrio_sucio" : C + "carroceria";
    }
}
