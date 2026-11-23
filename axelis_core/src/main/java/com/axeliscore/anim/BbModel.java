package com.axeliscore.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lee un proyecto de Blockbench (.bbmodel) tal cual, sin exportar: el modelo (grupos y cubos), las animaciones (con sus curvas Bézier,
 * pasos y catmullrom, sonidos, partículas y línea de tiempo) y las texturas que lleva dentro. Lo convierte al mismo formato que exporta
 * Blockbench para Bedrock y GeckoLib, así que se ve y se mueve igual que en Blockbench.
 * <p>
 * Convenciones de Blockbench al exportar a Bedrock (las mismas que se usan aquí): la x de los cubos y de los pivotes se voltea, los giros
 * de los grupos y cubos cambian de signo en x e y, y las caras de arriba y abajo llevan la UV al revés. Los valores de las animaciones
 * ya están guardados como en Bedrock.
 */
public final class BbModel {
    /** Lo que trae un .bbmodel, ya convertido. */
    public static final class Resultado {
        /** Modelo en formato Bedrock (para {@link GeoLoader}). */
        public final JsonObject geo;
        /** Animaciones en formato Bedrock (para {@link ClipLoader}); vacío si no tiene. */
        public final JsonObject animaciones;
        /** Texturas PNG que lleva dentro (en orden), con su nombre. */
        public final List<Textura> texturas;
        /** Avisos (cosas que no se pudieron leer, como mallas libres). */
        public final List<String> avisos;

        Resultado(JsonObject geo, JsonObject animaciones, List<Textura> texturas, List<String> avisos) {
            this.geo = geo;
            this.animaciones = animaciones;
            this.texturas = texturas;
            this.avisos = avisos;
        }
    }

    public record Textura(String nombre, byte[] png) {
    }

    private BbModel() {
    }

    public static Resultado parse(String json) {
        return parse(JsonParser.parseString(json).getAsJsonObject());
    }

