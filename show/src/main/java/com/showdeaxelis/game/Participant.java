package com.showdeaxelis.game;

import java.util.UUID;

/** Una persona que juega. */
public final class Participant {
    public final UUID id;
    public final String name;
    /** Equipo asignado en los modos por equipos (0..2). En todos contra todos no se usa. */
    public int team;
    /** Puntos personales de trivia (para estadistica). */
    public int points;

    public Participant(UUID id, String name, int team) {
        this.id = id;
        this.name = name;
        this.team = team;
    }
}
