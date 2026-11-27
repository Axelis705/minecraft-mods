package com.navidad.entidad;

import com.navidad.registry.ModRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Una huella de pezuña en la nieve (de la Noche del Krampus): se dibuja pegadita al piso, oscura, y se borra sola después de un minuto.
 * No choca con nada ni se guarda con el mundo.
 */
public class HuellaKrampus extends Entity {
    public static final int DURA = 1200;
    private static final EntityDataAccessor<Boolean> IZQUIERDA = SynchedEntityData.defineId(HuellaKrampus.class, EntityDataSerializers.BOOLEAN);

    public HuellaKrampus(EntityType<? extends HuellaKrampus> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** (Servidor) Deja una huella en {@code donde} (la altura exacta del piso), mirando hacia {@code giro}. */
    public static HuellaKrampus poner(ServerLevel l, Vec3 donde, float giro, boolean izquierda) {
        HuellaKrampus h = ModRegistry.HUELLA_KRAMPUS.get().create(l);
        if (h == null) {
            return null;
        }
        h.moveTo(donde.x, donde.y + 0.01, donde.z, giro, 0);
        h.entityData.set(IZQUIERDA, izquierda);
        l.addFreshEntity(h);
        return h;
    }

    public boolean izquierda() {
        return entityData.get(IZQUIERDA);
    }

    /** Qué tan visible está (se borra en los últimos 10 segundos). */
    public float alfa(float pt) {
        float t = tickCount + pt;
        return Math.max(0F, Math.min(1F, Math.min(t / 10F, (DURA - t) / 200F)));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > DURA) {
            discard();
        }
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(IZQUIERDA, false);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
