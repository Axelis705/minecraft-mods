package com.navidad.client;

import com.axeliscore.npc.Npc;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.navidad.Navidad;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.joml.Matrix4f;

/**
 * (Cliente) El nombre de la gente de Navidad (Santa, la Señora Claus, los duendes, Don Abeto, Ramiro...) en lugar del letrerito negro de
 * Minecraft: un listón rojo con orillas doradas y una ramita de acebo, el nombre en letras claras con contorno y, debajo, en chiquito,
 * quién es ("El duende viajero"). Se ve hasta 10 bloques y se desvanece al alejarte; con F1 se esconde como los nombres normales.
 */
public final class PlacaNombre {
    private static final ResourceLocation TEX = new ResourceLocation(Navidad.MODID, "textures/gui/placa_nombre.png");
    private static final float LEJOS = 10F;

    private PlacaNombre() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(PlacaNombre::alNombre);
        // el Core ya no pone su letrerito del nombre a la gente de Navidad (lo dibuja este listón); con un Core viejo no hace nada
        try {
            com.axeliscore.npc.client.NpcCliente.nombrePropio = d -> d.id.startsWith(Navidad.MODID + ":");
        } catch (LinkageError viejo) {
            Navidad.LOGGER.info("NAVIDAD_PLACA el Core es viejo: se queda también su letrerito");
        }
    }

    private static void alNombre(RenderNameTagEvent e) {
        if (!(e.getEntity() instanceof Npc n) || !n.defId().startsWith(Navidad.MODID + ":")) {
            return;
        }
        e.setResult(Event.Result.DENY);
        Minecraft mc = Minecraft.getInstance();
        if (!Minecraft.renderNames() || n.isInvisible()) {
            return;
        }
        double d = Math.sqrt(mc.getEntityRenderDispatcher().distanceToSqr(n));
        if (d > LEJOS) {
            return;
        }
        float alfa = Mth.clamp((float) (LEJOS - d) / 3F, 0F, 1F);
        String todo = e.getOriginalContent().getString();
        String nombre = todo, quien = "";
        int coma = todo.indexOf(", ");
        if (coma > 0) {
            nombre = todo.substring(0, coma);
            quien = todo.substring(coma + 2);
            if (!quien.isEmpty()) {
                quien = Character.toUpperCase(quien.charAt(0)) + quien.substring(1);
            }
        }
        // arriba del "!" o "?" de las misiones (que el Core pone justo sobre la cabeza)
        dibujar(e.getPoseStack(), e.getMultiBufferSource(), n.getBbHeight() + 1.05F, nombre, quien, alfa);
    }

    /** El listón con el nombre a {@code alto} bloques sobre los pies (la pose ya está en la entidad). */
    static void dibujar(PoseStack ps, MultiBufferSource buf, float alto, String nombre, String quien, float alfa) {
        Minecraft mc = Minecraft.getInstance();
        Font f = mc.font;
        ps.pushPose();
        ps.translate(0F, alto, 0F);
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f m = ps.last().pose();
        int luz = LightTexture.FULL_BRIGHT;
        FormattedCharSequence txt = Component.literal(nombre).withStyle(ChatFormatting.BOLD).getVisualOrderText();
        int w = f.width(txt);
        // el listón (detrás del texto): punta izquierda con el acebo, el centro que se estira y la punta derecha
        float x0 = -w / 2F - 12F, x1 = w / 2F + 12F, y0 = -5.5F, y1 = 13.5F, z = 0.03F;
        int a = (int) (alfa * 255);
        VertexConsumer vc = buf.getBuffer(RenderType.text(TEX));
        quad(vc, m, x0, y0, x0 + 12F, y1, z, 0F, 12F / 32F, a, luz);
        quad(vc, m, x0 + 12F, y0, x1 - 10F, y1, z, 12F / 32F, 22F / 32F, a, luz);
        quad(vc, m, x1 - 10F, y0, x1, y1, z, 22F / 32F, 1F, a, luz);
        // el nombre: claro, con contorno rojo oscuro (se lee de día y de noche)
        if (alfa > 0.1F) {
            int c = (int) (alfa * 255) << 24;
            f.drawInBatch8xOutline(txt, -w / 2F, 0F, c | 0xFFF6D8, c | 0x4A0610, m, buf, luz);
            if (!quien.isEmpty()) {
                ps.pushPose();
                ps.translate(0F, 15F, 0F);
                ps.scale(0.62F, 0.62F, 0.62F);
                int wq = f.width(quien);
                f.drawInBatch(quien, -wq / 2F, 0F, c | 0xF2E6C8, true, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, luz);
                ps.popPose();
            }
        }
        ps.popPose();
    }

    private static void quad(VertexConsumer vc, Matrix4f m, float x0, float y0, float x1, float y1, float z, float u0, float u1, int a, int luz) {
        vc.vertex(m, x0, y0, z).color(255, 255, 255, a).uv(u0, 0F).uv2(luz).endVertex();
        vc.vertex(m, x0, y1, z).color(255, 255, 255, a).uv(u0, 1F).uv2(luz).endVertex();
        vc.vertex(m, x1, y1, z).color(255, 255, 255, a).uv(u1, 1F).uv2(luz).endVertex();
        vc.vertex(m, x1, y0, z).color(255, 255, 255, a).uv(u1, 0F).uv2(luz).endVertex();
    }
}
