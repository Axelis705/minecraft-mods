package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Fx;
import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.vfx.VfxType;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.BossFx;
import com.mundosdeaxelis.world.BossRewards;
import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.FireArena;
import com.mundosdeaxelis.world.Scheduler;
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
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * El Corazón de Magma: jefe de la etapa 4 (el volcán). Un corazón de fuego del tamaño de una casa, cubierto de placas de
 * obsidiana, que late y cuelga de cadenas en el centro de la caverna.
 * <ul>
 *   <li><b>Encadenado (con escudo):</b> mientras quede alguna ancla en pie no le entra el daño. Ataca desde arriba: bombas de lava
 *   con aviso, gas volcánico en los respiraderos, larvas de magma, géiseres bajo los pies (fase 2+), el suelo que se derrumba a la
 *   lava (fase 2+), lluvia de rocas del techo y el latido que barre el suelo (fase 3).</li>
 *   <li><b>Sin cadenas:</b> al romper todas las anclas el corazón cae al suelo con la coraza agrietada y se le puede golpear unos
 *   segundos. Luego vuelve a subir y se encadena otra vez, con más anclas.</li>
 * </ul>
 */
public class CorazonMagma extends Monster {
    public static final int STAGE = 4;

    public static final int S_HIDDEN = 0;
    public static final int S_DESCEND = 1;
    public static final int S_CHAINED = 2;
    public static final int S_FALL = 3;
    public static final int S_EXPOSED = 4;
    public static final int S_RISE = 5;

    public static final int DEATH_TICKS = 90;
    public static final int PLATES = 14;

