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

    /** Texto de ayuda al pasar el mouse (null = nada). */
    public java.util.List<net.minecraft.network.chat.Component> tooltip() {
        return null;
    }
}
