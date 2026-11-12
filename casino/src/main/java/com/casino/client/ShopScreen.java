package com.casino.client;

import com.casino.chip.Chips;
import com.casino.diamond.PrizeShopBE;
import com.casino.diamond.ShopPacket;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Tienda de premios: tarjetas con el objeto y su precio; clic en "Comprar". El dueño (operador) ve además el modo edición: agregar
 * lo que tiene en la mano con un precio, quitar un premio o cambiar su precio, o volver a la lista inicial.
 */
public final class ShopScreen extends CasinoScreen {
    private static final int COLS = 4;
    private static final int CW = 96;
    private static final int CH = 62;
    private static ShopScreen current;

    private ShopPacket.Open data;
    private boolean editing;
    private int page;
    private int left;
    private int top;
    private int w;
    private int h;
    private EditBox priceBox;
    private final List<Button> cardButtons = new ArrayList<>();

    private ShopScreen(ShopPacket.Open data) {
        super(Component.translatable("casino.shop.title"));
        this.data = data;
    }

    /** Abre la tienda, o si ya está abierta (después de editar) solo cambia la lista. */
    public static void show(ShopPacket.Open m) {
        if (current != null && current.data.pos.equals(m.pos)) {
            current.data = m;
            current.rebuild();
            return;
        }
        current = new ShopScreen(m);
        ClientHooks.open(current);
    }

    private int perPage() {
        return COLS * 2;
    }

    private void rebuild() {
        clearWidgets();
        init();
    }

