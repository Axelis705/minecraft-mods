package com.mundosdeaxelis.item;

import com.mundosdeaxelis.block.BoxTier;
import com.mundosdeaxelis.block.MysteryLoot;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Dado de la suerte: lo lanzas y el número que salga decide tu pequeña fortuna. */
public class LuckyDieItem extends Item {
    public LuckyDieItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 20 * 6);
        final int result = 1 + sl.getRandom().nextInt(6);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xFFFFFF, 30);
        // El dado rueda: números que cambian y tintineo que sube.
        for (int i = 0; i < 12; i++) {
            final int k = i;
            Scheduler.runLater(i * 2 + 1, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                int shown = 1 + sl.getRandom().nextInt(6);
                sp.displayClientMessage(Component.literal("⚄  " + shown + "  ⚀").withStyle(ChatFormatting.WHITE), true);
                sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.NOTE_BLOCK_HAT.get(), SoundSource.PLAYERS,
                        0.7F, 0.8F + k * 0.07F);
                sl.sendParticles(Fx.spark(0xFFFFFF, 0.7F), sp.getX(), sp.getY() + 2.0 + (k % 3) * 0.1, sp.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
            });
        }
        Scheduler.runLater(27, () -> {
            if (sp.isAlive() && !sp.hasDisconnected()) {
                reveal(sl, sp, result);
            }
        });
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void reveal(ServerLevel sl, ServerPlayer sp, int n) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        String key = "message.mundos_de_axelis.die_" + n;
        sp.displayClientMessage(Component.translatable(key, n).withStyle(n == 1 ? ChatFormatting.RED : n == 6 ? ChatFormatting.GOLD : ChatFormatting.YELLOW), true);
        switch (n) {
            case 1 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 6, 1));
                sl.sendParticles(Fx.dust(0x777788, 1.2F), x, y + 1.0, z, 14, 0.5, 0.6, 0.5, 0.0);
                sl.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BASS.get(), SoundSource.PLAYERS, 1.0F, 0.5F);
            }
            case 2 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 20 * 40, 2));
                Fx.ring(sl, Fx.spark(0x9CFFB0, 1.0F), x, y + 0.2, z, 1.0, 14, 0);
                sl.playSound(null, x, y, z, SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 1.0F, 1.5F);
            }
            case 3 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 20, 1));
                sl.sendParticles(Fx.heart(0xFF6FA8, 1.0F), x, y + 1.2, z, 8, 0.5, 0.5, 0.5, 0.02);
                sl.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.3F);
            }
            case 4 -> {
                sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 40, 2));
                Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 20);
                sl.playSound(null, x, y, z, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.5F);
            }
            case 5 -> {
                sp.giveExperienceLevels(4);
                sl.sendParticles(Fx.spark(0x9CFF6A, 1.2F), x, y + 1.0, z, 14, 0.6, 0.7, 0.6, 0.0);
                sl.playSound(null, x, y, z, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            default -> {
                // ¡Premio gordo!
                BoxTier t = sl.getRandom().nextInt(3) == 0 ? BoxTier.EPIC : BoxTier.RARE;
                ItemEntity e = new ItemEntity(sl, x, y + 1.4, z, MysteryLoot.roll(sl.getRandom(), t));
                e.setDeltaMovement(0.0, 0.3, 0.0);
                sl.addFreshEntity(e);
                Fx.confetti(sl, x, y + 1.0, z, 40, 2.0);
                Fx.rainbowRing(sl, x, y + 0.2, z, 1.2, 26, 0);
                Fx.screen(sp, ScreenFxPacket.FLASH, 0xFFE9A0, 24);
                Fx.screen(sp, ScreenFxPacket.CONFETTI, 0xFFFFFF, 70);
                sl.playSound(null, x, y, z, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.1F);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "lucky_die", 2);
    }
}
