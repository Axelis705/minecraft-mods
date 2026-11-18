package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.LoboEscarcha;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja a los lobeznos de escarcha, con los ojos y los cristales del lomo brillando. */
public class LoboEscarchaRenderer extends MobRenderer<LoboEscarcha, LoboEscarchaModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/lobo_escarcha.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/lobo_escarcha_glow.png");

    public LoboEscarchaRenderer(EntityRendererProvider.Context context) {
        super(context, new LoboEscarchaModel(context.bakeLayer(LoboEscarchaModel.LAYER)), 0.7F);
        this.addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(LoboEscarcha entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(LoboEscarcha entity, PoseStack pose, float partialTick) {
        pose.scale(1.3F, 1.3F, 1.3F);
    }

    private static final class Glow extends RenderLayer<LoboEscarcha, LoboEscarchaModel> {
        Glow(LoboEscarchaRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, LoboEscarcha e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
