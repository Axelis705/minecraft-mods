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

/** Cinturón del Titán: piel dura; activo: golpe sísmico contra el suelo. */
public class TitanBeltItem extends RelicItem {
    public TitanBeltItem() {
        super("titan_belt", 0xB88A5A, 20 * 18, Rarity.RARE);
    }

    @Override
    protected void passive(ServerPlayer sp, ServerLevel sl, long time) {
        if (sp.getHealth() < sp.getMaxHealth() * 0.75F) {
            sp.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, true, false, true));
        }
    }

    @Override
    protected boolean active(ServerPlayer sp, ServerLevel sl, ItemStack stack) {
        double x = sp.getX();
        double y = sp.getY();
        double z = sp.getZ();
        Fx.sound(sl, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.8F, 0.6F);
        Fx.sound(sl, x, y, z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0F, 0.5F);
        Fx.screen(sp, ScreenFxPacket.VIGNETTE, 0xB88A5A, 40);
        sp.fallDistance = 0.0F;
        for (int i = 1; i <= 6; i++) {
            final int r = i;
            Scheduler.runLater(i, () -> {
                Fx.ring(sl, Fx.dust(0xB88A5A, 1.6F), x, y + 0.15, z, r, 14 + r * 4, 0);
                sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 6, r * 0.5, 0.05, r * 0.5, 0.01);
            });
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new AABB(x - 6, y - 3, z - 6, x + 6, y + 3, z + 6), e -> e != sp && e.isAlive())) {
            e.hurt(sl.damageSources().playerAttack(sp), 8.0F);
            e.setDeltaMovement(e.getDeltaMovement().add(0.0, 0.9, 0.0));
            e.hurtMarked = true;
        }
        return true;
    }
}
