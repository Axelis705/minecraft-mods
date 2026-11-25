package com.coleccionista.mapa;

/**
 * Las dos alas nuevas de la casa del Coleccionista (capítulo 2): una al norte y otra al sur del cuerpo de la casa, cada una con su pasillo
 * largo en los dos pisos, tres cuartos por piso, una escalera de servicio (con un cuartito escondido debajo) y un ático polvoso arriba
 * (se sube por otra escalera desde el pasillo de arriba). Se entra desde los cuartos de la casa por puertas en la pared del fondo (y en la
 * biblioteca, por un hueco secreto detrás de los libreros).
 * <p>
 * Se escribe una sola vez en coordenadas locales ({@link Marco}): {@code i} a lo largo del ala (0..44) y {@code k} alejándose de la casa:
 * k 0 el muro doble, k 1..3 el pasillo, k 4 el muro, k 5..14 los cuartos, k 15 el muro de afuera. Abajo el piso está en y 0 (alto 6), arriba
 * en y 7 (alto 5) y el ático en y 13 (alto 4). Cuartos: A i 1..14, B i 16..29, C i 31..43. La escalera de servicio sube por el pasillo
 * de abajo de i 36 a i 42 (y llega a un descanso en i 43, junto a la puerta del cuarto C de arriba); la del ático, por el pasillo de arriba,
 * de i 6 a i 1.
 */
final class MansionAlas {
    private static final String C = "coleccionista:";
    private static final String TABLA = "minecraft:dark_oak_planks";
    private final Plano p;
    private final Marco m;
    private final boolean norte;

    private MansionAlas(Plano p, boolean norte) {
        this.p = p;
        this.norte = norte;
        // sur: i 0 en x 6, k 0 en z 45 (giro 0); norte: i 0 en x 50, k 0 en z -2 (giro 180)
        this.m = norte ? new Marco(51, -1, 180) : new Marco(6, 45, 0);
    }

    static void construir(Plano p) {
        new MansionAlas(p, false).ala();
        new MansionAlas(p, true).ala();
        puertasCasa(p);
    }

    // ------------------------------------------------------------------ utilidades locales

    private void set(int i, int y, int k, String b) {
        m.set(p, i, y, k, b);
    }

    private int[] w(int i, int k) {
        return m.b(i, k);
    }

    /** Un cuarto (o pasillo) rectangular en local: lo vacía y le pone piso, techo y paredes (con su luz). */
    private void cuarto(int i0, int i1, int k0, int k1, int y0, int alto, String piso, String muro) {
        int[] a = w(i0, k0), b = w(i1, k1);
        Mansion.cuarto(p, Math.min(a[0], b[0]), Math.min(a[1], b[1]), y0, Math.max(a[0], b[0]), Math.max(a[1], b[1]), alto, piso, muro);
    }

    /** Un hueco de puerta (de {@code ancho} a lo largo de i, 3 de alto) que atraviesa la pared k. */
    private void puerta(int i, int k, int y, int ancho) {
        for (int a = 0; a < ancho; a++) {
            for (int yy = y; yy < y + 3; yy++) {
                set(i + a, yy, k, "air");
            }
            set(i + a, y - 1, k, C + "madera_pulida");
        }
    }

    private void armario(int i, int y0, int k, String miraLocal) {
        int[] b = w(i, k);
        Mansion.armario(p, b[0], y0, b[1], m.f(miraLocal));
    }

    /** Un nicho con cortina: la cortina en (i, k) y el hueco en (mi, mk), dentro de la pared. */
    private void nicho(int i, int y0, int k, int mi, int mk, String miraLocal) {
        int[] b = w(i, k), h = w(mi, mk);
        Mansion.nicho(p, b[0], y0, b[1], h[0], h[1], m.f(miraLocal));
    }

    private void escondite(int i, int y, int k) {
        int[] b = w(i, k);
        Mansion.escondite(p, b[0], y, b[1]);
    }

    private void cortina(int i, int y0, int k, String miraLocal) {
        set(i, y0 + 1, k, C + "cortina[facing=" + miraLocal + ",half=lower]");
        set(i, y0 + 2, k, C + "cortina[facing=" + miraLocal + ",half=upper]");
    }

