package com.casino.client;

import com.casino.Casino;
import com.casino.block.PokerBE;
import com.casino.chip.Chips;
import com.casino.game.BlackjackLayout;
import com.casino.game.PokerHands;
import com.casino.game.PokerTable;
import com.casino.game.PokerTable.Phase;
import com.casino.game.PokerTable.Result;
import com.casino.game.PokerTable.Seat;
import com.casino.game.PokerTable.SeatState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Dibuja toda la mesa de póker (media luna de fieltro con rieles, cuerpo de madera y zócalo dorado), y encima las cartas de cada asiento
 * y del crupier (con animación de reparto y de giro de la carta tapada), las fichas apostadas, los círculos de cada asiento, el zapato,
 * los totales y los resultados. Marco local: origen en el centro del bloque del medio del lado recto, +z hacia los jugadores, +x a su derecha.
 */
public class PokerRenderer implements BlockEntityRenderer<PokerBE> {
    private static final ResourceLocation CARDS = new ResourceLocation(Casino.MODID, "textures/entity/cards.png");
    private static final ResourceLocation WHITE = new ResourceLocation(Casino.MODID, "textures/entity/plinko_white.png");
    private static final ResourceLocation FELT = new ResourceLocation(Casino.MODID, "textures/block/poker_felt.png");
    private static final ResourceLocation WOOD = new ResourceLocation(Casino.MODID, "textures/block/bj_wood.png");
    private static final ResourceLocation LEATHER = new ResourceLocation(Casino.MODID, "textures/block/bj_leather.png");
    private static final ResourceLocation GOLD = new ResourceLocation(Casino.MODID, "textures/block/gold.png");

    private static final float CARD_W = 0.17F;
    private static final float CARD_H = 0.255F;
    private static final float SPACING = 0.15F;
    private static final double CZ = BlackjackLayout.CZ;
    private static final float R_OUT = (float) BlackjackLayout.RADIUS;
    private static final float RAIL = 0.125F;
    private static final float R_IN = R_OUT - RAIL;
    private static final float Y_FELT = 13.0F / 16.0F;
    private static final float Y_RAIL = 14.0F / 16.0F;
    private static final float Y_LIP = 11.0F / 16.0F;
    private static final float TABLE_Y = Y_FELT + 0.006F;
    private static final float SHOE_X = 0.85F;
    private static final float SHOE_Z = -0.27F;
    private static final double CARD_R = 1.62;
    private static final double ANTE_R = 0.92;
    private static final double CALL_R = 1.20;
    private static final double TOTAL_R = 1.98;
    private static final double NAME_R = 2.24;
    private static final float BOARD_Z = 0.25F;
    private static final int SEG = 48;
    private static final int FULL = 15728880;

    private final Font font;

