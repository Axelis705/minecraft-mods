package com.casino.client;

import com.casino.chip.Chips;
import com.casino.game.BullRide;
import com.casino.game.DuelRules;
import com.casino.game.GalleryPlan;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.west.BullBE;
import com.casino.west.GalleryBE;
import com.casino.west.WestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Locale;

/** Pantalla de apuesta del toro mecánico y de la galería de tiro: reglas, tabla de premios, récord, apuesta y "¡a jugar!". */
public final class WestBetScreen extends CasinoScreen {
    private static final int W = 300;
    private final int H;
    private static int lastBet = 10;
    private static int lastOpp = 0;
    private final Button[] oppButtons = new Button[DuelRules.OPPONENTS];
    private int opp = lastOpp;

    private final BlockPos pos;
    private final int kind;
    private final int[] bets;
    private final Button[] betButtons;
    private Button play;
    private int bet;
    private int left;
    private int top;

    public WestBetScreen(BlockPos pos, int kind) {
        super(Component.translatable(kind == WestClient.BULL ? "block.casino.mechanical_bull" : kind == WestClient.DUEL ? "block.casino.duel_bell"
                : "block.casino.shooting_gallery"));
        this.pos = pos;
        this.kind = kind;
        this.H = kind == WestClient.DUEL ? 248 : 214;
        this.bets = kind == WestClient.BULL ? BullRide.BETS : kind == WestClient.DUEL ? DuelRules.BETS : GalleryPlan.BETS;
        this.betButtons = new Button[bets.length];
        this.bet = lastBet;
    }

