package com.showdeaxelis.game;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Adivina la palabra: uno describe sin decir la palabra ni las prohibidas; su lado la adivina escribiendo en el chat. */
public final class TabooGame {
    public final String word;
    public final List<String> forbidden;
    public final UUID describer;
    public final int side;
    public final int startTick;
    public final int deadlineTick;
    public int strikes;
    public boolean solved;
    public boolean over;
    public UUID solver;
    public int gained;
    public final List<String> lines = new ArrayList<>();

    public TabooGame(String word, List<String> forbidden, UUID describer, int side, int startTick, int deadlineTick) {
        this.word = word;
        this.forbidden = forbidden;
        this.describer = describer;
        this.side = side;
        this.startTick = startTick;
        this.deadlineTick = deadlineTick;
    }
}
