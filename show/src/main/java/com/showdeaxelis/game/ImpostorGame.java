package com.showdeaxelis.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El Impostor: todos reciben la misma palabra secreta menos uno. Cada quien da pistas por turnos, se charla, se vota,
 * y si descubren al impostor este todavia puede ganar adivinando la palabra.
 */
public final class ImpostorGame {
    public static final int CLUE_ROUNDS = 2;

    public enum Stage { CLUES, DISCUSS, VOTE, GUESS, DONE }

    public final String category;
    public final String word;
    public final UUID impostor;
    /** Orden de turnos para las pistas. */
    public final List<UUID> order = new ArrayList<>();
    public Stage stage = Stage.CLUES;
    public int step;
    public final List<String[]> clues = new ArrayList<>();
    public final Map<UUID, UUID> votes = new LinkedHashMap<>();
    public UUID accused;
    public boolean impostorCaught;
    public boolean impostorGuessedWord;
    public String impostorGuess = "";

    public ImpostorGame(String category, String word, UUID impostor, List<UUID> order) {
        this.category = category;
        this.word = word;
        this.impostor = impostor;
        this.order.addAll(order);
    }

    public int totalClues() {
        return order.size() * CLUE_ROUNDS;
    }

    /** A quien le toca dar su pista. */
    public UUID current() {
        return stage == Stage.CLUES && step < totalClues() ? order.get(step % order.size()) : null;
    }
}
