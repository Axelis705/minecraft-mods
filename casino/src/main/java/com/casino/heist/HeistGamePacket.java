package com.casino.heist;

import com.casino.plat.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Servidor -> jugador: abre un minijuego del robo (ganzúa, caja fuerte, hackeo de cámaras o saqueo de un carrito). */
public final class HeistGamePacket {
    public static final int LOCKPICK = 0;
    public static final int SAFE = 1;
    public static final int HACK = 2;
    public static final int LOOT = 3;

    public final int mode;
    public final BlockPos pos;
    public final int[] data;

    public HeistGamePacket(int mode, BlockPos pos, int[] data) {
        this.mode = mode;
        this.pos = pos;
        this.data = data;
    }

    public static void encode(HeistGamePacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.mode);
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.data.length);
        for (int v : m.data) {
            b.writeVarInt(v);
        }
    }

    public static HeistGamePacket decode(FriendlyByteBuf b) {
        int mode = b.readVarInt();
        BlockPos pos = b.readBlockPos();
        int n = Math.min(16, b.readVarInt());
        int[] d = new int[n];
        for (int i = 0; i < n; i++) {
            d[i] = b.readVarInt();
        }
        return new HeistGamePacket(mode, pos, d);
    }

    public static void handle(HeistGamePacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.HeistClient.openGame(m));
    }
}
