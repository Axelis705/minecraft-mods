package com.pooltable.block;

import com.pooltable.game.VolleyMatch;
import com.pooltable.net.PoolNet;
import com.pooltable.net.VolleySyncPacket;
import com.pooltable.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * La cancha de voleibol. Servidor: la sala de espera (quién está en qué equipo, tamaño y nivel de los bots), la partida ({@link VolleyMatch}) y la copia de la
 * posición real de los jugadores a la cancha. Cliente: lo último que mandó el servidor (pelota, jugadores, marcador) para dibujarlo.
 * Marco de la cancha: u (x) a lo largo, hacia donde mira el bloque; v (z) a su derecha; y sobre el suelo (la cara de arriba del terreno donde está el bloque).
 */
public class VolleyCourtBE extends BlockEntity {
    public static final int A_JOIN = 0;
    public static final int A_LEAVE = 1;
    public static final int A_SIZE = 2;
    public static final int A_DIFF = 3;
    public static final int A_START = 4;
    public static final int A_STOP = 5;

    public static final int FREE = 0;
    public static final int LOBBY = 1;
    public static final int PLAYING = 2;
    public static final int MAX_TEAM = 3;
    private static final String[] BOT_NAMES = {"Coco", "Nube", "Rayo", "Lola", "Nova", "Kiko", "Pipa", "Mango", "Sol", "Luna", "Tito", "Brisa"};

    /** Quien está en la sala o en la partida. */
    public static final class Member {
        public final UUID id;
        public final String name;
        public int team;

        public Member(UUID id, String name, int team) {
            this.id = id;
            this.name = name;
            this.team = team;
        }
    }

    // ---- servidor
    public final VolleyMatch match = new VolleyMatch(new Random());
    public final List<Member> members = new ArrayList<>();
    private final Map<UUID, Integer> idxOf = new HashMap<>();
    private final Map<UUID, Long> diveAt = new HashMap<>();
    private boolean announcedEnd = false;

    // ---- común (se guarda y viaja en la etiqueta de actualización)
    public int phase = FREE;
    public int sizeSetting = 0;
    public int diffSetting = 1;
    public final List<String> lobbyNames = new ArrayList<>();
    public final List<Integer> lobbyTeams = new ArrayList<>();

    // ---- cliente
    public boolean clientInit = false;
    public long recvNanos = 0L;
    public int cState;
    public int cScoreA;
    public int cScoreB;
    public int cServing;
    public int cTouches;
    public int cTouchTeam = -1;
    public int cN = 2;
    public int cDiff = 1;
    public int cServerIdx = -1;
    public float cTimer;
    public final float[] cBall = new float[6];
    public int cCount;
    public final int[] cTeam = new int[6];
    public final int[] cFlags = new int[6];
    public final int[] cAct = new int[6];
    public final int[] cSwing = new int[6];
    public final int[] cEid = new int[6];
    public final float[] cX = new float[6];
    public final float[] cZ = new float[6];
    public final float[] cY = new float[6];
    /** Cartel de la jugada (clave de texto) y cuándo salió. */
    public String popup = "";
    public long popupAt = 0L;

