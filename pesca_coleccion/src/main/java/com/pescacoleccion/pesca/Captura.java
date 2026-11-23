package com.pescacoleccion.pesca;

import net.minecraft.util.RandomSource;

/** Qué pez pica (por ahora solo la rareza; las especies llegan en la etapa de los peces). */
public final class Captura {
    /** Probabilidad base de cada rareza (común ... legendario) en milésimas. */
    private static final int[] BASE = {560, 250, 130, 48, 12};

    private Captura() {
    }

    /** Rareza del pez que se acerca; la suerte de la caña mueve probabilidad de los comunes a los raros. */
    public static int rareza(RandomSource r, int suerte) {
        int[] w = BASE.clone();
        int moved = Math.min(w[0] - 200, suerte * 70);
        w[0] -= moved;
        w[2] += moved * 45 / 100;
        w[3] += moved * 35 / 100;
        w[4] += moved - moved * 45 / 100 - moved * 35 / 100;
        int total = 0;
        for (int x : w) {
            total += x;
        }
        int pick = r.nextInt(total);
        for (int i = 0; i < w.length; i++) {
            pick -= w[i];
            if (pick < 0) {
                return i;
            }
        }
        return 0;
    }

    /** Ticks de espera hasta que se acerca un pez (con la caña y algo de azar). */
    public static int espera(RandomSource r, Cana c, boolean lluvia) {
        int base = 100 + r.nextInt(300);          // 5 a 20 s
        if (lluvia) {
            base = base * 3 / 4;
        }
        return Math.max(40, Math.round(base * c.espera));
    }

    /** Las picadas: unos piquetes falsos y al final la buena (ticks desde que el pez llega). */
    public static int[] picadas(RandomSource r, int rareza) {
        int fakes = r.nextInt(2 + Math.min(3, rareza)) ;   // los raros desconfían más
        int[] t = new int[fakes + 1];
        int at = 24 + r.nextInt(20);
        for (int i = 0; i < t.length; i++) {
            t[i] = at;
            at += 10 + r.nextInt(26);
        }
        return t;
    }

    /** Ticks que dura la picada buena (lo que hay para reaccionar). */
    public static int ventana(int rareza) {
        return new int[] {16, 14, 12, 10, 8, 7}[Math.max(0, Math.min(5, rareza))];
    }
}
