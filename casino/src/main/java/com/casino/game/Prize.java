package com.casino.game;

import com.casino.plat.Ser;
import com.casino.plat.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/** Lo que gana un jugador: objetos y/o niveles de experiencia, con el texto para mostrarlo. */
public final class Prize {
    public final Component title;
    public final List<ItemStack> items;
    public final int xpLevels;
    public final int slice;
    public final boolean big;

    public Prize(Component title, List<ItemStack> items, int xpLevels, int slice, boolean big) {
        this.title = title;
        this.items = items;
        this.xpLevels = xpLevels;
        this.slice = slice;
        this.big = big;
    }

    public void deliver(ServerPlayer p) {
        for (ItemStack s : items) {
            ItemHandlerHelper.giveItemToPlayer(p, s.copy());
        }
        if (xpLevels > 0) {
            p.giveExperienceLevels(xpLevels);
        }
    }

    public CompoundTag save() {
        CompoundTag t = new CompoundTag();
        t.put("title", Ser.text(title));
        ListTag l = new ListTag();
        for (ItemStack s : items) {
            l.add(Ser.item(s));
        }
        t.put("items", l);
        t.putInt("xp", xpLevels);
        t.putInt("slice", slice);
        t.putBoolean("big", big);
        return t;
    }

    public static Prize load(CompoundTag t) {
        List<ItemStack> items = new ArrayList<>();
        ListTag l = Nbt.list(t, "items");
        for (int i = 0; i < l.size(); i++) {
            ItemStack s = Ser.item(Nbt.at(l, i));
            if (!s.isEmpty()) {
                items.add(s);
            }
        }
        Component title = Ser.text(t.get("title"));
        return new Prize(title, items, Nbt.i(t, "xp"), Nbt.i(t, "slice"), Nbt.b(t, "big"));
    }
}
