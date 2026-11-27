package com.coleccionista.mapa;

/**
 * La casa del Coleccionista (capítulo 2, "La mascota"): una mansión gótica victoriana de tres pisos y sótano, en medio de un jardín de
 * noche, donde las piezas se esconden mientras Cascabel, la mascota del Coleccionista, las busca. Se llega por el pasillo de servicio
 * ({@link Pasaje}) que da a la cripta del panteón de la familia; de ahí se sube al jardín y se ve la casa.
 * <p>
 * Se construye por código (no con un plano de texto), simétrica en lo de afuera: {@link MansionJardin} (el terreno, la cripta, el
 * panteón, la fuente, el laberinto de setos, el cementerio, el invernadero, el estanque...), {@link MansionObra} (muros, pisos, techo de
 * pizarra, torres, pórtico, chimeneas, escaleras y ventanas), el vestíbulo con la jaula (aquí) y {@link MansionCuartos} (cada cuarto
 * vestido). {@link Revisor} la camina entera en las pruebas ({@code tests/run.sh}): todos los cuartos y escondites se alcanzan a pie.
 * <pre>
 *   el terreno: x 0..53, z -64..95 (el pasto en y 6; se camina en y 7)
 *   la casa: muros en x 5 / 48 y z 0 / 51; pisos: sótano y 0, planta baja y 7, planta alta y 15, ático y 22, azotea y 30
 *   planta baja:  biblioteca y música | VESTÍBULO (x 17..36, z 1..22, doble altura, con la jaula al fondo) | comedor y mayordomo
 *                 salón de baile (z 24..37, doble altura) | pasillo de servicio (x 44..47, con escaleras al sótano y arriba)
 *                 billar | trofeos (puerta al jardín de atrás) | cocina
 *   planta alta:  recámara y cuarto del bebé | galería del vestíbulo y balcón del pórtico | estudio y huéspedes
 *                 balcón del salón | pasillo de servicio;   costura | baño | servidumbre (con la escalera al ático)
 *   ático: todo el bajo techo, con las dos torres (cada una con su mirador arriba)
 *   sótano: bodega de vinos, carbón, calderas, taller, bodega grande, muñecas rotas, lavandería, despensa y el pasillo de servicio
 *           (al este, x 44..47), que lleva al túnel de la salida (z 8..9, hacia la caja de música)
 * </pre>
 * Marcas: {@code man.entrada}, {@code man.centro}, {@code man.pantalla}, {@code man.jaula} (donde aparece Cascabel), {@code man.reja.K}
 * (las columnas de la reja de la jaula, de abajo), {@code man.esc.N} (escondites), {@code salida.mansion.a/.b} y su camino
 * {@code salida.mansion.via.N}, y el pasillo {@code pas_man}.
 */
public final class Mansion {
    static final String C = "coleccionista:";
    /** Los muros de afuera de la casa. */
    public static final int X0 = 5, X1 = 48, Z0 = 0, Z1 = 51;
    /** Los pisos (el bloque del piso): sótano, planta baja, planta alta, ático y la azotea; el piso del mirador de las torres. */
    public static final int SOT = 0, PB = 7, PA = 15, ATI = 22, AZOTEA = 30, TORRE = 31;
    /** El espejo (la casa es simétrica en x: el eje pasa entre x 26 y 27). */
    public static final int EJE = 53;
    /** El vestíbulo (interior, de doble altura). */
    public static final int HX0 = 17, HX1 = 36, HZ0 = 1, HZ1 = 22, ALTO_HALL = 20;
    /** La reja de la jaula de Cascabel: al fondo del vestíbulo (z 17), de x 21 a 32 (12 columnas), de y 8 a 13. */
    public static final int REJA_X0 = 21, REJA_Z = 17, REJA_Z0 = 0, REJA_Z1 = 11, REJA_ALTO = 6;
    /** El salón de baile (z 24..37) y el pasillo de servicio (x 44..47). */
    static final int BX0 = 6, BX1 = 42, BZ0 = 24, BZ1 = 37, SX0 = 44;
    /** La torre del noroeste (la otra es su espejo). */
    static final int TX0 = 5, TX1 = 12, TZ0 = -5, TZ1 = 2;
    /** El pórtico de la entrada. */
    static final int PX0 = 21, PX1 = 32, PZ0 = -5;

