package com.casino.block;

import com.casino.plat.Plat;
import com.casino.client.ClientHooks;
import com.casino.entity.DealerEntity;
import com.casino.game.RouletteLayout;
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
 * La Mesa de ruleta: 7 bloques de largo por 3 de fondo (21 bloques). A la izquierda de los jugadores va la rueda y a la derecha el paño de
 * apuestas. El bloque que se coloca es el del centro (ahí vive el bloque-entidad); los demás solo dan forma y colisión. Todo lo que se ve
 * (madera, rueda, bola, fichas, números) lo dibuja un renderizador propio, por eso los bloques no tienen modelo.
 * El crupier se para detrás de la rueda.
 */
public class RouletteBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Cuál de los 21 bloques de la mesa es (ver RouletteLayout.CELLS). */
    public static final IntegerProperty CELL = IntegerProperty.create("cell", 0, RouletteLayout.CELLS.length - 1);
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, RouletteLayout.Y_TOP, 1);

    public RouletteBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CELL, RouletteLayout.MASTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, CELL);
    }

    /** La derecha de quien juega mirando la mesa de frente. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos at(BlockPos master, Direction facing, int col, int row) {
        return master.relative(right(facing), col).relative(facing, row);
    }

    public static BlockPos cellPos(BlockPos master, Direction facing, int cell) {
        int[] c = RouletteLayout.CELLS[cell];
        return at(master, facing, c[0], c[1]);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        Direction f = st.getValue(FACING);
        int[] c = RouletteLayout.CELLS[st.getValue(CELL)];
        return at(pos, f, -c[0], -c[1]);
    }

    /** Donde se para el crupier: detrás de la rueda. */
    public static BlockPos dealerSpot(BlockPos master, Direction facing) {
        return at(master, facing, RouletteLayout.DEALER_COL, RouletteLayout.DEALER_ROW);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection().getOpposite(); // el frente (lado de los jugadores) mira a quien la pone
        Level level = ctx.getLevel();
        BlockPos master = ctx.getClickedPos();
        for (int i = 0; i < RouletteLayout.CELLS.length; i++) {
            BlockPos p = cellPos(master, f, i);
            if (!level.isInWorldBounds(p) || (i != RouletteLayout.MASTER && !level.getBlockState(p).canBeReplaced(ctx))) {
                return null;
            }
        }
        BlockPos back = dealerSpot(master, f);
        if (!level.getBlockState(back).getCollisionShape(level, back).isEmpty() || !level.getBlockState(back.above()).getCollisionShape(level, back.above()).isEmpty()) {
            return null; // no hay lugar para el crupier
        }
        return defaultBlockState().setValue(FACING, f).setValue(CELL, RouletteLayout.MASTER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int i = 0; i < RouletteLayout.CELLS.length; i++) {
            if (i != RouletteLayout.MASTER) {
                level.setBlock(cellPos(pos, f, i), defaultBlockState().setValue(FACING, f).setValue(CELL, i), 3);
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel sl && level.getBlockEntity(pos) instanceof RouletteBE be) {
            be.spawnDealer(sl);
        }
    }

    private void removeOthers(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos master = masterOf(pos, state);
        for (int i = 0; i < RouletteLayout.CELLS.length; i++) {
            BlockPos p = cellPos(master, f, i);
            if (p.equals(pos)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (s.is(this) && s.getValue(FACING) == f && s.getValue(CELL) == i) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
            }
        }
        // devolver las fichas apostadas y llevarse al crupier
        if (level.getBlockEntity(master) instanceof RouletteBE be && !level.isClientSide()) {
            be.refundAll();
        }
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

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
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
            com.casino.plat.Net.onClient(() -> ClientHooks.openRoulette(master));
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
        return state.getValue(CELL) == RouletteLayout.MASTER ? new RouletteBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.ROULETTE.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((RouletteBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((RouletteBE) be).serverTick();
    }
}
