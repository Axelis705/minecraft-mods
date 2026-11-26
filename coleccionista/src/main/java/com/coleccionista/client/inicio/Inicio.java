package com.coleccionista.client.inicio;

import com.axeliscore.test.ClientTest;
import com.coleccionista.Coleccionista;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * (Cliente) La entrada del evento: en lugar del menú de Minecraft, la pantalla del Coleccionista ({@link InicioScreen}: Jugar, Configurar,
 * Salir), su pausa sencilla ({@link PausaScreen}) y la interfaz siempre en tamaño 3.
 * <p>
 * Se configura con {@code config/coleccionista-inicio.properties} (lo escribe el launcher): {@code servidor} (la dirección a la que entra
 * "Jugar"), {@code nombre} y {@code activo} (false = el menú de siempre). El organizador tiene un atajo escondido, <b>Ctrl + Shift + O</b>
 * en cualquier menú: cambia entre los menús del evento y los de Minecraft (para entrar a un mundo, ver los mods, etc.).
 */
public final class Inicio {
    private static final String MOD = Coleccionista.MODID;
    public static final int ESCALA = 3;
    private static final Path ARCHIVO = FMLPaths.CONFIGDIR.get().resolve("coleccionista-inicio.properties");

    public static String servidor = "", nombre = "El Coleccionista";
    public static boolean activo = true;
    /** Con el atajo del organizador: los menús de Minecraft de siempre (hasta que lo vuelva a apretar). */
    public static boolean organizador;
    /** Ya se prendió la "tele" una vez (la estática y el saludo solo la primera vez que se abre en esta sesión). */
    static boolean saludo;

    private Inicio() {
    }

    public static void init() {
        leer();
        MinecraftForge.EVENT_BUS.addListener(Inicio::abriendo);
        MinecraftForge.EVENT_BUS.addListener(Inicio::tecla);
        MinecraftForge.EVENT_BUS.addListener(Inicio::tick);
    }

    /** ¿Se usan los menús del evento? (no en la prueba automática con fotos, que entra sola a su mundo). */
    public static boolean usa() {
        return activo && !organizador && !ClientTest.enabled();
    }

    private static void abriendo(ScreenEvent.Opening e) {
        if (!usa()) {
            return;
        }
        Screen s = e.getNewScreen();
        if (s instanceof TitleScreen) {
            e.setNewScreen(new InicioScreen());
        } else if (s instanceof PauseScreen && !InputConstantsF3.abajo()) {
            // (con F3 + Esc se pausa sin menú: eso se respeta)
            e.setNewScreen(new PausaScreen());
        }
    }

    /** El atajo escondido del organizador: Ctrl + Shift + O en cualquier menú. */
    private static void tecla(ScreenEvent.KeyPressed.Pre e) {
        if (e.getKeyCode() != GLFW.GLFW_KEY_O || !Screen.hasControlDown() || !Screen.hasShiftDown() || !activo) {
            return;
        }
        e.setCanceled(true);
        organizador = !organizador;
        Minecraft mc = Minecraft.getInstance();
        Coleccionista.LOGGER.info("COLECCIONISTA_INICIO organizador={}", organizador);
        if (mc.level == null) {
            mc.setScreen(organizador ? new TitleScreen() : new InicioScreen());
        } else {
            mc.setScreen(organizador ? new PauseScreen(true) : new PausaScreen());
        }
    }

    /** La interfaz siempre en 3 (para todos igual). */
    private static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !usa()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.guiScale().get() != ESCALA) {
            mc.options.guiScale().set(ESCALA);
            mc.options.save();
            mc.resizeDisplay();
            Coleccionista.LOGGER.info("COLECCIONISTA_INICIO escala {}", ESCALA);
        }
    }

    // ------------------------------------------------------------------ el archivo de configuración

    public static void leer() {
        Properties p = new Properties();
        if (Files.exists(ARCHIVO)) {
            try (InputStream in = Files.newInputStream(ARCHIVO)) {
                p.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            } catch (IOException ex) {
                Coleccionista.LOGGER.warn("No pude leer {}: {}", ARCHIVO, ex.toString());
            }
        }
        servidor = p.getProperty("servidor", "").trim();
        nombre = p.getProperty("nombre", "El Coleccionista").trim();
        activo = !"false".equalsIgnoreCase(p.getProperty("activo", "true").trim());
        if (!Files.exists(ARCHIVO)) {
            guardar();
        }
    }

    public static void guardar() {
        Properties p = new Properties();
        p.setProperty("servidor", servidor);
        p.setProperty("nombre", nombre);
        p.setProperty("activo", String.valueOf(activo));
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (OutputStream out = Files.newOutputStream(ARCHIVO)) {
                p.store(new java.io.OutputStreamWriter(out, StandardCharsets.UTF_8),
                        " La pantalla de inicio del Coleccionista (la escribe el launcher). servidor = la direccion a la que entra Jugar;"
                                + " activo = false para usar el menu de siempre.");
            }
        } catch (IOException ex) {
            Coleccionista.LOGGER.warn("No pude guardar {}: {}", ARCHIVO, ex.toString());
        }
    }

    // ------------------------------------------------------------------ sonidos

    static SoundEvent evento(String id) {
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(MOD, id));
    }

    /** Un sonido de la interfaz (no depende de dónde estás). */
    static SoundInstance sonar(String id, SoundSource fuente, float vol, float tono) {
        SoundInstance s = new SimpleSoundInstance(new ResourceLocation(MOD, id), fuente, vol, tono, RandomSource.create(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true);
        Minecraft.getInstance().getSoundManager().play(s);
        return s;
    }

    /** La música del menú: el vals de la caja de música (se repite casi sin pausa). */
    static final Music MUSICA = new Music(Holder.direct(evento("menu.musica")), 20, 60, true);

    /** ¿Está apretada F3? (para no cambiar la pausa sin menú de F3 + Esc). */
    private static final class InputConstantsF3 {
        static boolean abajo() {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_F3);
        }
    }
}
