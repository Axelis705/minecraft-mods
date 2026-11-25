package com.coleccionista.mapa;

/**
 * El Patio de las celdas: donde despiertan las piezas. Un solo piso, fácil de entender: 20 celdas alrededor de un patio largo (8 al norte, 8
 * al sur y 4 al oeste) que miran todas a la torre del centro, una torre negra con cámaras (el Coleccionista habla en la pantalla chica de cada celda). Al
 * este, el único camino: el portón de barrotes que sube y lleva a la sala grande. Azulejo de hospital, yeso descarapelado, barrotes
 * oxidados y lámparas que zumban; todo con bloques propios del mod.
 * <pre>
 *   x: 0 ... 54 (celdas del oeste 1..5, patio 7..53, portón en x 54)
 *   z: 0 ... 36 (celdas del norte 1..5, patio 7..29, celdas del sur 31..35)
 *   y: 0 ... 13 (las celdas miden 4 de alto; el patio, 12)
 * </pre>
 * Celdas: 1-8 al norte (de oeste a este), 9-16 al sur (de este a oeste) y 17-20 al oeste (de sur a norte).
 * Marcas por celda N: {@code celda.N} (donde aparece, mirando a su pantalla), {@code celda.N.esquina} (la esquina de atrás a la izquierda
 * del interior, con el giro de la celda), {@code celda.N.pantalla}, {@code celda.N.puerta} (mitad de abajo de la puerta),
 * {@code celda.N.candado}, {@code celda.N.barril} (el baúl con las pistas), {@code celda.N.letrero} y {@code celda.N.letrero2},
 * {@code celda.N.cama}, {@code celda.N.foco}, {@code celda.N.cam1} y {@code celda.N.cam2} (la cámara del despertar). Generales:
 * {@code pantalla_grande} (y {@code pantalla_grande.2..4}), {@code salida}, {@code centro}, {@code porton.K} (las columnas del portón) y
 * {@code ala.luz.K} (las lámparas del patio que se apagan en la sala de espera).
 */
public final class AlaCeldas {
    public static final int CELDAS = 20;
    /** El portón: columnas en x 54, z 16..20; mide 5 de alto. */
    public static final int PORTON_X = 54, PORTON_Z0 = 16, PORTON_Z1 = 20, PORTON_ALTO = 5;
    private static final String C = "coleccionista:";
    private static final String AZULEJO = C + "azulejo", AZULEJO_SUCIO = C + "azulejo_sucio", REMATE = C + "azulejo_remate", YESO = C + "yeso";
    private static final String CONCRETO = C + "concreto", PISO = C + "concreto_piso", LINOLEO = C + "linoleo", VIGA = C + "viga";
    private static final String BARROTES = C + "barrotes", METAL = C + "metal_placa", NEGRO = C + "metal_negro";
    private static final int XA = 7, XB = 53, ZA = 7, ZB = 29, TECHO = 13;
    private static int luces;

    private AlaCeldas() {
    }

    /** El marco (esquina y giro) de cada celda, del 1 al 20. */
    public static Marco marco(int n) {
        if (n <= 8) {
            return new Marco(7 + 6 * (n - 1), 1, 0);
        }
        if (n <= 16) {
            int a = 49 - 6 * (n - 9);
            return new Marco(a + 5, 36, 180);
        }
        int a = 25 - 6 * (n - 17);
        return new Marco(1, a + 5, 270);
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        luces = 0;
        // todo macizo y luego se vacía el patio: así no quedan huecos en las esquinas
        p.caja(0, 0, 0, 54, TECHO, 36, CONCRETO);
        p.caja(XA, 1, ZA, XB, TECHO - 1, ZB, "air");
        muros(p);
        piso(p);
        for (int n = 1; n <= CELDAS; n++) {
            celda(p, n, marco(n));
        }
        torre(p);
        techo(p);
        porton(p);
        guardia(p);
        lucesPatio(p);
        luzPareja(p);
        p.marca("centro", 40, 1, 18, 90);
        return p;
    }

    /** El muro según la altura: azulejo sucio abajo, remate y yeso arriba. */
    private static String banda(int y) {
        return y == 1 ? AZULEJO_SUCIO : y == 2 ? REMATE : y <= 4 ? YESO : y == 5 ? CONCRETO : y == 6 ? REMATE : YESO;
    }

