package com.casino.block;

import com.casino.plat.Plat;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.entity.DealerEntity;
import com.casino.game.Prize;
import com.casino.game.RouletteBets;
import com.casino.game.RouletteLayout;
import com.casino.game.RouletteTable;
import com.casino.game.RouletteTable.Phase;
import com.casino.game.RouletteTable.Slot;
import com.casino.game.RouletteWheel;
import com.casino.net.CasinoNet;
import com.casino.net.RoulettePacket;
import com.casino.net.WheelWinPacket;
import com.casino.registry.ModBlockEntities;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * El corazón de la mesa de ruleta (bloque del centro). La ronda completa vive aquí y se guarda con el mundo. El servidor decide todo
 * (apuestas, número ganador, pagos); el cliente solo pide jugadas y anima lo que ve. La tirada se anima en todos los clientes como función
 * del tiempo (ver RouletteWheel): el servidor solo anota qué número salió y cuándo empezó.
 *
 * Ronda: se apuesta con fichas (cada ficha se cobra al ponerla y se devuelve si se quita); con la primera apuesta corre el tiempo para
 * apostar (30 s) o se tira antes si todos los apostadores avisan que están listos. El crupier dice "no va más" y lanza la bola; cuando
 * se detiene se paga a todos y, unos segundos después, se limpia la mesa.
 */
public class RouletteBE extends CasinoBE {
    private static final int BET_TICKS = 600;
    private static final int START_DELAY = 24;
    private static final int RESULT_TICKS = 200;
    private static final double NEAR = 8.0;

    public final RouletteTable table = new RouletteTable();
    public final UUID[] players = new UUID[RouletteTable.MAX_PLAYERS];
    public final String[] names = new String[RouletteTable.MAX_PLAYERS];
    /** Fin del tiempo de apuestas (reloj del juego). */
    public long phaseEnd = 0;
    /** Cuándo empezó la última tirada, cuántas van, número que sale y semilla del lanzamiento. */
    public long spinStart = -1;
    public int spinSeq = 0;
    public int spinNumber = 0;
    public int spinSeed = 0;
    public long settledAt = 0;
    private boolean paid = false;
    private boolean warned = false;
    private final Random rng = new Random();
    /** Las últimas apuestas de cada jugador, para "repetir apuesta". */
    private final Map<UUID, int[]> lastBets = new LinkedHashMap<>();

    // ---- solo cliente
    private boolean clientInit = false;
    private long seenStart = -1;
    private int seenSeq = -1;
    private int seenSlotsHash = 0;
    private double lastRelIdx = Double.NaN;
    private long lastRoll = -100;
    private boolean landedPlayed = false;
    private boolean resultFx = false;
    /** Instante en que se puso la última ficha en cada casilla (para animarla). En el servidor solo se usa para jugador. */
    public final long[][] spotBorn = new long[RouletteTable.MAX_PLAYERS][RouletteBets.SPOTS.size()];
    private final int[] seenTotal = new int[RouletteTable.MAX_PLAYERS];
    private final int[][] seenStake = new int[RouletteTable.MAX_PLAYERS][RouletteBets.SPOTS.size()];
    public long resultShownAt = -1000;

