package com.mundosdeaxelis.client;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Piezas comunes para dibujar las pantallas del mod (menú de equipo, Mesa del Artífice): marco con adornos en las esquinas,
 * ranuras con relieve y rótulos de sección. Así todas las pantallas se ven con el mismo estilo cuidado.
 */
public final class PanelUi {
    private PanelUi() {
    }

    private static int a(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** Marco de una ventana: borde doble, degradado, brillo suave arriba y rombos en las cuatro esquinas. */
    public static void frame(GuiGraphics g, int x, int y, int w, int h, int accent) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF05030C);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, a(0xFF, accent));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF120A22);
        g.fillGradient(x, y, x + w, y + h, 0xFF2E1F52, 0xFF130B25);
        // brillo de la parte de arriba y línea fina interior
        g.fillGradient(x, y, x + w, y + 22, a(0x40, accent), a(0x00, accent));
        g.fill(x + 2, y + 2, x + w - 2, y + 3, a(0x30, accent));
        g.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, a(0x30, accent));
        g.fill(x + 2, y + 2, x + 3, y + h - 2, a(0x30, accent));
        g.fill(x + w - 3, y + 2, x + w - 2, y + h - 2, a(0x30, accent));
        // adornos en las esquinas
        diamond(g, x - 1, y - 1, accent);
        diamond(g, x + w + 1, y - 1, accent);
        diamond(g, x - 1, y + h + 1, accent);
        diamond(g, x + w + 1, y + h + 1, accent);
        animate(g, x, y, w, h, accent);
    }

    /**
     * Vida del marco: un destello que recorre el borde, luces que suben despacio por dentro del panel y un latido
     * suave en el brillo de las esquinas. Solo se dibuja con rectángulos; no cuesta nada de rendimiento.
     */
    private static void animate(GuiGraphics g, int x, int y, int w, int h, int accent) {
        double t = UiFx.time();
        // destello recorriendo el perímetro
        int per = 2 * (w + h);
        int head = (int) ((t * 70.0D) % per);
        for (int i = 0; i < 26; i++) {
            int d = (head - i + per) % per;
            int al = 0xD0 - i * 8;
            if (al <= 0) {
                break;
            }
            int px, py;
            if (d < w) {
                px = x + d;
                py = y - 2;
            } else if (d < w + h) {
                px = x + w + 1;
                py = y + (d - w);
            } else if (d < 2 * w + h) {
                px = x + w - (d - w - h);
                py = y + h + 1;
            } else {
                px = x - 2;
                py = y + h - (d - 2 * w - h);
            }
            g.fill(px, py, px + 1, py + 1, a(Math.max(0, al), 0xFFFFFF));
        }
        // luces que suben por el panel
        for (int i = 0; i < 12; i++) {
            double ph = (t * (0.05D + 0.02D * (i % 4)) + i * 0.137D) % 1.0D;
            int mx = x + 6 + (int) (((i * 53) % 97) / 97.0D * (w - 12)) + (int) (Math.sin(t * 0.8D + i) * 3.0D);
            int my = y + h - 4 - (int) (ph * (h - 8));
            int al = (int) (Math.sin(ph * Math.PI) * 0x46);
            g.fill(mx, my, mx + 1, my + 1, a(al, accent | 0x404040));
            if (i % 3 == 0) {
                g.fill(mx - 1, my, mx + 2, my + 1, a(al / 3, accent | 0x404040));
                g.fill(mx, my - 1, mx + 1, my + 2, a(al / 3, accent | 0x404040));
            }
        }
        // latido en el centro de los rombos
        int pulse = (int) (0x60 + 0x50 * Math.sin(t * 2.4D));
        glow(g, x - 1, y - 1, accent, pulse);
        glow(g, x + w + 1, y - 1, accent, pulse);
        glow(g, x - 1, y + h + 1, accent, pulse);
        glow(g, x + w + 1, y + h + 1, accent, pulse);
    }

    private static void glow(GuiGraphics g, int cx, int cy, int accent, int al) {
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, a(al, accent));
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
    }

    /** Título de la ventana: texto con sombra de color y rombitos a los lados. */
    public static void title(GuiGraphics g, net.minecraft.client.gui.Font font, net.minecraft.network.chat.Component text, int x, int y, int accent) {
        g.drawString(font, text, x + 1, y + 1, a(0xFF, (accent & 0xFEFEFE) >> 1), false);
        g.drawString(font, text, x, y, 0xFFFFFFFF, false);
        int w = font.width(text);
        int cy = y + 4;
        g.fill(x + w + 4, cy - 1, x + w + 7, cy + 2, a(0xFF, accent));
        g.fill(x + w + 5, cy - 2, x + w + 6, cy + 3, a(0xFF, accent));
        g.fillGradient(x + w + 9, cy, x + w + 34, cy + 1, a(0xAA, accent), a(0x00, accent));
    }

    /** Pestaña: la activa lleva borde dorado y una luz pulsante debajo; con el cursor encima se ilumina. */
    public static void tab(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, int h, net.minecraft.network.chat.Component label,
                           boolean on, boolean over, int accent) {
        g.fill(x, y, x + w, y + h, on ? a(0xFF, accent) : over ? a(0xFF, (accent & 0xFEFEFE) >> 1) : 0xFF3B2B66);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h, on ? 0xFF4A3480 : over ? 0xFF3A2C72 : 0xFF241846, on ? 0xFF2C1D52 : 0xFF140C28);
        if (on) {
            int pulse = (int) (0xC0 + 0x3F * Math.sin(UiFx.time() * 3.0D));
            g.fill(x + 1, y + h - 2, x + w - 1, y + h, a(pulse, accent));
            g.fillGradient(x + 1, y + 1, x + w - 1, y + 8, a(0x30, 0xFFFFFF), a(0x00, 0xFFFFFF));
        } else if (over) {
            g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, a(0x20, accent), a(0x00, accent));
        }
        g.drawString(font, label, x + w / 2 - font.width(label) / 2, y + 4, on ? 0xFFFFFFFF : over ? 0xFFE8DCFF : 0xFFB8AAD8, false);
    }

    /** Botón: borde de color, brillo al pasar el cursor, un destello que lo cruza cuando está listo para usarse. */
    public static void button(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, int h, net.minecraft.network.chat.Component label,
                              boolean enabled, boolean over, int accent) {
        g.fill(x, y, x + w, y + h, enabled ? a(0xFF, over ? 0xFFFFFF : accent) : 0xFF3B2B66);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, enabled ? (over ? 0xFF6A4CAC : 0xFF4A3480) : 0xFF1E1438,
                enabled ? (over ? 0xFF4A3084 : 0xFF2C1D52) : 0xFF140C28);
        if (enabled) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, a(over ? 0x60 : 0x30, 0xFFFFFF));
            // destello que cruza el botón cada pocos segundos
            double t = UiFx.time() + (x * 0.013D + y * 0.007D);
            double ph = (t % 3.2D) / 0.7D;
            if (ph < 1.0D) {
                int sx = x + 1 + (int) (ph * (w - 2));
                g.fill(sx, y + 1, Math.min(sx + 2, x + w - 1), y + h - 1, a(over ? 0x50 : 0x30, 0xFFFFFF));
            }
        }
        g.drawString(font, label, x + w / 2 - font.width(label) / 2, y + (h - 8) / 2 + 1, enabled ? 0xFFFFFFFF : 0xFF6C5E8C, false);
    }

    private static void diamond(GuiGraphics g, int cx, int cy, int accent) {
        g.fill(cx - 3, cy - 1, cx + 4, cy + 2, 0xFF05030C);
        g.fill(cx - 1, cy - 3, cx + 2, cy + 4, 0xFF05030C);
        g.fill(cx - 2, cy, cx + 3, cy + 1, a(0xFF, accent));
        g.fill(cx, cy - 2, cx + 1, cy + 3, a(0xFF, accent));
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
    }

    /** Línea de separación con un rombo en el centro. */
    public static void divider(GuiGraphics g, int x, int y, int w, int accent) {
        int mid = x + w / 2;
        g.fillGradient(x, y, mid - 4, y + 1, a(0x00, accent), a(0xAA, accent));
        g.fillGradient(mid + 5, y, x + w, y + 1, a(0xAA, accent), a(0x00, accent));
        g.fill(mid - 1, y - 2, mid + 2, y + 3, a(0xFF, accent));
        g.fill(mid - 2, y - 1, mid + 3, y + 2, a(0xFF, accent));
        g.fill(mid, y - 1, mid + 1, y + 2, 0xFFFFFFFF);
    }

    /** Ranura con relieve (sombra arriba-izquierda, luz abajo-derecha). */
    public static void slot(GuiGraphics g, int x, int y, int accent, boolean locked) {
        int c = locked ? 0x55 : 0xFF;
        g.fill(x - 1, y - 1, x + 17, y + 17, a(c, accent));
        g.fill(x, y, x + 16, y + 16, locked ? 0xFF0A0712 : 0xFF0C0818);
        if (!locked) {
            g.fill(x, y, x + 16, y + 1, 0xFF04020A);
            g.fill(x, y, x + 1, y + 16, 0xFF04020A);
            g.fill(x + 1, y + 15, x + 16, y + 16, 0x30FFFFFF);
            g.fill(x + 15, y + 1, x + 16, y + 16, 0x30FFFFFF);
        } else {
            g.fill(x + 4, y + 8, x + 12, y + 14, 0xFF6C5E8C);
            g.fill(x + 5, y + 3, x + 6, y + 8, 0xFF6C5E8C);
            g.fill(x + 10, y + 3, x + 11, y + 8, 0xFF6C5E8C);
            g.fill(x + 5, y + 2, x + 11, y + 3, 0xFF6C5E8C);
        }
    }

    /** Ranura sencilla del inventario. */
    public static void plain(GuiGraphics g, int x, int y) {
        g.fill(x - 1, y - 1, x + 17, y + 17, 0xFF3B2B66);
        g.fill(x, y, x + 16, y + 16, 0xFF0C0818);
        g.fill(x, y, x + 16, y + 1, 0xFF04020A);
        g.fill(x, y, x + 1, y + 16, 0xFF04020A);
    }

    /** Placa de fondo para un rótulo de sección: una barra de color a la izquierda y un degradado suave. */
    public static void sectionBar(GuiGraphics g, int x, int y, int w, int accent) {
        g.fillGradient(x, y, x + w, y + 10, a(0x38, accent), a(0x00, accent));
        g.fill(x, y, x + 2, y + 10, a(0xFF, accent));
    }
}
