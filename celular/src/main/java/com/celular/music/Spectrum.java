package com.celular.music;

/**
 * Las barras del ecualizador: mientras se decodifica la canción, cada 1024 cuadros se calcula cuánto suena cada banda (de graves a
 * agudos) y se guarda con su número de cuadro, para que la pantalla muestre las del momento que se está oyendo. Sin nada de Minecraft.
 */
public final class Spectrum {
    public static final int BANDS = 16;
    private static final int N = 1024;
    private static final int KEEP = 1024;

    private final long[] at = new long[KEEP];
    private final float[][] levels = new float[KEEP][BANDS];
    private int head;
    private int count;
    private final float[] window = new float[N];
    private final float[] buf = new float[N];
    private int filled;
    private long bufStart = -1;
    private final int[] bandLo = new int[BANDS];
    private final int[] bandHi = new int[BANDS];

    public Spectrum(float sampleRate) {
        for (int i = 0; i < N; i++) {
            window[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (N - 1)));
        }
        // bandas logarítmicas de 50 Hz a 15 kHz
        double lo = 50, hi = Math.min(15000, sampleRate / 2 - 1);
        for (int b = 0; b < BANDS; b++) {
            double f0 = lo * Math.pow(hi / lo, b / (double) BANDS), f1 = lo * Math.pow(hi / lo, (b + 1) / (double) BANDS);
            bandLo[b] = Math.max(1, (int) Math.floor(f0 * N / sampleRate));
            bandHi[b] = Math.max(bandLo[b] + 1, (int) Math.ceil(f1 * N / sampleRate));
        }
    }

    /** Agrega sonido decodificado (PCM 16 bits LE) que empieza en el cuadro {@code firstFrame}. */
    public void feed(byte[] pcm, int len, int channels, long firstFrame) {
        int fb = channels * 2;
        int frames = len / fb;
        for (int f = 0; f < frames; f++) {
            int i = f * fb;
            float s = (short) ((pcm[i] & 255) | pcm[i + 1] << 8);
            if (channels == 2) {
                s = (s + (short) ((pcm[i + 2] & 255) | pcm[i + 3] << 8)) * 0.5F;
            }
            if (filled == 0) {
                bufStart = firstFrame + f;
            }
            buf[filled++] = s / 32768F;
            if (filled == N) {
                analyze();
                filled = 0;
            }
        }
    }

    private void analyze() {
        float[] re = new float[N], im = new float[N];
        for (int i = 0; i < N; i++) {
            re[i] = buf[i] * window[i];
        }
        fft(re, im);
        float[] out = new float[BANDS];
        for (int b = 0; b < BANDS; b++) {
            double sum = 0;
            for (int k = bandLo[b]; k < Math.min(N / 2, bandHi[b]); k++) {
                sum = Math.max(sum, Math.hypot(re[k], im[k]));
            }
            double db = 20 * Math.log10(sum / (N / 4.0) + 1e-9);   // 0 dB = tono a todo volumen
            out[b] = (float) Math.max(0, Math.min(1, (db + 54) / 50));
        }
        synchronized (this) {
            at[head] = bufStart + N / 2;
            levels[head] = out;
            head = (head + 1) % KEEP;
            count = Math.min(KEEP, count + 1);
        }
    }

    /** Las barras del cuadro {@code frame} (o null si todavía no hay nada). */
    public synchronized float[] at(long frame) {
        float[] best = null;
        long bestAt = Long.MIN_VALUE;
        for (int i = 0; i < count; i++) {
            int k = Math.floorMod(head - 1 - i, KEEP);
            if (at[k] <= frame && at[k] > bestAt) {
                bestAt = at[k];
                best = levels[k];
                break;
            }
        }
        return best;
    }

    /** FFT de raíz 2 en el lugar (N potencia de 2). */
    static void fft(float[] re, float[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) {
                j ^= bit;
            }
            j ^= bit;
            if (i < j) {
                float t = re[i];
                re[i] = re[j];
                re[j] = t;
                t = im[i];
                im[i] = im[j];
                im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            float wr = (float) Math.cos(ang), wi = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k, b = i + k + len / 2;
                    float xr = re[b] * cr - im[b] * ci, xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr;
                    im[b] = im[a] - xi;
                    re[a] += xr;
                    im[a] += xi;
                    float nr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = nr;
                }
            }
        }
    }
}