    private void doble(int i, int y0, int k, String bloque, String miraLocal) {
        set(i, y0 + 1, k, C + bloque + "[facing=" + miraLocal + ",half=lower]");
        set(i, y0 + 2, k, C + bloque + "[facing=" + miraLocal + ",half=upper]");
    }

    // ------------------------------------------------------------------ el ala

    private void ala() {
        // los pasillos (abajo y arriba), los cuartos y el ático
        cuarto(1, 43, 1, 3, 0, 6, C + "parquet", C + "papel_tapiz");
        cuarto(1, 43, 1, 3, 7, 5, C + "alfombra_roja", C + "papel_tapiz");
        if (norte) {
            cuarto(1, 14, 5, 14, 0, 6, C + "azulejo", C + "azulejo_remate");
            cuarto(16, 29, 5, 14, 0, 6, C + "linoleo", C + "azulejo");
            cuarto(31, 43, 5, 14, 0, 6, C + "concreto_piso", C + "ladrillo_viejo");
            cuarto(1, 14, 5, 14, 7, 5, C + "parquet", C + "panel_madera");
            cuarto(16, 29, 5, 14, 7, 5, C + "parquet", C + "papel_tapiz");
            cuarto(31, 43, 5, 14, 7, 5, C + "parquet", C + "papel_tapiz");
        } else {
            // abajo, el salón de baile ocupa A y B juntos
            cuarto(1, 29, 5, 14, 0, 6, C + "parquet", C + "papel_tapiz");
            cuarto(31, 43, 5, 14, 0, 6, C + "parquet", C + "panel_madera");
            cuarto(1, 14, 5, 14, 7, 5, C + "parquet", C + "panel_madera");
            cuarto(16, 29, 5, 14, 7, 5, C + "alfombra_roja", C + "acolchado");
            cuarto(31, 43, 5, 14, 7, 5, C + "parquet", C + "papel_tapiz");
        }
        cuarto(1, 43, 1, 14, 13, 4, C + "tablon", C + "tablon");
        // los muros dobles entre la casa y el pasillo (k 0) se quedan de ladrillo; puertas del pasillo a los cuartos (k 4)
        for (int y : new int[] {1, 8}) {
            puerta(7, 4, y, 2);
            puerta(22, 4, y, 2);
            puerta(33, 4, y, 2);
        }
        escaleraServicio();
        escaleraAtico();
        atico();
        if (norte) {
            invernadero();
            lavanderia();
            calderas();
            estudio();
            bebe();
            costura();
        } else {
            baile();
            billar();
            retratos();
            guardarropa();
            huespedes();
        }
        // en los pasillos: retratos (placas) torcidos, relojes y un tapete al centro
        for (int i : new int[] {4, 12, 19, 27}) {
            set(i, 3, 3, C + "reloj[facing=north]");
        }
        for (int i : new int[] {9, 13, 19, 28}) {
            set(i, 10, 3, C + "espejo[facing=north]");
        }
        for (int i = 2; i <= 35; i++) {
            set(i, 0, 2, C + "tapete_rojo");
        }
        // un armario en el pasillo de abajo (al fondo, junto al muro de la casa) y un nicho en el de arriba
        armario(2, 0, 2, "east");
        nicho(30, 7, 1, 30, 0, "south");
    }

