package com.casino.client;

import com.casino.block.RouletteBE;
import com.casino.chip.Chips;
import com.casino.game.RouletteBets;
import com.casino.game.RouletteBets.Spot;
import com.casino.game.RouletteTable;
import com.casino.game.RouletteTable.Phase;
import com.casino.game.RouletteTable.Slot;
import com.casino.game.RouletteWheel;
import com.casino.net.CasinoNet;
import com.casino.net.RoulettePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de la mesa de ruleta: el paño de apuestas completo, para apostar a mano. Va pegado abajo y sin oscurecer, para ver la rueda
 * mientras se juega. Clic izquierdo = poner la ficha elegida; clic derecho = quitarla; cerca de las líneas se apuesta a varios números a
 * la vez (caballo, calle, cuadro, seisena). Shift = 5 fichas de una vez. Teclas: 1-5 elegir ficha, Z deshacer, C borrar, R repetir,
 * D doblar, Espacio listo.
 */
public final class RouletteScreen extends Screen {
    private static final int W = 424;
    private static final int H = 226;
    private static final int UW = 28;
    private static final int GX = 16;
    private static final int GY = 36;
    private static int lastChip = 1;

    private final BlockPos pos;
    private int left;
    private int top;
    private int chipIdx = lastChip;
    private Button undoBtn, clearBtn, rebetBtn, doubleBtn, readyBtn;
    private final Button[] callBtn = new Button[RouletteBets.CALLS];
    private int hover = -1;

    public RouletteScreen(BlockPos pos) {
        super(Component.translatable("block.casino.roulette"));
        this.pos = pos;
    }

    private RouletteBE table() {
        var level = Minecraft.getInstance().level;
        return level != null && level.getBlockEntity(pos) instanceof RouletteBE be ? be : null;
    }

    private long now() {
        var level = Minecraft.getInstance().level;
        return level == null ? 0 : level.getGameTime();
    }

