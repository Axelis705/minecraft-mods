package com.casino.decor;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Candelabro grande de techo (casi 3 bloques de ancho). "shift" corre el dibujo medio bloque para centrarlo en techos de ancho par:
 * 0 = centrado, 1 = hacia FACING, 2 = hacia FACING y su derecha (esquina). Al girar una estructura gira FACING y el corrimiento la sigue.
 */
public class GrandChandelierBlock extends DecorBlock {
    public static final IntegerProperty SHIFT = IntegerProperty.create("shift", 0, 2);

    public GrandChandelierBlock(Properties props) {
        super(props, false, new double[]{3, 6, 3, 13, 16, 13});
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(SHIFT);
    }
}
