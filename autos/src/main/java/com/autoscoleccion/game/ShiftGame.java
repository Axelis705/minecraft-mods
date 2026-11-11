package com.autoscoleccion.game;

/**
 * Cambios perfectos: seis marchas. En cada una la aguja del tacómetro sube cada vez más rápido y hay una franja verde (cada vez más
 * angosta). Hay que pulsar la tecla que aparece justo cuando la aguja está en la franja. Si pulsas otra tecla, o la aguja llega al
 * rojo, esa marcha vale 0.
 */
public final class ShiftGame extends SkillGame {
    public static final int GEARS = 6;
    static final int[] POOL = {'Q', 'W', 'E', 'R', 'A', 'S', 'D', 'F', 'J', 'K', 'L'};
    /** Pausa entre marchas (ms). */
    public static final int GAP = 550;
    /** La tecla aparece cuando la aguja pasa este valor (antes no se sabe cuál es). */
    public static final double REVEAL = 0.3;

    final int[] dur = new int[GEARS];
    final double[] center = new double[GEARS];
    final double[] width = new double[GEARS];
    final int[] key = new int[GEARS];
    final double[] result = new double[GEARS];
    /** Valor de la aguja al cambiar (para dibujar). */
    final double[] at = new double[GEARS];
    int gear;
    int gearStart;
    boolean waiting;
    int waitUntil;
    boolean finished;

    public ShiftGame(long seed) {
        super(seed);
        int prev = -1;
        for (int i = 0; i < GEARS; i++) {
            dur[i] = 1500 - 110 * i;
            width[i] = 0.17 - 0.02 * i;
            center[i] = 0.62 + rnd.nextDouble() * 0.26;
            int k;
            do {
                k = POOL[rnd.nextInt(POOL.length)];
            } while (k == prev);
            key[i] = k;
            prev = k;
            result[i] = -1;
        }
        gearStart = 900;
        waiting = true;
        waitUntil = 900;
    }

    @Override
    public int type() {
        return SHIFT;
    }

    /** Aguja (0 a 1) en el tiempo {@code t} para la marcha actual. */
    public double rpmAt(int t) {
        if (waiting || finished) {
            return 0.0;
        }
        double x = Math.max(0.0, (t - gearStart) / (double) dur[gear]);
        return Math.min(1.0, Math.pow(x, 1.35));
    }

    public double rpm() {
        return rpmAt(now);
    }

    public int gear() {
        return gear;
    }

    public boolean waiting() {
        return waiting;
    }

    public double center() {
        return center[Math.min(gear, GEARS - 1)];
    }

    public double width() {
        return width[Math.min(gear, GEARS - 1)];
    }

    /** La tecla de la marcha actual, o 0 si todavía no se muestra. */
    public int shownKey() {
        if (waiting || finished || rpm() < REVEAL) {
            return 0;
        }
        return key[gear];
    }

    public double result(int g) {
        return result[g];
    }

    public double needleAt(int g) {
        return at[g];
    }

    @Override
    public void update(int t) {
        if (t < now) {
            return;
        }
        now = t;
        while (!finished) {
            if (waiting) {
                if (now >= waitUntil) {
                    waiting = false;
                    gearStart = waitUntil;
                } else {
                    return;
                }
            }
            if (now >= gearStart + dur[gear]) {
                // al rojo sin cambiar: esa marcha no cuenta
                int end = gearStart + dur[gear];
                int keep = now;
                now = end;
                result[gear] = 0.0;
                at[gear] = 1.0;
                pop("¡Al rojo!", 0.0);
                nextGear(end);
                now = keep;
                continue;
            }
            return;
        }
    }

    private void nextGear(int t) {
        gear++;
        if (gear >= GEARS) {
            finished = true;
            return;
        }
        waiting = true;
        waitUntil = t + GAP;
    }

    @Override
    public void input(int t, int k, boolean down) {
        update(t);
        if (!down || finished || waiting) {
            return;
        }
        double r = rpmAt(t);
        at[gear] = r;
        if (r < REVEAL || k != key[gear]) {
            // demasiado pronto (la tecla ni se veía) o tecla equivocada
            result[gear] = 0.0;
            pop(r < REVEAL ? "¡Muy pronto!" : "Tecla equivocada", 0.0);
        } else {
            double half = width[gear] / 2.0;
            double d = Math.abs(r - center[gear]);
            double v;
            if (d <= half) {
                v = 1.0 - 0.6 * Math.pow(d / half, 1.5);
            } else if (d <= width[gear]) {
                v = 0.25 * (1.0 - (d - half) / half);
            } else {
                v = 0.0;
            }
            result[gear] = v;
            pop(judge(v), v);
        }
        nextGear(t);
    }

    @Override
    public boolean done() {
        return finished;
    }

    @Override
    public double score() {
        double s = 0.0;
        for (double r : result) {
            s += Math.max(0.0, r);
        }
        return s / GEARS;
    }

    @Override
    public int maxDuration() {
        int t = 900;
        for (int d : dur) {
            t += d + GAP;
        }
        return t + 500;
    }

    /** Para las pruebas: en qué tiempo (desde que empieza la marcha) la aguja pasa por {@code v}. */
    int timeFor(int g, double v) {
        return (int) Math.round(Math.pow(v, 1.0 / 1.35) * dur[g]);
    }
}
