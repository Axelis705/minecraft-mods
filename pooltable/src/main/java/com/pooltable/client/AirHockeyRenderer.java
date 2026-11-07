package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pooltable.PoolTable;
import com.pooltable.block.AirHockeyBE;
import com.pooltable.game.HockeyMatch;
import com.pooltable.game.HockeyPhysics;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
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
    private static final ResourceLocation RAIL = tex("block/hockey_rail");
    private static final ResourceLocation LIME = tex("block/hockey_lime");
    private static final ResourceLocation PUCK = tex("block/hockey_puck");
    private static final ResourceLocation GLASS_LIME = tex("block/hockey_glass_lime");
    private static final ResourceLocation GLASS_MAGENTA = tex("block/hockey_glass_magenta");
    private static final int FULL = LightTexture.FULL_BRIGHT;

    private static final float TOP = 0.875F;
    private static final float SURF = 0.80F;
    private static final float X0 = 0.2F;
    private static final float X1 = 2.8F;
    private static final float ZW = 0.7F;
    private static final float GOAL = (float) HockeyPhysics.GOAL_HALF;
    private static final float LINE = 0.006F;
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
        if (!HockeyCam.active && be.match.state != HockeyMatch.State.IDLE) {
            drawScore(be, ps, buf, light, bp);
        }
        ps.pushPose();
        ps.translate(o.x - bp.getX(), o.y - bp.getY(), o.z - bp.getZ());
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-f.getStepZ(), f.getStepX())));
        drawCabinet(ps, buf, light);
        drawPlay(be, ps, buf, light);
        ps.popPose();
    }

    /** El marcador flotando sobre la mesa, para quien mira desde fuera. */
    private void drawScore(AirHockeyBE be, PoseStack ps, MultiBufferSource buf, int light, BlockPos bp) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 c = be.center();
        String a = be.pname[0].isEmpty() ? "-" : be.pname[0];
        String b = be.pname[1].isEmpty() ? "-" : be.pname[1];
        Component line = Component.literal(a).withStyle(ChatFormatting.RED)
                .append(Component.literal("  " + be.match.scoreA + " : " + be.match.scoreB + "  ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(b).withStyle(ChatFormatting.AQUA));
        ps.pushPose();
        ps.translate(c.x - bp.getX(), c.y - bp.getY() + 0.95, c.z - bp.getZ());
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        ps.scale(-0.03F, -0.03F, 0.03F);
        float w = mc.font.width(line);
        mc.font.drawInBatch(line, -w / 2.0F, 0, 0xFFFFFFFF, true, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0x80000000, 0xF000F0);
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
        // el fondo cerrado (si no, desde abajo se ve el interior)
        {
            PoseStack.Pose p = ps.last();
            vtx(b, p, 0, 0.002F, -1, 0, 0, light, 0, -1, 0);
            vtx(b, p, 3, 0.002F, -1, 1, 0, light, 0, -1, 0);
            vtx(b, p, 3, 0.002F, 1, 1, 1, light, 0, -1, 0);
            vtx(b, p, 0, 0.002F, 1, 0, 1, light, 0, -1, 0);
        }
        // borde superior: franjas a los lados y en los extremos (con el hueco de la portería), cian y brillante
        VertexConsumer rail = buf.getBuffer(RenderType.entityCutoutNoCull(RAIL));
        top(rail, ps, FULL, 0, 3, -1.0F, -ZW, TOP);
        top(rail, ps, FULL, 0, 3, ZW, 1.0F, TOP);
        top(rail, ps, FULL, 0, X0, -ZW, -GOAL, TOP);
        top(rail, ps, FULL, 0, X0, GOAL, ZW, TOP);
        top(rail, ps, FULL, X1, 3, -ZW, -GOAL, TOP);
        top(rail, ps, FULL, X1, 3, GOAL, ZW, TOP);
        // paredes interiores del borde
        wallZ(rail, ps, FULL, X0, X1, SURF, TOP, ZW, -1);
        wallZ(rail, ps, FULL, X0, X1, SURF, TOP, -ZW, 1);
        wallX(rail, ps, FULL, GOAL, ZW, SURF, TOP, X0, 1);
        wallX(rail, ps, FULL, -ZW, -GOAL, SURF, TOP, X0, 1);
        wallX(rail, ps, FULL, GOAL, ZW, SURF, TOP, X1, -1);
        wallX(rail, ps, FULL, -ZW, -GOAL, SURF, TOP, X1, -1);
        // las porterías: un pozo oscuro detrás de cada hueco
        VertexConsumer k = buf.getBuffer(RenderType.entityCutoutNoCull(BLACK));
        top(k, ps, light, 0, X0, -GOAL, GOAL, 0.6F);
        top(k, ps, light, X1, 3, -GOAL, GOAL, 0.6F);
        wallZ(k, ps, light, 0, X0, 0.6F, TOP, GOAL, -1);
        wallZ(k, ps, light, 0, X0, 0.6F, TOP, -GOAL, 1);
        wallZ(k, ps, light, X1, 3, 0.6F, TOP, GOAL, -1);
        wallZ(k, ps, light, X1, 3, 0.6F, TOP, -GOAL, 1);
        // (un poquito hacia adentro: si no, quedan encimadas con la pared del gabinete y se ven dos texturas peleándose)
        wallX(k, ps, light, -GOAL, GOAL, 0.6F, TOP, 0.004F, 1);
        wallX(k, ps, light, -GOAL, GOAL, 0.6F, TOP, 2.996F, -1);
        // la superficie de juego
        VertexConsumer s = buf.getBuffer(RenderType.entityCutoutNoCull(SURFACE));
        top(s, ps, light, X0, X1, -ZW, ZW, SURF);
        // líneas cian: la del centro, el aro central y el arco de cada portería
        VertexConsumer bl = buf.getBuffer(RenderType.entityCutoutNoCull(BLUE));
        top(bl, ps, light, 1.5F - 0.012F, 1.5F + 0.012F, -ZW, ZW, SURF + LINE);
        ringLine(bl, ps, light, 1.5F, 0.0F, 0.28F, SURF + LINE);
        ringLine(bl, ps, light, X0, 0.0F, 0.34F, SURF + LINE);
        ringLine(bl, ps, light, X1, 0.0F, 0.34F, SURF + LINE);
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
            vtx(vc, p, cx + c1 * r, y1, cz + s1 * r, 0.5F + c1 * 0.5F, 0.5F + s1 * 0.5F, light, 0, 1, 0);
            vtx(vc, p, cx + c0 * r, y1, cz + s0 * r, 0.5F + c0 * 0.5F, 0.5F + s0 * 0.5F, light, 0, 1, 0);
            vtx(vc, p, cx, y1, cz, 0.5F, 0.5F, light, 0, 1, 0);
        }
    }

    private void drawPlay(AirHockeyBE be, PoseStack ps, MultiBufferSource buf, int light) {
        boolean live = be.match.state != HockeyMatch.State.IDLE;
        java.util.List<float[]> glass = new java.util.ArrayList<>(2);
        // los mazos siempre se ven (en reposo, cada uno en su sitio); se suavizan hacia lo que manda el servidor
        for (int s = 0; s < 2; s++) {
            double tx = live && be.clientInit ? be.cmx[s] : (s == 0 ? -0.9 : 0.9);
            double tz = live && be.clientInit ? be.cmz[s] : 0.0;
            if (HockeyCam.active && s == AirHockeyBE.localRole && be.getBlockPos().equals(AirHockeyBE.localPos)) {
                tx = AirHockeyBE.localX;
                tz = AirHockeyBE.localZ;
                sx[s] = tx;
                sz[s] = tz;
            } else {
                sx[s] += (tx - sx[s]) * 0.5;
                sz[s] += (tz - sz[s]) * 0.5;
            }
            float ux = 1.5F + (float) sx[s];
            float uz = (float) sz[s];
            float mr = (float) HockeyPhysics.MALLET_R;
            // base de color + tallo + esfera de vidrio translúcido (se dibuja al final por ser translúcida)
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(s == 0 ? LIME : RED)), ps, light, ux, uz, mr, SURF, SURF + 0.022F);
            cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(WHITE)), ps, light, ux, uz, 0.03F, SURF + 0.022F, SURF + 0.05F);
            glass.add(new float[] {ux, uz, s});
        }
        if (live && be.clientInit && be.match.state != HockeyMatch.State.WAITING) {
            double[] pk = be.puckNow();
            puck(buf, ps, 1.5F + (float) pk[0], (float) pk[1]);
        } else if (!live) {
            puck(buf, ps, 1.5F, 0.0F);
        }
        for (float[] g : glass) {
            VertexConsumer gv = buf.getBuffer(RenderType.entityTranslucent(g[2] == 0 ? GLASS_LIME : GLASS_MAGENTA));
            float rad = 0.075F;
            ps.pushPose();
            ps.translate(g[0], SURF + 0.03F + rad, g[1]);
            ps.scale(rad, rad, rad);
            BallMesh.draw(ps, gv, FULL);
            ps.popPose();
        }
    }

    /** El disco: lima con aro oscuro, y un brillo propio para que se vea bien desde la cámara. */
    private static void puck(MultiBufferSource buf, PoseStack ps, float x, float z) {
        cylinder(buf.getBuffer(RenderType.entityCutoutNoCull(PUCK)), ps, FULL, x, z, (float) HockeyPhysics.PUCK_R, SURF, SURF + 0.02F);
    }

    @Override
    public boolean shouldRenderOffScreen(AirHockeyBE be) {
        return true;
    }
}
