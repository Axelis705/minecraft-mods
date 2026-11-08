package com.pooltable.block;

import com.pooltable.game.HockeyAI;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;
import com.pooltable.net.HockeySyncPacket;
import com.pooltable.net.PoolNet;
import com.pooltable.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Random;
import java.util.UUID;

/**
 * La mesa de hockey de mesa (vive en la parte 0,0): la partida ({@link HockeyMatch}, con física y la máquina como rival), quién juega
 * (hasta 2 jugadores; el jugador A defiende el lado de x &lt; 0) y la red. Los jugadores mueven su mazo con el ratón desde una pantalla de vista
 * superior ({@code HockeyScreen}); el cliente manda dónde quiere el mazo y el servidor manda la posición del disco y los mazos cada tick.
 */
public class AirHockeyBE extends BlockEntity {
    public static final int A_CPU = 0;
    public static final int A_HOST = 1;
    public static final int A_JOIN = 2;
    public static final int A_LEAVE = 3;
    /** Altura de la superficie de juego sobre el bloque (0,8). */
    public static final double SURFACE_Y = 0.80;

    public final HockeyMatch match = new HockeyMatch(new Random());
    public final UUID[] pid = new UUID[2];
    public final String[] pname = {"", ""};

    // ---- cliente: si este jugador maneja un mazo con el ratón capturado, su mazo se dibuja donde él lo mueve (al instante)
    public static BlockPos localPos = null;
    public static int localRole = -1;
    public static double localX = 0;
    public static double localZ = 0;

    // ---- cliente (dibujo suave)
    public boolean clientInit = false;
    public long recvNanos = 0L;
    public double timer = 0;
    public double cpx;
    public double cpz;
    public double cvx;
    public double cvz;
    public final double[] cmx = {-0.9, 0.9};
    public final double[] cmz = {0.0, 0.0};
    /** Retraso de ida del servidor al cliente (segundos), lo pone el cliente antes de recibir cada estado. */
    public double netDelay = 0.0;
    /**
     * Predicción en el cliente: se calcula aquí mismo, con la misma física del servidor, el movimiento del disco y los mazos, así el mazo propio y el
     * disco se ven siempre coherentes (sin esperar a la red). Cada estado que llega del servidor corrige poco a poco las diferencias.
     */
    public final HockeyPhysics sim = new HockeyPhysics();
    private boolean simValid = false;
    private long simLast = 0L;
    /** Lo que se corrige al disco desde el servidor no se aplica de golpe: se guarda aquí y se va deshaciendo en ~0,1 s para que el movimiento no dé saltos. */
    public double offX = 0;
    public double offZ = 0;
    /** Tras un golpe mío se confía en lo calculado aquí hasta esta hora (el servidor todavía no ha visto mi golpe). */
    private long holdUntil = 0L;

