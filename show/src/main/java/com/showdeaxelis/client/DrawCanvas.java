package com.showdeaxelis.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.showdeaxelis.game.DrawGame;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** El lienzo de "Adivina el dibujo" en el cliente: una imagen que se va pintando con los trazos que llegan. */
public final class DrawCanvas {
    public static final int W = DrawGame.W;
    public static final int H = DrawGame.H;
    /** Colores del lienzo. El 1 (blanco) sirve de borrador. */
    public static final int[] PALETTE = {
        0x111111, 0xFFFFFF, 0xE53935, 0xFB8C00, 0xFDD835, 0x43A047, 0x1E88E5, 0x4FC3F7,
        0x8E24AA, 0xF48FB1, 0x795548, 0x9E9E9E, 0x1B5E20, 0x7F0000, 0x0D2A6B, 0xFFCC99
    };
    private static DynamicTexture tex;
    private static ResourceLocation loc;
    private static boolean dirty;

    private DrawCanvas() {
    }

    private static boolean ensure() {
        if (tex != null) {
            return true;
        }
        try {
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, W, H, true);
            tex = new DynamicTexture(img);
            loc = Minecraft.getInstance().getTextureManager().register("show_draw", tex);
            clear();
            return true;
        } catch (Throwable t) {
            tex = null;
            return false;
        }
    }

    private static int abgr(int rgb) {
        return 0xFF000000 | ((rgb & 0xFF) << 16) | (rgb & 0xFF00) | ((rgb >> 16) & 0xFF);
    }

    public static void clear() {
        if (!ensure()) {
            return;
        }
        tex.getPixels().fillRect(0, 0, W, H, 0xFFFFFFFF);
        dirty = true;
    }

    private static void dot(NativeImage img, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy <= r * r + r) {
                    int x = cx + dx;
                    int y = cy + dy;
                    if (x >= 0 && y >= 0 && x < W && y < H) {
                        img.setPixelRGBA(x, y, color);
                    }
                }
            }
        }
    }

    /** Pinta un trazo (puntos x,y,x,y...) con el color y grosor dados. */
    public static void stroke(int colorIdx, int size, int[] pts) {
        if (!ensure() || pts.length < 2) {
            return;
        }
        NativeImage img = tex.getPixels();
        int col = abgr(PALETTE[Math.floorMod(colorIdx, PALETTE.length)]);
        int r = Math.max(1, Math.min(6, size)) - 1;
        r = r == 0 ? 1 : r;
        dot(img, pts[0], pts[1], r, col);
        for (int i = 2; i + 1 < pts.length; i += 2) {
            int x0 = pts[i - 2];
            int y0 = pts[i - 1];
            int x1 = pts[i];
            int y1 = pts[i + 1];
            int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
            for (int k = 1; k <= Math.max(1, steps); k++) {
                dot(img, x0 + (x1 - x0) * k / Math.max(1, steps), y0 + (y1 - y0) * k / Math.max(1, steps), r, col);
            }
        }
        dirty = true;
    }

    /** Llega un paquete del servidor: op 0 trazo, op 1 borrar. */
    public static void onPacket(int op, int color, int size, int[] pts) {
        if (op == 1) {
            clear();
        } else {
            stroke(color, size, pts);
        }
    }

    /** Dibuja el lienzo en pantalla con un marco. */
    public static void render(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF1B1B3A);
        if (!ensure()) {
            g.fill(x, y, x + w, y + h, 0xFFFFFFFF);
            return;
        }
        if (dirty) {
            tex.upload();
            dirty = false;
        }
        g.blit(loc, x, y, w, h, 0f, 0f, W, H, W, H);
    }
}
