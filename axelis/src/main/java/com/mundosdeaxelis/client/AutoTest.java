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
        for (var en : Minecraft.getInstance().level.entitiesForRendering()) {
            if (en instanceof com.mundosdeaxelis.entity.Habitante h && "ruda".equals(h.kindId())) {
                return h;
            }
        }
        return null;
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

    private static void ruda() {
        net.minecraft.core.BlockPos door = new net.minecraft.core.BlockPos(SX + 8, SY, SZ0 + 26);
        add("ruda presa", 2500, () -> {
            cmd("gamemode survival @a");
            cmd("effect give @a minecraft:resistance 600 4 true");
            cmd("kill @e[type=mundos_de_axelis:brote]");
            cmd("kill @e[type=mundos_de_axelis:espinoso]");
            lookS(4.5, 0, 23.5, 11.5, 1.4, 24.5);
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
            check("el Raizal del altar suelta la llave de la celda", keyDropped);
            cmd("kill @e[type=item]");
            onServer(sp -> useOn(sp, new net.minecraft.world.item.ItemStack(com.mundosdeaxelis.registry.ModItems.LLAVE_CELDA.get()), door,
                    net.minecraft.core.Direction.WEST));
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
        add("ruda sale de la celda", 5000, () -> lookS(-2.5, 1, 22.5, 6, 0.8, 26.5));
        add("ruda sale de la celda (foto)", 300, () -> {
            rudaServer((sp, h) -> log("ruda servidor: en " + h.blockPosition().toShortString() + ", su sitio " + (h.home() == null ? "?" : h.home().toShortString())
                    + ", camino " + (h.getNavigation().getPath() == null ? "ninguno" : h.getNavigation().getPath().getNodeCount() + " pasos, llega=" + h.getNavigation().getPath().canReach())));
            com.mundosdeaxelis.entity.Habitante h = rudaClient();
            check("Ruda salió de la celda (x " + (h == null ? "?" : String.format("%.1f", h.getX())) + ")", h != null && h.getX() < SX + 8);
            shot("52_ruda_libre");
        });
        add("casa", 2500, () -> {
            cmd("execute in minecraft:overworld run tp @a " + (HX + 0.5) + " " + SY + " " + (HZ - 2.5) + " 0 45");
        });
        add("casa (plantar)", 2500, () -> onServer(sp -> {
            sp.teleportTo(sp.serverLevel(), HX + 0.5, SY, HZ - 2.5, 0.0F, 45.0F);
            net.minecraft.world.item.ItemStack kit = com.mundosdeaxelis.npc.NpcService.kit("ruda");
            useOn(sp, kit, new net.minecraft.core.BlockPos(HX, SY - 1, HZ), net.minecraft.core.Direction.UP);
        }));
        add("casa (comprobar)", 300, () -> {
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
                npcScreen().buy(0); // 3 bayas del santuario por 15
            }
        });
        add("tienda (comprar brújula)", 1500, () -> {
            check("la tienda cobra Esencia (" + essBefore + " -> " + TableClient.essence + ") y da las bayas",
                    TableClient.essence == essBefore - 15 && invCount(com.mundosdeaxelis.registry.ModItems.SANCTUARY_BERRY.get()) >= 3);
            if (npcScreen() != null) {
                npcScreen().buy(7); // brújula al santuario
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
            check("la brújula de Ruda apunta a un santuario", ok);
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
            clearMobs();
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
        // etapa 256: el Santuario del Bosque (la primera estructura) y que las estructuras no se pueden picar
        add("santuario", 6000, () -> {
            open(null);
            clearMobs();
            cmd("gamemode spectator @a");
            cmd("execute in minecraft:overworld positioned " + SX + " " + SY + " " + (SZ0 - 16) + " rotated 0 0 run mundos estructura bosque");
        });
        add("santuario de frente", 3000, () -> lookS(0, 6, -12, 0, 3, 22));
        add("santuario de frente (foto)", 300, () -> shot("46_santuario_frente"));
        add("santuario de lado", 2500, () -> lookS(24, 16, 2, 0, 2, 24));
        add("santuario de lado (foto)", 300, () -> shot("47_santuario_lado"));
        add("santuario dentro", 2500, () -> lookS(0.5, 0, 20.5, 0.5, 1, 30.5));
        add("santuario dentro (foto)", 300, () -> {
            net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(SX, SY + 1, SZ0 + 30);
            String id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(Minecraft.getInstance().level.getBlockState(p).getBlock()).getPath();
            check("la Piedra de Portal: Naturaleza está en el altar del santuario (" + id + ")", id.equals("portal_stone_forest"));
            check("enemigos del santuario a la vista (" + clientCount(com.mundosdeaxelis.entity.Brote.class) + " brotes, "
                    + clientCount(com.mundosdeaxelis.entity.Raizal.class) + " raizal)", clientCount(com.mundosdeaxelis.entity.Brote.class) >= 2
                    && clientCount(com.mundosdeaxelis.entity.Raizal.class) >= 1);
            shot("48_santuario_dentro");
        });
        add("celda", 2500, () -> lookS(4.5, 0, 23.5, 11, 1, 24.5));
        add("celda (foto)", 300, () -> shot("49_santuario_celda"));
        add("no se puede picar", 1500, () -> {
            cmd("gamemode survival @a");
            onServer(sp -> {
                // una pared del santuario y la Piedra de Portal: romper como un jugador en supervivencia no debe funcionar
                net.minecraft.core.BlockPos wall = new net.minecraft.core.BlockPos(SX - 8, SY + 1, SZ0 + 27);
                net.minecraft.core.BlockPos stone = new net.minecraft.core.BlockPos(SX, SY + 1, SZ0 + 30);
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
            cmd("mundos vecinos reiniciar");
            cmd("execute in minecraft:overworld positioned " + SX + " " + SY + " " + (SZ0 - 16) + " run mundos estructura deshacer");
            cmd("kill @e[type=item]");
        });
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
