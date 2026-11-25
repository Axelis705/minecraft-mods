package com.coleccionista.mapa;

/**
 * La caja de música (capítulo 3): un salón forrado como el interior de una caja de música, con un carrusel enorme en medio. El carrusel
 * tiene {@link #GONDOLAS} góndolas cerradas y numeradas (tres por lado, cada una mirando hacia afuera) y, escondido en su centro, el
 * escenario redondo donde al final se sientan todos en círculo y baja el arma del techo. Se llega por el pasillo de servicio
 * ({@link Pasaje}, idéntico al de la casa).
 * <pre>
 *   x: 0 ... 64   z: -1 ... 43   y: -1 ... 14
 *   el pasillo x 1..16 (entra al salón por x 16), el salón x 17..63 / z 0..42 (alto 12),
 *   el carrusel x 27..53 / z 8..34 (hasta y 11), su escenario (adentro) x 33..47 / z 14..28 (alto 9)
 * </pre>
 * Cada góndola por dentro (ancho 3, largo 4, alto 3): el asiento de atrás (un sillón con respaldo contra la pared del escenario), el de
 * adelante (un banquito sin respaldo: así el de atrás le ve la espalda) y, en la pared del frente, su número y una vela. Marcas:
 * {@code caja.gon.N.atras} y {@code caja.gon.N.frente} (N = 0..11, con el yaw hacia el frente), {@code caja.escenario} (el centro del
 * círculo), {@code caja.arma} (de donde cuelga el arma), {@code caja.centro}, {@code caja.entrada}, {@code caja.pantalla} y el pasillo
 * {@code pas_caj}.
 */
public final class CajaMusica {
    private static final String C = "coleccionista:";
    public static final int GONDOLAS = 12;
    /** El salón (interior). */
    public static final int HX0 = 17, HX1 = 63, HZ0 = 0, HZ1 = 42, ALTO = 12;
    /** El carrusel (por fuera) y su escenario (por dentro). */
    public static final int CX0 = 27, CX1 = 53, CZ0 = 8, CZ1 = 34, CARRUSEL_ALTO = 11;
    public static final int EX0 = 33, EX1 = 47, EZ0 = 14, EZ1 = 28, ESCENARIO_ALTO = 9;
    /** El centro del escenario (bloque). */
    public static final int MX = 40, MZ = 21;
    /** Qué tan lejos del centro van las sillas del círculo. */
    public static final double RADIO_CIRCULO = 5.2;

    private CajaMusica() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.caja(0, -1, -1, 64, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_caj", new Marco(2, 26, 270));
        salon(p);
        carrusel(p);
        escenario(p);
        int[][] gs = gondolas();
        for (int n = 0; n < gs.length; n++) {
            gondola(p, n, gs[n][0], gs[n][1], gs[n][2]);
        }
        return p;
    }

    /** Las góndolas: {x, z del asiento de atrás, yaw hacia el frente}, numeradas en el sentido del reloj desde el norte. */
    public static int[][] gondolas() {
        return new int[][] {
                {36, 12, 180}, {40, 12, 180}, {44, 12, 180},
                {49, 17, 270}, {49, 21, 270}, {49, 25, 270},
                {44, 30, 0}, {40, 30, 0}, {36, 30, 0},
                {31, 25, 90}, {31, 21, 90}, {31, 17, 90}};
    }

    /** El marco local de una góndola: k crece hacia el frente (el asiento de atrás es k 0), i de lado a lado (-1..1). */
    private static Marco marco(int bx, int bz, int yaw) {
        Marco tmp = new Marco(0, 0, yaw);
        int cx = bx + (1 - (tmp.ux + tmp.wx)) / 2, cz = bz + (1 - (tmp.uz + tmp.wz)) / 2;
        return new Marco(cx, cz, yaw);
    }

    // ------------------------------------------------------------------ el salón

