package com.coleccionista.mapa;

import static com.coleccionista.mapa.Mansion.*;

/**
 * El jardín de noche de la mansión y cómo se llega: el pasillo de servicio da a una cripta; de ahí una escalera sube dentro del panteón
 * de la familia y al salir se ve la casa al fondo del camino. Adelante: la fuente con el ángel, arriates de rosales secos, faroles,
 * árboles muertos, el laberinto de setos (al oeste) y el cementerio con su capilla (al este). A los lados, pasillos angostos (la leña,
 * los barriles). Atrás: la terraza, el invernadero, el estanque con su puente, la glorieta, el cobertizo y el cementerio de las mascotas
 * de antes. Todo cercado por un muro alto de sillar con picos.
 */
final class MansionJardin {
    /** El terreno: el pasto arriba (y 6), se camina en y 7. */
    static final int SUELO = PB - 1;
    static final int TX0 = 0, TX1 = 53, TZ0 = -64, TZ1 = 95;

    private MansionJardin() {
    }

    // ------------------------------------------------------------------ el terreno y el muro de afuera

    static void terreno(Trazo t) {
        for (int x = TX0; x <= TX1; x++) {
            for (int z = TZ0; z <= TZ1; z++) {
                for (int y = SOT; y < SUELO; y++) {
                    t.set(x, y, z, C + "tierra");
                }
                t.set(x, SUELO, z, C + "pasto");
            }
        }
        muro(t);
        adelante(t);
        laberinto(t);
        cementerio(t);
        lados(t);
        atras(t);
    }

    private static void muro(Trazo t) {
        for (int x = TX0; x <= TX1; x++) {
            columna(t, x, TZ0, (x - TX0) % 8 == 0 || x == TX1);
            columna(t, x, TZ1, (x - TX0) % 8 == 0 || x == TX1);
        }
        for (int z = TZ0; z <= TZ1; z++) {
            columna(t, TX0, z, (z - TZ0) % 8 == 0 || z == TZ1);
            columna(t, TX1, z, (z - TZ0) % 8 == 0 || z == TZ1);
        }
    }

    /** Una columna del muro del jardín: sillar con cornisa y picos de hierro; cada 8, un pilar más alto con su remate. */
    private static void columna(Trazo t, int x, int z, boolean pilar) {
        for (int y = SOT; y <= SUELO + 5; y++) {
            t.set(x, y, z, y <= SUELO + 1 ? C + "ladrillo_oscuro" : C + "piedra_capilla");
        }
        if (pilar) {
            t.set(x, SUELO + 6, z, C + "ladrillo_oscuro");
            t.set(x, SUELO + 7, z, C + "cornisa");
            t.set(x, SUELO + 8, z, C + "losa_sillar[type=bottom]");
        } else {
            t.set(x, SUELO + 6, z, C + "cornisa");
            t.set(x, SUELO + 7, z, C + "barrotes");
        }
    }

    // ------------------------------------------------------------------ cosas que se repiten

    /** Un árbol muerto: tronco, unas ramas gruesas de lado y ramitas secas en las puntas. */
    static void arbol(Trazo t, int x, int z, int alto) {
        int y0 = SUELO + 1;
        for (int y = y0; y < y0 + alto; y++) {
            t.set(x, y, z, C + "tronco[axis=y]");
        }
        // raíces
        t.set(x, SUELO, z, C + "tierra");
        int[][] lados = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int ramas = 2 + t.r.nextInt(2);
        for (int i = 0; i < ramas; i++) {
            int[] d = lados[(i + t.r.nextInt(2)) % 4];
            int y = y0 + alto - 2 - i * 2 - t.r.nextInt(2);
            if (y < y0 + 2) {
                y = y0 + 2;
            }
            int largo = 1 + t.r.nextInt(2);
            String eje = d[0] != 0 ? "x" : "z";
            int bx = x, bz = z;
            for (int k = 1; k <= largo; k++) {
                bx = x + d[0] * k;
                bz = z + d[1] * k;
                t.set(bx, y + (k == largo ? 1 : 0), bz, C + "tronco[axis=" + eje + "]");
            }
            t.set(bx + d[0], y + 1, bz + d[1], C + "ramas");
            t.set(bx, y + 2, bz, C + "ramas");
        }
        t.set(x, y0 + alto, z, C + "ramas");
        if (t.r.nextBoolean()) {
            t.set(x, y0 + alto + 1, z, C + "ramas");
        }
    }

    /** Un farol de jardín: poste de hierro y el farol arriba. */
    static void farola(Trazo t, int x, int z) {
        for (int y = SUELO + 1; y <= SUELO + 3; y++) {
            t.set(x, y, z, C + "poste[axis=y]");
        }
        t.set(x, SUELO + 4, z, C + "farol");
    }

    /** Una tumba: montículo de tierra, la lápida a la cabecera (mirando hacia {@code mira}) y a veces velas. */
    static void tumba(Trazo t, int x, int z, String mira, boolean cruz) {
        int dz = mira.equals("north") ? 1 : mira.equals("south") ? -1 : 0, dx = mira.equals("west") ? 1 : mira.equals("east") ? -1 : 0;
        t.set(x, SUELO, z, C + "tierra");
        t.set(x - dx, SUELO, z - dz, C + "tierra");
        t.set(x + dx, SUELO + 1, z + dz, C + (cruz ? "lapida_cruz" : "lapida") + "[facing=" + mira + "]");
        if (t.r.nextInt(3) == 0) {
            t.set(x, SUELO + 1, z, C + "vela");
        } else if (t.r.nextInt(3) == 0) {
            t.set(x, SUELO + 1, z, C + "rosal");
        }
    }

