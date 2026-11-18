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

/** Cristal Lunar: de noche ves y te recuperas; activo: pulso de gravedad que eleva a los enemigos. */
public class MoonCrystalItem extends RelicItem {
    public MoonCrystalItem() {
        super("moon_crystal", 0xB8C8FF, 20 * 30, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.isNight()) {
            sp.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, true, false, false));
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.5F);
        Fx.sound(sl, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xB8C8FF, 50);
        Fx.screen(sp, ScreenFxPacket.STARS, 0xE0E8FF, 60);
        for (int i = 1; i <= 8; i++) {
            final int r = i;
            Scheduler.runLater(i * 2, () -> {
                Fx.ring(sl, Fx.dust(0xB8C8FF, 1.4F), x, y + 0.2, z, r, 12 + r * 3, 0);
                Fx.ring(sl, Fx.spark(0xFFFFFF, 0.9F), x, y + 1.5, z, r, 8 + r * 2, r * 0.5);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 8, y - 4, z - 8, x + 8, y + 4, z + 8), e -> e instanceof Enemy && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40, 2));
            e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.6, 0.0));
            e.hurtMarked = true;
            sl.sendParticles(Fx.spark(0xB8C8FF, 1.2F), e.getX(), e.getY() + 1.0, e.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
        }
        return true;
    }
}
