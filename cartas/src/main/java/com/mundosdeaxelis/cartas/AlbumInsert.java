package com.mundosdeaxelis.cartas;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Mete en el album las cartas nuevas que el jugador lleva encima (una de cada; las repetidas se quedan en el inventario). */
public final class AlbumInsert {
    private AlbumInsert() {
    }

    public static void run(ServerPlayer sp) {
        AlbumCap cap = AlbumCaps.of(sp);
        double before = cap.bonusHealth();
        List<String> ids = new ArrayList<>();
        List<Boolean> holo = new ArrayList<>();
        // las normales primero, luego las holograficas; en el orden del album
        for (int pass = 0; pass < 2; pass++) {
            boolean h = pass == 1;
            for (CardDef d : Cards.ALL) {
                if (cap.has(d.id, h)) {
                    continue;
                }
                for (ItemStack s : sp.getInventory().items) {
                    if (!s.isEmpty() && s.getItem() == CartasItems.CARDS.get(d.id).get() && CardItem.isHolo(s) == h) {
                        s.shrink(1);
                        cap.add(d.id, h);
                        ids.add(d.id);
                        holo.add(h);
                        break;
                    }
                }
            }
        }
        if (!ids.isEmpty()) {
            sp.getInventory().setChanged();
            AlbumBonuses.apply(sp);
        }
        CartasNet.toPlayer(sp, new CartasNet.InsertResult(ids, holo, cap.normal, cap.holo, (float) before, (float) cap.bonusHealth()));
    }
}
