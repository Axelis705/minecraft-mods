package com.mundosdeaxelis.table;

import com.mundosdeaxelis.crate.Tier;
import com.mundosdeaxelis.crate.Tiers;
import com.mundosdeaxelis.relic.Equipment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Vínculo de alma: un objeto vinculado a ti no se pierde al morir (no va a la lápida: vuelve contigo al reaparecer).
 * Se guarda en el propio objeto: {@code mdc_bound} (UUID del dueño) y {@code mdc_bound_name}.
 */
public final class Bound {
    private Bound() {
    }

    public static final String TAG = "mdc_bound";
    public static final String NAME = "mdc_bound_name";
    /** Esencia que cuesta vincular, según la rareza del objeto. */
    private static final int[] COST = {20, 40, 80, 160, 320, 640, 1100, 1800};

    public static int cost(Tier t) {
        return COST[t.ordinal()];
    }

    public static int cost(ItemStack s) {
        return cost(Tiers.of(s));
    }

    public static boolean isBound(ItemStack s) {
        return !s.isEmpty() && s.hasTag() && s.getTag().contains(TAG);
    }

    public static boolean isBoundTo(ItemStack s, Player p) {
        return isBound(s) && s.getTag().getString(TAG).equals(p.getUUID().toString());
    }

    public static String ownerName(ItemStack s) {
        return isBound(s) ? s.getTag().getString(NAME) : "";
    }

    public static void bind(ItemStack s, Player p) {
        CompoundTag t = s.getOrCreateTag();
        t.putString(TAG, p.getUUID().toString());
        t.putString(NAME, p.getGameProfile().getName());
    }

    public static void unbind(ItemStack s) {
        if (s.hasTag()) {
            s.getTag().remove(TAG);
            s.getTag().remove(NAME);
            if (s.getTag().isEmpty()) {
                s.setTag(null);
            }
        }
    }

    /** Cuántos objetos tiene ya vinculados (en el inventario y en el menú de equipo). */
    public static int count(ServerPlayer sp) {
        int n = 0;
        Inventory inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (isBoundTo(inv.getItem(i), sp)) {
                n++;
            }
        }
        Equipment.Data d = Equipment.get(sp);
        for (ItemStack s : d.pets) {
            if (isBoundTo(s, sp)) {
                n++;
            }
        }
        for (ItemStack s : d.relics) {
            if (isBoundTo(s, sp)) {
                n++;
            }
        }
        return n;
    }
}
