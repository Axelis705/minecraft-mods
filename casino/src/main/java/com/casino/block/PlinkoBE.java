package com.casino.block;

import com.casino.plat.Plat;
import com.casino.plat.CasinoBE;
import com.casino.plat.Nbt;
import com.casino.chip.Chips;
import com.casino.data.CasinoData;
import com.casino.game.PlinkoLayout;
import com.casino.game.Prize;
import com.casino.net.CasinoNet;
import com.casino.net.WheelWinPacket;
import com.casino.registry.ModBlockEntities;
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
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * El corazón de la mesa de Plinko. El servidor cobra la apuesta, sortea el camino de la bola en el momento de soltarla y paga cuando
 * llega al casillero. El cliente solo anima las bolas con el reloj del juego (así todos ven lo mismo y siempre acaba donde el servidor decidió).
 */
public class PlinkoBE extends CasinoBE {
    public static final int MAX_BALLS = 8;
    /** Cuánto se ve el resplandor del casillero y el cartel del multiplicador después de caer. */
    public static final int LANDING_SHOW = 70;

    public static final class Ball {
        public UUID player;
        public String name = "";
        public long start;
        public int path;
        public int risk;
        public int bet;
        /** solo cliente: última clavija tocada (para el sonido) */
        public int lastHit = -2;
    }

    public static final class Landing {
        public long time;
        public int bucket;
        public int risk;
        public int bet;
        public int m10;
        public int payout;
    }

    public final List<Ball> balls = new ArrayList<>();
    public final List<Landing> landings = new ArrayList<>();
    /** Nivel de riesgo de la última bola: es el que muestran los casilleros del tablero. */
    public int shownRisk = 1;
    public int landSeq = 0;

    // ---- solo cliente
    private boolean clientInit = false;
    private int clientSeq = 0;

