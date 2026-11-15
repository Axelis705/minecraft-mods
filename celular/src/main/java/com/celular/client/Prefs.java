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
        try {
            Files.createDirectories(file().getParent());
            try (Writer w = Files.newBufferedWriter(file(), StandardCharsets.UTF_8)) {
                p.store(w, "Ajustes del celular");
            }
        } catch (IOException ignored) {
        }
    }
}
