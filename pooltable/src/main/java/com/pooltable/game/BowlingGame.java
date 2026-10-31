package com.pooltable.game;

/**
 * Una partida de boliche con hasta 4 jugadores que tiran por turnos (sin nada de Minecraft). Cada jugador tira su turno completo (uno o dos
 * tiros; tres en el décimo) antes de que siga el siguiente. Quien llama avisa cuántos pinos cayeron y recibe qué hay que hacer con la pista.
 */
public final class BowlingGame {
    public static final int MAX_PLAYERS = 4;

    public final BowlingScore[] scores;
    public int current = 0;
    public boolean over = false;

    public BowlingGame(int players) {
        scores = new BowlingScore[players];
        for (int i = 0; i < players; i++) {
            scores[i] = new BowlingScore();
        }
    }

    public int players() {
        return scores.length;
    }

    /** Pinos en pie para el siguiente tiro del jugador al que le toca. */
    public int standingForNext() {
        return over ? 0 : scores[current].standingForNext();
    }

    public boolean freshRackForNext() {
        return !over && scores[current].freshRackForNext();
    }

    /** Anota el tiro del jugador actual. Devuelve false si no es válido. */
    public boolean record(int pinsDown) {
        if (over) {
            return false;
        }
        BowlingScore s = scores[current];
        if (!s.add(pinsDown)) {
            return false;
        }
        if (s.frameJustEnded()) {
            advance();
        }
        return true;
    }

    private void advance() {
        for (int k = 1; k <= scores.length; k++) {
            int n = (current + k) % scores.length;
            if (!scores[n].isComplete()) {
                current = n;
                return;
            }
        }
        over = true;
    }

    /** El jugador con más puntos (el primero si hay empate), o -1 si aún no terminó. */
    public int winner() {
        if (!over) {
            return -1;
        }
        int best = 0;
        for (int i = 1; i < scores.length; i++) {
            if (scores[i].total() > scores[best].total()) {
                best = i;
            }
        }
        return best;
    }
}