    private Mansion() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        Trazo t = new Trazo(p, semilla, EJE);
        MansionJardin.terreno(t);
        MansionJardin.cripta(t);
        MansionObra.casa(t);
        vestibulo(t);
        jaula(t);
        MansionCuartos.plantaBaja(t);
        MansionCuartos.plantaAlta(t);
        MansionCuartos.atico(t);
        MansionCuartos.sotano(t);
        salida(t);
        alumbrar(t);
        return p;
    }

    /**
     * Para la revisión: ¿alguien parado en {@code (x, y, z)} estaría encima de un techo, una barda, un seto o una cornisa? (No son
     * cuartos: no se revisan como lugares a los que no se llega.)
     */
    public static boolean enTecho(Plano p, int x, int y, int z) {
        String b = p.get(x, y - 1, z), pies = p.get(x, y, z);
        if (b == null) {
            b = pies == null ? "" : pies;
        }
        // a cielo abierto y arriba del jardín: es un techo, una barda o la punta de un seto
        if (y > PB + 1) {
            boolean cielo = true;
            for (int yy = y + 1; yy <= p.y1 && cielo; yy++) {
                String s = p.get(x, yy, z);
                cielo = s == null || s.equals("air") || s.startsWith("light");
            }
            if (cielo) {
                return true;
            }
        }
        for (String r : new String[] {"pizarra", "seto", "cornisa", "sillar", "vidrio_sucio", "metal_negro", "piedra_capilla", "ladrillo_oscuro",
                "tronco", "ramas", "losa_tablon", "barrotes", "viga_madera", "marco_dorado", "balaustrada", "vitral", "poste", "angel", "cuervo", "estante_archivo", "telon", "lodo"}) {
            if (b.contains(r)) {
                return true;
            }
        }
        return false;
    }

    /** Un escondite más ({@code man.esc.N}): los pies de alguien escondido ahí. */
    static void esc(Trazo t, int x, int y, int z) {
        int n = 0;
        while (t.p.marcas.containsKey("man.esc." + n)) {
            n++;
        }
        t.p.marca("man.esc." + n, x, y, z, 0);
    }

    // ------------------------------------------------------------------ el vestíbulo

    private static void vestibulo(Trazo t) {
        int y = PB + 1;
        // el piso: cuadros de mármol con una orilla de mármol con oro y una alfombra roja de la puerta a la jaula
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                boolean orilla = x == HX0 || x == HX1 || z == HZ0;
                t.set(x, PB, z, orilla ? C + "marmol_oro" : ((x + z) & 1) == 0 ? C + "marmol_negro" : C + "marmol_crema");
            }
        }
        for (int z = HZ0; z <= REJA_Z - 2; z++) {
            t.set(26, y, z, C + "alfombra_roja");
            t.set(27, y, z, C + "alfombra_roja");
        }
        for (int x = 22; x <= 31; x++) {
            t.set(x, y, 10, C + "alfombra_roja");
        }
        // la galería de abajo del balcón (z 1..3): el clóset de abajo de cada escalera, con su puerta
        for (int x = HX0; x <= HX0 + 2; x++) {
            for (int yy = y; yy < PA - 1; yy++) {
                t.par(x, yy, HZ0 + 2, C + "panel_madera");
            }
        }
        t.puerta(18, y, HZ0 + 2, "dark_oak", "north", "left");
        t.puerta(t.e(18), y, HZ0 + 2, "dark_oak", "north", "right");
        for (boolean este : new boolean[] {false, true}) {
            int x = este ? t.e(18) : 18;
            esc(t, x, y, 5);
            t.set(este ? t.e(17) : 17, y, 6, C + "cajon[facing=" + (este ? "west" : "east") + "]");
            t.set(este ? t.e(19) : 19, y, 4, C + "telarana");
            t.set(x, PA - 2, 4, C + "telarana");
        }
        // abajo de la galería: un tapete y dos sillones a los lados de la puerta, plantas y luz baja
        t.altoPar(21, y, HZ0, "planta", "south");
        t.altoPar(24, y, HZ0, "sillon", "south");
        t.set(25, y, HZ0, C + "mesa[facing=south]");
        t.set(25, y + 1, HZ0, C + "lampara_mesa");
        t.set(28, y, HZ0, C + "mesa[facing=south]");
        t.set(28, y + 1, HZ0, C + "lampara_mesa");
        // los muros del vestíbulo: un friso dorado entre los dos pisos, retratos, armaduras, candelabros
        for (int z = HZ0 + 3; z <= HZ1; z++) {
            t.par(HX0 - 1, PA - 1, z, C + "marco_dorado");
        }
        for (int x = HX0; x <= HX1; x++) {
            t.set(x, PA - 1, BZ0 - 1, C + "marco_dorado");
        }
        t.altoPar(HX0, y, 15, "armadura", "east");
        t.altoPar(HX0, y, 17, "reloj_pie", "east");
        t.altoPar(HX0, y, 22, "planta", "east");
        t.decoPar(HX0, y + 3, 15, "retrato_caballero", "east");
        t.deco(HX0, y + 3, 17, "retrato_dama", "east");
        t.deco(HX1, y + 3, 17, "retrato_nina", "west");
        t.altoPar(20, y, 16, "candelabro", "north");
        // arriba (el piso de la galería): retratos grandes y estandartes sobre las escaleras
        for (int z : new int[] {6, 10}) {
            t.altoPar(HX0, PA + 2, z, "estandarte_rojo", "east");
        }
        t.decoPar(HX0, PA + 3, 8, "retrato_caballero", "east");
        t.decoPar(HX0, PA + 3, 14, "retrato_dama", "east");
        t.decoPar(HX0, PA + 3, 18, "retrato_nina", "east");
        t.altoPar(HX0, PA + 3, 21, "estandarte_negro", "east");
        // la galería del norte: alfombra, bustos, sillones
        for (int x = HX0 + 3; x <= HX1 - 3; x++) {
            t.set(x, PA + 1, HZ0, C + "alfombra_roja");
        }
        t.altoPar(22, PA + 1, HZ0, "sillon", "south");
        // las arañas: a los lados (fuera de la vista de la pantalla, desde abajo y desde la galería)
        for (int x : new int[] {21, 32}) {
            for (int z : new int[] {7, 13}) {
                t.arana(x, ATI - 1, z, 3);
            }
        }
        // luz: baja, cálida
        for (int x : new int[] {20, 33}) {
            for (int z : new int[] {6, 14, 20}) {
                t.luz(x, PA - 2, z, 11);
            }
        }
        t.luz(26, PA + 3, 2, 9);
        t.luz(26, PA + 4, 16, 10);
        t.luz(18, PA - 2, 2, 8);
        t.luz(35, PA - 2, 2, 8);
        // dónde se para la gente al llegar y el centro
        t.p.marca("man.entrada", 26, y, 7, 0);
        t.p.marca("man.centro", 26, y, 12, 0);
    }

    // ------------------------------------------------------------------ la jaula de Cascabel (al fondo del vestíbulo)

    private static void jaula(Trazo t) {
        int y = PB + 1, x0 = REJA_X0 - 1, x1 = REJA_X0 + REJA_Z1 + 1, z0 = REJA_Z, z1 = BZ0 - 2;
        // muros de los lados (metal), techo y el piso de lajas con paja vieja (tablones)
        for (int z = z0; z <= z1; z++) {
            for (int yy = y; yy <= PA - 1; yy++) {
                t.set(x0, yy, z, C + "metal_negro");
                t.set(x1, yy, z, C + "metal_negro");
            }
        }
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                t.set(x, PA - 1, z, C + "metal_negro");
                if (x > x0 && x < x1 && z > z0) {
                    t.set(x, PB, z, (x * 7 + z * 3) % 5 == 0 ? C + "tablon" : C + "piso_cloaca");
                    for (int yy = y; yy < PA - 1; yy++) {
                        t.set(x, yy, z, "air");
                    }
                }
            }
        }
        // la reja (se abre en el juego: Mascota quita y vuelve a poner los barrotes de cada columna)
        for (int k = 0; k <= REJA_Z1 - REJA_Z0; k++) {
            int x = REJA_X0 + k;
            for (int yy = y; yy < y + REJA_ALTO; yy++) {
                t.set(x, yy, z0, C + "barrotes");
            }
            t.set(x, PB, z0, C + "metal_placa");
            t.p.marca("man.reja." + k, x, y, z0, 180);
        }
        // adentro: cadenas que cuelgan, huesos, la argolla, rayones contando los días y poca luz
        for (int[] c : new int[][] {{22, 19}, {31, 19}, {24, 22}, {29, 22}}) {
            for (int yy = y + 3; yy < PA - 1; yy++) {
                t.set(c[0], yy, c[1], C + "cadena[axis=y]");
            }
        }
        t.set(23, y, 21, C + "craneo");
        t.set(30, y, 20, C + "craneo");
        t.set(27, y, 22, C + "argolla[facing=north]");
        t.set(25, y, 19, C + "sangre");
        t.set(29, y, 22, C + "barril[facing=north]");
        t.p.texto(x0 + 1, y + 2, 21, "rayon", "east", 0x8A8070, "IIII IIII", "IIII IIII", "IIII");
        t.p.texto(x1 - 1, y + 2, 20, "rayon", "west", 0x8A8070, "NO ME", "DEJES", "SALIR");
        t.luz(26, y + 3, 20, 4);
        t.p.marca("man.jaula", 26, y, 20, 180);
        // arriba de la jaula, hasta el techo: un gran mueble de madera y papel tapiz con marco dorado; en su frente, la pantalla grande
        // (11 x 7), a la vista de todo el vestíbulo (ya no la tapa el techo de la jaula)
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int yy = PA; yy <= ATI - 2; yy++) {
                    boolean frente = z == z0, lado = x == x0 || x == x1;
                    String b = C + "papel_tapiz";
                    if (frente && (lado || yy == ATI - 2)) {
                        b = C + "marco_dorado";
                    } else if (lado) {
                        b = yy == ATI - 2 ? C + "marco_dorado" : C + "panel_madera";
                    }
                    t.set(x, yy, z, b);
                }
            }
            t.set(x, PA - 1, z0, C + "madera_pulida");
        }
        t.set(26, PA + 2, z0 - 1, C + "pantalla[facing=north,grande=true]");
        t.p.nbt(26, PA + 2, z0 - 1, "{dx:-0.5f}");
        t.p.marca("man.pantalla", 26, PA + 2, z0 - 1, 180);
        // el nombre de la mascota, adentro de la jaula (se lee entre los barrotes)
        t.p.texto(26, y + 3, z1, "letrero", "north", 0xD9A93A, "CASCABEL");
    }

    // ------------------------------------------------------------------ la luz

    /**
     * La luz de toda la finca (luz invisible, encima de las cabezas): en cada cuarto una rejilla cada 4 bloques; en el jardín cada 5 (un
     * claro de luna tenue); en la fachada, luz que la baña desde abajo para que se vea la casa de noche. Lo oscuro de verdad queda en los
     * rincones, la jaula y los escondites.
     */
    private static void alumbrar(Trazo t) {
        Plano p = t.p;
        // adentro: {pies, nivel}
        int[][] pisos = {{SOT + 1, 14}, {PB + 1, 15}, {PA + 1, 15}, {ATI + 1, 13}, {TORRE + 1, 14}};
        for (int[] n : pisos) {
            int y = n[0] + 2;
            for (int x = X0 + 1; x <= X1 - 1; x++) {
                for (int z = TZ0 + 1; z <= Z1 - 1; z++) {
                    if (Math.floorMod(x - 2, 4) != 0 || Math.floorMod(z - 2, 4) != 0) {
                        continue;
                    }
                    boolean jaula = x >= REJA_X0 && x <= REJA_X0 + REJA_Z1 && z > REJA_Z && z <= HZ1 && y < PA;
                    if (!jaula && p.vacio(x, y, z)) {
                        t.luz(x, y, z, n[1]);
                    }
                }
            }
        }
        // las dos alturas del vestíbulo y del salón de baile (arriba, más luz)
        for (int x = HX0 + 1; x <= HX1 - 1; x += 4) {
            for (int z = HZ0 + 4; z <= HZ1; z += 4) {
                if (p.vacio(x, PA + 4, z)) {
                    t.luz(x, PA + 4, z, 14);
                }
            }
        }
        for (int x = BX0 + 4; x <= BX1 - 2; x += 5) {
            for (int z = BZ0 + 2; z <= BZ1; z += 5) {
                if (p.vacio(x, PA + 4, z)) {
                    t.luz(x, PA + 4, z, 14);
                }
            }
        }
        // afuera: el jardín (a cielo abierto, un poco arriba de las cabezas) y la cripta
        int yj = MansionJardin.SUELO + 3;
        for (int x = MansionJardin.TX0 + 2; x <= MansionJardin.TX1 - 2; x += 5) {
            for (int z = MansionJardin.TZ0 + 2; z <= MansionJardin.TZ1 - 2; z += 5) {
                boolean casa = x >= X0 && x <= X1 && z >= TZ0 && z <= Z1;
                if (!casa && p.vacio(x, yj, z)) {
                    t.luz(x, yj, z, 13);
                }
            }
        }
        for (int x = 22; x <= 31; x += 4) {
            for (int z = -46; z <= -38; z += 4) {
                if (p.vacio(x, SOT + 3, z)) {
                    t.luz(x, SOT + 3, z, 13);
                }
            }
        }
        // la fachada bañada de luz: al pie de los cuatro muros, cada 3, a la altura de cada piso
        for (int yy : new int[] {PB + 2, PA + 2}) {
            for (int x = X0 - 1; x <= X1 + 1; x += 3) {
                for (int z : new int[] {TZ0 - 1, Z0 - 1, Z1 + 1}) {
                    if (p.vacio(x, yy, z)) {
                        t.luz(x, yy, z, 15);
                    }
                }
            }
            for (int z = TZ0; z <= Z1; z += 3) {
                for (int x : new int[] {X0 - 1, X1 + 1}) {
                    if (p.vacio(x, yy, z)) {
                        t.luz(x, yy, z, 15);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ la salida (al túnel del sótano, hacia la caja de música)

    private static void salida(Trazo t) {
        // el túnel: del pasillo de servicio del sótano (x 44..47) al muro del terreno (x 53), en z 8..9
        for (int x = X1; x <= 52; x++) {
            for (int z = 7; z <= 10; z++) {
                boolean muro = z == 7 || z == 10;
                t.set(x, SOT, z, C + "concreto_piso");
                for (int y = SOT + 1; y <= SOT + 3; y++) {
                    t.set(x, y, z, muro ? C + "ladrillo_viejo" : "air");
                }
                t.set(x, SOT + 4, z, C + "ladrillo_viejo");
            }
        }
        for (int z = 8; z <= 9; z++) {
            t.set(X1, SOT + 4, z, C + "madera_pulida");
        }
        for (int x = 49; x <= 52; x += 3) {
            t.set(x, SOT + 3, 8, C + "farol");
        }
        Salida.puerta(t.p, "salida.mansion", 53, 8, 54, C + "concreto_piso");
        // letreros hacia la salida
        t.p.letrero(52, SOT + 2, 10, "letrero", "north", "red", "SALIDA");
        // el camino a la salida (para la flecha): del vestíbulo al cuarto del mayordomo, la escalera al sótano y el túnel
        int[][] via = {{26, PB + 1, 12}, {35, PB + 1, 19}, {40, PB + 1, 19}, {45, PB + 1, 21}, {46, PB + 1, BZ0}, {46, SOT + 1, BZ0 + 7},
                {45, SOT + 1, 9}, {50, SOT + 1, 9}};
        for (int i = 0; i < via.length; i++) {
            t.p.marca("salida.mansion.via." + i, via[i][0], via[i][1], via[i][2], 0);
        }
    }
}