    static void seto(Trazo t, int x0, int z0, int x1, int z1, int alto) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int y = SUELO + 1; y <= SUELO + alto; y++) {
                    t.set(x, y, z, C + "seto");
                }
            }
        }
    }

    static void grava(Trazo t, int x0, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                t.set(x, SUELO, z, C + "grava");
            }
        }
    }

    // ------------------------------------------------------------------ la cripta y el panteón (por donde se llega)

    static void cripta(Trazo t) {
        // el pasillo de servicio (de la cabina): su fondo da a la cripta
        Pasaje.pasillo(t.p, "pas_man", new Marco(23, -62, 0));
        int x0 = 20, x1 = 33, z0 = -48, z1 = -37, y1 = SUELO;
        // la cripta: bóveda de sillar, lajas, nichos con cráneos, dos sarcófagos y velas
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean muro = x == x0 || x == x1 || z == z0 || z == z1;
                if (z == z0 && x >= 22 && x <= 31) {
                    continue; // el muro del pasillo
                }
                t.set(x, SOT, z, C + "losa_piedra");
                for (int y = SOT + 1; y < y1; y++) {
                    t.set(x, y, z, muro ? C + "piedra_capilla" : "air");
                }
                t.set(x, y1, z, C + "piedra_capilla");
            }
        }
        // la bóveda: escalones al revés a lo largo de los muros largos
        for (int z = z0 + 1; z < z1; z++) {
            t.set(x0 + 1, y1 - 1, z, C + "escalera_sillar[facing=west,half=top]");
            t.set(x1 - 1, y1 - 1, z, C + "escalera_sillar[facing=east,half=top]");
        }
        for (int z = z0 + 2; z < z1; z += 3) {
            for (int x = x0 + 2; x <= x1 - 2; x++) {
                t.set(x, y1 - 1, z, C + "losa_sillar[type=top]");
            }
        }
        // nichos en los muros de los lados (con cráneos) y uno grande para esconderse
        for (int z = z0 + 2; z <= z1 - 2; z += 2) {
            for (int y : new int[] {SOT + 1, SOT + 3}) {
                t.set(x0, y, z, C + "craneo");
                t.set(x0 - 1, y, z, C + "piedra_capilla");
                t.set(x1, y, z, z % 4 == 0 ? C + "craneo" : C + "vela");
                t.set(x1 + 1, y, z, C + "piedra_capilla");
            }
        }
        // un nicho grande al este (se cabe parado)
        for (int z = -45; z <= -44; z++) {
            for (int y = SOT + 1; y <= SOT + 2; y++) {
                t.set(x1, y, z, "air");
                t.set(x1 + 1, y, z, "air");
            }
            t.set(x1, SOT, z, C + "losa_piedra");
            t.set(x1 + 1, SOT, z, C + "losa_piedra");
            t.set(x1 + 2, SOT + 1, z, C + "piedra_capilla");
            t.set(x1 + 2, SOT + 2, z, C + "piedra_capilla");
            t.set(x1, SOT + 3, z, C + "piedra_capilla");
            t.set(x1 + 1, SOT + 3, z, C + "piedra_capilla");
        }
        t.set(x1 + 1, SOT + 1, -43, C + "piedra_capilla");
        t.set(x1 + 1, SOT + 1, -46, C + "piedra_capilla");
        t.set(x1, SOT + 2, -45, C + "telarana");
        esc(t, x1 + 1, SOT + 1, -44);
        // sarcófagos (piedra con tapa de losa, velas encima)
        for (int x : new int[] {21, 31}) {
            for (int z = -44; z <= -41; z++) {
                for (int xx = x; xx <= x + 1; xx++) {
                    t.set(xx, SOT + 1, z, C + "piedra_capilla");
                    t.set(xx, SOT + 2, z, C + "losa_sillar[type=bottom]");
                }
            }
            t.set(x, SOT + 2, -44, C + "piedra_capilla");
            t.set(x + 1, SOT + 2, -44, C + "piedra_capilla");
            t.set(x, SOT + 3, -44, C + "vela");
        }
        esc(t, 23, SOT + 1, -40);
        for (int[] c : new int[][] {{22, -46}, {31, -46}, {26, -39}, {27, -39}, {21, -38}, {32, -38}}) {
            t.set(c[0], SOT + 1, c[1], C + "vela");
        }
        t.set(26, y1 - 1, -44, C + "farol");
        t.set(27, y1 - 1, -44, C + "telarana");
        t.set(21, y1 - 2, -47, C + "telarana");
        t.set(32, y1 - 2, -47, C + "telarana");
        t.luz(26, SOT + 4, -42, 6);
        t.p.letrero(26, SOT + 4, z1 - 1, "letrero", "north", "gold", "PANTEON DE LA", "FAMILIA");
        // la escalera que sube al panteón (x 24..29, sube al sur: z -37 abajo .. -32 arriba, al ras del piso del panteón)
        for (int k = 0; k < SUELO; k++) {
            int y = SOT + 1 + k, z = z1 + k;
            for (int x = 23; x <= 30; x++) {
                boolean lado = x == 23 || x == 30;
                if (lado) {
                    for (int yy = SOT; yy <= SUELO; yy++) {
                        t.set(x, yy, z, C + "piedra_capilla");
                    }
                    continue;
                }
                t.set(x, y, z, C + "escalera_sillar[facing=south,half=bottom]");
                for (int yy = SOT + 1; yy < y; yy++) {
                    t.set(x, yy, z, C + "piedra_capilla");
                }
                for (int yy = y + 1; yy <= SUELO + 1; yy++) {
                    t.set(x, yy, z, "air");
                }
            }
        }
        panteon(t);
    }

    /** El panteón de la familia (encima de la cripta): sillar, vitrales, techo de pizarra de dos aguas y la puerta hacia la casa. */
    private static void panteon(Trazo t) {
        int x0 = 20, x1 = 33, z0 = -41, z1 = -28, y0 = SUELO + 1, alto = 7;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean muro = x == x0 || x == x1 || z == z0 || z == z1;
                boolean hueco = x >= 24 && x <= 29 && z >= -37 && z <= -33;
                if (!hueco) {
                    t.set(x, SUELO, z, muro ? C + "ladrillo_oscuro" : ((x + z) & 1) == 0 ? C + "losa_piedra" : C + "marmol_negro");
                }
                for (int y = y0; y < y0 + alto; y++) {
                    if (muro) {
                        boolean esq = (x == x0 || x == x1) && (z == z0 || z == z1);
                        t.set(x, y, z, y == y0 ? C + "ladrillo_oscuro" : esq || y == y0 + alto - 1 ? C + "cornisa" : C + "piedra_capilla");
                    } else if (!hueco || y > y0 - 1) {
                        t.set(x, y, z, "air");
                    }
                }
            }
        }
        // contrafuertes en las esquinas
        for (int[] c : new int[][] {{x0 - 1, z0}, {x1 + 1, z0}, {x0 - 1, z1}, {x1 + 1, z1}, {x0, z0 - 1}, {x1, z0 - 1}, {x0, z1 + 1}, {x1, z1 + 1}}) {
            for (int y = y0; y < y0 + 4; y++) {
                t.set(c[0], y, c[1], C + "piedra_capilla");
            }
            String f = c[0] < x0 ? "east" : c[0] > x1 ? "west" : c[1] < z0 ? "south" : "north";
            t.set(c[0], y0 + 4, c[1], C + "escalera_sillar[facing=" + f + ",half=bottom]");
        }
        // el barandal de hierro alrededor del hueco de la escalera
        for (int z = -37; z <= -33; z++) {
            t.set(23, y0, z, C + "barrotes");
            t.set(30, y0, z, C + "barrotes");
        }
        for (int x = 23; x <= 30; x++) {
            t.set(x, y0, -38, C + "barrotes");
        }
        // la puerta del sur (hacia la casa): arco de 4, con escalones al revés arriba
        for (int x = 25; x <= 28; x++) {
            for (int y = y0; y < y0 + 4; y++) {
                t.set(x, y, z1, "air");
            }
        }
        t.set(25, y0 + 3, z1, C + "escalera_sillar[facing=west,half=top]");
        t.set(28, y0 + 3, z1, C + "escalera_sillar[facing=east,half=top]");
        t.set(24, y0 + 4, z1 - 0, C + "cornisa");
        for (int x = 24; x <= 29; x++) {
            t.set(x, y0 + 4, z1 + 1, C + "escalera_sillar[facing=north,half=top]");
        }
        t.p.letrero(26, y0 + 4, z1 + 1, "letrero", "south", "gold", "REQUIESCANT");
        // vitrales a los lados y una rosa grande al norte
        for (int z : new int[] {-37, -32}) {
            for (int x : new int[] {x0, x1}) {
                for (int y = y0 + 2; y <= y0 + 4; y++) {
                    t.set(x, y, z, C + "vitral");
                    t.set(x, y, z + 1, C + "vitral");
                }
            }
        }
        for (int x = 25; x <= 28; x++) {
            for (int y = y0 + 3; y <= y0 + 5; y++) {
                t.set(x, y, z0, C + "vitral");
            }
        }
        // adentro: el altar al norte, ángeles en las esquinas, la placa de la familia, candelabros, telarañas
        for (int x = 24; x <= 29; x++) {
            t.set(x, y0, z0 + 1, C + "marmol_negro");
            t.set(x, y0 + 1, z0 + 1, x == 26 || x == 27 ? C + "losa_marmol[type=bottom]" : C + "vela");
        }
        t.set(26, y0 + 2, z0 + 1, C + "craneo");
        t.alto(x0 + 1, y0, z0 + 1, "angel", "south");
        t.alto(x1 - 1, y0, z0 + 1, "angel", "south");
        t.alto(x0 + 1, y0, z1 - 1, "candelabro", "east");
        t.alto(x1 - 1, y0, z1 - 1, "candelabro", "west");
        t.p.texto(x0 + 1, y0 + 2, -35, "placa", "east", 0xC9B98A, "AQUI DESCANSAN", "LOS QUE ESTUVIERON", "ANTES QUE TU");
        t.p.texto(x1 - 1, y0 + 2, -35, "placa", "west", 0xC9B98A, "NINGUNO", "SALIO", "DEL JARDIN");
        t.set(x0 + 1, y0 + 5, z0 + 1, C + "telarana");
        t.set(x1 - 1, y0 + 5, z1 - 1, C + "telarana");
        // detrás del altar y entre los ángeles se esconde uno
        esc(t, 22, y0, z0 + 1);
        esc(t, 31, y0, z1 - 2);
        // el techo: dos aguas de pizarra (la cumbrera corre de norte a sur), con los muros de los frontones
        for (int i = 0; i <= 7; i++) {
            int y = y0 + alto + i, a = x0 - 1 + i, b = x1 + 1 - i;
            if (a > b) {
                break;
            }
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                t.set(a, y, z, C + "escalera_pizarra[facing=east,half=bottom]");
                t.set(b, y, z, C + "escalera_pizarra[facing=west,half=bottom]");
            }
            for (int x = a + 1; x < b; x++) {
                t.set(x, y, z0, C + "piedra_capilla");
                t.set(x, y, z1, C + "piedra_capilla");
            }
        }
        t.set(26, y0 + alto + 7, z1, C + "poste[axis=y]");
        t.set(26, y0 + alto + 8, z1, C + "cuervo[facing=south]");
        t.luz(26, y0 + 4, -30, 9);
        t.luz(22, y0 + 3, -39, 6);
        t.luz(31, y0 + 3, -39, 6);
    }

    // ------------------------------------------------------------------ el jardín de enfrente

    private static void adelante(Trazo t) {
        int y = SUELO + 1;
        // el camino de grava del panteón a la casa, con orilla de sillar
        grava(t, 25, -27, 28, -7);
        for (int z = -27; z <= -7; z++) {
            t.set(24, SUELO, z, C + "losa_piedra");
            t.set(29, SUELO, z, C + "losa_piedra");
        }
        // la fuente: taza redonda de sillar con agua y un ángel en una columna al centro, y un camino alrededor
        double cx = 26.5, cz = -17.5;
        for (int x = 18; x <= 35; x++) {
            for (int z = -27; z <= -8; z++) {
                double d = Math.hypot(x + 0.5 - cx, z + 0.5 - cz);
                if (d < 4.2) {
                    t.set(x, SUELO, z, "water");
                    t.set(x, SUELO - 1, z, C + "losa_piedra");
                } else if (d < 5.2) {
                    t.set(x, SUELO, z, C + "piedra_capilla");
                    t.set(x, y, z, C + "losa_sillar[type=bottom]");
                } else if (d < 7.4) {
                    t.set(x, SUELO, z, C + "grava");
                }
            }
        }
        for (int x = 26; x <= 27; x++) {
            for (int z = -18; z <= -17; z++) {
                for (int yy = SUELO; yy <= y + 2; yy++) {
                    t.set(x, yy, z, C + "piedra_capilla");
                }
                t.set(x, y + 3, z, C + "cornisa");
            }
        }
        for (int x = 25; x <= 28; x++) {
            for (int z = -19; z <= -16; z++) {
                boolean borde = x == 25 || x == 28 || z == -19 || z == -16;
                if (borde) {
                    t.set(x, y + 3, z, C + "losa_sillar[type=top]");
                }
            }
        }
        t.alto(26, y + 4, -18, "angel", "north");
        t.deco(27, y + 4, -17, "cuervo", "south");
        t.luz(26, y + 1, -21, 7);
        t.luz(26, y + 1, -14, 7);
        // los arriates: rosales secos con orilla de seto bajito, a los dos lados del camino
        for (int[] a : new int[][] {{8, -28, 19, -25}, {8, -10, 19, -7}}) {
            for (boolean este : new boolean[] {false, true}) {
                int ax = este ? t.e(a[2]) : a[0], bx = este ? t.e(a[0]) : a[2];
                for (int x = ax; x <= bx; x++) {
                    for (int z = a[1]; z <= a[3]; z++) {
                        boolean orilla = x == ax || x == bx || z == a[1] || z == a[3];
                        if (orilla) {
                            t.set(x, y, z, C + "seto");
                        } else {
                            t.set(x, SUELO, z, C + "tierra");
                            if ((x + z) % 2 == 0) {
                                t.set(x, y, z, C + "rosal");
                            }
                        }
                    }
                }
            }
        }
        // faroles a lo largo del camino y árboles muertos en el pasto
        for (int z : new int[] {-27, -9}) {
            farola(t, 23, z);
            farola(t, 30, z);
        }
        for (int[] a : new int[][] {{4, -22, 9}, {12, -15, 7}, {6, -9, 8}, {16, -30, 8}, {3, -31, 6}}) {
            arbol(t, a[0], a[1], a[2]);
            arbol(t, t.e(a[0]), a[1] + 2, a[2] - 1);
        }
        // bancas mirando a la fuente y una estatua de cuervo
        t.deco(20, y, -18, "banca", "east");
        t.deco(t.e(20), y, -17, "banca", "west");
        t.deco(10, y, -18, "cuervo", "east");
        t.alto(t.e(10), y, -18, "angel", "west");
        // escondites: detrás de los arriates y de los árboles
        esc(t, 7, y, -26);
        esc(t, t.e(7), y, -9);
        esc(t, 2, y, -12);
        // la orilla de la casa: un seto bajo con rosales al pie del frente (menos el pórtico y las torres)
        for (int x = 13; x <= 20; x++) {
            t.set(x, y, Z0 - 1, C + "rosal");
            t.set(t.e(x), y, Z0 - 1, C + "rosal");
        }
    }

    // ------------------------------------------------------------------ el laberinto de setos (al oeste, enfrente)

    private static void laberinto(Trazo t) {
        // celdas de 2 x 2 con muros de 1 (setos de 3 de alto): 5 columnas x 10 filas, de x 2 a 17 y de z -62 a -32
        int cols = 5, filas = 10, x0 = 2, z0 = -62, h = 3;
        int ancho = cols * 3 + 1, largo = filas * 3 + 1;
        seto(t, x0, z0, x0 + ancho - 1, z0 + largo - 1, h);
        boolean[][] visto = new boolean[cols][filas];
        int[][] grado = new int[cols][filas];
        java.util.ArrayDeque<int[]> pila = new java.util.ArrayDeque<>();
        pila.push(new int[] {cols - 1, filas - 1});
        visto[cols - 1][filas - 1] = true;
        abrir(t, x0, z0, cols - 1, filas - 1);
        java.util.List<int[]> puntas = new java.util.ArrayList<>();
        while (!pila.isEmpty()) {
            int[] c = pila.peek();
            java.util.List<int[]> vec = new java.util.ArrayList<>();
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int a = c[0] + d[0], b = c[1] + d[1];
                if (a >= 0 && a < cols && b >= 0 && b < filas && !visto[a][b]) {
                    vec.add(new int[] {a, b, d[0], d[1]});
                }
            }
            if (vec.isEmpty()) {
                pila.pop();
                continue;
            }
            int[] n = vec.get(t.r.nextInt(vec.size()));
            visto[n[0]][n[1]] = true;
            grado[c[0]][c[1]]++;
            grado[n[0]][n[1]]++;
            abrir(t, x0, z0, n[0], n[1]);
            // el muro de en medio
            int mx = x0 + 1 + c[0] * 3 + (n[2] > 0 ? 2 : n[2] < 0 ? -1 : 0), mz = z0 + 1 + c[1] * 3 + (n[3] > 0 ? 2 : n[3] < 0 ? -1 : 0);
            for (int i = 0; i < 2; i++) {
                int xx = n[2] != 0 ? mx : mx + i, zz = n[3] != 0 ? mz : mz + i;
                for (int y = SUELO + 1; y <= SUELO + h; y++) {
                    t.set(xx, y, zz, "air");
                }
                t.set(xx, SUELO, zz, C + "grava");
            }
            pila.push(new int[] {n[0], n[1]});
        }
        // entradas: abajo a la derecha (hacia la fuente) y a la derecha arriba (junto al panteón)
        for (int i = 0; i < 2; i++) {
            int xx = x0 + 1 + (cols - 1) * 3 + i;
            for (int y = SUELO + 1; y <= SUELO + h; y++) {
                t.set(xx, y, z0 + largo - 1, "air");
                t.set(x0 + ancho - 1, y, z0 + 1 + 3 + i, "air");
            }
            t.set(xx, SUELO, z0 + largo - 1, C + "grava");
            t.set(x0 + ancho - 1, SUELO, z0 + 4 + i, C + "grava");
        }
        // en medio (la celda 2, 4): un claro con una banca y una estatua
        int cxm = x0 + 1 + 2 * 3, czm = z0 + 1 + 4 * 3;
        t.deco(cxm, SUELO + 1, czm, "banca", "south");
        t.alto(cxm + 1, SUELO + 1, czm + 1, "angel", "north");
        // los escondites: las puntas sin salida (en su rincón)
        for (int a = 0; a < cols; a++) {
            for (int b = 0; b < filas; b++) {
                boolean entrada = a == cols - 1 && (b == filas - 1 || b == 1);
                if (grado[a][b] == 1 && !entrada && !(a == 2 && b == 4)) {
                    puntas.add(new int[] {a, b});
                }
            }
        }
        java.util.Collections.shuffle(puntas, t.r);
        int n = 0;
        for (int[] c : puntas) {
            if (n >= 7) {
                break;
            }
            esc(t, x0 + 1 + c[0] * 3, SUELO + 1, z0 + 1 + c[1] * 3);
            t.set(x0 + 2 + c[0] * 3, SUELO + 1, z0 + 2 + c[1] * 3, n % 2 == 0 ? C + "rosal" : C + "vela");
            n++;
        }
        esc(t, cxm, SUELO + 1, czm + 1);
        t.luz(x0 + 8, SUELO + 2, z0 + 15, 5);
        t.luz(x0 + 4, SUELO + 2, z0 + 6, 5);
        t.luz(x0 + 12, SUELO + 2, z0 + 25, 5);
    }

    private static void abrir(Trazo t, int x0, int z0, int c, int f) {
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                int x = x0 + 1 + c * 3 + i, z = z0 + 1 + f * 3 + j;
                for (int y = SUELO + 1; y <= SUELO + 3; y++) {
                    t.set(x, y, z, "air");
                }
                t.set(x, SUELO, z, C + "grava");
            }
        }
    }

    // ------------------------------------------------------------------ el cementerio con su capilla (al este, enfrente)

    private static void cementerio(Trazo t) {
        int x0 = 36, x1 = 51, z0 = -62, z1 = -32, y = SUELO + 1;
        // reja de hierro con postes de sillar y una puerta abierta al camino
        for (int x = x0; x <= x1; x++) {
            for (int z : new int[] {z0, z1}) {
                t.set(x, y, z, (x - x0) % 5 == 0 ? C + "ladrillo_oscuro" : C + "barrotes");
            }
        }
        for (int z = z0; z <= z1; z++) {
            t.set(x0, y, z, (z - z0) % 5 == 0 ? C + "ladrillo_oscuro" : C + "barrotes");
        }
        for (int x = 42; x <= 43; x++) {
            t.set(x, y, z1, "air");
        }
        t.set(41, y + 1, z1, C + "farol");
        t.set(44, y + 1, z1, C + "farol");
        for (int z = -40; z <= -39; z++) {
            t.set(x0, y, z, "air");
        }
        grava(t, 42, z1 - 8, 43, z1);
        grava(t, x0, -40, 43, -39);
        // las tumbas en filas, viejas (unas chuecas): lápidas y cruces
        for (int z : new int[] {-35, -43, -47, -51}) {
            for (int x : new int[] {38, 40, 46, 48, 50}) {
                tumba(t, x, z, "south", t.r.nextInt(3) == 0);
            }
        }
        // la capilla de la familia (al fondo): sillar, techo de pizarra, puerta de reja, un altar y dos escondites
        int cx0 = 40, cx1 = 49, cz0 = z0 + 1, cz1 = z0 + 9;
        for (int x = cx0; x <= cx1; x++) {
            for (int z = cz0; z <= cz1; z++) {
                boolean muro = x == cx0 || x == cx1 || z == cz0 || z == cz1;
                t.set(x, SUELO, z, muro ? C + "ladrillo_oscuro" : C + "losa_piedra");
                for (int yy = y; yy < y + 5; yy++) {
                    t.set(x, yy, z, muro ? (yy == y + 4 ? C + "cornisa" : C + "piedra_capilla") : "air");
                }
            }
        }
        for (int i = 0; i <= 5; i++) {
            int a = cx0 - 1 + i, b = cx1 + 1 - i;
            for (int z = cz0 - 1; z <= cz1 + 1; z++) {
                t.set(a, y + 5 + i, z, C + "escalera_pizarra[facing=east,half=bottom]");
                t.set(b, y + 5 + i, z, C + "escalera_pizarra[facing=west,half=bottom]");
            }
            for (int x = a + 1; x < b; x++) {
                t.set(x, y + 5 + i, cz0, C + "piedra_capilla");
                t.set(x, y + 5 + i, cz1, C + "piedra_capilla");
            }
        }
        for (int x = 44; x <= 45; x++) {
            for (int yy = y; yy < y + 3; yy++) {
                t.set(x, yy, cz1, "air");
            }
        }
        t.set(44, y + 2, cz1, C + "escalera_sillar[facing=west,half=top]");
        t.set(45, y + 2, cz1, C + "escalera_sillar[facing=east,half=top]");
        for (int x = 43; x <= 46; x++) {
            t.set(x, y + 2, cz0, C + "vitral");
            t.set(x, y + 3, cz0, C + "vitral");
        }
        for (int x = 43; x <= 46; x++) {
            t.set(x, y, cz0 + 1, C + "marmol_negro");
            t.set(x, y + 1, cz0 + 1, x == 44 || x == 45 ? C + "vela" : C + "craneo");
        }
        t.alto(cx0 + 1, y, cz0 + 1, "angel", "south");
        t.alto(cx1 - 1, y, cz0 + 1, "angel", "south");
        t.p.texto(cx0 + 1, y + 2, cz0 + 4, "placa", "east", 0xC9B98A, "LA COLECCION", "COMPLETA");
        t.set(cx1 - 1, y + 3, cz1 - 1, C + "telarana");
        t.luz(44, y + 3, cz0 + 4, 7);
        esc(t, cx0 + 1, y, cz1 - 1);
        esc(t, cx1 - 1, y, cz0 + 3);
        // árboles muertos y cuervos entre las tumbas, y un escondite atrás de un árbol
        arbol(t, 39, -45, 9);
        arbol(t, 50, -40, 8);
        arbol(t, 38, z0 + 4, 7);
        t.deco(51, y, -45, "cuervo", "west");
        t.deco(37, y, -34, "cuervo", "east");
        esc(t, 51, y, z0 + 11);
        esc(t, 37, y, -53);
        t.luz(43, y + 2, -45, 6);
    }

    // ------------------------------------------------------------------ los pasillos de los lados de la casa

    private static void lados(Trazo t) {
        int y = SUELO + 1;
        for (boolean este : new boolean[] {false, true}) {
            int a = este ? 51 : 1, b = este ? 52 : 2;
            grava(t, a, -6, b, Z1 + 4);
            // árboles y setos contra el muro
            for (int z = 30; z <= 40; z += 5) {
                arbol(t, este ? 50 : 3, z, 6);
            }
            farola(t, este ? 49 : 4, -4);
            farola(t, este ? 49 : 4, Z1 + 2);
        }
        // al oeste: la leña (troncos apilados) y un escondite detrás
        for (int z = 18; z <= 22; z++) {
            for (int yy = y; yy <= y + 1; yy++) {
                t.set(2, yy, z, C + "tronco[axis=z]");
                t.set(3, yy, z, C + "tronco[axis=z]");
            }
        }
        t.set(3, y + 2, 19, C + "tronco[axis=z]");
        esc(t, 4, y, 20);
        // al este: barriles y cajones del servicio, y la tapa del sótano (cerrada)
        for (int z = 16; z <= 18; z++) {
            t.set(51, y, z, C + "barril[facing=west]");
        }
        t.set(52, y, 17, C + "barril[facing=west]");
        t.set(51, y + 1, 16, C + "cajon[facing=west]");
        esc(t, 52, y, 19);
        for (int z = 26; z <= 27; z++) {
            t.set(50, SUELO, z, C + "trampilla[facing=west,half=top,open=false]");
            t.set(49, SUELO, z, C + "trampilla[facing=east,half=top,open=false]");
        }
    }

    // ------------------------------------------------------------------ atrás de la casa

    private static void atras(Trazo t) {
        int y = SUELO + 1;
        // la terraza (al ras del piso de la casa) con su balaustrada y la escalinata al jardín
        for (int x = 19; x <= 34; x++) {
            for (int z = Z1 + 1; z <= Z1 + 5; z++) {
                t.set(x, PB, z, ((x + z) & 1) == 0 ? C + "losa_piedra" : C + "marmol_negro");
                boolean orilla = x == 19 || x == 34 || z == Z1 + 5;
                if (orilla && !(z == Z1 + 5 && x >= 24 && x <= 29)) {
                    t.set(x, PB + 1, z, C + "balaustrada");
                }
            }
        }
        for (int x = 24; x <= 29; x++) {
            t.set(x, PB, Z1 + 6, C + "escalera_sillar[facing=north,half=bottom]");
        }
        for (int x : new int[] {19, 34}) {
            t.set(x, PB + 1, Z1 + 5, C + "poste[axis=y]");
            t.set(x, PB + 2, Z1 + 5, C + "farol");
        }
        t.deco(21, PB + 1, Z1 + 2, "banca", "east");
        t.deco(32, PB + 1, Z1 + 2, "banca", "west");
        t.alto(20, PB + 1, Z1 + 4, "planta", "south");
        t.alto(33, PB + 1, Z1 + 4, "planta", "south");
        // el camino de atrás hasta la glorieta
        grava(t, 25, Z1 + 7, 28, 82);
        for (int z = Z1 + 10; z <= 80; z += 10) {
            farola(t, 24, z);
            farola(t, 29, z);
        }
        invernadero(t);
        estanque(t);
        glorieta(t);
        cobertizo(t);
        mascotas(t);
        // árboles y setos de atrás
        seto(t, 1, TZ1 - 1, 52, TZ1 - 1, 2);
        for (int[] a : new int[][] {{22, 60, 7}, {8, 86, 8}, {16, 90, 6}, {34, 90, 9}, {51, 58, 7}, {3, 56, 6}}) {
            arbol(t, a[0], a[1], a[2]);
        }
        esc(t, 23, y, 61);
    }

    /** El invernadero (atrás al oeste): base de sillar, muros y techo de vidrio con armazón de hierro; mesas, macetas y plantas secas. */
    private static void invernadero(Trazo t) {
        int x0 = 3, x1 = 19, z0 = 62, z1 = 80, y = SUELO + 1, alto = 5;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean muro = x == x0 || x == x1 || z == z0 || z == z1;
                boolean poste = muro && ((x - x0) % 4 == 0 && (z == z0 || z == z1) || (z - z0) % 4 == 0 && (x == x0 || x == x1));
                t.set(x, SUELO, z, muro ? C + "ladrillo_oscuro" : ((x + z) & 1) == 0 ? C + "losa_piedra" : C + "piso_cloaca");
                for (int yy = y; yy < y + alto; yy++) {
                    String b = "air";
                    if (muro) {
                        b = poste ? C + "metal_negro" : yy == y ? C + "piedra_capilla" : C + "vidrio_sucio";
                    }
                    t.set(x, yy, z, b);
                }
            }
        }
        // el techo de dos aguas de vidrio (con vigas de hierro cada 4)
        for (int i = 0; i <= (x1 - x0) / 2; i++) {
            int a = x0 + i, b = x1 - i, yy = y + alto + i / 2;
            for (int z = z0; z <= z1; z++) {
                String v = (z - z0) % 4 == 0 ? C + "metal_negro" : C + "vidrio_sucio";
                t.set(a, yy, z, v);
                t.set(b, yy, z, v);
            }
        }
        // la puerta (al este, hacia el camino) y una al sur
        for (int z = 70; z <= 71; z++) {
            t.set(x1, y, z, "air");
            t.set(x1, y + 1, z, "air");
            t.set(x1, y + 2, z, C + "metal_negro");
        }
        grava(t, x1 + 1, 70, 24, 71);
        // adentro: mesas largas con macetas, arriates de tierra con rosales y plantas, un árbol seco al centro
        for (int z = z0 + 2; z <= z1 - 2; z++) {
            if (z == 70 || z == 71) {
                continue;
            }
            t.set(x0 + 1, SUELO, z, C + "tierra");
            t.set(x0 + 1, y, z, z % 2 == 0 ? C + "rosal" : "air");
            t.set(x1 - 1, y, z, C + "mesa[facing=west]");
            if (z % 3 == 0) {
                t.set(x1 - 1, y + 1, z, C + "vela");
            }
        }
        for (int z = z0 + 3; z <= z1 - 3; z += 4) {
            t.alto(x0 + 5, y, z, "planta", "east");
            t.alto(x0 + 9, y, z + 1, "planta", "west");
        }
        arbol(t, 11, 71, 4);
        t.set(11, SUELO, 71, C + "tierra");
        t.set(10, y, 72, C + "rosal");
        t.set(12, y, 70, C + "rosal");
        t.deco(14, y, z1 - 1, "banca", "north");
        t.set(x0 + 1, y + 3, z0 + 1, C + "telarana");
        t.luz(11, y + 3, 66, 6);
        t.luz(11, y + 3, 76, 6);
        esc(t, x0 + 1, y, z1 - 1);
        esc(t, x0 + 6, y, z0 + 1);
        esc(t, x1 - 2, y, z1 - 1);
    }

    /** El estanque (atrás al este): agua negra con orilla de piedra, un puente de madera y una islita con un árbol muerto. */
    private static void estanque(Trazo t) {
        double cx = 40.5, cz = 70.5;
        for (int x = 31; x <= 51; x++) {
            for (int z = 58; z <= 84; z++) {
                double dx = (x + 0.5 - cx) / 9.0, dz = (z + 0.5 - cz) / 11.0;
                double ondula = 0.12 * Math.sin(z * 0.7) + 0.1 * Math.cos(x * 0.9);
                double d = Math.sqrt(dx * dx + dz * dz) + ondula;
                if (d < 0.88) {
                    t.set(x, SUELO, z, "water");
                    t.set(x, SUELO - 1, z, d < 0.6 ? "water" : C + "lodo");
                    t.set(x, SUELO - 2, z, C + "lodo");
                } else if (d < 1.0) {
                    t.set(x, SUELO, z, C + "losa_piedra");
                }
            }
        }
        // la islita
        for (int x = 42; x <= 44; x++) {
            for (int z = 74; z <= 76; z++) {
                t.set(x, SUELO, z, C + "pasto");
                t.set(x, SUELO - 1, z, C + "tierra");
            }
        }
        arbol(t, 43, 75, 6);
        // el puente (de oeste a este, z 66..67): tablones un bloque arriba del agua, con rampa en cada orilla y barandal
        for (int x = 31; x <= 50; x++) {
            for (int z = 65; z <= 68; z++) {
                boolean orilla = z == 65 || z == 68;
                boolean rampa = x == 31 || x == 50;
                if (x > 31 && x < 50 && !orilla) {
                    t.set(x, SUELO, z, "water");
                }
                if (rampa) {
                    if (!orilla) {
                        t.set(x, SUELO + 1, z, C + "escalera_tablon[facing=" + (x == 31 ? "east" : "west") + ",half=bottom]");
                    }
                    continue;
                }
                t.set(x, SUELO + 1, z, orilla ? C + "losa_tablon[type=top]" : C + "tablon");
                if (orilla && x % 3 == 0) {
                    t.set(x, SUELO, z, C + "tronco[axis=y]");
                }
                if (orilla) {
                    t.set(x, SUELO + 2, z, C + "balaustrada");
                }
            }
        }
        grava(t, 29, 66, 31, 67);
        t.set(49, SUELO + 3, 65, C + "farol");
        t.set(32, SUELO + 3, 68, C + "farol");
        t.deco(46, SUELO + 1, 62, "banca", "south");
        t.deco(36, SUELO + 1, 80, "cuervo", "north");
        esc(t, 51, SUELO + 1, 80);
    }

    /** La glorieta al fondo del camino: piso de mármol, columnas, techo de pizarra en pirámide y una banca. */
    private static void glorieta(Trazo t) {
        int x0 = 22, x1 = 31, z0 = 83, z1 = 92, y = SUELO + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean esq = (x == x0 || x == x1) && (z == z0 || z == z1);
                if (!esq) {
                    t.set(x, SUELO, z, ((x + z) & 1) == 0 ? C + "marmol_crema" : C + "losa_piedra");
                }
            }
        }
        for (int[] c : new int[][] {{x0 + 1, z0 + 1}, {x1 - 1, z0 + 1}, {x0 + 1, z1 - 1}, {x1 - 1, z1 - 1}}) {
            for (int yy = y; yy <= y + 3; yy++) {
                t.set(c[0], yy, c[1], C + "viga_madera[axis=y]");
            }
        }
        for (int i = 0; i <= 4; i++) {
            int a = x0 + i, b = x1 - i, c = z0 + i, d = z1 - i, yy = y + 4 + i;
            for (int x = a; x <= b; x++) {
                t.set(x, yy, c, C + "escalera_pizarra[facing=south,half=bottom]");
                t.set(x, yy, d, C + "escalera_pizarra[facing=north,half=bottom]");
            }
            for (int z = c + 1; z < d; z++) {
                t.set(a, yy, z, C + "escalera_pizarra[facing=east,half=bottom]");
                t.set(b, yy, z, C + "escalera_pizarra[facing=west,half=bottom]");
            }
        }
        t.set(26, y + 9, 87, C + "poste[axis=y]");
        for (int yy = y + 4; yy <= y + 7; yy++) {
            t.set(26, yy, 87, C + "cadena[axis=y]");
        }
        t.set(26, y + 3, 87, C + "farol");
        t.deco(26, y, z1 - 2, "banca", "north");
        t.deco(27, y, z1 - 2, "banca", "north");
        t.set(x0 + 2, y, z0 + 2, C + "rosal");
        t.set(x1 - 2, y, z0 + 2, C + "rosal");
        esc(t, x1 - 2, y, z1 - 2);
    }

    /** El cobertizo de herramientas (atrás al este): tablones, techo de una sola agua, barriles, cajones y una carretilla de leña. */
    private static void cobertizo(Trazo t) {
        int x0 = 43, x1 = 51, z0 = 86, z1 = 93, y = SUELO + 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean muro = x == x0 || x == x1 || z == z0 || z == z1;
                t.set(x, SUELO, z, C + "tablon");
                for (int yy = y; yy < y + 4; yy++) {
                    t.set(x, yy, z, muro ? (x == x0 || x == x1 ? C + "viga_madera[axis=y]" : C + "tablon") : "air");
                }
                t.set(x, y + 4 + (z - z0) / 4, z, C + "losa_tablon[type=bottom]");
            }
        }
        for (int x = 46; x <= 47; x++) {
            t.set(x, y, z0, "air");
            t.set(x, y + 1, z0, "air");
        }
        t.set(x0 + 1, y, z1 - 1, C + "barril[facing=north]");
        t.set(x0 + 2, y, z1 - 1, C + "barril[facing=north]");
        t.set(x0 + 1, y + 1, z1 - 1, C + "cajon[facing=north]");
        t.set(x1 - 1, y, z1 - 1, C + "cajon[facing=north]");
        t.set(x1 - 1, y, z0 + 2, C + "mesa[facing=west]");
        t.set(x1 - 1, y + 1, z0 + 2, C + "farol");
        t.set(x0 + 1, y + 2, z0 + 1, C + "telarana");
        t.luz(47, y + 2, 90, 5);
        esc(t, x1 - 1, y, z1 - 2);
        esc(t, x0 + 1, y, z0 + 2);
    }

    /** El cementerio de las mascotas de antes (junto a la glorieta): crucecitas, un collar en una lápida y un letrero. */
    private static void mascotas(Trazo t) {
        int y = SUELO + 1;
        for (int x = 33; x <= 41; x++) {
            t.set(x, y, 84, C + "barrotes");
            t.set(x, y, 93, C + "barrotes");
        }
        for (int z = 84; z <= 93; z++) {
            t.set(33, y, z, z == 88 || z == 89 ? "air" : C + "barrotes");
            t.set(41, y, z, C + "barrotes");
        }
        for (int x = 35; x <= 39; x += 2) {
            for (int z = 86; z <= 91; z += 3) {
                tumba(t, x, z, "west", true);
            }
        }
        t.p.texto(36, y + 1, 85, "placa", "south", 0xB5A88A, "CASCABEL I", "CASCABEL II", "CASCABEL III");
        t.set(35, y, 85, C + "vela");
        esc(t, 40, y, 92);
    }
}
