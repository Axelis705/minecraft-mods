package com.casino.client;

import com.casino.diamond.ElevatorGoPacket;
import com.casino.diamond.OpenElevatorPacket;
import com.casino.net.CasinoNet;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Botonera del elevador: un botón por piso (el más alto arriba). Los pisos sin la tarjeta necesaria salen con candado. */
public final class ElevatorScreen extends CasinoScreen {
    private static final int W = 220;
    private final OpenElevatorPacket data;
    private int left;
    private int top;
    private int h;

    public ElevatorScreen(OpenElevatorPacket data) {
        super(Component.translatable("casino.elevator.title"));
        this.data = data;
    }

    static Component floorName(String f, int y) {
        if (f.isEmpty()) {
            return Component.translatable("casino.elevator.floor_n", y);
        }
        return Component.translatableWithFallback("casino.elevator.floor." + f, f);
    }

    @Override
    protected void init() {
        int n = data.ys.length;
        h = 44 + n * 24 + 30;
        left = (width - W) / 2;
        top = Math.max(4, (height - h) / 2);
        for (int k = 0; k < n; k++) {
            int i = n - 1 - k; // el piso más alto arriba
            int y = data.ys[i];
            boolean here = y == data.pos.getY();
            boolean locked = !data.creative && data.access[i] > data.tier;
            MutableComponent label = Component.empty().append(floorName(data.floors[i], y));
            if (here) {
                label = Component.literal("▶ ").append(label).withStyle(ChatFormatting.YELLOW);
            } else if (locked) {
                label = Component.literal("🔒 ").append(label).append(Component.literal("  "))
                        .append(Component.translatable(data.access[i] >= 2 ? "casino.elevator.access_2" : "casino.elevator.access_1"))
                        .withStyle(ChatFormatting.GRAY);
            } else if (data.access[i] > 0) {
                label = label.withStyle(data.access[i] >= 2 ? ChatFormatting.GOLD : ChatFormatting.AQUA);
            }
            Button b = Button.builder(label, btn -> go(y)).bounds(left + 14, top + 40 + k * 24, W - 28, 20).build();
            b.active = !here;
            addRenderableWidget(b);
        }
        addRenderableWidget(Button.builder(Component.translatable("casino.cashier.close"), b -> onClose())
                .bounds(left + W / 2 - 40, top + h - 26, 80, 20).build());
    }

    private void go(int y) {
        CasinoNet.sendToServer(new ElevatorGoPacket(data.pos, y));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        var p = Minecraft.getInstance().player;
        if (p == null || p.distanceToSqr(data.pos.getX() + 0.5, data.pos.getY() + 0.5, data.pos.getZ() + 0.5) > 36) {
            onClose();
        }
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        drawBackground(g, mx, my, pt);
        g.fill(left - 2, top - 2, left + W + 2, top + h + 2, 0xFFC8A040);
        g.fill(left, top, left + W, top + h, 0xF0101014);
        g.fill(left, top, left + W, top + 24, 0xFF2A2010);
        g.drawCenteredString(font, Component.translatable("casino.elevator.title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), left + W / 2, top + 8, 0xFFFFFF);
        Component card = Component.translatable("casino.elevator.your_card", Component.translatable("casino.elevator.access_" + Math.min(2, data.tier)));
        g.drawCenteredString(font, card, left + W / 2, top + 28, 0xFFB0B0B0);
        drawWidgets(g, mx, my, pt);
    }
}
