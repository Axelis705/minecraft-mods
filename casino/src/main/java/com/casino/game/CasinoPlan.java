package com.casino.game;

import java.util.ArrayList;
import java.util.List;

/**
 * El plano del edificio del casino, sin nada de Minecraft (así se puede probar solo: que nada se pise y que se pueda caminar a todo).
 * Marco local: x de 0 a W-1, z de 0 a D-1 (z grande = lado de la entrada, que mira al sur), y = 0 es el piso y 1 es el primer bloque de aire.
 * Las direcciones van de 0 a 3 en el sentido del reloj: 0 norte (-z), 1 este (+x), 2 sur (+z), 3 oeste (-x).
 * Las máquinas miran hacia donde está el jugador (su "frente"), igual que al ponerlas a mano.
 */
public final class CasinoPlan {
    public static final int W = 33;
    /** Edificio (33) + patio de entrada (4). */
    public static final int D = 37;
    public static final int BUILDING_Z = 33;
    /** Altura del techo y del cartel. */
    public static final int ROOF_Y = 8;
    public static final int TOP_Y = 16;
    public static final int DOOR_X0 = 14;
    public static final int DOOR_X1 = 18;

    public enum Mat { AIR, FLOOR_A, FLOOR_B, PATH, CARPET, WALL, TRIM, GLASS, ROOF, LAMP, LETTER, SIGN_BG, LANTERN }

    public enum Mach { BLACKJACK, POKER, ROULETTE, SLOT, SCRATCH, CASHIER, WHEEL, PLINKO }

    public interface Sink {
        void mat(int x, int y, int z, Mat m);

        /** (x, y, z) es el bloque principal de la máquina y dir hacia donde mira su frente. */
        void machine(Mach k, int x, int y, int z, int dir);
    }

    private CasinoPlan() {
    }

    public static final int[] DX = {0, 1, 0, -1};
    public static final int[] DZ = {-1, 0, 1, 0};

    /** La derecha de quien mira de frente a la máquina (el giro contrario al reloj, como en los bloques). */
    public static int right(int dir) {
        return (dir + 3) & 3;
    }

    /** Un bloque de una máquina: posición y los dos números que lo describen (celda / mitad / px,py). */
    public record Cell(int x, int y, int z, int a, int b) {
    }

    public static List<Cell> cells(Mach k, int mx, int my, int mz, int dir) {
        List<Cell> out = new ArrayList<>();
        int r = right(dir);
        switch (k) {
            case BLACKJACK, POKER -> {
                for (int i = 0; i < BlackjackLayout.CELLS.length; i++) {
                    int[] c = BlackjackLayout.CELLS[i];
                    out.add(new Cell(mx + DX[r] * c[0] + DX[dir] * c[1], my, mz + DZ[r] * c[0] + DZ[dir] * c[1], i, 0));
                }
            }
            case ROULETTE -> {
                for (int i = 0; i < RouletteLayout.CELLS.length; i++) {
                    int[] c = RouletteLayout.CELLS[i];
                    out.add(new Cell(mx + DX[r] * c[0] + DX[dir] * c[1], my, mz + DZ[r] * c[0] + DZ[dir] * c[1], i, 0));
                }
            }
            case SLOT, SCRATCH -> {
                out.add(new Cell(mx, my, mz, 0, 0));
                out.add(new Cell(mx, my + 1, mz, 1, 0));
            }
            case CASHIER -> out.add(new Cell(mx, my, mz, 0, 0));
            case WHEEL, PLINKO -> {
                for (int px = 0; px < 3; px++) {
                    for (int py = 0; py < 4; py++) {
                        out.add(new Cell(mx + DX[r] * (px - 1), my + py, mz + DZ[r] * (px - 1), px, py));
                    }
                }
            }
        }
        return out;
    }

    /** Donde se para el crupier (null si la máquina no tiene). */
    public static int[] dealerSpot(Mach k, int mx, int mz, int dir) {
        int r = right(dir);
        return switch (k) {
            case BLACKJACK, POKER -> new int[]{mx - DX[dir], mz - DZ[dir]};
            case ROULETTE -> new int[]{mx + DX[r] * RouletteLayout.DEALER_COL + DX[dir] * RouletteLayout.DEALER_ROW,
                    mz + DZ[r] * RouletteLayout.DEALER_COL + DZ[dir] * RouletteLayout.DEALER_ROW};
            default -> null;
        };
    }

    /** El bloque de piso por donde se llega a la máquina (el de enfrente de su lado de juego). */
    public static int[] frontCell(Mach k, int mx, int mz, int dir) {
        int depth = switch (k) {
            case BLACKJACK, POKER -> 3;
            case ROULETTE -> 2;
            default -> 1;
        };
        return new int[]{mx + DX[dir] * depth, mz + DZ[dir] * depth};
    }

    /** Las máquinas del casino: tipo, bloque principal (x, z) y dirección del frente. */
    public static final Object[][] MACHINES = buildMachines();

