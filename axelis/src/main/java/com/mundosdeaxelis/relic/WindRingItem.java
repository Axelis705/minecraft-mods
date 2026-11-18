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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Anillo del Viento: velocidad y saltos altos; activo: ráfaga que te impulsa hacia adelante. */
public class WindRingItem extends RelicItem {
    public WindRingItem() {
        super("wind_ring", 0xCFFFF0, 20 * 5, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 1, true, false, true));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 look = sp.getLookAngle();
        sp.setDeltaMovement(look.x * 2.3, Math.max(0.3, look.y * 1.2), look.z * 2.3);
        sp.hurtMarked = true;
        sp.fallDistance = 0.0F;
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0F, 1.6F);
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.4F);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 18);
        for (int i = 0; i < 14; i++) {
            final int k = i;
            Scheduler.runLater(i, () -> {
                if (sp.isAlive() && !sp.hasDisconnected()) {
                    ServerLevel w = sp.serverLevel();
                    w.sendParticles(Fx.dust(0xCFFFF0, 1.1F), sp.getX(), sp.getY() + 0.9, sp.getZ(), 3, 0.25, 0.4, 0.25, 0.0);
                    w.sendParticles(Fx.spark(0xFFFFFF, 0.8F), sp.getX(), sp.getY() + 0.9, sp.getZ(), 1, 0.2, 0.3, 0.2, 0.0);
                    sp.fallDistance = 0.0F;
                }
            });
        }
        return true;
    }
}
