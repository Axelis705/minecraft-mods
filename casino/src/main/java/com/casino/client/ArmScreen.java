package com.casino.client;

import com.casino.game.ArmRules;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.west.ArmBE;
import com.casino.west.MatchBE;
import com.casino.west.WestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Pantalla de las vencidas (transparente: se ve la mesa). Cuenta regresiva y luego hay que apretar A y D alternando lo más rápido que
 * se pueda (también sirven las flechas izquierda y derecha). Cada golpe alternado se manda al servidor; la barra muestra hacia dónde
 * se inclinan las manos. Esc dos veces = rendirse.
 */
public final class ArmScreen extends CasinoScreen {
    private final BlockPos pos;
    private boolean aimed;
    private int last = -1;
    private int pending;
    private int total;
    private long tapAt;
    private long escUntil;

    public ArmScreen(BlockPos pos) {
        super(Component.translatable("block.casino.arm_table"));
        this.pos = pos;
    }

    public BlockPos pos() {
        return pos;
    }

    private ArmBE be() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof ArmBE d ? d : null;
    }

    private int side(ArmBE d) {
        var p = Minecraft.getInstance().player;
        return d == null || p == null ? -1 : d.side(p.getUUID());
    }

    @Override
    protected void init() {
        ArmBE d = be();
        var p = Minecraft.getInstance().player;
        int s = side(d);
        if (!aimed && p != null && s >= 0) {
            // mirar las manos, sobre la mesa
            aimed = true;
            p.setYRot(d.yaw(s));
            p.setYHeadRot(d.yaw(s));
            p.setXRot(32);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        ArmBE d = be();
        int s = side(d);
        if (d == null || s < 0 || d.phase() == MatchBE.IDLE || d.phase() == MatchBE.WAITING) {
            onClose();
            return;
        }
        if (d.phase() != MatchBE.RUNNING || d.fightTime(0) < 0) {
            pending = 0;
            return;
        }
        if (pending > 0) {
            CasinoNet.sendToServer(new WestPacket(WestPacket.ARM_TAPS, pos, Math.min(ArmRules.MAX_TAPS_PER_TICK, pending)));
            pending = 0;
        }
    }

    /** 0 = tecla izquierda (A o flecha izquierda), 1 = derecha (D o flecha derecha), -1 = otra. */
    private static int hand(int key, int scan) {
        if (key == 263 || KeyChars.is(key, scan, 'A')) {
            return 0;
        }
        if (key == 262 || KeyChars.is(key, scan, 'D')) {
            return 1;
        }
        return -1;
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        ArmBE d = be();
        int s = side(d);
        long now = System.nanoTime();
        if (key == 256) {
            if (d == null || s < 0 || d.phase() != MatchBE.RUNNING || now < escUntil) {
                if (d != null && s >= 0 && d.phase() == MatchBE.RUNNING) {
                    CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_GIVE_UP, pos, 0));
                }
                onClose();
            } else {
                escUntil = now + 2_000_000_000L;
            }
            return true;
        }
        int h = hand(key, scan);
        if (h < 0 || d == null || d.phase() != MatchBE.RUNNING || d.fightTime(0) < 0) {
            return true;
        }
        // solo cuenta si alterna (dejar apretada una tecla no sirve)
        if (h != last) {
            last = h;
            pending++;
            total++;
            tapAt = now;
        }
        return true;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        ArmBE d = be();
        var p = Minecraft.getInstance().player;
        int s = side(d);
        if (d == null || p == null || s < 0) {
            return;
        }
        long now = System.nanoTime();
        int cx = width / 2;
        Component me = Component.literal(d.name(s)), rival = s == 0 ? d.rivalName() : Component.literal(d.name(0));
        g.fill(0, 0, width, 30, 0x90180C04);
        g.fill(0, 30, width, 31, 0xC0C08A3E);
        g.drawCenteredString(font, Component.translatable("casino.match.vs", me, rival).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), cx, 5, 0xFFFFFFFF);
        g.drawCenteredString(font, Component.translatable("casino.match.bet", d.bet()).withStyle(ChatFormatting.YELLOW), cx, 17, 0xFFFFFFFF);

        // a favor mío: +1 = gané, -1 = perdí
        double v = s == 0 ? d.x() : -d.x();
        if (d.phase() == MatchBE.RESULT) {
            arms(g, cx, height / 2 + 6, v);
            result(g, d, s, rival, cx);
            return;
        }
        double ft = d.fightTime(pt);
        int by = height / 2 + 34;
        arms(g, cx, by - 10, v);
        bar(g, cx, by, v, rival);
        if (ft < 0) {
            int n = (int) Math.ceil(-ft / 20.0);
            WestHud.big(g, font, Component.literal(String.valueOf(Math.max(1, n))).withStyle(ChatFormatting.BOLD), cx, 64, 4F, 0xFFE8D8B0);
            box(g, cx, 90, Component.translatable("casino.arm.scr_get_ready").withStyle(ChatFormatting.WHITE));
        } else {
            if (ft < 20) {
                WestHud.big(g, font, Component.translatable("casino.arm.scr_go").withStyle(ChatFormatting.BOLD), cx, 64, 4F, 0xFFFF5020);
            } else {
                int left = (int) Math.ceil((ArmRules.MAX_TICKS - ft) / 20.0);
                g.drawCenteredString(font, Component.translatable("casino.arm.scr_left", Math.max(0, left)).withStyle(ChatFormatting.GRAY), cx, 44, 0xFFFFFFFF);
            }
            // ritmos
            Component mine = Component.translatable("casino.arm.scr_rate_me", String.format(Locale.ROOT, "%.1f", d.rate(s)));
            Component theirs = Component.translatable("casino.arm.scr_rate_rival", rival, String.format(Locale.ROOT, "%.1f", d.rate(1 - s)));
            g.drawString(font, mine, cx - 130, by + 20, d.rate(s) >= d.rate(1 - s) ? 0xFF80F080 : 0xFFF0C080, true);
            g.drawString(font, theirs, cx + 130 - font.width(theirs), by + 20, 0xFFE0D0B0, true);
        }
        // las dos teclas: se ilumina la que toca
        int ky = height - 60;
        boolean fight = ft >= 0;
        for (int i = 0; i < 2; i++) {
            int kx = cx + (i == 0 ? -34 : 6);
            boolean next = fight && i != last;
            boolean hit = i == last && now - tapAt < 90_000_000L;
            int bg = hit ? 0xFFFFE070 : next ? ((now / 120_000_000L) % 2 == 0 ? 0xFFF4ECD8 : 0xFFD8C8A8) : 0xFF6A5A4A;
            g.fill(kx - 1, ky - 1, kx + 29, ky + 29, 0xFF1A0E06);
            g.fill(kx, ky, kx + 28, ky + 28, bg);
            g.fill(kx, ky + 24, kx + 28, ky + 28, 0x40000000);
            WestHud.big(g, font, Component.literal(i == 0 ? "A" : "D").withStyle(ChatFormatting.BOLD), kx + 14, ky + 13, 2F, 0xFF2A1A0A);
        }
        g.drawCenteredString(font, Component.translatable("casino.arm.scr_hint").withStyle(ChatFormatting.WHITE), cx, ky + 34, 0xFFFFFFFF);
        Component esc = now < escUntil ? Component.translatable("casino.match.esc_again").withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                : Component.translatable("casino.match.esc").withStyle(ChatFormatting.DARK_GRAY);
        g.drawCenteredString(font, esc, cx, height - 14, 0xFFFFFFFF);
    }

    /** Barra de fuerza: verde a la derecha si vas ganando, roja a la izquierda si vas perdiendo. */
    private void bar(G g, int cx, int y, double v, Component rival) {
        int half = 130;
        g.fill(cx - half - 2, y - 2, cx + half + 2, y + 12, 0xFF1A0E06);
        g.fill(cx - half, y, cx + half, y + 10, 0xFF3A2A1A);
        int m = (int) Math.round(v * half);
        if (m > 0) {
            g.fill(cx, y, cx + m, y + 10, 0xFF3AC03A);
        } else if (m < 0) {
            g.fill(cx + m, y, cx, y + 10, 0xFFD03A2A);
        }
        g.fill(cx - 1, y - 3, cx + 1, y + 13, 0xFFE8D8B0);
        g.fill(cx + m - 2, y - 4, cx + m + 2, y + 14, 0xFFFFFFFF);
        g.drawString(font, Component.translatable("casino.arm.scr_side_rival", rival).withStyle(ChatFormatting.RED), cx - half, y - 13, 0xFFFFFFFF, true);
        Component meWin = Component.translatable("casino.arm.scr_side_me").withStyle(ChatFormatting.GREEN);
        g.drawString(font, meWin, cx + half - font.width(meWin), y - 13, 0xFFFFFFFF, true);
    }

    /** Dos antebrazos agarrados que se inclinan (vista de frente). */
    private void arms(G g, int cx, int y, double v) {
        float a = (float) Math.toRadians(ArmRules.angle(v));
        g.fill(cx - 34, y, cx + 34, y + 4, 0xFF6A3A1A);   // la mesa
        g.push();
        g.translate(cx, y, 0);
        g.rotate(a);
        g.fill(-6, -46, 6, 0, 0xFFC68A5E);    // antebrazo
        g.fill(-8, -58, 8, -44, 0xFFD8A070);  // manos agarradas
        g.fill(-8, -50, 8, -48, 0xFFB07850);
        g.pop();
    }

    private void box(G g, int cx, int y, Component c) {
        int w = font.width(c) + 12;
        g.fill(cx - w / 2, y - 3, cx + w / 2, y + 11, 0xA0180C04);
        g.drawCenteredString(font, c, cx, y, 0xFFFFFFFF);
    }

    private void result(G g, ArmBE d, int s, Component rival, int cx) {
        int w = d.winner();
        boolean tie = w == MatchBE.TIE, won = w == s;
        Component head = tie ? Component.translatable("casino.match.tie").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                : won ? Component.translatable("casino.match.win").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.translatable("casino.match.lose").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        WestHud.big(g, font, head, cx, 56, 3F, 0xFFFFFFFF);
        box(g, cx, height / 2 + 20, DuelScreen.why(d, s, rival, "casino.arm."));
        Component money = tie ? Component.translatable("casino.match.refund").withStyle(ChatFormatting.YELLOW)
                : won ? Component.translatable("casino.match.paid", d.paid()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                : Component.translatable("casino.match.lost", d.bet()).withStyle(ChatFormatting.RED);
        box(g, cx, height / 2 + 38, money);
        if (total > 0) {
            g.drawCenteredString(font, Component.translatable("casino.arm.scr_taps", total).withStyle(ChatFormatting.GRAY), cx, height / 2 + 56, 0xFFFFFFFF);
        }
        g.drawCenteredString(font, Component.translatable("casino.match.close").withStyle(ChatFormatting.DARK_GRAY), cx, height - 14, 0xFFFFFFFF);
    }
}
