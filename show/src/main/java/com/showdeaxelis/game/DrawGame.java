package com.showdeaxelis.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Adivina el dibujo: uno dibuja la palabra en un lienzo y los demas la adivinan escribiendo en el chat. */
public final class DrawGame {
    /** Tamano del lienzo (en puntos). */
    public static final int W = 256;
    public static final int H = 192;
    public static final int MAX_STROKES = 4000;

    /** Un trazo: color (0..15), grosor (1..6) y puntos x,y,x,y... */
    public record Stroke(int color, int size, int[] pts) {
    }

    public final String word;
    public final UUID drawer;
    public final int startTick;
    public final int deadlineTick;
    public boolean over;
    public final Set<UUID> solved = new LinkedHashSet<>();
    public final List<Stroke> strokes = new ArrayList<>();
    /** Sube cada vez que se borra el lienzo, para que los clientes sepan que deben limpiar. */
    public int clearCount;

    public DrawGame(String word, UUID drawer, int startTick, int deadlineTick) {
        this.word = word;
        this.drawer = drawer;
        this.startTick = startTick;
        this.deadlineTick = deadlineTick;
    }

    /** La palabra tapada con rayas; al avanzar el tiempo se destapan hasta dos letras como ayuda. */
    public String mask(int nowTick) {
        int total = Math.max(1, deadlineTick - startTick);
        double f = Math.max(0, Math.min(1, (nowTick - startTick) / (double) total));
        int letters = 0;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                letters++;
            }
        }
        int reveal = f > 0.66 ? 2 : f > 0.4 ? 1 : 0;
        if (letters < 4) {
            reveal = 0;
        } else if (letters < 7) {
            reveal = Math.min(reveal, 1);
        }
        // posiciones que se destapan: siempre las mismas para esta palabra
        int[] order = new int[word.length()];
        int n = 0;
        int seed = Math.abs(word.hashCode());
        for (int i = 0; i < word.length(); i++) {
            if (Character.isLetterOrDigit(word.charAt(i))) {
                order[n++] = i;
            }
        }
        StringBuilder sb = new StringBuilder();
        int a = n == 0 ? -1 : order[seed % n];
        int b = n < 2 ? -1 : order[(seed % n + n / 2) % n];
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                sb.append(c);
            } else if (over || reveal >= 1 && i == a || reveal >= 2 && i == b) {
                sb.append(Character.toUpperCase(c));
            } else {
                sb.append('_');
            }
            sb.append(' ');
        }
        return sb.toString().trim();
    }
}
