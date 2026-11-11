package com.autoscoleccion.client;

import com.autoscoleccion.block.DisplayCaseBlock;
import com.autoscoleccion.block.DisplayCaseBlockEntity;
import com.autoscoleccion.game.CarDef;
import com.autoscoleccion.item.CarItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;

/** La vitrina: base giratoria con aro de luz del color de la rareza, el auto, un haz de luz desde el foco y la placa con el nombre. */
public class DisplayCaseRenderer implements BlockEntityRenderer<DisplayCaseBlockEntity> {
    private final Font font;

    public DisplayCaseRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    @Override
    public void render(DisplayCaseBlockEntity be, float partial, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partial;
        Direction facing = be.getBlockState().getValue(DisplayCaseBlock.FACING);
        int lit = LightTexture.pack(Math.max(LightTexture.block(light), 13), LightTexture.sky(light));
        int full = CarModel.FULL;
        boolean has = be.getCar().getItem() instanceof CarItem;
        CarDef car = has ? ((CarItem) be.getCar().getItem()).def() : null;
        int ring = has ? car.rarity.rgb : 0x3A3D44;
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CarModel.WHITE));
        float angle = be.isSpinning() ? (time * 1.5F) % 360.0F : be.frozenAngle;
        // base giratoria
        ps.pushPose();
        ps.scale(1 / 16.0F, 1 / 16.0F, 1 / 16.0F);
        ps.mulPose(Axis.YP.rotationDegrees(angle));
        CarModel.disc(vc, ps, 3.6F, 0, 6.0F, 32, 0x1E2026, 255, lit);
        CarModel.disc(vc, ps, 3.62F, 5.5F, 6.0F, 32, ring, 255, full);
        CarModel.band(vc, ps, 3.0F, 3.6F, 6.0F, 32, 0x121317, 255, lit);
        ps.popPose();
        if (has) {
            float since = time - be.placedTime;
            float pop = since < 12 ? Gfx.back(since / 12.0F) : 1.0F;
            ps.pushPose();
            ps.translate(0, 3.62 / 16.0 + (1 - Math.min(1, since / 12.0F)) * 0.25, 0);
            ps.mulPose(Axis.YP.rotationDegrees(angle));
            float k = 0.72F / 16.0F * pop;
            ps.scale(k, k, k);
            CarModel.render(car.style, ps, buf, lit, time, 0);
            ps.popPose();
            // haz de luz desde el foco
            VertexConsumer glow = buf.getBuffer(RenderType.entityTranslucentEmissive(CarModel.WHITE));
            int seg = 12;
            for (int i = 0; i < seg; i++) {
                double a0 = i * 2 * Math.PI / seg, a1 = (i + 1) * 2 * Math.PI / seg;
                float r0 = 0.9F / 16, r1 = 5.8F / 16, yt = 14.8F / 16, yb = 3.7F / 16;
                CarModel.quad(glow, ps, new float[] {(float) Math.cos(a0) * r0, yt, (float) Math.sin(a0) * r0},
                        new float[] {(float) Math.cos(a1) * r0, yt, (float) Math.sin(a1) * r0},
                        new float[] {(float) Math.cos(a1) * r1, yb, (float) Math.sin(a1) * r1},
                        new float[] {(float) Math.cos(a0) * r1, yb, (float) Math.sin(a0) * r1}, Gfx.mix(0xFFF6DF, ring, 0.35F), 20, full);
            }
            // placa con el nombre al frente
            ps.pushPose();
            ps.translate(0, 1.55F / 16.0F, 7.06F / 16.0F);
            String name = car.name;
            float s = Math.min(0.0085F, (12.0F / 16.0F) / Math.max(1, font.width(name)));
            ps.scale(s, -s, s);
            font.drawInBatch(name, -font.width(name) / 2.0F, -4, 0xFF000000 | car.rarity.rgb, false, ps.last().pose(), buf,
                    Font.DisplayMode.POLYGON_OFFSET, 0, full);
            ps.popPose();
        }
        // foco del techo
        CarModel.disc(vc, ps, 14.9F / 16, 0, 0.9F / 16, 12, has ? 0xFFF6DF : 0x55585E, 255, has ? full : lit);
        ps.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
