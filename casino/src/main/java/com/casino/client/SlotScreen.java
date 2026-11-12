package com.casino.client;

import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.plat.Ids;
import com.casino.block.SlotBE;
import com.casino.chip.Chips;
import com.casino.game.SlotLayout;
import com.casino.net.CasinoNet;
import com.casino.net.SlotPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * Panel de apuestas de la tragamonedas. Va pegado abajo y sin oscurecer la pantalla, para ver girar los carretes mientras se apuesta.
 * Eliges apuesta, ves lo que paga cada símbolo y tiras de la palanca (con clic, con Espacio o en automático).
 */
public final class SlotScreen extends CasinoScreen {
    public static final ResourceLocation SYMBOLS = Ids.rl("casino", "textures/entity/slot_symbols.png");
    public static final int SYM = 32;
    private static final int W = 330;
    private static final int H = 116;
    private static int lastBet = 1;

    private final BlockPos pos;
    private int left;
    private int top;
    private int betIdx = lastBet;
    private boolean auto = false;
    private int cooldown = 0;
    private Button betMinus;
    private Button betPlus;
    private Button spinBtn;
    private Button autoBtn;

    public SlotScreen(BlockPos pos) {
        super(Component.translatable("block.casino.slot"));
        this.pos = pos;
    }

    private int bet() {
        return SlotLayout.BETS[betIdx];
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 30;
        int x0 = left + 10;
        betMinus = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBet(-1)).bounds(x0, top + 28, 22, 20).build());
        betPlus = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBet(1)).bounds(x0 + 96, top + 28, 22, 20).build());
        spinBtn = addRenderableWidget(Button.builder(Component.translatable("casino.slot.spin").withStyle(ChatFormatting.BOLD), b -> spin())
                .bounds(x0, top + 90, 150, 22).build());
        autoBtn = addRenderableWidget(Button.builder(autoLabel(), b -> {
            auto = !auto;
            b.setMessage(autoLabel());
        }).bounds(x0 + 156, top + 90, 90, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(x0 + 252, top + 90, 68, 22).build());
        updateButtons();
    }

    private Component autoLabel() {
        return Component.translatable(auto ? "casino.plinko.auto_on" : "casino.plinko.auto_off");
    }

    private void changeBet(int d) {
        betIdx = Math.max(0, Math.min(SlotLayout.BETS.length - 1, betIdx + d));
        lastBet = betIdx;
        updateButtons();
    }

    private void updateButtons() {
        if (betMinus != null) {
            betMinus.active = betIdx > 0;
            betPlus.active = betIdx < SlotLayout.BETS.length - 1;
        }
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private boolean machineBusy() {
        var lvl = Minecraft.getInstance().level;
        return lvl != null && lvl.getBlockEntity(pos) instanceof SlotBE be && be.spinning;
    }

    private void spin() {
        if (balance() >= bet() && !machineBusy() && cooldown <= 0) {
            CasinoNet.sendToServer(new SlotPacket(pos, bet()));
            cooldown = 6;
        }
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5) > 12 * 12) {
            onClose();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (auto && cooldown <= 0 && !machineBusy()) {
            if (balance() >= bet()) {
                spin();
                cooldown = 12;
            } else {
                auto = false;
                autoBtn.setMessage(autoLabel());
            }
        }
        spinBtn.active = balance() >= bet() && !machineBusy();
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 32 || key == 257 || key == 335) { // Espacio o Enter
            spin();
            return true;
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

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        // sin renderBackground: la máquina tiene que verse
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xE0120C1A);
        g.fill(left, top, left + W, top + 16, 0xFF3A2410);
        g.drawString(font, Component.translatable("casino.slot.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 4, 0xFFFFFF, true);
        Component bal = Component.translatable("casino.cashier.balance", balance()).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 4, 0xFFFFFF, true);

        int x0 = left + 10;
        g.fill(x0 + 24, top + 28, x0 + 94, top + 48, 0xFF0A0710);
        frame(g, x0 + 24, top + 28, x0 + 94, top + 48, 0xFF8A6A1E);
        int chipValue = bet() >= 500 ? 500 : bet() >= 100 ? 100 : bet() >= 25 ? 25 : 5;
        g.renderItem(new ItemStack(Chips.itemFor(chipValue).get()), x0 + 27, top + 30);
        g.drawString(font, Component.literal(String.valueOf(bet())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 47, top + 34, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("casino.plinko.bet").withStyle(ChatFormatting.GRAY), x0, top + 19, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.slot.per_line", bet() / SlotLayout.LINES).withStyle(ChatFormatting.GRAY), x0 + 132, top + 34, 0xFFFFFF, false);

        drawWidgets(g, mx, my, pt);

        // premio gordo actual (a la derecha de "Apuesta", sin tocar el saldo de la barra de arriba)
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.getBlockEntity(pos) instanceof SlotBE be) {
            Component jp = Component.translatable("casino.slot.jackpot_label", be.jackpot.getCount() + " x ", be.jackpot.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE);
            g.drawString(font, jp, left + W - 10 - font.width(jp), top + 19, 0xFFFFFF, true);
        }

        // tabla de pagos: lo que da cada símbolo (tres iguales en una línea) con la apuesta actual
        int per = bet() / SlotLayout.LINES;
        int[] order = {SlotLayout.CHERRY, SlotLayout.LEMON, SlotLayout.BELL, SlotLayout.GEM, SlotLayout.STAR, SlotLayout.SEVEN, SlotLayout.CROWN};
        int[] pay = {6, 12, 32, 80, 200, 800, 120};
        int cw = 44;
        for (int i = 0; i < order.length; i++) {
            int bx = x0 + i * cw;
            g.fill(bx, top + 52, bx + cw - 3, top + 84, 0xFF1C1428);
            frame(g, bx, top + 52, bx + cw - 3, top + 84, i == 6 ? 0xFF9A5ACF : 0xFF4A3A60);
            g.blit(SYMBOLS, bx + (cw - 3 - 18) / 2, top + 54, 18, 18, order[i] * SYM, 0, SYM, SYM, SYM * SlotLayout.SYMBOLS, SYM);
            Component t = Component.literal(pay[i] * per + (i == 6 ? "+" : ""));
            g.drawString(font, t, bx + (cw - 3 - font.width(t)) / 2, top + 74, i == 6 ? 0xFFD8A0FF : 0xFFFFD23C, false);
        }
    }
}
