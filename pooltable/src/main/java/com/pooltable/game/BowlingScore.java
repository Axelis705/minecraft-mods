package com.pooltable.game;

import java.util.ArrayList;
import java.util.List;

/**
 * La planilla de un jugador de boliche de diez pinos (sin nada de Minecraft, así se puede probar sola).
 * Se guardan solo los pinos tirados en cada lanzamiento, en orden; de ahí salen las marcas (X, /, números), los puntos por turno y el total.
 * Reglas: 10 turnos; chuza (X, 10 en el primer tiro) suma los dos tiros siguientes; semichuza (/, 10 en dos tiros) suma el tiro siguiente;
 * el turno 10 da un tercer tiro si hubo chuza o semichuza (los pinos se vuelven a armar). Máximo 300.
 */
public final class BowlingScore {
    public static final int FRAMES = 10;

    private final List<Integer> throwsList = new ArrayList<>();

    public List<Integer> throwsList() {
        return throwsList;
    }

    public static BowlingScore of(int... pins) {
        BowlingScore s = new BowlingScore();
        for (int p : pins) {
            s.add(p);
        }
        return s;
    }

    /** Dónde empieza el turno f (0..9) dentro de la lista de tiros, o -1 si aún no empieza. Y cuántos tiros tiene hasta ahora. */
    private int[] frameInfo(int f) {
        int i = 0;
        for (int k = 0; k < f; k++) {
            if (i >= throwsList.size()) {
                return new int[]{-1, 0};
            }
            i += throwsList.get(i) == 10 ? 1 : 2;
        }
        if (i >= throwsList.size()) {
            return new int[]{-1, 0};
        }
        int n;
        if (f < FRAMES - 1) {
            n = throwsList.get(i) == 10 ? 1 : Math.min(2, throwsList.size() - i);
        } else {
            n = Math.min(3, throwsList.size() - i);
        }
        return new int[]{i, n};
    }

    /** Turno actual (0..9); 10 si la partida de este jugador terminó. */
    public int frame() {
        int i = 0;
        for (int f = 0; f < FRAMES - 1; f++) {
            if (i >= throwsList.size()) {
                return f;
            }
            if (throwsList.get(i) == 10) {
                i++;
            } else if (i + 1 < throwsList.size()) {
                i += 2;
            } else {
                return f; // falta el segundo tiro de este turno
            }
        }
        return isComplete() ? FRAMES : FRAMES - 1;
    }

    public boolean isComplete() {
        int i = 0;
        for (int f = 0; f < FRAMES - 1; f++) {
            if (i >= throwsList.size()) {
                return false;
            }
            i += throwsList.get(i) == 10 ? 1 : 2;
        }
        int left = throwsList.size() - i;
        if (left < 2) {
            return false;
        }
        int a = throwsList.get(i);
        int b = throwsList.get(i + 1);
        if (a == 10 || a + b == 10) {
            return left >= 3;
        }
        return true;
    }

    /** Los tiros del turno f (0..9) que ya se hicieron, en orden. */
    public List<Integer> frameThrows(int f) {
        int[] info = frameInfo(f);
        if (info[0] < 0) {
            return List.of();
        }
        return new ArrayList<>(throwsList.subList(info[0], info[0] + info[1]));
    }

    /** Cuántos pinos hay en pie al empezar el siguiente tiro. */
    public int standingForNext() {
        if (isComplete()) {
            return 0;
        }
        int f = frame();
        List<Integer> t = frameThrows(f);
        if (f < FRAMES - 1) {
            return t.isEmpty() ? 10 : 10 - t.get(0);
        }
        if (t.isEmpty()) {
            return 10;
        }
        if (t.size() == 1) {
            return t.get(0) == 10 ? 10 : 10 - t.get(0);
        }
        // dos tiros hechos en el turno 10 (el tercero existe solo si hubo chuza o semichuza): pinos nuevos si el último limpió la mesa
        int a = t.get(0);
        int b = t.get(1);
        if (a == 10) {
            return b == 10 ? 10 : 10 - b;
        }
        return 10; // semichuza: mesa nueva
    }

