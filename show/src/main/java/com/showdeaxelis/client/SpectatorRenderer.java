package com.showdeaxelis.client;

import com.showdeaxelis.ShowMod;
import com.showdeaxelis.entity.SpectatorEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class SpectatorRenderer extends HumanoidMobRenderer<SpectatorEntity, SpectatorModel> {
    private static final ResourceLocation[] SKINS = new ResourceLocation[SpectatorEntity.SKINS];

    static {
        for (int i = 0; i < SKINS.length; i++) {
            SKINS[i] = new ResourceLocation(ShowMod.MOD_ID, "textures/entity/spectator_" + i + ".png");
        }
    }

    public SpectatorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new SpectatorModel(ctx.bakeLayer(ModelLayers.PLAYER)), 0.4f);
    }

    @Override
    public ResourceLocation getTextureLocation(SpectatorEntity e) {
        return SKINS[e.skin()];
    }
}
