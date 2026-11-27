package com.coleccionista.mapa;

import static com.coleccionista.mapa.Mansion.*;

/**
 * La obra negra de la mansión: muros de afuera (zócalo de sillar negro, sillares, cornisas, esquinas y pilastras), pisos y plafones,
 * muros de adentro con sus puertas, escaleras, techo de pizarra a la francesa (mansarda) con buhardillas y crestería, las dos torres
 * con su chapitel, el pórtico con balcón, las chimeneas y las ventanas (con repisa, guardapolvo y cortinas). Los cuartos se visten
 * después ({@link MansionCuartos}).
 */
final class MansionObra {
    private MansionObra() {
    }

    static void casa(Trazo t) {
        cajon(t);
        murosAfuera(t);
        pisos(t);
        murosAdentro(t);
        puertas(t);
        escaleras(t);
        mansarda(t);
        hastial(t, true);
        hastial(t, false);
        torre(t, false);
        torre(t, true);
        portico(t);
        chimeneas(t);
        ventanas(t);
        forro(t);
    }

    // ------------------------------------------------------------------ el cajón: se vacía todo lo de adentro

    private static void cajon(Trazo t) {
        // lo de adentro, del sótano al ático (el techo lo pone la mansarda)
        t.aire(X0 + 1, SOT + 1, Z0 + 1, X1 - 1, ATI + 6, Z1 - 1);
    }

    // ------------------------------------------------------------------ muros de afuera

    /** Una columna del muro de afuera, de abajo arriba: ladrillo enterrado, zócalo negro, sillares y las cornisas de cada piso. */
    static void columnaMuro(Trazo t, int x, int z, int yTope, boolean esquina) {
        for (int y = SOT; y <= yTope; y++) {
            String b;
            if (y < PB) {
                b = C + "ladrillo_viejo";
            } else if (y <= PB + 1) {
                b = C + "ladrillo_oscuro";
            } else if (y == PA - 1 || y == ATI) {
                b = C + "cornisa";
            } else if (esquina && (y % 2 == 0)) {
                b = C + "ladrillo_oscuro";
            } else {
                b = C + "piedra_capilla";
            }
            t.set(x, y, z, b);
        }
    }