    @Override
    protected void init() {
        cardButtons.clear();
        w = COLS * (CW + 6) + 14;
        h = 34 + 2 * (CH + 6) + 30 + (data.edit ? 30 : 0);
        left = (width - w) / 2;
        top = Math.max(4, (height - h) / 2);
        int pages = Math.max(1, (data.offers.size() + perPage() - 1) / perPage());
        page = Math.min(page, pages - 1);
        for (int k = 0; k < perPage(); k++) {
            int i = page * perPage() + k;
            if (i >= data.offers.size()) {
                break;
            }
            int cx = left + 8 + (k % COLS) * (CW + 6);
            int cy = top + 30 + (k / COLS) * (CH + 6);
            int idx = i;
            if (editing) {
                addRenderableWidget(Button.builder(Component.literal("−"), b -> send(new ShopPacket.Edit(data.pos, 2, idx, price(idx) - step(idx))))
                        .bounds(cx + 2, cy + CH - 20, 18, 18).build());
                addRenderableWidget(Button.builder(Component.literal("+"), b -> send(new ShopPacket.Edit(data.pos, 2, idx, price(idx) + step(idx))))
                        .bounds(cx + 22, cy + CH - 20, 18, 18).build());
                addRenderableWidget(Button.builder(Component.translatable("casino.shop.remove").withStyle(ChatFormatting.RED),
                        b -> send(new ShopPacket.Edit(data.pos, 1, idx, 1))).bounds(cx + 42, cy + CH - 20, CW - 44, 18).build());
            } else {
                Button b = Button.builder(Component.translatable("casino.shop.buy"), bt -> send(new ShopPacket.Buy(data.pos, idx)))
                        .bounds(cx + 4, cy + CH - 20, CW - 8, 18).build();
                cardButtons.add(b);
                addRenderableWidget(b);
            }
        }
        int by = top + h - 26;
        if (pages > 1) {
            addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                page = (page + pages - 1) % pages;
                rebuild();
            }).bounds(left + 8, by, 20, 20).build());
            addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                page = (page + 1) % pages;
                rebuild();
            }).bounds(left + 32, by, 20, 20).build());
        }
        if (data.edit) {
            addRenderableWidget(Button.builder(Component.translatable(editing ? "casino.shop.done_edit" : "casino.shop.edit")
                    .withStyle(ChatFormatting.AQUA), b -> {
                editing = !editing;
                rebuild();
            }).bounds(left + w - 8 - 180, by, 90, 20).build());
            if (editing) {
                int ey = top + h - 56;
                priceBox = new EditBox(font, left + 8, ey, 80, 20, Component.translatable("casino.shop.price"));
                priceBox.setValue("100");
                priceBox.setMaxLength(8);
                priceBox.setResponder(s -> {
                    String d = s.replaceAll("[^0-9]", "");
                    if (!d.equals(s)) {
                        priceBox.setValue(d);
                    }
                });
                addRenderableWidget(priceBox);
                addRenderableWidget(Button.builder(Component.translatable("casino.shop.add_held"), b -> {
                    int p = 1;
                    try {
                        p = Integer.parseInt(priceBox.getValue());
                    } catch (NumberFormatException ignored) {
                    }
                    send(new ShopPacket.Edit(data.pos, 0, 0, p));
                }).bounds(left + 92, ey, 170, 20).build());
                addRenderableWidget(Button.builder(Component.translatable("casino.shop.defaults"), b -> send(new ShopPacket.Edit(data.pos, 3, 0, 1)))
                        .bounds(left + 266, ey, w - 274, 20).build());
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose())
                .bounds(left + w - 8 - 86, by, 86, 20).build());
    }

    private int price(int i) {
        return data.offers.get(i).price();
    }

    /** Cuánto sube o baja el precio con + y − (según su tamaño). */
    private int step(int i) {
        int p = price(i);
        return p >= 10000 ? 1000 : p >= 1000 ? 100 : p >= 100 ? 10 : 1;
    }

    private void send(Object pkt) {
        CasinoNet.sendToServer(pkt);
    }

    @Override
    public void removed() {
        if (current == this) {
            current = null;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(data.pos.getX() + 0.5, data.pos.getY() + 0.5, data.pos.getZ() + 0.5) > 64) {
            onClose();
            return;
        }
        int bal = Chips.balance(p);
        for (int k = 0; k < cardButtons.size(); k++) {
            int i = page * perPage() + k;
            if (i < data.offers.size()) {
                cardButtons.get(k).active = bal >= data.offers.get(i).price();
            }
        }
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + w + 2, top + h + 2, 0xFFC8A040);
        g.fill(left, top, left + w, top + h, 0xF012101A);
        g.fill(left, top, left + w, top + 22, 0xFF2A1A30);
        g.drawString(font, Component.translatable("casino.shop.title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), left + 8, top + 7, 0xFFFFFF, true);
        var pl = Minecraft.getInstance().player;
        Component bal = Component.translatable("casino.cashier.balance", pl == null ? 0 : Chips.balance(pl)).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + w - 8 - font.width(bal), top + 7, 0xFFFFFF, true);
        if (data.offers.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("casino.shop.empty").withStyle(ChatFormatting.GRAY), left + w / 2, top + 70, 0xFFFFFF);
        }
        ItemStack hover = ItemStack.EMPTY;
        for (int k = 0; k < perPage(); k++) {
            int i = page * perPage() + k;
            if (i >= data.offers.size()) {
                break;
            }
            PrizeShopBE.Offer o = data.offers.get(i);
            int cx = left + 8 + (k % COLS) * (CW + 6);
            int cy = top + 30 + (k / COLS) * (CH + 6);
            g.fill(cx, cy, cx + CW, cy + CH, 0xFF1E1A28);
            g.fill(cx, cy, cx + CW, cy + 1, 0xFF6A5A30);
            g.push();
            g.translate(cx + 6, cy + 6, 0);
            g.scale(1.5F, 1.5F, 1.0F);
            g.renderItem(o.item(), 0, 0);
            g.renderItemDecorations(font, o.item(), 0, 0);
            g.pop();
            String name = font.plainSubstrByWidth(o.item().getHoverName().getString(), CW - 36);
            g.drawString(font, name, cx + 32, cy + 8, 0xFFE8E0F0, false);
            g.drawString(font, Component.translatable("casino.shop.price_n", String.format("%,d", o.price())).withStyle(ChatFormatting.GOLD),
                    cx + 32, cy + 22, 0xFFFFFF, false);
            if (mx >= cx + 6 && mx < cx + 30 && my >= cy + 6 && my < cy + 30) {
                hover = o.item();
            }
        }
        drawWidgets(g, mx, my, pt);
        if (!hover.isEmpty()) {
            g.renderTooltip(font, net.minecraft.world.item.ItemStack.EMPTY == hover ? List.of() : List.of(hover.getHoverName()), mx, my);
        }
    }
}
