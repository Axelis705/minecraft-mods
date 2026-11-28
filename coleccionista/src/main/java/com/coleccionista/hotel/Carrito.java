package com.coleccionista.hotel;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Pose;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * El carrito de maletas que el Botones suelta cuando persigue a alguien: se queda donde lo dejó (con el timbre que todavía tiembla) hasta
 * que regresa por él. No se mueve ni se rompe.
 */
public class Carrito extends net.minecraft.world.entity.Entity implements Animated {
    private final AnimHost anim = new AnimHost();

    public Carrito(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    public AnimHost axHost() {
        return anim;
    }

    @Override
    public void axAnimate(AnimHost h, float pt) {
        h.animator().play("base", h.clip("idle"), 0.2F, 1F, false);
    }

    @Override
    public void axPose(AnimHost h, Pose pose, float pt) {
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(2, 1, 2);
    }

    @Override
    public boolean isPickable() {
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
