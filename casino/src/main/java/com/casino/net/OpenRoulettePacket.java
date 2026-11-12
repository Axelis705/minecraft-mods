package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.server.level.ServerPlayer;
import com.casino.client.ClientHooks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;


/** Servidor -> jugador: abrir el panel de una mesa de ruleta (cuando le hace clic al crupier). */
public final class OpenRoulettePacket {
    public final BlockPos pos;

    public OpenRoulettePacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(OpenRoulettePacket m, FriendlyByteBuf b) {
        b.writeBlockPos(m.pos);
    }

    public static OpenRoulettePacket decode(FriendlyByteBuf b) {
        return new OpenRoulettePacket(b.readBlockPos());
    }

    public static void handle(OpenRoulettePacket m, ServerPlayer p) {
        Net.onClient(() -> ClientHooks.openRoulette(m.pos));
    }
}