    /** Las paredes del patio arriba de las celdas (y la del este completa): la misma banda de azulejo. */
    private static void muros(Plano p) {
        for (int y = 5; y < TECHO; y++) {
            for (int x = XA - 1; x <= XB + 1; x++) {
                p.set(x, y, ZA - 1, banda(y));
                p.set(x, y, ZB + 1, banda(y));
            }
            for (int z = ZA; z <= ZB; z++) {
                p.set(XA - 1, y, z, banda(y));
            }
        }
        for (int y = 1; y < TECHO; y++) {
            for (int z = ZA; z <= ZB; z++) {
                p.set(XB + 1, y, z, banda(y));
            }
        }
        // sobre el frente de las celdas, una cornisa de acero con una franja de luz roja apagada (el "escaparate")
        for (int x = XA - 1; x <= XB + 1; x++) {
            p.set(x, 5, ZA - 1, VIGA + "[axis=x]");
            p.set(x, 5, ZB + 1, VIGA + "[axis=x]");
        }
        for (int z = ZA; z <= ZB; z++) {
            p.set(XA - 1, 5, z, VIGA + "[axis=z]");
        }
    }

    private static void piso(Plano p) {
        for (int x = XA; x <= XB; x++) {
            for (int z = ZA; z <= ZB; z++) {
                boolean orilla = z == ZA || z == ZB || x == XA;
                p.set(x, 0, z, orilla ? (Math.floorMod(x + z, 6) == 3 ? C + "coladera" : PISO) : ((x + z) % 2 == 0 ? LINOLEO : PISO));
            }
        }
        // la alfombra roja: de la torre al portón, y un anillo alrededor de la torre
        for (int x = 36; x <= XB; x++) {
            for (int z = 17; z <= 19; z++) {
                p.set(x, 1, z, C + "alfombra_roja");
            }
        }
        for (int x = 23; x <= 37; x++) {
            p.set(x, 1, 11, C + "alfombra_roja");
            p.set(x, 1, 25, C + "alfombra_roja");
        }
        for (int z = 11; z <= 25; z++) {
            p.set(23, 1, z, C + "alfombra_roja");
            p.set(37, 1, z, C + "alfombra_roja");
        }
        // charcos que gotean del techo, papeles y manchas tirados
        for (int x = XA + 2; x <= XB - 2; x += 5) {
            for (int z : new int[] {8, 28}) {
                if (p.azar(0.3)) {
                    p.set(x, 1, z, C + "charco[facing=north]");
                } else if (p.azar(0.3)) {
                    p.set(x, 1, z, C + (p.azar(0.5) ? "papeles" : "mancha") + "[facing=" + p.elegir("north", "east", "south", "west") + "]");
                }
            }
        }
    }

    // ------------------------------------------------------------------ una celda