    private String key(String k) {
        return (kind == WestClient.BULL ? "casino.bull." : kind == WestClient.DUEL ? "casino.duel." : "casino.gallery.") + k;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        int bw = (W - 24 - (bets.length - 1) * 4) / bets.length;
        for (int i = 0; i < bets.length; i++) {
            final int b = bets[i];
            betButtons[i] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(b)), x -> {
                bet = b;
                lastBet = b;
            }).bounds(left + 12 + i * (bw + 4), top + H - 56, bw, 20).build());
        }
        if (kind == WestClient.DUEL) {
            int ow = (W - 24 - 2 * 4) / 3;
            for (int i = 0; i < DuelRules.OPPONENTS; i++) {
                final int o = i;
                oppButtons[i] = addRenderableWidget(Button.builder(Component.translatable("casino.npc.duelist_" + (com.casino.entity.DealerEntity.WEST + DuelRules.SKIN[i])),
                        x -> {
                            opp = o;
                            lastOpp = o;
                        }).bounds(left + 12 + i * (ow + 4), top + H - 82, ow, 20).build());
            }
        }
        play = addRenderableWidget(Button.builder(Component.translatable(key("play"), bet), b -> go())
                .bounds(left + 12, top + H - 30, W - 86, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 66, top + H - 30, 54, 20).build());
    }

    private void go() {
        if (kind == WestClient.DUEL) {
            CasinoNet.sendToServer(new WestPacket(WestPacket.DUEL_BET, pos, opp * 100000 + bet));
        } else {
            CasinoNet.sendToServer(new WestPacket(kind == WestClient.BULL ? WestPacket.BULL_BET : WestPacket.GALLERY_BET, pos, bet));
        }
        onClose();
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 7 * 7) {
            onClose();
            return;
        }
        int bal = Chips.balance(p);
        boolean any = false;
        for (int i = 0; i < bets.length; i++) {
            betButtons[i].active = bal >= bets[i];
            any |= bets[i] == bet;
        }
        if (!any || bal < bet) {
            // la apuesta más alta que alcanza
            bet = bets[0];
            for (int b : bets) {
                if (b <= bal) {
                    bet = b;
                }
            }
        }
        play.active = bal >= bet;
        play.setMessage(Component.translatable(key("play"), bet));
        if (kind == WestClient.DUEL) {
            for (int i = 0; i < DuelRules.OPPONENTS; i++) {
                oppButtons[i].active = i != opp;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static net.minecraft.world.level.Level level0() {
        return Minecraft.getInstance().level;
    }

    private static String x(int x10) {
        return x10 % 10 == 0 ? "x" + x10 / 10 : "x" + x10 / 10 + "." + x10 % 10;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        // marco de madera
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF2A160A);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFC08A3E);
        g.fill(left, top, left + W, top + H, 0xFF3B2414);
        g.fill(left, top, left + W, top + 20, 0xFF6A3A1A);
        for (int x = left + 6; x < left + W - 6; x += 9) {
            g.fill(x, top + 21, x + 4, top + 22, 0xFF8A5A2A);
        }
        Component title = Component.translatable(key("title")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 6, 0xFFFFFF, true);
        var p = Minecraft.getInstance().player;
        Component bal = Component.translatable("casino.cashier.balance", p == null ? 0 : Chips.balance(p)).withStyle(ChatFormatting.YELLOW);
        // reglas
        int y = top + 28;
        List<FormattedCharSequence> rules = font.split(Component.translatable(key("rules")), W - 24);
        for (FormattedCharSequence line : rules) {
            g.drawString(font, line, left + 12, y, 0xFFF0E2C8, false);
            y += 10;
        }
        y += 4;
        // tabla de premios
        g.drawString(font, Component.translatable(key("prizes")).withStyle(ChatFormatting.GOLD), left + 12, y, 0xFFFFFF, false);
        y += 11;
        int n = kind == WestClient.BULL ? BullRide.TIER_TICKS.length : kind == WestClient.DUEL ? DuelRules.OPPONENTS : GalleryPlan.SCORE_AT.length;
        int cw = (W - 24) / n;
        for (int i = 0; i < n; i++) {
            int cx = left + 12 + i * cw;
            String need = kind == WestClient.BULL ? BullRide.TIER_TICKS[i] / 20 + " s" : kind == WestClient.DUEL
                    ? font.plainSubstrByWidth(Component.translatable("casino.npc.duelist_" + (com.casino.entity.DealerEntity.WEST + DuelRules.SKIN[i])).getString(), cw - 6)
                    : GalleryPlan.SCORE_AT[i] + " pts";
            String pay = x(kind == WestClient.BULL ? BullRide.TIER_X10[i] : kind == WestClient.DUEL ? DuelRules.PAY_X10[i] : GalleryPlan.SCORE_X10[i]);
            g.fill(cx, y, cx + cw - 3, y + 22, 0xFF24150A);
            g.drawCenteredString(font, need, cx + (cw - 3) / 2, y + 2, 0xFFE8D8B8);
            g.drawCenteredString(font, pay, cx + (cw - 3) / 2, y + 12, i == n - 1 ? 0xFFFFD040 : 0xFF8CF08C);
        }
        y += 28;
        if (kind == WestClient.DUEL) {
            boolean noon = level0() != null && DuelRules.noon(com.casino.plat.Plat.dayTime(level0()));
            Component nc = Component.translatable(noon ? "casino.duel.noon_now" : "casino.duel.noon_hint", "+0.5")
                    .withStyle(noon ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            for (FormattedCharSequence line : font.split(nc, W - 24)) {
                g.drawString(font, line, left + 12, y, 0xFFFFFFFF, false);
                y += 10;
            }
        }
        if (kind == WestClient.GALLERY) {
            String pts = Component.translatable("casino.gallery.points_legend", GalleryPlan.POINTS[0], GalleryPlan.POINTS[1], GalleryPlan.POINTS[2],
                    GalleryPlan.POINTS[4], GalleryPlan.POINTS[3]).getString();
            for (FormattedCharSequence line : font.split(Component.literal(pts), W - 24)) {
                g.drawString(font, line, left + 12, y, 0xFFD8C8A8, false);
                y += 10;
            }
        }
        // récord
        var level = Minecraft.getInstance().level;
        Component rec = null;
        if (level != null && level.getBlockEntity(pos) instanceof BullBE b && b.bestTicks() > 0) {
            rec = Component.translatable("casino.bull.record_line", b.bestName(), String.format(Locale.ROOT, "%.1f", b.bestTicks() / 20.0));
        } else if (level != null && level.getBlockEntity(pos) instanceof com.casino.west.DuelBE db && db.bestTicks() > 0) {
            rec = Component.translatable("casino.duel.record_line", db.bestName(), String.format(Locale.ROOT, "%.2f", db.bestTicks() / 20.0));
        } else if (level != null && level.getBlockEntity(pos) instanceof GalleryBE gb && gb.bestScore() > 0) {
            rec = Component.translatable("casino.gallery.record_line", gb.bestName(), gb.bestScore());
        }
        int ry = top + H - (kind == WestClient.DUEL ? 96 : 70);
        if (rec != null) {
            g.drawString(font, rec.copy().withStyle(ChatFormatting.AQUA), left + 12, ry, 0xFFFFFF, true);
        }
        g.drawString(font, bal, left + W - 12 - font.width(bal), ry, 0xFFFFFF, true);
        drawWidgets(g, mx, my, pt);
        if (kind == WestClient.DUEL) {
            Button b = oppButtons[opp];
            g.fill(b.getX() - 1, b.getY() + b.getHeight(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 2, 0xFFFFD040);
        }
        // la apuesta elegida, marcada
        for (int i = 0; i < bets.length; i++) {
            if (bets[i] == bet) {
                Button b = betButtons[i];
                g.fill(b.getX() - 1, b.getY() + b.getHeight(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 2, 0xFFFFD040);
            }
        }
    }
}
