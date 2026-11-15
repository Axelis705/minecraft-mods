package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * Servidor -> jugador: abrir la plática con el anfitrión de la ruleta de la suerte. mode 0 = te pregunta si quieres girar,
 * 1 = ya giraste hoy (con el tiempo que falta), 2 = la ruleta está ocupada.
 */
public final class WheelDialogPacket {
    public final int entity;
    public final int mode;
    public final int hours;
    public final int minutes;
    public final String prize;

    public WheelDialogPacket(int entity, int mode, int hours, int minutes, String prize) {
        this.entity = entity;
        this.mode = mode;
        this.hours = hours;
        this.minutes = minutes;
        this.prize = prize;
    }

    public static void encode(WheelDialogPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.entity);
        b.writeVarInt(m.mode);
        b.writeVarInt(m.hours);
        b.writeVarInt(m.minutes);
        b.writeUtf(m.prize.length() > 120 ? m.prize.substring(0, 120) : m.prize, 128);
    }

    public static WheelDialogPacket decode(FriendlyByteBuf b) {
        return new WheelDialogPacket(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readUtf(128));
    }

    public static void handle(WheelDialogPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.ClientHooks.openWheelDialog(m));
    }
}
