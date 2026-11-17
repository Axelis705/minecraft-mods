package com.celular.client;

import com.celular.music.MusicPlayer;
import com.celular.music.RemoteStream;
import com.celular.music.Track;
import com.celular.net.CelularNet;
import com.celular.net.RadioNet;
import com.celular.plat.Net;
import com.celular.radio.SongInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * La música del mundo en el celular: la lista de canciones subidas, la caché (.minecraft/celular/cache_musica, por SHA-1), las
 * bajadas y subidas por pedazos, los altavoces de los demás (suenan desde su celular según la distancia) y el Jam (lo que toca el
 * servidor, sincronizado con el reproductor). Todo corre en el hilo del juego; lo llama {@link Music#frame()} en cada cuadro.
 */
public final class Radio {
    /** Distancia a la que un altavoz deja de oírse. */
    private static final double HEAR = 24.0;
    /** Cuántos altavoces de otros suenan a la vez (los más cercanos). */
    private static final int MAX_REMOTES = 3;

    // ------------------------------------------------------------------ biblioteca
    public static final List<SongInfo> songs = new ArrayList<>();
    public static boolean loaded;
    public static long used, max;
    public static int maxSongMb = 15, maxPerPlayer = 50;
    public static boolean admin;
    private static long libAskedAt;

    private Radio() {
    }

    public static void requestLibrary() {
        long now = System.currentTimeMillis();
        if (now - libAskedAt > 1500) {
            libAskedAt = now;
            Net.toServer(new RadioNet.Req(RadioNet.Req.LIB));
        }
    }

    public static void library(RadioNet.Library m) {
        songs.clear();
        songs.addAll(m.songs);
        songs.sort((a, b) -> Long.compare(b.added, a.added));
        used = m.used;
        max = m.max;
        maxSongMb = m.maxSongMb;
        maxPerPlayer = m.maxPerPlayer;
        admin = m.admin;
        loaded = true;
        HASHING.clear();                                   // con la lista nueva se vuelve a buscar cuáles ya están
        for (Track t : MusicPlayer.library) {
            match(t);
        }
    }

    public static SongInfo song(long id) {
        for (SongInfo i : songs) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    /** Si esta canción de la PC ya está en el mundo (mismo archivo), queda marcada con su número. */
    private static void match(Track t) {
        if (t.hash.isEmpty()) {
            return;
        }
        t.worldId = 0;
        for (SongInfo i : songs) {
            if (i.hash.equals(t.hash)) {
                t.worldId = i.id;
            }
        }
    }

    public static boolean mine(SongInfo i) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && i.uploaderId.equals(mc.player.getUUID().toString());
    }

    public static void delete(SongInfo i) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.DELETE, i.id, 0, ""));
    }

    // ------------------------------------------------------------------ caché

    private static Path cacheDir() {
        return MusicPlayer.host.gameDir().resolve("celular").resolve("cache_musica");
    }

    private static Path cached(SongInfo i) {
        return cacheDir().resolve(i.hash + "." + i.ext);
    }

    private static final java.util.Set<Long> READY = new java.util.HashSet<>();
    private static final Map<Long, Long> CHECKED = new HashMap<>();

    /** ¿Ya está bajada en la caché? (se guarda la respuesta para no mirar el disco en cada cuadro). */
    public static boolean ready(SongInfo i) {
        if (READY.contains(i.id)) {
            return true;
        }
        long now = System.currentTimeMillis();
        Long at = CHECKED.get(i.id);
        if (at != null && now - at < 3000) {
            return false;
        }
        CHECKED.put(i.id, now);
        try {
            if (Files.size(cached(i)) == i.size) {
                READY.add(i.id);
                return true;
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private static final Map<Long, Track> TRACKS = new HashMap<>();

    /** La canción del mundo como pista del reproductor (desde la caché). */
    public static Track track(SongInfo i) {
        Track t = TRACKS.get(i.id);
        if (t == null) {
            t = new Track(cached(i), i.title, i.artist, i.ext, i.added);
            t.seconds = i.seconds;
            t.worldId = i.id;
            t.hash = i.hash;
            TRACKS.put(i.id, t);
        }
        return t;
    }

    // ------------------------------------------------------------------ tocar una del mundo

    /** La canción del mundo que se tocará en cuanto termine de bajar (0 = ninguna). */
    public static long pendingPlay;

    /** Toca una canción del mundo en tu celular (si aún no está bajada, la baja primero). Sigue con las demás ya bajadas. */
    public static void play(SongInfo i) {
        leaveJamForLocal();
        if (ready(i)) {
            pendingPlay = 0;
            List<Track> list = new ArrayList<>();
            int at = 0;
            for (SongInfo o : songs) {
                if (o == i || ready(o)) {
                    if (o == i) {
                        at = list.size();
                    }
                    list.add(track(o));
                }
            }
            MusicPlayer.playList(list, at);
        } else {
            pendingPlay = i.id;
            want(i, true);
        }
    }

    // ------------------------------------------------------------------ bajar

    private static final class Down {
        SongInfo info;
        byte[] buf;
        boolean[] got;
        int received, total, asked;
        long lastAt;
    }

    private static Down down;
    private static final LinkedHashMap<Long, SongInfo> WANT = new LinkedHashMap<>();
    private static final Map<Long, Boolean> SAVING = new HashMap<>();

    /** Pide bajar la canción (si no está ya). {@code urgent}: va primero en la fila. */
    public static void want(SongInfo i, boolean urgent) {
        if (i == null || ready(i) || SAVING.containsKey(i.id) || (down != null && down.info.id == i.id)) {
            return;
        }
        if (urgent && !WANT.isEmpty() && WANT.keySet().iterator().next() != i.id) {
            LinkedHashMap<Long, SongInfo> rest = new LinkedHashMap<>(WANT);
            WANT.clear();
            WANT.put(i.id, i);
            rest.remove(i.id);
            WANT.putAll(rest);
        } else {
            WANT.putIfAbsent(i.id, i);
        }
    }

    /** Cuánto lleva bajado (0..1), o -1 si no se está bajando. */
    public static float progress(long id) {
        if (down != null && down.info.id == id && down.total > 0) {
            return (float) down.received / down.total;
        }
        return SAVING.containsKey(id) ? 1F : -1F;
    }

    public static boolean queued(long id) {
        return WANT.containsKey(id);
    }

    private static void request(Down d, int from) {
        d.asked = Math.min(d.total, from + RadioNet.DOWN_BATCH);
        d.lastAt = System.currentTimeMillis();
        Net.toServer(new RadioNet.Req(RadioNet.Req.GET, d.info.id, from, ""));
    }

    public static void data(RadioNet.SongData m) {
        Down d = down;
        if (d == null || m.id != d.info.id || m.index < 0 || m.index >= d.total || d.got[m.index]) {
            return;
        }
        int off = m.index * RadioNet.DOWN_CHUNK;
        if (off + m.data.length > d.buf.length) {
            down = null;
            return;
        }
        System.arraycopy(m.data, 0, d.buf, off, m.data.length);
        d.got[m.index] = true;
        d.received++;
        d.lastAt = System.currentTimeMillis();
        if (d.received >= d.total) {
            finishDown(d);
        } else if (d.received >= d.asked) {
            request(d, d.asked);
        }
    }

    private static void finishDown(Down d) {
        down = null;
        SongInfo i = d.info;
        SAVING.put(i.id, true);
        byte[] data = d.buf;
        Thread t = new Thread(() -> {
            boolean ok = sha1(data).equals(i.hash);
            if (ok) {
                try {
                    Files.createDirectories(cacheDir());
                    Path tmp = cacheDir().resolve(i.hash + ".part");
                    Files.write(tmp, data);
                    Files.move(tmp, cached(i), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    ok = false;
                }
            }
            boolean fine = ok;
            Minecraft.getInstance().execute(() -> {
                SAVING.remove(i.id);
                CHECKED.remove(i.id);
                if (fine) {
                    READY.add(i.id);
                }
                if (!fine) {
                    WANT.putIfAbsent(i.id, i);      // se vuelve a intentar
                }
            });
        }, "Celular-Cache");
        t.setDaemon(true);
        t.start();
    }

    private static void pump() {
        long now = System.currentTimeMillis();
        if (down == null && !WANT.isEmpty()) {
            Iterator<SongInfo> it = WANT.values().iterator();
            SongInfo i = it.next();
            it.remove();
            if (ready(i)) {
                return;
            }
            Down d = new Down();
            d.info = i;
            d.buf = new byte[i.size];
            d.total = Math.max(1, (i.size + RadioNet.DOWN_CHUNK - 1) / RadioNet.DOWN_CHUNK);
            d.got = new boolean[d.total];
            down = d;
            request(d, 0);
        } else if (down != null && now - down.lastAt > 12000) {
            // no llegó nada en un rato: se pide otra vez desde el primero que falta
            int first = 0;
            while (first < down.total && down.got[first]) {
                first++;
            }
            down.received = first;
            for (int k = first; k < down.total; k++) {
                down.got[k] = false;
            }
            request(down, first);
        }
    }

    static String sha1(byte[] data) {
        try {
            byte[] dg = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : dg) {
                sb.append(String.format("%02x", b & 255));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // ------------------------------------------------------------------ subir

    public static Track uploading;
    public static float upProgress;
    private static byte[] upData;
    private static int upSession, upTotal, upNext, upAcked;
    private static long upAt;

    /** Sube una canción de la carpeta al mundo (en pedazos; el servidor confirma cada uno). */
    public static void upload(Track t) {
        if (uploading != null || t == null) {
            return;
        }
        uploading = t;
        upProgress = 0;
        Thread th = new Thread(() -> {
            byte[] data = null;
            try {
                long size = Files.size(t.path);
                if (size <= (long) maxSongMb * 1024 * 1024) {
                    data = Files.readAllBytes(t.path);
                }
            } catch (IOException ignored) {
            }
            byte[] d = data;
            String hash = d == null ? "" : sha1(d);
            Minecraft.getInstance().execute(() -> {
                if (d == null) {
                    uploading = null;
                    toast(Component.translatable("celular.radio.err_big"));
                    return;
                }
                t.hash = hash;
                upData = d;
                upSession = (int) (System.nanoTime() & 0x7FFFFFFF);
                upTotal = (d.length + RadioNet.UP_CHUNK - 1) / RadioNet.UP_CHUNK;
                upNext = 0;
                upAcked = 0;
                upAt = System.currentTimeMillis();
                sendMore();
            });
        }, "Celular-Subir");
        th.setDaemon(true);
        th.start();
    }

    public static void cancelUpload() {
        if (uploading != null) {
            uploading = null;
            upData = null;
            Net.toServer(new RadioNet.Req(RadioNet.Req.UP_CANCEL));
        }
    }

    private static void sendMore() {
        Track t = uploading;
        if (t == null || upData == null) {
            return;
        }
        while (upNext < upTotal && upNext - upAcked < 6) {
            int off = upNext * RadioNet.UP_CHUNK;
            int len = Math.min(RadioNet.UP_CHUNK, upData.length - off);
            byte[] part = new byte[len];
            System.arraycopy(upData, off, part, 0, len);
            boolean first = upNext == 0;
            Net.toServer(new RadioNet.SongUp(upSession, upNext, upTotal, upData.length, first ? t.ext : "", first ? SongInfo.cut(t.title, 100) : "",
                    first ? SongInfo.cut(t.artist, 100) : "", part));
            upNext++;
        }
    }

    public static void ack(RadioNet.SongAck m) {
        if (uploading == null || m.session != upSession) {
            return;
        }
        upAt = System.currentTimeMillis();
        switch (m.status) {
            case RadioNet.SongAck.MORE -> {
                upAcked = Math.max(upAcked, m.next);
                upProgress = (float) upAcked / Math.max(1, upTotal);
                sendMore();
            }
            case RadioNet.SongAck.DONE, RadioNet.SongAck.DUPLICATE -> {
                upProgress = 1;
                toast(Component.translatable(m.status == RadioNet.SongAck.DONE ? "celular.radio.uploaded" : "celular.radio.duplicate", m.msg));
                uploading = null;
                upData = null;
                requestLibrary();
            }
            default -> {
                toast(Component.translatable(m.msg.isEmpty() ? "celular.radio.err_bad" : m.msg, maxSongMb));
                uploading = null;
                upData = null;
            }
        }
    }

    private static void toast(Component c) {
        PhoneHud.toast(D.APP_MUSIC, "celular.app.music", c);
    }

    // ------------------------------------------------------------------ altavoces de los demás

    private static final class Remote {
        RadioNet.SpeakerState st;
        long recvAt;
        RemoteStream stream;
        boolean ended;

        double pos(long now) {
            return (st.posMs + (st.playing ? now - recvAt : 0)) / 1000.0;
        }
    }

    private static final Map<UUID, Remote> REMOTES = new HashMap<>();

    public static void speaker(RadioNet.SpeakerState m) {
        long now = System.currentTimeMillis();
        if (m.song == null) {
            Remote r = REMOTES.remove(m.owner);
            if (r != null && r.stream != null) {
                r.stream.kill();
            }
            return;
        }
        Remote r = REMOTES.computeIfAbsent(m.owner, k -> new Remote());
        boolean restart = r.st == null || r.st.song.id != m.song.id || r.st.playing != m.playing || r.st.source != m.source
                || Math.abs(r.pos(now) - m.posMs / 1000.0) > 2.0;
        if (restart) {
            if (r.stream != null) {
                r.stream.kill();
                r.stream = null;
            }
            r.ended = false;
        }
        r.st = m;
        r.recvAt = now;
    }

    /** Quién está poniendo música cerca (para mostrarlo en la app). */
    public static List<String> nearbySpeakers() {
        List<String> l = new ArrayList<>();
        for (Remote r : REMOTES.values()) {
            if (r.stream != null && !r.stream.done() && r.st.song != null) {
                l.add(r.st.name + ": " + r.st.song.title);
            }
        }
        return l;
    }

    private static void remotes(Minecraft mc) {
        if (!Prefs.hearOthers) {
            for (Remote r : REMOTES.values()) {
                if (r.stream != null) {
                    r.stream.kill();
                    r.stream = null;
                }
            }
            return;
        }
        long now = System.currentTimeMillis();
        Vec3 ear = mc.player.getEyePosition();
        List<Map.Entry<UUID, Remote>> near = new ArrayList<>();
        for (Map.Entry<UUID, Remote> e : REMOTES.entrySet()) {
            Remote r = e.getValue();
            Entity src = mc.level.getEntity(r.st.source);
            double d = src == null ? 1e9 : src.position().add(0, src.getBbHeight() * 0.6, 0).distanceTo(ear);
            if (d < HEAR + 4 && r.st.playing) {
                near.add(e);
                if (d < HEAR + 16) {
                    want(r.st.song, false);
                }
            } else if (r.stream != null) {
                r.stream.kill();
                r.stream = null;
            }
        }
        near.sort((a, b) -> {
            Entity ea = mc.level.getEntity(a.getValue().st.source), eb = mc.level.getEntity(b.getValue().st.source);
            return Double.compare(ea == null ? 1e9 : ea.distanceToSqr(ear), eb == null ? 1e9 : eb.distanceToSqr(ear));
        });
        for (int k = 0; k < near.size(); k++) {
            Remote r = near.get(k).getValue();
            if (k >= MAX_REMOTES) {
                if (r.stream != null) {
                    r.stream.kill();
                    r.stream = null;
                }
                continue;
            }
            Entity src = mc.level.getEntity(r.st.source);
            if (src == null) {
                continue;
            }
            if (r.stream != null && r.stream.done()) {
                r.stream = null;
                r.ended = true;               // se acabó la canción: se espera a que el dueño ponga otra
            }
            if (r.stream == null && !r.ended && ready(r.st.song)) {
                double pos = r.pos(now);
                if (pos < r.st.song.seconds - 1) {
                    r.stream = new RemoteStream(track(r.st.song), pos);
                    r.stream.left = 0;
                    r.stream.right = 0;
                    r.stream.start();
                }
            }
            if (r.stream != null) {
                float[] g = Music.spatial(mc, src.position().add(0, src.getBbHeight() * 0.6, 0), HEAR);
                float vol = Prefs.othersVolume;
                r.stream.left = g[0] * vol;
                r.stream.right = g[1] * vol;
            }
        }
    }

    // ------------------------------------------------------------------ mi altavoz

    private static long sentSong;
    private static boolean sentPlaying;
    private static int sentSource;
    private static double sentPos;
    private static long sentAt;
    private static final Map<Track, Boolean> HASHING = new HashMap<>();

    /** ¿Lo que suena en mi celular lo están oyendo los de cerca? */
    public static boolean broadcasting() {
        return sentSong != 0;
    }

    /** Calcula el SHA-1 de una canción de la PC, pero solo si alguna del mundo pesa lo mismo (si no, no puede ser la misma). */
    private static void hashLocal(Track t) {
        if (!t.hash.isEmpty() || HASHING.containsKey(t) || t.broken || songs.isEmpty()) {
            return;
        }
        HASHING.put(t, true);
        java.util.Set<Integer> sizes = new java.util.HashSet<>();
        for (SongInfo i : songs) {
            sizes.add(i.size);
        }
        Thread th = new Thread(() -> {
            String h = "";
            try {
                long size = Files.size(t.path);
                if (sizes.contains((int) size)) {
                    h = sha1(Files.readAllBytes(t.path));
                }
            } catch (IOException ignored) {
            }
            String hh = h;
            Minecraft.getInstance().execute(() -> {
                if (!hh.isEmpty()) {
                    t.hash = hh;
                    match(t);
                }
            });
        }, "Celular-Hash");
        th.setDaemon(true);
        th.start();
    }

    private static void mySpeaker(Minecraft mc) {
        Track cur = MusicPlayer.current;
        if (cur != null && cur.worldId == 0 && Prefs.speaker && !inJam()) {
            hashLocal(cur);
        }
        boolean on = Prefs.speaker && cur != null && !inJam() && cur.worldId != 0 && song(cur.worldId) != null;
        long now = System.currentTimeMillis();
        if (!on) {
            if (sentSong != 0) {
                Net.toServer(new RadioNet.SpeakerSet(0, 0, false, 0));
                sentSong = 0;
            }
            return;
        }
        int src = Music.speakerSource(mc);
        boolean pl = MusicPlayer.playing;
        double pos = MusicPlayer.position();
        double expected = sentPlaying ? sentPos + (now - sentAt) / 1000.0 : sentPos;
        if (cur.worldId != sentSong || pl != sentPlaying || src != sentSource || Math.abs(pos - expected) > 2.0) {
            Net.toServer(new RadioNet.SpeakerSet(cur.worldId, (long) (pos * 1000), pl, src));
            sentSong = cur.worldId;
            sentPlaying = pl;
            sentSource = src;
            sentPos = pos;
            sentAt = now;
        }
    }

    // ------------------------------------------------------------------ Jam

    public static RadioNet.JamState jam = RadioNet.JamState.none();
    private static long jamAt;
    public static RadioNet.JamList jams;
    /** Invitaciones recibidas: {id del Jam, nombre, quién invita}. */
    public static final List<String[]> invites = new ArrayList<>();
    private static long lastSeek;

    public static boolean inJam() {
        return jam.id != 0;
    }

    public static void jam(RadioNet.JamState m) {
        boolean was = inJam();
        jam = m;
        jamAt = System.currentTimeMillis();
        if (!inJam() && was && MusicPlayer.external) {
            MusicPlayer.stop();
        }
        if (inJam()) {
            invites.removeIf(v -> v[0].equals(String.valueOf(m.id)));
        }
    }

    public static void jamList(RadioNet.JamList m) {
        jams = m;
    }

    static void invite(CelularNet.Notify m) {
        String[] parts = m.text.split("\\|", 2);
        if (parts.length < 2) {
            return;
        }
        invites.removeIf(v -> v[0].equals(parts[0]));
        invites.add(new String[]{parts[0], parts[1], m.who});
        toast(Component.translatable("celular.radio.invited", m.who, parts[1]));
    }

    /** Último efecto de DJ que sonó (para dibujarlo en la app un momento). */
    public static String djWho = "";
    public static int djFx = -1;
    public static long djAt;

    /** Alguien del Jam tocó un botón de la botonera: suena aquí (sin importar dónde estés). */
    static void dj(CelularNet.Notify m) {
        int fx;
        try {
            fx = Integer.parseInt(m.text);
        } catch (NumberFormatException e) {
            return;
        }
        if (fx < 0 || fx >= com.celular.CelularReg.DJ.size() || !inJam()) {
            return;
        }
        float vol = Math.max(0.15F, MusicPlayer.volume);
        ClientHooks.play(com.celular.CelularReg.DJ.get(fx).get(), 1.0F, vol);
        djWho = m.who;
        djFx = fx;
        djAt = System.currentTimeMillis();
    }

    public static void djPress(int fx) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_DJ, fx, 0, ""));
    }

    /** Segundo actual del Jam según el servidor. */
    public static double jamPos() {
        return (jam.posMs + (jam.playing ? System.currentTimeMillis() - jamAt : 0)) / 1000.0;
    }

    private static void jamFrame() {
        if (!inJam()) {
            return;
        }
        SongInfo cur = jam.current();
        if (cur == null) {
            if (MusicPlayer.external && MusicPlayer.playing) {
                MusicPlayer.setPlaying(false);
            }
            return;
        }
        want(cur, true);
        if (jam.index + 1 < jam.queue.size()) {
            want(jam.queue.get(jam.index + 1), false);
        }
        if (!ready(cur)) {
            return;
        }
        Track t = track(cur);
        double expected = jamPos();
        if (expected >= cur.seconds - 0.5) {
            return;                                    // se está acabando: el servidor manda la siguiente
        }
        if (MusicPlayer.current != t || !MusicPlayer.external) {
            MusicPlayer.playExternal(t, expected, jam.playing);
            lastSeek = System.currentTimeMillis();
            return;
        }
        if (MusicPlayer.playing != jam.playing) {
            if (jam.playing && expected > cur.seconds - 4) {
                return;                                // aquí ya se acabó (la duración era aproximada): se espera la siguiente
            }
            MusicPlayer.setPlaying(jam.playing);
            if (jam.playing) {
                MusicPlayer.seek(expected);
            }
            lastSeek = System.currentTimeMillis();
            return;
        }
        long now = System.currentTimeMillis();
        if (jam.playing && Math.abs(MusicPlayer.position() - expected) > 1.5 && now - lastSeek > 3000) {
            MusicPlayer.seek(expected);
            lastSeek = now;
        }
    }

    public static void jamCreate(String name) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_CREATE, 0, 0, name));
    }

    public static void jamJoin(long id) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_JOIN, id, 0, ""));
    }

    public static void jamLeave() {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_LEAVE));
    }

    public static void jamAdd(SongInfo i) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_ADD, i.id, 0, ""));
        want(i, false);
    }

    public static void jamRemove(int qi) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_REMOVE, qi, 0, ""));
    }

    public static void jamInvite(UUID who) {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_INVITE, 0, 0, who.toString()));
    }

    public static void jamListRequest() {
        Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_LIST));
    }

    // ------------------------------------------------------------------ controles (pasan por el Jam si estás en uno)

    public static void toggle() {
        if (inJam()) {
            if (jam.amHost) {
                Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_PAUSE));
            } else {
                toast(Component.translatable("celular.radio.only_host"));
            }
            return;
        }
        MusicPlayer.toggle();
    }

    public static void next() {
        if (inJam()) {
            Net.toServer(new RadioNet.Req(RadioNet.Req.JAM_SKIP));
            return;
        }
        MusicPlayer.next();
    }

    public static void prev() {
        if (!inJam()) {
            MusicPlayer.prev();
        }
    }

    /** Al tocar algo de la lista estando en un Jam: se sale del Jam. */
    public static void leaveJamForLocal() {
        if (inJam()) {
            jamLeave();
            jam = RadioNet.JamState.none();
            toast(Component.translatable("celular.radio.left_jam"));
        }
    }

    // ------------------------------------------------------------------ fiesta: notas musicales sobre quien pone música

    private static long notesAt;
    private static final java.util.Random RND = new java.util.Random();

    private static void note(Minecraft mc, Entity e) {
        if (e == null || e.distanceToSqr(mc.player) > HEAR * HEAR) {
            return;
        }
        double x = e.getX() + (RND.nextDouble() - 0.5) * 0.6, y = e.getY() + e.getBbHeight() + 0.35, z = e.getZ() + (RND.nextDouble() - 0.5) * 0.6;
        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.NOTE, x, y, z, RND.nextInt(25) / 24.0, 0.0, 0.0);
    }

    private static void party(Minecraft mc) {
        long now = System.currentTimeMillis();
        if (now - notesAt < 650 || mc.isPaused() || !Prefs.partyNotes) {
            return;
        }
        notesAt = now;
        // altavoces de los demás que suenan
        for (Remote r : REMOTES.values()) {
            if (r.stream != null && !r.stream.done() && r.st.playing) {
                note(mc, mc.level.getEntity(r.st.source));
            }
        }
        // mi altavoz
        if (sentSong != 0 && sentPlaying) {
            note(mc, mc.level.getEntity(sentSource));
        }
        // los del Jam que están cerca (todos oyen lo mismo)
        if (inJam() && jam.playing) {
            for (net.minecraft.world.entity.player.Player p : mc.level.players()) {
                if (jam.members.contains(p.getScoreboardName())) {
                    note(mc, p);
                }
            }
        }
    }

    // ------------------------------------------------------------------ cada cuadro y reinicio

    static void frame(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        pump();
        if (pendingPlay != 0) {
            SongInfo p = song(pendingPlay);
            if (p == null || inJam()) {
                pendingPlay = 0;
            } else if (ready(p)) {
                play(p);
            }
        }
        jamFrame();
        mySpeaker(mc);
        remotes(mc);
        party(mc);
        if (uploading != null && System.currentTimeMillis() - upAt > 20000) {
            uploading = null;                              // el servidor no contestó
            upData = null;
            toast(Component.translatable("celular.radio.err_order"));
        }
    }

    static void reset() {
        for (Remote r : REMOTES.values()) {
            if (r.stream != null) {
                r.stream.kill();
            }
        }
        REMOTES.clear();
        songs.clear();
        loaded = false;
        down = null;
        WANT.clear();
        uploading = null;
        upData = null;
        jam = RadioNet.JamState.none();
        jams = null;
        invites.clear();
        sentSong = 0;
        pendingPlay = 0;
        HASHING.clear();
        READY.clear();
        CHECKED.clear();
        TRACKS.clear();
    }
}
