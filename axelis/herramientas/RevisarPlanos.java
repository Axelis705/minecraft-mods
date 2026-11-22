import com.mundosdeaxelis.world.StructBp;
import com.mundosdeaxelis.world.StructDesigns;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Revisión de los planos fuera del juego (etapa 282). Para cada estructura mira:
 * - cofres: el frente (hacia donde mira) tiene que dar a un hueco, y la espalda conviene que dé a algo sólido;
 * - puertas de hierro: las dos mitades, suelo debajo, paso libre delante y detrás, y muro a los lados;
 * - altares malditos: suelo debajo, aire encima y un claro alrededor para pelear;
 * - mímicos: junto a un cofre de verdad.
 * Uso: java -cp clases:. RevisarPlanos [volcado_dir] [diseño...]
 * Con volcado_dir escribe también "x y z estado" de cada plano (para el visor).
 */
public class RevisarPlanos {
    static final String[] TODOS = {"bosque", "santuario", "forja", "fragua", "hielo", "fortin", "volcan", "torre_arcana", "torre",
            "casa_ruda", "casa_bruno", "casa_hilda", "casa_teo", "casa_ignacio"};
    static int fallos = 0;

    public static void main(String[] a) throws Exception {
        Path dump = a.length > 0 && !a[0].equals("-") ? Path.of(a[0]) : null;
        List<String> ds = new ArrayList<>();
        for (int i = 1; i < a.length; i++) {
            ds.add(a[i]);
        }
        if (ds.isEmpty()) {
            ds.addAll(List.of(TODOS));
        }
        for (String d : ds) {
            StructBp b = StructDesigns.make(d);
            if (dump != null) {
                Files.createDirectories(dump);
                try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dump.resolve(d + ".txt")))) {
                    for (Map.Entry<Long, String> e : b.m.entrySet()) {
                        w.println(StructBp.kx(e.getKey()) + " " + StructBp.ky(e.getKey()) + " " + StructBp.kz(e.getKey()) + " " + e.getValue());
                    }
                }
            }
            revisar(d, b);
        }
        System.out.println(fallos == 0 ? "PLANOS_OK" : "PLANOS_FALLOS " + fallos);
        if (fallos > 0) {
            System.exit(1);
        }
    }

    static String n(String s) {
        if (s == null) {
            return "air";
        }
        s = s.startsWith("minecraft:") ? s.substring(10) : s;
        return s;
    }

    static String id(String s) {
        s = n(s);
        int i = s.indexOf('[');
        return i < 0 ? s : s.substring(0, i);
    }

    static String prop(String s, String p) {
        s = n(s);
        int i = s.indexOf(p + "=");
        if (i < 0) {
            return null;
        }
        int j = i + p.length() + 1;
        int k = j;
        while (k < s.length() && s.charAt(k) != ',' && s.charAt(k) != ']') {
            k++;
        }
        return s.substring(j, k);
    }

    /** ¿Se puede pasar / abrir? Aire y cosas finas. */
    static boolean hueco(String s) {
        String i = id(s);
        return i.equals("air") || i.endsWith("carpet") || i.endsWith("torch") || i.endsWith("button") || i.endsWith("pressure_plate")
                || i.endsWith("_sign") || i.endsWith("banner") || i.contains("rail") || i.endsWith("snow") || i.equals("cobweb")
                || i.endsWith("flower") || i.equals("grass") || i.equals("fern") || i.contains("sapling") || i.endsWith("candle")
                || i.equals("lever") || i.equals("redstone_wire") || i.equals("light") || i.equals("structure_void") || i.endsWith("_door")
                || i.contains("est_") && (i.contains("hueso") || i.contains("pergamino") || i.contains("vela"))
                || i.equals("vine") || i.endsWith("lichen") || i.equals("moss_carpet") || i.equals("water") || i.equals("lava")
                || i.endsWith("_mushroom") || i.equals("dead_bush") || i.endsWith("roots") || i.equals("soul_fire") || i.equals("fire");
    }

    static boolean solido(String s) {
        if (s == null) {
            return false;
        }
        String i = id(s);
        if (hueco(s)) {
            return false;
        }
        return !(i.endsWith("_slab") && "bottom".equals(prop(s, "type"))) && !i.endsWith("fence") && !i.endsWith("_pane")
                && !i.equals("iron_bars") && !i.endsWith("_wall") && !i.contains("chest") && !i.equals("lantern") && !i.equals("chain");
    }

    static int dx(String f) {
        return switch (f) {
            case "east" -> 1;
            case "west" -> -1;
            default -> 0;
        };
    }

    static int dz(String f) {
        return switch (f) {
            case "south" -> 1;
            case "north" -> -1;
            default -> 0;
        };
    }

    static void fallo(String d, String q) {
        fallos++;
        System.out.println("  FALLO " + d + ": " + q);
    }

    static void revisar(String d, StructBp b) {
        int cofres = 0, puertas = 0, altares = 0;
        for (Map.Entry<Long, String> e : b.m.entrySet()) {
            int x = StructBp.kx(e.getKey()), y = StructBp.ky(e.getKey()), z = StructBp.kz(e.getKey());
            String s = e.getValue();
            String i = id(s);
            if (i.contains("vault_chest") || i.equals("chest") || i.equals("trapped_chest") || i.endsWith("grave")) {
                cofres++;
                String f = prop(s, "facing");
                if (f == null) {
                    continue;
                }
                String delante = b.get(x + dx(f), y, z + dz(f));
                String detras = b.get(x - dx(f), y, z - dz(f));
                if (!hueco(delante) && !id(delante).contains("chest")) {
                    fallo(d, "cofre " + i + " en " + x + " " + y + " " + z + " mira a " + f + " contra " + n(delante)
                            + (hueco(detras) ? " (detrás hay hueco: está al revés)" : ""));
                }
            }
            if (i.equals("iron_door") && "lower".equals(prop(s, "half"))) {
                puertas++;
                String f = prop(s, "facing");
                String arriba = b.get(x, y + 1, z);
                if (!id(arriba).equals("iron_door") || !"upper".equals(prop(arriba, "half"))) {
                    fallo(d, "puerta de hierro en " + x + " " + y + " " + z + " sin mitad de arriba (" + n(arriba) + ")");
                }
                if (!solido(b.get(x, y - 1, z))) {
                    fallo(d, "puerta de hierro en " + x + " " + y + " " + z + " sin suelo (" + n(b.get(x, y - 1, z)) + ")");
                }
                for (int k : new int[]{1, -1}) {
                    String p = b.get(x + k * dx(f), y, z + k * dz(f));
                    String p2 = b.get(x + k * dx(f), y + 1, z + k * dz(f));
                    if (!hueco(p) || !hueco(p2)) {
                        fallo(d, "puerta de hierro en " + x + " " + y + " " + z + " (" + f + ") tapada " + (k > 0 ? "delante" : "detrás") + " por " + n(hueco(p) ? p2 : p));
                    }
                }
                // a los lados: muro (si no, la puerta queda suelta en medio de la sala)
                int lx = dz(f), lz = dx(f);
                boolean l1 = solidoOPuerta(b.get(x + lx, y, z + lz)), l2 = solidoOPuerta(b.get(x - lx, y, z - lz));
                if (!l1 && !l2) {
                    fallo(d, "puerta de hierro en " + x + " " + y + " " + z + " (" + f + ") suelta: sin muro a ningún lado");
                }
                String hinge = prop(s, "hinge");
                if (hinge != null && b.get(x + lx, y, z + lz) != null && id(b.get(x + lx, y, z + lz)).equals("iron_door")) {
                    // dos puertas juntas: deberían abrir hacia lados contrarios
                    String h2 = prop(b.get(x + lx, y, z + lz), "hinge");
                    if (hinge.equals(h2)) {
                        fallo(d, "puertas de hierro dobles en " + x + " " + y + " " + z + " con la bisagra del mismo lado");
                    }
                }
            }
            if (i.equals("mundos_de_axelis:altar_maldito")) {
                altares++;
                if (!solido(b.get(x, y - 1, z))) {
                    fallo(d, "altar en " + x + " " + y + " " + z + " sin suelo (" + n(b.get(x, y - 1, z)) + ")");
                }
                int tapado = 0, sinSuelo = 0;
                for (int ax = -2; ax <= 2; ax++) {
                    for (int az = -2; az <= 2; az++) {
                        if (ax == 0 && az == 0) {
                            continue;
                        }
                        for (int ay = 0; ay <= 2; ay++) {
                            if (!hueco(b.get(x + ax, y + ay, z + az))) {
                                tapado++;
                            }
                        }
                        if (!solido(b.get(x + ax, y - 1, z + az))) {
                            sinSuelo++;
                        }
                    }
                }
                for (int ay = 1; ay <= 3; ay++) {
                    if (!hueco(b.get(x, y + ay, z))) {
                        fallo(d, "altar en " + x + " " + y + " " + z + " con techo bajo (" + n(b.get(x, y + ay, z)) + " a " + ay + ")");
                        break;
                    }
                }
                if (tapado > 6 || sinSuelo > 3) {
                    fallo(d, "altar en " + x + " " + y + " " + z + " sin claro alrededor (" + tapado + " bloques encima del radio 2, " + sinSuelo + " sin suelo)");
                }
                if (!altarBien(b, x, y, z, true)) {
                    sugerir(d, b, x, y, z);
                }
            }
        }
        for (StructBp.Mob m : b.mobs) {
            if (m.kind().equals("mimico")) {
                boolean junto = false;
                for (int ax = -2; ax <= 2; ax++) {
                    for (int az = -2; az <= 2; az++) {
                        String s = b.get(m.x() + ax, m.y(), m.z() + az);
                        if (s != null && id(s).contains("chest")) {
                            junto = true;
                        }
                    }
                }
                if (!junto) {
                    fallo(d, "mímico en " + m.x() + " " + m.y() + " " + m.z() + " sin un cofre al lado");
                }
                if (!hueco(b.get(m.x(), m.y(), m.z())) || !solido(b.get(m.x(), m.y() - 1, m.z()))) {
                    fallo(d, "mímico en " + m.x() + " " + m.y() + " " + m.z() + " dentro de " + n(b.get(m.x(), m.y(), m.z())) + " o sin suelo");
                }
            }
        }
        // etapa 287: la celda de cada preso tiene que estar cerrada del todo (barrotes, muros y su puerta de hierro, sin huecos)
        for (StructBp.Mob m : b.mobs) {
            if (m.kind().equals("habitante")) {
                int[] r = celda(b, m.x(), m.y(), m.z());
                if (r[0] > 0) {
                    fallo(d, "la celda de " + m.tag() + " en " + m.x() + " " + m.y() + " " + m.z() + " no está cerrada: se sale por "
                            + r[1] + " " + r[2] + " " + r[3]);
                }
            }
        }
        System.out.println(d + ": " + cofres + " cofres, " + puertas + " puertas de hierro, " + altares + " altares, " + b.mobs.size() + " enemigos");
    }

    /**
     * Desde el preso, por el aire (y lo fino) sin cruzar puertas de hierro, barrotes ni bloques: si llega a 400 casillas o a fuera del
     * plano, la celda no está cerrada. Devuelve {0} si está cerrada, o {1, x, y, z} con la casilla por la que se escapa.
     */
    static int[] celda(StructBp b, int x0, int y0, int z0) {
        java.util.ArrayDeque<int[]> q = new java.util.ArrayDeque<>();
        java.util.Set<Long> vis = new java.util.HashSet<>();
        q.add(new int[]{x0, y0, z0});
        vis.add(StructBp.key(x0, y0, z0));
        // también en diagonal: un hueco en la esquina de los barrotes se ve (aunque no se pueda pasar por él)
        int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (vis.size() > 400) {
                return new int[]{1, c[0], c[1], c[2]};
            }
            for (int[] dd : dirs) {
                int x = c[0] + dd[0], y = c[1] + dd[1], z = c[2] + dd[2];
                if (!vis.add(StructBp.key(x, y, z))) {
                    continue;
                }
                String s = b.get(x, y, z);
                if (s == null) {
                    return new int[]{1, x, y, z};
                }
                if (hueco(s) && !id(s).equals("iron_door")) {
                    q.add(new int[]{x, y, z});
                }
            }
        }
        return new int[]{0};
    }

    /** ¿Buen sitio para un altar? Suelo, 3 de aire encima y un claro de radio 2 (para pelear contra la oleada). */
    static boolean altarBien(StructBp b, int x, int y, int z, boolean yaPuesto) {
        if (!solido(b.get(x, y - 1, z)) || (!yaPuesto && !hueco(b.get(x, y, z)))) {
            return false;
        }
        for (int ay = 1; ay <= 3; ay++) {
            if (!hueco(b.get(x, y + ay, z))) {
                return false;
            }
        }
        int tapado = 0, sinSuelo = 0;
        for (int ax = -2; ax <= 2; ax++) {
            for (int az = -2; az <= 2; az++) {
                if (ax == 0 && az == 0) {
                    continue;
                }
                for (int ay = 0; ay <= 2; ay++) {
                    if (!hueco(b.get(x + ax, y + ay, z + az))) {
                        tapado++;
                    }
                }
                if (!solido(b.get(x + ax, y - 1, z + az))) {
                    sinSuelo++;
                }
            }
        }
        if (!yaPuesto) {
            // bajo techo (dentro de la estructura, no en el prado de fuera)
            boolean techo = false;
            for (int ay = 4; ay <= 24 && !techo; ay++) {
                techo = solido(b.get(x, y + ay, z));
            }
            if (!techo) {
                return false;
            }
        }
        return tapado <= 6 && sinSuelo <= 3;
    }

    static void sugerir(String d, StructBp b, int x0, int y0, int z0) {
        int best = Integer.MAX_VALUE;
        int[] bp = null;
        for (int r = 1; r <= 16; r++) {
            for (int x = x0 - r; x <= x0 + r; x++) {
                for (int z = z0 - r; z <= z0 + r; z++) {
                    for (int y = y0 - 2; y <= y0 + 2; y++) {
                        if (altarBien(b, x, y, z, false)) {
                            int dd = (x - x0) * (x - x0) + (z - z0) * (z - z0) + 4 * (y - y0) * (y - y0);
                            if (dd < best) {
                                best = dd;
                                bp = new int[]{x, y, z};
                            }
                        }
                    }
                }
            }
            if (bp != null && r * r > best) {
                break;
            }
        }
        System.out.println("    sugerencia para el altar de " + d + ": " + (bp == null ? "ninguna" : bp[0] + " " + bp[1] + " " + bp[2]));
    }

    static boolean solidoOPuerta(String s) {
        return s != null && (solido(s) || id(s).equals("iron_door") || id(s).endsWith("_wall") || id(s).equals("iron_bars") || id(s).endsWith("_pane"));
    }
}
