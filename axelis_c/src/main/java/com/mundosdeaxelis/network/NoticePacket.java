package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: aviso pequeño para el panel de arriba a la derecha (título, texto opcional, color y duración). */
public class NoticePacket {
    public final String title;
    public final String subtitle;
    public final int rgb;
    public final int ticks;

    public NoticePacket(String title, String subtitle, int rgb, int ticks) {
        this.title = title;
        this.subtitle = subtitle == null ? "" : subtitle;
        this.rgb = rgb;
        this.ticks = ticks;
    }

    public static void encode(NoticePacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.title, 200);
        buf.writeUtf(msg.subtitle, 200);
        buf.writeInt(msg.rgb);
        buf.writeVarInt(msg.ticks);
    }

    public static NoticePacket decode(FriendlyByteBuf buf) {
        return new NoticePacket(buf.readUtf(200), buf.readUtf(200), buf.readInt(), buf.readVarInt());
    }

    public static void handle(NoticePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.notice(msg.title, msg.subtitle, msg.rgb, msg.ticks)));
        context.setPacketHandled(true);
    }
}
