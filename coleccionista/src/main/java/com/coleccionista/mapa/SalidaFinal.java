package com.coleccionista.mapa;

/**
 * La salida del ganador (el desenlace): detrás de la puerta del este de la mesa de juego empieza un pasillo largo que se va quedando
 * atrás de la casa del Coleccionista. Primero sigue la casa (parquet, alfombra roja, lambrín, sus muñecas en repisas), luego un sótano
 * viejo (ladrillo, piso húmedo, telarañas, cadenas, casi sin luz), luego un tramo de yeso blanco cada vez más alto y más claro, y al fondo un portón
 * abierto que da a la luz: un cuarto todo blanco, sin sombras. Ahí se acaba el evento.
 * <pre>
 *   x: 0 ... 100   z: 13 ... 30   y: -1 ... 11 (piso en y 0)
 *   se entra por x 0 (z 21 y 22, frente a la puerta de la mesa de juego); el pasillo va hacia el este por el centro (z 21.5)
 *   la casa x 1..22 · el sótano x 23..50 · el yeso x 51..74 · el portón x 75..77 · la luz x 78..99
 * </pre>
 * Marcas: {@code sf.inicio}, {@code sf.luz} (al pasar de aquí empieza el final), {@code sf.fin} (el fondo de la luz).
 */
public final class SalidaFinal {
    private static final String C = "coleccionista:";
    public static final int LARGO = 100;
    /** Dónde empieza cada tramo (x). */
    public static final int SOTANO = 23, YESO = 51, PORTON = 75, LUZ = 78;

    private SalidaFinal() {
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.caja(0, -1, 13, LARGO, 11, 30, C + "ladrillo_oscuro");
        Salida.llegada(p, C + "madera_pulida");
        casa(p);
        sotano(p);
        yeso(p);
        porton(p);
        luz(p);
        p.marca("sf.inicio", 2, 1, 21, 270);
        p.marca("sf.luz", LUZ + 2, 1, 21, 270);
        p.marca("sf.fin", LARGO - 6, 1, 21, 270);
        return p;
    }

    /** Vacía un tramo del pasillo (de x a hasta x, de za a zb, con techo en y alto + 1). */
    private static void hueco(Plano p, int xa, int xb, int za, int zb, int alto) {
        for (int x = xa; x <= xb; x++) {
            for (int z = za; z <= zb; z++) {
                for (int y = 1; y <= alto; y++) {
                    p.set(x, y, z, "air");
                }
            }
        }
    }

