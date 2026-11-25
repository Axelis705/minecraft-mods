package com.coleccionista.client;

import com.coleccionista.evento.Complejo;
import com.coleccionista.evento.Silla;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * (Cliente) La silla que se mueve: el modelo de la silla de metal del mod, girado hacia donde mira la entidad. Cuando está cerrada se le ven
 * las correas (unas cadenas cruzadas sobre el asiento).
 */
public class SillaRenderer extends EntityRenderer<Silla> {
    private static BlockState silla, correa;

    public SillaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0.4F;
    }

    @Override
    public ResourceLocation getTextureLocation(Silla s) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    @Override
    public void render(Silla s, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        if (silla == null) {
            silla = Complejo.estado("coleccionista:silla[facing=north]");
            correa = Complejo.estado("coleccionista:cadena[axis=x]");
        }
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - s.getViewYRot(pt)));
        ps.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(silla, ps, buf, light, OverlayTexture.NO_OVERLAY);
        if (s.cerrada() && correa != null) {
            // las correas: una cadena acostada sobre los brazos de la silla
            ps.pushPose();
            ps.translate(0, 0.38, 0.08);
            ps.scale(1F, 0.6F, 0.6F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(correa, ps, buf, light, OverlayTexture.NO_OVERLAY);
            ps.popPose();
        }
        ps.popPose();
        super.render(s, yaw, pt, ps, buf, light);
    }
}
