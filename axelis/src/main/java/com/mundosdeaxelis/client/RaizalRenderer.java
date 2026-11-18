package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Raizal;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja a Raizal. */
public class RaizalRenderer extends MobRenderer<Raizal, RaizalModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/raizal.png");

    public RaizalRenderer(EntityRendererProvider.Context context) {
        super(context, new RaizalModel(context.bakeLayer(RaizalModel.LAYER)), 0.8F);
    }

    @Override
    protected void scale(Raizal e, PoseStack pose, float partialTick) {
        float s = e.isElite() ? 1.5F : 1.2F;
        pose.scale(s, s, s);
    }

    @Override
    public ResourceLocation getTextureLocation(Raizal entity) {
        return TEXTURE;
    }
}
