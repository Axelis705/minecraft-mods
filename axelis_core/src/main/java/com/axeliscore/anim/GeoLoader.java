package com.axeliscore.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lee modelos .geo.json de Blockbench (formato Bedrock 1.12.0 y el viejo 1.8.0) y los pasa al espacio de Minecraft Java:
 * x igual, y = 24 - y (en Java la y crece hacia abajo), z igual; los ángulos en grados pasan a radianes con el mismo signo (así un
 * brazo a -90 en Blockbench queda hacia delante, igual que en los modelos de Minecraft).
 * <p>
 * Extensiones propias (opcionales, dentro del objeto de la geometría):
 * <pre>
 *   "axelis_bends": [{"bone": "right_arm", "child": "right_forearm", "at": 18}]   // codo: a la altura 18 (de Blockbench)
 *   "axelis_glow": ["eyes", "crystal"]                                         // huesos que brillan en la oscuridad
 * </pre>
 */
public final class GeoLoader {
    private GeoLoader() {
    }

    public static Geo parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    public static Geo parse(JsonObject root) {
        JsonObject g;
        String id;
        int tw, th;
        if (root.has("minecraft:geometry")) {
            g = root.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
            JsonObject d = g.getAsJsonObject("description");
            id = str(d, "identifier", "geometry.unknown");
            tw = (int) num(d, "texture_width", 64);
            th = (int) num(d, "texture_height", 64);
        } else {
            String key = null;
            for (String k : root.keySet()) {
                if (k.startsWith("geometry.")) {
                    key = k;
                    break;
                }
            }
            if (key == null) {
                throw new IllegalArgumentException("no hay geometría en el archivo");
            }
            g = root.getAsJsonObject(key);
            id = key;
            tw = (int) num(g, "texturewidth", 64);
            th = (int) num(g, "textureheight", 64);
        }
        JsonArray arr = g.getAsJsonArray("bones");
        Set<String> glow = new HashSet<>();
        if (g.has("axelis_glow")) {
            for (JsonElement e : g.getAsJsonArray("axelis_glow")) {
                glow.add(e.getAsString());
            }
        }
        // primero los nombres, para resolver los padres en cualquier orden
        List<JsonObject> raw = new ArrayList<>();
        Map<String, Integer> idx = new HashMap<>();
        for (JsonElement e : arr) {
            JsonObject b = e.getAsJsonObject();
            idx.put(b.get("name").getAsString(), raw.size());
            raw.add(b);
        }
        Geo.Bone[] bones = new Geo.Bone[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            JsonObject b = raw.get(i);
            String name = b.get("name").getAsString();
            int parent = b.has("parent") ? idx.getOrDefault(b.get("parent").getAsString(), -1) : -1;
            float[] piv = vec(b, "pivot", 0, 0, 0);
            float[] rot = vec(b, "rotation", 0, 0, 0);
            if (!b.has("rotation") && b.has("bind_pose_rotation")) {
                rot = vec(b, "bind_pose_rotation", 0, 0, 0);
            }
            boolean boneMirror = b.has("mirror") && b.get("mirror").getAsBoolean();
            float boneInflate = b.has("inflate") ? b.get("inflate").getAsFloat() : 0;
            List<Geo.Cube> cubes = new ArrayList<>();
            if (b.has("cubes")) {
                for (JsonElement ce : b.getAsJsonArray("cubes")) {
                    cubes.add(cube(ce.getAsJsonObject(), boneMirror, boneInflate));
                }
            }
            bones[i] = new Geo.Bone(name, parent, piv[0], 24 - piv[1], piv[2], rad(rot[0]), rad(rot[1]), rad(rot[2]), cubes, glow.contains(name));
        }
        Geo geo = new Geo(id, tw, th, bones);
        if (g.has("axelis_bends")) {
            for (JsonElement e : g.getAsJsonArray("axelis_bends")) {
                JsonObject o = e.getAsJsonObject();
                int bone = geo.bone(o.get("bone").getAsString());
                int child = geo.bone(o.get("child").getAsString());
                if (bone < 0 || child < 0) {
                    throw new IllegalArgumentException("doblez con un hueso que no existe: " + o);
                }
                float at = 24 - o.get("at").getAsFloat();
                for (Geo.Cube c : geo.bones[bone].cubes) {
                    if (at > c.y0 && at < c.y0 + c.sy) {
                        c.bendBone = child;
                        c.bendY = at;
                    }
                }
            }
        }
        return geo;
    }

    private static Geo.Cube cube(JsonObject c, boolean boneMirror, float boneInflate) {
        float[] o = vec(c, "origin", 0, 0, 0);
        float[] s = vec(c, "size", 0, 0, 0);
        float inflate = c.has("inflate") ? c.get("inflate").getAsFloat() : boneInflate;
        boolean mirror = c.has("mirror") ? c.get("mirror").getAsBoolean() : boneMirror;
        float[] rot = vec(c, "rotation", 0, 0, 0);
        float[] piv = vec(c, "pivot", 0, 0, 0);
        float u = 0, v = 0;
        float[][] faces = null;
        JsonElement uv = c.get("uv");
        if (uv != null && uv.isJsonArray()) {
            u = uv.getAsJsonArray().get(0).getAsFloat();
            v = uv.getAsJsonArray().get(1).getAsFloat();
        } else if (uv != null && uv.isJsonObject()) {
            JsonObject f = uv.getAsJsonObject();
            // orden de las caras como en ModelPart: abajo (en Java; es la de ARRIBA de Blockbench), arriba, oeste, norte, este, sur.
            // Las regiones son las mismas que usaría el UV de caja: "east" de Blockbench es la primera franja lateral (oeste en Java).
            faces = new float[6][];
            faces[0] = face(f, "up", false);
            faces[1] = face(f, "down", true);
            faces[2] = face(f, "east", false);
            faces[3] = face(f, "north", false);
            faces[4] = face(f, "west", false);
            faces[5] = face(f, "south", false);
        }
        return new Geo.Cube(o[0], 24 - (o[1] + s[1]), o[2], s[0], s[1], s[2], inflate, u, v, mirror, rad(rot[0]), rad(rot[1]), rad(rot[2]),
                piv[0], 24 - piv[1], piv[2], faces);
    }

    private static float[] face(JsonObject f, String name, boolean flipV) {
        if (!f.has(name)) {
            return null;
        }
        JsonObject o = f.getAsJsonObject(name);
        float[] uv = vec(o, "uv", 0, 0);
        float[] sz = vec(o, "uv_size", 0, 0);
        return flipV ? new float[] {uv[0], uv[1] + sz[1], uv[0] + sz[0], uv[1]} : new float[] {uv[0], uv[1], uv[0] + sz[0], uv[1] + sz[1]};
    }

    static float rad(float deg) {
        return (float) Math.toRadians(deg);
    }

    private static float[] vec(JsonObject o, String key, float... def) {
        if (!o.has(key) || !o.get(key).isJsonArray()) {
            return def.clone();
        }
        JsonArray a = o.getAsJsonArray(key);
        float[] r = def.clone();
        for (int i = 0; i < Math.min(r.length, a.size()); i++) {
            r[i] = a.get(i).getAsFloat();
        }
        return r;
    }

    private static String str(JsonObject o, String k, String def) {
        return o.has(k) ? o.get(k).getAsString() : def;
    }

    private static double num(JsonObject o, String k, double def) {
        return o.has(k) ? o.get(k).getAsDouble() : def;
    }
}
