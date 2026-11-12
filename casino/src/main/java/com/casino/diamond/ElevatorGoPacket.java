package com.casino.diamond;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Cliente -> servidor: ir a la parada de altura y desde el panel pos. El servidor revisa distancia y tarjeta. */
public final class ElevatorGoPacket {
    public final BlockPos pos;
    public final int y;

    public ElevatorGoPacket(BlockPos pos, int y) {
        this.pos = pos;
        this.y = y;
    }

    public static void encode(ElevatorGoPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.y);
    }

    public static ElevatorGoPacket decode(FriendlyByteBuf b) {
        return new ElevatorGoPacket(b.readBlockPos(), b.readVarInt());
    }

    public static void handle(ElevatorGoPacket m, ServerPlayer p) {
        if (p != null) {
            ElevatorBlock.travel(p, m.pos, m.y);
        }
    }
}
