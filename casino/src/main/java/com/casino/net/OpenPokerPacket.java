package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.server.level.ServerPlayer;
import com.casino.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;


/** Servidor -> jugador: abrir el panel de una mesa de póker (cuando le hace clic al crupier). */
public final class OpenPokerPacket {
    public final BlockPos pos;

    public OpenPokerPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenPokerPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
    }

    public static OpenPokerPacket decode(FriendlyByteBuf b) {
        return new OpenPokerPacket(b.readBlockPos());
    }

    public static void handle(OpenPokerPacket m, ServerPlayer p) {
        Net.onClient(() -> ClientHooks.openPoker(m.pos));
    }
}
