package com.coleccionista.mapa;

/**
 * La caja de música (capítulo 3): un salón forrado como el interior de una caja de música. Pegadas a los muros del norte, del este y del
 * sur hay {@link #GONDOLAS} góndolas abiertas (como casetas de feria, con su toldo de rayas y su número): cada una con dos asientos en fila
 * mirando hacia la pared (el de atrás le ve la espalda al de adelante). Las piezas entran y se sientan solas. En medio, el escenario redondo
 * con una compuerta en el piso: de ahí sale la escopeta al final. Se llega por el pasillo de servicio ({@link Pasaje}, idéntico al de la
 * casa), por el muro del oeste.
 * <pre>
 *   x: 0 ... 61   z: -1 ... 43   y: -3 ... 14
 *   el pasillo x 2..15 (entra al salón por x 16), el salón x 17..59 / z 0..42 (alto 12), su centro (38, 21)
 *   las góndolas: 4 al norte (z 0..4), 4 al este (x 55..59) y 4 al sur (z 38..42)
 *   la compuerta: x 37..39 / z 20..22 (trampillas a ras del piso) y abajo el foso (y -2..-1; el fondo en y -3)
 * </pre>
 * Cada góndola por dentro (ancho 3, fondo 5, alto 3): la entrada (del lado del salón), el sillón de atrás (con respaldo), el banquito de
 * adelante (sin respaldo: así se le ve la espalda) y la pared con su número y una vela. Marcas: {@code caja.gon.N.atras},
 * {@code caja.gon.N.frente} (N = 0..11, con el yaw hacia la pared) y {@code caja.gon.N.entrada}; {@code caja.escenario} (el centro del
 * círculo), {@code caja.arma} (el fondo del foso, donde va la escopeta), {@code caja.compuerta.K} (las 9 trampillas), {@code caja.luz.K}
 * (las luces del salón; su yaw es el nivel de luz, para bajarlas y volver a subirlas), {@code caja.centro}, {@code caja.entrada},
 * {@code caja.pantalla} y el pasillo {@code pas_caj}.
 */
public final class CajaMusica {
    private static final String C = "coleccionista:";
    public static final int GONDOLAS = 12;
    /** El salón (interior). */
    public static final int HX0 = 17, HX1 = 59, HZ0 = 0, HZ1 = 42, ALTO = 12;
    /** El centro del escenario (bloque). */
    public static final int MX = 38, MZ = 21;
    /** Qué tan lejos del centro van las sillas del círculo. */
    public static final double RADIO_CIRCULO = 5.2;
    /** Cuántas luces del salón tienen marca (caja.luz.0 .. N-1). */
    public static int luces;

    private CajaMusica() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        luces = 0;
        // abajo del piso, tierra firme (y más hondo junto a la compuerta, para el foso)
        p.caja(0, -1, -1, 61, -1, 43, C + "ladrillo_oscuro");
        p.caja(MX - 3, -3, MZ - 3, MX + 3, -2, MZ + 3, C + "ladrillo_oscuro");
        p.caja(0, 0, -1, 61, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_caj", new Marco(2, 26, 270), Pasaje.CAJA);
        // se llega caminando desde la sala anterior (el pasillo de en medio es la zona "paso_...")
        Salida.llegada(p, C + "madera_pulida");
        salon(p);
        escenario(p);
        int[][] gs = gondolas();
        for (int n = 0; n < gs.length; n++) {
            gondola(p, n, gs[n][0], gs[n][1], gs[n][2]);
        }
        // la puerta de salida hacia el capítulo siguiente (hacia adelante)
        Salida.puerta(p, "salida.caja", 60, 8, 61, C + "madera_pulida");
        adornos(p);
        return p;
    }

    // ------------------------------------------------------------------ los adornos: el mecanismo de la caja

