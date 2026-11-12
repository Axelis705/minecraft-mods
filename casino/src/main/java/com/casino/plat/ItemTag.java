package com.casino.plat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
//#if MC >= 1.20.5
//$$ import net.minecraft.core.component.DataComponents;
//$$ import net.minecraft.world.item.component.CustomData;
//#endif

import java.util.function.Consumer;

/** Datos propios guardados en un objeto (antes NBT del objeto, desde 1.20.5 el componente custom_data). */
public final class ItemTag {
    private ItemTag() {
    }

    /** Copia de los datos (vacía si no tiene). */
    public static CompoundTag get(ItemStack st) {
        //#if MC >= 1.20.5
        //$$ return st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        //#else
        return st.hasTag() ? st.getTag().copy() : new CompoundTag();
        //#endif
    }

    public static void edit(ItemStack st, Consumer<CompoundTag> fn) {
        //#if MC >= 1.20.5
        //$$ CustomData.update(DataComponents.CUSTOM_DATA, st, fn);
        //#else
        fn.accept(st.getOrCreateTag());
        //#endif
    }
}
