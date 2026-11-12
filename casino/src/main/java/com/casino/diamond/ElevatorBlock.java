package com.casino.diamond;

import com.casino.decor.Shapes;
import com.casino.item.VipCardItem;
import com.casino.net.CasinoNet;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Panel del elevador (va en la pared de la cabina, a la altura de la cabeza). Todos los paneles en la misma columna (mismo x y z)
 * son paradas del mismo elevador. Clic derecho: se elige el piso y el jugador aparece frente al panel de ese piso. Los pisos VIP
 * piden la tarjeta (Plata = 1, Oro = 2). Con agachado + clic, un operador cambia el nombre y el nivel de la parada.
 */
public class ElevatorBlock extends UseBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{3, 2, 14, 13, 14, 16});
    /** Hasta dónde se buscan otras paradas hacia arriba y abajo. */
    public static final int RANGE = 160;

    public ElevatorBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, f);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElevatorBE(pos, state);
    }

    /** Las paradas de este elevador, de abajo hacia arriba. */
    public static List<ElevatorBE> stops(Level level, BlockPos pos) {
        List<ElevatorBE> out = new ArrayList<>();
        int y0 = Math.max(Plat.minY(level), pos.getY() - RANGE);
        int y1 = Math.min(Plat.maxY(level) - 1, pos.getY() + RANGE);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = y0; y <= y1; y++) {
            p.set(pos.getX(), y, pos.getZ());
            if (level.getBlockEntity(p) instanceof ElevatorBE be) {
                out.add(be);
            }
        }
        return out;
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof ElevatorBE here) {
            if (player.isShiftKeyDown() && Plat.isOp(player)) {
                // operador: cambia el nivel de acceso de esta parada (0 → 1 → 2 → 0)
                here.access = (here.access + 1) % 3;
                here.setChanged();
                Plat.msg(sp, Component.translatable("casino.elevator.access_set", Component.translatable("casino.elevator.access_" + here.access)), true);
                return Plat.ok(false);
            }
            List<ElevatorBE> st = stops(level, pos);
            if (st.size() < 2) {
                Plat.msg(sp, Component.translatable("casino.elevator.alone").withStyle(ChatFormatting.GRAY), true);
                return Plat.ok(false);
            }
            CasinoNet.sendTo(sp, OpenElevatorPacket.of(pos, st, VipCardItem.tier(sp), sp.isCreative()));
        }
        return Plat.ok(level.isClientSide());
    }

    /** Viajar a la parada de la altura y (lo pide el jugador desde la pantalla). */
    public static void travel(ServerPlayer p, BlockPos from, int y) {
        Level level = p.level();
        if (!level.isLoaded(from) || !(level.getBlockEntity(from) instanceof ElevatorBE)) {
            return;
        }
        if (p.distanceToSqr(from.getX() + 0.5, from.getY() + 0.5, from.getZ() + 0.5) > 6 * 6 || Math.abs(y - from.getY()) > RANGE) {
            return;
        }
        BlockPos to = new BlockPos(from.getX(), y, from.getZ());
        if (!level.isLoaded(to) || !(level.getBlockEntity(to) instanceof ElevatorBE target) || to.equals(from)) {
            return;
        }
        if (!VipCardItem.canEnter(p, target.access)) {
            Plat.msg(p, Component.translatable(target.access >= 2 ? "casino.elevator.need_gold" : "casino.elevator.need_silver")
                    .withStyle(ChatFormatting.RED), true);
            level.playSound(null, p.blockPosition(), com.casino.registry.ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        Direction f = level.getBlockState(to).getValue(FACING);
        BlockPos stand = to.relative(f).below();
        p.stopRiding();
        float yaw = f.toYRot();
        level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
        p.connection.teleport(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, yaw, 0.0F);
        level.playSound(null, stand, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 1.6F);
        Plat.msg(p, Component.translatable("casino.elevator.arrived", target.title()).withStyle(ChatFormatting.GOLD), true);
    }
}
