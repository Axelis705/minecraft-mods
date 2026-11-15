package com.casino.net;

import com.casino.entity.DealerEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Jugador -> servidor: "sí, quiero girar" al anfitrión de la ruleta de la suerte. */
public final class WheelSpinPacket {
    public final int entity;

    public WheelSpinPacket(int entity) {
        this.entity = entity;
    }

    public static void encode(WheelSpinPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.entity);
    }

    public static WheelSpinPacket decode(FriendlyByteBuf b) {
        return new WheelSpinPacket(b.readVarInt());
    }

    public static void handle(WheelSpinPacket m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        if (p.level().getEntity(m.entity) instanceof DealerEntity d && d.getGame() == DealerEntity.GAME_WHEEL) {
            d.hostAccept(p);
        }
    }
}
