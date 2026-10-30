package com.kensei.net;

import com.kensei.client.DuelMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: abre el menú de duelo (elegir 1v1 o 2v2 y contra quién). Lleva el jugador o rival al que se apuntaba, o -1. */
public class OpenDuelMenuPacket {
    public final int targetId;

    public OpenDuelMenuPacket(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(OpenDuelMenuPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.targetId + 1);
    }

    public static OpenDuelMenuPacket decode(FriendlyByteBuf buf) {
        return new OpenDuelMenuPacket(buf.readVarInt() - 1);
    }

    public static void handle(OpenDuelMenuPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Minecraft.getInstance().setScreen(new DuelMenuScreen(m.targetId))));
        c.setPacketHandled(true);
    }
}
