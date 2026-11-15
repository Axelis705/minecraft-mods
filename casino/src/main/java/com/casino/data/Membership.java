package com.casino.data;

/**
 * Tarjeta de socio del casino: cinco niveles según lo que llevas apostado (Bronce, Plata, Oro, Platino, Diamante), un bono diario que
 * crece con el nivel y 20 logros con premio en fichas. Los números viven aquí; quién gana qué lo decide {@link CasinoStats}.
 */
public final class Membership {
    private Membership() {
    }

    // ------------------------------------------------------------------ niveles

    public static final String[] LEVELS = {"bronze", "silver", "gold", "platinum", "diamond"};
    /** Fichas apostadas (en total) para llegar a cada nivel. */
    public static final long[] AT = {0, 2_000, 10_000, 50_000, 200_000};
    /** Premio en fichas al subir a cada nivel. */
    public static final int[] LEVEL_REWARD = {0, 100, 500, 2_000, 10_000};
    /** Bono diario de cada nivel. */
    public static final int[] DAILY = {25, 50, 150, 400, 1_000};
    /** Color de cada nivel (tarjeta, títulos). */
    public static final int[] COLOR = {0xCD7F32, 0xC9D1DB, 0xF2C230, 0x8FD8E8, 0x7EF0FF};

    public static int level(long wagered) {
        int l = 0;
        for (int i = 0; i < AT.length; i++) {
            if (wagered >= AT[i]) {
                l = i;
            }
        }
        return l;
    }

    // ------------------------------------------------------------------ logros

    public static final int FIRST_BET = 0;
    public static final int HIGH_ROLLER = 1;
    public static final int BIG_WIN = 2;
    public static final int JACKPOT = 3;
    public static final int ALL_GAMES = 4;
    public static final int REGULAR = 5;
    public static final int VETERAN = 6;
    public static final int NATURAL = 7;
    public static final int POKER_FULL = 8;
    public static final int ROYAL = 9;
    public static final int STRAIGHT_UP = 10;
    public static final int SLOT_JACKPOT = 11;
    public static final int PLINKO_EDGE = 12;
    public static final int RACE_WIN = 13;
    public static final int SCRATCH_BIG = 14;
    public static final int WHEEL_FAN = 15;
    public static final int COCKTAILS = 16;
    public static final int VIP_GOLD = 17;
    public static final int DIAMOND = 18;
    public static final int NET_10K = 19;
    public static final int COUNT = 20;

    /** Fichas que da cada logro. */
    public static final int[] REWARD = {25, 200, 250, 2_000, 300, 150, 1_000, 100, 300, 5_000, 500, 1_500, 400, 250, 300, 200, 150, 500, 5_000, 2_500};

    /** Ruletas de la suerte y cócteles que hacen falta para sus logros. */
    public static final int WHEEL_NEEDED = 7;
    public static final int DRINKS_NEEDED = 10;
}
