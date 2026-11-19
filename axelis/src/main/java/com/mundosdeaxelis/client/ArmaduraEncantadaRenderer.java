package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.ArmaduraEncantada;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Armadura encantada: el cuerpo es invisible (textura transparente) y solo se ve la armadura de hierro que lleva puesta, como una
 * armadura vacía que se mueve sola. Los ojos son chispitas que pone la entidad cuando está despierta.
 */
public class ArmaduraEncantadaRenderer extends HumanoidMobRenderer<ArmaduraEncantada, HumanoidModel<ArmaduraEncantada>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/armadura_encantada.png");

    public ArmaduraEncantadaRenderer(EntityRendererProvider.Context context) {
        super(context, new LivelyHumanoidModel<ArmaduraEncantada>(context.bakeLayer(ModelLayers.ZOMBIE), false, true)
                .asleepWhen(e -> !e.isAwake()), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ArmaduraEncantada entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(ArmaduraEncantada e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(ArmaduraEncantada e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        if (e.isAwake() || e.deathTime > 0) {
            Juice.body(pose, e, CreatureStyles.of(e), pt);
        }
    }

    @Override
    protected void scale(ArmaduraEncantada e, PoseStack pose, float pt) {
        Juice.scale(pose, e, CreatureStyles.of(e), pt);
    }
}
