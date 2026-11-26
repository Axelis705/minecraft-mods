package com.navidad.bloque.deco;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * El catre de la celda: es una cama de verdad (el evento acuesta a las piezas en la sala de espera), pero con su propio modelo de metal y
 * colchón viejo en vez de la cama de lana.
 */
public class DecoCama extends BedBlock {
    public DecoCama(BlockBehaviour.Properties p) {
        super(DyeColor.GRAY, p);
    }

    @Override
    @SuppressWarnings("deprecation")
    public RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p, BlockState s) {
        return null;
    }
}
