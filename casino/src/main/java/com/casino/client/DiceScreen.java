package com.casino.client;

import com.casino.chip.Chips;
import com.casino.game.LiarsDice;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.west.DiceBE;
import com.casino.west.WestPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Pantalla de los dados del mentiroso: antes de jugar, reglas y apuesta; durante la partida, tus dados, el cubilete del tahúr, las
 * apuestas hechas y los botones para subir (cantidad y cara) o gritar "¡Mentiroso!"; al final, los dados destapados.
 */
public final class DiceScreen extends CasinoScreen {
    private static final int W = 320;
    private static final int H = 236;
    private static int lastBet = 10;

    private final BlockPos pos;
    private int left;
    private int top;
    private int bet = lastBet;
    private int qty = 1;
    private int face = 2;
    private int seenBids = -1;
    private final Button[] betButtons = new Button[LiarsDice.BETS.length];
    private final Button[] faceButtons = new Button[5];
    private Button play;
    private Button minus;
    private Button plus;
    private Button raise;
    private Button liar;

    public DiceScreen(BlockPos pos) {
        super(Component.translatable("block.casino.liars_dice"));
        this.pos = pos;
    }

    private DiceBE be() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof DiceBE d ? d : null;
    }

    private boolean mine(DiceBE d) {
        var p = Minecraft.getInstance().player;
        return d != null && p != null && d.isPlayer(p.getUUID());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        int bw = (W - 24 - 5 * 4) / 6;
        for (int i = 0; i < LiarsDice.BETS.length; i++) {
            final int b = LiarsDice.BETS[i];
            betButtons[i] = addRenderableWidget(Button.builder(Component.literal(String.valueOf(b)), x -> {
                bet = b;
                lastBet = b;
            }).bounds(left + 12 + i * (bw + 4), top + H - 56, bw, 20).build());
        }
        play = addRenderableWidget(Button.builder(Component.translatable("casino.dice.play", bet), b -> {
            CasinoNet.sendToServer(new WestPacket(WestPacket.DICE_BET, pos, bet));
        }).bounds(left + 12, top + H - 30, W - 86, 20).build());
        // controles de la partida
        int cy = top + H - 56;
        minus = addRenderableWidget(Button.builder(Component.literal("-"), b -> qty = Math.max(1, qty - 1)).bounds(left + 12, cy, 20, 20).build());
        plus = addRenderableWidget(Button.builder(Component.literal("+"), b -> qty = Math.min(LiarsDice.TOTAL, qty + 1)).bounds(left + 58, cy, 20, 20).build());
        for (int f = 2; f <= 6; f++) {
            final int ff = f;
            faceButtons[f - 2] = addRenderableWidget(Button.builder(Component.literal(""), b -> face = ff)
                    .bounds(left + 88 + (f - 2) * 26, cy, 22, 20).build());
        }
        raise = addRenderableWidget(Button.builder(Component.literal(""), b -> {
            CasinoNet.sendToServer(new WestPacket(WestPacket.DICE_BID, pos, qty * 10 + face));
        }).bounds(left + 12, top + H - 30, 150, 20).build());
        liar = addRenderableWidget(Button.builder(Component.translatable("casino.dice.liar").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                b -> CasinoNet.sendToServer(new WestPacket(WestPacket.DICE_CALL, pos, 0))).bounds(left + 166, top + H - 30, 84, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 66, top + H - 30, 54, 20).build());
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        DiceBE d = be();
        if (p == null || d == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 8 * 8) {
            onClose();
            return;
        }
        boolean playing = mine(d) && d.phase() != DiceBE.IDLE;
        boolean idle = d.phase() == DiceBE.IDLE;
        int bal = Chips.balance(p);
        for (int i = 0; i < betButtons.length; i++) {
            betButtons[i].visible = !playing;
            betButtons[i].active = idle && bal >= LiarsDice.BETS[i];
        }
        play.visible = !playing;
        play.active = idle && bal >= bet;
        play.setMessage(Component.translatable("casino.dice.play", bet));
        boolean myTurn = playing && d.phase() == DiceBE.PLAY && d.turn() == LiarsDice.PLAYER;
        LiarsDice g = d.game();
        // al cambiar las apuestas, proponer la subida más chica
        if (d.bids().length != seenBids) {
            seenBids = d.bids().length;
            LiarsDice.Bid b = g.last();
            if (b == null) {
                qty = 1;
                face = 2;
            } else if (b.face() < 6) {
                qty = b.qty();
                face = b.face() + 1;
            } else {
                qty = b.qty() + 1;
                face = 2;
            }
        }
        minus.visible = plus.visible = raise.visible = liar.visible = playing;
        for (Button fb : faceButtons) {
            fb.visible = playing;
            fb.active = myTurn;
        }
        minus.active = plus.active = myTurn;
        raise.active = myTurn && g.legal(qty, face);
        raise.setMessage(Component.translatable("casino.dice.raise", qty, Component.translatable("casino.dice.face_" + face)));
        liar.active = myTurn && g.last() != null;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Un dado con sus puntos. */
    static void die(G g, int x, int y, int s, int value, int bg, int pip) {
        g.fill(x, y, x + s, y + s, 0xFF2A1A0A);
        g.fill(x + 1, y + 1, x + s - 1, y + s - 1, bg);
        if (value < 1 || value > 6) {
            g.drawCenteredString(Minecraft.getInstance().font, "?", x + s / 2, y + s / 2 - 4, 0xFF5A4A3A);
            return;
        }
        int[][] spots = {{}, {4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}};
        int ps = Math.max(2, s / 6);
        for (int k : spots[value]) {
            int cx = x + s / 2 + (k % 3 - 1) * (s / 3 - 1), cy = y + s / 2 + (k / 3 - 1) * (s / 3 - 1);
            g.fill(cx - ps / 2, cy - ps / 2, cx - ps / 2 + ps, cy - ps / 2 + ps, pip);
        }
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF2A160A);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFC08A3E);
        g.fill(left, top, left + W, top + H, 0xFF1F4A2A);
        g.fill(left, top, left + W, top + 20, 0xFF6A3A1A);
        Component title = Component.translatable("casino.dice.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 6, 0xFFFFFF, true);
        DiceBE d = be();
        var p = Minecraft.getInstance().player;
        if (d == null || p == null) {
            drawWidgets(g, mx, my, pt);
            return;
        }
        Component npc = Component.translatable("casino.npc.duelist_" + (com.casino.entity.DealerEntity.WEST + 3));
        boolean playing = mine(d) && d.phase() != DiceBE.IDLE;
        if (!playing) {
            int y = top + 28;
            for (FormattedCharSequence line : font.split(Component.translatable("casino.dice.rules"), W - 24)) {
                g.drawString(font, line, left + 12, y, 0xFFF0E2C8, false);
                y += 10;
            }
            y += 4;
            g.drawString(font, Component.translatable("casino.dice.pays", "x1.9").withStyle(ChatFormatting.GOLD), left + 12, y, 0xFFFFFFFF, false);
            if (d.phase() != DiceBE.IDLE) {
                g.drawString(font, Component.translatable("casino.dice.busy", d.playerName()).withStyle(ChatFormatting.YELLOW), left + 12, y + 14,
                        0xFFFFFFFF, false);
            }
            int ry = top + H - 72;
            if (d.bestStreak() > 0) {
                g.drawString(font, Component.translatable("casino.dice.record_line", d.bestName(), d.bestStreak()).withStyle(ChatFormatting.AQUA),
                        left + 12, ry, 0xFFFFFFFF, true);
            }
            Component bal = Component.translatable("casino.cashier.balance", Chips.balance(p)).withStyle(ChatFormatting.YELLOW);
            g.drawString(font, bal, left + W - 12 - font.width(bal), ry, 0xFFFFFFFF, true);
            drawWidgets(g, mx, my, pt);
            for (int i = 0; i < LiarsDice.BETS.length; i++) {
                if (LiarsDice.BETS[i] == bet) {
                    Button b = betButtons[i];
                    g.fill(b.getX() - 1, b.getY() + b.getHeight(), b.getX() + b.getWidth() + 1, b.getY() + b.getHeight() + 2, 0xFFFFD040);
                }
            }
            return;
        }
        boolean reveal = d.phase() == DiceBE.REVEAL;
        LiarsDice g0 = d.game();
        LiarsDice.Bid last = g0.last();
        int hiFace = reveal && last != null ? last.face() : -1;
        // tus dados
        g.drawString(font, Component.translatable("casino.dice.yours").withStyle(ChatFormatting.WHITE), left + 12, top + 28, 0xFFFFFFFF, false);
        int[] pd = d.playerDice();
        for (int i = 0; i < pd.length; i++) {
            boolean hit = hiFace > 0 && (pd[i] == hiFace || pd[i] == 1);
            die(g, left + 12 + i * 26, top + 40, 22, pd[i], hit ? 0xFFFFE070 : 0xFFF4EEE0, 0xFF1A1010);
        }
        // los del tahúr
        Component nn = npc.copy().withStyle(ChatFormatting.WHITE);
        g.drawString(font, nn, left + W - 12 - font.width(nn), top + 28, 0xFFFFFFFF, false);
        int[] nd = d.npcDice();
        for (int i = 0; i < LiarsDice.DICE; i++) {
            int x = left + W - 12 - (LiarsDice.DICE - i) * 26 + 4;
            if (reveal) {
                boolean hit = hiFace > 0 && (nd[i] == hiFace || nd[i] == 1);
                die(g, x, top + 40, 22, nd[i], hit ? 0xFFFFE070 : 0xFFF4EEE0, 0xFF1A1010);
            } else {
                die(g, x, top + 40, 22, 0, 0xFF8A5A30, 0xFF000000);   // cubilete tapado
            }
        }
        // historial de apuestas
        int hy = top + 70;
        g.fill(left + 12, hy - 2, left + W - 12, hy + 76, 0x60000000);
        int[] bids = d.bids();
        int from = Math.max(0, bids.length - 7);
        for (int i = from; i < bids.length; i++) {
            int c = bids[i];
            boolean me = c / 100 == LiarsDice.PLAYER;
            Component who = me ? Component.translatable("casino.dice.you") : npc;
            Component line = Component.translatable("casino.dice.bid_line", who, c % 100 / 10, Component.translatable("casino.dice.face_" + c % 10));
            g.drawString(font, line, left + 18, hy + (i - from) * 10 + 2, me ? 0xFFB0E0FF : 0xFFFFD0A0, false);
        }
        if (bids.length == 0) {
            g.drawString(font, Component.translatable("casino.dice.no_bids").withStyle(ChatFormatting.GRAY), left + 18, hy + 2, 0xFFFFFFFF, false);
        }
        // estado
        int sy = top + 152;
        Component state;
        if (reveal) {
            boolean won = d.winner() == LiarsDice.PLAYER;
            state = last == null ? Component.translatable("casino.dice.left_short")
                    : Component.translatable("casino.dice.reveal", g0.count(last.face()), Component.translatable("casino.dice.face_" + last.face()), last.qty());
            g.drawCenteredString(font, state, left + W / 2, sy, 0xFFFFFFFF);
            Component res = won ? Component.translatable("casino.dice.won", LiarsDice.payout(d.bet())).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("casino.dice.lost", d.bet(), npc).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            g.drawCenteredString(font, res, left + W / 2, sy + 12, 0xFFFFFFFF);
        } else {
            state = d.turn() == LiarsDice.PLAYER ? Component.translatable("casino.dice.your_turn").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("casino.dice.thinking", npc).withStyle(ChatFormatting.YELLOW);
            g.drawCenteredString(font, state, left + W / 2, sy, 0xFFFFFFFF);
            if (last != null) {
                g.drawCenteredString(font, Component.translatable("casino.dice.current", last.qty(), Component.translatable("casino.dice.face_" + last.face())),
                        left + W / 2, sy + 12, 0xFFE0D0B0);
            }
        }
        drawWidgets(g, mx, my, pt);
        if (!reveal) {
            // cantidad entre los botones - y +, y los dados de las caras
            g.drawCenteredString(font, String.valueOf(qty), left + 45, top + H - 50, 0xFFFFFFFF);
            for (int f = 2; f <= 6; f++) {
                Button b = faceButtons[f - 2];
                die(g, b.getX() + 3, b.getY() + 2, 16, f, f == face ? 0xFFFFE070 : 0xFFF4EEE0, 0xFF1A1010);
            }
        }
    }
}
