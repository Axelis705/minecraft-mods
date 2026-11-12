package com.casino.client;

import com.casino.diamond.RaceBE;
import com.casino.game.HorseRace;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/** Aviso a un lado de la pantalla cuando estás sentado frente a la pantalla de carreras: "Presiona B para apostar" y el estado. */
public final class RaceHud {
    private static float slide;
    private static long lastFrame;

    private RaceHud() {
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        RaceBE be = RaceClient.active();
        long ms = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (ms - lastFrame) / 1000F);
        lastFrame = ms;
        boolean show = be != null && ClientScreens.none() && mc.level != null && mc.player != null;
        slide = Math.max(0, Math.min(1, slide + (show ? dt * 5 : -dt * 5)));
        if (slide <= 0 || be == null || mc.level == null || mc.player == null) {
            return;
        }
        Font font = mc.font;
        long now = mc.level.getGameTime();
        HorseRace r = be.race();
        RaceBE.Phase ph = be.phase(now);
        int w = 168;
        int h = 74;
        float ease = 1 - (1 - slide) * (1 - slide);
        int x = sw - (int) (w * ease) - 6;
        int y = sh / 2 - h / 2 - 10;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xC0C8A040);
        g.fill(x, y, x + w, y + h, 0xE0100E18);
        g.fill(x, y, x + w, y + 14, 0xF02A1E08);
        g.drawString(font, Component.translatable("casino.race.hud_title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), x + 6, y + 3, 0xFFFFFF, true);
        // botón de la tecla
        Component key = RaceClient.keyName();
        int kw = font.width(key) + 8;
        boolean open = ph == RaceBE.Phase.BET;
        int ky = y + 20;
        g.fill(x + 6, ky - 2, x + 6 + kw, ky + 10, open ? 0xFFE8E0C8 : 0xFF6A6470);
        g.fill(x + 6, ky + 8, x + 6 + kw, ky + 10, open ? 0xFF9A8A60 : 0xFF46404C);
        g.drawString(font, key, x + 10, ky, 0xFF141018, false);
        g.drawString(font, Component.translatable(open ? "casino.race.hud_press" : "casino.race.hud_view"), x + 12 + kw, ky, 0xFFE8E4F0, true);
        // estado
        Component status;
        int col;
        switch (ph) {
            case BET -> {
                status = Component.translatable("casino.race.hud_next", RaceArt.time(be.betTicksLeft(now)));
                col = be.betTicksLeft(now) < 200 ? 0xFFFF7070 : 0xFF70FF9A;
            }
            case GATE -> {
                status = Component.translatable("casino.race.hud_gate");
                col = 0xFFFFD040;
            }
            case RUN -> {
                status = Component.translatable("casino.race.hud_running");
                col = 0xFFFFD040;
            }
            default -> {
                status = Component.translatable("casino.race.hud_results", r.order[0] + 1);
                col = 0xFFFFD040;
            }
        }
        g.drawString(font, status, x + 6, y + 36, col, true);
        RaceBE.Bet b = be.bets.get(mc.player.getUUID());
        if (b != null) {
            RaceArt.badge(g, font, r, b.horse(), x + 6, y + 50, 11);
            String name = font.plainSubstrByWidth(RaceBE.horseName(r, b.horse()).getString(), w - 30);
            g.drawString(font, name, x + 22, y + 48, 0xFFFFFFFF, true);
            g.drawString(font, Component.translatable("casino.race.hud_bet", String.format("%,d", b.amount()),
                    String.format("%,d", r.payout(b.horse(), b.amount()))), x + 22, y + 59, 0xFFC8C0DC, false);
        } else {
            g.drawString(font, Component.translatable("casino.race.hud_nobet"), x + 6, y + 54, 0xFF9890B0, false);
        }
    }
}
