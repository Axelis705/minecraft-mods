package com.casino.block;

import com.casino.plat.Plat;
import com.casino.client.ClientHooks;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * La Tragamonedas: un bloque de ancho y dos de alto. La mitad de abajo es la que se coloca y la que tiene el bloque-entidad;
 * los carretes (en la mitad de arriba) los dibuja un renderizador propio. Se puede tocar en cualquiera de las dos mitades.
 */
public class SlotBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public SlotBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, HALF);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        return st.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
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
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    /** Clic derecho (igual en todas las versiones; abajo se conecta con la de cada una). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        BlockPos master = masterOf(pos, state);
        // agachado: ver o cambiar el premio gordo (como el podio de la ruleta)
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide() && level.getBlockEntity(master) instanceof SlotBE be && player instanceof ServerPlayer sp) {
                be.interactJackpot(sp);
            }
            return Plat.ok(level.isClientSide());
        }
        if (level.isClientSide()) {
            com.casino.plat.Net.onClient(() -> ClientHooks.openSlot(master));
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
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new SlotBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.SLOT.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((SlotBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((SlotBE) be).serverTick();
    }
}