    public static Resultado parse(JsonObject bb) {
        List<String> avisos = new ArrayList<>();
        String nombre = bb.has("name") ? bb.get("name").getAsString() : "modelo";
        if (bb.has("model_identifier") && !bb.get("model_identifier").getAsString().isEmpty()) {
            nombre = bb.get("model_identifier").getAsString();
        }
        int tw = 64, th = 64;
        if (bb.has("resolution")) {
            JsonObject r = bb.getAsJsonObject("resolution");
            tw = r.has("width") ? r.get("width").getAsInt() : 64;
            th = r.has("height") ? r.get("height").getAsInt() : 64;
        }
        boolean boxUvProyecto = bb.has("meta") && bb.getAsJsonObject("meta").has("box_uv") && bb.getAsJsonObject("meta").get("box_uv").getAsBoolean();
        // cubos por uuid
        Map<String, JsonObject> elementos = new HashMap<>();
        if (bb.has("elements")) {
            for (JsonElement e : bb.getAsJsonArray("elements")) {
                JsonObject o = e.getAsJsonObject();
                elementos.put(o.get("uuid").getAsString(), o);
            }
        }
        // desde Blockbench 5 los datos de los grupos van aparte ("groups") y el árbol ("outliner") solo lleva uuid e hijos
        Map<String, JsonObject> grupos = new HashMap<>();
        if (bb.has("groups")) {
            for (JsonElement e : bb.getAsJsonArray("groups")) {
                JsonObject o = e.getAsJsonObject();
                grupos.put(o.get("uuid").getAsString(), o);
            }
        }
        JsonArray huesos = new JsonArray();
        Map<String, String> nombrePorUuid = new HashMap<>();
        JsonArray raiz = bb.has("outliner") ? bb.getAsJsonArray("outliner") : new JsonArray();
        // cubos sueltos (fuera de un grupo) van a un hueso "root"
        JsonObject sueltos = null;
        for (JsonElement e : raiz) {
            if (e.isJsonObject()) {
                grupo(e.getAsJsonObject(), null, elementos, grupos, huesos, nombrePorUuid, boxUvProyecto, avisos);
            } else if (e.isJsonPrimitive()) {
                JsonObject el = elementos.get(e.getAsString());
                if (el == null) {
                    continue;
                }
                if (sueltos == null) {
                    sueltos = new JsonObject();
                    sueltos.addProperty("name", "bb_sueltos");
                    sueltos.add("pivot", arr(0, 0, 0));
                    sueltos.add("cubes", new JsonArray());
                    huesos.add(sueltos);
                }
                JsonObject c = cubo(el, boxUvProyecto, avisos);
                if (c != null) {
                    sueltos.getAsJsonArray("cubes").add(c);
                }
            }
        }
        JsonObject desc = new JsonObject();
        desc.addProperty("identifier", nombre.startsWith("geometry.") ? nombre : "geometry." + nombre);
        desc.addProperty("texture_width", tw);
        desc.addProperty("texture_height", th);
        JsonObject g = new JsonObject();
        g.add("description", desc);
        g.add("bones", huesos);
        JsonArray gs = new JsonArray();
        gs.add(g);
        JsonObject geo = new JsonObject();
        geo.addProperty("format_version", "1.12.0");
        geo.add("minecraft:geometry", gs);

        JsonObject anims = new JsonObject();
        if (bb.has("animations")) {
            for (JsonElement ae : bb.getAsJsonArray("animations")) {
                JsonObject a = ae.getAsJsonObject();
                try {
                    anims.add(a.get("name").getAsString(), animacion(a, nombrePorUuid));
                } catch (RuntimeException ex) {
                    avisos.add("animación " + a.get("name") + ": " + ex.getMessage());
                }
            }
        }
        JsonObject animRoot = new JsonObject();
        animRoot.addProperty("format_version", "1.8.0");
        animRoot.add("animations", anims);

        List<Textura> texturas = new ArrayList<>();
        if (bb.has("textures")) {
            for (JsonElement te : bb.getAsJsonArray("textures")) {
                JsonObject t = te.getAsJsonObject();
                String src = t.has("source") ? t.get("source").getAsString() : "";
                int coma = src.indexOf("base64,");
                if (coma < 0) {
                    avisos.add("la textura " + (t.has("name") ? t.get("name").getAsString() : "?") + " no va dentro del archivo");
                    continue;
                }
                try {
                    texturas.add(new Textura(t.has("name") ? t.get("name").getAsString() : "textura", Base64.getDecoder().decode(src.substring(coma + 7))));
                } catch (IllegalArgumentException ex) {
                    avisos.add("textura mal guardada: " + ex.getMessage());
                }
            }
        }
        return new Resultado(geo, animRoot, texturas, avisos);
    }

    // ------------------------------------------------------------------ modelo

    private static void grupo(JsonObject nodo, String padre, Map<String, JsonObject> elementos, Map<String, JsonObject> grupos, JsonArray out,
            Map<String, String> nombres, boolean boxUv, List<String> avisos) {
        JsonObject gr = nodo;
        if (!nodo.has("name") && nodo.has("uuid") && grupos.containsKey(nodo.get("uuid").getAsString())) {
            gr = grupos.get(nodo.get("uuid").getAsString());
        }
        String nombre = gr.has("name") ? gr.get("name").getAsString() : "grupo";
        float[] o = f3(gr, "origin", 0);
        float[] r = f3(gr, "rotation", 0);
        JsonObject b = new JsonObject();
        b.addProperty("name", nombre);
        if (padre != null) {
            b.addProperty("parent", padre);
        }
        b.add("pivot", arr(-o[0], o[1], o[2]));
        if (r[0] != 0 || r[1] != 0 || r[2] != 0) {
            b.add("rotation", arr(-r[0], -r[1], r[2]));
        }
        if (gr.has("uuid")) {
            nombres.put(gr.get("uuid").getAsString(), nombre);
        }
        JsonArray cubos = new JsonArray();
        out.add(b);
        if (nodo.has("children")) {
            for (JsonElement ch : nodo.getAsJsonArray("children")) {
                if (ch.isJsonObject()) {
                    grupo(ch.getAsJsonObject(), nombre, elementos, grupos, out, nombres, boxUv, avisos);
                } else {
                    JsonObject el = elementos.get(ch.getAsString());
                    if (el != null) {
                        JsonObject c = cubo(el, boxUv, avisos);
                        if (c != null) {
                            cubos.add(c);
                        }
                    }
                }
            }
        }
        if (cubos.size() > 0) {
            b.add("cubes", cubos);
        }
    }

