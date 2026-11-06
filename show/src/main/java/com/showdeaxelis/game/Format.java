package com.showdeaxelis.game;

/** Las formas de jugar: todos contra todos o en equipos. Maximo 6 jugadores. */
public enum Format {
    FFA("Todos contra todos", 0, 6),
    TWO_TEAMS("2 equipos (de 1 a 3)", 2, 3),
    THREE_TEAMS("3 equipos de 2", 3, 2);

    public final String label;
    /** Cuantos equipos hay (0 = cada jugador es su propio lado). */
    public final int teams;
    /** Maximo de jugadores por equipo. */
    public final int perTeam;

    Format(String label, int teams, int perTeam) {
        this.label = label;
        this.teams = teams;
        this.perTeam = perTeam;
    }

    public int maxPlayers() {
        return teams == 0 ? 6 : teams * perTeam;
    }

    public static Format byOrdinal(int i) {
        Format[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
