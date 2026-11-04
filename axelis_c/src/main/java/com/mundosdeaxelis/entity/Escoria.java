package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Escoria: un montón de roca fundida que corre hacia ti y te pega con sus puños al rojo. Es el mob común de la dungeon del Gran Herrero; al morir solo deja unas brasas decorativas (no hacen daño).
 * Su vida y su daño salen de {@link DungeonBalance} (se ajustan al entrar en la sala con {@link #scaleTo}).
 */
public class Escoria extends Monster {
    /** Golpes de espada B que aguanta y daño base (etapa 1). */
    private static final float HITS = 2.5F;
    private static final float BASE_DAMAGE = 6.5F;
    private static final float PUDDLE_DAMAGE = 1.5F;
    private float puddleDamage = DungeonBalance.dmg(2, PUDDLE_DAMAGE, 1) ;

    public Escoria(EntityType<? extends Escoria> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(2, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(2, BASE_DAMAGE, 1))
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Ajusta vida y daño a la etapa y a los jugadores de la dungeon. */
    public void scaleTo(int stage, int players) {
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DungeonBalance.dmg(stage, BASE_DAMAGE, players));
        puddleDamage = DungeonBalance.dmg(stage, PUDDLE_DAMAGE, players);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok) {
            playSound(ModSounds.ESCORIA_ATTACK.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 0.8, getZ(), 10, 0.4, 0.5, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.8, getZ(), 12, 0.4, 0.5, 0.4, 0.03);
            puddle(sl, new Vec3(getX(), getY() + 0.1, getZ()));
        }
        super.die(source);
    }

    /** Restos de brasas al morir: SOLO decoración (humo y chispas un instante). Un enemigo muerto no hace daño. */
    private void puddle(ServerLevel sl, Vec3 at) {
        for (int k = 0; k < 3; k++) {
            Scheduler.runLater(6 + k * 8, () -> {
                sl.sendParticles(ParticleTypes.SMALL_FLAME, at.x, at.y + 0.1, at.z, 5, 0.5, 0.05, 0.5, 0.01);
                sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.2, at.z, 3, 0.4, 0.1, 0.4, 0.01);
            });
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.tickCount % 7 == 0) {
                this.level().addParticle(ParticleTypes.DRIPPING_LAVA, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 0.45, getZ() + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
            }
            if (this.tickCount % 13 == 0) {
                this.level().addParticle(ParticleTypes.SMALL_FLAME, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + 1.4, getZ() + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.02, 0.0);
            }
        }
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.ESCORIA_STEP.get(), 0.45F, 0.9F + random.nextFloat() * 0.3F);
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ESCORIA_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ESCORIA_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ESCORIA_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("PuddleDamage", puddleDamage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PuddleDamage")) {
            puddleDamage = tag.getFloat("PuddleDamage");
        }
    }
}
