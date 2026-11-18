package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.DungeonBalance;
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
 * Brote: un retoño del bosque que corre hacia ti y te pega con sus enredaderas. Es el mob común de la dungeon del Rey Brote.
 * Su vida y su daño salen de {@link DungeonBalance} (se ajustan al entrar en la sala con {@link #scaleTo}).
 */
public class Brote extends Monster {
    /** Etiqueta que llevan todos los mobs de la dungeon (la sala la guarda el dato {@code mdc_room}). */
    public static final String DUNGEON_TAG = "mdc_dungeon_mob";
    /** Golpes de espada B que aguanta y daño base (etapa 1). */
    private static final float HITS = 2.5F;
    private static final float BASE_DAMAGE = 6.5F;

    public Brote(EntityType<? extends Brote> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(1, BASE_DAMAGE, 1))
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    /** Ajusta vida y daño a la etapa y a los jugadores de la dungeon. */
    public void scaleTo(int stage, int players) {
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DungeonBalance.dmg(stage, BASE_DAMAGE, players));
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
            playSound(ModSounds.BROTE_ATTACK.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 0.8, getZ(), 18, 0.4, 0.5, 0.4, 0.02);
            sl.sendParticles(ParticleTypes.COMPOSTER, getX(), getY() + 0.8, getZ(), 12, 0.4, 0.5, 0.4, 0.05);
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
    protected SoundEvent getAmbientSound() {
        return ModSounds.BROTE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BROTE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BROTE_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
    }
}