    private static JsonObject cubo(JsonObject el, boolean boxUvProyecto, List<String> avisos) {
        String tipo = el.has("type") ? el.get("type").getAsString() : "cube";
        if (!tipo.equals("cube")) {
            if (tipo.equals("mesh")) {
                avisos.add("la malla '" + (el.has("name") ? el.get("name").getAsString() : "?") + "' no se usa (solo cubos)");
            }
            return null;
        }
        if (el.has("export") && !el.get("export").getAsBoolean()) {
            return null;
        }
        float[] from = f3(el, "from", 0), to = f3(el, "to", 0);
        float[] size = {to[0] - from[0], to[1] - from[1], to[2] - from[2]};
        JsonObject c = new JsonObject();
        c.add("origin", arr(-(from[0] + size[0]), from[1], from[2]));
        c.add("size", arr(size[0], size[1], size[2]));
        if (el.has("inflate") && el.get("inflate").getAsFloat() != 0) {
            c.addProperty("inflate", el.get("inflate").getAsFloat());
        }
        float[] r = f3(el, "rotation", 0);
        if (r[0] != 0 || r[1] != 0 || r[2] != 0) {
            float[] p = f3(el, "origin", 0);
            c.add("pivot", arr(-p[0], p[1], p[2]));
            c.add("rotation", arr(-r[0], -r[1], r[2]));
        }
        boolean boxUv = el.has("box_uv") ? el.get("box_uv").getAsBoolean() : boxUvProyecto;
        if (el.has("mirror_uv") && el.get("mirror_uv").getAsBoolean()) {
            c.addProperty("mirror", true);
        }
        if (boxUv) {
            float[] uo = el.has("uv_offset") ? f2(el.getAsJsonArray("uv_offset")) : new float[2];
            c.add("uv", arr(uo[0], uo[1]));
        } else if (el.has("faces")) {
            JsonObject faces = el.getAsJsonObject("faces"), uv = new JsonObject();
            for (String k : new String[] {"north", "east", "south", "west", "up", "down"}) {
                if (!faces.has(k)) {
                    continue;
                }
                JsonObject f = faces.getAsJsonObject(k);
                if (f.has("texture") && f.get("texture").isJsonNull()) {
                    continue; // cara sin textura: no se dibuja
                }
                if (!f.has("uv")) {
                    continue;
                }
                JsonArray u = f.getAsJsonArray("uv");
                float u0 = u.get(0).getAsFloat(), v0 = u.get(1).getAsFloat(), u1 = u.get(2).getAsFloat(), v1 = u.get(3).getAsFloat();
                float[] pos = {u0, v0}, sz = {u1 - u0, v1 - v0};
                if (k.equals("up") || k.equals("down")) {
                    pos = new float[] {u0 + sz[0], v0 + sz[1]};
                    sz = new float[] {-sz[0], -sz[1]};
                }
                JsonObject fo = new JsonObject();
                fo.add("uv", arr(pos[0], pos[1]));
                fo.add("uv_size", arr(sz[0], sz[1]));
                uv.add(k, fo);
            }
            c.add("uv", uv);
        }
        return c;
    }

    // ------------------------------------------------------------------ animaciones

