package com.kensei.registry;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** El material de las armas de Kensei: por encima de la netherita en resistencia y daño. */
public enum KenseiTier implements Tier {
    KATANA(4, 3200, 9.0F, 4.0F, 24);

    private final int level;
    private final int uses;
    private final float speed;
    private final float damage;
    private final int enchantability;

    KenseiTier(int level, int uses, float speed, float damage, int enchantability) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantability = enchantability;
    }

    @Override
    public int getUses() {
        return uses;
    }

    @Override
    public float getSpeed() {
        return speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return damage;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getEnchantmentValue() {
        return enchantability;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.of(Items.NETHERITE_INGOT);
    }
}
