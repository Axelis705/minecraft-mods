package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.network.ScreenFxPacket;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.BossFx;
import com.mundosdeaxelis.world.BossRewards;
import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.Cold;
import com.mundosdeaxelis.world.Frozen;
import com.mundosdeaxelis.world.Props;
import com.mundosdeaxelis.world.HeatZones;
import com.mundosdeaxelis.world.Scheduler;
import com.mundosdeaxelis.world.Titles;
import com.mundosdeaxelis.world.WorldTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * El Lobo Ártico: el jefe de la etapa 3 (Hielo). Un hombre lobo enorme con capucha y capa negras y dos guadañas de escarcha.
 * <ul>
 *   <li><b>Tajo y tajo cruzado</b>: cuerpo a cuerpo, con la guadaña que se carga y baja (se ve venir).</li>
 *   <li><b>Embestida</b>: se agacha, marca una línea en el hielo y, un instante después, la recorre entera; quien lo toque
 *       queda congelado. Si se estrella contra un muro queda aturdido (y recibe más daño).</li>
 *   <li><b>Aullido</b>: invoca una manada de lobeznos de escarcha (hay que repartirse y matarlos).</li>
 *   <li><b>Ventisca</b> (fase 2): gira con las guadañas, la arena se llena de nieve que tapa la vista y sube el frío:
 *       solo se aguanta en las zonas de calor que aparecen en el suelo.</li>
 *   <li><b>Luna de escarcha</b> (fase 3): toda la arena se vuelve hielo liso, el lobo patina a toda velocidad y del suelo
 *       brotan pinchos de hielo.</li>
 * </ul>
 * Los estados se sincronizan con el cliente ({@link #state()}) y el modelo (LoboArticoModel) trae una pose para cada uno.
 */
public class LoboArtico extends Monster {
    public static final String MINION_TAG = "lobo_artico_minion";
    /** Etapa del jefe: de ella salen su vida y su daño (ver {@link BossBalance}). */
    public static final int STAGE = 3;

    public static final int S_IDLE = 0;
    public static final int S_WINDUP = 1;
    public static final int S_DASH = 2;
    public static final int S_HOWL = 3;
    public static final int S_SLASH = 4;
    public static final int S_CROSS = 5;
    public static final int S_BLIZZARD = 6;
    public static final int S_MOON = 7;
    public static final int S_STUN = 8;
    public static final int S_SKATE = 9;

    /** Ticks que dura su muerte (se arrodilla y cae). */
    public static final int DEATH_TICKS = 70;

    private static final int ICE = 0x9FE3FF;
    private static final int ICE_PALE = 0xE8F8FF;
    private static final int ICE_DEEP = 0x4FA8E8;
    private static final float DMG_MUL = BossBalance.dmgMul(STAGE);

    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(LoboArtico.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(LoboArtico.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(LoboArtico.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Lobo Ártico"),
            BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);

    private float dmgNow = DMG_MUL;
    private float hpScale = 1.0F;

    /** Ticks en el estado actual (servidor). */
    private int stateT;
    /** Tick de cliente en que empezó el estado actual (para animar). */
    private int clientStateStart;
    /** Ticks de muerte (en ambos lados). */
    private int deathTicks;

    private int atkCd = 50;
    private int dashCd = 110;
    private int howlCd = 200;
    private int blizzCd = 330;
    private int moonCd = 260;
    private int slashCount;
    private boolean slashLeft;
    private boolean pendingRoar;
    private boolean roarHowl;
    private int idleNoPlayer;
    private boolean victory;
    private boolean warnedHowl;
    private boolean warnedBlizzard;
    private boolean warnedMoon;
    private boolean warnedStun;

    private Vec3 home;
    private Vec3 dashDir = Vec3.ZERO;
    private Vec3 dashStart = Vec3.ZERO;
    private double dashLen;
    private int dashMax;
    private int stunLen;
    private final Set<UUID> dashHit = new HashSet<>();
    private final Map<UUID, Integer> contactCd = new HashMap<>();
    private final Map<BlockPos, BlockState> iced = new HashMap<>();
    private int skateKick;
    private int spikeCd;

    public LoboArtico(EntityType<? extends LoboArtico> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setCustomName(Component.literal("Lobo Ártico"));
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossBalance.realMax(BossBalance.hp(STAGE)))
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, S_IDLE);
        this.entityData.define(PHASE, 1);
        this.entityData.define(VARIANT, 0);
    }

    // ------------------------------------------------------------------ lo que lee el modelo

    public int state() {
        return this.entityData.get(STATE);
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    /** Ticks (con decimales) que lleva en el estado actual: lo que necesita la animación. */
    public float stateAge(float partialTick) {
        if (this.level().isClientSide) {
            return Math.max(0, this.tickCount - this.clientStateStart) + partialTick;
        }
        return this.stateT + partialTick;
    }

    /** 0 mientras vive; sube de 0 a 1 durante su muerte. */
    public float deathProgress(float partialTick) {
        return this.deathTicks > 0 ? Mth.clamp((this.deathTicks + partialTick) / DEATH_TICKS, 0.0F, 1.0F) : 0.0F;
    }

    /** Cuánto brillan sus ojos y cristales (0 a 1.2), según lo que esté haciendo. */
    public float glowLevel(float age) {
        float base = 0.72F + 0.12F * Mth.sin(age * 0.11F) + 0.06F * (phase() - 1);
        return switch (state()) {
            case S_HOWL, S_MOON -> base + 0.4F;
            case S_BLIZZARD, S_DASH, S_SKATE -> base + 0.25F;
            case S_WINDUP -> base + 0.2F + 0.1F * Mth.sin(age * 0.9F);
            case S_STUN -> base - 0.3F;
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
    public boolean canFreeze() {
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
        return ModSounds.LOBO_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.LOBO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LOBO_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.2F;
    }

    @Override
    public float getVoicePitch() {
        return 1.0F;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        if (state() == S_SKATE) {
            return;
        }
        playSound(ModSounds.LOBO_STEP.get(), 1.3F, 0.85F + this.random.nextFloat() * 0.3F);
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return 4.1F;
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
        return ph >= 3 ? BossEvent.BossBarColor.PURPLE : ph == 2 ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.WHITE;
    }

    // ------------------------------------------------------------------ muerte lenta (sin el rojo de la muerte normal)

    @Override
    protected void tickDeath() {
        this.deathTicks++;
        if (this.level().isClientSide) {
            return;
        }
        if (this.deathTicks % 6 == 0 && this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2.0, getZ(), 14, 0.9, 1.6, 0.9, 0.04);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), getX(), getY() + 1.0, getZ(),
                    8, 0.7, 0.8, 0.7, 0.1);
        }
        if (this.deathTicks == DEATH_TICKS - 14 && this.level() instanceof ServerLevel sl) {
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.LOBO_CRASH.get(), SoundSource.HOSTILE, 3.0F, 0.8F);
            RelicFx.shakeNear(sl, position(), 24, 1.6F, 12);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX(), getY() + 0.3, getZ(),
                    60, 1.6, 0.3, 1.6, 0.3);
        }
        if (this.deathTicks >= DEATH_TICKS && !isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl) {
            restoreFloor(sl);
            HeatZones.clear(sl);
        }
        super.remove(reason);
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

    /** Partículas que solo ven los jugadores cercanos: aliento helado, nieve que levantan las patas, remolino de la ventisca. */
    private void clientFx() {
        if (isDeadOrDying()) {
            return;
        }
        int st = state();
        Vec3 f = forward();
        if (this.tickCount % 6 == 0 && st != S_BLIZZARD) {
            double hy = getY() + (st == S_STUN ? 2.6 : st == S_WINDUP || st == S_DASH ? 3.0 : 4.0);
            this.level().addParticle(ParticleTypes.CLOUD, getX() + f.x * 1.3, hy, getZ() + f.z * 1.3, f.x * 0.06, -0.01, f.z * 0.06);
        }
        double speed = Math.hypot(getDeltaMovement().x, getDeltaMovement().z);
        if ((speed > 0.12 || st == S_DASH || st == S_SKATE) && this.tickCount % 2 == 0) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (this.random.nextDouble() - 0.5) * 1.8, getY() + 0.1,
                        getZ() + (this.random.nextDouble() - 0.5) * 1.8, (this.random.nextDouble() - 0.5) * 0.1, 0.08, (this.random.nextDouble() - 0.5) * 0.1);
            }
        }
        if (st == S_BLIZZARD) {
            float t = stateAge(0.0F);
            if (t > 8.0F && t < 74.0F) {
                for (int i = 0; i < 8; i++) {
                    double a = this.tickCount * 0.55 + i * (Math.PI * 2.0 / 8.0);
                    double rr = 1.6 + this.random.nextDouble() * 4.2;
                    this.level().addParticle(ParticleTypes.SNOWFLAKE, getX() + Math.cos(a) * rr, getY() + 0.3 + this.random.nextDouble() * 3.4,
                            getZ() + Math.sin(a) * rr, -Math.sin(a) * 0.55, 0.02, Math.cos(a) * 0.55);
                }
            }
        }
        if (st == S_HOWL || st == S_MOON) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, getX() + (this.random.nextDouble() - 0.5) * 3.0, getY() + 3.5 + this.random.nextDouble() * 1.5,
                    getZ() + (this.random.nextDouble() - 0.5) * 3.0, 0.0, 0.05, 0.0);
        }
    }

    // ------------------------------------------------------------------ IA

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

    private void knock(ServerPlayer p, Vec3 away, double horizontal, double up) {
        Vec3 d = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
        p.setDeltaMovement(p.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        p.hurtMarked = true;
    }

    private void knockFromMe(ServerPlayer p, double horizontal, double up) {
        knock(p, new Vec3(p.getX() - getX(), 0, p.getZ() - getZ()), horizontal, up);
    }

    private int cap() {
        return phase() == 1 ? 3 : phase() == 2 ? 4 : 6;
    }

    private List<LoboEscarcha> minions(ServerLevel sl, double range) {
        AABB box = getBoundingBox().inflate(range);
        return sl.getEntitiesOfClass(LoboEscarcha.class, box, s -> s.getTags().contains(MINION_TAG) && s.isAlive());
    }

    private void announce(ServerLevel sl, String title, String sub) {
        for (ServerPlayer p : players(sl, 80.0)) {
            Titles.hint(p, title, sub);
        }
    }

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
        if (dashCd > 0) {
            dashCd--;
        }
        if (howlCd > 0) {
            howlCd--;
        }
        if (blizzCd > 0) {
            blizzCd--;
        }
        if (moonCd > 0) {
            moonCd--;
        }
        contactCd.replaceAll((k, v) -> v - 1);
        contactCd.values().removeIf(v -> v <= 0);

        ServerPlayer tgt = nearestPlayer(sl, 70.0);
        if (tgt == null) {
            idleNoPlayer++;
            setTarget(null);
            getNavigation().stop();
            if (idleNoPlayer > 300) {
                this.bossEvent.removeAllPlayers();
                for (LoboEscarcha s : minions(sl, 80.0)) {
                    s.discard();
                }
                discard();
            }
            return;
        }
        idleNoPlayer = 0;
        setTarget(tgt);
        stateT++;
        switch (state()) {
            case S_IDLE -> idleTick(sl, tgt);
            case S_WINDUP -> windupTick(sl, tgt);
            case S_DASH -> dashTick(sl);
            case S_HOWL -> howlTick(sl, tgt);
            case S_SLASH -> slashTick(sl, tgt);
            case S_CROSS -> crossTick(sl, tgt);
            case S_BLIZZARD -> blizzardTick(sl, tgt);
            case S_MOON -> moonTick(sl);
            case S_STUN -> stunTick(sl);
            case S_SKATE -> skateTick(sl, tgt);
            default -> setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ guardia y elección de ataque

    private void idleTick(ServerLevel sl, ServerPlayer tgt) {
        double dist = hd(tgt.position());
        face(tgt.position(), 22.0F);
        if (atkCd > 0) {
            atkCd--;
        }
        if (pendingRoar && atkCd <= 8) {
            pendingRoar = false;
            startHowl(sl, true);
            return;
        }
        if (atkCd <= 0) {
            if (phase() >= 3 && moonCd <= 0) {
                startMoon(sl);
                return;
            }
            if (phase() >= 2 && blizzCd <= 0) {
                startBlizzard(sl);
                return;
            }
            if (howlCd <= 0 && minions(sl, 60.0).size() <= cap() - 2) {
                startHowl(sl, false);
                return;
            }
            if (dashCd <= 0 && dist > 7.5) {
                startWindup(tgt);
                return;
            }
            if (dist <= 4.8) {
                slashCount++;
                boolean cross = (slashCount % 3 == 0 && this.random.nextFloat() < 0.8F) || (phase() >= 2 && this.random.nextFloat() < 0.25F);
                if (cross) {
                    setState(S_CROSS);
                } else {
                    slashLeft = !slashLeft;
                    this.entityData.set(VARIANT, slashLeft ? 1 : 0);
                    setState(S_SLASH);
                }
                getNavigation().stop();
                return;
            }
        }
        if (dist > 3.0) {
            chase(tgt, 1.0 + 0.12 * (phase() - 1));
        } else {
            getNavigation().stop();
        }
    }

    /** Camina hacia el objetivo (el camino se recalcula cada pocos ticks, no a cada tick). */
    private void chase(ServerPlayer tgt, double speed) {
        if (getNavigation().isDone() || this.tickCount % 8 == 0) {
            getNavigation().moveTo(tgt, speed);
        }
    }

    private int gap() {
        return phase() == 1 ? 26 : phase() == 2 ? 20 : 14;
    }

    // ------------------------------------------------------------------ tajos

    /** Los jugadores que quedan delante, a alcance de la guadaña. */
    private List<ServerPlayer> inArc(ServerLevel sl, double reach, double cosHalf) {
        Vec3 f = forward();
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : players(sl, reach + 3.0)) {
            double dx = p.getX() - getX();
            double dz = p.getZ() - getZ();
            double d = Math.hypot(dx, dz);
            double dy = p.getY() - getY();
            if (d > reach + 0.5 || dy < -2.5 || dy > 4.5) {
                continue;
            }
            double dot = d < 0.3 ? 1.0 : (dx * f.x + dz * f.z) / d;
            if (dot >= cosHalf) {
                out.add(p);
            }
        }
        return out;
    }

    private void slashTick(ServerLevel sl, ServerPlayer tgt) {
        int t = stateT;
        if (t <= 9) {
            face(tgt.position(), 30.0F);
        }
        if (t == 8) {
            playSound(ModSounds.LOBO_SLASH.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        if (t == 10) {
            setDeltaMovement(forward().scale(0.42).add(0, 0.05, 0));
            this.hasImpulse = true;
        }
        if (t == 12) {
            Vec3 f = forward();
            Vec3 c = position().add(f.scale(1.9)).add(0, 2.2, 0);
            Vfx.slash(sl, c, f.scale(3.0), ICE_PALE, 4.0, slashLeft ? 0.5 : -0.5, 9);
            Vfx.slash(sl, c.add(0, -0.1, 0), f.scale(3.0), ICE, 3.0, slashLeft ? 0.5 : -0.5, 11);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y - 0.6, c.z, 22, 1.4, 0.7, 1.4, 0.12);
            for (ServerPlayer p : inArc(sl, 5.0, -0.1)) {
                p.hurt(damageSources().mobAttack(this), (6.0F + phase()) * dmgNow);
                Cold.add(p, 7.0F);
                knockFromMe(p, 0.85, 0.3);
            }
        }
        if (t >= 22) {
            atkCd = gap();
            setState(S_IDLE);
        }
    }

    private void crossTick(ServerLevel sl, ServerPlayer tgt) {
        int t = stateT;
        if (t <= 17) {
            face(tgt.position(), 26.0F);
        }
        if (t == 15) {
            playSound(ModSounds.LOBO_CROSS.get(), 2.2F, 0.95F);
            Vfx.rune(sl, position().add(0, 0.1, 0), ICE_DEEP, 3.0, 18);
        }
        if (t == 17) {
            setDeltaMovement(forward().scale(0.5).add(0, 0.05, 0));
            this.hasImpulse = true;
        }
        if (t == 19) {
            Vec3 f = forward();
            Vec3 c = position().add(f.scale(2.2)).add(0, 2.0, 0);
            Vfx.slash(sl, c, f.scale(3.4), ICE_PALE, 5.0, 0.7, 10);
            Vfx.slash(sl, c, f.scale(3.4), ICE_PALE, 5.0, -0.7, 10);
            Vfx.ring(sl, position().add(0, 0.1, 0), ICE, 6.0, 12);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, c.x, c.y - 0.5, c.z, 40, 2.0, 0.8, 2.0, 0.15);
            RelicFx.shakeNear(sl, position(), 14, 0.7F, 8);
            for (ServerPlayer p : inArc(sl, 5.8, -0.45)) {
                p.hurt(damageSources().mobAttack(this), (9.0F + phase()) * dmgNow);
                Cold.add(p, 12.0F);
                knockFromMe(p, 1.2, 0.4);
            }
        }
        if (t >= 30) {
            atkCd = gap() + 6;
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ embestida

    private int windDur() {
        return phase() == 1 ? 30 : phase() == 2 ? 24 : 19;
    }

    private void startWindup(ServerPlayer tgt) {
        getNavigation().stop();
        dashDir = dirTo(tgt.position());
        setState(S_WINDUP);
        playSound(ModSounds.LOBO_CHARGE.get(), 2.4F, 1.0F);
    }

    private void windupTick(ServerLevel sl, ServerPlayer tgt) {
        int wind = windDur();
        int lock = wind - 13;
        if (stateT < lock) {
            dashDir = dirTo(tgt.position());
            faceDir(dashDir, 40.0F);
            if (stateT % 4 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX() - dashDir.x * 1.5, getY() + 0.2, getZ() - dashDir.z * 1.5, 8, 0.8, 0.1, 0.8, 0.12);
            }
        } else if (stateT == lock) {
            double dist = hd(tgt.position());
            dashLen = Mth.clamp(dist + 9.0, 15.0, 30.0);
            dashStart = position();
            faceDir(dashDir, 90.0F);
            // la línea marcada en el hielo: círculos que se encienden a lo largo del camino
            int n = (int) (dashLen / 2.6);
            for (int i = 1; i <= n; i++) {
                double d = i * 2.6;
                Vec3 pt = dashStart.add(dashDir.scale(d)).add(0, 0.08, 0);
                final int delay = i / 2;
                Scheduler.runLater(delay, () -> Vfx.ring(sl, pt, ICE, 1.3, 11));
                sl.sendParticles(ParticleTypes.SNOWFLAKE, pt.x, pt.y + 0.1, pt.z, 3, 0.4, 0.05, 0.4, 0.0);
            }
            Vec3 end = dashStart.add(dashDir.scale(dashLen)).add(0, 0.1, 0);
            Vfx.ring(sl, end, 0xFF8A6A, 2.6, 14);
            Vfx.rune(sl, dashStart.add(0, 0.1, 0), ICE_DEEP, 2.4, 13);
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.LOBO_FREEZE.get(), SoundSource.HOSTILE, 1.6F, 1.4F);
        } else {
            faceDir(dashDir, 90.0F);
            if (stateT % 3 == 0) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 6, 0.9, 0.1, 0.9, 0.1);
            }
        }
        if (stateT >= wind) {
            dashHit.clear();
            dashMax = (int) Math.ceil(dashLen / dashSpeed()) + 2;
            playSound(ModSounds.LOBO_DASH.get(), 2.6F, 1.0F);
            Vfx.flash(sl, position().add(0, 1.5, 0), ICE_PALE, 4.0, 6);
            setState(S_DASH);
        }
    }

    private double dashSpeed() {
        return 1.0 + 0.1 * (phase() - 1);
    }

    private void dashTick(ServerLevel sl) {
        double sp = dashSpeed();
        faceDir(dashDir, 90.0F);
        setDeltaMovement(dashDir.x * sp, Math.min(0.0, getDeltaMovement().y), dashDir.z * sp);
        this.hasImpulse = true;
        if (stateT % 2 == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.3, getZ(), 10, 0.7, 0.3, 0.7, 0.15);
            sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.4, getZ(), 3, 0.5, 0.2, 0.5, 0.05);
        }
        if (stateT % 3 == 0) {
            sl.sendParticles(Fx.dust(ICE, 1.2F), getX(), getY() + 2.2, getZ(), 4, 0.8, 1.4, 0.8, 0.0);
        }
        for (ServerPlayer p : players(sl, 6.0)) {
            if (dashHit.contains(p.getUUID())) {
                continue;
            }
            double dy = p.getY() - getY();
            if (hd(p.position()) <= 2.4 && dy > -2.5 && dy < 4.0) {
                dashHit.add(p.getUUID());
                p.hurt(damageSources().mobAttack(this), (8.0F + phase()) * dmgNow);
                Frozen.freeze(p, 9 + 2 * phase());
                Cold.add(p, 14.0F);
                // se lo lleva por delante hacia un lado
                Vec3 side = new Vec3(-dashDir.z, 0, dashDir.x);
                double sg = (p.getX() - getX()) * side.x + (p.getZ() - getZ()) * side.z >= 0 ? 1.0 : -1.0;
                knock(p, side.scale(sg), 0.9, 0.35);
                playSound(ModSounds.LOBO_FREEZE.get(), 2.0F, 0.9F);
            }
        }
        boolean wall = stateT > 3 && this.horizontalCollision;
        if (wall || stateT >= dashMax) {
            setDeltaMovement(0, getDeltaMovement().y, 0);
            if (wall) {
                playSound(ModSounds.LOBO_CRASH.get(), 3.0F, 1.0F);
                RelicFx.shakeNear(sl, position(), 18, 1.2F, 10);
                Vec3 f = forward();
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), getX() + f.x * 1.5, getY() + 1.8,
                        getZ() + f.z * 1.5, 50, 1.0, 1.0, 1.0, 0.25);
                Vfx.ring(sl, position().add(0, 0.1, 0), ICE_PALE, 5.0, 12);
                stunLen = 66 - 10 * (phase() - 1);
            } else {
                stunLen = 24;
            }
            dashCd = phase() == 1 ? 120 : phase() == 2 ? 95 : 75;
            setState(S_STUN);
        }
    }

    private void stunTick(ServerLevel sl) {
        getNavigation().stop();
        if (stateT == 1 && stunLen >= 40 && !warnedStun) {
            warnedStun = true;
            announce(sl, "¡Aturdido!", "Es tu momento: recibe más daño");
        }
        if (stateT % 8 == 1) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 2.3, getZ(), 8, 0.7, 0.4, 0.7, 0.02);
        }
        if (stateT >= stunLen) {
            atkCd = 14;
            setState(S_IDLE);
        }
    }

    // ------------------------------------------------------------------ aullido

    private void startHowl(ServerLevel sl, boolean roar) {
        getNavigation().stop();
        roarHowl = roar;
        setState(S_HOWL);
    }

    private void howlTick(ServerLevel sl, ServerPlayer tgt) {
        int t = stateT;
        getNavigation().stop();
        if (t < 16) {
            face(tgt.position(), 20.0F);
        }
        if (t == 10) {
            playSound(roarHowl ? ModSounds.LOBO_ROAR.get() : ModSounds.LOBO_HOWL.get(), 5.0F, 1.0F);
        }
        if (t == 16) {
            Vfx.ring(sl, position().add(0, 0.1, 0), ICE, 10.0, 22);
            Vfx.rune(sl, position().add(0, 0.1, 0), ICE_DEEP, 5.5, 40);
            Vfx.column(sl, position().add(0, 0.1, 0), ICE_PALE, 1.2, 9.0, 26);
            RelicFx.shakeNear(sl, position(), 22, 0.9F, 14);
            for (ServerPlayer p : players(sl, 9.0)) {
                knockFromMe(p, 0.9, 0.3);
                Cold.add(p, 8.0F);
            }
        }
        if (t == 26) {
            int want = roarHowl ? phase() + 2 : phase() + 1;
            summon(sl, tgt, want);
            if (!warnedHowl) {
                warnedHowl = true;
                announce(sl, "¡Aullido!", "Llega la manada: repartíos y matad a los lobeznos");
            }
        }
        if (t >= 62) {
            howlCd = phase() == 1 ? 440 : phase() == 2 ? 380 : 320;
            atkCd = 20;
            roarHowl = false;
            setState(S_IDLE);
        }
    }

    private void summon(ServerLevel sl, ServerPlayer tgt, int count) {
        int have = minions(sl, 70.0).size();
        count = Math.min(count, cap() - have);
        for (int i = 0; i < count; i++) {
            LoboEscarcha s = ModEntities.LOBO_ESCARCHA.get().create(sl);
            if (s == null) {
                return;
            }
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 3.0 + this.random.nextDouble() * 1.5;
            s.moveTo(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r, (float) Math.toDegrees(a) + 90.0F, 0.0F);
            s.addTag(MINION_TAG);
            s.getPersistentData().putBoolean("mdc_allowed", true); // sin esto la protección de la dungeon los borraba al aparecer
            s.setPersistenceRequired();
            s.boost(dmgNow);
            s.setTarget(tgt);
            sl.addFreshEntity(s);
            s.setDeltaMovement(Math.cos(a) * 0.35, 0.35, Math.sin(a) * 0.35);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, s.getX(), s.getY() + 0.5, s.getZ(), 14, 0.3, 0.4, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.CLOUD, s.getX(), s.getY() + 0.3, s.getZ(), 6, 0.2, 0.2, 0.2, 0.05);
        }
    }

    // ------------------------------------------------------------------ ventisca

    private void startBlizzard(ServerLevel sl) {
        getNavigation().stop();
        blizzCd = phase() == 2 ? 560 : 440;
        setState(S_BLIZZARD);
        playSound(ModSounds.LOBO_ROAR.get(), 4.0F, 1.2F);
        if (!warnedBlizzard) {
            warnedBlizzard = true;
            announce(sl, "¡Ventisca!", "Corre a las zonas de calor del suelo o te congelarás. Se mueven.");
        }
    }

    private void blizzardTick(ServerLevel sl, ServerPlayer tgt) {
        int t = stateT;
        if (t == 1) {
            heatWave(sl, 8, 42);
        } else if (t == 38) {
            heatWave(sl, 46, 82);
        }
        if (t % 4 == 0) {
            heatZoneFx(sl);
        }
        if (t == 6) {
            playSound(ModSounds.LOBO_WIND.get(), 6.0F, 1.0F);
            for (ServerPlayer p : players(sl, 90.0)) {
                Fx.screen(p, ScreenFxPacket.WHITEOUT, 0xE4F4FF, 78);
            }
        }
        if (t == 50) {
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.LOBO_WIND.get(), SoundSource.HOSTILE, 5.0F, 0.85F);
        }
        if (t >= 8 && t <= 72) {
            // se acerca despacio, girando
            chase(tgt, 0.5);
            if (t % 4 == 0) {
                for (ServerPlayer p : players(sl, 90.0)) {
                    if (Cold.heat(p) <= 0) {
                        Cold.zone(p, phase() == 2 ? 7.0F : 9.0F, 26);
                    }
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 1.4, p.getZ(), 10, 2.6, 1.2, 2.6, 0.2);
                    sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 1.2, p.getZ(), 2, 2.2, 1.0, 2.2, 0.02);
                }
            }
            if (t % 2 == 0) {
                Fx.ring(sl, ParticleTypes.SNOWFLAKE, getX(), getY() + 0.5 + this.random.nextDouble() * 3.0, getZ(), 3.0 + this.random.nextDouble() * 2.6, 10,
                        this.random.nextDouble() * 6.0);
            }
        } else {
            getNavigation().stop();
        }
        // las guadañas que giran
        if (t >= 14 && t <= 70 && t % 8 == 0) {
            for (ServerPlayer p : players(sl, 9.0)) {
                double dy = p.getY() - getY();
                if (hd(p.position()) <= 5.6 && dy > -2.5 && dy < 4.5) {
                    p.hurt(damageSources().mobAttack(this), (4.0F + phase()) * dmgNow);
                    Cold.add(p, 5.0F);
                    knockFromMe(p, 1.0, 0.35);
                }
            }
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.LOBO_SLASH.get(), SoundSource.HOSTILE, 1.4F, 0.7F + this.random.nextFloat() * 0.2F);
        }
        if (t >= 82) {
            atkCd = 30;
            HeatZones.clear(sl);
            heatCenters.clear();
            setState(S_IDLE);
        }
    }

    /** Zonas de calor de la ventisca: avisan unos ticks y luego protegen del frío hasta {@code endT}. */
    private final List<Vec3> heatCenters = new ArrayList<>();
    private long heatActiveFrom;
    private long heatActiveUntil;

    private void heatWave(ServerLevel sl, int startT, int endT) {
        HeatZones.clear(sl);
        heatCenters.clear();
        Vec3 c = home == null ? position() : home;
        int n = Math.max(2, Math.min(4, 1 + players(sl, 90.0).size()));
        long now = sl.getGameTime();
        heatActiveFrom = now + (startT - stateT);
        heatActiveUntil = now + (endT - stateT);
        for (int i = 0; i < n; i++) {
            Vec3 best = null;
            for (int tries = 0; tries < 40 && best == null; tries++) {
                double a = this.random.nextDouble() * Math.PI * 2.0;
                double d = 5.0 + this.random.nextDouble() * 10.0;
                Vec3 cand = new Vec3(c.x + Math.cos(a) * d, getY(), c.z + Math.sin(a) * d);
                boolean ok = hd(cand) > 9.0;
                for (Vec3 o : heatCenters) {
                    if (ok && Math.hypot(o.x - cand.x, o.z - cand.z) < 8.0) {
                        ok = false;
                    }
                }
                if (ok) {
                    best = cand;
                }
            }
            if (best == null) {
                best = new Vec3(c.x + Math.cos(i * 2.1) * 9.0, getY(), c.z + Math.sin(i * 2.1) * 9.0);
            }
            heatCenters.add(best);
            HeatZones.add(sl, best, HEAT_R, heatActiveFrom, heatActiveUntil);
            Vec3 f = best.add(0, 0.12, 0);
            Vfx.ring(sl, f, ICE_PALE, HEAT_R, startT - stateT);
            Vfx.rune(sl, f, HEAT_COLOR, HEAT_R, endT - stateT);
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    private static final double HEAT_R = 3.4;
    private static final int HEAT_COLOR = 0xFF9A3C;

    private void heatZoneFx(ServerLevel sl) {
        boolean active = sl.getGameTime() >= heatActiveFrom;
        for (Vec3 z : heatCenters) {
            Fx.ring(sl, active ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, z.x, z.y + 0.15, z.z, HEAT_R, 22, this.random.nextDouble() * 6.0);
            if (active) {
                sl.sendParticles(ParticleTypes.FLAME, z.x, z.y + 0.2, z.z, 3, HEAT_R * 0.5, 0.05, HEAT_R * 0.5, 0.02);
                sl.sendParticles(ParticleTypes.LAVA, z.x, z.y + 0.3, z.z, 1, HEAT_R * 0.4, 0.05, HEAT_R * 0.4, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------ luna de escarcha

    private void startMoon(ServerLevel sl) {
        getNavigation().stop();
        moonCd = 900;
        setState(S_MOON);
        if (!warnedMoon) {
            warnedMoon = true;
            announce(sl, "¡Luna de escarcha!", "El suelo se vuelve hielo: no podrás frenar. ¡Esquiva al lobo y a los pinchos!");
        }
    }

    private void moonTick(ServerLevel sl) {
        int t = stateT;
        getNavigation().stop();
        Vec3 c = home == null ? position() : home;
        if (t == 4) {
            playSound(ModSounds.LOBO_MOON.get(), 6.0F, 1.0F);
            sl.playSound(null, c.x, c.y, c.z, ModSounds.LOBO_HOWL.get(), SoundSource.HOSTILE, 4.0F, 0.8F);
            Vfx.orb(sl, c.add(0, 30, 0), 0xE4F4FF, 11.0, 150, false);
            Vfx.dome(sl, c.add(0, 30, 0), ICE, 14.0, 120, false);
            Vfx.rune(sl, c.add(0, 0.1, 0), ICE_PALE, 24.0, 90);
            Vfx.column(sl, position().add(0, 0.1, 0), ICE_PALE, 1.6, 22.0, 60);
            for (ServerPlayer p : players(sl, 90.0)) {
                Fx.screen(p, ScreenFxPacket.FLASH, 0xCFEFFF, 12);
            }
        }
        if (t >= 14 && t <= 44) {
            double r0 = (t - 14) / 30.0 * 24.0;
            double r1 = (t - 13) / 30.0 * 24.0;
            iceFloor(sl, c, r0, r1);
            Vfx.ring(sl, c.add(0, 0.12, 0), ICE_PALE, r1, 5);
            if (t % 5 == 0) {
                RelicFx.shakeNear(sl, c, 40, 0.6F, 6);
            }
        }
        if (t >= 50) {
            this.skateKick = 0;
            this.spikeCd = 20;
            setState(S_SKATE);
        }
    }

    private void iceFloor(ServerLevel sl, Vec3 c, double from, double to) {
        int fy = Mth.floor(c.y) - 1;
        int cx = Mth.floor(c.x);
        int cz = Mth.floor(c.z);
        int rad = (int) Math.ceil(to);
        for (int dx = -rad; dx <= rad; dx++) {
            for (int dz = -rad; dz <= rad; dz++) {
                double d = Math.hypot(dx, dz);
                if (d < from || d >= to) {
                    continue;
                }
                BlockPos bp = new BlockPos(cx + dx, fy, cz + dz);
                if (iced.containsKey(bp) || !sl.isLoaded(bp)) {
                    continue;
                }
                BlockState st = sl.getBlockState(bp);
                if (st.isAir() || st.is(Blocks.BLUE_ICE) || sl.getBlockEntity(bp) != null || !st.isSolidRender(sl, bp)) {
                    continue;
                }
                iced.put(bp.immutable(), st);
                sl.setBlock(bp, Blocks.BLUE_ICE.defaultBlockState(), 2 | 16);
                if (this.random.nextInt(14) == 0) {
                    sl.sendParticles(ParticleTypes.SNOWFLAKE, bp.getX() + 0.5, bp.getY() + 1.1, bp.getZ() + 0.5, 3, 0.3, 0.1, 0.3, 0.03);
                }
            }
        }
    }

    /** Devuelve el suelo a como estaba antes de la luna. */
    private void restoreFloor(ServerLevel sl) {
        if (iced.isEmpty()) {
            return;
        }
        int n = 0;
        List<BlockPos> done = new ArrayList<>();
        for (Map.Entry<BlockPos, BlockState> en : iced.entrySet()) {
            BlockPos bp = en.getKey();
            if (!sl.isLoaded(bp)) {
                continue;
            }
            if (sl.getBlockState(bp).is(Blocks.BLUE_ICE)) {
                sl.setBlock(bp, en.getValue(), 2 | 16);
                if (n++ % 9 == 0) {
                    sl.sendParticles(ParticleTypes.CLOUD, bp.getX() + 0.5, bp.getY() + 1.1, bp.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.03);
                }
            }
            done.add(bp);
        }
        for (BlockPos bp : done) {
            iced.remove(bp);
        }
    }

    private void skateTick(ServerLevel sl, ServerPlayer tgt) {
        int t = stateT;
        getNavigation().stop();
        Vec3 mv = getDeltaMovement();
        double speed = Math.hypot(mv.x, mv.z);
        if (speed > 0.15) {
            faceDir(new Vec3(mv.x, 0, mv.z), 25.0F);
        } else {
            face(tgt.position(), 25.0F);
        }
        // un empujón cada medio segundo largo hacia alguien (el hielo hace el resto)
        if (t % 34 == 2 && onGround()) {
            List<ServerPlayer> all = players(sl, 80.0);
            ServerPlayer pick = all.isEmpty() ? tgt : all.get(this.random.nextInt(all.size()));
            Vec3 d = dirTo(pick.position());
            setDeltaMovement(d.x * 1.15, getDeltaMovement().y, d.z * 1.15);
            this.hasImpulse = true;
            playSound(ModSounds.LOBO_DASH.get(), 2.2F, 1.15F);
        }
        if (this.horizontalCollision && speed > 0.2) {
            Vec3 m = getDeltaMovement();
            setDeltaMovement(-m.x * 0.5, m.y, -m.z * 0.5);
        }
        if (speed > 0.3 && t % 2 == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 8, 0.7, 0.1, 0.7, 0.12);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLUE_ICE.defaultBlockState()), getX(), getY() + 0.1, getZ(), 4, 0.5, 0.05, 0.5, 0.2);
        }
        if (speed > 0.35) {
            for (ServerPlayer p : players(sl, 6.0)) {
                double dy = p.getY() - getY();
                if (!contactCd.containsKey(p.getUUID()) && hd(p.position()) <= 2.4 && dy > -2.5 && dy < 4.0) {
                    contactCd.put(p.getUUID(), 26);
                    p.hurt(damageSources().mobAttack(this), (7.0F + phase()) * dmgNow);
                    Cold.add(p, 12.0F);
                    knockFromMe(p, 1.1, 0.35);
                    playSound(ModSounds.LOBO_SLASH.get(), 2.0F, 0.8F);
                }
            }
        }
        // pinchos de hielo bajo los jugadores
        if (--spikeCd <= 0) {
            spikeCd = 30;
            int n = 0;
            for (ServerPlayer p : players(sl, 70.0)) {
                if (n++ >= 3) {
                    break;
                }
                spike(sl, p.position().add(p.getDeltaMovement().x * 8.0, 0, p.getDeltaMovement().z * 8.0));
            }
        }
        if (t >= 150) {
            restoreFloor(sl);
            sl.playSound(null, getX(), getY(), getZ(), ModSounds.LOBO_CRASH.get(), SoundSource.HOSTILE, 2.0F, 1.2F);
            stunLen = 56;
            setState(S_STUN);
        }
    }

    private void spike(ServerLevel sl, Vec3 where) {
        double gy = RelicFx.groundY(sl, where.x, where.y + 1.0, where.z);
        Vec3 at = new Vec3(where.x, gy, where.z);
        Vfx.ring(sl, at.add(0, 0.1, 0), ICE, 1.9, 22);
        sl.playSound(null, at.x, at.y, at.z, ModSounds.LOBO_FREEZE.get(), SoundSource.HOSTILE, 1.0F, 1.6F);
        Scheduler.runLater(22, () -> {
            if (!isAlive() || isRemoved()) {
                return;
            }
            Quaternionf q = new Quaternionf().rotateY(this.random.nextFloat() * 6.28F).rotateZ((this.random.nextFloat() - 0.5F) * 0.3F);
            Props.Piece piece = Props.spawn(sl, Blocks.BLUE_ICE, at, 0.8F, 3.0F, 0.8F, false, 13, q);
            Props.Piece tip = Props.spawn(sl, Blocks.PACKED_ICE, at.add(0, 2.6, 0), 0.4F, 1.2F, 0.4F, false, 13, q);
            sl.playSound(null, at.x, at.y, at.z, ModSounds.LOBO_CRASH.get(), SoundSource.HOSTILE, 1.2F, 1.5F);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()), at.x, at.y + 0.5, at.z, 26, 0.5, 0.6, 0.5, 0.25);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y + 1.0, at.z, 16, 0.6, 1.0, 0.6, 0.1);
            for (ServerPlayer q2 : sl.players()) {
                if (q2.isAlive() && Math.hypot(q2.getX() - at.x, q2.getZ() - at.z) <= 1.9 && Math.abs(q2.getY() - at.y) < 2.5) {
                    q2.hurt(damageSources().mobAttack(this), (7.0F + phase()) * dmgNow);
                    Cold.add(q2, 12.0F);
                    q2.setDeltaMovement(q2.getDeltaMovement().add(0, 0.6, 0));
                    q2.hurtMarked = true;
                }
            }
            Scheduler.runLater(26, () -> {
                if (piece != null) {
                    piece.remove();
                }
                if (tip != null) {
                    tip.remove();
                }
            });
        });
    }

    // ------------------------------------------------------------------ vida, fases, daño

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
        for (ServerPlayer p : players(sl, 12.0)) {
            knockFromMe(p, 1.2, 0.5);
        }
        BossFx.phase(sl, at, ph, ICE, ICE_PALE, ph == 2 ? "FASE II" : "FASE III", ph == 2 ? "LA VENTISCA" : "LUNA DE ESCARCHA",
                ParticleTypes.SNOWFLAKE);
        pendingRoar = true;
        dashCd = Math.min(dashCd, 50);
        if (ph == 3) {
            moonCd = Math.min(moonCd, 220);
        } else {
            blizzCd = Math.min(blizzCd, 240);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FREEZING)) {
            return false;
        }
        float a = amount * hpScale;
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
        for (LoboEscarcha s : minions(sl, 90.0)) {
            s.discard();
        }
        restoreFloor(sl);
        Vec3 at = position();
        spawnAtLocation(new ItemStack(Items.BLUE_ICE, 6 + random.nextInt(7)));
        spawnAtLocation(new ItemStack(Items.PACKED_ICE, 10 + random.nextInt(9)));
        List<ServerPlayer> present = players(sl, 90.0);
        if (BossRewards.exists(STAGE)) {
            BossRewards.distribute(sl, this, STAGE, present);
        }
        for (ServerPlayer p : present) {
            ExperienceOrb.award(sl, p.position(), 300);
            p.removeEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        }
        BossFx.defeat(sl, at, ICE, ICE_PALE, ParticleTypes.SNOWFLAKE, Blocks.PACKED_ICE.defaultBlockState(), "LOBO ÁRTICO DERROTADO",
                "LA VENTISCA SE CALMA");
        if (BossTravel.inArena(sl) && BossTravel.stageOf(sl) == STAGE) {
            sl.getServer().execute(() -> WorldTier.bossDefeated(sl.getServer(), STAGE));
            BossTravel.scheduleExit(sl);
        }
    }

    /** Ayuda para los comandos de prueba: quita lo que haya dejado el combate. */
    public void clearFight(ServerLevel sl) {
        restoreFloor(sl);
        for (LoboEscarcha s : minions(sl, 120.0)) {
            s.discard();
        }
        this.bossEvent.removeAllPlayers();
    }
}
