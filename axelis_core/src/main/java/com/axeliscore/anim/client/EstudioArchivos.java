package com.axeliscore.anim.client;

import com.axeliscore.AxelisCore;
import com.axeliscore.anim.BbModel;
import com.axeliscore.anim.ClipLoader;
import com.axeliscore.anim.GeoLoader;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Plat;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * (Cliente) La carpeta del estudio: {@code .minecraft/axelis_estudio/}. Lo que se guarde ahí reemplaza al modelo, las animaciones o la
 * textura del mismo nombre de cualquier mod, y se recarga solo al guardar (sin reiniciar ni F3+T):
 * <ul>
 *     <li>{@code persona.bbmodel}: el proyecto de Blockbench tal cual (modelo, animaciones y textura).</li>
 *     <li>{@code persona.geo.json} / {@code persona.animation.json}: lo exportado de Blockbench.</li>
 *     <li>{@code persona.png} (o {@code mara.png}): una textura.</li>
 * </ul>
 * Se busca por el nombre del archivo, sin importar el mod. Los errores salen en el chat (con lo que está mal) y el juego sigue con lo
 * de antes.
 */
public final class EstudioArchivos {
    private static Path carpeta;
    private static final Map<Path, Long> VISTOS = new HashMap<>();
    private static final Map<String, String> GEO = new HashMap<>(), ANIM = new HashMap<>(), EFECTOS = new HashMap<>();
    private static final Map<String, ResourceLocation> TEX = new HashMap<>();
    private static long proxima;
    private static int generacion, subidas;
    private static boolean listo, apagado;

    private EstudioArchivos() {
    }

    /** La carpeta (la crea si no existe). */
    public static Path carpeta() {
        if (carpeta == null) {
            carpeta = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("axelis_estudio");
        }
        return carpeta;
    }

    /** Cuántas veces se recargó algo (para saber si hay que volver a preparar los modelos). */
    public static int generacion() {
        return generacion;
    }

    /** ¿Hay algo cargado de la carpeta? */
    public static boolean activo() {
        return !GEO.isEmpty() || !ANIM.isEmpty() || !TEX.isEmpty() || !EFECTOS.isEmpty();
    }

    // ------------------------------------------------------------------ lo que usan los renderizadores

    static String base(ResourceLocation id) {
        String p = id.getPath();
        int s = p.lastIndexOf('/');
        p = s >= 0 ? p.substring(s + 1) : p;
        int d = p.indexOf('.');
        return (d >= 0 ? p.substring(0, d) : p).toLowerCase(Locale.ROOT);
    }

    /** El modelo del estudio que reemplaza a ese (o null). */
    public static String geo(ResourceLocation id) {
        return GEO.get(base(id));
    }

    /** Las animaciones del estudio que reemplazan a esas (o null). Primero las que se están editando en el juego. */
    public static String animaciones(ResourceLocation id) {
        String b = base(id);
        String e = EDITADAS.get(b);
        return e != null ? e : ANIM.get(b);
    }

    /** Animaciones que el editor del juego está cambiando (sin guardar todavía). */
    private static final Map<String, String> EDITADAS = new HashMap<>();

    /** (Editor) Usa estas animaciones para ese nombre ya mismo, sin escribir el archivo. null = volver a las de siempre. */
    public static void editadas(String base, String json) {
        if (json == null) {
            EDITADAS.remove(base);
        } else {
            EDITADAS.put(base, json);
        }
        GeoAssets.clear();
        generacion++;
    }

    /** El efecto de partículas del estudio ({@code nombre.efecto.json}) que reemplaza a ese (o null). */
    public static String efecto(ResourceLocation id) {
        return EFECTOS.get(base(id));
    }

    /** La textura del estudio que reemplaza a esa (o la misma). */
    public static ResourceLocation textura(ResourceLocation id) {
        if (id == null || TEX.isEmpty()) {
            return id;
        }
        ResourceLocation r = TEX.get(base(id));
        return r != null ? r : id;
    }

    // ------------------------------------------------------------------ revisar la carpeta

