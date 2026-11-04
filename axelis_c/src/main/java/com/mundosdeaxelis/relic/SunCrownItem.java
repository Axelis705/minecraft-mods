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

/** Corona del Rey Sol: regenera bajo el sol; activo: llamarada solar que quema y expulsa a los enemigos. */
public class SunCrownItem extends RelicItem {
    public SunCrownItem() {
        super("sun_crown", 0xFFC93A, 20 * 30, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.isDay() && sl.canSeeSky(sp.blockPosition())) {
            sp.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.2F, 1.5F);
        Fx.sound(sl, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
        Fx.screen(sp, ScreenFxPacket.SUNRISE, 0xFFC93A, 50);
        for (int i = 1; i <= 8; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.dust(0xFFC93A, 1.5F), x, y + 0.3, z, r, 12 + r * 4, r * 0.2);
                Fx.ring(sl, Fx.spark(0xFFF3B0, 1.0F), x, y + 0.9, z, r, 8 + r * 2, -r * 0.3);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 9, y - 4, z - 9, x + 9, y + 4, z + 9), e -> e instanceof Enemy && e.isAlive())) {
            e.hurt(sl.damageSources().playerAttack(sp), e.getMobType() == MobType.UNDEAD ? 14.0F : 7.0F);
            e.setSecondsOnFire(5);
            e.push((e.getX() - x) * 0.2, 0.35, (e.getZ() - z) * 0.2);
            e.hurtMarked = true;
        }
        return true;
    }
}
