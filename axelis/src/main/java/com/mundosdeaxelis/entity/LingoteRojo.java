package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Lingote al rojo (etapa 267): lo lanza el Coloso de Hierro en arco. Al dar a alguien le hace daño y lo prende un momento; al caer
 * al suelo chisporrotea y se apaga (no prende fuego al mundo).
 */
public class LingoteRojo extends ThrowableItemProjectile {
    private float damage = 6.0F;

    public LingoteRojo(EntityType<? extends LingoteRojo> type, Level level) {
        super(type, level);
    }

    public LingoteRojo(Level level, LivingEntity owner) {
        super(ModEntities.LINGOTE_ROJO.get(), owner, level);
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.LINGOTE_ROJO.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.FLAME, getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
            this.level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0.0, 0.02, 0.0);
        }
        if (this.tickCount > 200) {
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        Entity e = hit.getEntity();
        if (e instanceof ColosoHierro || e == getOwner()) {
            return;
        }
        e.hurt(damageSources().thrown(this, getOwner()), damage);
        e.setSecondsOnFire(3);
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY && ((EntityHitResult) result).getEntity() instanceof ColosoHierro) {
            return; // atraviesa a quien lo lanzó
        }
        super.onHit(result);
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY(), getZ(), 6, 0.2, 0.1, 0.2, 0.0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 4, 0.2, 0.1, 0.2, 0.02);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.LAVA_EXTINGUISH, SoundSource.HOSTILE, 0.8F, 1.2F);
            discard();
        }
    }
}
