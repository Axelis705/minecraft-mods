import com.celular.music.Meta;
import com.celular.music.Spectrum;
import com.celular.music.mp3.Sound;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Música del celular: etiquetas, duración, decodificar MP3 (también a media canción) y las barras del ecualizador. */
public class MusicTest {
    static void check(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
        System.out.println("  ok: " + what);
    }

    public static void main(String[] a) throws Exception {
        System.out.println("MusicTest");
        Path mp3 = Paths.get(a.length > 0 ? a[0] : "celular/tests/fixtures/tono.mp3");
        String[] tags = Meta.tags(mp3, "mp3");
        check(tags[0].equals("Tono") && tags[1].equals("Celular"), "etiquetas ID3 del MP3 (" + tags[0] + " / " + tags[1] + ")");
        int[] info = Meta.mp3Info(mp3);
        check(info[0] == 3 && info[1] == 1, "duración del MP3 = 3 s y es MPEG-1 (" + info[0] + ")");
        String[] n = Meta.fromName("Bad Bunny - Monaco.mp3");
        check(n[0].equals("Monaco") && n[1].equals("Bad Bunny"), "nombre de archivo Artista - Canción");
        check("mp3".equals(Meta.playable("A.MP3")) && Meta.playable("foto.png") == null, "solo formatos que se pueden tocar");

        // decodificar completo
        byte[] raw = Files.readAllBytes(mp3);
        int start = (int) Meta.id3Size(raw);
        Sound s = new Sound(new BufferedInputStream(new ByteArrayInputStream(raw, start, raw.length - start)));
        byte[] pcm = s.readAllBytes();
        double secs = pcm.length / 2.0 / s.getSamplingFrequency();
        check(secs > 2.9 && secs < 3.2, "MP3 decodificado: " + secs + " s");
        Spectrum sp = new Spectrum(s.getSamplingFrequency());
        sp.feed(pcm, pcm.length, 1, 0);
        float[] lv = sp.at(44100);
        int best = 0;
        for (int i = 1; i < lv.length; i++) {
            if (lv[i] > lv[best]) {
                best = i;
            }
        }
        check(lv[best] > 0.5F && best >= 6 && best <= 10, "el tono de 1000 Hz sube las barras del medio (banda " + best + ")");

        // empezar a media canción (salto por cuadros, sin decodificar lo de antes)
        long[] seek = Meta.mp3Seek(mp3, 2.0, 3);
        check(seek[0] > start && seek[1] > 0 && seek[1] <= 2 * 44100, "salto a 2 s: byte " + seek[0] + ", muestra " + seek[1]);
        Sound s2 = new Sound(new BufferedInputStream(new ByteArrayInputStream(raw, (int) seek[0], raw.length - (int) seek[0])));
        double rest = s2.readAllBytes().length / 2.0 / 44100;
        check(rest > 0.9 && rest < 1.3, "desde el salto quedan " + rest + " s");

        // WAV hecho aquí mismo
        int rate = 22050;
        byte[] w = new byte[rate * 2];
        for (int i = 0; i < rate; i++) {
            short v = (short) (Math.sin(2 * Math.PI * 440 * i / rate) * 12000);
            w[2 * i] = (byte) v;
            w[2 * i + 1] = (byte) (v >> 8);
        }
        Path wav = Files.createTempFile("celular", ".wav");
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(w), new AudioFormat(rate, 16, 1, true, false), rate),
                AudioFileFormat.Type.WAVE, wav.toFile());
        check(Meta.seconds(wav, "wav") == 1, "duración del WAV = 1 s");
        Files.delete(wav);
        System.out.println("MusicTest OK");
    }
}
