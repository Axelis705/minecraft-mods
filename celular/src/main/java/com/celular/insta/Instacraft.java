package com.celular.insta;

import com.celular.Celular;
import com.celular.net.CelularNet;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Instacraft en el servidor: guarda las publicaciones en la carpeta del mundo (world/celular/instacraft: un índice en JSON y una foto
 * .jpg por publicación), recibe las fotos en pedazos, reparte el muro, los "me gusta" y los avisos.
 */
public final class Instacraft {
    /** Máximo de publicaciones guardadas (al pasarse se borran las más viejas). */
    private static final int MAX_POSTS = 400;
    /** Cuántas publicaciones manda el muro general. */
    private static final int FEED_SIZE = 120;
    /** Tiempo mínimo entre publicaciones de un mismo jugador (los operadores no esperan). */
    private static final long COOLDOWN_MS = 15_000;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Una publicación guardada. */
    static final class Entry {
        long id;
        String author;
        String name;
        String caption;
        long time;
        LinkedHashSet<String> likes = new LinkedHashSet<>();
    }

    private static final class Upload {
        int session, total, next;
        String caption;
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    }

    private static MinecraftServer loadedFor;
    private static List<Entry> posts = new ArrayList<>();
    private static final Map<UUID, Upload> UPLOADS = new HashMap<>();
    private static final Map<UUID, Long> LAST_POST = new HashMap<>();

    private Instacraft() {
    }

    // ------------------------------------------------------------------ archivos

    private static Path dir(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("celular").resolve("instacraft");
    }

    private static void ensure(MinecraftServer s) {
        if (loadedFor == s) {
            return;
        }
        loadedFor = s;
        posts = new ArrayList<>();
        UPLOADS.clear();
        LAST_POST.clear();
        Path idx = dir(s).resolve("publicaciones.json");
        if (Files.exists(idx)) {
            try {
                JsonArray arr = JsonParser.parseString(Files.readString(idx, StandardCharsets.UTF_8)).getAsJsonArray();
                for (JsonElement el : arr) {
                    JsonObject o = el.getAsJsonObject();
                    Entry e = new Entry();
                    e.id = o.get("id").getAsLong();
                    e.author = o.get("author").getAsString();
                    e.name = o.has("name") ? o.get("name").getAsString() : "?";
                    e.caption = o.has("caption") ? o.get("caption").getAsString() : "";
                    e.time = o.has("time") ? o.get("time").getAsLong() : e.id;
                    if (o.has("likes")) {
                        for (JsonElement l : o.getAsJsonArray("likes")) {
                            e.likes.add(l.getAsString());
                        }
                    }
                    posts.add(e);
                }
            } catch (Exception ex) {
                Celular.LOGGER.error("No se pudo leer Instacraft", ex);
            }
        }
    }

