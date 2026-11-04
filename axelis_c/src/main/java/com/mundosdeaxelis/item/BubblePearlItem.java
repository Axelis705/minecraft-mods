package com.mundosdeaxelis.item;

import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Tips;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Perla burbuja: te envuelve en una burbuja que deja respirar bajo el agua y flotar con calma. */
public class BubblePearlItem extends Item {
    public BubblePearlItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 20 * 90, 0, false, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 20 * 45, 0, false, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 20, 0, false, false, true));
        sp.getCooldowns().addCooldown(this, 40);
        if (!sp.getAbilities().instabuild) {
            stack.shrink(1);
        }
        Fx.screen(sp, ScreenFxPacket.BUBBLES, 0x8FE3FF, 110);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.9F);

        // La burbuja: esferas de partículas que se forman y se pop-ean al final.
        for (int i = 0; i < 40; i++) {
            final int k = i;
            Scheduler.runLater(i + 1, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                ServerLevel w = sp.serverLevel();
                double cy = sp.getY() + 0.9;
                double r = Math.min(1.2, 0.3 + k * 0.06);
                for (int j = 0; j < 7; j++) {
                    double a = w.getRandom().nextDouble() * Math.PI * 2.0;
                    double b = (w.getRandom().nextDouble() - 0.5) * Math.PI;
                    w.sendParticles(Fx.bubble(0xBFEFFF, 0.8F + w.getRandom().nextFloat() * 0.6F), sp.getX() + Math.cos(a) * Math.cos(b) * r,
                            cy + Math.sin(b) * r * 1.3, sp.getZ() + Math.sin(a) * Math.cos(b) * r, 1, 0, 0, 0, 0);
                }
                if (k % 5 == 0) {
                    Fx.ring(w, Fx.dust(0x8FE3FF, 0.8F), sp.getX(), cy + Math.sin(k * 0.3) * 0.6, sp.getZ(), r, 14, k * 0.4);
                }
            });
        }
        Scheduler.runLater(42, () -> {
            if (sp.isAlive() && !sp.hasDisconnected()) {
                ServerLevel w = sp.serverLevel();
                w.sendParticles(Fx.bubble(0xBFEFFF, 1.2F), sp.getX(), sp.getY() + 0.9, sp.getZ(), 30, 0.8, 0.9, 0.8, 0.03);
                w.sendParticles(ParticleTypes.BUBBLE_POP, sp.getX(), sp.getY() + 0.9, sp.getZ(), 30, 0.8, 0.9, 0.8, 0.1);
                w.sendParticles(ParticleTypes.SPLASH, sp.getX(), sp.getY() + 0.9, sp.getZ(), 30, 0.6, 0.8, 0.6, 0.1);
                w.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        });
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "bubble_pearl", 2);
    }
}
