package com.mundosdeaxelis.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
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

/**
 * Piedra de regreso (etapa 263): está junto al altar del santuario del volcán. Con clic derecho te lleva a la entrada del volcán
 * (al pie del camino, mirando a la montaña). Es la única salida: al volcán solo se entra tirándose al lago del cráter.
 */
public class PiedraRegresoBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(3, 2, 3, 13, 13, 13), Block.box(2, 13, 2, 14, 16, 14));

    public PiedraRegresoBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            // etapa 282: primero una ventanita; «Confirmar» se activa a los 3 s (así nadie sale sin querer)
            com.mundosdeaxelis.world.Confirmar.pedir(sp, "salida_volcan", pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
        // chispas que suben (como si la piedra tirara hacia arriba) y alguna llama de almas
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.3 + r.nextDouble() * 0.4, pos.getY() + 1.0, pos.getZ() + 0.3 + r.nextDouble() * 0.4,
                    0.0, 0.03 + r.nextDouble() * 0.03, 0.0);
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5 + (r.nextDouble() - 0.5) * 0.9, pos.getY() + 0.2 + r.nextDouble() * 0.6,
                    pos.getZ() + 0.5 + (r.nextDouble() - 0.5) * 0.9, 0.0, 0.05, 0.0);
        }
        if (r.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0.0, 0.08, 0.0);
        }
    }
}
