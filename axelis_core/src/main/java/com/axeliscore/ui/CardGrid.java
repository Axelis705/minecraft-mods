package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Cuadrícula de tarjetas con desplazamiento (colecciones, tiendas, inventarios especiales). Cada tarjeta se dibuja con {@link Cell}
 * (o con {@link #slot} para el estilo de siempre: ranura con borde de rareza, ícono y nombre); al pasar el mouse se levanta un poco y
 * muestra su cuadro de información.
 */
public class CardGrid<T> extends Widget {
    public interface Cell<T> {
        void draw(G g, T item, int x, int y, int w, int h, boolean hovered, boolean selected, Theme t);
    }

    public final List<T> items = new ArrayList<>();
    public final int cellW, cellH, gap;
    public int selected = -1;
    private final Cell<T> cell;
    private final Function<T, InfoCard> cards;
    private final Consumer<T> onPick;
    private float scroll, scrollTarget;
    private int hovered = -1;
    private final float[] lift = new float[512];

    public CardGrid(int x, int y, int w, int h, int cellW, int cellH, int gap, Cell<T> cell, Function<T, InfoCard> cards,
            Consumer<T> onPick) {
        super(x, y, w, h);
        this.cellW = cellW;
        this.cellH = cellH;
        this.gap = gap;
        this.cell = cell;
        this.cards = cards;
        this.onPick = onPick;
    }

    public int cols() {
        return Math.max(1, (w - 8 + gap) / (cellW + gap));
    }

    private int rows() {
        return (items.size() + cols() - 1) / cols();
    }

    private float maxScroll() {
        return Math.max(0, rows() * (cellH + gap) - gap + 8 - h);
    }

    private int ox() {
        int used = cols() * (cellW + gap) - gap;
        return x + (w - 6 - used) / 2;
    }

    private int at(double mx, double my) {
        if (!inside(mx, my)) {
            return -1;
        }
        int cx = (int) Math.floor((mx - ox()) / (cellW + gap));
        int cy = (int) Math.floor((my - y - 4 + scroll) / (cellH + gap));
        if (cx < 0 || cx >= cols() || cy < 0) {
            return -1;
        }
        double inX = (mx - ox()) - cx * (cellW + gap), inY = (my - y - 4 + scroll) - cy * (cellH + gap);
        if (inX >= cellW || inY >= cellH) {
            return -1;
        }
        int i = cy * cols() + cx;
        return i < items.size() ? i : -1;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        scrollTarget = Math.max(0, Math.min(maxScroll(), scrollTarget));
        scroll += (scrollTarget - scroll) * 0.35F;
        hovered = at(mx, my);
        Ui.inset(g, x, y, w, h, t);
        g.enableScissor(x + 1, y + 1, x + w - 1, y + h - 1);
        int cols = cols(), ox = ox();
        for (int i = 0; i < items.size(); i++) {
            int r = i / cols, c = i % cols;
            int cy = y + 4 + r * (cellH + gap) - Math.round(scroll);
            if (cy > y + h || cy + cellH < y) {
                continue;
            }
            int li = Math.min(i, lift.length - 1);
            lift[li] += ((i == hovered ? 1 : 0) - lift[li]) * 0.3F;
            int dy = -Math.round(lift[li] * 2);
            cell.draw(g, items.get(i), ox + c * (cellW + gap), cy + dy, cellW, cellH, i == hovered, i == selected, t);
        }
        g.disableScissor();
        if (maxScroll() > 0) {
            int bh = Math.max(14, (int) (h * h / (h + maxScroll())));
            int by = y + (int) ((h - bh) * (scroll / maxScroll()));
            g.fill(x + w - 5, y + 2, x + w - 2, y + h - 2, Ui.alpha(0xFF000000, 0.3F));
            g.fill(x + w - 5, by + 1, x + w - 2, by + bh - 1, Ui.alpha(t.accent, 0.8F));
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        int i = at(mx, my);
        if (button == 0 && i >= 0) {
            selected = i;
            Ui.click(1.15F);
            if (onPick != null) {
                onPick.accept(items.get(i));
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean scroll(double mx, double my, double delta) {
        if (!inside(mx, my)) {
            return false;
        }
        scrollTarget -= (float) delta * (cellH + gap) * 0.75F;
        return true;
    }

    @Override
    public InfoCard card(double mx, double my) {
        int i = at(mx, my);
        return i >= 0 && cards != null ? cards.apply(items.get(i)) : null;
    }

    @Override
    public Object hoverKey(double mx, double my) {
        int i = at(mx, my);
        return i >= 0 ? items.get(i) : this;
    }

    /**
     * Tarjeta de estilo común: ranura hundida con borde del color de la rareza (que brilla al pasar el mouse), el objeto al centro y
     * el nombre abajo (si cabe). {@code locked}: se ve oscura con un signo de pregunta.
     */
    public static void slot(G g, int x, int y, int w, int h, net.minecraft.world.item.ItemStack icon, String name, Rareza r, boolean locked,
            boolean hovered, boolean selected, Theme t) {
        int col = r != null ? r.light : t.accent;
        int dark = r != null ? r.dark : Ui.mix(t.accent, 0xFF000000, 0.5F);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Ui.alpha(0xFF000000, 0.6F));
        g.fillGradient(x, y, x + w, y + h, Ui.alpha(Ui.mix(dark, 0xFF000000, 0.6F), 0.95F), Ui.alpha(Ui.mix(dark, 0xFF000000, 0.85F), 0.95F));
        // borde de rareza (más fuerte al pasar el mouse o si está elegida)
        float k = selected ? 1F : hovered ? 0.85F : 0.45F;
        int b = Ui.alpha(col, k);
        g.fill(x, y, x + w, y + 1, b);
        g.fill(x, y + h - 1, x + w, y + h, Ui.alpha(dark, k));
        g.fill(x, y, x + 1, y + h, b);
        g.fill(x + w - 1, y, x + w, y + h, Ui.alpha(dark, k));
        if (hovered || selected) {
            g.fill(x + 1, y + 1, x + w - 1, y + 2, Ui.alpha(0xFFFFFFFF, 0.25F));
        }
        boolean label = name != null && h >= 30;
        int iy = y + (label ? (h - 10 - 16) / 2 : (h - 16) / 2);
        if (locked) {
            g.drawString(Ui.font(), "?", x + (w - Ui.font().width("?")) / 2, iy + 4, Ui.alpha(0xFFFFFFFF, 0.35F), false);
        } else if (icon != null && !icon.isEmpty()) {
            g.renderItem(icon, x + (w - 16) / 2, iy);
        }
        if (label) {
            String s = locked ? "???" : name;
            net.minecraft.client.gui.Font f = Ui.font();
            while (f.width(s) > w - 4 && s.length() > 3) {
                s = s.substring(0, s.length() - 2) + "…";
            }
            g.drawString(f, s, x + (w - f.width(s)) / 2, y + h - 11, locked ? 0xFF7A7F8C : col, false);
        }
    }
}
