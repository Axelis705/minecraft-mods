package com.mundosdeaxelis.vfx;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Herramienta (no va en el mod): busca piezas sueltas en las mascotas 3D (patas que no tocan el cuerpo, etc.). Convierte la
 * superficie opaca del modelo en cubitos y busca grupos que no se tocan con el cuerpo principal. Revisa varias poses: quieta,
 * andando (varios momentos del paso) y dando su saltito.
 * Uso: java -cp <clases>:<esta carpeta> com.mundosdeaxelis.vfx.PetGaps <assets> <lista.txt> [celdas]
 * Escribe una línea por pieza suelta: id pose voxeles caja(min..max en fracción del tamaño del modelo).
 */
public final class PetGaps {
    /** Huecos de menos cubitos que esto no se notan (son casi un roce). */
    static int MIN_GAP = Integer.getInteger("gap", 3);

    public static void main(String[] a) throws Exception {
        PetBake.assets = a[0];
        int N = a.length > 2 ? Integer.parseInt(a[2]) : 140;
        for (String line : Files.readAllLines(new File(a[1]).toPath())) {
            String[] p = line.trim().split("\\s+");
            if (p.length < 5) {
                continue;
            }
            String id = p[0];
            int species = Integer.parseInt(p[1]);
            double size = Double.parseDouble(p[2]);
            V3 center = new V3(0, 0.5 * size, 0);
            Cam cam = PetBake.camFrom(30, 12, center);
            // {edad, velocidad, quieta(ticks)}
            double[][] poses = {{40, 0, 0}, {47, 0.2, 0}, {51, 0.2, 0}, {55, 0.2, 0}, {59, 0.2, 0}, {300, 0, 0}};
            String[] names = {"quieta", "anda1", "anda2", "anda3", "anda4", "quieta2"};
            StringBuilder rep = new StringBuilder();
            for (int k = 0; k < poses.length; k++) {
                List<PetBake.Q> qs = PetSheet.pose(species, size, poses[k][0], cam, poses[k][1], poses[k][2]);
                String r = analyze(qs, N);
                if (!r.isEmpty()) {
                    rep.append("  ").append(names[k]).append(": ").append(r).append('\n');
                }
            }
            System.out.println(id + " (" + species + ")" + (rep.length() == 0 ? " OK" : "\n" + rep.toString().stripTrailing()));
        }
    }

    static boolean opaque(PetBake.Q q) {
        if (q.blend() == Sink.ADD || q.tex() != VfxTex.CREATURE3D || PetBake.decalCell(q.uv())) {
            return false;
        }
        double am = (q.col()[3] + q.col()[7] + q.col()[11] + q.col()[15]) / 4;
        return am > 0.98;
    }

