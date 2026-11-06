package com.showdeaxelis.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Estado de una ronda de trivia. Los tiempos son ticks del servidor. */
public final class TriviaRound {
    public final TriviaQ q;
    public final int startTick;
    public final int deadlineTick;
    public boolean revealed;
    /** Quien contesto que, y en que tick (en orden de llegada). */
    public final Map<UUID, int[]> answers = new LinkedHashMap<>();
    /** Puntos ganados en esta ronda por cada jugador (se llena al revelar). */
    public final Map<UUID, Integer> gained = new LinkedHashMap<>();

    public TriviaRound(TriviaQ q, int startTick, int deadlineTick) {
        this.q = q;
        this.startTick = startTick;
        this.deadlineTick = deadlineTick;
    }
}
