package com.casino.diamond;

import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

/** Datos de una parada del elevador: nombre del piso y tarjeta que pide (0 nada, 1 Plata, 2 Oro). */
public class ElevatorBE extends CasinoBE {
    public String floor = "";
    public int access = 0;

    public ElevatorBE(BlockPos pos, BlockState state) {
        super(DiamondReg.ELEVATOR_BE.get(), pos, state);
    }

    /** Nombre para mostrar: los pisos de THE DIAMOND tienen traducción; cualquier otro nombre se muestra tal cual. */
    public Component title() {
        if (floor.isEmpty()) {
            return Component.translatable("casino.elevator.floor_n", worldPosition.getY());
        }
        return Component.translatableWithFallback("casino.elevator.floor." + floor, floor);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putString("Floor", floor);
        tag.putInt("Access", access);
    }

    @Override
    protected void readData(CompoundTag tag) {
        floor = Nbt.s(tag, "Floor");
        access = Nbt.i(tag, "Access");
    }
}
