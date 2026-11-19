package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Archimago;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja al Archimago: grande, con los ojos, la gema del bastón, los cristales y el borde de la túnica brillando en la oscuridad. */
public class ArchimagoRenderer extends AnimatedMobRenderer<Archimago, ArchimagoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/archimago.png");
    private static final ResourceLocation GLOW = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/archimago_glow.png");
    /** Tamaño del modelo respecto a su dibujo (las cajas miden 1/16 de bloque). */
    private static final float SCALE = 1.25F;

    public ArchimagoRenderer(EntityRendererProvider.Context context) {
        super(context, new ArchimagoModel(context.bakeLayer(ArchimagoModel.LAYER)), 1.2F);
        this.addLayer(new Glow(this));
    }

    @Override
    public ResourceLocation getTextureLocation(Archimago entity) {
        return TEXTURE;
    }


    /** Ya tiene barra de jefe: el rótulo con el nombre se quedaría metido en el sombrero. */
    @Override
    protected boolean shouldShowName(Archimago entity) {
        return false;
    }

    @Override
    protected void baseScale(Archimago entity, PoseStack pose, float partialTick) {
        pose.scale(SCALE, SCALE, SCALE);
    }

    /** Al morir se vence hacia atrás y se hunde (lo hace el modelo); aquí no se tumba como una criatura normal. */
    @Override
    protected void setupRotations(Archimago e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
    }

    private static final class Glow extends RenderLayer<Archimago, ArchimagoModel> {
        Glow(ArchimagoRenderer parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, Archimago e, float limbSwing, float limbSwingAmount,
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
