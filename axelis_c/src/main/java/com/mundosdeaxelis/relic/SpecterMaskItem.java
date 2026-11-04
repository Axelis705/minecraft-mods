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

/** Máscara Fantasma: agachado y quieto te vuelves invisible; activo: forma de niebla. */
public class SpecterMaskItem extends RelicItem {
    public SpecterMaskItem() {
        super("specter_mask", 0xC9B8FF, 20 * 40, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        Vec3 m = sp.getDeltaMovement();
        if (sp.isShiftKeyDown() && m.x * m.x + m.z * m.z < 0.0004) {
            sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, true, false, false));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 8, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20 * 8, 2));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 2));
        Fx.sound(sl, x, y, z, SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.5F, 0.8F);
        Fx.sound(sl, x, y, z, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.0F, 0.5F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xC9B8FF, 80);
        for (int i = 0; i < 40; i++) {
            double a = sl.getRandom().nextDouble() * Math.PI * 2.0;
            sl.sendParticles(Fx.dust(0xC9B8FF, 1.6F), x, y + 0.2 + sl.getRandom().nextDouble() * 1.6, z, 0, Math.cos(a) * 0.1, 0.03, Math.sin(a) * 0.1, 1.0);
        }
        sl.sendParticles(ParticleTypes.SOUL, x, y + 1.0, z, 12, 0.5, 0.7, 0.5, 0.03);
        for (int i = 0; i < 16; i++) {
            Scheduler.runLater(i * 10, () -> {
                if (sp.isAlive() && !sp.hasDisconnected()) {
                    sp.serverLevel().sendParticles(Fx.dust(0xC9B8FF, 1.0F), sp.getX(), sp.getY() + 0.9, sp.getZ(), 2, 0.3, 0.6, 0.3, 0.0);
                }
            });
        }
        return true;
    }
}
