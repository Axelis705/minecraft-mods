package com.celular.client;

import com.celular.Celular;
import com.celular.net.CelularNet;
import com.celular.plat.Ids;
import com.celular.plat.Net;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Las fotos: la galería del celular (archivos .jpg en .minecraft/celular/fotos), las de Instacraft que llegan del servidor, los filtros
 * y el paso entre imágenes y texturas. Las texturas se cargan de a poco (unas cuantas por cuadro) para que no se trabe el juego.
 */
public final class Photos {
    /** Las fotos se guardan cuadradas de este tamaño. */
    public static final int SIZE = 288;
    private static final int MAX_LOADED = 72;
    private static final int LOADS_PER_FRAME = 3;

    private static final class Loaded {
        ResourceLocation rl;
        long used;
        boolean requested, failed;
    }

    private static final Map<String, Loaded> CACHE = new HashMap<>();
    private static int counter;
    private static int loadsThisFrame;
    private static long frame;

    private Photos() {
    }

    public static Path dir() {
        return FMLPaths.GAMEDIR.get().resolve("celular").resolve("fotos");
    }

    /** Las fotos de la galería, la más nueva primero. */
    public static List<Path> gallery() {
        List<Path> out = new ArrayList<>();
        Path d = dir();
        if (!Files.isDirectory(d)) {
            return out;
        }
        try (Stream<Path> s = Files.list(d)) {
            s.filter(p -> p.getFileName().toString().toLowerCase().endsWith(".jpg")).forEach(out::add);
        } catch (IOException ignored) {
        }
        out.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        return out;
    }

    // ------------------------------------------------------------------ JPEG

    public static byte[] jpeg(BufferedImage img, float quality) {
        try {
            ImageWriter w = ImageIO.getImageWritersByFormatName("jpeg").next();
            ImageWriteParam p = w.getDefaultWriteParam();
            p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            p.setCompressionQuality(quality);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            try (ImageOutputStream out = ImageIO.createImageOutputStream(bo)) {
                w.setOutput(out);
                w.write(null, new IIOImage(img, null, null), p);
            } finally {
                w.dispose();
            }
            return bo.toByteArray();
        } catch (Exception ex) {
            Celular.LOGGER.warn("No se pudo comprimir la foto", ex);
            return null;
        }
    }

