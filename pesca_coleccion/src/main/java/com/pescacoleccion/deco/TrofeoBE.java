package com.pescacoleccion.deco;

import com.axeliscore.block.CoreBlockEntity;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.Ser;
import com.pescacoleccion.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** La placa de trofeo guarda un pez (con su tamaño y quién lo pescó). */
public class TrofeoBE extends CoreBlockEntity {
    public ItemStack pez = ItemStack.EMPTY;

    public TrofeoBE(BlockPos pos, BlockState state) {
        super(ModRegistry.TROFEO_BE.get(), pos, state);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        if (!pez.isEmpty()) {
            tag.put("pez", Ser.item(pez));
        }
    }

    @Override
    protected void readData(CompoundTag tag) {
        pez = Nbt.has(tag, "pez") ? Ser.item(tag.get("pez")) : ItemStack.EMPTY;
    }
}
