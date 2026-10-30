package com.kensei.net;

import com.kensei.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: se apretó la tecla de duelo. Lleva el jugador al que se apunta (o está fijado), o -1. Sirve para desafiar y para aceptar. */
public class DuelKeyPacket {
    public final int targetId;

    public DuelKeyPacket(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(DuelKeyPacket m, FriendlyByteBuf buf) {
        buf.writeVarInt(m.targetId + 1);
    }

    public static DuelKeyPacket decode(FriendlyByteBuf buf) {
        return new DuelKeyPacket(buf.readVarInt() - 1);
    }

    public static void handle(DuelKeyPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sender = c.getSender();
        c.enqueueWork(() -> {
            if (sender != null) {
                DuelManager.onKey(sender, m.targetId);
            }
        });
        c.setPacketHandled(true);
    }
}
