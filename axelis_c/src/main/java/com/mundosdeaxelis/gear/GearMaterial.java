package com.mundosdeaxelis.gear;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de una herramienta de la escalera (durabilidad, velocidad y daño según el set y el escalón). */
public final class GearMaterial implements Tier {
    private final GearDef def;

    public GearMaterial(GearDef def) {
        this.def = def;
    }

    @Override
    public int getUses() {
        return GearStats.uses(def.set().rank, def.ord());
    }

    @Override
    public float getSpeed() {
        return def.miningSpeed();
    }

    @Override
    public float getAttackDamageBonus() {
        return GearStats.materialBonus(def.family(), GearStats.total(def.family(), def.set().rank));
    }

    @Override
    public int getLevel() {
        return def.set().level();
    }

    @Override
    public int getEnchantmentValue() {
        return 14 + 3 * def.ord();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return def.set().repair();
    }
}
