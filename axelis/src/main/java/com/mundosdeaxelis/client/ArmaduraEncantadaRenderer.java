package com.mundosdeaxelis.client;

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
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)), context.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(ArmaduraEncantada entity) {
        return TEXTURE;
    }
}
