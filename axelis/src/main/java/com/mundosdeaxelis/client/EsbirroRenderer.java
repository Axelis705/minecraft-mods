package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Esbirro;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Esbirro del Archimago: cuerpo humano con túnica y capucha (textura propia) y la daga en la mano. */
public class EsbirroRenderer extends HumanoidMobRenderer<Esbirro, HumanoidModel<Esbirro>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/esbirro.png");

    public EsbirroRenderer(EntityRendererProvider.Context context) {
        super(context, new LivelyHumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE), true, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Esbirro entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Esbirro e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(Esbirro e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        Juice.body(pose, e, CreatureStyles.of(e), pt);
    }

    @Override
    protected void scale(Esbirro e, PoseStack pose, float pt) {
        Juice.scale(pose, e, CreatureStyles.of(e), pt);
    }
}
