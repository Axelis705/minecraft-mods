package com.pooltable.game;

import java.util.Random;

/**
 * El rival de la máquina para el hockey de mesa. Juega de cualquiera de los dos lados. Tres niveles (0 fácil, 1 normal, 2 difícil) que cambian su
 * velocidad, cuánto se equivoca al predecir el disco y qué tan seguido ataca. Pone su objetivo en {@link #tx} y {@link #tz} (en el marco de la mesa).
 */
public final class HockeyAI {
    public static final double[] SPEED = {1.5, 2.6, 3.8};
    public static final double[] ERR = {0.22, 0.11, 0.05};
    private static final double[] SLOW = {0.35, 0.6, 0.9};
    private static final double[] DEFEND = {1.08, 1.0, 0.95};

    public double tx;
    public double tz;
    private double err = 0;
    private double errTime = 0;
    private final Random rnd;

    public HockeyAI(Random rnd) {
        this.rnd = rnd;
    }

    /** side 0 = juega con A (mitad x < 0), 1 = con B (x > 0). */
    public void update(HockeyPhysics ph, int side, int diff, double dt) {
        double sgn = side == 1 ? 1.0 : -1.0;
        // todo se calcula como si jugara en la mitad positiva, y al final se refleja
        double px = ph.px * sgn;
        double pz = ph.pz;
        double vx = ph.pvx * sgn;
        double vz = ph.pvz;
        double mx = ph.mx[side] * sgn;
        double mz = ph.mz[side];
        errTime -= dt;
        if (errTime <= 0) {
            err = rnd.nextGaussian() * ERR[diff];
            errTime = 0.3 + rnd.nextDouble() * 0.2;
        }
        double rr = HockeyPhysics.PUCK_R + HockeyPhysics.MALLET_R;
        double ox;
        double oz;
        boolean mine = px > 0.04;
        if (mine && Math.abs(vx) < SLOW[diff] && Math.hypot(vx, vz) < SLOW[diff] * 1.4) {
            // ataque: se coloca detrás del disco, en línea con la portería rival, y lo empuja
            double gx = -HockeyPhysics.HL;
            double gz = (rnd.nextDouble() - 0.5) * 0.2 + err * 0.6;
            double dx = gx - px;
            double dz = gz - pz;
            double d = Math.hypot(dx, dz);
            dx /= d;
            dz /= d;
            double bx = px - dx * (rr + 0.01);
            double bz = pz - dz * (rr + 0.01);
            double behind = Math.hypot(bx - mx, bz - mz);
            if (behind > 0.07 + 0.03 * (2 - diff)) {
                ox = bx;
                oz = bz;
            } else {
                ox = px + dx * 0.30;
                oz = pz + dz * 0.30;
            }
        } else if (vx > 0.25) {
            // defensa: el disco viene hacia mi portería; voy al punto donde cruzará mi línea de defensa
            double def = DEFEND[diff];
            double t = (def - px) / vx;
            double z = pz + vz * Math.min(t, 3.0);
            double w = HockeyPhysics.HW - HockeyPhysics.PUCK_R;
            double span = 4 * w;
            z = ((z + w) % span + span) % span; // doblar los rebotes en las bandas
            z = z > 2 * w ? span - z - w : z - w;
            ox = def;
            oz = z + err;
        } else {
            // espera: cerca de mi portería, siguiendo al disco de lejos
            ox = DEFEND[diff] - 0.15;
            oz = pz * 0.45 + err;
        }
        double minX = HockeyPhysics.minX(1);
        double maxX = HockeyPhysics.maxX(1);
        ox = Math.max(minX, Math.min(maxX, ox));
        oz = Math.max(-HockeyPhysics.HW + HockeyPhysics.MALLET_R, Math.min(HockeyPhysics.HW - HockeyPhysics.MALLET_R, oz));
        tx = ox * sgn;
        tz = oz;
    }
}