    private static Object[][] buildMachines() {
        List<Object[]> l = new ArrayList<>();
        l.add(new Object[]{Mach.WHEEL, 16, 1, 2});
        l.add(new Object[]{Mach.PLINKO, 7, 1, 2});
        l.add(new Object[]{Mach.PLINKO, 25, 1, 2});
        for (int z = 5; z <= 15; z += 2) {
            l.add(new Object[]{Mach.SLOT, 1, z, 1});
            l.add(new Object[]{Mach.SLOT, 31, z, 3});
        }
        l.add(new Object[]{Mach.ROULETTE, 10, 13, 2});
        l.add(new Object[]{Mach.BLACKJACK, 23, 13, 2});
        l.add(new Object[]{Mach.POKER, 10, 23, 2});
        l.add(new Object[]{Mach.BLACKJACK, 22, 23, 2});
        l.add(new Object[]{Mach.CASHIER, 11, 31, 0});
        l.add(new Object[]{Mach.CASHIER, 21, 31, 0});
        l.add(new Object[]{Mach.SCRATCH, 6, 31, 0});
        l.add(new Object[]{Mach.SCRATCH, 26, 31, 0});
        return l.toArray(new Object[0][]);
    }

    private static final String[][] FONT = {
            {".XX.", "X..X", "X...", "X...", "X...", "X..X", ".XX."}, // C
            {".XX.", "X..X", "X..X", "XXXX", "X..X", "X..X", "X..X"}, // A
            {".XXX", "X...", "X...", ".XX.", "...X", "...X", "XXX."}, // S
            {"XXXX", ".XX.", ".XX.", ".XX.", ".XX.", ".XX.", "XXXX"}, // I
            {"X..X", "XX.X", "XX.X", "X.XX", "X.XX", "X..X", "X..X"}, // N
            {".XX.", "X..X", "X..X", "X..X", "X..X", "X..X", ".XX."}, // O
    };

    /** Genera todo el edificio (paredes, techo, piso, cartel, puerta y máquinas). Los huecos de aire sobre el piso los limpia quien lo coloca. */
    public static void build(Sink s) {
        int zf = BUILDING_Z - 1; // pared de la fachada (z = 32)
        // piso: tablero de ajedrez negro y rojo dentro, camino afuera
        for (int x = 0; x < W; x++) {
            for (int z = 0; z < D; z++) {
                boolean inside = z <= zf;
                s.mat(x, 0, z, inside ? (((x + z) & 1) == 0 ? Mat.FLOOR_A : Mat.FLOOR_B) : Mat.PATH);
            }
        }
        // alfombra roja desde el patio hasta la rueda
        for (int x = 15; x <= 17; x++) {
            for (int z = 5; z < D; z++) {
                s.mat(x, 1, z, Mat.CARPET);
            }
        }
        // paredes y techo
        for (int x = 0; x < W; x++) {
            for (int z = 0; z <= zf; z++) {
                boolean edge = x == 0 || x == W - 1 || z == 0 || z == zf;
                if (edge) {
                    for (int y = 1; y < ROOF_Y; y++) {
                        s.mat(x, y, z, wallMat(x, y, z, zf));
                    }
                    s.mat(x, ROOF_Y, z, Mat.TRIM);
                } else {
                    boolean lamp = x % 4 == 2 && z % 4 == 2;
                    s.mat(x, ROOF_Y, z, lamp ? Mat.LAMP : Mat.ROOF);
                }
            }
        }
        // puerta: 5 de ancho y 5 de alto, con marco dorado
        for (int x = DOOR_X0; x <= DOOR_X1; x++) {
            for (int y = 1; y <= 5; y++) {
                s.mat(x, y, zf, Mat.AIR);
            }
            s.mat(x, 6, zf, Mat.TRIM);
        }
        for (int y = 1; y <= 6; y++) {
            s.mat(DOOR_X0 - 1, y, zf, Mat.TRIM);
            s.mat(DOOR_X1 + 1, y, zf, Mat.TRIM);
        }
        // cartel sobre la fachada: panel oscuro con borde dorado y las letras CASINO
        for (int x = 0; x < W; x++) {
            for (int y = ROOF_Y + 1; y <= TOP_Y; y++) {
                boolean border = x == 0 || x == W - 1 || y == TOP_Y;
                s.mat(x, y, zf, border ? Mat.TRIM : Mat.SIGN_BG);
            }
        }
        int x0 = (W - 29) / 2;
        for (int i = 0; i < FONT.length; i++) {
            for (int row = 0; row < 7; row++) {
                for (int col = 0; col < 4; col++) {
                    if (FONT[i][row].charAt(col) == 'X') {
                        s.mat(x0 + i * 5 + col, TOP_Y - 1 - row, zf, Mat.LETTER);
                    }
                }
            }
        }
        // faroles en el patio
        for (int x : new int[]{12, 20}) {
            for (int y = 1; y <= 3; y++) {
                s.mat(x, y, 35, Mat.TRIM);
            }
            s.mat(x, 4, 35, Mat.LANTERN);
        }
        for (Object[] m : MACHINES) {
            s.machine((Mach) m[0], (int) m[1], 1, (int) m[2], (int) m[3]);
        }
    }

    private static Mat wallMat(int x, int y, int z, int zf) {
        boolean xWall = x == 0 || x == W - 1;
        int i = xWall ? z : x;
        if (i % 8 == 0 || (xWall && (z == 0 || z == zf)) || (!xWall && (x == 0 || x == W - 1))) {
            return Mat.TRIM;
        }
        boolean frontWall = z == zf;
        boolean doorZone = frontWall && i >= DOOR_X0 - 1 && i <= DOOR_X1 + 1;
        if (y >= 3 && y <= 5 && i % 8 >= 2 && i % 8 <= 6 && !doorZone) {
            return Mat.GLASS;
        }
        return Mat.WALL;
    }
}
