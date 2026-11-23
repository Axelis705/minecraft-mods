package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

import java.util.function.Consumer;

/** Interruptor encendido/apagado con su nombre a la izquierda. */
public class Toggle extends Widget {
    public String label;
    public boolean value;
    private float knob;
    private final Consumer<Boolean> onChange;
    private InfoCard card;

    public Toggle(int x, int y, int w, String label, boolean value, Consumer<Boolean> onChange) {
        super(x, y, w, 14);
        this.label = label;
        this.value = value;
        this.knob = value ? 1 : 0;
        this.onChange = onChange;
    }

    public Toggle card(InfoCard c) {
        this.card = c;
        return this;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        knob += ((value ? 1 : 0) - knob) * 0.35F;
        if (hover > 0.01F) {
            g.fill(x - 2, y - 1, x + w + 2, y + h + 1, Ui.alpha(0xFFFFFFFF, 0.05F * hover));
        }
        g.drawString(Ui.font(), label, x, y + 3, enabled ? t.text : t.textDim, false);
        int sw = 26, sx = x + w - sw, sy = y + 1;
        int on = t.good, off = 0xFF4A4F5C;
        // carril hundido (como una ranura) que se llena de color, y una palanca con la textura de botón
        Ui.inset(g, sx, sy, sw, 12, t);
        g.fill(sx + 1, sy + 1, sx + 1 + Math.round(knob * (sw - 2)), sy + 11, Ui.alpha(Ui.mix(off, on, knob), 0.85F));
        int kx = sx + Math.round(knob * (sw - 12));
        Ui.boton(g, kx, sy - 1, 12, 14, hover, false, enabled, t);
        g.fill(kx + 4, sy + 3, kx + 5, sy + 9, Ui.alpha(0xFF000000, 0.35F));
        g.fill(kx + 7, sy + 3, kx + 8, sy + 9, Ui.alpha(0xFF000000, 0.35F));
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button == 0 && enabled && inside(mx, my)) {
            value = !value;
            Ui.click(value ? 1.3F : 0.9F);
            if (onChange != null) {
                onChange.accept(value);
            }
            return true;
        }
        return false;
    }

    @Override
    public InfoCard card(double mx, double my) {
        return card;
    }
}
