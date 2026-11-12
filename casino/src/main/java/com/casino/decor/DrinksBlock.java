package com.casino.decor;

import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vasos y copas para poner en mesas y barras. Como las velas: con otra en la mano se agregan hasta 4. */
public class DrinksBlock extends UseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty DRINKS = IntegerProperty.create("drinks", 1, 4);
    private static final java.util.Map<Direction, VoxelShape> ONE = Shapes.rotated(new double[]{2.5, 0, 2.5, 6, 4.5, 6});
    private static final VoxelShape MANY = Block.box(2, 0, 2, 14, 5.5, 14);

    public DrinksBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(DRINKS, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, DRINKS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState here = ctx.getLevel().getBlockState(ctx.getClickedPos());
        if (here.is(this)) {
            return here.setValue(DRINKS, Math.min(4, here.getValue(DRINKS) + 1));
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext ctx) {
        return !ctx.isSecondaryUseActive() && ctx.getItemInHand().getItem() == asItem() && state.getValue(DRINKS) < 4
                || super.canBeReplaced(state, ctx);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return s.getValue(DRINKS) == 1 ? ONE.get(s.getValue(FACING)) : MANY;
    }

    @Override
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        return dir == Direction.DOWN && level instanceof LevelReader r && !canSurvive(state, r, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : state;
    }

    /** Necesita algo firme debajo. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP) || level.getBlockState(pos.below()).getBlock() instanceof DecorBlock
                || level.getBlockState(pos.below()).getBlock() instanceof CounterBlock;
    }
}
