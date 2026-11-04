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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Pluma estelar: te lanza al cielo y luego te baja flotando entre estrellas. */
public class StarFeatherItem extends Item {
    public StarFeatherItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 look = sp.getLookAngle();
        // Impulso hacia arriba (y un poco hacia donde miras).
        sp.setDeltaMovement(look.x * 0.35, 1.15, look.z * 0.35);
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 9, 0, false, false, true));
        sp.getCooldowns().addCooldown(this, 45);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));

        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.9F, 1.3F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.7F);
        Fx.ring(sl, Fx.spark(0xFFF4B0, 1.0F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 1.0, 14, 0);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xD8E8FF, 16);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xFFF1A8, 60);
        Fx.rainbowRing(sl, sp.getX(), sp.getY() + 0.2, sp.getZ(), 1.4, 24, 0);
        // Estela de estrellas mientras sube y baja.
        for (int i = 1; i <= 36; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                ServerLevel w = sp.serverLevel();
                w.sendParticles(Fx.spark(0xFFF1A8, 1.0F), sp.getX(), sp.getY() + 0.2, sp.getZ(), 2, 0.3, 0.1, 0.3, 0.02);
                w.sendParticles(Fx.dust(Fx.rainbow(k / 18.0F), 1.1F), sp.getX(), sp.getY() + 0.1, sp.getZ(), 4, 0.3, 0.05, 0.3, 0.0);
                if (k % 6 == 0) {
                    Fx.helix(w, Fx.spark(0xBFD8FF, 0.8F), sp.getX(), sp.getY() - 0.4, sp.getZ(), 0.6, 1.2, 6, k * 0.3);
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "star_feather", 2);
    }
}
