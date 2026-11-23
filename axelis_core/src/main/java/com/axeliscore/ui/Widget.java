package com.axeliscore.ui;

import com.axeliscore.plat.client.G;

/** Algo que se dibuja y se puede tocar dentro de una {@link UiScreen}. */
public abstract class Widget {
    public int x, y, w, h;
    public boolean visible = true, enabled = true;
    /** Animación de pasar el mouse (0..1, suave). */
    protected float hover;

    protected Widget(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean inside(double mx, double my) {
        return visible && mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** Dibuja (y anima el hover). */
    public void render(G g, int mx, int my, float dt, Theme t) {
        float target = inside(mx, my) && enabled ? 1 : 0;
        hover += (target - hover) * Math.min(1, dt * 12);
        draw(g, mx, my, t);
    }

    protected abstract void draw(G g, int mx, int my, Theme t);

    public boolean click(double mx, double my, int button) {
        return false;
    }

    public boolean release(double mx, double my, int button) {
        return false;
    }

    public boolean drag(double mx, double my, int button) {
        return false;
    }

    public boolean scroll(double mx, double my, double delta) {
        return false;
    }

    /** Texto de ayuda al pasar el mouse (null = nada). Mejor usar {@link #card}. */
    public java.util.List<net.minecraft.network.chat.Component> tooltip() {
        return null;
    }

    /** Cuadro de información al pasar el mouse por (mx, my) (null = nada). Por defecto arma uno con {@link #tooltip()}. */
    public InfoCard card(double mx, double my) {
        java.util.List<net.minecraft.network.chat.Component> t = tooltip();
        if (t == null || t.isEmpty()) {
            return null;
        }
        InfoCard c = new InfoCard(t.get(0)).width(180);
        for (int i = 1; i < t.size(); i++) {
            c.text(t.get(i));
        }
        return c;
    }

    /** Qué cosa está bajo el mouse (para saber cuándo empezó a estar encima y hacer aparecer el cuadro suave). */
    public Object hoverKey(double mx, double my) {
        return this;
    }
}
