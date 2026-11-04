package com.mundosdeaxelis.relic;

import com.mundosdeaxelis.registry.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menú de equipo: una fila de ranuras para mascotas, otra para reliquias y el inventario del jugador debajo.
 * Funciona como cualquier cofre (arrastrar, clic, Mayús + clic), con reglas de qué cabe en cada ranura.
 */
public class EquipMenu extends AbstractContainerMenu {
    public static final int SLOT_STEP = 20;
    public static final int PET_X = 8, PET_Y = 33;
    public static final int RELIC_X = 8, RELIC_Y = 70;
    /** Ranura de la capa: debajo de las reliquias. */
    public static final int CAPE_X = 8, CAPE_Y = 123;
    public static final int INV_Y = 156;

    public final int petN;
    public final int relicN;
    private final Container pets;
    private final Container relics;
    private final Container cape;

    public EquipMenu(int id, Inventory inv, Container pets, Container relics, Container cape, int petN, int relicN) {
        super(ModMenus.EQUIP.get(), id);
        this.pets = pets;
        this.relics = relics;
        this.cape = cape;
        this.petN = petN;
        this.relicN = relicN;
        for (int i = 0; i < petN; i++) {
            addSlot(new EquipSlot(pets, i, PET_X + i * SLOT_STEP, PET_Y, true));
        }
        for (int i = 0; i < relicN; i++) {
            addSlot(new EquipSlot(relics, i, RELIC_X + i * SLOT_STEP, RELIC_Y, false));
        }
        addSlot(new CapeSlot(cape, 0, CAPE_X, CAPE_Y));
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, 9 + r * 9 + c, 8 + c * 18, INV_Y + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 8 + c * 18, INV_Y + 58));
        }
    }

    /** Ranura que solo admite lo que le corresponde (mascotas o reliquias de tecla, sin repetir). */
    private static final class EquipSlot extends Slot {
        private final boolean pet;

        EquipSlot(Container c, int i, int x, int y, boolean pet) {
            super(c, i, x, y);
            this.pet = pet;
        }

        @Override
        public boolean mayPlace(ItemStack s) {
            if (!(s.getItem() instanceof RelicItem r) || r.isPet() != pet || r.kind() == RelicItem.Kind.HAND) {
                return false;
            }
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (i != getContainerSlot() && container.getItem(i).is(s.getItem())) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** Ranura de la capa: solo admite capas del mod. */
    private static final class CapeSlot extends Slot {
        CapeSlot(Container c, int i, int x, int y) {
            super(c, i, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack s) {
            return s.getItem() instanceof com.mundosdeaxelis.item.CapeItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int eq = petN + relicN + 1;
            if (index < eq) {
                if (!moveItemStackTo(stack, eq, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, eq, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer sp) {
            Equipment.save(sp);
            Equipment.sync(sp);
        }
    }
}
