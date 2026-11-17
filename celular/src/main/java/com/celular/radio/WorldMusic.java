package com.celular.radio;

import com.celular.Celular;
import com.celular.music.Meta;
import com.celular.net.RadioNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Las canciones subidas al mundo (world/celular/musica: un índice canciones.json y un archivo por canción). Cualquiera puede subir
 * (con límite de tamaño por canción, por jugador y del mundo entero, en config/celular-musica.properties); el que la subió o un
 * operador la puede borrar. Los celulares las bajan por pedazos y las guardan en su caché.
 */
public final class WorldMusic {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static int maxSongMb = 15, maxTotalMb = 500, maxPerPlayer = 50;

    private static final class Upload {
        int session, total, size, next;
        String ext, title, artist;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    }

    private static MinecraftServer loadedFor;
    private static List<SongInfo> songs = new ArrayList<>();
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();

    private WorldMusic() {
    }

    // ------------------------------------------------------------------ archivos

    private static Path dir(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("celular").resolve("musica");
    }

    private static Path file(MinecraftServer s, SongInfo i) {
        return dir(s).resolve(i.id + "." + i.ext);
    }

    private static void config() {
        Path f = FMLPaths.CONFIGDIR.get().resolve("celular-musica.properties");
        Properties p = new Properties();
        try {
            if (Files.exists(f)) {
                try (var in = Files.newInputStream(f)) {
                    p.load(in);
                }
            }
            maxSongMb = clamp(p.getProperty("maxCancionMB"), 15, 1, 100);
            maxTotalMb = clamp(p.getProperty("maxMundoMB"), 500, 10, 20000);
            maxPerPlayer = clamp(p.getProperty("maxPorJugador"), 50, 1, 1000);
            if (!Files.exists(f)) {
                Files.writeString(f, """
                        # Música del celular subida al mundo
                        # Tamaño máximo de cada canción (MB)
                        maxCancionMB=15
                        # Espacio total para canciones en cada mundo (MB)
                        maxMundoMB=500
                        # Cuántas canciones puede subir cada jugador
                        maxPorJugador=50
                        """, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            Celular.LOGGER.warn("Celular: no se pudo leer celular-musica.properties", e);
        }
    }

    private static int clamp(String v, int def, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(v.trim())));
        } catch (Exception e) {
            return def;
        }
    }

    private static void ensure(MinecraftServer s) {
        if (loadedFor == s) {
            return;
        }
        loadedFor = s;
        songs = new ArrayList<>();
        UPLOADS.clear();
        config();
        Path idx = dir(s).resolve("canciones.json");
        try {
            if (Files.exists(idx)) {
                List<SongInfo> l = GSON.fromJson(Files.readString(idx, StandardCharsets.UTF_8), new TypeToken<List<SongInfo>>() {
                }.getType());
                if (l != null) {
                    for (SongInfo i : l) {
                        if (i != null && Files.exists(file(s, i))) {
                            songs.add(i);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Celular.LOGGER.warn("Celular: no se pudo leer la lista de canciones del mundo", e);
        }
    }

    private static void save(MinecraftServer s) {
        try {
            Files.createDirectories(dir(s));
            Path tmp = dir(s).resolve("canciones.json.tmp");
            Files.writeString(tmp, GSON.toJson(songs), StandardCharsets.UTF_8);
            Files.move(tmp, dir(s).resolve("canciones.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Celular.LOGGER.warn("Celular: no se pudo guardar la lista de canciones del mundo", e);
        }
    }

    public static SongInfo find(MinecraftServer s, long id) {
        ensure(s);
        for (SongInfo i : songs) {
            if (i.id == id) {
                return i;
            }
        }
        return null;
    }

    private static long used() {
        long u = 0;
        for (SongInfo i : songs) {
            u += i.size;
        }
        return u;
    }

    // ------------------------------------------------------------------ lista

    public static void sendLibrary(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        Net.toPlayer(p, new RadioNet.Library(new ArrayList<>(songs), used(), maxTotalMb * 1024L * 1024L, maxSongMb, maxPerPlayer, Plat.isOp(p)));
    }

    private static void broadcastLibrary(MinecraftServer s) {
        for (ServerPlayer q : s.getPlayerList().getPlayers()) {
            sendLibrary(q);
        }
    }

    // ------------------------------------------------------------------ subir

    private static void ack(ServerPlayer p, int session, int next, int status, String msg) {
        Net.toPlayer(p, new RadioNet.SongAck(session, next, status, msg));
    }

    public static void cancel(ServerPlayer p) {
        UPLOADS.remove(p.getUUID());
    }

    public static void upload(ServerPlayer p, RadioNet.SongUp m) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        UUID id = p.getUUID();
        Upload u = UPLOADS.get(id);
        if (m.index == 0) {
            String ext = m.ext.toLowerCase(Locale.ROOT);
            if (!ext.equals("mp3") && !ext.equals("ogg") && !ext.equals("wav")) {
                ack(p, m.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_type");
                return;
            }
            long max = maxSongMb * 1024L * 1024L;
            if (m.size <= 0 || m.size > max || m.total != (m.size + RadioNet.UP_CHUNK - 1) / RadioNet.UP_CHUNK) {
                ack(p, m.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_big");
                return;
            }
            int mine = 0;
            for (SongInfo i : songs) {
                if (i.uploaderId.equals(id.toString())) {
                    mine++;
                }
            }
            if (mine >= maxPerPlayer && !Plat.isOp(p)) {
                ack(p, m.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_mine");
                return;
            }
            if (used() + m.size > maxTotalMb * 1024L * 1024L) {
                ack(p, m.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_full");
                return;
            }
            u = new Upload();
            u.session = m.session;
            u.total = m.total;
            u.size = m.size;
            u.ext = ext;
            u.title = SongInfo.cut(m.title, 100);
            u.artist = SongInfo.cut(m.artist, 100);
            UPLOADS.put(id, u);
        }
        if (u == null || u.session != m.session || m.index != u.next) {
            ack(p, m.session, u == null ? 0 : u.next, RadioNet.SongAck.ERROR, "celular.radio.err_order");
            UPLOADS.remove(id);
            return;
        }
        u.bytes.write(m.data, 0, m.data.length);
        u.next++;
        if (u.bytes.size() > u.size) {
            UPLOADS.remove(id);
            ack(p, m.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_big");
            return;
        }
        if (u.next < u.total) {
            ack(p, m.session, u.next, RadioNet.SongAck.MORE, "");
            return;
        }
        UPLOADS.remove(id);
        finish(p, s, u);
    }

    private static void finish(ServerPlayer p, MinecraftServer s, Upload u) {
        byte[] data = u.bytes.toByteArray();
        if (data.length != u.size) {
            ack(p, u.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_order");
            return;
        }
        String hash = sha1(data);
        for (SongInfo i : songs) {
            if (i.hash.equals(hash)) {
                ack(p, u.session, u.total, RadioNet.SongAck.DUPLICATE, i.title);
                return;
            }
        }
        SongInfo info = new SongInfo();
        info.id = System.currentTimeMillis() * 16 + (long) (Math.random() * 16);
        info.hash = hash;
        info.ext = u.ext;
        info.title = u.title.isEmpty() ? "?" : u.title;
        info.artist = u.artist;
        info.uploader = p.getScoreboardName();
        info.uploaderId = p.getUUID().toString();
        info.size = data.length;
        info.added = System.currentTimeMillis();
        Path f = file(s, info);
        try {
            Files.createDirectories(dir(s));
            Files.write(f, data);
            int sec = Meta.seconds(f, info.ext);
            if (sec <= 0) {
                Files.deleteIfExists(f);
                ack(p, u.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_bad");
                return;
            }
            info.seconds = sec;
        } catch (Throwable e) {
            try {
                Files.deleteIfExists(f);
            } catch (IOException ignored) {
            }
            ack(p, u.session, 0, RadioNet.SongAck.ERROR, "celular.radio.err_bad");
            return;
        }
        songs.add(info);
        save(s);
        Celular.LOGGER.info("Celular: {} subió la canción '{}' ({} KB)", info.uploader, info.title, info.size / 1024);
        ack(p, u.session, u.total, RadioNet.SongAck.DONE, info.title);
        broadcastLibrary(s);
    }

    private static String sha1(byte[] data) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-1").digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) {
                sb.append(String.format("%02x", b & 255));
            }
            return sb.toString();
        } catch (Exception e) {
            return Long.toHexString(System.nanoTime());
        }
    }

    // ------------------------------------------------------------------ bajar

    /** Manda al cliente unos pedazos de la canción (desde {@code from}); el cliente pide los siguientes al recibirlos. */
    public static void download(ServerPlayer p, long id, int from) {
        MinecraftServer s = Plat.server(p);
        SongInfo i = find(s, id);
        if (i == null) {
            return;
        }
        int total = (i.size + RadioNet.DOWN_CHUNK - 1) / RadioNet.DOWN_CHUNK;
        try (RandomAccessFile raf = new RandomAccessFile(file(s, i).toFile(), "r")) {
            for (int k = Math.max(0, from); k < Math.min(total, from + RadioNet.DOWN_BATCH); k++) {
                long off = (long) k * RadioNet.DOWN_CHUNK;
                int len = (int) Math.min(RadioNet.DOWN_CHUNK, i.size - off);
                byte[] buf = new byte[len];
                raf.seek(off);
                raf.readFully(buf);
                Net.toPlayer(p, new RadioNet.SongData(i.id, k, total, buf));
            }
        } catch (IOException e) {
            Celular.LOGGER.warn("Celular: no se pudo leer la canción {}", i.id, e);
        }
    }

    // ------------------------------------------------------------------ borrar

    public static void delete(ServerPlayer p, long id) {
        MinecraftServer s = Plat.server(p);
        SongInfo i = find(s, id);
        if (i == null) {
            return;
        }
        if (!i.uploaderId.equals(p.getUUID().toString()) && !Plat.isOp(p)) {
            return;
        }
        songs.remove(i);
        try {
            Files.deleteIfExists(file(s, i));
        } catch (IOException ignored) {
        }
        save(s);
        Jams.songRemoved(s, i.id);
        Speakers.songRemoved(s, i.id);
        broadcastLibrary(s);
    }

    /** Borra todas las canciones de un jugador (comando). */
    public static int clearUploader(MinecraftServer s, String name) {
        ensure(s);
        int n = 0;
        for (SongInfo i : new ArrayList<>(songs)) {
            if (i.uploader.equalsIgnoreCase(name)) {
                songs.remove(i);
                try {
                    Files.deleteIfExists(file(s, i));
                } catch (IOException ignored) {
                }
                Jams.songRemoved(s, i.id);
                Speakers.songRemoved(s, i.id);
                n++;
            }
        }
        if (n > 0) {
            save(s);
            broadcastLibrary(s);
        }
        return n;
    }

    public static int count(MinecraftServer s) {
        ensure(s);
        return songs.size();
    }

    public static long usedBytes(MinecraftServer s) {
        ensure(s);
        return used();
    }

    public static void logout(UUID id) {
        UPLOADS.remove(id);
    }
}
