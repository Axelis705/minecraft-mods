package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Botón cuadrado con un objeto o un dibujo adentro (para barras de herramientas, filtros, pestañas con ícono). */
public class IconButton extends Widget {
    public ItemStack icon = ItemStack.EMPTY;
    public ResourceLocation tex;
    public int texSize = 16;
    public boolean selected;
    public Runnable onClick;
    private InfoCard card;
    private boolean pressed;

    public IconButton(int x, int y, int size, ItemStack icon, Runnable onClick) {
        super(x, y, size, size);
        this.icon = icon;
        this.onClick = onClick;
    }

    public IconButton(int x, int y, int size, ResourceLocation tex, int texSize, Runnable onClick) {
        super(x, y, size, size);
        this.tex = tex;
        this.texSize = texSize;
        this.onClick = onClick;
    }

    public IconButton card(InfoCard c) {
        this.card = c;
        return this;
    }

    @Override
    protected void draw(G g, int mx, int my, Theme t) {
        int dy = pressed && inside(mx, my) ? 1 : 0;
        Ui.boton(g, x, y, w, h, hover, dy > 0, enabled, t);
        if (selected) {
            // elegido: marco del color de acento que late suave
            int c = Ui.alpha(t.accent, 0.75F + 0.25F * (float) Math.sin(Ui.tiempo() * 4));
            g.fill(x + 1, y + 1, x + w - 1, y + 2, c);
            g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, c);
            g.fill(x + 1, y + 2, x + 2, y + h - 2, c);
            g.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, c);
        }
        int s = tex != null ? texSize : 16;
        int ix = x + (w - s) / 2, iy = y + (h - 1 - s) / 2 + dy;
        // el ícono sube un pixel con el mouse encima
        iy -= Math.round(hover);
        if (tex != null) {
            g.blit(tex, ix, iy, s, s, 0, 0, s, s, s, s);
        } else if (!icon.isEmpty()) {
            g.renderItem(icon, ix, iy);
        }
        if (!enabled) {
            g.fill(x + 1, y + 1, x + w - 1, y + h - 1, Ui.alpha(0xFF000000, 0.5F));
        }
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
            Ui.click(1.2F);
            if (onClick != null) {
                onClick.run();
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
