package com.coleccionista.bloque;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un foco colgando del techo que el evento prende y apaga (el "¡pum!" del despertar, los parpadeos, los apagones). Sin redstone: lo
 * cambia el mod directamente.
 */
public class FocoBlock extends Block {
    public static final BooleanProperty ENCENDIDO = BooleanProperty.create("encendido");
    private static final VoxelShape FORMA = Block.box(5, 2, 5, 11, 16, 11);

    public FocoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(ENCENDIDO, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(ENCENDIDO);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }
}
