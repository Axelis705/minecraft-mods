package com.mundosdeaxelis.world;

import static com.mundosdeaxelis.world.StructKit.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Torre del Hechicero: una torre arcana de piedra oscura y púrpura, en forma de octógono que se estrecha hacia arriba.
 * Tiene sótano (cripta), ocho pisos y un observatorio con cúpula de cristal. Todo el edificio gira alrededor de una escalera de caracol
 * central: cada piso se llega por una puerta de la escalera. Las salas se planifican con {@link Arq}, que une cada una con puertas
 * y avisa si alguna quedara aislada. Se entra por z = 0 (avenida, plataforma escalonada y gran portón).
 */
final class DesignTorre {
    private DesignTorre() {
    }

    // ------------------------------------------------------------------ paleta
    private static final String DB = "deepslate_bricks";
    private static final String DBS = "deepslate_brick_stairs";
    private static final String DBL = "deepslate_brick_slab";
    private static final String DBW = "deepslate_brick_wall";
    private static final String CR = "cracked_deepslate_bricks";
    private static final String DT = "deepslate_tiles";
    private static final String PD = "polished_deepslate";
    private static final String PDL = "polished_deepslate_slab";
    private static final String CD = "chiseled_deepslate";
    private static final String PP = "purpur_pillar[axis=y]";
    private static final String PPS = "purpur_stairs";
    private static final String AM = "amethyst_block";
    private static final String GP = "purple_stained_glass_pane";
    private static final String SL = "soul_lantern";
    private static final String CHERRY = "cherry_leaves[persistent=true]";

    /** z del centro de la torre. */
    static final int CZ = 34;
    /** Radio (a la cara plana) de cada piso, de -1 (cripta) a 8 (observatorio). */
    private static final int[] RAD = {12, 12, 12, 11, 11, 10, 10, 9, 9, 8};

    static int fy(int i) {
        return 3 + 8 * i;
    }

    static int rad(int i) {
        return RAD[Math.max(-1, Math.min(8, i)) + 1];
    }

    /** Suma máxima |x|+|z| del octógono de radio r. */
    static int sum(int r) {
        return (int) Math.round(1.4142 * r);
    }

    static boolean inO(int x, int dz, int r) {
        int ax = Math.abs(x), az = Math.abs(dz);
        return ax <= r && az <= r && ax + az <= sum(r);
    }

    static boolean inI(int x, int dz, int r) {
        int ax = Math.abs(x), az = Math.abs(dz);
        return ax <= r - 2 && az <= r - 2 && ax + az <= sum(r) - 3;
    }

    static StructBp make() {
        StructBp b = new StructBp(7117);
        Arq a = new Arq(b);
        a.frameStair = PPS;
        a.frameTrim = PD;
        b.foundation(-27, 7, 27, 61, "dirt");
        site(b);
        mass(b);
        Arq.Sp core = a.spiral("nucleo", 0, CZ, fy(-1), 9, PPS, PP, PD, "-z", true);
        Arq.Sp[][] rooms = new Arq.Sp[9][];
        for (int i = -1; i <= 7; i++) {
            rooms[i + 1] = rooms(b, a, core, i);
        }
        Arq.Sp obs = observatory(b, a, core);
        Arq.Sp out = a.ghost("exterior", -6, 12, 6, 21, fy(0), 7);
        a.entrance = out;
        Arq.Sp balcon = a.ghost("balcon", -6, 19, 6, 21, fy(1), 6);
        exterior(b, a);
        a.door(out, rooms[1][0], fy(0), 0, 5, 6, true);
        a.door(balcon, rooms[2][0], fy(1), 0, 3, 3, true);
        for (int i = -1; i <= 7; i++) {
            dress(b, a, rooms[i + 1], i);
        }
        shaftDecor(b);
        topOfDome(b);
        a.seed(0, fy(8), CZ - 3);
        for (int i = -1; i <= 8; i++) {
            a.repair(fy(i));
        }
        for (String p : a.check()) {
            System.err.println("[Torre] " + p);
        }
        return b;
    }

    // ================================================================== entorno

    private static void site(StructBp b) {
        b.box(-40, -1, -8, 40, -1, 72, "grass_block");
        // plataforma escalonada: tres niveles
        b.disc(0, CZ, 26, 0, 0, DT);
        b.disc(0, CZ, 22, 0, 1, DB);
        b.disc(0, CZ, 18, 0, 2, DB);
        for (int x = -27; x <= 27; x++) {
            for (int z = CZ - 27; z <= CZ + 27; z++) {
                double d = Math.sqrt(x * x + (z - CZ) * (z - CZ));
                if (d <= 26 && d > 22) {
                    b.set(x, 0, z, d > 25 ? PD : (((x + z) & 1) == 0 ? DT : PD));
                } else if (d <= 22 && d > 18) {
                    b.set(x, 1, z, "grass_block");
                } else if (d <= 18) {
                    String m = d > 16.4 && d < 17.6 ? PD : (d > 14.2 && d < 15.2 ? AM : (((x + z) & 1) == 0 ? DT : DB));
                    b.set(x, 2, z, m);
                }
            }
        }
        // jardín con setos y flores
        for (int x = -23; x <= 23; x++) {
            for (int z = CZ - 23; z <= CZ + 23; z++) {
                double d = Math.sqrt(x * x + (z - CZ) * (z - CZ));
                if (d <= 22 && d > 18 && !(Math.abs(x) <= 3 && z < CZ)) {
                    if (d > 21 && b.chance(0.8)) {
                        b.set(x, 2, z, "flowering_azalea_leaves[persistent=true]");
                    } else if (b.chance(0.5)) {
                        b.set(x, 2, z, b.pick("allium", "cornflower", "azure_bluet", "pink_tulip", "lily_of_the_valley", "grass", "fern"));
                    }
                }
            }
        }
        // avenida: dos tramos de suelo y los escalones de la plataforma
        for (int z = -8; z <= 21; z++) {
            int top = z < 8 ? -1 : (z < 12 ? 0 : (z < 16 ? 1 : 2));
            for (int x = -2; x <= 2; x++) {
                b.set(x, top, z, Math.abs(x) == 2 || x == 0 ? PD : DT);
                b.set(x, top + 1, z, "air");
            }
        }
        // farolas
        for (int z : new int[]{-6, -3, 5}) {
            for (int s = -1; s <= 1; s += 2) {
                int x = 3 * s;
                b.set(x, -1, z, PD);
                b.set(x, 0, z, DBW);
                b.set(x, 1, z, DBW);
                b.set(x, 2, z, PD);
                b.set(x, 3, z, SL + "[hanging=false]");
            }
        }
        portal(b);
        obelisks(b);
        bigTree(b, -32, 20, 13, "cherry_log", new String[]{CHERRY}, 6);
        bigTree(b, 31, 20, 14, "cherry_log", new String[]{CHERRY}, 6);
        bigTree(b, -34, 44, 12, "cherry_log", new String[]{CHERRY}, 6);
        bigTree(b, 33, 46, 13, "cherry_log", new String[]{CHERRY}, 6);
        groundCover(b, -38, -6, 38, 70, -1, 0.04, "grass", "allium", "cornflower", "pink_tulip");
        // el camino queda limpio de flores
        for (int z = -8; z <= 21; z++) {
            int top = z < 8 ? -1 : (z < 12 ? 0 : (z < 16 ? 1 : 2));
            for (int x = -2; x <= 2; x++) {
                b.set(x, top + 1, z, "air");
            }
        }
    }

