package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.world.Cold;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.Blocks;

/**
 * Centinela de escarcha (etapa 258): soldado con armadura cubierta de hielo que guarda el Fortín de Escarcha. Pelea con espada;
 * cada golpe enfría y frena. Si te quedas cerca, cada tanto clava la espada en el suelo y suelta una onda helada a su alrededor:
 * antes se agacha un instante con un crujido y escarcha en el suelo (aviso), así se puede esquivar alejándose.
 */
public class CentinelaEscarcha extends Monster {
    private int stompCd = 120;
    private int stompWarn = -1;

    public CentinelaEscarcha(EntityType<? extends CentinelaEscarcha> type, Level level) {
        super(type, level);
        this.xpReward = 8;
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.26D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false) {
            @Override
            public boolean canUse() {
                return stompWarn < 0 && super.canUse();
            }
        });
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6D));
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

    /** ¿Está preparando la onda helada? (el cuerpo se agacha; lo usa el dibujo). */
    public boolean charging() {
        return stompWarn >= 0;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 0), this);
            if (target instanceof ServerPlayer p) {
                Cold.add(p, 5.0F);
            }
            le.setTicksFrozen(Math.min(le.getTicksRequiredToFreeze(), le.getTicksFrozen() + 40));
            playSound(SoundEvents.GLASS_HIT, 0.8F, 1.4F + this.random.nextFloat() * 0.2F);
        }
        return ok;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        if (getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD)); // puesto con la estructura
        }
        if (tickCount % 6 == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 1.2, getZ(), 1, 0.3, 0.5, 0.3, 0.0);
        }
        LivingEntity t = getTarget();
        if (stompWarn >= 0) {
            getNavigation().stop();
            if (stompWarn % 4 == 0) {
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState()),
                        getX(), getY() + 0.1, getZ(), 10, 1.4, 0.05, 1.4, 0.05);
            }
            if (--stompWarn < 0) {
                stomp(sl);
            }
            return;
        }
        if (t == null || !t.isAlive() || --stompCd > 0) {
            return;
        }
        if (distanceToSqr(t) < 4.5 * 4.5) {
            stompCd = 140 + this.random.nextInt(80);
            stompWarn = 16;
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 1.5F, 0.6F);
            sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 0.5F, 1.8F);
        } else {
            stompCd = 20;
        }
    }

    /** Onda helada: daña poco, frena mucho y empuja un poco a quien esté a menos de 4 bloques. */
    private void stomp(ServerLevel sl) {
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 1.0F, 0.8F);
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI * 2 / 24;
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * 1.5, getY() + 0.2, getZ() + Math.sin(a) * 1.5, 2,
                    Math.cos(a) * 0.3, 0.05, Math.sin(a) * 0.3, 0.15);
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), getX(), getY() + 0.3, getZ(), 40, 2.0, 0.2, 2.0, 0.2);
        List<Player> ps = sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(4.0, 1.5, 4.0), p -> p.isAlive() && !p.isSpectator());
        for (Player p : ps) {
            if (p.distanceToSqr(this) > 4.0 * 4.0 || p.isCreative()) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), 3.0F);
            p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2), this);
            p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze() + 20, p.getTicksFrozen() + 80));
            if (p instanceof ServerPlayer sp) {
                Cold.add(sp, 8.0F);
            }
            double dx = p.getX() - getX(), dz = p.getZ() - getZ(), d = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
            p.push(dx / d * 0.5, 0.25, dz / d * 0.5);
            p.hurtMarked = true;
        }
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.STRAY_STEP, 0.15F, 1.0F);
    }
}
