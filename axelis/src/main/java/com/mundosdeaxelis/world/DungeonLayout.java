package com.mundosdeaxelis.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * El plano de una dungeon: una cuadrícula de salas (cada sala ocupa una celda de {@link #PITCH} bloques) que sale de una semilla
 * y del número de jugadores. Sin nada de Minecraft dentro, así se puede probar sola.
 *
 * <p>Forma: un camino principal que empieza en la antesala (fila 0, pegada a la arena del jefe) y se aleja hacia el sur con giros
 * a izquierda y derecha, hasta el claro de entrada. En la primera sala tras la antesala está el mini-jefe. A los lados del camino
 * salen salas opcionales sin salida: Cámara de la Llave, Cripta (cofre sellado), Prueba de saltos y Prueba de runas. Con más
 * jugadores el camino es más largo y hay más salas laterales.
 *
 * <p>Direcciones: 0 = norte (z menor, hacia la arena), 1 = este (x mayor), 2 = sur, 3 = oeste.
 */
public final class DungeonLayout {
    public static final int PITCH = 32;
    /** Mitad del interior de una sala (el interior mide 19). */
    public static final int HALF = 9;
    /** Centro (z) de la antesala. */
    public static final int ANTE_ZC = 45;
    public static final int MAX_COL = 2;
    private static final int[] DX = {0, 1, 0, -1};
    private static final int[] DZ = {-1, 0, 1, 0};

    public enum Type {START, COMBAT, ELITE, ANTE, KEY, CRYPT, PARKOUR, RUNES}

    public enum Theme {ROOTS, SPINES, NURSERY, POLLEN, RAIZAL}

    public static final class Cell {
        public final int id;
        public final int col;
        public final int row;
        public Type type;
        public Theme theme = Theme.ROOTS;
        /** Puertas abiertas hacia cada dirección (0..3). */
        public final boolean[] door = new boolean[4];
        /** Si es una sala sin salida: por dónde se entra. */
        public int entry = -1;
        /** Orden en el camino principal (0 = antesala), -1 si es lateral. */
        public int order = -1;
        /** Sala de un desvío (camino alterno que sale del camino principal y vuelve a unirse). */
        public boolean detour;
        public long seed;

        Cell(int id, int col, int row) {
            this.id = id;
            this.col = col;
            this.row = row;
        }

        public int cx() {
            return col * PITCH;
        }

        public int cz() {
            return ANTE_ZC + row * PITCH;
        }

        public boolean isCombat() {
            return type == Type.COMBAT || type == Type.ELITE || type == Type.KEY;
        }
    }

    public final long seed;
    public final int players;
    /** Dungeon grande (la de la Forja): 2 salas más en el camino y hasta 9 salas laterales. La del bosque no lo es. */
    public final boolean big;
    public final List<Cell> cells = new ArrayList<>();
    public final Map<Long, Cell> grid = new HashMap<>();
    public final List<Cell> path = new ArrayList<>();
    /** Cuántos desvíos tiene el plano (solo el modo grande). */
    public int detours = 0;
    private int attempt = 0;

    private DungeonLayout(long seed, int players, boolean big) {
        this.seed = seed;
        this.players = players;
        this.big = big;
    }

    private static long key(int col, int row) {
        return ((long) (col + 64) << 32) | (row + 64);
    }

    public Cell at(int col, int row) {
        return grid.get(key(col, row));
    }

    private Cell add(int col, int row) {
        Cell c = new Cell(cells.size(), col, row);
        cells.add(c);
        grid.put(key(col, row), c);
        return c;
    }

    private void link(Cell a, Cell b) {
        for (int d = 0; d < 4; d++) {
            if (a.col + DX[d] == b.col && a.row + DZ[d] == b.row) {
                a.door[d] = true;
                b.door[(d + 2) % 4] = true;
                return;
            }
        }
        throw new IllegalStateException("celdas no vecinas");
    }

    /** Dirección (0..3) de la puerta por la que se entra a la sala: la sala lateral por donde se llega, o la del camino que está más cerca de la entrada. -1 si no hay. */
    public int entryDir(Cell c) {
        if (c.entry >= 0) {
            return c.entry;
        }
        for (int d = 0; d < 4; d++) {
            if (c.door[d]) {
                Cell o = at(c.col + DX[d], c.row + DZ[d]);
                if (o != null && o.order == c.order + 1) {
                    return d;
                }
            }
        }
        return -1;
    }

    /** La sala vecina por la puerta d, o null si no hay puerta. */
    public Cell neighbor(Cell c, int d) {
        return c.door[d] ? at(c.col + DX[d], c.row + DZ[d]) : null;
    }

    public Cell ante() {
        return path.get(0);
    }

    public Cell start() {
        return path.get(path.size() - 1);
    }

    public static int dx(int d) {
        return DX[d];
    }

    public static int dz(int d) {
        return DZ[d];
    }

    /** Jugadores que se tienen en cuenta (1 a 6). */
    public static int clampPlayers(int n) {
        return Math.max(1, Math.min(6, n));
    }

    public int count(Type t) {
        int n = 0;
        for (Cell c : cells) {
            if (c.type == t) {
                n++;
            }
        }
        return n;
    }

    public static DungeonLayout generate(long seed, int playersIn) {
        return generate(seed, playersIn, false);
    }

    public static DungeonLayout generate(long seed, int playersIn, boolean big) {
        int n = clampPlayers(playersIn);
        for (int attempt = 0; attempt < 200; attempt++) {
            DungeonLayout l = new DungeonLayout(seed, n, big);
            l.attempt = attempt;
            if (l.tryBuild(new Random(seed * 1000003L + attempt * 7919L + n))) {
                return l;
            }
        }
        throw new IllegalStateException("no se pudo generar la dungeon");
    }

    private boolean tryBuild(Random r) {
        int len = (big ? 7 : 5) + players; // celdas del camino: antesala + mini-jefe + salas + claro
        Cell cur = add(0, 0);
        cur.type = Type.ANTE;
        cur.order = 0;
        path.add(cur);
        int lastDir = 2;
        for (int i = 1; i < len; i++) {
            List<Integer> opts = new ArrayList<>();
            // pesos: sur 5, este y oeste 2
            int[] w = {0, 2, 5, 2};
            for (int d = 1; d <= 3; d++) {
                if (i == 1 && d != 2) {
                    continue; // el primer paso desde la antesala va al sur
                }
                if ((d + 2) % 4 == lastDir) {
                    continue;
                }
                int nc = cur.col + DX[d];
                int nr = cur.row + DZ[d];
                if (Math.abs(nc) > MAX_COL || at(nc, nr) != null) {
                    continue;
                }
                // no pegarse a otra celda del camino por un lado (evita pasillos dobles)
                for (int k = 0; k < w[d]; k++) {
                    opts.add(d);
                }
            }
            if (opts.isEmpty()) {
                return false;
            }
            int d = opts.get(r.nextInt(opts.size()));
            Cell next = add(cur.col + DX[d], cur.row + DZ[d]);
            link(cur, next);
            next.order = i;
            path.add(next);
            cur = next;
            lastDir = d;
        }
        // tipos del camino
        start().type = Type.START;
        path.get(1).type = Type.ELITE;
        List<Theme> themes = new ArrayList<>(List.of(Theme.values()));
        Collections.shuffle(themes, r);
        int ti = 0;
        for (int i = 2; i < path.size() - 1; i++) {
            Cell c = path.get(i);
            c.type = Type.COMBAT;
            c.theme = themes.get(ti++ % themes.size());
        }
        path.get(1).theme = Theme.ROOTS;
        // desvíos: caminos alternos que salen de una sala del camino y vuelven a unirse más adelante
        List<Cell> attachExtra = new ArrayList<>();
        if (big) {
            int want = players >= 4 ? 2 : 1;
            List<int[]> used = new ArrayList<>();
            for (int f = 0; f < want; f++) {
                List<int[]> pairs = new ArrayList<>();
                for (int i = 2; i <= path.size() - 4; i++) {
                    for (int j = i + 2; j <= Math.min(i + 4, path.size() - 2); j++) {
                        boolean free = true;
                        for (int[] u : used) {
                            if (!(j < u[0] || i > u[1])) {
                                free = false;
                            }
                        }
                        if (free) {
                            pairs.add(new int[]{i, j});
                        }
                    }
                }
                Collections.shuffle(pairs, r);
                for (int[] pr : pairs) {
                    Cell a = path.get(pr[0]);
                    Cell b = path.get(pr[1]);
                    int len2 = Math.max(1, Math.min(3, pr[1] - pr[0] - 1 + r.nextInt(3) - 1));
                    List<int[]> chain = new ArrayList<>();
                    if (!chain(a, b, len2, r, chain)) {
                        continue;
                    }
                    Cell prev = a;
                    for (int k = 0; k < chain.size(); k++) {
                        Cell dc = add(chain.get(k)[0], chain.get(k)[1]);
                        dc.type = Type.COMBAT;
                        dc.theme = themes.get(ti++ % themes.size());
                        dc.detour = true;
                        dc.order = Math.round(a.order + (b.order - a.order) * (k + 1) / (chain.size() + 1.0F));
                        link(prev, dc);
                        attachExtra.add(dc);
                        prev = dc;
                    }
                    link(prev, b);
                    used.add(new int[]{pr[0], pr[1]});
                    detours++;
                    break;
                }
            }
        }
        // salas laterales
        List<Type> sides = new ArrayList<>();
        Type[] seq = {Type.KEY, Type.CRYPT, Type.PARKOUR, Type.RUNES, Type.KEY, Type.CRYPT, Type.PARKOUR, Type.RUNES, Type.KEY};
        // siempre al menos 4 salas laterales: llave, cripta y las dos pruebas (saltos y runas); con 1 jugador antes salía solo una de las dos
        int want = big ? Math.min(9, 3 + players) : Math.min(7, Math.max(4, 2 + players));
        for (int i = 0; i < want; i++) {
            sides.add(seq[i]);
        }
        // si salen las dos pruebas en orden fijo, mezclar cuál va primero
        if (want >= 3 && r.nextBoolean()) {
            sides.set(2, Type.RUNES);
            if (want >= 4) {
                sides.set(3, Type.PARKOUR);
            }
        }
        List<Cell> attach = new ArrayList<>();
        for (int i = 2; i < path.size() - 1; i++) {
            attach.add(path.get(i));
        }
        attach.add(path.get(1));
        attach.addAll(attachExtra);
        for (Type t : sides) {
            List<int[]> cands = new ArrayList<>();
            for (Cell a : attach) {
                for (int d = 0; d < 4; d++) {
                    int nc = a.col + DX[d];
                    int nr = a.row + DZ[d];
                    if (Math.abs(nc) > MAX_COL || nr < 0 || at(nc, nr) != null) {
                        continue;
                    }
                    if (a.door[d]) {
                        continue;
                    }
                    int weight = (d == 1 || d == 3) ? 4 : 1;
                    for (int k = 0; k < weight; k++) {
                        cands.add(new int[]{a.id, d});
                    }
                }
            }
            if (cands.isEmpty()) {
                break;
            }
            int[] pick = cands.get(r.nextInt(cands.size()));
            Cell a = cells.get(pick[0]);
            Cell side = add(a.col + DX[pick[1]], a.row + DZ[pick[1]]);
            side.type = t;
            side.entry = (pick[1] + 2) % 4;
            link(a, side);
        }
        for (Cell c : cells) {
            c.seed = seed * 31L + c.id * 1_000_003L;
        }
        return validate();
    }

    /** Busca {@code len} celdas libres seguidas que salgan de {@code a} y acaben junto a {@code b} (un desvío). */
    private boolean chain(Cell a, Cell b, int len, Random r, List<int[]> out) {
        int col = out.isEmpty() ? a.col : out.get(out.size() - 1)[0];
        int row = out.isEmpty() ? a.row : out.get(out.size() - 1)[1];
        List<Integer> dirs = new ArrayList<>(List.of(0, 1, 2, 3));
        Collections.shuffle(dirs, r);
        for (int d : dirs) {
            int nc = col + DX[d];
            int nr = row + DZ[d];
            if (Math.abs(nc) > MAX_COL || nr < 1 || at(nc, nr) != null) {
                continue;
            }
            boolean dup = false;
            for (int[] o : out) {
                if (o[0] == nc && o[1] == nr) {
                    dup = true;
                }
            }
            if (dup) {
                continue;
            }
            out.add(new int[]{nc, nr});
            if (out.size() == len) {
                if (Math.abs(nc - b.col) + Math.abs(nr - b.row) == 1) {
                    return true;
                }
            } else if (chain(a, b, len, r, out)) {
                return true;
            }
            out.remove(out.size() - 1);
        }
        return false;
    }

    /** ¿Se pueden despejar todas las salas de combate empezando por la entrada? (una sala se puede abrir si da a la entrada o a una sala ya despejada) */
    public boolean completable() {
        boolean[] cleared = new boolean[cells.size()];
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Cell c : cells) {
                if (!c.isCombat() || cleared[c.id]) {
                    continue;
                }
                for (int d = 0; d < 4; d++) {
                    Cell x = neighbor(c, d);
                    if (x != null && (x.type == Type.START || (x.isCombat() && cleared[x.id]))) {
                        cleared[c.id] = true;
                        changed = true;
                        break;
                    }
                }
            }
        }
        for (Cell c : cells) {
            if (c.isCombat() && !cleared[c.id]) {
                return false;
            }
        }
        return true;
    }

    private boolean validate() {
        if (big && detours == 0 && attempt < 150) {
            return false;
        }
        if (!completable()) {
            return false;
        }
        // cada llave abre cofres: salas de llave >= criptas
        if (count(Type.KEY) < count(Type.CRYPT)) {
            return false;
        }
        // no hay celdas repetidas y todas están conectadas
        if (grid.size() != cells.size()) {
            return false;
        }
        boolean[] seen = new boolean[cells.size()];
        List<Cell> q = new ArrayList<>();
        q.add(cells.get(0));
        seen[0] = true;
        for (int i = 0; i < q.size(); i++) {
            Cell c = q.get(i);
            for (int d = 0; d < 4; d++) {
                if (c.door[d]) {
                    Cell o = at(c.col + DX[d], c.row + DZ[d]);
                    if (o == null) {
                        return false;
                    }
                    if (!seen[o.id]) {
                        seen[o.id] = true;
                        q.add(o);
                    }
                }
            }
        }
        return q.size() == cells.size() && !ante().door[0];
    }

    /** Resumen en una línea (para el comando de pruebas). */
    public String summary() {
        int combat = 0;
        for (Cell c : cells) {
            if (c.type == Type.COMBAT) {
                combat++;
            }
        }
        return "semilla " + seed + ", " + players + " jug.: " + cells.size() + " salas (" + combat + " de combate, " + detours + " desvíos, mini-jefe, "
                + count(Type.KEY) + " llave, " + count(Type.CRYPT) + " cripta, " + count(Type.PARKOUR) + " saltos, " + count(Type.RUNES) + " runas)";
    }

    /** Dibujo en texto del plano (para pruebas). */
    public String ascii() {
        int minC = 0, maxC = 0, maxR = 0;
        for (Cell c : cells) {
            minC = Math.min(minC, c.col);
            maxC = Math.max(maxC, c.col);
            maxR = Math.max(maxR, c.row);
        }
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row <= maxR; row++) {
            for (int col = minC; col <= maxC; col++) {
                Cell c = at(col, row);
                sb.append(c == null ? " .  " : "[" + sym(c) + "]" + (c.door[1] ? "-" : " "));
            }
            sb.append('\n');
            for (int col = minC; col <= maxC; col++) {
                Cell c = at(col, row);
                sb.append(c != null && c.door[2] ? " |  " : "    ");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static char sym(Cell c) {
        return switch (c.type) {
            case START -> 'E';
            case COMBAT -> (char) ('1' + c.theme.ordinal());
            case ELITE -> 'M';
            case ANTE -> 'A';
            case KEY -> 'L';
            case CRYPT -> 'C';
            case PARKOUR -> 'P';
            case RUNES -> 'R';
        };
    }
}
