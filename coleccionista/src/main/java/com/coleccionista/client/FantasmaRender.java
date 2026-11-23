package com.coleccionista.client;

import com.axeliscore.anim.Geo;
import com.axeliscore.anim.Pose;
import com.axeliscore.anim.client.GeoAssets;
import com.axeliscore.anim.client.GeoRenderer;
import com.axeliscore.plat.Ids;
import com.axeliscore.plat.Nbt;
import com.axeliscore.plat.client.Draw;
import com.coleccionista.Coleccionista;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * (Cliente) Los eliminados se ven como un fantasmita pequeño que flota y mueve los bracitos. Solo los ven los otros fantasmas y el
 * organizador (Minecraft ya no le dibuja los espectadores a los vivos).
 */
public final class FantasmaRender {
    private static final ResourceLocation GEO = Ids.rl(Coleccionista.MODID, "geo/fantasma.geo.json");
    private static final ResourceLocation TEX = Ids.rl(Coleccionista.MODID, "textures/entity/fantasma.png");
    private static Geo geo;
    private static Pose pose;
    private static GeoRenderer.Frame frame;

    private FantasmaRender() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(FantasmaRender::antes);
    }

    private static void antes(RenderPlayerEvent.Pre e) {
        Player p = e.getEntity();
        if (!EventoCliente.esFantasma(p.getUUID())) {
            return;
        }
        e.setCanceled(true);
        if (!veoFantasmas()) {
            return;
        }
        CompoundTag pz = EventoCliente.pieza(p.getUUID());
        String etiqueta = pz == null || p == Minecraft.getInstance().player ? null
                : String.format("%02d", Nbt.i(pz, "numero")) + " · " + Nbt.s(pz, "nombre");
        dibujar(p, e.getPartialTick(), Mth.rotLerp(e.getPartialTick(), p.yHeadRotO, p.yHeadRot), e.getPoseStack(), e.getMultiBufferSource(),
                etiqueta, 1.05F);
    }

    /** ¿El que está jugando en esta computadora puede ver fantasmas? (si es fantasma, espectador u organizador) */
    public static boolean veoFantasmas() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        if (EventoCliente.esFantasma(mc.player.getUUID())) {
            return true;
        }
        // el organizador puede apagarlos para que la toma salga limpia
        return (mc.player.isSpectator() || EventoCliente.soyOrganizador()) && Camara.verFantasmas;
    }

    /** Dibuja el fantasmita en la posición de la entidad (la pose ya está en su lugar), flotando a {@code altura}. */
    public static void dibujar(Entity ent, float pt, float yaw, PoseStack ps, MultiBufferSource buf, String etiqueta, float altura) {
        Geo g = GeoAssets.geo(GEO);
        if (g == null || g.bones.length == 0) {
            return;
        }
        if (g != geo) {
            geo = g;
            pose = new Pose(g.bones.length);
            frame = new GeoRenderer.Frame(g);
        }
        float t = (ent.tickCount + pt) / 20F + (ent.getUUID().hashCode() & 255) * 0.13F;
        pose.reset();
        int cuerpo = g.bone("cuerpo"), falda = g.bone("falda"), bi = g.bone("brazo_i"), bd = g.bone("brazo_d");
        if (cuerpo >= 0) {
            pose.pos[cuerpo * 3 + 1] = Mth.sin(t * 2.2F) * 0.9F;
            pose.rot[cuerpo * 3 + 2] = Mth.sin(t * 1.3F) * 0.06F;
        }
        if (falda >= 0) {
            pose.rot[falda * 3] = Mth.sin(t * 3.1F) * 0.14F;
            pose.rot[falda * 3 + 2] = Mth.cos(t * 2.6F) * 0.12F;
        }
        if (bi >= 0 && bd >= 0) {
            float s = Mth.sin(t * 3F) * 0.35F;
            pose.rot[bi * 3 + 2] = -0.35F - s;
            pose.rot[bd * 3 + 2] = 0.35F + s;
        }
        ps.pushPose();
        ps.translate(0, altura, 0);
        ps.mulPose(Axis.YP.rotationDegrees(180F - yaw));
        ps.scale(-0.9F, -0.9F, 0.9F);
        ps.translate(0, -1.501F, 0);
        GeoRenderer.compute(g, pose, ps, frame);
        Draw d = new Draw(buf);
        GeoRenderer.draw(g, pose, frame, d.translucent(TEX), GeoRenderer.Pass.ALL, 0xF000F0, OverlayTexture.NO_OVERLAY, 0xD8FFFFFF);
        ps.popPose();
        // su número encima, chiquito
        if (etiqueta != null) {
            ps.pushPose();
            ps.translate(0, altura + 0.9, 0);
            ps.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
            ps.scale(-0.02F, -0.02F, 0.02F);
            float w = Minecraft.getInstance().font.width(etiqueta) / 2F;
            d.text(ps, etiqueta, -w, 0, 0xFFBFEFFF, 0x40000000, 0xF000F0);
            ps.popPose();
        }
    }
}
