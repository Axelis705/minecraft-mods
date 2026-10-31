package com.pooltable.game;

/**
 * La física de la pista de boliche en el plano (sin nada de Minecraft, así se puede probar sola).
 * Marco: u = bloques desde el borde cercano de la pista hacia los pinos, v = a la derecha del centro de la pista (de -1 a 1).
 * La bola rueda con un poco de efecto, cae a la canaleta si se sale de la tabla, choca con los pinos (que se caen si el golpe es fuerte),
 * y los pinos caídos siguen rodando y pueden tirar a otros. Todo lo que pasa del fondo o cae por los lados desaparece.
 */
public final class BowlingPhysics {
    public static final int PINS = 10;
    public static final double LANE_L = 14.0;
    /** Mitad del ancho de la tabla (la pista juega 1,5 de ancho) y centro de la canaleta. */
    public static final double HALF_W = 0.75;
    public static final double GUTTER_V = 0.87;
    /** Línea de falta: donde sale la bola. */
    public static final double FOUL_U = 2.6;
    public static final double HEAD_U = 10.8;
    public static final double SPACING = 0.42;
    public static final double ROW = SPACING * 0.8660254;
    /** Más allá de esto la bola y los pinos caen al foso. */
    public static final double PIT_U = 12.75;
    public static final double DECK_END_U = HEAD_U + 3 * ROW + 0.30;
    public static final double BALL_R = 0.15;
    public static final double PIN_R = 0.085;
    public static final double DOWN_R = 0.11;

    public static final double BALL_MASS = 4.0;
    public static final double PIN_MASS = 1.0;
    /** Velocidad de choque mínima para tirar un pino que está de pie. */
    public static final double KNOCK = 0.22;
    public static final double HOOK = 1.1;

    public static final byte STANDING = 0;
    public static final byte DOWN = 1;
    public static final byte GONE = 2;

    public double bu;
    public double bv;
    public double bvu;
    public double bvv;
    public double spin;
    public boolean ballActive = false;
    public boolean inGutter = false;

    public final double[] pu = new double[PINS];
    public final double[] pv = new double[PINS];
    public final double[] pvu = new double[PINS];
    public final double[] pvv = new double[PINS];
    public final byte[] state = new byte[PINS];

    /** Contadores de choques desde la última vez que se pidieron (para los sonidos). */
    public int ballHits = 0;
    public int pinHits = 0;
    private double stillTime = 0;

    public static double spotU(int i) {
        return HEAD_U + row(i) * ROW;
    }

    public static double spotV(int i) {
        int r = row(i);
        int first = r * (r + 1) / 2;
        return (i - first - r / 2.0) * SPACING;
    }

    private static int row(int i) {
        return i == 0 ? 0 : i <= 2 ? 1 : i <= 5 ? 2 : 3;
    }

    public BowlingPhysics() {
        reset(0x3FF);
    }

    /** Coloca de pie los pinos de la máscara (bit i = pino i); los demás quedan fuera. Quita la bola. */
    public void reset(int mask) {
        for (int i = 0; i < PINS; i++) {
            pu[i] = spotU(i);
            pv[i] = spotV(i);
            pvu[i] = 0;
            pvv[i] = 0;
            state[i] = (mask & (1 << i)) != 0 ? STANDING : GONE;
        }
        ballActive = false;
        inGutter = false;
        stillTime = 0;
    }

    public int standingMask() {
        int m = 0;
        for (int i = 0; i < PINS; i++) {
            if (state[i] == STANDING) {
                m |= 1 << i;
            }
        }
        return m;
    }

    public int standingCount() {
        return Integer.bitCount(standingMask());
    }

    /** Lanza la bola desde la línea de falta: v = posición a lo ancho, angle = grados hacia la derecha, speed en bloques/s, spin de -1 a 1 (derecha positivo). */
    public void launch(double v, double angleDeg, double speed, double spinValue) {
        bu = FOUL_U;
        bv = Math.max(-HALF_W + 0.05, Math.min(HALF_W - 0.05, v));
        double a = Math.toRadians(angleDeg);
        bvu = Math.cos(a) * speed;
        bvv = Math.sin(a) * speed;
        spin = Math.max(-1, Math.min(1, spinValue));
        ballActive = true;
        inGutter = false;
        stillTime = 0;
        ballHits = 0;
        pinHits = 0;
    }

