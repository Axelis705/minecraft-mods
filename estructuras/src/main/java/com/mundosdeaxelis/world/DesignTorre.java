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
        a.join(rooms[1][0], rooms[2][0]);
        a.join(rooms[1][0], rooms[2][1]);
        a.door(core, rooms[2][1], fy(1), 0, 3, 3, true);
        String skip = System.getProperty("torre.skip", "");
        for (int i = -1; i <= 7; i++) {
            if (!("," + skip + ",").contains("," + i + ",")) {
                dress(b, a, rooms[i + 1], i);
            }
        }
        shaftDecor(b);
        topOfDome(b);
        a.seed(0, fy(8), CZ - 3);
        // Todas las plantas están hechas a mano y las revisa ConectividadTest (salas sin acceso / trampas),
        // así que no se llama a Arq.repair: quitaría muebles diseñados (mesas de encantamiento, estanterías, el estanque...).
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
        if (i == 0) {
            // Gran Salón: el piso 0 y el piso 1 son una sola nave de 15 bloques de alto (la galería del piso 1 se construye en TorreInterior)
            Arq.Sp hall = a.room("salon", -h, CZ - h, h, CZ - 3, y, 15);
            a.add(hall, -h, CZ + 3, h, CZ + h, y, 15);
            a.add(hall, -h, CZ - 2, -3, CZ + 2, y, 15);
            a.add(hall, 3, CZ - 2, h, CZ + 2, y, 15);
            trim(b, 0);
            trim(b, 1);
            a.door(core, hall);
            return new Arq.Sp[]{hall};
        }
        if (i == 1) {
            Arq.Sp gal = a.ghost("galeria", -h, CZ - h, h, CZ - h + 2, y, 7);
            Arq.Sp bridge = a.ghost("puente", -2, CZ - 7, 2, CZ - 3, y, 7);
            a.join(gal, bridge);
            return new Arq.Sp[]{gal, bridge};
        }
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

    // ================================================================== interiores (cada piso se diseña a mano en TorreInterior)

    private static void dress(StructBp b, Arq a, Arq.Sp[] rs, int i) {
        int f = fy(i);
        switch (i) {
            case -1 -> TorreInterior.crypt(b);
            case 0 -> {
                TorreInterior.hall(b);
                TorreInterior.secrets(b, a);
            }
            case 1 -> {
            }
            case 2 -> TorreInterior.alchemy(b);
            case 3 -> TorreInterior.library(b);
            case 4 -> TorreInterior.enchanting(b);
            case 5 -> TorreInterior.greenhouse(b);
            case 6 -> TorreInterior.stars(b);
            default -> TorreInterior.chambers(b);
        }
    }
}
