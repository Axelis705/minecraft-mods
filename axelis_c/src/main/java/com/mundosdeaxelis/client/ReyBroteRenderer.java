package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ReyBrote;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja al Rey Brote: cuerpo, gelatina translúcida por fuera y semilla brillante, con estiramiento al saltar y aplastarse. */
public class ReyBroteRenderer extends MobRenderer<ReyBrote, ReyBroteModel> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/rey_brote.png");

    public ReyBroteRenderer(EntityRendererProvider.Context context) {
        super(context, new ReyBroteModel(context.bakeLayer(ReyBroteModel.BODY)), 2.4F);
        this.addLayer(new Shell(this, new ReyBroteModel(context.bakeLayer(ReyBroteModel.SHELL))));
        this.addLayer(new Seed(this, new ReyBroteModel(context.bakeLayer(ReyBroteModel.SEED))));
    }

    @Override
    public ResourceLocation getTextureLocation(ReyBrote entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(ReyBrote entity) {
        return 0.0F;
    }

    @Override
    public void render(ReyBrote entity, float yaw, float pt, PoseStack pose, MultiBufferSource buffer, int light) {
        this.shadowRadius = 2.4F * entity.scaleFactor();
        super.render(entity, yaw, pt, pose, buffer, light);
    }

    /** Ya tiene barra de jefe: el rótulo con su nombre se quedaba metido dentro de la corona al apuntarle. */
    @Override
    protected boolean shouldShowName(ReyBrote entity) {
        return false;
    }

    @Override
    protected void scale(ReyBrote entity, PoseStack pose, float pt) {
        float base = 6.0F * entity.scaleFactor();
        float sq = Mth.lerp(pt, entity.sqO, entity.sq);
        // aplastado (sq < 0): más ancho y bajo; estirado (sq > 0): más alto y fino
        float sy = 1.0F + 0.28F * sq;
        float sxz = 1.0F - 0.16F * sq;
        float death = entity.deathTime > 0 ? Math.max(0.0F, 1.0F - (entity.deathTime + pt) / 20.0F) : 1.0F;
        pose.scale(base * sxz * death, base * sy * death, base * sxz * death);
    }

    /** La gelatina de fuera. */
    private static final class Shell extends RenderLayer<ReyBrote, ReyBroteModel> {
        private final ReyBroteModel model;

        Shell(ReyBroteRenderer parent, ReyBroteModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, ReyBrote e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            getParentModel().copyPropertiesTo(this.model);
            this.model.prepareMobModel(e, limbSwing, limbSwingAmount, pt);
            this.model.setupAnim(e, limbSwing, limbSwingAmount, age, yawHead, pitch);
            VertexConsumer vc = buffer.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.model.renderToBuffer(pose, vc, light, LivingEntityRenderer.getOverlayCoords(e, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    /** La semilla del centro, que late y brilla. */
    private static final class Seed extends RenderLayer<ReyBrote, ReyBroteModel> {
        private final ReyBroteModel model;

        Seed(ReyBroteRenderer parent, ReyBroteModel model) {
            super(parent);
            this.model = model;
        }

        @Override
        public void render(PoseStack pose, MultiBufferSource buffer, int light, ReyBrote e, float limbSwing, float limbSwingAmount,
                           float pt, float age, float yawHead, float pitch) {
            if (e.isInvisible()) {
                return;
            }
            getParentModel().copyPropertiesTo(this.model);
            float pulse = 0.75F + 0.25F * Mth.sin((age) * (0.12F + 0.04F * e.phase()));
            VertexConsumer vc = buffer.getBuffer(RenderType.eyes(TEXTURE));
            this.model.renderToBuffer(pose, vc, LightTexture.FULL_BRIGHT, LivingEntityRenderer.getOverlayCoords(e, 0.0F),
                    pulse, pulse, pulse, 1.0F);
        }
    }
}
