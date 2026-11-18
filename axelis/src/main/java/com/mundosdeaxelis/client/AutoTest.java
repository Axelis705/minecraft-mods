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
        // en el volcán, que el fuego en la pantalla no tape las fotos (la prueba tiene resistencia al fuego)
        if (mc.player.isOnFire() && mc.level.dimension().location().getPath().startsWith("boss_volcano")) {
            onServer(net.minecraft.world.entity.Entity::clearFire);
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

    /** Quita a los enemigos sin matarlos (si muere el Archimago cuenta como victoria y salen los premios en pantalla). */
    private static void clearMobs() {
        IntegratedServer srv = Minecraft.getInstance().getSingleplayerServer();
        srv.execute(() -> {
            for (net.minecraft.server.level.ServerLevel l : srv.getAllLevels()) {
                List<net.minecraft.world.entity.Entity> gone = new ArrayList<>();
                for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
                    if (e instanceof net.minecraft.world.entity.Mob) {
                        gone.add(e);
                    }
                }
                gone.forEach(net.minecraft.world.entity.Entity::discard);
            }
        });
    }

    /** Dónde se construye el volcán en la prueba: el plano (x, y, z) queda en el mundo en (x, VY + y, VZ + z). */
    private static final int VY = -52, VZ = 136;

    /** Como {@link #standLooking}, pero en coordenadas del plano del volcán. */
    private static void lookV(double x, double y, double z, double tx, double ty, double tz) {
        standLooking(x, VY + y, VZ + z, tx, VY + ty, VZ + tz);
    }

    /** Ejecuta algo en el servidor con mi jugador (en la dimensión donde esté). */
    private static void onServer(java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> c) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer srv = mc.getSingleplayerServer();
        java.util.UUID u = mc.player.getUUID();
        srv.execute(() -> {
            net.minecraft.server.level.ServerPlayer sp = srv.getPlayerList().getPlayer(u);
            if (sp != null) {
                c.accept(sp);
            }
        });
    }

    // ------------------------------------------------------------------ estudios para los retratos de las cartas

    /** X de cada estudio (todos en Z = {@link #SZ}, dentro de la zona que siempre está cargada cerca del punto de aparición). */
    private static final int ST_VOLCAN = -80, ST_FORJA = -110, ST_TORRE = -140, SZ = -60;

    /** Cuarto cerrado de 13 x 13 con suelo, pared del fondo decorada según el tema y luces invisibles. La criatura va en (x, -60, SZ). */
    private static void studio(int x, String theme) {
        int z = SZ;
        String wall = switch (theme) {
            case "volcan" -> "minecraft:blackstone";
            case "forja" -> "minecraft:polished_deepslate";
            default -> "minecraft:deepslate_bricks";
        };
        String floor = switch (theme) {
            case "volcan" -> "minecraft:polished_blackstone_bricks";
            case "forja" -> "minecraft:deepslate_tiles";
            default -> "minecraft:polished_deepslate";
        };
        List<String> c = new ArrayList<>();
        c.add(f("fill %d -61 %d %d -52 %d %s hollow", x - 7, z - 9, x + 7, z + 5, wall));
        c.add(f("fill %d -61 %d %d -61 %d %s", x - 6, z - 8, x + 6, z + 4, floor));
        switch (theme) {
            case "volcan" -> {
                c.add(f("fill %d -60 %d %d -53 %d minecraft:basalt", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -60 %d %d -60 %d minecraft:polished_blackstone_bricks", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -53 %d %d -53 %d minecraft:polished_blackstone_bricks", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -59 %d %d -54 %d mundos_de_axelis:lava_falsa", x - 3, z + 4, x - 3, z + 4));
                c.add(f("fill %d -59 %d %d -54 %d mundos_de_axelis:lava_falsa", x + 3, z + 4, x + 3, z + 4));
                c.add(f("fill %d -61 %d %d -61 %d mundos_de_axelis:lava_falsa", x - 6, z + 3, x + 6, z + 3));
                c.add(f("setblock %d -56 %d minecraft:crying_obsidian", x, z + 4));
                c.add(f("setblock %d -57 %d minecraft:crying_obsidian", x - 1, z + 4));
                c.add(f("setblock %d -57 %d minecraft:crying_obsidian", x + 1, z + 4));
                c.add(f("setblock %d -58 %d minecraft:magma_block", x, z + 4));
                for (int[] m : new int[][]{{-4, 1}, {4, -1}, {-2, -3}, {5, 2}, {-5, -2}, {2, 2}}) {
                    c.add(f("setblock %d -61 %d minecraft:magma_block", x + m[0], z + m[1]));
                }
                c.add(f("setblock %d -60 %d minecraft:soul_campfire", x - 5, z + 2));
                c.add(f("setblock %d -60 %d minecraft:soul_campfire", x + 5, z + 2));
            }
            case "forja" -> {
                c.add(f("fill %d -60 %d %d -53 %d minecraft:deepslate_bricks", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -59 %d %d -54 %d mundos_de_axelis:lava_falsa", x, z + 4, x, z + 4));
                c.add(f("fill %d -60 %d %d -53 %d minecraft:iron_bars", x - 1, z + 4, x - 1, z + 4));
                c.add(f("fill %d -60 %d %d -53 %d minecraft:iron_bars", x + 1, z + 4, x + 1, z + 4));
                c.add(f("fill %d -61 %d %d -61 %d mundos_de_axelis:lava_falsa", x - 6, z + 3, x + 6, z + 3));
                c.add(f("setblock %d -60 %d minecraft:blast_furnace[facing=north,lit=true]", x - 4, z + 2));
                c.add(f("setblock %d -60 %d minecraft:blast_furnace[facing=north,lit=true]", x + 4, z + 2));
                c.add(f("setblock %d -59 %d minecraft:blast_furnace[facing=north,lit=true]", x + 4, z + 2));
                c.add(f("setblock %d -60 %d minecraft:anvil[facing=east]", x - 5, z));
                c.add(f("fill %d -55 %d %d -53 %d minecraft:chain", x - 3, z + 2, x - 3, z + 2));
                c.add(f("fill %d -55 %d %d -53 %d minecraft:chain", x + 2, z + 1, x + 2, z + 1));
                c.add(f("setblock %d -56 %d minecraft:lantern[hanging=true]", x - 3, z + 2));
                c.add(f("setblock %d -56 %d minecraft:lantern[hanging=true]", x + 2, z + 1));
                for (int[] m : new int[][]{{-3, 0}, {3, -2}, {5, 1}, {-1, -4}}) {
                    c.add(f("setblock %d -61 %d minecraft:smooth_basalt", x + m[0], z + m[1]));
                }
            }
            default -> {
                c.add(f("fill %d -60 %d %d -56 %d minecraft:bookshelf", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -55 %d %d -53 %d minecraft:deepslate_tiles", x - 6, z + 4, x + 6, z + 4));
                c.add(f("fill %d -60 %d %d -53 %d minecraft:amethyst_block", x - 4, z + 4, x - 4, z + 4));
                c.add(f("fill %d -60 %d %d -53 %d minecraft:amethyst_block", x + 4, z + 4, x + 4, z + 4));
                c.add(f("fill %d -56 %d %d -54 %d minecraft:purple_stained_glass", x - 1, z + 4, x + 1, z + 4));
                c.add(f("fill %d -56 %d %d -54 %d minecraft:sea_lantern", x - 1, z + 5, x + 1, z + 5));
                c.add(f("setblock %d -55 %d minecraft:purple_candle[candles=3,lit=true]", x - 3, z + 4));
                c.add(f("setblock %d -55 %d minecraft:purple_candle[candles=4,lit=true]", x + 3, z + 4));
                c.add(f("setblock %d -55 %d minecraft:purple_candle[candles=2,lit=true]", x - 6, z + 4));
                c.add(f("setblock %d -55 %d minecraft:purple_candle[candles=2,lit=true]", x + 6, z + 4));
                c.add(f("fill %d -60 %d %d -60 %d minecraft:purple_carpet", x - 1, z - 8, x + 1, z + 3));
                c.add(f("setblock %d -53 %d minecraft:soul_lantern[hanging=true]", x - 3, z + 2));
                c.add(f("setblock %d -53 %d minecraft:soul_lantern[hanging=true]", x + 3, z + 2));
                c.add(f("setblock %d -60 %d minecraft:enchanting_table", x - 5, z + 2));
                c.add(f("setblock %d -60 %d minecraft:lectern[facing=west]", x + 5, z + 2));
            }
        }
        // luces invisibles alrededor de la criatura (se ve bien iluminada sin lámparas en la foto)
        c.add(f("setblock %d -58 %d minecraft:light[level=15]", x - 2, z - 1));
        c.add(f("setblock %d -58 %d minecraft:light[level=15]", x + 2, z - 1));
        c.add(f("setblock %d -55 %d minecraft:light[level=13]", x, z - 1));
        c.add(f("setblock %d -59 %d minecraft:light[level=12]", x, z - 3));
        for (String k : c) {
            cmd("execute in minecraft:overworld run " + k);
        }
    }

    private static String f(String fmt, Object... a) {
        return String.format(Locale.ROOT, fmt, a);
    }

    /**
     * Un retrato: pone la criatura quieta en el centro del estudio, mirando con el giro {@code yaw}, y la cámara delante a {@code d}
     * bloques, un poco por encima ({@code up}) de su centro {@code cy}; quita la interfaz y saca la foto {@code carta_<nombre>}.
     */
    private static void portrait(String card, int x, String id, double feetY, double cy, double d, double up, float yaw) {
        add("retrato " + card, 1700, () -> {
            clearMobs();
            cmd(f("execute in minecraft:overworld run summon mundos_de_axelis:%s %.1f %.2f %.1f "
                    + "{NoAI:1b,NoGravity:1b,Silent:1b,Invulnerable:1b,PersistenceRequired:1b,Rotation:[%.1ff,0f]}", id, x + 0.5, feetY, SZ + 0.5, yaw));
            standLooking(x + 0.5, cy + up - 1.62, SZ + 0.5 - d, x + 0.5, cy, SZ + 0.5);
            Minecraft.getInstance().options.hideGui = true;
        });
        add("retrato " + card + " (foto)", 300, () -> {
            int n = 0;
            for (net.minecraft.world.entity.Entity e : Minecraft.getInstance().level.entitiesForRendering()) {
                if (id.equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(e.getType()).getPath())) {
                    n++;
                }
            }
            check("retrato de " + card + " (en la foto: " + n + ")", n == 1);
            shot("carta_" + card);
        });
    }

    /** Me pone de pie en (x, y, z) de la dimensión donde estoy, mirando al punto (tx, ty, tz). */
    private static void standLooking(double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - (y + 1.62), dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), x, y, z, yaw, pitch);
            sp.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        });
    }

    /** El Corazón de Magma del volcán donde estoy (en el servidor). */
    private static void heart(java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, com.mundosdeaxelis.entity.CorazonMagma> c) {
        onServer(sp -> {
            com.mundosdeaxelis.entity.CorazonMagma h = com.mundosdeaxelis.world.FireTravel.find(sp.serverLevel());
            if (h == null) {
                log("FALLO no hay Corazón de Magma en " + sp.serverLevel().dimension().location());
                fails++;
                return;
            }
            c.accept(sp, h);
        });
    }

    /** Rompe (quita) todas las anclas de las cadenas, como si los jugadores las hubieran destrozado. */
    private static void breakAnchors() {
        onServer(sp -> {
            List<com.mundosdeaxelis.entity.AnclaMagma> as = sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.AnclaMagma.class,
                    sp.getBoundingBox().inflate(80.0));
            log("anclas rotas: " + as.size());
            as.forEach(net.minecraft.world.entity.Entity::discard);
        });
    }

    /** Le quita vida al corazón (expuesto) con un golpe de jugador para que cambie de fase o muera. */
    private static void hitHeart(float leaveFraction) {
        heart((sp, h) -> {
            h.setHealth(Math.max(1.0F, h.getMaxHealth() * leaveFraction));
            boolean ok = h.hurt(sp.damageSources().playerAttack(sp), leaveFraction <= 0 ? h.getMaxHealth() : 2.0F);
            log("golpe al corazón (estado " + h.state() + "): " + (ok ? "entró" : "no entró") + ", vida " + (int) h.getHealth());
        });
    }

    /** El corazón como lo ve el cliente. */
    private static com.mundosdeaxelis.entity.CorazonMagma clientHeart() {
        for (var en : Minecraft.getInstance().level.entitiesForRendering()) {
            if (en instanceof com.mundosdeaxelis.entity.CorazonMagma h && h.isAlive()) {
                return h;
            }
        }
        return null;
    }

    private static int clientCount(Class<?> type) {
        int n = 0;
        for (var en : Minecraft.getInstance().level.entitiesForRendering()) {
            if (type.isInstance(en)) {
                n++;
            }
        }
        return n;
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
            cmd("item replace entity @a hotbar.7 with mundos_de_axelis:comp_frost_pup");
            cmd("item replace entity @a hotbar.8 with mundos_de_axelis:comp_little_archmage");
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
        // 5a. enemigos del volcán (quietos): salamandra de lava, diablillo y cultista
        add("enemigos del volcán", 2500, () -> {
            clearMobs();
            cmd("summon mundos_de_axelis:salamandra_lava -2.5 -60 9 {NoAI:1b,Rotation:[200f,0f]}");
            cmd("summon mundos_de_axelis:diablillo 0.5 -58.6 10 {NoAI:1b,Rotation:[180f,0f]}");
            cmd("summon mundos_de_axelis:cultista_volcan 3.5 -60 10 {NoAI:1b,Rotation:[160f,0f]}");
        });
        photo("8d_enemigos_volcan", 2500, () -> lookFrom(0.5, 4.0, 0.5, GROUND + 1.0, 9.5));
        add("contar enemigos del volcán", 300, () -> {
            int n = clientCount(com.mundosdeaxelis.entity.SalamandraLava.class) + clientCount(com.mundosdeaxelis.entity.Diablillo.class)
                    + clientCount(com.mundosdeaxelis.entity.CultistaVolcan.class);
            check("enemigos del volcán a la vista en el cliente: " + n + " de 3", n == 3);
        });
        // 5b. mascotas de los jefes (Lobito Ártico y Archimaguito) invocadas a tu lado
        add("mascotas", 3000, () -> {
            clearMobs();
            cmd("tp @a 0.5 -60 0.5 0 18");
            Minecraft mc = Minecraft.getInstance();
            IntegratedServer srv = mc.getSingleplayerServer();
            java.util.UUID u = mc.player.getUUID();
            srv.execute(() -> {
                net.minecraft.server.level.ServerPlayer sp = srv.getPlayerList().getPlayer(u);
                if (sp != null) {
                    com.mundosdeaxelis.relic.Companions.summon(sp, com.mundosdeaxelis.relic.Companions.FROST_PUP, 1.0F);
                    com.mundosdeaxelis.relic.Companions.summon(sp, com.mundosdeaxelis.relic.Companions.LITTLE_ARCHMAGE, 1.0F);
                }
            });
        });
        add("mascotas (foto)", 2500, () -> shot("8b_mascotas"));
        add("mascotas de cerca (foto)", 300, () -> shot("8c_mascotas"));
        // 6. la Piedra de Portal de la Torre, como un jugador: clic derecho, ficha, «Comenzar» y entrar (etapa 5)
        add("piedra de la torre", 1500, () -> {
            clearMobs();
            cmd("setblock 0 -60 -3 mundos_de_axelis:portal_stone_tower");
            cmd("tp @a 0.5 -60 -0.5 180 35");
            cmd("item replace entity @a hotbar.8 with air");
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
        // 7. el volcán (etapa 4): piedra, ficha, entrar, la intro (el techo se agrieta y baja el corazón con cadenas) y la pelea
        add("salir de la torre", 5000, () -> {
            clearMobs();
            open(null);
            cmd("execute in minecraft:overworld run tp @a 0.5 -60 0.5 0 0");
        });
        add("piedra del volcán", 1500, () -> {
            cmd("setblock 0 -60 -3 mundos_de_axelis:portal_stone_volcano");
            cmd("tp @a 0.5 -60 -0.5 180 35");
            Minecraft.getInstance().player.getInventory().selected = 8;
        });
        add("clic derecho (volcán)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(0, -60, -3);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0),
                            net.minecraft.core.Direction.UP, pos, false));
        });
        add("ficha del volcán abierta", 300, () -> check("la piedra del volcán abre la ficha del jefe (pantalla " + screenName() + ")",
                Minecraft.getInstance().screen instanceof BossBriefScreen));
        add("ficha del volcán (foto)", 5500, () -> shot("11_ficha_volcan"));
        add("comenzar (volcán)", 45000, () -> {
            com.mundosdeaxelis.network.ModNetwork.CHANNEL.sendToServer(
                    new com.mundosdeaxelis.network.BossStartPacket(new net.minecraft.core.BlockPos(0, -60, -3)));
            open(null);
        });
        add("en el volcán", 300, () -> {
            String dim = Minecraft.getInstance().level.dimension().location().getPath();
            check("la piedra lleva al volcán (dimensión " + dim + ")", dim.startsWith("boss_volcano"));
            open(null);
        });
        add("antesala (foto)", 300, () -> shot("12_volcan_antesala"));
        add("larva", 2500, () -> {
            cmd("effect give @a minecraft:resistance 900 4 true");
            cmd("effect give @a minecraft:fire_resistance 900 0 true");
            net.minecraft.world.phys.Vec3 e = com.mundosdeaxelis.world.FireArena.entryPoint(0);
            standLooking(e.x, e.y, e.z, e.x + 1.0, e.y + 0.3, e.z - 3.0);
            onServer(sp -> {
                com.mundosdeaxelis.entity.LarvaMagma l = com.mundosdeaxelis.registry.ModEntities.LARVA_MAGMA.get().create(sp.serverLevel());
                if (l != null) {
                    l.moveTo(e.x + 1.0, e.y, e.z - 3.0, 30.0F, 0.0F);
                    l.setNoAi(true);
                    sp.serverLevel().addFreshEntity(l);
                }
            });
        });
        add("larva (foto)", 300, () -> {
            check("larva de magma a la vista", clientCount(com.mundosdeaxelis.entity.LarvaMagma.class) > 0);
            shot("13_larva");
        });
        add("enemigos vivos", 2800, () -> {
            onServer(sp -> sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.LarvaMagma.class, sp.getBoundingBox().inflate(30))
                    .forEach(net.minecraft.world.entity.Entity::discard));
            net.minecraft.world.phys.Vec3 e = com.mundosdeaxelis.world.FireArena.entryPoint(0);
            standLooking(e.x, e.y, e.z, e.x, e.y + 1.0, e.z - 8.0);
            cmd("execute as @a at @s run summon mundos_de_axelis:cultista_volcan ~2 ~ ~-9");
            cmd("execute as @a at @s run summon mundos_de_axelis:salamandra_lava ~-2 ~ ~-6");
            cmd("execute as @a at @s run summon mundos_de_axelis:diablillo ~ ~3 ~-5");
        });
        add("enemigos atacan (foto)", 2600, () -> shot("13b_enemigos_atacan"));
        add("enemigos atacan b (foto)", 300, () -> shot("13c_enemigos_atacan"));
        add("esperar el escudo", 3200, () -> {
        });
        add("escudo de lava", 400, () -> onServer(sp -> {
            int n = 0;
            net.minecraft.world.entity.LivingEntity first = null;
            for (net.minecraft.world.entity.LivingEntity e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                    sp.getBoundingBox().inflate(30), com.mundosdeaxelis.entity.EscudoLava::isAlly)) {
                if (com.mundosdeaxelis.entity.EscudoLava.has(e)) {
                    n++;
                    if (first == null) {
                        first = e;
                    }
                }
            }
            check("el cultista pone un escudo de lava a un aliado (" + n + " con escudo)", n > 0);
            if (first != null) {
                // la cámara, a 4 bloques del aliado protegido, del lado donde estoy
                net.minecraft.world.phys.Vec3 c = first.position().add(0, first.getBbHeight() * 0.5, 0);
                net.minecraft.world.phys.Vec3 d = sp.position().subtract(first.position()).multiply(1, 0, 1);
                d = d.lengthSqr() < 0.01 ? new net.minecraft.world.phys.Vec3(0, 0, 1) : d.normalize();
                if (first instanceof net.minecraft.world.entity.Mob m) {
                    m.setNoAi(true); // quieto para la foto (luego se quitan todos)
                }
                net.minecraft.world.phys.Vec3 cam = first.position().add(d.scale(4.0));
                float yaw = (float) Math.toDegrees(Math.atan2(-(c.x - cam.x), c.z - cam.z));
                float pitch = (float) Math.toDegrees(-Math.atan2(c.y - (cam.y + 1.62), Math.hypot(c.x - cam.x, c.z - cam.z)));
                sp.teleportTo(sp.serverLevel(), cam.x, cam.y, cam.z, yaw, pitch);
            }
        }));
        add("escudo de lava (foto)", 300, () -> shot("13d_escudo_lava"));
        add("quitar enemigos", 1000, () -> onServer(sp -> {
            for (Class<? extends net.minecraft.world.entity.Entity> k : List.of(com.mundosdeaxelis.entity.SalamandraLava.class,
                    com.mundosdeaxelis.entity.Diablillo.class, com.mundosdeaxelis.entity.CultistaVolcan.class, com.mundosdeaxelis.entity.LarvaMagma.class)) {
                sp.serverLevel().getEntitiesOfClass(k, sp.getBoundingBox().inflate(40)).forEach(net.minecraft.world.entity.Entity::discard);
            }
        }));
        add("a la arena", 2000, () -> {
            onServer(sp -> sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.LarvaMagma.class, sp.getBoundingBox().inflate(30))
                    .forEach(net.minecraft.world.entity.Entity::discard));
            open(null);
            net.minecraft.world.phys.Vec3 h = com.mundosdeaxelis.world.FireArena.heartHome();
            standLooking(0.5, com.mundosdeaxelis.world.FireArena.Y + 1, 16.5, h.x, h.y + 4, h.z);
        });
        add("temblor (foto)", 2600, () -> shot("14_volcan_temblor"));
        add("grieta (foto)", 2400, () -> shot("15_volcan_grieta"));
        add("baja el corazón (foto)", 4500, () -> {
            check("el corazón aparece en la intro", clientHeart() != null);
            shot("16_corazon_baja");
        });
        add("encadenado", 300, () -> {
            net.minecraft.world.phys.Vec3 h = com.mundosdeaxelis.world.FireArena.heartHome();
            standLooking(0.5, com.mundosdeaxelis.world.FireArena.Y + 1, 17.5, h.x, h.y, h.z);
        });
        add("encadenado (foto)", 1200, () -> {
            com.mundosdeaxelis.entity.CorazonMagma h = clientHeart();
            int anchors = clientCount(com.mundosdeaxelis.entity.AnclaMagma.class);
            check("el corazón queda encadenado (estado " + (h == null ? -1 : h.state()) + ", anclas " + anchors + ")",
                    h != null && h.state() == com.mundosdeaxelis.entity.CorazonMagma.S_CHAINED && anchors >= 3);
            shot("17_corazon_cadenas");
        });
        add("retrato corazón", 900, () -> {
            // de cerca, flotando a media altura y sin interfaz: para la carta del Corazón de Magma
            onServer(sp -> sp.setNoGravity(true));
            net.minecraft.world.phys.Vec3 h = com.mundosdeaxelis.world.FireArena.heartHome().add(0, 2.9, 0);
            standLooking(0.5, h.y - 0.8 - 1.62, 8.0, 0.5, h.y, 0.5);
            Minecraft.getInstance().options.hideGui = true;
        });
        add("retrato corazón (foto)", 300, () -> {
            shot("carta_corazon_magma");
            Minecraft.getInstance().options.hideGui = false;
            onServer(sp -> sp.setNoGravity(false));
        });
        add("ancla de cerca", 1500, () -> {
            net.minecraft.world.phys.Vec3 a = com.mundosdeaxelis.world.FireArena.anchor(0);
            net.minecraft.world.phys.Vec3 c = com.mundosdeaxelis.world.FireArena.center();
            net.minecraft.world.phys.Vec3 d = c.subtract(a).normalize();
            standLooking(a.x + d.x * 4.5 + d.z * 1.5, com.mundosdeaxelis.world.FireArena.Y + 1, a.z + d.z * 4.5 - d.x * 1.5, a.x, a.y + 0.8, a.z);
        });
        add("ancla (foto)", 300, () -> shot("18_ancla"));
        add("golpe a la coraza", 1200, () -> heart((sp, h) -> check("con cadenas no recibe daño",
                !h.hurt(sp.damageSources().playerAttack(sp), 10.0F))));
        add("romper anclas", 1800, () -> {
            net.minecraft.world.phys.Vec3 h = com.mundosdeaxelis.world.FireArena.heartHome();
            standLooking(0.5, com.mundosdeaxelis.world.FireArena.Y + 1, 14.5, h.x, com.mundosdeaxelis.world.FireArena.Y + 3, h.z);
            breakAnchors();
        });
        add("cae (foto)", 2500, () -> shot("19_corazon_cae"));
        add("expuesto (foto)", 300, () -> {
            com.mundosdeaxelis.entity.CorazonMagma h = clientHeart();
            check("sin anclas cae y queda expuesto (estado " + (h == null ? -1 : h.state()) + ")",
                    h != null && (h.state() == com.mundosdeaxelis.entity.CorazonMagma.S_EXPOSED || h.state() == com.mundosdeaxelis.entity.CorazonMagma.S_FALL));
            shot("20_corazon_expuesto");
        });
        add("fase 2", 1500, () -> hitHeart(0.62F));
        add("fase 2 (foto)", 5000, () -> {
            com.mundosdeaxelis.entity.CorazonMagma h = clientHeart();
            check("pasa a la fase 2 (fase " + (h == null ? -1 : h.phase()) + ")", h != null && h.phase() == 2);
            shot("21_fase2");
        });
        add("fase 2 ataques (foto)", 4000, () -> shot("22_fase2_ataques"));
        add("fase 2 ataques b (foto)", 4000, () -> shot("23_fase2_ataques"));
        add("anclas otra vez", 4500, () -> {
            check("vuelve a encadenarse con 4 anclas (" + clientCount(com.mundosdeaxelis.entity.AnclaMagma.class) + ")",
                    clientCount(com.mundosdeaxelis.entity.AnclaMagma.class) >= 4);
            breakAnchors();
        });
        add("fase 3", 1500, () -> hitHeart(0.3F));
        add("fase 3 (foto)", 5000, () -> {
            com.mundosdeaxelis.entity.CorazonMagma h = clientHeart();
            check("pasa a la fase 3 (fase " + (h == null ? -1 : h.phase()) + ")", h != null && h.phase() == 3);
            shot("24_fase3");
        });
        add("fase 3 ataques (foto)", 4000, () -> shot("25_fase3_ataques"));
        add("fase 3 ataques b (foto)", 5000, () -> shot("26_fase3_ataques"));
        add("anclas última vez", 4500, () -> breakAnchors());
        add("golpe final", 2500, () -> hitHeart(0.0F));
        add("muere (foto)", 4000, () -> shot("27_corazon_muere"));
        add("victoria (foto)", 300, () -> {
            check("el corazón ya no está", clientHeart() == null);
            shot("28_victoria");
        });
        add("premios (foto)", 3000, () -> shot("29_premios"));
        // 8. la estructura del volcán (/mundos estructura volcan): por fuera, el cráter, tirarse a la lava falsa, la dungeon y una erupción
        add("volver al mundo", 4000, () -> {
            open(null);
            clearMobs();
            cmd("execute in minecraft:overworld run tp @a 0.5 -60 60 180 0");
        });
        add("construir volcán", 30000, () -> {
            open(null);
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned 0 " + VY + " " + (VZ - 16) + " rotated 0 0 run mundos estructura volcan");
        });
        add("volcán de lejos", 4000, () -> lookV(0, 30, -75, 0, 32, 60));
        add("volcán de lejos (foto)", 300, () -> shot("30_volcan_lejos"));
        add("volcán de lado", 4000, () -> lookV(100, 55, 25, 0, 30, 60));
        add("volcán de lado (foto)", 300, () -> shot("31_volcan_lado"));
        add("camino", 3000, () -> lookV(48, 14, 2, 30, 18, 30));
        add("camino (foto)", 300, () -> shot("32_volcan_camino"));
        add("cráter", 3500, () -> lookV(0, 82, 36, 0, 61, 60));
        add("cráter (foto)", 300, () -> shot("33_volcan_crater"));
        add("a tirarse", 600, () -> {
            cmd("gamemode survival @a");
            // sin resistencia: así se comprueba que la caída por la lava falsa no hace daño
            cmd("effect clear @a minecraft:resistance");
            cmd("effect give @a minecraft:fire_resistance 900 0 true");
            cmd("effect give @a minecraft:instant_health 1 5 true");
            lookV(0.5, 64, 60.5, 0.5, 50, 72);
        });
        add("cayendo (foto)", 2500, () -> shot("34_volcan_cayendo"));
        add("aterriza", 9000, () -> {
        });
        add("cámara del cráter", 300, () -> {
            double ly = Minecraft.getInstance().player.getY() - VY;
            check("tirarse a la lava falsa lleva a la cámara del cráter (altura " + String.format(Locale.ROOT, "%.1f", ly) + ")", ly < 40 && ly > 28);
            check("la caída no hace daño (vida " + (int) Minecraft.getInstance().player.getHealth() + ")", Minecraft.getInstance().player.getHealth() >= 19.0F);
            cmd("effect give @a minecraft:resistance 900 4 true");
            lookV(0.5, 32, 51, 0, 35, 64);
        });
        add("cámara (foto)", 1500, () -> shot("35_volcan_camara"));
        add("puente", 1500, () -> lookV(-11.5, 24, 78.5, 4, 24, 78.5));
        add("puente (foto)", 300, () -> shot("36_volcan_puente"));
        add("pisar el puente", 700, () -> lookV(-8.5, 24, 78.5, 4, 23, 78.5));
        add("cruje (foto)", 2500, () -> shot("37_puente_cruje"));
        add("se cae", 300, () -> {
            double ly = Minecraft.getInstance().player.getY() - VY;
            check("el puente se derrumba y caes al foso (altura " + String.format(Locale.ROOT, "%.1f", ly) + ")", ly < 23);
            shot("38_puente_foso");
        });
        add("sala de erupciones", 3500, () -> {
            onServer(net.minecraft.world.entity.Entity::clearFire);
            lookV(5.5, 8, 67.5, 0, 9, 80);
        });
        add("erupciones (foto)", 2500, () -> shot("39_volcan_erupciones"));
        add("erupciones b (foto)", 2500, () -> shot("40_volcan_erupciones"));
        add("altar", 3000, () -> lookV(-14.5, 8, 76.5, -24, 9, 76.5));
        add("altar (foto)", 300, () -> shot("41_volcan_altar"));
        add("santuario", 3000, () -> {
            clearMobs();
            lookV(0.5, -8, 82.5, 0.5, -6, 90.5);
        });
        add("santuario (foto)", 300, () -> {
            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(0, VY - 6, VZ + 90);
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level.getBlockState(p).getBlock()).getPath();
            check("la Piedra de Portal: Volcán está en el santuario (" + id + ")", id.equals("portal_stone_volcano"));
            shot("42_volcan_santuario");
        });
        add("salida", 2500, () -> lookV(0.5, 2, 14.5, 0.5, 1, 0));
        add("salida (foto)", 300, () -> shot("43_volcan_salida"));
        add("erupción", 1500, () -> {
            cmd("gamemode spectator @a");
            lookV(70, 40, -20, 0, 60, 60);
            onServer(sp -> com.mundosdeaxelis.block.NucleoVolcanBlock.erupt(sp.serverLevel(),
                    new net.minecraft.core.BlockPos(0, VY + 61, VZ + 60), sp.getRandom()));
        });
        add("erupción (foto)", 2000, () -> shot("44_volcan_erupcion"));
        add("erupción b (foto)", 300, () -> shot("45_volcan_erupcion"));
        // 7. retratos para las cartas: tres "estudios" cerrados (volcán, forja y torre) y cada criatura quieta, de frente, sin interfaz
        add("estudios", 3000, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("forceload add " + (ST_TORRE - 10) + " " + (SZ - 12) + " " + (ST_VOLCAN + 10) + " " + (SZ + 8));
            cmd("execute in minecraft:overworld run tp @a " + ST_FORJA + " -50 " + (SZ - 20));
        });
        add("construir estudios", 2500, () -> {
            studio(ST_VOLCAN, "volcan");
            studio(ST_FORJA, "forja");
            studio(ST_TORRE, "torre");
        });
        portrait("cultista_volcan", ST_VOLCAN, "cultista_volcan", -60.0, -58.85, 2.7, 0.35, 165);
        portrait("salamandra_lava", ST_VOLCAN, "salamandra_lava", -60.0, -59.72, 1.25, 0.32, 212);
        portrait("diablillo", ST_VOLCAN, "diablillo", -59.4, -58.95, 1.05, 0.05, 172);
        portrait("larva_magma", ST_VOLCAN, "larva_magma", -60.0, -59.8, 0.95, 0.25, 205);
        portrait("fundidor", ST_FORJA, "fundidor", -60.0, -58.65, 2.6, 0.25, 170);
        portrait("ascua", ST_FORJA, "ascua", -59.5, -59.0, 1.6, 0.1, 180);
        portrait("esbirro", ST_TORRE, "esbirro", -60.0, -58.95, 2.1, 0.25, 168);
        portrait("armadura_encantada", ST_TORRE, "armadura_encantada", -60.0, -58.95, 2.2, 0.25, 192);
        portrait("libro_embrujado", ST_TORRE, "libro_embrujado", -59.3, -58.75, 0.95, 0.05, 160);
        portrait("archimago", ST_TORRE, "archimago", -60.0, -57.8, 4.6, 0.2, 175);
        add("fin de los retratos", 300, () -> {
            Minecraft.getInstance().options.hideGui = false;
            clearMobs();
            cmd("forceload remove all");
        });
        // 8. el álbum de cartas lleno: jefes (con el Corazón de Magma y el Archimago) y las criaturas nuevas
        add("álbum lleno", 1500, () -> {
            cmd("cartas llenar @p");
            cmd("execute in minecraft:overworld run tp @a 0.5 -60 0.5 0 0");
        });
        add("álbum: jefes", 2500, () -> open(new com.mundosdeaxelis.cartas.client.AlbumScreen()));
        add("álbum: jefes (foto)", 300, () -> {
            check("el álbum tiene " + com.mundosdeaxelis.cartas.Cards.ALL.size() + " cartas (41 esperadas)", com.mundosdeaxelis.cartas.Cards.ALL.size() == 41);
            shot("46_album_jefes");
        });
        add("álbum: criaturas", 1800, () -> Minecraft.getInstance().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
        add("álbum: criaturas (foto)", 300, () -> shot("47_album_criaturas"));
        add("álbum: criaturas nuevas", 1800, () -> Minecraft.getInstance().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
        add("álbum: criaturas nuevas (foto)", 300, () -> shot("48_album_criaturas_nuevas"));
        add("fin", 2000, () -> {
            clearMobs();
            open(null);
            log("AUTOTEST_FIN fotos=" + shots + " comprobaciones=" + checks + " fallos=" + fails);
        });
        add("salir", 1000, () -> Minecraft.getInstance().stop());
    }
}
