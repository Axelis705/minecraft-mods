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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Mueble que solo se pone (mesa, estante). Las mesas redondas no usan la dirección (su modelo es igual en las cuatro). */
public class DecorBlock extends UseBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final boolean facing;
    private final Map<Direction, VoxelShape> shapes;

    public DecorBlock(Properties props, boolean facing, double[]... shape) {
        super(props);
        this.facing = facing;
        this.shapes = Shapes.rotated(shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return facing ? defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()) : defaultBlockState();
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return shapes.get(facing ? s.getValue(FACING) : Direction.NORTH);
    }
}
