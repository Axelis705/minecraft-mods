package com.pooltable.client;

import com.pooltable.block.BowlingLaneBE;
import com.pooltable.game.BowlingAim;
import com.pooltable.game.BowlingGame;
import com.pooltable.game.BowlingScore;
import com.pooltable.item.BowlingBallItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** La barra de fuerza al cargar la bola y el marcador de la partida de la pista más cercana. */
public final class BowlingHud {
    private static final int NAME_W = 64;
    private static final int CELL = 26;
    private static final int ROW_H = 20;

    private BowlingHud() {
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) {
            return;
        }
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof BowlingBallItem) {
            drawPower(g, mc, p, partial, w, h);
        }
        BowlingLaneBE lane = BowlingLaneBE.nearestClient(p.level(), p.position(), 16.0);
        if (lane != null && lane.playing && lane.game != null) {
            drawBoard(g, mc.font, lane, w);
        }
    }

    private static void drawPower(GuiGraphics g, Minecraft mc, LocalPlayer p, float partial, int w, int h) {
        int cx = w / 2;
        int y = h - 62;
        if (BowlingLaneBE.findUnder(p.level(), p.position()) == null) {
            g.drawCenteredString(mc.font, Component.translatable("pooltable.bowling.not_on_lane"), cx, y, 0xFFFF6060);
            return;
        }
        float ticks = p.getTicksUsingItem() + partial;
        double m = BowlingAim.meter(ticks);
        int bw = 120;
        int x0 = cx - bw / 2;
        g.fill(x0 - 2, y - 2, x0 + bw + 2, y + 10, 0xC0101418);
        g.fill(x0, y, x0 + bw, y + 8, 0xFF2A3038);
        int zl = x0 + (int) Math.round(bw * BowlingAim.SWEET_LO);
        int zh = x0 + (int) Math.round(bw * BowlingAim.SWEET_HI);
        for (int i = 0; i < bw; i++) {
            float k = i / (float) bw;
            g.fill(x0 + i, y, x0 + i + 1, y + 8, 0xFF000000 | ((int) (80 + 120 * k) << 16) | ((int) (150 - 90 * k) << 8) | 60);
        }
        g.fill(zl, y - 1, zh, y + 9, 0xFF3CDC5A);
        int mx = x0 + (int) Math.round(bw * m);
        g.fill(mx - 1, y - 4, mx + 2, y + 12, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, Component.translatable("pooltable.bowling.hud.release"), cx, y - 14, 0xFFFFFFFF);
        g.drawCenteredString(mc.font, Component.translatable("pooltable.bowling.hud.spin"), cx, y + 15, 0xFFB0B8C0);
    }

    private static void drawBoard(GuiGraphics g, Font font, BowlingLaneBE lane, int screenW) {
        BowlingGame game = lane.game;
        int n = game.players();
        int boardW = NAME_W + CELL * 10 + 34;
        float scale = Math.min(1.0F, (screenW - 12) / (float) boardW);
        int x0 = Math.round((screenW - boardW * scale) / 2.0F);
        g.pose().pushPose();
        g.pose().translate(x0, 4, 0);
        g.pose().scale(scale, scale, 1.0F);
        int boardH = 14 + n * ROW_H + 2;
        g.fill(-2, -2, boardW + 2, boardH + 2, 0xB0101820);
        g.fill(0, 0, boardW, 12, 0xFF2B4A6B);
        for (int f = 0; f < 10; f++) {
            g.drawCenteredString(font, String.valueOf(f + 1), NAME_W + f * CELL + CELL / 2, 2, 0xFFFFFF);
        }
        g.drawCenteredString(font, "Tot", NAME_W + 10 * CELL + 17, 2, 0xFFFFFF);
        for (int i = 0; i < n; i++) {
            int y = 14 + i * ROW_H;
            boolean cur = !game.over && game.current == i;
            if (cur) {
                g.fill(0, y, boardW, y + ROW_H - 1, 0x60FFD23C);
            }
            String name = i < lane.pnames.size() ? lane.pnames.get(i) : "?";
            while (font.width(name) > NAME_W - 4 && name.length() > 1) {
                name = name.substring(0, name.length() - 1);
            }
            g.drawString(font, Component.literal(name).withStyle(cur ? ChatFormatting.GOLD : ChatFormatting.WHITE), 2, y + 6, 0xFFFFFF, false);
            BowlingScore sc = game.scores[i];
            int[] cum = sc.cumulative();
            for (int f = 0; f < 10; f++) {
                int cx = NAME_W + f * CELL;
                g.fill(cx, y, cx + 1, y + ROW_H - 1, 0x80FFFFFF);
                String[] marks = sc.marks(f);
                StringBuilder sb = new StringBuilder();
                for (String m : marks) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(m);
                }
                g.drawString(font, sb.toString(), cx + 3, y + 1, 0xFFE0E6EC, false);
                if (cum[f] >= 0) {
                    g.drawString(font, String.valueOf(cum[f]), cx + 3, y + 10, 0xFFFFFFFF, false);
                }
            }
            g.fill(NAME_W + 10 * CELL, y, NAME_W + 10 * CELL + 1, y + ROW_H - 1, 0x80FFFFFF);
            g.drawString(font, Component.literal(String.valueOf(sc.total())).withStyle(ChatFormatting.YELLOW), NAME_W + 10 * CELL + 6, y + 6, 0xFFFFFF, false);
        }
        g.pose().popPose();
    }
}
