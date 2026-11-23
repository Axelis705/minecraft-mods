package com.axeliscore.ui;

import com.axeliscore.plat.client.CoreScreen;
import com.axeliscore.plat.client.G;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla con el estilo del Core: un panel centrado (de {@link #pw} × {@link #ph}) que aparece con un pequeño rebote, título,
 * componentes ({@link Widget}) y ayudas al pasar el mouse. Las subclases agregan componentes en {@link #build} (con posiciones
 * relativas al panel: {@link #px}, {@link #py}) y dibujan lo suyo en {@link #content}.
 */
public abstract class UiScreen extends CoreScreen {
    protected final Theme theme;
    protected final int pw, ph;
    protected int px, py;
    protected final List<Widget> widgets = new ArrayList<>();
    private long opened = -1, last;
    protected float time;

    protected UiScreen(Component title, int pw, int ph, Theme theme) {
        super(title);
        this.pw = pw;
        this.ph = ph;
        this.theme = theme;
    }

    @Override
    protected void init() {
        super.init();
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        widgets.clear();
        build();
        if (opened < 0) {
            opened = System.currentTimeMillis();
        }
    }

    /** Agrega los componentes (se llama al abrir y al cambiar el tamaño de la ventana). */
    protected abstract void build();

    /** Lo propio de la pantalla (dentro del panel). */
    protected void content(G g, int mx, int my, float pt) {
    }

    protected <W extends Widget> W add(W w) {
        widgets.add(w);
        return w;
    }

    /** Cuánto lleva abierta (segundos). */
    protected float openTime() {
        return (System.currentTimeMillis() - opened) / 1000F;
    }

    @Override
    protected void draw(G g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float dt = last == 0 ? 0 : Math.min(0.1F, (now - last) / 1000F);
        last = now;
        time += dt;
        drawBackground(g, mx, my, pt);
        float a = Ui.pop(Math.min(1F, openTime() / 0.22F));
        g.push();
        float s = 0.9F + 0.1F * a;
        g.translate(width / 2F, height / 2F, 0);
        g.scale(s, s, 1);
        g.translate(-width / 2F, -height / 2F, 0);
        Ui.panel(g, px, py, pw, ph, theme, Math.min(1F, openTime() / 0.12F));
        Ui.title(g, getTitle().getString(), px + pw / 2, py + 8, theme);
        content(g, mx, my, pt);
        for (Widget w : widgets) {
            if (w.visible) {
                w.render(g, mx, my, dt, theme);
            }
        }
        g.pop();
        for (Widget w : widgets) {
            if (w.inside(mx, my) && w.tooltip() != null) {
                g.renderTooltip(Ui.font(), w.tooltip(), mx, my);
                break;
            }
        }
    }

    @Override
    protected boolean onClick(double x, double y, int b) {
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).click(x, y, b)) {
                return true;
            }
        }
        return super.onClick(x, y, b);
    }

    @Override
    protected boolean onRelease(double x, double y, int b) {
        boolean any = false;
        for (Widget w : widgets) {
            any |= w.release(x, y, b);
        }
        return any || super.onRelease(x, y, b);
    }

    @Override
    protected boolean onDrag(double x, double y, int b, double dx, double dy) {
        for (Widget w : widgets) {
            if (w.drag(x, y, b)) {
                return true;
            }
        }
        return super.onDrag(x, y, b, dx, dy);
    }

    @Override
    protected boolean onScroll(double x, double y, double delta) {
        for (Widget w : widgets) {
            if (w.scroll(x, y, delta)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