    public PlinkoBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLINKO.get(), pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(LuckyWheelBlock.FACING);
    }

    private Vec3 rightVec() {
        Direction r = facing().getCounterClockWise();
        return new Vec3(r.getStepX(), 0, r.getStepZ());
    }

    /** Punto del tablero (x a la derecha, y desde el suelo) en el mundo. */
    public Vec3 boardPoint(double x, double y) {
        Vec3 d = rightVec();
        Direction f = facing();
        return new Vec3(worldPosition.getX() + 0.5 + d.x * x + f.getStepX() * 0.12, worldPosition.getY() + y, worldPosition.getZ() + 0.5 + d.z * x + f.getStepZ() * 0.12);
    }

    // ------------------------------------------------------------------ servidor

    /** Un jugador suelta una bola. Todas las comprobaciones se hacen aquí (el cliente solo pide). */
    public void drop(ServerPlayer p, int bet, int risk) {
        if (level == null || !PlinkoLayout.validBet(bet) || risk < 0 || risk >= PlinkoLayout.RISKS) {
            return;
        }
        if (balls.size() >= MAX_BALLS) {
            Plat.msg(p, Component.translatable("casino.plinko.busy").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (!Chips.take(p, bet)) {
            Plat.msg(p, Component.translatable("casino.plinko.no_chips", bet).withStyle(ChatFormatting.RED), true);
            level.playSound(null, worldPosition, ModSounds.DENIED.get(), SoundSource.BLOCKS, 0.6F, 1.0F);
            return;
        }
        Ball b = new Ball();
        b.player = p.getUUID();
        b.name = p.getGameProfile().getName();
        b.start = level.getGameTime();
        b.path = level.getRandom().nextInt(1 << PlinkoLayout.ROWS);
        b.risk = risk;
        b.bet = bet;
        balls.add(b);
        shownRisk = risk;
        level.playSound(null, worldPosition, ModSounds.PLINKO_DROP.get(), SoundSource.BLOCKS, 0.8F, 0.95F + level.getRandom().nextFloat() * 0.1F);
        sync();
    }

    public void serverTick() {
        if (level == null || balls.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        boolean changed = false;
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            if (now >= b.start + PlinkoLayout.LAND_TICKS) {
                it.remove();
                land(b, now);
                changed = true;
            }
        }
        if (changed) {
            sync();
        }
    }

    private void land(Ball b, long now) {
        int bucket = PlinkoLayout.bucketOf(b.path);
        int m10 = PlinkoLayout.mult10(b.risk, bucket);
        int payout = PlinkoLayout.payout(b.bet, m10);
        Landing l = new Landing();
        l.time = now;
        l.bucket = bucket;
        l.risk = b.risk;
        l.bet = b.bet;
        l.m10 = m10;
        l.payout = payout;
        landings.add(l);
        while (landings.size() > 8) {
            landings.remove(0);
        }
        landSeq++;
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        String mult = PlinkoLayout.format(m10);
        boolean win = payout > b.bet;
        Prize prize = new Prize(Component.translatable("casino.plinko.banner", mult), Chips.stacksFor(payout), 0, -1, m10 >= 100);
        ServerPlayer p = sl.getServer().getPlayerList().getPlayer(b.player);
        if (p != null) {
            prize.deliver(p);
            Plat.msg(p, Component.translatable(win ? "casino.plinko.win" : "casino.plinko.back", payout, mult)
                    .withStyle(win ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
            if (m10 >= 50) {
                CasinoNet.sendTo(p, new WheelWinPacket(prize));
            }
        } else {
            CasinoData.get(sl.getServer()).addPending(b.player, prize);
        }
        if (m10 >= 100) {
            Component msg = Component.translatable("casino.plinko.won", Component.literal(b.name).withStyle(ChatFormatting.YELLOW),
                    Component.literal(payout + "").withStyle(ChatFormatting.GOLD), Component.literal("x" + mult).withStyle(ChatFormatting.LIGHT_PURPLE)).withStyle(ChatFormatting.GRAY);
            Vec3 h = boardPoint(0, 2);
            for (ServerPlayer o : sl.players()) {
                if (o.distanceToSqr(h.x, h.y, h.z) < 32 * 32) {
                    o.sendSystemMessage(msg);
                }
            }
        }
    }

    // ------------------------------------------------------------------ cliente

    public void clientTick() {
        if (level == null) {
            return;
        }
        RandomSource r = level.getRandom();
        long now = level.getGameTime();
        if (!clientInit) {
            clientInit = true;
            clientSeq = landSeq;
            return;
        }
        // "tic" de cada clavija que toca una bola
        for (Ball b : balls) {
            int hit = PlinkoLayout.pegsHit(now - b.start);
            if (hit != b.lastHit) {
                if (hit >= 0 && b.lastHit != -2) {
                    double[] p = PlinkoLayout.position(b.path, now - b.start);
                    Vec3 w = boardPoint(p[0], p[1]);
                    level.playLocalSound(w.x, w.y, w.z, ModSounds.PLINKO_PEG.get(), SoundSource.BLOCKS, 0.45F, 0.75F + hit * 0.06F + r.nextFloat() * 0.05F, false);
                }
                b.lastHit = hit;
            }
        }
        // bola que acaba de caer: sonido y chispas en su casillero
        if (landSeq != clientSeq) {
            int fresh = Math.max(0, Math.min(landings.size(), landSeq - clientSeq));
            for (int i = landings.size() - fresh; i < landings.size(); i++) {
                if (i < 0) {
                    continue;
                }
                Landing l = landings.get(i);
                Vec3 w = boardPoint((l.bucket - PlinkoLayout.ROWS / 2.0) * PlinkoLayout.DX, 1.1);
                if (l.m10 >= 100) {
                    level.playLocalSound(w.x, w.y, w.z, ModSounds.WHEEL_JACKPOT.get(), SoundSource.BLOCKS, 1.2F, 1.0F, false);
                } else if (l.m10 >= 30) {
                    level.playLocalSound(w.x, w.y, w.z, ModSounds.WHEEL_WIN.get(), SoundSource.BLOCKS, 0.9F, 1.0F, false);
                } else {
                    level.playLocalSound(w.x, w.y, w.z, ModSounds.PLINKO_LAND.get(), SoundSource.BLOCKS, 0.8F, l.m10 >= 10 ? 1.15F : 0.85F, false);
                }
                int n = l.m10 >= 100 ? 50 : l.m10 >= 30 ? 26 : l.m10 >= 10 ? 8 : 0;
                for (int k = 0; k < n; k++) {
                    double a = r.nextDouble() * Math.PI * 2;
                    double sp = 0.05 + r.nextDouble() * 0.16;
                    Vec3 d = rightVec();
                    level.addParticle(l.m10 >= 100 && k % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.FIREWORK, w.x, w.y, w.z,
                            d.x * Math.cos(a) * sp, Math.abs(Math.sin(a)) * sp + 0.03, d.z * Math.cos(a) * sp);
                }
            }
            clientSeq = landSeq;
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
        return box(worldPosition.offset(-3, -1, -3), worldPosition.offset(4, 6, 4));
    }




    @Override
    protected void writeData(CompoundTag tag) {
        tag.putInt("shownRisk", shownRisk);
        tag.putInt("landSeq", landSeq);
        ListTag bl = new ListTag();
        for (Ball b : balls) {
            CompoundTag t = new CompoundTag();
            if (b.player != null) {
                Nbt.putUuid(t, "player", b.player);
            }
            t.putString("name", b.name);
            t.putLong("start", b.start);
            t.putInt("path", b.path);
            t.putInt("risk", b.risk);
            t.putInt("bet", b.bet);
            bl.add(t);
        }
        tag.put("balls", bl);
        ListTag ll = new ListTag();
        for (Landing l : landings) {
            CompoundTag t = new CompoundTag();
            t.putLong("time", l.time);
            t.putInt("bucket", l.bucket);
            t.putInt("risk", l.risk);
            t.putInt("bet", l.bet);
            t.putInt("m10", l.m10);
            t.putInt("payout", l.payout);
            ll.add(t);
        }
        tag.put("landings", ll);
    }

    @Override
    protected void readData(CompoundTag tag) {
        shownRisk = Math.max(0, Math.min(PlinkoLayout.RISKS - 1, tag.contains("shownRisk") ? Nbt.i(tag, "shownRisk") : 1));
        landSeq = Nbt.i(tag, "landSeq");
        // las bolas que ya estaban (cliente) conservan su estado de sonido
        List<Ball> old = new ArrayList<>(balls);
        balls.clear();
        ListTag bl = Nbt.list(tag, "balls");
        for (int i = 0; i < bl.size(); i++) {
            CompoundTag t = Nbt.at(bl, i);
            Ball b = new Ball();
            b.player = Nbt.hasUuid(t, "player") ? Nbt.uuid(t, "player") : null;
            b.name = Nbt.s(t, "name");
            b.start = Nbt.l(t, "start");
            b.path = Nbt.i(t, "path");
            b.risk = Nbt.i(t, "risk");
            b.bet = Nbt.i(t, "bet");
            for (Ball o : old) {
                if (o.start == b.start && o.path == b.path && java.util.Objects.equals(o.player, b.player)) {
                    b.lastHit = o.lastHit;
                }
            }
            balls.add(b);
        }
        landings.clear();
        ListTag ll = Nbt.list(tag, "landings");
        for (int i = 0; i < ll.size(); i++) {
            CompoundTag t = Nbt.at(ll, i);
            Landing l = new Landing();
            l.time = Nbt.l(t, "time");
            l.bucket = Nbt.i(t, "bucket");
            l.risk = Nbt.i(t, "risk");
            l.bet = Nbt.i(t, "bet");
            l.m10 = Nbt.i(t, "m10");
            l.payout = Nbt.i(t, "payout");
            landings.add(l);
        }
    }
}