    /**
     * Una celda en su marco: interior de 5 x 5 (i 0..4, k 0..4), la pared del fondo en k -1 y los barrotes al frente en k 5. Se escribe
     * como si mirara al sur: el catre a la izquierda (oeste local), el baño a la derecha y la pantalla en la pared del fondo.
     */
    private static void celda(Plano p, int n, Marco m) {
        int y0 = 0;
        // muros de los lados y del fondo con su banda, y el hueco del interior
        for (int y = 1; y <= 4; y++) {
            for (int k = -1; k <= 4; k++) {
                m.set(p, -1, y, k, banda(y));
                m.set(p, 5, y, k, banda(y));
            }
            for (int i = 0; i <= 4; i++) {
                m.set(p, i, y, -1, banda(y));
            }
            // las columnas de acero entre celdas, al frente
            m.set(p, -1, y, 5, VIGA + "[axis=y]");
            m.set(p, 5, y, 5, VIGA + "[axis=y]");
        }
        m.caja(p, 0, 1, 0, 4, 4, 4, "air");
        m.caja(p, 0, y0, 0, 4, y0, 4, PISO);
        m.caja(p, 0, y0 + 5, 0, 4, y0 + 5, 4, CONCRETO);
        // frente: barrotes oxidados con dintel de acero y la puerta en medio
        for (int i = 0; i <= 4; i++) {
            for (int y = y0 + 1; y <= y0 + 4; y++) {
                m.set(p, i, y, 5, y == y0 + 4 ? VIGA + "[axis=x]" : BARROTES);
            }
            m.set(p, i, y0, 5, METAL);
        }
        m.set(p, 2, y0 + 1, 5, C + "puerta_celda[facing=north,half=lower,hinge=left,open=false]");
        m.set(p, 2, y0 + 2, 5, C + "puerta_celda[facing=north,half=upper,hinge=left,open=false]");
        m.set(p, 2, y0 + 3, 5, BARROTES);
        m.marca(p, "celda." + n + ".puerta", 2, y0 + 1, 5, 0);
        // el número de la celda: placa atornillada arriba de la puerta, del lado del patio
        m.texto(p, 2, y0 + 5, 6, "placa", "south", 0xE8E2D0, "PIEZA", String.format("%02d", n));
        // el catre (cabecera contra el fondo, a la izquierda)
        m.set(p, 0, y0 + 1, 0, C + "catre[facing=north,part=head]");
        m.set(p, 0, y0 + 1, 1, C + "catre[facing=north,part=foot]");
        m.marca(p, "celda." + n + ".cama", 0, y0 + 1, 0, 0);
        // el baúl con las pistas en la esquina del fondo a la derecha
        m.set(p, 4, y0 + 1, 0, C + "cajon[facing=south]");
        m.marca(p, "celda." + n + ".barril", 4, y0 + 1, 0, 0);
        // retrete y lavabo contra la pared derecha
        m.set(p, 4, y0 + 1, 2, C + "retrete[facing=west]");
        m.set(p, 4, y0 + 1, 3, C + "lavabo[facing=west]");
        m.set(p, 4, y0 + 2, 3, C + "espejo[facing=west]");
        // la pantalla chica en la pared del fondo, al centro (ahí habla el Coleccionista), y una rejilla de ventilación arriba
        m.set(p, 2, y0 + 3, 0, C + "pantalla[facing=south]");
        m.marca(p, "celda." + n + ".pantalla", 2, y0 + 3, 0, 0);
        m.set(p, 4, y0 + 4, 0, C + "ventila[facing=south]");
        // el candado junto a la puerta, por dentro
        m.set(p, 3, y0 + 2, 4, C + "candado[facing=north]");
        m.marca(p, "celda." + n + ".candado", 3, y0 + 2, 4, 180);
        // dos rayones (pistas) en las paredes
        m.marca(p, "celda." + n + ".letrero", 4, y0 + 3, 2, 90);
        m.marca(p, "celda." + n + ".letrero2", 0, y0 + 2, 1, 270);
        // suciedad: manchas, papeles, telarañas
        if (p.azar(0.6)) {
            m.set(p, 2, y0 + 1, 2, C + "mancha[facing=" + p.elegir("north", "east", "south", "west") + "]");
        }
        if (p.azar(0.3)) {
            m.set(p, 1, y0 + 1, 3, C + "papeles[facing=" + p.elegir("north", "east", "south", "west") + "]");
        }
        if (p.azar(0.7)) {
            m.set(p, 0, y0 + 4, 0, C + "telarana");
        }
        // lo que dejaron las "piezas anteriores": grilletes en la pared y rayitas contando los días
        if (p.azar(0.35)) {
            m.set(p, 0, y0 + 3, 2, C + "grilletes[facing=east]");
        }
        if (p.azar(0.45)) {
            int dias = 4 + p.r.nextInt(18);
            StringBuilder l1 = new StringBuilder(), l2 = new StringBuilder();
            for (int i = 0; i < dias; i++) {
                StringBuilder l = i < 10 ? l1 : l2;
                l.append(i % 5 == 4 ? "/ " : "I");
            }
            m.texto(p, 1, y0 + 2, 0, "rayon", "south", 0xB9B2A2, l1.toString().trim(), l2.toString().trim());
        }
        if (p.azar(0.4)) {
            m.set(p, 4, y0 + 4, 4, C + "telarana");
        }
        // el foco del techo (lo prende el evento en el "¡pum!" del despertar)
        m.set(p, 2, y0 + 4, 2, C + "foco[encendido=false]");
        m.marca(p, "celda." + n + ".foco", 2, y0 + 4, 2, 0);
        // la cámara del despertar: empieza arriba en la esquina de los barrotes y baja hasta la cama
        m.marca(p, "celda." + n + ".cam1", 4, y0 + 3, 4, 0);
        m.marca(p, "celda." + n + ".cam2", 1, y0 + 2, 2, 0);
        // donde aparece: en medio, mirando a la pantalla
        m.marca(p, "celda." + n, 2, y0 + 1, 3, 180);
        // la esquina del interior (para las pistas y para saber si alguien sigue en la celda)
        p.marca("celda." + n + ".esquina", m.cx, y0 + 1, m.cz, m.yaw);
    }

    // ------------------------------------------------------------------ la torre del Coleccionista

