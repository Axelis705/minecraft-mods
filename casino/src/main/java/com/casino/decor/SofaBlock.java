package com.casino.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Sofá que se une con los de al lado (mismo sentido): solo las puntas llevan brazo. */
public class SofaBlock extends SeatBlock {
    public static final BooleanProperty LEFT = BooleanProperty.create("left");
    public static final BooleanProperty RIGHT = BooleanProperty.create("right");

    public SofaBlock(Properties props) {
        super(props, 0.53, new double[]{0, 0, 1.5, 16, 8.5, 16}, new double[]{0, 8.5, 12, 16, 16, 16});
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LEFT, true).setValue(RIGHT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LEFT, RIGHT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return arms(super.getStateForPlacement(ctx), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis().isHorizontal() ? arms(state, level, pos) : state;
    }

    /** Quien se sienta mira hacia FACING: su izquierda es FACING girado a la izquierda. */
    private BlockState arms(BlockState s, BlockGetter level, BlockPos pos) {
        Direction f = s.getValue(FACING);
        return s.setValue(LEFT, !joins(level, pos.relative(f.getCounterClockWise()), f))
                .setValue(RIGHT, !joins(level, pos.relative(f.getClockWise()), f));
    }

    private static boolean joins(BlockGetter level, BlockPos p, Direction f) {
        BlockState n = level.getBlockState(p);
        return n.getBlock() instanceof SofaBlock && n.getValue(FACING) == f;
    }
}
