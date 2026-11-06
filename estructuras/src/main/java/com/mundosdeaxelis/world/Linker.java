package com.mundosdeaxelis.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Red de seguridad para estructuras ya construidas: recorre el plano como lo haría un jugador (camina, salta 1 bloque, baja hasta 3, usa escaleras de mano)
 * desde la entrada y, si alguna sala cerrada no se puede alcanzar, le abre una puerta con arco hacia la zona alcanzable más cercana.
 * Solo trabaja con el plano en texto, nada de Minecraft.
 */
final class Linker {
    private static final Set<String> PASS = new HashSet<>(List.of(("air cave_air void_air torch wall_torch soul_torch soul_wall_torch redstone_torch lantern soul_lantern chain "
            + "vine glow_lichen grass tall_grass fern large_fern dandelion poppy blue_orchid azure_bluet lily_of_the_valley cornflower allium oxeye_daisy "
            + "pink_tulip red_tulip orange_tulip white_tulip peony rose_bush lilac sunflower brown_mushroom red_mushroom moss_carpet snow cobweb "
            + "candle white_candle skeleton_skull wither_skeleton_skull zombie_head creeper_head player_head skeleton_wall_skull wither_skeleton_wall_skull "
            + "flower_pot button stone_button oak_button spruce_button dark_oak_button polished_blackstone_button lever tripwire tripwire_hook rail powered_rail "
            + "redstone_wire end_rod amethyst_cluster hanging_roots spore_blossom sea_pickle water bubble_column kelp seagrass tall_seagrass "
            + "oak_trapdoor spruce_trapdoor dark_oak_trapdoor birch_trapdoor jungle_trapdoor acacia_trapdoor crimson_trapdoor warped_trapdoor mangrove_trapdoor iron_trapdoor "
            + "oak_fence_gate spruce_fence_gate dark_oak_fence_gate birch_fence_gate crimson_fence_gate warped_fence_gate "
            + "oak_door spruce_door dark_oak_door birch_door jungle_door acacia_door crimson_door warped_door mangrove_door "
            + "fire soul_fire sculk_vein sweet_berry_bush pointed_dripstone small_amethyst_bud medium_amethyst_bud large_amethyst_bud ladder").split(" ")));
    private static final String[] PASS_SUFFIX = {"_carpet", "_pressure_plate", "_wall_banner", "_banner", "_wall_sign", "_wall_skull", "_sapling", "_tulip"};

    private static final boolean DEBUG = Boolean.getBoolean("linker.debug");
    private final StructBp bp;
    private int x0, y0, z0, w, h, d;
    private byte[] kind; // 0 libre, 1 sólido, 2 escalera de mano, 3 agua
    private boolean[] stand;
    private boolean[] reach;
    private boolean[] back;
    private boolean[] target;
    private boolean[] bad;
    private int startIdx;
    private boolean[] ceil;
    private final String frame;
    private final String stairBase;

    private Linker(StructBp bp, String frame, String stairBase) {
        this.bp = bp;
        this.frame = frame;
        this.stairBase = stairBase;
    }

