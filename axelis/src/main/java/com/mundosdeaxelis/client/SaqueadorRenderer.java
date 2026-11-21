package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Saqueador;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Saqueador del Tesoro (un poco más grande cuanto más alto el tier). */
public class SaqueadorRenderer extends MobRenderer<Saqueador, SaqueadorModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/saqueador.png");

    public SaqueadorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SaqueadorModel(ctx.bakeLayer(SaqueadorModel.LAYER)), 0.4F);
    }

    @Override
    protected void scale(Saqueador e, PoseStack pose, float pt) {
        float s = 1.0F + 0.03F * (e.tier() - 1);
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Saqueador e) {
        return TEXTURE;
    }
}
