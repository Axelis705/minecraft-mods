package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.CentinelaEscarcha;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Centinela de escarcha (etapa 258): soldado con armadura helada (textura propia) y espada; se agacha al cargar la onda helada. */
public class CentinelaEscarchaRenderer extends HumanoidMobRenderer<CentinelaEscarcha, HumanoidModel<CentinelaEscarcha>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/centinela_escarcha.png");

    public CentinelaEscarchaRenderer(EntityRendererProvider.Context context) {
        super(context, new LivelyHumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE), true, false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(CentinelaEscarcha entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(CentinelaEscarcha e) {
        return 0.0F;
    }

    @Override
    protected void setupRotations(CentinelaEscarcha e, PoseStack pose, float age, float yaw, float pt) {
        super.setupRotations(e, pose, age, yaw, pt);
        Juice.body(pose, e, CreatureStyles.of(e), pt);
    }

    @Override
    protected void scale(CentinelaEscarcha e, PoseStack pose, float pt) {
        // un poco más grande que un jugador; al cargar la onda se agacha
        float s = 1.06F;
        if (e.charging()) {
            pose.scale(s * 1.04F, s * 0.9F, s * 1.04F);
        } else {
            pose.scale(s, s, s);
        }
        Juice.scale(pose, e, CreatureStyles.of(e), pt);
    }
}
