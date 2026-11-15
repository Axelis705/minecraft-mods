package com.casino.data;

import com.casino.plat.Nbt;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToLongFunction;

/**
 * Las estadísticas del casino que se guardan con el mundo: cuánto apostó cada jugador, cuántas veces, cuánto le pagaron, su premio
 * más grande (de siempre y de hoy) y los totales de la casa por juego. Con esto se arman las pantallas de récords.
 */
public final class CasinoStats extends SavedData {
    /** Juegos (para los totales de la casa y los anuncios de premios). */
    public static final int BLACKJACK = 0;
    public static final int ROULETTE = 1;
    public static final int POKER = 2;
    public static final int SLOT = 3;
    public static final int PLINKO = 4;
    public static final int SCRATCH = 5;
    public static final int RACE = 6;
    public static final int GAMES = 7;

    /** Un jugador. Los campos "d" son de hoy (se borran solos al cambiar de día). */
    public static final class Entry {
        public String name = "?";
        public long wagered;
        public long bets;
        public long won;
        public long biggest;
        public long day;
        public long dWagered;
        public long dBets;
        public long dWon;
        public long dBiggest;

        public long net() {
            return won - wagered;
        }

        public long dNet() {
            return dWon - dWagered;
        }

        void roll(long today) {
            if (day != today) {
                day = today;
                dWagered = 0;
                dBets = 0;
                dWon = 0;
                dBiggest = 0;
            }
        }
    }

    /** Un premio que se anuncia en la cinta de abajo de las pantallas. */
    public record Win(String name, long amount, int game) {
    }

    private final Map<UUID, Entry> players = new HashMap<>();
    private final long[] gameBets = new long[GAMES];
    private final long[] gameWagered = new long[GAMES];
    private final long[] gamePaid = new long[GAMES];
    private final List<Win> recent = new ArrayList<>();
    private static final int RECENT = 10;
    /** Premios a partir de este monto salen en la cinta. */
    public static final long TICKER_MIN = 50;
    /** Sube con cada cambio (no se guarda): las pantallas mandan otra vez los récords solo si cambió. */
    private int rev = 1;

    //#if MC >= 1.21.5
    //$$ private static final net.minecraft.world.level.saveddata.SavedDataType<CasinoStats> TYPE = new net.minecraft.world.level.saveddata.SavedDataType<>(
    //$$         com.casino.plat.Ids.casino("casino_stats"), CasinoStats::new,
    //$$         CompoundTag.CODEC.xmap(CasinoStats::load, d -> d.save(new CompoundTag())), null);
    //#endif

    public static CasinoStats get(MinecraftServer server) {
        //#if MC >= 1.21.5
        //$$ return server.overworld().getDataStorage().computeIfAbsent(TYPE);
        //#elif MC >= 1.20.5
        //$$ return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(CasinoStats::new, (t, reg) -> load(t), null),
        //$$         "casino_stats");
        //#else
        return server.overworld().getDataStorage().computeIfAbsent(CasinoStats::load, CasinoStats::new, "casino_stats");
        //#endif
    }

    public int rev() {
        return rev;
    }

    // ------------------------------------------------------------------ anotar

    /** Una jugada completa: lo que apostó y lo que le pagaron (0 si perdió). */
    public static void play(MinecraftServer server, UUID id, String name, int game, long stake, long payout) {
        if (server == null || id == null || (stake <= 0 && payout <= 0)) {
            return;
        }
        get(server).add(id, name, game, stake, stake > 0 ? 1 : 0, payout);
    }

    public static void play(ServerPlayer p, int game, long stake, long payout) {
        play(com.casino.plat.Plat.server(p), p.getUUID(), p.getGameProfile().getName(), game, stake, payout);
    }

    /** Compra de varias jugadas de una vez (rasca y gana: cada boleto cuenta como una apuesta). */
    public static void buy(ServerPlayer p, int game, long cost, int count) {
        get(com.casino.plat.Plat.server(p)).add(p.getUUID(), p.getGameProfile().getName(), game, cost, Math.max(1, count), 0);
    }

    private void add(UUID id, String name, int game, long stake, long count, long payout) {
        long today = CasinoData.today();
        Entry e = players.computeIfAbsent(id, k -> new Entry());
        if (name != null && !name.isEmpty()) {
            e.name = name;
        }
        e.roll(today);
        e.wagered += stake;
        e.bets += count;
        e.won += payout;
        e.biggest = Math.max(e.biggest, payout);
        e.dWagered += stake;
        e.dBets += count;
        e.dWon += payout;
        e.dBiggest = Math.max(e.dBiggest, payout);
        if (game >= 0 && game < GAMES) {
            gameBets[game] += count;
            gameWagered[game] += stake;
            gamePaid[game] += payout;
        }
        if (payout >= TICKER_MIN && payout > stake) {
            recent.add(0, new Win(e.name, payout, game));
            while (recent.size() > RECENT) {
                recent.remove(recent.size() - 1);
            }
        }
        rev++;
        setDirty();
    }

