package com.mundosdeaxelis.table;

import com.mundosdeaxelis.crate.Tier;

import java.util.LinkedHashMap;
import java.util.Map;

/** Reglas de los ocho niveles personales de la Mesa del Artífice (los costos se ajustan al probar). Subir al nivel N pide el material del jefe de la etapa N-1. */
public final class TableLevels {
    private TableLevels() {
    }

    public static final int MAX = 8;
    /** Cuánto cuesta cada reliquia o mascota por el aumento de nivel, según la rareza (se multiplica por el nivel actual). */
    private static final int[] LEVEL_BASE = {6, 10, 16, 26, 40, 60, 90, 130};
    /** Esencia que da desmontar un objeto, según la rareza. */
    private static final int[] DISMANTLE = {4, 8, 16, 32, 64, 128, 220, 360};
    /** Esencia necesaria para subir la mesa AL nivel indicado (índice). */
    private static final int[] ESSENCE = {0, 0, 150, 450, 1100, 2200, 3600, 5500, 8000};

    public static String nameKey(int level) {
        return "table.mundos_de_axelis.level." + Math.max(1, Math.min(MAX, level));
    }

    public static int upgradeEssence(int toLevel) {
        return toLevel >= 0 && toLevel < ESSENCE.length ? ESSENCE[toLevel] : 0;
    }

    /** Materiales que pide subir la mesa AL nivel indicado (siempre incluye el material del jefe de la etapa anterior). */
    public static Map<Mats, Integer> upgradeMats(int toLevel) {
        Map<Mats, Integer> m = new LinkedHashMap<>();
        switch (toLevel) {
            case 2 -> {
                m.put(Mats.TORN_CLOTH, 6);
                m.put(Mats.RUNIC_BONE, 6);
                m.put(Mats.WOVEN_SILK, 6);
                m.put(Mats.SHINY_JELLY, 6);
            }
            case 3 -> {
                m.put(Mats.EXPLOSIVE_HEART, 4);
                m.put(Mats.PURPLE_ESSENCE, 4);
                m.put(Mats.ELIXIR_DROP, 4);
                m.put(Mats.LIVE_EMBER, 4);
                m.put(Mats.OLD_COIN, 4);
                m.put(Mats.PEARL_KELP, 4);
            }
            case 4 -> {
                m.put(Mats.DARK_ASH, 6);
                m.put(Mats.LAMENT_ECHO, 6);
                m.put(Mats.SHELL_SHARD, 6);
                m.put(Mats.SEA_SCALE, 6);
                m.put(Mats.ABYSS_CORE, 1);
            }
            case 5 -> {
                m.put(Mats.DARK_ASH, 10);
                m.put(Mats.SEA_SCALE, 10);
                m.put(Mats.BOSS_HEART, 1);
            }
            case 6 -> {
                m.put(Mats.LAMENT_ECHO, 10);
                m.put(Mats.SHELL_SHARD, 10);
                m.put(Mats.BOSS_HEART, 1);
            }
            case 7 -> {
                m.put(Mats.DARK_ASH, 8);
                m.put(Mats.LAMENT_ECHO, 8);
                m.put(Mats.ABYSS_CORE, 1);
                m.put(Mats.BOSS_HEART, 2);
            }
            case 8 -> {
                m.put(Mats.SEA_SCALE, 12);
                m.put(Mats.SHELL_SHARD, 12);
                m.put(Mats.ABYSS_CORE, 2);
                m.put(Mats.BOSS_HEART, 3);
            }
            default -> {
            }
        }
        Mats boss = Mats.ofBoss(toLevel - 1);
        if (boss != null) {
            m.put(boss, 1);
        }
        return m;
    }

    /** Descuento en la llave de la caja {@code k}: la Mesa de nivel N+1 abarata la llave de la caja N (y las anteriores). */
    public static float keyDiscount(int tableLevel, com.mundosdeaxelis.crate.CrateKind k) {
        return k.rank() >= 1 && tableLevel >= k.rank() + 1 ? 0.25F : 0.0F;
    }

    /** Lo que cuesta la llave en Esencia con el nivel de mesa con el que se trabaja. */
    public static int keyPrice(com.mundosdeaxelis.crate.CrateKind k, int tableLevel) {
        return Math.max(1, Math.round(k.keyCost() * (1.0F - keyDiscount(tableLevel, k))));
    }

    /** Descuento en el costo de subir de nivel un objeto (niveles de mesa 5 a 8). */
    public static float levelDiscount(int tableLevel) {
        return switch (tableLevel) {
            case 5 -> 0.10F;
            case 6 -> 0.15F;
            case 7 -> 0.20F;
            case 8 -> 0.30F;
            default -> 0.0F;
        };
    }

    /** Esencia extra al desmontar (niveles de mesa 6 a 8). */
    public static float dismantleBonus(int tableLevel) {
        return switch (tableLevel) {
            case 6 -> 0.25F;
            case 7 -> 0.40F;
            case 8 -> 0.60F;
            default -> 0.0F;
        };
    }

    /** Ranuras de mascotas (3 a 5) y de reliquias (6 a 8) que da el nivel personal. */
    public static int petSlots(int level) {
        return 3 + (level >= 2 ? 1 : 0) + (level >= 3 ? 1 : 0);
    }

    public static int relicSlots(int level) {
        return 6 + (level >= 2 ? 1 : 0) + (level >= 3 ? 1 : 0);
    }

    /** Hasta qué nivel (1 a 5) se puede subir un objeto en la mesa. */
    public static int maxItemLevel(int tableLevel) {
        return tableLevel <= 1 ? 3 : 5;
    }

    /** Rareza más alta a la que se puede ascender (se usa desde la etapa de ascensos). */
    public static Tier maxTier(int tableLevel) {
        return switch (Math.max(1, Math.min(MAX, tableLevel))) {
            case 1 -> Tier.RARE;
            case 2 -> Tier.EPIC;
            case 3 -> Tier.LEGENDARY;
            case 4, 5 -> Tier.MYTHIC;
            case 6, 7 -> Tier.STELLAR;
            default -> Tier.CELESTIAL;
        };
    }

    /** Límite de objetos vinculados (se usa desde la etapa de la lápida). */
    public static int boundLimit(int level) {
        return 2 + 2 * Math.max(1, Math.min(MAX, level));
    }

    /** Esencia para pasar un objeto del nivel actual al siguiente. */
    public static int levelCost(Tier tier, int currentLevel) {
        return LEVEL_BASE[tier.ordinal()] * Math.max(1, currentLevel);
    }

    /** Igual, con el descuento del nivel de mesa con el que se trabaja. */
    public static int levelCost(Tier tier, int currentLevel, int tableLevel) {
        return Math.max(1, Math.round(levelCost(tier, currentLevel) * (1.0F - levelDiscount(tableLevel))));
    }

    /** Esencia que da desmontar (más la mitad de lo gastado en subirlo de nivel). */
    public static int dismantleValue(Tier tier, int itemLevel) {
        int invested = 0;
        for (int l = 1; l < itemLevel; l++) {
            invested += levelCost(tier, l);
        }
        return DISMANTLE[tier.ordinal()] + invested / 2;
    }

    /** Igual, con el bono del nivel de mesa con el que se trabaja. */
    public static int dismantleValue(Tier tier, int itemLevel, int tableLevel) {
        return Math.round(dismantleValue(tier, itemLevel) * (1.0F + dismantleBonus(tableLevel)));
    }
}
