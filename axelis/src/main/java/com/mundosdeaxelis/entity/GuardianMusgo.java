package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Guardián de Musgo: el mini-jefe de la dungeon del Rey Brote. Un gólem de piedra con un arbolito en el hombro: camina lento
 * hacia ti y te pega fuerte; levanta los puños para un golpe de suelo con aviso (se esquiva saltando); con poca vida echa
 * raíces bajo tus pies y llama a Brotes. Vida y daño salen de {@link DungeonBalance}.
 */
public class GuardianMusgo extends Monster {
    private static final EntityDataAccessor<Integer> SLAM =
            SynchedEntityData.defineId(GuardianMusgo.class, EntityDataSerializers.INT);
    private static final float HITS = 28.0F;
    private static final float MELEE = 10.0F;
    /** Alcance del golpe cuerpo a cuerpo (distancia al centro) y radio del golpe de suelo. */
    private static final double REACH = 2.2, SLAM_RADIUS = 4.2;
    private static final float SLAM_DAMAGE = 15.0F;
    private static final int SLAM_CHARGE = 28;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Guardián de Musgo"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    private int stage = 1;
    private int players = 1;
    private float slamDamage = DungeonBalance.dmg(1, SLAM_DAMAGE, 1);
    private float thornDamage = DungeonBalance.dmg(1, 7.0F, 1);
    private int slamCd = 100;
    private int thornCd = 200;
    private int summonCd = 260;
    private boolean enraged = false;
    private int slamTicks = -1;

    public GuardianMusgo(EntityType<? extends GuardianMusgo> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        setPersistenceRequired();
        setCustomName(Component.literal("Guardián de Musgo"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(1, MELEE, 1))
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    public void scaleTo(int stage, int players) {
        this.stage = stage;
        this.players = players;
        float hp = DungeonBalance.hp(stage, HITS) * DungeonBalance.hpPlayers(players);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);
        setHealth(hp);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(DungeonBalance.dmg(stage, MELEE, players));
        slamDamage = DungeonBalance.dmg(stage, SLAM_DAMAGE, players);
        thornDamage = DungeonBalance.dmg(stage, 7.0F, players);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SLAM, 0);
    }

