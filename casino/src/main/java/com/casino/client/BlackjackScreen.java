package com.casino.client;

import com.casino.plat.Ids;
import com.casino.Casino;
import com.casino.block.BlackjackBE;
import com.casino.chip.Chips;
import com.casino.game.BlackjackGame;
import com.casino.game.BlackjackTable;
import com.casino.game.BlackjackTable.Phase;
import com.casino.game.BlackjackTable.Seat;
import com.casino.game.BlackjackTable.SeatState;
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
 * Muestra la mano del crupier y la tuya (con las cartas del atlas), quién más juega, y los botones según el momento:
 * apostar, esperar / repartir ya, o pedir / plantarse / doblar en tu turno.
 */
public final class BlackjackScreen extends Screen {
    private static final ResourceLocation CARDS = Ids.rl(Casino.MODID, "textures/entity/cards.png");
    private static final int W = 380;
    private static final int H = 150;
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
    private Button betBtn;
    private Button startBtn;
    private Button hitBtn;
    private Button standBtn;
    private Button doubleBtn;
    private Button splitBtn;
    private Button insureBtn;
    private Button declineBtn;

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

    private int mySeat(BlackjackBE be) {
        var p = Minecraft.getInstance().player;
        return p == null ? -1 : be.seatOf(p.getUUID());
    }

