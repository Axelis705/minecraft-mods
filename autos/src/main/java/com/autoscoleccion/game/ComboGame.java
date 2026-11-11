package com.autoscoleccion.game;

/**
 * Arrancón total, en tres partes:
 * <ol>
 *   <li><b>Arranque</b> (3,2 s): manteniendo W la aguja sube, soltándola baja, y además tiembla. Hay que mantenerla en la franja verde.</li>
 *   <li><b>Cambios</b>: cuatro marchas como en "Cambios perfectos", más rápidas.</li>
 *   <li><b>Nitro</b>: cinco flechas que hay que teclear en orden en 1,6 s.</li>
 * </ol>
 * Vale 30 % el arranque, 45 % los cambios y 25 % el nitro.
 */
public final class ComboGame extends SkillGame {
    public static final int PHASE_LAUNCH = 0;
    public static final int PHASE_SHIFT = 1;
    public static final int PHASE_NITRO = 2;
    public static final int PHASE_END = 3;

    static final int STEP = 10;
    public static final int LAUNCH_START = 900;
    public static final int LAUNCH_LEN = 3200;
    /** El tiempo en verde cuenta a partir de aquí (deja 0,6 s para subir la aguja). */
    static final int LAUNCH_COUNT = 600;
    public static final double BAND_LO = 0.60;
    public static final double BAND_HI = 0.72;

    public static final int SHIFTS = 4;
    static final int[] POOL = {'Q', 'E', 'R', 'A', 'S', 'D', 'F', 'J', 'K', 'L'};
    public static final int NITRO_LEN = 5;
    public static final int NITRO_TIME = 1600;
    static final int[] ARROWS = {K_LEFT, K_RIGHT, K_UP, K_DOWN};

    int phase = PHASE_LAUNCH;
    // arranque
    double rpm;
    boolean hold;
    int simT = LAUNCH_START;
    int inBand;
    int counted;
    final double w1;
    final double w2;
    final double ph1;
    final double ph2;
    double launchScore;
    // cambios
    final int[] dur = new int[SHIFTS];
    final double[] center = new double[SHIFTS];
    final double[] width = new double[SHIFTS];
    final int[] key = new int[SHIFTS];
    final double[] result = new double[SHIFTS];
    int gear;
    int gearStart;
    boolean waiting;
    int waitUntil;
    // nitro
    final int[] nitro = new int[NITRO_LEN];
    int nitroStart;
    int nitroPos;
    boolean nitroFail;
    int nitroEnd = -1;
    double nitroScore;
    int finishT = -1;

    public ComboGame(long seed) {
        super(seed);
        w1 = 2.0 + rnd.nextDouble() * 1.4;
        w2 = 4.6 + rnd.nextDouble() * 2.0;
        ph1 = rnd.nextDouble() * 6.28;
        ph2 = rnd.nextDouble() * 6.28;
        int prev = -1;
        for (int i = 0; i < SHIFTS; i++) {
            dur[i] = 1150 - 90 * i;
            width[i] = 0.14 - 0.017 * i;
            center[i] = 0.64 + rnd.nextDouble() * 0.24;
            int k;
            do {
                k = POOL[rnd.nextInt(POOL.length)];
            } while (k == prev);
            key[i] = k;
            prev = k;
            result[i] = -1;
        }
        for (int i = 0; i < NITRO_LEN; i++) {
            nitro[i] = ARROWS[rnd.nextInt(4)];
        }
    }

    @Override
    public int type() {
        return COMBO;
    }

    public int phase() {
        return phase;
    }

    public double launchRpm() {
        return rpm;
    }

    /** Fracción del arranque que lleva en verde (de lo que se ha contado). */
    public double launchSoFar() {
        return counted == 0 ? 0.0 : inBand / (double) counted;
    }

    public double launchProgress() {
        return Math.max(0.0, Math.min(1.0, (now - LAUNCH_START) / (double) LAUNCH_LEN));
    }

    public int gear() {
        return gear;
    }

    public boolean waiting() {
        return waiting;
    }

    public double shiftRpm() {
        if (phase != PHASE_SHIFT || waiting) {
            return 0.0;
        }
        double x = Math.max(0.0, (now - gearStart) / (double) dur[gear]);
        return Math.min(1.0, Math.pow(x, 1.35));
    }

    public double center() {
        return center[Math.min(gear, SHIFTS - 1)];
    }

    public double width() {
        return width[Math.min(gear, SHIFTS - 1)];
    }

    public int shownKey() {
        if (phase != PHASE_SHIFT || waiting || shiftRpm() < ShiftGame.REVEAL) {
            return 0;
        }
        return key[gear];
    }

    public int[] nitroKeys() {
        return nitro;
    }

    public int nitroPos() {
        return nitroPos;
    }

    public boolean nitroFailed() {
        return nitroFail;
    }

    public double nitroTimeLeft() {
        if (phase != PHASE_NITRO) {
            return 0.0;
        }
        return Math.max(0.0, 1.0 - (now - nitroStart) / (double) NITRO_TIME);
    }

    public double launchScore() {
        return launchScore;
    }

