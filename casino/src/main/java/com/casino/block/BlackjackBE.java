package com.casino.block;

import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.entity.DealerEntity;
import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackGame.Result;
import com.casino.game.BlackjackLayout;
import com.casino.game.BlackjackTable;
import com.casino.game.BlackjackTable.Phase;
import com.casino.game.BlackjackTable.Seat;
import com.casino.game.BlackjackTable.SeatState;
import com.casino.game.Prize;
import com.casino.net.BlackjackPacket;
import com.casino.net.CasinoNet;
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
 * El corazón de la mesa de blackjack (bloque del centro). La ronda completa vive aquí y se guarda con el mundo, así que si se cierra el
 * servidor a mitad de una mano no se pierde nada. El servidor decide todo; el cliente solo pide jugadas y anima lo que ve.
 * Hasta 5 jugadores a la vez: cada uno apuesta (se sienta en el asiento más cercano a donde está parado), se reparte, juegan por turnos,
 * el crupier juega su mano y paga a cada uno.
 */
public class BlackjackBE extends BlockEntity {
    /** Tiempo para apostar desde la primera apuesta (10 s). Si todos los que están cerca ya apostaron, se reparte antes. */
    private static final int BET_TICKS = 200;
    /** Tiempo para jugar cada turno (20 s); si se pasa, el crupier planta al jugador. */
    private static final int TURN_TICKS = 400;
    /** Cuánto se muestran los resultados antes de limpiar la mesa (6 s). */
    private static final int RESULT_TICKS = 120;
    /** Distancia a la que se considera que alguien "está en la mesa" para repartir sin esperar. */
    private static final double NEAR = 7.0;
    public static final int MAX_CARDS = 12;

    public final BlackjackTable table = new BlackjackTable();
    public final UUID[] players = new UUID[BlackjackTable.SEATS];
    public final String[] names = new String[BlackjackTable.SEATS];
    public int roundSeq = 0;
    /** Fin del tiempo de apuestas o del turno actual (reloj del juego). */
    public long phaseEnd = 0;
    private long nextStep = 0;
    public long settledAt = 0;
    private long readyAt = 0;
    private boolean paid = false;
    private boolean dealNow = false;
    private int lastTurnSeat = -1;
    private final Random rng = new Random();

    // ---- solo cliente: animación de las cartas
    public final long[][] born = new long[BlackjackTable.SEATS][MAX_CARDS];
    public final long[] dBorn = new long[MAX_CARDS];
    public long revealAt = -1000;
    public long resultAt = -1000;
    private boolean clientInit = false;
    private final int[] seenCards = new int[BlackjackTable.SEATS];
    private int seenD;
    private boolean seenHole;
    private int seenSeq;
    private Phase seenPhase = Phase.IDLE;

    public BlackjackBE(BlockPos pos, BlockState state) {
        super(com.casino.registry.ModBlockEntities.BLACKJACK.get(), pos, state);
        Arrays.fill(names, "");
    }

    public Direction facing() {
        return getBlockState().getValue(BlackjackBlock.FACING);
    }

    /** De coordenadas de la mesa (x a la derecha del jugador, z hacia los jugadores) a coordenadas del mundo. */
    public Vec3 worldPos(double u, double w, double y) {
        Direction f = facing();
        Direction r = f.getCounterClockWise();
        return new Vec3(worldPosition.getX() + 0.5 + u * r.getStepX() + w * f.getStepX(), worldPosition.getY() + y,
                worldPosition.getZ() + 0.5 + u * r.getStepZ() + w * f.getStepZ());
    }

