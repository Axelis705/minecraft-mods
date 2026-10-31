package com.casino.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Los 8 símbolos del rasca y gana (de menor a mayor premio) y cómo se dibujan. */
final class ScratchSymbols {
    private static final Item[] ITEMS = {Items.IRON_INGOT, Items.COPPER_INGOT, Items.GOLD_INGOT, Items.LAPIS_LAZULI, Items.EMERALD, Items.DIAMOND,
            Items.NETHERITE_INGOT, Items.NETHER_STAR};

    private ScratchSymbols() {
    }

    static ItemStack stack(int symbol) {
        return new ItemStack(ITEMS[Math.max(0, Math.min(ITEMS.length - 1, symbol))]);
    }
}