    /** Avance del golpe de suelo (0 = nada, 1 = puños arriba y a punto de caer), para animar el modelo. */
    public float slamProgress() {
        return Mth.clamp(this.entityData.get(SLAM) / (float) SLAM_CHARGE, 0.0F, 1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // alcance corto: solo golpea cuando ya está pegado al jugador (antes llegaba a casi 4 bloques por su tamaño)
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            protected double getAttackReachSqr(net.minecraft.world.entity.LivingEntity target) {
                return REACH * REACH + target.getBbWidth();
            }
        });
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof net.minecraft.world.entity.LivingEntity le) {
            Vec3 d = new Vec3(le.getX() - getX(), 0, le.getZ() - getZ());
            d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
            le.setDeltaMovement(le.getDeltaMovement().add(d.x * 0.9, 0.4, d.z * 0.9));
            le.hurtMarked = true;
            playSound(ModSounds.GUARDIAN_SLAM.get(), 0.7F, 1.4F);
        }
        return ok;
    }

    private List<ServerPlayer> playersNear(ServerLevel sl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceTo(this) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl) || !isAlive()) {
            return;
        }
        this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        if (!enraged && getHealth() <= getMaxHealth() * 0.5F) {
            enraged = true;
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            playSound(ModSounds.GUARDIAN_ROAR.get(), 3.0F, 1.0F);
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xFFC93C, 8.0, 18);
            RelicFx.shakeNear(sl, position(), 20, 1.0F, 12);
            slamCd = Math.min(slamCd, 30);
            summonCd = Math.min(summonCd, 40);
            for (ServerPlayer p : playersNear(sl, 80.0)) {
                p.displayClientMessage(Component.literal("¡El Guardián se enfurece!").withStyle(net.minecraft.ChatFormatting.GOLD), true);
            }
        }
        net.minecraft.world.entity.LivingEntity t = getTarget();
        if (t == null) {
            slamTicks = -1;
            this.entityData.set(SLAM, 0);
            return;
        }
        // golpe de suelo con aviso
        if (slamTicks >= 0) {
            slamTicks++;
            this.entityData.set(SLAM, slamTicks);
            getNavigation().stop();
            if (slamTicks == 1) {
                playSound(ModSounds.GUARDIAN_CHARGE.get(), 2.5F, 1.0F);
            }
            if (slamTicks >= SLAM_CHARGE) {
                slam(sl);
                slamTicks = -1;
                this.entityData.set(SLAM, 0);
                slamCd = enraged ? 90 : 140;
            }
            return;
        }
        if (--slamCd <= 0 && distanceTo(t) < 7.0F) {
            slamTicks = 0;
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xFFB03C, SLAM_RADIUS, SLAM_CHARGE);
        }
        if (enraged) {
            if (--thornCd <= 0) {
                thorns(sl);
                thornCd = 190;
            }
            if (--summonCd <= 0) {
                summon(sl, t);
                summonCd = 320;
            }
        }
    }

    private void slam(ServerLevel sl) {
        Vec3 at = position();
        double radius = SLAM_RADIUS;
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xE8D08A, radius + 0.5, 14);
        Vfx.flash(sl, at.add(0, 0.6, 0), 0xE8D08A, 4.0, 10);
        sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, at.x, at.y + 0.2, at.z, 14, radius * 0.4, 0.1, radius * 0.4, 0.02);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, at.x, at.y + 0.2, at.z, 30, radius * 0.4, 0.1, radius * 0.4, 0.1);
        playSound(ModSounds.GUARDIAN_SLAM.get(), 3.0F, 1.0F);
        RelicFx.shakeNear(sl, at, 18, 1.0F, 10);
        for (ServerPlayer p : playersNear(sl, radius + 2.0)) {
            if (Math.hypot(p.getX() - getX(), p.getZ() - getZ()) > radius || p.getY() - getY() > 1.3) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), slamDamage);
            Vec3 d = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
            d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
            p.setDeltaMovement(p.getDeltaMovement().add(d.x * 1.1, 0.55, d.z * 1.1));
            p.hurtMarked = true;
        }
    }

    /** Raíces: brotan bajo cada jugador después de un aviso (se esquivan saliendo del círculo). */
    private void thorns(ServerLevel sl) {
        for (ServerPlayer p : playersNear(sl, 30.0)) {
            Vec3 spot = p.position();
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0x2E8B3A, 1.9, 22);
            sl.playSound(null, spot.x, spot.y, spot.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
            Scheduler.runLater(22, () -> {
                sl.sendParticles(ParticleTypes.COMPOSTER, spot.x, spot.y + 0.5, spot.z, 36, 0.8, 0.8, 0.8, 0.1);
                sl.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
                for (ServerPlayer q : sl.players()) {
                    if (q.isAlive() && Math.hypot(q.getX() - spot.x, q.getZ() - spot.z) <= 1.9 && Math.abs(q.getY() - spot.y) < 2.5) {
                        q.hurt(damageSources().mobAttack(this), thornDamage);
                        q.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3, false, true));
                    }
                }
            });
        }
    }

    private void summon(ServerLevel sl, net.minecraft.world.entity.LivingEntity target) {
        AABB box = getBoundingBox().inflate(40.0);
        int have = sl.getEntitiesOfClass(Brote.class, box, b -> b.isAlive() && b.getTags().contains(Brote.DUNGEON_TAG)).size();
        int count = Math.min(2, 4 - have);
        for (int i = 0; i < count; i++) {
            Brote b = ModEntities.BROTE.get().create(sl);
            if (b == null) {
                return;
            }
            double a = random.nextDouble() * Math.PI * 2;
            b.moveTo(getX() + Math.cos(a) * 3.0, getY(), getZ() + Math.sin(a) * 3.0, random.nextFloat() * 360.0F, 0.0F);
            b.scaleTo(stage, players);
            b.addTag(Brote.DUNGEON_TAG);
            b.setPersistenceRequired();
            b.setTarget(target instanceof Player ? (Player) target : null);
            sl.addFreshEntity(b);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, b.getX(), b.getY() + 0.8, b.getZ(), 14, 0.3, 0.5, 0.3, 0.02);
        }
        if (count > 0) {
            playSound(SoundEvents.AZALEA_LEAVES_BREAK, 2.0F, 0.5F);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            this.bossEvent.removeAllPlayers();
            Vec3 at = position();
            Vfx.ring(sl, at.add(0, 0.1, 0), 0xE8D08A, 7.0, 20);
            sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.5, at.z, 40, 1.2, 1.2, 1.2, 0.1);
            spawnAtLocation(new ItemStack(Items.MOSS_BLOCK, 3 + random.nextInt(4)));
            spawnAtLocation(new ItemStack(Items.MOSSY_COBBLESTONE, 2 + random.nextInt(4)));
        }
        super.die(source);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
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
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.GUARDIAN_STEP.get(), 0.7F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GUARDIAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GUARDIAN_DEATH.get();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Stage", stage);
        tag.putInt("Players", players);
        tag.putBoolean("Enraged", enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Stage")) {
            stage = tag.getInt("Stage");
            players = tag.getInt("Players");
            enraged = tag.getBoolean("Enraged");
            slamDamage = DungeonBalance.dmg(stage, SLAM_DAMAGE, players);
            thornDamage = DungeonBalance.dmg(stage, 7.0F, players);
        }
    }
}
