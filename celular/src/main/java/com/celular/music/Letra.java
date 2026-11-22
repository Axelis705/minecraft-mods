package com.celular.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La letra de una canción, con el segundo en que empieza cada línea (y, si el archivo lo trae, cada palabra). Se lee de un texto
 * LRC (<code>[01:23.45]una línea</code>, también varias marcas en una línea, <code>[offset:+300]</code> y palabras con
 * <code>&lt;01:24.10&gt;</code>) o de una letra sin tiempos (que se reparte a lo largo de la canción). Sin nada de Minecraft.
 */
public final class Letra {
    /** Una línea: cuándo empieza, su texto y sus palabras con el segundo en que se canta cada una. */
    public static final class Linea {
        public final double t;
        public final String texto;
        public final String[] palabras;
        /** Segundo de cada palabra; null si el archivo no lo trae (se calcula al usarla). */
        public final double[] tPalabras;

        Linea(double t, String texto, String[] palabras, double[] tPalabras) {
            this.t = t;
            this.texto = texto;
            this.palabras = palabras;
            this.tPalabras = tPalabras;
        }
    }

    public final List<Linea> lineas;
    /** false = la letra no traía tiempos y se repartió a lo largo de la canción (aproximada). */
    public final boolean sincronizada;
    /** De dónde salió: "archivo", "mp3" (guardada en la canción), "internet" o "cache". */
    public String origen = "";

    public Letra(List<Linea> lineas, boolean sincronizada) {
        this.lineas = lineas;
        this.sincronizada = sincronizada;
    }

    public boolean vacia() {
        return lineas.isEmpty();
    }

    // ------------------------------------------------------------------ en qué línea vamos

    /** Índice de la línea que se canta en el segundo {@code t} (-1 antes de la primera). */
    public int linea(double t) {
        int lo = 0, hi = lineas.size() - 1, r = -1;
        while (lo <= hi) {
            int m = (lo + hi) >>> 1;
            if (lineas.get(m).t <= t) {
                r = m;
                lo = m + 1;
            } else {
                hi = m - 1;
            }
        }
        return r;
    }

    /** Hasta cuándo dura la línea i: hasta la siguiente, pero como mucho {@code max} segundos (en los solos no se queda colgada). */
    public double fin(int i, double max) {
        double t = lineas.get(i).t;
        double next = i + 1 < lineas.size() ? lineas.get(i + 1).t : t + max;
        return Math.min(next, t + max);
    }

    /** Segundo en que se canta cada palabra de la línea i (si el archivo no lo trae, se reparten según su largo). */
    public double[] tiempos(int i, double max) {
        Linea l = lineas.get(i);
        if (l.tPalabras != null && l.tPalabras.length == l.palabras.length) {
            return l.tPalabras;
        }
        double dur = Math.max(0.4, (fin(i, max) - l.t) * 0.85);
        dur = Math.min(dur, 0.42 * l.palabras.length + 0.4);
        int total = 0;
        for (String p : l.palabras) {
            total += p.length() + 1;
        }
        double[] out = new double[l.palabras.length];
        int acc = 0;
        for (int k = 0; k < out.length; k++) {
            out[k] = l.t + dur * acc / Math.max(1, total);
            acc += l.palabras[k].length() + 1;
        }
        return out;
    }

    // ------------------------------------------------------------------ leer

