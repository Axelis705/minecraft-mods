package com.pooltable.game;

import java.util.Random;

/**
 * La dificultad del lanzamiento (sin nada de Minecraft): mientras se mantiene el clic, un indicador sube y baja solo por la barra.
 * Hay que soltar cuando está dentro de la zona verde: ahí el tiro sale casi recto y fuerte; fuera de ella sale con más fuerza o menos de la
 * debida y se desvía al azar. Incluso en la zona verde queda un pequeño error, así que repetir el mismo gesto no da chuzas sin parar.
 */
public final class BowlingAim {
    /** Ticks que tarda el indicador en ir y volver (subir 18 y bajar 18). */
    public static final int PERIOD = 36;
    public static final double SWEET_LO = 0.80;
    public static final double SWEET_HI = 0.92;
    public static final double SWEET = 0.86;

    public static final double SPEED_MIN = 3.5;
    public static final double SPEED_MAX = 8.0;
    /** Error base (grados, bloques, efecto) incluso en el punto perfecto, y lo que crece por cada punto fuera de la zona. */
    public static final double ANGLE_BASE = 1.0;
    public static final double ANGLE_GROW = 24.0;
    public static final double LATERAL_BASE = 0.04;
    public static final double LATERAL_GROW = 0.65;
    public static final double SPIN_BASE = 0.12;
    public static final double SPIN_GROW = 1.4;

    public double angleErr;
    public double lateralErr;
    public double spinErr;
    public double speed;
    public double meter;

    /** Posición del indicador (de 0 a 1) tras {@code ticks} ticks de carga: sube hasta 1 y baja hasta 0 una y otra vez. */
    public static double meter(double ticks) {
        double t = (ticks % PERIOD) / PERIOD;
        if (t < 0) {
            t += 1.0;
        }
        return t < 0.5 ? t * 2.0 : 2.0 - t * 2.0;
    }

    /** Cuánto se salió de la zona verde (0 dentro de ella). */
    public static double miss(double m) {
        return m < SWEET_LO ? SWEET_LO - m : m > SWEET_HI ? m - SWEET_HI : 0.0;
    }

    /** Calcula el tiro: velocidad según el indicador, y los errores al azar según qué tan lejos de la zona verde se soltó. */
    public static BowlingAim shoot(double m, Random rnd) {
        BowlingAim a = new BowlingAim();
        double off = miss(m);
        a.meter = m;
        a.speed = SPEED_MIN + (SPEED_MAX - SPEED_MIN) * m;
        a.angleErr = rnd.nextGaussian() * (ANGLE_BASE + ANGLE_GROW * off);
        a.lateralErr = rnd.nextGaussian() * (LATERAL_BASE + LATERAL_GROW * off);
        a.spinErr = rnd.nextGaussian() * (SPIN_BASE + SPIN_GROW * off);
        return a;
    }
}
