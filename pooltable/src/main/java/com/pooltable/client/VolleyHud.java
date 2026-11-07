package com.pooltable.client;

import com.pooltable.block.VolleyCourtBE;
import com.pooltable.game.VolleyMatch;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;

/** Lo que se ve en pantalla jugando voleibol: marcador tipo píldora, quién saca, los toques del equipo, carteles de jugada y las teclas. */
public final class VolleyHud {
    private static final int BLUE = 0xFF2D8CFF;
    private static final int ORANGE = 0xFFFF8A1E;

    private VolleyHud() {
    }

    private static void round(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x + 3, y, x + w - 3, y + h, color);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, color);
        g.fill(x, y + 3, x + w, y + h - 3, color);
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        float u = t - 1.0F;
        return 1.0F + c3 * u * u * u + c1 * u * u;
    }

    public static void render(ForgeGui gui, GuiGraphics g, float partial, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || !VolleyClient.playing()) {
            return;
        }
        VolleyCourtBE t = VolleyClient.court;
        if (t == null || !t.clientInit || t.cState == VolleyMatch.State.IDLE.ordinal()) {
            return;
        }
        Font font = mc.font;
        int cx = w / 2;
        drawPill(g, font, t, cx, 6);
        drawTouches(g, t, cx, 40);
        drawPopup(g, font, t, cx, h);
        drawHints(g, font, cx, h);
    }

    private static void drawPill(GuiGraphics g, Font font, VolleyCourtBE t, int cx, int y) {
        int wTotal = 250;
        int hh = 28;
        int x = cx - wTotal / 2;
        round(g, x - 2, y - 2, wTotal + 4, hh + 4, 0xAAFFFFFF);
        round(g, x, y, wTotal, hh, 0xFF16405E);
        round(g, x + 3, y + 3, wTotal / 2 - 3 - 22, hh - 6, BLUE);
        round(g, x + wTotal / 2 + 22, y + 3, wTotal / 2 - 3 - 22, hh - 6, ORANGE);
        round(g, cx - 22, y + 2, 44, hh - 4, 0xFFEAF6FF);
        g.fill(x + 6, y + 4, x + wTotal / 2 - 24, y + 7, 0x55FFFFFF);
        g.fill(x + wTotal / 2 + 24, y + 4, x + wTotal - 6, y + 7, 0x55FFFFFF);
        boolean mineA = VolleyClient.myTeam == 0;
        Component na = Component.translatable("pooltable.voley.team.0");
        Component nb = Component.translatable("pooltable.voley.team.1");
        String a = font.plainSubstrByWidth(na.getString() + (mineA ? " ★" : ""), 90);
        String b = font.plainSubstrByWidth(nb.getString() + (!mineA ? " ★" : ""), 90);
        g.drawString(font, a, x + 10, y + 10, 0xFFFFFFFF, true);
        g.drawString(font, b, x + wTotal - 10 - font.width(b), y + 10, 0xFFFFFFFF, true);
        String score = t.cScoreA + " - " + t.cScoreB;
        g.pose().pushPose();
        g.pose().translate(cx, y + 5, 0);
        g.pose().scale(1.5F, 1.5F, 1.0F);
        g.drawString(font, score, -font.width(score) / 2, 0, 0xFF16405E, false);
        g.pose().popPose();
        // quién saca: una pelotita junto al nombre del equipo
        int bx = t.cServing == 0 ? x + 4 : x + wTotal - 4;
        g.fill(bx - 3, y + hh + 3, bx + 3, y + hh + 9, 0xFFFFFFFF);
        g.fill(bx - 2, y + hh + 4, bx + 2, y + hh + 8, 0xFFFFD23C);
        String first = Component.translatable("pooltable.voley.ui.first", VolleyMatch.WIN).getString();
        g.drawString(font, first, cx - font.width(first) / 2, y + hh + 4, 0xFFFFFFFF, true);
    }

    private static void drawTouches(GuiGraphics g, VolleyCourtBE t, int cx, int y) {
        if (t.cTouchTeam < 0 || t.cState != VolleyMatch.State.RALLY.ordinal()) {
            return;
        }
        int col = t.cTouchTeam == 0 ? BLUE : ORANGE;
        for (int i = 0; i < 3; i++) {
            int x = cx - 18 + i * 14;
            round(g, x, y + 8, 10, 10, 0xFF16405E);
            if (i < t.cTouches) {
                round(g, x + 1, y + 9, 8, 8, col);
            }
        }
    }

    private static void drawPopup(GuiGraphics g, Font font, VolleyCourtBE t, int cx, int h) {
        boolean over = t.cState == VolleyMatch.State.OVER.ordinal();
        String text = null;
        float age = 0;
        int color = 0xFFFFFFFF;
        if (over) {
            int win = t.cScoreA > t.cScoreB ? 0 : 1;
            text = Component.translatable("pooltable.voley.ui.win", Component.translatable("pooltable.voley.team." + win)).getString();
            color = win == 0 ? BLUE : ORANGE;
            age = 1.0F;
        } else if (!t.popup.isEmpty() && (System.nanoTime() - t.popupAt) / 1.0e9F < 1.6F) {
            float sec = (System.nanoTime() - t.popupAt) / 1.0e9F;
            text = Component.translatable("pooltable.voley.popup." + t.popup).getString();
            age = Math.min(1.0F, sec / 0.3F);
            color = switch (t.popup) {
                case "spike" -> 0xFFFF5A3C;
                case "block" -> 0xFF3CD7FF;
                case "net", "out" -> 0xFFFFD23C;
                default -> 0xFFFFFFFF;
            };
        } else if (t.cState == VolleyMatch.State.SERVE.ordinal()) {
            boolean mine = t.cServing == VolleyClient.myTeam;
            text = Component.translatable(mine ? "pooltable.voley.ui.serve.mine" : "pooltable.voley.ui.serve.other").getString();
            age = 1.0F;
        }
        if (text == null) {
            return;
        }
        float s = over ? 3.0F : 2.4F;
        float k = over ? 1.0F : Math.max(0.01F, easeOutBack(age));
        int tw = font.width(text);
        g.pose().pushPose();
        g.pose().translate(cx, h * 0.28F, 0);
        g.pose().scale(s * k, s * k, 1.0F);
        g.drawString(font, text, -tw / 2 + 1, 1, 0xFF16405E, false);
        g.drawString(font, text, -tw / 2, 0, color, false);
        g.pose().popPose();
    }

    private static void drawHints(GuiGraphics g, Font font, int cx, int h) {
        String line = Component.translatable("pooltable.voley.ui.hints", VolleyKeys.DIVE.getTranslatedKeyMessage(), VolleyKeys.CAM.getTranslatedKeyMessage()).getString();
        int tw = font.width(line);
        round(g, cx - tw / 2 - 6, h - 40, tw + 12, 14, 0x99102030);
        g.drawString(font, line, cx - tw / 2, h - 37, 0xFFEAF6FF, false);
    }
}