    public PokerRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    // ------------------------------------------------------------------ vértices

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz, int rgb) {
        vc.vertex(p.pose(), x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    private static float ax(double r, double deg) {
        return (float) (r * Math.cos(Math.toRadians(deg)));
    }

    private static float az(double r, double deg) {
        return (float) (CZ + r * Math.sin(Math.toRadians(deg)));
    }

    /** Pared curva (de 0 a 180 grados) a radio r entre dos alturas, con el frente hacia afuera (o hacia adentro). */
    private static void wallArc(VertexConsumer vc, PoseStack.Pose p, int light, float r, float y0, float y1, boolean inward, int rgb, float vScale) {
        for (int i = 0; i < SEG; i++) {
            double a0 = 180.0 * i / SEG;
            double a1 = 180.0 * (i + 1) / SEG;
            double am = Math.toRadians((a0 + a1) / 2);
            float nx = (float) Math.cos(am) * (inward ? -1 : 1);
            float nz = (float) Math.sin(am) * (inward ? -1 : 1);
            float u0 = (float) (r * Math.toRadians(a0)) * 2;
            float u1 = (float) (r * Math.toRadians(a1)) * 2;
            v(vc, p, ax(r, a0), y0, az(r, a0), u0, (1 - y0) * 2 * vScale, light, nx, 0, nz, rgb);
            v(vc, p, ax(r, a1), y0, az(r, a1), u1, (1 - y0) * 2 * vScale, light, nx, 0, nz, rgb);
            v(vc, p, ax(r, a1), y1, az(r, a1), u1, (1 - y1) * 2 * vScale, light, nx, 0, nz, rgb);
            v(vc, p, ax(r, a0), y1, az(r, a0), u0, (1 - y1) * 2 * vScale, light, nx, 0, nz, rgb);
        }
    }

    /** Pared recta del lado del crupier (z = centro), de x = -r a x = r. */
    private static void wallFlat(VertexConsumer vc, PoseStack.Pose p, int light, float r, float y0, float y1, float z, boolean towardPlayers, int rgb, float vScale) {
        float nz = towardPlayers ? 1 : -1;
        v(vc, p, -r, y0, z, -r * 2, (1 - y0) * 2 * vScale, light, 0, 0, nz, rgb);
        v(vc, p, r, y0, z, r * 2, (1 - y0) * 2 * vScale, light, 0, 0, nz, rgb);
        v(vc, p, r, y1, z, r * 2, (1 - y1) * 2 * vScale, light, 0, 0, nz, rgb);
        v(vc, p, -r, y1, z, -r * 2, (1 - y1) * 2 * vScale, light, 0, 0, nz, rgb);
    }

    /** Anillo horizontal entre dos radios (cara hacia arriba). El ángulo puede recortarse (a0..a1). */
    private static void ringTop(VertexConsumer vc, PoseStack.Pose p, int light, double rIn, double rOut, float y, double a0, double a1, int n, int rgb, boolean clampFlat) {
        for (int i = 0; i < n; i++) {
            double s0 = a0 + (a1 - a0) * i / n;
            double s1 = a0 + (a1 - a0) * (i + 1) / n;
            float zi0 = az(rIn, s0);
            float zi1 = az(rIn, s1);
            if (clampFlat) {
                zi0 = (float) Math.max(zi0, CZ + RAIL);
                zi1 = (float) Math.max(zi1, CZ + RAIL);
            }
            v(vc, p, ax(rIn, s0), y, zi0, ax(rIn, s0) * 2, zi0 * 2, light, 0, 1, 0, rgb);
            v(vc, p, ax(rOut, s0), y, az(rOut, s0), ax(rOut, s0) * 2, az(rOut, s0) * 2, light, 0, 1, 0, rgb);
            v(vc, p, ax(rOut, s1), y, az(rOut, s1), ax(rOut, s1) * 2, az(rOut, s1) * 2, light, 0, 1, 0, rgb);
            v(vc, p, ax(rIn, s1), y, zi1, ax(rIn, s1) * 2, zi1 * 2, light, 0, 1, 0, rgb);
        }
    }

    /** Un segmento plano finito sobre el fieltro (para líneas del dibujo de la mesa). */
    private static void line(VertexConsumer vc, PoseStack.Pose p, int light, float x0, float z0, float x1, float z1, float w, float y, int rgb) {
        float dx = x1 - x0;
        float dz = z1 - z0;
        float len = (float) Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-5F) {
            return;
        }
        float nx = -dz / len * w / 2;
        float nz = dx / len * w / 2;
        v(vc, p, x0 - nx, y, z0 - nz, 0, 0, light, 0, 1, 0, rgb);
        v(vc, p, x0 + nx, y, z0 + nz, 0, 1, light, 0, 1, 0, rgb);
        v(vc, p, x1 + nx, y, z1 + nz, 1, 1, light, 0, 1, 0, rgb);
        v(vc, p, x1 - nx, y, z1 - nz, 1, 0, light, 0, 1, 0, rgb);
    }

    private static void arcLine(VertexConsumer vc, PoseStack.Pose p, int light, double r, double a0, double a1, float w, float y, int rgb) {
        int n = 40;
        for (int i = 0; i < n; i++) {
            double s0 = a0 + (a1 - a0) * i / n;
            double s1 = a0 + (a1 - a0) * (i + 1) / n;
            line(vc, p, light, ax(r, s0), az(r, s0), ax(r, s1), az(r, s1), w, y, rgb);
        }
    }

    private static void ring(VertexConsumer vc, PoseStack.Pose p, int light, float cx, float cz, float r0, float r1, float y, int rgb) {
        int n = 20;
        for (int i = 0; i < n; i++) {
            double s0 = Math.PI * 2 * i / n;
            double s1 = Math.PI * 2 * (i + 1) / n;
            v(vc, p, cx + (float) Math.cos(s0) * r0, y, cz + (float) Math.sin(s0) * r0, 0, 0, light, 0, 1, 0, rgb);
            v(vc, p, cx + (float) Math.cos(s0) * r1, y, cz + (float) Math.sin(s0) * r1, 0, 1, light, 0, 1, 0, rgb);
            v(vc, p, cx + (float) Math.cos(s1) * r1, y, cz + (float) Math.sin(s1) * r1, 1, 1, light, 0, 1, 0, rgb);
            v(vc, p, cx + (float) Math.cos(s1) * r0, y, cz + (float) Math.sin(s1) * r0, 1, 0, light, 0, 1, 0, rgb);
        }
    }

    private static void box(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int light) {
        v(vc, p, x0, y1, z0, 0, 0, light, 0, 1, 0, rgb); v(vc, p, x0, y1, z1, 0, 1, light, 0, 1, 0, rgb);
        v(vc, p, x1, y1, z1, 1, 1, light, 0, 1, 0, rgb); v(vc, p, x1, y1, z0, 1, 0, light, 0, 1, 0, rgb);
        v(vc, p, x0, y0, z1, 0, 0, light, 0, 0, 1, scale(rgb, 0.8F)); v(vc, p, x1, y0, z1, 1, 0, light, 0, 0, 1, scale(rgb, 0.8F));
        v(vc, p, x1, y1, z1, 1, 1, light, 0, 0, 1, scale(rgb, 0.8F)); v(vc, p, x0, y1, z1, 0, 1, light, 0, 0, 1, scale(rgb, 0.8F));
        v(vc, p, x1, y0, z0, 0, 0, light, 0, 0, -1, scale(rgb, 0.8F)); v(vc, p, x0, y0, z0, 1, 0, light, 0, 0, -1, scale(rgb, 0.8F));
        v(vc, p, x0, y1, z0, 1, 1, light, 0, 0, -1, scale(rgb, 0.8F)); v(vc, p, x1, y1, z0, 0, 1, light, 0, 0, -1, scale(rgb, 0.8F));
        v(vc, p, x0, y0, z0, 0, 0, light, -1, 0, 0, scale(rgb, 0.65F)); v(vc, p, x0, y0, z1, 1, 0, light, -1, 0, 0, scale(rgb, 0.65F));
        v(vc, p, x0, y1, z1, 1, 1, light, -1, 0, 0, scale(rgb, 0.65F)); v(vc, p, x0, y1, z0, 0, 1, light, -1, 0, 0, scale(rgb, 0.65F));
        v(vc, p, x1, y0, z1, 0, 0, light, 1, 0, 0, scale(rgb, 0.65F)); v(vc, p, x1, y0, z0, 1, 0, light, 1, 0, 0, scale(rgb, 0.65F));
        v(vc, p, x1, y1, z0, 1, 1, light, 1, 0, 0, scale(rgb, 0.65F)); v(vc, p, x1, y1, z1, 0, 1, light, 1, 0, 0, scale(rgb, 0.65F));
    }

    private static void quadUV(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int light) {
        v(vc, p, x0, y0, 0, u0, v1, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x1, y0, 0, u1, v1, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x1, y1, 0, u1, v0, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x0, y1, 0, u0, v0, light, 0, 0, 1, 0xFFFFFF);
    }

    private static void cardQuad(VertexConsumer vc, PoseStack.Pose p, float halfW, int card, int light) {
        float u0;
        float v0;
        if (card < 0) {
            u0 = 0;
            v0 = 4 / 5.0F;
        } else {
            u0 = (card % 13) / 13.0F;
            v0 = PokerHands.suit(card) / 5.0F;
        }
        quadUV(vc, p, -halfW, -CARD_H / 2, halfW, CARD_H / 2, u0, v0, u0 + 1 / 13.0F, v0 + 1 / 5.0F, light);
    }

    // ------------------------------------------------------------------ la mesa

    private void drawTable(PoseStack ps, MultiBufferSource buf, int light) {
        PoseStack.Pose p = ps.last();
        float flat = (float) CZ;
        // zócalo dorado y cuerpo de madera
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        wallArc(vc, p, light, R_OUT - 0.02F, 0, 0.09F, false, 0xFFFFFF, 1);
        wallFlat(vc, p, light, R_OUT - 0.02F, 0, 0.09F, flat, false, 0xFFFFFF, 1);
        ringTop(vc, p, light, R_OUT - 0.08F, R_OUT - 0.02F, 0.09F, 0, 180, SEG, 0xFFFFFF, false);
        wallArc(vc, p, light, R_OUT - 0.07F, 0.50F, 0.57F, false, 0xFFFFFF, 1);
        wallFlat(vc, p, light, R_OUT - 0.07F, 0.50F, 0.57F, flat - 0.001F, false, 0xFFFFFF, 1);
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(WOOD));
        wallArc(vc, p, light, R_OUT - 0.08F, 0.09F, Y_LIP, false, 0xFFFFFF, 1);
        wallFlat(vc, p, light, R_OUT - 0.08F, 0.09F, Y_LIP, flat, false, 0xFFFFFF, 1);
        wallArc(vc, p, light, R_OUT, Y_LIP, Y_FELT, false, 0xFFFFFF, 1);
        wallFlat(vc, p, light, R_OUT, Y_LIP, Y_FELT, flat, false, 0xFFFFFF, 1);
        ringTop(vc, p, light, R_OUT - 0.08F, R_OUT, Y_LIP, 0, 180, SEG, 0xBBBBBB, false);
        // riel acolchado
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(LEATHER));
        wallArc(vc, p, light, R_OUT, Y_FELT, Y_RAIL, false, 0xFFFFFF, 1);
        wallFlat(vc, p, light, R_OUT, Y_FELT, Y_RAIL, flat, false, 0xFFFFFF, 1);
        wallArc(vc, p, light, R_IN, Y_FELT, Y_RAIL, true, 0xC8C8C8, 1);
        wallFlat(vc, p, light, R_IN, Y_FELT, Y_RAIL, flat + RAIL, true, 0xC8C8C8, 1);
        ringTop(vc, p, light, R_IN, R_OUT, Y_RAIL, 0, 180, SEG, 0xFFFFFF, true);
        // tira recta del riel del lado del crupier
        float zi = flat + RAIL;
        v(vc, p, -R_OUT, Y_RAIL, flat, -R_OUT * 2, flat * 2, light, 0, 1, 0, 0xFFFFFF);
        v(vc, p, -R_IN, Y_RAIL, zi, -R_IN * 2, zi * 2, light, 0, 1, 0, 0xFFFFFF);
        v(vc, p, R_IN, Y_RAIL, zi, R_IN * 2, zi * 2, light, 0, 1, 0, 0xFFFFFF);
        v(vc, p, R_OUT, Y_RAIL, flat, R_OUT * 2, flat * 2, light, 0, 1, 0, 0xFFFFFF);
        // fieltro: abanico desde el borde recto
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(FELT));
        float fz = flat + RAIL;
        double th0 = Math.toDegrees(Math.asin(RAIL / R_IN));
        int n = SEG;
        for (int i = 0; i < n; i++) {
            double s0 = th0 + (180 - 2 * th0) * i / n;
            double s1 = th0 + (180 - 2 * th0) * (i + 1) / n;
            float x0 = ax(R_IN, s0), z0 = az(R_IN, s0), x1 = ax(R_IN, s1), z1 = az(R_IN, s1);
            v(vc, p, 0, Y_FELT, fz, 0, fz * 2, light, 0, 1, 0, 0xFFFFFF);
            v(vc, p, x0, Y_FELT, z0, x0 * 2, z0 * 2, light, 0, 1, 0, 0xFFFFFF);
            v(vc, p, x1, Y_FELT, z1, x1 * 2, z1 * 2, light, 0, 1, 0, 0xFFFFFF);
            v(vc, p, 0, Y_FELT, fz, 0, fz * 2, light, 0, 1, 0, 0xFFFFFF);
        }
        // filo dorado sobre el riel
        vc = buf.getBuffer(RenderType.entityCutoutNoCull(GOLD));
        ringTop(vc, p, light, R_OUT - 0.085F, R_OUT - 0.05F, Y_RAIL + 0.002F, 0, 180, SEG, 0xFFFFFF, false);
    }

    /** El dibujo del fieltro: líneas doradas, carriles de cartas, círculos de ANTE e IGUALAR de cada asiento y el espacio de las cartas comunes. */
    private void drawDecor(PokerBE be, PoseStack ps, MultiBufferSource buf, int light, float time) {
        PoseStack.Pose p = ps.last();
        float y = Y_FELT + 0.003F;
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        int gold = 0xD9AE38;
        arcLine(vc, p, light, 1.38, 16, 164, 0.014F, y, gold);
        arcLine(vc, p, light, 1.88, 16, 164, 0.014F, y, gold);
        for (int i = 0; i <= BlackjackLayout.SEATS; i++) {
            double a = 155.0 - 32.5 * i + 16.25;
            line(vc, p, light, ax(1.38, a), az(1.38, a), ax(1.88, a), az(1.88, a), 0.012F, y, gold);
        }
        // las cinco cartas comunes: marcos dorados
        for (int j = 0; j < 5; j++) {
            float cx = (j - 2) * 0.2F;
            float hw = CARD_W / 2 + 0.012F;
            float hh = CARD_H / 2 + 0.012F;
            int c = be.table.board.size() > j ? 0x8A6E20 : gold;
            line(vc, p, light, cx - hw, BOARD_Z - hh, cx + hw, BOARD_Z - hh, 0.01F, y, c);
            line(vc, p, light, cx - hw, BOARD_Z + hh, cx + hw, BOARD_Z + hh, 0.01F, y, c);
            line(vc, p, light, cx - hw, BOARD_Z - hh, cx - hw, BOARD_Z + hh, 0.01F, y, c);
            line(vc, p, light, cx + hw, BOARD_Z - hh, cx + hw, BOARD_Z + hh, 0.01F, y, c);
        }
        PokerTable t = be.table;
        for (int i = 0; i < BlackjackLayout.SEATS; i++) {
            Seat s = t.seats[i];
            double[] c = BlackjackLayout.polar(ANTE_R, BlackjackLayout.seatAngle(i));
            double[] c2 = BlackjackLayout.polar(CALL_R, BlackjackLayout.seatAngle(i));
            int col = s.ante > 0 ? 0xFFE070 : 0xA98626;
            int col2 = s.call > 0 ? 0xFFE070 : 0x8A6E20;
            int lt = light;
            int lt2 = light;
            if (t.phase == Phase.DECIDE && s.state == SeatState.DECIDING) {
                float pulse = 0.65F + 0.35F * Mth.sin(time * 0.35F);
                col2 = scale(0x66FF88, pulse);
                lt2 = FULL;
            }
            ring(vc, p, lt, (float) c[0], (float) c[1], 0.12F, 0.15F, y + 0.001F, col);
            ring(vc, p, lt2, (float) c2[0], (float) c2[1], 0.08F, 0.105F, y + 0.001F, col2);
        }
    }

    private void flatText(PoseStack ps, MultiBufferSource buf, String text, double x, double z, float yaw, int color, float s, int light) {
        ps.pushPose();
        ps.translate(x, TABLE_Y + 0.002F, z);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(s, -s, s);
        float w = font.width(text);
        Matrix4f m = ps.last().pose();
        font.drawInBatch(text, -w / 2.0F, -font.lineHeight / 2.0F + 1, color, false, m, buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    /** Texto curvo a lo largo de un arco (se lee desde el lado de los jugadores). */
    private void arcText(PoseStack ps, MultiBufferSource buf, String text, double r, double centerDeg, double stepDeg, int color, float s, int light) {
        int n = text.length();
        for (int k = 0; k < n; k++) {
            double a = centerDeg + (n - 1) / 2.0 * stepDeg - k * stepDeg;
            double[] c = BlackjackLayout.polar(r, a);
            flatText(ps, buf, String.valueOf(text.charAt(k)), c[0], c[1], (float) (90.0 - a), color, s, light);
        }
    }

    // ------------------------------------------------------------------ cartas

    /** Dibuja una carta en el paño: llega volando del zapato hasta (x, z) y, si hace falta, se voltea. */
    private void drawCard(PoseStack ps, MultiBufferSource buf, int card, boolean faceDown, double flipT, float x, float z, float rotDeg, float born, float time, float stackY, int light) {
        float age = time - born;
        if (age < 0) {
            return;
        }
        float t = Mth.clamp(age / 8.0F, 0, 1);
        float e = 1 - (1 - t) * (1 - t);
        float cx = Mth.lerp(e, SHOE_X, x);
        float cz = Mth.lerp(e, SHOE_Z, z);
        float y = Mth.lerp(e, TABLE_Y + 0.07F, TABLE_Y + stackY) + (float) Math.sin(Math.PI * t) * 0.12F;
        float yaw = Mth.lerp(e, rotDeg + 90.0F, rotDeg);
        float half = CARD_W / 2;
        boolean back = faceDown;
        if (flipT >= 0 && flipT < 1) {
            float cos = Mth.cos((float) flipT * (float) Math.PI);
            back = flipT < 0.5;
            half = Math.max(0.004F, Math.abs(cos) * CARD_W / 2);
            y += (float) Math.sin(Math.PI * flipT) * 0.06F;
        }
        ps.pushPose();
        ps.translate(cx, y, cz);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CARDS));
        cardQuad(vc, ps.last(), half, back ? -1 : card, light);
        ps.popPose();
    }

    private void chipStack(PokerBE be, PoseStack ps, MultiBufferSource buf, Level level, int amount, double[] pos, int seed, int light) {
        List<ItemStack> stacks = Chips.stacksFor(amount);
        int layer = 0;
        for (ItemStack st : stacks) {
            for (int k = 0; k < Math.min(st.getCount(), 4) && layer < 9; k++, layer++) {
                ps.pushPose();
                ps.translate(pos[0] + ((layer * 5) % 3 - 1) * 0.008F, TABLE_Y + 0.012F + layer * 0.014F, pos[1]);
                ps.mulPose(Axis.XP.rotationDegrees(-90));
                ps.scale(0.17F, 0.17F, 0.17F);
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(st.getItem()), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, level, layer + seed);
                ps.popPose();
            }
        }
    }

    @Override
    public void render(PokerBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partial;
        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        int light = LightTexture.pack(Math.max(LightTexture.block(l0), 9), Math.max(LightTexture.sky(l0), 9));
        PokerTable t = be.table;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));

        drawTable(ps, buf, light);
        drawDecor(be, ps, buf, light, time);

        // zapato de cartas
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        box(vc, ps.last(), SHOE_X - 0.1F, Y_FELT, SHOE_Z - 0.1F, SHOE_X + 0.1F, Y_FELT + 0.075F, SHOE_Z + 0.1F, 0x2A1810, light);
        box(vc, ps.last(), SHOE_X - 0.085F, Y_FELT + 0.075F, SHOE_Z - 0.085F, SHOE_X + 0.085F, Y_FELT + 0.083F, SHOE_Z + 0.085F, 0xD8B040, light);

        // texto del paño
        arcText(ps, buf, Component.translatable("casino.poker.felt_text").getString(), 0.78, 90, 6.0, 0xE8C14A, 0.0105F, light);

        // cartas comunes
        for (int j = 0; j < Math.min(5, t.board.size()); j++) {
            drawCard(ps, buf, t.board.get(j), false, -1, (j - 2) * 0.2F, BOARD_Z, 90, be.bornBoard[j], time, 0.0025F * j, light);
        }
        // cartas del crupier: tapadas hasta que las muestra
        int dn = Math.min(2, Math.max(be.dealerCount, t.dealer.size()));
        for (int j = 0; j < dn; j++) {
            boolean shown = t.dealerRevealed && t.dealer.size() > j;
            double flip = shown ? (time - be.revealAt - j * 4) / 10.0 : -1;
            drawCard(ps, buf, shown ? t.dealer.get(j) : -1, !shown, flip, (j - 0.5F) * 0.2F, -0.2F, 90, be.bornDealer[j], time, 0.0025F * j, light);
        }
        if (t.dealerRevealed && t.dealerCat >= 0 && time >= be.revealAt + 14) {
            String name = Component.translatable("casino.poker.hand_" + t.dealerCat).getString();
            flatText(ps, buf, name, -0.9, -0.2, 0, 0xFFFFFF, 0.017F, light);
            flatText(ps, buf, Component.translatable(t.dealerQualified ? "casino.poker.qualifies" : "casino.poker.not_qualifies").getString(), 0.9, -0.2, 0,
                    t.dealerQualified ? 0xFFE070 : 0xFF8888, 0.014F, light);
        }

        for (int i = 0; i < BlackjackLayout.SEATS; i++) {
            Seat s = t.seats[i];
            if (s.ante <= 0) {
                continue;
            }
            double ang = BlackjackLayout.seatAngle(i);
            float rot = (float) (90.0 - ang);
            double[] cp = BlackjackLayout.polar(CARD_R, ang);
            double[] ap = BlackjackLayout.polar(ANTE_R, ang);
            double[] kp = BlackjackLayout.polar(CALL_R, ang);
            chipStack(be, ps, buf, level, s.ante, ap, i * 11, light);
            if (s.call > 0) {
                chipStack(be, ps, buf, level, s.call, kp, i * 11 + 5, light);
            }
            double[] np = BlackjackLayout.polar(NAME_R, ang);
            // las dos cartas del jugador, boca arriba y abiertas en abanico
            double a = Math.toRadians(ang);
            double tx = Math.sin(a);
            double tz = -Math.cos(a);
            boolean folded = s.state == SeatState.FOLDED;
            for (int j = 0; j < Math.min(2, s.hole.size()); j++) {
                double off = (j - 0.5) * 0.17;
                float jitter = (j == 0 ? -4 : 4) + ((i * 7) % 5 - 2);
                drawCard(ps, buf, s.hole.get(j), false, -1, (float) (cp[0] + tx * off), (float) (cp[1] + tz * off), rot + jitter, be.bornHole[i][j], time,
                        0.0025F * j - (folded ? 0.0F : 0.0F), folded ? scaleLight(light) : light);
            }
            boolean cardsIn = s.hole.size() == 2 && time >= be.bornHole[i][1] + 8;
            // nombre de la mano (con las cartas comunes que ya salieron)
            if (cardsIn && !folded && t.board.size() >= 3 && time >= be.bornBoard[2] + 8) {
                List<Integer> all = new java.util.ArrayList<>(s.hole);
                all.addAll(t.board);
                int cat = PokerHands.category(PokerHands.best(all));
                double[] tp = BlackjackLayout.polar(TOTAL_R, ang);
                flatText(ps, buf, Component.translatable("casino.poker.hand_" + cat).getString(), tp[0], tp[1], rot, 0xFFE070, 0.0125F, light);
            }
            boolean showResult = t.phase == Phase.RESULT && time >= be.resultAt && s.result != Result.NONE;
            if (showResult) {
                String key;
                int col;
                switch (s.result) {
                    case WIN -> { key = "casino.poker.tag_win"; col = 0x66FF66; }
                    case WIN_NQ -> { key = "casino.poker.tag_nq"; col = 0x66FF66; }
                    case PUSH -> { key = "casino.poker.tag_push"; col = 0xFFFFFF; }
                    case FOLD -> { key = "casino.poker.tag_fold"; col = 0xAAAAAA; }
                    default -> { key = "casino.poker.tag_lose"; col = 0xFF5555; }
                }
                flatText(ps, buf, Component.translatable(key).getString(), np[0], np[1], rot, col, 0.013F, light);
            } else if (folded) {
                flatText(ps, buf, Component.translatable("casino.poker.tag_fold").getString(), np[0], np[1], rot, 0xAAAAAA, 0.012F, light);
            } else if (!be.names[i].isEmpty()) {
                flatText(ps, buf, be.names[i], np[0], np[1], rot, 0xCFCFCF, 0.01F, light);
            }
        }
        ps.popPose();
    }

    /** Las cartas de quien se retiró se ven más oscuras. */
    private static int scaleLight(int light) {
        int b = Math.max(2, LightTexture.block(light) / 2);
        int s = Math.max(2, LightTexture.sky(light) / 2);
        return LightTexture.pack(b, s);
    }
}
