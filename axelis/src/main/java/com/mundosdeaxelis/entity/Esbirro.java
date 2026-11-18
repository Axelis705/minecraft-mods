package com.mundosdeaxelis.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * Esbirro del Archimago (Torre): guardián encapuchado con daga. Pelea cuerpo a cuerpo y, si te alejas, cada tanto se
 * teletransporta a tu lado con un destello morado (aviso: humo y sonido un instante antes).
 */
public class Esbirro extends Monster {
    private int blinkCd = 100;
    private int blinkWarn = -1;
    private Vec3 blinkTo;

    public Esbirro(EntityType<? extends Esbirro> type, Level level) {
        super(type, level);
        this.xpReward = 6;
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 22.0D)
                .add(Attributes.ATTACK_DAMAGE, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1D, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        SpawnGroupData data, net.minecraft.nbt.CompoundTag tag) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data, tag);
        populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        return out;
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD)); // puesto con la estructura, sin finalizeSpawn
        }
        LivingEntity t = getTarget();
        if (blinkWarn >= 0) {
            if (--blinkWarn <= 0 && blinkTo != null) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.0, getZ(), 30, 0.3, 0.6, 0.3, 0.05);
                teleportTo(blinkTo.x, blinkTo.y, blinkTo.z);
                sl.sendParticles(ParticleTypes.WITCH, blinkTo.x, blinkTo.y + 1.0, blinkTo.z, 20, 0.3, 0.6, 0.3, 0.05);
                sl.playSound(null, blinkTo.x, blinkTo.y, blinkTo.z, com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_BLINK.get(),
                        SoundSource.HOSTILE, 1.0F, 1.4F);
                blinkTo = null;
                blinkWarn = -1;
            }
            return;
        }
        if (t == null || !t.isAlive() || --blinkCd > 0) {
            return;
        }
        blinkCd = 120 + this.random.nextInt(80);
        if (distanceToSqr(t) < 7.0 * 7.0 || !hasLineOfSight(t)) {
            return;
        }
        // un sitio libre a 2 bloques del objetivo, a su costado
        for (int k = 0; k < 6; k++) {
            double ang = this.random.nextDouble() * Math.PI * 2;
            Vec3 p = new Vec3(t.getX() + Math.cos(ang) * 2.0, t.getY(), t.getZ() + Math.sin(ang) * 2.0);
            if (sl.noCollision(this, getBoundingBox().move(p.subtract(position())))
                    && !sl.getBlockState(net.minecraft.core.BlockPos.containing(p).below()).isAir()) {
                blinkTo = p;
                blinkWarn = 12;
                sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1.0, getZ(), 12, 0.3, 0.5, 0.3, 0.01);
                sl.playSound(null, getX(), getY(), getZ(), com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_CHARGE.get(),
                        SoundSource.HOSTILE, 0.6F, 1.6F);
                return;
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VINDICATOR_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VINDICATOR_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VINDICATOR_DEATH;
    }
}
