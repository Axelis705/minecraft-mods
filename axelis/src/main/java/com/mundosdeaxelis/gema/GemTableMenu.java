package com.mundosdeaxelis.gema;

import com.mundosdeaxelis.registry.ModBlocks;
import com.mundosdeaxelis.registry.ModMenus;
import com.mundosdeaxelis.table.Essence;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * La Mesa de Gemas (etapa 271). Dos zonas:
 * <ul>
 *     <li><b>Infundir</b>: una gema vacía + una esencia de infusión = una gema de ese elemento, con la pureza de la gema vacía
 *     (cuesta un poco de Esencia).</li>
 *     <li><b>Engarzar</b>: un arma + una gema = el arma con la gema puesta (si ya tenía una, vuelve al hueco de la gema).
 *     <b>Quitar</b> saca la gema del arma (cuesta Esencia).</li>
 * </ul>
 * Lo que dejes en la mesa vuelve a tu inventario al cerrar.
 */
public class GemTableMenu extends AbstractContainerMenu {
    public static final int B_INFUNDIR = 0, B_ENGARZAR = 1, B_QUITAR = 2;
    public static final int S_VACIA = 0, S_ESENCIA = 1, S_ARMA = 2, S_GEMA = 3;
    private final SimpleContainer box = new SimpleContainer(4);
    private final ContainerLevelAccess access;

