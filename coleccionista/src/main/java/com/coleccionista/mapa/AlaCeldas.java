package com.coleccionista.mapa;

/**
 * El Ala de celdas: donde despiertan las piezas. Un pabellón de una vieja institución (mitad prisión, mitad manicomio) que el Coleccionista
 * convirtió en su "vitrina": dos pisos de celdas con azulejo de hospital, yeso descarapelado, barrotes oxidados y pasarelas de rejilla; al
 * oeste, su escenario (la pantalla gigante entre cortinas de terciopelo y vitrinas con muñecas de porcelana); al este, la escalera y el
 * portón de salida. Todo con bloques propios del mod.
 * <pre>
 *   x: -8 ... 44 (vestíbulo oeste -7..-1, celdas 0..36, vestíbulo este 37..43)
 *   z: 0 ... 22 (celdas sur 1..5, pasillo 7..15, celdas norte 17..21)
 *   y: 0 ... 10 (piso de abajo 1..4, piso de arriba 6..9)
 * </pre>
 * Celdas: 1-6 abajo al sur, 7-12 abajo al norte, 13-18 arriba al sur, 19-24 arriba al norte (de oeste a este).
 * Marcas por celda N: {@code celda.N} (donde aparece, mirando a su pantalla), {@code celda.N.pantalla}, {@code celda.N.puerta} (mitad de
 * abajo de la puerta), {@code celda.N.candado}, {@code celda.N.barril} (el baúl con las pistas), {@code celda.N.letrero} y
 * {@code celda.N.letrero2} (rayones en las paredes), {@code celda.N.cama}, {@code celda.N.foco}, {@code celda.N.cam1} y {@code celda.N.cam2}
 * (la cámara del despertar). Generales: {@code pantalla_grande}, {@code salida}, {@code centro} y {@code ala.luz.K} (lámparas del pasillo
 * que se apagan en la sala de espera).
 */
public final class AlaCeldas {
    public static final int CELDAS = 24;
    private static final String C = "coleccionista:";
    private static final String AZULEJO = C + "azulejo", AZULEJO_SUCIO = C + "azulejo_sucio", REMATE = C + "azulejo_remate", YESO = C + "yeso";
    private static final String CONCRETO = C + "concreto", PISO = C + "concreto_piso", LINOLEO = C + "linoleo", VIGA = C + "viga";
    private static final String BARROTES = C + "barrotes", METAL = C + "metal_placa", NEGRO = C + "metal_negro";
    private static int luces;

