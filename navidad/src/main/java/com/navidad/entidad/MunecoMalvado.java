package com.navidad.entidad;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Pose;
import com.axeliscore.character.CoreMob;
import com.axeliscore.combat.Proyectil;
import com.navidad.Temporada;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

/**
 * El muñeco de nieve malvado: sale de noche en lugares nevados durante la temporada navideña (o con huevo/comando). Te avienta bolas de
 * nieve que pegan y congelan, se ríe cuando te da y al morir se le cae la cabeza (y a veces suelta la campana del Krampus).
 */
public class MunecoMalvado extends CoreMob implements RangedAttackMob {
    private LivingEntity lanzarA;
    private int lanzarEn = -1;

    public MunecoMalvado(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 18.0).add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    /** Sale solo de noche (o en lugares oscuros) y solo en temporada navideña. */
    public static boolean puedeSalir(EntityType<MunecoMalvado> t, ServerLevelAccessor l, MobSpawnType why, BlockPos p, RandomSource r) {
        if (why == MobSpawnType.NATURAL || why == MobSpawnType.CHUNK_GENERATION) {
            if (l.getServer() == null || !Temporada.activa(l.getServer())) {
                return false;
            }
        }
        return Monster.isDarkEnoughToSpawn(l, p, r) && Mob.checkMobSpawnRules(t, l, why, p, r);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 45, 14F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void performRangedAttack(LivingEntity t, float fuerza) {
        // la bola sale cuando el brazo termina de girar (evento "lanzar" de la animación, ~8 ticks)
        playAction("attack");
        lanzarA = t;
        lanzarEn = tickCount + 8;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level() instanceof ServerLevel && lanzarEn == tickCount && lanzarA != null && lanzarA.isAlive()) {
            Vec3 desde = position().add(0, 1.0, 0).add(Vec3.directionFromRotation(0, getYRot() + 40).scale(0.5));
            Proyectil.hacia(level(), Proyectiles.BOLA_NIEVE, this, desde, lanzarA, 1.0F);
            lanzarA = null;
        }
        // en tierra caliente se derrite
        if (!level().isClientSide && tickCount % 40 == 0 && level().getBiome(blockPosition()).value().getBaseTemperature() > 1.0F) {
            hurt(damageSources().onFire(), 1.0F);
        }
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity e) {
        boolean ok = super.doHurtTarget(e);
        if (ok) {
            playAction("laugh");
        }
        return ok;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        if (axDeath(h, "action")) {
            return;
        }
        axActions(h, n -> "action");
        axHurt(h, "action");
        axMove(h, 0.2F, 99F, 0.9F, 1F);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
        if (deathTime <= 0) {
            axLook(h, pose, pt, "head", "torso", 0.7F);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.SNOW_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SNOW_GOLEM_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SNOW_GOLEM_AMBIENT;
    }

    @Override
    protected void playStepSound(BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        playSound(SoundEvents.SNOW_STEP, 0.5F, 1.0F);
    }
}
