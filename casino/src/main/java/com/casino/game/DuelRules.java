package com.casino.game;

/**
 * Duelo al mediodía (Java puro): reglas, tiempos y premios. El jugador se para en la X de la calle con el revólver enfundado (mirando
 * al suelo). La campana suena tres veces, hay un silencio de duración al azar y se grita "¡DESENFUNDA!". Gana quien acierte primero;
 * si el jugador dispara o levanta la mirada antes de tiempo, pierde. Hay tres pistoleros: más rápidos y certeros pagan más.
 */
public final class DuelRules {
    public static final int TPS = 20;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /** Kid Pecas, Black Bart, el Doctor. */
    public static final int OPPONENTS = 3;
    /** Cuánto tarda cada pistolero en disparar después del grito (ticks) y cuánto varía. */
    public static final int[] REACTION = {16, 13, 11};
    public static final int[] JITTER = {3, 2, 2};
    /** Qué tan seguido acierta cada uno; si falla, vuelve a disparar un poco después. */
    public static final double[] ACCURACY = {0.72, 0.82, 0.90};
    public static final int REFIRE = 9;
    /** Premio (x10) contra cada uno, y lo que se suma si el duelo es al mediodía. */
    public static final int[] PAY_X10 = {15, 20, 35};
    public static final int NOON_BONUS_X10 = 5;
    /** Piel de vaquero (0..7) de cada pistolero. */
    public static final int[] SKIN = {1, 2, 5};

    /** Línea de tiempo (ticks desde que empieza): el pistolero llega, tres campanadas y un silencio al azar. */
    public static final int TOLL_1 = 40, TOLL_2 = 60, TOLL_3 = 80;
    public static final int WAIT_MIN = 20, WAIT_MAX = 70;
    /** Si nadie acierta en este tiempo después del grito, el pistolero no falla más. */
    public static final int LAST_CHANCE = 60;
    /** Distancia entre las dos X (bloques) y balas del jugador. */
    public static final int DISTANCE = 16;
    public static final int SHOTS = 6;
    /** Para tener el revólver "enfundado" hay que mirar hacia abajo al menos esto (grados; en Minecraft abajo es positivo). */
    public static final float HOLSTER_PITCH = 45F;

    private DuelRules() {
    }

    // azar propio (igual en todas partes)
    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double rand(long seed, int k) {
        return (mix(seed + 0x9E3779B97F4A7C15L * (k + 1)) >>> 11) * 0x1.0p-53;
    }

    /** Tick (desde el inicio) del grito "¡DESENFUNDA!". */
    public static int drawTick(long seed) {
        return TOLL_3 + WAIT_MIN + (int) (rand(seed, 0) * (WAIT_MAX - WAIT_MIN + 1));
    }

    /** Tick del disparo número n (0, 1, 2...) del pistolero. */
    public static int npcShot(long seed, int opp, int n) {
        int base = drawTick(seed) + REACTION[opp] + (int) Math.round((rand(seed, 1) * 2 - 1) * JITTER[opp]);
        return base + n * REFIRE;
    }

    /** ¿Acierta el disparo n del pistolero? Pasado el último aviso ya no falla. */
    public static boolean npcHits(long seed, int opp, int n) {
        int t = npcShot(seed, opp, n) - drawTick(seed);
        return t >= LAST_CHANCE || rand(seed, 10 + n) < ACCURACY[opp];
    }

    /** Tick en que el pistolero te da (el primero de sus disparos que acierta). */
    public static int npcKillTick(long seed, int opp) {
        for (int n = 0; ; n++) {
            if (npcHits(seed, opp, n)) {
                return npcShot(seed, opp, n);
            }
        }
    }

    public static int payoutX10(int opp, boolean noon) {
        return PAY_X10[opp] + (noon ? NOON_BONUS_X10 : 0);
    }

    public static long payout(long bet, int opp, boolean noon) {
        return bet * payoutX10(opp, noon) / 10;
    }

    /** Mediodía en el juego: de las 11:00 a las 13:00 (5000 a 7000 ticks del día). */
    public static boolean noon(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= 5000 && t <= 7000;
    }

    public static boolean validBet(int bet) {
        for (int b : BETS) {
            if (b == bet) {
                return true;
            }
        }
        return false;
    }
}
