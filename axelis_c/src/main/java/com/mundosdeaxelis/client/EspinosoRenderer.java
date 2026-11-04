package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Espinoso;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Espinoso (al morir se encoge en vez de caerse). */
public class EspinosoRenderer extends MobRenderer<Espinoso, EspinosoModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/espinoso.png");

    public EspinosoRenderer(EntityRendererProvider.Context context) {
        super(context, new EspinosoModel(context.bakeLayer(EspinosoModel.LAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Espinoso entity) {
        return TEXTURE;
    }

    @Override
    protected float getFlipDegrees(Espinoso entity) {
        return 0.0F;
    }

    @Override
    protected void scale(Espinoso entity, PoseStack pose, float pt) {
        float s = entity.deathTime > 0 ? Math.max(0.05F, 1.0F - (entity.deathTime + pt) / 20.0F) : 1.0F;
        pose.scale(s, s, s);
    }
}
