package com.coleccionista.bot;

import com.axeliscore.character.Persona;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Jugador de prueba: una persona con la piel de un jugador por defecto que entra al evento como una pieza más. Sirve para correr el evento
 * completo sin 15 amigos. No camina solo: las pruebas le dicen qué hacer. No recibe daño. Si lo eliminan queda como fantasma (igual que
 * un jugador): no choca con nadie, no se puede tocar y solo lo ven los fantasmas y el organizador.
 */
public class PiezaBot extends Persona {
    private static final EntityDataAccessor<Boolean> FANTASMA = SynchedEntityData.defineId(PiezaBot.class, EntityDataSerializers.BOOLEAN);

    public PiezaBot(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        setInvulnerable(true);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FANTASMA, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
    }

    public boolean fantasma() {
        return this.entityData.get(FANTASMA);
    }

    /** Lo vuelve fantasma (o lo devuelve al juego). */
    public void fantasma(boolean si) {
        this.entityData.set(FANTASMA, si);
        setNoAi(si);
        setNoGravity(si);
        setSilent(si);
        setCustomNameVisible(!si);
        if (si) {
            setDeltaMovement(0, 0, 0);
        }
    }

    @Override
    public boolean isPushable() {
        return !fantasma() && super.isPushable();
    }

    @Override
    public boolean canBeCollidedWith() {
        return !fantasma() && super.canBeCollidedWith();
    }

    @Override
    public boolean isPickable() {
        return !fantasma() && super.isPickable();
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity e) {
        if (!fantasma()) {
            super.doPush(e);
        }
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("Fantasma", fantasma());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.getBoolean("Fantasma")) {
            fantasma(true);
        }
    }
}