    /** Une las salas aisladas. Devuelve cuántas puertas abrió. {@code minRoom}: tamaño mínimo (casillas de suelo) para contar como sala. */
    static int link(StructBp bp, int sx, int sy, int sz, int minRoom, String frame, String stairBase) {
        Linker l = new Linker(bp, frame, stairBase);
        int doors = 0;
        Set<Integer> giveUp = new HashSet<>();
        for (int it = 0; it < 150; it++) {
            l.build();
            l.walk(sx, sy, sz);
            int n = l.stand.length;
            l.bad = new boolean[n];
            l.target = l.reach;
            for (int i = 0; i < n; i++) {
                l.bad[i] = l.stand[i] && l.ceil[i] && !l.reach[i];
            }
            List<List<Integer>> comps = l.isolated(minRoom, giveUp);
            if (comps.isEmpty()) {
                // zonas a las que se llega pero de las que no se puede volver
                l.target = l.back;
                for (int i = 0; i < n; i++) {
                    l.bad[i] = l.stand[i] && l.ceil[i] && l.reach[i] && !l.back[i];
                }
                comps = l.isolated(minRoom, giveUp);
            }
            if (comps.isEmpty()) {
                break;
            }
            int made = 0;
            for (List<Integer> c : comps) {
                if (l.connect(c) || l.connectStep(c) || l.connectVertical(c)) {
                    made++;
                    break; // una conexión por vuelta: el plano cambia y hay que volver a recorrerlo
                }
            }
            doors += made;
            if (made == 0) {
                for (List<Integer> c : comps) {
                    int i = c.get(0);
                    giveUp.add(i);
                    if (DEBUG) {
                        System.err.println("[Linker] sala sin conexión posible cerca de " + l.cx(i) + "," + l.cy(i) + "," + l.cz(i) + " (" + c.size() + " casillas)");
                    }
                }
            }
        }
        return doors;
    }

    /** Revisión sin tocar nada: {salas cerradas sin acceso, zonas a las que se llega y no se puede volver} con el mismo criterio que usa {@link #link}. */
    static int[] audit(StructBp bp, int sx, int sy, int sz, int minRoom) {
        Linker l = new Linker(bp, "stone_bricks", "stone_brick_stairs");
        l.build();
        l.walk(sx, sy, sz);
        int n = l.stand.length;
        l.bad = new boolean[n];
        for (int i = 0; i < n; i++) {
            l.bad[i] = l.stand[i] && l.ceil[i] && !l.reach[i];
        }
        List<List<Integer>> rs = l.isolated(minRoom, new HashSet<>());
        int rooms = rs.size();
        if (DEBUG) {
            for (List<Integer> c : rs) {
                int i = c.get(0);
                System.err.println("[Linker] sala sin acceso cerca de " + l.cx(i) + "," + l.cy(i) + "," + l.cz(i) + " (" + c.size() + " casillas)");
            }
        }
        for (int i = 0; i < n; i++) {
            l.bad[i] = l.stand[i] && l.ceil[i] && l.reach[i] && !l.back[i];
        }
        int traps = l.isolated(minRoom, new HashSet<>()).size();
        return new int[]{rooms, traps, l.startIdx < 0 ? 1 : 0};
    }

    private static boolean passName(String s) {
        int k = s.indexOf('[');
        String n = k < 0 ? s : s.substring(0, k);
        if (PASS.contains(n)) {
            return true;
        }
        for (String suf : PASS_SUFFIX) {
            if (n.endsWith(suf)) {
                return true;
            }
        }
        return n.startsWith("potted_");
    }

    private int idx(int x, int y, int z) {
        return ((x - x0) * h + (y - y0)) * d + (z - z0);
    }

    private int cx(int i) {
        return i / (h * d) + x0;
    }

    private int cy(int i) {
        return (i / d) % h + y0;
    }

    private int cz(int i) {
        return i % d + z0;
    }

    private boolean in(int x, int y, int z) {
        return x >= x0 && x < x0 + w && y >= y0 && y < y0 + h && z >= z0 && z < z0 + d;
    }

