package com.mundosdeaxelis.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Diablillo (volcán, etapa 4): bichito rojo con alas de murciélago que vuela en círculos a tu alrededor riéndose. Cada tanto
 * levanta una bola de fuego que crece en su mano (aviso de un segundo) y te la lanza; otras veces se lanza en picada a morderte y
 * vuelve a subir. Frágil, pero rápido y molesto.
 */
public class Diablillo extends Monster {
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(Diablillo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DIVE = SynchedEntityData.defineId(Diablillo.class, EntityDataSerializers.BOOLEAN);
    private static final int CHARGE_TICKS = 20;
    private static final float BALL_DAMAGE = 4.0F;

    private int shootCd = 50;
    private int diveCd = 120;
    private int diveT;
    private int moveCd;
    private final double orbit;

    public Diablillo(EntityType<? extends Diablillo> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        this.orbit = this.random.nextBoolean() ? 1.0D : -1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.65D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGE, 0);
        this.entityData.define(DIVE, false);
    }

    /** 0..1: lo grande que va la bola de fuego en su mano. */
    public float charge(float pt) {
        int c = this.entityData.get(CHARGE);
        return c <= 0 ? 0F : Math.min(1F, (c + pt) / CHARGE_TICKS);
    }

    public boolean diving() {
        return this.entityData.get(DIVE);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (this.random.nextDouble() - 0.5) * 0.5, getY() + 0.2,
                    getZ() + (this.random.nextDouble() - 0.5) * 0.5, 0.0, -0.02, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            this.entityData.set(CHARGE, 0);
            this.entityData.set(DIVE, false);
            if (--moveCd <= 0) {
                moveCd = 30 + this.random.nextInt(40);
                getMoveControl().setWantedPosition(getX() + this.random.nextGaussian() * 4, getY() + this.random.nextGaussian() * 0.8,
                        getZ() + this.random.nextGaussian() * 4, 0.6D);
            }
            return;
        }
        getLookControl().setLookAt(t, 40.0F, 40.0F);
        // en picada: va directo a morder y luego sube
        if (diving()) {
            diveT++;
            getMoveControl().setWantedPosition(t.getX(), t.getY() + t.getBbHeight() * 0.6, t.getZ(), 2.2D);
            if (distanceToSqr(t) < 1.7 * 1.7) {
                doHurtTarget(t);
                t.setSecondsOnFire(2);
                endDive(sl, t);
            } else if (diveT > 45) {
                endDive(sl, t);
            }
            return;
        }
        if (--moveCd <= 0) {
            moveCd = 10 + this.random.nextInt(10);
            Vec3 off = position().subtract(t.position());
            double ang = Math.atan2(off.z, off.x) + orbit * (0.5 + this.random.nextDouble() * 0.6);
            double dist = 4.5 + this.random.nextDouble() * 3.0;
            getMoveControl().setWantedPosition(t.getX() + Math.cos(ang) * dist, t.getY() + 2.0 + this.random.nextDouble() * 1.5,
                    t.getZ() + Math.sin(ang) * dist, 1.0D);
        }
        int c = this.entityData.get(CHARGE);
        if (c > 0) {
            this.entityData.set(CHARGE, c + 1);
            if (c % 6 == 0) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_BURN, SoundSource.HOSTILE, 0.6F, 1.6F);
            }
            if (c >= CHARGE_TICKS) {
                if (hasLineOfSight(t)) {
                    Vec3 hand = position().add(0, 0.95, 0).add(Vec3.directionFromRotation(0, this.yBodyRot - 90).scale(0.25));
                    VolcanFx.fireball(sl, this, t, hand, BALL_DAMAGE, 0.6, 0.22);
                }
                this.entityData.set(CHARGE, 0);
                shootCd = 50 + this.random.nextInt(30);
            }
            return;
        }
        if (--diveCd <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 12.0 * 12.0) {
            this.entityData.set(DIVE, true);
            diveT = 0;
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.VEX_CHARGE, SoundSource.HOSTILE, 1.0F, 1.3F);
            return;
        }
        if (--shootCd <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 18.0 * 18.0) {
            this.entityData.set(CHARGE, 1);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.VEX_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.5F);
        }
    }

    private void endDive(ServerLevel sl, LivingEntity t) {
        this.entityData.set(DIVE, false);
        diveCd = 140 + this.random.nextInt(80);
        moveCd = 20;
        Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
        away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
        getMoveControl().setWantedPosition(getX() + away.x * 5, t.getY() + 4.0, getZ() + away.z * 5, 1.4D);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.VEX_AMBIENT, SoundSource.HOSTILE, 1.0F, 1.7F);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.5, getZ(), 20, 0.25, 0.25, 0.25, 0.08);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 10, 0.2, 0.2, 0.2, 0.03);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.6F);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.25F + this.random.nextFloat() * 0.2F;
    }
}
