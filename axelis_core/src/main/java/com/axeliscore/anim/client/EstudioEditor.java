package com.axeliscore.anim.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.anim.AnimHost;
import com.axeliscore.anim.Animated;
import com.axeliscore.anim.Animator;
import com.axeliscore.anim.Clip;
import com.axeliscore.anim.ClipLoader;
import com.axeliscore.anim.Geo;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * (Cliente) El editor de animaciones dentro del juego: toma el control de una entidad animada (deja de hacer lo suyo), reproduce la
 * animación elegida en el tiempo que se quiera y cambia los cuadros clave. Lo que se cambia se ve al instante y se guarda en la carpeta
 * del estudio como {@code nombre.animation.json} (se abre igual en Blockbench).
 */
public final class EstudioEditor {
    static Entity objetivo;
    static GeoEntityRenderer<?> renderer;
    static String base = "", clip = "", hueso = "";
    static JsonObject doc;
    static float tiempo, velocidad = 1, giro = Float.NaN;
    static boolean reproducir = true, codigo, cambios;
    static String aviso = "";

    private EstudioEditor() {
    }

    public static boolean activo() {
        return objetivo != null && objetivo.isAlive();
    }

    public static Entity objetivo() {
        return objetivo;
    }

    /** Elige un hueso (por nombre). */
    public static void elegirHueso(String nombre) {
        hueso = nombre == null ? "" : nombre;
    }

    /** Elige una animación (por nombre completo o corto). */
    public static boolean elegirClip(String nombre) {
        for (String c : clips()) {
            if (c.equals(nombre) || c.endsWith("." + nombre)) {
                clip = c;
                tiempo = 0;
                return true;
            }
        }
        return false;
    }

    /** Pausa en ese segundo. */
    public static void irA(float t) {
        reproducir = false;
        tiempo = Math.max(0, Math.min(largo(), t));
    }

    public static String hueso() {
        return hueso;
    }

    public static String clip() {
        return clip;
    }

    public static boolean hayCambios() {
        return cambios;
    }

    /** Empieza a editar esa entidad (debe dibujarse con un {@link GeoEntityRenderer}). Devuelve false si no se puede. */
    public static boolean empezar(Entity e) {
        if (!(e instanceof Animated)) {
            return false;
        }
        var r = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(e);
        if (!(r instanceof GeoEntityRenderer<?> g) || g.animIds().length == 0) {
            return false;
        }
        objetivo = e;
        renderer = g;
        ResourceLocation anim = g.animIds()[0];
        base = EstudioArchivos.base(anim);
        String txt = EstudioArchivos.animaciones(anim);
        if (txt == null) {
            txt = GeoAssets.texto(anim);
        }
        try {
            doc = txt == null ? new JsonObject() : JsonParser.parseString(txt).getAsJsonObject();
        } catch (RuntimeException ex) {
            doc = new JsonObject();
        }
        if (!doc.has("animations")) {
            doc.addProperty("format_version", "1.8.0");
            doc.add("animations", new JsonObject());
        }
        List<String> cs = clips();
        clip = cs.isEmpty() ? "" : cs.get(0);
        hueso = "";
        tiempo = 0;
        reproducir = true;
        velocidad = 1;
        giro = Float.NaN;
        cambios = false;
        aviso = "";
        return true;
    }

    public static void terminar() {
        AnimHost h = host();
        if (h != null && h.ready()) {
            h.animator().stop("estudio", 0.2F);
            h.animator().pausar(false);
            h.animator().escala(1);
        }
        objetivo = null;
        renderer = null;
    }

    // ------------------------------------------------------------------ lo que usa el renderizador

    /** ¿El editor maneja a esta entidad? Si sí, pone la animación elegida en el tiempo elegido. */
    static boolean controla(Entity e, AnimHost h) {
        if (e != objetivo || objetivo == null) {
            return false;
        }
        Animator a = h.animator();
        for (Animator.Layer l : a.capas()) {
            if (!l.name.equals("estudio")) {
                l.peso(0);
            }
        }
        Clip c = h.clip(clip);
        Animator.Layer l = a.layer("estudio");
        if (c != null) {
            if (l.clip() != c) {
                a.play("estudio", c, 0, 1, true);
                l.tiempo(tiempo);
            }
            if (!reproducir) {
                l.tiempo(tiempo);
            } else {
                tiempo = l.time();
            }
        } else {
            a.stop("estudio", 0);
        }
        a.pausar(!reproducir);
        a.escala(velocidad);
        return true;
    }

    static boolean conCodigo() {
        return codigo;
    }

    static float giro(float yaw) {
        if (Float.isNaN(giro)) {
            giro = yaw;
        }
        return giro;
    }

    public static AnimHost host() {
        return objetivo instanceof Animated an ? an.axHost() : null;
    }

