package com.casino.net;

import com.casino.plat.Ser;
import com.casino.plat.Net;
import net.minecraft.server.level.ServerPlayer;
import com.casino.client.ClientHooks;
import com.casino.game.Prize;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Servidor -> ganador: lo que ganó, para el cartel de premio en pantalla. */
public final class WheelWinPacket {
    public final Component title;
    public final List<ItemStack> items;
    public final int xp;
    public final boolean big;

    public WheelWinPacket(Prize p) {
        this(p.title, p.items, p.xpLevels, p.big);
    }

    public WheelWinPacket(Component title, List<ItemStack> items, int xp, boolean big) {
        this.title = title;
        this.items = items;
        this.xp = xp;
        this.big = big;
    }

    public static void encode(WheelWinPacket m, FriendlyByteBuf b) {
        Ser.writeText(b, m.title);
        b.writeVarInt(Math.min(8, m.items.size()));
        for (int i = 0; i < Math.min(8, m.items.size()); i++) {
            Ser.writeItem(b, m.items.get(i));
        }
        b.writeVarInt(m.xp);
        b.writeBoolean(m.big);
    }

    public static WheelWinPacket decode(FriendlyByteBuf b) {
        Component t = Ser.readText(b);
        int n = b.readVarInt();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(Ser.readItem(b));
        }
        return new WheelWinPacket(t, items, b.readVarInt(), b.readBoolean());
    }

    public static void handle(WheelWinPacket m, ServerPlayer p) {
        Net.onClient(() -> ClientHooks.showWin(m));
    }
}
