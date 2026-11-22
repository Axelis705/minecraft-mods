package com.mundosdeaxelis.arma;

import com.mundosdeaxelis.gear.GearStats;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de un arma nueva: durabilidad, daño, encantabilidad y reparación según su tier. */
public final class ArmaMaterial implements Tier {
    private final TipoArma tipo;
    private final TierArma tier;

    public ArmaMaterial(TipoArma tipo, TierArma tier) {
        this.tipo = tipo;
        this.tier = tier;
    }

    /** Daño total del golpe (lo que se ve en la descripción). */
    public static float total(TipoArma tipo, TierArma tier) {
        return Math.round(tier.danoBase() * tipo.mult * 2.0F) / 2.0F;
    }

    @Override
    public int getUses() {
        float k = switch (tipo) {
            case MARTILLO, HACHA -> 1.15F;
            case DAGAS -> 0.9F;
            default -> 1.0F;
        };
        return Math.round(GearStats.uses(tier.puesto(), tier.rareza().ordinal()) * k);
    }

    @Override
    public float getSpeed() {
        return 1.5F;
    }

    @Override
    public float getAttackDamageBonus() {
        // la espada de Minecraft suma 1 de la mano; el arma se construye con 0 de daño propio
        return total(tipo, tier) - 1.0F;
    }

    @Override
    public int getLevel() {
        return tier.setB().level();
    }

    @Override
    public int getEnchantmentValue() {
        return 14 + 3 * tier.rareza().ordinal();
    }

    @Override
    public Ingredient getRepairIngredient() {
        return tier.setB().repair();
    }
}
