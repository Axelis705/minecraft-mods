import com.casino.game.BlackjackLayout;
import com.casino.game.CasinoPlan;
import com.casino.game.CasinoPlan.Cell;
import com.casino.game.CasinoPlan.Mach;
import com.casino.game.CasinoPlan.Mat;

import java.util.*;

/** Pruebas del plano del casino: que todo cabe, nada se pisa y se puede caminar desde la puerta hasta cada máquina (sin Minecraft). */
public class CasinoPlanTest {
    static int fails = 0;

    static void check(boolean ok, String what) {
        if (!ok) {
            fails++;
            if (fails < 40) {
                System.out.println("FALLA: " + what);
            }
        }
    }

    record Mc(Mach k, int x, int y, int z, int dir) {
    }

    public static void main(String[] args) {
        Map<String, Mat> mats = new HashMap<>();
        List<Mc> machines = new ArrayList<>();
        CasinoPlan.build(new CasinoPlan.Sink() {
            public void mat(int x, int y, int z, Mat m) {
                check(x >= 0 && x < CasinoPlan.W && z >= 0 && z < CasinoPlan.D && y >= 0 && y <= CasinoPlan.TOP_Y, "bloque fuera del plano " + x + "," + y + "," + z);
                mats.put(x + "," + y + "," + z, m);
            }

            public void machine(Mach k, int x, int y, int z, int dir) {
                machines.add(new Mc(k, x, y, z, dir));
            }
        });
        int tables = 0, slots = 0, cashiers = 0, scratch = 0, wheels = 0, plinkos = 0;
        for (Mc m : machines) {
            switch (m.k) {
                case BLACKJACK, POKER, ROULETTE -> tables++;
                case SLOT -> slots++;
                case CASHIER -> cashiers++;
                case SCRATCH -> scratch++;
                case WHEEL -> wheels++;
                case PLINKO -> plinkos++;
            }
        }
        check(tables == 4 && slots == 12 && cashiers == 2 && scratch == 2 && wheels == 1 && plinkos == 2, "faltan maquinas: " + machines.size());

        // ocupacion de cada bloque por las maquinas
        Map<String, Mach> occ = new HashMap<>();
        Set<String> dealers = new HashSet<>();
        for (Mc m : machines) {
            for (Cell c : CasinoPlan.cells(m.k, m.x, m.y, m.z, m.dir)) {
                check(c.x() >= 1 && c.x() <= CasinoPlan.W - 2 && c.z() >= 1 && c.z() <= CasinoPlan.BUILDING_Z - 2, m.k + " en " + m.x + "," + m.z + " se sale del salon: " + c);
                check(c.y() >= 1 && c.y() < CasinoPlan.ROOF_Y, "altura valida");
                String key = c.x() + "," + c.y() + "," + c.z();
                check(!occ.containsKey(key), m.k + " en " + m.x + "," + m.z + " se pisa con " + occ.get(key) + " en " + key);
                occ.put(key, m.k);
                Mat under = mats.get(c.x() + "," + c.y() + "," + c.z());
                check(under == null || under == Mat.CARPET || under == Mat.AIR, "la maquina cae sobre un bloque solido " + under);
            }
            int[] d = CasinoPlan.dealerSpot(m.k, m.x, m.z, m.dir);
            if (d != null) {
                check(d[0] >= 1 && d[0] <= CasinoPlan.W - 2 && d[1] >= 1 && d[1] <= CasinoPlan.BUILDING_Z - 2, "crupier fuera del salon");
                dealers.add(d[0] + "," + d[1]);
            }
        }
        for (String d : dealers) {
            String[] p = d.split(",");
            check(!occ.containsKey(p[0] + ",1," + p[1]) && !occ.containsKey(p[0] + ",2," + p[1]), "el crupier en " + d + " esta dentro de otra maquina");
        }
        check(dealers.size() == 4, "cuatro crupieres");

        // caminar: BFS desde el patio
        boolean[][] free = new boolean[CasinoPlan.W][CasinoPlan.D];
        for (int x = 0; x < CasinoPlan.W; x++) {
            for (int z = 0; z < CasinoPlan.D; z++) {
                Mat m1 = mats.get(x + ",1," + z);
                boolean solid = m1 != null && m1 != Mat.CARPET && m1 != Mat.AIR;
                free[x][z] = !solid && !occ.containsKey(x + ",1," + z) && !dealers.contains(x + "," + z);
            }
        }
        boolean[][] seen = new boolean[CasinoPlan.W][CasinoPlan.D];
        ArrayDeque<int[]> q = new ArrayDeque<>();
        q.add(new int[]{16, CasinoPlan.D - 1});
        seen[16][CasinoPlan.D - 1] = true;
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int d = 0; d < 4; d++) {
                int nx = c[0] + CasinoPlan.DX[d];
                int nz = c[1] + CasinoPlan.DZ[d];
                if (nx >= 0 && nx < CasinoPlan.W && nz >= 0 && nz < CasinoPlan.D && free[nx][nz] && !seen[nx][nz]) {
                    seen[nx][nz] = true;
                    q.add(new int[]{nx, nz});
                }
            }
        }
        check(free[16][CasinoPlan.BUILDING_Z - 1], "la puerta esta libre");
        for (Mc m : machines) {
            int[] f = CasinoPlan.frontCell(m.k, m.x, m.z, m.dir);
            check(f[0] >= 0 && f[0] < CasinoPlan.W && seen[f[0]][f[1]], m.k + " en " + m.x + "," + m.z + ": no se puede llegar a su frente " + f[0] + "," + f[1]);
            if (m.k == Mach.BLACKJACK || m.k == Mach.POKER) {
                int r = CasinoPlan.right(m.dir);
                for (int i = 0; i < BlackjackLayout.SEATS; i++) {
                    double[] p = BlackjackLayout.polar(3.2, BlackjackLayout.seatAngle(i));
                    int u = (int) Math.round(p[0]);
                    int w = (int) Math.round(p[1]);
                    int cx = m.x + CasinoPlan.DX[r] * u + CasinoPlan.DX[m.dir] * w;
                    int cz = m.z + CasinoPlan.DZ[r] * u + CasinoPlan.DZ[m.dir] * w;
                    check(seen[cx][cz], m.k + " en " + m.x + "," + m.z + ": nadie puede pararse en el asiento " + i + " (" + cx + "," + cz + ")");
                }
            }
            if (m.k == Mach.ROULETTE) {
                int r = CasinoPlan.right(m.dir);
                for (int col = -3; col <= 3; col++) {
                    int cx = m.x + CasinoPlan.DX[r] * col + CasinoPlan.DX[m.dir] * 2;
                    int cz = m.z + CasinoPlan.DZ[r] * col + CasinoPlan.DZ[m.dir] * 2;
                    check(seen[cx][cz], "ruleta: no se puede llegar a la fila de jugadores " + cx + "," + cz);
                }
            }
        }
        // la puerta atraviesa la pared y el techo tapa todo el salon
        for (int x = 1; x < CasinoPlan.W - 1; x++) {
            for (int z = 1; z < CasinoPlan.BUILDING_Z - 1; z++) {
                Mat r = mats.get(x + "," + CasinoPlan.ROOF_Y + "," + z);
                check(r == Mat.ROOF || r == Mat.LAMP, "techo en " + x + "," + z);
            }
        }
        for (int y = 1; y <= 5; y++) {
            check(mats.get("16," + y + ",32") == Mat.AIR, "puerta abierta en y=" + y);
        }
        int lamps = 0;
        for (Mat m : mats.values()) {
            if (m == Mat.LAMP) {
                lamps++;
            }
        }
        check(lamps > 40, "hay lamparas en el techo (" + lamps + ")");

        // dibujo del plano
        StringBuilder sb = new StringBuilder();
        for (int z = 0; z < CasinoPlan.D; z++) {
            for (int x = 0; x < CasinoPlan.W; x++) {
                char ch = '.';
                String k = x + ",1," + z;
                Mat m1 = mats.get(k);
                if (occ.containsKey(k)) {
                    ch = switch (occ.get(k)) {
                        case BLACKJACK -> 'B';
                        case POKER -> 'P';
                        case ROULETTE -> 'R';
                        case SLOT -> 's';
                        case SCRATCH -> 'x';
                        case CASHIER -> '$';
                        case WHEEL -> 'W';
                        case PLINKO -> 'L';
                    };
                } else if (dealers.contains(x + "," + z)) {
                    ch = 'D';
                } else if (m1 == Mat.CARPET) {
                    ch = '=';
                } else if (m1 == Mat.TRIM) {
                    ch = '#';
                } else if (m1 == Mat.WALL || m1 == Mat.GLASS) {
                    ch = m1 == Mat.GLASS ? ':' : '#';
                } else if (z >= CasinoPlan.BUILDING_Z) {
                    ch = ' ';
                } else if (!seen[x][z]) {
                    ch = '?';
                }
                sb.append(ch);
            }
            sb.append('\n');
        }
        System.out.print(sb);
        System.out.println(fails == 0 ? "CasinoPlanTest: TODO BIEN" : "CasinoPlanTest: " + fails + " FALLAS");
        if (fails > 0) {
            System.exit(1);
        }
    }
}
