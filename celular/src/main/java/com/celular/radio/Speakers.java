package com.celular.radio;

import com.celular.net.RadioNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Altavoces: cuando un jugador pone una canción del mundo con el altavoz encendido, el servidor avisa a todos los de su dimensión
 * qué canción, en qué segundo y desde qué entidad suena (el jugador o su celular tirado). Cada celular decide si la oye según la
 * distancia (y solo baja la canción si está cerca).
 */
public final class Speakers {
    private static final class Sp {
        UUID owner;
        String name;
        int source;
        long song;
        long startMs;
        long pausedPos;
        boolean playing;
        ResourceKey<Level> dim;

        long pos() {
            return playing ? System.currentTimeMillis() - startMs : pausedPos;
        }
    }

    private static final Map<UUID, Sp> ALL = new HashMap<>();

    private Speakers() {
    }

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
        send(s, sp, true);
    }

    public static void stop(MinecraftServer s, UUID owner) {
        Sp sp = ALL.remove(owner);
        if (sp != null) {
            send(s, sp, false);
        }
    }

    private static RadioNet.SpeakerState state(MinecraftServer s, Sp sp, boolean on) {
        SongInfo info = on ? WorldMusic.find(s, sp.song) : null;
        return new RadioNet.SpeakerState(sp.owner, sp.name, sp.source, info, sp.pos(), sp.playing && info != null);
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

    public static void clear() {
        ALL.clear();
    }
}
