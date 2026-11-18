package com.mundosdeaxelis.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

/**
 * Salamandra de lava (volcán, etapa 4): lagarto de basalto con manchas de magma. Camina sobre la lava como sobre el suelo y, si
 * está en ella, se cura poco a poco (sácala a tierra firme). De cerca muerde y quema; de lejos abre la boca (aviso de medio
 * segundo) y escupe una bola de lava en arco: un círculo rojo marca dónde va a caer.
 */
public class SalamandraLava extends Monster {
    private static final EntityDataAccessor<Integer> SPIT = SynchedEntityData.defineId(SalamandraLava.class, EntityDataSerializers.INT);
    private static final int SPIT_WINDUP = 12;
    private static final float GLOB_DAMAGE = 5.0F;

    private int spitCd = 50;

    public SalamandraLava(EntityType<? extends SalamandraLava> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        setPathfindingMalus(BlockPathTypes.LAVA, 0.0F);
        setPathfindingMalus(BlockPathTypes.DANGER_FIRE, 0.0F);
        setPathfindingMalus(BlockPathTypes.DAMAGE_FIRE, 0.0F);
        setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 24.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPIT, 0);
    }

    /** 0..1: cuánto tiene abierta la boca (va a escupir). */
    public float spitOpen(float pt) {
        int s = this.entityData.get(SPIT);
        if (s <= 0) {
            return 0F;
        }
        return Math.min(1F, (SPIT_WINDUP - s + pt) / 6F);
    }

    /** ¿Está sobre lava o metida en ella? (sus manchas brillan más). */
    public boolean onLava() {
        return isInLava() || this.level().getFluidState(blockPosition().below()).is(FluidTags.LAVA)
                || this.level().getFluidState(blockPosition()).is(FluidTags.LAVA);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, false));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------ caminar sobre lava (como el lagarto del Nether)

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new LavaNavigation(this, level);
    }

    @Override
    public boolean canStandOnFluid(FluidState fluid) {
        return fluid.is(FluidTags.LAVA);
    }

    @Override
    public void tick() {
        super.tick();
        // si se hunde en la lava, sube flotando a la superficie
        if (isInLava() && !this.level().isClientSide) {
            setOnGround(true);
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.05, 0.0));
        }
    }

    static final class LavaNavigation extends GroundPathNavigation {
        LavaNavigation(Mob mob, Level level) {
            super(mob, level);
        }

        @Override
        protected PathFinder createPathFinder(int maxVisited) {
            this.nodeEvaluator = new WalkNodeEvaluator();
            this.nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(this.nodeEvaluator, maxVisited);
        }

        @Override
        protected boolean hasValidPathType(BlockPathTypes type) {
            return type == BlockPathTypes.LAVA || type == BlockPathTypes.DAMAGE_FIRE || type == BlockPathTypes.DANGER_FIRE
                    || super.hasValidPathType(type);
        }

        @Override
        public boolean isStableDestination(BlockPos pos) {
            return this.level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
        }
    }

    // ------------------------------------------------------------------ escupir lava

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        // en la lava se recupera (1 de vida por segundo)
        if (this.tickCount % 20 == 0 && onLava() && getHealth() < getMaxHealth()) {
            heal(1.0F);
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 2, 0.3, 0.1, 0.3, 0.0);
        }
        int s = this.entityData.get(SPIT);
        LivingEntity t = getTarget();
        if (s > 0) {
            this.entityData.set(SPIT, s - 1);
            getNavigation().stop();
            if (t != null) {
                getLookControl().setLookAt(t, 60.0F, 60.0F);
                lookAtYaw(t);
            }
            if (s == 1 && t != null && t.isAlive()) {
                Vec3 mouth = position().add(Vec3.directionFromRotation(0, getYRot()).scale(0.8)).add(0, 0.35, 0);
                Vec3 aim = t.position().add(t.getDeltaMovement().scale(8.0));
                double d = mouth.distanceTo(aim);
                VolcanFx.lavaGlob(sl, this, mouth, aim, (int) Math.max(12, Math.min(26, d * 1.6)), 2.0 + d * 0.12, GLOB_DAMAGE, 1.6);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.9F, 0.7F);
                spitCd = 70 + this.random.nextInt(40);
            }
            return;
        }
        if (t == null || !t.isAlive()) {
            return;
        }
        double dist = distanceTo(t);
        if (--spitCd <= 0 && dist > 4.0 && dist < 14.0 && hasLineOfSight(t)) {
            this.entityData.set(SPIT, SPIT_WINDUP);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.STRIDER_HURT, SoundSource.HOSTILE, 1.0F, 0.6F);
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.4, getZ(), 6, 0.3, 0.1, 0.3, 0.02);
        }
    }

    private void lookAtYaw(LivingEntity t) {
        double dx = t.getX() - getX(), dz = t.getZ() - getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
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
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(onLava() ? 2 : 6) == 0) {
            this.level().addParticle(this.random.nextInt(4) == 0 ? ParticleTypes.LAVA : ParticleTypes.SMALL_FLAME,
                    getX() + (this.random.nextDouble() - 0.5) * 0.8, getY() + 0.3, getZ() + (this.random.nextDouble() - 0.5) * 0.8, 0.0, 0.01, 0.0);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.3, getZ(), 10, 0.4, 0.1, 0.4, 0.0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.3, getZ(), 10, 0.4, 0.2, 0.4, 0.02);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRIDER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRIDER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRIDER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F + this.random.nextFloat() * 0.15F;
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(onLava() ? SoundEvents.LAVA_POP : SoundEvents.STRIDER_STEP, 0.4F, 0.8F);
    }
}
