package com.pooltable.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pooltable.PoolTable;
import com.pooltable.block.VolleyCourtBE;
import com.pooltable.game.VolleyMatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Dibuja la cancha de voleibol alrededor del bloque: el piso (mitad azul y mitad naranja, con zona de afuera y líneas blancas), la red con sus postes,
 * la pelota con su sombra y un círculo donde va a caer, un aro de color bajo cada jugador y los bots (modelos de persona). El tamaño depende de cuántos juegan.
 * Marco local: x = u (a lo largo), z = v (a lo ancho), y sobre el suelo, con el origen en el centro del bloque.
 */
public class VolleyRenderer implements BlockEntityRenderer<VolleyCourtBE> {
    private static final ResourceLocation COURT_A = tex("block/volley_court_a");
    private static final ResourceLocation COURT_B = tex("block/volley_court_b");
    private static final ResourceLocation OUT = tex("block/volley_out");
    private static final ResourceLocation LINE = tex("block/volley_line");
    private static final ResourceLocation NET = tex("block/volley_net");
    private static final ResourceLocation POST = tex("block/volley_post");
    private static final ResourceLocation BALL = tex("block/volley_ball");
    private static final ResourceLocation WHITE = tex("block/volley_white");
    private static final ResourceLocation SKIN_A = new ResourceLocation(PoolTable.MODID, "textures/entity/volley_a.png");
    private static final ResourceLocation SKIN_B = new ResourceLocation(PoolTable.MODID, "textures/entity/volley_b.png");
    private static final int[] TEAM_RGB = {0x2D8CFF, 0xFF8A1E};

    private static final float MAT = 0.035F;
    private static final float LINE_Y = 0.06F;
    private static final float LINE_W = 0.12F;
    private static final float OUT_W = 2.2F;
    private static final int SEGS = 20;

    private final PlayerModel<Player> model;
    private final double[] sx = new double[6];
    private final double[] sz = new double[6];
    private final double[] sy = new double[6];
    private final double[] heading = new double[6];
    private final double[] walk = new double[6];
    private boolean smoothInit = false;

