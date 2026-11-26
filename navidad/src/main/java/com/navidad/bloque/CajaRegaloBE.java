package com.navidad.bloque;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Lo que guarda una caja de regalo: el objeto envuelto, quién lo envolvió y para quién es (si tiene nombre). */
public class CajaRegaloBE extends BlockEntity {
    public ItemStack contenido = ItemStack.EMPTY;
    public String de = "", para = "";

    public CajaRegaloBE(BlockPos p, BlockState s) {
        super(ModRegistry.CAJA_REGALO_BE.get(), p, s);
    }

    @Override
    public void load(CompoundTag t) {
        super.load(t);
        contenido = t.contains("Contenido") ? ItemStack.of(t.getCompound("Contenido")) : ItemStack.EMPTY;
        de = t.getString("De");
        para = t.getString("Para");
    }

    @Override
    protected void saveAdditional(CompoundTag t) {
        super.saveAdditional(t);
        if (!contenido.isEmpty()) {
            t.put("Contenido", contenido.save(new CompoundTag()));
        }
        t.putString("De", de);
        t.putString("Para", para);
    }
}
