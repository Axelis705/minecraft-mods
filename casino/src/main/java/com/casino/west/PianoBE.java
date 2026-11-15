package com.casino.west;

import com.casino.plat.CasinoBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/** La pianola: no guarda nada; existe para que el dibujante anime las teclas y el rollo. */
public class PianoBE extends CasinoBE {
    public PianoBE(BlockPos pos, BlockState state) {
        super(WestReg.PIANO_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.NORTH;
    }

    public boolean on() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.LIT) && s.getValue(WestBlocks.LIT);
    }

    @Override
    protected void writeData(CompoundTag tag) {
    }

    @Override
    protected void readData(CompoundTag tag) {
    }
}