    public static BufferedImage read(byte[] data) {
        try {
            BufferedImage bi = ImageIO.read(new ByteArrayInputStream(data));
            if (bi == null) {
                return null;
            }
            if (bi.getType() != BufferedImage.TYPE_INT_RGB) {
                BufferedImage c = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < bi.getHeight(); y++) {
                    for (int x = 0; x < bi.getWidth(); x++) {
                        c.setRGB(x, y, bi.getRGB(x, y) & 0xFFFFFF);
                    }
                }
                bi = c;
            }
            return bi;
        } catch (Exception ex) {
            return null;
        }
    }

    public static BufferedImage read(Path p) {
        try {
            return read(Files.readAllBytes(p));
        } catch (IOException ex) {
            return null;
        }
    }

    /** Guarda una foto nueva en la galería; devuelve el archivo. */
    public static Path save(BufferedImage img) {
        byte[] data = jpeg(img, 0.92F);
        if (data == null) {
            return null;
        }
        try {
            Files.createDirectories(dir());
            String base = "foto_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path f = dir().resolve(base + ".jpg");
            for (int i = 2; Files.exists(f); i++) {
                f = dir().resolve(base + "_" + i + ".jpg");
            }
            Files.write(f, data);
            return f;
        } catch (IOException ex) {
            Celular.LOGGER.warn("No se pudo guardar la foto", ex);
            return null;
        }
    }

    public static void delete(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
        forget("f:" + p);
    }

    // ------------------------------------------------------------------ texturas

    private static boolean canLoad() {
        long f = System.nanoTime() / 16_000_000L;
        if (f != frame) {
            frame = f;
            loadsThisFrame = 0;
        }
        return loadsThisFrame++ < LOADS_PER_FRAME;
    }

    private static ResourceLocation put(String key, Loaded l, BufferedImage img) {
        if (img == null) {
            l.failed = true;
            return null;
        }
        l.rl = Tex.upload(Ids.mod("foto/" + (counter++)), Tex.toNative(img));
        trim();
        return l.rl;
    }

    private static void trim() {
        if (CACHE.size() <= MAX_LOADED) {
            return;
        }
        List<Map.Entry<String, Loaded>> all = new ArrayList<>(CACHE.entrySet());
        all.sort(Comparator.comparingLong(e -> e.getValue().used));
        for (int i = 0; i < all.size() - MAX_LOADED; i++) {
            forget(all.get(i).getKey());
        }
    }

    private static void forget(String key) {
        Loaded l = CACHE.remove(key);
        if (l != null && l.rl != null) {
            Tex.release(l.rl);
        }
    }

    /** La textura de una foto de la galería (o null mientras se carga). */
    public static ResourceLocation local(Path p) {
        String key = "f:" + p;
        Loaded l = CACHE.computeIfAbsent(key, k -> new Loaded());
        l.used = System.currentTimeMillis();
        if (l.rl == null && !l.failed && canLoad()) {
            put(key, l, read(p));
        }
        return l.rl;
    }

    /** Una imagen ya hecha (por ejemplo la vista previa de un filtro), con su propia clave. */
    public static ResourceLocation image(String key, BufferedImage img) {
        forget("i:" + key);
        Loaded l = new Loaded();
        l.used = System.currentTimeMillis();
        CACHE.put("i:" + key, l);
        return put("i:" + key, l, img);
    }

    /** La foto de una publicación de Instacraft: la pide al servidor la primera vez. */
    public static ResourceLocation remote(long id) {
        String key = "r:" + id;
        Loaded l = CACHE.computeIfAbsent(key, k -> new Loaded());
        l.used = System.currentTimeMillis();
        if (l.rl == null && !l.requested) {
            l.requested = true;
            Net.toServer(new CelularNet.ImgReq(id));
        }
        return l.rl;
    }

    /** Llegó la foto de una publicación. */
    public static void received(long id, byte[] data) {
        String key = "r:" + id;
        Loaded l = CACHE.computeIfAbsent(key, k -> new Loaded());
        l.used = System.currentTimeMillis();
        l.requested = true;
        if (l.rl != null) {
            Tex.release(l.rl);
            l.rl = null;
        }
        put(key, l, read(data));
    }

    /** Al salir del mundo se sueltan todas las texturas. */
    public static void clearAll() {
        for (Iterator<Map.Entry<String, Loaded>> it = CACHE.entrySet().iterator(); it.hasNext(); ) {
            Loaded l = it.next().getValue();
            if (l.rl != null) {
                Tex.release(l.rl);
            }
            it.remove();
        }
    }

    // ------------------------------------------------------------------ filtros

    public static final String[] FILTERS = {"normal", "vivid", "warm", "cool", "mono", "sepia", "retro", "night"};

    private static int clamp(double v) {
        return v < 0 ? 0 : v > 255 ? 255 : (int) v;
    }

    /** Una copia de la imagen con el filtro aplicado. */
    public static BufferedImage filter(BufferedImage src, int f) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        double cx = w / 2.0, cy = h / 2.0, maxD = Math.sqrt(cx * cx + cy * cy);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = src.getRGB(x, y);
                double r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
                double l = 0.299 * r + 0.587 * g + 0.114 * b;
                switch (f) {
                    case 1 -> { // vívido: más color y contraste
                        r = l + (r - l) * 1.45;
                        g = l + (g - l) * 1.45;
                        b = l + (b - l) * 1.45;
                        r = (r - 128) * 1.12 + 128;
                        g = (g - 128) * 1.12 + 128;
                        b = (b - 128) * 1.12 + 128;
                    }
                    case 2 -> { // cálido
                        r = r * 1.08 + 12;
                        g = g * 1.02 + 4;
                        b = b * 0.86;
                    }
                    case 3 -> { // frío
                        r = r * 0.88;
                        g = g * 1.0 + 4;
                        b = b * 1.08 + 16;
                    }
                    case 4 -> { // blanco y negro con contraste
                        double v = (l - 128) * 1.2 + 128;
                        r = g = b = v;
                    }
                    case 5 -> { // sepia
                        double nr = 0.393 * r + 0.769 * g + 0.189 * b, ng = 0.349 * r + 0.686 * g + 0.168 * b, nb = 0.272 * r + 0.534 * g + 0.131 * b;
                        r = nr * 0.92;
                        g = ng * 0.92;
                        b = nb * 0.92;
                    }
                    case 6 -> { // retro: deslavado, cálido y con viñeta
                        r = 40 + r * 0.78 + 10;
                        g = 34 + g * 0.76;
                        b = 30 + b * 0.62;
                        double d = Math.hypot(x - cx, y - cy) / maxD;
                        double v = 1 - 0.55 * d * d;
                        r *= v;
                        g *= v;
                        b *= v;
                    }
                    case 7 -> { // noche: azul y oscuro
                        r = r * 0.55;
                        g = g * 0.7 + 6;
                        b = b * 1.0 + 30;
                        double d = Math.hypot(x - cx, y - cy) / maxD;
                        double v = 1 - 0.4 * d * d;
                        r *= v;
                        g *= v;
                        b *= v;
                    }
                    default -> {
                    }
                }
                out.setRGB(x, y, clamp(r) << 16 | clamp(g) << 8 | clamp(b));
            }
        }
        return out;
    }

    /** Achica la imagen (para las miniaturas de los filtros). */
    public static BufferedImage shrink(BufferedImage src, int out) {
        BufferedImage bi = new BufferedImage(out, out, BufferedImage.TYPE_INT_RGB);
        int w = src.getWidth();
        double step = w / (double) out;
        for (int oy = 0; oy < out; oy++) {
            for (int ox = 0; ox < out; ox++) {
                long r = 0, g = 0, b = 0;
                int k = 0;
                for (int y = (int) (oy * step); y < (int) ((oy + 1) * step); y++) {
                    for (int x = (int) (ox * step); x < (int) ((ox + 1) * step); x++) {
                        int c = src.getRGB(Math.min(x, w - 1), Math.min(y, src.getHeight() - 1));
                        r += c >> 16 & 255;
                        g += c >> 8 & 255;
                        b += c & 255;
                        k++;
                    }
                }
                if (k > 0) {
                    bi.setRGB(ox, oy, (int) (r / k) << 16 | (int) (g / k) << 8 | (int) (b / k));
                }
            }
        }
        return bi;
    }
}
