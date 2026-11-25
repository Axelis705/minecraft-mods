package com.coleccionista.mapa;

/**
 * La mesa de juego del Coleccionista (la final: veintiuno). Un cuarto privado y oscuro: alfombra roja, lambrín, vitrinas con sus piezas
 * favoritas, el trono vacío del Coleccionista al norte bajo la pantalla grande, y al centro una sola mesa de tapete, chica, con una lámpara
 * de billar encima: lo único bien iluminado. Los dos finalistas se sientan frente a frente (oeste y este) con la mano izquierda metida en
 * su máquina de latón, sobre la mesa.
 * <pre>
 *   x: 0 ... 49   z: -1 ... 43   y: -1 ... 14
 *   el pasillo x 2..15 (entra al cuarto por x 16), el cuarto x 17..47 / z 6..36 (alto 12)
 *   la mesa x 31..34 / z 20..22 (centro 33.0, 21.5; el tapete arriba en y 1.95); las sillas en x 30 (oeste, mira al este) y x 35
 *   (este, mira al oeste)
 * </pre>
 * Marcas: {@code v21.silla.0/1} (con su yaw), {@code v21.mesa} (el bloque del centro de la mesa), {@code v21.entrada},
 * {@code v21.salida}, {@code v21.trono}, {@code v21.luz.K} (el yaw es el nivel), {@code v21.pantalla} y el pasillo {@code pas_veintiuno}.
 */
public final class SalaVeintiuno {
    private static final String C = "coleccionista:";
    public static final int HX0 = 17, HX1 = 47, HZ0 = 6, HZ1 = 36, ALTO = 12;
    /** La mesa: de x 31 a 34 y de z 20 a 22; su centro (punto) en (33.0, 21.5) y la altura del tapete. */
    public static final int MX0 = 31, MX1 = 34, MZ0 = 20, MZ1 = 22;
    public static final double CX = 33.0, CZ = 21.5, TAPETE = 1.95;
    public static int luces;

