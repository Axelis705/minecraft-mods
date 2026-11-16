package com.celular.music;

import com.celular.music.mp3.Sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Sonido ya decodificado (PCM de 16 bits, little-endian, mono o estéreo) de una canción, para dárselo a Minecraft poco a poco.
 * MP3 con JavaMP3, WAV con el Java de siempre y OGG con el decodificador de Minecraft.
 */
public abstract class Pcm implements Closeable {
    protected AudioFormat format;

    public AudioFormat format() {
        return format;
    }

    public int frameBytes() {
        return format.getChannels() * 2;
    }

    /** Lee hasta {@code len} bytes; -1 al terminar. */
    public abstract int read(byte[] b, int off, int len) throws IOException;

    /** Se salta {@code frames} cuadros (para empezar a media canción). */
    public void skipFrames(long frames) throws IOException {
        byte[] tmp = new byte[64 * 1024];
        long left = frames * frameBytes();
        while (left > 0) {
            int n = read(tmp, 0, (int) Math.min(tmp.length, left));
            if (n < 0) {
                return;
            }
            left -= n;
        }
    }

    /** Abre la canción y la deja lista desde el segundo {@code startSec}. */
    public static Pcm open(Track t, double startSec) throws IOException {
        if (t.ext.equals("mp3") && startSec > 1) {
            // salto rápido: se abre unos cuadros antes del segundo pedido y se tira lo que sobra
            long[] seek = Meta.mp3Seek(t.path, startSec, 3);
            if (seek[0] > 0) {
                Pcm p = new Mp3(t.path, seek[0]);
                long skip = (long) (startSec * p.format.getSampleRate()) - seek[1];
                if (skip > 0) {
                    p.skipFrames(skip);
                }
                return p;
            }
        }
        Pcm p = switch (t.ext) {
            case "mp3" -> new Mp3(t.path, 0);
            case "wav" -> new Wav(t.path);
            case "ogg" -> new Ogg(t.path);
            default -> throw new IOException("formato desconocido");
        };
        if (p.format.getChannels() < 1 || p.format.getChannels() > 2) {
            p.close();
            throw new IOException("solo mono o estéreo");
        }
        if (startSec > 0.05) {
            p.skipFrames((long) (startSec * p.format.getSampleRate()));
        }
        return p;
    }

    // ------------------------------------------------------------------ MP3

    static final class Mp3 extends Pcm {
        private final Sound sound;

        /** {@code from}: byte donde empieza (0 = el principio, saltando la etiqueta ID3). */
        Mp3(Path path, long from) throws IOException {
            BufferedInputStream in = new BufferedInputStream(Files.newInputStream(path), 1 << 16);
            try {
                // la etiqueta ID3 del principio (con la portada) puede parecer sonido: se salta completa
                in.mark(16);
                byte[] head = in.readNBytes(10);
                in.reset();
                long id3 = from > 0 ? from : Meta.id3Size(head);
                long left = id3;
                while (left > 0) {
                    long s = in.skip(left);
                    if (s <= 0) {
                        break;
                    }
                    left -= s;
                }
                sound = new Sound(in);
            } catch (IOException | RuntimeException e) {
                in.close();
                throw e instanceof IOException io ? io : new IOException(e);
            }
            format = sound.getAudioFormat();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            try {
                return sound.read(b, off, len);
            } catch (RuntimeException e) {
                return -1;            // cuadro dañado al final del archivo: se termina ahí
            }
        }

        @Override
        public void close() throws IOException {
            sound.close();
        }
    }

    // ------------------------------------------------------------------ WAV

    static final class Wav extends Pcm {
        private final AudioInputStream in;

        Wav(Path path) throws IOException {
            AudioInputStream a;
            try {
                a = AudioSystem.getAudioInputStream(new BufferedInputStream(Files.newInputStream(path), 1 << 16));
            } catch (Exception e) {
                throw new IOException(e);
            }
            AudioFormat f = a.getFormat();
            if (f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || f.getSampleSizeInBits() != 16 || f.isBigEndian()) {
                AudioFormat to = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(),
                        f.getChannels() * 2, f.getSampleRate(), false);
                try {
                    a = AudioSystem.getAudioInputStream(to, a);
                } catch (Exception e) {
                    a.close();
                    throw new IOException(e);
                }
            }
            in = a;
            format = a.getFormat();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return in.read(b, off, len - len % frameBytes());
        }

        @Override
        public void skipFrames(long frames) throws IOException {
            long left = frames * frameBytes();
            while (left > 0) {
                long s = in.skip(left);
                if (s <= 0) {
                    super.skipFrames(left / frameBytes());
                    return;
                }
                left -= s;
            }
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    // ------------------------------------------------------------------ OGG (decodificador de Minecraft)

    static final class Ogg extends Pcm {
        private final net.minecraft.client.sounds.AudioStream stream;
        private ByteBuffer left;
        private boolean end;

        Ogg(Path path) throws IOException {
            InputStream in = new BufferedInputStream(Files.newInputStream(path), 1 << 16);
            try {
                //#if MC >= 1.20.2
                //$$ stream = new net.minecraft.client.sounds.JOrbisAudioStream(in);
                //#else
                stream = new com.mojang.blaze3d.audio.OggAudioStream(in);
                //#endif
            } catch (IOException | RuntimeException e) {
                in.close();
                throw e instanceof IOException io ? io : new IOException(e);
            }
            format = stream.getFormat();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int done = 0;
            while (done < len) {
                if (left == null || !left.hasRemaining()) {
                    if (end) {
                        break;
                    }
                    left = stream.read(Math.max(4096, len - done));
                    if (left == null || !left.hasRemaining()) {
                        end = true;
                        break;
                    }
                }
                int n = Math.min(len - done, left.remaining());
                left.get(b, off + done, n);
                done += n;
            }
            return done == 0 && end ? -1 : done;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }
    }
}