    private void build() {
        int ax = Integer.MAX_VALUE, bx = Integer.MIN_VALUE, ay = ax, by = bx, az = ax, bz = bx;
        for (long k : bp.m.keySet()) {
            ax = Math.min(ax, StructBp.kx(k));
            bx = Math.max(bx, StructBp.kx(k));
            ay = Math.min(ay, StructBp.ky(k));
            by = Math.max(by, StructBp.ky(k));
            az = Math.min(az, StructBp.kz(k));
            bz = Math.max(bz, StructBp.kz(k));
        }
        x0 = ax - 4;
        y0 = ay - 2;
        z0 = az - 6;
        w = bx - ax + 9;
        h = by - ay + 9;
        d = bz - az + 11;
        kind = new byte[w * h * d];
        for (int x = x0; x < x0 + w; x++) {
            for (int y = y0; y < y0 + h; y++) {
                for (int z = z0; z < z0 + d; z++) {
                    kind[idx(x, y, z)] = (byte) (y <= -1 ? 1 : 0);
                }
            }
        }
        for (var e : bp.m.entrySet()) {
            long k = e.getKey();
            String s = e.getValue();
            byte t;
            if (s.startsWith("ladder")) {
                t = 2;
            } else if (s.startsWith("water") || s.startsWith("bubble_column")) {
                t = 3;
            } else if (s.startsWith("iron_door") && s.contains("open=true")) {
                t = 0;
            } else {
                t = (byte) (passName(s) ? 0 : 1);
            }
            kind[idx(StructBp.kx(k), StructBp.ky(k), StructBp.kz(k))] = t;
        }
    }

    private boolean clear(int x, int y, int z) {
        return in(x, y, z) && kind[idx(x, y, z)] != 1;
    }

