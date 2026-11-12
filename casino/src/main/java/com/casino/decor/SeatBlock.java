package com.casino.decor;

import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/** Silla o taburete: clic derecho para sentarse (agachado se puede colocar otro bloque encima o al lado). */
public class SeatBlock extends UseBlock implements SeatEntity.Seat {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final double seatHeight;
    private final Map<Direction, VoxelShape> shapes;

    /** seatHeight en bloques (0.56 = 9 píxeles). shape: cajas mirando al norte. */
    public SeatBlock(Properties props, double seatHeight, double[]... shape) {
        super(props);
        this.seatHeight = seatHeight;
        this.shapes = Shapes.rotated(shape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return shapes.get(s.getValue(FACING));
    }

    @Override
    public Direction front(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown() || player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 9.0) {
                return InteractionResult.PASS;
            }
            if (!SeatEntity.sit(level, pos, seatHeight, front(state), player)) {
                Plat.msg(player, Component.translatable("casino.decor.seat_taken").withStyle(ChatFormatting.GRAY), true);
            }
        }
        return Plat.ok(level.isClientSide());
    }
}
