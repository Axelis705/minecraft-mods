package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Grimorio Arcano: lanza hasta 5 proyectiles mágicos que buscan a los enemigos cercanos. */
public class ArcaneTomeItem extends RelicItem {
    public ArcaneTomeItem() {
        super("arcane_tome", 0xC27BFF, 20 * 12, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.experienceLevel > 0 && time % 200 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        var targets = sl.getEntitiesOfClass(LivingEntity.class, new AABB(sp.blockPosition()).inflate(18.0), e -> e instanceof Enemy && e.isAlive());
        if (targets.isEmpty()) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.tome_none"), true);
            return false;
        }
        targets.sort((a, b) -> Double.compare(a.distanceToSqr(sp), b.distanceToSqr(sp)));
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xC27BFF, 30);
        int shots = Math.min(5, Math.max(targets.size(), 3));
        for (int s = 0; s < shots; s++) {
            final LivingEntity target = targets.get(s % targets.size());
            final double side = (s - (shots - 1) / 2.0) * 0.9;
            Scheduler.runLater(s * 3, () -> {
                Vec3 from = sp.position().add(0.0, 1.5, 0.0);
                Vec3 look = sp.getLookAngle();
                Vec3 perp = new Vec3(-look.z, 0.0, look.x);
                Vec3 start = from.add(perp.scale(side));
                for (int k = 1; k <= 10; k++) {
                    final int step = k;
                    Scheduler.runLater(k, () -> {
                        if (!target.isAlive()) {
                            return;
                        }
                        Vec3 end = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
                        double u = step / 10.0;
                        Vec3 p = start.lerp(end, u).add(0.0, Math.sin(u * Math.PI) * 1.2, 0.0);
                        ServerLevel w = sp.serverLevel();
                        w.sendParticles(Fx.dust(0xC27BFF, 1.2F), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
                        w.sendParticles(Fx.spark(0xF0DCFF, 0.9F), p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
                        if (step == 10) {
                            target.hurt(w.damageSources().indirectMagic(sp, sp), 7.0F);
                            w.sendParticles(Fx.spark(0xFFFFFF, 1.5F), end.x, end.y, end.z, 6, 0.3, 0.3, 0.3, 0.0);
                            w.playSound(null, end.x, end.y, end.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9F, 1.5F);
                        }
                    });
                }
            });
        }
        return true;
    }
}
