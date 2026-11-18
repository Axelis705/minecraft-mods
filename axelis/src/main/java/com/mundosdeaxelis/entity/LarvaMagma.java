package com.mundosdeaxelis.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Larva de magma: gusanito de lava que suelta el Corazón de Magma. Rápida y frágil; su mordida quema. Al morir revienta en una
 * nube de chispas (sin romper bloques).
 */
public class LarvaMagma extends Monster {
    public LarvaMagma(EntityType<? extends LarvaMagma> type, Level level) {
        super(type, level);
        this.xpReward = 2;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 14.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.31D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    /** Vida y daño según el jefe que la suelta (escala con la etapa y el número de jugadores). */
    public void scaleTo(float hp, float dmg) {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(dmg);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            target.setSecondsOnFire(3);
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.3, getZ(), 24, 0.3, 0.2, 0.3, 0.08);
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 6, 0.2, 0.1, 0.2, 0.0);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.MAGMA_CUBE_SQUISH, SoundSource.HOSTILE, 1.2F, 1.4F);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(1.4))) {
                if (!p.isCreative() && !p.isSpectator()) {
                    p.setSecondsOnFire(2);
                }
            }
        }
        super.die(source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(5) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (this.random.nextDouble() - 0.5) * 0.5, getY() + 0.35,
                    getZ() + (this.random.nextDouble() - 0.5) * 0.5, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.MAGMA_CUBE_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.MAGMA_CUBE_DEATH_SMALL;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.SILVERFISH_STEP, 0.4F, 0.6F);
    }
}
