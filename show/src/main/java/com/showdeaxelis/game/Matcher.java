package com.showdeaxelis.game;

import java.text.Normalizer;
import java.util.List;

/** Compara lo que escribe un jugador con las respuestas del tablero (sin acentos, sin mayusculas, con tolerancia a errores pequenos). */
public final class Matcher {
    private Matcher() {
    }

    public static String norm(String s) {
        if (s == null) {
            return "";
        }
        String t = Normalizer.normalize(s.toLowerCase(java.util.Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        t = t.replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").trim();
        for (String art : new String[] {"el ", "la ", "los ", "las ", "un ", "una ", "unos ", "unas ", "mi ", "su "}) {
            if (t.startsWith(art) && t.length() > art.length() + 2) {
                t = t.substring(art.length());
                break;
            }
        }
        return t;
    }

    private static int lev(String a, String b) {
        int[] p = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            p[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int[] c = new int[b.length() + 1];
            c[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                c[j] = Math.min(Math.min(c[j - 1] + 1, p[j] + 1), p[j - 1] + cost);
            }
            p = c;
        }
        return p[b.length()];
    }

    private static boolean exact(String g, Answer100 a) {
        if (g.equals(norm(a.text()))) {
            return true;
        }
        for (String al : a.aliases()) {
            if (g.equals(norm(al))) {
                return true;
            }
        }
        return false;
    }

    private static boolean loose(String g, Answer100 a) {
        List<String> all = new java.util.ArrayList<>(a.aliases());
        all.add(a.text());
        for (String raw : all) {
            String n = norm(raw);
            if (n.isEmpty()) {
                continue;
            }
            if (g.length() >= 4 && (" " + n + " ").contains(" " + g + " ")) {
                return true;
            }
            if (n.length() >= 4 && (" " + g + " ").contains(" " + n + " ")) {
                return true;
            }
            int tol = n.length() >= 9 ? 2 : n.length() >= 5 ? 1 : 0;
            if (tol > 0 && Math.abs(n.length() - g.length()) <= tol && lev(g, n) <= tol) {
                return true;
            }
        }
        return false;
    }

    /**
     * Busca la respuesta. Devuelve el indice de una respuesta aun oculta, -2 si es una respuesta que ya estaba descubierta
     * o -1 si no coincide con ninguna.
     */
    public static int find(String guess, Question100 q, boolean[] revealed) {
        String g = norm(guess);
        if (g.isEmpty()) {
            return -1;
        }
        int dup = -1;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < q.answers().size(); i++) {
                Answer100 a = q.answers().get(i);
                boolean ok = pass == 0 ? exact(g, a) : loose(g, a);
                if (!ok) {
                    continue;
                }
                if (!revealed[i]) {
                    return i;
                }
                dup = i;
            }
            if (dup >= 0) {
                return -2;
            }
        }
        return -1;
    }

    /** Dos textos cortos significan lo mismo (misma palabra sin acentos, o casi: tolera 1-2 letras mal). */
    public static boolean same(String a, String b) {
        String x = norm(a);
        String y = norm(b);
        if (x.isEmpty() || y.isEmpty()) {
            return false;
        }
        if (x.equals(y)) {
            return true;
        }
        int tol = y.length() >= 9 ? 2 : y.length() >= 5 ? 1 : 0;
        return tol > 0 && Math.abs(x.length() - y.length()) <= tol && lev(x, y) <= tol;
    }

    /** El texto contiene la palabra (o frase) como palabra completa, sin importar acentos ni mayusculas. */
    public static boolean containsWord(String text, String word) {
        String t = " " + norm(text) + " ";
        String w = norm(word);
        return !w.isEmpty() && t.contains(" " + w + " ");
    }
}
