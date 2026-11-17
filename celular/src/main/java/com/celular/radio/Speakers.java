package com.celular.radio;

import com.celular.SpeakerBlock;
import com.celular.net.CelularNet;
import com.celular.net.RadioNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Altavoces: cuando un jugador pone una canción del mundo con el altavoz encendido, el servidor avisa a todos los de su dimensión
 * qué canción, en qué segundo y desde dónde suena (el jugador, su celular tirado o la bocina que conectó). Cada celular decide si la
 * oye según la distancia (y solo baja la canción si está cerca). Las bocinas se encienden (luces y bajo animado) mientras suenan.
 */
public final class Speakers {
    /** Hasta qué distancia de su bocina puede estar el dueño para que suene por ella. */
    private static final double PAIR_RANGE = 96.0;

    private static final class Sp {
        UUID owner;
        String name;
        int source;
        long song;
        long startMs;
        long pausedPos;
        boolean playing;
        ResourceKey<Level> dim;
        long block = RadioNet.SpeakerState.NO_BLOCK;

        long pos() {
            return playing ? System.currentTimeMillis() - startMs : pausedPos;
        }
    }

    private record Pair(ResourceKey<Level> dim, BlockPos pos) {
    }

    private static final Map<UUID, Sp> ALL = new HashMap<>();
    /** La bocina conectada de cada jugador. */
    private static final Map<UUID, Pair> PAIRED = new HashMap<>();

    private Speakers() {
    }

    // ------------------------------------------------------------------ bocinas

    private static boolean valid(MinecraftServer s, Pair pr) {
        ServerLevel level = s.getLevel(pr.dim);
        return level != null && level.isLoaded(pr.pos) && level.getBlockState(pr.pos).getBlock() instanceof SpeakerBlock;
    }

    /** Enciende o apaga las luces de la bocina (si sigue ahí). */
    private static void light(MinecraftServer s, Pair pr, boolean on) {
        if (pr == null || !valid(s, pr)) {
            return;
        }
        ServerLevel level = s.getLevel(pr.dim);
        BlockState st = level.getBlockState(pr.pos);
        if (st.getValue(SpeakerBlock.ON) != on) {
            level.setBlock(pr.pos, st.setValue(SpeakerBlock.ON, on), 3);
        }
    }

