package com.axeliscore.test;

import com.axeliscore.AxelisCore;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Pruebas con el juego de verdad (para el CI): con {@code -Daxcore.autotest=<escenario>} el cliente entra al mundo, sigue los pasos
 * del escenario (comandos, mover la cámara, esperar), toma fotos en {@code screenshots/<escenario>_<nombre>.png} y cierra el juego.
 * Cualquier mod que use el Core puede registrar su escenario con {@link #scenario}. Sirve en 1.20.1, 1.21.1 y 26.3.
 */
public final class ClientTest {
    private static final String WANT = System.getProperty("axcore.autotest", "");
    private static final Map<String, Consumer<Script>> SCENARIOS = new LinkedHashMap<>();
    private static Script running;
    private static boolean started;
    private static volatile boolean pending;

    private ClientTest() {
    }

    /** Registra un escenario (normalmente desde el arranque del cliente del mod). */
    public static void scenario(String name, Consumer<Script> builder) {
        SCENARIOS.put(name, builder);
    }

    public static boolean enabled() {
        return !WANT.isEmpty();
    }

    /** Lo llama el Core al arrancar el cliente: si hay prueba pedida, arranca el reloj. */
    public static synchronized void startIfEnabled() {
        if (!enabled() || started) {
            return;
        }
        started = true;
        CoreScenario.register();
        ScheduledExecutorService ex = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AxelisCore-Prueba");
            t.setDaemon(true);
            return t;
        });
        ex.scheduleAtFixedRate(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && !pending) {
                pending = true;
                mc.execute(ClientTest::tick);
            }
        }, 3000, 50, TimeUnit.MILLISECONDS);
    }

    private static void tick() {
        pending = false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
                return;
            }
            if (running == null) {
                running = new Script(WANT);
                for (String n : WANT.split(",")) {
                    Consumer<Script> b = SCENARIOS.get(n.trim());
                    if (b == null) {
                        running.log("FALLO no existe el escenario " + n);
                        running.fails++;
                    } else {
                        running.prefix = n.trim();
                        b.accept(running);
                    }
                }
                running.add("fin", 1500, () -> {
                    running.log("AXCORE_AUTOTEST_FIN fotos=" + running.shots + " fallos=" + running.fails);
                    Minecraft.getInstance().stop();
                });
                running.next = System.currentTimeMillis() + 10000; // que carguen los chunks
                running.log("inicio pasos=" + running.steps.size());
                return;
            }
            running.step();
        } catch (Throwable e) {
            AxelisCore.LOGGER.error("AXCORE_AUTOTEST", e);
        }
    }

    /** Los pasos de una prueba. */
    public static final class Script {
        private record Step(String name, long pause, Runnable run) {
        }

        final List<Step> steps = new ArrayList<>();
        String prefix;
        int idx, shots, fails;
        long next;

        Script(String name) {
            this.prefix = name;
        }

        void step() {
            if (idx >= steps.size() || System.currentTimeMillis() < next) {
                return;
            }
            Step s = steps.get(idx++);
            try {
                s.run.run();
            } catch (Throwable e) {
                fails++;
                log("FALLO en " + s.name + ": " + e);
                AxelisCore.LOGGER.error("AXCORE_AUTOTEST", e);
            }
            next = System.currentTimeMillis() + s.pause;
        }

        public void log(String s) {
            AxelisCore.LOGGER.info("AXCORE_AUTOTEST " + s);
        }

        /** Un paso: hace algo y espera {@code waitMs} antes del siguiente. */
        public Script add(String name, long waitMs, Runnable r) {
            String p = prefix;
            steps.add(new Step(name, waitMs, () -> {
                prefix = p;
                r.run();
            }));
            return this;
        }

        /** Prepara algo, espera a que se vea y toma la foto. */
        public Script photo(String name, long waitMs, Runnable setup) {
            add(name, waitMs, setup);
            add(name + " (foto)", 300, () -> shot(name));
            return this;
        }

        /** Un comando como el servidor (con permisos). */
        public void cmd(String c) {
            IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
            s.execute(() -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c));
        }

        /** Me pongo en (x, y, z) mirando hacia (tx, ty, tz). */
        public void lookFrom(double x, double y, double z, double tx, double ty, double tz) {
            double dx = tx - x, dy = ty - (y + 1.62), dz = tz - z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
            cmd(String.format(Locale.ROOT, "tp @a %.3f %.3f %.3f %.2f %.2f", x, y, z, yaw, pitch));
        }

        /** Esconde la interfaz (barra, mira) para que las fotos salgan limpias (en 26.x no se puede de esta forma). */
        public void hud(boolean show) {
            //#if MC < 26.1
            Minecraft.getInstance().options.hideGui = !show;
            //#endif
        }

        public void check(boolean ok, String what) {
            if (!ok) {
                throw new AssertionError(what);
            }
            log("ok: " + what);
        }

        /** Guarda lo que se ve ahora. */
        public void shot(String name) {
            String file = prefix + "_" + name;
            screenshot(img -> {
                try (NativeImage n = img) {
                    int w = n.getWidth(), h = n.getHeight();
                    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    for (int y = 0; y < h; y++) {
                        for (int x = 0; x < w; x++) {
                            bi.setRGB(x, y, argb(n, x, y) & 0xFFFFFF);
                        }
                    }
                    Path dir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("screenshots");
                    Files.createDirectories(dir);
                    javax.imageio.ImageIO.write(bi, "png", dir.resolve(file + ".png").toFile());
                } catch (Exception e) {
                    fails++;
                    log("FALLO foto " + file + ": " + e);
                }
            });
            shots++;
            log("foto " + file);
        }
    }

    static void screenshot(Consumer<NativeImage> cb) {
        //#if MC >= 26.3
        //$$ Screenshot.takeScreenshot(Minecraft.getInstance().gameRenderer.mainRenderTarget(), cb);
        //#elif MC >= 1.21.5
        //$$ Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), cb);
        //#else
        cb.accept(Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget()));
        //#endif
    }

    static int argb(NativeImage n, int x, int y) {
        //#if MC >= 1.21.2
        //$$ return n.getPixel(x, y);
        //#else
        int c = n.getPixelRGBA(x, y); // viene como ABGR
        return (c & 0xFF00FF00) | (c & 0xFF) << 16 | (c >> 16 & 0xFF);
        //#endif
    }
}
