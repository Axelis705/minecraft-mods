package com.casino.plat;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Bloque con clic derecho y aviso de vecinos iguales en todas las versiones. Las clases hijas solo escriben {@link #onUse} y
 * {@link #onNeighbor}; aquí se conectan con el método que tenga cada versión.
 */
public abstract class UseBlock extends Block {
    protected UseBlock(Properties props) {
        super(props);
    }

    /** Clic derecho. PASS deja que el objeto en la mano haga lo suyo (por ejemplo, colocar un bloque). */
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    /** Un vecino cambió: devuelve el estado nuevo de este bloque (por ejemplo, para unir piezas). */
    protected BlockState onNeighbor(BlockState state, Direction dir, BlockState neighbor, BlockGetter level, BlockPos pos, BlockPos neighborPos) {
        return state;
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
    //$$
    //$$ @Override
    //$$ protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks,
    //$$         BlockPos pos, Direction dir, BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
    //$$     return onNeighbor(super.updateShape(state, level, ticks, pos, dir, neighborPos, neighbor, random), dir, neighbor, level, pos, neighborPos);
    //$$ }
    //#elif MC >= 1.20.5
    //$$ @Override
    //$$ protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
    //$$         Player player, InteractionHand hand, BlockHitResult hit) {
    //$$     InteractionResult r = onUse(state, level, pos, player, hand, hit);
    //$$     return r == InteractionResult.PASS ? net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION
    //$$             : r.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide()) : net.minecraft.world.ItemInteractionResult.FAIL;
    //$$ }
    //$$
    //$$ @Override
    //$$ protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    //$$     return onUse(state, level, pos, player, InteractionHand.MAIN_HAND, hit);
    //$$ }
    //$$
    //$$ @Override
    //$$ protected BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
    //$$         BlockPos neighborPos) {
    //$$     return onNeighbor(super.updateShape(state, dir, neighbor, level, pos, neighborPos), dir, neighbor, level, pos, neighborPos);
    //$$ }
    //#else
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onUse(state, level, pos, player, hand, hit);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, net.minecraft.world.level.LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        return onNeighbor(super.updateShape(state, dir, neighbor, level, pos, neighborPos), dir, neighbor, level, pos, neighborPos);
    }
    //#endif

    /** Girar con las plantillas de estructura: la dirección del frente y los lados unidos (norte/este/sur/oeste). */
    @Override
    public BlockState rotate(BlockState s, net.minecraft.world.level.block.Rotation r) {
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            var f = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
            s = s.setValue(f, r.rotate(s.getValue(f)));
        }
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH)) {
            net.minecraft.world.level.block.state.properties.BooleanProperty[] p = {
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH,
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST,
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH,
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST};
            boolean[] v = new boolean[4];
            for (int i = 0; i < 4; i++) {
                v[i] = s.getValue(p[i]);
            }
            int n = r.ordinal(); // NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90 = 0, 1, 2, 3 cuartos de vuelta
            for (int i = 0; i < 4; i++) {
                s = s.setValue(p[(i + n) & 3], v[i]);
            }
        }
        return s;
    }

    @Override
    public BlockState mirror(BlockState s, net.minecraft.world.level.block.Mirror m) {
        if (s.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return rotate(s, m.getRotation(s.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)));
        }
        return s;
    }
}
