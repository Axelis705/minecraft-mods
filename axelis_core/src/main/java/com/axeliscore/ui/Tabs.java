package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.function.IntConsumer;

/** Pestañas en fila; la activa queda levantada y unida al panel de abajo. */
public class Tabs extends Widget {
    public final String[] names;
    public int active;
    private final IntConsumer onChange;
    private final float[] hov;

    public Tabs(int x, int y, int w, int h, IntConsumer onChange, String... names) {
        super(x, y, w, h);
        this.names = names;
        this.onChange = onChange;
        this.hov = new float[names.length];
    }

    private int tabAt(double mx) {
        int n = names.length, tw = w / n;
        int i = (int) ((mx - x) / tw);
        return i >= 0 && i < n ? i : -1;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        int n = names.length, tw = w / n;
        int over = inside(mx, my) ? tabAt(mx) : -1;
        for (int i = 0; i < n; i++) {
            hov[i] += ((i == over ? 1 : 0) - hov[i]) * 0.3F;
            int tx = x + i * tw;
            boolean on = i == active;
            int top = on ? y : y + 2;
            int col = on ? t.bgTop : Ui.mix(t.button, t.buttonHover, hov[i]);
            g.fill(tx + 1, top, tx + tw - 1, y + h, t.border);
            g.fillGradient(tx + 2, top + 1, tx + tw - 2, y + h, Ui.mix(col, 0xFFFFFFFF, 0.08F), col);
            if (on) {
                g.fill(tx + 3, top + 1, tx + tw - 3, top + 2, t.accent);
            }
            var f = Ui.font();
            String s = names[i];
            g.drawString(f, s, tx + (tw - f.width(s)) / 2, top + (h - (top - y) - 8) / 2 + 1, on ? t.title : t.textDim, true);
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button != 0 || !inside(mx, my)) {
            return false;
        }
        int i = tabAt(mx);
        if (i >= 0 && i != active) {
            active = i;
            Ui.click(1.2F);
            if (onChange != null) {
                onChange.accept(i);
            }
        }
        return true;
    }
}
