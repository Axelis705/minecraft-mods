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
    /** Juegos del saloon del Viejo Oeste. */
    public static final int BULL = 7;
    public static final int GALLERY = 8;
    public static final int DUEL = 9;
    public static final int GAMES = 10;
    /** Los juegos que cuentan para el logro "probar todos" (los del principio; los nuevos no lo quitan a quien ya lo tiene). */
    public static final int CLASSIC = 7;
    public static final int CLASSIC_MASK = (1 << CLASSIC) - 1;

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
        /** Tarjeta de socio: logros (un bit cada uno), juegos que ya probó, ruletas de la suerte, cócteles, último bono y nivel premiado. */
        public long achievements;
        public int gamesMask;
        public int wheelSpins;
        public int drinks;
        public long lastBonus = -1;
        public int levelGiven;
        public boolean cardGiven;

        public int level() {
            return Membership.level(wagered);
        }

        public boolean has(int ach) {
            return (achievements & (1L << ach)) != 0;
        }

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
        get(server).add(server, id, name, game, stake, stake > 0 ? 1 : 0, payout);
    }

    public static void play(ServerPlayer p, int game, long stake, long payout) {
        play(com.casino.plat.Plat.server(p), p.getUUID(), p.getGameProfile().getName(), game, stake, payout);
    }

    /** Compra de varias jugadas de una vez (rasca y gana: cada boleto cuenta como una apuesta). */
    public static void buy(ServerPlayer p, int game, long cost, int count) {
        MinecraftServer server = com.casino.plat.Plat.server(p);
        get(server).add(server, p.getUUID(), p.getGameProfile().getName(), game, cost, Math.max(1, count), 0);
    }

    private void add(MinecraftServer server, UUID id, String name, int game, long stake, long count, long payout) {
        Tournament.bet(server, id, name, game, stake, count, payout);
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
            if (count > 0) {
                e.gamesMask |= 1 << game;
            }
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
        checkAll(server, id, e, stake, payout);
    }

    // ------------------------------------------------------------------ socio y logros

    @javax.annotation.Nullable
    public Entry entry(UUID id) {
        return players.get(id);
    }

    public Entry entryOrNew(UUID id, String name) {
        Entry e = players.computeIfAbsent(id, k -> new Entry());
        if (name != null && !name.isEmpty() && "?".equals(e.name)) {
            e.name = name;
        }
        return e;
    }

    /** Revisa los logros que dependen de las cuentas y si subió de nivel (después de cada jugada). */
    private void checkAll(MinecraftServer server, UUID id, Entry e, long stake, long payout) {
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null && !e.cardGiven) {
            // la primera vez que apuesta le damos su tarjeta de socio
            e.cardGiven = true;
            net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, new net.minecraft.world.item.ItemStack(com.casino.registry.ModItems.MEMBER_CARD.get()));
            com.casino.plat.Plat.msg(p, net.minecraft.network.chat.Component.translatable("casino.member.welcome").withStyle(net.minecraft.ChatFormatting.GOLD), false);
            MemberRewards.title(server, p, e.level());
        }
        if (e.bets >= 1) {
            unlock(server, id, e, Membership.FIRST_BET);
        }
        if (stake >= 1000) {
            unlock(server, id, e, Membership.HIGH_ROLLER);
        }
        if (payout >= 1000) {
            unlock(server, id, e, Membership.BIG_WIN);
        }
        if (payout >= 10000) {
            unlock(server, id, e, Membership.JACKPOT);
        }
        if ((e.gamesMask & CLASSIC_MASK) == CLASSIC_MASK) {
            unlock(server, id, e, Membership.ALL_GAMES);
        }
        if (e.bets >= 100) {
            unlock(server, id, e, Membership.REGULAR);
        }
        if (e.bets >= 1000) {
            unlock(server, id, e, Membership.VETERAN);
        }
        if (e.net() >= 10000) {
            unlock(server, id, e, Membership.NET_10K);
        }
        if (p != null && com.casino.item.VipCardItem.tier(p) >= 2) {
            unlock(server, id, e, Membership.VIP_GOLD);
        }
        int lvl = e.level();
        if (lvl > e.levelGiven) {
            for (int l = e.levelGiven + 1; l <= lvl; l++) {
                levelUp(server, id, l);
            }
            e.levelGiven = lvl;
            setDirty();
        }
        if (lvl >= 4) {
            unlock(server, id, e, Membership.DIAMOND);
        }
        if (p != null) {
            com.casino.net.MemberSyncPacket.send(p);
        }
    }

    /** Vuelve a revisar logros y nivel con lo que ya tiene (para quien jugó antes de que existieran los logros). */
    public static void recheck(ServerPlayer p) {
        MinecraftServer server = com.casino.plat.Plat.server(p);
        CasinoStats s = get(server);
        Entry e = s.entry(p.getUUID());
        if (e != null && e.bets > 0) {
            s.checkAll(server, p.getUUID(), e, 0, e.biggest);
        }
    }

    /** Un logro especial (blackjack natural, escalera real...): lo llaman los juegos. */
    public static void achieve(ServerPlayer p, int ach) {
        MinecraftServer server = com.casino.plat.Plat.server(p);
        CasinoStats s = get(server);
        s.unlock(server, p.getUUID(), s.entryOrNew(p.getUUID(), p.getGameProfile().getName()), ach);
    }

    public static void achieve(MinecraftServer server, UUID id, int ach) {
        if (server == null || id == null) {
            return;
        }
        CasinoStats s = get(server);
        Entry e = s.players.get(id);
        if (e != null) {
            s.unlock(server, id, e, ach);
        }
    }

    /** Una ruleta de la suerte más (para el logro de 7). */
    public static void wheelSpun(MinecraftServer server, UUID id, String name) {
        CasinoStats s = get(server);
        Entry e = s.entryOrNew(id, name);
        e.wheelSpins++;
        s.setDirty();
        if (e.wheelSpins >= Membership.WHEEL_NEEDED) {
            s.unlock(server, id, e, Membership.WHEEL_FAN);
        }
    }

    /** Un cóctel más (para el logro de 10). */
    public static void drank(ServerPlayer p) {
        MinecraftServer server = com.casino.plat.Plat.server(p);
        CasinoStats s = get(server);
        Entry e = s.entryOrNew(p.getUUID(), p.getGameProfile().getName());
        e.drinks++;
        s.setDirty();
        if (e.drinks >= Membership.DRINKS_NEEDED) {
            s.unlock(server, p.getUUID(), e, Membership.COCKTAILS);
        }
    }

    private void unlock(MinecraftServer server, UUID id, Entry e, int ach) {
        if (e.has(ach)) {
            return;
        }
        e.achievements |= 1L << ach;
        setDirty();
        int chips = Membership.REWARD[ach];
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        net.minecraft.network.chat.Component name = net.minecraft.network.chat.Component.translatable("casino.ach." + ach);
        java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>(com.casino.chip.Chips.stacksFor(chips));
        com.casino.game.Prize prize = new com.casino.game.Prize(net.minecraft.network.chat.Component.translatable("casino.member.ach_prize", name), items, 0, -1, false);
        if (p != null) {
            prize.deliver(p);
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("casino.member.unlocked",
                    name.copy().withStyle(net.minecraft.ChatFormatting.YELLOW), chips).withStyle(net.minecraft.ChatFormatting.GOLD));
            //#if MC >= 1.21.2
            //$$ p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
            //#else
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 0.6F, 1.0F);
            //#endif
        } else {
            CasinoData.get(server).addPending(id, prize);
        }
    }

    /** Subió de nivel de socio: banner, fichas de regalo (y un bloque de diamante al llegar a Diamante). */
    private void levelUp(MinecraftServer server, UUID id, int level) {
        java.util.List<net.minecraft.world.item.ItemStack> items = MemberRewards.chest(level, server.overworld().getRandom());
        net.minecraft.network.chat.Component lv = net.minecraft.network.chat.Component.translatable("casino.member.level_" + Membership.LEVELS[level]);
        com.casino.game.Prize prize = new com.casino.game.Prize(net.minecraft.network.chat.Component.translatable("casino.member.level_up", lv), items, 0, -1, true);
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) {
            java.util.List<net.minecraft.world.item.ItemStack> shown = new java.util.ArrayList<>();
            for (net.minecraft.world.item.ItemStack st : items) {
                shown.add(st.copy());
            }
            prize.deliver(p);
            // el cofre animado (lo de adentro ya está en el inventario) y el título nuevo sobre la cabeza
            com.casino.net.CasinoNet.sendTo(p, new com.casino.net.LevelChestPacket(level, shown));
            MemberRewards.title(server, p, level);
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("casino.member.level_up_chat", lv.copy().withStyle(net.minecraft.ChatFormatting.AQUA))
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        } else {
            CasinoData.get(server).addPending(id, prize);
        }
    }

    /** Bono diario de socio: devuelve las fichas dadas (0 si ya lo cobró hoy). */
    public static int claimDaily(ServerPlayer p) {
        MinecraftServer server = com.casino.plat.Plat.server(p);
        CasinoStats s = get(server);
        Entry e = s.entryOrNew(p.getUUID(), p.getGameProfile().getName());
        long today = CasinoData.today();
        if (e.lastBonus >= today) {
            return 0;
        }
        e.lastBonus = today;
        int amount = Membership.DAILY[e.level()];
        com.casino.chip.Chips.give(p, amount);
        s.setDirty();
        com.casino.net.MemberSyncPacket.send(p);
        return amount;
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
                e.achievements = Nbt.l(t, "ach");
                e.gamesMask = Nbt.i(t, "gm");
                e.wheelSpins = Nbt.i(t, "ws");
                e.drinks = Nbt.i(t, "dr");
                e.lastBonus = Nbt.has(t, "lb") ? Nbt.l(t, "lb") : -1;
                e.levelGiven = Nbt.has(t, "lg") ? Nbt.i(t, "lg") : Membership.level(e.wagered);
                e.cardGiven = Nbt.b(t, "card");
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
            t.putLong("ach", e.achievements);
            t.putInt("gm", e.gamesMask);
            t.putInt("ws", e.wheelSpins);
            t.putInt("dr", e.drinks);
            t.putLong("lb", e.lastBonus);
            t.putInt("lg", e.levelGiven);
            t.putBoolean("card", e.cardGiven);
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
