package com.coleccionista.mapa;

/**
 * La casa del Coleccionista (capítulo 2, "La mascota"): una mansión vieja de dos pisos donde se esconden las piezas mientras Cascabel, la
 * mascota del Coleccionista, las busca. Se llega por el pasillo de servicio ({@link Pasaje}, idéntico al de detrás del teatro).
 * <pre>
 *   x: 0 ... 54   z: -18 ... 61   y: -1 ... 19
 *   el pasillo x 2..15 (entra al vestíbulo por x 16), el vestíbulo x 17..40 / z 10..33 (doble altura, con galerías arriba en y 7),
 *   abajo: comedor y cocina al norte, salón de música y biblioteca al sur; arriba: dos recámaras, el cuarto de las muñecas y el baño;
 *   la jaula de Cascabel al este (x 42..52), con su reja hacia el vestíbulo
 *   las alas ({@link MansionAlas}): al norte (z -17..-2) y al sur (z 45..60), x 6..50, con pasillo, tres cuartos por piso y ático
 * </pre>
 * Marcas: {@code man.centro}, {@code man.pantalla}, {@code man.jaula} (donde aparece Cascabel), {@code man.reja.K} (las columnas de la reja
 * de la jaula, de abajo), {@code man.esc.N} (escondites: armarios, nichos con cortina, la chimenea, la tina...) y el pasillo {@code pas_man}.
 */
public final class Mansion {
    private static final String C = "coleccionista:";
    private static final String PUERTA = "minecraft:dark_oak_door", TABLA = "minecraft:dark_oak_planks";
    /** El vestíbulo (interior). */
    public static final int HX0 = 17, HX1 = 40, HZ0 = 10, HZ1 = 33, ALTO_HALL = 13;
    /** La reja de la jaula: en el muro del este del vestíbulo (x 41), de z 18 a 25, de y 1 a 6. */
    public static final int REJA_X = 41, REJA_Z0 = 18, REJA_Z1 = 25, REJA_ALTO = 6;
    private static int escondites;

