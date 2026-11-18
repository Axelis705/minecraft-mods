package com.mundosdeaxelis.gear;

/**
 * Habilidades activas de las espadas de la escalera (clic derecho, gastan almas). Suben con el puesto del set: las primeras
 * son sencillas (un brinco, un empujón) y las últimas son las de siempre (oleada, nova, embestida y sobrecarga).
 */
public enum Skill {
    HOP(1, 4, 20 * 5),
    SHOVE(2, 7, 20 * 7),
    SURGE(3, 10, 20 * 12),
    NOVA(4, 15, 20 * 10),
    RUSH(5, 20, 20 * 8),
    OVERDRIVE(6, 40, 20 * 25);

    public final int level;
    public final int souls;
    public final int cooldown;

    Skill(int level, int souls, int cooldown) {
        this.level = level;
        this.souls = souls;
        this.cooldown = cooldown;
    }

    /** Nivel de habilidad de la espada del puesto {@code rank} (1..16): la primera no tiene; de la 2.ª a la 16.ª sube cada pocos puestos. */
    public static int levelForRank(int rank) {
        if (rank < 2) {
            return 0;
        }
        if (rank <= 3) {
            return 1;
        }
        if (rank <= 5) {
            return 2;
        }
        if (rank <= 7) {
            return 3;
        }
        if (rank <= 10) {
            return 4;
        }
        if (rank <= 13) {
            return 5;
        }
        return 6;
    }

    public static Skill byLevel(int lvl) {
        for (Skill s : values()) {
            if (s.level == lvl) {
                return s;
            }
        }
        return null;
    }
}
