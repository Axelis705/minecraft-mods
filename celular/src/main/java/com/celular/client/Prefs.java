package com.celular.client;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Los ajustes del celular de este jugador (se guardan en .minecraft/celular/ajustes.properties). */
public final class Prefs {
    /** Fondo de pantalla: 0..N-1 los que trae el celular; -1 una foto de la galería. */
    public static int wallpaper = 0;
    public static String wallpaperPhoto = "";
    public static boolean notifications = true;
    public static boolean dark = true;
    public static boolean shutterSound = true;
    public static boolean grid = true;
    /** Música: volumen (0..1), aleatorio, repetir (0 no, 1 lista, 2 canción), callar la música de Minecraft, aviso al cambiar. */
    public static float musicVolume = 0.8F;
    public static boolean shuffle;
    public static int repeat;
    public static boolean muteGameMusic = true;
    public static boolean musicToasts = true;
    /** Pantalla de bloqueo al sacar el celular. */
    public static boolean lockScreen = true;
    /** Altavoz: lo que tocas del mundo lo oyen los que están cerca. */
    public static boolean speaker = true;
    /** Volumen de los altavoces de los demás (0..1). */
    public static float othersVolume = 0.8F;
    /** Mapa con amigos: los demás ven dónde estoy. */
    public static boolean shareLocation = true;
    /** Oír la música que ponen otros con su altavoz. */
    public static boolean hearOthers = true;
    /** Notas musicales flotando sobre quien pone música. */
    public static boolean partyNotes = true;
    /** Letras de las canciones (etapa de letras): las mías, las de los demás que oigo, el subtítulo de abajo y buscarlas en internet. */
    public static boolean lyricsMine = true, lyricsOthers = true, lyricsSubs = true, lyricsNet = true;
    /** Avisos de mensajes nuevos. */
    public static boolean msgToasts = true;
    private static boolean loaded;

    private Prefs() {
    }

    private static Path file() {
        return FMLPaths.GAMEDIR.get().resolve("celular").resolve("ajustes.properties");
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) {
            return;
        }
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
            p.load(r);
            wallpaper = Integer.parseInt(p.getProperty("fondo", "0"));
            wallpaperPhoto = p.getProperty("fondo_foto", "");
            notifications = Boolean.parseBoolean(p.getProperty("notificaciones", "true"));
            dark = Boolean.parseBoolean(p.getProperty("modo_oscuro", "true"));
            shutterSound = Boolean.parseBoolean(p.getProperty("sonido_camara", "true"));
            grid = Boolean.parseBoolean(p.getProperty("cuadricula", "true"));
            musicVolume = Math.max(0, Math.min(1, Float.parseFloat(p.getProperty("musica_volumen", "0.8"))));
            shuffle = Boolean.parseBoolean(p.getProperty("musica_aleatorio", "false"));
            repeat = Math.floorMod(Integer.parseInt(p.getProperty("musica_repetir", "0")), 3);
            muteGameMusic = Boolean.parseBoolean(p.getProperty("musica_callar_minecraft", "true"));
            musicToasts = Boolean.parseBoolean(p.getProperty("musica_avisos", "true"));
            lockScreen = Boolean.parseBoolean(p.getProperty("pantalla_bloqueo", "true"));
            speaker = Boolean.parseBoolean(p.getProperty("musica_altavoz", "true"));
            othersVolume = Math.max(0, Math.min(1, Float.parseFloat(p.getProperty("musica_volumen_otros", "0.8"))));
            shareLocation = Boolean.parseBoolean(p.getProperty("compartir_ubicacion", "true"));
            hearOthers = Boolean.parseBoolean(p.getProperty("oir_altavoces", "true"));
            partyNotes = Boolean.parseBoolean(p.getProperty("notas_fiesta", "true"));
            lyricsMine = Boolean.parseBoolean(p.getProperty("letras_mias", "true"));
            lyricsOthers = Boolean.parseBoolean(p.getProperty("letras_otros", "true"));
            lyricsSubs = Boolean.parseBoolean(p.getProperty("letras_subtitulo", "true"));
            lyricsNet = Boolean.parseBoolean(p.getProperty("letras_internet", "true"));
            msgToasts = Boolean.parseBoolean(p.getProperty("avisos_mensajes", "true"));
        } catch (Exception ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("fondo", Integer.toString(wallpaper));
        p.setProperty("fondo_foto", wallpaperPhoto);
        p.setProperty("notificaciones", Boolean.toString(notifications));
        p.setProperty("modo_oscuro", Boolean.toString(dark));
        p.setProperty("sonido_camara", Boolean.toString(shutterSound));
        p.setProperty("cuadricula", Boolean.toString(grid));
        p.setProperty("musica_volumen", Float.toString(musicVolume));
        p.setProperty("musica_aleatorio", Boolean.toString(shuffle));
        p.setProperty("musica_repetir", Integer.toString(repeat));
        p.setProperty("musica_callar_minecraft", Boolean.toString(muteGameMusic));
        p.setProperty("musica_avisos", Boolean.toString(musicToasts));
        p.setProperty("pantalla_bloqueo", Boolean.toString(lockScreen));
        p.setProperty("musica_altavoz", Boolean.toString(speaker));
        p.setProperty("musica_volumen_otros", Float.toString(othersVolume));
        p.setProperty("compartir_ubicacion", Boolean.toString(shareLocation));
        p.setProperty("oir_altavoces", Boolean.toString(hearOthers));
        p.setProperty("notas_fiesta", Boolean.toString(partyNotes));
        p.setProperty("letras_mias", Boolean.toString(lyricsMine));
        p.setProperty("letras_otros", Boolean.toString(lyricsOthers));
        p.setProperty("letras_subtitulo", Boolean.toString(lyricsSubs));
        p.setProperty("letras_internet", Boolean.toString(lyricsNet));
        p.setProperty("avisos_mensajes", Boolean.toString(msgToasts));
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(w, "Ajustes del celular");
            }
        } catch (IOException ignored) {
        }
    }
}
