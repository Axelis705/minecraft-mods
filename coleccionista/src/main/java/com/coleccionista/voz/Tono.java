package com.coleccionista.voz;

/**
 * Cambia el tono de una voz al vuelo (más grave o más aguda) sin cambiar su velocidad: dos "cabezas de lectura" recorren lo último que se
 * dijo a otra velocidad, cada una con una ventana triangular, y se mezclan para que no se noten los saltos (el truco de las cintas
 * giratorias). Trabaja por cuadros de 20 ms (960 muestras a 48 kHz, como los de Simple Voice Chat) y recuerda entre cuadros, así que cada
 * persona que habla necesita el suyo. No depende de Simple Voice Chat (se puede probar aparte).
 */
public final class Tono {
    private static final int TAM = 8192, MASK = TAM - 1;
    /** El tamaño de la ventana (muestras): ~32 ms. */
    private static final int W = 1536;
    private final float razon;
    private final float[] buf = new float[TAM];
    private int w;
    private float fase;

    /** @param razon 0.5 = una octava más grave; 2 = una más aguda; 1 = igual. */
    public Tono(float razon) {
        this.razon = razon;
    }

    public float razon() {
        return razon;
    }

    public void reiniciar() {
        java.util.Arrays.fill(buf, 0F);
        w = 0;
        fase = 0;
    }

    public short[] procesar(short[] in) {
        short[] out = new short[in.length];
        float paso = 1F - razon;
        for (int n = 0; n < in.length; n++) {
            buf[w & MASK] = in[n];
            // el retraso de cada cabeza va de 0 a W y da la vuelta
            fase += paso;
            if (fase >= W) {
                fase -= W;
            } else if (fase < 0) {
                fase += W;
            }
            float d1 = fase, d2 = fase + W / 2F;
            if (d2 >= W) {
                d2 -= W;
            }
            float y = leer(d1) * ventana(d1) + leer(d2) * ventana(d2);
            out[n] = (short) Math.max(-32768, Math.min(32767, Math.round(y)));
            w++;
        }
        return out;
    }

    /** La ventana triangular: 0 en las orillas (donde salta la cabeza), 1 en medio; las dos juntas suman 1. */
    private static float ventana(float d) {
        return 1F - Math.abs(2F * d / W - 1F);
    }

    /** Lo que se dijo hace {@code d} muestras (con interpolación). */
    private float leer(float d) {
        float pos = w - 1 - d - 2;
        int i = (int) Math.floor(pos);
        float f = pos - i;
        float a = buf[i & MASK], b = buf[(i + 1) & MASK];
        return a + (b - a) * f;
    }
}
