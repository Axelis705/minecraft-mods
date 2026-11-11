package com.autoscoleccion.game;

import java.util.ArrayList;
import java.util.List;

/**
 * Ritmo de teclas: cuatro carriles (D F J K). Las notas bajan y hay que pulsar su tecla justo cuando cruzan la línea. El ritmo se
 * acelera y en la segunda mitad llegan notas dobles. Pulsar sin nota resta.
 */
public final class RhythmGame extends SkillGame {
    public static final int[] LANES = {'D', 'F', 'J', 'K'};
    public static final int PERFECT = 32;
    public static final int GREAT = 65;
    public static final int GOOD = 115;
    public static final int START = 1600;

    /** Una nota: tiempo en que cruza la línea, carril y resultado (-1 = pendiente). */
    public static final class Note {
        public final int t;
        public final int lane;
        public double result = -1;

        Note(int t, int lane) {
            this.t = t;
            this.lane = lane;
        }
    }

    final List<Note> notes = new ArrayList<>();
    double penalty;
    int combo;
    int bestCombo;
    final int end;

    public RhythmGame(long seed) {
        super(seed);
        int t = START;
        int count = 34;
        int prevLane = -1;
        for (int i = 0; i < count; i++) {
            double k = i / (double) (count - 1);
            int gap = (int) Math.round(620 - 340 * k);
            // a veces una corchea (medio hueco) para romper el ritmo
            if (i > 6 && rnd.nextDouble() < 0.18) {
                gap /= 2;
            }
            t += gap;
            int lane;
            do {
                lane = rnd.nextInt(4);
            } while (lane == prevLane && rnd.nextDouble() < 0.7);
            prevLane = lane;
            notes.add(new Note(t, lane));
            if (i >= count / 2 && rnd.nextDouble() < 0.3) {
                int other = (lane + 1 + rnd.nextInt(3)) % 4;
                notes.add(new Note(t, other));
            }
        }
        end = t + 700;
    }

    @Override
    public int type() {
        return RHYTHM;
    }

    public List<Note> notes() {
        return notes;
    }

    public int combo() {
        return combo;
    }

    /** Tiempo que tarda una nota en bajar hasta la línea (se acelera). */
    public int fallTime(int noteT) {
        double k = Math.min(1.0, Math.max(0.0, (noteT - START) / 16000.0));
        return (int) Math.round(1150 - 350 * k);
    }

    @Override
    public void update(int t) {
        if (t < now) {
            return;
        }
        now = t;
        for (Note n : notes) {
            if (n.result < 0 && n.t + GOOD < now) {
                n.result = 0.0;
                combo = 0;
                pops.add(new Pop(n.t + GOOD, "Fallo", 0.0));
            }
        }
    }

    @Override
    public void input(int t, int key, boolean down) {
        update(t);
        if (!down || done()) {
            return;
        }
        int lane = -1;
        for (int i = 0; i < LANES.length; i++) {
            if (LANES[i] == key) {
                lane = i;
            }
        }
        if (lane < 0) {
            return;
        }
        Note best = null;
        int bd = Integer.MAX_VALUE;
        for (Note n : notes) {
            if (n.lane == lane && n.result < 0) {
                int d = Math.abs(n.t - t);
                if (d <= GOOD && d < bd) {
                    bd = d;
                    best = n;
                }
            }
        }
        if (best == null) {
            // pulsar al aire resta un poco y rompe el combo
            penalty += 0.25;
            combo = 0;
            pop("¡Sin nota!", 0.0);
            return;
        }
        double v = bd <= PERFECT ? 1.0 : bd <= GREAT ? 0.6 : 0.25;
        best.result = v;
        combo++;
        bestCombo = Math.max(bestCombo, combo);
        pop(v >= 1.0 ? "¡PERFECTO!" : v >= 0.6 ? "¡Genial!" : "Bien", v);
    }

    @Override
    public boolean done() {
        return now >= end;
    }

    @Override
    public double score() {
        double s = 0.0;
        for (Note n : notes) {
            s += Math.max(0.0, n.result);
        }
        return Math.max(0.0, Math.min(1.0, (s - penalty) / notes.size()));
    }

    @Override
    public int maxDuration() {
        return end + 200;
    }
}
