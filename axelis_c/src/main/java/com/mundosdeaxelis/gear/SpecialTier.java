package com.mundosdeaxelis.gear;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de una herramienta especial: la velocidad de minado y el nivel se eligen según su rareza. */
public final class SpecialTier implements Tier {
    private final float speed;
    private final int level;
    private final int enchant;
    private final float bonus;

    public SpecialTier(float speed, int level, int enchant) {
        this(speed, level, enchant, 0.0F);
    }

    /** {@code bonus}: daño extra del material (se suma al de la herramienta). */
    public SpecialTier(float speed, int level, int enchant, float bonus) {
        this.speed = speed;
        this.level = level;
        this.enchant = enchant;
        this.bonus = bonus;
    }

    @Override
    public int getUses() {
        return 1000;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return bonus;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchant;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }
}