    public GemTableMenu(int id, Inventory inv, ContainerLevelAccess access) {
        super(ModMenus.GEM_TABLE.get(), id);
        this.access = access;
        addSlot(new Slot(box, S_VACIA, 26, 36) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return st.getItem() instanceof GemaItems.Vacia || st.getItem() instanceof GemaItems.Gema;
            }
        });
        addSlot(new Slot(box, S_ESENCIA, 62, 36) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return st.getItem() instanceof GemaItems.Esencia;
            }
        });
        addSlot(new Slot(box, S_ARMA, 134, 36) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return Gemas.admite(st);
            }
        });
        addSlot(new Slot(box, S_GEMA, 170, 36) {
            @Override
            public boolean mayPlace(ItemStack st) {
                return st.getItem() instanceof GemaItems.Gema;
            }
        });
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inv, c + r * 9 + 9, 26 + c * 18, 118 + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inv, c, 26 + c * 18, 176));
        }
    }

    public static GemTableMenu client(int id, Inventory inv) {
        return new GemTableMenu(id, inv, ContainerLevelAccess.NULL);
    }

    /** Esencia que cuesta infundir una gema de esa pureza. */
    public static int infuseCost(int pureza) {
        return 20 * pureza;
    }

    public static final int REMOVE_COST = 15;

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (!(p instanceof ServerPlayer sp)) {
            return false;
        }
        switch (id) {
            case B_INFUNDIR -> infundir(sp);
            case B_ENGARZAR -> engarzar(sp);
            case B_QUITAR -> quitar(sp);
            default -> {
                return false;
            }
        }
        return true;
    }

    private void fx(ServerPlayer sp, boolean good, net.minecraft.sounds.SoundEvent ev) {
        ServerLevel sl = sp.serverLevel();
        sl.playSound(null, sp.blockPosition(), ev, SoundSource.PLAYERS, 0.9F, good ? 1.2F : 0.8F);
        if (good) {
            sl.sendParticles(ParticleTypes.ENCHANT, sp.getX(), sp.getY() + 1.4, sp.getZ(), 30, 0.5, 0.5, 0.5, 0.5);
        }
    }

    private void msg(ServerPlayer sp, String key, ChatFormatting col, Object... args) {
        sp.displayClientMessage(Component.translatable("message.mundos_de_axelis.gema." + key, args).withStyle(col), true);
    }

    private void infundir(ServerPlayer sp) {
        ItemStack v = box.getItem(S_VACIA), e = box.getItem(S_ESENCIA);
        if (!(v.getItem() instanceof GemaItems.Vacia) || !(e.getItem() instanceof GemaItems.Esencia es)) {
            msg(sp, "falta_infusion", ChatFormatting.RED);
            return;
        }
        int p = Gemas.pureza(v);
        int cost = infuseCost(p);
        if (!Essence.spend(sp, cost)) {
            msg(sp, "esencia", ChatFormatting.RED, cost);
            return;
        }
        Essence.sync(sp);
        v.shrink(1);
        e.shrink(1);
        ItemStack gema = Gemas.conPureza(new ItemStack(com.mundosdeaxelis.registry.ModItems.GEMAS.get(es.el).get()), p);
        if (box.getItem(S_VACIA).isEmpty()) {
            box.setItem(S_VACIA, gema);
        } else if (!sp.getInventory().add(gema)) {
            sp.drop(gema, false);
        }
        fx(sp, true, SoundEvents.AMETHYST_BLOCK_RESONATE);
        msg(sp, "infundida", ChatFormatting.LIGHT_PURPLE, gema.getHoverName());
        broadcastChanges();
    }

    private void engarzar(ServerPlayer sp) {
        ItemStack w = box.getItem(S_ARMA), g = box.getItem(S_GEMA);
        if (!Gemas.admite(w) || !(g.getItem() instanceof GemaItems.Gema gem)) {
            msg(sp, "falta_engarce", ChatFormatting.RED);
            return;
        }
        Gemas.Elemento old = Gemas.gemaDe(w);
        int oldP = Gemas.purezaDe(w);
        Gemas.engarzar(w, gem.el, Gemas.pureza(g));
        g.shrink(1);
        if (old != null) {
            ItemStack back = Gemas.conPureza(new ItemStack(com.mundosdeaxelis.registry.ModItems.GEMAS.get(old).get()), oldP);
            if (box.getItem(S_GEMA).isEmpty()) {
                box.setItem(S_GEMA, back);
            } else if (!sp.getInventory().add(back)) {
                sp.drop(back, false);
            }
        }
        fx(sp, true, SoundEvents.SMITHING_TABLE_USE);
        msg(sp, "engarzada", ChatFormatting.GOLD, w.getHoverName());
        broadcastChanges();
    }

    private void quitar(ServerPlayer sp) {
        ItemStack w = box.getItem(S_ARMA);
        Gemas.Elemento el = Gemas.gemaDe(w);
        if (el == null) {
            msg(sp, "sin_gema", ChatFormatting.RED);
            return;
        }
        if (!box.getItem(S_GEMA).isEmpty()) {
            msg(sp, "hueco_lleno", ChatFormatting.RED);
            return;
        }
        if (!Essence.spend(sp, REMOVE_COST)) {
            msg(sp, "esencia", ChatFormatting.RED, REMOVE_COST);
            return;
        }
        Essence.sync(sp);
        int p = Gemas.purezaDe(w);
        Gemas.quitar(w);
        box.setItem(S_GEMA, Gemas.conPureza(new ItemStack(com.mundosdeaxelis.registry.ModItems.GEMAS.get(el).get()), p));
        fx(sp, false, SoundEvents.GRINDSTONE_USE);
        msg(sp, "quitada", ChatFormatting.YELLOW);
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack st = slot.getItem();
            result = st.copy();
            if (index < 4) {
                if (!moveItemStackTo(st, 4, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                int target = st.getItem() instanceof GemaItems.Vacia ? S_VACIA : st.getItem() instanceof GemaItems.Esencia ? S_ESENCIA
                        : st.getItem() instanceof GemaItems.Gema ? S_GEMA : Gemas.admite(st) ? S_ARMA : -1;
                if (target < 0 || !moveItemStackTo(st, target, target + 1, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (st.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        access.execute((level, pos) -> clearContainer(p, box));
    }

    @Override
    public boolean stillValid(Player p) {
        return stillValid(access, p, ModBlocks.MESA_GEMAS.get());
    }
}
