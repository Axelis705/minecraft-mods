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
        if (c.enSilla()) {
            enSilla(c, m, slim, id, ps, buf, light, pt);
            super.render(c, yaw, pt, ps, buf, light);
            return;
        }
        if (c.veneno() || c.enJaula()) {
            envenenado(c, m, slim, id, ps, buf, light, pt);
            super.render(c, yaw, pt, ps, buf, light);
            return;
        }
        if (c.estatua()) {
            estatua(c, m, slim, id, ps, buf, light, pt);
            super.render(c, yaw, pt, ps, buf, light);
            return;
        }
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
        // con su uniforme (si lo llevaba)
        com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
        com.axeliscore.vestir.client.VestuarioCliente.dibujarJugador(id, c, slim, new net.minecraft.client.model.geom.ModelPart[] {m.head, m.body,
                m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}, ps, d, light, OverlayTexture.NO_OVERLAY, 1F, false);
        d.flush();
        ps.popPose();
        super.render(c, yaw, pt, ps, buf, light);
    }

    private static net.minecraft.world.level.block.state.BlockState silla, correa;

    /**
     * El que recibió el balazo en el círculo: sentado en su silla (atado), con la cabeza echada para atrás y los brazos colgando. Desde el
     * disparo, la silla se va de espaldas con él (gira sobre las patas de atrás, cada vez más rápido), pega en el piso y rebota un poquito.
     */
    private void enSilla(Cadaver c, PlayerModel<AbstractClientPlayer> m, boolean slim, UUID id, PoseStack ps, MultiBufferSource buf, int light,
            float pt) {
        if (silla == null) {
            silla = com.coleccionista.evento.Complejo.estado("coleccionista:silla[facing=north]");
            correa = com.coleccionista.evento.Complejo.estado("coleccionista:cadena[axis=x]");
        }
        int v = c.postura() - Cadaver.EN_SILLA;
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - c.getYRot()));
        float ang = 0F;
        if (c.cae() > 0 && c.level() != null) {
            float t = (c.level().getGameTime() - c.cae() + pt) / 9F;
            if (t < 1F) {
                ang = 84F * t * t;
            } else {
                float r = Math.min(1F, (t - 1F) / 0.45F);
                ang = 84F - 9F * (float) Math.sin(r * Math.PI);
            }
        }
        // gira sobre la orilla de atrás (las patas de atrás): atrás es +z aquí
        ps.translate(0, 0, 0.42F);
        ps.mulPose(Axis.XP.rotationDegrees(ang));
        ps.translate(0, 0, -0.42F);
        // la silla (con sus correas)
        if (silla != null) {
            ps.pushPose();
            ps.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(silla, ps, buf, light, OverlayTexture.NO_OVERLAY);
            if (correa != null) {
                ps.translate(0, 0.38, 0.08);
                ps.scale(1F, 0.6F, 0.6F);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(correa, ps, buf, light, OverlayTexture.NO_OVERLAY);
            }
            ps.popPose();
        }
        // el cuerpo, sentado: como uno vivo en la silla, pero flojo
        m.head.setRotation(-0.55F - 0.08F * v, 0.18F * (v - 1), 0.1F * (v - 1));
        m.body.setRotation(0, 0, 0);
        m.rightArm.setRotation(0.15F + 0.1F * v, 0, 0.22F);
        m.leftArm.setRotation(0.1F, 0, -0.2F - 0.06F * v);
        m.rightLeg.setRotation(-1.4137167F, 0.31415927F, 0.07853982F);
        m.leftLeg.setRotation(-1.4137167F, -0.31415927F, -0.07853982F);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        ps.translate(0, 0.07, 0);
        ps.scale(-1, -1, 1);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501, 0);
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(c))), light, OverlayTexture.NO_OVERLAY, 0.86F,
                0.82F, 0.82F, 1F);
        com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
        com.axeliscore.vestir.client.VestuarioCliente.dibujarJugador(id, c, slim, new net.minecraft.client.model.geom.ModelPart[] {m.head, m.body,
                m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}, ps, d, light, OverlayTexture.NO_OVERLAY, 1F, false);
        d.flush();
        ps.popPose();
    }

    /** El verde del veneno (multiplica la piel: queda verde enfermizo). */
    public static final float VERDE_R = 0.52F, VERDE_G = 0.86F, VERDE_B = 0.44F;

    /**
     * El envenenado del salón de té. En su silla: con las manos en el cuello y la cabeza echada para atrás; desde que cae, la silla se va de
     * espaldas con él (como el del balazo). En su jaula: sentado en el piso de la jaula, con las piernas al frente, los brazos caídos y la
     * cabeza colgando. Siempre verde.
     */
    private void envenenado(Cadaver c, PlayerModel<AbstractClientPlayer> m, boolean slim, UUID id, PoseStack ps, MultiBufferSource buf,
            int light, float pt) {
        boolean jaula = c.enJaula();
        int v = c.postura() - (jaula ? Cadaver.JAULA : Cadaver.VENENO);
        if (silla == null) {
            silla = com.coleccionista.evento.Complejo.estado("coleccionista:silla[facing=north]");
            correa = com.coleccionista.evento.Complejo.estado("coleccionista:cadena[axis=x]");
        }
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - c.getYRot()));
        if (!jaula) {
            float ang = 0F;
            if (c.cae() > 0 && c.level() != null) {
                float t = (c.level().getGameTime() - c.cae() + pt) / 9F;
                if (t < 1F) {
                    ang = 84F * t * t;
                } else {
                    float r = Math.min(1F, (t - 1F) / 0.45F);
                    ang = 84F - 9F * (float) Math.sin(r * Math.PI);
                }
            }
            ps.translate(0, 0, 0.42F);
            ps.mulPose(Axis.XP.rotationDegrees(ang));
            ps.translate(0, 0, -0.42F);
            if (silla != null) {
                ps.pushPose();
                ps.translate(-0.5, 0, -0.5);
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(silla, ps, buf, light, OverlayTexture.NO_OVERLAY);
                ps.popPose();
            }
            // ahogándose: las manos al cuello, la cabeza para atrás
            m.head.setRotation(-0.6F - 0.08F * v, 0.2F * (v - 1), 0.12F * (v - 1));
            m.body.setRotation(0, 0, 0);
            m.rightArm.setRotation(-2.05F, -0.55F, 0.1F);
            m.leftArm.setRotation(-1.95F, 0.6F, -0.1F);
            m.rightLeg.setRotation(-1.4137167F, 0.31415927F, 0.07853982F);
            m.leftLeg.setRotation(-1.4137167F, -0.31415927F, -0.07853982F);
            ps.translate(0, 0.07, 0);
        } else {
            // en la jaula: sentado en el piso, las piernas al frente, los brazos caídos y la cabeza colgando a un lado
            m.head.setRotation(0.75F + 0.05F * v, 0.25F * (v - 1), 0.35F * (v % 2 == 0 ? 1 : -1));
            m.body.setRotation(0, 0, 0);
            m.rightArm.setRotation(-0.35F, 0, 0.32F + 0.05F * v);
            m.leftArm.setRotation(-0.3F, 0, -0.3F - 0.05F * v);
            m.rightLeg.setRotation(-1.5F, 0.22F, 0.05F);
            m.leftLeg.setRotation(-1.5F, -0.18F, -0.05F);
            ps.translate(0, -0.55, 0);
        }
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        ps.scale(-1, -1, 1);
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.translate(0, -1.501, 0);
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(getTextureLocation(c))), light, OverlayTexture.NO_OVERLAY, VERDE_R,
                VERDE_G, VERDE_B, 1F);
        com.axeliscore.plat.client.Draw d = new com.axeliscore.plat.client.Draw(buf);
        com.axeliscore.vestir.client.VestuarioCliente.dibujarJugador(id, c, slim, new net.minecraft.client.model.geom.ModelPart[] {m.head, m.body,
                m.rightArm, m.leftArm, m.rightLeg, m.leftLeg}, ps, d, light, OverlayTexture.NO_OVERLAY, 1F, false);
        d.flush();
        ps.popPose();
    }

    private static final ResourceLocation PIEDRA = com.axeliscore.plat.Ids.rl(com.coleccionista.Coleccionista.MODID,
            "textures/entity/estatua_piedra.png");

    /**
     * La estatua de la vitrina: la cera endurecida en piedra con la forma de la pieza. Es su piel hecha piedra (los claros y oscuros de la
     * piel, en gris: se reconoce quién es) con grano y grietas encima. De pie, rígida, con la cabeza echada para atrás y los brazos a medio
     * subir (el grito). Cuando se rompe el vidrio se va de frente, cada vez más rápido, y queda acostada boca abajo (con un rebote).
     */
    private void estatua(Cadaver c, PlayerModel<AbstractClientPlayer> m, boolean slim, UUID id, PoseStack ps, MultiBufferSource buf, int light,
            float pt) {
        int v = c.postura() - Cadaver.ESTATUA;
        m.head.setRotation(-0.35F - 0.05F * (v % 3), 0.12F * ((v % 3) - 1), 0);
        m.body.setRotation(0, 0, 0);
        m.rightArm.setRotation(-0.9F - 0.15F * (v % 2), 0.15F, 0.12F);
        m.leftArm.setRotation(-0.7F + 0.1F * (v % 2), -0.15F, -0.12F);
        m.rightLeg.setRotation(0, 0, 0.03F);
        m.leftLeg.setRotation(0, 0, -0.03F);
        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftPants.copyFrom(m.leftLeg);
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(180F - c.getYRot()));
        if (c.cae() > 0 && c.level() != null) {
            // de frente (hacia -z aquí), girando sobre la punta de los pies: acelera, pega en el piso y rebota un poquito
            float t = (c.level().getGameTime() - c.cae() + pt) / 10F;
            float ang;
            if (t < 1F) {
                ang = 90F * t * t;
            } else {
                float r = Math.min(1F, (t - 1F) / 0.4F);
                ang = 90F - 7F * (float) Math.sin(r * Math.PI);
            }
            ps.translate(0, 0, -0.16F);
            ps.mulPose(Axis.XP.rotationDegrees(-ang));
            ps.translate(0, 0, 0.16F);
        }
        ps.scale(-1, -1, 1);
        ps.translate(0, -1.501, 0);
        // la piel hecha piedra (en gris, con sus claros y oscuros)
        ResourceLocation piel = EstatuaPiel.de(getTextureLocation(c));
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(piel)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F);
        // y una capa de piedra encima, un poquito más grande (grietas y grano), casi transparente para que se vea quién es
        ps.scale(1.03F, 1.015F, 1.03F);
        ps.translate(0, -0.01, 0);
        m.renderToBuffer(ps, buf.getBuffer(RenderType.entityTranslucent(PIEDRA)), light, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 0.32F);
        ps.popPose();
    }

    @Override
    protected boolean shouldShowName(Cadaver c) {
        return false;
    }
}