    // ------------------------------------------------------------------ leer

    /** Una fila de un récord: nombre y valor. */
    public record Row(String name, long value) {
    }

    /** Los primeros n según una medida (solo los que tienen algo en esa medida). */
    public List<Row> top(int n, ToLongFunction<Entry> f, boolean todayOnly) {
        long today = CasinoData.today();
        List<Row> out = new ArrayList<>();
        players.values().stream()
                .filter(e -> !todayOnly || e.day == today)
                .filter(e -> f.applyAsLong(e) > 0)
                .sorted(Comparator.comparingLong(f).reversed())
                .limit(n)
                .forEach(e -> out.add(new Row(e.name, f.applyAsLong(e))));
        return out;
    }

    public List<Win> recent() {
        return recent;
    }

    public long[] gameBets() {
        return gameBets;
    }

    public long[] gameWagered() {
        return gameWagered;
    }

    public long[] gamePaid() {
        return gamePaid;
    }

    public int playerCount() {
        return players.size();
    }

    public long todayWagered() {
        long today = CasinoData.today();
        long s = 0;
        for (Entry e : players.values()) {
            if (e.day == today) {
                s += e.dWagered;
            }
        }
        return s;
    }

    // ------------------------------------------------------------------ guardar

    public static CasinoStats load(CompoundTag tag) {
        CasinoStats d = new CasinoStats();
        CompoundTag pl = Nbt.c(tag, "players");
        for (String k : pl.getAllKeys()) {
            try {
                UUID id = UUID.fromString(k);
                CompoundTag t = Nbt.c(pl, k);
                Entry e = new Entry();
                e.name = Nbt.s(t, "n");
                e.wagered = Nbt.l(t, "w");
                e.bets = Nbt.l(t, "b");
                e.won = Nbt.l(t, "p");
                e.biggest = Nbt.l(t, "x");
                e.day = Nbt.l(t, "day");
                e.dWagered = Nbt.l(t, "dw");
                e.dBets = Nbt.l(t, "db");
                e.dWon = Nbt.l(t, "dp");
                e.dBiggest = Nbt.l(t, "dx");
                d.players.put(id, e);
            } catch (IllegalArgumentException ignored) {
                // clave dañada: se ignora
            }
        }
        CompoundTag g = Nbt.c(tag, "games");
        for (int i = 0; i < GAMES; i++) {
            d.gameBets[i] = Nbt.l(g, "b" + i);
            d.gameWagered[i] = Nbt.l(g, "w" + i);
            d.gamePaid[i] = Nbt.l(g, "p" + i);
        }
        ListTag rl = Nbt.list(tag, "recent");
        for (int i = 0; i < rl.size() && i < RECENT; i++) {
            CompoundTag t = Nbt.at(rl, i);
            d.recent.add(new Win(Nbt.s(t, "n"), Nbt.l(t, "a"), Nbt.i(t, "g")));
        }
        return d;
    }

    //#if MC >= 1.20.5 && MC < 1.21.5
    //$$ @Override
    //$$ public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider reg) {
    //$$     return save(tag);
    //$$ }
    //#endif

    //#if MC < 1.20.5
    @Override
    //#endif
    public CompoundTag save(CompoundTag tag) {
        CompoundTag pl = new CompoundTag();
        players.forEach((k, e) -> {
            CompoundTag t = new CompoundTag();
            t.putString("n", e.name);
            t.putLong("w", e.wagered);
            t.putLong("b", e.bets);
            t.putLong("p", e.won);
            t.putLong("x", e.biggest);
            t.putLong("day", e.day);
            t.putLong("dw", e.dWagered);
            t.putLong("db", e.dBets);
            t.putLong("dp", e.dWon);
            t.putLong("dx", e.dBiggest);
            pl.put(k.toString(), t);
        });
        tag.put("players", pl);
        CompoundTag g = new CompoundTag();
        for (int i = 0; i < GAMES; i++) {
            g.putLong("b" + i, gameBets[i]);
            g.putLong("w" + i, gameWagered[i]);
            g.putLong("p" + i, gamePaid[i]);
        }
        tag.put("games", g);
        ListTag rl = new ListTag();
        for (Win w : recent) {
            CompoundTag t = new CompoundTag();
            t.putString("n", w.name());
            t.putLong("a", w.amount());
            t.putInt("g", w.game());
            rl.add(t);
        }
        tag.put("recent", rl);
        return tag;
    }
}
