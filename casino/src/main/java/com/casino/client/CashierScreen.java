package com.casino.client;

import com.casino.plat.Plat;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.block.Cashier;
import com.casino.chip.Chips;
import com.casino.net.CashierPacket;
import com.casino.net.CasinoNet;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Pantalla del cajero, pensada para que se entienda sola:
 * arriba eliges si quieres COMPRAR fichas o CANJEARLAS, en medio eliges esmeralda o diamante y la cantidad,
 * y abajo ves exactamente lo que entregas y lo que recibes antes de pulsar CONFIRMAR.
 * Solo manda una petición; el servidor la comprueba.
 */
public final class CashierScreen extends CasinoScreen {
    private static final int W = 300;
    private static final int H = 250;
    private static final int CARD_W = 140;
    private static final int CARD_H = 44;
    private static final int CARD_Y = 104;

    private final BlockPos pos;
    private int left;
    private int top;
    private boolean buy = true;
    private int kind = 0;
    private int amount = 1;

    private Button tabBuy;
    private Button tabSell;
    private Button minus10;
    private Button minus1;
    private Button plus1;
    private Button plus10;
    private Button maxBtn;
    private Button confirm;

    public CashierScreen(BlockPos pos) {
        super(Component.translatable("block.casino.cashier"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = (height - H) / 2;
        tabBuy = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.tab_buy"), b -> setMode(true))
                .bounds(left + 8, top + 52, CARD_W, 20).build());
        tabSell = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.tab_sell"), b -> setMode(false))
                .bounds(left + 8 + CARD_W + 4, top + 52, CARD_W, 20).build());

        int y = top + 166;
        int x = left + (W - 248) / 2;
        minus10 = addRenderableWidget(Button.builder(Component.literal("-10"), b -> change(-10)).bounds(x, y, 32, 20).build());
        minus1 = addRenderableWidget(Button.builder(Component.literal("-1"), b -> change(-1)).bounds(x + 36, y, 32, 20).build());
        plus1 = addRenderableWidget(Button.builder(Component.literal("+1"), b -> change(1)).bounds(x + 36 + 32 + 4 + 56 + 4, y, 32, 20).build());
        plus10 = addRenderableWidget(Button.builder(Component.literal("+10"), b -> change(10)).bounds(x + 36 + 32 + 4 + 56 + 4 + 36, y, 32, 20).build());
        maxBtn = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.max"), b -> amount = max())
                .bounds(x + 36 + 32 + 4 + 56 + 4 + 72, y, 44, 20).build());

