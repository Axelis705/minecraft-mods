package com.casino.net;

import com.casino.data.CasinoStats;
import com.casino.plat.Net;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Servidor -> jugador: los récords del casino para las pantallas (los primeros de cada lista, los totales de la casa y los últimos
 * premios). Se manda solo a quien está cerca de una pantalla y solo cuando cambió algo.
 */
public final class LeaderboardPacket {
    /** Listas: ganancia neta, apostado, número de apuestas, pérdida, premio más grande; y de hoy: ganancia, apostado, premio. */
    public static final int NET = 0;
    public static final int WAGERED = 1;
    public static final int BETS = 2;
    public static final int LOST = 3;
    public static final int BIGGEST = 4;
    public static final int TODAY_NET = 5;
    public static final int TODAY_WAGERED = 6;
    public static final int TODAY_BIGGEST = 7;
    public static final int LISTS = 8;
    public static final int ROWS = 10;

    public final int rev;
    public final List<List<CasinoStats.Row>> lists;
    public final long[] gameBets;
    public final long[] gameWagered;
    public final long[] gamePaid;
    public final int players;
    public final long todayWagered;
    public final List<CasinoStats.Win> recent;

    public LeaderboardPacket(int rev, List<List<CasinoStats.Row>> lists, long[] gameBets, long[] gameWagered, long[] gamePaid, int players,
            long todayWagered, List<CasinoStats.Win> recent) {
        this.rev = rev;
        this.lists = lists;
        this.gameBets = gameBets;
        this.gameWagered = gameWagered;
        this.gamePaid = gamePaid;
        this.players = players;
        this.todayWagered = todayWagered;
        this.recent = recent;
    }

    public static LeaderboardPacket of(CasinoStats s) {
        List<List<CasinoStats.Row>> l = new ArrayList<>();
        l.add(s.top(ROWS, CasinoStats.Entry::net, false));
        l.add(s.top(ROWS, e -> e.wagered, false));
        l.add(s.top(ROWS, e -> e.bets, false));
        l.add(s.top(ROWS, e -> -e.net(), false));
        l.add(s.top(ROWS, e -> e.biggest, false));
        l.add(s.top(ROWS, CasinoStats.Entry::dNet, true));
        l.add(s.top(ROWS, e -> e.dWagered, true));
        l.add(s.top(ROWS, e -> e.dBiggest, true));
        return new LeaderboardPacket(s.rev(), l, s.gameBets().clone(), s.gameWagered().clone(), s.gamePaid().clone(), s.playerCount(),
                s.todayWagered(), new ArrayList<>(s.recent()));
    }

    // a quién ya se le mandó qué versión (un jugador nuevo, o que se reconectó, es otro objeto y se le manda de nuevo)
    private static final Map<ServerPlayer, Integer> SENT = new WeakHashMap<>();
    private static LeaderboardPacket cached;
    private static long cachedDay = -1;

    /** Le manda los récords si no tiene la versión de ahora (lo llaman las pantallas cada pocos segundos para los que están cerca). */
    public static void sendIfChanged(ServerPlayer p) {
        CasinoStats s = CasinoStats.get(com.casino.plat.Plat.server(p));
        long day = com.casino.data.CasinoData.today();
        if (cached == null || cached.rev != s.rev() || cachedDay != day) {
            cached = of(s);
            cachedDay = day;
            SENT.clear();
        }
        Integer had = SENT.get(p);
        if (had == null || had != cached.rev) {
            SENT.put(p, cached.rev);
            CasinoNet.sendTo(p, cached);
        }
    }

    public static void encode(LeaderboardPacket m, FriendlyByteBuf b) {
        b.writeVarInt(m.rev);
        b.writeVarInt(m.lists.size());
        for (List<CasinoStats.Row> l : m.lists) {
            b.writeVarInt(l.size());
            for (CasinoStats.Row r : l) {
                b.writeUtf(r.name(), 32);
                b.writeVarLong(r.value());
            }
        }
        b.writeVarInt(CasinoStats.GAMES);
        for (int i = 0; i < CasinoStats.GAMES; i++) {
            b.writeVarLong(m.gameBets[i]);
            b.writeVarLong(m.gameWagered[i]);
            b.writeVarLong(m.gamePaid[i]);
        }
        b.writeVarInt(m.players);
        b.writeVarLong(m.todayWagered);
        b.writeVarInt(m.recent.size());
        for (CasinoStats.Win w : m.recent) {
            b.writeUtf(w.name(), 32);
            b.writeVarLong(w.amount());
            b.writeVarInt(w.game());
        }
    }

    public static LeaderboardPacket decode(FriendlyByteBuf b) {
        int rev = b.readVarInt();
        int n = Math.min(16, b.readVarInt());
        List<List<CasinoStats.Row>> lists = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int k = Math.min(ROWS, b.readVarInt());
            List<CasinoStats.Row> l = new ArrayList<>();
            for (int j = 0; j < k; j++) {
                l.add(new CasinoStats.Row(b.readUtf(32), b.readVarLong()));
            }
            lists.add(l);
        }
        int g = Math.min(16, b.readVarInt());
        long[] gb = new long[CasinoStats.GAMES];
        long[] gw = new long[CasinoStats.GAMES];
        long[] gp = new long[CasinoStats.GAMES];
        for (int i = 0; i < g; i++) {
            long x = b.readVarLong();
            long y = b.readVarLong();
            long z = b.readVarLong();
            if (i < CasinoStats.GAMES) {
                gb[i] = x;
                gw[i] = y;
                gp[i] = z;
            }
        }
        int players = b.readVarInt();
        long today = b.readVarLong();
        int r = Math.min(32, b.readVarInt());
        List<CasinoStats.Win> recent = new ArrayList<>();
        for (int i = 0; i < r; i++) {
            recent.add(new CasinoStats.Win(b.readUtf(32), b.readVarLong(), b.readVarInt()));
        }
        while (lists.size() < LISTS) {
            lists.add(new ArrayList<>());
        }
        return new LeaderboardPacket(rev, lists, gb, gw, gp, players, today, recent);
    }

    public static void handle(LeaderboardPacket m, ServerPlayer p) {
        Net.onClient(() -> com.casino.client.Leaderboard.set(m));
    }
}
