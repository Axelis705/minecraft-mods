package com.mundosdeaxelis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

/** Termómetro de frío en el borde izquierdo de la pantalla: sube cuando te enfrías, parpadea cerca de 100. */
public final class ColdHud {
    private ColdHud() {
    }

    private static int target;
    private static float shown;
    private static long ticks;

    public static void set(int meter) {
        target = Mth.clamp(meter, 0, 100);
    }

    public static void tick() {
        ticks++;
        shown += (target - shown) * 0.2F;
        if (Math.abs(target - shown) < 0.05F) {
            shown = target;
        }
    }

    private static int a(float alpha, int rgb) {
        return Mth.clamp((int) (alpha * 255.0F), 0, 255) << 24 | (rgb & 0xFFFFFF);
    }

    private static int mix(int c1, int c2, float t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int gr = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return r << 16 | gr << 8 | b;
    }

    public static void render(GuiGraphics g, float pt, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        if (shown < 0.5F || mc.player == null || mc.options.hideGui) {
            return;
        }
        Font f = mc.font;
        float v = shown / 100.0F;
        int tw = 7, th = 70;
        int x = 8, y = height / 2 - th / 2;
        boolean danger = shown >= 85.0F;
        float pulse = danger ? 0.55F + 0.45F * (float) Math.sin((ticks + pt) * 0.7F) : 1.0F;
        int edge = danger ? mix(0x7FD0FF, 0xFF6B6B, 1.0F - pulse) : 0x7FD0FF;
        float alpha = Mth.clamp(shown / 8.0F, 0.0F, 1.0F);
        // tubo
        g.fill(x - 2, y - 2, x + tw + 2, y + th + 2, a(0.85F * alpha, 0x050B14));
        g.fill(x - 1, y - 1, x + tw + 1, y + th + 1, a(alpha, edge));
        g.fill(x, y, x + tw, y + th, a(alpha, 0x0A1A2C));
        int fh = (int) (th * v);
        g.fillGradient(x, y + th - fh, x + tw, y + th, a(alpha, mix(0xCFF3FF, 0x4FA8E8, v)), a(alpha, 0x2C6FB8));
        // marcas de 40 (lento) y 70 (manos torpes)
        for (int m : new int[]{40, 70}) {
            int my = y + th - th * m / 100;
            g.fill(x + tw + 1, my, x + tw + 5, my + 1, a(0.7F * alpha, 0xBFEFFF));
        }
        // bulbo
        g.fill(x - 2, y + th + 1, x + tw + 2, y + th + 8, a(alpha, edge));
        g.fill(x - 1, y + th + 2, x + tw + 1, y + th + 7, a(alpha, mix(0xCFF3FF, 0x4FA8E8, v)));
        g.drawString(f, "❄", x - 1, y - 12, a(alpha, 0xBFEFFF), true);
        String n = (int) shown + "";
        g.drawString(f, n, x + tw + 7, y + th - fh - 4, a(alpha, 0xE8FAFF), true);
    }
}
