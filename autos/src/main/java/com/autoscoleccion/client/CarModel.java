package com.autoscoleccion.client;

import com.autoscoleccion.AutosMod;
import com.autoscoleccion.game.CarMesh;
import com.autoscoleccion.game.CarStyle;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Dibuja un cochecito en el mundo, en la mano o en una pantalla: la forma sale de {@link CarMesh} y aquí se pasa a vértices de
 * Minecraft (color por vértice sobre una textura blanca; lo que brilla usa luz máxima).
 */
public final class CarModel {
    public static final ResourceLocation WHITE = new ResourceLocation(AutosMod.MODID, "textures/misc/white.png");
    public static final int FULL = 0xF000F0;

    private CarModel() {
    }

    /** Dibuja el auto en píxeles (1/16 de bloque): el que llama escala como quiera. */
    public static void render(CarStyle s, PoseStack ps, MultiBufferSource buf, int light, float time, int mode) {
        Matrix4f pose = ps.last().pose();
        Matrix3f normal = ps.last().normal();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        CarMesh.build(s, (a, b, c, d, rgb, em) -> {
            Vector3f u = new Vector3f(b[0] - a[0], b[1] - a[1], b[2] - a[2]);
            Vector3f v = new Vector3f(d[0] - a[0], d[1] - a[1], d[2] - a[2]);
            Vector3f n = u.cross(v);
            if (n.lengthSquared() < 1e-8F) {
                u = new Vector3f(c[0] - b[0], c[1] - b[1], c[2] - b[2]);
                v = new Vector3f(a[0] - b[0], a[1] - b[1], a[2] - b[2]);
                n = u.cross(v);
            }
            if (n.lengthSquared() < 1e-8F) {
                return;
            }
            n.normalize();
            int lt = em ? FULL : light;
            int r = (rgb >> 16) & 255;
            int g = (rgb >> 8) & 255;
            int bl = rgb & 255;
            for (float[] p : new float[][] {a, b, c, d}) {
                vc.vertex(pose, p[0], p[1], p[2]).color(r, g, bl, 255).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(lt).normal(normal, n.x, n.y, n.z).endVertex();
            }
        }, time, mode);
    }
}
