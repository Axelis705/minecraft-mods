package com.mundosdeaxelis.client;

import com.mundosdeaxelis.network.ConfirmadoPacket;
import com.mundosdeaxelis.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Etapa 282: «¿Seguro?» antes de algo que no tiene vuelta atrás (salir del volcán con la Piedra de regreso). El botón Confirmar
 * se llena durante 3 segundos y solo entonces se puede pulsar; Cancelar (o Esc) cierra sin hacer nada.
 */
public class ConfirmarScreen extends Screen {
    private static final int W = 230;
    private static final int H = 118;

    private final String kind;
    private final BlockPos pos;
    private final int ticks;
    private final long abierta = System.currentTimeMillis();
    private boolean listoSonado = false;
    private int hover = -1;

    public ConfirmarScreen(String kind, BlockPos pos, int ticks) {
        super(Component.translatable("confirmar.mundos_de_axelis." + kind + ".titulo"));
        this.kind = kind;
        this.pos = pos;
        this.ticks = ticks;
    }

    @Override
    protected void init() {
        UiFx.open();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int accent() {
        return switch (kind) {
            case "salida_volcan" -> 0xFF7A2E;
            default -> 0x7FC8FF;
        };
    }

    /** De 0 a 1: cuánto falta para poder confirmar. */
    private float llenado() {
        return Mth.clamp((System.currentTimeMillis() - abierta) / (ticks * 50.0F), 0.0F, 1.0F);
    }

    private boolean listo() {
        return llenado() >= 1.0F;
    }

    private int x0() {
        return (width - W) / 2;
    }

    private int y0() {
        return (height - H) / 2;
    }

    /** Botón 0 = Cancelar, 1 = Confirmar: {x, y, ancho, alto}. */
    private int[] boton(int i) {
        int bw = 96, bh = 20;
        int y = y0() + H - bh - 10;
        return i == 0 ? new int[]{x0() + 12, y, bw, bh} : new int[]{x0() + W - 12 - bw, y, bw, bh};
    }

    private int sobre(double mx, double my) {
        for (int i = 0; i < 2; i++) {
            int[] b = boton(i);
            if (mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int x = x0(), y = y0();
        int ac = accent();
        // marco con un brillo del color del sitio
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF000000 | (ac & 0xFEFEFE) >> 1);
        g.fillGradient(x, y, x + W, y + H, 0xF01A1410, 0xF00C0A08);
        g.fill(x, y, x + W, y + 2, 0xFF000000 | ac);
        // título
        g.drawCenteredString(font, title, x + W / 2, y + 10, 0xFF000000 | ac);
        // texto
        List<FormattedCharSequence> ls = font.split(Component.translatable("confirmar.mundos_de_axelis." + kind + ".texto"), W - 24);
        int ty = y + 26;
        for (FormattedCharSequence l : ls) {
            g.drawString(font, l, x + W / 2 - font.width(l) / 2, ty, 0xFFE8DDD0, false);
            ty += 10;
        }
        int h = sobre(mx, my);
        if (h != hover) {
            hover = h;
            if (h >= 0) {
                UiFx.hover();
            }
        }
        if (listo() && !listoSonado) {
            listoSonado = true;
            UiFx.success();
        }
        // Cancelar
        int[] b = boton(0);
        g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], hover == 0 ? 0xFF4A4038 : 0xFF2E2822);
        g.fill(b[0], b[1] + b[3] - 1, b[0] + b[2], b[1] + b[3], 0xFF000000);
        g.drawCenteredString(font, Component.translatable("gui.mundos_de_axelis.confirmar.no"), b[0] + b[2] / 2, b[1] + 6, 0xFFD8D0C8);
        // Confirmar: se va llenando
        b = boton(1);
        float f = llenado();
        g.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0xFF241A14);
        int fillW = Math.round(b[2] * f);
        int col = listo() ? (hover == 1 ? 0xFF000000 | brighter(ac) : 0xFF000000 | ac) : 0xFF000000 | ((ac & 0xFEFEFE) >> 1);
        g.fill(b[0], b[1], b[0] + fillW, b[1] + b[3], col);
        g.fill(b[0], b[1] + b[3] - 1, b[0] + b[2], b[1] + b[3], 0xFF000000);
        Component label;
        if (listo()) {
            label = Component.translatable("gui.mundos_de_axelis.confirmar.si");
        } else {
            int s = (int) Math.ceil((1.0F - f) * ticks / 20.0F);
            label = Component.translatable("gui.mundos_de_axelis.confirmar.espera", Math.max(1, s));
        }
        g.drawCenteredString(font, label, b[0] + b[2] / 2, b[1] + 6, listo() ? 0xFFFFFFFF : 0xFFB8A898);
        super.render(g, mx, my, pt);
    }

    private static int brighter(int c) {
        int r = Math.min(255, ((c >> 16) & 255) + 40), gg = Math.min(255, ((c >> 8) & 255) + 40), bb = Math.min(255, (c & 255) + 40);
        return r << 16 | gg << 8 | bb;
    }

    private void confirmar() {
        if (!listo()) {
            UiFx.error();
            return;
        }
        UiFx.click();
        ModNetwork.CHANNEL.sendToServer(new ConfirmadoPacket(kind, pos));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int h = sobre(mx, my);
        if (h == 0) {
            UiFx.click();
            onClose();
            return true;
        }
        if (h == 1) {
            confirmar();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            confirmar();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }
}
