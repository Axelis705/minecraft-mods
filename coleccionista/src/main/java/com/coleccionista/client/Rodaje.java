package com.coleccionista.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * (Cliente) El rodaje del tráiler: en la prueba con el juego de verdad ({@code -Dcoleccionista.rodaje=si}), en ciertas fotos el mundo se
 * congela (una pantalla de pausa invisible: el servidor de un jugador deja de avanzar) y una cámara de cine se mueve alrededor de lo que
 * muestra la foto, cuadro por cuadro, a 30 cuadros por segundo de video (sin importar qué tan lento dibuje la computadora). Cada toma
 * queda como JPEG en {@code screenshots/trailer/<foto>/0001.jpg...} y el flujo de GitHub las junta en videos.
 * <p>
 * Los movimientos salen de la propia foto (de dónde está la cámara y hacia dónde mira), así no hay que adivinar coordenadas:
 * empuje (avanza), retroceso, lateral, grúa (sube) y órbita (da la vuelta alrededor de lo que se ve). Antes de moverse revisa con rayos
 * cuánto espacio libre hay para no meter la cámara en una pared.
 */
public final class Rodaje {
    public static final boolean ACTIVO = "si".equals(System.getProperty("coleccionista.rodaje", ""));
    private static final int FPS = 30;

    /** Una toma: el movimiento y cuántos segundos dura. */
    private record Toma(String mov, float seg) {
    }

    private static final Map<String, Toma> TOMAS = new HashMap<>();

    static {
        // el despertar y las celdas
        toma("23_celda_luz", "empuje", 3.5F);
        toma("24_tele_habla", "empuje", 4.5F);
        toma("25b_zoom_subtitulo", "retroceso", 3.5F);
        toma("26b_reja_cerrada", "lateral", 3.5F);
        toma("27_pasillo", "empuje", 4.5F);
        toma("30_patio", "grua", 4.5F);
        // capítulo 1
        toma("c1_52_sala_grande", "empuje", 4F);
        toma("c1_53_ascensores", "lateral", 3.5F);
        toma("c1_58_pasillo_munecas", "empuje", 4F);
        toma("c1_59_postes", "orbita", 3.5F);
        toma("c1_61_baldosas", "grua", 3.5F);
        toma("c1_66_vitrinas", "lateral", 4F);
        toma("c1_67_cera", "empuje", 3.5F);
        // capítulo 2: la mascota
        toma("c2_02_vestibulo", "grua", 4F);
        toma("c2_03_jaula", "empuje", 3.5F);
        toma("c2_06_cascabel", "orbita", 4F);
        toma("c2_08_ruge", "empuje", 2.5F);
        toma("c2_10_atrapa", "retroceso", 3F);
        // capítulo 3: la caja de música
        toma("c3_02_salon", "lateral", 4F);
        toma("c3_02c_gondolas", "empuje", 3.5F);
        toma("c3_08_circulo", "orbita", 4F);
        toma("c3_11b_carga", "empuje", 2.5F);
        toma("c3_12_bang", "retroceso", 2.5F);
        // capítulo 4: la caja sorpresa
        toma("c4_02c_gradas", "lateral", 3.5F);
        toma("c4_04_manivela", "empuje", 3F);
        toma("c4_05_salta", "orbita", 4.5F);
        toma("c4_07_mordida", "empuje", 2.5F);
        // capítulo 5: la hora del té
        toma("c5_02_salon", "empuje", 4F);
        toma("c5_05_mesa", "orbita", 4F);
        toma("c5_06_manos", "empuje", 3F);
        toma("c5_09_veneno", "retroceso", 3F);
        // capítulo 6: el piano gigante
        toma("c6_03_bailarina", "orbita", 4.5F);
        toma("c6_05_toca", "empuje", 3F);
        toma("c6_08_desde_arriba", "grua", 4F);
        toma("c6_09_se_abre", "empuje", 3F);
        // capítulo 7: veintiuno
        toma("c7_03_mesa", "empuje", 4F);
        toma("c7_04_sentado", "orbita", 3.5F);
        toma("c7_08_aprieta", "empuje", 3F);
        toma("c7_10_pum", "retroceso", 2.5F);
        toma("c7_13_la_mesa_al_final", "orbita", 4.5F);
    }

    private static void toma(String foto, String mov, float seg) {
        TOMAS.put(foto, new Toma(mov, seg));
    }

