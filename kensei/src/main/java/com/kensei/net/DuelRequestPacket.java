package com.kensei.net;

import com.kensei.duel.DuelManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Cliente -> servidor: el menú de duelo pidió empezar. {@code mode} = 1 (1v1) o 2 (2v2); {@code slots} = ids de entidad de cada puesto
 * (1v1: [rival]; 2v2: [aliado, rival1, rival2]), con -1 para "rival de práctica".
 */
public class DuelRequestPacket {
    public final int mode;
    public final int[] slots;

    public DuelRequestPacket(int mode, int[] slots) {
        this.mode = mode;
        this.slots = slots;
    }

    public static void encode(DuelRequestPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.mode);
        buf.writeByte(m.slots.length);
        for (int s : m.slots) {
            buf.writeVarInt(s + 1);
        }
    }

    public static DuelRequestPacket decode(FriendlyByteBuf buf) {
        int mode = buf.readByte();
        int n = Math.min(3, Math.max(0, buf.readByte()));
        int[] slots = new int[n];
        for (int i = 0; i < n; i++) {
            slots[i] = buf.readVarInt() - 1;
        }
        return new DuelRequestPacket(mode, slots);
    }

    public static void handle(DuelRequestPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sender = c.getSender();
        c.enqueueWork(() -> {
            if (sender != null) {
                DuelManager.request(sender, m.mode, m.slots);
            }
        });
        c.setPacketHandled(true);
    }
}