    public AirHockeyBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_HOCKEY.get(), pos, state);
    }

    // ------------------------------------------------------------------ geometría

    public Direction facing() {
        return getBlockState().getValue(AirHockeyBlock.FACING);
    }

    /** El punto del suelo en el borde de la mesa (u = 0) y en su línea central (v = 0). */
    public Vec3 origin() {
        Direction f = facing();
        Direction r = f.getClockWise();
        return new Vec3(worldPosition.getX() + 0.5 - 0.5 * f.getStepX() + 0.5 * r.getStepX(), worldPosition.getY(),
                worldPosition.getZ() + 0.5 - 0.5 * f.getStepZ() + 0.5 * r.getStepZ());
    }

    /** El centro de la cancha en el mundo. */
    public Vec3 center() {
        Direction f = facing();
        Vec3 o = origin();
        return new Vec3(o.x + f.getStepX() * 1.5, o.y + SURFACE_Y, o.z + f.getStepZ() * 1.5);
    }

    public boolean isPlayer(UUID id) {
        return id != null && (id.equals(pid[0]) || id.equals(pid[1]));
    }

    public int roleOf(UUID id) {
        return id == null ? -1 : id.equals(pid[0]) ? 0 : id.equals(pid[1]) ? 1 : -1;
    }

    // ------------------------------------------------------------------ juego (servidor)

    private void broadcast(Component c, boolean actionBar) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 ctr = center();
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 24 * 24) {
                p.displayClientMessage(c, actionBar);
            }
        }
    }

    private void sound(net.minecraft.sounds.SoundEvent ev, double x, double z, float vol, float pitch) {
        Vec3 c = center();
        Direction f = facing();
        Vec3 r = new Vec3(f.getStepX(), 0, f.getStepZ());
        Vec3 side = new Vec3(-f.getStepZ(), 0, f.getStepX());
        level.playSound(null, c.x + r.x * x + side.x * z, c.y, c.z + r.z * x + side.z * z, ev, SoundSource.BLOCKS, vol, pitch);
    }

    /** Lleva al jugador a su extremo de la mesa, mirando hacia ella, para que desde fuera se vea quién juega. */
    private void placePlayer(ServerPlayer p, int role) {
        Direction f = facing();
        Direction look = role == 0 ? f : f.getOpposite();
        double d = HockeyPhysics.HL + 0.7;
        Vec3 c = center();
        double sign = role == 0 ? -1.0 : 1.0;
        double x = c.x + f.getStepX() * d * sign;
        double z = c.z + f.getStepZ() * d * sign;
        double y = worldPosition.getY();
        net.minecraft.core.BlockPos feet = net.minecraft.core.BlockPos.containing(x, y, z);
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return; // no cabe ahí: se queda donde está
        }
        p.connection.teleport(x, y, z, look.toYRot(), 20.0F);
    }

    /** Un chorro de partículas en un punto de la cancha (x a lo largo, z a lo ancho, en el marco de la mesa). */
    private void burst(net.minecraft.core.particles.ParticleOptions type, double x, double z, double up, int count, double spread, double speed) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 c = center();
        Direction f = facing();
        double wx = c.x + f.getStepX() * x - f.getStepZ() * z;
        double wz = c.z + f.getStepZ() * x + f.getStepX() * z;
        sl.sendParticles(type, wx, c.y + up, wz, count, spread, spread * 0.6, spread, speed);
    }

    private static String diffName(int d) {
        return d == 0 ? "facil" : d == 1 ? "normal" : "dificil";
    }

    private void endGame(Component why) {
        match.reset();
        pid[0] = null;
        pid[1] = null;
        pname[0] = "";
        pname[1] = "";
        if (why != null) {
            broadcast(why, false);
        }
        sendSync();
        sync();
    }

    public void menuAction(ServerPlayer p, int action, int arg) {
        UUID id = p.getUUID();
        switch (action) {
            case A_CPU -> {
                if (match.state != HockeyMatch.State.IDLE) {
                    p.displayClientMessage(Component.translatable("pooltable.hockey.busy"), true);
                    return;
                }
                int d = Math.max(0, Math.min(2, arg));
                pid[0] = id;
                pid[1] = null;
                pname[0] = p.getGameProfile().getName();
                pname[1] = Component.translatable("pooltable.hockey.cpu." + diffName(d)).getString();
                match.start(true, d);
                placePlayer(p, 0);
            }
            case A_HOST -> {
                if (match.state != HockeyMatch.State.IDLE) {
                    p.displayClientMessage(Component.translatable("pooltable.hockey.busy"), true);
                    return;
                }
                pid[0] = id;
                pid[1] = null;
                pname[0] = p.getGameProfile().getName();
                pname[1] = "";
                match.waitForRival();
                placePlayer(p, 0);
            }
            case A_JOIN -> {
                if (match.state != HockeyMatch.State.WAITING || id.equals(pid[0])) {
                    return;
                }
                pid[1] = id;
                pname[1] = p.getGameProfile().getName();
                match.start(false, 1);
                placePlayer(p, 1);
                broadcast(Component.translatable("pooltable.hockey.started", pname[0], pname[1]), false);
            }
            case A_LEAVE -> {
                int role = roleOf(id);
                if (role < 0) {
                    return;
                }
                boolean playing = match.state != HockeyMatch.State.WAITING && match.state != HockeyMatch.State.IDLE;
                endGame(playing && !match.vsCpu ? Component.translatable("pooltable.hockey.left", p.getGameProfile().getName()) : null);
                return;
            }
            default -> {
                return;
            }
        }
        sendSync();
        sync();
    }

    /** Dónde quiere el jugador su mazo (x a lo largo de la mesa, z a lo ancho, en el marco de la mesa). */
    public void setTarget(ServerPlayer p, double x, double z) {
        int role = roleOf(p.getUUID());
        if (role < 0 || Double.isNaN(x) || Double.isNaN(z) || Double.isInfinite(x) || Double.isInfinite(z)) {
            return;
        }
        match.ph.tx[role] = x;
        match.ph.tz[role] = z;
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl) || match.state == HockeyMatch.State.IDLE) {
            return;
        }
        // los jugadores tienen que seguir cerca y conectados
        Vec3 ctr = center();
        for (int i = 0; i < 2; i++) {
            if (pid[i] == null) {
                continue;
            }
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(pid[i]);
            if (p == null || p.isRemoved() || p.level() != level || p.distanceToSqr(ctr.x, ctr.y, ctr.z) > 12 * 12) {
                String n = pname[i];
                endGame(match.state == HockeyMatch.State.WAITING || match.vsCpu ? null : Component.translatable("pooltable.hockey.left", n));
                return;
            }
        }
        HockeyMatch.State before = match.state;
        match.tick(0.05);
        HockeyPhysics ph = match.ph;
        if (ph.mallethits > 0) {
            if (pid[ph.hitSide] != null) {
                ServerPlayer hitter = sl.getServer().getPlayerList().getPlayer(pid[ph.hitSide]);
                if (hitter != null) {
                    hitter.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                }
            }
            float v = (float) Math.min(1.0, 0.25 + ph.hitSpeed / 6.0);
            sound(SoundEvents.BAMBOO_WOOD_HIT, ph.px, ph.pz, v, 1.3F);
            burst(net.minecraft.core.particles.ParticleTypes.CRIT, ph.px, ph.pz, 0.06, 2 + (int) Math.min(3, ph.hitSpeed * 0.5), 0.03, 0.12);
        }
        if (ph.wallHits > 0) {
            sound(SoundEvents.WOOD_HIT, ph.px, ph.pz, 0.5F, 1.5F);
        }
        if (match.goalEvent != 0) {
            int g = match.goalEvent;
            String who = pname[g == 1 ? 0 : 1];
            sound(SoundEvents.PLAYER_LEVELUP, 0, 0, 0.8F, 1.2F);
            double gx = g == 1 ? HockeyPhysics.HL : -HockeyPhysics.HL; // la portería donde entró el disco
            burst(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, gx, 0, 0.15, 6, 0.15, 0.0);
            sound(SoundEvents.FIREWORK_ROCKET_TWINKLE, gx, 0, 0.7F, 1.0F);
            if (match.state == HockeyMatch.State.OVER) {
                broadcast(Component.translatable("pooltable.hockey.win", pname[match.winner], match.scoreA, match.scoreB), false);
            } else {
                broadcast(Component.translatable("pooltable.hockey.goal", who, match.scoreA, match.scoreB), true);
            }
            sync();
        }
        if (match.state == HockeyMatch.State.COUNTDOWN && before == HockeyMatch.State.COUNTDOWN) {
            int s = (int) Math.ceil(match.timer);
            int sp = (int) Math.ceil(match.timer + 0.05);
            if (s != sp && match.timer > 0.1) {
                sound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0, 0, 0.6F, 0.8F);
            }
        }
        if (before != match.state && match.state == HockeyMatch.State.IDLE) {
            // la partida terminó sola (pasó la pantalla final): se liberan los jugadores
            pid[0] = null;
            pid[1] = null;
            pname[0] = "";
            pname[1] = "";
            sync();
        }
        sendSync();
    }

    // ------------------------------------------------------------------ red y guardado

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void sendSync() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        HockeyPhysics ph = match.ph;
        PoolNet.sendToTracking(sl, worldPosition, new HockeySyncPacket(worldPosition, (byte) match.state.ordinal(), (byte) match.scoreA, (byte) match.scoreB,
                (float) match.timer, (float) ph.px, (float) ph.pz, (float) ph.pvx, (float) ph.pvz,
                (float) ph.mx[0], (float) ph.mz[0], (float) ph.mx[1], (float) ph.mz[1]));
    }

    /** Lo que recibe el cliente en cada tick. */
    public void clientReceive(byte state, byte sa, byte sb, float timer, float px, float pz, float vx, float vz, float ax, float az, float bx, float bz) {
        match.state = HockeyMatch.State.values()[Math.max(0, Math.min(HockeyMatch.State.values().length - 1, state))];
        match.scoreA = sa;
        match.scoreB = sb;
        this.timer = timer;
        cpx = px;
        cpz = pz;
        cvx = vx;
        cvz = vz;
        cmx[0] = ax;
        cmz[0] = az;
        cmx[1] = bx;
        cmz[1] = bz;
        recvNanos = System.nanoTime();
        clientInit = true;
        reconcile();
    }

    /** El estado del servidor ya tiene un retraso de red: se adelanta ese tiempo antes de comparar con lo que se calculó aquí. */
    private void reconcile() {
        if (match.state != HockeyMatch.State.PLAYING || !simValid) {
            simValid = false;
            offX = 0;
            offZ = 0;
            return;
        }
        double d = Math.max(0.0, Math.min(0.2, netDelay));
        double lim = HockeyPhysics.HL + HockeyPhysics.PUCK_R;
        double sx = Math.max(-lim, Math.min(lim, cpx + cvx * d));
        double sz = Math.max(-HockeyPhysics.HW + HockeyPhysics.PUCK_R, Math.min(HockeyPhysics.HW - HockeyPhysics.PUCK_R, cpz + cvz * d));
        double ex = sx - sim.px;
        double ez = sz - sim.pz;
        double err = Math.hypot(ex, ez);
        boolean holding = System.nanoTime() < holdUntil;
        double k;
        if (err > 0.9 || (!holding && err > 0.45)) {
            k = 1.0;
        } else if (holding) {
            // acabo de golpear: el servidor aún no sabe de mi golpe, así que su disco va "atrasado"; se confía en el cálculo propio
            return;
        } else {
            k = 0.35;
        }
        sim.px += ex * k;
        sim.pz += ez * k;
        // el cambio de posición se reparte en el tiempo: lo que se ve no salta
        offX -= ex * k;
        offZ -= ez * k;
        sim.pvx += (cvx - sim.pvx) * (k >= 1.0 ? 1.0 : 0.5);
        sim.pvz += (cvz - sim.pvz) * (k >= 1.0 ? 1.0 : 0.5);
    }

    /** ¿Este jugador maneja ahora el mazo {@code s} de esta mesa con el ratón? */
    private boolean mine(int s) {
        return localRole == s && worldPosition.equals(localPos);
    }

    /** Avanza la predicción del cliente hasta este momento (se llama al dibujar). */
    public void simUpdate() {
        long now = System.nanoTime();
        double dt = simLast == 0L ? 0.0 : Math.min(0.05, (now - simLast) / 1.0e9);
        simLast = now;
        if (!clientInit) {
            return;
        }
        boolean playing = match.state == HockeyMatch.State.PLAYING;
        if (!playing || !simValid) {
            // sin juego (cuenta atrás, gol, espera) todo se ve tal como lo manda el servidor
            sim.px = cpx;
            sim.pz = cpz;
            sim.pvx = playing ? cvx : 0;
            sim.pvz = playing ? cvz : 0;
            for (int s = 0; s < 2; s++) {
                double x = mine(s) ? localX : cmx[s];
                double z = mine(s) ? localZ : cmz[s];
                sim.putMallet(s, x, z);
            }
            simValid = playing;
            offX = 0;
            offZ = 0;
            return;
        }
        for (int s = 0; s < 2; s++) {
            sim.tx[s] = mine(s) ? localX : cmx[s];
            sim.tz[s] = mine(s) ? localZ : cmz[s];
            // el mazo propio sigue al ratón con el mismo límite de velocidad que tiene en el servidor
            if (mine(s)) {
                sim.maxSpeed[s] = HockeyMatch.HUMAN_SPEED;
            } else {
                // el rival llega suave a donde dice el servidor (en unos 60 ms) en vez de dar saltos cada tick
                double dd = Math.hypot(sim.tx[s] - sim.mx[s], sim.tz[s] - sim.mz[s]);
                sim.maxSpeed[s] = Math.max(0.5, Math.min(30.0, dd / 0.06));
            }
        }
        sim.step(dt, true);
        if (sim.mallethits > 0 && mine(sim.hitSide)) {
            holdUntil = now + 300_000_000L;
        }
        double decay = Math.exp(-dt / 0.1);
        offX *= decay;
        offZ *= decay;
        if (sim.goal != 0) {
            // metió gol (en el cliente): el disco se queda en la portería hasta que el servidor confirme
            sim.goal = 0;
            sim.pvx = 0;
            sim.pvz = 0;
            sim.px = Math.max(-HockeyPhysics.HL - 0.12, Math.min(HockeyPhysics.HL + 0.12, sim.px));
        }
        sim.clearEvents();
    }

    /** Posición del disco para dibujar: la última recibida más lo que avanzó desde entonces (sin pasar de las bandas). */
    public double[] puckNow() {
        double dt = Math.min(0.08, (System.nanoTime() - recvNanos) / 1.0e9);
        double moving = match.state == HockeyMatch.State.PLAYING ? 1.0 : 0.0;
        double x = cpx + cvx * dt * moving;
        double z = cpz + cvz * dt * moving;
        double lim = HockeyPhysics.HL + HockeyPhysics.PUCK_R;
        x = Math.max(-lim, Math.min(lim, x));
        z = Math.max(-HockeyPhysics.HW + HockeyPhysics.PUCK_R, Math.min(HockeyPhysics.HW - HockeyPhysics.PUCK_R, z));
        return new double[] {x, z};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putByte("state", (byte) match.state.ordinal());
        tag.putBoolean("cpu", match.vsCpu);
        tag.putInt("diff", match.diff);
        tag.putInt("sa", match.scoreA);
        tag.putInt("sb", match.scoreB);
        for (int i = 0; i < 2; i++) {
            if (pid[i] != null) {
                tag.putUUID("id" + i, pid[i]);
            }
            tag.putString("n" + i, pname[i]);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < 2; i++) {
            pid[i] = tag.hasUUID("id" + i) ? tag.getUUID("id" + i) : null;
            pname[i] = tag.getString("n" + i);
        }
        match.vsCpu = tag.getBoolean("cpu");
        match.diff = tag.getInt("diff");
        match.state = HockeyMatch.State.values()[Math.max(0, Math.min(HockeyMatch.State.values().length - 1, tag.getByte("state")))];
        match.scoreA = tag.getInt("sa");
        match.scoreB = tag.getInt("sb");
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // al cargar el mundo no se retoma una partida a medias: la mesa queda libre
            for (int i = 0; i < 2; i++) {
                pid[i] = null;
                pname[i] = "";
            }
            match.reset();
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
        return new AABB(worldPosition).inflate(4.0, 2.0, 4.0);
    }
}
