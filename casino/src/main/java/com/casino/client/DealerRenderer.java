package com.casino.client;

import com.casino.Casino;
import com.casino.entity.DealerEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** El crupier se dibuja como una persona (modelo de jugador) con su propia piel: camisa blanca, chaleco negro y moño rojo. */
public class DealerRenderer extends MobRenderer<DealerEntity, PlayerModel<DealerEntity>> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Casino.MODID, "textures/entity/dealer.png");

    private static final ResourceLocation TEXTURE_ROULETTE = new ResourceLocation(Casino.MODID, "textures/entity/dealer_roulette.png");

    public DealerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.45F);
    }

    @Override
    public ResourceLocation getTextureLocation(DealerEntity entity) {
        return entity.getGame() == DealerEntity.GAME_ROULETTE ? TEXTURE_ROULETTE : TEXTURE;
    }
}
