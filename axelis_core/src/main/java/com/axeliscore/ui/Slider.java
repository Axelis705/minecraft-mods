package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.function.Consumer;
import java.util.function.Function;

/** Deslizador con nombre y valor (se arrastra o se hace clic en la barra; la rueda del mouse lo mueve de a un paso). */
public class Slider extends Widget {
    public String label;
    public float min, max, step, value;
    private final Consumer<Float> onChange;
    private final Function<Float, String> fmt;
    private boolean dragging;
    private InfoCard card;

    public Slider(int x, int y, int w, String label, float min, float max, float step, float value, Function<Float, String> fmt,
            Consumer<Float> onChange) {
        super(x, y, w, 22);
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.value = value;
        this.fmt = fmt;
        this.onChange = onChange;
    }

    public Slider card(InfoCard c) {
        this.card = c;
        return this;
    }

    private float frac() {
        return max > min ? (value - min) / (max - min) : 0;
    }

    private float shown = -1;

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        if (shown < 0) {
            shown = frac();
        }
        g.drawString(Ui.font(), label, x, y, t.text, false);
        String v = fmt != null ? fmt.apply(value) : String.valueOf(value);
        g.drawString(Ui.font(), v, x + w - Ui.font().width(v), y, t.accent, true);
        int ty = y + 13;
        Ui.inset(g, x, ty, w, 6, t);
        shown += (frac() - shown) * 0.4F;
        int fw = Math.round((w - 2) * shown);
        g.fillGradient(x + 1, ty + 1, x + 1 + fw, ty + 5, Ui.mix(t.accent, 0xFFFFFFFF, 0.3F), t.accent);
        // marcas de los pasos (si son pocos)
        if (step > 0 && (max - min) / step <= 12) {
            int n = Math.round((max - min) / step);
            for (int i = 1; i < n; i++) {
                int mx2 = x + 1 + (w - 2) * i / n;
                g.fill(mx2, ty + 1, mx2 + 1, ty + 5, Ui.alpha(0xFF000000, 0.3F));
            }
        }
        int kx = x + 1 + fw - 4;
        Ui.boton(g, kx, ty - 4, 9, 14, hover, false, enabled, t);
        g.fill(kx + 4, ty - 1, kx + 5, ty + 7, Ui.alpha(0xFF000000, 0.3F));
    }

    private void setFrom(double mx) {
        float f = (float) Math.max(0, Math.min(1, (mx - x) / w));
        float v = min + f * (max - min);
        if (step > 0) {
            v = Math.round(v / step) * step;
        }
        v = Math.max(min, Math.min(max, v));
        if (v != value) {
            value = v;
            if (onChange != null) {
                onChange.accept(v);
            }
        }
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button == 0 && enabled && inside(mx, my) && my >= y + 9) {
            dragging = true;
            setFrom(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean drag(double mx, double my, int button) {
        if (dragging) {
            setFrom(mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean release(double mx, double my, int button) {
        if (dragging) {
            dragging = false;
            Ui.click(1.1F);
            return true;
        }
        return false;
    }

    @Override
    public boolean scroll(double mx, double my, double delta) {
        if (!inside(mx, my) || !enabled) {
            return false;
        }
        float s = step > 0 ? step : (max - min) / 20F;
        value = Math.max(min, Math.min(max, value + (float) Math.signum(delta) * s));
        if (onChange != null) {
            onChange.accept(value);
        }
        return true;
    }

    @Override
    public InfoCard card(double mx, double my) {
        return card;
    }
}
