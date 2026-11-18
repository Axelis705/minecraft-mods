package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.GuardianMusgo;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja al Guardián de Musgo, con los ojos y la runa del pecho brillando (más fuerte al preparar el golpe de suelo). */
public class GuardianRenderer extends MobRenderer<GuardianMusgo, GuardianModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/guardian_musgo.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/guardian_musgo_glow.png");

    public GuardianRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardianModel(context.bakeLayer(GuardianModel.LAYER)), 1.2F);
        this.addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(GuardianMusgo entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<GuardianMusgo, GuardianModel> {
        Glow(GuardianRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, GuardianMusgo e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = 0.7F + 0.3F * Mth.sin(age * 0.1F) + 0.6F * e.slamProgress();
            k = Mth.clamp(k, 0.0F, 1.0F);
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
