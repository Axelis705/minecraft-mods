package com.casino.decor;

import com.casino.plat.Plat;
import com.casino.plat.UseBlock;
import com.casino.registry.ModDecor;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

/**
 * Bocina de jazz: clic derecho la prende o la apaga. Mientras haya una prendida cerca suena la música del casino (una sola
 * pista aunque haya varias bocinas: se oye más fuerte cerca de la más cercana). El volumen sigue al de "Bloques musicales".
 */
public class SpeakerBlock extends UseBlock implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ON = BlockStateProperties.LIT;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotated(new double[]{2, 0, 4, 14, 16, 13});

    public SpeakerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ON, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, ON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPES.get(s.getValue(FACING));
    }

    @Override
    protected InteractionResult onUse(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            boolean on = !state.getValue(ON);
            level.setBlock(pos, state.setValue(ON, on), 3);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.5F, on ? 1.2F : 0.8F);
        }
        return Plat.ok(level.isClientSide());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Be(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide() || type != ModDecor.SPEAKER_BE.get()) {
            return null;
        }
        return (lvl, p, st, be) -> {
            if (st.getValue(ON)) {
                com.casino.plat.Net.onClient(() -> com.casino.client.SpeakerMusic.seen(p));
            }
        };
    }

    /** Solo existe para avisar cada tick (en el cliente) que la bocina está ahí. No guarda nada. */
    public static class Be extends BlockEntity {
        public Be(BlockPos pos, BlockState state) {
            super(ModDecor.SPEAKER_BE.get(), pos, state);
        }
    }
}
