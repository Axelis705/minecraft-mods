package com.casino.heist;

import com.casino.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Servidor -> jugador que está en el casino durante un robo: "¿ayudas a seguridad?" (aviso en pantalla con una tecla). */
public final class HeistOfferPacket {
    public final int robbers;

    public HeistOfferPacket(int robbers) {
        this.robbers = robbers;
    }

    public static void encode(HeistOfferPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.robbers);
    }

    public static HeistOfferPacket decode(FriendlyByteBuf b) {
        return new HeistOfferPacket(b.readVarInt());
    }

    public static void handle(HeistOfferPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.HeistHud.offer(m.robbers));
    }
}