    /** Clic derecho en una bocina: la conecta a tu celular (o la desconecta si ya era la tuya). */
    public static void pair(ServerPlayer p, Level level, BlockPos pos) {
        MinecraftServer s = Plat.server(p);
        UUID id = p.getUUID();
        Pair cur = PAIRED.get(id);
        if (cur != null && cur.pos.equals(pos) && cur.dim == level.dimension()) {
            PAIRED.remove(id);
            light(s, cur, false);
            Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.SPEAKER_PAIR, "", ""));
            Plat.msg(p, Component.translatable("celular.speaker.unpaired"), true);
            resend(s, id);
            return;
        }
        if (cur != null) {
            light(s, cur, false);
        }
        // una bocina es de una sola persona a la vez
        Pair pr = new Pair(level.dimension(), pos.immutable());
        PAIRED.entrySet().removeIf(e -> {
            if (e.getValue().equals(pr) && !e.getKey().equals(id)) {
                ServerPlayer o = s.getPlayerList().getPlayer(e.getKey());
                if (o != null) {
                    Net.toPlayer(o, new CelularNet.Notify(CelularNet.Notify.SPEAKER_PAIR, "", ""));
                }
                return true;
            }
            return false;
        });
        PAIRED.put(id, pr);
        Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.SPEAKER_PAIR, "", pos.asLong() + "|" + level.dimension()));
        Plat.msg(p, Component.translatable("celular.speaker.paired"), true);
        resend(s, id);
    }

    /** Vuelve a avisar desde dónde suena (al conectar o desconectar la bocina mientras suena). */
    private static void resend(MinecraftServer s, UUID id) {
        Sp sp = ALL.get(id);
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        if (sp != null && p != null) {
            place(s, p, sp);
            send(s, sp, true);
        }
    }

    /** Decide si suena por la bocina (conectada, en la misma dimensión y no muy lejos) y la enciende o apaga. */
    private static void place(MinecraftServer s, ServerPlayer p, Sp sp) {
        Pair pr = PAIRED.get(sp.owner);
        boolean use = pr != null && pr.dim == p.level().dimension() && valid(s, pr) && p.position().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(pr.pos)) < PAIR_RANGE;
        sp.block = use ? pr.pos.asLong() : RadioNet.SpeakerState.NO_BLOCK;
        if (pr != null) {
            light(s, pr, use && sp.playing);
        }
    }

    // ------------------------------------------------------------------ altavoces

    public static void set(ServerPlayer p, RadioNet.SpeakerSet m) {
        MinecraftServer s = Plat.server(p);
        if (m.song == 0) {
            stop(s, p.getUUID());
            return;
        }
        SongInfo info = WorldMusic.find(s, m.song);
        if (info == null) {
            stop(s, p.getUUID());
            return;
        }
        Sp sp = ALL.computeIfAbsent(p.getUUID(), k -> new Sp());
        if (sp.dim != null && sp.dim != p.level().dimension()) {
            send(s, sp, false);                 // se cambió de dimensión: se apaga en la vieja
        }
        sp.owner = p.getUUID();
        sp.name = p.getScoreboardName();
        sp.source = m.source;
        sp.song = m.song;
        sp.playing = m.playing;
        long pos = Math.max(0, m.posMs);
        sp.startMs = System.currentTimeMillis() - pos;
        sp.pausedPos = pos;
        sp.dim = p.level().dimension();
        place(s, p, sp);
        send(s, sp, true);
    }

    public static void stop(MinecraftServer s, UUID owner) {
        Sp sp = ALL.remove(owner);
        light(s, PAIRED.get(owner), false);
        if (sp != null) {
            send(s, sp, false);
        }
    }

    private static RadioNet.SpeakerState state(MinecraftServer s, Sp sp, boolean on) {
        SongInfo info = on ? WorldMusic.find(s, sp.song) : null;
        return new RadioNet.SpeakerState(sp.owner, sp.name, sp.source, info, sp.pos(), sp.playing && info != null, sp.block);
    }

    private static void send(MinecraftServer s, Sp sp, boolean on) {
        RadioNet.SpeakerState st = state(s, sp, on);
        for (ServerPlayer q : s.getPlayerList().getPlayers()) {
            if (q.level().dimension() == sp.dim && !q.getUUID().equals(sp.owner)) {
                Net.toPlayer(q, st);
            }
        }
    }

    /** Al entrar al mundo o cambiar de dimensión: los altavoces que suenan donde está. */
    public static void sendAll(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        for (Sp sp : ALL.values()) {
            if (sp.dim == p.level().dimension() && !sp.owner.equals(p.getUUID())) {
                Net.toPlayer(p, state(s, sp, true));
            }
        }
    }

    /** El dueño se fue o cambió de dimensión: su altavoz se apaga (su celular lo vuelve a encender si sigue sonando). */
    public static void ownerGone(MinecraftServer s, UUID owner) {
        stop(s, owner);
    }

    /** Se desconectó del servidor: también se suelta su bocina. */
    public static void logout(MinecraftServer s, UUID owner) {
        stop(s, owner);
        PAIRED.remove(owner);
    }

    public static void songRemoved(MinecraftServer s, long song) {
        List<UUID> off = new ArrayList<>();
        for (Sp sp : ALL.values()) {
            if (sp.song == song) {
                off.add(sp.owner);
            }
        }
        for (UUID u : off) {
            stop(s, u);
        }
    }

    /** ¿Esta bocina está sonando ahora? (si no, se apaga sola: por ejemplo si el servidor se cerró mientras sonaba). */
    public static boolean active(Level level, BlockPos pos) {
        for (Map.Entry<UUID, Pair> e : PAIRED.entrySet()) {
            Sp sp = ALL.get(e.getKey());
            if (sp != null && sp.playing && e.getValue().pos.equals(pos) && e.getValue().dim == level.dimension()) {
                return true;
            }
        }
        return false;
    }

    public static void clear(MinecraftServer s) {
        for (Pair pr : PAIRED.values()) {
            light(s, pr, false);
        }
        ALL.clear();
        PAIRED.clear();
    }
}
