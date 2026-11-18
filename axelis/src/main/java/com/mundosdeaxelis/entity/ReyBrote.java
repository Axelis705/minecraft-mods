package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.Scheduler;
import com.mundosdeaxelis.world.WorldTier;
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
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * El Rey Brote: el jefe de la etapa 1 (Naturaleza). Un slime ancestral con un arbolito en la cabeza y una semilla brillante
 * dentro del cuerpo. Salta hacia ti (y puedes esquivar sus golpes al suelo saltando), da grandes saltos con aviso en el
 * suelo, escupe baba que ralentiza, invoca gelatinitas y las absorbe para curarse; en la fase 2 echa raíces (zarzas bajo tus
 * pies) y en la fase 3 se enfurece.
 */
public class ReyBrote extends Monster {
    public static final String MINION_TAG = "rey_brote_minion";
    private static final int GREEN = 0x7BD66B;
    /** Etapa del jefe: de ella salen su vida y su daño (ver {@link BossBalance}). */
    public static final int STAGE = 1;
    private static final float DMG_MUL = BossBalance.dmgMul(STAGE);
    /** Daño actual: el de la etapa por los jugadores que hay en la arena (ver {@link #rescale}). */
    private float dmgNow = DMG_MUL;
    /** Fracción del daño que recibe cuando la vida de diseño (varios jugadores) pasa del tope de 1024 del atributo. */
    private float hpScale = 1.0F;
    private boolean warnedMinions = false;
    private boolean absorbPending = false;

    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(ReyBrote.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SQUASH =
            SynchedEntityData.defineId(ReyBrote.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(ReyBrote.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Rey Brote"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Estiramiento suavizado para dibujar (solo cliente). */
    public float sq;
    public float sqO;

    private int hopCd = 40;
    private int leapCd = 180;
    private int spitCd = 120;
    private int summonCd = 220;
    private int thornCd = 160;
    private int absorbCd = 260;
    private int attackCd = 0;
    private int leapCharge = -1;
    private Vec3 leapTarget = Vec3.ZERO;
    private boolean leaping = false;
    private int airTicks = 0;
    private boolean wasGround = true;
    private int landSquash = 0;
    private int idleNoPlayer = 0;
    private boolean victory = false;

    public ReyBrote(EntityType<? extends ReyBrote> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setCustomName(Component.literal("Rey Brote"));
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossBalance.hp(STAGE))
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SCALE, 1.0F);
        this.entityData.define(SQUASH, 0.0F);
        this.entityData.define(PHASE, 1);
    }

    public float scaleFactor() {
        return this.entityData.get(SCALE);
    }

    public float squash() {
        return this.entityData.get(SQUASH);
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return super.getDimensions(pose).scale(scaleFactor());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (SCALE.equals(key)) {
            refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    // ------------------------------------------------------------------ propiedades

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return false;
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
        return com.mundosdeaxelis.registry.ModSounds.REY_BROTE_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.mundosdeaxelis.registry.ModSounds.REY_BROTE_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.mundosdeaxelis.registry.ModSounds.REY_BROTE_DEATH.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase());
        tag.putBoolean("Victory", victory);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int ph = Math.max(1, Math.min(3, tag.getInt("Phase")));
        this.entityData.set(PHASE, ph);
        this.entityData.set(SCALE, scaleForPhase(ph));
        this.bossEvent.setColor(ph >= 3 ? BossEvent.BossBarColor.RED : ph == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        this.victory = tag.getBoolean("Victory");
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getDisplayName());
    }

    private static float scaleForPhase(int ph) {
        return ph >= 3 ? 1.35F : ph == 2 ? 1.2F : 1.0F;
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            this.sqO = this.sq;
            this.sq += (this.entityData.get(SQUASH) - this.sq) * 0.45F;
            return;
        }
        // goteo de baba para que se note que es de gelatina
        if (this.tickCount % 6 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.ITEM_SLIME, getX() + (random.nextDouble() - 0.5) * getBbWidth(),
                    getY() + getBbHeight() * 0.8, getZ() + (random.nextDouble() - 0.5) * getBbWidth(), 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl) || victory || !isAlive()) {
            return;
        }
        this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        float frac = getHealth() / getMaxHealth();
        int ph = frac <= 0.30F ? 3 : frac <= 0.65F ? 2 : 1;
        if (ph > phase()) {
            enterPhase(sl, ph);
        }

