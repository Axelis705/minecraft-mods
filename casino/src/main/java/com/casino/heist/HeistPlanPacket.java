package com.casino.heist;

import com.casino.plat.Net;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/** Servidor -> jugador: el contacto le explica el robo. reason: 0 listo, 1 falta nivel Diamante, 2 vetado, 3 ya hay un robo, 4 la bóveda está cerrada hasta mañana, 5 no hay bóveda. */
public final class HeistPlanPacket {
    public final int reason;
    public final int level;
    public final List<String> crew;
    public final BlockPos npc;

    public HeistPlanPacket(int reason, int level, List<String> crew, BlockPos npc) {
        this.reason = reason;
        this.level = level;
        this.crew = crew;
        this.npc = npc;
    }

    public static void encode(HeistPlanPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.reason);
        b.writeVarInt(m.level);
        b.writeVarInt(Math.min(8, m.crew.size()));
        for (int i = 0; i < Math.min(8, m.crew.size()); i++) {
            b.writeUtf(m.crew.get(i), 32);
        }
        b.writeBlockPos(m.npc);
    }

    public static HeistPlanPacket decode(FriendlyByteBuf b) {
        int r = b.readVarInt(), l = b.readVarInt(), n = Math.min(8, b.readVarInt());
        List<String> c = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            c.add(b.readUtf(32));
        }
        return new HeistPlanPacket(r, l, c, b.readBlockPos());
    }

    public static void handle(HeistPlanPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.HeistClient.openPlan(m));
    }
}
