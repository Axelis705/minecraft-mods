package com.mundosdeaxelis.world;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.mundosdeaxelis.world.StructKit.*;

/**
 * Planificador de salas. Cada sala es una caja de aire ({@link Sp}); las puertas se calculan solas entre salas vecinas
 * (buscando el muro que las separa) y {@link #check()} avisa de salas que se tocan o se encimen y de salas que no
 * quedaron conectadas a la entrada. Así una estructura no se puede "olvidar" de unir una zona con otra.
 */
final class Arq {
    /** Una sala: una o varias cajas de aire unidas (x0,y0,z0,x1,y1,z1 incluidos; el suelo es el bloque y0-1). */
    static final class Sp {
        final String name;
        final List<int[]> boxes = new ArrayList<>();
        final Set<Sp> links = new LinkedHashSet<>();

        Sp(String name) {
            this.name = name;
        }

        int[] first() {
            return boxes.get(0);
        }

        @Override
        public String toString() {
            int[] f = first();
            return name + "[" + f[0] + "," + f[1] + "," + f[2] + ".." + f[3] + "," + f[4] + "," + f[5] + "]";
        }
    }

    final StructBp b;
    final List<Sp> all = new ArrayList<>();
    final List<String> problems = new ArrayList<>();
    final List<Sp[]> joined = new ArrayList<>();
    /** Marco de las puertas. */
    String frameStair = "stone_brick_stairs";
    String frameTrim = "chiseled_stone_bricks";
    Sp entrance;
    /** Casillas (x, piso, z) que el mobiliario no debe tapar: pasos de puertas, frente a ventanas. */
    final Set<Long> resv = new HashSet<>();
    /** Casillas de paso de las puertas (semillas para comprobar que todo el suelo de un piso se puede alcanzar). */
    final Set<Long> doorResv = new HashSet<>();

    void reserve(int x, int f, int z) {
        resv.add(StructBp.key(x, f, z));
    }

    boolean free(int x, int f, int z) {
        return !resv.contains(StructBp.key(x, f, z));
    }

    Arq(StructBp b) {
        this.b = b;
    }

    // ------------------------------------------------------------------ salas

    /** Sala nueva: se vacía (aire) y su suelo es el bloque y0-1 (se deja el que ya haya). */
    Sp room(String name, int x0, int z0, int x1, int z1, int y0, int h) {
        Sp s = new Sp(name);
        add(s, x0, z0, x1, z1, y0, h);
        all.add(s);
        b.rooms.put(name, s.boxes);
        return s;
    }

    /** Añade una caja más a la sala (por ejemplo para salas en L o en anillo). */
    Sp add(Sp s, int x0, int z0, int x1, int z1, int y0, int h) {
        s.boxes.add(new int[]{Math.min(x0, x1), y0, Math.min(z0, z1), Math.max(x0, x1), y0 + h - 1, Math.max(z0, z1)});
        b.air(Math.min(x0, x1), y0, Math.min(z0, z1), Math.max(x0, x1), y0 + h - 1, Math.max(z0, z1));
        return s;
    }

    /** Sala redonda (disco de aire) con una caja que la envuelve. */
    Sp disc(String name, int cx, int cz, double r, int y0, int h) {
        Sp s = new Sp(name);
        int ri = (int) Math.ceil(r);
        s.boxes.add(new int[]{cx - ri, y0, cz - ri, cx + ri, y0 + h - 1, cz + ri});
        b.disc(cx, cz, r, y0, y0 + h - 1, "air");
        all.add(s);
        b.rooms.put(name, s.boxes);
        return s;
    }

    /** Zona que ya existe (por ejemplo el exterior): no se vacía, solo sirve como punto de unión. */
    Sp ghost(String name, int x0, int z0, int x1, int z1, int y0, int h) {
        Sp s = new Sp(name);
        s.boxes.add(new int[]{Math.min(x0, x1), y0, Math.min(z0, z1), Math.max(x0, x1), y0 + h - 1, Math.max(z0, z1)});
        all.add(s);
        return s;
    }

