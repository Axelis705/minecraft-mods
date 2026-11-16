package com.casino.client;

import com.casino.heist.HeistActionPacket;
import com.casino.heist.HeistSyncPacket;
import com.casino.net.CasinoNet;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Panel del robo arriba al centro: la fase, el objetivo, la barra de sospecha, la combinación que llevas, el tiempo de la bóveda y lo
 * que cargas en la mochila. Con la alarma parpadea en rojo. A quien está en el casino durante el escape le sale el aviso "pulsa H para
 * ayudar a seguridad".
 */
public final class HeistHud {
    //#if MC >= 26.1
    //$$ public static final KeyMapping HELP = new KeyMapping("key.casino.heist_help", com.mojang.blaze3d.platform.InputConstants.KEY_H, RaceClient.CAT);
    //#else
    public static final KeyMapping HELP = new KeyMapping("key.casino.heist_help", com.mojang.blaze3d.platform.InputConstants.KEY_H, "key.categories.casino");
    //#endif

    private static HeistSyncPacket data;
    private static float slide;
    private static long lastFrame;
    private static long offerUntil;
    private static int offerRobbers;
    private static float shownSus;

    private HeistHud() {
    }

    public static void set(HeistSyncPacket m) {
        data = m.role == 0 ? null : m;
        if (m.role != 0) {
            offerUntil = 0;
        }
    }

    public static void offer(int robbers) {
        offerUntil = System.currentTimeMillis() + 15000;
        offerRobbers = robbers;
    }

    /** ¿Hay un robo en marcha para este jugador? (lo usa la mini tarjeta para hacerse a un lado) */
    public static boolean active() {
        return data != null;
    }

