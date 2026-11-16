package com.celular.music;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Datos de una canción sin reproducirla: título y artista (etiquetas ID3 del MP3, comentarios del OGG o el nombre del archivo
 * "Artista - Título") y duración. Sin nada de Minecraft (se prueba aparte en {@code tests/MusicTest}).
 */
public final class Meta {
    private Meta() {
    }

    /** {título, artista} ("" si no hay artista). */
    public static String[] tags(Path p, String ext) {
        String[] t = null;
        try {
            if (ext.equals("mp3")) {
                t = id3(p);
            } else if (ext.equals("ogg")) {
                t = vorbisComments(p);
            }
        } catch (Exception ignored) {
        }
        String[] fromName = fromName(p.getFileName().toString());
        if (t == null || t[0] == null || t[0].isBlank()) {
            return fromName;
        }
        String artist = t[1] == null || t[1].isBlank() ? fromName[1] : t[1].trim();
        return new String[]{t[0].trim(), artist};
    }

    /** "Artista - Título.mp3" → {Título, Artista}; si no, el nombre sin extensión. */
    public static String[] fromName(String file) {
        int dot = file.lastIndexOf('.');
        String n = (dot > 0 ? file.substring(0, dot) : file).replace('_', ' ').trim();
        int dash = n.indexOf(" - ");
        if (dash > 0) {
            return new String[]{n.substring(dash + 3).trim(), n.substring(0, dash).trim()};
        }
        return new String[]{n, ""};
    }

    /** Extensión en minúsculas si es un formato que se puede tocar; si no, null. */
    public static String playable(String file) {
        int dot = file.lastIndexOf('.');
        if (dot < 0) {
            return null;
        }
        String e = file.substring(dot + 1).toLowerCase(Locale.ROOT);
        return e.equals("mp3") || e.equals("ogg") || e.equals("wav") ? e : null;
    }

    // ------------------------------------------------------------------ MP3

    /** Tamaño de la etiqueta ID3v2 del principio (0 si no tiene). */
    public static long id3Size(byte[] head) {
        if (head.length >= 10 && head[0] == 'I' && head[1] == 'D' && head[2] == '3') {
            long size = (head[6] & 0x7F) << 21 | (head[7] & 0x7F) << 14 | (head[8] & 0x7F) << 7 | (head[9] & 0x7F);
            boolean footer = (head[5] & 0x10) != 0;
            return 10 + size + (footer ? 10 : 0);
        }
        return 0;
    }

    private static String[] id3(Path p) throws IOException {
        byte[] head = new byte[10];
        try (InputStream in = Files.newInputStream(p)) {
            if (in.readNBytes(head, 0, 10) < 10) {
                return null;
            }
            long total = id3Size(head);
            if (total <= 10 || total > 8_000_000) {
                return id3v1(p);
            }
            int ver = head[3];
            byte[] tag = in.readNBytes((int) total - 10);
            String title = null, artist = null;
            int i = 0;
            int idLen = ver == 2 ? 3 : 4, hdr = ver == 2 ? 6 : 10;
            while (i + hdr <= tag.length) {
                String id = new String(tag, i, idLen, StandardCharsets.ISO_8859_1);
                if (id.charAt(0) == 0) {
                    break;
                }
                int size;
                if (ver == 2) {
                    size = (tag[i + 3] & 255) << 16 | (tag[i + 4] & 255) << 8 | (tag[i + 5] & 255);
                } else if (ver >= 4) {
                    size = (tag[i + 4] & 0x7F) << 21 | (tag[i + 5] & 0x7F) << 14 | (tag[i + 6] & 0x7F) << 7 | (tag[i + 7] & 0x7F);
                } else {
                    size = (tag[i + 4] & 255) << 24 | (tag[i + 5] & 255) << 16 | (tag[i + 6] & 255) << 8 | (tag[i + 7] & 255);
                }
                int start = i + hdr;
                if (size <= 0 || start + size > tag.length) {
                    break;
                }
                if (id.equals("TIT2") || id.equals("TT2")) {
                    title = text(tag, start, size);
                } else if (id.equals("TPE1") || id.equals("TP1")) {
                    artist = text(tag, start, size);
                }
                i = start + size;
            }
            if (title == null) {
                return id3v1(p);
            }
            return new String[]{title, artist};
        }
    }

    private static String text(byte[] b, int off, int len) {
        if (len < 1) {
            return null;
        }
        int enc = b[off];
        Charset cs = switch (enc) {
            case 1 -> StandardCharsets.UTF_16;
            case 2 -> StandardCharsets.UTF_16BE;
            case 3 -> StandardCharsets.UTF_8;
            default -> StandardCharsets.ISO_8859_1;
        };
        String s = new String(b, off + 1, len - 1, cs);
        int z = s.indexOf('\0');
        return (z >= 0 ? s.substring(0, z) : s).trim();
    }

