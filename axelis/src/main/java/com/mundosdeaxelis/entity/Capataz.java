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
 * Capataz: el mini-jefe de la dungeon del Gran Herrero. Un supervisor acorazado con un martillo enorme: camina lento hacia ti y
 * te pega fuerte; levanta el martillo para un golpe de suelo con aviso (se esquiva saltando); con poca vida hace brotar brasas
 * bajo tus pies y llama a Escorias. Vida y daño salen de {@link DungeonBalance}.
 */
public class Capataz extends Monster {
    private static final EntityDataAccessor<Integer> SLAM =
            SynchedEntityData.defineId(Capataz.class, EntityDataSerializers.INT);
    private static final float HITS = 28.0F;
    private static final float MELEE = 10.0F;
    /** Alcance del golpe cuerpo a cuerpo (distancia al centro) y radio del golpe de suelo. */
    private static final double REACH = 2.2, SLAM_RADIUS = 4.2;
    private static final float SLAM_DAMAGE = 15.0F;
    private static final int SLAM_CHARGE = 28;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Capataz"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    private int stage = 2;
    private int players = 1;
    private float slamDamage = DungeonBalance.dmg(2, SLAM_DAMAGE, 1);
    private float emberDamage = DungeonBalance.dmg(2, 7.0F, 1);
    private int slamCd = 100;
    private int emberCd = 200;
    private int summonCd = 260;
    private boolean enraged = false;
    private int slamTicks = -1;

    public Capataz(EntityType<? extends Capataz> type, Level level) {
        super(type, level);
        this.xpReward = 40;
        setPersistenceRequired();
        setCustomName(Component.literal("Capataz"));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(2, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(2, MELEE, 1))
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
        emberDamage = DungeonBalance.dmg(stage, 7.0F, players);
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
            playSound(ModSounds.CAPATAZ_SLAM.get(), 0.7F, 1.4F);
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
            playSound(ModSounds.CAPATAZ_ROAR.get(), 3.0F, 1.0F);
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xFF7A1E, 8.0, 18);
            RelicFx.shakeNear(sl, position(), 20, 1.0F, 12);
            slamCd = Math.min(slamCd, 30);
            summonCd = Math.min(summonCd, 40);
            for (ServerPlayer p : playersNear(sl, 80.0)) {
                p.displayClientMessage(Component.literal("¡El Capataz se enfurece!").withStyle(net.minecraft.ChatFormatting.GOLD), true);
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
                playSound(ModSounds.CAPATAZ_CHARGE.get(), 2.5F, 1.0F);
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
            if (--emberCd <= 0) {
                embers(sl);
                emberCd = 190;
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
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xFF8A2A, radius + 0.5, 14);
        Vfx.flash(sl, at.add(0, 0.6, 0), 0xFFB060, 4.0, 10);
        sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 16, radius * 0.4, 0.1, radius * 0.4, 0.02);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 40, radius * 0.4, 0.1, radius * 0.4, 0.08);
        playSound(ModSounds.CAPATAZ_SLAM.get(), 3.0F, 1.0F);
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

    /** Brasas: estallan bajo cada jugador después de un aviso (se esquivan saliendo del círculo). */
    private void embers(ServerLevel sl) {
        for (ServerPlayer p : playersNear(sl, 30.0)) {
            Vec3 spot = p.position();
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0xFF5A1E, 1.9, 22);
            sl.playSound(null, spot.x, spot.y, spot.z, ModSounds.CAPATAZ_BURST.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
            Scheduler.runLater(22, () -> {
                sl.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.5, spot.z, 40, 0.8, 0.8, 0.8, 0.08);
                sl.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.5, spot.z, 14, 0.8, 0.5, 0.8, 0.0);
                sl.playSound(null, spot.x, spot.y, spot.z, ModSounds.HERRERO_BLAST.get(), SoundSource.HOSTILE, 0.8F, 1.5F);
                for (ServerPlayer q : sl.players()) {
                    if (q.isAlive() && Math.hypot(q.getX() - spot.x, q.getZ() - spot.z) <= 1.9 && Math.abs(q.getY() - spot.y) < 2.5) {
                        q.hurt(damageSources().mobAttack(this), emberDamage);
                    }
                }
            });
        }
    }

    private void summon(ServerLevel sl, net.minecraft.world.entity.LivingEntity target) {
        AABB box = getBoundingBox().inflate(40.0);
        int have = sl.getEntitiesOfClass(Escoria.class, box, b -> b.isAlive() && b.getTags().contains(Brote.DUNGEON_TAG)).size();
        int count = Math.min(2, 4 - have);
        for (int i = 0; i < count; i++) {
            Escoria b = ModEntities.ESCORIA.get().create(sl);
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
            sl.sendParticles(ParticleTypes.LAVA, b.getX(), b.getY() + 0.8, b.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
        if (count > 0) {
            playSound(SoundEvents.LAVA_POP, 2.0F, 0.5F);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            this.bossEvent.removeAllPlayers();
            Vec3 at = position();
            Vfx.ring(sl, at.add(0, 0.1, 0), 0xFF8A2A, 7.0, 20);
            sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1.5, at.z, 30, 1.2, 1.2, 1.2, 0.1);
            sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1.5, at.z, 30, 1.2, 1.2, 1.2, 0.05);
            spawnAtLocation(new ItemStack(Items.COAL, 3 + random.nextInt(4)));
            spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 4 + random.nextInt(6)));
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
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            double yaw = Math.toRadians(this.yBodyRot);
            double lx = Math.cos(yaw);
            double lz = Math.sin(yaw);
            float charge = slamProgress();
            if (this.tickCount % (charge > 0.2F ? 2 : 11) == 0) {
                this.level().addParticle(charge > 0.2F ? ParticleTypes.FLAME : ParticleTypes.SMALL_FLAME,
                        getX() - lx * 0.85 + (random.nextDouble() - 0.5) * 0.3, getY() + 0.15 + charge * 2.0, getZ() - lz * 0.85 + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.03, 0.0);
            }
            if (this.tickCount % 17 == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 0.4, getY() + 2.2, getZ() + (random.nextDouble() - 0.5) * 0.4, 0.0, 0.04, 0.0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.CAPATAZ_AMBIENT.get();
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.CAPATAZ_STEP.get(), 0.7F, 0.9F + random.nextFloat() * 0.2F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CAPATAZ_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CAPATAZ_DEATH.get();
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
            emberDamage = DungeonBalance.dmg(stage, 7.0F, players);
        }
    }
}
