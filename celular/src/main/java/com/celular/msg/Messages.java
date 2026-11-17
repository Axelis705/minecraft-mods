package com.celular.msg;

import com.celular.Celular;
import com.celular.CelularReg;
import com.celular.net.AppNet;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mensajes: chats privados entre jugadores y un grupo "Todos" del mundo. Se guardan en {@code world/celular/mensajes.json} (los
 * últimos 200 de cada chat), así que llegan aunque el otro no esté conectado: los verá al volver.
 */
public final class Messages {
    /** El grupo de todos los del mundo. */
    public static final UUID ALL = new UUID(0L, 0L);
    private static final int KEEP = 200, MAX_LEN = 256;
    private static final Gson GSON = new GsonBuilder().create();

    private static final class Msg {
        String from;
        String text;
        long time;
    }

    private static final class Chat {
        final List<Msg> msgs = new ArrayList<>();
    }

    private static MinecraftServer loadedFor;
    private static final Map<String, Chat> CHATS = new LinkedHashMap<>();
    /** "jugador|chat" → hasta cuándo leyó. */
    private static final Map<String, Long> READ = new HashMap<>();
    /** uuid → último nombre conocido. */
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<UUID, Long> LAST_SENT = new HashMap<>();
    private static boolean dirty;
    private static long lastSave;

    private Messages() {
    }

