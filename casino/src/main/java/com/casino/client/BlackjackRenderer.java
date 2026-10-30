package com.casino.client;

import com.casino.Casino;
import com.casino.block.BlackjackBE;
import com.casino.chip.Chips;
import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackGame.Phase;
import com.casino.game.BlackjackGame.Result;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Dibuja lo que pasa sobre el fieltro: las cartas de los dos (con animación de reparto y de giro de la carta tapada),
 * las fichas apostadas, el zapato de cartas y los totales. Marco local: +z hacia el jugador, +x a su derecha, y = 0 en el piso del bloque.
 */
public class BlackjackRenderer implements BlockEntityRenderer<BlackjackBE> {
    private static final ResourceLocation CARDS = new ResourceLocation(Casino.MODID, "textures/entity/cards.png");
    private static final ResourceLocation WHITE = new ResourceLocation(Casino.MODID, "textures/entity/plinko_white.png");
    private static final float CARD_W = 0.19F;
    private static final float CARD_H = 0.285F;
    private static final float TABLE_Y = 13.0F / 16.0F + 0.004F;
    private static final float PLAYER_Z = 0.22F;
    private static final float DEALER_Z = -0.22F;
    private static final float SPACING = 0.17F;
    private static final float SHOE_X = 1.2F;
    private static final float SHOE_Z = -0.28F;
    private static final float BET_X = 1.0F;
    private static final float BET_Z = 0.2F;
    private static final float TEXT_X = -1.08F;

    private final Font font;

