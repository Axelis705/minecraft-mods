package com.casino.west;

import com.casino.chip.Chips;
import com.casino.data.CasinoStats;
import com.casino.entity.DealerAnim;
import com.casino.entity.DealerEntity;
import com.casino.game.LiarsDice;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.Arrays;
import java.util.UUID;

/**
 * Mesa de dados del mentiroso: mano a mano contra el tahúr. Guarda los dados, las apuestas y de quién es el turno. Los dados del tahúr
 * no se mandan a los clientes hasta que se destapan los cubiletes. FACING = hacia el jugador.
 */
public class DiceBE extends CasinoBE {
    public static final int IDLE = 0, PLAY = 1, REVEAL = 2;
    private static final int SHOW = 120;

    private int phase;
    private UUID player;
    private String playerName = "";
    private int bet;
    private int[] pd = new int[LiarsDice.DICE];
    private int[] nd = new int[LiarsDice.DICE];
    /** Apuestas: quién * 100 + cantidad * 10 + cara. */
    private int[] bids = new int[0];
    private int turn;
    private int winner = -1;
    private int caller = -1;
    private long npcAt;
    private long until;
    private int streak;
    private int bestStreak;
    private String bestName = "";

    public DiceBE(BlockPos pos, BlockState state) {
        super(WestReg.DICE_BE.get(), pos, state);
    }

    // ------------------------------------------------------------------ consultas

    public Direction facing() {
        BlockState s = getBlockState();
        return s.hasProperty(WestBlocks.FACING) ? s.getValue(WestBlocks.FACING) : Direction.SOUTH;
    }

    public int phase() {
        return phase;
    }

    public boolean isPlayer(UUID id) {
        return phase != IDLE && player != null && player.equals(id);
    }

    public String playerName() {
        return playerName;
    }

    public int bet() {
        return bet;
    }

    public int[] playerDice() {
        return pd;
    }

    /** Los dados del tahúr (solo se saben al destapar). */
    public int[] npcDice() {
        return nd;
    }

    public int[] bids() {
        return bids;
    }

    public int turn() {
        return turn;
    }

    public int winner() {
        return winner;
    }

