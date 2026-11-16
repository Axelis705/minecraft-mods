package com.casino.game;

/**
 * Duelo al mediodía (Java puro): reglas, tiempos y premios. Los duelistas se paran en sus X. La campana suena tres veces, hay un
 * silencio de duración al azar y se grita "¡DESENFUNDA!" mostrando una tecla al azar del teclado. Gana quien la apriete primero
 * (el que dispara primero). Apretar antes del grito o salirse de la X pierde. Se juega contra un pistolero (3, cada uno más rápido y
 * mejor pagado) o contra otro jugador (el ganador se lleva las dos apuestas menos una comisión del 5 %).
 */
public final class DuelRules {
    public static final int TPS = 20;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};

    /** Kid Pecas, Black Bart, el Doctor. */
    public static final int OPPONENTS = 3;
    /** Tiempo de reacción de cada pistolero (milisegundos desde que sale la tecla) y cuánto varía. */
    public static final int[] NPC_MS = {900, 740, 620};
    public static final int[] NPC_SD = {150, 110, 80};
    /** Premio (x10) contra cada uno, y lo que se suma si el duelo es al mediodía. */
    public static final int[] PAY_X10 = {15, 20, 40};
    public static final int NOON_BONUS_X10 = 5;
    /** Entre jugadores el ganador cobra esto (x10) de su apuesta: las dos apuestas menos el 5 % de la casa. */
    public static final int PVP_PAY_X10 = 19;
    /** Piel de vaquero (0..7) de cada pistolero. */
    public static final int[] SKIN = {1, 2, 5};

    /** Línea de tiempo (ticks desde que empieza): tres campanadas y un silencio al azar antes del grito. */
    public static final int TOLL_1 = 40, TOLL_2 = 60, TOLL_3 = 80;
    public static final int WAIT_MIN = 20, WAIT_MAX = 70;
    /** Después del primer disparo bueno, se espera esto (ticks) por si el otro disparó antes pero su aviso viene en camino. */
    public static final int GRACE = 8;
    /** Si nadie dispara en este tiempo después del grito, se cancela (entre jugadores se devuelven las apuestas). */
    public static final int TIMEOUT = 120;
    /** Apretar una tecla equivocada traba el revólver este tiempo (milisegundos). */
    public static final int WRONG_LOCK_MS = 500;
    /** Cuánto espera un reto entre jugadores a que alguien lo acepte (ticks). */
    public static final int CHALLENGE_WAIT = 60 * TPS;
    /** Distancia entre las dos X (bloques). */
    public static final int DISTANCE = 16;
    /** Teclas que pueden salir (letras y números; en todos los teclados latinos están en el mismo lugar). */
    public static final String KEYS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";

    private DuelRules() {
    }

    /** Tick (desde el inicio) del grito "¡DESENFUNDA!" para un número al azar r en [0, 1). */
    public static int drawTick(double r) {
        return TOLL_3 + WAIT_MIN + (int) (r * (WAIT_MAX - WAIT_MIN + 1));
    }

    /** La tecla para un número al azar r en [0, 1) (siempre una letra o número en mayúscula; su código GLFW es el mismo). */
    public static char key(double r) {
        return KEYS.charAt(Math.min(KEYS.length() - 1, (int) (r * KEYS.length())));
    }

    /** Reacción del pistolero con un valor normal g (media 0, desviación 1): nunca menos de 250 ms. */
    public static int npcMs(int opp, double g) {
        return Math.max(250, (int) Math.round(NPC_MS[opp] + g * NPC_SD[opp]));
    }

    /** ¿Es creíble ese tiempo de reacción? (nadie reacciona en menos de 80 ms) */
    public static boolean validMs(int ms) {
        return ms >= 80 && ms <= TIMEOUT * 50;
    }

    public static int payoutX10(int opp, boolean noon) {
        return PAY_X10[opp] + (noon ? NOON_BONUS_X10 : 0);
    }

    public static long payout(long bet, int opp, boolean noon) {
        return bet * payoutX10(opp, noon) / 10;
    }

    public static long pvpPayout(long bet) {
        return bet * PVP_PAY_X10 / 10;
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
