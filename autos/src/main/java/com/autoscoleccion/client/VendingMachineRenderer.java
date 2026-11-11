package com.autoscoleccion.client;

import com.autoscoleccion.block.VendingMachineBlock;
import com.autoscoleccion.block.VendingMachineBlockEntity;
import com.autoscoleccion.game.Boxes;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.game.Cars;
import com.autoscoleccion.game.Rarity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que se mueve en la máquina expendedora: el premio mayor girando detrás del vidrio, el neón del marco que cambia de color, el
 * letrero luminoso, la ranura de la moneda que parpadea y la bandeja que se ilumina al soltar una caja.
 */
public class VendingMachineRenderer implements BlockEntityRenderer<VendingMachineBlockEntity> {
    private static final List<CarDef> SHOWCASE = new ArrayList<>();

    static {
        for (CarDef c : Cars.all()) {
            if (c.rarity == Rarity.LEGENDARIO || c.rarity == Rarity.EPICO) {
                SHOWCASE.add(c);
            }
        }
    }

    private final Font font;

    public VendingMachineRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    private static int hue(float h) {
        return java.awt.Color.HSBtoRGB(h, 0.75F, 1.0F) & 0xFFFFFF;
    }

    @Override
    public void render(VendingMachineBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partial;
        Direction facing = be.getBlockState().getValue(VendingMachineBlock.FACING);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        int full = CarModel.FULL;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CarModel.WHITE));

        // ---- vitrina de arriba: base giratoria y el premio mayor
        CarDef star = SHOWCASE.get((int) (time / 160.0F) % SHOWCASE.size());
        ps.pushPose();
        ps.translate(0, 1.0 + 1.3 / 16.0, -0.02);
        ps.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
        float k = 1 / 16.0F;
        ps.pushPose();
        ps.scale(k, k, k);
        CarModel.disc(vc, ps, 0.6F, 0, 5.4F, 24, 0x23252B, 255, light);
        CarModel.disc(vc, ps, 0.62F, 5.0F, 5.4F, 24, star.rarity.rgb, 255, full);
        CarModel.band(vc, ps, 0.0F, 0.6F, 5.4F, 24, 0x15161A, 255, light);
        ps.popPose();
        float cs = 0.56F / 16.0F;
        ps.translate(0, 0.62 / 16.0, 0);
        ps.scale(cs, cs, cs);
        CarModel.render(star.style, ps, buf, Math.max(light, 0xB000B0), time, 0);
        ps.popPose();

        // ---- neón del marco de la ventana (cambia de color)
        float zf = 6.55F / 16.0F;
        float x0 = -5.2F / 16, x1 = 5.2F / 16, y0 = 1 + 0.6F / 16, y1 = 1 + 12.85F / 16, t = 0.35F / 16;
        int neon = hue((time * 0.004F) % 1.0F);
        quadZ(vc, ps, x0 - t, y0, x0, y1, zf, neon, full);
        quadZ(vc, ps, x1, y0, x1 + t, y1, zf, neon, full);
        quadZ(vc, ps, x0 - t, y1, x1 + t, y1 + t, zf, neon, full);
        quadZ(vc, ps, x0 - t, y0 - t, x1 + t, y0, zf, neon, full);
        // focos de adentro
        quadZ(vc, ps, x0 + 0.05F, y1 - 0.06F, x1 - 0.05F, y1 - 0.02F, -4.0F / 16, 0xFFF4DC, full);

        // ---- vidrio con reflejo
        VertexConsumer glass = buf.getBuffer(RenderType.entityTranslucent(CarModel.WHITE));
        float zg = 6.3F / 16.0F;
        CarModel.quad(glass, ps, new float[] {x0, y0, zg}, new float[] {x1, y0, zg}, new float[] {x1, y1, zg}, new float[] {x0, y1, zg}, 0xBFE4FF, 38, full);
        float sh = ((time * 0.01F) % 2.0F) - 0.5F;
        float sx = x0 + (x1 - x0) * sh;
        if (sh > -0.3F && sh < 1.3F) {
            CarModel.quad(glass, ps, new float[] {Math.max(x0, sx), y0, zg + 0.002F}, new float[] {Math.max(x0, Math.min(x1, sx + 0.08F)), y0, zg + 0.002F},
                    new float[] {Math.max(x0, Math.min(x1, sx + 0.22F)), y1, zg + 0.002F}, new float[] {Math.max(x0, Math.min(x1, sx + 0.14F)), y1, zg + 0.002F},
                    0xFFFFFF, 60, full);
        }

        // ---- letrero luminoso
        ps.pushPose();
        ps.translate(0, 1 + 14.6F / 16.0F, 7.23F / 16.0F);
        float ts = 0.0072F;
        ps.scale(ts, -ts, ts);
        String title = "AUTOS DE COLECCIÓN";
        int pulse = Gfx.mix(0xFFD24A, 0xFFFFFF, 0.5F + 0.5F * (float) Math.sin(time * 0.15F));
        font.drawInBatch(title, -font.width(title) / 2.0F, -4, 0xFF000000 | pulse, false, ps.last().pose(), buf, Font.DisplayMode.POLYGON_OFFSET, 0,
                full);
        ps.popPose();

        // ---- ranura de la moneda: parpadea invitando; muy brillante al meter una
        float since = time - be.coinTime;
        boolean blink = ((int) (time / 10)) % 2 == 0;
        int slot = since < 20 ? 0xFFE066 : blink ? 0x3CFF7A : 0x0E5A2A;
        quadZ(vc, ps, -3.1F / 16, 10.0F / 16, -1.0F / 16, 10.6F / 16, 6.62F / 16, slot, full);

        // ---- bandeja: se ilumina del color de la caja que salió
        float ds = time - be.dispenseTime;
        if (ds < 60) {
            float a = 1 - ds / 60.0F;
            int col = Gfx.mix(0x101010, Boxes.COLOR[Math.max(0, Math.min(2, be.dispenseBox))], a * (0.6F + 0.4F * (float) Math.sin(ds * 0.8F)));
            quadZ(vc, ps, -4.6F / 16, 2.3F / 16, 4.6F / 16, 5.6F / 16, 6.04F / 16, col, full);
        }
        ps.popPose();
    }

    /** Rectángulo vertical mirando hacia +z (el frente). */
    private static void quadZ(VertexConsumer vc, PoseStack ps, float x0, float y0, float x1, float y1, float z, int rgb, int light) {
        CarModel.quad(vc, ps, new float[] {x0, y0, z}, new float[] {x1, y0, z}, new float[] {x1, y1, z}, new float[] {x0, y1, z}, rgb, 255, light);
    }

    @Override
    public boolean shouldRenderOffScreen(VendingMachineBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
