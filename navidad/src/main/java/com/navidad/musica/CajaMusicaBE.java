package com.navidad.musica;

import com.navidad.registry.ModRegistry;
import com.navidad.registry.Musica;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/** Lo que guarda la caja de música: qué villancico va, si está sonando y desde cuándo (para saber cuándo termina). */
public class CajaMusicaBE extends BlockEntity {
    public static final int CANCIONES = 4;
    /** Cuántas veces se ha puesto a tocar una caja (para la prueba con fotos). */
    public static int tocadas;
    public int cancion;
    public boolean sonando;
    public long desde;
    /** (Cliente) qué tan abierta está la tapa (0 cerrada, 1 abierta), cómo va el giro y la manivela. */
    public float abierta, abiertaAntes, giro, giroAntes, vel;
    /** (Cliente) el sonido que está tocando ahora (lo maneja CajaMusicaCliente). */
    public Object sonido;

    public CajaMusicaBE(BlockPos p, BlockState s) {
        super(ModRegistry.CAJA_MUSICA_BE.get(), p, s);
    }

    public static int largo(int c) {
        return switch (c) {
            case 1 -> Musica.CAJA_NAVIDAD;
            case 2 -> Musica.CAJA_NOCHE_PAZ;
            case 3 -> Musica.CAJA_ADORNEN;
            default -> Musica.CAJA_CAMPANAS;
        };
    }

    public static SoundEvent sonidoDe(int c) {
        return switch (c) {
            case 1 -> ModRegistry.CAJA_NAVIDAD.get();
            case 2 -> ModRegistry.CAJA_NOCHE_PAZ.get();
            case 3 -> ModRegistry.CAJA_ADORNEN.get();
            default -> ModRegistry.CAJA_CAMPANAS.get();
        };
    }

    void tocar(ServerLevel l) {
        sonando = true;
        desde = l.getGameTime();
        tocadas++;
        avisar();
    }

    void parar() {
        sonando = false;
        avisar();
    }

    private void avisar() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    void tick() {
        if (level == null) {
            return;
        }
        if (!level.isClientSide) {
            if (sonando && level.getGameTime() - desde > largo(cancion) + 10) {
                parar();
            }
            return;
        }
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.navidad.client.CajaMusicaCliente.tick(this));
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        cancion = Math.floorMod(t.getInt("Cancion"), CANCIONES);
        sonando = t.getBoolean("Sonando");
        desde = t.getLong("Desde");
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        t.putInt("Cancion", cancion);
        t.putBoolean("Sonando", sonando);
        t.putLong("Desde", desde);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
    }
}
