package com.pooltable.game;

/**
 * La física del hockey de mesa en el plano (sin nada de Minecraft, así se prueba sola). Marco: x a lo largo de la mesa (de -HL a HL),
 * z a lo ancho (de -HW a HW). El jugador A defiende la portería de x = -HL y el B la de x = +HL.
 * El disco rebota en las bandas (casi sin pérdida) y entra en las porterías; los mazos (uno por jugador) se mueven hacia su objetivo con
 * una velocidad máxima y no pueden pasar de su mitad. Al golpear, el disco recibe la velocidad del mazo.
 */
public final class HockeyPhysics {
    public static final double HL = 1.3;
    public static final double HW = 0.7;
    public static final double GOAL_HALF = 0.26;
    public static final double PUCK_R = 0.055;
    public static final double MALLET_R = 0.09;
    public static final double MAX_PUCK = 7.5;
    public static final double DRAG = 0.22;
    public static final double WALL_REST = 0.92;
    public static final double MALLET_REST = 0.85;

    public double px;
    public double pz;
    public double pvx;
    public double pvz;
    /** Mazos: [0] = A (mitad x < 0), [1] = B (mitad x > 0). */
    public final double[] mx = {-0.9, 0.9};
    public final double[] mz = {0.0, 0.0};
    public final double[] mvx = new double[2];
    public final double[] mvz = new double[2];
    public final double[] tx = {-0.9, 0.9};
    public final double[] tz = {0.0, 0.0};
    /** Velocidad máxima de cada mazo (bloques por segundo). */
    public final double[] maxSpeed = {5.0, 5.0};

    /** Choques desde la última vez que se pidieron (para los sonidos): golpes de mazo (con su rapidez más fuerte) y rebotes en banda. */
    public int mallethits = 0;
    public double hitSpeed = 0;
    public int wallHits = 0;
    /** Qué mazo dio el último golpe (0 = A, 1 = B). */
    public int hitSide = 0;
    /** 0 = nada, 1 = gol del jugador A (entró en la portería de B), 2 = gol del B. */
    public int goal = 0;

    public void placePuck(double x, double z) {
        px = x;
        pz = z;
        pvx = 0;
        pvz = 0;
    }

    public static double minX(int side) {
        return side == 0 ? -HL + MALLET_R : 0.03;
    }

    public static double maxX(int side) {
        return side == 0 ? -0.03 : HL - MALLET_R;
    }

    /** Pone un mazo en un sitio (dentro de su mitad) sin movimiento y apuntando ahí. */
    public void putMallet(int side, double x, double z) {
        mx[side] = Math.max(minX(side), Math.min(maxX(side), x));
        mz[side] = Math.max(-HW + MALLET_R, Math.min(HW - MALLET_R, z));
        tx[side] = mx[side];
        tz[side] = mz[side];
        mvx[side] = 0;
        mvz[side] = 0;
    }

    /** Paso máximo interno: con 1/240 s ni el disco ni los mazos avanzan más de 5 cm por paso (el disco y el mazo miden 14,5 cm juntos), así no se atraviesan. */
    private static final double SUB = 1.0 / 240.0;

    /** Mueve los mazos hacia sus objetivos (y los guarda en su mitad). Con {@code puck} = false el disco no se mueve (cuenta atrás, tras un gol). */
    public void step(double dt, boolean puck) {
        int n = Math.max(1, (int) Math.ceil(dt / SUB - 1e-9));
        double h = dt / n;
        for (int i = 0; i < n; i++) {
            substep(h, puck);
        }
    }

    private void substep(double dt, boolean puck) {
        for (int s = 0; s < 2; s++) {
            double gx = Math.max(minX(s), Math.min(maxX(s), tx[s]));
            double gz = Math.max(-HW + MALLET_R, Math.min(HW - MALLET_R, tz[s]));
            double dx = gx - mx[s];
            double dz = gz - mz[s];
            double d = Math.hypot(dx, dz);
            double reach = maxSpeed[s] * dt;
            double ox = mx[s];
            double oz = mz[s];
            if (d <= reach) {
                mx[s] = gx;
                mz[s] = gz;
            } else {
                mx[s] += dx / d * reach;
                mz[s] += dz / d * reach;
            }
            mvx[s] = (mx[s] - ox) / dt;
            mvz[s] = (mz[s] - oz) / dt;
        }
        if (!puck) {
            return;
        }
        double k = Math.max(0.0, 1.0 - DRAG * dt);
        pvx *= k;
        pvz *= k;
        px += pvx * dt;
        pz += pvz * dt;
        // bandas largas
        if (pz > HW - PUCK_R) {
            pz = HW - PUCK_R;
            if (pvz > 0) {
                pvz = -pvz * WALL_REST;
                wallHits++;
            }
        } else if (pz < -HW + PUCK_R) {
            pz = -HW + PUCK_R;
            if (pvz < 0) {
                pvz = -pvz * WALL_REST;
                wallHits++;
            }
        }
        // bandas cortas con la portería en el centro
        boolean mouth = Math.abs(pz) < GOAL_HALF - PUCK_R * 0.5;
        if (px > HL - PUCK_R && !mouth) {
            px = HL - PUCK_R;
            if (pvx > 0) {
                pvx = -pvx * WALL_REST;
                wallHits++;
            }
        } else if (px < -HL + PUCK_R && !mouth) {
            px = -HL + PUCK_R;
            if (pvx < 0) {
                pvx = -pvx * WALL_REST;
                wallHits++;
            }
        }
        if (px > HL + PUCK_R) {
            goal = 1;
        } else if (px < -HL - PUCK_R) {
            goal = 2;
        }
        for (int s = 0; s < 2; s++) {
            double dx = px - mx[s];
            double dz = pz - mz[s];
            double d = Math.hypot(dx, dz);
            double r = PUCK_R + MALLET_R;
            if (d >= r) {
                continue;
            }
            double nx;
            double nz;
            if (d < 1e-9) {
                nx = s == 0 ? 1 : -1;
                nz = 0;
                d = 0;
            } else {
                nx = dx / d;
                nz = dz / d;
            }
            double rel = (pvx - mvx[s]) * nx + (pvz - mvz[s]) * nz;
            if (rel < 0) {
                double j = -(1 + MALLET_REST) * rel;
                pvx += j * nx;
                pvz += j * nz;
                mallethits++;
                hitSide = s;
                hitSpeed = Math.max(hitSpeed, -rel);
            }
            px = mx[s] + nx * r;
            pz = mz[s] + nz * r;
            // el disco no puede quedar fuera de la cancha por un empujón
            pz = Math.max(-HW + PUCK_R, Math.min(HW - PUCK_R, pz));
            if (!(Math.abs(pz) < GOAL_HALF)) {
                px = Math.max(-HL + PUCK_R, Math.min(HL - PUCK_R, px));
            }
        }
        double sp = Math.hypot(pvx, pvz);
        if (sp > MAX_PUCK) {
            pvx *= MAX_PUCK / sp;
            pvz *= MAX_PUCK / sp;
        }
    }

    public void clearEvents() {
        mallethits = 0;
        hitSpeed = 0;
        wallHits = 0;
        goal = 0;
    }
}
