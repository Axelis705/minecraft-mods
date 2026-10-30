package com.casino.block;

import com.casino.client.ClientHooks;
import com.casino.entity.DealerEntity;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import javax.annotation.Nullable;

/**
 * La Mesa de blackjack: 3 bloques de ancho. El bloque que se coloca es el del centro (ahí vive el bloque-entidad); los otros dos
 * son los extremos de la mesa. El crupier (una entidad) se queda de pie detrás del centro; el jugador se para enfrente.
 */
public class BlackjackBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 0 = izquierda, 1 = centro, 2 = derecha (mirando la mesa de frente, desde el lado del jugador). */
    public static final IntegerProperty PX = IntegerProperty.create("px", 0, 2);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public BlackjackBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PX, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, PX);
    }

    private static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static BlockPos cell(BlockPos master, Direction facing, int px) {
        return master.relative(right(facing), px - 1);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        return pos.relative(right(st.getValue(FACING)).getOpposite(), st.getValue(PX) - 1);
    }

    /** Donde se para el crupier: el bloque de detrás del centro de la mesa. */
    public static BlockPos dealerSpot(BlockPos master, Direction facing) {
        return master.relative(facing.getOpposite());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection().getOpposite(); // el frente de la mesa mira a quien la pone
        Level level = ctx.getLevel();
        BlockPos master = ctx.getClickedPos();
        for (int x = 0; x < 3; x++) {
            BlockPos p = cell(master, f, x);
            if (!level.isInWorldBounds(p) || (x != 1 && !level.getBlockState(p).canBeReplaced(ctx))) {
                return null;
            }
        }
        BlockPos back = dealerSpot(master, f);
        if (!level.getBlockState(back).getCollisionShape(level, back).isEmpty() || !level.getBlockState(back.above()).getCollisionShape(level, back.above()).isEmpty()) {
            return null; // no hay lugar para el crupier
        }
        return defaultBlockState().setValue(FACING, f).setValue(PX, 1);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int x = 0; x < 3; x++) {
            if (x != 1) {
                level.setBlock(cell(pos, f, x), defaultBlockState().setValue(FACING, f).setValue(PX, x), 3);
            }
        }
        if (level instanceof net.minecraft.server.level.ServerLevel sl && level.getBlockEntity(pos) instanceof BlackjackBE be) {
            be.spawnDealer(sl);
        }
    }

    private void removeOthers(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos master = masterOf(pos, state);
        for (int x = 0; x < 3; x++) {
            BlockPos p = cell(master, f, x);
            if (p.equals(pos)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            if (s.is(this) && s.getValue(FACING) == f && s.getValue(PX) == x) {
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
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            removeOthers(level, pos, state);
            if (!player.isCreative()) {
                Block.popResource(level, masterOf(pos, state), new ItemStack(asItem()));
            }
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            removeOthers(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            BlockPos master = masterOf(pos, state);
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openBlackjack(master));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PX) == 1 ? new BlackjackBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != ModBlockEntities.BLACKJACK.get()) {
            return null;
        }
        if (level.isClientSide) {
            return (lvl, p, st, be) -> ((BlackjackBE) be).clientTick();
        }
        return (lvl, p, st, be) -> ((BlackjackBE) be).serverTick();
    }
}
