package com.kensei.client;

import com.kensei.Kensei;
import com.kensei.entity.RivalEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Dibuja al rival de práctica como un espadachín con katana (la katana la pone la capa normal de objetos en la mano). */
public class RivalRenderer extends HumanoidMobRenderer<RivalEntity, RivalModel> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Kensei.MODID, "textures/entity/rival.png");

    public RivalRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new RivalModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(RivalEntity e) {
        return TEXTURE;
    }
}
