package com.coleccionista.mapa;

import java.util.ArrayList;
import java.util.List;

/**
 * El circuito de un equipo en la prueba de los sentidos (uno por equipo, todos iguales y lejos entre sí para que no se vean ni se oigan).
 * "El taller del Coleccionista": cuartos unidos por pasillos oscuros que dan vueltas (nunca se ve lo que sigue):
 * <ol>
 *     <li>El ascensor falso (idéntico al de la sala grande) donde llegan.</li>
 *     <li>El taller: la mesa con la venda, la cinta y las esposas; cada uno toma un objeto (en parejas solo hay venda y esposas).</li>
 *     <li>El pasillo de las muñecas (cuatro vueltas) y el cuarto de los postes, donde la cadena se enreda.</li>
 *     <li>Otro pasillo con vueltas y la pasarela: una zanja de lodo (no muy honda: nadie se lastima) con una tabla angosta en zigzag. Si
 *     alguien cae, todo el equipo regresa al principio de la pasarela.</li>
 *     <li>Las baldosas: un campo largo; el camino seguro se ve unos segundos y nunca va en diagonal (de una baldosa a la de enfrente o a
 *     la de al lado). Si alguien pisa una mala, todo el equipo regresa a la orilla (lejos de todo peligro).</li>
 *     <li>El cuarto a oscuras: sin una sola luz, con muros de muebles que lo vuelven un zigzag y muñecas grandes en el camino (cambian
 *     de lugar en cada equipo). Solo el de las esposas lleva farol: ve, guía con la voz y no toca nada. Si alguien choca con una muñeca,
 *     susto y todo el equipo regresa a la entrada del cuarto.</li>
 *     <li>La salida ({@link Salidas}): tres sillas frente a una puerta de SALIDA.</li>
 * </ol>
 * Sin vacíos: en ningún lado se puede caer más de dos bloques. Marcas con {@code circ.K.} (K = número del circuito); el ascensor es
 * {@code cab.circ_K} y el cuarto de la salida {@code sal_circ_K}.
 */
public final class Circuito {
    public static final int CIRCUITOS = 10;
    private static final String C = "coleccionista:";
    private static final String MURO = C + "ladrillo_oscuro", PISO = C + "tablon", TECHO_B = C + "viga_madera";
    /** Hasta dónde llega (z local) cada parte: al pasar todos de ahí, empieza la siguiente. */
    public static final double Z_PASARELA = 58.5, Z_PASARELA_FIN = 71.2, Z_BALD_FIN = 100.2;
    /** La zanja de la pasarela (x, z locales) y su piso. */
    public static final int ZANJA_X0 = 32, ZANJA_X1 = 49, ZANJA_Z0 = 61, ZANJA_Z1 = 70;
    /** Las baldosas: 6 de ancho (x 24..29) por 12 filas (z 88..99). */
    public static final int BALD_X0 = 24, BALD_Z0 = 88, BALD_ANCHO = 6, BALD_FILAS = 12;
    /** El cuarto a oscuras: su interior (x, z locales). Tres muros de muebles (en z 115, 119 y 123) lo vuelven un zigzag de 4 filas. */
    public static final int OSC_X0 = 30, OSC_X1 = 49, OSC_Z0 = 112, OSC_Z1 = 125;
    /** Al pasar todos de esta z (local) ya están en la última fila, frente a la salida: se abre la puerta. */
    public static final double Z_OSCURO_FIN = 124.0;
    /**
     * Los tramos del camino donde se ponen las muñecas: {z0, z1, x0, x1} (fila del cuarto y las columnas x donde puede ir una). Lejos de
     * la entrada, de las vueltas y de la salida, para que siempre se pueda pasar por un lado.
     */
    public static final int[][] OSC_TRAMOS = {{112, 114, 34, 35}, {116, 118, 35, 44}, {120, 122, 35, 44}, {124, 125, 34, 36}};
    /** Los rincones sin salida (la punta este de la primera y la última fila): ahí también espera alguna muñeca. */
    public static final int[][] OSC_RINCONES = {{112, 114, 44, 48}, {124, 125, 43, 48}};
    /** El cuarto de la salida: su marco (la esquina de su interior). */
    public static final int SAL_CX = 22, SAL_CZ = 134;

