package com.celular.wallet;

import com.celular.Celular;
import com.celular.net.AppNet;
import com.celular.net.CelularNet;
import com.celular.plat.Ids;
import com.celular.plat.Net;
import com.celular.plat.Plat;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Billetera del casino: las fichas del mod Casino que llevas (casino:chip_1 ... chip_500) y enviar fichas a otro jugador conectado.
 * No depende del Casino: si no está instalado, simplemente no hay fichas.
 */
public final class Wallet {
    /** De mayor a menor (igual que en el Casino). */
    public static final int[] VALUES = {500, 100, 25, 5, 1};
    private static final int KEEP = 30;

    private static final class Entry {
        String who;
        int amount;
        long time;
    }

    private static MinecraftServer loadedFor;
    private static final Map<String, List<Entry>> HIST = new HashMap<>();

    private Wallet() {
    }

    /** Valor de una ficha del casino (0 si no es ficha). */
    public static int value(ItemStack s) {
        if (s.isEmpty()) {
            return 0;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(s.getItem());
        if (id == null || !id.getNamespace().equals("casino") || !id.getPath().startsWith("chip_")) {
            return 0;
        }
        try {
            return Integer.parseInt(id.getPath().substring(5));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static long balance(net.minecraft.world.entity.player.Player p) {
        long t = 0;
        for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
            t += (long) value(s) * s.getCount();
        }
        return t;
    }

    private static Item chip(int v) {
        Item it = ForgeRegistries.ITEMS.getValue(Ids.rl("casino", "chip_" + v));
        return it == null || it == net.minecraft.world.item.Items.AIR ? null : it;
    }

    /** Le da {@code amount} en fichas (las más grandes primero); lo que no cabe cae al suelo. */
    private static void give(ServerPlayer p, long amount) {
        for (int v : VALUES) {
            Item it = chip(v);
            if (it == null) {
                continue;
            }
            long n = amount / v;
            amount -= n * v;
            while (n > 0) {
                int c = (int) Math.min(n, it.getDefaultInstance().getMaxStackSize());
                n -= c;
                // a la mochila, y lo que no quepa cae a sus pies
                net.minecraftforge.items.ItemHandlerHelper.giveItemToPlayer(p, new ItemStack(it, c));
            }
        }
    }

    // ------------------------------------------------------------------ historial

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("celular").resolve("billetera.json");
    }

    private static void ensure(MinecraftServer s) {
        if (loadedFor == s) {
            return;
        }
        loadedFor = s;
        HIST.clear();
        Path f = file(s);
        if (!Files.exists(f)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                List<Entry> l = new ArrayList<>();
                for (JsonElement el : e.getValue().getAsJsonArray()) {
                    JsonObject o = el.getAsJsonObject();
                    Entry x = new Entry();
                    x.who = o.get("con").getAsString();
                    x.amount = o.get("fichas").getAsInt();
                    x.time = o.get("hora").getAsLong();
                    l.add(x);
                }
                HIST.put(e.getKey(), l);
            }
        } catch (Exception ex) {
            Celular.LOGGER.error("No se pudo leer la billetera", ex);
        }
    }

    private static void save(MinecraftServer s) {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, List<Entry>> e : HIST.entrySet()) {
            JsonArray arr = new JsonArray();
            for (Entry x : e.getValue()) {
                JsonObject o = new JsonObject();
                o.addProperty("con", x.who);
                o.addProperty("fichas", x.amount);
                o.addProperty("hora", x.time);
                arr.add(o);
            }
            root.add(e.getKey(), arr);
        }
        try {
            Path f = file(s);
            Files.createDirectories(f.getParent());
            Files.writeString(f, root.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            Celular.LOGGER.error("No se pudo guardar la billetera", ex);
        }
    }

    private static void log(UUID p, String who, int amount) {
        List<Entry> l = HIST.computeIfAbsent(p.toString(), k -> new ArrayList<>());
        Entry e = new Entry();
        e.who = who;
        e.amount = amount;
        e.time = System.currentTimeMillis();
        l.add(0, e);
        while (l.size() > KEEP) {
            l.remove(l.size() - 1);
        }
    }

    public static void sendHist(ServerPlayer p) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        AppNet.WalletHist h = new AppNet.WalletHist();
        for (Entry e : HIST.getOrDefault(p.getUUID().toString(), List.of())) {
            h.who.add(e.who);
            h.amount.add(e.amount);
            h.time.add(e.time);
        }
        Net.toPlayer(p, h);
    }

    // ------------------------------------------------------------------ enviar

    private static void tell(ServerPlayer p, String key) {
        Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.WALLET, "", key));
    }

    public static void send(ServerPlayer p, UUID who, long amount) {
        MinecraftServer s = Plat.server(p);
        ensure(s);
        ServerPlayer q = who == null ? null : s.getPlayerList().getPlayer(who);
        if (q == null || q == p) {
            tell(p, "celular.wallet.err_offline");
            return;
        }
        if (chip(1) == null) {
            tell(p, "celular.wallet.no_casino");
            return;
        }
        if (amount <= 0 || amount > 1_000_000) {
            return;
        }
        long have = balance(p);
        if (have < amount) {
            tell(p, "celular.wallet.err_money");
            return;
        }
        // se quitan todas las fichas y se devuelve el cambio (así siempre se puede pagar la cantidad justa)
        List<ItemStack> all = Plat.mainAndOffhand(p.getInventory());
        for (ItemStack st : all) {
            if (value(st) > 0) {
                st.setCount(0);
            }
        }
        give(p, have - amount);
        give(q, amount);
        log(p.getUUID(), q.getScoreboardName(), (int) -amount);
        log(q.getUUID(), p.getScoreboardName(), (int) amount);
        save(s);
        Net.toPlayer(p, new CelularNet.Notify(CelularNet.Notify.WALLET_SENT, q.getScoreboardName(), Long.toString(amount)));
        Net.toPlayer(q, new CelularNet.Notify(CelularNet.Notify.WALLET_IN, p.getScoreboardName(), Long.toString(amount)));
        sendHist(p);
        sendHist(q);
    }

    public static void stopping() {
        loadedFor = null;
    }
}
