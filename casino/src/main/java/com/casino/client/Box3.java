package com.casino.client;

import com.casino.Casino;
import com.casino.plat.Ids;
import com.casino.plat.client.VB;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/** Cajas de color (y hacia arriba) para dibujar piezas sin modelo: cámaras, rayos, la puerta de la bóveda. */
public final class Box3 {
    public static final ResourceLocation WHITE = Ids.rl(Casino.MODID, "textures/entity/bubble.png");
    private final VB vb;
    private final PoseStack.Pose p;
    private final int light;

    public Box3(VB vb, PoseStack.Pose p, int light) {
        this.vb = vb;
        this.p = p;
        this.light = light;
    }

    public void box(double ax, double ay, double az, double bx, double by, double bz, int rgb) {
        box(ax, ay, az, bx, by, bz, rgb, 255);
    }

    public void box(double ax, double ay, double az, double bx, double by, double bz, int rgb, int alpha) {
        float x0 = (float) Math.min(ax, bx), x1 = (float) Math.max(ax, bx);
        float y0 = (float) Math.min(ay, by), y1 = (float) Math.max(ay, by);
        float z0 = (float) Math.min(az, bz), z1 = (float) Math.max(az, bz);
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        quad(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, r, g, b, alpha, 1.0F);
        quad(x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, r, g, b, alpha, 0.5F);
        quad(x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0, 0, 0, -1, r, g, b, alpha, 0.8F);
        quad(x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1, 0, 0, 1, r, g, b, alpha, 0.8F);
        quad(x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1, -1, 0, 0, r, g, b, alpha, 0.65F);
        quad(x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0, 1, 0, 0, r, g, b, alpha, 0.65F);
    }

    private void quad(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
            float nx, float ny, float nz, int r, int g, int b, int a, float k) {
        int rr = (int) (r * k), gg = (int) (g * k), bb = (int) (b * k);
        vb.v(p, ax, ay, az, rr, gg, bb, a, 0.4F, 0.4F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        vb.v(p, bx, by, bz, rr, gg, bb, a, 0.4F, 0.6F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        vb.v(p, cx, cy, cz, rr, gg, bb, a, 0.6F, 0.6F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
        vb.v(p, dx, dy, dz, rr, gg, bb, a, 0.6F, 0.4F, OverlayTexture.NO_OVERLAY, light, nx, ny, nz);
    }
}
