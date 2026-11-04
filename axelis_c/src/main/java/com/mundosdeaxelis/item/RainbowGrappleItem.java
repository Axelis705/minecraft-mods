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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Gancho arcoíris: lanza un hilo de colores al bloque que miras y te jala hasta allá. */
public class RainbowGrappleItem extends Item {
    private static final double RANGE = 36.0;

    public RainbowGrappleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        Vec3 eye = sp.getEyePosition();
        Vec3 end = eye.add(sp.getLookAngle().scale(RANGE));
        BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        if (hit.getType() != HitResult.Type.BLOCK) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.grapple_miss"), true);
            sl.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.PLAYERS, 0.8F, 0.8F);
            sp.getCooldowns().addCooldown(this, 10);
            return InteractionResultHolder.fail(stack);
        }
        // Un poco antes del bloque, del lado del jugador, para no quedar dentro de él.
        Vec3 target = hit.getLocation().add(hit.getDirection().getStepX() * 0.6, hit.getDirection().getStepY() * 0.6 - 0.2,
                hit.getDirection().getStepZ() * 0.6);
        sp.getCooldowns().addCooldown(this, 50);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.CROSSBOW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
        Fx.rainbowLine(sl, eye.add(0, -0.3, 0), hit.getLocation(), 0.35, 0.0F);
        Fx.confetti(sl, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 14, 1.0);
        sl.sendParticles(Fx.spark(0xFFFFFF, 1.6F), hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 3, 0.1, 0.1, 0.1, 0.0);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 26);
        sp.fallDistance = 0.0F;

        // Durante unos ticks te acerca al punto; el hilo se vuelve a dibujar cada poco.
        pull(sp, target, 0, hit.getLocation());
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    private static void pull(ServerPlayer sp, Vec3 target, int tick, Vec3 anchor) {
        if (!sp.isAlive() || sp.hasDisconnected() || tick > 30) {
            finish(sp);
            return;
        }
        Vec3 d = target.subtract(sp.position().add(0, 0.9, 0));
        double dist = d.length();
        if (dist < 1.6) {
            finish(sp);
            return;
        }
        double speed = Math.min(1.35, 0.55 + tick * 0.08);
        sp.setDeltaMovement(d.scale(1.0 / dist).scale(speed));
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        ServerLevel w = sp.serverLevel();
        if (tick % 2 == 0) {
            Fx.rainbowLine(w, sp.position().add(0, 1.2, 0), anchor, 0.7, tick * 0.05F);
        }
        w.sendParticles(Fx.spark(Fx.rainbow(tick * 0.08F), 1.0F), sp.getX(), sp.getY() + 0.5, sp.getZ(), 2, 0.25, 0.25, 0.25, 0.0);
        Scheduler.runLater(1, () -> pull(sp, target, tick + 1, anchor));
    }

    private static void finish(ServerPlayer sp) {
        if (!sp.isAlive() || sp.hasDisconnected()) {
            return;
        }
        sp.setDeltaMovement(sp.getDeltaMovement().scale(0.35));
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        sp.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 3, 0, false, false, false));
        ServerLevel w = sp.serverLevel();
        Fx.rainbowRing(w, sp.getX(), sp.getY() + 0.8, sp.getZ(), 0.9, 18, 0);
        w.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.PLAYERS, 0.8F, 1.4F);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "rainbow_grapple", 2);
    }
}
