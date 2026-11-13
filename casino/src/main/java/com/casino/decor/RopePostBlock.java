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

/**
 * Poste con cordón de terciopelo: el cordón se tiende solo hacia los postes de al lado (de cualquier color), también en diagonal
 * cuando no hay otro poste en medio que ya los una. No se puede cruzar.
 */
public class RopePostBlock extends UseBlock {
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    /** Diagonales: noreste, sureste, suroeste y noroeste. */
    public static final BooleanProperty NE = BooleanProperty.create("ne");
    public static final BooleanProperty SE = BooleanProperty.create("se");
    public static final BooleanProperty SW = BooleanProperty.create("sw");
    public static final BooleanProperty NW = BooleanProperty.create("nw");
    private static final BooleanProperty[] DIAG = {NE, SE, SW, NW};
    private static final int[][] DXZ = {{1, -1}, {1, 1}, {-1, 1}, {-1, -1}};
    private static final Map<Direction, BooleanProperty> PROPS = new EnumMap<>(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST,
            Direction.SOUTH, SOUTH, Direction.WEST, WEST));
    private static final VoxelShape POST = Block.box(6.5, 0, 6.5, 9.5, 14.8, 9.5);
    /** Para chocar: el cordón cuenta como valla (alto), aunque se vea a media altura. */
    private static final Map<Direction, VoxelShape> ROPE = Shapes.rotated(new double[]{7.4, 0, 0, 8.6, 24, 8});
    private static final Map<Direction, VoxelShape> ROPE_LOOK = Shapes.rotated(new double[]{7.4, 9, 0, 8.6, 10.7, 8});

    public RopePostBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false)
                .setValue(NE, false).setValue(SE, false).setValue(SW, false).setValue(NW, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NORTH, EAST, SOUTH, WEST, NE, SE, SW, NW);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> e : PROPS.entrySet()) {
            s = s.setValue(e.getValue(), ctx.getLevel().getBlockState(ctx.getClickedPos().relative(e.getKey())).getBlock() instanceof RopePostBlock);
        }
        return diagonals(s, ctx.getLevel(), ctx.getClickedPos());
    }

    private static boolean post(BlockGetter g, BlockPos p) {
        return g.getBlockState(p).getBlock() instanceof RopePostBlock;
    }

    /** Une en diagonal con un poste que está en la esquina, si no hay otro poste al lado que ya haga el camino. */
    public static BlockState diagonals(BlockState s, BlockGetter g, BlockPos pos) {
        for (int i = 0; i < 4; i++) {
            int dx = DXZ[i][0];
            int dz = DXZ[i][1];
            boolean c = post(g, pos.offset(dx, 0, dz)) && !post(g, pos.offset(dx, 0, 0)) && !post(g, pos.offset(0, 0, dz));
            s = s.setValue(DIAG[i], c);
        }
        return s;
    }

    private static void refreshSelf(net.minecraft.world.level.Level level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.getBlock() instanceof RopePostBlock) {
            BlockState ns = diagonals(st, level, pos);
            if (ns != st) {
                level.setBlock(pos, ns, 2);
            }
        }
    }

    /** Los postes en diagonal no reciben aviso de Minecraft cuando se pone o se quita uno: se actualizan aquí. */
    private static void refreshCorners(net.minecraft.world.level.Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (int[] d : DXZ) {
            BlockPos q = pos.offset(d[0], 0, d[1]);
            BlockState st = level.getBlockState(q);
            if (st.getBlock() instanceof RopePostBlock) {
                BlockState ns = diagonals(st, level, q);
                if (ns != st) {
                    level.setBlock(q, ns, 2);
                }
            }
        }
    }

    //#if MC >= 1.20.5
    //$$ @Override
    //$$ protected void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moved) {
    //$$     super.onPlace(state, level, pos, old, moved);
    //$$     if (!level.isClientSide()) {
    //$$         refreshSelf(level, pos);
    //$$         refreshCorners(level, pos);
    //$$     }
    //$$ }
    //#else
    @Override
    public void onPlace(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide()) {
            refreshSelf(level, pos);
            refreshCorners(level, pos);
        }
    }
    //#endif

    //#if MC >= 1.21.5
    //$$ @Override
    //$$ protected void affectNeighborsAfterRemoval(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, boolean moved) {
    //$$     super.affectNeighborsAfterRemoval(state, level, pos, moved);
    //$$     refreshCorners(level, pos);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ protected void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState now, boolean moved) {
    //$$     super.onRemove(state, level, pos, now, moved);
    //$$     if (!(now.getBlock() instanceof RopePostBlock)) {
    //$$         refreshCorners(level, pos);
    //$$     }
    //$$ }
    //#else
    @Override
    public void onRemove(BlockState state, net.minecraft.world.level.Level level, BlockPos pos, BlockState now, boolean moved) {
        super.onRemove(state, level, pos, now, moved);
        if (!(now.getBlock() instanceof RopePostBlock)) {
            refreshCorners(level, pos);
        }
    }
    //#endif

    /** Al girar una estructura, las diagonales también giran (noreste -> sureste -> ...). */
    @Override
    public BlockState rotate(BlockState s, net.minecraft.world.level.block.Rotation r) {
        BlockState out = super.rotate(s, r);
        int n = r.ordinal();
        for (int i = 0; i < 4; i++) {
            out = out.setValue(DIAG[(i + n) & 3], s.getValue(DIAG[i]));
        }
        return out;
    }

    @Override
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        BooleanProperty p = PROPS.get(dir);
        boolean now = neighbor.getBlock() instanceof RopePostBlock;
        if (p == null || state.getValue(p) == now) {
            // nada cambió de este lado: las diagonales se quedan como están (al construir una plantilla los postes van llegando en
            // desorden y aquí todavía puede faltar el de la esquina)
            return state;
        }
        return diagonals(state.setValue(p, now), level, pos);
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
