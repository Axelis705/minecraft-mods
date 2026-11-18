package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: algo que acabas de obtener (objeto con su id exacto, o Esencia si el id va vacío). */
public class LootPacket {
    public final String itemId;
    public final int count;

    public LootPacket(String itemId, int count) {
        this.itemId = itemId == null ? "" : itemId;
        this.count = count;
    }

    public static void encode(LootPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.itemId, 120);
        buf.writeVarInt(msg.count);
    }

    public static LootPacket decode(FriendlyByteBuf buf) {
        return new LootPacket(buf.readUtf(120), buf.readVarInt());
    }

    public static void handle(LootPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.loot(msg.itemId, msg.count)));
        context.setPacketHandled(true);
    }
}
