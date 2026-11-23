package com.axeliscore.ui.client;

import com.axeliscore.plat.client.G;
import com.axeliscore.ui.InfoCard;
import com.axeliscore.ui.Ui;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * (Cliente) Dibuja los {@link InfoCard}: borde del color de la rareza, encabezado con ícono, título y etiqueta, y las partes (texto,
 * barras, pares, estrellas, pistas). Se usa igual para lo que sale al pasar el ratón, al mirar algo en el mundo y dentro de pantallas.
 */
public final class Cards {
    private static final int PAD = 6, LINE = 10, BAR_W = 64;
    private static final int TEXT = 0xFFE8ECF6, DIM = 0xFF9AA3BC, BG_TOP = 0xF2161B28, BG_BOTTOM = 0xF20B0E16, OUTER = 0xFF04060B;
    /** Estrella de 7x7. */
    private static final String[] STAR = {"...#...", "..###..", "#######", ".#####.", "..###..", ".##.##.", "#.....#"};

    private Cards() {
    }

    /** Cómo queda armado un cuadro (ancho, alto y los renglones ya cortados). */
    public static final class Layout {
        public int w, h, headerH, textW;
        final List<List<String>> wrapped = new ArrayList<>();
    }

    private static Font font() {
        return Ui.font();
    }

    public static Layout measure(InfoCard c) {
        Font f = font();
        Layout l = new Layout();
        boolean hasIcon = !c.icon.isEmpty() || c.image != null;
        int iconW = hasIcon ? Math.max(20, c.imageSize + 4) + 5 : 0;
        int titleW = c.title == null ? 0 : f.width(c.title);
        Component tag = tag(c);
        int tagW = tag == null ? 0 : f.width(tag) + 10;
        int subW = c.subtitle == null ? 0 : f.width(c.subtitle);
        int head = iconW + Math.max(titleW + (tagW > 0 ? tagW + 6 : 0), subW);
        int inner = Math.max(96, head);
        for (InfoCard.Part p : c.parts) {
            switch (p.kind) {
                case BAR -> inner = Math.max(inner, f.width(p.a) + 6 + BAR_W + 6 + (p.valueText == null ? 0 : f.width(p.valueText)));
                case PAIR -> inner = Math.max(inner, f.width(p.a) + 14 + f.width(p.b));
                case STARS -> inner = Math.max(inner, (p.a == null ? 0 : f.width(p.a) + 6) + p.max * 9);
                default -> {
                }
            }
        }
        int maxInner = Math.max(inner, c.maxWidth - PAD * 2);
        // los textos se cortan al ancho máximo, pero el cuadro no crece más de lo que necesita
        int textMax = 0;
        for (InfoCard.Part p : c.parts) {
            if (p.kind == InfoCard.Kind.TEXT || p.kind == InfoCard.Kind.HINT) {
                List<String> lines = Ui.wrap(p.a.getString(), maxInner);
                l.wrapped.add(lines);
                for (String s : lines) {
                    textMax = Math.max(textMax, f.width(s));
                }
            } else {
                l.wrapped.add(null);
            }
        }
        inner = Math.min(maxInner, Math.max(inner, textMax));
        l.textW = inner;
        l.w = inner + PAD * 2;
        int hh = PAD;
        int titleBlock = (c.title == null ? 0 : LINE) + (c.subtitle == null ? 0 : LINE);
        hh += Math.max(titleBlock, hasIcon ? Math.max(20, c.imageSize + 4) : 0);
        l.headerH = hh;
        int y = hh + (c.parts.isEmpty() ? 0 : 5);
        for (int i = 0; i < c.parts.size(); i++) {
            InfoCard.Part p = c.parts.get(i);
            switch (p.kind) {
                case TEXT, HINT -> y += l.wrapped.get(i).size() * LINE + (p.kind == InfoCard.Kind.HINT ? 2 : 1);
                case BAR -> y += LINE + 2;
                case PAIR -> y += LINE;
                case STARS -> y += 10;
                case DIVIDER -> y += 6;
            }
        }
        l.h = y + PAD - 1;
        return l;
    }

    private static Component tag(InfoCard c) {
        if (c.tag != null) {
            return c.tag;
        }
        return c.rareza != null ? c.rareza.nombre() : null;
    }

    /** Dibuja el cuadro con la esquina de arriba a la izquierda en (x, y). {@code a}: opacidad (0..1). */
    public static void draw(G g, InfoCard c, int x, int y, float a) {
        draw(g, c, measure(c), x, y, a);
    }

