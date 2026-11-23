package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista con desplazamiento (rueda del mouse o arrastrando la barra). Cada fila se dibuja con {@link Row}; al hacer clic se elige.
 */
public class ListView<T> extends Widget {
    public interface Row<T> {
        void draw(G g, T item, int x, int y, int w, int h, boolean selected, boolean hovered, Theme t);
    }

    public final List<T> items = new ArrayList<>();
    /** Cuadro de información de cada fila al pasar el mouse (opcional). */
    public java.util.function.Function<T, InfoCard> cards;
    public final int rowH;
    public int selected = -1;
    private final Row<T> row;
    private final java.util.function.Consumer<T> onPick;
    private float scroll, scrollTarget;
    private boolean dragging;

    public ListView(int x, int y, int w, int h, int rowH, Row<T> row, java.util.function.Consumer<T> onPick) {
        super(x, y, w, h);
        this.rowH = rowH;
        this.row = row;
        this.onPick = onPick;
    }

    private float maxScroll() {
        return Math.max(0, items.size() * rowH - h);
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll += (scrollTarget - scroll) * 0.35F;
        Ui.inset(g, x, y, w, h, t);
        boolean bar = maxScroll() > 0;
        int iw = w - (bar ? 6 : 0);
        g.enableScissor(x + 1, y + 1, x + iw - 1, y + h - 1);
        int first = (int) (scroll / rowH);
        for (int i = first; i < items.size(); i++) {
            int ry = y + i * rowH - (int) scroll;
            if (ry > y + h) {
                break;
            }
            boolean hov = inside(mx, my) && mx < x + iw && my >= ry && my < ry + rowH;
            if (i % 2 == 1) {
                g.fill(x + 1, ry, x + iw - 1, ry + rowH, Ui.alpha(0xFFFFFFFF, 0.03F));
            }
            if (i == selected) {
                g.fill(x + 1, ry, x + iw - 1, ry + rowH, Ui.alpha(t.accent, 0.22F));
                g.fill(x + 1, ry, x + 3, ry + rowH, t.accent);
            } else if (hov) {
                g.fill(x + 1, ry, x + iw - 1, ry + rowH, Ui.alpha(0xFFFFFFFF, 0.07F));
            }
            row.draw(g, items.get(i), x + 4, ry, iw - 8, rowH, i == selected, hov, t);
        }
        g.disableScissor();
        if (bar) {
            float frac = h / (float) (items.size() * rowH);
            int bh = Math.max(12, (int) (h * frac));
            int by = y + (int) ((h - bh) * (scroll / maxScroll()));
            g.fill(x + w - 5, y + 1, x + w - 1, y + h - 1, Ui.alpha(0xFF000000, 0.3F));
            g.fill(x + w - 5, by, x + w - 1, by + bh, Ui.mix(t.button, t.accent, dragging ? 0.8F : 0.4F));
        }
    }

    @Override
    public boolean scroll(double mx, double my, double delta) {
        if (!inside(mx, my)) {
            return false;
        }
        scrollTarget -= (float) delta * rowH * 1.5F;
        return true;
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0 || !inside(mx, my)) {
            return false;
        }
        if (maxScroll() > 0 && mx >= x + w - 6) {
            dragging = true;
            drag(mx, my, button);
            return true;
        }
        int i = (int) ((my - y + scroll) / rowH);
        if (i >= 0 && i < items.size()) {
            selected = i;
            Ui.click(1.4F);
            if (onPick != null) {
                onPick.accept(items.get(i));
            }
        }
        return true;
    }

    @Override
    public boolean drag(double mx, double my, int button) {
        if (!dragging) {
            return false;
        }
        float f = (float) ((my - y) / h);
        scrollTarget = scroll = Math.max(0, Math.min(maxScroll(), f * maxScroll()));
        return true;
    }

    @Override
    public boolean release(double mx, double my, int button) {
        boolean d = dragging;
        dragging = false;
        return d;
    }

    private int rowAt(double mx, double my) {
        if (!inside(mx, my)) {
            return -1;
        }
        int i = (int) Math.floor((my - y + scroll) / rowH);
        return i >= 0 && i < items.size() ? i : -1;
    }

    @Override
    public InfoCard card(double mx, double my) {
        int i = rowAt(mx, my);
        return i >= 0 && cards != null ? cards.apply(items.get(i)) : null;
    }

    @Override
    public Object hoverKey(double mx, double my) {
        int i = rowAt(mx, my);
        return i >= 0 ? items.get(i) : this;
    }
}