    public VolleyCourtBE(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VOLLEY_COURT.get(), pos, state);
    }

    // ------------------------------------------------------------------ geometría

    public Direction facing() {
        return getBlockState().getValue(VolleyCourtBlock.FACING);
    }

    /** Punto del mundo (u, altura, v) de la cancha. */
    public Vec3 world(double u, double y, double v) {
        Direction f = facing();
        Direction r = f.getClockWise();
        return new Vec3(worldPosition.getX() + 0.5 + f.getStepX() * u + r.getStepX() * v, worldPosition.getY() + y,
                worldPosition.getZ() + 0.5 + f.getStepZ() * u + r.getStepZ() * v);
    }

    /** Tamaño por equipo que se dibuja y se juega: el de la partida, o el que saldría con la sala actual. */
    public int courtN() {
        if (level != null && level.isClientSide) {
            if (clientInit && cState != VolleyMatch.State.IDLE.ordinal()) {
                return cN;
            }
        } else if (phase == PLAYING) {
            return match.n;
        }
        return previewN();
    }

    public int previewN() {
        int a = 0;
        int b = 0;
        for (int t : lobbyTeams) {
            if (t == 0) {
                a++;
            } else {
                b++;
            }
        }
        int most = Math.max(a, b);
        if (sizeSetting == 0) {
            return phase == FREE ? 2 : Math.max(1, most);
        }
        return Math.min(3, Math.max(sizeSetting, most));
    }

    public Member member(UUID id) {
        for (Member m : members) {
            if (m.id.equals(id)) {
                return m;
            }
        }
        return null;
    }

    public int teamOf(UUID id) {
        Member m = member(id);
        return m == null ? -1 : m.team;
    }

    // ------------------------------------------------------------------ sala

    private void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args), true);
    }

    private void broadcast(Component c) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 ctr = world(0, 1, 0);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(ctr.x, ctr.y, ctr.z) <= 40 * 40) {
                p.displayClientMessage(c, false);
            }
        }
    }

    private void refreshLobbyLists() {
        lobbyNames.clear();
        lobbyTeams.clear();
        for (Member m : members) {
            lobbyNames.add(m.name);
            lobbyTeams.add(m.team);
        }
        sync();
    }

    public void menuAction(ServerPlayer p, int action, int arg) {
        UUID id = p.getUUID();
        Member me = member(id);
        switch (action) {
            case A_JOIN -> {
                if (phase == PLAYING) {
                    say(p, "pooltable.voley.busy");
                    return;
                }
                int team = arg == 1 ? 1 : 0;
                int count = 0;
                for (Member m : members) {
                    if (m.team == team && m != me) {
                        count++;
                    }
                }
                if (count >= MAX_TEAM) {
                    say(p, "pooltable.voley.full");
                    return;
                }
                if (me == null) {
                    members.add(new Member(id, p.getGameProfile().getName(), team));
                } else {
                    me.team = team;
                }
                phase = LOBBY;
                refreshLobbyLists();
            }
            case A_LEAVE -> {
                if (me == null) {
                    return;
                }
                leave(p);
            }
            case A_SIZE -> {
                if (me != null && phase != PLAYING) {
                    sizeSetting = Math.max(0, Math.min(3, arg));
                    sync();
                }
            }
            case A_DIFF -> {
                if (me != null && phase != PLAYING) {
                    diffSetting = Math.max(0, Math.min(2, arg));
                    sync();
                }
            }
            case A_START -> {
                if (me != null && phase == LOBBY) {
                    startMatch();
                }
            }
            case A_STOP -> {
                if (me != null && phase == PLAYING) {
                    endMatch(Component.translatable("pooltable.voley.stopped", p.getGameProfile().getName()));
                }
            }
            default -> {
            }
        }
    }

    private void leave(ServerPlayer p) {
        Member me = member(p.getUUID());
        if (me == null) {
            return;
        }
        if (phase == PLAYING) {
            replaceWithBot(me);
            members.remove(me);
            idxOf.remove(p.getUUID());
            if (members.isEmpty()) {
                endMatch(null);
                return;
            }
            refreshLobbyLists();
        } else {
            members.remove(me);
            if (members.isEmpty()) {
                phase = FREE;
            }
            refreshLobbyLists();
        }
    }

    private void replaceWithBot(Member me) {
        Integer idx = idxOf.get(me.id);
        if (idx == null || idx >= match.players.size()) {
            return;
        }
        VolleyMatch.P old = match.players.get(idx);
        VolleyMatch.P bot = new VolleyMatch.P(old.team, old.slot, false);
        bot.name = BOT_NAMES[match.rnd.nextInt(BOT_NAMES.length)];
        bot.x = old.x;
        bot.z = old.z;
        bot.y = old.y;
        bot.homeX = old.homeX;
        bot.homeZ = old.homeZ;
        bot.reachScale = com.pooltable.game.VolleyAI.REACH_SCALE[match.diff];
        match.players.set(idx, bot);
    }

    private void startMatch() {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        int a = 0;
        int b = 0;
        for (Member m : members) {
            if (m.team == 0) {
                a++;
            } else {
                b++;
            }
        }
        int n = sizeSetting == 0 ? Math.max(1, Math.max(a, b)) : Math.max(sizeSetting, Math.max(a, b));
        n = Math.min(3, n);
        match.reset();
        idxOf.clear();
        List<String> names = new ArrayList<>(List.of(BOT_NAMES));
        java.util.Collections.shuffle(names, match.rnd);
        int bot = 0;
        for (int team = 0; team < 2; team++) {
            int placed = 0;
            for (Member m : members) {
                if (m.team == team) {
                    int idx = match.addPlayer(team, true, m.name);
                    idxOf.put(m.id, idx);
                    placed++;
                }
            }
            while (placed < n) {
                match.addPlayer(team, false, names.get(bot++ % names.size()));
                placed++;
            }
        }
        injectPositions();
        match.start(n, diffSetting);
        phase = PLAYING;
        announcedEnd = false;
        // todos los humanos a su lugar de inicio, mirando a la red
        if (level instanceof ServerLevel sl) {
            for (Member m : members) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(m.id);
                Integer idx = idxOf.get(m.id);
                if (sp != null && idx != null) {
                    VolleyMatch.P p = match.players.get(idx);
                    placeHuman(sp, p.team, p.homeX, p.homeZ);
                }
            }
        }
        broadcast(Component.translatable("pooltable.voley.started", n, n));
        refreshLobbyLists();
        sendSync();
        match.clearEvents();
    }

    private void endMatch(Component why) {
        phase = FREE;
        members.clear();
        idxOf.clear();
        match.reset();
        announcedEnd = false;
        if (why != null) {
            broadcast(why);
        }
        refreshLobbyLists();
        sendSync();
    }

    // ------------------------------------------------------------------ juego (servidor)

    /** Lleva a un humano a un punto de la cancha, mirando a la red. */
    private void placeHuman(ServerPlayer sp, int team, double u, double v) {
        Vec3 w = world(u, 0, v);
        BlockPos feet = BlockPos.containing(w.x, w.y, w.z);
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return;
        }
        Direction look = team == 0 ? facing() : facing().getOpposite();
        sp.connection.teleport(w.x, w.y, w.z, look.toYRot(), 8.0F);
    }

    /** Copia a la partida dónde están de verdad los humanos (en coordenadas de la cancha). */
    private void injectPositions() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Direction f = facing();
        Direction r = f.getClockWise();
        double cx = worldPosition.getX() + 0.5;
        double cz = worldPosition.getZ() + 0.5;
        for (Member m : members) {
            Integer idx = idxOf.get(m.id);
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(m.id);
            if (idx == null || sp == null || idx >= match.players.size()) {
                continue;
            }
            VolleyMatch.P p = match.players.get(idx);
            if (!p.human) {
                continue;
            }
            double dx = sp.getX() - cx;
            double dz = sp.getZ() - cz;
            p.x = dx * f.getStepX() + dz * f.getStepZ();
            p.z = dx * r.getStepX() + dz * r.getStepZ();
            p.y = Math.max(0.0, sp.getY() - worldPosition.getY());
            Vec3 mv = sp.getDeltaMovement();
            p.vx = (mv.x * f.getStepX() + mv.z * f.getStepZ()) * 20.0;
            p.vz = (mv.x * r.getStepX() + mv.z * r.getStepZ()) * 20.0;
            p.vy = mv.y * 20.0;
            p.face = Math.toRadians(sp.getYRot());
        }
    }

    /** Un golpe de un humano (clic izquierdo = 0, derecho = 1). */
    public void humanHit(ServerPlayer sp, int kind, double fwd, double right, boolean dive) {
        if (phase != PLAYING || !(level instanceof ServerLevel sl)) {
            return;
        }
        Integer idx = idxOf.get(sp.getUUID());
        if (idx == null || idx >= match.players.size()) {
            return;
        }
        if (dive && kind == VolleyMatch.HIT) {
            long now = sl.getGameTime();
            Long last = diveAt.get(sp.getUUID());
            if (last != null && now - last < 30) {
                dive = false;
            } else {
                diveAt.put(sp.getUUID(), now);
                // el clavado: un salto corto hacia la pelota
                Vec3 b = world(match.ball[0], 0, match.ball[2]);
                double dx = b.x - sp.getX();
                double dz = b.z - sp.getZ();
                double d = Math.max(0.2, Math.hypot(dx, dz));
                sp.setDeltaMovement(dx / d * 0.6, 0.25, dz / d * 0.6);
                sp.hurtMarked = true;
            }
        }
        injectPositions();
        match.humanAct(idx, kind, fwd, right, dive);
    }

    private void sound(SoundEvent ev, double u, double y, double v, float vol, float pitch) {
        Vec3 w = world(u, y, v);
        level.playSound(null, w.x, w.y, w.z, ev, SoundSource.BLOCKS, vol, pitch);
    }

    private void burst(ParticleOptions type, double u, double y, double v, int count, double spread, double speed) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Vec3 w = world(u, y, v);
        sl.sendParticles(type, w.x, w.y, w.z, count, spread, spread * 0.6, spread, speed);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (phase == LOBBY && (sl.getGameTime() % 20L) == 0L) {
            // la sala se vacía sola si se van todos
            Vec3 ctr = world(0, 1, 0);
            boolean changed = false;
            for (int i = members.size() - 1; i >= 0; i--) {
                ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(members.get(i).id);
                if (sp == null || sp.level() != level || sp.distanceToSqr(ctr.x, ctr.y, ctr.z) > 30 * 30) {
                    members.remove(i);
                    changed = true;
                }
            }
            if (changed) {
                if (members.isEmpty()) {
                    phase = FREE;
                }
                refreshLobbyLists();
            }
        }
        if (phase != PLAYING) {
            return;
        }
        // humanos que se fueron: los reemplaza un bot; si ya no queda ninguno, se acaba
        Vec3 ctr = world(0, 1, 0);
        boolean changed = false;
        for (int i = members.size() - 1; i >= 0; i--) {
            Member m = members.get(i);
            ServerPlayer sp = sl.getServer().getPlayerList().getPlayer(m.id);
            if (sp == null || sp.isRemoved() || sp.level() != level || sp.distanceToSqr(ctr.x, ctr.y, ctr.z) > 32 * 32) {
                replaceWithBot(m);
                idxOf.remove(m.id);
                members.remove(i);
                changed = true;
                broadcast(Component.translatable("pooltable.voley.left", m.name));
            }
        }
        if (members.isEmpty()) {
            endMatch(null);
            return;
        }
        if (changed) {
            refreshLobbyLists();
        }
        injectPositions();
        match.tick(0.05);
        handleEvents(sl);
        sendSync();
        match.clearEvents();
        if (match.state == VolleyMatch.State.IDLE) {
            endMatch(null);
        }
    }

    private ServerPlayer humanAt(ServerLevel sl, int idx) {
        for (Map.Entry<UUID, Integer> e : idxOf.entrySet()) {
            if (e.getValue() == idx) {
                return sl.getServer().getPlayerList().getPlayer(e.getKey());
            }
        }
        return null;
    }

    private void handleEvents(ServerLevel sl) {
        double bu = match.ball[0];
        double by = match.ball[1];
        double bv = match.ball[2];
        for (int[] h : match.hits) {
            VolleyMatch.Act act = VolleyMatch.Act.values()[h[0]];
            switch (act) {
                case SERVE -> {
                    sound(SoundEvents.SLIME_BLOCK_HIT, bu, by, bv, 0.9F, 1.4F);
                    sound(SoundEvents.PLAYER_ATTACK_SWEEP, bu, by, bv, 0.5F, 1.2F);
                }
                case BUMP -> sound(SoundEvents.SLIME_BLOCK_HIT, bu, by, bv, 0.8F, 1.0F);
                case SET -> sound(SoundEvents.SLIME_BLOCK_HIT, bu, by, bv, 0.6F, 1.6F);
                case FREE -> sound(SoundEvents.SLIME_BLOCK_HIT, bu, by, bv, 0.8F, 1.2F);
                case SPIKE -> {
                    sound(SoundEvents.PLAYER_ATTACK_STRONG, bu, by, bv, 1.0F, 0.9F);
                    sound(SoundEvents.SLIME_BLOCK_HIT, bu, by, bv, 1.0F, 0.7F);
                    burst(ParticleTypes.CRIT, bu, by, bv, 14, 0.15, 0.3);
                    burst(ParticleTypes.CLOUD, bu, by, bv, 5, 0.1, 0.05);
                }
                case BLOCK -> {
                    sound(SoundEvents.SHIELD_BLOCK, bu, by, bv, 0.9F, 1.1F);
                    burst(ParticleTypes.POOF, bu, by, bv, 8, 0.15, 0.05);
                }
                default -> {
                }
            }
            ServerPlayer sp = humanAt(sl, h[1]);
            if (sp != null) {
                sp.swing(InteractionHand.MAIN_HAND, true);
            }
        }
        if (match.evNet) {
            sound(SoundEvents.WOOL_HIT, 0, Math.min(by, 2.0), bv, 0.8F, 0.8F);
        }
        if (match.evLand) {
            sound(SoundEvents.SAND_BREAK, bu, 0.2, bv, 1.0F, 0.8F);
            burst(ParticleTypes.CLOUD, bu, 0.15, bv, 6, 0.2, 0.02);
        }
        if (match.evPoint != 0) {
            int team = match.evPoint - 1;
            sound(SoundEvents.PLAYER_LEVELUP, 0, 1.5, 0, 0.7F, 1.3F);
            burst(ParticleTypes.FIREWORK, bu, 0.4, bv, 24, 0.3, 0.12);
            Component who = Component.translatable("pooltable.voley.team." + team);
            Component msg = Component.translatable(match.evOut ? "pooltable.voley.point.out" : "pooltable.voley.point", who, match.scoreA, match.scoreB);
            for (ServerPlayer p : sl.players()) {
                Vec3 c = world(0, 1, 0);
                if (p.distanceToSqr(c.x, c.y, c.z) <= 40 * 40) {
                    p.displayClientMessage(msg, true);
                }
            }
        }
        if (match.evRejectBy >= 0 && !match.evReject.isEmpty()) {
            ServerPlayer sp = humanAt(sl, match.evRejectBy);
            if (sp != null) {
                say(sp, "pooltable.voley.rej." + match.evReject);
            }
        }
        if (match.evServeStart) {
            // el humano que saca va detrás de la línea de fondo
            ServerPlayer sp = humanAt(sl, match.serverIdx);
            if (sp != null) {
                double[] spot = match.serveSpot(match.serving);
                placeHuman(sp, match.serving, spot[0], spot[1]);
            }
        }
        if (match.state == VolleyMatch.State.OVER && !announcedEnd) {
            announcedEnd = true;
            sound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0, 1.5, 0, 1.0F, 1.0F);
            broadcast(Component.translatable("pooltable.voley.win", Component.translatable("pooltable.voley.team." + match.winner), match.scoreA, match.scoreB));
        }
    }

    /** Prueba interna: una partida solo de bots durante unos ticks, sin jugadores (para comprobar en el servidor que la lógica no falla). */
    public String debugRun(int ticks) {
        if (!(level instanceof ServerLevel)) {
            return "sin servidor";
        }
        match.reset();
        idxOf.clear();
        members.clear();
        for (int t = 0; t < 2; t++) {
            for (int i = 0; i < 2; i++) {
                match.addPlayer(t, false, "bot");
            }
        }
        match.start(2, 2);
        phase = PLAYING;
        int hits = 0;
        for (int i = 0; i < ticks && match.state != VolleyMatch.State.IDLE; i++) {
            match.tick(0.05);
            if (level instanceof ServerLevel sl) {
                handleEvents(sl);
                sendSync();
            }
            hits += match.hits.size();
            match.clearEvents();
        }
        String res = "golpes=" + hits + " marcador=" + match.scoreA + "-" + match.scoreB;
        boolean ok = hits > 4 && !Double.isNaN(match.ball[0]);
        phase = FREE;
        match.reset();
        refreshLobbyLists();
        return (ok ? "OK " : "FALLA ") + res;
    }

    // ------------------------------------------------------------------ red y guardado

    private void sendSync() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        PoolNet.sendToTracking(sl, worldPosition, VolleySyncPacket.of(worldPosition, match, idxOf, members, sl));
    }

    public void sync() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    /** Lo que recibe el cliente en cada tick de la partida. */
    public void clientReceive(VolleySyncPacket m) {
        cState = m.state;
        cScoreA = m.scoreA;
        cScoreB = m.scoreB;
        cServing = m.serving;
        cTouches = m.touches;
        cTouchTeam = m.touchTeam;
        cN = m.n;
        cDiff = m.diff;
        cServerIdx = m.serverIdx;
        cTimer = m.timer;
        System.arraycopy(m.ball, 0, cBall, 0, 6);
        cCount = Math.min(6, m.count);
        for (int i = 0; i < cCount; i++) {
            cTeam[i] = m.team[i];
            cFlags[i] = m.flags[i];
            cAct[i] = m.act[i];
            cSwing[i] = m.swing[i];
            cEid[i] = m.eid[i];
            cX[i] = m.px[i];
            cZ[i] = m.pz[i];
            cY[i] = m.py[i];
        }
        String pop = m.popup();
        if (!pop.isEmpty()) {
            popup = pop;
            popupAt = System.nanoTime();
        }
        recvNanos = System.nanoTime();
        clientInit = true;
    }

    /** Posición de la pelota para dibujar: lo último recibido más lo que avanzó desde entonces. */
    public float[] ballNow() {
        double dt = Math.min(0.1, (System.nanoTime() - recvNanos) / 1.0e9);
        boolean flying = cState == VolleyMatch.State.RALLY.ordinal();
        if (!flying) {
            return new float[] {cBall[0], cBall[1], cBall[2]};
        }
        double g = VolleyMatch.G;
        double y = Math.max(VolleyMatch.R, cBall[1] + cBall[4] * dt - 0.5 * g * dt * dt);
        return new float[] {(float) (cBall[0] + cBall[3] * dt), (float) y, (float) (cBall[2] + cBall[5] * dt)};
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("phase", phase);
        tag.putInt("size", sizeSetting);
        tag.putInt("diff", diffSetting);
        ListTag names = new ListTag();
        for (String s : lobbyNames) {
            names.add(StringTag.valueOf(s));
        }
        tag.put("names", names);
        int[] teams = new int[lobbyTeams.size()];
        for (int i = 0; i < teams.length; i++) {
            teams[i] = lobbyTeams.get(i);
        }
        tag.putIntArray("teams", teams);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        phase = tag.getInt("phase");
        sizeSetting = tag.getInt("size");
        diffSetting = tag.getInt("diff");
        lobbyNames.clear();
        lobbyTeams.clear();
        ListTag names = tag.getList("names", 8);
        int[] teams = tag.getIntArray("teams");
        for (int i = 0; i < names.size() && i < teams.length; i++) {
            lobbyNames.add(names.getString(i));
            lobbyTeams.add(teams[i]);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // al cargar el mundo no se retoma una partida a medias
            phase = FREE;
            members.clear();
            lobbyNames.clear();
            lobbyTeams.clear();
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
        return new AABB(worldPosition).inflate(12.0, 10.0, 12.0);
    }
}
