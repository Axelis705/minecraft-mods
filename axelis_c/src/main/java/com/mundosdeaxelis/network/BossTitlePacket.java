package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: título épico de jefe (letras que se juntan, líneas que se abren, subtítulo). */
public class BossTitlePacket {
    public final String title;
    public final String subtitle;
    public final int rgb;
    public final int ticks;

    public BossTitlePacket(String title, String subtitle, int rgb, int ticks) {
        this.title = title;
        this.subtitle = subtitle;
        this.rgb = rgb;
        this.ticks = ticks;
    }

    public static void encode(BossTitlePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title, 80);
        buf.writeUtf(msg.subtitle, 120);
        buf.writeInt(msg.rgb);
        buf.writeVarInt(msg.ticks);
    }

    public static BossTitlePacket decode(FriendlyByteBuf buf) {
        return new BossTitlePacket(buf.readUtf(80), buf.readUtf(120), buf.readInt(), buf.readVarInt());
    }

    public static void handle(BossTitlePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.bossTitle(msg.title, msg.subtitle, msg.rgb, msg.ticks)));
        context.setPacketHandled(true);
    }
}
