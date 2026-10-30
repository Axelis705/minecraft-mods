package com.casino.game;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * El estado de una mesa de ruleta (lógica pura): quién apostó cuánto a qué casilla, la fase de la ronda, el último número y el historial.
 * Hasta 8 jugadores a la vez, cada uno con su propia lista de apuestas. Los pagos siguen las reglas europeas: pleno 35 a 1, caballo 17,
 * calle y trío 11, cuadro y primeros cuatro 8, seisena 5, columnas y docenas 2, chances simples 1 (y si sale el cero se recupera la mitad).
 */
public final class RouletteTable {
    public static final int MAX_PLAYERS = 8;
    /** Lo máximo que un jugador puede poner en una casilla y en total en una tirada. */
    public static final int MAX_SPOT = 1000;
    public static final int MAX_TOTAL = 5000;
    public static final int HISTORY = 14;
    /** Fichas que se pueden apostar de una vez (las de la lista de denominaciones). */
    public static final int[] CHIPS = {1, 5, 25, 100, 500};

    public enum Phase { IDLE, BETTING, SPINNING, RESULT }

    /** Resultados de colocar o quitar una ficha. */
    public static final int OK = 0, BAD_SPOT = 1, BAD_PHASE = 2, SPOT_LIMIT = 3, TOTAL_LIMIT = 4, NO_SLOT = 5;

    public static final class Slot {
        public final int[] stake = new int[RouletteBets.SPOTS.size()];
        public int total;
        public boolean ready;
        /** Después de la tirada: lo que se le paga (apuesta incluida) y cuánto ganó o perdió en neto. */
        public int payout;
        public int net;
        /** Para deshacer: las últimas fichas puestas (casilla, cantidad). */
        public final List<int[]> history = new ArrayList<>();

        public void clear() {
            Arrays.fill(stake, 0);
            total = 0;
            ready = false;
            payout = 0;
            net = 0;
            history.clear();
        }
    }

    public final Slot[] slots = new Slot[MAX_PLAYERS];
    public Phase phase = Phase.IDLE;
    public int winning = -1;
    /** Últimos números que salieron (el más reciente primero). */
    public final List<Integer> past = new ArrayList<>();

    public RouletteTable() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = new Slot();
        }
    }

    public int place(int slot, int spot, int amount) {
        if (slot < 0 || slot >= MAX_PLAYERS) {
            return NO_SLOT;
        }
        if (phase != Phase.IDLE && phase != Phase.BETTING) {
            return BAD_PHASE;
        }
        if (spot < 0 || spot >= RouletteBets.SPOTS.size() || amount <= 0) {
            return BAD_SPOT;
        }
        Slot s = slots[slot];
        if (s.stake[spot] + amount > MAX_SPOT) {
            return SPOT_LIMIT;
        }
        if (s.total + amount > MAX_TOTAL) {
            return TOTAL_LIMIT;
        }
        s.stake[spot] += amount;
        s.total += amount;
        s.ready = false;
        s.history.add(new int[]{spot, amount});
        if (s.history.size() > 80) {
            s.history.remove(0);
        }
        return OK;
    }

    /** Quita hasta "amount" fichas de una casilla; devuelve cuánto se quitó realmente. */
    public int remove(int slot, int spot, int amount) {
        if (slot < 0 || slot >= MAX_PLAYERS || (phase != Phase.IDLE && phase != Phase.BETTING) || spot < 0 || spot >= RouletteBets.SPOTS.size()) {
            return 0;
        }
        Slot s = slots[slot];
        int q = Math.min(s.stake[spot], Math.max(0, amount));
        s.stake[spot] -= q;
        s.total -= q;
        s.ready = false;
        int left = q;
        for (int i = s.history.size() - 1; i >= 0 && left > 0; i--) {
            int[] h = s.history.get(i);
            if (h[0] == spot) {
                int t = Math.min(h[1], left);
                h[1] -= t;
                left -= t;
                if (h[1] <= 0) {
                    s.history.remove(i);
                }
            }
        }
        return q;
    }

    /** Deshace la última apuesta puesta: devuelve (casilla, cantidad) o null si no hay nada que deshacer. */
    public int[] undo(int slot) {
        if (slot < 0 || slot >= MAX_PLAYERS || (phase != Phase.IDLE && phase != Phase.BETTING)) {
            return null;
        }
        Slot s = slots[slot];
        while (!s.history.isEmpty()) {
            int[] h = s.history.remove(s.history.size() - 1);
            int q = Math.min(h[1], s.stake[h[0]]);
            if (q > 0) {
                s.stake[h[0]] -= q;
                s.total -= q;
                s.ready = false;
                return new int[]{h[0], q};
            }
        }
        return null;
    }

    /** Quita todas las apuestas de un jugador y devuelve cuánto era. */
    public int clear(int slot) {
        if (slot < 0 || slot >= MAX_PLAYERS || (phase != Phase.IDLE && phase != Phase.BETTING)) {
            return 0;
        }
        int t = slots[slot].total;
        slots[slot].clear();
        return t;
    }

    public int betCount() {
        int n = 0;
        for (Slot s : slots) {
            if (s.total > 0) {
                n++;
            }
        }
        return n;
    }

    public int totalBets() {
        int t = 0;
        for (Slot s : slots) {
            t += s.total;
        }
        return t;
    }

    /** Sale este número: se calcula lo que se paga a cada jugador. */
    public void settle(int number) {
        winning = number;
        past.add(0, number);
        while (past.size() > HISTORY) {
            past.remove(past.size() - 1);
        }
        for (Slot s : slots) {
            int pay = 0;
            for (int i = 0; i < s.stake.length; i++) {
                if (s.stake[i] > 0) {
                    pay += RouletteBets.payoutFor(RouletteBets.SPOTS.get(i), s.stake[i], number);
                }
            }
            s.payout = pay;
            s.net = pay - s.total;
        }
        phase = Phase.RESULT;
    }

    public void clearRound() {
        for (Slot s : slots) {
            s.clear();
        }
        phase = Phase.IDLE;
    }
}
