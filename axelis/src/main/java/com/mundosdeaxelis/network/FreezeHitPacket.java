package com.mundosdeaxelis.network;

import com.mundosdeaxelis.world.Frozen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: una pulsación de salto estando atrapado en hielo. */
public class FreezeHitPacket {
    public FreezeHitPacket() {
    }

    public static void encode(FreezeHitPacket msg, FriendlyByteBuf buf) {
    }

    public static FreezeHitPacket decode(FriendlyByteBuf buf) {
        return new FreezeHitPacket();
    }

    public static void handle(FreezeHitPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        ServerPlayer sender = context.getSender();
        context.enqueueWork(() -> {
            if (sender != null) {
                Frozen.hit(sender, 1);
            }
        });
        context.setPacketHandled(true);
    }
}
