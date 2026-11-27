package com.coleccionista.mapa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Revisa un {@link Plano} sin Minecraft, como si alguien lo caminara: desde la entrada, ¿a dónde se puede llegar a pie? Encuentra lo que
 * en el juego se ve como "salas rotas":
 * <ul>
 * <li><b>islas</b>: lugares donde se puede estar parado pero a los que no se llega caminando desde la entrada (cuartos inaccesibles);</li>
 * <li><b>marcas inaccesibles</b>: escondites, puntos de aparición o asientos a los que no se llega (o donde no cabe una persona);</li>
 * <li><b>escalones raros</b>: dentro de un mismo cuarto, el piso cambia de altura un bloque sin escalón ni losa (pisos desfasados);</li>
 * <li><b>hoyos</b>: un bloque de piso que falta (un hueco de 1×1 donde se atora uno);</li>
 * <li><b>puertas malas</b>: puertas sin piso, sin espacio a un lado o tapadas.</li>
 * </ul>
 * Las formas de los bloques del complejo salen de {@code deco.json} (quién choca y qué tan alto es); las de Minecraft, por su nombre. Una
 * persona cabe en una casilla si sus dos bloques (pies y cabeza) no chocan y abajo hay algo donde pararse. Camina a las cuatro casillas de
 * al lado subiendo hasta un bloque (saltando) o bajando hasta tres; por las escaleras de mano sube y baja.
 */
public final class Revisor {
    /** Cómo es un bloque para caminar: si choca, qué tan alto llega (0..1), y si es puerta, escalera de mano, escalón o losa. */
    public record Forma(boolean choca, double alto, boolean puerta, boolean mano, boolean escalon, boolean mueble) {
        public Forma(boolean choca, double alto, boolean puerta, boolean mano, boolean escalon) {
            this(choca, alto, puerta, mano, escalon, false);
        }

        static final Forma AIRE = new Forma(false, 0, false, false, false);
        static final Forma LLENO = new Forma(true, 1, false, false, false);
        static final Forma MUEBLE = new Forma(true, 1, false, false, false, true);
    }

    /** Un problema encontrado: qué es, dónde y algo de detalle. */
    public record Problema(String tipo, int x, int y, int z, String detalle) {
        @Override
        public String toString() {
            return tipo + " en (" + x + ", " + y + ", " + z + ")" + (detalle.isEmpty() ? "" : ": " + detalle);
        }
    }

    private final Plano p;
    private final Map<String, Forma> deco;
    private final Map<String, Forma> cache = new HashMap<>();
    /** Las casillas donde cabe alguien parado (pies) y las que se alcanzan caminando desde la entrada. */
    public final Set<Long> parado = new HashSet<>(), alcanzado = new HashSet<>();
    public final List<Problema> problemas = new ArrayList<>();
    /** Las islas (grupos de casillas donde se puede estar pero no se llega): su tamaño y una casilla de ejemplo. */
    public final List<long[]> islas = new ArrayList<>();

    public Revisor(Plano p, Map<String, Forma> deco) {
        this.p = p;
        this.deco = deco;
    }

    // ------------------------------------------------------------------ las formas

    /** Lee las formas de los bloques del complejo de {@code deco.json} (sin librerías: solo lo que hace falta). */
    public static Map<String, Forma> leerDeco(Path deco) throws IOException {
        String s = Files.readString(deco, StandardCharsets.UTF_8);
        Map<String, Forma> out = new HashMap<>();
        // cada bloque es un objeto {...} dentro de "blocks"; se recorre contando llaves
        int i = s.indexOf("\"blocks\"");
        i = s.indexOf('[', i);
        int prof = 0, ini = -1;
        for (int k = i + 1; k < s.length(); k++) {
            char c = s.charAt(k);
            if (c == '{') {
                if (prof == 0) {
                    ini = k;
                }
                prof++;
            } else if (c == '}') {
                prof--;
                if (prof == 0 && ini >= 0) {
                    leerBloque(s.substring(ini, k + 1), out);
                    ini = -1;
                }
            } else if (c == ']' && prof == 0) {
                break;
            }
        }
        return out;
    }

