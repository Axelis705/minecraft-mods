package com.mundosdeaxelis.network;

import com.mundosdeaxelis.client.ClientBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> jugador: cuántas almas tiene (para el contador de la pantalla). */
public class SoulsPacket {
    public final int souls;
    public final int max;

    public SoulsPacket(int souls, int max) {
        this.souls = souls;
        this.max = max;
    }

    public static void encode(SoulsPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.souls);
        buf.writeVarInt(m.max);
    }

    public static SoulsPacket decode(FriendlyByteBuf buf) {
        return new SoulsPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(SoulsPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientBridge.souls(m)));
        c.setPacketHandled(true);
    }
}