    private static void casa(Plano p) {
        int za = 20, zb = 23, alto = 5;
        hueco(p, 1, SOTANO - 1, za, zb, alto);
        for (int x = 1; x < SOTANO; x++) {
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, C + "parquet");
                p.set(x, alto + 1, z, (x % 4 == 0) ? C + "marco_dorado" : C + "panel_madera");
            }
            // la alfombra roja por el centro
            p.set(x, 1, 21, C + "alfombra_roja");
            p.set(x, 1, 22, C + "alfombra_roja");
            for (int y = 1; y <= alto; y++) {
                String b = y <= 2 ? C + "panel_madera" : y == 3 || y == alto ? C + "marco_dorado" : C + "papel_tapiz";
                p.set(x, y, za - 1, b);
                p.set(x, y, zb + 1, b);
            }
            // repisas con muñecas que miran pasar al ganador (una de cada lado, alternadas)
            if (x % 6 == 3) {
                p.set(x, 3, za, C + "losa_madera");
                p.set(x, 4, za, C + "muneca[facing=south]");
            } else if (x % 6 == 0 && x > 1) {
                p.set(x, 3, zb, C + "losa_madera");
                p.set(x, 4, zb, x % 12 == 0 ? C + "craneo[facing=north]" : C + "muneca[facing=north]");
            }
            if (x % 6 == 2) {
                p.set(x, alto, 21, "lantern[hanging=true]");
                p.set(x, alto - 1, 22, "light[level=7]");
            }
        }
    }

    private static void sotano(Plano p) {
        int za = 20, zb = 23, alto = 5;
        hueco(p, SOTANO, YESO - 1, za, zb, alto);
        for (int x = SOTANO; x < YESO; x++) {
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, p.azar(0.25) ? C + "concreto_humedo" : C + "losa_piedra");
                p.set(x, alto + 1, z, C + "ladrillo_viejo");
            }
            for (int y = 1; y <= alto; y++) {
                p.set(x, y, za - 1, C + "ladrillo_viejo");
                p.set(x, y, zb + 1, C + "ladrillo_viejo");
            }
            if (p.azar(0.25)) {
                p.set(x, alto, p.azar(0.5) ? za : zb, "cobweb");
            }
            if (x % 7 == 4) {
                // una cadena colgando y un foco pobre
                p.set(x, alto, za + 1, "chain[axis=y]");
                p.set(x, alto - 1, za + 1, "chain[axis=y]");
                p.set(x, alto, zb - 1, "light[level=" + (x < 40 ? 4 : 6) + "]");
            }
        }
    }

    private static void yeso(Plano p) {
        int za = 19, zb = 24, alto = 7;
        hueco(p, YESO, PORTON - 1, za, zb, alto);
        for (int x = YESO; x < PORTON; x++) {
            double k = (x - YESO) / (double) (PORTON - YESO);
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, C + "marmol_crema");
                p.set(x, alto + 1, z, C + "yeso");
            }
            for (int y = 1; y <= alto; y++) {
                p.set(x, y, za - 1, C + "yeso");
                p.set(x, y, zb + 1, C + "yeso");
            }
            // cada vez más luz: paneles en el techo más seguidos y luz más fuerte
            int cada = k < 0.35 ? 5 : k < 0.7 ? 3 : 2;
            if ((x - YESO) % cada == 0) {
                p.set(x, alto + 1, 21, C + "panel_luz");
                p.set(x, alto + 1, 22, C + "panel_luz");
                int nivel = (int) Math.round(8 + 7 * k);
                p.set(x, alto, 20, "light[level=" + nivel + "]");
                p.set(x, alto, 23, "light[level=" + nivel + "]");
            }
        }
    }

    private static void porton(Plano p) {
        int za = 17, zb = 26, alto = 8;
        hueco(p, PORTON, LUZ - 1, za, zb, alto);
        for (int x = PORTON; x < LUZ; x++) {
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, C + "marmol_crema");
                p.set(x, alto + 1, z, C + "marco_dorado");
            }
            for (int y = 1; y <= alto; y++) {
                p.set(x, y, za - 1, C + "marco_dorado");
                p.set(x, y, zb + 1, C + "marco_dorado");
            }
        }
        // el marco del portón (abierto de par en par: las hojas pegadas a los lados) y su dintel
        for (int y = 1; y <= alto; y++) {
            for (int z : new int[] {za, za + 1, zb - 1, zb}) {
                p.set(PORTON + 1, y, z, y <= 6 ? C + "panel_madera" : C + "marco_dorado");
            }
        }
        for (int z = za + 2; z <= zb - 2; z++) {
            p.set(PORTON + 1, alto, z, C + "marco_dorado");
            p.set(PORTON + 1, alto - 1, z, "light[level=15]");
        }
    }

    private static void luz(Plano p) {
        int za = 14, zb = 29, alto = 10;
        hueco(p, LUZ, LARGO - 1, za, zb, alto);
        for (int x = LUZ; x < LARGO; x++) {
            for (int z = za; z <= zb; z++) {
                p.set(x, 0, z, "white_concrete");
                p.set(x, alto + 1, z, C + "panel_luz");
                for (int y = 1; y <= alto; y++) {
                    // aire con luz por todos lados: ni una sombra
                    p.set(x, y, z, (x + y + z) % 3 == 0 ? "light[level=15]" : "air");
                }
            }
            for (int y = 1; y <= alto; y++) {
                p.set(x, y, za - 1, "white_concrete");
                p.set(x, y, zb + 1, "white_concrete");
            }
        }
        for (int z = za - 1; z <= zb + 1; z++) {
            for (int y = 0; y <= alto + 1; y++) {
                p.set(LARGO, y, z, "white_concrete");
            }
        }
        // el muro del portón visto desde la luz, también blanco
        for (int z = za; z <= zb; z++) {
            for (int y = 1; y <= alto; y++) {
                if (z < 17 || z > 26 || y > 8) {
                    p.set(LUZ - 1, y, z, "white_concrete");
                }
            }
        }
    }
}
