package com.mundosdeaxelis.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mundosdeaxelis.ClientConfig;
import com.mundosdeaxelis.MundosDeAxelis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Prueba con el juego de verdad, solo para el CI (workflow "Prueba con el cliente: Mundos de Axelis"). Se activa con
 * {@code -Dmundos.autotest=true}; sin esa opción no hace nada. Entra a un mundo plano, toma capturas (HUD, menú de pausa con el
 * cuadrito, pantalla de opciones, HUD sin la barra de almas, inventario con objetos del mod, bloques y enemigos de la Torre),
 * escribe en el registro líneas "AUTOTEST ..." con lo que comprobó y cierra el juego.
 */
@Mod.EventBusSubscriber(modid = MundosDeAxelis.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AutoTest {
    public static final boolean ON = Boolean.getBoolean("mundos.autotest");
    private static final double GROUND = -60;

    private record Step(String name, long pause, Runnable run) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int idx;
    private static long next;
    private static long joinedAt;
    private static long confirmAt;
    private static int shots, fails, checks;

    private AutoTest() {
    }

    static void log(String s) {
        MundosDeAxelis.LOGGER.info("AUTOTEST " + s);
    }

    private static void check(String what, boolean ok) {
        checks++;
        if (!ok) {
            fails++;
        }
        log((ok ? "OK " : "FALLO ") + what);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            // el mundo de prueba lo creó un servidor: Forge avisa de "ajustes experimentales" (dimensiones del mod) y espera un clic
            if (mc.screen instanceof net.minecraft.client.gui.screens.ConfirmScreen && System.currentTimeMillis() > confirmAt) {
                confirmAt = System.currentTimeMillis() + 3000;
                for (GuiEventListener l : mc.screen.children()) {
                    if (l instanceof Button b) {
                        log("aviso al abrir el mundo: se pulsa \"" + b.getMessage().getString() + "\"");
                        b.onPress();
                        break;
                    }
                }
            }
            return;
        }
        long now = System.currentTimeMillis();
        if (joinedAt == 0) {
            joinedAt = now;
            next = now + 12000; // que carguen los chunks y pasen los avisos de bienvenida
            build();
            log("inicio pasos=" + STEPS.size());
            return;
        }
        if (idx >= STEPS.size() || now < next) {
            return;
        }
        Step s = STEPS.get(idx++);
        try {
            s.run.run();
        } catch (Throwable t) {
            fails++;
            log("FALLO en " + s.name + ": " + t);
            MundosDeAxelis.LOGGER.error("AUTOTEST", t);
        }
        next = System.currentTimeMillis() + s.pause;
    }

    private static void add(String name, long wait, Runnable r) {
        STEPS.add(new Step(name, wait, r));
    }

    /** Una foto: primero prepara, espera a que se dibuje y luego la toma. */
    private static void photo(String name, long wait, Runnable setup) {
        add(name, wait, setup);
        add(name + " (foto)", 300, () -> shot(name));
    }

    static void cmd(String c) {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        s.execute(() -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c));
    }

    private static void open(Screen sc) {
        Minecraft.getInstance().setScreen(sc);
    }

    /** Me pongo en (x, z) mirando hacia el punto (tx, ty, tz). */
    private static void lookFrom(double x, double z, double tx, double ty, double tz) {
        double ey = GROUND + 1.62;
        double dx = tx - x, dy = ty - ey, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        cmd(String.format(Locale.ROOT, "tp @a %.3f %.1f %.3f %.2f %.2f", x, GROUND, z, yaw, pitch));
    }

    /** Guarda lo que se ve en {@code screenshots/axelis_<nombre>.png}. */
    static void shot(String name) {
        try (NativeImage n = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            int w = n.getWidth(), h = n.getHeight();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int c = n.getPixelRGBA(x, y); // ABGR
                    bi.setRGB(x, y, (c & 0xFF) << 16 | (c & 0xFF00) | (c >> 16 & 0xFF));
                }
            }
            Path dir = FMLPaths.GAMEDIR.get().resolve("screenshots");
            Files.createDirectories(dir);
            javax.imageio.ImageIO.write(bi, "png", dir.resolve("axelis_" + name + ".png").toFile());
            shots++;
            log("foto " + name);
        } catch (Exception ex) {
            fails++;
            log("FALLO foto " + name + ": " + ex);
        }
    }

    /** Pone el ratón en ese punto de la interfaz (solo en la prueba; en el entorno de desarrollo los nombres no están ofuscados). */
    private static void hover(int guiX, int guiY) {
        Minecraft mc = Minecraft.getInstance();
        double k = mc.getWindow().getGuiScale();
        try {
            for (String f : new String[]{"xpos", "ypos"}) {
                java.lang.reflect.Field fd = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                fd.setAccessible(true);
                fd.setDouble(mc.mouseHandler, (f.equals("xpos") ? guiX : guiY) * k);
            }
        } catch (Exception ex) {
            log("no se pudo mover el ratón: " + ex);
        }
    }

    private static <T> T find(Class<T> type, String simpleName) {
        Screen sc = Minecraft.getInstance().screen;
        if (sc == null) {
            return null;
        }
        for (GuiEventListener l : sc.children()) {
            if (type.isInstance(l) && (simpleName == null || l.getClass().getSimpleName().equals(simpleName))) {
                return type.cast(l);
            }
        }
        return null;
    }

    private static String screenName() {
        Screen sc = Minecraft.getInstance().screen;
        return sc == null ? "ninguna" : sc.getClass().getSimpleName();
    }

    private static void build() {
        add("reglas", 800, () -> {
            cmd("gamerule sendCommandFeedback false");
            cmd("gamerule doDaylightCycle false");
            cmd("gamerule doWeatherCycle false");
            cmd("gamerule doMobSpawning false");
            cmd("time set 6000");
            cmd("weather clear 999999");
            cmd("kill @e[type=!player]");
            cmd("difficulty peaceful");
        });
        add("objetos", 2500, () -> {
            open(null);
            cmd("gamemode survival @a");
            cmd("clear @a");
            cmd("item replace entity @a hotbar.0 with mundos_de_axelis:magma_boots");
            cmd("item replace entity @a hotbar.1 with mundos_de_axelis:est_pila_libros");
            cmd("item replace entity @a hotbar.2 with mundos_de_axelis:portal_stone_tower");
            cmd("item replace entity @a hotbar.3 with mundos_de_axelis:archimago_spawn_egg");
            cmd("item replace entity @a hotbar.4 with mundos_de_axelis:esbirro_spawn_egg");
            cmd("item replace entity @a hotbar.5 with mundos_de_axelis:libro_embrujado_spawn_egg");
            cmd("item replace entity @a hotbar.6 with mundos_de_axelis:armadura_encantada_spawn_egg");
            cmd("item replace entity @a armor.feet with mundos_de_axelis:magma_boots");
            cmd("mundos desbloquear 5");
            cmd("tp @a 0.5 -60 0.5 0 0");
        });
        // 1. HUD normal (con la barra de almas a la derecha)
        photo("1_hud", 1500, () -> open(null));
        // 2. menú de pausa con el cuadrito arriba a la izquierda
        photo("2_pausa", 1500, () -> open(new PauseScreen(true)));
        add("cuadrito", 200, () -> {
            Button b = find(Button.class, "IconButton");
            check("cuadrito en el menú de pausa (pantalla " + screenName() + ")", b != null);
            if (b != null) {
                check("cuadrito arriba a la izquierda x=" + b.getX() + " y=" + b.getY() + " " + b.getWidth() + "x" + b.getHeight(),
                        b.getX() < 40 && b.getY() < 40);
                b.onPress();
            }
        });
        add("pantalla de opciones", 1500, () -> check("el cuadrito abre las opciones (pantalla " + screenName() + ")",
                Minecraft.getInstance().screen instanceof MundosOptionsScreen));
        add("opciones (foto)", 300, () -> shot("3_opciones"));
        photo("3b_opciones_ayuda", 800, () -> {
            // el ratón encima de "Sacudidas de cámara" para ver la explicación
            List<CycleButton<?>> cs = new ArrayList<>();
            for (GuiEventListener l : Minecraft.getInstance().screen.children()) {
                if (l instanceof CycleButton<?> c) {
                    cs.add(c);
                }
            }
            check("interruptores en las opciones: " + cs.size() + " de 6", cs.size() == 6);
            if (cs.size() > 4) {
                hover(cs.get(4).getX() + 30, cs.get(4).getY() + 10);
            }
        });
        add("ratón fuera", 300, () -> hover(1, 1));
        add("apagar barra de almas", 600, () -> {
            CycleButton<?> c = find(CycleButton.class, null);
            check("hay interruptores en las opciones", c != null);
            if (c != null) {
                c.onPress(); // el primero es la barra de almas
            }
        });
        add("opciones apagado (foto)", 300, () -> shot("4_opciones_sin_almas"));
        add("revisar guardado", 300, () -> {
            check("la opción quedó apagada en memoria", !ClientConfig.SHOW_SOULS.get());
            try {
                Path f = FMLPaths.CONFIGDIR.get().resolve("mundos_de_axelis-client.toml");
                String txt = Files.exists(f) ? Files.readString(f) : "";
                check("la opción se guardó en el archivo (show_souls = false)", txt.replace(" ", "").contains("show_souls=false"));
            } catch (Exception ex) {
                check("leer el archivo de opciones: " + ex, false);
            }
        });
        add("hecho", 600, () -> {
            Button done = null;
            Screen sc = Minecraft.getInstance().screen;
            if (sc != null) {
                for (GuiEventListener l : sc.children()) {
                    if (l instanceof Button b && !(l instanceof CycleButton<?>)) {
                        done = b;
                    }
                }
            }
            if (done != null) {
                done.onPress();
            }
            check("Hecho vuelve al menú de pausa (pantalla " + screenName() + ")", Minecraft.getInstance().screen instanceof PauseScreen);
        });
        photo("5_hud_sin_almas", 1500, () -> open(null));
        add("volver a encender", 300, () -> {
            ClientConfig.SHOW_SOULS.set(true);
            ClientConfig.SPEC.save();
        });
        // 3. inventario con objetos del mod (icono animado de las Botas de Magma)
        photo("6_inventario", 1500, () -> open(new InventoryScreen(Minecraft.getInstance().player)));
        add("cerrar", 600, () -> open(null));
        // 4. pila de libros en las cuatro direcciones
        add("bloques", 1200, () -> {
            cmd("setblock 0 -60 4 mundos_de_axelis:est_pila_libros[facing=north]");
            cmd("setblock 2 -60 4 mundos_de_axelis:est_pila_libros[facing=east]");
            cmd("setblock 4 -60 4 mundos_de_axelis:est_pila_libros[facing=south]");
            cmd("setblock 6 -60 4 mundos_de_axelis:est_pila_libros[facing=west]");
        });
        photo("7_pila_libros", 2000, () -> lookFrom(3.0, 0.5, 3.5, GROUND + 0.4, 4.5));
        add("comprobar bloque", 300, () -> check("la pila de libros está puesta",
                "est_pila_libros".equals(net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(
                        Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(0, -60, 4)).getBlock()).getPath())));
        // 5. enemigos de la Torre y el Archimago (quietos)
        add("enemigos", 2500, () -> {
            cmd("difficulty normal");
            cmd("summon mundos_de_axelis:esbirro -3 -60 10 {NoAI:1b,Rotation:[180f,0f]}");
            cmd("summon mundos_de_axelis:armadura_encantada 0 -60 10 {NoAI:1b,Rotation:[180f,0f]}");
            cmd("summon mundos_de_axelis:libro_embrujado 3 -58.5 10 {NoAI:1b,Rotation:[180f,0f]}");
            cmd("summon mundos_de_axelis:archimago 7 -60 12 {NoAI:1b,Rotation:[180f,0f]}");
        });
        photo("8_enemigos", 2500, () -> lookFrom(2.0, 2.5, 2.0, GROUND + 1.2, 10.5));
        add("contar enemigos", 300, () -> {
            var lv = Minecraft.getInstance().level;
            int n = 0;
            for (var en : lv.entitiesForRendering()) {
                String id = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(en.getType()).getPath();
                if (id.equals("esbirro") || id.equals("armadura_encantada") || id.equals("libro_embrujado") || id.equals("archimago")) {
                    n++;
                }
            }
            check("enemigos a la vista en el cliente: " + n + " de 4", n == 4);
        });
        // 6. la Piedra de Portal de la Torre, como un jugador: clic derecho, ficha, «Comenzar» y entrar (etapa 5)
        add("piedra de la torre", 1500, () -> {
            cmd("kill @e[type=!player]");
            cmd("setblock 0 -60 -3 mundos_de_axelis:portal_stone_tower");
            cmd("tp @a 0.5 -60 -0.5 180 35");
            Minecraft.getInstance().player.getInventory().selected = 8; // mano vacía
        });
        add("clic derecho", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, -60, -3);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0),
                            net.minecraft.core.Direction.UP, pos, false));
        });
        add("ficha abierta", 300, () -> check("la piedra abre la ficha del jefe (pantalla " + screenName() + ")",
                Minecraft.getInstance().screen instanceof BossBriefScreen));
        add("ficha (foto)", 5500, () -> shot("9_ficha_torre"));
        add("comenzar", 30000, () -> {
            // lo mismo que hace el botón «Comenzar» (activo a los 5 s)
            com.mundosdeaxelis.network.ModNetwork.CHANNEL.sendToServer(
                    new com.mundosdeaxelis.network.BossStartPacket(new net.minecraft.core.BlockPos(0, -60, -3)));
            open(null);
        });
        add("en la torre", 300, () -> {
            String dim = Minecraft.getInstance().level.dimension().location().getPath();
            check("la piedra lleva a la Torre (dimensión " + dim + ")", dim.startsWith("boss_tower"));
        });
        add("torre (foto)", 300, () -> shot("10_torre"));
        add("fin", 2000, () -> {
            cmd("kill @e[type=!player]");
            open(null);
            log("AUTOTEST_FIN fotos=" + shots + " comprobaciones=" + checks + " fallos=" + fails);
        });
        add("salir", 1000, () -> Minecraft.getInstance().stop());
    }
}