    private Circuito() {
    }

    /** Los postes donde se enreda la cadena (centros x, z locales): en el cuarto de los postes, en hileras chuecas. */
    public static List<double[]> postes() {
        List<double[]> l = new ArrayList<>();
        int fila = 0;
        for (int z = 39; z <= 49; z += 3) {
            for (int x = (fila % 2 == 0 ? 5 : 7); x <= 24; x += 4) {
                // la entrada (x 12..15 en z 36..38) y la salida (x 23..26 en z 45..50) quedan libres
                if (x >= 22 && z >= 44) {
                    continue;
                }
                int j = (x * 7 + z * 13) % 3 - 1;
                l.add(new double[] {x + 0.5 + j * 0.0, z + 0.5});
            }
            fila++;
        }
        return l;
    }

    /** El camino de la pasarela (bloques x, z locales), de la orilla norte a la sur. */
    public static List<int[]> pasarela() {
        List<int[]> l = new ArrayList<>();
        int x = 41, z = ZANJA_Z0;
        int[][] tramos = {{41, 62}, {46, 62}, {46, 65}, {35, 65}, {35, 68}, {40, 68}, {40, ZANJA_Z1}};
        for (int[] t : tramos) {
            while (x != t[0] || z != t[1]) {
                l.add(new int[] {x, z});
                if (x != t[0]) {
                    x += Integer.signum(t[0] - x);
                } else {
                    z += Integer.signum(t[1] - z);
                }
            }
        }
        l.add(new int[] {x, z});
        return l;
    }

    /** Los puntos por donde va el indicador de "ir acá" (x, y, z locales), en orden, de la mesa a la salida. */
    public static List<double[]> camino() {
        List<double[]> l = new ArrayList<>();
        double y = 1.6;
        double[][] p = {{10, 15.5}, {10, 19.5}, {24, 20}, {24, 30}, {14, 30}, {14, 37}, {25, 47.5}, {32, 47.5}, {32, 38}, {42, 38},
                {42, 59}, {41.5, 61}, {41.5, 72}, {42, 77.5}, {28, 77.5}, {28, 86}, {27, 100.5}, {27, 105}, {40, 106.5}, {40, 112},
                {39.5, 113.5}, {32, 113.5}, {32, 117.5}, {47.5, 117.5}, {47.5, 121.5}, {32, 121.5}, {32, 124.8}, {40, 124.8}, {40, 127},
                {40, 128.5}, {26, 128.5}, {26, 133}};
        for (double[] q : p) {
            l.add(new double[] {q[0], y, q[1]});
        }
        return l;
    }

