package com.mundosdeaxelis.entity;

import com.mundosdeaxelis.fx.Vfx;
import com.mundosdeaxelis.registry.ModEntities;
import com.mundosdeaxelis.registry.ModSounds;
import com.mundosdeaxelis.world.DungeonBalance;
import com.mundosdeaxelis.world.Scheduler;
import net.minecraft.ChatFormatting;
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
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 288: la Dríade del Roble, mini-jefa del Santuario del Bosque. El espíritu que cuidaba el santuario, al que la corrupción del
 * Rey Brote volvió contra todos. Duerme arrodillada entre las raíces del suelo del santuario (no se la puede herir) hasta que alguien
 * se acerca; entonces se levanta y pelea. Lleva la llave de la celda de Ruda.
 * <ul>
 * <li><b>Latigazo</b>: golpe cuerpo a cuerpo que frena.</li>
 * <li><b>Enredadera</b> (de lejos, con 0,6 s de aviso): un látigo de hojas que tira de su objetivo hacia ella.</li>
 * <li><b>Flores venenosas</b>: marca en el suelo (anillos verdes) bajo quien pelea y alrededor; a 1,2 s revientan en esporas que
 * envenenan. Hay que salir del anillo.</li>
 * <li><b>Raíces del roble</b> (de cerca, con 0,8 s de aviso: levanta los brazos): raíces alrededor de ella que hieren y atrapan.</li>
 * <li>A media vida <b>florece</b>: llama a dos Brotes y se vuelve más rápida.</li>
 * </ul>
 * Elige a quién pegar con {@link com.mundosdeaxelis.world.JefeIA} (amenaza, mascotas, contra el kiteo). Vida y daño de tier 1.
 */
public class DriadeRoble extends Monster {
    private static final EntityDataAccessor<Boolean> DORMIDA = SynchedEntityData.defineId(DriadeRoble.class, EntityDataSerializers.BOOLEAN);
    /** Lo que está haciendo (para la animación): 0 nada, 1 enredadera, 2 brazos arriba (flores o raíces). */
    private static final EntityDataAccessor<Integer> GESTO = SynchedEntityData.defineId(DriadeRoble.class, EntityDataSerializers.INT);
    public static final int NADA = 0, ENREDADERA = 1, BRAZOS = 2;

