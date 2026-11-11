package com.autoscoleccion.game;

import java.util.Random;

/**
 * Las tres líneas de cajas. La caja N trae los autos de su línea y de las anteriores (caja 1: 10 autos, caja 2: 20, caja 3: 30).
 * Dentro de cada caja cada auto tiene su propio peso según su rareza; los autos de líneas anteriores salen menos en las cajas nuevas.
 * La máquina expendedora decide qué caja sale según el puntaje (0 a 1) de la prueba de habilidad.
 */
public final class Boxes {
    public static final int COUNT = 3;
    public static final String[] ID = {"caja_ruta_urbana", "caja_pista_nocturna", "caja_leyendas"};
    public static final String[] NAME = {"Ruta Urbana", "Pista Nocturna", "Leyendas del Circuito"};
    public static final String[] TAGLINE = {"Autos de la calle", "Tuneados y neón", "Superautos y prototipos"};
    /** Color principal y secundario del empaque. */
    public static final int[] COLOR = {0xF2A93B, 0x8A3FFC, 0xE8C15A};
    public static final int[] COLOR2 = {0x2D6CDF, 0x16F2E0, 0x1A1A1A};
    /** En las cajas nuevas, los autos de líneas anteriores pesan esto por cada línea de diferencia. */
    public static final double OLD_FACTOR = 0.5;

    private Boxes() {
    }

    /** Peso del auto dentro de la caja {@code box} (0 si no puede salir en ella). */
    public static double weight(CarDef c, int box) {
        if (c.line > box) {
            return 0.0;
        }
        return c.rarity.weight * Math.pow(OLD_FACTOR, box - c.line);
    }

    /** Probabilidad (0 a 1) de que salga ese auto al abrir la caja. */
    public static double chance(CarDef c, int box) {
        double total = 0.0;
        for (CarDef d : Cars.all()) {
            total += weight(d, box);
        }
        return weight(c, box) / total;
    }

    /** Abre una caja: devuelve el índice (0-29) del auto. */
    public static int roll(int box, Random rnd) {
        double total = 0.0;
        for (CarDef d : Cars.all()) {
            total += weight(d, box);
        }
        double x = rnd.nextDouble() * total;
        for (CarDef d : Cars.all()) {
            double w = weight(d, box);
            if (w <= 0.0) {
                continue;
            }
            x -= w;
            if (x < 0.0) {
                return d.index();
            }
        }
        // por redondeo: el último que puede salir
        for (int i = Cars.COUNT - 1; i >= 0; i--) {
            if (weight(Cars.get(i), box) > 0.0) {
                return i;
            }
        }
        return 0;
    }

    /** Cuántos autos distintos trae la caja. */
    public static int size(int box) {
        int n = 0;
        for (CarDef d : Cars.all()) {
            if (d.line <= box) {
                n++;
            }
        }
        return n;
    }

    // ------------------------------------------------------------------ máquina: puntaje -> caja

    /**
     * Probabilidades de caja según el puntaje de la prueba (0 a 1): {Ruta Urbana, Pista Nocturna, Leyendas}. Con 0 casi siempre sale
     * la urbana; con un puntaje perfecto la de Leyendas sale un 35 % de las veces y la urbana solo un 5 %.
     */
    public static double[] odds(double score) {
        double s = Math.max(0.0, Math.min(1.0, score));
        double legend = 0.01 + 0.34 * Math.pow(s, 2.2);
        double night = 0.10 + 0.50 * Math.pow(s, 1.1);
        double urban = Math.max(0.0, 1.0 - legend - night);
        return new double[] {urban, night, legend};
    }

    public static int rollBox(double score, Random rnd) {
        double[] o = odds(score);
        double x = rnd.nextDouble();
        if (x < o[2]) {
            return 2;
        }
        if (x < o[2] + o[1]) {
            return 1;
        }
        return 0;
    }
}
