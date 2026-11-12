package com.casino.client;

import com.casino.plat.client.G;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Cartel de premio en la parte de arriba de la pantalla: aparece con rebote, brilla y suelta confeti unos segundos. */
public final class WinBanner {
    private static final int[] CONFETTI = {0xFFE040, 0xFF4060, 0x40D0FF, 0x60E070, 0xFF9030, 0xC060FF, 0xFFFFFF};
    private static Component title;
    private static List<ItemStack> items = new ArrayList<>();
    private static int xp;
    private static boolean big;
    private static long startMs = -1;
    private static long seed;

    private WinBanner() {
    }

    public static void start(Component t, List<ItemStack> list, int levels, boolean isBig) {
        title = t;
        items = new ArrayList<>(list);
        xp = levels;
        big = isBig;
        startMs = Util.getMillis();
        seed = startMs;
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158F;
        float c3 = c1 + 1;
        float p = x - 1;
        return 1 + c3 * p * p * p + c1 * p * p;
    }

    public static void render(G g, int sw, int sh) {
        if (startMs < 0) {
            return;
        }
        float t = (Util.getMillis() - startMs) / 1000.0F;
        float total = big ? 7.0F : 5.0F;
        if (t > total) {
            startMs = -1;
            return;
        }
        Font font = Minecraft.getInstance().font;
        float fadeOut = Math.min(1, (total - t) / 0.6F);
        float pop = easeOutBack(Math.min(1, t / 0.5F));
        int pw = 250;
        int ph = big ? 92 : 78;
        int cx = sw / 2;
        int top = Math.max(8, sh / 8);

        // confeti (detrás del cartel)
        Random r = new Random(seed);
        float confettiTime = big ? 5.0F : 3.0F;
        if (t < confettiTime) {
            int n = big ? 110 : 60;
            for (int i = 0; i < n; i++) {
                float x0 = r.nextFloat() * sw;
                float delay = r.nextFloat() * 1.2F;
                float speed = 60 + r.nextFloat() * 90;
                float sway = (float) Math.sin((t * 3 + i) * 1.3) * 12;
                float y = (t - delay) * speed - 10;
                if (y < -10 || y > sh) {
                    continue;
                }
                int col = 0xFF000000 | CONFETTI[r.nextInt(CONFETTI.length)];
                int w = 3 + r.nextInt(3);
                int h = 4 + r.nextInt(4);
                g.fill((int) (x0 + sway), (int) y, (int) (x0 + sway) + w, (int) y + h, col);
            }
        }

        g.push();
        g.translate(cx, top + ph / 2.0F, 0);
        g.scale(pop, pop, 1);
        g.translate(-cx, -(top + ph / 2.0F), 0);
        int a = (int) (fadeOut * 255);
        int x0 = cx - pw / 2;
        // marco dorado + fondo
        g.fill(x0 - 3, top - 3, x0 + pw + 3, top + ph + 3, (a << 24) | 0xE0B030);
        g.fill(x0 - 1, top - 1, x0 + pw + 1, top + ph + 1, (a << 24) | 0x5A3A0A);
        g.fillGradient(x0, top, x0 + pw, top + ph, ((int) (fadeOut * 235) << 24) | 0x2A1240, ((int) (fadeOut * 235) << 24) | 0x0A0612);
        // brillo que cruza el cartel
        float sweep = (t * 0.9F) % 2.0F;
        if (sweep < 1.0F) {
            int bx = x0 + (int) (sweep * (pw + 60)) - 30;
            for (int i = 0; i < 24; i++) {
                int xx = bx + i;
                if (xx > x0 && xx < x0 + pw) {
                    int al = (int) (fadeOut * 55 * (1 - Math.abs(i - 12) / 12.0F));
                    g.fill(xx, top, xx + 1, top + ph, (al << 24) | 0xFFFFFF);
                }
            }
        }
        // texto
        float pulse = 1.0F + 0.06F * (float) Math.sin(t * 8);
        g.push();
        g.translate(cx, top + 8, 0);
        g.scale(2.0F * pulse, 2.0F * pulse, 1);
        Component head = Component.translatable(big ? "casino.win.big" : "casino.win.normal").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        g.drawString(font, head, -font.width(head) / 2, 0, 0xFFFFFF, true);
        g.pop();
        g.drawCenteredString(font, title, cx, top + 32, 0xFFFFFF);
        int y = top + 46;
        if (xp > 0) {
            g.drawCenteredString(font, Component.translatable("casino.win.xp", xp).withStyle(ChatFormatting.GREEN), cx, y + 4, 0xFFFFFF);
        }
        int shown = Math.min(items.size(), 9);
        if (shown > 0) {
            int total2 = shown * 22;
            int ix = cx - total2 / 2;
            for (int i = 0; i < shown; i++) {
                float bob = (float) Math.sin(t * 4 + i) * 1.5F;
                g.renderItem(items.get(i), ix + i * 22, (int) (y + bob) + (xp > 0 ? 14 : 0));
                g.renderItemDecorations(font, items.get(i), ix + i * 22, (int) (y + bob) + (xp > 0 ? 14 : 0));
            }
        }
        g.pop();
    }
}
