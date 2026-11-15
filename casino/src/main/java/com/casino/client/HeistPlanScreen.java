package com.casino.client;

import com.casino.heist.HeistActionPacket;
import com.casino.heist.HeistPlanPacket;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * El plan del contacto: un pizarrón de corcho con el plano del golpe en 6 pasos, la lista del equipo (quien está cerca entra), las
 * reglas (tiempo en la bóveda, mochila, veto si te atrapan) y el botón para empezar. Si no se puede, dice por qué.
 */
public final class HeistPlanScreen extends CasinoScreen {
    private static final int W = 340;
    private static final int H = 224;
    private final HeistPlanPacket d;
    private int left;
    private int top;
    private int age;

    public HeistPlanScreen(HeistPlanPacket d) {
        super(Component.translatable("casino.heist.plan.title"));
        this.d = d;
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H) / 2);
        Button go = Button.builder(Component.translatable("casino.heist.plan.go").withStyle(ChatFormatting.BOLD), b -> {
            CasinoNet.sendToServer(new HeistActionPacket(HeistActionPacket.START, d.npc));
            onClose();
        }).bounds(left + W - 150, top + H - 28, 90, 20).build();
        go.active = d.reason == 0;
        addRenderableWidget(go);
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose()).bounds(left + W - 56, top + H - 28, 48, 20).build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        age++;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        int x0 = left, y0 = top, x1 = left + W, y1 = top + H;
        // marco de madera y corcho
        g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 4, 0x80000000);
        g.fill(x0 - 3, y0 - 3, x1 + 3, y1 + 3, 0xFF4A2A12);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF2A1608);
        g.fill(x0, y0, x1, y1, 0xFFB98A55);
        for (int i = 0; i < 140; i++) {
            int px = x0 + (int) ((i * 97L) % W), py = y0 + (int) ((i * 61L + i * i) % H);
            g.fill(px, py, px + 2, py + 1, i % 3 == 0 ? 0x30000000 : 0x18FFFFFF);
        }
        // título en un papel
        Component title = Component.translatable("casino.heist.plan.title").withStyle(ChatFormatting.BOLD);
        int tw = font.width(title) + 16;
        g.fill(x0 + 10, y0 + 8, x0 + 10 + tw, y0 + 24, 0xFFF4EED8);
        g.fill(x0 + 10 + tw / 2 - 2, y0 + 6, x0 + 10 + tw / 2 + 2, y0 + 10, 0xFFD02020);
        g.drawString(font, title, x0 + 18, y0 + 12, 0xFF2A1A10, false);

        // los 6 pasos, en notas con chinchetas y unidos por un hilo rojo
        int nx = x0 + 12, ny = y0 + 32;
        int[][] at = new int[6][];
        for (int i = 0; i < 6; i++) {
            int cx = nx + (i % 3) * 102, cy = ny + (i / 3) * 56;
            at[i] = new int[]{cx, cy};
        }
        for (int i = 0; i < 5; i++) {
            line(g, at[i][0] + 48, at[i][1] + 4, at[i + 1][0] + 48, at[i + 1][1] + 4, 0xFFC02020);
        }
        for (int i = 0; i < 6; i++) {
            int cx = at[i][0], cy = at[i][1];
            float tilt = (i % 2 == 0 ? -1 : 1);
            g.fill(cx + 2, cy + 2, cx + 98, cy + 50, 0x40000000);
            g.fill(cx, cy, cx + 96, cy + 48, i == 5 ? 0xFFF7E58A : 0xFFFBF7E8);
            g.fill(cx + 46, cy + 1, cx + 50, cy + 5, 0xFFD02020);
            g.drawString(font, Component.literal(String.valueOf(i + 1)).withStyle(ChatFormatting.BOLD), cx + 4, cy + 4, 0xFFB02020, false);
            List<FormattedCharSequence> lines = font.split(Component.translatable("casino.heist.plan.step_" + i), 88);
            for (int l = 0; l < Math.min(4, lines.size()); l++) {
                g.drawString(font, lines.get(l), cx + 4, cy + 14 + l * 9, 0xFF2A2A30, false);
            }
            if (tilt > 2) {
                break;
            }
        }
        // equipo y reglas
        int ry = y0 + 148;
        g.fill(x0 + 10, ry, x0 + 160, y1 - 8, 0xFFFBF7E8);
        g.drawString(font, Component.translatable("casino.heist.plan.crew").withStyle(ChatFormatting.BOLD), x0 + 14, ry + 4, 0xFF2A2A30, false);
        for (int i = 0; i < Math.min(5, d.crew.size()); i++) {
            g.drawString(font, (i == 0 ? "★ " : "• ") + d.crew.get(i), x0 + 16, ry + 15 + i * 9, 0xFF303040, false);
        }
        List<FormattedCharSequence> rules = font.split(Component.translatable("casino.heist.plan.rules"), W - 182);
        for (int l = 0; l < Math.min(4, rules.size()); l++) {
            g.drawString(font, rules.get(l), x0 + 168, ry + 2 + l * 9, 0xFF2A1A10, false);
        }
        if (d.reason != 0) {
            Component why = Component.translatable("casino.heist.plan.no_" + d.reason).withStyle(ChatFormatting.BOLD);
            g.drawString(font, font.plainSubstrByWidth(why.getString(), W - 180), x0 + 168, y1 - 44, 0xFFB01818, false);
        }
        drawWidgets(g, mx, my, pt);
    }

    private static void line(G g, int ax, int ay, int bx, int by, int col) {
        int n = Math.max(Math.abs(bx - ax), Math.abs(by - ay));
        for (int i = 0; i <= n; i += 1) {
            int x = ax + (bx - ax) * i / Math.max(1, n), y = ay + (by - ay) * i / Math.max(1, n);
            g.fill(x, y, x + 1, y + 1, col);
        }
    }
}
