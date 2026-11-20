package com.mundosdeaxelis.relic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * Lo que cada mascota recuerda (etapa 254). Va guardado en su figurita, así viaja con ella: nombre, cariño, modo de combate,
 * si tiene encendida la luz o el «recoger», cuándo puede volver a usar su acción especial, el día de su último regalo y la mochila.
 */
public final class PetData {
    private PetData() {
    }

    public static final String KEY = "mdc_pet";
    public static final int MAX_LOVE = 100;
    public static final int MAX_NAME = 24;

    public static CompoundTag read(ItemStack st) {
        CompoundTag t = st.isEmpty() ? null : st.getTagElement(KEY);
        return t == null ? new CompoundTag() : t;
    }

    public static CompoundTag edit(ItemStack st) {
        return st.getOrCreateTagElement(KEY);
    }

    public static String name(ItemStack st) {
        return read(st).getString("Name");
    }

    public static int love(ItemStack st) {
        return Math.max(0, Math.min(MAX_LOVE, read(st).getInt("Love")));
    }

    public static int mode(ItemStack st) {
        return read(st).getByte("Mode");
    }

    public static boolean flag(ItemStack st, String key) {
        return read(st).getBoolean(key);
    }

    /** Carga la mochila guardada en la figurita. */
    public static void loadBag(ItemStack st, SimpleContainer bag) {
        ListTag list = read(st).getList("Bag", Tag.TAG_COMPOUND);
        for (int i = 0; i < bag.getContainerSize(); i++) {
            bag.setItem(i, ItemStack.EMPTY);
        }
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int slot = e.getByte("Slot") & 255;
            if (slot < bag.getContainerSize()) {
                bag.setItem(slot, ItemStack.of(e));
            }
        }
    }

    /** Guarda la mochila en la figurita. */
    public static void saveBag(ItemStack st, SimpleContainer bag) {
        if (st.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (int i = 0; i < bag.getContainerSize(); i++) {
            ItemStack it = bag.getItem(i);
            if (!it.isEmpty()) {
                CompoundTag e = new CompoundTag();
                e.putByte("Slot", (byte) i);
                it.save(e);
                list.add(e);
            }
        }
        edit(st).put("Bag", list);
    }

    /** Cuántos objetos lleva en la mochila (para la descripción de la figurita). */
    public static int bagCount(ItemStack st) {
        return read(st).getList("Bag", Tag.TAG_COMPOUND).size();
    }
}
