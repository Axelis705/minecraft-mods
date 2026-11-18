package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GranHerrero;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a GranHerrero con las partes incandescentes brillando en la oscuridad (y apagándose al morir). */
public class GranHerreroRenderer extends MobRenderer<GranHerrero, GranHerreroModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/gran_herrero.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/gran_herrero_glow.png");

    public GranHerreroRenderer(EntityRendererProvider.Context context) {
        super(context, new GranHerreroModel(context.bakeLayer(GranHerreroModel.LAYER)), 2.2F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected float getFlipDegrees(GranHerrero e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(GranHerrero e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        ForgeDeath.topple(pose, e, pt);
    }

    @Override
    protected void scale(GranHerrero e, PoseStack pose, float partialTick) {
        float s = 1.5F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(GranHerrero entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<GranHerrero, GranHerreroModel> {
        Glow(GranHerreroRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, GranHerrero e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(e.glowLevel(age), 0.0F, 1.0F) * (1.0F - 0.85F * ForgeDeath.progress(e, pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
