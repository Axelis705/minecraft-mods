package com.showdeaxelis.game;

import java.util.ArrayList;
import java.util.List;

/** Un "lado" del marcador: un equipo, o un jugador en todos contra todos. */
public final class Side {
    public final int index;
    public String name;
    public final int color;
    public int score;
    public final List<Participant> members = new ArrayList<>();
    /** Quien da la cara en el siguiente cara a cara; avanza cada ronda. */
    public int repCursor;

    public Side(int index, String name, int color) {
        this.index = index;
        this.name = name;
        this.color = color;
    }

    public Participant rep() {
        return members.isEmpty() ? null : members.get(Math.floorMod(repCursor, members.size()));
    }
}