    private int chip() {
        return RouletteTable.CHIPS[chipIdx];
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private int mySlot(RouletteBE be) {
        var p = Minecraft.getInstance().player;
        return p == null ? -1 : be.slotOf(p.getUUID());
    }

    private boolean open(RouletteBE be) {
        RouletteTable t = be.table;
        return t.phase == Phase.IDLE || t.phase == Phase.BETTING || (t.phase == Phase.RESULT && now() >= be.settledAt + 50);
    }

    /** La bola todavía rueda: el número que ya conoce el servidor no se muestra hasta que se detiene. */
    private boolean rolling(RouletteBE be) {
        return be.table.phase == Phase.SPINNING || (be.table.phase == Phase.RESULT && now() < be.spinStart + RouletteWheel.SPIN_TICKS);
    }

    // ------------------------------------------------------------------ geometría de la cuadrícula en pantalla

    private double pxY(double gy) {
        if (gy < 3) {
            return 24 * gy;
        }
        if (gy < 4) {
            return 72 + 20 * (gy - 3);
        }
        return 92 + 20 * (gy - 4);
    }

    private double gridY(double py) {
        if (py < 72) {
            return py / 24.0;
        }
        if (py < 92) {
            return 3 + (py - 72) / 20.0;
        }
        return 4 + (py - 92) / 20.0;
    }

    private int sx(double gx) {
        return (int) Math.round(left + GX + gx * UW);
    }

    private int sy(double gy) {
        return (int) Math.round(top + GY + pxY(gy));
    }

    private int spotAt(double mx, double my) {
        double gx = (mx - (left + GX)) / UW;
        double py = my - (top + GY);
        if (py < 0 || py >= 112) {
            return -1;
        }
        return RouletteBets.hit(gx, gridY(py), 0.27, 0.30);
    }

    // ------------------------------------------------------------------ botones

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 14;
        int y1 = top + 152;
        int bx = left + GX + 5 * 36 + 8;
        undoBtn = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.undo"), b -> send(RoulettePacket.UNDO, 0, 0))
                .bounds(bx, y1, 52, 20).tooltip(Tooltip.create(Component.translatable("casino.roulette.undo_tip"))).build());
        clearBtn = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.clear"), b -> send(RoulettePacket.CLEAR, 0, 0))
                .bounds(bx + 54, y1, 52, 20).tooltip(Tooltip.create(Component.translatable("casino.roulette.clear_tip"))).build());
        rebetBtn = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.rebet"), b -> send(RoulettePacket.REBET, 0, 0))
                .bounds(bx + 108, y1, 52, 20).tooltip(Tooltip.create(Component.translatable("casino.roulette.rebet_tip"))).build());
        doubleBtn = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.double"), b -> send(RoulettePacket.DOUBLE, 0, 0))
                .bounds(bx + 162, y1, 52, 20).tooltip(Tooltip.create(Component.translatable("casino.roulette.double_tip"))).build());
        int y2 = top + 176;
        String[] keys = {"voisins", "tiers", "orphelins", "zero"};
        int cx = left + GX;
        for (int i = 0; i < RouletteBets.CALLS; i++) {
            final int which = i;
            callBtn[i] = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.call_" + keys[i]), b -> send(RoulettePacket.CALL, which, chip()))
                    .bounds(cx + i * 64, y2, 62, 20)
                    .tooltip(Tooltip.create(Component.translatable("casino.roulette.call_" + keys[i] + "_tip", RouletteBets.units(RouletteBets.call(i))))).build());
        }
        readyBtn = addRenderableWidget(Button.builder(Component.translatable("casino.roulette.ready"), b -> send(RoulettePacket.READY, 0, 0))
                .bounds(cx + 4 * 64 + 6, y2, 80, 20).tooltip(Tooltip.create(Component.translatable("casino.roulette.ready_tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 16 - 44, y2, 44, 20).build());
        updateButtons();
    }

    private void send(int action, int a1, int a2) {
        CasinoNet.sendToServer(new RoulettePacket(pos, action, a1, a2));
    }

    private void updateButtons() {
        RouletteBE be = table();
        boolean o = be != null && open(be);
        int me = be == null ? -1 : mySlot(be);
        boolean has = me >= 0 && be.table.slots[me].total > 0;
        undoBtn.active = o && has;
        clearBtn.active = o && has;
        rebetBtn.active = o;
        doubleBtn.active = o && has;
        for (Button b : callBtn) {
            b.active = o;
        }
        boolean ready = has && be.table.slots[me].ready;
        readyBtn.active = has && be.table.phase == Phase.BETTING;
        readyBtn.setMessage(Component.translatable(ready ? "casino.roulette.ready_on" : "casino.roulette.ready").withStyle(ready ? ChatFormatting.GREEN : ChatFormatting.WHITE));
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || table() == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > 14 * 14) {
            onClose();
            return;
        }
        updateButtons();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ entrada

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) {
            return true;
        }
        RouletteBE be = table();
        int spot = spotAt(mx, my);
        if (be != null && spot >= 0 && open(be)) {
            int mult = hasShiftDown() ? 5 : 1;
            if (button == 0) {
                send(RoulettePacket.PLACE, spot, chip() * mult);
                return true;
            }
            if (button == 1) {
                send(RoulettePacket.REMOVE, spot, chip() * mult);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        chipIdx = Math.max(0, Math.min(RouletteTable.CHIPS.length - 1, chipIdx + (delta > 0 ? 1 : -1)));
        lastChip = chipIdx;
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key >= 49 && key <= 53) { // 1..5
            chipIdx = key - 49;
            lastChip = chipIdx;
            return true;
        }
        if (key == 90 || key == 259) {
            send(RoulettePacket.UNDO, 0, 0);
            return true;
        }
        if (key == 67) {
            send(RoulettePacket.CLEAR, 0, 0);
            return true;
        }
        if (key == 82) {
            send(RoulettePacket.REBET, 0, 0);
            return true;
        }
        if (key == 68) {
            send(RoulettePacket.DOUBLE, 0, 0);
            return true;
        }
        if (key == 32 || key == 257 || key == 335) {
            send(RoulettePacket.READY, 0, 0);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    // ------------------------------------------------------------------ dibujo

    private static int colorOf(int n) {
        return switch (RouletteBets.colorOf(n)) {
            case 0 -> 0xFF1E8A45;
            case 1 -> 0xFFB32222;
            default -> 0xFF17171A;
        };
    }

    private void box(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y1, color);
    }

    private void outline(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private void cell(GuiGraphics g, double gx0, double gy0, double gx1, double gy1, int fill, int border) {
        int x0 = sx(gx0), y0 = sy(gy0), x1 = sx(gx1), y1 = sy(gy1);
        box(g, x0, y0, x1, y1, fill);
        outline(g, x0, y0, x1, y1, border);
    }

    private void centered(GuiGraphics g, String s, double gx0, double gy0, double gx1, double gy1, int color, float scale) {
        int x0 = sx(gx0), y0 = sy(gy0), x1 = sx(gx1), y1 = sy(gy1);
        g.pose().pushPose();
        float cx = (x0 + x1) / 2.0F, cy = (y0 + y1) / 2.0F;
        g.pose().translate(cx, cy, 0);
        g.pose().scale(scale, scale, 1);
        g.drawString(font, s, -font.width(s) / 2, -font.lineHeight / 2 + 1, color, false);
        g.pose().popPose();
    }

    private Component spotName(Spot s) {
        String nums = s.numbers.length <= 6 ? joinNumbers(s.numbers) : "";
        return switch (s.kind) {
            case STRAIGHT -> Component.translatable("casino.roulette.kind_straight", nums);
            case SPLIT -> Component.translatable("casino.roulette.kind_split", nums);
            case STREET -> Component.translatable("casino.roulette.kind_street", nums);
            case TRIO -> Component.translatable("casino.roulette.kind_trio", nums);
            case FIRST_FOUR -> Component.translatable("casino.roulette.kind_first_four");
            case CORNER -> Component.translatable("casino.roulette.kind_corner", nums);
            case SIX_LINE -> Component.translatable("casino.roulette.kind_six_line", nums);
            case COLUMN -> Component.translatable("casino.roulette.kind_column", s.index + 1);
            case DOZEN -> Component.translatable("casino.roulette.felt_dozen_" + (s.index + 1));
            case EVEN_MONEY -> Component.translatable(switch (s.index) {
                case RouletteBets.LOW -> "casino.roulette.felt_low";
                case RouletteBets.HIGH -> "casino.roulette.felt_high";
                case RouletteBets.EVEN -> "casino.roulette.felt_even";
                case RouletteBets.ODD -> "casino.roulette.felt_odd";
                case RouletteBets.RED -> "casino.roulette.red";
                default -> "casino.roulette.black";
            });
        };
    }

    private static String joinNumbers(int[] n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n.length; i++) {
            if (i > 0) {
                sb.append('-');
            }
            sb.append(n[i]);
        }
        return sb.toString();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        RouletteBE be = table();
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xE0102A18);
        g.fill(left, top, left + W, top + 16, 0xFF3A2410);
        g.drawString(font, Component.translatable("casino.roulette.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 4, 0xFFFFFF, true);
        Component bal = Component.translatable("casino.cashier.balance", balance()).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 4, 0xFFFFFF, true);
        if (be == null) {
            super.render(g, mx, my, pt);
            return;
        }
        RouletteTable t = be.table;
        int me = mySlot(be);
        Slot mine = me >= 0 ? t.slots[me] : null;
        long now = now();
        boolean rolling = rolling(be);
        hover = open(be) ? spotAt(mx, my) : -1;

        // ---- historial y estado
        int from = rolling && t.phase == Phase.RESULT ? 1 : 0;
        g.drawString(font, Component.translatable("casino.roulette.history").withStyle(ChatFormatting.GRAY), left + GX, top + 20, 0xFFFFFF, false);
        int hx = left + GX + 40;
        for (int i = from; i < t.past.size() && i < from + 12; i++) {
            int n = t.past.get(i);
            int x = hx + (i - from) * 17;
            box(g, x, top + 18, x + 15, top + 29, colorOf(n));
            g.drawString(font, String.valueOf(n), x + 8 - font.width(String.valueOf(n)) / 2, top + 20, 0xFFFFFF, false);
        }
        Component status;
        if (t.phase == Phase.BETTING) {
            status = Component.translatable("casino.roulette.status_bets", Math.max(0, (int) Math.ceil((be.phaseEnd - now) / 20.0))).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        } else if (rolling) {
            status = Component.translatable(now < be.spinStart ? "casino.roulette.status_nomore" : "casino.roulette.status_spinning").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        } else if (t.phase == Phase.RESULT) {
            status = Component.translatable("casino.roulette.status_result", Component.translatable("casino.roulette.number_" + RouletteBets.colorOf(t.winning), t.winning))
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        } else {
            status = Component.translatable("casino.roulette.status_idle").withStyle(ChatFormatting.GRAY);
        }
        g.drawString(font, status, left + W - 10 - font.width(status), top + 20, 0xFFFFFF, true);

        // ---- paño
        int felt = 0xFF0E5C2C;
        box(g, sx(0) - 2, sy(0) - 2, sx(14) + 2, sy(5) + 2, 0xFF0A3A1C);
        int line = 0xFFD6CBA8;
        cell(g, 0, 0, 1, 3, colorOf(0), line);
        centered(g, "0", 0, 0, 1, 3, 0xFFFFFFFF, 1.3F);
        for (int n = 1; n <= 36; n++) {
            double[] c = RouletteBets.numberCenter(n);
            cell(g, c[0] - 0.5, c[1] - 0.5, c[0] + 0.5, c[1] + 0.5, colorOf(n), line);
            centered(g, String.valueOf(n), c[0] - 0.5, c[1] - 0.5, c[0] + 0.5, c[1] + 0.5, 0xFFFFFFFF, 1.0F);
        }
        for (int k = 0; k < 3; k++) {
            cell(g, 13, 2 - k, 14, 3 - k, felt, line);
            centered(g, "2:1", 13, 2 - k, 14, 3 - k, 0xFFE8DFC4, 0.8F);
            cell(g, 1 + 4 * k, 3, 5 + 4 * k, 4, felt, line);
            centered(g, Component.translatable("casino.roulette.felt_dozen_" + (k + 1)).getString(), 1 + 4 * k, 3, 5 + 4 * k, 4, 0xFFE8DFC4, 0.85F);
        }
        String[] names = {Component.translatable("casino.roulette.felt_low").getString(), Component.translatable("casino.roulette.felt_even").getString(), "", "",
                Component.translatable("casino.roulette.felt_odd").getString(), Component.translatable("casino.roulette.felt_high").getString()};
        for (int k = 0; k < 6; k++) {
            cell(g, 1 + 2 * k, 4, 3 + 2 * k, 5, k == RouletteBets.RED ? 0xFFB32222 : k == RouletteBets.BLACK ? 0xFF17171A : felt, line);
            if (!names[k].isEmpty()) {
                centered(g, names[k], 1 + 2 * k, 4, 3 + 2 * k, 5, 0xFFE8DFC4, 0.85F);
            }
        }
        // casillas ganadoras (después de que la bola se detiene)
        if (t.phase == Phase.RESULT && !rolling && t.winning >= 0) {
            long since = now - (be.spinStart + RouletteWheel.SPIN_TICKS);
            int pulse = 0x60 + (int) (0x50 * (0.5 + 0.5 * Math.sin(now * 0.3)));
            double[] c = RouletteBets.numberCenter(t.winning);
            if (t.winning == 0) {
                outline(g, sx(0), sy(0), sx(1), sy(3), 0xFFFFD84A);
            } else {
                outline(g, sx(c[0] - 0.5) - 1, sy(c[1] - 0.5) - 1, sx(c[0] + 0.5) + 1, sy(c[1] + 0.5) + 1, 0xFFFFD84A);
            }
            for (int id = RouletteBets.COLUMN_BASE; id < RouletteBets.SPOTS.size(); id++) {
                Spot s = RouletteBets.SPOTS.get(id);
                if (s.covers(t.winning)) {
                    double[] r = outsideBox(s);
                    g.fill(sx(r[0]) + 1, sy(r[1]) + 1, sx(r[2]) - 1, sy(r[3]) - 1, (pulse << 24) | 0xFFD84A);
                }
            }
        }
        // casillas que cubre la apuesta bajo el ratón
        if (hover >= 0) {
            Spot s = RouletteBets.SPOTS.get(hover);
            for (int n : s.numbers) {
                double[] c = RouletteBets.numberCenter(n);
                if (n == 0) {
                    g.fill(sx(0), sy(0), sx(1), sy(3), 0x55FFE070);
                } else {
                    g.fill(sx(c[0] - 0.5), sy(c[1] - 0.5), sx(c[0] + 0.5), sy(c[1] + 0.5), 0x55FFE070);
                }
            }
            if (s.kind == RouletteBets.Kind.COLUMN || s.kind == RouletteBets.Kind.DOZEN || s.kind == RouletteBets.Kind.EVEN_MONEY) {
                double[] r = outsideBox(s);
                g.fill(sx(r[0]), sy(r[1]), sx(r[2]), sy(r[3]), 0x40FFE070);
            }
        }
        // fichas: las mías (con ficha y cantidad) y las de los demás (punto de su color)
        for (Spot s : RouletteBets.SPOTS) {
            int px = sx(s.gx), py = sy(s.gy);
            int mineAmt = mine != null ? mine.stake[s.id] : 0;
            int others = 0;
            for (int i = 0; i < RouletteTable.MAX_PLAYERS; i++) {
                if (i != me && t.slots[i].stake[s.id] > 0) {
                    others++;
                    int col = 0xFF000000 | RouletteRenderer.PLAYER_COLORS[i];
                    g.fill(px - 7 + others * 4, py - 9, px - 4 + others * 4, py - 6, col);
                }
            }
            if (mineAmt > 0) {
                int v = 1;
                for (int cv : Chips.VALUES) {
                    if (mineAmt >= cv) {
                        v = cv;
                        break;
                    }
                }
                g.pose().pushPose();
                g.pose().translate(px - 5, py - 5, 0);
                g.pose().scale(0.62F, 0.62F, 1);
                g.renderItem(new ItemStack(Chips.itemFor(v).get()), 0, 0);
                g.pose().popPose();
                String a = String.valueOf(mineAmt);
                g.pose().pushPose();
                g.pose().translate(px, py + 2, 200);
                g.pose().scale(0.6F, 0.6F, 1);
                g.drawString(font, a, -font.width(a) / 2, 0, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }

        // ---- selector de fichas
        int cy = top + 152;
        for (int i = 0; i < RouletteTable.CHIPS.length; i++) {
            int x = left + GX + i * 36;
            boolean sel = i == chipIdx;
            box(g, x, cy, x + 34, cy + 20, sel ? 0xFF3A5A2A : 0xFF0A0710);
            outline(g, x, cy, x + 34, cy + 20, sel ? 0xFFFFD84A : 0xFF6A5016);
            g.renderItem(new ItemStack(Chips.itemFor(RouletteTable.CHIPS[i]).get()), x + 2, cy + 2);
            g.drawString(font, String.valueOf(RouletteTable.CHIPS[i]), x + 20 - (RouletteTable.CHIPS[i] >= 100 ? 2 : 0), cy + 6, sel ? 0xFFFFE070 : 0xFFBBBBBB, true);
        }

        // ---- línea de estado de abajo
        int ly = top + 202;
        if (mine != null && mine.total > 0) {
            g.drawString(font, Component.translatable("casino.roulette.bet_total", mine.total).withStyle(ChatFormatting.YELLOW), left + GX, ly, 0xFFFFFF, true);
            if (t.phase == Phase.RESULT && !rolling) {
                Component r = mine.net > 0 ? Component.translatable("casino.roulette.you_win", mine.net).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                        : mine.net == 0 ? Component.translatable("casino.roulette.you_even").withStyle(ChatFormatting.YELLOW)
                        : Component.translatable("casino.roulette.you_lose", -mine.net).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                g.drawString(font, r, left + GX + 110, ly, 0xFFFFFF, true);
            }
        } else {
            g.drawString(font, Component.translatable("casino.roulette.help").withStyle(ChatFormatting.GRAY), left + GX, ly, 0xFFFFFF, false);
        }
        // jugadores en la mesa
        int rx = left + W - 12;
        for (int i = RouletteTable.MAX_PLAYERS - 1; i >= 0; i--) {
            if (be.players[i] != null && !be.names[i].isEmpty() && t.slots[i].total > 0) {
                String nm = (be.names[i].length() > 8 ? be.names[i].substring(0, 8) : be.names[i]) + (t.slots[i].ready ? "✔" : "");
                int w = font.width(nm);
                g.fill(rx - w - 6, ly + 1, rx - w - 2, ly + 5, 0xFF000000 | RouletteRenderer.PLAYER_COLORS[i]);
                g.drawString(font, nm, rx - w, ly, i == me ? 0xFFFFE070 : 0xFFCCCCCC, false);
                rx -= w + 12;
            }
        }
        super.render(g, mx, my, pt);
        if (hover >= 0) {
            Spot s = RouletteBets.SPOTS.get(hover);
            List<Component> tip = new ArrayList<>();
            tip.add(spotName(s).copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
            tip.add(Component.translatable("casino.roulette.tip_payout", s.payout).withStyle(ChatFormatting.GOLD));
            if (s.kind == RouletteBets.Kind.EVEN_MONEY) {
                tip.add(Component.translatable("casino.roulette.tip_partage").withStyle(ChatFormatting.GRAY));
            }
            if (mine != null && mine.stake[s.id] > 0) {
                tip.add(Component.translatable("casino.roulette.tip_yours", mine.stake[s.id]).withStyle(ChatFormatting.YELLOW));
            }
            g.renderTooltip(font, tip, java.util.Optional.empty(), mx, my);
        }
    }

    private static double[] outsideBox(Spot s) {
        return switch (s.kind) {
            case COLUMN -> new double[]{13, 2 - s.index, 14, 3 - s.index};
            case DOZEN -> new double[]{1 + 4 * s.index, 3, 5 + 4 * s.index, 4};
            default -> new double[]{1 + 2 * s.index, 4, 3 + 2 * s.index, 5};
        };
    }
}
