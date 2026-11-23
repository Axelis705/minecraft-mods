package com.coleccionista.mapa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

/**
 * Los Laberintos: el mapa de la prueba 4, "A ciegas". Cuatro laberintos iguales en fila (uno por equipo). Los que caminan van con los
 * ojos vendados por dentro; el guía camina arriba, sobre un piso de vidrio, viendo todo el laberinto, y los guía con la voz. Al norte de
 * cada laberinto, la salida.
 * <pre>
 *   x: 0 ... 84   z: 0 ... 22   y: -1 ... 12
 *   laberinto K (K = 1..4): x 20K-18 .. 20K-4, z 5..19 (15 x 15: celdas de 7 x 7), muros de 3 de alto
 *   piso de vidrio del guía: y 8 (encima de todo el laberinto); techo con luz: y 12
 *   salida: z 1..3, al norte de la esquina noreste del laberinto
 * </pre>
 * Marcas: {@code cie.lab.K.inicio}, {@code cie.lab.K.guia}, {@code cie.lab.K.salida}, {@code cie.lab.K.origen} (la esquina noroeste del
 * laberinto, z 5). El camino bueno y las celdas libres salen de {@link #celdas()} (el mismo para los cuatro).
 */
public final class Laberintos {
    public static final int X0 = 0, X1 = 84, Z0 = 0, Z1 = 22, Y0 = -1, Y1 = 12;
    public static final int LABERINTOS = 4, N = 7, LADO = 2 * N + 1, ZL = 5;
    private static boolean[][] abierto;

    private Laberintos() {
    }

    public static int origenX(int k) {
        return 20 * k - 18;
    }

    /** El laberinto en bloques (LADO x LADO): true = se puede caminar. Siempre el mismo (semilla fija). */
    public static boolean[][] celdas() {
        if (abierto == null) {
            abierto = generar(4242L);
        }
        return abierto;
    }

    /** Laberinto perfecto (un solo camino entre dos puntos) con el algoritmo de "retroceder" desde la celda de inicio. */
    private static boolean[][] generar(long semilla) {
        boolean[][] a = new boolean[LADO][LADO];
        boolean[][] visto = new boolean[N][N];
        Random r = new Random(semilla);
        Deque<int[]> pila = new ArrayDeque<>();
        int[] ini = {0, N - 1};
        visto[ini[0]][ini[1]] = true;
        a[1][2 * (N - 1) + 1] = true;
        pila.push(ini);
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!pila.isEmpty()) {
            int[] c = pila.peek();
            List<int[]> op = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx >= 0 && nz >= 0 && nx < N && nz < N && !visto[nx][nz]) {
                    op.add(d);
                }
            }
            if (op.isEmpty()) {
                pila.pop();
                continue;
            }
            Collections.shuffle(op, r);
            int[] d = op.get(0);
            int nx = c[0] + d[0], nz = c[1] + d[1];
            visto[nx][nz] = true;
            a[2 * c[0] + 1 + d[0]][2 * c[1] + 1 + d[1]] = true;
            a[2 * nx + 1][2 * nz + 1] = true;
            pila.push(new int[] {nx, nz});
        }
        return a;
    }

    /** El camino bueno (en bloques) del inicio (suroeste) a la salida (noreste). */
    public static List<int[]> camino() {
        boolean[][] a = celdas();
        int[] ini = {1, LADO - 2}, fin = {LADO - 2, 1};
        int[][] de = new int[LADO * LADO][];
        boolean[][] v = new boolean[LADO][LADO];
        Deque<int[]> q = new ArrayDeque<>();
        q.add(ini);
        v[ini[0]][ini[1]] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[0] == fin[0] && c[1] == fin[1]) {
                break;
            }
            for (int[] d : dirs) {
                int nx = c[0] + d[0], nz = c[1] + d[1];
                if (nx >= 0 && nz >= 0 && nx < LADO && nz < LADO && a[nx][nz] && !v[nx][nz]) {
                    v[nx][nz] = true;
                    de[nx * LADO + nz] = c;
                    q.add(new int[] {nx, nz});
                }
            }
        }
        List<int[]> l = new ArrayList<>();
        int[] c = fin;
        while (c != null) {
            l.add(0, c);
            c = de[c[0] * LADO + c[1]];
        }
        return l;
    }

    public static Plano crear(long semilla) {
        Plano p = new Plano(semilla);
        p.cajaMezcla(X0, Y0, Z0, X1, Y1, Z1, "deepslate_bricks", "deepslate_tiles", "polished_deepslate");
        boolean[][] a = celdas();
        for (int k = 1; k <= LABERINTOS; k++) {
            int ox = origenX(k);
            for (int dx = 0; dx < LADO; dx++) {
                for (int dz = 0; dz < LADO; dz++) {
                    int x = ox + dx, z = ZL + dz;
                    p.set(x, 0, z, "smooth_stone");
                    for (int y = 1; y <= 7; y++) {
                        p.set(x, y, z, y <= 3 && !a[dx][dz] ? (y == 3 ? "polished_blackstone_bricks" : "deepslate_bricks") : "air");
                    }
                    // el piso de vidrio del guía y su espacio arriba
                    p.set(x, 8, z, "glass");
                    for (int y = 9; y <= 11; y++) {
                        p.set(x, y, z, "air");
                    }
                    p.set(x, 12, z, (dx % 4 == 2 && dz % 4 == 2) ? "glowstone" : "deepslate_tiles");
                    // luz invisible entre los muros y el vidrio: para que el guía vea bien el laberinto desde arriba
                    if (dx % 4 == 1 && dz % 4 == 1) {
                        p.set(x, 6, z, "light[level=13]");
                    }
                }
            }
            // la entrada (abajo a la izquierda, por el sur) y la salida (arriba a la derecha, al norte)
            p.set(ox + 1, 0, ZL + LADO - 2, "red_terracotta");
            p.marca("cie.lab." + k + ".inicio", ox + 1, 1, ZL + LADO - 2, 180);
            for (int z = 1; z < ZL; z++) {
                for (int dx = LADO - 4; dx <= LADO - 1; dx++) {
                    p.set(ox + dx, 0, z, "polished_blackstone");
                    for (int y = 1; y <= 4; y++) {
                        p.set(ox + dx, y, z, "air");
                    }
                }
            }
            // la puerta de salida en el muro norte del laberinto
            for (int y = 1; y <= 3; y++) {
                p.set(ox + LADO - 2, y, ZL, "air");
            }
            p.set(ox + LADO - 2, 0, ZL, "lime_terracotta");
            p.set(ox + LADO - 2, 3, 1, "lantern[hanging=true]");
            p.marca("cie.lab." + k + ".salida", ox + LADO - 2, 1, 2, 180);
            p.marca("cie.lab." + k + ".guia", ox + 1, 9, ZL + LADO - 2, 180);
            p.marca("cie.lab." + k + ".origen", ox, 0, ZL, 0);
            p.letrero(ox + 7, 10, ZL, "dark_oak_wall_sign", "south", "red", "", "LABERINTO " + k, "", "");
        }
        return p;
    }
}