    private static void murosAfuera(Trazo t) {
        for (int x = X0; x <= X1; x++) {
            boolean es = x == X0 || x == X1 || pilastraX(x);
            columnaMuro(t, x, Z0, ATI, es);
            columnaMuro(t, x, Z1, ATI, es);
        }
        for (int z = Z0; z <= Z1; z++) {
            boolean es = z == Z0 || z == Z1 || pilastraZ(z);
            columnaMuro(t, X0, z, ATI, es);
            columnaMuro(t, X1, z, ATI, es);
        }
        // las ménsulas de la cornisa del alero (escalones de sillar al revés, pegados al muro)
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            t.set(x, ATI, Z0 - 1, C + "escalera_sillar[facing=south,half=top]");
            t.set(x, ATI, Z1 + 1, C + "escalera_sillar[facing=north,half=top]");
        }
        for (int z = Z0; z <= Z1; z++) {
            t.set(X0 - 1, ATI, z, C + "escalera_sillar[facing=east,half=top]");
            t.set(X1 + 1, ATI, z, C + "escalera_sillar[facing=west,half=top]");
        }
        // un goterón entre los dos pisos (losa de sillar arriba de la cornisa)
        for (int x = X0 - 1; x <= X1 + 1; x++) {
            t.set(x, PA - 1, Z0 - 1, C + "losa_sillar[type=top]");
            t.set(x, PA - 1, Z1 + 1, C + "losa_sillar[type=top]");
        }
        for (int z = Z0; z <= Z1; z++) {
            t.set(X0 - 1, PA - 1, z, C + "losa_sillar[type=top]");
            t.set(X1 + 1, PA - 1, z, C + "losa_sillar[type=top]");
        }
    }

    /** Pilastras de la fachada (a plomo con los muros de adentro). */
    static boolean pilastraX(int x) {
        return x == 16 || x == 37 || x == 20 || x == 33;
    }

    static boolean pilastraZ(int z) {
        return z == 15 || z == 23 || z == 38;
    }

    // ------------------------------------------------------------------ pisos y plafones

    private static void pisos(Trazo t) {
        for (int x = X0 + 1; x <= X1 - 1; x++) {
            for (int z = Z0 + 1; z <= Z1 - 1; z++) {
                t.set(x, SOT, z, C + "concreto_piso");
                // el techo del sótano: tablones con vigas
                t.set(x, PB - 1, z, z % 4 == 0 ? C + "viga_madera[axis=x]" : C + "tablon");
                t.set(x, PB, z, C + "parquet");
                t.set(x, PA - 1, z, C + "techo_yeso");
                t.set(x, PA, z, C + "parquet");
                t.set(x, ATI - 1, z, C + "techo_yeso");
                t.set(x, ATI, z, C + "tablon");
            }
        }
        // el vestíbulo: de doble altura (solo su galería del norte, z 1..3, tiene piso arriba)
        t.aire(HX0, PA - 1, HZ0 + 3, HX1, PA, HZ1);
        // el salón de baile: de doble altura, con un balcón al oeste (x 6..8)
        t.aire(BX0 + 3, PA - 1, BZ0, BX1, PA, BZ1);
    }

    // ------------------------------------------------------------------ muros de adentro

    /** Una columna de muro de adentro: lambrín de madera abajo, papel tapiz, una moldura oscura entre pisos. */
    static void tabique(Trazo t, int x, int z, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            String b;
            if (y < PB) {
                b = C + "ladrillo_viejo";
            } else if (y == PB + 1 || y == PB + 2 || y == PA + 1) {
                b = C + "panel_madera";
            } else if (y == PA - 1 || y == PA) {
                b = C + "madera_pulida";
            } else {
                b = C + "papel_tapiz";
            }
            t.set(x, y, z, b);
        }
    }

    static void tabiqueX(Trazo t, int x0, int x1, int z, int y0, int y1) {
        for (int x = x0; x <= x1; x++) {
            tabique(t, x, z, y0, y1);
        }
    }

    static void tabiqueZ(Trazo t, int x, int z0, int z1, int y0, int y1) {
        for (int z = z0; z <= z1; z++) {
            tabique(t, x, z, y0, y1);
        }
    }

    private static void murosAdentro(Trazo t) {
        int arriba = ATI - 1;
        // fila A: biblioteca y música | vestíbulo | comedor y cuarto del mayordomo (y arriba: recámara y bebé | estudio y huéspedes)
        tabiqueZ(t, HX0 - 1, Z0 + 1, BZ0 - 2, PB + 1, arriba);
        tabiqueZ(t, HX1 + 1, Z0 + 1, BZ0 - 2, PB + 1, arriba);
        tabiqueX(t, X0 + 1, HX0 - 2, 15, PB + 1, PA - 1);
        tabiqueX(t, HX1 + 2, X1 - 1, 15, PB + 1, PA - 1);
        tabiqueX(t, X0 + 1, HX0 - 2, 13, PA, arriba);
        tabiqueX(t, HX1 + 2, X1 - 1, 13, PA, arriba);
        // entre la fila A y la B (el salón de baile y el pasillo de servicio), de dos pisos
        tabiqueX(t, X0 + 1, X1 - 1, BZ0 - 1, PB + 1, arriba);
        // el salón de baile y el pasillo de servicio
        tabiqueZ(t, SX0 - 1, BZ0, BZ1, PB + 1, arriba);
        // entre la fila B y la C
        tabiqueX(t, X0 + 1, X1 - 1, BZ1 + 1, PB + 1, arriba);
        // la fila C: billar | trofeos | cocina (y arriba: costura | baño | servidumbre)
        tabiqueZ(t, 19, BZ1 + 2, Z1 - 1, PB + 1, arriba);
        tabiqueZ(t, 34, BZ1 + 2, Z1 - 1, PB + 1, arriba);
        // el sótano: muros de ladrillo (pasillo de servicio al este y cuartos)
        for (int z = Z0 + 1; z <= Z1 - 1; z++) {
            t.set(SX0 - 1, SOT + 1, z, C + "ladrillo_viejo");
            for (int y = SOT + 1; y < PB - 1; y++) {
                t.set(SX0 - 1, y, z, C + "ladrillo_viejo");
            }
        }
        for (int x = X0 + 1; x < SX0 - 1; x++) {
            for (int y = SOT + 1; y < PB - 1; y++) {
                t.set(x, y, BZ0 - 1, C + "ladrillo_viejo");
                t.set(x, y, BZ1 + 1, C + "ladrillo_viejo");
            }
        }
        for (int y = SOT + 1; y < PB - 1; y++) {
            for (int z = Z0 + 1; z < BZ0 - 1; z++) {
                t.set(21, y, z, C + "ladrillo_viejo");
                t.set(29, y, z, C + "ladrillo_viejo");
            }
            for (int x = 30; x < SX0 - 1; x++) {
                t.set(x, y, 15, C + "ladrillo_viejo");
            }
            for (int z = BZ1 + 2; z <= Z1 - 1; z++) {
                t.set(19, y, z, C + "ladrillo_viejo");
                t.set(34, y, z, C + "ladrillo_viejo");
            }
        }
    }

    // ------------------------------------------------------------------ puertas

    /** Un hueco de puerta de dos de ancho y dos de alto (sin hoja) en un muro, con su dintel de madera. */
    static void arco(Trazo t, int x, int y, int z, boolean correX) {
        for (int i = 0; i < 2; i++) {
            int xx = correX ? x + i : x, zz = correX ? z : z + i;
            t.set(xx, y, zz, "air");
            t.set(xx, y + 1, zz, "air");
            t.set(xx, y + 2, zz, C + "madera_pulida");
        }
    }

    private static void puertas(Trazo t) {
        int b = PB + 1, a = PA + 1, s = SOT + 1;
        // ---- planta baja
        t.puertaDoble(26, b, Z0, "dark_oak", "south");                       // la de enfrente
        for (int i = 0; i < 2; i++) {
            t.set(26 + i, b + 2, Z0, C + "vitral");                             // montante de vitral
        }
        t.puertaDoble(HX0 - 1, b, 12, "dark_oak", "west");                   // vestíbulo -> biblioteca
        t.puertaDoble(HX0 - 1, b, 19, "dark_oak", "west");                   // vestíbulo -> música
        t.puertaDoble(HX1 + 1, b, 12, "dark_oak", "east");                   // vestíbulo -> comedor
        t.puertaDoble(HX1 + 1, b, 19, "dark_oak", "east");                   // vestíbulo -> mayordomo
        t.puertaDoble(18, b, BZ0 - 1, "dark_oak", "south");                  // al salón de baile (a los lados de la jaula)
        t.puertaDoble(34, b, BZ0 - 1, "dark_oak", "south");
        t.puerta(10, b, 15, "dark_oak", "south", "left");                    // biblioteca <-> música
        t.puerta(43, b, 15, "dark_oak", "south", "right");                   // comedor <-> mayordomo
        t.puertaDoble(45, b, BZ0 - 1, "dark_oak", "south");                  // mayordomo -> pasillo de servicio (y escalera al sótano)
        t.puertaDoble(SX0 - 1, b, 30, "dark_oak", "east");                   // salón -> pasillo de servicio
        t.puertaDoble(10, b, BZ1 + 1, "dark_oak", "south");                  // salón -> billar
        t.puertaDoble(26, b, BZ1 + 1, "dark_oak", "south");                  // salón -> trofeos
        t.puertaDoble(45, b, BZ1 + 1, "dark_oak", "south");                  // pasillo -> cocina
        t.puertaDoble(19, b, 44, "dark_oak", "east");                        // billar <-> trofeos
        t.puertaDoble(34, b, 44, "dark_oak", "east");                        // trofeos <-> cocina
        t.puertaDoble(26, b, Z1, "dark_oak", "south");                       // trofeos -> jardín de atrás
        // ---- planta alta
        t.puertaDoble(HX0 - 1, a, 1, "dark_oak", "west");                    // galería -> recámara
        t.puertaDoble(HX1 + 1, a, 1, "dark_oak", "east");                    // galería -> estudio
        t.puertaDoble(26, a, Z0, "dark_oak", "north");                       // galería -> balcón del pórtico
        t.puerta(10, a, 13, "dark_oak", "south", "left");                    // recámara <-> bebé
        t.puerta(43, a, 13, "dark_oak", "south", "right");                   // estudio <-> huéspedes
        t.puertaDoble(7, a, BZ0 - 1, "dark_oak", "south");                   // bebé -> balcón del salón
        t.puertaDoble(45, a, BZ0 - 1, "dark_oak", "south");                  // huéspedes -> pasillo de servicio
        t.puertaDoble(7, a, BZ1 + 1, "dark_oak", "south");                   // balcón -> costura
        t.puertaDoble(45, a, BZ1 + 1, "dark_oak", "south");                  // pasillo -> servidumbre
        t.puertaDoble(19, a, 44, "dark_oak", "east");                        // costura <-> baño
        t.puertaDoble(34, a, 44, "dark_oak", "east");                        // baño <-> servidumbre
        // ---- sótano (arcos sin hoja)
        arco(t, SX0 - 1, s, 4, false);                                       // bodega de carbón
        arco(t, SX0 - 1, s, 19, false);                                      // taller
        arco(t, SX0 - 1, s, 9, false);                                       // calderas
        arco(t, SX0 - 1, s, 30, false);                                      // bodega grande (abajo del salón)
        arco(t, SX0 - 1, s, 44, false);                                      // despensa
        arco(t, 21, s, 8, false);
        arco(t, 29, s, 8, false);
        arco(t, 12, s, BZ0 - 1, true);                                       // vinos -> bodega grande
        arco(t, 12, s, BZ1 + 1, true);                                       // bodega grande -> muñecas
        arco(t, 26, s, BZ1 + 1, true);                                       // bodega grande -> lavandería
        arco(t, 19, s, 44, false);
        arco(t, 34, s, 44, false);
    }

    // ------------------------------------------------------------------ escaleras

    private static void escaleras(Trazo t) {
        String m = C + "escalera_madera";
        // ---- la escalera imperial del vestíbulo: dos tramos pegados a los muros, suben al norte hasta la galería (piso PA)
        for (int k = 0; k < PA - PB; k++) {
            int y = PB + 1 + k, z = 11 - k;
            for (int x = HX0; x <= HX0 + 2; x++) {
                t.par(x, y, z, m + "[facing=north,half=bottom]");
                for (int yy = PB + 1; yy < y; yy++) {
                    if (z >= 7) {
                        t.par(x, yy, z, C + "madera_pulida");
                    }
                }
            }
            // la zanca (un murito de madera) y el pasamanos del lado del vestíbulo
            for (int yy = PB + 1; yy <= y; yy++) {
                t.par(HX0 + 3, yy, z, C + "madera_pulida");
            }
            t.par(HX0 + 3, y + 1, z, C + "balaustrada");
        }
        // el clóset de abajo de la escalera (z 4..6): se entra por la galería de abajo (z 3)
        // ---- la galería del norte (piso PA, z 1..3) con su balaustrada
        for (int x = HX0 + 3; x <= HX1 - 3; x++) {
            t.set(x, PA + 1, HZ0 + 2, C + "balaustrada");
        }
        // ---- del cuarto del mayordomo al sótano (x 46..47, baja al sur: z 24 arriba .. 30 abajo)
        for (int k = 0; k < PB - SOT; k++) {
            int y = PB - k, z = BZ0 + k;
            for (int x = 46; x <= 47; x++) {
                t.set(x, y, z, m + "[facing=north,half=bottom]");
                for (int yy = y + 1; yy <= y + 3 && yy <= PB; yy++) {
                    t.set(x, yy, z, "air");
                }
                for (int yy = SOT + 1; yy < y; yy++) {
                    t.set(x, yy, z, C + "ladrillo_viejo");
                }
            }
        }
        // ---- del pasillo de servicio a la planta alta (x 46..47, sube al norte: z 37 abajo .. 30 arriba)
        for (int k = 0; k < PA - PB; k++) {
            int y = PB + 1 + k, z = BZ1 - k;
            for (int x = 46; x <= 47; x++) {
                t.set(x, y, z, m + "[facing=north,half=bottom]");
                for (int yy = y + 1; yy <= y + 3; yy++) {
                    t.set(x, yy, z, "air");
                }
            }
        }
        for (int x = 46; x <= 47; x++) {
            t.set(x, PA + 1, 34, C + "balaustrada");
        }
        // ---- de la servidumbre al ático (x 46..47, sube al norte: z 48 abajo .. 42 arriba)
        for (int k = 0; k < ATI - PA; k++) {
            int y = PA + 1 + k, z = Z1 - 3 - k;
            for (int x = 46; x <= 47; x++) {
                t.set(x, y, z, m + "[facing=north,half=bottom]");
                for (int yy = y + 1; yy <= y + 3; yy++) {
                    t.set(x, yy, z, "air");
                }
            }
        }
        for (int z = 43; z <= 45; z++) {
            t.set(45, ATI + 1, z, C + "balaustrada");
        }
        for (int z = 45; z <= 48; z++) {
            t.set(45, PA + 1, z, C + "balaustrada");
        }
        for (int x = 46; x <= 47; x++) {
            t.set(x, ATI + 1, 46, C + "balaustrada");
        }
    }

    // ------------------------------------------------------------------ el techo a la francesa

    /** Cuánto se mete la mansarda en cada altura (y ATI+1 .. ATI+5); arriba queda la azotea plana. */
    static final int[] METE = {0, 0, 0, 1, 1, 2, 2};

    private static void mansarda(Trazo t) {
        for (int l = 0; l < METE.length; l++) {
            int y = ATI + 1 + l, i = METE[l], sig = l + 1 < METE.length ? METE[l + 1] : 3;
            boolean escalon = sig > i;
            for (int x = X0 + i; x <= X1 - i; x++) {
                t.set(x, y, Z0 + i, escalon ? C + "escalera_pizarra[facing=south,half=bottom]" : C + "pizarra");
                t.set(x, y, Z1 - i, escalon ? C + "escalera_pizarra[facing=north,half=bottom]" : C + "pizarra");
            }
            for (int z = Z0 + i + 1; z <= Z1 - i - 1; z++) {
                t.set(X0 + i, y, z, escalon ? C + "escalera_pizarra[facing=east,half=bottom]" : C + "pizarra");
                t.set(X1 - i, y, z, escalon ? C + "escalera_pizarra[facing=west,half=bottom]" : C + "pizarra");
            }
        }
        // la azotea (plana) y su crestería de hierro, con remates en las esquinas y cada 6
        int top = ATI + METE.length + 1, m = 3;
        for (int x = X0 + m; x <= X1 - m; x++) {
            for (int z = Z0 + m; z <= Z1 - m; z++) {
                t.set(x, top, z, C + "pizarra");
            }
        }
        for (int x = X0 + m; x <= X1 - m; x++) {
            for (int z : new int[] {Z0 + m, Z1 - m}) {
                boolean remate = x == X0 + m || x == X1 - m || (x - X0 - m) % 6 == 0;
                t.set(x, top + 1, z, remate ? C + "poste[axis=y]" : C + "barrotes");
                if (remate) {
                    t.set(x, top + 2, z, C + "losa_sillar[type=bottom]");
                }
            }
        }
        for (int z = Z0 + m + 1; z <= Z1 - m - 1; z++) {
            for (int x : new int[] {X0 + m, X1 - m}) {
                boolean remate = (z - Z0 - m) % 6 == 0;
                t.set(x, top + 1, z, remate ? C + "poste[axis=y]" : C + "barrotes");
                if (remate) {
                    t.set(x, top + 2, z, C + "losa_sillar[type=bottom]");
                }
            }
        }
        // buhardillas: una ventana en la parte recta de la mansarda con su techito de dos aguas
        for (int x : new int[] {14, 38, 22, 30}) {
            buhardilla(t, x, Z0, -1, true);
        }
        for (int x : new int[] {9, 22, 30, 43}) {
            buhardilla(t, x, Z1, 1, true);
        }
        for (int z : new int[] {6, 18, 30, 43}) {
            buhardilla(t, z, X0, -1, false);
            buhardilla(t, z, X1, 1, false);
        }
    }

    /**
     * El hastial (frontón de dos aguas muy parado) arriba del centro de la fachada ({@code frente}) o de atrás: sale de la mansarda sobre los
     * muros del vestíbulo (x 16..37), con un rosetón de vitral y dos lancetas; arriba de la azotea sigue su techo hasta z 16 (o 35 atrás).
     * Por dentro es parte del ático (un desván alto).
     */
    private static void hastial(Trazo t, boolean frente) {
        int muro = frente ? Z0 : Z1, s = frente ? 1 : -1, fondo = frente ? 16 : Z1 - 16;
        int za = muro - s, zf = fondo;
        for (int i = 0; i <= 11; i++) {
            int y = ATI + 1 + i, xl = 15 + i, xr = 38 - i;
            for (int z = Math.min(za, zf); z <= Math.max(za, zf); z++) {
                boolean frenteMansarda = frente ? z <= Z0 + 2 : z >= Z1 - 2;
                if (!frenteMansarda && y < AZOTEA) {
                    continue;
                }
                // lo de adentro: aire (es ático)
                for (int x = xl + 1; x <= xr - 1; x++) {
                    boolean pared = z == muro || z == fondo;
                    if (z == za) {
                        continue;
                    }
                    t.set(x, y, z, pared ? C + "piedra_capilla" : "air");
                }
                t.set(xl, y, z, C + "escalera_pizarra[facing=east,half=bottom]");
                t.set(xr, y, z, C + "escalera_pizarra[facing=west,half=bottom]");
            }
        }
        // la cornisa del frontón (abajo) y el remate de arriba
        for (int x = 15; x <= 38; x++) {
            t.set(x, ATI, za, C + "escalera_sillar[facing=" + (frente ? "south" : "north") + ",half=top]");
        }
        t.set(26, ATI + 13, za, C + "poste[axis=y]");
        t.set(27, ATI + 13, za, C + "poste[axis=y]");
        t.set(26, ATI + 14, za, C + "losa_sillar[type=bottom]");
        t.set(27, ATI + 14, za, C + "losa_sillar[type=bottom]");
        // el rosetón (círculo de vitral con marco de cantera) y dos lancetas abajo
        double cy = ATI + 7.0;
        for (int x = 21; x <= 32; x++) {
            for (int y = ATI + 2; y <= ATI + 11; y++) {
                double d = Math.hypot(x + 0.5 - 27.0, y + 0.5 - cy);
                if (d < 2.6) {
                    t.set(x, y, muro, C + "vitral");
                } else if (d < 3.4) {
                    t.set(x, y, muro, C + "cornisa");
                }
            }
        }
        for (int x : new int[] {21, 31}) {
            for (int y = ATI + 1; y <= ATI + 3; y++) {
                t.set(x, y, muro, C + "vitral");
                t.set(x + 1, y, muro, C + "vitral");
            }
            t.set(x, ATI + 4, muro - s, C + "escalera_sillar[facing=" + (frente ? "south" : "north") + ",half=top]");
            t.set(x + 1, ATI + 4, muro - s, C + "escalera_sillar[facing=" + (frente ? "south" : "north") + ",half=top]");
        }
    }

    /** Una buhardilla de dos de ancho (a, a+1) en el muro de la mansarda (fijo), que sale hacia {@code sal}. */
    private static void buhardilla(Trazo t, int a, int fijo, int sal, boolean correX) {
        int y = ATI + 1;
        String izq = correX ? "east" : "south", der = correX ? "west" : "north";
        for (int i = -1; i <= 2; i++) {
            for (int s = 0; s <= 1; s++) {
                int f = fijo + sal * s;
                int x = correX ? a + i : f, z = correX ? f : a + i;
                boolean lado = i == -1 || i == 2;
                for (int yy = y; yy <= y + 1; yy++) {
                    if (lado) {
                        t.set(x, yy, z, C + "piedra_capilla");
                    } else if (s == 0) {
                        t.set(x, yy, z, C + "vidrio_sucio");
                    }
                }
                // el techito
                t.set(x, y + 2, z, C + "escalera_pizarra[facing=" + (i <= 0 ? izq : der) + ",half=bottom]");
                if (i == 0 || i == 1) {
                    t.set(x, y + 3, z, C + "escalera_pizarra[facing=" + (i == 0 ? izq : der) + ",half=bottom]");
                }
            }
        }
        // la repisa de la ventana
        for (int i = 0; i <= 1; i++) {
            int x = correX ? a + i : fijo + sal, z = correX ? fijo + sal : a + i;
            t.set(x, y - 1, z, C + "losa_sillar[type=top]");
            t.set(x, y, z, "air");
            t.set(x, y + 1, z, "air");
        }
    }

    // ------------------------------------------------------------------ las torres de las esquinas del frente

    /** La torre del noroeste (x 5..12, z -5..2) o su espejo (x 41..48). Pisos en PB, PA, ATI y TORRE; chapitel de pizarra arriba. */
    private static void torre(Trazo t, boolean este) {
        int ax = este ? t.e(TX1) : TX0, bx = este ? t.e(TX0) : TX1;
        int za = TZ0, zb = TZ1, tope = TORRE + 5;
        // muros (de abajo hasta arriba) y lo de adentro
        for (int x = ax; x <= bx; x++) {
            for (int z = za; z <= zb; z++) {
                boolean muro = x == ax || x == bx || z == za || z == zb;
                boolean dentroCasa = z >= Z0;
                for (int y = SOT; y <= tope; y++) {
                    if (muro) {
                        if (dentroCasa && z > Z0 && y <= ATI) {
                            continue; // ahí van los muros de la casa
                        }
                        if (z == zb && y <= ATI) {
                            continue;
                        }
                        boolean esq = (x == ax || x == bx) && (z == za || z == zb);
                        String b = y < PB ? C + "ladrillo_viejo" : y <= PB + 1 ? C + "ladrillo_oscuro"
                                : y == PA - 1 || y == ATI || y == TORRE ? C + "cornisa" : esq && y % 2 == 0 ? C + "ladrillo_oscuro" : C + "piedra_capilla";
                        t.set(x, y, z, b);
                    } else if (y < PB && !dentroCasa) {
                        t.set(x, y, z, C + "tierra");
                    } else if (y > SOT && !(dentroCasa && y < PB)) {
                        String b = y == PB || y == PA ? C + "parquet" : y == PA - 1 || y == ATI - 1 || y == TORRE - 1 ? C + "techo_yeso"
                                : y == ATI || y == TORRE ? C + "tablon" : "air";
                        t.set(x, y, z, b);
                    }
                }
            }
        }
        // arriba de la casa la torre tiene su muro del sur (z 2) con una puerta al ático
        for (int x = ax; x <= bx; x++) {
            for (int y = ATI + 1; y <= tope; y++) {
                t.set(x, y, zb, y == TORRE ? C + "cornisa" : C + "piedra_capilla");
            }
        }
        // la puerta al ático (z 2, al centro)
        for (int x0 : new int[] {8, 9}) {
            int x = este ? t.e(x0) : x0;
            t.set(x, ATI + 1, zb, "air");
            t.set(x, ATI + 2, zb, "air");
        }
        // la escalera de caracol del cuarto del ático al mirador de arriba: pegada al muro de adentro (x bx-1, o ax+1 en la del este),
        // sube al norte y da la vuelta por el muro del norte; el último escalón queda al ras del piso del mirador
        int sx = este ? ax + 1 : bx - 1, ox = este ? 1 : -1;
        java.util.List<int[]> camino = new java.util.ArrayList<>();
        for (int z = zb - 1; z >= za + 1; z--) {
            camino.add(new int[] {sx, z});
        }
        for (int i = 1; camino.size() < TORRE - ATI; i++) {
            camino.add(new int[] {sx + ox * i, za + 1});
        }
        java.util.Set<Long> enCamino = new java.util.HashSet<>();
        for (int k = 0; k < camino.size(); k++) {
            int[] c = camino.get(k);
            int y = ATI + 1 + k;
            boolean vuelta = c[1] == za + 1 && c[0] != sx;
            String f = vuelta ? (este ? "east" : "west") : "north";
            t.set(c[0], y, c[1], C + "escalera_madera[facing=" + f + ",half=bottom]");
            for (int yy = y + 1; yy <= y + 3; yy++) {
                t.set(c[0], yy, c[1], "air");
            }
            for (int yy = ATI + 1; yy < y; yy++) {
                t.set(c[0], yy, c[1], C + "madera_pulida");
            }
            enCamino.add(Plano.key(c[0], 0, c[1]));
        }
        // el barandal del mirador alrededor del hueco (donde se quitó el piso para pasar la cabeza)
        for (int[] c : camino) {
            if (t.p.vacio(c[0], TORRE, c[1])) {
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = c[0] + d[0], nz = c[1] + d[1];
                    if (nx > ax && nx < bx && nz > za && nz < zb && !enCamino.contains(Plano.key(nx, 0, nz))
                            && !t.p.vacio(nx, TORRE, nz)) {
                        t.set(nx, TORRE + 1, nz, C + "balaustrada");
                    }
                }
            }
        }
        // ventanas: angostas en los pisos de la casa, grandes en el mirador
        int fx = este ? bx : ax, cx = este ? t.e(8) : 8;
        for (int[] v : new int[][] {{PB + 2, PB + 5}, {PA + 2, PA + 5}}) {
            ventana(t, true, za, -1, cx, cx + (este ? -1 : 1), v[0], v[1], C + "vidrio_sucio", false);
            ventana(t, false, fx, este ? 1 : -1, -3, -2, v[0], v[1], C + "vidrio_sucio", false);
        }
        ventana(t, true, za, -1, cx, cx, ATI + 2, ATI + 4, C + "vitral", false);
        ventana(t, false, fx, este ? 1 : -1, -2, -2, ATI + 2, ATI + 4, C + "vitral", false);
        for (int[] w : new int[][] {{1, za, -1}, {1, zb, 1}, {0, ax, -1}, {0, bx, 1}}) {
            boolean corre = w[0] == 1;
            if (este && corre && w[1] == za) {
                continue; // ahí va el reloj
            }
            if (corre) {
                ventana(t, true, w[1], w[2], ax + 3, ax + 4, TORRE + 1, TORRE + 4, C + "vidrio_sucio", false);
            } else {
                ventana(t, false, w[1], w[2], za + 3, za + 4, TORRE + 1, TORRE + 4, C + "vidrio_sucio", false);
            }
        }
        // el alero y el chapitel (pirámide muy parada) con su pararrayos
        int y = tope + 1;
        for (int x = ax - 1; x <= bx + 1; x++) {
            t.set(x, y - 1, za - 1, C + "escalera_sillar[facing=south,half=top]");
            t.set(x, y - 1, zb + 1, C + "escalera_sillar[facing=north,half=top]");
        }
        for (int z = za; z <= zb; z++) {
            t.set(ax - 1, y - 1, z, C + "escalera_sillar[facing=east,half=top]");
            t.set(bx + 1, y - 1, z, C + "escalera_sillar[facing=west,half=top]");
        }
        for (int i = 0; i <= 3; i++) {
            int x0 = ax - 1 + i, x1 = bx + 1 - i, z0 = za - 1 + i, z1 = zb + 1 - i;
            for (int r = 0; r < (i == 0 ? 1 : 2); r++) {
                boolean escalon = r == 1 || i == 0;
                for (int x = x0; x <= x1; x++) {
                    for (int z = z0; z <= z1; z++) {
                        boolean orilla = x == x0 || x == x1 || z == z0 || z == z1;
                        if (!orilla) {
                            t.set(x, y, z, C + "tablon");
                            continue;
                        }
                        String f = z == z0 ? "south" : z == z1 ? "north" : x == x0 ? "east" : "west";
                        t.set(x, y, z, escalon ? C + "escalera_pizarra[facing=" + f + ",half=bottom]" : C + "pizarra");
                    }
                }
                y++;
            }
        }
        // lo de arriba (2 x 2): pizarra, una losa y el pararrayos de hierro
        int mx = ax + 3, mz = za + 3;
        for (int x = mx; x <= mx + 1; x++) {
            for (int z = mz; z <= mz + 1; z++) {
                t.set(x, y, z, C + "pizarra");
                t.set(x, y + 1, z, C + "escalera_pizarra[facing=" + (z == mz ? "south" : "north") + ",half=bottom]");
            }
        }
        t.set(mx, y + 2, mz, C + "poste[axis=y]");
        t.set(mx, y + 3, mz, C + "poste[axis=y]");
        t.set(mx, y + 4, mz, C + "tubo[axis=y]");
        t.set(mx, y + 5, mz, C + "barrotes");
        if (este) {
            // el reloj de la torre del noreste (en su cara del norte, a la altura del mirador)
            int rx = mx;
            for (int dx = 0; dx <= 2; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    t.set(rx + dx, TORRE + 1 + dy, za, dx == 1 && dy == 1 ? C + "marmol_crema" : C + "marco_dorado");
                }
            }
            t.set(rx + 1, TORRE + 2, za - 1, C + "reloj[facing=north]");
            // (las ventanas del mirador del norte se quitan donde va el reloj)
        }
    }

    // ------------------------------------------------------------------ el pórtico con su balcón

    private static void portico(Trazo t) {
        int x0 = PX0, x1 = PX1, z0 = PZ0, z1 = Z0 - 1;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = SOT; y < PB; y++) {
                    t.set(x, y, z, C + "tierra");
                }
                t.set(x, PB, z, ((x + z) & 1) == 0 ? C + "marmol_negro" : C + "marmol_crema");
                for (int y = PB + 1; y < PA - 1; y++) {
                    t.set(x, y, z, "air");
                }
                t.set(x, PA - 1, z, C + "techo_yeso");
                t.set(x, PA, z, C + "losa_piedra");
            }
        }
        // el escalón de la entrada (todo el frente, menos las esquinas de las columnas)
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            t.set(x, PB, z0 - 1, C + "escalera_sillar[facing=north,half=bottom]");
        }
        // columnas al frente (y medias columnas pegadas al muro), con su capitel de cornisa
        for (int x : new int[] {x0, x0 + 3, x1 - 3, x1}) {
            for (int y = PB + 1; y < PA - 1; y++) {
                t.set(x, y, z0, y == PA - 2 ? C + "cornisa" : y == PB + 1 ? C + "ladrillo_oscuro" : C + "viga_madera[axis=y]");
            }
            t.set(x, PB + 1, z1, C + "ladrillo_oscuro");
        }
        // los arcos entre columnas (escalones al revés arriba, con la espalda contra cada columna)
        int[] col = {x0, x0 + 3, x1 - 3, x1};
        for (int i = 0; i + 1 < col.length; i++) {
            t.set(col[i] + 1, PA - 2, z0, C + "escalera_sillar[facing=west,half=top]");
            t.set(col[i + 1] - 1, PA - 2, z0, C + "escalera_sillar[facing=east,half=top]");
        }
        for (int x = x0; x <= x1; x++) {
            t.set(x, PA - 1, z0, C + "cornisa");
        }
        for (int z = z0; z <= z1; z++) {
            t.set(x0, PA - 1, z, C + "cornisa");
            t.set(x1, PA - 1, z, C + "cornisa");
        }
        // el balcón de arriba: balaustrada alrededor
        for (int x = x0; x <= x1; x++) {
            t.set(x, PA + 1, z0, C + "balaustrada");
        }
        for (int z = z0; z <= z1; z++) {
            t.set(x0, PA + 1, z, C + "balaustrada");
            t.set(x1, PA + 1, z, C + "balaustrada");
        }
        for (int x : new int[] {x0, x1}) {
            t.set(x, PA + 1, z0, C + "poste[axis=y]");
            t.set(x, PA + 2, z0, C + "vela");
        }
        // faroles colgados del techo del pórtico
        for (int x : new int[] {x0 + 2, x1 - 2}) {
            t.set(x, PA - 2, z0 + 2, C + "cadena[axis=y]");
            t.set(x, PA - 3, z0 + 2, C + "farol");
        }
        t.set(26, PA - 2, z0 + 2, C + "farol");
        t.set(27, PA - 2, z0 + 2, C + "farol");
    }

    // ------------------------------------------------------------------ chimeneas

    private static void chimeneas(Trazo t) {
        // las de los lados: pecho afuera del muro (x 3..4 y su espejo), del zócalo hasta arriba del techo
        for (boolean este : new boolean[] {false, true}) {
            for (int x = 3; x <= 4; x++) {
                int xx = este ? t.e(x) : x;
                for (int z = 9; z <= 12; z++) {
                    for (int y = PB; y <= AZOTEA + 3; y++) {
                        boolean angosta = y > ATI + 1 && (z == 9 || z == 12);
                        if (angosta) {
                            continue;
                        }
                        t.set(xx, y, z, y <= PB + 1 ? C + "ladrillo_oscuro" : y == AZOTEA + 3 ? C + "cornisa" : C + "ladrillo_viejo");
                    }
                }
                for (int z = 10; z <= 11; z++) {
                    t.set(xx, AZOTEA + 4, z, C + "tubo[axis=y]");
                }
            }
            // donde la chimenea se angosta: un remate
            for (int z : new int[] {9, 12}) {
                int xx = este ? t.e(3) : 3, x2 = este ? t.e(4) : 4;
                t.set(xx, ATI + 1, z, C + "escalera_sillar[facing=" + (z == 9 ? "south" : "north") + ",half=bottom]");
                t.set(x2, ATI + 1, z, C + "escalera_sillar[facing=" + (z == 9 ? "south" : "north") + ",half=bottom]");
            }
            // los hogares: en la planta baja y en la alta
            for (int yb : new int[] {PB, PA}) {
                int wx = este ? X1 : X0, ix = este ? X1 - 1 : X0 + 1, fx = este ? t.e(4) : 4;
                String hacia = este ? "west" : "east";
                for (int z = 9; z <= 12; z++) {
                    for (int y = yb + 1; y <= yb + 4; y++) {
                        t.set(wx, y, z, C + "marmol_negro");
                    }
                }
                for (int z = 10; z <= 11; z++) {
                    t.set(wx, yb + 1, z, C + "brasas");
                    t.set(wx, yb + 2, z, "air");
                    t.set(fx, yb + 1, z, C + "ladrillo_oscuro");
                    t.set(ix, yb + 1, z, C + "barrotes");
                    t.set(wx, yb, z, C + "ladrillo_oscuro");
                }
                // la repisa del hogar, con candelabros
                for (int z = 9; z <= 12; z++) {
                    t.set(ix, yb + 3, z, C + "losa_marmol[type=top]");
                    t.set(ix, yb, z, C + "marmol_negro");
                }
                t.set(ix, yb + 4, 9, C + "candelabro[facing=" + hacia + ",half=lower]");
                t.set(ix, yb + 5, 9, C + "candelabro[facing=" + hacia + ",half=upper]");
                t.set(ix, yb + 4, 12, C + "candelabro[facing=" + hacia + ",half=lower]");
                t.set(ix, yb + 5, 12, C + "candelabro[facing=" + hacia + ",half=upper]");
            }
        }
        // la de la cocina (muro del sur, x 40..41)
        for (int x = 39; x <= 42; x++) {
            for (int z = Z1 + 1; z <= Z1 + 2; z++) {
                for (int y = PB; y <= AZOTEA + 3; y++) {
                    if (y > ATI + 1 && (x == 39 || x == 42)) {
                        continue;
                    }
                    t.set(x, y, z, y <= PB + 1 ? C + "ladrillo_oscuro" : y == AZOTEA + 3 ? C + "cornisa" : C + "ladrillo_viejo");
                }
            }
        }
        for (int x = 40; x <= 41; x++) {
            t.set(x, AZOTEA + 4, Z1 + 1, C + "tubo[axis=y]");
        }
    }

    // ------------------------------------------------------------------ ventanas

    /**
     * Una ventana en un muro: {@code correX} si el muro corre en x (z = fijo) o en z (x = fijo); {@code sal}: hacia dónde es afuera (+1/-1).
     * Va de a0 a a1 a lo largo del muro y de y0 a y1. Afuera: repisa abajo y guardapolvo arriba; adentro, si se pide, cortinas a los lados.
     */
    static void ventana(Trazo t, boolean correX, int fijo, int sal, int a0, int a1, int y0, int y1, String vidrio, boolean cortinas) {
        int lo = Math.min(a0, a1), hi = Math.max(a0, a1);
        String haciaMuro = correX ? (sal < 0 ? "south" : "north") : (sal < 0 ? "east" : "west");
        String adentro = haciaMuro;
        for (int a = lo; a <= hi; a++) {
            for (int y = y0; y <= y1; y++) {
                set(t, correX, a, y, fijo, vidrio);
            }
            // la repisa: afuera solo arriba de la cabeza de quien pasa (en la planta baja va metida en el muro)
            if (y0 - 1 > PB + 2) {
                set(t, correX, a, y0 - 1, fijo + sal, C + "losa_sillar[type=top]");
            } else {
                set(t, correX, a, y0 - 1, fijo, C + "cornisa");
            }
        }
        for (int a = lo - 1; a <= hi + 1; a++) {
            set(t, correX, a, y1 + 1, fijo + sal, C + "escalera_sillar[facing=" + haciaMuro + ",half=top]");
        }
        if (cortinas) {
            for (int a : new int[] {lo - 1, hi + 1}) {
                int x = correX ? a : fijo - sal, z = correX ? fijo - sal : a;
                if (t.p.vacio(x, y1 - 1, z) && t.p.vacio(x, y1, z)) {
                    set(t, correX, a, y1 - 1, fijo - sal, C + "cortina[facing=" + adentro + ",half=lower]");
                    set(t, correX, a, y1, fijo - sal, C + "cortina[facing=" + adentro + ",half=upper]");
                }
            }
        }
    }

    /**
     * El forro de los muros de afuera por dentro (en la planta baja y la alta): lambrín y papel tapiz pegados al sillar, con las ventanas
     * metidas (el hueco de cada ventana queda libre: su repisa por dentro). No se forra donde hay chimenea, puerta, escalera o algo puesto.
     */
    private static void forro(Trazo t) {
        for (int[] nivel : new int[][] {{PB, PA - 2}, {PA, ATI - 2}}) {
            for (int y = nivel[0] + 1; y <= nivel[1]; y++) {
                for (int x = X0 + 1; x <= X1 - 1; x++) {
                    if (nivel[0] == PB || x < HX0 - 1 || x > HX1 + 1) {
                        forrar(t, x, y, Z0 + 1, x, Z0, nivel[0]);
                    }
                    forrar(t, x, y, Z1 - 1, x, Z1, nivel[0]);
                }
                for (int z = Z0 + 1; z <= Z1 - 1; z++) {
                    if (z >= 8 && z <= 13) {
                        continue;
                    }
                    forrar(t, X0 + 1, y, z, X0, z, nivel[0]);
                    if (z < BZ0 || z > BZ1) {
                        forrar(t, X1 - 1, y, z, X1, z, nivel[0]);
                    }
                }
            }
        }
    }

    private static void forrar(Trazo t, int x, int y, int z, int mx, int mz, int piso) {
        String muro = t.p.get(mx, y, mz), aqui = t.p.get(x, y, z);
        if (aqui != null && !aqui.equals("air") || muro == null) {
            return;
        }
        boolean sillar = muro.contains("piedra_capilla") || muro.contains("cornisa") || muro.contains("ladrillo_oscuro");
        if (sillar) {
            t.set(x, y, z, y <= piso + 2 ? C + "panel_madera" : y == piso + 3 ? C + "madera_pulida" : C + "papel_tapiz");
        } else if (muro.contains("vidrio") || muro.contains("vitral")) {
            // la ventana metida: abajo del hueco, una repisa de madera por dentro
            String abajo = t.p.get(x, y - 1, z);
            if (abajo != null && abajo.contains("papel") || abajo != null && abajo.contains("panel") || abajo != null && abajo.contains("madera_pulida")) {
                t.set(x, y - 1, z, C + "losa_madera[type=top]");
            }
        }
    }

    private static void set(Trazo t, boolean correX, int a, int y, int f, String b) {
        if (correX) {
            t.set(a, y, f, b);
        } else {
            t.set(f, y, a, b);
        }
    }

    private static void ventanas(Trazo t) {
        String v = C + "vidrio_sucio", vt = C + "vitral";
        int[] pb = {PB + 2, PB + 5}, pa = {PA + 2, PA + 5};
        // ---- el frente (z 0)
        for (int[] y : new int[][] {pb, pa}) {
            ventana(t, true, Z0, -1, 14, 15, y[0], y[1], v, true);
            ventana(t, true, Z0, -1, t.e(15), t.e(14), y[0], y[1], v, true);
        }
        ventana(t, true, Z0, -1, 22, 23, PB + 2, PB + 5, v, false);
        ventana(t, true, Z0, -1, 30, 31, PB + 2, PB + 5, v, false);
        // arriba del pórtico: dos lancetas altas de vitral y el vitral arriba de la puerta del balcón
        ventana(t, true, Z0, -1, 22, 24, PA + 1, ATI - 1, vt, false);
        ventana(t, true, Z0, -1, 29, 31, PA + 1, ATI - 1, vt, false);
        for (int x = 26; x <= 27; x++) {
            for (int y = PA + 3; y <= ATI - 1; y++) {
                t.set(x, y, Z0, vt);
            }
        }
        // ---- atrás (z 51)
        for (int[] y : new int[][] {pb, pa}) {
            for (int x : new int[] {9, 14, 22, 30, 36, 45}) {
                ventana(t, true, Z1, 1, x, x + 1, y[0], y[1], v, true);
            }
        }
        ventana(t, true, Z1, 1, 26, 27, PA + 2, PA + 5, v, true);
        // ---- los lados (x 5 y x 48)
        for (boolean este : new boolean[] {false, true}) {
            int f = este ? X1 : X0, s = este ? 1 : -1;
            for (int[] y : new int[][] {pb, pa}) {
                for (int z : new int[] {4, 17, 21, 42, 47}) {
                    // (arriba al este, en la servidumbre, va la escalera del ático pegada al muro: sin cortinas)
                    ventana(t, false, f, s, z, z + 1, y[0], y[1], v, !(este && y == pa && z > 38));
                }
            }
            // el salón de baile: ventanales de dos pisos (al este dan al pasillo de servicio)
            for (int z : new int[] {26, 30, 34}) {
                ventana(t, false, f, s, z, z + 1, PB + 2, PA - 2, v, !este);
                ventana(t, false, f, s, z, z + 1, PA + 1, ATI - 2, v, false);
            }
        }
    }
}
