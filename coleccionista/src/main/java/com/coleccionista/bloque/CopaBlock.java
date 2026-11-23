package com.coleccionista.bloque;

import com.coleccionista.prueba.LasCopas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Una copa de la final (oro con vino oscuro). En tu turno, clic derecho para beberla: una de ellas está envenenada. */
public class CopaBlock extends Block {
    private static final VoxelShape FORMA = Shapes.or(Block.box(5, 0, 5, 11, 1, 11), Block.box(7, 1, 7, 9, 5, 9), Block.box(5, 5, 5, 11, 10, 11));

    public CopaBlock(Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (!l.isClientSide() && p instanceof ServerPlayer sp) {
            LasCopas.beber(sp, pos);
        }
        return InteractionResult.sidedSuccess(l.isClientSide());
    }
}
