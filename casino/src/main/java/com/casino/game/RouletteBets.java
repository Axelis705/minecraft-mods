package com.casino.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Todas las apuestas de la ruleta europea (37 números, un solo cero) y su geometría sobre el paño. Sin nada de Minecraft (así se prueba sola).
 *
 * El paño se describe con una cuadrícula de 14 x 5 unidades (x hacia la derecha, y hacia los jugadores):
 * el cero ocupa la primera columna (x 0..1, alto 3), los números 1..36 van en 12 columnas de 3 (x 1..13; arriba 3-6-9..., abajo 1-4-7...),
 * las apuestas de columna (2 a 1) están a la derecha (x 13..14), las docenas debajo (y 3..4) y las chances simples debajo de todo (y 4..5).
 * Cada apuesta ("casilla") tiene un identificador fijo, los números que cubre, lo que paga y el punto de la cuadrícula donde se pone la ficha.
 */
public final class RouletteBets {
    public enum Kind { STRAIGHT, SPLIT, STREET, TRIO, FIRST_FOUR, CORNER, SIX_LINE, COLUMN, DOZEN, EVEN_MONEY }

    /** Una casilla de apuesta. */
    public static final class Spot {
        public final int id;
        public final Kind kind;
        public final int[] numbers;
        /** Paga X a 1. */
        public final int payout;
        /** Centro de la casilla (donde va la ficha), en unidades de la cuadrícula. */
        public final double gx;
        public final double gy;
        /** Para columnas, docenas y chances simples: cuál de ellas (0..n-1). Para el resto, 0. */
        public final int index;

        Spot(int id, Kind kind, int[] numbers, int payout, double gx, double gy, int index) {
            this.id = id;
            this.kind = kind;
            this.numbers = numbers;
            this.payout = payout;
            this.gx = gx;
            this.gy = gy;
            this.index = index;
        }

