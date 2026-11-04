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
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Cañón de confeti: una fiesta en forma de cono. Los que atrapa brillan y salen volando un poquito. */
public class ConfettiCannonItem extends Item {
    public ConfettiCannonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        ServerLevel sl = sp.serverLevel();
        sp.getCooldowns().addCooldown(this, 30);
        stack.hurtAndBreak(1, sp, p -> p.broadcastBreakEvent(hand));

        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Vec3 muzzle = eye.add(look.scale(0.9)).add(0, -0.25, 0);
        Fx.sound(sl, eye.x, eye.y, eye.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 1.0F);
        Fx.sound(sl, eye.x, eye.y, eye.z, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.PLAYERS, 1.0F, 1.2F);
        Fx.sound(sl, eye.x, eye.y, eye.z, SoundEvents.NOTE_BLOCK_BELL.get(), SoundSource.PLAYERS, 1.0F, 1.6F);
        sl.sendParticles(ParticleTypes.FLASH, muzzle.x, muzzle.y, muzzle.z, 1, 0, 0, 0, 0);
        Fx.screen(sp, ScreenFxPacket.CONFETTI, 0xFFFFFF, 64);

        // Base del cono: dos vectores perpendiculares a la mirada.
        Vec3 up = Math.abs(look.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(up).normalize();
        Vec3 upv = right.cross(look).normalize();

        // La nube de confeti avanza en 8 pasos (como si fuera disparada).
        for (int step = 0; step < 9; step++) {
            final int s = step;
            Scheduler.runLater(step + 1, () -> {
                if (!sp.isAlive() || sp.hasDisconnected()) {
                    return;
                }
                ServerLevel w = sp.serverLevel();
                RandomSource r = w.getRandom();
                double dist = 1.0 + s * 1.15;
                for (int i = 0; i < 12; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rad = Math.sqrt(r.nextDouble()) * (0.25 + dist * 0.28);
                    Vec3 p = muzzle.add(look.scale(dist)).add(right.scale(Math.cos(a) * rad)).add(upv.scale(Math.sin(a) * rad));
                    // Papelito con velocidad hacia adelante: sale disparado y luego cae dando vueltas.
                    w.sendParticles(Fx.paper(Fx.rainbow(r.nextFloat()), 1.0F + r.nextFloat() * 0.8F), p.x, p.y, p.z, 0,
                            look.x * 0.35 + (r.nextDouble() - 0.5) * 0.1, look.y * 0.35 + 0.05, look.z * 0.35 + (r.nextDouble() - 0.5) * 0.1, 1.0);
                    if (i % 3 == 0) {
                        w.sendParticles(Fx.spark(Fx.rainbow(r.nextFloat()), 0.9F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                }
                Vec3 c = muzzle.add(look.scale(dist));
                w.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 4, 0.4, 0.4, 0.4, 0.05);
                if (s == 3 || s == 6) {
                    w.sendParticles(ParticleTypes.NOTE, c.x, c.y, c.z, 2, 0.6, 0.4, 0.6, 1.0);
                }
            });
        }

        // Quienes estén en el cono: brillan y reciben un empujón de fiesta.
        AABB area = new AABB(muzzle, muzzle.add(look.scale(11))).inflate(2.5);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, area, t -> t != sp && t.isAlive())) {
            Vec3 to = e.getEyePosition().subtract(muzzle);
            double len = to.length();
            if (len < 0.1 || len > 11.5 || to.normalize().dot(look) < 0.72) {
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20 * 8, 0, false, false, false));
            e.push(look.x * 0.6, 0.35, look.z * 0.6);
            e.hurtMarked = true;
            sl.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
        }
        sp.push(-look.x * 0.15, 0.0, -look.z * 0.15);
        sp.hurtMarked = true;
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Tips.add(tooltip, "confetti_cannon", 2);
    }
}