    private static void salon(Plano p) {
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                p.set(x, 0, z, C + "parquet");
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ALTO + 1, z, (x - HX0) % 6 == 0 || (z - HZ0) % 6 == 0 ? C + "marco_dorado" : C + "madera_pulida");
            }
        }
        // las paredes: forradas como el interior de una caja de música (acolchado de terciopelo entre molduras doradas)
        for (int y = 1; y <= ALTO; y++) {
            String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO ? C + "marco_dorado" : C + "acolchado";
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
        // un tapete rojo alrededor del carrusel y otro de la entrada hasta él
        for (int x = CX0 - 2; x <= CX1 + 2; x++) {
            for (int z = CZ0 - 2; z <= CZ1 + 2; z++) {
                boolean orilla = x == CX0 - 2 || x == CX1 + 2 || z == CZ0 - 2 || z == CZ1 + 2;
                if (x < CX0 || x > CX1 || z < CZ0 || z > CZ1) {
                    p.set(x, 0, z, C + (orilla ? "tapete_orilla" : "tapete_rojo"));
                }
            }
        }
        for (int x = HX0; x < CX0 - 2; x++) {
            for (int z = 19; z <= 23; z++) {
                p.set(x, 0, z, C + (z == 19 || z == 23 ? "tapete_orilla" : "tapete_rojo"));
            }
        }
        // repisas con bailarinas de porcelana en los muros del norte y del sur (miran al carrusel)
        for (int x = HX0 + 2; x <= HX1 - 2; x++) {
            p.set(x, 4, HZ0, C + "losa_madera[type=top]");
            p.set(x, 4, HZ1, C + "losa_madera[type=top]");
            if (x % 3 == 0) {
                p.set(x, 5, HZ0, C + "muneca[facing=south]");
                p.set(x, 5, HZ1, C + "muneca[facing=north]");
            }
        }
        // columnas doradas en las esquinas y candelabros
        for (int x : new int[] {HX0, HX1}) {
            for (int z : new int[] {HZ0, HZ1}) {
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, C + "viga_madera[axis=y]");
                }
            }
        }
        for (int z : new int[] {6, 36}) {
            p.set(HX0, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(HX0, 2, z, C + "candelabro[facing=east,half=upper]");
            p.set(HX1, 1, z, C + "candelabro[facing=west,half=lower]");
            p.set(HX1, 2, z, C + "candelabro[facing=west,half=upper]");
        }
        // luz tibia y baja (como dentro de una caja cerrada): alcanza para ver, no más
        for (int x = HX0 + 3; x <= HX1 - 2; x += 7) {
            for (int z = HZ0 + 3; z <= HZ1 - 2; z += 7) {
                if (x >= CX0 - 1 && x <= CX1 + 1 && z >= CZ0 - 1 && z <= CZ1 + 1) {
                    continue;
                }
                p.set(x, 3, z, "light[level=12]");
                p.set(x, ALTO - 1, z, "light[level=11]");
            }
        }
        p.marca("caja.centro", HX0 + 4, 1, MZ, 270);
        p.marca("caja.entrada", HX0 + 2, 1, MZ, 270);
    }

    // ------------------------------------------------------------------ el carrusel

    private static void carrusel(Plano p) {
        // el cuerpo macizo (luego se le cavan el escenario y las góndolas)
        for (int x = CX0; x <= CX1; x++) {
            for (int z = CZ0; z <= CZ1; z++) {
                boolean orilla = x == CX0 || x == CX1 || z == CZ0 || z == CZ1;
                p.set(x, 0, z, C + "marmol_oro");
                for (int y = 1; y <= CARRUSEL_ALTO; y++) {
                    String b;
                    if (!orilla) {
                        b = y == CARRUSEL_ALTO ? C + "metal_negro" : C + "panel_madera";
                    } else if (y == 1 || y == 5 || y == CARRUSEL_ALTO) {
                        b = C + "marco_dorado";
                    } else if (y <= 4) {
                        b = C + "madera_pulida";
                    } else {
                        // la lona del carrusel: rayas rojas y blancas
                        int t = (x + z) / 2;
                        b = t % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool";
                    }
                    p.set(x, y, z, b);
                }
            }
        }
        // las esquinas redondeadas (casi un octágono)
        int[][] esq = {{CX0, CZ0, 1, 1}, {CX1, CZ0, -1, 1}, {CX0, CZ1, 1, -1}, {CX1, CZ1, -1, -1}};
        for (int[] e : esq) {
            for (int a = 0; a < 3; a++) {
                for (int b = 0; b < 3 - a; b++) {
                    int x = e[0] + a * e[2], z = e[1] + b * e[3];
                    for (int y = 1; y <= CARRUSEL_ALTO; y++) {
                        p.set(x, y, z, "air");
                    }
                    p.set(x, 0, z, C + "tapete_rojo");
                }
            }
            // el borde nuevo de la esquina (diagonal) también dorado y con lona
            for (int a = 0; a <= 3; a++) {
                int x = e[0] + a * e[2], z = e[1] + (3 - a) * e[3];
                for (int y = 1; y <= CARRUSEL_ALTO; y++) {
                    p.set(x, y, z, y == 1 || y == 5 || y == CARRUSEL_ALTO ? C + "marco_dorado"
                            : y <= 4 ? C + "madera_pulida" : a % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
                }
            }
        }
        // arriba: una corona de focos (luces de feria) alrededor del techo del carrusel
        for (int x = CX0 + 2; x <= CX1 - 2; x += 3) {
            p.set(x, CARRUSEL_ALTO + 1, CZ0 + 1, C + "foco");
            p.set(x, CARRUSEL_ALTO + 1, CZ1 - 1, C + "foco");
        }
        for (int z = CZ0 + 2; z <= CZ1 - 2; z += 3) {
            p.set(CX0 + 1, CARRUSEL_ALTO + 1, z, C + "foco");
            p.set(CX1 - 1, CARRUSEL_ALTO + 1, z, C + "foco");
        }
        // la pantalla grande del Coleccionista, colgada en la cara del oeste del carrusel (la que ven al entrar)
        p.set(CX0 - 1, 8, MZ, C + "pantalla[facing=west,grande=true]");
        p.marca("caja.pantalla", CX0 - 1, 8, MZ, 90);
    }

    /** Una góndola: el hueco, el piso, los dos asientos, el número (adentro y afuera) y una vela. */
    private static void gondola(Plano p, int n, int bx, int bz, int yaw) {
        Marco m = marco(bx, bz, yaw);
        String num = String.valueOf(n + 1);
        // adentro: de la pared del escenario (k -1) a la del frente (k 4); de lado, i -1..1
        for (int i = -1; i <= 1; i++) {
            for (int k = 0; k <= 3; k++) {
                m.set(p, i, 0, k, C + "tapete_rojo");
                for (int y = 1; y <= 3; y++) {
                    m.set(p, i, y, k, "air");
                }
                m.set(p, i, 4, k, C + "madera_pulida");
            }
        }
        // los lados (la pared de atrás es la del escenario: se queda como está)
        for (int k = 0; k <= 4; k++) {
            for (int y = 1; y <= 4; y++) {
                for (int i : new int[] {-2, 2}) {
                    m.set(p, i, y, k, y == 1 ? C + "marco_dorado" : C + "acolchado");
                }
            }
        }
        // el asiento de atrás: un sillón con respaldo (contra la pared); el de adelante: un banquito (sin respaldo: se le ve la espalda)
        m.set(p, 0, 1, 0, C + "sillon[facing=south,half=lower]");
        m.set(p, 0, 2, 0, C + "sillon[facing=south,half=upper]");
        m.set(p, 0, 1, 2, C + "losa_madera[type=bottom]");
        // la pared del frente: su número grande, una vela en una repisa y una luz bajita (se ven las espaldas, no mucho más)
        m.texto(p, 0, 3, 3, "placa", "north", 0xD9A93A, num);
        m.set(p, 1, 1, 3, C + "losa_madera[type=top]");
        m.set(p, 1, 2, 3, C + "vela");
        m.set(p, 0, 3, 1, "light[level=10]");
        // afuera: el número pintado en el frente de la góndola
        m.texto(p, 0, 3, 5, "placa", "south", 0xD9A93A, num);
        m.marca(p, "caja.gon." + n + ".atras", 0, 1, 0, 0);
        m.marca(p, "caja.gon." + n + ".frente", 0, 1, 2, 0);
    }

    // ------------------------------------------------------------------ el escenario del centro

    private static void escenario(Plano p) {
        for (int x = EX0; x <= EX1; x++) {
            for (int z = EZ0; z <= EZ1; z++) {
                double d = Math.sqrt((x - MX) * (x - MX) + (z - MZ) * (z - MZ));
                String piso = d < 2.6 ? "tapete_rojo" : d < 7.6 ? ((x + z) % 2 == 0 ? "marmol_negro" : "marmol_crema") : "parquet";
                p.set(x, 0, z, C + piso);
                for (int y = 1; y <= ESCENARIO_ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ESCENARIO_ALTO + 1, z, C + "madera_pulida");
            }
        }
        for (int y = 1; y <= ESCENARIO_ALTO; y++) {
            String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ESCENARIO_ALTO ? C + "marco_dorado" : C + "papel_tapiz";
            for (int x = EX0 - 1; x <= EX1 + 1; x++) {
                p.set(x, y, EZ0 - 1, b);
                p.set(x, y, EZ1 + 1, b);
            }
            for (int z = EZ0; z <= EZ1; z++) {
                p.set(EX0 - 1, y, z, b);
                p.set(EX1 + 1, y, z, b);
            }
        }
        // el hueco del techo de donde baja el arma (oscuro, con cadenas)
        for (int x = MX - 1; x <= MX + 1; x++) {
            for (int z = MZ - 1; z <= MZ + 1; z++) {
                p.set(x, ESCENARIO_ALTO + 1, z, "air");
            }
        }
        p.set(MX - 1, ESCENARIO_ALTO + 1, MZ - 1, C + "cadena");
        p.set(MX + 1, ESCENARIO_ALTO + 1, MZ + 1, C + "cadena");
        // en las esquinas, candelabros y engranes viejos (relojes) en las paredes
        for (int[] c : new int[][] {{EX0, EZ0}, {EX1, EZ0}, {EX0, EZ1}, {EX1, EZ1}}) {
            p.set(c[0], 1, c[1], C + "candelabro[facing=" + (c[1] == EZ0 ? "south" : "north") + ",half=lower]");
            p.set(c[0], 2, c[1], C + "candelabro[facing=" + (c[1] == EZ0 ? "south" : "north") + ",half=upper]");
        }
        p.set(MX, 6, EZ0, C + "reloj[facing=south]");
        p.set(MX, 6, EZ1, C + "reloj[facing=north]");
        p.set(EX0, 6, MZ, C + "reloj[facing=east]");
        p.set(EX1, 6, MZ, C + "reloj[facing=west]");
        // luz: la del centro (sobre el círculo) un poco más fuerte
        for (int[] c : new int[][] {{EX0 + 2, EZ0 + 2}, {EX1 - 2, EZ0 + 2}, {EX0 + 2, EZ1 - 2}, {EX1 - 2, EZ1 - 2}}) {
            p.set(c[0], 7, c[1], "light[level=12]");
            p.set(c[0], 2, c[1] + (c[1] == EZ0 ? 1 : -1), "light[level=11]");
        }
        p.set(MX + 2, 7, MZ, "light[level=15]");
        p.set(MX - 2, 7, MZ, "light[level=15]");
        p.set(MX, 7, MZ + 2, "light[level=14]");
        p.set(MX, 7, MZ - 2, "light[level=14]");
        p.marca("caja.escenario", MX, 1, MZ, 0);
        p.marca("caja.arma", MX, ESCENARIO_ALTO, MZ, 0);
    }
}
