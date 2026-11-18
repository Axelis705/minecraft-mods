package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.LoboArtico;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja al Lobo Ártico: grande, con los ojos, los cristales y los filos de las guadañas brillando en la oscuridad. */
public class LoboArticoRenderer extends MobRenderer<LoboArtico, LoboArticoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/lobo_artico.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/lobo_artico_glow.png");
    /** Tamaño del modelo respecto a su dibujo (las cajas miden 1/16 de bloque). */
    private static final float SCALE = 1.4F;

    public LoboArticoRenderer(EntityRendererProvider.Context context) {
        super(context, new LoboArticoModel(context.bakeLayer(LoboArticoModel.LAYER)), 2.1F);
        this.addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(LoboArtico entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(LoboArtico entity) {
        return 0.0F;
    }

    /** Ya tiene barra de jefe: el rótulo con el nombre se quedaría metido en la capucha. */
    @Override
    protected boolean shouldShowName(LoboArtico entity) {
        return false;
    }

    @Override
    protected void scale(LoboArtico entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    /** Al morir se arrodilla y, al final, se va de bruces hacia delante. */
    @Override
    protected void setupRotations(LoboArtico e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        float p = e.deathProgress(pt);
        if (p > 0.0F) {
            float k = Mth.clamp((p - 0.5F) / 0.5F, 0.0F, 1.0F);
            k = k * k * (3.0F - 2.0F * k);
            pose.mulPose(Axis.XP.rotationDegrees(k * 78.0F));
        }
    }

    private static final class Glow extends RenderLayer<LoboArtico, LoboArticoModel> {
        Glow(LoboArticoRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, LoboArtico e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            float k = Mth.clamp(e.glowLevel(age), 0.0F, 1.0F) * (1.0F - 0.8F * e.deathProgress(pt));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(GLOW));
            getParentModel().renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F), k, k, k, 1.0F);
        }
    }
}
