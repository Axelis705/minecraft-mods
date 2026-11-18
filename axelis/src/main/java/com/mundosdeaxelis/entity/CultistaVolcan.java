package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cultista del volcán (etapa 4): encapuchado con máscara de obsidiana y bastón con un cristal de magma. Prefiere mantenerse a
 * distancia: levanta los brazos (aviso) y bajo tus pies aparece un círculo con una runa; si no te apartas, sale una columna de
 * fuego que quema y te levanta. Una vez, cuando está herido, hace una ofrenda y del suelo salen dos larvas de magma. Si lo
 * acorralas, golpea con el bastón.
 */
public class CultistaVolcan extends Monster {
    private static final EntityDataAccessor<Integer> CAST = SynchedEntityData.defineId(CultistaVolcan.class, EntityDataSerializers.INT);
    private static final int PILLAR_TICKS = 28;
    private static final int OFFERING_TICKS = 36;
    private static final float PILLAR_DAMAGE = 6.0F;

    private int castCd = 40;
    private int castLen;
    private boolean offering;
    private boolean offered;
    private int meleeCd;
    private int moveCd;

    public CultistaVolcan(EntityType<? extends CultistaVolcan> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 24.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAST, 0);
    }

    /** 0..1: cuánto tiene levantados los brazos (está lanzando un conjuro). */
    public float cast(float pt) {
        int c = this.entityData.get(CAST);
        if (c <= 0) {
            return 0F;
        }
        return Math.min(1F, Math.min((c + pt) / 8F, 1F));
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Offered", offered);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.offered = tag.getBoolean("Offered");
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (meleeCd > 0) {
            meleeCd--;
        }
        LivingEntity t = getTarget();
        int c = this.entityData.get(CAST);
        if (c > 0) {
            this.entityData.set(CAST, c + 1);
            getNavigation().stop();
            if (t != null) {
                getLookControl().setLookAt(t, 30.0F, 30.0F);
            }
            Vec3 top = position().add(0, 2.3, 0);
            if (c % 4 == 0) {
                sl.sendParticles(ParticleTypes.FLAME, top.x, top.y, top.z, 3, 0.15, 0.15, 0.15, 0.02);
            }
            if (offering && c % 8 == 0) {
                sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.1, getZ(), 3, 1.2, 0.0, 1.2, 0.0);
            }
            if (c >= castLen) {
                this.entityData.set(CAST, 0);
                if (offering) {
                    summonLarvae(sl);
                    offering = false;
                }
                castCd = 90 + this.random.nextInt(40);
            }
            return;
        }
        if (t == null || !t.isAlive()) {
            return;
        }
        getLookControl().setLookAt(t, 30.0F, 30.0F);
        double dist = distanceTo(t);
        // la ofrenda: una vez, al quedar por debajo del 60 % de vida
        if (!offered && getHealth() < getMaxHealth() * 0.6F) {
            offered = true;
            offering = true;
            castLen = OFFERING_TICKS;
            this.entityData.set(CAST, 1);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.2F, 0.7F);
            return;
        }
        // acorralado: bastonazo
        if (dist < 2.3 && meleeCd <= 0) {
            meleeCd = 30;
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            doHurtTarget(t);
        }
        // guarda la distancia: se aleja si te acercas, se acerca si te alejas
        if (--moveCd <= 0) {
            moveCd = 15;
            if (dist < 6.0) {
                Vec3 away = position().subtract(t.position()).multiply(1, 0, 1);
                away = away.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : away.normalize();
                getNavigation().moveTo(getX() + away.x * 6, getY(), getZ() + away.z * 6, 1.15D);
            } else if (dist > 13.0 || !hasLineOfSight(t)) {
                getNavigation().moveTo(t, 1.0D);
            } else {
                getNavigation().stop();
            }
        }
        if (--castCd <= 0 && dist < 18.0 && hasLineOfSight(t)) {
            offering = false;
            castLen = PILLAR_TICKS;
            this.entityData.set(CAST, 1);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 1.2F, 0.7F);
            // el aviso sale ya bajo los pies del objetivo (y de otro jugador cercano)
            VolcanFx.pillar(sl, this, t.position(), PILLAR_TICKS, PILLAR_DAMAGE, 1.9);
            for (Player p : sl.getEntitiesOfClass(Player.class, t.getBoundingBox().inflate(8.0))) {
                if (p != t && !p.isCreative() && !p.isSpectator() && this.random.nextBoolean()) {
                    VolcanFx.pillar(sl, this, p.position(), PILLAR_TICKS + 6, PILLAR_DAMAGE, 1.9);
                    break;
                }
            }
            Vec3 top = position().add(0, 2.3, 0);
            sl.playSound(null, top.x, top.y, top.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 0.6F);
        }
    }

    private void summonLarvae(ServerLevel sl) {
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2F, 0.5F);
        for (int i = 0; i < 2; i++) {
            LarvaMagma l = ModEntities.LARVA_MAGMA.get().create(sl);
            if (l == null) {
                continue;
            }
            double a = this.random.nextDouble() * Math.PI * 2;
            double x = getX() + Math.cos(a) * 1.8, z = getZ() + Math.sin(a) * 1.8;
            l.moveTo(x, getY(), z, (float) Math.toDegrees(a), 0.0F);
            l.scaleTo(12.0F, 3.0F);
            if (getTarget() != null) {
                l.setTarget(getTarget());
            }
            sl.addFreshEntity(l);
            sl.sendParticles(ParticleTypes.LAVA, x, getY() + 0.2, z, 8, 0.3, 0.1, 0.3, 0.0);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, x, getY() + 0.2, z, 8, 0.3, 0.2, 0.3, 0.02);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            target.setSecondsOnFire(2);
        }
        return ok;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && this.random.nextInt(8) == 0) {
            this.level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (this.random.nextDouble() - 0.5) * 0.6, getY() + 0.1,
                    getZ() + (this.random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1.0, getZ(), 24, 0.3, 0.6, 0.3, 0.04);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.EVOKER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.EVOKER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.EVOKER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.75F + this.random.nextFloat() * 0.1F;
    }
}
