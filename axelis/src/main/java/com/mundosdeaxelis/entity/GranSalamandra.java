package com.mundosdeaxelis.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Gran Salamandra (etapa 255): el guardián de la cámara que hay entre la sala de erupciones y el santuario del volcán. Es una
 * salamandra de lava enorme (casi el doble): muerde y quema, escupe abanicos de tres bolas de lava, y si te acercas golpea el
 * suelo y salen columnas de fuego bajo tus pies. En las pozas de lava se cura deprisa (sácala a tierra firme). A media vida ruge
 * y llama a dos salamandras más. Al morir se abre la reja hacia el santuario.
 */
public class GranSalamandra extends SalamandraLava {
    private final ServerBossEvent bossEvent = new ServerBossEvent(Component.translatable("entity.mundos_de_axelis.gran_salamandra"),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private BlockPos home;
    private boolean enraged;
    private int volleyCd = 80;
    private int quakeCd = 140;

    public GranSalamandra(EntityType<? extends GranSalamandra> type, Level level) {
        super(type, level);
        this.xpReward = 80;
        setPersistenceRequired();
        if (!level.isClientSide) {
            com.mundosdeaxelis.world.BarrasLejanas.track(this.bossEvent, this);
        }
    }

    /** Etapa 284: elige a quién pegar por la amenaza (jugadores y mascotas) en lugar de «el último que me pegó». */
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(g -> true);
        this.targetSelector.addGoal(1, new com.mundosdeaxelis.world.JefeIA.MetaObjetivo(this, 32.0));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 220.0D)
                .add(Attributes.ATTACK_DAMAGE, 9.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
                .add(Attributes.FOLLOW_RANGE, 28.0D);
    }

    public boolean enraged() {
        return enraged;
    }

