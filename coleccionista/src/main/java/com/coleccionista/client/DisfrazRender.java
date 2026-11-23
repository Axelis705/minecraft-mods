package com.coleccionista.client;

import com.axeliscore.plat.Ids;
import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * (Cliente) El organizador disfrazado del Coleccionista: se dibuja con la piel del Coleccionista (al triple de detalle, con los mismos
 * dibujos que en la tele: máscara de porcelana con grieta y lágrima, frac, moño, guantes), los puntos rojos de los ojos que brillan en
 * la oscuridad y un sombrero de copa, con todas las animaciones normales del jugador, y su nombre dice "El Coleccionista".
 */
public final class DisfrazRender {
    static final ResourceLocation PIEL = Ids.rl(Coleccionista.MODID, "textures/entity/coleccionista.png");
    static final ResourceLocation SOMBRERO = Ids.rl(Coleccionista.MODID, "textures/entity/sombrero.png");
    /** Los puntos rojos de los ojos (brillan en la oscuridad, como en la tele). */
    static final ResourceLocation OJOS = Ids.rl(Coleccionista.MODID, "textures/entity/coleccionista_ojos.png");
    private static Disfrazado renderer;

    private DisfrazRender() {
    }

    /** Se arma con el contexto de los renderizadores (bus del mod). */
    public static void capas(EntityRenderersEvent.AddLayers e) {
        renderer = new Disfrazado(e.getContext());
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(DisfrazRender::antes);
    }

    /** El dibujo del disfraz vuelve a lanzar el mismo evento: mientras dibujamos, se deja pasar (si no, se llamaría sin fin). */
    private static boolean dibujando;

    private static void antes(RenderPlayerEvent.Pre e) {
        if (dibujando || renderer == null || !EventoCliente.disfrazado(e.getEntity().getUUID())
                || EventoCliente.esFantasma(e.getEntity().getUUID())) {
            return;
        }
        if (!(e.getEntity() instanceof AbstractClientPlayer p)) {
            return;
        }
        e.setCanceled(true);
        dibujando = true;
        try {
            renderer.render(p, p.getViewYRot(e.getPartialTick()), e.getPartialTick(), e.getPoseStack(), e.getMultiBufferSource(),
                    e.getPackedLight());
        } finally {
            dibujando = false;
        }
    }

    /** Un jugador con la piel del Coleccionista. */
    static final class Disfrazado extends PlayerRenderer {
        Disfrazado(EntityRendererProvider.Context ctx) {
            super(ctx, false);
            addLayer(new Sombrero(this));
            addLayer(new net.minecraft.client.renderer.entity.layers.EyesLayer<>(this) {
                @Override
                public RenderType renderType() {
                    return RenderType.eyes(OJOS);
                }
            });
        }

        @Override
        public ResourceLocation getTextureLocation(AbstractClientPlayer p) {
            return PIEL;
        }

        @Override
        protected void renderNameTag(AbstractClientPlayer p, Component name, PoseStack ps, MultiBufferSource buf, int light) {
            super.renderNameTag(p, Component.literal("El Coleccionista").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), ps, buf,
                    light);
        }
    }

    /** El sombrero de copa: ala ancha y copa alta con cinta roja, pegado a la cabeza. */
    static final class Sombrero extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        Sombrero(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> p) {
            super(p);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, float swing, float swingAmt, float pt,
                float age, float headYaw, float headPitch) {
            ps.pushPose();
            getParentModel().head.translateAndRotate(ps);
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(SOMBRERO));
            // en píxeles del modelo: la cabeza va de y -8 (arriba) a 0; el ala un poco más ancha que la cabeza
            caja(vc, ps, -5.5F, -8.7F, -5.5F, 5.5F, -8.0F, 5.5F, 0, 11, 32, 16, light);
            caja(vc, ps, -3.6F, -15.5F, -3.6F, 3.6F, -8.7F, 3.6F, 0, 0, 32, 11, light);
            ps.popPose();
        }

        /** Una caja (en píxeles) con toda su superficie usando la parte {@code u0,v0..u1,v1} de la textura (32x16). */
        private static void caja(VertexConsumer vc, PoseStack ps, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0,
                float u1, float v1, int light) {
            float a = x0 / 16F, b = y0 / 16F, c = z0 / 16F, d = x1 / 16F, e = y1 / 16F, f = z1 / 16F;
            float uA = u0 / 32F, uB = u1 / 32F, vA = v0 / 16F, vB = v1 / 16F;
            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            // frente (-z), atrás (+z), izquierda (-x), derecha (+x), arriba (-y), abajo (+y)
            cara(vc, m, n, a, b, c, d, b, c, d, e, c, a, e, c, uA, vA, uB, vB, 0, 0, -1, light);
            cara(vc, m, n, d, b, f, a, b, f, a, e, f, d, e, f, uA, vA, uB, vB, 0, 0, 1, light);
            cara(vc, m, n, a, b, f, a, b, c, a, e, c, a, e, f, uA, vA, uB, vB, -1, 0, 0, light);
            cara(vc, m, n, d, b, c, d, b, f, d, e, f, d, e, c, uA, vA, uB, vB, 1, 0, 0, light);
            cara(vc, m, n, a, b, f, d, b, f, d, b, c, a, b, c, uA, vA, uA + 0.01F, vA + 0.01F, 0, -1, 0, light);
            cara(vc, m, n, a, e, c, d, e, c, d, e, f, a, e, f, uA, vA, uA + 0.01F, vA + 0.01F, 0, 1, 0, light);
        }

        private static void cara(VertexConsumer vc, Matrix4f m, Matrix3f n, float x1, float y1, float z1, float x2, float y2, float z2, float x3,
                float y3, float z3, float x4, float y4, float z4, float u0, float v0, float u1, float v1, float nx, float ny, float nz, int light) {
            vc.vertex(m, x1, y1, z1).color(255, 255, 255, 255).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, -ny, nz)
                    .endVertex();
            vc.vertex(m, x2, y2, z2).color(255, 255, 255, 255).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, -ny, nz)
                    .endVertex();
            vc.vertex(m, x3, y3, z3).color(255, 255, 255, 255).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, -ny, nz)
                    .endVertex();
            vc.vertex(m, x4, y4, z4).color(255, 255, 255, 255).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(n, nx, -ny, nz)
                    .endVertex();
        }
    }
}
