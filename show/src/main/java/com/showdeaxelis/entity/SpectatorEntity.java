package com.showdeaxelis.entity;

import com.showdeaxelis.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/** Una persona del publico. No se mueve ni piensa; el show le cambia el humor (aplaude, vitorea, abuchea, se rie). */
public class SpectatorEntity extends Mob {
    public static final int SKINS = 8;
    public static final int IDLE = 0;
    public static final int CLAP = 1;
    public static final int CHEER = 2;
    public static final int BOO = 3;
    public static final int LAUGH = 4;

    private static final EntityDataAccessor<Integer> SKIN = SynchedEntityData.defineId(SpectatorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOOD = SynchedEntityData.defineId(SpectatorEntity.class, EntityDataSerializers.INT);
    private int moodTicks;

    public SpectatorEntity(EntityType<? extends SpectatorEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
    }

    public SpectatorEntity(Level level) {
        this(ModEntities.SPECTATOR.get(), level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SKIN, 0);
        this.entityData.define(MOOD, IDLE);
    }

    public int skin() {
        return Math.floorMod(this.entityData.get(SKIN), SKINS);
    }

    public void setSkin(int s) {
        this.entityData.set(SKIN, Math.floorMod(s, SKINS));
    }

    public int mood() {
        return this.entityData.get(MOOD);
    }

    public void setMood(int m, int ticks) {
        this.entityData.set(MOOD, m);
        this.moodTicks = ticks;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && moodTicks > 0 && --moodTicks == 0) {
            this.entityData.set(MOOD, IDLE);
        }
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // solo el creativo (o el vacio) las quita
        return src.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurt(src, amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Skin", skin());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSkin(tag.getInt("Skin"));
    }
}
