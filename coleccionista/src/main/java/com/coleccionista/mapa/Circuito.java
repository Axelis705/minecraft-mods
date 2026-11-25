package com.coleccionista.mapa;

import java.util.ArrayList;
import java.util.List;

/**
 * El circuito de un equipo en la prueba de los sentidos (uno por equipo, todos iguales y lejos entre sí para que no se vean ni se oigan).
 * "El taller del Coleccionista": un pasillo de cuartos, siempre hacia adelante (hacia el sur):
 * <ol>
 *     <li>El ascensor falso (idéntico al de la sala grande) donde llegan.</li>
 *     <li>La mesa: venda, cinta y esposas; cada uno toma un objeto (en parejas solo hay venda y esposas). Al tenerlos, la cadena.</li>
 *     <li>Los postes: un bosque de postes donde la cadena se enreda.</li>
 *     <li>El puente: un foso oscuro con una tabla angosta en zigzag. Si alguien cae, todo el equipo regresa al principio del puente.</li>
 *     <li>Las baldosas: el camino seguro se ve unos segundos; si alguien pisa una baldosa mala, trampa y de regreso al inicio.</li>
 *     <li>Las palancas: el tablero muestra arriba/abajo; quien tiene manos las acomoda y se abre la puerta.</li>
 *     <li>La salida: unas sillas ("tomen asiento"); cuando todos se sientan, las sillas se los llevan por el túnel.</li>
 * </ol>
 * <pre>
 *   x: 2 ... 17 (interior 3..16)   z: -1 ... 90   y: -8 ... 7
 * </pre>
 * Todas las marcas empiezan con {@code circ.K.} (K = número del circuito); el ascensor es {@code cab.circ_K}.
 */
public final class Circuito {
    public static final int CIRCUITOS = 10;
    private static final String C = "coleccionista:";
    private static final String MURO = C + "ladrillo_oscuro", PISO = C + "tablon", TECHO_B = C + "viga_madera";
    /** Las zonas (z) de cada cuarto. */
    public static final int Z_MESA0 = 5, Z_MESA1 = 14, Z_POSTES0 = 16, Z_POSTES1 = 31, Z_PUENTE0 = 32, Z_PUENTE1 = 45, Z_BALD0 = 47,
            Z_BALD1 = 54, Z_PAL0 = 56, Z_PAL1 = 65, Z_SAL0 = 67, Z_SAL1 = 76, Z_TUNEL1 = 88;
    /** Las baldosas: 6 de ancho (x 6..11 + 1) por 8 filas. */
    public static final int BALD_X0 = 6, BALD_ANCHO = 6, BALD_FILAS = 8;
    public static final int PALANCAS = 5;

    private Circuito() {
    }

    /** Los postes donde se enreda la cadena (centros x, z locales). */
    public static List<double[]> postes() {
        List<double[]> l = new ArrayList<>();
        int fila = 0;
        for (int z = Z_POSTES0 + 2; z <= Z_POSTES1 - 2; z += 3) {
            for (int x = (fila % 2 == 0 ? 5 : 6); x <= 15; x += 3) {
                l.add(new double[] {x + 0.5, z + 0.5});
            }
            fila++;
        }
        return l;
    }