    private static final Pattern ID = Pattern.compile("\"id\"\\s*:\\s*\"([a-z0-9_]+)\"");
    private static final Pattern KIND = Pattern.compile("\"kind\"\\s*:\\s*\"([a-z0-9_]+)\"");
    private static final Pattern NO_CHOCA = Pattern.compile("\"collide\"\\s*:\\s*false");
    private static final Pattern SHAPE = Pattern.compile("\"shape\"\\s*:\\s*\\[(.*?)\\]\\s*\\]", Pattern.DOTALL);
    private static final Pattern NUM = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?");

    private static void leerBloque(String o, Map<String, Forma> out) {
        Matcher m = ID.matcher(o);
        if (!m.find()) {
            return;
        }
        String id = m.group(1);
        Matcher k = KIND.matcher(o);
        String kind = k.find() ? k.group(1) : "cubo";
        boolean choca = !NO_CHOCA.matcher(o).find();
        Forma f;
        switch (kind) {
            case "alfombra", "capas" -> f = new Forma(false, 0.0625, false, false, false);
            case "losa" -> f = new Forma(true, 0.5, false, false, true);
            case "escalera" -> f = new Forma(true, 1, false, false, true);
            case "trampilla" -> f = new Forma(false, 0.1875, false, false, false);
            case "palanca", "cadena" -> f = Forma.AIRE;
            case "barrotes", "reja" -> f = new Forma(true, 1.5, false, false, false);
            case "tubo", "pilar" -> f = Forma.MUEBLE;
            default -> {
                if (!choca) {
                    f = Forma.AIRE;
                } else if ("prop".equals(kind)) {
                    double alto = 1;
                    Matcher sh = SHAPE.matcher(o);
                    if (sh.find()) {
                        alto = 0;
                        // cajas de 6 números (x0 y0 z0 x1 y1 z1, en px): la y de arriba es la 5a
                        Matcher n = NUM.matcher(sh.group(1));
                        List<Double> v = new ArrayList<>();
                        while (n.find()) {
                            v.add(Double.parseDouble(n.group()));
                        }
                        for (int j = 0; j + 5 < v.size(); j += 6) {
                            alto = Math.max(alto, v.get(j + 4) / 16.0);
                        }
                    }
                    f = new Forma(true, Math.min(1, alto), false, false, false, true);
                } else {
                    f = Forma.LLENO;
                }
            }
        }
        out.put(id, f);
    }

    /** La forma del bloque que hay en ese lugar (nada = aire). */
    public Forma forma(int x, int y, int z) {
        String s = p.get(x, y, z);
        if (s == null) {
            return Forma.AIRE;
        }
        return cache.computeIfAbsent(s, this::calcular);
    }