    private long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 30;
        int x0 = left + 10;
        betMinus = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBet(-1)).bounds(x0 + 160, top + 122, 20, 22).build());
        betPlus = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBet(1)).bounds(x0 + 232, top + 122, 20, 22).build());
        betBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.deal").withStyle(ChatFormatting.BOLD), b -> send(BlackjackPacket.DEAL))
                .bounds(x0, top + 122, 150, 22).build());
        startBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.start_now").withStyle(ChatFormatting.BOLD), b -> send(BlackjackPacket.START))
                .bounds(x0, top + 122, 150, 22).build());
        hitBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.hit"), b -> send(BlackjackPacket.HIT)).bounds(x0, top + 122, 68, 22).build());
        standBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.stand"), b -> send(BlackjackPacket.STAND)).bounds(x0 + 71, top + 122, 78, 22).build());
        doubleBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.double"), b -> send(BlackjackPacket.DOUBLE)).bounds(x0 + 152, top + 122, 74, 22).build());
        splitBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.split"), b -> send(BlackjackPacket.SPLIT)).bounds(x0 + 229, top + 122, 78, 22).build());
        insureBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.insure_btn", 0), b -> send(BlackjackPacket.INSURE)).bounds(x0, top + 122, 150, 22).build());
        declineBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.insure_no"), b -> send(BlackjackPacket.DECLINE)).bounds(x0 + 156, top + 122, 110, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(x0 + 322, top + 122, 38, 22).build());
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

    private enum Mode { BET, WAIT_BET, PLAY, INSURE, WATCH, NONE }

    private Mode mode(BlackjackBE be) {
        BlackjackTable t = be.table;
        int me = mySeat(be);
        switch (t.phase) {
            case IDLE:
                return Mode.BET;
            case BETTING:
                return me >= 0 ? Mode.WAIT_BET : Mode.BET;
            case TURNS:
                return me >= 0 && t.current == me ? Mode.PLAY : Mode.WATCH;
            case INSURANCE:
                return me >= 0 && t.canInsure(me) ? Mode.INSURE : Mode.WATCH;
            case RESULT:
                return now() >= be.settledAt + 40 ? Mode.BET : Mode.WATCH;
            default:
                return Mode.WATCH;
        }
    }

    private void updateButtons() {
        BlackjackBE be = table();
        Mode m = be == null ? Mode.NONE : mode(be);
        betMinus.visible = m == Mode.BET;
        betPlus.visible = m == Mode.BET;
        betBtn.visible = m == Mode.BET;
        startBtn.visible = m == Mode.WAIT_BET;
        hitBtn.visible = m == Mode.PLAY;
        standBtn.visible = m == Mode.PLAY;
        doubleBtn.visible = m == Mode.PLAY;
        splitBtn.visible = m == Mode.PLAY;
        insureBtn.visible = m == Mode.INSURE;
        declineBtn.visible = m == Mode.INSURE;
        betMinus.active = betIdx > 0;
        betPlus.active = betIdx < BlackjackGame.BETS.length - 1;
        betBtn.active = balance() >= bet();
        if (be != null) {
            int me = mySeat(be);
            doubleBtn.active = me >= 0 && be.table.canDouble(me) && balance() >= be.table.activeBet(me);
            splitBtn.active = me >= 0 && be.table.canSplit(me) && balance() >= be.table.activeBet(me);
            if (me >= 0 && m == Mode.INSURE) {
                int cost = be.table.seats[me].bet / 2;
                insureBtn.setMessage(Component.translatable("casino.blackjack.insure_btn", cost));
                insureBtn.active = balance() >= cost;
            }
        }
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
                if (key == 80 && splitBtn.active) { // P
                    send(BlackjackPacket.SPLIT);
                    return true;
                }
            } else if (m == Mode.INSURE) {
                if (key == 89 && insureBtn.active) { // Y / S no hay: Y = sí
                    send(BlackjackPacket.INSURE);
                    return true;
                }
                if (key == 78) { // N
                    send(BlackjackPacket.DECLINE);
                    return true;
                }
            } else if (m == Mode.BET && (key == 32 || key == 257 || key == 335) && betBtn.active) {
                send(BlackjackPacket.DEAL);
                return true;
            } else if (m == Mode.WAIT_BET && (key == 32 || key == 257 || key == 335)) {
                send(BlackjackPacket.START);
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

    private static String seatText(BlackjackTable t, int i) {
        Seat s = t.seats[i];
        if (s.cards.isEmpty()) {
            return "";
        }
        if (s.split) {
            return s.total() + " | " + s.total2();
        }
        String tot = String.valueOf(s.total());
        return switch (s.state) {
            case BUST -> tot + " ✖";
            case BLACKJACK -> "BJ";
            case STAND -> tot + " ✔";
            default -> tot;
        };
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
        BlackjackTable t = be.table;
        Mode m = mode(be);
        int me = mySeat(be);
        long now = now();
        boolean running = t.phase == Phase.TURNS || t.phase == Phase.DEALER || t.phase == Phase.RESULT || t.phase == Phase.INSURANCE;

        // manos: crupier arriba, jugador abajo
        int hx = left + 10;
        g.drawString(font, Component.translatable("casino.blackjack.dealer").withStyle(ChatFormatting.GRAY), hx, top + 20, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.blackjack.you").withStyle(ChatFormatting.GRAY), hx, top + 68, 0xFFFFFF, false);
        if (running && !t.dealer.isEmpty()) {
            drawHand(g, hx, top + 30, t.dealer, !t.holeRevealed);
            String dt = t.holeRevealed ? String.valueOf(BlackjackGame.total(t.dealer)) : t.dealerShownTotal() + " + ?";
            g.drawString(font, Component.literal(dt).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), hx + 30 + (Math.max(t.dealer.size(), 2) - 1) * (CW - 6) + CW, top + 44, 0xFFFFFF, true);
        } else {
            g.drawString(font, Component.translatable("casino.blackjack.rules").withStyle(ChatFormatting.GREEN), hx, top + 36, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("casino.blackjack.rules2").withStyle(ChatFormatting.GREEN), hx, top + 48, 0xFFFFFF, false);
        }
        if (me >= 0 && !t.seats[me].cards.isEmpty()) {
            Seat s = t.seats[me];
            for (int h = 0; h < (s.split ? 2 : 1); h++) {
                List<Integer> hc = s.cards(h);
                int hxx = hx + h * 120;
                drawHand(g, hxx, top + 78, hc, false);
                int tot = BlackjackGame.total(hc);
                String pt2 = String.valueOf(tot);
                if (BlackjackGame.soft(hc) && tot > 11 && tot < 21) {
                    pt2 = (tot - 10) + " / " + tot;
                }
                boolean active = s.split && t.phase == Phase.TURNS && t.current == me && s.hand == h;
                g.drawString(font, Component.literal((active ? "▶ " : "") + pt2).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), hxx + 30 + (hc.size() - 1) * (CW - 6) + CW - (active ? 10 : 0), top + 92, 0xFFFFFF, true);
            }
        } else if (!running) {
            g.drawString(font, Component.translatable("casino.blackjack.rules3").withStyle(ChatFormatting.GREEN), hx, top + 84, 0xFFFFFF, false);
        } else if (me < 0) {
            g.drawString(font, Component.translatable("casino.blackjack.watching").withStyle(ChatFormatting.GRAY), hx, top + 84, 0xFFFFFF, false);
        }

        // columna de la derecha: estado y quién juega
        int rx = left + 235;
        int ry = top + 20;
        Component status = switch (t.phase) {
            case BETTING -> Component.translatable("casino.blackjack.betting", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.AQUA);
            case TURNS -> m == Mode.PLAY
                    ? Component.translatable("casino.blackjack.your_turn", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("casino.blackjack.turn_of", t.current >= 0 ? be.names[t.current] : "").withStyle(ChatFormatting.YELLOW);
            case INSURANCE -> m == Mode.INSURE
                    ? Component.translatable("casino.blackjack.insurance_ask", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("casino.blackjack.insurance_wait").withStyle(ChatFormatting.YELLOW);
            case DEALER -> Component.translatable("casino.blackjack.dealer_plays").withStyle(ChatFormatting.AQUA);
            case RESULT -> Component.translatable("casino.blackjack.result_title").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("casino.blackjack.place_bet").withStyle(ChatFormatting.GRAY);
        };
        g.drawString(font, status, rx, ry, 0xFFFFFF, true);
        int line = 0;
        for (int i = 0; i < BlackjackTable.SEATS; i++) {
            Seat s = t.seats[i];
            if (s.bet <= 0 || be.names[i].isEmpty()) {
                continue;
            }
            boolean turn = t.phase == Phase.TURNS && t.current == i;
            ChatFormatting col = i == me ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            String name = be.names[i].length() > 10 ? be.names[i].substring(0, 10) : be.names[i];
            String right = t.phase == Phase.RESULT ? resultShort(s) : (s.cards.isEmpty() ? s.bet + "" : seatText(t, i));
            g.drawString(font, Component.literal((turn ? "▶ " : "  ") + name).withStyle(col), rx, ry + 14 + line * 10, 0xFFFFFF, false);
            Component r = Component.literal(right).withStyle(turn ? ChatFormatting.GREEN : ChatFormatting.GRAY);
            g.drawString(font, r, left + W - 10 - font.width(r), ry + 14 + line * 10, 0xFFFFFF, false);
            line++;
        }
        if (me >= 0 && t.phase == Phase.RESULT) {
            Seat s = t.seats[me];
            String key = switch (s.result) {
                case BLACKJACK -> "casino.blackjack.res_bj";
                case WIN -> "casino.blackjack.res_win";
                case PUSH -> "casino.blackjack.res_push";
                case BUST -> "casino.blackjack.res_bust";
                default -> "casino.blackjack.res_lose";
            };
            ChatFormatting col = switch (s.result) {
                case BLACKJACK -> ChatFormatting.GOLD;
                case WIN -> ChatFormatting.GREEN;
                case PUSH -> ChatFormatting.YELLOW;
                default -> ChatFormatting.RED;
            };
            int amount = s.result == BlackjackGame.Result.BUST || s.result == BlackjackGame.Result.LOSE ? s.bet : s.payout;
            Component res = Component.translatable(key, amount).withStyle(col, ChatFormatting.BOLD);
            if (s.split || s.insurance > 0) {
                int net = s.totalPayout() - s.stake();
                res = net > 0 ? Component.translatable("casino.blackjack.res_total_win", net).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        : net == 0 ? Component.translatable("casino.blackjack.res_total_even").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        : Component.translatable("casino.blackjack.res_total_lose", -net).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            }
            g.drawString(font, res, hx + (s.split ? 250 : 150), top + 96, 0xFFFFFF, true);
        }
        if (me >= 0 && t.phase != Phase.RESULT && t.seats[me].bet > 0) {
            Seat ms = t.seats[me];
            g.drawString(font, Component.translatable("casino.blackjack.bet_now", ms.bet + ms.bet2 + ms.insurance).withStyle(ChatFormatting.GRAY), hx + (ms.split ? 250 : 150), top + 96, 0xFFFFFF, false);
        }
        if (m == Mode.BET) {
            int x0 = left + 10;
            g.drawString(font, Component.translatable("casino.plinko.bet").withStyle(ChatFormatting.GRAY), x0 + 160, top + 110, 0xFFFFFF, false);
            g.fill(x0 + 182, top + 122, x0 + 230, top + 144, 0xFF0A0710);
            frame(g, x0 + 182, top + 122, x0 + 230, top + 144, 0xFF8A6A1E);
            int chipValue = bet() >= 500 ? 500 : bet() >= 100 ? 100 : bet() >= 25 ? 25 : 5;
            g.renderItem(new ItemStack(Chips.itemFor(chipValue).get()), x0 + 184, top + 125);
            g.drawString(font, Component.literal(String.valueOf(bet())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 202, top + 129, 0xFFFFFF, true);
        }
        super.render(g, mx, my, pt);
    }

    private static String resultShort(Seat s) {
        if (s.split || s.insurance > 0) {
            int net = s.totalPayout() - s.stake();
            return net > 0 ? "+" + net : net == 0 ? "=" : "-" + (-net);
        }
        return switch (s.result) {
            case BLACKJACK -> "BJ +" + (s.payout - s.bet);
            case WIN -> "+" + (s.payout - s.bet);
            case PUSH -> "=";
            case BUST -> "✖ -" + s.bet;
            default -> "-" + s.bet;
        };
    }
}
