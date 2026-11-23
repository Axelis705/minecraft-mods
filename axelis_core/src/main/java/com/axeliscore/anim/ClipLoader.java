package com.axeliscore.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lee animaciones .animation.json de Blockbench (formato Bedrock, también lo que exporta el plugin de GeckoLib):
 * <pre>
 * "animation.persona.saludar": {
 *   "loop": false,                      // true, false o "hold_on_last_frame"
 *   "animation_length": 1.2,
 *   "bones": {
 *     "right_arm": {
 *       "rotation": {
 *         "0.0": [0, 0, 0],
 *         "0.3": {"vector": [-150, 0, 20], "easing": "easeOutBack"},
 *         "0.6": {"pre": [-140, 0, 30], "post": [-140, 0, 30], "lerp_mode": "catmullrom"}
 *       },
 *       "position": [0, "math.sin(q.anim_time * 720) * 0.5", 0]
 *     }
 *   },
 *   "sound_effects": {"0.3": {"effect": "axelis_core:persona.wave"}},
 *   "timeline": {"0.3": "golpe"}
 * }
 * </pre>
 */
public final class ClipLoader {
    private ClipLoader() {
    }

    public static Map<String, Clip> parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    public static Map<String, Clip> parse(JsonObject root) {
        Map<String, Clip> out = new LinkedHashMap<>();
        JsonObject anims = root.has("animations") ? root.getAsJsonObject("animations") : root;
        for (Map.Entry<String, JsonElement> e : anims.entrySet()) {
            if (!e.getValue().isJsonObject()) {
                continue;
            }
            try {
                out.put(e.getKey(), clip(e.getKey(), e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("animación " + e.getKey() + ": " + ex.getMessage(), ex);
            }
        }
        return out;
    }

    /** Biblioteca con nombre largo ("animation.persona.walk") y corto (lo que va después del último punto: "walk"). */
    public static Clip.Library library(Map<String, Clip> clips) {
        Map<String, Clip> all = new LinkedHashMap<>(clips);
        for (Clip c : clips.values()) {
            int dot = c.name.lastIndexOf('.');
            if (dot >= 0) {
                all.putIfAbsent(c.name.substring(dot + 1), c);
            }
        }
        Map<String, Clip> ro = Collections.unmodifiableMap(all);
        return new Clip.Library() {
            @Override
            public Clip get(String name) {
                return ro.get(name);
            }

            @Override
            public Map<String, Clip> all() {
                return ro;
            }
        };
    }

    static Clip clip(String name, JsonObject a) {
        Clip.Loop loop = Clip.Loop.ONCE;
        if (a.has("loop")) {
            JsonElement l = a.get("loop");
            if (l.isJsonPrimitive() && l.getAsJsonPrimitive().isBoolean()) {
                loop = l.getAsBoolean() ? Clip.Loop.LOOP : Clip.Loop.ONCE;
            } else if ("hold_on_last_frame".equals(l.getAsString())) {
                loop = Clip.Loop.HOLD;
            } else if ("true".equals(l.getAsString())) {
                loop = Clip.Loop.LOOP;
            }
        }
        List<Clip.Track> tracks = new ArrayList<>();
        float last = 0;
        if (a.has("bones")) {
            for (Map.Entry<String, JsonElement> b : a.getAsJsonObject("bones").entrySet()) {
                JsonObject o = b.getValue().getAsJsonObject();
                Clip.Channel r = channel(o.get("rotation"), 0);
                Clip.Channel p = channel(o.get("position"), 0);
                Clip.Channel s = channel(o.get("scale"), 1);
                for (Clip.Channel c : new Clip.Channel[] {r, p, s}) {
                    if (c != null) {
                        last = Math.max(last, c.keys[c.keys.length - 1].t);
                    }
                }
                tracks.add(new Clip.Track(b.getKey(), r, p, s));
            }
        }
        List<Clip.Event> events = new ArrayList<>();
        events(a, "sound_effects", "sound", events);
        events(a, "particle_effects", "particle", events);
        events(a, "timeline", "timeline", events);
        events.sort(Comparator.comparingDouble(ev -> ev.t));
        for (Clip.Event ev : events) {
            last = Math.max(last, ev.t);
        }
        float len = a.has("animation_length") ? a.get("animation_length").getAsFloat() : last;
        return new Clip(name, len, loop, tracks.toArray(new Clip.Track[0]), events.toArray(new Clip.Event[0]));
    }

    private static void events(JsonObject a, String key, String kind, List<Clip.Event> out) {
        if (!a.has(key)) {
            return;
        }
        for (Map.Entry<String, JsonElement> e : a.getAsJsonObject(key).entrySet()) {
            float t = Float.parseFloat(e.getKey());
            List<JsonElement> items = new ArrayList<>();
            if (e.getValue().isJsonArray()) {
                e.getValue().getAsJsonArray().forEach(items::add);
            } else {
                items.add(e.getValue());
            }
            for (JsonElement it : items) {
                if (it.isJsonObject()) {
                    JsonObject o = it.getAsJsonObject();
                    String v = o.has("effect") ? o.get("effect").getAsString() : "";
                    String loc = o.has("locator") ? o.get("locator").getAsString() : null;
                    out.add(new Clip.Event(t, kind, v, loc));
                } else {
                    out.add(new Clip.Event(t, kind, it.getAsString(), null));
                }
            }
        }
    }

    /** Un canal: un valor fijo, un vector o cuadros clave por tiempo. */
    static Clip.Channel channel(JsonElement e, float def) {
        if (e == null || e.isJsonNull()) {
            return null;
        }
        if (e.isJsonPrimitive() || e.isJsonArray()) {
            Expr[] v = vec(e, def);
            return new Clip.Channel(new Clip.Key[] {new Clip.Key(0, v, v, Clip.Lerp.LINEAR, Ease.LINEAR)});
        }
        JsonObject o = e.getAsJsonObject();
        // un solo cuadro escrito como objeto ({"vector": [...]}) sin tiempos
        if (o.has("vector") || o.has("pre") || o.has("post")) {
            return new Clip.Channel(new Clip.Key[] {key(0, o, def)});
        }
        List<Clip.Key> keys = new ArrayList<>();
        for (Map.Entry<String, JsonElement> k : o.entrySet()) {
            float t = Float.parseFloat(k.getKey());
            JsonElement v = k.getValue();
            if (v.isJsonObject()) {
                keys.add(key(t, v.getAsJsonObject(), def));
            } else {
                Expr[] x = vec(v, def);
                keys.add(new Clip.Key(t, x, x, Clip.Lerp.LINEAR, Ease.LINEAR));
            }
        }
        if (keys.isEmpty()) {
            return null;
        }
        keys.sort(Comparator.comparingDouble(k -> k.t));
        return new Clip.Channel(keys.toArray(new Clip.Key[0]));
    }

    private static Clip.Key key(float t, JsonObject o, float def) {
        Expr[] pre, post;
        if (o.has("vector")) {
            pre = post = vec(o.get("vector"), def);
        } else {
            post = o.has("post") ? vec(o.get("post"), def) : null;
            pre = o.has("pre") ? vec(o.get("pre"), def) : post;
            if (post == null) {
                post = pre;
            }
            if (pre == null) {
                pre = post = vec(new JsonPrimitive(def), def);
            }
        }
        Clip.Lerp lerp = Clip.Lerp.LINEAR;
        if (o.has("lerp_mode")) {
            String m = o.get("lerp_mode").getAsString().toLowerCase(Locale.ROOT);
            lerp = m.equals("catmullrom") ? Clip.Lerp.CATMULLROM : m.equals("step") ? Clip.Lerp.STEP : Clip.Lerp.LINEAR;
        }
        Ease.Fn ease = o.has("easing") ? Ease.get(o.get("easing").getAsString()) : Ease.LINEAR;
        if (o.has("easing") && "catmullrom".equalsIgnoreCase(o.get("easing").getAsString())) {
            lerp = Clip.Lerp.CATMULLROM;
        }
        if (o.has("easing") && "step".equalsIgnoreCase(o.get("easing").getAsString())) {
            lerp = Clip.Lerp.STEP;
        }
        return new Clip.Key(t, pre, post, lerp, ease);
    }

    /** [x, y, z] (números o expresiones) o un solo valor para los tres. */
    static Expr[] vec(JsonElement e, float def) {
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            Expr[] v = new Expr[3];
            for (int i = 0; i < 3; i++) {
                v[i] = i < a.size() ? one(a.get(i)) : Expr.num(def);
            }
            return v;
        }
        Expr x = one(e);
        return new Expr[] {x, x, x};
    }

    private static Expr one(JsonElement e) {
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isNumber()) {
            return Expr.num(p.getAsDouble());
        }
        String s = p.getAsString().trim();
        if (s.isEmpty()) {
            return Expr.num(0);
        }
        return Expr.parse(s);
    }
}
