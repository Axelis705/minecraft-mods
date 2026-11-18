package com.mundosdeaxelis.gear;

/**
 * Estadísticas base de las armas y herramientas de los 16 sets (puesto 1 = nivel hierro, 16 = el mejor). Aquí se cambia el reparto.
 * Espada: A→B +2 y de una caja a la siguiente +4 (6 → 50). Velocidad de minado: A→B +1 y a la siguiente caja +2 (6 → 28).
 * Durabilidad: 250 (hierro) y ×1,15 por puesto, más un 10 % por cada escalón de rareza.
 */
public final class GearStats {
    private GearStats() {
    }

    /** Daño total de la espada (lo que se ve en la descripción) por puesto 1..16. */
    private static final int[] SWORD = {6, 8, 12, 14, 18, 20, 24, 26, 30, 32, 36, 38, 42, 44, 48, 50};
    /** Velocidad de minado por puesto 1..16 (hierro = 6). */
    private static final float[] SPEED = {6, 7, 9, 10, 12, 13, 15, 16, 18, 19, 21, 22, 24, 25, 27, 28};

    private static int idx(int rank) {
        return Math.max(0, Math.min(15, rank - 1));
    }

    public static int sword(int rank) {
        return SWORD[idx(rank)];
    }

    public static float speed(int rank) {
        return SPEED[idx(rank)];
    }

    /** Usos (durabilidad) de una pieza del set de puesto {@code rank} y escalón de rareza {@code ord} (0 Común ... 7 Celestial). */
    public static int uses(int rank, int ord) {
        return Math.round(250.0F * (float) Math.pow(1.15, rank - 1) * (1.0F + 0.10F * ord));
    }

    /**
     * Daño total de cada herramienta respecto a la espada del mismo puesto, con las mismas diferencias que en el hierro de Minecraft:
     * hacha +3, pico −2, pala −2 (la espada de hierro hace 6, el hacha 9, el pico y la pala 4).
     */
    public static float total(Family fam, int rank) {
        int s = sword(rank);
        return switch (fam) {
            case SWORD -> s;
            case AXE -> s + 3;
            case PICK, SHOVEL -> s - 2;
        };
    }

    /** Daño que debe aportar el material para que el total sea {@code total} (la base ya incluye 1 de la mano y el daño propio de la clase). */
    public static float materialBonus(Family fam, float total) {
        float own = switch (fam) {
            case SWORD -> 3.0F;
            case AXE -> 4.0F;
            case PICK -> 1.0F;
            case SHOVEL -> 2.0F;
        };
        return total - 1.0F - own;
    }

    /**
     * Armas y herramientas especiales: se calculan a partir del set B de su caja ({@code rank} = 2, 4... 16) y salen un poco por encima
     * ({@code extra} de daño total; velocidad +1; durabilidad +25 %).
     */
    public static float specialBonus(Family fam, float baseParam, int rank, float extra) {
        return total(fam, rank) + extra - 1.0F - baseParam;
    }

    public static float specialSpeed(int rank) {
        return speed(rank) + 1.0F;
    }

    public static int specialUses(int rank, int ord) {
        return Math.round(uses(rank, ord) * 1.25F);
    }
}