    /** El camino del puente (bloques x, z locales), de la orilla norte a la sur. */
    public static List<int[]> puente() {
        List<int[]> l = new ArrayList<>();
        int x = 9, z = Z_PUENTE0;
        int[][] tramos = {{9, 35}, {13, 35}, {13, 39}, {6, 39}, {6, 42}, {10, 42}, {10, Z_PUENTE1}};
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

    public static Plano crear(long semilla, int k) {
        Plano p = new Plano(semilla + k);
        String pre = "circ." + k + ".";
        // casco: todo macizo y luego cada cuarto se vacía
        p.caja(2, 0, -1, 17, 7, Z_TUNEL1 + 1, MURO);
        // el ascensor falso: mismo marco que uno del sur de la sala grande, girado para que su puerta dé al cuarto de la mesa
        Cabinas.ascensor(p, "circ_" + k, new Marco(12, 4, 180));
        cuarto(p, 3, Z_MESA0, 16, Z_MESA1);
        mesa(p, pre);
        puerta(p, pre + "puerta1", Z_MESA1 + 1);
        cuarto(p, 3, Z_POSTES0, 16, Z_POSTES1);
        postes(p, pre);
        cuarto(p, 3, Z_PUENTE0, 16, Z_PUENTE1);
        puente(p, pre);
        cuarto(p, 3, Z_PUENTE1 + 1, 16, Z_BALD1 + 1);
        baldosas(p, pre);
        puerta(p, pre + "puerta3", Z_PAL0);
        cuarto(p, 3, Z_PAL0 + 1, 16, Z_PAL1);
        palancas(p, pre);
        puerta(p, pre + "puerta4", Z_PAL1 + 1);
        cuarto(p, 3, Z_SAL0, 16, Z_SAL1);
        salida(p, pre);
        return p;
    }

    /** Un cuarto vacío con piso de tablón, muros de ladrillo oscuro y vigas en el techo (alto 5). */
    private static void cuarto(Plano p, int x0, int z0, int x1, int z1) {
        p.caja(x0, 1, z0, x1, 5, z1, "air");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                p.set(x, 0, z, PISO);
                if (z % 4 == 0) {
                    p.set(x, 6, z, TECHO_B + "[axis=x]");
                }
            }
        }
        // una luz baja cada tantos bloques (con velas en las paredes)
        for (int z = z0 + 1; z <= z1; z += 4) {
            p.set(x0, 3, z, C + "vela");
            p.set(x1, 3, z, C + "vela");
            p.set((x0 + x1) / 2, 5, z, "light[level=9]");
        }
    }

    /** Una puerta de reja de dos hojas que se abre con el evento, en x 9..10 del muro z. */
    private static void puerta(Plano p, String marca, int z) {
        for (int x = 3; x <= 16; x++) {
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, z, MURO);
            }
        }
        p.set(9, 1, z, C + "puerta_celda[facing=south,half=lower,hinge=left,open=false]");
        p.set(9, 2, z, C + "puerta_celda[facing=south,half=upper,hinge=left,open=false]");
        p.set(10, 1, z, C + "puerta_celda[facing=south,half=lower,hinge=right,open=false]");
        p.set(10, 2, z, C + "puerta_celda[facing=south,half=upper,hinge=right,open=false]");
        p.set(9, 3, z, C + "viga[axis=x]");
        p.set(10, 3, z, C + "viga[axis=x]");
        p.marca(marca + ".a", 9, 1, z, 0);
        p.marca(marca + ".b", 10, 1, z, 0);
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
        // la pantalla chica del Coleccionista y el letrero
        p.set(3, 3, 10, C + "pantalla[facing=east]");
        p.letrero(16, 3, 9, "", "west", "red", "TOMEN UN", "OBJETO", "CADA UNO");
        p.letrero(16, 3, 11, "", "west", "white", "VENDA: no ves", "CINTA: no hablas", "ESPOSAS: no tocas");
        p.set(16, 4, 6, C + "camara[facing=west]");
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
        p.marca(pre + "ck.postes", 9, 1, Z_POSTES0 + 1, 0);
        p.letrero(3, 3, Z_POSTES0 + 1, "", "east", "white", "No se", "suelten.");
    }

    private static void puente(Plano p, String pre) {
        // el foso: sin piso, muy hondo y oscuro, con lodo al fondo
        p.caja(3, -7, Z_PUENTE0, 16, 0, Z_PUENTE1, "air");
        p.caja(2, -8, Z_PUENTE0 - 1, 17, -8, Z_PUENTE1 + 1, C + "lodo");
        for (int z = Z_PUENTE0; z <= Z_PUENTE1; z++) {
            for (int y = -7; y <= 0; y++) {
                p.set(2, y, z, MURO);
                p.set(17, y, z, MURO);
            }
        }
        for (int x = 3; x <= 16; x++) {
            for (int y = -7; y <= 0; y++) {
                p.set(x, y, Z_PUENTE0 - 1, MURO);
                p.set(x, y, Z_PUENTE1 + 1, MURO);
            }
        }
        for (int[] b : puente()) {
            p.set(b[0], 0, b[1], C + "losa_tablon[type=top]");
        }
        // cuerdas colgando y casi nada de luz
        for (int z = Z_PUENTE0 + 2; z <= Z_PUENTE1; z += 5) {
            p.set(4, 5, z, C + "cadena");
            p.set(15, 5, z, C + "cadena");
        }
        p.marca(pre + "ck.puente", 9, 1, Z_PUENTE0 - 1, 0);
        p.marca(pre + "foso", 3, -1, Z_PUENTE0, 0);
        p.letrero(3, 3, Z_PUENTE0 - 1, "", "east", "red", "Si uno cae,", "caen todos.");
    }

    private static void baldosas(Plano p, String pre) {
        // muros a los lados del campo (no se puede rodear) y las baldosas apagadas
        int z0 = Z_BALD0, x0 = BALD_X0, x1 = BALD_X0 + BALD_ANCHO - 1;
        for (int z = z0; z < z0 + BALD_FILAS; z++) {
            for (int y = 1; y <= 5; y++) {
                for (int x = 3; x < x0; x++) {
                    p.set(x, y, z, MURO);
                }
                for (int x = x1 + 1; x <= 16; x++) {
                    p.set(x, y, z, MURO);
                }
            }
            for (int x = x0; x <= x1; x++) {
                p.set(x, 0, z, C + "marco_luz_apagado");
            }
        }
        p.marca(pre + "baldosa.0.0", x0, 0, z0, 0);
        p.marca(pre + "ck.baldosas", 9, 1, Z_PUENTE1 + 1, 0);
        p.letrero(5, 3, Z_PUENTE1 + 1, "", "north", "white", "Mírenlo bien.", "Solo se ve", "unos segundos.");
        // el botón para ver el camino otra vez (lo aprieta quien tenga manos)
        p.set(12, 2, Z_PUENTE1 + 1, C + "palanca[face=wall,facing=north,powered=false]");
        p.marca(pre + "ver", 12, 2, Z_PUENTE1 + 1, 0);
    }

    private static void palancas(Plano p, String pre) {
        // las palancas en la pared del este, el tablero (cómo deben quedar) en la del oeste
        for (int i = 0; i < PALANCAS; i++) {
            int z = Z_PAL0 + 1 + 2 * i;
            p.set(16, 2, z, C + "palanca[face=wall,facing=west,powered=false]");
            p.marca(pre + "palanca." + i, 16, 2, z, 0);
            p.set(3, 3, z, C + "marco_luz_apagado");
            p.marca(pre + "tablero." + i, 3, 3, z, 0);
            p.texto(3, 2, z, "placa", "east", 0xE8E2D0, String.valueOf(i + 1));
            p.texto(16, 3, z, "placa", "west", 0xE8E2D0, String.valueOf(i + 1));
        }
        p.letrero(3, 4, Z_PAL0 + 6, "", "east", "white", "VERDE: arriba", "ROJO: abajo");
        p.marca(pre + "ck.palancas", 9, 1, Z_PAL0 + 1, 0);
    }

    private static void salida(Plano p, String pre) {
        // el letrero de "SALIDA" encendido, la tarima con las sillas y la cortina de metal que da al túnel
        p.texto(9, 4, Z_SAL0, "letrero", "south", 0x4CB050, "SALIDA");
        for (int x = 6; x <= 13; x++) {
            for (int z = 70; z <= 72; z++) {
                p.set(x, 0, z, C + "metal_placa");
            }
        }
        for (int i = 0; i < 3; i++) {
            p.marca(pre + "silla." + i, 7 + 2 * i, 1, 71, 0);
        }
        p.letrero(13, 3, Z_SAL0, "", "south", "gold", "TOMEN", "ASIENTO");
        // el muro del fondo con la cortina de barrotes (se abre) y el túnel oscuro detrás
        for (int x = 3; x <= 16; x++) {
            for (int y = 1; y <= 5; y++) {
                p.set(x, y, Z_SAL1 + 1, MURO);
            }
        }
        for (int x = 6; x <= 12; x++) {
            for (int y = 1; y <= 3; y++) {
                p.set(x, y, Z_SAL1 + 1, C + "barrotes");
                p.marca(pre + "cortina." + (x - 6) + "." + (y - 1), x, y, Z_SAL1 + 1, 0);
            }
        }
        p.caja(6, 1, Z_SAL1 + 2, 12, 3, Z_TUNEL1, "air");
        for (int z = Z_SAL1 + 2; z <= Z_TUNEL1; z++) {
            for (int x = 6; x <= 12; x++) {
                p.set(x, 0, z, C + "rejilla_piso[type=double]");
            }
        }
        p.marca(pre + "tunel", 9, 1, Z_TUNEL1 - 2, 0);
        p.marca(pre + "salida", 9, 1, Z_SAL0 + 1, 0);
    }
}
