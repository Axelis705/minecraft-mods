package com.casino.west;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** El toro mecánico: guarda el récord (quién aguantó más) para el letrero del ruedo. La montada vive en {@link BullRideEntity}. */
public class BullBE extends CasinoBE {
    private int bestTicks;
    private String bestName = "";

    public BullBE(BlockPos pos, BlockState state) {
        super(WestReg.BULL_BE.get(), pos, state);
    }

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.SOUTH;
    }

    public int bestTicks() {
        return bestTicks;
    }

    public String bestName() {
        return bestName;
    }

    /** La montada de ahorita (o null). */
    public BullRideEntity ride() {
        if (level == null) {
            return null;
        }
        var l = level.getEntitiesOfClass(BullRideEntity.class, new AABB(worldPosition).inflate(0.6, 1.0, 0.6));
        return l.isEmpty() ? null : l.get(0);
    }

    /** Al terminar una montada: ¿nuevo récord? */
    public void offer(ServerPlayer p, int ticks) {
        if (ticks <= bestTicks || ticks < 20) {
            return;
        }
        bestTicks = ticks;
        bestName = p.getGameProfile().getName();
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        Plat.msg(p, Component.translatable("casino.bull.record", String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0))
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), false);
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(3.5, 0, 3.5).expandTowards(0, 3, 0);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt("BestTicks", bestTicks);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readData(CompoundTag tag) {
        bestTicks = Nbt.i(tag, "BestTicks");
        bestName = Nbt.s(tag, "BestName");
    }
}