    /** La escalera de servicio: del pasillo de abajo (i 36, y 1) al descanso de arriba (i 43, y 7); debajo, un cuartito escondido. */
    private void escaleraServicio() {
        for (int s = 0; s <= 6; s++) {
            int i = 36 + s, y = 1 + s;
            for (int k = 1; k <= 3; k++) {
                set(i, y, k, C + "escalera_tablon[facing=east]");
                for (int yy = y + 1; yy <= 12; yy++) {
                    set(i, yy, k, "air");
                }
                // abajo de los primeros escalones, macizo; de i 40 en adelante, hueco (el cuartito)
                for (int yy = 1; yy < y; yy++) {
                    set(i, yy, k, i < 40 ? C + "madera_pulida" : "air");
                }
            }
        }
        // el descanso (i 43) y el barandal del hueco arriba (i 35, en el pasillo de arriba)
        for (int k = 1; k <= 3; k++) {
            set(43, 7, k, C + "parquet");
            for (int y = 1; y <= 6; y++) {
                set(43, y, k, "air");
            }
            set(35, 8, k, C + "barandal");
        }
        // el cuartito debajo de la escalera: se entra desde el cuarto C de abajo (por una puerta baja)
        set(43, 0, 2, C + "parquet");
        puerta(42, 4, 1, 2);
        set(41, 1, 3, C + "barril[facing=west]");
        set(43, 1, 1, C + "telarana");
        set(42, 3, 3, C + "telarana");
        escondite(42, 1, 2);
        escondite(43, 1, 3);
        // la puerta del descanso al cuarto C de arriba
        puerta(42, 4, 8, 2);
    }

    /** La escalera al ático: por el pasillo de arriba, de i 6 (y 8) a i 1 (y 13); arriba, barandal alrededor del hueco. */
    private void escaleraAtico() {
        for (int s = 0; s <= 5; s++) {
            int i = 6 - s, y = 8 + s;
            for (int k = 2; k <= 3; k++) {
                set(i, y, k, C + "escalera_tablon[facing=west]");
                for (int yy = y + 1; yy <= 16; yy++) {
                    set(i, yy, k, "air");
                }
                for (int yy = 8; yy < y; yy++) {
                    set(i, yy, k, C + "madera_pulida");
                }
            }
        }
        for (int i = 2; i <= 6; i++) {
            set(i, 14, 1, C + "barandal");
            set(i, 14, 4, C + "barandal");
        }
        set(7, 14, 2, C + "barandal");
        set(7, 14, 3, C + "barandal");
    }

    /** El ático: baúles, muebles tapados, muñecas viejas, telarañas y muy poca luz. Muchos rincones para esconderse. */
    private void atico() {
        // vigas del techo
        for (int i = 3; i <= 41; i += 4) {
            for (int k = 1; k <= 14; k++) {
                set(i, 17, k, C + "viga_madera[axis=z]");
            }
        }
        // baúles y cajas en filas, con huecos detrás
        int[][] pilas = {{10, 6}, {10, 11}, {18, 3}, {18, 9}, {26, 6}, {26, 12}, {34, 4}, {34, 10}, {40, 7}};
        for (int[] q : pilas) {
            set(q[0], 14, q[1], C + "barril[facing=north]");
            set(q[0] + 1, 14, q[1], C + "barril[facing=north]");
            set(q[0], 15, q[1], C + "barril[facing=north]");
            set(q[0], 14, q[1] + 1, C + "cajon[facing=north]");
            escondite(q[0] + 1, 14, q[1] + 1);
        }
        // muebles tapados con sábanas (lana blanca) y muñecas viejas mirando
        for (int[] q : new int[][] {{14, 13}, {22, 2}, {30, 13}, {38, 2}}) {
            set(q[0], 14, q[1], "minecraft:white_wool");
            set(q[0] + 1, 14, q[1], "minecraft:white_wool");
            set(q[0], 15, q[1], "minecraft:white_carpet");
        }
        for (int[] q : new int[][] {{13, 8}, {21, 13}, {29, 3}, {37, 13}, {42, 4}}) {
            set(q[0], 14, q[1], C + "muneca[facing=" + (q[1] > 7 ? "north" : "south") + "]");
        }
        for (int i = 2; i <= 42; i += 5) {
            set(i, 16, 1, C + "telarana");
            set(i + 2, 16, 14, C + "telarana");
        }
        // un armario viejo al fondo y una cuna rota
        armario(43, 13, 13, "west");
        set(24, 14, 8, C + "catre[facing=east,part=head]");
        set(23, 14, 8, C + "catre[facing=east,part=foot]");
        // la luz: muy poca (un farol que falla)
        for (int i = 8; i <= 40; i += 11) {
            set(i, 16, 7, C + "farol[facing=north,encendida=true,falla=" + (i % 2 == 0) + "]");
            set(i, 15, 8, "light[level=5]");
        }
    }

    // ------------------------------------------------------------------ ala norte

