package com.celular.client;

import com.celular.plat.client.G;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;

/** Una app del celular. Dibuja dentro de la pantalla del celular ({@link PhoneScreen#dx0} ... {@link PhoneScreen#dy1}). */
abstract class App {
    final PhoneScreen s;
    final int icon;
    final String key;

    App(PhoneScreen s, int icon, String key) {
        this.s = s;
        this.icon = icon;
        this.key = key;
    }

    Font font() {
        return Minecraft.getInstance().font;
    }

    /** Color de fondo (para la animación al abrir). */
    int bg() {
        return s.bg();
    }

    /** ¿La barra de arriba (hora, batería) va con letras oscuras? */
    boolean lightStatus() {
        return !Prefs.dark;
    }

    void enter() {
    }

    /** Al salir de la app (quitar cajas de texto, etc.). */
    void leave() {
    }

    abstract void draw(G g, int mx, int my, float pt);

    boolean click(double x, double y, int b) {
        return false;
    }

    boolean release(double x, double y, int b) {
        return false;
    }

    boolean drag(double x, double y, double dx, double dy) {
        return false;
    }

    boolean scroll(double x, double y, double d) {
        return false;
    }

    boolean key(int key) {
        return false;
    }

    /** Atrás dentro de la app (true si se quedó en la app). */
    boolean back() {
        return false;
    }

    /** Encabezado con título y, si se pide, flecha para volver. Devuelve la y donde empieza el contenido. */
    int header(G g, String titleKey, boolean arrow, int mx, int my) {
        int y = s.cy0;
        g.fill(s.dx0, y, s.dx1, y + 18, s.bg());
        g.fill(s.dx0, y + 18, s.dx1, y + 19, s.line());
        int tx = s.dx0 + 6;
        if (arrow) {
            boolean h = s.in(mx, my, s.dx0 + 2, y + 2, s.dx0 + 16, y + 16);
            D.icon(g, D.BACK, s.dx0 + 3, y + 3, 12, h ? s.accent() : s.fg());
            tx = s.dx0 + 18;
        }
        g.drawString(font(), net.minecraft.network.chat.Component.translatable(titleKey).withStyle(net.minecraft.ChatFormatting.BOLD), tx, y + 5,
                s.fg(), false);
        return y + 19;
    }

    /** ¿Hicieron clic en la flecha del encabezado? */
    boolean headerArrow(double x, double y) {
        return s.in(x, y, s.dx0, s.cy0, s.dx0 + 18, s.cy0 + 18);
    }

    /** Un botón redondeado con texto; devuelve si el mouse está encima. */
    boolean button(G g, int x0, int y0, int x1, int y1, net.minecraft.network.chat.Component text, int color, int mx, int my) {
        boolean h = s.in(mx, my, x0, y0, x1, y1);
        D.rr(g, x0, y0, x1, y1, (y1 - y0) / 2, h ? D.mix(color, 0xFFFFFFFF, 0.15F) : color);
        Font f = font();
        g.drawString(f, text, (x0 + x1) / 2 - f.width(text) / 2, (y0 + y1) / 2 - 4, 0xFFFFFFFF, false);
        return h;
    }

    /** Ruedita de "cargando". */
    void spinner(G g, int cx, int cy, int color) {
        long t = System.currentTimeMillis();
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + t / 160.0;
            int x = cx + (int) Math.round(Math.cos(a) * 6), y = cy + (int) Math.round(Math.sin(a) * 6);
            g.fill(x - 1, y - 1, x + 1, y + 1, D.alpha(color, 0.25F + 0.75F * (i / 7F)));
        }
    }

    /** Texto centrado en varias líneas (para los mensajes de "no hay nada aquí"). */
    void centered(G g, net.minecraft.network.chat.Component text, int y, int color) {
        Font f = font();
        int w = s.dx1 - s.dx0 - 16;
        for (net.minecraft.util.FormattedCharSequence l : f.split(text, w)) {
            g.drawString(f, l, (s.dx0 + s.dx1) / 2 - f.width(l) / 2, y, color, false);
            y += 10;
        }
    }
}
