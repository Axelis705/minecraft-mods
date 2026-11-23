package com.coleccionista.bloque;

import com.coleccionista.prueba.Traidor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** El teclado de la bóveda del Archivo (El traidor): al usarlo se abre la ventana para escribir el código del equipo. */
public class TecladoBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape N = Block.box(4, 2, 13, 12, 14, 16), S = Block.box(4, 2, 0, 12, 14, 3), E = Block.box(0, 2, 4, 3, 14, 12),
            W = Block.box(13, 2, 4, 16, 14, 12);

    public TecladoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        Direction d = c.getClickedFace().getAxis().isHorizontal() ? c.getClickedFace() : c.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, d);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return switch (s.getValue(FACING)) {
            case NORTH -> N;
            case EAST -> E;
            case WEST -> W;
            default -> S;
        };
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (!l.isClientSide() && p instanceof ServerPlayer sp) {
            Traidor.usarTeclado(sp, pos);
        }
        return InteractionResult.sidedSuccess(l.isClientSide());
    }
}
