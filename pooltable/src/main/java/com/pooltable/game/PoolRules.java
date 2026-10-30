package com.pooltable.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Las reglas de los modos de juego, sin nada de Minecraft (solo números y textos por clave), para poder probarlas solas.
 * <ul>
 *   <li><b>Reto en solitario</b> (1 jugador): embocar las 15 bolas en el menor número de tiros; la blanca que cae suma 2 tiros.</li>
 *   <li><b>8 bolas</b> (2, 4 o 6 jugadores): lisas contra rayadas; con 4 o 6 se juega por equipos (jugadores en orden: equipo A, B, A, B...).</li>
 *   <li><b>9 bolas</b> (2 a 6): se juega con las bolas 1 a 9; siempre se toca primero la más baja; gana quien emboca la 9 sin falta.</li>
 *   <li><b>Rotación</b> (2 a 6): 15 bolas, se toca primero la más baja; cada bola vale su número; gana quien llegue a 61 puntos.</li>
 * </ul>
 * Después de cada tiro, {@link #resolve} aplica las reglas y devuelve qué pasó (mensajes, quién gana, bolas que hay que volver a poner...).
 */
public final class PoolRules {
    public static final int MAX_PLAYERS = 6;
    public static final int ROTATION_TARGET = 61;
    public static final int SOLO_SCRATCH_PENALTY = 2;

    public enum Mode {
        SOLO("solo", 1, 1, false),
        EIGHT("eight", 2, 6, true),
        NINE("nine", 2, 6, false),
        ROTATION("rotation", 2, 6, false);

        public final String id;
        public final int min;
        public final int max;
        public final boolean even;

        Mode(String id, int min, int max, boolean even) {
            this.id = id;
            this.min = min;
            this.max = max;
            this.even = even;
        }

        public boolean accepts(int n) {
            return n >= min && n <= max && (!even || n % 2 == 0);
        }

        public static Mode byOrdinal(int o) {
            Mode[] v = values();
            return o >= 0 && o < v.length ? v[o] : EIGHT;
        }

        /** ¿Se juega por equipos con este número de jugadores? */
        public boolean teams(int n) {
            return this == EIGHT && n > 2;
        }
    }

    // ---- argumentos de los mensajes (el que muestra el texto los convierte: jugador, equipo, grupo, número o clave de texto)
    public record P(int i) {
    }

    public record T(int i) {
    }

    public record G(int g) {
    }

    public record N(int n) {
    }

    public record Key(String k) {
    }

    public static final int INFO = 0;
    public static final int GOOD = 1;
    public static final int GOLD = 2;
    public static final int BAD = 3;

    public static final class Ev {
        public final String key;
        public final int style;
        /** true = va en la barra de acción (corto); false = va al chat. */
        public final boolean bar;
        public final Object[] args;

        Ev(String key, int style, boolean bar, Object... args) {
            this.key = key;
            this.style = style;
            this.bar = bar;
            this.args = args;
        }
    }

    /** Qué pasó con un tiro. */
    public static final class Outcome {
        public final List<Ev> events = new ArrayList<>();
        public boolean ended = false;
        /** Ganador (índice de jugador o de equipo en 8 bolas por equipos); -1 = empate / nadie. */
        public int winner = -1;
        public boolean winnerIsTeam = false;
        /** Hay que armar el triángulo de nuevo (la negra cayó en la salida). */
        public boolean rerack = false;
        /** Bolas que hay que volver a poner en el punto del triángulo (la 9 embocada con falta). */
        public final List<Integer> respot = new ArrayList<>();
        /** La blanca cayó: hay que volver a ponerla en la salida. */
        public boolean cueScratch = false;
    }

    // ---- estado de la partida
    public Mode mode = Mode.EIGHT;
    public int players = 0;
    public int turn = 0;
    public final int[] group = {-1, -1};
    public final int[] score = new int[MAX_PLAYERS];
    public int shots = 0;
    /** Mejor resultado del reto en solitario (0 = ninguno). */
    public int best = 0;

    public int teamOf(int player) {
        return player % 2;
    }

    // ------------------------------------------------------------------ empezar

    /** Arma la mesa para el modo elegido y pone a cero la partida. Devuelve los mensajes de inicio. */
    public Outcome start(PoolPhysics ph, Random rnd, int n) {
        players = n;
        turn = 0;
        shots = 0;
        group[0] = -1;
        group[1] = -1;
        java.util.Arrays.fill(score, 0);
        switch (mode) {
            case NINE -> ph.rackNine(rnd);
            case ROTATION -> ph.rackRotation(rnd);
            default -> ph.rack(rnd);
        }
        Outcome o = new Outcome();
        switch (mode) {
            case SOLO -> o.events.add(new Ev("pooltable.msg.start.solo", GOLD, false, new P(0)));
            case EIGHT -> o.events.add(new Ev("pooltable.msg.start.eight", GOLD, false, new T(0), new T(1)));
            case NINE -> o.events.add(new Ev("pooltable.msg.start.nine", GOLD, false));
            case ROTATION -> o.events.add(new Ev("pooltable.msg.start.rotation", GOLD, false, new N(ROTATION_TARGET)));
        }
        if (mode != Mode.SOLO) {
            o.events.add(new Ev("pooltable.msg.break", GOOD, false, new P(0)));
        }
        return o;
    }

    /** Bolas que quedan fuera de juego en este modo (máscara de bits). */
    public static int offMask(Mode m) {
        if (m != Mode.NINE) {
            return 0;
        }
        int mask = 0;
        for (int b = 10; b < PoolPhysics.N; b++) {
            mask |= 1 << b;
        }
        return mask;
    }

    // ------------------------------------------------------------------ después de cada tiro

    public Outcome resolve(PoolPhysics ph) {
        Outcome o = new Outcome();
        shots++;
        o.cueScratch = ph.potted.contains(0);
        switch (mode) {
            case SOLO -> solo(ph, o);
            case EIGHT -> eight(ph, o);
            case NINE -> nine(ph, o);
            case ROTATION -> rotation(ph, o);
        }
        return o;
    }

    private void next() {
        turn = (turn + 1) % Math.max(1, players);
    }

    /** La bola de número más bajo que había en la mesa antes del tiro (las que cayeron en él cuentan). */
    private static int lowestBefore(PoolPhysics ph) {
        for (int b = 1; b < PoolPhysics.N; b++) {
            if (!ph.out[b] || ph.potted.contains(b)) {
                return b;
            }
        }
        return -1;
    }

    private static int onTable(PoolPhysics ph) {
        int c = 0;
        for (int b = 1; b < PoolPhysics.N; b++) {
            if (!ph.out[b]) {
                c++;
            }
        }
        return c;
    }

    // ---- reto en solitario
    private void solo(PoolPhysics ph, Outcome o) {
        if (o.cueScratch) {
            shots += SOLO_SCRATCH_PENALTY;
            o.events.add(new Ev("pooltable.msg.solo_scratch", INFO, true, new N(SOLO_SCRATCH_PENALTY)));
        }
        if (onTable(ph) == 0) {
            o.ended = true;
            o.winner = 0;
            o.events.add(new Ev("pooltable.msg.solo_done", GOLD, false, new N(shots)));
            if (best == 0 || shots < best) {
                best = shots;
                o.events.add(new Ev("pooltable.msg.solo_record", GOLD, false, new N(best)));
            }
        }
    }

    // ---- 8 bolas
    private void eight(PoolPhysics ph, Outcome o) {
        List<Integer> pot = new ArrayList<>(ph.potted);
        boolean scratch = pot.contains(0);
        int fc = ph.firstContact;
        int me = teamOf(turn);
        int other = 1 - me;
        boolean groupsSet = group[me] >= 0;
        boolean foul = scratch || fc < 0;
        String reason = scratch ? "pooltable.foul.scratch" : fc < 0 ? "pooltable.foul.nohit" : null;
        int potMine = 0;
        int potAny = 0;
        for (int b : pot) {
            if (b != 0 && b != 8) {
                potAny++;
                if (groupsSet && PoolPhysics.groupOf(b) == group[me]) {
                    potMine++;
                }
            }
        }
        int mineBefore = groupsSet ? ph.remaining(group[me]) + potMine : 15;
        if (!foul && fc >= 0) {
            if (!groupsSet && fc == 8) {
                foul = true;
                reason = "pooltable.foul.eight";
            } else if (groupsSet && fc == 8 && mineBefore > 0) {
                foul = true;
                reason = "pooltable.foul.eight";
            } else if (groupsSet && fc != 8 && PoolPhysics.groupOf(fc) != group[me]) {
                foul = true;
                reason = "pooltable.foul.wrong";
            }
        }
        // la negra
        if (pot.contains(8)) {
            if (shots <= 1) {
                o.events.add(new Ev("pooltable.msg.eight_break", GOLD, false));
                o.rerack = true;
                shots = 0;
                return;
            }
            boolean win = groupsSet && mineBefore == 0 && !foul;
            o.ended = true;
            o.winnerIsTeam = true;
            o.winner = win ? me : other;
            o.events.add(new Ev("pooltable.msg.win_team", GOLD, false, new T(o.winner)));
            if (!win) {
                o.events.add(new Ev("pooltable.msg.lost_by", INFO, false, new Key(foul ? reason : "pooltable.foul.early_eight")));
            }
            return;
        }
        // grupos
        if (!groupsSet && !foul && potAny > 0) {
            for (int b : pot) {
                if (b != 0 && b != 8) {
                    group[me] = PoolPhysics.groupOf(b);
                    group[other] = 1 - group[me];
                    groupsSet = true;
                    potMine = 0;
                    for (int c : pot) {
                        if (c != 0 && c != 8 && PoolPhysics.groupOf(c) == group[me]) {
                            potMine++;
                        }
                    }
                    o.events.add(new Ev("pooltable.msg.groups", GOLD, false, new T(me), new G(group[me]), new T(other), new G(group[other])));
                    break;
                }
            }
        }
        if (foul) {
            o.events.add(new Ev("pooltable.msg.foul", BAD, false, new P(turn), new Key(reason)));
            next();
            o.events.add(new Ev("pooltable.msg.turn", GOOD, true, new P(turn)));
        } else if (potMine > 0) {
            o.events.add(new Ev("pooltable.msg.again", GOOD, true, new P(turn)));
            if (groupsSet && ph.remaining(group[me]) == 0) {
                o.events.add(new Ev("pooltable.msg.eight_now", GOLD, false, new T(me)));
            }
        } else {
            next();
            o.events.add(new Ev("pooltable.msg.turn", GOOD, true, new P(turn)));
        }
    }

    // ---- 9 bolas
    private void nine(PoolPhysics ph, Outcome o) {
        List<Integer> pot = ph.potted;
        boolean scratch = pot.contains(0);
        int fc = ph.firstContact;
        int low = lowestBefore(ph);
        boolean foul = scratch || fc < 0 || fc != low;
        String reason = scratch ? "pooltable.foul.scratch" : fc < 0 ? "pooltable.foul.nohit" : "pooltable.foul.lowest";
        boolean nine = pot.contains(9);
        if (nine && !foul) {
            o.ended = true;
            o.winner = turn;
            o.events.add(new Ev("pooltable.msg.win", GOLD, false, new P(turn)));
            return;
        }
        if (nine) {
            o.respot.add(9);
            o.events.add(new Ev("pooltable.msg.nine_respot", INFO, false));
        }
        int legal = 0;
        for (int b : pot) {
            if (b != 0 && b != 9) {
                legal++;
            }
        }
        if (foul) {
            o.events.add(new Ev("pooltable.msg.foul", BAD, false, new P(turn), new Key(reason)));
            next();
            o.events.add(new Ev("pooltable.msg.turn", GOOD, true, new P(turn)));
        } else if (legal > 0) {
            o.events.add(new Ev("pooltable.msg.again", GOOD, true, new P(turn)));
        } else {
            next();
            o.events.add(new Ev("pooltable.msg.turn", GOOD, true, new P(turn)));
        }
    }

    // ---- rotación
    private void rotation(PoolPhysics ph, Outcome o) {
        List<Integer> pot = ph.potted;
        boolean scratch = pot.contains(0);
        int fc = ph.firstContact;
        int low = lowestBefore(ph);
        boolean foul = scratch || fc < 0 || fc != low;
        String reason = scratch ? "pooltable.foul.scratch" : fc < 0 ? "pooltable.foul.nohit" : "pooltable.foul.lowest";
        int gained = 0;
        if (!foul) {
            for (int b : pot) {
                if (b != 0) {
                    gained += b;
                }
            }
            score[turn] += gained;
            if (gained > 0) {
                o.events.add(new Ev("pooltable.msg.points", GOOD, true, new P(turn), new N(gained), new N(score[turn])));
            }
        } else {
            score[turn] -= 1;
            o.events.add(new Ev("pooltable.msg.foul_point", BAD, false, new P(turn), new Key(reason)));
        }
        if (score[turn] >= ROTATION_TARGET) {
            o.ended = true;
            o.winner = turn;
            o.events.add(new Ev("pooltable.msg.win", GOLD, false, new P(turn)));
            return;
        }
        if (onTable(ph) == 0) {
            int bestScore = Integer.MIN_VALUE;
            int bestPlayer = -1;
            boolean tie = false;
            for (int i = 0; i < players; i++) {
                if (score[i] > bestScore) {
                    bestScore = score[i];
                    bestPlayer = i;
                    tie = false;
                } else if (score[i] == bestScore) {
                    tie = true;
                }
            }
            o.ended = true;
            if (tie) {
                o.events.add(new Ev("pooltable.msg.draw", GOLD, false));
            } else {
                o.winner = bestPlayer;
                o.events.add(new Ev("pooltable.msg.win", GOLD, false, new P(bestPlayer)));
            }
            return;
        }
        if (!foul && gained > 0) {
            o.events.add(new Ev("pooltable.msg.again", GOOD, true, new P(turn)));
        } else {
            next();
            o.events.add(new Ev("pooltable.msg.turn", GOOD, true, new P(turn)));
        }
    }

    /** Bola objetivo (la más baja que sigue en la mesa) para mostrarla en el cartel; -1 si no hay. */
    public static int targetBall(PoolPhysics ph) {
        for (int b = 1; b < PoolPhysics.N; b++) {
            if (!ph.out[b]) {
                return b;
            }
        }
        return -1;
    }
}