    /** Portal de la avenida: dos pilares y un dintel con una linterna. */
    private static void portal(StructBp b) {
        for (int s = -1; s <= 1; s += 2) {
            int x0 = s < 0 ? -6 : 4;
            b.box(x0, -1, 0, x0 + 2, 1, 2, PD);
            b.box(x0, 2, 0, x0 + 2, 9, 2, PP);
            b.box(x0, 2, 0, x0 + 2, 2, 2, DB);
            b.box(x0, 10, 0, x0 + 2, 10, 2, CD);
            b.set(x0 + 1, 11, 1, AM);
            b.set(x0 + 1, 12, 1, "amethyst_cluster[facing=up]");
        }
        b.box(-3, 10, 0, 3, 10, 2, DB);
        b.box(-3, 11, 0, 3, 11, 2, PD);
        for (int z = 0; z <= 2; z++) {
            b.set(-3, 9, z, sti(PPS, "-x"));
            b.set(3, 9, z, sti(PPS, "+x"));
        }
        hangLantern(b, 0, 9, 1, 2, SL);
        b.set(0, 12, 1, AM);
        b.set(0, 13, 1, "amethyst_cluster[facing=up]");
    }

    /** Cuatro agujas en las diagonales de la plataforma. */
    private static void obelisks(StructBp b) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = 17 * sx;
                int cz = CZ + 17 * sz;
                b.box(cx - 1, 0, cz - 1, cx + 1, 2, cz + 1, PD);
                b.box(cx - 1, 3, cz - 1, cx + 1, 3, cz + 1, CD);
                b.box(cx - 1, 4, cz - 1, cx + 1, 6, cz + 1, PP);
                b.box(cx - 1, 7, cz - 1, cx + 1, 7, cz + 1, CD);
                b.box(cx, 8, cz, cx, 17, cz, PP);
                for (int y = 9; y <= 17; y += 4) {
                    b.set(cx, y, cz, AM);
                }
                b.set(cx, 18, cz, AM);
                b.set(cx, 19, cz, "amethyst_cluster[facing=up]");
                b.set(cx + 1, 8, cz, SL + "[hanging=false]");
                b.set(cx - 1, 8, cz, SL + "[hanging=false]");
                b.set(cx, 8, cz + 1, SL + "[hanging=false]");
                b.set(cx, 8, cz - 1, SL + "[hanging=false]");
            }
        }
    }

    // ================================================================== cuerpo de la torre

    private static String wallMat(StructBp b) {
        int n = b.r.nextInt(100);
        return n < 70 ? DB : (n < 82 ? CR : (n < 92 ? PD : DT));
    }

    private static void octFill(StructBp b, int r, int y0, int y1) {
        for (int y = y0; y <= y1; y++) {
            for (int x = -r; x <= r; x++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (inO(x, dz, r)) {
                        b.set(x, y, CZ + dz, wallMat(b));
                    }
                }
            }
        }
    }

    private static void mass(StructBp b) {
        for (int i = -1; i <= 7; i++) {
            int y = fy(i);
            octFill(b, rad(i - 1), y - 1, y - 1);
            octFill(b, rad(i), y, y + 6);
        }
        octFill(b, rad(7), 66, 66);
        octFill(b, 8, 67, 73);
    }

    /** Tras vaciar las salas (cajas rectangulares): rellena las esquinas que quedan dentro del grosor del octógono. */
    private static void trim(StructBp b, int i) {
        int r = rad(i);
        int y = fy(i);
        for (int x = -r; x <= r; x++) {
            for (int dz = -r; dz <= r; dz++) {
                if (inI(x, dz, r)) {
                    continue;
                }
                for (int yy = y; yy <= y + 6; yy++) {
                    if ("air".equals(b.get(x, yy, CZ + dz))) {
                        if (inO(x, dz, r)) {
                            b.set(x, yy, CZ + dz, wallMat(b));
                        } else if (yy >= 0 && yy <= 2) {
                            b.set(x, yy, CZ + dz, DB); // relleno de la plataforma
                        } else {
                            b.m.remove(StructBp.key(x, yy, CZ + dz));
                        }
                    }
                }
            }
        }
        // fuera del octógono también pueden haber quedado cajas de aire por encima/abajo: las quitamos del todo
        for (int x = -r - 3; x <= r + 3; x++) {
            for (int dz = -r - 3; dz <= r + 3; dz++) {
                if (!inO(x, dz, r)) {
                    for (int yy = y; yy <= y + 6; yy++) {
                        if ("air".equals(b.get(x, yy, CZ + dz))) {
                            if (yy >= 0 && yy <= 2) {
                                b.set(x, yy, CZ + dz, DB);
                            } else {
                                b.m.remove(StructBp.key(x, yy, CZ + dz));
                            }
                        }
                    }
                }
            }
        }
    }

    // ================================================================== plano de salas

    /** Salas de un piso: planta en anillo (A, pisos pares) o en cuatro salas (B, pisos impares y sótano). */
    private static Arq.Sp[] rooms(StructBp b, Arq a, Arq.Sp core, int i) {
        int y = fy(i);
        int h = rad(i) - 2;
        if (i >= 0 && i % 2 == 0) {
            Arq.Sp ring = a.room("anillo" + i, -h, CZ - h, h, CZ - 3, y, 7);
            a.add(ring, -h, CZ + 3, h, CZ + h, y, 7);
            a.add(ring, -h, CZ - 2, -3, CZ + 2, y, 7);
            a.add(ring, 3, CZ - 2, h, CZ + 2, y, 7);
            trim(b, i);
            a.door(core, ring);
            return new Arq.Sp[]{ring};
        }
        Arq.Sp fm = a.room("frente" + i, -2, CZ - h, 2, CZ - 3, y, 7);
        Arq.Sp bm = a.room("fondo" + i, -2, CZ + 3, 2, CZ + h, y, 7);
        Arq.Sp lh = a.room("izq" + i, -h, CZ - h, -4, CZ + h, y, 7);
        Arq.Sp rh = a.room("der" + i, 4, CZ - h, h, CZ + h, y, 7);
        trim(b, i);
        a.door(core, fm);
        a.door(fm, lh);
        a.door(fm, rh);
        a.door(bm, lh);
        a.door(bm, rh);
        return new Arq.Sp[]{fm, bm, lh, rh};
    }

    // ================================================================== observatorio

    private static Arq.Sp observatory(StructBp b, Arq a, Arq.Sp core) {
        int f = fy(8); // 67
        Arq.Sp obs = a.disc("observatorio", 0, CZ, 6.4, f, 7);
        a.join(obs, core);
        for (int x = -7; x <= 7; x++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (x * x + dz * dz <= 6.4 * 6.4) {
                    b.set(x, f - 1, CZ + dz, ((x + dz) & 1) == 0 ? PD : CD);
                }
            }
        }
        // la boca de la escalera se tapa salvo los dos últimos peldaños
        int[][] ring = {{0, -1}, {1, -1}, {1, 0}, {1, 1}, {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}};
        for (int k = 1; k <= 4; k++) {
            b.set(ring[k][0], f - 1, CZ + ring[k][1], PD);
        }
        // las casillas 5, 6 y 7 del caracol siguen abiertas (necesitan altura libre para subir): es por donde se llega
        for (int k = 5; k <= 7; k++) {
            b.set(ring[k][0], f - 1, CZ + ring[k][1], "air");
        }
        b.set(-2, f, CZ - 1, DBW);
        b.set(-2, f, CZ, DBW);
        b.set(-2, f, CZ + 1, DBW);
        b.set(-1, f, CZ - 2, DBW);
        b.set(-1, f, CZ + 2, DBW);
        // cúpula de cristal con costillas
        for (int y = 74; y <= 82; y++) {
            for (int x = -8; x <= 8; x++) {
                for (int dz = -8; dz <= 8; dz++) {
                    double d = Math.sqrt(x * x + dz * dz + (y - 74.0) * (y - 74.0));
                    if (d < 6.0) {
                        b.set(x, y, CZ + dz, "air");
                    } else if (d < 7.0) {
                        double ang = Math.atan2(dz, x);
                        double k = Math.round(ang / (Math.PI / 4));
                        double off = Math.abs(ang - k * Math.PI / 4) * Math.max(1, Math.sqrt(x * x + dz * dz));
                        boolean rib = off < 0.85 || (y - 74) % 4 == 2;
                        b.set(x, y, CZ + dz, rib ? PP : "purple_stained_glass");
                    }
                }
            }
        }
        for (int x = -9; x <= 9; x++) {
            for (int dz = -9; dz <= 9; dz++) {
                double d = Math.sqrt(x * x + dz * dz);
                if (inO(x, dz, 8) && d >= 6.0) {
                    b.set(x, 74, CZ + dz, d < 7.0 ? PP : DB);
                }
            }
        }
        // orrery flotante sobre el pilar de la escalera
        for (int[] o : new int[][]{{0, 72, 0}, {1, 72, 0}, {-1, 72, 0}, {0, 72, 1}, {0, 72, -1}, {0, 73, 0}, {0, 71, 0}}) {
            b.set(o[0], o[1], CZ + o[2], AM);
        }
        b.set(0, 70, CZ, "end_rod[facing=up]");
        b.set(0, 74, CZ, "amethyst_cluster[facing=up]");
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4;
            int ox = (int) Math.round(Math.cos(an) * 3.6);
            int oz = (int) Math.round(Math.sin(an) * 3.6);
            b.set(ox, 70 + (k % 3), CZ + oz, k % 2 == 0 ? "purple_stained_glass" : AM);
            b.set(ox, 71 + (k % 3), CZ + oz, "end_rod[facing=up]");
        }
        // mesas del observatorio contra el muro del tambor
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4 + Math.PI / 8;
            int ox = (int) Math.round(Math.cos(an) * 5.4);
            int oz = (int) Math.round(Math.sin(an) * 5.4);
            if (k % 2 == 0) {
                b.set(ox, f, CZ + oz, "lectern[facing=" + (Math.abs(ox) > Math.abs(oz) ? (ox > 0 ? "west" : "east") : (oz > 0 ? "north" : "south")) + "]");
            } else {
                b.set(ox, f, CZ + oz, "cartography_table");
            }
        }
        return obs;
    }

    private static void topOfDome(StructBp b) {
        for (int y = 82; y <= 88; y++) {
            b.set(0, y, CZ, y < 85 ? PP : (y < 88 ? "end_rod[facing=up]" : "lightning_rod[facing=up]"));
        }
        b.set(0, 81, CZ, AM);
        // cristales flotantes alrededor de la cima
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4 + 0.4;
            int ox = (int) Math.round(Math.cos(an) * 12.5);
            int oz = (int) Math.round(Math.sin(an) * 12.5);
            int oy = 78 + (k % 3) * 3;
            b.set(ox, oy, CZ + oz, AM);
            b.set(ox, oy + 1, CZ + oz, AM);
            b.set(ox, oy + 2, CZ + oz, "amethyst_cluster[facing=up]");
            b.set(ox, oy - 1, CZ + oz, "amethyst_cluster[facing=down]");
            b.set(ox + 1, oy, CZ + oz, "amethyst_cluster[facing=east]");
            b.set(ox - 1, oy, CZ + oz, "amethyst_cluster[facing=west]");
        }
    }

    // ================================================================== exterior

    private static void exterior(StructBp b, Arq a) {
        for (int i = 0; i <= 8; i++) {
            int r = rad(i);
            int y = fy(i);
            int s = sum(r);
            int[][] vert = {{r, s - r}, {r, -(s - r)}, {-r, s - r}, {-r, -(s - r)}, {s - r, r}, {-(s - r), r}, {s - r, -r}, {-(s - r), -r}};
            // pilastras en los ocho vértices
            for (int[] v : vert) {
                b.set(v[0], y - 1, CZ + v[1], AM);
                for (int yy = y; yy <= y + 5; yy++) {
                    b.set(v[0], yy, CZ + v[1], PP);
                }
            }
            // banda y cornisa en lo alto de cada piso, zócalo en la planta baja
            for (int x = -r - 1; x <= r + 1; x++) {
                for (int dz = -r - 1; dz <= r + 1; dz++) {
                    if (inO(x, dz, r)) {
                        boolean edge = !inO(x + 1, dz, r) || !inO(x - 1, dz, r) || !inO(x, dz + 1, r) || !inO(x, dz - 1, r);
                        if (edge) {
                            b.set(x, y + 6, CZ + dz, PD);
                            if (i == 0) {
                                for (int yy = 2; yy <= 4; yy++) {
                                    if (!PP.equals(b.get(x, yy, CZ + dz))) {
                                        b.set(x, yy, CZ + dz, yy == 4 ? CD : PD);
                                    }
                                }
                            }
                        }
                    } else if (inO(x, dz, r + 1)) {
                        String up;
                        if (x > 0 && inO(x - 1, dz, r)) {
                            up = "-x";
                        } else if (x < 0 && inO(x + 1, dz, r)) {
                            up = "+x";
                        } else if (dz > 0 && inO(x, dz - 1, r)) {
                            up = "-z";
                        } else if (dz < 0 && inO(x, dz + 1, r)) {
                            up = "+z";
                        } else {
                            continue;
                        }
                        b.set(x, y + 6, CZ + dz, sti(DBS, up));
                    }
                }
            }
            // ventanas altas en las caras planas (en el frente, el portón y el balcón ocupan el sitio)
            if (i >= 2) {
                windowZ(b, a, CZ - r, CZ - r + 1, y, false);
            }
            windowZ(b, a, CZ + r, CZ + r - 1, y, true);
            windowX(b, a, r, r - 1, y, true);
            windowX(b, a, -r, -r + 1, y, false);
            // ventanas estrechas en los chaflanes
            int cm = (int) Math.round(s / 2.0);
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    for (int yy = y + 2; yy <= y + 4; yy++) {
                        b.set(sx * cm, yy, CZ + sz * (s - cm), GP);
                        b.set(sx * (cm - 1), yy, CZ + sz * (s - cm), GP);
                        b.set(sx * (cm - 1), yy, CZ + sz * (s - cm - 1), "air");
                    }
                }
            }
            // setbacks: parapeto almenado y pináculos en los vértices
            if (i % 2 == 1) {
                int ry = y + 8;
                for (int x = -r; x <= r; x++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (inO(x, dz, r) && !inO(x, dz, r - 1) && ((x + dz) & 1) == 0) {
                            b.set(x, ry, CZ + dz, DBW);
                        }
                    }
                }
                for (int[] v : vert) {
                    b.set(v[0], ry, CZ + v[1], PP);
                    b.set(v[0], ry + 1, CZ + v[1], PP);
                    b.set(v[0], ry + 2, CZ + v[1], AM);
                    b.set(v[0], ry + 3, CZ + v[1], "amethyst_cluster[facing=up]");
                }
            }
        }
        portico(b);
        turrets(b);
        // pendones
        for (int i = 2; i <= 7; i++) {
            int r = rad(i);
            int y = fy(i);
            for (int s = -1; s <= 1; s += 2) {
                int bx = 4 * s;
                wallBanner(b, bx, y + 3, CZ - r - 1, "purple", "-z");
                wallBanner(b, bx, y + 2, CZ - r - 1, "purple", "-z");
                wallBanner(b, bx, y + 3, CZ + r + 1, "purple", "+z");
                wallBanner(b, bx, y + 2, CZ + r + 1, "purple", "+z");
            }
        }
    }

    /** Cuatro torreones macizos pegados a las diagonales de la base, con tejado cónico púrpura. */
    private static void turrets(StructBp b) {
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = 10 * sx;
                int cz = CZ + 10 * sz;
                b.disc(cx, cz, 3.5, 3, 34, DB);
                for (int y : new int[]{3, 4, 14, 15, 22, 23, 30, 31}) {
                    b.disc(cx, cz, 3.5, y, y, y < 5 ? PD : CD);
                }
                b.disc(cx, cz, 4.4, 34, 34, PD);
                for (int y = 8; y <= 30; y += 8) {
                    b.set(cx + 3 * sx, y, cz, GP);
                    b.set(cx + 3 * sx, y + 1, cz, GP);
                    b.set(cx, y, cz + 3 * sz, GP);
                    b.set(cx, y + 1, cz + 3 * sz, GP);
                }
                cone(b, cx, cz, 4.6, 35, 11, "purpur_block", null, AM);
                b.set(cx, 46, cz, "amethyst_cluster[facing=up]");
                b.set(cx, 47, cz, "end_rod[facing=up]");
                // linternas colgando del alero
                for (int[] d : new int[][]{{4, 0}, {-4, 0}, {0, 4}, {0, -4}}) {
                    hangLantern(b, cx + d[0], 33, cz + d[1], 1, SL);
                    b.set(cx + d[0], 34, cz + d[1], DBW);
                }
                // base de piedra para el torreón sobre la plataforma
                b.disc(cx, cz, 4.2, 3, 5, PD);
            }
        }
    }

    /** Pórtico del portón de entrada. Su tejado es el balcón del primer piso. */
    private static void portico(StructBp b) {
        int z0 = CZ - rad(0) - 3; // 19
        int zw = CZ - rad(0) - 1; // 21: última casilla antes del muro
        for (int s = -1; s <= 1; s += 2) {
            for (int dx = 0; dx < 2; dx++) {
                for (int dz = 0; dz < 2; dz++) {
                    int x = s < 0 ? -6 + dx : 5 + dx;
                    int z = z0 + dz;
                    b.set(x, 3, z, PD);
                    for (int y = 4; y <= 8; y++) {
                        b.set(x, y, z, PP);
                    }
                    b.set(x, 9, z, CD);
                }
            }
        }
        b.box(-7, 10, z0, 7, 10, zw, DB);
        for (int x = -7; x <= 7; x++) {
            b.set(x, 10, z0 - 1, sti(DBS, "+z"));
        }
        // balaustrada del balcón
        for (int x = -7; x <= 7; x++) {
            b.set(x, 11, z0, x % 3 == 0 ? PP : DBW);
        }
        for (int z = z0; z <= zw; z++) {
            b.set(-7, 11, z, z == z0 ? PP : DBW);
            b.set(7, 11, z, z == z0 ? PP : DBW);
        }
        b.set(-7, 12, z0, AM);
        b.set(7, 12, z0, AM);
        hangLantern(b, 0, 9, z0 + 1, 1, SL);
        hangLantern(b, 3, 9, z0 + 1, 1, SL);
        hangLantern(b, -3, 9, z0 + 1, 1, SL);
    }

    /** Ventana alta con arco en una cara que mira a ±z. */
    private static void windowZ(StructBp b, Arq a, int tOut, int tIn, int y, boolean back) {
        for (int o = -1; o <= 1; o++) {
            for (int r = 0; r <= 3; r++) {
                int yy = y + 2 + r;
                if (r == 3 && o != 0) {
                    b.set(o, yy, tOut, sti(PPS, o < 0 ? "-x" : "+x"));
                    b.set(o, yy, tIn, sti(PPS, o < 0 ? "-x" : "+x"));
                    continue;
                }
                b.set(o, yy, tOut, GP);
                b.set(o, yy, tIn, "air");
            }
            b.set(o, y + 1, tIn, "polished_deepslate_slab[type=top]");
        }
        for (int r = 0; r <= 4; r++) {
            for (int s = -1; s <= 1; s += 2) {
                b.set(2 * s, y + 2 + r, tOut, r == 4 ? CD : PP);
                b.set(2 * s, y + 2 + r, tIn, PD);
            }
        }
        int inner = back ? tIn - 1 : tIn + 1;
        for (int o = -3; o <= 3; o++) {
            a.reserve(o, y, inner);
            a.reserve(o, y, back ? inner - 1 : inner + 1);
        }
    }

    private static void windowX(StructBp b, Arq a, int tOut, int tIn, int y, boolean east) {
        for (int o = -1; o <= 1; o++) {
            for (int r = 0; r <= 3; r++) {
                int yy = y + 2 + r;
                if (r == 3 && o != 0) {
                    b.set(tOut, yy, CZ + o, sti(PPS, o < 0 ? "-z" : "+z"));
                    b.set(tIn, yy, CZ + o, sti(PPS, o < 0 ? "-z" : "+z"));
                    continue;
                }
                b.set(tOut, yy, CZ + o, GP);
                b.set(tIn, yy, CZ + o, "air");
            }
            b.set(tIn, y + 1, CZ + o, "polished_deepslate_slab[type=top]");
        }
        for (int r = 0; r <= 4; r++) {
            for (int s = -1; s <= 1; s += 2) {
                b.set(tOut, y + 2 + r, CZ + 2 * s, r == 4 ? CD : PP);
                b.set(tIn, y + 2 + r, CZ + 2 * s, PD);
            }
        }
        int inner = east ? tIn - 1 : tIn + 1;
        for (int o = -3; o <= 3; o++) {
            a.reserve(inner, y, CZ + o);
            a.reserve(east ? inner - 1 : inner + 1, y, CZ + o);
        }
    }

    // ================================================================== escalera

    private static void shaftDecor(StructBp b) {
        for (int n = -1; n <= 7; n++) {
            int y = fy(n);
            soulTorch(b, 1, y + 5, CZ, "-x");
            soulTorch(b, -1, y + 2, CZ, "+x");
            if (n < 7) {
                soulTorch(b, 0, y + 7, CZ + 1, "-z");
            }
        }
    }

    // ================================================================== mobiliario

    private interface WallFn {
        void at(int x, int z, String toWall);
    }

    private static boolean isWall(StructBp b, int x, int y, int z) {
        String g = b.get(x, y, z);
        if (g == null) {
            return false;
        }
        int k = g.indexOf('[');
        String n = k < 0 ? g : g.substring(0, k);
        return n.equals(DB) || n.equals(CR) || n.equals(DT) || n.equals(PD) || n.equals(CD) || n.equals("purpur_pillar") || n.equals("purpur_stairs")
                || n.equals(AM) || n.equals("polished_deepslate_slab");
    }

    /** Recorre las casillas libres pegadas a un muro de la estructura (no a los muebles). Primero se reúnen y luego se aplican. */
    private static void forWalls(StructBp b, Arq a, Arq.Sp s, int f, WallFn fn) {
        Set<Long> seen = new HashSet<>();
        java.util.List<int[]> list = new java.util.ArrayList<>();
        String[] dirs = {"+x", "-x", "+z", "-z"};
        for (int[] bx : s.boxes) {
            for (int x = bx[0]; x <= bx[3]; x++) {
                for (int z = bx[2]; z <= bx[5]; z++) {
                    if (!"air".equals(b.get(x, f, z)) || !seen.add(StructBp.key(x, 0, z)) || !a.free(x, f, z)) {
                        continue;
                    }
                    for (int k = 0; k < 4; k++) {
                        if (isWall(b, x + dx(dirs[k]), f, z + dz(dirs[k]))) {
                            list.add(new int[]{x, z, k});
                            break;
                        }
                    }
                }
            }
        }
        for (int[] c : list) {
            fn.at(c[0], c[1], dirs[c[2]]);
        }
    }

    private static boolean ok(StructBp b, Arq a, int x, int f, int z) {
        return "air".equals(b.get(x, f, z)) && a.free(x, f, z);
    }

    private static void floorMat(StructBp b, Arq.Sp s, int f, String m1, String m2) {
        for (int[] bx : s.boxes) {
            for (int x = bx[0]; x <= bx[3]; x++) {
                for (int z = bx[2]; z <= bx[5]; z++) {
                    if ("air".equals(b.get(x, f, z))) {
                        b.set(x, f - 1, z, m2 == null || ((x + z) & 1) == 0 ? m1 : m2);
                    }
                }
            }
        }
    }

    private static void dress(StructBp b, Arq a, Arq.Sp[] rs, int i) {
        int f = fy(i);
        switch (i) {
            case -1 -> crypt(b, a, rs, f);
            case 0 -> vestibule(b, a, rs[0], f);
            case 1 -> library(b, a, rs, f);
            case 2 -> alchemy(b, a, rs[0], f);
            case 3 -> quarters(b, a, rs, f);
            case 4 -> enchanting(b, a, rs[0], f);
            case 5 -> greenhouse(b, a, rs, f);
            case 6 -> mapRoom(b, a, rs[0], f);
            default -> chambers(b, a, rs, f);
        }
    }

    private static void lamp(StructBp b, int x, int f, int z, int drop, String lantern) {
        if ("air".equals(b.get(x, f + 6, z))) {
            chandelier(b, x, f + 6, z, drop, lantern);
        }
    }

    private static void ringLamps(StructBp b, int f, int h, String lantern) {
        int mid = (3 + h) / 2;
        lamp(b, 0, f, CZ - mid, 3, lantern);
        lamp(b, 0, f, CZ + mid, 3, lantern);
        lamp(b, -mid, f, CZ, 3, lantern);
        lamp(b, mid, f, CZ, 3, lantern);
    }

    private static void shelvesAlong(StructBp b, Arq a, Arq.Sp s, int f, int h, double prob) {
        forWalls(b, a, s, f, (x, z, d) -> {
            if (b.chance(prob)) {
                for (int k = 0; k < h; k++) {
                    b.set(x, f + k, z, k == h - 1 && b.chance(0.3) ? "chiseled_bookshelf[facing=" + card(opp(d)) + "]" : "bookshelf");
                }
            }
        });
    }

    private static void torchesAlong(StructBp b, Arq a, Arq.Sp s, int f, int step, boolean soul) {
        int[] c = {0};
        forWalls(b, a, s, f, (x, z, d) -> {
            if ((c[0]++ % step) == 0 && "air".equals(b.get(x, f + 3, z))) {
                if (soul) {
                    soulTorch(b, x, f + 3, z, opp(d));
                } else {
                    wallTorch(b, x, f + 3, z, opp(d));
                }
            }
        });
    }

    /** Coloca un sarcófago de 2x4 en el primer sitio libre del ala. */
    private static boolean sarco(StructBp b, Arq a, int f, int z0, int[] xs, int sgn) {
        for (int xx : xs) {
            boolean room = true;
            for (int z = z0 - 1; z <= z0 + 4 && room; z++) {
                for (int x = xx - 1; x <= xx + 2; x++) {
                    String g = b.get(x, f, z);
                    if (z >= z0 && z < z0 + 4 && x >= xx && x <= xx + 1) {
                        if (!"air".equals(g) || !a.free(x, f, z)) {
                            room = false;
                        }
                    } else if (g != null && !"air".equals(g) && x != xx - 1 && x != xx + 2) {
                        room = false;
                    }
                }
            }
            if (room) {
                sarcophagus(b, xx, f, z0, 'z', 4, "stone_bricks", "stone_brick_slab", "stone_brick_stairs");
                int cx = sgn < 0 ? xx + 2 : xx - 1;
                if (ok(b, a, cx, f, z0 + 1)) {
                    candles(b, cx, f, z0 + 1, 3);
                }
                if (ok(b, a, cx, f, z0 + 3)) {
                    skull(b, cx, f, z0 + 3, sgn < 0 ? 4 : 12);
                }
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- pisos

    private static void crypt(StructBp b, Arq a, Arq.Sp[] rs, int f) {
        Arq.Sp bm = rs[1];
        for (Arq.Sp s : rs) {
            floorMat(b, s, f, DT, DB);
            torchesAlong(b, a, s, f, 5, true);
        }
        // antecámara: brasero de almas en el centro
        b.set(0, f, CZ - 6, PD);
        b.set(0, f + 1, CZ - 6, "soul_campfire");
        lamp(b, 0, f, CZ - 5, 2, SL);
        // sarcófagos en las alas
        for (int sgn = -1; sgn <= 1; sgn += 2) {
            int[] xs = sgn < 0 ? new int[]{-9, -8, -7} : new int[]{8, 7, 6};
            for (int z0 = CZ - 9; z0 <= CZ + 6; z0 += 5) {
                sarco(b, a, f, z0, xs, sgn);
            }
            lamp(b, sgn * 6, f, CZ, 2, SL);
            lamp(b, sgn * 6, f, CZ - 6, 2, SL);
            lamp(b, sgn * 6, f, CZ + 6, 2, SL);
        }
        // osario al fondo
        forWalls(b, a, bm, f, (x, z, d) -> {
            if (b.chance(0.7)) {
                b.set(x, f, z, "bone_block[axis=y]");
                if (b.chance(0.6)) {
                    b.set(x, f + 1, z, "bone_block[axis=y]");
                    skull(b, x, f + 2, z, b.r.nextInt(16));
                }
            }
        });
        if (ok(b, a, 0, f, CZ + 7)) {
            b.set(0, f, CZ + 7, PD);
            b.set(0, f + 1, CZ + 7, "soul_campfire");
        }
        lamp(b, 0, f, CZ + 5, 2, SL);
        for (Arq.Sp s : rs) {
            forWalls(b, a, s, f, (x, z, d) -> {
                if (b.chance(0.06) && "air".equals(b.get(x, f + 5, z))) {
                    b.set(x, f + 5, z, "cobweb");
                }
            });
        }
    }

    private static void vestibule(StructBp b, Arq a, Arq.Sp ring, int f) {
        int h = rad(0) - 2;
        floorMat(b, ring, f, PD, DT);
        for (int x = -1; x <= 1; x++) {
            for (int z = CZ - h; z <= CZ - 3; z++) {
                b.set(x, f, z, "purple_carpet");
            }
        }
        for (int z = CZ - h; z <= CZ - 3; z++) {
            b.set(-2, f, z, "magenta_carpet");
            b.set(2, f, z, "magenta_carpet");
        }
        torchesAlong(b, a, ring, f, 4, true);
        ringLamps(b, f, h, SL);
        // columnas
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sz = -1; sz <= 1; sz += 2) {
                int cx = 6 * sx;
                int cz = CZ + 6 * sz;
                if (!"air".equals(b.get(cx, f, cz))) {
                    continue;
                }
                b.set(cx, f, cz, PD);
                for (int y = f + 1; y <= f + 5; y++) {
                    b.set(cx, y, cz, PP);
                }
                b.set(cx, f + 5, cz, AM);
                b.set(cx, f + 6, cz, CD);
            }
        }
        // estanque mágico al fondo
        for (int x = -3; x <= 3; x++) {
            for (int z = CZ + 5; z <= CZ + 9; z++) {
                boolean edge = x == -3 || x == 3 || z == CZ + 5 || z == CZ + 9;
                if ("air".equals(b.get(x, f, z)) && inI(x, z - CZ, rad(0))) {
                    if (edge) {
                        b.set(x, f, z, PD);
                    } else {
                        b.set(x, f - 1, z, "sea_lantern");
                        b.set(x, f, z, "water");
                    }
                }
            }
        }
        forWalls(b, a, ring, f, (x, z, d) -> {
            if ((x + z) % 7 == 0 && "air".equals(b.get(x, f + 2, z))) {
                wallBanner(b, x, f + 2, z, "purple", opp(d));
            }
        });
        for (int s = -1; s <= 1; s += 2) {
            b.set(4 * s, f, CZ - h, PD);
            b.set(4 * s, f + 1, CZ - h, "soul_campfire");
        }
    }

    private static void library(StructBp b, Arq a, Arq.Sp[] rs, int f) {
        Arq.Sp bm = rs[1], lh = rs[2], rh = rs[3];
        for (Arq.Sp s : rs) {
            floorMat(b, s, f, "dark_oak_planks", "spruce_planks");
            torchesAlong(b, a, s, f, 5, false);
        }
        shelvesAlong(b, a, lh, f, 4, 0.9);
        shelvesAlong(b, a, bm, f, 4, 0.85);
        shelvesAlong(b, a, rh, f, 3, 0.6);
        for (int k = 0; k < 3; k++) {
            int z = CZ - 5 + k * 5;
            if (ok(b, a, -7, f, z) && ok(b, a, -7, f, z + 1) && ok(b, a, -7, f, z + 2)) {
                table(b, -7, f, z, 'z', 3, "spruce_slab", "spruce_fence");
                chair(b, -6, f, z + 1, "-x", "spruce_stairs");
                candles(b, -7, f + 2, z + 1, 2);
            }
        }
        for (int k = 0; k < 3; k++) {
            int z = CZ - 5 + k * 5;
            if (ok(b, a, 7, f, z) && ok(b, a, 7, f, z + 1)) {
                b.set(7, f, z, "lectern[facing=west]");
                b.set(7, f, z + 1, "cartography_table");
                chair(b, 6, f, z, "+x", "spruce_stairs");
            }
        }
        b.set(0, f, CZ - 7, "lectern[facing=south]");
        lamp(b, 0, f, CZ - 5, 2, "lantern");
        lamp(b, 0, f, CZ + 5, 2, "lantern");
        for (int dz : new int[]{-7, 0, 7}) {
            lamp(b, -7, f, CZ + dz, 3, "lantern");
            lamp(b, 7, f, CZ + dz, 3, "lantern");
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = CZ + 4; z <= CZ + 7; z++) {
                if ("air".equals(b.get(x, f, z))) {
                    b.set(x, f, z, "red_carpet");
                }
            }
        }
    }

    private static void alchemy(StructBp b, Arq a, Arq.Sp ring, int f) {
        int h = rad(2) - 2;
        floorMat(b, ring, f, DT, "polished_blackstone");
        torchesAlong(b, a, ring, f, 4, true);
        ringLamps(b, f, h, "lantern");
        int[] c = {0};
        forWalls(b, a, ring, f, (x, z, d) -> {
            int n = c[0]++ % 6;
            if (n == 0) {
                b.set(x, f, z, "barrel[facing=up]");
            } else if (n == 2) {
                b.set(x, f, z, "cauldron");
            } else if (n == 3) {
                b.set(x, f, z, "brewing_stand");
            } else if (n == 4) {
                b.set(x, f, z, "composter");
            }
        });
        for (int s = -1; s <= 1; s += 2) {
            int cx = s * ((3 + h) / 2);
            if (ok(b, a, cx, f, CZ) && ok(b, a, cx, f, CZ + 1)) {
                b.set(cx, f, CZ, "blast_furnace[facing=" + (s < 0 ? "east" : "west") + "]");
                b.set(cx, f + 1, CZ, "cauldron");
                b.set(cx, f, CZ + 1, "brewing_stand");
            }
        }
    }

    private static void quarters(StructBp b, Arq a, Arq.Sp[] rs, int f) {
        Arq.Sp bm = rs[1];
        int h = rad(3) - 2;
        for (Arq.Sp s : rs) {
            floorMat(b, s, f, "spruce_planks", "dark_oak_planks");
            torchesAlong(b, a, s, f, 5, false);
        }
        for (int k = 0; k < 4; k++) {
            int z = CZ - 7 + k * 4;
            for (int x = -h + 1; x <= -h + 3; x++) {
                if (ok(b, a, x, f, z) && ok(b, a, x + 1, f, z) && "air".equals(b.get(x - 1, f, z)) == false) {
                    b.set(x, f, z, "purple_bed[facing=east,part=foot]");
                    b.set(x + 1, f, z, "purple_bed[facing=east,part=head]");
                    if (ok(b, a, x, f, z + 1)) {
                        b.set(x, f, z + 1, "barrel[facing=up]");
                    }
                    break;
                }
            }
        }
        for (int s = 0; s < 2; s++) {
            int z = CZ - 6 + s * 7;
            if (ok(b, a, 6, f, z) && ok(b, a, 6, f, z + 4)) {
                table(b, 6, f, z, 'z', 5, "spruce_slab", "spruce_fence");
                for (int k = 1; k <= 3; k++) {
                    chair(b, 5, f, z + k, "+x", "dark_oak_stairs");
                    chair(b, 7, f, z + k, "-x", "dark_oak_stairs");
                }
                candles(b, 6, f + 2, z + 2, 3);
            }
        }
        forWalls(b, a, bm, f, (x, z, d) -> {
            int n = Math.abs(x + z) % 4;
            b.set(x, f, z, n == 0 ? "smoker[facing=" + card(opp(d)) + "]" : (n == 1 ? "barrel[facing=up]" : (n == 2 ? "furnace[facing=" + card(opp(d)) + "]" : "hay_block")));
        });
        lamp(b, 0, f, CZ - 5, 2, "lantern");
        lamp(b, 0, f, CZ + 5, 2, "lantern");
        for (int dz : new int[]{-6, 0, 6}) {
            lamp(b, -5, f, CZ + dz, 3, "lantern");
            lamp(b, 5, f, CZ + dz, 3, "lantern");
        }
    }

    private static void enchanting(StructBp b, Arq a, Arq.Sp ring, int f) {
        int h = rad(4) - 2;
        floorMat(b, ring, f, PD, AM);
        torchesAlong(b, a, ring, f, 4, true);
        ringLamps(b, f, h, SL);
        shelvesAlong(b, a, ring, f, 3, 0.55);
        int mid = (3 + h) / 2;
        for (int[] t : new int[][]{{0, -mid}, {0, mid}, {-mid, 0}, {mid, 0}}) {
            int x = t[0], z = CZ + t[1];
            if (ok(b, a, x, f, z)) {
                b.set(x, f, z, "enchanting_table");
                for (int[] d : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) {
                    if (ok(b, a, x + d[0], f, z + d[1])) {
                        b.set(x + d[0], f, z + d[1], "bookshelf");
                        b.set(x + d[0], f + 1, z + d[1], "bookshelf");
                    }
                }
            }
        }
    }

    private static void greenhouse(StructBp b, Arq a, Arq.Sp[] rs, int f) {
        Arq.Sp bm = rs[1], lh = rs[2], rh = rs[3];
        for (Arq.Sp s : rs) {
            floorMat(b, s, f, "moss_block", "rooted_dirt");
            torchesAlong(b, a, s, f, 5, false);
        }
        for (Arq.Sp s : new Arq.Sp[]{lh, rh}) {
            for (int[] bx : s.boxes) {
                for (int x = bx[0]; x <= bx[3]; x++) {
                    for (int z = bx[2]; z <= bx[5]; z++) {
                        if (ok(b, a, x, f, z) && b.chance(0.35)) {
                            b.set(x, f, z, b.pick("azalea", "flowering_azalea", "fern", "grass", "moss_carpet", "lily_of_the_valley"));
                        }
                    }
                }
            }
        }
        int h = rad(5) - 2;
        int px = (4 + h) / 2;
        for (int x = px - 2; x <= px + 2; x++) {
            for (int z = CZ - 2; z <= CZ + 2; z++) {
                boolean edge = x == px - 2 || x == px + 2 || z == CZ - 2 || z == CZ + 2;
                if (!"air".equals(b.get(x, f, z)) && !isPlant(b.get(x, f, z))) {
                    continue;
                }
                if (edge) {
                    b.set(x, f, z, "mossy_cobblestone");
                } else {
                    b.set(x, f - 1, z, "clay");
                    b.set(x, f, z, "water");
                }
            }
        }
        forWalls(b, a, bm, f, (x, z, d) -> b.set(x, f, z, b.chance(0.5) ? "composter" : "flowering_azalea_leaves[persistent=true]"));
        for (int x : new int[]{-7, 7}) {
            for (int dz : new int[]{-7, 0, 7}) {
                lamp(b, x, f, CZ + dz, 2, "lantern");
            }
        }
        lamp(b, 0, f, CZ - 5, 2, "lantern");
        lamp(b, 0, f, CZ + 5, 2, "lantern");
        for (int dz = CZ - 5; dz <= CZ + 5; dz += 2) {
            if (ok(b, a, 0, f, dz)) {
                b.set(0, f, dz, "azalea");
            }
        }
    }

    private static boolean isPlant(String s) {
        return s != null && (s.startsWith("azalea") || s.startsWith("flowering_azalea") && !s.contains("leaves") || s.equals("fern") || s.equals("grass")
                || s.endsWith("mushroom") || s.equals("moss_carpet") || s.equals("lily_of_the_valley"));
    }

    private static void mapRoom(StructBp b, Arq a, Arq.Sp ring, int f) {
        int h = rad(6) - 2;
        floorMat(b, ring, f, DT, PD);
        torchesAlong(b, a, ring, f, 4, true);
        ringLamps(b, f, h, SL);
        shelvesAlong(b, a, ring, f, 2, 0.4);
        int mid = (3 + h) / 2;
        for (int[] t : new int[][]{{0, -mid}, {0, mid}, {-mid, 0}, {mid, 0}}) {
            int x = t[0], z = CZ + t[1];
            if (ok(b, a, x, f, z)) {
                b.set(x, f, z, "cartography_table");
                if (ok(b, a, x + 1, f, z)) {
                    b.set(x + 1, f, z, "lectern[facing=" + (t[0] != 0 ? "west" : "south") + "]");
                }
                if (ok(b, a, x, f + 1, z)) {
                    candles(b, x, f + 1, z, 3);
                }
            }
        }
        for (int k = 0; k < 8; k++) {
            double an = k * Math.PI / 4 + Math.PI / 8;
            int cx = (int) Math.round(Math.cos(an) * (h - 1));
            int cz = (int) Math.round(Math.sin(an) * (h - 1));
            if (ok(b, a, cx, f, CZ + cz)) {
                b.set(cx, f, CZ + cz, "lodestone");
                b.set(cx, f + 1, CZ + cz, "end_rod[facing=up]");
            }
        }
    }

    private static void chambers(StructBp b, Arq a, Arq.Sp[] rs, int f) {
        Arq.Sp fm = rs[0], bm = rs[1], lh = rs[2], rh = rs[3];
        floorMat(b, lh, f, "dark_oak_planks", "spruce_planks");
        floorMat(b, rh, f, PD, DT);
        floorMat(b, fm, f, PD, AM);
        floorMat(b, bm, f, "gold_block", "polished_blackstone");
        for (Arq.Sp s : rs) {
            torchesAlong(b, a, s, f, 5, true);
        }
        int h = rad(7) - 2;
        int xb = -h + 1;
        for (int dz = -2; dz <= -1; dz++) {
            if (ok(b, a, xb, f, CZ + dz) && ok(b, a, xb + 1, f, CZ + dz)) {
                b.set(xb, f, CZ + dz, "magenta_bed[facing=east,part=foot]");
                b.set(xb + 1, f, CZ + dz, "magenta_bed[facing=east,part=head]");
            }
        }
        shelvesAlong(b, a, lh, f, 3, 0.35);
        shelvesAlong(b, a, rh, f, 3, 0.8);
        for (int dz : new int[]{-5, 0, 5}) {
            lamp(b, -5, f, CZ + dz, 3, "lantern");
            lamp(b, 5, f, CZ + dz, 3, SL);
        }
        for (int k = 0; k < 2; k++) {
            int z = CZ - 4 + k * 5;
            if (ok(b, a, 6, f, z) && ok(b, a, 6, f, z + 1) && ok(b, a, 6, f, z + 2)) {
                table(b, 6, f, z, 'z', 3, "dark_oak_slab", "dark_oak_fence");
                candles(b, 6, f + 2, z + 1, 3);
                chair(b, 5, f, z + 1, "+x", "dark_oak_stairs");
            }
        }
        if (ok(b, a, 0, f, CZ - h)) {
            chair(b, 0, f, CZ - h, "+z", PPS);
            wallBanner(b, 0, f + 2, CZ - h, "purple", "+z");
        }
        lamp(b, 0, f, CZ - 5, 2, SL);
        forWalls(b, a, bm, f, (x, z, d) -> {
            if (b.chance(0.5)) {
                b.set(x, f, z, "chest[facing=" + card(opp(d)) + "]");
            } else if (b.chance(0.5)) {
                b.set(x, f, z, "gold_block");
            }
        });
        lamp(b, 0, f, CZ + 5, 2, SL);
    }
}
