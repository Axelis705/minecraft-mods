package com.navidad.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un bloque por capas (de 1 a 8, como la nieve): la cera que sube en las vitrinas del teatro. A diferencia de la nieve, no se derrite con
 * la luz ni necesita nada debajo.
 */
public class DecoCapas extends Block {
    public static final IntegerProperty CAPAS = BlockStateProperties.LAYERS;
    private static final VoxelShape[] FORMA = new VoxelShape[9];

    static {
        for (int i = 0; i <= 8; i++) {
            FORMA[i] = Block.box(0, 0, 0, 16, i * 2, 16);
        }
    }

    public DecoCapas(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(CAPAS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(CAPAS);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORMA[s.getValue(CAPAS)];
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORMA[s.getValue(CAPAS) - 1];
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean useShapeForLightOcclusion(BlockState s) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState s, BlockGetter l, BlockPos p, PathComputationType t) {
        return s.getValue(CAPAS) < 5;
    }
}