    public static Plano crear(long semilla, int k) {
        Plano p = new Plano(semilla + k);
        String pre = "circ." + k + ".";
        // casco: todo macizo y luego cada cuarto y pasillo se vacía
        p.caja(0, -3, -1, 51, 8, 143, MURO);
        // el ascensor falso: mismo marco que uno del sur de la sala grande, girado para que su puerta dé al taller
        Cabinas.ascensor(p, "circ_" + k, new Marco(12, 4, 180));
        // el taller
        cuarto(p, 3, 5, 16, 14, 15);
        mesa(p, pre);
        puerta(p, pre + "puerta1", 9, 15);
        // el pasillo de las muñecas: cuatro vueltas
        pasillo(p, 8, 16, 11, 21);
        pasillo(p, 8, 18, 25, 21);
        pasillo(p, 22, 18, 25, 31);
        pasillo(p, 12, 28, 25, 31);
        pasillo(p, 12, 28, 15, 35);
        munecas(p, 8, 16, 11, 21);
        munecas(p, 22, 18, 25, 31);
        munecas(p, 12, 28, 25, 31);
        // los postes
        cuarto(p, 3, 36, 26, 51, 13);
        postes(p, pre);
        // otro pasillo con vueltas, hasta la pasarela
        pasillo(p, 27, 46, 33, 49);
        pasillo(p, 30, 36, 33, 49);
        pasillo(p, 30, 36, 43, 39);
        pasillo(p, 40, 36, 43, 57);
        munecas(p, 30, 36, 33, 49);
        munecas(p, 40, 36, 43, 57);
        // la pasarela
        cuarto(p, ZANJA_X0, 58, ZANJA_X1, 73, 12);
        pasarela(p, pre);
        // pasillo a las baldosas
        pasillo(p, 40, 74, 43, 79);
        pasillo(p, 26, 76, 43, 79);
        pasillo(p, 26, 76, 29, 83);
        munecas(p, 26, 76, 43, 79);
        // las baldosas
        baldosas(p, pre);
        puerta(p, pre + "puerta3", 26, 104);
        // pasillo al cuarto a oscuras (cada vez con menos luz)
        pasillo(p, 25, 105, 28, 108);
        pasillo(p, 25, 105, 41, 108);
        pasillo(p, 38, 105, 41, 111);
        munecas(p, 25, 105, 41, 108);
        for (int x = 38; x <= 41; x++) {
            for (int z = 108; z <= 111; z++) {
                if (p.get(x, 4, z) != null && p.get(x, 4, z).startsWith("light")) {
                    p.set(x, 4, z, "air");
                }
            }
        }
        // el cuarto a oscuras
        oscuro(p, pre);
        puerta(p, pre + "puerta4", 39, 126);
        // pasillo a la salida
        pasillo(p, 38, 127, 41, 130);
        pasillo(p, 24, 127, 41, 130);
        pasillo(p, 24, 127, 27, 132);
        // la salida (el cuarto idéntico al del teatro)
        Salidas.sala(p, "sal_circ_" + k, new Marco(SAL_CX, SAL_CZ, 0), 0);
        p.marca(pre + "salida", SAL_CX + 4, 1, SAL_CZ + 2, 0);
        return p;
    }

