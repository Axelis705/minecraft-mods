package com.coleccionista.mapa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Construye una casa "a mano" desde un plano de texto: cada piso es un dibujo donde cada letra es una columna de bloques. Así lo que se ve en
 * el dibujo es justo lo que queda en el juego (nada de cuentas de coordenadas a mano), y {@link Revisor} lo revisa caminándolo.
 * <p>
 * El archivo ({@code data/coleccionista/planos/<nombre>.txt}) tiene:
 * <pre>
 *   # comentario
 *   estilo v nombre=Vestíbulo piso=coleccionista:marmol_negro|coleccionista:marmol_crema pared=coleccionista:papel_tapiz luz=10
 *   mueble L minecraft:bookshelf alto=3
 *   mueble B minecraft:red_bed cama
 *   mueble S coleccionista:silla mira=centro
 *   punto 1 man.entrada 90
 *   marcas man.esc.
 *   piso 0 alto 6 origen 0 -18
 *   ...filas del dibujo (una por z; cada letra, una x)...
 *   fin
 * </pre>
 * Las letras del dibujo:
 * <ul>
 * <li>minúsculas: el piso de un cuarto (su {@code estilo}); arriba, aire hasta el techo, y luz cada tanto;</li>
 * <li>{@code #} muro de adentro (con zoclo de madera abajo y el papel del cuarto de al lado); {@code X} muro de afuera (ladrillo);
 * {@code W} muro de afuera con ventana;</li>
 * <li>{@code D} puerta de madera; {@code |} paso sin puerta (3 de alto);</li>
 * <li>{@code ^ v < >} escalera que sube hacia allá (cada escalón uno más alto; se le abre el techo encima);</li>
 * <li>{@code ~} hueco: nada (el aire del cuarto de abajo, como un vestíbulo de doble altura); {@code :} barandal;</li>
 * <li>{@code @} escondite (piso y dos de aire, con su marca); {@code %} cortina (para tapar un escondite);</li>
 * <li>mayúsculas: muebles ({@code mueble}); números: puntos con nombre ({@code punto}); espacio: no se toca.</li>
 * </ul>
 */
public final class Casa {
    private static final String C = "coleccionista:";

    /** Cómo es un cuarto: su piso (uno, o dos en ajedrez), sus paredes, el zoclo, el techo y la luz. */
    public record Estilo(char letra, String nombre, String piso, String piso2, String pared, String zoclo, String techo, int luz, String alfombra) {
    }

    /** Un mueble: su bloque, cuántos de alto, hacia dónde mira, si es cama (dos casillas) y si es un escondite. */
    public record Mueble(char letra, String bloque, int alto, String mira, boolean cama, boolean escondite, boolean doble, String encima,
                         int y) {
    }

    /** Un piso del dibujo: su altura (el bloque del piso), qué tan alto es por dentro y sus filas. */
    public record Piso(int y, int alto, int ox, int oz, List<String> filas) {
        char en(int x, int z) {
            int f = z - oz, c = x - ox;
            if (f < 0 || f >= filas.size()) {
                return ' ';
            }
            String s = filas.get(f);
            return c < 0 || c >= s.length() ? ' ' : s.charAt(c);
        }
    }

    public final Map<Character, Estilo> estilos = new LinkedHashMap<>();
    public final Map<Character, Mueble> muebles = new LinkedHashMap<>();
    public final Map<Character, String[]> puntos = new HashMap<>();
    public final List<Piso> pisos = new ArrayList<>();
    /** Avisos al construir (escaleras de largo raro, letras sin definir...). */
    public final List<String> avisos = new ArrayList<>();
    private String prefijoEscondites = "esc.";
    private int escondites;
    private final List<int[]> escalones = new ArrayList<>();

    // ------------------------------------------------------------------ leer el archivo

    public static Casa leer(String recurso) {
        InputStream in = Casa.class.getResourceAsStream(recurso);
        if (in == null) {
            throw new IllegalStateException("no está el plano " + recurso);
        }
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            Casa c = new Casa();
            String l;
            Piso actual = null;
            while ((l = r.readLine()) != null) {
                if (actual != null) {
                    if (l.trim().equals("fin")) {
                        c.pisos.add(actual);
                        actual = null;
                    } else {
                        actual.filas().add(l);
                    }
                    continue;
                }
                String t = l.trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                String[] p = t.split("\\s+");
                switch (p[0]) {
                    case "incluir" -> {
                        Casa o = leer(p[1]);
                        c.estilos.putAll(o.estilos);
                        c.muebles.putAll(o.muebles);
                        c.puntos.putAll(o.puntos);
                    }
                    case "estilo" -> c.estilo(p);
                    case "mueble" -> c.mueble(p);
                    case "punto" -> c.puntos.put(p[1].charAt(0), new String[] {p[2], p.length > 3 ? p[3] : "0"});
                    case "marcas" -> c.prefijoEscondites = p[1];
                    case "piso" -> actual = new Piso(Integer.parseInt(p[1]), Integer.parseInt(p[3]), Integer.parseInt(p[5]),
                            Integer.parseInt(p[6]), new ArrayList<>());
                    default -> c.avisos.add("no entiendo: " + t);
                }
            }
            return c;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, String> pares(String[] p, int desde) {
        Map<String, String> m = new HashMap<>();
        for (int i = desde; i < p.length; i++) {
            int k = p[i].indexOf('=');
            m.put(k < 0 ? p[i] : p[i].substring(0, k), k < 0 ? "" : p[i].substring(k + 1));
        }
        return m;
    }

    private void estilo(String[] p) {
        Map<String, String> m = pares(p, 2);
        String[] piso = m.getOrDefault("piso", C + "parquet").split("\\|");
        estilos.put(p[1].charAt(0), new Estilo(p[1].charAt(0), m.getOrDefault("nombre", p[1]).replace('_', ' '), piso[0],
                piso.length > 1 ? piso[1] : piso[0], m.getOrDefault("pared", C + "papel_tapiz"), m.getOrDefault("zoclo", C + "madera_pulida"),
                m.getOrDefault("techo", C + "madera_pulida"), Integer.parseInt(m.getOrDefault("luz", "9")), m.getOrDefault("alfombra", "")));
    }

    private void mueble(String[] p) {
        Map<String, String> m = pares(p, 3);
        muebles.put(p[1].charAt(0), new Mueble(p[1].charAt(0), p[2], Integer.parseInt(m.getOrDefault("alto", "1")), m.getOrDefault("mira", "pared"),
                m.containsKey("cama"), m.containsKey("escondite"), m.containsKey("doble"), m.getOrDefault("encima", ""),
                Integer.parseInt(m.getOrDefault("y", "1"))));
    }

    // ------------------------------------------------------------------ construir

    /** Construye todos los pisos en el plano (de abajo hacia arriba) y al final abre el aire encima de las escaleras. */
    public void construir(Plano p) {
        Estilo base = estilos.isEmpty() ? new Estilo('?', "?", C + "parquet", C + "parquet", C + "papel_tapiz", C + "madera_pulida",
                C + "madera_pulida", 9, "") : estilos.values().iterator().next();
        for (Piso f : pisos) {
            for (int fi = 0; fi < f.filas().size(); fi++) {
                String fila = f.filas().get(fi);
                for (int ci = 0; ci < fila.length(); ci++) {
                    int x = f.ox() + ci, z = f.oz() + fi;
                    celda(p, f, x, z, fila.charAt(ci), base);
                }
            }
            luces(p, f);
        }
        // el aire encima de cada escalón (tres de alto): abre el techo/piso de arriba si estorba
        for (int[] e : escalones) {
            for (int y = e[1] + 1; y <= e[1] + 3; y++) {
                String s = p.get(e[0], y, e[2]);
                if (s == null || !s.contains("escalera") && !s.contains("_stairs")) {
                    p.set(e[0], y, e[2], "air");
                }
            }
        }
    }

    /** El estilo del cuarto de al lado (para el piso de una puerta, un mueble o un muro). */
    private Estilo vecino(Piso f, int x, int z, Estilo base) {
        int[][] d = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}, {0, 2}, {0, -2}, {2, 0}, {-2, 0}};
        for (int[] q : d) {
            Estilo e = estilos.get(f.en(x + q[0], z + q[1]));
            if (e != null) {
                return e;
            }
        }
        return base;
    }

    private static String piso(Estilo e, int x, int z) {
        return (x + z) % 2 == 0 ? e.piso() : e.piso2();
    }

    private void celda(Plano p, Piso f, int x, int z, char c, Estilo base) {
        int y0 = f.y(), alto = f.alto();
        if (c == ' ') {
            return;
        }
        Estilo est = estilos.get(c);
        if (est != null) {
            cuarto(p, x, z, y0, alto, est);
            return;
        }
        Estilo v = vecino(f, x, z, base);
        switch (c) {
            case '#' -> muro(p, x, z, y0, alto, v, false);
            case 'X' -> {
                for (int y = y0; y <= y0 + alto + 1; y++) {
                    p.set(x, y, z, C + "ladrillo_viejo");
                }
            }
            case 'W' -> {
                for (int y = y0; y <= y0 + alto + 1; y++) {
                    p.set(x, y, z, y == y0 + 2 || y == y0 + 3 ? C + "vidrio_sucio" : C + "ladrillo_viejo");
                }
            }
            case 'D' -> {
                muro(p, x, z, y0, alto, v, true);
                boolean muroEnX = esMuro(f.en(x - 1, z)) || esMuro(f.en(x + 1, z));
                String mira = muroEnX ? "south" : "east";
                p.set(x, y0 + 1, z, "minecraft:dark_oak_door[facing=" + mira + ",half=lower,hinge=left,open=false]");
                p.set(x, y0 + 2, z, "minecraft:dark_oak_door[facing=" + mira + ",half=upper,hinge=left,open=false]");
            }
            case '|' -> {
                muro(p, x, z, y0, alto, v, true);
                for (int y = y0 + 1; y <= y0 + 3; y++) {
                    p.set(x, y, z, "air");
                }
            }
            case '~' -> {
                // el hueco: aire en todo este piso (se ve el cuarto de abajo); el techo de arriba sí va
                for (int y = y0; y <= y0 + alto; y++) {
                    p.set(x, y, z, "air");
                }
                p.set(x, y0 + alto + 1, z, v.techo());
            }
            case ':' -> {
                cuarto(p, x, z, y0, alto, v);
                p.set(x, y0 + 1, z, C + "barandal");
            }
            case '@' -> {
                cuarto(p, x, z, y0, alto, v);
                p.marca(prefijoEscondites + escondites++, x, y0 + 1, z, 0);
            }
            case '%' -> {
                cuarto(p, x, z, y0, alto, v);
                String mira = mira(f, x, z, "pared");
                p.set(x, y0 + 1, z, C + "cortina[facing=" + mira + ",half=lower]");
                p.set(x, y0 + 2, z, C + "cortina[facing=" + mira + ",half=upper]");
            }
            case '^', 'v', '<', '>' -> escalon(p, f, x, z, c, v);
            default -> {
                String[] pt = puntos.get(c);
                if (pt != null) {
                    cuarto(p, x, z, y0, alto, v);
                    p.marca(pt[0], x, y0 + 1, z, Float.parseFloat(pt[1]));
                    return;
                }
                Mueble m = muebles.get(c);
                if (m == null) {
                    avisos.add("letra sin definir '" + c + "' en (" + x + ", " + z + ")");
                    cuarto(p, x, z, y0, alto, v);
                    return;
                }
                cuarto(p, x, z, y0, alto, v);
                mueble(p, f, x, z, m);
            }
        }
    }

    private static boolean esMuro(char c) {
        return c == '#' || c == 'X' || c == 'W' || c == 'D' || c == '|';
    }

    /** Una casilla de cuarto: su piso, aire hasta el techo y el techo. */
    private static void cuarto(Plano p, int x, int z, int y0, int alto, Estilo e) {
        p.set(x, y0, z, piso(e, x, z));
        for (int y = y0 + 1; y <= y0 + alto; y++) {
            p.set(x, y, z, "air");
        }
        // la alfombra va ENCIMA del piso (una alfombra no aguanta a nadie: abajo siempre va piso de verdad)
        if (!e.alfombra().isEmpty()) {
            p.set(x, y0 + 1, z, e.alfombra());
        }
        p.set(x, y0 + alto + 1, z, e.techo());
    }

    /** Un muro de adentro: zoclo de madera abajo y el papel del cuarto; {@code hueco}: solo lo de arriba (para puertas y pasos). */
    private static void muro(Plano p, int x, int z, int y0, int alto, Estilo e, boolean hueco) {
        p.set(x, y0, z, piso(e, x, z));
        for (int y = y0 + 1; y <= y0 + alto; y++) {
            if (hueco && y <= y0 + 3) {
                continue;
            }
            p.set(x, y, z, y == y0 + 1 ? e.zoclo() : y == y0 + alto ? C + "marco_dorado" : e.pared());
        }
        p.set(x, y0 + alto + 1, z, e.techo());
    }

    /** Hacia dónde mira un mueble: hacia el cuarto (lejos del muro de al lado), o lo que diga el mueble. */
    private static String mira(Piso f, int x, int z, String modo) {
        switch (modo) {
            case "norte":
                return "north";
            case "sur":
                return "south";
            case "este":
                return "east";
            case "oeste":
                return "west";
            default:
                break;
        }
        // lejos del muro: si al norte hay muro, mira al sur...
        if (esMuro(f.en(x, z - 1)) || f.en(x, z - 1) == ' ') {
            return "south";
        }
        if (esMuro(f.en(x, z + 1)) || f.en(x, z + 1) == ' ') {
            return "north";
        }
        if (esMuro(f.en(x - 1, z)) || f.en(x - 1, z) == ' ') {
            return "east";
        }
        if (esMuro(f.en(x + 1, z)) || f.en(x + 1, z) == ' ') {
            return "west";
        }
        return "south";
    }

    private static final String[] LADOS = {"north", "south", "east", "west"};
    private static final int[][] DIR = {{0, -1}, {0, 1}, {1, 0}, {-1, 0}};

    private void mueble(Plano p, Piso f, int x, int z, Mueble m) {
        int y0 = f.y();
        String props = "";
        String b = m.bloque();
        int k = b.indexOf('[');
        if (k >= 0) {
            props = b.substring(k + 1, b.length() - 1);
            b = b.substring(0, k);
        }
        String mira = mira(f, x, z, m.mira());
        if ("centro".equals(m.mira()) || "mesa".equals(m.mira())) {
            // una silla mira hacia la mesa (o el mueble) de al lado
            for (int i = 0; i < 4; i++) {
                char c = f.en(x + DIR[i][0], z + DIR[i][1]);
                Mueble o = muebles.get(c);
                if (o != null && o.letra() != m.letra()) {
                    mira = LADOS[i];
                    break;
                }
            }
        }
        if (m.cama()) {
            // la cama: dos casillas; la cabecera es la que está junto al muro
            for (int i = 0; i < 4; i++) {
                if (f.en(x + DIR[i][0], z + DIR[i][1]) == m.letra()) {
                    boolean cabecera = esMuro(f.en(x - DIR[i][0], z - DIR[i][1]));
                    String hacia = LADOS[i ^ 1];
                    // facing = hacia los pies
                    String facing = cabecera ? hacia : LADOS[i];
                    p.set(x, y0 + 1, z, b + "[facing=" + facing + ",part=" + (cabecera ? "head" : "foot") + "]");
                    return;
                }
            }
        }
        String conMira = props.isEmpty() ? "facing=" + mira : props;
        boolean sinMira = b.endsWith("bookshelf") || b.endsWith("_planks") || b.endsWith("barrel") && props.isEmpty() || props.equals("-");
        String bloque = sinMira ? b : b + "[" + conMira + "]";
        int yb = y0 + m.y();
        if (m.doble()) {
            p.set(x, yb, z, b + "[" + conMira + ",half=lower]");
            p.set(x, yb + 1, z, b + "[" + conMira + ",half=upper]");
        } else {
            for (int i = 0; i < m.alto(); i++) {
                p.set(x, yb + i, z, bloque);
            }
        }
        if (!m.encima().isEmpty()) {
            // algo encima del mueble (una copa sobre la mesa, una muñeca sobre el cajón)
            p.set(x, yb + (m.doble() ? 2 : m.alto()), z, m.encima().replace("{mira}", mira));
        }
        if (m.escondite()) {
            p.marca(prefijoEscondites + escondites++, x, y0 + 1, z, 0);
        }
    }

    /** Un escalón: sube hacia {@code c}; su altura es cuántos escalones lleva la fila desde abajo. */
    private void escalon(Plano p, Piso f, int x, int z, char c, Estilo v) {
        int dx = c == '>' ? 1 : c == '<' ? -1 : 0, dz = c == 'v' ? 1 : c == '^' ? -1 : 0;
        int n = 1;
        while (f.en(x - dx * n, z - dz * n) == c) {
            n++;
        }
        String facing = dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west";
        int y0 = f.y();
        p.set(x, y0, z, piso(v, x, z));
        for (int y = y0 + 1; y < y0 + n; y++) {
            p.set(x, y, z, C + "tablon");
        }
        p.set(x, y0 + n, z, C + "escalera_tablon[facing=" + facing + "]");
        // arriba del escalón, aire hasta el techo de este piso (y el techo)
        for (int y = y0 + n + 1; y <= y0 + f.alto(); y++) {
            p.set(x, y, z, "air");
        }
        p.set(x, y0 + f.alto() + 1, z, v.techo());
        escalones.add(new int[] {x, y0 + n, z});
        // el largo de la fila: si es la de hasta arriba, revisa que llegue al piso de arriba
        if (f.en(x + dx, z + dz) != c && n != f.alto()) {
            avisos.add("escalera de " + n + " escalones en (" + x + ", " + z + "): para subir un piso de " + f.alto()
                    + " de alto hacen falta " + f.alto());
        }
    }

    /**
     * La luz: bloques de luz invisibles cada 5 (oscuro en los rincones, como una casa vieja), a 4 del piso (o bajo el techo si el cuarto
     * es más bajo o ahí hay algo): en los cuartos altos, desde el techo casi no llegaba al piso.
     */
    private void luces(Plano p, Piso f) {
        for (int fi = 0; fi < f.filas().size(); fi++) {
            String fila = f.filas().get(fi);
            for (int ci = 0; ci < fila.length(); ci++) {
                int x = f.ox() + ci, z = f.oz() + fi;
                Estilo e = estilos.get(fila.charAt(ci));
                if (e == null || e.luz() <= 0 || Math.floorMod(x, 5) != 2 || Math.floorMod(z, 5) != 2) {
                    continue;
                }
                int y = f.y() + Math.min(f.alto(), 4);
                String ahi = p.get(x, y, z);
                if (ahi != null && !ahi.equals("air")) {
                    y = f.y() + f.alto();
                }
                p.set(x, y, z, "light[level=" + e.luz() + "]");
            }
        }
    }

    public int escondites() {
        return escondites;
    }
}
