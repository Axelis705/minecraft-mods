package com.coleccionista.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * (Cliente) Lo que se ve de cada papel en la prueba de los sentidos, sobre el jugador: la <b>venda</b> negra en los ojos, la <b>cinta</b>
 * gris en la boca y las <b>esposas</b> (dos argollas de metal en las muñecas unidas por una cadena corta).
 */
public class RolesCapa extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation BLANCO = com.axeliscore.plat.client.Draw.BLANCO;
    private static final ResourceLocation CADENA = new ResourceLocation("minecraft", "textures/block/chain.png");

    public RolesCapa(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) {
        super(p);
    }

    /** Se agrega a los dos modelos de jugador (ancho y delgado). */
    public static void capas(EntityRenderersEvent.AddLayers e) {
        for (String skin : e.getSkins()) {
            if (e.getSkin(skin) instanceof PlayerRenderer r) {
                r.addLayer(new RolesCapa(r));
            }
        }
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int luz, AbstractClientPlayer p, float a, float b, float pt, float c, float d,
            float e) {
        String rol = SentidosCliente.rolDe(p.getUUID());
        if (rol.isEmpty() || "NADA".equals(rol) || p.isInvisible()) {
            return;
        }
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(BLANCO));
        switch (rol) {
            case "VENDA" -> {
                // una tira negra alrededor de la cabeza, a la altura de los ojos, con el nudo atrás
                ps.pushPose();
                m.head.translateAndRotate(ps);
                caja(vc, ps, -4.4F, -5.6F, -4.4F, 4.4F, -3.6F, 4.4F, 0xFF151515, luz);
                caja(vc, ps, -1F, -5.3F, 4.4F, 1F, -2.8F, 5.2F, 0xFF101010, luz);
                ps.popPose();
            }
            case "CINTA" -> {
                // un pedazo de cinta gris sobre la boca, un poco chueco
                ps.pushPose();
                m.head.translateAndRotate(ps);
                caja(vc, ps, -2.8F, -2.6F, -4.45F, 2.8F, -1.0F, -4.2F, 0xFFB8B4AA, luz);
                caja(vc, ps, -2.9F, -2.1F, -4.5F, -2.3F, -1.4F, -4.4F, 0xFF9C988E, luz);
                ps.popPose();
            }
            case "ESPOSAS" -> {
                // argollas en las muñecas y una cadena corta entre ellas
                float[] der = muneca(ps, m.rightArm, -1F), izq = muneca(ps, m.leftArm, 1F);
                ps.pushPose();
                m.rightArm.translateAndRotate(ps);
                caja(vc, ps, -3.6F, 8.0F, -2.6F, 1.6F, 9.6F, 2.6F, 0xFF8A8E94, luz);
                ps.popPose();
                ps.pushPose();
                m.leftArm.translateAndRotate(ps);
                caja(vc, ps, -1.6F, 8.0F, -2.6F, 3.6F, 9.6F, 2.6F, 0xFF8A8E94, luz);
                ps.popPose();
                VertexConsumer vk = buf.getBuffer(RenderType.entityCutoutNoCull(CADENA));
                PoseStack id = new PoseStack();
                CadenaRender.cadena(vk, id, new net.minecraft.world.phys.Vec3(der[0], der[1], der[2]),
                        new net.minecraft.world.phys.Vec3(izq[0], izq[1], izq[2]), 0.55, -1e9, luz);
            }
            default -> {
            }
        }
    }

    /** Dónde queda la muñeca de un brazo (en el espacio de la cámara, para dibujar la cadena con una pila vacía). */
    private static float[] muneca(PoseStack ps, net.minecraft.client.model.geom.ModelPart brazo, float lado) {
        ps.pushPose();
        brazo.translateAndRotate(ps);
        org.joml.Vector4f v = new org.joml.Vector4f(lado * 0.5F / 16F, 9F / 16F, 0F, 1F).mul(ps.last().pose());
        ps.popPose();
        return new float[] {v.x(), v.y(), v.z()};
    }

    /** Una caja de color (medidas en pixeles del modelo, 1/16 de bloque). */
    private static void caja(VertexConsumer vc, PoseStack ps, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int luz) {
        float s = 1 / 16F;
        x0 *= s;
        y0 *= s;
        z0 *= s;
        x1 *= s;
        y1 *= s;
        z1 *= s;
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255, a = argb >>> 24;
        // seis caras
        cara(vc, m, n, r, g, b, a, luz, 0, -1, 0, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);
        cara(vc, m, n, r, g, b, a, luz, 0, 1, 0, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);
        cara(vc, m, n, r, g, b, a, luz, 0, 0, -1, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0);
        cara(vc, m, n, r, g, b, a, luz, 0, 0, 1, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1);
        cara(vc, m, n, r, g, b, a, luz, -1, 0, 0, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0);
        cara(vc, m, n, r, g, b, a, luz, 1, 0, 0, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1);
    }

    private static void cara(VertexConsumer vc, Matrix4f m, Matrix3f n, int r, int g, int b, int a, int luz, float nx, float ny, float nz,
            float... p) {
        for (int i = 0; i < 4; i++) {
            vc.vertex(m, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(luz)
                    .normal(n, nx, ny, nz).endVertex();
        }
    }
}
