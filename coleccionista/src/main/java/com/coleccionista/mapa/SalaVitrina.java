package com.coleccionista.mapa;

/**
 * El teatro de la colección: el final del capítulo 1. Diez cuartos de "salida" (copias idénticas de los de los circuitos, ver
 * {@link Salidas}) dan al teatro: cinco abajo y cinco en el palco de arriba. Cuando todos terminan, los que se salvaron pasan sentados
 * de su cuarto del circuito a su copia de aquí (sin que se note) y se les abre la puerta: frente a ellos, un vidrio enorme con un telón
 * rojo cerrado. Detrás del telón, el escenario con seis vitrinas, donde despierta el último equipo.
 * <pre>
 *   x: 0 ... 72   z: 0 ... 60   y: 0 ... 17
 *   cuartos z 2..8 (abajo y en el palco, y 7), la sala z 10..38, el vidrio en z 39, el telón en z 40, el escenario z 41..58,
 *   la salida en el muro del este (x 55) y la antesala detrás (x 56..70)
 * </pre>
 * Marcas: {@code cab.sal_tea_N.*} (los cuartos, N = 1..10), {@code vit.caja.N}, {@code vit.frente.N}, {@code vit.boquilla.N},
 * {@code vit.luz.N}, {@code vit.foco.N}, {@code vit.placa.N} y {@code vit.cae.N} (las vitrinas, 0..5), {@code vit.pantalla},
 * {@code vit.centro}, {@code vit.salida.a/.b} y {@code vit.antesala} (el pasillo de servicio {@code pas_tea}, ver {@link Pasaje}).
 */
public final class SalaVitrina {
    /** Tres vitrinas (en un equipo hay de 2 a 3 piezas y solo pierde un equipo), cerca del vidrio para que se vean bien. */
    public static final int VITRINAS = 3, CUARTOS = 10;
    /** El telón: de x 4 a 51 en z 40, de y 1 a 11. */
    public static final int TELON_X0 = 4, TELON_X1 = 51, TELON_Z = 40, TELON_Y1 = 11;
    /** La salida del teatro (muro del este) y hasta dónde llega la antesala. */
    public static final int SALIDA_X = 55, ANTESALA_X1 = 70;
    private static final String C = "coleccionista:";
    private static final int X1 = 54, TECHO = 16;

    private SalaVitrina() {
    }

    /** El marco del cuarto N del teatro (1..5 abajo, 6..10 en el palco, de oeste a este) y el piso donde está. */
    public static Marco marco(int n) {
        return new Marco(3 + 10 * ((n - 1) % 5), 2, 0);
    }

    /** En qué orden se llenan las vitrinas: primero la del centro. */
    public static int orden(int i) {
        return new int[] {1, 0, 2}[Math.max(0, Math.min(2, i))];
    }

    public static int piso(int n) {
        return n <= 5 ? 0 : 7;
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.caja(0, 0, 0, ANTESALA_X1 + 1, TECHO + 1, 60, C + "marmol_negro");
        // la sala: piso de parquet con pasillo de tapete, muros de papel tapiz con moldura dorada
        p.caja(1, 1, 10, X1, TECHO - 1, 38, "air");
        for (int x = 1; x <= X1; x++) {
            for (int z = 10; z <= 38; z++) {
                p.set(x, 0, z, C + (x >= 25 && x <= 30 ? (x == 25 || x == 30 ? "tapete_orilla" : "tapete_rojo") : "parquet"));
            }
        }
        for (int y = 1; y < TECHO; y++) {
            String b = y <= 2 ? C + "madera_pulida" : y == 3 ? C + "marco_dorado" : C + "papel_tapiz";
            for (int z = 10; z <= 38; z++) {
                p.set(0, y, z, b);
                p.set(X1 + 1, y, z, b);
            }
        }
        // los cuartos de la salida: cinco abajo y cinco en el palco
        for (int n = 1; n <= CUARTOS; n++) {
            Salidas.sala(p, "sal_tea_" + n, marco(n), piso(n));
            // el número sobre la puerta, por fuera
            p.texto(marco(n).cx + 3, piso(n) + 4, 10, "placa", "south", 0xD9A93A, "PALCO " + n);
        }
        palco(p);
        butacas(p);
        escenario(p);
        salida(p);
        luces(p);
        p.marca("vit.centro", 27, 1, 30, 0);
        return p;
    }

