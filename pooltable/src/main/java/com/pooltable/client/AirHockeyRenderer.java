package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pooltable.PoolTable;
import com.pooltable.block.AirHockeyBE;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Dibuja la mesa de hockey completa (gabinete, cancha con sus líneas, porterías), el disco y los dos mazos.
 * Marco local: x = u (a lo largo, de 0 a 3), z = v (a lo ancho, de -1 a 1), y = 0 en la base del bloque.
 */
public class AirHockeyRenderer implements BlockEntityRenderer<AirHockeyBE> {
    private static final ResourceLocation BODY = tex("block/hockey_body");
    private static final ResourceLocation SURFACE = tex("block/hockey_surface");
    private static final ResourceLocation RED = tex("block/hockey_red");
    private static final ResourceLocation BLUE = tex("block/hockey_blue");
    private static final ResourceLocation BLACK = tex("block/hockey_black");
    private static final ResourceLocation WHITE = tex("block/hockey_white");

    private static final float TOP = 0.875F;
    private static final float SURF = 0.80F;
    private static final float X0 = 0.2F;
    private static final float X1 = 2.8F;
    private static final float ZW = 0.7F;
    private static final float GOAL = (float) HockeyPhysics.GOAL_HALF;
    private static final int SEGS = 18;

    private final double[] sx = {-0.9, 0.9};
    private final double[] sz = {0.0, 0.0};

    public AirHockeyRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    private static ResourceLocation tex(String p) {
        return new ResourceLocation(PoolTable.MODID, "textures/" + p + ".png");
    }

