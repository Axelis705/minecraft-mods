package com.autoscoleccion.game;

/**
 * Cómo se maneja cada auto a control remoto: velocidad máxima (bloques por tick), aceleración, giro y agarre. Los más raros y los
 * deportivos corren más; las camionetas giran más lento pero agarran mejor.
 */
public final class CarStats {
    public final float top;
    public final float accel;
    public final float turn;
    public final float grip;

    private CarStats(float top, float accel, float turn, float grip) {
        this.top = top;
        this.accel = accel;
        this.turn = turn;
        this.grip = grip;
    }

    public static CarStats of(CarDef c) {
        CarStyle s = c.style;
        float top = 0.36F + 0.075F * c.rarity.stars;
        float accel = 0.016F + 0.004F * c.rarity.stars;
        float turn = 22.0F;
        float grip = 0.38F;
        switch (s.shape) {
            case PICKUP, SUV, BOXVAN, VAN -> {
                top *= 0.85F;
                turn = 17.0F;
                grip = 0.45F;
            }
            case HYPER, PROTO, WEDGE, FORMULA -> {
                top *= 1.12F;
                accel *= 1.25F;
                grip = 0.42F;
            }
            case COUPE, RALLY, HATCH -> {
                turn = 25.0F;
                grip = 0.33F;
            }
            case MUSCLE -> {
                accel *= 1.15F;
                grip = 0.3F;
            }
            default -> {
            }
        }
        return new CarStats(top, accel, turn, grip);
    }
}