    /** El palco: el piso de arriba (y 7) frente a los cuartos 6..10, con barandal dorado y una escalera en cada orilla. */
    private static void palco(Plano p) {
        for (int x = 1; x <= X1; x++) {
            for (int z = 10; z <= 14; z++) {
                p.set(x, 7, z, C + "madera_pulida");
            }
            if (x >= 4 && x <= X1 - 3) {
                p.set(x, 8, 14, C + "barandal");
            }
        }
        // columnas que lo sostienen
        for (int x = 8; x <= X1 - 4; x += 10) {
            for (int y = 1; y <= 6; y++) {
                p.set(x, y, 14, C + "viga_madera[axis=y]");
            }
        }
        // las escaleras: de la orilla del palco hacia el escenario, en las dos esquinas
        for (int x : new int[] {1, 2, X1 - 1, X1}) {
            for (int i = 0; i < 7; i++) {
                int y = 7 - i, z = 15 + i;
                p.set(x, y, z, C + "escalera_tablon[facing=north]");
                for (int yy = 0; yy < y; yy++) {
                    p.set(x, yy, z, C + "madera_pulida");
                }
            }
            p.set(x, 8, 14, "air");
        }
        p.set(3, 8, 14, C + "barandal");
        p.set(X1 - 2, 8, 14, C + "barandal");
    }

    /** Filas de butacas de terciopelo frente al vidrio (adorno), con pasillos al centro y a los lados. */
    private static void butacas(Plano p) {
        for (int z = 20; z <= 32; z += 3) {
            for (int x = 5; x <= X1 - 4; x += 2) {
                if (x >= 23 && x <= 32) {
                    continue;
                }
                p.set(x, 1, z, C + "sillon[facing=south,half=lower]");
                p.set(x, 2, z, C + "sillon[facing=south,half=upper]");
            }
        }
    }

    /** El vidrio, el telón y el escenario con sus tres vitrinas. */
    private static void escenario(Plano p) {
        // el escenario
        p.caja(2, 1, 41, X1 - 1, TECHO - 1, 58, "air");
        for (int x = 2; x <= X1 - 1; x++) {
            for (int z = 41; z <= 58; z++) {
                p.set(x, 0, z, C + ((x + z) % 2 == 0 ? "marmol_crema" : "marmol_oro"));
            }
            for (int y = 1; y < TECHO; y++) {
                p.set(x, y, 59, C + (y <= 2 ? "madera_pulida" : "papel_tapiz"));
            }
        }
        // el vidrio con su marco dorado
        for (int x = TELON_X0 - 1; x <= TELON_X1 + 1; x++) {
            for (int y = 1; y <= TELON_Y1 + 1; y++) {
                boolean marco = x == TELON_X0 - 1 || x == TELON_X1 + 1 || y == TELON_Y1 + 1;
                p.set(x, y, 39, C + (marco ? "marco_dorado" : "vidrio_armado"));
            }
        }
        // el telón: terciopelo rojo con fleco dorado abajo (el evento lo abre del centro hacia los lados)
        for (int x = TELON_X0; x <= TELON_X1; x++) {
            for (int y = 1; y <= TELON_Y1; y++) {
                p.set(x, y, TELON_Z, C + (y == 1 ? "telon_borde" : "telon"));
            }
            p.set(x, TELON_Y1 + 1, TELON_Z, C + "telon_borde");
        }
        for (int n = 0; n < VITRINAS; n++) {
            vitrina(p, n);
        }
        // los tubos de la cera: uno grande que corre sobre las vitrinas (y 6) y sube al techo por las dos orillas
        for (int x = 18; x <= 38; x++) {
            p.set(x, 6, 44, C + "tubo[axis=x]");
        }
        for (int x : new int[] {18, 38}) {
            for (int y = 6; y < TECHO; y++) {
                p.set(x, y, 44, C + "tubo[axis=y]");
            }
        }
        // la pantalla del Coleccionista, colgada del techo justo detrás de las vitrinas (se ve bien desde las butacas), con sus cadenas
        p.set(28, 11, 48, C + "pantalla[facing=north,grande=true]");
        p.marca("vit.pantalla", 28, 11, 48, 180);
        for (int x : new int[] {23, 33}) {
            p.set(x, TECHO - 1, 48, C + "cadena");
        }
        for (int x : new int[] {6, 49}) {
            p.set(x, 1, 58, C + "candelabro[facing=north,half=lower]");
            p.set(x, 2, 58, C + "candelabro[facing=north,half=upper]");
        }
        for (int x = 2; x <= X1 - 1; x++) {
            if (x % 6 == 0) {
                p.set(x, 1, 58, C + "cortina[facing=north,half=lower]");
                p.set(x, 2, 58, C + "cortina[facing=north,half=upper]");
            }
        }
    }

