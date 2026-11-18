package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.fx.Fx;
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

/** Amuleto del Zorro: ligero y veloz; activo: bomba de humo que te vuelve invisible y desorienta a los monstruos. */
public class FoxCharmItem extends RelicItem {
    public FoxCharmItem() {
        super("fox_charm", 0xFF9A3A, 20 * 35, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 0, true, false, true));
        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0, true, false, true));
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.8F, 1.6F);
        Fx.sound(sl, x, y, z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8F, 1.6F);
        sp.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20 * 8, 0));
        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 8, 1));
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 20, 0.8, 0.6, 0.8, 0.02);
        for (int i = 0; i < 30; i++) {
            double a = sl.getRandom().nextDouble() * Math.PI * 2.0;
            sl.sendParticles(Fx.dust(0xB0B0B8, 1.8F), x, y + 0.6, z, 0, Math.cos(a) * 0.15, 0.05, Math.sin(a) * 0.15, 1.0);
        }
        Fx.ring(sl, Fx.spark(0xFFB060, 1.0F), x, y + 0.5, z, 1.2, 14, 0);
        for (Mob m : sl.getEntitiesOfClass(Mob.class, new AABB(x - 10, y - 4, z - 10, x + 10, y + 4, z + 10), m -> m instanceof Enemy && m.isAlive())) {
            m.setTarget(null);
            m.getNavigation().stop();
            m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 5, 0));
            m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 3, 1));
        }
        return true;
    }
}