    private AlaCeldas() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        luces = 0;
        int xa = -8, xb = 44, za = 0, zb = 22, ya = 0, yb = 10;
        // casco: piso, techo y muros exteriores (azulejo abajo, remate, yeso arriba; igual en los dos pisos)
        p.caja(xa, ya, za, xb, ya, zb, PISO);
        p.caja(xa, yb, za, xb, yb, zb, CONCRETO);
        for (int y = ya + 1; y < yb; y++) {
            for (int x = xa; x <= xb; x++) {
                p.set(x, y, za, banda(y));
                p.set(x, y, zb, banda(y));
            }
            for (int z = za; z <= zb; z++) {
                p.set(xa, y, z, banda(y));
                p.set(xb, y, z, banda(y));
            }
        }
        p.caja(xa + 1, ya + 1, za + 1, xb - 1, yb - 1, zb - 1, "air");
        pasillo(p);
        for (int k = 0; k < 6; k++) {
            int xs = 1 + 6 * k;
            celda(p, 1 + k, xs, 0, true);
            celda(p, 7 + k, xs, 0, false);
            celda(p, 13 + k, xs, 5, true);
            celda(p, 19 + k, xs, 5, false);
        }
        // muros entre celdas (con la misma banda de azulejo) y al frente, las columnas de acero
        for (int k = 0; k <= 6; k++) {
            int x = 6 * k;
            for (int y = 1; y <= 9; y++) {
                for (int z = 1; z <= 5; z++) {
                    p.set(x, y, z, y == 5 ? CONCRETO : banda(y));
                    p.set(x, y, 22 - z, y == 5 ? CONCRETO : banda(y));
                }
                p.set(x, y, 6, VIGA + "[axis=y]");
                p.set(x, y, 16, VIGA + "[axis=y]");
            }
        }
        pasarelas(p);
        techo(p);
        vestibuloOeste(p);
        vestibuloEste(p);
        lucesPasillo(p);
        p.marca("centro", 18, 1, 11, 90);
        return p;
    }

    /** El muro según la altura: azulejo sucio abajo, remate, yeso arriba (se repite en el segundo piso) y concreto en la losa. */
    private static String banda(int y) {
        int h = y <= 5 ? y : y - 5;
        if (y == 5) {
            return CONCRETO;
        }
        return h == 1 ? AZULEJO_SUCIO : h == 2 ? REMATE : YESO;
    }

    // ------------------------------------------------------------------ pasillo

    private static void pasillo(Plano p) {
        for (int x = -7; x <= 43; x++) {
            for (int z = 7; z <= 15; z++) {
                p.set(x, 0, z, z == 7 || z == 15 ? (Math.floorMod(x, 6) == 3 ? C + "coladera" : PISO) : LINOLEO);
            }
            // la alfombra roja del Coleccionista, gastada en medio
            for (int z = 10; z <= 12; z++) {
                p.set(x, 1, z, C + "alfombra_roja");
            }
        }
        // charcos que gotean del techo, papeles y manchas tirados
        for (int x = -6; x <= 42; x += 5) {
            if (p.azar(0.35)) {
                p.set(x, 1, p.azar(0.5) ? 8 : 14, C + "charco[facing=north]");
            } else if (p.azar(0.3)) {
                p.set(x, 1, p.azar(0.5) ? 8 : 14, C + (p.azar(0.5) ? "papeles" : "mancha") + "[facing=" + p.elegir("north", "east", "south", "west") + "]");
            }
        }
    }

    // ------------------------------------------------------------------ una celda

    /**
     * Celda {@code n} con su interior en x = xs..xs+4. {@code sur}: del lado sur (interior z 1..5, frente en z 6) o norte (interior z
     * 17..21, frente en z 16). {@code dy}: 0 abajo, 5 arriba.
     */
    private static void celda(Plano p, int n, int xs, int dy, boolean sur) {
        int zf = sur ? 6 : 16; // muro del frente (barrotes)
        int zi0 = sur ? 1 : 17, zi1 = sur ? 5 : 21; // interior
        int zFondo = sur ? 1 : 21; // la fila pegada a la pared del fondo
        int zFrente = sur ? 5 : 17; // la fila pegada a los barrotes
        int dz = sur ? 1 : -1; // hacia el pasillo
        String haciaPasillo = sur ? "south" : "north"; // hacia dónde mira algo puesto en el fondo
        String haciaFondo = sur ? "north" : "south";
        int y0 = dy; // piso de la celda
        // piso y techo de concreto
        p.caja(xs, y0, zi0, xs + 4, y0, zi1, PISO);
        p.caja(xs, y0 + 5, zi0, xs + 4, y0 + 5, zi1, CONCRETO);
        // frente: barrotes oxidados con dintel de acero y la puerta en medio
        for (int x = xs; x <= xs + 4; x++) {
            for (int y = y0 + 1; y <= y0 + 4; y++) {
                p.set(x, y, zf, y == y0 + 4 ? VIGA + "[axis=x]" : BARROTES);
            }
            p.set(x, y0, zf, METAL);
        }
        int xp = xs + 2;
        p.set(xp, y0 + 1, zf, C + "puerta_celda[facing=" + haciaFondo + ",half=lower,hinge=left,open=false]");
        p.set(xp, y0 + 2, zf, C + "puerta_celda[facing=" + haciaFondo + ",half=upper,hinge=left,open=false]");
        p.set(xp, y0 + 3, zf, BARROTES);
        p.marca("celda." + n + ".puerta", xp, y0 + 1, zf, sur ? 0 : 180);
        // número de la celda: placa atornillada al dintel, del lado del pasillo
        p.texto(xp, y0 + 4, zf + dz, "placa", haciaPasillo, 0xE8E2D0, "PIEZA", String.format("%02d", n));
        // el catre (cabecera contra el fondo, del lado oeste)
        p.set(xs, y0 + 1, zFondo, C + "catre[facing=" + haciaFondo + ",part=head]");
        p.set(xs, y0 + 1, zFondo + dz, C + "catre[facing=" + haciaFondo + ",part=foot]");
        p.marca("celda." + n + ".cama", xs, y0 + 1, zFondo, 0);
        // el baúl con las pistas en la esquina del fondo al este
        p.set(xs + 4, y0 + 1, zFondo, C + "cajon[facing=" + haciaPasillo + "]");
        p.marca("celda." + n + ".barril", xs + 4, y0 + 1, zFondo, 0);
        // retrete y lavabo contra la pared este
        p.set(xs + 4, y0 + 1, zFondo + 2 * dz, C + "retrete[facing=west]");
        p.set(xs + 4, y0 + 1, zFondo + 3 * dz, C + "lavabo[facing=west]");
        p.set(xs + 4, y0 + 2, zFondo + 3 * dz, C + "espejo[facing=west]");
        // la pantalla en la pared del fondo, al centro (mira hacia la celda), y una rejilla de ventilación arriba
        p.set(xs + 2, y0 + 3, zFondo, "coleccionista:pantalla[facing=" + haciaPasillo + "]");
        p.marca("celda." + n + ".pantalla", xs + 2, y0 + 3, zFondo, sur ? 0 : 180);
        p.set(xs + 4, y0 + 4, zFondo, C + "ventila[facing=" + haciaPasillo + "]");
        // el candado junto a la puerta, por dentro
        p.set(xs + 3, y0 + 2, zFrente, "coleccionista:candado[facing=" + haciaFondo + "]");
        p.marca("celda." + n + ".candado", xs + 3, y0 + 2, zFrente, sur ? 180 : 0);
        // dos rayones (pistas) en las paredes: uno en la pared este y otro arriba de los pies del catre
        p.marca("celda." + n + ".letrero", xs + 4, y0 + 3, zi0 + 2, 90);
        p.marca("celda." + n + ".letrero2", xs, y0 + 2, zFondo + dz, 270);
        // suciedad: manchas, papeles, telarañas
        if (p.azar(0.6)) {
            p.set(xs + 2, y0 + 1, zFondo + 2 * dz, C + "mancha[facing=" + p.elegir("north", "east", "south", "west") + "]");
        }
        if (p.azar(0.3)) {
            p.set(xs + 1, y0 + 1, zFondo + 3 * dz, C + "papeles[facing=" + p.elegir("north", "east", "south", "west") + "]");
        }
        if (p.azar(0.7)) {
            p.set(xs, y0 + 4, zFondo, C + "telarana");
        }
        // lo que dejaron las "piezas anteriores": grilletes en la pared y rayitas contando los días
        if (p.azar(0.35)) {
            p.set(xs, y0 + 3, zFondo + 2 * dz, C + "grilletes[facing=east]");
        }
        if (p.azar(0.45)) {
            int dias = 4 + p.r.nextInt(18);
            StringBuilder l1 = new StringBuilder(), l2 = new StringBuilder();
            for (int i = 0; i < dias; i++) {
                StringBuilder l = i < 10 ? l1 : l2;
                l.append(i % 5 == 4 ? "/ " : "I");
            }
            p.texto(xs + 1, y0 + 2, zFondo, "rayon", haciaPasillo, 0xB9B2A2, l1.toString().trim(), l2.toString().trim());
        }
        if (p.azar(0.4)) {
            p.set(xs + 4, y0 + 4, zFrente, C + "telarana");
        }
        // el foco del techo (lo prende el evento en el "¡pum!" del despertar)
        p.set(xs + 2, y0 + 4, zFondo + 2 * dz, "coleccionista:foco[encendido=false]");
        p.marca("celda." + n + ".foco", xs + 2, y0 + 4, zFondo + 2 * dz, 0);
        // la cámara del despertar: empieza arriba en la esquina de los barrotes y baja hasta la cama
        p.marca("celda." + n + ".cam1", xs + 4, y0 + 3, zFrente, 0);
        p.marca("celda." + n + ".cam2", xs + 1, y0 + 2, zFondo + 2 * dz, 0);
        // donde aparece: en medio, mirando a la pantalla
        p.marca("celda." + n, xs + 2, y0 + 1, zFrente - dz, sur ? 180 : 0);
    }

    // ------------------------------------------------------------------ pasarelas del segundo piso

    private static void pasarelas(Plano p) {
        for (int x = 0; x <= 36; x++) {
            for (int z = 7; z <= 9; z++) {
                p.set(x, 5, z, C + "rejilla_piso[type=top]");
                p.set(x, 5, 22 - z, C + "rejilla_piso[type=top]");
            }
            p.set(x, 6, 9, C + "barandal");
            p.set(x, 6, 13, C + "barandal");
            // ménsulas de acero bajo las pasarelas, cada 6
            if (x % 6 == 0) {
                p.set(x, 4, 7, VIGA + "[axis=z]");
                p.set(x, 4, 15, VIGA + "[axis=z]");
            }
        }
    }

    // ------------------------------------------------------------------ techo: vigas y tuberías

    private static void techo(Plano p) {
        for (int x = 0; x <= 36; x += 6) {
            for (int z = 6; z <= 16; z++) {
                p.set(x, 9, z, VIGA + "[axis=z]");
            }
        }
        // dos tuberías corren todo el pasillo pegadas al techo, y bajan por la pared del este
        for (int x = -7; x <= 43; x++) {
            if (Math.floorMod(x, 6) != 0 || x < 0 || x > 36) {
                p.set(x, 9, 8, C + "tubo[axis=x]");
                p.set(x, 9, 14, C + "tubo[axis=x]");
            }
        }
    }

    // ------------------------------------------------------------------ vestíbulos

    private static void vestibuloOeste(Plano p) {
        // el escenario: muro de madera oscura hasta media altura, la pantalla gigante en un marco de metal negro
        for (int z = 1; z <= 21; z++) {
            for (int y = 1; y <= 3; y++) {
                p.set(-8, y, z, C + "panel_madera");
            }
        }
        for (int z = 5; z <= 17; z++) {
            for (int y = 1; y <= 9; y++) {
                p.set(-8, y, z, NEGRO);
            }
        }
        for (int z = 6; z <= 16; z++) {
            p.set(-7, 1, z, C + "escalera_metal[facing=east]");
        }
        p.set(-7, 5, 11, "coleccionista:pantalla[facing=east,grande=true]");
        p.marca("pantalla_grande", -7, 5, 11, 270);
        // candelabros a los lados, cortinas de terciopelo y bocinas
        for (int z : new int[] {7, 15}) {
            p.set(-6, 1, z, C + "candelabro[facing=east,half=lower]");
            p.set(-6, 2, z, C + "candelabro[facing=east,half=upper]");
        }
        for (int z : new int[] {4, 18}) {
            for (int y : new int[] {1, 3, 5, 7}) {
                p.set(-7, y, z, C + "cortina[facing=east,half=lower]");
                p.set(-7, y + 1, z, C + "cortina[facing=east,half=upper]");
            }
            p.set(-7, 9, z == 4 ? 6 : 16, C + "bocina[facing=east]");
        }
        // vitrinas de la colección: muñecas de porcelana
        vitrina(p, -5, 2);
        vitrina(p, -5, 20);
        vitrina(p, -2, 2);
        vitrina(p, -2, 20);
        p.set(-4, 1, 9, C + "silla[facing=east]");
        // cámaras vigilando el pasillo
        p.set(-7, 8, 2, C + "camara[facing=east]");
        p.set(-7, 8, 20, C + "camara[facing=east]");
    }

    private static void vitrina(Plano p, int x, int z) {
        String f = z < 11 ? "south" : "north";
        p.set(x, 1, z, C + "vitrina[facing=" + f + ",half=lower]");
        p.set(x, 2, z, C + "vitrina[facing=" + f + ",half=upper]");
    }

    private static void vestibuloEste(Plano p) {
        // escaleras de metal a las pasarelas (bajan hacia el este), con barandal
        for (int i = 0; i < 5; i++) {
            int x = 37 + i, y = 5 - i;
            for (int z = 7; z <= 9; z++) {
                p.set(x, y, z, C + "escalera_metal[facing=west]");
                p.set(x, y, 22 - z, C + "escalera_metal[facing=west]");
                for (int yy = 1; yy < y; yy++) {
                    p.set(x, yy, z, METAL);
                    p.set(x, yy, 22 - z, METAL);
                }
            }
            p.set(x, y + 1, 9, C + "barandal");
            p.set(x, y + 1, 13, C + "barandal");
        }
        // el portón de salida (barrotes de 3x4) en la pared este
        for (int z = 10; z <= 12; z++) {
            for (int y = 1; y <= 4; y++) {
                p.set(44, y, z, BARROTES);
            }
            p.set(44, 5, z, VIGA + "[axis=z]");
        }
        p.marca("salida", 43, 1, 11, 270);
        p.texto(43, 6, 11, "letrero", "west", 0xB3272A, "LA COLECCIÓN", "continúa");
        p.set(43, 8, 11, C + "reloj[facing=west]");
        p.set(43, 8, 2, C + "camara[facing=west]");
        // una camilla y una silla de ruedas abandonadas en las esquinas
        p.set(40, 1, 3, C + "camilla[facing=east]");
        p.set(41, 1, 19, C + "silla_ruedas[facing=" + p.elegir("west", "north") + "]");
        p.set(43, 8, 20, C + "camara[facing=west]");
    }

    // ------------------------------------------------------------------ luces

    private static void lucesPasillo(Plano p) {
        // lámparas fluorescentes colgando sobre el pasillo (luz fría; algunas fallan)
        for (int x = -4; x <= 40; x += 3) {
            // cuelgan de un tubo que baja del techo
            p.set(x, 9, 11, C + "tubo[axis=y]");
            luz(p, x, 8, 11, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
        }
        // focos de jaula en las columnas, abajo y arriba, mirando al pasillo
        for (int k = 0; k <= 6; k++) {
            int x = 6 * k;
            luz(p, x, 3, 7, C + "lampara_jaula[facing=south,encendida=true,falla=false]");
            luz(p, x, 3, 15, C + "lampara_jaula[facing=north,encendida=true,falla=false]");
            if (k % 2 == 1) {
                luz(p, x, 8, 7, C + "lampara_jaula[facing=south,encendida=true,falla=false]");
                luz(p, x, 8, 15, C + "lampara_jaula[facing=north,encendida=true,falla=false]");
            }
        }
    }

    private static void luz(Plano p, int x, int y, int z, String bloque) {
        p.set(x, y, z, bloque);
        p.marca("ala.luz." + luces++, x, y, z, 0);
    }
}
