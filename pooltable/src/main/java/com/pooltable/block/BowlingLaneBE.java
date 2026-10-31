package com.pooltable.block;

import com.pooltable.game.BowlingGame;
import com.pooltable.game.BowlingPhysics;
import com.pooltable.game.BowlingScore;
import com.pooltable.net.BowlingSyncPacket;
import com.pooltable.net.PoolNet;
import com.pooltable.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * El corazón de la pista (vive en la parte 0,0): los 10 pinos y la bola con su física ({@link BowlingPhysics}), quién juega (de 1 a 4 jugadores)
 * y el marcador ({@link BowlingGame}). El servidor manda; el cliente recibe las posiciones cada tick mientras algo se mueve.
 * Sin partida en curso la pista es de práctica libre: cualquiera tira y los pinos se rearman solos.
 */
public class BowlingLaneBE extends BlockEntity {
    public static final int A_JOIN = 0;
    public static final int A_LEAVE = 1;
    public static final int A_START = 2;
    public static final int A_STOP = 3;
    /** Alto de la superficie de la pista sobre el bloque (2/16). */
    public static final double SURFACE_Y = 2.0 / 16.0;
    public static final double MAX_SPEED = 8.0;
    public static final double MIN_SPEED = 3.5;
    private static final int SETTLE_TICKS = 40;
    private static final int MAX_ROLL_TICKS = 20 * 25;

    public final BowlingPhysics ph = new BowlingPhysics();
    private final Random rnd = new Random();

    public boolean playing = false;
    public final List<UUID> pids = new ArrayList<>();
    public final List<String> pnames = new ArrayList<>();
    public BowlingGame game = null;

    // ---- servidor
    /** 0 = esperando un tiro, 1 = la bola rueda, 2 = esperando para rearmar los pinos. */
    private int phase = 0;
    private int wait = 0;
    private int rollTick = 0;
    private int standingBefore = 10;
    private boolean freshBefore = true;
    private boolean needsRerack = false;

    // ---- cliente (dibujo suave)
    public static final Set<BowlingLaneBE> CLIENT_LANES = ConcurrentHashMap.newKeySet();
    public boolean clientInit = false;
    public boolean clientMoving = false;
    public long recvNanos = 0L;
    public float pbu;
    public float pbv;
    public float cbu;
    public float cbv;
    public boolean cBallActive = false;
    public final float[] ppu = new float[BowlingPhysics.PINS];
    public final float[] ppv = new float[BowlingPhysics.PINS];
    public final float[] cpu = new float[BowlingPhysics.PINS];
    public final float[] cpv = new float[BowlingPhysics.PINS];
    public final byte[] cstate = new byte[BowlingPhysics.PINS];
    public final float[] heading = new float[BowlingPhysics.PINS];
    public final long[] fallStart = new long[BowlingPhysics.PINS];

