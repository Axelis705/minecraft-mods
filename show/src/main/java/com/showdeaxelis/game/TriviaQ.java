package com.showdeaxelis.game;

import java.util.List;

/** Una pregunta de trivia con cuatro opciones. */
public record TriviaQ(String id, String category, String text, List<String> options, int correct, String note) {
}