    /** Avanza dt segundos (se llama en pasos pequeños). */
    public void step(double dt) {
        if (ballActive) {
            double speed = Math.hypot(bvu, bvv);
            if (!inGutter) {
                double ramp = Math.max(0.15, Math.min(1.0, (bu - FOUL_U) / 6.0));
                bvv += spin * HOOK * ramp * dt;
            }
            if (speed > 0) {
                double ns = Math.max(0, speed - 0.25 * dt);
                bvu *= ns / speed;
                bvv *= ns / speed;
            }
            bu += bvu * dt;
            bv += bvv * dt;
            if (!inGutter && bu > FOUL_U && Math.abs(bv) > HALF_W) {
                inGutter = true;
                bvv = 0;
            }
            if (inGutter) {
                bv = bv < 0 ? -GUTTER_V : GUTTER_V;
                bvv = 0;
            }
            if (bu > PIT_U || bu < 0 || Math.hypot(bvu, bvv) < 0.08) {
                ballActive = false;
            }
        }
        for (int i = 0; i < PINS; i++) {
            if (state[i] != DOWN) {
                continue;
            }
            double s = Math.hypot(pvu[i], pvv[i]);
            if (s > 0) {
                double ns = Math.max(0, s - 5.0 * dt);
                pvu[i] *= ns / s;
                pvv[i] *= ns / s;
            }
            pu[i] += pvu[i] * dt;
            pv[i] += pvv[i] * dt;
            if (pu[i] > DECK_END_U || pu[i] < HEAD_U - 1.5 || Math.abs(pv[i]) > HALF_W + 0.08) {
                state[i] = GONE;
            }
        }
        if (ballActive && !inGutter) {
            for (int i = 0; i < PINS; i++) {
                if (state[i] != GONE) {
                    ballVsPin(i);
                }
            }
        }
        for (int i = 0; i < PINS; i++) {
            if (state[i] != DOWN) {
                continue;
            }
            for (int j = i + 1; j < PINS; j++) {
                if (state[j] != GONE) {
                    pinVsPin(i, j);
                }
            }
        }
        // en reposo cuando nada se mueve
        stillTime = isMoving() ? 0 : stillTime + dt;
    }

    private void ballVsPin(int i) {
        double r = BALL_R + (state[i] == DOWN ? DOWN_R : PIN_R);
        double du = pu[i] - bu;
        double dv = pv[i] - bv;
        double d = Math.hypot(du, dv);
        if (d >= r || d < 1e-9) {
            return;
        }
        double nu = du / d;
        double nv = dv / d;
        double rel = (pvu[i] - bvu) * nu + (pvv[i] - bvv) * nv; // < 0: se acercan
        if (state[i] == STANDING) {
            double impact = -rel;
            if (impact <= 0) {
                return;
            }
            ballHits++;
            if (impact > KNOCK) {
                state[i] = DOWN;
                pinHits++;
            } else {
                // golpe flojo: la bola rebota en el pino, que no se cae
                double j = (1 + 0.3) * impact;
                bvu -= j * nu;
                bvv -= j * nv;
                bu -= nu * (r - d);
                bv -= nv * (r - d);
                return;
            }
        }
        if (rel < 0) {
            double j = -(1 + 0.5) * rel / (1 / BALL_MASS + 1 / PIN_MASS);
            bvu -= j / BALL_MASS * nu;
            bvv -= j / BALL_MASS * nv;
            pvu[i] += j / PIN_MASS * nu;
            pvv[i] += j / PIN_MASS * nv;
        }
        double push = r - d;
        double tot = 1 / BALL_MASS + 1 / PIN_MASS;
        bu -= nu * push * (1 / BALL_MASS) / tot;
        bv -= nv * push * (1 / BALL_MASS) / tot;
        pu[i] += nu * push * (1 / PIN_MASS) / tot;
        pv[i] += nv * push * (1 / PIN_MASS) / tot;
    }

    /** Un pino caído (i) contra otro (j) de pie o caído. */
    private void pinVsPin(int i, int j) {
        double ri = DOWN_R;
        double rj = state[j] == DOWN ? DOWN_R : PIN_R;
        double du = pu[j] - pu[i];
        double dv = pv[j] - pv[i];
        double d = Math.hypot(du, dv);
        double r = ri + rj;
        if (d >= r || d < 1e-9) {
            return;
        }
        double nu = du / d;
        double nv = dv / d;
        double rel = (pvu[j] - pvu[i]) * nu + (pvv[j] - pvv[i]) * nv;
        if (state[j] == STANDING) {
            double impact = -rel;
            if (impact <= KNOCK) {
                // el pino de pie frena al caído
                if (impact > 0) {
                    pvu[i] += impact * nu * 0.6;
                    pvv[i] += impact * nv * 0.6;
                }
                return;
            }
            state[j] = DOWN;
            pinHits++;
        }
        if (rel < 0) {
            double jmp = -(1 + 0.6) * rel / 2.0;
            pvu[i] -= jmp * nu;
            pvv[i] -= jmp * nv;
            pvu[j] += jmp * nu;
            pvv[j] += jmp * nv;
        }
        double push = (r - d) / 2.0;
        pu[i] -= nu * push;
        pv[i] -= nv * push;
        pu[j] += nu * push;
        pv[j] += nv * push;
    }

    public boolean isMoving() {
        if (ballActive) {
            return true;
        }
        for (int i = 0; i < PINS; i++) {
            if (state[i] == DOWN && Math.hypot(pvu[i], pvv[i]) > 0.03) {
                return true;
            }
        }
        return false;
    }

    /** La jugada terminó: la bola se fue, los pinos pararon y pasó un rato en calma. */
    public boolean settled() {
        return !isMoving() && stillTime > 0.6;
    }
}
