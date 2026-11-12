package com.casino.plat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.Arrays;

/**
 * Un "pincel" para poner vértices igual en todas las versiones. En 1.20/1.21 escribe directo en el VertexConsumer de Minecraft; en 26.x
 * guarda los vértices (ya transformados) y {@link Draw} los entrega al final.
 */
public final class VB {
    private final VertexConsumer vc;
    // grabación (26.x): x y z u v nx ny nz + color, luz, overlay
    float[] f = new float[0];
    int[] i = new int[0];
    int n;

    VB(VertexConsumer vc) {
        this.vc = vc;
    }

    /** Un vértice: posición en el espacio de la pose, color RGBA (0-255), UV de la textura, overlay, luz y normal. */
    public VB v(PoseStack.Pose p, float x, float y, float z, int r, int g, int b, int a, float u, float w, int overlay, int light, float nx, float ny,
            float nz) {
        if (vc != null) {
            //#if MC >= 1.21
            //$$ vc.addVertex(p.pose(), x, y, z).setColor(r, g, b, a).setUv(u, w).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
            //#else
            vc.vertex(p.pose(), x, y, z).color(r, g, b, a).uv(u, w).overlayCoords(overlay).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
            //#endif
            return this;
        }
        Vector4f pos = new Vector4f(x, y, z, 1).mul(p.pose());
        Vector3f nor = new Vector3f(nx, ny, nz).mul(p.normal());
        if (n == f.length / 8) {
            int cap = Math.max(64, n * 2);
            f = Arrays.copyOf(f, cap * 8);
            i = Arrays.copyOf(i, cap * 3);
        }
        int o = n * 8;
        f[o] = pos.x;
        f[o + 1] = pos.y;
        f[o + 2] = pos.z;
        f[o + 3] = u;
        f[o + 4] = w;
        f[o + 5] = nor.x;
        f[o + 6] = nor.y;
        f[o + 7] = nor.z;
        i[n * 3] = (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
        i[n * 3 + 1] = light;
        i[n * 3 + 2] = overlay;
        n++;
        return this;
    }

    /** Lo mismo con matriz y normal sueltas (para código viejo que guarda la matriz). */
    public VB v(Matrix4f m, Matrix3f nm, float x, float y, float z, int r, int g, int b, int a, float u, float w, int overlay, int light, float nx,
            float ny, float nz) {
        PoseStack ps = new PoseStack();
        ps.last().pose().set(m);
        ps.last().normal().set(nm);
        return v(ps.last(), x, y, z, r, g, b, a, u, w, overlay, light, nx, ny, nz);
    }

    /** Entrega lo grabado a un VertexConsumer de Minecraft (26.x). */
    void replay(VertexConsumer out) {
        for (int k = 0; k < n; k++) {
            int o = k * 8;
            int c = i[k * 3];
            //#if MC >= 1.21
            //$$ out.addVertex(f[o], f[o + 1], f[o + 2]).setColor((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).setUv(f[o + 3], f[o + 4])
            //$$         .setOverlay(i[k * 3 + 2]).setLight(i[k * 3 + 1]).setNormal(f[o + 5], f[o + 6], f[o + 7]);
            //#else
            out.vertex(f[o], f[o + 1], f[o + 2]).color((c >> 16) & 255, (c >> 8) & 255, c & 255, (c >>> 24) & 255).uv(f[o + 3], f[o + 4])
                    .overlayCoords(i[k * 3 + 2]).uv2(i[k * 3 + 1]).normal(f[o + 5], f[o + 6], f[o + 7]).endVertex();
            //#endif
        }
    }
}
