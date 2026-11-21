package com.mundosdeaxelis.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor (etapa 270): cómo terminó la forja (tier de la llave; 0 = salió sin empezar, 1 forjada, 2 perfecta, 3 fallida). */
public class ForjaResultPacket {
    public final BlockPos pos;
    public final int tier;
    public final int code;

    public ForjaResultPacket(BlockPos pos, int tier, int code) {
        this.pos = pos;
        this.tier = tier;
        this.code = code;
    }

    public static void encode(ForjaResultPacket m, FriendlyByteBuf buf) {
        buf.writeBlockPos(m.pos);
        buf.writeVarInt(m.tier);
        buf.writeVarInt(m.code);
    }

    public static ForjaResultPacket decode(FriendlyByteBuf buf) {
        return new ForjaResultPacket(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ForjaResultPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        ServerPlayer sp = c.getSender();
        c.enqueueWork(() -> {
            if (sp != null) {
                com.mundosdeaxelis.forja.Forja.result(sp, m.pos, m.tier, m.code);
            }
        });
        c.setPacketHandled(true);
    }
}
