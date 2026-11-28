package com.coleccionista.hotel;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Pose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * La trampa de una puerta de servicio: una escopeta recortada en un tripié, adentro del clóset, a la altura de la cabeza y apuntando a la
 * puerta, con un hilo amarrado a la manija. Al abrir, ¡pum! (la animación "disparo": culatazo, fogonazo y el hilo que se afloja). Mira
 * hacia la puerta. El servidor solo cuenta los disparos; el cliente anima.
 */
public class TrampaEscopeta extends net.minecraft.world.entity.Entity implements Animated {
    private static final EntityDataAccessor<Integer> DISPAROS = SynchedEntityData.defineId(TrampaEscopeta.class, EntityDataSerializers.INT);
    private final AnimHost anim = new AnimHost();

    public TrampaEscopeta(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = false;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(DISPAROS, 0);
    }

    public int disparos() {
        return entityData.get(DISPAROS);
    }

    public void disparar() {
        entityData.set(DISPAROS, disparos() + 1);
    }

    // ------------------------------------------------------------------ cómo se dibuja (cliente)

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        Object visto = h.data.get("disparos");
        int d = disparos();
        if (!(visto instanceof Integer v) || v != d) {
            boolean nuevo = visto instanceof Integer;
            h.data.put("disparos", d);
            if (nuevo && h.clip("disparo") != null) {
                h.animator().play("base", h.clip("disparo"), 0.01F, 1F, true);
                h.data.put("disparando", true);
                return;
            }
        }
        if (!Boolean.TRUE.equals(h.data.get("disparando"))) {
            h.animator().play("base", h.clip("idle"), 0.1F, 1F, false);
        }
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
    }

    // ------------------------------------------------------------------ como entidad

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(1, 1.5, 1).move(0, 1, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 40 * 40;
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
    public boolean hurt(DamageSource src, float amount) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
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