    private static JsonObject animacion(JsonObject a, Map<String, String> nombres) {
        JsonObject out = new JsonObject();
        String loop = a.has("loop") ? a.get("loop").getAsString() : "once";
        switch (loop) {
            case "loop" -> out.addProperty("loop", true);
            case "hold" -> out.addProperty("loop", "hold_on_last_frame");
            default -> out.addProperty("loop", false);
        }
        if (a.has("length")) {
            out.addProperty("animation_length", a.get("length").getAsFloat());
        }
        JsonObject bones = new JsonObject(), sonidos = new JsonObject(), particulas = new JsonObject(), linea = new JsonObject();
        if (a.has("animators")) {
            for (Map.Entry<String, JsonElement> en : a.getAsJsonObject("animators").entrySet()) {
                JsonObject an = en.getValue().getAsJsonObject();
                String tipo = an.has("type") ? an.get("type").getAsString() : "bone";
                JsonArray kfs = an.has("keyframes") ? an.getAsJsonArray("keyframes") : new JsonArray();
                if (tipo.equals("effect")) {
                    for (JsonElement ke : kfs) {
                        efecto(ke.getAsJsonObject(), sonidos, particulas, linea);
                    }
                    continue;
                }
                if (!tipo.equals("bone")) {
                    continue;
                }
                String hueso = nombres.getOrDefault(en.getKey(), an.has("name") ? an.get("name").getAsString() : en.getKey());
                JsonObject b = new JsonObject();
                for (JsonElement ke : kfs) {
                    JsonObject k = ke.getAsJsonObject();
                    String canal = k.get("channel").getAsString();
                    if (!canal.equals("rotation") && !canal.equals("position") && !canal.equals("scale")) {
                        continue;
                    }
                    if (!b.has(canal)) {
                        b.add(canal, new JsonObject());
                    }
                    b.getAsJsonObject(canal).add(tiempo(k.get("time").getAsFloat()), cuadro(k, canal));
                }
                if (b.size() > 0) {
                    bones.add(hueso, b);
                }
            }
        }
        out.add("bones", bones);
        if (sonidos.size() > 0) {
            out.add("sound_effects", sonidos);
        }
        if (particulas.size() > 0) {
            out.add("particle_effects", particulas);
        }
        if (linea.size() > 0) {
            out.add("timeline", linea);
        }
        return out;
    }

    /**
     * Un cuadro clave. Blockbench guarda los giros con x e y al revés que en Bedrock, y la posición con la x al revés (comprobado con su
     * propio exportador): aquí se voltean igual, también en las manijas Bézier.
     */
    private static JsonObject cuadro(JsonObject k, String canal) {
        float def = canal.equals("scale") ? 1 : 0;
        boolean[] voltear = canal.equals("rotation") ? new boolean[] {true, true, false}
                : canal.equals("position") ? new boolean[] {true, false, false} : new boolean[3];
        JsonArray dps = k.getAsJsonArray("data_points");
        JsonObject o = new JsonObject();
        JsonArray pre = vec(dps.get(0).getAsJsonObject(), def, voltear);
        if (dps.size() > 1) {
            o.add("pre", pre);
            o.add("post", vec(dps.get(1).getAsJsonObject(), def, voltear));
        } else {
            o.add("vector", pre);
        }
        String interp = k.has("interpolation") ? k.get("interpolation").getAsString() : "linear";
        switch (interp) {
            case "catmullrom" -> o.addProperty("lerp_mode", "catmullrom");
            case "step" -> o.addProperty("lerp_mode", "step");
            case "bezier" -> {
                JsonObject bz = new JsonObject();
                bz.add("lt", f3arr(k, "bezier_left_time", -0.1F));
                bz.add("lv", voltear(f3arr(k, "bezier_left_value", 0), voltear));
                bz.add("rt", f3arr(k, "bezier_right_time", 0.1F));
                bz.add("rv", voltear(f3arr(k, "bezier_right_value", 0), voltear));
                o.add("axelis_bezier", bz);
            }
            default -> {
            }
        }
        // curvas del plugin de GeckoLib
        if (k.has("easing")) {
            o.add("easing", k.get("easing"));
        }
        if (k.has("easingArgs")) {
            o.add("easingArgs", k.get("easingArgs"));
        }
        return o;
    }