    private static ArmorStand cam;
    private static Entity antes;
    private static boolean rodando, listo = true, guiAntes;
    private static String nombre = "";
    private static Path dir;
    private static int cuadro, total, calentar;
    /** El camino: posiciones de la cámara (al inicio y al final, o la órbita) y a dónde mira. */
    private static Vec3 desde, hasta, pivote;
    private static double radio, ang0, ang1, altura;
    private static boolean orbita;
    private static final ExecutorService ESCRIBIR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "rodaje-jpeg");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicInteger PENDIENTES = new AtomicInteger();

    private Rodaje() {
    }

    public static void init() {
        if (!ACTIVO) {
            return;
        }
        MinecraftForge.EVENT_BUS.addListener(Rodaje::cuadro);
        MinecraftForge.EVENT_BUS.addListener((RenderGuiEvent.Pre e) -> {
            if (rodando) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderHandEvent e) -> {
            if (rodando) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderLivingEvent.Pre<?, ?> e) -> {
            if (rodando && e.getEntity() == Minecraft.getInstance().player) {
                e.setCanceled(true);
            }
        });
        MinecraftForge.EVENT_BUS.addListener((RenderNameTagEvent e) -> {
            if (rodando) {
                e.setResult(Event.Result.DENY);
            }
        });
    }

    /** ¿Hay toma para esta foto? */
    public static boolean hay(String foto) {
        return ACTIVO && TOMAS.containsKey(foto);
    }

    public static boolean listo() {
        return listo && PENDIENTES.get() == 0;
    }

    /** Empieza la toma de esa foto: congela el mundo y prepara el camino de la cámara desde lo que se ve ahora. */
    public static void empezar(String foto) {
        Minecraft mc = Minecraft.getInstance();
        Toma t = TOMAS.get(foto);
        if (t == null || mc.level == null || mc.player == null) {
            return;
        }
        antes = mc.getCameraEntity() == null ? mc.player : mc.getCameraEntity();
        Vec3 ojo = antes.getEyePosition(1F);
        Vec3 mira = antes.getViewVector(1F).normalize();
        double d = libre(ojo, mira, 18);
        double r = Mth.clamp(d * 0.85, 2.5, 7);
        Vec3 blanco = ojo.add(mira.scale(r));
        Vec3 der = new Vec3(-mira.z, 0, mira.x);
        if (der.lengthSqr() < 1e-4) {
            der = new Vec3(1, 0, 0);
        }
        der = der.normalize();
        Vec3 arriba = new Vec3(0, 1, 0);
        orbita = false;
        pivote = blanco;
        switch (t.mov()) {
            case "empuje" -> {
                double atras = Math.min(1.4, libre(ojo, mira.scale(-1), 3) - 0.35);
                double frente = Math.min(Math.min(1.8, r * 0.4), d - 1.2);
                desde = ojo.add(mira.scale(-Math.max(0, atras)));
                hasta = ojo.add(mira.scale(Math.max(0.2, frente)));
            }
            case "retroceso" -> {
                double frente = Math.min(0.8, d - 1.2);
                double atras = Math.min(2.2, libre(ojo, mira.scale(-1), 4) - 0.35);
                desde = ojo.add(mira.scale(Math.max(0, frente)));
                hasta = ojo.add(mira.scale(-Math.max(0.2, atras)));
            }
            case "lateral" -> {
                double izq = Math.min(1.6, libre(ojo, der.scale(-1), 3) - 0.4);
                double de = Math.min(1.6, libre(ojo, der, 3) - 0.4);
                desde = ojo.add(der.scale(-Math.max(0, izq)));
                hasta = ojo.add(der.scale(Math.max(0, de)));
            }
            case "grua" -> {
                double abajo = Math.min(0.9, libre(ojo, arriba.scale(-1), 2) - 0.35);
                double sube = Math.min(2.2, libre(ojo, arriba, 3.5) - 0.4);
                desde = ojo.add(arriba.scale(-Math.max(0, abajo)));
                hasta = ojo.add(arriba.scale(Math.max(0.2, sube)));
            }
            default -> {
                // órbita: alrededor del blanco, a la misma altura, de un lado a otro (menos si hay paredes)
                orbita = true;
                Vec3 v = ojo.subtract(blanco);
                radio = Math.hypot(v.x, v.z);
                altura = v.y;
                double base = Math.atan2(v.z, v.x);
                double abre = Math.toRadians(24);
                for (int k = 0; k < 4 && !(libreOrbita(base - abre) && libreOrbita(base + abre)); k++) {
                    abre *= 0.6;
                }
                ang0 = base - abre;
                ang1 = base + abre;
            }
        }
        if (cam == null || cam.level() != mc.level) {
            cam = new ArmorStand(EntityType.ARMOR_STAND, mc.level);
            cam.setInvisible(true);
            cam.setNoGravity(true);
        }
        nombre = foto;
        total = Math.max(2, Math.round(t.seg() * FPS));
        cuadro = 0;
        calentar = 4;
        dir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("screenshots").resolve("trailer").resolve(foto);
        try {
            Files.createDirectories(dir);
        } catch (Exception e) {
            com.coleccionista.Coleccionista.LOGGER.warn("COLECCIONISTA_RODAJE no se pudo crear {}", dir);
        }
        guiAntes = mc.options.hideGui;
        mc.options.hideGui = true;
        mc.setScreen(new Pausa());
        poner(0);
        mc.setCameraEntity(cam);
        rodando = true;
        listo = false;
        com.coleccionista.Coleccionista.LOGGER.info("COLECCIONISTA_RODAJE empieza {} {} cuadros={} d={}", foto, t.mov(), total,
                String.format(java.util.Locale.ROOT, "%.1f", d));
    }

    /** Cuántos bloques libres hay desde p hacia la dirección v (hasta max). */
    private static double libre(Vec3 p, Vec3 v, double max) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 fin = p.add(v.normalize().scale(max));
        BlockHitResult h = mc.level.clip(new ClipContext(p, fin, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cam != null ? cam : mc.player));
        return h.getType() == HitResult.Type.MISS ? max : h.getLocation().distanceTo(p);
    }

    private static boolean libreOrbita(double ang) {
        Vec3 p = pivote.add(Math.cos(ang) * radio, altura, Math.sin(ang) * radio);
        Vec3 d = p.subtract(pivote);
        return libre(pivote, d, d.length() + 0.4) >= d.length() + 0.3;
    }

    private static float suave(float x) {
        x = Mth.clamp(x, 0, 1);
        return x * x * (3 - 2 * x);
    }

    /** La cámara en el cuadro i. */
    private static void poner(int i) {
        float s = suave(total <= 1 ? 1 : i / (float) (total - 1));
        Vec3 p;
        if (orbita) {
            double a = ang0 + (ang1 - ang0) * s;
            p = pivote.add(Math.cos(a) * radio, altura, Math.sin(a) * radio);
        } else {
            p = desde.lerp(hasta, s);
        }
        Vec3 d = pivote.subtract(p);
        if (!orbita && d.lengthSqr() < 0.25) {
            d = hasta.subtract(desde);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.hypot(d.x, d.z)));
        double y = p.y - cam.getEyeHeight();
        cam.setPos(p.x, y, p.z);
        cam.xo = p.x;
        cam.yo = y;
        cam.zo = p.z;
        cam.xOld = p.x;
        cam.yOld = y;
        cam.zOld = p.z;
        cam.setYRot(yaw);
        cam.yRotO = yaw;
        cam.setXRot(pitch);
        cam.xRotO = pitch;
        cam.yHeadRot = yaw;
        cam.yHeadRotO = yaw;
    }

    private static void cuadro(TickEvent.RenderTickEvent e) {
        if (!rodando) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || cam == null || cam.level() != mc.level) {
            terminar();
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            if (!(mc.screen instanceof Pausa)) {
                mc.setScreen(new Pausa());
            }
            if (mc.getCameraEntity() != cam) {
                mc.setCameraEntity(cam);
            }
            poner(cuadro);
            return;
        }
        // fin del cuadro: ya se dibujó con la cámara en su lugar
        if (calentar > 0) {
            calentar--;
            return;
        }
        if (PENDIENTES.get() >= 8) {
            return;
        }
        guardar(cuadro);
        cuadro++;
        if (cuadro >= total) {
            terminar();
        }
    }

    private static void guardar(int i) {
        NativeImage n = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());
        int w = n.getWidth(), h = n.getHeight();
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = n.getPixelRGBA(x, y); // ABGR
                px[y * w + x] = (c & 0xFF) << 16 | (c & 0xFF00) | (c >> 16 & 0xFF);
            }
        }
        n.close();
        File f = dir.resolve(String.format("%04d.jpg", i + 1)).toFile();
        PENDIENTES.incrementAndGet();
        ESCRIBIR.submit(() -> {
            try {
                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                bi.setRGB(0, 0, w, h, px, 0, w);
                ImageWriter wr = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam p = wr.getDefaultWriteParam();
                p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                p.setCompressionQuality(0.93F);
                try (ImageOutputStream out = ImageIO.createImageOutputStream(f)) {
                    wr.setOutput(out);
                    wr.write(null, new IIOImage(bi, null, null), p);
                }
                wr.dispose();
            } catch (Exception ex) {
                com.coleccionista.Coleccionista.LOGGER.warn("COLECCIONISTA_RODAJE no se pudo guardar {}: {}", f, ex.toString());
            } finally {
                PENDIENTES.decrementAndGet();
            }
        });
    }

    private static void terminar() {
        Minecraft mc = Minecraft.getInstance();
        rodando = false;
        if (mc.screen instanceof Pausa) {
            mc.setScreen(null);
        }
        if (antes != null && antes.isAlive() && antes.level() == mc.level) {
            mc.setCameraEntity(antes);
        } else if (mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        mc.options.hideGui = guiAntes;
        listo = true;
        com.coleccionista.Coleccionista.LOGGER.info("COLECCIONISTA_RODAJE listo {} cuadros={}", nombre, cuadro);
    }

    /** La pausa invisible: congela el mundo (el servidor de un jugador deja de avanzar) y no dibuja nada. */
    static final class Pausa extends Screen {
        Pausa() {
            super(Component.empty());
        }

        @Override
        public boolean isPauseScreen() {
            return true;
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return false;
        }

        @Override
        public void render(GuiGraphics g, int mx, int my, float pt) {
        }
    }
}