    private SalaVeintiuno() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        luces = 0;
        p.caja(0, -1, -1, 49, ALTO + 2, 43, C + "ladrillo_oscuro");
        Pasaje.pasillo(p, "pas_veintiuno", new Marco(2, 26, 270));
        cuarto(p);
        mesa(p);
        trono(p);
        return p;
    }

    private static void luz(Plano p, int x, int y, int z, int nivel) {
        p.set(x, y, z, "light[level=" + nivel + "]");
        p.marca("v21.luz." + luces++, x, y, z, nivel);
    }

    private static void cuarto(Plano p) {
        double cx = CX - 0.5, cz = CZ - 0.5;
        for (int x = HX0; x <= HX1; x++) {
            for (int z = HZ0; z <= HZ1; z++) {
                double d = Math.hypot(x - cx, z - cz);
                // la alfombra roja redonda alrededor de la mesa (con orilla dorada); afuera, parquet
                String piso = d <= 7.2 ? C + "alfombra_roja" : d <= 7.9 ? C + "marco_dorado" : C + "parquet";
                p.set(x, 0, z, piso.equals(C + "alfombra_roja") ? C + "parquet" : piso);
                if (piso.equals(C + "alfombra_roja")) {
                    p.set(x, 1, z, C + "alfombra_roja");
                }
                for (int y = piso.equals(C + "alfombra_roja") ? 2 : 1; y <= ALTO; y++) {
                    p.set(x, y, z, "air");
                }
                boolean marco = (x - HX0) % 5 == 0 || (z - HZ0) % 5 == 0;
                p.set(x, ALTO, z, marco ? C + "marco_dorado" : C + "panel_madera");
                p.set(x, ALTO + 1, z, C + "madera_pulida");
            }
        }
        // las paredes: lambrín abajo, papel tapiz, cenefa dorada
        for (int y = 1; y < ALTO; y++) {
            String b = y <= 2 ? C + "panel_madera" : y == 3 || y == ALTO - 1 ? C + "marco_dorado" : C + "papel_tapiz";
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
        // vitrinas con sus piezas favoritas (muñecas) al este y al sur; estandartes rojos entre ellas
        for (int z = HZ0 + 3; z <= HZ1 - 3; z += 4) {
            p.set(HX1, 1, z, C + "vitrina[facing=west,half=lower]");
            p.set(HX1, 2, z, C + "vitrina[facing=west,half=upper]");
            if (z + 2 <= HZ1 - 2) {
                p.set(HX1 + 1, 6, z + 2, C + "estandarte_rojo[facing=west,half=upper]");
                p.set(HX1 + 1, 5, z + 2, C + "estandarte_rojo[facing=west,half=lower]");
            }
        }
        for (int x = HX0 + 3; x <= HX1 - 3; x += 4) {
            p.set(x, 1, HZ1, C + "vitrina[facing=north,half=lower]");
            p.set(x, 2, HZ1, C + "vitrina[facing=north,half=upper]");
        }
        // repisas con muñecas y cráneos en el muro del oeste (a los lados de la puerta)
        for (int z = HZ0 + 2; z <= HZ1 - 2; z += 3) {
            if (Math.abs(z - 21.5) < 4) {
                continue;
            }
            p.set(HX0, 3, z, C + "losa_madera");
            p.set(HX0, 4, z, z % 2 == 0 ? C + "muneca[facing=east]" : C + "craneo[facing=east]");
        }
        // candelabros en las esquinas y telarañas
        for (int[] e : new int[][] {{HX0 + 1, HZ0 + 1}, {HX1 - 1, HZ0 + 1}, {HX0 + 1, HZ1 - 1}, {HX1 - 1, HZ1 - 1}}) {
            p.set(e[0], 1, e[1], C + "candelabro[facing=south,half=lower]");
            p.set(e[0], 2, e[1], C + "candelabro[facing=south,half=upper]");
            p.set(e[0], ALTO - 1, e[1], C + "telarana");
        }
        // la luz: tenue en el cuarto (para que la mesa resalte)
        for (int x = HX0 + 3; x <= HX1 - 3; x += 7) {
            for (int z = HZ0 + 3; z <= HZ1 - 3; z += 7) {
                luz(p, x, 5, z, 10);
            }
        }
        p.marca("v21.entrada", HX0 + 2, 1, 21, 270);
        p.marca("v21.salida", HX0 + 5, 1, 21, 270);
    }

    private static void mesa(Plano p) {
        for (int x = MX0; x <= MX1; x++) {
            for (int z = MZ0; z <= MZ1; z++) {
                p.set(x, 1, z, C + "mesa_tapete[facing=east]");
            }
        }
        int z = (int) Math.floor(CZ);
        // las sillas (las pone la prueba): la del oeste mira al este (270) y la del este al oeste (90)
        p.marca("v21.silla.0", MX0 - 1, 1, z, 270);
        p.marca("v21.silla.1", MX1 + 1, 1, z, 90);
        p.marca("v21.mesa", (int) Math.floor(CX), 2, z, 0);
        // la lámpara de billar encima de la mesa, colgando de una cadena
        for (int y = 7; y <= ALTO - 1; y++) {
            p.set((int) Math.floor(CX), y, z, C + "cadena");
        }
        p.set((int) Math.floor(CX), 6, z, C + "lampara_billar[facing=east]");
        p.set((int) Math.floor(CX) - 1, 6, z, C + "lampara_billar[facing=east]");
        // la luz de la mesa: fuerte, no se baja nunca
        luz(p, (int) Math.floor(CX), 5, z, 15);
        luz(p, MX0 - 1, 5, z, 14);
        luz(p, MX1 + 1, 5, z, 14);
        // alrededor de la mesa: que se vean las máquinas, las cartas y las caras
        for (int x : new int[] {MX0, MX1}) {
            luz(p, x, 4, MZ0 - 1, 13);
            luz(p, x, 4, MZ1 + 1, 13);
        }
        // velas en las esquinas de la mesa
        p.set(MX0, 2, MZ0, C + "vela");
        p.set(MX1, 2, MZ1, C + "vela");
    }

    private static void trono(Plano p) {
        int x = (int) Math.floor(CX), z = HZ0 + 1;
        // la tarima y el trono vacío del Coleccionista, mirando a la mesa (al sur)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(x + dx, 1, z + dz, C + "marmol_negro");
            }
        }
        p.set(x, 2, z, C + "trono[facing=south,half=lower]");
        p.set(x, 3, z, C + "trono[facing=south,half=upper]");
        p.marca("v21.trono", x, 2, z, 0);
        for (int dx : new int[] {-2, 2}) {
            p.set(x + dx, 2, z, C + "candelabro[facing=south,half=lower]");
            p.set(x + dx, 3, z, C + "candelabro[facing=south,half=upper]");
        }
        luz(p, x, 4, z + 2, 11);
        // la pantalla grande arriba del trono, en el muro del norte
        p.set(x, 7, HZ0 - 1, C + "pantalla[facing=south,grande=true]");
        p.marca("v21.pantalla", x, 7, HZ0 - 1, 0);
    }
}
