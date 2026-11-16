package com.casino.client;

import com.casino.block.Cashier;
import com.casino.chip.Chips;
import com.casino.game.Exchange;
import com.casino.net.CashierPacket;
import com.casino.net.CasinoNet;
import com.casino.plat.Plat;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * La caja del casino, con dos apartados:
 * <ul>
 *   <li><b>Cambio</b>: arriba ves lo que llevas (fichas, monedas arcade, boletos, esmeraldas y diamantes). Eliges qué ENTREGAS y qué
 *   RECIBES, la cantidad, y abajo ves exactamente el cambio antes de confirmar. Todo vale lo mismo de ida y de vuelta
 *   ({@link Exchange}).</li>
 *   <li><b>Tarjetas VIP</b>: las dos tarjetas con su precio, qué abren y si ya la tienes.</li>
 * </ul>
 * Solo manda peticiones; el servidor las comprueba.
 */
public final class CashierScreen extends CasinoScreen {
    private static final int W = 332;
    private static final int H = 262;
    private static final int TILE = 28;
    private static final int TILES_Y = 88;
    private static final int[] ORDER = {Exchange.CHIPS, Exchange.COINS, Exchange.TICKETS, Exchange.EMERALD, Exchange.DIAMOND};

    private final BlockPos pos;
    private int left;
    private int top;
    private int tab = 0;
    private int from = Exchange.EMERALD;
    private int to = Exchange.CHIPS;
    /** Cuántos lotes (ver {@link Exchange}). */
    private int lots = 1;

    private Button tabEx;
    private Button tabVip;
    private final List<Button> exButtons = new ArrayList<>();
    private Button minus10;
    private Button minus1;
    private Button plus1;
    private Button plus10;
    private Button maxBtn;
    private Button confirm;
    private final Button[] vipBuy = new Button[2];

