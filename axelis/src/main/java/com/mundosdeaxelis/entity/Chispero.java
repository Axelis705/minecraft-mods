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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Chispero: un horno de hierro con patas, anclado a una repisa. No se mueve: te mira, se pone al rojo casi un segundo (aviso)
 * y escupe una flecha de fuego (te prende unos segundos). Hay que romper su línea de visión o acabar con él.
 * Vida y daño salen de {@link DungeonBalance}.
 */
public class Chispero extends Monster {
    private static final EntityDataAccessor<Boolean> CHARGING =
            SynchedEntityData.defineId(Chispero.class, EntityDataSerializers.BOOLEAN);
    private static final float HITS = 2.0F;
    private static final float BASE_DAMAGE = 6.0F;
    private static final float SPEED = 1.6F;

    private float shotDamage = DungeonBalance.dmg(2, BASE_DAMAGE, 1);
    private int shootCd = 40;
    private int chargeTicks = 0;
    /** Solo para el dibujo: cuánto están abiertas las puertas del horno (0-1), su valor anterior y el retroceso de la tobera tras disparar. */
    public float open, openO, recoil;
    private boolean wasCharging;

    public Chispero(EntityType<? extends Chispero> type, Level level) {
        super(type, level);
        this.xpReward = 5;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(2, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 22.0D);
    }

    public void scaleTo(int stage, int players) {
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        shotDamage = DungeonBalance.dmg(stage, BASE_DAMAGE, players);
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
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.openO = this.open;
        boolean ch = isCharging();
        this.open += ((ch ? 1.0F : 0.0F) - this.open) * 0.3F;
        if (this.wasCharging && !ch && this.open > 0.5F) {
            this.recoil = 1.0F; // acaba de disparar
        }
        this.recoil = Math.max(0.0F, this.recoil - 0.12F);
        this.wasCharging = ch;
        if (this.level().isClientSide) {
            if (this.random.nextInt(ch ? 2 : 7) == 0) {
                this.level().addParticle(ch ? ParticleTypes.FLAME : ParticleTypes.SMOKE,
                        getX() + 0.12 + (this.random.nextDouble() - 0.5) * 0.15, getY() + 1.45, getZ() + (this.random.nextDouble() - 0.5) * 0.15,
                        0.0, 0.04, 0.0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive() || distanceToSqr(t) > 22.0 * 22.0 || !hasLineOfSight(t)) {
            this.entityData.set(CHARGING, false);
            chargeTicks = 0;
            if (shootCd > 20) {
                shootCd--;
            }
            return;
        }
        getLookControl().setLookAt(t, 40.0F, 40.0F);
        if (isCharging()) {
            chargeTicks++;
            if (chargeTicks % 4 == 0) {
                sl.sendParticles(ParticleTypes.FLAME, getX(), getEyeY(), getZ(), 3, 0.25, 0.25, 0.25, 0.01);
            }
            if (chargeTicks >= 22) {
                fire(sl, t);
                this.entityData.set(CHARGING, false);
                chargeTicks = 0;
                shootCd = 55 + random.nextInt(20);
            }
        } else if (--shootCd <= 0) {
            this.entityData.set(CHARGING, true);
            chargeTicks = 0;
            playSound(ModSounds.CHISPERO_CHARGE.get(), 1.0F, 1.0F);
        }
    }

    private void fire(ServerLevel sl, LivingEntity t) {
        Arrow arrow = new Arrow(sl, this);
        arrow.setPos(getX(), getEyeY() - 0.15, getZ());
        Vec3 d = t.getEyePosition().subtract(arrow.position());
        double flat = Math.hypot(d.x, d.z);
        arrow.shoot(d.x, d.y + flat * 0.04, d.z, SPEED, 1.5F);
        arrow.setBaseDamage(shotDamage / SPEED);
        arrow.setCritArrow(false);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        sl.addFreshEntity(arrow);
        Scheduler.runLater(100, arrow::discard);
        playSound(ModSounds.CHISPERO_SHOOT.get(), 1.2F, 0.9F + random.nextFloat() * 0.2F);
        sl.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getEyeY(), getZ(), 8, 0.2, 0.2, 0.2, 0.05);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            Shots.cancelOwned(sl, this);
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.8, getZ(), 8, 0.4, 0.6, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 14, 0.4, 0.6, 0.4, 0.04);
        }
        super.die(source);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CHISPERO_AMBIENT.get();
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CHISPERO_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CHISPERO_DEATH.get();
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
