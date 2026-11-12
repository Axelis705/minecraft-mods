package com.casino.client;

import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import com.casino.chip.Chips;
import com.casino.game.PlinkoLayout;
import com.casino.net.CasinoNet;
import com.casino.net.PlinkoPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Panel de apuestas del Plinko. Va pegado abajo y sin oscurecer la pantalla, para ver caer las bolas en la mesa mientras se apuesta.
 * Eliges apuesta y riesgo, ves lo que paga cada casillero y sueltas bolas (con clic, con Espacio o en automático).
 */
public final class PlinkoScreen extends CasinoScreen {
    private static final int W = 360;
    private static final int H = 104;
    private static final int[] RISK_COLORS = {0x45E6D0, 0xFFD23C, 0xFF4A4A};
    private static int lastBet = 1;
    private static int lastRisk = 1;

    private final BlockPos pos;
    private int left;
    private int top;
    private int betIdx = lastBet;
    private int risk = lastRisk;
    private boolean auto = false;
    private int cooldown = 0;
    private Button betMinus;
    private Button betPlus;
    private final Button[] riskBtn = new Button[PlinkoLayout.RISKS];
    private Button dropBtn;
    private Button autoBtn;

    public PlinkoScreen(BlockPos pos) {
        super(Component.translatable("block.casino.plinko"));
        this.pos = pos;
    }

    private int bet() {
        return PlinkoLayout.BETS[betIdx];
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = height - H - 30;
        int x0 = left + 10;
        betMinus = addRenderableWidget(Button.builder(Component.literal("-"), b -> changeBet(-1)).bounds(x0, top + 28, 22, 20).build());
        betPlus = addRenderableWidget(Button.builder(Component.literal("+"), b -> changeBet(1)).bounds(x0 + 96, top + 28, 22, 20).build());
        String[] keys = {"casino.plinko.risk_low", "casino.plinko.risk_mid", "casino.plinko.risk_high"};
        for (int i = 0; i < PlinkoLayout.RISKS; i++) {
            final int r = i;
            riskBtn[i] = addRenderableWidget(Button.builder(Component.translatable(keys[i]), b -> setRisk(r)).bounds(x0 + 132 + i * 74, top + 28, 70, 20).build());
        }
        dropBtn = addRenderableWidget(Button.builder(Component.translatable("casino.plinko.drop").withStyle(ChatFormatting.BOLD), b -> drop())
                .bounds(x0, top + 78, 160, 22).build());
        autoBtn = addRenderableWidget(Button.builder(autoLabel(), b -> {
            auto = !auto;
            b.setMessage(autoLabel());
        }).bounds(x0 + 166, top + 78, 90, 22).build());
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(x0 + 262, top + 78, 78, 22).build());
        updateButtons();
    }

    private Component autoLabel() {
        return Component.translatable(auto ? "casino.plinko.auto_on" : "casino.plinko.auto_off");
    }

    private void changeBet(int d) {
        betIdx = Math.max(0, Math.min(PlinkoLayout.BETS.length - 1, betIdx + d));
        lastBet = betIdx;
        updateButtons();
    }

    private void setRisk(int r) {
        risk = r;
        lastRisk = r;
    }

    private void updateButtons() {
        if (betMinus != null) {
            betMinus.active = betIdx > 0;
            betPlus.active = betIdx < PlinkoLayout.BETS.length - 1;
        }
    }

    private int balance() {
        var p = Minecraft.getInstance().player;
        return p == null ? 0 : Chips.balance(p);
    }

    private void drop() {
        if (balance() >= bet()) {
            CasinoNet.sendToServer(new PlinkoPacket(pos, bet(), risk));
            cooldown = 5;
        }
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5) > 12 * 12) {
            onClose();
            return;
        }
        if (cooldown > 0) {
            cooldown--;
        }
        if (auto && cooldown <= 0) {
            if (balance() >= bet()) {
                drop();
                cooldown = 14;
            } else {
                auto = false;
                autoBtn.setMessage(autoLabel());
            }
        }
        dropBtn.active = balance() >= bet();
    }

    @Override
    protected boolean onKey(int key, int scan, int mods) {
        if (key == 32 || key == 257 || key == 335) { // Espacio o Enter
            if (cooldown <= 0) {
                drop();
            }
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
        // sin renderBackground: el tablero tiene que verse
        g.fill(left - 2, top - 2, left + W + 2, top + H + 2, 0xFF8A6A1E);
        g.fill(left, top, left + W, top + H, 0xE0120C1A);
        g.fill(left, top, left + W, top + 16, 0xFF3A2410);
        g.drawString(font, Component.translatable("casino.plinko.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + 8, top + 4, 0xFFFFFF, true);
        Component bal = Component.translatable("casino.cashier.balance", balance()).withStyle(ChatFormatting.YELLOW);
        g.drawString(font, bal, left + W - 8 - font.width(bal), top + 4, 0xFFFFFF, true);

        // apuesta
        int x0 = left + 10;
        g.fill(x0 + 24, top + 28, x0 + 94, top + 48, 0xFF0A0710);
        frame(g, x0 + 24, top + 28, x0 + 94, top + 48, 0xFF8A6A1E);
        int chipValue = bet() >= 500 ? 500 : bet() >= 100 ? 100 : bet() >= 25 ? 25 : 5;
        g.renderItem(new ItemStack(Chips.itemFor(chipValue).get()), x0 + 27, top + 30);
        g.drawString(font, Component.literal(String.valueOf(bet())).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD), x0 + 47, top + 34, 0xFFFFFF, true);
        g.drawString(font, Component.translatable("casino.plinko.bet").withStyle(ChatFormatting.GRAY), x0, top + 19, 0xFFFFFF, false);
        g.drawString(font, Component.translatable("casino.plinko.risk").withStyle(ChatFormatting.GRAY), x0 + 132, top + 19, 0xFFFFFF, false);

        drawWidgets(g, mx, my, pt);

        // riesgo elegido: marco del color de la bola
        Button sel = riskBtn[risk];
        frame(g, sel.getX() - 1, sel.getY() - 1, sel.getX() + sel.getWidth() + 1, sel.getY() + sel.getHeight() + 1, 0xFF000000 | RISK_COLORS[risk]);
        g.fill(sel.getX() + 4, sel.getY() + sel.getHeight() + 2, sel.getX() + sel.getWidth() - 4, sel.getY() + sel.getHeight() + 4, 0xFF000000 | RISK_COLORS[risk]);

        // lo que paga cada casillero (con la apuesta actual)
        int sx = left + 10;
        int cw = 31;
        for (int b = 0; b < PlinkoLayout.BUCKETS; b++) {
            int m10 = PlinkoLayout.mult10(risk, b);
            int col = PlinkoLayout.colorFor(m10);
            int bx = sx + b * cw;
            g.fill(bx, top + 54, bx + cw - 2, top + 70, 0xFF000000 | (((col >> 16 & 255) * 4 / 10) << 16) | (((col >> 8 & 255) * 4 / 10) << 8) | ((col & 255) * 4 / 10));
            frame(g, bx, top + 54, bx + cw - 2, top + 70, 0xFF000000 | col);
            Component t = Component.literal("x" + PlinkoLayout.format(m10));
            g.drawString(font, t, bx + (cw - 2 - font.width(t)) / 2, top + 58, 0xFF000000 | col, false);
        }
    }
}
