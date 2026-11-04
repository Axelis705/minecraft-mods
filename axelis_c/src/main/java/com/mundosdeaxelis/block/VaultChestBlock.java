package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cofre del santuario (el de la dungeon). Cada jugador tiene su propio botín dentro, así que no hay peleas por el contenido.
 * La versión sellada necesita una Llave del Santuario (una por jugador). No se puede romper en supervivencia.
 */
public class VaultChestBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE_NS = Block.box(0.5, 0.0, 1.5, 15.5, 13.0, 14.5);
    private static final VoxelShape SHAPE_EW = Block.box(1.5, 0.0, 0.5, 14.5, 13.0, 15.5);
    public final boolean sealed;
    /** Cofre con cerradura: se abre con el minijuego de la ganzúa (cada jugador abre la suya). */
    public final boolean locked;

    public VaultChestBlock(Properties properties, boolean sealed) {
        this(properties, sealed, false);
    }

    public VaultChestBlock(Properties properties, boolean sealed, boolean locked) {
        super(properties);
        this.sealed = sealed;
        this.locked = locked;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @javax.annotation.Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VaultChestBlockEntity(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof VaultChestBlockEntity be) {
            be.open(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(6) == 0) {
            level.addParticle(sealed ? ParticleTypes.SOUL_FIRE_FLAME : locked ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.2 + r.nextDouble() * 0.6, pos.getY() + 1.0 + r.nextDouble() * 0.3, pos.getZ() + 0.2 + r.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        }
    }
}