    /** Dos salas que se unen sin muro (se permite que se toquen o se superpongan). */
    void join(Sp a, Sp c) {
        joined.add(new Sp[]{a, c});
        a.links.add(c);
        c.links.add(a);
    }

    // ------------------------------------------------------------------ puertas

    /** Puerta de 3 de ancho y 3 de alto entre dos salas, en el centro del muro que las separa. Devuelve false si no hay muro común. */
    boolean door(Sp a, Sp c) {
        return door(a, c, Integer.MIN_VALUE, Integer.MIN_VALUE, 3, 3, true);
    }

    /** Puerta con el suelo a la altura {@code yf} y centrada en {@code at} (x o z del muro, según su eje). */
    boolean door(Sp a, Sp c, int yf, int at) {
        return door(a, c, yf, at, 3, 3, true);
    }

    boolean door(Sp a, Sp c, int yf, int at, int w, int h, boolean arch) {
        for (int[] ba : a.boxes) {
            for (int[] bc : c.boxes) {
                // eje X: a a la izquierda de c
                if (tryDoor(a, c, ba, bc, 'x', yf, at, w, h, arch) || tryDoor(a, c, bc, ba, 'x', yf, at, w, h, arch)
                        || tryDoor(a, c, ba, bc, 'z', yf, at, w, h, arch) || tryDoor(a, c, bc, ba, 'z', yf, at, w, h, arch)) {
                    a.links.add(c);
                    c.links.add(a);
                    return true;
                }
            }
        }
        problems.add("SIN PUERTA posible entre " + a + " y " + c + (yf == Integer.MIN_VALUE ? "" : " (yf=" + yf + " at=" + at + ")"));
        return false;
    }

    private boolean tryDoor(Sp a, Sp c, int[] lo, int[] hi, char axis, int yf, int at, int w, int h, boolean arch) {
        // lo está antes de hi en el eje; el muro está entre lo.max+1 y hi.min-1
        int lMax = axis == 'x' ? lo[3] : lo[5];
        int hMin = axis == 'x' ? hi[0] : hi[2];
        int gap = hMin - lMax - 1;
        if (gap < 1 || gap > 5) {
            return false;
        }
        int oLo = axis == 'x' ? Math.max(lo[2], hi[2]) : Math.max(lo[0], hi[0]);
        int oHi = axis == 'x' ? Math.min(lo[5], hi[5]) : Math.min(lo[3], hi[3]);
        if (oHi - oLo + 1 < w) {
            return false;
        }
        int yMin = Math.max(lo[1], hi[1]);
        int yMax = Math.min(lo[4], hi[4]);
        int f = yf == Integer.MIN_VALUE ? yMin : yf;
        if (f < yMin || f + h - 1 > yMax) {
            return false;
        }
        int center = at == Integer.MIN_VALUE ? (oLo + oHi) / 2 : at;
        int half = w / 2;
        if (center - half < oLo || center + half > oHi) {
            return false;
        }
        for (int t = lMax - 1; t <= hMin + 1; t++) {
            for (int o = center - half; o <= center + half; o++) {
                if (axis == 'x') {
                    reserve(t, f, o);
                    doorResv.add(StructBp.key(t, f, o));
                } else {
                    reserve(o, f, t);
                    doorResv.add(StructBp.key(o, f, t));
                }
            }
        }
        // las dos salas deben tener suelo a esa altura: el primer bloque de aire de la puerta es f
        for (int t = lMax + 1; t <= hMin - 1; t++) {
            for (int o = center - half; o <= center + half; o++) {
                for (int y = f; y < f + h; y++) {
                    if (axis == 'x') {
                        b.set(t, y, o, "air");
                    } else {
                        b.set(o, y, t, "air");
                    }
                }
                // el umbral queda sólido para poder pisar
                if (axis == 'x') {
                    if (!b.solid(t, f - 1, o)) {
                        b.set(t, f - 1, o, frameTrim);
                    }
                } else {
                    if (!b.solid(o, f - 1, t)) {
                        b.set(o, f - 1, t, frameTrim);
                    }
                }
            }
            if (arch && w >= 3) {
                // esquinas redondeadas arriba: escaleras invertidas que miran hacia afuera
                int y = f + h - 1;
                int o1 = center - half;
                int o2 = center + half;
                if (axis == 'x') {
                    b.set(t, y, o1, stairTop(frameStair, "-z"));
                    b.set(t, y, o2, stairTop(frameStair, "+z"));
                } else {
                    b.set(o1, y, t, stairTop(frameStair, "-x"));
                    b.set(o2, y, t, stairTop(frameStair, "+x"));
                }
            }
            // jambas de adorno a los lados
            for (int y = f; y < f + h + 1; y++) {
                if (axis == 'x') {
                    keepSolid(t, y, center - half - 1, frameTrim);
                    keepSolid(t, y, center + half + 1, frameTrim);
                } else {
                    keepSolid(center - half - 1, y, t, frameTrim);
                    keepSolid(center + half + 1, y, t, frameTrim);
                }
            }
        }
        return true;
    }

