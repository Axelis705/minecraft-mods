package com.mundosdeaxelis.cartas;

import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** El jugador coloca UNA carta en su hueco del album. El servidor comprueba que la lleve encima y que el hueco este libre. */
public final class AlbumInsert {
    private AlbumInsert() {
    }

    public static void place(ServerPlayer sp, String id, boolean holo) {
        AlbumCap cap = AlbumCaps.of(sp);
        double before = cap.bonusHealth();
        boolean ok = false;
        CardDef d = Cards.get(id);
        if (d != null && !cap.has(id, holo)) {
            ItemStack s = find(sp, d, holo);
            if (s != null) {
                s.shrink(1);
                cap.add(id, holo);
                sp.getInventory().setChanged();
                AlbumBonuses.apply(sp);
                ok = true;
            }
        }
        List<String> ids = ok ? List.of(id) : List.of();
        List<Boolean> hs = ok ? List.of(holo) : List.of();
        CartasNet.toPlayer(sp, new CartasNet.InsertResult(ids, hs, cap.normal, cap.holo, (float) before, (float) cap.bonusHealth()));
    }

    private static ItemStack find(ServerPlayer sp, CardDef d, boolean holo) {
        net.minecraft.world.item.Item item = CartasItems.CARDS.get(d.id).get();
        for (ItemStack s : sp.getInventory().items) {
            if (!s.isEmpty() && s.getItem() == item && CardItem.isHolo(s) == holo) {
                return s;
            }
        }
        for (ItemStack s : sp.getInventory().offhand) {
            if (!s.isEmpty() && s.getItem() == item && CardItem.isHolo(s) == holo) {
                return s;
            }
        }
        return null;
    }
}
