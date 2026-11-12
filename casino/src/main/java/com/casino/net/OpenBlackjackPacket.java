package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.server.level.ServerPlayer;
import com.casino.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;


/** Servidor -> jugador: abrir el panel de una mesa de blackjack (cuando le hace clic al crupier). */
public final class OpenBlackjackPacket {
    public final BlockPos pos;

    public OpenBlackjackPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenBlackjackPacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
    }

    public static OpenBlackjackPacket decode(FriendlyByteBuf b) {
        return new OpenBlackjackPacket(b.readBlockPos());
    }

    public static void handle(OpenBlackjackPacket m, ServerPlayer p) {
        Net.onClient(() -> ClientHooks.openBlackjack(m.pos));
    }
}
