package com.coleccionista.client;

import com.axeliscore.anim.AnimHost;
import com.axeliscore.client.SimpleGeoRenderer;
import com.coleccionista.Coleccionista;
import com.coleccionista.evento.Maquina;
import com.coleccionista.mapa.SalaVeintiuno;
import com.coleccionista.prueba.Veintiuno;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * (Cliente) La máquina de latón de la final (se mueve con {@link Maquina#axPose}); bien iluminada, como la mesa. Ya que cortó, encima se
 * queda la mitad del brazo de quien la tenía puesta (de la mitad a la mano, con su piel y el corte rojo), justo donde estaba.
 */
public class MaquinaRenderer extends SimpleGeoRenderer<Maquina> {
    private PlayerModel<AbstractClientPlayer> ancho, delgado;

    public MaquinaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, Coleccionista.MODID, "maquina", 0.4F, 1F);
    }

    @Override
    protected void setupLayers(AnimHost h) {
        h.animator().layer("base");
    }

    @Override
    public void render(Maquina m, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luzMundo) {
        int light = LightTexture.pack(Math.max(LightTexture.block(luzMundo), 12), LightTexture.sky(luzMundo));
        super.render(m, yaw, pt, ps, buf, light);
        if (m.cortado(pt) && m.dueno().isPresent()) {
            antebrazo(m, m.dueno().get(), ps, buf, light);
        }
    }

    /** La mitad del brazo que se quedó: en la misma pose que tenía sobre la máquina (se calcula desde el lugar de la silla). */
    private void antebrazo(Maquina m, UUID id, PoseStack ps, MultiBufferSource buf, int light) {
        Minecraft mc = Minecraft.getInstance();
        if (ancho == null) {
            ancho = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
            delgado = new PlayerModel<>(mc.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
        PlayerInfo i = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(id);
        boolean slim = i != null ? "slim".equals(i.getModelName()) : "slim".equals(DefaultPlayerSkin.getSkinModelName(id));
        ResourceLocation piel = i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
        PlayerModel<AbstractClientPlayer> pm = slim ? delgado : ancho;
        float y = m.getYRot();
        double r = Math.toRadians(y);
        double fx = -Math.sin(r), fz = Math.cos(r), ix = fz, iz = -fx;
        // el lugar de la silla, desde la máquina (al revés que Veintiuno.posMaquina)
        double dx = -fx * Veintiuno.MAQ_FRENTE - ix * Veintiuno.MAQ_LADO, dz = -fz * Veintiuno.MAQ_FRENTE - iz * Veintiuno.MAQ_LADO;
        double dy = 1 - SalaVeintiuno.TAPETE;
        ps.pushPose();
        ps.translate(dx, dy, dz);
        ps.mulPose(Axis.YP.rotationDegrees(180F - y));
        ps.mulPose(Axis.XP.rotationDegrees(Corte.INCLINA));
        ps.translate(0, 0.07, 0);
        ps.scale(-1, -1, 1);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501, 0);
        pm.leftArm.setRotation(Corte.AX, Corte.AY, Corte.AZ);
        pm.leftSleeve.copyFrom(pm.leftArm);
        Corte.antebrazo(pm, ps, buf.getBuffer(RenderType.entityCutoutNoCull(piel)), light);
        ps.popPose();
    }
}
