package com.casino.client;

import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.chip.Chips;
import com.casino.game.ScratchCards;
import com.casino.net.CasinoNet;
import com.casino.net.ScratchBuyPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** La tienda de la máquina de rasca y gana: se eligen cuántos boletos comprar (1, 5 o 10); los boletos llegan al inventario. */
public final class ScratchShopScreen extends CasinoScreen {
    private static final int W = 270;
    private static final int H = 176;
    private static final int[] COUNTS = {1, 5, 10};

    private final BlockPos pos;
    private int left;
    private int top;
    private int cooldown;
    private final Button[] buttons = new Button[COUNTS.length];

    public ScratchShopScreen(BlockPos pos) {
        super(Component.translatable("block.casino.scratch_machine"));
        this.pos = pos;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        for (int i = 0; i < COUNTS.length; i++) {
            final int n = COUNTS[i];
            buttons[i] = addRenderableWidget(Button.builder(Component.translatable("casino.scratch.buy_btn", n, n * ScratchCards.PRICE), b -> buy(n))
                    .bounds(left + 12 + i * 82, top + 112, 78, 22).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 62, top + H - 28, 50, 20).build());
    }

    private void buy(int n) {
        if (cooldown > 0) {
            return;
        }
        CasinoNet.sendToServer(new ScratchBuyPacket(pos, n));
        cooldown = 5;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > 8 * 8) {
            onClose();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        int bal = Chips.balance(p);
        for (int i = 0; i < COUNTS.length; i++) {
            buttons[i].active = bal >= COUNTS[i] * ScratchCards.PRICE;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFFD9AE38);
        g.fill(left, top, left + W, top + H, 0xFF3A0810);
        g.fill(left, top, left + W, top + 18, 0xFF6E0F1C);
        Component title = Component.translatable("casino.scratch.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, title, left + (W - font.width(title)) / 2, top + 5, 0xFFFFFF, true);
        var p = Minecraft.getInstance().player;
        Component bal = Component.translatable("casino.cashier.balance", p == null ? 0 : Chips.balance(p)).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 24, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("casino.scratch.rules").withStyle(ChatFormatting.WHITE), left + 12, top + 24, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.scratch.price", ScratchCards.PRICE).withStyle(ChatFormatting.GRAY), left + 12, top + 36, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.scratch.chance", Math.round(ScratchCards.winChance() * 100)).withStyle(ChatFormatting.GRAY), left + 12, top + 48, 0xFFFFFF, false);
        for (int s = 0; s < ScratchCards.SYMBOLS; s++) {
            int cx = left + 12 + (s % 4) * 63;
            int cy = top + 62 + (s / 4) * 20;
            g.renderItem(ScratchSymbols.stack(s), cx, cy);
            g.drawString(font, Component.literal(String.valueOf(ScratchCards.PRIZES[s])).withStyle(ChatFormatting.WHITE), cx + 19, cy + 4, 0xFFFFFF, true);
        }
        g.drawString(font, Component.translatable("casino.scratch.shop_hint").withStyle(ChatFormatting.DARK_GRAY), left + 12, top + 140, 0xFFFFFF, false);
        drawWidgets(g, mx, my, pt);
    }
}
