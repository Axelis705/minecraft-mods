package com.casino.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Sombrilla de alberca: dos bloques de alto (como una puerta), así la lona queda a dos bloques y medio del piso y se puede pasar
 * por debajo. Abajo van la base y el mástil; arriba, el mástil y la lona. Al romper una mitad se va la otra y suelta una sola sombrilla.
 */
public class ParasolBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LOWER = net.minecraft.world.phys.shapes.Shapes.or(Block.box(3.5, 0, 3.5, 12.5, 2.4, 12.5),
            Block.box(7.3, 2.4, 7.3, 8.7, 16, 8.7));
    private static final VoxelShape UPPER = Block.box(7.3, 0, 7.3, 8.7, 16, 8.7);

    public ParasolBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos p = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (!level.isInWorldBounds(p.above()) || !level.getBlockState(p.above()).canBeReplaced(ctx)) {
            return null;
        }
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    private void removeOther(Level level, BlockPos pos, BlockState state) {
        BlockPos other = state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
        BlockState s = level.getBlockState(other);
        if (s.is(this) && s.getValue(HALF) != state.getValue(HALF)) {
            level.setBlock(other, Blocks.AIR.defaultBlockState(), 35);
        }
    }

    @Override
    //#if MC >= 1.20.2
    //$$ public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //#else
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //#endif
        if (!level.isClientSide()) {
            removeOther(level, pos, state);
            if (!player.isCreative()) {
                BlockPos lower = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
                Block.popResource(level, lower, new ItemStack(asItem()));
            }
        }
        //#if MC >= 1.20.2
        //$$ return super.playerWillDestroy(level, pos, state, player);
        //#else
        super.playerWillDestroy(level, pos, state, player);
        //#endif
    }

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
    //$$     removeOther(level, pos, state);
    //$$     super.affectNeighborsAfterRemoval(state, level, pos, moved);
    //$$ }
    //#else
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            removeOther(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
    //#endif

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return s.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }
}
