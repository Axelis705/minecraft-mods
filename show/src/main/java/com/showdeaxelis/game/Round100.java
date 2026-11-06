package com.showdeaxelis.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Estado de una ronda de 100 mexicanos dijeron. */
public final class Round100 {
    public static final int FACE = 0;
    public static final int PLAY = 1;
    public static final int STEAL = 2;
    public static final int END = 3;
    /** Antes de todo: el presentador llama a los representantes (la pregunta todavia no se ve). */
    public static final int CALL = 4;
    /** Ya hay ganador del cara a cara: el presentador decide si juegan o pasan. */
    public static final int CHOOSE = 5;

    public final Question100 q;
    public final boolean[] revealed;
    public final int mult;
    public int stage = CALL;
    /** La pregunta ya se anuncio (hasta entonces solo la ve el presentador). */
    public boolean qShown;
    /** La campana esta abierta (en el cara a cara) o se puede contestar. */
    public boolean open;
    /** Quien gano el cara a cara (antes de elegir jugar o pasar). */
    public int faceWinner = -1;
    public int bank;
    public int control = -1;
    public int strikes;
    public int turn;
    public int winner = -1;
    public int faceTry;
    /** Quien esta contestando ahora (null = nadie todavia). */
    public UUID answerer;
    /** Lo que escribio y espera que el presentador decida (null = nada pendiente). */
    public String pending;
    /** Lados que ya dieron su respuesta en el cara a cara, y cuanto valio. */
    public final Set<Integer> faceDone = new HashSet<>();
    public final Map<Integer, Integer> faceGuess = new HashMap<>();

    public Round100(Question100 q, int mult) {
        this.q = q;
        this.revealed = new boolean[q.answers().size()];
        this.mult = Math.max(1, mult);
    }

    public boolean allRevealed() {
        for (boolean b : revealed) {
            if (!b) {
                return false;
            }
        }
        return true;
    }
}
