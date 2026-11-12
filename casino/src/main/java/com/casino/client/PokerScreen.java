package com.casino.client;

import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.plat.Ids;
import com.casino.Casino;
import com.casino.block.PokerBE;
import com.casino.chip.Chips;
import com.casino.game.PokerHands;
import com.casino.game.PokerTable;
import com.casino.game.PokerTable.Phase;
import com.casino.game.PokerTable.Seat;
import com.casino.game.PokerTable.SeatState;
import com.casino.net.CasinoNet;
import com.casino.net.PokerPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de la mesa de póker (Texas Hold'em contra el crupier). Va pegado abajo y sin oscurecer. Muestra las cartas del crupier, las comunes y
 * las tuyas, quién más juega y los botones según el momento: poner el ante, esperar / repartir ya, o IGUALAR / RETIRARSE.
 */
public final class PokerScreen extends CasinoScreen {
    private static final ResourceLocation CARDS = Ids.rl(Casino.MODID, "textures/entity/cards.png");
    private static final int W = 380;
    private static final int H = 160;
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
    private Button anteBtn;
    private Button startBtn;
    private Button callBtn;
    private Button foldBtn;

    public PokerScreen(BlockPos pos) {
        super(Component.translatable("block.casino.poker"));
        this.pos = pos;
    }

    private PokerBE table() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof PokerBE be ? be : null;
    }

    private int ante() {
        return PokerTable.ANTES[betIdx];
    }

    private int mySeat(PokerBE be) {
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
        int y = top + 132;
        betMinus = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBet(-1)).bounds(x0 + 160, y, 20, 22).build());
        betPlus = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBet(1)).bounds(x0 + 232, y, 20, 22).build());
        anteBtn = addRenderableWidget(Button.builder(Component.translatable("casino.poker.ante_btn").withStyle(ChatFormatting.BOLD), b -> send(PokerPacket.ANTE))
                .bounds(x0, y, 150, 22).build());
        startBtn = addRenderableWidget(Button.builder(Component.translatable("casino.blackjack.start_now").withStyle(ChatFormatting.BOLD), b -> send(PokerPacket.START))
                .bounds(x0, y, 150, 22).build());
        callBtn = addRenderableWidget(Button.builder(Component.translatable("casino.poker.call_btn", 0), b -> send(PokerPacket.CALL)).bounds(x0, y, 150, 22).build());
        foldBtn = addRenderableWidget(Button.builder(Component.translatable("casino.poker.fold_btn"), b -> send(PokerPacket.FOLD)).bounds(x0 + 156, y, 110, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(x0 + 322, y, 38, 22).build());
        updateButtons();
    }

    private void changeBet(int d) {
        betIdx = Math.max(0, Math.min(PokerTable.ANTES.length - 1, betIdx + d));
        lastBet = betIdx;
        updateButtons();
    }

    private void send(int action) {
        if (cooldown > 0) {
            return;
        }
        CasinoNet.sendToServer(new PokerPacket(pos, action, ante()));
        cooldown = 4;
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private enum Mode { BET, WAIT_BET, DECIDE, WATCH, NONE }

    private Mode mode(PokerBE be) {
        PokerTable t = be.table;
        int me = mySeat(be);
        switch (t.phase) {
            case IDLE:
                return Mode.BET;
            case BETTING:
                return me >= 0 ? Mode.WAIT_BET : Mode.BET;
            case DECIDE:
                return me >= 0 && t.seats[me].state == SeatState.DECIDING ? Mode.DECIDE : Mode.WATCH;
            case RESULT:
                return now() >= be.settledAt + 40 ? Mode.BET : Mode.WATCH;
            default:
                return Mode.WATCH;
        }
    }

    private void updateButtons() {
        PokerBE be = table();
        Mode m = be == null ? Mode.NONE : mode(be);
        betMinus.visible = m == Mode.BET;
        betPlus.visible = m == Mode.BET;
        anteBtn.visible = m == Mode.BET;
        startBtn.visible = m == Mode.WAIT_BET;
        callBtn.visible = m == Mode.DECIDE;
        foldBtn.visible = m == Mode.DECIDE;
        betMinus.active = betIdx > 0;
        betPlus.active = betIdx < PokerTable.ANTES.length - 1;
        anteBtn.active = balance() >= ante();
        if (be != null && m == Mode.DECIDE) {
            int me = mySeat(be);
            int cost = be.table.callCost(me);
            callBtn.setMessage(Component.translatable("casino.poker.call_btn", cost));
            boolean ready = now() >= be.actFrom;
            callBtn.active = ready && balance() >= cost;
            foldBtn.active = ready;
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
    protected boolean onKey(int key, int scan, int mods) {
        PokerBE be = table();
        if (be != null) {
            Mode m = mode(be);
            if (m == Mode.DECIDE) {
                if ((key == 67 || key == 32) && callBtn.active) { // C o Espacio
                    send(PokerPacket.CALL);
                    return true;
                }
                if (key == 70 && foldBtn.active) { // F
                    send(PokerPacket.FOLD);
                    return true;
                }
            } else if (m == Mode.BET && (key == 32 || key == 257 || key == 335) && anteBtn.active) {
                send(PokerPacket.ANTE);
                return true;
            } else if (m == Mode.WAIT_BET && (key == 32 || key == 257 || key == 335)) {
                send(PokerPacket.START);
                return true;
            }
        }
        return superKey(key, scan, mods);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void frame(G g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private void drawCard(G g, int x, int y, int card) {
        int col = card < 0 ? 0 : card % 13;
        int row = card < 0 ? 4 : PokerHands.suit(card);
        g.blit(CARDS, x, y, CW, CH, col * 32, row * 48, 32, 48, 416, 240);
    }

    private void slot(G g, int x, int y) {
        g.fill(x, y, x + CW, y + CH, 0x40000000);
        frame(g, x, y, x + CW, y + CH, 0xFF5A4A1A);
    }

    private static String shortName(String n) {
        return n.length() > 10 ? n.substring(0, 10) : n;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        PokerBE be = table();
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xE00F1E3A);
        g.fill(left, top, left + W, top + 16, 0xFF3A2410);
        g.drawString(font, Component.translatable("casino.poker.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 4, 0xFFFFFF, true);
        Component bal = Component.translatable("casino.cashier.balance", balance()).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 4, 0xFFFFFF, true);
        if (be == null) {
            drawWidgets(g, mx, my, pt);
            return;
        }
        PokerTable t = be.table;
        Mode m = mode(be);
        int me = mySeat(be);
        long now = now();
        boolean running = t.phase == Phase.DECIDE || t.phase == Phase.SHOWDOWN || t.phase == Phase.RESULT;
        int hx = left + 10;

        // fila de arriba: crupier (2 cartas) y mesa (5 cartas comunes)
        g.drawString(font, Component.translatable("casino.blackjack.dealer").withStyle(ChatFormatting.GRAY), hx, top + 20, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.poker.board").withStyle(ChatFormatting.GRAY), hx + 70, top + 20, 0xFFFFFF, false);
        int dx = hx;
        int bx = hx + 70;
        for (int j = 0; j < 2; j++) {
            slot(g, dx + j * (CW + 2), top + 31);
            if (running) {
                boolean shown = t.dealerRevealed && t.dealer.size() > j;
                drawCard(g, dx + j * (CW + 2), top + 31, shown ? t.dealer.get(j) : -1);
            }
        }
        for (int j = 0; j < 5; j++) {
            slot(g, bx + j * (CW + 2), top + 31);
            if (j < t.board.size()) {
                drawCard(g, bx + j * (CW + 2), top + 31, t.board.get(j));
            }
        }
        if (t.dealerRevealed && t.dealerCat >= 0) {
            Component dh = Component.translatable("casino.poker.hand_" + t.dealerCat).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
            g.drawString(font, dh, dx, top + 69, 0xFFFFFF, true);
            // el nombre de la mano puede ser largo (p. ej. "Doble pareja" o en otros idiomas): el aviso va después, sin pisarlo
            int qx = Math.max(bx + 2, dx + font.width(dh) + 8);
            g.drawString(font, Component.translatable(t.dealerQualified ? "casino.poker.qualifies" : "casino.poker.not_qualifies")
                    .withStyle(t.dealerQualified ? ChatFormatting.YELLOW : ChatFormatting.RED), qx, top + 69, 0xFFFFFF, true);
        }

        // fila de abajo: tus cartas
        g.drawString(font, Component.translatable("casino.blackjack.you").withStyle(ChatFormatting.GRAY), hx, top + 83, 0xFFFFFF, false);
        if (me >= 0 && !t.seats[me].hole.isEmpty()) {
            Seat s = t.seats[me];
            for (int j = 0; j < s.hole.size(); j++) {
                drawCard(g, hx + j * (CW + 2), top + 93, s.hole.get(j));
            }
            List<Integer> all = new ArrayList<>(s.hole);
            all.addAll(t.board);
            if (all.size() >= 5 && s.state != SeatState.FOLDED) {
                int cat = PokerHands.category(PokerHands.best(all));
                g.drawString(font, Component.translatable("casino.poker.hand_" + cat).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), hx + 2 * (CW + 2) + 4, top + 100, 0xFFFFFF, true);
            }
        } else if (!running) {
            g.drawString(font, Component.translatable("casino.poker.rules").withStyle(ChatFormatting.AQUA), hx, top + 94, 0xFFFFFF, false);
            g.drawString(font, Component.translatable("casino.poker.rules2").withStyle(ChatFormatting.AQUA), hx, top + 104, 0xFFFFFF, false);
        } else if (me < 0) {
            g.drawString(font, Component.translatable("casino.blackjack.watching").withStyle(ChatFormatting.GRAY), hx, top + 96, 0xFFFFFF, false);
        }
        if (me >= 0 && t.phase == Phase.RESULT) {
            Seat s = t.seats[me];
            int spent = s.ante + s.call;
            int net = s.payout - spent;
            Component r = switch (s.result) {
                case WIN -> Component.translatable("casino.poker.res_win", net, Component.translatable("casino.poker.hand_" + s.handCat)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                case WIN_NQ -> Component.translatable("casino.poker.res_nq", net).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
                case PUSH -> Component.translatable("casino.poker.res_push", s.payout).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
                case FOLD -> Component.translatable("casino.poker.res_fold", s.ante).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                default -> Component.translatable("casino.poker.res_lose", spent).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
            };
            g.drawString(font, r, hx + 120, top + 112, 0xFFFFFF, true);
        } else if (me >= 0 && t.seats[me].ante > 0) {
            Seat s = t.seats[me];
            boolean cards = !s.hole.isEmpty();
            g.drawString(font, Component.translatable("casino.poker.bet_now", s.ante, s.call).withStyle(ChatFormatting.GRAY), cards ? hx + 120 : hx, cards ? top + 112 : top + 118, 0xFFFFFF, false);
        }

        // columna de la derecha: estado y quién juega
        int rx = left + 235;
        int ry = top + 20;
        Component status = switch (t.phase) {
            case BETTING -> Component.translatable("casino.blackjack.betting", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.AQUA);
            case DECIDE -> m == Mode.DECIDE
                    ? Component.translatable("casino.poker.your_decision", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : Component.translatable("casino.poker.deciding").withStyle(ChatFormatting.YELLOW);
            case SHOWDOWN -> Component.translatable("casino.poker.showdown").withStyle(ChatFormatting.AQUA);
            case RESULT -> Component.translatable("casino.blackjack.result_title").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("casino.poker.place_ante").withStyle(ChatFormatting.GRAY);
        };
        g.drawString(font, status, rx, ry, 0xFFFFFF, true);
        int line = 0;
        for (int i = 0; i < PokerTable.SEATS; i++) {
            Seat s = t.seats[i];
            if (s.ante <= 0 || be.names[i].isEmpty()) {
                continue;
            }
            ChatFormatting col = i == me ? ChatFormatting.YELLOW : ChatFormatting.WHITE;
            String right;
            ChatFormatting rcol = ChatFormatting.GRAY;
            if (t.phase == Phase.RESULT) {
                int net = s.payout - s.ante - s.call;
                right = s.result == PokerTable.Result.FOLD ? "✖ -" + s.ante : net > 0 ? "+" + net : net == 0 ? "=" : "-" + (-net);
                rcol = net > 0 ? ChatFormatting.GREEN : net == 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            } else if (s.state == SeatState.FOLDED) {
                right = Component.translatable("casino.poker.tag_fold").getString();
            } else if (s.state == SeatState.CALLED) {
                right = "✔ " + (s.ante + s.call);
                rcol = ChatFormatting.GREEN;
            } else if (s.state == SeatState.DECIDING) {
                right = "…";
                rcol = ChatFormatting.YELLOW;
            } else {
                right = String.valueOf(s.ante);
            }
            g.drawString(font, Component.literal("  " + shortName(be.names[i])).withStyle(col), rx, ry + 14 + line * 10, 0xFFFFFF, false);
            Component r = Component.literal(right).withStyle(rcol);
            g.drawString(font, r, left + W - 10 - font.width(r), ry + 14 + line * 10, 0xFFFFFF, false);
            line++;
        }
        if (m == Mode.BET) {
            int x0 = left + 10;
            g.drawString(font, Component.translatable("casino.poker.ante").withStyle(ChatFormatting.GRAY), x0 + 160, top + 123, 0xFFFFFF, false);
            g.fill(x0 + 182, top + 132, x0 + 230, top + 154, 0xFF0A0710);
            frame(g, x0 + 182, top + 132, x0 + 230, top + 154, 0xFF8A6A1E);
            int chipValue = ante() >= 500 ? 500 : ante() >= 100 ? 100 : ante() >= 25 ? 25 : 5;
            g.renderItem(new ItemStack(Chips.itemFor(chipValue).get()), x0 + 184, top + 135);
            g.drawString(font, Component.literal(String.valueOf(ante())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 202, top + 139, 0xFFFFFF, true);
        }
        drawWidgets(g, mx, my, pt);
    }
}
