package com.navidad.objeto;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.List;

/**
 * El saco del Krampus (de su botín): una mochila de costal con 27 espacios. Clic derecho para abrirla; las cosas se guardan en el
 * saco mismo (si lo tiras o se lo das a alguien, van adentro). No caben otros sacos ni cajas de shulker. Mientras lo traes en el
 * inventario se ve colgado en tu espalda.
 */
public class SacoKrampusItem extends Item {
    public static final int ESPACIOS = 27;

    public SacoKrampusItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        ItemStack st = p.getItemInHand(h);
        if (!l.isClientSide) {
            int slot = h == InteractionHand.MAIN_HAND ? p.getInventory().selected : Inventory.SLOT_OFFHAND;
            p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new Menu(id, inv, st, slot), st.getHoverName()));
            l.playSound(null, p.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
        return InteractionResultHolder.sidedSuccess(st, l.isClientSide);
    }

    public static boolean cabe(ItemStack st) {
        return !(st.getItem() instanceof SacoKrampusItem)
                && !(st.getItem() instanceof net.minecraft.world.item.BlockItem b && b.getBlock() instanceof ShulkerBoxBlock);
    }

    private static SimpleContainer leer(ItemStack saco) {
        NonNullList<ItemStack> cosas = NonNullList.withSize(ESPACIOS, ItemStack.EMPTY);
        CompoundTag t = saco.getTag();
        if (t != null && t.contains("Items")) {
            ContainerHelper.loadAllItems(t, cosas);
        }
        SimpleContainer c = new SimpleContainer(ESPACIOS) {
            @Override
            public boolean canPlaceItem(int i, ItemStack st) {
                return cabe(st);
            }
        };
        for (int i = 0; i < ESPACIOS; i++) {
            c.setItem(i, cosas.get(i));
        }
        return c;
    }

    private static void guardar(ItemStack saco, SimpleContainer c) {
        NonNullList<ItemStack> cosas = NonNullList.withSize(ESPACIOS, ItemStack.EMPTY);
        for (int i = 0; i < ESPACIOS; i++) {
            cosas.set(i, c.getItem(i));
        }
        ContainerHelper.saveAllItems(saco.getOrCreateTag(), cosas, true);
    }

    /** El cofre del saco (en el cliente se ve como un cofre normal de 3 filas). */
    static final class Menu extends ChestMenu {
        private final ItemStack saco;
        private final SimpleContainer cont;
        /** El espacio del inventario donde está el saco abierto (no se puede mover mientras). */
        private final int slotSaco;

        Menu(int id, Inventory inv, ItemStack saco, int slotInv) {
            this(id, inv, saco, slotInv, leer(saco));
        }

        private Menu(int id, Inventory inv, ItemStack saco, int slotInv, SimpleContainer c) {
            super(MenuType.GENERIC_9x3, id, inv, c, 3);
            this.saco = saco;
            this.cont = c;
            c.addListener(x -> guardar(saco, c));
            int encontrado = -1;
            for (int i = 0; i < slots.size(); i++) {
                Slot s = slots.get(i);
                if (s.container == inv && s.getContainerSlot() == slotInv) {
                    encontrado = i;
                }
            }
            this.slotSaco = encontrado;
        }

        @Override
        public void clicked(int slot, int boton, ClickType tipo, Player p) {
            if (slot == slotSaco && slot >= 0) {
                return;
            }
            if (tipo == ClickType.SWAP) {
                ItemStack otro = boton == 40 ? p.getOffhandItem() : p.getInventory().getItem(boton);
                if (otro == saco || slot >= 0 && slot < ESPACIOS && !cabe(otro)) {
                    return;
                }
            }
            if (slot >= 0 && slot < ESPACIOS && !cabe(getCarried()) && (tipo == ClickType.PICKUP || tipo == ClickType.QUICK_CRAFT)) {
                return;
            }
            super.clicked(slot, boton, tipo, p);
        }

        @Override
        public ItemStack quickMoveStack(Player p, int i) {
            if (i >= ESPACIOS && i < slots.size() && !cabe(slots.get(i).getItem())) {
                return ItemStack.EMPTY;
            }
            return super.quickMoveStack(p, i);
        }

        @Override
        public boolean stillValid(Player p) {
            return p.getMainHandItem() == saco || p.getOffhandItem() == saco;
        }

        @Override
        public void removed(Player p) {
            super.removed(p);
            guardar(saco, cont);
            p.level().playSound(null, p.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.7F);
        }
    }

    @Override
    public void appendHoverText(ItemStack st, Level l, List<Component> tip, TooltipFlag f) {
        int n = 0;
        CompoundTag t = st.getTag();
        if (t != null) {
            n = t.getList("Items", 10).size();
        }
        tip.add(Component.translatable("item.navidad.saco_krampus.ayuda", n, ESPACIOS).withStyle(ChatFormatting.GRAY));
    }
}
