package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Servidor -> jugadores cercanos: qué capa lleva puesta un jugador ("" = ninguna), para dibujarla en su espalda. */
public class CapeSyncPacket {
    public final UUID player;
    public final String cape;

    public CapeSyncPacket(UUID player, String cape) {
        this.player = player;
        this.cape = cape;
    }

    public static void encode(CapeSyncPacket m, FriendlyByteBuf buf) {
        buf.writeUUID(m.player);
        buf.writeUtf(m.cape, 64);
    }

    public static CapeSyncPacket decode(FriendlyByteBuf buf) {
        return new CapeSyncPacket(buf.readUUID(), buf.readUtf(64));
    }

    public static void handle(CapeSyncPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.cape(m)));
        c.setPacketHandled(true);
    }
}
