package com.mundosdeaxelis.table;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Los 17 materiales que sueltan los mobs. Cada uno se refina en la Mesa del Artífice y da Esencia.
 * {@code chance} es la probabilidad base de soltarlo (por cada mob), y cada nivel de Botín suma 5 %.
 */
public enum Mats {
    TORN_CLOTH("torn_cloth", 2, 40, 0x86A078, Rarity.COMMON, EntityType.ZOMBIE, EntityType.HUSK),
    RUNIC_BONE("runic_bone", 2, 40, 0xE8E2CC, Rarity.COMMON, EntityType.SKELETON, EntityType.STRAY),
    WOVEN_SILK("woven_silk", 2, 40, 0xE8ECF2, Rarity.COMMON, EntityType.SPIDER, EntityType.CAVE_SPIDER),
    EXPLOSIVE_HEART("explosive_heart", 4, 30, 0x5FC84E, Rarity.UNCOMMON, EntityType.CREEPER),
    PURPLE_ESSENCE("purple_essence", 6, 25, 0xA84FFF, Rarity.UNCOMMON, EntityType.ENDERMAN),
    ELIXIR_DROP("elixir_drop", 5, 30, 0xE84FB8, Rarity.UNCOMMON, EntityType.WITCH),
    SHINY_JELLY("shiny_jelly", 3, 35, 0x6FE85F, Rarity.COMMON, EntityType.SLIME),
    LIVE_EMBER("live_ember", 6, 30, 0xFF7A20, Rarity.UNCOMMON, EntityType.BLAZE),
    OLD_COIN("old_coin", 4, 30, 0xE8B830, Rarity.UNCOMMON, EntityType.PIGLIN, EntityType.PIGLIN_BRUTE),
    LUNAR_VEIL("lunar_veil", 5, 30, 0xB8D0F0, Rarity.UNCOMMON, EntityType.PHANTOM),
    PEARL_KELP("pearl_kelp", 3, 35, 0x3C9A50, Rarity.COMMON, EntityType.DROWNED),
    SEA_SCALE("sea_scale", 6, 30, 0x3FC8C0, Rarity.RARE, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN),
    DARK_ASH("dark_ash", 10, 20, 0x4A4A56, Rarity.RARE, EntityType.WITHER_SKELETON),
    LAMENT_ECHO("lament_echo", 10, 25, 0xB8D0F8, Rarity.RARE, EntityType.GHAST),
    SHELL_SHARD("shell_shard", 12, 20, 0xB06AE0, Rarity.RARE, EntityType.SHULKER),
    ABYSS_CORE("abyss_core", 150, 100, 0x30E0FF, Rarity.EPIC, EntityType.WARDEN),
    BOSS_HEART("boss_heart", 300, 100, 0xE03040, Rarity.EPIC, EntityType.ENDER_DRAGON, EntityType.WITHER),
    // ---- materiales de jefe de dungeon (etapa 1 a 7): se dan por pelear contra el jefe y sirven para subir la Mesa (nivel = etapa + 1). No se refinan.
    SAP_HEART("sap_heart", 1, 0x7BD66B, Rarity.RARE),
    FORGE_CORE("forge_core", 2, 0xFF8A2B, Rarity.RARE),
    FROST_CRYSTAL("frost_crystal", 3, 0x8FDFFF, Rarity.EPIC),
    MAGMA_CORE("magma_core", 4, 0xFF4A1A, Rarity.EPIC),
    ARCANE_SHARD("arcane_shard", 5, 0xC070FF, Rarity.EPIC),
    LIVING_RUBY("living_ruby", 6, 0xFF3A5E, Rarity.EPIC),
    STORM_FEATHER("storm_feather", 7, 0x7AB8FF, Rarity.EPIC);

    public final String id;
    /** Esencia que da al refinarlo. */
    public final int essence;
    public final int chance;
    public final int color;
    public final Rarity rarity;
    /** Etapa del jefe que da este material (1 a 7), o 0 si sale de mobs. */
    public final int bossStage;
    private final EntityType<?>[] sources;

    Mats(String id, int essence, int chance, int color, Rarity rarity, EntityType<?>... sources) {
        this.id = id;
        this.essence = essence;
        this.chance = chance;
        this.color = color;
        this.rarity = rarity;
        this.bossStage = 0;
        this.sources = sources;
    }

    /** Material de jefe: no cae de mobs ni da Esencia. */
    Mats(String id, int stage, int color, Rarity rarity) {
        this.id = id;
        this.essence = 0;
        this.chance = 0;
        this.color = color;
        this.rarity = rarity;
        this.bossStage = stage;
        this.sources = new EntityType<?>[0];
    }

    public boolean isBoss() {
        return bossStage > 0;
    }

    /** Material de jefe de esa etapa (1 a 7), o null si no hay. */
    public static Mats ofBoss(int stage) {
        for (Mats m : values()) {
            if (m.bossStage == stage) {
                return m;
            }
        }
        return null;
    }

    public String itemId() {
        return "mat_" + id;
    }

    public boolean dropsFrom(EntityType<?> t) {
        for (EntityType<?> s : sources) {
            if (s == t) {
                return true;
            }
        }
        return false;
    }

    /** Todos los materiales que suelta ese tipo de mob. */
    public static List<Mats> from(EntityType<?> t) {
        List<Mats> out = new ArrayList<>(1);
        for (Mats m : values()) {
            if (m.dropsFrom(t)) {
                out.add(m);
            }
        }
        return out;
    }
}
