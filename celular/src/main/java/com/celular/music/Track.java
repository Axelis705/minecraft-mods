package com.celular.music;

import java.nio.file.Path;

/** Una canción de la carpeta de música: archivo, título, artista y duración (se calcula aparte, -1 mientras no se sabe). */
public final class Track {
    public final Path path;
    public final String title;
    public final String artist;
    /** "mp3", "ogg" o "wav". */
    public final String ext;
    public final long modified;
    public volatile int seconds = -1;
    /** Si no se pudo leer (formato raro o archivo dañado). */
    public volatile boolean broken;
    /** Si es una canción subida al mundo: su número en el servidor (0 = solo está en esta PC). */
    public volatile long worldId;
    /** SHA-1 del archivo (se calcula al subirla o al reconocerla en el mundo; "" = aún no). */
    public volatile String hash = "";

    public Track(Path path, String title, String artist, String ext, long modified) {
        this.path = path;
        this.title = title;
        this.artist = artist;
        this.ext = ext;
        this.modified = modified;
    }

    /** Un número fijo por canción (para el color de su portada). */
    public int hue() {
        return Math.floorMod((title + "|" + artist).hashCode(), 360);
    }

    public String key() {
        return path.getFileName().toString();
    }
}