    /** El invernadero (abajo, A): plantas, macetas, un estanque seco y arbustos huecos donde cabe uno. */
    private void invernadero() {
        for (int i = 2; i <= 13; i++) {
            for (int k = 6; k <= 13; k++) {
                if ((i + k) % 5 == 0) {
                    set(i, 1, k, "minecraft:potted_fern");
                }
            }
        }
        // arbustos de hojas (dos de ellos huecos: un escondite)
        for (int[] q : new int[][] {{3, 12}, {9, 12}}) {
            for (int di = -1; di <= 1; di++) {
                for (int dk = -1; dk <= 1; dk++) {
                    for (int y = 1; y <= 3; y++) {
                        set(q[0] + di, y, q[1] + dk, "minecraft:azalea_leaves[persistent=true]");
                    }
                }
            }
            set(q[0], 1, q[1], "air");
            set(q[0], 2, q[1], "air");
            set(q[0], 1, q[1] - 1, "air");
            set(q[0], 2, q[1] - 1, "air");
            escondite(q[0], 1, q[1]);
        }
        // el estanque seco con su estatua (una muñeca de piedra)
        for (int i = 6; i <= 8; i++) {
            for (int k = 7; k <= 9; k++) {
                set(i, 0, k, C + "marmol_crema");
            }
        }
        set(7, 1, 8, C + "muneca[facing=north]");
        set(5, 1, 6, C + "silla[facing=east]");
        set(10, 1, 6, C + "silla[facing=west]");
        set(13, 4, 14, "minecraft:vine[north=true]");
        set(2, 4, 14, "minecraft:vine[north=true]");
    }

    /** La lavandería (abajo, B): tinas, tendederos con sábanas (se puede uno esconder detrás) y un ducto para la ropa. */
    private void lavanderia() {
        for (int i = 17; i <= 20; i++) {
            set(i, 1, 14, C + "barril[facing=north]");
        }
        set(17, 2, 14, C + "lavabo[facing=north]");
        // tendederos: sábanas colgadas (cortinas) en dos filas
        for (int i = 19; i <= 27; i++) {
            if (i % 4 != 0) {
                cortina(i, 0, 8, "north");
                cortina(i, 0, 11, "south");
            }
        }
        escondite(21, 1, 9);
        escondite(25, 1, 10);
        // el ducto de la ropa sucia (un hueco en la pared del fondo, tapado)
        nicho(28, 0, 14, 28, 15, "north");
        armario(17, 0, 6, "east");
    }

    /** El cuarto de calderas (abajo, C): la caldera, tubos, carbón, y un hueco entre la caldera y la pared. */
    private void calderas() {
        for (int i = 35; i <= 39; i++) {
            for (int k = 11; k <= 13; k++) {
                for (int y = 1; y <= 3; y++) {
                    set(i, y, k, (i + y) % 3 == 0 ? C + "cobre_viejo" : C + "metal_negro");
                }
            }
        }
        for (int y = 4; y <= 6; y++) {
            set(37, y, 12, C + "tubo[axis=y]");
        }
        for (int i = 32; i <= 42; i++) {
            set(i, 5, 14, C + "tubo[axis=x]");
        }
        set(35, 1, 14, "air");
        set(35, 2, 14, "air");
        set(36, 1, 14, "air");
        set(36, 2, 14, "air");
        escondite(35, 1, 14);
        set(41, 1, 6, C + "barril[facing=west]");
        set(41, 2, 6, C + "barril[facing=west]");
        set(42, 1, 9, C + "argolla[facing=west]");
        set(38, 2, 10, "light[level=7]");
        set(33, 1, 13, C + "cofre_boveda[facing=east]");
    }

