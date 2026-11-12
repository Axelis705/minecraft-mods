package com.autoscoleccion.game;

/**
 * El trazado de la mini pista, en bloques y en coordenadas "locales" del bloque de la pista: x hacia la derecha, z hacia adelante
 * (a donde mira el bloque). Es una curva cerrada (Catmull-Rom) que pasa por puntos dibujados a mano: recta de salida frente al bloque,
 * curva amplia, chicana, horquilla y regreso. La línea de meta está en s = 0 y se corre en el sentido de la recta (hacia +x).
 */
public final class TrackLayout {
    /** Mitad del ancho del asfalto (bloques). */
    public static final float HALF_W = 2.6F;
    /** Puntos de control (x, z). */
    private static final float[] PTS = {
            0, 6, 12, 6, 19, 8.5F, 22, 14, 19, 19, 16.5F, 22.5F, 19, 27, 16, 32, 8, 32, 3, 26.5F, -2, 32, -12, 32, -19, 28.5F, -16, 22,
            -21, 17, -21, 10, -15, 6};
    private static final float STEP = 0.5F;

    public static final TrackLayout MAIN = new TrackLayout();

    /** Muestras a lo largo de la pista: x, z, tangente x, tangente z (cada STEP bloques). */
    private final float[] xs, zs, txs, tzs;
    public final int n;
    public final float length;

    private TrackLayout() {
        int k = PTS.length / 2;
        int fine = 400;
        float[] fx = new float[k * fine + 1], fz = new float[k * fine + 1];
        int m = 0;
        for (int i = 0; i < k; i++) {
            float[] p0 = pt(i - 1, k), p1 = pt(i, k), p2 = pt(i + 1, k), p3 = pt(i + 2, k);
            for (int j = 0; j < fine; j++) {
                float t = j / (float) fine;
                fx[m] = cr(p0[0], p1[0], p2[0], p3[0], t);
                fz[m] = cr(p0[1], p1[1], p2[1], p3[1], t);
                m++;
            }
        }
        fx[m] = fx[0];
        fz[m] = fz[0];
        m++;
        // largo total y re-muestreo parejo
        float[] acc = new float[m];
        for (int i = 1; i < m; i++) {
            acc[i] = acc[i - 1] + (float) Math.hypot(fx[i] - fx[i - 1], fz[i] - fz[i - 1]);
        }
        length = acc[m - 1];
        n = (int) Math.floor(length / STEP);
        xs = new float[n];
        zs = new float[n];
        txs = new float[n];
        tzs = new float[n];
        int j = 0;
        for (int i = 0; i < n; i++) {
            float s = i * length / n;
            while (j < m - 2 && acc[j + 1] < s) {
                j++;
            }
            float u = (s - acc[j]) / Math.max(1e-6F, acc[j + 1] - acc[j]);
            xs[i] = fx[j] + (fx[j + 1] - fx[j]) * u;
            zs[i] = fz[j] + (fz[j + 1] - fz[j]) * u;
        }
        for (int i = 0; i < n; i++) {
            float dx = xs[(i + 1) % n] - xs[(i - 1 + n) % n], dz = zs[(i + 1) % n] - zs[(i - 1 + n) % n];
            float l = (float) Math.hypot(dx, dz);
            txs[i] = dx / l;
            tzs[i] = dz / l;
        }
    }

    private static float[] pt(int i, int k) {
        int q = ((i % k) + k) % k;
        return new float[] {PTS[q * 2], PTS[q * 2 + 1]};
    }

    private static float cr(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5F * (2 * p1 + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t2 + (-p0 + 3 * p1 - 3 * p2 + p3) * t3);
    }

    private int idx(float s) {
        float u = ((s % length) + length) % length;
        return Math.min(n - 1, (int) (u / length * n));
    }

    /** Punto del centro de la pista a la distancia s de la meta: x, z, tangente x, tangente z. */
    public float[] at(float s) {
        float u = ((s % length) + length) % length;
        float f = u / length * n;
        int i = (int) Math.floor(f) % n, i2 = (i + 1) % n;
        float k = f - (float) Math.floor(f);
        float tx = txs[i] + (txs[i2] - txs[i]) * k, tz = tzs[i] + (tzs[i2] - tzs[i]) * k;
        float l = (float) Math.hypot(tx, tz);
        return new float[] {xs[i] + (xs[i2] - xs[i]) * k, zs[i] + (zs[i2] - zs[i]) * k, tx / l, tz / l};
    }

    /** La distancia a lo largo de la pista del punto más cercano a (x, z), y qué tan lejos del centro está: {s, distancia}. */
    public float[] nearest(float x, float z) {
        int best = 0;
        float bd = Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            float d = (xs[i] - x) * (xs[i] - x) + (zs[i] - z) * (zs[i] - z);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return new float[] {best * length / n, (float) Math.sqrt(bd)};
    }

    /** Lo mismo pero buscando cerca de una s conocida (más rápido y no salta a otra parte de la pista). */
    public float[] nearestNear(float x, float z, float sHint, float window) {
        int c = idx(sHint);
        int w = (int) Math.ceil(window / length * n);
        int best = c;
        float bd = Float.MAX_VALUE;
        for (int o = -w; o <= w; o++) {
            int i = ((c + o) % n + n) % n;
            float d = (xs[i] - x) * (xs[i] - x) + (zs[i] - z) * (zs[i] - z);
            if (d < bd) {
                bd = d;
                best = i;
            }
        }
        return new float[] {best * length / n, (float) Math.sqrt(bd)};
    }

    /** Distancia mínima entre partes de la pista que no son vecinas (para que entren el asfalto y los muros). */
    public float minGap() {
        float min = Float.MAX_VALUE;
        int far = (int) (14 / STEP);
        for (int i = 0; i < n; i++) {
            for (int j = i + far; j < n; j++) {
                if (n - (j - i) < far) {
                    continue;
                }
                min = Math.min(min, (float) Math.hypot(xs[i] - xs[j], zs[i] - zs[j]));
            }
        }
        return min;
    }

    /** Caja que ocupa todo (con bordes y muros): minX, minZ, maxX, maxZ. */
    public float[] bounds(float margin) {
        float a = Float.MAX_VALUE, b = Float.MAX_VALUE, c = -Float.MAX_VALUE, d = -Float.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            a = Math.min(a, xs[i]);
            b = Math.min(b, zs[i]);
            c = Math.max(c, xs[i]);
            d = Math.max(d, zs[i]);
        }
        return new float[] {a - margin, b - margin, c + margin, d + margin};
    }
}
