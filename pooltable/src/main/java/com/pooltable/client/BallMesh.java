package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/** Una esfera de radio 1 hecha de cuadrados (anillos x segmentos), con coordenadas de textura equirectangulares. */
public final class BallMesh {
    private static final int RINGS = 12;
    private static final int SEGS = 24;
    /** Por cada cuadrado: 4 vértices x (x, y, z, u, v). La normal de una esfera unitaria es su propia posición. */
    private static final float[] DATA = new float[RINGS * SEGS * 4 * 5];

    static {
        int k = 0;
        for (int i = 0; i < RINGS; i++) {
            for (int j = 0; j < SEGS; j++) {
                int[][] corner = {{i, j}, {i + 1, j}, {i + 1, j + 1}, {i, j + 1}};
                for (int[] c : corner) {
                    double th = Math.PI * c[0] / RINGS;
                    double ph = 2 * Math.PI * c[1] / SEGS;
                    DATA[k++] = (float) (Math.sin(th) * Math.cos(ph));
                    DATA[k++] = (float) Math.cos(th);
                    DATA[k++] = (float) (Math.sin(th) * Math.sin(ph));
                    DATA[k++] = (float) c[1] / SEGS;
                    DATA[k++] = (float) c[0] / RINGS;
                }
            }
        }
    }

    private BallMesh() {
    }

    public static void draw(PoseStack ps, VertexConsumer vc, int light) {
        PoseStack.Pose pose = ps.last();
        for (int q = 0; q < RINGS * SEGS; q++) {
            for (int v = 0; v < 4; v++) {
                int o = (q * 4 + v) * 5;
                float x = DATA[o];
                float y = DATA[o + 1];
                float z = DATA[o + 2];
                vc.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(DATA[o + 3], DATA[o + 4])
                        .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(), x, y, z).endVertex();
            }
        }
    }
}
