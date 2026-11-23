package com.coleccionista.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tubería delgada en cualquier eje (por el techo, las paredes o el piso). */
public class DecoTubo extends RotatedPillarBlock {
    private static final VoxelShape Y = Block.box(5, 0, 5, 11, 16, 11), X = Block.box(0, 5, 5, 16, 11, 11), Z = Block.box(5, 5, 0, 11, 11, 16);

    public DecoTubo(BlockBehaviour.Properties p) {
        super(p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState s, BlockGetter g, BlockPos p, CollisionContext c) {
        Direction.Axis a = s.getValue(AXIS);
        return a == Direction.Axis.X ? X : a == Direction.Axis.Z ? Z : Y;
    }
}
