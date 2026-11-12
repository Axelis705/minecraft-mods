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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

/** Poste con cordón de terciopelo: el cordón se tiende solo hacia los postes de al lado (de cualquier color). No se puede cruzar. */
public class RopePostBlock extends UseBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    private static final Map<Direction, BooleanProperty> PROPS = new EnumMap<>(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 14.8, 9.5);
    /** Para chocar: el cordón cuenta como valla (alto), aunque se vea a media altura. */
    private static final Map<Direction, VoxelShape> ROPE = Shapes.rotated(new double[]{7.4, 0, 0, 8.6, 24, 8});
    private static final Map<Direction, VoxelShape> ROPE_LOOK = Shapes.rotated(new double[]{7.4, 9, 0, 8.6, 10.7, 8});

    public RopePostBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> e : PROPS.entrySet()) {
            s = s.setValue(e.getValue(), ctx.getLevel().getBlockState(ctx.getClickedPos().relative(e.getKey())).getBlock() instanceof RopePostBlock);
        }
        return s;
    }

    @Override
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty p = PROPS.get(dir);
        return p == null ? state : state.setValue(p, neighbor.getBlock() instanceof RopePostBlock);
    }

    private static VoxelShape shape(BlockState s, Map<Direction, VoxelShape> rope) {
        VoxelShape out = rope == ROPE ? Block.box(6.5, 0, 6.5, 9.5, 24, 9.5) : POST;
        for (Map.Entry<Direction, BooleanProperty> e : PROPS.entrySet()) {
            if (s.getValue(e.getValue())) {
                out = net.minecraft.world.phys.shapes.Shapes.or(out, rope.get(e.getKey()));
            }
        }
        return out;
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return shape(s, ROPE_LOOK);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return shape(s, ROPE);
    }
}
