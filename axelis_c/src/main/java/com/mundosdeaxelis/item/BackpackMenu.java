package com.mundosdeaxelis.item;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Pantalla de la mochila (3 filas, como un cofre). No deja mover la mochila mientras está abierta. */
public class BackpackMenu extends ChestMenu {
    private final ItemStack bag;

    public BackpackMenu(int id, Inventory inv, BackpackContainer container, ItemStack bag) {
        super(MenuType.GENERIC_9x3, id, inv, container, 3);
        this.bag = bag;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < this.slots.size()) {
            Slot slot = this.slots.get(slotId);
            if (slot.getItem() == bag) {
                return; // la mochila abierta no se puede mover
            }
            if (type == ClickType.SWAP && button >= 0 && button < 9) {
                // Intercambio con la barra rápida: tampoco con la mochila.
                if (player.getInventory().getItem(button) == bag) {
                    return;
                }
            }
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return !bag.isEmpty() && (player.getInventory().contains(bag) || player.getMainHandItem() == bag
                || player.getOffhandItem() == bag);
    }
}