        confirm = addRenderableWidget(Button.builder(Component.translatable("casino.cashier.confirm").withStyle(ChatFormatting.BOLD), b -> send())
                .bounds(left + 8, top + H - 28, 180, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose())
                .bounds(left + 8 + 180 + 4, top + H - 28, W - 16 - 184, 20).build());
        clamp();
    }

    private void setMode(boolean buyMode) {
        if (buy != buyMode) {
            buy = buyMode;
            amount = 1;
            clamp();
        }
    }

    private void change(int d) {
        amount += d;
        clamp();
    }

    private void send() {
        clamp();
        if (amount > 0) {
            CasinoNet.sendToServer(new CashierPacket(pos, kind, buy, amount));
        }
    }

    private static int count(Item item) {
        int n = 0;
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

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    /** Máximo que se puede cambiar ahora mismo con la opción elegida. */
    private int max() {
        return buy ? count(Cashier.CURRENCY[kind]) : balance() / Cashier.SELL[kind];
    }

    private void clamp() {
        int m = max();
        amount = m <= 0 ? 0 : Math.max(1, Math.min(amount, m));
        updateButtons();
    }

    private void updateButtons() {
        if (confirm == null) {
            return;
        }
        int m = max();
        minus10.active = amount > 1;
        minus1.active = amount > 1;
        plus1.active = amount < m;
        plus10.active = amount < m;
        maxBtn.active = amount < m;
        confirm.active = amount > 0;
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

    private int cardX(int i) {
        return left + 8 + i * (CARD_W + 4);
    }

    @Override
    protected boolean onClick(double mx, double my, int button) {
        if (button == 0) {
            for (int i = 0; i < 2; i++) {
                int cx = cardX(i);
                int cy = top + CARD_Y;
                if (mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H) {
                    if (kind != i) {
                        kind = i;
                        amount = 1;
                        clamp();
                    }
                    Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance
                            .forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }
        return superClick(mx, my, button);
    }

    private void frame(G g, int x0, int y0, int x1, int y1, int color) {
        g.fill(x0, y0, x1, y0 + 2, color);
        g.fill(x0, y1 - 2, x1, y1, color);
        g.fill(x0, y0, x0 + 2, y1, color);
        g.fill(x1 - 2, y0, x1, y1, color);
    }

    private void bigItem(G g, ItemStack s, int x, int y, float scale) {
        g.push();
        g.translate(x, y, 0);
        g.scale(scale, scale, 1);
        g.renderItem(s, 0, 0);
        g.pop();
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xF0140F1A);
        g.fill(left, top, left + W, top + 22, 0xFF3A2410);
        g.drawCenteredString(font, Component.translatable("casino.cashier.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, top + 7, 0xFFFFFF);

        // saldo
        int bal = balance();
        g.fill(left + 8, top + 27, left + W - 8, top + 47, 0xFF2A1F36);
        int x = left + 14;
        for (int i = Chips.VALUES.length - 1; i >= 0; i--) {
            g.renderItem(new ItemStack(Chips.itemFor(Chips.VALUES[i]).get()), x, top + 29);
            x += 16;
        }
        g.drawString(font, Component.translatable("casino.cashier.balance", bal).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x + 8, top + 33, 0xFFFFFF, true);

        // pestañas (la elegida queda marcada con un marco dorado)
        drawWidgets(g, mx, my, pt);
        Button sel = buy ? tabBuy : tabSell;
        frame(g, sel.getX() - 1, sel.getY() - 1, sel.getX() + sel.getWidth() + 1, sel.getY() + sel.getHeight() + 1, 0xFFFFD34D);
        g.drawCenteredString(font, Component.translatable(buy ? "casino.cashier.help_buy" : "casino.cashier.help_sell").withStyle(ChatFormatting.GRAY), left + W / 2, top + 78, 0xFFFFFF);

        // paso 1: qué cambiar
        g.drawString(font, Component.translatable("casino.cashier.step_item").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), left + 10, top + 92, 0xFFFFFF, true);
        for (int i = 0; i < 2; i++) {
            int cx = cardX(i);
            int cy = top + CARD_Y;
            boolean selected = i == kind;
            boolean hover = mx >= cx && mx < cx + CARD_W && my >= cy && my < cy + CARD_H;
            g.fill(cx, cy, cx + CARD_W, cy + CARD_H, selected ? 0xFF3C2F52 : (hover ? 0xFF2E2440 : 0xFF221A2E));
            frame(g, cx, cy, cx + CARD_W, cy + CARD_H, selected ? 0xFFFFD34D : 0xFF55456B);
            ItemStack icon = new ItemStack(Cashier.CURRENCY[i]);
            bigItem(g, icon, cx + 8, cy + 6, 2.0F);
            g.drawString(font, icon.getHoverName().copy().withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), cx + 44, cy + 7, 0xFFFFFF, true);
            Component rate = buy ? Component.translatable("casino.cashier.rate_buy", Cashier.BUY[i]) : Component.translatable("casino.cashier.rate_sell", Cashier.SELL[i]);
            g.drawString(font, rate.copy().withStyle(ChatFormatting.GOLD), cx + 44, cy + 19, 0xFFFFFF, true);
            Component extra;
            if (buy) {
                extra = Component.translatable("casino.cashier.you_have", count(Cashier.CURRENCY[i]));
            } else {
                extra = Component.translatable("casino.cashier.max").append(": " + bal / Cashier.SELL[i]);
            }
            g.drawString(font, extra.copy().withStyle(ChatFormatting.GRAY), cx + 44, cy + 31, 0xFFFFFF, false);
        }

        // paso 2: cantidad
        g.drawString(font, Component.translatable("casino.cashier.step_amount").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), left + 10, top + 154, 0xFFFFFF, true);
        int bx = left + (W - 248) / 2 + 36 + 32 + 4;
        g.fill(bx, top + 166, bx + 56, top + 186, 0xFF0A0710);
        frame(g, bx, top + 166, bx + 56, top + 186, 0xFF8A6A1E);
        g.drawCenteredString(font, Component.literal(String.valueOf(amount)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), bx + 28, top + 172, 0xFFFFFF);

        // resultado: lo que entregas y lo que recibes
        int ry = top + 192;
        g.fill(left + 8, ry, left + W - 8, ry + 26, 0xFF1D3326);
        frame(g, left + 8, ry, left + W - 8, ry + 26, 0xFF2E6B45);
        if (amount > 0) {
            ItemStack item = new ItemStack(Cashier.CURRENCY[kind]);
            ItemStack chip = new ItemStack(Chips.itemFor(buy ? 5 : 25).get());
            Component itemText = Component.literal(amount + " ").append(item.getHoverName());
            Component chipText = Component.translatable("casino.cashier.chips_n", buy ? amount * Cashier.BUY[kind] : amount * Cashier.SELL[kind]);
            ItemStack giveIcon = buy ? item : new ItemStack(Chips.itemFor(25).get());
            ItemStack getIcon = buy ? new ItemStack(Chips.itemFor(25).get()) : item;
            Component giveText = buy ? itemText : chipText;
            Component getText = buy ? chipText : itemText;
            g.drawString(font, Component.translatable("casino.cashier.give").withStyle(ChatFormatting.GRAY), left + 14, ry + 3, 0xFFFFFF, false);
            g.renderItem(giveIcon, left + 14, ry + 9);
            g.drawString(font, giveText.copy().withStyle(ChatFormatting.WHITE), left + 34, ry + 14, 0xFFFFFF, true);
            g.drawString(font, Component.translatable("casino.cashier.receive").withStyle(ChatFormatting.GRAY), left + W / 2 + 14, ry + 3, 0xFFFFFF, false);
            g.renderItem(getIcon, left + W / 2 + 14, ry + 9);
            g.drawString(font, getText.copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), left + W / 2 + 34, ry + 14, 0xFFFFFF, true);
            g.drawCenteredString(font, Component.literal("→").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, ry + 10, 0xFFFFFF);
        } else {
            Component msg = buy
                    ? Component.translatable("casino.cashier.nothing_buy", new ItemStack(Cashier.CURRENCY[kind]).getHoverName())
                    : Component.translatable("casino.cashier.nothing_sell", Cashier.SELL[kind]);
            g.drawCenteredString(font, msg.copy().withStyle(ChatFormatting.RED), left + W / 2, ry + 9, 0xFFFFFF);
        }
    }
}
