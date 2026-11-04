package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.DungeonBalance;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.Blocks;

/**
 * Raizal: un gólem de raíces, lento y muy resistente. Pega fuerte con sus garras y, cada pocos segundos, levanta los brazos
 * (aviso de casi un segundo) y clava raíces en el suelo: quien esté cerca recibe daño y queda inmovilizado un rato. La versión
 * "Anciano" (élite) es más grande, aguanta casi el doble y se usa de guardián de la Cámara de la Llave.
 * Vida y daño salen de {@link DungeonBalance}.
 */
public class Raizal extends Monster {
    private static final EntityDataAccessor<Boolean> ROOTING = SynchedEntityData.defineId(Raizal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ELITE = SynchedEntityData.defineId(Raizal.class, EntityDataSerializers.BOOLEAN);
    private static final float HITS = 8.0F;
    private static final float BASE_DAMAGE = 9.0F;
    private static final float ROOT_DAMAGE = 6.0F;
    private static final float ROOT_RANGE = 6.5F;
    private static final int WINDUP = 18;

    private float rootDamage = DungeonBalance.dmg(1, ROOT_DAMAGE, 1);
    private int rootCd = 80;
    private int rootTicks = 0;

    public Raizal(EntityType<? extends Raizal> type, Level level) {
        super(type, level);
        this.xpReward = 18;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(1, BASE_DAMAGE, 1))
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Ajusta vida y daño a la etapa y a los jugadores; {@code elite} lo convierte en el Raizal Anciano. */
    public void scaleTo(int stage, int players, boolean elite) {
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players) * (elite ? 1.8F : 1.0F);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DungeonBalance.dmg(stage, BASE_DAMAGE, players) * (elite ? 1.15F : 1.0F));
        rootDamage = DungeonBalance.dmg(stage, ROOT_DAMAGE, players) * (elite ? 1.15F : 1.0F);
        this.entityData.set(ELITE, elite);
        if (elite) {
            setCustomName(net.minecraft.network.chat.Component.translatable("entity.mundos_de_axelis.raizal_elder"));
            setCustomNameVisible(true);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ROOTING, false);
        this.entityData.define(ELITE, false);
    }

    public boolean isRooting() {
        return this.entityData.get(ROOTING);
    }

    public boolean isElite() {
        return this.entityData.get(ELITE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (isRooting()) {
            getNavigation().stop();
            rootTicks++;
            LivingEntity t = getTarget();
            if (t != null) {
                getLookControl().setLookAt(t, 40.0F, 40.0F);
            }
            if (rootTicks % 3 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PODZOL.defaultBlockState()), getX(), getY() + 0.1, getZ(), 6,
                        ROOT_RANGE * 0.4, 0.05, ROOT_RANGE * 0.4, 0.02);
            }
            if (rootTicks >= WINDUP) {
                burst(sl);
                this.entityData.set(ROOTING, false);
                rootTicks = 0;
                rootCd = (isElite() ? 110 : 150) + random.nextInt(50);
            }
            return;
        }
        LivingEntity t = getTarget();
        if (t != null && t.isAlive()) {
            if (--rootCd <= 0 && distanceToSqr(t) < 9.0 * 9.0) {
                this.entityData.set(ROOTING, true);
                rootTicks = 0;
                playSound(ModSounds.RAIZAL_ROOTS.get(), 1.4F, 1.0F);
            }
        } else if (rootCd < 60) {
            rootCd++;
        }
    }

    private void burst(ServerLevel sl) {
        float range = isElite() ? ROOT_RANGE + 1.0F : ROOT_RANGE;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(range, 2.0, range), x -> x.isAlive() && !x.isSpectator() && !x.isCreative())) {
            if (p.distanceToSqr(this) > range * range) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), rootDamage);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
            p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), p.getX(), p.getY() + 0.2, p.getZ(), 16, 0.3, 0.2, 0.3, 0.1);
        }
        sl.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.3, getZ(), 24, range * 0.35, 0.2, range * 0.35, 0.05);
        playSound(ModSounds.GUARDIAN_SLAM.get(), 1.0F, 1.3F);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
        }
        return ok;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.RAIZAL_STEP.get(), 0.6F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), getX(), getY() + 1.0, getZ(), 30, 0.5, 0.8, 0.5, 0.1);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.0, getZ(), 18, 0.5, 0.8, 0.5, 0.02);
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
        return ModSounds.RAIZAL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RAIZAL_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("RootDamage", rootDamage);
        tag.putBoolean("Elite", isElite());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("RootDamage")) {
            rootDamage = tag.getFloat("RootDamage");
        }
        this.entityData.set(ELITE, tag.getBoolean("Elite"));
    }
}
