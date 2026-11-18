package com.mundosdeaxelis.entity;

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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Armadura encantada (armería secreta de la Torre): parece una armadura de hierro vacía puesta de adorno. Cuando alguien se acerca
 * a menos de 5 bloques (o la golpea) despierta: ruido de metal, le brillan los ojos y ataca, lenta pero dura y difícil de empujar.
 */
public class ArmaduraEncantada extends Monster {
    private static final EntityDataAccessor<Boolean> AWAKE = SynchedEntityData.defineId(ArmaduraEncantada.class, EntityDataSerializers.BOOLEAN);
    private static final double WAKE_RANGE = 5.0D;

    public ArmaduraEncantada(EntityType<? extends ArmaduraEncantada> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        dress();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 20.0D);
    }

    /** Armadura de hierro completa y espada (no se sueltan: el botín va en su tabla). */
    private void dress() {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        for (EquipmentSlot s : EquipmentSlot.values()) {
            setDropChance(s, 0.0F);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AWAKE, false);
    }

    public boolean isAwake() {
        return this.entityData.get(AWAKE);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true) {
            @Override
            public boolean canUse() {
                return isAwake() && super.canUse();
            }
        });
    }

    private void wake(ServerLevel sl) {
        if (isAwake()) {
            return;
        }
        this.entityData.set(AWAKE, true);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.ARMOR_EQUIP_IRON, SoundSource.HOSTILE, 1.4F, 0.6F);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.HOSTILE, 1.0F, 0.7F);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 1.7, getZ(), 10, 0.2, 0.1, 0.2, 0.02);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (!isAwake()) {
            getNavigation().stop();
            setTarget(null);
            if (this.tickCount % 5 == 0) {
                Player p = sl.getNearestPlayer(getX(), getY(), getZ(), WAKE_RANGE, pl -> !((Player) pl).isCreative() && !pl.isSpectator());
                if (p != null && hasLineOfSight(p)) {
                    wake(sl);
                }
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide && isAwake() && this.tickCount % 3 == 0) {
            // los ojos: dos chispitas azules dentro del yelmo
            double yaw = Math.toRadians(this.yHeadRot);
            double fx = -Math.sin(yaw) * 0.32, fz = Math.cos(yaw) * 0.32;
            double sx = Math.cos(yaw) * 0.11, sz = Math.sin(yaw) * 0.11;
            double y = getY() + getEyeHeight();
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + fx + sx, y, getZ() + fz + sz, 0, 0.003, 0);
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + fx - sx, y, getZ() + fz - sz, 0, 0.003, 0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!isAwake() && this.level() instanceof ServerLevel sl && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            wake(sl);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return isAwake() && super.isPushable();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Awake", isAwake());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(AWAKE, tag.getBoolean("Awake"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAwake() ? SoundEvents.CHAIN_STEP : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.CHAIN_STEP, 0.6F, 0.6F);
    }
}