    @Override
    public boolean removeWhenFarAway(double dist) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (home == null) {
                home = blockPosition();
            }
            restrictTo(home, 11);
            this.bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            if (this.tickCount % 10 == 0 && this.level() instanceof ServerLevel sl) {
                // la barra solo se ve de cerca (en su cámara), no por todo el volcán ni desde fuera
                for (ServerPlayer p : new java.util.ArrayList<>(this.bossEvent.getPlayers())) {
                    if (p.isRemoved() || p.level() != sl || p.distanceToSqr(this) > 24 * 24) {
                        this.bossEvent.removePlayer(p);
                    }
                }
                for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(18))) {
                    if (p.distanceToSqr(this) <= 18 * 18) {
                        this.bossEvent.addPlayer(p);
                    }
                }
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(this.level() instanceof ServerLevel sl)) {
            return;
        }
        // en la lava se cura más deprisa que las pequeñas (3 por segundo en total)
        if (this.tickCount % 20 == 0 && onLava() && getHealth() < getMaxHealth()) {
            heal(2.0F);
        }
        if (!enraged && getHealth() < getMaxHealth() * 0.5F) {
            enrage(sl);
        }
        LivingEntity t = getTarget();
        if (t == null || !t.isAlive()) {
            return;
        }
        double dist = distanceTo(t);
        if (--volleyCd <= 0 && dist > 3.5 && dist < 16.0 && hasLineOfSight(t)) {
            volley(sl, t);
            volleyCd = (enraged ? 70 : 110) + this.random.nextInt(30);
        }
        if (--quakeCd <= 0 && dist < 5.5) {
            quake(sl, t);
            quakeCd = (enraged ? 110 : 170) + this.random.nextInt(40);
        }
    }

    /** Tres bolas de lava en abanico: a donde estás, y una a cada lado. */
    private void volley(ServerLevel sl, LivingEntity t) {
        Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x);
        Vec3 mouth = position().add(fwd.scale(1.5)).add(0, 0.7, 0);
        Vec3 aim = t.position().add(t.getDeltaMovement().scale(6.0));
        for (int i = -1; i <= 1; i++) {
            Vec3 to = aim.add(side.scale(i * 2.6));
            double d = mouth.distanceTo(to);
            VolcanFx.lavaGlob(sl, this, mouth, to, (int) Math.max(14, Math.min(28, d * 1.6)), 2.5 + d * 0.12, 6.0F, 1.7);
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.3F, 0.5F);
        sl.sendParticles(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, 12, 0.3, 0.2, 0.3, 0.04);
    }

    /** Golpea el suelo: columnas de fuego bajo quien esté cerca (con aviso). */
    private void quake(ServerLevel sl, LivingEntity t) {
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.RAVAGER_STEP, SoundSource.HOSTILE, 2.0F, 0.5F);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 20, 1.5, 0.1, 1.5, 0.02);
        VolcanFx.pillar(sl, this, t.position(), 22, 7.0F, 1.6);
        int extra = enraged ? 3 : 1;
        for (int i = 0; i < extra; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 2.5 + this.random.nextDouble() * 3.0;
            VolcanFx.pillar(sl, this, position().add(Math.cos(a) * r, 0, Math.sin(a) * r), 26 + i * 6, 6.0F, 1.4);
        }
    }

    /** A media vida: ruge y llama a dos salamandras de lava. */
    private void enrage(ServerLevel sl) {
        enraged = true;
        this.bossEvent.setOverlay(BossEvent.BossBarOverlay.NOTCHED_20);
        sl.playSound(null, getX(), getY(), getZ(), com.mundosdeaxelis.registry.ModSounds.CORAZON_RUGIDO.get(), SoundSource.HOSTILE, 1.6F, 1.35F);
        sl.sendParticles(ParticleTypes.LAVA, getX(), getY() + 1.0, getZ(), 24, 1.2, 0.4, 1.2, 0.0);
        for (int i = 0; i < 2; i++) {
            SalamandraLava s = com.mundosdeaxelis.registry.ModEntities.SALAMANDRA_LAVA.get().create(sl);
            if (s == null) {
                continue;
            }
            double a = this.random.nextDouble() * Math.PI * 2;
            s.moveTo(getX() + Math.cos(a) * 3.0, getY(), getZ() + Math.sin(a) * 3.0, this.random.nextFloat() * 360F, 0F);
            s.setTarget(getTarget());
            s.getPersistentData().putBoolean("MundosEsbirroVolcan", true);
            sl.addFreshEntity(s);
            sl.sendParticles(ParticleTypes.LAVA, s.getX(), s.getY() + 0.3, s.getZ(), 8, 0.3, 0.2, 0.3, 0.0);
        }
        for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(24))) {
            p.displayClientMessage(Component.translatable("message.mundos_de_axelis.gran_salamandra.enraged"), true);
        }
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel sl && !isRemoved() && !this.dead) {
            BlockPos at = home != null ? home : blockPosition();
            int opened = com.mundosdeaxelis.block.RejaVolcanBlock.setChannel(sl, at, 1, 16, 5, true);
            for (ServerPlayer p : sl.getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32))) {
                p.displayClientMessage(Component.translatable(opened > 0 ? "message.mundos_de_axelis.gran_salamandra.dead" : "message.mundos_de_axelis.gran_salamandra.dead_nogate"), false);
            }
            sl.playSound(null, getX(), getY(), getZ(), com.mundosdeaxelis.registry.ModSounds.CORAZON_MUERTE.get(), SoundSource.HOSTILE, 1.2F, 1.4F);
            drop(sl, new ItemStack(com.mundosdeaxelis.registry.ModItems.MATS.get(com.mundosdeaxelis.table.Mats.LIVE_EMBER).get(), 2 + this.random.nextInt(3)));
            if (this.random.nextInt(100) < 45) {
                drop(sl, new ItemStack(com.mundosdeaxelis.registry.ModItems.MATS.get(com.mundosdeaxelis.table.Mats.DARK_ASH).get(), 1));
            }
            drop(sl, new ItemStack(net.minecraft.world.item.Items.MAGMA_CREAM, 2 + this.random.nextInt(3)));
            this.bossEvent.removeAllPlayers();
        }
        super.die(source);
    }

    private void drop(ServerLevel sl, ItemStack st) {
        ItemEntity it = new ItemEntity(sl, getX(), getY() + 0.6, getZ(), st);
        it.setDefaultPickUpDelay();
        sl.addFreshEntity(it);
    }

    @Override
    public void remove(RemovalReason reason) {
        this.bossEvent.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", enraged);
        if (home != null) {
            tag.putLong("Home", home.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        enraged = tag.getBoolean("Enraged");
        if (tag.contains("Home")) {
            home = BlockPos.of(tag.getLong("Home"));
        }
    }

    @Override
    public float getVoicePitch() {
        return 0.35F + this.random.nextFloat() * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }
}