    public static Geo geo() {
        AnimHost h = host();
        return h == null || !h.ready() ? null : h.geo();
    }

    // ------------------------------------------------------------------ datos de las animaciones

    static JsonObject anims() {
        return doc.getAsJsonObject("animations");
    }

    public static List<String> clips() {
        return new ArrayList<>(anims().keySet());
    }

    /** Duración de la animación elegida (segundos). */
    public static float largo() {
        JsonObject a = anims().has(clip) ? anims().getAsJsonObject(clip) : null;
        if (a != null && a.has("animation_length")) {
            return Math.max(0.05F, a.get("animation_length").getAsFloat());
        }
        Clip c = clipVivo();
        return c == null ? 1F : Math.max(0.05F, c.length);
    }

    public static boolean enBucle() {
        JsonObject a = anims().has(clip) ? anims().getAsJsonObject(clip) : null;
        return a != null && a.has("loop") && a.get("loop").isJsonPrimitive() && a.get("loop").getAsJsonPrimitive().isBoolean()
                && a.get("loop").getAsBoolean();
    }

    private static JsonObject canalJson(String bone, String canal, boolean crear) {
        JsonObject a = anims().has(clip) ? anims().getAsJsonObject(clip) : null;
        if (a == null) {
            return null;
        }
        if (!a.has("bones")) {
            if (!crear) {
                return null;
            }
            a.add("bones", new JsonObject());
        }
        JsonObject bs = a.getAsJsonObject("bones");
        if (!bs.has(bone)) {
            if (!crear) {
                return null;
            }
            bs.add(bone, new JsonObject());
        }
        JsonObject b = bs.getAsJsonObject(bone);
        if (!b.has(canal)) {
            if (!crear) {
                return null;
            }
            b.add(canal, new JsonObject());
        }
        JsonElement ch = b.get(canal);
        if (!ch.isJsonObject() || ch.getAsJsonObject().has("vector") || ch.getAsJsonObject().has("pre") || ch.getAsJsonObject().has("post")) {
            // un valor fijo: se vuelve cuadro clave en 0 para poder agregar más
            if (!crear) {
                JsonObject fake = new JsonObject();
                fake.add("0.0", ch);
                return fake;
            }
            JsonObject o = new JsonObject();
            o.add("0.0", ch);
            b.add(canal, o);
            return o;
        }
        return ch.getAsJsonObject();
    }

    /** Tiempos de los cuadros clave de un hueso en un canal ("rotation", "position", "scale"). */
    public static List<Float> claves(String bone, String canal) {
        List<Float> r = new ArrayList<>();
        JsonObject ch = canalJson(bone, canal, false);
        if (ch == null) {
            return r;
        }
        for (String k : ch.keySet()) {
            try {
                r.add(Float.parseFloat(k));
            } catch (NumberFormatException ignored) {
                // claves raras
            }
        }
        r.sort(Float::compare);
        return r;
    }

    /** Valor del hueso en el tiempo actual (como en Blockbench: grados, píxeles, escala). */
    public static float[] valor(String bone, String canal) {
        float def = canal.equals("scale") ? 1 : 0;
        float[] out = {def, def, def};
        Clip c = clipVivo();
        if (c == null) {
            return out;
        }
        for (Clip.Track tr : c.tracks) {
            if (tr.bone.equals(bone)) {
                Clip.Channel ch = canal.equals("rotation") ? tr.rot : canal.equals("position") ? tr.pos : tr.scl;
                if (ch != null) {
                    ch.sample(tiempo, n -> 0, out);
                }
            }
        }
        return out;
    }

    private static Map<String, Clip> vivo;
    private static int vivoGen = -1;
    private static JsonObject vivoDoc;

    /**
     * La animación elegida tal como está ahora en el documento que se edita. La criatura recibe los cambios en el siguiente cuadro; los
     * controles los necesitan ya (si no, después de "+Clave" o "Quitar" mostrarían el valor de antes).
     */
    static Clip clipVivo() {
        if (doc != null && (vivo == null || vivoGen != EstudioArchivos.generacion() || vivoDoc != doc)) {
            try {
                vivo = ClipLoader.parse(doc);
            } catch (RuntimeException ex) {
                vivo = Map.of();
            }
            vivoGen = EstudioArchivos.generacion();
            vivoDoc = doc;
        }
        Clip c = vivo == null ? null : vivo.get(clip);
        if (c == null) {
            AnimHost h = host();
            c = h == null ? null : h.clip(clip);
        }
        return c;
    }

