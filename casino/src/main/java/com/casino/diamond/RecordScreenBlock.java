package com.casino.diamond;

import com.casino.decor.Shapes;
import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Pantalla de récords del casino (los que más ganan, apuestan, pierden...). Se pega a la pared; varias juntas forman una pantalla
 * grande. Clic derecho con la mano vacía: siguiente canal.
 */
public class RecordScreenBlock extends UseBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{0, 0, 14, 16, 16, 16});

    public RecordScreenBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // pegada a la pared que se clickeó; si se pone junto a otra pantalla, toma su dirección para unirse
        Direction f = ctx.getClickedFace().getAxis().isHorizontal() ? ctx.getClickedFace() : ctx.getHorizontalDirection().getOpposite();
        BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
        if (against.getBlock() instanceof RecordScreenBlock) {
            f = against.getValue(FACING);
        }
        return defaultBlockState().setValue(FACING, f);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            BlockPos a = RecordScreenBE.findAnchor(level, pos, state.getValue(FACING));
            if (level.getBlockEntity(a) instanceof RecordScreenBE be) {
                be.offset = Math.floorMod(be.offset + 1, RecordScreenBE.CHANNELS);
                be.sync();
                level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, 1.4F);
            }
        }
        return Plat.ok(level.isClientSide());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RecordScreenBE(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != DiamondReg.RECORD_BE.get()) {
            return null;
        }
        if (level.isClientSide()) {
            return (lvl, p, st, be) -> ((RecordScreenBE) be).clientTick(lvl);
        }
        return (lvl, p, st, be) -> RecordScreenBE.serverTick(lvl, p, st, (RecordScreenBE) be);
    }
}
