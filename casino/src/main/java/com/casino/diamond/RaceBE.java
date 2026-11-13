package com.casino.diamond;

import com.casino.chip.Chips;
import com.casino.game.HorseRace;
import com.casino.net.CasinoNet;
import com.casino.net.WheelWinPacket;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.plat.Plat;
import com.casino.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La pantalla gigante de las carreras de caballos (una por sala). Lleva el ciclo compartido: apuestas (45 s) → cajones con cuenta
 * regresiva (5 s) → carrera → resultados (12 s) → siguiente carrera. Todo sale de la semilla de la carrera ({@link HorseRace}).
 * Si nadie está cerca durante las apuestas, la cuenta espera.
 */
public class RaceBE extends CasinoBE {
    public enum Phase { BET, GATE, RUN, RESULT }

    public static final int MIN_BET = 10;
    public static final int MAX_BET = 50_000;
    /** Distancia para apostar y para que la carrera avance. */
    public static final double RANGE = 32;

    public record Bet(int horse, int amount, String name) {
    }

    public long seed;
    public long cycleStart;
    public int lastWinner = -1;
    public long lastSeed;
    public final Map<UUID, Bet> bets = new LinkedHashMap<>();
    private final Map<UUID, Long> pending = new HashMap<>();
    /** Tamaño de la pantalla en bloques y cuántos bloques se extiende hacia la izquierda del bloque (visto de frente). */
    public int width = 12;
    public int height = 6;
    public int left = -5;

    private boolean settled;
    private Phase lastPhase;
    private HorseRace cached;

    public RaceBE(BlockPos pos, BlockState state) {
        super(DiamondReg.RACE_BE.get(), pos, state);
    }

    public HorseRace race() {
        if (cached == null || cached.seed != seed) {
            cached = HorseRace.of(seed);
        }
        return cached;
    }

    public Direction facing() {
        return getBlockState().getValue(RaceScreenBlock.FACING);
    }

    public long elapsed(long now) {
        return Math.max(0, now - cycleStart);
    }

    public Phase phase(long now) {
        long t = elapsed(now);
        if (t < HorseRace.BET_TICKS) {
            return Phase.BET;
        }
        t -= HorseRace.BET_TICKS;
        if (t < HorseRace.GATE_TICKS) {
            return Phase.GATE;
        }
        t -= HorseRace.GATE_TICKS;
        if (t < race().raceTicks()) {
            return Phase.RUN;
        }
        return Phase.RESULT;
    }

    /** Ticks que faltan para cerrar las apuestas. */
    public int betTicksLeft(long now) {
        return (int) Math.max(0, HorseRace.BET_TICKS - elapsed(now));
    }

    /** Segundos de carrera (negativo antes de la salida). */
    public double raceSeconds(long now, float partial) {
        return (elapsed(now) - HorseRace.BET_TICKS - HorseRace.GATE_TICKS + partial) / 20.0;
    }

    public long cycleTicks() {
        return HorseRace.BET_TICKS + HorseRace.GATE_TICKS + race().raceTicks() + HorseRace.RESULT_TICKS;
    }

    /** Centro de la pantalla en el mundo (para sonidos y distancias). */
    public Vec3 center() {
        Direction f = facing();
        Direction right = f.getCounterClockWise();
        double cx = left + width / 2.0 - 0.5;
        return Vec3.atCenterOf(worldPosition).add(right.getStepX() * cx, height / 2.0 - 0.5, right.getStepZ() * cx);
    }

    @Override
    protected AABB renderBox() {
        return new AABB(worldPosition).inflate(Math.max(width, height) + 1);
    }

    // ------------------------------------------------------------------ servidor

    public static void serverTick(Level level, BlockPos pos, BlockState st, RaceBE be) {
        if (level instanceof ServerLevel sl) {
            be.tick(sl);
        }
    }

    private void newRace(ServerLevel level, long now) {
        if (seed != 0) {
            lastSeed = seed;
            lastWinner = race().order[0];
        }
        seed = level.getRandom().nextLong();
        if (seed == 0) {
            seed = 1;
        }
        cycleStart = now;
        bets.clear();
        settled = false;
        lastPhase = Phase.BET;
        sync();
    }