    private static final Pattern MARCA = Pattern.compile("\\[(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?]");
    private static final Pattern PALABRA = Pattern.compile("<(\\d{1,3}):(\\d{1,2})(?:[.:](\\d{1,3}))?>");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\s*]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ETIQUETA = Pattern.compile("^\\[[a-zA-Z#]+:.*]$");

    private static double seg(Matcher m) {
        double s = Integer.parseInt(m.group(1)) * 60.0 + Integer.parseInt(m.group(2));
        String f = m.group(3);
        if (f != null) {
            s += Integer.parseInt(f) / Math.pow(10, f.length());
        }
        return s;
    }

    /** ¿El texto tiene tiempos de LRC? */
    public static boolean esLrc(String texto) {
        return texto != null && MARCA.matcher(texto).find();
    }

    /** Lee un texto LRC. Si no trae ninguna marca de tiempo, devuelve null. */
    public static Letra lrc(String texto) {
        if (texto == null) {
            return null;
        }
        double offset = 0;
        Matcher om = OFFSET.matcher(texto);
        if (om.find()) {
            // un offset positivo hace que la letra salga antes
            offset = Integer.parseInt(om.group(1)) / 1000.0;
        }
        List<Linea> out = new ArrayList<>();
        for (String raw : texto.replace("\r", "").split("\n")) {
            String s = raw.trim();
            if (s.isEmpty() || (ETIQUETA.matcher(s).matches() && !MARCA.matcher(s).lookingAt())) {
                continue;
            }
            Matcher m = MARCA.matcher(s);
            List<Double> ts = new ArrayList<>();
            int end = 0;
            while (true) {
                m.region(end, s.length());
                if (!m.lookingAt()) {
                    break;
                }
                ts.add(seg(m) - offset);
                end = m.end();
            }
            if (ts.isEmpty()) {
                continue;
            }
            String resto = s.substring(end).trim();
            // palabras con su tiempo (<mm:ss.xx>palabra)
            List<String> pal = new ArrayList<>();
            List<Double> tp = new ArrayList<>();
            boolean conTiempos = PALABRA.matcher(resto).find();
            String limpio;
            if (conTiempos) {
                Matcher pm = PALABRA.matcher(resto);
                int last = 0;
                double cur = -1;
                StringBuilder sb = new StringBuilder();
                while (pm.find()) {
                    agregar(resto.substring(last, pm.start()), cur, pal, tp, sb);
                    cur = seg(pm) - offset;
                    last = pm.end();
                }
                agregar(resto.substring(last), cur, pal, tp, sb);
                limpio = sb.toString().trim().replaceAll("\\s+", " ");
            } else {
                limpio = resto.replaceAll("\\s+", " ");
                for (String w : limpio.split(" ")) {
                    if (!w.isEmpty()) {
                        pal.add(w);
                    }
                }
            }
            for (double t : ts) {
                double[] arr = null;
                if (conTiempos && tp.size() == pal.size() && !tp.contains(-1.0)) {
                    arr = new double[tp.size()];
                    for (int k = 0; k < arr.length; k++) {
                        arr[k] = tp.get(k) + (t - ts.get(0));      // si la línea se repite, las palabras se corren igual
                    }
                }
                out.add(new Linea(t, limpio, pal.toArray(new String[0]), arr));
            }
        }
        if (out.isEmpty()) {
            return null;
        }
        out.sort((a, b) -> Double.compare(a.t, b.t));
        return new Letra(out, true);
    }

    private static void agregar(String trozo, double t, List<String> pal, List<Double> tp, StringBuilder sb) {
        for (String w : trozo.trim().split("\\s+")) {
            if (!w.isEmpty()) {
                pal.add(w);
                tp.add(t);
                sb.append(w).append(' ');
            }
        }
    }

    /** Una letra sin tiempos: las líneas se reparten entre el 7 % y el 93 % de la canción. */
    public static Letra plana(String texto, int segundos) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        List<String> ls = new ArrayList<>();
        for (String raw : texto.replace("\r", "").split("\n")) {
            String s = raw.trim().replaceAll("\\s+", " ");
            if (!s.isEmpty() && !ETIQUETA.matcher(s).matches()) {
                ls.add(s);
            }
        }
        if (ls.isEmpty()) {
            return null;
        }
        double dur = segundos > 0 ? segundos : 180;
        double a = dur * 0.07, b = dur * 0.93;
        List<Linea> out = new ArrayList<>();
        for (int i = 0; i < ls.size(); i++) {
            String s = ls.get(i);
            out.add(new Linea(a + (b - a) * i / ls.size(), s, s.split(" "), null));
        }
        return new Letra(out, false);
    }

    /** Lo que sea que traiga el texto: LRC si tiene tiempos, si no letra sin tiempos. */
    public static Letra de(String texto, int segundos) {
        Letra l = esLrc(texto) ? lrc(texto) : null;
        return l != null ? l : plana(texto, segundos);
    }

    /** Para guardarla en la caché: en LRC si tiene tiempos; si no, el texto tal cual (así se vuelve a repartir al leerla). */
    public String guardar() {
        if (!sincronizada) {
            StringBuilder b = new StringBuilder();
            for (Linea l : lineas) {
                b.append(l.texto).append('\n');
            }
            return b.toString();
        }
        return aLrc();
    }

    /** La letra en formato LRC. */
    public String aLrc() {
        StringBuilder b = new StringBuilder();
        for (Linea l : lineas) {
            int cs = (int) Math.round(Math.max(0, l.t) * 100);
            b.append(String.format(Locale.ROOT, "[%02d:%02d.%02d]", cs / 6000, cs / 100 % 60, cs % 100)).append(l.texto).append('\n');
        }
        return b.toString();
    }
}