    public BlackjackRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.getFont();
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float w, int light, float nx, float ny, float nz, int rgb) {
        vc.vertex(p.pose(), x, y, z).color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light).normal(p.normal(), nx, ny, nz).endVertex();
    }

    /** Un rectángulo en el plano XY (normal +z), con la parte de arriba de la imagen en y1. */
    private static void quadUV(VertexConsumer vc, PoseStack.Pose p, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int light) {
        v(vc, p, x0, y0, 0, u0, v1, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x1, y0, 0, u1, v1, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x1, y1, 0, u1, v0, light, 0, 0, 1, 0xFFFFFF);
        v(vc, p, x0, y1, 0, u0, v0, light, 0, 0, 1, 0xFFFFFF);
    }

    /** Caja de un solo color (para el zapato). */
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

    private static int scale(int rgb, float k) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * k));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * k));
        int b = Math.min(255, (int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /** Un "rect" de la carta en el atlas (13 columnas x 5 filas; fila 4 = reverso). */
    private static void cardQuad(VertexConsumer vc, PoseStack.Pose p, float halfW, int card, int light) {
        float u0;
        float v0;
        if (card < 0) {
            u0 = 0;
            v0 = 4 / 5.0F;
        } else {
            u0 = BlackjackGame.rank(card) / 13.0F;
            v0 = BlackjackGame.suit(card) / 5.0F;
        }
        quadUV(vc, p, -halfW, -CARD_H / 2, halfW, CARD_H / 2, u0, v0, u0 + 1 / 13.0F, v0 + 1 / 5.0F, light);
    }

    @Override
    public void render(BlackjackBE be, float partial, PoseStack ps, MultiBufferSource buf, int packedLight, int overlay) {
        Level level = be.getLevel();
        if (level == null) {
            return;
        }
        float time = level.getGameTime() + partial;
        int l0 = LevelRenderer.getLightColor(level, be.getBlockPos().above());
        int light = LightTexture.pack(Math.max(LightTexture.block(l0), 9), Math.max(LightTexture.sky(l0), 9));
        BlackjackGame g = be.game;

        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-be.facing().toYRot()));

        // zapato de cartas (siempre está)
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        box(vc, ps.last(), SHOE_X - 0.1F, TABLE_Y - 0.003F, SHOE_Z - 0.15F, SHOE_X + 0.1F, TABLE_Y + 0.07F, SHOE_Z + 0.15F, 0x2A1810, light);
        box(vc, ps.last(), SHOE_X - 0.085F, TABLE_Y + 0.07F, SHOE_Z - 0.13F, SHOE_X + 0.085F, TABLE_Y + 0.078F, SHOE_Z + 0.13F, 0xD8B040, light);

        // cartas del jugador y del crupier
        drawHand(be, ps, buf, g.player, be.pBorn, PLAYER_Z, false, time, light);
        drawHand(be, ps, buf, g.dealer, be.dBorn, DEALER_Z, true, time, light);

        // fichas apostadas
        if (g.phase != Phase.IDLE && g.bet > 0) {
            List<ItemStack> stacks = Chips.stacksFor(g.bet);
            int layer = 0;
            for (ItemStack s : stacks) {
                for (int k = 0; k < Math.min(s.getCount(), 4) && layer < 9; k++, layer++) {
                    ps.pushPose();
                    ps.translate(BET_X + ((layer * 5) % 3 - 1) * 0.01F, TABLE_Y + 0.01F + layer * 0.014F, BET_Z);
                    ps.mulPose(Axis.XP.rotationDegrees(-90));
                    ps.scale(0.2F, 0.2F, 0.2F);
                    Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(s.getItem()), ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, level, layer);
                    ps.popPose();
                }
            }
        }

        // totales y resultado (texto plano sobre el fieltro, a la izquierda)
        if (g.phase != Phase.IDLE && !g.player.isEmpty()) {
            long age = (long) (time - firstShown(be));
            boolean dealt = time >= be.pBorn[Math.min(g.player.size(), BlackjackBE.MAX_CARDS) - 1] + 8;
            if (dealt) {
                String pt = String.valueOf(g.playerTotal());
                if (BlackjackGame.soft(g.player) && g.playerTotal() < 21 && g.playerTotal() > 11) {
                    pt = (g.playerTotal() - 10) + "/" + g.playerTotal();
                }
                flatText(ps, buf, pt, TEXT_X, PLAYER_Z, 0xFFE070, 0.022F, light);
                boolean dealerVisible = time >= be.dBorn[0] + 8;
                if (dealerVisible) {
                    String dt = g.holeRevealed && time >= be.revealAt + 10 ? String.valueOf(BlackjackGame.total(g.dealer)) : g.dealerShownTotal() + "+?";
                    flatText(ps, buf, dt, TEXT_X, DEALER_Z, 0xFFFFFF, 0.022F, light);
                }
            }
            if (g.phase == Phase.DONE && time >= be.doneSeenAt) {
                String key;
                int col;
                switch (g.result) {
                    case BLACKJACK -> { key = "casino.blackjack.tag_bj"; col = 0xFFD23C; }
                    case WIN -> { key = "casino.blackjack.tag_win"; col = 0x66FF66; }
                    case PUSH -> { key = "casino.blackjack.tag_push"; col = 0xFFFFFF; }
                    case BUST -> { key = "casino.blackjack.tag_bust"; col = 0xFF5555; }
                    default -> { key = "casino.blackjack.tag_lose"; col = 0xFF5555; }
                }
                flatText(ps, buf, net.minecraft.network.chat.Component.translatable(key).getString(), TEXT_X, 0.0F, col, 0.017F, light);
            }
        }
        ps.popPose();
    }

    private static float firstShown(BlackjackBE be) {
        return be.pBorn[0];
    }

    private void flatText(PoseStack ps, MultiBufferSource buf, String text, float x, float z, int color, float s, int light) {
        ps.pushPose();
        ps.translate(x, TABLE_Y + 0.002F, z);
        ps.mulPose(Axis.XP.rotationDegrees(-90));
        ps.scale(s, -s, s);
        float w = font.width(text);
        Matrix4f m = ps.last().pose();
        font.drawInBatch(text, -w / 2.0F, -font.lineHeight / 2.0F + 1, color, false, m, buf, Font.DisplayMode.NORMAL, 0, light);
        ps.popPose();
    }

    private void drawHand(BlackjackBE be, PoseStack ps, MultiBufferSource buf, List<Integer> hand, long[] born, float z, boolean dealer, float time, int light) {
        int n = Math.min(hand.size(), BlackjackBE.MAX_CARDS);
        for (int i = 0; i < n; i++) {
            float age = time - born[i];
            if (age < 0) {
                continue; // todavía no se reparte
            }
            float tx = (i - (n - 1) / 2.0F) * SPACING;
            float ty = TABLE_Y + 0.002F * i;
            float rot = ((i * 37 + (dealer ? 11 : 0)) % 9 - 4) * 1.6F;
            float t = Mth.clamp(age / 8.0F, 0, 1);
            float e = 1 - (1 - t) * (1 - t);
            float x = Mth.lerp(e, SHOE_X, tx);
            float zz = Mth.lerp(e, SHOE_Z, z);
            float y = Mth.lerp(e, TABLE_Y + 0.07F, ty) + (float) Math.sin(Math.PI * t) * 0.12F;
            float yaw = Mth.lerp(e, 90.0F, rot);
            float half = CARD_W / 2;
            boolean faceDown = false;
            if (dealer && i == 1 && !be.game.holeRevealed) {
                faceDown = true;
            } else if (dealer && i == 1) {
                // se da vuelta: el ancho se achica hasta 0 (mostrando el reverso) y vuelve a crecer (mostrando la cara)
                float f = Mth.clamp((time - be.revealAt) / 10.0F, 0, 1);
                if (f < 1) {
                    float cos = Mth.cos(f * (float) Math.PI);
                    faceDown = f < 0.5F;
                    half = Math.max(0.004F, Math.abs(cos) * CARD_W / 2);
                    y += (float) Math.sin(Math.PI * f) * 0.06F;
                }
            }
            ps.pushPose();
            ps.translate(x, y, zz);
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            ps.mulPose(Axis.XP.rotationDegrees(-90));
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(CARDS));
            cardQuad(vc, ps.last(), half, faceDown ? -1 : hand.get(i), light);
            ps.popPose();
        }
    }
}
