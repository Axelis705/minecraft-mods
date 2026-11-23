package com.coleccionista.client;

import com.coleccionista.evento.Cadaver;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * (Cliente) El cuerpo de una pieza eliminada: el modelo de jugador con su propia piel, boca arriba en el suelo, con los brazos y piernas
 * un poco abiertos (cada uno un poco distinto) y algo pálido.
 */
public class CadaverRenderer extends EntityRenderer<Cadaver> {
    private final PlayerModel<AbstractClientPlayer> ancho, delgado;

    public CadaverRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        ancho = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
        delgado = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        shadowRadius = 0.6F;
        shadowStrength = 0.8F;
    }

    private static PlayerInfo info(UUID id) {
        var c = Minecraft.getInstance().getConnection();
        return id == null || c == null ? null : c.getPlayerInfo(id);
    }

    @Override
    public ResourceLocation getTextureLocation(Cadaver c) {
        UUID id = c.dueno();
        PlayerInfo i = info(id);
        return i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id == null ? c.getUUID() : id);
    }

    @Override
    public void render(Cadaver c, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        UUID id = c.dueno() == null ? c.getUUID() : c.dueno();
        PlayerInfo i = info(id);
        boolean slim = i != null ? "slim".equals(i.getModelName()) : "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
        PlayerModel<AbstractClientPlayer> m = slim ? delgado : ancho;
        m.setAllVisible(true);
        m.young = false;
        m.crouching = false;
        m.riding = false;
        int pos = c.postura();
        m.head.setRotation(-0.25F, (pos - 1.5F) * 0.35F, 0);
        m.body.setRotation(0, 0, 0);
        m.rightArm.setRotation(-0.15F, 0, 0.4F + pos * 0.1F);
        m.leftArm.setRotation(pos % 2 == 0 ? -0.6F : -0.1F, 0, -0.3F - pos * 0.08F);
        m.rightLeg.setRotation(0, 0, 0.1F + pos * 0.03F);
        m.leftLeg.setRotation(0.05F, 0, -0.14F);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - c.getYRot()));
        // boca arriba: el cuerpo acostado a lo largo, centrado en la entidad y apoyado en el suelo
        ps.translate(0, 0.13, -0.9);
        ps.mulPose(Axis.XP.rotationDegrees(90));
        ps.scale(-1, -1, 1);
        ps.translate(0, -1.501, 0);
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(c))), light, OverlayTexture.NO_OVERLAY, 0.86F,
                0.82F, 0.82F, 1F);
        ps.popPose();
        super.render(c, yaw, pt, ps, buf, light);
    }

    @Override
    protected boolean shouldShowName(Cadaver c) {
        return false;
    }
}
