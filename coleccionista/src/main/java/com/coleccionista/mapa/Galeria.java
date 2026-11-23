package com.coleccionista.mapa;

import java.util.ArrayList;
import java.util.List;

/**
 * La Galería de los postes: el mapa de "Encadenados de tres". Seis carriles iguales en fila (uno por trío), separados por barrotes para que
 * se vean entre ellos. Cada carril: la sala de la mesa (donde eligen venda, cinta o esposas), la reja de salida y el recorrido:
 * <ol>
 *   <li>Bosque de postes (la cuerda se enreda si cada quien pasa por un lado).</li>
 *   <li>Pasillo en zigzag de un bloque de ancho.</li>
 *   <li>Túnel bajo (hay que pasar agachado).</li>
 *   <li>Reja de dos palancas: se abre con las dos jaladas a la vez (lejos una de la otra; las esposas no pueden jalar).</li>
 *   <li>Otro bosque de postes, más cerrado.</li>
 * </ol>
 * Al final, la sala de salida.
 * <pre>
 *   x: 0 ... 60 (carril K = 1..6: interior x 10K-9 .. 10K-1, centro 10K-5)    z: 0 ... 47    y: -1 ... 5
 * </pre>
 * Marcas: {@code tres.K.lugar.N} (N 0..3), {@code tres.K.objeto.N} (N 0..3: sobre cada mesa), {@code tres.K.reja} (la reja de salida),
 * {@code tres.K.reja2} (la reja de las palancas), {@code tres.K.palanca.a/b}, {@code tres.K.salida} (desde ahí ya salieron),
 * {@code tres.K.pantalla}, {@code tres.K.origen}.
 */
public final class Galeria {
    public static final int X0 = 0, X1 = 60, Z0 = 0, Z1 = 47, Y0 = -1, Y1 = 5;
    public static final int CARRILES = 6, Z_SALIDA = 42;
    private static final String C = "coleccionista:";
    /** Los postes del recorrido, relativos al centro del carril (dx, z). */
    private static final int[][] POSTES = {
            {-3, 12}, {0, 12}, {3, 12}, {-2, 15}, {2, 15}, {-3, 18}, {0, 18}, {3, 18},
            {-3, 36}, {-1, 36}, {1, 36}, {3, 36}, {-2, 38}, {0, 38}, {2, 38}, {-3, 40}, {-1, 40}, {1, 40}, {3, 40}};

    private Galeria() {
    }

    public static int centro(int k) {
        return 10 * k - 5;
    }

    /** Los postes de un carril en coordenadas del plano (x + 0.5, z + 0.5: el centro del bloque). */
    public static List<double[]> postes(int k) {
        List<double[]> l = new ArrayList<>();
        for (int[] p : POSTES) {
            l.add(new double[] {centro(k) + p[0] + 0.5, p[1] + 0.5});
        }
        return l;
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        // casco: piso de concreto, techo de concreto, muros con la banda de azulejo
        p.caja(X0, Y0, Z0, X1, Y0, Z1, C + "concreto");
        p.caja(X0, 0, Z0, X1, 0, Z1, C + "concreto_piso");
        p.caja(X0, Y1, Z0, X1, Y1, Z1, C + "concreto");
        for (int y = 1; y < Y1; y++) {
            String m = y == 1 ? C + "azulejo_sucio" : y == 2 ? C + "azulejo_remate" : C + "yeso";
            for (int x = X0; x <= X1; x++) {
                p.set(x, y, Z0, m);
                p.set(x, y, Z1, m);
            }
            for (int z = Z0; z <= Z1; z++) {
                p.set(X0, y, z, m);
                p.set(X1, y, z, m);
            }
        }
        p.caja(X0 + 1, 1, Z0 + 1, X1 - 1, Y1 - 1, Z1 - 1, "air");
        for (int k = 1; k <= CARRILES; k++) {
            carril(p, k);
        }
        // separadores entre carriles: muro en la sala de la mesa, barrotes en el recorrido (se ven entre ellos)
        for (int k = 1; k < CARRILES; k++) {
            int x = 10 * k;
            for (int z = 1; z < Z1; z++) {
                for (int y = 1; y < Y1; y++) {
                    boolean muro = z <= 9 || z >= Z_SALIDA - 1 || y == Y1 - 1;
                    p.set(x, y, z, muro ? (y == 1 ? C + "azulejo_sucio" : y == 2 ? C + "azulejo_remate" : C + "yeso") : C + "barrotes");
                }
                if (z % 6 == 0) {
                    for (int y = 1; y < Y1; y++) {
                        p.set(x, y, z, C + "viga[axis=y]");
                    }
                }
            }
        }
        return p;
    }

