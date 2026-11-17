package com.celular.radio;

import com.celular.net.CelularNet;
import com.celular.net.RadioNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Jams (como el de Spotify): una sesión con anfitrión y miembros que oyen lo mismo al mismo tiempo, estén donde estén. Cualquiera
 * agrega canciones del mundo a la cola; el anfitrión pausa, salta o quita; los demás votan para saltar (con la mitad basta).
 * El servidor lleva el reloj: avanza solo a la siguiente canción cuando termina la actual.
 */
public final class Jams {
    private static final int MAX_QUEUE = 100;

    private static final class Item {
        long song;
        String by;
    }

    private static final class Jam {
        long id;
        String name;
        UUID host;
        final LinkedHashSet<UUID> members = new LinkedHashSet<>();
        final List<Item> queue = new ArrayList<>();
        int index;
        long startMs;
        long pausedPos;
        boolean playing;
        final Set<UUID> votes = new HashSet<>();

        long pos() {
            return playing ? System.currentTimeMillis() - startMs : pausedPos;
        }
    }

    private static final Map<Long, Jam> JAMS = new LinkedHashMap<>();
    private static final Map<UUID, Long> MEMBER = new HashMap<>();
    private static final ScheduledExecutorService TIMER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Celular Jams");
        t.setDaemon(true);
        return t;
    });
    private static ScheduledFuture<?> ticking;
    private static MinecraftServer server;

    private Jams() {
    }

    // ------------------------------------------------------------------ reloj

    private static void startTimer(MinecraftServer s) {
        server = s;
        if (ticking == null) {
            ticking = TIMER.scheduleAtFixedRate(() -> {
                MinecraftServer sv = server;
                if (sv != null) {
                    sv.execute(Jams::tick);
                }
            }, 500, 500, TimeUnit.MILLISECONDS);
        }
    }

    private static void tick() {
        MinecraftServer s = server;
        if (s == null) {
            return;
        }
        for (Jam j : new ArrayList<>(JAMS.values())) {
            if (!j.playing || j.index >= j.queue.size()) {
                continue;
            }
            SongInfo cur = WorldMusic.find(s, j.queue.get(j.index).song);
            if (cur == null || j.pos() >= cur.seconds * 1000L + 400) {
                advance(s, j);
            }
        }
        if (JAMS.isEmpty() && ticking != null) {
            ticking.cancel(false);
            ticking = null;
        }
    }

    private static void advance(MinecraftServer s, Jam j) {
        j.votes.clear();
        j.index++;
        if (j.index < j.queue.size()) {
            j.playing = true;
            j.startMs = System.currentTimeMillis();
        } else {
            j.index = j.queue.size();
            j.playing = false;
            j.pausedPos = 0;
        }
        broadcast(s, j);
    }

    // ------------------------------------------------------------------ estado

    private static Jam of(UUID p) {
        Long id = MEMBER.get(p);
        return id == null ? null : JAMS.get(id);
    }

    private static String name(MinecraftServer s, UUID id) {
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        return p == null ? "?" : p.getScoreboardName();
    }

    private static int needed(Jam j) {
        return Math.max(1, (j.members.size() + 1) / 2);
    }

    private static RadioNet.JamState state(MinecraftServer s, Jam j, UUID to) {
        List<String> mem = new ArrayList<>();
        for (UUID u : j.members) {
            mem.add(name(s, u));
        }
        List<SongInfo> q = new ArrayList<>();
        List<String> by = new ArrayList<>();
        for (Item it : j.queue) {
            SongInfo i = WorldMusic.find(s, it.song);
            if (i != null) {
                q.add(i);
                by.add(it.by);
            }
        }
        return new RadioNet.JamState(j.id, j.name, name(s, j.host), j.host.equals(to), mem, q, by, j.index, j.pos(), j.playing, j.votes.size(),
                needed(j));
    }

    private static void broadcast(MinecraftServer s, Jam j) {
        for (UUID u : j.members) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            if (p != null) {
                Net.toPlayer(p, state(s, j, u));
            }
        }
    }

    public static void sendList(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        RadioNet.JamList l = new RadioNet.JamList();
        for (Jam j : JAMS.values()) {
            l.ids.add(j.id);
            l.names.add(j.name);
            l.hosts.add(name(s, j.host));
            l.members.add(j.members.size());
            SongInfo cur = j.index < j.queue.size() ? WorldMusic.find(s, j.queue.get(j.index).song) : null;
            l.now.add(cur == null ? "" : cur.title);
        }
        Net.toPlayer(p, l);
    }

    // ------------------------------------------------------------------ acciones

    public static void handle(ServerPlayer p, RadioNet.Req m) {
        MinecraftServer s = Plat.server(p);
        switch (m.what) {
            case RadioNet.Req.JAM_LIST -> sendList(p);
            case RadioNet.Req.JAM_CREATE -> create(s, p, m.s);
            case RadioNet.Req.JAM_JOIN -> join(s, p, m.a);
            case RadioNet.Req.JAM_LEAVE -> leave(s, p.getUUID(), true);
            case RadioNet.Req.JAM_ADD -> add(s, p, m.a);
            case RadioNet.Req.JAM_SKIP -> skip(s, p);
            case RadioNet.Req.JAM_PAUSE -> pause(s, p);
            case RadioNet.Req.JAM_REMOVE -> remove(s, p, (int) m.a);
            case RadioNet.Req.JAM_INVITE -> invite(s, p, m.s);
            case RadioNet.Req.JAM_DJ -> dj(s, p, (int) m.a);
            default -> {
            }
        }
    }

    private static void create(MinecraftServer s, ServerPlayer p, String name) {
        leave(s, p.getUUID(), false);
        startTimer(s);
        Jam j = new Jam();
        j.id = System.currentTimeMillis();
        while (JAMS.containsKey(j.id)) {
            j.id++;
        }
        String n = SongInfo.cut(name, 32);
        j.name = n.isEmpty() ? p.getScoreboardName() : n;
        j.host = p.getUUID();
        j.members.add(p.getUUID());
        JAMS.put(j.id, j);
        MEMBER.put(p.getUUID(), j.id);
        broadcast(s, j);
    }

    private static void join(MinecraftServer s, ServerPlayer p, long id) {
        Jam j = JAMS.get(id);
        if (j == null) {
            Net.toPlayer(p, RadioNet.JamState.none());
            sendList(p);
            return;
        }
        if (j.members.contains(p.getUUID())) {
            Net.toPlayer(p, state(s, j, p.getUUID()));
            return;
        }
        leave(s, p.getUUID(), false);
        j.members.add(p.getUUID());
        MEMBER.put(p.getUUID(), j.id);
        broadcast(s, j);
    }

    /** Sale del Jam en el que esté. Si era el anfitrión, pasa al siguiente; si no queda nadie, el Jam se acaba. */
    public static void leave(MinecraftServer s, UUID u, boolean tell) {
        Jam j = of(u);
        MEMBER.remove(u);
        if (j == null) {
            return;
        }
        j.members.remove(u);
        j.votes.remove(u);
        if (tell) {
            ServerPlayer p = s.getPlayerList().getPlayer(u);
            if (p != null) {
                Net.toPlayer(p, RadioNet.JamState.none());
            }
        }
        if (j.members.isEmpty()) {
            JAMS.remove(j.id);
            return;
        }
        if (j.host.equals(u)) {
            j.host = j.members.iterator().next();
        }
        broadcast(s, j);
    }

    private static void add(MinecraftServer s, ServerPlayer p, long song) {
        Jam j = of(p.getUUID());
        if (j == null || WorldMusic.find(s, song) == null || j.queue.size() >= MAX_QUEUE) {
            return;
        }
        Item it = new Item();
        it.song = song;
        it.by = p.getScoreboardName();
        boolean waiting = j.index >= j.queue.size();
        j.queue.add(it);
        if (waiting) {
            // la cola se había acabado (o estaba vacía): empieza con la nueva
            j.index = j.queue.size() - 1;
            j.playing = true;
            j.startMs = System.currentTimeMillis();
            j.votes.clear();
        }
        broadcast(s, j);
    }

    private static void skip(MinecraftServer s, ServerPlayer p) {
        Jam j = of(p.getUUID());
        if (j == null || j.index >= j.queue.size()) {
            return;
        }
        if (j.host.equals(p.getUUID())) {
            advance(s, j);
            return;
        }
        j.votes.add(p.getUUID());
        if (j.votes.size() >= needed(j)) {
            advance(s, j);
        } else {
            broadcast(s, j);
        }
    }

    private static void pause(MinecraftServer s, ServerPlayer p) {
        Jam j = of(p.getUUID());
        if (j == null || !j.host.equals(p.getUUID()) || j.index >= j.queue.size()) {
            return;
        }
        if (j.playing) {
            j.pausedPos = j.pos();
            j.playing = false;
        } else {
            j.startMs = System.currentTimeMillis() - j.pausedPos;
            j.playing = true;
        }
        broadcast(s, j);
    }

    /** Quita una canción de la cola (el anfitrión cualquiera; los demás solo las que agregaron). La que suena se salta. */
    private static void remove(MinecraftServer s, ServerPlayer p, int qi) {
        Jam j = of(p.getUUID());
        if (j == null || qi < 0 || qi >= j.queue.size()) {
            return;
        }
        Item it = j.queue.get(qi);
        if (!j.host.equals(p.getUUID()) && !it.by.equals(p.getScoreboardName())) {
            return;
        }
        if (qi == j.index) {
            j.queue.remove(qi);
            j.index--;
            advance(s, j);
            return;
        }
        j.queue.remove(qi);
        if (qi < j.index) {
            j.index--;
        }
        broadcast(s, j);
    }

    private static void invite(MinecraftServer s, ServerPlayer p, String target) {
        Jam j = of(p.getUUID());
        if (j == null) {
            return;
        }
        ServerPlayer t;
        try {
            t = s.getPlayerList().getPlayer(UUID.fromString(target));
        } catch (IllegalArgumentException e) {
            t = s.getPlayerList().getPlayerByName(target);
        }
        if (t == null || j.members.contains(t.getUUID())) {
            return;
        }
        Net.toPlayer(t, new CelularNet.Notify(CelularNet.Notify.JAM_INVITE, p.getScoreboardName(), j.id + "|" + j.name));
    }

    private static final Map<UUID, Long> LAST_DJ = new HashMap<>();

    /** Botonera de DJ: todos los del Jam oyen el efecto (como mucho uno cada 1.2 s por persona). */
    private static void dj(MinecraftServer s, ServerPlayer p, int fx) {
        Jam j = of(p.getUUID());
        if (j == null || fx < 0 || fx > 3) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = LAST_DJ.get(p.getUUID());
        if (last != null && now - last < 1200) {
            return;
        }
        LAST_DJ.put(p.getUUID(), now);
        CelularNet.Notify n = new CelularNet.Notify(CelularNet.Notify.DJ, p.getScoreboardName(), Integer.toString(fx));
        for (UUID u : j.members) {
            ServerPlayer q = s.getPlayerList().getPlayer(u);
            if (q != null) {
                Net.toPlayer(q, n);
            }
        }
    }

    /** La canción se borró del mundo: sale de todas las colas. */
    public static void songRemoved(MinecraftServer s, long song) {
        for (Jam j : JAMS.values()) {
            boolean changed = false;
            for (int i = j.queue.size() - 1; i >= 0; i--) {
                if (j.queue.get(i).song != song) {
                    continue;
                }
                j.queue.remove(i);
                changed = true;
                if (i < j.index) {
                    j.index--;
                } else if (i == j.index) {
                    j.index = i - 1;           // la que sigue queda en su lugar: advance la pone a sonar
                    advance(s, j);
                }
            }
            if (changed) {
                broadcast(s, j);
            }
        }
    }

    public static void clear() {
        LAST_DJ.clear();
        JAMS.clear();
        MEMBER.clear();
        if (ticking != null) {
            ticking.cancel(false);
            ticking = null;
        }
        server = null;
    }
}