    /** El estudio del Coleccionista (arriba, A): escritorio, su trono, vitrinas y libreros; detrás de uno, un cuartito secreto. */
    private void estudio() {
        set(7, 8, 12, C + "mesa[facing=north]");
        set(8, 8, 12, C + "mesa[facing=north]");
        doble(7, 7, 13, "trono", "north");
        set(8, 9, 12, C + "papeles[facing=north]");
        set(7, 9, 12, C + "vela");
        for (int i = 2; i <= 5; i++) {
            doble(i, 7, 14, "vitrina", "north");
        }
        for (int i = 10; i <= 13; i++) {
            doble(i, 7, 14, "vitrina", "north");
        }
        // los libreros del lado de la casa (i 1) con un hueco tapado por una cortina: el cuartito secreto en el muro
        for (int k = 5; k <= 14; k++) {
            for (int y = 8; y <= 11; y++) {
                set(1, y, k, C + "estante_archivo");
            }
        }
        for (int y = 8; y <= 9; y++) {
            set(1, y, 9, "air");
            set(0, y, 9, "air");
            set(0, y, 10, "air");
        }
        cortina(1, 7, 9, "east");
        escondite(0, 8, 10);
        set(13, 8, 6, C + "craneo");
        set(3, 8, 6, C + "atril[facing=east]");
    }

    /** El cuarto del bebé (arriba, B): cunas, muñecas por todos lados, un reloj, y un ropero. */
    private void bebe() {
        for (int[] q : new int[][] {{18, 12}, {22, 12}, {26, 12}}) {
            set(q[0], 8, q[1], C + "catre[facing=north,part=head]");
            set(q[0], 8, q[1] + 1, C + "catre[facing=north,part=foot]");
        }
        for (int i = 17; i <= 28; i += 2) {
            set(i, 8, 6, C + "muneca[facing=south]");
        }
        set(20, 8, 9, C + "muneca[facing=north]");
        set(25, 8, 9, C + "muneca[facing=north]");
        set(29, 10, 10, C + "reloj[facing=west]");
        armario(17, 7, 9, "east");
        armario(28, 7, 7, "west");
    }

    /** El cuarto de costura (arriba, C): mesas con telas, muñecas a medio coser y un baúl grande. */
    private void costura() {
        set(36, 8, 9, C + "mesa_tapete[facing=north]");
        set(37, 8, 9, C + "mesa_tapete[facing=north]");
        set(36, 9, 9, C + "papeles[facing=north]");
        set(38, 8, 12, C + "muneca[facing=north]");
        set(40, 8, 12, C + "muneca[facing=north]");
        set(39, 8, 6, C + "silla[facing=south]");
        cortina(33, 7, 14, "north");
        cortina(34, 7, 14, "north");
        nicho(41, 7, 14, 41, 15, "north");
        armario(42, 7, 10, "west");
    }

    // ------------------------------------------------------------------ ala sur

    /** El salón de baile (abajo, A+B): la araña, espejos, cortinas largas con nichos detrás y figuras de baile. */
    private void baile() {
        for (int i = 3; i <= 27; i += 6) {
            nicho(i, 0, 14, i, 15, "north");
            set(i + 3, 3, 14, C + "espejo[facing=north]");
        }
        for (int i = 2; i <= 28; i++) {
            for (int k = 6; k <= 13; k++) {
                if ((i + k) % 2 == 0) {
                    set(i, 0, k, C + "marmol_negro");
                } else {
                    set(i, 0, k, C + "marmol_crema");
                }
            }
        }
        set(15, 6, 10, C + "cadena");
        set(15, 5, 10, C + "arana");
        for (int[] q : new int[][] {{9, 8}, {11, 11}, {19, 8}, {21, 11}}) {
            set(q[0], 1, q[1], C + "muneca[facing=" + (q[1] > 9 ? "north" : "south") + "]");
        }
        // un estrado con el gramófono (el atril) y sillas alrededor de la pista
        set(27, 1, 9, C + "atril[facing=west]");
        for (int i = 3; i <= 27; i += 4) {
            set(i, 1, 5, C + "silla[facing=south]");
        }
        armario(28, 0, 5, "west");
    }

