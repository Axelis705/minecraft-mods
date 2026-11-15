package com.casino.data;

import com.casino.chip.Chips;
import com.casino.game.Prize;
import com.casino.net.CasinoNet;
import com.casino.plat.Plat;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Torneo de cada noche: al atardecer se anuncia, al anochecer empieza un torneo de un juego (cambia cada día) y dura hasta la
 * madrugada (6000 ticks, unos 5 minutos). Gana quien más fichas le saque a ese juego (ganado menos apostado). Premios: 5,000, 2,500 y
 * 1,000 fichas a los tres primeros y 150 a todos los que jueguen al menos 5 rondas. Lo mueven las máquinas y pantallas del casino
 * (si no hay ninguna cargada, no hay torneo: nadie está en un casino).
 */
public final class Tournament {
    public static final int ANNOUNCE = 11500;
    public static final int START = 13000;
    public static final int END = 19000;
    static final int[] GAMES = {CasinoStats.BLACKJACK, CasinoStats.ROULETTE, CasinoStats.SLOT, CasinoStats.POKER, CasinoStats.PLINKO, CasinoStats.SCRATCH};
    static final int[] PRIZES = {5000, 2500, 1000};
    static final int PARTICIPATE = 150;

    private static long lastTick = -1;
    private static long day = -1;
    private static int stage;
    private static int game;
    private static final Map<UUID, long[]> SCORES = new HashMap<>();
    private static final Map<UUID, String> NAMES = new HashMap<>();

    private Tournament() {
    }

    public static boolean running() {
        return stage == 2;
    }

    public static int game() {
        return game;
    }

    private static Component gameName(int g) {
        return Component.translatable("casino.records.game_" + g);
    }

    private static void all(MinecraftServer s, Component c) {
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            Plat.msg(p, c, false);
        }
    }

    /** Lo llaman los bloques del casino en cada tick (una vez por tick del servidor). */
    public static void tick(MinecraftServer s) {
        if (s == null) {
            return;
        }
        ServerLevel ow = s.overworld();
        long now = ow.getGameTime();
        if (now == lastTick) {
            return;
        }
        lastTick = now;
        long dt = Plat.dayTime(ow);
        long d = dt / 24000L;
        long t = dt % 24000L;
        if (d != day) {
            if (stage == 2) {
                finish(s);
            }
            day = d;
            stage = 0;
            SCORES.clear();
            NAMES.clear();
            game = GAMES[(int) Math.floorMod(d, GAMES.length)];
        }
        if (stage == 0 && t >= ANNOUNCE && t < START) {
            stage = 1;
            all(s, Component.translatable("casino.tournament.announce", gameName(game).copy().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GOLD));
        }
        if (stage <= 1 && t >= START && t < END) {
            stage = 2;
            all(s, Component.translatable("casino.tournament.start", gameName(game).copy().withStyle(ChatFormatting.AQUA), (END - START) / 1200)
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            for (ServerPlayer p : s.getPlayerList().getPlayers()) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8F, 1.2F);
            }
            sync(s, t);
        }
        if (stage == 2 && t >= END) {
            finish(s);
            stage = 3;
        }
        if (stage == 2 && now % 100 == 0) {
            sync(s, t);
        }
    }

    /** Cada jugada (lo llama CasinoStats). */
    static void bet(MinecraftServer s, UUID id, String name, int g, long stake, long count, long payout) {
        if (stage != 2 || g != game || id == null) {
            return;
        }
        long[] sc = SCORES.computeIfAbsent(id, k -> new long[2]);
        sc[0] += payout - stake;
        sc[1] += count;
        if (name != null) {
            NAMES.put(id, name);
        }
    }

    private static List<UUID> ranking() {
        List<UUID> ids = new ArrayList<>(SCORES.keySet());
        ids.sort((a, b) -> Long.compare(SCORES.get(b)[0], SCORES.get(a)[0]));
        return ids;
    }

    private static void sync(MinecraftServer s, long t) {
        List<UUID> r = ranking();
        String[] top = new String[3];
        long[] topScore = new long[3];
        for (int i = 0; i < 3; i++) {
            top[i] = i < r.size() ? NAMES.getOrDefault(r.get(i), "?") : "";
            topScore[i] = i < r.size() ? SCORES.get(r.get(i))[0] : 0;
        }
        int left = (int) Math.max(0, END - t);
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            int rank = r.indexOf(p.getUUID());
            long mine = rank >= 0 ? SCORES.get(p.getUUID())[0] : 0;
            CasinoNet.sendTo(p, new com.casino.net.TournamentPacket(stage == 2, game, left, top, topScore, rank, mine, r.size()));
        }
    }

    private static void finish(MinecraftServer s) {
        List<UUID> r = ranking();
        CasinoData data = CasinoData.get(s);
        all(s, Component.translatable("casino.tournament.end", gameName(game).copy().withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        int placed = 0;
        for (UUID id : r) {
            long[] sc = SCORES.get(id);
            if (placed < 3 && sc[0] > 0) {
                int amount = PRIZES[placed];
                List<ItemStack> items = new ArrayList<>(Chips.stacksFor(amount));
                Prize prize = new Prize(Component.translatable("casino.tournament.prize", placed + 1, gameName(game)), items, 0, -1, placed == 0);
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                all(s, Component.translatable("casino.tournament.place", placed + 1, NAMES.getOrDefault(id, "?"), String.format("%,d", sc[0]),
                        String.format("%,d", amount)).withStyle(placed == 0 ? ChatFormatting.YELLOW : ChatFormatting.WHITE));
                if (p != null) {
                    prize.deliver(p);
                    CasinoNet.sendTo(p, new com.casino.net.WheelWinPacket(prize));
                } else {
                    data.addPending(id, prize);
                }
                placed++;
            } else if (sc[1] >= 5) {
                ServerPlayer p = s.getPlayerList().getPlayer(id);
                if (p != null) {
                    Chips.give(p, PARTICIPATE);
                    Plat.msg(p, Component.translatable("casino.tournament.thanks", PARTICIPATE).withStyle(ChatFormatting.GRAY), false);
                }
            }
        }
        if (placed == 0) {
            all(s, Component.translatable("casino.tournament.nobody").withStyle(ChatFormatting.GRAY));
        }
        stage = 3;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            CasinoNet.sendTo(p, new com.casino.net.TournamentPacket(false, game, 0, new String[]{"", "", ""}, new long[3], -1, 0, 0));
        }
    }
}