    private static void save(MinecraftServer s) {
        try {
            Files.createDirectories(dir(s));
            Path tmp = dir(s).resolve("publicaciones.json.tmp");
            JsonArray arr = new JsonArray();
            for (Entry e : posts) {
                JsonObject o = new JsonObject();
                o.addProperty("id", e.id);
                o.addProperty("author", e.author);
                o.addProperty("name", e.name);
                o.addProperty("caption", e.caption);
                o.addProperty("time", e.time);
                JsonArray likes = new JsonArray();
                for (String l : e.likes) {
                    likes.add(l);
                }
                o.add("likes", likes);
                arr.add(o);
            }
            Files.writeString(tmp, GSON.toJson(arr), StandardCharsets.UTF_8);
            Files.move(tmp, dir(s).resolve("publicaciones.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Celular.LOGGER.error("No se pudo guardar Instacraft", ex);
        }
    }

    private static Path photo(MinecraftServer s, long id) {
        return dir(s).resolve(id + ".jpg");
    }

    private static Entry find(long id) {
        for (Entry e : posts) {
            if (e.id == id) {
                return e;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ mensajes

    private static CelularNet.Post view(Entry e, ServerPlayer p) {
        String me = p.getUUID().toString();
        return new CelularNet.Post(e.id, UUID.fromString(e.author), e.name, e.caption, e.time, e.likes.size(), e.likes.contains(me),
                e.author.equals(me) || Plat.isOp(p));
    }

    public static void sendFeed(ServerPlayer p, UUID author) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        ensure(s);
        List<CelularNet.Post> out = new ArrayList<>();
        String a = author == null ? null : author.toString();
        for (int i = posts.size() - 1; i >= 0 && out.size() < (a == null ? FEED_SIZE : MAX_POSTS); i--) {
            Entry e = posts.get(i);
            if (a == null || a.equals(e.author)) {
                out.add(view(e, p));
            }
        }
        Net.toPlayer(p, new CelularNet.Feed(author, out));
    }

    public static void sendImage(ServerPlayer p, long id) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        ensure(s);
        if (find(id) == null) {
            return;
        }
        try {
            Path f = photo(s, id);
            if (Files.exists(f) && Files.size(f) <= CelularNet.MAX_PHOTO) {
                Net.toPlayer(p, new CelularNet.Img(id, Files.readAllBytes(f)));
            }
        } catch (IOException ex) {
            Celular.LOGGER.warn("No se pudo leer la foto {}", id, ex);
        }
    }

    public static void upload(ServerPlayer p, CelularNet.Upload m) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        ensure(s);
        UUID me = p.getUUID();
        if (m.index == 0) {
            if (m.total < 1 || m.total > CelularNet.MAX_PHOTO / CelularNet.CHUNK + 1) {
                return;
            }
            Long last = LAST_POST.get(me);
            if (!Plat.isOp(p) && last != null && System.currentTimeMillis() - last < COOLDOWN_MS) {
                Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.ERROR, "", "celular.insta.wait"));
                return;
            }
            Upload u = new Upload();
            u.session = m.session;
            u.total = m.total;
            u.caption = clean(m.caption);
            UPLOADS.put(me, u);
        }
        Upload u = UPLOADS.get(me);
        if (u == null || u.session != m.session || u.next != m.index) {
            return;
        }
        u.bytes.write(m.data, 0, m.data.length);
        u.next++;
        if (u.bytes.size() > CelularNet.MAX_PHOTO) {
            UPLOADS.remove(me);
            Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.ERROR, "", "celular.insta.too_big"));
            return;
        }
        if (u.next < u.total) {
            return;
        }
        UPLOADS.remove(me);
        byte[] img = u.bytes.toByteArray();
        // tiene que ser un JPEG (empieza con FF D8 y termina con FF D9)
        if (img.length < 4 || (img[0] & 255) != 0xFF || (img[1] & 255) != 0xD8) {
            Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.ERROR, "", "celular.insta.bad_photo"));
            return;
        }
        Entry e = new Entry();
        long now = System.currentTimeMillis();
        e.id = Math.max(now, posts.isEmpty() ? 0 : posts.get(posts.size() - 1).id + 1);
        e.author = me.toString();
        e.name = p.getScoreboardName();
        e.caption = u.caption;
        e.time = now;
        try {
            Files.createDirectories(dir(s));
            Files.write(photo(s, e.id), img);
        } catch (IOException ex) {
            Celular.LOGGER.error("No se pudo guardar la foto", ex);
            Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.ERROR, "", "celular.insta.bad_photo"));
            return;
        }
        posts.add(e);
        while (posts.size() > MAX_POSTS) {
            Entry old = posts.remove(0);
            try {
                Files.deleteIfExists(photo(s, old.id));
            } catch (IOException ignored) {
            }
        }
        save(s);
        LAST_POST.put(me, now);
        Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.POSTED, e.name, e.caption));
        for (ServerPlayer o : s.getPlayerList().getPlayers()) {
            if (o != p) {
                Net.toPlayer(o, new CelularNet.Notify(CelularNet.Notify.NEW_POST, e.name, e.caption));
            }
        }
        sendFeed(p, null);
    }

    /** Quita saltos de línea y espacios de más; máximo 150 letras. */
    private static String clean(String c) {
        String t = c == null ? "" : c.replaceAll("[\\r\\n\\t§]", " ").replaceAll(" {2,}", " ").trim();
        return t.length() > 150 ? t.substring(0, 150) : t;
    }

    public static void like(ServerPlayer p, long id) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        ensure(s);
        Entry e = find(id);
        if (e == null) {
            return;
        }
        String me = p.getUUID().toString();
        boolean added;
        if (e.likes.contains(me)) {
            e.likes.remove(me);
            added = false;
        } else {
            e.likes.add(me);
            added = true;
        }
        save(s);
        Net.toPlayer(p, new CelularNet.PostUpd(view(e, p)));
        if (added && !e.author.equals(me)) {
            ServerPlayer author = s.getPlayerList().getPlayer(UUID.fromString(e.author));
            if (author != null) {
                Net.toPlayer(author, new CelularNet.Notify(CelularNet.Notify.LIKE, p.getScoreboardName(), e.caption));
            }
        }
    }

    public static void delete(ServerPlayer p, long id) {
        MinecraftServer s = Plat.server(p);
        if (p == null || s == null) {
            return;
        }
        ensure(s);
        Entry e = find(id);
        if (e == null || !(e.author.equals(p.getUUID().toString()) || Plat.isOp(p))) {
            return;
        }
        posts.remove(e);
        try {
            Files.deleteIfExists(photo(s, e.id));
        } catch (IOException ignored) {
        }
        save(s);
        sendFeed(p, null);
    }

    /** Cuántas publicaciones hay guardadas (para el comando). */
    public static int count(MinecraftServer s) {
        ensure(s);
        return posts.size();
    }

    /** Borra todas las publicaciones de un jugador (comando de operador). Devuelve cuántas borró. */
    public static int clearAuthor(MinecraftServer s, String name) {
        ensure(s);
        int n = 0;
        for (Entry e : new ArrayList<>(posts)) {
            if (e.name != null && e.name.equalsIgnoreCase(name)) {
                posts.remove(e);
                try {
                    Files.deleteIfExists(photo(s, e.id));
                } catch (IOException ignored) {
                }
                n++;
            }
        }
        if (n > 0) {
            save(s);
        }
        return n;
    }

    /** Para que el servidor no guarde subidas a medias de alguien que se fue. */
    public static void logout(UUID id) {
        UPLOADS.remove(id);
    }
}