    private Mansion() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        escondites = 0;
        p.caja(0, -1, -18, 54, 19, 61, C + "ladrillo_viejo");
        // el pasillo de servicio, idéntico al del teatro: sus dos puertas del fondo dan al vestíbulo
        Pasaje.pasillo(p, "pas_man", new Marco(2, 26, 270));
        vestibulo(p);
        jaula(p);
        // planta baja
        cuarto(p, 17, 0, 0, 30, 8, 6, C + "parquet", C + "papel_tapiz");
        comedor(p);
        cuarto(p, 32, 0, 0, 44, 8, 6, C + "linoleo", C + "azulejo");
        cocina(p);
        cuarto(p, 17, 35, 0, 30, 43, 6, C + "parquet", C + "papel_tapiz");
        musica(p);
        cuarto(p, 32, 35, 0, 44, 43, 6, C + "parquet", C + "panel_madera");
        biblioteca(p);
        // puertas del vestíbulo a los cuartos de abajo, y entre el comedor y la cocina
        hueco(p, 23, 9, 1, 2, true);
        hueco(p, 37, 9, 1, 2, true);
        hueco(p, 23, 34, 1, 2, true);
        hueco(p, 37, 34, 1, 2, true);
        hueco(p, 31, 4, 1, 2, false);
        hueco(p, 31, 39, 1, 2, false);
        // planta alta (piso en y 7)
        cuarto(p, 17, 0, 7, 30, 8, 5, C + "parquet", C + "papel_tapiz");
        recamara(p, 17, 0, true);
        cuarto(p, 32, 0, 7, 44, 8, 5, C + "parquet", C + "acolchado");
        munecas(p);
        cuarto(p, 17, 35, 7, 30, 43, 5, C + "parquet", C + "papel_tapiz");
        recamara(p, 17, 35, false);
        cuarto(p, 32, 35, 7, 44, 43, 5, C + "azulejo", C + "azulejo");
        bano(p);
        hueco(p, 23, 9, 8, 2, true);
        hueco(p, 37, 9, 8, 2, true);
        hueco(p, 23, 34, 8, 2, true);
        hueco(p, 37, 34, 8, 2, true);
        // las alas del norte y del sur (pasillos, más cuartos, escaleras de servicio y áticos)
        MansionAlas.construir(p);
        return p;
    }

    /** Vacía un cuarto (interior de x0..x1, z0..z1, de y0+1 a y0+alto) con su piso y sus paredes. */
    static void cuarto(Plano p, int x0, int z0, int y0, int x1, int z1, int alto, String piso, String muro) {
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            for (int z = z0 - 1; z <= z1 + 1; z++) {
                boolean borde = x < x0 || x > x1 || z < z0 || z > z1;
                for (int y = y0 + 1; y <= y0 + alto; y++) {
                    if (borde) {
                        String b = p.get(x, y, z);
                        if (b != null && !b.equals("air") && (b.endsWith("ladrillo_viejo") || b.endsWith("papel_tapiz") || b.endsWith("azulejo")
                                || b.endsWith("panel_madera") || b.endsWith("acolchado") || b.endsWith("madera_pulida"))) {
                            p.set(x, y, z, y == y0 + 1 ? C + "madera_pulida" : muro);
                        }
                    } else {
                        p.set(x, y, z, "air");
                    }
                }
                if (!borde) {
                    p.set(x, y0, z, piso);
                    p.set(x, y0 + alto + 1, z, C + "madera_pulida");
                }
            }
        }
        // luz de casa vieja: alcanza para ver los muebles, pero los rincones (y los armarios) quedan oscuros
        for (int x = x0 + 2; x <= x1 - 1; x += 6) {
            for (int z = z0 + 2; z <= z1 - 1; z += 6) {
                p.set(x, y0 + Math.min(3, alto), z, "light[level=10]");
            }
        }
    }

    /** Un hueco de puerta (2 de ancho, 3 de alto) en un muro: en z (a lo largo de x) o en x (a lo largo de z). */
    static void hueco(Plano p, int x, int z, int y, int ancho, boolean muroEnZ) {
        for (int a = 0; a < ancho; a++) {
            for (int yy = y; yy < y + 3; yy++) {
                p.set(muroEnZ ? x + a : x, yy, muroEnZ ? z : z + a, "air");
            }
            p.set(muroEnZ ? x + a : x, y - 1, muroEnZ ? z : z + a, C + "madera_pulida");
        }
    }

    /**
     * Un armario de madera para esconderse: el hueco (x, z) de 1 x 2 de alto con su puerta (que abre cualquiera) del lado {@code mira}.
     */
    static void armario(Plano p, int x, int y0, int z, String mira) {
        int dx = mira.equals("east") ? 1 : mira.equals("west") ? -1 : 0, dz = mira.equals("south") ? 1 : mira.equals("north") ? -1 : 0;
        for (int y = y0 + 1; y <= y0 + 3; y++) {
            // los lados y la espalda
            p.set(x + dz, y, z + dx, TABLA);
            p.set(x - dz, y, z - dx, TABLA);
            if (p.vacio(x - dx, y, z - dz)) {
                p.set(x - dx, y, z - dz, TABLA);
            }
            p.set(x, y, z, y == y0 + 3 ? TABLA : "air");
        }
        p.set(x + dx, y0 + 1, z + dz, PUERTA + "[facing=" + mira + ",half=lower,hinge=left,open=false]");
        p.set(x + dx, y0 + 2, z + dz, PUERTA + "[facing=" + mira + ",half=upper,hinge=left,open=false]");
        p.set(x + dx, y0 + 3, z + dz, TABLA);
        p.set(x + dz + dx, y0 + 3, z + dx + dz, TABLA);
        p.set(x - dz + dx, y0 + 3, z - dx + dz, TABLA);
        escondite(p, x, y0 + 1, z);
    }

    /** Un nicho en la pared tapado con una cortina de terciopelo (se puede entrar y quedarse detrás). */
    static void nicho(Plano p, int x, int y0, int z, int mx, int mz, String mira) {
        // (x, z): delante del muro, donde va la cortina; (mx, mz): el hueco dentro del muro
        p.set(mx, y0 + 1, mz, "air");
        p.set(mx, y0 + 2, mz, "air");
        p.set(mx, y0, mz, C + "parquet");
        p.set(x, y0 + 1, z, C + "cortina[facing=" + mira + ",half=lower]");
        p.set(x, y0 + 2, z, C + "cortina[facing=" + mira + ",half=upper]");
        escondite(p, mx, y0 + 1, mz);
    }

    static void escondite(Plano p, int x, int y, int z) {
        p.marca("man.esc." + escondites++, x, y, z, 0);
    }

    /** El vestíbulo: doble altura, piso de mármol en tablero, la araña, galerías arriba con barandal y dos escaleras. */
    private static void vestibulo(Plano p) {
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                p.set(x, 0, z, C + ((x / 2 + z / 2) % 2 == 0 ? "marmol_negro" : "marmol_crema"));
                for (int y = 1; y <= ALTO_HALL; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, ALTO_HALL + 1, z, C + "madera_pulida");
            }
        }
        // las paredes: madera abajo, moldura dorada y papel tapiz
        for (int y = 1; y <= ALTO_HALL; y++) {
            String b = y <= 2 ? C + "madera_pulida" : y == 3 ? C + "marco_dorado" : C + "papel_tapiz";
            for (int x = HX0 - 1; x <= HX1 + 1; x++) {
                p.set(x, y, HZ0 - 1, b);
                p.set(x, y, HZ1 + 1, b);
            }
            for (int z = HZ0; z <= HZ1; z++) {
                if (!p.vacio(HX0 - 1, y, z) && !p.get(HX0 - 1, y, z).contains("puerta")) {
                    p.set(HX0 - 1, y, z, b);
                }
                p.set(HX1 + 1, y, z, b);
            }
        }
        // el tapete de la entrada a la jaula
        for (int x = HX0; x <= HX1; x++) {
            for (int z = 20; z <= 23; z++) {
                p.set(x, 0, z, C + (z == 20 || z == 23 ? "tapete_orilla" : "tapete_rojo"));
            }
        }
        // las galerías de arriba (y 7), al norte y al sur, con barandal
        for (int x = HX0; x <= HX1; x++) {
            for (int z : new int[] {10, 11, 12, 31, 32, 33}) {
                p.set(x, 7, z, C + "madera_pulida");
            }
            if (x < 28 || x > 30) {
                p.set(x, 8, 12, C + "barandal");
                p.set(x, 8, 31, C + "barandal");
            }
        }
        // las escaleras: suben hacia el oeste junto a cada galería
        for (int i = 0; i < 7; i++) {
            int x = 35 - i, y = 1 + i;
            for (int z : new int[] {13, 14, 29, 30}) {
                p.set(x, y, z, C + "escalera_tablon[facing=west]");
                for (int yy = 1; yy < y; yy++) {
                    p.set(x, yy, z, C + "madera_pulida");
                }
            }
        }
        // columnas, sillones, relojes y la araña
        for (int x : new int[] {HX0, HX1}) {
            for (int z : new int[] {HZ0 + 4, HZ1 - 4}) {
                for (int y = 1; y <= 6; y++) {
                    p.set(x, y, z, C + "viga_madera[axis=y]");
                }
            }
        }
        p.set(HX0 + 1, 1, 15, C + "sillon[facing=east,half=lower]");
        p.set(HX0 + 1, 2, 15, C + "sillon[facing=east,half=upper]");
        p.set(HX0 + 1, 1, 28, C + "sillon[facing=east,half=lower]");
        p.set(HX0 + 1, 2, 28, C + "sillon[facing=east,half=upper]");
        p.set(28, ALTO_HALL, 21, C + "cadena");
        p.set(28, ALTO_HALL - 1, 21, C + "arana");
        for (int x = HX0 + 2; x <= HX1 - 1; x += 6) {
            for (int z = HZ0 + 3; z <= HZ1 - 1; z += 6) {
                p.set(x, 3, z, "light[level=11]");
                p.set(x, 10, z, "light[level=10]");
            }
        }
        // cuatro armarios bajo las galerías
        armario(p, HX0 + 1, 0, HZ0 + 1, "south");
        armario(p, HX0 + 1, 0, HZ1 - 1, "north");
        armario(p, HX1 - 1, 0, HZ0 + 1, "south");
        armario(p, HX1 - 1, 0, HZ1 - 1, "north");
        p.marca("man.centro", 26, 1, 21, 90);
        p.marca("man.entrada", HX0 + 2, 1, 21, 270);
    }

    /** La jaula de Cascabel: paja, huesos, cadenas, su plato y la reja grande hacia el vestíbulo; arriba, la pantalla del Coleccionista. */
    private static void jaula(Plano p) {
        for (int x = 42; x <= 52; x++) {
            for (int z = 14; z <= 29; z++) {
                p.set(x, 0, z, (x + z) % 3 == 0 ? "minecraft:hay_block" : C + "piso_cloaca");
                for (int y = 1; y <= 8; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, 9, z, C + "metal_negro");
            }
        }
        for (int z = REJA_Z0; z <= REJA_Z1; z++) {
            for (int y = 1; y <= REJA_ALTO; y++) {
                p.set(REJA_X, y, z, C + "barrotes");
            }
            p.set(REJA_X, 0, z, C + "metal_placa");
            p.set(REJA_X, REJA_ALTO + 1, z, C + "viga[axis=z]");
            p.marca("man.reja." + (z - REJA_Z0), REJA_X, 1, z, 90);
        }
        // cadenas que cuelgan, huesos, el plato y rasguños
        for (int[] c : new int[][] {{44, 16}, {50, 17}, {45, 27}, {51, 26}}) {
            for (int y = 5; y <= 8; y++) {
                p.set(c[0], y, c[1], C + "cadena");
            }
        }
        p.set(47, 1, 16, C + "craneo");
        p.set(50, 1, 22, C + "craneo");
        p.set(44, 1, 25, C + "craneo");
        p.set(52, 1, 21, C + "argolla[facing=west]");
        p.texto(42, 4, 21, "rayon", "east", 0x8A8070, "IIII IIII", "IIII IIII", "IIII");
        p.set(48, 4, 21, "light[level=4]");
        p.marca("man.jaula", 48, 1, 21, 90);
        // la pantalla grande arriba de la reja y el letrero con su nombre
        p.set(HX1, 9, 21, C + "pantalla[facing=west,grande=true]");
        p.marca("man.pantalla", HX1, 9, 21, 90);
        p.texto(HX1, 7, 21, "letrero", "west", 0xD9A93A, "CASCABEL");
        // los candelabros van sobre una repisa pegada al muro (no flotando)
        p.set(HX1, 6, 17, C + "losa_madera[type=top]");
        p.set(HX1, 6, 26, C + "losa_madera[type=top]");
        p.set(HX1, 7, 17, C + "candelabro[facing=west,half=lower]");
        p.set(HX1, 8, 17, C + "candelabro[facing=west,half=upper]");
        p.set(HX1, 7, 26, C + "candelabro[facing=west,half=lower]");
        p.set(HX1, 8, 26, C + "candelabro[facing=west,half=upper]");
    }

    private static void comedor(Plano p) {
        // la mesa larga (losas arriba) con sillas, y la chimenea en el muro del oeste (se puede meter uno)
        for (int x = 20; x <= 27; x++) {
            for (int z = 3; z <= 5; z++) {
                p.set(x, 1, z, C + "losa_madera[type=top]");
            }
            if (x % 2 == 0) {
                p.set(x, 1, 2, C + "silla[facing=south]");
                p.set(x, 1, 6, C + "silla[facing=north]");
            }
        }
        p.set(23, 2, 4, C + "candelabro[facing=north,half=lower]");
        p.set(23, 3, 4, C + "candelabro[facing=north,half=upper]");
        for (int z = 3; z <= 4; z++) {
            p.caja(15, 1, z, 16, 2, z, "air");
            p.set(15, 0, z, C + "ladrillo_oscuro");
            p.set(16, 0, z, C + "ladrillo_oscuro");
            p.set(16, 3, z, C + "ladrillo_oscuro");
        }
        p.set(15, 1, 3, C + "cadena");
        escondite(p, 16, 1, 4);
        armario(p, 18, 0, 1, "south");
        armario(p, 29, 0, 1, "south");
    }

    private static void cocina(Plano p) {
        for (int x = 33; x <= 41; x++) {
            p.set(x, 1, 8, C + "madera_pulida");
            p.set(x, 2, 8, C + "losa_madera[type=bottom]");
        }
        p.set(36, 1, 4, C + "metal_negro");
        p.set(37, 1, 4, C + "metal_negro");
        p.set(36, 2, 4, C + "farol[facing=north,encendida=true,falla=false]");
        p.set(33, 1, 1, C + "barril[facing=south]");
        p.set(34, 1, 1, C + "barril[facing=south]");
        // la despensa: un cuartito con puerta en la esquina
        p.caja(42, 1, 0, 42, 3, 2, TABLA);
        p.caja(43, 1, 2, 44, 3, 2, TABLA);
        p.caja(43, 1, 0, 44, 2, 1, "air");
        p.set(43, 1, 2, PUERTA + "[facing=south,half=lower,hinge=left,open=false]");
        p.set(43, 2, 2, PUERTA + "[facing=south,half=upper,hinge=left,open=false]");
        escondite(p, 44, 1, 0);
        armario(p, 33, 0, 7, "north");
    }

    private static void musica(Plano p) {
        // el piano (cola), sillones y tres nichos con cortina en el muro del sur
        p.caja(22, 1, 38, 25, 1, 40, C + "metal_negro");
        p.caja(22, 2, 38, 25, 2, 38, C + "madera_pulida");
        p.set(23, 1, 37, C + "silla[facing=south]");
        for (int x : new int[] {19, 24, 29}) {
            nicho(p, x, 0, 43, x, 44, "north");
        }
        p.set(18, 1, 36, C + "sillon[facing=east,half=lower]");
        p.set(18, 2, 36, C + "sillon[facing=east,half=upper]");
        p.set(28, 3, 35, C + "reloj[facing=south]");
        armario(p, 18, 0, 40, "east");
    }

    private static void biblioteca(Plano p) {
        // pasillos de libreros y un hueco secreto detrás del último
        for (int z : new int[] {37, 40}) {
            for (int x = 33; x <= 41; x++) {
                for (int y = 1; y <= 4; y++) {
                    p.set(x, y, z, C + "estante_archivo");
                }
            }
        }
        p.caja(33, 1, 43, 41, 2, 43, "air");
        p.caja(33, 1, 42, 41, 4, 42, C + "estante_archivo");
        p.set(42, 1, 42, "air");
        p.set(42, 2, 42, "air");
        escondite(p, 34, 1, 43);
        escondite(p, 40, 1, 43);
        p.set(43, 1, 36, C + "atril[facing=west]");
        armario(p, 43, 0, 38, "west");
    }

    private static void recamara(Plano p, int x0, int z0, boolean norte) {
        int y0 = 7;
        int zc = norte ? z0 + 1 : z0 + 7;
        String hacia = norte ? "south" : "north";
        String cabeza = norte ? "north" : "south";
        p.set(x0 + 6, y0 + 1, zc, C + "catre[facing=" + cabeza + ",part=head]");
        p.set(x0 + 6, y0 + 1, zc + (norte ? 1 : -1), C + "catre[facing=" + cabeza + ",part=foot]");
        p.set(x0 + 8, y0 + 1, zc, C + "cajon[facing=" + hacia + "]");
        p.set(x0 + 8, y0 + 2, zc, C + "vela");
        armario(p, x0 + 1, y0, zc, hacia);
        armario(p, x0 + 12, y0, zc, hacia);
        int zn = norte ? z0 - 1 : z0 + 9;
        nicho(p, x0 + 4, y0, norte ? z0 : z0 + 8, x0 + 4, zn, hacia);
        p.set(x0 + 10, y0 + 2, norte ? z0 : z0 + 8, C + "espejo[facing=" + hacia + "]");
    }

    private static void munecas(Plano p) {
        // el cuarto de las muñecas: repisas llenas, muñecas en el piso mirando a la puerta, una cuna
        int y0 = 7;
        for (int x = 33; x <= 43; x++) {
            p.set(x, y0 + 2, 0, C + "losa_madera[type=top]");
            if (x % 2 == 1) {
                p.set(x, y0 + 3, 0, C + "muneca[facing=south]");
            }
        }
        int[][] piso = {{34, 4}, {36, 6}, {39, 3}, {41, 6}, {43, 4}, {35, 7}, {40, 7}};
        for (int[] q : piso) {
            p.set(q[0], y0 + 1, q[1], C + "muneca[facing=south]");
        }
        p.caja(37, y0 + 1, 2, 38, y0 + 1, 3, C + "madera_pulida");
        p.set(37, y0 + 2, 2, C + "muneca[facing=south]");
        armario(p, 33, y0, 2, "east");
        armario(p, 43, y0, 2, "west");
    }

    private static void bano(Plano p) {
        // la tina (se puede meter uno, con la cortina corrida), el lavabo, el retrete y un armario
        int y0 = 7;
        p.caja(39, y0 + 1, 40, 43, y0 + 1, 43, C + "marmol_crema");
        p.caja(40, y0 + 1, 41, 42, y0 + 1, 42, "air");
        p.caja(40, y0, 41, 42, y0, 42, C + "azulejo");
        for (int x = 39; x <= 43; x++) {
            p.set(x, y0 + 2, 39, C + "cortina[facing=north,half=lower]");
            p.set(x, y0 + 3, 39, C + "cortina[facing=north,half=upper]");
        }
        escondite(p, 41, y0 + 1, 41);
        p.set(33, y0 + 1, 43, C + "retrete[facing=north]");
        p.set(35, y0 + 1, 43, C + "lavabo[facing=north]");
        p.set(35, y0 + 2, 43, C + "espejo[facing=north]");
        armario(p, 33, y0, 36, "east");
    }
}
