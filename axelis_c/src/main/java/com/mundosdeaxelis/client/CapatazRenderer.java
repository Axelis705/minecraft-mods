package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Capataz;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a Capataz con las partes incandescentes brillando en la oscuridad (y apagándose al morir). */
public class CapatazRenderer extends MobRenderer<Capataz, CapatazModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/capataz.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/capataz_glow.png");

    public CapatazRenderer(EntityRendererProvider.Context context) {
        super(context, new CapatazModel(context.bakeLayer(CapatazModel.LAYER)), 1.1F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected float getFlipDegrees(Capataz e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(Capataz e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        ForgeDeath.topple(pose, e, pt);
    }

    @Override
    protected void scale(Capataz e, PoseStack pose, float partialTick) {
        float s = 1.3F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Capataz entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<Capataz, CapatazModel> {
        Glow(CapatazRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Capataz e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(0.7F + 0.3F * Mth.sin(age * 0.1F) + 0.5F * e.slamProgress(), 0.0F, 1.0F) * (1.0F - 0.85F * ForgeDeath.progress(e, pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