    private void walk(int sx, int sy, int sz) {
        int n = w * h * d;
        stand = new boolean[n];
        for (int x = x0; x < x0 + w; x++) {
            for (int y = y0 + 1; y < y0 + h - 2; y++) {
                for (int z = z0; z < z0 + d; z++) {
                    int i = idx(x, y, z);
                    if (kind[i] == 1 || kind[idx(x, y + 1, z)] == 1) {
                        continue;
                    }
                    int below = kind[idx(x, y - 1, z)];
                    if (below == 1 || below == 3 || kind[i] == 2 || kind[i] == 3) {
                        stand[i] = true;
                    }
                }
            }
        }
        reach = new boolean[n];
        int sY = sy;
        for (int k = 0; k < 6; k++) {
            if (in(sx, sY + k, sz) && stand[idx(sx, sY + k, sz)]) {
                sY += k;
                break;
            }
        }
        ArrayDeque<Integer> dq = new ArrayDeque<>();
        startIdx = -1;
        if (in(sx, sY, sz) && stand[idx(sx, sY, sz)]) {
            startIdx = idx(sx, sY, sz);
            reach[startIdx] = true;
            dq.add(startIdx);
        }
        while (!dq.isEmpty()) {
            int c = dq.poll();
            int[] sc = succ(c);
            for (int j : sc) {
                if (!reach[j]) {
                    reach[j] = true;
                    dq.add(j);
                }
            }
        }
        // celdas desde las que se puede volver a la entrada (grafo inverso)
        back = new boolean[n];
        if (startIdx >= 0) {
            java.util.Map<Integer, List<Integer>> rev = new java.util.HashMap<>();
            for (int i = 0; i < n; i++) {
                if (reach[i]) {
                    for (int j : succ(i)) {
                        rev.computeIfAbsent(j, k -> new ArrayList<>()).add(i);
                    }
                }
            }
            ArrayDeque<Integer> q2 = new ArrayDeque<>();
            back[startIdx] = true;
            q2.add(startIdx);
            while (!q2.isEmpty()) {
                int c = q2.poll();
                List<Integer> pr = rev.get(c);
                if (pr != null) {
                    for (int j : pr) {
                        if (!back[j]) {
                            back[j] = true;
                            q2.add(j);
                        }
                    }
                }
            }
        }
        ceil = new boolean[n];
        for (int x = x0; x < x0 + w; x++) {
            for (int z = z0; z < z0 + d; z++) {
                for (int y = y0; y < y0 + h; y++) {
                    for (int k = 2; k <= 10; k++) {
                        if (y + k < y0 + h && kind[idx(x, y + k, z)] == 1) {
                            ceil[idx(x, y, z)] = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    private int[] succ(int c) {
        int x = cx(c), y = cy(c), z = cz(c);
        int[] out = new int[24];
        int m = 0;
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] dbs = {1, 0, -1, -2, -3};
        for (int[] dd : dir) {
            int nx = x + dd[0], nz = z + dd[1];
            if (nx < x0 || nx >= x0 + w || nz < z0 || nz >= z0 + d) {
                continue;
            }
            for (int db : dbs) {
                int ny = y + db;
                if (ny < y0 + 1 || ny >= y0 + h - 2) {
                    continue;
                }
                int ni = idx(nx, ny, nz);
                if (!stand[ni]) {
                    continue;
                }
                if (db == 1 && !(clear(x, y + 2, z) && clear(nx, y + 1, nz) && clear(nx, y + 2, nz))) {
                    continue;
                }
                if (db <= 0 && !(clear(nx, y, nz) && clear(nx, y + 1, nz))) {
                    continue;
                }
                out[m++] = ni;
            }
        }
        if (kind[c] == 2 || kind[c] == 3) {
            for (int db : new int[]{1, -1}) {
                int ny = y + db;
                if (ny >= y0 + 1 && ny < y0 + h - 2 && stand[idx(x, ny, z)]
                        && (kind[idx(x, ny, z)] == 2 || kind[idx(x, ny, z)] == 3 || db == 1 && clear(x, ny + 1, z))) {
                    out[m++] = idx(x, ny, z);
                }
            }
        }
        return java.util.Arrays.copyOf(out, m);
    }

    /** Componentes de suelo interior no alcanzable (los de al menos {@code minRoom} casillas), de mayor a menor. */
    private List<List<Integer>> isolated(int minRoom, Set<Integer> giveUp) {
        int n = w * h * d;
        boolean[] seen = new boolean[n];
        List<List<Integer>> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!bad[i] || seen[i]) {
                continue;
            }
            List<Integer> comp = new ArrayList<>();
            ArrayDeque<Integer> st = new ArrayDeque<>();
            st.add(i);
            seen[i] = true;
            while (!st.isEmpty()) {
                int c = st.poll();
                comp.add(c);
                int x = cx(c), y = cy(c), z = cz(c);
                for (int a = -1; a <= 1; a++) {
                    for (int bb = -1; bb <= 1; bb++) {
                        for (int cc = -1; cc <= 1; cc++) {
                            if (!in(x + a, y + bb, z + cc)) {
                                continue;
                            }
                            int j = idx(x + a, y + bb, z + cc);
                            if (!seen[j] && bad[j]) {
                                seen[j] = true;
                                st.add(j);
                            }
                        }
                    }
                }
            }
            if (comp.size() >= minRoom && !giveUp.contains(comp.get(0)) && hasFlat(comp)) {
                out.add(comp);
            }
        }
        out.sort((a, b) -> b.size() - a.size());
        return out;
    }

    /** Una sala de verdad tiene al menos un trozo de suelo plano de 3x3; las pendientes de cúpulas, cornisas y repisas no cuentan. */
    private boolean hasFlat(List<Integer> comp) {
        Set<Integer> cs = new HashSet<>(comp);
        for (int c : comp) {
            int x = cx(c), y = cy(c), z = cz(c), ok = 0;
            for (int a = -1; a <= 1; a++) {
                for (int b = -1; b <= 1; b++) {
                    if (in(x + a, y, z + b) && cs.contains(idx(x + a, y, z + b))) {
                        ok++;
                    }
                }
            }
            if (ok >= 5) {
                return true;
            }
        }
        return false;
    }

    private boolean connect(List<Integer> comp) {
        Set<Integer> in = new HashSet<>(comp);
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        long bestCost = Long.MAX_VALUE;
        int[] best = null;
        for (int c : comp) {
            int x = cx(c), y = cy(c), z = cz(c);
            for (int di = 0; di < 4; di++) {
                int ddx = dir[di][0], ddz = dir[di][1];
                int tx = -ddz, tz = ddx; // tangente
                // el suelo del lado de la sala debe ser ancho (3 casillas de la misma sala)
                boolean wide = in.contains(idx(x + tx, y, z + tz)) && in.contains(idx(x - tx, y, z - tz));
                for (int n = 2; n <= 6; n++) {
                    int px = x + ddx * n, pz = z + ddz * n;
                    if (!this.in(px, y, pz)) {
                        break;
                    }
                    int hit = -1;
                    for (int dy : new int[]{0, 1, -1}) {
                        int py = y + dy;
                        if (this.in(px, py, pz) && stand[idx(px, py, pz)] && target[idx(px, py, pz)]) {
                            hit = py;
                            break;
                        }
                    }
                    if (hit < 0) {
                        continue;
                    }
                    // las casillas entre medias no pueden ser suelo alcanzable de otra zona: deben ser muro (o aire sin suelo)
                    boolean wall = true;
                    for (int m = 1; m < n && wall; m++) {
                        int mx = x + ddx * m, mz = z + ddz * m;
                        if (stand[idx(mx, y, mz)] || stand[idx(mx, y + 1, mz)]) {
                            wall = false;
                        }
                    }
                    if (!wall) {
                        break;
                    }
                    boolean interior = ceil[idx(px, hit, pz)];
                    long cost = n * 10L + (interior ? 0 : 7) + (wide ? 0 : 25) + Math.abs(hit - y) * 3L;
                    // preferimos puertas lejos de las esquinas: casillas vecinas de la sala a ambos lados
                    if (cost < bestCost) {
                        bestCost = cost;
                        best = new int[]{x, y, z, di, n, hit};
                    }
                    break;
                }
            }
        }
        if (best == null) {
            return false;
        }
        int x = best[0], y = best[1], z = best[2], di = best[3], n = best[4], hit = best[5];
        if (DEBUG) {
            System.err.println("[Linker] puerta en " + x + "," + y + "," + z + " dir " + di + " grosor " + n + " nivel " + hit);
        }
        int ddx = dir[di][0], ddz = dir[di][1];
        int tx = -ddz, tz = ddx;
        int fl = Math.max(y, hit);
        for (int m = 1; m < n; m++) {
            for (int t = -1; t <= 1; t++) {
                int px = x + ddx * m + tx * t, pz = z + ddz * m + tz * t;
                for (int yy = fl; yy < fl + 3; yy++) {
                    bp.set(px, yy, pz, "air");
                }
                if (!solidBp(px, fl - 1, pz)) {
                    bp.set(px, fl - 1, pz, frame);
                }
                if (m == 1 || m == n - 1) {
                    // jambas y arco
                    if (t == -1 || t == 1) {
                        bp.set(px, fl + 2, pz, StructBp.stair(stairBase, t == -1 ? signName(-tx, -tz) : signName(tx, tz), true));
                    }
                }
            }
            int jx1 = x + ddx * m + tx * 2, jz1 = z + ddz * m + tz * 2;
            int jx2 = x + ddx * m - tx * 2, jz2 = z + ddz * m - tz * 2;
            for (int yy = fl; yy < fl + 3; yy++) {
                if (solidBp(jx1, yy, jz1)) {
                    bp.set(jx1, yy, jz1, frame);
                }
                if (solidBp(jx2, yy, jz2)) {
                    bp.set(jx2, yy, jz2, frame);
                }
            }
        }
        // altura libre sobre la casilla de la sala y la de destino, por si el techo es bajo
        bp.set(x, fl + 2, z, "air");
        int gx = x + ddx * n, gz = z + ddz * n;
        bp.set(gx, fl + 2, gz, "air");
        return true;
    }

    /** Un desnivel de 2 bloques entre un suelo y otro (te caes y no puedes volver a subir): se pone un escalón junto a la pared. */
    private boolean connectStep(List<Integer> comp) {
        Set<Integer> cs = new HashSet<>(comp);
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        long bestCost = Long.MAX_VALUE;
        int[] best = null;
        for (int c : comp) {
            int x = cx(c), y = cy(c), z = cz(c);
            for (int di = 0; di < 4; di++) {
                int nx = x + dir[di][0], nz = z + dir[di][1];
                if (!in(nx, y + 2, nz) || !stand[idx(nx, y + 2, nz)] || !target[idx(nx, y + 2, nz)]) {
                    continue;
                }
                if (kind[idx(nx, y, nz)] != 1 || kind[idx(nx, y + 1, nz)] != 1) {
                    continue;
                }
                if (!clear(x, y + 1, z) || !clear(x, y + 2, z) || !clear(x, y + 3, z) || kind[idx(x, y, z)] == 1) {
                    continue;
                }
                // se prefiere un tramo ancho (tres casillas junto a la pared) y de la misma sala
                int tx = -dir[di][1], tz = dir[di][0];
                int wide = 0;
                for (int t = -1; t <= 1; t += 2) {
                    if (cs.contains(idx(x + tx * t, y, z + tz * t)) && stepOk(x + tx * t, y, z + tz * t, dir[di])) {
                        wide++;
                    }
                }
                long cost = (2 - wide) * 10L + (ceil[idx(x, y, z)] ? 0 : 5);
                if (cost < bestCost) {
                    bestCost = cost;
                    best = new int[]{x, y, z, di};
                }
            }
        }
        if (best == null) {
            return false;
        }
        int x = best[0], y = best[1], z = best[2], di = best[3];
        if (DEBUG) {
            System.err.println("[Linker] escalón en " + x + "," + y + "," + z + " dir " + di);
        }
        int tx = -dir[di][1], tz = dir[di][0];
        String st = StructBp.stair(stairBase, signName(dir[di][0], dir[di][1]), false);
        bp.set(x, y, z, st);
        for (int t = -1; t <= 1; t += 2) {
            if (cs.contains(idx(x + tx * t, y, z + tz * t)) && stepOk(x + tx * t, y, z + tz * t, dir[di])) {
                bp.set(x + tx * t, y, z + tz * t, st);
            }
        }
        return true;
    }

    private boolean stepOk(int x, int y, int z, int[] d) {
        int nx = x + d[0], nz = z + d[1];
        return in(nx, y + 2, nz) && stand[idx(nx, y + 2, nz)] && kind[idx(nx, y, nz)] == 1 && kind[idx(nx, y + 1, nz)] == 1
                && clear(x, y + 1, z) && clear(x, y + 2, z) && clear(x, y + 3, z) && kind[idx(x, y, z)] != 1;
    }

    // ------------------------------------------------------------------ conexión vertical: escalera de caracol

    /** Suelo casi plano de 3x3 (se admite un pilar o un hueco suelto: la escalera de caracol lo sustituye). */
    private boolean flat(int cx, int cz, int y, boolean needReach, Set<Integer> compSet) {
        int ok = 0;
        for (int a = -1; a <= 1; a++) {
            for (int c = -1; c <= 1; c++) {
                if (!in(cx + a, y, cz + c)) {
                    return false;
                }
                int i = idx(cx + a, y, cz + c);
                if (stand[i] && (needReach ? target[i] : compSet.contains(i))) {
                    ok++;
                }
            }
        }
        return ok >= 7;
    }

    private boolean openNeighbor(int x, int y, int z, int rx, int rz, boolean needReach, Set<Integer> compSet) {
        int[][] outs = rx != 0 && rz != 0 ? new int[][]{{rx, 0}, {0, rz}} : (rx != 0 ? new int[][]{{rx, 0}} : new int[][]{{0, rz}});
        for (int[] o : outs) {
            int nx = x + rx + o[0], nz = z + rz + o[1];
            if (in(nx, y, nz)) {
                int i = idx(nx, y, nz);
                if (stand[i] && (needReach ? target[i] : compSet.contains(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static final int[][] RING = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};

    private boolean connectVertical(List<Integer> comp) {
        Set<Integer> cs = new HashSet<>(comp);
        long bestCost = Long.MAX_VALUE;
        int[] best = null; // cx, cz, yc, yr, s, dir
        Set<Long> tried = new HashSet<>();
        for (int c : comp) {
            int cx = cx(c), cz = cz(c), yc = cy(c);
            if (!tried.add(((long) cx << 40) ^ ((long) cz << 20) ^ yc) || !flat(cx, cz, yc, false, cs)) {
                continue;
            }
            for (int yr = yc - 40; yr <= yc + 40; yr++) {
                int dlt = Math.abs(yr - yc);
                if (dlt < 4 || !in(cx, yr, cz) || !flat(cx, cz, yr, true, null)) {
                    continue;
                }
                boolean inter = ceil[idx(cx, yr, cz)];
                int lo = Math.min(yc, yr), hi = Math.max(yc, yr);
                boolean loComp = yc < yr;
                for (int s = 0; s < 8; s++) {
                    for (int dir = -1; dir <= 1; dir += 2) {
                        int e = ((s + dir * dlt) % 8 + 8) % 8;
                        boolean okLo = loComp ? openNeighbor(cx, lo, cz, RING[s][0], RING[s][1], false, cs) : openNeighbor(cx, lo, cz, RING[s][0], RING[s][1], true, null);
                        boolean okHi = loComp ? openNeighbor(cx, hi, cz, RING[e][0], RING[e][1], true, null) : openNeighbor(cx, hi, cz, RING[e][0], RING[e][1], false, cs);
                        if (!okLo || !okHi) {
                            continue;
                        }
                        long cost = dlt * 10L + (inter ? 0 : 60) + (long) Math.abs(cx - 0) / 8 + s;
                        if (cost < bestCost) {
                            bestCost = cost;
                            best = new int[]{cx, cz, lo, hi, s, dir};
                        }
                    }
                }
            }
        }
        if (best == null) {
            return false;
        }
        int cx = best[0], cz = best[1], lo = best[2], hi = best[3], s = best[4], dir = best[5];
        int dlt = hi - lo;
        if (DEBUG) {
            System.err.println("[Linker] escalera en " + cx + "," + cz + " de y=" + lo + " a y=" + hi + " (inicio " + s + ", sentido " + dir + ")");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = lo; y <= hi + 2; y++) {
                    bp.set(cx + x, y, cz + z, "air");
                }
            }
        }
        for (int y = lo; y <= hi - 1; y++) {
            bp.set(cx, y, cz, frame);
        }
        int[] kmax = new int[8];
        java.util.Arrays.fill(kmax, -1);
        for (int k = 1; k <= dlt - 1; k++) {
            int ci = ((s + dir * k) % 8 + 8) % 8;
            int ni = ((s + dir * (k + 1)) % 8 + 8) % 8;
            int[] a = RING[ci], nb = RING[ni];
            bp.set(cx + a[0], lo + k - 1, cz + a[1], StructBp.stair(stairBase, signName(nb[0] - a[0], nb[1] - a[1]), false));
            kmax[ci] = k;
        }
        int landing = ((s + dir * dlt) % 8 + 8) % 8;
        bp.set(cx + RING[landing][0], hi - 1, cz + RING[landing][1], frame);
        for (int ci = 0; ci < 8; ci++) {
            if (ci == landing) {
                continue;
            }
            if (kmax[ci] < 0 || lo + kmax[ci] <= hi - 4) {
                bp.set(cx + RING[ci][0], hi - 1, cz + RING[ci][1], frame);
            }
        }
        return true;
    }

    private boolean solidBp(int x, int y, int z) {
        String s = bp.get(x, y, z);
        if (s == null) {
            return y <= -1;
        }
        return !passName(s);
    }

    private static String signName(int dx, int dz) {
        if (dx > 0) {
            return "+x";
        }
        if (dx < 0) {
            return "-x";
        }
        return dz > 0 ? "+z" : "-z";
    }
}