    private void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (seed == 0 || cycleStart == 0 || now < cycleStart) {
            newRace(level, now);
            return;
        }
        Phase ph = phase(now);
        if (now % 20 == 0) {
            payPending(level.getServer());
            // nadie mirando: la cuenta de las apuestas espera
            if (ph == Phase.BET && bets.isEmpty() && !playersNear(level) && elapsed(now) > 40) {
                cycleStart = now;
                sync();
                return;
            }
        }
        if (ph != lastPhase) {
            Phase prev = lastPhase;
            lastPhase = ph;
            if (ph == Phase.GATE) {
                sound(level, ModSounds.RACE_BUGLE.get(), 1.0F);
            } else if (ph == Phase.RUN) {
                sound(level, ModSounds.RACE_GATE.get(), 1.0F);
            } else if (ph == Phase.RESULT && prev != null) {
                settle(level);
            }
        }
        if (ph == Phase.RESULT && !settled) {
            settle(level);
        }
        if (elapsed(now) >= cycleTicks()) {
            newRace(level, now);
        }
    }

    private boolean playersNear(Level level) {
        for (Player p : level.players()) {
            if (!p.isSpectator() && inHall(p)) {
                return true;
            }
        }
        return false;
    }

    /** ¿El jugador está en la sala, frente a la pantalla? (hasta 26 bloques al frente, el ancho de la pantalla más 4 a cada lado). */
    public boolean inHall(Player p) {
        Vec3 d = p.position().subtract(Vec3.atCenterOf(worldPosition));
        Direction f = facing();
        Direction right = f.getCounterClockWise();
        double fwd = d.x * f.getStepX() + d.z * f.getStepZ();
        double lat = d.x * right.getStepX() + d.z * right.getStepZ() - (left + width / 2.0 - 0.5);
        return fwd > -1.5 && fwd < 26 && Math.abs(lat) < width / 2.0 + 4 && d.y > -6 && d.y < 8;
    }

    private void sound(ServerLevel level, SoundEvent ev, float pitch) {
        Vec3 c = center();
        level.playSound(null, c.x, c.y, c.z, ev, SoundSource.BLOCKS, 3.0F, pitch);
    }

    public static MutableComponent horseName(HorseRace r, int i) {
        return Component.translatable("casino.horse.name." + r.name[i]);
    }

    private void settle(ServerLevel level) {
        if (settled) {
            return;
        }
        settled = true;
        HorseRace r = race();
        int w = r.order[0];
        sound(level, ModSounds.RACE_CROWD.get(), 1.0F);
        Component winLine = Component.translatable("casino.race.winner", w + 1, horseName(r, w), r.oddsText(w)).withStyle(ChatFormatting.GOLD);
        // el resultado solo se avisa a quien está en la sala de la pantalla (antes llenaba el chat de todo el casino)
        for (ServerPlayer p : level.players()) {
            if (inHall(p)) {
                Plat.msg(p, winLine, false);
            }
        }
        MinecraftServer server = level.getServer();
        for (Map.Entry<UUID, Bet> e : bets.entrySet()) {
            Bet b = e.getValue();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (b.horse() == w) {
                long pay = r.payout(w, b.amount());
                if (p != null) {
                    pay(p, (int) Math.min(Integer.MAX_VALUE, pay), r, w, b.amount());
                } else {
                    pending.merge(e.getKey(), pay, Long::sum);
                }
            } else if (p != null) {
                Plat.msg(p, Component.translatable("casino.race.lost", b.horse() + 1, horseName(r, b.horse()), r.place[b.horse()] + 1)
                        .withStyle(ChatFormatting.GRAY), false);
            }
        }
        setChanged();
    }

    private static void pay(ServerPlayer p, int pay, HorseRace r, int w, int stake) {
        Chips.give(p, pay);
        Component title = Component.translatable("casino.race.you_won", pay);
        CasinoNet.sendTo(p, new WheelWinPacket(title, List.of(), 0, pay >= stake * 5));
        Plat.msg(p, Component.translatable("casino.race.paid", w + 1, horseName(r, w), pay).withStyle(ChatFormatting.GREEN), false);
    }

    private void payPending(MinecraftServer server) {
        if (pending.isEmpty()) {
            return;
        }
        for (UUID u : new ArrayList<>(pending.keySet())) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) {
                long v = pending.remove(u);
                Chips.give(p, (int) Math.min(Integer.MAX_VALUE, v));
                Plat.msg(p, Component.translatable("casino.race.pending", v).withStyle(ChatFormatting.GREEN), false);
                setChanged();
            }
        }
    }

    /** Apuesta de un jugador (amount 0 = retirar la apuesta). Solo mientras están abiertas las apuestas. */
    public void placeBet(ServerPlayer p, int horse, int amount) {
        Level level = p.level();
        long now = level.getGameTime();
        if (p.distanceToSqr(center()) > RANGE * RANGE) {
            return;
        }
        if (phase(now) != Phase.BET || betTicksLeft(now) < 10) {
            Plat.msg(p, Component.translatable("casino.race.closed").withStyle(ChatFormatting.RED), true);
            return;
        }
        Bet prev = bets.get(p.getUUID());
        int back = prev == null ? 0 : prev.amount();
        if (amount <= 0) {
            if (prev != null) {
                bets.remove(p.getUUID());
                Chips.give(p, back);
                Plat.msg(p, Component.translatable("casino.race.cancelled", back).withStyle(ChatFormatting.YELLOW), true);
                sync();
            }
            return;
        }
        if (horse < 0 || horse >= HorseRace.N) {
            return;
        }
        amount = Math.max(MIN_BET, Math.min(MAX_BET, amount));
        if (Chips.balance(p) + back < amount) {
            Plat.msg(p, Component.translatable("casino.cashier.no_chips").withStyle(ChatFormatting.RED), true);
            level.playSound(null, p.blockPosition(), ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        if (back > 0) {
            Chips.give(p, back);
        }
        if (!Chips.take(p, amount)) {
            return;
        }
        bets.put(p.getUUID(), new Bet(horse, amount, p.getName().getString()));
        HorseRace r = race();
        Plat.msg(p, Component.translatable("casino.race.bet_ok", amount, horse + 1, horseName(r, horse), r.payout(horse, amount))
                .withStyle(ChatFormatting.GOLD), true);
        level.playSound(null, p.blockPosition(), ModSounds.CHIP.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        sync();
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState s = getBlockState();
            level.sendBlockUpdated(worldPosition, s, s, 3);
        }
    }

    // ------------------------------------------------------------------ datos

    @Override
    protected void writeData(CompoundTag tag) {
        tag.putLong("Seed", seed);
        tag.putLong("Start", cycleStart);
        tag.putInt("LastWinner", lastWinner);
        tag.putLong("LastSeed", lastSeed);
        tag.putBoolean("Settled", settled);
        tag.putInt("W", width);
        tag.putInt("H", height);
        tag.putInt("L", left);
        ListTag l = new ListTag();
        for (Map.Entry<UUID, Bet> e : bets.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("U", e.getKey().toString());
            t.putInt("Horse", e.getValue().horse());
            t.putInt("Amount", e.getValue().amount());
            t.putString("Name", e.getValue().name());
            l.add(t);
        }
        tag.put("Bets", l);
        ListTag pl = new ListTag();
        for (Map.Entry<UUID, Long> e : pending.entrySet()) {
            CompoundTag t = new CompoundTag();
            t.putString("U", e.getKey().toString());
            t.putLong("V", e.getValue());
            pl.add(t);
        }
        tag.put("Pending", pl);
    }

    @Override
    protected CompoundTag syncTag(CompoundTag full) {
        full.remove("Pending");
        return full;
    }

    @Override
    protected void readData(CompoundTag tag) {
        seed = Nbt.l(tag, "Seed");
        cycleStart = Nbt.l(tag, "Start");
        lastWinner = Nbt.has(tag, "LastWinner") ? Nbt.i(tag, "LastWinner") : -1;
        lastSeed = Nbt.l(tag, "LastSeed");
        settled = Nbt.b(tag, "Settled");
        if (Nbt.has(tag, "W")) {
            width = Math.max(2, Nbt.i(tag, "W"));
            height = Math.max(2, Nbt.i(tag, "H"));
            left = Nbt.i(tag, "L");
        }
        bets.clear();
        ListTag l = Nbt.list(tag, "Bets");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag t = Nbt.at(l, i);
            try {
                bets.put(UUID.fromString(Nbt.s(t, "U")), new Bet(Nbt.i(t, "Horse"), Nbt.i(t, "Amount"), Nbt.s(t, "Name")));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (Nbt.has(tag, "Pending")) {
            pending.clear();
            ListTag pl = Nbt.list(tag, "Pending");
            for (int i = 0; i < pl.size(); i++) {
                CompoundTag t = Nbt.at(pl, i);
                try {
                    pending.put(UUID.fromString(Nbt.s(t, "U")), Nbt.l(t, "V"));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }
}
