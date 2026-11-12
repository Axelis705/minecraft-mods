package com.autoscoleccion.game;

/**
 * El trazado de la mini pista, en bloques y en coordenadas "locales" del bloque de la pista: x hacia la derecha, z hacia adelante
 * (a donde mira el bloque). Es una curva cerrada (Catmull-Rom) que pasa por puntos dibujados a mano: recta de salida frente al bloque,
 * curva amplia, chicana, horquilla y regreso. La línea de meta está en s = 0 y se corre en el sentido de la recta (hacia +x).
 */
public final class TrackLayout {
    /** Mitad del ancho del asfalto de la pista clásica (bloques). */
    public static final float HALF_W = 2.6F;
    private static final float STEP = 0.5F;

    /** Las pistas, en el orden del tema guardado en el bloque. */
    public static final TrackLayout[] ALL = {
            // 0 Clásica: chiquita, delante del bloque
            new TrackLayout("Circuito Clásico", "Asfalto, pianos rojo y blanco. La original.", 2.6F,
                    0, 6, 12, 6, 19, 8.5F, 22, 14, 19, 19, 16.5F, 22.5F, 19, 27, 16, 32, 8, 32, 3, 26.5F, -2, 32, -12, 32, -19, 28.5F, -16, 22,
                    -21, 17, -21, 10, -15, 6),
            // 1 Desierto: grande, curvas rápidas alrededor del bloque
            new TrackLayout("Desierto Dorado", "Arenisca y terracota, cactus y una recta larguísima.", 3.0F,
                    0, -26, 20, -26, 36, -21, 43, -8, 36, 4, 42, 17, 35, 31, 18, 36, 8, 26, -4, 36, -22, 36, -38, 28, -44, 12, -36, 0, -43, -14,
                    -30, -25, -14, -27),
            // 2 Ciudad neón: esquinas como cuadras, de noche
            new TrackLayout("Ciudad Neón", "Asfalto negro, líneas amarillas y faroles de neón.", 3.0F,
                    0, -30, 22, -30, 34, -28, 38, -16, 38, -2, 28, 0, 20, 10, 32, 22, 38, 30, 32, 40, 16, 42, -4, 42, -20, 40, -26, 30, -12, 19,
                    -30, 8, -40, 0, -42, -16, -36, -28, -18, -30),
            // 3 Nieve: subidas y bajadas de montaña, horquillas
            new TrackLayout("Pico Nevado", "Hielo compacto, nieve y pinos. Horquillas cerradas.", 3.0F,
                    0, -28, 18, -28, 34, -22, 40, -8, 30, 2, 14, 2, 8, 12, 18, 22, 34, 22, 42, 32, 34, 44, 14, 44, -2, 36, -14, 44, -32, 44,
                    -42, 32, -34, 20, -42, 6, -40, -14, -26, -26, -12, -28),
            // 4 Volcán: la más larga, roca negra y magma
            new TrackLayout("Volcán Infierno", "Roca negra, magma y la pista más larga de todas.", 3.0F,
                    0, -34, 26, -34, 44, -26, 48, -10, 38, -2, 22, -6, 12, 2, 16, 14, 32, 14, 46, 24, 44, 42, 28, 52, 10, 48, 0, 38, -10, 48,
                    -28, 52, -44, 42, -48, 24, -36, 14, -46, 0, -44, -20, -30, -32, -14, -34)};
    public static final TrackLayout MAIN = ALL[0];

    public final String name;
    public final String blurb;
    public final float halfW;

    /** Muestras a lo largo de la pista: x, z, tangente x, tangente z (cada STEP bloques). */
    private final float[] xs, zs, txs, tzs;
    public final int n;
    public final float length;

    private TrackLayout(String name, String blurb, float halfW, float... PTS) {
        this.name = name;
        this.blurb = blurb;
        this.halfW = halfW;
        int k = PTS.length / 2;
        int fine = 400;
        float[] fx = new float[k * fine + 1], fz = new float[k * fine + 1];
        int m = 0;
        for (int i = 0; i < k; i++) {
            float[] p0 = pt(PTS, i - 1, k), p1 = pt(PTS, i, k), p2 = pt(PTS, i + 1, k), p3 = pt(PTS, i + 2, k);
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

    private static float[] pt(float[] PTS, int i, int k) {
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
        int far = (int) ((halfW * 2 + 9) / STEP);
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
