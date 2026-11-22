package com.celular.client;

import com.celular.Celular;
import com.celular.CelularReg;
import com.celular.PhoneUse;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Prueba con el juego de verdad, solo para el CI (workflow "Prueba con el cliente"): se activa con {@code -Dcelular.autotest=true}.
 * Sirve en las 4 versiones. Entra a un mundo plano, toma capturas del celular y sus apps (en 1.20.1 también en otros tamaños), del celular en la mano, de las poses
 * que ven los demás (con otro jugador de mentira, desde varios lados), de los audífonos, la bocina y la linterna de noche, y cierra
 * el juego. Así se ve cómo queda todo sin abrir el juego a mano. Sin esa opción no hace nada.
 */
public final class AutoTest {
    public static final boolean ON = Boolean.getBoolean("celular.autotest");
    /** Prueba de dos jugadores en un servidor: "A" o "B" (ver {@link AutoTestMulti}). */
    static final String ROLE = System.getProperty("celular.autotest.rol", "");

    private AutoTest() {
    }

    /** Lo llama {@link Ticker} cada 50 ms en el hilo del juego. */
    static void tick() {
        if (!ROLE.isEmpty()) {
            AutoTestMulti.tick();
        } else if (ON && AutoVideo.ON) {
            AutoVideo.tick();
        } else if (ON) {
            tickRun();
        }
    }

    private record Step(String name, long pause, Runnable run) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int idx;
    private static long next;
    private static long joinedAt;
    static int shots, fails;
    static RemotePlayer fake;

    /** El otro jugador está en (0.5, -60, 0.5) mirando al norte; desde aquí se le toman las fotos. */
    static final double FX = 0.5, FZ = 0.5, GROUND = -60;

    static void log(String s) {
        Celular.LOGGER.info("AUTOTEST " + s);
    }