    public RouletteBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROULETTE.get(), pos, state);
        Arrays.fill(names, "");
    }

    public Direction facing() {
        return getBlockState().getValue(RouletteBlock.FACING);
    }

    /** De coordenadas de la mesa (x a la derecha del jugador, z hacia los jugadores) a coordenadas del mundo. */
    public Vec3 worldPos(double u, double w, double y) {
        Direction f = facing();
        Direction r = f.getCounterClockWise();
        return new Vec3(worldPosition.getX() + 0.5 + u * r.getStepX() + w * f.getStepX(), worldPosition.getY() + y,
                worldPosition.getZ() + 0.5 + u * r.getStepZ() + w * f.getStepZ());
    }

    public int slotOf(@Nullable UUID id) {
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
        AABB box = new AABB(RouletteBlock.dealerSpot(worldPosition, facing())).inflate(1.2, 1.0, 1.2);
        return level.getEntitiesOfClass(DealerEntity.class, box, d -> worldPosition.equals(d.getTable()));
    }

    public void spawnDealer(ServerLevel sl) {
        if (!dealers().isEmpty()) {
            return;
        }
        Direction f = facing();
        BlockPos spot = RouletteBlock.dealerSpot(worldPosition, f);
        DealerEntity d = Plat.create(ModEntities.DEALER.get(), sl);
        if (d == null) {
            return;
        }
        d.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, f.toYRot(), 0.0F);
        d.setYHeadRot(f.toYRot());
        d.setYBodyRot(f.toYRot());
        d.setGame(DealerEntity.GAME_ROULETTE);
        d.setTable(worldPosition);
        sl.addFreshEntity(d);
    }

    private void dealerSwing() {
        for (DealerEntity d : dealers()) {
            Plat.swing(d);
        }
    }

    private Vec3 center() {
        // el centro de la rueda: de ahí salen todos los sonidos y efectos
        return worldPos(RouletteLayout.WHEEL_X, RouletteLayout.WHEEL_Z, RouletteLayout.Y_TOP + 0.1);
    }

    private void sound(SoundEvent s, float vol, float pitch) {
        if (level != null) {
            Vec3 c = center();
            level.playSound(null, c.x, c.y, c.z, s, SoundSource.BLOCKS, vol, pitch);
        }
    }

    private void msg(ServerPlayer p, String key, ChatFormatting color, Object... args) {
        Plat.msg(p, Component.translatable(key, args).withStyle(color), true);
    }

    @Nullable
    private ServerPlayer online(int slot) {
        return level instanceof ServerLevel sl && players[slot] != null ? sl.getServer().getPlayerList().getPlayer(players[slot]) : null;
    }

    // ------------------------------------------------------------------ jugadas (servidor)

    private boolean bettingOpen(ServerPlayer p, long now) {
        if (table.phase == Phase.RESULT && now >= settledAt + 50) {
            endRound(); // los resultados ya se vieron: se limpia y se puede apostar de nuevo
        }
        if (table.phase != Phase.IDLE && table.phase != Phase.BETTING) {
            msg(p, "casino.roulette.closed", ChatFormatting.YELLOW);
            return false;
        }
        return true;
    }

    /** El asiento de este jugador; si no tiene, le da uno libre (o -1 si la mesa está llena). */
    private int slotFor(ServerPlayer p) {
        int s = slotOf(p.getUUID());
        if (s >= 0) {
            return s;
        }
        for (int i = 0; i < players.length; i++) {
            if (players[i] == null) {
                players[i] = p.getUUID();
                names[i] = p.getGameProfile().getName();
                table.slots[i].clear();
                return i;
            }
        }
        msg(p, "casino.roulette.full", ChatFormatting.YELLOW);
        return -1;
    }

    private void releaseIfEmpty(int slot) {
        if (slot >= 0 && table.slots[slot].total == 0 && (table.phase == Phase.IDLE || table.phase == Phase.BETTING)) {
            players[slot] = null;
            names[slot] = "";
            table.slots[slot].clear();
        }
    }

    private void betPlaced(long now) {
        if (table.phase == Phase.IDLE) {
            table.phase = Phase.BETTING;
            phaseEnd = now + BET_TICKS;
            paid = false;
            warned = false;
        }
        sound(ModSounds.CHIP.get(), 0.8F, 0.9F + rng.nextFloat() * 0.3F);
        sync();
    }

    private void noChips(ServerPlayer p, int needed) {
        msg(p, "casino.plinko.no_chips", ChatFormatting.RED, needed);
        sound(ModSounds.DENIED.get(), 0.6F, 1.0F);
    }

    private void reject(ServerPlayer p, int code) {
        switch (code) {
            case RouletteTable.SPOT_LIMIT -> msg(p, "casino.roulette.spot_limit", ChatFormatting.RED, RouletteTable.MAX_SPOT);
            case RouletteTable.TOTAL_LIMIT -> msg(p, "casino.roulette.total_limit", ChatFormatting.RED, RouletteTable.MAX_TOTAL);
            default -> msg(p, "casino.roulette.closed", ChatFormatting.YELLOW);
        }
        sound(ModSounds.DENIED.get(), 0.5F, 1.0F);
    }

    /** Pone fichas en una casilla: cobra, y si la mesa no lo permite devuelve todo. */
    private boolean tryPlace(ServerPlayer p, int slot, int spot, int amount) {
        int code = table.place(slot, spot, amount);
        if (code != RouletteTable.OK) {
            reject(p, code);
            return false;
        }
        if (!Chips.take(p, amount)) {
            table.remove(slot, spot, amount);
            noChips(p, amount);
            return false;
        }
        return true;
    }

    public void act(ServerPlayer p, int action, int arg1, int arg2) {
        if (level == null || level.isClientSide()) {
            return;
        }
        long now = level.getGameTime();
        switch (action) {
            case RoulettePacket.PLACE -> {
                if (!isChip(arg2) || !bettingOpen(p, now)) {
                    return;
                }
                int slot = slotFor(p);
                if (slot < 0) {
                    return;
                }
                if (tryPlace(p, slot, arg1, arg2)) {
                    betPlaced(now);
                } else {
                    releaseIfEmpty(slot);
                }
            }
            case RoulettePacket.REMOVE -> {
                int slot = slotOf(p.getUUID());
                int q = table.remove(slot, arg1, arg2);
                if (q > 0) {
                    Chips.give(p, q);
                    sound(ModSounds.CHIP.get(), 0.5F, 1.4F);
                    afterRemove(slot, now);
                }
            }
            case RoulettePacket.UNDO -> {
                int slot = slotOf(p.getUUID());
                int[] u = table.undo(slot);
                if (u != null) {
                    Chips.give(p, u[1]);
                    sound(ModSounds.CHIP.get(), 0.5F, 1.4F);
                    afterRemove(slot, now);
                }
            }
            case RoulettePacket.CLEAR -> {
                int slot = slotOf(p.getUUID());
                int q = table.clear(slot);
                if (q > 0) {
                    Chips.give(p, q);
                    sound(ModSounds.CHIP.get(), 0.6F, 1.2F);
                    afterRemove(slot, now);
                }
            }
            case RoulettePacket.REBET -> {
                int[] last = lastBets.get(p.getUUID());
                if (last == null || last.length == 0) {
                    msg(p, "casino.roulette.nothing_to_rebet", ChatFormatting.YELLOW);
                    return;
                }
                if (!bettingOpen(p, now)) {
                    return;
                }
                int slot = slotFor(p);
                if (slot < 0) {
                    return;
                }
                List<int[]> plan = new ArrayList<>();
                int cost = 0;
                for (int i = 0; i + 1 < last.length; i += 2) {
                    plan.add(new int[]{last[i], last[i + 1]});
                    cost += last[i + 1];
                }
                // "repetir" reemplaza lo que ya tenías puesto (así no se duplica si se pulsa dos veces)
                int current = table.slots[slot].total;
                if (Chips.balance(p) + current < cost) {
                    noChips(p, cost);
                    releaseIfEmpty(slot);
                    return;
                }
                if (current > 0) {
                    table.clear(slot);
                    Chips.give(p, current);
                }
                placeAll(p, slot, plan, now);
            }
            case RoulettePacket.DOUBLE -> {
                int slot = slotOf(p.getUUID());
                if (slot < 0 || table.slots[slot].total == 0 || !bettingOpen(p, now)) {
                    return;
                }
                List<int[]> plan = new ArrayList<>();
                Slot s = table.slots[slot];
                for (int i = 0; i < s.stake.length; i++) {
                    if (s.stake[i] > 0) {
                        plan.add(new int[]{i, s.stake[i]});
                    }
                }
                placeAll(p, slot, plan, now);
            }
            case RoulettePacket.CALL -> {
                if (arg1 < 0 || arg1 >= RouletteBets.CALLS || !isChip(arg2) || !bettingOpen(p, now)) {
                    return;
                }
                int slot = slotFor(p);
                if (slot < 0) {
                    return;
                }
                List<int[]> plan = new ArrayList<>();
                for (int[] c : RouletteBets.call(arg1)) {
                    plan.add(new int[]{c[0], c[1] * arg2});
                }
                placeAll(p, slot, plan, now);
            }
            case RoulettePacket.READY -> {
                int slot = slotOf(p.getUUID());
                if (slot >= 0 && table.phase == Phase.BETTING && table.slots[slot].total > 0) {
                    table.slots[slot].ready = !table.slots[slot].ready;
                    sound(ModSounds.CHIP.get(), 0.4F, table.slots[slot].ready ? 1.6F : 1.1F);
                    sync();
                }
            }
            default -> {
            }
        }
    }

    private static boolean isChip(int v) {
        for (int c : RouletteTable.CHIPS) {
            if (c == v) {
                return true;
            }
        }
        return false;
    }

    /** Pone varias apuestas juntas (repetir, doblar, apuestas de llamada): o entran todas o no entra ninguna. */
    private void placeAll(ServerPlayer p, int slot, List<int[]> plan, long now) {
        Slot s = table.slots[slot];
        int cost = 0;
        for (int[] x : plan) {
            cost += x[1];
            if (x[0] < 0 || x[0] >= RouletteBets.SPOTS.size() || s.stake[x[0]] + x[1] > RouletteTable.MAX_SPOT) {
                reject(p, RouletteTable.SPOT_LIMIT);
                return;
            }
        }
        if (s.total + cost > RouletteTable.MAX_TOTAL) {
            reject(p, RouletteTable.TOTAL_LIMIT);
            return;
        }
        if (Chips.balance(p) < cost) {
            noChips(p, cost);
            releaseIfEmpty(slot);
            return;
        }
        if (!Chips.take(p, cost)) {
            noChips(p, cost);
            return;
        }
        for (int[] x : plan) {
            table.place(slot, x[0], x[1]);
        }
        betPlaced(now);
    }

    private void afterRemove(int slot, long now) {
        if (table.betCount() == 0 && table.phase == Phase.BETTING) {
            table.phase = Phase.IDLE;
        }
        releaseIfEmpty(slot);
        sync();
    }

    private void endRound() {
        table.clearRound();
        Arrays.fill(players, null);
        Arrays.fill(names, "");
        paid = false;
        sync();
    }

    /** Se rompe la mesa: se devuelven las fichas de quien apostó y todavía no se tiró. */
    public void refundAll() {
        if (!(level instanceof ServerLevel sl) || table.phase == Phase.RESULT) {
            return;
        }
        for (int i = 0; i < players.length; i++) {
            Slot s = table.slots[i];
            if (players[i] != null && s.total > 0) {
                refund(sl, players[i], s.total);
            }
            s.clear();
        }
    }

    private void refund(ServerLevel sl, UUID id, int amount) {
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
        Prize prize = new Prize(Component.translatable("casino.roulette.refund"), new ArrayList<>(Chips.stacksFor(amount)), 0, -1, false);
        if (p != null) {
            prize.deliver(p);
        } else {
            CasinoData.get(sl.getServer()).addPending(id, prize);
        }
    }

    // ------------------------------------------------------------------ la ronda

    private boolean everyoneReady() {
        boolean any = false;
        for (int i = 0; i < players.length; i++) {
            Slot s = table.slots[i];
            if (players[i] != null && s.total > 0) {
                if (!s.ready) {
                    return false;
                }
                any = true;
            }
        }
        return any;
    }

    private void startSpin(long now) {
        table.phase = Phase.SPINNING;
        spinNumber = rng.nextInt(RouletteWheel.N);
        spinSeed = rng.nextInt();
        spinSeq++;
        spinStart = now + START_DELAY;
        paid = false;
        for (int i = 0; i < players.length; i++) {
            Slot s = table.slots[i];
            if (players[i] != null && s.total > 0) {
                int[] pairs = new int[2 * countSpots(s)];
                int k = 0;
                for (int sp = 0; sp < s.stake.length; sp++) {
                    if (s.stake[sp] > 0) {
                        pairs[k++] = sp;
                        pairs[k++] = s.stake[sp];
                    }
                }
                lastBets.remove(players[i]);
                lastBets.put(players[i], pairs);
                ServerPlayer p = online(i);
                if (p != null) {
                    msg(p, "casino.roulette.no_more_bets", ChatFormatting.GOLD);
                }
            }
        }
        while (lastBets.size() > 24) {
            lastBets.remove(lastBets.keySet().iterator().next());
        }
        sound(ModSounds.ROULETTE_BELL.get(), 1.0F, 1.0F);
        sync();
    }

    private static int countSpots(Slot s) {
        int n = 0;
        for (int v : s.stake) {
            if (v > 0) {
                n++;
            }
        }
        return n;
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        if (now % 100 == 9 && level instanceof ServerLevel sl) {
            spawnDealer(sl); // si al crupier le pasó algo, vuelve a aparecer
        }
        switch (table.phase) {
            case BETTING -> {
                if (!warned && now >= phaseEnd - 100) {
                    warned = true;
                    sound(ModSounds.CHIP.get(), 0.5F, 1.8F);
                    if (level instanceof ServerLevel sl) {
                        for (int i = 0; i < players.length; i++) {
                            ServerPlayer p = online(i);
                            if (p != null && table.slots[i].total > 0) {
                                msg(p, "casino.roulette.last_call", ChatFormatting.GOLD);
                            }
                        }
                    }
                }
                if (table.betCount() == 0) {
                    table.phase = Phase.IDLE;
                    sync();
                } else if (now >= phaseEnd || (now % 5 == 0 && everyoneReady())) {
                    startSpin(now);
                }
            }
            case SPINNING -> {
                if (now == spinStart) {
                    dealerSwing();
                }
                if (now >= spinStart + RouletteWheel.SPIN_TICKS) {
                    table.settle(spinNumber);
                    settledAt = now;
                    settleAll(now);
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

    /** La bola se detuvo: se paga a cada jugador (al instante si está, o queda guardado hasta que vuelva). */
    private void settleAll(long now) {
        if (paid || !(level instanceof ServerLevel sl)) {
            return;
        }
        paid = true;
        int win = table.winning;
        boolean anyWin = false;
        Component num = Component.translatable("casino.roulette.number_" + RouletteBets.colorOf(win), win);
        for (int i = 0; i < players.length; i++) {
            Slot s = table.slots[i];
            if (players[i] == null || s.total <= 0) {
                continue;
            }
            ServerPlayer p = online(i);
            int pay = s.payout;
            if (pay > 0) {
                boolean straight = s.stake[win] > 0;
                boolean big = s.net >= 500 || straight;
                Component title = straight ? Component.translatable("casino.roulette.banner_straight", win) : Component.translatable("casino.roulette.banner", pay);
                Prize prize = new Prize(title, new ArrayList<>(Chips.stacksFor(pay)), 0, -1, big);
                if (p != null) {
                    prize.deliver(p);
                    if (s.net >= 200 || straight) {
                        CasinoNet.sendTo(p, new WheelWinPacket(prize));
                    }
                } else {
                    CasinoData.get(sl.getServer()).addPending(players[i], prize);
                }
            }
            if (p != null) {
                if (s.net > 0) {
                    msg(p, "casino.roulette.res_win", ChatFormatting.GREEN, num, s.net);
                } else if (s.net == 0) {
                    msg(p, "casino.roulette.res_even", ChatFormatting.YELLOW, num);
                } else {
                    msg(p, "casino.roulette.res_lose", ChatFormatting.RED, num, -s.net);
                }
            }
            anyWin |= s.net > 0;
            if (s.net >= 2000) {
                Component m = Component.translatable("casino.roulette.won_big", Component.literal(names[i]).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf(s.net)).withStyle(ChatFormatting.GOLD), num).withStyle(ChatFormatting.GRAY);
                Vec3 c = center();
                for (ServerPlayer o : sl.players()) {
                    if (o.distanceToSqr(c.x, c.y, c.z) < 32 * 32) {
                        o.sendSystemMessage(m);
                    }
                }
            }
        }
        if (anyWin) {
            sound(ModSounds.COINS.get(), 0.9F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ cliente

    public void clientTick() {
        if (level == null) {
            return;
        }
        long now = level.getGameTime();
        Vec3 c = center();
        // fichas nuevas: se animan al caer
        for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
            Slot sl = table.slots[i];
            if (sl.total != seenTotal[i]) {
                for (int sp = 0; sp < sl.stake.length; sp++) {
                    if (clientInit && sl.stake[sp] > seenStake[i][sp]) {
                        spotBorn[i][sp] = now;
                    }
                    seenStake[i][sp] = sl.stake[sp];
                }
                seenTotal[i] = sl.total;
            }
        }
        if (!clientInit) {
            clientInit = true;
            seenStart = spinStart;
            seenSeq = spinSeq;
            landedPlayed = true;
            resultFx = true;
            return;
        }
        if (spinSeq != seenSeq) {
            seenSeq = spinSeq;
            seenStart = spinStart;
            lastRelIdx = Double.NaN;
            landedPlayed = false;
            resultFx = false;
            lastRoll = -100;
        }
        if (table.phase == Phase.SPINNING || (table.phase == Phase.RESULT && !landedPlayed)) {
            double tau = now - spinStart;
            if (tau >= 0 && tau < RouletteWheel.SPIN_TICKS) {
                RouletteWheel.Ball b = RouletteWheel.ball(now, spinStart, spinSeq, spinNumber, spinSeed);
                if (b.phase == 0 && now - lastRoll >= 11) {
                    lastRoll = now;
                    float k = (float) tau / RouletteWheel.TA;
                    level.playLocalSound(c.x, c.y, c.z, ModSounds.ROULETTE_ROLL.get(), SoundSource.BLOCKS, 0.55F, 1.1F - 0.25F * k, false);
                }
                if (b.phase == 1) {
                    double idx = Math.floor(b.rel / RouletteWheel.POCKET);
                    if (!Double.isNaN(lastRelIdx) && idx != lastRelIdx) {
                        level.playLocalSound(c.x, c.y, c.z, ModSounds.ROULETTE_TICK.get(), SoundSource.BLOCKS, 0.45F, 0.85F + (float) level.getRandom().nextDouble() * 0.35F, false);
                    }
                    lastRelIdx = idx;
                }
            }
            if (tau >= RouletteWheel.SPIN_TICKS - 1 && !landedPlayed) {
                landedPlayed = true;
                level.playLocalSound(c.x, c.y, c.z, ModSounds.ROULETTE_LAND.get(), SoundSource.BLOCKS, 0.8F, 1.0F, false);
            }
        }
        if (table.phase == Phase.RESULT && !resultFx) {
            resultFx = true;
            resultShownAt = now;
            int win = table.winning;
            double[] gc = RouletteBets.numberCenter(win);
            Vec3 w = worldPos(RouletteLayout.feltX(gc[0]), RouletteLayout.feltZ(gc[1]), RouletteLayout.Y_TOP + 0.1);
            for (int k = 0; k < 14; k++) {
                double a = level.getRandom().nextDouble() * Math.PI * 2;
                double sp = 0.02 + level.getRandom().nextDouble() * 0.05;
                level.addParticle(k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.HAPPY_VILLAGER, w.x, w.y, w.z, Math.cos(a) * sp,
                        Math.abs(Math.sin(a)) * sp + 0.03, Math.sin(a) * sp);
            }
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
        return new AABB(worldPosition).inflate(4.6, 3.0, 4.6);
    }




    private static int[] sparse(int[] stake) {
        int n = 0;
        for (int v : stake) {
            if (v > 0) {
                n++;
            }
        }
        int[] out = new int[n * 2];
        int k = 0;
        for (int i = 0; i < stake.length; i++) {
            if (stake[i] > 0) {
                out[k++] = i;
                out[k++] = stake[i];
            }
        }
        return out;
    }

    @Override
    protected void writeData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
            Slot s = table.slots[i];
            CompoundTag t = new CompoundTag();
            t.putIntArray("stakes", sparse(s.stake));
            t.putBoolean("ready", s.ready);
            t.putInt("payout", s.payout);
            t.putInt("net", s.net);
            if (players[i] != null) {
                Nbt.putUuid(t, "player", players[i]);
            }
            t.putString("name", names[i]);
            list.add(t);
        }
        tag.put("slots", list);
        tag.putInt("phase", table.phase.ordinal());
        tag.putInt("winning", table.winning);
        tag.putIntArray("past", table.past.stream().mapToInt(Integer::intValue).toArray());
        tag.putLong("phaseEnd", phaseEnd);
        tag.putLong("spinStart", spinStart);
        tag.putInt("spinSeq", spinSeq);
        tag.putInt("spinNumber", spinNumber);
        tag.putInt("spinSeed", spinSeed);
        tag.putLong("settledAt", settledAt);
        tag.putBoolean("paid", paid);
        ListTag lb = new ListTag();
        for (Map.Entry<UUID, int[]> e : lastBets.entrySet()) {
            CompoundTag t = new CompoundTag();
            Nbt.putUuid(t, "id", e.getKey());
            t.putIntArray("bets", e.getValue());
            lb.add(t);
        }
        tag.put("lastBets", lb);
    }

    @Override
    protected void readData(CompoundTag tag) {
        ListTag list = Nbt.list(tag, "slots");
        for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
            Slot s = table.slots[i];
            Arrays.fill(s.stake, 0);
            s.total = 0;
            if (i < list.size()) {
                CompoundTag t = Nbt.at(list, i);
                int[] st = Nbt.ia(t, "stakes");
                for (int k = 0; k + 1 < st.length; k += 2) {
                    if (st[k] >= 0 && st[k] < s.stake.length) {
                        s.stake[st[k]] = st[k + 1];
                        s.total += st[k + 1];
                    }
                }
                s.ready = Nbt.b(t, "ready");
                s.payout = Nbt.i(t, "payout");
                s.net = Nbt.i(t, "net");
                players[i] = Nbt.hasUuid(t, "player") ? Nbt.uuid(t, "player") : null;
                names[i] = Nbt.s(t, "name");
            } else {
                s.ready = false;
                s.payout = 0;
                s.net = 0;
                players[i] = null;
                names[i] = "";
            }
        }
        Phase[] ph = Phase.values();
        table.phase = ph[Math.max(0, Math.min(ph.length - 1, Nbt.i(tag, "phase")))];
        table.winning = Nbt.i(tag, "winning");
        table.past.clear();
        for (int v : Nbt.ia(tag, "past")) {
            if (v >= 0 && v <= 36 && table.past.size() < RouletteTable.HISTORY) {
                table.past.add(v);
            }
        }
        phaseEnd = Nbt.l(tag, "phaseEnd");
        spinStart = tag.contains("spinStart") ? Nbt.l(tag, "spinStart") : -1;
        spinSeq = Nbt.i(tag, "spinSeq");
        spinNumber = Math.max(0, Math.min(36, Nbt.i(tag, "spinNumber")));
        spinSeed = Nbt.i(tag, "spinSeed");
        settledAt = Nbt.l(tag, "settledAt");
        paid = Nbt.b(tag, "paid");
        lastBets.clear();
        ListTag lb = Nbt.list(tag, "lastBets");
        for (int i = 0; i < lb.size(); i++) {
            CompoundTag t = Nbt.at(lb, i);
            if (Nbt.hasUuid(t, "id")) {
                lastBets.put(Nbt.uuid(t, "id"), Nbt.ia(t, "bets"));
            }
        }
    }
}
