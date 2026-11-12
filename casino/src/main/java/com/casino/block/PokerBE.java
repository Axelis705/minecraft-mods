package com.casino.block;

import com.casino.plat.Plat;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.entity.DealerEntity;
import com.casino.game.BlackjackLayout;
import com.casino.game.PokerHands;
import com.casino.game.PokerTable;
import com.casino.game.PokerTable.Phase;
import com.casino.game.PokerTable.Result;
import com.casino.game.PokerTable.Seat;
import com.casino.game.PokerTable.SeatState;
import com.casino.game.Prize;
import com.casino.net.CasinoNet;
import com.casino.net.PokerPacket;
import com.casino.net.WheelWinPacket;
import com.casino.registry.ModEntities;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * El corazón de la mesa de póker (Texas Hold'em contra el crupier). La ronda completa vive aquí y se guarda con el mundo. El servidor decide
 * todo; el cliente solo pide jugadas y anima lo que ve. Las cartas que todavía no se pueden ver (las del crupier y las comunes que no
 * salieron) no se mandan a los clientes, así que no se pueden espiar.
 * Hasta 5 jugadores: apuestan (ante), se reparte, todos deciden a la vez (igualar o retirarse), salen las cartas comunes que faltan, el
 * crupier muestra y paga.
 */
public class PokerBE extends CasinoBE {
    private static final int BET_TICKS = 240;
    /** Tiempo para decidir después del reparto (25 s); quien no decide se retira. */
    private static final int DECIDE_TICKS = 500;
    /** Lo que tarda en animarse el reparto antes de poder decidir. */
    private static final int DEAL_ANIM = 70;
    private static final int STEP_TICKS = 28;
    private static final int RESULT_TICKS = 180;
    private static final double NEAR = 7.0;

    public final PokerTable table = new PokerTable();
    public final UUID[] players = new UUID[PokerTable.SEATS];
    public final String[] names = new String[PokerTable.SEATS];
    public int roundSeq = 0;
    public long phaseEnd = 0;
    public long settledAt = 0;
    /** Antes de este momento todavía se están repartiendo las cartas y no se puede decidir. */
    public long actFrom = 0;
    private long nextStep = 0;
    private long readyAt = 0;
    private boolean paid = false;
    private boolean dealNow = false;
    /** Cuántas cartas tiene el crupier (el cliente no recibe cuáles son mientras estén tapadas). */
    public int dealerCount = 0;
    private final Random rng = new Random();

    // ---- solo cliente: animación
    public final long[][] bornHole = new long[PokerTable.SEATS][2];
    public final long[] bornDealer = new long[2];
    public final long[] bornBoard = new long[5];
    public long revealAt = -1000;
    public long resultAt = -1000;
    private boolean clientInit = false;
    private int seenSeq;
    private int seenBoard;
    private boolean seenReveal;
    private Phase seenPhase = Phase.IDLE;
    private final boolean[] seenHole = new boolean[PokerTable.SEATS];

    public PokerBE(BlockPos pos, BlockState state) {
        super(com.casino.registry.ModBlockEntities.POKER.get(), pos, state);
        Arrays.fill(names, "");
    }

    public Direction facing() {
        return getBlockState().getValue(PokerBlock.FACING);
    }

    public Vec3 worldPos(double u, double w, double y) {
        Direction f = facing();
        Direction r = f.getCounterClockWise();
        return new Vec3(worldPosition.getX() + 0.5 + u * r.getStepX() + w * f.getStepX(), worldPosition.getY() + y,
                worldPosition.getZ() + 0.5 + u * r.getStepZ() + w * f.getStepZ());
    }

    public int seatOf(@Nullable UUID id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < players.length; i++) {
            if (id.equals(players[i])) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ crupier

    private List<DealerEntity> dealers() {
        if (level == null) {
            return List.of();
        }
        AABB box = new AABB(PokerBlock.dealerSpot(worldPosition, facing())).inflate(1.2, 1.0, 1.2);
        return level.getEntitiesOfClass(DealerEntity.class, box, d -> worldPosition.equals(d.getTable()));
    }

    public void spawnDealer(ServerLevel sl) {
        if (!dealers().isEmpty()) {
            return;
        }
        Direction f = facing();
        BlockPos spot = PokerBlock.dealerSpot(worldPosition, f);
        DealerEntity d = Plat.create(ModEntities.DEALER.get(), sl);
        if (d == null) {
            return;
        }
        d.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, f.toYRot(), 0.0F);
        d.setYHeadRot(f.toYRot());
        d.setYBodyRot(f.toYRot());
        d.setTable(worldPosition);
        d.setGame(DealerEntity.GAME_POKER);
        sl.addFreshEntity(d);
    }

    private void dealerSwing() {
        for (DealerEntity d : dealers()) {
            Plat.swing(d);
        }
    }

    private Vec3 center() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.0, worldPosition.getZ() + 0.5);
    }

    private void sound(SoundEvent s, float vol, float pitch) {
        if (level != null) {
            level.playSound(null, worldPosition, s, SoundSource.BLOCKS, vol, pitch);
        }
    }

    private void msg(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        Plat.msg(p, Component.translatable(key, args).withStyle(color), true);
    }

    @Nullable
    private ServerPlayer online(int seat) {
        return level instanceof ServerLevel sl && players[seat] != null ? sl.getServer().getPlayerList().getPlayer(players[seat]) : null;
    }

    // ------------------------------------------------------------------ jugadas (servidor)

    public void act(ServerPlayer p, int action, int betChips) {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        int seat = seatOf(p.getUUID());
        switch (action) {
            case PokerPacket.ANTE -> {
                if (table.phase == Phase.RESULT && now >= settledAt + 40) {
                    endRound();
                }
                if (table.phase != Phase.IDLE && table.phase != Phase.BETTING) {
                    msg(p, "casino.poker.round_running", ChatFormatting.YELLOW);
                    return;
                }
                if (seat >= 0 || !PokerTable.validAnte(betChips)) {
                    return;
                }
                boolean[] free = new boolean[PokerTable.SEATS];
                for (int i = 0; i < free.length; i++) {
                    free[i] = players[i] == null && table.seats[i].state == SeatState.EMPTY;
                }
                Direction f = facing();
                Direction r = f.getCounterClockWise();
                double dx = p.getX() - (worldPosition.getX() + 0.5);
                double dz = p.getZ() - (worldPosition.getZ() + 0.5);
                int s = BlackjackLayout.nearestSeat(dx * r.getStepX() + dz * r.getStepZ(), dx * f.getStepX() + dz * f.getStepZ(), free);
                if (s < 0) {
                    msg(p, "casino.poker.full", ChatFormatting.YELLOW);
                    return;
                }
                if (!Chips.take(p, betChips)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, betChips);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                boolean first = table.phase == Phase.IDLE;
                table.placeAnte(s, betChips);
                players[s] = p.getUUID();
                names[s] = p.getGameProfile().getName();
                if (first) {
                    phaseEnd = now + BET_TICKS;
                    readyAt = 0;
                    dealNow = false;
                    paid = false;
                }
                sound(ModSounds.CHIP.get(), 0.8F, 1.0F);
                sync();
            }
            case PokerPacket.START -> {
                if (seat >= 0 && table.phase == Phase.BETTING) {
                    dealNow = true;
                }
            }
            case PokerPacket.CALL -> {
                if (seat < 0 || !table.canCall(seat) || now < actFrom) {
                    return;
                }
                int cost = table.callCost(seat);
                if (!Chips.take(p, cost)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, cost);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                table.call(seat);
                sound(ModSounds.CHIP.get(), 0.9F, 0.9F);
                afterDecision(now);
            }
            case PokerPacket.FOLD -> {
                if (seat < 0 || !table.canCall(seat) || now < actFrom) {
                    return;
                }
                table.fold(seat);
                sound(ModSounds.CARD_FLIP.get(), 0.6F, 1.2F);
                afterDecision(now);
            }
            default -> {
            }
        }
    }

    private void afterDecision(long now) {
        if (table.phase == Phase.DECIDE && table.allDecided()) {
            endDecision(now);
        }
        sync();
    }

    /** Todos decidieron (o se acabó el tiempo): se sigue con el showdown, o se cierra si nadie igualó. */
    private void endDecision(long now) {
        table.foldUndecided();
        table.startShowdown();
        nextStep = now + STEP_TICKS;
        if (table.phase == Phase.RESULT && !paid) {
            settleAll(now);
        }
        sync();
    }

    private void endRound() {
        table.clearRound();
        Arrays.fill(players, null);
        Arrays.fill(names, "");
        dealerCount = 0;
        nextStep = 0;
        readyAt = 0;
        dealNow = false;
        sync();
    }

    private boolean everyoneNearHasBet() {
        if (!(level instanceof ServerLevel sl)) {
            return false;
        }
        Vec3 c = center();
        for (ServerPlayer o : sl.players()) {
            if (!o.isSpectator() && o.distanceToSqr(c.x, c.y, c.z) <= NEAR * NEAR && seatOf(o.getUUID()) < 0) {
                return false;
            }
        }
        return true;
    }

    private void startDeal(long now) {
        table.deal(rng);
        dealerCount = table.dealer.size();
        roundSeq++;
        dealNow = false;
        readyAt = 0;
        actFrom = now + DEAL_ANIM;
        phaseEnd = actFrom + DECIDE_TICKS;
        sound(ModSounds.CARD_DEAL.get(), 0.9F, 1.0F);
        dealerSwing();
        for (int i = 0; i < PokerTable.SEATS; i++) {
            ServerPlayer p = online(i);
            if (p != null && table.seats[i].state == SeatState.DECIDING) {
                msg(p, "casino.poker.decide_bar", ChatFormatting.GREEN);
            }
        }
        sync();
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % 100 == 7 && level instanceof ServerLevel sl) {
            spawnDealer(sl);
        }
        switch (table.phase) {
            case BETTING -> {
                if (now % 5 == 0 && readyAt == 0 && everyoneNearHasBet()) {
                    readyAt = now + 10;
                } else if (now % 5 == 0 && readyAt != 0 && !everyoneNearHasBet()) {
                    readyAt = 0;
                }
                if (dealNow || now >= phaseEnd || (readyAt != 0 && now >= readyAt)) {
                    startDeal(now);
                }
            }
            case DECIDE -> {
                if (now < actFrom) {
                    return;
                }
                Vec3 c = center();
                boolean changed = false;
                for (int i = 0; i < PokerTable.SEATS; i++) {
                    if (table.seats[i].state == SeatState.DECIDING) {
                        ServerPlayer sp = online(i);
                        if (sp == null || sp.distanceToSqr(c.x, c.y, c.z) > 24 * 24) {
                            table.fold(i); // se fue
                            changed = true;
                        }
                    }
                }
                if (now >= phaseEnd || table.allDecided()) {
                    endDecision(now);
                } else if (changed) {
                    sync();
                }
            }
            case SHOWDOWN -> {
                if (nextStep != 0 && now >= nextStep) {
                    int boardBefore = table.board.size();
                    boolean revealedBefore = table.dealerRevealed;
                    table.showdownStep();
                    nextStep = now + STEP_TICKS;
                    if (table.board.size() > boardBefore) {
                        sound(ModSounds.CARD_DEAL.get(), 0.8F, 0.95F);
                        dealerSwing();
                    } else if (!revealedBefore && table.dealerRevealed) {
                        sound(ModSounds.CARD_FLIP.get(), 0.9F, 1.0F);
                        nextStep = now + 45; // un momento para ver las cartas del crupier antes del resultado
                    }
                    if (table.phase == Phase.RESULT && !paid) {
                        settleAll(now);
                    }
                    sync();
                }
            }
            case RESULT -> {
                if (now >= settledAt + RESULT_TICKS) {
                    endRound();
                }
            }
            default -> {
            }
        }
    }

    /** La ronda terminó: se paga a cada jugador (al instante si está, o queda guardado hasta que vuelva). */
    private void settleAll(long now) {
        paid = true;
        settledAt = now;
        nextStep = 0;
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        boolean anyWin = false;
        boolean anyBig = false;
        for (int i = 0; i < PokerTable.SEATS; i++) {
            Seat s = table.seats[i];
            if (s.ante <= 0 || players[i] == null) {
                continue;
            }
            ServerPlayer p = online(i);
            int pay = s.payout;
            int spent = s.ante + s.call;
            int net = pay - spent;
            boolean bigHand = s.result == Result.WIN && s.handCat >= PokerHands.FULL_HOUSE;
            if (pay > 0) {
                List<ItemStack> items = new ArrayList<>(Chips.stacksFor(pay));
                boolean big = bigHand || net >= 250;
                Component title = bigHand ? Component.translatable("casino.poker.hand_" + s.handCat).withStyle(ChatFormatting.BOLD)
                        : Component.translatable("casino.blackjack.banner", pay);
                Prize prize = new Prize(title, items, 0, -1, big);
                if (p != null) {
                    prize.deliver(p);
                    if (bigHand || net >= 100) {
                        CasinoNet.sendTo(p, new WheelWinPacket(prize));
                    }
                } else {
                    CasinoData.get(sl.getServer()).addPending(players[i], prize);
                }
            }
            if (p != null) {
                switch (s.result) {
                    case WIN -> msg(p, "casino.poker.res_win", ChatFormatting.GREEN, net, Component.translatable("casino.poker.hand_" + s.handCat));
                    case WIN_NQ -> msg(p, "casino.poker.res_nq", ChatFormatting.GREEN, net);
                    case PUSH -> msg(p, "casino.poker.res_push", ChatFormatting.YELLOW, pay);
                    case FOLD -> msg(p, "casino.poker.res_fold", ChatFormatting.RED, s.ante);
                    default -> msg(p, "casino.poker.res_lose", ChatFormatting.RED, spent);
                }
            }
            anyWin |= net > 0;
            anyBig |= bigHand;
            if (net >= 500) {
                Component m = Component.translatable("casino.poker.won_big", Component.literal(names[i]).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf(pay)).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY);
                Vec3 c = center();
                for (ServerPlayer o : sl.players()) {
                    if (o.distanceToSqr(c.x, c.y, c.z) < 32 * 32) {
                        o.sendSystemMessage(m);
                    }
                }
            }
        }
        if (anyWin) {
            sound(anyBig ? ModSounds.WHEEL_WIN.get() : ModSounds.COINS.get(), 0.9F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ cliente

    public void clientTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (!clientInit) {
            clientInit = true;
            for (long[] b : bornHole) {
                Arrays.fill(b, -1000);
            }
            Arrays.fill(bornDealer, -1000);
            Arrays.fill(bornBoard, -1000);
            seenBoard = table.board.size();
            for (int i = 0; i < PokerTable.SEATS; i++) {
                seenHole[i] = !table.seats[i].hole.isEmpty();
            }
            seenReveal = table.dealerRevealed;
            seenSeq = roundSeq;
            seenPhase = table.phase;
            return;
        }
        Vec3 c = center();
        boolean fresh = false;
        if (roundSeq != seenSeq || (table.phase == Phase.IDLE && seenPhase != Phase.IDLE)) {
            fresh = roundSeq != seenSeq;
            seenSeq = roundSeq;
            seenBoard = 0;
            seenReveal = false;
            Arrays.fill(seenHole, false);
            revealAt = -1000;
            resultAt = -1000;
            for (long[] b : bornHole) {
                Arrays.fill(b, -1000);
            }
            Arrays.fill(bornDealer, -1000);
            Arrays.fill(bornBoard, -1000);
        }
        if (fresh) {
            // reparto: primero una carta a cada jugador y al crupier, luego la segunda; después el flop
            int participants = 0;
            for (Seat s : table.seats) {
                if (s.ante > 0) {
                    participants++;
                }
            }
            for (int round = 0; round < 2; round++) {
                int k = 0;
                for (int i = 0; i < PokerTable.SEATS; i++) {
                    if (table.seats[i].ante > 0) {
                        bornHole[i][round] = now + (round * (participants + 1) + k) * 4L;
                        k++;
                    }
                }
                bornDealer[round] = now + (round * (participants + 1) + k) * 4L;
            }
            long flopAt = now + (2 * (participants + 1)) * 4L + 6;
            for (int j = 0; j < Math.min(3, table.board.size()); j++) {
                bornBoard[j] = flopAt + j * 6L;
            }
            seenBoard = Math.min(3, table.board.size());
            for (int i = 0; i < PokerTable.SEATS; i++) {
                seenHole[i] = !table.seats[i].hole.isEmpty();
            }
        } else {
            // cartas que se agregan después (turn y river), o asientos que aparecen sin reparto visto
            for (int j = seenBoard; j < Math.min(5, table.board.size()); j++) {
                bornBoard[j] = now;
            }
            seenBoard = Math.max(seenBoard, Math.min(5, table.board.size()));
        }
        if (table.dealerRevealed && !seenReveal) {
            revealAt = now;
        }
        seenReveal = table.dealerRevealed;
        if (table.phase == Phase.RESULT && seenPhase != Phase.RESULT) {
            resultAt = now + (revealAt > now - 12 ? 14 : 4);
        }
        if (resultAt == now) {
            for (int i = 0; i < PokerTable.SEATS; i++) {
                Seat s = table.seats[i];
                if (s.result == Result.WIN || s.result == Result.WIN_NQ) {
                    double[] p = BlackjackLayout.polar(1.6, BlackjackLayout.seatAngle(i));
                    Vec3 w = worldPos(p[0], p[1], 0.95);
                    int n = s.handCat >= PokerHands.FULL_HOUSE ? 26 : 8;
                    for (int k = 0; k < n; k++) {
                        double a = level.getRandom().nextDouble() * Math.PI * 2;
                        double sp = 0.03 + level.getRandom().nextDouble() * 0.08;
                        level.addParticle(s.handCat >= PokerHands.FULL_HOUSE && k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER,
                                w.x, w.y, w.z, Math.cos(a) * sp, Math.abs(Math.sin(a)) * sp + 0.03, Math.sin(a) * sp);
                    }
                }
            }
        }
        seenPhase = table.phase;
        boolean card = false;
        for (int i = 0; i < PokerTable.SEATS && !card; i++) {
            card = bornHole[i][0] == now || bornHole[i][1] == now;
        }
        for (int j = 0; j < 5 && !card; j++) {
            card = bornBoard[j] == now;
        }
        card |= bornDealer[0] == now || bornDealer[1] == now;
        if (card) {
            level.playLocalSound(c.x, c.y, c.z, ModSounds.CARD_DEAL.get(), SoundSource.BLOCKS, 0.5F, 1.0F + (now % 3) * 0.05F, false);
        }
        if (revealAt == now) {
            level.playLocalSound(c.x, c.y, c.z, ModSounds.CARD_FLIP.get(), SoundSource.BLOCKS, 0.7F, 1.0F, false);
        }
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected AABB renderBox() {
        return box(worldPosition.offset(-3, 0, -3), worldPosition.offset(4, 3, 5));
    }

    /** Lo que ven los clientes: sin las cartas del crupier (hasta que las muestre) ni las comunes que faltan. */
    @Override
    protected CompoundTag syncTag(CompoundTag t) {
        if (!table.dealerRevealed) {
            t.remove("dealer");
        }
        t.remove("future");
        t.putInt("dealerN", table.dealer.size());
        return t;
    }



    private static int[] toArray(List<Integer> l) {
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void fill(List<Integer> l, int[] a) {
        l.clear();
        for (int v : a) {
            if (v >= 0 && v < 52 && l.size() < 7) {
                l.add(v);
            }
        }
    }

    @Override
    protected void writeData(CompoundTag tag) {
        ListTag seats = new ListTag();
        for (int i = 0; i < PokerTable.SEATS; i++) {
            Seat s = table.seats[i];
            CompoundTag t = new CompoundTag();
            t.putIntArray("hole", toArray(s.hole));
            t.putInt("ante", s.ante);
            t.putInt("call", s.call);
            t.putInt("state", s.state.ordinal());
            t.putInt("result", s.result.ordinal());
            t.putInt("payout", s.payout);
            t.putInt("handCat", s.handCat);
            if (players[i] != null) {
                Nbt.putUuid(t, "player", players[i]);
            }
            t.putString("name", names[i]);
            seats.add(t);
        }
        tag.put("seats", seats);
        tag.putIntArray("board", toArray(table.board));
        tag.putIntArray("dealer", toArray(table.dealer));
        tag.putIntArray("future", toArray(table.future));
        tag.putInt("phase", table.phase.ordinal());
        tag.putBoolean("revealed", table.dealerRevealed);
        tag.putBoolean("dealerQ", table.dealerQualified);
        tag.putInt("dealerCat", table.dealerCat);
        tag.putInt("roundSeq", roundSeq);
        tag.putLong("phaseEnd", phaseEnd);
        tag.putLong("nextStep", nextStep);
        tag.putLong("settledAt", settledAt);
        tag.putLong("actFrom", actFrom);
        tag.putBoolean("paid", paid);
    }

    @Override
    protected void readData(CompoundTag tag) {
        ListTag seats = Nbt.list(tag, "seats");
        Phase[] ph = Phase.values();
        SeatState[] ss = SeatState.values();
        Result[] rs = Result.values();
        for (int i = 0; i < PokerTable.SEATS; i++) {
            Seat s = table.seats[i];
            if (i < seats.size()) {
                CompoundTag t = Nbt.at(seats, i);
                fill(s.hole, Nbt.ia(t, "hole"));
                s.ante = Nbt.i(t, "ante");
                s.call = Nbt.i(t, "call");
                s.state = ss[Math.max(0, Math.min(ss.length - 1, Nbt.i(t, "state")))];
                s.result = rs[Math.max(0, Math.min(rs.length - 1, Nbt.i(t, "result")))];
                s.payout = Nbt.i(t, "payout");
                s.handCat = Nbt.i(t, "handCat");
                players[i] = Nbt.hasUuid(t, "player") ? Nbt.uuid(t, "player") : null;
                names[i] = Nbt.s(t, "name");
            } else {
                s.hole.clear();
                s.ante = 0;
                s.call = 0;
                s.state = SeatState.EMPTY;
                s.result = Result.NONE;
                s.payout = 0;
                s.handCat = -1;
                players[i] = null;
                names[i] = "";
            }
        }
        fill(table.board, Nbt.ia(tag, "board"));
        fill(table.dealer, Nbt.ia(tag, "dealer"));
        fill(table.future, Nbt.ia(tag, "future"));
        table.phase = ph[Math.max(0, Math.min(ph.length - 1, Nbt.i(tag, "phase")))];
        table.dealerRevealed = Nbt.b(tag, "revealed");
        table.dealerQualified = Nbt.b(tag, "dealerQ");
        table.dealerCat = tag.contains("dealerCat") ? Nbt.i(tag, "dealerCat") : -1;
        dealerCount = tag.contains("dealerN") ? Nbt.i(tag, "dealerN") : table.dealer.size();
        roundSeq = Nbt.i(tag, "roundSeq");
        phaseEnd = Nbt.l(tag, "phaseEnd");
        nextStep = Nbt.l(tag, "nextStep");
        settledAt = Nbt.l(tag, "settledAt");
        actFrom = Nbt.l(tag, "actFrom");
        paid = Nbt.b(tag, "paid");
    }
}