    // ------------------------------------------------------------------ archivo

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("celular").resolve("mensajes.json");
    }

    private static void ensure(MinecraftServer s) {
        if (loadedFor == s) {
            return;
        }
        loadedFor = s;
        CHATS.clear();
        READ.clear();
        NAMES.clear();
        LAST_SENT.clear();
        Path f = file(s);
        if (!Files.exists(f)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject chats = root.getAsJsonObject("chats");
            for (Map.Entry<String, JsonElement> e : chats.entrySet()) {
                Chat c = new Chat();
                for (JsonElement el : e.getValue().getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    Msg m = new Msg();
                    m.from = o.get("de").getAsString();
                    m.text = o.get("texto").getAsString();
                    m.time = o.get("hora").getAsLong();
                    c.msgs.add(m);
                }
                CHATS.put(e.getKey(), c);
            }
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("leido").entrySet()) {
                READ.put(e.getKey(), e.getValue().getAsLong());
            }
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("nombres").entrySet()) {
                NAMES.put(e.getKey(), e.getValue().getAsString());
            }
        } catch (Exception ex) {
            Celular.LOGGER.error("No se pudieron leer los mensajes", ex);
        }
    }

    private static void save(MinecraftServer s, boolean force) {
        long now = System.currentTimeMillis();
        if (!dirty || (!force && now - lastSave < 3000)) {
            return;
        }
        dirty = false;
        lastSave = now;
        JsonObject root = new JsonObject();
        JsonObject chats = new JsonObject();
        for (Map.Entry<String, Chat> e : CHATS.entrySet()) {
            JsonArray arr = new JsonArray();
            for (Msg m : e.getValue().msgs) {
                JsonObject o = new JsonObject();
                o.addProperty("de", m.from);
                o.addProperty("texto", m.text);
                o.addProperty("hora", m.time);
                arr.add(o);
            }
            chats.add(e.getKey(), arr);
        }
        root.add("chats", chats);
        JsonObject read = new JsonObject();
        READ.forEach(read::addProperty);
        root.add("leido", read);
        JsonObject names = new JsonObject();
        NAMES.forEach(names::addProperty);
        root.add("nombres", names);
        try {
            Path f = file(s);
            Files.createDirectories(f.getParent());
            Path tmp = f.resolveSibling("mensajes.json.tmp");
            Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Celular.LOGGER.error("No se pudieron guardar los mensajes", ex);
        }
    }

    // ------------------------------------------------------------------ ayudas

    private static String key(UUID a, UUID b) {
        if (ALL.equals(b)) {
            return "todos";
        }
        String x = a.toString(), y = b.toString();
        return x.compareTo(y) < 0 ? x + "|" + y : y + "|" + x;
    }

    /** El otro de un chat (o ALL para el grupo); null si {@code me} no está en ese chat. */
    private static UUID other(String key, UUID me) {
        if (key.equals("todos")) {
            return ALL;
        }
        String m = me.toString();
        String[] p = key.split("\\|");
        if (p.length != 2 || (!p[0].equals(m) && !p[1].equals(m))) {
            return null;
        }
        try {
            return UUID.fromString(p[0].equals(m) ? p[1] : p[0]);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String nameOf(MinecraftServer s, UUID id) {
        if (ALL.equals(id)) {
            return "";
        }
        ServerPlayer p = s.getPlayerList().getPlayer(id);
        if (p != null) {
            return p.getScoreboardName();
        }
        return NAMES.getOrDefault(id.toString(), "?");
    }

    private static int unread(UUID me, String key, Chat c) {
        long since = READ.getOrDefault(me + "|" + key, 0L);
        int n = 0;
        String m = me.toString();
        for (int i = c.msgs.size() - 1; i >= 0 && n < 99; i--) {
            Msg x = c.msgs.get(i);
            if (x.time <= since) {
                break;
            }
            if (!x.from.equals(m)) {
                n++;
            }
        }
        return n;
    }

    private static AppNet.Msg view(Msg m, UUID me, MinecraftServer s) {
        AppNet.Msg v = new AppNet.Msg();
        v.mine = m.from.equals(me.toString());
        v.from = NAMES.getOrDefault(m.from, "?");
        v.text = m.text;
        v.time = m.time;
        return v;
    }

    // ------------------------------------------------------------------ pedidos

    public static void handle(ServerPlayer p, AppNet.Req m) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        NAMES.put(p.getUUID().toString(), p.getScoreboardName());
        switch (m.what) {
            case AppNet.Req.MSG_LIST -> sendList(p);
            case AppNet.Req.MSG_OPEN -> open(s, p, m.who);
            case AppNet.Req.MSG_SEND -> send(s, p, m.who, m.s);
            case AppNet.Req.MSG_READ -> markRead(s, p, m.who);
            default -> {
            }
        }
        save(s, m.what == AppNet.Req.MSG_SEND);
    }

    public static void sendList(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        UUID me = p.getUUID();
        AppNet.MsgList out = new AppNet.MsgList();
        java.util.Set<UUID> seen = new java.util.HashSet<>();
        // el grupo de todos siempre arriba
        Chat all = CHATS.get("todos");
        AppNet.Conv g = new AppNet.Conv();
        g.id = ALL;
        g.online = true;
        if (all != null && !all.msgs.isEmpty()) {
            Msg last = all.msgs.get(all.msgs.size() - 1);
            g.last = last.text;
            g.lastMine = last.from.equals(me.toString());
            g.name = NAMES.getOrDefault(last.from, "?");
            g.time = last.time;
            g.unread = unread(me, "todos", all);
        }
        out.convs.add(g);
        List<AppNet.Conv> mine = new ArrayList<>();
        for (Map.Entry<String, Chat> e : CHATS.entrySet()) {
            UUID o = other(e.getKey(), me);
            if (o == null || ALL.equals(o) || e.getValue().msgs.isEmpty()) {
                continue;
            }
            Msg last = e.getValue().msgs.get(e.getValue().msgs.size() - 1);
            AppNet.Conv c = new AppNet.Conv();
            c.id = o;
            c.name = nameOf(s, o);
            c.last = last.text;
            c.lastMine = last.from.equals(me.toString());
            c.time = last.time;
            c.unread = unread(me, e.getKey(), e.getValue());
            c.online = s.getPlayerList().getPlayer(o) != null;
            mine.add(c);
            seen.add(o);
        }
        mine.sort((a, b) -> Long.compare(b.time, a.time));
        out.convs.addAll(mine);
        // los conectados con celular con los que aún no hay chat (para empezar uno)
        for (ServerPlayer q : s.getPlayerList().getPlayers()) {
            if (q != p && !seen.contains(q.getUUID()) && CelularReg.phoneColor(q) >= 0) {
                AppNet.Conv c = new AppNet.Conv();
                c.id = q.getUUID();
                c.name = q.getScoreboardName();
                c.online = true;
                out.convs.add(c);
            }
        }
        Net.toPlayer(p, out);
    }

    private static void open(MinecraftServer s, ServerPlayer p, UUID who) {
        if (who == null) {
            return;
        }
        UUID me = p.getUUID();
        String k = key(me, who);
        Chat c = CHATS.get(k);
        List<AppNet.Msg> l = new ArrayList<>();
        if (c != null) {
            for (Msg m : c.msgs) {
                l.add(view(m, me, s));
            }
        }
        boolean on = ALL.equals(who) || s.getPlayerList().getPlayer(who) != null;
        Net.toPlayer(p, new AppNet.MsgThread(who, nameOf(s, who), on, l));
        READ.put(me + "|" + k, System.currentTimeMillis());
        dirty = true;
    }

    private static void markRead(MinecraftServer s, ServerPlayer p, UUID who) {
        if (who != null) {
            READ.put(p.getUUID() + "|" + key(p.getUUID(), who), System.currentTimeMillis());
            dirty = true;
        }
    }

    private static void send(MinecraftServer s, ServerPlayer p, UUID who, String raw) {
        if (who == null || who.equals(p.getUUID())) {
            return;
        }
        String text = raw.replace('§', ' ').replaceAll("[\\p{Cntrl}]", " ").trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > MAX_LEN) {
            text = text.substring(0, MAX_LEN);
        }
        long now = System.currentTimeMillis();
        Long last = LAST_SENT.get(p.getUUID());
        if (last != null && now - last < 400) {
            return;                                        // muy seguido
        }
        LAST_SENT.put(p.getUUID(), now);
        if (!ALL.equals(who) && !NAMES.containsKey(who.toString()) && s.getPlayerList().getPlayer(who) == null) {
            return;                                        // alguien que nunca ha estado en el mundo
        }
        String k = key(p.getUUID(), who);
        Chat c = CHATS.computeIfAbsent(k, x -> new Chat());
        Msg m = new Msg();
        m.from = p.getUUID().toString();
        m.text = text;
        m.time = now;
        c.msgs.add(m);
        while (c.msgs.size() > KEEP) {
            c.msgs.remove(0);
        }
        READ.put(p.getUUID() + "|" + k, now);
        dirty = true;
        // a quién le llega
        List<ServerPlayer> to = new ArrayList<>();
        if (ALL.equals(who)) {
            to.addAll(s.getPlayerList().getPlayers());
        } else {
            to.add(p);
            ServerPlayer q = s.getPlayerList().getPlayer(who);
            if (q != null) {
                to.add(q);
            }
        }
        for (ServerPlayer q : to) {
            UUID conv = ALL.equals(who) ? ALL : q == p ? who : p.getUUID();
            String convName = ALL.equals(who) ? "" : nameOf(s, conv);
            Net.toPlayer(q, new AppNet.MsgNew(conv, convName, view(m, q.getUUID(), s)));
        }
    }

    // ------------------------------------------------------------------ entrar y apagar

    public static void login(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        NAMES.put(p.getUUID().toString(), p.getScoreboardName());
        dirty = true;
        sendList(p);                                       // para el globito de mensajes sin leer
    }

    public static void stopping(MinecraftServer s) {
        if (loadedFor == s) {
            save(s, true);
        }
        loadedFor = null;
    }
}
