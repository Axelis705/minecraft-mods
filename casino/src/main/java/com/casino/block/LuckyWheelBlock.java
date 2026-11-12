package com.casino.block;

import com.casino.plat.Plat;
import com.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * La Ruleta de la suerte: una estructura de 3 de ancho por 4 de alto. El bloque que se coloca es la parte de abajo en el centro
 * (aquí vive el bloque-entidad); el resto son partes que solo dan forma (se pueden tocar en cualquier punto de la ruleta).
 * La ruleta gira con un renderizador propio, no con el modelo del bloque.
 */
public class LuckyWheelBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int NX = 3;
    public static final int NY = 4;
    /** Columna (0..2, de izquierda a derecha mirando la ruleta de frente) y fila (0..3, de abajo a arriba). */
    public static final IntegerProperty PX = IntegerProperty.create("px", 0, NX - 1);
    public static final IntegerProperty PY = IntegerProperty.create("py", 0, NY - 1);
    private static final VoxelShape SHAPE_NS = Block.box(0, 0, 5, 16, 16, 11);
    private static final VoxelShape SHAPE_EW = Block.box(5, 0, 0, 11, 16, 16);

    public LuckyWheelBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PX, 1).setValue(PY, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PX, PY);
    }

    /** La derecha de quien mira la ruleta de frente. */
    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cell(BlockPos master, Direction facing, int px, int py) {
        return master.relative(right(facing), px - 1).above(py);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        Direction f = st.getValue(FACING);
        return pos.relative(right(f).getOpposite(), st.getValue(PX) - 1).below(st.getValue(PY));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection().getOpposite(); // la cara de la ruleta mira a quien la pone
        Level level = ctx.getLevel();
        BlockPos master = ctx.getClickedPos();
        for (int x = 0; x < NX; x++) {
            for (int y = 0; y < NY; y++) {
                BlockPos p = cell(master, f, x, y);
                if (!level.isInWorldBounds(p) || (!(x == 1 && y == 0) && !level.getBlockState(p).canBeReplaced(ctx))) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, f).setValue(PX, 1).setValue(PY, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int x = 0; x < NX; x++) {
            for (int y = 0; y < NY; y++) {
                if (x == 1 && y == 0) {
                    continue;
                }
                level.setBlock(cell(pos, f, x, y), defaultBlockState().setValue(FACING, f).setValue(PX, x).setValue(PY, y), 3);
            }
        }
        if (level.getBlockEntity(pos) instanceof LuckyWheelBE be) {
            be.setOwner(placer);
        }
    }

    private void removeOthers(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos master = masterOf(pos, state);
        for (int x = 0; x < NX; x++) {
            for (int y = 0; y < NY; y++) {
                BlockPos p = cell(master, f, x, y);
                if (p.equals(pos)) {
                    continue;
                }
                BlockState s = level.getBlockState(p);
                if (s.is(this) && s.getValue(FACING) == f && s.getValue(PX) == x && s.getValue(PY) == y) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                }
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
        return s.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    /** Solo las partes de la base tienen modelo; el resto es invisible (la ruleta la dibuja el renderizador). */
    @Override
    public RenderShape getRenderShape(BlockState s) {
        int x = s.getValue(PX);
        int y = s.getValue(PY);
        return (y == 0) || (x == 1 && y == 1) ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    /** Clic derecho (igual en todas las versiones; abajo se conecta con la de cada una). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(masterOf(pos, state)) instanceof LuckyWheelBE be && player instanceof ServerPlayer sp) {
            be.interact(sp);
        }
        return InteractionResult.CONSUME;
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
        return state.getValue(PX) == 1 && state.getValue(PY) == 0 ? new LuckyWheelBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.LUCKY_WHEEL.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((LuckyWheelBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((LuckyWheelBE) be).serverTick();
    }
}