    private static final float HITS = 22.0F;
    private static final float MELEE = 7.0F, WHIP = 4.0F, BLOOM = 5.0F, ROOTS = 6.0F;
    public static final double WAKE_RANGE = 9.0;
    private static final int VERDE = 0x7CE05A, ROSA = 0xF08CBA;

    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.mundos_de_axelis.driade_roble"),
            BossEvent.BossBarColor.GREEN, BossEvent.BossBarOverlay.NOTCHED_10);
    private int whipCd = 90, bloomCd = 140, rootCd = 160;
    private int gestoTicks;
    private boolean florecio;
    private Vec3 home;

    public DriadeRoble(EntityType<? extends DriadeRoble> type, Level level) {
        super(type, level);
        this.xpReward = 60;
        setPersistenceRequired();
        if (!level.isClientSide) {
            com.mundosdeaxelis.world.BarrasLejanas.track(this.bossEvent, this);
            this.bossEvent.setVisible(false);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, DungeonBalance.hp(1, HITS))
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(Attributes.ATTACK_DAMAGE, DungeonBalance.dmg(1, MELEE, 1))
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DORMIDA, true);
        this.entityData.define(GESTO, NADA);
    }

    public boolean dormida() {
        return this.entityData.get(DORMIDA);
    }

    public int gesto() {
        return this.entityData.get(GESTO);
    }

    private void setGesto(int g) {
        this.entityData.set(GESTO, g);
        gestoTicks = 0;
    }

    public boolean florecio() {
        return florecio;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05D, false) {
            @Override
            public boolean canUse() {
                return !dormida() && gesto() == NADA && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !dormida() && gesto() == NADA && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 12.0F) {
            @Override
            public boolean canUse() {
                return !dormida() && super.canUse();
            }
        });
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return !dormida() && super.canUse();
            }
        });
        this.targetSelector.addGoal(1, new com.mundosdeaxelis.world.JefeIA.MetaObjetivo(this, 28.0, () -> !dormida()));
    }

    // ------------------------------------------------------------------ dormir y despertar

    private List<ServerPlayer> jugadores(ServerLevel sl, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (p.isAlive() && !p.isSpectator() && !p.isCreative() && p.distanceTo(this) <= r) {
                out.add(p);
            }
        }
        return out;
    }

    private void despertar(ServerLevel sl) {
        if (!dormida()) {
            return;
        }
        this.entityData.set(DORMIDA, false);
        this.bossEvent.setVisible(true);
        playSound(SoundEvents.AZALEA_LEAVES_BREAK, 2.0F, 0.6F);
        playSound(ModSounds.REY_BROTE_ROAR.get(), 1.6F, 1.5F);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.2, getZ(), 50, 1.2, 1.0, 1.2, 0.02);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2.0, getZ(), 30, 1.0, 0.6, 1.0, 0.02);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), getX(), getY() + 0.1, getZ(), 40,
                1.2, 0.1, 1.2, 0.1);
        Vfx.ring(sl, position().add(0, 0.1, 0), VERDE, 3.5, 20);
        for (ServerPlayer p : jugadores(sl, 40.0)) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.driade.despierta").withStyle(ChatFormatting.GREEN), true);
        }
    }

    /** Para las pruebas: la despierta como si alguien se hubiera acercado. */
    public void despertarYa() {
        if (this.level() instanceof ServerLevel sl) {
            despertar(sl);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel sl) {
            if (home == null) {
                home = position();
            }
            if (dormida()) {
                if (position().distanceToSqr(home) > 0.25) {
                    setPos(home.x, home.y, home.z);
                }
                setDeltaMovement(Vec3.ZERO);
                if (this.tickCount % 10 == 0 && !jugadores(sl, WAKE_RANGE).isEmpty()) {
                    despertar(sl);
                }
                if (this.tickCount % 30 == 0) {
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.4, getZ(), 2, 0.4, 0.4, 0.4, 0.0);
                }
            }
            this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            if (this.tickCount % 10 == 0) {
                // la barra solo se ve dentro del santuario
                for (ServerPlayer p : new ArrayList<>(this.bossEvent.getPlayers())) {
                    if (p.isRemoved() || p.level() != sl || p.distanceToSqr(this) > 24 * 24) {
                        this.bossEvent.removePlayer(p);
                    }
                }
                if (!dormida()) {
                    for (ServerPlayer p : jugadores(sl, 18.0)) {
                        this.bossEvent.addPlayer(p);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ ataques

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl) || !isAlive()) {
            return;
        }
        if (dormida()) {
            getNavigation().stop();
            setTarget(null);
            return;
        }
        if (!florecio && getHealth() < getMaxHealth() * 0.5F) {
            florecer(sl);
        }
        int g = gesto();
        if (g != NADA) {
            gestoTicks++;
            getNavigation().stop();
            LivingEntity t = getTarget();
            if (t != null) {
                getLookControl().setLookAt(t, 40.0F, 40.0F);
            }
            if (g == ENREDADERA && gestoTicks == 12) {
                if (t != null && t.isAlive()) {
                    enredadera(sl, t);
                }
                setGesto(NADA);
            } else if (g == BRAZOS && gestoTicks >= 16) {
                setGesto(NADA);
            }
            return;
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            return;
        }
        double d = distanceTo(t);
        float rapida = florecio ? 0.7F : 1.0F;
        if (--rootCd <= 0 && d < 5.5) {
            raices(sl);
            rootCd = (int) ((150 + random.nextInt(40)) * rapida);
        } else if (--bloomCd <= 0 && d < 16) {
            flores(sl);
            bloomCd = (int) ((170 + random.nextInt(40)) * rapida);
        } else if (--whipCd <= 0 && d > 4.0 && d < 13.0 && hasLineOfSight(t)) {
            setGesto(ENREDADERA);
            playSound(SoundEvents.VINE_STEP, 1.6F, 0.6F);
            whipCd = (int) ((110 + random.nextInt(40)) * rapida);
        }
    }

    /** Enredadera: un látigo de hojas hasta el objetivo, que lo trae hacia ella (y le hace algo de daño). */
    private void enredadera(ServerLevel sl, LivingEntity t) {
        Vec3 a = position().add(0, 1.4, 0), b = t.position().add(0, 1.0, 0);
        int n = (int) (a.distanceTo(b) * 3);
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.lerp(b, i / (double) n);
            sl.sendParticles(ParticleTypes.COMPOSTER, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        Vec3 pull = position().subtract(t.position());
        double len = pull.length();
        if (len > 0.1) {
            pull = pull.scale(Math.min(1.6, len * 0.22) / len);
            t.setDeltaMovement(t.getDeltaMovement().add(pull.x, 0.35, pull.z));
            t.hurtMarked = true;
        }
        t.hurt(damageSources().mobAttack(this), DungeonBalance.dmg(1, WHIP, 1));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
        playSound(SoundEvents.CAVE_VINES_PICK_BERRIES, 1.5F, 0.8F);
    }

    /** Flores venenosas: anillos bajo quienes pelean (y alguno más alrededor) que a 1,2 s revientan en esporas. */
    private void flores(ServerLevel sl) {
        setGesto(BRAZOS);
        playSound(SoundEvents.FLOWERING_AZALEA_PLACE, 2.0F, 0.7F);
        List<Vec3> sitios = new ArrayList<>();
        for (ServerPlayer p : jugadores(sl, 16.0)) {
            sitios.add(p.position());
        }
        LivingEntity t = getTarget();
        if (t != null && !(t instanceof Player)) {
            sitios.add(t.position()); // también bajo las mascotas
        }
        int extra = florecio ? 3 : 2;
        for (int i = 0; i < extra; i++) {
            double ang = random.nextDouble() * Math.PI * 2, r = 2.5 + random.nextDouble() * 3.5;
            sitios.add(position().add(Math.cos(ang) * r, 0, Math.sin(ang) * r));
        }
        float dmg = DungeonBalance.dmg(1, BLOOM, Math.max(1, jugadores(sl, 16.0).size()));
        for (Vec3 s : sitios) {
            Vfx.ring(sl, s.add(0, 0.1, 0), ROSA, 2.0, 24);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, s.x, s.y + 0.4, s.z, 6, 0.8, 0.2, 0.8, 0.01);
            Scheduler.runLater(24, () -> {
                if (isAlive()) {
                    revientan(sl, s, dmg);
                }
            });
        }
    }

    /** Una flor revienta en esporas: daño y veneno a quien esté a 2,2 bloques (no a los monstruos). Devuelve a cuántos alcanzó. */
    public int revientan(ServerLevel sl, Vec3 s, float dmg) {
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, s.x, s.y + 0.6, s.z, 30, 1.0, 0.5, 1.0, 0.02);
        sl.sendParticles(ParticleTypes.ITEM_SLIME, s.x, s.y + 0.4, s.z, 12, 0.8, 0.3, 0.8, 0.05);
        sl.playSound(null, s.x, s.y, s.z, SoundEvents.PUFFER_FISH_BLOW_UP, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.2F);
        int n = 0;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s, s).inflate(2.2, 2.0, 2.2))) {
            if (le == this || le instanceof Monster || le instanceof Player pl && (pl.isCreative() || pl.isSpectator())) {
                continue;
            }
            if (le.position().distanceToSqr(s) <= 2.2 * 2.2) {
                le.hurt(damageSources().indirectMagic(this, this), dmg);
                le.addEffect(new MobEffectInstance(MobEffects.POISON, 70, 0));
                n++;
            }
        }
        return n;
    }

    /** Raíces del roble: levanta los brazos y, al bajarlos, salen raíces a su alrededor que hieren y atrapan. */
    private void raices(ServerLevel sl) {
        setGesto(BRAZOS);
        playSound(ModSounds.RAIZAL_ROOTS.get(), 1.6F, 1.2F);
        double r = florecio ? 6.0 : 5.0;
        Vfx.ring(sl, position().add(0, 0.1, 0), VERDE, r, 16);
        Scheduler.runLater(16, () -> {
            if (isAlive()) {
                raicesGolpe(sl, r);
            }
        });
    }

    /** Salen las raíces: daño y atrapa a quien esté a {@code r} bloques (no a los monstruos). Devuelve a cuántos alcanzó. */
    public int raicesGolpe(ServerLevel sl, double r) {
        float dmg = DungeonBalance.dmg(1, ROOTS, Math.max(1, jugadores(sl, 16.0).size()));
        int n = 0;
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 2.0, r))) {
            if (le == this || le instanceof Monster || le instanceof Player pl && (pl.isCreative() || pl.isSpectator())) {
                continue;
            }
            if (le.distanceToSqr(this) <= r * r) {
                le.hurt(damageSources().mobAttack(this), dmg);
                le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3));
                sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LOG.defaultBlockState()), le.getX(), le.getY() + 0.2,
                        le.getZ(), 14, 0.3, 0.2, 0.3, 0.1);
                n++;
            }
        }
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ROOTED_DIRT.defaultBlockState()), getX(), getY() + 0.1, getZ(), 50,
                r * 0.4, 0.1, r * 0.4, 0.1);
        playSound(ModSounds.GUARDIAN_SLAM.get(), 1.2F, 1.4F);
        return n;
    }

    /** Para las pruebas: florece ya (como al llegar a media vida). */
    public void florecerYa() {
        if (this.level() instanceof ServerLevel sl && !florecio) {
            florecer(sl);
        }
    }

    /** A media vida florece: dos Brotes salen del suelo y ella se vuelve más rápida. */
    private void florecer(ServerLevel sl) {
        florecio = true;
        this.bossEvent.setColor(BossEvent.BossBarColor.PINK);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32D);
        playSound(SoundEvents.FLOWERING_AZALEA_BREAK, 2.0F, 0.6F);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 2.2, getZ(), 60, 1.4, 0.8, 1.4, 0.02);
        for (int i = 0; i < 2; i++) {
            Brote b = ModEntities.BROTE.get().create(sl);
            if (b == null) {
                continue;
            }
            double ang = random.nextDouble() * Math.PI * 2;
            BlockPos p = BlockPos.containing(getX() + Math.cos(ang) * 2.5, getY(), getZ() + Math.sin(ang) * 2.5);
            b.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            b.setPersistenceRequired();
            if (getTarget() != null) {
                b.setTarget(getTarget());
            }
            sl.addFreshEntity(b);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState()), b.getX(), b.getY() + 0.2, b.getZ(),
                    20, 0.3, 0.2, 0.3, 0.1);
        }
        for (ServerPlayer p : jugadores(sl, 30.0)) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.driade.florece").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        }
    }

    // ------------------------------------------------------------------ daño, muerte y guardar

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean ok = super.doHurtTarget(target);
        if (ok && target instanceof LivingEntity le) {
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            playSound(SoundEvents.VINE_STEP, 1.4F, 1.4F);
        }
        return ok;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (dormida() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (this.level() instanceof ServerLevel sl && source.getEntity() instanceof Player) {
                despertar(sl);
            }
            return false; // dormida entre las raíces no se la puede herir: al tocarla, despierta
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_FALL)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isPushable() {
        return !dormida() && super.isPushable();
    }

    @Override
    public void push(double x, double y, double z) {
        if (!dormida()) {
            super.push(x, y, z);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, getX(), getY() + 1.2, getZ(), 80, 1.0, 1.0, 1.0, 0.03);
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, getX(), getY() + 1.0, getZ(), 40, 1.0, 1.0, 1.0, 0.02);
            for (ServerPlayer p : jugadores(sl, 30.0)) {
                p.displayClientMessage(Component.translatable("message.mundos_de_axelis.driade.vencida").withStyle(ChatFormatting.GOLD), false);
            }
            this.bossEvent.removeAllPlayers();
        }
        super.die(source);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bossEvent.removeAllPlayers();
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
    protected SoundEvent getAmbientSound() {
        return dormida() ? null : SoundEvents.AZALEA_LEAVES_STEP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AZALEA_LEAVES_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FLOWERING_AZALEA_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Dormida", dormida());
        tag.putBoolean("Florecio", florecio);
        if (home != null) {
            tag.putDouble("CasaX", home.x);
            tag.putDouble("CasaY", home.y);
            tag.putDouble("CasaZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Dormida")) {
            this.entityData.set(DORMIDA, tag.getBoolean("Dormida"));
            this.bossEvent.setVisible(!tag.getBoolean("Dormida"));
        }
        florecio = tag.getBoolean("Florecio");
        if (florecio) {
            this.bossEvent.setColor(BossEvent.BossBarColor.PINK);
        }
        if (tag.contains("CasaX")) {
            home = new Vec3(tag.getDouble("CasaX"), tag.getDouble("CasaY"), tag.getDouble("CasaZ"));
        }
    }
}
