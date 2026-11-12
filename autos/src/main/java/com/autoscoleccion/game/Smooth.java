package com.autoscoleccion.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Normales suaves: en cada esquina se promedian las normales de las caras que la comparten y miran casi hacia el mismo lado. Así las
 * curvas de la carrocería se ven redondas y brillantes, pero los cantos marcados (más de ~40°) siguen marcados.
 */
public final class Smooth {
    private static final float CREASE = 0.76F;

    private Smooth() {
    }

    public static float[] faceNormal(float[] q) {
        float ux = q[3] - q[0], uy = q[4] - q[1], uz = q[5] - q[2];
        float vx = q[9] - q[0], vy = q[10] - q[1], vz = q[11] - q[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        if (nx * nx + ny * ny + nz * nz < 1e-10F) {
            ux = q[6] - q[3];
            uy = q[7] - q[4];
            uz = q[8] - q[5];
            vx = q[0] - q[3];
            vy = q[1] - q[4];
            vz = q[2] - q[5];
            nx = uy * vz - uz * vy;
            ny = uz * vx - ux * vz;
            nz = ux * vy - uy * vx;
        }
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6F) {
            return null;
        }
        // el área (para pesar el promedio) va en el 4.º valor
        return new float[] {nx / l, ny / l, nz / l, l};
    }

    private static long key(float x, float y, float z) {
        long a = Math.round(x * 200), b = Math.round(y * 200), c = Math.round(z * 200);
        return (a & 0x1FFFFF) << 42 | (b & 0x1FFFFF) << 21 | (c & 0x1FFFFF);
    }

    /**
     * @param quads 12 floats por cara (4 esquinas)
     * @param faceN normal de cada cara (de {@link #faceNormal})
     * @return 12 floats por cara: la normal de cada esquina
     */
    public static float[] vertexNormals(List<float[]> quads, List<float[]> faceN) {
        int n = quads.size();
        Map<Long, List<Integer>> at = new HashMap<>();
        for (int i = 0; i < n; i++) {
            float[] q = quads.get(i);
            for (int k = 0; k < 4; k++) {
                at.computeIfAbsent(key(q[k * 3], q[k * 3 + 1], q[k * 3 + 2]), x -> new ArrayList<>(6)).add(i);
            }
        }
        float[] out = new float[n * 12];
        for (int i = 0; i < n; i++) {
            float[] q = quads.get(i);
            float[] fn = faceN.get(i);
            for (int k = 0; k < 4; k++) {
                List<Integer> list = at.get(key(q[k * 3], q[k * 3 + 1], q[k * 3 + 2]));
                float sx = 0, sy = 0, sz = 0;
                int last = -1;
                for (int j : list) {
                    if (j == last) {
                        continue;
                    }
                    last = j;
                    float[] o = faceN.get(j);
                    float d = o[0] * fn[0] + o[1] * fn[1] + o[2] * fn[2];
                    if (d >= CREASE) {
                        float w = Math.min(o[3], 4.0F) + 0.05F;
                        sx += o[0] * w;
                        sy += o[1] * w;
                        sz += o[2] * w;
                    }
                }
                float l = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
                if (l < 1e-6F) {
                    sx = fn[0];
                    sy = fn[1];
                    sz = fn[2];
                    l = 1;
                }
                out[i * 12 + k * 3] = sx / l;
                out[i * 12 + k * 3 + 1] = sy / l;
                out[i * 12 + k * 3 + 2] = sz / l;
            }
        }
        return out;
    }
}
