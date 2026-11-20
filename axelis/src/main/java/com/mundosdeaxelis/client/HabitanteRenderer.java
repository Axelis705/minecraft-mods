package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mundosdeaxelis.MundosDeAxelis;
import com.mundosdeaxelis.entity.Habitante;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * Habitantes (etapa 257): cuerpo de jugador de brazos finos (el de Alex) con su propia piel de 64 × 64, un poco más bajito que un
 * jugador. La piel sale de {@code textures/entity/npc/<id>.png}. En los retratos de la ventana no se dibuja su nombre.
 */
public class HabitanteRenderer extends HumanoidMobRenderer<Habitante, PlayerModel<Habitante>> {
    static boolean gui;

    public HabitanteRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Habitante e) {
        String k = e.kindId();
        return new ResourceLocation(MundosDeAxelis.MODID, "textures/entity/npc/" + (k == null || k.isEmpty() ? "ruda" : k) + ".png");
    }

    @Override
    protected void scale(Habitante e, PoseStack pose, float pt) {
        pose.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Override
    protected boolean shouldShowName(Habitante e) {
        return !gui && super.shouldShowName(e);
    }
}
