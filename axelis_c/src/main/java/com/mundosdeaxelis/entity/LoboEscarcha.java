package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.Cold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lobezno de escarcha: lo invoca el Lobo Ártico con su aullido (y sirve de enemigo de manada en la dungeon del hielo).
 * Frágil pero rápido; cada mordisco enfría. Hay que repartirse y quitárselos de encima antes de que te rodeen.
 */
public class LoboEscarcha extends Monster {
    public LoboEscarcha(EntityType<? extends LoboEscarcha> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.38D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D * BossBalance.dmgMul(LoboArtico.STAGE))
                .add(Attributes.ARMOR, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** El jefe les ajusta el mordisco al daño que le toca (según los jugadores que haya). */
    public void boost(float dmgNow) {
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0D * dmgNow);
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.getTags().contains(LoboArtico.MINION_TAG) && super.removeWhenFarAway(distance);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            playSound(ModSounds.LOBO_PUP_BITE.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
            if (target instanceof ServerPlayer p) {
                Cold.add(p, 6.0F);
            }
        }
        return ok;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LOBO_PUP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LOBO_PUP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LOBO_PUP_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(net.minecraft.sounds.SoundEvents.SNOW_STEP, 0.5F, 1.1F);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.6, getZ(), 24, 0.4, 0.4, 0.4, 0.12);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), getX(), getY() + 0.5, getZ(), 14, 0.3, 0.3, 0.3, 0.2);
        }
        super.die(source);
    }
}
