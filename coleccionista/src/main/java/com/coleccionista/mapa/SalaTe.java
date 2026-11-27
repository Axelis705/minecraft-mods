package com.coleccionista.mapa;

/**
 * El salón de té (capítulo 5): una sala de visitas vieja y elegante, con papel tapiz, cuadros de muñecas y una araña enorme. Al centro, la
 * mesa larga del té con una silla en cada punta (ahí se sientan los dos del duelo, frente a frente) y el mecanismo de las manos de porcelana
 * en medio. Alrededor de la mesa, colgadas del techo con cadenas, las jaulas de pájaro (barrotes dorados delgados, para que desde adentro se
 * vea bien la mesa). Al entrar (por el pasillo de servicio, por el oeste) están los sillones de dos, donde se eligen las parejas. La
 * pantalla grande está en el muro del este.
 * <pre>
 *   x: 0 ... 63   z: -1 ... 43   y: -1 ... 19
 *   el pasillo x 2..15 (entra al salón por x 16), el salón x 17..61 / z 0..42 (alto 17)
 *   la mesa x 40..48 / z 20..22 (centro 44.5, 21.5); las sillas en x 39 (oeste, mira al este) y x 49 (este, mira al oeste)
 *   las jaulas: anillo de radio 10 alrededor de la mesa; piso de la jaula en y 5 (se para en y 6), techo en y 9
 *   los sillones de dos: x 23 y x 29, al norte y al sur del camino de la entrada
 * </pre>
 * Marcas: {@code te.silla.0/1} (las sillas del duelo, con su yaw), {@code te.mesa} (el centro de la mesa, sobre el mantel),
 * {@code te.jaula.K} (el piso de adentro de cada jaula: ahí se para quien va adentro), {@code te.sofa.K.a/b} (los asientos de los sillones,
 * con su yaw), {@code te.entrada}, {@code te.salida}, {@code te.centro}, {@code te.luz.K} (el yaw es el nivel), {@code te.pantalla} y el
 * pasillo {@code pas_te}.
 */
public final class SalaTe {
    private static final String C = "coleccionista:";
    public static final int HX0 = 17, HX1 = 61, HZ0 = 0, HZ1 = 42, ALTO = 17;
    /** La mesa: de x 40 a 48 y de z 20 a 22; su centro (punto) en (44.5, 21.5). */
    public static final int MX0 = 40, MX1 = 48, MZ0 = 20, MZ1 = 22;
    public static final double CX = 44.5, CZ = 21.5;
    /** Las jaulas: cuántas, a qué distancia de la mesa y la altura del piso de adentro (donde se para la pieza). */
    public static final int JAULAS = 12, PISO_JAULA = 6;
    public static final double RADIO_JAULAS = 10.0;
    public static int jaulas, sofas, luces;

    private SalaTe() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        jaulas = 0;
        sofas = 0;
        luces = 0;
        p.caja(0, -1, -1, 63, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_te", new Marco(2, 26, 270), Pasaje.TE);
        // se llega caminando desde la sala anterior (el pasillo de en medio es la zona "paso_...")
        Salida.llegada(p, C + "madera_pulida");
        salon(p);
        mesa(p);
        jaulas(p);
        sillones(p);
        // la puerta de salida hacia el capítulo siguiente (hacia adelante)
        Salida.puerta(p, "salida.te", 62, 4, 63, C + "madera_pulida");
        adornos(p);
        return p;
    }

    // ------------------------------------------------------------------ los adornos: la merienda

    /**
     * La merienda servida: mesitas con juegos de té y platones de pastelitos contra los muros del norte y del sur, dos arpas junto a la
     * pantalla, violines colgados y relojes de pie en las esquinas del fondo.
     */
    private static void adornos(Plano p) {
        for (int x = 34; x <= 55; x += 6) {
            p.set(x, 1, HZ0 + 1, C + "mesa[facing=south]");
            p.set(x, 2, HZ0 + 1, C + ((x / 6) % 2 == 0 ? "tetera[facing=south]" : "pastelero[facing=south]"));
            p.set(x + 3, 1, HZ1 - 1, C + "mesa[facing=north]");
            p.set(x + 3, 2, HZ1 - 1, C + ((x / 6) % 2 == 0 ? "pastelero[facing=north]" : "tetera[facing=north]"));
        }
        for (int z : new int[] {17, 26}) {
            p.set(HX1, 1, z, C + "arpa[facing=west,half=lower]");
            p.set(HX1, 2, z, C + "arpa[facing=west,half=upper]");
        }
        for (int x : new int[] {36, 50}) {
            p.set(x, 3, HZ0, C + "violin[facing=south]");
            p.set(x + 2, 3, HZ1, C + "violin[facing=north]");
        }
        p.set(HX1, 1, HZ1, C + "reloj_pie[facing=west,half=lower]");
        p.set(HX1, 2, HZ1, C + "reloj_pie[facing=west,half=upper]");
        p.set(33, 1, HZ0, C + "reloj_pie[facing=south,half=lower]");
        p.set(33, 2, HZ0, C + "reloj_pie[facing=south,half=upper]");
    }

