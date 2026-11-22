package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.relic.RelicFx;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Coloso de Hierro (etapa 267): el minijefe de la Fragua Olvidada. Un gólem gigante de hierro oxidado que construyó el Gran Herrero
 * para guardar su tesoro. Duerme de pie, como una estatua, delante del altar; se despierta cuando alguien se acerca (o le pega).
 * <ul>
 * <li><b>Puñetazo</b> si estás pegado a él.</li>
 * <li><b>Golpe de suelo</b>: levanta los dos puños (con aviso: anillo naranja y chirrido) y golpea el suelo; salen tres <b>ondas en
 * anillo</b> que avanzan por el suelo: hay que <b>saltarlas</b> (en el aire no hacen daño).</li>
 * <li><b>Lingotes al rojo</b>: si estás lejos, te lanza tres lingotes incandescentes en arco (queman).</li>
 * <li><b>Se recalienta</b>: después de dos golpes de suelo se queda quieto unos segundos echando vapor. Entonces es
 * <b>vulnerable</b>: recibe casi el doble de daño.</li>
 * </ul>
 */
public class ColosoHierro extends Monster {
    /** 0 normal, 1 cargando el golpe de suelo, 2 lanzando lingotes, 3 recalentado, 4 dormido. */
    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(ColosoHierro.class, EntityDataSerializers.INT);
    public static final int NORMAL = 0, CHARGE = 1, THROW = 2, HOT = 3, ASLEEP = 4;
    public static final int CHARGE_TICKS = 26, THROW_TICKS = 18, HOT_TICKS = 90;
    private static final float WAVE_DAMAGE = 8.0F, INGOT_DAMAGE = 6.0F;
    private static final double WAVE_MAX = 13.0, WAVE_SPEED = 0.42, WAKE_RANGE = 9.0;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.mundos_de_axelis.coloso_hierro"),
            BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);

    private int stateTicks;
    /** Dónde duerme (dormido no se mueve: si algo lo empuja, vuelve a su sitio). */
    private Vec3 home;
    private int slamCd = 80;
    private int throwCd = 60;
    private int slamsSinceRest;

    /** Ondas de choque que avanzan por el suelo: radio actual, altura del suelo y quién ya recibió el golpe. */
    private static final class Wave {
        double r = 1.6;
        final Vec3 at;
        final Set<UUID> hit = new HashSet<>();

        Wave(Vec3 at) {
            this.at = at;
        }
    }

    private final List<Wave> waves = new ArrayList<>();
    private final List<int[]> pendingWaves = new ArrayList<>();

    public ColosoHierro(EntityType<? extends ColosoHierro> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        setPersistenceRequired();
        this.bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 180.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.ATTACK_DAMAGE, 11.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STATE, ASLEEP);
    }

    public int state() {
        return this.entityData.get(STATE);
    }

    /** Cuántos ticks lleva en el estado de ahora (en el cliente se cuenta aparte, para animar). */
    public int stateTicks() {
        return stateTicks;
    }

    private void setState(int s) {
        if (state() != s) {
            this.entityData.set(STATE, s);
            stateTicks = 0;
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            stateTicks = 0;
        }
    }

    public boolean asleep() {
        return state() == ASLEEP;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true) {
            @Override
            public boolean canUse() {
                return state() == NORMAL && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return state() == NORMAL && super.canContinueToUse();
            }

            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 3.0 * 3.0 + target.getBbWidth();
            }
        });
        // dormido no mira a nadie: parece una estatua
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F) {
            @Override
            public boolean canUse() {
                return !asleep() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !asleep() && super.canUse();
            }
        });
        // etapa 284: elige a quién pegar por la amenaza (jugadores y mascotas) y salta si le kitean
        this.targetSelector.addGoal(1, new com.mundosdeaxelis.world.JefeIA.MetaObjetivo(this, 32.0, () -> !asleep()));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            Vec3 d = new Vec3(le.getX() - getX(), 0, le.getZ() - getZ());
            d = d.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : d.normalize();
            le.setDeltaMovement(le.getDeltaMovement().add(d.x * 1.0, 0.45, d.z * 1.0));
            le.hurtMarked = true;
            playSound(SoundEvents.IRON_GOLEM_ATTACK, 1.6F, 0.6F);
        }
        return ok;
    }

    private List<ServerPlayer> playersNear(ServerLevel sl, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceTo(this) <= range) {
                out.add(p);
            }
        }
        return out;
    }

    private void wake(ServerLevel sl) {
        if (!asleep()) {
            return;
        }
        setState(NORMAL);
        this.bossEvent.setVisible(true);
        playSound(ModSounds.CAPATAZ_ROAR.get(), 3.0F, 0.55F);
        playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.5F);
        RelicFx.shakeNear(sl, position(), 24, 0.8F, 14);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 2.5, getZ(), 30, 0.8, 1.0, 0.8, 0.02);
        for (ServerPlayer p : playersNear(sl, 40.0)) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.coloso.despierta").withStyle(ChatFormatting.GOLD), true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        stateTicks++;
        if (this.level() instanceof ServerLevel sl) {
            if (asleep()) {
                if (home == null) {
                    home = position();
                } else if (position().distanceToSqr(home) > 0.25) {
                    setPos(home.x, home.y, home.z);
                    setDeltaMovement(Vec3.ZERO);
                }
            }
            if (asleep() && this.tickCount % 10 == 0 && !playersNear(sl, WAKE_RANGE).isEmpty()) {
                wake(sl);
            }
            tickWaves(sl);
        } else {
            clientFx();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl) || !isAlive()) {
            return;
        }
        this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        int s = state();
        if (s == ASLEEP) {
            getNavigation().stop();
            setTarget(null);
            return;
        }
        if (s == HOT) {
            getNavigation().stop();
            if (stateTicks % 4 == 0) {
                sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 2.6, getZ(), 3, 0.7, 0.5, 0.7, 0.03);
            }
            if (stateTicks >= HOT_TICKS) {
                setState(NORMAL);
                this.bossEvent.setColor(BossEvent.BossBarColor.YELLOW);
                playSound(SoundEvents.IRON_GOLEM_REPAIR, 2.0F, 0.6F);
            }
            return;
        }
        LivingEntity t = getTarget();
        if (s == CHARGE) {
            getNavigation().stop();
            if (t != null) {
                getLookControl().setLookAt(t, 30.0F, 30.0F);
            }
            if (stateTicks == 2) {
                playSound(ModSounds.CAPATAZ_CHARGE.get(), 2.5F, 0.6F);
            }
            if (stateTicks >= CHARGE_TICKS) {
                slam(sl);
            }
            return;
        }
        if (s == THROW) {
            getNavigation().stop();
            if (t != null) {
                getLookControl().setLookAt(t, 30.0F, 30.0F);
                if (stateTicks == 11) {
                    throwIngots(sl, t);
                }
            }
            if (stateTicks >= THROW_TICKS) {
                setState(NORMAL);
            }
            return;
        }
        if (t == null || !t.isAlive()) {
            return;
        }
        double d = distanceTo(t);
        if (--slamCd <= 0 && d < 10.0) {
            setState(CHARGE);
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xFFB03C, 4.0, CHARGE_TICKS);
            return;
        }
        if (--throwCd <= 0 && d > 5.0 && d < 24.0 && hasLineOfSight(t)) {
            setState(THROW);
            throwCd = 90 + random.nextInt(40);
        }
    }

    /** Cae con los dos puños: temblor, chispas y tres ondas en anillo (una cada 12 ticks). */
    private void slam(ServerLevel sl) {
        Vec3 at = position();
        playSound(ModSounds.CAPATAZ_SLAM.get(), 3.0F, 0.7F);
        playSound(SoundEvents.ANVIL_LAND, 2.0F, 0.6F);
        RelicFx.shakeNear(sl, at, 22, 1.2F, 12);
        Vfx.flash(sl, at.add(0, 0.6, 0), 0xFFB060, 4.0, 10);
        sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 14, 1.4, 0.1, 1.4, 0.02);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.3, at.z, 20, 1.6, 0.2, 1.6, 0.04);
        for (int i = 0; i < 3; i++) {
            pendingWaves.add(new int[]{this.tickCount + i * 12});
        }
        slamsSinceRest++;
        slamCd = 110 + random.nextInt(40);
        if (slamsSinceRest >= 2) {
            slamsSinceRest = 0;
            setState(HOT);
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
            playSound(SoundEvents.LAVA_EXTINGUISH, 2.5F, 0.6F);
            for (ServerPlayer p : playersNear(sl, 40.0)) {
                p.displayClientMessage(Component.translatable("message.mundos_de_axelis.coloso.recalienta").withStyle(ChatFormatting.RED), true);
            }
        } else {
            setState(NORMAL);
        }
    }

    private void tickWaves(ServerLevel sl) {
        for (int i = pendingWaves.size() - 1; i >= 0; i--) {
            if (this.tickCount >= pendingWaves.get(i)[0]) {
                pendingWaves.remove(i);
                waves.add(new Wave(position()));
                sl.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 1.6F);
            }
        }
        for (int i = waves.size() - 1; i >= 0; i--) {
            Wave w = waves.get(i);
            w.r += WAVE_SPEED;
            if (w.r > WAVE_MAX) {
                waves.remove(i);
                continue;
            }
            // la onda se ve: chispas y polvo de hierro a lo largo del anillo, sobre el suelo
            int n = (int) Math.max(12, w.r * 5.0);
            double off = random.nextDouble() * Math.PI * 2;
            for (int k = 0; k < n; k++) {
                double a = off + k * Math.PI * 2 / n;
                double x = w.at.x + Math.cos(a) * w.r, z = w.at.z + Math.sin(a) * w.r;
                sl.sendParticles(k % 3 == 0 ? ParticleTypes.FLAME : ParticleTypes.CRIT, x, w.at.y + 0.15, z, 1, 0.05, 0.05, 0.05, 0.0);
                if (k % 4 == 0) {
                    BlockState below = sl.getBlockState(BlockPos.containing(x, w.at.y - 0.5, z));
                    if (!below.isAir()) {
                        sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, below), x, w.at.y + 0.1, z, 2, 0.1, 0.05, 0.1, 0.1);
                    }
                }
            }
            for (ServerPlayer p : playersNear(sl, w.r + 3.0)) {
                if (w.hit.contains(p.getUUID())) {
                    continue;
                }
                double dist = Math.hypot(p.getX() - w.at.x, p.getZ() - w.at.z);
                // en el aire (saltando) la onda pasa por debajo
                if (Math.abs(dist - w.r) < 0.75 && p.onGround() && Math.abs(p.getY() - w.at.y) < 1.6) {
                    w.hit.add(p.getUUID());
                    p.hurt(damageSources().mobAttack(this), WAVE_DAMAGE);
                    Vec3 dir = new Vec3(p.getX() - w.at.x, 0, p.getZ() - w.at.z);
                    dir = dir.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : dir.normalize();
                    p.setDeltaMovement(p.getDeltaMovement().add(dir.x * 0.6, 0.5, dir.z * 0.6));
                    p.hurtMarked = true;
                }
            }
        }
    }

    /** Tres lingotes al rojo en arco hacia el objetivo (el del medio, directo; los otros, un poco abiertos). */
    private void throwIngots(ServerLevel sl, LivingEntity t) {
        playSound(SoundEvents.BLAZE_SHOOT, 2.0F, 0.6F);
        for (int i = -1; i <= 1; i++) {
            LingoteRojo ing = new LingoteRojo(sl, this);
            double dx = t.getX() - getX(), dz = t.getZ() - getZ();
            double dist = Math.max(1.0, Math.hypot(dx, dz));
            double a = Math.atan2(dz, dx) + i * 0.22;
            ing.setPos(getX() + Math.cos(a) * 1.2, getY() + 2.9, getZ() + Math.sin(a) * 1.2);
            double speed = Mth.clamp(dist * 0.055 + 0.55, 0.7, 1.7);
            double dy = (t.getY() + t.getBbHeight() * 0.5 - (getY() + 2.7)) / dist + 0.18 + dist * 0.012;
            ing.shoot(Math.cos(a), dy, Math.sin(a), (float) speed, 3.0F);
            ing.setDamage(INGOT_DAMAGE);
            sl.addFreshEntity(ing);
        }
    }

    private void clientFx() {
        int s = state();
        if (s == ASLEEP) {
            if (this.tickCount % 30 == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 0.6, getY() + 3.2, getZ() + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.02, 0.0);
            }
            return;
        }
        double yaw = Math.toRadians(this.yBodyRot);
        if (s == CHARGE && this.tickCount % 2 == 0) {
            for (int side = -1; side <= 1; side += 2) {
                double px = getX() + Math.cos(yaw) * side * 1.3, pz = getZ() + Math.sin(yaw) * side * 1.3;
                this.level().addParticle(ParticleTypes.FLAME, px, getY() + 3.6, pz, 0.0, 0.02, 0.0);
            }
        }
        if (s == HOT && this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.CLOUD, getX() + (random.nextDouble() - 0.5) * 1.6, getY() + 1.5 + random.nextDouble() * 1.5,
                    getZ() + (random.nextDouble() - 0.5) * 1.6, 0.0, 0.06, 0.0);
            this.level().addParticle(ParticleTypes.LAVA, getX(), getY() + 2.0, getZ(), 0.0, 0.0, 0.0);
        }
        if (this.tickCount % 13 == 0) {
            this.level().addParticle(ParticleTypes.SMOKE, getX() + (random.nextDouble() - 0.5) * 0.5, getY() + 3.3, getZ() + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.05, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL) || source.is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)
                || source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            return false;
        }
        if (this.level() instanceof ServerLevel sl && asleep()) {
            wake(sl);
        }
        if (state() == HOT) {
            amount *= 1.8F; // recalentado: el hierro blando recibe casi el doble
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT, getX(), getY() + 2.0, getZ(), 8, 0.6, 0.6, 0.6, 0.2);
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return !asleep() && super.isPushable();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!asleep()) {
            super.push(x, y, z);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            this.bossEvent.removeAllPlayers();
            Vec3 at = position();
            Vfx.ring(sl, at.add(0, 0.1, 0), 0xFF8A2A, 7.0, 20);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1.8, at.z, 40, 1.2, 1.4, 1.2, 0.05);
            sl.sendParticles(ParticleTypes.LAVA, at.x, at.y + 1.8, at.z, 20, 1.0, 1.0, 1.0, 0.1);
            playSound(SoundEvents.ANVIL_DESTROY, 2.5F, 0.6F);
            spawnAtLocation(new ItemStack(Items.IRON_INGOT, 8 + random.nextInt(6)));
            spawnAtLocation(new ItemStack(Items.IRON_BLOCK, 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(Items.GOLD_INGOT, 3 + random.nextInt(3)));
            spawnAtLocation(new ItemStack(Items.COAL, 4 + random.nextInt(5)));
            for (ServerPlayer p : playersNear(sl, 40.0)) {
                p.displayClientMessage(Component.translatable("message.mundos_de_axelis.coloso.vencido").withStyle(ChatFormatting.GOLD), true);
            }
        }
        super.die(source);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.4F, 0.55F);
    }

    @Override
    public float getVoicePitch() {
        return 0.55F;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormido", asleep());
        if (home != null) {
            tag.putDouble("CasaX", home.x);
            tag.putDouble("CasaY", home.y);
            tag.putDouble("CasaZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("CasaX")) {
            home = new Vec3(tag.getDouble("CasaX"), tag.getDouble("CasaY"), tag.getDouble("CasaZ"));
        }
        if (tag.contains("Dormido") && !tag.getBoolean("Dormido")) {
            this.entityData.set(STATE, NORMAL);
            this.bossEvent.setVisible(true);
        }
    }

    /** Para las pruebas: empieza a cargar el golpe de suelo ya (si está despierto). */
    public void forceSlam() {
        if (this.level() instanceof ServerLevel sl && !asleep()) {
            setState(CHARGE);
            Vfx.ring(sl, position().add(0, 0.1, 0), 0xFFB03C, 4.0, CHARGE_TICKS);
        }
    }

    /** Para las pruebas: lo despierta como si alguien se hubiera acercado. */
    public void wakeNow() {
        if (this.level() instanceof ServerLevel sl) {
            wake(sl);
        }
    }

    public static EntityType<ColosoHierro> type() {
        return ModEntities.COLOSO_HIERRO.get();
    }
}
