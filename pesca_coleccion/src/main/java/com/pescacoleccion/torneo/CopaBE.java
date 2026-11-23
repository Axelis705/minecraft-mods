package com.pescacoleccion.torneo;

import com.axeliscore.block.CoreBlockEntity;
import com.axeliscore.plat.Nbt;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** La copa guarda quién la ganó y en qué torneo (lo mismo que lleva el objeto). */
public class CopaBE extends CoreBlockEntity {
    public CompoundTag info = new CompoundTag();

    public CopaBE(BlockPos pos, BlockState state) {
        super(ModRegistry.COPA_BE.get(), pos, state);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.put("info", info.copy());
    }

    @Override
    protected void readData(CompoundTag tag) {
        info = Nbt.c(tag, "info").copy();
    }
}
