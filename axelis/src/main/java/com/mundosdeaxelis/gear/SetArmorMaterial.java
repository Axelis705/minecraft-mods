package com.mundosdeaxelis.gear;

import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

/** Material de la armadura de un set (la textura sale de su nombre: textures/models/armor/{set}_layer_1.png y _layer_2.png). */
public final class SetArmorMaterial implements ArmorMaterial {
    private final GearSet set;

    public SetArmorMaterial(GearSet set) {
        this.set = set;
    }

    private static ArmorDefs.Piece piece(ArmorItem.Type t) {
        for (ArmorDefs.Piece p : ArmorDefs.Piece.values()) {
            if (p.type == t) {
                return p;
            }
        }
        return ArmorDefs.Piece.CHESTPLATE;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return ArmorDefs.durability(set, piece(type));
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return ArmorDefs.defense(set, piece(type));
    }

    @Override
    public int getEnchantmentValue() {
        return ArmorDefs.enchantability(set);
    }

    @Override
    public SoundEvent getEquipSound() {
        return switch (set) {
            case FOREST, BLOOM -> SoundEvents.ARMOR_EQUIP_LEATHER;
            case FORGE, BRONZE, MAGMA -> SoundEvents.ARMOR_EQUIP_IRON;
            case ICE, AURORA, MAGIC, MOON -> SoundEvents.ARMOR_EQUIP_CHAIN;
            case RUBY, GOLDEN, STORM, PEGASUS, OBSIDIAN -> SoundEvents.ARMOR_EQUIP_DIAMOND;
            case SHADOW, COSMIC -> SoundEvents.ARMOR_EQUIP_NETHERITE;
        };
    }

    @Override
    public Ingredient getRepairIngredient() {
        return set.repair();
    }

    @Override
    public String getName() {
        return MundosDeAxelis.MODID + ":" + set.id;
    }

    @Override
    public float getToughness() {
        return ArmorDefs.toughness(set);
    }

    @Override
    public float getKnockbackResistance() {
        return ArmorDefs.knockback(set);
    }
}
