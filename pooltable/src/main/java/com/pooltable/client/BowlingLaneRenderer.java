package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pooltable.PoolTable;
import com.pooltable.block.BowlingLaneBE;
import com.pooltable.block.BowlingLaneBlock;
import com.pooltable.game.BowlingPhysics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Dibuja toda la pista (suelo, canaletas, foso del fondo), los pinos y la bola. Marco local: x = u (hacia los pinos), z = v (a la derecha),
 * y = 0 en la superficie de la pista.
 */
public class BowlingLaneRenderer implements BlockEntityRenderer<BowlingLaneBE> {
    private static final ResourceLocation WOOD = new ResourceLocation(PoolTable.MODID, "textures/block/bowling_lane.png");
    private static final ResourceLocation DARK = new ResourceLocation(PoolTable.MODID, "textures/block/bowling_dark.png");
    private static final ResourceLocation PIN = new ResourceLocation(PoolTable.MODID, "textures/entity/bowling_pin.png");
    private static final ResourceLocation BALL = new ResourceLocation(PoolTable.MODID, "textures/entity/bowling_ball.png");

    private static final float GUTTER_Y = -0.0625F;
    private static final float BOT_Y = -0.12F;
    private static final float DECK_END = 12.4F;
    private static final float WALL_TOP = 0.8125F;
    private static final float HALF = (float) BowlingPhysics.HALF_W;
    private static final float PIN_PIVOT = 0.07F;