    private static void efecto(JsonObject k, JsonObject sonidos, JsonObject particulas, JsonObject linea) {
        String canal = k.get("channel").getAsString();
        String t = tiempo(k.get("time").getAsFloat());
        JsonArray dps = k.has("data_points") ? k.getAsJsonArray("data_points") : new JsonArray();
        for (JsonElement de : dps) {
            JsonObject d = de.getAsJsonObject();
            switch (canal) {
                case "sound" -> {
                    JsonObject s = new JsonObject();
                    s.addProperty("effect", d.has("effect") ? d.get("effect").getAsString() : "");
                    agregar(sonidos, t, s);
                }
                case "particle" -> {
                    JsonObject p = new JsonObject();
                    p.addProperty("effect", d.has("effect") ? d.get("effect").getAsString() : "");
                    if (d.has("locator") && !d.get("locator").getAsString().isEmpty()) {
                        p.addProperty("locator", d.get("locator").getAsString());
                    }
                    agregar(particulas, t, p);
                }
                case "timeline" -> {
                    String sc = d.has("script") ? d.get("script").getAsString() : "";
                    if (!sc.isEmpty()) {
                        agregar(linea, t, new JsonPrimitive(sc));
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void agregar(JsonObject o, String t, JsonElement v) {
        if (!o.has(t)) {
            o.add(t, v);
            return;
        }
        JsonElement ya = o.get(t);
        JsonArray a;
        if (ya.isJsonArray()) {
            a = ya.getAsJsonArray();
        } else {
            a = new JsonArray();
            a.add(ya);
            o.add(t, a);
        }
        a.add(v);
    }

    private static String tiempo(float t) {
        // como Blockbench: hasta 4 decimales, sin ceros de más
        String s = String.format(java.util.Locale.ROOT, "%.4f", t);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    private static JsonArray vec(JsonObject d, float def, boolean[] voltear) {
        JsonArray a = new JsonArray();
        String[] ejes = {"x", "y", "z"};
        for (int i = 0; i < 3; i++) {
            String ax = ejes[i];
            float sg = voltear[i] ? -1 : 1;
            if (!d.has(ax)) {
                a.add(def * (voltear[i] && def != 0 ? -1 : 1));
                continue;
            }
            JsonElement v = d.get(ax);
            if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isNumber()) {
                a.add(v.getAsFloat() * sg);
            } else {
                String s = v.getAsString().trim();
                if (s.isEmpty()) {
                    a.add(def);
                } else {
                    try {
                        a.add(Float.parseFloat(s) * sg);
                    } catch (NumberFormatException e) {
                        a.add(voltear[i] ? "-(" + s + ")" : s); // expresión (Molang)
                    }
                }
            }
        }
        return a;
    }

    private static JsonArray voltear(JsonArray a, boolean[] v) {
        JsonArray r = new JsonArray();
        for (int i = 0; i < a.size(); i++) {
            r.add(a.get(i).getAsFloat() * (i < v.length && v[i] ? -1 : 1));
        }
        return r;
    }

    // ------------------------------------------------------------------ ayudas

    private static float[] f3(JsonObject o, String k, float def) {
        float[] r = {def, def, def};
        if (o.has(k) && o.get(k).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(k);
            for (int i = 0; i < Math.min(3, a.size()); i++) {
                r[i] = a.get(i).getAsFloat();
            }
        }
        return r;
    }

    private static JsonArray f3arr(JsonObject o, String k, float def) {
        float[] v = f3(o, k, def);
        return arr(v[0], v[1], v[2]);
    }

    private static float[] f2(JsonArray a) {
        return new float[] {a.size() > 0 ? a.get(0).getAsFloat() : 0, a.size() > 1 ? a.get(1).getAsFloat() : 0};
    }

    private static JsonArray arr(float... v) {
        JsonArray a = new JsonArray();
        for (float f : v) {
            a.add(f);
        }
        return a;
    }
}
