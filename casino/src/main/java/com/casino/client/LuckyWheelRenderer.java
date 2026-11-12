package com.casino.client;

import com.casino.plat.Ids;
import com.casino.Casino;
import com.casino.block.LuckyWheelBE;
import com.casino.game.WheelLayout;
import com.casino.registry.ModSounds;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Dibuja la Ruleta de la suerte: disco con 20 casillas que gira, marco dorado con luces que corren, puntero que salta con cada casilla
 * y el premio gordo girando arriba con su halo. El giro se calcula con el reloj del juego (igual para todos los jugadores).
 */
public class LuckyWheelRenderer implements BlockEntityRenderer<LuckyWheelBE> {
    private static final ResourceLocation FACE = tex("wheel_face");
    private static final ResourceLocation GOLD = tex("wheel_gold");
    private static final ResourceLocation BACK = tex("wheel_back");
    private static final ResourceLocation GLOW = tex("glow");
    private static final ResourceLocation POINTER = tex("pointer");

    private static final int SEG = 48;
    private static final float R_FACE = 1.37F;
    private static final float R_OUT = 1.50F;
    private static final float THICK = 0.07F;
    private static final int BULBS = 40;
    private static final int FULL = 0xF000F0;

    private final Font font;

    public LuckyWheelRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    private static ResourceLocation tex(String n) {
        return Ids.rl(Casino.MODID, "textures/entity/" + n + ".png");
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz,
                          int r, int g, int b, int a) {
        vc.vertex(p.pose(), x, y, z).color(r, g, b, a).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    private static int hsv(float h, float s, float val) {
        return java.awt.Color.HSBtoRGB(h, s, val) & 0xFFFFFF;
    }

    @Override
    public void render(LuckyWheelBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partial;
        float angle = be.currentAngle(partial);
        Vec3 hub = be.hub();

        // el "tic" del puntero con cada casilla que pasa
        int slice = WheelLayout.sliceAt(angle);
        if (be.clientInit && slice != be.lastSlice) {
            if (be.spinning) {
                be.kickTime = time;
                level.playLocalSound(hub.x, hub.y, hub.z, ModSounds.WHEEL_TICK.get(), SoundSource.BLOCKS, 0.7F, 0.92F + level.random.nextFloat() * 0.16F, false);
            }
            be.lastSlice = slice;
        }

        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above(2));
        int lit = LightTexture.pack(Math.max(LightTexture.block(l0), 13), Math.max(LightTexture.sky(l0), 9));

        ps.pushPose();
        ps.translate(0.5, LuckyWheelBE.HUB_Y, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));

        // ---- marco fijo: fondo, canto y anillo dorado
        PoseStack.Pose pose = ps.last();
        // un "pincel" por tipo de dibujo, pedido justo antes de usarlo (Minecraft comparte el almacén y termina el anterior al pedir otro)
        VertexConsumer back = buf.getBuffer(RenderType.entityCutoutNoCull(BACK));
        for (int i = 0; i < SEG; i++) {
            double a0 = Math.PI * 2 * i / SEG;
            double a1 = Math.PI * 2 * (i + 1) / SEG;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            // fondo (mira hacia atrás)
            v(back, pose, 0, 0, -THICK, 0.5F, 0.5F, lit, 0, 0, -1, 255, 255, 255, 255);
            v(back, pose, R_OUT * c1, R_OUT * s1, -THICK, 0.5F + 0.5F * c1, 0.5F - 0.5F * s1, lit, 0, 0, -1, 255, 255, 255, 255);
            v(back, pose, R_OUT * c0, R_OUT * s0, -THICK, 0.5F + 0.5F * c0, 0.5F - 0.5F * s0, lit, 0, 0, -1, 255, 255, 255, 255);
            v(back, pose, 0, 0, -THICK, 0.5F, 0.5F, lit, 0, 0, -1, 255, 255, 255, 255);
        }
        VertexConsumer gold = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        for (int i = 0; i < SEG; i++) {
            double a0 = Math.PI * 2 * i / SEG;
            double a1 = Math.PI * 2 * (i + 1) / SEG;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            // canto
            v(gold, pose, R_OUT * c0, R_OUT * s0, -THICK, 0, 0, lit, c0, s0, 0, 255, 255, 255, 255);
            v(gold, pose, R_OUT * c1, R_OUT * s1, -THICK, 1, 0, lit, c1, s1, 0, 255, 255, 255, 255);
            v(gold, pose, R_OUT * c1, R_OUT * s1, THICK + 0.01F, 1, 1, lit, c1, s1, 0, 255, 255, 255, 255);
            v(gold, pose, R_OUT * c0, R_OUT * s0, THICK + 0.01F, 0, 1, lit, c0, s0, 0, 255, 255, 255, 255);
            // anillo dorado al frente
            float ri = R_FACE - 0.02F;
            float z = THICK + 0.01F;
            v(gold, pose, ri * c0, ri * s0, z, 0, 0, lit, 0, 0, 1, 255, 255, 255, 255);
            v(gold, pose, ri * c1, ri * s1, z, 1, 0, lit, 0, 0, 1, 255, 255, 255, 255);
            v(gold, pose, R_OUT * c1, R_OUT * s1, z, 1, 1, lit, 0, 0, 1, 255, 255, 255, 255);
            v(gold, pose, R_OUT * c0, R_OUT * s0, z, 0, 1, lit, 0, 0, 1, 255, 255, 255, 255);
        }

