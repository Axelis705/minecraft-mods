package com.mundosdeaxelis.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ancla de magma: cristales de lava sobre una roca de obsidiana, en un pedestal de la arena del volcán. De ella sale una cadena
 * hasta el Corazón de Magma: mientras quede alguna en pie, el corazón tiene su escudo. Solo se rompe a golpes de jugadores.
 */
public class AnclaMagma extends Mob {
    public AnclaMagma(EntityType<? extends AnclaMagma> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setNoGravity(true);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    /** Ajusta su vida (al aparecer): la pone llena con ese máximo. */
    public void setLife(float max) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
        setHealth(max);
    }

    /** Punto donde se engancha la cadena (el grillete, a media altura de los cristales). */
    public Vec3 hook() {
        return position().add(0, 0.95, 0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // solo la rompen los jugadores (ni el fuego, ni la lava, ni las explosiones del corazón)
        if (!(source.getEntity() instanceof Player) || source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        boolean ok = super.hurt(source, amount);
        if (ok && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.9, getZ(), 3, 0.25, 0.3, 0.25, 0.0);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.4F, 0.5F + this.random.nextFloat() * 0.2F);
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.8, getZ(), 30, 0.4, 0.5, 0.4, 0.0);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 40, 0.4, 0.6, 0.4, 0.08);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        // se rompe al momento (sin la animación de caer de lado)
        if (!this.level().isClientSide && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        if (this.level().isClientSide && this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.FLAME, getX() + (this.random.nextDouble() - 0.5) * 0.6, getY() + 0.6 + this.random.nextDouble() * 0.8,
                    getZ() + (this.random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_CLUSTER_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.CHAIN_BREAK;
    }
}
