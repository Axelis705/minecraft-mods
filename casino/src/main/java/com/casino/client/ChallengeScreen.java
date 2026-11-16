package com.casino.client;

import com.casino.chip.Chips;
import com.casino.entity.DealerEntity;
import com.casino.game.ArmRules;
import com.casino.game.DuelRules;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.west.ArmBE;
import com.casino.west.DuelBE;
import com.casino.west.MatchBE;
import com.casino.west.WestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Locale;

/**
 * Pantalla para retar en el duelo o en las vencidas: contra uno de los tres del pueblo (cada uno más difícil y mejor pagado) o contra
 * otro jugador (el reto queda abierto un minuto; quien lo acepte pone lo mismo y el ganador se lleva las dos apuestas menos el 5 %).
 * Si alguien ya retó, aquí se acepta; si el reto es mío, aquí se cancela.
 */
public final class ChallengeScreen extends CasinoScreen {
    private static final int W = 310;
    private static final int H = 270;
    private static int lastBet = 10;
    private static final int[] lastOpp = new int[4];

    private final BlockPos pos;
    private final int kind;
    private final int[] bets;
    private final int[] payX10;
    private final int[] skin;
    private final Button[] betButtons;
    private final Button[] oppButtons;
    private Button npcPlay;
    private Button pvp;
    private Button accept;
    private Button cancel;
    private int bet = lastBet;
    private int opp;
    private int left;
    private int top;

    public ChallengeScreen(BlockPos pos, int kind) {
        super(Component.translatable(kind == WestClient.DUEL ? "block.casino.duel_bell" : "block.casino.arm_table"));
        this.pos = pos;
        this.kind = kind;
        this.bets = kind == WestClient.DUEL ? DuelRules.BETS : ArmRules.BETS;
        this.payX10 = kind == WestClient.DUEL ? DuelRules.PAY_X10 : ArmRules.PAY_X10;
        this.skin = kind == WestClient.DUEL ? DuelRules.SKIN : ArmRules.SKIN;
        this.betButtons = new Button[bets.length];
        this.oppButtons = new Button[skin.length];
        this.opp = Math.min(skin.length - 1, lastOpp[kind]);
    }

    private String k(String s) {
        return (kind == WestClient.DUEL ? "casino.duel." : "casino.arm.") + s;
    }

