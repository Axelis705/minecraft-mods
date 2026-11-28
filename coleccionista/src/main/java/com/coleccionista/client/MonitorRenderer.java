package com.coleccionista.client;

import com.axeliscore.plat.Ids;
import com.axeliscore.plat.client.Draw;
import com.coleccionista.Coleccionista;
import com.coleccionista.hotel.Monitor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * (Cliente) La imagen de un monitor de la sala de espera, sobre el vidrio de la tele vieja: la cara de quien sigue en el hotel (con su
 * piel), su nombre, en qué piso va y su vida, con tinte verdoso de cámara de seguridad, rayas que bajan, el número de cámara y la luz
 * roja de "en vivo". Si no muestra a nadie: estática y "SIN SEÑAL".
 */
public class MonitorRenderer extends EntityRenderer<Monitor> {
    private static final ResourceLocation BLANCO = Draw.BLANCO;
    private static final ResourceLocation ESTATICA = Ids.rl(Coleccionista.MODID, "textures/pantalla/estatica.png");
    private static final int LUZ = 0xF000F0;

    public MonitorRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        shadowRadius = 0F;
    }

    @Override
    public ResourceLocation getTextureLocation(Monitor m) {
        return BLANCO;
    }

    @Override
    protected boolean shouldShowName(Monitor m) {
        return false;
    }

    private static ResourceLocation piel(UUID id) {
        var c = Minecraft.getInstance().getConnection();
        PlayerInfo i = c == null ? null : c.getPlayerInfo(id);
        return i != null ? i.getSkinLocation() : DefaultPlayerSkin.getDefaultSkin(id);
    }

    @Override
    public void render(Monitor m, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-m.getYRot()));
        // el vidrio de la tele (el modelo mira al sur): centro en x +1 px, y 7 px, al frente
        ps.translate(1F / 16F, 7F / 16F, 14.7F / 16F - 0.5F);
        float w = 9.2F / 16F, h = 7.6F / 16F, z = 0F;
        Draw d = new Draw(buf);
        long ms = System.currentTimeMillis();
        UUID q = m.quien();
        Font font = Minecraft.getInstance().font;
        if (q == null) {
            float u = (ms / 40 % 7) * 0.13F, v = (ms / 40 % 5) * 0.17F;
            quad(luz(buf, ESTATICA), ps, -w / 2, -h / 2, w / 2, h / 2, z, u, v, u + 0.35F, v + 0.3F, 0xFF9A9A9A, 0xFFE0E0E0);
            texto(d, ps, font, net.minecraft.network.chat.Component.translatable(Coleccionista.MODID + ".hotel.monitor.sin").getString(), 0,
                    -0.02F, 0.0042F, 0xFFE8E8E8, z + 0.003F, true);
        } else {
            // el fondo de la cámara (un pasillo oscuro, verdoso)
            quad(luz(buf, BLANCO), ps, -w / 2, -h / 2, w / 2, h / 2, z, 0, 0, 1, 1, 0xFF0A140E, 0xFF1C2E22);
            // la cara (piel y gorra), tintada como cámara vieja
            float lado = h * 0.62F, x0 = -w / 2 + 0.035F, y0 = -h / 2 + 0.07F;
            ResourceLocation p = piel(q);
            quad(luz(buf, p), ps, x0, y0, x0 + lado, y0 + lado, z + 0.002F, 8F / 64F, 8F / 64F, 16F / 64F, 16F / 64F, 0xFFB8E6C0, 0xFFC8F2CE);
            quad(luz(buf, p), ps, x0 - 0.01F, y0 - 0.01F, x0 + lado + 0.01F, y0 + lado + 0.01F, z + 0.003F, 40F / 64F, 8F / 64F, 48F / 64F,
                    16F / 64F, 0xFFB8E6C0, 0xFFC8F2CE);
            // nombre, piso y vida
            float tx = x0 + lado + 0.03F;
            String n = m.nombre();
            float esc = Math.min(0.0034F, (w / 2 - tx - 0.02F) / Math.max(1, font.width(n)));
            texto(d, ps, font, n, tx, h / 2 - 0.12F, esc, 0xFFE8FFE8, z + 0.004F, false);
            String piso = m.linea().isEmpty() ? "" : net.minecraft.network.chat.Component.translatable(Coleccionista.MODID + ".hotel.monitor.piso",
                    m.linea()).getString();
            texto(d, ps, font, piso, tx, h / 2 - 0.2F, 0.0026F, 0xFF9AD8A0, z + 0.004F, false);
            float vida = Math.max(0, Math.min(1, m.vida() / 20F));
            float bw = w / 2 - tx - 0.03F;
            quad(luz(buf, BLANCO), ps, tx, -h / 2 + 0.07F, tx + bw, -h / 2 + 0.1F, z + 0.004F, 0, 0, 1, 1, 0xFF301010, 0xFF301010);
            quad(luz(buf, BLANCO), ps, tx, -h / 2 + 0.07F, tx + bw * vida, -h / 2 + 0.1F, z + 0.005F, 0, 0, 1, 1, 0xFFB3121A, 0xFFFF4A4A);
            // "EN VIVO" que parpadea
            if (ms / 600 % 2 == 0) {
                float r = 0.012F, cx = w / 2 - 0.035F, cy = h / 2 - 0.035F;
                quad(luz(buf, Draw.ORBE), ps, cx - r, cy - r, cx + r, cy + r, z + 0.006F, 0, 0, 1, 1, 0xFFFF2020, 0xFFFF4040);
            }
        }
        // el número de la cámara y las rayas de tele
        texto(d, ps, font, String.format("CAM %02d", m.numero()), -w / 2 + 0.02F, h / 2 - 0.045F, 0.0022F, 0xC0FFFFFF, z + 0.006F, false);
        float paso = h / 26F, corre = (ms % 2000) / 2000F * paso;
        for (float y = -h / 2 + corre; y < h / 2; y += paso) {
            quad(luz(buf, BLANCO), ps, -w / 2, y, w / 2, Math.min(h / 2, y + paso * 0.35F), z + 0.007F, 0, 0, 1, 1, 0x26000000, 0x26000000);
        }
        // el reflejo del vidrio
        quad(luz(buf, BLANCO), ps, -w / 2, h / 2 - h * 0.22F, w / 2, h / 2, z + 0.008F, 0, 0, 1, 1, 0x00FFFFFF, 0x12FFFFFF);
        d.flush();
        ps.popPose();
        super.render(m, yaw, pt, ps, buf, light);
    }

    private static void texto(Draw d, PoseStack ps, Font font, String t, float x, float y, float esc, int color, float z, boolean centro) {
        ps.pushPose();
        ps.translate(x, y, z);
        ps.scale(esc, -esc, esc);
        float dx = centro ? -font.width(t) / 2F : 0F;
        d.text(ps, t, dx, -4, color, 0, LUZ);
        ps.popPose();
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, PoseStack ps, float x0, float y0, float x1, float y1, float z, float u0,
            float v0, float u1, float v1, int c0, int c1) {
        PoseStack.Pose p = ps.last();
        org.joml.Matrix4f m = p.pose();
        org.joml.Matrix3f n = p.normal();
        int a0 = c0 >>> 24, r0 = (c0 >> 16) & 255, g0 = (c0 >> 8) & 255, b0 = c0 & 255;
        int a1 = c1 >>> 24, r1 = (c1 >> 16) & 255, g1 = (c1 >> 8) & 255, b1 = c1 & 255;
        vc.vertex(m, x0, y0, z).color(r0, g0, b0, a0).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LUZ).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y0, z).color(r0, g0, b0, a0).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LUZ).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x1, y1, z).color(r1, g1, b1, a1).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LUZ).normal(n, 0, 0, 1).endVertex();
        vc.vertex(m, x0, y1, z).color(r1, g1, b1, a1).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LUZ).normal(n, 0, 0, 1).endVertex();
    }

    /** El pincel de la pantalla: brilla solo y deja ver lo transparente (pedirlo justo antes de usarlo). */
    private static com.mojang.blaze3d.vertex.VertexConsumer luz(MultiBufferSource buf, ResourceLocation tex) {
        return buf.getBuffer(net.minecraft.client.renderer.RenderType.entityTranslucentEmissive(tex));
    }
}