    private static String[] id3v1(Path p) throws IOException {
        try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "r")) {
            if (f.length() < 128) {
                return null;
            }
            byte[] b = new byte[128];
            f.seek(f.length() - 128);
            f.readFully(b);
            if (b[0] != 'T' || b[1] != 'A' || b[2] != 'G') {
                return null;
            }
            return new String[]{clean(new String(b, 3, 30, StandardCharsets.ISO_8859_1)), clean(new String(b, 33, 30, StandardCharsets.ISO_8859_1))};
        }
    }

    private static String clean(String s) {
        int z = s.indexOf('\0');
        return (z >= 0 ? s.substring(0, z) : s).trim();
    }

    private static final int[][] BITRATES = {
            {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448},   // MPEG-1 capa I
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},      // MPEG-1 capa II
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320},       // MPEG-1 capa III
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256},      // MPEG-2 capa I
            {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160}};          // MPEG-2 capas II y III
    private static final int[] RATES = {44100, 48000, 32000};

    /**
     * Duración de un MP3 en segundos (contando los cuadros, sin decodificar) y si es MPEG-1 (el único que sabe tocar el decodificador).
     * Devuelve {segundos, 1 si es MPEG-1 / 0 si no}; {-1, 0} si no parece un MP3.
     */
    public static int[] mp3Info(Path p) throws IOException {
        byte[] all = Files.readAllBytes(p);
        int i = (int) Math.min(all.length, id3Size(all));
        long samples = 0;
        int rate = 0, frames = 0;
        boolean mpeg1 = true;
        while (i + 4 <= all.length) {
            int h = (all[i] & 255) << 24 | (all[i + 1] & 255) << 16 | (all[i + 2] & 255) << 8 | (all[i + 3] & 255);
            if ((h >>> 21) != 0x7FF) {
                i++;
                continue;
            }
            int ver = h >>> 19 & 3, layer = h >>> 17 & 3, bri = h >>> 12 & 15, sri = h >>> 10 & 3, pad = h >>> 9 & 1;
            if (ver == 1 || layer == 0 || bri == 0 || bri == 15 || sri == 3) {
                i++;
                continue;
            }
            boolean m1 = ver == 3;
            int sr = RATES[sri] >> (m1 ? 0 : ver == 2 ? 1 : 2);
            int lay = 4 - layer;                     // 1, 2 o 3
            int br = BITRATES[m1 ? lay - 1 : lay == 1 ? 3 : 4][bri] * 1000;
            int len, spf;
            if (lay == 1) {
                len = (12 * br / sr + pad) * 4;
                spf = 384;
            } else {
                spf = lay == 3 && !m1 ? 576 : 1152;
                len = spf / 8 * br / sr + pad;
            }
            if (len < 4) {
                i++;
                continue;
            }
            if (frames == 0) {
                rate = sr;
                mpeg1 = m1;
                // cabecera Xing/Info (MP3 de tasa variable): trae el número de cuadros
                int side = m1 ? ((h >>> 6 & 3) == 3 ? 17 : 32) : ((h >>> 6 & 3) == 3 ? 9 : 17);
                int x = i + 4 + side;
                if (x + 12 <= all.length) {
                    String tagName = new String(all, x, 4, StandardCharsets.ISO_8859_1);
                    if ((tagName.equals("Xing") || tagName.equals("Info")) && (all[x + 7] & 1) != 0) {
                        long n = (all[x + 8] & 255L) << 24 | (all[x + 9] & 255) << 16 | (all[x + 10] & 255) << 8 | (all[x + 11] & 255);
                        return new int[]{(int) Math.round(n * spf / (double) sr), mpeg1 ? 1 : 0};
                    }
                }
            }
            frames++;
            samples += spf;
            i += len;
        }
        if (frames == 0 || rate == 0) {
            return new int[]{-1, 0};
        }
        return new int[]{(int) Math.round(samples / (double) rate), mpeg1 ? 1 : 0};
    }

    /**
     * Para empezar un MP3 a media canción sin decodificar todo lo anterior: {byte donde empieza el cuadro, muestra en que empieza}
     * de un cuadro unos cuadros antes del segundo pedido (los primeros salen mal por la "reserva de bits" y se tiran).
     */
    public static long[] mp3Seek(Path p, double sec, int back) throws IOException {
        byte[] all = Files.readAllBytes(p);
        int i = (int) Math.min(all.length, id3Size(all));
        long samples = 0;
        int rate = 0;
        long[] offs = new long[Math.max(1, back + 1)];
        long[] smp = new long[offs.length];
        int k = 0;
        while (i + 4 <= all.length) {
            int h = (all[i] & 255) << 24 | (all[i + 1] & 255) << 16 | (all[i + 2] & 255) << 8 | (all[i + 3] & 255);
            int ver = h >>> 19 & 3, layer = h >>> 17 & 3, bri = h >>> 12 & 15, sri = h >>> 10 & 3, pad = h >>> 9 & 1;
            if ((h >>> 21) != 0x7FF || ver != 3 || layer == 0 || bri == 0 || bri == 15 || sri == 3) {
                i++;
                continue;
            }
            int sr = RATES[sri];
            int lay = 4 - layer;
            int br = BITRATES[lay - 1][bri] * 1000;
            int len = lay == 1 ? (12 * br / sr + pad) * 4 : 144 * br / sr + pad;
            int spf = lay == 1 ? 384 : 1152;
            if (len < 4) {
                i++;
                continue;
            }
            rate = sr;
            offs[k % offs.length] = i;
            smp[k % offs.length] = samples;
            if (samples + spf > sec * sr) {
                int j = k >= back ? (k - back) % offs.length : 0;
                return new long[]{k >= back ? offs[j] : offs[0], k >= back ? smp[j] : 0};
            }
            k++;
            samples += spf;
            i += len;
        }
        return new long[]{-1, 0};
    }

    // ------------------------------------------------------------------ OGG

    private static int indexOf(byte[] b, byte[] what, int from, int to) {
        outer:
        for (int i = Math.max(0, from); i <= Math.min(b.length, to) - what.length; i++) {
            for (int j = 0; j < what.length; j++) {
                if (b[i + j] != what[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static int le32(byte[] b, int i) {
        return (b[i] & 255) | (b[i + 1] & 255) << 8 | (b[i + 2] & 255) << 16 | (b[i + 3] & 255) << 24;
    }

    private static final byte[] VORBIS_ID = {1, 'v', 'o', 'r', 'b', 'i', 's'};
    private static final byte[] VORBIS_COMMENT = {3, 'v', 'o', 'r', 'b', 'i', 's'};

    private static String[] vorbisComments(Path p) throws IOException {
        byte[] head;
        try (InputStream in = Files.newInputStream(p)) {
            head = in.readNBytes(65536);
        }
        int c = indexOf(head, VORBIS_COMMENT, 0, head.length);
        if (c < 0) {
            return null;
        }
        int i = c + 7;
        int vendor = le32(head, i);
        i += 4 + vendor;
        if (vendor < 0 || i + 4 > head.length) {
            return null;
        }
        int n = le32(head, i);
        i += 4;
        String title = null, artist = null;
        for (int k = 0; k < n && i + 4 <= head.length; k++) {
            int len = le32(head, i);
            i += 4;
            if (len < 0 || i + len > head.length) {
                break;
            }
            String s = new String(head, i, len, StandardCharsets.UTF_8);
            i += len;
            int eq = s.indexOf('=');
            if (eq > 0) {
                String key = s.substring(0, eq).toUpperCase(Locale.ROOT);
                if (key.equals("TITLE")) {
                    title = s.substring(eq + 1);
                } else if (key.equals("ARTIST")) {
                    artist = s.substring(eq + 1);
                }
            }
        }
        return new String[]{title, artist};
    }

    /** Duración de un OGG Vorbis (la última posición de la última página entre la frecuencia). */
    public static int oggSeconds(Path p) throws IOException {
        long rate;
        try (InputStream in = Files.newInputStream(p)) {
            byte[] head = in.readNBytes(4096);
            int v = indexOf(head, VORBIS_ID, 0, head.length);
            if (v < 0 || v + 16 > head.length) {
                return -1;
            }
            rate = le32(head, v + 12) & 0xFFFFFFFFL;
        }
        try (RandomAccessFile f = new RandomAccessFile(p.toFile(), "r")) {
            long len = f.length();
            int n = (int) Math.min(len, 65536);
            byte[] tail = new byte[n];
            f.seek(len - n);
            f.readFully(tail);
            for (int i = n - 14; i >= 0; i--) {
                if (tail[i] == 'O' && tail[i + 1] == 'g' && tail[i + 2] == 'g' && tail[i + 3] == 'S') {
                    long gran = 0;
                    for (int k = 7; k >= 0; k--) {
                        gran = gran << 8 | (tail[i + 6 + k] & 255);
                    }
                    return rate > 0 && gran > 0 ? (int) Math.round(gran / (double) rate) : -1;
                }
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------ todos

    /** Duración en segundos (-1 si no se sabe); lanza IOException si el archivo no sirve. */
    public static int seconds(Path p, String ext) throws Exception {
        switch (ext) {
            case "mp3" -> {
                int[] info = mp3Info(p);
                if (info[0] < 0) {
                    throw new IOException("no es un MP3");
                }
                if (info[1] == 0) {
                    throw new IOException("MP3 de tipo MPEG-2 (no compatible)");
                }
                return info[0];
            }
            case "ogg" -> {
                return oggSeconds(p);
            }
            case "wav" -> {
                AudioFileFormat f = AudioSystem.getAudioFileFormat(p.toFile());
                long frames = f.getFrameLength();
                float rate = f.getFormat().getFrameRate();
                return frames > 0 && rate > 0 ? Math.round(frames / rate) : -1;
            }
            default -> throw new IOException("formato desconocido");
        }
    }
}
