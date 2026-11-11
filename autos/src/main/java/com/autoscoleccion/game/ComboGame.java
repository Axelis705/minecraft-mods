package com.autoscoleccion.game;

/**
 * Control de motor (antes "Arrancón total"): una sola prueba de 10 segundos. Manteniendo W el motor acelera y la aguja sube; al
 * soltarla baja. La aguja tiene inercia (sigue subiendo un poco después de soltar) y el motor da tirones solos, así que hay que
 * anticiparse. La franja verde se mueve y se hace cada vez más angosta. El puntaje es el tiempo que la aguja estuvo en verde (cerca
 * de la franja vale una parte).
 */
public final class ComboGame extends SkillGame {
    public static final int PHASE_LAUNCH = 0;
    public static final int PHASE_END = 3;

    static final int STEP = 5;
    public static final int LAUNCH_START = 900;
    public static final int LAUNCH_LEN = 10000;
    /** El tiempo en verde cuenta a partir de aquí (deja 0,8 s para subir la aguja la primera vez). */
    static final int LAUNCH_COUNT = 800;
    /** Cada cuánto sale un aviso con lo bien que vas (ms). */
    static final int SEGMENT = 2000;

    int phase = PHASE_LAUNCH;
    double rpm;
    double vel;
    boolean hold;
    int simT = LAUNCH_START;
    double got;
    int counted;
    double segGot;
    int segCount;
    final double w1, w2, w3, ph1, ph2, ph3;
    final double c1, c2, cp1, cp2;
    int finishT = -1;

    public ComboGame(long seed) {
        super(seed);
        w1 = 1.7 + rnd.nextDouble() * 1.1;
        w2 = 3.9 + rnd.nextDouble() * 1.8;
        w3 = 0.6 + rnd.nextDouble() * 0.5;
        ph1 = rnd.nextDouble() * 6.28;
        ph2 = rnd.nextDouble() * 6.28;
        ph3 = rnd.nextDouble() * 6.28;
        c1 = 0.55 + rnd.nextDouble() * 0.35;
        c2 = 1.3 + rnd.nextDouble() * 0.6;
        cp1 = rnd.nextDouble() * 6.28;
        cp2 = rnd.nextDouble() * 6.28;
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

    /** Fracción del tiempo que lleva en verde (de lo que se ha contado). */
    public double launchSoFar() {
        return counted == 0 ? 0.0 : got / counted;
    }

    public double launchProgress() {
        return Math.max(0.0, Math.min(1.0, (now - LAUNCH_START) / (double) LAUNCH_LEN));
    }

    /** Centro de la franja verde en el tiempo t: se pasea entre 0,35 y 0,8. */
    public double bandCenter(int t) {
        double s = Math.max(0, t - LAUNCH_START) / 1000.0;
        return 0.575 + 0.15 * Math.sin(s * c1 + cp1) + 0.075 * Math.sin(s * c2 + cp2);
    }

    /** Ancho de la franja: de 0,17 a 0,09. */
    public double bandWidth(int t) {
        double k = Math.max(0.0, Math.min(1.0, (t - LAUNCH_START) / (double) LAUNCH_LEN));
        return 0.15 - 0.075 * k;
    }

    public double bandLo() {
        return bandCenter(now) - bandWidth(now) / 2;
    }

    public double bandHi() {
        return bandCenter(now) + bandWidth(now) / 2;
    }

    public boolean holding() {
        return hold;
    }

    /** Tirones del motor (aceleración extra que no controlas). */
    private double kick(int t) {
        double s = t / 1000.0;
        return 0.9 * Math.sin(s * w1 + ph1) + 0.55 * Math.sin(s * w2 + ph2) + 0.5 * Math.sin(s * w3 + ph3);
    }

    @Override
    public void update(int t) {
        if (t < now) {
            return;
        }
        now = t;
        if (phase != PHASE_LAUNCH) {
            return;
        }
        int end = LAUNCH_START + LAUNCH_LEN;
        while (simT + STEP <= Math.min(now, end)) {
            simT += STEP;
            double dt = STEP / 1000.0;
            vel += ((hold ? 2.6 : -2.4) + kick(simT) * 1.3) * dt;
            vel *= Math.pow(0.12, dt);
            rpm += vel * dt;
            if (rpm < 0) {
                rpm = 0;
                vel = Math.max(0, vel);
            } else if (rpm > 1) {
                rpm = 1;
                vel = Math.min(0, vel);
            }
            if (simT - LAUNCH_START >= LAUNCH_COUNT) {
                double half = bandWidth(simT) / 2;
                double d = Math.abs(rpm - bandCenter(simT));
                double v = d <= half ? 1.0 : d <= half * 1.6 ? 0.35 * (1.0 - (d - half) / (half * 0.6)) : 0.0;
                got += v;
                counted++;
                segGot += v;
                segCount++;
                if ((simT - LAUNCH_START) % SEGMENT == 0 && segCount > 0) {
                    double sv = segGot / segCount;
                    pops.add(new Pop(simT, sv >= 0.92 ? "¡PERFECTO!" : sv >= 0.7 ? "¡Genial!" : sv >= 0.4 ? "Bien" : sv > 0.1 ? "Justo" : "¡Fuera!",
                            sv));
                    segGot = 0;
                    segCount = 0;
                }
            }
        }
        if (now >= end) {
            phase = PHASE_END;
            finishT = end;
        }
    }

    @Override
    public void input(int t, int k, boolean down) {
        update(t);
        if (phase == PHASE_LAUNCH && k == K_W) {
            hold = down;
        }
    }

    @Override
    public boolean done() {
        return phase == PHASE_END;
    }

    @Override
    public double score() {
        if (counted == 0) {
            return 0.0;
        }
        double f = got / counted;
        return Math.max(0.0, Math.min(1.0, Math.pow(f, 1.25)));
    }

    @Override
    public int maxDuration() {
        return LAUNCH_START + LAUNCH_LEN + 300;
    }
}
