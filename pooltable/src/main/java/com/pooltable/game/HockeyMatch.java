package com.pooltable.game;

import java.util.Random;

/**
 * Una partida de hockey de mesa (sin nada de Minecraft): marcador, cuenta atrás, pausa tras cada gol, saque, y la máquina como rival si
 * se juega contra ella. Gana quien llegue primero a {@link #WIN} goles. Se avanza con {@link #tick(double)}; los jugadores ponen los
 * objetivos de sus mazos en {@code ph.tx[]} y {@code ph.tz[]}.
 */
public final class HockeyMatch {
    /** Velocidad máxima del mazo de una persona (bloques por segundo); el cliente usa la misma para predecir. */
    public static final double HUMAN_SPEED = 11.0;
    public static final int WIN = 7;

    public enum State { IDLE, WAITING, COUNTDOWN, PLAYING, GOAL, OVER }

    public final HockeyPhysics ph = new HockeyPhysics();
    public State state = State.IDLE;
    public int scoreA;
    public int scoreB;
    public boolean vsCpu;
    public int diff = 1;
    /** Segundos que faltan para cambiar de estado. */
    public double timer;
    /** Último en marcar (0 = A, 1 = B), o -1. */
    public int lastScorer = -1;
    /** Quién ganó (0 = A, 1 = B) cuando termina, o -1. */
    public int winner = -1;
    /** Gol ocurrido en el último tick (0 = ninguno, 1 = A, 2 = B), para avisos. */
    public int goalEvent = 0;
    private final HockeyAI ai;
    private final Random rnd;

    public HockeyMatch(Random rnd) {
        this.rnd = rnd;
        this.ai = new HockeyAI(rnd);
    }

    /** Deja la mesa libre. */
    public void reset() {
        state = State.IDLE;
        scoreA = 0;
        scoreB = 0;
        winner = -1;
        lastScorer = -1;
        ph.putMallet(0, -0.9, 0);
        ph.putMallet(1, 0.9, 0);
        ph.placePuck(0, 0);
        ph.clearEvents();
    }

    /** Un jugador espera rival (1 contra 1). */
    public void waitForRival() {
        reset();
        vsCpu = false;
        state = State.WAITING;
    }

    public void start(boolean vsCpu, int diff) {
        reset();
        this.vsCpu = vsCpu;
        this.diff = Math.max(0, Math.min(2, diff));
        ph.maxSpeed[0] = HUMAN_SPEED;
        ph.maxSpeed[1] = vsCpu ? HockeyAI.SPEED[this.diff] : HUMAN_SPEED;
        serve(rnd.nextInt(2));
        state = State.COUNTDOWN;
        timer = 3.0;
    }

    /** Coloca el disco en la mitad de quien recibió el gol (o de uno al azar) y vuelve a empezar. */
    private void serve(int side) {
        ph.placePuck(side == 0 ? -0.45 : 0.45, 0.0);
    }

    public boolean running() {
        return state == State.COUNTDOWN || state == State.PLAYING || state == State.GOAL;
    }

    public void tick(double dt) {
        goalEvent = 0;
        ph.clearEvents();
        int sub = 8;
        double h = dt / sub;
        switch (state) {
            case IDLE, WAITING -> {
                for (int i = 0; i < sub; i++) {
                    ph.step(h, false);
                }
            }
            case COUNTDOWN -> {
                for (int i = 0; i < sub; i++) {
                    ph.step(h, false);
                }
                timer -= dt;
                if (timer <= 0) {
                    state = State.PLAYING;
                }
            }
            case PLAYING -> {
                if (vsCpu) {
                    ai.update(ph, 1, diff, dt);
                    ph.tx[1] = ai.tx;
                    ph.tz[1] = ai.tz;
                }
                for (int i = 0; i < sub; i++) {
                    ph.step(h, true);
                    if (ph.goal != 0) {
                        break;
                    }
                }
                if (ph.goal != 0) {
                    int g = ph.goal;
                    goalEvent = g;
                    if (g == 1) {
                        scoreA++;
                        lastScorer = 0;
                    } else {
                        scoreB++;
                        lastScorer = 1;
                    }
                    ph.placePuck(g == 1 ? 1.0 : -1.0, 0); // el disco queda en el hoyo hasta el siguiente saque
                    ph.pvx = 0;
                    ph.pvz = 0;
                    ph.goal = 0;
                    if (scoreA >= WIN || scoreB >= WIN) {
                        state = State.OVER;
                        winner = scoreA >= WIN ? 0 : 1;
                        timer = 5.0;
                    } else {
                        state = State.GOAL;
                        timer = 1.6;
                    }
                }
            }
            case GOAL -> {
                if (vsCpu) {
                    ai.update(ph, 1, diff, dt);
                    ph.tx[1] = ai.tx;
                    ph.tz[1] = ai.tz;
                }
                for (int i = 0; i < sub; i++) {
                    ph.step(h, false);
                }
                timer -= dt;
                if (timer <= 0) {
                    serve(lastScorer == 0 ? 1 : 0);
                    state = State.COUNTDOWN;
                    timer = 1.5;
                }
            }
            case OVER -> {
                for (int i = 0; i < sub; i++) {
                    ph.step(h, false);
                }
                timer -= dt;
                if (timer <= 0) {
                    reset();
                }
            }
            default -> {
            }
        }
    }
}
