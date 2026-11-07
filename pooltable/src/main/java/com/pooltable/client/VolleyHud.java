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
        drawTouches(g, t, cx, 48);
        drawPopup(g, font, t, cx, h);
        drawQuality(g, font, t, cx, h);
        drawServeBar(g, font, t, cx, h);
        drawHints(g, font, t, cx, h);
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
        String first;
        if (t.cBestOf > 1) {
            first = Component.translatable("pooltable.voley.ui.set", t.cSetNo, t.cSetsA, t.cSetsB, t.cTarget).getString();
        } else {
            first = Component.translatable("pooltable.voley.ui.first", t.cTarget).getString();
        }
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
            int win = t.cSetsA > t.cSetsB ? 0 : 1;
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
                case "net", "out", "fault" -> 0xFFFFD23C;
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

    /** Cómo salió mi último golpe: da ánimo (o avisa) para que se note qué se hizo bien. */
    private static void drawQuality(GuiGraphics g, Font font, VolleyCourtBE t, int cx, int h) {
        if (t.cQuality <= 0 || t.cQualityBy != VolleyClient.myIdx) {
            return;
        }
        float sec = (System.nanoTime() - t.qualityAt) / 1.0e9F;
        if (sec > 0.9F) {
            return;
        }
        String key = t.cQuality >= 3 ? "perfect" : (t.cQuality == 2 ? "good" : "bad");
        int color = t.cQuality >= 3 ? 0xFF5CFF8A : (t.cQuality == 2 ? 0xFFFFFFFF : 0xFFFF7A5C);
        String text = Component.translatable("pooltable.voley.quality." + key).getString();
        float k = Math.max(0.01F, easeOutBack(Math.min(1.0F, sec / 0.18F)));
        float a = Math.min(1.0F, (0.9F - sec) / 0.3F);
        int alpha = (int) (255 * Math.max(0.05F, a));
        g.pose().pushPose();
        g.pose().translate(cx, h * 0.62F - sec * 14.0F, 0);
        g.pose().scale(1.6F * k, 1.6F * k, 1.0F);
        g.drawString(font, text, -font.width(text) / 2, 0, (alpha << 24) | (color & 0xFFFFFF), true);
        g.pose().popPose();
    }

    /** La barra de fuerza del saque: la zona verde es la buena; más flojo se queda corto o en la red, más fuerte se pasa. */
    private static void drawServeBar(GuiGraphics g, Font font, VolleyCourtBE t, int cx, int h) {
        if (!VolleyClient.iServe()) {
            return;
        }
        int bw = 180;
        int bx = cx - bw / 2;
        int by = h - 70;
        round(g, bx - 3, by - 3, bw + 6, 16, 0xCC102030);
        g.fill(bx, by, bx + bw, by + 10, 0xFF2A3B4C);
        g.fill(bx, by, bx + (int) (bw * 0.3), by + 10, 0xFF8C3A3A);
        g.fill(bx + (int) (bw * VolleyMatch.SERVE_LO), by, bx + (int) (bw * VolleyMatch.SERVE_HI), by + 10, 0xFF3CC86A);
        g.fill(bx + (int) (bw * 0.93), by, bx + bw, by + 10, 0xFF8C3A3A);
        float c = VolleyClient.chargeNow();
        if (c >= 0) {
            int mx = bx + (int) (bw * c);
            g.fill(mx - 1, by - 3, mx + 2, by + 13, 0xFFFFFFFF);
        }
        String txt = Component.translatable(c >= 0 ? "pooltable.voley.ui.serve.release" : "pooltable.voley.ui.serve.hold").getString();
        g.drawString(font, txt, cx - font.width(txt) / 2, by - 14, 0xFFFFFFFF, true);
        if (t.cTimer < 10.0F) {
            String left = Component.translatable("pooltable.voley.ui.serve.time", (int) Math.ceil(t.cTimer)).getString();
            g.drawString(font, left, cx - font.width(left) / 2, by + 16, t.cTimer < 5.0F ? 0xFFFF7A5C : 0xFFEAF6FF, true);
        }
    }

    private static void drawHints(GuiGraphics g, Font font, VolleyCourtBE t, int cx, int h) {
        String line = Component.translatable("pooltable.voley.ui.hints", VolleyKeys.DIVE.getTranslatedKeyMessage(), VolleyKeys.CAM.getTranslatedKeyMessage(),
                VolleyKeys.MENU.getTranslatedKeyMessage()).getString();
        int tw = font.width(line);
        round(g, cx - tw / 2 - 6, h - 40, tw + 12, 14, 0x99102030);
        g.drawString(font, line, cx - tw / 2, h - 37, 0xFFEAF6FF, false);
    }
}
