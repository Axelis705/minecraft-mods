package com.mundosdeaxelis.item;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Termo de chocolate caliente: se bebe, te cura con calidez y tiene 5 sorbos. */
public class CocoaFlaskItem extends Item {
    public CocoaFlaskItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getHealth() >= player.getMaxHealth() && player.getFoodData().getFoodLevel() >= 20) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.mundos_de_axelis.flask_full"), true);
            }
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp && remaining % 5 == 0) {
            ServerLevel sl = sp.serverLevel();
            sl.sendParticles(Fx.dust(0xC98A5A, 0.7F), sp.getX(), sp.getY() + 1.7, sp.getZ(), 1, 0.15, 0.1, 0.15, 0.0);
            sl.sendParticles(Fx.dust(0xFFFFFF, 0.6F), sp.getX(), sp.getY() + 1.9, sp.getZ(), 0, 0.0, 0.05, 0.0, 1.0);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer sp) {
            ServerLevel sl = sp.serverLevel();
            sp.heal(6.0F);
            sp.getFoodData().eat(5, 0.6F);
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8, 0));
            sp.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 90, 0));
            sp.getCooldowns().addCooldown(this, 20 * 3);
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1.0F, 1.0F);
            Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
            sl.sendParticles(Fx.heart(0xFF8FB0, 1.0F), sp.getX(), sp.getY() + 1.5, sp.getZ(), 7, 0.4, 0.4, 0.4, 0.03);
            Fx.screen(sp, ScreenFxPacket.HEARTS, 0xFF9CB8, 36);
            stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "cocoa_flask", 2);
    }
}
