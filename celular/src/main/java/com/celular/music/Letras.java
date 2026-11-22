package com.celular.music;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Busca la letra de una canción, en este orden:
 * <ol>
 * <li>un archivo <code>.lrc</code> (o <code>.txt</code>) con el mismo nombre junto a la canción, o en una carpeta <code>letras</code>;</li>
 * <li>la letra guardada dentro de la canción (MP3: marcos SYLT y USLT; OGG: comentarios LYRICS);</li>
 * <li>lo que ya se bajó antes (carpeta de caché);</li>
 * <li>internet: LRCLIB (lrclib.net, gratis), por título, artista y duración. Lo que encuentra se guarda en la caché.</li>
 * </ol>
 * Todo pasa en un hilo aparte ("Celular-Letras"); mientras tanto {@link #get} devuelve null. Sin nada de Minecraft.
 */
public final class Letras {
    private Letras() {
    }

    /** Lo que hace falta para buscar la letra de una canción. */
    public static final class Pedido {
        public final String clave;
        public final Path audio;
        public final String ext;
        public final String titulo;
        public final String artista;
        public final int segundos;

        public Pedido(String clave, Path audio, String ext, String titulo, String artista, int segundos) {
            this.clave = clave;
            this.audio = audio;
            this.ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
            this.titulo = titulo == null ? "" : titulo.trim();
            this.artista = artista == null ? "" : artista.trim();
            this.segundos = segundos;
        }
    }

    /** Carpeta de la caché (la pone el cliente: .minecraft/celular/letras). Null = sin caché. */
    public static volatile Path cache;
    /** ¿Se puede buscar en internet? (ajuste del celular) */
    public static volatile boolean internet = true;
    /** Dirección de LRCLIB (se puede cambiar en las pruebas). */
    public static volatile String servidor = "https://lrclib.net";
    /** Si no se encontró, se vuelve a intentar en internet después de este tiempo (ms). */
    static final long REINTENTO = 3L * 24 * 3600 * 1000;

    private static final Object NINGUNA = new Object();
    private static final Map<String, Object> RES = new HashMap<>();
    private static final ArrayDeque<Pedido> COLA = new ArrayDeque<>();
    private static Thread hilo;

    /** La letra si ya está (null mientras se busca o si no hay). La primera vez empieza a buscarla. */
    public static Letra get(Pedido p) {
        synchronized (RES) {
            Object r = RES.get(p.clave);
            if (r instanceof Letra l) {
                return l;
            }
            if (r == null) {
                RES.put(p.clave, Boolean.TRUE);       // buscando
                COLA.add(p);
                if (hilo == null || !hilo.isAlive()) {
                    hilo = new Thread(Letras::trabajar, "Celular-Letras");
                    hilo.setDaemon(true);
                    hilo.start();
                }
            }
            return null;
        }
    }

    /** "lista", "buscando", "ninguna" o "" (aún no se pidió). */
    public static String estado(String clave) {
        synchronized (RES) {
            Object r = RES.get(clave);
            return r == null ? "" : r instanceof Letra ? "lista" : r == NINGUNA ? "ninguna" : "buscando";
        }
    }

    /** Olvida lo encontrado (por ejemplo, para volver a buscar después de poner un .lrc). */
    public static void olvidar() {
        synchronized (RES) {
            RES.entrySet().removeIf(e -> e.getValue() != Boolean.TRUE);
        }
    }

    private static void trabajar() {
        while (true) {
            Pedido p;
            synchronized (RES) {
                p = COLA.poll();
                if (p == null) {
                    hilo = null;
                    return;
                }
            }
            Letra l = null;
            try {
                l = buscar(p);
            } catch (Throwable ignored) {
            }
            synchronized (RES) {
                RES.put(p.clave, l != null && !l.vacia() ? l : NINGUNA);
            }
        }
    }

    // ------------------------------------------------------------------ buscar (sin hilo: lo usan las pruebas)

    public static Letra buscar(Pedido p) throws IOException {
        // 1) archivo junto a la canción
        if (p.audio != null) {
            String txt = archivoJunto(p.audio);
            if (txt != null) {
                Letra l = Letra.de(txt, p.segundos);
                if (l != null) {
                    l.origen = "archivo";
                    return l;
                }
            }
            // 2) guardada dentro de la canción
            if (Files.isRegularFile(p.audio)) {
                txt = embebida(p.audio, p.ext);
                if (txt != null) {
                    Letra l = Letra.de(txt, p.segundos);
                    if (l != null) {
                        l.origen = "mp3";
                        return l;
                    }
                }
            }
        }
        // 3) caché
        Path c = cache;
        String nombre = seguro(p.clave);
        if (c != null) {
            for (String ext : new String[]{".lrc", ".txt"}) {
                Path f = c.resolve(nombre + ext);
                if (Files.isRegularFile(f)) {
                    Letra l = Letra.de(Files.readString(f, StandardCharsets.UTF_8), p.segundos);
                    if (l != null) {
                        l.origen = "cache";
                        return l;
                    }
                }
            }
            Path none = c.resolve(nombre + ".none");
            if (Files.isRegularFile(none) && System.currentTimeMillis() - Files.getLastModifiedTime(none).toMillis() < REINTENTO) {
                return null;
            }
        }
        // 4) internet
        if (!internet || p.titulo.isEmpty()) {
            return null;
        }
        Letra l = null;
        String[] r = lrclib(p);
        if (r != null) {
            l = r[0] != null ? Letra.lrc(r[0]) : null;
            if (l == null && r[1] != null) {
                l = Letra.plana(r[1], p.segundos);
            }
        }
        if (c != null) {
            try {
                Files.createDirectories(c);
                if (l != null) {
                    Files.writeString(c.resolve(nombre + (l.sincronizada ? ".lrc" : ".txt")), l.guardar(), StandardCharsets.UTF_8);
                } else if (r != null) {
                    Files.writeString(c.resolve(nombre + ".none"), "", StandardCharsets.UTF_8);   // respondió y no hay letra
                }
            } catch (IOException ignored) {
            }
        }
        if (l != null) {
            l.origen = "internet";
        }
        return l;
    }

    static String seguro(String clave) {
        StringBuilder b = new StringBuilder();
        for (char ch : clave.toCharArray()) {
            b.append(Character.isLetterOrDigit(ch) && ch < 128 ? ch : '_');
        }
        String s = b.toString();
        if (s.length() > 60) {
            s = s.substring(0, 40) + "_" + Integer.toHexString(clave.hashCode());
        }
        return s;
    }

    /** Un .lrc o .txt con el mismo nombre que la canción (junto a ella o en la carpeta "letras" de al lado). */
    static String archivoJunto(Path audio) throws IOException {
        Path dir = audio.getParent();
        if (dir == null) {
            return null;
        }
        String base = audio.getFileName().toString();
        int dot = base.lastIndexOf('.');
        if (dot > 0) {
            base = base.substring(0, dot);
        }
        for (Path d : new Path[]{dir, dir.resolve("letras"), dir.resolve("lyrics")}) {
            for (String ext : new String[]{".lrc", ".LRC", ".txt", ".TXT"}) {
                Path f = d.resolve(base + ext);
                if (Files.isRegularFile(f)) {
                    return leerTexto(Files.readAllBytes(f));
                }
            }
        }
        return null;
    }

    /** UTF-8 (con o sin BOM) o UTF-16 con BOM; si no es UTF-8 válido, Latin-1 (los .lrc viejos de Windows). */
    static String leerTexto(byte[] b) {
        if (b.length >= 2 && (b[0] & 255) == 0xFF && (b[1] & 255) == 0xFE) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        }
        if (b.length >= 2 && (b[0] & 255) == 0xFE && (b[1] & 255) == 0xFF) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        }
        int off = b.length >= 3 && (b[0] & 255) == 0xEF && (b[1] & 255) == 0xBB && (b[2] & 255) == 0xBF ? 3 : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(b, off, b.length - off)).toString();
        } catch (java.nio.charset.CharacterCodingException e) {
            return new String(b, off, b.length - off, StandardCharsets.ISO_8859_1);
        }
    }

    // ------------------------------------------------------------------ letra guardada dentro de la canción

    /** La letra de dentro de la canción, en texto (LRC si traía tiempos). Null si no tiene. */
    public static String embebida(Path p, String ext) throws IOException {
        if (ext.equals("mp3")) {
            return id3(p);
        }
        if (ext.equals("ogg")) {
            return vorbis(p);
        }
        return null;
    }

    private static String id3(Path p) throws IOException {
        byte[] head = new byte[10];
        byte[] tag;
        int ver;
        try (InputStream in = Files.newInputStream(p)) {
            if (in.readNBytes(head, 0, 10) < 10) {
                return null;
            }
            long total = Meta.id3Size(head);
            if (total <= 10 || total > 16_000_000) {
                return null;
            }
            ver = head[3];
            tag = in.readNBytes((int) total - 10);
        }
        String uslt = null, sylt = null;
        int i = 0;
        int idLen = ver == 2 ? 3 : 4, hdr = ver == 2 ? 6 : 10;
        while (i + hdr <= tag.length) {
            String id = new String(tag, i, idLen, StandardCharsets.ISO_8859_1);
            if (id.charAt(0) == 0) {
                break;
            }
            int size;
            if (ver == 2) {
                size = (tag[i + 3] & 255) << 16 | (tag[i + 4] & 255) << 8 | (tag[i + 5] & 255);
            } else if (ver >= 4) {
                size = (tag[i + 4] & 0x7F) << 21 | (tag[i + 5] & 0x7F) << 14 | (tag[i + 6] & 0x7F) << 7 | (tag[i + 7] & 0x7F);
            } else {
                size = (tag[i + 4] & 255) << 24 | (tag[i + 5] & 255) << 16 | (tag[i + 6] & 255) << 8 | (tag[i + 7] & 255);
            }
            int start = i + hdr;
            if (size <= 0 || start + size > tag.length) {
                break;
            }
            if ((id.equals("USLT") || id.equals("ULT")) && uslt == null) {
                uslt = uslt(tag, start, size);
            } else if ((id.equals("SYLT") || id.equals("SLT")) && sylt == null) {
                sylt = sylt(tag, start, size);
            }
            i = start + size;
        }
        if (sylt != null && !sylt.isBlank()) {
            return sylt;
        }
        return uslt != null && !uslt.isBlank() ? uslt : null;
    }

    private static Charset cs(int enc) {
        return switch (enc) {
            case 1 -> StandardCharsets.UTF_16;
            case 2 -> StandardCharsets.UTF_16BE;
            case 3 -> StandardCharsets.UTF_8;
            default -> StandardCharsets.ISO_8859_1;
        };
    }

    /** Fin de un texto terminado en cero (dos ceros alineados en UTF-16). */
    private static int fin(byte[] b, int from, int to, int enc) {
        if (enc == 1 || enc == 2) {
            for (int k = from; k + 1 < to; k += 2) {
                if (b[k] == 0 && b[k + 1] == 0) {
                    return k;
                }
            }
            return to;
        }
        for (int k = from; k < to; k++) {
            if (b[k] == 0) {
                return k;
            }
        }
        return to;
    }

    private static int salto(int enc) {
        return enc == 1 || enc == 2 ? 2 : 1;
    }

    /** USLT: codificación, idioma (3), descripción terminada en cero y la letra. */
    static String uslt(byte[] b, int off, int len) {
        int end = off + len;
        int enc = b[off];
        int k = off + 4;
        if (k > end) {
            return null;
        }
        int d = fin(b, k, end, enc);
        k = Math.min(end, d + salto(enc));
        String s = new String(b, k, end - k, cs(enc));
        int z = s.indexOf('\0');
        return (z >= 0 ? s.substring(0, z) : s).trim();
    }

    /** SYLT: codificación, idioma, formato de tiempo (2 = ms), tipo, descripción y luego (texto, tiempo de 4 bytes) repetido. */
    static String sylt(byte[] b, int off, int len) {
        int end = off + len;
        if (len < 7) {
            return null;
        }
        int enc = b[off];
        int fmt = b[off + 4];
        int k = off + 6;
        k = Math.min(end, fin(b, k, end, enc) + salto(enc));
        StringBuilder out = new StringBuilder();
        StringBuilder linea = new StringBuilder();
        long lineaMs = -1;
        while (k < end) {
            int z = fin(b, k, end, enc);
            String txt = new String(b, k, z - k, cs(enc));
            k = z + salto(enc);
            if (k + 4 > end) {
                break;
            }
            long t = (b[k] & 255L) << 24 | (b[k + 1] & 255L) << 16 | (b[k + 2] & 255L) << 8 | (b[k + 3] & 255L);
            k += 4;
            if (fmt != 2) {
                return null;                          // tiempos en cuadros de MPEG: no se usan
            }
            // en SYLT cada trozo puede ser una sílaba; un salto de línea empieza una línea nueva
            boolean nueva = txt.startsWith("\n") || txt.startsWith("\r") || lineaMs < 0;
            if (nueva && linea.length() > 0) {
                out.append(marca(lineaMs)).append(linea.toString().trim()).append('\n');
                linea.setLength(0);
            }
            if (nueva) {
                lineaMs = t;
            }
            linea.append(txt.replace("\r", "").replace("\n", ""));
        }
        if (linea.length() > 0) {
            out.append(marca(lineaMs)).append(linea.toString().trim()).append('\n');
        }
        return out.length() == 0 ? null : out.toString();
    }

    private static String marca(long ms) {
        long cs = Math.max(0, ms) / 10;
        return String.format(Locale.ROOT, "[%02d:%02d.%02d]", cs / 6000, cs / 100 % 60, cs % 100);
    }

    /** OGG: comentarios LYRICS, SYNCEDLYRICS o UNSYNCEDLYRICS (pueden ser largos: se lee más del principio). */
    private static String vorbis(Path p) throws IOException {
        byte[] head;
        try (InputStream in = Files.newInputStream(p)) {
            head = in.readNBytes(262144);
        }
        byte[] id = {3, 'v', 'o', 'r', 'b', 'i', 's'};
        int c = -1;
        outer:
        for (int i = 0; i + id.length <= head.length; i++) {
            for (int j = 0; j < id.length; j++) {
                if (head[i + j] != id[j]) {
                    continue outer;
                }
            }
            c = i;
            break;
        }
        if (c < 0) {
            return null;
        }
        // el paquete de comentarios puede cruzar páginas del OGG: se quitan las cabeceras de página que aparezcan en medio
        byte[] data = sinPaginas(head, c);
        int i = 7;
        int vendor = le32(data, i);
        i += 4 + vendor;
        if (vendor < 0 || i + 4 > data.length) {
            return null;
        }
        int n = le32(data, i);
        i += 4;
        String synced = null, plain = null;
        for (int k = 0; k < n && i + 4 <= data.length; k++) {
            int len = le32(data, i);
            i += 4;
            if (len < 0 || i + len > data.length) {
                break;
            }
            String s = new String(data, i, len, StandardCharsets.UTF_8);
            i += len;
            int eq = s.indexOf('=');
            if (eq > 0) {
                String key = s.substring(0, eq).toUpperCase(Locale.ROOT);
                String v = s.substring(eq + 1);
                if (key.equals("SYNCEDLYRICS") || (key.equals("LYRICS") && Letra.esLrc(v))) {
                    synced = v;
                } else if (key.equals("LYRICS") || key.equals("UNSYNCEDLYRICS")) {
                    plain = v;
                }
            }
        }
        return synced != null ? synced : plain;
    }

    /** Desde {@code from}, copia los datos saltándose las cabeceras de página OGG ("OggS" + segmentos). */
    private static byte[] sinPaginas(byte[] b, int from) {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int i = from;
        while (i < b.length) {
            if (i + 27 <= b.length && b[i] == 'O' && b[i + 1] == 'g' && b[i + 2] == 'g' && b[i + 3] == 'S') {
                int segs = b[i + 26] & 255;
                i += 27 + segs;
                continue;
            }
            out.write(b[i]);
            i++;
        }
        return out.toByteArray();
    }

    private static int le32(byte[] b, int i) {
        if (i + 4 > b.length) {
            return -1;
        }
        return (b[i] & 255) | (b[i + 1] & 255) << 8 | (b[i + 2] & 255) << 16 | (b[i + 3] & 255) << 24;
    }

    // ------------------------------------------------------------------ LRCLIB

    private static HttpClient http;

    private static synchronized HttpClient http() {
        if (http == null) {
            http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).followRedirects(HttpClient.Redirect.NORMAL).build();
        }
        return http;
    }

    private static String q(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    /** Pide algo a LRCLIB: {código, cuerpo}. */
    private static Object[] pedir(String path) throws IOException {
        HttpRequest req = HttpRequest.newBuilder(URI.create(servidor + path)).timeout(Duration.ofSeconds(10))
                .header("User-Agent", "Celular (mod de Minecraft) - letras de canciones").header("Accept", "application/json").GET().build();
        try {
            HttpResponse<String> r = http().send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Object[]{r.statusCode(), r.body()};
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    /**
     * Busca en LRCLIB: {letra con tiempos, letra sin tiempos}; null si no se pudo preguntar (sin internet). Si respondió pero no hay
     * letra, devuelve {null, null}.
     */
    static String[] lrclib(Pedido p) throws IOException {
        String dur = p.segundos > 0 ? "&duration=" + p.segundos : "";
        boolean respondio = false;
        if (!p.artista.isEmpty()) {
            Object[] r = pedir("/api/get?track_name=" + q(p.titulo) + "&artist_name=" + q(p.artista) + dur);
            respondio = true;
            if ((int) r[0] == 200) {
                String[] l = letraDe(Json.parse((String) r[1]));
                if (l != null) {
                    return l;
                }
            }
        }
        List<String> busquedas = new ArrayList<>();
        busquedas.add("/api/search?track_name=" + q(p.titulo) + (p.artista.isEmpty() ? "" : "&artist_name=" + q(p.artista)));
        busquedas.add("/api/search?q=" + q((p.artista + " " + p.titulo).trim()));
        for (String b : busquedas) {
            Object[] r = pedir(b);
            respondio = true;
            if ((int) r[0] != 200) {
                continue;
            }
            Object arr = Json.parse((String) r[1]);
            String[] l = mejor(arr, p.segundos);
            if (l != null) {
                return l;
            }
        }
        return respondio ? new String[]{null, null} : null;
    }

    /** De una lista de resultados, la que mejor cuadra con la duración (las que tienen tiempos primero). */
    static String[] mejor(Object arr, int segundos) {
        if (!(arr instanceof List<?> l)) {
            return null;
        }
        String[] best = null;
        double bestScore = Double.MAX_VALUE;
        for (Object o : l) {
            if (!(o instanceof Map<?, ?> m) || Boolean.TRUE.equals(m.get("instrumental"))) {
                continue;
            }
            String[] le = letraDe(m);
            if (le == null) {
                continue;
            }
            double d = m.get("duration") instanceof Number n ? n.doubleValue() : -1;
            double diff = segundos > 0 && d > 0 ? Math.abs(d - segundos) : 0;
            if (segundos > 0 && d > 0 && diff > 8) {
                continue;                            // otra versión de la canción: los tiempos no cuadrarían
            }
            double score = diff + (le[0] == null ? 5 : 0);
            if (score < bestScore) {
                bestScore = score;
                best = le;
            }
        }
        return best;
    }

    static String[] letraDe(Object o) {
        if (!(o instanceof Map<?, ?> m) || Boolean.TRUE.equals(m.get("instrumental"))) {
            return null;
        }
        String s = m.get("syncedLyrics") instanceof String x && !x.isBlank() ? x : null;
        String p = m.get("plainLyrics") instanceof String x && !x.isBlank() ? x : null;
        return s == null && p == null ? null : new String[]{s, p};
    }

    // ------------------------------------------------------------------ un lector de JSON pequeño (sin Gson, para las pruebas)

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        static Object parse(String s) {
            Json j = new Json(s);
            j.ws();
            return j.value();
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private Object value() {
            ws();
            if (i >= s.length()) {
                return null;
            }
            char c = s.charAt(i);
            if (c == '{') {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                ws();
                if (s.charAt(i) == '}') {
                    i++;
                    return m;
                }
                while (i < s.length()) {
                    ws();
                    String k = str();
                    ws();
                    i++;                                 // ':'
                    m.put(k, value());
                    ws();
                    char e = s.charAt(i++);
                    if (e == '}') {
                        break;
                    }
                }
                return m;
            }
            if (c == '[') {
                i++;
                List<Object> l = new ArrayList<>();
                ws();
                if (s.charAt(i) == ']') {
                    i++;
                    return l;
                }
                while (i < s.length()) {
                    l.add(value());
                    ws();
                    char e = s.charAt(i++);
                    if (e == ']') {
                        break;
                    }
                }
                return l;
            }
            if (c == '"') {
                return str();
            }
            if (s.startsWith("true", i)) {
                i += 4;
                return Boolean.TRUE;
            }
            if (s.startsWith("false", i)) {
                i += 5;
                return Boolean.FALSE;
            }
            if (s.startsWith("null", i)) {
                i += 4;
                return null;
            }
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            try {
                return Double.parseDouble(s.substring(st, i));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String str() {
            StringBuilder b = new StringBuilder();
            i++;                                         // '"'
            while (i < s.length()) {
                char c = s.charAt(i++);
                if (c == '"') {
                    break;
                }
                if (c == '\\' && i < s.length()) {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> b.append('\n');
                        case 'r' -> b.append('\r');
                        case 't' -> b.append('\t');
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'u' -> {
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> b.append(e);
                    }
                } else {
                    b.append(c);
                }
            }
            return b.toString();
        }
    }
}
