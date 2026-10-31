package com.pooltable.block;

import com.pooltable.registry.ModBlockEntities;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * La mesa de hockey de mesa: 3 bloques de largo (u = 0..2, en la dirección en que mira quien la pone) por 2 de ancho (v = 0..1, hacia su derecha).
 * Al poner el bloque se colocan las 6 partes; solo la parte (0,0) tiene bloque-entidad (la partida). Todo se dibuja con el renderizador.
 */
public class AirHockeyBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final int NU = 3;
    public static final int NV = 2;
    public static final IntegerProperty U = IntegerProperty.create("u", 0, NU - 1);
    public static final IntegerProperty V = IntegerProperty.create("v", 0, NV - 1);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 14, 16);

    public AirHockeyBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.EAST).setValue(U, 0).setValue(V, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, U, V);
    }

    public static BlockPos cell(BlockPos master, Direction facing, int u, int v) {
        return master.relative(facing, u).relative(facing.getClockWise(), v);
    }

    public static BlockPos masterOf(BlockPos pos, BlockState st) {
        Direction f = st.getValue(FACING);
        return pos.relative(f.getOpposite(), st.getValue(U)).relative(f.getClockWise().getOpposite(), st.getValue(V));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction f = ctx.getHorizontalDirection();
        Level level = ctx.getLevel();
        BlockPos master = ctx.getClickedPos();
        for (int u = 0; u < NU; u++) {
            for (int v = 0; v < NV; v++) {
                BlockPos p = cell(master, f, u, v);
                if (!level.isInWorldBounds(p) || (!(u == 0 && v == 0) && !level.getBlockState(p).canBeReplaced(ctx))) {
                    return null;
                }
            }
        }
        return defaultBlockState().setValue(FACING, f).setValue(U, 0).setValue(V, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        Direction f = state.getValue(FACING);
        for (int u = 0; u < NU; u++) {
            for (int v = 0; v < NV; v++) {
                if (u == 0 && v == 0) {
                    continue;
                }
                level.setBlock(cell(pos, f, u, v), defaultBlockState().setValue(FACING, f).setValue(U, u).setValue(V, v), 3);
            }
        }
        if (level.getBlockEntity(pos) instanceof AirHockeyBE t) {
            t.sync();
        }
    }

    private void removeOthers(Level level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(FACING);
        BlockPos master = masterOf(pos, state);
        for (int u = 0; u < NU; u++) {
            for (int v = 0; v < NV; v++) {
                BlockPos p = cell(master, f, u, v);
                if (p.equals(pos)) {
                    continue;
                }
                BlockState s = level.getBlockState(p);
                if (s.is(this) && s.getValue(FACING) == f && s.getValue(U) == u && s.getValue(V) == v) {
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 35);
                }
            }
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            removeOthers(level, pos, state);
            if (!player.isCreative()) {
                Block.popResource(level, pos, new ItemStack(asItem()));
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
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            BlockPos master = masterOf(pos, state);
            net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT, () -> () -> com.pooltable.client.PoolClient.openHockey(master));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(U) == 0 && state.getValue(V) == 0 ? new AirHockeyBE(pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.AIR_HOCKEY.get()) {
            return null;
        }
        return (lvl, p, st, be) -> ((AirHockeyBE) be).serverTick();
    }
}
