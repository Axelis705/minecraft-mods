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

    /** Una cabina de un pasillo (elevador o vagón): 3 de ancho, 3 de fondo; las dos van espalda con espalda en el pasillo de 3. */
    public static final int CAB_ANCHO = 3, CAB_FONDO = 3;

    /** Cómo se ve por dentro una cabina de pasillo. */
    public enum Estilo {
        /** El elevador de la merienda: madera, papel rosa, puertas de latón y un espejo. */
        ELEVADOR(4),
        /** El vagón del tren fantasma de los payasos: rayas rojas y blancas, focos amarillos y una banca. */
        TREN(3);

        public final int alto;

        Estilo(int alto) {
            this.alto = alto;
        }
    }

    /**
     * Dos cabinas idénticas espalda con espalda a lo largo de un pasillo de 3 de ancho (x0..x0+2) que se camina hacia +z ({@code dir} = 1)
     * o hacia -z (-1): la puerta de la de salida ({@code a}) está en z = {@code d} (de frente a quien llega), y la de llegada ({@code b})
     * abre del otro lado, hacia lo que sigue del pasillo. Entre las dos, un muro (la marca {@code trans.<a>.hueco}: por ahí la revisión de
     * mapas las da por unidas, porque se viaja de una a la otra).
     */
    public static void cabinaDoble(Plano p, String a, String b, int x0, int d, int dir, Estilo e) {
        Marco ma = dir > 0 ? new Marco(x0, d + 1, 0) : new Marco(x0 + 3, d, 180);
        Marco mb = dir > 0 ? new Marco(x0 + 3, d + 8, 180) : new Marco(x0, d - 7, 0);
        cabina(p, a, ma, e);
        cabina(p, b, mb, e);
        p.marca("trans." + a + ".hueco", x0 + 1, 1, d + 4 * dir, 0);
    }

    /** Una cabina de pasillo: la puerta (dos hojas) en el frente (k = -1, del lado izquierdo), el fondo en k = CAB_FONDO. */
    private static void cabina(Plano p, String id, Marco m, Estilo e) {
        int h = e.alto;
        boolean tren = e == Estilo.TREN;
        for (int i = -1; i <= CAB_ANCHO; i++) {
            for (int k = -1; k <= CAB_FONDO; k++) {
                boolean muro = i == -1 || i == CAB_ANCHO || k == -1 || k == CAB_FONDO;
                m.set(p, i, 0, k, tren ? C + "tablon" : C + "parquet");
                m.set(p, i, h + 1, k, tren ? (muro ? C + "telon_borde" : C + "marco_luz_amarilla") : (muro ? C + "marco_dorado" : C + "techo_yeso"));
                for (int y = 1; y <= h; y++) {
                    String w;
                    if (tren) {
                        w = y == 1 ? C + "panel_madera" : (i + k + y) % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool";
                    } else {
                        w = y == 1 ? C + "panel_madera" : y == h ? C + "marco_dorado" : C + "papel_rosa";
                    }
                    m.set(p, i, y, k, muro ? w : "air");
                }
            }
        }
        // la puerta: dos hojas del lado izquierdo del frente (la tercera columna es el tablero de botones)
        String hoja = tren ? "puerta_metal" : "puerta_ascensor";
        m.set(p, 0, 1, -1, C + hoja + "[facing=south,half=lower,hinge=left,open=false]");
        m.set(p, 0, 2, -1, C + hoja + "[facing=south,half=upper,hinge=left,open=false]");
        m.set(p, 1, 1, -1, C + hoja + "[facing=south,half=lower,hinge=right,open=false]");
        m.set(p, 1, 2, -1, C + hoja + "[facing=south,half=upper,hinge=right,open=false]");
        for (int y = 3; y <= h; y++) {
            m.set(p, 0, y, -1, tren ? C + "telon" : C + "marco_dorado");
            m.set(p, 1, y, -1, tren ? C + "telon" : C + "marco_dorado");
        }
        m.marca(p, "cab." + id + ".puerta.a", 0, 1, -1, 0);
        m.marca(p, "cab." + id + ".puerta.b", 1, 1, -1, 0);
        p.marca("cab." + id + ".origen", m.cx, 1, m.cz, m.yaw);
        m.marca(p, id + ".subir", 0, 1, -2, 0);
        m.marca(p, id + ".centro", 1, 1, 1, 0);
        // adentro
        if (tren) {
            for (int k = 1; k < CAB_FONDO; k++) {
                m.set(p, 0, 1, k, C + "losa_madera[type=bottom]");
                m.set(p, CAB_ANCHO - 1, 1, k, C + "losa_madera[type=bottom]");
            }
            m.set(p, CAB_ANCHO - 1, h, 0, C + "marioneta[facing=north]");
            m.set(p, 2, 2, -1, C + "panel_madera");
            m.texto(p, 0, h, CAB_FONDO - 1, "placa", "north", 0xD9A93A, "NO SAQUEN", "LAS MANOS");
            m.set(p, 1, h, 1, "light[level=11]");
        } else {
            m.set(p, CAB_ANCHO - 1, 1, CAB_FONDO - 1, C + "sillon[facing=north,half=lower]");
            m.set(p, CAB_ANCHO - 1, 2, CAB_FONDO - 1, C + "sillon[facing=north,half=upper]");
            m.set(p, 1, 3, CAB_FONDO - 1, C + "reloj[facing=north]");
            m.set(p, 0, 2, CAB_FONDO - 1, C + "espejo[facing=north]");
            m.set(p, 1, h, 1, "light[level=12]");
        }
        // afuera, arriba de la puerta: el letrero
        int[] q = m.b(1, -2);
        p.letrero(q[0], h, q[1], "", m.f("north"), "gold", tren ? "TREN" : "ASCENSOR", tren ? "FANTASMA" : "LA MERIENDA");
    }
}