    static String t(float t) {
        String s = String.format(Locale.ROOT, "%.4f", t);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    /** Clave existente cerca del tiempo actual (a menos de medio cuadro), o null. */
    static String claveCerca(JsonObject ch) {
        for (String k : ch.keySet()) {
            try {
                if (Math.abs(Float.parseFloat(k) - tiempo) < 1 / 48F) {
                    return k;
                }
            } catch (NumberFormatException ignored) {
                // nada
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ efectos en la animación

    /** Los efectos de partículas de la animación elegida (tiempo y nombre), en orden. */
    public static List<Map.Entry<Float, String>> efectos() {
        List<Map.Entry<Float, String>> r = new ArrayList<>();
        JsonObject pe = efectosJson(false);
        if (pe == null) {
            return r;
        }
        for (Map.Entry<String, JsonElement> e : pe.entrySet()) {
            try {
                JsonElement v = e.getValue().isJsonArray() && e.getValue().getAsJsonArray().size() > 0 ? e.getValue().getAsJsonArray().get(0)
                        : e.getValue();
                String n = v.isJsonObject() ? (v.getAsJsonObject().has("effect") ? v.getAsJsonObject().get("effect").getAsString() : "")
                        : v.getAsString();
                r.add(Map.entry(Float.parseFloat(e.getKey()), n));
            } catch (RuntimeException ignored) {
                // claves raras
            }
        }
        r.sort(Map.Entry.comparingByKey());
        return r;
    }

    /** El efecto que sale en este momento de la animación ("" si no hay). */
    public static String efecto() {
        JsonObject pe = efectosJson(false);
        String k = pe == null ? null : claveCerca(pe);
        if (k == null) {
            return "";
        }
        JsonElement v = pe.get(k);
        if (v.isJsonArray() && v.getAsJsonArray().size() > 0) {
            v = v.getAsJsonArray().get(0);
        }
        return v.isJsonObject() && v.getAsJsonObject().has("effect") ? v.getAsJsonObject().get("effect").getAsString()
                : v.isJsonPrimitive() ? v.getAsString() : "";
    }

    /**
     * Pone en este momento de la animación un efecto de partículas que sale del hueso elegido (o del centro si no hay hueso); con "" lo
     * quita. Se guarda como {@code particle_effects} de Blockbench, así que también se ve y se mueve en Blockbench.
     */
    public static void efecto(String id) {
        if (clip.isEmpty()) {
            return;
        }
        JsonObject pe = efectosJson(true);
        String k = claveCerca(pe);
        if (k != null) {
            pe.remove(k);
        }
        if (id != null && !id.isEmpty()) {
            JsonObject o = new JsonObject();
            o.addProperty("effect", id);
            if (!hueso.isEmpty()) {
                o.addProperty("locator", hueso);
            }
            pe.add(k != null ? k : t(tiempo), o);
        } else if (pe.size() == 0) {
            anims().getAsJsonObject(clip).remove("particle_effects");
        }
        aplicar();
    }

    private static JsonObject efectosJson(boolean crear) {
        if (clip.isEmpty() || !anims().has(clip)) {
            return null;
        }
        JsonObject a = anims().getAsJsonObject(clip);
        if (!a.has("particle_effects") || !a.get("particle_effects").isJsonObject()) {
            if (!crear) {
                return null;
            }
            a.add("particle_effects", new JsonObject());
        }
        return a.getAsJsonObject("particle_effects");
    }

    /** Pone (o cambia) el cuadro clave del hueso elegido en el tiempo actual. */
    public static void poner(String canal, float[] v) {
        if (hueso.isEmpty() || clip.isEmpty()) {
            return;
        }
        JsonObject ch = canalJson(hueso, canal, true);
        String k = claveCerca(ch);
        JsonArray arr = new JsonArray();
        for (float f : v) {
            arr.add(Math.round(f * 1000) / 1000F);
        }
        if (k != null && ch.get(k).isJsonObject()) {
            JsonObject o = ch.getAsJsonObject(k);
            o.remove("pre");
            o.remove("post");
            o.add("vector", arr);
        } else {
            ch.add(k != null ? k : t(tiempo), arr);
        }
        aplicar();
    }

    /** Quita el cuadro clave del hueso elegido en el tiempo actual (en los tres canales). */
    public static void quitar() {
        for (String canal : new String[] {"rotation", "position", "scale"}) {
            JsonObject ch = canalJson(hueso, canal, false);
            if (ch == null) {
                continue;
            }
            JsonObject real = canalJson(hueso, canal, true);
            String k = claveCerca(real);
            if (k != null) {
                real.remove(k);
            }
            if (real.size() == 0) {
                anims().getAsJsonObject(clip).getAsJsonObject("bones").getAsJsonObject(hueso).remove(canal);
            }
        }
        aplicar();
    }

    /** Curvas para el cuadro clave actual (en el orden en que se recorren con el botón). */
    public static final String[] CURVAS = {"linear", "catmullrom", "step", "easeInOutSine", "easeInQuad", "easeOutQuad", "easeOutBack",
            "easeInBack", "easeOutElastic", "easeOutBounce"};

    /** La curva del cuadro clave actual del hueso (en rotación, o el primero que tenga). */
    public static String curva() {
        for (String canal : new String[] {"rotation", "position", "scale"}) {
            JsonObject ch = canalJson(hueso, canal, false);
            if (ch == null) {
                continue;
            }
            String k = claveCerca(ch);
            if (k == null) {
                continue;
            }
            JsonElement e = ch.get(k);
            if (e.isJsonObject()) {
                JsonObject o = e.getAsJsonObject();
                if (o.has("lerp_mode")) {
                    return o.get("lerp_mode").getAsString();
                }
                if (o.has("easing")) {
                    return o.get("easing").getAsString();
                }
            }
            return "linear";
        }
        return "";
    }

    /** Cambia la curva de llegada del cuadro clave actual (en los canales que tengan uno aquí). */
    public static void curva(String nombre) {
        for (String canal : new String[] {"rotation", "position", "scale"}) {
            JsonObject ch = canalJson(hueso, canal, false);
            if (ch == null) {
                continue;
            }
            JsonObject real = canalJson(hueso, canal, true);
            String k = claveCerca(real);
            if (k == null) {
                continue;
            }
            JsonElement e = real.get(k);
            JsonObject o;
            if (e.isJsonObject()) {
                o = e.getAsJsonObject();
            } else {
                o = new JsonObject();
                o.add("vector", e);
                real.add(k, o);
            }
            o.remove("lerp_mode");
            o.remove("easing");
            if (nombre.equals("catmullrom") || nombre.equals("step")) {
                o.addProperty("lerp_mode", nombre);
            } else if (!nombre.equals("linear")) {
                o.addProperty("easing", nombre);
            }
        }
        aplicar();
    }

    /** Duración de la animación elegida. */
    public static void largo(float s) {
        if (!anims().has(clip)) {
            return;
        }
        anims().getAsJsonObject(clip).addProperty("animation_length", Math.round(s * 100) / 100F);
        aplicar();
    }

    public static void bucle(boolean b) {
        if (!anims().has(clip)) {
            return;
        }
        anims().getAsJsonObject(clip).addProperty("loop", b);
        aplicar();
    }

    /** Una animación nueva vacía (1 segundo, en bucle). */
    public static String nueva() {
        int n = 1;
        String nombre;
        do {
            nombre = "animation." + base + ".nueva" + n++;
        } while (anims().has(nombre));
        JsonObject a = new JsonObject();
        a.addProperty("loop", true);
        a.addProperty("animation_length", 1.0F);
        a.add("bones", new JsonObject());
        anims().add(nombre, a);
        clip = nombre;
        tiempo = 0;
        aplicar();
        return nombre;
    }

    /** Usa los cambios ya mismo (sin guardar el archivo). */
    static void aplicar() {
        cambios = true;
        EstudioArchivos.editadas(base, doc.toString());
    }

    /** Guarda en la carpeta del estudio ({@code nombre.animation.json}). */
    public static Path guardar() {
        try {
            Path dir = EstudioArchivos.carpeta();
            Files.createDirectories(dir);
            Path f = dir.resolve(base + ".animation.json");
            // ordenar los tiempos de cada canal (como Blockbench)
            for (String c : anims().keySet()) {
                JsonObject a = anims().getAsJsonObject(c);
                if (!a.has("bones")) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> b : a.getAsJsonObject("bones").entrySet()) {
                    JsonObject bo = b.getValue().getAsJsonObject();
                    for (String canal : new ArrayList<>(bo.keySet())) {
                        JsonElement ch = bo.get(canal);
                        if (ch.isJsonObject() && !ch.getAsJsonObject().has("vector") && !ch.getAsJsonObject().has("pre")) {
                            TreeMap<Float, Map.Entry<String, JsonElement>> orden = new TreeMap<>();
                            for (Map.Entry<String, JsonElement> k : ch.getAsJsonObject().entrySet()) {
                                try {
                                    orden.put(Float.parseFloat(k.getKey()), k);
                                } catch (NumberFormatException ignored) {
                                    // nada
                                }
                            }
                            JsonObject n = new JsonObject();
                            for (Map.Entry<String, JsonElement> k : orden.values()) {
                                n.add(k.getKey(), k.getValue());
                            }
                            bo.add(canal, n);
                        }
                    }
                }
            }
            Files.writeString(f, new GsonBuilder().setPrettyPrinting().create().toJson(doc), StandardCharsets.UTF_8);
            cambios = false;
            aviso = f.getFileName().toString();
            AxelisCore.LOGGER.info("AXCORE_ESTUDIO guardado {}", f);
            return f;
        } catch (Exception e) {
            aviso = "error: " + e.getMessage();
            return null;
        }
    }
}
