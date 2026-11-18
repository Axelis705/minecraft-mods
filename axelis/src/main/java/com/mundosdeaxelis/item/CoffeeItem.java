package com.mundosdeaxelis.item;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.ItemStack;

/** Una taza de café humeante: se bebe y da velocidad un rato. */
public class CoffeeItem extends Item {
    private static final FoodProperties FOOD = new FoodProperties.Builder()
            .nutrition(3).saturationMod(0.4F).alwaysEat()
            .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 45, 0), 1.0F)
            .build();

    public CoffeeItem(Properties properties) {
        super(properties.food(FOOD));
    }

    @Override
    public void appendHoverText(ItemStack stack, @javax.annotation.Nullable net.minecraft.world.level.Level level,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(net.minecraft.network.chat.Component.literal("Uy never")
                .withStyle(net.minecraft.ChatFormatting.GRAY, net.minecraft.ChatFormatting.ITALIC));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }
}