    public CashierScreen(BlockPos pos) {
        super(Component.translatable("block.casino.cashier"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        exButtons.clear();
        int tw = (W - 20) / 2;
        tabEx = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.tab_exchange"), b -> setTab(0))
                .bounds(left + 8, top + 26, tw, 18).build());
        tabVip = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.tab_vip"), b -> setTab(1))
                .bounds(left + 12 + tw, top + 26, tw, 18).build());

        int y = top + 156;
        int x = left + (W - 248) / 2;
        minus10 = ex(Button.builder(Component.literal("-10"), b -> change(-10)).bounds(x, y, 32, 20).build());
        minus1 = ex(Button.builder(Component.literal("-1"), b -> change(-1)).bounds(x + 36, y, 32, 20).build());
        plus1 = ex(Button.builder(Component.literal("+1"), b -> change(1)).bounds(x + 136, y, 32, 20).build());
        plus10 = ex(Button.builder(Component.literal("+10"), b -> change(10)).bounds(x + 172, y, 32, 20).build());
        maxBtn = ex(Button.builder(Component.translatable("casino.cashier.max"), b -> {
            lots = max();
            updateButtons();
        }).bounds(x + 208, y, 40, 20).build());
        confirm = ex(Button.builder(Component.translatable("casino.cashier.confirm").withStyle(ChatFormatting.BOLD), b -> send())
                .bounds(left + 8, top + H - 28, 200, 20).build());

        int cw = (W - 20) / 2;
        for (int t = 0; t < 2; t++) {
            int tier = t;
            vipBuy[t] = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.vip_buy",
                    String.format("%,d", Cashier.VIP_PRICE[t])).withStyle(t == 0 ? ChatFormatting.WHITE : ChatFormatting.GOLD),
                    b -> CasinoNet.sendToServer(new CashierPacket(pos, 100 + tier, false, 1)))
                    .bounds(left + 16 + t * (cw + 4), top + H - 56, cw - 16, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose())
                .bounds(left + 212, top + H - 28, W - 220, 20).build());
        setTab(tab);
    }

    private Button ex(Button b) {
        exButtons.add(addRenderableWidget(b));
        return b;
    }

    private void setTab(int t) {
        tab = t;
        for (Button b : exButtons) {
            b.visible = t == 0;
        }
        for (Button b : vipBuy) {
            b.visible = t == 1;
        }
        clamp();
    }

    private void change(int d) {
        lots += d;
        clamp();
    }

    private void send() {
        clamp();
        if (lots > 0 && Exchange.valid(from, to)) {
            CasinoNet.sendToServer(new CashierPacket(pos, 1000 + from * 10 + to, false, lots));
        }
    }

    // ------------------------------------------------------------------ lo que lleva el jugador

    private static long count(Item item) {
        long n = 0;
        var p = Minecraft.getInstance().player;
        if (p != null) {
            for (ItemStack s : Plat.mainItems(p.getInventory())) {
                if (s.is(item)) {
                    n += s.getCount();
                }
            }
        }
        return n;
    }

    private static long have(int cur) {
        Item it = Cashier.itemOf(cur);
        if (it == null) {
            var p = Minecraft.getInstance().player;
            return p == null ? 0 : Chips.balance(p);
        }
        return count(it);
    }

    private static ItemStack icon(int cur) {
        Item it = Cashier.itemOf(cur);
        return new ItemStack(it == null ? Chips.itemFor(25).get() : it);
    }

    private static String compact(long n) {
        if (n >= 10_000_000) {
            return (n / 1_000_000) + "M";
        }
        if (n >= 100_000) {
            return (n / 1_000) + "k";
        }
        return String.format("%,d", n);
    }

    private int max() {
        return Exchange.maxLots(from, to, have(from));
    }

    private void clamp() {
        int m = max();
        lots = m <= 0 ? 0 : Math.max(1, Math.min(lots, m));
        updateButtons();
    }

    private void updateButtons() {
        if (confirm == null) {
            return;
        }
        int m = max();
        minus10.active = lots > 1;
        minus1.active = lots > 1;
        plus1.active = lots < m;
        plus10.active = lots < m;
        maxBtn.active = lots < m;
        confirm.active = lots > 0;
        for (int t = 0; t < 2; t++) {
            vipBuy[t].active = !hasCard(t + 1);
        }
    }

    /** ¿Lleva una tarjeta VIP de este nivel o mejor? (lo comprado y guardado lo revisa el servidor, que da una copia si la perdió). */
    private static boolean hasCard(int level) {
        var p = Minecraft.getInstance().player;
        if (p == null) {
            return false;
        }
        for (ItemStack s : Plat.mainAndOffhand(p.getInventory())) {
            if (s.getItem() instanceof com.casino.item.VipCardItem c && c.level >= level) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 64) {
            onClose();
            return;
        }
        clamp();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ fichas para elegir

    private int tileX(int col, int i) {
        return left + (col == 0 ? 12 : 172) + i * (TILE + 2);
    }

    private int hoverTile(double mx, double my, int col) {
        for (int i = 0; i < ORDER.length; i++) {
            int x = tileX(col, i), y = top + TILES_Y;
            if (mx >= x && mx < x + TILE && my >= y && my < y + TILE) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (button == 0 && tab == 0) {
            for (int col = 0; col < 2; col++) {
                int i = hoverTile(mx, my, col);
                if (i < 0) {
                    continue;
                }
                int cur = ORDER[i];
                if (col == 0) {
                    if (cur == to) {
                        to = from;                    // elegir lo mismo de los dos lados: se voltea el cambio
                    }
                    from = cur;
                } else {
                    if (cur == from) {
                        from = to;
                    }
                    to = cur;
                }
                lots = 1;
                clamp();
                Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                        .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                return true;
            }
        }
        return superClick(mx, my, button);
    }

    // ------------------------------------------------------------------ dibujo

    private void frame(G g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 1, color);
        g.fill(x0, y1 - 1, x1, y1, color);
        g.fill(x0, y0, x0 + 1, y1, color);
        g.fill(x1 - 1, y0, x1, y1, color);
    }

    private void bigItem(G g, ItemStack s, int x, int y, float scale) {
        g.push();
        g.translate(x, y, 0);
        g.scale(scale, scale, 1);
        g.renderItem(s, 0, 0);
        g.pop();
    }

    private static Component name(int cur) {
        return Component.translatable("casino.cashier.cur_" + cur);
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        // marco con luces de marquesina
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, 0xFF5E4614);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFB08A2E);
        g.fill(left, top, left + W, top + H, 0xF2120D19);
        g.fillGradient(left, top, left + W, top + 22, 0xFF4A2E12, 0xFF2A180A);
        long t = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        for (int i = 0; i < 16; i++) {
            boolean on = (i + t / 6) % 3 == 0;
            int bx = left + 6 + i * ((W - 12) / 15);
            g.fill(bx - 1, top - 2, bx + 1, top, on ? 0xFFFFF2A0 : 0xFF7A5A20);
            g.fill(bx - 1, top + H, bx + 1, top + H + 2, on ? 0xFFFFF2A0 : 0xFF7A5A20);
        }
        g.drawCenteredString(font, Component.translatable("casino.cashier.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, top + 7, 0xFFFFFF);

        // cartera: lo que llevas de cada cosa
        int wy = top + 48;
        g.fill(left + 8, wy, left + W - 8, wy + 22, 0xFF221A30);
        frame(g, left + 8, wy, left + W - 8, wy + 22, 0xFF3E3254);
        int sw = (W - 16) / ORDER.length;
        List<Component> tip = null;
        for (int i = 0; i < ORDER.length; i++) {
            int cur = ORDER[i];
            int sx = left + 8 + i * sw;
            g.renderItem(icon(cur), sx + 4, wy + 3);
            g.drawString(font, Component.literal(compact(have(cur))).withStyle(ChatFormatting.YELLOW), sx + 22, wy + 7, 0xFFFFFF, true);
            if (mx >= sx && mx < sx + sw && my >= wy && my < wy + 22) {
                tip = List.of(name(cur).copy().withStyle(ChatFormatting.WHITE),
                        Component.translatable("casino.cashier.worth", Exchange.VALUE[cur]).withStyle(ChatFormatting.GOLD));
            }
        }

        drawWidgets(g, mx, my, pt);
        Button sel = tab == 0 ? tabEx : tabVip;
        frame(g, sel.getX() - 1, sel.getY() - 1, sel.getX() + sel.getWidth() + 1, sel.getY() + sel.getHeight() + 1, 0xFFFFD34D);

        if (tab == 0) {
            List<Component> t2 = drawExchange(g, mx, my);
            if (t2 != null) {
                tip = t2;
            }
        } else {
            drawVip(g);
        }
        if (tip != null) {
            g.renderTooltip(font, tip, mx, my);
        }
    }

    private List<Component> drawExchange(G g, int mx, int my) {
        List<Component> tip = null;
        g.drawCenteredString(font, Component.translatable("casino.cashier.give_col").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                left + 12 + (5 * TILE + 8) / 2, top + 76, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("casino.cashier.get_col").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                left + 172 + (5 * TILE + 8) / 2, top + 76, 0xFFFFFF);
        for (int col = 0; col < 2; col++) {
            int chosen = col == 0 ? from : to;
            int hv = hoverTile(mx, my, col);
            for (int i = 0; i < ORDER.length; i++) {
                int cur = ORDER[i];
                int x = tileX(col, i), y = top + TILES_Y;
                boolean selected = cur == chosen;
                boolean empty = col == 0 && have(cur) <= 0;
                g.fill(x, y, x + TILE, y + TILE, selected ? 0xFF4A3A16 : (hv == i ? 0xFF2E2440 : 0xFF1C1626));
                frame(g, x, y, x + TILE, y + TILE, selected ? 0xFFFFD34D : 0xFF4A3D60);
                g.renderItem(icon(cur), x + 6, y + 6);
                if (empty) {
                    g.fill(x + 1, y + 1, x + TILE - 1, y + TILE - 1, 0x99101010);
                }
                if (hv == i) {
                    tip = List.of(name(cur).copy().withStyle(ChatFormatting.WHITE),
                            Component.translatable("casino.cashier.you_have", compact(have(cur))).withStyle(ChatFormatting.GRAY));
                }
            }
            g.drawCenteredString(font, name(chosen).copy().withStyle(ChatFormatting.WHITE), left + (col == 0 ? 12 : 172) + (5 * TILE + 8) / 2,
                    top + TILES_Y + TILE + 4, 0xFFFFFF);
        }
        g.drawCenteredString(font, Component.literal("→").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, top + TILES_Y + 10, 0xFFFFFF);

        // a cuánto está el cambio
        int gv = Exchange.give(from, to), rv = Exchange.get(from, to);
        g.drawCenteredString(font, Component.translatable("casino.cashier.rate", Cashier.amountOf(from, gv), Cashier.amountOf(to, rv))
                .withStyle(ChatFormatting.GOLD), left + W / 2, top + 134, 0xFFFFFF);

        // cantidad (lo que vas a recibir)
        int bx = left + (W - 248) / 2 + 72;
        g.fill(bx, top + 156, bx + 60, top + 176, 0xFF0A0710);
        frame(g, bx, top + 156, bx + 60, top + 176, 0xFFB08A2E);
        g.drawCenteredString(font, Component.literal(compact((long) lots * rv)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), bx + 30, top + 162, 0xFFFFFF);
        g.drawCenteredString(font, Component.translatable("casino.cashier.amount").withStyle(ChatFormatting.GRAY), bx + 30, top + 146, 0xFFFFFF);

        // el cambio: lo que entregas y lo que recibes
        int ry = top + 182;
        g.fill(left + 8, ry, left + W - 8, ry + 28, 0xFF15291E);
        frame(g, left + 8, ry, left + W - 8, ry + 28, 0xFF2E6B45);
        if (lots > 0) {
            g.drawString(font, Component.translatable("casino.cashier.give").withStyle(ChatFormatting.GRAY), left + 14, ry + 3, 0xFFFFFF, false);
            g.renderItem(icon(from), left + 14, ry + 10);
            g.drawString(font, Cashier.amountOf(from, (long) lots * gv).copy().withStyle(ChatFormatting.WHITE), left + 34, ry + 15, 0xFFFFFF, true);
            g.drawString(font, Component.translatable("casino.cashier.receive").withStyle(ChatFormatting.GRAY), left + W / 2 + 14, ry + 3, 0xFFFFFF, false);
            g.renderItem(icon(to), left + W / 2 + 14, ry + 10);
            g.drawString(font, Cashier.amountOf(to, (long) lots * rv).copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), left + W / 2 + 34, ry + 15, 0xFFFFFF, true);
            g.drawCenteredString(font, Component.literal("→").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, ry + 11, 0xFFFFFF);
        } else {
            g.drawCenteredString(font, Component.translatable("casino.cashier.need", Cashier.amountOf(from, gv)).withStyle(ChatFormatting.RED),
                    left + W / 2, ry + 10, 0xFFFFFF);
        }
        g.drawCenteredString(font, Component.translatable("casino.cashier.fair").withStyle(ChatFormatting.DARK_GRAY), left + W / 2, ry + 32, 0xFFFFFF);
        return tip;
    }

    private void drawVip(G g) {
        int cw = (W - 20) / 2;
        long chips = have(Exchange.CHIPS);
        for (int t = 0; t < 2; t++) {
            int x0 = left + 8 + t * (cw + 4), y0 = top + 74, x1 = x0 + cw, y1 = top + H - 32;
            boolean gold = t == 1;
            g.fillGradient(x0, y0, x1, y1, gold ? 0xFF3A2C0C : 0xFF262B33, 0xFF15121C);
            frame(g, x0, y0, x1, y1, gold ? 0xFFE8B83A : 0xFFB8C2CE);
            ItemStack card = new ItemStack(gold ? com.casino.registry.ModItems.VIP_GOLD.get() : com.casino.registry.ModItems.VIP_SILVER.get());
            g.drawCenteredString(font, card.getHoverName().copy().withStyle(gold ? ChatFormatting.GOLD : ChatFormatting.WHITE, ChatFormatting.BOLD),
                    (x0 + x1) / 2, y0 + 6, 0xFFFFFF);
            bigItem(g, card, x0 + 8, y0 + 18, 2.0F);
            g.drawString(font, Component.translatable("casino.cashier.vip_price", String.format("%,d", Cashier.VIP_PRICE[t]))
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 46, y0 + 23, 0xFFFFFF, true);
            g.drawString(font, Component.translatable("casino.cashier.vip_forever").withStyle(ChatFormatting.GRAY), x0 + 46, y0 + 35, 0xFFFFFF, false);
            int ly = y0 + 56;
            for (FormattedCharSequence line : font.split(Component.translatable(gold ? "casino.cashier.vip_perks_gold" : "casino.cashier.vip_perks_silver")
                    .withStyle(ChatFormatting.GRAY), cw - 14)) {
                g.drawString(font, line, x0 + 7, ly, 0xFFFFFF);
                ly += 10;
            }
            Component status;
            if (hasCard(t + 1)) {
                status = Component.translatable("casino.cashier.vip_owned").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
            } else if (chips >= Cashier.VIP_PRICE[t]) {
                status = Component.translatable("casino.cashier.vip_can").withStyle(ChatFormatting.YELLOW);
            } else {
                status = Component.translatable("casino.cashier.vip_missing", String.format("%,d", Cashier.VIP_PRICE[t] - chips)).withStyle(ChatFormatting.RED);
            }
            g.drawCenteredString(font, status, (x0 + x1) / 2, top + H - 70, 0xFFFFFF);
        }
    }
}
