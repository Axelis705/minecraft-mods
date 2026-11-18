package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: corta la cinemática en curso. */
public class StopCinematicPacket {
    public StopCinematicPacket() {
    }

    public static void encode(StopCinematicPacket msg, FriendlyByteBuf buf) {
    }

    public static StopCinematicPacket decode(FriendlyByteBuf buf) {
        return new StopCinematicPacket();
    }

    public static void handle(StopCinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientBridge::stopCinematic));
        context.setPacketHandled(true);
    }
}
