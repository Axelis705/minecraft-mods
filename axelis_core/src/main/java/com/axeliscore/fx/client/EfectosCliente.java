package com.axeliscore.fx.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.anim.client.EstudioArchivos;
import com.axeliscore.anim.client.Huesos;
import com.axeliscore.plat.Ids;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * (Cliente) Arma los efectos de {@link com.axeliscore.fx.Efectos} con partículas de Minecraft, por capas. Un efecto es un JSON:
 * <pre>
 * {"sonido": "minecraft:entity.blaze.shoot", "volumen": 0.8, "tono": 1.2,
 *  "capas": [
 *   {"particula": "polvo", "color": "#FFC04D", "color2": "#FF4D4D", "tam": 1.4,      // polvo de color (y a qué color pasa)
 *    "forma": "anillo", "radio": 1.5, "cuantas": 32,                                  // punto, esfera, anillo, disco, linea, cono, espiral, columna
 *    "velocidad": 0.15, "hacia": "afuera",                                            // afuera, adentro, arriba, abajo, dir, azar, nada
 *    "retraso": 0, "duracion": 6, "repetir": 1, "cada": 4,                            // en ticks: empieza, reparte, repite
 *    "alto": 0.1, "vueltas": 2, "abertura": 30, "largo": 3}                           // según la forma
 *  ]}
 * </pre>
 * Partículas: polvo, brillo, chispa, fuego, fuego_alma, humo, humo_grande, nube, explosion, critico, magia, encantar, portal, nota,
 * corazon, feliz, enojado, burbuja, nieve, ceniza, hoja, totem, vara (end_rod), tinta, salpicar, lava, sculk, cera, y cualquier
 * "minecraft:..." sencilla.
 */
public final class EfectosCliente {
    /** Una capa en marcha. */
    private static final class Activo {
        final JsonObject capa;
        final Vec3 pos, dir;
        final float escala;
        final long inicio;
        int hechas, ronda;

        Activo(JsonObject capa, Vec3 pos, Vec3 dir, float escala, long inicio) {
            this.capa = capa;
            this.pos = pos;
            this.dir = dir;
            this.escala = escala;
            this.inicio = inicio;
        }
    }

    private static final Map<String, JsonObject> CACHE = new HashMap<>();
    private static final List<Activo> ACTIVOS = new ArrayList<>();
    private static final Random R = new Random();
    private static long ultimoTick = Long.MIN_VALUE;
    private static int generacion = -1;
    /** Cuántos efectos del Core se lanzaron (para las pruebas). */
    public static int lanzados;

    private EfectosCliente() {
    }

    public static void init() {
        Hud.add((g, w, h) -> tick());
    }

    /** Olvida los efectos leídos (se vuelven a leer; lo llama la recarga del estudio). */
    public static void limpiar() {
        CACHE.clear();
    }

