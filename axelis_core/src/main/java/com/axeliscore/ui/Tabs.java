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

    private int[] bordes;
    private int bordesW = -1, bordesX;

    /**
     * Dónde empieza cada pestaña: del ancho de su nombre y lo que sobra repartido parejo (así "Tarjetas" no se corta si hay lugar); si
     * no caben todas, todas iguales.
     */
    private int[] bordes() {
        int n = names.length;
        if (bordes == null || bordesW != w || bordesX != x) {
            bordes = new int[n + 1];
            int[] nat = new int[n];
            int suma = 0;
            for (int i = 0; i < n; i++) {
                nat[i] = Ui.font().width(names[i]) + 10;
                suma += nat[i];
            }
            int acc = 0;
            for (int i = 0; i < n; i++) {
                bordes[i] = x + acc;
                acc += suma <= w ? nat[i] + (w - suma) * (i + 1) / n - (w - suma) * i / n : w * (i + 1) / n - w * i / n;
            }
            bordes[n] = x + w;
            bordesW = w;
            bordesX = x;
        }
        return bordes;
    }

    private int tabAt(double mx) {
        int[] b = bordes();
        for (int i = 0; i < names.length; i++) {
            if (mx >= b[i] && mx < b[i + 1]) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        int n = names.length;
        int[] b = bordes();
        int over = inside(mx, my) ? tabAt(mx) : -1;
        if (sel < 0) {
            sel = active;
        }
        // la marca de la pestaña elegida se desliza de una a otra
        sel += (active - sel) * 0.25F;
        for (int i = 0; i < n; i++) {
            hov[i] += ((i == over ? 1 : 0) - hov[i]) * 0.3F;
            int tx = b[i], tw = b[i + 1] - b[i];
            boolean on = i == active;
            int top = on ? y - 1 : y + 2 - (int) (hov[i] * 1.5F);
            if (on) {
                // la elegida: del color del panel, sin borde abajo (queda unida al contenido)
                Ui.boton(g, tx + 1, top, tw - 2, y + h - top + 2, 0, false, true, activa(t));
                g.fill(tx + 2, y + h, tx + tw - 2, y + h + 2, Ui.mix(t.bgTop | 0xFF000000, 0xFFFFFFFF, 0.06F));
            } else {
                Ui.boton(g, tx + 1, top, tw - 2, y + h - top, hov[i], false, true, t);
            }
            Ui.textoQueCabe(g, names[i], tx + 3, top + 1, tw - 6, y + h - top, on ? t.title : Ui.mix(t.textDim, 0xFFFFFFFF, hov[i] * 0.5F),
                    true, true);
        }
        // la marca se desliza entre los bordes de las pestañas (que pueden ser de distinto ancho)
        int i0 = Math.max(0, Math.min(n - 1, (int) Math.floor(sel))), i1 = Math.min(n - 1, i0 + 1);
        float f = sel - i0;
        float l0 = b[i0] + (b[i1] - b[i0]) * f, r0 = b[i0 + 1] + (b[i1 + 1] - b[i0 + 1]) * f;
        g.fill(Math.round(l0) + 4, y + h - 2, Math.round(r0) - 4, y + h - 1, t.accent);
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
