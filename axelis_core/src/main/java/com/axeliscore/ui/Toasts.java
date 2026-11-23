package com.axeliscore.ui;

import com.axeliscore.plat.client.G;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Avisos que entran por la derecha, uno debajo de otro, y se van solos a los 4.5 segundos. */
public final class Toasts {
    private static final class T {
        final ItemStack icon;
        final Component title, text;
        final int color;
        final long born = System.currentTimeMillis();

        T(Item icon, Component title, Component text, int color) {
            this.icon = icon == null || icon == Items.AIR ? ItemStack.EMPTY : new ItemStack(icon);
            this.title = title;
            this.text = text;
            this.color = color | 0xFF000000;
        }
    }

    private static final List<T> LIST = new ArrayList<>();
    static final long LIFE = 4500;

    private Toasts() {
    }

    public static void add(Item icon, Component title, Component text, int color) {
        synchronized (LIST) {
            LIST.add(new T(icon, title, text, color));
            while (LIST.size() > 4) {
                LIST.remove(0);
            }
        }
        Ui.click(1.8F);
    }

    public static void render(G g, int sw, int sh) {
        long now = System.currentTimeMillis();
        synchronized (LIST) {
            LIST.removeIf(t -> now - t.born > LIFE);
            int y = 8;
            for (T t : LIST) {
                float age = (now - t.born) / 1000F;
                // entra con un pequeño rebote y sale deslizándose
                float in = Ui.pop(Math.min(1, age / 0.4F));
                float out = Ui.ease(Math.min(1, Math.max(0, (now - t.born - (LIFE - 400)) / 400F)));
                int w = 170, h = 34;
                int x = sw - (int) ((w + 8) * (in - out));
                Theme th = Theme.NOCHE;
                int col = t.color | 0xFF000000;
                Ui.round(g, x + 2, y + 3, w, h, 0x66000000);
                Ui.round(g, x - 1, y - 1, w + 2, h + 2, th.border);
                Ui.fondo(g, th, x, y, w, h, 1F);
                g.fillGradient(x, y, x + w, y + h, Ui.alpha(th.bgTop, 0.55F), Ui.alpha(th.bgBottom, 0.8F));
                Ui.relieve(g, x, y, w, h, 1F);
                g.fill(x, y, x + 3, y + h, col);
                g.fill(x + 3, y, x + 4, y + h, Ui.alpha(col, 0.4F));
                // brillo que cruza al aparecer
                if (age < 0.9F) {
                    int bx = x + (int) (age / 0.9F * (w + 40)) - 20;
                    g.enableScissor(x, y, x + w, y + h);
                    for (int i = 0; i < 12; i++) {
                        g.fill(bx + i - h / 2, y + i * h / 12, bx + i + 6 - h / 2, y + (i + 1) * h / 12, Ui.alpha(0xFFFFFFFF, 0.12F));
                    }
                    g.disableScissor();
                }
                // lo que le queda (línea fina abajo)
                float queda = 1 - Math.min(1, (now - t.born) / (float) LIFE);
                g.fill(x + 4, y + h - 1, x + 4 + (int) ((w - 4) * queda), y + h, Ui.alpha(col, 0.6F));
                int tx = x + 8;
                if (!t.icon.isEmpty()) {
                    Ui.ranura(g, x + 7, y + 8, th, false);
                    g.renderItem(t.icon, x + 8, y + 9);
                    tx = x + 30;
                }
                g.drawString(Ui.font(), t.title.getString(), tx, y + 6, t.color, true);
                String body = t.text.getString();
                var lines = Ui.wrap(body, w - (tx - x) - 6);
                g.drawString(Ui.font(), lines.isEmpty() ? "" : lines.get(0), tx, y + 19, th.text, false);
                y += h + 6;
            }
        }
    }
}