    public VolleyRenderer(BlockEntityRendererProvider.Context ctx) {
        this.model = new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false);
    }

    private static ResourceLocation tex(String p) {
        return new ResourceLocation(PoolTable.MODID, "textures/" + p + ".png");
    }

    @Override
    public void render(VolleyCourtBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        if (be.getLevel() == null) {
            return;
        }
        Direction f = be.facing();
        BlockPos bp = be.getBlockPos();
        int raw = LevelRenderer.getLightColor(be.getLevel(), bp.above());
        int light = LightTexture.pack(Math.max(11, LightTexture.block(raw)), LightTexture.sky(raw));
        int n = Math.max(1, Math.min(3, be.courtN()));
        double hl = VolleyMatch.HL[n - 1];
        double hw = VolleyMatch.HW[n - 1];
        boolean live = be.clientInit && be.cState != VolleyMatch.State.IDLE.ordinal();
        ps.pushPose();
        ps.translate(0.5, 0.0, 0.5);
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-f.getStepZ(), f.getStepX())));
        drawCourt(ps, buf, light, (float) hl, (float) hw, (float) VolleyMatch.ATK[n - 1]);
        drawNet(ps, buf, light, (float) hw);
        if (live) {
            drawActors(be, ps, buf, light, hl, hw);
        }
        ps.popPose();
    }

    // ------------------------------------------------------------------ helpers

    private static void quad(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float z0, float z1, float y, float u0, float u1, float v0, float v1,
                             int r, int g, int b, int a) {
        PoseStack.Pose p = ps.last();
        vc.vertex(p.pose(), x0, y, z0).color(r, g, b, a).uv(u0, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
        vc.vertex(p.pose(), x0, y, z1).color(r, g, b, a).uv(u0, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
        vc.vertex(p.pose(), x1, y, z1).color(r, g, b, a).uv(u1, v1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
        vc.vertex(p.pose(), x1, y, z0).color(r, g, b, a).uv(u1, v0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
    }

    private static void flat(VertexConsumer vc, PoseStack ps, int light, float x0, float x1, float z0, float z1, float y) {
        quad(vc, ps, light, x0, x1, z0, z1, y, 0, 1, 0, 1, 255, 255, 255, 255);
    }

    /** Un cuadrado vertical (en un plano z fijo o x fijo). */
    private static void wallX(VertexConsumer vc, PoseStack ps, int light, float x, float z0, float z1, float y0, float y1, float nx) {
        PoseStack.Pose p = ps.last();
        vc.vertex(p.pose(), x, y0, z0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, 0).endVertex();
        vc.vertex(p.pose(), x, y0, z1).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, 0).endVertex();
        vc.vertex(p.pose(), x, y1, z1).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, 0).endVertex();
        vc.vertex(p.pose(), x, y1, z0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, 0).endVertex();
    }

    private static void wallZ(VertexConsumer vc, PoseStack ps, int light, float z, float x0, float x1, float y0, float y1, float nz) {
        PoseStack.Pose p = ps.last();
        vc.vertex(p.pose(), x0, y0, z).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 0, nz).endVertex();
        vc.vertex(p.pose(), x1, y0, z).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 0, nz).endVertex();
        vc.vertex(p.pose(), x1, y1, z).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 0, nz).endVertex();
        vc.vertex(p.pose(), x0, y1, z).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 0, nz).endVertex();
    }

    /** Un anillo plano (de radio interior a exterior) hecho de tramos. */
    private static void ring(VertexConsumer vc, PoseStack ps, int light, float cx, float cz, float r0, float r1, float y, int rgb, int alpha) {
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        PoseStack.Pose p = ps.last();
        for (int i = 0; i < SEGS; i++) {
            double a0 = Math.PI * 2 * i / SEGS;
            double a1 = Math.PI * 2 * (i + 1) / SEGS;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            vc.vertex(p.pose(), cx + c0 * r0, y, cz + s0 * r0).color(r, g, b, alpha).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), cx + c0 * r1, y, cz + s0 * r1).color(r, g, b, alpha).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), cx + c1 * r1, y, cz + s1 * r1).color(r, g, b, alpha).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), cx + c1 * r0, y, cz + s1 * r0).color(r, g, b, alpha).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
        }
    }

    private static void post(VertexConsumer vc, PoseStack ps, int light, float cz, float r, float y1) {
        PoseStack.Pose p = ps.last();
        int segs = 10;
        for (int i = 0; i < segs; i++) {
            double a0 = Math.PI * 2 * i / segs;
            double a1 = Math.PI * 2 * (i + 1) / segs;
            float c0 = (float) Math.cos(a0) * r;
            float s0 = (float) Math.sin(a0) * r;
            float c1 = (float) Math.cos(a1) * r;
            float s1 = (float) Math.sin(a1) * r;
            float nx = (float) Math.cos((a0 + a1) / 2);
            float nz = (float) Math.sin((a0 + a1) / 2);
            vc.vertex(p.pose(), c0, 0, cz + s0).color(255, 255, 255, 255).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, nz).endVertex();
            vc.vertex(p.pose(), c1, 0, cz + s1).color(255, 255, 255, 255).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, nz).endVertex();
            vc.vertex(p.pose(), c1, y1, cz + s1).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, nz).endVertex();
            vc.vertex(p.pose(), c0, y1, cz + s0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, 0, nz).endVertex();
            // tapa
            vc.vertex(p.pose(), 0, y1, cz).color(255, 255, 255, 255).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), c1, y1, cz + s1).color(255, 255, 255, 255).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), c0, y1, cz + s0).color(255, 255, 255, 255).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
            vc.vertex(p.pose(), 0, y1, cz).color(255, 255, 255, 255).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), 0, 1, 0).endVertex();
        }
    }

    // ------------------------------------------------------------------ cancha y red

    private void drawCourt(PoseStack ps, MultiBufferSource buf, int light, float hl, float hw, float atk) {
        // zona de afuera (arena), con su borde para que no se vea hueco bajo el piso
        VertexConsumer out = buf.getBuffer(RenderType.entityCutoutNoCull(OUT));
        float ox = hl + OUT_W;
        float oz = hw + OUT_W;
        flat(out, ps, light, -ox, ox, -oz, oz, MAT - 0.01F);
        wallZ(out, ps, light, -oz, -ox, ox, 0, MAT - 0.01F, -1);
        wallZ(out, ps, light, oz, -ox, ox, 0, MAT - 0.01F, 1);
        wallX(out, ps, light, -ox, -oz, oz, 0, MAT - 0.01F, -1);
        wallX(out, ps, light, ox, -oz, oz, 0, MAT - 0.01F, 1);
        // las dos mitades
        flat(buf.getBuffer(RenderType.entityCutoutNoCull(COURT_A)), ps, light, -hl, 0, -hw, hw, MAT);
        flat(buf.getBuffer(RenderType.entityCutoutNoCull(COURT_B)), ps, light, 0, hl, -hw, hw, MAT);
        // líneas blancas
        VertexConsumer ln = buf.getBuffer(RenderType.entityCutoutNoCull(LINE));
        float w = LINE_W;
        int fb = LightTexture.FULL_BRIGHT;
        // borde (las líneas son parte de la cancha: la pelota que cae sobre ellas es buena)
        flat(ln, ps, fb, -hl - w, hl + w, -hw - w, -hw + w, LINE_Y);
        flat(ln, ps, fb, -hl - w, hl + w, hw - w, hw + w, LINE_Y);
        flat(ln, ps, fb, -hl - w, -hl + w, -hw - w, hw + w, LINE_Y);
        flat(ln, ps, fb, hl - w, hl + w, -hw - w, hw + w, LINE_Y);
        // línea central (bajo la red) y líneas de ataque a 3 m de la red
        flat(ln, ps, fb, -w, w, -hw, hw, LINE_Y);
        flat(ln, ps, fb, -atk - w / 2, -atk + w / 2, -hw, hw, LINE_Y);
        flat(ln, ps, fb, atk - w / 2, atk + w / 2, -hw, hw, LINE_Y);
    }

    private void drawNet(PoseStack ps, MultiBufferSource buf, int light, float hw) {
        float zEdge = hw + 0.35F;
        float top = (float) VolleyMatch.NET_TOP;
        VertexConsumer pc = buf.getBuffer(RenderType.entityCutoutNoCull(POST));
        post(pc, ps, light, -zEdge, 0.07F, top + 0.25F);
        post(pc, ps, light, zEdge, 0.07F, top + 0.25F);
        // malla: cuadros de 0,5 con la textura de red
        VertexConsumer net = buf.getBuffer(RenderType.entityCutoutNoCull(NET));
        float y0 = 0.45F;
        float cell = 0.5F;
        for (float z = -zEdge; z < zEdge - 0.01F; z += cell) {
            float z1 = Math.min(zEdge, z + cell);
            for (float y = y0; y < top - 0.01F; y += cell) {
                float yy = Math.min(top, y + cell);
                wallX(net, ps, light, 0, z, z1, y, yy, 1);
            }
        }
        // cinta blanca arriba y cuerda abajo
        VertexConsumer tape = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        wallX(tape, ps, light, 0.002F, -zEdge, zEdge, top - 0.12F, top, 1);
        wallX(tape, ps, light, -0.002F, -zEdge, zEdge, top - 0.12F, top, -1);
        wallX(tape, ps, light, 0.002F, -zEdge, zEdge, y0, y0 + 0.04F, 1);
        wallX(tape, ps, light, -0.002F, -zEdge, zEdge, y0, y0 + 0.04F, -1);
    }

    // ------------------------------------------------------------------ pelota y jugadores

    private void drawActors(VolleyCourtBE be, PoseStack ps, MultiBufferSource buf, int light, double hl, double hw) {
        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        double time = now / 1.0e9;
        // suavizar a los jugadores
        for (int i = 0; i < be.cCount; i++) {
            if (!smoothInit) {
                sx[i] = be.cX[i];
                sz[i] = be.cZ[i];
                sy[i] = be.cY[i];
            } else {
                double dx = be.cX[i] - sx[i];
                double dz = be.cZ[i] - sz[i];
                double sp = Math.hypot(dx, dz);
                if (sp > 0.02) {
                    heading[i] = Math.atan2(dz, dx);
                }
                walk[i] += Math.min(1.0, sp) * 1.6;
                sx[i] += dx * 0.45;
                sz[i] += dz * 0.45;
                sy[i] += (be.cY[i] - sy[i]) * 0.6;
            }
        }
        smoothInit = true;
        float[] b = be.ballNow();
        boolean flying = be.cState == VolleyMatch.State.RALLY.ordinal();

        // bots (modelos de persona)
        for (int i = 0; i < be.cCount; i++) {
            if ((be.cFlags[i] & 1) != 0) {
                continue;
            }
            drawBot(be, ps, buf, light, i, b);
        }
        // la pelota
        ps.pushPose();
        ps.translate(b[0], b[1], b[2]);
        float rr = (float) VolleyMatch.R;
        if (flying) {
            ps.mulPose(Axis.YP.rotation((float) (time * 5.0)));
            ps.mulPose(Axis.XP.rotation((float) (time * 4.0)));
        }
        ps.scale(rr, rr, rr);
        BallMesh.draw(ps, buf.getBuffer(RenderType.entityCutoutNoCull(BALL)), LightTexture.FULL_BRIGHT);
        ps.popPose();

        // sombra de la pelota y círculo de dónde va a caer (translúcidos, al final)
        VertexConsumer tr = buf.getBuffer(RenderType.entityTranslucent(WHITE));
        float sh = (float) (rr * (1.1 + b[1] * 0.12));
        int sa = (int) Math.max(40, 150 - b[1] * 18);
        ring(tr, ps, light, b[0], b[2], 0.0F, sh, LINE_Y + 0.004F, 0x101010, sa);
        if (flying && be.cBall[4] * be.cBall[4] + 2 * VolleyMatch.G * (be.cBall[1] - VolleyMatch.R) >= 0) {
            double vy = be.cBall[4];
            double tl = (vy + Math.sqrt(vy * vy + 2 * VolleyMatch.G * (be.cBall[1] - VolleyMatch.R))) / VolleyMatch.G;
            double lx = be.cBall[0] + be.cBall[3] * tl;
            double lz = be.cBall[2] + be.cBall[5] * tl;
            double dt = (now - be.recvNanos) / 1.0e9;
            double left = Math.max(0.0, tl - dt);
            float pulse = (float) (0.55 + 0.1 * Math.sin(time * 9.0));
            int rgb = lx < 0 ? TEAM_RGB[0] : TEAM_RGB[1];
            if (left > 0.05 && Math.abs(lx) < hl + 3 && Math.abs(lz) < hw + 3) {
                ring(tr, ps, light, (float) lx, (float) lz, pulse - 0.1F, pulse, LINE_Y + 0.006F, rgb, 210);
                ring(tr, ps, light, (float) lx, (float) lz, 0.0F, (float) Math.max(0.05, 0.5 - left * 0.25), LINE_Y + 0.005F, rgb, 90);
            }
        }
        // la marca de puntería del jugador de este equipo: verde si cae dentro de las líneas, roja si apunta fuera
        if (VolleyClient.court == be && VolleyClient.playing()) {
            float au = (float) VolleyClient.aimU();
            float av = (float) VolleyClient.aimV();
            int rgb = VolleyClient.aimInside() ? 0x3CFF6A : 0xFF4040;
            float pulse = (float) (0.5 + 0.05 * Math.sin(time * 7.0));
            ring(tr, ps, light, au, av, pulse - 0.1F, pulse, LINE_Y + 0.012F, rgb, 235);
            ring(tr, ps, light, au, av, 0.0F, 0.13F, LINE_Y + 0.012F, rgb, 235);
            ring(tr, ps, light, au, av, pulse + 0.05F, pulse + 0.09F, LINE_Y + 0.011F, 0xFFFFFF, 170);
        }
        // aros de equipo bajo cada jugador
        Player me = mc.player;
        for (int i = 0; i < be.cCount; i++) {
            int rgb = TEAM_RGB[be.cTeam[i] == 0 ? 0 : 1];
            boolean mine = me != null && be.cEid[i] == me.getId();
            float r1 = mine ? 0.62F : 0.52F;
            ring(tr, ps, light, (float) sx[i], (float) sz[i], r1 - 0.09F, r1, LINE_Y + 0.003F, rgb, 235);
            if (mine) {
                ring(tr, ps, light, (float) sx[i], (float) sz[i], r1 + 0.04F, r1 + 0.1F, LINE_Y + 0.003F, 0xFFFFFF, 235);
            }
        }
    }

    private void drawBot(VolleyCourtBE be, PoseStack ps, MultiBufferSource buf, int light, int i, float[] ball) {
        int team = be.cTeam[i] == 0 ? 0 : 1;
        double hx = ball[0] - sx[i];
        double hz = ball[2] - sz[i];
        double face = Math.atan2(hz, hx);
        boolean moving = Math.hypot(be.cX[i] - sx[i], be.cZ[i] - sz[i]) > 0.03;
        if (moving) {
            face = heading[i] * 0.5 + face * 0.5;
        }
        // en el marco local el modelo mira a +z al girar 0: yaw = atan2(-du, dv)
        float yawDeg = (float) Math.toDegrees(Math.atan2(-Math.cos(face), Math.sin(face)));
        int act = be.cAct[i];
        float swing = be.cSwing[i] / 35.0F;
        boolean hitting = be.cSwing[i] > 0;
        ps.pushPose();
        ps.translate(sx[i], sy[i], sz[i]);
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - yawDeg));
        ps.scale(0.9375F, 0.9375F, 0.9375F);
        ps.scale(-1.0F, -1.0F, 1.0F);
        ps.translate(0.0, -1.501, 0.0);
        float legs = moving ? (float) Math.sin(walk[i]) * 0.9F : 0.0F;
        boolean air = sy[i] > 0.08;
        model.head.xRot = 0;
        model.head.yRot = 0;
        model.body.xRot = 0;
        model.rightLeg.xRot = air ? -0.4F : legs;
        model.leftLeg.xRot = air ? 0.3F : -legs;
        float ra = moving ? -legs * 0.6F : 0.0F;
        float la = moving ? legs * 0.6F : 0.0F;
        float rz = 0.05F;
        float lz = -0.05F;
        if (hitting) {
            // act: 1 saque, 2 recepción, 3 colocación, 4 remate, 5 bloqueo, 6 pelota libre
            switch (act) {
                case 2, 6 -> {
                    ra = -1.05F;
                    la = -1.05F;
                    rz = -0.2F;
                    lz = 0.2F;
                }
                case 3 -> {
                    ra = -2.5F;
                    la = -2.5F;
                    rz = -0.25F;
                    lz = 0.25F;
                }
                case 4 -> {
                    ra = -3.0F + (1.0F - swing) * 2.0F;
                    la = -2.6F;
                }
                case 5 -> {
                    ra = -3.1F;
                    la = -3.1F;
                    rz = -0.1F;
                    lz = 0.1F;
                }
                case 1 -> {
                    ra = -2.9F + (1.0F - swing) * 2.2F;
                    la = -2.0F;
                }
                default -> {
                }
            }
        }
        model.rightArm.xRot = ra;
        model.leftArm.xRot = la;
        model.rightArm.zRot = rz;
        model.leftArm.zRot = lz;
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
        model.renderToBuffer(ps, buf.getBuffer(RenderType.entityCutoutNoCull(team == 0 ? SKIN_A : SKIN_B)), light, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(VolleyCourtBE be) {
        return true;
    }
}
