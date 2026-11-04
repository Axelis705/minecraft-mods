package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Fundidor;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a Fundidor con las partes incandescentes brillando en la oscuridad (y apagándose al morir). */
public class FundidorRenderer extends MobRenderer<Fundidor, FundidorModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/fundidor.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/fundidor_glow.png");

    public FundidorRenderer(EntityRendererProvider.Context context) {
        super(context, new FundidorModel(context.bakeLayer(FundidorModel.LAYER)), 0.8F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected float getFlipDegrees(Fundidor e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(Fundidor e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        ForgeDeath.topple(pose, e, pt);
    }

    @Override
    protected void scale(Fundidor e, PoseStack pose, float partialTick) {
        float s = 1.15F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Fundidor entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<Fundidor, FundidorModel> {
        Glow(FundidorRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Fundidor e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(e.isRooting() ? 1.0F : 0.7F + 0.3F * Mth.sin(age * 0.12F), 0.0F, 1.0F) * (1.0F - 0.85F * ForgeDeath.progress(e, pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