    /** Valor de la aguja que se suma al temblor del arranque. */
    private double wobble(int t) {
        double s = t / 1000.0;
        return 0.55 * Math.sin(s * w1 + ph1) + 0.35 * Math.sin(s * w2 + ph2);
    }

    @Override
    public void update(int t) {
        if (t < now) {
            return;
        }
        now = t;
        while (true) {
            if (phase == PHASE_LAUNCH) {
                int end = LAUNCH_START + LAUNCH_LEN;
                while (simT + STEP <= Math.min(now, end)) {
                    simT += STEP;
                    double dt = STEP / 1000.0;
                    rpm += (hold ? 0.95 : -0.75) * dt + wobble(simT) * dt * 0.9;
                    rpm = Math.max(0.0, Math.min(1.0, rpm));
                    if (simT - LAUNCH_START >= LAUNCH_COUNT) {
                        counted++;
                        if (rpm >= BAND_LO && rpm <= BAND_HI) {
                            inBand++;
                        }
                    }
                }
                if (now >= end) {
                    launchScore = counted == 0 ? 0.0 : Math.min(1.0, inBand / (double) counted / 0.85);
                    pops.add(new Pop(end, judge(launchScore), launchScore));
                    phase = PHASE_SHIFT;
                    waiting = true;
                    waitUntil = end + 500;
                    continue;
                }
                return;
            }
            if (phase == PHASE_SHIFT) {
                if (waiting) {
                    if (now >= waitUntil) {
                        waiting = false;
                        gearStart = waitUntil;
                    } else {
                        return;
                    }
                }
                if (now >= gearStart + dur[gear]) {
                    int endT = gearStart + dur[gear];
                    result[gear] = 0.0;
                    pops.add(new Pop(endT, "¡Al rojo!", 0.0));
                    nextGear(endT);
                    continue;
                }
                return;
            }
            if (phase == PHASE_NITRO) {
                if (nitroEnd < 0 && now >= nitroStart + NITRO_TIME) {
                    nitroEnd = nitroStart + NITRO_TIME;
                    finishNitro(nitroEnd);
                    continue;
                }
                if (nitroEnd >= 0 && now >= nitroEnd + 700) {
                    phase = PHASE_END;
                    finishT = nitroEnd + 700;
                }
                return;
            }
            return;
        }
    }

    private void nextGear(int t) {
        gear++;
        if (gear >= SHIFTS) {
            phase = PHASE_NITRO;
            nitroStart = t + 700;
            return;
        }
        waiting = true;
        waitUntil = t + 450;
    }

    private void finishNitro(int t) {
        nitroScore = nitroPos / (double) NITRO_LEN;
        pops.add(new Pop(t, nitroPos >= NITRO_LEN ? "¡NITRO!" : nitroFail ? "Se apagó el nitro" : "Sin tiempo", nitroScore));
    }

    @Override
    public void input(int t, int k, boolean down) {
        update(t);
        if (phase == PHASE_LAUNCH) {
            if (k == K_W) {
                hold = down;
            }
            return;
        }
        if (!down) {
            return;
        }
        if (phase == PHASE_SHIFT && !waiting) {
            double r = shiftRpm();
            if (r < ShiftGame.REVEAL || k != key[gear]) {
                result[gear] = 0.0;
                pop(r < ShiftGame.REVEAL ? "¡Muy pronto!" : "Tecla equivocada", 0.0);
            } else {
                double half = width[gear] / 2.0;
                double d = Math.abs(r - center[gear]);
                double v = d <= half ? 1.0 - 0.6 * Math.pow(d / half, 1.5) : d <= width[gear] ? 0.25 * (1.0 - (d - half) / half) : 0.0;
                result[gear] = v;
                pop(judge(v), v);
            }
            nextGear(t);
            return;
        }
        if (phase == PHASE_NITRO && nitroEnd < 0 && t >= nitroStart) {
            if (k == nitro[nitroPos]) {
                nitroPos++;
                if (nitroPos >= NITRO_LEN) {
                    nitroEnd = t;
                    finishNitro(t);
                }
            } else if (k == K_LEFT || k == K_RIGHT || k == K_UP || k == K_DOWN) {
                nitroFail = true;
                nitroEnd = t;
                finishNitro(t);
            }
        }
    }

    @Override
    public boolean done() {
        return phase == PHASE_END;
    }

    @Override
    public double score() {
        double sh = 0.0;
        for (double r : result) {
            sh += Math.max(0.0, r);
        }
        sh /= SHIFTS;
        return 0.30 * launchScore + 0.45 * sh + 0.25 * nitroScore;
    }

    @Override
    public int maxDuration() {
        int t = LAUNCH_START + LAUNCH_LEN + 500;
        for (int d : dur) {
            t += d + 450;
        }
        return t + 700 + NITRO_TIME + 900;
    }

    // para las pruebas
    double shiftCenter(int g) {
        return center[g];
    }

    int shiftDur(int g) {
        return dur[g];
    }

    int shiftKey(int g) {
        return key[g];
    }

    int gearStart() {
        return gearStart;
    }
}
