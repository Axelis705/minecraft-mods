package com.coleccionista.bloque;

import com.coleccionista.evento.Celdas;
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

/**
 * El candado de la celda: un candado de combinación con tres ruedas, colgado de los barrotes junto a la puerta, por dentro. Al usarlo la
 * cámara se acerca y la mano del jugador gira las ruedas (los 3 números están escondidos en la celda); si es el bueno, se abre la puerta.
 */
public class CandadoBlock extends HorizontalDirectionalBlock {
    /**
     * El candado cuelga de los barrotes del bloque de enfrente (el modelo está corrido 7 px hacia los barrotes): su forma también, para
     * que se pueda apuntar y hacer clic en él donde se ve.
     */
    private static final VoxelShape N = Block.box(3.5, 1.5, 19, 12.5, 14.5, 23), S = Block.box(3.5, 1.5, -7, 12.5, 14.5, -3),
            E = Block.box(-7, 1.5, 3.5, -3, 14.5, 12.5), W = Block.box(19, 1.5, 3.5, 23, 14.5, 12.5);

    public CandadoBlock(Properties p) {
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
            Celdas.usarCandado(sp, pos);
        }
        return InteractionResult.sidedSuccess(l.isClientSide());
    }
}
