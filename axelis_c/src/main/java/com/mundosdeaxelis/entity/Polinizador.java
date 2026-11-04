package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Polinizador: un insecto-flor que vuela en círculos a unos 7 bloques de ti. Antes de disparar echa la cola hacia atrás
 * (aviso de un segundo) y lanza una espora que hace daño y te deja Lentitud y Debilidad unos segundos. Es frágil: hay que
 * perseguirlo o esperar a que baje. Vida y daño salen de {@link DungeonBalance}.
 */
public class Polinizador extends Monster {
    private static final EntityDataAccessor<Boolean> CHARGING = SynchedEntityData.defineId(Polinizador.class, EntityDataSerializers.BOOLEAN);
    private static final float HITS = 1.6F;
    private static final float BASE_DAMAGE = 5.5F;
    private static final float SPEED = 1.3F;

    private float shotDamage = DungeonBalance.dmg(1, BASE_DAMAGE, 1);
    private int shootCd = 50;
    private int chargeTicks = 0;
    private int moveCd = 0;
    private double orbit = 1.0D;

    public Polinizador(EntityType<? extends Polinizador> type, Level level) {
        super(type, level);
        this.xpReward = 7;
        this.moveControl = new FlyingMoveControl(this, 20, true);
        this.setNoGravity(true);
        setPersistenceRequired();
        this.orbit = random.nextBoolean() ? 1.0D : -1.0D;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 26.0D);
    }

    public void scaleTo(int stage, int players) {
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        shotDamage = DungeonBalance.dmg(stage, BASE_DAMAGE, players);
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
        this.entityData.define(CHARGING, false);
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGING);
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
        if (this.level().isClientSide && random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 0.3, getZ() + (random.nextDouble() - 0.5) * 0.6, 0.0, -0.02, 0.0);
        }
        if (!this.level().isClientSide && tickCount % 40 == 0 && random.nextInt(3) == 0) {
            playSound(ModSounds.POLINIZADOR_BUZZ.get(), 0.6F, 0.9F + random.nextFloat() * 0.2F);
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
            this.entityData.set(CHARGING, false);
            chargeTicks = 0;
            return;
        }
        getLookControl().setLookAt(t, 40.0F, 40.0F);
        // vuela en círculos a unos 7 bloques, un poco por encima del objetivo
        if (--moveCd <= 0) {
            moveCd = 12 + random.nextInt(10);
            Vec3 off = position().subtract(t.position());
            double ang = Math.atan2(off.z, off.x) + orbit * (0.5 + random.nextDouble() * 0.5);
            double dist = 6.0 + random.nextDouble() * 2.5;
            double tx = t.getX() + Math.cos(ang) * dist;
            double tz = t.getZ() + Math.sin(ang) * dist;
            double ty = t.getY() + 2.0 + random.nextDouble() * 1.5;
            this.getMoveControl().setWantedPosition(tx, ty, tz, 1.0D);
        }
        if (isCharging()) {
            chargeTicks++;
            if (chargeTicks % 4 == 0) {
                sl.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.3, getZ(), 3, 0.2, 0.2, 0.2, 0.02);
            }
            if (chargeTicks >= 20) {
                if (hasLineOfSight(t)) {
                    fire(sl, t);
                }
                this.entityData.set(CHARGING, false);
                chargeTicks = 0;
                shootCd = 60 + random.nextInt(30);
            }
        } else if (--shootCd <= 0 && hasLineOfSight(t) && distanceToSqr(t) < 20.0 * 20.0) {
            this.entityData.set(CHARGING, true);
            chargeTicks = 0;
        }
    }

    private void fire(ServerLevel sl, LivingEntity t) {
        Arrow arrow = new Arrow(sl, this);
        arrow.setPos(getX(), getY() + 0.2, getZ());
        Vec3 d = t.getEyePosition().subtract(arrow.position());
        double flat = Math.hypot(d.x, d.z);
        arrow.shoot(d.x, d.y + flat * 0.03, d.z, SPEED, 2.0F);
        arrow.setBaseDamage(shotDamage / SPEED);
        arrow.setCritArrow(false);
        arrow.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 0));
        arrow.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        sl.addFreshEntity(arrow);
        Scheduler.runLater(100, arrow::discard);
        playSound(ModSounds.POLINIZADOR_SPORE.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.2, getZ(), 8, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            Shots.cancelOwned(sl, this);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.4, getZ(), 18, 0.4, 0.4, 0.4, 0.03);
            sl.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.4, getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        }
        super.die(source);
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
        return ModSounds.POLINIZADOR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.POLINIZADOR_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("ShotDamage", shotDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("ShotDamage")) {
            shotDamage = tag.getFloat("ShotDamage");
        }
    }
}
