package com.celular.voice;

/**
 * Efecto de "voz por teléfono": deja pasar solo de ~300 a ~3400 Hz (como una línea telefónica), satura un poquito y agrega un siseo muy
 * bajo. Trabaja sobre audio de 48 kHz en bloques; guarda el estado de los filtros entre bloques para que no haga clics.
 */
public final class PhoneFilter {
    private final Biquad high = Biquad.highPass(48000, 320, 0.707);
    private final Biquad low = Biquad.lowPass(48000, 3300, 0.707);
    private final Biquad low2 = Biquad.lowPass(48000, 3300, 0.707);
    private final java.util.Random rnd = new java.util.Random();

    public void apply(short[] pcm) {
        for (int i = 0; i < pcm.length; i++) {
            double x = pcm[i] / 32768.0;
            x = high.process(x);
            x = low2.process(low.process(x));
            x = Math.tanh(x * 2.2) * 0.75;               // un poco de saturación
            x += (rnd.nextDouble() - 0.5) * 0.004;       // siseo de línea
            int v = (int) Math.round(x * 32767.0);
            pcm[i] = (short) Math.max(-32768, Math.min(32767, v));
        }
    }

    /** Filtro de segundo orden (fórmulas del "Audio EQ Cookbook"). */
    static final class Biquad {
        private final double b0, b1, b2, a1, a2;
        private double x1, x2, y1, y2;

        private Biquad(double b0, double b1, double b2, double a0, double a1, double a2) {
            this.b0 = b0 / a0;
            this.b1 = b1 / a0;
            this.b2 = b2 / a0;
            this.a1 = a1 / a0;
            this.a2 = a2 / a0;
        }

        static Biquad lowPass(double rate, double f, double q) {
            double w = 2 * Math.PI * f / rate, c = Math.cos(w), alpha = Math.sin(w) / (2 * q);
            return new Biquad((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha);
        }

        static Biquad highPass(double rate, double f, double q) {
            double w = 2 * Math.PI * f / rate, c = Math.cos(w), alpha = Math.sin(w) / (2 * q);
            return new Biquad((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha);
        }

        double process(double x) {
            double y = b0 * x + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
            x2 = x1;
            x1 = x;
            y2 = y1;
            y1 = y;
            return y;
        }
    }
}
