package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Polinizador;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja a Polinizador. */
public class PolinizadorRenderer extends MobRenderer<Polinizador, PolinizadorModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/polinizador.png");

    public PolinizadorRenderer(EntityRendererProvider.Context context) {
        super(context, new PolinizadorModel(context.bakeLayer(PolinizadorModel.LAYER)), 0.3F);
    }

    @Override
    protected void scale(Polinizador e, PoseStack pose, float partialTick) {
        float s = 1.4F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Polinizador entity) {
        return TEXTURE;
    }
}
