package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: tu medidor de frío (0 a 100). */
public class ColdPacket {
    public final int meter;

    public ColdPacket(int meter) {
        this.meter = meter;
    }

    public static void encode(ColdPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.meter);
    }

    public static ColdPacket decode(FriendlyByteBuf buf) {
        return new ColdPacket(buf.readVarInt());
    }

    public static void handle(ColdPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.cold(msg.meter)));
        context.setPacketHandled(true);
    }
}
