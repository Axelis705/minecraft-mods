package com.casino.client;

import com.casino.game.BullRide;
import com.casino.game.GalleryPlan;
import com.casino.plat.client.G;
import com.casino.west.BullRideEntity;
import com.casino.west.GalleryBE;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Avisos en pantalla de los juegos del saloon. Toro: la barra de equilibrio con su aguja (¡mantenla en el centro con A y D!), el
 * tiempo, el premio que llevas y el siguiente. Galería: tiempo, balas, puntos y el premio que llevas.
 */
public final class WestHud {
    private static long seenTick = -1;
    private static long seenAt;

    private WestHud() {
    }

    /** Fracción del tick actual (0..1), medida con el reloj: igual en todas las versiones. */
    private static float partial(Minecraft mc) {
        long gt = mc.level.getGameTime();
        long now = System.nanoTime();
        if (gt != seenTick) {
            seenTick = gt;
            seenAt = now;
        }
        return Math.max(0F, Math.min(1F, (now - seenAt) / 50_000_000F));
    }

    public static void render(G g, int sw, int sh) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        float pt = partial(mc);
        if (mc.player.getVehicle() instanceof BullRideEntity e) {
            bull(g, mc.font, e, sw, sh, pt);
            return;
        }
        com.casino.west.DuelBE db = WestClient.duel();
        if (db != null) {
            duel(g, mc, db, sw, sh, pt);
            return;
        }
        GalleryBE gb = WestClient.gallery();
        if (gb != null) {
            gallery(g, mc.font, gb, sw, sh, pt);
        }
    }

    private static String x(int x10) {
        return x10 % 10 == 0 ? "x" + x10 / 10 : "x" + x10 / 10 + "." + x10 % 10;
    }

    private static void big(G g, Font f, Component c, int cx, int cy, float scale, int color) {
        g.push();
        g.translate(cx, cy, 0);
        g.scale(scale, scale, 1);
        g.drawString(f, c, -f.width(c) / 2, -4, color, true);
        g.pop();
    }

    // ------------------------------------------------------------------ toro

    private static void bull(G g, Font f, BullRideEntity e, int sw, int sh, float pt) {
        int clock = e.clock();
        int cx = sw / 2;
        if (clock < BullRide.COUNTDOWN) {
            int n = (BullRide.COUNTDOWN - clock + 19) / 20;
            big(g, f, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.BOLD), cx, sh / 2 - 40, 4F, 0xFFFFD040);
            Component hint = Component.translatable("casino.bull.hud_hint", Minecraft.getInstance().options.keyLeft.getTranslatedKeyMessage(),
                    Minecraft.getInstance().options.keyRight.getTranslatedKeyMessage());
            g.drawCenteredString(f, hint, cx, sh / 2 - 8, 0xFFFFFFFF);
            g.drawCenteredString(f, Component.translatable("casino.bull.hud_cashout"), cx, sh / 2 + 4, 0xFFC8C0B0);
            return;
        }
        int t = e.rideTicks();
        if (t < 16) {
            big(g, f, Component.translatable("casino.bull.go").withStyle(ChatFormatting.BOLD), cx, sh / 2 - 40, 3F, 0xFFFF7040);
        }
        // barra de equilibrio
        int bw = 182, bh = 12;
        int bx = cx - bw / 2, by = sh - 74;
        g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, 0xE0201008);
        for (int i = 0; i < bw; i++) {
            float k = Math.abs(i - bw / 2F) / (bw / 2F);       // 0 centro, 1 orilla
            int col = k < 0.35F ? 0xFF3CB043 : k < 0.7F ? 0xFFE0B020 : 0xFFD03020;
            g.fill(bx + i, by, bx + i + 1, by + bh, col);
        }
        g.fill(cx, by - 3, cx + 1, by + bh + 3, 0xFFFFFFFF);
        float lean = Math.max(-1F, Math.min(1F, e.lean(pt)));
        int nx = cx + Math.round(lean * bw / 2F);
        g.fill(nx - 2, by - 5, nx + 2, by + bh + 5, 0xFF101010);
        g.fill(nx - 1, by - 4, nx + 1, by + bh + 4, 0xFFFFF4D0);
        // flechas: cuál conviene apretar
        int in = WestClient.input();
        Component lk = Minecraft.getInstance().options.keyLeft.getTranslatedKeyMessage();
        Component rk = Minecraft.getInstance().options.keyRight.getTranslatedKeyMessage();
        boolean needLeft = lean > 0.25F, needRight = lean < -0.25F;
        g.drawString(f, Component.literal("◀ ").append(lk), bx - 30, by + 2, in < 0 ? 0xFFFFFFFF : needLeft ? 0xFFFF6040 : 0xFF908070, true);
        g.drawString(f, rk.copy().append(" ▶"), bx + bw + 8, by + 2, in > 0 ? 0xFFFFFFFF : needRight ? 0xFFFF6040 : 0xFF908070, true);
        // tiempo y premio
        String secs = String.format(Locale.ROOT, "%.1f s", t / 20.0);
        big(g, f, Component.literal(secs).withStyle(ChatFormatting.BOLD), cx, by - 22, 2F, 0xFFFFFFFF);
        int x10 = BullRide.payoutX10(t);
        int tier = BullRide.tier(t);
        Component now = x10 > 0 ? Component.translatable("casino.bull.hud_now", (long) e.bet() * x10 / 10, x(x10)).withStyle(ChatFormatting.GREEN)
                : Component.translatable("casino.bull.hud_none").withStyle(ChatFormatting.GRAY);
        g.drawCenteredString(f, now, cx, by + bh + 6, 0xFFFFFFFF);
        if (tier + 1 < BullRide.TIER_TICKS.length) {
            int next = BullRide.TIER_TICKS[tier + 1];
            Component nc = Component.translatable("casino.bull.hud_next", String.format(Locale.ROOT, "%.1f", Math.max(0, next - t) / 20.0),
                    x(BullRide.TIER_X10[tier + 1]));
            g.drawCenteredString(f, nc, cx, by + bh + 17, 0xFFE0C890);
            // barrita de avance al siguiente premio
            int prev = tier < 0 ? 0 : BullRide.TIER_TICKS[tier];
            float k = (t - prev) / (float) (next - prev);
            g.fill(cx - 60, by + bh + 28, cx + 60, by + bh + 30, 0x80402010);
            g.fill(cx - 60, by + bh + 28, cx - 60 + Math.round(120 * k), by + bh + 30, 0xFFFFD040);
        }
    }

    // ------------------------------------------------------------------ galería

    private static void gallery(G g, Font f, GalleryBE be, int sw, int sh, float pt) {
        double t = be.time(pt);
        int cx = sw / 2;
        if (t < 0) {
            int n = (int) Math.ceil(-t / 20.0);
            big(g, f, Component.literal(String.valueOf(n)).withStyle(ChatFormatting.BOLD), cx, sh / 2 - 40, 4F, 0xFFFFD040);
            g.drawCenteredString(f, Component.translatable("casino.gallery.hud_hint"), cx, sh / 2 - 8, 0xFFFFFFFF);
            return;
        }
        if (t < 16) {
            big(g, f, Component.translatable("casino.gallery.go").withStyle(ChatFormatting.BOLD), cx, sh / 2 - 40, 3F, 0xFFFF7040);
        }
        int w = 150, h = 50;
        int x = sw - w - 8, y = 8;
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0xFFC08A3E);
        g.fill(x, y, x + w, y + h, 0xE0301A0C);
        int left = (int) Math.max(0, Math.ceil((GalleryPlan.DURATION - t) / 20.0));
        g.drawString(f, Component.translatable("casino.gallery.hud_time", left).withStyle(left <= 5 ? ChatFormatting.RED : ChatFormatting.WHITE), x + 6, y + 5, 0xFFFFFFFF, true);
        Component sc = Component.translatable("casino.gallery.hud_score", be.score()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(f, sc, x + w - 6 - f.width(sc), y + 5, 0xFFFFFFFF, true);
        // balas
        for (int i = 0; i < GalleryPlan.SHOTS; i++) {
            int bx = x + 6 + i * 11;
            boolean full = i < be.shots();
            g.fill(bx, y + 18, bx + 6, y + 30, full ? 0xFF8A6A2A : 0xFF3A2A1A);
            g.fill(bx + 1, y + 16, bx + 5, y + 18, full ? 0xFFD8B060 : 0xFF3A2A1A);
        }
        int x10 = GalleryPlan.payoutX10(Math.max(0, be.score()));
        Component now = x10 > 0 ? Component.translatable("casino.gallery.hud_now", (long) be.bet() * x10 / 10, x(x10)).withStyle(ChatFormatting.GREEN)
                : Component.translatable("casino.gallery.hud_need", GalleryPlan.SCORE_AT[0]).withStyle(ChatFormatting.GRAY);
        g.drawString(f, now, x + 6, y + 37, 0xFFFFFFFF, false);
    }

    // ------------------------------------------------------------------ duelo

    private static long drawFor = Long.MIN_VALUE;
    private static long drawNanos;

    private static void duel(G g, Minecraft mc, com.casino.west.DuelBE be, int sw, int sh, float pt) {
        Font f = mc.font;
        int cx = sw / 2;
        Component name = Component.translatable("casino.npc.duelist_" + (com.casino.entity.DealerEntity.WEST + com.casino.game.DuelRules.SKIN[be.opponent()]));
        if (be.phase() == com.casino.west.DuelBE.RESULT) {
            int r = be.result();
            Component head = switch (r) {
                case com.casino.west.DuelBE.R_WIN -> Component.translatable("casino.duel.hud_win").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
                case com.casino.west.DuelBE.R_LOSE -> Component.translatable("casino.duel.hud_lose", name).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                case com.casino.west.DuelBE.R_EARLY -> Component.translatable("casino.duel.hud_early").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                case com.casino.west.DuelBE.R_LOOKED -> Component.translatable("casino.duel.hud_looked").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                default -> Component.translatable("casino.duel.hud_left").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            };
            big(g, f, head, cx, sh / 2 - 46, 2.5F, 0xFFFFFFFF);
            if (r == com.casino.west.DuelBE.R_WIN) {
                g.drawCenteredString(f, Component.translatable("casino.duel.hud_time", String.format(Locale.ROOT, "%.2f", be.reaction() / 20.0)), cx, sh / 2 - 24, 0xFFFFFFFF);
                g.drawCenteredString(f, Component.translatable("casino.duel.won", be.pay()).withStyle(ChatFormatting.GREEN), cx, sh / 2 - 12, 0xFFFFFFFF);
            } else {
                g.drawCenteredString(f, Component.translatable("casino.duel.lost", be.bet()).withStyle(ChatFormatting.RED), cx, sh / 2 - 24, 0xFFFFFFFF);
            }
            if (r == com.casino.west.DuelBE.R_LOSE) {
                // destello rojo del golpe
                g.fill(0, 0, sw, sh, 0x30FF0000);
            }
            return;
        }
        double t = be.time(pt);
        boolean holstered = mc.player != null && mc.player.getXRot() >= com.casino.game.DuelRules.HOLSTER_PITCH;
        if (!be.drawn(pt)) {
            int tolls = t >= com.casino.game.DuelRules.TOLL_3 ? 3 : t >= com.casino.game.DuelRules.TOLL_2 ? 2 : t >= com.casino.game.DuelRules.TOLL_1 ? 1 : 0;
            if (tolls == 0) {
                big(g, f, Component.translatable("casino.duel.hud_vs", name).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, 40, 2F, 0xFFFFFFFF);
            } else {
                big(g, f, Component.literal(String.valueOf(tolls)).withStyle(ChatFormatting.BOLD), cx, 44, 4F, 0xFFE8D8B0);
            }
            if (be.noon()) {
                g.drawCenteredString(f, Component.translatable("casino.duel.hud_noon").withStyle(ChatFormatting.YELLOW), cx, 66, 0xFFFFFFFF);
            }
            Component hol = holstered ? Component.translatable("casino.duel.hud_holstered").withStyle(ChatFormatting.GREEN)
                    : Component.translatable("casino.duel.hud_look_down").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            g.drawCenteredString(f, hol, cx, sh / 2 + 18, 0xFFFFFFFF);
            g.drawCenteredString(f, Component.translatable("casino.duel.hud_hint"), cx, sh / 2 + 30, 0xFFD0C0A0);
            return;
        }
        long now = System.nanoTime();
        if (drawFor != be.drawAt()) {
            drawFor = be.drawAt();
            drawNanos = now;
        }
        double secs = (now - drawNanos) / 1e9;
        if (secs < 1.2 && ((int) (secs * 8)) % 2 == 0) {
            g.fill(0, 0, sw, sh, 0x18FF8040);
        }
        big(g, f, Component.translatable("casino.duel.go").withStyle(ChatFormatting.BOLD), cx, 44, 3.2F, 0xFFFF4020);
        g.drawCenteredString(f, Component.literal(String.format(Locale.ROOT, "%.2f s", secs)), cx, 70, 0xFFFFFFFF);
        // balas
        for (int i = 0; i < com.casino.game.DuelRules.SHOTS; i++) {
            int bx = cx - 33 + i * 11;
            boolean full = i < be.shots();
            g.fill(bx, sh - 70, bx + 6, sh - 58, full ? 0xFF8A6A2A : 0xFF3A2A1A);
            g.fill(bx + 1, sh - 72, bx + 5, sh - 70, full ? 0xFFD8B060 : 0xFF3A2A1A);
        }
    }
}