    /** Llegó un efecto del servidor. */
    public static void recibir(String id, double x, double y, double z, float dx, float dy, float dz, float escala, int ent, String hueso) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 pos = new Vec3(x, y, z);
        if (ent >= 0 && mc.level != null) {
            Entity e = mc.level.getEntity(ent);
            if (e != null) {
                Vec3 hp = Huesos.posicion(e, hueso, 1F);
                pos = hp != null ? hp : e.position().add(0, e.getBbHeight() * 0.5, 0);
            }
        }
        lanzar(id, pos, new Vec3(dx, dy, dz), escala);
    }

    /** Lanza un efecto aquí mismo (solo en este cliente). */
    public static void lanzar(String id, Vec3 pos, Vec3 dir, float escala) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        JsonObject def = definicion(id);
        if (def == null) {
            // una partícula suelta de Minecraft ("minecraft:flame"): un puñito
            ParticleOptions p = particula(id, null);
            if (p != null) {
                for (int i = 0; i < 8; i++) {
                    mc.level.addParticle(p, pos.x + (R.nextDouble() - 0.5) * 0.3, pos.y + (R.nextDouble() - 0.5) * 0.3,
                            pos.z + (R.nextDouble() - 0.5) * 0.3, (R.nextDouble() - 0.5) * 0.05, R.nextDouble() * 0.05,
                            (R.nextDouble() - 0.5) * 0.05);
                }
            }
            return;
        }
        if (def.has("sonido")) {
            try {
                var snd = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Ids.parse(def.get("sonido").getAsString()));
                float vol = def.has("volumen") ? def.get("volumen").getAsFloat() : 1F;
                float tono = def.has("tono") ? def.get("tono").getAsFloat() : 1F;
                mc.level.playLocalSound(pos.x, pos.y, pos.z, snd, net.minecraft.sounds.SoundSource.NEUTRAL, vol, tono, false);
            } catch (RuntimeException ignored) {
                // sonido mal escrito
            }
        }
        lanzados++;
        long ahora = mc.level.getGameTime();
        if (def.has("capas")) {
            for (JsonElement c : def.getAsJsonArray("capas")) {
                if (c.isJsonObject()) {
                    ACTIVOS.add(new Activo(c.getAsJsonObject(), pos, dir, escala, ahora));
                }
            }
        }
        if (ACTIVOS.size() > 400) {
            ACTIVOS.subList(0, ACTIVOS.size() - 400).clear();
        }
    }

    /** Desde un cuadro de partícula de una animación: efecto del Core en el hueso (localizador) o una partícula de Minecraft. */
    public static void desdeAnimacion(Entity e, String efecto, String localizador) {
        if (efecto == null || efecto.isEmpty()) {
            return;
        }
        Vec3 p = localizador == null ? null : Huesos.posicion(e, localizador, 1F);
        if (p == null) {
            p = e.position().add(0, e.getBbHeight() * 0.6, 0);
        }
        lanzar(efecto.contains(":") ? efecto : AxelisCore.MODID + ":" + efecto, p, e.getLookAngle(), 1F);
    }

    // ------------------------------------------------------------------ cada tick

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            ACTIVOS.clear();
            return;
        }
        if (generacion != EstudioArchivos.generacion()) {
            generacion = EstudioArchivos.generacion();
            CACHE.clear();
        }
        long t = mc.level.getGameTime();
        if (t == ultimoTick) {
            return;
        }
        ultimoTick = t;
        Iterator<Activo> it = ACTIVOS.iterator();
        while (it.hasNext()) {
            Activo a = it.next();
            if (!paso(mc, a, t)) {
                it.remove();
            }
        }
    }

    /** Hace lo que toque a esta capa en este tick. Devuelve false si ya terminó. */
    private static boolean paso(Minecraft mc, Activo a, long t) {
        JsonObject c = a.capa;
        int retraso = i(c, "retraso", 0), duracion = Math.max(0, i(c, "duracion", 0)), repetir = Math.max(1, i(c, "repetir", 1)),
                cada = Math.max(1, i(c, "cada", duracion + 1));
        int cuantas = Math.max(1, Math.round(i(c, "cuantas", 10) * Math.max(0.2F, a.escala)));
        long local = t - a.inicio - retraso;
        if (local < 0) {
            return true;
        }
        long enRonda = local - (long) a.ronda * cada;
        if (a.ronda >= repetir) {
            return false;
        }
        if (enRonda < 0) {
            return true;
        }
        // cuántas deberían llevarse en esta ronda
        int debe = duracion <= 0 ? cuantas : (int) Math.min(cuantas, (enRonda + 1) * cuantas / (duracion + 1));
        ParticleOptions p = particula(s(c, "particula", "vara"), c);
        for (; a.hechas < debe; a.hechas++) {
            if (p != null) {
                una(mc, a, p, a.hechas, cuantas);
            }
        }
        if (a.hechas >= cuantas) {
            a.ronda++;
            a.hechas = 0;
            return a.ronda < repetir;
        }
        return true;
    }

    /** Una partícula de la capa (la número k de n). */
    private static void una(Minecraft mc, Activo a, ParticleOptions p, int k, int n) {
        JsonObject c = a.capa;
        float esc = a.escala;
        double radio = f(c, "radio", 0.5F) * esc, alto = f(c, "alto", 0) * esc, vel = f(c, "velocidad", 0.05F) * esc;
        String forma = s(c, "forma", "esfera"), hacia = s(c, "hacia", "afuera");
        Vec3 dir = a.dir.lengthSqr() < 1e-6 ? new Vec3(0, 1, 0) : a.dir.normalize();
        double x = 0, y = 0, z = 0;
        double ang = (k + R.nextDouble() * 0.3) / n * Math.PI * 2;
        switch (forma) {
            case "punto" -> {
            }
            case "anillo" -> {
                x = Math.cos(ang) * radio;
                z = Math.sin(ang) * radio;
                y = alto;
            }
            case "disco" -> {
                double r = Math.sqrt(R.nextDouble()) * radio;
                double an = R.nextDouble() * Math.PI * 2;
                x = Math.cos(an) * r;
                z = Math.sin(an) * r;
                y = alto;
            }
            case "linea" -> {
                double l = f(c, "largo", 2) * esc * (k / (double) Math.max(1, n - 1));
                x = dir.x * l;
                y = dir.y * l;
                z = dir.z * l;
            }
            case "cono" -> {
                // dentro del cono, a lo largo de la dirección
                double ab = Math.toRadians(f(c, "abertura", 25));
                Vec3 v = desviar(dir, ab);
                double l = R.nextDouble() * radio;
                x = v.x * l;
                y = v.y * l;
                z = v.z * l;
            }
            case "espiral" -> {
                double vueltas = f(c, "vueltas", 2);
                double fr = k / (double) Math.max(1, n - 1);
                double an = fr * vueltas * Math.PI * 2;
                x = Math.cos(an) * radio;
                z = Math.sin(an) * radio;
                y = fr * alto;
            }
            case "columna" -> {
                double an = R.nextDouble() * Math.PI * 2;
                x = Math.cos(an) * radio * R.nextDouble();
                z = Math.sin(an) * radio * R.nextDouble();
                y = R.nextDouble() * alto;
            }
            default -> {
                // esfera: puntos al azar dentro
                Vec3 v = new Vec3(R.nextGaussian(), R.nextGaussian(), R.nextGaussian()).normalize();
                double r = Math.cbrt(R.nextDouble()) * radio;
                x = v.x * r;
                y = v.y * r + alto;
                z = v.z * r;
            }
        }
        double vx, vy, vz;
        switch (hacia) {
            case "adentro" -> {
                Vec3 v = new Vec3(-x, -y, -z);
                v = v.lengthSqr() < 1e-6 ? Vec3.ZERO : v.normalize();
                vx = v.x * vel;
                vy = v.y * vel;
                vz = v.z * vel;
            }
            case "arriba" -> {
                vx = 0;
                vy = vel;
                vz = 0;
            }
            case "abajo" -> {
                vx = 0;
                vy = -vel;
                vz = 0;
            }
            case "dir" -> {
                Vec3 v = forma.equals("cono") ? new Vec3(x, y, z).normalize() : dir;
                vx = v.x * vel;
                vy = v.y * vel;
                vz = v.z * vel;
            }
            case "azar" -> {
                vx = (R.nextDouble() - 0.5) * 2 * vel;
                vy = (R.nextDouble() - 0.5) * 2 * vel;
                vz = (R.nextDouble() - 0.5) * 2 * vel;
            }
            case "nada" -> {
                vx = 0;
                vy = 0;
                vz = 0;
            }
            default -> {
                Vec3 v = new Vec3(x, forma.equals("anillo") || forma.equals("disco") ? 0 : y, z);
                v = v.lengthSqr() < 1e-6 ? new Vec3(R.nextGaussian(), R.nextGaussian(), R.nextGaussian()).normalize() : v.normalize();
                vx = v.x * vel;
                vy = v.y * vel + f(c, "subir", 0) * esc;
                vz = v.z * vel;
            }
        }
        double jit = f(c, "dispersion", 0.02F);
        mc.level.addParticle(p, a.pos.x + x + R.nextGaussian() * jit, a.pos.y + y + R.nextGaussian() * jit, a.pos.z + z + R.nextGaussian() * jit,
                vx, vy, vz);
    }

    private static Vec3 desviar(Vec3 d, double ab) {
        // un vector al azar dentro de un cono alrededor de d
        Vec3 a = Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 u = d.cross(a).normalize(), w = d.cross(u).normalize();
        double t = R.nextDouble() * ab, fi = R.nextDouble() * Math.PI * 2;
        return d.scale(Math.cos(t)).add(u.scale(Math.sin(t) * Math.cos(fi))).add(w.scale(Math.sin(t) * Math.sin(fi))).normalize();
    }

    // ------------------------------------------------------------------ partículas por nombre

    /** La partícula por nombre (con su color si es polvo); null si no se conoce. */
    public static ParticleOptions particula(String n, JsonObject c) {
        if (n == null) {
            return null;
        }
        switch (n) {
            case "polvo" -> {
                int a = color(c, "color", 0xFFFFFF), b = c != null && c.has("color2") ? color(c, "color2", a) : -1;
                float tam = c == null ? 1F : Math.max(0.1F, Math.min(4F, f(c, "tam", 1F)));
                return b >= 0 ? transicion(a, b, tam) : polvo(a, tam);
            }
            case "brillo" -> {
                return ParticleTypes.GLOW;
            }
            case "chispa" -> {
                return ParticleTypes.ELECTRIC_SPARK;
            }
            case "fuego" -> {
                return ParticleTypes.FLAME;
            }
            case "fuego_alma" -> {
                return ParticleTypes.SOUL_FIRE_FLAME;
            }
            case "humo" -> {
                return ParticleTypes.SMOKE;
            }
            case "humo_grande" -> {
                return ParticleTypes.LARGE_SMOKE;
            }
            case "nube" -> {
                return ParticleTypes.CLOUD;
            }
            case "explosion" -> {
                return ParticleTypes.EXPLOSION;
            }
            case "critico" -> {
                return ParticleTypes.CRIT;
            }
            case "magia" -> {
                return ParticleTypes.ENCHANTED_HIT;
            }
            case "encantar" -> {
                return ParticleTypes.ENCHANT;
            }
            case "portal" -> {
                return ParticleTypes.PORTAL;
            }
            case "nota" -> {
                return ParticleTypes.NOTE;
            }
            case "corazon" -> {
                return ParticleTypes.HEART;
            }
            case "feliz" -> {
                return ParticleTypes.HAPPY_VILLAGER;
            }
            case "enojado" -> {
                return ParticleTypes.ANGRY_VILLAGER;
            }
            case "burbuja" -> {
                return ParticleTypes.BUBBLE_POP;
            }
            case "nieve" -> {
                return ParticleTypes.SNOWFLAKE;
            }
            case "ceniza" -> {
                return ParticleTypes.WHITE_ASH;
            }
            case "hoja" -> {
                return ParticleTypes.CHERRY_LEAVES;
            }
            case "totem" -> {
                return ParticleTypes.TOTEM_OF_UNDYING;
            }
            case "vara" -> {
                return ParticleTypes.END_ROD;
            }
            case "tinta" -> {
                return ParticleTypes.SQUID_INK;
            }
            case "salpicar" -> {
                return ParticleTypes.SPLASH;
            }
            case "lava" -> {
                return ParticleTypes.LAVA;
            }
            case "sculk" -> {
                return ParticleTypes.SCULK_SOUL;
            }
            case "cera" -> {
                return ParticleTypes.WAX_ON;
            }
            default -> {
                return vanilla(n);
            }
        }
    }

    /** Una partícula sencilla de Minecraft por su id (las que no necesitan datos). */
    private static ParticleOptions vanilla(String id) {
        try {
            ResourceLocation rl = Ids.parse(id.contains(":") ? id : "minecraft:" + id);
            //#if MC >= 1.21.2
            //$$ var t = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getValue(rl);
            //#else
            var t = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(rl);
            //#endif
            return t instanceof net.minecraft.core.particles.SimpleParticleType s ? s : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ParticleOptions polvo(int rgb, float tam) {
        //#if MC >= 1.21.2
        //$$ return new DustParticleOptions(rgb, tam);
        //#else
        return new DustParticleOptions(new org.joml.Vector3f(((rgb >> 16) & 255) / 255F, ((rgb >> 8) & 255) / 255F, (rgb & 255) / 255F), tam);
        //#endif
    }

    private static ParticleOptions transicion(int a, int b, float tam) {
        //#if MC >= 1.21.2
        //$$ return new DustColorTransitionOptions(a, b, tam);
        //#else
        return new DustColorTransitionOptions(new org.joml.Vector3f(((a >> 16) & 255) / 255F, ((a >> 8) & 255) / 255F, (a & 255) / 255F),
                new org.joml.Vector3f(((b >> 16) & 255) / 255F, ((b >> 8) & 255) / 255F, (b & 255) / 255F), tam);
        //#endif
    }

    // ------------------------------------------------------------------ leer los efectos

    /** El efecto por id ("axelis_core:chispas"): primero el del estudio (chispas.efecto.json), luego el del mod. */
    public static JsonObject definicion(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return CACHE.computeIfAbsent(id, k -> {
            ResourceLocation rl;
            try {
                rl = Ids.parse(k.contains(":") ? k : AxelisCore.MODID + ":" + k);
            } catch (RuntimeException e) {
                return null;
            }
            String txt = EstudioArchivos.efecto(rl);
            if (txt == null) {
                txt = leer(Ids.rl(rl.getNamespace(), "efectos/" + rl.getPath() + ".json"));
            }
            if (txt == null) {
                return null;
            }
            try {
                return JsonParser.parseString(txt).getAsJsonObject();
            } catch (RuntimeException e) {
                AxelisCore.LOGGER.error("Axelis Core: efecto {} mal escrito: {}", k, e.getMessage());
                return null;
            }
        });
    }

    private static String leer(ResourceLocation id) {
        try {
            Optional<Resource> r = Minecraft.getInstance().getResourceManager().getResource(id);
            if (r.isEmpty()) {
                return null;
            }
            try (BufferedReader br = r.get().openAsReader()) {
                return br.lines().collect(Collectors.joining("\n"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ ayudas

    private static int i(JsonObject o, String k, int d) {
        return o != null && o.has(k) ? o.get(k).getAsInt() : d;
    }

    private static float f(JsonObject o, String k, float d) {
        return o != null && o.has(k) ? o.get(k).getAsFloat() : d;
    }

    private static String s(JsonObject o, String k, String d) {
        return o != null && o.has(k) ? o.get(k).getAsString() : d;
    }

    private static int color(JsonObject o, String k, int d) {
        if (o == null || !o.has(k)) {
            return d;
        }
        JsonElement e = o.get(k);
        if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsInt() & 0xFFFFFF;
        }
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            return (Math.round(a.get(0).getAsFloat() * 255) << 16) | (Math.round(a.get(1).getAsFloat() * 255) << 8) | Math.round(a.get(2).getAsFloat() * 255);
        }
        String s = e.getAsString().replace("#", "");
        try {
            return Integer.parseInt(s, 16) & 0xFFFFFF;
        } catch (NumberFormatException ex) {
            return d;
        }
    }
}
