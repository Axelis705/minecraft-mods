package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Paleta arcoíris: golosina rápida que da energía. */
public class LollipopItem extends Item {
    public LollipopItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 25, 1));
            sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 25, 0));
            Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 24);
            Fx.rainbowRing(sl, sp.getX(), sp.getY() + 0.2, sp.getZ(), 1.0, 20, 0);
            Fx.confetti(sl, sp.getX(), sp.getY() + 1.2, sp.getZ(), 14, 1.0);
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "rainbow_lollipop", 2);
    }
}
