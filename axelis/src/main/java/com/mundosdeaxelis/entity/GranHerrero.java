package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import com.mundosdeaxelis.world.BossBalance;
import com.mundosdeaxelis.world.BossTravel;
import com.mundosdeaxelis.world.Scheduler;
import com.mundosdeaxelis.world.WorldTier;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * El Gran Herrero de Brasas: el jefe de la etapa 2 (Forja). Un coloso de hierro y lava con un martillo enorme y un yunque a la
 * espalda. Fase 1: martillazos con onda de choque, lluvia de chispas y forja Escorias. Fase 2 (65 %): caen yunques y lanza anillos
 * de fuego con un hueco; además alterna entre «templado» (frío: su coraza aguanta casi todo) y «al rojo» (caliente: recibe más
 * daño, pero golpea más fuerte). Fase 3 (30 %): erupciones de lava y doble onda en cada martillazo.
 */
public class GranHerrero extends Monster {
    public static final String MINION_TAG = "gran_herrero_minion";
    /** Etapa del jefe: de ella salen su vida y su daño (ver {@link BossBalance}). */
    public static final int STAGE = 2;
    /** Ticks que tarda en levantar el martillo antes de golpear, y en recuperarse después (para animar el modelo). */
    public static final int WINDUP = 26;
    public static final int RECOVER = 14;
    private static final int ORANGE = 0xFF7A18;
    private static final float DMG_MUL = BossBalance.dmgMul(STAGE);

    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(GranHerrero.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLAM = SynchedEntityData.defineId(GranHerrero.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> POSE = SynchedEntityData.defineId(GranHerrero.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HOT = SynchedEntityData.defineId(GranHerrero.class, EntityDataSerializers.BOOLEAN);

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.literal("Gran Herrero de Brasas"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    private float dmgNow = DMG_MUL;
    /** Fracción del daño que recibe (la vida de diseño puede pasar del tope de 1024 del atributo). */
    private float hpScale = BossBalance.hpScale(BossBalance.hp(STAGE));
    private int busy = 0;
    private int gcd = 60;
    private int slamCd = 50;
    private int sparksCd = 150;
    private int forgeCd = 260;
    private int anvilCd = 200;
    private int ringCd = 300;
    private int eruptCd = 220;
    private int heatT = 0;
    private int meleeCd = 0;
    private int slamTick = -1;
    private Vec3 slamSpot = Vec3.ZERO;
    private int idleNoPlayer = 0;
    private boolean victory = false;
    private boolean warnedForge = false;
    private boolean warnedHeat = false;

    public GranHerrero(EntityType<? extends GranHerrero> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        setCustomName(Component.translatable("entity.mundos_de_axelis.gran_herrero"));
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BossBalance.hp(STAGE))
                .add(Attributes.MOVEMENT_SPEED, 0.23D)
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.ARMOR, 6.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 56.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, 1);
        this.entityData.define(SLAM, 0);
        this.entityData.define(POSE, 0);
        this.entityData.define(HOT, true);
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    /** Martillo: de 0 a {@link #WINDUP} mientras lo levanta; negativo mientras se recupera tras el golpe. */
    public int slamValue() {
        return this.entityData.get(SLAM);
    }

    /** 0 = normal; 1 = brazos en alto (rugido, forja, yunques, anillo). */
    public int pose() {
        return this.entityData.get(POSE);
    }

    public boolean isHot() {
        return this.entityData.get(HOT);
    }

    /** Cuánto brillan las partes incandescentes (el brillo baja cuando está «templado»). */
    public float glowLevel(float age) {
        float base = isHot() ? 0.85F : 0.3F;
        float pulse = 0.12F * Mth.sin(age * 0.15F);
        float charge = slamValue() > 0 ? 0.2F * slamValue() / WINDUP : 0.0F;
        return Mth.clamp(base + pulse + charge, 0.0F, 1.0F);
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
        return ModSounds.HERRERO_HURT.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.HERRERO_AMBIENT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HERRERO_DEATH.get();
    }

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(ModSounds.HERRERO_STEP.get(), 0.9F, 0.9F + random.nextFloat() * 0.2F);
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
        this.victory = tag.getBoolean("Victory");
        colorBar();
        if (hasCustomName()) {
            this.bossEvent.setName(getDisplayName());
        }
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getDisplayName());
    }

    private void colorBar() {
        this.bossEvent.setColor(phase() >= 3 ? BossEvent.BossBarColor.RED : phase() == 2 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.YELLOW);
    }

    // ------------------------------------------------------------------ tick

    /** Efectos solo visuales: humo por las chimeneas de la espalda, llamas en las puntas de los hombros y chispas del horno. */
    private void clientFx() {
        double yaw = Math.toRadians(this.yBodyRot);
        double fx = -Math.sin(yaw);
        double fz = Math.cos(yaw);
        double lx = Math.cos(yaw);
        double lz = Math.sin(yaw);
        boolean hot = isHot();
        if (this.tickCount % (hot ? 3 : 8) == 0) {
            // chimenea derecha (x -0,75) e izquierda (x +0,75), 0,94 hacia atrás
            double side = this.random.nextBoolean() ? -0.75 : 0.75;
            double top = side < 0 ? 5.8 : 5.44;
            this.level().addParticle(hot ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                    getX() + lx * side - fx * 0.94 + (random.nextDouble() - 0.5) * 0.2, getY() + top, getZ() + lz * side - fz * 0.94 + (random.nextDouble() - 0.5) * 0.2,
                    0.0, 0.06, 0.0);
        }
        if (hot && this.tickCount % 2 == 0) {
            double side = this.random.nextBoolean() ? -1.78 : 1.78;
            this.level().addParticle(ParticleTypes.FLAME, getX() + lx * side + (random.nextDouble() - 0.5) * 0.15, getY() + 4.9 + random.nextDouble() * 0.2,
                    getZ() + lz * side + (random.nextDouble() - 0.5) * 0.15, 0.0, 0.03, 0.0);
        }
        if (hot && this.tickCount % 9 == 0) {
            this.level().addParticle(ParticleTypes.LAVA, getX() + fx * 0.9 + (random.nextDouble() - 0.5) * 0.8, getY() + 3.0 + random.nextDouble() * 0.8,
                    getZ() + fz * 0.9 + (random.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            clientFx();
            return;
        }
        if (this.tickCount % 5 == 0 && this.level() instanceof ServerLevel sl && isHot()) {
            sl.sendParticles(ParticleTypes.FLAME, getX() + (random.nextDouble() - 0.5) * 2.0, getY() + 2.5 + random.nextDouble() * 1.5,
                    getZ() + (random.nextDouble() - 0.5) * 2.0, 1, 0.0, 0.02, 0.0, 0.0);
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

        ServerPlayer tgt = nearestPlayer(sl, 80.0);
        if (tgt == null) {
            idleNoPlayer++;
            setTarget(null);
            if (idleNoPlayer > 300) {
                // nadie vivo en la arena: se reinicia (volverá a aparecer en la siguiente entrada)
                this.bossEvent.removeAllPlayers();
                for (Escoria s : minions(sl, 90.0)) {
                    s.discard();
                }
                discard();
            }
            return;
        }
        idleNoPlayer = 0;
        setTarget(tgt);

        if (phase() >= 2) {
            tickHeat(sl);
        }
        if (meleeCd > 0) {
            meleeCd--;
        }
        if (gcd > 0) {
            gcd--;
        }

        // ----- martillazo en curso (aviso → golpe → recuperación)
        if (slamTick >= 0) {
            tickSlam(sl);
            return;
        }
        int sv = slamValue();
        if (sv < 0) {
            this.entityData.set(SLAM, sv + 1);
        }

        // ----- acción ocupada (rugido, forja, yunques...): quieto con los brazos en alto
        if (busy > 0) {
            busy--;
            getNavigation().stop();
            face(tgt.position());
            if (busy == 0) {
                this.entityData.set(POSE, 0);
            }
            return;
        }

        face(tgt.position());
        double dist = horizontalDistance(tgt.position());

        // ----- cooldowns
        slamCd--;
        sparksCd--;
        forgeCd--;
        if (phase() >= 2) {
            anvilCd--;
            ringCd--;
        }
        if (phase() >= 3) {
            eruptCd--;
        }

        // ----- elegir habilidad (una cada vez, con una pausa entre ellas)
        if (gcd <= 0) {
            if (phase() >= 3 && eruptCd <= 0) {
                startErupt(sl);
                return;
            }
            if (phase() >= 2 && ringCd <= 0) {
                startRing(sl);
                return;
            }
            if (phase() >= 2 && anvilCd <= 0) {
                startAnvils(sl);
                return;
            }
            if (forgeCd <= 0 && minions(sl, 90.0).size() < minionCap()) {
                startForge(sl);
                return;
            }
            if (sparksCd <= 0) {
                startSparks(sl);
                return;
            }
            if (slamCd <= 0 && dist < 9.0) {
                startSlam(sl, tgt);
                return;
            }
        }

        // ----- contacto: golpe corto de martillo
        double reach = getBbWidth() * 0.5 + 1.9;
        if (meleeCd <= 0 && dist < reach && Math.abs(tgt.getY() - getY()) < getBbHeight()) {
            swing(InteractionHand.MAIN_HAND);
            tgt.hurt(damageSources().mobAttack(this), dmg(6.0F + phase()));
            knock(tgt, 1.1, 0.4);
            meleeCd = 30;
            playSound(ModSounds.HERRERO_SWING.get(), 1.4F, 1.0F);
            playSound(ModSounds.HERRERO_LAND.get(), 0.9F, 0.8F);
        }

        // ----- caminar hacia el objetivo
        if (dist > reach - 0.6) {
            if (this.tickCount % 8 == 0) {
                getNavigation().moveTo(tgt, 1.0);
            }
        } else {
            getNavigation().stop();
        }
    }

    // ------------------------------------------------------------------ utilidades

    private float dmg(float base) {
        float hotOut = phase() >= 2 && isHot() ? 1.2F : 1.0F;
        return base * dmgNow * hotOut;
    }

    private int minionCap() {
        return phase() == 1 ? 3 : phase() == 2 ? 4 : 6;
    }

    private double horizontalDistance(Vec3 p) {
        return Math.hypot(p.x - getX(), p.z - getZ());
    }

    private void face(Vec3 p) {
        float yaw = (float) (Mth.atan2(p.z - getZ(), p.x - getX()) * (180.0F / Math.PI)) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
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

    private void knock(ServerPlayer p, double horizontal, double up) {
        Vec3 d = new Vec3(p.getX() - getX(), 0, p.getZ() - getZ());
        d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
        p.setDeltaMovement(p.getDeltaMovement().add(d.x * horizontal, up, d.z * horizontal));
        p.hurtMarked = true;
    }

    private void tell(ServerLevel sl, String msg, ChatFormatting color) {
        for (ServerPlayer p : players(sl, 90.0)) {
            p.displayClientMessage(Component.literal(msg).withStyle(color), true);
        }
    }

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
        float max = BossBalance.realMax(target); // el atributo de vida no pasa de 1024: el resto se compensa recortando el daño recibido
        if (Math.abs(max - getMaxHealth()) > 0.5F) {
            float frac = getHealth() / getMaxHealth();
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(max);
            setHealth(Mth.clamp(frac * max, 1.0F, max));
        }
    }

    private List<Escoria> minions(ServerLevel sl, double range) {
        AABB box = getBoundingBox().inflate(range);
        return sl.getEntitiesOfClass(Escoria.class, box, s -> s.getTags().contains(MINION_TAG) && s.isAlive());
    }

    private boolean playerWithin(ServerPlayer p, Vec3 spot, double r, double dy) {
        return p.isAlive() && !p.isSpectator() && Math.hypot(p.getX() - spot.x, p.getZ() - spot.z) <= r && Math.abs(p.getY() - spot.y) < dy;
    }

    /** Ya no prende fuego a nadie (la quemadura queda para la dungeon del dragón); se deja el método para no tocar los ataques. */
    private void ignite(ServerPlayer p, int seconds) {
    }

    // ------------------------------------------------------------------ calor («templado» / «al rojo»)

    private void tickHeat(ServerLevel sl) {
        heatT++;
        int limit = isHot() ? 260 : 240;
        if (heatT < limit) {
            return;
        }
        heatT = 0;
        boolean hot = !isHot();
        this.entityData.set(HOT, hot);
        playSound(ModSounds.HERRERO_TEMPER.get(), 2.0F, hot ? 1.2F : 0.7F);
        Vec3 c = position().add(0, 2.5, 0);
        if (hot) {
            Vfx.flash(sl, c, 0xFFB060, 5.0, 10);
            tell(sl, "¡Está al rojo vivo! ¡Golpéalo ahora, pero cuidado: pega más fuerte!", ChatFormatting.GOLD);
        } else {
            sl.sendParticles(ParticleTypes.CLOUD, c.x, c.y, c.z, 40, 1.2, 1.5, 1.2, 0.05);
            tell(sl, "Se templa: su coraza se endurece. Esquiva y espera a que se caliente.", ChatFormatting.AQUA);
        }
    }

    // ------------------------------------------------------------------ martillazo

    private void startSlam(ServerLevel sl, ServerPlayer tgt) {
        slamCd = phase() == 1 ? 110 : phase() == 2 ? 85 : 65;
        gcd = 25;
        slamTick = 0;
        double dx = tgt.getX() - getX();
        double dz = tgt.getZ() - getZ();
        double d = Math.hypot(dx, dz);
        double reach = Math.min(d, 8.0);
        slamSpot = d < 1.0E-3 ? position() : new Vec3(getX() + dx / d * reach, getY(), getZ() + dz / d * reach);
        Vfx.ring(sl, slamSpot.add(0, 0.1, 0), 0xFF5A1E, 4.2, WINDUP + 4);
        Vfx.rune(sl, slamSpot.add(0, 0.12, 0), 0xFFB040, 4.0, WINDUP + 4);
        playSound(ModSounds.HERRERO_CHARGE.get(), 2.0F, 1.0F);
        this.entityData.set(SLAM, 1);
    }

    private void tickSlam(ServerLevel sl) {
        getNavigation().stop();
        face(slamSpot);
        slamTick++;
        if (slamTick < WINDUP) {
            this.entityData.set(SLAM, slamTick);
            if (slamTick == WINDUP - 6) {
                playSound(ModSounds.HERRERO_SWING.get(), 2.2F, 0.85F);
            }
            return;
        }
        // golpe
        this.entityData.set(SLAM, -RECOVER);
        slamTick = -1;
        Vec3 at = slamSpot;
        playSound(ModSounds.HERRERO_SLAM.get(), 3.0F, 1.0F);
        Vfx.flash(sl, at.add(0, 0.6, 0), 0xFFB060, 4.5, 9);
        sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 24, 1.8, 0.2, 1.8, 0.2);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 40, 2.0, 0.2, 2.0, 0.12);
        RelicFx.shakeNear(sl, at, 18, 1.0F, 10);
        for (ServerPlayer p : players(sl, 12.0)) {
            if (playerWithin(p, at, 4.2, 3.0)) {
                p.hurt(damageSources().mobAttack(this), dmg(9.0F + 2.0F * (phase() - 1)));
                knock(p, 0.9, 0.5);
                if (phase() >= 2) {
                    ignite(p, 2);
                }
            }
        }
        shockwave(sl, at, 4.5, 0.55, 15, dmg(5.0F + phase()));
        if (phase() >= 3) {
            Scheduler.runLater(12, () -> {
                if (isAlive() && !victory) {
                    shockwave(sl, at, 4.5, 0.55, 15, dmg(5.0F + phase()));
                }
            });
        }
    }

    /** Onda que se expande desde el golpe: la esquivas saltando (más de 1,3 bloques sobre el suelo del jefe). */
    private void shockwave(ServerLevel sl, Vec3 at, double startR, double speed, int steps, float damage) {
        Set<UUID> hit = new HashSet<>();
        for (int k = 1; k <= steps; k++) {
            final double r = startR + k * speed * 2.0;
            Scheduler.runLater(k * 2, () -> {
                if (!isAlive() && !victory) {
                    return;
                }
                Vfx.ring(sl, at.add(0, 0.1, 0), 0xFF8A2A, r, 5);
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive() || p.isSpectator() || hit.contains(p.getUUID())) {
                        continue;
                    }
                    double d = Math.hypot(p.getX() - at.x, p.getZ() - at.z);
                    if (Math.abs(d - r) <= 1.5 && p.getY() - at.y < 1.3) {
                        hit.add(p.getUUID());
                        p.hurt(damageSources().mobAttack(this), damage);
                        knock(p, 0.7, 0.35);
                        ignite(p, 2);
                    }
                }
            });
        }
    }

