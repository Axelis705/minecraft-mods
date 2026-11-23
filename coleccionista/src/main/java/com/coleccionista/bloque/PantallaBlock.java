package com.coleccionista.bloque;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La pantalla del Coleccionista: se cuelga en la pared y dibuja una pantalla de 3x2 bloques (la grande, de 11x7) centrada en ella. Lo
 * que muestra (apagada, estática, "esperando", el Coleccionista hablando) lo decide el evento para todas a la vez.
 */
public class PantallaBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final BooleanProperty GRANDE = BooleanProperty.create("grande");
    private static final VoxelShape N = Block.box(0, 0, 14, 16, 16, 16), S = Block.box(0, 0, 0, 16, 16, 2), E = Block.box(0, 0, 0, 2, 16, 16),
            W = Block.box(14, 0, 0, 16, 16, 16);

    public PantallaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(GRANDE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, GRANDE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        Direction d = c.getClickedFace().getAxis().isHorizontal() ? c.getClickedFace() : c.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, d);
    }

    /** Pegada a la pared de atrás: la pantalla mira hacia {@code facing}. */
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case NORTH -> N;
            case EAST -> E;
            case WEST -> W;
            default -> S;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PantallaBE(pos, state);
    }
}
