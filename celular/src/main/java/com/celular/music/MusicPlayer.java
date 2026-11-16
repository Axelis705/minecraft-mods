package com.celular.music;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * El reproductor de música del celular. Las canciones están en la carpeta {@code .minecraft/celular/musica} (MP3, OGG o WAV) y suenan
 * con el audio de Java en un hilo aparte, así que funciona igual en todas las versiones de Minecraft y se puede pausar, adelantar y
 * saber el segundo exacto. Sigue sonando con el celular guardado; se pausa cuando el juego está en pausa.
 * <p>
 * Este archivo no usa nada de Minecraft: lo que necesita (carpeta, volumen del juego, si está en pausa) se lo da {@link Host}.
 */
public final class MusicPlayer {
    /** Lo que el reproductor necesita del juego. */
    public interface Host {
        Path gameDir();

        /** Volumen del juego para la música del celular (general × discos), de 0 a 1. */
        float gameVolume();

        boolean gamePaused();
    }

    public static Host host;

    public static final List<Track> library = new ArrayList<>();
    public static volatile boolean scanning;
    public static volatile long scannedAt;

    private static final List<Track> queue = new ArrayList<>();
    private static int qi = -1;
    private static final Random RND = new Random();

    /** La canción cargada (suene o esté en pausa). */
    public static volatile Track current;
    /** ¿El usuario la quiere sonando? */
    public static volatile boolean playing;
    public static volatile float volume = 0.8F;
    public static volatile boolean shuffle;
    /** 0 = no repetir, 1 = repetir la lista, 2 = repetir la canción. */
    public static volatile int repeat;
    /** Cambió la canción (para el aviso en pantalla); lo limpia quien lo muestra. */
    public static volatile Track announce;
    /** Último error para mostrar ("" si ninguno). */
    public static volatile String error = "";
    /** Segundo donde se quedó en pausa (cuando no hay hilo). */
    private static volatile double pausedAt;

    private static Worker worker;

    private MusicPlayer() {
    }

    // ------------------------------------------------------------------ biblioteca

    public static Path folder() {
        return host.gameDir().resolve("celular").resolve("musica");
    }

    /** Crea la carpeta (con un archivo que explica qué poner) si no existe. */
    public static Path ensureFolder() {
        Path f = folder();
        try {
            if (!Files.isDirectory(f)) {
                Files.createDirectories(f);
            }
            Path readme = f.resolve("LEEME - README.txt");
            if (!Files.exists(readme)) {
                Files.writeString(readme, """
                        Pon aquí tus canciones (MP3, OGG o WAV) y ábrelas con la app Música del celular.
                        Si el archivo se llama "Artista - Canción.mp3" se muestra así; si el MP3 trae etiquetas, se usan esas.

                        Put your songs here (MP3, OGG or WAV) and play them with the phone's Music app.
                        """);
            }
        } catch (IOException ignored) {
        }
        return f;
    }

    /** Vuelve a leer la carpeta (rápido) y calcula las duraciones aparte. */
    public static synchronized void scan() {
        Path dir = ensureFolder();
        List<Track> found = new ArrayList<>();
        try (Stream<Path> s = Files.walk(dir, 3)) {
            s.filter(Files::isRegularFile).forEach(p -> {
                String ext = Meta.playable(p.getFileName().toString());
                if (ext != null) {
                    String[] tags = Meta.tags(p, ext);
                    long mod = 0;
                    try {
                        mod = Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException ignored) {
                    }
                    found.add(new Track(p, tags[0], tags[1], ext, mod));
                }
            });
        } catch (IOException ignored) {
        }
        found.sort(Comparator.comparing((Track t) -> t.title.toLowerCase(Locale.ROOT)).thenComparing(t -> t.artist.toLowerCase(Locale.ROOT)));
        // conserva lo que ya se sabía de las que siguen ahí
        for (int i = 0; i < found.size(); i++) {
            Track n = found.get(i);
            for (Track o : library) {
                if (o.path.equals(n.path) && o.modified == n.modified) {
                    found.set(i, o);
                }
            }
        }
        library.clear();
        library.addAll(found);
        scannedAt = System.currentTimeMillis();
        List<Track> todo = new ArrayList<>();
        for (Track t : found) {
            if (t.seconds < 0 && !t.broken) {
                todo.add(t);
            }
        }
        if (!todo.isEmpty() && !scanning) {
            scanning = true;
            Thread th = new Thread(() -> {
                for (Track t : todo) {
                    try {
                        t.seconds = Math.max(0, Meta.seconds(t.path, t.ext));
                    } catch (Throwable e) {
                        t.broken = true;
                    }
                }
                scanning = false;
            }, "Celular-Musica-Duraciones");
            th.setDaemon(true);
            th.start();
        }
    }