        // ---- cara con las 20 casillas (gira)
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotationDegrees(-angle));
        PoseStack.Pose fp = ps.last();
        VertexConsumer face = buf.getBuffer(RenderType.entityCutoutNoCull(FACE));
        float fz = THICK;
        for (int i = 0; i < SEG; i++) {
            double a0 = Math.PI * 2 * i / SEG;
            double a1 = Math.PI * 2 * (i + 1) / SEG;
            float c0 = (float) Math.cos(a0);
            float s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1);
            float s1 = (float) Math.sin(a1);
            v(face, fp, 0, 0, fz, 0.5F, 0.5F, lit, 0, 0, 1, 255, 255, 255, 255);
            v(face, fp, R_FACE * c0, R_FACE * s0, fz, 0.5F + 0.5F * c0, 0.5F - 0.5F * s0, lit, 0, 0, 1, 255, 255, 255, 255);
            v(face, fp, R_FACE * c1, R_FACE * s1, fz, 0.5F + 0.5F * c1, 0.5F - 0.5F * s1, lit, 0, 0, 1, 255, 255, 255, 255);
            v(face, fp, 0, 0, fz, 0.5F, 0.5F, lit, 0, 0, 1, 255, 255, 255, 255);
        }
        ps.popPose();

        // ---- puntero: salta con cada casilla
        float since = time - be.kickTime;
        float flap = since < 4 ? 26.0F * (1 - since / 4.0F) : 0;
        ps.pushPose();
        ps.translate(0, 1.66F, THICK + 0.07F);
        ps.mulPose(Axis.ZP.rotationDegrees(-flap));
        PoseStack.Pose pp = ps.last();
        VertexConsumer pv = buf.getBuffer(RenderType.entityCutoutNoCull(POINTER));
        v(pv, pp, -0.2F, 0.06F, 0, 0, 0, lit, 0, 0, 1, 255, 255, 255, 255);
        v(pv, pp, 0.2F, 0.06F, 0, 1, 0, lit, 0, 0, 1, 255, 255, 255, 255);
        v(pv, pp, 0.2F, -0.46F, 0, 1, 1, lit, 0, 0, 1, 255, 255, 255, 255);
        v(pv, pp, -0.2F, -0.46F, 0, 0, 1, lit, 0, 0, 1, 255, 255, 255, 255);
        ps.popPose();

        // ---- bombillas del marco (brillan solas)
        boolean flash = level.getGameTime() < be.flashEnd;
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(GLOW));
        PoseStack.Pose bp = ps.last();
        for (int i = 0; i < BULBS; i++) {
            double a = Math.PI * 2 * i / BULBS;
            float bx = (float) Math.sin(a) * 1.43F;
            float by = (float) Math.cos(a) * 1.43F;
            int rgb;
            float bright;
            if (be.spinning) {
                boolean on = ((i + (int) (time * 0.7F)) & 1) == 0;
                rgb = hsv((i / (float) BULBS + time * 0.02F) % 1.0F, 0.75F, 1.0F);
                bright = on ? 1.0F : 0.25F;
            } else if (flash) {
                boolean on = (((int) (time / 3)) & 1) == 0;
                rgb = ((i & 1) == (on ? 0 : 1)) ? 0xFFE040 : 0xFFFFFF;
                bright = 1.0F;
            } else {
                float ph = ((i - time * 0.35F) % 8 + 8) % 8;
                rgb = 0xFFC030;
                bright = ph < 1.5F ? 1.0F : 0.28F;
            }
            int r = (int) (((rgb >> 16) & 255) * bright);
            int g = (int) (((rgb >> 8) & 255) * bright);
            int b = (int) ((rgb & 255) * bright);
            float s = 0.11F;
            float z = THICK + 0.06F;
            v(glow, bp, bx - s, by - s, z, 0, 1, FULL, 0, 0, 1, r, g, b, 255);
            v(glow, bp, bx + s, by - s, z, 1, 1, FULL, 0, 0, 1, r, g, b, 255);
            v(glow, bp, bx + s, by + s, z, 1, 0, FULL, 0, 0, 1, r, g, b, 255);
            v(glow, bp, bx - s, by + s, z, 0, 0, FULL, 0, 0, 1, r, g, b, 255);
        }
        ps.popPose();

        renderPrize(be, time, ps, buf);
    }

    /** El premio gordo: halo dorado, objeto girando y su nombre. */
    private void renderPrize(LuckyWheelBE be, float time, PoseStack ps, MultiBufferSource buf) {
        Minecraft mc = Minecraft.getInstance();
        float bob = (float) Math.sin(time * 0.08F) * 0.07F;
        ps.pushPose();
        ps.translate(0.5, LuckyWheelBE.HUB_Y + 2.55 + bob, 0.5);

        // halo que mira a la cámara
        ps.pushPose();
        ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        float pulse = 0.75F + 0.25F * (float) Math.sin(time * 0.15F);
        int c = (int) (200 * pulse);
        PoseStack.Pose hp = ps.last();
        VertexConsumer glow = buf.getBuffer(RenderType.eyes(GLOW));
        float hs = 1.05F;
        v(glow, hp, -hs, -hs, 0, 0, 1, FULL, 0, 0, 1, c, (int) (c * 0.78F), (int) (c * 0.25F), 255);
        v(glow, hp, hs, -hs, 0, 1, 1, FULL, 0, 0, 1, c, (int) (c * 0.78F), (int) (c * 0.25F), 255);
        v(glow, hp, hs, hs, 0, 1, 0, FULL, 0, 0, 1, c, (int) (c * 0.78F), (int) (c * 0.25F), 255);
        v(glow, hp, -hs, hs, 0, 0, 0, FULL, 0, 0, 1, c, (int) (c * 0.78F), (int) (c * 0.25F), 255);
        ps.popPose();

        // objeto
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(time * 2.2F));
        ps.scale(1.25F, 1.25F, 1.25F);
        mc.getItemRenderer().renderStatic(be.podium, ItemDisplayContext.GROUND, FULL, OverlayTexture.NO_OVERLAY, ps, buf, be.getLevel(), 0);
        ps.popPose();

        // nombre (mira a la cámara)
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 pos = be.prizePos();
        if (cam.distanceToSqr(pos) < 24 * 24) {
            ps.pushPose();
            ps.translate(0, 0.95, 0);
            ps.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            ps.scale(-0.022F, -0.022F, 0.022F);
            Matrix4f mat = ps.last().pose();
            Component l1 = Component.translatable("casino.wheel.label").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            Component l2 = Component.literal(be.podium.getCount() + " x ").append(be.podium.getHoverName()).withStyle(ChatFormatting.WHITE);
            font.drawInBatch(l1, -font.width(l1) / 2.0F, -10, 0xFFFFFFFF, false, mat, buf, Font.DisplayMode.NORMAL, 0x40000000, FULL);
            font.drawInBatch(l2, -font.width(l2) / 2.0F, 0, 0xFFFFFFFF, false, mat, buf, Font.DisplayMode.NORMAL, 0x40000000, FULL);
            ps.popPose();
        }
        ps.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LuckyWheelBE be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
