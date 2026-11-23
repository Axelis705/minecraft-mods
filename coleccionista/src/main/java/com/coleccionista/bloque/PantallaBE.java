package com.coleccionista.bloque;

import com.coleccionista.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** La pantalla no guarda nada: lo que muestra es igual en todas y lo manda el evento. Solo existe para poder dibujarla grande. */
public class PantallaBE extends BlockEntity {
    public PantallaBE(BlockPos pos, BlockState state) {
        super(ModRegistry.PANTALLA_BE.get(), pos, state);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        // se dibuja más grande que el bloque (3x2 o 11x7)
        return new net.minecraft.world.phys.AABB(worldPosition).inflate(7);
    }
}
