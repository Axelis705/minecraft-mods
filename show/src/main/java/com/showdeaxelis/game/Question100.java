package com.showdeaxelis.game;

import java.util.List;

/** Una pregunta de encuesta con sus respuestas, de la mas popular a la menos. */
public record Question100(String id, String text, List<Answer100> answers) {
}
