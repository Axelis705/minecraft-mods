package com.casino.decor;

import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Barra de bar: el frente (paneles y reposapiés) mira hacia quien la pone. Se une en esquinas como las escaleras:
 * "outer" = esquina hacia afuera (dos frentes en la punta), "inner" = esquina hacia adentro (rincón).
 * LEFT = el segundo frente queda a la izquierda del primero (visto desde afuera, mirando al frente).
 */
public class CounterBlock extends UseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<StairsShape> SHAPE = BlockStateProperties.STAIRS_SHAPE;
    private static final VoxelShape BOX = Block.box(0, 0, 0, 16, 15.5, 16);

    public CounterBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SHAPE, StairsShape.STRAIGHT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, SHAPE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
        return s.setValue(SHAPE, shape(s, ctx.getLevel(), ctx.getClickedPos()));
    }

    @Override
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        return dir.getAxis().isHorizontal() ? state.setValue(SHAPE, shape(state, level, pos)) : state;
    }

    private static StairsShape shape(BlockState s, BlockGetter level, BlockPos pos) {
        Direction f = s.getValue(FACING);
        Direction back = facingOf(level, pos.relative(f.getOpposite()));
        if (back != null && back.getAxis() != f.getAxis()) {
            return back == f.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
        }
        Direction front = facingOf(level, pos.relative(f));
        if (front != null && front.getAxis() != f.getAxis()) {
            return front == f.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
        }
        return StairsShape.STRAIGHT;
    }

    private static Direction facingOf(BlockGetter level, BlockPos p) {
        BlockState n = level.getBlockState(p);
        return n.getBlock() instanceof CounterBlock ? n.getValue(FACING) : null;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return BOX;
    }
}