    private static void luz(Plano p, int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
        p.marca("te.luz." + luces++, x, y, z, nivel);
    }

    // ------------------------------------------------------------------ el salón

    private static void salon(Plano p) {
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                p.set(x, 0, z, C + "parquet");
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ALTO + 1, z, C + "madera_pulida");
            }
        }
        // las paredes: lambrín de madera abajo, papel tapiz arriba, molduras doradas
        for (int y = 1; y <= ALTO; y++) {
            String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO || y == 11 ? C + "marco_dorado" : C + "papel_tapiz";
            for (int x = HX0 - 1; x <= HX1 + 1; x++) {
                p.set(x, y, HZ0 - 1, b);
                p.set(x, y, HZ1 + 1, b);
            }
            for (int z = HZ0; z <= HZ1; z++) {
                String w = p.get(HX0 - 1, y, z);
                if (w == null || !w.contains("puerta")) {
                    p.set(HX0 - 1, y, z, b);
                }
                p.set(HX1 + 1, y, z, b);
            }
        }
        // el techo: casetones de madera con un marco dorado
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                boolean marco = (x - HX0) % 6 == 0 || (z - HZ0) % 6 == 0;
                p.set(x, ALTO, z, marco ? C + "marco_dorado" : C + "panel_madera");
            }
        }
        // el piso: tablero de mármol bajo la mesa y las jaulas, y una alfombra larga desde la entrada
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                double d = Math.hypot(x + 0.5 - CX, z + 0.5 - CZ);
                if (d <= 13.5) {
                    p.set(x, 0, z, C + ((x + z) % 2 == 0 ? "marmol_negro" : "marmol_crema"));
                }
                if (d <= 5.5) {
                    p.set(x, 0, z, C + "tapete_rojo");
                }
            }
        }
        for (int x = HX0; x <= 33; x++) {
            for (int z = 19; z <= 24; z++) {
                p.set(x, 0, z, C + (z == 19 || z == 24 ? "tapete_orilla" : "tapete_rojo"));
            }
        }
        // repisas con muñecas y tazas en los muros del norte y del sur; cuadros (estandartes) entre ellas
        for (int x = HX0 + 1; x <= HX1 - 1; x++) {
            p.set(x, 5, HZ0, C + "losa_madera[type=top]");
            p.set(x, 5, HZ1, C + "losa_madera[type=top]");
            if (x % 3 == 0) {
                p.set(x, 6, HZ0, C + "muneca[facing=south]");
                p.set(x, 6, HZ1, C + "muneca[facing=north]");
            } else if (x % 3 == 1) {
                p.set(x, 6, HZ0, C + "copa");
                p.set(x, 6, HZ1, C + "copa");
            }
        }
        for (int x = HX0 + 4; x <= HX1 - 4; x += 7) {
            p.set(x, 8, HZ0, C + "estandarte_rojo[facing=south,half=lower]");
            p.set(x, 9, HZ0, C + "estandarte_rojo[facing=south,half=upper]");
            p.set(x, 8, HZ1, C + "estandarte_rojo[facing=north,half=lower]");
            p.set(x, 9, HZ1, C + "estandarte_rojo[facing=north,half=upper]");
        }
        // telarañas en las esquinas del techo
        for (int[] e : new int[][] {{HX0, HZ0}, {HX1, HZ0}, {HX0, HZ1}, {HX1, HZ1}}) {
            p.set(e[0], ALTO - 1, e[1], C + "telarana");
        }
        // candelabros junto a la entrada y la pantalla
        for (int z : new int[] {17, 26}) {
            p.set(HX0, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(HX0, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        for (int z : new int[] {14, 29}) {
            p.set(HX1, 1, z, C + "candelabro[facing=west,half=lower]");
            p.set(HX1, 2, z, C + "candelabro[facing=west,half=upper]");
        }
        // la pantalla grande en el muro del este (la ven las jaulas y los sillones)
        int pz = (int) Math.floor(CZ);
        p.set(HX1, 9, pz, C + "pantalla[facing=west,grande=true]");
        p.marca("te.pantalla", HX1, 9, pz, 90);
        // la luz del salón: arriba (para el techo y las jaulas) y a media altura (para el piso); se baja durante los duelos
        for (int x = HX0 + 3; x <= HX1 - 3; x += 7) {
            for (int z = HZ0 + 4; z <= HZ1 - 3; z += 7) {
                luz(p, x, ALTO - 2, z, 11);
            }
        }
        for (int x = HX0 + 2; x <= HX1 - 2; x += 6) {
            for (int z = HZ0 + 3; z <= HZ1 - 2; z += 6) {
                luz(p, x, 4, z, 12);
            }
        }
        p.marca("te.entrada", HX0 + 2, 1, pz, 270);
        p.marca("te.salida", HX0 + 5, 1, pz, 270);
        p.marca("te.centro", (int) Math.floor(CX), 1, pz, 270);
    }

    // ------------------------------------------------------------------ la mesa del té

    private static void mesa(Plano p) {
        for (int x = MX0; x <= MX1; x++) {
            for (int z = MZ0; z <= MZ1; z++) {
                p.set(x, 1, z, C + "mesa_tapete[facing=east]");
            }
        }
        // velas en las orillas de la mesa (no estorban a los cálices ni a las manos)
        p.set(MX0 + 2, 2, MZ0, C + "vela");
        p.set(MX1 - 2, 2, MZ0, C + "vela");
        p.set(MX0 + 2, 2, MZ1, C + "vela");
        p.set(MX1 - 2, 2, MZ1, C + "vela");
        int z = (int) Math.floor(CZ);
        // las dos sillas del duelo (las pone la prueba): oeste mira al este (270) y este mira al oeste (90)
        p.marca("te.silla.0", MX0 - 1, 1, z, 270);
        p.marca("te.silla.1", MX1 + 1, 1, z, 90);
        p.marca("te.mesa", (int) Math.floor(CX), 2, z, 0);
        // la araña grande encima de la mesa, colgando de una cadena larga
        for (int y = 12; y <= ALTO - 1; y++) {
            p.set((int) Math.floor(CX), y, z, C + "cadena");
        }
        p.set((int) Math.floor(CX), 11, z, C + "arana");
        // la luz de la mesa: fuerte (14), no se baja: la mesa siempre se ve
        luz(p, (int) Math.floor(CX), 6, z, 14);
        luz(p, MX0 - 1, 5, z, 13);
        luz(p, MX1 + 1, 5, z, 13);
        // y alrededor de la mesa (tampoco se bajan): que la mesa y los que beben se vean bien aunque el salón esté oscuro
        for (int x = MX0; x <= MX1; x += 2) {
            luz(p, x, 4, MZ0 - 2, 13);
            luz(p, x, 4, MZ1 + 2, 13);
        }
    }

    // ------------------------------------------------------------------ las jaulas colgadas

    /** El bloque central de la jaula k (el piso de adentro está en {@link #PISO_JAULA}). */
    public static int[] jaula(int k) {
        double a = Math.toRadians(15 + 360.0 / JAULAS * k);
        return new int[] {(int) Math.floor(CX + Math.cos(a) * RADIO_JAULAS), (int) Math.floor(CZ + Math.sin(a) * RADIO_JAULAS)};
    }

    private static void jaulas(Plano p) {
        int y0 = PISO_JAULA - 1;
        for (int k = 0; k < JAULAS; k++) {
            int[] c = jaula(k);
            int x = c[0], z = c[1];
            // el piso: tablitas oscuras con un aro dorado; un cojín rojo al centro
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    p.set(x + dx, y0, z + dz, dx == 0 && dz == 0 ? C + "madera_pulida" : C + "marco_dorado");
                    p.set(x + dx, y0 + 4, z + dz, C + "marco_dorado");
                    if (dx != 0 || dz != 0) {
                        for (int y = y0 + 1; y <= y0 + 3; y++) {
                            p.set(x + dx, y, z + dz, C + "barrotes_dorados");
                        }
                    }
                }
            }
            p.set(x, y0 + 1, z, C + "alfombra_roja");
            // de la jaula al techo: una cadena
            for (int y = y0 + 5; y <= ALTO - 1; y++) {
                p.set(x, y, z, C + "cadena");
            }
            // la luz de adentro (se ven las caras y, si queda un muerto, se ve verde)
            luz(p, x, y0 + 3, z, 9);
            float yaw = (float) Math.toDegrees(Math.atan2(-(CX - (x + 0.5)), CZ - (z + 0.5)));
            p.marca("te.jaula." + jaulas++, x, PISO_JAULA, z, yaw);
        }
    }

    // ------------------------------------------------------------------ los sillones de dos (para elegir pareja)

    private static void sillones(Plano p) {
        int[][] lugares = {{23, 4}, {23, 11}, {29, 7}, {23, 30}, {23, 37}, {29, 34}};
        for (int[] l : lugares) {
            int x = l[0], z = l[1];
            // dos sillones juntos mirando al este (hacia la mesa y la pantalla), una mesita con vela a un lado
            for (int dz = 0; dz <= 1; dz++) {
                p.set(x, 1, z + dz, C + "sillon[facing=east,half=lower]");
                p.set(x, 2, z + dz, C + "sillon[facing=east,half=upper]");
                p.marca("te.sofa." + sofas + (dz == 0 ? ".a" : ".b"), x, 1, z + dz, 270);
            }
            p.set(x, 1, z - 1, C + "mesa[facing=east]");
            p.set(x, 2, z - 1, C + "vela");
            p.set(x - 1, 0, z, C + "tapete_rojo");
            p.set(x - 1, 0, z + 1, C + "tapete_rojo");
            p.set(x + 1, 0, z, C + "tapete_orilla");
            p.set(x + 1, 0, z + 1, C + "tapete_orilla");
            sofas++;
        }
    }
}
