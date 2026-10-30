package com.casino.client;

import com.casino.Casino;
import com.casino.block.BlackjackBE;
import com.casino.chip.Chips;
import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackGame.Phase;
import com.casino.net.BlackjackPacket;
import com.casino.net.CasinoNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Panel de la mesa de blackjack. Va pegado abajo y sin oscurecer, para ver al crupier y la mesa mientras se juega.
 * Muestra las dos manos (con las cartas del atlas) y los botones según el momento: apostar y repartir, o pedir / plantarse / doblar.
 */
public final class BlackjackScreen extends Screen {
    private static final ResourceLocation CARDS = new ResourceLocation(Casino.MODID, "textures/entity/cards.png");
    private static final int W = 360;
    private static final int H = 148;
    private static final int CW = 24;
    private static final int CH = 36;
    private static int lastBet = 1;

    private final BlockPos pos;
    private int left;
    private int top;
    private int betIdx = lastBet;
    private int cooldown = 0;
    private Button betMinus;
    private Button betPlus;
    private Button dealBtn;
    private Button hitBtn;
    private Button standBtn;
    private Button doubleBtn;

    public BlackjackScreen(BlockPos pos) {
        super(Component.translatable("block.casino.blackjack"));
        this.pos = pos;
    }

    private BlackjackBE table() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof BlackjackBE be ? be : null;
    }

    private int bet() {
        return BlackjackGame.BETS[betIdx];
    }

    private boolean mine(BlackjackBE be) {
        var p = Minecraft.getInstance().player;
        return p != null && p.getUUID().equals(be.player);
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 30;
        int x0 = left + 10;
        betMinus = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBet(-1)).bounds(x0 + 160, top + 120, 20, 22).build());
        betPlus = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBet(1)).bounds(x0 + 232, top + 120, 20, 22).build());
        dealBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.deal").withStyle(ChatFormatting.BOLD), b -> send(BlackjackPacket.DEAL))
                .bounds(x0, top + 120, 150, 22).build());
        hitBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.hit"), b -> send(BlackjackPacket.HIT)).bounds(x0, top + 120, 100, 22).build());
        standBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.stand"), b -> send(BlackjackPacket.STAND)).bounds(x0 + 104, top + 120, 100, 22).build());
        doubleBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.double"), b -> send(BlackjackPacket.DOUBLE)).bounds(x0 + 208, top + 120, 100, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(x0 + 312, top + 120, 38, 22).build());
        updateButtons();
    }

    private void changeBet(int d) {
        betIdx = Math.max(0, Math.min(BlackjackGame.BETS.length - 1, betIdx + d));
        lastBet = betIdx;
        updateButtons();
    }

    private void send(int action) {
        if (cooldown > 0) {
            return;
        }
        CasinoNet.sendToServer(new BlackjackPacket(pos, action, bet()));
        cooldown = 4;
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private enum Mode { START, PLAY, WAIT, BUSY }

    private Mode mode(BlackjackBE be) {
        if (be.game.phase == Phase.IDLE || be.game.phase == Phase.DONE) {
            return Mode.START;
        }
        if (!mine(be)) {
            return Mode.BUSY;
        }
        return be.game.phase == Phase.PLAYER ? Mode.PLAY : Mode.WAIT;
    }

    private void updateButtons() {
        BlackjackBE be = table();
        Mode m = be == null ? Mode.WAIT : mode(be);
        betMinus.visible = m == Mode.START;
        betPlus.visible = m == Mode.START;
        dealBtn.visible = m == Mode.START;
        hitBtn.visible = m == Mode.PLAY;
        standBtn.visible = m == Mode.PLAY;
        doubleBtn.visible = m == Mode.PLAY;
        betMinus.active = betIdx > 0;
        betPlus.active = betIdx < BlackjackGame.BETS.length - 1;
        dealBtn.active = balance() >= bet();
        doubleBtn.active = be != null && be.game.canDouble() && balance() >= be.game.bet;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || table() == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > 12 * 12) {
            onClose();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        updateButtons();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        BlackjackBE be = table();
        if (be != null) {
            Mode m = mode(be);
            if (m == Mode.PLAY) {
                if (key == 72) { // H
                    send(BlackjackPacket.HIT);
                    return true;
                }
                if (key == 83) { // S
                    send(BlackjackPacket.STAND);
                    return true;
                }
                if (key == 68 && doubleBtn.active) { // D
                    send(BlackjackPacket.DOUBLE);
                    return true;
                }
            } else if (m == Mode.START && (key == 32 || key == 257 || key == 335) && dealBtn.active) {
                send(BlackjackPacket.DEAL);
                return true;
            }
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void frame(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private void drawCard(GuiGraphics g, int x, int y, int card) {
        int col = card < 0 ? 0 : BlackjackGame.rank(card);
        int row = card < 0 ? 4 : BlackjackGame.suit(card);
        g.blit(CARDS, x, y, CW, CH, col * 32, row * 48, 32, 48, 416, 240);
    }

    private void drawHand(GuiGraphics g, int x, int y, List<Integer> hand, boolean hideSecond) {
        for (int i = 0; i < hand.size(); i++) {
            drawCard(g, x + i * (CW - 6), y, hideSecond && i == 1 ? -1 : hand.get(i));
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        BlackjackBE be = table();
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xE0102A18);
        g.fill(left, top, left + W, top + 16, 0xFF3A2410);
        g.drawString(font, Component.translatable("casino.blackjack.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 4, 0xFFFFFF, true);
        Component bal = Component.translatable("casino.cashier.balance", balance()).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 4, 0xFFFFFF, true);
        if (be == null) {
            super.render(g, mx, my, pt);
            return;
        }
        BlackjackGame game = be.game;
        Mode m = mode(be);
        boolean showPlayer = (m != Mode.START || game.phase == Phase.DONE) && !game.player.isEmpty();

        // manos: crupier arriba, jugador abajo
        int hx = left + 10;
        g.drawString(font, Component.translatable("casino.blackjack.dealer").withStyle(ChatFormatting.GRAY), hx, top + 20, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.blackjack.you").withStyle(ChatFormatting.GRAY), hx, top + 64, 0xFFFFFF, false);
        if (showPlayer || m == Mode.BUSY) {
            boolean hide = !game.holeRevealed;
            drawHand(g, hx, top + 30, game.dealer, hide);
            drawHand(g, hx, top + 74, game.player, false);
            String dt = game.holeRevealed ? String.valueOf(BlackjackGame.total(game.dealer)) : game.dealerShownTotal() + " + ?";
            g.drawString(font, Component.literal(dt).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), hx + 30 + (Math.max(game.dealer.size(), 2) - 1) * (CW - 6) + CW, top + 44, 0xFFFFFF, true);
            String pt2 = String.valueOf(game.playerTotal());
            if (BlackjackGame.soft(game.player) && game.playerTotal() > 11 && game.playerTotal() < 21) {
                pt2 = (game.playerTotal() - 10) + " / " + game.playerTotal();
            }
            g.drawString(font, Component.literal(pt2).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), hx + 30 + (game.player.size() - 1) * (CW - 6) + CW, top + 88, 0xFFFFFF, true);
        } else {
            g.drawString(font, Component.translatable("casino.blackjack.rules").withStyle(ChatFormatting.GREEN), hx, top + 36, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("casino.blackjack.rules2").withStyle(ChatFormatting.GREEN), hx, top + 48, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("casino.blackjack.rules3").withStyle(ChatFormatting.GREEN), hx, top + 60, 0xFFFFFF, false);
        }

        // columna de la derecha: estado
        int rx = left + 230;
        int ry = top + 22;
        if (game.phase == Phase.DONE && mine(be)) {
            String key = switch (game.result) {
                case BLACKJACK -> "casino.blackjack.res_bj";
                case WIN -> "casino.blackjack.res_win";
                case PUSH -> "casino.blackjack.res_push";
                case BUST -> "casino.blackjack.res_bust";
                default -> "casino.blackjack.res_lose";
            };
            ChatFormatting col = switch (game.result) {
                case BLACKJACK -> ChatFormatting.GOLD;
                case WIN -> ChatFormatting.GREEN;
                case PUSH -> ChatFormatting.YELLOW;
                default -> ChatFormatting.RED;
            };
            int amount = game.result == BlackjackGame.Result.BUST || game.result == BlackjackGame.Result.LOSE ? game.bet : game.payout;
            g.drawString(font, Component.translatable(key, amount).withStyle(col, ChatFormatting.BOLD), rx, ry, 0xFFFFFF, true);
        } else if (m == Mode.WAIT) {
            g.drawString(font, Component.translatable("casino.blackjack.dealer_plays").withStyle(ChatFormatting.AQUA), rx, ry, 0xFFFFFF, true);
        } else if (m == Mode.BUSY) {
            g.drawString(font, Component.translatable("casino.blackjack.busy", be.playerName).withStyle(ChatFormatting.YELLOW), rx, ry, 0xFFFFFF, true);
        } else if (m == Mode.PLAY) {
            g.drawString(font, Component.translatable("casino.blackjack.your_turn").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), rx, ry, 0xFFFFFF, true);
        }
        if (m == Mode.PLAY || m == Mode.WAIT || (m == Mode.START && game.phase == Phase.DONE && mine(be))) {
            Component b = Component.translatable("casino.blackjack.bet_now", game.bet).withStyle(ChatFormatting.GRAY);
            g.drawString(font, b, rx, ry + 14, 0xFFFFFF, false);
        }
        if (m == Mode.START) {
            int x0 = left + 10;
            g.drawString(font, Component.translatable("casino.plinko.bet").withStyle(ChatFormatting.GRAY), x0 + 160, top + 108, 0xFFFFFF, false);
            g.fill(x0 + 182, top + 120, x0 + 230, top + 142, 0xFF0A0710);
            frame(g, x0 + 182, top + 120, x0 + 230, top + 142, 0xFF8A6A1E);
            int chipValue = bet() >= 500 ? 500 : bet() >= 100 ? 100 : bet() >= 25 ? 25 : 5;
            g.renderItem(new ItemStack(Chips.itemFor(chipValue).get()), x0 + 184, top + 123);
            g.drawString(font, Component.literal(String.valueOf(bet())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 202, top + 127, 0xFFFFFF, true);
        }
        super.render(g, mx, my, pt);
    }
}