    /** Una vitrina: caja de vidrio de 3 x 3 sobre una tarima, la boquilla de la cera arriba, el reflector y la placa con el nombre. */
    private static void vitrina(Plano p, int n) {
        int x0 = 19 + 8 * n, z0 = 43;
        for (int x = x0; x <= x0 + 2; x++) {
            for (int z = z0; z <= z0 + 2; z++) {
                p.set(x, 0, z, C + "metal_negro");
                p.set(x, 5, z, C + "metal_negro");
                for (int y = 1; y <= 4; y++) {
                    boolean borde = x == x0 || x == x0 + 2 || z == z0 || z == z0 + 2;
                    boolean esquina = (x == x0 || x == x0 + 2) && (z == z0 || z == z0 + 2);
                    p.set(x, y, z, esquina ? C + "viga[axis=y]" : borde ? C + "vidrio_armado" : "air");
                }
            }
        }
        // el frente (hacia el público) se queda abierto hasta que llega la silla: lo cierra el evento
        p.marca("vit.frente." + n, x0 + 1, 1, z0, 0);
        for (int y = 1; y <= 3; y++) {
            p.set(x0 + 1, y, z0, "air");
        }
        p.marca("vit.caja." + n, x0 + 1, 1, z0 + 1, 0);
        // el tubo de la cera sale del techo de la vitrina y se une al tubo grande que corre por arriba (ver escenario)
        p.marca("vit.boquilla." + n, x0 + 1, 4, z0 + 1, 0);
        p.set(x0 + 1, 5, z0 - 1, C + "lampara_jaula[facing=north,encendida=false,falla=false]");
        p.marca("vit.luz." + n, x0 + 1, 5, z0 - 1, 0);
        // el reflector de verdad (una luz invisible que el evento prende frente al vidrio cuando hay alguien dentro)
        p.marca("vit.foco." + n, x0 + 1, 3, z0 - 1, 0);
        p.texto(x0, 1, z0 - 1, "placa", "north", 0xD9A93A, "PIEZA", "—");
        p.marca("vit.placa." + n, x0, 1, z0 - 1, 0);
        // donde cae la estatua cuando se abre la vitrina
        p.marca("vit.cae." + n, x0 + 1, 1, z0 - 2, 180);
    }

    /**
     * La salida del teatro (en el muro del este, cerca del vidrio) y la antesala detrás: el pasillo de servicio ({@link Pasaje}), idéntico
     * al de la entrada de la mansión. Su puerta de atrás es la salida del teatro.
     */
    private static void salida(Plano p) {
        int x = SALIDA_X;
        Pasaje.pasillo(p, "pas_tea", new Marco(x + 1, 39, 270));
        p.marca("vit.salida.a", x, 1, 34, 270);
        p.marca("vit.salida.b", x, 1, 35, 270);
        p.texto(x - 1, 4, 34, "letrero", "west", 0xB3272A, "LA FUNCIÓN", "continúa");
        p.marca("vit.antesala", x + 6, 1, 34, 270);
    }

    /** Luz: la sala bien iluminada (arañas y luz pareja), el escenario en penumbra hasta que se prenden los reflectores. */
    private static void luces(Plano p) {
        for (int x = 4; x <= X1 - 2; x += 6) {
            for (int z = 12; z <= 37; z += 5) {
                if (p.vacio(x, 5, z)) {
                    p.set(x, 5, z, "light[level=12]");
                }
                if (p.vacio(x, 12, z)) {
                    p.set(x, 12, z, "light[level=11]");
                }
            }
            for (int z = 43; z <= 57; z += 7) {
                p.set(x, 10, z, "light[level=7]");
            }
        }
        for (int x = 9; x <= X1 - 4; x += 12) {
            p.set(x, TECHO - 1, 26, C + "cadena");
            p.set(x, TECHO - 2, 26, C + "arana");
        }
        p.set(1, 13, 37, C + "camara[facing=east]");
        p.set(X1, 13, 37, C + "camara[facing=west]");
    }
}