    // ------------------------------------------------------------------ chispas

    private void startSparks(ServerLevel sl) {
        sparksCd = phase() == 1 ? 200 : phase() == 2 ? 160 : 120;
        gcd = 25;
        busy = 20;
        playSound(ModSounds.HERRERO_SPARKS.get(), 2.0F, 1.0F);
        int budget = 3 + phase() * 2;
        List<ServerPlayer> ps = players(sl, 60.0);
        for (int i = 0; i < budget && !ps.isEmpty(); i++) {
            ServerPlayer p = ps.get(i % ps.size());
            double a = random.nextDouble() * Math.PI * 2;
            double r = i < ps.size() ? 0.0 : 1.5 + random.nextDouble() * 3.0;
            Vec3 spot = new Vec3(p.getX() + Math.cos(a) * r, getY(), p.getZ() + Math.sin(a) * r);
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0xFFA030, 2.2, 22);
            Scheduler.runLater(22, () -> sparkBurst(sl, spot));
        }
    }

    private void sparkBurst(ServerLevel sl, Vec3 spot) {
        if (!isAlive() || victory) {
            return;
        }
        sl.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.3, spot.z, 14, 1.0, 0.2, 1.0, 0.2);
        sl.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.4, spot.z, 26, 0.9, 0.5, 0.9, 0.1);
        sl.playSound(null, spot.x, spot.y, spot.z, ModSounds.HERRERO_BURST.get(), SoundSource.HOSTILE, 1.2F, 1.0F);
        for (ServerPlayer p : sl.players()) {
            if (playerWithin(p, spot, 2.2, 2.5)) {
                p.hurt(damageSources().mobAttack(this), dmg(5.0F + phase()));
                ignite(p, 3);
            }
        }
    }

    // ------------------------------------------------------------------ forja de Escorias

    private void startForge(ServerLevel sl) {
        forgeCd = phase() == 1 ? 420 : phase() == 2 ? 340 : 280;
        gcd = 30;
        busy = 40;
        this.entityData.set(POSE, 1);
        playSound(ModSounds.HERRERO_ANVIL.get(), 2.5F, 0.8F);
        Vfx.flash(sl, position().add(0, 2.5, 0), 0xFFB060, 4.0, 10);
        int count = Math.min(phase() + 1, minionCap() - minions(sl, 90.0).size());
        final int players = Math.max(1, sl.players().size());
        for (int i = 0; i < count; i++) {
            Scheduler.runLater(16 + i * 6, () -> spawnEscoria(sl, players));
        }
        if (!warnedForge) {
            warnedForge = true;
            tell(sl, "¡Forja Escorias en su yunque! Acaba con ellas o te rodearán.", ChatFormatting.YELLOW);
        }
    }

    private void spawnEscoria(ServerLevel sl, int players) {
        if (!isAlive() || victory) {
            return;
        }
        Escoria e = ModEntities.ESCORIA.get().create(sl);
        if (e == null) {
            return;
        }
        double a = random.nextDouble() * Math.PI * 2;
        double r = getBbWidth() * 0.5 + 2.2;
        e.moveTo(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r, random.nextFloat() * 360.0F, 0.0F);
        e.scaleTo(STAGE, players);
        e.addTag(MINION_TAG);
        e.setPersistenceRequired();
        ServerPlayer t = nearestPlayer(sl, 80.0);
        if (t != null) {
            e.setTarget(t);
        }
        sl.addFreshEntity(e);
        e.setDeltaMovement(Math.cos(a) * 0.3, 0.4, Math.sin(a) * 0.3);
        sl.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 0.5, e.getZ(), 14, 0.3, 0.4, 0.3, 0.05);
        sl.playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.HERRERO_CLINK.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
    }

    // ------------------------------------------------------------------ yunques que caen

    private void startAnvils(ServerLevel sl) {
        anvilCd = phase() == 2 ? 260 : 190;
        gcd = 30;
        busy = 36;
        this.entityData.set(POSE, 1);
        playSound(ModSounds.HERRERO_CHARGE.get(), 2.0F, 0.8F);
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : players(sl, 60.0)) {
            spots.add(p.position());
        }
        int extra = phase() == 2 ? 2 : 4;
        for (int i = 0; i < extra; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 4.0 + random.nextDouble() * 18.0;
            spots.add(new Vec3(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r));
        }
        for (Vec3 s : spots) {
            Vec3 spot = new Vec3(s.x, getY(), s.z);
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0xFFD060, 2.4, 34);
            Vfx.rune(sl, spot.add(0, 0.12, 0), 0xFFB040, 2.2, 34);
            Scheduler.runLater(34, () -> anvilDrop(sl, spot));
        }
    }

    private void anvilDrop(ServerLevel sl, Vec3 spot) {
        if (!isAlive() || victory) {
            return;
        }
        Vfx.column(sl, spot, 0xFFB050, 1.0, 8.0, 7);
        Vfx.flash(sl, spot.add(0, 0.5, 0), 0xFFE0A0, 3.0, 7);
        sl.sendParticles(ParticleTypes.CRIT, spot.x, spot.y + 0.4, spot.z, 30, 1.0, 0.3, 1.0, 0.3);
        sl.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.4, spot.z, 8, 0.8, 0.2, 0.8, 0.2);
        sl.playSound(null, spot.x, spot.y, spot.z, ModSounds.HERRERO_ANVIL.get(), SoundSource.HOSTILE, 2.0F, 0.9F);
        RelicFx.shakeNear(sl, spot, 10, 0.5F, 6);
        for (ServerPlayer p : sl.players()) {
            if (playerWithin(p, spot, 2.4, 3.0)) {
                p.hurt(damageSources().mobAttack(this), dmg(10.0F + phase()));
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.35, 0));
                p.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ anillo de fuego con hueco

    private void startRing(ServerLevel sl) {
        ringCd = phase() == 2 ? 340 : 240;
        gcd = 40;
        busy = 34;
        this.entityData.set(POSE, 1);
        playSound(ModSounds.HERRERO_ROAR.get(), 2.5F, 1.1F);
        Vec3 c = position();
        double gapCenter = random.nextDouble() * Math.PI * 2;
        double gapHalf = Math.toRadians(phase() == 2 ? 40 : 30);
        tell(sl, "¡Anillo de fuego! Busca el hueco o salta por encima.", ChatFormatting.RED);
        // marca el hueco con columnas de luz
        for (int s = -1; s <= 1; s++) {
            double a = gapCenter + s * gapHalf;
            Vec3 m = new Vec3(c.x + Math.cos(a) * 9.0, c.y, c.z + Math.sin(a) * 9.0);
            Vfx.column(sl, m, 0x9AE0FF, 0.35, 3.0, 36);
        }
        final Set<UUID> hit = new HashSet<>();
        for (int k = 0; k < 32; k++) {
            final double r = 3.0 + k * 0.85;
            Scheduler.runLater(24 + k, () -> {
                if (!isAlive() || victory) {
                    return;
                }
                Vfx.ring(sl, c.add(0, 0.1, 0), 0xFF5A1E, r, 4);
                for (int j = 0; j < 14; j++) {
                    double a = random.nextDouble() * Math.PI * 2;
                    if (angleGap(a, gapCenter) < gapHalf) {
                        continue;
                    }
                    sl.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.0, 0.1, 0.0, 0.0);
                }
                for (ServerPlayer p : sl.players()) {
                    if (!p.isAlive() || p.isSpectator() || hit.contains(p.getUUID())) {
                        continue;
                    }
                    double dx = p.getX() - c.x;
                    double dz = p.getZ() - c.z;
                    double d = Math.hypot(dx, dz);
                    if (Math.abs(d - r) > 1.1 || p.getY() - c.y >= 1.3) {
                        continue;
                    }
                    if (angleGap(Math.atan2(dz, dx), gapCenter) < gapHalf) {
                        continue;
                    }
                    hit.add(p.getUUID());
                    p.hurt(damageSources().mobAttack(this), dmg(7.0F + phase()));
                    knock(p, 0.6, 0.3);
                    ignite(p, 3);
                }
            });
        }
    }

    private static double angleGap(double a, double b) {
        double d = Math.abs(a - b) % (Math.PI * 2);
        return d > Math.PI ? Math.PI * 2 - d : d;
    }

    // ------------------------------------------------------------------ erupciones (fase 3)

    private void startErupt(ServerLevel sl) {
        eruptCd = 200;
        gcd = 30;
        busy = 26;
        this.entityData.set(POSE, 1);
        playSound(ModSounds.HERRERO_ROAR.get(), 2.5F, 0.8F);
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : players(sl, 60.0)) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 2.0;
            spots.add(new Vec3(p.getX() + Math.cos(a) * r, getY(), p.getZ() + Math.sin(a) * r));
        }
        for (int i = 0; i < 3; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 5.0 + random.nextDouble() * 18.0;
            spots.add(new Vec3(getX() + Math.cos(a) * r, getY(), getZ() + Math.sin(a) * r));
        }
        for (Vec3 spot : spots) {
            Vfx.ring(sl, spot.add(0, 0.1, 0), 0xFF3A10, 2.3, 28);
            Vfx.rune(sl, spot.add(0, 0.12, 0), 0xFF7A2A, 2.1, 28);
            Scheduler.runLater(28, () -> erupt(sl, spot));
        }
    }

    private void erupt(ServerLevel sl, Vec3 spot) {
        if (!isAlive() || victory) {
            return;
        }
        Vfx.column(sl, spot, 0xFF5A1E, 1.4, 7.0, 10);
        Vfx.flash(sl, spot.add(0, 0.5, 0), 0xFFB060, 3.5, 8);
        sl.sendParticles(ParticleTypes.LAVA, spot.x, spot.y + 0.5, spot.z, 30, 1.0, 1.5, 1.0, 0.3);
        sl.sendParticles(ParticleTypes.FLAME, spot.x, spot.y + 0.5, spot.z, 40, 0.8, 2.0, 0.8, 0.15);
        sl.playSound(null, spot.x, spot.y, spot.z, ModSounds.HERRERO_BLAST.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        for (ServerPlayer p : sl.players()) {
            if (playerWithin(p, spot, 2.3, 3.0)) {
                p.hurt(damageSources().mobAttack(this), dmg(8.0F + phase()));
                ignite(p, 4);
                p.setDeltaMovement(p.getDeltaMovement().add(0, 0.9, 0));
                p.hurtMarked = true;
            }
        }
    }

    // ------------------------------------------------------------------ fases

    private void enterPhase(ServerLevel sl, int ph) {
        this.entityData.set(PHASE, ph);
        colorBar();
        this.entityData.set(POSE, 1);
        busy = 44;
        gcd = 60;
        slamTick = -1;
        this.entityData.set(SLAM, 0);
        playSound(ModSounds.HERRERO_ROAR.get(), 3.5F, 1.0F);
        Vec3 at = position();
        Vfx.ring(sl, at.add(0, 0.1, 0), 0xFFB060, 10.0, 18);
        Vfx.flash(sl, at.add(0, 2.0, 0), 0xFFD090, 6.0, 12);
        RelicFx.shakeNear(sl, at, 22, 1.2F, 16);
        for (ServerPlayer p : players(sl, 14.0)) {
            knock(p, 1.3, 0.5);
        }
        heatT = 0;
        this.entityData.set(HOT, true);
        if (ph == 2) {
            tell(sl, "¡El Gran Herrero se pone a trabajar de verdad! Caen yunques y lanza anillos de fuego.", ChatFormatting.GOLD);
            if (!warnedHeat) {
                warnedHeat = true;
                Scheduler.runLater(60, () -> tell(sl, "Cuando se ablanda (al rojo) recibe más daño; templado, casi no le haces nada.", ChatFormatting.YELLOW));
            }
        } else {
            tell(sl, "¡Se enfurece! La lava brota del suelo y cada martillazo trae dos ondas.", ChatFormatting.RED);
        }
        forgeCd = Math.min(forgeCd, 70);
        slamCd = Math.min(slamCd, 100);
        com.mundosdeaxelis.world.BossFx.phase(sl, at, ph, 0xFF7A2A, 0xFFC24A, ph == 2 ? "FASE II" : "FASE III",
                ph == 2 ? "LA FORJA SE ENCIENDE" : "FURIA DE LAVA", ParticleTypes.LAVA);
    }

    // ------------------------------------------------------------------ daño y muerte

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (phase() >= 2) {
            amount *= isHot() ? 1.25F : 0.6F;
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
        for (Escoria s : minions(sl, 90.0)) {
            s.discard();
        }
        Vec3 at = position();
        Vfx.ring(sl, at.add(0, 0.1, 0), ORANGE, 14.0, 24);
        Vfx.flash(sl, at.add(0, 2.0, 0), 0xFFE0A0, 8.0, 20);
        sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1.5, at.z, 60, 2.0, 2.0, 2.0, 0.2);
        sl.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.5, at.z, 80, 2.0, 2.0, 2.0, 0.1);
        RelicFx.shakeNear(sl, at, 26, 1.0F, 22);
        spawnAtLocation(new ItemStack(Items.IRON_INGOT, 10 + random.nextInt(9)));
        spawnAtLocation(new ItemStack(Items.MAGMA_BLOCK, 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(Items.COAL, 8 + random.nextInt(9)));
        // sello, material de Mesa y extras: según el daño que hizo cada jugador (ver BossRewards)
        List<ServerPlayer> present = players(sl, 100.0);
        com.mundosdeaxelis.world.BossRewards.distribute(sl, this, STAGE, present);
        for (ServerPlayer p : present) {
            ExperienceOrb.award(sl, p.position(), 250);
        }
        com.mundosdeaxelis.world.BossFx.defeat(sl, at, 0xFF7A2A, 0xFFC24A, ParticleTypes.LAVA,
                net.minecraft.world.level.block.Blocks.BLACKSTONE.defaultBlockState(), "GRAN HERRERO DERROTADO", "LA FORJA SE APAGA");
        sl.getServer().execute(() -> WorldTier.bossDefeated(sl.getServer(), STAGE));
        BossTravel.scheduleExit(sl);
    }
}