    private void keepSolid(int x, int y, int z, String mat) {
        if (b.solid(x, y, z)) {
            b.set(x, y, z, mat);
        }
    }

    private static String stairTop(String base, String up) {
        return StructBp.stair(base, up, true);
    }

    // ------------------------------------------------------------------ escalera de caracol

    /**
     * Escalera de caracol de 3x3 con pilar central: {@code climbs} pisos de altura 8 (una vuelta completa por piso) desde el suelo y0.
     * La puerta de cada piso queda en el centro del lado {@code side} ("+z", "-z", "+x" o "-x").
     * Devuelve la sala (caja de aire de 3x3 desde y0 hasta el techo del último piso + 2).
     */
    Sp spiral(String name, int cx, int cz, int y0, int climbs, String stair, String pillar, String landing, String side, boolean clockwise) {
        Sp s = new Sp(name);
        int top = y0 + climbs * 8 + 2;
        s.boxes.add(new int[]{cx - 1, y0, cz - 1, cx + 1, top, cz + 1});
        b.air(cx - 1, y0, cz - 1, cx + 1, top, cz + 1);
        all.add(s);
        // anillo de 8 casillas empezando en el medio del lado de la puerta
        int[][] ring = ringCells(side, clockwise);
        // pilar central
        b.box(cx, y0 - 1, cz, cx, top - 1, cz, pillar);
        // suelo de la base
        b.box(cx - 1, y0 - 1, cz - 1, cx + 1, y0 - 1, cz + 1, landing);
        // Caracol "de rampa suave": cada casilla del anillo sube MEDIO bloque (losa / bloque entero alternados), así se sube y se baja
        // andando sin saltar y sin atascarse en las esquinas. Un piso (8 bloques) son dos vueltas al anillo de 8 casillas (16 pasos);
        // las dos vueltas quedan 4 bloques una sobre otra, de sobra para pasar de pie.
        String slab = stair.replace("_stairs", "_slab") + "[type=bottom]";
        String full = stair.startsWith("purpur") ? "purpur_block" : landing;
        for (int n = 0; n < climbs; n++) {
            int f = y0 + 8 * n; // superficie del piso n (el bloque de suelo está en f-1)
            for (int st = 1; st <= 16; st++) {
                int[] c = ring[st % 8];
                if (st == 16) {
                    b.set(cx + c[0], f + 7, cz + c[1], landing); // llegada al piso siguiente
                } else if (st % 2 == 0) {
                    b.set(cx + c[0], f + st / 2 - 1, cz + c[1], full);
                } else {
                    b.set(cx + c[0], f + (st - 1) / 2, cz + c[1], slab);
                }
            }
        }
        return s;
    }

    private static String dirName(int dx, int dz) {
        if (dx > 0) {
            return "+x";
        }
        if (dx < 0) {
            return "-x";
        }
        return dz > 0 ? "+z" : "-z";
    }

