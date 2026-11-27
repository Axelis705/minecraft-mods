package com.navidad.nieve;

import com.navidad.registry.ModRegistry;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Las ranuras del tanque de nieve (6, para bolas o bloques de nieve) con tu inventario abajo. El tanque puede estar en tu mano o
 * puesto en la espalda; lo que metes o sacas se guarda en él al momento.
 */
public class TanqueMenu extends AbstractContainerMenu {
    public static final int MANO = 0, MANO_IZQ = 1, ESPALDA = 2;
    public final int donde;
    private final Player jugador;
    private final SimpleContainer cont;
    private boolean cargando;

    public TanqueMenu(int id, Inventory inv, int donde) {
        super(ModRegistry.TANQUE_MENU.get(), id);
        this.donde = donde;
        this.jugador = inv.player;
        this.cont = new SimpleContainer(TanqueNieve.RANURAS) {
            @Override
            public void setChanged() {
                super.setChanged();
                guardarEnTanque();
            }
        };
        if (!jugador.level().isClientSide) {
            cargando = true;
            NonNullList<ItemStack> l = TanqueNieve.ranuras(tanque());
            for (int i = 0; i < l.size(); i++) {
                cont.setItem(i, l.get(i));
            }
            cargando = false;
        }
        for (int i = 0; i < TanqueNieve.RANURAS; i++) {
            addSlot(new Slot(cont, i, 98 + (i % 3) * 18, 24 + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack st) {
                    return TanqueNieve.tiros(st) > 0;
                }
            });
        }
        for (int fila = 0; fila < 3; fila++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + fila * 9 + 9, 8 + col * 18, 84 + fila * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            final boolean bloqueada = donde == MANO && col == inv.selected;
            addSlot(new Slot(inv, col, 8 + col * 18, 142) {
                @Override
                public boolean mayPickup(Player p) {
                    return !bloqueada && super.mayPickup(p);
                }
            });
        }
    }

    /** Abre las ranuras del tanque de alguien (en su mano o en su espalda). */
    public static void abrir(ServerPlayer sp, int donde) {
        net.minecraftforge.network.NetworkHooks.openScreen(sp, new SimpleMenuProvider((id, inv, p) -> new TanqueMenu(id, inv, donde),
                Component.translatable("item.navidad.tanque_nieve")), buf -> buf.writeByte(donde));
    }

    public ItemStack tanque() {
        return switch (donde) {
            case MANO -> jugador.getMainHandItem();
            case MANO_IZQ -> jugador.getOffhandItem();
            default -> jugador.getItemBySlot(EquipmentSlot.CHEST);
        };
    }

    private void guardarEnTanque() {
        if (cargando || jugador.level().isClientSide) {
            return;
        }
        ItemStack t = tanque();
        if (!(t.getItem() instanceof TanqueNieve)) {
            return;
        }
        NonNullList<ItemStack> l = NonNullList.withSize(TanqueNieve.RANURAS, ItemStack.EMPTY);
        for (int i = 0; i < l.size(); i++) {
            l.set(i, cont.getItem(i).copy());
        }
        TanqueNieve.guardar(t, l);
    }

    /** Los tiros que hay en las ranuras (lo suelto lo sabe el tanque). */
    public int tirosEnRanuras() {
        int n = 0;
        for (int i = 0; i < TanqueNieve.RANURAS; i++) {
            ItemStack s = cont.getItem(i);
            n += TanqueNieve.tiros(s) * s.getCount();
        }
        return n;
    }

    @Override
    public boolean stillValid(Player p) {
        return tanque().getItem() instanceof TanqueNieve;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int i) {
        Slot s = slots.get(i);
        if (!s.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack st = s.getItem(), antes = st.copy();
        int n = TanqueNieve.RANURAS;
        if (i < n) {
            if (!moveItemStackTo(st, n, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (TanqueNieve.tiros(st) > 0) {
            if (!moveItemStackTo(st, 0, n, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (st.isEmpty()) {
            s.set(ItemStack.EMPTY);
        } else {
            s.setChanged();
        }
        return antes;
    }
}
