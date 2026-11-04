package com.mundosdeaxelis.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Asado del Herrero: comida de la dungeon de la Forja. Después de comerlo hay una espera corta antes de poder comer otro,
 * para que no se pueda curar sin parar en mitad de una pelea (el cooldown lo respeta el juego al usar el objeto).
 */
public class ForgeRoastItem extends Item {
    /** Espera tras comer, en ticks (5 segundos). */
    public static final int COOLDOWN = 100;

    public ForgeRoastItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, COOLDOWN);
        }
        return result;
    }
}
