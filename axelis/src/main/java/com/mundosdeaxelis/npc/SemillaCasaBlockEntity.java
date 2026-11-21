package com.mundosdeaxelis.npc;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Guarda de quién es la Semilla de casa puesta en el suelo (y se lo manda al cliente para dibujar el holograma).
 * Los campos {@code scan*} solo los usa el cliente: qué bloques estorban, revisado cada medio segundo.
 */
public class SemillaCasaBlockEntity extends BlockEntity {
    private String kind = "";

    /** Cliente: última revisión del sitio (tick del juego y giro con que se hizo). */
    public long scanTick = Long.MIN_VALUE;
    public Object scanKey;
    /** Cliente: posiciones relativas (a la semilla) que estorban / columnas con un hueco grande debajo. */
    public Set<BlockPos> scanBlocked = Set.of();
    public Set<BlockPos> scanUneven = Set.of();

    public SemillaCasaBlockEntity(BlockPos pos, BlockState state) {
        super(com.mundosdeaxelis.registry.ModBlockEntities.SEMILLA_CASA.get(), pos, state);
    }

    public String kind() {
        return kind.isEmpty() ? NpcKinds.RUDA.id() : kind;
    }

    public void setKind(String k) {
        this.kind = k == null ? "" : k;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        kind = tag.getString("Npc");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("Npc", kind);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /** El holograma es mucho más grande que el bloque: que no se deje de dibujar al mirar a otro lado. */
    @Override
    public AABB getRenderBoundingBox() {
        return INFINITE_EXTENT_AABB;
    }
}
