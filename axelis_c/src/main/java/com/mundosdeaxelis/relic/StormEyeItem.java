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

/** Ojo de la Tormenta: rayo que cae donde miras (hasta 40 bloques). */
public class StormEyeItem extends RelicItem {
    public StormEyeItem() {
        super("storm_eye", 0x8FA8FF, 20 * 20, Rarity.EPIC);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sl.isThundering() || sl.isRaining()) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        Vec3 eye = sp.getEyePosition();
        Vec3 look = sp.getLookAngle();
        var hit = sl.clip(new ClipContext(eye, eye.add(look.scale(40.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp));
        if (hit.getType() == HitResult.Type.MISS) {
            sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.storm_miss"), true);
            return false;
        }
        Vec3 t = hit.getLocation();
        Fx.sound(sl, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 2.0F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0x8FA8FF, 30);
        for (int i = 0; i < 6; i++) {
            final int k = i;
            Scheduler.runLater(k * 2, () -> Fx.ring(sl, Fx.spark(0xC8D6FF, 1.2F), t.x, t.y + 0.2, t.z, 3.5 - k * 0.5, 12, k));
        }
        Scheduler.runLater(12, () -> {
            var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(sl);
            if (bolt != null) {
                bolt.moveTo(t.x, t.y, t.z);
                bolt.setVisualOnly(true);
                sl.addFreshEntity(bolt);
            }
            Fx.ring(sl, Fx.dust(0x8FA8FF, 1.6F), t.x, t.y + 0.3, t.z, 1.5, 20, 0);
            for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(t.x - 3.5, t.y - 2, t.z - 3.5, t.x + 3.5, t.y + 4, t.z + 3.5), e -> e != sp && e.isAlive())) {
                e.hurt(sl.damageSources().playerAttack(sp), 14.0F);
                e.setSecondsOnFire(3);
            }
        });
        return true;
    }
}
