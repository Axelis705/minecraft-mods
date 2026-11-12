package com.casino.block;

import com.casino.plat.Plat;
import com.casino.client.ClientHooks;
import com.casino.entity.DealerEntity;
import com.casino.game.BlackjackLayout;
import com.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * La Mesa de blackjack: una media luna de 13 bloques (5 de ancho, 3 de fondo). El bloque que se coloca es el del centro del lado recto
 * (ahí vive el bloque-entidad); los demás solo dan forma y colisión. La mesa entera (fieltro, rieles, cartas, fichas) la dibuja un
 * renderizador propio, por eso los bloques no tienen modelo. El crupier (una entidad) se para detrás del lado recto; los jugadores,
 * del lado curvo (hasta 5 asientos).
 */
public class BlackjackBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Cuál de los 13 bloques de la mesa es (ver BlackjackLayout.CELLS). */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, BlackjackLayout.CELLS.length - 1);
    private static final double TABLE_TOP = 12.0 / 16.0;
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][BlackjackLayout.CELLS.length];

    public BlackjackBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, BlackjackLayout.MASTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, CELL);
    }

    /** La derecha de quien juega mirando la mesa de frente. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cellPos(BlockPos master, Direction facing, int cell) {
        int[] c = BlackjackLayout.CELLS[cell];
        return master.relative(right(facing), c[0]).relative(facing, c[1]);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        Direction f = st.getValue(FACING);
        int[] c = BlackjackLayout.CELLS[st.getValue(CELL)];
        return pos.relative(right(f).getOpposite(), c[0]).relative(f.getOpposite(), c[1]);
    }

    /** Donde se para el crupier: el bloque de detrás del centro del lado recto. */
    public static BlockPos dealerSpot(BlockPos master, Direction facing) {
        return master.relative(facing.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection().getOpposite(); // el lado curvo mira a quien la pone
        Level level = ctx.getLevel();
        BlockPos master = ctx.getClickedPos();
        for (int i = 0; i < BlackjackLayout.CELLS.length; i++) {
            BlockPos p = cellPos(master, f, i);
            if (!level.isInWorldBounds(p) || (i != BlackjackLayout.MASTER && !level.getBlockState(p).canBeReplaced(ctx))) {
                return null;
            }
        }
        BlockPos back = dealerSpot(master, f);
        if (!level.getBlockState(back).getCollisionShape(level, back).isEmpty() || !level.getBlockState(back.above()).getCollisionShape(level, back.above()).isEmpty()) {
            return null; // no hay lugar para el crupier
        }
        return defaultBlockState().setValue(FACING, f).setValue(CELL, BlackjackLayout.MASTER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int i = 0; i < BlackjackLayout.CELLS.length; i++) {
            if (i != BlackjackLayout.MASTER) {
                level.setBlock(cellPos(pos, f, i), defaultBlockState().setValue(FACING, f).setValue(CELL, i), 3);
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel sl && level.getBlockEntity(pos) instanceof BlackjackBE be) {
            be.spawnDealer(sl);
        }
    }

    private void removeOthers(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos master = masterOf(pos, state);
        for (int i = 0; i < BlackjackLayout.CELLS.length; i++) {
            BlockPos p = cellPos(master, f, i);
            if (p.equals(pos)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (s.is(this) && s.getValue(FACING) == f && s.getValue(CELL) == i) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        // el crupier se va con la mesa
        BlockPos spot = dealerSpot(master, f);
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(spot).inflate(1.5, 1.0, 1.5))) {
            if (master.equals(d.getTable())) {
                d.discard();
            }
        }
    }

    @Override
    //#if MC >= 1.20.2
    //$$ public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //#else
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
    //#endif
        if (!level.isClientSide()) {
            removeOthers(level, pos, state);
            if (!player.isCreative()) {
                Block.popResource(level, masterOf(pos, state), new ItemStack(asItem()));
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
    //$$     removeOthers(level, pos, state);
    //$$     super.affectNeighborsAfterRemoval(state, level, pos, moved);
    //$$ }
    //#else
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            removeOthers(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }
    //#endif

    // ------------------------------------------------------------------ forma: solo la parte del bloque que está dentro de la media luna

    private static VoxelShape computeShape(Direction f, int cell) {
        int[] c = BlackjackLayout.CELLS[cell];
        Direction r = right(f);
        VoxelShape shape = Shapes.empty();
        // 16 columnas de 1/16 de bloque a lo largo del eje de la mesa (x); en cada una, el tramo de z que cae dentro de la media luna
        for (int i = 0; i < 16; i++) {
            double x0 = c[0] - 0.5 + i / 16.0;
            double x1 = x0 + 1.0 / 16.0;
            double xm = (x0 + x1) / 2;
            double zLo = c[1] - 0.5;
            double zHi = c[1] + 0.5;
            double lo = Math.max(zLo, BlackjackLayout.CZ);
            double dx = xm - BlackjackLayout.CX;
            double r2 = BlackjackLayout.RADIUS * BlackjackLayout.RADIUS - dx * dx;
            if (r2 <= 0) {
                continue;
            }
            double hi = Math.min(zHi, BlackjackLayout.CZ + Math.sqrt(r2));
            if (hi - lo < 1e-6) {
                continue;
            }
            // de coordenadas de la mesa (x derecha, z hacia los jugadores) a coordenadas del bloque (0..1) en el mundo
            double[] ax = {x0 - c[0], x1 - c[0]};
            double[] az = {lo - c[1], hi - c[1]};
            double minX = 1e9, maxX = -1e9, minZ = 1e9, maxZ = -1e9;
            for (double u : ax) {
                for (double w : az) {
                    double wx = 0.5 + u * r.getStepX() + w * f.getStepX();
                    double wz = 0.5 + u * r.getStepZ() + w * f.getStepZ();
                    minX = Math.min(minX, wx);
                    maxX = Math.max(maxX, wx);
                    minZ = Math.min(minZ, wz);
                    maxZ = Math.max(maxZ, wz);
                }
            }
            shape = Shapes.or(shape, Shapes.box(minX, 0, minZ, maxX, TABLE_TOP, maxZ));
        }
        return shape.isEmpty() ? Shapes.box(0.45, 0, 0.45, 0.55, TABLE_TOP, 0.55) : shape.optimize();
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        Direction f = s.getValue(FACING);
        int cell = s.getValue(CELL);
        int fi = f.get2DDataValue();
        VoxelShape sh = SHAPES[fi][cell];
        if (sh == null) {
            sh = computeShape(f, cell);
            SHAPES[fi][cell] = sh;
        }
        return sh;
    }

    /** Los bloques no tienen modelo: la mesa la dibuja el renderizador. */
    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    /** Clic derecho (igual en todas las versiones; abajo se conecta con la de cada una). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            BlockPos master = masterOf(pos, state);
            com.casino.plat.Net.onClient(() -> ClientHooks.openBlackjack(master));
        }
        return Plat.ok(level.isClientSide());
    }

    //#if MC >= 1.21.2
    //$$ @Override
    //$$ protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
    //$$         InteractionHand hand, BlockHitResult hit) {
    //$$     return onUse(state, level, pos, player, hand, hit);
    //$$ }
    //$$
    //$$ @Override
    //$$ protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    //$$     return onUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
    //$$         Player player, InteractionHand hand, BlockHitResult hit) {
    //$$     InteractionResult r = onUse(state, level, pos, player, hand, hit);
    //$$     return r == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
    //$$             : r.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide()) : net.minecraft.world.ItemInteractionResult.FAIL;
    //$$ }
    //$$
    //$$ @Override
    //$$ protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    //$$     return onUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    //$$ }
    //#else
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onUse(state, level, pos, player, hand, hit);
    }
    //#endif

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(CELL) == BlackjackLayout.MASTER ? new BlackjackBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.BLACKJACK.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((BlackjackBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((BlackjackBE) be).serverTick();
    }
}
