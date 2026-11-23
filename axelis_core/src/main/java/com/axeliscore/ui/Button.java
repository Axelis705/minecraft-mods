package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Botón con texto (y ayuda opcional). */
public class Button extends Widget {
    public String label;
    public Runnable onClick;
    private boolean pressed;
    private List<Component> help;

    public Button(int x, int y, int w, int h, String label, Runnable onClick) {
        super(x, y, w, h);
        this.label = label;
        this.onClick = onClick;
    }

    public Button help(Component... lines) {
        this.help = List.of(lines);
        return this;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        Ui.button(g, x, y, w, h, label, hover, pressed && inside(mx, my), enabled, t);
    }

    @Override
    public boolean click(double mx, double my, int button) {
        if (button == 0 && enabled && inside(mx, my)) {
            pressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean release(double mx, double my, int button) {
        boolean was = pressed;
        pressed = false;
        if (was && inside(mx, my) && enabled) {
            Ui.click(1F);
            if (onClick != null) {
                onClick.run();
            }
            return true;
        }
        return false;
    }

    @Override
    public List<Component> tooltip() {
        return help;
    }
}
