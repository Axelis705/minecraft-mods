package com.autoscoleccion.game;

/**
 * Secuencia relámpago: cinco rondas. En cada una aparece una fila de teclas (de 5 a 9) y hay que teclearlas en orden antes de que
 * se acabe el tiempo. Un error corta la ronda. Terminar rápido da puntos extra.
 */
public final class SequenceGame extends SkillGame {
    public static final int ROUNDS = 5;
    static final int[] POOL = {'Q', 'W', 'E', 'R', 'A', 'S', 'D', 'F', K_LEFT, K_RIGHT, K_UP, K_DOWN};
    /** Medio segundo para ver la fila antes de que empiece a correr el tiempo. */
    public static final int PREVIEW = 600;
    public static final int GAP = 900;

    final int[][] seq = new int[ROUNDS][];
    final int[] limit = new int[ROUNDS];
    final double[] result = new double[ROUNDS];
    int round;
    int pos;
    int roundStart;
    boolean between;
    int nextAt;
    boolean finished;
    boolean failed;

    public SequenceGame(long seed) {
        super(seed);
        for (int r = 0; r < ROUNDS; r++) {
            int n = 5 + r;
            seq[r] = new int[n];
            int prev = -1;
            for (int i = 0; i < n; i++) {
                int k;
                do {
                    k = POOL[rnd.nextInt(POOL.length)];
                } while (k == prev);
                seq[r][i] = k;
                prev = k;
            }
            limit[r] = 2500 - 220 * r;
            result[r] = -1;
        }
        between = true;
        nextAt = 900;
    }

    @Override
    public int type() {
        return SEQUENCE;
    }

    public int round() {
        return round;
    }

    public int[] sequence() {
        return seq[Math.min(round, ROUNDS - 1)];
    }

    public int pos() {
        return pos;
    }

    public boolean between() {
        return between;
    }

    public boolean failed() {
        return failed;
    }

    /** ¿Ya corre el tiempo de la ronda? */
    public boolean running() {
        return !between && !finished && now >= roundStart + PREVIEW;
    }

    public double timeLeft() {
        if (between || finished) {
            return 1.0;
        }
        double used = Math.max(0, now - roundStart - PREVIEW);
        return Math.max(0.0, 1.0 - used / limit[round]);
    }

    public double result(int r) {
        return result[r];
    }

    @Override
    public void update(int t) {
        if (t < now) {
            return;
        }
        now = t;
        while (!finished) {
            if (between) {
                if (now >= nextAt) {
                    between = false;
                    roundStart = nextAt;
                    pos = 0;
                    failed = false;
                    continue;
                }
                return;
            }
            int deadline = roundStart + PREVIEW + limit[round];
            if (now >= deadline) {
                result[round] = 0.38 * pos / seq[round].length;
                pops.add(new Pop(deadline, "¡Sin tiempo!", result[round]));
                endRound(deadline);
                continue;
            }
            return;
        }
    }

    private void endRound(int t) {
        round++;
        if (round >= ROUNDS) {
            finished = true;
            round = ROUNDS - 1;
            return;
        }
        between = true;
        nextAt = t + GAP;
    }

    @Override
    public void input(int t, int key, boolean down) {
        update(t);
        if (!down || finished || between || t < roundStart + PREVIEW) {
            return;
        }
        int[] s = seq[round];
        if (key == s[pos]) {
            pos++;
            if (pos >= s.length) {
                double used = (t - roundStart - PREVIEW) / (double) limit[round];
                result[round] = 0.72 + 0.3 * Math.max(0.0, 1.0 - used);
                pop(judge(result[round]), result[round]);
                endRound(t);
            }
        } else {
            failed = true;
            result[round] = 0.38 * pos / s.length;
            pop("¡Error!", result[round]);
            endRound(t);
        }
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
        return s / ROUNDS;
    }

    @Override
    public int maxDuration() {
        int t = 900;
        for (int l : limit) {
            t += PREVIEW + l + GAP;
        }
        return t + 300;
    }

    int[] seqOf(int r) {
        return seq[r];
    }

    int roundStartT() {
        return roundStart;
    }
}
