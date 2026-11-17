package com.celular.music;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * Una canción que suena desde el celular de OTRO jugador (su altavoz): un hilo aparte que la decodifica desde un segundo y la toca
 * con el volumen de cada lado que le pongan (según la distancia y hacia dónde queda). No tiene lista ni ecualizador.
 */
public final class RemoteStream extends Thread {
    public final Track track;
    private final double startSec;
    /** Volumen de cada lado (0..1), ya con la distancia. */
    public volatile float left, right;
    public volatile boolean paused;
    private volatile boolean quit;
    private volatile SourceDataLine line;
    private float lastL = -1, lastR = -1;

    public RemoteStream(Track track, double startSec) {
        super("Celular-Altavoz");
        setDaemon(true);
        this.track = track;
        this.startSec = Math.max(0, startSec);
    }

    public void kill() {
        quit = true;
        interrupt();
        SourceDataLine l = line;
        if (l != null) {
            try {
                l.stop();
                l.flush();
                l.close();
            } catch (Exception ignored) {
            }
        }
    }

    public boolean done() {
        return quit || !isAlive();
    }

    @Override
    public void run() {
        Pcm pcm = null;
        try {
            pcm = Pcm.open(track, startSec);
            AudioFormat f = pcm.format();
            int fb = f.getChannels() * 2;
            SourceDataLine l = AudioSystem.getSourceDataLine(f);
            l.open(f, Math.max(fb * 1024, (int) (f.getSampleRate() * 0.25F) * fb));
            line = l;
            l.start();
            byte[] buf = new byte[fb * 2048];
            while (!quit) {
                MusicPlayer.Host h = MusicPlayer.host;
                if (paused || (h != null && h.gamePaused())) {
                    if (l.isRunning()) {
                        l.stop();
                    }
                    Thread.sleep(30);
                    continue;
                }
                if (!l.isRunning()) {
                    l.start();
                }
                int n = 0;
                while (n < buf.length) {
                    int r = pcm.read(buf, n, buf.length - n);
                    if (r < 0) {
                        break;
                    }
                    n += r;
                }
                n -= n % fb;
                if (n <= 0) {
                    l.drain();
                    break;
                }
                float g = h == null ? 1F : h.gameVolume();
                float tl = left * g, tr = right * g;
                MusicPlayer.gain(buf, n, f.getChannels(), lastL < 0 ? tl : lastL, lastR < 0 ? tr : lastR, tl, tr);
                lastL = tl;
                lastR = tr;
                l.write(buf, 0, n);
            }
        } catch (InterruptedException ignored) {
        } catch (Throwable e) {
            track.broken = true;
        } finally {
            quit = true;
            if (pcm != null) {
                try {
                    pcm.close();
                } catch (Exception ignored) {
                }
            }
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
