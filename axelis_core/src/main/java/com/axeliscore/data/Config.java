package com.axeliscore.data;

import com.axeliscore.AxelisCore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opciones sencillas de un mod en {@code config/<mod>.json}, fáciles de cambiar a mano. Cada opción lleva al lado una línea "//nombre"
 * que explica para qué sirve. Si falta o tiene un valor raro, se usa el de fábrica y se vuelve a escribir.
 * <pre>
 *     Config c = Config.of("pesca");
 *     int max = c.num("peces_por_dia", 50, 1, 1000, "Cuántos peces raros se pueden pescar por día");
 * </pre>
 */
public final class Config {
    private static final Map<String, Config> ALL = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private final Path path;
    private JsonObject json = new JsonObject();
    private boolean changed;

    private Config(String modid) {
        this.path = FMLPaths.CONFIGDIR.get().resolve(modid + ".json");
        reload();
    }

    public static Config of(String modid) {
        return ALL.computeIfAbsent(modid, Config::new);
    }

    /** Vuelve a leer el archivo (por ejemplo, desde un comando). */
    public synchronized void reload() {
        try {
            if (Files.exists(path)) {
                JsonElement e = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
                json = e != null && e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
            }
        } catch (IOException | RuntimeException e) {
            AxelisCore.LOGGER.warn("no pude leer {} ({}); se usan los valores de fábrica", path, e.toString());
            json = new JsonObject();
        }
    }

    public synchronized boolean bool(String key, boolean def, String help) {
        JsonElement e = json.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
            return e.getAsBoolean();
        }
        put(key, new JsonPrimitive(def), help);
        return def;
    }

    public synchronized int num(String key, int def, int min, int max, String help) {
        JsonElement e = json.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            double v = e.getAsDouble();
            if (v >= min && v <= max && v == Math.rint(v)) {
                return (int) v;
            }
        }
        put(key, new JsonPrimitive(def), help + " (" + min + " a " + max + ")");
        return def;
    }

    public synchronized double dec(String key, double def, double min, double max, String help) {
        JsonElement e = json.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            double v = e.getAsDouble();
            if (v >= min && v <= max) {
                return v;
            }
        }
        put(key, new JsonPrimitive(def), help + " (" + min + " a " + max + ")");
        return def;
    }

    public synchronized String str(String key, String def, String help) {
        JsonElement e = json.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
            return e.getAsString();
        }
        put(key, new JsonPrimitive(def), help);
        return def;
    }

    /** Cambia una opción desde el juego y la guarda. */
    public synchronized void set(String key, Object value) {
        JsonPrimitive v = value instanceof Boolean b ? new JsonPrimitive(b) : value instanceof Number n ? new JsonPrimitive(n)
                : new JsonPrimitive(String.valueOf(value));
        json.add(key, v);
        changed = true;
        save();
    }

    private void put(String key, JsonPrimitive v, String help) {
        if (help != null && !help.isEmpty()) {
            json.addProperty("//" + key, help);
        }
        json.add(key, v);
        changed = true;
        save();
    }

    /** Escribe el archivo si algo cambió. */
    public synchronized void save() {
        if (!changed) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(json), StandardCharsets.UTF_8);
            changed = false;
        } catch (IOException e) {
            AxelisCore.LOGGER.warn("no pude guardar {}: {}", path, e.toString());
        }
    }
}
