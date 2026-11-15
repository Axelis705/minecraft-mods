package com.casino.net;

import com.casino.plat.Net;
import com.casino.plat.Ser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Servidor -> jugador: subiste de nivel; se abre el cofre de premio con lo que te tocó (ya está en tu inventario). */
public final class LevelChestPacket {
    public final int level;
    public final List<ItemStack> items;

    public LevelChestPacket(int level, List<ItemStack> items) {
        this.level = level;
        this.items = items;
    }

    public static void encode(LevelChestPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.level);
        b.writeVarInt(m.items.size());
        for (ItemStack s : m.items) {
            Ser.writeItem(b, s);
        }
    }

    public static LevelChestPacket decode(FriendlyByteBuf b) {
        int level = b.readVarInt();
        int n = Math.min(32, b.readVarInt());
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(Ser.readItem(b));
        }
        return new LevelChestPacket(level, items);
    }

    public static void handle(LevelChestPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.ClientHooks.openLevelChest(m));
    }
}
