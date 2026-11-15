package com.casino.client;

import com.casino.net.CasinoNet;
import com.casino.net.WheelDialogPacket;
import com.casino.net.WheelSpinPacket;
import com.casino.plat.client.CasinoScreen;
import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;

import java.util.List;

/**
 * Plática con el anfitrión de la ruleta de la suerte, como en un juego de rol: un cuadro abajo de la pantalla (el mundo se sigue
 * viendo detrás), su nombre arriba y el texto que se va escribiendo. Si puedes girar, te pregunta y tú eliges "Sí" o "Ahora no";
 * si ya giraste hoy, te dice cuánto falta.
 */
public final class WheelDialogScreen extends CasinoScreen {
    private static final int BOX_W = 300;
    private static final int BOX_H = 92;
    private final WheelDialogPacket data;
    private final Component text;
    private int left;
    private int top;
    private float shown;
    private int age;
    private boolean buttonsAdded;

    public WheelDialogScreen(WheelDialogPacket data) {
        super(Component.translatable("casino.npc.wheel"));
        this.data = data;
        this.text = switch (data.mode) {
            case 0 -> data.prize.isEmpty() ? Component.translatable("casino.wheel.dialog.ask_plain")
                    : Component.translatable("casino.wheel.dialog.ask", Component.literal(data.prize).withStyle(ChatFormatting.GOLD));
            case 1 -> Component.translatable("casino.wheel.dialog.wait",
                    Component.literal(data.hours + "h " + String.format("%02d", data.minutes) + "m").withStyle(ChatFormatting.YELLOW));
            default -> Component.translatable("casino.wheel.dialog.busy");
        };
    }

    @Override
    protected void init() {
        left = (width - BOX_W) / 2;
        top = height - BOX_H - 34;
        buttonsAdded = false;
        if (shown >= length()) {
            addButtons();
        }
    }

    private int length() {
        return text.getString().length();
    }

    private void addButtons() {
        if (buttonsAdded) {
            return;
        }
        buttonsAdded = true;
        int by = top + BOX_H - 26;
        if (data.mode == 0) {
            addRenderableWidget(Button.builder(Component.translatable("casino.wheel.dialog.yes").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), b -> yes())
                    .bounds(left + BOX_W / 2 - 124, by, 120, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("casino.wheel.dialog.no"), b -> onClose())
                    .bounds(left + BOX_W / 2 + 4, by, 120, 20).build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("casino.wheel.dialog.ok"), b -> onClose())
                    .bounds(left + BOX_W / 2 - 60, by, 120, 20).build());
        }
    }

    private void yes() {
        CasinoNet.sendToServer(new WheelSpinPacket(data.entity));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        age++;
        var mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(data.entity);
        if (mc.player == null || e == null || mc.player.distanceToSqr(e) > 10 * 10) {
            onClose();
        }
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        // un clic con el texto a medias lo termina de escribir
        if (shown < length()) {
            shown = length();
            addButtons();
            return true;
        }
        return super.onClick(x, y, b);
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        float t = age + pt;
        if (shown < length()) {
            shown = Math.min(length(), shown + 0.9F);
            if (shown >= length()) {
                addButtons();
            }
        }
        // aparece subiendo un poco
        int rise = (int) (Math.max(0, 1 - t / 6F) * 18);
        int x0 = left, y0 = top + rise, x1 = left + BOX_W, y1 = top + BOX_H + rise;
        // sombra, filo dorado, fondo morado oscuro con degradado
        g.fill(x0 + 3, y0 + 4, x1 + 3, y1 + 4, 0x80000000);
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFFC9A227);
        g.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xFF2A1A05);
        g.fillGradient(x0, y0, x1, y1, 0xF0241238, 0xF0100818);
        // destellos en las esquinas
        int sp = (int) (t / 4) % 4;
        int[][] corners = {{x0 + 3, y0 + 3}, {x1 - 5, y0 + 3}, {x1 - 5, y1 - 5}, {x0 + 3, y1 - 5}};
        for (int i = 0; i < 4; i++) {
            g.fill(corners[i][0], corners[i][1], corners[i][0] + 2, corners[i][1] + 2, i == sp ? 0xFFFFF2A0 : 0xFF8A6A20);
        }
        // nombre en su pestaña
        Component name = Component.literal("★ ").append(Component.translatable("casino.npc.wheel")).withStyle(ChatFormatting.BOLD);
        int nw = font.width(name) + 16;
        g.fill(x0 + 8, y0 - 14, x0 + 8 + nw, y0 + 1, 0xFFC9A227);
        g.fill(x0 + 9, y0 - 13, x0 + 7 + nw, y0 + 1, 0xFF6A2A9A);
        g.drawString(font, name, x0 + 16, y0 - 10, 0xFFFFE680);
        // texto que se va escribiendo
        String full = text.getString();
        int n = (int) shown;
        List<FormattedCharSequence> lines = font.split(n >= full.length() ? text : Component.literal(full.substring(0, n)), BOX_W - 24);
        int ly = y0 + 10;
        for (int i = 0; i < lines.size() && i < 5; i++) {
            g.drawString(font, lines.get(i), x0 + 12, ly, 0xFFF4EEFF);
            ly += 11;
        }
        drawWidgets(g, mx, my, pt);
    }
}