    /** Un cuarto vacío con piso de tablón, muros de ladrillo oscuro y vigas en el techo (alto 5), con su luz. */
    private static void cuarto(Plano p, int x0, int z0, int x1, int z1, int luz) {
        p.caja(x0, 1, z0, x1, 5, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 0, z, PISO);
                if (z % 4 == 0) {
                    p.set(x, 6, z, TECHO_B + "[axis=x]");
                }
            }
        }
        // velas en el piso junto a las paredes y una luz pareja (invisible) para que no quede negro
        for (int z = z0 + 1; z <= z1; z += 4) {
            p.set(x0, 1, z, C + "vela");
            p.set(x1, 1, z, C + "vela");
            for (int x = x0 + 1; x <= x1 - 1; x += 4) {
                p.set(x, 4, z, "light[level=" + luz + "]");
            }
        }
    }

    /** Un tramo de pasillo (de 4 de ancho): más bajo y más oscuro que los cuartos, con velas cada tanto. */
    private static void pasillo(Plano p, int x0, int z0, int x1, int z1) {
        p.caja(x0, 1, z0, x1, 4, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 0, z, C + "parquet");
                if ((x + z) % 5 == 0) {
                    p.set(x, 4, z, "light[level=9]");
                }
            }
        }
    }

    /**
     * Repisas con muñecas de porcelana a los dos lados de un tramo de pasillo (las que ya no le gustaron al Coleccionista), relojes y algún
     * espejo. Solo donde hay pared (no tapan las vueltas).
     */
    private static void munecas(Plano p, int x0, int z0, int x1, int z1) {
        boolean largoX = x1 - x0 > z1 - z0;
        if (largoX) {
            for (int x = x0 + 1; x < x1; x += 3) {
                repisa(p, x, z0 - 1, x, z0, "south");
                repisa(p, x + 1, z1 + 1, x + 1, z1, "north");
            }
        } else {
            for (int z = z0 + 1; z < z1; z += 3) {
                repisa(p, x0 - 1, z, x0, z, "east");
                repisa(p, x1 + 1, z + 1, x1, z + 1, "west");
            }
        }
    }

    /** Una repisa en el muro (mx, mz) con una muñeca encima mirando al pasillo (en el bloque ax, az de junto). */
    private static void repisa(Plano p, int mx, int mz, int ax, int az, String mira) {
        // arriba de la cabeza (la repisa en y 3, la muñeca en y 4): nadie se pega con ella
        String muro = p.get(mx, 3, mz);
        if (muro == null || !muro.equals(MURO) || !p.vacio(ax, 3, az) || !p.vacio(ax, 4, az)) {
            return;
        }
        p.set(ax, 3, az, C + "losa_madera[type=top]");
        if (p.azar(0.75)) {
            p.set(ax, 4, az, C + "muneca[facing=" + mira + "]");
        } else if (p.azar(0.5)) {
            p.set(ax, 4, az, C + "vela");
        }
    }

    /** Una puerta de reja de dos hojas (x, x+1) en el muro z, que abre el evento. */
    private static void puerta(Plano p, String marca, int x, int z) {
        for (int xx = x - 3; xx <= x + 4; xx++) {
            for (int y = 1; y <= 5; y++) {
                if (p.vacio(xx, y, z)) {
                    p.set(xx, y, z, MURO);
                }
            }
        }
        p.set(x, 0, z, PISO);
        p.set(x + 1, 0, z, PISO);
        p.set(x, 1, z, C + "puerta_celda[facing=south,half=lower,hinge=left,open=false]");
        p.set(x, 2, z, C + "puerta_celda[facing=south,half=upper,hinge=left,open=false]");
        p.set(x + 1, 1, z, C + "puerta_celda[facing=south,half=lower,hinge=right,open=false]");
        p.set(x + 1, 2, z, C + "puerta_celda[facing=south,half=upper,hinge=right,open=false]");
        p.set(x, 3, z, C + "viga[axis=x]");
        p.set(x + 1, 3, z, C + "viga[axis=x]");
        p.marca(marca + ".a", x, 1, z, 0);
        p.marca(marca + ".b", x + 1, 1, z, 0);
    }

    private static void mesa(Plano p, String pre) {
        // tres mesitas con su objeto encima (venda, cinta, esposas)
        String[] obj = {"venda", "cinta", "esposas"};
        for (int i = 0; i < 3; i++) {
            int x = 6 + 3 * i, z = 10;
            p.set(x, 1, z, C + "mesa[facing=north]");
            p.set(x, 2, z, C + "objeto_" + obj[i] + "[facing=north]");
            p.marca(pre + "objeto." + i, x, 2, z, 0);
            p.texto(x, 1, z - 1, "placa", "north", 0xE8E2D0, obj[i].toUpperCase());
        }
        // donde aparecen si hay que acomodarlos (frente a la mesa)
        for (int i = 0; i < 3; i++) {
            p.marca(pre + "lugar." + i, 8 + i, 1, 7, 0);
        }
        // la pantalla chica del Coleccionista, el letrero y unas repisas con muñecas
        p.set(3, 3, 10, C + "pantalla[facing=east]");
        p.marca(pre + "pantalla", 3, 3, 10, 270);
        p.letrero(16, 3, 9, "", "west", "red", "TOMEN UN", "OBJETO", "CADA UNO");
        p.letrero(16, 3, 11, "", "west", "white", "VENDA: no ves", "CINTA: no hablas", "ESPOSAS: no tocas");
        p.set(16, 4, 6, C + "camara[facing=west]");
        for (int z : new int[] {6, 13}) {
            p.set(3, 3, z, C + "losa_madera[type=top]");
            p.set(3, 4, z, C + "muneca[facing=east]");
            p.set(16, 3, z, C + "losa_madera[type=top]");
            p.set(16, 4, z, C + "muneca[facing=west]");
        }
        // las cosas del taller: la máquina de coser, un maniquí a medio vestir, baúles y maletas
        p.set(4, 1, 13, C + "maquina_coser[facing=east]");
        p.set(15, 1, 13, C + "maniqui[facing=west,half=lower]");
        p.set(15, 2, 13, C + "maniqui[facing=west,half=upper]");
        p.set(4, 1, 6, C + "cajon[facing=east]");
        p.set(15, 1, 6, C + "maleta[facing=west]");
        p.set(14, 1, 14, C + "papeles");
        // el taller: dentro del cuarto (todos fuera del ascensor) es donde explica el Coleccionista
        p.marca(pre + "taller.a", 3, 1, 5, 0);
        p.marca(pre + "taller.b", 16, 1, 14, 0);
    }

    private static void postes(Plano p, String pre) {
        int n = 0;
        for (double[] q : postes()) {
            int x = (int) Math.floor(q[0]), z = (int) Math.floor(q[1]);
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, z, C + "poste[axis=y]");
            }
            p.marca(pre + "poste." + n++, x, 1, z, 0);
        }
        p.marca(pre + "ck.postes", 13, 1, 37, 0);
        p.letrero(3, 3, 37, "", "east", "white", "No se", "suelten.");
        // salida del cuarto: un hueco en el muro del este
        p.caja(27, 1, 46, 27, 4, 49, "air");
        for (int z = 46; z <= 49; z++) {
            p.set(27, 0, z, C + "parquet");
        }
    }

    private static void pasarela(Plano p, String pre) {
        // la zanja: dos bloques de hondo, con lodo al fondo (nadie se lastima, pero al caer regresan todos)
        for (int x = ZANJA_X0; x <= ZANJA_X1; x++) {
            for (int z = ZANJA_Z0; z <= ZANJA_Z1; z++) {
                if (!p.vacio(x, 1, z)) {
                    p.set(x, 1, z, "air");
                }
                p.set(x, 0, z, "air");
                p.set(x, -1, z, "air");
                p.set(x, -2, z, C + "lodo");
            }
        }
        for (int[] b : pasarela()) {
            p.set(b[0], 0, b[1], C + "losa_tablon[type=top]");
        }
        // cadenas colgando y poca luz encima de la zanja
        for (int z = ZANJA_Z0 + 1; z <= ZANJA_Z1; z += 4) {
            p.set(34, 5, z, C + "cadena");
            p.set(47, 5, z, C + "cadena");
        }
        p.marca(pre + "ck.pasarela", 41, 1, 59, 0);
        p.marca(pre + "zanja", ZANJA_X0, -1, ZANJA_Z0, 0);
        p.letrero(ZANJA_X0, 3, 59, "", "east", "red", "Si uno cae,", "regresan todos.");
        // la salida del cuarto: un hueco al sur
        p.caja(40, 1, 74, 43, 4, 74, "air");
    }

    private static void baldosas(Plano p, String pre) {
        // la orilla de arriba (de donde se mira), el campo entre muros y la orilla de abajo
        cuarto(p, 22, 84, 33, 87, 13);
        cuarto(p, 22, 100, 33, 103, 13);
        int x0 = BALD_X0, x1 = BALD_X0 + BALD_ANCHO - 1;
        p.caja(x0, 1, BALD_Z0, x1, 5, BALD_Z0 + BALD_FILAS - 1, "air");
        for (int z = BALD_Z0; z < BALD_Z0 + BALD_FILAS; z++) {
            for (int x = x0; x <= x1; x++) {
                p.set(x, 0, z, C + "marco_luz_apagado");
            }
            if ((z - BALD_Z0) % 3 == 1) {
                p.set(x0 + 2, 4, z, "light[level=12]");
            }
        }
        p.marca(pre + "baldosa.0.0", x0, 0, BALD_Z0, 0);
        p.marca(pre + "ck.baldosas", 27, 1, 85, 0);
        p.letrero(23, 3, 84, "", "south", "white", "Mírenlo bien.", "Solo se ve", "unos segundos.");
        p.letrero(32, 3, 87, "", "north", "red", "Ni en diagonal", "ni a ciegas.");
        // la palanca para ver el camino otra vez (la jala quien tenga manos)
        p.set(31, 2, 84, C + "palanca[face=wall,facing=south,powered=false]");
        p.marca(pre + "ver", 31, 2, 84, 0);
        // la entrada desde el pasillo
        p.caja(26, 1, 83, 29, 4, 83, "air");
    }

    /**
     * El cuarto a oscuras: ni una sola luz. Tres muros de muebles hasta el techo (libreros, archiveros, barriles) lo vuelven un zigzag de
     * cuatro filas de 3 de ancho: se entra a la mitad de la primera, las vueltas van al oeste, al este y al oeste, y la salida queda a la
     * mitad de la última. En las puntas sin salida hay juguetes y muebles viejos. Las muñecas grandes las pone el evento (cambian de
     * lugar con cada equipo), ver {@link #OSC_TRAMOS}.
     */
    private static void oscuro(Plano p, String pre) {
        p.caja(OSC_X0, 1, OSC_Z0, OSC_X1, 4, OSC_Z1, "air");
        for (int x = OSC_X0; x <= OSC_X1; x++) {
            for (int z = OSC_Z0; z <= OSC_Z1; z++) {
                p.set(x, 0, z, C + "parquet");
                if (z % 3 == 1) {
                    p.set(x, 5, z, TECHO_B + "[axis=x]");
                }
            }
        }
        muroMuebles(p, 34, OSC_X1, 115);
        muroMuebles(p, OSC_X0, 45, 119);
        muroMuebles(p, 34, OSC_X1, 123);
        // las puntas sin salida: el cuarto de juguetes olvidado
        p.set(49, 1, 112, C + "ropero[facing=west,half=lower]");
        p.set(49, 2, 112, C + "ropero[facing=west,half=upper]");
        p.set(49, 1, 113, C + "cuna[facing=west]");
        p.set(49, 1, 114, C + "caballito[facing=west]");
        p.set(49, 1, 124, C + "maniqui[facing=west,half=lower]");
        p.set(49, 2, 124, C + "maniqui[facing=west,half=upper]");
        p.set(49, 1, 125, C + "cajon[facing=west]");
        p.set(49, 2, 125, C + "muneca[facing=west]");
        // retratos de niñas y relojes (arriba de la cabeza: nadie se pega con ellos), telarañas en las esquinas
        p.set(OSC_X0, 3, 113, C + "retrato_nina[facing=east]");
        p.set(OSC_X0, 3, 121, C + "retrato_nina[facing=east]");
        p.set(OSC_X1, 3, 117, C + "retrato_nina[facing=west]");
        p.set(40, 3, 116, C + "reloj[facing=south]");
        p.set(37, 3, 122, C + "reloj[facing=north]");
        for (int[] q : new int[][] {{OSC_X0, OSC_Z0}, {OSC_X1, OSC_Z0}, {OSC_X0, OSC_Z1}, {OSC_X1, OSC_Z1}, {OSC_X0, 118}, {OSC_X1, 120}}) {
            p.set(q[0], 4, q[1], C + "telarana");
        }
        p.letrero(38, 3, 109, "", "east", "white", "Solo uno lleva luz.", "No toquen", "a las muñecas.");
        p.marca(pre + "ck.oscuro", 39, 1, 113, 0);
        p.marca(pre + "oscuro", OSC_X0, 1, OSC_Z0, 0);
        p.marca(pre + "ck.salida", 40, 1, 128, 0);
    }

    /** Un muro de muebles de un bloque de grueso hasta el techo (y 1 a 4) en la fila z, de x0 a x1: libreros, archiveros y barriles. */
    private static void muroMuebles(Plano p, int x0, int x1, int z) {
        for (int x = x0; x <= x1; x++) {
            String r = p.elegir("libros", "libros", "libros", "libros", "archivo", "archivo", "barril", "barril", "panel");
            String[] col;
            if (r.equals("libros")) {
                col = new String[] {"minecraft:bookshelf", "minecraft:bookshelf", "minecraft:bookshelf", "minecraft:bookshelf"};
            } else if (r.equals("archivo")) {
                col = new String[] {C + "estante_archivo", C + "estante_archivo", C + "estante_archivo", C + "panel_madera"};
            } else if (r.equals("barril")) {
                col = new String[] {"minecraft:barrel[facing=up]", "minecraft:barrel[facing=up]", "minecraft:bookshelf", "minecraft:bookshelf"};
            } else {
                col = new String[] {C + "panel_madera", C + "panel_madera", "minecraft:bookshelf", C + "panel_madera"};
            }
            for (int y = 1; y <= 4; y++) {
                p.set(x, y, z, col[y - 1]);
            }
        }
    }
}
