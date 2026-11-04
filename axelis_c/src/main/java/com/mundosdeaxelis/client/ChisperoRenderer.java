package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Chispero;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a Chispero con las partes incandescentes brillando en la oscuridad (y apagándose al morir). */
public class ChisperoRenderer extends MobRenderer<Chispero, ChisperoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/chispero.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/chispero_glow.png");

    public ChisperoRenderer(EntityRendererProvider.Context context) {
        super(context, new ChisperoModel(context.bakeLayer(ChisperoModel.LAYER)), 0.5F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected float getFlipDegrees(Chispero e) {
        return 0.0F;
    }

    @Override
    protected void scale(Chispero e, PoseStack pose, float partialTick) {
        float d = ForgeDeath.progress(e, partialTick);
        float s = 1.0F;
        pose.scale(s * (1.0F + 0.35F * d), s * (1.0F - 0.85F * d), s * (1.0F + 0.35F * d));
    }

    @Override
    public ResourceLocation getTextureLocation(Chispero entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<Chispero, ChisperoModel> {
        Glow(ChisperoRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Chispero e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(e.isCharging() ? 1.0F : 0.7F + 0.3F * Mth.sin(age * 0.25F), 0.0F, 1.0F) * (1.0F - 0.85F * ForgeDeath.progress(e, pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