    private static final int LAVA = 0xFF6A1A;
    private static final int HOT = 0xFFC040;
    private static final int PALE = 0xFFF0B0;
    private static final int WARN = 0xFF3A2A;
    private static final int GAS = 0xC8D24A;
    private static final float DMG_MUL = BossBalance.dmgMul(STAGE);

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(CorazonMagma.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(CorazonMagma.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PLATES_LEFT = SynchedEntityData.defineId(CorazonMagma.class, EntityDataSerializers.INT);
    /** Coraza agrietada (0..100). */
    private static final EntityDataAccessor<Integer> CRACK = SynchedEntityData.defineId(CorazonMagma.class, EntityDataSerializers.INT);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Corazón de Magma"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);

    /** Algo marcado en el suelo que estalla al cabo de unos ticks. */
    private static final class Mark {
        static final int BOMB = 0;
        static final int GEYSER = 1;
        static final int METEOR = 2;
        final Vec3 pos;
        final int kind;
        int ticks;

        Mark(Vec3 pos, int kind, int ticks) {
            this.pos = pos;
            this.kind = kind;
            this.ticks = ticks;
        }
    }

    /** Nube de gas sobre un respiradero. */
    private static final class Cloud {
        final Vec3 pos;
        int warn;
        int life;

        Cloud(Vec3 pos, int warn, int life) {
            this.pos = pos;
            this.warn = warn;
            this.life = life;
        }
    }

    /** Trozo de suelo caído que vuelve a su sitio. */
    private static final class Hole {
        final List<int[]> cells;
        int warn;
        int back;

        Hole(List<int[]> cells, int warn, int back) {
            this.cells = cells;
            this.warn = warn;
            this.back = back;
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
    private int lastAtk = -1;
    private int exposeLen = 260;
    private boolean warnedShield;
    private boolean warnedExposed;
    private int waveR = -1;
    private Vec3 home;
    private Vec3 from;
    private final List<Mark> marks = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Hole> holes = new ArrayList<>();

    public CorazonMagma(EntityType<? extends CorazonMagma> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.noPhysics = true;
        setNoGravity(true);
        setCustomName(Component.literal("Corazón de Magma"));
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossBalance.realMax(BossBalance.hp(STAGE)))
                .add(Attributes.MOVEMENT_SPEED, 0.0D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, S_HIDDEN);
        this.entityData.define(PHASE, 1);
        this.entityData.define(PLATES_LEFT, PLATES);
        this.entityData.define(CRACK, 0);
    }

    // ------------------------------------------------------------------ lo que lee el renderer

    public int state() {
        return this.entityData.get(STATE);
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    public int plates() {
        return this.entityData.get(PLATES_LEFT);
    }

    public float crack() {
        return this.entityData.get(CRACK) / 100.0F;
    }

    public float stateAge(float partialTick) {
        if (this.level().isClientSide) {
            return Math.max(0, this.tickCount - this.clientStateStart) + partialTick;
        }
        return this.stateT + partialTick;
    }

    /** Ticks entre latidos: más rápido sin coraza, en fases altas y al morir. */
    public float beatPeriod() {
        float p = state() == S_EXPOSED ? 18F : 30F - 4F * (phase() - 1);
        return this.deathTicks > 0 ? 9F : p;
    }

    public float deathProgress(float partialTick) {
        return this.deathTicks > 0 ? Mth.clamp((this.deathTicks + partialTick) / DEATH_TICKS, 0.0F, 1.0F) : 0.0F;
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
    protected void pushEntities() {
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
        return SoundEvents.BASALT_BREAK;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WARDEN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected float getStandingEyeHeight(net.minecraft.world.entity.Pose pose, net.minecraft.world.entity.EntityDimensions dims) {
        return dims.height * 0.5F;
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
        tag.putInt("State", state());
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
        this.entityData.set(PHASE, Math.max(1, Math.min(3, tag.getInt("Phase"))));
        this.victory = tag.getBoolean("Victory");
        if (tag.contains("HomeX")) {
            this.home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
        }
        // si se guardó a media pelea, vuelve a empezar encadenado
        int st = tag.getInt("State");
        setState(st == S_HIDDEN || st == S_DESCEND ? st : S_RISE);
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }

    /** Dónde flota encadenado (centro de la caverna, en alto). */
    public void setHome(Vec3 hover) {
        this.home = hover;
    }

    // ------------------------------------------------------------------ ayudas

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.stateT = 0;
    }

    private List<ServerPlayer> players(ServerLevel sl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceTo(this) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    /** Todos los que pelean (también en creativo, para que el corazón no se quede sin objetivo en las pruebas). */
    private List<ServerPlayer> watchers(ServerLevel sl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceTo(this) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    private void announce(ServerLevel sl, String title, String sub) {
        for (ServerPlayer p : watchers(sl, 90.0)) {
            Titles.hint(p, title, sub);
        }
    }

    private void sound(ServerLevel sl, Vec3 at, SoundEvent s, float vol, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, s, SoundSource.HOSTILE, vol, pitch);
    }

    /** Suelo de la arena a pie (Y+1) en ese punto, recortado para que no se salga. */
    private static Vec3 floorPoint(double x, double z) {
        double dx = x - 0.5, dz = z - 0.5;
        double d = Math.hypot(dx, dz);
        double max = FireArena.R - 1.5;
        if (d > max) {
            dx = dx / d * max;
            dz = dz / d * max;
        }
        return new Vec3(0.5 + dx, FireArena.Y + 1.0, 0.5 + dz);
    }

    private Vec3 core() {
        return position().add(0, getBbHeight() * 0.5, 0);
    }

    private void knockFrom(ServerPlayer p, Vec3 c, double horizontal, double up) {
        Vec3 d = new Vec3(p.getX() - c.x, 0, p.getZ() - c.z);
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        p.setDeltaMovement(p.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        p.hurtMarked = true;
    }

    private int liveAnchors(ServerLevel sl) {
        int n = 0;
        for (AnclaMagma a : sl.getEntitiesOfClass(AnclaMagma.class, new AABB(home, home).inflate(40.0))) {
            if (a.isAlive()) {
                n++;
            }
        }
        return n;
    }

    private int fighters(ServerLevel sl) {
        int n = 0;
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator()) {
                n++;
            }
        }
        return Math.max(1, n);
    }

    // ------------------------------------------------------------------ intro (la llama el viaje)

    /** Empieza a bajar desde el techo (el viaje ya abrió la grieta). */
    public void startDescend() {
        this.from = position();
        setState(S_DESCEND);
    }

    // ------------------------------------------------------------------ tick

    @Override
    public void tick() {
        setDeltaMovement(Vec3.ZERO);
        super.tick();
        if (this.level().isClientSide) {
            clientFx();
        }
    }

    private void clientFx() {
        if (state() == S_HIDDEN) {
            return;
        }
        double h = getBbHeight();
        if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.DRIPPING_LAVA, getX() + (this.random.nextDouble() - 0.5) * 1.4, getY() + h * 0.2,
                    getZ() + (this.random.nextDouble() - 0.5) * 1.4, 0.0, 0.0, 0.0);
        }
        if (this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.FLAME, getX() + (this.random.nextDouble() - 0.5) * 2.2, getY() + h * (0.3 + this.random.nextDouble() * 0.6),
                    getZ() + (this.random.nextDouble() - 0.5) * 2.2, 0.0, 0.04, 0.0);
        }
        if (this.tickCount % 5 == 0) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (this.random.nextDouble() - 0.5) * 1.6, getY() + h * 0.95,
                    getZ() + (this.random.nextDouble() - 0.5) * 1.6, 0.0, 0.06, 0.0);
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
        if (this.tickCount % 20 == 0) {
            rescale(sl);
        }
        tickMarks(sl);
        tickClouds(sl);
        tickHoles(sl);
        tickWave(sl);
        // el latido se oye (y se siente cerca)
        int per = Math.round(beatPeriod());
        if (state() != S_HIDDEN && this.tickCount % per == 0) {
            sound(sl, core(), SoundEvents.WARDEN_HEARTBEAT, 4.0F, state() == S_EXPOSED ? 0.75F : 0.55F);
        }
        List<ServerPlayer> ps = watchers(sl, 80.0);
        if (ps.isEmpty() && state() != S_HIDDEN) {
            if (++idleNoPlayer > 300) {
                clearFight(sl);
                discard();
            }
            return;
        }
        idleNoPlayer = 0;
        stateT++;
        switch (state()) {
            case S_HIDDEN -> {
            }
            case S_DESCEND -> descendTick(sl);
            case S_CHAINED -> chainedTick(sl);
            case S_FALL -> fallTick(sl);
            case S_EXPOSED -> exposedTick(sl);
            case S_RISE -> riseTick(sl);
            default -> setState(S_CHAINED);
        }
    }

    private void rescale(ServerLevel sl) {
        int n = fighters(sl);
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

    private void moveTowards(Vec3 a, Vec3 b, double k) {
        Vec3 p = a.lerp(b, Mth.clamp(k, 0.0, 1.0));
        setPos(p.x, p.y, p.z);
    }

    // ------------------------------------------------------------------ bajar, encadenarse, caer, subir

    private void descendTick(ServerLevel sl) {
        int len = 90;
        double k = stateT / (double) len;
        double e = 1 - Math.pow(1 - Math.min(1, k), 3);
        moveTowards(from == null ? position() : from, home, e);
        if (stateT % 10 == 0) {
            RelicFx.shakeNear(sl, home, 60, 0.5F, 8);
        }
        if (stateT >= len) {
            setPos(home.x, home.y, home.z);
            chain(sl, true);
        }
    }

    /** Sale el número de anclas de esta fase y las cadenas se tensan: escudo puesto. */
    private void chain(ServerLevel sl, boolean first) {
        int want = phase() == 1 ? 3 : 4;
        int skip = want == 4 ? -1 : this.random.nextInt(4);
        int n = fighters(sl);
        float life = BossBalance.swordAvg(STAGE) * (6.0F + phase()) * (1.0F + 0.35F * (n - 1));
        int k = 0;
        for (int i = 0; i < 4; i++) {
            if (i == skip) {
                continue;
            }
            Vec3 at = FireArena.anchor(i).add(home.x - 0.5, home.y - FireArena.heartHome().y, home.z - 0.5);
            final int delay = k * 8;
            k++;
            Scheduler.runLater(delay, () -> {
                if (!isAlive() || victory) {
                    return;
                }
                AnclaMagma a = ModEntities.ANCLA_MAGMA.get().create(sl);
                if (a == null) {
                    return;
                }
                a.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
                a.setLife(life);
                sl.addFreshEntity(a);
                Vfx.column(sl, at, LAVA, 1.2, 10.0, 16);
                Vfx.ring(sl, at.add(0, 0.1, 0), HOT, 2.5, 14);
                sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 16, 0.4, 0.4, 0.4, 0.0);
                sound(sl, at, SoundEvents.CHAIN_PLACE, 3.0F, 0.6F);
                sound(sl, at, SoundEvents.RESPAWN_ANCHOR_CHARGE, 2.5F, 0.7F);
            });
        }
        this.entityData.set(CRACK, 0);
        this.entityData.set(PLATES_LEFT, PLATES - 2 * (phase() - 1));
        setState(S_CHAINED);
        atkCd = first ? 50 : 70;
        if (!warnedShield) {
            warnedShield = true;
            announce(sl, "Escudo de cadenas", "Rompe las anclas de cristal para que el corazón caiga");
        } else {
            announce(sl, "Se encadena otra vez", "Rompe sus anclas");
        }
    }

    private void chainedTick(ServerLevel sl) {
        // flota y se mece un poco
        double sway = Math.sin(this.tickCount * 0.04) * 0.25;
        setPos(home.x + sway * 0.4, home.y + Math.sin(this.tickCount * 0.07) * 0.2, home.z + sway * 0.3);
        if (stateT > 30 && liveAnchors(sl) == 0) {
            startFall(sl);
            return;
        }
        if (stateT % 10 == 1) {
            Vfx.domeFollow(sl, this, LAVA, 2.6, 12);
        }
        if (--atkCd > 0) {
            return;
        }
        int ph = phase();
        List<Integer> opts = new ArrayList<>(List.of(0, 1, 2));
        if (ph >= 2) {
            opts.add(3);
            opts.add(4);
        }
        if (ph >= 3) {
            opts.add(5);
            opts.add(6);
        }
        opts.remove(Integer.valueOf(lastAtk));
        int pick = opts.get(this.random.nextInt(opts.size()));
        lastAtk = pick;
        switch (pick) {
            case 0 -> bombs(sl);
            case 1 -> gas(sl);
            case 2 -> larvae(sl, 1 + ph);
            case 3 -> geysers(sl);
            case 4 -> collapse(sl);
            case 5 -> meteors(sl);
            default -> wave(sl);
        }
        atkCd = ph == 1 ? 80 : ph == 2 ? 64 : 52;
    }

    private void startFall(ServerLevel sl) {
        from = position();
        setState(S_FALL);
        sound(sl, core(), SoundEvents.CHAIN_BREAK, 4.0F, 0.5F);
        sound(sl, core(), SoundEvents.WARDEN_ROAR, 3.0F, 0.6F);
        announce(sl, "¡Las cadenas se rompen!", "");
    }

    private void fallTick(ServerLevel sl) {
        int len = 14;
        Vec3 ground = new Vec3(home.x, FireArena.Y + 1.0, home.z);
        double k = stateT / (double) len;
        moveTowards(from, ground, k * k);
        if (stateT >= len) {
            setPos(ground.x, ground.y, ground.z);
            Vec3 c = ground.add(0, 0.2, 0);
            Vfx.ring(sl, c, HOT, 7.0, 14);
            Vfx.flash(sl, c.add(0, 1.0, 0), PALE, 5.0, 10);
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 40, 2.0, 0.5, 2.0, 0.0);
            sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.x, c.y + 1.0, c.z, 30, 2.0, 0.6, 2.0, 0.02);
            sound(sl, c, SoundEvents.GENERIC_EXPLODE, 3.0F, 0.6F);
            sound(sl, c, SoundEvents.BASALT_BREAK, 4.0F, 0.5F);
            RelicFx.shakeNear(sl, c, 40, 1.4F, 14);
            for (ServerPlayer p : players(sl, 5.0)) {
                p.hurt(damageSources().mobAttack(this), 4.0F * dmgNow);
                knockFrom(p, c, 1.1, 0.5);
            }
            this.entityData.set(CRACK, 100);
            this.entityData.set(PLATES_LEFT, Math.max(4, plates() - 3));
            setState(S_EXPOSED);
            exposeLen = phase() == 3 ? 220 : 260;
            if (!warnedExposed) {
                warnedExposed = true;
                announce(sl, "¡Coraza rota!", "Golpéalo ahora, antes de que vuelva a subir");
            } else {
                announce(sl, "¡Coraza rota!", "");
            }
        }
    }

    private void exposedTick(ServerLevel sl) {
        Vec3 c = core();
        if (stateT % 6 == 0) {
            sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 6, 1.0, 1.0, 1.0, 0.03);
        }
        // a mitad del tiempo suelta un par de larvas para que no sea solo pegar
        if (stateT == exposeLen / 2) {
            larvae(sl, 1 + phase() / 2);
        }
        if (stateT == exposeLen - 40) {
            Vfx.ring(sl, position().add(0, 0.15, 0), WARN, 5.5, 40);
            sound(sl, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, 3.0F, 0.5F);
            announce(sl, "¡Va a subir!", "Apártate");
        }
        if (stateT >= exposeLen) {
            startRise(sl);
        }
    }

    private void startRise(ServerLevel sl) {
        Vec3 c = position().add(0, 0.3, 0);
        Vfx.flash(sl, c.add(0, 1.5, 0), HOT, 6.0, 10);
        Vfx.ring(sl, c, LAVA, 6.0, 12);
        for (ServerPlayer p : players(sl, 5.5)) {
            p.hurt(damageSources().mobAttack(this), 3.0F * dmgNow);
            knockFrom(p, c, 1.3, 0.6);
            p.setSecondsOnFire(3);
        }
        sound(sl, c, SoundEvents.BLAZE_SHOOT, 3.0F, 0.5F);
        from = position();
        setState(S_RISE);
    }

    private void riseTick(ServerLevel sl) {
        int len = 50;
        double k = stateT / (double) len;
        moveTowards(from == null ? position() : from, home, 1 - Math.pow(1 - Math.min(1, k), 2));
        int want = PLATES - 2 * (phase() - 1);
        if (stateT % 6 == 0 && plates() < want) {
            this.entityData.set(PLATES_LEFT, plates() + 1);
        }
        this.entityData.set(CRACK, (int) (100 * (1 - Math.min(1, k))));
        if (stateT >= len) {
            setPos(home.x, home.y, home.z);
            chain(sl, false);
        }
    }

    // ------------------------------------------------------------------ fases, daño y victoria

    private void enterPhase(ServerLevel sl, int ph) {
        this.entityData.set(PHASE, ph);
        this.bossEvent.setColor(ph >= 3 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.PINK);
        Vec3 at = core();
        RelicFx.shakeNear(sl, at, 50, 1.4F, 18);
        BossFx.phase(sl, at, ph, ph == 2 ? LAVA : HOT, PALE, ph == 2 ? "FASE II" : "FASE III", ph == 2 ? "EL VOLCÁN DESPIERTA" : "EL LATIDO FINAL",
                ParticleTypes.LAVA);
        // en cuanto cambia de fase vuelve a subir y se encadena con más anclas
        if (state() == S_EXPOSED) {
            stateT = Math.max(stateT, exposeLen - 30);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL) || source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        if (state() != S_EXPOSED) {
            // con la coraza y las cadenas no le entra nada
            if (this.level() instanceof ServerLevel sl && this.tickCount % 4 == 0) {
                Vec3 c = core();
                sl.sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, 6, 0.8, 0.8, 0.8, 0.1);
                sound(sl, c, SoundEvents.ANVIL_LAND, 1.0F, 1.6F);
                if (source.getEntity() instanceof ServerPlayer sp && this.tickCount % 40 < 4) {
                    Titles.actionBar(sp, "La coraza aguanta: rompe las anclas de las cadenas");
                }
            }
            return false;
        }
        float a = amount * hpScale * 1.15F;
        boolean ok = super.hurt(source, a);
        if (ok && this.level() instanceof ServerLevel sl) {
            Vec3 c = core();
            sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 4, 0.8, 0.8, 0.8, 0.0);
            // cada golpe fuerte arranca alguna placa
            if (amount > 4 && this.random.nextInt(3) == 0 && plates() > 2) {
                this.entityData.set(PLATES_LEFT, plates() - 1);
                sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
                        c.x, c.y, c.z, 14, 0.8, 0.8, 0.8, 0.2);
            }
            float frac = getHealth() / getMaxHealth();
            int ph = frac <= 0.33F ? 3 : frac <= 0.66F ? 2 : 1;
            if (ph > phase()) {
                enterPhase(sl, ph);
            }
        }
        return ok;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel sl && !victory) {
            victory = true;
            victory(sl);
        }
        super.die(source);
    }

    @Override
    protected void tickDeath() {
        this.deathTicks++;
        if (this.level() instanceof ServerLevel sl) {
            Vec3 c = core();
            if (this.deathTicks % 4 == 0) {
                sl.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 6, 1.0, 1.0, 1.0, 0.0);
                sl.sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 12, 1.2, 1.2, 1.2, 0.05);
            }
            if (this.deathTicks % 12 == 0) {
                sound(sl, c, SoundEvents.WARDEN_HEARTBEAT, 5.0F, 0.9F + this.deathTicks / (float) DEATH_TICKS);
                RelicFx.shakeNear(sl, c, 40, 0.6F + this.deathTicks / 60F, 8);
            }
            if (this.deathTicks == DEATH_TICKS - 6) {
                Vfx.flash(sl, c, PALE, 12.0, 14);
                Vfx.dome(sl, c, LAVA, 14.0, 14, false);
                sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.OBSIDIAN.defaultBlockState()),
                        c.x, c.y, c.z, 120, 1.5, 1.5, 1.5, 0.4);
                sound(sl, c, SoundEvents.GENERIC_EXPLODE, 5.0F, 0.5F);
                RelicFx.shakeNear(sl, c, 60, 2.0F, 20);
            }
        }
        if (this.deathTicks >= DEATH_TICKS && !isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    private void victory(ServerLevel sl) {
        this.bossEvent.setProgress(0.0F);
        clearFight(sl);
        Vec3 at = position();
        spawnAtLocation(new ItemStack(Items.MAGMA_BLOCK, 6 + random.nextInt(6)));
        spawnAtLocation(new ItemStack(Items.BLAZE_ROD, 4 + random.nextInt(5)));
        List<ServerPlayer> present = watchers(sl, 90.0);
        if (BossRewards.exists(STAGE)) {
            BossRewards.distribute(sl, this, STAGE, present);
        }
        for (ServerPlayer p : present) {
            ExperienceOrb.award(sl, p.position(), 350);
        }
        BossFx.defeat(sl, at, LAVA, PALE, ParticleTypes.LAVA, Blocks.MAGMA_BLOCK.defaultBlockState(), "CORAZÓN DE MAGMA DERROTADO",
                "EL VOLCÁN SE APAGA");
        if (BossTravel.inArena(sl) && BossTravel.stageOf(sl) == STAGE) {
            sl.getServer().execute(() -> WorldTier.bossDefeated(sl.getServer(), STAGE));
            BossTravel.scheduleExit(sl);
        }
    }

    /** Quita lo que haya dejado el combate: marcas, nubes, larvas, anclas y el suelo caído vuelve. */
    public void clearFight(ServerLevel sl) {
        marks.clear();
        clouds.clear();
        for (Hole h : holes) {
            FireArena.restore(sl, h.cells);
        }
        holes.clear();
        waveR = -1;
        AABB box = new AABB(home == null ? position() : home, home == null ? position() : home).inflate(48.0);
        for (AnclaMagma a : sl.getEntitiesOfClass(AnclaMagma.class, box)) {
            a.discard();
        }
        for (LarvaMagma l : sl.getEntitiesOfClass(LarvaMagma.class, box)) {
            l.discard();
        }
        this.bossEvent.removeAllPlayers();
    }

    // ------------------------------------------------------------------ ataques

    /** Orbe que vuela en arco de un punto a otro (lo mueve el servidor) y llama a {@code land} al llegar. */
    private void lob(ServerLevel sl, Vec3 a, Vec3 b, int ticks, double arc, int rgb, double size, Runnable land) {
        int id = Vfx.make(sl, VfxType.ORB).at(a).rgb(rgb).size(size).life(ticks + 6).flags(VfxType.F_TRAIL).send();
        for (int t = 1; t <= ticks; t++) {
            final double u = t / (double) ticks;
            final boolean last = t == ticks;
            Scheduler.runLater(t, () -> {
                if (isRemoved()) {
                    Vfx.remove(sl, id);
                    return;
                }
                Vec3 p = a.lerp(b, u).add(0, arc * 4 * u * (1 - u), 0);
                Vfx.make(sl, VfxType.ORB).at(p).rgb(rgb).size(size).flags(VfxType.F_TRAIL).life(0).update(id);
                if (last) {
                    Vfx.remove(sl, id);
                    land.run();
                }
            });
        }
    }

    private void bombs(ServerLevel sl) {
        List<ServerPlayer> ps = players(sl, 60.0);
        int n = 3 + phase();
        sound(sl, core(), SoundEvents.BLAZE_SHOOT, 3.0F, 0.6F);
        for (int i = 0; i < n; i++) {
            Vec3 tgt;
            if (!ps.isEmpty() && i < ps.size() * 2) {
                ServerPlayer p = ps.get(i % ps.size());
                tgt = floorPoint(p.getX() + (this.random.nextDouble() - 0.5) * 5, p.getZ() + (this.random.nextDouble() - 0.5) * 5);
            } else {
                double a = this.random.nextDouble() * Math.PI * 2, r = 4 + this.random.nextDouble() * 14;
                tgt = floorPoint(0.5 + Math.cos(a) * r, 0.5 + Math.sin(a) * r);
            }
            marks.add(new Mark(tgt, Mark.BOMB, 34 + i * 3));
            Vfx.ring(sl, tgt.add(0, 0.08, 0), WARN, 2.6, 34 + i * 3);
            final Vec3 t2 = tgt;
            Scheduler.runLater(6 + i * 3, () -> {
                if (isAlive()) {
                    lob(sl, core(), t2.add(0, 0.3, 0), 28, 6.0, LAVA, 0.5, () -> {
                    });
                }
            });
        }
    }

    private void gas(ServerLevel sl) {
        List<ServerPlayer> ps = players(sl, 60.0);
        List<Integer> vents = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
        // primero los respiraderos más cerca de los jugadores
        vents.sort((x, y) -> Double.compare(nearestTo(ps, FireArena.vent(x)), nearestTo(ps, FireArena.vent(y))));
        int n = Math.min(6, 2 + phase());
        for (int i = 0; i < n; i++) {
            Vec3 v = FireArena.vent(vents.get(i)).add(home.x - 0.5, 0, home.z - 0.5);
            clouds.add(new Cloud(v, 40, 140));
            Vfx.ring(sl, v.add(0, 0.1, 0), GAS, 3.5, 40);
            sound(sl, v, SoundEvents.FIRE_EXTINGUISH, 2.5F, 0.5F);
        }
        announce(sl, "¡Gas volcánico!", "Aléjate de los respiraderos");
    }

    private static double nearestTo(List<ServerPlayer> ps, Vec3 p) {
        double best = 1e9;
        for (ServerPlayer q : ps) {
            best = Math.min(best, q.position().distanceTo(p));
        }
        return best;
    }

    private void larvae(ServerLevel sl, int n) {
        AABB box = new AABB(home, home).inflate(40.0);
        int have = sl.getEntitiesOfClass(LarvaMagma.class, box).size();
        int cap = 3 + 2 * phase();
        n = Math.min(n, cap - have);
        if (n <= 0) {
            return;
        }
        sound(sl, core(), SoundEvents.MAGMA_CUBE_JUMP, 3.0F, 0.5F);
        float hp = BossBalance.swordAvg(STAGE) * 1.4F;
        float dmg = 2.5F * dmgNow;
        for (int i = 0; i < n; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = 5 + this.random.nextDouble() * 9;
            Vec3 tgt = floorPoint(home.x + Math.cos(a) * r, home.z + Math.sin(a) * r);
            lob(sl, core(), tgt, 22, 4.0, HOT, 0.35, () -> {
                LarvaMagma l = ModEntities.LARVA_MAGMA.get().create(sl);
                if (l == null || !isAlive()) {
                    return;
                }
                l.moveTo(tgt.x, tgt.y, tgt.z, this.random.nextFloat() * 360F, 0.0F);
                l.scaleTo(hp, dmg);
                sl.addFreshEntity(l);
                sl.sendParticles(ParticleTypes.LAVA, tgt.x, tgt.y + 0.2, tgt.z, 8, 0.3, 0.1, 0.3, 0.0);
                sound(sl, tgt, SoundEvents.MAGMA_CUBE_SQUISH, 1.6F, 0.8F);
            });
        }
    }

    private void geysers(ServerLevel sl) {
        List<ServerPlayer> ps = players(sl, 60.0);
        int i = 0;
        for (ServerPlayer p : ps) {
            if (i++ >= 6) {
                break;
            }
            Vec3 at = floorPoint(p.getX(), p.getZ());
            marks.add(new Mark(at, Mark.GEYSER, 30));
            Vfx.ring(sl, at.add(0, 0.08, 0), WARN, 1.9, 30);
            Vfx.rune(sl, at.add(0, 0.06, 0), LAVA, 1.6, 30);
        }
        sound(sl, core(), SoundEvents.LAVA_AMBIENT, 4.0F, 0.6F);
    }

    private void collapse(ServerLevel sl) {
        // el trozo donde más jugadores hay (anillo medio o de fuera)
        int bestRing = 1 + this.random.nextInt(2), bestSec = this.random.nextInt(FireArena.SECTORS), bestN = -1;
        for (int ring = 1; ring <= 2; ring++) {
            for (int s = 0; s < FireArena.SECTORS; s++) {
                int n = 0;
                for (ServerPlayer p : players(sl, 60.0)) {
                    double x = p.getX() - home.x, z = p.getZ() - home.z;
                    if (FireArena.ringOf(x, z) == ring && FireArena.sectorOf(x, z) == s) {
                        n++;
                    }
                }
                if (n > bestN) {
                    bestN = n;
                    bestRing = ring;
                    bestSec = s;
                }
            }
        }
        List<int[]> cells = FireArena.sectorCells(bestRing, bestSec);
        for (Hole h : holes) {
            if (!h.cells.isEmpty() && cells.size() == h.cells.size() && h.cells.get(0)[0] == cells.get(0)[0] && h.cells.get(0)[1] == cells.get(0)[1]) {
                return; // ese trozo ya está caído
            }
        }
        holes.add(new Hole(cells, 50, 50 + 220));
        sound(sl, core(), SoundEvents.BASALT_BREAK, 4.0F, 0.4F);
        announce(sl, "¡El suelo cruje!", "Sal de la zona que tiembla");
    }

    private void meteors(ServerLevel sl) {
        List<ServerPlayer> ps = players(sl, 60.0);
        int n = 6 + 2 * ps.size();
        sound(sl, core(), SoundEvents.WARDEN_ROAR, 3.0F, 0.8F);
        RelicFx.shakeNear(sl, core(), 60, 1.0F, 30);
        for (int i = 0; i < n; i++) {
            Vec3 tgt;
            if (!ps.isEmpty() && i % 2 == 0) {
                ServerPlayer p = ps.get(this.random.nextInt(ps.size()));
                tgt = floorPoint(p.getX() + (this.random.nextDouble() - 0.5) * 4, p.getZ() + (this.random.nextDouble() - 0.5) * 4);
            } else {
                double a = this.random.nextDouble() * Math.PI * 2, r = 3 + this.random.nextDouble() * 16;
                tgt = floorPoint(home.x + Math.cos(a) * r, home.z + Math.sin(a) * r);
            }
            int delay = 40 + i * 4;
            marks.add(new Mark(tgt, Mark.METEOR, delay));
            Vfx.ring(sl, tgt.add(0, 0.08, 0), WARN, 2.3, delay);
            final Vec3 t2 = tgt;
            Scheduler.runLater(delay - 18, () -> {
                if (isAlive()) {
                    Vec3 top = new Vec3(t2.x, FireArena.Y + 22.0, t2.z);
                    lob(sl, top, t2.add(0, 0.3, 0), 18, 0.0, 0xD8783A, 0.7, () -> {
                    });
                }
            });
        }
        announce(sl, "¡Se cae el techo!", "Esquiva las marcas");
    }

    private void wave(ServerLevel sl) {
        // se encoge, late fuerte y una onda de fuego barre el suelo desde el centro: hay que saltarla
        Vec3 c = new Vec3(home.x, FireArena.Y + 1.0, home.z);
        Vfx.ring(sl, c.add(0, 0.1, 0), WARN, 3.0, 30);
        sound(sl, core(), SoundEvents.RESPAWN_ANCHOR_CHARGE, 4.0F, 0.4F);
        announce(sl, "¡Latido!", "Salta cuando llegue la onda");
        Scheduler.runLater(30, () -> {
            if (isAlive() && !victory) {
                waveR = 2;
                sound(sl, core(), SoundEvents.WARDEN_SONIC_BOOM, 4.0F, 0.6F);
            }
        });
    }

    private void tickWave(ServerLevel sl) {
        if (waveR < 0) {
            return;
        }
        Vec3 c = new Vec3(home.x, FireArena.Y + 1.0, home.z);
        if (this.tickCount % 2 == 0) {
            Vfx.ring(sl, c.add(0, 0.25, 0), LAVA, waveR, 4);
            Fx.ring(sl, ParticleTypes.FLAME, c.x, c.y + 0.2, c.z, waveR, (int) (waveR * 6), this.tickCount * 0.1);
        }
        for (ServerPlayer p : players(sl, 60.0)) {
            double d = Math.hypot(p.getX() - c.x, p.getZ() - c.z);
            if (Math.abs(d - waveR) < 0.9 && p.onGround() && p.invulnerableTime <= 0) {
                p.hurt(damageSources().mobAttack(this), 5.0F * dmgNow);
                knockFrom(p, c, 0.8, 0.5);
                p.setSecondsOnFire(2);
            }
        }
        waveR += 1;
        if (waveR > FireArena.R + 1) {
            waveR = -1;
        }
    }

    private void tickMarks(ServerLevel sl) {
        Iterator<Mark> it = marks.iterator();
        while (it.hasNext()) {
            Mark m = it.next();
            if (--m.ticks > 0) {
                if (m.kind == Mark.GEYSER && m.ticks % 6 == 0) {
                    sl.sendParticles(ParticleTypes.LAVA, m.pos.x, m.pos.y + 0.1, m.pos.z, 2, 0.4, 0.0, 0.4, 0.0);
                }
                continue;
            }
            it.remove();
            switch (m.kind) {
                case Mark.BOMB -> {
                    Vfx.flash(sl, m.pos.add(0, 0.6, 0), HOT, 3.4, 8);
                    sl.sendParticles(ParticleTypes.LAVA, m.pos.x, m.pos.y + 0.3, m.pos.z, 12, 0.8, 0.2, 0.8, 0.0);
                    sl.sendParticles(ParticleTypes.FLAME, m.pos.x, m.pos.y + 0.4, m.pos.z, 24, 1.0, 0.4, 1.0, 0.05);
                    sound(sl, m.pos, SoundEvents.GENERIC_EXPLODE, 1.6F, 1.3F);
                    for (ServerPlayer p : players(sl, 60.0)) {
                        if (p.position().distanceTo(m.pos) <= 2.7) {
                            p.hurt(damageSources().mobAttack(this), 6.0F * dmgNow);
                            p.setSecondsOnFire(3);
                            knockFrom(p, m.pos, 0.6, 0.35);
                        }
                    }
                }
                case Mark.GEYSER -> {
                    Vfx.column(sl, m.pos, LAVA, 1.3, 9.0, 14);
                    sl.sendParticles(ParticleTypes.LAVA, m.pos.x, m.pos.y + 1.0, m.pos.z, 20, 0.5, 1.5, 0.5, 0.0);
                    sl.sendParticles(ParticleTypes.FLAME, m.pos.x, m.pos.y + 2.0, m.pos.z, 40, 0.5, 3.0, 0.5, 0.1);
                    sound(sl, m.pos, SoundEvents.BLAZE_SHOOT, 2.0F, 0.5F);
                    for (ServerPlayer p : players(sl, 60.0)) {
                        if (Math.hypot(p.getX() - m.pos.x, p.getZ() - m.pos.z) <= 1.9 && Math.abs(p.getY() - m.pos.y) < 4) {
                            p.hurt(damageSources().mobAttack(this), 5.0F * dmgNow);
                            p.setDeltaMovement(p.getDeltaMovement().x, 1.15, p.getDeltaMovement().z);
                            p.hurtMarked = true;
                            p.setSecondsOnFire(3);
                        }
                    }
                }
                default -> {
                    Vfx.flash(sl, m.pos.add(0, 0.6, 0), 0xFFA050, 3.0, 8);
                    sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, Blocks.BASALT.defaultBlockState()),
                            m.pos.x, m.pos.y + 0.4, m.pos.z, 30, 0.8, 0.3, 0.8, 0.15);
                    sl.sendParticles(ParticleTypes.LAVA, m.pos.x, m.pos.y + 0.3, m.pos.z, 6, 0.6, 0.2, 0.6, 0.0);
                    sound(sl, m.pos, SoundEvents.BASALT_BREAK, 2.0F, 0.7F);
                    sound(sl, m.pos, SoundEvents.GENERIC_EXPLODE, 1.2F, 1.5F);
                    for (ServerPlayer p : players(sl, 60.0)) {
                        if (p.position().distanceTo(m.pos) <= 2.4) {
                            p.hurt(damageSources().mobAttack(this), 7.0F * dmgNow);
                            knockFrom(p, m.pos, 0.7, 0.4);
                        }
                    }
                }
            }
        }
    }

    private void tickClouds(ServerLevel sl) {
        Iterator<Cloud> it = clouds.iterator();
        while (it.hasNext()) {
            Cloud c = it.next();
            if (c.warn > 0) {
                c.warn--;
                if (c.warn % 4 == 0) {
                    sl.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.pos.x, c.pos.y + 0.2, c.pos.z, 3, 0.2, 0.1, 0.2, 0.02);
                }
                if (c.warn == 0) {
                    sound(sl, c.pos, SoundEvents.FIRE_EXTINGUISH, 3.0F, 0.4F);
                    Vfx.flash(sl, c.pos.add(0, 1.0, 0), GAS, 3.0, 8);
                }
                continue;
            }
            if (--c.life <= 0) {
                it.remove();
                continue;
            }
            if (c.life % 3 == 0) {
                Fx.dustAt(sl, GAS, 2.6F, c.pos.x, c.pos.y + 1.0, c.pos.z, 6, 2.6);
                sl.sendParticles(ParticleTypes.SMOKE, c.pos.x, c.pos.y + 1.2, c.pos.z, 4, 2.0, 0.8, 2.0, 0.01);
            }
            if (c.life % 20 == 0) {
                for (ServerPlayer p : players(sl, 60.0)) {
                    if (Math.hypot(p.getX() - c.pos.x, p.getZ() - c.pos.z) <= 3.6 && Math.abs(p.getY() - c.pos.y) < 3.5) {
                        p.hurt(damageSources().magic(), 2.0F * dmgNow);
                        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1, false, true, true));
                        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true, true));
                    }
                }
            }
        }
    }

    private void tickHoles(ServerLevel sl) {
        Iterator<Hole> it = holes.iterator();
        while (it.hasNext()) {
            Hole h = it.next();
            if (h.warn > 0) {
                h.warn--;
                if (h.warn % 5 == 0 && !h.cells.isEmpty()) {
                    for (int k = 0; k < 6; k++) {
                        int[] c = h.cells.get(this.random.nextInt(h.cells.size()));
                        sl.sendParticles(ParticleTypes.LAVA, c[0] + 0.5, FireArena.Y + 1.05, c[1] + 0.5, 1, 0.2, 0.0, 0.2, 0.0);
                        sl.sendParticles(ParticleTypes.LARGE_SMOKE, c[0] + 0.5, FireArena.Y + 1.1, c[1] + 0.5, 1, 0.2, 0.0, 0.2, 0.01);
                    }
                    if (h.warn % 15 == 0) {
                        sound(sl, new Vec3(h.cells.get(0)[0], FireArena.Y + 1, h.cells.get(0)[1]), SoundEvents.BASALT_BREAK, 2.0F, 0.5F);
                    }
                }
                if (h.warn == 0) {
                    FireArena.drop(sl, h.cells);
                    sound(sl, new Vec3(h.cells.get(0)[0], FireArena.Y, h.cells.get(0)[1]), SoundEvents.GENERIC_EXPLODE, 2.5F, 0.6F);
                    RelicFx.shakeNear(sl, core(), 50, 0.8F, 10);
                }
            }
            if (--h.back <= 0) {
                FireArena.restore(sl, h.cells);
                it.remove();
            }
        }
    }
}
