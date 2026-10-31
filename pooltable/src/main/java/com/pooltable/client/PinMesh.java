package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Un pino de boliche: superficie de revolución con el perfil real (cuello fino, cabeza redonda), a escala de bloque. Base en y = 0, punta en y = HEIGHT. */
public final class PinMesh {
    /** Perfil real en cm: {altura, radio}. */
    private static final double[][] PROFILE = {
            {0.0, 2.85}, {1.0, 3.35}, {4.0, 4.6}, {8.0, 5.7}, {11.4, 6.05}, {14.5, 5.75}, {18.0, 4.5}, {21.0, 3.0}, {23.5, 2.3},
            {26.0, 2.35}, {29.0, 3.3}, {32.0, 4.2}, {34.0, 4.5}, {36.0, 3.7}, {37.5, 2.2}, {38.1, 0.0}};
    private static final double SCALE = 0.53 / 38.1;
    public static final float HEIGHT = (float) (38.1 * SCALE);
    private static final int SEGS = 12;
    private static final int N = PROFILE.length;
    private static final float[] DATA = new float[(N - 1) * SEGS * 4 * 8];

    static {
        double[] ny = new double[N];
        double[] nr = new double[N];
        for (int i = 0; i < N; i++) {
            int a = Math.max(0, i - 1);
            int b = Math.min(N - 1, i + 1);
            double dr = PROFILE[b][1] - PROFILE[a][1];
            double dy = PROFILE[b][0] - PROFILE[a][0];
            double len = Math.hypot(dr, dy);
            nr[i] = dy / len;
            ny[i] = -dr / len;
        }
        int k = 0;
        for (int i = 0; i < N - 1; i++) {
            for (int j = 0; j < SEGS; j++) {
                int[][] c = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] p : c) {
                    double ang = 2 * Math.PI * p[1] / SEGS;
                    double r = PROFILE[p[0]][1] * SCALE;
                    DATA[k++] = (float) (Math.cos(ang) * r);
                    DATA[k++] = (float) (PROFILE[p[0]][0] * SCALE);
                    DATA[k++] = (float) (Math.sin(ang) * r);
                    DATA[k++] = (float) p[1] / SEGS;
                    DATA[k++] = (float) (1.0 - PROFILE[p[0]][0] / 38.1);
                    DATA[k++] = (float) (Math.cos(ang) * nr[p[0]]);
                    DATA[k++] = (float) ny[p[0]];
                    DATA[k++] = (float) (Math.sin(ang) * nr[p[0]]);
                }
            }
        }
    }

    private PinMesh() {
    }

    public static void draw(PoseStack ps, VertexConsumer vc, int light) {
        PoseStack.Pose pose = ps.last();
        int quads = (N - 1) * SEGS;
        for (int q = 0; q < quads; q++) {
            for (int v = 0; v < 4; v++) {
                int o = (q * 4 + v) * 8;
                vc.vertex(pose.pose(), DATA[o], DATA[o + 1], DATA[o + 2]).color(255, 255, 255, 255).uv(DATA[o + 3], DATA[o + 4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), DATA[o + 5], DATA[o + 6], DATA[o + 7]).endVertex();
            }
        }
    }
}