        if (this.tickCount % 20 == 0) {
            rescale(sl);
        }

        ServerPlayer tgt = nearestPlayer(sl, 64.0);
        if (tgt == null) {
            idleNoPlayer++;
            setTarget(null);
            if (idleNoPlayer > 300) {
                // nadie vivo en la arena: el jefe se reinicia (volverá a aparecer en la siguiente entrada)
                this.bossEvent.removeAllPlayers();
                for (Slime s : minions(sl, 80.0)) {
                    s.discard();
                }
                discard();
            }
            return;
        }
        idleNoPlayer = 0;
        setTarget(tgt);
        face(tgt.position());

        boolean onG = onGround();
        float squashTarget = 0.0F;
        if (attackCd > 0) {
            attackCd--;
        }

        // ----- gran salto: carga con aviso
        if (leapCharge >= 0) {
            leapCharge++;
            squashTarget = -Math.min(1.0F, leapCharge / 20.0F);
            if (leapCharge % 10 == 1) {
                sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.3, getZ(), 12, getBbWidth() * 0.4, 0.1,
                        getBbWidth() * 0.4, 0.05);
            }
            if (leapCharge >= 30) {
                launchLeap(sl);
                leapCharge = -1;
            }
            this.entityData.set(SQUASH, squashTarget);
            return;
        }

        // ----- aterrizaje
        if (onG && !wasGround && airTicks >= 4) {
            if (leaping) {
                leaping = false;
                shock(sl, 6.5 * scaleFactor(), 8.0F + 2.0F * (phase() - 1), 1.1, 0.55, true);
                RelicFx.shakeNear(sl, position(), 16, 1.0F, 10);
                if (phase() >= 3) {
                    summon(sl, tgt, 1);
                }
            } else {
                shock(sl, 3.4 * scaleFactor(), 4.0F + phase(), 0.6, 0.3, false);
            }
            landSquash = 8;
        }
        if (onG) {
            airTicks = 0;
        } else {
            airTicks++;
        }
        wasGround = onG;

        // ----- contacto
        double reach = getBbWidth() * 0.5 + 0.7;
        if (attackCd <= 0 && horizontalDistance(tgt.position()) < reach && Math.abs(tgt.getY() - getY()) < getBbHeight()) {
            tgt.hurt(damageSources().mobAttack(this), (5.0F + phase()) * dmgNow);
            knock(tgt, 0.8, 0.35);
            attackCd = 24;
            playSound(SoundEvents.SLIME_ATTACK, 1.2F, 0.6F);
        }

        // ----- habilidades (de una en una)
        if (onG && !leaping) {
            double dist = horizontalDistance(tgt.position());
            if (--leapCd <= 0 && dist > 4.0) {
                startLeap(sl, tgt);
                this.entityData.set(SQUASH, -0.3F);
                return;
            }
            if (--hopCd <= 0) {
                hop(tgt, dist);
            }
        }
        if (--spitCd <= 0) {
            spit(sl);
            spitCd = phase() == 1 ? 120 : phase() == 2 ? 95 : 70;
        }
        if (--summonCd <= 0) {
            summon(sl, tgt, phase() == 1 ? 1 : phase() == 2 ? 2 : 3);
            summonCd = phase() == 1 ? 360 : phase() == 2 ? 300 : 250;
        }
        if (phase() >= 2) {
            if (--thornCd <= 0) {
                thorns(sl);
                thornCd = phase() == 2 ? 160 : 115;
            }
            if (--absorbCd <= 0 && !absorbPending) {
                warnAbsorb(sl);
                absorbCd = 240;
            }
        }

        // ----- estiramiento para la animación
        if (landSquash > 0) {
            squashTarget = -0.8F * landSquash / 8.0F;
            landSquash--;
        } else if (!onG) {
            squashTarget = getDeltaMovement().y > 0 ? 0.55F : 0.25F;
        }
        this.entityData.set(SQUASH, squashTarget);
    }

    // ------------------------------------------------------------------ movimiento

    private double horizontalDistance(Vec3 p) {
        return Math.hypot(p.x - getX(), p.z - getZ());
    }

    private void face(Vec3 p) {
        float yaw = (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * (180.0F / Math.PI)) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void hop(ServerPlayer tgt, double dist) {
        double v = Mth.clamp(dist / 7.2, 0.25, 0.75);
        Vec3 d = new Vec3(tgt.getX() - getX(), 0, tgt.getZ() - getZ());
        d = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize();
        setDeltaMovement(d.x * v, 0.62, d.z * v);
        this.hasImpulse = true;
        playSound(SoundEvents.SLIME_JUMP, 1.6F, 0.5F);
        hopCd = phase() == 1 ? 30 : phase() == 2 ? 22 : 15;
    }

    private void startLeap(ServerLevel sl, ServerPlayer tgt) {
        leapCharge = 0;
        leapTarget = tgt.position();
        leapCd = phase() == 1 ? 170 : phase() == 2 ? 130 : 100;
        // aviso: el círculo en el suelo es donde va a caer
        Vfx.ring(sl, leapTarget.add(0, 0.1, 0), 0xFF5A3C, 6.5 * scaleFactor(), 30);
        Vfx.rune(sl, leapTarget.add(0, 0.12, 0), 0x9CE05A, 6.0 * scaleFactor(), 30);
        playSound(com.mundosdeaxelis.registry.ModSounds.REY_BROTE_CHARGE.get(), 2.0F, 1.0F);
    }

    private void launchLeap(ServerLevel sl) {
        Vec3 d = new Vec3(leapTarget.x - getX(), 0, leapTarget.z - getZ());
        double dist = d.length();
        d = dist < 1.0E-4 ? Vec3.ZERO : d.normalize();
        double v = Mth.clamp(dist / 10.1, 0.2, 1.15);
        setDeltaMovement(d.x * v, 1.15, d.z * v);
        this.hasImpulse = true;
        this.leaping = true;
        this.airTicks = 0;
        this.wasGround = false;
        playSound(SoundEvents.SLIME_JUMP, 2.0F, 0.35F);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, getX(), getY() + 0.2, getZ(), 24, getBbWidth() * 0.5, 0.1,
                getBbWidth() * 0.5, 0.1);
    }

    // ------------------------------------------------------------------ ataques

    private List<ServerPlayer> players(ServerLevel sl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceTo(this) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    private ServerPlayer nearestPlayer(ServerLevel sl, double range) {
        ServerPlayer best = null;
        double bd = Double.MAX_VALUE;
        for (ServerPlayer p : players(sl, range)) {
            double d = p.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private void knock(ServerPlayer p, double horizontal, double up) {
        Vec3 d = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        p.setDeltaMovement(p.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        p.hurtMarked = true;
    }

    /** Onda de choque al aterrizar: si saltas encima de ella (más de 1,3 bloques sobre el suelo del jefe) la esquivas. */
    private void shock(ServerLevel sl, double radius, float damage, double knockH, double knockUp, boolean big) {
        Vec3 at = position();
        Vfx.ring(sl, at.add(0, 0.1, 0), GREEN, radius + 0.5, big ? 16 : 10);
        if (big) {
            Vfx.flash(sl, at.add(0, 0.6, 0), 0xC8FFB0, 4.0, 10);
        }
        sl.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.2, at.z, big ? 60 : 24, radius * 0.4, 0.15, radius * 0.4, 0.2);
        if (big) {
            playSound(com.mundosdeaxelis.registry.ModSounds.REY_BROTE_SLAM.get(), 2.5F, 1.0F);
        } else {
            playSound(SoundEvents.SLIME_SQUISH, 1.4F, 0.5F);
        }
        for (ServerPlayer p : players(sl, radius + 2.0)) {
            if (horizontalDistance(p.position()) > radius || p.getY() - getY() > 1.3) {
                continue;
            }
            p.hurt(damageSources().mobAttack(this), damage * dmgNow);
            knock(p, knockH, knockUp);
        }
    }

    /** Escupe baba: marca un círculo bajo cada jugador y, un segundo después, estalla y deja un charco que ralentiza. */
    private void spit(ServerLevel sl) {
        int n = 0;
        for (ServerPlayer p : players(sl, 40.0)) {
            if (n++ >= 3) {
                break;
            }
            Vec3 spot = new Vec3(p.getX() + (random.nextDouble() - 0.5) * 3.0, getY(), p.getZ() + (random.nextDouble() - 0.5) * 3.0);
            Vec3 from = position().add(0, getBbHeight() * 0.8, 0);
            for (int i = 1; i <= 12; i++) {
                double t = i / 12.0;
                sl.sendParticles(ParticleTypes.ITEM_SLIME, Mth.lerp(t, from.x, spot.x), Mth.lerp(t, from.y, spot.y)
                        + Math.sin(t * Math.PI) * 4.0, Mth.lerp(t, from.z, spot.z), 2, 0.1, 0.1, 0.1, 0.0);
            }
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0x9CE05A, 2.4, 24);
            playSound(com.mundosdeaxelis.registry.ModSounds.REY_BROTE_SPIT.get(), 1.6F, 1.0F);
            final int ph = phase();
            Scheduler.runLater(24, () -> splat(sl, spot, ph));
        }
    }

    private void splat(ServerLevel sl, Vec3 spot, int ph) {
        sl.sendParticles(ParticleTypes.ITEM_SLIME, spot.x, spot.y + 0.3, spot.z, 30, 1.0, 0.3, 1.0, 0.15);
        sl.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2F, 0.6F);
        for (int k = 0; k < 9; k++) {
            final boolean first = k == 0;
            Scheduler.runLater(k * 10, () -> {
                if (first) {
                    for (ServerPlayer p : sl.players()) {
                        if (p.isAlive() && Math.hypot(p.getX() - spot.x, p.getZ() - spot.z) <= 2.4 && Math.abs(p.getY() - spot.y) < 2.5) {
                            p.hurt(damageSources().mobAttack(this), (3.0F + ph) * dmgNow);
                        }
                    }
                }
                sl.sendParticles(ParticleTypes.ITEM_SLIME, spot.x, spot.y + 0.1, spot.z, 5, 1.2, 0.05, 1.2, 0.0);
                for (ServerPlayer p : sl.players()) {
                    if (p.isAlive() && Math.hypot(p.getX() - spot.x, p.getZ() - spot.z) <= 2.2 && Math.abs(p.getY() - spot.y) < 2.0) {
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
                    }
                }
            });
        }
    }

    private List<Slime> minions(ServerLevel sl, double range) {
        AABB box = getBoundingBox().inflate(range);
        return sl.getEntitiesOfClass(Slime.class, box, s -> s.getTags().contains(MINION_TAG) && s.isAlive());
    }

    /** Invoca gelatinitas alrededor (con un máximo según la fase). */
    private void summon(ServerLevel sl, ServerPlayer tgt, int count) {
        int cap = phase() == 1 ? 3 : phase() == 2 ? 4 : 6;
        int have = minions(sl, 60.0).size();
        count = Math.min(count, cap - have);
        for (int i = 0; i < count; i++) {
            Slime s = EntityType.SLIME.create(sl);
            if (s == null) {
                return;
            }
            s.setSize(1, true);
            s.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0D);
            s.setHealth(8.0F);
            s.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(3.0D);
            double a = random.nextDouble() * Math.PI * 2;
            double r = getBbWidth() * 0.5 + 1.5;
            s.moveTo(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r, random.nextFloat() * 360.0F, 0.0F);
            s.addTag(MINION_TAG);
            s.getPersistentData().putBoolean("mdc_allowed", true); // sin esto la protección de la dungeon borraba al slime al aparecer
            s.setCustomName(Component.literal("Gelatinita"));
            s.setPersistenceRequired();
            s.setTarget(tgt);
            sl.addFreshEntity(s);
            s.setDeltaMovement(Math.cos(a) * 0.25, 0.45, Math.sin(a) * 0.25);
            sl.sendParticles(ParticleTypes.ITEM_SLIME, s.getX(), s.getY() + 0.3, s.getZ(), 8, 0.2, 0.2, 0.2, 0.1);
        }
        if (count > 0) {
            playSound(SoundEvents.SLIME_SQUISH, 1.6F, 0.5F);
            if (!warnedMinions) {
                warnedMinions = true;
                for (ServerPlayer p : players(sl, 80.0)) {
                    p.displayClientMessage(Component.literal("¡Invoca Gelatinitas! Si las deja vivas, se las traga y se cura.")
                            .withStyle(net.minecraft.ChatFormatting.YELLOW), true);
                }
            }
        }
    }

    /** Aviso antes de absorber: las gelatinitas brillan 2 segundos para que dé tiempo a matarlas. */
    private void warnAbsorb(ServerLevel sl) {
        List<Slime> list = minions(sl, 14.0);
        if (list.isEmpty()) {
            return;
        }
        absorbPending = true;
        for (Slime s : list) {
            s.addEffect(new MobEffectInstance(MobEffects.GLOWING, 45, 0, false, false));
        }
        for (ServerPlayer p : players(sl, 80.0)) {
            p.displayClientMessage(Component.literal("¡El Rey Brote va a absorber a las Gelatinitas y curarse! ¡Mátalas ya!")
                    .withStyle(net.minecraft.ChatFormatting.RED), true);
        }
        playSound(SoundEvents.SLIME_SQUISH, 1.2F, 0.4F);
        Scheduler.runLater(45, () -> {
            absorbPending = false;
            if (isAlive() && !victory && this.level() instanceof ServerLevel l) {
                absorb(l);
            }
        });
    }

    /** Ajusta vida y daño a los jugadores que hay en la arena (mantiene la proporción de vida que tiene el jefe). */
    private void rescale(ServerLevel sl) {
        int n = 0;
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                n++;
            }
        }
        n = Math.max(1, n);
        dmgNow = DMG_MUL * BossBalance.dmgPlayers(n);
        float target = BossBalance.hp(STAGE) * BossBalance.hpPlayers(n);
        hpScale = BossBalance.hpScale(target);
        float max = BossBalance.realMax(target); // el atributo de vida no pasa de 1024 (con varios jugadores la vida de diseño sí)
        if (Math.abs(max - getMaxHealth()) > 0.5F) {
            float frac = getHealth() / getMaxHealth();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
            setHealth(Mth.clamp(frac * max, 1.0F, max));
        }
    }

    /** Se traga a las gelatinitas cercanas y se cura un poco por cada una: conviene matarlas antes. */
    private void absorb(ServerLevel sl) {
        List<Slime> list = minions(sl, 14.0);
        if (list.isEmpty()) {
            return;
        }
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        for (Slime s : list) {
            Vec3 p = s.position().add(0, 0.3, 0);
            for (int i = 1; i <= 8; i++) {
                double t = i / 8.0;
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, Mth.lerp(t, p.x, c.x), Mth.lerp(t, p.y, c.y), Mth.lerp(t, p.z, c.z), 1, 0, 0, 0, 0);
            }
            s.discard();
            heal(getMaxHealth() * 0.015F);
        }
        playSound(com.mundosdeaxelis.registry.ModSounds.REY_BROTE_ABSORB.get(), 2.0F, 1.0F);
        Vfx.flash(sl, c, 0xB8FF90, 3.5, 8);
    }

    /** Zarzas: brotan del suelo bajo cada jugador después de un aviso. */
    private void thorns(ServerLevel sl) {
        for (ServerPlayer p : players(sl, 40.0)) {
            Vec3 spot = p.position();
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0x2E8B3A, 1.9, 22);
            sl.playSound(null, spot.x, spot.y, spot.z, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
            Scheduler.runLater(22, () -> {
                sl.sendParticles(ParticleTypes.COMPOSTER, spot.x, spot.y + 0.5, spot.z, 40, 0.8, 0.8, 0.8, 0.1);
                sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, spot.x, spot.y + 1.0, spot.z, 14, 0.6, 1.0, 0.6, 0.0);
                sl.playSound(null, spot.x, spot.y, spot.z, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
                for (ServerPlayer q : sl.players()) {
                    if (q.isAlive() && Math.hypot(q.getX() - spot.x, q.getZ() - spot.z) <= 1.9 && Math.abs(q.getY() - spot.y) < 2.5) {
                        q.hurt(damageSources().mobAttack(this), (6.0F + phase()) * dmgNow);
                        q.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3, false, true));
                        q.setDeltaMovement(q.getDeltaMovement().add(0, 0.5, 0));
                        q.hurtMarked = true;
                    }
                }
            });
        }
    }

    private void enterPhase(ServerLevel sl, int ph) {
        this.entityData.set(PHASE, ph);
        this.entityData.set(SCALE, scaleForPhase(ph));
        refreshDimensions();
        this.bossEvent.setColor(ph >= 3 ? BossEvent.BossBarColor.RED : ph == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.GREEN);
        playSound(com.mundosdeaxelis.registry.ModSounds.REY_BROTE_ROAR.get(), 3.0F, 1.0F);
        Vec3 at = position();
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xC8FFB0, 9.0, 18);
        Vfx.flash(sl, at.add(0, 1.0, 0), 0xC8FFB0, 5.0, 12);
        RelicFx.shakeNear(sl, at, 20, 1.2F, 14);
        for (ServerPlayer p : players(sl, 12.0)) {
            knock(p, 1.2, 0.5);
        }
        String msg = ph == 2 ? "¡El Rey Brote echa raíces!" : "¡El Rey Brote se enfurece!";
        for (ServerPlayer p : players(sl, 80.0)) {
            p.displayClientMessage(Component.literal(msg).withStyle(net.minecraft.ChatFormatting.GREEN), true);
        }
        summonCd = Math.min(summonCd, 30);
        leapCd = Math.min(leapCd, 60);
        com.mundosdeaxelis.world.BossFx.phase(sl, at, ph, 0x5CD66C, 0xB8FFB0, ph == 2 ? "FASE II" : "FASE III",
                ph == 2 ? "LAS RAÍCES DESPIERTAN" : "FURIA DEL BOSQUE", net.minecraft.core.particles.ParticleTypes.ITEM_SLIME);
    }

    // ------------------------------------------------------------------ daño y muerte

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        return super.hurt(source, amount * hpScale);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && !victory) {
            victory = true;
            victory(sl);
        }
        super.die(source);
    }

    private void victory(ServerLevel sl) {
        this.bossEvent.setProgress(0.0F);
        for (Slime s : minions(sl, 80.0)) {
            s.discard();
        }
        Vec3 at = position();
        Vfx.ring(sl, at.add(0, 0.1, 0), GREEN, 12.0, 24);
        Vfx.flash(sl, at.add(0, 1.0, 0), 0xE8FFD0, 7.0, 20);
        sl.sendParticles(ParticleTypes.HAPPY_VILLAGER, at.x, at.y + 1.0, at.z, 80, 2.0, 1.5, 2.0, 0.1);
        RelicFx.shakeNear(sl, at, 24, 1.0F, 20);
        spawnAtLocation(new ItemStack(Items.SLIME_BALL, 12 + random.nextInt(9)));
        spawnAtLocation(new ItemStack(Items.MOSS_BLOCK, 6 + random.nextInt(7)));
        // sello, material de Mesa y extras: según el daño que hizo cada jugador (ver BossRewards)
        List<ServerPlayer> present = players(sl, 90.0);
        com.mundosdeaxelis.world.BossRewards.distribute(sl, this, STAGE, present);
        for (ServerPlayer p : present) {
            ExperienceOrb.award(sl, p.position(), 250);
        }
        com.mundosdeaxelis.world.BossFx.defeat(sl, at, 0x5CD66C, 0xB8FFB0, ParticleTypes.HAPPY_VILLAGER,
                net.minecraft.world.level.block.Blocks.MOSS_BLOCK.defaultBlockState(), "REY BROTE DERROTADO", "EL BOSQUE VUELVE A RESPIRAR");
        sl.getServer().execute(() -> WorldTier.bossDefeated(sl.getServer(), 1));
        BossTravel.scheduleExit(sl);
    }
}
