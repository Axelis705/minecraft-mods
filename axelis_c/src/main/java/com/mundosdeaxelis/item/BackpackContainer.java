package com.mundosdeaxelis.item;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/** El inventario de una mochila: 27 espacios que se guardan dentro del propio objeto. */
public class BackpackContainer extends SimpleContainer {
    public static final int SIZE = 27;
    private static final String TAG = "BackpackItems";

    private final ItemStack bag;
    private boolean loading = true;

    public BackpackContainer(ItemStack bag) {
        super(SIZE);
        this.bag = bag;
        CompoundTag tag = bag.getTag();
        if (tag != null && tag.contains(TAG)) {
            NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag.getCompound(TAG), list);
            for (int i = 0; i < SIZE; i++) {
                setItem(i, list.get(i));
            }
        }
        loading = false;
    }

    /** Cuántos espacios están ocupados (para mostrarlo en la descripción). */
    public static int filledSlots(ItemStack bag) {
        CompoundTag tag = bag.getTag();
        if (tag == null || !tag.contains(TAG)) {
            return 0;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound(TAG), list);
        int n = 0;
        for (ItemStack s : list) {
            if (!s.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // Una mochila no puede guardarse dentro de otra mochila.
        return !(stack.getItem() instanceof PocketBackpackItem);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (loading) {
            return;
        }
        NonNullList<ItemStack> list = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        boolean any = false;
        for (int i = 0; i < SIZE; i++) {
            list.set(i, getItem(i));
            any |= !getItem(i).isEmpty();
        }
        if (any) {
            bag.getOrCreateTag().put(TAG, ContainerHelper.saveAllItems(new CompoundTag(), list));
        } else if (bag.hasTag()) {
            bag.getTag().remove(TAG);
            if (bag.getTag().isEmpty()) {
                bag.setTag(null);
            }
        }
    }

    @Override
    public boolean stillValid(net.minecraft.world.entity.player.Player player) {
        return true;
    }
}