    /** Asiento de este jugador, o -1. */
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
        AABB box = new AABB(BlackjackBlock.dealerSpot(worldPosition, facing())).inflate(1.2, 1.0, 1.2);
        return level.getEntitiesOfClass(DealerEntity.class, box, d -> worldPosition.equals(d.getTable()));
    }

    public void spawnDealer(ServerLevel sl) {
        if (!dealers().isEmpty()) {
            return;
        }
        Direction f = facing();
        BlockPos spot = BlackjackBlock.dealerSpot(worldPosition, f);
        DealerEntity d = ModEntities.DEALER.get().create(sl);
        if (d == null) {
            return;
        }
        d.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, f.toYRot(), 0.0F);
        d.setYHeadRot(f.toYRot());
        d.setYBodyRot(f.toYRot());
        d.setTable(worldPosition);
        sl.addFreshEntity(d);
    }

    private void dealerSwing() {
        for (DealerEntity d : dealers()) {
            d.swing(InteractionHand.MAIN_HAND);
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
        p.displayClientMessage(Component.translatable(key, args).withStyle(color), true);
    }

    @Nullable
    private ServerPlayer online(int seat) {
        return level instanceof ServerLevel sl && players[seat] != null ? sl.getServer().getPlayerList().getPlayer(players[seat]) : null;
    }

    // ------------------------------------------------------------------ jugadas (servidor)

    public void act(ServerPlayer p, int action, int betChips) {
        if (level == null || level.isClientSide) {
            return;
        }
        long now = level.getGameTime();
        int seat = seatOf(p.getUUID());
        switch (action) {
            case BlackjackPacket.DEAL -> {
                if (table.phase == Phase.RESULT && now >= settledAt + 40) {
                    endRound(); // los resultados ya se vieron: se limpia y se puede apostar de nuevo
                }
                if (table.phase != Phase.IDLE && table.phase != Phase.BETTING) {
                    msg(p, "casino.blackjack.round_running", ChatFormatting.YELLOW);
                    return;
                }
                if (seat >= 0 || !BlackjackGame.validBet(betChips)) {
                    return;
                }
                boolean[] free = new boolean[BlackjackTable.SEATS];
                for (int i = 0; i < free.length; i++) {
                    free[i] = players[i] == null && table.seats[i].state == SeatState.EMPTY;
                }
                Direction f = facing();
                Direction r = f.getCounterClockWise();
                double dx = p.getX() - (worldPosition.getX() + 0.5);
                double dz = p.getZ() - (worldPosition.getZ() + 0.5);
                int s = BlackjackLayout.nearestSeat(dx * r.getStepX() + dz * r.getStepZ(), dx * f.getStepX() + dz * f.getStepZ(), free);
                if (s < 0) {
                    msg(p, "casino.blackjack.full", ChatFormatting.YELLOW);
                    return;
                }
                if (!Chips.take(p, betChips)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, betChips);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                boolean first = table.phase == Phase.IDLE;
                table.placeBet(s, betChips);
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
            case BlackjackPacket.START -> {
                if (seat >= 0 && table.phase == Phase.BETTING) {
                    dealNow = true;
                }
            }
            case BlackjackPacket.HIT -> {
                if (seat < 0 || table.phase != Phase.TURNS || table.current != seat) {
                    return;
                }
                table.hit(seat, rng);
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 1.1F);
                dealerSwing();
                afterChange(now);
            }
            case BlackjackPacket.STAND -> {
                if (seat < 0 || table.phase != Phase.TURNS || table.current != seat) {
                    return;
                }
                table.stand(seat);
                sound(ModSounds.CHIP.get(), 0.4F, 1.4F);
                afterChange(now);
            }
            case BlackjackPacket.DOUBLE -> {
                if (seat < 0 || !table.canDouble(seat)) {
                    return;
                }
                if (!Chips.take(p, table.seats[seat].bet)) {
                    msg(p, "casino.plinko.no_chips", ChatFormatting.RED, table.seats[seat].bet);
                    sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
                    return;
                }
                table.doubleDown(seat, rng);
                sound(ModSounds.CHIP.get(), 0.8F, 0.9F);
                sound(ModSounds.CARD_DEAL.get(), 0.8F, 1.0F);
                dealerSwing();
                afterChange(now);
            }
            default -> {
            }
        }
    }

    private void endRound() {
        table.clearRound();
        Arrays.fill(players, null);
        Arrays.fill(names, "");
        lastTurnSeat = -1;
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
        roundSeq++;
        nextStep = 0;
        dealNow = false;
        readyAt = 0;
        lastTurnSeat = -1;
        sound(ModSounds.CARD_DEAL.get(), 0.9F, 1.0F);
        dealerSwing();
        afterChange(now + 50); // el reparto inicial tarda en animarse: los tiempos arrancan un poco después
    }

    /** Después de cada cambio: programa el turno del crupier, avisa a quien le toca y paga cuando la ronda termina. */
    private void afterChange(long now) {
        if (table.phase == Phase.TURNS && table.current != lastTurnSeat) {
            lastTurnSeat = table.current;
            phaseEnd = now + TURN_TICKS;
            ServerPlayer p = online(table.current);
            if (p != null) {
                msg(p, "casino.blackjack.your_turn_bar", ChatFormatting.GREEN);
                level.playSound(null, p.blockPosition(), ModSounds.CHIP.get(), SoundSource.BLOCKS, 0.6F, 1.5F);
            }
        }
        if (table.phase == Phase.DEALER && nextStep == 0) {
            nextStep = now + 14;
        }
        if (table.phase == Phase.RESULT && !paid) {
            settleAll(now);
        }
        sync();
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % 100 == 7 && level instanceof ServerLevel sl) {
            spawnDealer(sl); // si al crupier le pasó algo, vuelve a aparecer
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
            case TURNS -> {
                int s = table.current;
                ServerPlayer sp = s >= 0 ? online(s) : null;
                Vec3 c = center();
                if (s >= 0 && (sp == null || sp.distanceToSqr(c.x, c.y, c.z) > 24 * 24 || now >= phaseEnd)) {
                    table.stand(s); // el jugador se fue o se tardó: el crupier lo planta
                    afterChange(now);
                }
            }
            case DEALER -> {
                if (nextStep != 0 && now >= nextStep) {
                    int before = table.dealer.size();
                    boolean wasHole = table.holeRevealed;
                    table.dealerStep(rng);
                    nextStep = now + 16;
                    if (table.dealer.size() > before) {
                        sound(ModSounds.CARD_DEAL.get(), 0.8F, 0.95F);
                        dealerSwing();
                    } else if (!wasHole) {
                        sound(ModSounds.CARD_FLIP.get(), 0.9F, 1.0F);
                    }
                    afterChange(now);
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
        boolean anyBJ = false;
        for (int i = 0; i < BlackjackTable.SEATS; i++) {
            Seat s = table.seats[i];
            if (s.bet <= 0 || players[i] == null) {
                continue;
            }
            ServerPlayer p = online(i);
            Result r = s.result;
            int pay = s.payout;
            int net = pay - s.bet;
            if (pay > 0) {
                List<ItemStack> items = new ArrayList<>(Chips.stacksFor(pay));
                boolean big = r == Result.BLACKJACK || net >= 250;
                Component title = r == Result.BLACKJACK ? Component.translatable("casino.blackjack.banner_bj") : Component.translatable("casino.blackjack.banner", pay);
                Prize prize = new Prize(title, items, 0, -1, big);
                if (p != null) {
                    prize.deliver(p);
                    if (r == Result.BLACKJACK || net >= 100) {
                        CasinoNet.sendTo(p, new WheelWinPacket(prize));
                    }
                } else {
                    CasinoData.get(sl.getServer()).addPending(players[i], prize);
                }
            }
            if (p != null) {
                switch (r) {
                    case BLACKJACK -> msg(p, "casino.blackjack.res_bj", ChatFormatting.GOLD, pay);
                    case WIN -> msg(p, "casino.blackjack.res_win", ChatFormatting.GREEN, pay);
                    case PUSH -> msg(p, "casino.blackjack.res_push", ChatFormatting.YELLOW, pay);
                    case BUST -> msg(p, "casino.blackjack.res_bust", ChatFormatting.RED, s.bet);
                    default -> msg(p, "casino.blackjack.res_lose", ChatFormatting.RED, s.bet);
                }
            }
            anyWin |= pay > s.bet;
            anyBJ |= r == Result.BLACKJACK;
            if (net >= 500) {
                Component m = Component.translatable("casino.blackjack.won_big", Component.literal(names[i]).withStyle(ChatFormatting.YELLOW),
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
            sound(anyBJ ? ModSounds.WHEEL_WIN.get() : ModSounds.COINS.get(), 0.9F, 1.0F);
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
            for (int i = 0; i < BlackjackTable.SEATS; i++) {
                seenCards[i] = table.seats[i].cards.size();
                Arrays.fill(born[i], -1000);
            }
            seenD = table.dealer.size();
            Arrays.fill(dBorn, -1000);
            seenHole = table.holeRevealed;
            seenSeq = roundSeq;
            seenPhase = table.phase;
            return;
        }
        Vec3 c = center();
        boolean fresh = false;
        if (roundSeq != seenSeq || (table.phase == Phase.IDLE && seenPhase != Phase.IDLE)) {
            if (roundSeq != seenSeq) {
                fresh = true;
            }
            seenSeq = roundSeq;
            Arrays.fill(seenCards, 0);
            seenD = 0;
            seenHole = false;
            revealAt = -1000;
            resultAt = -1000;
            for (long[] b : born) {
                Arrays.fill(b, -1000);
            }
            Arrays.fill(dBorn, -1000);
        }
        // las cartas del reparto inicial salen una por una: primero una a cada asiento y al crupier, luego la segunda
        int participants = 0;
        for (Seat s : table.seats) {
            if (s.bet > 0) {
                participants++;
            }
        }
        int order = 0;
        for (int round = 0; round < 2; round++) {
            int k = 0;
            for (int i = 0; i < BlackjackTable.SEATS; i++) {
                Seat s = table.seats[i];
                if (s.bet > 0) {
                    if (round < s.cards.size() && round >= seenCards[i] && round < MAX_CARDS) {
                        born[i][round] = now + (fresh ? (round * (participants + 1) + k) * 4L : 0);
                    }
                    k++;
                }
            }
            if (round < table.dealer.size() && round >= seenD && round < MAX_CARDS) {
                dBorn[round] = now + (fresh ? (round * (participants + 1) + k) * 4L : 0);
            }
        }
        for (int i = 0; i < BlackjackTable.SEATS; i++) {
            int n = table.seats[i].cards.size();
            for (int j = Math.max(2, seenCards[i]); j < n && j < MAX_CARDS; j++) {
                born[i][j] = now;
            }
            seenCards[i] = Math.max(seenCards[i], n);
        }
        int nd = table.dealer.size();
        for (int j = Math.max(2, seenD); j < nd && j < MAX_CARDS; j++) {
            dBorn[j] = now;
        }
        seenD = Math.max(seenD, nd);
        if (table.holeRevealed && !seenHole) {
            revealAt = now + (fresh ? 30 : 0);
        }
        seenHole = table.holeRevealed;
        if (table.phase == Phase.RESULT && seenPhase != Phase.RESULT) {
            resultAt = now + (revealAt > now ? revealAt - now + 12 : 12);
        }
        // efectos al mostrarse los resultados
        if (resultAt == now) {
            for (int i = 0; i < BlackjackTable.SEATS; i++) {
                Seat s = table.seats[i];
                if (s.result == Result.WIN || s.result == Result.BLACKJACK) {
                    double[] p = BlackjackLayout.polar(1.6, BlackjackLayout.seatAngle(i));
                    Vec3 w = worldPos(p[0], p[1], 0.95);
                    int n = s.result == Result.BLACKJACK ? 24 : 8;
                    for (int k = 0; k < n; k++) {
                        double a = level.random.nextDouble() * Math.PI * 2;
                        double sp = 0.03 + level.random.nextDouble() * 0.08;
                        level.addParticle(s.result == Result.BLACKJACK && k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER,
                                w.x, w.y, w.z, Math.cos(a) * sp, Math.abs(Math.sin(a)) * sp + 0.03, Math.sin(a) * sp);
                    }
                }
            }
        }
        seenPhase = table.phase;
        // sonidos de cartas cuando "llegan" (las del reparto inicial salen escalonadas)
        boolean card = false;
        for (int i = 0; i < BlackjackTable.SEATS && !card; i++) {
            for (int j = 0; j < MAX_CARDS; j++) {
                if (born[i][j] == now) {
                    card = true;
                    break;
                }
            }
        }
        for (int j = 0; j < MAX_CARDS && !card; j++) {
            card = dBorn[j] == now;
        }
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
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.offset(-3, 0, -3), worldPosition.offset(4, 3, 5));
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag t = pkt.getTag();
        if (t != null) {
            load(t);
        }
    }

    private static int[] toArray(List<Integer> l) {
        return l.stream().mapToInt(Integer::intValue).toArray();
    }

    private static void fill(List<Integer> l, int[] a) {
        l.clear();
        for (int v : a) {
            if (v >= 0 && v < 52 && l.size() < MAX_CARDS) {
                l.add(v);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ListTag seats = new ListTag();
        for (int i = 0; i < BlackjackTable.SEATS; i++) {
            Seat s = table.seats[i];
            CompoundTag t = new CompoundTag();
            t.putIntArray("cards", toArray(s.cards));
            t.putInt("bet", s.bet);
            t.putBoolean("doubled", s.doubled);
            t.putInt("state", s.state.ordinal());
            t.putInt("result", s.result.ordinal());
            t.putInt("payout", s.payout);
            if (players[i] != null) {
                t.putUUID("player", players[i]);
            }
            t.putString("name", names[i]);
            seats.add(t);
        }
        tag.put("seats", seats);
        tag.putIntArray("dealer", toArray(table.dealer));
        tag.putInt("phase", table.phase.ordinal());
        tag.putBoolean("hole", table.holeRevealed);
        tag.putInt("current", table.current);
        tag.putInt("roundSeq", roundSeq);
        tag.putLong("phaseEnd", phaseEnd);
        tag.putLong("nextStep", nextStep);
        tag.putLong("settledAt", settledAt);
        tag.putBoolean("paid", paid);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ListTag seats = tag.getList("seats", Tag.TAG_COMPOUND);
        BlackjackTable.Phase[] ph = Phase.values();
        SeatState[] ss = SeatState.values();
        Result[] rs = Result.values();
        for (int i = 0; i < BlackjackTable.SEATS; i++) {
            Seat s = table.seats[i];
            if (i < seats.size()) {
                CompoundTag t = seats.getCompound(i);
                fill(s.cards, t.getIntArray("cards"));
                s.bet = t.getInt("bet");
                s.doubled = t.getBoolean("doubled");
                s.state = ss[Math.max(0, Math.min(ss.length - 1, t.getInt("state")))];
                s.result = rs[Math.max(0, Math.min(rs.length - 1, t.getInt("result")))];
                s.payout = t.getInt("payout");
                players[i] = t.hasUUID("player") ? t.getUUID("player") : null;
                names[i] = t.getString("name");
            } else {
                s.cards.clear();
                s.bet = 0;
                s.doubled = false;
                s.state = SeatState.EMPTY;
                s.result = Result.NONE;
                s.payout = 0;
                players[i] = null;
                names[i] = "";
            }
        }
        fill(table.dealer, tag.getIntArray("dealer"));
        table.phase = ph[Math.max(0, Math.min(ph.length - 1, tag.getInt("phase")))];
        table.holeRevealed = tag.getBoolean("hole");
        table.current = tag.getInt("current");
        roundSeq = tag.getInt("roundSeq");
        phaseEnd = tag.getLong("phaseEnd");
        nextStep = tag.getLong("nextStep");
        settledAt = tag.getLong("settledAt");
        paid = tag.getBoolean("paid");
    }
}
