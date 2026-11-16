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

    // ------------------------------------------------------------------ pelea de cantina (la dirige la pianola)

    private Brawl brawl;
    private long nextBrawl = -1;
    private boolean silenced;

    /** Arranca una pelea ya (comando de operador). Devuelve false si no hay dos vaqueros cerca. */
    public boolean brawlNow() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl) || brawl != null) {
            return false;
        }
        brawl = Brawl.start(sl, worldPosition);
        if (brawl != null) {
            silence(true);
        }
        return brawl != null;
    }

    private void silence(boolean quiet) {
        BlockState s = getBlockState();
        if (level == null || !s.hasProperty(WestBlocks.LIT)) {
            return;
        }
        if (quiet && s.getValue(WestBlocks.LIT)) {
            silenced = true;
            level.setBlock(worldPosition, s.setValue(WestBlocks.LIT, false), 3);
        } else if (!quiet && silenced) {
            silenced = false;
            level.setBlock(worldPosition, s.setValue(WestBlocks.LIT, true), 3);
        }
    }

    public void serverTick() {
        if (!(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        long now = level.getGameTime();
        if (brawl != null) {
            brawl.tick();
            if (brawl.done()) {
                brawl = null;
                silence(false);
                nextBrawl = now + 6000 + level.getRandom().nextInt(6000);
            }
            return;
        }
        if (nextBrawl < 0) {
            nextBrawl = now + 2400 + level.getRandom().nextInt(3600);
        }
        if (now < nextBrawl || now % 100 != 0) {
            return;
        }
        if (!on() || !Brawl.audience(sl, worldPosition)) {
            nextBrawl = now + 600;
            return;
        }
        if (!brawlNow()) {
            nextBrawl = now + 1200;
        }
    }

    @Override
    public void setRemoved() {
        if (brawl != null) {
            brawl.abort();
            brawl = null;
        }
        super.setRemoved();
    }

    @Override
    protected void writeData(CompoundTag tag) {
    }

    @Override
    protected void readData(CompoundTag tag) {
    }
}
