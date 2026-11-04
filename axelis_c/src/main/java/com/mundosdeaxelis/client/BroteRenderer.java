package com.mundosdeaxelis.client;

import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Brote;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al Brote. */
public class BroteRenderer extends MobRenderer<Brote, BroteModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/brote.png");

    public BroteRenderer(EntityRendererProvider.Context context) {
        super(context, new BroteModel(context.bakeLayer(BroteModel.LAYER)), 0.4F);
    }

    @Override
    public ResourceLocation getTextureLocation(Brote entity) {
        return TEXTURE;
    }
}
