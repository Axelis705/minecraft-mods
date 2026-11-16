package com.casino.game;

/**
 * Vencidas (pulso) en la mesa del saloon (Java puro). Los dos agarran las manos; cada uno aprieta dos teclas alternando lo más rápido
 * que puede (A y D). La fuerza de cada lado es su ritmo de golpes por segundo (suavizado); las manos se inclinan hacia el más lento.
 * Gana quien tumbe la mano del otro (|x| llega a 1). Si en 30 s nadie gana, gana quien la tenga inclinada a su favor.
 * Contra un forzudo (3, cada uno más fuerte y mejor pagado) o contra otro jugador (el ganador se lleva las dos apuestas menos 5 %).
 */
public final class ArmRules {
    public static final int TPS = 20;
    public static final int[] BETS = {10, 25, 50, 100, 250, 500};
    /** Gus el Herrero, el Oso y Rosie la Brava. */
    public static final int OPPONENTS = 3;
    /** Ritmo de cada forzudo (golpes por segundo), cuánto sube y baja, y su premio (x10). */
    public static final double[] NPC_RATE = {6.6, 7.6, 8.6};
    public static final double[] NPC_SWING = {1.2, 1.4, 1.5};
    public static final int[] PAY_X10 = {15, 20, 35};
    public static final int PVP_PAY_X10 = 19;
    /** Piel de vaquero (0..7) de cada forzudo. */
    public static final int[] SKIN = {7, 0, 4};
    /** Cuenta regresiva (3, 2, 1, ¡YA!) y tiempo máximo de la lucha (ticks). */
    public static final int COUNTDOWN = 60;
    public static final int MAX_TICKS = 30 * TPS;
    /** Qué tan rápido se mueven las manos por cada golpe/s de diferencia (por segundo). */
    public static final double K = 0.095;
    /** Suavizado del ritmo (por tick). */
    public static final double EMA = 0.12;
    /** Más golpes que estos por tick no cuentan (nadie aprieta 60 veces por segundo). */
    public static final int MAX_TAPS_PER_TICK = 3;

    private ArmRules() {
    }

    /** Ritmo suavizado nuevo con los golpes de este tick. */
    public static double rate(double prev, int taps) {
        return prev * (1 - EMA) + Math.min(taps, MAX_TAPS_PER_TICK) * TPS * EMA;
    }

    /** Ritmo del forzudo en el tick t de la lucha (con sus empujones; phase lo hace distinto cada vez). */
    public static double npcRate(int opp, int t, double phase) {
        double ramp = Math.min(1, t / 12.0);
        double swing = Math.sin(t * 0.11 + phase) * NPC_SWING[opp] + Math.sin(t * 0.037 + phase * 2.3) * NPC_SWING[opp] * 0.5;
        double tired = 1 - Math.min(0.15, t / (double) MAX_TICKS * 0.15);
        return Math.max(0, (NPC_RATE[opp] + swing) * ramp * tired);
    }

    /** Un tick: dónde quedan las manos (x > 0 = va ganando el lado 0). */
    public static double step(double x, double rate0, double rate1, double noise) {
        return Math.max(-1, Math.min(1, x + K * (rate0 - rate1) / TPS + noise * 0.004));
    }

    /** ¿Ya ganó alguien? 0, 1 o -1. */
    public static int winner(double x) {
        return x >= 1 ? 0 : x <= -1 ? 1 : -1;
    }

    /** Ángulo de las manos para dibujar (grados, positivo = hacia el lado 1, es decir, ganando el lado 0). */
    public static double angle(double x) {
        return x * 75;
    }

    public static long payout(long bet, int opp) {
        return bet * PAY_X10[opp] / 10;
    }

    public static long pvpPayout(long bet) {
        return bet * PVP_PAY_X10 / 10;
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
