package com.coleccionista.evento;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * El cuerpo de una pieza eliminada: queda tirado en el suelo con la piel del jugador (como en los eventos profesionales). No se puede
 * golpear ni empujar; cae si quedó en el aire. Se quitan con {@code /evento cuerpos limpiar} o al reiniciar el evento.
 */
public class Cadaver extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DUENO = SynchedEntityData.defineId(Cadaver.class,
            EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<String> NOMBRE = SynchedEntityData.defineId(Cadaver.class, EntityDataSerializers.STRING);
    /** Un número para variar la postura (brazos y piernas). */
    private static final EntityDataAccessor<Integer> POSTURA = SynchedEntityData.defineId(Cadaver.class, EntityDataSerializers.INT);

    public Cadaver(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DUENO, Optional.empty());
        this.entityData.define(NOMBRE, "");
        this.entityData.define(POSTURA, 0);
    }

    public void de(UUID id, String nombre, int postura) {
        this.entityData.set(DUENO, Optional.ofNullable(id));
        this.entityData.set(NOMBRE, nombre == null ? "" : nombre);
        this.entityData.set(POSTURA, postura);
    }

    public UUID dueno() {
        return this.entityData.get(DUENO).orElse(null);
    }

    public String nombre() {
        return this.entityData.get(NOMBRE);
    }

    public int postura() {
        return this.entityData.get(POSTURA);
    }

    /** Postura desde la que el cuerpo es una estatua de cera sentada (la vitrina): no cae y se dibuja color cera. */
    public static final int ESTATUA = 100;

    public boolean estatua() {
        return postura() >= ESTATUA;
    }

    @Override
    public void tick() {
        super.tick();
        if (estatua()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (!onGround()) {
            Vec3 v = getDeltaMovement().add(0, -0.04, 0);
            move(MoverType.SELF, v);
            setDeltaMovement(v.scale(0.98));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.hasUUID("Dueno")) {
            this.entityData.set(DUENO, Optional.of(t.getUUID("Dueno")));
        }
        this.entityData.set(NOMBRE, t.getString("Nombre"));
        this.entityData.set(POSTURA, t.getInt("Postura"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        UUID d = dueno();
        if (d != null) {
            t.putUUID("Dueno", d);
        }
        t.putString("Nombre", nombre());
        t.putInt("Postura", postura());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
}
