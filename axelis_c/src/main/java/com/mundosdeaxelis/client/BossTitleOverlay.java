package com.mundosdeaxelis.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;

/**
 * Título épico de jefe: las letras llegan muy separadas y se juntan, dos líneas se abren desde el centro, el subtítulo
 * aparece debajo y todo se desvanece. Solo cliente.
 */
public final class BossTitleOverlay {
    private static String title = "";
    private static String subtitle = "";
    private static int rgb = 0xFFC24A;
    private static int total = 0;
    private static int elapsed = -1;
    private static boolean compact = false;

    private BossTitleOverlay() {
    }

    public static void start(String t, String s, int color, int ticks) {
        title = t;
        subtitle = s;
        rgb = color & 0xFFFFFF;
        total = Math.max(40, ticks);
        compact = total <= 70;
        elapsed = 0;
    }

    public static void tick() {
        if (elapsed >= 0 && ++elapsed > total) {
            elapsed = -1;
        }
    }

    private static int withAlpha(int color, float a) {
        int al = Mth.clamp((int) (a * 255.0F), 5, 255);
        return (al << 24) | (color & 0xFFFFFF);
    }

    /** Dibuja una línea de texto con separación entre letras, centrada en cx. */
    private static void spaced(GuiGraphics g, Font font, String text, float cx, float y, float scale, float spacing, int color, float alpha, boolean bold) {
        if (text.isEmpty()) {
            return;
        }
        float w = 0.0F;
        int[] cw = new int[text.length()];
        Style st = Style.EMPTY.withBold(bold);
        for (int i = 0; i < text.length(); i++) {
            cw[i] = font.width(Component.literal(String.valueOf(text.charAt(i))).withStyle(st));
            w += cw[i] + (i < text.length() - 1 ? spacing : 0.0F);
        }
        float x = -w * 0.5F;
        g.pose().pushPose();
        g.pose().translate(cx, y, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        for (int i = 0; i < text.length(); i++) {
            g.drawString(font, Component.literal(String.valueOf(text.charAt(i))).withStyle(st), Math.round(x), 0, withAlpha(color, alpha), true);
            x += cw[i] + spacing;
        }
        g.pose().popPose();
    }

    public static void render(GuiGraphics g, float partial, int w, int h) {
        if (elapsed < 0) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        float t = elapsed + partial;
        float in = Mth.clamp(t / 22.0F, 0.0F, 1.0F);
        float out = Mth.clamp((total - t) / 24.0F, 0.0F, 1.0F);
        float a = Math.min(in, out);
        if (a <= 0.01F) {
            return;
        }
        float ease = 1.0F - (1.0F - in) * (1.0F - in) * (1.0F - in);
        float cx = w * 0.5F;
        float cy = compact ? h * 0.22F : h * 0.42F;
        // banda oscura detrás para que se lea
        int band = (int) (h * (compact ? 0.11F : 0.17F));
        int bandAlpha = Mth.clamp((int) (a * (compact ? 60.0F : 130.0F)), 0, 255);
        g.fill(0, (int) cy - band / 2, w, (int) cy + band, (bandAlpha << 24));
        // líneas que se abren
        int half = (int) (w * 0.30F * ease);
        int lineA = Mth.clamp((int) (a * 230.0F), 5, 255);
        int ly1 = (int) cy - band / 2 + 4;
        int ly2 = (int) cy + band - 5;
        g.fill((int) cx - half, ly1, (int) cx + half, ly1 + 1, (lineA << 24) | rgb);
        g.fill((int) cx - half, ly2, (int) cx + half, ly2 + 1, (lineA << 24) | rgb);
        // título: las letras se juntan
        float scale = Math.min(3.4F, w / Math.max(160.0F, font.width(title) * 1.9F + 40.0F));
        scale = Math.max(1.6F, scale);
        if (compact) {
            scale = Math.min(scale, 2.2F);
        }
        float spacing = 2.0F + (1.0F - ease) * 26.0F;
        spaced(g, font, title, cx, cy - 8.0F * scale * 0.5F, scale, spacing, 0xFFFFFF, a, true);
        // reflejo de color suave del mismo título, apenas desplazado
        spaced(g, font, title, cx, cy - 8.0F * scale * 0.5F + 1.0F, scale, spacing, rgb, a * 0.28F, true);
        float sub = Mth.clamp((t - 18.0F) / 18.0F, 0.0F, 1.0F) * out;
        if (sub > 0.01F) {
            spaced(g, font, subtitle, cx, cy + 8.0F * scale * 0.5F + 10.0F, 1.25F, 3.0F + (1.0F - sub) * 6.0F, rgb, sub, false);
        }
    }
}