    /** (Cada cuadro) Una vez por segundo mira si cambió algo. */
    public static void revisar() {
        if (apagado) {
            return;
        }
        long ahora = System.currentTimeMillis();
        if (ahora < proxima) {
            return;
        }
        proxima = ahora + 1000;
        try {
            Path dir = carpeta();
            if (!listo) {
                listo = true;
                Files.createDirectories(dir);
                Path leeme = dir.resolve("LEEME.txt");
                if (!Files.exists(leeme)) {
                    Files.writeString(leeme, """
                            Carpeta del estudio de Axelis Core
                            ===================================
                            Guarda aqui lo que estes haciendo en Blockbench y el juego lo cambia al instante (sin reiniciar):
                              - nombre.bbmodel         el proyecto de Blockbench tal cual (modelo + animaciones + textura)
                              - nombre.geo.json        modelo exportado
                              - nombre.animation.json  animaciones exportadas
                              - nombre.png             textura
                              - nombre.efecto.json     efecto de particulas (por capas)
                            "nombre" es el del modelo del mod (por ejemplo persona, coloso, bestia) o el de la textura.
                            Para volver a lo del mod, borra o mueve el archivo.
                            Dentro del juego: /axeliscore estudio (editor de animaciones).
                            """, StandardCharsets.UTF_8);
                }
            }
            List<Path> archivos = new ArrayList<>();
            try (Stream<Path> st = Files.list(dir)) {
                st.filter(Files::isRegularFile).forEach(archivos::add);
            }
            boolean cambio = archivos.size() != VISTOS.size();
            for (Path f : archivos) {
                long m = Files.getLastModifiedTime(f).toMillis();
                Long antes = VISTOS.get(f);
                if (antes == null || antes != m) {
                    cambio = true;
                }
            }
            if (!cambio) {
                return;
            }
            // Blockbench escribe en dos pasos: se espera a que el archivo deje de cambiar un momento
            Thread.sleep(60);
            cargarTodo(archivos);
        } catch (IOException e) {
            AxelisCore.LOGGER.warn("Axelis Core: no se pudo leer la carpeta del estudio: {}", e.toString());
            apagado = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void cargarTodo(List<Path> archivos) throws IOException {
        Map<Path, Long> nuevos = new HashMap<>();
        Map<String, String> geo = new HashMap<>(), anim = new HashMap<>(), efectos = new HashMap<>();
        Map<String, byte[]> png = new HashMap<>();
        List<String> cargados = new ArrayList<>(), errores = new ArrayList<>();
        for (Path f : archivos) {
            nuevos.put(f, Files.getLastModifiedTime(f).toMillis());
            String n = f.getFileName().toString();
            String nl = n.toLowerCase(Locale.ROOT);
            Long antes = VISTOS.get(f);
            boolean nuevo = antes == null || antes != Files.getLastModifiedTime(f).toMillis();
            try {
                if (nl.endsWith(".bbmodel")) {
                    String b = nl.substring(0, nl.length() - 8);
                    BbModel.Resultado r = BbModel.parse(Files.readString(f, StandardCharsets.UTF_8));
                    String g = r.geo.toString();
                    GeoLoader.parse(g); // comprobar que se puede leer
                    geo.put(b, g);
                    if (r.animaciones.getAsJsonObject("animations").size() > 0) {
                        String a = r.animaciones.toString();
                        ClipLoader.parse(a);
                        anim.put(b, a);
                    }
                    if (!r.texturas.isEmpty()) {
                        png.put(b, r.texturas.get(0).png());
                    }
                    if (nuevo) {
                        cargados.add(n + " (" + r.geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones").size()
                                + " huesos, " + r.animaciones.getAsJsonObject("animations").size() + " animaciones)");
                        for (String av : r.avisos) {
                            errores.add(n + ": " + av);
                        }
                    }
                } else if (nl.endsWith(".geo.json")) {
                    String txt = Files.readString(f, StandardCharsets.UTF_8);
                    GeoLoader.parse(txt);
                    geo.putIfAbsent(nl.substring(0, nl.length() - 9), txt);
                    if (nuevo) {
                        cargados.add(n);
                    }
                } else if (nl.endsWith(".animation.json")) {
                    String txt = Files.readString(f, StandardCharsets.UTF_8);
                    ClipLoader.parse(txt);
                    anim.putIfAbsent(nl.substring(0, nl.length() - 15), txt);
                    if (nuevo) {
                        cargados.add(n);
                    }
                } else if (nl.endsWith(".efecto.json")) {
                    String txt = Files.readString(f, StandardCharsets.UTF_8);
                    com.google.gson.JsonParser.parseString(txt).getAsJsonObject();
                    efectos.put(nl.substring(0, nl.length() - 12), txt);
                    if (nuevo) {
                        cargados.add(n);
                    }
                } else if (nl.endsWith(".png")) {
                    png.putIfAbsent(nl.substring(0, nl.length() - 4), Files.readAllBytes(f));
                    if (nuevo) {
                        cargados.add(n);
                    }
                }
            } catch (RuntimeException e) {
                errores.add(n + ": " + e.getMessage());
            }
        }
        boolean habia = !VISTOS.isEmpty() || activo();
        VISTOS.clear();
        VISTOS.putAll(nuevos);
        GEO.clear();
        GEO.putAll(geo);
        ANIM.clear();
        ANIM.putAll(anim);
        EFECTOS.clear();
        EFECTOS.putAll(efectos);
        // texturas: se suben otra vez con otro nombre (así no se mezclan con las viejas)
        Map<String, ResourceLocation> viejas = new HashMap<>(TEX);
        TEX.clear();
        for (Map.Entry<String, byte[]> e : png.entrySet()) {
            try {
                TEX.put(e.getKey(), subir(e.getKey(), e.getValue()));
            } catch (IOException | RuntimeException ex) {
                errores.add(e.getKey() + ".png: " + ex.getMessage());
            }
        }
        for (ResourceLocation v : viejas.values()) {
            Minecraft.getInstance().getTextureManager().release(v);
        }
        GeoAssets.clear();
        generacion++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && (habia || !cargados.isEmpty() || !errores.isEmpty())) {
            if (!cargados.isEmpty()) {
                Plat.msg(mc.player, Component.literal("[Estudio] ").withStyle(ChatFormatting.AQUA)
                        .append(Component.literal("recargado: " + String.join(", ", cargados)).withStyle(ChatFormatting.WHITE)), false);
            }
            for (String err : errores) {
                Plat.msg(mc.player, Component.literal("[Estudio] ").withStyle(ChatFormatting.AQUA).append(Component.literal(err)
                        .withStyle(ChatFormatting.RED)), false);
            }
        }
        AxelisCore.LOGGER.info("AXCORE_ESTUDIO recargado modelos={} animaciones={} texturas={} errores={}", GEO.size(), ANIM.size(), TEX.size(),
                errores.size());
    }

    private static ResourceLocation subir(String nombre, byte[] bytes) throws IOException {
        BufferedImage bi = javax.imageio.ImageIO.read(new ByteArrayInputStream(bytes));
        if (bi == null) {
            throw new IOException("no es una imagen PNG");
        }
        int w = bi.getWidth(), h = bi.getHeight();
        NativeImage n = new NativeImage(w, h, false);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = bi.getRGB(x, y);
                //#if MC >= 1.21.2
                //$$ n.setPixel(x, y, argb);
                //#else
                n.setPixelRGBA(x, y, (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >> 16 & 0xFF));
                //#endif
            }
        }
        ResourceLocation rl = Ids.core("estudio/" + nombre.replaceAll("[^a-z0-9_.-]", "_") + "_" + (++subidas));
        //#if MC >= 1.21.5
        //$$ DynamicTexture t = new DynamicTexture(rl::toString, n);
        //#else
        DynamicTexture t = new DynamicTexture(n);
        //#endif
        Minecraft.getInstance().getTextureManager().register(rl, t);
        return rl;
    }
}