    private static void carril(Plano p, int k) {
        int c = centro(k);
        p.marca("tres." + k + ".origen", c, 0, 0, 0);
        // ---- la sala de la mesa (z 1..8)
        for (int i = 0; i < 4; i++) {
            int x = c - 1 + i;
            p.set(x, 1, 4, C + "mesa[facing=south]");
            p.marca("tres." + k + ".objeto." + i, x, 2, 4, 0);
        }
        for (int i = 0; i < 4; i++) {
            p.marca("tres." + k + ".lugar." + i, c - 2 + (i < 2 ? i : i + 1), 1, 6, 180);
        }
        p.set(c, 3, 1, "coleccionista:pantalla[facing=south]");
        p.marca("tres." + k + ".pantalla", c, 3, 1, 0);
        p.set(c, 4, 4, C + "lampara_tubo[facing=north,encendida=true,falla=false]");
        p.texto(c, 3, 8, "letrero", "north", 0xE8E2D0, "TRÍO " + k);
        p.set(c - 3, 1, 2, C + "silla[facing=south]");
        p.set(c + 3, 1, 7, C + "mancha[facing=east]");
        // la reja de salida (z 9): muro con la reja de 3 en medio
        for (int x = c - 4; x <= c + 4; x++) {
            for (int y = 1; y < Y1; y++) {
                boolean reja = Math.abs(x - c) <= 1 && y <= 3;
                if (!reja) {
                    p.set(x, y, 9, y == 1 ? C + "azulejo_sucio" : y == 2 ? C + "azulejo_remate" : C + "yeso");
                } else {
                    p.set(x, y, 9, C + "barrotes");
                }
            }
        }
        p.marca("tres." + k + ".reja", c - 1, 1, 9, 0);
        // ---- recorrido: piso de rejilla sobre concreto, lámparas cada 6
        for (int z = 10; z < Z_SALIDA; z++) {
            for (int x = c - 4; x <= c + 4; x++) {
                if (p.azar(0.04)) {
                    p.set(x, 1, z, C + p.elegir("mancha", "charco", "papeles") + "[facing=" + p.elegir("north", "east", "south", "west") + "]");
                }
            }
            if (z % 6 == 1) {
                p.set(c, 4, z, C + "lampara_tubo[facing=east,encendida=true,falla=false]");
            }
        }
        // 1) y 5) los bosques de postes
        for (int[] q : POSTES) {
            for (int y = 1; y <= 3; y++) {
                p.set(c + q[0], y, q[1], C + (y == 1 ? "poste" : "poste_liso") + "[axis=y]");
            }
            p.set(c + q[0], 4, q[1], C + "concreto");
        }
        // 2) zigzag: muros que dejan un hueco de 1 alternando lados (z 20..24)
        for (int z : new int[] {20, 22, 24}) {
            int hueco = z == 22 ? c + 3 : c - 3;
            for (int x = c - 4; x <= c + 4; x++) {
                if (x != hueco) {
                    for (int y = 1; y < Y1; y++) {
                        p.set(x, y, z, y == 1 ? C + "azulejo_sucio" : y == 2 ? C + "azulejo_remate" : C + "yeso");
                    }
                }
            }
        }
        // 3) túnel bajo (z 26..28): techo de rejilla a 1.5 bloques, hay que ir agachado
        for (int z = 26; z <= 28; z++) {
            for (int x = c - 4; x <= c + 4; x++) {
                p.set(x, 2, z, Math.abs(x - c) <= 1 ? C + "rejilla_piso[type=top]" : C + "concreto");
                p.set(x, 3, z, C + "concreto");
                if (Math.abs(x - c) > 1) {
                    p.set(x, 1, z, C + "concreto");
                }
            }
            p.set(c - 1, 1, z, z == 27 ? C + "tubo[axis=z]" : "air");
        }
        // 4) la reja de las dos palancas (z 33), las palancas lejos una de la otra (en los muros de los lados, z 30)
        for (int x = c - 4; x <= c + 4; x++) {
            for (int y = 1; y < Y1; y++) {
                boolean reja = Math.abs(x - c) <= 1 && y <= 3;
                p.set(x, y, 33, reja ? C + "barrotes" : y == 1 ? C + "azulejo_sucio" : y == 2 ? C + "azulejo_remate" : C + "yeso");
            }
        }
        p.marca("tres." + k + ".reja2", c - 1, 1, 33, 0);
        p.set(c - 4, 2, 30, C + "palanca[face=wall,facing=east,powered=false]");
        p.set(c + 4, 2, 30, C + "palanca[face=wall,facing=west,powered=false]");
        p.marca("tres." + k + ".palanca.a", c - 4, 2, 30, 0);
        p.marca("tres." + k + ".palanca.b", c + 4, 2, 30, 0);
        p.set(c - 4, 3, 30, C + "lampara_jaula[facing=east,encendida=true,falla=false]");
        p.set(c + 4, 3, 30, C + "lampara_jaula[facing=west,encendida=true,falla=false]");
        // ---- la salida
        for (int x = c - 4; x <= c + 4; x++) {
            p.set(x, 0, Z_SALIDA, C + "metal_placa");
        }
        p.texto(c, 3, Z_SALIDA - 1, "letrero", "north", 0x4CB050, "SALIDA");
        p.marca("tres." + k + ".salida", c, 1, Z_SALIDA + 1, 180);
        p.set(c, 4, Z_SALIDA + 2, C + "lampara_tubo[facing=east,encendida=true,falla=false]");
    }
}
