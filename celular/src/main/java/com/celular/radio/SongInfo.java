package com.celular.radio;

import net.minecraft.network.FriendlyByteBuf;

/** Una canción subida al mundo (la lista del servidor y lo que se manda a los celulares). */
public final class SongInfo {
    public long id;
    /** SHA-1 del archivo (40 letras): con esto se guarda en la caché del cliente y se reconoce si ya estaba. */
    public String hash = "";
    public String ext = "mp3";
    public String title = "";
    public String artist = "";
    public String uploader = "";
    public String uploaderId = "";
    public int size;
    public int seconds;
    public long added;

    public void write(FriendlyByteBuf b) {
        b.writeLong(id);
        b.writeUtf(hash, 40);
        b.writeUtf(ext, 4);
        b.writeUtf(title, 120);
        b.writeUtf(artist, 120);
        b.writeUtf(uploader, 40);
        b.writeUtf(uploaderId, 40);
        b.writeVarInt(size);
        b.writeVarInt(seconds);
        b.writeLong(added);
    }

    public static SongInfo read(FriendlyByteBuf b) {
        SongInfo s = new SongInfo();
        s.id = b.readLong();
        s.hash = b.readUtf(40);
        s.ext = b.readUtf(4);
        s.title = b.readUtf(120);
        s.artist = b.readUtf(120);
        s.uploader = b.readUtf(40);
        s.uploaderId = b.readUtf(40);
        s.size = b.readVarInt();
        s.seconds = b.readVarInt();
        s.added = b.readLong();
        return s;
    }

    public static String cut(String s, int max) {
        if (s == null) {
            return "";
        }
        s = s.replace('\n', ' ').replace('\r', ' ').trim();
        return s.length() > max ? s.substring(0, max) : s;
    }
}
