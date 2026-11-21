package com.mundosdeaxelis.forja;

import java.util.Random;

/**
 * Reglas del juego del Gran Yunque (etapa 270), sin nada de dibujo: tres fases, más difíciles cuanto más alto el tier de la llave.
 * <ol>
 *     <li><b>Fundir</b>: mientras mantienes pulsado, el fuelle sube la temperatura; al soltar, baja (y el fuego no es del todo
 *     estable). Hay que tenerla dentro de la zona de fusión hasta llenar la barra antes de {@link #MELT_LIMIT} segundos. Si se pasa
 *     de la raya (sobrecalentado), la barra baja deprisa.</li>
 *     <li><b>Martillar</b>: un indicador va y viene por una barra; hay que golpear (clic) cuando pasa por el punto dorado. Cada
 *     golpe es perfecto, bueno o fallo. De 3 golpes (tier 1) a 6 (tier 8).</li>
 *     <li><b>Templar</b>: el metal se enfría (blanco → amarillo → naranja → rojo cereza → oscuro); hay que meter la llave en el
 *     agua (clic) cuando está en el rojo cereza.</li>
 * </ol>
 * Resultado: <b>fallida</b> si no se funde a tiempo o hay 2 fallos en total; <b>perfecta</b> si no hay fallos, como mucho un golpe
 * «bueno» y la fusión fue limpia; si no, <b>forjada</b>.
 */
public final class ForjaGame {
    public enum Phase { MELT, HAMMER, TEMPER, DONE }

    public enum Result { NONE, OK, PERFECT, FAIL }

    public static final double MELT_LIMIT = 15.0;

    public final int tier;
    private final Random rnd;
    public Phase phase = Phase.MELT;
    public Result result = Result.NONE;
    public String failReason = "";

    // ---------------------------------------------------------------- fundir
    public double temp = 0.12;
    public final double bandLo, bandHi;
    public double melt = 0.0;
    public double meltTime = 0.0;
    private double inBand = 0.0, total = 0.0;
    private double drift = 0.0, driftT = 0.0;
    public boolean holding = false;

    // ---------------------------------------------------------------- martillar
    public final int strikes;
    public int struck = 0;
    public double hammerPos = 0.0;
    private double hammerDir = 1.0;
    public final double hammerSpeed;
    public double sweet;
    public final double perfectHalf, goodHalf;
    public int perfects = 0, goods = 0, misses = 0;
    /** Último golpe: -1 ninguno, 0 fallo, 1 bueno, 2 perfecto; y hace cuánto (segundos). */
    public int lastHit = -1;
    public double lastHitT = 99.0;

    // ---------------------------------------------------------------- templar
    public double cool = 0.0;
    public final double coolSpeed;
    public final double target, targetHalf;
    public int temperHit = -1;

    public ForjaGame(int tier, long seed) {
        this.tier = Math.max(1, Math.min(8, tier));
        this.rnd = new Random(seed);
        int t = this.tier - 1;
        double w = 0.24 - 0.018 * t;
        double c = 0.55 + rnd.nextDouble() * 0.2;
        bandLo = c - w / 2;
        bandHi = c + w / 2;
        strikes = 3 + (t * 3) / 7;
        hammerSpeed = 0.85 + 0.12 * t;
        perfectHalf = 0.045 - 0.0015 * t;
        goodHalf = 0.12 - 0.004 * t;
        sweet = 0.25 + rnd.nextDouble() * 0.5;
        coolSpeed = 0.20 + 0.025 * t;
        target = 0.62 + (rnd.nextDouble() - 0.5) * 0.12;
        targetHalf = 0.075 - 0.003 * t;
    }

    /** Pulsar (clic o Espacio). En la fusión empieza a soplar; en las otras fases es el golpe o el temple. */
    public void press() {
        switch (phase) {
            case MELT -> holding = true;
            case HAMMER -> strike();
            case TEMPER -> temper();
            default -> {
            }
        }
    }

    public void release() {
        holding = false;
    }

    public boolean inBand() {
        return temp >= bandLo && temp <= bandHi;
    }

    public void tick(double dt) {
        lastHitT += dt;
        switch (phase) {
            case MELT -> tickMelt(dt);
            case HAMMER -> {
                hammerPos += hammerDir * hammerSpeed * dt;
                if (hammerPos > 1.0) {
                    hammerPos = 2.0 - hammerPos;
                    hammerDir = -1.0;
                } else if (hammerPos < 0.0) {
                    hammerPos = -hammerPos;
                    hammerDir = 1.0;
                }
            }
            case TEMPER -> {
                cool += coolSpeed * dt;
                if (cool >= 1.0) {
                    cool = 1.0;
                    temperHit = 0;
                    misses++;
                    finish();
                }
            }
            default -> {
            }
        }
    }

    private void tickMelt(double dt) {
        meltTime += dt;
        // el fuego no es estable: un empujón suave que cambia cada poco (más fuerte en los tiers altos)
        driftT -= dt;
        if (driftT <= 0) {
            driftT = 0.6 + rnd.nextDouble() * 0.8;
            drift = (rnd.nextDouble() - 0.5) * (0.10 + 0.03 * (tier - 1));
        }
        temp += (holding ? 0.55 : -0.40) * dt + drift * dt;
        temp = Math.max(0.0, Math.min(1.0, temp));
        total += dt;
        if (temp > 0.96) {
            melt = Math.max(0.0, melt - 0.25 * dt);
        } else if (inBand()) {
            inBand += dt;
            melt = Math.min(1.0, melt + 0.17 * dt);
        } else {
            melt = Math.max(0.0, melt - 0.04 * dt);
        }
        if (melt >= 1.0) {
            phase = Phase.HAMMER;
            holding = false;
        } else if (meltTime >= MELT_LIMIT) {
            failReason = "melt";
            result = Result.FAIL;
            phase = Phase.DONE;
        }
    }

    private void strike() {
        double d = Math.abs(hammerPos - sweet);
        if (d <= perfectHalf) {
            perfects++;
            lastHit = 2;
        } else if (d <= goodHalf) {
            goods++;
            lastHit = 1;
        } else {
            misses++;
            lastHit = 0;
        }
        lastHitT = 0.0;
        struck++;
        sweet = 0.2 + rnd.nextDouble() * 0.6;
        if (misses >= 2) {
            failReason = "hammer";
            result = Result.FAIL;
            phase = Phase.DONE;
        } else if (struck >= strikes) {
            phase = Phase.TEMPER;
        }
    }

    private void temper() {
        double d = Math.abs(cool - target);
        if (d <= targetHalf * 0.5) {
            temperHit = 2;
            perfects++;
        } else if (d <= targetHalf) {
            temperHit = 1;
            goods++;
        } else {
            temperHit = 0;
            misses++;
        }
        lastHit = temperHit;
        lastHitT = 0.0;
        finish();
    }

    private void finish() {
        phase = Phase.DONE;
        if (misses >= 2) {
            failReason = "temper";
            result = Result.FAIL;
        } else if (misses == 0 && goods <= 1 && meltClean()) {
            result = Result.PERFECT;
        } else {
            result = Result.OK;
        }
    }

    /** ¿La fusión fue limpia? (más de la mitad del tiempo dentro de la zona). */
    public boolean meltClean() {
        return total <= 0 || inBand / total >= 0.5;
    }

    /** Código del resultado para el servidor: 1 forjada, 2 perfecta, 3 fallida (0 = sin empezar). */
    public int code() {
        return switch (result) {
            case OK -> 1;
            case PERFECT -> 2;
            case FAIL -> 3;
            default -> 0;
        };
    }
}