    public static void draw(G g, InfoCard c, Layout l, int x, int y, float a) {
        if (a <= 0.01F) {
            return;
        }
        Font f = font();
        int w = l.w, h = l.h;
        int border = c.border() | 0xFF000000;
        int dark = c.rareza != null ? c.rareza.dark | 0xFF000000 : Ui.mix(border, 0xFF000000, 0.45F);
        // brillo suave por fuera, contorno oscuro, borde de color y fondo
        g.fill(x - 2, y - 1, x + w + 2, y + h + 1, Ui.alpha(border, a * 0.18F));
        g.fill(x - 1, y - 2, x + w + 1, y + h + 2, Ui.alpha(border, a * 0.18F));
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Ui.alpha(OUTER, a));
        g.fillGradient(x, y, x + w, y + h, Ui.alpha(border, a), Ui.alpha(dark, a));
        int top = Ui.mix(BG_TOP, Ui.alpha(dark, 1), 0.22F);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, Ui.alpha(top, a * 0.96F), Ui.alpha(BG_BOTTOM, a * 0.96F));
        // esquinas con un punto de color
        g.fill(x + 2, y + 2, x + 3, y + 3, Ui.alpha(border, a * 0.8F));
        g.fill(x + w - 3, y + 2, x + w - 2, y + 3, Ui.alpha(border, a * 0.8F));
        // encabezado
        int cx = x + PAD, cy = y + PAD;
        boolean hasIcon = !c.icon.isEmpty() || c.image != null;
        int tx = cx;
        if (hasIcon) {
            int box = Math.max(20, c.imageSize + 4);
            g.fill(cx, cy, cx + box, cy + box, Ui.alpha(0xFF000000, a * 0.45F));
            g.fillGradient(cx + 1, cy + 1, cx + box - 1, cy + box - 1, Ui.alpha(Ui.mix(dark, 0xFF000000, 0.55F), a),
                    Ui.alpha(Ui.mix(dark, 0xFF000000, 0.8F), a));
            g.fill(cx, cy, cx + box, cy + 1, Ui.alpha(border, a * 0.7F));
            g.fill(cx, cy + box - 1, cx + box, cy + box, Ui.alpha(dark, a * 0.7F));
            if (c.image != null) {
                int s = c.imageSize;
                g.blitTint(c.image, cx + (box - s) / 2, cy + (box - s) / 2, s, s, 0, 0, s, s, s, s, Ui.alpha(0xFFFFFFFF, a));
            } else if (a > 0.5F) {
                g.renderItem(c.icon, cx + (box - 16) / 2, cy + (box - 16) / 2);
            }
            tx = cx + box + 5;
        }
        int ty = cy + (hasIcon ? Math.max(0, (Math.max(20, c.imageSize + 4) - (c.subtitle == null ? 9 : 19)) / 2) : 0);
        if (c.title != null) {
            g.drawString(f, c.title, tx, ty, Ui.alpha(border, a), true);
            Component tag = tag(c);
            if (tag != null) {
                int tw = f.width(tag) + 8;
                int px = x + w - PAD - tw;
                g.fill(px, ty - 1, px + tw, ty + 9, Ui.alpha(Ui.mix(dark, 0xFF000000, 0.35F), a));
                g.fill(px, ty - 1, px + tw, ty, Ui.alpha(border, a * 0.6F));
                g.drawString(f, tag, px + 4, ty, Ui.alpha(border, a), false);
            }
            ty += LINE;
        }
        if (c.subtitle != null) {
            g.drawString(f, c.subtitle, tx, ty, Ui.alpha(DIM, a), false);
        }
        int yy = y + l.headerH;
        if (!c.parts.isEmpty()) {
            // línea que separa el encabezado (se desvanece a los lados)
            line(g, x + PAD, yy + 1, w - PAD * 2, border, a * 0.75F);
            yy += 5;
        }
        for (int i = 0; i < c.parts.size(); i++) {
            InfoCard.Part p = c.parts.get(i);
            switch (p.kind) {
                case TEXT, HINT -> {
                    int col = p.kind == InfoCard.Kind.HINT ? DIM : p.color != 0 ? p.color : TEXT;
                    if (p.kind == InfoCard.Kind.HINT) {
                        yy += 1;
                    }
                    for (String s : l.wrapped.get(i)) {
                        g.drawString(f, s, cx, yy, Ui.alpha(col, a), p.kind != InfoCard.Kind.HINT);
                        yy += LINE;
                    }
                    yy += 1;
                }
                case BAR -> {
                    g.drawString(f, p.a, cx, yy + 1, Ui.alpha(DIM, a), false);
                    int bx = x + w - PAD - BAR_W - (p.valueText == null ? 0 : f.width(p.valueText) + 5);
                    int col = p.color != 0 ? p.color : border;
                    g.fill(bx - 1, yy + 2, bx + BAR_W + 1, yy + 8, Ui.alpha(0xFF000000, a));
                    g.fill(bx, yy + 3, bx + BAR_W, yy + 7, Ui.alpha(0xFF262A36, a));
                    int fw = Math.round(BAR_W * Math.max(0, Math.min(1, p.value)));
                    if (fw > 0) {
                        g.fillGradient(bx, yy + 3, bx + fw, yy + 7, Ui.alpha(Ui.mix(col, 0xFFFFFFFF, 0.3F), a), Ui.alpha(col, a));
                        g.fill(bx, yy + 3, bx + fw, yy + 4, Ui.alpha(0xFFFFFFFF, a * 0.35F));
                    }
                    // marquitas cada cuarto
                    for (int k = 1; k < 4; k++) {
                        g.fill(bx + BAR_W * k / 4, yy + 3, bx + BAR_W * k / 4 + 1, yy + 7, Ui.alpha(0xFF000000, a * 0.35F));
                    }
                    if (p.valueText != null) {
                        g.drawString(f, p.valueText, x + w - PAD - f.width(p.valueText), yy + 1, Ui.alpha(TEXT, a), true);
                    }
                    yy += LINE + 2;
                }
                case PAIR -> {
                    g.drawString(f, p.a, cx, yy, Ui.alpha(DIM, a), false);
                    int vw = f.width(p.b);
                    int lx = cx + f.width(p.a) + 3, rx = x + w - PAD - vw - 3;
                    for (int d = lx; d < rx; d += 3) {
                        g.fill(d, yy + 7, d + 1, yy + 8, Ui.alpha(DIM, a * 0.45F));
                    }
                    g.drawString(f, p.b, x + w - PAD - vw, yy, Ui.alpha(TEXT, a), true);
                    yy += LINE;
                }
                case STARS -> {
                    int sx = cx;
                    if (p.a != null) {
                        g.drawString(f, p.a, cx, yy + 1, Ui.alpha(DIM, a), false);
                        sx = x + w - PAD - p.max * 9 + 2;
                    }
                    for (int s = 0; s < p.max; s++) {
                        star(g, sx + s * 9, yy + 1, s < p.n ? 0xFFFFD24A : 0xFF3A3F4E, s < p.n ? 0xFFB8860B : 0xFF262A36, a);
                    }
                    yy += 10;
                }
                case DIVIDER -> {
                    line(g, x + PAD, yy + 2, w - PAD * 2, DIM, a * 0.5F);
                    yy += 6;
                }
            }
        }
    }

    /** Línea horizontal que se desvanece en las puntas. */
    public static void line(G g, int x, int y, int w, int color, float a) {
        int seg = Math.max(1, w / 16);
        for (int i = 0; i < w; i += seg) {
            float t = (i + seg / 2F) / w;
            float k = 1 - Math.abs(t * 2 - 1);
            g.fill(x + i, y, Math.min(x + w, x + i + seg), y + 1, Ui.alpha(color | 0xFF000000, a * Math.min(1, k * 1.8F)));
        }
    }

    /** Estrella de 7x7 con su sombra. */
    public static void star(G g, int x, int y, int color, int shadow, float a) {
        for (int r = 0; r < STAR.length; r++) {
            for (int q = 0; q < 7; q++) {
                if (STAR[r].charAt(q) == '#') {
                    g.fill(x + q + 1, y + r + 1, x + q + 2, y + r + 2, Ui.alpha(0xFF000000, a * 0.5F));
                }
            }
        }
        for (int r = 0; r < STAR.length; r++) {
            for (int q = 0; q < 7; q++) {
                if (STAR[r].charAt(q) == '#') {
                    g.fill(x + q, y + r, x + q + 1, y + r + 1, Ui.alpha(r >= 4 ? shadow : color, a));
                }
            }
        }
    }

    /**
     * Dibuja el cuadro junto al ratón (a la derecha, o a la izquierda si no cabe), siempre dentro de la pantalla, encima de todo.
     * {@code age}: segundos que lleva el ratón ahí (para aparecer suave).
     */
    public static void drawAtMouse(G g, InfoCard c, int mx, int my, int sw, int sh, float age) {
        Layout l = measure(c);
        int x = mx + 12, y = my - 12;
        if (x + l.w > sw - 4) {
            x = mx - 12 - l.w;
        }
        x = Math.max(4, x);
        y = Math.max(4, Math.min(y, sh - l.h - 4));
        float a = Ui.ease(Math.min(1, age / 0.12F));
        g.push();
        g.top();
        g.translate(0, (1 - a) * 3, 0);
        draw(g, c, l, x, y, a);
        g.pop();
    }
}
