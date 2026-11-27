package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.entidad.HuellaKrampus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** (Cliente) La huella de pezuña: un dibujo plano pegado al piso, que se borra poco a poco. */
public class HuellaRenderer extends EntityRenderer<HuellaKrampus> {
    private static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/entity/huella_krampus.png");

    public HuellaRenderer(EntityRendererProvider.Context c) {
        super(c);
    }

    @Override
    public void render(HuellaKrampus h, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float a = h.alfa(pt);
        if (a <= 0.01F) {
            return;
        }
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-h.getYRot()));
        if (h.izquierda()) {
            ps.scale(-1, 1, 1);
        }
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucent(TEX));
        int al = (int) (a * 235);
        float w = 0.24F, l = 0.32F;
        p(vc, m, n, -w, -l, 0, 1, al, light);
        p(vc, m, n, w, -l, 1, 1, al, light);
        p(vc, m, n, w, l, 1, 0, al, light);
        p(vc, m, n, -w, l, 0, 0, al, light);
        ps.popPose();
    }

    private static void p(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float z, float u, float v, int a, int light) {
        vc.vertex(m, x, 0.02F, z).color(255, 255, 255, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, 0, 1, 0)
                .endVertex();
    }

    @Override
    public ResourceLocation getTextureLocation(HuellaKrampus h) {
        return TEX;
    }
}
