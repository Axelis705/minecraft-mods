package com.autoscoleccion.block;

import com.autoscoleccion.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Solo guarda lo que necesita la animación: cuándo soltó la última caja (y cuál) y cuándo le metieron una moneda. */
public class VendingMachineBlockEntity extends BlockEntity {
    public long dispenseTime = -1000;
    public int dispenseBox;
    public long coinTime = -1000;

    public VendingMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VENDING_MACHINE.get(), pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (level == null) {
            return false;
        }
        if (id == 1) {
            dispenseTime = level.getGameTime();
            dispenseBox = param;
            return true;
        }
        if (id == 2) {
            coinTime = level.getGameTime();
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 3, 2));
    }
}
