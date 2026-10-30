package com.casino.game;

/**
 * Las 20 casillas de la Ruleta de la suerte, igual que la de GTA: 20 casillas y todas con la misma probabilidad (5% cada una).
 * Sin nada de Minecraft, para poder probarla sola. La casilla 0 está arriba y se cuenta en el sentido de las agujas del reloj.
 */
public final class WheelLayout {
    public enum Kind {
        PODIUM, MYSTERY, CHIPS, EMERALD, XP, GOLD, DIAMOND, FOOD, APPLE
    }

    public record Slice(Kind kind, int amount) {
    }

    public static final int COUNT = 20;
    public static final double SLICE_DEG = 360.0 / COUNT;

    /** Colores intercalados para que dos casillas del mismo tipo nunca queden juntas. */
    public static final Slice[] SLICES = {
            new Slice(Kind.PODIUM, 1),
            new Slice(Kind.CHIPS, 100),
            new Slice(Kind.XP, 5),
            new Slice(Kind.EMERALD, 16),
            new Slice(Kind.MYSTERY, 1),
            new Slice(Kind.XP, 10),
            new Slice(Kind.CHIPS, 50),
            new Slice(Kind.GOLD, 6),
            new Slice(Kind.XP, 3),
            new Slice(Kind.EMERALD, 8),
            new Slice(Kind.CHIPS, 250),
            new Slice(Kind.FOOD, 12),
            new Slice(Kind.XP, 15),
            new Slice(Kind.DIAMOND, 3),
            new Slice(Kind.CHIPS, 150),
            new Slice(Kind.EMERALD, 24),
            new Slice(Kind.GOLD, 12),
            new Slice(Kind.XP, 8),
            new Slice(Kind.CHIPS, 500),
            new Slice(Kind.APPLE, 2)
    };

    private WheelLayout() {
    }

    /** Tira la ruleta: todas las casillas tienen la misma probabilidad. */
    public static int roll(java.util.Random rnd) {
        return rnd.nextInt(COUNT);
    }

    /**
     * Ángulo (en grados, sentido de las agujas del reloj, desde 0 = reposo) al que hay que dejar la ruleta para que {@code slice}
     * quede bajo el puntero (que está arriba). {@code offset} (-0,35..0,35) mueve el punto de parada dentro de la casilla.
     */
    public static double restAngleFor(int slice, double offset) {
        double centre = slice * SLICE_DEG + SLICE_DEG / 2 + offset * SLICE_DEG;
        return ((360 - centre) % 360 + 360) % 360;
    }

    /** Qué casilla queda bajo el puntero cuando la ruleta ha girado {@code angle} grados. */
    public static int sliceAt(double angle) {
        double a = ((-angle) % 360 + 360) % 360;
        return (int) Math.floor(a / SLICE_DEG) % COUNT;
    }

    /** Premios grandes: más fuegos artificiales y fanfarria más larga. */
    public static boolean isBig(int slice) {
        Slice s = SLICES[slice];
        return switch (s.kind()) {
            case PODIUM, MYSTERY, DIAMOND -> true;
            case CHIPS -> s.amount() >= 250;
            case EMERALD -> s.amount() >= 24;
            default -> false;
        };
    }
}
