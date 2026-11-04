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

/** Escama de Dragón: defensa al estar herido; activo: aliento de dragón en cono. */
public class DragonScaleItem extends RelicItem {
    public DragonScaleItem() {
        super("dragon_scale", 0xFF5A3A, 20 * 15, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getHealth() <= sp.getMaxHealth() * 0.4F) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 1, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.PLAYERS, 1.0F, 1.3F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xFF5A3A, 30);
        for (int i = 0; i < 12; i++) {
            final int k = i;
            Scheduler.runLater(k, () -> {
                Vec3 l = sp.getLookAngle();
                Vec3 o = sp.getEyePosition();
                ServerLevel w = sp.serverLevel();
                for (int s = 1; s <= 10; s++) {
                    double spread = s * 0.06;
                    w.sendParticles(Fx.dust(0xFF5A3A, 1.0F + s * 0.08F), o.x + l.x * s, o.y + l.y * s - 0.2, o.z + l.z * s, 1, spread, spread, spread, 0.0);
                    if (s % 3 == 0) {
                        w.sendParticles(Fx.spark(0xFFC36B, 1.0F), o.x + l.x * s, o.y + l.y * s - 0.2, o.z + l.z * s, 1, spread, spread, spread, 0.0);
                    }
                }
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye.add(look.scale(11.0))).inflate(3.0), e -> e != sp && e.isAlive())) {
            Vec3 d = e.getEyePosition().subtract(eye);
            double len = d.length();
            if (len > 0.1 && len < 11.0 && d.normalize().dot(look) > 0.85) {
                e.hurt(sl.damageSources().playerAttack(sp), 9.0F);
                e.setSecondsOnFire(5);
            }
        }
        return true;
    }
}
