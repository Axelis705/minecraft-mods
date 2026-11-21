package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ColosoHierro;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * Dibuja al Coloso de Hierro. El horno del pecho, la rendija de los ojos y las grietas de los puños brillan en la oscuridad: casi
 * apagados mientras duerme, con un latido lento despierto, más fuerte al cargar el golpe y al rojo vivo cuando se recalienta.
 */
public class ColosoRenderer extends AnimatedMobRenderer<ColosoHierro, ColosoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/coloso_hierro.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/coloso_hierro_glow.png");

    public ColosoRenderer(EntityRendererProvider.Context context) {
        super(context, new ColosoModel(context.bakeLayer(ColosoModel.LAYER)), 1.5F);
        this.addLayer(new Glow(this));
    }

    @Override
    protected void baseScale(ColosoHierro e, PoseStack pose, float partialTick) {
        float s = 1.3F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(ColosoHierro entity) {
        return TEXTURE;
    }

    private static final class Glow extends RenderLayer<ColosoHierro, ColosoModel> {
        Glow(ColosoRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, ColosoHierro e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            int st = e.state();
            float k;
            if (st == ColosoHierro.ASLEEP) {
                k = 0.18F + 0.06F * Mth.sin(age * 0.05F);
            } else if (st == ColosoHierro.HOT) {
                k = 0.95F + 0.05F * Mth.sin(age * 0.9F);
            } else if (st == ColosoHierro.CHARGE) {
                k = 0.85F + 0.15F * Mth.sin(age * 0.6F);
            } else {
                k = 0.62F + 0.22F * Mth.sin(age * 0.12F);
            }
            k *= 1.0F - 0.85F * ForgeDeath.progress(e, pt);
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            float red = 1.0F, green = st == ColosoHierro.HOT ? 0.75F : 1.0F, blue = st == ColosoHierro.HOT ? 0.6F : 1.0F;
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k * red, k * green, k * blue, 1.0F);
        }
    }
}