    // ------------------------------------------------------------------ controles (desde la pantalla)

    /** Toca la canción {@code index} de {@code list} (y sigue con las demás de la lista). */
    public static synchronized void playList(List<Track> list, int index) {
        if (index < 0 || index >= list.size()) {
            return;
        }
        Track first = list.get(index);
        queue.clear();
        queue.addAll(list);
        qi = index;
        if (shuffle) {
            reshuffle(first);
        }
        start(first, 0);
    }

    private static void reshuffle(Track first) {
        queue.remove(first);
        Collections.shuffle(queue, RND);
        queue.add(0, first);
        qi = 0;
    }

    public static synchronized void setShuffle(boolean on) {
        shuffle = on;
        if (current == null) {
            return;
        }
        if (on) {
            reshuffle(current);
        } else {
            List<Track> sorted = new ArrayList<>(library);
            sorted.retainAll(queue);
            if (!sorted.isEmpty()) {
                queue.clear();
                queue.addAll(sorted);
            }
            qi = Math.max(0, queue.indexOf(current));
        }
    }

    public static synchronized void toggle() {
        if (current == null) {
            if (!library.isEmpty()) {
                playList(library, shuffle ? RND.nextInt(library.size()) : 0);
            }
            return;
        }
        if (playing) {
            pausedAt = position();
            playing = false;
            if (worker != null) {
                worker.paused = true;
            }
        } else {
            playing = true;
            if (worker != null && worker.isAlive()) {
                worker.paused = false;
            } else {
                start(current, pausedAt);
            }
        }
    }

    public static synchronized void next() {
        Track t = step(1, false);
        if (t != null) {
            start(t, 0);
        }
    }

    public static synchronized void prev() {
        if (position() > 3) {
            seek(0);
            return;
        }
        Track t = step(-1, false);
        if (t != null) {
            start(t, 0);
        }
    }

    public static synchronized void seek(double sec) {
        if (current == null) {
            return;
        }
        sec = Math.max(0, sec);
        if (worker != null && worker.isAlive()) {
            worker.seekTo = sec;
        } else {
            pausedAt = sec;
            if (playing) {
                start(current, sec);
            }
        }
    }

    public static synchronized void stop() {
        if (worker != null) {
            worker.kill();
            worker = null;
        }
        playing = false;
        current = null;
        pausedAt = 0;
    }

    /** Siguiente (o anterior) según la lista, el aleatorio y el repetir. {@code auto}: se acabó sola. Null = ya no hay más. */
    private static Track step(int dir, boolean auto) {
        if (queue.isEmpty()) {
            return null;
        }
        if (auto && repeat == 2) {
            return current;
        }
        int n = qi + dir;
        if (n >= queue.size()) {
            if (auto && repeat == 0) {
                return null;
            }
            if (shuffle && current != null) {
                reshuffle(queue.get(RND.nextInt(queue.size())));
                return queue.get(0);
            }
            n = 0;
        } else if (n < 0) {
            n = queue.size() - 1;
        }
        // se salta las que no se pueden tocar
        for (int k = 0; k < queue.size(); k++) {
            Track t = queue.get(Math.floorMod(n + k * (dir < 0 ? -1 : 1), queue.size()));
            if (!t.broken) {
                qi = queue.indexOf(t);
                return t;
            }
        }
        return null;
    }

    private static void start(Track t, double sec) {
        if (worker != null) {
            worker.kill();
        }
        if (t != current) {
            announce = t;
        }
        current = t;
        playing = true;
        error = "";
        pausedAt = sec;
        worker = new Worker(t, sec);
        worker.start();
    }

    /** El hilo terminó la canción solo: la siguiente (desde el hilo; devuelve null si ya no hay más). */
    private static synchronized Track finished(Worker w) {
        if (w != worker) {
            return null;
        }
        Track t = step(1, true);
        if (t == null) {
            playing = false;
            pausedAt = 0;
            worker = null;
            return null;
        }
        current = t;
        announce = t;
        return t;
    }

    private static synchronized void failed(Worker w, Track t, String why) {
        t.broken = true;
        error = why;
        if (w == worker) {
            worker = null;
            Track n = step(1, false);
            if (n != null && n != t) {
                start(n, 0);
            } else {
                playing = false;
            }
        }
    }

    // ------------------------------------------------------------------ estado (para dibujar)

    public static double position() {
        Worker w = worker;
        if (w != null && w.isAlive() && w.track == current) {
            return w.position();
        }
        return pausedAt;
    }

    public static int length() {
        Track t = current;
        return t == null ? 0 : Math.max(0, t.seconds);
    }

    /** Barras del ecualizador del momento (0..1); todo en cero si no suena. */
    public static float[] levels() {
        Worker w = worker;
        if (w == null || !playing || w.spectrum == null) {
            return null;
        }
        return w.spectrum.at(w.frameNow());
    }

