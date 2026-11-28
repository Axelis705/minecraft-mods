package com.coleccionista.hotel;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * Un monitor de la sala de espera: la imagen de la tele vieja (la cara de alguien que sigue en el hotel, su nombre, en qué piso va y su
 * vida, con rayas de tele y la luz de "en vivo"; estática si no hay nadie). Con clic derecho se ve desde los ojos de esa persona
 * ({@link JuegoHotel#espectar}). El servidor decide a quién muestra cada monitor.
 */
public class Monitor extends net.minecraft.world.entity.Entity {
    private static final EntityDataAccessor<Optional<UUID>> QUIEN = SynchedEntityData.defineId(Monitor.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> NOMBRE = SynchedEntityData.defineId(Monitor.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> LINEA = SynchedEntityData.defineId(Monitor.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Float> VIDA = SynchedEntityData.defineId(Monitor.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> NUMERO = SynchedEntityData.defineId(Monitor.class, EntityDataSerializers.INT);

    public Monitor(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(QUIEN, Optional.empty());
        entityData.define(NOMBRE, "");
        entityData.define(LINEA, "");
        entityData.define(VIDA, 0F);
        entityData.define(NUMERO, 0);
    }

    public UUID quien() {
        return entityData.get(QUIEN).orElse(null);
    }

    public String nombre() {
        return entityData.get(NOMBRE);
    }

    public String linea() {
        return entityData.get(LINEA);
    }

    public float vida() {
        return entityData.get(VIDA);
    }

    public int numero() {
        return entityData.get(NUMERO);
    }

    public void numero(int n) {
        entityData.set(NUMERO, n);
    }

    /** (Servidor) A quién muestra (null: estática). Solo manda lo que cambió. */
    public void mostrar(UUID id, String nombre, String linea, float vida) {
        if (!java.util.Objects.equals(quien(), id)) {
            entityData.set(QUIEN, Optional.ofNullable(id));
        }
        if (!nombre().equals(nombre)) {
            entityData.set(NOMBRE, nombre);
        }
        if (!linea().equals(linea)) {
            entityData.set(LINEA, linea);
        }
        if (Math.abs(vida() - vida) > 0.01F) {
            entityData.set(VIDA, vida);
        }
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (p instanceof ServerPlayer sp) {
            JuegoHotel.espectar(sp, quien());
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 40 * 40;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