    /** La torre del centro: 11 x 11 de acero negro hasta el techo, con una pantalla gigante en cada cara. */
    private static void torre(Plano p) {
        int x0 = 25, x1 = 35, z0 = 13, z1 = 23;
        for (int y = 1; y < TECHO; y++) {
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    boolean borde = x == x0 || x == x1 || z == z0 || z == z1;
                    if (!borde) {
                        p.set(x, y, z, NEGRO);
                        continue;
                    }
                    boolean esquina = (x == x0 || x == x1) && (z == z0 || z == z1);
                    p.set(x, y, z, esquina ? VIGA + "[axis=y]" : y == 1 || y == 11 ? METAL : NEGRO);
                }
            }
        }
        // sin pantallas grandes (el Coleccionista habla en la pantalla de cada celda, no en el patio): una cámara por cara, que mira a las celdas
        p.set(30, 6, z0 - 1, C + "camara[facing=north]");
        p.set(30, 6, z1 + 1, C + "camara[facing=south]");
        p.set(x0 - 1, 6, 18, C + "camara[facing=west]");
        p.set(x1 + 1, 6, 18, C + "camara[facing=east]");
        p.marca("pantalla_grande", x0 - 1, 6, 18, 90);
        p.marca("pantalla_grande.2", 30, 6, z0 - 1, 180);
        p.marca("pantalla_grande.3", 30, 6, z1 + 1, 0);
        p.marca("pantalla_grande.4", x1 + 1, 6, 18, 270);
        // al pie de cada cara: escalón de metal, candelabros y cortinas de terciopelo
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            p.set(x, 1, z0 - 1, C + "escalera_metal[facing=south]");
            p.set(x, 1, z1 + 1, C + "escalera_metal[facing=north]");
        }
        for (int z = z0; z <= z1; z++) {
            p.set(x0 - 1, 1, z, C + "escalera_metal[facing=east]");
            p.set(x1 + 1, 1, z, C + "escalera_metal[facing=west]");
        }
        // los candelabros, en el piso al pie del escalón (no encima: el escalón es medio bloque)
        for (int x : new int[] {x0 + 1, x1 - 1}) {
            p.set(x, 1, z0 - 2, C + "candelabro[facing=north,half=lower]");
            p.set(x, 2, z0 - 2, C + "candelabro[facing=north,half=upper]");
            p.set(x, 1, z1 + 2, C + "candelabro[facing=south,half=lower]");
            p.set(x, 2, z1 + 2, C + "candelabro[facing=south,half=upper]");
        }
        for (int z : new int[] {z0 + 1, z1 - 1}) {
            p.set(x0 - 2, 1, z, C + "candelabro[facing=west,half=lower]");
            p.set(x0 - 2, 2, z, C + "candelabro[facing=west,half=upper]");
            p.set(x1 + 2, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(x1 + 2, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        // bocinas y cámaras arriba en las esquinas de la torre
        p.set(x0 - 1, 11, z0 - 1, C + "camara[facing=north]");
        p.set(x1 + 1, 11, z1 + 1, C + "camara[facing=south]");
        p.set(x0 - 1, 11, z1 + 1, C + "bocina[facing=west]");
        p.set(x1 + 1, 11, z0 - 1, C + "bocina[facing=east]");
    }

    // ------------------------------------------------------------------ techo: vigas y tuberías

    private static void techo(Plano p) {
        for (int x = XA + 2; x <= XB; x += 6) {
            for (int z = ZA; z <= ZB; z++) {
                p.set(x, TECHO - 1, z, VIGA + "[axis=z]");
            }
        }
        for (int x = XA; x <= XB; x++) {
            if (Math.floorMod(x - XA - 2, 6) != 0) {
                p.set(x, TECHO - 1, ZA + 1, C + "tubo[axis=x]");
                p.set(x, TECHO - 1, ZB - 1, C + "tubo[axis=x]");
            }
        }
    }

    // ------------------------------------------------------------------ el portón y lo del guardia

    /** El portón de barrotes del este: sube (el evento lo abre) y del otro lado empieza la sala grande. */
    private static void porton(Plano p) {
        for (int z = PORTON_Z0; z <= PORTON_Z1; z++) {
            for (int y = 1; y <= PORTON_ALTO; y++) {
                p.set(PORTON_X, y, z, BARROTES);
            }
            p.set(PORTON_X, PORTON_ALTO + 1, z, VIGA + "[axis=z]");
            p.set(PORTON_X, 0, z, METAL);
            p.marca("porton." + (z - PORTON_Z0), PORTON_X, 1, z, 270);
        }
        // el marco del portón: columnas de acero y una lámpara roja arriba
        for (int y = 1; y <= PORTON_ALTO + 1; y++) {
            p.set(PORTON_X, y, PORTON_Z0 - 1, VIGA + "[axis=y]");
            p.set(PORTON_X, y, PORTON_Z1 + 1, VIGA + "[axis=y]");
        }
        p.marca("salida", PORTON_X - 1, 1, 18, 270);
        p.texto(PORTON_X - 1, PORTON_ALTO + 2, 18, "letrero", "west", 0xB3272A, "LA COLECCIÓN", "continúa");
        p.set(PORTON_X - 1, PORTON_ALTO + 4, 18, C + "reloj[facing=west]");
        // el reglamento a los lados del portón
        p.letrero(PORTON_X - 1, 3, 13, "", "west", "red", "REGLAMENTO", "1. Silencio.", "2. No tocar las rejas.", "3. Obedecer.");
        p.letrero(PORTON_X - 1, 3, 23, "", "west", "white", "4. No hay visitas.", "5. No hay salida.", "6. Ustedes son", "   la colección.");
        p.texto(PORTON_X - 1, 2, 25, "rayon", "west", 0x9A958A, "IIII IIII", "IIII IIII", "IIII III");
    }

    /** Las esquinas del este: el escritorio del guardia, casilleros, una camilla y una silla de ruedas abandonadas. */
    private static void guardia(Plano p) {
        // (contra la pared del este, lejos de las puertas de las celdas)
        for (int z = ZA + 1; z <= ZA + 3; z++) {
            p.set(XB, 1, z, C + "archivero");
            p.set(XB, 2, z, C + "archivero");
        }
        p.letrero(XB, 3, ZA + 2, "", "west", "gray", "CASILLEROS", "PERSONAL");
        p.set(XB - 1, 1, ZA + 5, C + "mesa[facing=west]");
        p.set(XB - 1, 2, ZA + 5, C + "papeles[facing=west]");
        p.set(XB, 1, ZA + 5, C + "cajon[facing=west]");
        p.set(XB, 4, ZA + 5, C + "lampara_jaula[facing=west,encendida=true,falla=false]");
        p.set(XB - 1, 1, ZB - 2, C + "camilla[facing=north]");
        p.set(XB, 1, ZB - 5, C + "silla_ruedas[facing=" + p.elegir("west", "north") + "]");
        for (int z = ZB - 4; z <= ZB - 2; z++) {
            p.set(XB, 1, z, C + "losa_tablon[type=bottom]");
        }
        // cámaras vigilando desde las esquinas del patio
        p.set(XA, 10, ZA, C + "camara[facing=south]");
        p.set(XA, 10, ZB, C + "camara[facing=north]");
        p.set(XB, 10, ZA, C + "camara[facing=west]");
        p.set(XB, 10, ZB, C + "camara[facing=west]");
    }

    // ------------------------------------------------------------------ luces

    private static void lucesPatio(Plano p) {
        // lámparas fluorescentes colgando sobre el patio, en tres filas (luz fría; algunas fallan)
        for (int x = XA + 2; x <= XB - 1; x += 4) {
            for (int z : new int[] {ZA + 3, 18, ZB - 3}) {
                if (x >= 24 && x <= 36 && z == 18) {
                    continue; // ahí está la torre
                }
                p.set(x, TECHO - 1, z, C + "tubo[axis=y]");
                p.set(x, TECHO - 2, z, C + "tubo[axis=y]");
                luz(p, x, TECHO - 3, z, C + "lampara_tubo[facing=north,encendida=true,falla=" + (p.azar(0.12) ? "true" : "false") + "]");
            }
        }
        // focos de jaula en las columnas de las celdas, mirando al patio
        for (int n = 1; n <= CELDAS; n++) {
            Marco m = marco(n);
            int[] b = m.b(-1, 6);
            if (b[0] >= XA && b[0] <= XB && b[1] >= ZA && b[1] <= ZB) {
                luz(p, b[0], 4, b[1], m.bloque(C + "lampara_jaula[facing=south,encendida=true,falla=false]"));
            }
        }
    }

    /** Luz invisible pareja a media altura (que se vea el patio sin quitarle lo tétrico). */
    private static void luzPareja(Plano p) {
        for (int x = XA + 2; x <= XB - 1; x += 6) {
            for (int z = ZA + 2; z <= ZB - 1; z += 6) {
                if (p.vacio(x, 5, z)) {
                    p.set(x, 5, z, "light[level=12]");
                }
            }
        }
    }

    private static void luz(Plano p, int x, int y, int z, String bloque) {
        p.set(x, y, z, bloque);
        p.marca("ala.luz." + luces++, x, y, z, 0);
    }
}