    private static int[][] ringCells(String side, boolean clockwise) {
        // anillo visto desde arriba (x al este, z al sur) empezando por el norte (0,-1), en el sentido de las agujas del reloj
        int[][] base = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        int rot = switch (side) {
            case "-z" -> 0;
            case "+x" -> 1;
            case "+z" -> 2;
            default -> 3;
        };
        int[][] out = new int[8][];
        for (int i = 0; i < 8; i++) {
            int[] c = base[i];
            for (int r = 0; r < rot; r++) {
                c = new int[]{-c[1], c[0]}; // giro de 90° en el sentido del reloj
            }
            out[i] = c;
        }
        if (!clockwise) {
            int[][] rev = new int[8][];
            rev[0] = out[0];
            for (int i = 1; i < 8; i++) {
                rev[i] = out[8 - i];
            }
            return rev;
        }
        return out;
    }

    // ------------------------------------------------------------------ reparación del mobiliario

    /** Punto de partida extra para {@link #repair(int)} (por ejemplo la llegada de una escalera). */
    void seed(int x, int f, int z) {
        doorResv.add(StructBp.key(x, f, z));
    }

    private static boolean passBlock(String s) {
        if (s == null) {
            return false;
        }
        int k = s.indexOf('[');
        String n = k < 0 ? s : s.substring(0, k);
        return n.equals("air") || n.endsWith("_carpet") || n.equals("candle") || n.endsWith("torch") || n.endsWith("banner") || n.equals("fern")
                || n.equals("grass") || n.endsWith("tulip") || n.equals("allium") || n.equals("cornflower") || n.equals("azure_bluet") || n.equals("lily_of_the_valley")
                || n.endsWith("skull") || n.equals("cobweb") || n.equals("lantern") || n.equals("soul_lantern") || n.equals("chain") || n.equals("end_rod")
                || n.endsWith("mushroom") || n.equals("amethyst_cluster") || n.equals("water");
    }

    private boolean pass(int x, int f, int z) {
        return passBlock(b.get(x, f, z)) && passBlock(b.get(x, f + 1, z));
    }

    private static boolean wallLike(String s) {
        if (s == null) {
            return true;
        }
        int k = s.indexOf('[');
        String n = k < 0 ? s : s.substring(0, k);
        return n.startsWith("deepslate") || n.startsWith("polished_deepslate") || n.startsWith("chiseled_deepslate") || n.startsWith("cracked_deepslate")
                || n.startsWith("purpur") || n.equals("amethyst_block") || n.endsWith("glass_pane") || n.endsWith("glass");
    }

