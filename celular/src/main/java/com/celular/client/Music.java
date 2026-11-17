package com.celular.client;

import com.celular.music.MusicPlayer;
import com.celular.music.Track;
import com.celular.PhoneItem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.Locale;

/** Une el reproductor (que no sabe nada de Minecraft) con el juego: carpeta, volumen, pausa, avisos y la música del juego. */
public final class Music {
    private static volatile float gameVolume = 1;
    private static long lastMute;
    private static boolean ready;
    /** Distancia a la que el celular tirado deja de oírse. */
    private static final double HEAR = 24.0;
    /** El celular tirado que hace de bocina: su entidad y el último sitio donde se vio (null = el celular está contigo). */
    private static int speakerId = -1;
    private static Vec3 speakerPos;
    /** Desde cuándo no se encuentra el celular (0 = sí se encuentra). */
    private static long lostSince;

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
        speaker(mc);
        Radio.frame(mc);
        Flash.frame(mc);
        Notes.frame(mc);
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

    /** ¿Tiene el jugador un celular (en el inventario o agarrado con el ratón)? */
    private static boolean holdsPhone(Minecraft mc) {
        return ClientHooks.hasPhone() || mc.player.containerMenu.getCarried().getItem() instanceof PhoneItem;
    }

    private static ItemEntity phoneNear(Minecraft mc, Vec3 at, double r) {
        ItemEntity best = null;
        double bd = r * r;
        for (ItemEntity e : mc.level.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(r))) {
            double d = e.distanceToSqr(at);
            if (e.isAlive() && e.getItem().getItem() instanceof PhoneItem && d <= bd) {
                best = e;
                bd = d;
            }
        }
        return best;
    }

    /**
     * Modo bocina: si el celular está tirado en el suelo, la música sale desde ahí (más bajo con la distancia y hacia el lado en que
     * queda). Si el celular desaparece cerca de ti (alguien lo recogió, se quemó o lo guardaste en un cofre), la música se pausa.
     */
    private static void speaker(Minecraft mc) {
        if (mc.player == null || mc.level == null || holdsPhone(mc)) {
            speakerId = -1;
            speakerPos = null;
            lostSince = 0;
            MusicPlayer.spatialL = 1F;
            MusicPlayer.spatialR = 1F;
            return;
        }
        long now = System.currentTimeMillis();
        net.minecraft.world.entity.Entity e = speakerId >= 0 ? mc.level.getEntity(speakerId) : null;
        ItemEntity phone = e instanceof ItemEntity it && it.isAlive() && it.getItem().getItem() instanceof PhoneItem ? it : null;
        if (phone == null) {
            phone = phoneNear(mc, speakerPos != null ? speakerPos : mc.player.position(), 6.0);
        }
        if (phone != null) {
            speakerId = phone.getId();
            speakerPos = phone.position();
            lostSince = 0;
        } else {
            if (lostSince == 0) {
                lostSince = now;
            }
            boolean near = speakerPos == null || speakerPos.distanceTo(mc.player.position()) < 20.0;
            if (near && now - lostSince > 1500) {
                // el celular ya no está (en un cofre, lo recogió otro, se quemó...): se pausa
                if (Radio.inJam()) {
                    Radio.jamLeave();                      // sin celular no hay Jam
                    com.celular.plat.Plat.msg(mc.player, Component.translatable("celular.music.phone_gone"), true);
                } else if (MusicPlayer.playing) {
                    MusicPlayer.toggle();
                    com.celular.plat.Plat.msg(mc.player, Component.translatable("celular.music.phone_gone"), true);
                }
                speakerPos = null;
                speakerId = -1;
            }
            if (speakerPos == null) {
                // un momento de gracia (el celular recién tirado aún no aparece en el mundo): se oye normal
                MusicPlayer.spatialL = 1F;
                MusicPlayer.spatialR = 1F;
                return;
            }
        }
        float[] g = spatial(mc, speakerPos.add(0, 0.25, 0), HEAR);
        MusicPlayer.spatialL = g[0];
        MusicPlayer.spatialR = g[1];
    }

    /**
     * Volumen de cada oído para algo que suena en {@code pos}: más bajo con la distancia (se deja de oír a {@code hear} bloques) y más
     * fuerte del lado en que queda. Devuelve {izquierdo, derecho}.
     */
    static float[] spatial(Minecraft mc, Vec3 pos, double hear) {
        Vec3 ear = mc.player.getEyePosition();
        Vec3 to = pos.subtract(ear);
        double d = to.length();
        double att = d <= 2.0 ? 1.0 : Math.max(0.0, 1.0 - (d - 2.0) / (hear - 2.0));
        att = att * att;
        // hacia qué lado queda: derecha positiva
        double yaw = Math.toRadians(mc.player.getYRot());
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        double flat = Math.hypot(to.x, to.z);
        double pan = flat < 0.3 ? 0.0 : (to.x * rx + to.z * rz) / flat;
        return new float[]{(float) (att * (1.0 - 0.65 * Math.max(0.0, pan))), (float) (att * (1.0 - 0.65 * Math.max(0.0, -pan)))};
    }

    /** Desde qué entidad sale mi música para los demás: el celular tirado si lo está, si no yo mismo. */
    static int speakerSource(Minecraft mc) {
        return speakerId >= 0 && speakerPos != null ? speakerId : mc.player.getId();
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