    public int caller() {
        return caller;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public String bestName() {
        return bestName;
    }

    /** La partida tal como está (para revisar apuestas legales). */
    public LiarsDice game() {
        return game(0);
    }

    private LiarsDice game(long seed) {
        LiarsDice g = new LiarsDice(seed);
        g.dice[LiarsDice.PLAYER] = pd.clone();
        g.dice[LiarsDice.NPC] = nd.clone();
        for (int c : bids) {
            g.bids.add(new LiarsDice.Bid(c % 100 / 10, c % 10, c / 100));
        }
        g.turn = turn;
        return g;
    }

    // ------------------------------------------------------------------ jugar

    private DealerEntity npc() {
        if (level == null) {
            return null;
        }
        for (DealerEntity d : level.getEntitiesOfClass(DealerEntity.class, new AABB(worldPosition).inflate(3))) {
            if (d.getGame() == DealerEntity.GAME_DUELIST) {
                return d;
            }
        }
        return null;
    }

    private Component npcName() {
        DealerEntity d = npc();
        return Component.translatable("casino.npc.duelist_" + (d == null ? DealerEntity.WEST + 3 : d.getVariant()));
    }

    public void begin(ServerPlayer p, int betChips) {
        if (level == null || phase != IDLE || !LiarsDice.validBet(betChips)) {
            return;
        }
        if (!Chips.take(p, betChips)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", betChips).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        LiarsDice g = new LiarsDice(level.getRandom().nextLong());
        pd = g.dice[LiarsDice.PLAYER].clone();
        nd = g.dice[LiarsDice.NPC].clone();
        Arrays.sort(pd);
        Arrays.sort(nd);
        bids = new int[0];
        // empieza el que gana el volado: así a veces abre el tahúr
        turn = level.getRandom().nextBoolean() ? LiarsDice.PLAYER : LiarsDice.NPC;
        winner = -1;
        caller = -1;
        phase = PLAY;
        if (player == null || !player.equals(p.getUUID())) {
            streak = 0;
        }
        player = p.getUUID();
        playerName = p.getGameProfile().getName();
        bet = betChips;
        npcAt = level.getGameTime() + 40;
        level.playSound(null, worldPosition, ModSounds.DICE_SHAKE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        DealerEntity d = npc();
        if (d != null) {
            d.anim(DealerAnim.NOD);
        }
        Plat.msg(p, Component.translatable(turn == LiarsDice.PLAYER ? "casino.dice.you_start" : "casino.dice.npc_starts", npcName())
                .withStyle(ChatFormatting.GOLD), false);
        sync();
    }

    /** El jugador sube la apuesta. */
    public void playerBid(ServerPlayer p, int qty, int face) {
        if (phase != PLAY || !isPlayer(p.getUUID()) || turn != LiarsDice.PLAYER) {
            return;
        }
        LiarsDice g = game();
        if (!g.bid(LiarsDice.PLAYER, qty, face)) {
            return;
        }
        store(g);
        npcAt = level.getGameTime() + 25 + level.getRandom().nextInt(30);
        level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5F, 1.4F);
        sync();
    }

    /** El jugador grita "¡mentiroso!". */
    public void playerCall(ServerPlayer p) {
        if (phase != PLAY || !isPlayer(p.getUUID()) || turn != LiarsDice.PLAYER) {
            return;
        }
        LiarsDice g = game();
        if (g.call(LiarsDice.PLAYER)) {
            reveal(g, p);
        }
    }

    private void store(LiarsDice g) {
        bids = new int[g.bids.size()];
        for (int i = 0; i < bids.length; i++) {
            LiarsDice.Bid b = g.bids.get(i);
            bids[i] = b.who() * 100 + b.qty() * 10 + b.face();
        }
        turn = g.turn;
    }

    private void reveal(LiarsDice g, ServerPlayer p) {
        store(g);
        winner = g.winner;
        caller = g.caller;
        phase = REVEAL;
        until = level.getGameTime() + SHOW;
        level.playSound(null, worldPosition, ModSounds.DICE_SHAKE.get(), SoundSource.BLOCKS, 0.8F, 1.3F);
        DealerEntity d = npc();
        boolean won = winner == LiarsDice.PLAYER;
        if (d != null) {
            d.anim(won ? DealerAnim.SHAKE : DealerAnim.CLAP);
        }
        LiarsDice.Bid b = g.last();
        if (p != null) {
            long pay = won ? LiarsDice.payout(bet) : 0;
            if (pay > 0) {
                Chips.give(p, (int) pay);
                level.playSound(null, worldPosition, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.2F);
                streak++;
                if (WestRecords.offer(level, WestRecords.DICE, streak, playerName)) {
                    bestStreak = streak;
                    bestName = playerName;
                }
            } else {
                streak = 0;
            }
            CasinoStats.play(p, CasinoStats.DICE, bet, pay);
            Component head = Component.translatable("casino.dice.reveal", g.count(b.face()), Component.translatable("casino.dice.face_" + b.face()),
                    b.qty());
            Component tail = won ? Component.translatable("casino.dice.won", pay).withStyle(ChatFormatting.GREEN)
                    : Component.translatable("casino.dice.lost", bet, npcName()).withStyle(ChatFormatting.RED);
            Plat.msg(p, head.copy().withStyle(ChatFormatting.GOLD).append(" ").append(tail), false);
            Plat.msg(p, tail, true);
        }
        sync();
    }

    public void serverTick() {
        if (level != null && WestRecords.due(level, worldPosition)) {
            WestRecords.Rec r = WestRecords.pull(level, WestRecords.DICE, bestStreak, bestName);
            if (r.value() != bestStreak || !r.name().equals(bestName)) {
                bestStreak = r.value();
                bestName = r.name();
                sync();
            }
        }
        if (level == null || phase == IDLE) {
            return;
        }
        long now = level.getGameTime();
        if (phase == REVEAL) {
            if (now >= until) {
                phase = IDLE;
                sync();
            }
            return;
        }
        ServerPlayer p = level.getServer() == null || player == null ? null : level.getServer().getPlayerList().getPlayer(player);
        if (p == null || p.level() != level || p.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5) > 8 * 8) {
            // se fue de la mesa: pierde
            LiarsDice g = game();
            winner = LiarsDice.NPC;
            store(g);
            phase = REVEAL;
            until = now + 40;
            streak = 0;
            if (p != null) {
                CasinoStats.play(p, CasinoStats.DICE, bet, 0);
                Plat.msg(p, Component.translatable("casino.dice.left", bet).withStyle(ChatFormatting.RED), false);
            }
            sync();
            return;
        }
        if (turn == LiarsDice.NPC && now >= npcAt) {
            LiarsDice g = game(level.getRandom().nextLong());
            boolean called = g.think(LiarsDice.NPC, LiarsDice.NPC_CALL, LiarsDice.NPC_BLUFF);
            DealerEntity d = npc();
            if (called) {
                Plat.msg(p, Component.translatable("casino.dice.npc_calls", npcName()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
                if (d != null) {
                    d.anim(DealerAnim.STOP);
                }
                reveal(g, p);
            } else {
                LiarsDice.Bid b = g.last();
                store(g);
                Plat.msg(p, Component.translatable("casino.dice.npc_bids", npcName(), b.qty(), Component.translatable("casino.dice.face_" + b.face()))
                        .withStyle(ChatFormatting.YELLOW), true);
                if (d != null) {
                    d.anim(DealerAnim.NOD);
                }
                level.playSound(null, worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.5F, 1.1F);
                sync();
            }
        }
    }

    public void clientTick() {
        if (phase != IDLE && player != null) {
            com.casino.plat.Net.onClient(() -> com.casino.client.WestClient.diceSeen(this));
        }
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** A los clientes no se les mandan los dados del tahúr hasta destapar. */
    @Override
    protected CompoundTag syncTag(CompoundTag full) {
        if (phase != REVEAL) {
            full.putIntArray("NpcDice", new int[LiarsDice.DICE]);
        }
        return full;
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(1, 1, 1);
    }

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt("Phase", phase);
        if (player != null) {
            Nbt.putUuid(tag, "Player", player);
        }
        tag.putString("PlayerName", playerName);
        tag.putInt("Bet", bet);
        tag.putIntArray("PlayerDice", pd);
        tag.putIntArray("NpcDice", nd);
        tag.putIntArray("Bids", bids);
        tag.putInt("Turn", turn);
        tag.putInt("Winner", winner);
        tag.putInt("Caller", caller);
        tag.putLong("NpcAt", npcAt);
        tag.putLong("Until", until);
        tag.putInt("Streak", streak);
        tag.putInt("BestStreak", bestStreak);
        tag.putString("BestName", bestName);
    }

    @Override
    protected void readData(CompoundTag tag) {
        phase = Nbt.i(tag, "Phase");
        player = Nbt.hasUuid(tag, "Player") ? Nbt.uuid(tag, "Player") : null;
        playerName = Nbt.s(tag, "PlayerName");
        bet = Nbt.i(tag, "Bet");
        pd = fix(Nbt.ia(tag, "PlayerDice"));
        nd = fix(Nbt.ia(tag, "NpcDice"));
        bids = Nbt.ia(tag, "Bids");
        turn = Nbt.i(tag, "Turn");
        winner = Nbt.has(tag, "Winner") ? Nbt.i(tag, "Winner") : -1;
        caller = Nbt.has(tag, "Caller") ? Nbt.i(tag, "Caller") : -1;
        npcAt = Nbt.l(tag, "NpcAt");
        until = Nbt.l(tag, "Until");
        streak = Nbt.i(tag, "Streak");
        bestStreak = Nbt.i(tag, "BestStreak");
        bestName = Nbt.s(tag, "BestName");
    }

    private static int[] fix(int[] a) {
        return a.length == LiarsDice.DICE ? a : Arrays.copyOf(a, LiarsDice.DICE);
    }
}
