package com.casino.net;

import com.casino.client.ClientHooks;
import com.casino.game.Prize;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

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
        b.writeComponent(m.title);
        b.writeVarInt(Math.min(8, m.items.size()));
        for (int i = 0; i < Math.min(8, m.items.size()); i++) {
            b.writeItem(m.items.get(i));
        }
        b.writeVarInt(m.xp);
        b.writeBoolean(m.big);
    }

    public static WheelWinPacket decode(FriendlyByteBuf b) {
        Component t = b.readComponent();
        int n = b.readVarInt();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            items.add(b.readItem());
        }
        return new WheelWinPacket(t, items, b.readVarInt(), b.readBoolean());
    }

    public static void handle(WheelWinPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.showWin(m)));
        ctx.get().setPacketHandled(true);
    }
}