    /**
     * Comprueba que todo el suelo libre de un piso se puede alcanzar desde sus puertas; si un mueble deja una esquina aislada,
     * se quita el mueble que la tapa. Devuelve cuántos muebles se quitaron.
     */
    int repair(int f) {
        Set<Long> cells = new HashSet<>();
        for (Sp s : all) {
            for (int[] bx : s.boxes) {
                if (bx[1] == f) {
                    for (int x = bx[0]; x <= bx[3]; x++) {
                        for (int z = bx[2]; z <= bx[5]; z++) {
                            cells.add(StructBp.key(x, 0, z));
                        }
                    }
                }
            }
        }
        for (long k : doorResv) {
            if (StructBp.ky(k) == f) {
                cells.add(StructBp.key(StructBp.kx(k), 0, StructBp.kz(k)));
            }
        }
        int removed = 0;
        int[][] dd = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int iter = 0; iter < 80; iter++) {
            Set<Long> reach = new HashSet<>();
            ArrayDeque<int[]> dq = new ArrayDeque<>();
            for (long k : doorResv) {
                if (StructBp.ky(k) == f && pass(StructBp.kx(k), f, StructBp.kz(k)) && reach.add(StructBp.key(StructBp.kx(k), 0, StructBp.kz(k)))) {
                    dq.add(new int[]{StructBp.kx(k), StructBp.kz(k)});
                }
            }
            while (!dq.isEmpty()) {
                int[] c = dq.poll();
                for (int[] d : dd) {
                    int nx = c[0] + d[0], nz = c[1] + d[1];
                    long key = StructBp.key(nx, 0, nz);
                    if (cells.contains(key) && !reach.contains(key) && pass(nx, f, nz)) {
                        reach.add(key);
                        dq.add(new int[]{nx, nz});
                    }
                }
            }
            // casillas libres que no se alcanzan
            java.util.List<int[]> lost = new ArrayList<>();
            for (long key : cells) {
                int x = StructBp.kx(key), z = StructBp.kz(key);
                if (!reach.contains(key) && pass(x, f, z) && b.solid(x, f - 1, z)) {
                    lost.add(new int[]{x, z});
                }
            }
            if (lost.isEmpty()) {
                break;
            }
            boolean any = false;
            for (int[] u : lost) {
                // mueble vecino que toca también una casilla alcanzada: quitarlo; si no, cualquier mueble vecino
                int[] best = null;
                int[] any1 = null;
                for (int[] d : dd) {
                    int nx = u[0] + d[0], nz = u[1] + d[1];
                    if (pass(nx, f, nz) || wallLike(b.get(nx, f, nz)) && wallLike(b.get(nx, f + 1, nz))) {
                        continue;
                    }
                    if (!cells.contains(StructBp.key(nx, 0, nz))) {
                        continue;
                    }
                    any1 = new int[]{nx, nz};
                    for (int[] d2 : dd) {
                        if (reach.contains(StructBp.key(nx + d2[0], 0, nz + d2[1]))) {
                            best = new int[]{nx, nz};
                        }
                    }
                }
                int[] pick = best != null ? best : any1;
                if (pick != null) {
                    if (Boolean.getBoolean("linker.debug")) {
                        System.err.println("[Arq.repair] piso " + f + ": quita mueble en " + pick[0] + "," + pick[1] + " (" + b.get(pick[0], f, pick[1]) + ") por la casilla aislada " + u[0] + "," + u[1]);
                    }
                    for (int y = f; y <= f + 3; y++) {
                        String g = b.get(pick[0], y, pick[1]);
                        if (g != null && !wallLike(g)) {
                            b.set(pick[0], y, pick[1], "air");
                        }
                    }
                    removed++;
                    any = true;
                }
            }
            if (!any) {
                break;
            }
        }
        return removed;
    }

    // ------------------------------------------------------------------ comprobaciones

    /** Revisa solapes entre salas y salas aisladas de la entrada. Devuelve la lista de problemas (vacía = bien). */
    List<String> check() {
        List<String> out = new ArrayList<>(problems);
        for (int i = 0; i < all.size(); i++) {
            for (int j = i + 1; j < all.size(); j++) {
                Sp a = all.get(i);
                Sp c = all.get(j);
                if (isJoined(a, c)) {
                    continue;
                }
                for (int[] ba : a.boxes) {
                    for (int[] bc : c.boxes) {
                        int sx = Math.max(ba[0] - bc[3], bc[0] - ba[3]) - 1;
                        int sy = Math.max(ba[1] - bc[4], bc[1] - ba[4]) - 1;
                        int sz = Math.max(ba[2] - bc[5], bc[2] - ba[5]) - 1;
                        if (sx < 1 && sy < 1 && sz < 1) {
                            out.add("SE ENCIMAN/TOCAN: " + a + " con " + c);
                        }
                    }
                }
            }
        }
        if (entrance != null) {
            Set<Sp> seen = new LinkedHashSet<>();
            ArrayDeque<Sp> dq = new ArrayDeque<>();
            dq.add(entrance);
            seen.add(entrance);
            while (!dq.isEmpty()) {
                Sp u = dq.poll();
                for (Sp v : u.links) {
                    if (seen.add(v)) {
                        dq.add(v);
                    }
                }
            }
            for (Sp s : all) {
                if (!seen.contains(s)) {
                    out.add("SALA SIN CONEXION desde la entrada: " + s);
                }
            }
        }
        return out;
    }

    private boolean isJoined(Sp a, Sp c) {
        for (Sp[] j : joined) {
            if (j[0] == a && j[1] == c || j[0] == c && j[1] == a) {
                return true;
            }
        }
        return false;
    }
}
