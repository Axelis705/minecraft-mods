package com.mundosdeaxelis.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mundosdeaxelis.block.ArtificerTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * Cristal flotante sobre la Mesa del Artífice. Su color y su tamaño dependen del nivel de mesa del jugador que mira
 * (Básica violeta, Reforzada celeste, Encantada dorada, Maestra rosa) y de la vela salen destellos.
 */
public class ArtificerTableRenderer implements BlockEntityRenderer<ArtificerTableBlockEntity> {
    private static final int[] COLORS = {0xB9A0FF, 0x5BC8FF, 0xFFD34D, 0xFF6BD6, 0xFF8A3C, 0x8CFFB0, 0xFFFFFF, 0xFF4A5E};

    public ArtificerTableRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public int getViewDistance() {
        return 48;
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float vv,
                          float r, float g, float b, float a) {
        vc.vertex(p.pose(), x, y, z).color(r, g, b, a).uv(u, vv)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT)
                .normal(p.normal(), 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static void cell(VertexConsumer vc, PoseStack ps, int[] c, float cx, float cy, float z, float w, float h, int rgb, float a) {
        float u0 = c[0] / (float) CrateUi.ATLAS_W, u1 = (c[0] + 32) / (float) CrateUi.ATLAS_W;
        float v0 = c[1] / (float) CrateUi.ATLAS_H, v1 = (c[1] + 32) / (float) CrateUi.ATLAS_H;
        float r = CrateUi.r(rgb), g = CrateUi.g(rgb), b = CrateUi.b(rgb);
        PoseStack.Pose p = ps.last();
        v(vc, p, cx - w / 2, cy - h / 2, z, u0, v1, r, g, b, a);
        v(vc, p, cx + w / 2, cy - h / 2, z, u1, v1, r, g, b, a);
        v(vc, p, cx + w / 2, cy + h / 2, z, u1, v0, r, g, b, a);
        v(vc, p, cx - w / 2, cy + h / 2, z, u0, v0, r, g, b, a);
    }

    private static void billboard(PoseStack ps) {
        ps.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
        ps.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    @Override
    public void render(ArtificerTableBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        int lv = Mth.clamp(TableClient.level, 1, 8);
        int sz = Math.min(lv, 4);
        int col = COLORS[lv - 1];
        float time = level.getGameTime() + pt;
        float pulse = 0.7F + 0.3F * Mth.sin(time * 0.1F);
        float bob = 0.05F * Mth.sin(time * 0.07F);

        ps.pushPose();
        ps.translate(0.5, 1.4 + bob, 0.5);
        // brillo detras del cristal y destellos
        ps.pushPose();
        billboard(ps);
        VertexConsumer vc = buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.ATLAS));
        float g = 0.42F + 0.08F * sz;
        cell(vc, ps, CrateUi.GLOW, 0, 0, -0.2F, g, g, col, 0.5F * pulse);
        for (int k = 0; k < lv; k++) {
            float ph = time * 0.05F + k * 1.7F;
            float tw = Math.max(0, Mth.sin(ph * 3.0F));
            cell(vc, ps, CrateUi.STAR, Mth.cos(ph) * 0.3F, Mth.sin(ph * 1.2F) * 0.26F, 0.2F, 0.16F * tw, 0.16F * tw, 0xFFFFFF, tw);
        }
        ps.popPose();
        VertexConsumer solid = buf.getBuffer(RenderType.entityCutoutNoCull(CrateUi.WHITE));
        Gem3D.draw(solid, ps, Gem3D.Shape.BRILLIANT, col, 0.20F + 0.025F * sz, time * 2.2F, 1.0F);
        // esquirlas que orbitan (una mas por nivel)
        for (int k = 0; k < lv; k++) {
            float ang = time * 0.07F + k * Mth.TWO_PI / lv;
            ps.pushPose();
            ps.translate(Mth.cos(ang) * 0.27F, 0.05F * Mth.sin(time * 0.1F + k * 2.0F), Mth.sin(ang) * 0.27F);
            Gem3D.draw(solid, ps, Gem3D.Shape.SPIKE, CrateUi.lerp(col, 0xFFFFFF, 0.4F), 0.07F, -time * 5.0F, 1.0F);
            ps.popPose();
        }
        ps.popPose();

        // llama de la vela
        ps.pushPose();
        ps.translate(0.8, 1.14, 0.79);
        billboard(ps);
        float fl = 0.85F + 0.15F * Mth.sin(time * 0.6F);
        cell(buf.getBuffer(RenderType.entityTranslucentEmissive(CrateUi.ATLAS)), ps, CrateUi.GLOW, 0, 0, 0, 0.3F * fl, 0.3F * fl, 0xFFB347, 0.6F);
        ps.popPose();
    }
}
