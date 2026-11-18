package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Ascua;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja a Ascua con las partes incandescentes brillando en la oscuridad (y apagándose al morir). */
public class AscuaRenderer extends MobRenderer<Ascua, AscuaModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/ascua.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/ascua_glow.png");

    public AscuaRenderer(EntityRendererProvider.Context context) {
        super(context, new AscuaModel(context.bakeLayer(AscuaModel.LAYER)), 0.3F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected float getFlipDegrees(Ascua e) {
        return 0.0F;
    }

    @Override
    protected void scale(Ascua e, PoseStack pose, float partialTick) {
        float d = ForgeDeath.progress(e, partialTick);
        float s = 1.2F * (1.0F - 0.9F * d);
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Ascua entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<Ascua, AscuaModel> {
        Glow(AscuaRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Ascua e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(e.isCharging() ? 1.0F : 0.75F + 0.25F * Mth.sin(age * 0.3F), 0.0F, 1.0F) * (1.0F - 0.85F * ForgeDeath.progress(e, pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