    public BowlingLaneBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOWLING_LANE.get(), pos, state);
        for (int i = 0; i < BowlingPhysics.PINS; i++) {
            cpu[i] = ppu[i] = (float) BowlingPhysics.spotU(i);
            cpv[i] = ppv[i] = (float) BowlingPhysics.spotV(i);
            cstate[i] = BowlingPhysics.STANDING;
        }
    }

    // ------------------------------------------------------------------ geometría

    public Direction facing() {
        return getBlockState().getValue(BowlingLaneBlock.FACING);
    }

    /** El punto del suelo en el borde de salida de la pista (u = 0) y en su línea central (v = 0). */
    public Vec3 origin() {
        Direction f = facing();
        Direction r = f.getClockWise();
        return new Vec3(worldPosition.getX() + 0.5 - 0.5 * f.getStepX() + 0.5 * r.getStepX(),
                worldPosition.getY() + SURFACE_Y,
                worldPosition.getZ() + 0.5 - 0.5 * f.getStepZ() + 0.5 * r.getStepZ());
    }

    public Vec3 worldAt(double u, double v) {
        Direction f = facing();
        Direction r = f.getClockWise();
        Vec3 o = origin();
        return new Vec3(o.x + f.getStepX() * u + r.getStepX() * v, o.y, o.z + f.getStepZ() * u + r.getStepZ() * v);
    }

    /** {u, v, altura sobre la pista} de un punto del mundo. */
    public double[] toLane(Vec3 p) {
        Direction f = facing();
        Direction r = f.getClockWise();
        Vec3 o = origin();
        double dx = p.x - o.x;
        double dz = p.z - o.z;
        return new double[] {dx * f.getStepX() + dz * f.getStepZ(), dx * r.getStepX() + dz * r.getStepZ(), p.y - o.y};
    }

    public Vec3 center() {
        return worldAt(7.0, 0.0);
    }

    /** La pista sobre la que está parado el jugador (en el bloque de sus pies o en el de abajo), o null. */
    public static BowlingLaneBE findUnder(Level level, Vec3 p) {
        BlockPos feet = BlockPos.containing(p);
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos q = feet.offset(0, dy, 0);
            BlockState st = level.getBlockState(q);
            if (st.getBlock() instanceof BowlingLaneBlock) {
                BlockPos m = BowlingLaneBlock.masterOf(q, st);
                if (level.getBlockEntity(m) instanceof BowlingLaneBE be) {
                    return be;
                }
            }
        }
        return null;
    }

    /** Cliente: la pista con partida más cercana al punto (dentro del alcance), o null. */
    public static BowlingLaneBE nearestClient(Level level, Vec3 p, double reach) {
        BowlingLaneBE best = null;
        double bd = reach;
        for (BowlingLaneBE be : CLIENT_LANES) {
            if (be.isRemoved() || be.getLevel() != level) {
                continue;
            }
            Vec3 c = be.center();
            double d = Math.sqrt((c.x - p.x) * (c.x - p.x) + (c.z - p.z) * (c.z - p.z));
            if (d <= bd) {
                bd = d;
                best = be;
            }
        }
        return best;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_LANES.add(this);
        } else {
            needsRerack = true;
        }
    }

    @Override
    public void setRemoved() {
        CLIENT_LANES.remove(this);
        super.setRemoved();
    }

    // ------------------------------------------------------------------ juego (servidor)

    private void broadcast(Component c, boolean actionBar) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 ctr = center();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 28 * 28) {
                p.displayClientMessage(c, actionBar);
            }
        }
    }

    private String currentName() {
        return playing && game != null && !game.over && game.current < pnames.size() ? pnames.get(game.current) : "";
    }

    /** Un lanzamiento. power: de 0 a 1 según cuánto cargó. */
    public void tryThrow(ServerPlayer sp, double power) {
        if (phase != 0) {
            sp.displayClientMessage(Component.translatable("pooltable.bowling.busy"), true);
            return;
        }
        double[] lv = toLane(sp.position());
        if (lv[0] > BowlingPhysics.FOUL_U + 0.45) {
            sp.displayClientMessage(Component.translatable("pooltable.bowling.foul_line"), true);
            return;
        }
        if (playing) {
            if (game == null || game.over) {
                sp.displayClientMessage(Component.translatable("pooltable.bowling.game_over_hint"), true);
                return;
            }
            if (!sp.getUUID().equals(pids.get(game.current))) {
                sp.displayClientMessage(Component.translatable("pooltable.bowling.not_your_turn", currentName()), true);
                return;
            }
        }
        standingBefore = ph.standingCount();
        freshBefore = !playing || game.freshRackForNext();
        double v = Mth.clamp(lv[1], -0.55, 0.55);
        float rel = Mth.wrapDegrees(sp.getYRot() - facing().toYRot());
        double angle = Mth.clamp(rel, -10.0F, 10.0F);
        double speed = MIN_SPEED + (MAX_SPEED - MIN_SPEED) * power;
        // el efecto sale de la tecla de lado (A o D) al soltar; al usar un objeto el movimiento se frena, por eso el factor
        double spin = Mth.clamp(-sp.xxa * 5.0, -1.0, 1.0);
        ph.launch(v, angle, speed, spin);
        phase = 1;
        rollTick = 0;
        wait = 0;
        Vec3 w = worldAt(BowlingPhysics.FOUL_U, v);
        level.playSound(null, w.x, w.y, w.z, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.7F, 0.6F + 0.3F * (float) power);
        sendMoveSync();
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        if (needsRerack) {
            needsRerack = false;
            phase = 0;
            rerack();
        }
        if (phase == 1) {
            int pinHits0 = ph.pinHits;
            for (int i = 0; i < 12; i++) {
                ph.step(1.0 / 240.0);
            }
            rollTick++;
            if (ph.ballActive && !ph.inGutter && rollTick % 3 == 0 && ph.bu < BowlingPhysics.HEAD_U - 0.5) {
                Vec3 w = worldAt(ph.bu, ph.bv);
                level.playSound(null, w.x, w.y, w.z, SoundEvents.GRAVEL_STEP, SoundSource.BLOCKS, 0.35F, 0.55F);
            }
            int knocked = Math.min(4, ph.pinHits - pinHits0);
            if (knocked > 0) {
                Vec3 w = worldAt(BowlingPhysics.HEAD_U + 0.5, 0.0);
                for (int i = 0; i < knocked; i++) {
                    level.playSound(null, w.x, w.y, w.z, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 1.0F, 0.7F + rnd.nextFloat() * 0.7F);
                }
            }
            sendMoveSync();
            if (ph.settled() || rollTick > MAX_ROLL_TICKS) {
                finishThrow();
            }
        } else if (phase == 2) {
            if (--wait <= 0) {
                phase = 0;
                rerack();
            }
        }
    }

    private void finishThrow() {
        ph.ballActive = false;
        int down = Math.max(0, standingBefore - ph.standingCount());
        boolean strike = freshBefore && down == standingBefore && standingBefore == 10;
        boolean spare = !freshBefore && down == standingBefore && standingBefore > 0;
        String who = playing && game != null && !game.over ? currentName() : "";
        if (playing && game != null && !game.over) {
            game.record(down);
        }
        Component what;
        if (strike) {
            what = Component.translatable("pooltable.bowling.strike");
        } else if (spare) {
            what = Component.translatable("pooltable.bowling.spare");
        } else if (down == 0) {
            what = Component.translatable(ph.inGutter ? "pooltable.bowling.gutter" : "pooltable.bowling.miss");
        } else {
            what = Component.translatable("pooltable.bowling.pins", down);
        }
        broadcast(who.isEmpty() ? what : Component.translatable("pooltable.bowling.who_what", who, what), true);
        if (strike || spare) {
            Vec3 w = worldAt(BowlingPhysics.HEAD_U, 0.0);
            level.playSound(null, w.x, w.y, w.z, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, strike ? 1.4F : 1.1F);
        }
        if (playing && game != null) {
            if (game.over) {
                int w = game.winner();
                broadcast(Component.translatable("pooltable.bowling.win", pnames.get(w), game.scores[w].total()), false);
            } else {
                broadcast(Component.translatable("pooltable.bowling.turn", currentName()), false);
            }
        }
        phase = 2;
        wait = SETTLE_TICKS;
        sendMoveSync();
        sync();
    }

    /** Barre los pinos caídos y deja en pie los que tocan: los diez si empieza turno o no hay partida. */
    private void rerack() {
        boolean fresh = !playing || game == null || game.over || game.freshRackForNext();
        ph.reset(fresh ? 0x3FF : ph.standingMask());
        sendMoveSync();
        sync();
    }

    public void menuAction(ServerPlayer p, int action) {
        UUID id = p.getUUID();
        switch (action) {
            case A_JOIN -> {
                if (playing && game != null && !game.over) {
                    p.displayClientMessage(Component.translatable("pooltable.bowling.in_progress"), true);
                    return;
                }
                if (playing) { // la partida anterior ya terminó: se arma una sala nueva
                    playing = false;
                    game = null;
                    pids.clear();
                    pnames.clear();
                }
                if (pids.contains(id)) {
                    return;
                }
                if (pids.size() >= BowlingGame.MAX_PLAYERS) {
                    p.displayClientMessage(Component.translatable("pooltable.bowling.full"), true);
                    return;
                }
                pids.add(id);
                pnames.add(p.getGameProfile().getName());
            }
            case A_LEAVE -> {
                if (playing && game != null && !game.over) {
                    p.displayClientMessage(Component.translatable("pooltable.bowling.in_progress"), true);
                    return;
                }
                int i = pids.indexOf(id);
                if (i >= 0) {
                    pids.remove(i);
                    pnames.remove(i);
                }
            }
            case A_START -> {
                if (pids.isEmpty() || phase == 1 || (playing && game != null && !game.over)) {
                    return;
                }
                game = new BowlingGame(pids.size());
                playing = true;
                phase = 0;
                rerack();
                broadcast(Component.translatable("pooltable.bowling.started", currentName()), false);
            }
            case A_STOP -> {
                playing = false;
                game = null;
                pids.clear();
                pnames.clear();
                phase = 0;
                ph.reset(0x3FF);
                sendMoveSync();
                broadcast(Component.translatable("pooltable.bowling.stopped", p.getGameProfile().getName()), false);
            }
            default -> {
                return;
            }
        }
        sync();
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void sendMoveSync() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        int n = BowlingPhysics.PINS;
        float[] u = new float[n];
        float[] v = new float[n];
        byte[] st = new byte[n];
        for (int i = 0; i < n; i++) {
            u[i] = (float) ph.pu[i];
            v[i] = (float) ph.pv[i];
            st[i] = ph.state[i];
        }
        PoolNet.sendToTracking(sl, worldPosition, new BowlingSyncPacket(worldPosition, (float) ph.bu, (float) ph.bv, ph.ballActive, u, v, st, phase != 0));
    }

    /** Lo que recibe el cliente en cada tick de movimiento. */
    public void clientReceive(float bu, float bv, boolean ballActive, float[] u, float[] v, byte[] st, boolean moving) {
        int n = BowlingPhysics.PINS;
        boolean had = clientInit;
        long now = System.nanoTime();
        if (!had) {
            pbu = bu;
            pbv = bv;
            for (int i = 0; i < n; i++) {
                ppu[i] = u[i];
                ppv[i] = v[i];
            }
            clientInit = true;
        } else {
            pbu = cbu;
            pbv = cbv;
            System.arraycopy(cpu, 0, ppu, 0, n);
            System.arraycopy(cpv, 0, ppv, 0, n);
        }
        if (ballActive && !cBallActive) {
            pbu = bu; // la bola aparece directamente en la línea de salida
            pbv = bv;
        }
        cbu = bu;
        cbv = bv;
        cBallActive = ballActive;
        for (int i = 0; i < n; i++) {
            byte old = cstate[i];
            if (had && old == BowlingPhysics.STANDING && st[i] == BowlingPhysics.DOWN) {
                fallStart[i] = now;
                float hu = u[i] - ppu[i];
                float hv = v[i] - ppv[i];
                if (hu * hu + hv * hv < 1.0e-6F) {
                    hu = u[i] - bu;
                    hv = v[i] - bv;
                }
                heading[i] = hu * hu + hv * hv < 1.0e-8F ? 0.0F : (float) Math.atan2(hv, hu);
            }
            if (st[i] == BowlingPhysics.STANDING && old != BowlingPhysics.STANDING) {
                ppu[i] = u[i]; // pino rearmado: aparece sin deslizarse
                ppv[i] = v[i];
            }
            cstate[i] = st[i];
            cpu[i] = u[i];
            cpv[i] = v[i];
        }
        clientMoving = moving;
        recvNanos = now;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("playing", playing);
        ListTag pl = new ListTag();
        for (int i = 0; i < pids.size(); i++) {
            CompoundTag c = new CompoundTag();
            c.putUUID("id", pids.get(i));
            c.putString("name", pnames.get(i));
            if (game != null && i < game.scores.length) {
                List<Integer> th = game.scores[i].throwsList();
                int[] arr = new int[th.size()];
                for (int k = 0; k < arr.length; k++) {
                    arr[k] = th.get(k);
                }
                c.putIntArray("th", arr);
            }
            pl.add(c);
        }
        tag.put("players", pl);
        if (game != null) {
            tag.putBoolean("hasGame", true);
            tag.putInt("cur", game.current);
            tag.putBoolean("over", game.over);
        }
        tag.putByteArray("ps", ph.state.clone());
        ListTag lu = new ListTag();
        ListTag lv = new ListTag();
        for (int i = 0; i < BowlingPhysics.PINS; i++) {
            lu.add(FloatTag.valueOf((float) ph.pu[i]));
            lv.add(FloatTag.valueOf((float) ph.pv[i]));
        }
        tag.put("pu", lu);
        tag.put("pv", lv);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        playing = tag.getBoolean("playing");
        pids.clear();
        pnames.clear();
        ListTag pl = tag.getList("players", Tag.TAG_COMPOUND);
        List<int[]> throwsOf = new ArrayList<>();
        for (int i = 0; i < pl.size(); i++) {
            CompoundTag c = pl.getCompound(i);
            pids.add(c.getUUID("id"));
            pnames.add(c.getString("name"));
            throwsOf.add(c.getIntArray("th"));
        }
        game = null;
        if (tag.getBoolean("hasGame") && !pids.isEmpty()) {
            game = new BowlingGame(pids.size());
            for (int i = 0; i < pids.size(); i++) {
                game.scores[i] = BowlingScore.of(throwsOf.get(i));
            }
            game.current = Math.min(tag.getInt("cur"), pids.size() - 1);
            game.over = tag.getBoolean("over");
        }
        byte[] ps = tag.getByteArray("ps");
        ListTag lu = tag.getList("pu", Tag.TAG_FLOAT);
        ListTag lv = tag.getList("pv", Tag.TAG_FLOAT);
        if (ps.length == BowlingPhysics.PINS && lu.size() == BowlingPhysics.PINS && lv.size() == BowlingPhysics.PINS) {
            for (int i = 0; i < BowlingPhysics.PINS; i++) {
                ph.state[i] = ps[i];
                ph.pu[i] = lu.getFloat(i);
                ph.pv[i] = lv.getFloat(i);
            }
            if (level != null && level.isClientSide && !clientMoving) {
                for (int i = 0; i < BowlingPhysics.PINS; i++) {
                    byte old = cstate[i];
                    cstate[i] = ps[i];
                    cpu[i] = ppu[i] = lu.getFloat(i);
                    cpv[i] = ppv[i] = lv.getFloat(i);
                    if (old == BowlingPhysics.STANDING && ps[i] == BowlingPhysics.DOWN && fallStart[i] == 0L) {
                        fallStart[i] = 1L;
                    }
                }
                clientInit = true;
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag t = new CompoundTag();
        saveAdditional(t);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(15.0, 3.0, 15.0);
    }
}
