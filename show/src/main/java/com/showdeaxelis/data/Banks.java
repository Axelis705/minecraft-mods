package com.showdeaxelis.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.showdeaxelis.game.Answer100;
import com.showdeaxelis.game.Question100;
import com.showdeaxelis.game.TriviaQ;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Los bancos de preguntas. Se copian al jugar la primera vez a config/show_de_axelis/ y ahi se pueden editar con cualquier editor de texto;
 * /show recargar los vuelve a leer sin reiniciar el juego.
 */
public final class Banks {
    private static final Logger LOG = LogManager.getLogger("show_de_axelis");
    public static final String[] FILES = {"100mexicanos.json", "trivia.json", "palabras.json", "prohibidas.json", "anuncios.json", "retos.json", "dibujos.json"};

    public record Taboo(String word, List<String> forbidden) {
    }

    public static final List<Question100> Q100 = new ArrayList<>();
    public static final List<TriviaQ> TRIVIA = new ArrayList<>();
    public static final Map<String, List<String>> WORDS = new LinkedHashMap<>();
    public static final List<Taboo> TABOO = new ArrayList<>();
    public static final Map<String, List<String>> LINES = new LinkedHashMap<>();
    public static final List<String> PRIZES = new ArrayList<>();
    public static final List<String> RETOS = new ArrayList<>();
    public static final List<String> DIBUJOS = new ArrayList<>();

    private Banks() {
    }

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("show_de_axelis");
    }

    private static String jar(String name) {
        try (InputStream in = Banks.class.getResourceAsStream("/data/show_de_axelis/banco/" + name)) {
            return in == null ? "[]" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** Copia los bancos de fabrica a la carpeta de config. Con {@code force} reemplaza los que ya existen. */
    public static void install(boolean force) {
        try {
            Files.createDirectories(dir());
            for (String f : FILES) {
                Path p = dir().resolve(f);
                if (force || !Files.exists(p)) {
                    Files.writeString(p, jar(f), StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            LOG.warn("No se pudieron copiar los bancos: {}", e.toString());
        }
    }

    private static JsonElement read(String name) {
        try {
            return JsonParser.parseString(Files.readString(dir().resolve(name), StandardCharsets.UTF_8));
        } catch (Exception e) {
            LOG.warn("El banco {} tiene un error ({}); se usa el de fabrica", name, e.toString());
            return JsonParser.parseString(jar(name));
        }
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : d;
    }

    private static List<String> strs(JsonObject o, String k) {
        List<String> l = new ArrayList<>();
        if (o.has(k) && o.get(k).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(k)) {
                l.add(e.getAsString());
            }
        }
        return l;
    }

    /** Lee todo. Devuelve un resumen para mostrarle al presentador. */
    public static String load() {
        install(false);
        Q100.clear();
        TRIVIA.clear();
        WORDS.clear();
        TABOO.clear();
        LINES.clear();
        PRIZES.clear();
        RETOS.clear();
        DIBUJOS.clear();
        try {
            int i = 0;
            for (JsonElement e : read("100mexicanos.json").getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                List<Answer100> as = new ArrayList<>();
                for (JsonElement ae : o.getAsJsonArray("respuestas")) {
                    JsonObject a = ae.getAsJsonObject();
                    as.add(new Answer100(str(a, "t", "?"), a.has("p") ? a.get("p").getAsInt() : 1, strs(a, "alias")));
                }
                Q100.add(new Question100(str(o, "id", "q" + i), str(o, "pregunta", "?"), as));
                i++;
            }
            i = 0;
            for (JsonElement e : read("trivia.json").getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                TRIVIA.add(new TriviaQ(str(o, "id", "t" + i), str(o, "cat", "Trivia"), str(o, "pregunta", "?"), strs(o, "op"),
                        o.has("ok") ? o.get("ok").getAsInt() : 0, str(o, "nota", "")));
                i++;
            }
            for (JsonElement e : read("palabras.json").getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                WORDS.put(str(o, "cat", "Cosas"), strs(o, "palabras"));
            }
            for (JsonElement e : read("prohibidas.json").getAsJsonArray()) {
                JsonObject o = e.getAsJsonObject();
                TABOO.add(new Taboo(str(o, "palabra", "?"), strs(o, "no")));
            }
            for (JsonElement e : read("retos.json").getAsJsonArray()) {
                RETOS.add(e.getAsString());
            }
            for (JsonElement e : read("dibujos.json").getAsJsonArray()) {
                DIBUJOS.add(e.getAsString());
            }
            JsonObject l = read("anuncios.json").getAsJsonObject();
            // frases nuevas de fabrica que el archivo editado todavia no tiene
            for (Map.Entry<String, JsonElement> en : JsonParser.parseString(jar("anuncios.json")).getAsJsonObject().entrySet()) {
                if (!l.has(en.getKey())) {
                    l.add(en.getKey(), en.getValue());
                }
            }
            for (Map.Entry<String, JsonElement> en : l.entrySet()) {
                if (en.getValue().isJsonArray()) {
                    List<String> ls = new ArrayList<>();
                    for (JsonElement x : (JsonArray) en.getValue()) {
                        ls.add(x.getAsString());
                    }
                    if (en.getKey().equals("premios")) {
                        PRIZES.addAll(ls);
                    } else {
                        LINES.put(en.getKey(), ls);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error leyendo los bancos", e);
            return "Error leyendo los bancos: " + e.getMessage();
        }
        return Q100.size() + " preguntas de 100 mexicanos, " + TRIVIA.size() + " de trivia, " + WORDS.size() + " categorias de impostor, "
                + TABOO.size() + " palabras prohibidas, " + DIBUJOS.size() + " dibujos";
    }
}