    @Override
    public void render(AirHockeyBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        Direction f = be.facing();
        Vec3 o = be.origin();
        BlockPos bp = be.getBlockPos();
        int light = LevelRenderer.getLightColor(be.getLevel(), bp.above());
        ps.pushPose();
        ps.translate(o.x - bp.getX(), o.y - bp.getY(), o.z - bp.getZ());
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-f.getStepZ(), f.getStepX())));
        drawCabinet(ps, buf, light);
        drawPlay(be, ps, buf, light);
        ps.popPose();
    }

    // ------------------------------------------------------------------ cuadrados

    private static void vtx(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        vc.vertex(pose.pose(), x, y, z).color(255, 255, 255, 255).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(pose.normal(), nx, ny, nz).endVertex();
    }

    private static void top(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float z0, float z1, float y) {
        PoseStack.Pose p = ps.last();
        vtx(vc, p, x0, y, z0, 0, 0, light, 0, 1, 0);
        vtx(vc, p, x0, y, z1, 0, 1, light, 0, 1, 0);
        vtx(vc, p, x1, y, z1, 1, 1, light, 0, 1, 0);
        vtx(vc, p, x1, y, z0, 1, 0, light, 0, 1, 0);
    }

    /** Pared a lo largo de x (a z fijo). */
    private static void wallZ(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float y0, float y1, float z, float nz) {
        PoseStack.Pose p = ps.last();
        vtx(vc, p, x0, y0, z, 0, 0, light, 0, 0, nz);
        vtx(vc, p, x1, y0, z, 1, 0, light, 0, 0, nz);
        vtx(vc, p, x1, y1, z, 1, 1, light, 0, 0, nz);
        vtx(vc, p, x0, y1, z, 0, 1, light, 0, 0, nz);
    }

    /** Pared a lo largo de z (a x fijo). */
    private static void wallX(VertexConsumer vc, PoseStack ps, int light, float z0, float z1, float y0, float y1, float x, float nx) {
        PoseStack.Pose p = ps.last();
        vtx(vc, p, x, y0, z0, 0, 0, light, nx, 0, 0);
        vtx(vc, p, x, y0, z1, 1, 0, light, nx, 0, 0);
        vtx(vc, p, x, y1, z1, 1, 1, light, nx, 0, 0);
        vtx(vc, p, x, y1, z0, 0, 1, light, nx, 0, 0);
    }

    private void drawCabinet(PoseStack ps, MultiBufferSource buf, int light) {
        VertexConsumer b = buf.getBuffer(RenderType.entityCutoutNoCull(BODY));
        // laterales del gabinete
        wallZ(b, ps, light, 0, 3, 0, TOP, 1.0F, 1);
        wallZ(b, ps, light, 0, 3, 0, TOP, -1.0F, -1);
        wallX(b, ps, light, -1, 1, 0, TOP, 0.0F, -1);
        wallX(b, ps, light, -1, 1, 0, TOP, 3.0F, 1);
        // borde superior: franjas a los lados y en los extremos (con el hueco de la portería)
        top(b, ps, light, 0, 3, -1.0F, -ZW, TOP);
        top(b, ps, light, 0, 3, ZW, 1.0F, TOP);
        top(b, ps, light, 0, X0, -ZW, -GOAL, TOP);
        top(b, ps, light, 0, X0, GOAL, ZW, TOP);
        top(b, ps, light, X1, 3, -ZW, -GOAL, TOP);
        top(b, ps, light, X1, 3, GOAL, ZW, TOP);
        // paredes interiores del borde
        wallZ(b, ps, light, X0, X1, SURF, TOP, ZW, -1);
        wallZ(b, ps, light, X0, X1, SURF, TOP, -ZW, 1);
        wallX(b, ps, light, GOAL, ZW, SURF, TOP, X0, 1);
        wallX(b, ps, light, -ZW, -GOAL, SURF, TOP, X0, 1);
        wallX(b, ps, light, GOAL, ZW, SURF, TOP, X1, -1);
        wallX(b, ps, light, -ZW, -GOAL, SURF, TOP, X1, -1);
        // las porterías: un pozo oscuro detrás de cada hueco
        VertexConsumer k = buf.getBuffer(RenderType.entityCutoutNoCull(BLACK));
        top(k, ps, light, 0, X0, -GOAL, GOAL, 0.6F);
        top(k, ps, light, X1, 3, -GOAL, GOAL, 0.6F);
        wallZ(k, ps, light, 0, X0, 0.6F, TOP, GOAL, -1);
        wallZ(k, ps, light, 0, X0, 0.6F, TOP, -GOAL, 1);
        wallZ(k, ps, light, X1, 3, 0.6F, TOP, GOAL, -1);
        wallZ(k, ps, light, X1, 3, 0.6F, TOP, -GOAL, 1);
        wallX(k, ps, light, -GOAL, GOAL, 0.6F, TOP, 0.0F, 1);
        wallX(k, ps, light, -GOAL, GOAL, 0.6F, TOP, 3.0F, -1);
        // la superficie de juego
        VertexConsumer s = buf.getBuffer(RenderType.entityCutoutNoCull(SURFACE));
        top(s, ps, light, X0, X1, -ZW, ZW, SURF);
        // líneas: la del centro y un aro en el centro, en rojo; las de cada portería, en azul
        VertexConsumer r = buf.getBuffer(RenderType.entityCutoutNoCull(RED));
        top(r, ps, light, 1.5F - 0.012F, 1.5F + 0.012F, -ZW, ZW, SURF + 0.002F);
        ringLine(r, ps, light, 1.5F, 0.0F, 0.28F, SURF + 0.002F);
        VertexConsumer bl = buf.getBuffer(RenderType.entityCutoutNoCull(BLUE));
        ringLine(bl, ps, light, X0, 0.0F, 0.34F, SURF + 0.002F);
        ringLine(bl, ps, light, X1, 0.0F, 0.34F, SURF + 0.002F);
    }

    /** Un aro fino (en el suelo de la cancha) hecho de tramos pequeños; solo la parte que cae dentro de la cancha. */
    private static void ringLine(VertexConsumer vc, PoseStack ps, int light, float cx, float cz, float r, float y) {
        float w = 0.012F;
        PoseStack.Pose p = ps.last();
        for (int i = 0; i < 40; i++) {
            double a0 = Math.PI * 2 * i / 40;
            double a1 = Math.PI * 2 * (i + 1) / 40;
            float x0 = cx + (float) Math.cos(a0) * r;
            float z0 = cz + (float) Math.sin(a0) * r;
            float x1 = cx + (float) Math.cos(a1) * r;
            float z1 = cz + (float) Math.sin(a1) * r;
            if (x0 < X0 || x0 > X1 || x1 < X0 || x1 > X1) {
                continue;
            }
            float dx = (float) Math.cos((a0 + a1) / 2) * w;
            float dz = (float) Math.sin((a0 + a1) / 2) * w;
            vtx(vc, p, x0 - dx, y, z0 - dz, 0, 0, light, 0, 1, 0);
            vtx(vc, p, x0 + dx, y, z0 + dz, 0, 1, light, 0, 1, 0);
            vtx(vc, p, x1 + dx, y, z1 + dz, 1, 1, light, 0, 1, 0);
            vtx(vc, p, x1 - dx, y, z1 - dz, 1, 0, light, 0, 1, 0);
        }
    }

    /** Un cilindro con tapa de arriba (centro cx, cz; de y0 a y1). */
    private static void cylinder(VertexConsumer vc, PoseStack ps, int light, float cx, float cz, float r, float y0, float y1) {
        PoseStack.Pose p = ps.last();
        for (int i = 0; i < SEGS; i++) {
            double a0 = Math.PI * 2 * i / SEGS;
            double a1 = Math.PI * 2 * (i + 1) / SEGS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            float nx = (float) Math.cos((a0 + a1) / 2);
            float nz = (float) Math.sin((a0 + a1) / 2);
            vtx(vc, p, cx + c0 * r, y0, cz + s0 * r, 0, 0, light, nx, 0, nz);
            vtx(vc, p, cx + c1 * r, y0, cz + s1 * r, 1, 0, light, nx, 0, nz);
            vtx(vc, p, cx + c1 * r, y1, cz + s1 * r, 1, 1, light, nx, 0, nz);
            vtx(vc, p, cx + c0 * r, y1, cz + s0 * r, 0, 1, light, nx, 0, nz);
            // tapa: un triángulo (el centro se repite)
            vtx(vc, p, cx, y1, cz, 0.5F, 0.5F, light, 0, 1, 0);
            vtx(vc, p, cx + c1 * r, y1, cz + s1 * r, 1, 0, light, 0, 1, 0);
            vtx(vc, p, cx + c0 * r, y1, cz + s0 * r, 0, 0, light, 0, 1, 0);
            vtx(vc, p, cx, y1, cz, 0.5F, 0.5F, light, 0, 1, 0);
        }
    }

    private void drawPlay(AirHockeyBE be, PoseStack ps, MultiBufferSource buf, int light) {
        boolean live = be.match.state != HockeyMatch.State.IDLE;
        // los mazos siempre se ven (en reposo, cada uno en su sitio); se suavizan hacia lo que manda el servidor
        for (int s = 0; s < 2; s++) {
            double tx = live && be.clientInit ? be.cmx[s] : (s == 0 ? -0.9 : 0.9);
            double tz = live && be.clientInit ? be.cmz[s] : 0.0;
            sx[s] += (tx - sx[s]) * 0.5;
            sz[s] += (tz - sz[s]) * 0.5;
            float ux = 1.5F + (float) sx[s];
            float uz = (float) sz[s];
            ResourceLocation col = s == 0 ? RED : BLUE;
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(col)), ps, light, ux, uz, (float) HockeyPhysics.MALLET_R, SURF, SURF + 0.025F);
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(WHITE)), ps, light, ux, uz, 0.035F, SURF + 0.025F, SURF + 0.075F);
        }
        if (live && be.clientInit && be.match.state != HockeyMatch.State.WAITING) {
            double[] pk = be.puckNow();
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(BLACK)), ps, light, 1.5F + (float) pk[0], (float) pk[1], (float) HockeyPhysics.PUCK_R, SURF, SURF + 0.02F);
        } else if (!live) {
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(BLACK)), ps, light, 1.5F, 0.0F, (float) HockeyPhysics.PUCK_R, SURF, SURF + 0.02F);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(AirHockeyBE be) {
        return true;
    }
}
