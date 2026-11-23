package com.axeliscore.ui;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Un bloque que muestra un cuadro de información al mirarlo (se llama en el cliente). */
public interface InfoBlock {
    void axInfo(Level level, BlockPos pos, BlockState state, InfoCard card, boolean detail);
}
