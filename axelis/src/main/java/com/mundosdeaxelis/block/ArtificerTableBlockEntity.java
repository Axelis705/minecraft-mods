package com.mundosdeaxelis.block;

import com.mundosdeaxelis.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** No guarda nada: existe solo para que el cristal flotante de la mesa se dibuje animado. */
public class ArtificerTableBlockEntity extends BlockEntity {
    public ArtificerTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARTIFICER_TABLE.get(), pos, state);
    }
}