    private static String time(int ticks) {
        int s = Math.max(0, ticks) / 20;
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        long ms = System.currentTimeMillis();
        float dt = lastFrame == 0 ? 0 : Math.min(0.1F, (ms - lastFrame) / 1000F);
        lastFrame = ms;
        Font font = mc.font;
        while (HELP.consumeClick()) {
            if (ms < offerUntil && mc.player != null) {
                CasinoNet.sendToServer(new HeistActionPacket(HeistActionPacket.HELP, BlockPos.ZERO));
                offerUntil = 0;
            }
        }
        if (ms < offerUntil && data == null) {
            boolean on = (ms / 400) % 2 == 0;
            Component a = Component.translatable("casino.heist.hud.offer", offerRobbers).withStyle(ChatFormatting.BOLD);
            Component b = Component.translatable("casino.heist.hud.offer_key", HELP.getTranslatedKeyMessage());
            int w = Math.max(font.width(a), font.width(b)) + 16;
            int x = sw / 2 - w / 2, y = sh - 92;
            g.fill(x, y, x + w, y + 26, 0xC0100008);
            g.fill(x, y, x + w, y + 1, on ? 0xFFFF3030 : 0xFF3060FF);
            g.drawString(font, a, sw / 2 - font.width(a) / 2, y + 4, on ? 0xFFFF6060 : 0xFF80A0FF, true);
            g.drawString(font, b, sw / 2 - font.width(b) / 2, y + 15, 0xFFE8E8F0, false);
        }
        //#if MC >= 26.1
        //$$ boolean show = data != null;
        //#else
        boolean show = data != null && !mc.options.hideGui;
        //#endif
        slide = Math.max(0, Math.min(1, slide + (show ? dt * 4 : -dt * 4)));
        if (slide <= 0 || data == null) {
            return;
        }
        HeistSyncPacket d = data;
        float ease = 1 - (1 - slide) * (1 - slide);
        int w = 230;
        int x = sw / 2 - w / 2;
        int y = 4 - (int) ((1 - ease) * 90);
        boolean alarm = d.alarm;
        boolean flash = alarm && (ms / 300) % 2 == 0;
        int lines = 3 + (d.role == 1 && d.phase == 0 && !alarm ? 1 : 0) + (d.role == 1 && (d.combo[0] >= 0 || d.combo[1] >= 0 || d.combo[2] >= 0 || d.objective == 1 || d.objective == 2) && d.phase == 0 ? 1 : 0)
                + (d.timer != 0 ? 1 : 0) + (d.role == 1 && d.phase >= 2 ? 1 : 0);
        int h = 8 + lines * 11;
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, flash ? 0xFFFF2020 : alarm ? 0xFF801010 : 0xFFC9A227);
        g.fillGradient(x, y, x + w, y + h, 0xE8100C16, 0xE8080610);
        int ty = y + 5;
        Component title = Component.translatable(d.role == 2 ? "casino.heist.hud.cop_title" : "casino.heist.hud.title").withStyle(ChatFormatting.BOLD);
        g.drawString(font, title, x + 6, ty, d.role == 2 ? 0xFF7FC8FF : 0xFFFFD45A, true);
        if (alarm) {
            Component al = Component.translatable("casino.heist.hud.alarm").withStyle(ChatFormatting.BOLD);
            g.drawString(font, al, x + w - 6 - font.width(al), ty, flash ? 0xFFFF4040 : 0xFFB02020, true);
        }
        ty += 12;
        Component obj = Component.translatable("casino.heist.hud.obj_" + d.objective, d.robbers);
        g.drawString(font, font.plainSubstrByWidth(obj.getString(), w - 12), x + 6, ty, 0xFFFFFFFF, false);
        ty += 11;
        if (d.role == 1 && d.phase == 0 && !alarm) {
            shownSus += (d.suspicion - shownSus) * Math.min(1, dt * 6);
            g.drawString(font, Component.translatable("casino.heist.hud.suspicion"), x + 6, ty, 0xFFB8BCD0, false);
            int bx = x + 70, bw = w - 76;
            g.fill(bx, ty + 1, bx + bw, ty + 8, 0xFF000000);
            int fw = (int) ((bw - 2) * Math.min(1, shownSus / 100F));
            int col = shownSus < 40 ? 0xFF50D070 : shownSus < 75 ? 0xFFE8C040 : 0xFFE84040;
            if (fw > 0) {
                g.fill(bx + 1, ty + 2, bx + 1 + fw, ty + 7, col);
            }
            ty += 11;
        }
        if (d.role == 1 && d.phase == 0 && (d.combo[0] >= 0 || d.combo[1] >= 0 || d.combo[2] >= 0 || d.objective == 1 || d.objective == 2)) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                sb.append(i > 0 ? " · " : "").append(d.combo[i] >= 0 ? String.format(Locale.ROOT, "%02d", d.combo[i]) : "??");
            }
            g.drawString(font, Component.translatable("casino.heist.hud.combo", sb.toString()), x + 6, ty, 0xFF80E0FF, false);
            ty += 11;
        }
        if (d.timer > 0) {
            boolean low = d.timer < 400;
            Component tm = Component.translatable("casino.heist.hud.vault_time", time(d.timer)).withStyle(ChatFormatting.BOLD);
            g.drawString(font, tm, x + 6, ty, low && (ms / 250) % 2 == 0 ? 0xFFFF4040 : 0xFFFFFFFF, true);
            ty += 11;
        } else if (d.timer < 0) {
            g.drawString(font, Component.translatable("casino.heist.hud.cams_off", time(-d.timer)), x + 6, ty, 0xFF70FF9A, false);
            ty += 11;
        }
        if (d.role == 1 && d.phase >= 2) {
            g.drawString(font, Component.translatable("casino.heist.hud.bag", String.format(Locale.ROOT, "%,d", d.bag)), x + 6, ty, 0xFFFFD45A, false);
            for (int i = 0; i < d.maxSlots; i++) {
                int sx = x + w - 6 - (d.maxSlots - i) * 9;
                g.fill(sx, ty, sx + 7, ty + 8, 0xFF2A2436);
                if (i < d.slots) {
                    g.fill(sx + 1, ty + 1, sx + 6, ty + 7, 0xFFE8B83A);
                }
            }
        }
    }
}
