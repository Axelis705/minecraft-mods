package com.casino.heist;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Jugador -> servidor: lo que hizo (empezar el robo, resultado de un minijuego, cerrar sin terminar, ayudar a seguridad). */
public final class HeistActionPacket {
    public static final int START = 0;
    public static final int LOCKPICK = 1;
    public static final int SAFE = 2;
    public static final int HACK = 3;
    public static final int LOOT = 4;
    public static final int CANCEL = 5;
    public static final int HELP = 6;

    public final int action;
    public final BlockPos pos;
    public final int[] values;

    public HeistActionPacket(int action, BlockPos pos, int... values) {
        this.action = action;
        this.pos = pos;
        this.values = values;
    }

    public static void encode(HeistActionPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.action);
        b.writeBlockPos(m.pos);
        b.writeVarInt(m.values.length);
        for (int v : m.values) {
            b.writeVarInt(v);
        }
    }

    public static HeistActionPacket decode(FriendlyByteBuf b) {
        int a = b.readVarInt();
        BlockPos pos = b.readBlockPos();
        int n = Math.min(8, b.readVarInt());
        int[] v = new int[n];
        for (int i = 0; i < n; i++) {
            v[i] = b.readVarInt();
        }
        return new HeistActionPacket(a, pos, v);
    }

    public static void handle(HeistActionPacket m, ServerPlayer p) {
        if (p == null) {
            return;
        }
        if (m.action == CANCEL) {
            HeistManager.cancel(p);
        } else {
            HeistManager.action(p, m.action, m.pos, m.values);
        }
    }
}
