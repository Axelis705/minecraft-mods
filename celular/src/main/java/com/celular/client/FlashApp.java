package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Linterna: un botón grande para encenderla. Con el celular en la mano, la luz sale hacia donde miras (también con la pantalla
 * cerrada) y tu brazo se estira hacia adelante para que los demás vean que alumbras.
 */
final class FlashApp extends App {
    /** Cuándo se tocó el botón (se hunde un poquito). */
    private long pressedAt;

    FlashApp(PhoneScreen s) {
        super(s, D.APP_FLASH, "flash");
    }

    @Override
    int bg() {
        return 0xFF0B0B0F;
    }

    @Override
    boolean lightStatus() {
        return false;
    }

    private int radius() {
        return Math.max(16, Math.min(28, s.dw() / 3));
    }

    private int centerY() {
        return (s.cy0 + s.cy1) / 2 - 16;
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, bg());
        boolean on = Flash.on;
        int cx = (s.dx0 + s.dx1) / 2, cy = centerY(), r = radius();
        float lit = Anim.ease("linterna", on ? 1F : 0F, 9F);
        if (lit > 0.01F) {
            // resplandor que crece al encender y respira un poquito
            float k = (float) (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 400.0));
            int reach = Math.round(22 * lit);
            for (int rr = r + reach; rr > r; rr -= 3) {
                D.circle(g, cx, cy, rr, D.alpha(0xFFFFF3B0, (0.05F + 0.03F * k) * lit));
            }
        }
        boolean h = s.in(mx, my, cx - r, cy - r, cx + r, cy + r);
        int offCol = h ? 0xFF3A3A3C : 0xFF2C2C2E;
        int press = Math.round((1 - Math.min(1F, (System.currentTimeMillis() - pressedAt) / 160F)) * 2);
        D.circle(g, cx, cy, r - press, D.mix(offCol, 0xFFFFFFFF, lit));
        int is = r;
        D.icon(g, D.FLASH, cx - is / 2, cy - is / 2, is, D.mix(0xFFFFFFFF, 0xFF1C1C1E, lit));
        int y = cy + r + 10;
        centered(g, Component.translatable(on ? "celular.flash.on" : "celular.flash.off").withStyle(ChatFormatting.BOLD), y,
                on ? 0xFFFFE066 : 0xFFE5E5EA);
        y += 16;
        boolean hand = Flash.inHand(Minecraft.getInstance());
        centered(g, Component.translatable(hand ? "celular.flash.hint" : "celular.flash.need_hand"), y, hand ? 0xFF8E8E93 : 0xFFFF9F0A);
    }

    @Override
    boolean click(double x, double y, int b) {
        int cx = (s.dx0 + s.dx1) / 2, cy = centerY(), r = radius();
        if (s.in(x, y, cx - r - 4, cy - r - 4, cx + r + 4, cy + r + 4)) {
            Flash.toggle();
            pressedAt = System.currentTimeMillis();
        }
        return true;
    }

    @Override
    boolean key(int key) {
        if (key == 32 || key == 257) {       // espacio o Enter
            Flash.toggle();
            return true;
        }
        return false;
    }
}