        public boolean covers(int n) {
            for (int v : numbers) {
                if (v == n) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final int COLS = 14;
    public static final int ROWS = 5;
    /** Orden de las casillas de la rueda europea, empezando por el cero y siguiendo el sentido de las agujas del reloj. */
    public static final int[] WHEEL = {0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26};
    private static final int[] REDS = {1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36};

    public static final List<Spot> SPOTS = new ArrayList<>();
    private static final Map<String, Integer> BY_NUMBERS = new HashMap<>();
    /** Primer identificador de cada grupo (para el dibujo y las pruebas). */
    public static final int COLUMN_BASE;
    public static final int DOZEN_BASE;
    public static final int EVEN_BASE;
    /** Chances simples, en el orden del paño: 1-18, par, rojo, negro, impar, 19-36. */
    public static final int LOW = 0, EVEN = 1, RED = 2, BLACK = 3, ODD = 4, HIGH = 5;

    static {
        for (int n = 0; n <= 36; n++) {
            double[] c = numberCenter(n);
            add(Kind.STRAIGHT, new int[]{n}, 35, c[0], c[1], 0);
        }
        // caballos con el cero
        add(Kind.SPLIT, new int[]{0, 1}, 17, 1, 2.5, 0);
        add(Kind.SPLIT, new int[]{0, 2}, 17, 1, 1.5, 0);
        add(Kind.SPLIT, new int[]{0, 3}, 17, 1, 0.5, 0);
        // caballos horizontales (n, n+3) y verticales (n, n+1)
        for (int n = 1; n <= 33; n++) {
            int col = (n - 1) / 3;
            int row = (n - 1) % 3;
            add(Kind.SPLIT, new int[]{n, n + 3}, 17, 2 + col, 2.5 - row, 0);
        }
        for (int n = 1; n <= 35; n++) {
            if (n % 3 == 0) {
                continue;
            }
            int col = (n - 1) / 3;
            int row = (n - 1) % 3;
            add(Kind.SPLIT, new int[]{n, n + 1}, 17, 1.5 + col, 2 - row, 0);
        }
        // calles y seisenas
        for (int n = 1; n <= 34; n += 3) {
            add(Kind.STREET, new int[]{n, n + 1, n + 2}, 11, 1.5 + (n - 1) / 3, 3.0, 0);
        }
        for (int n = 1; n <= 31; n += 3) {
            add(Kind.SIX_LINE, new int[]{n, n + 1, n + 2, n + 3, n + 4, n + 5}, 5, 2 + (n - 1) / 3, 3.0, 0);
        }
        // cuadros
        for (int n = 1; n <= 32; n++) {
            if (n % 3 == 0) {
                continue;
            }
            int col = (n - 1) / 3;
            int row = (n - 1) % 3;
            add(Kind.CORNER, new int[]{n, n + 1, n + 3, n + 4}, 8, 2 + col, 2 - row, 0);
        }
        add(Kind.TRIO, new int[]{0, 1, 2}, 11, 1, 2.0, 0);
        add(Kind.TRIO, new int[]{0, 2, 3}, 11, 1, 1.0, 0);
        add(Kind.FIRST_FOUR, new int[]{0, 1, 2, 3}, 8, 1, 3.0, 0);
        COLUMN_BASE = SPOTS.size();
        for (int k = 0; k < 3; k++) {
            int[] nums = new int[12];
            for (int i = 0; i < 12; i++) {
                nums[i] = 3 * i + 1 + k;
            }
            add(Kind.COLUMN, nums, 2, 13.5, 2.5 - k, k);
        }
        DOZEN_BASE = SPOTS.size();
        for (int k = 0; k < 3; k++) {
            int[] nums = new int[12];
            for (int i = 0; i < 12; i++) {
                nums[i] = 12 * k + 1 + i;
            }
            add(Kind.DOZEN, nums, 2, 3 + 4 * k, 3.5, k);
        }
        EVEN_BASE = SPOTS.size();
        add(Kind.EVEN_MONEY, range(1, 18), 1, 2, 4.5, LOW);
        add(Kind.EVEN_MONEY, filter(0), 1, 4, 4.5, EVEN);
        add(Kind.EVEN_MONEY, filterRed(true), 1, 6, 4.5, RED);
        add(Kind.EVEN_MONEY, filterRed(false), 1, 8, 4.5, BLACK);
        add(Kind.EVEN_MONEY, filter(1), 1, 10, 4.5, ODD);
        add(Kind.EVEN_MONEY, range(19, 36), 1, 12, 4.5, HIGH);
    }

    private RouletteBets() {
    }

    private static int[] range(int a, int b) {
        int[] r = new int[b - a + 1];
        for (int i = 0; i < r.length; i++) {
            r[i] = a + i;
        }
        return r;
    }

    private static int[] filter(int parity) {
        int[] r = new int[18];
        int k = 0;
        for (int n = 1; n <= 36; n++) {
            if (n % 2 == parity) {
                r[k++] = n;
            }
        }
        return r;
    }

    private static int[] filterRed(boolean red) {
        int[] r = new int[18];
        int k = 0;
        for (int n = 1; n <= 36; n++) {
            if (isRed(n) == red) {
                r[k++] = n;
            }
        }
        return r;
    }

    private static void add(Kind kind, int[] numbers, int payout, double gx, double gy, int index) {
        int[] s = numbers.clone();
        Arrays.sort(s);
        Spot sp = new Spot(SPOTS.size(), kind, s, payout, gx, gy, index);
        SPOTS.add(sp);
        BY_NUMBERS.put(key(s), sp.id);
    }

    private static String key(int[] sorted) {
        return Arrays.toString(sorted);
    }

    /** La casilla que cubre exactamente estos números, o -1. */
    public static int byNumbers(int... nums) {
        int[] s = nums.clone();
        Arrays.sort(s);
        Integer id = BY_NUMBERS.get(key(s));
        return id == null ? -1 : id;
    }

    public static boolean isRed(int n) {
        for (int r : REDS) {
            if (r == n) {
                return true;
            }
        }
        return false;
    }

    /** 0 = verde, 1 = rojo, 2 = negro. */
    public static int colorOf(int n) {
        return n == 0 ? 0 : isRed(n) ? 1 : 2;
    }

    /** Centro del casillero de un número en la cuadrícula (el cero ocupa todo el alto de la primera columna). */
    public static double[] numberCenter(int n) {
        if (n == 0) {
            return new double[]{0.5, 1.5};
        }
        return new double[]{1.5 + (n - 1) / 3, 2.5 - (n - 1) % 3};
    }

    /** Fila (0 = arriba) y columna (0..11) de un número del 1 al 36 en la cuadrícula. */
    public static int numberAt(int col, int row) {
        return col * 3 + (2 - row) + 1;
    }

    /** Cuánto se devuelve al jugador (apuesta incluida) por una casilla cuando sale este número. Con "La Partage" en las chances simples. */
    public static int payoutFor(Spot s, int stake, int number) {
        if (s.covers(number)) {
            return stake * (s.payout + 1);
        }
        if (number == 0 && s.kind == Kind.EVEN_MONEY) {
            return stake / 2; // La Partage: si sale el cero, se recupera la mitad
        }
        return 0;
    }

    /** Qué casilla hay bajo un punto de la cuadrícula (gx, gy), o -1 si ninguna. Cerca de los bordes se apuesta a varios números a la vez. */
    public static int hit(double gx, double gy) {
        return hit(gx, gy, 0.26, 0.28);
    }

    public static int hit(double gx, double gy, double ex, double ey) {
        if (gx < 0 || gx >= COLS || gy < 0 || gy >= ROWS) {
            return -1;
        }
        if (gy >= 4) { // chances simples
            if (gx < 1 || gx >= 13) {
                return -1;
            }
            return EVEN_BASE + (int) ((gx - 1) / 2);
        }
        if (gx >= 13) { // columnas 2 a 1
            return gy < 3 ? COLUMN_BASE + (2 - (int) gy) : -1;
        }
        if (gy >= 3 + 0.08 || (gy >= 3 && gx < 1 - ex)) { // docenas
            if (gx < 1 || gx >= 13) {
                return -1;
            }
            return DOZEN_BASE + (int) ((gx - 1) / 4);
        }
        if (gx < 1) { // el cero y su borde derecho
            if (gx > 1 - ex) {
                if (gy >= 3 - ey) {
                    return byNumbers(0, 1, 2, 3);
                }
                if (Math.abs(gy - 2) < ey / 1.5) {
                    return byNumbers(0, 1, 2);
                }
                if (Math.abs(gy - 1) < ey / 1.5) {
                    return byNumbers(0, 2, 3);
                }
                int row = (int) gy;
                return byNumbers(0, numberAt(0, row));
            }
            return 0;
        }
        int col = (int) (gx - 1);
        double fx = gx - 1 - col;
        int row = (int) Math.min(gy, 2.999999);
        double fy = gy - row;
        boolean nearL = fx < ex;
        boolean nearR = fx > 1 - ex && col < 11;
        boolean nearT = fy < ey && row > 0;
        boolean nearB = fy > 1 - ey && row < 2;
        boolean street = row == 2 && fy > 1 - ey;
        if (street) { // borde de abajo del paño de números: calles y seisenas
            if (col == 0 && nearL) {
                return byNumbers(0, 1, 2, 3);
            }
            if (nearL && col > 0) {
                return byNumbers(range(numberAt(col - 1, 2), numberAt(col, 0)));
            }
            if (nearR) {
                return byNumbers(range(numberAt(col, 2), numberAt(col + 1, 0)));
            }
            return byNumbers(numberAt(col, 2), numberAt(col, 1), numberAt(col, 0));
        }
        int h = nearL ? -1 : nearR ? 1 : 0;
        int v = nearT ? -1 : nearB ? 1 : 0;
        if (h == -1 && col == 0) { // borde con el cero
            if (v == 0) {
                return byNumbers(0, numberAt(0, row));
            }
            int r2 = row + v;
            return byNumbers(0, numberAt(0, row), numberAt(0, r2));
        }
        int a = numberAt(col, row);
        if (h != 0 && v != 0) {
            return byNumbers(a, numberAt(col + h, row), numberAt(col, row + v), numberAt(col + h, row + v));
        }
        if (h != 0) {
            return byNumbers(a, numberAt(col + h, row));
        }
        if (v != 0) {
            return byNumbers(a, numberAt(col, row + v));
        }
        return a;
    }

    /** Textos cortos de una casilla para el cartel (clave de traducción y argumentos se resuelven en el cliente). */
    public static String describe(Spot s) {
        return switch (s.kind) {
            case STRAIGHT -> "N" + s.numbers[0];
            default -> s.kind.name() + Arrays.toString(s.numbers);
        };
    }

    // ------------------------------------------------------------------ apuestas "de llamada" (como en los casinos de verdad)

    /** Un grupo de fichas que se apuestan de una vez: pares (casilla, cantidad de fichas). */
    public static int[][] call(int which) {
        return switch (which) {
            case 0 -> voisins();
            case 1 -> tiers();
            case 2 -> orphelins();
            default -> jeuZero();
        };
    }

    public static final int CALLS = 4;

    private static int[] u(int spot, int units) {
        return new int[]{spot, units};
    }

    /** Vecinos del cero: 17 números, 9 fichas. */
    private static int[][] voisins() {
        return new int[][]{u(byNumbers(0, 2, 3), 2), u(byNumbers(4, 7), 1), u(byNumbers(12, 15), 1), u(byNumbers(18, 21), 1), u(byNumbers(19, 22), 1),
                u(byNumbers(32, 35), 1), u(byNumbers(25, 26, 28, 29), 2)};
    }

    /** Tercio del cilindro: 12 números opuestos al cero, 6 fichas. */
    private static int[][] tiers() {
        return new int[][]{u(byNumbers(5, 8), 1), u(byNumbers(10, 11), 1), u(byNumbers(13, 16), 1), u(byNumbers(23, 24), 1), u(byNumbers(27, 30), 1),
                u(byNumbers(33, 36), 1)};
    }

    /** Huérfanos: los 8 números que quedan, 5 fichas. */
    private static int[][] orphelins() {
        return new int[][]{u(1, 1), u(byNumbers(6, 9), 1), u(byNumbers(14, 17), 1), u(byNumbers(17, 20), 1), u(byNumbers(31, 34), 1)};
    }

    /** Juego del cero: 7 números alrededor del cero, 4 fichas. */
    private static int[][] jeuZero() {
        return new int[][]{u(byNumbers(0, 3), 1), u(byNumbers(12, 15), 1), u(26, 1), u(byNumbers(32, 35), 1)};
    }

    public static int units(int[][] call) {
        int t = 0;
        for (int[] c : call) {
            t += c[1];
        }
        return t;
    }
}