    /**
     * Lo que hace que se sienta adentro de una caja de música: la llave de cuerda gigante saliendo del muro del oeste (arriba de la entrada),
     * engranes de latón en los muros, notas musicales colgando del techo, bailarinas de porcelana en las cuatro esquinas, soldaditos de
     * plomo cuidando la entrada y partituras tiradas. Nada estorba: todo va arriba de las cabezas, en las esquinas o pegado a los muros.
     */
    private static void adornos(Plano p) {
        String oro = C + "marco_dorado";
        // la llave de cuerda: el eje sale del muro, un collar y el moño de dos ojos (en el plano del muro)
        int ky = 9, kz = MZ;
        for (int x = HX0; x <= HX0 + 2; x++) {
            p.set(x, ky, kz, oro);
        }
        for (int y = ky - 1; y <= ky + 1; y++) {
            for (int z = kz - 1; z <= kz + 1; z++) {
                p.set(HX0 + 1, y, z, oro);
            }
        }
        for (int y = ky - 3; y <= ky + 3; y++) {
            for (int z = kz - 6; z <= kz + 6; z++) {
                double d1 = Math.hypot(y - ky, z - (kz - 3.5)), d2 = Math.hypot(y - ky, z - (kz + 3.5));
                boolean aro = d1 >= 1.5 && d1 <= 2.9 || d2 >= 1.5 && d2 <= 2.9;
                boolean centro = Math.abs(z - kz) <= 1 && Math.abs(y - ky) <= 1;
                if (aro || centro) {
                    p.set(HX0 + 3, y, z, oro);
                }
            }
        }
        // engranes de latón en los muros del norte y del sur (arriba de las repisas) y en el del este (a los lados de la pantalla)
        for (int x = HX0 + 3; x <= HX1 - 6; x += 6) {
            p.set(x, 10, HZ0, C + "engrane[facing=south]");
            p.set(x + 1, 11, HZ0, C + "engrane[facing=south]");
            p.set(x + 1, 9, HZ0, C + "engrane[facing=south]");
            p.set(x + 3, 10, HZ1, C + "engrane[facing=north]");
            p.set(x + 4, 11, HZ1, C + "engrane[facing=north]");
            p.set(x + 4, 9, HZ1, C + "engrane[facing=north]");
        }
        for (int z : new int[] {HZ0 + 4, HZ0 + 9, HZ1 - 9, HZ1 - 4}) {
            p.set(HX1, 10, z, C + "engrane[facing=west]");
            p.set(HX1, 11, z + 1, C + "engrane[facing=west]");
        }
        // notas musicales colgando del techo: un anillo alrededor del escenario y unas en las esquinas
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(k * 30 + 15);
            int x = (int) Math.round(MX + Math.cos(a) * 11), z = (int) Math.round(MZ + Math.sin(a) * 11);
            p.set(x, ALTO, z, C + "nota_musical[facing=" + (k % 2 == 0 ? "south" : "east") + "]");
        }
        for (int[] q : new int[][] {{HX0 + 4, HZ0 + 6}, {HX0 + 5, HZ1 - 7}, {HX1 - 6, HZ0 + 6}, {HX1 - 5, HZ1 - 7}}) {
            p.set(q[0], ALTO, q[1], C + "nota_musical[facing=south]");
            p.set(q[0] + 2, ALTO - 1, q[1] + 1, C + "nota_musical[facing=east]");
        }
        // las bailarinas de porcelana, en las cuatro esquinas (mirando al centro), con partituras a su alrededor
        for (int[] q : new int[][] {{HX0 + 4, HZ0 + 4, 0}, {HX0 + 4, HZ1 - 4, 1}, {HX1 - 7, HZ0 + 6, 2}, {HX1 - 7, HZ1 - 6, 3}}) {
            String mira = q[2] == 0 ? "east" : q[2] == 1 ? "east" : "west";
            p.set(q[0], 1, q[1], C + "bailarina_figura[facing=" + mira + ",half=lower]");
            p.set(q[0], 2, q[1], C + "bailarina_figura[facing=" + mira + ",half=upper]");
            p.set(q[0] + 1, 1, q[1] + 1, C + "papeles");
            p.set(q[0] - 1, 1, q[1] - 1, C + "papeles");
        }
        // los soldaditos de plomo, a los dos lados de la alfombra de la entrada (mirando al escenario)
        for (int x : new int[] {HX0 + 3, HX0 + 9}) {
            for (int z : new int[] {MZ - 3, MZ + 3}) {
                p.set(x, 1, z, C + "soldadito[facing=east,half=lower]");
                p.set(x, 2, z, C + "soldadito[facing=east,half=upper]");
            }
        }
    }

    /**
     * Las góndolas: {x, z del bloque de la entrada (por dentro), yaw hacia la pared}, numeradas en el sentido del reloj: 1..4 al norte (de
     * oeste a este), 5..8 al este (de norte a sur) y 9..12 al sur (de este a oeste).
     */
    public static int[][] gondolas() {
        return new int[][] {
                {32, 4, 180}, {36, 4, 180}, {40, 4, 180}, {44, 4, 180},
                {55, 15, 270}, {55, 19, 270}, {55, 23, 270}, {55, 27, 270},
                {44, 38, 0}, {40, 38, 0}, {36, 38, 0}, {32, 38, 0}};
    }

    /** El marco local de una góndola: k crece hacia la pared (k 0 es la entrada, por dentro), i de lado a lado (-1..1). */
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
        // el tapete de la entrada hasta el escenario, y uno alrededor del escenario
        for (int x = HX0; x <= MX - 9; x++) {
            for (int z = MZ - 2; z <= MZ + 2; z++) {
                p.set(x, 0, z, C + (z == MZ - 2 || z == MZ + 2 ? "tapete_orilla" : "tapete_rojo"));
            }
        }
        // repisas con bailarinas de porcelana arriba de las góndolas (norte y sur) y en el muro del oeste
        for (int x = HX0 + 2; x <= HX1 - 2; x++) {
            p.set(x, 7, HZ0, C + "losa_madera[type=top]");
            p.set(x, 7, HZ1, C + "losa_madera[type=top]");
            if (x % 3 == 0) {
                p.set(x, 8, HZ0, C + "muneca[facing=south]");
                p.set(x, 8, HZ1, C + "muneca[facing=north]");
            }
        }
        for (int z = HZ0 + 3; z <= HZ1 - 3; z++) {
            if (z >= MZ - 6 && z <= MZ + 6) {
                continue;
            }
            p.set(HX0, 7, z, C + "losa_madera[type=top]");
            if (z % 3 == 0) {
                p.set(HX0, 8, z, C + "muneca[facing=east]");
            }
        }
        // columnas en las esquinas y candelabros junto a la entrada
        for (int x : new int[] {HX0, HX1}) {
            for (int z : new int[] {HZ0, HZ1}) {
                for (int y = 1; y <= ALTO; y++) {
                    p.set(x, y, z, C + "viga_madera[axis=y]");
                }
            }
        }
        for (int z : new int[] {MZ - 6, MZ + 6}) {
            p.set(HX0, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(HX0, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        // relojes viejos (el mecanismo de la caja) en las esquinas libres
        p.set(HX0 + 4, 6, HZ0, C + "reloj[facing=south]");
        p.set(HX1, 6, HZ0 + 6, C + "reloj[facing=west]");
        p.set(HX1, 6, HZ1 - 6, C + "reloj[facing=west]");
        p.set(HX0 + 4, 6, HZ1, C + "reloj[facing=north]");
        // luz tibia (se baja en la revisión: cada luz tiene su marca, con su nivel en el yaw)
        for (int x = HX0 + 4; x <= HX1 - 3; x += 7) {
            for (int z = HZ0 + 7; z <= HZ1 - 6; z += 7) {
                luz(p, x, ALTO - 1, z, 12);
            }
        }
        for (int z = HZ0 + 6; z <= HZ1 - 5; z += 6) {
            luz(p, HX0 + 2, 3, z, 11);
        }
        for (int x = HX0 + 9; x <= HX1 - 9; x += 6) {
            luz(p, x, 3, MZ - 12, 10);
            luz(p, x, 3, MZ + 12, 10);
        }
        luz(p, HX1 - 12, 3, MZ, 10);
        p.marca("caja.centro", HX0 + 4, 1, MZ, 270);
        p.marca("caja.entrada", HX0 + 2, 1, MZ, 270);
        // la pantalla grande del Coleccionista: en el muro del este, arriba de las góndolas (la ven todos al entrar)
        p.set(HX1, 9, MZ, C + "pantalla[facing=west,grande=true]");
        p.marca("caja.pantalla", HX1, 9, MZ, 90);
    }

    private static void luz(Plano p, int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
        p.marca("caja.luz." + luces++, x, y, z, nivel);
    }

    // ------------------------------------------------------------------ el escenario del centro

    private static void escenario(Plano p) {
        for (int x = MX - 9; x <= MX + 9; x++) {
            for (int z = MZ - 9; z <= MZ + 9; z++) {
                double d = Math.sqrt((x - MX) * (x - MX) + (z - MZ) * (z - MZ));
                if (d > 8.6) {
                    continue;
                }
                String piso = d < 2.6 ? "metal_negro" : d < 3.6 ? "marco_dorado" : d < 7.6 ? ((x + z) % 2 == 0 ? "marmol_negro" : "marmol_crema")
                        : "marmol_oro";
                p.set(x, 0, z, C + piso);
            }
        }
        // la compuerta (3 x 3): trampillas de hierro cerradas a ras del piso; al abrirse, cada una queda parada contra la orilla de afuera
        int k = 0;
        for (int x = MX - 1; x <= MX + 1; x++) {
            for (int z = MZ - 1; z <= MZ + 1; z++) {
                String hacia = x < MX ? "east" : x > MX ? "west" : z < MZ ? "south" : z > MZ ? "north" : "south";
                p.set(x, 0, z, C + "trampilla[facing=" + hacia + ",half=top,open=false]");
                p.marca("caja.compuerta." + k++, x, 0, z, 0);
                p.set(x, -1, z, "air");
                p.set(x, -2, z, "air");
                p.set(x, -3, z, C + "metal_negro");
            }
        }
        // las paredes del foso: metal y cobre viejo (se ven al abrirse la compuerta)
        for (int x = MX - 2; x <= MX + 2; x++) {
            for (int z = MZ - 2; z <= MZ + 2; z++) {
                if (Math.abs(x - MX) == 2 || Math.abs(z - MZ) == 2) {
                    p.set(x, -1, z, C + "metal_negro");
                    p.set(x, -2, z, C + "cobre_viejo");
                }
            }
        }
        p.set(MX - 1, -2, MZ - 1, "light[level=6]");
        // arriba del escenario: el toldo de rayas en el techo y una araña
        for (int x = MX - 6; x <= MX + 6; x++) {
            for (int z = MZ - 6; z <= MZ + 6; z++) {
                double d = Math.sqrt((x - MX) * (x - MX) + (z - MZ) * (z - MZ));
                if (d <= 6.4) {
                    int t = (int) Math.floor((Math.atan2(z - MZ, x - MX) + Math.PI) / (Math.PI / 6));
                    p.set(x, ALTO, z, t % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
                }
            }
        }
        p.set(MX, ALTO - 1, MZ, C + "cadena");
        p.set(MX, ALTO - 2, MZ, C + "arana");
        // cuatro candelabros alrededor del escenario (en el piso, fuera del círculo de sillas)
        for (int[] c : new int[][] {{MX - 8, MZ - 3}, {MX - 8, MZ + 3}, {MX + 8, MZ - 3}, {MX + 8, MZ + 3}}) {
            String hacia = c[0] < MX ? "east" : "west";
            p.set(c[0], 1, c[1], C + "candelabro[facing=" + hacia + ",half=lower]");
            p.set(c[0], 2, c[1], C + "candelabro[facing=" + hacia + ",half=upper]");
        }
        for (int[] c : new int[][] {{MX + 5, MZ}, {MX - 5, MZ}, {MX, MZ + 5}, {MX, MZ - 5}}) {
            luz(p, c[0], 7, c[1], 13);
        }
        p.marca("caja.escenario", MX, 1, MZ, 0);
        p.marca("caja.arma", MX, -2, MZ, 0);
    }

    // ------------------------------------------------------------------ las góndolas

    /** Una góndola abierta: el hueco, el piso, los dos asientos, el número (adentro y afuera), una vela y su toldo de rayas. */
    private static void gondola(Plano p, int n, int bx, int bz, int yaw) {
        Marco m = marco(bx, bz, yaw);
        String num = String.valueOf(n + 1);
        // adentro: de la entrada (k 0) a la pared del salón (k 5); de lado, i -1..1
        for (int i = -1; i <= 1; i++) {
            for (int k = 0; k <= 4; k++) {
                m.set(p, i, 0, k, C + "tapete_rojo");
                for (int y = 1; y <= 3; y++) {
                    m.set(p, i, y, k, "air");
                }
                m.set(p, i, 4, k, C + "madera_pulida");
            }
        }
        // los lados (acolchados, con moldura dorada abajo) y el arco de la entrada
        for (int k = 0; k <= 4; k++) {
            for (int y = 1; y <= 4; y++) {
                for (int i : new int[] {-2, 2}) {
                    m.set(p, i, y, k, y == 1 ? C + "marco_dorado" : y == 4 ? C + "madera_pulida" : C + "acolchado");
                }
            }
        }
        for (int i = -2; i <= 2; i++) {
            m.set(p, i, 4, 0, C + "marco_dorado");
            // el toldo de rayas arriba de cada góndola (como las casetas de una feria)
            for (int k = -1; k <= 4; k++) {
                m.set(p, i, 5, k, (i + 2) % 2 == 0 ? "minecraft:red_wool" : "minecraft:white_wool");
            }
        }
        // el asiento de atrás: un sillón con respaldo; el de adelante: un banquito (sin respaldo: se le ve la espalda)
        m.set(p, 0, 1, 1, C + "sillon[facing=south,half=lower]");
        m.set(p, 0, 2, 1, C + "sillon[facing=south,half=upper]");
        m.set(p, 0, 1, 3, C + "losa_madera[type=bottom]");
        // la pared del fondo: su número grande, una vela en una repisa y una luz bajita
        m.texto(p, 0, 3, 4, "placa", "north", 0xD9A93A, num);
        m.set(p, 1, 1, 4, C + "losa_madera[type=top]");
        m.set(p, 1, 2, 4, C + "vela");
        m.set(p, 0, 3, 2, "light[level=10]");
        // afuera: el número arriba de la entrada (bajo el toldo)
        m.texto(p, 0, 4, -1, "placa", "north", 0xD9A93A, num);
        m.marca(p, "caja.gon." + n + ".atras", 0, 1, 1, 0);
        m.marca(p, "caja.gon." + n + ".frente", 0, 1, 3, 0);
        m.marca(p, "caja.gon." + n + ".entrada", 0, 1, 0, 180);
    }
}
