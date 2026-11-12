package com.casino.plat;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
//#if MC >= 1.21
//$$ import net.minecraft.core.HolderLookup;
//#endif
//#if MC >= 1.21.6
//$$ import net.minecraft.world.level.storage.ValueInput;
//$$ import net.minecraft.world.level.storage.ValueOutput;
//#endif
//#if MC < 1.21
import net.minecraft.network.Connection;
//#endif

/**
 * Base de las entidades de bloque del casino: cada una solo escribe y lee sus datos en un CompoundTag ({@link #writeData}/{@link #readData})
 * y aquí se conectan con la forma de guardar y sincronizar de cada versión.
 */
public abstract class CasinoBE extends BlockEntity {
    protected CasinoBE(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void writeData(CompoundTag tag);

    protected abstract void readData(CompoundTag tag);

    /** Lo que se manda a los clientes (por defecto, todo). Se puede quitar lo secreto. */
    protected CompoundTag syncTag(CompoundTag full) {
        return full;
    }

    /** Hasta dónde se dibuja (por defecto, el bloque). */
    protected AABB renderBox() {
        return new AABB(worldPosition);
    }

    public static AABB box(BlockPos a, BlockPos b) {
        return new AABB(a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ());
    }

    private CompoundTag dataTag() {
        CompoundTag t = new CompoundTag();
        writeData(t);
        return t;
    }

    @Override
    public AABB getRenderBoundingBox() {
        return renderBox();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    //#if MC >= 1.21.6
    //$$ @Override
    //$$ protected void saveAdditional(ValueOutput out) {
    //$$     super.saveAdditional(out);
    //$$     out.store("casino", CompoundTag.CODEC, dataTag());
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void loadAdditional(ValueInput in) {
    //$$     super.loadAdditional(in);
    //$$     in.read("casino", CompoundTag.CODEC).ifPresent(this::readData);
    //$$ }
    //$$
    //$$ @Override
    //$$ public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
    //$$     CompoundTag w = new CompoundTag();
    //$$     w.put("casino", syncTag(dataTag()));
    //$$     return w;
    //$$ }
    //#elif MC >= 1.21
    //$$ @Override
    //$$ protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
    //$$     super.saveAdditional(tag, reg);
    //$$     writeData(tag);
    //$$ }
    //$$
    //$$ @Override
    //$$ protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
    //$$     super.loadAdditional(tag, reg);
    //$$     readData(tag);
    //$$ }
    //$$
    //$$ @Override
    //$$ public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
    //$$     return syncTag(dataTag());
    //$$ }
    //#else
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readData(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return syncTag(dataTag());
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag t = pkt.getTag();
        if (t != null) {
            readData(t);
        }
    }
    //#endif
}