    static String analyze(List<PetBake.Q> all, int N) {
        List<PetBake.Q> qs = new ArrayList<>();
        double x0 = 1e9, y0 = 1e9, z0 = 1e9, x1 = -1e9, y1 = -1e9, z1 = -1e9;
        for (PetBake.Q q : all) {
            if (!opaque(q)) {
                continue;
            }
            qs.add(q);
            for (int i = 0; i < 4; i++) {
                double x = q.p()[i * 3], y = q.p()[i * 3 + 1], z = q.p()[i * 3 + 2];
                x0 = Math.min(x0, x);
                y0 = Math.min(y0, y);
                z0 = Math.min(z0, z);
                x1 = Math.max(x1, x);
                y1 = Math.max(y1, y);
                z1 = Math.max(z1, z);
            }
        }
        if (qs.isEmpty()) {
            return "";
        }
        double ext = Math.max(x1 - x0, Math.max(y1 - y0, z1 - z0));
        double h = ext / N;
        int nx = (int) ((x1 - x0) / h) + 3, ny = (int) ((y1 - y0) / h) + 3, nz = (int) ((z1 - z0) / h) + 3;
        byte[] g = new byte[nx * ny * nz];
        for (PetBake.Q q : qs) {
            fill(g, nx, ny, q.p(), 0, 1, 2, x0, y0, z0, h);
            fill(g, nx, ny, q.p(), 0, 2, 3, x0, y0, z0, h);
        }
        int[] comp = new int[g.length];
        List<int[]> comps = new ArrayList<>(); // {voxeles, minx,miny,minz,maxx,maxy,maxz}
        ArrayDeque<Integer> st = new ArrayDeque<>();
        int label = 0;
        for (int i = 0; i < g.length; i++) {
            if (g[i] == 0 || comp[i] != 0) {
                continue;
            }
            label++;
            int[] c = {0, 1 << 30, 1 << 30, 1 << 30, -1, -1, -1};
            comp[i] = label;
            st.push(i);
            while (!st.isEmpty()) {
                int v = st.pop();
                int x = v % nx, y = (v / nx) % ny, z = v / (nx * ny);
                c[0]++;
                c[1] = Math.min(c[1], x);
                c[2] = Math.min(c[2], y);
                c[3] = Math.min(c[3], z);
                c[4] = Math.max(c[4], x);
                c[5] = Math.max(c[5], y);
                c[6] = Math.max(c[6], z);
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dx = -1; dx <= 1; dx++) {
                            int X = x + dx, Y = y + dy, Z = z + dz;
                            if (X < 0 || Y < 0 || Z < 0 || X >= nx || Y >= ny || Z >= nz) {
                                continue;
                            }
                            int w = X + nx * (Y + ny * Z);
                            if (g[w] != 0 && comp[w] == 0) {
                                comp[w] = label;
                                st.push(w);
                            }
                        }
                    }
                }
            }
            comps.add(c);
        }
        if (comps.size() <= 1) {
            return "";
        }
        int big = 0;
        for (int i = 1; i < comps.size(); i++) {
            if (comps.get(i)[0] > comps.get(big)[0]) {
                big = i;
            }
        }
        // distancia (en cubitos) desde el cuerpo principal a cada pieza suelta: se expande por el aire paso a paso
        int[] dist = new int[g.length];
        java.util.Arrays.fill(dist, -1);
        ArrayDeque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < g.length; i++) {
            if (comp[i] == big + 1) {
                dist[i] = 0;
                q.add(i);
            }
        }
        int[] gap = new int[comps.size()];
        java.util.Arrays.fill(gap, 99);
        while (!q.isEmpty()) {
            int v = q.poll();
            int dv = dist[v];
            if (dv >= 40) {
                continue;
            }
            int x = v % nx, y = (v / nx) % ny, z = v / (nx * ny);
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int X = x + dx, Y = y + dy, Z = z + dz;
                        if (X < 0 || Y < 0 || Z < 0 || X >= nx || Y >= ny || Z >= nz) {
                            continue;
                        }
                        int w = X + nx * (Y + ny * Z);
                        if (dist[w] >= 0) {
                            continue;
                        }
                        dist[w] = dv + 1;
                        if (g[w] != 0) {
                            int ci = comp[w] - 1;
                            gap[ci] = Math.min(gap[ci], dv);
                        } else {
                            q.add(w);
                        }
                    }
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < comps.size(); i++) {
            int[] c = comps.get(i);
            if (i == big || c[0] < 30 || gap[i] < MIN_GAP) {
                continue;
            }
            sb.append(String.format("[hueco %d, %d vox y %.2f..%.2f x %.2f..%.2f z %.2f..%.2f] ", gap[i], c[0],
                    c[2] / (double) N, c[5] / (double) N, c[1] / (double) N - 0.5 * nx / N, c[4] / (double) N - 0.5 * nx / N,
                    c[3] / (double) N, c[6] / (double) N));
        }
        return sb.toString().trim();
    }

    static void fill(byte[] g, int nx, int ny, double[] p, int a, int b, int c, double x0, double y0, double z0, double h) {
        double ax = p[a * 3], ay = p[a * 3 + 1], az = p[a * 3 + 2];
        double bx = p[b * 3], by = p[b * 3 + 1], bz = p[b * 3 + 2];
        double cx = p[c * 3], cy = p[c * 3 + 1], cz = p[c * 3 + 2];
        double l1 = Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
        double l2 = Math.sqrt((cx - ax) * (cx - ax) + (cy - ay) * (cy - ay) + (cz - az) * (cz - az));
        int n = (int) Math.ceil(Math.max(l1, l2) / (h * 0.5)) + 1;
        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= n - i; j++) {
                double u = i / (double) n, v = j / (double) n;
                double x = ax + (bx - ax) * u + (cx - ax) * v, y = ay + (by - ay) * u + (cy - ay) * v, z = az + (bz - az) * u + (cz - az) * v;
                int X = (int) ((x - x0) / h) + 1, Y = (int) ((y - y0) / h) + 1, Z = (int) ((z - z0) / h) + 1;
                g[X + nx * (Y + ny * Z)] = 1;
            }
        }
    }
}
