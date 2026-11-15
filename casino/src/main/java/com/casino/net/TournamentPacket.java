package com.casino.net;

import com.casino.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/** Servidor -> jugador: el torneo de esta noche (juego, cuánto falta, los tres primeros y tu lugar). */
public final class TournamentPacket {
    public final boolean active;
    public final int game;
    public final int ticksLeft;
    public final String[] top;
    public final long[] topScore;
    public final int rank;
    public final long mine;
    public final int players;

    public TournamentPacket(boolean active, int game, int ticksLeft, String[] top, long[] topScore, int rank, long mine, int players) {
        this.active = active;
        this.game = game;
        this.ticksLeft = ticksLeft;
        this.top = top;
        this.topScore = topScore;
        this.rank = rank;
        this.mine = mine;
        this.players = players;
    }

    public static void encode(TournamentPacket m, FriendlyByteBuf b) {
        b.writeBoolean(m.active);
        b.writeVarInt(m.game);
        b.writeVarInt(m.ticksLeft);
        for (int i = 0; i < 3; i++) {
            b.writeUtf(m.top[i] == null ? "" : m.top[i], 32);
            b.writeLong(m.topScore[i]);
        }
        b.writeVarInt(m.rank + 1);
        b.writeLong(m.mine);
        b.writeVarInt(m.players);
    }

    public static TournamentPacket decode(FriendlyByteBuf b) {
        boolean a = b.readBoolean();
        int g = b.readVarInt(), left = b.readVarInt();
        String[] top = new String[3];
        long[] sc = new long[3];
        for (int i = 0; i < 3; i++) {
            top[i] = b.readUtf(32);
            sc[i] = b.readLong();
        }
        return new TournamentPacket(a, g, left, top, sc, b.readVarInt() - 1, b.readLong(), b.readVarInt());
    }

    public static void handle(TournamentPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.MemberHud.tournament(m));
    }
}
