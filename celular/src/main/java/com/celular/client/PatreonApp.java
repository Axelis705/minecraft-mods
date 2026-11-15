package com.celular.client;

import com.celular.net.CelularNet;
import com.celular.plat.Net;
import com.celular.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Patreon: los nombres de quienes apoyan el mod, con brillo dorado. */
final class PatreonApp extends App {
    private static final int CORAL = 0xFFFF424D;
    private final Scroll scroll = new Scroll();

    PatreonApp(PhoneScreen s) {
        super(s, D.APP_PATREON, "patreon");
    }

    @Override
    int bg() {
        return 0xFF141414;
    }

    @Override
    boolean lightStatus() {
        return false;
    }

    @Override
    void enter() {
        Net.toServer(new CelularNet.Simple(CelularNet.Simple.PATREON));
        scroll.reset();
    }

    @Override
    void draw(G g, int mx, int my, float pt) {
        Font f = font();
        long now = System.currentTimeMillis();
        g.fill(s.dx0, s.dy0, s.dx1, s.dy1, 0xFF141414);
        // portada
        int top = s.dy0;
        int bh = 64;
        g.fillGradient(s.dx0, top, s.dx1, top + bh, 0xFFFF424D, 0xFF8E1C3A);
        for (int i = 0; i < 6; i++) {
            double a = now / 2400.0 + i;
            int x = s.dx0 + (int) ((Math.sin(a * 0.7 + i * 2.1) * 0.5 + 0.5) * s.dw());
            int y = top + 14 + (int) ((Math.cos(a * 0.9 + i) * 0.5 + 0.5) * (bh - 20));
            D.circle(g, x, y, 6 + i % 3 * 3, 0x18FFFFFF);
        }
        int cx = (s.dx0 + s.dx1) / 2;
        D.app(g, D.APP_PATREON, cx - 12, top + 16, 24);
        Component title = Component.translatable("celular.patreon.title").withStyle(ChatFormatting.BOLD);
        g.drawString(f, title, cx - f.width(title) / 2, top + bh - 14, 0xFFFFFFFF, true);
        int y0 = top + bh + 4;
        int bottom = s.cy1;
        List<String> names = ClientHooks.patreon;
        Component thanks = Component.translatable("celular.patreon.thanks");
        int ty = y0;
        for (net.minecraft.util.FormattedCharSequence l : f.split(thanks, s.dw() - 14)) {
            g.drawString(f, l, cx - f.width(l) / 2, ty, 0xFFC8C8C8, false);
            ty += 10;
        }
        int listTop = ty + 4;
        if (names == null) {
            spinner(g, cx, listTop + 20, 0xFFFFFFFF);
            return;
        }
        int rowH = 24;
        scroll.update(names.size() * rowH + 30, bottom - listTop);
        g.scissor(s.dx0, listTop, s.dx1, bottom);
        int y = listTop - scroll.off();
        for (int i = 0; i < names.size(); i++, y += rowH) {
            D.rr(g, s.dx0 + 6, y, s.dx1 - 6, y + rowH - 4, 6, 0xFF222226);
            D.rrLine(g, s.dx0 + 6, y, s.dx1 - 6, y + rowH - 4, 6, 0xFF3A2E1A);
            D.icon(g, D.STAR, s.dx0 + 11, y + 4, 12, 0xFFFFC94A);
            // nombre con un brillo que pasa
            String n = D.cut(f, names.get(i), s.dw() - 40);
            int x = s.dx0 + 27;
            for (int k = 0; k < n.length(); k++) {
                String ch = n.substring(k, k + 1);
                double shine = Math.sin(now / 300.0 - (k + i * 3) * 0.5);
                int c = D.mix(0xFFE8B84A, 0xFFFFF4C8, (float) Math.max(0, shine) * 0.9F);
                g.drawString(f, Component.literal(ch).withStyle(ChatFormatting.BOLD), x, y + 6, c, false);
                x += f.width(Component.literal(ch).withStyle(ChatFormatting.BOLD));
            }
        }
        y += 4;
        Component more = Component.translatable("celular.patreon.more");
        for (net.minecraft.util.FormattedCharSequence l : f.split(more, s.dw() - 14)) {
            g.push();
            g.translate(cx - f.width(l) * 0.375F, y, 0);
            g.scale(0.75F, 0.75F, 1);
            g.drawString(f, l, 0, 0, 0xFF8A8A8A, false);
            g.pop();
            y += 8;
        }
        g.noScissor();
    }

    @Override
    boolean drag(double x, double y, double dx, double dy) {
        scroll.drag(dy);
        return true;
    }

    @Override
    boolean scroll(double x, double y, double d) {
        scroll.wheel(d);
        return true;
    }
}
