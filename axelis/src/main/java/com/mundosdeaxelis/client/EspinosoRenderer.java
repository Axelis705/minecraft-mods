package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Espinoso;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Espinoso (al morir se marchita y se desmorona: lo hace Juice con su estilo). */
public class EspinosoRenderer extends AnimatedMobRenderer<Espinoso, EspinosoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/espinoso.png");

    public EspinosoRenderer(EntityRendererProvider.Context context) {
        super(context, new EspinosoModel(context.bakeLayer(EspinosoModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Espinoso entity) {
        return TEXTURE;
    }
}
