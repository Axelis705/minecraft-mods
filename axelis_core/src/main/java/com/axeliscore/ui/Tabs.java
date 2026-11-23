package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.function.IntConsumer;

/** Pestañas en fila; la activa queda levantada y unida al panel de abajo. */
public class Tabs extends Widget {
    public final String[] names;
    public int active;
    private final IntConsumer onChange;
    private final float[] hov;
    private float sel = -1;
    private Theme base, activa;

    private Theme activa(Theme t) {
        if (base != t) {
            base = t;
            activa = Theme.of(t.bgTop | 0xFF000000, t.accent);
        }
        return activa;
    }

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
        if (sel < 0) {
            sel = active;
        }
        // la marca de la pestaña elegida se desliza de una a otra
        sel += (active - sel) * 0.25F;
        for (int i = 0; i < n; i++) {
            hov[i] += ((i == over ? 1 : 0) - hov[i]) * 0.3F;
            int tx = x + i * tw;
            boolean on = i == active;
            int top = on ? y - 1 : y + 2 - (int) (hov[i] * 1.5F);
            if (on) {
                // la elegida: del color del panel, sin borde abajo (queda unida al contenido)
                Ui.boton(g, tx + 1, top, tw - 2, y + h - top + 2, 0, false, true, activa(t));
                g.fill(tx + 2, y + h, tx + tw - 2, y + h + 2, Ui.mix(t.bgTop | 0xFF000000, 0xFFFFFFFF, 0.06F));
            } else {
                Ui.boton(g, tx + 1, top, tw - 2, y + h - top, hov[i], false, true, t);
            }
            var f = Ui.font();
            String s = Ui.fit(names[i], tw - 6);
            g.drawString(f, s, tx + (tw - f.width(s)) / 2, top + (y + h - top - 8) / 2 + 1, on ? t.title : Ui.mix(t.textDim, 0xFFFFFFFF, hov[i] * 0.5F),
                    true);
        }
        int lx = x + (int) (sel * tw) + 4;
        g.fill(lx, y + h - 2, lx + tw - 8, y + h - 1, t.accent);
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