    private MatchBE be() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof MatchBE m ? m : null;
    }

    private static Component npc(int variant) {
        return Component.translatable("casino.npc.duelist_" + variant);
    }

    private static String x(int x10) {
        return x10 % 10 == 0 ? "x" + x10 / 10 : "x" + x10 / 10 + "." + x10 % 10;
    }

    private boolean noon() {
        var level = Minecraft.getInstance().level;
        return kind == WestClient.DUEL && level != null && DuelRules.noon(com.casino.plat.Plat.dayTime(level));
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        int ow = (W - 24 - (skin.length - 1) * 4) / skin.length;
        for (int i = 0; i < skin.length; i++) {
            final int o = i;
            oppButtons[i] = addRenderableWidget(Button.builder(npc(DealerEntity.WEST + skin[i]), b -> {
                opp = o;
                lastOpp[kind] = o;
            }).bounds(left + 12 + i * (ow + 4), top + H - 106, ow, 20).build());
        }
        int bw = (W - 24 - (bets.length - 1) * 4) / bets.length;
        for (int i = 0; i < bets.length; i++) {
            final int b = bets[i];
            betButtons[i] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(b)), x -> {
                bet = b;
                lastBet = b;
            }).bounds(left + 12 + i * (bw + 4), top + H - 80, bw, 20).build());
        }
        npcPlay = addRenderableWidget(Button.builder(Component.literal(""), b -> {
            CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_NPC, pos, opp * 100000 + bet));
            onClose();
        }).bounds(left + 12, top + H - 54, W - 24, 20).build());
        accept = addRenderableWidget(Button.builder(Component.literal(""),
                b -> CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_ACCEPT, pos, 0))).bounds(left + 12, top + H - 54, W - 24, 20).build());
        cancel = addRenderableWidget(Button.builder(Component.translatable("casino.match.cancel").withStyle(ChatFormatting.RED),
                b -> CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_CANCEL, pos, 0))).bounds(left + 12, top + H - 54, W - 24, 20).build());
        pvp = addRenderableWidget(Button.builder(Component.literal(""),
                b -> CasinoNet.sendToServer(new WestPacket(WestPacket.MATCH_PVP, pos, bet))).bounds(left + 12, top + H - 30, W - 86, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 66, top + H - 30, 54, 20).build());
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        MatchBE m = be();
        if (p == null || m == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8 * 8) {
            onClose();
            return;
        }
        int bal = Chips.balance(p);
        boolean idle = m.phase() == MatchBE.IDLE;
        boolean waiting = m.phase() == MatchBE.WAITING;
        boolean mine = waiting && m.side(p.getUUID()) == 0;
        boolean any = false;
        for (int i = 0; i < bets.length; i++) {
            betButtons[i].visible = !waiting;
            betButtons[i].active = idle && bal >= bets[i];
            any |= bets[i] == bet;
        }
        if (!any || bal < bet) {
            bet = bets[0];
            for (int b : bets) {
                if (b <= bal) {
                    bet = b;
                }
            }
        }
        for (int i = 0; i < oppButtons.length; i++) {
            oppButtons[i].visible = !waiting;
            oppButtons[i].active = idle && i != opp;
        }
        npcPlay.visible = !waiting;
        npcPlay.active = idle && bal >= bet;
        npcPlay.setMessage(Component.translatable("casino.match.vs_npc", npc(DealerEntity.WEST + skin[opp]), bet).withStyle(ChatFormatting.BOLD));
        pvp.visible = !waiting;
        pvp.active = idle && bal >= bet;
        pvp.setMessage(Component.translatable("casino.match.vs_player", bet));
        accept.visible = waiting && !mine;
        accept.active = bal >= m.bet();
        accept.setMessage(Component.translatable("casino.match.accept", m.name(0), m.bet()).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        cancel.visible = mine;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF2A160A);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFC08A3E);
        g.fill(left, top, left + W, top + H, 0xFF3B2414);
        g.fill(left, top, left + W, top + 20, 0xFF6A3A1A);
        for (int xx = left + 6; xx < left + W - 6; xx += 9) {
            g.fill(xx, top + 21, xx + 4, top + 22, 0xFF8A5A2A);
        }
        Component title = Component.translatable(k("title")).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 6, 0xFFFFFF, true);
        var p = Minecraft.getInstance().player;
        MatchBE m = be();
        if (p == null || m == null) {
            drawWidgets(g, mx, my, pt);
            return;
        }
        int y = top + 28;
        for (FormattedCharSequence line : font.split(Component.translatable(k("rules")), W - 24)) {
            g.drawString(font, line, left + 12, y, 0xFFF0E2C8, false);
            y += 10;
        }
        y += 4;
        boolean noon = noon();
        if (kind == WestClient.DUEL) {
            Component nc = Component.translatable(noon ? "casino.duel.noon_now" : "casino.duel.noon_hint", "+0.5")
                    .withStyle(noon ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
            for (FormattedCharSequence line : font.split(nc, W - 24)) {
                g.drawString(font, line, left + 12, y, 0xFFFFFFFF, false);
                y += 10;
            }
        }
        // estado del reto
        int sy = top + H - 132;
        if (m.phase() == MatchBE.WAITING) {
            boolean mine = m.side(p.getUUID()) == 0;
            Component c = mine ? Component.translatable("casino.match.waiting", m.waitLeft() / 20).withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("casino.match.open", m.name(0), m.bet()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
            g.drawCenteredString(font, c, left + W / 2, top + H - 96, 0xFFFFFFFF);
            g.drawCenteredString(font, Component.translatable("casino.match.pvp_pays", x(kind == WestClient.DUEL ? DuelRules.PVP_PAY_X10 : ArmRules.PVP_PAY_X10))
                    .withStyle(ChatFormatting.GRAY), left + W / 2, top + H - 82, 0xFFFFFFFF);
        } else {
            Component c = m.phase() != MatchBE.IDLE ? Component.translatable("casino.match.busy_line", m.name(0), m.rivalName()).withStyle(ChatFormatting.YELLOW)
                    : Component.translatable("casino.match.pick").withStyle(ChatFormatting.GOLD);
            g.drawString(font, c, left + 12, sy, 0xFFFFFFFF, false);
            // premio de cada rival debajo de su nombre
            int ow = (W - 24 - (skin.length - 1) * 4) / skin.length;
            for (int i = 0; i < skin.length; i++) {
                int px10 = payX10[i] + (noon ? DuelRules.NOON_BONUS_X10 : 0);
                g.drawCenteredString(font, Component.literal(x(px10)), left + 12 + i * (ow + 4) + ow / 2, top + H - 117, i == skin.length - 1 ? 0xFFFFD040 : 0xFF8CF08C);
            }
        }
        // récord y saldo
        Component rec = null;
        if (m instanceof DuelBE d && d.bestMs() > 0) {
            rec = Component.translatable("casino.duel.record_line", d.bestName(), String.format(Locale.ROOT, "%.3f", d.bestMs() / 1000.0));
        } else if (m instanceof ArmBE a && a.bestTicks() > 0) {
            rec = Component.translatable("casino.arm.record_line", a.bestName(), String.format(Locale.ROOT, "%.1f", a.bestTicks() / 20.0));
        }
        int ry = top + H - 146;
        if (rec != null) {
            g.drawString(font, rec.copy().withStyle(ChatFormatting.AQUA), left + 12, ry, 0xFFFFFF, true);
        }
        Component bal = Component.translatable("casino.cashier.balance", Chips.balance(p)).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 12 - font.width(bal), ry, 0xFFFFFF, true);
        drawWidgets(g, mx, my, pt);
        if (m.phase() != MatchBE.WAITING) {
            Button ob = oppButtons[opp];
            g.fill(ob.getX() - 1, ob.getY() + ob.getHeight(), ob.getX() + ob.getWidth() + 1, ob.getY() + ob.getHeight() + 2, 0xFFFFD040);
            for (int i = 0; i < bets.length; i++) {
                if (bets[i] == bet) {
                    Button b = betButtons[i];
                    g.fill(b.getX() - 1, b.getY() + b.getHeight(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 2, 0xFFFFD040);
                }
            }
        }
    }
}
