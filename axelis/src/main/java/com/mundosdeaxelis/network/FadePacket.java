package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: cubre la pantalla con un color (fundido) y la destapa después. */
public class FadePacket {
    private final int rgb;
    private final int fadeIn;
    private final int hold;
    private final int fadeOut;

    public FadePacket(int rgb, int fadeIn, int hold, int fadeOut) {
        this.rgb = rgb;
        this.fadeIn = fadeIn;
        this.hold = hold;
        this.fadeOut = fadeOut;
    }

    public static void encode(FadePacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.rgb);
        buf.writeVarInt(msg.fadeIn);
        buf.writeVarInt(msg.hold);
        buf.writeVarInt(msg.fadeOut);
    }

    public static FadePacket decode(FriendlyByteBuf buf) {
        return new FadePacket(buf.readInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(FadePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        final int rgb = msg.rgb;
        final int in = msg.fadeIn;
        final int hold = msg.hold;
        final int out = msg.fadeOut;
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.startFade(rgb, in, hold, out)));
        context.setPacketHandled(true);
    }
}
