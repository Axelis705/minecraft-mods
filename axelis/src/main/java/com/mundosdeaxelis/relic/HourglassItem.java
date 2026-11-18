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

/** Reloj de Arena: detiene el tiempo a tu alrededor; los enemigos casi no se mueven y tú vas veloz. */
public class HourglassItem extends RelicItem {
    public HourglassItem() {
        super("hourglass", 0xE8D8A8, 20 * 60, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 0, true, false, true));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.2F, 0.5F);
        Fx.sound(sl, x, y, z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 0.6F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xE8D8A8, 160);
        Fx.screen(sp, ScreenFxPacket.SPEED, 0xFFFFFF, 30);
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 3));
        sp.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20 * 8, 2));
        for (int i = 1; i <= 10; i++) {
            final int r = i * 2;
            Scheduler.runLater(i, () -> Fx.ring(sl, Fx.dust(0xE8D8A8, 1.3F), x, y + 0.2, z, r, 12 + r * 3, r * 0.1));
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 16, y - 8, z - 16, x + 16, y + 8, z + 16), e -> e != sp && !(e instanceof ServerPlayer) && e.isAlive())) {
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 8, 6));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 8, 3));
            sl.sendParticles(Fx.spark(0xE8D8A8, 0.9F), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 5, 0.3, 0.4, 0.3, 0.0);
        }
        return true;
    }
}
