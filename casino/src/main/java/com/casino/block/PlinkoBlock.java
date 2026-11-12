package com.casino.block;

import com.casino.plat.Plat;
import com.casino.client.ClientHooks;
import com.casino.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * La Mesa de Plinko: misma estructura que la ruleta (3 de ancho por 4 de alto, el bloque con el bloque-entidad es el de abajo en el centro).
 * Solo la fila de abajo tiene modelo; el tablero, las clavijas y las bolas las dibuja el renderizador.
 */
public class PlinkoBlock extends LuckyWheelBlock {
    public PlinkoBlock(Properties props) {
        super(props);
    }

    @Override
    public RenderShape getRenderShape(BlockState s) {
        return s.getValue(PY) == 0 ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    /** Clic derecho (igual en todas las versiones; abajo se conecta con la de cada una). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            BlockPos master = masterOf(pos, state);
            com.casino.plat.Net.onClient(() -> ClientHooks.openPlinko(master));
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
        return state.getValue(PX) == 1 && state.getValue(PY) == 0 ? new PlinkoBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.PLINKO.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((PlinkoBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((PlinkoBE) be).serverTick();
    }
}
