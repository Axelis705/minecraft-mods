package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.crate.Tier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Un set de equipo. Un SET es: 4 piezas de armadura + 3 herramientas (hacha, pico, pala) + 1 espada + 1 capa + 3 decoraciones, todo
 * con la misma rareza. Cada caja de colección da dos sets (A y B): los A tienen puesto 1, 3, 5... y los B 2, 4, 6... (de 1 a 16),
 * así cada set es mejor que el anterior.
 */
public enum GearSet {
    // El puesto (rank 1..16) sale del orden de las cajas: Naturaleza A=1 B=2, Forja A=3 B=4... Los ids internos se conservan
    // (forest, forge, ice, magma, magic, ruby, storm, shadow) porque ya existen en los mundos; "ruby" es la caja Dragón.
    FOREST("forest", 0x7BD66B, 1, false, Tier.COMMON, "common"),
    BLOOM("bloom", 0xFF9CC8, 2, false, Tier.COMMON, "common"),
    FORGE("forge", 0xC9D4DE, 3, false, Tier.COMMON, "common"),
    BRONZE("bronze", 0xD9954A, 4, false, Tier.UNCOMMON, "uncommon"),
    ICE("ice", 0x8FDFFF, 5, false, Tier.UNCOMMON, "uncommon"),
    AURORA("aurora", 0x5CF2B0, 6, false, Tier.RARE, "rare"),
    MAGMA("magma", 0xFF7A2A, 7, true, Tier.RARE, "rare"),
    OBSIDIAN("obsidian", 0xFF5A1A, 8, true, Tier.EPIC, "epic"),
    MAGIC("magic", 0xC070FF, 9, false, Tier.EPIC, "epic"),
    MOON("moon", 0xA8D8FF, 10, false, Tier.LEGENDARY, "legendary"),
    RUBY("ruby", 0xFF3A5E, 11, true, Tier.LEGENDARY, "legendary"),
    GOLDEN("golden", 0xF0B830, 12, true, Tier.MYTHIC, "mythic"),
    STORM("storm", 0x7AA8FF, 13, false, Tier.MYTHIC, "legendary"),
    PEGASUS("pegasus", 0xCFE8FF, 14, false, Tier.STELLAR, "stellar"),
    SHADOW("shadow", 0x7A4CC8, 15, true, Tier.STELLAR, "mythic"),
    COSMIC("cosmic", 0xD048D0, 16, true, Tier.CELESTIAL, "celestial");

    public final String id;
    public final int color;
    /** Puesto del set entre los 16 (1 = el más sencillo, 16 = el mejor). */
    public final int rank;
    /** Rareza de todo el set (herramientas, armadura, capa y decoración). */
    public final Tier tier;
    public final boolean fireproof;
    /** Sufijo de los archivos de arte de las herramientas ({@code forest_sword_<sufijo>}); no tiene que coincidir con la rareza. */
    public final String artSuffix;

    GearSet(String id, int color, int rank, boolean fireproof, Tier tier, String artSuffix) {
        this.id = id;
        this.color = color;
        this.rank = rank;
        this.tier = tier;
        this.fireproof = fireproof;
        this.artSuffix = artSuffix;
    }

    /** Poder del set en una escala de 1 a 8,5 (cada caja sube 1; el set B de una caja es medio punto mejor que el A). */
    public double power() {
        return 1.0 + (rank - 1) / 2.0;
    }

    /** Índice (0..7) de la caja que lo da. */
    public int crateIndex() {
        return (rank - 1) / 2;
    }

    /** Nivel de minado (1 piedra, 2 hierro, 3 diamante, 4 netherita). */
    public int level() {
        return new int[]{1, 2, 2, 3, 3, 3, 4, 4}[crateIndex()];
    }

    public Ingredient repair() {
        return switch (this) {
            case FOREST -> Ingredient.of(ItemTags.PLANKS);
            case BLOOM -> Ingredient.of(Items.CHERRY_LEAVES, Items.PINK_PETALS);
            case FORGE -> Ingredient.of(Items.IRON_INGOT);
            case BRONZE -> Ingredient.of(Items.COPPER_INGOT, Items.GOLD_INGOT);
            case ICE -> Ingredient.of(Items.PACKED_ICE, Items.BLUE_ICE);
            case AURORA -> Ingredient.of(Items.BLUE_ICE, Items.AMETHYST_SHARD);
            case MAGMA -> Ingredient.of(Items.MAGMA_BLOCK, Items.BLAZE_ROD);
            case OBSIDIAN -> Ingredient.of(Items.OBSIDIAN, Items.BLAZE_POWDER);
            case MAGIC -> Ingredient.of(Items.LAPIS_LAZULI, Items.AMETHYST_SHARD);
            case MOON -> Ingredient.of(Items.QUARTZ, Items.PRISMARINE_CRYSTALS);
            case RUBY -> Ingredient.of(Items.REDSTONE_BLOCK, Items.DIAMOND);
            case GOLDEN -> Ingredient.of(Items.GOLD_INGOT, Items.EMERALD);
            case STORM -> Ingredient.of(Items.COPPER_INGOT, Items.LIGHTNING_ROD);
            case PEGASUS -> Ingredient.of(Items.PHANTOM_MEMBRANE, Items.GOLD_INGOT);
            case SHADOW -> Ingredient.of(Items.ECHO_SHARD, Items.OBSIDIAN);
            case COSMIC -> Ingredient.of(Items.ECHO_SHARD, Items.NETHERITE_INGOT);
        };
    }

    public static GearSet byId(String id) {
        for (GearSet s : values()) {
            if (s.id.equals(id)) {
                return s;
            }
        }
        return FOREST;
    }
}
