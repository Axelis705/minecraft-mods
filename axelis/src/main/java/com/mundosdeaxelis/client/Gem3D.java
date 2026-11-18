package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Cristal facetado en 3D de verdad (sólido, con sombreado por cara) que flota sobre los crates.
 * Se arma con triángulos / cuadriláteros y una textura blanca; el color sale del vértice.
 */
public final class Gem3D {
    private Gem3D() {
    }

    /** Forma del cristal. */
    public enum Shape {
        /** Brillante clásico: mesa plana arriba y punta abajo. */
        BRILLIANT(8, 0.30F, 0.62F, 0.50F, 0.0F),
        /** Octaedro alargado (cristal de roca, punta arriba y abajo). */
        SPIKE(4, 0.26F, 0.55F, 0.45F, 0.0F),
        /** Prisma hexagonal con puntas en los dos extremos. */
        ROD(6, 0.20F, 0.30F, 0.30F, 0.34F),
        /** Gema baja y ancha, tipo esmeralda. */
        EMERALD(8, 0.34F, 0.38F, 0.42F, 0.0F);

        final int sides;
        final float r, top, bot, body;

        Shape(int sides, float r, float top, float bot, float body) {
            this.sides = sides;
            this.r = r;
            this.top = top;
            this.bot = bot;
            this.body = body;
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float[] a, float[] b, float[] c, float[] d,
                             int rgb, float k, float alpha) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
        float vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1.0E-6F) {
            return;
        }
        nx /= len;
        ny /= len;
        nz /= len;
        // la normal siempre hacia afuera del cristal (centro cerca del origen)
        float cx = (a[0] + b[0] + c[0] + d[0]) * 0.25F, cy = (a[1] + b[1] + c[1] + d[1]) * 0.25F, cz = (a[2] + b[2] + c[2] + d[2]) * 0.25F;
        if (nx * cx + ny * cy + nz * cz < 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        float r = Math.min(1, CrateUi.r(rgb) * k), g = Math.min(1, CrateUi.g(rgb) * k), bl = Math.min(1, CrateUi.b(rgb) * k);
        float[][] vs = {a, b, c, d};
        for (float[] v : vs) {
            vc.vertex(p.pose(), v[0], v[1], v[2]).color(r, g, bl, alpha).uv(0.5F, 0.5F)
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                    .normal(p.normal(), nx, ny, nz).endVertex();
        }
    }

    /**
     * Dibuja el cristal centrado en el origen del PoseStack actual.
     *
     * @param scale tamaño general
     * @param spin  giro en grados alrededor del eje vertical
     */
    public static void draw(VertexConsumer vc, PoseStack ps, Shape s, int rgb, float scale, float spin, float alpha) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(spin));
        ps.scale(scale, scale, scale);
        PoseStack.Pose p = ps.last();
        int n = s.sides;
        float[][] ring = new float[n][3];
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2.0 * i / n + (n == 4 ? Math.PI / 4 : 0.0);
            ring[i] = new float[]{(float) Math.cos(a) * s.r, 0.0F, (float) Math.sin(a) * s.r};
        }
        if (s.body > 0) {
            // prisma: anillo bajo, anillo alto y puntas
            float[][] lo = new float[n][3], hi = new float[n][3];
            for (int i = 0; i < n; i++) {
                lo[i] = new float[]{ring[i][0], -s.body, ring[i][2]};
                hi[i] = new float[]{ring[i][0], s.body, ring[i][2]};
            }
            float[] apexT = {0, s.body + s.top, 0}, apexB = {0, -s.body - s.bot, 0};
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float k = 0.70F + 0.30F * (float) Math.abs(Math.sin(i * 1.7 + 0.4));
                quad(vc, p, lo[i], lo[j], hi[j], hi[i], rgb, k, alpha);
                quad(vc, p, hi[i], hi[j], apexT, apexT, rgb, 0.95F + 0.20F * (i % 2), alpha);
                quad(vc, p, lo[j], lo[i], apexB, apexB, rgb, 0.55F + 0.20F * (i % 2), alpha);
            }
        } else if (s == Shape.BRILLIANT || s == Shape.EMERALD) {
            // corona con mesa plana, cintura y pabellón
            float tr = s.r * 0.55F;
            float[][] table = new float[n][3];
            for (int i = 0; i < n; i++) {
                table[i] = new float[]{ring[i][0] * 0.55F, s.top, ring[i][2] * 0.55F};
            }
            float[] low = {0, -s.bot, 0};
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float k = 0.62F + 0.45F * (float) Math.abs(Math.sin(i * 1.3 + 0.7));
                quad(vc, p, ring[i], ring[j], table[j], table[i], rgb, k + 0.1F, alpha);
                quad(vc, p, ring[j], ring[i], low, low, rgb, k * 0.8F, alpha);
            }
            float[][] tq = table;
            for (int i = 1; i < n - 1; i++) {
                quad(vc, p, tq[0], tq[i], tq[i + 1], tq[i + 1], rgb, 1.25F, alpha);
            }
        } else {
            float[] apexT = {0, s.top, 0}, apexB = {0, -s.bot, 0};
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                float k = 0.70F + 0.35F * (float) Math.abs(Math.sin(i * 2.1 + 0.5));
                quad(vc, p, ring[i], ring[j], apexT, apexT, rgb, k + 0.15F, alpha);
                quad(vc, p, ring[j], ring[i], apexB, apexB, rgb, k * 0.7F, alpha);
            }
        }
        ps.popPose();
    }
}
