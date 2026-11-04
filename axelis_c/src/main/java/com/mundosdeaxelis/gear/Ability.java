package com.mundosdeaxelis.gear;

/** Habilidad especial de cada arma/herramienta. {@code cooldown} está en ticks (0 = sin habilidad activa). */
public enum Ability {
    DAWN("gear_dawn_blade", 20 * 18, 0xFFE9A0, 15),
    FROST("gear_frost_blade", 20 * 15, 0x9FDBFF, 15),
    STORM("gear_storm_blade", 0, 0x8FC0FF, 0),
    VAMPIRE("gear_vampire_saber", 20 * 30, 0xC01830, 20),
    DRAGON("gear_dragon_sword", 20 * 8, 0xFF7A2A, 20),
    CHEER("gear_cheer_pick", 0, 0xFFD966, 0),
    PROSPECTOR("gear_prospector_pick", 20 * 8, 0xFFE070, 0),
    VEIN("gear_vein_pick", 0, 0xC060FF, 0),
    CORE("gear_core_pick", 0, 0xFF4F8B, 0),
    EXCAVATOR("gear_excavator", 0, 0x4FA8FF, 0),
    LUMBER("gear_lumber_axe", 0, 0x9AD070, 0),
    THUNDER("gear_thunder_axe", 20 * 15, 0xFFE866, 25);

    public final String id;
    public final int cooldown;
    public final int color;
    /** Almas que gasta la habilidad activa (0 = no gasta). */
    public final int souls;

    Ability(String id, int cooldown, int color, int souls) {
        this.id = id;
        this.cooldown = cooldown;
        this.color = color;
        this.souls = souls;
    }

    /** Capacidades de minado que tenían las especiales, expresadas con el sistema nuevo. */
    public static java.util.Map<Perk, Integer> perksOf(Ability a) {
        if (a == null) {
            return java.util.Map.of();
        }
        return switch (a) {
            case CHEER -> Perk.parse("XP2");
            case VEIN -> Perk.parse("VEIN1");
            case CORE -> Perk.parse("AREA1 SMELT HASTE1");
            case EXCAVATOR -> Perk.parse("AREA1 HASTE1");
            case LUMBER, THUNDER -> Perk.parse("FELL2");
            case PROSPECTOR -> Perk.parse("RADAR");
            default -> java.util.Map.of();
        };
    }

    public boolean hasActive() {
        return cooldown > 0;
    }
}
