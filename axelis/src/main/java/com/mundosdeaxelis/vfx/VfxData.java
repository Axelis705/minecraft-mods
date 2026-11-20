package com.mundosdeaxelis.vfx;

/** Estado de un efecto visual (se usa en el cliente y en las pruebas). */
public final class VfxData {
    public static final int TRAIL_MAX = 28;

    public int id, type, rgb, flags, life, follow;
    public float size, a, b;
    /** Posición actual y anterior (para interpolar entre ticks). */
    public double x, y, z, px, py, pz;
    /** Llegó una actualización de posición en este tick (para suavizar el movimiento). */
    public boolean updated;
    /** Vector auxiliar (dirección, segundo punto, velocidad...). */
    public double dx, dy, dz;
    /** Desplazamiento respecto a la entidad seguida. */
    public double ox, oy, oz;
    /** Tick del cliente en el que apareció, y edad (en ticks) a la que debe terminar (<=0: sin fin). */
    public long start;
    public double endAge;
    /** Orientación suavizada (solo para modelos 3D): la actual, la del tick anterior y si ya se inicializó. */
    public double yaw, pyaw;
    public boolean yawInit;
    /**
     * "Movimiento con vida" de las criaturas 3D, puesto al día cada tick por el cliente (con su valor del tick anterior para
     * interpolar): velocidad suavizada, fase de la zancada (avanza con la distancia recorrida: las patas no "patinan"),
     * inclinación al acelerar o frenar, ladeo al girar y aplastado al aterrizar.
     */
    public double mSpd, pmSpd, mGait, pmGait, mLean, pmLean, mLeanV, mBank, pmBank, mBankV, mSq, pmSq, mSqV, mVy;
    public boolean mInit;
    /**
     * Más vida de las criaturas 3D: ticks seguidos sin moverse (para dormirse), giro en el sitio suavizado (para dar
     * pasitos al girar), ticks del susto al despertar y luz del mundo donde está (0..1; la pone el cliente).
     */
    public double mIdle, mTurn, pmTurn;
    public int mWake;
    /** Reacciones al jugador cercano: saltito cuando salta (ticks que quedan) y sentarse cuando se agacha (0..1). */
    public int mHop;
    public double mSit, pmSit;
    public float light = 1F;
    /**
     * Etapa 254: dibujada como entidad de verdad. Entonces no pone su sombrita (la sombra es la del juego), el giro del cuerpo
     * llega ya calculado ({@code eYaw}, en radianes del modelo), la cabeza mira hacia donde mira la entidad ({@code eLookYaw},
     * {@code eLookPitch}, con fuerza {@code eLook}) y {@code eYawOff} es un giro extra (el truco de dar una vuelta).
     */
    public boolean entity;
    public double eYaw, eYawOff, eLookYaw, eLookPitch, eLook;
    public double tx, ty, tz;
    public boolean hasT;
    public final double[] trail = new double[TRAIL_MAX * 3];
    public int trailN;

    public void pushTrail(double tx, double ty, double tz) {
        if (trailN > 0) {
            int last = (trailN - 1) * 3;
            double ddx = trail[last] - tx, ddy = trail[last + 1] - ty, ddz = trail[last + 2] - tz;
            if (ddx * ddx + ddy * ddy + ddz * ddz < 1.0E-6) {
                return;
            }
        }
        if (trailN == TRAIL_MAX) {
            System.arraycopy(trail, 3, trail, 0, (TRAIL_MAX - 1) * 3);
            trailN--;
        }
        int i = trailN * 3;
        trail[i] = tx;
        trail[i + 1] = ty;
        trail[i + 2] = tz;
        trailN++;
    }
}