    /** Si el siguiente tiro empieza con los 10 pinos armados (hay que reponerlos). */
    public boolean freshRackForNext() {
        return !isComplete() && standingForNext() == 10;
    }

    /** Si el tiro que acaba de entrar cerró el turno (o la partida): el siguiente jugador es quien tira. */
    public boolean frameJustEnded() {
        if (throwsList.isEmpty()) {
            return false;
        }
        if (isComplete()) {
            return true;
        }
        int f = frame();
        if (f >= FRAMES - 1) {
            return false;
        }
        // el turno actual (f) aún no empezó: el anterior se cerró con el último tiro
        return frameThrows(f).isEmpty();
    }

    public boolean add(int pins) {
        if (isComplete() || pins < 0 || pins > standingForNext()) {
            return false;
        }
        throwsList.add(pins);
        return true;
    }

    /** Puntos de un turno con sus bonos, o -1 si todavía no se pueden contar. */
    private int frameScore(int f) {
        int[] info = frameInfo(f);
        int i = info[0];
        if (i < 0) {
            return -1;
        }
        int n = throwsList.size();
        if (f < FRAMES - 1) {
            int a = throwsList.get(i);
            if (a == 10) {
                return i + 2 < n ? 10 + throwsList.get(i + 1) + throwsList.get(i + 2) : -1;
            }
            if (i + 1 >= n) {
                return -1;
            }
            int b = throwsList.get(i + 1);
            if (a + b == 10) {
                return i + 2 < n ? 10 + throwsList.get(i + 2) : -1;
            }
            return a + b;
        }
        if (!isComplete()) {
            return -1;
        }
        int s = 0;
        for (int k = i; k < n; k++) {
            s += throwsList.get(k);
        }
        return s;
    }

    /** Puntos acumulados al final de cada turno (-1 si todavía no se pueden contar). */
    public int[] cumulative() {
        int[] out = new int[FRAMES];
        int total = 0;
        boolean ok = true;
        for (int f = 0; f < FRAMES; f++) {
            int s = ok ? frameScore(f) : -1;
            if (s < 0) {
                ok = false;
                out[f] = -1;
            } else {
                total += s;
                out[f] = total;
            }
        }
        return out;
    }

    /** El total con lo que ya se puede contar (lo que se ve en el marcador). */
    public int total() {
        int t = 0;
        for (int v : cumulative()) {
            if (v >= 0) {
                t = v;
            }
        }
        return t;
    }

    /** Las marcas de un turno para mostrar: X, /, números y - (cero); el turno 10 tiene hasta tres. */
    public String[] marks(int f) {
        List<Integer> t = frameThrows(f);
        String[] out = new String[f == FRAMES - 1 ? 3 : 2];
        java.util.Arrays.fill(out, "");
        if (f < FRAMES - 1) {
            if (t.size() >= 1) {
                if (t.get(0) == 10) {
                    out[1] = "X";
                } else {
                    out[0] = digit(t.get(0));
                    if (t.size() >= 2) {
                        out[1] = t.get(0) + t.get(1) == 10 ? "/" : digit(t.get(1));
                    }
                }
            }
            return out;
        }
        int prev = 0;
        for (int k = 0; k < t.size(); k++) {
            int p = t.get(k);
            if (p == 10) {
                out[k] = "X";
                prev = 0;
            } else if (k > 0 && prev + p == 10 && prev != 0 && !(t.get(k - 1) == 10)) {
                out[k] = "/";
                prev = 0;
            } else if (k > 0 && t.get(k - 1) == 10) {
                out[k] = digit(p);
                prev = p;
            } else {
                out[k] = digit(p);
                prev = p;
            }
        }
        return out;
    }

    private static String digit(int p) {
        return p == 0 ? "-" : String.valueOf(p);
    }
}