    private static void tickRun() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (joinedAt == 0) {
            joinedAt = now;
            next = now + 10000;           // que carguen los chunks
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
        } catch (Throwable e) {
            fails++;
            log("FALLO en " + s.name + ": " + e);
            Celular.LOGGER.error("AUTOTEST", e);
        }
        next = System.currentTimeMillis() + s.pause;
    }

    /** Una comprobación: si falla, cuenta como fallo de la prueba (y queda en el registro). */
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        log("ok: " + what);
    }

    private static void add(String name, long wait, Runnable r) {
        STEPS.add(new Step(name, wait, r));
    }

    static void cmd(String c) {
        IntegratedServer s = Minecraft.getInstance().getSingleplayerServer();
        s.execute(() -> s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), c));
    }

    /** Guarda lo que se ve en {@code screenshots/celular_<nombre>.png} (con Tex.screenshot, que sirve en todas las versiones). */
    static void shot(String name) {
        Tex.screenshot(img -> {
            try (NativeImage n = img) {
                int w = n.getWidth(), h = n.getHeight();
                BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        bi.setRGB(x, y, Tex.argb(n, x, y) & 0xFFFFFF);
                    }
                }
                Path dir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("screenshots");
                Files.createDirectories(dir);
                javax.imageio.ImageIO.write(bi, "png", dir.resolve("celular_" + name + ".png").toFile());
            } catch (Exception e) {
                fails++;
                log("FALLO foto " + name + ": " + e);
            }
        });
        shots++;
        log("foto " + name);
    }

    /** Una foto: primero prepara, espera a que se dibuje y luego la toma. */
    private static void photo(String name, long wait, Runnable setup) {
        add(name, wait, setup);
        add(name + " (foto)", 250, () -> shot(name));
    }

    /** Qué pantalla muestra mi celular (como si yo lo estuviera usando), puesto desde el servidor. */
    static void myScreen(int id) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer s = mc.getSingleplayerServer();
        UUID u = mc.player.getUUID();
        s.execute(() -> {
            ServerPlayer sp = s.getPlayerList().getPlayer(u);
            if (sp != null) {
                PhoneUse.set(sp, id);
            }
        });
    }

    /** Me pongo en (x, z) mirando hacia el punto (tx, ty, tz). */
    static void lookFrom(double x, double z, double tx, double ty, double tz) {
        double ey = GROUND + 1.62;
        double dx = tx - x, dy = ty - ey, dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.hypot(dx, dz)));
        cmd(String.format(Locale.ROOT, "tp @a %.3f %.1f %.3f %.2f %.2f", x, GROUND, z, yaw, pitch));
    }

    static void ui(boolean hide) {
        //#if MC < 26.1
        Minecraft.getInstance().options.hideGui = hide;
        //#elif MC >= 26.3
        //$$ CameraScreen.hud(hide);
        //#endif
    }

    static void close() {
        ClientHooks.open(null);
    }

    //#if MC < 1.20.2
    private static void guiScale(int s) {
        Minecraft mc = Minecraft.getInstance();
        mc.options.guiScale().set(s);
        mc.resizeDisplay();
    }
    //#endif

    static void makeFake() {
        Minecraft mc = Minecraft.getInstance();
        fake = new RemotePlayer(mc.level, new GameProfile(UUID.fromString("3c1f5e2a-1111-4000-8000-00000000abcd"), "Odette"));
        //#if MC >= 1.21.5
        //$$ fake.snapTo(FX, GROUND, FZ, 180F, 15F);
        //#else
        fake.moveTo(FX, GROUND, FZ, 180F, 15F);
        //#endif
        fake.yRotO = 180F;
        fake.xRotO = 15F;
        fake.yBodyRot = fake.yBodyRotO = 180F;
        fake.yHeadRot = fake.yHeadRotO = 180F;
        fake.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CelularReg.PHONES.get(2).get()));
        fake.setId(1_999_999_001);       // en 26.x hay que darle un número antes de agregarlo (y no choca con los del servidor)
        //#if MC >= 1.20.2
        //$$ mc.level.addEntity(fake);
        //#else
        mc.level.addPlayer(fake.getId(), fake);
        //#endif
    }

    /** Pone en el celular del otro jugador lo que está usando (así lo vería cualquiera en un servidor). */
    static void fakeUses(int id, float pitch) {
        PhoneUse.put(fake.getMainHandItem(), id);
        fake.setXRot(pitch);
        fake.xRotO = pitch;
    }

    /** Las cámaras alrededor del otro jugador: {nombre, x, z}. */
    private static final Object[][] CAMS = {
            {"frente", FX, FZ - 2.2},
            {"derecha", FX + 2.2, FZ},
            {"34", FX + 1.56, FZ - 1.56},
            {"izquierda", FX - 2.2, FZ},
    };

    private static void poseShots(String pose, int id, float pitch, int cams) {
        for (int c = 0; c < cams; c++) {
            Object[] cam = CAMS[c];
            photo("4_pose_" + pose + "_" + cam[0], 1200, () -> {
                fakeUses(id, pitch);
                lookFrom((double) cam[1], (double) cam[2], FX, GROUND + 1.15, FZ);
            });
        }
    }

    private static final String[] APPS = {"insta", "gallery", "phone", "patreon", "settings", "music", "weather", "compass", "calc", "flash",
            "messages", "notes", "map", "wallet"};

    private static void build() {
        Minecraft mc = Minecraft.getInstance();
        add("reglas", 600, () -> {
            cmd("gamerule sendCommandFeedback false");
            cmd("gamerule logAdminCommands false");
        });
        add("mundo", 3000, () -> {
            cmd("gamemode creative @a");
            cmd("time set 6000");
            cmd("gamerule doDaylightCycle false");
            cmd("weather clear 999999");
            cmd("gamerule doWeatherCycle false");
            cmd("gamerule randomTickSpeed 0");
            cmd("gamerule doMobSpawning false");
            cmd("kill @e[type=!player]");
            cmd("clear @a");
            cmd("item replace entity @a hotbar.0 with celular:phone");
            cmd("item replace entity @a hotbar.1 with celular:speaker");
            cmd("item replace entity @a hotbar.2 with celular:headphones");
            cmd("setblock 6 -60 -6 celular:speaker[facing=south,on=true]");
            cmd("setblock 8 -60 -6 celular:speaker[facing=south,on=false]");
            cmd("setblock 7 -60 -7 minecraft:oak_planks");
            cmd("tp @a 0.5 -60 6.5 180 0");
            ClientHooks.select(mc.player.getInventory(), 0);
            Prefs.load();
            Prefs.lockScreen = true;
            ui(false);
        });

        // ---- el celular abierto: bloqueo, inicio y cada app (pantalla de 240 de alto: la más chica que da el juego)
        photo("1_bloqueo", 1800, () -> ClientHooks.openPhone(0));
        photo("1_inicio", 1800, () -> {
            close();
            Prefs.lockScreen = false;
            ClientHooks.openPhone(0);
        });
        photo("1_inicio_cancion", 2500, () -> {
            close();
            com.celular.music.MusicPlayer.volume = 0F;
            com.celular.music.MusicPlayer.scan();
            if (com.celular.music.MusicPlayer.library.isEmpty()) {
                log("no hay canciones en la carpeta de música");
            } else {
                com.celular.music.MusicPlayer.playList(new ArrayList<>(com.celular.music.MusicPlayer.library), 0);
            }
            ClientHooks.openPhone(0);
        });
        for (String a : APPS) {
            photo("2_app_" + a, a.equals("map") ? 3500 : 1800, () -> ClientHooks.open(new PhoneScreen(0, a)));
        }
        photo("2_app_camera", 2500, () -> ClientHooks.open(new CameraScreen(0)));
        add("cerrar", 800, AutoTest::close);

        // ---- el celular en la mano (primera persona)
        photo("3_mano_bloqueo", 1500, () -> {
            close();
            myScreen(PhoneUse.OFF);
        });
        photo("3_mano_inicio", 1500, () -> myScreen(PhoneUse.HOME));
        photo("3_mano_musica", 1500, () -> myScreen(PhoneUse.MUSIC));
        add("apagar", 500, () -> myScreen(PhoneUse.OFF));

        // ---- lo que ven los demás: otro jugador usando el celular, desde varios lados
        add("otro jugador", 1500, () -> {
            ui(true);
            makeFake();
        });
        poseShots("texto", PhoneUse.HOME, 15F, 4);
        poseShots("oreja", PhoneUse.EAR, 0F, 4);
        photo("4_pose_oreja_mira_abajo", 1200, () -> {
            fakeUses(PhoneUse.EAR, 35F);
            lookFrom(FX + 1.56, FZ - 1.56, FX, GROUND + 1.15, FZ);
        });
        photo("4_pose_oreja_gira_cabeza", 1200, () -> {
            fakeUses(PhoneUse.EAR, 0F);
            fake.yHeadRot = fake.yHeadRotO = 180F + 45F;
            lookFrom(FX + 1.56, FZ - 1.56, FX, GROUND + 1.15, FZ);
        });
        add("cabeza derecha", 200, () -> fake.yHeadRot = fake.yHeadRotO = 180F);
        poseShots("foto", PhoneUse.CAMERA, 0F, 3);
        poseShots("linterna", PhoneUse.FLASH, 10F, 3);
        add("audifonos", 300, () -> fake.setItemSlot(EquipmentSlot.HEAD, new ItemStack(CelularReg.HEADPHONES.get())));
        poseShots("audifonos", PhoneUse.MUSIC, 15F, 3);
        add("sin audifonos", 300, () -> fake.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY));

        // ---- letras de la canción (tono.lrc está junto a tono.mp3): las mías alrededor y las de otro jugador
        add("letras (preparar)", 1500, () -> {
            ui(false);
            fakeUses(PhoneUse.OFF, 0F);
            Prefs.lyricsNet = false;
            Prefs.lyricsMine = Prefs.lyricsOthers = Prefs.lyricsSubs = true;
            if (com.celular.music.MusicPlayer.current == null && !com.celular.music.MusicPlayer.library.isEmpty()) {
                com.celular.music.MusicPlayer.playList(new ArrayList<>(com.celular.music.MusicPlayer.library), 0);
            }
            LetrasHud.debugSeg = 4.8;
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            cmd("tp @a 0.5 -60 6.5 180 0");
        });
        add("letras (comprobar)", 400, () -> {
            check(LetrasHud.estadoMia().equals("lista"), "la letra de tono.mp3 se lee del .lrc de al lado (" + LetrasHud.estadoMia() + ")");
            check(LetrasHud.palabras > 0 && LetrasHud.linea.equals("Letras que flotan a tu alrededor"),
                    "las letras flotan y el subtítulo va en la línea correcta (" + LetrasHud.palabras + ", " + LetrasHud.linea + ")");
            int sw = mc.getWindow().getGuiScaledWidth(), sh = mc.getWindow().getGuiScaledHeight();
            float[] c = LetrasHud.probarCentro(mc, sw, sh);
            check(c != null && Math.abs(c[0] - sw / 2F) < 2 && Math.abs(c[1] - sh / 2F) < 2 && Math.abs(c[2] - sw / 2F) < 3 && Math.abs(c[3] - sh / 2F) < 3,
                    "lo que está delante de la cámara cae en el centro de la pantalla (" + (c == null ? "null" : c[0] + "," + c[1] + " / " + c[2] + "," + c[3]) + ")");
        });
        photo("9_letras_primera_persona", 300, () -> { });
        photo("9_letras_tercera_persona", 1200, () -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
        photo("9_letras_de_frente", 1200, () -> mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
        add("letras (de otro)", 1200, () -> {
            mc.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            Prefs.lyricsMine = false;
            LetrasHud.debugSeg = -1;
            LetrasHud.debugOtro = fake;
            LetrasHud.debugLetraOtro = com.celular.music.Letra.lrc("[00:00.30]Hola mundo de bloques\n[00:01.20]Suena el celular\n[00:02.10]Letras que flotan a tu alrededor\n");
            LetrasHud.debugSegOtro = 4.8;
            lookFrom(FX + 2.8, FZ - 2.8, FX, GROUND + 1.3, FZ);
        });
        add("letras (de otro, comprobar)", 300, () -> check(LetrasHud.palabras > 0, "letras alrededor del otro jugador (" + LetrasHud.palabras + ")"));
        photo("9_letras_de_otro", 200, () -> { });
        add("letras (fin)", 300, () -> {
            LetrasHud.debugOtro = null;
            LetrasHud.debugLetraOtro = null;
            Prefs.lyricsMine = true;
            Prefs.lyricsNet = true;
            ui(true);
        });

        // ---- la bocina (encendida y apagada)
        photo("5_bocinas", 2000, () -> lookFrom(7.5, -3.6, 7.5, GROUND + 0.5, -6.5));
        photo("5_bocina_cerca", 1500, () -> lookFrom(6.5, -4.4, 6.5, GROUND + 0.45, -6.5));

        // ---- de noche: la linterna (la mía en la mano y la del otro jugador)
        photo("6_noche_linterna_otro", 3000, () -> {
            cmd("time set 18000");
            fakeUses(PhoneUse.FLASH, 20F);
            lookFrom(FX + 3.5, FZ + 2.5, FX, GROUND + 1.0, FZ - 2);
        });
        photo("6_noche_mi_linterna", 3000, () -> {
            fakeUses(PhoneUse.OFF, 0F);
            ui(false);
            cmd("tp @a 0.5 -60 8.5 180 25");
            myScreen(PhoneUse.FLASH);
        });
        add("de día", 1000, () -> {
            myScreen(PhoneUse.OFF);
            cmd("time set 6000");
        });

        // ---- otros tamaños de pantalla (solo en 1.20.1: el acomodo es el mismo código en todas las versiones)
        //#if MC < 1.20.2
        add("escala 2", 1500, () -> guiScale(2));
        photo("7_g360_inicio", 1800, () -> ClientHooks.openPhone(0));
        photo("7_g360_musica", 1800, () -> ClientHooks.open(new PhoneScreen(0, "music")));
        photo("7_g360_mensajes", 1800, () -> ClientHooks.open(new PhoneScreen(0, "messages")));
        add("1080p", 2500, () -> {
            close();
            mc.getWindow().setWindowed(1920, 1080);
        });
        add("escala 4", 1500, () -> guiScale(4));     // después de que la ventana ya cambió (si no, la escala 4 no se permite)
        photo("8_g270_inicio", 2000, () -> ClientHooks.openPhone(0));
        photo("8_g270_musica", 1800, () -> ClientHooks.open(new PhoneScreen(0, "music")));
        photo("8_g270_ajustes", 1800, () -> ClientHooks.open(new PhoneScreen(0, "settings")));

        //#endif

        add("fin", 4000, () -> {
            close();
            com.celular.music.MusicPlayer.stop();
            log("AUTOTEST_FIN fotos=" + shots + " fallos=" + fails);
        });
        add("salir", 1000, () -> Minecraft.getInstance().stop());
    }
}