    /** La sala de billar (abajo, C): la mesa con su lámpara, sillones y una barra. */
    private void billar() {
        for (int i = 35; i <= 38; i++) {
            for (int k = 9; k <= 10; k++) {
                set(i, 1, k, C + "mesa_tapete[facing=east]");
            }
        }
        set(36, 4, 9, C + "lampara_billar[facing=east]");
        set(37, 4, 10, C + "lampara_billar[facing=east]");
        doble(32, 0, 12, "sillon", "east");
        doble(32, 0, 7, "sillon", "east");
        for (int k = 6; k <= 13; k++) {
            set(42, 1, k, k == 9 ? "air" : C + "madera_pulida");
        }
        set(43, 1, 7, C + "barril[facing=west]");
        set(43, 1, 12, C + "barril[facing=west]");
        set(42, 2, 6, C + "copa");
        set(42, 2, 13, C + "copa");
        escondite(43, 1, 9);
    }

    /** La galería de retratos (arriba, A): marcos en las paredes, bustos, cortinas con nichos. */
    private void retratos() {
        for (int i = 2; i <= 13; i += 3) {
            p.texto(w(i, 14)[0], 9, w(i, 14)[1], "placa", m.f("north"), 0xB8A07A, "†");
            p.texto(w(i, 5)[0], 9, w(i, 5)[1], "placa", m.f("south"), 0xB8A07A, "†");
        }
        nicho(4, 7, 14, 4, 15, "north");
        nicho(11, 7, 14, 11, 15, "north");
        doble(7, 7, 10, "vitrina", "east");
        doble(9, 7, 10, "vitrina", "west");
    }

    /** El guardarropa (arriba, B): filas de vestidos colgados (se pasa entre ellos), roperos y un espejo de cuerpo entero. */
    private void guardarropa() {
        for (int k : new int[] {7, 10, 13}) {
            for (int i = 17; i <= 28; i++) {
                if (i % 5 != 0) {
                    cortina(i, 7, k, k == 13 ? "north" : "south");
                }
            }
        }
        escondite(19, 8, 8);
        escondite(24, 8, 11);
        escondite(27, 8, 14);
        armario(29, 7, 6, "west");
        set(16, 9, 9, C + "espejo[facing=east]");
    }

    /** El cuarto de huéspedes (arriba, C): la cama, un ropero, el tocador y la tina detrás de un biombo. */
    private void huespedes() {
        set(38, 8, 13, C + "catre[facing=south,part=head]");
        set(38, 8, 12, C + "catre[facing=south,part=foot]");
        set(40, 8, 14, C + "cajon[facing=north]");
        set(40, 9, 14, C + "vela");
        armario(32, 7, 12, "east");
        armario(43, 7, 12, "west");
        cortina(41, 7, 9, "west");
        cortina(41, 7, 10, "west");
        escondite(42, 8, 9);
        set(35, 9, 14, C + "espejo[facing=north]");
    }

    // ------------------------------------------------------------------ las puertas de la casa a las alas

    /** Puertas en la pared del fondo de los cuartos de la casa (2 bloques de grueso) hacia los pasillos de las alas. */
    private static void puertasCasa(Plano p) {
        // norte (muro en z -1 y -2): comedor y cocina abajo; recámara y cuarto de las muñecas arriba
        for (int[] d : new int[][] {{23, 1}, {37, 1}, {25, 8}, {38, 8}}) {
            for (int z = -2; z <= -1; z++) {
                Mansion.hueco(p, d[0], z, d[1], 2, true);
            }
        }
        // en el cuarto de las muñecas, quitar la repisa (y sus muñecas) frente a la puerta
        for (int x = 38; x <= 39; x++) {
            p.set(x, 9, 0, "air");
            p.set(x, 10, 0, "air");
        }
        // sur (muro en z 44 y 45): el cuarto de música abajo; la recámara y el baño arriba
        for (int[] d : new int[][] {{21, 1}, {25, 8}, {37, 8}}) {
            for (int z = 44; z <= 45; z++) {
                Mansion.hueco(p, d[0], z, d[1], 2, true);
            }
        }
        // la biblioteca: un hueco secreto detrás de los libreros (el rincón escondido) al pasillo del sur, tapado con una cortina
        for (int z = 44; z <= 45; z++) {
            p.set(37, 1, z, "air");
            p.set(37, 2, z, "air");
            p.set(37, 0, z, C + "madera_pulida");
        }
        p.set(37, 1, 45, C + "cortina[facing=south,half=lower]");
        p.set(37, 2, 45, C + "cortina[facing=south,half=upper]");
    }
}
