package com.showdeaxelis.data;

import java.util.List;
import java.util.Map;
import java.util.Random;

/** El locutor del show: elige una frase al azar para cada cosa que pasa. Las frases estan en anuncios.json y se pueden editar. */
public final class Announcer {
    private static final Random RND = new Random();

    private Announcer() {
    }

    public static String line(String type, Map<String, String> vars) {
        List<String> ls = Banks.LINES.get(type);
        if (ls == null || ls.isEmpty()) {
            return "";
        }
        String s = ls.get(RND.nextInt(ls.size()));
        for (Map.Entry<String, String> e : vars.entrySet()) {
            s = s.replace("{" + e.getKey() + "}", e.getValue());
        }
        return s;
    }

    public static String prize() {
        return Banks.PRIZES.isEmpty() ? "una felicitacion" : Banks.PRIZES.get(RND.nextInt(Banks.PRIZES.size()));
    }
}