    private Forma calcular(String s) {
        String props = "";
        int c = s.indexOf('[');
        if (c >= 0) {
            props = s.substring(c);
            s = s.substring(0, c);
        }
        boolean nuestro = s.startsWith("coleccionista:");
        String n = s.substring(s.indexOf(':') + 1);
        if (n.equals("air") || n.equals("cave_air") || n.equals("light") || n.equals("void_air")) {
            return Forma.AIRE;
        }
        if (nuestro) {
            Forma f = deco.get(n);
            if (f == null) {
                // pantalla y teclado: la pantalla es un bloque lleno; el teclado va en la pared y no estorba
                return n.equals("teclado") ? Forma.AIRE : Forma.LLENO;
            }
            if (f.escalon() && f.alto() == 0.5 && props.contains("type=top")) {
                return new Forma(true, 1, false, false, false);
            }
            if (f.escalon() && f.alto() == 0.5 && props.contains("type=double")) {
                return Forma.LLENO;
            }
            if (deco.containsKey(n) && n.contains("puerta")) {
                return new Forma(false, 0, true, false, false);
            }
            return f;
        }
        if (n.endsWith("_door") || n.equals("door")) {
            return new Forma(false, 0, true, false, false);
        }
        if (n.endsWith("trapdoor")) {
            return props.contains("open=true") ? Forma.AIRE : new Forma(false, 0.1875, false, false, false);
        }
        if (n.equals("ladder") || n.endsWith("vine") || n.equals("scaffolding")) {
            return new Forma(false, 0, false, true, false);
        }
        if (n.endsWith("_carpet") || n.equals("snow") || n.endsWith("pressure_plate")) {
            return new Forma(false, 0.0625, false, false, false);
        }
        if (n.endsWith("_slab")) {
            if (props.contains("type=top")) {
                return new Forma(true, 1, false, false, false);
            }
            return props.contains("type=double") ? Forma.LLENO : new Forma(true, 0.5, false, false, true);
        }
        if (n.endsWith("_stairs")) {
            return new Forma(true, 1, false, false, true);
        }
        if (n.contains("torch") || n.contains("sign") || n.contains("banner") || n.contains("button") || n.equals("lever")
                || n.contains("flower") || n.equals("grass") || n.equals("tall_grass") || n.contains("fern") || n.contains("rail")
                || n.contains("redstone_wire") || n.contains("tripwire") || n.equals("cobweb") || n.contains("sapling") || n.contains("_head")
                || n.contains("skull") || n.equals("fire") || n.equals("water") || n.equals("structure_void") || n.contains("coral")
                || n.contains("mushroom") && !n.contains("block")) {
            return Forma.AIRE;
        }
        if (n.equals("bookshelf") || n.endsWith("_bed") || n.equals("cauldron") || n.equals("smoker") || n.equals("furnace") || n.equals("note_block")
                || n.equals("jukebox") || n.equals("barrel") || n.equals("chest") || n.endsWith("leaves") || n.startsWith("potted_")
                || n.equals("lectern") || n.equals("crafting_table") || n.equals("loom") || n.equals("hay_block")) {
            return Forma.MUEBLE;
        }
        if (n.endsWith("fence") || n.endsWith("_wall") || n.endsWith("fence_gate") || n.endsWith("iron_bars") || n.endsWith("glass_pane")) {
            return new Forma(true, 1.5, false, false, false);
        }
        return Forma.LLENO;
    }

    // ------------------------------------------------------------------ dónde cabe alguien

    /** ¿Choca a la altura del cuerpo? (lo bajito, como una alfombra o una trampilla en el piso, no estorba) */
    private boolean estorba(int x, int y, int z) {
        Forma f = forma(x, y, z);
        return f.choca() && f.alto() > 0.25;
    }

    /** ¿Se puede parar aquí (pies en y)? Sus dos bloques libres y algo firme abajo (o un escalón, una losa o una escalera de mano). */
    public boolean cabe(int x, int y, int z) {
        Forma pies = forma(x, y, z), cabeza = forma(x, y + 1, z);
        if (pies.choca() && pies.alto() > 0.5 || cabeza.choca()) {
            return false;
        }
        if (pies.choca() && pies.alto() > 0) {
            // parado encima de una losa o un escalón bajo: necesita un bloque más arriba libre
            return !forma(x, y + 2, z).choca();
        }
        Forma abajo = forma(x, y - 1, z);
        if (abajo.alto() > 1) {
            // encima de un barandal o una cerca no se para nadie (no se alcanza de un salto)
            return false;
        }
        return abajo.choca() && abajo.alto() >= 0.5 || pies.alto() > 0 || pies.mano() || abajo.mano();
    }