    public static boolean active() {
        return current != null;
    }

    // ------------------------------------------------------------------ el hilo que suena

    private static final class Worker extends Thread {
        volatile Track track;
        volatile boolean quit;
        volatile boolean paused;
        volatile double seekTo = -1;
        volatile Spectrum spectrum;
        private SourceDataLine line;
        private AudioFormat lineFormat;
        private volatile long baseFrame;
        private volatile long lineBase;
        private volatile float rate = 44100;
        private double startSec;

        Worker(Track t, double sec) {
            super("Celular-Musica");
            setDaemon(true);
            this.track = t;
            this.startSec = sec;
        }

        /** Que se calle ya (cierra su salida para que no termine de escribir lo que tenía). */
        void kill() {
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

        double position() {
            SourceDataLine l = line;
            if (l == null) {
                return startSec;
            }
            return (baseFrame + Math.max(0, l.getLongFramePosition() - lineBase)) / (double) rate;
        }

        long frameNow() {
            SourceDataLine l = line;
            return l == null ? 0 : baseFrame + Math.max(0, l.getLongFramePosition() - lineBase);
        }

        @Override
        public void run() {
            try {
                while (!quit) {
                    Track t = track;
                    boolean done = playOne(t);
                    if (quit) {
                        break;
                    }
                    if (!done) {
                        return;                       // falló (ya se avisó)
                    }
                    Track n = finished(this);
                    if (n == null) {
                        break;
                    }
                    track = n;
                    startSec = 0;
                }
            } finally {
                closeLine();
            }
        }

        private void closeLine() {
            SourceDataLine l = line;
            if (l != null) {
                try {
                    l.stop();
                    l.flush();
                    l.close();
                } catch (Exception ignored) {
                }
            }
            line = null;
        }

        /** Toca una canción entera. true si llegó al final (o la quitaron), false si falló. */
        private boolean playOne(Track t) {
            Pcm pcm;
            try {
                pcm = Pcm.open(t, startSec);
            } catch (Throwable e) {
                failed(this, t, "celular.music.error_file");
                return false;
            }
            try {
                AudioFormat f = pcm.format();
                rate = f.getSampleRate();
                spectrum = new Spectrum(rate);
                if (line == null || !sameFormat(f, lineFormat)) {
                    closeLine();
                    try {
                        SourceDataLine l = AudioSystem.getSourceDataLine(f);
                        int fb = f.getChannels() * 2;
                        l.open(f, Math.max(fb * 1024, (int) (rate * 0.25F) * fb));
                        line = l;
                        lineFormat = f;
                    } catch (Throwable e) {
                        failed(this, t, "celular.music.error_audio");
                        return false;
                    }
                }
                SourceDataLine l = line;
                baseFrame = (long) (startSec * rate);
                lineBase = l.getLongFramePosition();
                long fed = baseFrame;
                l.start();
                int fb = f.getChannels() * 2;
                byte[] buf = new byte[fb * 2048];
                while (!quit) {
                    double sk = seekTo;
                    if (sk >= 0) {
                        seekTo = -1;
                        pcm.close();
                        pcm = Pcm.open(t, sk);
                        l.stop();
                        l.flush();
                        baseFrame = (long) (sk * rate);
                        lineBase = l.getLongFramePosition();
                        fed = baseFrame;
                        if (!paused) {
                            l.start();
                        }
                    }
                    boolean hold = paused || (host != null && host.gamePaused());
                    if (hold) {
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
                        // se acabó: deja que termine de sonar lo que está en la salida
                        l.drain();
                        return true;
                    }
                    spectrum.feed(buf, n, f.getChannels(), fed);
                    fed += n / fb;
                    gain(buf, n, volume * volume * (host == null ? 1 : host.gameVolume()));
                    l.write(buf, 0, n);
                }
                return true;
            } catch (InterruptedException e) {
                return true;
            } catch (Throwable e) {
                if (quit) {
                    return true;                     // la cortaron a propósito (otra canción o detener)
                }
                failed(this, t, "celular.music.error_file");
                return false;
            } finally {
                try {
                    pcm.close();
                } catch (IOException ignored) {
                }
            }
        }

        private static boolean sameFormat(AudioFormat a, AudioFormat b) {
            return b != null && a.getSampleRate() == b.getSampleRate() && a.getChannels() == b.getChannels();
        }

        private static void gain(byte[] b, int n, float g) {
            if (g >= 0.999F) {
                return;
            }
            for (int i = 0; i + 1 < n; i += 2) {
                int s = (short) ((b[i] & 255) | b[i + 1] << 8);
                s = Math.round(s * g);
                b[i] = (byte) s;
                b[i + 1] = (byte) (s >> 8);
            }
        }
    }
}
