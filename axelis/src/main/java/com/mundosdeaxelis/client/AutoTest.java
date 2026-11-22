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

    /** El Coloso de Hierro más cercano que ve el cliente (etapa 267). */
    private static com.mundosdeaxelis.entity.ColosoHierro clientColoso() {
        Minecraft mc = Minecraft.getInstance();
        com.mundosdeaxelis.entity.ColosoHierro best = null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof com.mundosdeaxelis.entity.ColosoHierro c && (best == null || c.distanceTo(mc.player) < best.distanceTo(mc.player))) {
                best = c;
            }
        }
        return best;
    }

    /** Cuántos objetos sueltos de ese tipo ve el cliente (a 40 bloques). */
    private static int clientCountItems(net.minecraft.world.item.Item it) {
        Minecraft mc = Minecraft.getInstance();
        int n = 0;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof net.minecraft.world.entity.item.ItemEntity ie && ie.getItem().is(it) && e.distanceTo(mc.player) < 40) {
                n++;
            }
        }
        return n;
    }

    /** Como {@link #clearMobs()}, pero deja a los habitantes (etapa 263: Ignacio sigue preso en su celda del volcán). */
    private static void clearHostiles() {
        IntegratedServer srv = Minecraft.getInstance().getSingleplayerServer();
        srv.execute(() -> {
            for (net.minecraft.server.level.ServerLevel l : srv.getAllLevels()) {
                List<net.minecraft.world.entity.Entity> gone = new ArrayList<>();
                for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
                    if (e instanceof net.minecraft.world.entity.Mob && !(e instanceof com.mundosdeaxelis.entity.Habitante)) {
                        gone.add(e);
                    }
                }
                gone.forEach(net.minecraft.world.entity.Entity::discard);
            }
        });
    }

    /** Dónde se construye el volcán en la prueba: el plano (x, y, z) queda en el mundo en (x, VY + y, VZ + z). */
    private static final int VY = -52, VZ = 136;
    /** Santuario del Bosque (etapa 256): origen de la prueba (en el mundo plano, lejos del volcán). */
    private static final int SX = 260, SY = -60, SZ0 = 20;
    private static volatile boolean guardBroke;

    private static void lookS(double x, double y, double z, double tx, double ty, double tz) {
        standLooking(SX + x, SY + y, SZ0 + z, SX + tx, SY + ty, SZ0 + tz);
    }

    /** Como {@link #standLooking}, pero en coordenadas del plano del volcán. */
    private static void lookV(double x, double y, double z, double tx, double ty, double tz) {
        standLooking(x, VY + y, VZ + z, tx, VY + ty, VZ + tz);
    }

    private static volatile boolean zombieSeen;
    private static net.minecraft.world.item.ItemStack[] savedPets;

    /** Etapa 254: equipa estas figuritas en las ranuras de mascotas del jugador (las criaturas aparecen solas). */
    private static void equipPets(com.mundosdeaxelis.relic.Companions.Spec... specs) {
        onServer(sp -> {
            com.mundosdeaxelis.relic.Equipment.Data d = com.mundosdeaxelis.relic.Equipment.get(sp);
            for (int i = 0; i < d.pets.length; i++) {
                net.minecraft.world.item.Item it = i < specs.length
                        ? net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(MundosDeAxelis.MODID, specs[i].id()))
                        : null;
                d.pets[i] = it == null ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(it);
            }
            com.mundosdeaxelis.relic.Equipment.sync(sp);
        });
    }

    /** Mascotas-criatura que ve el cliente. */
    private static List<com.mundosdeaxelis.entity.Mascota> pets() {
        List<com.mundosdeaxelis.entity.Mascota> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof com.mundosdeaxelis.entity.Mascota m && m.isAlive()) {
                    out.add(m);
                }
            }
        }
        return out;
    }

    /** Mascotas-criatura del jugador en el servidor. */
    private static List<com.mundosdeaxelis.entity.Mascota> serverPets(net.minecraft.server.level.ServerPlayer sp) {
        List<com.mundosdeaxelis.entity.Mascota> out = new ArrayList<>();
        for (net.minecraft.world.entity.Entity e : sp.serverLevel().getAllEntities()) {
            if (e instanceof com.mundosdeaxelis.entity.Mascota m && m.isAlive() && sp.getUUID().equals(m.getOwnerUUID())) {
                out.add(m);
            }
        }
        out.sort(java.util.Comparator.comparingInt(net.minecraft.world.entity.Entity::getId));
        return out;
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

    // ------------------------------------------------------------------ etapa 257: Ruda, la herbolaria (NPC de la celda)

    /** Donde se planta la casa de Ruda en la prueba (la puerta queda en HZ + 1, mirando al norte). */
    private static final int HX = 230, HZ = -10;
    private static volatile boolean keyDropped;
    private static volatile int essBefore;

    private static com.mundosdeaxelis.entity.Habitante rudaClient() {
        return npcClient("ruda");
    }

    private static com.mundosdeaxelis.entity.Habitante npcClient(String kind) {
        for (var en : Minecraft.getInstance().level.entitiesForRendering()) {
            if (en instanceof com.mundosdeaxelis.entity.Habitante h && kind.equals(h.kindId())) {
                return h;
            }
        }
        return null;
    }

    private static void npcServer(String kind, java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, com.mundosdeaxelis.entity.Habitante> c) {
        onServer(sp -> {
            for (com.mundosdeaxelis.entity.Habitante h : sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.Habitante.class,
                    sp.getBoundingBox().inflate(64), x -> kind.equals(x.kindId()))) {
                c.accept(sp, h);
                return;
            }
        });
    }

    private static void rudaServer(java.util.function.BiConsumer<net.minecraft.server.level.ServerPlayer, com.mundosdeaxelis.entity.Habitante> c) {
        onServer(sp -> {
            for (com.mundosdeaxelis.entity.Habitante h : sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.Habitante.class,
                    sp.getBoundingBox().inflate(64))) {
                c.accept(sp, h);
                return;
            }
        });
    }

    private static NpcScreen npcScreen() {
        return Minecraft.getInstance().screen instanceof NpcScreen s ? s : null;
    }

    private static int invCount(net.minecraft.world.item.Item it) {
        int n = 0;
        for (net.minecraft.world.item.ItemStack st : Minecraft.getInstance().player.getInventory().items) {
            if (st.is(it)) {
                n += st.getCount();
            }
        }
        return n;
    }

    /** Usa el objeto que tengo en la mano sobre un bloque (como el clic derecho de un jugador). */
    private static void useOn(net.minecraft.server.level.ServerPlayer sp, net.minecraft.world.item.ItemStack st, net.minecraft.core.BlockPos p,
                              net.minecraft.core.Direction face) {
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, st);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p), face, p, false);
        sp.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(sp, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
    }

    /** Clic derecho en un bloque (agachado o no), como haría el jugador. */
    private static void useBlock(net.minecraft.server.level.ServerPlayer sp, net.minecraft.core.BlockPos p, boolean sneak) {
        sp.setShiftKeyDown(sneak);
        net.minecraft.world.phys.BlockHitResult hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(p),
                net.minecraft.core.Direction.UP, p, false);
        sp.serverLevel().getBlockState(p).use(sp.serverLevel(), sp, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        sp.setShiftKeyDown(false);
    }

    /** Pone la Semilla de casa en el suelo (delante, mirando al sur) y la hace crecer. */
    private static void plantSeed(net.minecraft.server.level.ServerPlayer sp, String kind, int x, int z) {
        useOn(sp, com.mundosdeaxelis.npc.NpcService.kit(kind), new net.minecraft.core.BlockPos(x, SY - 1, z), net.minecraft.core.Direction.UP);
        useBlock(sp, new net.minecraft.core.BlockPos(x, SY, z), false);
    }

    private static void ruda() {
        net.minecraft.core.BlockPos door = new net.minecraft.core.BlockPos(SX, SY, SZ0 + 28); // etapa 262: la celda está al fondo
        add("ruda presa", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("kill @e[type=mundos_de_axelis:brote]");
            cmd("kill @e[type=mundos_de_axelis:espinoso]");
            lookS(0.5, 0, 24.5, 0.5, 1.4, 30.5);
        });
        add("ruda presa (hablar)", 2600, () -> {
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("Ruda está presa en la celda", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.PRESO);
            rudaServer(com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("ruda presa (foto)", 300, () -> {
            check("se abre la ventana de diálogo de Ruda", npcScreen() != null);
            shot("50_ruda_celda_dialogo");
            open(null);
        });
        add("llave", 1200, () -> {
            cmd("kill @e[type=mundos_de_axelis:raizal]");
            onServer(sp -> keyDropped = !sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(48),
                    i -> i.getItem().is(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get())).isEmpty());
        });
        add("llave en la puerta", 2600, () -> {
            check("el Raizal que vigila la celda suelta la llave", keyDropped);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()), door,
                    net.minecraft.core.Direction.NORTH));
        });
        add("ruda rescatada (foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ds = Minecraft.getInstance().level.getBlockState(door);
            check("con la llave se abre la puerta de la celda", ds.getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                    && ds.getValue(net.minecraft.world.level.block.DoorBlock.OPEN));
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("Ruda queda libre", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            check("Ruda te da la Semilla de casa", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            check("sale la ventana de Ruda al rescatarla", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            shot("51_ruda_rescatada");
            open(null);
        });
        add("ruda sale de la celda", 5000, () -> lookS(-3.5, 1, 21.5, 0.5, 0.8, 27));
        add("ruda sale de la celda (foto)", 300, () -> {
            rudaServer((sp, h) -> log("ruda servidor: en " + h.blockPosition().toShortString() + ", su sitio " + (h.home() == null ? "?" : h.home().toShortString())
                    + ", camino " + (h.getNavigation().getPath() == null ? "ninguno" : h.getNavigation().getPath().getNodeCount() + " pasos, llega=" + h.getNavigation().getPath().canReach())));
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("Ruda salió de la celda (z " + (h == null ? "?" : String.format("%.1f", h.getZ())) + ")", h != null && h.getZ() < SZ0 + 28);
            shot("52_ruda_libre");
        });
        add("casa", 2500, () -> {
            cmd("execute in minecraft:overworld run tp @a " + (HX + 0.5) + " " + SY + " " + (HZ - 2.5) + " 0 45");
        });
        // etapa 260: la semilla se pone como bloque y sale el holograma de la casa (con lo que estorba en rojo: un tronco en la pared de la ventana)
        net.minecraft.core.BlockPos seedPos = new net.minecraft.core.BlockPos(HX, SY, HZ);
        add("semilla (poner)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), HX + 0.5, SY, HZ - 2.5, 0.0F, 45.0F);
            net.minecraft.world.item.ItemStack kit = com.mundosdeaxelis.npc.NpcService.kit("ruda");
            useOn(sp, kit, new net.minecraft.core.BlockPos(HX, SY - 1, HZ), net.minecraft.core.Direction.UP);
        }));
        add("holograma (mirar)", 2000, () -> {
            cmd("gamemode spectator @a"); // en el aire sin caerse, para la foto
            standLooking(HX + 9.5, SY + 5, HZ - 7.5, HX - 0.5, SY + 2, HZ + 4);
        });
        add("holograma (foto)", 300, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("la semilla queda puesta en el suelo como bloque", lv.getBlockState(seedPos).is(com.mundosdeaxelis.registry.ModBlocks.SEMILLA_CASA.get()));
            check("la semilla sabe que es la casa de Ruda", lv.getBlockEntity(seedPos) instanceof com.mundosdeaxelis.npc.SemillaCasaBlockEntity be
                    && "ruda".equals(be.kind()));
            check("al ponerla se gastó de la mano", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) == 0);
            check("el holograma no marca nada en rojo en un sitio libre", lv.getBlockEntity(seedPos) instanceof com.mundosdeaxelis.npc.SemillaCasaBlockEntity be
                    && be.scanTick != Long.MIN_VALUE && be.scanBlocked.isEmpty());
            shot("52b_holograma_ruda");
        });
        add("estorbo", 1500, () -> cmd("execute in minecraft:overworld run fill " + (HX - 3) + " " + SY + " " + (HZ + 5) + " " + (HX - 3) + " " + (SY + 2)
                + " " + (HZ + 5) + " minecraft:oak_log"));
        add("holograma con estorbo (foto)", 300, () -> {
            int n = Minecraft.getInstance().level.getBlockEntity(seedPos) instanceof com.mundosdeaxelis.npc.SemillaCasaBlockEntity be ? be.scanBlocked.size() : -1;
            check("el holograma marca en rojo el tronco que estorba (" + n + " bloques)", n == 3);
            shot("52c_holograma_estorbo");
            cmd("gamemode survival @a");
            onServer(sp -> {
                sp.teleportTo(sp.serverLevel(), HX + 0.5, SY, HZ - 2.5, 0.0F, 45.0F);
                useBlock(sp, seedPos, false);
            });
        });
        add("no crece con estorbo", 1200, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("con algo en medio la casa no crece y la semilla sigue en su sitio", lv.getBlockState(seedPos).is(com.mundosdeaxelis.registry.ModBlocks.SEMILLA_CASA.get())
                    && !lv.getBlockState(seedPos.south()).is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR));
            cmd("execute in minecraft:overworld run fill " + (HX - 3) + " " + SY + " " + (HZ + 5) + " " + (HX - 3) + " " + (SY + 2) + " " + (HZ + 5) + " minecraft:air");
            onServer(sp -> useBlock(sp, seedPos, true)); // agachado: gira 90°
        });
        add("holograma girado", 1500, () -> {
            cmd("gamemode spectator @a");
            standLooking(HX + 8.5, SY + 7, HZ + 9.5, HX - 4.5, SY + 1, HZ + 0.5);
        });
        add("holograma girado (foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState st = Minecraft.getInstance().level.getBlockState(seedPos);
            check("agachado y con clic derecho la casa gira (" + (st.hasProperty(com.mundosdeaxelis.npc.SemillaCasaBlock.FACING)
                    ? st.getValue(com.mundosdeaxelis.npc.SemillaCasaBlock.FACING) : "?") + ")",
                    st.hasProperty(com.mundosdeaxelis.npc.SemillaCasaBlock.FACING)
                            && st.getValue(com.mundosdeaxelis.npc.SemillaCasaBlock.FACING) == net.minecraft.core.Direction.WEST);
            shot("52d_holograma_girado");
            onServer(sp -> {
                for (int i = 0; i < 3; i++) {
                    useBlock(sp, seedPos, true); // otras tres vueltas: mira otra vez al sur
                }
            });
            cmd("gamemode survival @a");
        });
        add("casa (plantar)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), HX + 0.5, SY, HZ - 2.5, 0.0F, 45.0F);
            useBlock(sp, seedPos, false);
        }));
        add("casa (comprobar)", 300, () -> {
            check("la semilla se convierte en la casa (ya no está)", !Minecraft.getInstance().level.getBlockState(seedPos)
                    .is(com.mundosdeaxelis.registry.ModBlocks.SEMILLA_CASA.get()));
            net.minecraft.world.level.block.state.BlockState ds = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(HX, SY, HZ + 1));
            check("crece la casa de Ruda (puerta: " + ds.getBlock() + ")", ds.is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR));
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("Ruda vive en su casa", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.VECINO
                    && Math.abs(h.getX() - HX) < 8 && Math.abs(h.getZ() - (HZ + 4)) < 8);
            check("la semilla se gastó", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) == 0);
        });
        add("casa de fuera", 2500, () -> standLooking(HX + 7.5, SY + 3, HZ - 7.5, HX, SY + 2.5, HZ + 4));
        add("casa de fuera (foto)", 300, () -> shot("53_casa_ruda"));
        add("casa por dentro", 2500, () -> standLooking(HX + 0.5, SY, HZ + 2.3, HX - 0.5, SY + 1.2, HZ + 7));
        add("casa por dentro (foto)", 300, () -> shot("54_casa_ruda_dentro"));
        add("ruda vecina", 2600, () -> {
            standLooking(HX + 0.5, SY, HZ + 2.3, HX + 0.5, SY + 1.4, HZ + 4.5);
            onServer(sp -> {
                com.mundosdeaxelis.table.Essence.add(sp, 500);
                com.mundosdeaxelis.table.Essence.sync(sp);
            });
            rudaServer(com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("ruda vecina (foto)", 300, () -> {
            check("ventana de Ruda vecina", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            shot("55_ruda_vecina");
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda (foto)", 800, () -> {
            check("se abre la tienda de Ruda", npcScreen() != null && npcScreen().inShop());
            shot("56_ruda_tienda");
            essBefore = TableClient.essence;
            if (npcScreen() != null) {
                npcScreen().buy(offer("berries")); // 3 bayas del santuario
            }
        });
        add("tienda (comprar brújula)", 1500, () -> {
            check("la tienda cobra Esencia (" + essBefore + " -> " + TableClient.essence + ") y da las bayas",
                    TableClient.essence == essBefore - lastPrice && invCount(com.mundosdeaxelis.registry.ModItems.SANCTUARY_BERRY.get()) >= 3);
            if (npcScreen() != null) {
                // etapa 285: Ruda solo vende el camino al siguiente habitante (Hilda, en la Gran Fundición)
                check("Ruda no vende el templo del bosque (eso lo vende Hilda)", npcScreen().ofertaIdx("compass_bosque") < 0);
                npcScreen().buy(offer("compass_forja"));
            }
        });
        add("pistas", 3000, () -> {
            boolean ok = false;
            for (net.minecraft.world.item.ItemStack st : Minecraft.getInstance().player.getInventory().items) {
                if (st.is(net.minecraft.world.item.Items.COMPASS) && st.hasTag() && st.getTag().contains("LodestonePos")) {
                    ok = true;
                    log("brújula -> " + st.getTag().get("LodestonePos") + " " + st.getHoverName().getString());
                }
            }
            check("la brújula de Ruda apunta a la Gran Fundición (el siguiente habitante)", ok);
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_BACK);
                npcScreen().press(NpcScreen.B_HINT);
            }
        });
        add("pistas (foto)", 300, () -> {
            String t = npcScreen() == null ? "" : npcScreen().lineText();
            log("pista: " + t);
            check("Ruda da una pista", !t.isEmpty());
            shot("57_ruda_pistas");
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_MOVE);
                npcScreen().press(NpcScreen.B_MOVE);
            }
        });
        add("mudarse (comprobar)", 2000, () -> {
            net.minecraft.world.level.block.state.BlockState ds = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(HX, SY, HZ + 1));
            check("al mudarse, la casa se desarma", ds.isAir());
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("al mudarse, Ruda queda libre con otra semilla", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE
                    && invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            open(null);
        });
    }

    // ------------------------------------------------------------------ etapa 258: Fortín de Escarcha y Bruno, el cartógrafo

    /** El fortín se construye con su origen en (FX, SY, FZ): la sala va de FZ + 24 a FZ + 43, la celda al este (x FX + 9..14). */
    private static final int FX = 360, FZ = 20;
    /** Casa de Bruno (la puerta queda en BZ + 1). */
    private static final int BX = 200, BZ = -10;
    private static volatile boolean fortKey;

    private static void fortin() {
        net.minecraft.core.BlockPos door = new net.minecraft.core.BlockPos(FX + 9, SY, FZ + 32);
        add("fortín", 9000, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned " + FX + " " + SY + " " + (FZ - 16) + " rotated 0 0 run mundos estructura fortin");
        });
        add("fortín (fuera)", 2500, () -> standLooking(FX + 0.5, SY + 9, FZ - 26, FX, SY + 3, FZ + 26));
        add("fortín (fuera, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(FX, SY + 1, FZ + 40));
            check("en el estrado del fortín está la mesa de mapas de Bruno, no la Piedra de Portal (" + ps.getBlock() + ")",
                    ps.is(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE));
            shot("58_fortin_fuera");
        });
        add("fortín (patio)", 2500, () -> standLooking(FX + 0.5, SY, FZ - 5, FX + 0.5, SY + 3, FZ + 24));
        add("fortín (patio, foto)", 300, () -> shot("59_fortin_patio"));
        add("fortín (sala)", 2500, () -> standLooking(FX + 0.5, SY, FZ + 26.5, FX + 0.5, SY + 1.2, FZ + 40));
        add("fortín (sala, foto)", 300, () -> {
            int n = clientCount(com.mundosdeaxelis.entity.CentinelaEscarcha.class);
            check("hay centinelas de escarcha en el fortín (" + n + ")", n >= 2);
            check("hay lobeznos de escarcha en el fortín", clientCount(com.mundosdeaxelis.entity.LoboEscarcha.class) >= 2);
            shot("60_fortin_sala");
        });
        // etapa 269: sin haber vencido al jefe anterior, el fortín (tier 3) está sellado: lentitud y el cofre no se abre
        net.minecraft.core.BlockPos sealedChest = new net.minecraft.core.BlockPos(FX - 14, SY, FZ + 31);
        add("sellado (olvidar el progreso)", 3000, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("kill @e[type=mundos_de_axelis:lobo_escarcha]");
            cmd("execute as @a run mundos prueba olvidar_progreso");
            Minecraft.getInstance().player.getInventory().selected = 8;
            cmd("item replace entity @a hotbar.8 with air");
            standLooking(FX - 12.5, SY, FZ + 31.5, FX - 14 + 0.5, SY + 0.5, FZ + 31.5);
        });
        add("sellado (cofre)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            check("dentro de una estructura que aún no te toca, un poder antiguo te frena (lentitud)",
                    mc.player.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN));
            useBlock(sealedChest);
        });
        add("sellado (comprobar)", 300, () -> {
            check("el cofre de escarcha está sellado si no venciste al jefe anterior (pantalla " + screenName() + ")",
                    !(Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>));
            shot("59b_fortin_sellado");
        });
        add("sellado (recuperar el progreso)", 1500, () -> {
            open(null);
            cmd("mundos desbloquear 5");
            cmd("effect clear @a minecraft:slowness");
        });
        add("bruno preso", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("kill @e[type=mundos_de_axelis:lobo_escarcha]");
            standLooking(FX + 6.5, SY, FZ + 29.5, FX + 12.5, SY + 1.4, FZ + 30.5);
        });
        add("bruno preso (hablar)", 2600, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("bruno");
            check("Bruno está preso en la celda del fortín", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.PRESO);
            npcServer("bruno", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("bruno preso (foto)", 300, () -> {
            check("se abre la ventana de Bruno", npcScreen() != null);
            shot("61_bruno_preso");
            open(null);
        });
        add("llave del centinela", 1200, () -> {
            cmd("kill @e[type=mundos_de_axelis:centinela_escarcha,nbt={ForgeData:{MundosTag:\"llave_celda\"}}]");
            onServer(sp -> fortKey = !sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(48),
                    i -> i.getItem().is(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get())).isEmpty());
        });
        add("llave en la celda del fortín", 2600, () -> {
            check("el centinela del estrado suelta la llave", fortKey);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()), door,
                    net.minecraft.core.Direction.WEST));
        });
        add("bruno rescatado (foto)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("bruno");
            check("Bruno queda libre", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            check("Bruno da su Semilla de casa", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            shot("62_bruno_rescatado");
            open(null);
        });
        add("bruno sale (foto)", 5000, () -> {
        });
        add("bruno sale (comprobar)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("bruno");
            check("Bruno salió de la celda", h != null && h.getX() < FX + 9);
            cmd("kill @e[type=mundos_de_axelis:centinela_escarcha]");
        });
        add("dos casas", 2500, () -> {
            // Ruda vuelve a tener casa (se había mudado) y se planta la de Bruno con su semilla
            cmd("execute in minecraft:overworld positioned " + HX + " " + SY + " " + HZ + " rotated 0 0 run mundos vecinos plantar ruda");
            cmd("execute in minecraft:overworld run tp @a " + (BX + 0.5) + " " + SY + " " + (BZ - 2.5) + " 0 45");
        });
        add("casa de bruno (plantar)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), BX + 0.5, SY, BZ - 2.5, 0.0F, 45.0F);
            plantSeed(sp, "bruno", BX, BZ);
        }));
        add("casa de bruno (comprobar)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ds = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(BX, SY, BZ + 1));
            check("crece la cabaña de Bruno", ds.is(net.minecraft.world.level.block.Blocks.SPRUCE_DOOR)
                    && Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(BX + 2, SY, BZ + 2)).is(net.minecraft.world.level.block.Blocks.CARTOGRAPHY_TABLE));
            com.mundosdeaxelis.entity.Habitante h = npcClient("bruno");
            check("Bruno vive en su cabaña", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.VECINO);
        });
        add("casa de bruno (fuera)", 2500, () -> standLooking(BX + 7.5, SY + 3, BZ - 7.5, BX, SY + 2.5, BZ + 4));
        add("casa de bruno (fuera, foto)", 300, () -> shot("63_casa_bruno"));
        add("casa de bruno (dentro)", 2500, () -> standLooking(BX + 0.5, SY, BZ + 2.3, BX + 0.5, SY + 1.2, BZ + 7));
        add("casa de bruno (dentro, foto)", 300, () -> shot("64_casa_bruno_dentro"));
        add("bruno vecino", 2600, () -> {
            standLooking(BX + 0.5, SY, BZ + 2.3, BX + 0.5, SY + 1.4, BZ + 4.5);
            onServer(sp -> {
                com.mundosdeaxelis.table.Essence.add(sp, 500);
                com.mundosdeaxelis.table.Essence.sync(sp);
            });
            npcServer("bruno", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("bruno vecino (tienda)", 800, () -> {
            check("ventana de Bruno vecino", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda (bajar con la rueda)", 600, () -> {
            if (npcScreen() != null) {
                for (int i = 0; i < 4; i++) {
                    npcScreen().mouseScrolled(0, 0, -1);
                }
            }
        });
        add("tienda con dos vecinos (foto)", 800, () -> {
            shot("65_bruno_tienda");
            essBefore = TableClient.essence;
            if (npcScreen() != null) {
                npcScreen().buy(offer("pack_silver")); // sobre de plata: necesita 2 vecinos con casa
            }
        });
        add("tienda con dos vecinos (comprobar)", 1500, () -> {
            check("con dos vecinos se puede comprar el sobre de plata (" + essBefore + " -> " + TableClient.essence + ")",
                    TableClient.essence == essBefore - lastPrice
                            && invCount(com.mundosdeaxelis.cartas.CartasItems.PACKS.get("plata").get()) >= 1);
            open(null);
            cmd("gamemode spectator @a");
            // Ruda y Bruno se quedan con su casa: con Hilda (la fragua) serán tres vecinos
            cmd("execute in minecraft:overworld positioned " + FX + " " + SY + " " + (FZ - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
    }

    /** Posición de una oferta de la tienda de los habitantes (por su nombre). */
    private static int offer(String id) {
        int i = npcScreen() == null ? -1 : npcScreen().ofertaIdx(id);
        if (i < 0) {
            log("FALLO no existe la oferta " + id);
            fails++;
            return 0;
        }
        lastPrice = npcScreen().precio(id);
        return i;
    }

    /** Precio de la última oferta buscada con {@link #offer} (etapa 285: los precios ya no van escritos en la prueba). */
    private static volatile int lastPrice;

    // ------------------------------------------------------------------ etapa 259: Fragua Olvidada, Hilda y tres vecinos

    /** Fragua: la entrada (z local 0) en {@code GZ}; casa de Hilda en {@code KX, KZ}. */
    private static final int GX = 460, GZ = 20;
    private static final int KX = 170, KZ = -10;
    private static volatile boolean forgeKey;

    /** Gran Fundición (etapa 266): la entrada (z local 0) en {@code FDZ0}; se construye 12 más arriba que el suelo (los hornos bajan). */
    private static final int FDX = -1300, FDY = -48, FDZ0 = 100;

    private static void fragua() {
        net.minecraft.core.BlockPos door = new net.minecraft.core.BlockPos(FDX - 11, FDY - 13, FDZ0 + 76);
        add("fragua (ir)", 2500, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld run tp @a " + (GX + 0.5) + " " + (SY + 10) + " " + (GZ - 20) + " 0 20");
        });
        add("fragua", 9000, () -> cmd("execute in minecraft:overworld positioned " + GX + " " + SY + " " + (GZ - 16) + " rotated 0 0 run mundos estructura fragua"));
        add("fragua (fuera)", 2500, () -> standLooking(GX + 7.5, SY + 8, GZ - 20, GX, SY + 4, GZ + 26));
        add("fragua (fuera, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(GX, SY + 1, GZ + 35));
            check("en el altar de la fragua está el tesoro del Gran Herrero, no la Piedra de Portal (" + ps.getBlock() + ")",
                    ps.is(com.mundosdeaxelis.registry.ModBlocks.VAULT_CHEST_LOCKED_FORGE.get()));
            shot("66_fragua_fuera");
        });
        add("fragua (patio)", 2500, () -> standLooking(GX + 0.5, SY, GZ + 9, GX + 0.5, SY + 3, GZ + 30));
        add("fragua (patio, foto)", 300, () -> {
            check("hay escorias en la fragua", clientCount(com.mundosdeaxelis.entity.Escoria.class) >= 2);
            shot("67_fragua_patio");
        });
        add("fragua (nave)", 2500, () -> standLooking(GX + 0.5, SY, GZ + 23.5, GX + 0.5, SY + 1.2, GZ + 35));
        add("fragua (nave, foto)", 300, () -> {
            check("hay un fundidor en la fragua", clientCount(com.mundosdeaxelis.entity.Fundidor.class) >= 1);
            check("hay un chispero en el balcón", clientCount(com.mundosdeaxelis.entity.Chispero.class) >= 1);
            shot("68_fragua_nave");
        });
        // etapa 267: el Coloso de Hierro duerme delante del altar; al acercarse despierta, carga el golpe y suelta las ondas
        add("coloso dormido (foto)", 300, () -> {
            com.mundosdeaxelis.entity.ColosoHierro c = clientColoso();
            log("DIAG coloso cliente " + (c == null ? "ninguno" : String.format(Locale.ROOT, "%.1f %.1f %.1f estado %d", c.getX() - GX, c.getY() - SY,
                    c.getZ() - GZ, c.state())));
            check("el Coloso de Hierro duerme delante del altar de la fragua", c != null && c.asleep()
                    && Math.abs(c.getX() - (GX + 0.5)) < 2.0 && Math.abs(c.getZ() - (GZ + 30.5)) < 2.0);
            shot("68d_coloso_dormido");
        });
        add("coloso despierta", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("effect give @a minecraft:fire_resistance 600 0 true");
            cmd("kill @e[type=mundos_de_axelis:escoria]");
            cmd("kill @e[type=mundos_de_axelis:chispero]");
            // a 7 bloques del Coloso (donde esté), mirándolo
            com.mundosdeaxelis.entity.ColosoHierro c = clientColoso();
            double cx = c == null ? GX + 0.5 : c.getX(), cz = c == null ? GZ + 30.5 : c.getZ();
            standLooking(cx, SY, cz - 7.0, cx, SY + 2.0, cz);
        });
        add("coloso despierto (foto)", 300, () -> {
            check("al acercarse, el Coloso de Hierro despierta", clientColoso() != null && !clientColoso().asleep());
            shot("68e_coloso_despierto");
            onServer(sp -> {
                for (com.mundosdeaxelis.entity.ColosoHierro c : sp.serverLevel().getEntitiesOfClass(com.mundosdeaxelis.entity.ColosoHierro.class,
                        sp.getBoundingBox().inflate(30))) {
                    c.forceSlam();
                }
            });
        });
        add("coloso carga", 700, () -> {
        });
        add("coloso carga (foto)", 300, () -> {
            check("el Coloso levanta los puños para el golpe de suelo", clientColoso() != null && clientColoso().state() == com.mundosdeaxelis.entity.ColosoHierro.CHARGE);
            shot("68f_coloso_carga");
        });
        add("coloso golpea", 900, () -> {
        });
        add("coloso golpea (foto)", 300, () -> shot("68g_coloso_ondas"));
        add("coloso vencido", 1500, () -> cmd("kill @e[type=mundos_de_axelis:coloso_hierro]"));
        add("coloso vencido (comprobar)", 300, () -> {
            check("al vencer al Coloso suelta hierro", clientCountItems(net.minecraft.world.item.Items.IRON_INGOT) > 0);
            cmd("kill @e[type=item]");
        });
        add("fragua (deshacer)", 2000, () -> {
            cmd("execute in minecraft:overworld positioned " + GX + " " + SY + " " + (GZ - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
        // la Gran Fundición: la Piedra de Portal en la isla de los hornos y la celda de Hilda en la pared del fondo
        add("fundición (ir)", 2500, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld run tp @a " + (FDX + 0.5) + " " + (FDY + 40) + " " + (FDZ0 - 50) + " 0 25");
        });
        add("fundición", 30000, () -> cmd("execute in minecraft:overworld positioned " + FDX + " " + FDY + " " + (FDZ0 - 16)
                + " rotated 0 0 run mundos estructura fundicion"));
        add("fundición (fuera)", 3000, () -> standLooking(FDX + 55.5, FDY + 32, FDZ0 - 30, FDX, FDY + 8, FDZ0 + 40));
        add("fundición (fuera, foto)", 300, () -> {
            check("hay escorias en la fundición", clientCount(com.mundosdeaxelis.entity.Escoria.class) >= 1);
            shot("68b_fundicion_fuera");
        });
        add("fundición (hornos)", 2500, () -> standLooking(FDX + 0.5, FDY - 11, FDZ0 + 50.5, FDX + 0.5, FDY - 9.5, FDZ0 + 59.5));
        add("fundición (hornos, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(FDX, FDY - 10, FDZ0 + 59));
            check("en la isla de los hornos está la Piedra de Portal: Forja (" + ps.getBlock() + ")",
                    ps.is(com.mundosdeaxelis.registry.ModBlocks.PORTAL_STONE_FORGE.get()));
            check("un fundidor guarda la isla", clientCount(com.mundosdeaxelis.entity.Fundidor.class) >= 1);
            shot("68c_fundicion_hornos");
        });
        add("hilda presa", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("effect give @a minecraft:fire_resistance 600 0 true");
            cmd("kill @e[type=mundos_de_axelis:escoria]");
            cmd("kill @e[type=mundos_de_axelis:chispero]");
            standLooking(FDX - 10.5, FDY - 13, FDZ0 + 73.5, FDX - 12.5, FDY - 11.6, FDZ0 + 78.5);
        });
        add("hilda presa (hablar)", 2600, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("hilda");
            check("Hilda está presa en la celda de los hornos de la fundición", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.PRESO);
            npcServer("hilda", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("hilda presa (foto)", 300, () -> {
            check("se abre la ventana de Hilda", npcScreen() != null);
            shot("69_hilda_presa");
            open(null);
        });
        add("llave del fundidor", 1200, () -> {
            cmd("clear @a mundos_de_axelis:llave_celda");
            cmd("kill @e[type=mundos_de_axelis:fundidor,nbt={ForgeData:{MundosTag:\"llave_celda\"}}]");
        });
        add("llave del fundidor (buscar)", 1200, () -> {
            // la llave está en el suelo cerca de la isla (o ya la recogió el jugador)
            onServer(sp -> forgeKey = sp.getInventory().countItem(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()) > 0
                    || !sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(FDX - 24, FDY - 24, FDZ0 + 36, FDX + 24, FDY + 4, FDZ0 + 82),
                    i -> i.getItem().is(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get())).isEmpty());
        });
        add("llave en la celda de la fundición", 2600, () -> {
            check("el fundidor de la isla suelta la llave", forgeKey);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()), door,
                    net.minecraft.core.Direction.NORTH));
        });
        add("hilda rescatada (foto)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("hilda");
            check("Hilda queda libre", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            check("Hilda da su Semilla de casa", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            shot("70_hilda_rescatada");
            open(null);
        });
        add("hilda sale", 5000, () -> {
        });
        add("hilda sale (comprobar)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("hilda");
            check("Hilda salió de la celda", h != null && h.getZ() < FDZ0 + 76);
            cmd("kill @e[type=mundos_de_axelis:fundidor]");
            cmd("execute in minecraft:overworld positioned " + FDX + " " + FDY + " " + (FDZ0 - 16) + " run mundos estructura deshacer");
            cmd("execute in minecraft:overworld run tp @a " + (KX + 0.5) + " " + SY + " " + (KZ - 2.5) + " 0 45");
        });
        add("casa de hilda (plantar)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), KX + 0.5, SY, KZ - 2.5, 0.0F, 45.0F);
            plantSeed(sp, "hilda", KX, KZ);
        }));
        add("casa de hilda (comprobar)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ds = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(KX, SY, KZ + 1));
            check("crece la herrería de Hilda", ds.is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR)
                    && Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(KX + 2, SY, KZ + 5)).is(net.minecraft.world.level.block.Blocks.SMITHING_TABLE));
            com.mundosdeaxelis.entity.Habitante h = npcClient("hilda");
            check("Hilda vive en su herrería", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            cmd("gamemode spectator @a");
        });
        add("casa de hilda (fuera)", 2500, () -> standLooking(KX + 9.5, SY + 3, KZ - 6.5, KX + 1, SY + 2.5, KZ + 4));
        add("casa de hilda (fuera, foto)", 300, () -> shot("71_casa_hilda"));
        add("casa de hilda (dentro)", 2500, () -> standLooking(KX + 0.5, SY, KZ + 2.3, KX + 0.5, SY + 1.2, KZ + 7));
        add("casa de hilda (dentro, foto)", 300, () -> shot("72_casa_hilda_dentro"));
        add("hilda vecina", 2600, () -> {
            cmd("gamemode survival @a");
            standLooking(KX + 0.5, SY, KZ + 2.3, KX + 0.5, SY + 1.4, KZ + 4.5);
            onServer(sp -> {
                com.mundosdeaxelis.table.Essence.add(sp, 600);
                com.mundosdeaxelis.table.Essence.sync(sp);
            });
            npcServer("hilda", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("hilda vecina (tienda)", 800, () -> {
            check("ventana de Hilda vecina", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda (hasta abajo)", 600, () -> {
            if (npcScreen() != null) {
                for (int i = 0; i < 12; i++) {
                    npcScreen().mouseScrolled(0, 0, -1);
                }
            }
        });
        add("tienda con tres vecinos (foto)", 800, () -> {
            shot("73_tienda_tres_vecinos");
            essBefore = TableClient.essence;
            if (npcScreen() != null) {
                npcScreen().buy(offer("pack_gold")); // sobre de oro: necesita 3 vecinos con casa
            }
        });
        add("tienda con tres vecinos (comprobar)", 1500, () -> {
            check("con tres vecinos se puede comprar el sobre de oro (" + essBefore + " -> " + TableClient.essence + ")",
                    TableClient.essence == essBefore - lastPrice
                            && invCount(com.mundosdeaxelis.cartas.CartasItems.PACKS.get("oro").get()) >= 1);
            open(null);
            cmd("gamemode spectator @a");
            // Ruda, Bruno e Hilda se quedan con su casa: con Teo (la torre) serán cuatro
            cmd("kill @e[type=item]");
        });
    }

    // ------------------------------------------------------------------ etapa 264: Ciudadela de cristal (la Piedra de Portal: Hielo)

    /** Ciudadela: la entrada (z local 0) en {@code CDZ0}; se construye 12 más arriba que el suelo plano (las mazmorras bajan 12). */
    private static final int CDX = -700, CDY = -48, CDZ0 = 100;

    private static void ciudadela() {
        add("ciudadela (ir)", 2500, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld run tp @a " + (CDX + 0.5) + " " + (CDY + 30) + " " + (CDZ0 - 40) + " 0 20");
        });
        add("ciudadela", 15000, () -> cmd("execute in minecraft:overworld positioned " + CDX + " " + CDY + " " + (CDZ0 - 16)
                + " rotated 0 0 run mundos estructura ciudadela"));
        add("ciudadela (fuera)", 2500, () -> standLooking(CDX + 52.5, CDY + 34, CDZ0 - 30, CDX, CDY + 8, CDZ0 + 40));
        add("ciudadela (fuera, foto)", 300, () -> {
            check("hay lobos de escarcha en los patios de la ciudadela", clientCount(com.mundosdeaxelis.entity.LoboEscarcha.class) >= 2);
            shot("61b_ciudadela_fuera");
        });
        add("ciudadela (cripta)", 2500, () -> standLooking(CDX + 0.5, CDY - 11, CDZ0 + 70.5, CDX + 0.5, CDY - 8.5, CDZ0 + 79.5));
        add("ciudadela (cripta, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(CDX, CDY - 9, CDZ0 + 79));
            check("al final de la ciudadela, en la cripta, está la Piedra de Portal: Hielo (" + ps.getBlock() + ")",
                    ps.is(com.mundosdeaxelis.registry.ModBlocks.PORTAL_STONE_ICE.get()));
            check("centinelas de escarcha guardan la cripta", clientCount(com.mundosdeaxelis.entity.CentinelaEscarcha.class) >= 1);
            shot("61c_ciudadela_cripta");
        });
        add("ciudadela (deshacer)", 2500, () -> {
            cmd("execute in minecraft:overworld positioned " + CDX + " " + CDY + " " + (CDZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
    }

    // ------------------------------------------------------------------ etapa 262: Gran Templo del Bosque (la Piedra de Portal: Naturaleza)

    /** Templo: la entrada (z local 0) en {@code TPZ0}; se construye 12 más arriba que el suelo plano (la cripta baja 11). */
    private static final int TPX = -400, TPY = -48, TPZ0 = 100;

    private static void templo() {
        add("templo (ir)", 2500, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld run tp @a " + (TPX + 0.5) + " " + (TPY + 30) + " " + (TPZ0 - 40) + " 0 20");
        });
        add("templo", 15000, () -> cmd("execute in minecraft:overworld positioned " + TPX + " " + TPY + " " + (TPZ0 - 16)
                + " rotated 0 0 run mundos estructura templo"));
        add("templo (fuera)", 2500, () -> standLooking(TPX + 48.5, TPY + 42, TPZ0 - 28, TPX, TPY + 12, TPZ0 + 50));
        add("templo (fuera, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(TPX, TPY + 21, TPZ0 + 52));
            check("al final del templo, junto al árbol del mundo, está la Piedra de Portal: Naturaleza (" + ps.getBlock() + ")",
                    ps.is(com.mundosdeaxelis.registry.ModBlocks.PORTAL_STONE_FOREST.get()));
            check("hay enemigos del bosque por el camino del templo", clientCount(com.mundosdeaxelis.entity.Brote.class) >= 2
                    && clientCount(com.mundosdeaxelis.entity.Espinoso.class) >= 2);
            shot("46b_templo_fuera");
        });
        add("templo (entrada)", 2500, () -> standLooking(TPX + 0.5, TPY + 3, TPZ0 - 6, TPX + 0.5, TPY + 12, TPZ0 + 30));
        add("templo (entrada, foto)", 300, () -> shot("46c_templo_entrada"));
        add("templo (altar)", 2500, () -> standLooking(TPX + 0.5, TPY + 20, TPZ0 + 46.5, TPX + 0.5, TPY + 21.5, TPZ0 + 52.5));
        add("templo (altar, foto)", 300, () -> {
            check("el Raizal guarda el altar del templo", clientCount(com.mundosdeaxelis.entity.Raizal.class) >= 1);
            shot("46d_templo_altar");
        });
        add("templo (deshacer)", 2500, () -> {
            cmd("execute in minecraft:overworld positioned " + TPX + " " + TPY + " " + (TPZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
    }

    // ------------------------------------------------------------------ etapa 268: el botín nuevo

    /** Dónde se prueban los cofres nuevos y la forja de llaves (un rincón libre del mundo plano). */
    private static final int LTX = 40, LTY = -60, LTZ = -40;
    private static volatile int vaultStacks, vaultFrags, vaultBooks;
    private static volatile int i0 = -1, e0;

    private static void useBlock(net.minecraft.core.BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0, 0.5, 0),
                        net.minecraft.core.Direction.UP, pos, false));
    }

    /** Los cinco cofres de cada tier, el gran tesoro de la magia abierto y la forja de una llave con cuatro fragmentos en la Mesa. */
    private static void botin() {
        String[] themes = {"vault_chest", "vault_chest_forge", "vault_chest_ice", "vault_chest_volcano", "vault_chest_magic"};
        net.minecraft.core.BlockPos magic = new net.minecraft.core.BlockPos(LTX + 4, LTY, LTZ + 3);
        net.minecraft.core.BlockPos table = new net.minecraft.core.BlockPos(LTX, LTY, LTZ - 6);
        add("botín (ir)", 2500, () -> {
            clearMobs();
            open(null);
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("execute in minecraft:overworld run forceload add " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
            cmd("execute in minecraft:overworld run fill " + (LTX - 8) + " " + LTY + " " + (LTZ - 10) + " " + (LTX + 8) + " " + (LTY + 6) + " " + (LTZ + 8) + " air");
            for (int i = 0; i < themes.length; i++) {
                cmd("execute in minecraft:overworld run setblock " + (LTX - 4 + 2 * i) + " " + LTY + " " + (LTZ + 3) + " mundos_de_axelis:" + themes[i]
                        + "[facing=north]{Kind:\"TREASURE\",Stage:" + (i + 1) + "}");
            }
            cmd("execute in minecraft:overworld run setblock " + (LTX - 1) + " " + LTY + " " + (LTZ + 6) + " mundos_de_axelis:vault_chest_locked_ice[facing=north]");
            cmd("execute in minecraft:overworld run setblock " + (LTX + 1) + " " + LTY + " " + (LTZ + 6) + " mundos_de_axelis:vault_chest_locked_magic[facing=north]");
            cmd("execute in minecraft:overworld run tp @a " + (LTX + 0.5) + " " + LTY + " " + (LTZ - 3.5) + " 0 20");
            cmd("item replace entity @a hotbar.8 with air");
            Minecraft.getInstance().player.getInventory().selected = 8;
        });
        add("botín (cofres de cada tier)", 2000, () -> standLooking(LTX + 0.5, LTY + 1.2, LTZ - 2.5, LTX + 0.5, LTY, LTZ + 4));
        add("botín (cofres, foto)", 300, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("están los cofres de escarcha y arcano (" + lv.getBlockState(new net.minecraft.core.BlockPos(LTX, LTY, LTZ + 3)).getBlock() + ", "
                            + lv.getBlockState(magic).getBlock() + ")",
                    lv.getBlockState(new net.minecraft.core.BlockPos(LTX, LTY, LTZ + 3)).is(com.mundosdeaxelis.registry.ModBlocks.VAULT_CHEST_ICE.get())
                            && lv.getBlockState(magic).is(com.mundosdeaxelis.registry.ModBlocks.VAULT_CHEST_MAGIC.get()));
            shot("90_botin_cofres");
        });
        add("botín (abrir el gran tesoro)", 1500, () -> standLooking(LTX + 4.5, LTY, LTZ + 1.5, LTX + 4.5, LTY + 0.5, LTZ + 3.5));
        add("botín (clic)", 2000, () -> useBlock(magic));
        add("botín (contenido)", 300, () -> {
            Minecraft mc = Minecraft.getInstance();
            int n = 0, f = 0, b = 0, hf = 0;
            if (mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                for (int i = 0; i < 27 && i < cs.getMenu().slots.size(); i++) {
                    net.minecraft.world.item.ItemStack it = cs.getMenu().slots.get(i).getItem();
                    if (!it.isEmpty()) {
                        n++;
                    }
                    if (it.is(com.mundosdeaxelis.registry.ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.MAGIC).get())) {
                        f += it.getCount();
                    }
                    if (it.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
                        b++;
                    }
                    if (it.is(com.mundosdeaxelis.registry.ModItems.HEART_FRAGMENT.get())) {
                        hf++;
                    }
                }
            }
            check("el gran tesoro trae un fragmento de corazón (" + hf + ")", hf >= 1);
            vaultStacks = n;
            vaultFrags = f;
            vaultBooks = b;
            hover(4, 4); // el ratón fuera de los huecos: que no tape el botín con una descripción
            check("el gran tesoro arcano se abre con botín (" + screenName() + ": " + n + " pilas, " + f + " fragmentos de llave de la Magia, "
                    + b + " libros encantados)", n >= 12 && f >= 2 && b >= 1);
            shot("91_botin_gran_tesoro");
        });
        add("corazón (dar)", 1200, () -> {
            open(null);
            cmd("item replace entity @a hotbar.8 with mundos_de_axelis:heart_fragment 4");
            Minecraft.getInstance().player.getInventory().selected = 8;
        });
        for (int i = 0; i < 4; i++) {
            add("corazón (absorber " + (i + 1) + ")", 900, () -> {
                Minecraft mc = Minecraft.getInstance();
                if (i0 < 0) {
                    i0 = Math.round(mc.player.getMaxHealth());
                }
                mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
            });
        }
        add("corazón (comprobar)", 600, () -> {
            float mh = Minecraft.getInstance().player.getMaxHealth();
            check("cuatro fragmentos de corazón dan un corazón más para siempre (vida máxima " + i0 + " → " + mh + ")", mh >= i0 + 2);
            shot("93a_botin_corazon");
        });
        add("runa (dar)", 1000, () -> {
            e0 = TableClient.essence;
            cmd("item replace entity @a hotbar.8 with mundos_de_axelis:essence_rune_5 1");
        });
        add("runa (romper)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        add("runa (comprobar)", 300, () -> check("la Runa de Esencia V se rompe y da Esencia (" + e0 + " → " + TableClient.essence + ")",
                TableClient.essence >= e0 + com.mundosdeaxelis.item.EssenceRuneItem.VALUE[5]));
        add("pergamino (dar)", 1000, () -> cmd("item replace entity @a hotbar.8 with mundos_de_axelis:lore_scroll{Pergamino:\"magia_2\"} 1"));
        add("pergamino (leer)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        add("pergamino (foto)", 300, () -> {
            check("el pergamino de la crónica se abre como un libro (" + screenName() + ")",
                    Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.BookViewScreen);
            shot("93b_botin_pergamino");
        });
        add("mesa (poner)", 1500, () -> {
            open(null);
            cmd("execute in minecraft:overworld run setblock " + table.getX() + " " + table.getY() + " " + table.getZ() + " mundos_de_axelis:artificer_table");
            cmd("clear @a mundos_de_axelis:key_forest");
            cmd("clear @a mundos_de_axelis:gran_yunque");
            cmd("clear @a mundos_de_axelis:mesa_gemas");
            // lo que piden el Gran Yunque (yunque, alto horno y 2 bloques de hierro) y la Mesa de Gemas (amatista, mesa de herrería, lapislázuli)
            cmd("give @a minecraft:anvil 1");
            cmd("give @a minecraft:blast_furnace 1");
            cmd("give @a minecraft:iron_block 2");
            cmd("give @a minecraft:amethyst_shard 8");
            cmd("give @a minecraft:smithing_table 1");
            cmd("give @a minecraft:lapis_block 1");
            cmd("execute as @a run mundos prueba esencia 300");
        });
        add("mesa (abrir)", 1500, () -> {
            standLooking(LTX + 0.5, LTY, LTZ - 3.5, LTX + 0.5, LTY + 0.5, LTZ - 5.5);
        });
        add("mesa (clic)", 1500, () -> useBlock(table));
        add("mesa (pestaña Crear)", 1200, () -> {
            Screen sc = Minecraft.getInstance().screen;
            check("la Mesa del Artífice se abre (" + screenName() + ")", sc instanceof TableScreen);
            try {
                java.lang.reflect.Method m = TableScreen.class.getDeclaredMethod("setTab", int.class);
                m.setAccessible(true);
                m.invoke(sc, 4);
            } catch (Exception ex) {
                log("no se pudo cambiar de pestaña: " + ex);
            }
        });
        add("mesa (foto)", 300, () -> shot("92_botin_mesa_llaves"));
        add("mesa (crear el Gran Yunque)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            int idx = -1;
            List<com.mundosdeaxelis.table.TableRecipes.Recipe> all = com.mundosdeaxelis.table.TableRecipes.all();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id().equals("gran_yunque")) {
                    idx = i;
                }
            }
            check("la Mesa ya no forja llaves: crea el Gran Yunque de Fundición (receta " + idx + ")", idx >= 0
                    && all.stream().noneMatch(r -> r.id().startsWith("key_")));
            if (mc.screen instanceof TableScreen ts && idx >= 0) {
                mc.gameMode.handleInventoryButtonClick(ts.getMenu().containerId, com.mundosdeaxelis.table.TableMenu.B_CRAFT + idx);
            }
        });
        add("mesa (crear la Mesa de Gemas)", 1200, () -> {
            Minecraft mc = Minecraft.getInstance();
            int idx = -1;
            List<com.mundosdeaxelis.table.TableRecipes.Recipe> all = com.mundosdeaxelis.table.TableRecipes.all();
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i).id().equals("mesa_gemas")) {
                    idx = i;
                }
            }
            if (mc.screen instanceof TableScreen ts && idx >= 0) {
                mc.gameMode.handleInventoryButtonClick(ts.getMenu().containerId, com.mundosdeaxelis.table.TableMenu.B_CRAFT + idx);
            }
        });
        add("mesa (yunque creado)", 300, () -> {
            net.minecraft.world.entity.player.Inventory inv = Minecraft.getInstance().player.getInventory();
            check("la Mesa crea el Gran Yunque de Fundición", inv.countItem(com.mundosdeaxelis.registry.ModItems.GRAN_YUNQUE.get()) >= 1);
            check("la Mesa crea la Mesa de Gemas", inv.countItem(com.mundosdeaxelis.registry.ModItems.MESA_GEMAS.get()) >= 1);
            shot("93_botin_mesa_yunque");
            open(null);
            cmd("execute in minecraft:overworld run forceload remove " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
        });
        yunque();
        gemas();
        mimico();
    }

    // ------------------------------------------------------------------ etapa 272: el mímico

    private static volatile int mimicEss0;

    private static net.minecraft.world.entity.Entity nearestMimic() {
        Minecraft mc = Minecraft.getInstance();
        net.minecraft.world.entity.Entity best = null;
        for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
            if (en instanceof com.mundosdeaxelis.entity.Mimico && (best == null || en.distanceToSqr(mc.player) < best.distanceToSqr(mc.player))) {
                best = en;
            }
        }
        return best;
    }

    /**
     * El Mímico: dormido parece un cofre del santuario (con su haz de luz); al intentar abrirlo despierta (tapa con dientes,
     * patitas, ojos) y al matarlo suelta su botín.
     */
    private static void mimico() {
        double mx = LTX - 3.5, mz = LTZ - 0.5;
        add("mimico (poner)", 2000, () -> {
            cmd("execute in minecraft:overworld run forceload add " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
            cmd("kill @e[type=mundos_de_axelis:mimico]");
            cmd("kill @e[type=minecraft:item]");
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:mimico " + mx + " " + LTY + " " + mz + " {Tier:1}");
            standLooking(LTX - 3.5, LTY, LTZ - 3.6, mx, LTY + 0.4, mz);
        });
        add("mimico (dormido, foto)", 400, () -> {
            net.minecraft.world.entity.Entity e = nearestMimic();
            check("hay un mímico dormido que parece un cofre", e instanceof com.mundosdeaxelis.entity.Mimico mm && !mm.isAwake() && mm.tier() == 1);
            check("el mímico dormido mira al jugador, como un cofre bien puesto (yaw " + (e == null ? "-" : e.getYRot()) + ")",
                    e != null && Math.abs(net.minecraft.util.Mth.wrapDegrees(e.getYRot() - 180.0F)) < 1.0F);
            shot("105_mimico_dormido");
        });
        add("mimico (abrirlo)", 500, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.entity.Entity e = nearestMimic();
            if (e != null) {
                mc.gameMode.interact(mc.player, e, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        });
        add("mimico (despierto, foto)", 700, () -> {
            net.minecraft.world.entity.Entity e = nearestMimic();
            check("al intentar abrirlo, el mímico despierta", e instanceof com.mundosdeaxelis.entity.Mimico mm && mm.isAwake());
            shot("106_mimico_despierto");
        });
        add("mimico (morder, foto)", 600, () -> shot("107_mimico_ataca"));
        add("mimico (matarlo)", 1500, () -> {
            mimicEss0 = invCount(com.mundosdeaxelis.registry.ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.VENENO).get());
            cmd("kill @e[type=minecraft:item]");
            cmd("execute as @a run damage @e[type=mundos_de_axelis:mimico,limit=1,sort=nearest] 100000 minecraft:player_attack by @s");
        });
        add("mimico (botin, foto)", 300, () -> {
            Minecraft mc = Minecraft.getInstance();
            int items = 0;
            boolean ess = false;
            for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.item.ItemEntity ie && en.distanceToSqr(mx, LTY, mz) < 36) {
                    items++;
                    ess |= ie.getItem().getItem() instanceof com.mundosdeaxelis.gema.GemaItems.Esencia;
                }
            }
            // la esencia puede estar en el suelo o ya recogida por el jugador, que está al lado
            ess |= invCount(com.mundosdeaxelis.registry.ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.VENENO).get()) > mimicEss0;
            check("al matar al mímico suelta su botín (" + items + " objetos, esencia " + ess + ")", items >= 2 && ess);
            shot("108_mimico_botin");
            cmd("kill @e[type=minecraft:item]");
            cmd("kill @e[type=mundos_de_axelis:mimico]");
        });
        arrojadizo();
    }

    private static volatile int huskId = -1;

    /** Etapa 273: lanzar un Orbe de Escarcha a un husk: el husk recibe daño (y se ve la escarcha). */
    private static void arrojadizo() {
        double hx = LTX - 3.5, hz = LTZ + 0.5;
        add("arrojadizo (preparar)", 1500, () -> {
            cmd("kill @e[type=minecraft:husk]");
            cmd("execute in minecraft:overworld run summon minecraft:husk " + hx + " " + LTY + " " + hz + " {NoAI:1b,PersistenceRequired:1b}");
            cmd("item replace entity @a hotbar.0 with mundos_de_axelis:orbe_escarcha 4");
            Minecraft.getInstance().player.getInventory().selected = 0;
            standLooking(LTX - 3.5, LTY, LTZ - 3.5, hx, LTY + 0.6, hz);
        });
        add("arrojadizo (lanzar)", 900, () -> {
            Minecraft mc = Minecraft.getInstance();
            huskId = -1;
            for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.monster.Husk) {
                    huskId = en.getId();
                }
            }
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        add("arrojadizo (foto)", 300, () -> {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.entity.Entity h = mc.level.getEntity(huskId);
            check("el Orbe de Escarcha daña al husk (" + (h instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() : -1) + ")",
                    h == null || (h instanceof net.minecraft.world.entity.LivingEntity le && le.getHealth() < le.getMaxHealth()));
            check("lanzar gasta un Orbe de Escarcha", mc.player.getInventory().getItem(0).getCount() == 3);
            shot("109_arrojadizo_escarcha");
            cmd("kill @e[type=minecraft:husk]");
            cmd("item replace entity @a hotbar.0 with minecraft:air");
        });
        elite();
    }

    /** Etapa 275: un enemigo de élite se ve con su nombre y apodo de color, y con el doble y medio de vida. */
    private static void elite() {
        double ex = LTX - 3.5, ez = LTZ + 0.5;
        add("elite (poner)", 1800, () -> {
            cmd("kill @e[type=mundos_de_axelis:escoria]");
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:escoria " + ex + " " + LTY + " " + ez
                    + " {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],ForgeData:{mdc_elite:\"ardiente\",mdc_elite_tier:2}}");
            standLooking(LTX - 3.5, LTY, LTZ - 3.0, ex, LTY + 1.4, ez);
        });
        add("elite (foto)", 400, () -> {
            net.minecraft.world.entity.LivingEntity el = null;
            for (net.minecraft.world.entity.Entity en : Minecraft.getInstance().level.entitiesForRendering()) {
                if (en instanceof com.mundosdeaxelis.entity.Escoria es) {
                    el = es;
                }
            }
            String nm = el == null || el.getCustomName() == null ? "-" : el.getCustomName().getString();
            check("el enemigo de élite lleva su apodo a la vista (" + nm + ")", nm.contains("★") && !nm.contains("elite."));
            check("el élite tiene el doble y medio de vida (" + (el == null ? -1 : el.getMaxHealth()) + ")", el != null
                    && el.getMaxHealth() >= el.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH) * 2.4D);
            shot("110_elite_ardiente");
            cmd("kill @e[type=mundos_de_axelis:escoria]");
        });
        saqueador();
    }

    /** Etapa 276: el Saqueador del Tesoro te ve y huye; atrapado, su saco revienta en botín. */
    private static void saqueador() {
        double sx = LTX + 0.5, sz = LTZ + 0.5;
        add("saqueador (de cerca)", 1500, () -> {
            cmd("kill @e[type=mundos_de_axelis:saqueador]");
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:saqueador " + (LTX + 0.5) + " " + LTY + " " + (LTZ - 2.0)
                    + " {Tier:1,NoAI:1b,Rotation:[150f,0f]}");
            standLooking(LTX + 0.5, LTY, LTZ - 4.6, LTX + 0.5, LTY + 0.5, LTZ - 2.0);
        });
        add("saqueador (de cerca, foto)", 300, () -> {
            shot("111a_saqueador_cerca");
            cmd("kill @e[type=mundos_de_axelis:saqueador]");
            cmd("kill @e[type=minecraft:item]");
        });
        add("saqueador (poner)", 1500, () -> {
            cmd("kill @e[type=mundos_de_axelis:saqueador]");
            cmd("kill @e[type=minecraft:item]");
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:saqueador " + sx + " " + LTY + " " + sz + " {Tier:1}");
            standLooking(LTX + 0.5, LTY, LTZ - 4.5, sx, LTY + 0.6, sz);
        });
        add("saqueador (huye, foto)", 600, () -> {
            com.mundosdeaxelis.entity.Saqueador sq = null;
            for (net.minecraft.world.entity.Entity en : Minecraft.getInstance().level.entitiesForRendering()) {
                if (en instanceof com.mundosdeaxelis.entity.Saqueador s2) {
                    sq = s2;
                }
            }
            check("el Saqueador del Tesoro te ve y huye", sq != null && sq.spotted());
            shot("111_saqueador");
        });
        add("saqueador (atraparlo)", 1500, () -> {
            cmd("kill @e[type=minecraft:item]");
            cmd("execute as @a run damage @e[type=mundos_de_axelis:saqueador,limit=1,sort=nearest] 100000 minecraft:player_attack by @s");
        });
        add("saqueador (botin, foto)", 300, () -> {
            int items = 0;
            for (net.minecraft.world.entity.Entity en : Minecraft.getInstance().level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.item.ItemEntity && en.distanceToSqr(sx, LTY, sz) < 400) {
                    items++;
                }
            }
            check("atrapado, el Saqueador suelta su saco de botín (" + items + " objetos)", items >= 6);
            shot("112_saqueador_botin");
            cmd("kill @e[type=minecraft:item]");
        });
        altar();
        armas();
        cuerpoArmas();
        etapa282();
        tablon();
    }

    /** Etapa 285: la tienda nueva de Hilda (pestañas, el camino al siguiente en dorado y el detalle de lo elegido). */
    private static void tiendaNueva() {
        int tx = LTX + 30, tz = LTZ - 30;
        add("tienda nueva (casas)", 4000, () -> {
            cmd("mundos vecinos reiniciar");
            cmd("execute as @a run mundos vecinos semillas"); // todos libres (para poder plantar la casa)
            cmd("execute as @a run mundos prueba esencia 900");
            cmd("execute in minecraft:overworld positioned " + tx + " " + LTY + " " + tz + " rotated 0 0 run mundos vecinos plantar hilda");
            standLooking(tx + 0.5, LTY, tz - 4.5, tx + 0.5, LTY + 1.4, tz + 4.0);
        });
        add("tienda nueva (hablar)", 1500, () -> {
            Minecraft mc = Minecraft.getInstance();
            com.mundosdeaxelis.entity.Habitante h = null;
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                if (e instanceof com.mundosdeaxelis.entity.Habitante x && "hilda".equals(x.kindId())) {
                    h = x;
                }
            }
            check("Hilda vive en su casa de prueba", h != null);
            if (h != null) {
                mc.gameMode.interact(mc.player, h, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        });
        add("tienda nueva (abrir)", 1200, () -> {
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda nueva (destinos)", 800, () -> {
            check("se abre la tienda nueva de Hilda", npcScreen() != null && npcScreen().inShop());
            check("Hilda vende el camino a Bruno y los sitios del bosque, no los suyos",
                    npcScreen() != null && npcScreen().ofertaIdx("compass_fortin") >= 0 && npcScreen().ofertaIdx("compass_bosque") >= 0
                            && npcScreen().ofertaIdx("compass_fragua") < 0);
            shot("180_tienda_destinos");
        });
        add("tienda nueva (provisiones)", 600, () -> {
            if (npcScreen() != null) {
                npcScreen().verTab(com.mundosdeaxelis.npc.NpcShop.PROVISIONES);
            }
        });
        add("tienda nueva (provisiones, foto)", 300, () -> shot("181_tienda_provisiones"));
        add("tienda nueva (cerrar)", 300, () -> {
            open(null);
            cmd("mundos vecinos reiniciar");
        });
    }

    /** Para las fotos de lado: el cuerpo (y la cabeza) del jugador se giran estos grados respecto a la cámara (NaN = normal). */
    public static volatile float ladoFoto = Float.NaN;

    /**
     * Etapa 283: el golpe de todo el cuerpo con cada arma (de lado, preparando y en el impacto), las dos dagas (tercera y primera
     * persona) y la regla de las armas a dos manos (con una antorcha en la otra mano no se golpea).
     */
    private static void cuerpoArmas() {
        String[] mano = {"arma_espada_bosque", "arma_hacha_forja", "arma_martillo_hielo", "arma_lanza_magma", "arma_dagas_magia"};
        com.mundosdeaxelis.arma.TipoArma[] tp = com.mundosdeaxelis.arma.TipoArma.values();
        add("cuerpo (preparar)", 1500, () -> {
            killTier1();
            cmd("kill @e[type=minecraft:item]");
            cmd("clear @a");
            for (int i = 0; i < mano.length; i++) {
                cmd("item replace entity @a hotbar." + i + " with mundos_de_axelis:" + mano[i]);
            }
            Minecraft.getInstance().player.getInventory().selected = 0;
            standLooking(LTX + 0.5, LTY, LTZ + 0.5, LTX + 0.5, LTY + 1.4, LTZ - 8.5);
            Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            ladoFoto = 90.0F;
        });
        for (int i = 0; i < tp.length; i++) {
            final int k = i;
            final float tPrep = clave(tp[k], 1), tGolpe = clave(tp[k], 2);
            add("cuerpo (" + tp[k].id + " preparando)", 800, () -> {
                Minecraft.getInstance().player.getInventory().selected = k;
                ArmaAnim.fijoPrincipal = true;
                ArmaAnim.fijo = tPrep;
            });
            add("cuerpo (" + tp[k].id + " preparando, foto)", 300, () -> shot("14" + k + "_cuerpo_" + tp[k].id + "_prepara"));
            add("cuerpo (" + tp[k].id + " golpe)", 500, () -> ArmaAnim.fijo = tGolpe);
            add("cuerpo (" + tp[k].id + " golpe, foto)", 300, () -> {
                float[] f = com.mundosdeaxelis.client.ArmaCuerpo.cuadro(tp[k], tGolpe);
                check("el golpe de " + tp[k].id + " mueve el cuerpo (giro, inclinación y piernas)",
                        Math.abs(f[ArmaCuerpo.GIRO]) > 0.05F && Math.abs(f[ArmaCuerpo.INCL]) > 3.0F && Math.abs(f[ArmaCuerpo.DELANTE] - f[ArmaCuerpo.DETRAS]) > 0.3F);
                shot("15" + k + "_cuerpo_" + tp[k].id + "_golpe");
            });
        }
        // las dos dagas: de frente, golpeando con la otra mano
        add("cuerpo (dagas de frente)", 600, () -> {
            ladoFoto = Float.NaN;
            Minecraft.getInstance().player.getInventory().selected = 4;
            ArmaAnim.fijoPrincipal = false;
            ArmaAnim.fijo = clave(com.mundosdeaxelis.arma.TipoArma.DAGAS, 2);
        });
        add("cuerpo (dagas de frente, foto)", 300, () -> {
            check("con las dagas y la otra mano libre se ven las dos", com.mundosdeaxelis.client.ArmaCuerpo.dagasDobles(Minecraft.getInstance().player));
            shot("160_dos_dagas_3p");
        });
        add("cuerpo (dagas 1p)", 600, () -> Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
        add("cuerpo (dagas 1p, foto)", 300, () -> shot("161_dos_dagas_1p"));
        add("cuerpo (martillo de frente)", 600, () -> {
            Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            Minecraft.getInstance().player.getInventory().selected = 2;
            ArmaAnim.fijoPrincipal = true;
            ArmaAnim.fijo = clave(com.mundosdeaxelis.arma.TipoArma.MARTILLO, 2);
        });
        add("cuerpo (martillo de frente, foto)", 300, () -> {
            shot("162_martillo_de_frente");
            ArmaAnim.fijo = -1.0F;
            Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        });
        // a dos manos: con una antorcha en la otra mano, la lanza no golpea (y avisa)
        add("dos manos (preparar)", 1200, () -> {
            cmd("summon minecraft:husk " + (LTX + 0.5) + " " + LTY + " " + (LTZ - 1.5) + " {NoAI:1b,Health:400f,Attributes:[{Name:\"generic.max_health\",Base:400}],Tags:[\"arma_foto\"]}");
            cmd("item replace entity @a weapon.offhand with minecraft:torch");
            Minecraft.getInstance().player.getInventory().selected = 3;
            standLooking(LTX + 0.5, LTY, LTZ + 1.0, LTX + 0.5, LTY + 1.0, LTZ - 1.5);
        });
        add("dos manos (golpear)", 700, () -> {
            Minecraft mc = Minecraft.getInstance();
            for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.monster.Husk && en.distanceToSqr(mc.player) < 16) {
                    mc.gameMode.attack(mc.player, en);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    break;
                }
            }
        });
        add("dos manos (foto)", 500, () -> {
            shot("163_dos_manos_aviso");
            onServer(sp -> {
                java.util.List<net.minecraft.world.entity.monster.Husk> hs = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                        sp.getBoundingBox().inflate(6), h -> h.getTags().contains("arma_foto"));
                boolean sano = !hs.isEmpty() && hs.stream().allMatch(h -> h.getHealth() >= h.getMaxHealth());
                check("con algo en la otra mano, la lanza no golpea (" + hs.size() + " muñecos)", sano);
            });
        });
        add("dos manos (sin antorcha)", 600, () -> {
            cmd("item replace entity @a weapon.offhand with minecraft:air");
            Minecraft mc = Minecraft.getInstance();
            mc.player.resetAttackStrengthTicker();
        });
        add("dos manos (golpear sin antorcha)", 1200, () -> {
            Minecraft mc = Minecraft.getInstance();
            for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.monster.Husk && en.distanceToSqr(mc.player) < 16) {
                    mc.gameMode.attack(mc.player, en);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    break;
                }
            }
        });
        add("dos manos (comprobar)", 500, () -> onServer(sp -> {
            java.util.List<net.minecraft.world.entity.monster.Husk> hs = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                    sp.getBoundingBox().inflate(6), h -> h.getTags().contains("arma_foto"));
            check("con la otra mano libre, la lanza sí golpea", !hs.isEmpty() && hs.stream().anyMatch(h -> h.getHealth() < h.getMaxHealth()));
            cmd("kill @e[type=minecraft:husk,tag=arma_foto]");
        }));
    }

    /** El tiempo del fotograma {@code i} del golpe de todo el cuerpo (1 = preparando, 2 = impacto). */
    private static float clave(com.mundosdeaxelis.arma.TipoArma t, int i) {
        return com.mundosdeaxelis.client.ArmaCuerpo.tiempoClave(t, i);
    }

    /** Etapa 282: la ventanita de confirmar la salida del volcán (Confirmar se llena en 3 s) y el estallido en la ficha del arma. */
    private static void etapa282() {
        net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(LTX, LTY, LTZ - 6);
        add("etapa 282 (confirmar salida)", 600, () -> {
            Minecraft.getInstance().setScreen(new ConfirmarScreen("salida_volcan", p, com.mundosdeaxelis.world.Confirmar.ESPERA));
        });
        add("etapa 282 (confirmar: esperando)", 300, () -> {
            check("se abre la ventanita de confirmar la salida", Minecraft.getInstance().screen instanceof ConfirmarScreen);
            shot("170_confirmar_salida_espera");
        });
        add("etapa 282 (confirmar: lista)", 3200, () -> shot("171_confirmar_salida_lista"));
        add("etapa 282 (cerrar)", 300, () -> {
            Minecraft.getInstance().setScreen(null);
            net.minecraft.world.item.ItemStack st = new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.arma(
                    com.mundosdeaxelis.arma.TipoArma.MARTILLO, com.mundosdeaxelis.arma.TierArma.values()[3]));
            java.util.List<net.minecraft.network.chat.Component> tip = st.getTooltipLines(Minecraft.getInstance().player, net.minecraft.world.item.TooltipFlag.NORMAL);
            boolean estallido = false;
            for (net.minecraft.network.chat.Component c : tip) {
                estallido |= c.getString().contains("✸");
            }
            check("la ficha del arma dice su estallido", estallido);
            check("el arrojadizo ya no sale en la pestaña del mod",
                    !net.minecraft.world.item.CreativeModeTabs.allTabs().stream().anyMatch(t -> t.getDisplayItems().stream()
                            .anyMatch(s -> s.getItem() instanceof com.mundosdeaxelis.arrojadizo.ArrojadizoItem)));
        });
    }

    /** Etapa 281: las armas nuevas en la mano (primera y tercera persona), las 40 en un cofre y dos especiales en acción. */
    private static void armas() {
        String[] mano = {"arma_espada_cielo", "arma_hacha_magma", "arma_martillo_hielo", "arma_lanza_dragon", "arma_dagas_abismo"};
        String[] tipos = {"espada", "hacha", "martillo", "lanza", "dagas"};
        String[] tiers = {"bosque", "forja", "hielo", "magma", "magia", "dragon", "cielo", "abismo"};
        net.minecraft.core.BlockPos cofre = new net.minecraft.core.BlockPos(LTX + 1, LTY, LTZ - 9);
        add("armas (preparar)", 1500, () -> {
            killTier1();
            cmd("kill @e[type=minecraft:item]");
            cmd("clear @a");
            for (int i = 0; i < mano.length; i++) {
                cmd("item replace entity @a hotbar." + i + " with mundos_de_axelis:" + mano[i]);
            }
            Minecraft.getInstance().player.getInventory().selected = 0;
            standLooking(LTX + 0.5, LTY, LTZ + 0.5, LTX + 0.5, LTY + 1.4, LTZ - 8.5);
        });
        for (int i = 0; i < mano.length; i++) {
            final int k = i;
            add("armas (" + tipos[k] + " en la mano)", 900, () -> Minecraft.getInstance().player.getInventory().selected = k);
            add("armas (" + tipos[k] + ", foto)", 300, () -> {
                net.minecraft.world.item.ItemStack st = Minecraft.getInstance().player.getMainHandItem();
                check("el arma " + mano[k] + " está en la mano", st.getItem() instanceof com.mundosdeaxelis.arma.ArmaItem a && a.tipo.id.equals(tipos[k]));
                shot("12" + k + "_arma_" + tipos[k]);
            });
        }
        // animación de ataque: cada arma en el momento del golpe (y el martillo también arriba, antes de bajar)
        float[] golpe = {0.40F, 0.50F, 0.58F, 0.42F, 0.30F};
        for (int i = 0; i < mano.length; i++) {
            final int k = i;
            add("armas (" + tipos[k] + " golpeando)", 700, () -> {
                Minecraft.getInstance().player.getInventory().selected = k;
                ArmaAnim.fijo = golpe[k];
            });
            add("armas (" + tipos[k] + " golpeando, foto)", 300, () -> {
                float[] p = ArmaAnim.pose(com.mundosdeaxelis.arma.TipoArma.values()[k], golpe[k]);
                check("la animación de ataque de " + tipos[k] + " mueve el arma", Math.abs(p[5]) + Math.abs(p[2]) > 0.3F);
                shot("13" + k + "_golpe_" + tipos[k]);
            });
        }
        add("armas (martillo arriba)", 700, () -> {
            Minecraft.getInstance().player.getInventory().selected = 2;
            ArmaAnim.fijo = 0.28F;
        });
        add("armas (martillo arriba, foto)", 300, () -> {
            shot("135_golpe_martillo_arriba");
            ArmaAnim.fijo = -1.0F;
        });
        add("armas (tercera persona)", 900, () -> {
            Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
            Minecraft.getInstance().player.getInventory().selected = 2;
        });
        add("armas (martillo, foto 3p)", 300, () -> shot("125_arma_martillo_3p"));
        add("armas (lanza 3p)", 900, () -> Minecraft.getInstance().player.getInventory().selected = 3);
        add("armas (lanza, foto 3p)", 300, () -> {
            shot("126_arma_lanza_3p");
            Minecraft.getInstance().options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
        });
        // las 40 en un cofre doble: la mitad derecha va primero
        add("armas (cofre)", 1500, () -> {
            cmd("execute in minecraft:overworld run setblock " + cofre.getX() + " " + LTY + " " + cofre.getZ() + " minecraft:chest[facing=north,type=left]");
            cmd("execute in minecraft:overworld run setblock " + (cofre.getX() + 1) + " " + LTY + " " + cofre.getZ() + " minecraft:chest[facing=north,type=right]");
            int n = 0;
            for (String t : tiers) {
                for (String tp : tipos) {
                    int bx = n < 27 ? cofre.getX() + 1 : cofre.getX();
                    int slot = n < 27 ? n : n - 27;
                    cmd("execute in minecraft:overworld run item replace block " + bx + " " + LTY + " " + cofre.getZ() + " container." + slot
                            + " with mundos_de_axelis:arma_" + tp + "_" + t);
                    n++;
                }
            }
            standLooking(cofre.getX() + 1.0, LTY, cofre.getZ() + 3.0, cofre.getX() + 1.0, LTY + 0.5, cofre.getZ() + 0.5);
        });
        add("armas (abrir cofre)", 1200, () -> useBlock(cofre));
        add("armas (cofre, foto)", 300, () -> {
            int n = 0;
            if (Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> cs) {
                for (net.minecraft.world.inventory.Slot sl : cs.getMenu().slots) {
                    if (sl.getItem().getItem() instanceof com.mundosdeaxelis.arma.ArmaItem) {
                        n++;
                    }
                }
            }
            check("las 40 armas nuevas en un cofre (" + n + ", " + screenName() + ")", n >= 40);
            shot("127_armas_todas");
            Minecraft.getInstance().player.closeContainer();
            cmd("execute in minecraft:overworld run fill " + cofre.getX() + " " + LTY + " " + cofre.getZ() + " " + (cofre.getX() + 1) + " " + LTY + " " + cofre.getZ() + " air");
        });
        // dos especiales con clic derecho (con almas): el Tajo creciente y el Terremoto
        add("armas (tajo, preparar)", 1500, () -> {
            for (int i = 0; i < 3; i++) {
                cmd("summon minecraft:husk " + (LTX - 1 + i) + " " + LTY + " " + (LTZ - 5) + " {NoAI:1b,Health:400f,Attributes:[{Name:\"generic.max_health\",Base:400}],Tags:[\"arma_foto\"]}");
            }
            onServer(sp -> com.mundosdeaxelis.gear.Souls.set(sp, com.mundosdeaxelis.gear.Souls.max(sp)));
            standLooking(LTX + 0.5, LTY, LTZ + 1.5, LTX + 0.5, LTY + 1.0, LTZ - 5.0);
            Minecraft.getInstance().player.getInventory().selected = 0;
        });
        add("armas (tajo, usar)", 250, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        add("armas (tajo, foto)", 900, () -> {
            shot("128_tajo_creciente");
            onServer(sp -> {
                boolean gasto = com.mundosdeaxelis.gear.Souls.get(sp) < com.mundosdeaxelis.gear.Souls.max(sp);
                java.util.List<net.minecraft.world.entity.monster.Husk> hs = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                        sp.getBoundingBox().inflate(12), h -> h.getTags().contains("arma_foto"));
                long heridos = hs.stream().filter(h -> h.getHealth() < h.getMaxHealth()).count();
                check("el Tajo creciente gasta almas y hiere a los 3 muñecos (" + heridos + "/" + hs.size() + ")", gasto && heridos >= 3);
            });
        });
        add("armas (terremoto, preparar)", 1200, () -> {
            cmd("kill @e[type=minecraft:husk,tag=arma_foto]");
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2;
                cmd(String.format(java.util.Locale.ROOT, "summon minecraft:husk %.2f %d %.2f {NoAI:1b,Health:400f,Attributes:[{Name:\"generic.max_health\",Base:400}],Tags:[\"arma_foto\"]}",
                        LTX + 0.5 + 2.5 * Math.cos(a), LTY, LTZ - 3.5 + 2.5 * Math.sin(a)));
            }
            onServer(sp -> com.mundosdeaxelis.gear.Souls.set(sp, com.mundosdeaxelis.gear.Souls.max(sp)));
            standLooking(LTX + 0.5, LTY, LTZ - 3.5, LTX + 0.5, LTY + 0.5, LTZ - 9.0);
            Minecraft.getInstance().player.getInventory().selected = 2;
        });
        add("armas (terremoto, usar)", 950, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.gameMode.useItem(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND);
        });
        add("armas (terremoto, foto)", 600, () -> {
            shot("129_terremoto");
            onServer(sp -> {
                java.util.List<net.minecraft.world.entity.monster.Husk> hs = sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class,
                        sp.getBoundingBox().inflate(12), h -> h.getTags().contains("arma_foto"));
                long heridos = hs.stream().filter(h -> h.getHealth() < h.getMaxHealth()).count();
                check("el Terremoto hiere a los que tiene alrededor (" + heridos + "/" + hs.size() + ")", heridos >= 3);
            });
        });
        add("armas (recoger)", 300, () -> {
            cmd("kill @e[type=minecraft:husk,tag=arma_foto]");
            cmd("clear @a");
            Minecraft.getInstance().player.getInventory().selected = 8;
        });
    }

    /** Etapa 280: el Tablón de Encargos: se ve con sus 3 encargos y uno cumplido se recoge. */
    private static void tablon() {
        net.minecraft.core.BlockPos tp = new net.minecraft.core.BlockPos(LTX + 2, LTY, LTZ - 3);
        add("tablón (poner)", 1500, () -> {
            cmd("execute in minecraft:overworld run setblock " + tp.getX() + " " + tp.getY() + " " + tp.getZ() + " mundos_de_axelis:tablon_encargos[facing=north]");
            cmd("execute as @a run mundos prueba encargos_listos");
            standLooking(tp.getX() + 0.5, LTY, tp.getZ() - 2.5, tp.getX() + 0.5, LTY + 0.6, tp.getZ() + 0.5);
        });
        add("tablón (foto)", 300, () -> shot("117_tablon"));
        add("tablón (abrir)", 1500, () -> useBlock(tp));
        add("tablón (pantalla, foto)", 300, () -> {
            check("el Tablón de Encargos abre su pantalla (" + screenName() + ")", Minecraft.getInstance().screen instanceof TablonScreen);
            if (Minecraft.getInstance().screen instanceof TablonScreen ts) {
                log("encargos: " + TablonScreen.debugRows(ts.getMenu()));
            }
            shot("118_tablon_encargos");
            e0 = TableClient.essence;
            if (Minecraft.getInstance().screen instanceof TablonScreen ts) {
                ts.claim(0);
            }
        });
        add("tablón (recogido, foto)", 1200, () -> {
            boolean ok = Minecraft.getInstance().screen instanceof TablonScreen ts && ts.getMenu().claimed(0) && !ts.getMenu().claimed(1);
            check("un encargo cumplido se recoge (y da Esencia: " + e0 + " → " + TableClient.essence + ")", ok && TableClient.essence > e0);
            shot("119_tablon_recogido");
            open(null);
            cmd("execute in minecraft:overworld run setblock " + tp.getX() + " " + tp.getY() + " " + tp.getZ() + " minecraft:air");
            cmd("execute in minecraft:overworld run forceload remove " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
        });
    }

    private static void killTier1() {
        for (String k : new String[]{"brote", "espinoso", "raizal", "polinizador"}) {
            cmd("kill @e[type=mundos_de_axelis:" + k + "]");
        }
    }

    /** Etapa 278: el Altar Maldito: despierta, trae oleadas (con barra), y al vencerlas suelta botín y se apaga. */
    private static void altar() {
        // lejos del Gran Yunque (que está en LTX-6, LTZ-6): la cámara mira hacia los cofres por el lado de la Mesa de Gemas
        net.minecraft.core.BlockPos ap = new net.minecraft.core.BlockPos(LTX + 5, LTY, LTZ);
        add("altar (poner)", 1500, () -> {
            killTier1();
            cmd("execute in minecraft:overworld run setblock " + ap.getX() + " " + ap.getY() + " " + ap.getZ() + " mundos_de_axelis:altar_maldito[cargado=true]{Tier:1}");
            standLooking(ap.getX() + 0.5, LTY, ap.getZ() - 4.5, ap.getX() + 0.5, LTY + 0.8, ap.getZ() + 0.5);
        });
        add("altar (foto)", 300, () -> {
            check("el Altar Maldito está puesto y cargado", Minecraft.getInstance().level.getBlockState(ap).is(com.mundosdeaxelis.registry.ModBlocks.ALTAR_MALDITO.get())
                    && Minecraft.getInstance().level.getBlockState(ap).getValue(com.mundosdeaxelis.block.AltarMalditoBlock.CARGADO));
            shot("113_altar_cargado");
        });
        add("altar (despertar)", 4200, () -> useBlock(ap));
        add("altar (oleada 1, foto)", 300, () -> {
            int n = 0;
            for (net.minecraft.world.entity.Entity en : Minecraft.getInstance().level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.Mob && en.distanceToSqr(ap.getX(), ap.getY(), ap.getZ()) < 100
                        && com.mundosdeaxelis.world.Elites.eligibleTier(en) == 1) {
                    n++;
                }
            }
            check("al tocar el altar sale la primera oleada (" + n + " enemigos)", n >= 3);
            shot("114_altar_oleada");
            killTier1();
        });
        add("altar (oleada 2)", 4200, () -> {
        });
        add("altar (oleada 2, matar)", 4200, () -> killTier1());
        add("altar (oleada 3, foto)", 300, () -> {
            shot("115_altar_oleada3");
            killTier1();
        });
        add("altar (vencido)", 2500, () -> {
        });
        add("altar (botin, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState st = Minecraft.getInstance().level.getBlockState(ap);
            int items = 0;
            for (net.minecraft.world.entity.Entity en : Minecraft.getInstance().level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.item.ItemEntity && en.distanceToSqr(ap.getX(), ap.getY(), ap.getZ()) < 64) {
                    items++;
                }
            }
            check("vencidas las 3 oleadas, el altar se apaga y suelta botín (" + items + " objetos)",
                    st.is(com.mundosdeaxelis.registry.ModBlocks.ALTAR_MALDITO.get()) && !st.getValue(com.mundosdeaxelis.block.AltarMalditoBlock.CARGADO) && items >= 2);
            shot("116_altar_botin");
            cmd("kill @e[type=minecraft:item]");
            killTier1();
            cmd("execute in minecraft:overworld run setblock " + ap.getX() + " " + ap.getY() + " " + ap.getZ() + " minecraft:air");
            cmd("execute in minecraft:overworld run forceload remove " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
        });
    }

    // ------------------------------------------------------------------ etapa 271: las gemas

    /** Índice en el menú abierto del primer hueco con ese objeto (o -1). */
    private static int menuSlotOf(net.minecraft.world.item.Item it, int from) {
        if (Minecraft.getInstance().player == null) {
            return -1;
        }
        net.minecraft.world.inventory.AbstractContainerMenu m = Minecraft.getInstance().player.containerMenu;
        for (int i = from; i < m.slots.size(); i++) {
            if (m.slots.get(i).getItem().is(it)) {
                return i;
            }
        }
        return -1;
    }

    private static void menuClick(int slot, net.minecraft.world.inventory.ClickType type) {
        Minecraft mc = Minecraft.getInstance();
        if (slot >= 0 && mc.player != null && mc.gameMode != null) {
            mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, slot, 0, type, mc.player);
        }
    }

    private static void menuButton(int id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.gameMode != null) {
            mc.gameMode.handleInventoryButtonClick(mc.player.containerMenu.containerId, id);
        }
    }

    private static net.minecraft.world.item.ItemStack menuItem(int slot) {
        return Minecraft.getInstance().player.containerMenu.slots.get(slot).getItem();
    }

    private static volatile int pigId = -1;

    /**
     * La Mesa de Gemas: se pone, se abre, se infunde una gema vacía Pulida con Esencia de Fuego (gasta esencia y Esencia), se
     * engarza en una espada, al cerrar todo vuelve al inventario y la espada lleva la gema en su descripción; al golpear a un
     * cerdo, este arde.
     */
    private static void gemas() {
        net.minecraft.core.BlockPos mesa = new net.minecraft.core.BlockPos(LTX + 5, LTY, LTZ - 6);
        net.minecraft.world.item.Item vacia = com.mundosdeaxelis.registry.ModItems.GEMA_VACIA.get();
        net.minecraft.world.item.Item esFuego = com.mundosdeaxelis.registry.ModItems.ESENCIAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO).get();
        net.minecraft.world.item.Item gemFuego = com.mundosdeaxelis.registry.ModItems.GEMAS.get(com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO).get();
        add("gemas (poner)", 1500, () -> {
            cmd("execute in minecraft:overworld run forceload add " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
            cmd("execute in minecraft:overworld run setblock " + mesa.getX() + " " + mesa.getY() + " " + mesa.getZ() + " mundos_de_axelis:mesa_gemas");
            cmd("clear @a minecraft:diamond_sword");
            cmd("clear @a mundos_de_axelis:gema_vacia");
            cmd("clear @a mundos_de_axelis:esencia_fuego");
            cmd("clear @a mundos_de_axelis:gema_fuego");
            cmd("give @a mundos_de_axelis:gema_vacia{Pureza:3} 1");
            cmd("give @a mundos_de_axelis:esencia_fuego 2");
            cmd("give @a minecraft:diamond_sword 1");
            cmd("execute as @a run mundos prueba esencia 300");
            standLooking(LTX + 5.5, LTY, LTZ - 9.5, LTX + 5.5, LTY + 0.6, LTZ - 5.5);
        });
        add("gemas (mesa, foto)", 300, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("la Mesa de Gemas está puesta", lv.getBlockState(mesa).is(com.mundosdeaxelis.registry.ModBlocks.MESA_GEMAS.get()));
            shot("100_mesa_gemas");
        });
        add("gemas (abrir)", 1500, () -> useBlock(mesa));
        add("gemas (meter)", 1200, () -> {
            check("la Mesa de Gemas abre su pantalla (" + screenName() + ")", Minecraft.getInstance().screen instanceof GemTableScreen);
            menuClick(menuSlotOf(vacia, 4), net.minecraft.world.inventory.ClickType.QUICK_MOVE);
            menuClick(menuSlotOf(esFuego, 4), net.minecraft.world.inventory.ClickType.QUICK_MOVE);
            menuClick(menuSlotOf(net.minecraft.world.item.Items.DIAMOND_SWORD, 4), net.minecraft.world.inventory.ClickType.QUICK_MOVE);
        });
        add("gemas (infundir, foto)", 300, () -> {
            check("con mayúsculas y clic, cada cosa va a su hueco (gema vacía, esencia y espada)",
                    menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA).is(vacia) && menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_ESENCIA).is(esFuego)
                            && menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_ARMA).is(net.minecraft.world.item.Items.DIAMOND_SWORD));
            shot("101_gemas_antes");
            e0 = TableClient.essence;
            menuButton(com.mundosdeaxelis.gema.GemTableMenu.B_INFUNDIR);
        });
        add("gemas (infundida)", 1200, () -> {
            net.minecraft.world.item.ItemStack g = menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA);
            check("infundir da una Gema de Fuego Pulida (" + g + ", pureza " + com.mundosdeaxelis.gema.Gemas.pureza(g) + ")",
                    g.is(gemFuego) && com.mundosdeaxelis.gema.Gemas.pureza(g) == 3);
            check("infundir gasta una esencia y " + com.mundosdeaxelis.gema.GemTableMenu.infuseCost(3) + " de Esencia (" + e0 + " → " + TableClient.essence + ")",
                    menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_ESENCIA).getCount() == 1
                            && TableClient.essence == e0 - com.mundosdeaxelis.gema.GemTableMenu.infuseCost(3));
            shot("102_gemas_infundida");
            // la gema pasa al hueco del engarce
            menuClick(com.mundosdeaxelis.gema.GemTableMenu.S_VACIA, net.minecraft.world.inventory.ClickType.PICKUP);
            menuClick(com.mundosdeaxelis.gema.GemTableMenu.S_GEMA, net.minecraft.world.inventory.ClickType.PICKUP);
        });
        add("gemas (engarzar)", 1000, () -> menuButton(com.mundosdeaxelis.gema.GemTableMenu.B_ENGARZAR));
        add("gemas (engarzada, foto)", 1200, () -> {
            net.minecraft.world.item.ItemStack w = menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_ARMA);
            check("engarzar pone la Gema de Fuego en la espada y gasta la gema",
                    com.mundosdeaxelis.gema.Gemas.gemaDe(w) == com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO
                            && menuItem(com.mundosdeaxelis.gema.GemTableMenu.S_GEMA).isEmpty());
            shot("103_gemas_engarzada");
            if (Minecraft.getInstance().screen instanceof GemTableScreen gs) {
                gs.toggleGuide();
            }
        });
        add("gemas (guía, foto)", 1200, () -> {
            check("la Mesa de Gemas tiene una guía con las 8 gemas", Minecraft.getInstance().screen instanceof GemTableScreen gs && gs.guideOpen());
            shot("103b_gemas_guia");
            if (Minecraft.getInstance().screen instanceof GemTableScreen gs) {
                gs.toggleGuide();
            }
            // cerrar como con Esc (avisa al servidor, que devuelve lo que quedó en la mesa)
            Minecraft.getInstance().player.closeContainer();
        });
        add("gemas (devuelve)", 1200, () -> {
            net.minecraft.world.item.ItemStack sword = net.minecraft.world.item.ItemStack.EMPTY;
            for (net.minecraft.world.item.ItemStack st : Minecraft.getInstance().player.getInventory().items) {
                if (st.is(net.minecraft.world.item.Items.DIAMOND_SWORD)) {
                    sword = st;
                }
            }
            check("al cerrar la mesa, la espada (con gema) y la esencia sobrante vuelven al inventario",
                    com.mundosdeaxelis.gema.Gemas.gemaDe(sword) == com.mundosdeaxelis.gema.Gemas.Elemento.FUEGO && invCount(esFuego) == 1);
            boolean tip = false;
            if (!sword.isEmpty()) {
                for (net.minecraft.network.chat.Component c : sword.getTooltipLines(Minecraft.getInstance().player, net.minecraft.world.item.TooltipFlag.Default.NORMAL)) {
                    tip |= c.getString().contains("◆");
                }
            }
            check("la descripción de la espada muestra la gema engarzada", tip);
            cmd("clear @a minecraft:diamond_sword");
            cmd("item replace entity @a hotbar.0 with minecraft:diamond_sword{MdcGema:{E:\"fuego\",P:3}}");
            Minecraft.getInstance().player.getInventory().selected = 0;
            cmd("kill @e[type=minecraft:pig]");
            cmd("execute in minecraft:overworld run summon minecraft:pig " + (LTX + 5.5) + " " + LTY + " " + (LTZ - 2.5) + " {NoAI:1b,Health:40f,Attributes:[{Name:\"generic.max_health\",Base:40d}]}");
            standLooking(LTX + 5.5, LTY, LTZ - 4.4, LTX + 5.5, LTY + 0.5, LTZ - 2.5);
        });
        add("gemas (golpear)", 1200, () -> {
            Minecraft mc = Minecraft.getInstance();
            pigId = -1;
            for (net.minecraft.world.entity.Entity en : mc.level.entitiesForRendering()) {
                if (en instanceof net.minecraft.world.entity.animal.Pig) {
                    pigId = en.getId();
                    mc.gameMode.attack(mc.player, en);
                    mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                    break;
                }
            }
        });
        add("gemas (fuego, foto)", 400, () -> {
            net.minecraft.world.entity.Entity pig = Minecraft.getInstance().level.getEntity(pigId);
            check("al golpear con la espada de Gema de Fuego, el cerdo arde", pig != null && pig.isOnFire());
            shot("104_gemas_fuego");
            cmd("kill @e[type=minecraft:pig]");
            cmd("execute in minecraft:overworld run forceload remove " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
        });
    }

    // ------------------------------------------------------------------ etapa 270: el Gran Yunque de Fundición

    private static Object forjaGame() {
        try {
            java.lang.reflect.Field f = ForjaScreen.class.getDeclaredField("game");
            f.setAccessible(true);
            return Minecraft.getInstance().screen instanceof ForjaScreen fs ? f.get(fs) : null;
        } catch (Exception ex) {
            log("no se pudo leer la forja: " + ex);
            return null;
        }
    }

    private static void forjaStart(int tier) {
        try {
            java.lang.reflect.Method m = ForjaScreen.class.getDeclaredMethod("start", int.class);
            m.setAccessible(true);
            m.invoke(Minecraft.getInstance().screen, tier);
        } catch (Exception ex) {
            log("no se pudo empezar la forja: " + ex);
        }
    }

    private static volatile int keys0, frags0;

    /**
     * El Gran Yunque: se pone con el objeto (dos partes), se abre la forja con 4 fragmentos, fotos de las tres fases y del
     * resultado, una forja perfecta (llave + 1 fragmento de vuelta) y una fallida (se pierde 1 fragmento).
     */
    private static void yunque() {
        net.minecraft.core.BlockPos ground = new net.minecraft.core.BlockPos(LTX - 6, LTY - 1, LTZ - 6);
        net.minecraft.core.BlockPos anvil = ground.above();
        net.minecraft.world.item.Item frag = com.mundosdeaxelis.registry.ModItems.KEY_FRAGMENTS.get(com.mundosdeaxelis.crate.CrateKind.FOREST).get();
        net.minecraft.world.item.Item key = com.mundosdeaxelis.registry.ModItems.KEYS.get(com.mundosdeaxelis.crate.CrateKind.FOREST).get();
        add("yunque (poner)", 1500, () -> {
            cmd("execute in minecraft:overworld run forceload add " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
            cmd("clear @a mundos_de_axelis:key_fragment_forest");
            cmd("clear @a mundos_de_axelis:key_forest");
            cmd("item replace entity @a hotbar.8 with mundos_de_axelis:gran_yunque");
            Minecraft.getInstance().player.getInventory().selected = 8;
            standLooking(LTX - 5.5, LTY, LTZ - 9.5, LTX - 5.5, LTY - 1, LTZ - 5.5);
        });
        add("yunque (colocar)", 1500, () -> useBlock(ground));
        add("yunque (dos partes)", 1200, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            net.minecraft.world.level.block.state.BlockState a = lv.getBlockState(anvil);
            boolean ok = a.getBlock() instanceof com.mundosdeaxelis.block.GranYunqueBlock;
            boolean two = false;
            if (ok) {
                net.minecraft.core.BlockPos other = anvil.relative(com.mundosdeaxelis.block.GranYunqueBlock.forgeSide(a.getValue(com.mundosdeaxelis.block.GranYunqueBlock.FACING)));
                two = lv.getBlockState(other).getBlock() instanceof com.mundosdeaxelis.block.GranYunqueBlock;
            }
            check("el Gran Yunque se pone con sus dos partes: el yunque y el crisol con lava", ok && two);
            standLooking(LTX - 5.5, LTY + 1.2, LTZ - 10.5, LTX - 5.0, LTY + 0.5, LTZ - 5.5);
        });
        add("yunque (foto)", 300, () -> {
            shot("94_yunque");
            cmd("give @a mundos_de_axelis:key_fragment_forest 4");
            cmd("execute as @a run mundos prueba esencia 200");
        });
        add("yunque (abrir)", 1500, () -> {
            keys0 = invCount(key);
            useBlock(anvil);
        });
        add("yunque (elegir, foto)", 300, () -> {
            check("el Gran Yunque abre la forja (" + screenName() + ")", Minecraft.getInstance().screen instanceof ForjaScreen);
            shot("95_forja_elegir");
            forjaStart(1);
        });
        add("forja (fundir)", 1800, () -> {
            Object g = forjaGame();
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.press(); // soplar el fuelle
            }
        });
        add("forja (fundir, foto)", 300, () -> {
            Object g = forjaGame();
            check("la forja empieza fundiendo los fragmentos", g instanceof com.mundosdeaxelis.forja.ForjaGame fg && fg.phase == com.mundosdeaxelis.forja.ForjaGame.Phase.MELT);
            shot("96_forja_fundir");
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.release();
                fg.melt = 1.0;
                fg.phase = com.mundosdeaxelis.forja.ForjaGame.Phase.HAMMER;
            }
        });
        add("forja (martillar)", 1200, () -> {
            Object g = forjaGame();
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.hammerPos = fg.sweet;
                fg.press();
            }
        });
        add("forja (martillar, foto)", 300, () -> {
            Object g = forjaGame();
            check("el primer golpe en el punto dorado es perfecto", g instanceof com.mundosdeaxelis.forja.ForjaGame fg && fg.perfects >= 1);
            shot("97_forja_martillar");
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.phase = com.mundosdeaxelis.forja.ForjaGame.Phase.TEMPER;
            }
        });
        add("forja (templar)", 1500, () -> {
        });
        add("forja (templar, foto)", 300, () -> {
            shot("98_forja_templar");
            Object g = forjaGame();
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.cool = fg.target;
                fg.press();
            }
        });
        add("forja (resultado)", 1500, () -> {
            Object g = forjaGame();
            check("al templar en el rojo cereza la forja termina bien (" + (g instanceof com.mundosdeaxelis.forja.ForjaGame fg ? fg.result : "?") + ")",
                    g instanceof com.mundosdeaxelis.forja.ForjaGame fg && fg.result != com.mundosdeaxelis.forja.ForjaGame.Result.FAIL);
            shot("99_forja_resultado");
        });
        add("forja (llave)", 1200, () -> {
            open(null);
            int k = invCount(key), f = invCount(frag);
            check("el Gran Yunque da la Llave de la Naturaleza (llaves " + keys0 + " → " + k + ", fragmentos que quedan " + f + ")",
                    k == keys0 + 1 && f <= 1);
        });
        // forja fallida: la llave se quiebra y se pierde 1 fragmento
        add("forja fallida (abrir)", 1500, () -> {
            cmd("clear @a mundos_de_axelis:key_fragment_forest");
            cmd("give @a mundos_de_axelis:key_fragment_forest 4");
        });
        add("forja fallida (clic)", 1500, () -> useBlock(anvil));
        add("forja fallida (empezar)", 5600, () -> forjaStart(1));
        add("forja fallida (romper)", 1500, () -> {
            Object g = forjaGame();
            if (g instanceof com.mundosdeaxelis.forja.ForjaGame fg) {
                fg.result = com.mundosdeaxelis.forja.ForjaGame.Result.FAIL;
                fg.phase = com.mundosdeaxelis.forja.ForjaGame.Phase.DONE;
            }
            if (Minecraft.getInstance().screen instanceof ForjaScreen fs) {
                fs.onClose();
            }
        });
        add("forja fallida (comprobar)", 300, () -> {
            int f = invCount(frag);
            check("si la forja falla, la llave se quiebra y se pierde 1 fragmento (quedan " + f + " de 4)", f == 3);
            cmd("execute in minecraft:overworld run forceload remove " + (LTX - 16) + " " + (LTZ - 16) + " " + (LTX + 16) + " " + (LTZ + 16));
        });
    }


    // ------------------------------------------------------------------ etapa 261: Torre Arcana, Teo y cuatro vecinos

    /** Torre: la entrada (z local 0) en {@code TZ0}; casa de Teo en {@code LX, LZ}. */
    private static final int TX = 580, TZ0 = 20;
    private static final int LX = 140, LZ = -10;
    private static volatile boolean towerKey;

    /** Fortaleza del Archimago (etapa 265): la entrada (z local 0) en {@code FTZ0}; se construye 12 más arriba que el suelo (la cripta baja). */
    private static final int FTX = -1000, FTY = -48, FTZ0 = 100;

    private static void torre() {
        int tz = TZ0 + 30; // centro de la torre
        net.minecraft.core.BlockPos door = new net.minecraft.core.BlockPos(FTX + 7, FTY - 5, FTZ0 + 36);
        net.minecraft.core.BlockPos seed = new net.minecraft.core.BlockPos(LX, SY, LZ);
        add("torre (ir)", 2500, () -> {
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld run tp @a " + (TX + 0.5) + " " + (SY + 12) + " " + (TZ0 - 20) + " 0 20");
        });
        add("torre", 9000, () -> cmd("execute in minecraft:overworld positioned " + TX + " " + SY + " " + (TZ0 - 16) + " rotated 0 0 run mundos estructura torre"));
        add("torre (fuera)", 2500, () -> standLooking(TX + 16.5, SY + 10, TZ0 - 16, TX, SY + 16, tz));
        add("torre (fuera, foto)", 300, () -> shot("74_torre_fuera"));
        add("torre (patio)", 2500, () -> standLooking(TX + 0.5, SY + 1, TZ0 + 8, TX + 0.5, SY + 5, tz));
        add("torre (patio, foto)", 300, () -> {
            check("hay esbirros en la torre", clientCount(com.mundosdeaxelis.entity.Esbirro.class) >= 2);
            shot("75_torre_patio");
        });
        add("torre (biblioteca)", 2500, () -> standLooking(TX + 4.5, SY + 8, tz - 4.5, TX - 4, SY + 9, tz + 3));
        add("torre (biblioteca, foto)", 300, () -> {
            check("hay libros embrujados en la biblioteca", clientCount(com.mundosdeaxelis.entity.LibroEmbrujado.class) >= 1);
            shot("76_torre_biblioteca");
        });
        add("torre (observatorio)", 2500, () -> standLooking(TX - 4.5, SY + 24, tz - 4.5, TX, SY + 25.5, tz + 2));
        add("torre (observatorio, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ts = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(TX, SY + 25, tz));
            check("en el observatorio de la Torre Arcana está el tesoro del Archimago, no la Piedra de Portal (" + ts.getBlock() + ")",
                    ts.is(com.mundosdeaxelis.registry.ModBlocks.VAULT_CHEST_LOCKED_MAGIC.get()));
            check("hay armaduras encantadas en la torre", clientCount(com.mundosdeaxelis.entity.ArmaduraEncantada.class) >= 1);
            shot("77_torre_observatorio");
        });
        add("torre (deshacer)", 2500, () -> {
            cmd("execute in minecraft:overworld positioned " + TX + " " + SY + " " + (TZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
        // la Fortaleza del Archimago: la Piedra de Portal arriba del todo y Teo en la cripta
        add("fortaleza (ir)", 2500, () -> {
            clearMobs();
            cmd("execute in minecraft:overworld run tp @a " + (FTX + 0.5) + " " + (FTY + 40) + " " + (FTZ0 - 50) + " 0 25");
        });
        add("fortaleza", 25000, () -> cmd("execute in minecraft:overworld positioned " + FTX + " " + FTY + " " + (FTZ0 - 16)
                + " rotated 0 0 run mundos estructura fortaleza"));
        add("fortaleza (fuera)", 3000, () -> standLooking(FTX + 55.5, FTY + 40, FTZ0 - 30, FTX, FTY + 30, FTZ0 + 34));
        add("fortaleza (fuera, foto)", 300, () -> shot("77b_fortaleza_fuera"));
        add("fortaleza (observatorio)", 2500, () -> standLooking(FTX - 2.5, FTY + 67, FTZ0 + 30.5, FTX, FTY + 67.5, FTZ0 + 38));
        add("fortaleza (observatorio, foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState ps = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(FTX, FTY + 67, FTZ0 + 38));
            check("arriba del todo de la fortaleza está la Piedra de Portal: Torre (" + ps.getBlock() + ")",
                    ps.is(com.mundosdeaxelis.registry.ModBlocks.PORTAL_STONE_TOWER.get()));
            check("la armadura del observatorio guarda la llave", clientCount(com.mundosdeaxelis.entity.ArmaduraEncantada.class) >= 1);
            shot("77c_fortaleza_observatorio");
        });
        add("teo preso", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("kill @e[type=mundos_de_axelis:esbirro]");
            cmd("kill @e[type=mundos_de_axelis:libro_embrujado]");
            cmd("kill @e[type=mundos_de_axelis:armadura_encantada,nbt=!{ForgeData:{MundosTag:\"llave_celda\"}}]");
            standLooking(FTX + 5.5, FTY - 5, FTZ0 + 35.5, FTX + 9.5, FTY - 3.6, FTZ0 + 37.5);
        });
        add("teo preso (hablar)", 2600, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("teo");
            check("Teo está preso en la celda de la cripta de la fortaleza", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.PRESO);
            npcServer("teo", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("teo preso (foto)", 300, () -> {
            check("se abre la ventana de Teo", npcScreen() != null);
            shot("78_teo_preso");
            open(null);
        });
        add("llave de la armadura", 1200, () -> {
            cmd("kill @e[type=mundos_de_axelis:armadura_encantada,nbt={ForgeData:{MundosTag:\"llave_celda\"}}]");
            onServer(sp -> towerKey = !sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                    new net.minecraft.world.phys.AABB(FTX - 12, FTY + 60, FTZ0 + 24, FTX + 12, FTY + 76, FTZ0 + 46),
                    i -> i.getItem().is(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get())).isEmpty());
        });
        add("llave en la celda de la fortaleza", 2600, () -> {
            check("la armadura del observatorio suelta la llave", towerKey);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()), door,
                    net.minecraft.core.Direction.WEST));
        });
        add("teo rescatado (foto)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("teo");
            check("Teo queda libre", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            check("Teo da su Semilla de casa", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            shot("79_teo_rescatado");
            open(null);
        });
        add("teo sale", 5000, () -> {
        });
        add("teo sale (comprobar)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("teo");
            check("Teo salió de la celda", h != null && h.getX() < FTX + 7);
            cmd("kill @e[type=mundos_de_axelis:armadura_encantada]");
            cmd("execute in minecraft:overworld run tp @a " + (LX + 0.5) + " " + SY + " " + (LZ - 2.5) + " 0 45");
        });
        add("casa de teo (poner semilla)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), LX + 0.5, SY, LZ - 2.5, 0.0F, 45.0F);
            useOn(sp, com.mundosdeaxelis.npc.NpcService.kit("teo"), new net.minecraft.core.BlockPos(LX, SY - 1, LZ), net.minecraft.core.Direction.UP);
        }));
        add("holograma de teo", 2000, () -> {
            cmd("gamemode spectator @a");
            standLooking(LX + 9.5, SY + 5, LZ - 7.5, LX, SY + 2, LZ + 4);
        });
        add("holograma de teo (foto)", 300, () -> {
            check("la semilla de Teo queda puesta con su holograma", Minecraft.getInstance().level.getBlockEntity(seed) instanceof com.mundosdeaxelis.npc.SemillaCasaBlockEntity be
                    && "teo".equals(be.kind()) && be.scanTick != Long.MIN_VALUE && be.scanBlocked.isEmpty());
            shot("80_holograma_teo");
            cmd("gamemode survival @a");
            onServer(sp -> {
                sp.teleportTo(sp.serverLevel(), LX + 0.5, SY, LZ - 2.5, 0.0F, 45.0F);
                useBlock(sp, seed, false);
            });
        });
        add("casa de teo (comprobar)", 1200, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("crece la biblioteca de Teo", lv.getBlockState(new net.minecraft.core.BlockPos(LX, SY, LZ + 1)).is(net.minecraft.world.level.block.Blocks.DARK_OAK_DOOR)
                    && lv.getBlockState(new net.minecraft.core.BlockPos(LX - 1, SY, LZ + 6)).is(net.minecraft.world.level.block.Blocks.CHISELED_BOOKSHELF));
            com.mundosdeaxelis.entity.Habitante h = npcClient("teo");
            check("Teo vive en su biblioteca", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            cmd("gamemode spectator @a");
        });
        add("casa de teo (fuera)", 2500, () -> standLooking(LX + 9.5, SY + 3, LZ - 6.5, LX + 1, SY + 2.5, LZ + 4));
        add("casa de teo (fuera, foto)", 300, () -> shot("81_casa_teo"));
        add("casa de teo (dentro)", 2500, () -> standLooking(LX + 0.5, SY, LZ + 2.3, LX + 0.5, SY + 1.2, LZ + 7));
        add("casa de teo (dentro, foto)", 300, () -> shot("82_casa_teo_dentro"));
        add("teo vecino", 2600, () -> {
            cmd("gamemode survival @a");
            standLooking(LX + 0.5, SY, LZ + 2.3, LX + 0.5, SY + 1.4, LZ + 4.5);
            onServer(sp -> {
                com.mundosdeaxelis.table.Essence.add(sp, 600);
                com.mundosdeaxelis.table.Essence.sync(sp);
            });
            npcServer("teo", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("teo vecino (tienda)", 800, () -> {
            check("ventana de Teo vecino", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda (hasta el final)", 600, () -> {
            if (npcScreen() != null) {
                for (int i = 0; i < 14; i++) {
                    npcScreen().mouseScrolled(0, 0, -1);
                }
            }
        });
        add("tienda con cuatro vecinos (foto)", 800, () -> {
            shot("83_tienda_cuatro_vecinos");
            essBefore = TableClient.essence;
            if (npcScreen() != null) {
                npcScreen().buy(offer("book_mending")); // libro encantado: necesita 4 vecinos con casa
            }
        });
        add("tienda con cuatro vecinos (comprobar)", 1500, () -> {
            check("con cuatro vecinos se puede comprar el libro de Reparación (" + essBefore + " -> " + TableClient.essence + ")",
                    TableClient.essence == essBefore - lastPrice && invCount(net.minecraft.world.item.Items.ENCHANTED_BOOK) >= 1);
            open(null);
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned " + FTX + " " + FTY + " " + (FTZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
    }

    // ------------------------------------------------------------------ etapa 263: la casa de Ignacio y la tienda de cinco vecinos

    /** Casa de Ignacio (rescatado en la prueba del volcán) en {@code IX, IZ}. */
    private static final int IX = 110, IZ = -10;

    private static void ignacio() {
        net.minecraft.core.BlockPos seed = new net.minecraft.core.BlockPos(IX, SY, IZ);
        add("ignacio (ir)", 1500, () -> {
            open(null);
            cmd("gamemode survival @a");
            cmd("execute in minecraft:overworld run tp @a " + (IX + 0.5) + " " + SY + " " + (IZ - 2.5) + " 0 45");
        });
        add("casa de ignacio (poner semilla)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), IX + 0.5, SY, IZ - 2.5, 0.0F, 45.0F);
            useOn(sp, com.mundosdeaxelis.npc.NpcService.kit("ignacio"), new net.minecraft.core.BlockPos(IX, SY - 1, IZ), net.minecraft.core.Direction.UP);
        }));
        add("holograma de ignacio", 2000, () -> {
            cmd("gamemode spectator @a");
            standLooking(IX + 10.5, SY + 5, IZ - 7.5, IX, SY + 2, IZ + 4);
        });
        add("holograma de ignacio (foto)", 300, () -> {
            check("la semilla de Ignacio queda puesta con su holograma", Minecraft.getInstance().level.getBlockEntity(seed) instanceof com.mundosdeaxelis.npc.SemillaCasaBlockEntity be
                    && "ignacio".equals(be.kind()) && be.scanTick != Long.MIN_VALUE && be.scanBlocked.isEmpty());
            shot("84_holograma_ignacio");
            cmd("gamemode survival @a");
            onServer(sp -> {
                sp.teleportTo(sp.serverLevel(), IX + 0.5, SY, IZ - 2.5, 0.0F, 45.0F);
                useBlock(sp, seed, false);
            });
        });
        add("casa de ignacio (comprobar)", 1200, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("crece la ermita de Ignacio", lv.getBlockState(new net.minecraft.core.BlockPos(IX, SY, IZ + 1)).is(net.minecraft.world.level.block.Blocks.MANGROVE_DOOR)
                    && lv.getBlockState(new net.minecraft.core.BlockPos(IX - 2, SY, IZ + 5)).is(net.minecraft.world.level.block.Blocks.LECTERN));
            com.mundosdeaxelis.entity.Habitante h = npcClient("ignacio");
            check("Ignacio vive en su ermita", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            cmd("gamemode spectator @a");
        });
        add("casa de ignacio (fuera)", 2500, () -> standLooking(IX + 9.5, SY + 3, IZ - 6.5, IX + 1, SY + 2, IZ + 4));
        add("casa de ignacio (fuera, foto)", 300, () -> shot("85_casa_ignacio"));
        add("casa de ignacio (dentro)", 2500, () -> standLooking(IX + 0.5, SY, IZ + 2.3, IX + 0.5, SY + 1.2, IZ + 7));
        add("casa de ignacio (dentro, foto)", 300, () -> shot("86_casa_ignacio_dentro"));
        add("ignacio vecino", 2600, () -> {
            cmd("gamemode survival @a");
            standLooking(IX + 0.5, SY, IZ + 2.3, IX + 0.5, SY + 1.4, IZ + 4.5);
            onServer(sp -> {
                com.mundosdeaxelis.table.Essence.add(sp, 1000);
                com.mundosdeaxelis.table.Essence.sync(sp);
            });
            npcServer("ignacio", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("ignacio vecino (tienda)", 800, () -> {
            check("ventana de Ignacio vecino", npcScreen() != null && npcScreen().state() == com.mundosdeaxelis.entity.Habitante.VECINO);
            if (npcScreen() != null) {
                npcScreen().press(NpcScreen.B_SHOP);
            }
        });
        add("tienda de ignacio (hasta el final)", 600, () -> {
            if (npcScreen() != null) {
                for (int i = 0; i < 18; i++) {
                    npcScreen().mouseScrolled(0, 0, -1);
                }
            }
        });
        add("tienda con cinco vecinos (foto)", 800, () -> {
            shot("87_tienda_cinco_vecinos");
            essBefore = TableClient.essence;
            if (npcScreen() != null) {
                npcScreen().buy(offer("god_apple")); // manzana encantada: necesita 5 vecinos con casa
            }
        });
        add("tienda con cinco vecinos (comprobar)", 1500, () -> {
            check("con cinco vecinos se puede comprar la manzana encantada (" + essBefore + " -> " + TableClient.essence + ")",
                    TableClient.essence == essBefore - lastPrice && invCount(net.minecraft.world.item.Items.ENCHANTED_GOLDEN_APPLE) >= 1);
            open(null);
            cmd("gamemode spectator @a");
            cmd("mundos vecinos reiniciar");
            cmd("kill @e[type=item]");
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

    // ------------------------------------------------------------------ hojas de animación (cuadros seguidos de cada criatura)

    /** Zona de la prueba de animación (campo abierto del mundo plano). Criatura en (AX, -60, AZ). */
    private static final int AX = -200, AZ = -60;
    private static final int CELL = 200, COLS = 10, ROWS = 6;
    private static String clipName;
    private static int clipLeft, clipN;
    private static long clipNext, clipEvery;
    private static BufferedImage clipSheet;

    /** Quita todo lo que no sea un jugador (criaturas, objetos tirados, restos de los jefes) sin matarlo. */
    private static void clearAll() {
        IntegratedServer srv = Minecraft.getInstance().getSingleplayerServer();
        srv.execute(() -> {
            for (net.minecraft.server.level.ServerLevel l : srv.getAllLevels()) {
                List<net.minecraft.world.entity.Entity> gone = new ArrayList<>();
                for (net.minecraft.world.entity.Entity e : l.getAllEntities()) {
                    if (!(e instanceof net.minecraft.world.entity.player.Player)) {
                        gone.add(e);
                    }
                }
                gone.forEach(net.minecraft.world.entity.Entity::discard);
            }
        });
    }

    /** Empieza a guardar {@code n} cuadros, uno cada {@code every} ms, en la hoja de la criatura actual. */
    private static void clip(int n, long every) {
        clipLeft = n;
        clipEvery = every;
        clipNext = 0;
    }

    @SubscribeEvent
    public static void onRender(TickEvent.RenderTickEvent e) {
        if (!ON || e.phase != TickEvent.Phase.END || clipLeft <= 0 || clipSheet == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < clipNext) {
            return;
        }
        clipNext = now + clipEvery;
        clipLeft--;
        try (NativeImage n = Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())) {
            int w = n.getWidth(), h = n.getHeight();
            int side = (int) (Math.min(w, h) * 0.86);
            int x0 = (w - side) / 2, y0 = (h - side) / 2;
            int step = Math.max(1, side / CELL);
            BufferedImage cell = new BufferedImage(CELL, CELL, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < CELL; y++) {
                for (int x = 0; x < CELL; x++) {
                    // media de un cuadrito de píxeles (más suave que tomar uno solo)
                    long r = 0, g = 0, b = 0;
                    int cnt = 0;
                    int sx = x0 + x * side / CELL, sy = y0 + y * side / CELL;
                    for (int j = 0; j < step; j += 2) {
                        for (int i = 0; i < step; i += 2) {
                            int c = n.getPixelRGBA(Math.min(w - 1, sx + i), Math.min(h - 1, sy + j));
                            r += c & 0xFF;
                            g += c >> 8 & 0xFF;
                            b += c >> 16 & 0xFF;
                            cnt++;
                        }
                    }
                    cell.setRGB(x, y, (int) (r / cnt) << 16 | (int) (g / cnt) << 8 | (int) (b / cnt));
                }
            }
            int k = clipN++;
            if (k < COLS * ROWS) {
                clipSheet.getGraphics().drawImage(cell, k % COLS * CELL, k / COLS * CELL, null);
            }
        } catch (Exception ex) {
            log("FALLO cuadro de " + clipName + ": " + ex);
        }
    }

    /** La criatura de la prueba de animación (en el servidor). */
    private static void onMob(String id, java.util.function.Consumer<net.minecraft.world.entity.Mob> c) {
        onServer(sp -> {
            for (net.minecraft.world.entity.Entity en : sp.serverLevel().getAllEntities()) {
                if (en instanceof net.minecraft.world.entity.Mob mob && id.equals(net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(en.getType()).getPath())) {
                    try {
                        c.accept(mob);
                    } catch (Throwable t) {
                        log("aviso en la animación de " + id + ": " + t);
                    }
                    return;
                }
            }
        });
    }

    /**
     * Graba una hoja de cuadros de la criatura {@code id}: quieta (6), andando de un lado a otro (18), atacando (12), recibiendo
     * un golpe (8) y muriendo (16). La cámara la mira de lado desde {@code d} bloques, centrada en la altura {@code cy}. Los jefes
     * ({@code boss}) no mueren (su muerte cuenta como victoria): se quitan.
     */
    private static void animClip(String id, double y, double cy, double d, String nbt, boolean boss) {
        add("animación " + id, 1800, () -> {
            clearAll();
            clipName = id;
            clipN = 0;
            clipSheet = new BufferedImage(CELL * COLS, CELL * ROWS, BufferedImage.TYPE_INT_RGB);
            cmd(f("execute in minecraft:overworld run summon mundos_de_axelis:%s %.1f %.2f %.1f {PersistenceRequired:1b,Silent:1b,Rotation:[90f,0f]%s}",
                    id, AX + 0.5, y, AZ + 0.5, nbt));
            onMob(id, mob -> {
                mob.goalSelector.removeAllGoals(g -> true);
                mob.targetSelector.removeAllGoals(g -> true);
                mob.setHealth(mob.getMaxHealth());
            });
            standLooking(AX + 0.5, cy - 1.2, AZ + 0.5 - d, AX + 0.5, cy, AZ + 0.5);
        });
        add("animación " + id + ": quieta", 6 * 110 + 200, () -> clip(6, 110));
        add("animación " + id + ": andando", 18 * 110 + 200, () -> {
            if (nbt.contains("Awake:0b")) {
                cmd(f("execute in minecraft:overworld run data merge entity @e[type=mundos_de_axelis:%s,limit=1] {Awake:1b}", id));
            }
            onMob(id, mob -> {
                mob.setAggressive(true);
                // de un lado a otro sin salirse del cuadro (lo que se ve a esa distancia)
                double r = d * 0.45;
                mob.getNavigation().moveTo(AX + 0.5 + r, y, AZ + 0.5, 1.0);
                for (int leg = 1; leg <= 3; leg++) {
                    double tx = AX + 0.5 + (leg % 2 == 0 ? r : -r);
                    com.mundosdeaxelis.world.Scheduler.runLater(leg * 13, () -> {
                        if (mob.isAlive()) {
                            mob.getNavigation().moveTo(tx, y, AZ + 0.5, 1.1);
                        }
                    });
                }
            });
            clip(18, 110);
        });
        add("animación " + id + ": ataca", 12 * 85 + 200, () -> {
            onMob(id, mob -> {
                mob.getNavigation().stop();
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                com.mundosdeaxelis.world.Scheduler.runLater(14, () -> mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND));
            });
            clip(12, 85);
        });
        add("animación " + id + ": golpe", 8 * 85 + 200, () -> {
            onMob(id, mob -> mob.hurt(mob.damageSources().generic(), Math.min(2.0F, mob.getHealth() * 0.1F)));
            clip(8, 85);
        });
        add("animación " + id + ": muere", 16 * 90 + 300, () -> {
            if (boss) {
                onMob(id, mob -> mob.hurt(mob.damageSources().generic(), Math.min(2.0F, mob.getHealth() * 0.1F)));
            } else {
                onMob(id, net.minecraft.world.entity.LivingEntity::kill);
            }
            clip(16, 90);
        });
        add("animación " + id + " (hoja)", 300, () -> {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("screenshots");
                Files.createDirectories(dir);
                javax.imageio.ImageIO.write(clipSheet, "png", dir.resolve("axelis_anim_" + id + ".png").toFile());
                check("hoja de animación de " + id + " (" + clipN + " cuadros)", clipN >= 50);
            } catch (Exception ex) {
                fails++;
                log("FALLO hoja de " + id + ": " + ex);
            }
            clipSheet = null;
        });
    }

    /** Mascotas nuevas siguiendo al dueño, que se va moviendo de un lado a otro (para ver cómo andan, vuelan y se inclinan). */
    private static void petClip() {
        add("animación mascotas", 2500, () -> {
            clearMobs();
            clipName = "mascotas";
            clipN = 0;
            clipSheet = new BufferedImage(CELL * COLS, CELL * ROWS, BufferedImage.TYPE_INT_RGB);
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 900 4 true");
            standLooking(AX - 4.5, -60.0, AZ - 4.5, AX - 4.5, -60.4, AZ - 1.0);
            // etapa 254: equipadas, son criaturas de verdad que te siguen (quietas más de 45 s se duermen: se ve al final de la tanda quieta)
            equipPets(com.mundosdeaxelis.relic.Companions.LION, com.mundosdeaxelis.relic.Companions.SHEEP,
                    com.mundosdeaxelis.relic.Companions.CRYSTAL_DRAGON);
        });
        // ahora son criaturas que te siguen por detrás (como los lobos): para el video se ponen delante de la cámara y luego caminan
        // de un lado a otro con su propio camino (las órdenes de ir van directas a su navegación)
        add("animación mascotas: delante", 1500, () -> onServer(sp -> {
            int i = 0;
            for (com.mundosdeaxelis.entity.Mascota m : serverPets(sp)) {
                m.moveTo(AX - 5.7 + i * 1.2, m.flier() ? -59.0 : -60.0, AZ - 1.2, 180F, 0F);
                m.setYBodyRot(180F);
                m.setYHeadRot(180F);
                m.getNavigation().stop();
                i++;
            }
        }));
        add("animación mascotas: quietas", 10 * 110 + 200, () -> clip(10, 110));
        for (int k = 0; k < 5; k++) {
            final int kk = k;
            add("animación mascotas: caminan " + k, 10 * 110 + 200, () -> {
                onServer(sp -> {
                    int i = 0;
                    for (com.mundosdeaxelis.entity.Mascota m : serverPets(sp)) {
                        double tx = AX - 4.5 + (kk % 2 == 0 ? 2.8 : -2.8) + (i - 1) * 0.9;
                        m.getNavigation().moveTo(tx, m.flier() ? -58.8 : -60.0, AZ - 0.6 - i * 0.6, 1.0);
                        i++;
                    }
                });
                clip(10, 110);
            });
        }
        add("animación mascotas (hoja)", 300, () -> {
            try {
                Path dir = FMLPaths.GAMEDIR.get().resolve("screenshots");
                Files.createDirectories(dir);
                javax.imageio.ImageIO.write(clipSheet, "png", dir.resolve("axelis_anim_mascotas.png").toFile());
                check("hoja de animación de las mascotas (" + clipN + " cuadros)", clipN >= 50);
            } catch (Exception ex) {
                fails++;
                log("FALLO hoja de mascotas: " + ex);
            }
            clipSheet = null;
            equipPets();
            cmd("gamemode spectator @a");
        });
    }

    /** Solo una parte de la prueba (para probar rápido): -Dmundos.autotest.solo=armas */
    private static final String SOLO = System.getProperty("mundos.autotest.solo", "").trim();

    private static void build() {
        if (SOLO.equals("armas")) {
            add("reglas", 800, () -> {
                cmd("gamerule sendCommandFeedback false");
                cmd("gamerule doDaylightCycle false");
                cmd("gamerule doMobSpawning false");
                cmd("time set 6000");
                cmd("weather clear 999999");
                cmd("kill @e[type=!player]");
                cmd("difficulty normal");
                cmd("gamemode survival @a");
                cmd("effect give @a minecraft:resistance 99999 4 true");
                cmd("mundos desbloquear 5");
            });
            armas();
            cuerpoArmas();
            etapa282();
            tiendaNueva();
            add("fin", 2000, () -> {
                open(null);
                log("AUTOTEST_FIN fotos=" + shots + " comprobaciones=" + checks + " fallos=" + fails);
            });
            add("salir", 1000, () -> Minecraft.getInstance().stop());
            return;
        }
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
        // 5b. etapa 254: las mascotas equipadas son criaturas de verdad (Lobito Ártico, Archimaguito y el Dragón de la Noche, que vuela)
        add("fichas de mascotas", 300, () -> {
            int miss = 0;
            for (com.mundosdeaxelis.relic.Companions.Spec sp : com.mundosdeaxelis.relic.Companions.ALL) {
                if (com.mundosdeaxelis.relic.PetKinds.of(sp.species()) == null) {
                    miss++;
                    log("sin ficha: " + sp.id());
                }
            }
            check("todas las mascotas tienen ficha de criatura (" + com.mundosdeaxelis.relic.Companions.ALL.length + ")", miss == 0);
        });
        add("mascotas", 3500, () -> {
            clearMobs();
            cmd("tp @a 0.5 -60 0.5 0 18");
            equipPets(com.mundosdeaxelis.relic.Companions.FROST_PUP, com.mundosdeaxelis.relic.Companions.LITTLE_ARCHMAGE,
                    com.mundosdeaxelis.relic.Companions.NIGHT_DRAGON);
        });
        add("mascotas: aparecen", 300, () -> check("las 3 mascotas equipadas son criaturas en el mundo (cliente ve " + pets().size() + ")",
                pets().size() == 3));
        add("mascotas: delante", 1500, () -> onServer(sp -> {
            // que se pongan delante de la cámara para la foto
            int i = 0;
            for (com.mundosdeaxelis.entity.Mascota m : serverPets(sp)) {
                m.moveTo(-1.2 + i * 1.3, m.flier() ? -58.6 : -60.0, 3.2, 180F, 0F);
                m.setYBodyRot(180F);
                m.setYHeadRot(180F);
                m.getNavigation().stop();
                i++;
            }
        }));
        add("mascotas (foto)", 600, () -> shot("8b_mascotas"));
        add("mascotas de cerca (foto)", 300, () -> shot("8c_mascotas"));
        add("mascota: sentarse", 1500, () -> onServer(sp -> {
            List<com.mundosdeaxelis.entity.Mascota> l = serverPets(sp);
            if (!l.isEmpty()) {
                l.get(0).toggleSit(sp);
            }
        }));
        add("mascota: sentada", 300, () -> {
            boolean sat = false;
            for (com.mundosdeaxelis.entity.Mascota m : pets()) {
                sat |= m.isInSittingPose();
            }
            check("clic derecho: la mascota se sienta", sat);
            onServer(sp -> {
                boolean home = false;
                for (net.minecraft.world.item.ItemStack st : com.mundosdeaxelis.relic.Equipment.get(sp).pets) {
                    home |= com.mundosdeaxelis.relic.PetData.read(st).contains("Home");
                }
                check("la figurita recuerda dónde te espera sentada", home);
            });
        });
        add("mascota: pide comida", 1800, () -> onServer(sp -> {
            List<com.mundosdeaxelis.entity.Mascota> l = serverPets(sp);
            if (l.size() > 1) {
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(l.get(1).kind().food()));
            }
        }));
        add("mascota: pide comida (resultado)", 300, () -> {
            boolean beg = false;
            for (com.mundosdeaxelis.entity.Mascota m : pets()) {
                beg |= m.hasFlag(com.mundosdeaxelis.entity.Mascota.F_BEG);
            }
            check("con su comida favorita en tu mano, te la pide (ladea la cabeza)", beg);
            onServer(sp -> sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY));
        });
        add("mascota: comer", 1200, () -> onServer(sp -> {
            List<com.mundosdeaxelis.entity.Mascota> l = serverPets(sp);
            if (!l.isEmpty()) {
                com.mundosdeaxelis.entity.Mascota m = l.get(0);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(m.kind().food(), 3));
                m.mobInteract(sp, net.minecraft.world.InteractionHand.MAIN_HAND);
                sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }));
        add("mascota: cariño", 300, () -> {
            int love = 0;
            for (com.mundosdeaxelis.entity.Mascota m : pets()) {
                love = Math.max(love, m.love());
            }
            check("al darle su comida favorita sube su cariño (" + love + ")", love >= 8);
        });
        add("mascota: menú", 1500, () -> onServer(sp -> {
            List<com.mundosdeaxelis.entity.Mascota> l = serverPets(sp);
            if (!l.isEmpty()) {
                sp.setShiftKeyDown(true);
                l.get(l.size() - 1).mobInteract(sp, net.minecraft.world.InteractionHand.MAIN_HAND);
                sp.setShiftKeyDown(false);
            }
        }));
        add("mascota: menú abierto", 300, () -> check("agachado + clic derecho abre el menú de la mascota (pantalla " + screenName() + ")",
                Minecraft.getInstance().screen instanceof PetScreen));
        add("mascota: menú (foto)", 1200, () -> shot("8e_menu_mascota"));
        add("mascota: nombre", 1000, () -> onServer(sp -> {
            if (sp.containerMenu instanceof com.mundosdeaxelis.relic.PetMenu pm) {
                pm.rename(sp, "Pelusa");
            }
        }));
        add("mascota: nombre puesto", 300, () -> {
            boolean ok = false;
            for (com.mundosdeaxelis.entity.Mascota m : pets()) {
                ok |= m.hasCustomName() && m.getCustomName().getString().equals("Pelusa");
            }
            check("el menú le cambia el nombre a la mascota", ok);
            onServer(sp -> {
                boolean saved = false;
                for (net.minecraft.world.item.ItemStack st : com.mundosdeaxelis.relic.Equipment.get(sp).pets) {
                    saved |= com.mundosdeaxelis.relic.PetData.name(st).equals("Pelusa");
                }
                check("el nombre queda guardado en la figurita", saved);
            });
        });
        add("mascota: cerrar menú", 800, () -> open(null));
        add("mascota: pelea", 4500, () -> {
            // (la dificultad ya es «normal» desde la tanda de enemigos: no se cambia, la necesitan las pruebas del volcán)
            cmd("summon minecraft:zombie 0.5 -60 6.5 {NoAI:1b,PersistenceRequired:1b,Tags:[\"pet_target\"]}");
            onServer(sp -> {
                net.minecraft.world.entity.LivingEntity z = null;
                for (net.minecraft.world.entity.Entity e : sp.serverLevel().getAllEntities()) {
                    if (e.getTags().contains("pet_target") && e instanceof net.minecraft.world.entity.LivingEntity le) {
                        z = le;
                    }
                }
                zombieSeen = z != null;
                for (com.mundosdeaxelis.entity.Mascota m : serverPets(sp)) {
                    if (m.isOrderedToSit()) {
                        m.toggleSit(sp);
                    }
                    if (z != null && m.kind().combat()) {
                        m.setTarget(z);
                    }
                }
            });
        });
        add("mascota: pelea (resultado)", 300, () -> onServer(sp -> {
            float hp = -1, max = 1;
            boolean found = false;
            for (net.minecraft.world.entity.Entity e : sp.serverLevel().getAllEntities()) {
                if (e.getTags().contains("pet_target") && e instanceof net.minecraft.world.entity.LivingEntity le) {
                    found = true;
                    hp = le.getHealth();
                    max = le.getMaxHealth();
                    e.discard();
                }
            }
            check("las mascotas de combate atacan a un zombi (" + (found ? "vida " + hp + " de " + max : "lo vencieron") + ")",
                    zombieSeen && (!found || hp < max));
        }));
        add("mascota: se cansa", 400, () -> onServer(sp -> {
            List<com.mundosdeaxelis.entity.Mascota> l = serverPets(sp);
            if (!l.isEmpty()) {
                l.get(0).hurt(sp.damageSources().generic(), 1000F);
            }
        }));
        add("mascota: cansada (se duerme)", 2200, () -> {
            boolean dying = false;
            Minecraft mc = Minecraft.getInstance();
            for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                dying |= e instanceof com.mundosdeaxelis.entity.Mascota m && !m.isRemoved() && m.isDeadOrDying();
            }
            check("al cansarse no desaparece de golpe: se duerme y se encoge", dying);
        });
        add("mascota: descansando", 300, () -> {
            check("al cansarse vuelve a su figurita (quedan " + pets().size() + " de 3)", pets().size() == 2);
            onServer(sp -> {
                boolean rest = false;
                for (net.minecraft.world.item.ItemStack st : com.mundosdeaxelis.relic.Equipment.get(sp).pets) {
                    rest |= com.mundosdeaxelis.relic.PetData.read(st).getLong("RestUntil") > sp.level().getGameTime();
                }
                check("su descanso queda guardado en la figurita", rest);
            });
        });
        // las mismas figuritas (con lo que recuerdan) se quitan del equipo y se vuelven a poner
        add("mascotas: quitar y poner", 1200, () -> onServer(sp -> {
            com.mundosdeaxelis.relic.Equipment.Data d = com.mundosdeaxelis.relic.Equipment.get(sp);
            savedPets = d.pets.clone();
            java.util.Arrays.fill(d.pets, net.minecraft.world.item.ItemStack.EMPTY);
            com.mundosdeaxelis.relic.Equipment.sync(sp);
        }));
        add("mascotas: poner otra vez", 2200, () -> onServer(sp -> {
            com.mundosdeaxelis.relic.Equipment.Data d = com.mundosdeaxelis.relic.Equipment.get(sp);
            if (savedPets != null) {
                System.arraycopy(savedPets, 0, d.pets, 0, Math.min(savedPets.length, d.pets.length));
            }
            com.mundosdeaxelis.relic.Equipment.sync(sp);
        }));
        add("mascotas: la cansada sigue descansando", 300, () -> check("quitar y poner la figurita no se salta el descanso (aparecen "
                + pets().size() + " de 3)", pets().size() == 2));
        add("mascotas: quitarlas", 1800, () -> equipPets());
        add("mascotas: se van", 300, () -> check("al quitarlas del menú de equipo se van (quedan " + pets().size() + ")", pets().isEmpty()));
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
        add("camino (foto)", 300, () -> {
            check("fuera del volcán no suena su música (zona " + AmbientMusic.volcanZone() + ")", AmbientMusic.volcanZone() == 0);
            shot("32_volcan_camino");
        });
        add("cono y fumarolas", 3500, () -> lookV(62, 22, 122, 37, 10, 97));
        add("cono y fumarolas (foto)", 300, () -> shot("32b_volcan_cono"));
        add("pie del volcán", 3000, () -> lookV(-70, 4, 30, -40, 0, 50));
        add("pie del volcán (foto)", 300, () -> shot("32c_volcan_pie"));
        add("cráter", 3500, () -> lookV(0, 82, 36, 0, 61, 60));
        add("cráter (foto)", 300, () -> shot("33_volcan_crater"));
        // etapa 263: la pista para tirarse (arco rojo, muelle y cartel) y la lava de los ríos que corre
        add("salto de fe", 3000, () -> lookV(15.5, 70, 45.5, 6, 63, 55.5));
        add("salto de fe (foto)", 300, () -> {
            net.minecraft.world.level.Level lv = Minecraft.getInstance().level;
            check("al final del camino hay un arco rojo sobre el muelle del lago", lv.getBlockState(new net.minecraft.core.BlockPos(7, VY + 66, VZ + 55))
                    .is(net.minecraft.world.level.block.Blocks.RED_NETHER_BRICKS));
            check("y un cartel «Salto de fe»", lv.getBlockState(new net.minecraft.core.BlockPos(9, VY + 65, VZ + 53)).is(net.minecraft.world.level.block.Blocks.CRIMSON_SIGN));
            net.minecraft.world.level.block.state.BlockState river = lv.getBlockState(new net.minecraft.core.BlockPos(-40, VY + 6, VZ + 35));
            check("la lava de los ríos corre ladera abajo (" + river + ")", river.is(com.mundosdeaxelis.registry.ModBlocks.LAVA_FALSA.get())
                    && river.getValue(com.mundosdeaxelis.block.LavaFalsaBlock.FLUJO) == net.minecraft.core.Direction.WEST);
            shot("33b_volcan_salto");
        });
        add("lago del cráter", 2500, () -> lookV(-6, 66, 62, 6, 61, 56));
        add("lago del cráter (foto)", 300, () -> shot("33c_volcan_lago"));
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
        add("cámara (foto)", 1500, () -> {
            long since = Minecraft.getInstance().level.getGameTime() - BossBars.granBarTick;
            check("lejos de su cámara no se ve la barra de la Gran Salamandra", since > 40);
            shot("35_volcan_camara");
        });
        add("puente", 1500, () -> lookV(-11.5, 24, 78.5, 4, 24, 78.5));
        add("puente (foto)", 300, () -> shot("36_volcan_puente"));
        add("pisar el puente", 700, () -> lookV(-8.5, 24, 78.5, 4, 23, 78.5));
        add("cruje (foto)", 2500, () -> shot("37_puente_cruje"));
        // si el servidor de pruebas va lento, el derrumbe (bloque a bloque) tarda un poco más en llegar a los pies
        add("se derrumba", 1500, () -> {
        });
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
        add("altar (foto)", 300, () -> {
            check("dentro del volcán suena su música (zona " + AmbientMusic.volcanZone() + ")", AmbientMusic.volcanZone() == 1);
            shot("41_volcan_altar");
        });
        // etapa 255: el acertijo de los braseros, la sala del tesoro y el guardián
        add("braseros", 2500, () -> {
            onServer(sp -> {
                net.minecraft.server.level.ServerLevel sl = sp.serverLevel();
                for (int[] c : new int[][]{{-15, 67}, {-15, 85}, {9, 67}, {9, 85}}) {
                    net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(c[0], VY + 8, VZ + c[1]);
                    sl.getBlockState(p).use(sl, sp, net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.phys.BlockHitResult(
                            net.minecraft.world.phys.Vec3.atCenterOf(p), net.minecraft.core.Direction.UP, p, false));
                }
            });
            lookV(-14.5, 9, 66.5, 2, 8, 80);
        });
        add("braseros (foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState g = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(15, VY + 8, VZ + 76));
            check("encender los cuatro braseros abre la reja del tesoro", g.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock
                    && g.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN));
            shot("41b_volcan_braseros");
        });
        add("sala del tesoro", 2500, () -> {
            clearHostiles();
            lookV(13.5, 8, 76.5, 22, 8.5, 76.5);
        });
        add("sala del tesoro (foto)", 300, () -> shot("41c_volcan_tesoro"));
        add("guardián", 3000, () -> {
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:gran_salamandra 22.5 " + (VY - 8) + " " + (VZ + 100.5));
            lookV(28.5, -8, 95.5, 22, -7, 100.5);
        });
        add("guardián (foto)", 300, () -> {
            check("la Gran Salamandra guarda su cámara", clientCount(com.mundosdeaxelis.entity.GranSalamandra.class) > 0);
            check("cerca de ella se ve su barra", Minecraft.getInstance().level.getGameTime() - BossBars.granBarTick < 40);
            net.minecraft.world.level.block.state.BlockState g = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(14, VY - 8, VZ + 100));
            check("la reja del guardián está cerrada", g.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock
                    && !g.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN));
            shot("41d_volcan_guardian");
        });
        add("vencer al guardián", 2500, () -> cmd("kill @e[type=mundos_de_axelis:gran_salamandra]"));
        add("reja abierta (foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState g = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(14, VY - 8, VZ + 100));
            check("al vencer a la Gran Salamandra se abre la reja", g.getBlock() instanceof com.mundosdeaxelis.block.RejaVolcanBlock
                    && g.getValue(com.mundosdeaxelis.block.RejaVolcanBlock.OPEN));
            shot("41e_volcan_reja");
        });
        add("santuario", 3000, () -> {
            clearHostiles();
            lookV(0.5, -8, 82.5, 0.5, -6, 90.5);
        });
        add("santuario (foto)", 300, () -> {
            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(0, VY - 6, VZ + 90);
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level.getBlockState(p).getBlock()).getPath();
            check("la Piedra de Portal: Volcán está en el santuario (" + id + ")", id.equals("portal_stone_volcano"));
            shot("42_volcan_santuario");
        });
        // etapa 263: Ignacio, preso en la celda del santuario; el sumo sacerdote lleva la llave
        add("ignacio preso", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            lookV(-9.5, -8, 90.5, -13, -6.6, 90.5);
        });
        add("ignacio preso (hablar)", 2600, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("ignacio");
            check("Ignacio está preso en la celda del volcán", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.PRESO);
            npcServer("ignacio", com.mundosdeaxelis.npc.NpcService::interact);
        });
        add("ignacio preso (foto)", 300, () -> {
            check("se abre la ventana de Ignacio", npcScreen() != null);
            shot("42b_ignacio_preso");
            open(null);
        });
        add("llave del sumo sacerdote", 1500, () -> {
            cmd("execute in minecraft:overworld run summon mundos_de_axelis:cultista_volcan -3.5 " + (VY - 8) + " " + (VZ + 86.5)
                    + " {ForgeData:{MundosTag:\"llave_celda\"}}");
            cmd("kill @e[type=mundos_de_axelis:cultista_volcan,nbt={ForgeData:{MundosTag:\"llave_celda\"}}]");
        });
        add("llave del sumo sacerdote (comprobar)", 300, () -> onServer(sp -> towerKey = !sp.serverLevel().getEntitiesOfClass(
                net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(32),
                i -> i.getItem().is(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get())).isEmpty()));
        add("llave en la celda del volcán", 2600, () -> {
            check("el sumo sacerdote suelta la llave de la celda", towerKey);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()),
                    new net.minecraft.core.BlockPos(-11, VY - 8, VZ + 92), net.minecraft.core.Direction.EAST));
        });
        add("ignacio rescatado (foto)", 300, () -> {
            com.mundosdeaxelis.entity.Habitante h = npcClient("ignacio");
            check("Ignacio queda libre", h != null && h.state() == com.mundosdeaxelis.entity.Habitante.LIBRE);
            check("Ignacio da su Semilla de casa", invCount(com.mundosdeaxelis.registry.ModItems.SEMILLA_CASA.get()) >= 1);
            shot("42c_ignacio_rescatado");
            open(null);
        });
        // la salida: la Piedra de regreso, junto al altar, lleva a la entrada del volcán
        add("piedra de regreso", 2500, () -> {
            cmd("clear @a mundos_de_axelis:semilla_casa");
            lookV(5.5, -8, 85.5, 3.5, -7.5, 87.5);
        });
        add("piedra de regreso (foto)", 300, () -> {
            net.minecraft.world.level.block.state.BlockState st = Minecraft.getInstance().level.getBlockState(new net.minecraft.core.BlockPos(3, VY - 8, VZ + 87));
            check("junto al altar está la Piedra de regreso (" + st.getBlock() + ")", st.is(com.mundosdeaxelis.registry.ModBlocks.PIEDRA_REGRESO.get()));
            shot("42d_volcan_piedra_regreso");
            onServer(sp -> useBlock(sp, new net.minecraft.core.BlockPos(3, VY - 8, VZ + 87), false));
        });
        add("en la entrada", 2500, () -> {
        });
        add("en la entrada (foto)", 300, () -> {
            net.minecraft.world.entity.player.Player pl = Minecraft.getInstance().player;
            double lx = pl.getX(), lz = pl.getZ() - VZ;
            check("la Piedra de regreso te deja en la entrada del volcán (" + String.format(Locale.ROOT, "%.1f %.1f %.1f", lx, pl.getY() - VY, lz) + ")",
                    Math.abs(lx) < 4 && lz > -6 && lz < 4);
            shot("43_volcan_regreso");
        });
        add("erupción", 1500, () -> {
            cmd("gamemode spectator @a");
            lookV(70, 40, -20, 0, 60, 60);
            onServer(sp -> com.mundosdeaxelis.block.NucleoVolcanBlock.erupt(sp.serverLevel(),
                    new net.minecraft.core.BlockPos(0, VY + 61, VZ + 60), sp.getRandom()));
        });
        add("erupción (foto)", 2000, () -> shot("44_volcan_erupcion"));
        add("erupción b (foto)", 300, () -> shot("45_volcan_erupcion"));
        // etapa 256: el Santuario del Bosque (la primera estructura) y que las estructuras no se pueden picar
        add("santuario", 6000, () -> {
            open(null);
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned " + SX + " " + SY + " " + (SZ0 - 16) + " rotated 0 0 run mundos estructura santuario");
        });
        add("santuario de frente", 3000, () -> lookS(0, 6, -12, 0, 3, 22));
        add("santuario de frente (foto)", 300, () -> shot("46_santuario_frente"));
        add("santuario de lado", 2500, () -> lookS(24, 16, 2, 0, 2, 24));
        add("santuario de lado (foto)", 300, () -> shot("47_santuario_lado"));
        add("santuario dentro", 2500, () -> lookS(0.5, 0, 20.5, 0.5, 1, 30.5));
        add("santuario dentro (foto)", 300, () -> {
            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(SX, SY, SZ0 + 28);
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level.getBlockState(p).getBlock()).getPath();
            check("al fondo del santuario está la celda de Ruda (" + id + ")", id.equals("iron_door"));
            check("enemigos del santuario a la vista (" + clientCount(com.mundosdeaxelis.entity.Brote.class) + " brotes, "
                    + clientCount(com.mundosdeaxelis.entity.Raizal.class) + " raizal)", clientCount(com.mundosdeaxelis.entity.Brote.class) >= 2
                    && clientCount(com.mundosdeaxelis.entity.Raizal.class) >= 1);
            shot("48_santuario_dentro");
        });
        add("celda", 2500, () -> lookS(0.5, 0, 23.5, 0.5, 1, 30));
        add("celda (foto)", 300, () -> shot("49_santuario_celda"));
        add("no se puede picar", 1500, () -> {
            cmd("gamemode survival @a");
            onServer(sp -> {
                // una pared del santuario y la Piedra de Portal: romper como un jugador en supervivencia no debe funcionar
                net.minecraft.core.BlockPos wall = new net.minecraft.core.BlockPos(SX - 8, SY + 1, SZ0 + 27);
                net.minecraft.core.BlockPos stone = new net.minecraft.core.BlockPos(SX - 2, SY, SZ0 + 28); // los barrotes de la celda
                boolean a = sp.gameMode.destroyBlock(wall);
                boolean b = sp.gameMode.destroyBlock(stone);
                guardBroke = a || b || sp.serverLevel().getBlockState(wall).isAir() || sp.serverLevel().getBlockState(stone).isAir();
            });
        });
        add("no se puede picar (comprobar)", 300, () -> {
            check("las estructuras no se pueden picar en supervivencia", !guardBroke);
        });
        ruda();
        add("santuario (deshacer)", 1500, () -> {
            open(null);
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned " + SX + " " + SY + " " + (SZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
        templo();
        ciudadela();
        fortin();
        fragua();
        torre();
        ignacio();
        botin();
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
        // como lo abre el jugador: el servidor manda su álbum (si no, el cliente no sabe qué cartas tiene)
        add("álbum: jefes", 2500, () -> onServer(com.mundosdeaxelis.cartas.CartasNet::openAlbum));
        add("álbum: jefes (foto)", 300, () -> {
            check("el álbum tiene " + com.mundosdeaxelis.cartas.Cards.ALL.size() + " cartas (41 esperadas)", com.mundosdeaxelis.cartas.Cards.ALL.size() == 41);
            shot("46_album_jefes");
        });
        add("álbum: criaturas", 1800, () -> Minecraft.getInstance().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
        add("álbum: criaturas (foto)", 300, () -> shot("47_album_criaturas"));
        add("álbum: criaturas nuevas", 1800, () -> Minecraft.getInstance().screen.keyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT, 0, 0));
        add("álbum: criaturas nuevas (foto)", 300, () -> shot("48_album_criaturas_nuevas"));
        // 9. animaciones en movimiento: cada criatura quieta, andando, atacando, recibiendo un golpe y muriendo, en hojas de cuadros
        add("estudio de animación", 2500, () -> {
            open(null);
            clearAll();
            Minecraft.getInstance().options.hideGui = true;
            cmd("gamemode spectator @a");
            cmd("time set 6000");
        });
        animClip("brote", -60.0, -59.3, 2.0, "", false);
        animClip("espinoso", -60.0, -59.2, 2.0, "", false);
        animClip("polinizador", -58.6, -58.4, 1.8, "", false);
        animClip("raizal", -60.0, -58.6, 3.0, "", false);
        animClip("guardian_musgo", -60.0, -58.3, 3.8, "", false);
        animClip("rey_brote", -60.0, -57.8, 6.0, "", true);
        animClip("escoria", -60.0, -59.4, 1.8, "", false);
        animClip("chispero", -60.0, -59.3, 2.0, "", false);
        animClip("ascua", -58.8, -58.7, 1.7, "", false);
        animClip("fundidor", -60.0, -58.6, 3.0, "", false);
        animClip("capataz", -60.0, -58.4, 3.5, "", false);
        animClip("gran_herrero", -60.0, -57.4, 6.5, "", true);
        animClip("lobo_escarcha", -60.0, -59.2, 2.8, "", false);
        animClip("lobo_artico", -60.0, -57.9, 5.5, "", true);
        animClip("esbirro", -60.0, -59.0, 2.6, "", false);
        animClip("armadura_encantada", -60.0, -59.0, 2.6, ",Awake:0b", false);
        animClip("libro_embrujado", -59.0, -58.8, 1.6, "", false);
        animClip("archimago", -60.0, -58.0, 4.0, "", true);
        animClip("salamandra_lava", -60.0, -59.6, 2.0, "", false);
        animClip("diablillo", -58.8, -58.7, 1.7, "", false);
        animClip("cultista_volcan", -60.0, -58.9, 2.5, "", false);
        animClip("larva_magma", -60.0, -59.7, 1.6, "", false);
        petClip();
        add("fin de las animaciones", 500, () -> {
            clearMobs();
            Minecraft.getInstance().options.hideGui = false;
        });
        add("fin", 2000, () -> {
            clearMobs();
            open(null);
            log("AUTOTEST_FIN fotos=" + shots + " comprobaciones=" + checks + " fallos=" + fails);
        });
        add("salir", 1000, () -> Minecraft.getInstance().stop());
    }
}
