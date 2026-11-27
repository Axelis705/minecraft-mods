package com.navidad.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.navidad.Navidad;
import com.navidad.bloque.CajaRegalo;
import com.navidad.bloque.CajaRegaloBE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

/**
 * Abrir un regalo: la caja tiembla y da brinquitos cada vez más fuerte, la tapa con el moño sale volando dando vueltas, de adentro salen
 * rayos de luz del color de lo que trae (dorado, azul cielo o morado si es épico) y el regalo sube girando; al final la caja se hace
 * chiquita y se entrega con confeti. Solo dibuja mientras se abre (cerrada, la caja es un modelo normal).
 */
public class CajaRenderer implements BlockEntityRenderer<CajaRegaloBE> {
    public static final ResourceLocation[] CUERPO = new ResourceLocation[4], TAPA = new ResourceLocation[4];
    private static final int[] LUZ = {0xFFD870, 0x70E8FF, 0xD080FF};

    static {
        for (int c = 0; c < 4; c++) {
            CUERPO[c] = new ResourceLocation(Navidad.MODID, "block/caja_regalo_" + c + "_cuerpo");
            TAPA[c] = new ResourceLocation(Navidad.MODID, "block/caja_regalo_" + c + "_tapa");
        }
    }

    public CajaRenderer(BlockEntityRendererProvider.Context c) {
    }

    @Override
    public void render(CajaRegaloBE be, float pt, PoseStack ps, MultiBufferSource buf, int luz, int overlay) {
        if (be.abriendo < 0 || be.getLevel() == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        BlockState st = be.getBlockState();
        if (!st.hasProperty(CajaRegalo.COLOR)) {
            return;
        }
        int color = st.getValue(CajaRegalo.COLOR);
        Direction f = st.getValue(CajaRegalo.FACING);
        float t = be.getLevel().getGameTime() - be.abriendo + pt;
        float tapaT = CajaRegaloBE.TAPA, dura = CajaRegaloBE.DURA;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot() + 180));
        // temblor que crece hasta que salta la tapa
        float fuerza = t < tapaT ? t / tapaT : Math.max(0, 1 - (t - tapaT) / 4F);
        float giro = Mth.sin(t * 2.3F) * 8F * fuerza;
        float brinco = Math.abs(Mth.sin(t * 1.15F)) * 0.06F * fuerza;
        float enco = t > dura - 6 ? Math.max(0.05F, 1 - (t - (dura - 6)) / 6F) : 1F;
        ps.pushPose();
        ps.translate(0, brinco, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(giro));
        ps.mulPose(Axis.XP.rotationDegrees(giro * 0.6F));
        ps.scale(enco, enco * (1 + 0.04F * Mth.sin(t * 4.6F) * fuerza), enco);
        ps.translate(-0.5, 0, -0.5);
        modelo(mc, ps, buf, CUERPO[color], luz);
        // la tapa: tiembla encima y luego sale volando dando vueltas
        if (t < tapaT) {
            float rattle = fuerza * 0.04F * Math.abs(Mth.sin(t * 3.1F));
            ps.translate(0, rattle, 0);
            modelo(mc, ps, buf, TAPA[color], luz);
        } else {
            float k = t - tapaT;
            float s = k > 12 ? Math.max(0, 1 - (k - 12) / 8F) : 1F;
            if (s > 0) {
                ps.pushPose();
                ps.translate(0.5 + k * 0.03, 0.62 + k * 0.16 - k * k * 0.006, 0.5);
                ps.mulPose(Axis.YP.rotationDegrees(k * 28));
                ps.mulPose(Axis.ZP.rotationDegrees(-k * 9));
                ps.scale(s, s, s);
                ps.translate(-0.5, -0.62, -0.5);
                modelo(mc, ps, buf, TAPA[color], luz);
                ps.popPose();
            }
        }
        ps.popPose();
        // la luz y lo que trae
        if (t > tapaT - 1) {
            float k = (t - tapaT) / (dura - tapaT);
            float a = Mth.clamp((t - tapaT + 1) / 4F, 0, 1) * Mth.clamp((dura + 2 - t) / 6F, 0, 1);
            rayos(ps, buf, t, a, LUZ[Mth.clamp(be.rareza, 0, 2)], be.rareza);
            PinoRenderer.resplandor(ps, buf, 0, 0.75F + 0.4F * Math.min(1, k), 0, 0.9F + be.rareza * 0.3F, LUZ[Mth.clamp(be.rareza, 0, 2)],
                    (int) (160 * a));
            if (!be.contenido.isEmpty()) {
                float sube = 1 - (1 - Math.min(1, k * 1.25F)) * (1 - Math.min(1, k * 1.25F));
                ps.pushPose();
                ps.translate(0, 0.45 + 0.8 * sube + Mth.sin(t * 0.3F) * 0.03, 0);
                ps.mulPose(Axis.YP.rotationDegrees(t * 9));
                float s = 0.35F + 0.4F * sube;
                ps.scale(s, s, s);
                mc.getItemRenderer().renderStatic(be.contenido, ItemDisplayContext.GROUND, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ps,
                        buf, be.getLevel(), (int) be.getBlockPos().asLong());
                ps.popPose();
            }
        }
        ps.popPose();
    }

    private static void modelo(Minecraft mc, PoseStack ps, MultiBufferSource buf, ResourceLocation id, int luz) {
        BakedModel m = mc.getModelManager().getModel(id);
        mc.getBlockRenderer().getModelRenderer().renderModel(ps.last(), buf.getBuffer(Sheets.cutoutBlockSheet()), null, m, 1F, 1F, 1F, luz,
                OverlayTexture.NO_OVERLAY);
    }

    /** Rayos de luz que salen de la caja hacia arriba, girando despacio (se suman a lo de atrás). */
    private static void rayos(PoseStack ps, MultiBufferSource buf, float t, float alfa, int rgb, int rareza) {
        if (alfa <= 0.01F) {
            return;
        }
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int n = 7 + rareza * 3;
        ps.pushPose();
        ps.translate(0, 0.55, 0);
        ps.mulPose(Axis.YP.rotationDegrees(t * 3));
        Matrix4f m = ps.last().pose();
        for (int i = 0; i < n; i++) {
            float ang = i * Mth.TWO_PI / n + Mth.sin(t * 0.05F + i) * 0.2F;
            float largo = (1.2F + 0.5F * Mth.sin(t * 0.21F + i * 2.3F)) * (0.8F + rareza * 0.15F);
            float ab = 0.12F;
            float ca = Mth.cos(ang), sa = Mth.sin(ang);
            float tx = ca * largo * 0.55F, ty = largo, tz = sa * largo * 0.55F;
            float px = -sa * ab, pz = ca * ab;
            int a0 = (int) (200 * alfa), a1 = 0;
            for (int lado = 0; lado < 2; lado++) {
                float sgn = lado == 0 ? 1 : -1;
                vc.vertex(m, 0, 0, 0).color(r, g, b, a0).endVertex();
                vc.vertex(m, tx + px * sgn, ty, tz + pz * sgn).color(r, g, b, a1).endVertex();
                vc.vertex(m, tx - px * sgn, ty, tz - pz * sgn).color(r, g, b, a1).endVertex();
                vc.vertex(m, 0, 0, 0).color(r, g, b, a0).endVertex();
            }
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(CajaRegaloBE be) {
        return true;
    }
}