    /** Recorre todo: dónde se puede estar y a dónde se llega desde {@code (x, y, z)} (los pies de alguien en la entrada). */
    public Revisor caminar(int x, int y, int z) {
        for (int yy = p.y0; yy <= p.y1; yy++) {
            for (int xx = p.x0; xx <= p.x1; xx++) {
                for (int zz = p.z0; zz <= p.z1; zz++) {
                    if (cabe(xx, yy, zz)) {
                        parado.add(Plano.key(xx, yy, zz));
                    }
                }
            }
        }
        if (!parado.contains(Plano.key(x, y, z))) {
            problemas.add(new Problema("entrada sin lugar", x, y, z, "ahí no cabe nadie parado"));
            return this;
        }
        ArrayDeque<Long> cola = new ArrayDeque<>();
        cola.add(Plano.key(x, y, z));
        alcanzado.add(Plano.key(x, y, z));
        int[][] lados = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!cola.isEmpty()) {
            long k = cola.poll();
            int cx = Plano.kx(k), cy = Plano.ky(k), cz = Plano.kz(k);
            // escaleras de mano: arriba y abajo
            if (forma(cx, cy, cz).mano() || forma(cx, cy + 1, cz).mano()) {
                for (int dy : new int[] {1, -1}) {
                    long q = Plano.key(cx, cy + dy, cz);
                    if ((parado.contains(q) || forma(cx, cy + dy, cz).mano() && !forma(cx, cy + dy + 1, cz).choca()) && alcanzado.add(q)) {
                        cola.add(q);
                    }
                }
            }
            boolean techoAlto = !forma(cx, cy + 2, cz).choca();
            for (int[] d : lados) {
                int nx = cx + d[0], nz = cz + d[1];
                // subir uno (saltando, o por un escalón/losa), mismo nivel, o bajar hasta tres
                for (int dy = 1; dy >= -3; dy--) {
                    int ny = cy + dy;
                    if (dy == 1 && !techoAlto) {
                        continue;
                    }
                    if (dy < 0) {
                        // para bajar, el espacio de enfrente tiene que estar libre hasta ahí
                        boolean libre = true;
                        for (int yy = ny + 1; yy <= cy + 1 && libre; yy++) {
                            libre = !estorba(nx, yy, nz);
                        }
                        if (!libre) {
                            break;
                        }
                    }
                    long q = Plano.key(nx, ny, nz);
                    if (parado.contains(q)) {
                        if (dy == 1 && estorba(nx, cy + 2, nz)) {
                            continue;
                        }
                        if (alcanzado.add(q)) {
                            cola.add(q);
                        }
                        break;
                    }
                }
            }
        }
        return this;
    }

    // ------------------------------------------------------------------ lo que se revisa

    /** Cajas que no se revisan como islas (x0, z0, x1, z1): lugares cerrados a propósito, como la jaula de Cascabel. */
    private final List<int[]> cerrados = new ArrayList<>();

    public Revisor cerrado(int x0, int z0, int x1, int z1) {
        cerrados.add(new int[] {x0, z0, x1, z1});
        return this;
    }

    /** Lo que no cuenta como isla aunque no se llegue (techos, bardas, setos...): se prueba con {x, y, z} (los pies). */
    private java.util.function.Predicate<int[]> ignorar = c -> false;

    public Revisor ignorarIslas(java.util.function.Predicate<int[]> p) {
        ignorar = p;
        return this;
    }

    private boolean enCerrado(int x, int z) {
        for (int[] c : cerrados) {
            if (x >= c[0] && x <= c[2] && z >= c[1] && z <= c[3]) {
                return true;
            }
        }
        return false;
    }

    /**
     * Las islas: grupos de casillas donde se puede estar, pero no se llega (solo los de {@code minimo} casillas o más). No cuentan los que
     * están todos encima de muebles (arriba de un librero o una vitrina) ni los de los lugares {@link #cerrado}.
     */
    public Revisor islas(int minimo, int yMin, int yMax) {
        Set<Long> vistos = new HashSet<>(alcanzado);
        for (long k : parado) {
            if (vistos.contains(k) || Plano.ky(k) < yMin || Plano.ky(k) > yMax) {
                continue;
            }
            // un grupo: las casillas paradas vecinas al mismo nivel (o a uno de diferencia)
            List<Long> grupo = new ArrayList<>();
            ArrayDeque<Long> cola = new ArrayDeque<>();
            cola.add(k);
            vistos.add(k);
            while (!cola.isEmpty()) {
                long a = cola.poll();
                grupo.add(a);
                int ax = Plano.kx(a), ay = Plano.ky(a), az = Plano.kz(a);
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    for (int dy = -1; dy <= 1; dy++) {
                        long q = Plano.key(ax + d[0], ay + dy, az + d[1]);
                        if (parado.contains(q) && vistos.add(q)) {
                            cola.add(q);
                        }
                    }
                }
            }
            boolean enMuebles = true, cerrado = true;
            for (long a : grupo) {
                enMuebles &= forma(Plano.kx(a), Plano.ky(a) - 1, Plano.kz(a)).mueble();
                cerrado &= enCerrado(Plano.kx(a), Plano.kz(a)) || ignorar.test(new int[] {Plano.kx(a), Plano.ky(a), Plano.kz(a)});
            }
            if (grupo.size() >= minimo && !enMuebles && !cerrado) {
                int x0 = Integer.MAX_VALUE, z0 = Integer.MAX_VALUE, x1 = Integer.MIN_VALUE, z1 = Integer.MIN_VALUE, y0 = Integer.MAX_VALUE;
                for (long a : grupo) {
                    x0 = Math.min(x0, Plano.kx(a));
                    x1 = Math.max(x1, Plano.kx(a));
                    z0 = Math.min(z0, Plano.kz(a));
                    z1 = Math.max(z1, Plano.kz(a));
                    y0 = Math.min(y0, Plano.ky(a));
                }
                islas.add(new long[] {grupo.size(), k});
                problemas.add(new Problema("cuarto inaccesible", Plano.kx(k), Plano.ky(k), Plano.kz(k),
                        grupo.size() + " casillas, de (" + x0 + ", " + z0 + ") a (" + x1 + ", " + z1 + ") en y " + y0));
            }
        }
        return this;
    }

    /** Las marcas que empiezan con {@code prefijo}: tiene que caber alguien y se tiene que llegar caminando (o estar junto a donde se llega). */
    public Revisor marcas(String prefijo) {
        for (Map.Entry<String, Plano.Marca> e : p.marcas.entrySet()) {
            if (!e.getKey().startsWith(prefijo)) {
                continue;
            }
            Plano.Marca m = e.getValue();
            if (!cabe(m.x(), m.y(), m.z())) {
                problemas.add(new Problema("no cabe nadie", m.x(), m.y(), m.z(), e.getKey()));
            } else if (!alcanzado.contains(Plano.key(m.x(), m.y(), m.z()))) {
                problemas.add(new Problema("no se llega", m.x(), m.y(), m.z(), e.getKey()));
            }
        }
        return this;
    }

    /**
     * Dentro de la caja {@code x0..x1, z0..z1} y entre {@code yMin..yMax}: escalones de un bloque entre casillas vecinas a las que se llega
     * (sin escalón ni losa que lo explique) y hoyos de 1×1 en el piso.
     */
    public Revisor pisos(int x0, int z0, int x1, int z1, int yMin, int yMax) {
        Set<String> vistos = new HashSet<>();
        for (long k : alcanzado) {
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            if (x < x0 || x > x1 || z < z0 || z > z1 || y < yMin || y > yMax) {
                continue;
            }
            // un hoyo: alrededor (los cuatro lados) se está un bloque más arriba
            int altos = 0;
            for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                if (alcanzado.contains(Plano.key(x + d[0], y + 1, z + d[1]))) {
                    altos++;
                }
            }
            if (altos >= 3 && !forma(x, y + 1, z).choca() && !forma(x, y - 1, z).escalon()) {
                problemas.add(new Problema("hoyo en el piso", x, y - 1, z, "alrededor el piso está un bloque más arriba"));
            }
            // un escalón raro: al lado se está un bloque más arriba, sin escalón/losa en medio
            for (int[] d : new int[][] {{1, 0}, {0, 1}}) {
                int nx = x + d[0], nz = z + d[1];
                for (int dy : new int[] {1, -1}) {
                    if (!alcanzado.contains(Plano.key(nx, y + dy, nz))) {
                        continue;
                    }
                    int bajo = dy > 0 ? y : y + dy;
                    int bx = dy > 0 ? nx : x, bz = dy > 0 ? nz : z;
                    // lo que hace el escalón: el bloque bajo los pies del que está arriba
                    Forma f = forma(bx, bajo, bz);
                    Forma sa = forma(x, y - 1, z), sb = forma(nx, y + dy - 1, nz);
                    if (f.escalon() || forma(bx, bajo + 1, bz).escalon() || f.mueble() || sa.mueble() || sb.mueble() || sa.escalon()
                            || sb.escalon()) {
                        continue;
                    }
                    String clave = Math.min(x, nx) + "," + Math.min(z, nz) + "," + bajo;
                    if (vistos.add(clave)) {
                        problemas.add(new Problema("escalón raro", bx, bajo, bz, "el piso brinca un bloque sin escalón (¿piso desfasado?)"));
                    }
                }
            }
        }
        return this;
    }

    /** Las puertas: con piso, y con lugar para pararse a los dos lados (y que se llegue a uno de ellos). */
    public Revisor puertas() {
        for (Map.Entry<Long, String> e : p.m.entrySet()) {
            String s = e.getValue();
            if (!s.contains("door") && !s.contains("puerta") || s.contains("trapdoor") || s.contains("half=upper")) {
                continue;
            }
            long k = e.getKey();
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            if (!forma(x, y, z).puerta()) {
                continue;
            }
            if (!forma(x, y - 1, z).choca()) {
                problemas.add(new Problema("puerta sin piso", x, y, z, s));
                continue;
            }
            boolean ejeX = s.contains("facing=east") || s.contains("facing=west");
            int ax = ejeX ? 1 : 0, az = ejeX ? 0 : 1;
            boolean a = cabeCerca(x + ax, y, z + az), b = cabeCerca(x - ax, y, z - az);
            if (!a || !b) {
                problemas.add(new Problema("puerta tapada", x, y, z, (a ? "" : "un lado ") + (b ? "" : "el otro lado ") + "sin lugar"));
            } else if (!alcanzado.contains(Plano.key(x, y, z)) && !alcanzado.contains(Plano.key(x + ax, y, z + az))
                    && !alcanzado.contains(Plano.key(x - ax, y, z - az))) {
                problemas.add(new Problema("puerta a la que no se llega", x, y, z, s));
            }
        }
        return this;
    }

    private boolean cabeCerca(int x, int y, int z) {
        return cabe(x, y, z) || cabe(x, y + 1, z) || cabe(x, y - 1, z);
    }

    /**
     * Las puertas corredizas (de acero, de celda, de elevador): al abrirse, cada hoja se corre de lado. La hoja con bisagra izquierda se
     * corre hacia (según hacia dónde mire) el este si mira al norte, el sur si mira al este, el oeste si mira al sur y el norte si mira al
     * oeste; la derecha, al revés (así están los modelos "abierta_i" y "abierta_d"). Tiene que meterse en el muro: si se corre encima de la
     * otra hoja, la puerta abierta se ve cerrada y la gente la atraviesa como fantasma.
     */
    public Revisor corredizas() {
        for (Map.Entry<Long, String> e : p.m.entrySet()) {
            String s = e.getValue();
            if (!(s.startsWith("coleccionista:puerta_metal[") || s.startsWith("coleccionista:puerta_celda[")
                    || s.startsWith("coleccionista:puerta_ascensor[")) || !s.contains("half=lower")) {
                continue;
            }
            long k = e.getKey();
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            int[] d = s.contains("facing=north") ? new int[] {1, 0} : s.contains("facing=east") ? new int[] {0, 1}
                    : s.contains("facing=south") ? new int[] {-1, 0} : new int[] {0, -1};
            if (s.contains("hinge=right")) {
                d = new int[] {-d[0], -d[1]};
            }
            String al = p.get(x + d[0], y, z + d[1]);
            if (al != null && (al.contains("puerta") || al.contains("door"))) {
                problemas.add(new Problema("puerta corrediza abre encima de la otra hoja", x, y, z, s));
            } else if (!forma(x + d[0], y, z + d[1]).choca()) {
                problemas.add(new Problema("puerta corrediza abre al paso", x, y, z, s));
            }
        }
        return this;
    }

    // ------------------------------------------------------------------ muebles partidos

    private static String idDe(String b) {
        int i = b.indexOf('[');
        return i < 0 ? b : b.substring(0, i);
    }

    /**
     * Muebles de dos bloques partidos: la mitad de abajo de un mueble alto (o de una puerta) sin la de arriba o al revés (algo se puso
     * encima), y camas sin pies. En el juego la mitad sola desaparece o se ve rota.
     */
    public Revisor mitades() {
        for (Map.Entry<Long, String> e : p.m.entrySet()) {
            String v = e.getValue();
            long k = e.getKey();
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            if (v.contains("half=lower") || v.contains("half=upper")) {
                boolean abajo = v.contains("half=lower");
                String o = p.get(x, y + (abajo ? 1 : -1), z);
                if (o == null || !idDe(o).equals(idDe(v)) || !o.contains(abajo ? "half=upper" : "half=lower")) {
                    problemas.add(new Problema("mueble partido", x, y, z, v + (abajo ? " sin su mitad de arriba (" : " sin su mitad de abajo (") + o + ")"));
                }
            } else if (v.contains("part=head")) {
                boolean pies = false;
                for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    String o = p.get(x + d[0], y, z + d[1]);
                    pies |= o != null && idDe(o).equals(idDe(v)) && o.contains("part=foot");
                }
                if (!pies) {
                    problemas.add(new Problema("mueble partido", x, y, z, v + " sin los pies"));
                }
            }
        }
        return this;
    }

    // ------------------------------------------------------------------ las pantallas

    /** El tamaño de la imagen de una pantalla, como la dibuja el cliente (PantallaRenderer): {ancho, alto}. */
    public static float[] tamPantalla(String bloque, String nbt) {
        boolean grande = bloque.contains("grande=true");
        int tam = numero(nbt, "tam");
        if (grande && tam == 1) {
            return new float[] {7F, 4F};
        }
        if (grande && tam == 2) {
            return new float[] {4F, 3F};
        }
        return grande ? new float[] {11F, 7F} : new float[] {3F, 2F};
    }

    private static int numero(String nbt, String clave) {
        java.util.regex.Matcher mm = Pattern.compile(clave + ":(-?[0-9]+)").matcher(nbt == null ? "" : nbt);
        return mm.find() ? Integer.parseInt(mm.group(1)) : 0;
    }

    /** El corrimiento a la derecha (de quien la ve) de la imagen, en bloques ({@code dx} en el NBT). */
    public static float dxPantalla(String nbt) {
        java.util.regex.Matcher mm = Pattern.compile("dx:(-?[0-9.]+)f?").matcher(nbt == null ? "" : nbt);
        return mm.find() ? Float.parseFloat(mm.group(1)) : 0F;
    }

    /**
     * Las pantallas: la imagen (más grande que su bloque) no debe tapar una puerta ni un hueco por donde se camina (si no, la gente la
     * atraviesa), ni meterse en un muro (se corta). Se mira la capa de la pantalla y la de atrás, casilla por casilla.
     */
    public Revisor pantallas() {
        for (Map.Entry<Long, String> e : p.m.entrySet()) {
            String s = e.getValue();
            if (!s.startsWith("coleccionista:pantalla")) {
                continue;
            }
            long k = e.getKey();
            int x = Plano.kx(k), y = Plano.ky(k), z = Plano.kz(k);
            int nx = s.contains("facing=east") ? 1 : s.contains("facing=west") ? -1 : 0;
            int nz = s.contains("facing=south") ? 1 : s.contains("facing=north") ? -1 : 0;
            int rx = nz, rz = -nx;
            String nbt = p.nbt.get(k);
            float[] wh = tamPantalla(s, nbt);
            float w = wh[0], h = wh[1], dx = dxPantalla(nbt);
            double cx = x + 0.5 + rx * dx, cz = z + 0.5 + rz * dx, y0 = y + 0.5 - h / 2, y1 = y + 0.5 + h / 2;
            int alcance = (int) Math.ceil(w / 2 + Math.abs(dx)) + 1;
            int tapa = 0, corta = 0;
            String ejemplo = "";
            for (int capa = 0; capa <= 1; capa++) {
                for (int t = -alcance; t <= alcance; t++) {
                    int bx = x + rx * t - nx * capa, bz = z + rz * t - nz * capa;
                    double u = (bx + 0.5 - cx) * rx + (bz + 0.5 - cz) * rz;
                    double oh = Math.min(u + 0.5, w / 2.0) - Math.max(u - 0.5, -w / 2.0);
                    if (oh < 0.5) {
                        continue;
                    }
                    for (int by = (int) Math.floor(y0); by < y1; by++) {
                        double ov = Math.min(by + 1, y1) - Math.max(by, y0);
                        if (ov < 0.5 || capa == 0 && bx == x && by == y && bz == z) {
                            continue;
                        }
                        Forma f = forma(bx, by, bz);
                        String b = p.get(bx, by, bz);
                        boolean puerta = f.puerta() || b != null && (b.contains("door") || b.contains("puerta")) && !b.contains("trapdoor");
                        if (capa == 1 && (puerta || !f.choca() && (cabe(bx, by, bz) || cabe(bx, by - 1, bz)))) {
                            tapa++;
                            ejemplo = bx + " " + by + " " + bz + (puerta ? " (puerta)" : " (hueco)");
                        } else if (capa == 0 && f.choca() && f.alto() >= 1 && !f.mueble() && !b.startsWith("coleccionista:pantalla")) {
                            corta++;
                        }
                    }
                }
            }
            if (tapa > 0) {
                problemas.add(new Problema("pantalla tapa el paso", x, y, z, tapa + " casillas detrás de la imagen, p. ej. " + ejemplo));
            }
            if (corta > 0) {
                problemas.add(new Problema("pantalla metida en el muro", x, y, z, corta + " casillas de muro dentro de la imagen"));
            }
        }
        return this;
    }

    /** Un resumen por tipo (y los primeros de cada uno). */
    public String informe(String titulo, int porTipo) {
        Map<String, List<Problema>> t = new LinkedHashMap<>();
        for (Problema q : problemas) {
            t.computeIfAbsent(q.tipo(), x -> new ArrayList<>()).add(q);
        }
        StringBuilder sb = new StringBuilder("== " + titulo + ": " + alcanzado.size() + " casillas a las que se llega, " + problemas.size()
                + " problemas\n");
        for (Map.Entry<String, List<Problema>> e : t.entrySet()) {
            sb.append("  ").append(e.getKey()).append(": ").append(e.getValue().size()).append('\n');
            for (int i = 0; i < Math.min(porTipo, e.getValue().size()); i++) {
                sb.append("    - ").append(e.getValue().get(i)).append('\n');
            }
        }
        return sb.toString();
    }

    public long cuantos(String tipo) {
        return problemas.stream().filter(q -> q.tipo().equals(tipo)).count();
    }
}
