package com.casino.diamond;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Cliente -> servidor: apostar amount fichas al caballo horse (0 fichas = retirar la apuesta). */
public final class RaceBetPacket {
    public final BlockPos pos;
    public final int horse;
    public final int amount;

    public RaceBetPacket(BlockPos pos, int horse, int amount) {
        this.pos = pos;
        this.horse = horse;
        this.amount = amount;
    }

    public static void encode(RaceBetPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.horse);
        b.writeVarInt(m.amount);
    }

    public static RaceBetPacket decode(FriendlyByteBuf b) {
        return new RaceBetPacket(b.readBlockPos(), b.readVarInt(), b.readVarInt());
    }

    public static void handle(RaceBetPacket m, ServerPlayer p) {
        if (p != null && p.level().isLoaded(m.pos) && p.level().getBlockEntity(m.pos) instanceof RaceBE be) {
            be.placeBet(p, m.horse, m.amount);
        }
    }
}
