package com.mundosdeaxelis.entity;

import net.minecraft.world.entity.LivingEntity;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.BossFx;
import com.mundosdeaxelis.world.BossRewards;
import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.Titles;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * El Archimago: el jefe de la etapa 4 (la Torre). Un mago enorme con túnica, bastón y cristales flotantes. No persigue: mantiene la
 * distancia, se teletransporta y llena la arena de hechizos que SIEMPRE avisan antes de hacer daño.
 * <ul>
 *   <li><b>Fase 1, la biblioteca</b>: rayos arcanos en abanico (primero una línea fina, luego el rayo), círculos rúnicos que
 *       estallan bajo los jugadores, bastonazo si te acercas y parpadeo (se va y deja un eco que estalla).</li>
 *   <li><b>Fase 2, la alquimia</b>: frascos que dejan charcos de veneno y el <b>escudo de sellos</b>: se vuelve casi inmune hasta
 *       que los jugadores pisen los tres sellos de luz de la arena; si lo logran queda aturdido, si no, suelta una nova.</li>
 *   <li><b>Fase 3, las estrellas</b>: lluvia de meteoritos marcada en el suelo y rayos giratorios desde el centro.</li>
 * </ul>
 * Los estados se sincronizan con el cliente ({@link #state()}) y el modelo (ArchimagoModel) trae una pose para cada uno.
 */
public class Archimago extends Monster {
    /** Etapa del jefe: de ella salen su vida y su daño (ver {@link BossBalance}). */
    public static final int STAGE = 5;

    public static final int S_IDLE = 0;
    public static final int S_RAYS = 1;
    public static final int S_RUNES = 2;
    public static final int S_BLINK = 3;
    public static final int S_STAFF = 4;
    public static final int S_FLASKS = 5;
    public static final int S_SEALS = 6;
    public static final int S_STUN = 7;
    public static final int S_METEORS = 8;
    public static final int S_BEAMS = 9;

    public static final int DEATH_TICKS = 70;

    private static final int ARCANE = 0x78F0FF;
    private static final int ARCANE_PALE = 0xD8FBFF;
    private static final int VIOLET = 0xB46EFF;
    private static final int GOLD = 0xFFD86A;
    private static final int VENOM = 0x7CFF5A;
    private static final int WARN = 0xFF6A5A;
    private static final float DMG_MUL = BossBalance.dmgMul(STAGE);
    /** Radio útil de la arena alrededor de su centro (para parpadeos, sellos y meteoritos). */
    private static final double ARENA_R = 17.0;

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(Archimago.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(Archimago.class, EntityDataSerializers.INT);
    /** Sellos del escudo que quedan en pie (0 = sin escudo). */
    private static final EntityDataAccessor<Integer> SHIELD =
            SynchedEntityData.defineId(Archimago.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Archimago"),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Algo marcado en el suelo que estalla al cabo de unos ticks. */
    private static final class Mark {
        static final int RUNE = 0;
        static final int ECHO = 1;
        static final int FLASK = 2;
        static final int METEOR = 3;
        final Vec3 pos;
        final double radius;
        final int kind;
        int ticks;

        Mark(Vec3 pos, double radius, int kind, int ticks) {
            this.pos = pos;
            this.radius = radius;
            this.kind = kind;
            this.ticks = ticks;
        }
    }

    /** Charco de veneno: dura unos segundos y daña a quien se quede dentro. */
    private static final class Puddle {
        final Vec3 pos;
        int ticks;

        Puddle(Vec3 pos, int ticks) {
            this.pos = pos;
            this.ticks = ticks;
        }
    }

    private float dmgNow = DMG_MUL;
    private float hpScale = 1.0F;
    private int stateT;
    private int clientStateStart;
    private int deathTicks;
    private boolean victory;
    private int idleNoPlayer;

    private int atkCd = 60;
    private int raysCd = 40;
    private int runesCd = 120;
    private int blinkCd = 100;
    private int flasksCd = 160;
    private int sealsCd = 0;
    private int meteorsCd = 120;
    private int beamsCd = 260;
    private int closeTicks;
    private boolean pendingSeals;
    private boolean pendingMeteors;
    private boolean warnedSeals;
    private boolean warnedBeams;

    private Vec3 home;
    private final List<Mark> marks = new ArrayList<>();
    private final List<Puddle> puddles = new ArrayList<>();
    private final List<Vec3> rays = new ArrayList<>();
    private final List<Vec3> seals = new ArrayList<>();
    private final Map<UUID, Integer> beamCd = new HashMap<>();
    private Vec3 blinkTo = Vec3.ZERO;
    private double beamAngle;
    private double beamDir = 1.0;
    private int stunLen = 100;

    public Archimago(EntityType<? extends Archimago> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setCustomName(Component.literal("Archimago"));
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossBalance.realMax(BossBalance.hp(STAGE)))
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, S_IDLE);
        this.entityData.define(PHASE, 1);
        this.entityData.define(SHIELD, 0);
    }

    // ------------------------------------------------------------------ lo que lee el modelo

    public int state() {
        return this.entityData.get(STATE);
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    /** Sellos del escudo que quedan (0 = sin escudo). */
    public int shield() {
        return this.entityData.get(SHIELD);
    }

    public float stateAge(float partialTick) {
        if (this.level().isClientSide) {
            return Math.max(0, this.tickCount - this.clientStateStart) + partialTick;
        }
        return this.stateT + partialTick;
    }

    public float deathProgress(float partialTick) {
        return this.deathTicks > 0 ? Mth.clamp((this.deathTicks + partialTick) / DEATH_TICKS, 0.0F, 1.0F) : 0.0F;
    }

    /** Cuánto brillan sus ojos, el bastón y los cristales (0 a 1.2). */
    public float glowLevel(float age) {
        float base = 0.7F + 0.12F * Mth.sin(age * 0.1F) + 0.06F * (phase() - 1);
        return switch (state()) {
            case S_SEALS, S_BEAMS, S_METEORS -> base + 0.4F;
            case S_RAYS, S_RUNES, S_FLASKS -> base + 0.25F;
            case S_STUN -> base - 0.4F;
            default -> base;
        };
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (STATE.equals(key)) {
            this.clientStateStart = this.tickCount;
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
        return com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return com.mundosdeaxelis.registry.ModSounds.ARCHIMAGO_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return 3.3F;
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
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Phase", phase());
        tag.putBoolean("Victory", victory);
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        int ph = Math.max(1, Math.min(3, tag.getInt("Phase")));
        this.entityData.set(PHASE, ph);
        this.bossEvent.setColor(barColor(ph));
        this.victory = tag.getBoolean("Victory");
        if (tag.contains("HomeX")) {
            this.home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }

    /** Centro de la arena (en el combate de prueba es donde apareció). */
    public void setHome(Vec3 center) {
        this.home = center;
    }

    private static BossEvent.BossBarColor barColor(int ph) {
        return ph >= 3 ? BossEvent.BossBarColor.YELLOW : ph == 2 ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.BLUE;
    }

    // ------------------------------------------------------------------ muerte lenta

    @Override
    protected void tickDeath() {
        this.deathTicks++;
        if (this.level().isClientSide) {
            return;
        }
        if (this.level() instanceof ServerLevel sl) {
            if (this.deathTicks % 5 == 0) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 2.0, getZ(), 10, 0.7, 1.4, 0.7, 0.05);
                sl.sendParticles(ParticleTypes.ENCHANT, getX(), getY() + 2.5, getZ(), 24, 0.9, 1.4, 0.9, 0.6);
            }
            if (this.deathTicks == DEATH_TICKS - 12) {
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3.0F, 0.6F);
                RelicFx.shakeNear(sl, position(), 24, 1.4F, 12);
                Vfx.flash(sl, position().add(0, 2.0, 0), ARCANE_PALE, 6.0, 10);
            }
        }
        if (this.deathTicks >= DEATH_TICKS && !isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientFx();
        }
    }

    private Vec3 forward() {
        float r = this.yBodyRot * ((float) Math.PI / 180.0F);
        return new Vec3(-Mth.sin(r), 0.0, Mth.cos(r));
    }

    /** Partículas que solo ven los jugadores cercanos: chispas del bastón y polvo mágico de la túnica. */
    private void clientFx() {
        if (isDeadOrDying()) {
            return;
        }
        int st = state();
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.ENCHANT, getX() + (this.random.nextDouble() - 0.5) * 1.6, getY() + 0.4 + this.random.nextDouble() * 1.2,
                    getZ() + (this.random.nextDouble() - 0.5) * 1.6, 0.0, 0.4, 0.0);
        }
        if (st == S_RAYS || st == S_RUNES || st == S_METEORS || st == S_BEAMS || st == S_SEALS) {
            Vec3 f = forward();
            this.level().addParticle(ParticleTypes.END_ROD, getX() + f.x * 0.9 + (this.random.nextDouble() - 0.5) * 0.4, getY() + 4.2,
                    getZ() + f.z * 0.9 + (this.random.nextDouble() - 0.5) * 0.4, 0.0, 0.03, 0.0);
        }
        if (st == S_STUN && this.tickCount % 4 == 0) {
            this.level().addParticle(ParticleTypes.CRIT, getX() + (this.random.nextDouble() - 0.5) * 1.2, getY() + 3.6,
                    getZ() + (this.random.nextDouble() - 0.5) * 1.2, 0.0, 0.05, 0.0);
        }
    }

    // ------------------------------------------------------------------ ayudas

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.stateT = 0;
    }

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

    private double hd(Vec3 p) {
        return Math.hypot(p.x - getX(), p.z - getZ());
    }

    private static double hd(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private Vec3 dirTo(Vec3 p) {
        Vec3 d = new Vec3(p.x - getX(), 0.0, p.z - getZ());
        return d.lengthSqr() < 1.0E-4 ? forward() : d.normalize();
    }

    private void faceDir(Vec3 d, float maxStep) {
        float want = (float) (Mth.atan2(d.z, d.x) * (180.0F / Math.PI)) - 90.0F;
        float yaw = Mth.approachDegrees(getYRot(), want, maxStep);
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void face(Vec3 p, float maxStep) {
        faceDir(dirTo(p), maxStep);
    }

    private void knockFromMe(ServerPlayer p, double horizontal, double up) {
        Vec3 d = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        p.setDeltaMovement(p.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        p.hurtMarked = true;
    }

    private void announce(ServerLevel sl, String title, String sub) {
        for (ServerPlayer p : players(sl, 80.0)) {
            Titles.hint(p, title, sub);
        }
    }

    private void sound(ServerLevel sl, Vec3 at, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    /** La punta del bastón (de donde salen los hechizos). */
    private Vec3 staffTip() {
        return position().add(forward().scale(0.9)).add(0, 4.2, 0);
    }

    /** Un punto del suelo de la arena a cierta distancia del centro, a la altura del jefe. */
    private Vec3 arenaPoint(double angle, double radius) {
        return new Vec3(home.x + Math.cos(angle) * radius, home.y, home.z + Math.sin(angle) * radius);
    }

    /** Recorta un punto para que no se salga de la arena. */
    private Vec3 clampToArena(Vec3 p) {
        double dx = p.x - home.x;
        double dz = p.z - home.z;
        double d = Math.hypot(dx, dz);
        if (d <= ARENA_R) {
            return new Vec3(p.x, home.y, p.z);
        }
        return new Vec3(home.x + dx / d * ARENA_R, home.y, home.z + dz / d * ARENA_R);
    }

    private boolean inCircle(ServerPlayer p, Vec3 c, double r) {
        return hd(p.position(), c) <= r && Math.abs(p.getY() - c.y) < 3.5;
    }

    private int gap() {
        return phase() == 1 ? 34 : phase() == 2 ? 26 : 20;
    }

    // ------------------------------------------------------------------ IA

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl) || victory || !isAlive()) {
            return;
        }
        if (this.home == null) {
            this.home = position();
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
        raysCd = Math.max(0, raysCd - 1);
        runesCd = Math.max(0, runesCd - 1);
        blinkCd = Math.max(0, blinkCd - 1);
        flasksCd = Math.max(0, flasksCd - 1);
        sealsCd = Math.max(0, sealsCd - 1);
        meteorsCd = Math.max(0, meteorsCd - 1);
        beamsCd = Math.max(0, beamsCd - 1);
        beamCd.replaceAll((k, v) -> v - 1);
        beamCd.values().removeIf(v -> v <= 0);
        tickMarks(sl);
        tickPuddles(sl);

        // etapa 284: el objetivo lo elige la amenaza (jugadores y mascotas), no solo «el más cercano»
        LivingEntity tgt = com.mundosdeaxelis.world.JefeIA.objetivo(this, sl, 70.0, true);
        if (tgt == null) {
            idleNoPlayer++;
            setTarget(null);
            getNavigation().stop();
            if (idleNoPlayer > 300) {
                this.bossEvent.removeAllPlayers();
                discard();
            }
            return;
        }
        idleNoPlayer = 0;
        setTarget(tgt);
        // etapa 284: las mascotas que le muerden se llevan un manotazo
        com.mundosdeaxelis.world.JefeIA.manotazo(this, sl, 5.0F * dmgNow);
        stateT++;
        switch (state()) {
            case S_IDLE -> idleTick(sl, tgt);
            case S_RAYS -> raysTick(sl, tgt);
            case S_RUNES -> runesTick(sl);
            case S_BLINK -> blinkTick(sl, tgt);
            case S_STAFF -> staffTick(sl, tgt);
            case S_FLASKS -> flasksTick(sl);
            case S_SEALS -> sealsTick(sl, tgt);
            case S_STUN -> stunTick(sl);
            case S_METEORS -> meteorsTick(sl, tgt);
            case S_BEAMS -> beamsTick(sl);
            default -> setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ guardia y elección de hechizo

    private void idleTick(ServerLevel sl, LivingEntity tgt) {
        double dist = hd(tgt.position());
        face(tgt.position(), 20.0F);
        if (atkCd > 0) {
            atkCd--;
        }
        closeTicks = dist < 5.5 ? closeTicks + 1 : Math.max(0, closeTicks - 2);
        if (atkCd <= 0) {
            if (pendingSeals) {
                pendingSeals = false;
                startSeals(sl);
                return;
            }
            if (pendingMeteors) {
                pendingMeteors = false;
                startMeteors(sl);
                return;
            }
            if (phase() >= 3 && beamsCd <= 0) {
                startBeams(sl);
                return;
            }
            if (phase() >= 3 && meteorsCd <= 0) {
                startMeteors(sl);
                return;
            }
            if (phase() >= 2 && sealsCd <= 0) {
                startSeals(sl);
                return;
            }
            if (dist <= 4.6) {
                getNavigation().stop();
                setState(S_STAFF);
                return;
            }
            if (phase() >= 2 && flasksCd <= 0) {
                getNavigation().stop();
                setState(S_FLASKS);
                return;
            }
            if (runesCd <= 0) {
                getNavigation().stop();
                setState(S_RUNES);
                return;
            }
            if (raysCd <= 0) {
                getNavigation().stop();
                setState(S_RAYS);
                return;
            }
        }
        // si lo acorralan un rato, se va
        if (closeTicks > 50 && blinkCd <= 0) {
            startBlink(sl, tgt, false);
            return;
        }
        // no persigue: solo se acerca si el jugador se aleja demasiado, y vuelve si se sale de la arena
        if (hd(home) > ARENA_R + 2.0) {
            startBlink(sl, tgt, true);
        } else if (dist > 18.0) {
            if (getNavigation().isDone() || this.tickCount % 10 == 0) {
                getNavigation().moveTo(tgt, 1.0);
            }
        } else {
            getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ rayos arcanos

    /** Tres salvas: una línea fina avisa por dónde irá cada rayo y medio segundo después pasa el rayo de verdad. */
    private void raysTick(ServerLevel sl, LivingEntity tgt) {
        int volleys = phase() >= 2 ? 3 : 2;
        int cycle = 26;
        int t = (stateT - 1) % cycle;
        int volley = (stateT - 1) / cycle;
        if (volley >= volleys) {
            raysCd = 70 + this.random.nextInt(40);
            atkCd = gap();
            setState(S_IDLE);
            return;
        }
        if (t < 4) {
            face(tgt.position(), 40.0F);
        }
        if (t == 4) {
            rays.clear();
            Vec3 d = dirTo(tgt.position());
            double base = Math.atan2(d.z, d.x);
            int n = phase() >= 3 ? 7 : phase() == 2 ? 5 : 3;
            double spread = Math.toRadians(phase() >= 2 ? 15.0 : 18.0);
            // las salvas pares apuntan a los huecos de las impares: quedarse quieto no sirve
            double shift = volley % 2 == 1 ? spread / 2.0 : 0.0;
            Vec3 from = staffTip();
            for (int i = 0; i < n; i++) {
                double a = base + (i - (n - 1) / 2.0) * spread + shift;
                Vec3 dir = new Vec3(Math.cos(a), 0.0, Math.sin(a));
                rays.add(dir);
                Vec3 end = position().add(dir.scale(24.0)).add(0, 1.0, 0);
                Vfx.beam(sl, from, end, WARN, 0.12, 13, 0);
            }
            sound(sl, position(), ModSounds.ARCHIMAGO_CHARGE.get(), 2.0F, 1.2F);
        }
        if (t == 17) {
            Vec3 from = staffTip();
            Vec3 feet = position().add(0, 1.0, 0);
            for (Vec3 dir : rays) {
                Vec3 end = feet.add(dir.scale(24.0));
                Vfx.beam(sl, from, end, ARCANE, 0.9, 7, 0);
                Vfx.beam(sl, from, end, ARCANE_PALE, 0.4, 7, 0);
                for (ServerPlayer p : players(sl, 28.0)) {
                    if (RelicFx.segDist(p.position().add(0, 1.0, 0), feet, end) <= 1.0) {
                        p.hurt(damageSources().indirectMagic(this, this), 6.0F * dmgNow);
                        knockFromMe(p, 0.5, 0.2);
                    }
                }
            }
            sound(sl, position(), ModSounds.ARCHIMAGO_CAST.get(), 2.4F, 0.95F);
            sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 3.0F, 0.6F);
        }
    }

    // ------------------------------------------------------------------ círculos rúnicos

    /** Círculos bajo cada jugador y alrededor: estallan un segundo y medio después. En fase 2 y 3 hay una segunda tanda. */
    private void runesTick(ServerLevel sl) {
        int waves = phase() >= 2 ? 2 : 1;
        if (stateT == 8 || (waves == 2 && stateT == 34)) {
            sound(sl, position(), ModSounds.ARCHIMAGO_CAST.get(), 2.2F, 0.8F);
            for (ServerPlayer p : players(sl, 40.0)) {
                addMark(sl, clampToArena(p.position()), 2.6, Mark.RUNE, 30);
                int extra = 1 + phase();
                for (int i = 0; i < extra; i++) {
                    double a = this.random.nextDouble() * Math.PI * 2.0;
                    double r = 3.5 + this.random.nextDouble() * 4.5;
                    addMark(sl, clampToArena(p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r)), 2.6, Mark.RUNE, 30 + this.random.nextInt(8));
                }
            }
        }
        if (stateT >= (waves == 2 ? 62 : 40)) {
            runesCd = 150 + this.random.nextInt(60);
            atkCd = gap();
            setState(S_IDLE);
        }
    }

    private void addMark(ServerLevel sl, Vec3 pos, double radius, int kind, int ticks) {
        Vec3 g = new Vec3(pos.x, pos.y + 0.08, pos.z);
        marks.add(new Mark(g, radius, kind, ticks));
        int rgb = kind == Mark.FLASK ? VENOM : kind == Mark.METEOR ? WARN : kind == Mark.ECHO ? ARCANE : VIOLET;
        Vfx.rune(sl, g, rgb, radius, ticks + 2);
        Vfx.ring(sl, g, WARN, radius, Math.min(ticks, 14));
    }

    /** Las marcas del suelo siguen su cuenta pase lo que pase con el jefe (así los hechizos se encadenan). */
    private void tickMarks(ServerLevel sl) {
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            m.ticks--;
            if (m.ticks == 8) {
                Vfx.ring(sl, m.pos, WARN, m.radius, 8);
            }
            if (m.ticks > 0) {
                continue;
            }
            it.remove();
            explode(sl, m);
        }
    }

    private void explode(ServerLevel sl, Mark m) {
        float dmg;
        switch (m.kind) {
            case Mark.METEOR -> {
                dmg = 9.0F;
                Vfx.beam(sl, m.pos.add(4.0, 22.0, 2.0), m.pos, GOLD, 1.1, 6, 0);
                Vfx.flash(sl, m.pos.add(0, 0.6, 0), GOLD, 5.0, 7);
                Vfx.ring(sl, m.pos, GOLD, m.radius + 1.5, 10);
                sl.sendParticles(ParticleTypes.EXPLOSION, m.pos.x, m.pos.y + 0.5, m.pos.z, 3, 0.8, 0.3, 0.8, 0.0);
                sl.sendParticles(ParticleTypes.LAVA, m.pos.x, m.pos.y + 0.3, m.pos.z, 10, 1.0, 0.2, 1.0, 0.0);
                sound(sl, m.pos, ModSounds.ARCHIMAGO_BURST.get(), 2.4F, 0.9F + this.random.nextFloat() * 0.25F);
                RelicFx.shakeNear(sl, m.pos, 12, 0.6F, 6);
            }
            case Mark.FLASK -> {
                dmg = 4.0F;
                puddles.add(new Puddle(m.pos, 120));
                sl.sendParticles(ParticleTypes.ITEM_SLIME, m.pos.x, m.pos.y + 0.3, m.pos.z, 30, 1.2, 0.2, 1.2, 0.1);
                sound(sl, m.pos, SoundEvents.SPLASH_POTION_BREAK, 2.4F, 0.7F);
            }
            case Mark.ECHO -> {
                dmg = 6.0F;
                Vfx.column(sl, m.pos, ARCANE, m.radius, 5.0, 9);
                Vfx.ring(sl, m.pos, ARCANE_PALE, m.radius + 2.0, 10);
                sound(sl, m.pos, SoundEvents.AMETHYST_BLOCK_BREAK, 3.0F, 0.6F);
            }
            default -> {
                dmg = 7.0F;
                Vfx.column(sl, m.pos, VIOLET, m.radius, 6.0, 9);
                sl.sendParticles(ParticleTypes.WITCH, m.pos.x, m.pos.y + 1.0, m.pos.z, 16, 1.0, 1.0, 1.0, 0.1);
                sound(sl, m.pos, SoundEvents.EVOKER_FANGS_ATTACK, 1.6F, 0.6F + this.random.nextFloat() * 0.2F);
            }
        }
        for (ServerPlayer p : players(sl, 60.0)) {
            if (inCircle(p, m.pos, m.radius)) {
                p.hurt(damageSources().indirectMagic(this, this), dmg * dmgNow);
                if (m.kind != Mark.FLASK) {
                    p.setDeltaMovement(p.getDeltaMovement().add(0, 0.45, 0));
                    p.hurtMarked = true;
                }
            }
        }
    }

    // ------------------------------------------------------------------ charcos de veneno

    private void tickPuddles(ServerLevel sl) {
        Iterator<Puddle> it = puddles.iterator();
        while (it.hasNext()) {
            Puddle pu = it.next();
            pu.ticks--;
            if (pu.ticks <= 0) {
                it.remove();
                continue;
            }
            if (pu.ticks % 10 == 0) {
                Vfx.ring(sl, pu.pos, VENOM, 3.0, 12);
                sl.sendParticles(ParticleTypes.ITEM_SLIME, pu.pos.x, pu.pos.y + 0.2, pu.pos.z, 8, 1.4, 0.1, 1.4, 0.0);
                for (ServerPlayer p : players(sl, 60.0)) {
                    if (inCircle(p, pu.pos, 3.0)) {
                        p.hurt(damageSources().magic(), 2.5F * dmgNow);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                    }
                }
            }
        }
    }

    /** Tres frascos: cada uno cae donde está (o hacia donde va) un jugador y deja un charco seis segundos. */
    private void flasksTick(ServerLevel sl) {
        if (stateT == 8 || stateT == 18 || stateT == 28) {
            List<ServerPlayer> ps = players(sl, 40.0);
            if (!ps.isEmpty()) {
                ServerPlayer p = ps.get(this.random.nextInt(ps.size()));
                Vec3 lead = p.position().add(p.getDeltaMovement().multiply(12.0, 0.0, 12.0));
                Vec3 at = clampToArena(lead);
                addMark(sl, at, 3.0, Mark.FLASK, 22);
                Vfx.beam(sl, staffTip(), at.add(0, 0.3, 0), VENOM, 0.25, 8, 0);
                sound(sl, position(), ModSounds.ARCHIMAGO_ORB.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
                face(p.position(), 60.0F);
            }
        }
        if (stateT >= 42) {
            flasksCd = 200 + this.random.nextInt(80);
            atkCd = gap();
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ bastonazo y parpadeo

    private void staffTick(ServerLevel sl, LivingEntity tgt) {
        if (stateT <= 10) {
            face(tgt.position(), 30.0F);
        }
        if (stateT == 4) {
            Vfx.ring(sl, position().add(0, 0.1, 0), WARN, 5.0, 10);
        }
        if (stateT == 13) {
            Vfx.ring(sl, position().add(0, 0.2, 0), ARCANE_PALE, 6.0, 9);
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.4, getZ(), 6, 2.0, 0.3, 2.0, 0.0);
            sound(sl, position(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.4F, 0.6F);
            for (ServerPlayer p : players(sl, 5.2)) {
                if (Math.abs(p.getY() - getY()) < 3.5) {
                    p.hurt(damageSources().mobAttack(this), 5.0F * dmgNow);
                    knockFromMe(p, 1.5, 0.45);
                }
            }
        }
        if (stateT >= 24) {
            atkCd = 14;
            if (blinkCd <= 30) {
                startBlink(sl, tgt, false);
            } else {
                setState(S_IDLE);
            }
        }
    }

    /** Se va al punto de la arena más lejos del jugador (o al centro) y deja un eco que estalla donde estaba. */
    private void startBlink(ServerLevel sl, LivingEntity tgt, boolean toCenter) {
        getNavigation().stop();
        if (toCenter) {
            blinkTo = home;
        } else {
            Vec3 best = home;
            double bd = -1.0;
            for (int i = 0; i < 10; i++) {
                Vec3 c = arenaPoint(this.random.nextDouble() * Math.PI * 2.0, 6.0 + this.random.nextDouble() * 8.0);
                double d = hd(c, tgt.position());
                if (d > bd) {
                    bd = d;
                    best = c;
                }
            }
            blinkTo = best;
        }
        closeTicks = 0;
        setState(S_BLINK);
    }

    private void blinkTick(ServerLevel sl, LivingEntity tgt) {
        if (stateT == 1) {
            sound(sl, position(), ModSounds.ARCHIMAGO_BLINK.get(), 2.4F, 0.9F);
            sl.sendParticles(ParticleTypes.PORTAL, getX(), getY() + 2.0, getZ(), 60, 0.6, 1.6, 0.6, 0.4);
        }
        if (stateT == 7) {
            addMark(sl, position(), 3.5, Mark.ECHO, 16);
            teleportTo(blinkTo.x, blinkTo.y, blinkTo.z);
            getNavigation().stop();
            sound(sl, blinkTo, ModSounds.ARCHIMAGO_BLINK.get(), 2.4F, 1.1F);
            sl.sendParticles(ParticleTypes.PORTAL, blinkTo.x, blinkTo.y + 2.0, blinkTo.z, 60, 0.6, 1.6, 0.6, 0.4);
            Vfx.flash(sl, blinkTo.add(0, 2.0, 0), ARCANE_PALE, 3.0, 6);
        }
        if (stateT > 7) {
            face(tgt.position(), 40.0F);
        }
        if (stateT >= 16) {
            blinkCd = 140 + this.random.nextInt(60);
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ escudo de sellos

    /**
     * Va al centro y levanta un escudo: casi no recibe daño mientras queden sellos. Aparecen tres columnas de luz en la arena; pisar
     * una la rompe. Rotos los tres, queda aturdido; si pasan 20 s, suelta una nova que golpea más cuantos más sellos queden.
     */
    private void startSeals(ServerLevel sl) {
        getNavigation().stop();
        teleportTo(home.x, home.y, home.z);
        sl.sendParticles(ParticleTypes.PORTAL, home.x, home.y + 2.0, home.z, 60, 0.6, 1.6, 0.6, 0.4);
        seals.clear();
        double a0 = this.random.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 3; i++) {
            seals.add(arenaPoint(a0 + i * (Math.PI * 2.0 / 3.0), 11.0));
        }
        this.entityData.set(SHIELD, 3);
        setState(S_SEALS);
        sound(sl, home, SoundEvents.BEACON_ACTIVATE, 4.0F, 0.7F);
        if (!warnedSeals) {
            warnedSeals = true;
            announce(sl, "Escudo de sellos", "Pisa las tres columnas de luz para romper su escudo");
        } else {
            announce(sl, "Escudo de sellos", "");
        }
    }

    private void sealsTick(ServerLevel sl, LivingEntity tgt) {
        int limit = 400;
        face(tgt.position(), 12.0F);
        if (stateT % 10 == 1) {
            Vfx.domeFollow(sl, this, shield() >= 2 ? GOLD : ARCANE, 3.2, 12);
            for (Vec3 s : seals) {
                Vfx.column(sl, s, GOLD, 1.1, 6.0, 12);
                Vfx.rune(sl, s.add(0, 0.08, 0), GOLD, 1.8, 12);
            }
        }
        // mientras canaliza sigue lanzando un rayo suelto de vez en cuando
        if (stateT % 70 == 30 && stateT < limit - 40) {
            Vec3 dir = dirTo(tgt.position());
            Vec3 from = staffTip();
            Vec3 feet = position().add(0, 1.0, 0);
            Vec3 end = feet.add(dir.scale(24.0));
            Vfx.beam(sl, from, end, WARN, 0.12, 14, 0);
            com.mundosdeaxelis.world.Scheduler.runLater(14, () -> {
                if (!isAlive() || state() != S_SEALS) {
                    return;
                }
                Vfx.beam(sl, from, end, ARCANE, 0.9, 7, 0);
                sound(sl, position(), ModSounds.ARCHIMAGO_CAST.get(), 2.0F, 1.05F);
                for (ServerPlayer p : players(sl, 28.0)) {
                    if (RelicFx.segDist(p.position().add(0, 1.0, 0), feet, end) <= 1.0) {
                        p.hurt(damageSources().indirectMagic(this, this), 6.0F * dmgNow);
                    }
                }
            });
        }
        Iterator<Vec3> it = seals.iterator();
        while (it.hasNext()) {
            Vec3 s = it.next();
            boolean stepped = false;
            for (ServerPlayer p : players(sl, 60.0)) {
                if (inCircle(p, s, 1.7)) {
                    stepped = true;
                    break;
                }
            }
            if (stepped) {
                it.remove();
                this.entityData.set(SHIELD, seals.size());
                Vfx.flash(sl, s.add(0, 1.0, 0), GOLD, 3.5, 8);
                Vfx.ring(sl, s.add(0, 0.1, 0), GOLD, 5.0, 12);
                sl.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 1.5, s.z, 30, 0.4, 1.5, 0.4, 0.15);
                sound(sl, s, SoundEvents.GLASS_BREAK, 3.0F, 1.2F);
                sound(sl, s, SoundEvents.AMETHYST_BLOCK_CHIME, 3.0F, 1.4F);
            }
        }
        if (seals.isEmpty()) {
            // escudo roto: aturdido, recibe más daño
            this.entityData.set(SHIELD, 0);
            sealsCd = 700;
            stunLen = 110;
            setState(S_STUN);
            sound(sl, position(), SoundEvents.BEACON_DEACTIVATE, 4.0F, 0.8F);
            Vfx.ring(sl, position().add(0, 0.2, 0), ARCANE_PALE, 9.0, 14);
            RelicFx.shakeNear(sl, position(), 20, 1.0F, 10);
            announce(sl, "¡Escudo roto!", "Está aturdido: recibe más daño");
            return;
        }
        if (stateT == limit - 30) {
            Vfx.ring(sl, position().add(0, 0.2, 0), WARN, 26.0, 30);
            Vfx.rune(sl, position().add(0, 0.1, 0), WARN, 8.0, 30);
            sound(sl, position(), ModSounds.ARCHIMAGO_CHARGE.get(), 4.0F, 0.7F);
        }
        if (stateT >= limit) {
            int left = seals.size();
            Vfx.dome(sl, position().add(0, 1.5, 0), VIOLET, 22.0, 12, true);
            Vfx.flash(sl, position().add(0, 2.0, 0), ARCANE_PALE, 10.0, 8);
            sound(sl, position(), ModSounds.ARCHIMAGO_BURST.get(), 4.0F, 0.7F);
            RelicFx.shakeNear(sl, position(), 30, 1.6F, 14);
            for (ServerPlayer p : players(sl, 26.0)) {
                p.hurt(damageSources().indirectMagic(this, this), 4.5F * left * dmgNow);
                knockFromMe(p, 1.4, 0.5);
            }
            seals.clear();
            this.entityData.set(SHIELD, 0);
            sealsCd = 600;
            atkCd = gap() + 20;
            setState(S_IDLE);
        }
    }

    private void stunTick(ServerLevel sl) {
        getNavigation().stop();
        if (stateT >= stunLen) {
            atkCd = 20;
            blinkCd = 0;
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ meteoritos

    private void startMeteors(ServerLevel sl) {
        getNavigation().stop();
        setState(S_METEORS);
        sound(sl, position(), ModSounds.ARCHIMAGO_CHARGE.get(), 3.0F, 0.8F);
        announce(sl, "Lluvia de meteoritos", "Sal de los círculos rojos");
    }

    /** Seis segundos de meteoritos: cada uno marca su círculo rojo algo más de un segundo antes de caer. */
    private void meteorsTick(ServerLevel sl, LivingEntity tgt) {
        face(tgt.position(), 10.0F);
        if (stateT >= 12 && stateT <= 120 && stateT % 7 == 0) {
            List<ServerPlayer> ps = players(sl, 50.0);
            if (!ps.isEmpty()) {
                ServerPlayer p = ps.get(this.random.nextInt(ps.size()));
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double r = this.random.nextDouble() * 4.5;
                addMark(sl, clampToArena(p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r)), 3.2, Mark.METEOR, 26);
            }
            // y otro al azar por la arena, para que no haya rincón seguro
            addMark(sl, arenaPoint(this.random.nextDouble() * Math.PI * 2.0, this.random.nextDouble() * ARENA_R), 3.2, Mark.METEOR, 30);
        }
        if (stateT >= 140) {
            meteorsCd = 320 + this.random.nextInt(100);
            atkCd = gap();
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ rayos giratorios

    private void startBeams(ServerLevel sl) {
        getNavigation().stop();
        teleportTo(home.x, home.y, home.z);
        sl.sendParticles(ParticleTypes.PORTAL, home.x, home.y + 2.0, home.z, 60, 0.6, 1.6, 0.6, 0.4);
        beamAngle = this.random.nextDouble() * Math.PI * 2.0;
        beamDir = this.random.nextBoolean() ? 1.0 : -1.0;
        setState(S_BEAMS);
        sound(sl, home, SoundEvents.BEACON_POWER_SELECT, 4.0F, 0.6F);
        if (!warnedBeams) {
            warnedBeams = true;
            announce(sl, "Rayos giratorios", "Corre en el sentido del giro; a mitad cambian de sentido");
        } else {
            announce(sl, "Rayos giratorios", "");
        }
    }

    /**
     * Desde el centro salen dos rayos opuestos (cuatro en cruz con poca vida) que barren la arena girando. Primero se ven finos
     * (aviso), luego hacen daño. A mitad se paran un instante y cambian de sentido.
     */
    private void beamsTick(ServerLevel sl) {
        int warm = 30;
        int total = 230;
        int half = 125;
        int n = getHealth() / getMaxHealth() < 0.15F ? 4 : 2;
        boolean paused = stateT >= half && stateT < half + 18;
        if (stateT == half) {
            beamDir = -beamDir;
            sound(sl, position(), SoundEvents.BEACON_POWER_SELECT, 3.0F, 1.2F);
        }
        if (stateT >= warm && !paused) {
            beamAngle += beamDir * Math.toRadians(1.5);
        }
        setYRot((float) Math.toDegrees(beamAngle) - 90.0F);
        this.yBodyRot = getYRot();
        this.yHeadRot = getYRot();
        boolean live = stateT >= warm && !paused;
        if (stateT % 2 == 0) {
            Vec3 c = position().add(0, 1.2, 0);
            for (int i = 0; i < n; i++) {
                double a = beamAngle + i * (Math.PI * 2.0 / n);
                Vec3 end = c.add(Math.cos(a) * 22.0, 0, Math.sin(a) * 22.0);
                if (live) {
                    Vfx.beam(sl, c, end, ARCANE, 0.8, 3, 0);
                } else {
                    Vfx.beam(sl, c, end, WARN, 0.12, 3, 0);
                }
            }
        }
        if (live) {
            Vec3 c = position().add(0, 1.0, 0);
            for (ServerPlayer p : players(sl, 24.0)) {
                if (beamCd.containsKey(p.getUUID()) || Math.abs(p.getY() - getY()) > 3.0) {
                    continue;
                }
                for (int i = 0; i < n; i++) {
                    double a = beamAngle + i * (Math.PI * 2.0 / n);
                    Vec3 end = c.add(Math.cos(a) * 22.0, 0, Math.sin(a) * 22.0);
                    if (RelicFx.segDist(new Vec3(p.getX(), c.y, p.getZ()), c, end) <= 0.9) {
                        p.hurt(damageSources().indirectMagic(this, this), 5.0F * dmgNow);
                        beamCd.put(p.getUUID(), 12);
                        break;
                    }
                }
            }
            if (stateT % 20 == 0) {
                sound(sl, position(), SoundEvents.BEACON_AMBIENT, 3.0F, 1.6F);
            }
        }
        if (stateT >= total) {
            beamsCd = 520 + this.random.nextInt(120);
            atkCd = gap() + 10;
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ fases, daño y victoria

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
        float max = BossBalance.realMax(target);
        if (Math.abs(max - getMaxHealth()) > 0.5F) {
            float frac = getHealth() / getMaxHealth();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
            setHealth(Mth.clamp(frac * max, 1.0F, max));
        }
    }

    private void enterPhase(ServerLevel sl, int ph) {
        this.entityData.set(PHASE, ph);
        this.bossEvent.setColor(barColor(ph));
        Vec3 at = position();
        RelicFx.shakeNear(sl, at, 22, 1.2F, 14);
        for (ServerPlayer p : players(sl, 10.0)) {
            knockFromMe(p, 1.2, 0.5);
        }
        BossFx.phase(sl, at, ph, ph == 2 ? VENOM : GOLD, ARCANE_PALE, ph == 2 ? "FASE II" : "FASE III", ph == 2 ? "LA ALQUIMIA" : "LAS ESTRELLAS",
                ParticleTypes.ENCHANT);
        // si estaba con el escudo puesto, se cae sin castigo
        if (state() == S_SEALS) {
            seals.clear();
            this.entityData.set(SHIELD, 0);
            setState(S_IDLE);
        }
        atkCd = 40;
        if (ph == 2) {
            pendingSeals = true;
        } else {
            pendingMeteors = true;
            beamsCd = Math.min(beamsCd, 200);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) {
            return false;
        }
        float a = amount * hpScale;
        if (shield() > 0) {
            // con el escudo en pie casi no le entra nada (y se nota: chispas doradas)
            a *= 0.12F;
            if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, getX(), getY() + 2.0, getZ(), 8, 0.8, 1.2, 0.8, 0.1);
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 1.4F);
            }
        }
        if (state() == S_STUN) {
            a *= 1.3F;
        }
        return super.hurt(source, a);
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && !victory) {
            victory = true;
            victory(sl);
        }
        setState(S_IDLE);
        super.die(source);
    }

    private void victory(ServerLevel sl) {
        this.bossEvent.setProgress(0.0F);
        marks.clear();
        puddles.clear();
        seals.clear();
        this.entityData.set(SHIELD, 0);
        Vec3 at = position();
        spawnAtLocation(new ItemStack(Items.AMETHYST_SHARD, 8 + random.nextInt(9)));
        spawnAtLocation(new ItemStack(Items.LAPIS_LAZULI, 10 + random.nextInt(9)));
        List<ServerPlayer> present = players(sl, 90.0);
        if (BossRewards.exists(STAGE)) {
            BossRewards.distribute(sl, this, STAGE, present);
        }
        for (ServerPlayer p : present) {
            ExperienceOrb.award(sl, p.position(), 400);
            p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }
        BossFx.defeat(sl, at, ARCANE, ARCANE_PALE, ParticleTypes.ENCHANT, Blocks.AMETHYST_BLOCK.defaultBlockState(), "ARCHIMAGO DERROTADO",
                "LA TORRE GUARDA SILENCIO");
        if (BossTravel.inArena(sl) && BossTravel.stageOf(sl) == STAGE) {
            sl.getServer().execute(() -> WorldTier.bossDefeated(sl.getServer(), STAGE));
            BossTravel.scheduleExit(sl);
        }
    }

    /** Ayuda para los comandos de prueba: quita lo que haya dejado el combate. */
    public void clearFight(ServerLevel sl) {
        marks.clear();
        puddles.clear();
        seals.clear();
        this.bossEvent.removeAllPlayers();
    }
}
