package com.mundosdeaxelis.network;

import com.mundosdeaxelis.cinematic.CinematicManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "quiero saltar la cinemática" (tecla Enter por defecto). */
public class SkipCinematicPacket {
    public SkipCinematicPacket() {
    }

    public static void encode(SkipCinematicPacket msg, FriendlyByteBuf buf) {
    }

    public static SkipCinematicPacket decode(FriendlyByteBuf buf) {
        return new SkipCinematicPacket();
    }

    public static void handle(SkipCinematicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                CinematicManager.skip(sender);
            }
        });
        context.setPacketHandled(true);
    }
}
