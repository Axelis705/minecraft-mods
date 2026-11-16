package com.celular.client;

import com.celular.music.MusicPlayer;
import com.celular.music.Track;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Locale;

/** Une el reproductor (que no sabe nada de Minecraft) con el juego: carpeta, volumen, pausa, avisos y la música del juego. */
public final class Music {
    private static volatile float gameVolume = 1;
    private static long lastMute;
    private static boolean ready;

    private Music() {
    }

    /** Se llama al abrir el celular o cada cuadro del HUD. */
    public static void init() {
        if (ready) {
            return;
        }
        ready = true;
        Prefs.load();
        MusicPlayer.volume = Prefs.musicVolume;
        MusicPlayer.shuffle = Prefs.shuffle;
        MusicPlayer.repeat = Prefs.repeat;
        MusicPlayer.host = new MusicPlayer.Host() {
            @Override
            public Path gameDir() {
                return FMLPaths.GAMEDIR.get();
            }

            @Override
            public float gameVolume() {
                return gameVolume;
            }

            @Override
            public boolean gamePaused() {
                return Minecraft.getInstance().isPaused();
            }
        };
    }

    /** Cada cuadro (desde el HUD y el celular): volumen del juego, callar la música de Minecraft y el aviso de canción nueva. */
    public static void frame() {
        init();
        Minecraft mc = Minecraft.getInstance();
        gameVolume = readGameVolume(mc);
        if (MusicPlayer.playing && Prefs.muteGameMusic) {
            long now = System.currentTimeMillis();
            if (now - lastMute > 1500) {
                lastMute = now;
                mc.getMusicManager().stopPlaying();
            }
        }
        Track t = MusicPlayer.announce;
        if (t != null) {
            MusicPlayer.announce = null;
            if (Prefs.musicToasts && !(ClientHooks.screen() instanceof PhoneScreen)) {
                PhoneHud.toastQuiet(D.APP_MUSIC, "celular.music.now_playing",
                        Component.literal(t.artist.isEmpty() ? t.title : t.title + " · " + t.artist));
            }
        }
        String err = MusicPlayer.error;
        if (!err.isEmpty() && mc.player != null) {
            MusicPlayer.error = "";
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable(err));
        }
    }

    private static float readGameVolume(Minecraft mc) {
        return mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MASTER)
                * mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.RECORDS);
    }

    /** Abre la carpeta de música en el explorador de archivos de la computadora. */
    public static void openFolder() {
        init();
        Path dir = MusicPlayer.ensureFolder().toAbsolutePath();
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] cmd;
        if (os.contains("win")) {
            cmd = new String[]{"explorer.exe", dir.toString()};
        } else if (os.contains("mac")) {
            cmd = new String[]{"open", dir.toString()};
        } else {
            cmd = new String[]{"xdg-open", dir.toString()};
        }
        try {
            new ProcessBuilder(cmd).start();
        } catch (Exception e) {
            PhoneHud.toastQuiet(D.APP_MUSIC, "celular.app.music", Component.translatable("celular.music.folder_error", dir.toString()));
        }
    }
}
