package com.coleccionista.mapa;

import static com.coleccionista.mapa.Mansion.*;

/**
 * Cada cuarto de la mansión, vestido: muebles, tapetes, lámparas, cuadros, telarañas y sus escondites ({@code man.esc.N}). Las puertas y
 * las ventanas ya las puso {@link MansionObra}; aquí se cuida no tapar ninguna (al lado de cada puerta queda libre).
 */
final class MansionCuartos {
    private MansionCuartos() {
    }

    // ------------------------------------------------------------------ cosas que se repiten

    /** Un tapete de bloque en el piso (orilla y centro). */
    static void tapete(Trazo t, int x0, int z0, int x1, int z1, int piso) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean orilla = x == x0 || x == x1 || z == z0 || z == z1;
                t.set(x, piso, z, orilla ? C + "tapete_orilla" : C + "tapete_rojo");
            }
        }
    }

    /** Libreros de piso a techo (con una moldura arriba). */
    static void libreros(Trazo t, int x0, int z0, int x1, int z1, int piso, int alto) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int y = piso + 1; y <= piso + alto; y++) {
                    t.set(x, y, z, y == piso + alto ? C + "madera_pulida" : C + "estante_archivo");
                }
            }
        }
    }

    /** Las paredes de adentro de un cuarto (la cara que da al cuarto: el bloque del muro), de un material. */
    static void paredes(Trazo t, int x0, int z0, int x1, int z1, int y0, int y1, String b, boolean lambrin) {
        for (int y = y0; y <= y1; y++) {
            String m = lambrin && y <= y0 + 1 ? C + "panel_madera" : b;
            for (int x = x0 - 1; x <= x1 + 1; x++) {
                pinta(t, x, y, z0 - 1, m);
                pinta(t, x, y, z1 + 1, m);
            }
            for (int z = z0; z <= z1; z++) {
                pinta(t, x0 - 1, y, z, m);
                pinta(t, x1 + 1, y, z, m);
            }
            // el forro de los muros de afuera (si lo hay)
            for (int x = x0; x <= x1; x++) {
                forro(t, x, y, z0, m);
                forro(t, x, y, z1, m);
            }
            for (int z = z0; z <= z1; z++) {
                forro(t, x0, y, z, m);
                forro(t, x1, y, z, m);
            }
        }
    }

    /** Pinta un bloque de muro (solo si es muro de papel, lambrín o madera; no toca puertas, ventanas ni sillar). */
    private static void pinta(Trazo t, int x, int y, int z, String m) {
        String b = t.p.get(x, y, z);
        if (b != null && (b.endsWith("papel_tapiz") || b.endsWith("panel_madera") || b.contains(":papel_"))) {
            t.set(x, y, z, m);
        }
    }

    private static void forro(Trazo t, int x, int y, int z, String m) {
        String b = t.p.get(x, y, z);
        if (b != null && (b.endsWith("papel_tapiz") || b.endsWith("panel_madera"))) {
            t.set(x, y, z, m);
        }
    }

    /** Una cama doble antigua (dos camas juntas) con dosel: postes de madera y un techo de terciopelo. Cabecera hacia {@code mira}. */
    static void camaDosel(Trazo t, int x, int y, int z, String mira, boolean dosel) {
        // las dos camas: la cabecera en (x, z) y los pies hacia atrás
        int dx = mira.equals("east") ? -1 : mira.equals("west") ? 1 : 0, dz = mira.equals("south") ? -1 : mira.equals("north") ? 1 : 0;
        int lx = dz != 0 ? 1 : 0, lz = dx != 0 ? 1 : 0;
        for (int i = 0; i < 2; i++) {
            int bx = x + lx * i, bz = z + lz * i;
            t.set(bx, y, bz, C + "cama_antigua[facing=" + mira + ",part=head]");
            t.set(bx + dx, y, bz + dz, C + "cama_antigua[facing=" + mira + ",part=foot]");
        }
        if (!dosel) {
            return;
        }
        // postes en las cuatro esquinas de afuera y el dosel arriba
        int[][] esq = {{x - lx - dx * 0, z - lz}, {x + lx * 2, z + lz * 2}, {x - lx + dx, z - lz + dz}, {x + lx * 2 + dx, z + lz * 2 + dz}};
        for (int[] c : esq) {
            for (int yy = y; yy <= y + 3; yy++) {
                t.set(c[0], yy, c[1], C + "viga_madera[axis=y]");
            }
        }
        for (int i = -1; i <= 2; i++) {
            for (int j = 0; j <= 1; j++) {
                int cx = x + lx * i + dx * j, cz = z + lz * i + dz * j;
                t.set(cx, y + 4, cz, C + "telon");
            }
        }
    }

    /** Un clóset de 2 x 1 metido en un muro, con su puerta (el muro de atrás queda). */
    static void armario(Trazo t, int x, int y, int z, boolean correX, int fondo, String mira, String bisagra) {
        // (x, z): la puerta; el hueco va {fondo} hacia atrás
        int dx = correX ? 0 : fondo, dz = correX ? fondo : 0;
        t.puerta(x, y, z, "dark_oak", mira, bisagra);
        for (int yy = y; yy <= y + 2; yy++) {
            t.set(x + dx, yy, z + dz, yy == y + 2 ? C + "madera_pulida" : "air");
        }
        t.set(x + dx, y - 1, z + dz, C + "parquet");
    }

    // ------------------------------------------------------------------ planta baja

    static void plantaBaja(Trazo t) {
        biblioteca(t);
        musica(t);
        comedor(t);
        mayordomo(t);
        salonBaile(t);
        pasilloServicio(t);
        billar(t);
        trofeos(t);
        cocina(t);
    }

    private static void biblioteca(Trazo t) {
        int y = PB + 1, x0 = X0 + 1, x1 = HX0 - 2, z0 = Z0 + 1, z1 = 14;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "papel_verde", true);
        // libreros: al este (menos junto a la puerta del vestíbulo), al sur (menos la puerta de la música) y al oeste entre ventana y chimenea
        libreros(t, x1, z0 + 1, x1, 10, PB, 6);
        libreros(t, x0 + 1, z1, x0 + 2, z1, PB, 6);
        libreros(t, x1 - 2, z1, x1, z1, PB, 6);
        libreros(t, x0, 6, x0, 7, PB, 6);
        libreros(t, x0, z1 - 1, x0, z1, PB, 6);
        libreros(t, 12, z0, 13, z0, PB, 6);
        // un hueco en los libreros del este: escondite
        for (int yy = y; yy <= y + 1; yy++) {
            t.set(x1, yy, 5, "air");
        }
        t.set(x1, y + 2, 5, C + "estante_archivo");
        MansionObra.tabique(t, HX0 - 1, 5, PB + 1, PA - 1);
        esc(t, x1, y, 5);
        // tapete y sala frente a la chimenea
        tapete(t, 8, 8, 12, 13, PB);
        t.alto(8, y, 9, "sillon", "west");
        t.alto(8, y, 12, "sillon", "west");
        t.deco(9, y, 10, "mesa", "west");
        t.set(9, y + 1, 10, C + "lampara_mesa");
        t.deco(9, y, 11, "mesa", "west");
        t.set(9, y + 1, 11, C + "papeles");
        // escritorio y atril
        t.deco(12, y, 3, "mesa", "south");
        t.deco(13, y, 3, "mesa", "south");
        t.set(12, y + 1, 3, C + "lampara_mesa");
        t.set(13, y + 1, 3, C + "papeles");
        t.deco(12, y, 4, "silla", "north");
        t.deco(13, y, 7, "atril", "west");
        t.alto(14, y, 9, "busto", "west");
        t.deco(11, y, 6, "globo", "south");
        // el mirador de la torre: telescopio, sillón y libros tirados
        t.alto(8, y, -3, "telescopio", "north");
        t.alto(10, y, -3, "sillon", "south");
        t.set(7, y, -1, C + "papeles");
        t.set(6, y + 4, -4, C + "telarana");
        esc(t, 6, y, -4);
        // cuadros y lámparas
        t.deco(x0, y + 3, 1, "retrato_caballero", "east");
        t.deco(6, y + 4, 10, "retrato_dama", "east");
        t.arana(10, PA - 1, 7, 1);
        t.luz(12, PA - 2, 11, 9);
        t.luz(8, PA - 2, -2, 7);
        t.set(x1, PA - 2, z1 - 1, C + "telarana");
    }

    private static void musica(Trazo t) {
        int y = PB + 1, x0 = X0 + 1, x1 = HX0 - 2, z0 = 16, z1 = BZ0 - 2;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "papel_azul", true);
        // el piano de cola (cuerpo de madera pulida, teclas de mármol, la tapa levantada) con su banquito
        for (int x = 7; x <= 10; x++) {
            for (int z = 18; z <= 20; z++) {
                if (x == 7 && z == 20) {
                    continue;
                }
                t.set(x, y, z, C + "madera_pulida");
            }
        }
        for (int z = 18; z <= 20; z++) {
            t.set(11, y, z, C + "losa_marmol[type=top]");
        }
        t.set(8, y + 1, 18, C + "escalera_madera[facing=south,half=bottom]");
        t.set(9, y + 1, 18, C + "escalera_madera[facing=south,half=bottom]");
        t.set(9, y + 1, 19, C + "atril[facing=east]");
        t.deco(12, y, 19, "silla", "west");
        t.alto(7, y, 21, "candelabro", "east");
        // sillas para oír, un sillón, la vitrina con muñecas músicos
        for (int z = 17; z <= 21; z += 2) {
            t.deco(14, y, z, "silla", "west");
        }
        t.alto(x1, y, z0, "vitrina", "west");
        t.alto(x0, y, z0, "reloj_pie", "east");
        t.deco(13, y, z1, "atril", "north");
        t.deco(x1, y + 3, 18, "retrato_nina", "west");
        tapete(t, 7, 17, 12, 21, PB);
        t.arana(10, PA - 1, 19, 1);
        t.luz(13, PA - 2, 20, 8);
        // escondite: atrás del piano, contra la pared
        esc(t, 14, y, 16);
        t.set(x0 + 1, PA - 2, z1, C + "telarana");
    }

    private static void comedor(Trazo t) {
        int y = PB + 1, x0 = HX1 + 2, x1 = X1 - 1, z0 = Z0 + 1, z1 = 14;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "papel_tapiz", true);
        // la mesa larga con su mantel (mesas juntas), sillas a los lados y sillones en las cabeceras
        for (int z = 4; z <= 11; z++) {
            for (int x = 42; x <= 43; x++) {
                t.deco(x, y, z, "mesa", "south");
            }
            t.deco(41, y, z, "silla", "east");
            t.deco(44, y, z, "silla", "west");
        }
        tapete(t, 40, 2, 45, 13, PB);
        t.alto(42, y, 3, "sillon", "south");
        t.alto(43, y, 12, "sillon", "north");
        // encima: candelabros, velas, platos (cráneos de la colección...), papeles
        t.alto(42, y + 1, 5, "candelabro", "south");
        t.alto(43, y + 1, 8, "candelabro", "south");
        t.alto(42, y + 1, 10, "candelabro", "south");
        t.set(43, y + 1, 6, C + "vela");
        t.set(42, y + 1, 7, C + "craneo");
        t.set(43, y + 1, 10, C + "vela");
        // el aparador y las vitrinas contra los muros
        t.alto(x0, y, 4, "vitrina", "east");
        t.alto(x0, y, 8, "vitrina", "east");
        t.deco(x0, y, 6, "cajon", "east");
        t.set(x0, y + 1, 6, C + "vela");
        t.alto(x1, y, 14, "reloj_pie", "west");
        t.deco(x0, y + 3, 6, "retrato_dama", "east");
        t.deco(x1, y + 3, 1, "retrato_caballero", "west");
        // el mirador de la torre del este: una mesita para el té y dos sillas
        t.deco(44, y, -2, "mesa", "north");
        t.set(44, y + 1, -2, C + "vela");
        t.deco(43, y, -2, "silla", "east");
        t.deco(45, y, -2, "silla", "west");
        t.alto(47, y, -4, "planta", "west");
        t.arana(42, PA - 1, 6, 1);
        t.arana(43, PA - 1, 10, 1);
        t.luz(39, PA - 2, 9, 8);
        // escondite: entre la vitrina y la esquina
        esc(t, x0, y, 2);
        esc(t, 46, y, -4);
    }

    private static void mayordomo(Trazo t) {
        int y = PB + 1, x0 = HX1 + 2, x1 = X1 - 1, z0 = 16, z1 = BZ0 - 2;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "panel_madera", false);
        // el tablero de las campanas (placas), un escritorio, la caja fuerte, el perchero (maniquí) y cajones
        t.p.texto(x0, y + 2, 17, "placa", "east", 0xB89A5A, "BIBLIOTECA", "COMEDOR", "RECAMARA");
        t.p.texto(x0, y + 2, 18, "placa", "east", 0xB89A5A, "BEBE", "ESTUDIO", "HUESPEDES");
        t.deco(40, y, z1, "mesa", "north");
        t.deco(41, y, z1, "mesa", "north");
        t.set(40, y + 1, z1, C + "lampara_mesa");
        t.set(41, y + 1, z1, C + "papeles");
        t.deco(40, y, z1 - 1, "silla", "south");
        t.deco(x1, y, 17, "cofre_boveda", "west");
        t.alto(x1, y, 19, "maniqui", "west");
        t.deco(x1, y, 20, "cajon", "west");
        t.set(x1, y + 1, 20, C + "cajon[facing=west]");
        t.set(x0 + 1, y + 2, 16, C + "argolla[facing=south]");
        t.set(x0 + 2, y + 2, 16, C + "argolla[facing=south]");
        t.p.letrero(44, y + 2, z1, "letrero", "north", "gold", "SOTANO", "->");
        // un armario empotrado (en el muro del vestíbulo no: en la esquina) para esconderse
        for (int yy = y; yy <= PA - 2; yy++) {
            t.set(x0 + 1, yy, 21, C + "panel_madera");
            t.set(x0 + 1, yy, 22, yy >= y + 2 ? C + "panel_madera" : "air");
        }
        t.puerta(x0 + 1, y, 22, "dark_oak", "east", "left");
        esc(t, x0, y, 22);
        t.luz(42, PA - 2, 19, 8);
        t.set(x1, y + 3, z1, C + "farol");
    }

    private static void salonBaile(Trazo t) {
        int y = PB + 1, x0 = BX0, x1 = BX1, z0 = BZ0, z1 = BZ1;
        // el piso: parquet con orilla de mármol negro y una estrella de mármol con oro al centro
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean orilla = x == 9 || x == x1 || z == z0 || z == z1;
                double d = Math.hypot(x + 0.5 - 24.5, z + 0.5 - 31.0);
                String b = orilla ? C + "marmol_negro" : d < 2.0 ? C + "marmol_oro" : d < 4.5 && ((x + z) & 1) == 0 ? C + "marmol_crema"
                        : C + "parquet";
                t.set(x, PB, z, b);
            }
        }
        // el balcón del oeste (arriba): balaustrada, sillones, candelabros
        for (int z = z0; z <= z1; z++) {
            t.set(9, PA + 1, z, C + "balaustrada");
            t.set(9, PA, z, C + "parquet");
            t.set(9, PA - 1, z, C + "cornisa");
        }
        t.alto(8, PA + 1, 27, "sillon", "east");
        t.alto(8, PA + 1, 34, "sillon", "east");
        t.alto(8, PA + 1, 30, "candelabro", "east");
        t.alto(8, PA + 1, 32, "planta", "east");
        esc(t, 8, PA + 1, 36);
        // abajo del balcón: sillones contra la pared
        for (int z = 26; z <= 35; z += 3) {
            t.alto(7, y, z, "sillon", "east");
        }
        // el escenario de los músicos al este (una tarima) y el telón detrás, con un pasillo escondido
        for (int x = 36; x <= 40; x++) {
            for (int z = 27; z <= 34; z++) {
                t.set(x, y, z, x == 36 ? C + "escalera_madera[facing=west,half=bottom]" : C + "madera_pulida");
            }
        }
        for (int z = 26; z <= 35; z++) {
            for (int yy = y; yy <= PA + 3; yy++) {
                t.set(41, yy, z, yy == PA + 3 ? C + "telon_borde" : C + "telon");
            }
        }
        for (int yy = y; yy <= y + 1; yy++) {
            t.set(41, yy, 26, "air");
        }
        for (int z = 26; z <= 35; z++) {
            for (int yy = y; yy <= y + 2; yy++) {
                t.set(x1, yy, z, "air");
            }
        }
        esc(t, x1, y, 33);
        for (int[] a : new int[][] {{38, 28}, {38, 33}, {39, 30}}) {
            t.deco(a[0], y + 1, a[1], "atril", "west");
            t.deco(a[0] + 1, y + 1, a[1], "silla", "west");
        }
        // los bailarines: maniquíes en pareja, quietos a media pieza
        for (int[] a : new int[][] {{14, 28}, {15, 28}, {20, 33}, {21, 33}, {28, 27}, {29, 27}, {30, 34}, {31, 34}}) {
            t.alto(a[0], y, a[1], "maniqui", a[0] % 2 == 0 ? "east" : "west");
        }
        // espejos y candelabros en los muros, una mesa de bocadillos
        for (int x : new int[] {14, 21, 25, 29}) {
            t.deco(x, y + 2, z0, "espejo", "south");
        }
        for (int x : new int[] {13, 16, 22, 27, 31}) {
            t.alto(x, y, z0, "candelabro", "south");
        }
        for (int x : new int[] {13, 29}) {
            t.deco(x, y, z1, "mesa", "north");
            t.deco(x + 1, y, z1, "mesa", "north");
            t.set(x, y + 1, z1, C + "vela");
            t.set(x + 1, y + 1, z1, C + "craneo");
        }
        // las arañas grandes colgando de cadenas largas
        for (int x : new int[] {16, 25, 33}) {
            t.arana(x, ATI - 1, 30, 5);
            t.arana(x + 1, ATI - 1, 31, 5);
        }
        for (int x = 12; x <= 36; x += 8) {
            t.luz(x, PA + 3, 27, 10);
            t.luz(x, PA + 3, 34, 10);
        }
        t.luz(8, PA + 3, 30, 8);
        // estandartes arriba
        for (int x = 14; x <= 34; x += 10) {
            t.alto(x, PA + 2, z0, "estandarte_rojo", "south");
            t.alto(x, PA + 2, z1, "estandarte_negro", "north");
        }
        t.set(x1, ATI - 2, z0, C + "telarana");
    }

    private static void pasilloServicio(Trazo t) {
        int y = PB + 1;
        // tubos por el techo, faroles, una banca y el letrero del sótano
        for (int z = BZ0; z <= BZ1; z++) {
            t.set(44, PA - 2, z, C + "tubo[axis=z]");
            t.set(44, ATI - 2, z, C + "tubo[axis=z]");
        }
        t.set(45, PA - 2, 27, C + "farol");
        t.set(45, PA - 2, 35, C + "farol");
        t.set(45, ATI - 2, 31, C + "farol");
        t.p.letrero(44, y + 1, BZ0, "letrero", "south", "red", "SOTANO");
        t.set(44, PA + 1, 36, C + "cajon[facing=east]");
        t.set(44, PA + 1, 26, C + "barril[facing=east]");
    }

    private static void billar(Trazo t) {
        int y = PB + 1, x0 = X0 + 1, x1 = 18, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "panel_madera", false);
        // la mesa de billar (2 x 3) con su lámpara, y una mesa de cartas
        for (int x = 10; x <= 11; x++) {
            for (int z = 43; z <= 45; z++) {
                t.deco(x, y, z, "mesa_tapete", "north");
            }
        }
        t.set(10, PA - 2, 44, C + "lampara_billar");
        t.set(11, PA - 2, 44, C + "lampara_billar");
        t.deco(15, y, 47, "mesa_tapete", "north");
        t.deco(16, y, 47, "mesa_tapete", "north");
        t.deco(15, y, 48, "silla", "north");
        t.deco(16, y, 46, "silla", "south");
        t.deco(17, y, 47, "silla", "west");
        t.set(15, y + 1, 47, C + "papeles");
        t.set(14, PA - 2, 47, C + "lampara_billar");
        // la barra (mesas en fila) con botellas (velas) y el escondite detrás
        for (int z = 40; z <= 42; z++) {
            t.deco(16, y, z, "mesa", "west");
        }
        t.set(16, y + 1, 41, C + "vela");
        t.alto(x1, y, 40, "vitrina", "west");
        esc(t, x1, y, 42);
        // trofeos de caza, sillones, un reloj
        t.deco(x0, y + 3, 45, "cabeza_trofeo", "east");
        t.deco(x0, y + 3, 40, "cabeza_trofeo", "east");
        t.deco(12, y + 3, z1, "cabeza_trofeo", "north");
        t.alto(x0, y, 45, "sillon", "east");
        t.alto(x0, y, 46, "sillon", "east");
        t.alto(x0, y, z1, "reloj_pie", "east");
        t.alto(x1, y, z1, "armadura", "west");
        tapete(t, 8, 41, 13, 47, PB);
        t.luz(8, PA - 2, 40, 7);
    }

    private static void trofeos(Trazo t) {
        int y = PB + 1, x0 = 20, x1 = 33, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "papel_dorado", true);
        // la colección: vitrinas con muñecas en filas, pedestales con bustos y cráneos, armaduras, cabezas en los muros
        for (int x = 22; x <= 31; x += 3) {
            t.alto(x, y, 42, "vitrina", "south");
            t.alto(x, y, 47, "vitrina", "north");
        }
        t.alto(x0, y, 41, "armadura", "east");
        t.alto(x1, y, 41, "armadura", "west");
        t.alto(x0, y, 48, "busto", "east");
        t.alto(x1, y, 48, "busto", "west");
        for (int x = 23; x <= 30; x += 7) {
            t.set(x, y, 44, C + "marmol_negro");
            t.set(x, y + 1, 44, C + "craneo");
        }
        t.deco(x0, y + 3, 43, "cabeza_trofeo", "east");
        t.deco(x1, y + 3, 46, "cabeza_trofeo", "west");
        t.deco(24, y + 3, z0, "retrato_caballero", "south");
        t.deco(29, y + 3, z0, "retrato_dama", "south");
        t.p.texto(26, y + 2, z0, "placa", "south", 0xD9A93A, "LA COLECCION", "DEL SEÑOR", "DE LA CASA");
        t.alto(25, y, z1, "planta", "north");
        t.alto(28, y, z1, "planta", "north");
        for (int z = 40; z <= 50; z++) {
            t.set(26, y, z, C + "alfombra_roja");
            t.set(27, y, z, C + "alfombra_roja");
        }
        t.arana(26, PA - 1, 44, 1);
        t.arana(27, PA - 1, 45, 1);
        t.luz(21, PA - 2, 45, 8);
        t.luz(32, PA - 2, 45, 8);
        esc(t, 21, y, 49);
        esc(t, 32, y, 40);
    }

    private static void cocina(Trazo t) {
        int y = PB + 1, x0 = 35, x1 = X1 - 1, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, PA - 2, C + "azulejo", false);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                t.set(x, PB, z, C + "linoleo");
            }
        }
        // el fogón grande en el muro del sur (x 40..41, con su chimenea afuera)
        for (int x = 39; x <= 42; x++) {
            for (int yy = y; yy <= y + 3; yy++) {
                t.set(x, yy, z1, x == 39 || x == 42 || yy >= y + 2 ? C + "ladrillo_viejo" : "air");
            }
        }
        for (int x = 40; x <= 41; x++) {
            t.set(x, y, Z1, C + "brasas");
            t.set(x, y + 1, Z1, "air");
            t.set(x, PB, Z1, C + "ladrillo_oscuro");
            t.set(x, y, z1, C + "brasas");
            t.set(x, y + 1, z1, "air");
        }
        for (int x = 39; x <= 42; x++) {
            t.set(x, y + 2, z1 - 1, C + "losa_madera[type=top]");
        }
        t.set(40, y + 1, z1 - 1, C + "cadena[axis=y]");
        // la mesa del centro, los muebles de los muros, las ollas colgadas
        for (int x = 39; x <= 43; x++) {
            t.deco(x, y, 44, "mesa", "south");
        }
        t.set(39, y + 1, 44, C + "vela");
        t.set(41, y + 1, 44, C + "sangre");
        t.set(43, y + 1, 44, C + "papeles");
        for (int z = 41; z <= 47; z++) {
            if (z == 44 || z == 45) {
                continue;
            }
            t.deco(x0, y, z, "cajon", "east");
        }
        for (int x = 36; x <= 38; x++) {
            t.deco(x, y, z1, "cajon", "north");
        }
        t.set(x1, y, 40, C + "barril[facing=west]");
        t.set(x1, y, 41, C + "barril[facing=west]");
        t.set(x1, y + 1, 40, C + "barril[facing=west]");
        for (int x = 39; x <= 43; x += 2) {
            t.set(x, PA - 2, 44, C + "cadena[axis=y]");
            t.set(x, PA - 3, 44, C + "farol");
        }
        t.set(44, y, z1, C + "sangre");
        t.set(x1, y + 1, 45, C + "lavabo[facing=west]");
        t.luz(37, PA - 2, 42, 8);
        // la despensa (un cuartito con puerta) para esconderse
        for (int yy = y; yy <= PA - 2; yy++) {
            for (int z = 47; z <= 50; z++) {
                t.set(45, yy, z, C + "panel_madera");
            }
            t.set(46, yy, 47, C + "panel_madera");
            t.set(47, yy, 47, C + "panel_madera");
        }
        t.puerta(45, y, 49, "dark_oak", "west", "left");
        t.set(47, y, 50, C + "barril[facing=north]");
        t.set(47, y + 1, 50, C + "cajon[facing=north]");
        esc(t, 46, y, 49);
    }

    // ------------------------------------------------------------------ planta alta

    static void plantaAlta(Trazo t) {
        recamara(t);
        bebe(t);
        estudio(t);
        huespedes(t);
        costura(t);
        bano(t);
        servidumbre(t);
    }

    private static void recamara(Trazo t) {
        int y = PA + 1, x0 = X0 + 1, x1 = HX0 - 2, z0 = Z0 + 1, z1 = 12;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "papel_tapiz", true);
        tapete(t, 10, 4, 15, 9, PA);
        // la cama con dosel (cabecera al este), burós con lámparas, roperos, tocador, sillón frente a la chimenea
        camaDosel(t, x1, y, 6, "east", true);
        t.deco(x1, y, 4, "cajon", "west");
        t.set(x1, y + 1, 4, C + "lampara_mesa");
        t.deco(x1, y, 9, "cajon", "west");
        t.set(x1, y + 1, 9, C + "lampara_mesa");
        t.alto(12, y, z0, "ropero", "south");
        t.alto(13, y, z0, "ropero", "south");
        t.deco(x0, y, 3, "mesa", "east");
        t.deco(x0, y + 2, 3, "espejo", "east");
        t.deco(x0 + 1, y, 3, "silla", "west");
        t.alto(8, y, 11, "sillon", "west");
        t.deco(x0 + 2, y, z1, "cuna", "north");
        t.deco(x1, y + 3, 7, "retrato_dama", "west");
        // el mirador: un maniquí con vestido, cajones (vestidor), y detrás un escondite
        t.alto(8, y, -3, "maniqui", "south");
        t.alto(10, y, -4, "maniqui", "south");
        t.deco(7, y, -4, "cajon", "south");
        esc(t, 6, y, -4);
        t.arana(9, ATI - 1, 6, 1);
        t.luz(12, ATI - 2, 10, 8);
        t.set(6, ATI - 2, 1, C + "telarana");
        esc(t, 14, y, 11);
    }

    private static void bebe(Trazo t) {
        int y = PA + 1, x0 = X0 + 1, x1 = HX0 - 2, z0 = 14, z1 = BZ0 - 2;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "papel_rosa", true);
        tapete(t, 8, 16, 13, 21, PA);
        // la cuna al centro, el caballito, muñecas por todos lados mirando a la puerta, una camita y el baúl de juguetes
        t.deco(10, y, 18, "cuna", "north");
        t.deco(13, y, 20, "caballito", "west");
        t.set(x1, y, 15, C + "cama_antigua[facing=east,part=head]");
        t.set(x1 - 1, y, 15, C + "cama_antigua[facing=east,part=foot]");
        t.deco(x1, y, 17, "cofre_boveda", "west");
        for (int[] m : new int[][] {{7, 16}, {8, 21}, {12, 17}, {14, 19}, {9, 15}, {11, 22}, {6, 19}}) {
            t.set(m[0], y, m[1], C + "muneca[facing=north]");
        }
        t.set(x1, y + 1, 17, C + "muneca[facing=west]");
        t.deco(x0, y + 2, 20, "retrato_nina", "east");
        t.deco(x1, y + 2, 18, "reloj", "west");
        t.p.texto(x0, y + 1, 15, "rayon", "east", 0x5A3A3C, "JUEGA", "CONMIGO");
        t.arana(10, ATI - 1, 18, 2);
        t.luz(13, ATI - 2, 16, 7);
        t.set(x0, ATI - 2, z0, C + "telarana");
        // el clóset de los juguetes (con puerta) en la esquina
        for (int yy = y; yy <= ATI - 2; yy++) {
            t.set(13, yy, 21, C + "panel_madera");
            t.set(13, yy, 22, yy >= y + 2 ? C + "panel_madera" : "air");
            t.set(14, yy, 21, C + "panel_madera");
            t.set(15, yy, 21, C + "panel_madera");
        }
        t.puerta(13, y, 22, "dark_oak", "west", "right");
        t.set(15, y, 22, C + "muneca[facing=west]");
        esc(t, 14, y, 22);
    }

    private static void estudio(Trazo t) {
        int y = PA + 1, x0 = HX1 + 2, x1 = X1 - 1, z0 = Z0 + 1, z1 = 12;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "papel_verde", true);
        // el escritorio grande, libreros, archiveros, la caja fuerte, el globo; un cuartito secreto detrás de un librero
        libreros(t, x0, 4, x0, 10, PA, 5);
        libreros(t, 40, z1, 42, z1, PA, 5);
        for (int z = 6; z <= 8; z++) {
            t.set(x0, y, z, "air");
            t.set(x0, y + 1, z, "air");
        }
        // el cuarto secreto: x 38..39, z 6..8 (el librero de enfrente tiene un hueco)
        for (int z = 5; z <= 9; z++) {
            for (int yy = y; yy <= ATI - 2; yy++) {
                t.set(x0 + 2, yy, z, C + "estante_archivo");
            }
        }
        for (int yy = y; yy <= ATI - 2; yy++) {
            t.set(x0 + 1, yy, 5, C + "estante_archivo");
            t.set(x0 + 1, yy, 9, C + "estante_archivo");
        }
        t.set(x0 + 2, y, 7, "air");
        t.set(x0 + 2, y + 1, 7, "air");
        t.set(x0, y, 6, C + "cofre_boveda[facing=east]");
        t.set(x0, y, 8, C + "papeles");
        t.set(x0 + 1, ATI - 2, 7, C + "telarana");
        esc(t, x0 + 1, y, 7);
        tapete(t, 42, 2, 46, 10, PA);
        t.deco(44, y, 4, "mesa", "south");
        t.deco(45, y, 4, "mesa", "south");
        t.set(44, y + 1, 4, C + "lampara_mesa");
        t.set(45, y + 1, 4, C + "papeles");
        t.alto(44, y, 3, "sillon", "south");
        t.set(x1, y, 3, C + "archivero");
        t.set(x1, y + 1, 3, C + "archivero");
        t.deco(43, y, 9, "globo", "south");
        t.alto(x1, y, z1, "reloj_pie", "west");
        t.alto(44, y, -3, "telescopio", "north");
        t.deco(42, y, -4, "atril", "south");
        t.deco(x1, y + 3, 7, "cabeza_trofeo", "west");
        t.arana(44, ATI - 1, 7, 1);
        t.luz(46, ATI - 2, 10, 8);
        esc(t, 47, y, -4);
    }

    private static void huespedes(Trazo t) {
        int y = PA + 1, x0 = HX1 + 2, x1 = X1 - 1, z0 = 14, z1 = BZ0 - 2;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "papel_azul", true);
        camaDosel(t, x1, y, 17, "east", false);
        t.deco(x1, y, 16, "cajon", "west");
        t.set(x1, y + 1, 16, C + "vela");
        t.alto(x0, y, 15, "ropero", "east");
        t.alto(x0, y, 21, "maniqui", "east");
        t.deco(41, y, 20, "mesa", "north");
        t.deco(41, y, 21, "silla", "north");
        t.set(41, y + 1, 20, C + "lampara_mesa");
        t.deco(x1, y + 3, 18, "retrato_caballero", "west");
        for (int[] c : new int[][] {{x0, z0}, {x1, z1}, {42, 18}}) {
            t.set(c[0], ATI - 2, c[1], C + "telarana");
        }
        tapete(t, 41, 16, 45, 20, PA);
        t.luz(42, ATI - 2, 18, 7);
        esc(t, x0, y, 16);
    }

    private static void costura(Trazo t) {
        int y = PA + 1, x0 = X0 + 1, x1 = 18, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "papel_dorado", true);
        tapete(t, 8, 41, 16, 48, PA);
        // maniquíes con vestidos, máquinas de coser, rollos de tela, cajones con hilos
        for (int[] m : new int[][] {{9, 42}, {12, 42}, {15, 42}, {11, 47}, {14, 47}}) {
            t.alto(m[0], y, m[1], "maniqui", m[1] < 45 ? "south" : "north");
        }
        for (int x = 8; x <= 14; x += 3) {
            t.deco(x, y, z1, "mesa", "north");
            t.set(x, y + 1, z1, C + "maquina_coser");
            t.deco(x, y, z1 - 1, "silla", "south");
        }
        for (int z = 41; z <= 43; z++) {
            t.set(x1, y, z, C + "telon");
            t.set(x1, y + 1, z, z == 42 ? C + "telon" : "air");
        }
        t.deco(x0, y, 44, "cajon", "east");
        t.deco(x0, y, 45, "cajon", "east");
        t.set(x0, y + 1, 44, C + "papeles");
        t.deco(x0, y + 2, 46, "espejo", "east");
        t.alto(x0, y, 47, "maniqui", "east");
        t.p.texto(x1, y + 2, 47, "rayon", "west", 0x6A5226, "A LA MEDIDA", "DE CADA UNO");
        t.arana(12, ATI - 1, 45, 1);
        t.luz(16, ATI - 2, 45, 7);
        t.set(x0, ATI - 2, z1, C + "telarana");
        esc(t, x1, y, 40);
        esc(t, 16, y, 48);
    }

    private static void bano(Trazo t) {
        int y = PA + 1, x0 = 20, x1 = 33, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "azulejo", false);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                t.set(x, PA, z, C + "linoleo");
            }
        }
        // la tina de patas (dos mitades), lavabos con espejo, retrete, un biombo de tela y charcos
        t.deco(24, y, 45, "tina", "north");
        t.deco(24, y, 46, "tina", "south");
        t.deco(29, y, 45, "tina", "north");
        t.deco(29, y, 46, "tina", "south");
        t.deco(22, y, z0, "lavabo", "south");
        t.deco(22, y + 1, z0, "espejo", "south");
        t.deco(31, y, z0, "lavabo", "south");
        t.deco(31, y + 1, z0, "espejo", "south");
        t.deco(x0, y, 48, "retrete", "east");
        t.deco(x1, y, 48, "retrete", "west");
        for (int x : new int[] {21, 22, 31, 32}) {
            t.set(x, y, 47, C + "telon");
            t.set(x, y + 1, 47, C + "telon_borde");
        }
        t.set(26, y, 44, C + "charco");
        t.set(27, y, 47, C + "charco");
        t.set(24, y, 47, C + "sangre");
        t.p.texto(26, y + 2, z0, "rayon", "south", 0x7A2020, "NO TE", "ESCONDAS", "AQUI");
        t.arana(26, ATI - 1, 45, 1);
        t.luz(26, ATI - 2, 48, 7);
        esc(t, 21, y, 49);
        esc(t, 32, y, 49);
    }

    private static void servidumbre(Trazo t) {
        int y = PA + 1, x0 = 35, x1 = X1 - 1, z0 = BZ1 + 2, z1 = Z1 - 1;
        paredes(t, x0, z0, x1, z1, y, ATI - 2, C + "yeso", false);
        // catres en fila, cajones, un lavabo, ropa colgada
        for (int x = 36; x <= 42; x += 3) {
            t.set(x, y, 40, C + "catre[facing=north,part=head]");
            t.set(x, y, 41, C + "catre[facing=north,part=foot]");
            t.deco(x + 1, y, 40, "cajon", "south");
            t.set(x + 1, y + 1, 40, C + "vela");
        }
        t.deco(x0, y, 47, "lavabo", "east");
        t.deco(x0, y + 1, 47, "espejo", "east");
        t.alto(x0, y, 49, "maniqui", "east");
        t.set(40, y, 48, C + "barril[facing=north]");
        t.set(41, y, 48, C + "cajon[facing=north]");
        t.set(40, y + 1, 48, C + "cajon[facing=north]");
        t.p.texto(x0, y + 2, 44, "rayon", "east", 0x5A5A5A, "NADIE SALE", "DE AQUI");
        t.luz(40, ATI - 2, 45, 6);
        t.set(x0, ATI - 2, z0, C + "telarana");
        esc(t, 44, y, 49);
        esc(t, 38, y, 49);
    }

    // ------------------------------------------------------------------ el ático

    static void atico(Trazo t) {
        int y = ATI + 1;
        // vigas y pilares de madera (el armazón del techo)
        for (int z = 4; z <= 48; z += 6) {
            for (int x = 8; x <= 45; x++) {
                if (t.p.vacio(x, AZOTEA - 1, z)) {
                    t.set(x, AZOTEA - 1, z, C + "viga_madera[axis=x]");
                }
            }
            for (int x = 10; x <= 43; x += 11) {
                for (int yy = y; yy < AZOTEA - 1; yy++) {
                    if (t.p.vacio(x, yy, z)) {
                        t.set(x, yy, z, C + "viga_madera[axis=y]");
                    }
                }
            }
        }
        // montones de cajas y muebles viejos que hacen pasillos y rincones
        int[][] montones = {{12, 8, 15, 10}, {20, 7, 22, 12}, {30, 9, 34, 10}, {38, 14, 40, 18}, {14, 18, 17, 20}, {24, 24, 27, 26}, {33, 22, 35, 27},
                {9, 28, 12, 30}, {18, 33, 21, 35}, {29, 32, 32, 34}, {38, 30, 41, 32}, {12, 40, 15, 42}, {22, 41, 24, 45}, {31, 40, 34, 42},
                {8, 46, 10, 48}};
        for (int[] m : montones) {
            for (int x = m[0]; x <= m[2]; x++) {
                for (int z = m[1]; z <= m[3]; z++) {
                    int h = 1 + Math.floorMod(x * 31 + z * 17, 3);
                    for (int yy = y; yy < y + h; yy++) {
                        if (t.p.vacio(x, yy, z)) {
                            t.set(x, yy, z, (x + z + yy) % 3 == 0 ? C + "barril[facing=north]" : C + "cajon[facing=" + (z % 2 == 0 ? "north" : "east") + "]");
                        }
                    }
                }
            }
        }
        // muebles viejos sueltos: roperos, espejos, el caballito, retratos recargados, maniquíes y muñecas
        t.alto(18, y, 15, "ropero", "south");
        t.alto(27, y, 15, "ropero", "north");
        t.alto(36, y, 37, "maniqui", "west");
        t.alto(16, y, 26, "maniqui", "east");
        t.deco(25, y, 20, "caballito", "east");
        t.deco(28, y, 38, "cuna", "north");
        t.alto(40, y, 24, "reloj_pie", "west");
        t.alto(11, y, 35, "sillon", "east");
        for (int[] m : new int[][] {{19, 22}, {29, 29}, {13, 44}, {37, 44}, {23, 9}, {35, 5}}) {
            t.set(m[0], y, m[1], C + "muneca[facing=south]");
        }
        for (int[] m : new int[][] {{12, 12}, {33, 16}, {21, 37}, {40, 42}}) {
            t.set(m[0], y, m[1], C + "papeles");
        }
        // telarañas en las vigas y poca luz (faroles colgados)
        for (int z = 4; z <= 48; z += 12) {
            for (int x = 12; x <= 42; x += 10) {
                if (t.p.vacio(x, AZOTEA - 2, z + 1)) {
                    t.set(x, AZOTEA - 2, z + 1, C + "telarana");
                }
            }
        }
        for (int[] f : new int[][] {{16, 10}, {30, 20}, {20, 30}, {36, 34}, {14, 44}, {30, 45}}) {
            t.set(f[0], AZOTEA - 2, f[1], C + "farol");
        }
        // los desvanes de los hastiales (al frente y atrás): un baúl, un retrato tapado y un escondite
        t.deco(26, y, 2, "cofre_boveda", "south");
        t.set(27, y + 4, 2, C + "telarana");
        t.alto(23, y, 1, "maniqui", "south");
        t.deco(26, y, Z1 - 2, "cofre_boveda", "north");
        t.alto(30, y, Z1 - 1, "maniqui", "north");
        // escondites entre las cajas
        for (int[] e : new int[][] {{11, 9}, {21, 13}, {37, 17}, {15, 21}, {26, 23}, {34, 28}, {10, 31}, {22, 34}, {39, 33}, {14, 43}, {25, 42},
                {33, 43}, {9, 49}, {36, 3}, {23, 2}}) {
            esc(t, e[0], y, e[1]);
        }
        // los cuartos de las torres a la altura del ático: cajas, telarañas, un retrato
        for (boolean este : new boolean[] {false, true}) {
            int x = este ? t.e(7) : 7;
            t.set(x, y, -4, C + "cajon[facing=south]");
            t.set(x, y + 1, -4, C + "cajon[facing=south]");
            t.set(x, ATI + 5, -1, C + "telarana");
            esc(t, este ? t.e(6) : 6, y, -1);
            // el mirador de arriba: una banca, un telescopio y velas
            t.alto(este ? t.e(8) : 8, TORRE + 1, -1, "telescopio", "north");
            t.deco(este ? t.e(7) : 7, TORRE + 1, 1, "banca", "south");
            t.set(este ? t.e(6) : 6, TORRE + 1, -4, C + "vela");
            t.luz(este ? t.e(8) : 8, TORRE + 3, 0, 7);
            esc(t, este ? t.e(6) : 6, TORRE + 1, 1);
        }
        t.luz(26, y + 3, 26, 5);
    }

    // ------------------------------------------------------------------ el sótano

    static void sotano(Trazo t) {
        int y = SOT + 1;
        // pasillo de servicio del sótano: tubos, faroles, lodo y charcos
        for (int z = Z0 + 1; z <= Z1 - 1; z++) {
            if (z < BZ0 - 1 || z > BZ0 + 7) {
                t.set(47, PB - 2, z, C + "tubo[axis=z]");
            }
            if (z % 8 == 2) {
                t.set(45, PB - 2, z, C + "farol");
            }
        }
        for (int z : new int[] {14, 37, 45}) {
            t.set(44, y, z, C + "charco");
        }
        t.p.letrero(47, y + 1, 11, "letrero", "west", "red", "SALIDA");
        // la bodega de vinos (x 6..20, z 1..22): barriles en hileras, columnas, mesa de catar
        for (int x = X0 + 1; x <= 20; x++) {
            for (int z = Z0 + 1; z <= 22; z++) {
                t.set(x, SOT, z, C + "losa_piedra");
            }
        }
        for (int z = 2; z <= 21; z++) {
            if (z % 5 == 0) {
                continue;
            }
            t.set(X0 + 1, y, z, C + "barril[facing=east]");
            t.set(X0 + 1, y + 1, z, C + "barril[facing=east]");
        }
        for (int x = 9; x <= 18; x += 3) {
            for (int z = 4; z <= 19; z++) {
                if (z == 11 || z == 12) {
                    continue;
                }
                t.set(x, y, z, C + "barril[facing=north]");
                if (z % 2 == 0) {
                    t.set(x, y + 1, z, C + "barril[facing=north]");
                }
            }
        }
        for (int x = 8; x <= 19; x += 6) {
            for (int yy = y; yy < PB - 1; yy++) {
                t.set(x, yy, 11, C + "viga_madera[axis=y]");
            }
        }
        t.deco(13, y, 21, "mesa", "north");
        t.set(13, y + 1, 21, C + "vela");
        t.deco(14, y, 21, "mesa", "north");
        t.set(14, y + 1, 21, C + "craneo");
        for (int x = 8; x <= 18; x += 5) {
            t.set(x, PB - 2, 6, C + "telarana");
            t.set(x, PB - 2, 17, C + "farol");
        }
        esc(t, 10, y, 9);
        esc(t, 16, y, 16);
        // el cuarto del carbón (x 22..28): montones negros y una pala
        for (int x = 22; x <= 28; x++) {
            for (int z = 1; z <= 22; z++) {
                int h = Math.max(0, 3 - Math.abs(x - 25) - (z > 6 ? 3 : 0));
                for (int yy = y; yy < y + h; yy++) {
                    t.set(x, yy, z, C + "lodo");
                }
            }
        }
        t.set(26, PB - 2, 14, C + "farol");
        esc(t, 23, y, 21);
        // las calderas (x 30..42, z 1..14): una caldera grande de cobre con su fogón y tubos que suben
        for (int x = 33; x <= 38; x++) {
            for (int z = 4; z <= 9; z++) {
                boolean esq = (x == 33 || x == 38) && (z == 4 || z == 9);
                if (esq) {
                    continue;
                }
                for (int yy = y; yy <= y + 3; yy++) {
                    t.set(x, yy, z, yy == y ? C + "metal_negro" : C + "cobre_viejo");
                }
            }
        }
        for (int z = 6; z <= 7; z++) {
            t.set(33, y, z, C + "brasas");
            t.set(33, y + 1, z, "air");
        }
        for (int[] c : new int[][] {{35, 5}, {36, 8}, {37, 6}}) {
            t.set(c[0], y + 4, c[1], C + "tubo[axis=y]");
        }
        for (int x = 30; x <= 42; x++) {
            t.set(x, PB - 2, 2, C + "tubo[axis=x]");
        }
        t.deco(32, y + 2, 1, "reloj", "south");
        t.luz(31, y + 2, 7, 8);
        esc(t, 39, y, 12);
        esc(t, 41, y, 2);
        // el taller (x 30..42, z 16..22): banco de trabajo, una muñeca a medio hacer, cera, herramientas
        t.deco(34, y, 22, "mesa", "north");
        t.deco(35, y, 22, "mesa", "north");
        t.deco(36, y, 22, "mesa", "north");
        t.set(34, y + 1, 22, C + "muneca[facing=north]");
        t.set(35, y + 1, 22, C + "cera");
        t.set(36, y + 1, 22, C + "lampara_mesa");
        t.deco(35, y, 21, "silla", "south");
        for (int x = 31; x <= 33; x++) {
            t.set(x, y + 2, 16, C + "argolla[facing=south]");
        }
        t.set(32, y, 19, C + "sangre");
        t.alto(41, y, 17, "maniqui", "west");
        esc(t, 31, y, 22);
        // la bodega grande abajo del salón (x 6..42, z 24..37): repisas, cajas, muebles tapados y muñecas en cajas
        for (int x = 9; x <= 39; x += 6) {
            for (int z = 25; z <= 36; z++) {
                if (z == 30 || z == 31) {
                    continue;
                }
                for (int yy = y; yy <= y + 2; yy++) {
                    t.set(x, yy, z, (z + yy) % 3 == 0 ? C + "barril[facing=north]" : C + "cajon[facing=east]");
                }
            }
        }
        for (int x = 12; x <= 36; x += 12) {
            for (int yy = y; yy < PB - 1; yy++) {
                t.set(x, yy, 30, C + "viga_madera[axis=y]");
                t.set(x, yy, 31, C + "viga_madera[axis=y]");
            }
        }
        t.alto(7, y, 25, "ropero", "east");
        t.alto(7, y, 36, "ropero", "east");
        t.alto(20, y, 27, "maniqui", "south");
        t.alto(26, y, 34, "armadura", "north");
        t.deco(30, y, 26, "caballito", "south");
        for (int[] m : new int[][] {{11, 28}, {17, 33}, {23, 26}, {29, 35}, {35, 28}, {41, 33}}) {
            t.set(m[0], y, m[1], C + "muneca[facing=north]");
        }
        for (int x = 10; x <= 40; x += 10) {
            t.set(x, PB - 2, 30, C + "farol");
        }
        for (int[] e : new int[][] {{10, 25}, {22, 36}, {34, 25}, {40, 36}, {16, 33}}) {
            esc(t, e[0], y, e[1]);
        }
        // el cuarto de las muñecas rotas (x 6..18, z 39..50): estantes con muñecas, muñecas colgadas, cera y una mesa de operar
        for (int x = X0 + 1; x <= 18; x++) {
            t.set(x, y, Z1 - 1, C + "estante_archivo");
            t.set(x, y + 1, Z1 - 1, C + "muneca[facing=north]");
        }
        for (int[] m : new int[][] {{9, 42}, {12, 44}, {15, 42}, {10, 47}, {14, 47}}) {
            for (int yy = y + 2; yy < PB - 1; yy++) {
                t.set(m[0], yy, m[1], C + "cadena[axis=y]");
            }
            t.set(m[0], y + 1, m[1], C + "muneca[facing=south]");
        }
        t.deco(12, y, 45, "camilla", "north");
        t.set(11, y, 45, C + "cera");
        t.set(13, y, 45, C + "sangre");
        t.set(7, y, 41, C + "craneo");
        t.p.texto(X0 + 1, y + 2, 44, "rayon", "east", 0x8A2020, "TODAS", "TIENEN", "NUMERO");
        t.luz(12, y + 3, 44, 5);
        esc(t, 17, y, 40);
        esc(t, 7, y, 48);
        // la lavandería (x 20..33): tinas, ropa tendida (cortinas en cadenas), charcos
        t.deco(22, y, 41, "tina", "north");
        t.deco(22, y, 42, "tina", "south");
        t.deco(25, y, 41, "tina", "north");
        t.deco(25, y, 42, "tina", "south");
        for (int x = 21; x <= 32; x++) {
            t.set(x, PB - 2, 46, C + "cadena[axis=x]");
        }
        for (int x = 23; x <= 31; x += 4) {
            t.set(x, y + 3, 46, C + "telon_borde");
        }
        t.set(28, y, 44, C + "charco");
        t.set(30, y, 48, C + "barril[facing=north]");
        t.set(31, y, 48, C + "barril[facing=north]");
        t.set(21, PB - 2, 49, C + "farol");
        esc(t, 32, y, 50);
        // la despensa (x 35..42): estantes, barriles, frascos (velas apagadas), costales (cajones)
        for (int x = 35; x <= 42; x++) {
            t.set(x, y, Z1 - 1, C + "estante_archivo");
            t.set(x, y + 1, Z1 - 1, C + "estante_archivo");
        }
        for (int z = 40; z <= 46; z++) {
            if (z == 44 || z == 45) {
                continue;
            }
            t.set(42, y, z, C + "barril[facing=west]");
        }
        t.set(36, y, 41, C + "cajon[facing=south]");
        t.set(37, y, 41, C + "cajon[facing=south]");
        t.set(36, y + 1, 41, C + "cajon[facing=south]");
        t.set(38, PB - 2, 45, C + "farol");
        esc(t, 36, y, 48);
    }
}