    public BowlingLaneRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(BowlingLaneBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        Direction f = be.facing();
        Vec3 o = be.origin();
        BlockPos bp = be.getBlockPos();
        ps.pushPose();
        ps.translate(o.x - bp.getX(), o.y - bp.getY(), o.z - bp.getZ());
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-f.getStepZ(), f.getStepX())));

        drawStatic(be, ps, buf);
        drawPins(be, ps, buf);
        drawBall(be, ps, buf);
        ps.popPose();
    }

    private static int lightAt(BowlingLaneBE be, double u, double v) {
        int cu = Mth.clamp((int) Math.floor(u), 0, BowlingLaneBlock.NU - 1);
        BlockPos p = BowlingLaneBlock.cell(be.getBlockPos(), be.facing(), cu, v < 0 ? 0 : 1).above();
        return LevelRenderer.getLightColor(be.getLevel(), p);
    }

    // ------------------------------------------------------------------ cuadrados

    private static void vtx(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }

    /** Suelo horizontal de (x0,z0) a (x1,z1) a altura y; la textura se repite cada {@code tile} bloques. */
    private static void floor(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float z0, float z1, float y, float tile) {
        PoseStack.Pose p = ps.last();
        float u1 = (x1 - x0) / tile;
        float v1 = (z1 - z0) / tile;
        vtx(vc, p, x0, y, z0, 0, 0, light, 0, 1, 0);
        vtx(vc, p, x0, y, z1, 0, v1, light, 0, 1, 0);
        vtx(vc, p, x1, y, z1, u1, v1, light, 0, 1, 0);
        vtx(vc, p, x1, y, z0, u1, 0, light, 0, 1, 0);
    }

    /** Pared que corre a lo largo de x (a z fijo), de x0 a x1 y de y0 a y1. */
    private static void wallZ(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float y0, float y1, float z, float nz) {
        PoseStack.Pose p = ps.last();
        float u1 = (x1 - x0) / 0.5F;
        float v1 = (y1 - y0) / 0.5F;
        vtx(vc, p, x0, y0, z, 0, 0, light, 0, 0, nz);
        vtx(vc, p, x1, y0, z, u1, 0, light, 0, 0, nz);
        vtx(vc, p, x1, y1, z, u1, v1, light, 0, 0, nz);
        vtx(vc, p, x0, y1, z, 0, v1, light, 0, 0, nz);
    }

    /** Pared que corre a lo largo de z (a x fijo). */
    private static void wallX(VertexConsumer vc, PoseStack ps, int light, float z0, float z1, float y0, float y1, float x, float nx) {
        PoseStack.Pose p = ps.last();
        float u1 = (z1 - z0) / 0.5F;
        float v1 = (y1 - y0) / 0.5F;
        vtx(vc, p, x, y0, z0, 0, 0, light, nx, 0, 0);
        vtx(vc, p, x, y0, z1, u1, 0, light, nx, 0, 0);
        vtx(vc, p, x, y1, z1, u1, v1, light, nx, 0, 0);
        vtx(vc, p, x, y1, z0, 0, v1, light, nx, 0, 0);
    }

    private void drawStatic(BowlingLaneBE be, PoseStack ps, MultiBufferSource buf) {
        // madera: la pista jugable, y la zona de salida a lo ancho
        VertexConsumer wood = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        for (int i = 0; i < 13; i++) {
            float x0 = i;
            float x1 = Math.min(DECK_END, i + 1);
            for (int s = 0; s < 2; s++) {
                float z0 = s == 0 ? -HALF : 0.0F;
                float z1 = s == 0 ? 0.0F : HALF;
                floor(wood, ps, lightAt(be, i, z0 + 0.1), x0, x1, z0, z1, 0.0F, HALF);
            }
        }
        float fu = (float) BowlingPhysics.FOUL_U;
        for (int i = 0; i < 3; i++) {
            float x0 = i;
            float x1 = Math.min(fu, i + 1);
            floor(wood, ps, lightAt(be, i, 0.9), x0, x1, HALF, 1.0F, 0.0F, HALF);
            floor(wood, ps, lightAt(be, i, -0.9), x0, x1, -1.0F, -HALF, 0.0F, HALF);
        }

        // piezas oscuras: línea de falta, canaletas, laterales, foso
        VertexConsumer dk = buf.getBuffer(RenderType.entityCutoutNoCull(DARK));
        int lf = lightAt(be, fu, 0.1);
        floor(dk, ps, lf, fu - 0.025F, fu + 0.025F, -HALF, HALF, 0.003F, 1.0F);
        for (int i = 0; i < 10; i++) {
            float x0 = fu + i;
            float x1 = Math.min(DECK_END, fu + i + 1);
            if (x0 >= DECK_END) {
                break;
            }
            int lg = lightAt(be, x0, 0.9);
            floor(dk, ps, lg, x0, x1, HALF, 1.0F, GUTTER_Y, 1.0F);
            floor(dk, ps, lg, x0, x1, -1.0F, -HALF, GUTTER_Y, 1.0F);
        }
        float len = DECK_END - fu;
        int lg = lightAt(be, 6.0, 0.9);
        wallZ(dk, ps, lg, fu, DECK_END, GUTTER_Y, 0.0F, HALF, -1);
        wallZ(dk, ps, lg, fu, DECK_END, GUTTER_Y, 0.0F, -HALF, 1);
        wallX(dk, ps, lg, HALF, 1.0F, GUTTER_Y, 0.0F, fu, -1);
        wallX(dk, ps, lg, -1.0F, -HALF, GUTTER_Y, 0.0F, fu, -1);
        // laterales y cabecera del bloque (el borde de madera visto desde fuera)
        int lm = lightAt(be, 7.0, 0.5);
        wallZ(dk, ps, lm, 0.0F, 12.4F, BOT_Y, 0.0F, 1.0F, 1);
        wallZ(dk, ps, lm, 0.0F, 12.4F, BOT_Y, 0.0F, -1.0F, -1);
        wallZ(dk, ps, lm, fu, DECK_END, GUTTER_Y, 0.0F, 1.0F, -1);
        wallZ(dk, ps, lm, fu, DECK_END, GUTTER_Y, 0.0F, -1.0F, 1);
        wallX(dk, ps, lm, -1.0F, 1.0F, BOT_Y, 0.0F, 0.0F, -1);
        // foso del fondo
        int lp = lightAt(be, 13.0, 0.5);
        floor(dk, ps, lp, DECK_END, 14.0F, -1.0F, 1.0F, BOT_Y, 1.0F);
        wallX(dk, ps, lp, -1.0F, 1.0F, BOT_Y, 0.0F, DECK_END, -1);
        wallX(dk, ps, lp, -1.0F, 1.0F, BOT_Y, WALL_TOP, 13.99F, -1);
        wallZ(dk, ps, lp, DECK_END, 14.0F, BOT_Y, WALL_TOP, 1.0F, 1);
        wallZ(dk, ps, lp, DECK_END, 14.0F, BOT_Y, WALL_TOP, -1.0F, -1);
        floor(dk, ps, lp, DECK_END, 14.0F, -1.0F, 1.0F, WALL_TOP, 1.0F);
        wallX(dk, ps, lp, -1.0F, 1.0F, 0.55F, WALL_TOP, DECK_END, -1);
    }

    private void drawPins(BowlingLaneBE be, PoseStack ps, MultiBufferSource buf) {
        if (!be.clientInit) {
            return;
        }
        float a = be.clientMoving ? Mth.clamp((System.nanoTime() - be.recvNanos) / 50.0e6F, 0.0F, 1.0F) : 1.0F;
        long now = System.nanoTime();
        for (int i = 0; i < BowlingPhysics.PINS; i++) {
            byte st = be.cstate[i];
            if (st == BowlingPhysics.GONE) {
                continue;
            }
            float u = Mth.lerp(a, be.ppu[i], be.cpu[i]);
            float v = Mth.lerp(a, be.ppv[i], be.cpv[i]);
            int light = lightAt(be, u, v);
            ps.pushPose();
            ps.translate(u, 0.0F, v);
            if (st == BowlingPhysics.DOWN) {
                float t = (now - be.fallStart[i]) / 1.0e9F;
                float k = Mth.clamp(t / 0.3F, 0.0F, 1.0F);
                float fall = k * k;
                ps.mulPose(Axis.YP.rotation(-be.heading[i]));
                ps.translate(-(PIN_PIVOT + PinMesh.HEIGHT * 0.5F) * fall, 0.0F, 0.0F);
                ps.translate(PIN_PIVOT, 0.0F, 0.0F);
                ps.mulPose(Axis.ZP.rotation(-fall * Mth.HALF_PI));
                ps.translate(-PIN_PIVOT, 0.0F, 0.0F);
            }
            PinMesh.draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(PIN)), light);
            ps.popPose();
        }
    }

    private void drawBall(BowlingLaneBE be, PoseStack ps, MultiBufferSource buf) {
        if (!be.clientInit || !be.cBallActive) {
            return;
        }
        float a = be.clientMoving ? Mth.clamp((System.nanoTime() - be.recvNanos) / 50.0e6F, 0.0F, 1.0F) : 1.0F;
        float u = Mth.lerp(a, be.pbu, be.cbu);
        float v = Mth.lerp(a, be.pbv, be.cbv);
        float r = (float) BowlingPhysics.BALL_R;
        ps.pushPose();
        ps.translate(u, r, v);
        ps.mulPose(Axis.ZP.rotation(-u / r));
        ps.mulPose(Axis.XP.rotation(v / r));
        ps.scale(r, r, r);
        BallMesh.draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BALL)), lightAt(be, u, v));
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(BowlingLaneBE be) {
        return true;
    }
}
