import com.celular.voice.PhoneFilter;

/** El filtro de "voz por teléfono": deja pasar la voz (1 kHz) y corta graves (100 Hz) y agudos (9 kHz), sin desbordarse. */
public class PhoneFilterTest {
    static double rms(double freq) {
        PhoneFilter f = new PhoneFilter();
        double sum = 0;
        int n = 0;
        for (int block = 0; block < 50; block++) {
            short[] pcm = new short[960];
            for (int i = 0; i < pcm.length; i++) {
                int k = block * 960 + i;
                pcm[i] = (short) (Math.sin(2 * Math.PI * freq * k / 48000.0) * 8000);
            }
            f.apply(pcm);
            if (block >= 10) {
                for (short v : pcm) {
                    sum += v * (double) v;
                    n++;
                }
            }
        }
        return Math.sqrt(sum / n);
    }

    public static void main(String[] a) {
        double low = rms(100), mid = rms(1000), high = rms(9000);
        System.out.printf("100 Hz: %.0f  1 kHz: %.0f  9 kHz: %.0f%n", low, mid, high);
        if (!(mid > 3 * low && mid > 3 * high && mid > 1000)) {
            throw new AssertionError("el filtro no se comporta como un teléfono");
        }
        System.out.println("PhoneFilterTest OK");
    }
}
